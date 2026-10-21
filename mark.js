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
  function poolOf(day) {
    var text = typeof day === 'string' ? day : day && typeof day.text === 'string' ? day.text : '';
    return read(text).pool;
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
  // returns { time(date), answering(on) }: time moves the shadow, and
  // answering sets the slow pulse along the line while Claude answers
  // (drawn by the page's CSS on the class). Days lighting the marks come
  // with keeping.
  var SVGNS = 'http://www.w3.org/2000/svg';
  var CX = 232, CY = 140;          // the dial's centre in the heart's own units
  var RING = 186, SHADOW = 160;
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
  function dial(svg) {
    var art = svg.querySelector('.breathe');
    var id = 'sd' + Math.random().toString(36).slice(2, 8);
    var defs = node('defs', {});
    svg.insertBefore(defs, svg.firstChild);
    var glowFill = node('radialGradient', { id: id + '-glow' }, defs);
    node('stop', { offset: '0', 'stop-color': 'var(--wash)', 'stop-opacity': '0.3' }, glowFill);
    node('stop', { offset: '1', 'stop-color': 'var(--wash)', 'stop-opacity': '0' }, glowFill);

    var under = node('g', { 'class': 'under' });
    art.insertBefore(under, art.firstChild);
    var glow = node('circle', { 'class': 'glow', cx: CX, cy: CY, r: 120, fill: 'url(#' + id + '-glow)' }, under);
    var shadow = node('line', { 'class': 'shadow', x1: CX, y1: CY, x2: CX, y2: CY - SHADOW,
      stroke: 'var(--ink)', 'stroke-opacity': '0.1', 'stroke-width': 7, 'stroke-linecap': 'round' }, under);

    var ring = node('g', { 'class': 'ring' }, art);
    for (var i = 0; i < 9; i++) {
      var a = -90 + i * 40, p = at(a, RING - 7), q = at(a, RING + 7);
      node('line', { x1: p.x.toFixed(1), y1: p.y.toFixed(1), x2: q.x.toFixed(1), y2: q.y.toFixed(1),
        stroke: 'var(--ink)', 'stroke-opacity': '0.55', 'stroke-width': 2.5, 'stroke-linecap': 'round' }, ring);
    }

    var c = centre(0, 0), dot = art.querySelector('.centre');
    if (dot) {
      dot.setAttribute('transform', 'rotate(-18 250 138) translate(250 138) scale(' + c.scale + ') translate(-250 -138)');
      dot.setAttribute('opacity', c.opacity);
    }

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
    return { time: time, answering: answering };
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
    dial: dial,
    test: test
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Mark = api;
})(this);
