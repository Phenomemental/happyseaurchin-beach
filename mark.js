/* Sundial's mark: the six places a day can stand in, a day as the plain
   text its one slot holds, the sums the drawing is made from, and the
   drawing (the plan on the beach at spine:front-door, 7.142 and 7.16 to
   7.17). One table holds each place's hue, the quiet line beneath a
   sheet, the question an empty pool asks, and where its Beings stand in
   beings:night-watchman; the Beings themselves are always read from the
   beach, never written here. Everything but dial() runs without a page. */
(function (root) {
  // In pool order, 1 to 6: the order of the colour along the heart's
  // stroke and of the tide-pools round the ring, Facts at the top.
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
    { n: 6, name: 'Outcomes', hue: '#5B7F95', phrase: 'what’s ahead', at: '2.3',
      question: 'What are you expecting, or fearing?' }
  ];

  // ---- A day, as its slot holds it --------------------------------------------
  // One plain text, line by line: "place — title", the lines, "Tomorrow: …"
  // when they voiced one, and last the pool's quiet line, "A day of what
  // happened." A day written elsewhere (in Claude, say) may be one line
  // with none of these; it is still a day, with no colour.
  function plainPhrase(s) {
    return String(s).toLowerCase().replace(/[’‘`]/g, '\'').replace(/\s+/g, ' ').trim();
  }
  function poolByPhrase(s) {
    var want = plainPhrase(s);
    for (var i = 0; i < POOLS.length; i++) if (plainPhrase(POOLS[i].phrase) === want) return POOLS[i].n;
    return 0;
  }

  // A sheet as Claude returns it ({title, lines, tomorrow, pool}) and the
  // place, as the text of the day's slot.
  function write(sheet, place) {
    var rows = [(place ? place + ' — ' : '') + String(sheet.title || '').trim()];
    (sheet.lines || []).forEach(function (l) {
      if (String(l).trim()) rows.push(String(l).trim());
    });
    var tomorrow = String(sheet.tomorrow || '').trim().replace(/^tomorrow:\s*/i, '');
    if (tomorrow) rows.push('Tomorrow: ' + tomorrow);
    var pool = POOLS[Number(sheet.pool) - 1];
    if (pool) rows.push('A day of ' + pool.phrase + '.');
    return rows.join('\n');
  }

  // The text of a day's slot, read back as {place, title, lines, tomorrow,
  // pool}; the pool is read only from a last line that names one.
  function read(text) {
    var rows = String(text || '').split('\n').map(function (r) { return r.trim(); }).filter(Boolean);
    var day = { place: '', title: '', lines: [], tomorrow: '', pool: 0 };
    if (!rows.length) return day;
    var last = /^a day of (.+?)\.?$/i.exec(rows[rows.length - 1]);
    if (rows.length > 1 && last && poolByPhrase(last[1])) {
      day.pool = poolByPhrase(last[1]);
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

  // The pool a day stood in, 1 to 6, from its text (or {text}); 0 when it
  // names none.
  // A day's pool: its own quiet line, else the place this browser was given
  // for it (a day kept through Claude has no line; plan 6.8.2).
  function poolOf(day) {
    var text = typeof day === 'string' ? day : day && typeof day.text === 'string' ? day.text : '';
    var own = read(text).pool, placed = day && typeof day === 'object' ? Number(day.pool) : 0;
    return own || (placed >= 1 && placed <= 6 ? placed : 0);
  }

  // ---- The sums ---------------------------------------------------------------
  // How many days stood in each pool, in pool order.
  function counts(days) {
    var c = [0, 0, 0, 0, 0, 0];
    (days || []).forEach(function (d) {
      var n = poolOf(d);
      if (n) c[n - 1]++;
    });
    return c;
  }

  // Each pool's share of the days, summing to 1; all nought before any day.
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
  var RING = 186, OUTER = 208, SHADOW = 160, STRETCH = 160;
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

  // How the ring stands for n days: the marks lit on the inner ring (the
  // days since the last full nine) and the open marks on the outer ring
  // (one per nine, up to nine of them: eighty-one days).
  function ring(n) {
    return { inner: n % 9, outer: Math.min(9, Math.floor(n / 9)) };
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
    var dot = art.querySelector('.centre');
    var halo = null;
    if (dot) {
      halo = node('ellipse', { 'class': 'halo', cx: 250, cy: 138, rx: 64, ry: 40, fill: 'var(--crimson)', opacity: 0,
        transform: 'rotate(-18 250 138)' });
      art.insertBefore(halo, dot);
    }

    // The colour inside the ink: six stretches along the line, in pool order
    // from where it comes in (top right) to where it leaves (bottom left).
    var tints = node('g', { 'class': 'tints', mask: 'url(#' + id + '-sweep)' });
    if (flow) art.insertBefore(tints, flow);
    else art.appendChild(tints);
    var stretches = POOLS.map(function (p, i) {
      return node('path', { d: path, fill: 'none', stroke: p.hue, 'stroke-width': 7, 'stroke-linecap': 'butt',
        pathLength: 1000, 'stroke-dasharray': STRETCH + ' ' + (1000 - STRETCH), 'stroke-dashoffset': -i * STRETCH, opacity: 0 }, tints);
    });

    var marks = node('g', { 'class': 'ring' }, art);
    for (var i = 0; i < 9; i++) {
      var a = -90 + i * 40, p = at(a, RING - 7), q = at(a, RING + 7);
      node('line', { x1: p.x.toFixed(1), y1: p.y.toFixed(1), x2: q.x.toFixed(1), y2: q.y.toFixed(1),
        stroke: 'var(--ink)', 'stroke-opacity': '0.55', 'stroke-width': 2.5, 'stroke-linecap': 'round' }, marks);
    }
    var lit = node('g', { 'class': 'lit' }, art);
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
      list.slice(n - r.inner).forEach(function (d, i) {
        var pool = POOLS[poolOf(d) - 1], c = at(-90 + i * 40, RING);
        node('circle', { cx: c.x.toFixed(1), cy: c.y.toFixed(1), r: 7, fill: pool ? pool.hue : 'var(--muted)',
          'class': kept && i === r.inner - 1 ? 'mark new' : 'mark' }, lit);
      });
      for (var k = 0; k < r.outer; k++) {
        var o = at(-90 + k * 40, OUTER);
        node('circle', { cx: o.x.toFixed(1), cy: o.y.toFixed(1), r: 4, fill: 'none', stroke: 'var(--ink)',
          'stroke-opacity': '0.5', 'stroke-width': 1.5, 'class': kept && r.inner === 0 && k === r.outer - 1 ? 'mark new' : 'mark' }, lit);
      }
      stretches.forEach(function (el, i) { el.setAttribute('opacity', tint(s[i]).toFixed(3)); });
      if (wash) wash.setAttribute('opacity', (0.6 * Math.min(1, n / 27)).toFixed(3));
      var c = centre(n, even);
      if (dot) {
        dot.setAttribute('transform', 'rotate(-18 250 138) translate(250 138) scale(' + c.scale + ') translate(-250 -138)');
        dot.setAttribute('opacity', c.opacity);
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
      poolsAt(list).forEach(function (p, i) {
        var pool = POOLS[i];
        var g = node('g', { 'class': 'sd-tidepool' + (p.days ? '' : ' empty'), role: 'button', tabindex: 0,
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
      Array.prototype.forEach.call(pooled.querySelectorAll('.sd-tidepool'), function (g, k) { g.classList.toggle('picked', k === i); });
    }
    days([]);
    return { time: time, answering: answering, days: days, pools: pools, gather: gather, picked: picked };
  }

  // ---- Hold: the pools -----------------------------------------------------------
  // The six tide-pools in a ring round the centre (6.8), Facts at the top and
  // clockwise, each sized by its share of the days (its area, so a pool
  // holding twice the days looks twice as full); an empty pool is a pale
  // ring of its own small size.
  var POOL_RING = 128, POOL_EMPTY = 14, POOL_MIN = 16, POOL_MAX = 50;
  function poolsAt(list) {
    var s = shares(list || []), c = counts(list || []);
    return POOLS.map(function (p, i) {
      var at_ = at(-90 + i * 60, POOL_RING);
      return { n: p.n, x: at_.x, y: at_.y, days: c[i], share: s[i],
        r: s[i] > 0 ? POOL_MIN + (POOL_MAX - POOL_MIN) * Math.sqrt(s[i]) : POOL_EMPTY };
    });
  }

  // ---- The tideline -------------------------------------------------------------
  // tideline(svg, list, width, label, pick) lays the days along a faint
  // tideline (6.7): one shell per day, oldest left and newest right, each in
  // its pool's hue (a day with no colour line in the muted ink), set a little
  // above or below the line and turned a little by its own address, so the
  // same days always lie the same way. Each shell is a button named by
  // label(day); pick(i) runs when one is tapped or chosen with Enter. The
  // shells keep at least a finger's width apart, so a long tideline grows
  // wider than the screen; the width drawn is returned.
  var SHELL = 26, PAD = 22, TIDE_H = 120, TIDE_BASE = 62;
  function scatter(address) {
    var h = 2166136261;
    String(address).split('').forEach(function (c) {
      h ^= c.charCodeAt(0);
      h = Math.imul(h, 16777619) >>> 0;
    });
    return { dy: (h % 29) - 14, turn: ((h >>> 8) % 51) - 25 };
  }
  function shellsAt(list, width) {
    var n = list.length;
    var step = n > 1 ? Math.max(SHELL, (width - 2 * PAD) / (n - 1)) : 0;
    var w = n > 1 ? Math.max(width, 2 * PAD + step * (n - 1)) : width;
    return { width: w, at: list.map(function (day, i) {
      var s = scatter(day.address);
      return { x: n > 1 ? PAD + step * i : w / 2, y: TIDE_BASE + s.dy, turn: s.turn };
    }) };
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
      var p = lay.at[i], pool = POOLS[poolOf(day) - 1];
      var g = node('g', { 'class': 'sd-shell', role: 'button', tabindex: 0, 'aria-label': label(day),
        transform: 'translate(' + p.x.toFixed(1) + ' ' + p.y.toFixed(1) + ') rotate(' + p.turn + ')' }, svg);
      node('circle', { 'class': 'sd-shell-hit', r: 17, fill: 'transparent' }, g);
      node('path', { 'class': 'sd-shell-body', d: 'M 0 10 L -11 -2 A 12 12 0 0 1 11 -2 Z', fill: pool ? pool.hue : 'var(--muted)' }, g);
      ['M 0 10 L -5.5 -8', 'M 0 10 L 0 -10', 'M 0 10 L 5.5 -8'].forEach(function (r) {
        node('path', { d: r, fill: 'none', stroke: 'var(--page)', 'stroke-width': 1, 'stroke-opacity': 0.75 }, g);
      });
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
  // Mark.test(days) with the trial fixture (fixtures/days-trial.json, nine
  // days whose pools are 1,1,3,2,1,3,5,6,1): the day text read and written,
  // the counts, shares and evenness after the first 1, 3 and 9 days and at
  // 27, against sums worked by hand, and where the shadow falls.
  function test(days, log) {
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
    check('the fixture holds nine days, pools 1,1,3,2,1,3,5,6,1',
      days.length === 9 && days.map(poolOf).join() === '1,1,3,2,1,3,5,6,1');
    var last = read(days[8].text);
    check('a day reads back as place, title, lines, tomorrow and pool',
      last.place === 'Arona' && last.title === 'Lizards and the first quiet' && last.lines.length === 1 &&
      last.tomorrow === 'walk up to the old town before it gets hot.' && last.pool === 1);
    var sheet = { title: 'Lizards and the first quiet', lines: ['A slow morning.', 'Your youngest laughing.'],
      tomorrow: 'Tomorrow: walk up early.', pool: 6 };
    var text = write(sheet, 'Arona');
    check('a sheet writes as the slot’s text, line by line',
      text === 'Arona — Lizards and the first quiet\nA slow morning.\nYour youngest laughing.\nTomorrow: walk up early.\nA day of what’s ahead.');
    var back = read(text);
    check('and reads back the same', back.place === 'Arona' && back.title === sheet.title &&
      back.lines.join('|') === sheet.lines.join('|') && back.tomorrow === 'walk up early.' && back.pool === 6);
    check('a straight apostrophe names the same pool', poolOf('Here — A day\nSome words.\nA day of what\'s ahead.') === 6);
    check('a day of one plain line has a title and no colour',
      read('Into bed early, quietly relieved.').title === 'Into bed early, quietly relieved.' && poolOf('Into bed early.') === 0);
    check('a line of the person’s own that begins "A day of" is not a pool unless it is last and names one',
      poolOf('Home — Rain\nA day of rain, mostly.') === 0);
    check('the table names six pools in order, with the plan’s hues',
      POOLS.map(function (p) { return p.n + p.hue; }).join() ===
        '1#4A6B8A,2#B0413E,3#7A5C99,4#2E7D6B,5#B8860B,6#5B7F95');
    check('no days: no shares, evenness 0', all(shares([]), [0, 0, 0, 0, 0, 0]) && evenness(shares([])) === 0);
    check('1 day: all in what happened, evenness 0',
      all(shares(days.slice(0, 1)), [1, 0, 0, 0, 0, 0]) && near(evenness(shares(days.slice(0, 1))), 0));
    check('3 days: shares 2/3 and 1/3, evenness 8/15',
      all(shares(days.slice(0, 3)), [2 / 3, 0, 1 / 3, 0, 0, 0]) && near(evenness(shares(days.slice(0, 3))), 8 / 15));
    check('9 days: counts 4,1,2,0,1,1', counts(days).join() === '4,1,2,0,1,1');
    check('9 days: shares 4/9, 1/9, 2/9, 0, 1/9, 1/9',
      all(shares(days), [4 / 9, 1 / 9, 2 / 9, 0, 1 / 9, 1 / 9]));
    check('9 days: evenness 116/135 (' + evenness(shares(days)).toFixed(4) + ')',
      near(evenness(shares(days)), 116 / 135));
    check('27 days: the same shares and evenness as nine',
      counts(thrice).join() === '12,3,6,0,3,3' && near(evenness(shares(thrice)), 116 / 135));
    check('one day in each pool: evenness 1',
      near(evenness(shares([1, 2, 3, 4, 5, 6].map(function (n) { return 'Here — A day\nA day of ' + POOLS[n - 1].phrase + '.'; }))), 1));
    check('the shadow: up at noon, left at six, right at six in the evening',
      shadowAngle(clock(12, 0)) === -90 && shadowAngle(clock(6, 0)) === -180 && shadowAngle(clock(18, 0)) === 0);
    check('the shadow at 19:30, and none at 05:59 or 23:00',
      shadowAngle(clock(19, 30)) === 22.5 && shadowAngle(clock(5, 59)) === null && shadowAngle(clock(23, 0)) === null);
    check('the empty heart’s centre: half size at 0.45; with days, 0.55 to 1 by evenness',
      centre(0, 0).scale === 0.5 && centre(0, 0).opacity === 0.45 && centre(3, 0).scale === 0.55 && centre(9, 1).scale === 1);
    check('the ring at 1, 3, 9, 10 and 81 days, and past it',
      [1, 3, 9, 10, 81, 90].map(function (n) { var r = ring(n); return r.inner + '/' + r.outer; }).join() === '1/0,3/0,0/1,1/1,0/9,0/9');
    check('the colour inside the ink: none for an empty pool, 0.35 + 1.6 x share, at most 1',
      tint(0) === 0 && near(tint(1 / 9), 0.35 + 1.6 / 9) && tint(4 / 9) === 1);
    var addressed = days.map(function (d, i) { return { address: '20264131' + (i + 1) + '00', text: d.text }; });
    var lay = shellsAt(addressed, 350);
    check('the tideline: oldest left, newest right, within the width',
      lay.at.every(function (p, i) { return i === 0 || p.x > lay.at[i - 1].x; }) && lay.at[0].x === PAD && Math.abs(lay.at[8].x - (350 - PAD)) < 1e-9);
    check('the tideline lies the same way every time, each shell near the line',
      JSON.stringify(shellsAt(addressed, 350)) === JSON.stringify(lay) &&
      lay.at.every(function (p) { return Math.abs(p.y - TIDE_BASE) <= 14 && Math.abs(p.turn) <= 25; }));
    var many = shellsAt(new Array(40).fill(0).map(function (x, i) { return { address: String(2026000000 + i * 100), text: '' }; }), 350);
    check('forty days: the shells keep a finger apart and the tideline grows wider', many.width > 350 && Math.abs(many.at[1].x - many.at[0].x - SHELL) < 1e-9);
    var ps = poolsAt(days);
    check('hold: six pools in a ring, Facts at the top and clockwise',
      ps.length === 6 && Math.abs(ps[0].x - CX) < 1e-9 && ps[0].y < CY && ps[1].x > CX && ps[3].y > CY && ps[5].x < CX);
    check('each pool sized by its share (by area); an empty pool a pale ring',
      Math.abs(ps[0].r - (POOL_MIN + (POOL_MAX - POOL_MIN) * Math.sqrt(4 / 9))) < 1e-9 && ps[3].r === POOL_EMPTY && ps[3].days === 0 &&
      ps.every(function (p) { return p.r <= POOL_MAX; }) && POOL_RING + POOL_MAX < RING);
    var ok = results.every(Boolean);
    log(ok ? 'Mark: all ' + results.length + ' checks pass.' : 'Mark: a check failed.');
    return ok;
  }

  var api = {
    POOLS: POOLS,
    read: read,
    write: write,
    poolOf: poolOf,
    counts: counts,
    shares: shares,
    evenness: evenness,
    shadowAngle: shadowAngle,
    centre: centre,
    ring: ring,
    tint: tint,
    dial: dial,
    poolsAt: poolsAt,
    scatter: scatter,
    shellsAt: shellsAt,
    tideline: tideline,
    test: test
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Mark = api;
})(this);
