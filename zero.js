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
  var KEY = 'zero.key', TABLE = 'zero.table';
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
    ['lobby', 'invited', 'waiting', 'table', 'end', 'trailview', 'look'].forEach(function (s) { $(s).hidden = s !== id; });
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
  function look(block, query, ms) {
    var ctl = window.AbortController ? new AbortController() : null;
    var timer = setTimeout(function () { if (ctl) ctl.abort(); }, ms || LIMIT_MS);
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
  // A write in the beach's own shape. Never throws. The beach reads a block,
  // changes it and writes it back, so this device sends its writes to one
  // block one after another (as door.js does).
  var queue = {};
  function write(block, body) {
    var run = function () { return send(block, body); };
    var p = (queue[block] || Promise.resolve()).then(run, run);
    queue[block] = p;
    return p;
  }
  function send(block, body) {
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
  var room = { floor: 0, cache: {}, cacheFloor: 0 };
  function outline(ms) {
    var guess = room.floor || 2;
    return look(ROOM, '&pscale=' + Math.max(0, guess - 1), ms).then(function (r) {
      if (r.status === 404) return { floor: 0, tops: [] };
      if (r.status !== 200 || !r.json) throw new Error('room');
      var f = Number(r.json.floor) || 1;
      if (f !== guess) { room.floor = f; return outline(ms); }
      var tops = (r.json.entries || []).map(function (e) { return String(e.address); }).filter(function (a) { return /^[1-9]$/.test(a); }).sort();
      return { floor: f, tops: tops };
    });
  }
  function pad(addr, f) { addr = String(addr || ''); return addr.length >= f ? addr : '0'.repeat(f - addr.length) + addr; }
  // Every entry from address `from` on; with no `from`, the recent ones.
  function readRoom(from, ms) {
    return outline(ms).then(function (o) {
      // A supernest moves every older entry one rung down: what was kept is read afresh.
      if (o.floor !== room.cacheFloor) { room.cache = {}; room.cacheFloor = o.floor; }
      room.floor = o.floor;
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
        return look(ROOM, '&spindle=' + (t === '0' ? '0' : t + '0'.repeat(F - 1)), ms).then(function (r) {
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
    'Seats: each seat writes one word that belongs under the zero, hidden until all are in. Two things knock a word out for that hand: words that match after lower-casing and singular form knock each other out; and the table\u2019s eye: when the words are turned, every seat may mark one word that does not belong under the zero (never its own), and any word marked by more than half the seats goes out. So the skill is a word that belongs and that nobody else thought of.',
    'The turn: every seat still in writes one short sentence that brings all the surviving words together, and stakes from 0 to all its chips, both hidden until all are in.',
    'The showdown: every seat votes for one sentence, never its own. Most votes takes the whole pot; that sentence becomes the voice of the place, signed. A tie splits the pot.',
    'Deeper: the winner picks one surviving word, and it becomes the next zero, one level down.',
    'Out: a seat knocked out sits out that hand only. A seat with no chips writes no more words or sentences, but keeps its vote to the end.',
    'The end: after four hands, most chips wins. The winning sentences read from the top of the trail down are the game\u2019s poem.',
    'The craft: a sentence wins by bringing all the words together in a single breath. Short sentences win; compression is the skill.',
    'Computer seats fill any seat no person takes. Each writes as if alone, in its own voice, short and plain, and collisions are allowed.'
  ].join('\n');
  // What a player writes is a sentence, everywhere (spine:zero 4.2): the law
  // as read from the beach is told to the computer players in the same words.
  function sentenceWords(text) {
    return String(text).replace(/\bLines\b/g, 'Sentences').replace(/\blines\b/g, 'sentences').replace(/\bLine\b/g, 'Sentence').replace(/\bline\b/g, 'sentence');
  }
  var lawRead = false;
  function readLaw() {
    if (lawRead) return Promise.resolve(LAW_TEXT);
    return look(LAW).then(function (r) {
      if (r.status === 200 && r.json) {
        var lines = [];
        for (var i = 1; i <= 9; i++) { var t = Z.textOf(r.json[String(i)]); if (t) lines.push(t); }
        if (lines.length >= 5) { LAW_TEXT = sentenceWords(lines.join('\n')); lawRead = true; }
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
  var S_LINE = { type: 'object', additionalProperties: false, required: ['sentence', 'stake'], properties: { sentence: { type: 'string' }, stake: { type: 'integer' } } };
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
    var name = Z.nameOf(t, s), chips = Z.stakeable(h, s);
    var left = h.ring.map(function (r) { return h.words[r]; });
    var user = 'The zero word is “' + h.zero + '”. The words still in after the flop are: ' + left.join(', ') + '. Your own word among them is “' + h.words[s] + '”.' +
      '\nWrite a short sentence that brings all these words together, in your own voice. Under twenty words; shorter usually wins.' +
      '\nYou have ' + chips + ' chips. Stake from 0 to ' + chips + ' on your sentence: more if you believe the table will vote for it. Answer as {"sentence": "...", "stake": n}.';
    return tries(3, function () {
      return claude(key, persona(name), user, S_LINE).then(function (j) {
        var l = Z.oneLine(j.sentence != null ? j.sentence : j.line);
        if (!l.ok) throw new Error('again');
        return { move: l.line, stake: Math.max(0, Math.min(chips, Math.round(Number(j.stake) || 0))) };
      });
    });
  }
  function askVote(key, t, h, s) {
    var name = Z.nameOf(t, s);
    var options = Object.keys(h.lines).map(Number).filter(function (x) { return x !== s; });
    var left = h.ring.map(function (r) { return h.words[r]; });
    var user = 'The zero word is “' + h.zero + '” and the words still in are: ' + left.join(', ') + '. The sentences:\n' +
      Object.keys(h.lines).map(function (x) { return 'seat ' + x + ' (' + Z.nameOf(t, Number(x)) + '): ' + h.lines[x].line; }).join('\n') +
      '\nYou are seat ' + s + '. Vote for the sentence that brings the words together best. Not your own. Say why in a few words, in your voice. Answer as {"seat": n, "why": "..."}.';
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
  // The table's eye (function:zero 2): a computer player marks a word only if
  // it plainly does not belong under the zero, never its own; else it skips.
  function askEye(key, t, h, s) {
    var name = Z.nameOf(t, s);
    var options = h.survivors.filter(function (x) { return x !== s; });
    if (!options.length) return Promise.resolve({ move: '0', why: '' });
    var user = 'The zero word is “' + h.zero + '”. The words still in after the flop:\n' +
      h.survivors.map(function (x) { return 'seat ' + x + ': ' + h.words[x] + (x === s ? ' (your own)' : ''); }).join('\n') +
      '\nThe table may send out a word that does not belong under “' + h.zero + '”. Mark a word only if it plainly does not belong, by its seat, and never your own. If every word belongs, or you are unsure, skip with seat 0. Be honest: most words belong. Answer as {"seat": n, "why": "..."}.';
    return tries(3, function () {
      return claude(key, persona(name), user, S_SEAT).then(function (j) {
        var v = Number(j.seat) || 0;
        if (v && options.indexOf(v) < 0) throw new Error('again');
        return { move: String(v), why: String(j.why || '').slice(0, 140) };
      });
    }).catch(function (e) {
      if (e.status != null) throw e;
      return { move: '0', why: '' };
    });
  }
  function askPick(key, t, h, s) {
    var name = Z.nameOf(t, s);
    var user = 'Your sentence won the vote at “' + h.zero + '”. Pick one surviving word to become the next zero, one level deeper:\n' +
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
  var S = jget(TABLE);           // {code, start, name, seat, host, secrets, done}
  function saveS() { jset(TABLE, S); }
  function forgetTable() { S = null; jset(TABLE, null); T = null; if ($('e-shows')) { $('e-shows').dataset.sig = ''; $('e-shows').textContent = ''; } }
  // Who is at this phone comes through the door (door.js), as on Matthew's
  // other pages: "I'm new" offers three words as the key to a name and a
  // card to keep them; "I've been here" takes a name and its words. The
  // words never leave the door: Zero itself writes without a key.
  function nameProblem(n) {
    if (!n) return 'Your name, please.';
    if (!/^[\p{L}\p{N}][\p{L}\p{N} '’.\-]*$/u.test(n)) return 'Letters and numbers for your name, please.';
    if (Z.PERSONAS.some(function (p) { return p.toLowerCase() === n.toLowerCase(); }) || /^computer/i.test(n)) return 'That name belongs to a computer player. Choose another.';
    return '';
  }

  // ---- the lobby -------------------------------------------------------------------------
  var mode = '', places = [], chosen = '', moreShown = false, deeper = '', lobbyTables = {};
  function lobby(asHost, beneath, note) {
    show('lobby');
    address('');
    deeper = beneath || '';
    $('lobby-error').textContent = '';
    setMode(asHost === true ? 'host' : asHost === false ? 'join' : '');
    // a plain line where it can be seen, above the doors: the game was closed, or left
    $('lobby-note').textContent = note || '';
    $('lobby-note').hidden = !note;
  }
  function setMode(m) {
    mode = m;
    if (m) { $('lobby-note').textContent = ''; $('lobby-note').hidden = true; }
    $('door-host').setAttribute('aria-pressed', String(m === 'host'));
    $('door-join').setAttribute('aria-pressed', String(m === 'join'));
    $('lobby-form').hidden = !m;
    $('host-part').hidden = m !== 'host';
    $('join-part').hidden = m !== 'join';
    $('lobby-error').textContent = '';
    if (m === 'host') loadPlaces();
  }
  // The page's address: the table's invite link while seated, else the plain page.
  function inviteUrl(code) {
    var q = new URLSearchParams(location.search);
    ['table', 'code', 'trail', 'look'].forEach(function (k) { q.delete(k); });
    if (code) q.set('table', Z.codeKey(code));
    var qs = q.toString();
    return location.origin + location.pathname + (qs ? '?' + qs : '');
  }
  function address(code) {
    try { if (location.href !== inviteUrl(code)) history.replaceState(null, '', inviteUrl(code)); } catch (e) {}
  }
  function prettyCode(key) { return String(key).replace(/^([A-Z]+)([0-9]+)$/, '$1 $2'); }
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
  // The name at this phone: known already, or asked through the door now.
  function whoAmI() {
    return window.Door.name().then(function (m) {
      var p = nameProblem(m.name);
      if (p) throw new Error(p);
      return { name: m.name };
    }, function () { throw new Error('First, a name: I\u2019m new, or I\u2019ve been here.'); });
  }
  function openTable() {
    var first = '';
    if (chosen === 'new') {
      var w = Z.oneWord($('first-word').value);
      if (!w.ok) { lobbyError(w.why === 'One word, no spaces' ? 'One word to begin, no spaces.' : w.why); $('first-word').focus(); return; }
      first = w.word;
    } else if (!chosen) { lobbyError('Choose where to play.'); return; }
    $('open-go').disabled = true;
    lobbyError('');
    // the whole room, so the new table's code is one no table has used (Look around tells games apart by it)
    whoAmI().then(function (who) { return Promise.all([readTree(), readRoom('0').catch(function () { return readRoom(''); }).catch(function () { return []; })]).then(function (r) {
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
      return post('open', {
        who: who.name, at: Z.at(place), code: code,
        text: code + ' · ' + who.name + ' opens a table at ' + word,
        data: { _: word, 1: who.name, 5: chosen === 'new' ? 'new' : '', 6: 'eye', 7: 'chips' }
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
          S = { code: code, start: start, name: who.name, seat: 1, host: true, secrets: {}, done: {} };
          saveS();
          enterTable();
        });
      });
    }); }).catch(function (e) {
      lobbyError(e.message || 'That didn’t work. Try again.');
      loadPlaces();
    }).then(function () { $('open-go').disabled = false; });
  }
  function joinTable() {
    var key = Z.codeKey($('join-code').value);
    if (!/^[A-Z]+[0-9]+$/.test(key)) { lobbyError('The table code is a word and a number, like LAMP 4.'); $('join-code').focus(); return; }
    $('join-go').disabled = true;
    lobbyError('');
    seatAt(key).then(null, function (e) { lobbyError(e.message || 'That didn\u2019t work. Try again.'); })
      .then(function () { $('join-go').disabled = false; });
  }
  // Take the next free seat at the table with this code, or the seat already
  // held under this name (a phone closed and opened again, spine:zero 4.28).
  function seatAt(key) {
    var who;
    return whoAmI().then(function (m) { who = m; return readRoom(''); }).then(function (list) {
      var open = list.filter(function (e) { return e.kind === 'open' && Z.codeKey(e.code) === key; })[0];
      if (!open) throw new Error('No table with that code is open. Check it with the host.');
      return readRoom(open.addr).then(function (entries) { return Z.derive(entries, open.code); }).then(function (t) {
        var mine = function () {
          var seats = t.seats, found = 0;
          Object.keys(seats).forEach(function (s) { if (seats[s].kind === 'person' && seats[s].name.toLowerCase() === who.name.toLowerCase()) found = Number(s); });
          return found;
        };
        if (mine() && t.roster && t.gone[mine()]) throw new Error('You left this game, so your seat has gone.');
        if (mine()) return { t: t, seat: mine(), open: open };
        if (t.closed) throw new Error('That table has closed.');
        if (t.roster) throw new Error('That table has already started.');
        var free = 9 - t.open.computers - 1 - t.sits.length;
        if (free <= 0) throw new Error('That table is full.');
        return post('sit', {
          who: who.name, at: Z.at(t.open.place), code: open.code, text: open.code + ' · ' + who.name + ' takes a seat'
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
      // The seat's own spelling of the name, so every seal is read as this seat's;
      // the host coming back by the link is the host again.
      var name = r.t.seats[r.seat].name;
      S = { code: r.open.code, start: r.open.addr, name: name, seat: r.seat, host: r.seat === 1 && name === r.t.open.host, secrets: {}, done: {} };
      saveS();
      enterTable();
    });
  }
  // Arriving by an invite link (zero.html?table=<code>): through the door
  // first if this phone has no name, then straight to the next free seat.
  function invited(key) {
    if (S && S.start && Z.codeKey(S.code) === key) { show('table'); $('t-status').textContent = 'Finding the table\u2026'; enterTable(); return; }
    show('invited');
    $('i-code').textContent = prettyCode(key);
    var go = function () {
      $('i-go').disabled = true;
      $('i-error').textContent = '';
      seatAt(key).then(null, function (e) { $('i-error').textContent = e.message || 'That didn\u2019t work. Try again.'; })
        .then(function () { $('i-go').disabled = false; });
    };
    $('i-go').onclick = go;
    go();
  }

  // ---- at the table ------------------------------------------------------------------------
  var T = null, TREE_JSON = null, treeHand = 0, timer = null, polling = false, running = {}, firstSeen = {}, flipped = {}, voteChoice = 0, eyeChoice = 0;
  var lastOk = 0, failing = false, inked = {};
  function enterTable() {
    T = null; treeHand = -1; flipped = {}; firstSeen = {}; voteChoice = 0;
    closeAsk(false);
    address(S.code);
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
      if (T) { draw(); if (T) duties(); }      // drawing a closed table leaves it
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
    if (T.phase === 'closed') { var wasHost = S.host; forgetTable(); clearTimeout(timer); lobby(undefined, undefined, wasHost ? 'The game is closed.' : 'The host closed the game.'); return; }
    if (T.phase === 'waiting') return drawWaiting();
    if (T.phase === 'end' && S.ended) return drawEnd();
    drawTable();
  }

  // The waiting room (spine:zero 4.26): the code and the invite link, who
  // has arrived, the empty seats, and Deal whenever the host chooses.
  function drawWaiting() {
    if ($('waiting').hidden) show('waiting');
    var t = T, o = t.open, seats = t.seats, hasKey = !!sget(KEY);
    $('w-place').textContent = o.isNew ? 'a new first word' : 'beneath ' + o.word;
    $('w-code').textContent = t.code;
    var url = inviteUrl(t.code);
    if ($('w-link').value !== url) $('w-link').value = url;
    $('w-share').hidden = !navigator.share;
    var ul = $('w-roster'), people = [];
    ul.textContent = '';
    Object.keys(seats).map(Number).sort(function (a, b) { return a - b; }).forEach(function (s) {
      var x = seats[s];
      if (x.kind !== 'person') return;
      people.push(s);
      var li = el('li', s === S.seat ? 'me' : '');
      li.appendChild(el('span', 'n', x.name));
      li.appendChild(el('span', 'r', x.name === o.host ? 'host' : s === S.seat ? 'you' : ''));
      ul.appendChild(li);
    });
    var empty = 9 - Object.keys(seats).length;
    $('w-empty').textContent = !empty ? 'Every seat is taken.' :
      empty + (empty === 1 ? ' seat' : ' seats') + ' empty' + (!S.host ? '' : hasKey ? ' \u00b7 computer fills them' : ' \u00b7 without a key, the table plays with who is here');
    $('w-host').hidden = !S.host;
    if (S.host) {
      $('w-key-part').hidden = hasKey;
      $('w-key-kept').hidden = !hasKey;
      var canDeal = hasKey || people.length >= 3;
      $('deal-go').disabled = !canDeal;
      $('w-say').textContent = !canDeal ? 'Without a key, the table needs three people or more. Add a key, or invite more.' :
        hasKey ? (empty ? 'Deal whenever you\u2019re ready: computer players take the ' + empty + ' empty ' + (empty === 1 ? 'seat' : 'seats') + '.' : 'Everyone is here. Deal whenever you\u2019re ready.') :
        'Deal whenever you\u2019re ready: the table plays with the ' + people.length + ' of you.';
      $('w-keep').textContent = hasKey ? 'Keep this page open while you play. The computer players live on your phone.' : 'Keep this page open while you play. The table runs from your phone.';
    } else {
      $('w-say').textContent = 'You\u2019re seated. The game starts when the host deals.';
      $('w-keep').textContent = 'If the host closes their page, the table pauses until they come back.';
    }
    $('w-leave').textContent = S.host ? 'Close this table' : 'Leave this table';
  }
  // The deal: every empty seat becomes a computer player when the host has a
  // key; without one the table plays with who is there, three or more.
  function dealFirst() {
    var t = T, o = t.open, seats = t.seats, used = {}, hasKey = !!sget(KEY);
    var people = Object.keys(seats).filter(function (s) { return seats[s].kind === 'person'; }).length;
    if (!hasKey && people < 3) { $('w-error').textContent = 'Without a key, the table needs three people or more.'; return; }
    Object.keys(seats).forEach(function (s) { used[seats[s].name] = true; });
    var spare = shuffle(Z.PERSONAS.filter(function (p) { return !used[p]; }));
    var roster = { _: o.word };
    for (var s = 1; s <= 9; s++) {
      var x = seats[s];
      if (!x) {
        if (!hasKey) continue;
        x = { name: spare.shift(), kind: 'computer' };
      }
      roster[String(s)] = { _: x.name, 1: x.kind };
    }
    $('deal-go').disabled = true;
    $('w-error').textContent = '';
    readTree().then(function (tree) {
      TREE_JSON = tree;
      var node = Z.nodeAt(tree, o.place);
      if (o.isNew) {
        if (node != null) throw new Error('Someone has just written a first word in that seat. Close this table and open another.');
        return write(TREE, { spindle: Z.at(o.place), content: o.word + ' \u00b7 ' + S.name }).then(function (r) { if (!r.ok) throw new Error(refusal(r)); });
      }
      if (node == null || Z.played(node)) throw new Error('This place has been played since the table opened. Close this table and open another.');
    }).then(function () {
      return post('deal', { who: S.name, at: Z.at(o.place), code: t.code, hand: 1, text: t.code + ' \u00b7 hand one at ' + o.word, data: roster });
    }).then(function (r) {
      if (r && !r.ok) throw new Error(refusal(r));
      soon();
    }).catch(function (e) {
      $('w-error').textContent = e.message || 'The deal didn\u2019t reach the beach. Try again.';
      $('deal-go').disabled = false;
    });
  }
  function copyLink() {
    var url = $('w-link').value, said = $('w-copied');
    var done = function () { said.textContent = 'The link is copied. Send it to whoever you\u2019d like at the table.'; };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(url).then(done, function () { $('w-link').select(); said.textContent = 'Select the link to copy it.'; });
    } else {
      $('w-link').select();
      try { document.execCommand('copy'); done(); } catch (e) { said.textContent = 'Select the link to copy it.'; }
    }
  }
  function shareLink() {
    if (!navigator.share) return;
    navigator.share({ title: 'Zero', text: 'Come and play Zero with me: nine seats, one word.', url: $('w-link').value }).then(null, function () {});
  }
  function addKey() {
    var k = String($('w-key').value || '').trim();
    if (!k) { $('w-key').focus(); return; }
    sset(KEY, k);
    $('w-key').value = '';
    plainCall = false;
    keyBlocked = false;
    drawWaiting();
  }
  // Leaving from the waiting room: the host closes the table for everyone;
  // anyone else frees their seat for the next to arrive.
  function leave() {
    var wasHost = S && S.host;
    if (S && T && !T.over) {
      post(wasHost ? 'close' : 'leave', { who: S.name, at: T.open ? Z.at(T.open.place) : '', code: S.code,
        text: S.code + ' · ' + S.name + (wasHost ? ' closed the table' : ' leaves the table') });
    }
    forgetTable();
    clearTimeout(timer);
    lobby(undefined, undefined, wasHost ? 'You closed the table.' : 'You left the table. Your seat is free.');
  }
  // "Close the game", at the foot of every game screen: asked once, then the
  // host ends the table for everyone, and anyone else frees their seat.
  function closeAsk(on) {
    $('t-close-ask').hidden = !on;
    $('t-close-go').hidden = on;
    if (!on) return;
    $('t-close-say').textContent = S && S.host ? 'This ends the game for everyone at the table.' : 'This frees your seat. The table plays on without you.';
    $('t-close-yes').textContent = S && S.host ? 'Close it for everyone' : 'Leave the table';
  }
  function closeGame() {
    if (!S) return;
    var wasHost = S.host, yes = $('t-close-yes');
    yes.disabled = true;
    post(wasHost ? 'close' : 'leave', { who: S.name, at: T && T.current ? Z.at(T.current.place) : '', code: S.code,
      text: S.code + ' · ' + S.name + (wasHost ? ' closed the game' : ' leaves the table') }).then(function (r) {
      if (!r.ok) throw new Error(refusal(r));
      forgetTable();
      clearTimeout(timer);
      closeAsk(false);
      lobby(undefined, undefined, wasHost ? 'You closed the game. Everyone at the table has been told.' : 'You left the table. Your seat is free.');
    }).catch(function (e) { $('t-error').textContent = e.message; }).then(function () { yes.disabled = false; });
  }

  // ---- the table, drawn ----------------------------------------------------------------------
  function stageWord(t) {
    var h = t.current;
    if (!h) return '';
    if (t.over) return 'the voice';
    if (needsTurn(h)) return 'the flop';
    return { word: 'seats', eye: 'the flop', line: 'the turn', vote: 'the showdown', voice: 'the voice', done: 'the voice' }[h.stage] || '';
  }
  function needsTurn(h) {
    return h.steps.word.shown && !flipped[h.n] && (h.stage === 'eye' || (h.stage === 'done' && h.noRing && !h.steps.eye));
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
    $('t-close').hidden = !!t.over;
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
        if (h.eyeOut[s]) seat.title = 'the table sent it out';
        if (h.wordChips[s]) card.appendChild(el('span', 'earn', s === h.pick && h.chosenChips ? '+2 \u00b7 chosen' : h.wordChips[s] === 2 ? '+2 \u00b7 never played before' : '+1'));
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
      if (t.gone[s]) seat.classList.add('out');
      seat.appendChild(el('span', 'who', x ? x.name + (t.gone[s] ? ' \u00b7 left' : '') : 'open'));
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
    } else if (h.stage === 'eye') {
      // The table's eye (spine:zero 4.23): mark one word that does not belong, never your own, or skip.
      var canMark = st.need.indexOf(mine) >= 0 && !st.seals[mine] && !st.skipSeal[mine] && !computersWaiting(st).length;
      var eask = el('p', 'ask');
      eask.appendChild(document.createTextNode('Does any word not belong under '));
      eask.appendChild(el('em', null, h.zero));
      eask.appendChild(document.createTextNode('? Tap one, or tap Skip.'));
      box.appendChild(eask);
      if (computersWaiting(st).length) {
        box.appendChild(el('p', 'note', 'The computer players are looking first…'));
      } else if (canMark) {
        var tiles = el('div', 'picks eye');
        h.survivors.forEach(function (s) {
          var tb2 = el('button', null, h.words[s]);
          tb2.type = 'button';
          tb2.disabled = s === mine;
          if (s === mine) tb2.title = 'your own word';
          tb2.setAttribute('aria-pressed', String(eyeChoice === s));
          tb2.addEventListener('click', function () { eyeChoice = s; box.dataset.sig = ''; drawMoves(t, h); });
          tiles.appendChild(tb2);
        });
        box.appendChild(tiles);
        var erow = el('div', 'picks');
        var chosenOk = eyeChoice && eyeChoice !== mine && h.survivors.indexOf(eyeChoice) >= 0;
        var mark = el('button', null, 'Seal it');
        mark.type = 'button';
        mark.disabled = !chosenOk;
        var skip = el('button', null, 'Skip');
        skip.type = 'button';
        var eerr = el('p', 'error');
        mark.addEventListener('click', function () {
          mark.disabled = skip.disabled = true;
          sealMine(h, 'eye', String(eyeChoice)).catch(function (e) { eerr.textContent = e.message; mark.disabled = skip.disabled = false; });
        });
        skip.addEventListener('click', function () {
          mark.disabled = skip.disabled = true;
          sealMine(h, 'eye', '0').catch(function (e) { eerr.textContent = e.message; mark.disabled = skip.disabled = false; });
        });
        erow.appendChild(mark); erow.appendChild(skip);
        box.appendChild(erow);
        box.appendChild(eerr);
      } else {
        box.appendChild(el('p', 'note', waitingLine(st)));
      }
    } else if (h.stage === 'line') {
      box.appendChild(wordsLeft(h));
      var gone = sentOut(h);
      if (gone) box.appendChild(gone);
      if (h.ring.indexOf(mine) < 0) {
        box.appendChild(el('p', 'note', h.active.indexOf(mine) < 0 ? 'You have no chips left. You keep your vote.' :
          h.eyeOut[mine] ? 'The table sent your word out, so you sit out this hand. You still vote.' : 'Your word was matched, so you sit out this hand. You still vote.'));
        if (st.need.length) box.appendChild(el('p', 'note', waitingLine(st)));
      } else if (t.open.chips && !h.banked) {
        box.appendChild(el('p', 'note', 'Paying the word chips…'));
      } else if (computersWaiting(st).length) {
        box.appendChild(el('p', 'note', 'The computer players are writing their sentences first…'));
      } else if (!st.seals[mine] && !st.skipSeal[mine]) {
        var chips = Z.stakeable(h, mine);
        var lf = el('form');
        lf.noValidate = true;
        var lab = el('label', 'ask', 'Write a short sentence that brings all these words together.');
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
        var lb = el('button', null, 'Seal it');
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
      box.appendChild(el('p', 'ask', canVote ? 'Vote for the sentence that brings the words together best. Not your own.' : 'The sentences'));
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
        box.appendChild(el('p', 'note', 'There is no sentence for you to vote for but your own.'));
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
      h.pick, t.over, voteChoice, eyeChoice, S.ended ? 1 : 0, h.banked ? 1 : 0].join('|');
  }
  // The words still in, shown above the sentence box; and any the table sent out.
  function wordsLeft(h) {
    var p = el('p', 'ringwords');
    p.setAttribute('aria-label', 'The words');
    h.ring.forEach(function (s) { p.appendChild(el('span', null, h.words[s])); });
    return p;
  }
  function sentOut(h) {
    var out = Object.keys(h.eyeOut).map(Number);
    if (!out.length) return null;
    var p = el('p', 'note');
    out.forEach(function (s, i) {
      if (i) p.appendChild(document.createTextNode(', '));
      p.appendChild(el('s', null, h.words[s]));
    });
    p.appendChild(document.createTextNode(out.length === 1 ? ' \u00b7 the table sent it out' : ' \u00b7 the table sent them out'));
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
      box.appendChild(el('p', 'ask', 'Every word went out, so there are no sentences this hand.'));
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
        box.appendChild(el('p', 'ask', 'Your sentence won. Choose the next word. The table goes one level deeper.'));
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
        box.appendChild(el('p', 'note', t.gone[picker] ? Z.nameOf(t, picker) + ' has left the table, so the host chooses the next word.' : Z.nameOf(t, picker) + ' is choosing the next zero…'));
        var pk = 'pickwait:' + h.n;
        if (!firstSeen[pk]) firstSeen[pk] = Date.now();
        if (S.host && !Z.isComputer(t, picker) && (t.gone[picker] || Date.now() - firstSeen[pk] >= WAIT_BEFORE_GO_ON)) {
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
    var sealWait = Z.waitingSeal(st).filter(function (s) { return !Z.isComputer(t, s) && s !== S.seat; });
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
      return post('seal', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · ' + S.name + ' seals ' + ({ word: 'a word', eye: 'a mark', line: 'a sentence and a stake', vote: 'a vote' })[step], data: data });
    }).then(function (r) {
      if (!r.ok) throw new Error(refusal(r));
      voteChoice = 0;
      eyeChoice = 0;
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
      said.push(Z.nameOf(T, s) + ': ' + (st.name === 'vote' ? 'votes ' + Z.nameOf(T, Number(x.move)) + (x.why ? ' (' + x.why + ')' : '') : st.name === 'eye' ? (x.move === '0' ? 'skips' : 'marks ' + h.words[Number(x.move)]) : st.name === 'line' ? x.move + ' (stake ' + (x.stake || 0) + ')' : x.move));
    });
    if (!any) return Promise.resolve();
    return post('show', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · ' + ({ word: 'words', eye: 'marks', line: 'sentences', vote: 'votes' })[st.name] + ' at ' + h.zero + ': ' + said.join(' | '), data: data });
  }

  // ---- what this device does on each read ------------------------------------------------
  var keyBlocked = false;     // Claude refused the key: wait for another
  // A duty whose key is marked once (S.done) never runs again on this device,
  // whatever a later read shows: a move is never written twice.
  function once(key, fn) {
    if (S.done[key]) return;
    duty(key, function () {
      return Promise.resolve().then(fn).then(function (r) {
        if (r && r.ok === false) throw new Error(refusal(r));
        S.done[key] = true;
        saveS();
      });
    });
  }
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
    if (st && st.sealed && !st.shown) once('show:' + h.n + st.name, function () { return showMine(h, st); });
    if (!S.host) return;
    var key = sget(KEY);
    if (owesWords(t, h)) { once('bank:' + h.n, function () { return payWords(h); }); return; }
    if (st && !st.closed) {
      var comp = computersWaiting(st);
      if (comp.length) {
        if (keyBlocked) return hostEnd(t, h);
        if (!key) { $('t-error').textContent = 'The computer players need your Anthropic key on this device.'; keyBlocked = true; drawKeyBox(); return; }
        once('cmp:' + h.n + st.name, function () { return computerMoves(key, t, h, st, comp); });
      }
    }
    hostEnd(t, h);
  }
  // Word chips (function:zero 2.1), paid from the bank once the eye has spoken:
  // the host's page reads the tree at that moment and puts the award in the
  // table's record, so every phone reads the same payment for good.
  function owesWords(t, h) {
    return !!(t.open && t.open.chips && h && h.steps.word.shown && !h.banked && h.ring.length && !h.noRing &&
      ['line', 'vote', 'voice', 'done'].indexOf(h.stage) >= 0);
  }
  function payWords(h) {
    return readTree().then(function (tree) {
      TREE_JSON = tree;
      var data = { _: 'words' }, said = [];
      h.ring.forEach(function (s) {
        var fresh = !Z.playedBefore(tree, h.words[s]);
        data[String(s)] = fresh ? '2' : '1';
        said.push(h.words[s] + (fresh ? ' +2 (never played before)' : ' +1'));
      });
      return post('bank', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' \u00b7 word chips at ' + h.zero + ': ' + said.join(', '), data: data });
    });
  }
  function hostEnd(t, h) {
    if (!S.host || !h) return;
    var resolved = h.winners.length && (h.stage === 'voice' || h.stage === 'done');
    var treeKey = 'tree:' + h.n;
    if (resolved && !S.done[treeKey]) duty(treeKey, function () { return writePlace(t, h); });
    var treeOk = !resolved || S.done[treeKey];
    if (h.stage === 'voice' && Z.isComputer(t, h.picker) && treeOk && !keyBlocked) {
      var key = sget(KEY);
      if (key) once('pick:' + h.n, function () {
        return askPick(key, t, h, h.picker).then(function (s) {
          return post('pick', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · ' + Z.nameOf(t, h.picker) + ' picks ' + h.words[s] + ' as the next zero', data: { _: String(s) } });
        });
      });
    }
    if (h.stage === 'done' && !t.over && treeOk) {
      var k = 'deal:' + (h.n + 1);
      if (!firstSeen[k]) firstSeen[k] = Date.now();
      if (Date.now() - firstSeen[k] >= PAUSE_BEFORE_DEAL) once(k, function () { return dealNext(t, h); });
      else schedule(Math.min(POLL_MS, PAUSE_BEFORE_DEAL - (Date.now() - firstSeen[k]) + 50));
    }
    if (t.over && treeOk) once('closed', function () {
      return post('close', { who: S.name, at: Z.at(h.place), code: S.code, text: S.code + ' · game over: ' + finalLine(t) });
    });
  }
  function computerMoves(key, t, h, st, seats) {
    var ask = st.name === 'word' ? askWord : st.name === 'eye' ? askEye : st.name === 'line' ? askLine : askVote;
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
      return post('seal', { who: S.name, at: Z.at(h.place), code: S.code, hand: h.n, text: S.code + ' · the computer players seal ' + ({ word: 'their words', eye: 'their marks', line: 'their sentences and stakes', vote: 'their votes' })[st.name], data: data });
    }).then(function (r) { if (!r.ok) throw new Error(refusal(r)); return r; });
  }
  // The played place goes into the tree: its voice, signed, and each
  // surviving word in its seat. Never over a seat or a voice already there.
  // Another table writing elsewhere in the tree at the same moment could
  // be lost under it, so the write is read back, and sent again if missing.
  function writePlace(t, h, attempt) {
    attempt = attempt || 1;
    var nameOf = function (s) { return Z.nameOf(t, s); };
    var content = Z.placeAfter(h, nameOf);
    return readTree().then(function (tree) {
      TREE_JSON = tree;
      var node = Z.nodeAt(tree, h.place);
      if (node != null && Z.played(node)) {
        if (Z.textOf(node) !== content._) $('t-error').textContent = 'This place already had a voice in the tree, so this hand stays in the room only.';
        return { ok: true, stood: true };
      }
      return write(TREE, { spindle: Z.at(h.place), content: content });
    }).then(function (r) {
      if (!r.ok) throw new Error(refusal(r));
      if (r.stood) return true;
      return readTree().then(function (tree) {
        TREE_JSON = tree;
        return Z.textOf(Z.nodeAt(tree, h.place)) === content._;
      });
    }).then(function (landed) {
      if (!landed) {
        if (attempt >= 4) throw new Error('The tree didn\u2019t keep this place. Trying again.');
        return new Promise(function (r) { setTimeout(r, 700 * attempt); }).then(function () { return writePlace(t, h, attempt + 1); });
      }
      S.done['tree:' + h.n] = true;
      saveS();
    });
  }
  function dealNext(t, h) {
    var place = Z.nextPlace(h);
    if (!place) return Promise.resolve();
    var word = h.noRing ? h.zero : h.words[h.pick];
    return post('deal', { who: S.name, at: Z.at(place), code: S.code, hand: h.n + 1, text: S.code + ' · hand ' + HAND_WORDS[h.n + 1] + ' at ' + word, data: { _: word } });
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
  function signed(n) { return n < 0 ? '\u2212' + (-n) : String(n); }
  function bookLines(t) {
    var book = Z.chipsBook(t);
    return Object.keys(t.seats).map(Number).sort(function (a, b) { return a - b; }).map(function (s) {
      var b = book[s] || { words: 0, sentences: 0, total: t.chips[s] };
      return { name: t.seats[s].name, words: b.words, sentences: b.sentences, total: b.total };
    });
  }
  // Everyone who sat, each with chips from words beside chips from sentences.
  function chipsList(t) {
    var ul = el('ul', 'book');
    bookLines(t).forEach(function (r) {
      var li = el('li');
      li.appendChild(el('span', 'bn', r.name));
      li.appendChild(el('span', 'bw', r.words + ' from words \u00b7 ' + signed(r.sentences) + ' from sentences'));
      li.appendChild(el('span', 'bt', String(r.total)));
      ul.appendChild(li);
    });
    return ul;
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
    if (full && t.open && t.open.chips) box.appendChild(chipsList(t));
    else if (full) box.appendChild(el('p', 'sat', sittersLine(t)));
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
    // What this game shows, beneath the poem card, open by default (spine:zero 4.62, 4.91);
    // drawn once a game, so a fold or a lit name stays while the page keeps reading.
    var shows = $('e-shows'), sig = t.code + '|' + t.hands.length + '|' + (t.over ? 1 : 0);
    if (shows.dataset.sig !== sig && window.ZeroLook) {
      shows.dataset.sig = sig;
      shows.textContent = '';
      try {
        var game = window.ZeroLook.tableGame(t);
        var panel = window.ZeroLook.showsPanel(shows, game);
        // Words new to the tree need the tree and the other games, read once,
        // exactly as Look around reads them, so both say the same.
        Promise.all([readTree(), readRoom('0', 25000)]).then(function (r) {
          return window.ZeroLook.games(r[0], r[1]);
        }).then(function (gs) {
          var mine = gs.filter(function (x) { return x.kind === 'table' && Z.codeKey(x.code) === Z.codeKey(t.code) && x.stamp === game.stamp; })[0];
          game.fresh = mine ? mine.fresh : null;
        }).catch(function () { game.fresh = null; }).then(function () {
          if (shows.dataset.sig === sig) window.ZeroLook.setFresh(panel, game);
        });
      } catch (e) { shows.dataset.sig = ''; }
    }
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
      if (t.open && t.open.chips) bookLines(t).forEach(function (r) {
        parts.push({ t: r.name + ' \u00b7 ' + r.words + ' from words \u00b7 ' + signed(r.sentences) + ' from sentences \u00b7 ' + r.total, f: 'italic 400 30px ' + FACE, c: '#6d6158', lh: 42 });
      });
      else wrap(sittersLine(t), 'italic 400 30px ' + FACE, inner).forEach(function (l) { parts.push({ t: l, f: 'italic 400 30px ' + FACE, c: '#6d6158', lh: 42 }); });
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

  // ---- Look around (spine:zero 4.9) ----------------------------------------------------------
  // The tree, every game played as its poem card, and what each game shows,
  // read live from the tree and the whole room; nothing is written.
  function openLook() {
    show('look');
    try { var u = new URL(inviteUrl('')); u.searchParams.set('look', '1'); history.replaceState(null, '', u.href); } catch (e) {}
    var box = $('look-body');
    box.textContent = '';
    box.appendChild(el('p', 'look-note', 'Reading the tree\u2026'));
    Promise.all([readTree(), readRoom('0', 25000)]).then(function (r) {
      return window.ZeroLook.games(r[0], r[1]).then(function (gs) { window.ZeroLook.render(box, r[0], gs); });
    }).catch(function () {
      box.textContent = '';
      box.appendChild(el('p', 'error', 'The tree is out of reach just now. Try again in a moment.'));
    });
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
  $('door-look').addEventListener('click', openLook);
  $('look-back').addEventListener('click', function () { lobby(); });
  $('w-copy').addEventListener('click', copyLink);
  $('w-share').addEventListener('click', shareLink);
  $('w-link').addEventListener('focus', function () { $('w-link').select(); });
  $('w-key-go').addEventListener('click', addKey);
  $('w-key').addEventListener('keydown', function (ev) { if (ev.key === 'Enter') { ev.preventDefault(); addKey(); } });
  $('w-key-forget').addEventListener('click', function () { sset(KEY, null); drawWaiting(); });
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
  $('t-close-go').addEventListener('click', function () { closeAsk(true); });
  $('t-close-no').addEventListener('click', function () { closeAsk(false); });
  $('t-close-yes').addEventListener('click', closeGame);
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

  if (window.Door) { window.Door.whoLine($('who-line')); window.Door.whoLine($('i-who')); }
  var invite = Z.codeKey(params.get('table') || params.get('code') || '');
  if (params.get('trail')) trailView(Z.digitsOf(params.get('trail')));
  else if (params.get('look')) openLook();
  else if (/^[A-Z]+[0-9]+$/.test(invite)) invited(invite);
  else if (S && S.code && S.start) { show('table'); $('t-status').textContent = 'Finding the table…'; enterTable(); }
  else lobby();
})();
