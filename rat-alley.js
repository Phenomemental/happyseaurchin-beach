/* Rat Alley's engine: the alley a day's name draws (its dustbins, its
   lamp, and the rats that come out round the bins), one step of a run in
   it, and a run kept as the swings its player made, which re-runs to the
   same count, swing for swing, in any browser. Nothing here touches the
   page; rat-alley.html draws it and takes the input.

   A run is tapped: each tap swings the bat down on the nearest rat within
   reach and squashes it, or misses, and a miss jars the hand for a moment.
   The rats grow bigger by the second, so the bat reaches further as the
   run goes on. A rat's place at a step is worked out from whole numbers
   and + - * / over tables built once from the day's name, then rounded to
   whole units, so two browsers re-running one ghost squash the same rats
   at the same steps. The lamp and the dark only change what can be seen;
   drawing may use anything, and the simulation never reads what drawing
   computes. */
(function (root) {
  // The law runs are made under now, and every law a kept run may carry. A
  // kept run re-runs under its own law.
  var VERSION = 'ra1';
  var VERSIONS = ['ra1'];
  var RATE = 60;                // steps a second

  // The alley is a box W wide and H tall, in whole units, made for a phone
  // held upright: the back wall across the top, its foot at WALL, and the
  // ground the rats run on from there to the front.
  var W = 1000, H = 1600, WALL = 330;

  // How a run goes (alley law ra1, until frozen by playing on phones).
  var LAW = {
    steps: 30 * RATE,           // the big rat comes thirty seconds after the first touch
    reach: 52,                  // a swing squashes a rat whose middle is this near at the first touch, in units
    grow: 48,                   // and this much further by the end, as the rats grow
    rest: 6,                    // steps after any swing before the next one counts
    missRest: 24                // steps after a miss before the next one counts
  };

  // ---- An alley from a name --------------------------------------------------

  // FNV-1a over the name's characters: the same name, the same seed.
  function hash(s) {
    var h = 0x811c9dc5;
    for (var i = 0; i < s.length; i++) {
      h ^= s.charCodeAt(i);
      h = Math.imul(h, 0x01000193);
    }
    return h >>> 0;
  }

  // mulberry32: integer steps, so every engine draws the same numbers.
  function random(seed) {
    var a = seed >>> 0;
    return function () {
      a = (a + 0x6d2b79f5) >>> 0;
      var t = a;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  // A name is what follows track= in a link: a day's address, or a word.
  function validTrack(track) {
    return typeof track === 'string' && /^[a-z0-9-]{1,40}$/.test(track);
  }

  // Each day draws its rats and its lamp from its seed, a third each:
  // sneaky rats (peeping out of the bins), scurrying rats or a rat pack;
  // a steady lamp, a flickering lamp or a swinging lamp.
  var RATS = ['sneaky rats', 'scurrying rats', 'a rat pack'];
  var LAMPS = ['a steady lamp', 'a flickering lamp', 'a swinging lamp'];
  function kinds(track) {
    return { rats: hash(track + ':rats') % 3, lamp: hash(track + ':lamp') % 3 };
  }
  // What a day's alley is like, in words: 'a swinging lamp, sneaky rats'.
  function describe(track) {
    var k = kinds(track);
    return LAMPS[k.lamp] + ', ' + RATS[k.rats];
  }

  function whole(v) { return Math.floor(v); }

  // How far a swing reaches at step s: further as the rats grow.
  function reach(s) {
    return LAW.reach + whole(LAW.grow * s / LAW.steps);
  }

  // The alley a track's name draws under a law (the one runs are made
  // under now, unless named): its bins, the door at the back, and its rats,
  // each with when it comes out (from, a step, which may be before the
  // first touch so some are already out), how long it is out (life), the
  // points its way bends round, and its beats: how many steps it runs, then
  // stops to sniff, then runs again ([steps, 1 when running]).
  function alley(track, version) {
    version = version || VERSION;
    var k = kinds(track), r = random(hash(track));
    // The bins: three to five, spread over the ground, none too near
    // another or the door.
    var door = 300 + whole(r() * 400), bins = [], want = 3 + hash(track + ':bins') % 3;
    for (var tries = 0; tries < 300 && bins.length < want; tries++) {
      var bx = 130 + whole(r() * 740), by = 600 + whole(r() * 780), ok = true;
      for (var j = 0; j < bins.length; j++) {
        var ex = bins[j].x - bx, ey = bins[j].y - by;
        if (ex * ex + ey * ey < 280 * 280) ok = false;
      }
      if ((bx - door) * (bx - door) + (by - WALL) * (by - WALL) < 300 * 300) ok = false;
      if (ok) bins.push({ x: bx, y: by });
    }
    // Where a rat can come out or go in: under a bin, at the door, or off
    // either side of the alley.
    function hole(which) {
      if (which < bins.length) return [bins[which].x, bins[which].y + 16];
      if (which === bins.length) return [door, WALL + 30];
      return [which === bins.length + 1 ? -70 : W + 70, 480 + whole(r() * 1000)];
    }
    var holes = bins.length + 3;
    function anyHole() { return whole(r() * holes); }
    function onGround(x, y) {
      return [Math.min(W - 60, Math.max(60, whole(x))), Math.min(H - 70, Math.max(WALL + 80, whole(y)))];
    }

    // When each rat comes out: in six stretches of five seconds, more in
    // each than the one before, the first starting before the first touch.
    var SLOT = LAW.steps / 6, counts = [[9, 10, 11, 12, 14, 15], [9, 10, 11, 13, 14, 16], [7, 8, 9, 10, 12, 13]][k.rats];
    var times = [];
    for (var sl = 0; sl < 6; sl++) {
      var lo = sl ? sl * SLOT : -150, span = (sl + 1) * SLOT - lo;
      for (var c = 0; c < counts[sl]; c++) times.push(lo + whole((c + r()) * span / counts[sl]));
    }
    // A rat pack also runs in four packs of five to seven, across the
    // alley together, at times of the day's own.
    var packs = [];
    if (k.rats === 2) {
      for (var p = 0; p < 4; p++) packs.push({ at: whole((p + 0.2 + r() * 0.6) * LAW.steps / 4) - 60, size: 5 + whole(r() * 3), from: anyHole(), to: anyHole() });
    }

    var rats = [];
    // Later rats are quicker: their beats shrink from whole length at the
    // first touch to two thirds of it at the end.
    function quick(n, from) {
      var at = from < 0 ? 0 : from;
      return Math.max(1, whole(n * (3 * LAW.steps - at) / (3 * LAW.steps)));
    }
    function add(from, pts, beats) {
      var life = 0, moving = 0;
      beats = beats.map(function (b) {
        var n = quick(b[0], from);
        life += n;
        if (b[1]) moving += n;
        return [n, b[1]];
      });
      rats.push({ from: from, life: life, moving: moving, pts: pts, beats: beats });
    }
    // A peep: out from under a bin, a stop to sniff, and back in.
    function peep(from) {
      if (!bins.length) return dash(from, anyHole(), anyHole(), 1, 0, 48);
      var b = whole(r() * bins.length), h = hole(b), a = r() * 2 - 1, out = 90 + r() * 70;
      var to = onGround(h[0] + a * out, h[1] + (1 - Math.abs(a)) * out * 0.8 + 20);
      add(from, [h, to, h], [[18 + whole(r() * 10), 1], [30 + whole(r() * 45), 0], [16 + whole(r() * 8), 1]]);
    }
    // A run: from one hole to another, round one to three bends, with
    // stops to sniff on the way (fewer for scurrying rats).
    function dash(from, a, b, bends, stops, pace) {
      if (a === b) b = (b + 1 + whole(r() * (holes - 1))) % holes;
      var start = hole(a), end = hole(b), pts = [start];
      for (var i = 1; i <= bends; i++) {
        var along = i / (bends + 1);
        pts.push(onGround(start[0] + (end[0] - start[0]) * along + (r() - 0.5) * 420,
          start[1] + (end[1] - start[1]) * along + (r() - 0.5) * 420));
      }
      pts.push(end);
      var beats = [], legs = stops + 1, run = pace * (bends + 1);
      for (var l = 0; l < legs; l++) {
        beats.push([whole(run / legs) + whole(r() * 12), 1]);
        if (l < stops) beats.push([20 + whole(r() * 30), 0]);
      }
      add(from, pts, beats);
    }
    times.forEach(function (from) {
      if (k.rats === 0) {
        if (r() < 0.6) peep(from);
        else dash(from, anyHole(), anyHole(), 1 + whole(r() * 2), 1 + whole(r() * 2), 56);
      } else if (k.rats === 1) {
        if (r() < 0.2) peep(from);
        else dash(from, anyHole(), anyHole(), 1 + whole(r() * 3), whole(r() * 2), 42);
      } else {
        if (r() < 0.45) peep(from);
        else dash(from, anyHole(), anyHole(), 1 + whole(r() * 2), whole(r() * 2), 48);
      }
    });
    packs.forEach(function (pk) {
      for (var i = 0; i < pk.size; i++) dash(pk.at + i * (8 + whole(r() * 10)), pk.from, pk.to, 2, 0, 36);
    });
    rats.sort(function (a, b) { return a.from - b.from; });
    return { track: track, version: version, rats: k.rats, lamp: k.lamp, door: door, bins: bins, n: rats.length, list: rats };
  }

  // Where rat b is at step s, along a uniform cubic B-spline through its
  // points (the ends held by repeating them), going on only while it runs;
  // s may be fractional for drawing. With it, for drawing only: which way
  // it faces (hx, hy), whether it has stopped to sniff, and how far
  // through its time out it is (t, 0 to 1). Null when it isn't out at s.
  function at(sk, b, s) {
    var rat = sk.list[b], age = s - rat.from;
    if (age < 0 || age >= rat.life) return null;
    var moved = 0, left = age, running = false;
    for (var k = 0; k < rat.beats.length; k++) {
      var d = rat.beats[k][0];
      if (left < d) {
        running = !!rat.beats[k][1];
        if (running) moved += left;
        break;
      }
      if (rat.beats[k][1]) moved += d;
      left -= d;
    }
    var p = rat.pts, q = [p[0], p[0]].concat(p, [p[p.length - 1], p[p.length - 1]]);
    var segs = q.length - 3, f = moved / rat.moving * segs, i = Math.floor(f);
    if (i > segs - 1) i = segs - 1;
    var t = f - i, t2 = t * t, t3 = t2 * t, u = 1 - t;
    var b0 = u * u * u / 6, b1 = (3 * t3 - 6 * t2 + 4) / 6, b2 = (-3 * t3 + 3 * t2 + 3 * t + 1) / 6, b3 = t3 / 6;
    var d0 = -u * u / 2, d1 = (3 * t2 - 4 * t) / 2, d2 = (-3 * t2 + 2 * t + 1) / 2, d3 = t2 / 2;
    var p0 = q[i], p1 = q[i + 1], p2 = q[i + 2], p3 = q[i + 3];
    return {
      x: b0 * p0[0] + b1 * p1[0] + b2 * p2[0] + b3 * p3[0],
      y: b0 * p0[1] + b1 * p1[1] + b2 * p2[1] + b3 * p3[1],
      hx: d0 * p0[0] + d1 * p1[0] + d2 * p2[0] + d3 * p3[0],
      hy: d0 * p0[1] + d1 * p1[1] + d2 * p2[1] + d3 * p3[1],
      running: running,
      t: age / rat.life
    };
  }

  // Where rat b is at step s in whole units, as the run counts it, or
  // null when it isn't out in the alley.
  function where(sk, b, s) {
    var p = at(sk, b, s);
    if (!p) return null;
    var x = Math.round(p.x), y = Math.round(p.y);
    return x >= 0 && x <= W && y >= 0 && y <= H ? { x: x, y: y } : null;
  }

  // ---- A run -----------------------------------------------------------------

  // status: 'running' until the big rat has 'come'. gone marks the rats
  // squashed; readyAt is the step from which a swing counts.
  function start(sk) {
    return { step: 0, caught: 0, taps: 0, readyAt: 0, gone: new Uint8Array(sk.n), status: 'running' };
  }

  // A tap's place, made whole and kept inside the alley.
  function spot(v, most) {
    v = Math.round(+v || 0);
    return v < 0 ? 0 : v > most ? most : v;
  }

  function ready(st) {
    return st.status === 'running' && st.step >= st.readyAt;
  }

  // A swing at (x, y) on this step: it squashes the nearest rat out in the
  // alley within reach (the lower number when two are as near) and answers
  // with that rat's number, or -1 for a miss; null when the swing doesn't
  // count (the hand still jarred, or the big rat come).
  function tap(st, x, y, sk) {
    if (!ready(st)) return null;
    x = spot(x, W);
    y = spot(y, H);
    var far = reach(st.step), best = -1, near = far * far + 1;
    for (var b = 0; b < sk.n; b++) {
      if (st.gone[b]) continue;
      var p = where(sk, b, st.step);
      if (!p) continue;
      var dx = p.x - x, dy = p.y - y, d2 = dx * dx + dy * dy;
      if (d2 < near) { near = d2; best = b; }
    }
    st.taps++;
    if (best >= 0) {
      st.gone[best] = 1;
      st.caught++;
      st.readyAt = st.step + LAW.rest;
    } else {
      st.readyAt = st.step + LAW.missRest;
    }
    return best;
  }

  // One step on: when the last has passed, the big rat has come.
  function step(st) {
    if (st.status !== 'running') return st;
    st.step++;
    if (st.step >= LAW.steps) st.status = 'come';
    return st;
  }

  // How far the run has gone, from 0 at the first touch to 1 when the big
  // rat comes.
  function progress(st) {
    return st.step / LAW.steps;
  }

  // ---- A run kept: every swing that counted ----------------------------------
  // "ra1.<track>.<squashed>.<swings>": each swing as three varints, the
  // steps since the swing before (since the first touch, for the first),
  // then x and y in whole units. The bytes are in base64url.

  var B64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';

  function toB64(bytes) {
    var s = '';
    for (var i = 0; i < bytes.length; i += 3) {
      var a = bytes[i], b = bytes[i + 1], c = bytes[i + 2];
      var n = (a << 16) | ((b || 0) << 8) | (c || 0);
      s += B64.charAt((n >> 18) & 63) + B64.charAt((n >> 12) & 63);
      if (i + 1 < bytes.length) s += B64.charAt((n >> 6) & 63);
      if (i + 2 < bytes.length) s += B64.charAt(n & 63);
    }
    return s;
  }

  function fromB64(s) {
    var out = [], bits = 0, val = 0;
    for (var i = 0; i < s.length; i++) {
      var c = B64.indexOf(s.charAt(i));
      if (c < 0) return null;
      val = ((val << 6) | c) & 0xffff;
      bits += 6;
      if (bits >= 8) {
        bits -= 8;
        out.push((val >> bits) & 255);
      }
    }
    return out;
  }

  function put(bytes, n) {
    while (n >= 128) { bytes.push((n & 127) | 128); n = Math.floor(n / 128); }
    bytes.push(n);
  }

  // taps: one [step, x, y] for each swing that counted, in order.
  function encode(track, taps, caught) {
    var bytes = [], last = 0;
    for (var i = 0; i < taps.length; i++) {
      put(bytes, taps[i][0] - last);
      put(bytes, spot(taps[i][1], W));
      put(bytes, spot(taps[i][2], H));
      last = taps[i][0];
    }
    return [VERSION, track, caught, toB64(bytes)].join('.');
  }

  // A kept run's parts, or null when it isn't one this engine can re-run.
  function decode(replay) {
    var parts = typeof replay === 'string' ? replay.split('.') : [];
    if (parts.length !== 4 || VERSIONS.indexOf(parts[0]) < 0 || !validTrack(parts[1]) || !/^\d{1,3}$/.test(parts[2])) return null;
    var bytes = fromB64(parts[3]);
    if (!bytes) return null;
    var nums = [], n = 0, mul = 1;
    for (var i = 0; i < bytes.length; i++) {
      n += (bytes[i] & 127) * mul;
      if (bytes[i] & 128) { mul *= 128; continue; }
      nums.push(n);
      n = 0;
      mul = 1;
    }
    if (mul !== 1 || nums.length % 3) return null;
    var taps = [], s = 0;
    for (i = 0; i < nums.length; i += 3) {
      if (i > 0 && nums[i] < 1) return null;
      s += nums[i];
      if (s >= LAW.steps || nums[i + 1] > W || nums[i + 2] > H) return null;
      taps.push([s, nums[i + 1], nums[i + 2]]);
    }
    return { version: parts[0], track: parts[1], caught: +parts[2], taps: taps };
  }

  // Run a kept run again in its alley, drawn under the run's own law. It is
  // good when every swing counts and it squashes as many rats as it says;
  // hits holds each swing's step, place and the rat it squashed (-1 for a
  // miss), and alley the alley it ran in.
  function rerun(replay, sk) {
    var r = decode(replay);
    if (!r) return null;
    sk = sk && sk.track === r.track && sk.version === r.version ? sk : alley(r.track, r.version);
    var st = start(sk), hits = [], k = 0, good = true;
    while (st.status === 'running') {
      if (k < r.taps.length && r.taps[k][0] === st.step) {
        var got = tap(st, r.taps[k][1], r.taps[k][2], sk);
        if (got === null) { good = false; break; }
        hits.push({ step: st.step, x: r.taps[k][1], y: r.taps[k][2], rat: got });
        k++;
      }
      step(st);
    }
    return {
      ok: good && k === r.taps.length && st.caught === r.caught,
      track: r.track, caught: st.caught, taps: st.taps, hits: hits, alley: sk
    };
  }

  // Whether run a did better than run b: more rats, then fewer swings.
  function better(a, b) {
    return !b || a.caught > b.caught || (a.caught === b.caught && a.taps < b.taps);
  }

  // A count in words, as the page shows it.
  function words(caught) {
    return caught + (caught === 1 ? ' rat' : ' rats');
  }

  var api = {
    VERSION: VERSION,
    RATE: RATE,
    W: W,
    H: H,
    WALL: WALL,
    LAW: LAW,
    validTrack: validTrack,
    describe: describe,
    reach: reach,
    alley: alley,
    at: at,
    where: where,
    start: start,
    ready: ready,
    tap: tap,
    step: step,
    progress: progress,
    encode: encode,
    decode: decode,
    rerun: rerun,
    better: better,
    words: words
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.RatAlley = api;
})(this);
