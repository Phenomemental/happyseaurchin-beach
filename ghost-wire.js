/* Ghost Wire's engine: the wire a track's name draws, one step of a run
   along it, and a run kept as the moves its player's finger made, which
   re-runs to the same run, step for step, in any browser. Nothing here
   touches the page; ghost-wire.html draws it and takes the input.

   A run is traced: the point moves as the finger does, from anywhere on
   the screen, and buzzes if it leaves the wire's walls. Each step's move
   arrives as whole numbers of a small unit, so the run depends only on
   whole numbers and + - * / over tables built once from the track's name,
   and two browsers re-running one ghost agree to the last bit. Drawing may
   use anything; the simulation never reads what drawing computes. */
(function (root) {
  var VERSION = 'gw1';
  var RATE = 60;                // steps a second
  var DS = 0.0025;              // spacing of the wire's tables, in wire units

  // The wire stands in a box one unit wide and HEIGHT tall, made for a
  // phone held upright; it starts at the foot and ends at the head.
  var HEIGHT = 1.6;

  // How a run moves (wire law gw1, until frozen by playing on phones).
  var LAW = {
    half: 0.045,                // from the wire's middle to its wall: room for a finger's play
    unit: 0.001,                // a move is counted in these, in wire units
    most: 20,                   // the most a move may be in a step, in units, along each way
    maxSteps: 180 * RATE        // three minutes, then the attempt ends
  };

  // ---- A wire from a name ----------------------------------------------------

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

  // Six points climbing the box, swinging side to side, mostly turning
  // back and now and then running on to the same side.
  function points(track) {
    var r = random(hash(track));
    var n = 6, out = [], side = r() < 0.5 ? -1 : 1;
    for (var i = 0; i < n; i++) {
      var y = (HEIGHT - 0.1) - i * ((HEIGHT - 0.2) / (n - 1));
      var x = 0.5;
      if (i > 0 && i < n - 1) {
        if (r() < 0.75) side = -side;
        x = 0.5 + side * (0.1 + 0.25 * r());
        y += (r() - 0.5) * 0.05;
      }
      out.push([x, y]);
    }
    return out;
  }

  // A uniform cubic B-spline over the points, drawn fine: it passes near
  // each point rather than through it, so every turn is a rounded bend,
  // never a hairpin. The ends are held by repeating the first and last.
  function curve(p) {
    var q = [p[0], p[0]].concat(p, [p[p.length - 1], p[p.length - 1]]);
    var out = [], per = 64;
    for (var i = 0; i < q.length - 3; i++) {
      var p0 = q[i], p1 = q[i + 1], p2 = q[i + 2], p3 = q[i + 3];
      for (var j = 0; j < per; j++) {
        var t = j / per, t2 = t * t, t3 = t2 * t, u = 1 - t;
        var b0 = u * u * u / 6, b1 = (3 * t3 - 6 * t2 + 4) / 6, b2 = (-3 * t3 + 3 * t2 + 3 * t + 1) / 6, b3 = t3 / 6;
        out.push([
          b0 * p0[0] + b1 * p1[0] + b2 * p2[0] + b3 * p3[0],
          b0 * p0[1] + b1 * p1[1] + b2 * p2[1] + b3 * p3[1]
        ]);
      }
    }
    out.push(p[p.length - 1].slice());
    return out;
  }

  // The wire as tables at equal steps along it: where each step is (x, y)
  // and the normal pointing to its left (nx, ny), for drawing its walls.
  function wire(track) {
    var fine = curve(points(track));
    var acc = [0];
    for (var i = 1; i < fine.length; i++) {
      var dx = fine[i][0] - fine[i - 1][0], dy = fine[i][1] - fine[i - 1][1];
      acc.push(acc[i - 1] + Math.sqrt(dx * dx + dy * dy));
    }
    var length = acc[acc.length - 1];
    var n = Math.floor(length / DS) + 1;
    var x = new Float64Array(n), y = new Float64Array(n);
    var at = 0;
    for (var k = 0; k < n; k++) {
      var s = k * DS;
      while (at < acc.length - 2 && acc[at + 1] < s) at++;
      var seg = acc[at + 1] - acc[at];
      var f = seg > 0 ? (s - acc[at]) / seg : 0;
      if (f > 1) f = 1;
      x[k] = fine[at][0] + (fine[at + 1][0] - fine[at][0]) * f;
      y[k] = fine[at][1] + (fine[at + 1][1] - fine[at][1]) * f;
    }
    var nx = new Float64Array(n), ny = new Float64Array(n);
    for (k = 0; k < n; k++) {
      var a = k > 0 ? k - 1 : 0, b = k < n - 1 ? k + 1 : n - 1;
      var tx = x[b] - x[a], ty = y[b] - y[a], tl = Math.sqrt(tx * tx + ty * ty);
      if (tl > 0) { nx[k] = -ty / tl; ny[k] = tx / tl; }
    }
    return { track: track, length: length, n: n, x: x, y: y, nx: nx, ny: ny };
  }

  // Where a point a distance s along the wire, d to its left, stands.
  function place(w, s, d) {
    var f = s / DS, i = Math.floor(f);
    if (i < 0) { i = 0; f = 0; }
    if (i >= w.n - 1) { i = w.n - 2; f = i + 1; }
    var t = f - i;
    var x = w.x[i] + (w.x[i + 1] - w.x[i]) * t, y = w.y[i] + (w.y[i + 1] - w.y[i]) * t;
    var nx = w.nx[i] + (w.nx[i + 1] - w.nx[i]) * t, ny = w.ny[i] + (w.ny[i + 1] - w.ny[i]) * t;
    return { x: x + nx * d, y: y + ny * d };
  }

  // ---- A run -----------------------------------------------------------------

  // status: 'running' until it 'finished', 'buzzed' (touched a wall) or
  // 'stalled' (ran out of time). i is the step of the wire nearest the point.
  function start(w) {
    return { step: 0, x: w.x[0], y: w.y[0], i: 0, status: 'running' };
  }

  // A move, made whole and no larger than the most a step allows.
  function bound(q) {
    q = Math.round(+q || 0);
    return q > LAW.most ? LAW.most : q < -LAW.most ? -LAW.most : q;
  }

  // One step: the point moves by (qx, qy) units. The nearest part of the
  // wire is looked for close to where it was last, so the point follows
  // the wire and can't jump to another part of it that passes nearby.
  // Past the wall it buzzes; across the far end, inside the walls, it's
  // finished.
  var LOOK = 30;
  function step(st, qx, qy, w) {
    if (st.status !== 'running') return st;
    st.x += bound(qx) * LAW.unit;
    st.y += bound(qy) * LAW.unit;
    var best = st.i, near = Infinity;
    var from = st.i > LOOK ? st.i - LOOK : 0, to = st.i + LOOK < w.n ? st.i + LOOK : w.n - 1;
    for (var j = from; j <= to; j++) {
      var dx = st.x - w.x[j], dy = st.y - w.y[j], d2 = dx * dx + dy * dy;
      if (d2 < near) { near = d2; best = j; }
    }
    st.i = best;
    st.step++;
    var e = w.n - 1;
    var past = (st.x - w.x[e]) * (w.x[e] - w.x[e - 1]) + (st.y - w.y[e]) * (w.y[e] - w.y[e - 1]);
    if (near > LAW.half * LAW.half) st.status = 'buzzed';
    else if (best >= e - 2 && past >= 0) st.status = 'finished';
    else if (st.step >= LAW.maxSteps) st.status = 'stalled';
    return st;
  }

  // How far along the wire the point is, from 0 at the start to 1 at the end.
  function progress(st, w) {
    return st.i / (w.n - 1);
  }

  // ---- A run kept: every step's move -----------------------------------------
  // "gw1.<track>.<steps>.<moves>": each step's move as two zigzag varints
  // (x, then y); a run of steps without a move is written 0, 0 and how many
  // more steps it lasted. The bytes are in base64url.

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

  // moves: one [qx, qy] for each step of the run.
  function encode(track, moves, steps) {
    var bytes = [];
    for (var i = 0; i < moves.length; i++) {
      var qx = bound(moves[i][0]), qy = bound(moves[i][1]);
      if (qx || qy) { put(bytes, zig(qx)); put(bytes, zig(qy)); continue; }
      var still = 1;
      while (i + still < moves.length && !bound(moves[i + still][0]) && !bound(moves[i + still][1])) still++;
      put(bytes, 0); put(bytes, 0); put(bytes, still - 1);
      i += still - 1;
    }
    return [VERSION, track, steps, toB64(bytes)].join('.');
  }

  // A kept run's parts, or null when it isn't one this engine can re-run.
  function decode(replay) {
    var parts = typeof replay === 'string' ? replay.split('.') : [];
    if (parts.length !== 4 || parts[0] !== VERSION || !validTrack(parts[1]) || !/^\d{1,7}$/.test(parts[2])) return null;
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
    if (mul !== 1) return null;
    var steps = +parts[2], moves = [];
    for (i = 0; i < nums.length; ) {
      var qx = zag(nums[i]), qy = zag(nums[i + 1]);
      if (nums[i + 1] == null) return null;
      i += 2;
      if (qx || qy) { moves.push([qx, qy]); continue; }
      if (nums[i] == null) return null;
      for (var k = 0; k <= nums[i] && moves.length <= steps; k++) moves.push([0, 0]);
      i++;
    }
    if (moves.length !== steps) return null;
    return { track: parts[1], steps: steps, moves: moves };
  }

  // Run a kept run again on its wire. It is good when it finishes on its
  // very last step; x and y hold where it stood after each step (index 0
  // is the start).
  function rerun(replay, w) {
    var r = decode(replay);
    if (!r) return null;
    w = w && w.track === r.track ? w : wire(r.track);
    var st = start(w), x = [st.x], y = [st.y];
    for (var k = 0; k < r.moves.length && st.status === 'running'; k++) {
      step(st, r.moves[k][0], r.moves[k][1], w);
      x.push(st.x);
      y.push(st.y);
    }
    return { ok: st.status === 'finished' && st.step === r.steps, track: r.track, steps: st.step, status: st.status, x: x, y: y };
  }

  // The time a run took, from its steps, as the page shows it.
  function seconds(steps) {
    return (steps / RATE).toFixed(2);
  }

  var api = {
    VERSION: VERSION,
    RATE: RATE,
    HEIGHT: HEIGHT,
    LAW: LAW,
    validTrack: validTrack,
    wire: wire,
    place: place,
    start: start,
    step: step,
    progress: progress,
    bound: bound,
    encode: encode,
    decode: decode,
    rerun: rerun,
    seconds: seconds
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.GhostWire = api;
})(this);
