/* Sundial's listening shell (the plan at spine:front-door 6.8): the one
   shell a filled pool offers, drawn in ink and tinted with the pool's hue,
   and the sea that sounds in it while it is held to the ear. Never named.

   Three forms, unlike the small fans on the tideline: a nautilus (a
   logarithmic spiral with its chambers), a whelk (a spindle with its
   whorls), and a moon snail (a round shell turning inward). Each pool has
   its own form. The sea is made by the browser itself (Web Audio: soft
   noise, filtered and slowly swelling); it starts only on the person's own
   tap and fades when the shell is put back. No sound files, no library. */
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

  // The sea in the shell: brown noise through a soft band, swelling slowly.
  var sea = null;
  function listen() {
    var AC = root.AudioContext || root.webkitAudioContext;
    if (!AC) return false;
    try {
      if (sea) hush(0);
      var ctx = (sea && sea.ctx) || new AC();
      if (ctx.resume) ctx.resume();
      var len = ctx.sampleRate * 4, buf = ctx.createBuffer(1, len, ctx.sampleRate), data = buf.getChannelData(0), last = 0;
      for (var k = 0; k < len; k++) {
        last = (last + 0.02 * (Math.random() * 2 - 1)) / 1.02;
        data[k] = last * 3.5;
      }
      var src = ctx.createBufferSource(), band = ctx.createBiquadFilter(), swell = ctx.createGain(),
        lfo = ctx.createOscillator(), depth = ctx.createGain(), master = ctx.createGain(), now = ctx.currentTime;
      src.buffer = buf;
      src.loop = true;
      band.type = 'bandpass';
      band.frequency.value = 520;
      band.Q.value = 0.6;
      swell.gain.value = 0.6;
      lfo.frequency.value = 0.12;
      depth.gain.value = 0.35;
      lfo.connect(depth);
      depth.connect(swell.gain);
      master.gain.setValueAtTime(0, now);
      master.gain.linearRampToValueAtTime(0.16, now + 1.8);
      src.connect(band);
      band.connect(swell);
      swell.connect(master);
      master.connect(ctx.destination);
      src.start();
      lfo.start();
      sea = { ctx: ctx, src: src, lfo: lfo, master: master };
      return true;
    } catch (e) {
      sea = null;
      return false;
    }
  }
  // Let the sea go quiet over a breath (seconds), then stop.
  function hush(over) {
    if (!sea || !sea.src) return;
    var s = sea, t = s.ctx.currentTime, wait = over == null ? 1.6 : over;
    sea = { ctx: s.ctx };
    try {
      s.master.gain.cancelScheduledValues(t);
      s.master.gain.setValueAtTime(s.master.gain.value, t);
      s.master.gain.linearRampToValueAtTime(0, t + wait);
      setTimeout(function () {
        try { s.src.stop(); s.lfo.stop(); } catch (e) { /* already stopped */ }
      }, wait * 1000 + 100);
    } catch (e) { /* the context has gone */ }
  }
  function sounding() { return !!(sea && sea.src); }

  var api = { FORMS: FORMS, draw: draw, listen: listen, hush: hush, sounding: sounding };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Shell = api;
})(this);
