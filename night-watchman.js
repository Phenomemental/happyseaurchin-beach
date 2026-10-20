/* What the Night Watchman's three pages share. Their words are Dorylus's,
   kept on the beach at beings:night-watchman 8.8363 (the front door at 1,
   Begin at 2, the Beings at 3) and read live; each page is written with
   those words already in place, so it makes sense before the beach answers
   or if it never does, and a part whose words can't be read keeps them. */
(function () {
  var BLOCK = 'beings:night-watchman';
  var PATH = ['8', '8', '3', '6', '3'];

  // A node's own words: a string, or its "_", followed down when the beach
  // has nested older words there. A dated note at the end saying how the
  // words were changed ("(Changed 19 October 2026, evening: …)") is for
  // their keepers, never the page.
  var CHANGED = /\s*\([^()]*\bchanged\b[^()]*\b\d{4}\b[^()]*\)\s*$/i;
  function own(node) {
    while (node && typeof node === 'object' && node._ && typeof node._ === 'object') node = node._;
    return (Beach.text(node) || '').trim().replace(CHANGED, '');
  }
  function at(node, path) {
    for (var i = 0; i < path.length && node; i++) node = node && typeof node === 'object' ? node[path[i]] : null;
    return node || null;
  }

  // "Label: value. Label: value." Each label is looked for after the one
  // before it, at the start of a sentence; a label written twice is given
  // twice ("Button", "Button#2"). Only the labels found are returned.
  function fields(s, labels) {
    var found = [], from = 0;
    labels.forEach(function (key) {
      var name = key.replace(/#\d+$/, '') + ': ';
      var i = s.indexOf(name, from);
      while (i > 0 && !/(^|[.:]\s)$/.test(s.slice(Math.max(0, i - 2), i))) i = s.indexOf(name, i + 1);
      if (i < 0) return;
      found.push({ key: key, start: i, from: i + name.length });
      from = i + name.length;
    });
    var out = {};
    found.forEach(function (f, j) {
      out[f.key] = s.slice(f.from, j + 1 < found.length ? found[j + 1].start : s.length).trim();
    });
    return out;
  }
  // "i, Title: body. ii, Title: body." as items; a list with no titles
  // ("i, In Claude, open Settings…") gives the whole item as its body.
  function numbered(s, titled) {
    var re = /(?:^|[\s.:])(iv|vi|v|iii|ii|i), /g, marks = [], m;
    while ((m = re.exec(s))) marks.push({ n: m[1], start: m.index + m[0].length - m[1].length - 2, from: m.index + m[0].length });
    return marks.map(function (k, j) {
      var t = s.slice(k.from, j + 1 < marks.length ? marks[j + 1].start : s.length).trim();
      if (!titled) return { n: k.n, title: '', body: t };
      var c = t.indexOf(': ');
      return c < 0 ? { n: k.n, title: t, body: '' } : { n: k.n, title: t.slice(0, c), body: t.slice(c + 2) };
    });
  }
  // A part that opens in capitals ("WHAT YOU NEED: …"): its head in
  // sentence case, and the rest.
  function head(s) {
    var m = /^([A-Z][A-Z ,'’-]*[A-Z])[.:]\s*([\s\S]*)$/.exec(s);
    if (!m) return { head: '', rest: s };
    var h = m[1].toLowerCase();
    return { head: h.charAt(0).toUpperCase() + h.slice(1), rest: m[2] };
  }
  // A button's words without the sentence's full stop.
  function bare(s) {
    return (s || '').replace(/\.\s*$/, '');
  }
  function sentences(s) {
    return (s.match(/[^.!?]+[.!?]+["”’)]*(\s+|$)|[^.!?]+$/g) || []).map(function (x) { return x.trim(); }).filter(Boolean);
  }
  function set(el, s) {
    if (el && s) el.textContent = s;
  }

  // A line of care: the number to call stands out and can be called from a
  // phone, and the helpline finder opens.
  function care(el, s) {
    if (!el || !s) return;
    el.textContent = '';
    var re = /(Samaritans, 116 123|116 123|findahelpline\.com)/g, last = 0, m;
    while ((m = re.exec(s))) {
      if (m.index > last) el.appendChild(document.createTextNode(s.slice(last, m.index)));
      if (m[1] === 'findahelpline.com') {
        var a = document.createElement('a');
        a.href = 'https://findahelpline.com';
        a.textContent = m[1];
        el.appendChild(a);
      } else {
        var tel = document.createElement('a');
        tel.className = 'tel';
        tel.href = 'tel:116123';
        var b = document.createElement('strong');
        b.textContent = m[1];
        tel.appendChild(b);
        el.appendChild(tel);
      }
      last = re.lastIndex;
    }
    if (last < s.length) el.appendChild(document.createTextNode(s.slice(last)));
  }

  // What all three pages carry (8.83634): the signature each ends with,
  // and the care line the Beings page sets under its opening.
  function forAll(words) {
    var t = own(words && words['4']);
    var sig = /\bsignature ([A-Z][^,.]*?)[,.]/.exec(t);
    var care = /\bunder its opening: ([\s\S]+)$/.exec(t);
    return { signature: sig ? sig[1].trim() : '', care: care ? care[1].trim() : '' };
  }

  // Read the words, now and whenever the page comes back into view, and
  // hand them to render(words, block) only when they, or the Beings and
  // their portals (1, 2), how to read through one (3), the two extremes
  // (4), play (9) or the pages' design (8.1), have changed. lost() is
  // called when the beach can't be read before it has ever answered. The
  // beach's own status line is kept off the page.
  function follow(render, lost) {
    var shown = '';
    Beach.live(function () {
      return Beach.get(BLOCK).then(function (block) {
        var words = at(block, PATH);
        if (!words) throw new Error('no words');
        var parts = ['1', '2', '3', '4', '9'].map(function (k) { return block[k]; }).concat([at(block, ['8', '1'])]);
        var sig = JSON.stringify(words) + JSON.stringify(parts);
        if (sig === shown) return;
        shown = sig;
        render(words, block);
      }, function (e) {
        if (!shown && lost) lost();
        throw e;
      });
    }, document.createElement('p'), null, '');
  }

  // A place's own words: "FACTS — the data: what could be checked, …" is
  // the place Facts, and what follows the dash.
  function place(s) {
    var m = /^([A-Z][A-Z'’ ]*[A-Z])\s*[—–]\s*([\s\S]*)$/.exec(s || '');
    if (!m) return null;
    var n = m[1].toLowerCase();
    return { name: n.charAt(0).toUpperCase() + n.slice(1), rest: m[2].charAt(0).toUpperCase() + m[2].slice(1) };
  }
  // The six places' order round the centre, clockwise from the top, as
  // spine:p6 gives it ("… CLOCKWISE …: Outcomes (2.3), Fictions (1.3), …").
  function order(spine) {
    var root = own(spine), from = root.search(/CLOCKWISE/i), list = [];
    (from < 0 ? root : root.slice(from)).replace(/\((\d\.\d)\)/g, function (all, a) {
      if (list.indexOf(a) < 0) list.push(a);
    });
    return list;
  }

  // ---- The ring --------------------------------------------------------------------
  // The drawing the front door and the Beings share, so the two pages are
  // one place: six places round a still dark centre, clockwise from the
  // top, each a lantern that breathes slowly and opens when touched.
  // ring(g, c, places, open, opts) draws into the SVG group g, centred at
  // c ({x, y}); places are [{addr, name}]; open(place) is what a touch
  // does; opts.amp sets the heart's ampersand in the centre. It returns
  // each place's centre and angle, for whatever stands round it.
  var SVG = 'http://www.w3.org/2000/svg';
  var RING = { r: 100, place: 38 };
  function svg(tag, attrs) {
    var e = document.createElementNS(SVG, tag);
    Object.keys(attrs).forEach(function (k) { e.setAttribute(k, attrs[k]); });
    return e;
  }
  function point(c, r, deg) {
    var a = deg * Math.PI / 180;
    return { x: Math.round((c.x + r * Math.cos(a)) * 10) / 10, y: Math.round((c.y + r * Math.sin(a)) * 10) / 10 };
  }
  function pressable(e, fn) {
    e.addEventListener('click', function () { fn(); });
    e.addEventListener('keydown', function (ev) {
      if (ev.key === 'Enter' || ev.key === ' ') { ev.preventDefault(); fn(); }
    });
  }
  function ring(g, c, places, open, opts) {
    opts = opts || {};
    var owner = g.ownerSVGElement, R = RING.r, RS = RING.place, out = [];
    if (owner && !owner.querySelector('#nw-lamp')) {
      var defs = svg('defs', {}), grad = svg('radialGradient', { id: 'nw-lamp', cx: '50%', cy: '50%', r: '50%' });
      grad.appendChild(svg('stop', { offset: '0', 'stop-color': 'var(--crimson)', 'stop-opacity': '.42' }));
      grad.appendChild(svg('stop', { offset: '1', 'stop-color': 'var(--crimson)', 'stop-opacity': '0' }));
      defs.appendChild(grad);
      owner.insertBefore(defs, owner.firstChild);
    }
    g.appendChild(svg('circle', { cx: c.x, cy: c.y, r: R, fill: 'none', stroke: 'var(--frame)', 'stroke-width': '0.9' }));
    places.forEach(function (p, k) {
      var deg = -90 + k * 360 / places.length, s = point(c, R, deg);
      var from = point(c, 34, deg), to = point(c, R - RS - 9, deg);
      g.appendChild(svg('line', { x1: from.x, y1: from.y, x2: to.x, y2: to.y, stroke: 'var(--frame)', 'stroke-width': '1', 'stroke-dasharray': '2 4', 'stroke-linecap': 'round' }));
      var lamp = svg('g', { 'class': 'place', tabindex: '0', role: 'button', 'aria-label': p.name || 'A place to stand' });
      lamp.dataset.addr = p.addr;
      // each breathes slowly, a little out of step with the others
      lamp.style.setProperty('--d', (8.5 + (k * 5 % 6) * 0.7).toFixed(1) + 's');
      lamp.style.setProperty('--l', (-k * 1.7).toFixed(1) + 's');
      lamp.appendChild(svg('circle', { 'class': 'glow', cx: s.x, cy: s.y, r: RS + 22, fill: 'url(#nw-lamp)' }));
      lamp.appendChild(svg('circle', { 'class': 'breath', cx: s.x, cy: s.y, r: RS + 8, fill: 'var(--frame)' }));
      lamp.appendChild(svg('circle', { 'class': 'disc', cx: s.x, cy: s.y, r: RS, fill: 'var(--page)', stroke: 'var(--ink)', 'stroke-width': '1.2' }));
      lamp.appendChild(svg('circle', { 'class': 'ring', cx: s.x, cy: s.y, r: RS - 5, fill: 'none', stroke: 'var(--ink)', 'stroke-width': '0.7', opacity: '.45' }));
      var t = svg('text', { 'class': 'place-label', x: s.x, y: s.y + 3.6, 'text-anchor': 'middle' });
      t.textContent = (p.name || '').toUpperCase();
      lamp.appendChild(t);
      if (p.name) pressable(lamp, function () { open(p); });
      g.appendChild(lamp);
      out.push({ place: p, at: s, deg: deg });
    });
    // the still centre
    g.appendChild(svg('circle', { cx: c.x, cy: c.y, r: 31, fill: 'var(--frame)', opacity: '.35' }));
    g.appendChild(svg('circle', { 'class': 'still', cx: c.x, cy: c.y, r: 26, fill: 'var(--ink)' }));
    if (opts.amp) {
      var amp = svg('text', { 'class': 'amp', x: c.x, y: c.y + 12, 'text-anchor': 'middle' });
      amp.textContent = '&';
      g.appendChild(amp);
    }
    fit(g);
    return out;
  }
  // A place's name, made narrower if it would pass its ring.
  function fit(g) {
    Array.prototype.forEach.call(g.querySelectorAll('.place-label'), function (t) {
      t.removeAttribute('textLength');
      try {
        if (t.getComputedTextLength() > 2 * (RING.place - 8)) {
          t.setAttribute('textLength', 2 * (RING.place - 8));
          t.setAttribute('lengthAdjust', 'spacingAndGlyphs');
        }
      } catch (e) {}
    });
  }
  // The open lantern stays lit; every other one is dark.
  function light(g, addr) {
    Array.prototype.forEach.call(g.querySelectorAll('[data-addr]'), function (e) {
      e.classList.toggle('lit', !!addr && e.dataset.addr === addr);
    });
  }

  window.NW = {
    own: own,
    at: at,
    fields: fields,
    numbered: numbered,
    head: head,
    bare: bare,
    sentences: sentences,
    set: set,
    care: care,
    forAll: forAll,
    follow: follow,
    place: place,
    order: order,
    ring: ring,
    fit: fit,
    light: light,
    svg: svg,
    point: point,
    pressable: pressable
  };
})();
