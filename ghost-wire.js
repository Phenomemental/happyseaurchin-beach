/* Ghost Wire's engine: the wire a track's name draws, one step of a run
   along it, and a run kept as the moments its player pressed and let go,
   which re-runs to the same run, step for step, in any browser. Nothing
   here touches the page; ghost-wire.html draws it and takes the input.

   Everything a run depends on uses only + - * / and Math.sqrt, over tables
   built once from the track's name, so two browsers re-running one ghost
   agree to the last bit. Drawing may use anything; the simulation never
   reads what drawing computes. */
(function (root) {
  var VERSION = 'gw1';
  var RATE = 120;               // steps a second
  var DT = 1 / RATE;
  var DS = 0.0025;              // spacing of the wire's tables, in wire units

  // The wire stands in a box one unit wide and HEIGHT tall, made for a
  // phone held upright; it starts at the foot and ends at the head.
  var HEIGHT = 1.6;

  // How a run moves (wire law gw1, until frozen by playing on phones).
  var LAW = {
    accel: 0.25,                // speed gained a second while held
    brake: 0.8,                 // speed lost a second once let go
    vmax: 0.3,                  // top speed, wire units a second
    swing: 0.039,               // how far a bend at speed pushes the point out
    settle: 10,                 // how quickly the point follows that push
    half: 0.014,                // from the wire's middle to its wall
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

  // Nine points climbing the box, swinging side to side, mostly turning
  // back and now and then running on to the same side.
  function points(track) {
    var r = random(hash(track));
    var n = 9, out = [], side = r() < 0.5 ? -1 : 1;
    for (var i = 0; i < n; i++) {
      var y = (HEIGHT - 0.1) - i * ((HEIGHT - 0.2) / (n - 1));
      var x = 0.5;
      if (i > 0 && i < n - 1) {
        if (r() < 0.75) side = -side;
        x = 0.5 + side * (0.1 + 0.27 * r());
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

  // The wire as tables at equal steps along it: where each step is (x, y),
  // the normal pointing to its left (nx, ny), and how sharply it bends
  // there (k, positive bending left), smoothed so a bend is felt as one.
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
    var nx = new Float64Array(n), ny = new Float64Array(n), raw = new Float64Array(n);
    for (k = 0; k < n; k++) {
      var a = k > 0 ? k - 1 : 0, b = k < n - 1 ? k + 1 : n - 1;
      var tx = x[b] - x[a], ty = y[b] - y[a], tl = Math.sqrt(tx * tx + ty * ty);
      if (tl > 0) { nx[k] = -ty / tl; ny[k] = tx / tl; }
      if (k > 0 && k < n - 1) {
        var ax = x[k] - x[k - 1], ay = y[k] - y[k - 1], bx = x[k + 1] - x[k], by = y[k + 1] - y[k];
        var la = Math.sqrt(ax * ax + ay * ay), lb = Math.sqrt(bx * bx + by * by);
        if (la > 0 && lb > 0) raw[k] = (ax * by - ay * bx) / (la * lb * DS);
      }
    }
    var k1 = smooth(smooth(raw, 6), 6);
    return { track: track, length: length, n: n, x: x, y: y, nx: nx, ny: ny, k: k1 };
  }

  function smooth(v, r) {
    var n = v.length, out = new Float64Array(n);
    for (var i = 0; i < n; i++) {
      var sum = 0, c = 0;
      for (var j = i - r; j <= i + r; j++) {
        if (j >= 0 && j < n) { sum += v[j]; c++; }
      }
      out[i] = sum / c;
    }
    return out;
  }

  // How sharply the wire bends a distance s along it.
  function bendAt(w, s) {
    var f = s / DS, i = Math.floor(f);
    if (i < 0) return w.k[0];
    if (i >= w.n - 1) return w.k[w.n - 1];
    return w.k[i] + (w.k[i + 1] - w.k[i]) * (f - i);
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

  // status: 'running' until it 'finished', 'buzzed' (touched the wall) or
  // 'stalled' (ran out of time).
  function start() {
    return { step: 0, s: 0, v: 0, d: 0, status: 'running' };
  }

  // One step, held or not. A bend taken at speed pushes the point out
  // from the bend, the more the faster; past the wall, it buzzes.
  function step(st, held, w) {
    if (st.status !== 'running') return st;
    var v = st.v + (held ? LAW.accel : -LAW.brake) * DT;
    if (v < 0) v = 0;
    if (v > LAW.vmax) v = LAW.vmax;
    var s = st.s + v * DT;
    var push = -LAW.swing * v * v * bendAt(w, s);
    var d = st.d + (push - st.d) * LAW.settle * DT;
    st.v = v;
    st.s = s;
    st.d = d;
    st.step++;
    if (d > LAW.half || d < -LAW.half) st.status = 'buzzed';
    else if (s >= w.length) st.status = 'finished';
    else if (st.step >= LAW.maxSteps) st.status = 'stalled';
    return st;
  }

  // ---- A run kept: the steps at which hold changed --------------------------
  // "gw1.<track>.<steps>.<changes>": the changes are the gaps between the
  // steps where hold changed, the first counted from step 0 (the run starts
  // released), each a varint, the bytes in base64url.

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
      val = (val << 6) | c;
      bits += 6;
      if (bits >= 8) {
        bits -= 8;
        out.push((val >> bits) & 255);
      }
    }
    return out;
  }

  function encode(track, changes, steps) {
    var bytes = [], last = 0;
    changes.forEach(function (at) {
      var gap = at - last;
      last = at;
      while (gap >= 128) { bytes.push((gap & 127) | 128); gap = Math.floor(gap / 128); }
      bytes.push(gap);
    });
    return [VERSION, track, steps, toB64(bytes)].join('.');
  }

  // A kept run's parts, or null when it isn't one this engine can re-run.
  function decode(replay) {
    var parts = typeof replay === 'string' ? replay.split('.') : [];
    if (parts.length !== 4 || parts[0] !== VERSION || !validTrack(parts[1]) || !/^\d{1,7}$/.test(parts[2])) return null;
    var bytes = fromB64(parts[3]);
    if (!bytes) return null;
    var changes = [], at = 0, gap = 0, mul = 1;
    for (var i = 0; i < bytes.length; i++) {
      gap += (bytes[i] & 127) * mul;
      if (bytes[i] & 128) { mul *= 128; continue; }
      at += gap;
      changes.push(at);
      gap = 0;
      mul = 1;
    }
    if (mul !== 1) return null;
    return { track: parts[1], steps: +parts[2], changes: changes };
  }

  // Run a kept run again on its wire. It is good when it finishes on the
  // very step it says it did; s and d hold where it stood after each step
  // (index 0 is the start).
  function rerun(replay, w) {
    var r = decode(replay);
    if (!r) return null;
    w = w && w.track === r.track ? w : wire(r.track);
    var st = start(), held = false, next = 0;
    var s = [0], d = [0];
    while (st.status === 'running') {
      while (next < r.changes.length && r.changes[next] === st.step) { held = !held; next++; }
      step(st, held, w);
      s.push(st.s);
      d.push(st.d);
    }
    return { ok: st.status === 'finished' && st.step === r.steps, track: r.track, steps: st.step, status: st.status, s: s, d: d };
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
    encode: encode,
    decode: decode,
    rerun: rerun,
    seconds: seconds
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.GhostWire = api;
})(this);
