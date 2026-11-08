/* zero-law.js — the law of Zero (function:zero on the beach), as the page
   plays it. Pure logic, no page and no network: the flop, the seals, the
   chips, and the whole table worked out again from the room's entries every
   time it is read, so every phone at a table sees the same game.

   A table lives in pool:zero as entries, each {_: words anyone can read,
   1: who wrote it, 2: the place being played, 3: the beach's arrival stamp,
   6: the table code, 7: what kind of entry, 8: the hand, 9: the move}. Kinds:
     open   the host opens a table: 9 {_: the zero word, 1: host, 2: host's
            three words, 3: computer seats, 4: their names in seat order,
            5: 'new' when the place is a new first word}
     sit    a person takes a seat: 9 {_: their three words}
     deal   the host deals a hand: 9 {_: the zero word}; hand one also names
            every seat, 9.n {_: name, 1: 'person' or 'computer', 2: words}
     seal   moves committed: 9 {_: step, n: SHA-256 for seat n}
     show   moves revealed: 9 {_: step, n: {_: move, 1: salt, 2: stake}}
     go     the host goes on without seats: 9 {_: step, 1: 'seal' or 'show',
            2: the seats, comma separated}
     pick   the winner picks the next zero: 9 {_: the seat whose word}
     close  the host closes the table
     note   a word about the game, left at the end
   Steps are 'word', 'line' (a line and its stake) and 'vote'.

   Where function:zero is silent the page settles it (spine:zero 5.1.7):
   every word knocked out makes a hand with no ring, which counts, and the
   next is dealt at the same zero; a tie between equal stakes goes to the
   line that reached the beach first; a seat the host goes on without sits
   out that move only. */
(function (root) {
  'use strict';

  var PERSONAS = ['Child', 'Sailor', 'Poet', 'Engineer', 'Grandmother', 'Chef', 'Farmer', 'Night nurse'];
  var START = 10;          // chips each seat starts with (function:zero 1)
  var HANDS = 4;           // a game is four hands (function:zero 1, 7)
  var SEATS = 9;
  var STEPS = ['word', 'line', 'vote'];

  // ---- words ------------------------------------------------------------------
  // A word as typed, trimmed of the punctuation round it; inside it, letters,
  // hyphens and apostrophes stay.
  function clean(w) {
    return String(w == null ? '' : w).replace(/[‘’]/g, "'").trim()
      .replace(/^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$/gu, '');
  }
  // One word, or why not (spine:zero 4.2.3).
  function oneWord(raw) {
    var t = String(raw == null ? '' : raw).trim();
    if (!t) return { ok: false, why: 'One word, no spaces' };
    if (/\s/.test(t)) return { ok: false, why: 'One word, no spaces' };
    var w = clean(t);
    if (!w || !/\p{L}/u.test(w)) return { ok: false, why: 'One word, no spaces' };
    if (w.length > 32) return { ok: false, why: 'A shorter word' };
    return { ok: true, word: w };
  }
  function fold(w) { return clean(w).toLowerCase(); }
  // p is the plural of s, in the simple way: house and houses, box and
  // boxes, berry and berries. Watch and watching stay apart (function:zero 2).
  function plural(p, s) {
    if (!s) return false;
    if (p === s + 's' || p === s + 'es') return true;
    return p.length > 3 && /ies$/.test(p) && /y$/.test(s) && p.slice(0, -3) === s.slice(0, -1);
  }
  function same(a, b) {
    var x = fold(a), y = fold(b);
    return !!x && (x === y || plural(x, y) || plural(y, x));
  }
  // The flop: words {seat: word} → {out: {seat: true}, ring: [seats]}.
  // Every seat whose word matches another's is knocked out.
  function flop(words) {
    var seats = Object.keys(words).map(Number).sort(function (a, b) { return a - b; });
    var out = {};
    seats.forEach(function (a) {
      seats.forEach(function (b) {
        if (a !== b && same(words[a], words[b])) out[a] = true;
      });
    });
    return { out: out, ring: seats.filter(function (s) { return !out[s]; }) };
  }
  // A line, kept to two short lines' worth.
  function oneLine(raw) {
    var t = String(raw == null ? '' : raw).replace(/\s+/g, ' ').trim().replace(/^[\[{]+\s*/, '');
    if (!t) return { ok: false, why: 'Write a line first' };
    if (t.length > 160) return { ok: false, why: 'Shorter: a line wins by being held in a breath' };
    return { ok: true, line: t };
  }

  // ---- seals ------------------------------------------------------------------
  function subtle() {
    var c = root.crypto || (typeof require === 'function' ? require('crypto').webcrypto : null);
    return c && c.subtle;
  }
  function randomBytes(n) {
    var c = root.crypto || (typeof require === 'function' ? require('crypto').webcrypto : null);
    var a = new Uint8Array(n);
    c.getRandomValues(a);
    return a;
  }
  function hex(bytes) {
    return Array.prototype.map.call(new Uint8Array(bytes), function (b) { return ('0' + b.toString(16)).slice(-2); }).join('');
  }
  function salt() { return hex(randomBytes(16)); }
  // What a move is, as sealed: a word, a line and its stake, a seat voted for.
  function moveText(step, move, stake) {
    return step === 'line' ? String(move) + '|' + String(stake == null ? 0 : stake) : String(move);
  }
  function sealText(code, hand, step, seat, move, stake, s) {
    return [code, hand, step, seat, moveText(step, move, stake), s].join('|');
  }
  var digests = {};
  function sha256(text) {
    if (digests[text]) return Promise.resolve(digests[text]);
    var bytes = new TextEncoder().encode(text);
    return subtle().digest('SHA-256', bytes).then(function (d) {
      digests[text] = hex(d);
      return digests[text];
    });
  }
  function seal(code, hand, step, seat, move, stake, s) {
    return sha256(sealText(code, hand, step, seat, move, stake, s));
  }

  // ---- addresses and the tree -----------------------------------------------------
  // A place is kept as its digits ("1583") and written as the beach writes
  // it in a floor-one block ("1.583").
  function at(digits) { digits = String(digits || ''); return digits.length > 1 ? digits[0] + '.' + digits.slice(1) : digits; }
  function digitsOf(addr) { return String(addr || '').replace(/[^0-9]/g, ''); }
  function nodeAt(tree, digits) {
    var n = tree;
    for (var i = 0; i < digits.length; i++) {
      if (!n || typeof n !== 'object') return undefined;
      n = n[digits[i]];
    }
    return n;
  }
  function textOf(node) {
    if (typeof node === 'string') return node;
    if (node && typeof node._ === 'string') return node._;
    return '';
  }
  function hasSeats(node) {
    if (!node || typeof node !== 'object') return false;
    for (var i = 1; i <= 9; i++) if (node[String(i)] != null) return true;
    return false;
  }
  // "torch · Child" → {word: 'torch', by: 'Child'}; "candle — line (…)" → {word: 'candle', voice: …}.
  function readPlace(node) {
    var t = textOf(node).trim();
    var dash = t.indexOf(' — ');
    if (dash > 0) return { word: t.slice(0, dash).trim(), voice: t.slice(dash + 3).trim(), by: '' };
    var dot = t.lastIndexOf(' · ');
    if (dot > 0) return { word: t.slice(0, dot).trim(), by: t.slice(dot + 3).trim(), voice: '' };
    return { word: t, by: '', voice: '' };
  }
  // Played: it has seats beneath it or a voice. A place never played again.
  function played(node) {
    return hasSeats(node) || !!readPlace(node).voice;
  }
  // Every word in the tree whose nine seats are all still open, in tree order.
  function openPlaces(tree) {
    var out = [];
    (function walk(node, digits, parent) {
      for (var i = 1; i <= 9; i++) {
        var k = String(i), n = node && node[k];
        if (n == null) continue;
        var p = readPlace(n), d = digits + k;
        if (!p.word) continue;
        if (!played(n)) out.push({ digits: d, word: p.word, by: p.by, parent: parent });
        else walk(n, d, p.word);
      }
    })(tree, '', '');
    return out;
  }
  // Top seats with no word yet: where a new first word can go.
  function freeTops(tree) {
    var out = [];
    for (var i = 1; i <= 9; i++) if (!tree || tree[String(i)] == null) out.push(String(i));
    return out;
  }
  // The words along a trail, top first: [{digits, word, voice}].
  function trail(tree, digits) {
    var out = [];
    for (var i = 1; i <= digits.length; i++) {
      var d = digits.slice(0, i), p = readPlace(nodeAt(tree, d));
      out.push({ digits: d, word: p.word, voice: p.voice, by: p.by });
    }
    return out;
  }
  function plural1(n, one, many) { return n + ' ' + (n === 1 ? one : many); }
  // The place's voice, as the tree has always carried it.
  function voiceText(word, h, nameOf) {
    var m = h.votesCast;
    if (h.winners.length === 1) {
      var w = h.winners[0];
      return word + ' — ' + h.lines[w].line + ' (' + nameOf(w) + ', ' + plural1(h.tally[w] || 0, 'vote', 'votes') + ' of ' + m + ')';
    }
    var parts = h.winners.map(function (w) { return '“' + h.lines[w].line + '” (' + nameOf(w) + ')'; });
    var joined = parts.length === 2 ? parts.join(' and ') : parts.slice(0, -1).join(', ') + ' and ' + parts[parts.length - 1];
    return word + ' — tied, ' + plural1(h.tally[h.winners[0]] || 0, 'vote', 'votes') + ' each of ' + m + ': ' + joined;
  }
  // What a played place becomes: its voice, and each surviving word in its seat.
  function placeAfter(h, nameOf) {
    var o = { _: voiceText(h.zero, h, nameOf) };
    h.ring.forEach(function (s) { o[String(s)] = h.words[s] + ' · ' + nameOf(s); });
    return o;
  }

  // ---- the room's entries ----------------------------------------------------------
  // An entry as read: {addr, who, at, stamp, code, kind, hand, data, text}.
  function entryOf(addr, n) {
    if (!n || typeof n !== 'object' || typeof n._ !== 'string') return null;
    return {
      addr: addr,
      text: n._,
      who: typeof n['1'] === 'string' ? n['1'] : '',
      at: typeof n['2'] === 'string' ? n['2'] : '',
      stamp: typeof n['3'] === 'string' ? n['3'] : '',
      code: typeof n['6'] === 'string' ? n['6'] : '',
      kind: typeof n['7'] === 'string' ? n['7'] : '',
      hand: typeof n['8'] === 'string' ? Number(n['8']) || 0 : 0,
      data: n['9'] && typeof n['9'] === 'object' ? n['9'] : (typeof n['9'] === 'string' ? { _: n['9'] } : {})
    };
  }
  // Every entry under node, which stands at prefix in a block of the given
  // floor: entries sit at depth floor, and a "_" that holds an object is an
  // older, supernested root, read with a nought.
  function entriesIn(node, prefix, floor, out) {
    out = out || [];
    if (!node || typeof node !== 'object') return out;
    if (prefix.length >= floor) {
      var e = entryOf(prefix, node);
      if (e) out.push(e);
      return out;
    }
    if (node._ && typeof node._ === 'object') entriesIn(node._, prefix + '0', floor, out);
    for (var i = 1; i <= 9; i++) entriesIn(node[String(i)], prefix + i, floor, out);
    return out;
  }
  function codeKey(c) { return String(c || '').toUpperCase().replace(/[^A-Z0-9]/g, ''); }
  function byAddr(a, b) {
    var w = Math.max(a.addr.length, b.addr.length);
    var x = ('0'.repeat(w) + a.addr).slice(-w), y = ('0'.repeat(w) + b.addr).slice(-w);
    return x < y ? -1 : x > y ? 1 : 0;
  }

  // ---- the table, worked out --------------------------------------------------------
  function nameFold(n) { return String(n || '').toLowerCase().replace(/\s+/g, ' ').trim(); }
  function seatsCsv(s) {
    return String(s || '').split(',').map(function (x) { return Number(x.trim()); }).filter(function (x) { return x >= 1 && x <= 9; });
  }
  function newStep(name, need) {
    return { name: name, need: need.slice(), seals: {}, shows: {}, skipSeal: {}, skipShow: {}, closed: false, sealed: false, shown: false };
  }
  function stepFor(h) {
    return h.stage === 'word' ? h.steps.word : h.stage === 'line' ? h.steps.line : h.stage === 'vote' ? h.steps.vote : null;
  }
  // Seats still to seal, or to show, in a step.
  function waitingSeal(st) {
    return st.need.filter(function (s) { return !st.seals[s] && !st.skipSeal[s]; });
  }
  function waitingShow(st) {
    return st.need.filter(function (s) { return st.seals[s] && !st.shows[s] && !st.skipShow[s]; });
  }

  // The whole table from its entries, oldest first. Resolves the state:
  // {code, open, sits, roster, hands, chips, phase, over, closed, final}.
  function derive(all, code) {
    var key = codeKey(code);
    var list = all.filter(function (e) { return codeKey(e.code) === key; }).sort(byAddr);
    var t = { code: code, open: null, sits: [], roster: null, hands: [], chips: {}, phase: 'none', over: false, closed: false, notes: [], last: null };
    var checks = [];
    // First pass: gather what must be checked against its seal.
    list.forEach(function (e) { t.last = e; });
    return Promise.all(list.map(function (e, i) {
      if (e.kind !== 'show') return null;
      var step = e.data._, out = {};
      return Promise.all(Object.keys(e.data).filter(function (k) { return /^[1-9]$/.test(k); }).map(function (k) {
        var m = e.data[k];
        if (!m || typeof m !== 'object') return null;
        return seal(e.code, e.hand, step, k, textOf(m), m['2'], m['1']).then(function (h) { out[k] = h; });
      })).then(function () { checks[i] = out; });
    })).then(function () {
      list.forEach(function (e, i) { apply(t, e, checks[i] || {}); });
      finish(t);
      return t;
    });
  }

  function hostName(t) { return t.open ? t.open.host : ''; }
  function nameOf(t, s) {
    var r = t.roster && t.roster[s];
    if (r) return r.name;
    var seats = provisional(t);
    return seats[s] ? seats[s].name : 'seat ' + s;
  }
  // Seats before the deal: the host first, people in the order they sat,
  // then the computer seats.
  function provisional(t) {
    var seats = {};
    if (!t.open) return seats;
    var people = SEATS - t.open.computers;
    seats[1] = { name: t.open.host, kind: 'person', three: t.open.three };
    var n = 2;
    t.sits.forEach(function (s) { if (n <= people) seats[n++] = { name: s.name, kind: 'person', three: s.three }; });
    t.open.personas.forEach(function (p, i) { seats[people + 1 + i] = { name: p, kind: 'computer', three: '' }; });
    return seats;
  }
  function isComputer(t, s) { return !!(t.roster && t.roster[s] && t.roster[s].kind === 'computer'); }
  // Who may write for a seat: its person, or the host for a computer seat.
  function writerOf(t, s) { return isComputer(t, s) ? hostName(t) : nameOf(t, s); }

  function apply(t, e, check) {
    var d = e.data || {};
    if (e.kind === 'note' && t.open) { t.notes.push({ who: e.who, text: e.text, stamp: e.stamp }); return; }
    if (t.closed) return;
    if (e.kind === 'open') {
      if (t.open) return;
      var personas = String(d['4'] || '').split(',').map(function (x) { return x.trim(); }).filter(Boolean);
      t.open = {
        addr: e.addr, host: String(d['1'] || e.who || ''), three: String(d['2'] || ''), word: String(d._ || ''),
        place: digitsOf(e.at), computers: Math.max(0, Math.min(8, Number(d['3']) || 0)), personas: personas,
        isNew: d['5'] === 'new', stamp: e.stamp
      };
      t.open.personas = personas.slice(0, t.open.computers);
      t.phase = 'waiting';
      return;
    }
    if (!t.open) return;
    var host = e.who === t.open.host;
    if (e.kind === 'close') { if (host) t.closed = true; return; }
    if (e.kind === 'sit') {
      if (t.roster) return;
      var nm = String(e.who || '').trim();
      var taken = [t.open.host].concat(t.sits.map(function (s) { return s.name; })).concat(t.open.personas);
      if (!nm || taken.some(function (x) { return nameFold(x) === nameFold(nm); })) return;
      if (t.sits.length >= SEATS - t.open.computers - 1) return;
      t.sits.push({ name: nm, three: String(d._ || ''), addr: e.addr });
      return;
    }
    if (e.kind === 'deal') {
      if (!host) return;
      var n = t.hands.length + 1;
      if (e.hand !== n || n > HANDS) return;
      var prev = t.hands[n - 2];
      if (prev && prev.stage !== 'done') return;
      if (n === 1) {
        var roster = {};
        for (var s = 1; s <= SEATS; s++) {
          var r = d[String(s)];
          if (r && typeof r === 'object' && textOf(r)) roster[s] = { name: textOf(r), kind: r['1'] === 'computer' ? 'computer' : 'person', three: String(r['2'] || '') };
        }
        if (!Object.keys(roster).length) return;
        t.roster = roster;
        Object.keys(roster).forEach(function (k) { t.chips[k] = START; });
      }
      var place = digitsOf(e.at);
      var active = Object.keys(t.roster).map(Number).filter(function (k) { return t.chips[k] > 0; }).sort(function (a, b) { return a - b; });
      var h = {
        n: n, place: place, zero: String(d._ || ''), dealAddr: e.addr, active: active,
        steps: { word: newStep('word', active) }, words: {}, voidSeats: {}, out: {}, ring: [], lines: {}, votes: {},
        tally: {}, winners: [], pot: 0, shares: {}, picker: 0, pick: 0, stage: 'word', noRing: false, votesCast: 0, chipsBefore: copy(t.chips)
      };
      t.hands.push(h);
      if (!active.length) h.stage = 'done';
      return;
    }
    var h2 = t.hands[t.hands.length - 1];
    if (!h2 || e.hand !== h2.n) return;
    if (e.kind === 'pick') {
      if (h2.stage !== 'voice') return;
      // The picker picks; the host may pick for a computer seat, or for a
      // person who has gone (the page offers it only after a long wait).
      if (e.who !== writerOf(t, h2.picker) && !host) return;
      var ps = Number(d._);
      if (h2.ring.indexOf(ps) < 0) return;
      h2.pick = ps;
      h2.stage = 'done';
      return;
    }
    var st = stepFor(h2);
    if (!st || d._ !== st.name) return;
    if (e.kind === 'seal') {
      if (st.closed) return;
      for (var k = 1; k <= SEATS; k++) {
        var hash = d[String(k)];
        if (typeof hash !== 'string' || !/^[0-9a-f]{64}$/.test(hash)) continue;
        if (st.need.indexOf(k) < 0 || st.seals[k] || st.skipSeal[k]) continue;
        if (e.who !== writerOf(t, k)) continue;
        st.seals[k] = { hash: hash, addr: e.addr };
      }
    } else if (e.kind === 'go') {
      if (!host) return;
      seatsCsv(d['2']).forEach(function (k) {
        if (d['1'] === 'show') { if (st.seals[k] && !st.shows[k]) st.skipShow[k] = true; }
        else if (!st.seals[k]) st.skipSeal[k] = true;
      });
    } else if (e.kind === 'show') {
      for (var j = 1; j <= SEATS; j++) {
        var m = d[String(j)];
        if (!m || typeof m !== 'object' || !st.seals[j] || st.shows[j] || st.skipShow[j]) continue;
        st.closed = true;     // sealing ends with the first reveal
        st.shows[j] = { move: textOf(m), stake: Number(m['2']) || 0, ok: check[String(j)] === st.seals[j].hash, addr: e.addr };
      }
    }
    advance(t, h2);
  }

  function copy(o) { var c = {}; Object.keys(o).forEach(function (k) { c[k] = o[k]; }); return c; }

  // Move a hand on as far as its moves allow.
  function advance(t, h) {
    var st = stepFor(h);
    if (!st) return;
    if (!waitingSeal(st).length) { st.sealed = true; st.closed = true; }
    if (!st.sealed || waitingShow(st).length) return;
    st.shown = true;
    var ok = function (s) { return st.shows[s] && st.shows[s].ok; };
    if (st.name === 'word') {
      var words = {};
      h.active.forEach(function (s) {
        if (ok(s) && oneWord(st.shows[s].move).ok) words[s] = oneWord(st.shows[s].move).word;
        else h.voidSeats[s] = true;
      });
      var f = flop(words);
      h.words = words;
      h.out = f.out;
      h.active.forEach(function (s) { if (h.voidSeats[s]) h.out[s] = true; });
      h.ring = f.ring;
      if (!h.ring.length) { h.noRing = true; h.stage = 'done'; return; }
      h.stage = 'line';
      h.steps.line = newStep('line', h.ring);
      return advance(t, h);
    }
    if (st.name === 'line') {
      h.ring.forEach(function (s) {
        if (!ok(s) || !oneLine(st.shows[s].move).ok) return;
        var stake = Math.max(0, Math.min(h.chipsBefore[s] || 0, Math.floor(st.shows[s].stake || 0)));
        h.lines[s] = { line: oneLine(st.shows[s].move).line, stake: stake, sealAddr: st.seals[s].addr };
      });
      var writers = Object.keys(h.lines).map(Number);
      if (!writers.length) { h.noRing = true; h.stage = 'done'; return; }
      h.pot = writers.reduce(function (a, s) { return a + h.lines[s].stake; }, 0);
      // Every seat votes, never for its own line; a seat with nothing else to vote for sits it out.
      var voters = Object.keys(t.roster).map(Number).filter(function (s) {
        return writers.some(function (w) { return w !== s; });
      }).sort(function (a, b) { return a - b; });
      h.stage = 'vote';
      h.steps.vote = newStep('vote', voters);
      return advance(t, h);
    }
    if (st.name === 'vote') {
      var lineSeats = Object.keys(h.lines).map(Number);
      lineSeats.forEach(function (s) { h.tally[s] = 0; });
      st.need.forEach(function (s) {
        if (!ok(s)) return;
        var v = Number(st.shows[s].move);
        if (v === s || !h.lines[v]) return;
        h.votes[s] = v;
        h.tally[v]++;
        h.votesCast++;
      });
      var max = Math.max.apply(null, lineSeats.map(function (s) { return h.tally[s]; }));
      var order = function (a, b) {
        return (h.lines[b].stake - h.lines[a].stake) || byAddr({ addr: h.lines[a].sealAddr }, { addr: h.lines[b].sealAddr }) || (a - b);
      };
      h.winners = lineSeats.filter(function (s) { return h.tally[s] === max; }).sort(order);
      var each = Math.floor(h.pot / h.winners.length), odd = h.pot - each * h.winners.length;
      h.winners.forEach(function (s, i) { h.shares[s] = each + (i < odd ? 1 : 0); });
      lineSeats.forEach(function (s) { t.chips[s] -= h.lines[s].stake; });
      h.winners.forEach(function (s) { t.chips[s] += h.shares[s]; });
      h.picker = h.winners[0];
      h.stage = h.n >= HANDS ? 'done' : 'voice';
    }
  }

  function finish(t) {
    var h = t.hands[t.hands.length - 1];
    var anyChips = t.roster && Object.keys(t.roster).some(function (k) { return t.chips[k] > 0; });
    if (h && h.stage === 'done' && (h.n >= HANDS || !anyChips)) t.over = true;
    if (t.over) {
      var max = Math.max.apply(null, Object.keys(t.roster).map(function (k) { return t.chips[k]; }));
      t.final = Object.keys(t.roster).map(Number).filter(function (k) { return t.chips[k] === max; });
    }
    t.phase = t.closed && !t.over ? 'closed' : t.over ? 'end' : !t.open ? 'none' : !h ? 'waiting' : h.stage;
    t.current = h || null;
    t.seats = t.roster || provisional(t);
  }

  // Where the next hand is dealt: beneath the picked word, or at the same
  // zero when a hand had no ring.
  function nextPlace(h) {
    if (!h || h.stage !== 'done') return null;
    if (h.noRing) return h.place;
    if (h.pick) return h.place + String(h.pick);
    return null;
  }

  // The game's poem: the winning lines down the trail.
  function poem(t) {
    return t.hands.filter(function (h) { return h.winners.length; }).map(function (h) {
      return { word: h.zero, digits: h.place, lines: h.winners.map(function (w) { return { line: h.lines[w].line, name: nameOf(t, w), votes: h.tally[w] }; }) };
    });
  }

  var api = {
    PERSONAS: PERSONAS, START: START, HANDS: HANDS, SEATS: SEATS, STEPS: STEPS,
    clean: clean, oneWord: oneWord, oneLine: oneLine, fold: fold, same: same, flop: flop,
    salt: salt, seal: seal, sealText: sealText, sha256: sha256,
    at: at, digitsOf: digitsOf, nodeAt: nodeAt, textOf: textOf, readPlace: readPlace, played: played,
    openPlaces: openPlaces, freeTops: freeTops, trail: trail, voiceText: voiceText, placeAfter: placeAfter,
    entryOf: entryOf, entriesIn: entriesIn, codeKey: codeKey, byAddr: byAddr,
    derive: derive, nameOf: nameOf, isComputer: isComputer, writerOf: writerOf, provisional: provisional,
    waitingSeal: waitingSeal, waitingShow: waitingShow, stepFor: stepFor, nextPlace: nextPlace, poem: poem
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.ZeroLaw = api;
})(typeof self !== 'undefined' ? self : this);
