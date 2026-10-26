/* app.js — Community Recovery's own app (app.html), built to the plan at
   spine:leros-on-the-commons 1.172 and its decisions at 1.17218.

   A member's card or link opens this page with ?from=<their name>. At the table a newcomer gives
   a name and three words (Join us's own way, through kit.js) and is in; the arrival is added to
   the open block `arrivals`. Then the app carries the rest, as they want it: keeping their days
   (sealed with their own words before they leave the phone), what they can offer and could use,
   crossings and words between members, organisations joining through a person, and Community
   Recovery's own pages shown as runs of small pages, opening one by one over the first nine days
   kept (1.17222).

   Every read and write goes to Community Recovery's own place on the beach (config.js), through
   kit.js and beach.js. Nothing here calls any other service, and no AI is called from this page.
   It stands alone: it loads nothing from Matthew's site and links nowhere outside this folder but
   help lines and Community Recovery's Facebook.

   THE BLOCKS IT USES, all in Community Recovery's place:
     passport:<name>     made by kit.js foundName, latched with the three words. Position 1 is
                         'What I can offer' (the existing 'what I'd bring' line); position 3 is
                         'What I could use'. Both open for everyone to read.
     now:<name>          the person's kept days, in the form days.js uses: ten nested "_" with the
                         block's line innermost, each day at its ten-digit address (the date's
                         eight digits, then 00). Each day is a sealed envelope in the beach's own
                         gray (gray.js, salted with this place's address, exactly as the beach's
                         connector seals here), so only the person's words open it. Latched with
                         the words.
     mark:<name>         public: only how many days are kept, and the date of the last.
                         {_: 'DAYS KEPT — …', 1: '<count>', 2: 'YYYY-MM-DD'}. Latched.
     arrivals            open accumulator of who brought whom, grown by append. The beach stamps
                         position 3 of every appended entry with the moment it lands, so the
                         entry is {_: 'person' | 'organisation', 1: newcomer, 2: inviter or '',
                         3: (the beach's stamp), 4: ISO time from the phone, 5: org block for an
                         organisation}. (The plan's {1,2,3: kind,4} shape would lose the kind to
                         that stamp, found on the live beach 26 October 2026; the kind sits at _.)
     pool:<name>         the person's room: words left for them, {_: text, 1: from, 2: '', 3: time},
                         appended by anyone, as every pool on this beach is.
     org:<short-name>    {_: name, 1: what it offers, 2: how to reach it, 3: who it is for,
                         4: its people, comma-separated, founder first, 5: vouched by (the
                         inviter), 6: ISO time}. Latched with the founder's words.
     agreement:<name>, constitution:<name>, standards:<name>   the notebooks kit.js makes;
                         written here the way join.html and kit.js leaf() write them.

   A CROSSING (the one convention this file sets): an entry in a person's room whose text begins
   'CROSSING:' — {_: 'CROSSING: <one or two plain sentences>', 1: <who offered it, the crossing
   agent's name>, 2: <the other person's name>, 3: time}. Position 2, an address in an ordinary
   note, names the other person in a crossing. The crossing agent writes one to each person's room.

   For trying the nine days by hand: ?days=N adds N kept days, and works only for a name that
   begins 'trial-'. */
(function () {
  'use strict';
  var CFG = window.CR || {};
  var el = Kit.el;
  var app = document.getElementById('app');
  var STILL = window.matchMedia ? matchMedia('(prefers-reduced-motion: reduce)') : { matches: false };
  var PARAMS = new URLSearchParams(location.search);
  var FROM = Kit.app.tidyName(PARAMS.get('from') || PARAMS.get('invite') || '');
  var SAMARITANS = 'A hard day? Samaritans, any time: 116 123';
  var HELP_ALL = 'A hard day? Samaritans, any time: 116 123. For urgent mental health help, NHS 111, option 2. If life is at risk, 999.';
  var SAY = {
    taken: 'Someone here already has that name. Pick another, or add a letter.',
    beach: 'The beach is out of reach just now. Please try again in a moment.',
    noname: 'Nobody here has that name yet. Check the spelling, or choose I’m new here.',
    words: 'Check each of your three words and try again.',
    three: 'Your words are three words. Please type all three.',
    stand: 'Your old words still stand. A keeper can clear them with you in person first.'
  };
  // The crossing agent's own name. Matthew names the agent; this must match the name it writes
  // at position 1 of every crossing, or its crossings show as ordinary words.
  var CROSSING_AGENT = 'turnstone';
  var FORGOTTEN = "Forgotten at the holder's request.";
  var RESETTING = 'Your keeper is finishing your reset. You can keep days again once it\u2019s done.';
  var SEALED = 'Sealed with your words. Keepers can\u2019t read them.';
  var LOST = 'Lost your words? A keeper can reset them with you in person. Your days stay private, even from them.';

  function sget(k) { try { return localStorage.getItem(k); } catch (e) { return null; } }
  function sset(k, v) { try { if (v == null) localStorage.removeItem(k); else localStorage.setItem(k, v); } catch (e) {} }
  function ssget(k) { try { return sessionStorage.getItem(k); } catch (e) { return null; } }
  function ssset(k, v) { try { if (v == null) sessionStorage.removeItem(k); else sessionStorage.setItem(k, v); } catch (e) {} }
  if (FROM) ssset('cr:app:from', FROM);
  function inviter() { return FROM || ssget('cr:app:from') || ''; }

  function add(parent) {
    for (var i = 1; i < arguments.length; i++) {
      var c = arguments[i];
      if (c == null || c === false) continue;
      parent.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
    }
    return parent;
  }
  function p(cls, t) { return el('p', cls, t); }
  function button(cls, t, fn) {
    var b = el('button', cls, t); b.type = 'button';
    if (fn) b.addEventListener('click', fn);
    return b;
  }
  var SVGNS = 'http://www.w3.org/2000/svg';
  function svg(tag, attrs) {
    var e = document.createElementNS(SVGNS, tag);
    Object.keys(attrs || {}).forEach(function (k) { e.setAttribute(k, attrs[k]); });
    return e;
  }
  function icon(kind) {
    var s = svg('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'aria-hidden': 'true' });
    s.appendChild(svg('path', { d: kind === 'menu' ? 'M4 7h16M4 12h16M4 17h16' : kind === 'close' ? 'M6 6l12 12M18 6L6 18' : 'M15 5l-7 7 7 7' }));
    return s;
  }
  function iconBtn(kind, label, fn) {
    var b = button('icon-btn', null, fn);
    b.setAttribute('aria-label', label);
    b.appendChild(icon(kind));
    return b;
  }
  function field(labelText, id, multi, value, max) {
    var f = el('div', 'field');
    var l = el('label', null, labelText); l.htmlFor = id;
    var i = el(multi ? 'textarea' : 'input');
    if (!multi) i.type = 'text';
    i.id = id;
    if (multi) i.rows = typeof multi === 'number' ? multi : 2;
    if (max) i.maxLength = max;
    if (value) i.value = value;
    i.autocomplete = 'off';
    add(f, l, i);
    return { box: f, input: i };
  }
  function tick(labelText, on) {
    var l = el('label', 'tick-s'), i = el('input'); i.type = 'checkbox'; i.checked = !!on;
    add(l, i, el('span', null, labelText));
    return { box: l, input: i };
  }
  function status() { var s = p('status-s'); s.setAttribute('role', 'status'); return s; }
  function when(iso) {
    var d = new Date(iso);
    if (isNaN(d)) return '';
    return d.toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' });
  }
  function topDate() {
    return new Date().toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' }).replace(/,/g, '').toUpperCase();
  }
  function lines(s) { return String(s || '').split('\n').map(function (x) { return x.trim(); }).filter(Boolean); }
  function split(s) { return Kit.split(s); }

  // ---- the beach -------------------------------------------------------------------------------
  function read(block) { return Kit.read(block); }
  function post(body) { return Kit.post(body); }
  function append(block, content) { return post({ block: block, append: true, content: content }); }
  function note(pool, from, text) {
    return append(pool, { _: text, 1: from, 2: '', 3: new Date().toISOString() });
  }
  // The beach's own gray, loaded as a module beside this script.
  function gray() {
    if (window.Gray) return Promise.resolve(window.Gray);
    return new Promise(function (resolve, reject) {
      var t = setTimeout(function () { reject(new Error('seal')); }, 20000);
      window.addEventListener('gray-ready', function () { clearTimeout(t); resolve(window.Gray); }, { once: true });
    });
  }

  // ---- the person ------------------------------------------------------------------------------
  var S = {
    me: null,               // {name, words}
    now: null,              // now:<name> as read
    days: [],               // [{address, node}], oldest first
    real: 0,                // days kept, as the beach holds them
    pool: null,
    orgs: [],               // org blocks this person stands in: [{block, data}]
    org: false,             // joined for an organisation
    passport: null,
    shown: {},              // screens' small state for this visit
    newLine: null           // this visit's invitation line, once shown
  };
  function trialDays() {
    var n = parseInt(PARAMS.get('days') || '', 10);
    return S.me && /^trial-/i.test(S.me.name) && n > 0 ? Math.min(n, 99) : 0;
  }
  function count() { return S.real + trialDays(); }
  function level() { return Math.min(count(), 9); }

  // ---- days: date addresses, as days.js and the beach's clock make them ---------------------------
  // The calendar date of the phone, as a day's address: the year, season 1-4, month of the
  // season 1-3, seven-day band 1-5, day of the band 1-7, then two noughts.
  function dayAddress(d) {
    d = d || new Date();
    var y = d.getFullYear(), m = d.getMonth(), dd = d.getDate();
    return String(y) + (Math.floor(m / 3) + 1) + (m % 3 + 1) + (Math.floor((dd - 1) / 7) + 1) + ((dd - 1) % 7 + 1) + '00';
  }
  function dateOf(address) {
    var a = String(address);
    return new Date(Number(a.slice(0, 4)), (Number(a[4]) - 1) * 3 + Number(a[5]) - 1, (Number(a[6]) - 1) * 7 + Number(a[7]), 12);
  }
  function isoDay(d) {
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
  }
  function floorChain(line) { var n = line; for (var i = 0; i < 10; i++) n = { _: n }; return n; }
  // Every day in a now block, oldest first: the node at depth 8 reached by digits (a year digit
  // may be nought, written "_"; the season, month, band and day never are).
  function daysIn(block) {
    var out = [];
    (function walk(node, digits) {
      if (!node || typeof node !== 'object') return;
      Object.keys(node).forEach(function (k) {
        var d = k === '_' ? '0' : k;
        var ok = digits.length === 0 ? /^[1-9]$/.test(d) : digits.length < 4 ? /^[0-9]$/.test(d) : /^[1-9]$/.test(d);
        if (!ok) return;
        var next = node[k];
        if (digits.length === 7) { if (next != null && Kit.text(next) !== '') out.push({ address: digits + d + '00', node: next }); }
        else walk(next, digits + d);
      });
    })(block, '');
    return out.sort(function (a, b) { return a.address < b.address ? -1 : 1; });
  }
  function todayKept() {
    var a = dayAddress();
    return S.days.some(function (d) { return d.address === a; });
  }
  function nowLine(name) { return name + '’s days, kept in Community Recovery’s app. Each day is sealed with ' + name + '’s own words; only the count is open, in mark:' + name + '.'; }

  // Keep today: seal the words with the person's own words, then write. A second keep the same
  // day opens what stands and adds to it. Resolves true, or throws a message.
  function keepToday(text) {
    var m = S.me, block = 'now:' + m.name, at = dayAddress();
    var G;
    return gray().then(function (g) {
      G = g;
      if (S.now) return null;
      return post({ block: block, content: floorChain(nowLine(m.name)), new_lock: m.words }).then(function (r) {
        if (!r.ok) { if (r.status === 403 || r.data.code === 'confirm_required') { sset('cr:app:reset:' + m.name, '1'); throw RESETTING; } throw SAY.beach; }
        S.now = floorChain(nowLine(m.name));
      });
    }).then(function () {
      var there = S.days.filter(function (d) { return d.address === at; })[0];
      if (!there) return text;
      if (!G.isGray(there.node)) return (Kit.text(there.node) + '\n\n' + text).trim();
      return G.open(there.node, m.words, m.name).then(function (old) {
        return old == null ? text : (old + (old && text ? '\n\n' : '') + text);
      });
    }).then(function (full) {
      return G.seal(full, m.words);
    }).then(function (env) {
      return post({ block: block, spindle: at, content: env, secret: m.words }).then(function (r) {
        // the person's words were proved at the door, so a refusal here is a day book still
        // latched to old words: the keeper's reset is unfinished
        if (!r.ok) { if (r.status === 403) { sset('cr:app:reset:' + m.name, '1'); throw RESETTING; } throw SAY.beach; }
        sset('cr:app:reset:' + m.name, null);
        return read(block);
      });
    }).then(function (back) {
      S.now = back;
      S.days = daysIn(back);
      if (!todayKept()) throw SAY.beach;
      S.real = S.days.length;
      return writeMark();
    });
  }
  function writeMark() {
    var m = S.me, block = 'mark:' + m.name;
    var content = { _: 'DAYS KEPT — how many days ' + m.name + ' has kept in Community Recovery’s app, and the last one. A count only: the days themselves are sealed.', 1: String(S.real), 2: isoDay(new Date()) };
    return read(block).then(function (b) {
      return b ? post({ block: block, content: content, secret: m.words, confirm: true })
               : post({ block: block, content: content, new_lock: m.words });
    }).then(function () { return true; }, function () { return true; });
  }

  // ---- what the person has: loaded once they are known -------------------------------------------
  function load() {
    var m = S.me;
    return Promise.all([
      read('passport:' + m.name), read('now:' + m.name), read('pool:' + m.name), Kit.index()
    ]).then(function (r) {
      if (!r[0]) { Kit.forgetMe(); S.me = null; return false; }
      S.passport = r[0];
      S.now = r[1];
      S.days = r[1] ? daysIn(r[1]) : [];
      S.real = S.days.length;
      S.pool = r[2];
      var orgBlocks = Kit.names(r[3]).filter(function (n) { return /^org:/.test(n); });
      return Promise.all(orgBlocks.map(function (b) {
        return read(b).then(function (d) { return { block: b, data: d }; }, function () { return null; });
      })).then(function (os) {
        S.orgs = os.filter(function (o) { return o && o.data && peopleOf(o.data).some(function (x) { return x.toLowerCase() === m.name.toLowerCase(); }); });
        S.org = S.orgs.length > 0 || sget('cr:app:org:' + m.name) === '1';
        return true;
      });
    });
  }
  function peopleOf(org) { return String(Kit.text(org && org['4']) || '').split(/\s*,\s*/).filter(Boolean); }

  // ---- crossings and words in the person's room ----------------------------------------------------
  function entriesOf(pool) {
    var out = [];
    (function walk(node, path) {
      if (!node || typeof node !== 'object') return;
      if (node._ && typeof node._ === 'object') walk(node._, path + '0');
      for (var i = 1; i <= 9; i++) {
        var e = node[String(i)];
        if (e && typeof e === 'object' && typeof e._ === 'string' && typeof e['3'] === 'string') {
          if (e._.trim()) out.push({
            id: path + i, text: e._.trim(),
            who: typeof e['1'] === 'string' && e['1'].length <= 60 ? e['1'] : '',
            other: typeof e['2'] === 'string' && e['2'].length <= 60 ? e['2'] : '',
            when: String(e['3']).replace(/\s*\(.*$/, '')
          });
        } else if (e && typeof e === 'object') walk(e, path + i);
      }
    })(pool, '');
    return out;
  }
  function isCrossing(e) { return /^CROSSING:/.test(e.text) && e.who === CROSSING_AGENT; }
  function crossings() { return entriesOf(S.pool).filter(isCrossing).reverse(); }
  function wordsFor() {
    var mine = S.me ? S.me.name.toLowerCase() : '';
    return entriesOf(S.pool).filter(function (e) { return !isCrossing(e) && e.who.toLowerCase() !== mine; }).reverse();
  }
  function seenKey() { return 'cr:app:seen:' + S.me.name; }
  function seen() { try { return JSON.parse(sget(seenKey()) || '{}'); } catch (e) { return {}; } }
  function markSeen(ids) { var s = seen(); ids.forEach(function (id) { s[id] = 1; }); sset(seenKey(), JSON.stringify(s)); }
  function waiting() {
    var s = seen();
    return {
      crossing: crossings().some(function (c) { return !s[c.id]; }),
      word: wordsFor().some(function (w) { return !s[w.id]; })
    };
  }

  // ---- the menu, growing over nine days kept (1.17222) ------------------------------------------
  // [id, label, section, day it opens]
  var ITEMS = [
    ['today', 'Today', 'YOURS', 0],
    ['days', 'Your days', 'YOURS', 1],
    ['bring', 'What I bring', 'YOURS', 4],
    ['crossings', 'Crossings and words', 'YOURS', 5],
    ['invite', 'My invitation', 'YOURS', 9],
    ['where', 'Where and when', 'US', 1],
    ['agreement', 'How we look after each other', 'US', 2],
    ['story', 'Our story', 'US', 3],
    ['who', 'Who we are to each other', 'US', 6],
    ['room', 'The room', 'US', 5],
    ['constitution', 'The constitution', 'HOW WE RUN', 7],
    ['standards', 'The standards', 'HOW WE RUN', 8],
    ['map', 'The map', 'THE WIDER PICTURE', 9],
    ['presentation', 'Introducing Community Recovery', 'THE WIDER PICTURE', 9]
  ];
  var SECTIONS = ['YOURS', 'US', 'HOW WE RUN', 'THE WIDER PICTURE'];
  function open(id) {
    var it = ITEMS.filter(function (x) { return x[0] === id; })[0];
    if (!it) return true;
    if (S.org && id === 'bring') return true;
    return level() >= it[3];
  }
  function label(id) { var it = ITEMS.filter(function (x) { return x[0] === id; })[0]; return it ? it[1] : id; }
  function levelKey() { return 'cr:app:level:' + S.me.name; }
  // The one invitation line, for what the newest kept day opened, shown once.
  function newInMenu() {
    if (S.newLine) return S.newLine;
    var told = parseInt(sget(levelKey()) || '0', 10) || 0, lv = level();
    if (lv <= told) return null;
    var fresh = ITEMS.filter(function (x) { return x[3] === lv && !(S.org && x[0] === 'bring'); });
    sset(levelKey(), String(lv));
    if (!fresh.length) return null;
    S.newLine = { ids: fresh.map(function (x) { return x[0]; }), text: fresh.map(function (x) { return x[1]; }).join(', ') };
    return S.newLine;
  }

  // ---- screens ----------------------------------------------------------------------------------
  var current = null;
  function show(name, arg, replace) {
    current = { name: name, arg: arg };
    var st = { s: name, a: arg == null ? null : arg };
    try { if (replace) history.replaceState(st, ''); else history.pushState(st, ''); } catch (e) {}
    draw();
  }
  window.addEventListener('popstate', function (ev) {
    var st = ev.state;
    current = st && st.s ? { name: st.s, arg: st.a } : { name: S.me ? 'today' : 'welcome' };
    if (!S.me && !/^(welcome|join|back|lost)$/.test(current.name)) current = { name: 'welcome' };
    if (S.me && /^(welcome|join|back|lost)$/.test(current.name)) current = { name: 'today' };
    draw();
  });
  function draw() {
    var f = SCREENS[current.name] || SCREENS.today;
    var node = f(current.arg);
    app.textContent = '';
    app.appendChild(node);
    window.scrollTo(0, 0);
    var h = node.querySelector('h1');
    if (h && current.name !== 'today') { h.tabIndex = -1; try { h.focus({ preventScroll: true }); } catch (e) {} }
  }
  function screen(cls) { return el('section', 'screen' + (cls ? ' ' + cls : '')); }
  function backBar(to, labelText) {
    return iconBtn('back', labelText || 'Back', function () { if (history.length > 1 && history.state) history.back(); else show(to || 'today'); });
  }

  // The logo on its black patch, with the nine dots round it.
  // opt: {lit: dots lit lilac, today: index of today's dot, kept: today's dot is kept, cls}
  function ring(opt) {
    opt = opt || {};
    var box = el('div', 'ring' + (opt.cls ? ' ' + opt.cls : ''));
    var patch = el('div', 'patch'), img = el('img');
    img.src = 'img/logo-app.jpg'; img.alt = 'Community Recovery: two hands clasped in a heart. Stronger together.';
    img.width = 384; img.height = 384;
    patch.appendChild(img);
    box.appendChild(patch);
    if (opt.dots === false) return box;
    var s = svg('svg', { 'class': 'dots', viewBox: '0 0 240 240', 'aria-hidden': 'true' });
    for (var i = 0; i < 9; i++) {
      var a = (i * 40 - 90) * Math.PI / 180, x = 120 + Math.cos(a) * 104, y = 120 + Math.sin(a) * 104;
      x = x.toFixed(1); y = y.toFixed(1);
      if (i === opt.today && opt.kept) {
        s.appendChild(svg('circle', { cx: x, cy: y, r: 13, fill: '#F4C86A', opacity: 0.22, 'class': 'dot-glow' }));
        s.appendChild(svg('circle', { cx: x, cy: y, r: 6.5, fill: '#F4C86A', 'class': 'dot-gold' }));
      } else if (i === opt.today) {
        s.appendChild(svg('circle', { cx: x, cy: y, r: 7, fill: 'none', stroke: '#F4C86A', 'stroke-width': 2.5 }));
      } else if (i < opt.lit) {
        s.appendChild(svg('circle', { cx: x, cy: y, r: 5, fill: '#C9A6F0' }));
      } else {
        s.appendChild(svg('circle', { cx: x, cy: y, r: 3, fill: '#4A3A5E' }));
      }
    }
    if (opt.kept) box.insertBefore(svg('svg', { 'class': 'dots', viewBox: '0 0 240 240', 'aria-hidden': 'true' }), box.firstChild).appendChild(svg('circle', { cx: 120, cy: 120, r: 92, fill: '#F4C86A', opacity: 0.07 }));
    box.appendChild(s);
    return box;
  }
  // The nine of the moment: lilac for each day kept in the current nine, today's place outlined
  // gold, folding round at nine.
  function nine() {
    var n = count(), k = todayKept();
    if (k) { var lit = ((n - 1) % 9) + 1; return { lit: lit - 1, today: lit - 1, kept: true }; }
    return { lit: n % 9, today: n % 9, kept: false };
  }
  function daysWords(n) { return n === 1 ? '1 day kept' : n + ' days kept'; }

  var SCREENS = {};

  // WELCOME, from the card (or without one)
  SCREENS.welcome = function () {
    var s = screen('welcome');
    add(s, p('eyebrow-s', 'Community Recovery'));
    add(s, ring({ cls: 'welcome-ring', dots: false }));
    var from = inviter();
    if (from) {
      add(s, p('eyebrow-q', 'An invitation from'), (function () { var x = p('name-big', from); x.style.marginTop = '8px'; return x; })());
      s.querySelector('.eyebrow-q').style.marginTop = '36px';
    }
    var hand = p('hand-s', 'Come as you are.'); hand.style.marginTop = from ? '6px' : '36px';
    add(s, hand);
    var go = el('div', 'stack push');
    add(go,
      button('btn', 'I’m new here', function () { show('join', { org: false }); }),
      button('btn line', 'I’ve been before', function () { show('back', { org: false }); }),
      button('btn text', 'Joining for an organisation', function () { show('join', { org: true }); }));
    go.lastChild.style.alignSelf = 'center';
    add(s, go);
    var where = p('quiet-s', ''); where.style.textAlign = 'center'; where.style.marginTop = '16px';
    add(s, where);
    whereLine().then(function (t) { where.textContent = t; });
    return s;
  };
  var whereCache = null;
  function whereBlock() { return whereCache || (whereCache = read('where-and-when').catch(function () { return null; })); }
  function whereLine() {
    return whereBlock().then(function (b) {
      var t = b ? Kit.text(b['1']) : '';
      return t;
    });
  }

  // JOIN: a name and three words, as Join us asks them
  SCREENS.join = function (arg) {
    arg = arg || {};
    var s = screen();
    add(s, backBar('welcome'));
    add(s, ring({ cls: 'small', dots: false }));
    s.lastChild.style.alignSelf = 'center';
    var h = el('h1', null, 'Welcome'); h.style.textAlign = 'center'; h.style.marginTop = '18px';
    add(s, h);
    var lead = p('lead-s', 'Pick a name the community will know you by. Three words are your key.');
    lead.style.textAlign = 'center';
    add(s, lead);
    var form = el('form', 'stack'); form.noValidate = true; form.style.marginTop = '28px'; form.style.gap = '18px';
    var nm = field('A name', 'cr-name', false, '', 24);
    nm.input.setAttribute('autocapitalize', 'words');
    var small = p('quiet-s', 'A first name or a nickname is fine. Everyone can read it.');
    var words = Kit.app.threeWords();
    var wf = el('div', 'field'), wl = el('p', 'lab', 'Your three words'), chips = el('div', 'words-s');
    function paint() {
      chips.textContent = '';
      words.forEach(function (w) { chips.appendChild(el('span', 'w', w)); });
      chips.appendChild(button('btn text', 'Other words', function () { words = Kit.app.threeWords(); paint(); }));
    }
    paint();
    add(wf, wl, chips);
    var priv = p('quiet-s', 'Your days stay yours. ' + SEALED + ' What you choose to bring is open for the community to see.');
    var lost = p('quiet-s', LOST);
    var adult = tick('I am 18 or over', false);
    var rem = tick('Remember me on this ' + Kit.app.device, true);
    var st = status();
    var go = el('button', 'btn', 'Come in'); go.type = 'submit';
    add(form, nm.box, small, wf, priv, lost, adult.box, rem.box, go, st);
    add(s, form);
    var alt = button('btn text', 'I\u2019ve been before', function () { show('back', arg); });
    alt.style.alignSelf = 'center';
    add(s, alt);
    var from = inviter();
    if (from) { var inv = p('quiet-s', 'Invited by ' + from); inv.style.textAlign = 'center'; inv.style.marginTop = '14px'; add(s, inv); }
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var name = Kit.app.tidyName(nm.input.value);
      if (name.length < 2) { st.textContent = 'Please choose a name of two letters or more.'; nm.input.focus(); return; }
      if (!adult.input.checked) { st.textContent = 'This place is for people aged 18 or over. Please tick the box if that’s you.'; return; }
      go.disabled = true; st.textContent = 'One moment…';
      Kit.app.foundName(name, words.join('-')).then(function (r) {
        go.disabled = false;
        if (!r.ok) { st.textContent = SAY[r.why] || SAY.beach; return; }
        var m = { name: r.name, words: r.words };
        Kit.app.keepMe(m, rem.input.checked);
        S.me = m;
        if (arg.org) sset('cr:app:org:' + m.name, '1');
        // the arrival: who came, who brought them, and the moment; nothing else
        var arrival = arg.org ? null : append('arrivals', { _: 'person', 1: m.name, 2: inviter(), 4: new Date().toISOString() });
        Promise.resolve(arrival).then(function () {
          show('words', { org: !!arg.org, fresh: true }, true);
        });
      });
    });
    setTimeout(function () { nm.input.focus(); }, 30);
    return s;
  };

  // YOUR WORDS: the card to keep, as Join us shows it
  SCREENS.words = function (arg) {
    arg = arg || {};
    var m = S.me, s = screen();
    if (!m) return SCREENS.welcome();
    add(s, p('eyebrow-s', 'Your key'));
    add(s, el('h1', null, 'You’re ' + m.name));
    add(s, p('lead-s', 'These three words are the only key to your name. Keep them somewhere safe.'));
    var chips = el('div', 'words-s'); chips.style.marginTop = '18px';
    m.words.split('-').forEach(function (w) { chips.appendChild(el('span', 'w', w)); });
    add(s, chips);
    var src = null;
    try { src = Kit.app.cardPicture(m.name, m.words, myLink()); } catch (e) { src = null; }
    if (src) {
      var save = el('a', 'btn line', 'Save the picture'); save.href = src; save.download = 'my-card-' + m.name + '.png';
      save.style.marginTop = '22px';
      add(s, save);
    }
    var lost = p('quiet-s', LOST); lost.style.marginTop = '16px';
    add(s, lost);
    var go = el('div', 'stack push'), st = status();
    add(go, button('btn', 'I\u2019ve kept them', function () {
      if (arg.org) { show('org-new', null, true); return; }
      st.textContent = 'Opening your days\u2026';
      load().then(function (ok) { show(ok ? 'today' : 'welcome', null, true); }, function () { st.textContent = SAY.beach; });
    }), st);
    add(s, go);
    return s;
  };

  // BEEN BEFORE: name and words
  SCREENS.back = function (arg) {
    arg = arg || {};
    var s = screen();
    add(s, backBar('welcome'));
    add(s, ring({ cls: 'small', dots: false }));
    s.lastChild.style.alignSelf = 'center';
    var h = el('h1', null, 'Welcome back'); h.style.textAlign = 'center'; h.style.marginTop = '18px';
    add(s, h);
    var form = el('form', 'stack'); form.noValidate = true; form.style.marginTop = '28px'; form.style.gap = '18px';
    var nm = field('Your name', 'cr-name2', false, '', 24); nm.input.setAttribute('autocapitalize', 'words');
    var wd = field('Your three words', 'cr-words', false, '', 80);
    wd.input.setAttribute('autocapitalize', 'none'); wd.input.spellcheck = false;
    var rem = tick('Remember me on this ' + Kit.app.device, true);
    var st = status();
    var go = el('button', 'btn', 'Come in'); go.type = 'submit';
    add(form, nm.box, wd.box, rem.box, go, st);
    add(s, form);
    var lost = p('quiet-s', LOST); lost.style.marginTop = '18px';
    add(s, lost, button('btn text', 'My words were reset. Give me new ones', function () { show('lost', arg); }));
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      go.disabled = true; st.textContent = 'One moment…';
      Kit.app.comeBack(nm.input.value, wd.input.value).then(function (r) {
        go.disabled = false;
        if (!r.ok) { st.textContent = r.why === 'word' ? 'Check the spelling of “' + r.which + '”.' : (SAY[r.why] || SAY.beach); return; }
        var m = { name: r.name, words: r.words };
        Kit.app.keepMe(m, rem.input.checked);
        S.me = m;
        if (arg.org) sset('cr:app:org:' + m.name, '1');
        st.textContent = 'Opening your days…';
        load().then(function (ok) { show(ok ? (arg.org ? 'org-new' : 'today') : 'welcome', null, true); }, function () { st.textContent = SAY.beach; });
      });
    });
    setTimeout(function () { nm.input.focus(); }, 30);
    return s;
  };

  // NEW WORDS, after a keeper has cleared the old ones with the person there
  SCREENS.lost = function (arg) {
    var s = screen();
    add(s, backBar('back'));
    add(s, el('h1', null, 'New words'));
    add(s, p('lead-s', 'A keeper clears your old words with you, in person. Then take new ones here. Your name, what you bring and your agreement carry on. Days kept under your old words stay sealed under them, for good.'));
    var words = Kit.app.threeWords();
    var form = el('form', 'stack'); form.noValidate = true; form.style.marginTop = '22px'; form.style.gap = '18px';
    var nm = field('Your name', 'cr-name3', false, '', 24);
    var chips = el('div', 'words-s');
    words.forEach(function (w) { chips.appendChild(el('span', 'w', w)); });
    var st = status();
    var go = el('button', 'btn', 'My old words were cleared. Give me these'); go.type = 'submit';
    add(form, nm.box, p('lab', 'Your new words'), chips, go, st);
    add(s, form);
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      go.disabled = true; st.textContent = 'One moment…';
      Kit.app.newWords(nm.input.value, words.join('-')).then(function (r) {
        go.disabled = false;
        if (!r.ok) { st.textContent = SAY[r.why] || SAY.beach; return; }
        var m = { name: r.name, words: r.words };
        Kit.app.keepMe(m, true);
        S.me = m;
        // the day book and the count answer to the new words too, where the beach allows it
        Promise.all(['now:', 'mark:'].map(function (b) {
          return read(b + m.name).then(function (have) { return have ? post({ block: b + m.name, new_lock: m.words }) : { ok: true }; }, function () { return { ok: true }; });
        })).then(function (rs) {
          sset('cr:app:reset:' + m.name, rs.every(function (x) { return x.ok; }) ? null : '1');
          show('words', { org: arg && arg.org }, true);
        });
      });
    });
    return s;
  };

  // TODAY
  SCREENS.today = function () {
    var m = S.me, s = screen();
    if (!m) return SCREENS.welcome();
    var bar = el('div', 'topbar');
    add(bar, iconBtn('menu', 'Menu', function () { show('menu'); }), p('eyebrow-q', topDate() + ' · COMMUNITY RECOVERY'));
    add(s, bar);
    var mid = el('div'); mid.style.cssText = 'margin-top:14px;display:flex;flex-direction:column;align-items:center';
    var n9 = nine(), r = ring(n9);
    r.setAttribute('role', 'img');
    r.setAttribute('aria-label', daysWords(count()) + (n9.kept ? ', today kept' : ', today waiting'));
    add(mid, r, p('who-s', m.name), p('quiet-s', daysWords(count())));
    add(s, mid);
    var w = waiting();
    if (w.crossing || w.word) {
      var pill = button('pill-s', null, function () { show('crossings'); });
      add(pill, el('span', 'spark'), w.crossing ? 'A crossing for you' : 'A word for you');
      pill.style.margin = '16px auto 0';
      add(s, pill);
    }
    var fresh = newInMenu();
    if (fresh) {
      var line = button('invite-line', null, function () { S.newLine = null; show(fresh.ids[0] === 'invite' ? 'bring' : fresh.ids[0]); });
      add(line, el('span', 'spark'), 'Something new in your menu: ' + fresh.text);
      line.querySelector('.spark').style.cssText = 'width:9px;height:9px;border-radius:50%;background:#F4C86A;flex:none';
      line.style.marginTop = '14px';
      add(s, line);
    }
    var foot = el('form', 'stack push'); foot.noValidate = true; foot.style.gap = '10px';
    var lab = el('label', 'today-label', 'How’s today been?'); lab.htmlFor = 'cr-today';
    var ta = el('textarea'); ta.id = 'cr-today'; ta.rows = 3; ta.maxLength = 4000;
    var draftKey = 'cr:app:draft:' + m.name;
    ta.value = ssget(draftKey) || '';
    ta.addEventListener('input', function () { ssset(draftKey, ta.value || null); });
    var noteP = p('mic-note'); noteP.setAttribute('role', 'status');
    var mic = Kit.speakButton(ta, noteP);
    var keep = el('button', 'btn', 'Keep today'); keep.type = 'submit';
    var st = status();
    add(foot, lab, ta, mic, noteP, keep, st);
    if (sget('cr:app:reset:' + m.name) === '1') st.textContent = RESETTING;
    var hard = p('tiny-s', SAMARITANS); hard.style.textAlign = 'center'; hard.style.marginTop = '4px';
    add(foot, hard);
    add(s, foot);
    foot.addEventListener('submit', function (ev) {
      ev.preventDefault();
      keep.disabled = true; st.textContent = 'Keeping today, sealed with your words…';
      var before = level();
      keepToday(ta.value.trim()).then(function () {
        ssset(draftKey, null);
        S.newLine = null;
        show('kept', { opened: level() > before }, true);
      }).catch(function (e) {
        keep.disabled = false;
        st.textContent = typeof e === 'string' ? e : SAY.beach;
      });
    });
    return s;
  };

  // THE KEEP: an event
  SCREENS.kept = function () {
    var s = screen();
    s.style.alignItems = 'center';
    var bar = el('div', 'topbar');
    add(bar, iconBtn('menu', 'Menu', function () { show('menu'); }), p('eyebrow-q', topDate() + ' · COMMUNITY RECOVERY'));
    add(s, bar);
    var n9 = nine(), r = ring({ lit: n9.lit, today: n9.today, kept: true, cls: 'big kept-now' });
    r.style.marginTop = '48px';
    r.setAttribute('role', 'img'); r.setAttribute('aria-label', 'Today kept, glowing gold. ' + daysWords(count()) + '.');
    add(s, r);
    var h = el('h1', 'fade-in later', 'Today is kept'); h.style.marginTop = '28px'; h.style.textAlign = 'center';
    add(s, h);
    var c = p('fade-in later', daysWords(count()) + '. ' + SEALED); c.style.cssText = 'margin-top:8px;font-size:16px;text-align:center';
    add(s, c);
    var hand = p('hand-s gold fade-in latest', 'Stronger together.'); hand.style.marginTop = '18px';
    add(s, hand);
    if (count() === 9) { var nn = p('card-line fade-in latest', 'Nine days kept. Ask a volunteer for your card.'); add(s, nn); }
    var go = el('div', 'stack push');
    add(go, button('btn line', 'Back to today', function () { show('today', null, true); }));
    add(s, go);
    return s;
  };

  // THE MENU
  SCREENS.menu = function () {
    var s = screen('menu-screen');
    var bar = el('div', 'topbar');
    var t = p(null, 'COMMUNITY RECOVERY'); t.style.cssText = 'font-family:var(--cr-display);font-weight:500;font-size:18px;letter-spacing:0.08em;color:#F3EDF9';
    add(bar, iconBtn('close', 'Close the menu', function () { show('today'); }), t);
    add(s, bar);
    var w = waiting();
    SECTIONS.forEach(function (sec) {
      var items = ITEMS.filter(function (x) { return x[2] === sec && open(x[0]); });
      var extra = sec === 'YOURS' ? S.orgs.map(function (o) { return ['org:' + o.block, Kit.text(o.data) || 'Our organisation']; }) : [];
      if (!items.length && !extra.length) return;
      add(s, p('menu-sec', sec));
      var nav = el('nav', 'menu-list'); nav.setAttribute('aria-label', sec.toLowerCase());
      items.forEach(function (x) {
        var b = button('menu-item', x[1], function () { show(x[0] === 'invite' ? 'bring' : x[0], x[0] === 'invite' ? { invite: true } : null); });
        if (x[0] === 'crossings' && (w.crossing || w.word)) b.appendChild(el('span', 'badge', 'new'));
        nav.appendChild(b);
      });
      extra.forEach(function (x) { nav.appendChild(button('menu-item', x[1], function () { show('org', x[0].slice(4)); })); });
      if (sec === 'YOURS' && S.org && !S.orgs.length) nav.appendChild(button('menu-item', 'Add our organisation', function () { show('org-new'); }));
      add(s, nav, el('div', 'menu-rule'));
    });
    // early arrivals always show, even before their day
    if (!open('crossings') && (w.crossing || w.word)) {
      add(s, button('menu-item', w.crossing ? 'A crossing for you' : 'A word for you', function () { show('crossings'); }));
    }
    if (count() >= 9) add(s, p('card-line', 'Nine days kept. Ask a volunteer for your card.'));
    var foot = el('div', 'stack push'); foot.style.gap = '10px';
    var row = el('div', 'menu-foot');
    add(row,
      button(null, 'About', function () { show('about'); }),
      button(null, 'Not you?', function () { signOut(); }),
      button(null, 'Forget me', function () { show('forget'); }));
    add(foot, row, p('tiny-s', HELP_ALL));
    add(s, foot);
    return s;
  };

  function signOut() {
    Kit.forgetMe();
    S.me = null; S.now = null; S.days = []; S.real = 0; S.pool = null; S.orgs = []; S.org = false; S.newLine = null;
    cache = {}; S.shown = {};
    show('welcome', null, true);
  }

  // ---- small pages: one thing per screen, Next and Back -------------------------------------------
  // pages: [{eyebrow, title, body: node | string | [strings], after(node)}]
  function pagesScreen(opt) {
    var key = 'pages:' + opt.id;
    var i = Math.max(0, Math.min(S.shown[key] || 0, (opt.pages.length || 1) - 1));
    var s = screen();
    var bar = el('div', 'topbar');
    add(bar, backBar('menu', 'Back to the menu'));
    s.appendChild(bar);
    if (!opt.pages.length) {
      add(s, el('h1', null, opt.title), p('lead-s', opt.empty || 'This opens here as soon as the beach answers.'));
      return s;
    }
    var pg = opt.pages[i];
    var count2 = p('count', (i + 1) + ' of ' + opt.pages.length);
    count2.className = 'eyebrow-q';
    bar.appendChild(count2);
    add(s, p('eyebrow-s', pg.eyebrow || opt.title));
    add(s, el('h1', null, pg.title || opt.title));
    var body = el('div', 'page-body');
    var b = pg.body;
    if (typeof b === 'string') lines(b).forEach(function (l) { body.appendChild(p(null, l)); });
    else if (Array.isArray(b)) { var ul = el('ul'); b.forEach(function (l) { ul.appendChild(el('li', null, l)); }); body.appendChild(ul); }
    else if (b) body.appendChild(b);
    if (pg.after) pg.after(body);
    add(s, body);
    var dots = el('div', 'pages-dots'); dots.setAttribute('aria-hidden', 'true');
    opt.pages.forEach(function (x, k) { dots.appendChild(el('span', k === i ? 'on' : null)); });
    var nav = el('div', 'pager push');
    var back = button('btn line small', 'Back', function () { S.shown[key] = i - 1; draw(); });
    var next = button('btn small', i === opt.pages.length - 1 ? 'Done' : 'Next', function () {
      if (i === opt.pages.length - 1) { S.shown[key] = 0; show('menu'); } else { S.shown[key] = i + 1; draw(); }
    });
    if (i === 0) back.style.visibility = 'hidden';
    add(nav, back, next);
    add(s, dots, nav);
    return s;
  }
  // A screen whose pages come from the beach: drawn once loaded, then from memory.
  var cache = {};
  function loading(id, title, loader, builder) {
    if (cache[id]) return pagesScreen({ id: id, title: title, pages: builder(cache[id]) });
    var s = screen();
    add(s, backBar('menu', 'Back to the menu'), el('h1', null, title), p('lead-s calm', 'Opening…'));
    loader().then(function (data) {
      cache[id] = data;
      if (current && SCREENS[current.name] && current.name === id) draw();
    }, function () {
      var l = s.querySelector('.lead-s'); if (l) l.textContent = SAY.beach;
    });
    return s;
  }

  // WHERE AND WHEN: the new block, one place per page
  SCREENS.where = function () {
    return loading('where', 'Where and when', function () { return read('where-and-when'); }, function (b) {
      return Kit.digits(b).map(function (k) {
        var t = Kit.text(b[k]), cut = t.indexOf('. ');
        var place = cut > 0 ? t.slice(0, cut) : t, rest = cut > 0 ? t.slice(cut + 2) : '';
        var parts = place.split(','), head = parts.shift();
        return { eyebrow: 'Where and when', title: head, body: [parts.join(',').trim(), rest].filter(Boolean).join('\n') };
      });
    });
  };

  // HOW WE LOOK AFTER EACH OTHER: the community agreement, one thing per screen, agreed at the end
  SCREENS.agreement = function () {
    return loading('agreement', 'How we look after each other', function () {
      return Promise.all([read('spine:agreement'), read('function:agreement'), read('agreement:' + S.me.name)]);
    }, function (r) {
      var spine = r[0], law = r[1], mine = r[2];
      if (!spine) return [];
      var mm = law && Kit.text(law['1']).match(/wording standing now is the ([^.]+?) wording/);
      var wording = mm ? mm[1] : '';
      var pages = [];
      var numbered = Kit.digits(spine).filter(function (k) { return spine[k] && typeof spine[k] === 'object' && /^\d+\.\s/.test(Kit.text(spine[k])); });
      Kit.digits(spine).forEach(function (k) {
        var n = spine[k];
        if (typeof n === 'string') {
          var ls = lines(n);
          pages.push({ eyebrow: 'In your own time', title: ls[0].replace(/\.$/, ''), body: ls.slice(1).join('\n') });
          return;
        }
        var sp = split(Kit.text(n));
        var title = sp.title.replace(/^\d+\.\s*/, '');
        if (/^\d+\.\s/.test(Kit.text(n)) && /CHIME/.test(sp.gloss)) {
          // CHIME first: the five letters, then each of the four as its own page
          var box = el('div');
          add(box, p('big-s', 'CHIME guides how we work together.'));
          var grid = el('div', 'chime'); grid.style.marginTop = '12px';
          [['C', 'Connection'], ['H', 'Hope'], ['I', 'Identity'], ['M', 'Meaning'], ['E', 'Empowerment']].forEach(function (c) {
            var d = el('div'); add(d, el('b', null, c[0]), el('span', null, c[1])); grid.appendChild(d);
          });
          add(box, grid, p(null, 'By joining, I agree to do my best with what comes next.'));
          pages.push({ eyebrow: 'Our community agreement', title: 'CHIME', body: box });
          Kit.digits(n).forEach(function (j) {
            var c = n[j];
            pages.push({ eyebrow: 'Our community agreement', title: Kit.text(c).replace(/\.$/, ''), body: Kit.digits(c).map(function (x) { return Kit.text(c[x]); }) });
          });
          return;
        }
        if (/^\d+\.\s/.test(Kit.text(n))) {
          var all = Kit.digits(n).map(function (x) { return Kit.text(n[x]); });
          for (var q = 0; q < all.length; q += 2) pages.push({ eyebrow: 'Taking part', title: title, body: all.slice(q, q + 2) });
          return;
        }
        Kit.digits(n).forEach(function (j) {
          var ls = lines(Kit.text(n[j]));
          pages.push({ eyebrow: title, title: ls[0].replace(/\.$/, ''), body: ls.slice(1).join('\n') });
        });
      });
      // agreed at the end, when they are ready
      pages.push({ eyebrow: 'When you are ready', title: 'I agree', body: '', after: function (body) {
        function agreed() { return numbered.length && numbered.every(function (k) { return /^Agreed\b/.test(Kit.text(Kit.at(mine || {}, k))); }); }
        var st = status();
        if (agreed()) { add(body, p('done-s', '✓ You’ve agreed. Welcome.')); return; }
        add(body, p('big-s', 'I agree to do my best with our community agreement, and I understand what taking part means.'));
        var b = button('btn', 'I agree', function () {
          b.disabled = true; st.textContent = 'One moment…';
          var t = 'Agreed. ' + Kit.today() + (wording ? ', the ' + wording + ' wording.' : '.');
          var m = S.me;
          Kit.ready(m).then(function () {
            return numbered.reduce(function (pr, k) {
              return pr.then(function (ok) { return post({ block: 'agreement:' + m.name, spindle: k, content: t, secret: m.words }).then(function (r) { return ok && r.ok; }); });
            }, Promise.resolve(true));
          }).then(function (ok) {
            if (!ok) { b.disabled = false; st.textContent = SAY.beach; return; }
            mine = mine && typeof mine === 'object' ? mine : {};
            numbered.forEach(function (k) { if (mine[k] && typeof mine[k] === 'object') mine[k]._ = t; else mine[k] = t; });
            cache.agreement = [spine, law, mine];
            draw();
          });
        });
        add(body, b, st, p('quiet-s', 'One more step, in person: a welcoming volunteer goes through the rest with you on paper.'));
      } });
      return pages;
    });
  };

  // OUR STORY: the journey, one stepping stone per screen, then members' words about their week
  SCREENS.story = function () {
    return loading('story', 'Our story', function () { return Promise.all([read('journey'), read('pool:journey')]); }, function (r) {
      var stones = [];
      (function walk(node) {
        if (!node || typeof node !== 'object') return;
        if (node._ && typeof node._ === 'object') walk(node._);
        for (var i = 1; i <= 9; i++) {
          var e = node[String(i)];
          if (e && typeof e === 'object' && typeof e._ === 'string' && (typeof e['1'] === 'string' || typeof e['2'] === 'string')) {
            if (e._.trim()) stones.push({ what: e._.trim(), when: typeof e['1'] === 'string' ? e['1'].replace(/\s*\(.*$/, '') : '', ahead: /^\s*ahead/i.test(String(e['2'] || '')) });
          } else if (e && typeof e === 'object') walk(e);
        }
      })(r[0]);
      var pages = stones.map(function (st, k) {
        return { eyebrow: 'Stepping stone ' + (k + 1) + (st.when ? ' · ' + st.when : ''), title: k === 0 ? 'Where it began' : (st.ahead ? 'What’s ahead' : 'Our story'), body: st.what };
      });
      Beach.notes(r[1]).slice().reverse().forEach(function (n) {
        pages.push({ eyebrow: 'In members’ own words' + (n.when ? ' · ' + when(n.when) : ''), title: 'This week', body: n.text });
      });
      return pages;
    });
  };

  // WHO WE ARE TO EACH OTHER: the constitution's opening clauses and its second part, as meet.html reads them
  function clause(node) {
    var ls = lines(Kit.text(node));
    if (ls.length < 2) return null;
    return { title: ls[0].replace(/^[\d.]+\s+/, ''), lines: ls.slice(1) };
  }
  SCREENS.who = function () {
    return loading('who', 'Who we are to each other', function () { return read('spine:constitution'); }, function (c) {
      var pages = [];
      ['1.2.2', '1.2.3', '1.2.4', '1.2.5'].forEach(function (pos) {
        var x = clause(Beach.place.at(c, pos)); if (x) pages.push({ eyebrow: 'Who we are', title: x.title, body: x.lines.join('\n') });
      });
      var m = clause(Beach.place.at(c, '2.1.1')); if (m) pages.push({ eyebrow: 'Who we are to each other', title: 'Members', body: m.lines.join('\n') });
      var lv = clause(Beach.place.at(c, '2.1.6'));
      if (lv) lv.lines.slice(1).forEach(function (l) {
        var x = l.match(/^(.+?)\s+(?:may|are|is)\s/);
        pages.push({ eyebrow: 'Who we are to each other', title: x ? Kit.cap(x[1]) : 'Volunteering', body: l });
      });
      ['2.1.5', '2.1.7'].forEach(function (pos) {
        var x = clause(Beach.place.at(c, pos)); if (x) pages.push({ eyebrow: 'Stepping in as a volunteer', title: x.title, body: x.lines.join('\n') });
      });
      return pages;
    });
  };

  // A family read with every member's notebook, for the constitution and the standards.
  function family(fam) {
    return Promise.all([read('spine:' + fam), read('function:' + fam), Kit.index()]).then(function (r) {
      var list = Kit.names(r[2]).filter(function (n) { return n.indexOf(fam + ':') === 0; });
      return Promise.all(list.map(function (n) { return read(n).then(function (b) { return { who: n.slice(fam.length + 1), block: b }; }, function () { return null; }); }))
        .then(function (books) { return { spine: r[0], law: r[1], books: books.filter(Boolean) }; });
    });
  }
  // the three words beneath the law's 2, each "Word — what it means" (as kit.js reads them)
  function threeOf(law) {
    var n = law && law['2'];
    return Kit.digits(n).map(function (k) { var sp = split(Kit.text(n[k])); return { word: sp.title, means: sp.gloss }; })
      .filter(function (w) { return w.means && w.word.length <= 16; }).slice(0, 3);
  }
  function tokenOf(t, words) {
    var low = String(t || '').trim().toLowerCase();
    for (var i = 0; i < words.length; i++) {
      var w = words[i].word.toLowerCase();
      if (low.indexOf(w) === 0 && !/[a-z]/.test(low.charAt(w.length) || '')) return { i: i, rest: String(t).trim().slice(w.length).replace(/^[\s.:,;—-]+/, '').trim() };
    }
    return { i: -1, rest: String(t || '').trim() };
  }
  // One answer at a part: the three words as buttons, a box to say more, saved to <fam>:<name> at
  // the part's own address, as kit.js leaf() saves a clause: "Word." then the person's own words.
  function answerBox(fam, data, k, words, prompt, threeBoxes) {
    var m = S.me, box = el('div', 'stack');
    var mineBook = data.books.filter(function (b) { return b.who === m.name; })[0];
    var cur = mineBook ? tokenOf(Kit.text(Kit.at(mineBook.block || {}, k)), words) : { i: -1, rest: '' };
    var pick = cur.i;
    var row = el('div', 'answers');
    words.forEach(function (w, i) {
      var b = button('chip', w.word, function () { pick = i; [].forEach.call(row.children, function (c, j) { c.setAttribute('aria-pressed', j === i ? 'true' : 'false'); }); });
      b.title = w.means; b.setAttribute('aria-pressed', i === pick ? 'true' : 'false');
      row.appendChild(b);
    });
    add(box, p('lab', 'Your answer'), row);
    var boxes = [];
    if (threeBoxes) {
      var HEADS = ['In place', 'Not there yet', 'Would show it'];
      HEADS.forEach(function (h, j) {
        var re = new RegExp(h + ':\\s*([\\s\\S]*?)(?=\\s*(?:In place:|Not there yet:|Would show it:)|$)');
        var mm = cur.rest.match(re);
        var f = field(h, 'cr-' + fam + '-' + k + '-' + j, 2, mm ? mm[1].trim() : '', 600);
        boxes.push(f.input); add(box, f.box);
      });
    } else {
      var f = field('Say more, if you like', 'cr-' + fam + '-' + k, 3, cur.rest, 1500);
      f.input.placeholder = prompt || '';
      boxes.push(f.input); add(box, f.box);
      var noteP = p('mic-note'); var mic = Kit.speakButton(f.input, noteP);
      add(box, mic, noteP);
    }
    var st = status();
    var save = button('btn small', 'Save', function () {
      if (pick < 0) { st.textContent = 'Choose one of the three first.'; return; }
      var rest = threeBoxes
        ? ['In place', 'Not there yet', 'Would show it'].map(function (h, j) { var v = boxes[j].value.trim(); return v ? h + ': ' + v : ''; }).filter(Boolean).join(' ')
        : boxes[0].value.trim();
      var t = words[pick].word + '.' + (rest ? ' ' + rest : '');
      save.disabled = true; st.textContent = 'Saving…';
      Kit.ready(m).then(function () { return post({ block: fam + ':' + m.name, spindle: k, content: t, secret: m.words }); }).then(function (r) {
        save.disabled = false;
        if (!r.ok) { st.textContent = SAY.beach; return; }
        st.textContent = 'Saved. Thank you.';
        if (!mineBook) { mineBook = { who: m.name, block: {} }; data.books.push(mineBook); }
        mineBook.block = mineBook.block && typeof mineBook.block === 'object' ? mineBook.block : {};
        var c = mineBook.block[k];
        if (c && typeof c === 'object') c._ = t; else mineBook.block[k] = t;
      });
    });
    save.style.alignSelf = 'flex-start';
    add(box, save, st);
    return box;
  }

  // THE CONSTITUTION, shaped together: its six parts as six pages, each with Yes, Change or Not sure
  SCREENS.constitution = function () {
    return loading('constitution', 'The constitution', function () { return family('constitution'); }, function (d) {
      if (!d.spine) return [];
      var words = threeOf(d.law);
      return Kit.digits(d.spine).map(function (k, idx) {
        var sp = split(Kit.text(d.spine[k]));
        var heads = Kit.digits(d.spine[k]).map(function (j) { return split(Kit.text(d.spine[k][j])).title.replace(/^\d+\.\s*/, ''); }).filter(Boolean);
        return { eyebrow: 'Part ' + (idx + 1) + ' of the constitution', title: sp.title, after: function (body) {
          if (sp.gloss) add(body, p('big-s', Kit.cap(sp.gloss)));
          if (heads.length) { var ul = el('ul'); heads.forEach(function (h) { ul.appendChild(el('li', null, h)); }); add(body, ul); }
          add(body, answerBox('constitution', d, k, words, 'A reason, a question, or a wording you would prefer'));
        } };
      });
    });
  };

  // THE STANDARDS: the four groups as four pages, and where Community Recovery stands on each
  SCREENS.standards = function () {
    return loading('standards', 'The standards', function () { return family('standards'); }, function (d) {
      if (!d.spine) return [];
      var words = threeOf(d.law);
      return Kit.digits(d.spine).map(function (k) {
        var sp = split(Kit.text(d.spine[k]));
        var g = d.spine[k];
        var names = Kit.digits(g).map(function (j) { var t = lines(Kit.text(g[j])); return t.length > 1 ? t[1] : t[0]; });
        // readings so far, across every member's notebook, within this group
        var tally = [0, 0, 0];
        d.books.forEach(function (b) {
          var own = b.block && b.block[k];
          if (own != null) { var t0 = tokenOf(Kit.text(own), words); if (t0.i >= 0) tally[t0.i]++; }
          (function walk(n) {
            Kit.digits(n).forEach(function (j) {
              var c = n[j], t = typeof c === 'string' ? c : Kit.text(c);
              var tk = tokenOf(t, words); if (tk.i >= 0) tally[tk.i]++;
              if (c && typeof c === 'object') walk(c);
            });
          })(b.block && b.block[k]);
        });
        var stand = words.map(function (w, i) { return tally[i] + ' ' + w.word; }).join(' · ');
        return { eyebrow: 'The standards', title: sp.title.replace(/^[A-D]\.\s*/, ''), after: function (body) {
          var ul = el('ul'); names.forEach(function (x) { ul.appendChild(el('li', null, x)); }); add(body, ul);
          add(body, p('quiet-s', 'Where we stand so far, from members’ readings: ' + stand + '.'));
          add(body, answerBox('standards', d, k, words, '', true));
        } };
      });
    });
  };

  // ABOUT: the six policies as six short pages
  SCREENS.about = function () {
    return loading('about', 'About', function () { return policies(); }, function (b) {
      if (!b) return [];
      return ['2', '3', '4', '5', '6', '7'].map(function (k) {
        var sp = split(Kit.text(b[k]));
        return { eyebrow: 'About', title: Kit.cap(sp.title.toLowerCase()), after: function (body) {
          if (sp.gloss) add(body, p('big-s', Kit.cap(sp.gloss)));
          Kit.digits(b[k]).forEach(function (j) { body.appendChild(lead(p(null), Kit.text(b[k][j]))); });
        } };
      });
    });
  };

  // THE MAP: lero:earth on the earth beach, from the UK down to where Community Recovery stands
  function placeOf(node) {
    var t = Kit.text(node), first = t.split('\n')[0];
    var m = first.match(/^(.+?)\s+—\s+(\d+)\s+LEROs?\b/);
    var leros = t.split('\n').filter(function (l) { return /^·\s/.test(l); }).map(function (l) {
      var parts = l.slice(2).split(' — '), nm = parts[0] || '', o = nm.indexOf(' (');
      return { name: o > 0 ? nm.slice(0, o) : nm, where: o > 0 ? nm.slice(o + 2).replace(/\)$/, '') : '', what: parts[1] || '' };
    });
    return { name: m ? m[1] : first, count: m ? +m[2] : 0, leros: leros };
  }
  SCREENS.map = function () {
    return loading('map', 'The map', function () { return Beach.earth.get('lero:earth'); }, function (lero) {
      var found = null;
      (function walk(n, path) {
        if (found || !n || typeof n !== 'object') return;
        if (path.length && /·\s*Community Recovery \(/.test(Kit.text(n))) { found = path; return; }
        for (var i = 1; i <= 9; i++) walk(n[String(i)], path.concat(String(i)));
      })(lero, []);
      var path = found || ['3', '1', '1', '8', '2', '1'];
      var pages = [];
      for (var k = 3; k <= path.length; k++) {
        var node = Beach.place.at(lero, path.slice(0, k).join('.'));
        if (!node) break;
        var pl = placeOf(node);
        pages.push({ eyebrow: 'The LERO map', title: pl.name, after: (function (pl, last) { return function (body) {
          add(body, p('big-s', pl.count + (pl.count === 1 ? ' lived experience recovery organisation' : ' lived experience recovery organisations') + ' known here and beneath.'));
          if (last && pl.leros.length) { var ul = el('ul'); pl.leros.forEach(function (l) { ul.appendChild(el('li', null, l.name + (l.where ? ', ' + l.where : '') + (l.what ? ': ' + l.what : ''))); }); add(body, ul); }
        }; })(pl, k === path.length) });
      }
      return pages;
    });
  };

  // INTRODUCING COMMUNITY RECOVERY: spine:presentation, its four parts, one slide per page
  SCREENS.presentation = function () {
    return loading('presentation', 'Introducing Community Recovery', function () { return read('spine:presentation'); }, function (b) {
      var pages = [];
      Kit.digits(b).forEach(function (part, pi) {
        var pt = Kit.text(b[part]);
        Kit.digits(b[part]).forEach(function (sl) {
          var slide = b[part][sl], L = lines(Kit.text(Kit.at(slide, '1')));
          var notes = String(Kit.text(Kit.at(slide, '2')) || '').split(/\n\s*\n/)[0];
          if (!L.length) return;
          pages.push({ eyebrow: 'Part ' + (pi + 1) + ' · ' + pt.split(':')[0], title: L[0], after: function (body) {
            if (L[1]) add(body, p('big-s', L[1]));
            if (notes) add(body, p(null, notes.trim()));
          } });
        });
      });
      return pages;
    });
  };

  // YOUR DAYS: opened with the person's words, newest first
  SCREENS.days = function () {
    var s = screen();
    add(s, backBar('menu', 'Back to the menu'), p('eyebrow-s', 'Yours'), el('h1', null, 'Your days'));
    var list = el('div', 'list-s'); list.style.marginTop = '18px';
    var st = p('lead-s calm', S.days.length ? 'Opening your days with your words…' : 'Your kept days gather here, newest first.');
    add(s, st, list);
    if (!S.days.length) return s;
    var m = S.me;
    gray().then(function (G) {
      var sealedAway = 0;
      return S.days.slice().reverse().reduce(function (pr, d) {
        return pr.then(function () {
          var get = G.isGray(d.node) ? G.open(d.node, m.words, m.name) : Promise.resolve(Kit.text(d.node));
          return get.then(function (t) {
            if (t == null) { sealedAway++; return; }
            var n = el('div', 'note-s');
            add(n, p('meta', dateOf(d.address).toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })), p('txt', t || 'Kept.'));
            list.appendChild(n);
          });
        });
      }, Promise.resolve()).then(function () {
        st.textContent = sealedAway ? sealedAway + (sealedAway === 1 ? ' earlier day stays' : ' earlier days stay') + ' sealed under your old words.' : '';
      });
    }).catch(function () { st.textContent = 'Your days open here in a moment. Please try again shortly.'; });
    return s;
  };

  // WHAT I BRING, and MY INVITATION
  SCREENS.bring = function (arg) {
    var m = S.me, s = screen(), pp = S.passport || {};
    add(s, backBar('menu', 'Back to the menu'), el('h1', null, 'What I bring'));
    add(s, p('lead-s', 'These two are open for the community to see, so people can find each other. Your days stay yours.'));
    var form = el('form', 'stack'); form.noValidate = true; form.style.marginTop = '20px'; form.style.gap = '16px';
    var offer = field('What I can offer', 'cr-offer', 2, typeof pp['1'] === 'string' ? pp['1'] : Kit.text(pp['1']), 600);
    var use = field('What I could use', 'cr-use', 2, typeof pp['3'] === 'string' ? pp['3'] : Kit.text(pp['3']), 600);
    var st = status();
    var save = el('button', 'btn', 'Save'); save.type = 'submit'; save.style.minHeight = '50px';
    add(form, offer.box, use.box, save, st);
    add(s, form);
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      save.disabled = true; st.textContent = 'Saving…';
      Promise.all([
        post({ block: 'passport:' + m.name, spindle: '1', content: offer.input.value.trim(), secret: m.words }),
        post({ block: 'passport:' + m.name, spindle: '3', content: use.input.value.trim(), secret: m.words })
      ]).then(function (rs) {
        save.disabled = false;
        var ok = rs.every(function (r) { return r.ok; });
        st.textContent = ok ? 'Saved.' : SAY.beach;
        if (ok) { pp['1'] = offer.input.value.trim(); pp['3'] = use.input.value.trim(); S.passport = pp; }
      });
    });
    if (open('invite')) {
      var card = invitationCard();
      card.style.marginTop = '26px';
      add(s, card);
      if (arg && arg.invite) setTimeout(function () { card.scrollIntoView({ block: 'start', behavior: STILL.matches ? 'auto' : 'smooth' }); }, 60);
    }
    return s;
  };
  function myLink() {
    return location.origin + location.pathname + '?from=' + encodeURIComponent(S.me.name);
  }
  function invitationCard() {
    var c = el('div', 'card-s');
    var link = myLink();
    add(c, p('lab', 'My invitation'), p(null, 'Hand someone your card, or share your link. You’ll be shown as the one who invited them.'));
    var row = el('div', 'link-row');
    var url = el('input', 'url'); url.type = 'text'; url.readOnly = true; url.value = link; url.setAttribute('aria-label', 'Your invitation link');
    url.style.cssText = 'min-height:44px;font-size:14px;padding:8px 12px;border-radius:12px';
    var st = status();
    var copy = button('btn line small', 'Copy', function () {
      function fallback() { url.focus(); url.select(); try { document.execCommand('copy'); st.textContent = 'Copied.'; } catch (e) { st.textContent = 'Selected: copy it from here.'; } }
      if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(link).then(function () { st.textContent = 'Copied.'; }, fallback);
      else fallback();
    });
    add(row, url, copy);
    add(c, row);
    if ('NDEFReader' in window) {
      add(c, button('btn soft', 'Write my card', function () {
        st.textContent = 'Hold a blank card to the back of your phone…';
        try {
          var w = new window.NDEFReader();
          w.write({ records: [{ recordType: 'url', data: link }] }).then(function () {
            st.textContent = 'Your card is written. It carries your link, and nothing else.';
          }, function () { st.textContent = 'The card was out of reach. Hold it still against the phone and try again.'; });
        } catch (e) { st.textContent = 'Writing cards works in Chrome on Android. Try again there.'; }
      }));
      add(c, p('tiny-s', 'On an Android phone, hold a blank card to the back. A card carries your link only, never your words.'));
    } else {
      add(c, p('tiny-s', 'On other phones, any Android phone in the room can write it for you.'));
    }
    add(c, st);
    return c;
  }

  // CROSSINGS AND WORDS
  SCREENS.crossings = function () {
    var m = S.me, s = screen();
    add(s, backBar('today'), el('h1', null, 'Crossings and words'));
    var cs = crossings(), ws = wordsFor(), sn = seen();
    var later = [];
    cs.forEach(function (c) {
      var card = el('div', 'card-s gold'); card.style.marginTop = '18px';
      var lab = p('lab gold'); add(lab, el('span', 'spark'), 'A crossing for you');
      lab.firstChild.style.cssText = 'width:8px;height:8px;border-radius:50%;background:#F4C86A';
      add(card, lab, p('big-s', c.text.replace(/^CROSSING:\s*/i, '')));
      var more = el('div', 'stack'); more.hidden = true;
      var row = el('div', 'row');
      add(row, button('btn small', 'Tell me more', function () {
        more.hidden = !more.hidden;
        if (!more.hidden && !more.firstChild) tellMore(more, c.other);
        markSeen([c.id]);
      }), button('btn text', 'Later', function () { markSeen([c.id]); card.style.opacity = '0.7'; }));
      add(card, row, more);
      if (sn[c.id]) later.push(card); else add(s, card);
    });
    later.forEach(function (c) { add(s, c); });
    add(s, (function () { var x = p('lab', 'Words for you'); x.style.margin = '28px 0 10px'; return x; })());
    var list = el('div', 'list-s');
    if (!ws.length) list.appendChild(p('quiet-s', 'Words members leave for you arrive here, newest first.'));
    ws.forEach(function (w) {
      var n = el('div', 'note-s');
      add(n, p('meta', (w.who || 'Someone') + (w.when ? ' · ' + when(w.when) : '')), p('txt', w.text));
      list.appendChild(n);
    });
    add(s, list);
    var senders = [];
    ws.forEach(function (w) { if (w.who && senders.indexOf(w.who) < 0) senders.push(w.who); });
    if (senders.length) {
      var to = senders[0];
      var form = el('form', 'stack push'); form.noValidate = true; form.style.gap = '8px';
      var f = field('Leave a word for ' + to, 'cr-word-back', 2, '', 1500);
      if (senders.length > 1) {
        var chips = el('div', 'chips');
        senders.slice(0, 6).forEach(function (n, i) {
          var b = button('chip', n, function () { to = n; f.box.querySelector('label').textContent = 'Leave a word for ' + n; [].forEach.call(chips.children, function (c, j) { c.setAttribute('aria-pressed', j === i ? 'true' : 'false'); }); });
          b.setAttribute('aria-pressed', i === 0 ? 'true' : 'false');
          chips.appendChild(b);
        });
        add(form, chips);
      }
      var st = status();
      var send = el('button', 'btn', 'Send'); send.type = 'submit'; send.style.minHeight = '50px';
      add(form, f.box, send, st);
      add(s, form);
      form.addEventListener('submit', function (ev) {
        ev.preventDefault();
        var t = f.input.value.trim(); if (!t) return;
        send.disabled = true; st.textContent = 'Sending…';
        note('pool:' + to, m.name, t).then(function (r) {
          send.disabled = false;
          if (!r.ok) { st.textContent = SAY.beach; return; }
          f.input.value = ''; st.textContent = 'Sent to ' + to + '.';
        });
      });
    }
    markSeen(ws.map(function (w) { return w.id; }));
    return s;
  };
  function tellMore(box, other) {
    if (!other) { add(box, p('quiet-s', 'Ask a volunteer at a session to introduce you.')); return; }
    add(box, p('quiet-s', 'Opening…'));
    read('passport:' + other).then(function (pp) {
      box.textContent = '';
      var offer = pp ? Kit.text(pp['1']) : '', use = pp ? Kit.text(pp['3']) : '';
      add(box, p('lab', other + ' can offer'), p(null, offer || 'Something to talk about in person.'));
      if (use) add(box, p('lab', other + ' could use'), p(null, use));
      var f = field('Leave ' + other + ' a word', 'cr-cross-' + other, 2, '', 1500);
      var st = status();
      var b = button('btn small', 'Send', function () {
        var t = f.input.value.trim(); if (!t) return;
        b.disabled = true; st.textContent = 'Sending…';
        note('pool:' + other, S.me.name, t).then(function (r) {
          b.disabled = false;
          st.textContent = r.ok ? 'Sent to ' + other + '.' : SAY.beach;
          if (r.ok) f.input.value = '';
        });
      });
      b.style.alignSelf = 'flex-start';
      add(box, f.box, b, st);
    }, function () { box.textContent = SAY.beach; });
  }

  // THE ROOM: pool:community-recovery, newest first, and a note to leave
  SCREENS.room = function () {
    var m = S.me, s = screen();
    add(s, backBar('menu', 'Back to the menu'), p('eyebrow-s', 'Us'), el('h1', null, 'The room'));
    var lead0 = p('lead-s', 'Notes for everyone who looks after Community Recovery. Everyone can read them.');
    var list = el('div', 'list-s'); list.style.marginTop = '18px';
    add(s, lead0, list);
    read('pool:community-recovery').then(function (pool) {
      var ns = Beach.notes(pool).slice().reverse();
      ns.forEach(function (n) {
        var d = el('div', 'note-s');
        add(d, p('meta', (n.who || 'Someone') + (n.when ? ' · ' + when(n.when) : '')), p('txt', n.text));
        list.appendChild(d);
      });
    }, function () { list.appendChild(p('quiet-s', SAY.beach)); });
    var form = el('form', 'stack push'); form.noValidate = true; form.style.gap = '8px';
    var f = field('Leave a note', 'cr-room', 3, '', 1500);
    var st = status();
    var send = el('button', 'btn', 'Leave it'); send.type = 'submit'; send.style.minHeight = '50px';
    add(form, f.box, send, st);
    add(s, form);
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var t = f.input.value.trim(); if (!t) return;
      send.disabled = true; st.textContent = 'Leaving it…';
      note('pool:community-recovery', m.name, t).then(function (r) {
        send.disabled = false;
        if (!r.ok) { st.textContent = SAY.beach; return; }
        f.input.value = ''; st.textContent = 'It’s in the room.';
        var d = el('div', 'note-s'); add(d, p('meta', m.name + ' · ' + when(new Date().toISOString())), p('txt', t));
        list.insertBefore(d, list.firstChild);
      });
    });
    return s;
  };

  // ORGANISATIONS, always through a person
  function shortName(name) {
    return String(name || '').toLowerCase().replace(/&/g, ' and ').replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 40);
  }
  SCREENS['org-new'] = function () {
    var m = S.me, s = screen();
    if (!m) return SCREENS.welcome();
    add(s, backBar('menu'), p('eyebrow-s', 'Joining for an organisation'), el('h1', null, 'What your organisation offers'));
    var intro = p('lead-s'); add(intro, 'You’re signed in as ', el('strong', null, m.name), '. Your organisation stands on you as its first person, and you can add colleagues later.');
    add(s, intro);
    var form = el('form', 'stack'); form.noValidate = true; form.style.marginTop = '18px'; form.style.gap = '14px';
    var nm = field('Its name', 'org-name', false, '', 80);
    var of = field('What it offers, where and when', 'org-offer', 3, '', 1000);
    var rc = field('How to reach it', 'org-reach', false, '', 200);
    add(form, nm.box, of.box, rc.box);
    var from = inviter();
    if (from) {
      var v = el('div', 'note-s'); v.style.flexDirection = 'row'; v.style.alignItems = 'center';
      var vp = p(null); add(vp, 'Vouched for by ', el('strong', null, from), ', who brought you in.');
      add(v, vp); add(form, v);
    }
    var st = status();
    var go = el('button', 'btn', 'Add our organisation'); go.type = 'submit';
    add(form, go, st, (function () { var x = p('quiet-s', 'Members see what it offers alongside each other’s gifts. People first, services alongside.'); x.style.textAlign = 'center'; return x; })());
    add(s, form);
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var name = nm.input.value.trim(), base = shortName(name);
      if (base.length < 2) { st.textContent = 'Please give its name.'; nm.input.focus(); return; }
      go.disabled = true; st.textContent = 'One moment…';
      Kit.index().then(function (idx) {
        var have = Kit.names(idx), short = base, k = 2;
        while (have.indexOf('org:' + short) >= 0) short = base + '-' + (k++);
        var block = 'org:' + short;
        var content = { _: name, 1: of.input.value.trim(), 2: rc.input.value.trim(), 3: '', 4: m.name, 5: from, 6: new Date().toISOString() };
        return post({ block: block, content: content, new_lock: m.words }).then(function (r) {
          if (!r.ok) throw new Error('beach');
          return append('arrivals', { _: 'organisation', 1: m.name, 2: from, 4: new Date().toISOString(), 5: block }).then(function () {
            sset('cr:app:org:' + m.name, '1');
            S.orgs.push({ block: block, data: content });
            S.org = true;
            show('org', block, true);
          });
        });
      }).catch(function () { go.disabled = false; st.textContent = SAY.beach; });
    });
    return s;
  };
  SCREENS.org = function (block) {
    var m = S.me, s = screen();
    var o = S.orgs.filter(function (x) { return x.block === block; })[0];
    if (!o) return SCREENS.menu();
    var d = o.data;
    add(s, backBar('menu'), p('eyebrow-s', 'Our organisation'), el('h1', null, Kit.text(d) || 'Our organisation'));
    var form = el('form', 'stack'); form.noValidate = true; form.style.marginTop = '18px'; form.style.gap = '14px';
    var of = field('What it offers, where and when', 'org-offer2', 3, Kit.text(d['1']), 1000);
    var rc = field('How to reach it', 'org-reach2', false, Kit.text(d['2']), 200);
    var fr = field('Who it is for, if you like', 'org-for', 2, Kit.text(d['3']), 400);
    var people = peopleOf(d);
    var pl = field('Its people, by their names here', 'org-people', false, people.join(', '), 300);
    var vouch = Kit.text(d['5']);
    add(form, of.box, rc.box, fr.box, pl.box, p('quiet-s', 'Add colleagues by the names they use here, so it carries on beyond any one person.'));
    if (vouch) { var vp = p('quiet-s'); add(vp, 'Vouched for by ', el('strong', null, vouch), '.'); add(form, vp); }
    var st = status();
    var go = el('button', 'btn', 'Save'); go.type = 'submit';
    add(form, go, st);
    add(s, form);
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var ppl = pl.input.value.split(',').map(function (x) { return Kit.app.tidyName(x); }).filter(Boolean);
      if (ppl.indexOf(m.name) < 0 && peopleOf(d)[0] === m.name) ppl.unshift(m.name);
      var content = { _: Kit.text(d), 1: of.input.value.trim(), 2: rc.input.value.trim(), 3: fr.input.value.trim(), 4: ppl.join(', '), 5: vouch, 6: Kit.text(d['6']) };
      go.disabled = true; st.textContent = 'Saving…';
      post({ block: block, content: content, secret: m.words, confirm: true }).then(function (r) {
        go.disabled = false;
        if (!r.ok) { st.textContent = r.status === 403 ? 'Its founder’s words keep this page. Ask them to make the change.' : SAY.beach; return; }
        o.data = content; st.textContent = 'Saved.';
      });
    });
    return s;
  };

  // FORGET ME: asked here, in the page
  SCREENS.forget = function () {
    var m = S.me, s = screen();
    if (!m) return SCREENS.welcome();
    add(s, backBar('menu'), el('h1', null, 'Forget me'));
    add(s, p('lead-s', 'This removes ' + m.name + ' from Community Recovery’s place: your card, your days, what you bring, your notebooks and your room. It cannot be brought back.'));
    add(s, p('quiet-s', 'Your arrival stays in the record as a name only. Ask a keeper if you want that gone too.'));
    var box = el('div', 'confirm-s'); box.style.marginTop = '22px';
    var st = status();
    var busy = false;
    var yes = button('btn', 'Yes, forget me', function () {
      if (busy) return;
      busy = true; yes.disabled = true; no.disabled = true; st.textContent = 'Removing everything…';
      // every step can be repeated safely, so a second tap simply tries again
      forgetAll().then(function (res) {
        busy = false;
        if (res.ok) { signOut(); show('left', { notes: res.notes }, true); return; }
        yes.disabled = false; no.disabled = false; st.textContent = SAY.beach;
      }, function () { busy = false; yes.disabled = false; no.disabled = false; st.textContent = SAY.beach; });
    });
    var no = button('btn line', 'Keep me', function () { show('menu', null, true); });
    add(box, yes, no, st);
    add(s, box);
    return s;
  };
  SCREENS.left = function (arg) {
    var s = screen('center');
    add(s, ring({ cls: 'small', dots: false }));
    var h = el('h1', null, 'You’ve left'); h.style.marginTop = '18px';
    add(s, h, p('lead-s', 'Everything you kept here is gone.'));
    ((arg && arg.notes) || []).forEach(function (n) { var x = p('quiet-s', n); x.style.marginTop = '12px'; add(s, x); });
    var b = button('btn line small', 'Back to the start', function () { show('welcome', null, true); });
    b.style.marginTop = '24px';
    add(s, b);
    return s;
  };
  // Gone: no block, or a block that holds only the forgotten line at its root.
  function isGone(b) { return b == null || Kit.text(b) === FORGOTTEN; }
  // A block written whole as the one forgotten line, so nothing beneath it stays (on the beach a
  // deleted block can keep its children). A latched block takes the person's words; an open one
  // (a room) is written without them.
  function forgetWhole(block, words) {
    var c = { _: FORGOTTEN };
    return post({ block: block, content: c, secret: words, confirm: true }).then(function (r) {
      return r.ok ? r : post({ block: block, content: c, confirm: true });
    });
  }
  // Every block of the person's is first written whole as the forgotten line, then removed as
  // Kit.leave removes them (it leaves pool: blocks, so the room is removed here too). The person
  // is taken out of any organisation; one they alone stand in goes with them. Resolves
  // {ok, notes}: ok once nothing of theirs is left but the forgotten line; notes say plainly
  // what a founder still has to do.
  function forgetAll() {
    var m = S.me, notes = [];
    var low = m.name.toLowerCase();
    var orgs = S.orgs.slice();
    var mine = [];
    return orgs.reduce(function (pr, o) {
      return pr.then(function () {
        var ppl = peopleOf(o.data), rest = ppl.filter(function (x) { return x.toLowerCase() !== low; });
        if (!rest.length) return forgetWhole(o.block, m.words).then(function () { return Kit.wipe(o.block, m.words); });
        var c = {}; Object.keys(o.data).forEach(function (k) { c[k] = o.data[k]; }); c['4'] = rest.join(', ');
        return post({ block: o.block, content: c, secret: m.words, confirm: true }).then(function (r) {
          if (!r.ok) notes.push('Ask ' + (ppl[0] || 'its founder') + ' to take your name off ' + (Kit.text(o.data) || 'your organisation') + '.');
        });
      });
    }, Promise.resolve()).then(function () {
      return Kit.index();
    }).then(function (idx) {
      mine = Kit.names(idx).filter(function (n) {
        return n.indexOf(':') > 0 && n.slice(n.lastIndexOf(':') + 1) === m.name && !/^(sed|grain|org):/.test(n);
      });
      ['passport:', 'now:', 'mark:', 'pool:'].forEach(function (pre) { if (mine.indexOf(pre + m.name) < 0) mine.push(pre + m.name); });
      return mine.reduce(function (pr, b) {
        return pr.then(function () {
          return read(b).then(function (have) { return have && !isGone(have) ? forgetWhole(b, m.words) : null; }, function () { return null; });
        });
      }, Promise.resolve());
    }).then(function () {
      return read('pool:' + m.name).then(function (pool) { return pool ? Kit.wipe('pool:' + m.name, m.words) : null; }, function () { return null; });
    }).then(function () {
      return Kit.leave().catch(function () { return false; });
    }).then(function () {
      return Promise.all(mine.map(function (b) { return read(b).catch(function () { return { _: 'unreachable' }; }); }));
    }).then(function (left) {
      var ok = left.every(isGone);
      if (ok) { sset('cr:app:org:' + m.name, null); sset(seenKey(), null); sset(levelKey(), null); sset('cr:app:reset:' + m.name, null); }
      return { ok: ok, notes: notes };
    }, function () { return { ok: false, notes: notes }; });
  }

  // ---- the footer: Community Recovery's own lines, policies part 1, as nav.js draws them ---------
  var reading = null;
  function policies() {
    if (!reading) reading = read('policies').then(function (b) { if (!b) reading = null; return b; }, function (e) { reading = null; throw e; });
    return reading;
  }
  // Words with their links, as nav.js makes them, keeping only what the app may link to: help
  // lines (tel: and sms:), email, and Community Recovery's Facebook. A web address stays as words.
  var LINKS = new RegExp([
    '([\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+)',
    '(Facebook:\\s*(\\w+(?:\\.\\w+)*))',
    '\\b(text\\s+\\w+\\s+to\\s+(\\d{5}))\\b',
    '\\b(999|111|116 ?\\d{3}|0\\d{3} ?\\d{3} ?\\d{4}|0\\d{4} ?\\d{6}|0\\d{2} ?\\d{4} ?\\d{4})\\b'
  ].join('|'), 'gi');
  function linked(s, before) {
    var frag = document.createDocumentFragment(), skip = before ? before.length : 0, last = skip, m;
    s = (before || '') + s;
    LINKS.lastIndex = 0;
    while ((m = LINKS.exec(s))) {
      var from = Math.max(m.index, skip), end = m.index + m[0].length;
      if (end <= skip) continue;
      if (from > last) frag.appendChild(document.createTextNode(s.slice(last, from)));
      var a = el('a', null, s.slice(from, end));
      if (m[1]) a.href = 'mailto:' + m[1];
      else if (m[2]) { a.href = 'https://www.facebook.com/' + m[3]; a.rel = 'noopener'; if (!/facebook/i.test(a.textContent)) a.setAttribute('aria-label', a.textContent + ' on Facebook'); }
      else if (m[4]) a.href = 'sms:' + m[5];
      else { a.href = 'tel:' + m[6].replace(/\D/g, ''); a.setAttribute('aria-label', 'Call ' + m[6]); }
      frag.appendChild(a);
      last = end;
    }
    if (last < s.length) frag.appendChild(document.createTextNode(s.slice(last)));
    return frag;
  }
  function lead(pe, s) {
    var mm = /^([^:?]{1,80}[:?])\s+/.exec(s);
    if (mm && !/[.!]\s/.test(mm[1])) { pe.appendChild(el('strong', null, mm[1])); pe.appendChild(document.createTextNode(' ')); pe.appendChild(linked(s.slice(mm[0].length), mm[0])); }
    else pe.appendChild(linked(s));
    return pe;
  }
  var HELP = 'Need help now? In an emergency call 999. For urgent mental health help call NHS 111 and choose option 2. Samaritans, any time: 116 123. Text SHOUT to 85258.';
  function drawFooter(block) {
    var box = document.getElementById('legal');
    if (!box) return;
    box.textContent = '';
    function line(a) { return block ? String(Kit.text(Kit.at(block, a.replace('.', ''))) || '').trim() : ''; }
    if (block) {
      var who = el('p');
      ['1.1', '1.2'].forEach(function (a) { var t = line(a); if (!t) return; if (who.firstChild) who.appendChild(document.createTextNode(' ')); lead(who, t); });
      if (who.firstChild) box.appendChild(who);
      ['1.3', '1.4'].forEach(function (a) { if (line(a)) box.appendChild(lead(el('p'), line(a))); });
    }
    box.appendChild(lead(el('p', 'cr-help'), line('1.5') || HELP));
    if (line('1.6')) box.appendChild(lead(el('p', 'tiny-s'), line('1.6')));
  }

  // ---- the installed app ------------------------------------------------------------------------
  if ('serviceWorker' in navigator && /^https?:$/.test(location.protocol)) {
    window.addEventListener('load', function () {
      navigator.serviceWorker.register('app-sw.js', { scope: 'app.html' }).catch(function () {});
    });
  }

  // ---- begin ------------------------------------------------------------------------------------
  drawFooter(null);
  policies().then(drawFooter, function () {});
  S.me = Kit.me();
  if (!S.me) { show('welcome', null, true); return; }
  load().then(function (ok) {
    show(ok ? 'today' : 'welcome', null, true);
  }, function () {
    app.textContent = '';
    var s = screen('center');
    add(s, ring({ cls: 'small', dots: false }), (function () { var x = p('lead-s', SAY.beach); x.style.marginTop = '18px'; return x; })(),
      (function () { var b = button('btn line small', 'Try again', function () { location.reload(); }); b.style.marginTop = '18px'; return b; })(),
      (function () { var x = p('tiny-s', HELP_ALL); x.style.marginTop = '24px'; return x; })());
    app.appendChild(s);
  });
})();
