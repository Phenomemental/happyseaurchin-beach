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

  // ---- The day's light (6.9, after 8.77: a sundial, not a clock) ------------
  // As on a horizontal sundial with north at the top: the shadow's direction
  // for the hour h by the phone's clock is -90 + (h - 12) x 15 degrees from
  // the centre, so at noon it points up, in the morning left (west), in the
  // evening right (east); the sun is opposite. Daylight runs 06:00 to 19:30
  // (not true sunrise); outside it, null, and the dial glows instead.
  function hourOf(date) { return date.getHours() + date.getMinutes() / 60; }
  function shadowAngle(date) {
    var h = hourOf(date);
    if (h < 6 || h > 19.5) return null;
    return -90 + (h - 12) * 15;
  }
  // The sun's height: 0 at the ends of daylight, 1 at 12:45; null at night.
  function sunHeight(date) {
    var h = hourOf(date);
    if (h < 6 || h > 19.5) return null;
    return Math.sin(Math.PI * (h - 6) / 13.5);
  }
  // The shadow the heart casts: thrown along the shadow's direction by
  // 14 + 70 x (1 - s), long and soft at dawn and dusk, short at midday, at
  // ink 0.16 x min(1, 4s); null at night.
  function castOf(date) {
    var s = sunHeight(date);
    if (s === null) return null;
    var a = shadowAngle(date) * Math.PI / 180, L = 14 + 70 * (1 - s);
    return { dx: L * Math.cos(a), dy: L * Math.sin(a), length: L, opacity: 0.16 * Math.min(1, 4 * s) };
  }
  // The sunlight: its brightest point moved from the centre towards the sun
  // by 36 x (1 - s) + 10; warm apricot at dawn, pale sun by day, amber at
  // evening, by minute; strength 0.25 + 0.5 x s, times the days (all of it
  // at 27, never under 0.35 so the first day already has light); null at
  // night.
  var DAWN = '#F3CB8F', MIDDAY = '#F1E79B', EVENING = '#EDB98A';
  function mixHex(a, b, f) {
    var x = parseInt(a.slice(1), 16), y = parseInt(b.slice(1), 16), out = '#';
    [16, 8, 0].forEach(function (sh) {
      var v = Math.round(((x >> sh) & 255) * (1 - f) + ((y >> sh) & 255) * f);
      out += (v < 16 ? '0' : '') + v.toString(16).toUpperCase();
    });
    return out;
  }
  function lightOf(date, days) {
    var s = sunHeight(date);
    if (s === null) return null;
    var a = (shadowAngle(date) + 180) * Math.PI / 180, D = 36 * (1 - s) + 10;
    return { x: CX + D * Math.cos(a), y: CY + D * Math.sin(a), shift: D,
      colour: mixHex(hourOf(date) < 12.75 ? DAWN : EVENING, MIDDAY, s),
      strength: (0.25 + 0.5 * s) * Math.max(0.35, Math.min(1, (days || 0) / 27)) };
  }

  // The crimson centre: scaled 0.55 to 1 by evenness once there are days;
  // before any, half size at 0.45.
  function centre(days, even) {
    return days ? { scale: 0.55 + 0.45 * even, opacity: 1 } : { scale: 0.5, opacity: 0.45 };
  }

  // ---- The drawing ------------------------------------------------------------
  // dial(svg) dresses the heart already in the page (night-watchman.html's,
  // copied exactly, inside a group of class "breathe", with the wash, the
  // centre and the flow line marked by class) as a sundial, not a clock
  // (6.9, after 8.77): the sunlight behind the heart, the shadow the heart
  // casts, the night glow, and the spiral of days. It returns { time(date),
  // answering(on), days(list, kept, count), … }: time moves the light and
  // the shadow; answering sets the slow pulse along the line while Claude
  // answers (drawn by the page's CSS on the class); days takes each day's
  // text for the colour inside the ink (by share) and the centre (by
  // evenness), and draws the days kept as ink dots along the spiral. With
  // kept, the newest dot appears and the colour sweeps in along the stroke
  // (the page's CSS draws both, and holds still for reduced motion).
  var SVGNS = 'http://www.w3.org/2000/svg';
  var CX = 232, CY = 140;          // the dial's centre in the heart's own units
  var RING = 182, STRETCH = 160, MOMENTS = 166, LIGHT_R = 118;
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

  // ---- The spiral of days (6.9, after 8.71 and 8.77) --------------------------
  // No ticks and no ring: one faint open line winds clockwise out from the
  // top of the dial, day n (from 1) at -90 + 40 x (n - 1) degrees and radius
  // 182 + 0.3 x (n - 1), so the 81st lies at 206 and the line never closes.
  var SPIRAL_IN = 182, SPIRAL_STEP = 0.3;
  function dayAt(n) {
    return at(-90 + 40 * (n - 1), SPIRAL_IN + SPIRAL_STEP * (n - 1));
  }
  // The line, drawn only from day 1 to day n ('' before the second day).
  function spiralPath(n) {
    if (n < 2) return '';
    var pts = [];
    for (var k = 0; 1 + k / 10 < n; k++) pts.push(dayAt(1 + k / 10));
    pts.push(dayAt(n));
    return 'M ' + pts.map(function (p) { return p.x.toFixed(1) + ',' + p.y.toFixed(1); }).join(' L ');
  }
  // How day k of n is drawn: every ninth a slightly larger open circle; the
  // current turn (the latest nine days) full; earlier turns fine and faint.
  function dotOf(k, n) {
    if (k % 9 === 0) return { kind: 'ninth', r: 5 };
    if (k > n - 9) return { kind: 'full', r: 6, opacity: 1 };
    return { kind: 'faint', r: 2, opacity: 0.35 };
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
    // The sunlight reads as the sun, never a container: a soft radial light
    // with no outline, its brightest point moving towards the sun's side.
    var washFill = node('radialGradient', { id: id + '-wash', gradientUnits: 'userSpaceOnUse', cx: CX, cy: CY, r: LIGHT_R, fx: CX, fy: CY }, defs);
    var washStops = [[0, 1], [0.5, 0.7], [1, 0]].map(function (s) {
      return node('stop', { offset: s[0], 'stop-color': MIDDAY, 'stop-opacity': s[1] }, washFill);
    });
    // The heart's shadow is its own ink, softened.
    var blur = node('filter', { id: id + '-blur', x: '-40%', y: '-40%', width: '180%', height: '180%' }, defs);
    node('feGaussianBlur', { stdDeviation: 4 }, blur);
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

    var wash = art.querySelector('.wash');
    if (wash) {
      wash.setAttribute('fill', 'url(#' + id + '-wash)');
      wash.setAttribute('rx', LIGHT_R + 6);
      wash.setAttribute('ry', LIGHT_R + 6);
      wash.removeAttribute('stroke');
    }
    // The shadow: a copy of the heart's two ink paths (not the crimson),
    // beneath the ink and over the light, blurred, thrown away from the sun.
    var cast = node('g', { 'class': 'cast', filter: 'url(#' + id + '-blur)', opacity: 0 });
    Array.prototype.forEach.call(art.querySelectorAll('path'), function (p) {
      if (p.getAttribute('fill') === 'var(--ink)') cast.appendChild(node('path', { d: p.getAttribute('d'), fill: 'var(--ink)' }));
    });
    if (wash && wash.nextSibling) art.insertBefore(cast, wash.nextSibling);
    else art.insertBefore(cast, under.nextSibling);
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

    // The spiral of days: the line, then a dot for each day kept.
    var spiral = node('path', { 'class': 'spiral', d: '', fill: 'none', stroke: 'var(--ink)', 'stroke-opacity': '0.15',
      'stroke-width': 1.5, 'stroke-linecap': 'round' }, art);
    var kept_ = node('g', { 'class': 'days-kept' }, art);
    var momentsAt = node('g', { 'class': 'sd-moments' }, art);
    var pooled = node('g', { 'class': 'sd-tidepools' }, svg);

    // The light and the shadow at this minute; at night, the glow instead.
    var now = null, count = 0;
    function light() {
      if (!now) return;
      var c = castOf(now), l = lightOf(now, count);
      glow.setAttribute('visibility', l ? 'hidden' : 'visible');
      cast.style.opacity = c ? c.opacity.toFixed(3) : '0';
      if (c) cast.style.transform = 'translate(' + c.dx.toFixed(1) + 'px, ' + c.dy.toFixed(1) + 'px)';
      if (wash) wash.setAttribute('opacity', l ? l.strength.toFixed(3) : '0');
      if (l) {
        washFill.setAttribute('fx', l.x.toFixed(1));
        washFill.setAttribute('fy', l.y.toFixed(1));
        washStops.forEach(function (s) { s.setAttribute('stop-color', l.colour); });
      }
    }
    function time(date) {
      now = date;
      light();
    }
    function answering(on) {
      svg.classList.toggle('answering', !!on);
    }
    // `count` is how many days are kept (days with a line or moments; the
    // list's length when not given); `ahead`, that the latest began from
    // tomorrow's line and has no words yet.
    function days(list, kept, opts) {
      list = list || [];
      opts = opts || {};
      var n = opts.count != null ? opts.count : list.length, s = shares(list), even = evenness(s);
      count = n;
      while (kept_.firstChild) kept_.removeChild(kept_.firstChild);
      spiral.setAttribute('d', spiralPath(n));
      // Before the first day, one faint point where the spiral will begin.
      if (!n) {
        var o = dayAt(1);
        node('circle', { 'class': 'origin', cx: o.x.toFixed(1), cy: o.y.toFixed(1), r: 1.8, fill: 'var(--ink)', 'fill-opacity': '0.3' }, kept_);
      }
      // A dot says only that a day was kept: ink, never a place's colour.
      for (var k = 1; k <= n; k++) {
        var c = dayAt(k), d = dotOf(k, n), cls = 'mark day ' + d.kind + (kept && k === n ? ' new' : '');
        node('circle', d.kind === 'ninth'
          ? { 'class': cls, cx: c.x.toFixed(1), cy: c.y.toFixed(1), r: d.r, fill: 'none', stroke: 'var(--ink)', 'stroke-width': 1.5,
            'stroke-opacity': k === n && opts.ahead ? 0.4 : 1 }
          : { 'class': cls, cx: c.x.toFixed(1), cy: c.y.toFixed(1), r: d.r, fill: 'var(--ink)',
            'fill-opacity': k === n && opts.ahead ? 0.4 : d.opacity }, kept_);
      }
      stretches.forEach(function (el, k) { el.setAttribute('opacity', tint(s[P6[k] - 1]).toFixed(3)); });
      light();
      var cen = centre(n, even);
      if (dot) {
        dot.setAttribute('transform', 'rotate(-18 250 138) translate(250 138) scale(' + cen.scale + ') translate(-250 -138)');
        dot.setAttribute('opacity', cen.opacity);
      }
      if (halo) halo.setAttribute('opacity', n ? (0.18 * (1 - even)).toFixed(3) : 0);
      svg.setAttribute('aria-label', n
        ? 'A sundial: a heart drawn in ink, casting its shadow at this hour, ' + n + (n === 1 ? ' day' : ' days') + ' kept along a spiral'
        : 'A sundial: a heart drawn in ink, casting its shadow at this hour, no days kept yet');
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
      var angles = (list || []).map(function (m) { return -90 + (hourOf(m.when) - 12) * 15; });
      if (angles.length) {
        // A faint arc beneath them, only as wide as today's moments.
        var a0 = Math.min.apply(null, angles) - 8, a1 = Math.max.apply(null, angles) + 8, pts = [];
        for (var a = a0; a < a1; a += 2) pts.push(at(a, MOMENTS));
        pts.push(at(a1, MOMENTS));
        node('path', { 'class': 'moment-arc', d: 'M ' + pts.map(function (p) { return p.x.toFixed(1) + ',' + p.y.toFixed(1); }).join(' L '),
          fill: 'none', stroke: 'var(--ink)', 'stroke-opacity': '0.12', 'stroke-width': 1 }, momentsAt);
      }
      angles.forEach(function (ang) {
        var c = at(ang, MOMENTS);
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

  // ---- The strandline's shells (6.7, after 8.77) ------------------------------
  // A day's shell is the sundial shell as it is in nature, seen from above:
  // a flat spiral of about two and a half turns from its centre, in ink.
  // Where the day touched a place, a stretch of the coil carries a soft band
  // of that place's hue beneath the ink line, the bands in P6's order from
  // the centre outward, each touched place an equal share of the coil's
  // length with a small gap between; a day of one place is banded along
  // its whole coil. Untouched places are not drawn: nothing pale, nothing
  // empty. A tiny crimson point at the centre. Drawn in -30..30.
  var COIL_R = 25, COIL_TURNS = 2.5, COIL_STEPS = 300, COIL_GAP = 0.03;
  function coilPoints() {
    var pts = [], len = 0;
    for (var i = 0; i <= COIL_STEPS; i++) {
      var f = i / COIL_STEPS, a = (-90 + f * COIL_TURNS * 360) * Math.PI / 180, r = COIL_R * (0.1 + 0.9 * f);
      var p = { x: r * Math.cos(a), y: r * Math.sin(a) };
      if (pts.length) len += Math.hypot(p.x - pts[pts.length - 1].x, p.y - pts[pts.length - 1].y);
      p.len = len;
      pts.push(p);
    }
    pts.forEach(function (q) { q.at = q.len / len; });
    return pts;
  }
  // The bands for a day's places, as fractions of the coil's length.
  function coilBands(places) {
    var ps = inOrder(places), n = ps.length;
    return ps.map(function (q, k) {
      return { n: q, from: k / n + (k ? COIL_GAP / 2 : 0), to: (k + 1) / n - (k < n - 1 ? COIL_GAP / 2 : 0) };
    });
  }
  function coilPath(pts, from, to) {
    var on = pts.filter(function (q) { return q.at >= from - 1e-9 && q.at <= to + 1e-9; });
    return on.length > 1 ? 'M ' + on.map(function (q) { return q.x.toFixed(2) + ',' + q.y.toFixed(2); }).join(' L ') : '';
  }
  function coil(svg, places) {
    while (svg.firstChild) svg.removeChild(svg.firstChild);
    svg.setAttribute('viewBox', '-30 -30 60 60');
    var pts = coilPoints();
    coilBands(places).forEach(function (b) {
      node('path', { 'class': 'band', 'data-place': b.n, d: coilPath(pts, b.from, b.to), fill: 'none', stroke: POOLS[b.n - 1].hue,
        'stroke-width': 5, 'stroke-opacity': 0.75, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, svg);
    });
    node('path', { 'class': 'coil', d: coilPath(pts, 0, 1), fill: 'none', stroke: 'var(--ink)', 'stroke-width': 1.2,
      'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, svg);
    node('circle', { 'class': 'presence', r: 1.3, fill: 'var(--crimson)' }, svg);
  }

  // ---- The console check ------------------------------------------------------
  // Mark.test(days, log, mixed) with the trial fixture (fixtures/days-trial.json,
  // nine days of one place each, the older 'A day of …' line: pools
  // 1,1,3,2,1,3,5,6,1) and the mixed fixture (fixtures/days-mixed.json, nine
  // days touching 1+2, 1, 1+6+2, 3, 1+3+4, 2, 5+6, all six, 4+2): the text read
  // and written, the touches, shares and evenness, against sums worked by
  // hand; the spiral of days, the light and the cast shadow, the coiled
  // shells, the pools' order.
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
    check('the shadow’s direction: up at noon, left at six, right at six in the evening',
      shadowAngle(clock(12, 0)) === -90 && shadowAngle(clock(6, 0)) === -180 && shadowAngle(clock(18, 0)) === 0);
    check('daylight 06:00 to 19:30: none at 05:59 or 23:00',
      shadowAngle(clock(19, 30)) === 22.5 && shadowAngle(clock(5, 59)) === null && sunHeight(clock(23, 0)) === null && castOf(clock(21, 0)) === null && lightOf(clock(21, 0), 9) === null);
    check('the sun’s height: 0 at 06:00 and 19:30, 1 at 12:45',
      near(sunHeight(clock(6, 0)), 0) && Math.abs(sunHeight(clock(19, 30))) < 1e-12 && near(sunHeight(clock(12, 45)), 1));
    var c1245 = castOf(clock(12, 45)), c0630 = castOf(clock(6, 30)), c1700 = castOf(clock(17, 0));
    check('the cast shadow: short beneath the heart at 12:45 (14, at 0.16), long and faint at dawn, thrown left in the morning, right in the evening',
      near(c1245.length, 14) && near(c1245.opacity, 0.16) && c0630.length > 70 && c0630.opacity < 0.16 && c0630.dx < 0 && c1700.dx > 0 && c1245.dy < 0);
    var l0900 = lightOf(clock(9, 0), 27), l1245 = lightOf(clock(12, 45), 27), l1700 = lightOf(clock(17, 0), 27);
    var sunAt = function (l, h, m) { return Math.atan2(l.y - CY, l.x - CX) * 180 / Math.PI - (shadowAngle(clock(h, m)) + 180); };
    check('the light: moved towards the sun, opposite the shadow (right in the morning, left in the evening), 10 from the centre at 12:45',
      l0900.x > CX && l1700.x < CX && near(Math.hypot(l1245.x - CX, l1245.y - CY), 10) &&
      [[l0900, 9, 0], [l1245, 12, 45], [l1700, 17, 0]].every(function (x) { var d = (sunAt(x[0], x[1], x[2]) % 360 + 360) % 360; return d < 1e-6 || d > 360 - 1e-6; }));
    check('the light’s colour: apricot at dawn, pale sun at 12:45, amber at dusk, by minute',
      lightOf(clock(6, 0), 27).colour === '#F3CB8F' && l1245.colour === '#F1E79B' && lightOf(clock(19, 29), 27).colour.slice(0, 3) === '#ED' &&
      mixHex('#000000', '#FFFFFF', 0.5) === '#808080');
    check('the light’s strength: (0.25 + 0.5s) x days, all of it at 27, never under 0.35',
      near(l1245.strength, 0.75) && near(lightOf(clock(12, 45), 1).strength, 0.75 * 0.35) && near(lightOf(clock(12, 45), 18).strength, 0.75 * 18 / 27));
    check('the empty heart’s centre: half size at 0.45; with days, 0.55 to 1 by evenness',
      centre(0, 0).scale === 0.5 && centre(0, 0).opacity === 0.45 && centre(3, 0).scale === 0.55 && centre(9, 1).scale === 1);
    var d1 = dayAt(1), d10 = dayAt(10), d81 = dayAt(81);
    check('the spiral of days: day 1 at the top at 182, day 10 back at the top a little further out, day 81 at 206',
      near(d1.x, CX) && near(d1.y, CY - 182) && near(d10.x, CX) && near(d10.y, CY - 182.3 * 1 - 2.4) &&
      near(Math.hypot(d81.x - CX, d81.y - CY), 206) && near(Math.atan2(dayAt(2).y - CY, dayAt(2).x - CX) * 180 / Math.PI, -50));
    check('the line is drawn only from day 1 to the latest day; none before the second',
      spiralPath(0) === '' && spiralPath(1) === '' && spiralPath(2).split(' L ').length === 11 &&
      spiralPath(81).endsWith(' ' + dayAt(81).x.toFixed(1) + ',' + dayAt(81).y.toFixed(1)) && spiralPath(81).indexOf('M 232.0,-42.0 ') === 0);
    check('the dots: the latest nine full (r 6), earlier fine and faint (r 2 at 35%), every ninth an open circle (r 5)',
      dotOf(22, 22).kind === 'full' && dotOf(14, 22).kind === 'full' && dotOf(13, 22).kind === 'faint' && dotOf(13, 22).r === 2 && dotOf(13, 22).opacity === 0.35 &&
      dotOf(9, 22).kind === 'ninth' && dotOf(18, 22).r === 5 && dotOf(81, 81).kind === 'ninth' && dotOf(1, 1).r === 6);
    check('the colour inside the ink: none for a place no day touched, 0.35 + 1.6 x share, at most 1',
      tint(0) === 0 && near(tint(1 / 9), 0.35 + 1.6 / 9) && tint(4 / 9) === 1);
    var pts = coilPoints();
    check('a shell’s coil: two and a half turns from the centre out to its rim',
      near(pts[0].x, 0) && near(pts[0].y, -2.5) && near(Math.hypot(pts[pts.length - 1].x, pts[pts.length - 1].y), COIL_R) && near(pts[pts.length - 1].at, 1));
    var three = coilBands([2, 6, 1]), one = coilBands([4]);
    check('its bands: the places touched, in P6’s order from the centre out, equal shares of the coil’s length, a small gap between',
      three.map(function (b) { return b.n; }).join() === '1,6,2' && near(three[0].from, 0) && near(three[2].to, 1) &&
      near(three[1].to - three[1].from, 1 / 3 - COIL_GAP) && near(three[1].from - three[0].to, COIL_GAP) &&
      one.length === 1 && near(one[0].from, 0) && near(one[0].to, 1) && coilBands([]).length === 0);
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
    tint: tint,
    dial: dial,
    poolsAt: poolsAt,
    sunHeight: sunHeight,
    castOf: castOf,
    lightOf: lightOf,
    dayAt: dayAt,
    spiralPath: spiralPath,
    dotOf: dotOf,
    coilBands: coilBands,
    coil: coil,
    test: test
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Mark = api;
})(this);
