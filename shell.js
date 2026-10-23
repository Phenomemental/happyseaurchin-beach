/* Sundial's listening shell (the plan at spine:front-door 6.8): the one
   shell a filled pool offers, drawn in ink and tinted with the pool's hue,
   and the sea that sounds in it while it is held to the ear. Never named.

   Three forms, unlike the small fans on the tideline: a nautilus (a
   logarithmic spiral with its chambers), a whelk (a spindle with its
   whorls), and a moon snail (a round shell turning inward). Each pool has
   its own form. The sea is made by the browser itself (Web Audio: soft
   noise, filtered and slowly swelling), each pool's water its own and
   never the same twice, with wind, a breaking wave and far seabirds where
   6.8 gives them; it starts only on the person's own tap and fades when
   the shell is put back. No sound files, no library. */
(function (root) {
  var NS = 'http://www.w3.org/2000/svg';
  // By pool, Facts to Outcomes.
  var FORMS = ['whelk', 'nautilus', 'moon', 'nautilus', 'whelk', 'moon'];

  function f(n) { return n.toFixed(1); }
  function spiral(a, b, from, to, turn) {
    var pts = [];
    for (var t = from; t <= to + 1e-9; t += 0.08) {
      var r = a * Math.exp(b * t);
      pts.push(f(r * Math.cos(t + turn)) + ',' + f(r * Math.sin(t + turn)));
    }
    return pts;
  }

  // Each form as {body, lines}: the body is filled with the hue, the lines
  // are drawn over it in ink. All within -60..60.
  function at(r, t) { return f(r * Math.cos(t)) + ',' + f(r * Math.sin(t)); }
  // A nautilus: one wide turn of a fast spiral, closed at its opening, with
  // the stripes of the older shell and the inner coil seen in the mouth.
  function nautilus() {
    var b = 0.2, end = 4 * Math.PI, a = 54 / Math.exp(b * end), turn = 0.5;
    var body = 'M ' + spiral(a, b, end - 2 * Math.PI, end, turn).join(' L ') + ' Z';
    var lines = ['M ' + spiral(a, b, end - 3 * Math.PI, end - 2 * Math.PI, turn).join(' L ')];
    for (var k = 0; k < 6; k++) {
      var t = end - 2 * Math.PI + 0.5 + k * 0.62, ro = a * Math.exp(b * t), ri = a * Math.exp(b * (t - 2 * Math.PI));
      lines.push('M ' + at(ri + (ro - ri) * 0.15, t + turn) + ' Q ' + at(ri + (ro - ri) * 0.6, t + turn + 0.28) + ' ' + at(ro - 1.5, t + turn + 0.18));
    }
    return { body: body, lines: lines };
  }
  // A whelk: whorls stacked to a point, the wide last whorl with its
  // opening, and the canal beneath.
  function whelk() {
    var body = 'M 0,-57 Q 9,-51 8,-45 Q 17,-39 14,-31 Q 25,-23 21,-13 Q 35,-3 31,13 Q 28,31 12,41 Q 8,49 6,58 ' +
      'L 2,58 Q 0,49 -4,43 Q -27,33 -28,13 Q -30,-3 -19,-13 Q -23,-23 -12,-31 Q -15,-39 -7,-45 Q -9,-51 0,-57 Z';
    var lines = [
      'M -7,-45 Q 0,-42 8,-45', 'M -12,-31 Q 1,-26 14,-31', 'M -19,-13 Q 2,-6 21,-13',
      'M -26,2 Q -2,10 24,1', 'M -27,20 Q -12,26 -2,24',
      'M 5,-1 Q 27,5 22,25 Q 16,37 6,35 Q 1,20 5,-1 Z'
    ];
    return { body: body, lines: lines };
  }
  // A moon snail: round, turning inward, its opening a crescent at the end
  // of the turn.
  function moon() {
    var b = 0.12, end = 5 * Math.PI, a = 52 / Math.exp(b * end), turn = 2.2;
    var body = 'M ' + spiral(a, b, end - 2 * Math.PI, end, turn).join(' L ') + ' Z';
    var ro = a * Math.exp(b * end), ri = a * Math.exp(b * (end - 2 * Math.PI)), e = end + turn;
    var lines = ['M ' + spiral(a, b, 1.2, end - 2 * Math.PI, turn).join(' L '),
      'M ' + at(ri + 2, e - 0.05) + ' Q ' + at((ri + ro) / 2, e - 0.75) + ' ' + at(ro - 3, e - 0.08) +
        ' Q ' + at((ri + ro) / 2 + 3, e - 0.35) + ' ' + at(ri + 2, e - 0.05) + ' Z'];
    return { body: body, lines: lines };
  }
  var MAKE = { nautilus: nautilus, whelk: whelk, moon: moon };

  // Draw pool i's shell into an <svg> (viewBox -60 -60 120 120).
  function draw(svg, i, hue) {
    while (svg.firstChild) svg.removeChild(svg.firstChild);
    var form = MAKE[FORMS[i] || 'nautilus'](), g = document.createElementNS(NS, 'g');
    g.setAttribute('class', 'sd-listen-shell');
    var body = document.createElementNS(NS, 'path');
    body.setAttribute('d', form.body);
    body.setAttribute('fill', hue);
    body.setAttribute('fill-opacity', '0.38');
    body.setAttribute('stroke', 'var(--ink)');
    body.setAttribute('stroke-width', '1.6');
    body.setAttribute('stroke-linejoin', 'round');
    g.appendChild(body);
    form.lines.forEach(function (d) {
      var l = document.createElementNS(NS, 'path');
      l.setAttribute('d', d);
      l.setAttribute('fill', 'none');
      l.setAttribute('stroke', 'var(--ink)');
      l.setAttribute('stroke-width', '1');
      l.setAttribute('stroke-opacity', '0.7');
      l.setAttribute('stroke-linecap', 'round');
      g.appendChild(l);
    });
    svg.appendChild(g);
    // Centre the drawing on its own outline, square, with room to tilt.
    try {
      var bb = g.getBBox(), size = Math.max(bb.width, bb.height) + 16;
      if (bb.width) svg.setAttribute('viewBox', [bb.x + bb.width / 2 - size / 2, bb.y + bb.height / 2 - size / 2, size, size].map(f).join(' '));
    } catch (e) { /* not drawn yet: the default view stands */ }
    return FORMS[i];
  }

  // ---- The sea in the shell (6.8, amended 23 October 2026) ---------------------
  // Never the same twice. Each pool has its own water; the part of the day
  // softens it at dawn and in the night; each lift draws its own swells and
  // gusts; by day, now and then, far off, a seabird calls. All of it made by
  // the browser itself (Web Audio), scheduled ahead on the audio clock.
  //   band, q     where the water sounds, and how wide (a bandpass on soft noise)
  //   swell       the swell's period in seconds; depth, how much it rises
  //   drift       how unsteady the swell is, and how much the water shifts
  //   wind        gusts across the water, 0 to 1
  //   breaks      a wave that gathers and breaks
  //   far         heard from a distance: duller, quieter
  //   near        louder, closer
  var WATERS = [
    { band: 520, q: 0.7, swell: 9, depth: 0.3, drift: 0.05, wind: 0.1, breaks: false, far: false, near: 1 },   // what happened: an even swell
    { band: 300, q: 0.6, swell: 13, depth: 0.45, drift: 0.2, wind: 0.05, breaks: false, far: false, near: 1.2 }, // what you felt: deeper and nearer
    { band: 580, q: 0.8, swell: 7, depth: 0.35, drift: 0.5, wind: 0.6, breaks: false, far: false, near: 1 },   // what you made of it: shifting, wind across it
    { band: 420, q: 0.3, swell: 16, depth: 0.2, drift: 0.1, wind: 0.1, breaks: false, far: false, near: 1 },   // what matters: wide and calm
    { band: 480, q: 0.6, swell: 10, depth: 0.2, drift: 0.2, wind: 0.1, breaks: true, far: false, near: 1 },    // a choice: a wave gathers and breaks
    { band: 380, q: 0.9, swell: 12, depth: 0.3, drift: 0.3, wind: 0.85, breaks: false, far: true, near: 0.75 } // what's ahead: far off, in the wind
  ];
  var SOFT = { dawn: 1, night: 1, 'deep night': 1 };
  var NIGHT = { night: 1, 'deep night': 1 };
  var EDGE = { dawn: 1, evening: 1 };
  var AHEAD = 240;   // seconds of gusts, waves and calls laid out at each lift

  // This lift's water: the pool's, varied a little every time.
  function waterOf(i, part, rand) {
    var w = WATERS[i] || WATERS[0], r = rand || Math.random;
    function vary(x, by) { return x * (1 - by + 2 * by * r()); }
    return {
      band: vary(w.band, 0.12), q: vary(w.q, 0.2), swell: vary(w.swell, 0.2), depth: vary(w.depth, 0.2),
      drift: w.drift, wind: w.wind, breaks: w.breaks, far: w.far,
      level: 0.16 * w.near * (SOFT[part] ? 0.7 : 1) * vary(1, 0.08),
      birds: !NIGHT[part], rare: !!EDGE[part]
    };
  }
  // When the gusts, the breaking waves and the calls come in this lift, in
  // seconds from its start, up to `ahead`.
  function moments(water, rand, ahead) {
    var r = rand || Math.random, out = { gusts: [], waves: [], calls: [] }, t;
    // Each gust ends before the next begins; the windier, the closer.
    if (water.wind > 0.05) {
      for (t = 1 + 4 * r(); t < ahead;) {
        var g = { at: t, rise: 1.5 + 1.5 * r(), hold: 0.5 + 1.5 * r(), fall: 2 + 2 * r(), size: 0.5 + 0.5 * r() };
        out.gusts.push(g);
        t += g.rise + g.hold + g.fall + 0.3 + (1 + 6 * r()) * (1 - water.wind);
      }
    }
    if (water.breaks) {
      for (t = 2 + 4 * r(); t < ahead; t += 9 + 6 * r()) out.waves.push({ at: t, gather: 3 + 2 * r(), size: 0.7 + 0.3 * r() });
    }
    if (water.birds) {
      var KINDS = ['gull', 'kleep', 'curlew'];
      for (t = 2 + 7 * r(); t < ahead; t += (8 + 12 * r()) * (water.rare ? 1.8 : 1)) {
        out.calls.push({ at: t, kind: KINDS[Math.floor(r() * 3) % 3], pan: (r() * 2 - 1) * 0.7, notes: 2 + Math.floor(r() * 3), size: 0.6 + 0.4 * r() });
      }
    }
    return out;
  }

  function noise(ctx, seconds) {
    var len = Math.floor(ctx.sampleRate * seconds), buf = ctx.createBuffer(1, len, ctx.sampleRate), data = buf.getChannelData(0), last = 0;
    for (var k = 0; k < len; k++) {
      last = (last + 0.02 * (Math.random() * 2 - 1)) / 1.02;
      data[k] = last * 3.5;
    }
    return buf;
  }
  function sine(ctx, hz, amount, into, started) {
    var o = ctx.createOscillator(), g = ctx.createGain();
    o.frequency.value = hz;
    g.gain.value = amount;
    o.connect(g);
    g.connect(into);
    started.push(o);
    return o;
  }

  // One far seabird's call on a voice: a gull's falling cry, an
  // oystercatcher's high pips, or a curlew's rising whistle.
  function call(c, at, voice) {
    var f = voice.osc.frequency, g = voice.gain.gain, p = voice.pan, size = c.size;
    if (p && p.pan) p.pan.setValueAtTime(c.pan, at);
    for (var n = 0; n < c.notes; n++) {
      var s;
      if (c.kind === 'gull') {
        s = at + n * (0.45 + 0.1 * (n % 2));
        f.setValueAtTime(1050 - n * 40, s);
        f.linearRampToValueAtTime(1450 - n * 40, s + 0.07);
        f.exponentialRampToValueAtTime(780 - n * 30, s + 0.36);
        g.setValueAtTime(0, s);
        g.linearRampToValueAtTime(size, s + 0.04);
        g.linearRampToValueAtTime(size * 0.7, s + 0.25);
        g.linearRampToValueAtTime(0, s + 0.38);
      } else if (c.kind === 'kleep') {
        s = at + n * 0.17;
        f.setValueAtTime(2500, s);
        f.linearRampToValueAtTime(2950, s + 0.05);
        g.setValueAtTime(0, s);
        g.linearRampToValueAtTime(size * 0.8, s + 0.015);
        g.linearRampToValueAtTime(0, s + 0.09);
      } else {
        s = at + n * 1.0;
        f.setValueAtTime(1500 + n * 60, s);
        f.linearRampToValueAtTime(2300 + n * 120, s + 0.7);
        g.setValueAtTime(0, s);
        g.linearRampToValueAtTime(size * 0.45, s + 0.15);
        g.linearRampToValueAtTime(size * 0.3, s + 0.55);
        g.linearRampToValueAtTime(0, s + 0.75);
      }
    }
  }

  // The whole sea, built on any audio context (an offline one too), from
  // `water` and its `times`, starting at `start`. Returns {master, started}.
  function build(ctx, water, times, start) {
    var started = [], t0 = start || ctx.currentTime;
    var master = ctx.createGain();
    master.gain.setValueAtTime(0, t0);
    master.gain.linearRampToValueAtTime(water.level, t0 + 1.8);
    master.connect(ctx.destination);
    var buf = noise(ctx, 6);

    // The water: noise through its band, swelling by two slow waves at
    // unrelated periods, so it never quite repeats; the band itself drifts.
    var src = ctx.createBufferSource(), band = ctx.createBiquadFilter(), swell = ctx.createGain(), dull = ctx.createBiquadFilter();
    src.buffer = buf;
    src.loop = true;
    src.loopStart = Math.random() * 3;
    band.type = 'bandpass';
    band.frequency.value = water.band;
    band.Q.value = water.q;
    swell.gain.value = 0.6;
    dull.type = 'lowpass';
    dull.frequency.value = water.far ? 650 : 6000;
    src.connect(band);
    band.connect(swell);
    swell.connect(dull);
    dull.connect(master);
    // Two swells together stay under the water's own 0.6, so it never
    // turns over.
    var depth = Math.min(water.depth, 0.4);
    sine(ctx, 1 / water.swell, depth, swell.gain, started);
    sine(ctx, 1 / (water.swell * 2.7), depth * (0.2 + 0.5 * water.drift), swell.gain, started);
    sine(ctx, 1 / (water.swell * 1.9), water.band * 0.25 * water.drift, band.frequency, started);
    started.push(src);

    // The wind: the same noise, higher and thinner, in gusts that sweep.
    if (times.gusts.length) {
      var wsrc = ctx.createBufferSource(), wband = ctx.createBiquadFilter(), wind = ctx.createGain();
      wsrc.buffer = buf;
      wsrc.loop = true;
      wband.type = 'bandpass';
      wband.Q.value = 1.2;
      wband.frequency.value = 1300;
      wind.gain.setValueAtTime(water.wind * 0.12, t0);
      wsrc.connect(wband);
      wband.connect(wind);
      wind.connect(master);
      times.gusts.forEach(function (g) {
        var a = t0 + g.at, peak = water.wind * 0.12 + water.wind * 0.5 * g.size;
        wind.gain.setValueAtTime(water.wind * 0.12, a);
        wind.gain.linearRampToValueAtTime(peak, a + g.rise);
        wind.gain.setValueAtTime(peak, a + g.rise + g.hold);
        wind.gain.linearRampToValueAtTime(water.wind * 0.12, a + g.rise + g.hold + g.fall);
        wband.frequency.setValueAtTime(1100, a);
        wband.frequency.linearRampToValueAtTime(1700 + 700 * g.size, a + g.rise);
        wband.frequency.linearRampToValueAtTime(1200, a + g.rise + g.hold + g.fall);
      });
      started.push(wsrc);
    }

    // A wave that gathers and breaks: a wider rush, rising, then spilling.
    if (times.waves.length) {
      var bsrc = ctx.createBufferSource(), bband = ctx.createBiquadFilter(), crash = ctx.createGain();
      bsrc.buffer = buf;
      bsrc.loop = true;
      bband.type = 'bandpass';
      bband.Q.value = 0.4;
      bband.frequency.value = 900;
      crash.gain.setValueAtTime(0, t0);
      bsrc.connect(bband);
      bband.connect(crash);
      crash.connect(master);
      times.waves.forEach(function (w) {
        var a = t0 + w.at, top = a + w.gather;
        crash.gain.setValueAtTime(0, a);
        crash.gain.linearRampToValueAtTime(0.35 * w.size, top);
        crash.gain.linearRampToValueAtTime(0.9 * w.size, top + 0.25);
        crash.gain.setTargetAtTime(0, top + 0.3, 0.9);
        bband.frequency.setValueAtTime(700, a);
        bband.frequency.linearRampToValueAtTime(1600, top + 0.25);
        bband.frequency.linearRampToValueAtTime(800, top + 3);
      });
      started.push(bsrc);
    }

    // Seabirds, far off: three voices, each quiet until it calls, softened
    // by distance and a faint echo off the water.
    if (times.calls.length) {
      var far = ctx.createBiquadFilter(), echo = ctx.createDelay(1), back = ctx.createGain(), birds = ctx.createGain();
      far.type = 'lowpass';
      far.frequency.value = 3200;
      echo.delayTime.value = 0.21;
      back.gain.value = 0.22;
      birds.gain.value = 0.16;
      far.connect(birds);
      far.connect(echo);
      echo.connect(back);
      back.connect(echo);
      back.connect(birds);
      birds.connect(master);
      var voices = {};
      ['gull', 'kleep', 'curlew'].forEach(function (kind) {
        var osc = ctx.createOscillator(), gain = ctx.createGain(), pan = ctx.createStereoPanner ? ctx.createStereoPanner() : null;
        osc.type = kind === 'gull' ? 'sawtooth' : kind === 'kleep' ? 'triangle' : 'sine';
        gain.gain.setValueAtTime(0, t0);
        osc.connect(gain);
        if (pan) { gain.connect(pan); pan.connect(far); } else gain.connect(far);
        if (kind === 'curlew') sine(ctx, 11, 28, osc.frequency, started);
        voices[kind] = { osc: osc, gain: gain, pan: pan };
        started.push(osc);
      });
      times.calls.forEach(function (c) { call(c, t0 + c.at, voices[c.kind]); });
    }

    started.forEach(function (n) { n.start(t0); });
    return { master: master, started: started };
  }

  var sea = null;
  // Hold the shell to the ear: pool i's water, at this part of the day.
  function listen(opts) {
    var AC = root.AudioContext || root.webkitAudioContext;
    if (!AC) return false;
    opts = opts || {};
    try {
      if (sea) hush(0);
      var ctx = (sea && sea.ctx) || new AC();
      if (ctx.resume) ctx.resume();
      var water = waterOf(opts.pool || 0, opts.part || 'midday');
      var made = build(ctx, water, moments(water, Math.random, AHEAD), ctx.currentTime);
      sea = { ctx: ctx, master: made.master, started: made.started, water: water };
      return true;
    } catch (e) {
      sea = null;
      return false;
    }
  }
  // Let the sea go quiet over a breath (seconds), then stop.
  function hush(over) {
    if (!sea || !sea.started) return;
    var s = sea, t = s.ctx.currentTime, wait = over == null ? 1.6 : over;
    sea = { ctx: s.ctx };
    try {
      s.master.gain.cancelScheduledValues(t);
      s.master.gain.setValueAtTime(s.master.gain.value, t);
      s.master.gain.linearRampToValueAtTime(0, t + wait);
      setTimeout(function () {
        s.started.forEach(function (n) { try { n.stop(); } catch (e) { /* already stopped */ } });
        try { s.master.disconnect(); } catch (e) { /* gone */ }
      }, wait * 1000 + 100);
    } catch (e) { /* the context has gone */ }
  }
  function sounding() { return !!(sea && sea.started); }

  var api = { FORMS: FORMS, WATERS: WATERS, draw: draw, waterOf: waterOf, moments: moments, build: build,
    listen: listen, hush: hush, sounding: sounding };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Shell = api;
})(this);
