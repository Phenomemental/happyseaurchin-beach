/* zero.js — the page of Zero (spine:zero 4.2 to 4.6). The law, the seals
   and the working-out of a table are in zero-law.js; this file reads and
   writes the beach, draws the table, and runs the computer seats.

   Every move is a keyless write to the beach (spine:zero 3): hidden moves
   go to pool:zero as a seal first and are shown when every seal is in. The
   host's browser runs the computer seats with the host's own Anthropic key,
   kept in this browser's storage and nowhere else, and writes each played
   place into the tree, zero. Every phone works the table out again from the
   room on every read, so nobody's page is the referee.

   On this computer only (localhost), ?beach=<origin> points the page at a
   test beach, as door.js allows; a published page writes to the main beach. */
(function () {
  'use strict';
  var Z = window.ZeroLaw;
  var LOCAL = /^(localhost|127\.0\.0\.1|\[::1\])$/.test(location.hostname);
  var params = new URLSearchParams(location.search);
  var BEACH = 'https://beach.happyseaurchin.com';
  if (LOCAL && params.get('beach')) BEACH = params.get('beach').replace(/\/+$/, '');
  var WIRE = BEACH + '/.well-known/pscale-beach';
  var ROOM = 'pool:zero', TREE = 'zero', LAW = 'function:zero';
  var API = 'https://api.anthropic.com/v1/messages', MODEL = 'claude-haiku-5-5';
  var KEY = 'zero.key', ME = 'zero.me', TABLE = 'zero.table', CP = 'zero.computers';
  var LIMIT_MS = 8000, POLL_MS = 2500, QUIET_MS = 10000, WAIT_BEFORE_GO_ON = 120000, PAUSE_BEFORE_DEAL = 8000;
  var LIVE_MS = 2 * 60 * 60 * 1000;   // a table with nothing for two hours no longer holds its place
  if (LOCAL && params.get('fast')) { POLL_MS = 500; PAUSE_BEFORE_DEAL = 1200; WAIT_BEFORE_GO_ON = 4000; }   // a builder's test

  var $ = function (id) { return document.getElementById(id); };
  function el(tag, cls, text) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text != null) n.textContent = text;
    return n;
  }
  function sget(k) { try { return localStorage.getItem(k); } catch (e) { return null; } }
  function sset(k, v) { try { if (v == null) localStorage.removeItem(k); else localStorage.setItem(k, v); } catch (e) {} }
  function jget(k) { try { return JSON.parse(sget(k) || 'null'); } catch (e) { return null; } }
  function jset(k, v) { sset(k, v == null ? null : JSON.stringify(v)); }
  function show(id) {
    ['lobby', 'waiting', 'table', 'end', 'trailview'].forEach(function (s) { $(s).hidden = s !== id; });
    window.scrollTo(0, 0);
  }
  function cap(w) { w = String(w || ''); return w.charAt(0).toUpperCase() + w.slice(1); }
  var HAND_WORDS = ['', 'one', 'two', 'three', 'four'];

  // ---- the beach ---------------------------------------------------------------------
  var offset = 0;   // the beach's clock less this device's
  function clock(r) {
    var h = r && r.headers && r.headers.get('X-Pscale-Now');
    var iso = h ? h.split('|')[0].trim() : '';
    var t = Date.parse(iso);
    if (!isNaN(t)) offset = t - Date.now();
  }
  function now() { return Date.now() + offset; }
  // A read: {status, json}; status 0 is no answer. Never throws.
  function look(block, query) {
    var ctl = window.AbortController ? new AbortController() : null;
    var timer = setTimeout(function () { if (ctl) ctl.abort(); }, LIMIT_MS);
    var url = WIRE + '?block=' + encodeURIComponent(block) + (query || '');
    return fetch(url, { cache: 'no-store', signal: ctl ? ctl.signal : undefined }).then(function (r) {
      clearTimeout(timer);
      clock(r);
      return r.json().catch(function () { return null; }).then(function (j) { return { status: r.status, json: j }; });
    }, function () {
      clearTimeout(timer);
      return { status: 0, json: null };
    });
  }
  // A write in the beach's own shape. Never throws.
  function write(block, body) {
    return fetch(WIRE + '?block=' + encodeURIComponent(block), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      cache: 'no-store',
      credentials: 'omit',
      referrerPolicy: 'no-referrer'
    }).then(function (r) {
      clock(r);
      return r.json().catch(function () { return null; }).then(function (j) {
        return { status: r.status, json: j, ok: r.ok && !!j && j.ok !== false };
      });
    }, function () { return { status: 0, json: null, ok: false }; });
  }
  function refusal(r) {
    if (!r || !r.status) return 'The beach is out of reach just now. Trying again.';
    if (r.status === 403 || (r.json && /lock/.test(r.json.code || ''))) return 'The beach refused that move: the room is locked.';
    return 'The beach didn’t take that move' + (r.json && r.json.error ? ': ' + r.json.error : '.');
  }

  // An entry in the room. Position 3 is left empty so the beach stamps it.
  function post(kind, fields) {
    var c = { _: fields.text, 1: fields.who, 2: fields.at || '', 6: fields.code, 7: kind };
    if (fields.hand) c['8'] = String(fields.hand);
    if (fields.data) c['9'] = fields.data;
    return write(ROOM, { append: true, content: c });
  }

  // ---- reading the room ----------------------------------------------------------------
  // The room only grows, so it is read by its outline (the top-level nodes)
  // and then only the nodes a table needs; a node is kept once a later one
  // has begun, since the beach fills slots in order.
  var room = { floor: 0, cache: {} };
  function outline() {
    var guess = room.floor || 2;
    return look(ROOM, '&pscale=' + Math.max(0, guess - 1)).then(function (r) {
      if (r.status === 404) return { floor: 0, tops: [] };
      if (r.status !== 200 || !r.json) throw new Error('room');
      var f = Number(r.json.floor) || 1;
      if (f !== guess) { room.floor = f; return outline(); }
      var tops = (r.json.entries || []).map(function (e) { return String(e.address); }).filter(function (a) { return /^[1-9]$/.test(a); }).sort();
      return { floor: f, tops: tops };
    });
  }
  function pad(addr, f) { addr = String(addr || ''); return addr.length >= f ? addr : '0'.repeat(f - addr.length) + addr; }
  // Every entry from address `from` on; with no `from`, the recent ones.
  function readRoom(from) {
    return outline().then(function (o) {
      if (o.floor !== room.floor) { room.floor = o.floor; room.cache = {}; }
      var F = o.floor;
      if (!F) return [];
      var tops = o.tops, want;
      var start = from ? pad(from, F) : '';
      if (start) {
        want = tops.filter(function (t) { return t >= start[0]; });
        if (start[0] === '0') want.unshift('0');
      } else {
        want = tops.slice(F <= 2 ? -4 : -2);
        if (F >= 2 && tops.length < 2) want.unshift('0');
      }
      var max = tops[tops.length - 1] || '0';
      return Promise.all(want.map(function (t) {
        var c = room.cache[t];
        if (c && c.complete) return c.entries;
        return look(ROOM, '&spindle=' + (t === '0' ? '0' : t + '0'.repeat(F - 1))).then(function (r) {
          if (r.status !== 200 || !r.json) throw new Error('room');
          var list = Z.entriesIn(r.json, t, F);
          room.cache[t] = { entries: list, complete: t !== max && t < max };
          return list;
        });
      })).then(function (lists) {
        var all = [].concat.apply([], lists);
        if (start) all = all.filter(function (e) { return Z.byAddr(e, { addr: start }) >= 0; });
        return all.sort(Z.byAddr);
      });
    });
  }
  // The tables seen in recent entries: {key: {code, open, closed, last, places}}.
  function liveTables(list) {
    var out = {};
    list.forEach(function (e) {
      var k = Z.codeKey(e.code);
      if (!k) return;
      var t = out[k] || (out[k] = { code: e.code, open: null, closed: false, last: 0, places: {} });
      var s = Date.parse(e.stamp);
      if (!isNaN(s) && s > t.last) t.last = s;
      if (e.kind === 'open' && !t.open) t.open = e;
      if (e.kind === 'close') t.closed = true;
      if ((e.kind === 'open' || e.kind === 'deal') && e.at) t.places[Z.digitsOf(e.at)] = true;
    });
    Object.keys(out).forEach(function (k) {
      var t = out[k];
      t.live = !t.closed && !!t.open && now() - t.last < LIVE_MS;
    });
    return out;
  }
  function claimed(tables) {
    var c = {};
    Object.keys(tables).forEach(function (k) { if (tables[k].live) Object.keys(tables[k].places).forEach(function (p) { c[p] = k; }); });
    return c;
  }
  function readTree() {
    return look(TREE).then(function (r) {
      if (r.status === 404) return { _: 'The tree of Zero.' };
      if (r.status !== 200 || !r.json) throw new Error('tree');
      return r.json;
    });
  }

  // ---- the law, as the computer seats read it --------------------------------------------
  var LAW_TEXT = [
    'The table: nine seats around a zero word. Every seat starts with 10 chips. A game is four hands.',
    'Seats: each seat writes one word that belongs under the zero, hidden until all are in. Words that match after lower-casing and singular form knock each other out for that hand.',
    'The turn: every seat still in writes one line that holds the surviving ring, and stakes from 0 to all its chips, both hidden until all are in.',
    'The showdown: every seat votes for one line, never its own. Most votes takes the whole pot; that line becomes the voice of the place, signed. A tie splits the pot.',
    'Deeper: the winner picks one surviving word, and it becomes the next zero, one level down.',
    'Out: a seat knocked out sits out that hand only. A seat with no chips writes no more words or lines, but keeps its vote to the end.',
    'The end: after four hands, most chips wins. The winning lines read from the top of the trail down are the game’s poem.',
    'The craft: a line wins by holding the whole ring in a breath. Compression is the skill.',
    'Computer seats fill any seat no person takes. Each writes as if alone, in its own voice, short and plain, and collisions are allowed.'
  ].join('\n');
  var lawRead = false;
  function readLaw() {
    if (lawRead) return Promise.resolve(LAW_TEXT);
    return look(LAW).then(function (r) {
      if (r.status === 200 && r.json) {
        var lines = [];
        for (var i = 1; i <= 9; i++) { var t = Z.textOf(r.json[String(i)]); if (t) lines.push(t); }
        if (lines.length >= 5) { LAW_TEXT = lines.join('\n'); lawRead = true; }
      }
      return LAW_TEXT;
    });
  }

  // ---- the computer seats ---------------------------------------------------------------
  // Each seat's own voice (function:zero 9): kept apart so they don't sound alike (spine:zero 6).
  var VOICES = {
    'Child': 'You are seven years old. You say what you see, plainly and with delight: small words, real things, no grown-up phrases.',
    'Sailor': 'You have spent your life at sea. You think in weather, watches, ropes, distance and the long way home. Gruff and spare.',
    'Poet': 'You hear words before you see them. One clean image, said once. Never ornate, never long, never rhyming for its own sake.',
    'Engineer': 'You think in mechanisms, signals, energy and measurement. Precise, dry, a little literal.',
    'Grandmother': 'You raised a family and have outlived friends. Care, kitchens, waiting up, what we carry for each other. Warm and plain.',
    'Chef': 'You live by heat, timing and the table. Food, fire, service, the room after everyone has gone home.',
    'Farmer': 'You rise before light for the animals and the weather. Seasons, soil, birth, mud and patience. Few words.',
    'Night nurse': 'You work the ward through the small hours. Quiet watching, care, the hush at four in the morning. Gentle and exact.'
  };
  var plainCall = false;
  function claude(key, system, user, schema) {
    var body = { model: MODEL, max_tokens: 1024, system: system, messages: [{ role: 'user', content: user }], output_config: { effort: 'low' } };
    if (!plainCall) body.output_config.format = { type: 'json_schema', schema: schema };
    return fetch(API, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'x-api-key': key,
        'anthropic-version': '2023-06-01',
        'anthropic-dangerous-direct-browser-access': 'true'
      },
      body: JSON.stringify(body)
    }).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (j) {
        if (r.status === 400 && !plainCall && !/credit/i.test((j.error && j.error.message) || '')) {
          plainCall = true;
          return claude(key, system, user, schema);
        }
        if (r.status !== 200) { var e = new Error(whyNot(r.status, j)); e.status = r.status; throw e; }
        if (j.stop_reason === 'refusal') throw new Error('Claude declined that move.');
        var text = (j.content || []).filter(function (b) { return b.type === 'text'; }).map(function (b) { return b.text; }).join('');
        var m = /\{[\s\S]*\}/.exec(text);
        if (!m) throw new Error('Claude’s answer had no move in it.');
        return JSON.parse(m[0]);
      });
    }, function () { var e = new Error('Claude couldn’t be reached. Check the connection.'); e.status = 0; throw e; });
  }
  function whyNot(status, j) {
    var msg = (j && j.error && j.error.message) || '';
    if (status === 401 || status === 403) return 'Claude didn’t accept this key.';
    if (status === 400 && /credit/i.test(msg)) return 'This key has run out of credit.';
    if (status === 429 || status === 529 || status >= 500) return 'Claude is busy. Trying again.';
    return 'Claude couldn’t answer just now' + (msg ? ': ' + msg : '.');
  }
  function persona(name) {
    return 'You are one of the computer players in Zero, a word game for nine seats, playing as the ' + name + '.\n' +
      (VOICES[name] || 'Write in your own voice, short and plain.') + '\n\n' +
      'The law of Zero, as the table reads it:\n' + LAW_TEXT + '\n\n' +
      'Play as if you were alone at the table: you never see anyone’s move before making yours. Stay in your own voice. Reply only with the JSON asked for.';
  }
  var S_WORD = { type: 'object', additionalProperties: false, required: ['word'], properties: { word: { type: 'string' } } };
  var S_LINE = { type: 'object', additionalProperties: false, required: ['line', 'stake'], properties: { line: { type: 'string' }, stake: { type: 'integer' } } };
  var S_SEAT = { type: 'object', additionalProperties: false, required: ['seat', 'why'], properties: { seat: { type: 'integer' }, why: { type: 'string' } } };
  function trailWords(h) {
    var up = TREE_JSON ? Z.trail(TREE_JSON, h.place.slice(0, -1)).map(function (x) { return x.word; }).filter(Boolean) : [];
    return up.concat([h.zero]);
  }
  function askWord(key, t, h, s) {
    var name = Z.nameOf(t, s), tw = trailWords(h);
    var user = 'The zero word is “' + h.zero + '”.' + (tw.length > 1 ? ' It sits at the end of the trail ' + tw.join(' › ') + '.' : '') +
      '\nWrite one word that belongs under it, the word a ' + name.toLowerCase() + ' would think of. Nobody else at the table should think of it, though collisions happen. One word, no spaces. Answer as {"word": "..."}.';
    return tries(3, function () {
      return claude(key, persona(name), user, S_WORD).then(function (j) {
        var w = Z.oneWord(j.word);
        if (!w.ok || Z.same(w.word, h.zero)) throw new Error('again');
        return { move: w.word };
      });
    });
  }
  function askLine(key, t, h, s) {
    var name = Z.nameOf(t, s), chips = h.chipsBefore[s] || 0;
    var ring = h.ring.map(function (r) { return h.words[r]; });
    var user = 'The zero word is “' + h.zero + '”. After the flop the surviving ring is: ' + ring.join(', ') + '. Your own word in it is “' + h.words[s] + '”.' +
      '\nWrite one short line, in your own voice, that holds the whole ring in a breath. Under twenty words; shorter usually wins.' +
      '\nYou have ' + chips + ' chips. Stake from 0 to ' + chips + ' on your line: more if you believe the table will vote for it. Answer as {"line": "...", "stake": n}.';
    return tries(3, function () {
      return claude(key, persona(name), user, S_LINE).then(function (j) {
        var l = Z.oneLine(j.line);
        if (!l.ok) throw new Error('again');
        return { move: l.line, stake: Math.max(0, Math.min(chips, Math.round(Number(j.stake) || 0))) };
      });
    });
  }
  function askVote(key, t, h, s) {
    var name = Z.nameOf(t, s);
    var options = Object.keys(h.lines).map(Number).filter(function (x) { return x !== s; });
    var ring = h.ring.map(function (r) { return h.words[r]; });
    var user = 'The zero word is “' + h.zero + '” and the ring is: ' + ring.join(', ') + '. The lines:\n' +
      Object.keys(h.lines).map(function (x) { return 'seat ' + x + ' (' + Z.nameOf(t, Number(x)) + '): ' + h.lines[x].line; }).join('\n') +
      '\nYou are seat ' + s + '. Vote for the one line that best holds the whole ring in a breath; never your own. Say why in a few words, in your voice. Answer as {"seat": n, "why": "..."}.';
    return tries(3, function () {
      return claude(key, persona(name), user, S_SEAT).then(function (j) {
        var v = Number(j.seat);
        if (options.indexOf(v) < 0) throw new Error('again');
        return { move: String(v), why: String(j.why || '').slice(0, 140) };
      });
    }).catch(function (e) {
      if (e.status != null) throw e;
      return { move: String(options[Math.floor(Math.random() * options.length)]), why: '' };
    });
  }
  function askPick(key, t, h, s) {
    var name = Z.nameOf(t, s);
    var user = 'Your line won the vote at “' + h.zero + '”. Pick one surviving word to become the next zero, one level deeper:\n' +
      h.ring.map(function (r) { return 'seat ' + r + ': ' + h.words[r]; }).join('\n') + '\nAnswer as {"seat": n, "why": "..."}.';
    return tries(3, function () {
      return claude(key, persona(name), user, S_SEAT).then(function (j) {
        var v = Number(j.seat);
        if (h.ring.indexOf(v) < 0) throw new Error('again');
        return v;
      });
    }).catch(function (e) {
      if (e.status != null) throw e;
      return h.ring[Math.floor(Math.random() * h.ring.length)];
    });
  }
  // A few tries: a move that came back malformed is asked again; a busy
  // Claude is waited on; a refused key stops at once.
  function tries(n, fn) {
    return fn().catch(function (e) {
      if (n <= 1 || e.status === 401 || e.status === 403 || (e.status === 400 && /credit/.test(e.message))) throw e;
      return new Promise(function (r) { setTimeout(r, e.status ? 2000 : 0); }).then(function () { return tries(n - 1, fn); });
    });
  }

  // ---- who I am, and the table I'm at ---------------------------------------------------
  var S = jget(TABLE);           // {code, start, name, three, seat, host, secrets, done}
  function saveS() { jset(TABLE, S); }
  function forgetTable() { S = null; jset(TABLE, null); T = null; }
  var me = jget(ME) || { name: '', three: '' };
  function cleanName(v) { return String(v || '').replace(/\s+/g, ' ').trim().slice(0, 32); }
  function nameProblem(n) {
    if (!n) return 'Your name, please.';
    if (!/^[\p{L}\p{N}][\p{L}\p{N} '’.\-]*$/u.test(n)) return 'Letters and numbers for your name, please.';
    if (Z.PERSONAS.some(function (p) { return p.toLowerCase() === n.toLowerCase(); }) || /^computer/i.test(n)) return 'That name belongs to a computer player. Choose another.';
    return '';
  }

  // ---- the lobby -------------------------------------------------------------------------
  var mode = '', places = [], chosen = '', moreShown = false, deeper = '', computers = 8, lobbyTables = {};
  (function () { var c = Number(sget(CP)); if (sget(CP) != null && c >= 0 && c <= 8) computers = c; })();
  function lobby(asHost, beneath) {
    show('lobby');
    deeper = beneath || '';
    $('my-name').value = me.name || '';
    $('my-three').value = me.three || '';
    $('lobby-error').textContent = '';
    setMode(asHost === true ? 'host' : asHost === false ? 'join' : '');
  }
  function setMode(m) {
    mode = m;
    $('door-host').setAttribute('aria-pressed', String(m === 'host'));
    $('door-join').setAttribute('aria-pressed', String(m === 'join'));
    $('lobby-form').hidden = !m;
    $('host-part').hidden = m !== 'host';
    $('join-part').hidden = m !== 'join';
    $('lobby-error').textContent = '';
    if (m === 'host') { drawComputers(); loadPlaces(); }
  }
  function drawComputers() {
    $('cp-count').textContent = String(computers);
    $('cp-less').disabled = computers <= 0;
    $('cp-more').disabled = computers >= 8;
    $('cp-line').textContent = computers ? 'They fill empty seats, using your key on this device.' : 'Only people. No key needed.';
    var kept = !!sget(KEY);
    $('key-part').hidden = !computers || kept;
    $('key-kept').hidden = !computers || !kept;
  }
  function loadPlaces() {
    $('places').textContent = '';
    $('places').appendChild(el('p', 'soft it', 'Reading the tree…'));
    Promise.all([readTree(), readRoom('').catch(function () { return []; })]).then(function (r) {
      TREE_JSON = r[0];
      lobbyTables = liveTables(r[1]);
      var taken = claimed(lobbyTables);
      places = Z.openPlaces(r[0]).filter(function (p) { return !taken[p.digits]; });
      if (deeper) places = places.filter(function (p) { return p.digits.length === deeper.length + 1 && p.digits.indexOf(deeper) === 0; });
      if (chosen && chosen !== 'new' && !places.some(function (p) { return p.digits === chosen; })) chosen = '';
      if (!chosen) chosen = deeper && places.length ? places[0].digits : 'new';
      drawPlaces();
    }, function () {
      $('places').textContent = '';
      $('places').appendChild(el('p', 'error', 'The tree is out of reach just now. Try again in a moment.'));
    });
  }
  function drawPlaces() {
    var box = $('places');
    box.textContent = '';
    var rows = [{ digits: 'new' }].concat(places);
    if (deeper) rows = places.slice();
    var limit = moreShown ? rows.length : 7;
    rows.slice(0, limit).forEach(function (p) {
      var b = el('button');
      b.type = 'button';
      b.setAttribute('aria-pressed', String(chosen === p.digits));
      if (p.digits === 'new') b.appendChild(el('span', 'pw', 'A new first word'));
      else {
        b.appendChild(document.createTextNode('beneath '));
        b.appendChild(el('span', 'pw', p.word));
        b.appendChild(document.createTextNode(' · nine seats open'));
      }
      b.addEventListener('click', function () { chosen = p.digits; drawPlaces(); });
      box.appendChild(b);
    });
    if (deeper && !places.length) box.appendChild(el('p', 'soft it', 'Every word beneath the last place is taken. Choose from the whole tree.'));
    $('places-more').hidden = !(rows.length > limit) && !deeper;
    $('places-more').textContent = deeper ? 'Anywhere in the tree' : 'More places';
    $('first-part').hidden = chosen !== 'new';
  }
  var CODES = ['LAMP', 'MOTH', 'REED', 'TIDE', 'SALT', 'FERN', 'PIER', 'KITE', 'BELL', 'WREN', 'DUSK', 'HARE', 'ROOK', 'SLOE', 'MOSS', 'PEAR', 'DOVE', 'RAIN', 'LARK', 'OAR'];
  function newCode(tables) {
    for (var i = 0; i < 50; i++) {
      var c = CODES[Math.floor(Math.random() * CODES.length)] + ' ' + (1 + Math.floor(Math.random() * 9));
      var t = tables[Z.codeKey(c)];
      if (!t) return c;
    }
    return CODES[0] + ' ' + Date.now() % 1000;
  }
  function shuffle(a) {
    a = a.slice();
    for (var i = a.length - 1; i > 0; i--) { var j = Math.floor(Math.random() * (i + 1)); var x = a[i]; a[i] = a[j]; a[j] = x; }
    return a;
  }
  function lobbyError(t) { $('lobby-error').textContent = t || ''; }
  function readMe() {
    var name = cleanName($('my-name').value), three = String($('my-three').value || '').replace(/\s+/g, ' ').trim().slice(0, 60);
    var p = nameProblem(name);
    if (p) { lobbyError(p); $('my-name').focus(); return null; }
    if (!three) { lobbyError('Three words about you, please.'); $('my-three').focus(); return null; }
    me = { name: name, three: three };
    jset(ME, me);
    return me;
  }
  function openTable() {
    var who = readMe();
    if (!who) return;
    if (computers) {
      var typed = String($('my-key').value || '').trim();
      if (typed) { sset(KEY, typed); $('my-key').value = ''; }
      if (!sget(KEY)) { lobbyError('The computer players need your Anthropic key. Or choose 0 for people only.'); $('my-key').focus(); return; }
    }
    var first = '';
    if (chosen === 'new') {
      var w = Z.oneWord($('first-word').value);
      if (!w.ok) { lobbyError(w.why === 'One word, no spaces' ? 'One word to begin, no spaces.' : w.why); $('first-word').focus(); return; }
      first = w.word;
    } else if (!chosen) { lobbyError('Choose where to play.'); return; }
    sset(CP, String(computers));
    $('open-go').disabled = true;
    lobbyError('');
    Promise.all([readTree(), readRoom('').catch(function () { return []; })]).then(function (r) {
      TREE_JSON = r[0];
      var tables = liveTables(r[1]), taken = claimed(tables), place, word;
      if (chosen === 'new') {
        var free = Z.freeTops(r[0]).filter(function (d) { return !taken[d]; });
        if (!free.length) throw new Error('The top of the tree is full: every first word is taken. Choose a place beneath one.');
        place = free[0]; word = first;
      } else {
        var node = Z.nodeAt(r[0], chosen);
        if (node == null || Z.played(node) || taken[chosen]) throw new Error('That place was taken a moment ago. Choose another.');
        place = chosen; word = Z.readPlace(node).word;
      }
      var code = newCode(tables);
      var personas = shuffle(Z.PERSONAS).slice(0, computers);
      return post('open', {
        who: who.name, at: Z.at(place), code: code,
        text: code + ' · ' + who.name + ' opens a table at ' + word + ' (' + (9 - computers) + ' seats for people, ' + computers + ' computer)',
        data: { _: word, 1: who.name, 2: who.three, 3: String(computers), 4: personas.join(', '), 5: chosen === 'new' ? 'new' : '' }
      }).then(function (res) {
        if (!res.ok) throw new Error(refusal(res));
        return readRoom('').then(function (list) {
          var mine = list.filter(function (e) { return e.kind === 'open' && Z.codeKey(e.code) === Z.codeKey(code); })[0];
          var start = (res.json && res.json.slot) || (mine && mine.addr);
          if (!start) throw new Error('The table opened, but the room can’t find it. Try again.');
          // Two hosts at one place in the same moment: the earlier open keeps it.
          var t2 = liveTables(list), rival = null;
          Object.keys(t2).forEach(function (k) {
            var t = t2[k];
            if (k === Z.codeKey(code) || !t.live || !t.places[place]) return;
            if (Z.byAddr(t.open, { addr: start }) < 0) rival = t;
          });
          if (rival) {
            post('close', { who: who.name, at: Z.at(place), code: code, text: code + ' · closed: the place was taken a moment before' });
            throw new Error('That place was taken a moment ago. Choose another.');
          }
          S = { code: code, start: start, name: who.name, three: who.three, seat: 1, host: true, secrets: {}, done: {} };
          saveS();
          enterTable();
        });
      });
    }).catch(function (e) {
      lobbyError(e.message || 'That didn’t work. Try again.');
      loadPlaces();
    }).then(function () { $('open-go').disabled = false; });
  }
  function joinTable() {
    var who = readMe();
    if (!who) return;
    var key = Z.codeKey($('join-code').value);
    if (!/^[A-Z]+[0-9]+$/.test(key)) { lobbyError('The table code is a word and a number, like LAMP 4.'); $('join-code').focus(); return; }
    $('join-go').disabled = true;
    lobbyError('');
    readRoom('').then(function (list) {
      var open = list.filter(function (e) { return e.kind === 'open' && Z.codeKey(e.code) === key; })[0];
      if (!open) throw new Error('No table with that code is open. Check it with the host.');
      return readRoom(open.addr).then(function (entries) { return Z.derive(entries, open.code); }).then(function (t) {
        if (t.closed) throw new Error('That table has closed.');
        var mine = function () {
          var seats = t.seats, found = 0;
          Object.keys(seats).forEach(function (s) { if (seats[s].kind === 'person' && seats[s].name.toLowerCase() === who.name.toLowerCase()) found = Number(s); });
          return found;
        };
        if (t.roster) {
          if (mine()) return { t: t, seat: mine(), open: open };
          throw new Error('That table has already started.');
        }
        if (mine()) return { t: t, seat: mine(), open: open };
        var free = 9 - t.open.computers - 1 - t.sits.length;
        if (free <= 0) throw new Error('That table is full.');
        return post('sit', {
          who: who.name, at: Z.at(t.open.place), code: open.code, text: open.code + ' · ' + who.name + ' takes a seat', data: { _: who.three }
        }).then(function (res) {
          if (!res.ok) throw new Error(refusal(res));
          return readRoom(open.addr).then(function (entries) { return Z.derive(entries, open.code); }).then(function (t3) {
            t = t3;
            if (!mine()) throw new Error('Someone took the last seat a moment before you.');
            return { t: t, seat: mine(), open: open };
          });
        });
      });
    }).then(function (r) {
      // The seat's own spelling of the name, so every seal is read as this seat's.
      S = { code: r.open.code, start: r.open.addr, name: r.t.seats[r.seat].name, three: who.three, seat: r.seat, host: false, secrets: {}, done: {} };
      saveS();
      enterTable();
    }, function (e) {
      lobbyError(e.message || 'That didn’t work. Try again.');
    }).then(function () { $('join-go').disabled = false; });
  }

  // ---- at the table ------------------------------------------------------------------------
  var T = null, TREE_JSON = null, treeHand = 0, timer = null, polling = false, running = {}, firstSeen = {}, flipped = {}, voteChoice = 0;
  var lastOk = 0, failing = false, inked = {};
  function enterTable() {
    T = null; treeHand = -1; flipped = {}; firstSeen = {}; voteChoice = 0;
    readLaw();
    poll();
  }
  function schedule(ms) {
    clearTimeout(timer);
    timer = setTimeout(poll, ms != null ? ms : (document.hidden ? QUIET_MS : POLL_MS));
  }
  function poll() {
    if (!S || polling) return schedule();
    polling = true;
    readRoom(S.start).then(function (list) {
      return Z.derive(list, S.code);
    }).then(function (t) {
      T = t;
      lastOk = Date.now(); failing = false;
      var n = t.current ? t.current.n : 0;
      if (n !== treeHand || !TREE_JSON) {
        return readTree().then(function (tree) { TREE_JSON = tree; treeHand = n; }, function () {});
      }
    }).then(function () {
      if (T) { draw(); duties(); }
    }, function () {
      failing = true;
      if (T) draw();
    }).then(function () {
      polling = false;
      schedule();
    });
  }
  function soon() { schedule(150); }

  function draw() {
    if (!S || !T) return;
    if (T.phase === 'none') { $('t-status').textContent = 'Finding the table…'; return; }
    if (T.phase === 'closed') { forgetTable(); lobby(); lobbyError('That table has closed.'); return; }
    if (T.phase === 'waiting') return drawWaiting();
    if (T.phase === 'end' && S.ended) return drawEnd();
    drawTable();
  }

  // Seated, before the deal (spine:zero 4.2.5: "You're seated. The game starts when the host deals.")
  function drawWaiting() {
    if ($('waiting').hidden) show('waiting');
    var t = T, o = t.open, people = 9 - o.computers;
    $('w-place').textContent = o.isNew ? 'a new first word' : 'beneath ' + o.word;
    $('w-code').textContent = t.code;
    $('w-count').textContent = people + (people === 1 ? ' seat' : ' seats') + ' for people · ' + o.computers + ' computer';
    var ul = $('w-roster');
    ul.textContent = '';
    var seats = t.seats, filled = 0;
    for (var s = 1; s <= 9; s++) {
      var x = seats[s];
      var li = el('li', x ? (s === S.seat ? 'me' : '') : 'empty', x ? x.name : 'open');
      if (x && x.kind === 'person') filled++;
      ul.appendChild(li);
    }
    var missing = people - filled, hasKey = !!sget(KEY);
    var deal = $('deal-go');
    deal.hidden = !S.host;
    if (S.host) {
      deal.disabled = missing > 0 && !hasKey;
      deal.textContent = missing > 0 && hasKey ? 'Deal now (computer players take the empty seats)' : 'Deal';
      $('w-say').textContent = missing > 0 ? 'Waiting for ' + missing + ' more ' + (missing === 1 ? 'person' : 'people') + '. Show them the code.' : 'Everyone is here.';
    } else {
      $('w-say').textContent = 'You’re seated. The game starts when the host deals.';
    }
    $('w-leave').textContent = S.host ? 'Close this table' : 'Leave this table';
  }
  function dealFirst() {
    var t = T, o = t.open, seats = t.seats, used = {};
    var hasKey = !!sget(KEY);
    Object.keys(seats).forEach(function (s) { used[seats[s].name] = true; });
    var spare = shuffle(Z.PERSONAS.filter(function (p) { return !used[p]; }));
    var roster = { _: o.word };
    for (var s = 1; s <= 9; s++) {
      var x = seats[s];
      if (!x) {
        if (!hasKey) { $('w-error').textContent = 'Every seat needs someone before the deal.'; return; }
        x = { name: spare.shift(), kind: 'computer', three: '' };
      }
      roster[String(s)] = { _: x.name, 1: x.kind };
      if (x.three) roster[String(s)]['2'] = x.three;
    }
    $('deal-go').disabled = true;
    $('w-error').textContent = '';
    readTree().then(function (tree) {
      TREE_JSON = tree;
      var node = Z.nodeAt(tree, o.place);
      if (o.isNew) {
        if (node != null) throw new Error('Someone has just written a first word in that seat. Close this table and open another.');
        return write(TREE, { spindle: Z.at(o.place), content: o.word + ' · ' + S.name }).then(function (r) { if (!r.ok) throw new Error(refusal(r)); });
      }
      if (node == null || Z.played(node)) throw new Error('This place has been played since the table opened. Close this table and open another.');
    }).then(function () {
      return post('deal', { who: S.name, at: Z.at(o.place), code: t.code, hand: 1, text: t.code + ' · hand one at ' + o.word, data: roster });
    }).then(function (r) {
      if (r && !r.ok) throw new Error(refusal(r));
      soon();
    }).catch(function (e) {
      $('w-error').textContent = e.message || 'The deal didn’t reach the beach. Try again.';
      $('deal-go').disabled = false;
    });
  }
  function leave() {
    if (S && S.host && T && !T.over) {
      post('close', { who: S.name, at: T.open ? Z.at(T.open.place) : '', code: S.code, text: S.code + ' · the host closed the table' });
    }
    forgetTable();
    clearTimeout(timer);
    lobby();
  }

  // ---- the table, drawn ----------------------------------------------------------------------
  function stageWord(t) {
    var h = t.current;
    if (!h) return '';
    if (t.over) return 'the voice';
    if (needsTurn(h)) return 'the flop';
    return { word: 'seats', line: 'the turn', vote: 'the showdown', voice: 'the voice', done: 'the voice' }[h.stage] || '';
  }
  function needsTurn(h) {
    return h.steps.word.shown && !flipped[h.n] && (h.stage === 'line' || (h.stage === 'done' && h.noRing));
  }
  function wordsShown(h) { return h.steps.word.shown && !needsTurn(h); }
  function mySeats() {
    var out = [S.seat];
    if (S.host && T && T.roster) Object.keys(T.roster).forEach(function (s) { if (T.roster[s].kind === 'computer') out.push(Number(s)); });
    return out;
  }
  function computersWaiting(st) {
    return st.need.filter(function (s) { return Z.isComputer(T, s) && !st.seals[s] && !st.skipSeal[s]; });
  }
  function names(list) {
    var n = list.map(function (s) { return Z.nameOf(T, s); });
    return n.length <= 1 ? n.join('') : n.slice(0, -1).join(', ') + ' and ' + n[n.length - 1];
  }

  function drawTable() {
    if ($('table').hidden) show('table');
    var t = T, h = t.current;
    $('t-phase').textContent = h ? 'Hand ' + HAND_WORDS[h.n] + ' · ' + stageWord(t) : '';
    // The trail: the words above this place in the tree, then the zero.
    var trail = $('t-trail');
    trail.textContent = '';
    var words = trailWords(h).map(cap);
    words.forEach(function (w, i) {
      if (i) trail.appendChild(document.createTextNode(' › '));
      trail.appendChild(i === words.length - 1 ? el('b', null, w) : document.createTextNode(w));
    });
    if (!t.over) trail.appendChild(document.createTextNode(' › …'));
    drawRing(t, h);
    drawMoves(t, h);
    $('t-status').textContent = failing ? 'The beach is out of reach just now. Trying again…' :
      'Live from the beach · checked ' + new Date(lastOk).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  function drawRing(t, h) {
    var ring = $('t-ring');
    Array.prototype.slice.call(ring.querySelectorAll('.seat')).forEach(function (n) { n.remove(); });
    var st = Z.stepFor(h), shown = wordsShown(h), turning = flipped[h.n] === 'now';
    for (var s = 1; s <= 9; s++) {
      var a = (-90 + (s - 1) * 40) * Math.PI / 180;
      var seat = el('div', 'seat');
      seat.style.left = (50 + 39 * Math.cos(a)) + '%';
      seat.style.top = (50 + 39 * Math.sin(a)) + '%';
      var card = el('div', 'card');
      var x = t.seats[s];
      if (!x) { seat.classList.add('empty'); }
      if (s === S.seat) seat.classList.add('me');
      var inHand = h.active.indexOf(s) >= 0;
      if (!inHand) seat.classList.add('out');
      if (shown && inHand) {
        var w = h.words[s];
        var wd = el('span', 'w', w || '—');
        if (w && w.length > 9) wd.style.fontSize = '0.72rem';
        card.appendChild(wd);
        if (h.out[s]) seat.classList.add('struck', 'out');
        if (turning) seat.classList.add('turning');
      } else if (h.stage === 'word' || needsTurn(h)) {
        var wst = h.steps.word;
        if (wst.seals[s]) card.appendChild(el('span', 'sealed', 'sealed'));
        else if (wst.skipSeal[s]) card.appendChild(el('span', 'w soft', '—'));
      }
      if (st && st.name !== 'word' && st.seals[s] && !needsTurn(h)) card.style.boxShadow = 'inset 0 -2px 0 var(--crimson)';
      if (h.winners.indexOf(s) >= 0 && (h.stage === 'voice' || h.stage === 'done')) seat.classList.add('win');
      if (t.roster) card.appendChild(el('span', 'chips', String(t.chips[s] != null ? t.chips[s] : '')));
      seat.appendChild(card);
      seat.appendChild(el('span', 'who', x ? x.name : 'open'));
      ring.appendChild(seat);
    }
    if (turning) setTimeout(function () { if (flipped[h.n] === 'now') { flipped[h.n] = true; } }, 800);
    var z = $('t-zero');
    z.textContent = h.zero;
    z.style.fontSize = h.zero.length > 11 ? '1.25rem' : h.zero.length > 8 ? '1.55rem' : '';
    var voice = $('t-voice'), pot = $('t-pot');
    voice.textContent = '';
    voice.classList.remove('inked');
    pot.textContent = '';
    if (h.winners.length && (h.stage === 'voice' || h.stage === 'done')) {
      voice.textContent = h.winners.map(function (w) { return h.lines[w].line; }).join(' / ');
      if (!inked[h.n]) { inked[h.n] = true; voice.classList.add('inked'); }
      pot.textContent = h.winners.length === 1 ? Z.nameOf(t, h.winners[0]) + ' takes ' + h.pot : 'the pot of ' + h.pot + ' is split';
    } else if (h.stage === 'vote') {
      pot.textContent = 'pot ' + h.pot;
    }
  }

  function drawMoves(t, h) {
    var box = $('t-moves');
    var keep = document.activeElement && box.contains(document.activeElement) ? document.activeElement.id : '';
    var sig = movesSig(t, h);
    if (box.dataset.sig === sig) { drawGoOn(t, h); return; }
    // Keep what someone is typing when the panel is redrawn.
    var typed = { word: ($('m-word') || {}).value, line: ($('m-line') || {}).value, stake: ($('m-stake') || {}).value };
    box.dataset.sig = sig;
    box.textContent = '';
    var st = Z.stepFor(h), mine = S.seat;
    if (h.stage === 'word') {
      if (h.active.indexOf(mine) < 0) {
        box.appendChild(el('p', 'note', 'You have no chips left, so you write no more words. You keep your vote.'));
      } else if (computersWaiting(st).length) {
        box.appendChild(el('p', 'note', 'The computer players are writing first…'));
      } else if (!st.seals[mine] && !st.skipSeal[mine]) {
        var ask = el('p', 'ask');
        ask.appendChild(document.createTextNode('One word that belongs under '));
        ask.appendChild(el('em', null, h.zero));
        ask.appendChild(document.createTextNode('. Nobody else should think of it.'));
        box.appendChild(ask);
        var f = el('form');
        f.noValidate = true;
        var inp = el('input');
        inp.type = 'text'; inp.id = 'm-word'; inp.maxLength = 32; inp.autocomplete = 'off'; inp.spellcheck = false;
        inp.setAttribute('autocapitalize', 'off');
        inp.setAttribute('aria-label', 'Your word');
        if (typed.word) inp.value = typed.word;
        var b = el('button', null, 'Seal it');
        b.type = 'submit';
        var err = el('p', 'error');
        f.appendChild(inp); f.appendChild(b); f.appendChild(err);
        f.addEventListener('submit', function (ev) {
          ev.preventDefault();
          var w = Z.oneWord(inp.value);
          if (!w.ok) { err.textContent = w.why; inp.focus(); return; }
          if (Z.same(w.word, h.zero)) { err.textContent = 'A word under ' + h.zero + ', not ' + h.zero + ' itself.'; return; }
          b.disabled = true;
          sealMine(h, 'word', w.word).catch(function (e) { err.textContent = e.message; b.disabled = false; });
        });
        box.appendChild(f);
      } else {
        box.appendChild(el('p', 'note', waitingLine(st)));
      }
    } else if (needsTurn(h)) {
      box.appendChild(el('p', 'ask', h.noRing ? 'Every word is in.' : 'All the words are in.'));
      var tb = el('button', null, 'Turn the cards');
      tb.type = 'button';
      tb.addEventListener('click', function () { flipped[h.n] = 'now'; drawTable(); });
      box.appendChild(tb);
    } else if (h.stage === 'line') {
      box.appendChild(ringWords(h));
      if (h.ring.indexOf(mine) < 0) {
        box.appendChild(el('p', 'note', h.active.indexOf(mine) < 0 ? 'You have no chips left. You keep your vote.' : 'Your word was matched, so you sit out this hand. You still vote.'));
        if (st.need.length) box.appendChild(el('p', 'note', waitingLine(st)));
      } else if (computersWaiting(st).length) {
        box.appendChild(el('p', 'note', 'The computer players are writing their lines first…'));
      } else if (!st.seals[mine] && !st.skipSeal[mine]) {
        var chips = h.chipsBefore[mine] || 0;
        var lf = el('form');
        lf.noValidate = true;
        var lab = el('label', null, 'One line that holds the ring');
        lab.htmlFor = 'm-line';
        var ta = el('textarea');
        ta.id = 'm-line'; ta.rows = 2; ta.maxLength = 160;
        if (typed.line) ta.value = typed.line;
        var row = el('div', 'stake');
        var sl = el('label', null, 'Stake');
        sl.htmlFor = 'm-stake';
        var rng = el('input');
        rng.type = 'range'; rng.id = 'm-stake'; rng.min = '0'; rng.max = String(chips); rng.step = '1';
        rng.value = typed.stake != null && typed.stake !== '' ? typed.stake : String(Math.min(3, chips));
        var out = el('output', null, '');
        var say = function () { out.textContent = rng.value + ' of ' + chips; };
        rng.addEventListener('input', say); say();
        row.appendChild(sl); row.appendChild(rng); row.appendChild(out);
        var lb = el('button', null, 'Seal line and stake');
        lb.type = 'submit';
        var lerr = el('p', 'error');
        lf.appendChild(lab); lf.appendChild(ta); lf.appendChild(row); lf.appendChild(lb); lf.appendChild(lerr);
        lf.addEventListener('submit', function (ev) {
          ev.preventDefault();
          var l = Z.oneLine(ta.value);
          if (!l.ok) { lerr.textContent = l.why; ta.focus(); return; }
          lb.disabled = true;
          sealMine(h, 'line', l.line, Number(rng.value)).catch(function (e) { lerr.textContent = e.message; lb.disabled = false; });
        });
        box.appendChild(lf);
      } else {
        box.appendChild(el('p', 'note', waitingLine(st)));
      }
    } else if (h.stage === 'vote') {
      var canVote = st.need.indexOf(mine) >= 0 && !st.seals[mine] && !st.skipSeal[mine] && !computersWaiting(st).length;
      box.appendChild(el('p', 'ask', canVote ? 'Tap the line that holds the ring best.' : 'The lines'));
      Object.keys(h.lines).map(Number).sort(function (a, b) { return a - b; }).forEach(function (s) {
        var b = el('button', 'slip');
        b.type = 'button';
        b.appendChild(el('span', 'l', h.lines[s].line));
        b.appendChild(el('span', 'by', Z.nameOf(t, s) + ' · staked ' + h.lines[s].stake));
        b.disabled = s === mine || !canVote;
        b.setAttribute('aria-pressed', String(voteChoice === s));
        b.addEventListener('click', function () { voteChoice = s; box.dataset.sig = ''; drawMoves(t, h); });
        box.appendChild(b);
      });
      if (canVote) {
        var vb = el('button', null, voteChoice && h.lines[voteChoice] && voteChoice !== mine ? 'Seal vote for ' + Z.nameOf(t, voteChoice) : 'Seal vote');
        vb.type = 'button';
        vb.disabled = !(voteChoice && h.lines[voteChoice] && voteChoice !== mine);
        var verr = el('p', 'error');
        vb.addEventListener('click', function () {
          vb.disabled = true;
          sealMine(h, 'vote', String(voteChoice)).catch(function (e) { verr.textContent = e.message; vb.disabled = false; });
        });
        box.appendChild(vb);
        box.appendChild(verr);
      } else if (computersWaiting(st).length) {
        box.appendChild(el('p', 'note', 'The computer players are voting first…'));
      } else if (st.need.indexOf(mine) < 0) {
        box.appendChild(el('p', 'note', 'There is no line for you to vote for but your own.'));
        box.appendChild(el('p', 'note', waitingLine(st)));
      } else {
        box.appendChild(el('p', 'note', waitingLine(st)));
      }
    } else {
      drawVoice(box, t, h);
    }
    drawGoOn(t, h);
    if (keep && $(keep)) $(keep).focus();
  }
  function movesSig(t, h) {
    var st = Z.stepFor(h);
    return [h.n, h.stage, needsTurn(h), st ? Object.keys(st.seals).join(',') + '/' + Object.keys(st.shows).join(',') + '/' + Object.keys(st.skipSeal).join(',') : '',
      h.pick, t.over, voteChoice, S.ended ? 1 : 0].join('|');
  }
  function ringWords(h) {
    var p = el('p', 'ringwords');
    p.setAttribute('aria-label', 'The ring');
    h.ring.forEach(function (s) { p.appendChild(el('span', null, h.words[s])); });
    return p;
  }
  function waitingLine(st) {
    var a = Z.waitingSeal(st), b = Z.waitingShow(st);
    if (a.length) return 'Sealed. Waiting for ' + names(a) + '.';
    if (b.length) return 'Turning over ' + names(b) + '’s ' + (b.length === 1 ? 'move' : 'moves') + '…';
    return 'Everyone has sealed. Turning the cards…';
  }
  function drawVoice(box, t, h) {
    if (h.noRing) {
      box.appendChild(el('p', 'ask', 'Every word was matched, so there is no ring this hand.'));
      box.appendChild(el('p', 'note', t.over ? 'That was the last hand.' : 'The next hand is dealt again at ' + h.zero + '.'));
    } else {
      var ul = el('ul', 'tally');
      Object.keys(h.lines).map(Number).sort(function (a, b) { return (h.tally[b] - h.tally[a]) || (a - b); }).forEach(function (s) {
        var li = el('li', h.winners.indexOf(s) >= 0 ? 'won' : '');
        li.appendChild(el('span', null, Z.nameOf(t, s)));
        li.appendChild(el('span', null, (h.tally[s] || 0) + (h.tally[s] === 1 ? ' vote' : ' votes')));
        ul.appendChild(li);
      });
      box.appendChild(ul);
      box.appendChild(el('p', 'note', h.winners.length === 1
        ? Z.nameOf(t, h.winners[0]) + ' takes the pot of ' + h.pot + '.'
        : 'Tied: ' + names(h.winners) + ' split the pot of ' + h.pot + '.'));
      box.appendChild(el('p', 'poem-head', 'the poem so far'));
      box.appendChild(poemSheet(t, false));
    }
    if (t.over) {
      var e = el('button', null, 'The poem');
      e.type = 'button';
      e.addEventListener('click', function () { S.ended = true; saveS(); drawEnd(); });
      box.appendChild(e);
      return;
    }
    if (h.stage === 'voice') {
      var picker = h.picker;
      if (picker === S.seat) {
        box.appendChild(el('p', 'ask', 'Your line won. Pick the next zero, one level down.'));
        var row = el('div', 'picks');
        h.ring.forEach(function (s) {
          var b = el('button', null, h.words[s]);
          b.type = 'button';
          b.addEventListener('click', function () {
            Array.prototype.forEach.call(row.querySelectorAll('button'), function (x) { x.disabled = true; });
            post('pick', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · ' + S.name + ' picks ' + h.words[s] + ' as the next zero', data: { _: String(s) } })
              .then(function (r) { if (!r.ok) throw new Error(refusal(r)); soon(); })
              .catch(function (er) { $('t-error').textContent = er.message; Array.prototype.forEach.call(row.querySelectorAll('button'), function (x) { x.disabled = false; }); });
          });
          row.appendChild(b);
        });
        box.appendChild(row);
      } else {
        box.appendChild(el('p', 'note', Z.nameOf(t, picker) + ' is choosing the next zero…'));
        var pk = 'pickwait:' + h.n;
        if (!firstSeen[pk]) firstSeen[pk] = Date.now();
        if (S.host && !Z.isComputer(t, picker) && Date.now() - firstSeen[pk] >= WAIT_BEFORE_GO_ON) {
          var row2 = el('div', 'picks');
          row2.appendChild(el('p', 'note', 'Choose for ' + Z.nameOf(t, picker) + ':'));
          h.ring.forEach(function (s) {
            var b2 = el('button', 'quiet', h.words[s]);
            b2.type = 'button';
            b2.addEventListener('click', function () {
              b2.disabled = true;
              post('pick', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · the host picks ' + h.words[s] + ' for ' + Z.nameOf(t, picker), data: { _: String(s) } }).then(function () { soon(); });
            });
            row2.appendChild(b2);
          });
          box.appendChild(row2);
        }
      }
    } else if (h.pick) {
      box.appendChild(el('p', 'note', Z.nameOf(t, h.picker) + ' picked ' + h.words[h.pick] + '. Dealing the next hand…'));
    } else if (h.noRing) {
      box.appendChild(el('p', 'note', 'Dealing…'));
    }
  }
  // "Go on without them": for the host, after two minutes' wait on people.
  function drawGoOn(t, h) {
    var old = $('m-goon');
    if (old) old.remove();
    if (!S.host) return;
    var st = Z.stepFor(h);
    if (!st) return;
    var sealWait = Z.waitingSeal(st).filter(function (s) { return !Z.isComputer(t, s); });
    var showWait = Z.waitingShow(st).filter(function (s) { return !Z.isComputer(t, s) && s !== S.seat; });
    var phase = sealWait.length ? 'seal' : showWait.length ? 'show' : '';
    if (!phase) return;
    var key = h.n + st.name + phase;
    if (!firstSeen[key]) firstSeen[key] = Date.now();
    if (Date.now() - firstSeen[key] < WAIT_BEFORE_GO_ON) return;
    var who = phase === 'seal' ? sealWait : showWait;
    var b = el('button', 'quiet', 'Go on without ' + names(who));
    b.id = 'm-goon';
    b.type = 'button';
    b.addEventListener('click', function () {
      b.disabled = true;
      post('go', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · the table goes on without ' + names(who), data: { _: st.name, 1: phase, 2: who.join(',') } })
        .then(function () { soon(); });
    });
    $('t-moves').appendChild(b);
  }

  // ---- moves: seal, then show ---------------------------------------------------------------
  function secretKey(h, step, seat) { return 'h' + h.n + ':' + step + ':' + seat; }
  function keep(h, step, seat, move, stake) {
    var s = Z.salt();
    S.secrets[secretKey(h, step, seat)] = { move: String(move), stake: stake, salt: s };
    saveS();
    return Z.seal(S.code, h.n, step, seat, String(move), stake, s);
  }
  function sealMine(h, step, move, stake) {
    return keep(h, step, S.seat, move, stake).then(function (hash) {
      var data = { _: step };
      data[String(S.seat)] = hash;
      return post('seal', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · ' + S.name + ' seals ' + ({ word: 'a word', line: 'a line and a stake', vote: 'a vote' })[step], data: data });
    }).then(function (r) {
      if (!r.ok) throw new Error(refusal(r));
      voteChoice = 0;
      soon();
    });
  }
  // Show every move this device sealed once the step's seals are all in.
  function showMine(h, st) {
    var data = { _: st.name }, any = false, said = [];
    mySeats().forEach(function (s) {
      if (!st.seals[s] || st.shows[s] || st.skipShow[s]) return;
      var x = S.secrets[secretKey(h, st.name, s)];
      if (!x) return;
      var m = { _: x.move, 1: x.salt };
      if (st.name === 'line') m['2'] = String(x.stake || 0);
      data[String(s)] = m;
      any = true;
      said.push(Z.nameOf(T, s) + ': ' + (st.name === 'vote' ? 'votes ' + Z.nameOf(T, Number(x.move)) + (x.why ? ' (' + x.why + ')' : '') : st.name === 'line' ? x.move + ' (stake ' + (x.stake || 0) + ')' : x.move));
    });
    if (!any) return Promise.resolve();
    return post('show', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · ' + ({ word: 'words', line: 'lines', vote: 'votes' })[st.name] + ' at ' + h.zero + ': ' + said.join(' | '), data: data });
  }

  // ---- what this device does on each read ------------------------------------------------
  var keyBlocked = false;     // Claude refused the key: wait for another
  function duty(key, fn) {
    if (running[key]) return;
    running[key] = true;
    Promise.resolve().then(fn).then(function () {
      delete running[key];
      if (!keyBlocked) $('t-error').textContent = '';
      soon();
    }, function (e) {
      $('t-error').textContent = (e && e.message) || 'Something went wrong. Trying again.';
      if (e && (e.status === 401 || e.status === 403)) { keyBlocked = true; drawKeyBox(); }
      setTimeout(function () { delete running[key]; }, e && e.status === 0 ? 4000 : 6000);
    });
  }
  function duties() {
    var t = T, h = t.current;
    if (!h || t.phase === 'end' && S.ended) return hostEnd(t, h);
    var st = Z.stepFor(h);
    if (st && st.sealed && !st.shown) duty('show:' + h.n + st.name, function () { return showMine(h, st); });
    if (!S.host) return;
    var key = sget(KEY);
    if (st && !st.closed) {
      var comp = computersWaiting(st);
      if (comp.length) {
        if (keyBlocked) return hostEnd(t, h);
        if (!key) { $('t-error').textContent = 'The computer players need your Anthropic key on this device.'; keyBlocked = true; drawKeyBox(); return; }
        duty('cmp:' + h.n + st.name, function () { return computerMoves(key, t, h, st, comp); });
      }
    }
    hostEnd(t, h);
  }
  function hostEnd(t, h) {
    if (!S.host || !h) return;
    var resolved = h.winners.length && (h.stage === 'voice' || h.stage === 'done');
    var treeKey = 'tree:' + h.n;
    if (resolved && !S.done[treeKey]) duty(treeKey, function () { return writePlace(t, h); });
    var treeOk = !resolved || S.done[treeKey];
    if (h.stage === 'voice' && Z.isComputer(t, h.picker) && treeOk && !keyBlocked) {
      var key = sget(KEY);
      if (key) duty('pick:' + h.n, function () {
        return askPick(key, t, h, h.picker).then(function (s) {
          return post('pick', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · ' + Z.nameOf(t, h.picker) + ' picks ' + h.words[s] + ' as the next zero', data: { _: String(s) } });
        });
      });
    }
    if (h.stage === 'done' && !t.over && treeOk) {
      var k = 'deal:' + (h.n + 1);
      if (!firstSeen[k]) firstSeen[k] = Date.now();
      if (Date.now() - firstSeen[k] >= PAUSE_BEFORE_DEAL) duty(k, function () { return dealNext(t, h); });
      else schedule(Math.min(POLL_MS, PAUSE_BEFORE_DEAL - (Date.now() - firstSeen[k]) + 50));
    }
    if (t.over && treeOk && !S.done.closed) duty('close', function () {
      return post('close', { who: S.name, at: Z.at(h.place), code: S.code, text: S.code + ' · game over: ' + finalLine(t) }).then(function (r) {
        if (r.ok) { S.done.closed = true; saveS(); }
      });
    });
  }
  function computerMoves(key, t, h, st, seats) {
    var ask = st.name === 'word' ? askWord : st.name === 'line' ? askLine : askVote;
    $('t-error').textContent = '';
    return Promise.all(seats.map(function (s) {
      var have = S.secrets[secretKey(h, st.name, s)];
      if (have) return Z.seal(S.code, h.n, st.name, s, have.move, have.stake, have.salt).then(function (hash) { return { s: s, hash: hash }; });
      return ask(key, t, h, s).then(function (mv) {
        return keep(h, st.name, s, mv.move, mv.stake).then(function (hash) {
          if (mv.why) { S.secrets[secretKey(h, st.name, s)].why = mv.why; saveS(); }
          return { s: s, hash: hash };
        });
      });
    })).then(function (sealed) {
      var data = { _: st.name };
      sealed.forEach(function (x) { data[String(x.s)] = x.hash; });
      return post('seal', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · the computer players seal ' + ({ word: 'their words', line: 'their lines and stakes', vote: 'their votes' })[st.name], data: data });
    }).then(function (r) { if (!r.ok) throw new Error(refusal(r)); });
  }
  // The played place goes into the tree: its voice, signed, and each
  // surviving word in its seat. Never over a seat or a voice already there.
  function writePlace(t, h) {
    var nameOf = function (s) { return Z.nameOf(t, s); };
    var content = Z.placeAfter(h, nameOf);
    return readTree().then(function (tree) {
      TREE_JSON = tree;
      var node = Z.nodeAt(tree, h.place);
      if (node != null && Z.played(node)) {
        if (Z.textOf(node) !== content._) $('t-error').textContent = 'This place already had a voice in the tree, so this hand stays in the room only.';
        return { ok: true };
      }
      return write(TREE, { spindle: Z.at(h.place), content: content });
    }).then(function (r) {
      if (!r.ok) throw new Error(refusal(r));
      S.done['tree:' + h.n] = true;
      saveS();
      return readTree().then(function (tree) { TREE_JSON = tree; }, function () {});
    });
  }
  function dealNext(t, h) {
    var place = Z.nextPlace(h);
    if (!place) return Promise.resolve();
    var word = h.noRing ? h.zero : h.words[h.pick];
    return post('deal', { who: S.name, at: Z.at(place), code: S.code, hand: h.n + 1, text: S.code + ' · hand ' + HAND_WORDS[h.n + 1] + ' at ' + word, data: { _: word } })
      .then(function (r) { if (!r.ok) throw new Error(refusal(r)); });
  }
  function drawKeyBox() {
    if ($('m-key')) return;
    var f = el('form');
    f.noValidate = true;
    var inp = el('input');
    inp.type = 'password'; inp.id = 'm-key'; inp.autocomplete = 'off';
    inp.setAttribute('aria-label', 'Your Anthropic key');
    var b = el('button', null, 'Use this key');
    b.type = 'submit';
    f.appendChild(inp); f.appendChild(b);
    f.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var k = inp.value.trim();
      if (!k) return;
      sset(KEY, k);
      plainCall = false;
      keyBlocked = false;
      f.remove();
      $('t-error').textContent = '';
      soon();
    });
    $('t-error').appendChild(f);
  }

  // ---- the end: the poem card (spine:zero 4.6.1) ---------------------------------------------
  function finalLine(t) {
    var w = t.final || [];
    if (!w.length) return '';
    var c = t.chips[w[0]];
    return w.length === 1 ? Z.nameOf(t, w[0]) + ' wins, with ' + c + ' chips.' : names(w) + ' share the win, with ' + c + ' chips each.';
  }
  function dateWords(stamp) {
    var d = new Date(stamp || now());
    if (isNaN(d)) d = new Date();
    return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' });
  }
  function poemPlaces(t) {
    return Z.poem(t);
  }
  function sittersLine(t) {
    return Object.keys(t.seats).sort(function (a, b) { return a - b; }).map(function (s) { return t.seats[s].name; }).join(' · ');
  }
  function poemSheet(t, full) {
    var box = el('div', 'poem');
    box.appendChild(el('p', 'top', 'a game of Zero · ' + dateWords(t.open && t.open.stamp)));
    box.appendChild(el('p', 'path', t.hands.map(function (h) { return cap(h.zero); }).filter(function (w, i, a) { return a.indexOf(w) === i; }).join(' · ')));
    poemPlaces(t).forEach(function (p) {
      p.lines.forEach(function (l, i) {
        var d = el('div', 'place');
        if (!i) d.appendChild(el('div', 'pword', p.word));
        d.appendChild(el('div', 'pline', l.line));
        d.appendChild(el('div', 'pby', l.name));
        box.appendChild(d);
      });
    });
    if (full) box.appendChild(el('p', 'sat', sittersLine(t)));
    return box;
  }
  function lastPlace(t) {
    var played = t.hands.filter(function (h) { return h.winners.length; });
    return played.length ? played[played.length - 1].place : (t.current ? t.current.place : '');
  }
  function trailUrl(t) {
    return location.origin + location.pathname + '?trail=' + lastPlace(t);
  }
  function drawEnd() {
    if ($('end').hidden) show('end');
    var t = T;
    $('e-winner').textContent = finalLine(t);
    var box = $('e-poem');
    box.replaceWith(poemSheet(t, true));
    var fresh = document.querySelector('#end .poem');
    fresh.id = 'e-poem';
    $('e-trail').href = trailUrl(t);
    var notes = $('e-notes');
    notes.textContent = '';
    t.notes.forEach(function (n) {
      var d = el('div', 'n');
      d.appendChild(el('b', null, n.who));
      d.appendChild(el('p', null, n.text));
      notes.appendChild(d);
    });
  }

  // The card as a picture: one printed sheet, drawn like door.js draws its card.
  function fontsReady() {
    if (!document.fonts || !document.fonts.load) return Promise.resolve();
    var loads = Promise.all(['italic 400 40px', 'italic 500 60px', '400 30px'].map(function (f) { return document.fonts.load(f + ' "Cormorant Garamond"'); }));
    return Promise.race([loads, new Promise(function (r) { setTimeout(r, 2500); })]).catch(function () {});
  }
  function cardPicture(t) {
    return fontsReady().then(function () {
      var W = 1080, FACE = '"Cormorant Garamond", Garamond, Georgia, serif';
      var c = document.createElement('canvas');
      var g = c.getContext('2d');
      function wrap(text, font, max) {
        g.font = font;
        var words = String(text).split(/\s+/), lines = [], cur = '';
        words.forEach(function (w) {
          var tryLine = cur ? cur + ' ' + w : w;
          if (g.measureText(tryLine).width > max && cur) { lines.push(cur); cur = w; } else cur = tryLine;
        });
        if (cur) lines.push(cur);
        return lines;
      }
      var parts = [];
      var inner = W - 260;
      parts.push({ t: 'a game of Zero · ' + dateWords(t.open && t.open.stamp), f: 'italic 400 34px ' + FACE, c: '#6d6158', lh: 46 });
      var path = t.hands.map(function (h) { return cap(h.zero); }).filter(function (w, i, a) { return a.indexOf(w) === i; }).join(' · ');
      wrap(path, 'italic 500 46px ' + FACE, inner).forEach(function (l) { parts.push({ t: l, f: 'italic 500 46px ' + FACE, c: '#8e1b1b', lh: 58 }); });
      parts.push({ gap: 40 });
      poemPlaces(t).forEach(function (p) {
        p.lines.forEach(function (l, i) {
          if (!i) parts.push({ t: p.word, f: 'italic 400 32px ' + FACE, c: '#8e1b1b', lh: 44 });
          wrap(l.line, 'italic 400 50px ' + FACE, inner).forEach(function (x) { parts.push({ t: x, f: 'italic 400 50px ' + FACE, c: '#2a2420', lh: 62 }); });
          parts.push({ t: l.name, f: 'italic 400 30px ' + FACE, c: '#6d6158', lh: 42 });
          parts.push({ gap: 30 });
        });
      });
      parts.push({ rule: true });
      wrap(sittersLine(t), 'italic 400 30px ' + FACE, inner).forEach(function (l) { parts.push({ t: l, f: 'italic 400 30px ' + FACE, c: '#6d6158', lh: 42 }); });
      parts.push({ gap: 24 });
      parts.push({ t: 'Zero · made by Matthew Bushell, on David Pinto’s beach', f: 'italic 400 26px ' + FACE, c: '#8a7f74', lh: 36 });
      var H = 170 + parts.reduce(function (a, p) { return a + (p.gap || (p.rule ? 50 : p.lh)); }, 0) + 110;
      c.width = W; c.height = H;
      g.fillStyle = '#f4ecdc'; g.fillRect(0, 0, W, H);
      g.fillStyle = '#faf5ea'; g.fillRect(70, 70, W - 140, H - 140);
      g.strokeStyle = '#b9a98c'; g.lineWidth = 2;
      g.strokeRect(70, 70, W - 140, H - 140);
      g.strokeRect(56, 56, W - 112, H - 112);
      g.textAlign = 'center'; g.textBaseline = 'alphabetic';
      var y = 170;
      parts.forEach(function (p) {
        if (p.gap) { y += p.gap; return; }
        if (p.rule) { g.beginPath(); g.moveTo(W / 2 - 120, y + 10); g.lineTo(W / 2 + 120, y + 10); g.stroke(); y += 50; return; }
        g.font = p.f; g.fillStyle = p.c;
        g.fillText(p.t, W / 2, y);
        y += p.lh;
      });
      return new Promise(function (resolve) { c.toBlob(function (b) { resolve(b); }, 'image/png'); });
    });
  }
  function cardName(t) { return 'zero-' + t.hands.map(function (h) { return h.zero; }).filter(function (w, i, a) { return a.indexOf(w) === i; }).join('-').toLowerCase().replace(/[^a-z0-9-]+/g, '') + '.png'; }
  function keepCard() {
    var said = $('e-said');
    said.textContent = 'Making the card…';
    cardPicture(T).then(function (blob) {
      var a = el('a');
      a.href = URL.createObjectURL(blob);
      a.download = cardName(T);
      document.body.appendChild(a);
      a.click();
      setTimeout(function () { URL.revokeObjectURL(a.href); a.remove(); }, 4000);
      said.textContent = 'The card is in your downloads.';
    }, function () { said.textContent = 'The card couldn’t be made on this device.'; });
  }
  function shareCard() {
    var said = $('e-said'), t = T, url = trailUrl(t);
    var text = 'A game of Zero: ' + poemPlaces(t).map(function (p) { return p.lines[0].line; }).join(' / ');
    cardPicture(t).then(function (blob) {
      var file = blob && window.File ? new File([blob], cardName(t), { type: 'image/png' }) : null;
      if (file && navigator.canShare && navigator.canShare({ files: [file] })) return navigator.share({ files: [file], title: 'Zero', text: text + ' ' + url });
      if (navigator.share) return navigator.share({ title: 'Zero', text: text, url: url });
      if (navigator.clipboard) return navigator.clipboard.writeText(url).then(function () { said.textContent = 'The link to this trail is copied.'; });
      said.textContent = url;
    }).catch(function () {});
  }
  function leaveNote(ev) {
    ev.preventDefault();
    var text = String($('note-text').value || '').trim();
    if (!text || !T) return;
    $('note-go').disabled = true;
    post('note', { who: S.name, at: Z.at(lastPlace(T)), code: S.code, text: text }).then(function (r) {
      if (!r.ok) throw new Error(refusal(r));
      $('note-text').value = '';
      $('note-error').textContent = 'Thank you. Your word is on the beach.';
      soon();
    }).catch(function (e) { $('note-error').textContent = e.message; }).then(function () { $('note-go').disabled = false; });
  }

  // ---- a trail read on its own --------------------------------------------------------------
  function trailView(digits) {
    show('trailview');
    var box = $('v-poem');
    box.textContent = 'Reading the tree…';
    readTree().then(function (tree) {
      var tr = Z.trail(tree, digits).filter(function (x) { return x.word; });
      box.textContent = '';
      box.appendChild(el('p', 'top', 'a trail of Zero'));
      box.appendChild(el('p', 'path', tr.map(function (x) { return cap(x.word); }).join(' · ')));
      tr.forEach(function (x) {
        if (!x.voice) return;
        voiceParts(x.voice).forEach(function (v, i) {
          var d = el('div', 'place');
          if (!i) d.appendChild(el('div', 'pword', x.word));
          d.appendChild(el('div', 'pline', v.line));
          if (v.by) d.appendChild(el('div', 'pby', v.by));
          box.appendChild(d);
        });
      });
      if (!tr.length) box.appendChild(el('p', 'soft it', 'This trail isn’t in the tree.'));
    }, function () { box.textContent = 'The tree is out of reach just now.'; });
  }
  // "line (Name, 3 votes of 7)" or 'tied, …: "a" (A) and "b" (B)' → [{line, by}].
  function voiceParts(v) {
    var tie = /^tied,[^:]*:\s*(.*)$/.exec(v);
    if (tie) {
      var out = [], re = /[“"]([^”"]+)[”"]\s*\(([^)]+)\)/g, m;
      while ((m = re.exec(tie[1]))) out.push({ line: m[1], by: m[2] });
      if (out.length) return out;
    }
    var one = /^(.*)\s\(([^,()]+),[^()]*\)\s*$/.exec(v);
    return one ? [{ line: one[1], by: one[2] }] : [{ line: v, by: '' }];
  }

  // ---- wiring ------------------------------------------------------------------------------------
  $('door-host').addEventListener('click', function () { setMode('host'); });
  $('door-join').addEventListener('click', function () { setMode('join'); });
  $('cp-less').addEventListener('click', function () { if (computers > 0) computers--; drawComputers(); });
  $('cp-more').addEventListener('click', function () { if (computers < 8) computers++; drawComputers(); });
  $('key-forget').addEventListener('click', function () { sset(KEY, null); drawComputers(); });
  $('places-more').addEventListener('click', function () {
    if (deeper) { deeper = ''; loadPlaces(); return; }
    moreShown = true; drawPlaces();
  });
  $('lobby-form').addEventListener('submit', function (ev) {
    ev.preventDefault();
    if (mode === 'host') openTable(); else if (mode === 'join') joinTable();
  });
  $('deal-go').addEventListener('click', dealFirst);
  $('w-leave').addEventListener('click', leave);
  $('e-keep').addEventListener('click', keepCard);
  $('e-share').addEventListener('click', shareCard);
  $('e-deeper').addEventListener('click', function () {
    var lp = T ? lastPlace(T) : '';
    forgetTable();
    clearTimeout(timer);
    lobby(true, lp);
  });
  $('e-again').addEventListener('click', function () { forgetTable(); clearTimeout(timer); lobby(); });
  $('note-form').addEventListener('submit', leaveNote);
  document.addEventListener('visibilitychange', function () { if (!document.hidden && S) soon(); });

  if (params.get('trail')) trailView(Z.digitsOf(params.get('trail')));
  else if (S && S.code && S.start) { show('table'); $('t-status').textContent = 'Finding the table…'; enterTable(); }
  else lobby();
  if (params.get('code') && !S) { lobby(false); $('join-code').value = params.get('code'); }
})();
