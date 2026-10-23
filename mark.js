/* Sundial's mark: the six places a day can stand in, a day as the plain
   text its one slot holds, the sums the drawing is made from, and the
   drawing (the plan on the beach at spine:front-door, 7.142 and 7.16 to
   7.17). One table holds each place's hue, the quiet line beneath a
   sheet, the question an empty pool asks, and where its Beings stand in
   beings:night-watchman; the Beings themselves are always read from the
   beach, never written here. Everything but dial() runs without a page. */
(function (root) {
  // In pool order, 1 to 6: the places' own numbers, as Claude names them.
  // They are drawn in P6's order instead (below).
  var POOLS = [
    { n: 1, name: 'Facts', hue: '#4A6B8A', phrase: 'what happened', at: '1.1',
      question: 'What could you have pointed to today?' },
    { n: 2, name: 'Feelings', hue: '#B0413E', phrase: 'what you felt', at: '1.2',
      question: 'What did your body know today?' },
    { n: 3, name: 'Fictions', hue: '#7A5C99', phrase: 'what you made of it', at: '1.3',
      question: 'What did you make of it, that might not be so?' },
    { n: 4, name: 'Purpose', hue: '#2E7D6B', phrase: 'what matters', at: '2.1',
      question: 'What mattered, underneath?' },
    { n: 5, name: 'Decision', hue: '#B8860B', phrase: 'a choice', at: '2.2',
      question: 'What did you choose, or let be chosen?' },
    { n: 6, name: 'Outcomes', hue: '#6FA8DC', phrase: 'what’s ahead', at: '2.3',
      question: 'What are you expecting, or fearing?' }
  ];

  // P6's order round presence, clockwise from Facts at the top (spine:p6;
  // plan 6.7, 6.8, 6.9; spine:front-door 8.73): the order every picture of
  // the places keeps, and the order the quiet line names them in. Pool
  // numbers: Facts, Outcomes, Fictions, Decision, Purpose, Feelings.
  var P6 = [1, 6, 3, 5, 4, 2];

  // ---- A day, as its slot holds it --------------------------------------------
  // One plain text, line by line: "place — title", the lines, "Tomorrow: …"
  // when they voiced one, and last the quiet line naming every place the
  // text touched, in P6's order (4.2): "What happened · what's ahead · what
  // you felt." An older text's "A day of what happened." still reads, as
  // touching that one place. A day written elsewhere (in Claude, say) may
  // be one line with none of these; it is still a day, with no colour.
  function plainPhrase(s) {
    return String(s).toLowerCase().replace(/[’‘`]/g, '\'').replace(/\s+/g, ' ').trim();
  }
  function poolByPhrase(s) {
    var want = plainPhrase(s);
    for (var i = 0; i < POOLS.length; i++) if (plainPhrase(POOLS[i].phrase) === want) return POOLS[i].n;
    return 0;
  }

  // Places as a list of pool numbers, each once, in P6's order; anything
  // that is not a place is left out. Never ranked.
  function inOrder(places) {
    var have = {};
    (Array.isArray(places) ? places : []).forEach(function (n) { have[Number(n)] = 1; });
    return P6.filter(function (n) { return have[n]; });
  }
  // The quiet words for places, in P6's order, joined by ' · ':
  // "what happened · what’s ahead · what you felt" ('' for none).
  function placesWords(places) {
    return inOrder(places).map(function (n) { return POOLS[n - 1].phrase; }).join(' · ');
  }
  // A last line that names places: every piece a place's phrase.
  function placesIn(row) {
    var old = /^a day of (.+?)\.?$/i.exec(row);
    if (old) return poolByPhrase(old[1]) ? [poolByPhrase(old[1])] : [];
    var pieces = row.replace(/\.\s*$/, '').split('·').map(function (x) { return x.trim(); });
    var ns = pieces.map(poolByPhrase);
    return pieces.length && ns.every(Boolean) ? inOrder(ns) : [];
  }

  // A sheet as Claude returns it ({title, lines, tomorrow, places}) and the
  // place, as the text of the slot. (A sheet with a single `pool`, as the
  // first law gave, writes as that one place.)
  function write(sheet, place) {
    var rows = [(place ? place + ' — ' : '') + String(sheet.title || '').trim()];
    (sheet.lines || []).forEach(function (l) {
      if (String(l).trim()) rows.push(String(l).trim());
    });
    var tomorrow = String(sheet.tomorrow || '').trim().replace(/^tomorrow:\s*/i, '');
    if (tomorrow) rows.push('Tomorrow: ' + tomorrow);
    var words = placesWords(sheet.places || (sheet.pool ? [sheet.pool] : []));
    if (words) rows.push(words.charAt(0).toUpperCase() + words.slice(1) + '.');
    return rows.join('\n');
  }

  // The text of a slot, read back as {place, title, lines, tomorrow,
  // places}; the places are read only from a last line that names them.
  function read(text) {
    var rows = String(text || '').split('\n').map(function (r) { return r.trim(); }).filter(Boolean);
    var day = { place: '', title: '', lines: [], tomorrow: '', places: [] };
    if (!rows.length) return day;
    var named = rows.length > 1 ? placesIn(rows[rows.length - 1]) : [];
    if (named.length) {
      day.places = named;
      rows.pop();
    }
    var first = rows.shift(), cut = first.indexOf(' — ');
    if (cut > 0) {
      day.place = first.slice(0, cut).trim();
      day.title = first.slice(cut + 3).trim();
    } else {
      day.title = first;
    }
    rows.forEach(function (r) {
      var t = /^tomorrow:\s*(.*)$/i.exec(r);
      if (t && !day.tomorrow) day.tomorrow = t[1];
      else day.lines.push(r);
    });
    return day;
  }

  // Every place a day touched (its text, or {text}), in P6's order: its own
  // quiet line, else the places this browser was given for it (a day kept
  // through Claude has no line; plan 6.8.2). [] when it names none.
  function placesOf(day) {
    var text = typeof day === 'string' ? day : day && typeof day.text === 'string' ? day.text : '';
    var own = read(text).places;
    if (own.length) return own;
    return day && typeof day === 'object' ? inOrder(day.places || (day.pool ? [day.pool] : [])) : [];
  }

  // ---- The sums ---------------------------------------------------------------
  // How many days touched each place, in pool order: a day counts once in
  // every place it touched (6.9).
  function counts(days) {
    var c = [0, 0, 0, 0, 0, 0];
    (days || []).forEach(function (d) {
      placesOf(d).forEach(function (n) { c[n - 1]++; });
    });
    return c;
  }

  // Each place's share: the days that touched it over all the touches
  // across all the days, summing to 1; all nought before any day.
  function shares(days) {
    var c = counts(days);
    var total = c.reduce(function (a, b) { return a + b; }, 0);
    return c.map(function (x) { return total ? x / total : 0; });
  }

  // How evenly the days spread across the six: 1 less the variance of the
  // shares over the most it can be (every day in one pool, 5/36), so 1 when
  // all six hold the same and 0 when one holds everything. Nought before
  // any day: the drawing gives the empty heart its own centre.
  var MOST = 5 / 36;
  function evenness(s) {
    var total = s.reduce(function (a, b) { return a + b; }, 0);
    if (!total) return 0;
    var mean = 1 / 6, v = 0;
    s.forEach(function (x) { v += (x - mean) * (x - mean); });
    v /= 6;
    return Math.min(1, Math.max(0, 1 - v / MOST));
  }

  // Where the shadow falls by the phone's clock: between 06:00 and 19:30
  // an angle in degrees from the centre, -90 + (hour - 12) x 15, so morning
  // falls left, noon up and evening right; outside those hours null, and
  // the dial glows instead.
  function shadowAngle(date) {
    var h = date.getHours() + date.getMinutes() / 60;
    if (h < 6 || h > 19.5) return null;
    return -90 + (h - 12) * 15;
  }

  // The crimson centre: scaled 0.55 to 1 by evenness once there are days;
  // before any, half size at 0.45.
  function centre(days, even) {
    return days ? { scale: 0.55 + 0.45 * even, opacity: 1 } : { scale: 0.5, opacity: 0.45 };
  }

  // ---- The drawing ------------------------------------------------------------
  // dial(svg) dresses the heart already in the page (night-watchman.html's,
  // copied exactly, inside a group of class "breathe", with the wash, the
  // centre and the flow line marked by class) as the sundial: the shadow
  // or the night glow beneath the ink, and the ring of nine marks. It
  // returns { time(date), answering(on), days(list, kept) }: time moves the
  // shadow; answering sets the slow pulse along the line while Claude
  // answers (drawn by the page's CSS on the class); days draws the days
  // kept, each a day's text (6.9): a lit mark per day on the ring in its
  // pool's hue, folding to the outer ring at each nine, the colour inside
  // the ink by share, the wash by count and the centre by evenness. With
  // kept, the newest mark lights and the colour sweeps in along the stroke
  // (the page's CSS draws the sweep, and holds still for reduced motion).
  var SVGNS = 'http://www.w3.org/2000/svg';
  var CX = 232, CY = 140;          // the dial's centre in the heart's own units
  var RING = 186, SHADOW = 160, STRETCH = 160, MOMENTS = 166;
  function node(name, attrs, parent) {
    var n = document.createElementNS(SVGNS, name);
    Object.keys(attrs).forEach(function (k) { n.setAttribute(k, attrs[k]); });
    if (parent) parent.appendChild(n);
    return n;
  }
  function at(angle, r) {
    var a = angle * Math.PI / 180;
    return { x: CX + r * Math.cos(a), y: CY + r * Math.sin(a) };
  }

  // How the ring stands for n days: the marks lit on the ring (the days
  // since the last full nine) and the folds, one per nine, up to nine of
  // them: eighty-one days.
  function ring(n) {
    return { inner: n % 9, outer: Math.min(9, Math.floor(n / 9)) };
  }
  // The fold is a spiral, never a second closed circle (6.9, after 8.71):
  // a faint open stroke from the top at radius 200, winding clockwise and
  // widening to 214 across one full turn. A point on it, t degrees along
  // from the top; the j-th fold sits at 40 x j degrees, so the ninth closes
  // the turn at 214, outside where it began, and never meets itself.
  var FOLD_IN = 200, FOLD_OUT = 214;
  function foldAt(t) {
    return at(-90 + t, FOLD_IN + (FOLD_OUT - FOLD_IN) * t / 360);
  }
  // The stroke for k folds: drawn only as far as the last of them.
  function foldPath(k) {
    if (!k) return '';
    var end = 40 * Math.min(9, k), pts = [];
    for (var t = 0; t < end; t += 4) pts.push(foldAt(t));
    pts.push(foldAt(end));
    return 'M ' + pts.map(function (p) { return p.x.toFixed(1) + ',' + p.y.toFixed(1); }).join(' L ');
  }
  // A pool's stretch of colour inside the ink: 0.35 + 1.6 x its share,
  // capped at 1; nothing for a pool with no days.
  function tint(share) {
    return share > 0 ? Math.min(1, 0.35 + 1.6 * share) : 0;
  }

  function dial(svg) {
    var art = svg.querySelector('.breathe');
    var flow = art.querySelector('.flow');
    var path = flow ? flow.getAttribute('d') : '';
    var id = 'sd' + Math.random().toString(36).slice(2, 8);
    var defs = node('defs', {});
    svg.insertBefore(defs, svg.firstChild);
    // The wash reads as the sun, never a container: soft-edged, no outline.
    var washFill = node('radialGradient', { id: id + '-wash' }, defs);
    node('stop', { offset: '0', 'stop-color': 'var(--wash)', 'stop-opacity': '1' }, washFill);
    node('stop', { offset: '0.62', 'stop-color': 'var(--wash)', 'stop-opacity': '0.9' }, washFill);
    node('stop', { offset: '1', 'stop-color': 'var(--wash)', 'stop-opacity': '0' }, washFill);
    var glowFill = node('radialGradient', { id: id + '-glow' }, defs);
    node('stop', { offset: '0', 'stop-color': 'var(--wash)', 'stop-opacity': '0.3' }, glowFill);
    node('stop', { offset: '1', 'stop-color': 'var(--wash)', 'stop-opacity': '0' }, glowFill);
    // The sweep: colour shows only where this line has drawn so far.
    var mask = node('mask', { id: id + '-sweep', maskUnits: 'userSpaceOnUse', x: 17, y: -75, width: 430, height: 430 }, defs);
    var sweep = node('path', { 'class': 'sweep', d: path, fill: 'none', stroke: '#fff', 'stroke-width': 14,
      'stroke-linecap': 'round', pathLength: 1000, 'stroke-dasharray': '1000 1000' }, mask);

    var under = node('g', { 'class': 'under' });
    art.insertBefore(under, art.firstChild);
    var glow = node('circle', { 'class': 'glow', cx: CX, cy: CY, r: 120, fill: 'url(#' + id + '-glow)' }, under);
    var shadow = node('line', { 'class': 'shadow', x1: CX, y1: CY, x2: CX, y2: CY - SHADOW,
      stroke: 'var(--ink)', 'stroke-opacity': '0.1', 'stroke-width': 7, 'stroke-linecap': 'round' }, under);

    var wash = art.querySelector('.wash');
    if (wash) {
      wash.setAttribute('fill', 'url(#' + id + '-wash)');
      wash.removeAttribute('stroke');
    }
    var dot = art.querySelector('.centre');
    var halo = null;
    if (dot) {
      halo = node('ellipse', { 'class': 'halo', cx: 250, cy: 138, rx: 64, ry: 40, fill: 'var(--crimson)', opacity: 0,
        transform: 'rotate(-18 250 138)' });
      art.insertBefore(halo, dot);
    }

    // The colour inside the ink: six stretches along the line, in P6's order
    // from where it comes in (top right) to where it leaves (bottom left),
    // starting from Facts.
    var tints = node('g', { 'class': 'tints', mask: 'url(#' + id + '-sweep)' });
    if (flow) art.insertBefore(tints, flow);
    else art.appendChild(tints);
    var stretches = P6.map(function (n, k) {
      return node('path', { d: path, fill: 'none', stroke: POOLS[n - 1].hue, 'stroke-width': 7, 'stroke-linecap': 'butt',
        pathLength: 1000, 'stroke-dasharray': STRETCH + ' ' + (1000 - STRETCH), 'stroke-dashoffset': -k * STRETCH, opacity: 0,
        'data-place': n }, tints);
    });

    var marks = node('g', { 'class': 'ring' }, art);
    for (var i = 0; i < 9; i++) {
      var a = -90 + i * 40, p = at(a, RING - 7), q = at(a, RING + 7);
      node('line', { x1: p.x.toFixed(1), y1: p.y.toFixed(1), x2: q.x.toFixed(1), y2: q.y.toFixed(1),
        stroke: 'var(--ink)', 'stroke-opacity': '0.55', 'stroke-width': 2.5, 'stroke-linecap': 'round' }, marks);
    }
    var fold = node('path', { 'class': 'fold', d: '', fill: 'none', stroke: 'var(--ink)', 'stroke-opacity': '0.15',
      'stroke-width': 1.5, 'stroke-linecap': 'round' }, art);
    var lit = node('g', { 'class': 'lit' }, art);
    var momentsAt = node('g', { 'class': 'sd-moments' }, art);
    var pooled = node('g', { 'class': 'sd-tidepools' }, svg);

    function time(date) {
      var angle = shadowAngle(date);
      if (angle === null) {
        shadow.setAttribute('visibility', 'hidden');
        glow.setAttribute('visibility', 'visible');
        return;
      }
      var end = at(angle, SHADOW);
      shadow.setAttribute('x2', end.x.toFixed(1));
      shadow.setAttribute('y2', end.y.toFixed(1));
      shadow.setAttribute('visibility', 'visible');
      glow.setAttribute('visibility', 'hidden');
    }
    function answering(on) {
      svg.classList.toggle('answering', !!on);
    }
    function days(list, kept) {
      list = list || [];
      var n = list.length, s = shares(list), even = evenness(s), r = ring(n);
      while (lit.firstChild) lit.removeChild(lit.firstChild);
      // A lit mark says only that a day was kept: ink, never a place's colour.
      for (var i = 0; i < r.inner; i++) {
        var c = at(-90 + i * 40, RING);
        node('circle', { cx: c.x.toFixed(1), cy: c.y.toFixed(1), r: 7, fill: 'var(--ink)',
          'class': kept && i === r.inner - 1 ? 'mark new' : 'mark' }, lit);
      }
      // The folds: small open circles along the spiral, from the top.
      fold.setAttribute('d', foldPath(r.outer));
      for (var k = 1; k <= r.outer; k++) {
        var o = foldAt(40 * k);
        node('circle', { cx: o.x.toFixed(1), cy: o.y.toFixed(1), r: 4, fill: 'none', stroke: 'var(--ink)',
          'stroke-opacity': '0.5', 'stroke-width': 1.5, 'class': kept && r.inner === 0 && k === r.outer ? 'mark new' : 'mark' }, lit);
      }
      stretches.forEach(function (el, k) { el.setAttribute('opacity', tint(s[P6[k] - 1]).toFixed(3)); });
      if (wash) wash.setAttribute('opacity', (0.6 * Math.min(1, n / 27)).toFixed(3));
      var cen = centre(n, even);
      if (dot) {
        dot.setAttribute('transform', 'rotate(-18 250 138) translate(250 138) scale(' + cen.scale + ') translate(-250 -138)');
        dot.setAttribute('opacity', cen.opacity);
      }
      if (halo) halo.setAttribute('opacity', n ? (0.18 * (1 - even)).toFixed(3) : 0);
      svg.setAttribute('aria-label', n
        ? 'A sundial: a heart drawn in ink, its shadow at this hour, coloured by ' + n + (n === 1 ? ' day' : ' days') + ' kept'
        : 'A sundial: a heart drawn in ink, its shadow at this hour, and nine marks around it, none lit yet');
      // The sweep restarts from the line's entry when a day is kept.
      sweep.classList.remove('sweeping');
      if (kept) {
        void sweep.getBoundingClientRect();
        sweep.classList.add('sweeping');
      }
    }
    // Hold: the colour drains into six pools; gather() brings it back.
    function pools(list, label, pick) {
      while (pooled.firstChild) pooled.removeChild(pooled.firstChild);
      var ps = poolsAt(list);
      // Drawn, and reached by Tab, in P6's order from Facts at the top; each
      // keeps its own number, i, for the page.
      P6.forEach(function (n) {
        var i = n - 1, p = ps[i], pool = POOLS[i];
        var g = node('g', { 'class': 'sd-tidepool' + (p.days ? '' : ' empty'), role: 'button', tabindex: 0, 'data-pool': i,
          'aria-label': label(pool, p.days), transform: 'translate(' + p.x.toFixed(1) + ' ' + p.y.toFixed(1) + ')' }, pooled);
        var inner = node('g', { 'class': 'sd-tidepool-grow' }, g);
        node('circle', { 'class': 'sd-tidepool-hit', r: Math.max(p.r, 22), fill: 'transparent' }, inner);
        node('circle', p.days
          ? { 'class': 'sd-tidepool-water', r: p.r.toFixed(1), fill: pool.hue, 'fill-opacity': 0.9 }
          : { 'class': 'sd-tidepool-water', r: p.r, fill: 'none', stroke: 'var(--frame)', 'stroke-width': 2 }, inner);
        g.addEventListener('click', function (ev) { ev.stopPropagation(); pick(i); });
        g.addEventListener('keydown', function (ev) {
          if (ev.key !== 'Enter' && ev.key !== ' ') return;
          ev.preventDefault();
          ev.stopPropagation();
          if (!ev.repeat) pick(i);
        });
      });
      svg.classList.add('pooled');
    }
    function gather() {
      svg.classList.remove('pooled');
      while (pooled.firstChild) pooled.removeChild(pooled.firstChild);
    }
    function picked(i) {
      Array.prototype.forEach.call(pooled.querySelectorAll('.sd-tidepool'), function (g) { g.classList.toggle('picked', Number(g.getAttribute('data-pool')) === i); });
    }
    days([]);
    // Today's moments (6.9): each a small ink dot on its own ring at the
    // hour and minute it was spoken, read as a 24-hour day (noon up,
    // midnight down). It says when they spoke, never where it stood.
    // `list` is [{when: Date, text}].
    function moments(list) {
      while (momentsAt.firstChild) momentsAt.removeChild(momentsAt.firstChild);
      (list || []).forEach(function (m) {
        var h = m.when.getHours() + m.when.getMinutes() / 60;
        var c = at(-90 + (h - 12) * 15, MOMENTS);
        node('circle', { cx: c.x.toFixed(1), cy: c.y.toFixed(1), r: 4.5, fill: 'var(--ink)', 'fill-opacity': '0.6', 'class': 'moment' }, momentsAt);
      });
    }
    return { time: time, answering: answering, days: days, moments: moments, pools: pools, gather: gather, picked: picked };
  }

  // ---- Hold: the pools -----------------------------------------------------------
  // The six tide-pools in a ring round the centre (6.8), in P6's order:
  // Facts at the top, then clockwise Outcomes, Fictions, Decision, Purpose,
  // Feelings. Each is sized by its share (its area, so a place touched by
  // twice the days looks twice as full); an empty pool is a pale ring of
  // its own small size. In pool order, each with where it is drawn.
  var POOL_RING = 128, POOL_EMPTY = 14, POOL_MIN = 16, POOL_MAX = 50;
  function poolsAt(list) {
    var s = shares(list || []), c = counts(list || []);
    return POOLS.map(function (p, i) {
      var at_ = at(-90 + P6.indexOf(p.n) * 60, POOL_RING);
      return { n: p.n, x: at_.x, y: at_.y, days: c[i], share: s[i],
        r: s[i] > 0 ? POOL_MIN + (POOL_MAX - POOL_MIN) * Math.sqrt(s[i]) : POOL_EMPTY };
    });
  }

  // ---- The tideline -------------------------------------------------------------
  // tideline(svg, list, width, label, pick) lays the days along a faint
  // tideline (6.7): one shell per day, oldest left and newest right, set a
  // little above or below the line by its own address, so the same days
  // always lie the same way. Each is a sundial shell (Architectonica) seen
  // from above, carrying the day's mix (sundialShell, below), always
  // upright so Facts stays at the top. Each shell is a button named by
  // label(day); pick(i) runs when one is tapped or chosen with Enter. The
  // shells keep at least a finger's width apart, so a long tideline grows
  // wider than the screen; the width drawn is returned. Nothing is counted
  // or labelled beside them.
  var SHELL = 32, PAD = 22, TIDE_H = 120, TIDE_BASE = 62, SHELL_R = 13;
  function scatter(address) {
    var h = 2166136261;
    String(address).split('').forEach(function (c) {
      h ^= c.charCodeAt(0);
      h = Math.imul(h, 16777619) >>> 0;
    });
    return { dy: (h % 29) - 14 };
  }
  function shellsAt(list, width) {
    var n = list.length;
    var step = n > 1 ? Math.max(SHELL, (width - 2 * PAD) / (n - 1)) : 0;
    var w = n > 1 ? Math.max(width, 2 * PAD + step * (n - 1)) : width;
    return { width: w, at: list.map(function (day, i) {
      var s = scatter(day.address);
      return { x: n > 1 ? PAD + step * i : w / 2, y: TIDE_BASE + s.dy };
    }) };
  }
  // The six wedges of a sundial shell of radius r, in P6's order clockwise
  // from Facts at the top, each centred on its own direction: [{n, d}].
  function wedges(r) {
    return P6.map(function (n, k) {
      var a0 = (-90 + k * 60 - 30) * Math.PI / 180, a1 = (-90 + k * 60 + 30) * Math.PI / 180;
      return { n: n, d: 'M 0,0 L ' + (r * Math.cos(a0)).toFixed(2) + ',' + (r * Math.sin(a0)).toFixed(2) +
        ' A ' + r + ' ' + r + ' 0 0 1 ' + (r * Math.cos(a1)).toFixed(2) + ',' + (r * Math.sin(a1)).toFixed(2) + ' Z' };
    });
  }
  // One spiral coiling out from presence at the centre to the rim, fine so
  // it never hides the wedges.
  function shellSpiral(r) {
    var turns = 2.5, pts = [];
    for (var t = 0; t <= turns * 360; t += 12) {
      var rr = r * 0.94 * t / (turns * 360), a = (t - 90) * Math.PI / 180;
      pts.push((rr * Math.cos(a)).toFixed(2) + ',' + (rr * Math.sin(a)).toFixed(2));
    }
    return 'M ' + pts.join(' L ');
  }
  // A day's sundial shell into a group: each wedge lit in its place's hue
  // where the day touched it, pale (the page with a faint ink edge) where
  // it did not; then the spiral and presence.
  function sundialShell(g, places, r) {
    var touched = {};
    places.forEach(function (n) { touched[n] = 1; });
    wedges(r).forEach(function (w) {
      node('path', touched[w.n]
        ? { 'class': 'sd-wedge lit', d: w.d, fill: POOLS[w.n - 1].hue, stroke: 'var(--page)', 'stroke-width': 0.6, 'data-place': w.n }
        : { 'class': 'sd-wedge', d: w.d, fill: 'var(--page)', stroke: 'var(--ink)', 'stroke-opacity': 0.22, 'stroke-width': 0.6, 'data-place': w.n }, g);
    });
    node('path', { 'class': 'sd-shell-spiral', d: shellSpiral(r), fill: 'none', stroke: 'var(--ink)', 'stroke-opacity': 0.55,
      'stroke-width': 0.7, 'stroke-linecap': 'round' }, g);
    node('circle', { r: r, fill: 'none', stroke: 'var(--ink)', 'stroke-opacity': 0.3, 'stroke-width': 0.7 }, g);
    node('circle', { 'class': 'sd-shell-presence', r: 1.4, fill: 'var(--crimson)' }, g);
  }
  function tideline(svg, list, width, label, pick) {
    while (svg.firstChild) svg.removeChild(svg.firstChild);
    var lay = shellsAt(list, width), w = lay.width;
    svg.setAttribute('viewBox', '0 0 ' + w.toFixed(0) + ' ' + TIDE_H);
    svg.setAttribute('width', w.toFixed(0));
    svg.setAttribute('height', TIDE_H);
    var waves = Math.max(2, Math.round(w / 140)), d = 'M 0 ' + TIDE_BASE;
    for (var k = 0; k < waves; k++) {
      var x0 = w * k / waves, x1 = w * (k + 1) / waves, third = (x1 - x0) / 3;
      d += ' C ' + (x0 + third).toFixed(1) + ' ' + (TIDE_BASE - 6) + ' ' + (x0 + 2 * third).toFixed(1) + ' ' +
        (TIDE_BASE + 6) + ' ' + x1.toFixed(1) + ' ' + TIDE_BASE;
    }
    node('path', { 'class': 'sd-tide-line', d: d, fill: 'none', stroke: 'var(--frame)', 'stroke-width': 1.5, 'stroke-linecap': 'round' }, svg);
    list.forEach(function (day, i) {
      var p = lay.at[i];
      var g = node('g', { 'class': 'sd-shell', role: 'button', tabindex: 0, 'aria-label': label(day),
        transform: 'translate(' + p.x.toFixed(1) + ' ' + p.y.toFixed(1) + ')' }, svg);
      node('circle', { 'class': 'sd-shell-hit', r: 17, fill: 'transparent' }, g);
      sundialShell(g, placesOf(day), SHELL_R);
      g.addEventListener('click', function () { pick(i); });
      g.addEventListener('keydown', function (ev) {
        if (ev.key !== 'Enter' && ev.key !== ' ') return;
        ev.preventDefault();
        pick(i);
      });
    });
    return w;
  }

  // ---- The console check ------------------------------------------------------
  // Mark.test(days, log, mixed) with the trial fixture (fixtures/days-trial.json,
  // nine days of one place each, the older 'A day of …' line: pools
  // 1,1,3,2,1,3,5,6,1) and the mixed fixture (fixtures/days-mixed.json, nine
  // days touching 1+2, 1, 1+6+2, 3, 1+3+4, 2, 5+6, all six, 4+2): the text read
  // and written, the touches, shares and evenness, against sums worked by
  // hand; the fold, the tideline's shells, the pools' order, the shadow.
  function test(days, log, mixed) {
    log = log || function (s) { console.log(s); };
    var results = [];
    function near(a, b) { return Math.abs(a - b) < 1e-12; }
    function all(a, b) { return a.length === b.length && a.every(function (x, i) { return near(x, b[i]); }); }
    function check(label, ok) {
      results.push(ok);
      log((ok ? 'pass  ' : 'FAIL  ') + label);
    }
    function clock(h, m) { return new Date(2026, 9, 21, h, m); }
    var thrice = days.concat(days, days);
    check('P6’s order: Facts, Outcomes, Fictions, Decision, Purpose, Feelings', P6.map(function (n) { return POOLS[n - 1].name; }).join() ===
      'Facts,Outcomes,Fictions,Decision,Purpose,Feelings');
    check('the trial fixture: nine days of one place each, 1,1,3,2,1,3,5,6,1 (the older line still reads)',
      days.length === 9 && days.map(function (d) { return placesOf(d).join('+'); }).join() === '1,1,3,2,1,3,5,6,1');
    var last = read(days[8].text);
    check('a day reads back as place, title, lines, tomorrow and places',
      last.place === 'Arona' && last.title === 'Lizards and the first quiet' && last.lines.length === 1 &&
      last.tomorrow === 'walk up to the old town before it gets hot.' && last.places.join() === '1');
    var sheet = { title: 'Lizards and the first quiet', lines: ['A slow morning.', 'Your youngest laughing.'],
      tomorrow: 'Tomorrow: walk up early.', places: [2, 6, 1] };
    var text = write(sheet, 'Arona');
    check('a sheet writes its places as the last line, in P6’s order, joined by " · "',
      text === 'Arona — Lizards and the first quiet\nA slow morning.\nYour youngest laughing.\nTomorrow: walk up early.\nWhat happened · what’s ahead · what you felt.');
    var back = read(text);
    check('and reads back the same', back.place === 'Arona' && back.title === sheet.title &&
      back.lines.join('|') === sheet.lines.join('|') && back.tomorrow === 'walk up early.' && back.places.join() === '1,6,2');
    check('places are never ranked: any order, repeats and strays come out once each in P6’s order',
      write({ title: 'T', lines: [], places: [4, 1, 4, 9, 3] }) === 'T\nWhat happened · what you made of it · what matters.');
    check('one place, and a sheet with the first law’s single pool, write as one place',
      write({ title: 'T', lines: ['x'], places: [6] }) === 'T\nx\nWhat’s ahead.' && write({ title: 'T', lines: [], pool: 5 }) === 'T\nA choice.');
    check('a straight apostrophe names the same place', placesOf('Here — A day\nSome words.\nWhat happened · what\'s ahead.').join() === '1,6' &&
      placesOf('Here — A day\nSome words.\nA day of what\'s ahead.').join() === '6');
    check('a day of one plain line has a title and no places',
      read('Into bed early, quietly relieved.').title === 'Into bed early, quietly relieved.' && placesOf('Into bed early.').length === 0);
    check('a line of their own is not a places line unless every piece names a place',
      placesOf('Home — Rain\nA day of rain, mostly.').length === 0 && placesOf('Tea\nTea · toast · the sea.').length === 0 &&
      placesOf('Tea\nWhat happened · the sea.').length === 0 && read('Tea\nWhat happened · the sea.').lines.length === 1);
    check('places this browser was given stand in for a day with no line, in P6’s order',
      placesOf({ text: 'Kept through Claude.', places: [2, 1] }).join() === '1,2' && placesOf({ text: 'Kept.', pool: 3 }).join() === '3' &&
      placesOf({ text: 'Mine\nx\nA choice.', places: [1] }).join() === '5');
    check('the table names six places in their numbers, with the plan’s hues',
      POOLS.map(function (p) { return p.n + p.hue; }).join() ===
        '1#4A6B8A,2#B0413E,3#7A5C99,4#2E7D6B,5#B8860B,6#6FA8DC');
    check('no days: no shares, evenness 0', all(shares([]), [0, 0, 0, 0, 0, 0]) && evenness(shares([])) === 0);
    check('1 day: all in what happened, evenness 0',
      all(shares(days.slice(0, 1)), [1, 0, 0, 0, 0, 0]) && near(evenness(shares(days.slice(0, 1))), 0));
    check('3 days: shares 2/3 and 1/3, evenness 8/15',
      all(shares(days.slice(0, 3)), [2 / 3, 0, 1 / 3, 0, 0, 0]) && near(evenness(shares(days.slice(0, 3))), 8 / 15));
    check('9 days: touches 4,1,2,0,1,1', counts(days).join() === '4,1,2,0,1,1');
    check('9 days: shares 4/9, 1/9, 2/9, 0, 1/9, 1/9',
      all(shares(days), [4 / 9, 1 / 9, 2 / 9, 0, 1 / 9, 1 / 9]));
    check('9 days: evenness 116/135 (' + evenness(shares(days)).toFixed(4) + ')',
      near(evenness(shares(days)), 116 / 135));
    check('27 days: the same shares and evenness as nine',
      counts(thrice).join() === '12,3,6,0,3,3' && near(evenness(shares(thrice)), 116 / 135));
    check('one day touching all six counts once in each: evenness 1',
      counts(['Here\nA day\nWhat happened · what’s ahead · what you made of it · a choice · what matters · what you felt.']).join() === '1,1,1,1,1,1' &&
      near(evenness(shares(['Here\nx\nWhat happened · what’s ahead · what you made of it · a choice · what matters · what you felt.'])), 1));
    if (mixed) {
      check('the mixed fixture: nine days, their places as the fixture gives them, in P6’s order',
        mixed.length === 9 && mixed.every(function (d) { return placesOf(d).join() === inOrder(d.places).join(); }));
      check('mixed, 9 days: touches 5,5,3,3,2,3 (21 in all)', counts(mixed).join() === '5,5,3,3,2,3');
      check('mixed, 9 days: shares are touches over 21', all(shares(mixed), [5, 5, 3, 3, 2, 3].map(function (x) { return x / 21; })));
      check('mixed, 9 days: evenness 48/49 (' + evenness(shares(mixed)).toFixed(4) + ')', near(evenness(shares(mixed)), 48 / 49));
      check('mixed, 3 days: touches 3,2,0,0,0,1 over 6; evenness 11/15',
        counts(mixed.slice(0, 3)).join() === '3,2,0,0,0,1' && near(evenness(shares(mixed.slice(0, 3))), 11 / 15));
    }
    check('the shadow: up at noon, left at six, right at six in the evening',
      shadowAngle(clock(12, 0)) === -90 && shadowAngle(clock(6, 0)) === -180 && shadowAngle(clock(18, 0)) === 0);
    check('the shadow at 19:30, and none at 05:59 or 23:00',
      shadowAngle(clock(19, 30)) === 22.5 && shadowAngle(clock(5, 59)) === null && shadowAngle(clock(23, 0)) === null);
    check('the empty heart’s centre: half size at 0.45; with days, 0.55 to 1 by evenness',
      centre(0, 0).scale === 0.5 && centre(0, 0).opacity === 0.45 && centre(3, 0).scale === 0.55 && centre(9, 1).scale === 1);
    check('the ring at 1, 3, 9, 10 and 81 days, and past it',
      [1, 3, 9, 10, 81, 90].map(function (n) { var r = ring(n); return r.inner + '/' + r.outer; }).join() === '1/0,3/0,0/1,1/1,0/9,0/9');
    var top = foldAt(0), turn = foldAt(360), fourth = foldAt(160);
    check('the fold: a spiral from the top at 200, widening to 214 at one full turn',
      near(top.x, CX) && near(top.y, CY - 200) && near(turn.x, CX) && near(turn.y, CY - 214) &&
      near(Math.hypot(fourth.x - CX, fourth.y - CY), 200 + 14 * 160 / 360));
    check('the fold is drawn only as far as the last nine: none before nine days, then to 40 x k degrees',
      foldPath(0) === '' && /^M 232\.0,-60\.0 /.test(foldPath(1)) &&
      foldPath(9).split(' L ').length === 91 && /L 232\.0,-74\.0$/.test(foldPath(9)) && /L [\d.-]+,[\d.-]+$/.test(foldPath(1)) &&
      foldPath(1).split(' L ').length === 11);
    check('the colour inside the ink: none for a place no day touched, 0.35 + 1.6 x share, at most 1',
      tint(0) === 0 && near(tint(1 / 9), 0.35 + 1.6 / 9) && tint(4 / 9) === 1);
    var addressed = days.map(function (d, i) { return { address: '20264131' + (i + 1) + '00', text: d.text }; });
    var lay = shellsAt(addressed, 350);
    check('the tideline: oldest left, newest right, within the width',
      lay.at.every(function (p, i) { return i === 0 || p.x > lay.at[i - 1].x; }) && lay.at[0].x === PAD && Math.abs(lay.at[8].x - (350 - PAD)) < 1e-9);
    check('the tideline lies the same way every time, each shell near the line and upright',
      JSON.stringify(shellsAt(addressed, 350)) === JSON.stringify(lay) &&
      lay.at.every(function (p) { return Math.abs(p.y - TIDE_BASE) <= 14 && p.turn === undefined; }));
    var many = shellsAt(new Array(40).fill(0).map(function (x, i) { return { address: String(2026000000 + i * 100), text: '' }; }), 350);
    check('forty days: the shells keep a finger apart, wider than a shell, and the tideline grows wider',
      many.width > 350 && Math.abs(many.at[1].x - many.at[0].x - SHELL) < 1e-9 && SHELL > 2 * SHELL_R);
    var ws = wedges(SHELL_R);
    function mid(w) {
      var m = /L (-?[\d.]+),(-?[\d.]+) A [\d.]+ [\d.]+ 0 0 1 (-?[\d.]+),(-?[\d.]+)/.exec(w.d);
      return Math.atan2((+m[2] + +m[4]) / 2, (+m[1] + +m[3]) / 2) * 180 / Math.PI;
    }
    check('a sundial shell: six wedges in P6’s order clockwise from Facts at the top',
      ws.map(function (w) { return w.n; }).join() === '1,6,3,5,4,2' &&
      ws.every(function (w, k) { return Math.abs(((mid(w) - (-90 + k * 60) + 540) % 360) - 180) < 0.5; }));
    var ps = poolsAt(days);
    function bearing(p) { return (Math.atan2(p.y - CY, p.x - CX) * 180 / Math.PI + 450) % 360; }
    check('hold: six pools in P6’s order, Facts at the top, then clockwise Outcomes, Fictions, Decision, Purpose, Feelings',
      ps.length === 6 && [1, 6, 3, 5, 4, 2].every(function (n, k) { return Math.abs(bearing(ps[n - 1]) - k * 60) < 1e-9; }));
    check('each pool sized by its share (by area); an empty pool a pale ring',
      Math.abs(ps[0].r - (POOL_MIN + (POOL_MAX - POOL_MIN) * Math.sqrt(4 / 9))) < 1e-9 && ps[3].r === POOL_EMPTY && ps[3].days === 0 &&
      ps.every(function (p) { return p.r <= POOL_MAX; }) && POOL_RING + POOL_MAX < RING);
    if (mixed) {
      var pm = poolsAt(mixed);
      check('mixed: a pool’s days are every day that touched it; sized by touches over all touches',
        pm.map(function (p) { return p.days; }).join() === '5,5,3,3,2,3' &&
        Math.abs(pm[4].r - (POOL_MIN + (POOL_MAX - POOL_MIN) * Math.sqrt(2 / 21))) < 1e-9);
    }
    var ok = results.every(Boolean);
    log(ok ? 'Mark: all ' + results.length + ' checks pass.' : 'Mark: a check failed.');
    return ok;
  }

  var api = {
    POOLS: POOLS,
    P6: P6,
    read: read,
    write: write,
    inOrder: inOrder,
    placesWords: placesWords,
    placesOf: placesOf,
    counts: counts,
    shares: shares,
    evenness: evenness,
    shadowAngle: shadowAngle,
    centre: centre,
    ring: ring,
    foldAt: foldAt,
    foldPath: foldPath,
    tint: tint,
    dial: dial,
    poolsAt: poolsAt,
    scatter: scatter,
    shellsAt: shellsAt,
    wedges: wedges,
    tideline: tideline,
    test: test
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Mark = api;
})(this);
