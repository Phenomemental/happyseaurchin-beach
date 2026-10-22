/* Bat Sky's engine: the night sky a day's name draws (its bats, the
   ways they fly, and the clouds that drift over them), one step of a run
   under it, and a run kept as the taps its player made, which re-runs to
   the same catch, tap for tap, in any browser. Nothing here touches the
   page; bat-sky.html draws it and takes the input.

   A run is tapped: each tap catches the nearest bat within reach, or
   misses, and a miss leaves the hand resting for a moment. A bat's place
   at a step is worked out from whole numbers and + - * / over tables
   built once from the day's name, then rounded to whole units, so two
   browsers re-running one ghost catch the same bats at the same steps.
   The clouds and the dark only change what can be seen; drawing may use
   anything, and the simulation never reads what drawing computes. */
(function (root) {
  // The law runs are made under now, and every law a kept run may carry:
  // bs1, the first morning's bats; bs2, the same bats 15% slower, for the
  // children (22 October 2026); bs3, another 10% slower again, so they are
  // easier still to see and catch. A kept run re-runs under its own law.
  var VERSION = 'bs3';
  var VERSIONS = ['bs1', 'bs2', 'bs3'];
  var RATE = 60;                // steps a second

  // The sky is a box W wide and H tall, in whole units, made for a phone
  // held upright; the sea's horizon crosses it at HORIZON.
  var W = 1000, H = 1600, HORIZON = 1400;

  // How a run goes (sky law bs1, until frozen by playing on phones).
  var LAW = {
    steps: 20 * RATE,           // the sun rises twenty seconds after the first touch
    reach: 75,                  // a tap catches a bat whose middle is this near, in units
    rest: 6,                    // steps after any tap before the next one counts
    missRest: 30                // steps after a miss before the next one counts
  };

  // ---- A sky from a name -----------------------------------------------------

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

  // Each day draws its bats and its clouds from its seed, a third each:
  // drifting, darting or a swarm; a clear sky, broken cloud or heavy cloud.
  var FLIGHTS = ['drifting bats', 'darting bats', 'a swarm of bats'];
  var CLOUDS = ['a clear sky', 'broken cloud', 'heavy cloud'];
  function kinds(track) {
    return { flight: hash(track + ':bats') % 3, cloud: hash(track + ':cloud') % 3 };
  }
  // What a day's sky is like, in words: 'broken cloud, darting bats'.
  function describe(track) {
    var k = kinds(track);
    return CLOUDS[k.cloud] + ', ' + FLIGHTS[k.flight];
  }

  function whole(v) { return Math.floor(v); }

  // The sky a track's name draws under a law (the one runs are made under
  // now, unless named): its bats, each with when it comes (from, a step,
  // which may be before the first touch so some are already out), how long
  // it stays (life), the points its flight bends round, and its flutter (a
  // rise and fall of amp units every period steps); and its clouds, for
  // drawing.
  function sky(track, version) {
    version = version || VERSION;
    var k = kinds(track), r = random(hash(track));
    var n = [40, 48, 52][k.flight];
    var waves = [];
    if (k.flight === 2) {
      // A swarm comes in four waves, the first as the night starts to end and
      // the rest at times of the day's own.
      for (var w = 0; w < 4; w++) waves.push(w ? whole(w * LAW.steps / 4 - 40 + r() * 120) : -60);
    }
    var bats = [];
    for (var b = 0; b < n; b++) {
      var from, life, amp, period, bends;
      // Drifting and darting bats come one after another, each in its own
      // share of the night, so the sky is never empty for long; some are
      // already out at the first touch.
      if (k.flight === 0) {
        from = -260 + whole((b + r()) * (LAW.steps + 160) / n);
        life = 300 + whole(r() * 150);
        amp = 8 + whole(r() * 12);
        period = 36 + whole(r() * 24);
        bends = 3;
      } else if (k.flight === 1) {
        from = -160 + whole((b + r()) * (LAW.steps + 100) / n);
        life = 170 + whole(r() * 100);
        amp = 15 + whole(r() * 20);
        period = 24 + whole(r() * 16);
        bends = 5;
      } else {
        from = waves[b % 4] - 60 + whole(r() * 160);
        life = 240 + whole(r() * 120);
        amp = 10 + whole(r() * 15);
        period = 28 + whole(r() * 20);
        bends = 4;
      }
      // From bs2 every bat takes 20/17 as long over the same flight, and
      // flutters as much more slowly: 15% slower, with the same paths. From
      // bs3 it takes 10/9 as long again: another 10% slower.
      if (version !== 'bs1') {
        life = whole(life * 20 / 17);
        period = whole(period * 20 / 17);
      }
      if (version !== 'bs1' && version !== 'bs2') {
        life = whole(life * 10 / 9);
        period = whole(period * 10 / 9);
      }
      // From one side of the sky to the other, or, in a swarm, now and
      // then up from the beach below.
      var left = r() < 0.5, pts = [];
      var start = k.flight === 2 && r() < 0.4
        ? [100 + whole(r() * 800), HORIZON + 90]
        : [left ? -90 : W + 90, 120 + whole(r() * 1080)];
      var end = [left ? W + 90 : -90, 100 + whole(r() * 1100)];
      pts.push(start);
      for (var i = 1; i <= bends; i++) {
        var along = i / (bends + 1);
        var x = start[0] + (end[0] - start[0]) * along + (r() - 0.5) * 360;
        pts.push([Math.min(W - 60, Math.max(60, whole(x))), 100 + whole(r() * 1150)]);
      }
      pts.push(end);
      bats.push({ from: from, life: life, amp: amp, period: period, pts: pts, face: left ? 1 : -1 });
    }
    // Clouds: small heaps of puffs drifting with the day's wind, each at a
    // height of its own, so no one cloud hides the middle of the sky. (Only
    // drawing reads them, so changing them leaves every kept run as it was.)
    var clouds = [], wind = r() < 0.5 ? -1 : 1, many = [3, 6, 9][k.cloud];
    for (var c = 0; c < many; c++) {
      var size = [70, 90, 110][k.cloud] + whole(r() * [60, 70, 80][k.cloud]), puffs = [];
      for (var p = 0; p < 5; p++) {
        puffs.push([whole((r() - 0.5) * size * 1.2), whole((r() - 0.5) * size * 0.3), whole(size * (0.25 + 0.17 * r()))]);
      }
      clouds.push({ x: -300 + whole(r() * 1600), y: 160 + whole((c + r()) * 1000 / many), v: wind * (0.25 + 0.5 * r()), size: size, puffs: puffs });
    }
    return { track: track, version: version, flight: k.flight, cloud: k.cloud, n: n, bats: bats, clouds: clouds };
  }

  // Where bat b is at step s, along a uniform cubic B-spline through its
  // points (the ends held by repeating them), with its flutter; s may be
  // fractional for drawing. Null when it isn't out at s.
  function at(sk, b, s) {
    var bat = sk.bats[b], age = s - bat.from;
    if (age < 0 || age >= bat.life) return null;
    var p = bat.pts, q = [p[0], p[0]].concat(p, [p[p.length - 1], p[p.length - 1]]);
    var segs = q.length - 3, f = age / bat.life * segs, i = Math.floor(f);
    if (i > segs - 1) i = segs - 1;
    var t = f - i, t2 = t * t, t3 = t2 * t, u = 1 - t;
    var b0 = u * u * u / 6, b1 = (3 * t3 - 6 * t2 + 4) / 6, b2 = (-3 * t3 + 3 * t2 + 3 * t + 1) / 6, b3 = t3 / 6;
    var p0 = q[i], p1 = q[i + 1], p2 = q[i + 2], p3 = q[i + 3];
    var m = age % bat.period;
    var flutter = (Math.abs(2 * m - bat.period) * 2 - bat.period) / bat.period;
    return {
      x: b0 * p0[0] + b1 * p1[0] + b2 * p2[0] + b3 * p3[0],
      y: b0 * p0[1] + b1 * p1[1] + b2 * p2[1] + b3 * p3[1] + bat.amp * flutter
    };
  }

  // Where bat b is at step s in whole units, as the run counts it, or
  // null when it isn't in the sky.
  function where(sk, b, s) {
    var p = at(sk, b, s);
    if (!p) return null;
    var x = Math.round(p.x), y = Math.round(p.y);
    return x >= 0 && x <= W && y >= 0 && y <= H ? { x: x, y: y } : null;
  }

  // ---- A run -----------------------------------------------------------------

  // status: 'running' until the sun is 'up'. gone marks the bats
  // caught; readyAt is the step from which a tap counts.
  function start(sk) {
    return { step: 0, caught: 0, taps: 0, readyAt: 0, gone: new Uint8Array(sk.n), status: 'running' };
  }

  // A tap's place, made whole and kept inside the sky.
  function spot(v, most) {
    v = Math.round(+v || 0);
    return v < 0 ? 0 : v > most ? most : v;
  }

  function ready(st) {
    return st.status === 'running' && st.step >= st.readyAt;
  }

  // A tap at (x, y) on this step: it catches the nearest bat in the sky
  // within reach (the lower number when two are as near) and answers with
  // that bat's number, or -1 for a miss; null when the tap doesn't count
  // (the hand still resting, or the sun up).
  function tap(st, x, y, sk) {
    if (!ready(st)) return null;
    x = spot(x, W);
    y = spot(y, H);
    var best = -1, near = LAW.reach * LAW.reach + 1;
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

  // One step on: when the last has passed, the sun is up.
  function step(st) {
    if (st.status !== 'running') return st;
    st.step++;
    if (st.step >= LAW.steps) st.status = 'up';
    return st;
  }

  // How far the night has gone, from 0 at the first touch to 1 at sunrise.
  function progress(st) {
    return st.step / LAW.steps;
  }

  // ---- A run kept: every tap that counted ------------------------------------
  // "bs3.<track>.<caught>.<taps>" (bs1 and bs2 before the bats slowed):
  // each tap as three varints, the steps since the tap before (since the
  // first touch, for the first), then x and y in whole units. The bytes
  // are in base64url.

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

  // taps: one [step, x, y] for each tap that counted, in order.
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

  // Run a kept run again under its sky, drawn under the run's own law. It
  // is good when every tap counts and it catches as many bats as it says;
  // hits holds each tap's step, place and the bat it caught (-1 for a
  // miss), and sky the sky it ran under.
  function rerun(replay, sk) {
    var r = decode(replay);
    if (!r) return null;
    sk = sk && sk.track === r.track && sk.version === r.version ? sk : sky(r.track, r.version);
    var st = start(sk), hits = [], k = 0, good = true;
    while (st.status === 'running') {
      if (k < r.taps.length && r.taps[k][0] === st.step) {
        var got = tap(st, r.taps[k][1], r.taps[k][2], sk);
        if (got === null) { good = false; break; }
        hits.push({ step: st.step, x: r.taps[k][1], y: r.taps[k][2], bat: got });
        k++;
      }
      step(st);
    }
    return {
      ok: good && k === r.taps.length && st.caught === r.caught,
      track: r.track, caught: st.caught, taps: st.taps, hits: hits, sky: sk
    };
  }

  // Whether run a did better than run b: more bats, then fewer taps.
  function better(a, b) {
    return !b || a.caught > b.caught || (a.caught === b.caught && a.taps < b.taps);
  }

  // A catch in words, as the page shows it.
  function words(caught) {
    return caught + (caught === 1 ? ' bat' : ' bats');
  }

  var api = {
    VERSION: VERSION,
    RATE: RATE,
    W: W,
    H: H,
    HORIZON: HORIZON,
    LAW: LAW,
    validTrack: validTrack,
    describe: describe,
    sky: sky,
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
  else root.BatSky = api;
})(this);
