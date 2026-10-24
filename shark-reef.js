/* Shark Reef's engine: the reef a day's name draws (its fish and the ways
   their shoals swim), one step of a run in it, and a run kept as the
   moves its shark made, which re-runs to the same run, step for step, in
   any browser. Nothing here touches the page; shark-reef.html draws it and
   takes the input.

   A run is steered: the shark swims after the finger, and each step's move
   is how far it swims, in whole units, never faster than its top speed
   and never turning sharper than it can. The fish flee when it comes near,
   darting aside when it comes straight at them, and swim back to their
   shoals when it has gone. Everything a step depends on is whole numbers
   and + - * / over tables built once from the day's name (a square root
   is found in whole numbers too), so two browsers re-running one ghost
   eat the same fish at the same steps. The water only changes what can be
   seen; drawing may use anything, and the simulation never reads what
   drawing computes. */
(function (root) {
  // The law runs are made under now, and every law a kept run may carry. A
  // kept run re-runs under its own law.
  var VERSION = 'sr1';
  var VERSIONS = ['sr1'];
  var RATE = 60;                // steps a second

  // The reef is a box W wide and H tall, in whole units, made for a phone
  // held upright: the surface at the top, the sea floor at the foot. The
  // shark and the fish swim in the water between, inside EDGE.
  var W = 1000, H = 1600;
  var EDGE = { x0: 40, x1: 960, y0: 70, y1: 1470 };

  // How a run goes (reef law sr1, until frozen by playing on phones).
  var LAW = {
    steps: 45 * RATE,           // forty-five seconds after the first touch, the run is over
    most: 16,                   // the shark's top speed: a move is at most this long, in units
    turn: 3,                    // a move differs from the one before by at most this, along each way
    bite: 60                    // a fish this near the shark's mouth is eaten, in units
  };

  // The fish swim in sixteenths of a unit, so a slow drift can go any way.
  var FINE = 16;

  // ---- A reef from a name ----------------------------------------------------

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

  // Each day draws its fish and its water from its seed, a third each:
  // one big shoal, little shoals, or lone darters; sunlit water, murky
  // water or a kelp forest.
  var FISH = ['one big shoal', 'little shoals', 'lone darters'];
  var WATERS = ['sunlit water', 'murky water', 'a kelp forest'];
  function kinds(track) {
    return { fish: hash(track + ':fish') % 3, water: hash(track + ':water') % 3 };
  }
  // What a day's reef is like, in words: 'murky water, little shoals'.
  function describe(track) {
    var k = kinds(track);
    return WATERS[k.water] + ', ' + FISH[k.fish];
  }

  // How each kind of fish swims, in sixteenths of a unit a step: at ease
  // (cruise, and how much its move may change in a step, calm), fleeing
  // (flee, and fast), and tired (tired); how far aside it darts from a
  // shark coming close and straight at it (dart, within dartNear units);
  // how near the shark may come before it flees, in units: nearStill to a
  // still shark, nearFast to one at full speed; and how many steps it can
  // flee at full speed before it tires (puff).
  var SWIM = [
    { cruise: 40, calm: 6, flee: 128, fast: 16, tired: 56, dart: 60, dartNear: 140, nearStill: 90, nearFast: 220, puff: 45 },
    { cruise: 40, calm: 6, flee: 136, fast: 16, tired: 56, dart: 60, dartNear: 140, nearStill: 90, nearFast: 220, puff: 45 },
    { cruise: 44, calm: 7, flee: 150, fast: 20, tired: 64, dart: 90, dartNear: 150, nearStill: 90, nearFast: 200, puff: 60 }
  ];

  function whole(v) { return Math.floor(v); }

  // The whole part of a square root, found exactly: the first guess may be
  // off by one, and the check is in whole numbers.
  function isqrt(n) {
    if (n <= 0) return 0;
    var r = Math.floor(Math.sqrt(n));
    while (r * r > n) r--;
    while ((r + 1) * (r + 1) <= n) r++;
    return r;
  }
  // a / b, cut to a whole number towards zero.
  function div(a, b) {
    var q = a / b;
    return q < 0 ? Math.ceil(q) : Math.floor(q);
  }

  // The reef a track's name draws under a law (the one runs are made under
  // now, unless named): its shoals, each going round a loop of its own, and
  // its fish, each keeping its own place in its shoal (ox, oy, in units).
  function sea(track, version) {
    version = version || VERSION;
    var k = kinds(track), r = random(hash(track));
    var shoals = [], list = [], i, j;
    // A shoal's loop: four or five points round the water, gone round once
    // in twenty to forty seconds, starting anywhere on it, either way.
    function loop(top) {
      var n = 4 + whole(r() * 2), pts = [], cx = 250 + whole(r() * 500), cy = top + whole(r() * (1000 - top));
      for (var p = 0; p < n; p++) {
        pts.push([Math.min(EDGE.x1 - 120, Math.max(EDGE.x0 + 120, cx + whole((r() - 0.5) * 640))),
          Math.min(EDGE.y1 - 160, Math.max(EDGE.y0 + 140, cy + whole((r() - 0.5) * 620)))]);
      }
      if (r() < 0.5) pts.reverse();
      return { pts: pts, period: 1200 + whole(r() * 1200), phase: whole(r() * 2400) };
    }
    // A place in a shoal: within a ring of the given size, never two fish
    // closer than a fish's length.
    function places(n, size) {
      var out = [];
      for (var tries = 0; out.length < n && tries < 400; tries++) {
        var x = whole((r() * 2 - 1) * size), y = whole((r() * 2 - 1) * size * 0.7), ok = x * x + y * y * 2 <= size * size;
        for (var q = 0; q < out.length && ok; q++) {
          var dx = out[q][0] - x, dy = out[q][1] - y;
          if (dx * dx + dy * dy < 34 * 34) ok = false;
        }
        if (ok) out.push([x, y]);
      }
      return out;
    }
    if (k.fish === 0) {
      // One big shoal of twenty-eight to thirty-two.
      shoals.push(loop(200));
      places(28 + whole(r() * 5), 170).forEach(function (o) { list.push({ shoal: 0, ox: o[0], oy: o[1] }); });
    } else if (k.fish === 1) {
      // Four little shoals of six to eight.
      for (i = 0; i < 4; i++) {
        shoals.push(loop(120 + i * 140));
        places(6 + whole(r() * 3), 90).forEach(function (o) { list.push({ shoal: i, ox: o[0], oy: o[1] }); });
      }
    } else {
      // Fourteen to eighteen lone fish, each going round a loop of its own.
      var lone = 14 + whole(r() * 5);
      for (i = 0; i < lone; i++) {
        shoals.push(loop(100 + whole(r() * 700)));
        list.push({ shoal: i, ox: 0, oy: 0 });
      }
    }
    for (j = 0; j < list.length; j++) list[j].n = j;
    return {
      track: track, version: version, fish: k.fish, water: k.water,
      swim: SWIM[k.fish], shoals: shoals, n: list.length, list: list,
      start: { x: 500, y: 1440 }
    };
  }

  // Where shoal k's middle is at step s, along a closed uniform cubic
  // B-spline round its loop's points, in whole units. s may be fractional
  // for drawing; a run only asks at whole steps.
  function shoalAt(sk, k, s) {
    var sh = sk.shoals[k], p = sh.pts, m = p.length;
    var turn = (s + sh.phase) % sh.period;
    if (turn < 0) turn += sh.period;
    var f = turn * m / sh.period, i = Math.floor(f), t = f - i;
    var t2 = t * t, t3 = t2 * t, u = 1 - t;
    var b0 = u * u * u / 6, b1 = (3 * t3 - 6 * t2 + 4) / 6, b2 = (-3 * t3 + 3 * t2 + 3 * t + 1) / 6, b3 = t3 / 6;
    var p0 = p[(i + m - 1) % m], p1 = p[i % m], p2 = p[(i + 1) % m], p3 = p[(i + 2) % m];
    return {
      x: Math.round(b0 * p0[0] + b1 * p1[0] + b2 * p2[0] + b3 * p3[0]),
      y: Math.round(b0 * p0[1] + b1 * p1[1] + b2 * p2[1] + b3 * p3[1])
    };
  }

  // Where fish b would be at ease at step s: its shoal's middle and its
  // own place in the shoal, kept inside the water, in sixteenths. mid may
  // hold the shoals' middles at s, found once for every fish.
  function home(sk, b, s, mid) {
    var f = sk.list[b], c = mid ? mid[f.shoal] || (mid[f.shoal] = shoalAt(sk, f.shoal, s)) : shoalAt(sk, f.shoal, s);
    var x = Math.min(EDGE.x1 - 10, Math.max(EDGE.x0 + 10, c.x + f.ox));
    var y = Math.min(EDGE.y1 - 10, Math.max(EDGE.y0 + 10, c.y + f.oy));
    return { x: x * FINE, y: y * FINE };
  }

  // ---- A run -----------------------------------------------------------------

  // status: 'running' until it is 'finished' (every fish eaten) or 'over'
  // (the time ran out). x, y is the shark's mouth and vx, vy its last move,
  // in units; fx, fy where each fish is and fvx, fvy its last move, in
  // sixteenths, and tire how long it has been fleeing; gone marks the fish
  // eaten, and ate the ones eaten on the last step.
  function start(sk) {
    var st = {
      step: 0, x: sk.start.x, y: sk.start.y, vx: 0, vy: 0, eaten: 0, ate: [], status: 'running',
      fx: new Int32Array(sk.n), fy: new Int32Array(sk.n), fvx: new Int32Array(sk.n), fvy: new Int32Array(sk.n),
      tire: new Int32Array(sk.n), gone: new Uint8Array(sk.n)
    };
    for (var b = 0; b < sk.n; b++) {
      var h = home(sk, b, 0);
      st.fx[b] = h.x;
      st.fy[b] = h.y;
    }
    return st;
  }

  // A move as the shark can make it from where it is: whole, within its
  // turn of the move before along each way, and no longer than its top
  // speed (the longer way given up a unit at a time until it isn't).
  function bound(st, qx, qy) {
    qx = Math.round(+qx || 0);
    qy = Math.round(+qy || 0);
    var t = LAW.turn, m = LAW.most;
    qx = qx > st.vx + t ? st.vx + t : qx < st.vx - t ? st.vx - t : qx;
    qy = qy > st.vy + t ? st.vy + t : qy < st.vy - t ? st.vy - t : qy;
    while (qx * qx + qy * qy > m * m) {
      if (Math.abs(qx) >= Math.abs(qy)) qx += qx > 0 ? -1 : 1;
      else qy += qy > 0 ? -1 : 1;
    }
    return [qx, qy];
  }

  function clamp(v, lo, hi) { return v < lo ? lo : v > hi ? hi : v; }

  // One step: the shark makes its move (kept inside the water), then every
  // fish still swimming takes its way, then any fish within the bite of
  // the shark's mouth is eaten. A fish that flees tires: after puff steps
  // of it, it slows to tired over as many steps again; at ease it gets its
  // breath back twice as fast.
  function step(st, qx, qy, sk) {
    st.ate = [];
    if (st.status !== 'running') return st;
    var q = bound(st, qx, qy);
    st.vx = q[0];
    st.vy = q[1];
    st.x = clamp(st.x + q[0], EDGE.x0, EDGE.x1);
    st.y = clamp(st.y + q[1], EDGE.y0, EDGE.y1);
    st.step++;
    var sw = sk.swim, SX = st.x * FINE, SY = st.y * FINE;
    var speed = isqrt(st.vx * st.vx + st.vy * st.vy);
    // How near the shark may come before a fish flees: further the faster
    // it swims, so a shark that creeps can come close.
    var near = (sw.nearStill + div((sw.nearFast - sw.nearStill) * speed, LAW.most)) * FINE;
    var bite = LAW.bite * FINE;
    var lo = { x: EDGE.x0 * FINE, y: EDGE.y0 * FINE }, hi = { x: EDGE.x1 * FINE, y: EDGE.y1 * FINE }, mid = [];
    for (var b = 0; b < sk.n; b++) {
      if (st.gone[b]) continue;
      var x = st.fx[b], y = st.fy[b], dx = x - SX, dy = y - SY, d2 = dx * dx + dy * dy;
      var wx, wy, most, acc;
      if (d2 < near * near) {
        // Fleeing: straight away from the shark's mouth, and when it is
        // close and coming straight at the fish (its way within an eighth
        // of a turn of the fish), aside too, to the side of its way the
        // fish is on.
        var d = isqrt(d2) || 1;
        wx = div(dx * sw.flee, d);
        wy = div(dy * sw.flee, d);
        var ahead = st.vx * dx + st.vy * dy, side = st.vx * dy - st.vy * dx;
        if (speed > 0 && d < sw.dartNear * FINE && ahead > 0 && Math.abs(side) <= ahead) {
          if (side === 0) side = b % 2 ? 1 : -1;
          var px = side > 0 ? -st.vy : st.vy, py = side > 0 ? st.vx : -st.vx;
          wx += div(px * sw.dart, speed);
          wy += div(py * sw.dart, speed);
        }
        var t = st.tire[b] < 2 * sw.puff ? ++st.tire[b] : st.tire[b];
        most = t <= sw.puff ? sw.flee : sw.flee - div((sw.flee - sw.tired) * (t - sw.puff), sw.puff);
        acc = sw.fast;
      } else {
        // At ease: back towards its place in its shoal, slowing as it nears it.
        var h = home(sk, b, st.step, mid);
        wx = div(h.x - x, 8);
        wy = div(h.y - y, 8);
        most = sw.cruise;
        acc = sw.calm;
        st.tire[b] = st.tire[b] > 2 ? st.tire[b] - 2 : 0;
      }
      var w2 = wx * wx + wy * wy;
      if (w2 > most * most) {
        var wl = isqrt(w2);
        wx = div(wx * most, wl);
        wy = div(wy * most, wl);
      }
      var vx = st.fvx[b] + clamp(wx - st.fvx[b], -acc, acc), vy = st.fvy[b] + clamp(wy - st.fvy[b], -acc, acc);
      x += vx;
      y += vy;
      // At the water's edge a fish goes along it.
      if (x < lo.x) { x = lo.x; if (vx < 0) vx = 0; }
      if (x > hi.x) { x = hi.x; if (vx > 0) vx = 0; }
      if (y < lo.y) { y = lo.y; if (vy < 0) vy = 0; }
      if (y > hi.y) { y = hi.y; if (vy > 0) vy = 0; }
      st.fx[b] = x;
      st.fy[b] = y;
      st.fvx[b] = vx;
      st.fvy[b] = vy;
      var ex = x - SX, ey = y - SY;
      if (ex * ex + ey * ey <= bite * bite) {
        st.gone[b] = 1;
        st.eaten++;
        st.ate.push(b);
      }
    }
    if (st.eaten === sk.n) st.status = 'finished';
    else if (st.step >= LAW.steps) st.status = 'over';
    return st;
  }

  // How far the run has gone, from 0 at the first touch to 1 when the time
  // is up.
  function progress(st) {
    return st.step / LAW.steps;
  }

  // ---- A run kept: every step's move -----------------------------------------
  // "sr1.<track>.<eaten>.<steps>.<moves>": each step's move as how it
  // differs from the move before (the first from standing still), along
  // each way between -3 and 3 (the shark's turn), made one number in
  // zigzag, 7 times the first and the second, as a varint; a run of steps
  // with no change is written 0 and how many more steps it lasted. The
  // bytes are in base64url.

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
  function zig(q) { return q >= 0 ? 2 * q : -2 * q - 1; }
  function zag(n) { return n % 2 ? -(n + 1) / 2 : n / 2; }
  var SPAN = 2 * LAW.turn + 1;

  // moves: one [qx, qy] for each step of the run, as bound made them.
  function encode(track, moves, eaten, steps) {
    var bytes = [], px = 0, py = 0, i, codes = [];
    for (i = 0; i < moves.length; i++) {
      var dx = clamp(moves[i][0] - px, -LAW.turn, LAW.turn), dy = clamp(moves[i][1] - py, -LAW.turn, LAW.turn);
      codes.push(zig(dx) * SPAN + zig(dy));
      px += dx;
      py += dy;
    }
    for (i = 0; i < codes.length; i++) {
      if (codes[i]) { put(bytes, codes[i]); continue; }
      var same = 1;
      while (i + same < codes.length && !codes[i + same]) same++;
      put(bytes, 0);
      put(bytes, same - 1);
      i += same - 1;
    }
    return [VERSION, track, eaten, steps, toB64(bytes)].join('.');
  }

  // A kept run's parts, or null when it isn't one this engine can re-run.
  function decode(replay) {
    var parts = typeof replay === 'string' ? replay.split('.') : [];
    if (parts.length !== 5 || VERSIONS.indexOf(parts[0]) < 0 || !validTrack(parts[1]) ||
        !/^\d{1,3}$/.test(parts[2]) || !/^\d{1,5}$/.test(parts[3])) return null;
    var steps = +parts[3];
    if (steps < 1 || steps > LAW.steps) return null;
    var bytes = fromB64(parts[4]);
    if (!bytes) return null;
    var nums = [], n = 0, mul = 1;
    for (var i = 0; i < bytes.length; i++) {
      n += (bytes[i] & 127) * mul;
      if (bytes[i] & 128) { mul *= 128; continue; }
      nums.push(n);
      n = 0;
      mul = 1;
    }
    if (mul !== 1) return null;
    var moves = [], px = 0, py = 0;
    for (i = 0; i < nums.length; i++) {
      var c = nums[i];
      if (c >= SPAN * SPAN) return null;
      if (c) {
        px += zag(Math.floor(c / SPAN));
        py += zag(c % SPAN);
        moves.push([px, py]);
        continue;
      }
      if (nums[i + 1] == null) return null;
      for (var k = 0; k <= nums[i + 1] && moves.length <= steps; k++) moves.push([px, py]);
      i++;
    }
    if (moves.length !== steps) return null;
    return { version: parts[0], track: parts[1], eaten: +parts[2], steps: steps, moves: moves };
  }

  // Run a kept run again in its reef, drawn under the run's own law. It is
  // good when every move is one the shark could make, and it ends on its
  // very last step, every fish eaten or the time up, having eaten as many
  // as it says. x and y hold where the shark's mouth was after each step
  // (index 0 is the start), eatenAt the step each fish was eaten at (or
  // Infinity), and sea the reef it swam in.
  function rerun(replay, sk) {
    var r = decode(replay);
    if (!r) return null;
    sk = sk && sk.track === r.track && sk.version === r.version ? sk : sea(r.track, r.version);
    var st = start(sk), x = [st.x], y = [st.y], eatenAt = [], good = true;
    for (var b = 0; b < sk.n; b++) eatenAt.push(Infinity);
    for (var k = 0; k < r.moves.length && st.status === 'running'; k++) {
      var q = bound(st, r.moves[k][0], r.moves[k][1]);
      if (q[0] !== r.moves[k][0] || q[1] !== r.moves[k][1]) { good = false; break; }
      step(st, q[0], q[1], sk);
      for (var a = 0; a < st.ate.length; a++) eatenAt[st.ate[a]] = st.step;
      x.push(st.x);
      y.push(st.y);
    }
    return {
      ok: good && st.status !== 'running' && st.step === r.steps && st.eaten === r.eaten,
      version: r.version, track: r.track, eaten: st.eaten, steps: st.step, total: sk.n,
      status: st.status, x: x, y: y, eatenAt: eatenAt, sea: sk
    };
  }

  // Whether run a did better than run b: more fish, then sooner.
  function better(a, b) {
    return !b || a.eaten > b.eaten || (a.eaten === b.eaten && a.steps < b.steps);
  }

  // The time a run took, from its steps, as the page shows it.
  function seconds(steps) {
    return (steps / RATE).toFixed(2);
  }

  // A run in words, as the page shows it: its time when it ate every fish,
  // else how many it ate.
  function fish(n) {
    return n + ' fish';
  }
  function words(r) {
    return r.eaten >= r.total ? seconds(r.steps) + ' s' : fish(r.eaten);
  }

  var api = {
    VERSION: VERSION,
    RATE: RATE,
    W: W,
    H: H,
    EDGE: EDGE,
    LAW: LAW,
    FINE: FINE,
    validTrack: validTrack,
    describe: describe,
    sea: sea,
    shoalAt: shoalAt,
    start: start,
    bound: bound,
    step: step,
    progress: progress,
    encode: encode,
    decode: decode,
    rerun: rerun,
    better: better,
    seconds: seconds,
    fish: fish,
    words: words
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.SharkReef = api;
})(this);
