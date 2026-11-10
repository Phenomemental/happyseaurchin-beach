/* zero-look.js — Look around (spine:zero 4.9): the tree, every game played
   as its poem card, and what each game shows, read live from the tree zero
   and each table's record in pool:zero. Nothing is stored for any of it.

   A game played on the page is read exactly: its table's entries are worked
   out again by the law (zero-law.js), seals and all. The first games were
   played in chat before the page existed; their places are in the tree, and
   their record in the room is prose, written by the computer table as it
   went ("Child votes Phenomemental", "Flop: watch from both Sailor and Night
   nurse"), so for those the votes and the matches are read from that prose.

   The analysis is pure (games(), across()) and runs in Node for the checks;
   render() draws it in the page. */
(function (root) {
  'use strict';
  var Z = root.ZeroLaw || (typeof require === 'function' ? require('./zero-law.js') : null);

  // ---- the tree's voices ---------------------------------------------------------------
  // "line (Name, 3 votes of 7)" or 'tied, 2 votes each of 7: "a" (A) and "b" (B)' → [{line, by, votes}].
  function voiceParts(v) {
    v = String(v || '');
    var tie = /^tied,\s*(\d+)\s+votes?\s+each[^:]*:\s*(.*)$/.exec(v);
    if (tie) {
      var out = [], re = /[“"]([^”"]+)[”"]\s*\(([^)]+)\)/g, m;
      while ((m = re.exec(tie[2]))) out.push({ line: m[1], by: m[2], votes: Number(tie[1]) });
      if (out.length) return out;
    }
    var one = /^(.*)\s\(([^,()]+),\s*(\d+)\s+votes?\s+of\s+\d+\)\s*$/.exec(v);
    if (one) return [{ line: one[1], by: one[2], votes: Number(one[3]) }];
    var loose = /^(.*)\s\(([^,()]+),[^()]*\)\s*$/.exec(v);
    return loose ? [{ line: loose[1], by: loose[2], votes: 0 }] : [{ line: v, by: '', votes: 0 }];
  }
  // Every voiced place in the tree, in tree order: [digits].
  function voicedPlaces(tree) {
    var out = [];
    (function walk(node, digits) {
      for (var i = 1; i <= 9; i++) {
        var n = node && node[String(i)];
        if (n == null) continue;
        if (Z.readPlace(n).voice) out.push(digits + i);
        if (typeof n === 'object') walk(n, digits + i);
      }
    })(tree, '');
    return out;
  }
  function stampOk(s) { return typeof s === 'string' && !isNaN(Date.parse(s)); }
  function escape(s) { return String(s).replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }
  function uniq(list) { var seen = {}; return list.filter(function (x) { if (!x || seen[x]) return false; seen[x] = true; return true; }); }

  // ---- games played on the page ----------------------------------------------------------
  function tableGame(t) {
    var nameOf = function (s) { return Z.nameOf(t, s); };
    var num = function (a, b) { return a - b; };
    var seats = Object.keys(t.roster).map(Number).sort(num);
    var g = {
      kind: 'table', code: t.code, stamp: t.open.stamp || '', people: seats.map(nameOf),
      places: Z.poem(t), votes: [], same: [], alone: [], sentOut: [], words: [], hands: [], rounds: [], computer: {},
      book: (function () {
        var b = Z.chipsBook(t);
        return Object.keys(t.roster).map(Number).sort(function (x, y) { return x - y; }).map(function (s) {
          return { name: nameOf(s), words: b[s].words, sentences: b[s].sentences, total: b[s].total };
        });
      })()
    };
    t.hands.forEach(function (h) {
      if (!h.steps.word.shown) return;
      g.hands.push(h.place);
      // where two (or more) thought the same: the seats each match knocked out
      var matched = Object.keys(h.words).map(Number).filter(function (s) { return h.out[s] && !h.eyeOut[s] && !h.voidSeats[s]; });
      var groups = [];
      matched.forEach(function (s) {
        var grp = groups.filter(function (gr) { return Z.same(h.words[gr[0]], h.words[s]); })[0];
        if (grp) grp.push(s); else groups.push([s]);
      });
      groups.filter(function (gr) { return gr.length > 1; }).forEach(function (gr) {
        g.same.push({ word: h.words[gr[0]], words: gr.map(function (s) { return h.words[s]; }), names: gr.map(nameOf), place: h.zero });
      });
      h.ring.forEach(function (s) { g.alone.push({ word: h.words[s], name: nameOf(s), place: h.zero }); });
      Object.keys(h.eyeOut).forEach(function (s) { g.sentOut.push({ word: h.words[s], name: nameOf(Number(s)), place: h.zero }); });
      Object.keys(h.words).forEach(function (s) { g.words.push({ word: h.words[s], name: nameOf(Number(s)), place: h.zero, collided: matched.indexOf(Number(s)) >= 0 }); });
      Object.keys(h.votes).forEach(function (s) { g.votes.push({ from: nameOf(Number(s)), to: nameOf(h.votes[s]), place: h.zero }); });
      // the hand as the readings of 4.91 need it: every word turned, every
      // sentence with its stake and votes, and the eye's marks
      var voiced = h.winners.length > 0;
      var r = { place: h.place, zero: h.zero, words: [], lines: [], eye: !!(h.steps.eye && h.steps.eye.shown && !h.noEye), marks: [] };
      Object.keys(h.words).map(Number).sort(num).forEach(function (s) {
        r.words.push({ seat: s, word: h.words[s], name: nameOf(s), kept: voiced && h.ring.indexOf(s) >= 0 });
      });
      Object.keys(h.lines).map(Number).sort(num).forEach(function (s) {
        r.lines.push({ name: nameOf(s), line: h.lines[s].line, stake: h.lines[s].stake, votes: h.tally[s] || 0, won: h.winners.indexOf(s) >= 0, thread: h.thread[s] || '' });
      });
      Object.keys(h.marks).map(Number).sort(num).forEach(function (s) {
        var m = h.marks[s];
        r.marks.push({ from: nameOf(s), to: nameOf(m), word: h.words[m], out: !!h.eyeOut[m] });
      });
      g.rounds.push(r);
    });
    seats.forEach(function (s) { if (Z.isComputer(t, s)) g.computer[nameOf(s)] = true; });
    g.first = (g.hands[0] || '')[0] || '';
    g.threaded = !!t.open.thread;          // this table paid the thread
    g.titles = t.over ? Z.titles(t) : [];
    return g;
  }
  // A table is its entries from its open on. A code can come round again
  // for a later table, so each open begins a table of its own.
  // Every table in the room that played a hand to its vote, as the law works it out.
  function tables(entries) {
    var byCode = {}, lists = [];
    entries.slice().sort(Z.byAddr).forEach(function (e) {
      var k = Z.codeKey(e.code);
      if (!k || !e.kind) return;
      var runs = byCode[k] || (byCode[k] = []);
      if (e.kind === 'open' || !runs.length) runs.push([]);
      runs[runs.length - 1].push(e);
    });
    Object.keys(byCode).forEach(function (k) { byCode[k].forEach(function (list) { lists.push(list); }); });
    return Promise.all(lists.map(function (list) {
      if (!list.some(function (e) { return e.kind === 'open'; }) || !list.some(function (e) { return e.kind === 'deal'; })) return null;
      return Z.derive(list, list[0].code).then(function (t) {
        return t.roster && t.hands.some(function (h) { return h.winners.length; }) ? t : null;
      });
    })).then(function (ts) { return ts.filter(Boolean); });
  }
  function tableGames(entries) {
    return tables(entries).then(function (ts) { return ts.map(tableGame); });
  }

  // ---- games played in chat, before the page --------------------------------------------
  // Voiced places no page table covers, gathered into trails: each joins the
  // trail whose last place is its parent.
  function earlierGames(tree, entries, covered) {
    var chains = [];
    voicedPlaces(tree).filter(function (d) { return !covered[d]; }).sort(function (a, b) { return a.length - b.length || (a < b ? -1 : 1); }).forEach(function (d) {
      var c = chains.filter(function (ch) { return ch[ch.length - 1] === d.slice(0, -1); })[0];
      if (c) c.push(d); else chains.push([d]);
    });
    return chains.map(function (chain) {
      var inChain = {};
      chain.forEach(function (d) { inChain[d] = true; });
      var g = { kind: 'earlier', code: '', stamp: '', people: [], places: [], votes: [], same: [], alone: [], sentOut: [], words: [], hands: chain.slice(), first: chain[0][0], rounds: [], computer: {} };
      var people = [], wordOf = {}, seatOf = {}, roundAt = {};
      chain.forEach(function (d) {
        var node = Z.nodeAt(tree, d), rp = Z.readPlace(node);
        wordOf[d] = rp.word;
        var lines = voiceParts(rp.voice).map(function (v) { return { line: v.line, name: v.by, votes: v.votes }; });
        g.places.push({ word: rp.word, digits: d, lines: lines });
        var r = roundAt[d] = { place: d, zero: rp.word, words: [], lines: [], eye: false, marks: [], winners: lines.map(function (l) { return l.name; }) };
        g.rounds.push(r);
        lines.forEach(function (l) { people.push(l.name); });
        for (var i = 1; i <= 9; i++) {
          var child = node && typeof node === 'object' ? node[String(i)] : null;
          if (child == null) continue;
          var cp = Z.readPlace(child);
          if (!cp.word) continue;
          people.push(cp.by);
          if (cp.by && !(seatOf[cp.by] <= i)) seatOf[cp.by] = i;
          g.alone.push({ word: cp.word, name: cp.by, place: rp.word });
          g.words.push({ word: cp.word, name: cp.by, place: rp.word, collided: false });
          r.words.push({ seat: i, word: cp.word, name: cp.by, kept: true });
        }
      });
      // the record the computer table wrote at these places, in prose
      var prose = entries.filter(function (e) { return !e.kind && inChain[Z.digitsOf(e.at)]; });
      prose.forEach(function (e) { people.push(e.who); });
      // in the order they sat: by the seat each first wrote in
      g.people = uniq(people.filter(function (n) { return n && n !== 'zero-table' && n.length <= 40; })).sort(function (a, b) {
        return (seatOf[a] || 10) - (seatOf[b] || 10);
      });
      var stamps = prose.map(function (e) { return e.stamp; }).filter(stampOk).sort();
      g.stamp = stamps[0] || '';
      var alt = g.people.slice().sort(function (a, b) { return b.length - a.length; }).map(escape).join('|');
      var seen = {};
      prose.forEach(function (e) {
        var place = wordOf[Z.digitsOf(e.at)];
        var add = function (from, to) {
          var k = place + '|' + from;
          if (seen[k] || from === to) return;
          seen[k] = true;
          g.votes.push({ from: from, to: to, place: place });
        };
        if (alt) {
          var re = new RegExp('(' + alt + ') (?:votes|to) (' + alt + ')\\b', 'g'), m;
          while ((m = re.exec(e.text))) add(m[1], m[2]);
          var own = new RegExp('Showdown vote at [^:]+: (' + alt + ')\\.').exec(e.text);
          if (own) add(e.who, own[1]);
        }
        var r = roundAt[Z.digitsOf(e.at)];
        var fre = /Flop: (\S+) from both ([^,.;]+?) and ([^,.;]+?)(?=[,.;])/g, f;
        while ((f = fre.exec(e.text))) {
          g.same.push({ word: f[1], words: [f[1], f[1]], names: [f[2].trim(), f[3].trim()], place: place });
          g.words.push({ word: f[1], name: f[2].trim(), place: place, collided: true }, { word: f[1], name: f[3].trim(), place: place, collided: true });
          if (r) r.words.push({ seat: 0, word: f[1], name: f[2].trim(), kept: false }, { seat: 0, word: f[1], name: f[3].trim(), kept: false });
        }
        // the sentences and their stakes: "Child (stake 3): Things that let you see in the dark. | Sailor (stake 5): …",
        // a player's own "Turn line at Light (stake 5): …", and "Phenomemental stakes 5 at Vigil" named later
        if (!r) return;
        var said = function (name, line, stake) {
          if (!name || !line || r.lines.some(function (l) { return l.name === name; })) return;
          r.lines.push({ name: name, line: line.trim(), stake: stake });
        };
        if (alt) {
          var lre = new RegExp('(?:^|[.:|] )(' + alt + ') \\(stake (\\d+)[^)]*\\): (.*?)(?= \\| (?:' + alt + ') \\(stake| Votes:|$)', 'g'), l;
          while ((l = lre.exec(e.text))) said(l[1], l[3], Number(l[2]));
          var later = new RegExp('(' + alt + ') stakes (\\d+) at ').exec(e.text);
          if (later) r.lines.forEach(function (x) { if (x.name === later[1] && x.stake == null) x.stake = Number(later[2]); });
        }
        var mine = /^Turn line at [^(:]+\(stake ([^)]*)\): (.*)$/.exec(e.text);
        if (mine) said(e.who, mine[2], /^\d+/.test(mine[1]) ? parseInt(mine[1], 10) : null);
      });
      // each sentence's votes and whether it won, from the votes read above and the place's voice
      g.rounds.forEach(function (r) {
        r.lines.forEach(function (l) {
          l.votes = g.votes.filter(function (v) { return v.place === r.zero && v.to === l.name; }).length;
          l.won = r.winners.indexOf(l.name) >= 0;
        });
        delete r.winners;
      });
      g.people.forEach(function (n) { if (Z.PERSONAS.indexOf(n) >= 0) g.computer[n] = true; });
      return g;
    });
  }

  // Every game, newest first.
  function games(tree, entries) {
    return tableGames(entries).then(function (tg) {
      var covered = {};
      tg.forEach(function (g) { g.hands.forEach(function (d) { covered[d] = true; }); });
      var all = tg.concat(earlierGames(tree, entries, covered));
      all.sort(function (a, b) {
        if (!a.stamp !== !b.stamp) return a.stamp ? -1 : 1;
        return a.stamp < b.stamp ? 1 : a.stamp > b.stamp ? -1 : 0;
      });
      freshen(tree, all);
      return all;
    });
  }

  // Words new to the tree (spine:zero 4.91): the words a game put into the
  // tree that nobody had played anywhere in it before that game began. A
  // place in the tree belongs to the game that wrote its word there (a first
  // word, to the earliest game dealt at it); what stood before a game is
  // every place belonging to no game or to an earlier one. Case and singular
  // form are ignored, as for the word chips. Each word is listed once, where
  // the game first played it.
  function earlierThan(a, b) { return !a.stamp || !b.stamp ? !a.stamp && !!b.stamp : a.stamp < b.stamp; }
  function freshen(tree, gs) {
    var owner = {}, seat = {};
    gs.forEach(function (g, i) {
      (g.rounds || []).forEach(function (r) {
        r.words.forEach(function (w) { if (w.kept && w.seat) { var d = r.place + w.seat; if (owner[d] == null) { owner[d] = i; seat[d] = true; } } });
      });
    });
    gs.forEach(function (g, i) {
      (g.rounds || []).forEach(function (r) {
        if (seat[r.place]) return;
        if (owner[r.place] == null || earlierThan(g, gs[owner[r.place]])) owner[r.place] = i;
      });
    });
    var places = [];
    (function walk(node, digits) {
      for (var k = 1; k <= 9; k++) {
        var n = node && typeof node === 'object' ? node[String(k)] : null;
        if (n == null) continue;
        var w = Z.readPlace(n).word;
        if (w) places.push({ digits: digits + k, word: w });
        walk(n, digits + k);
      }
    })(tree, '');
    gs.forEach(function (g, i) {
      var before = places.filter(function (p) { var o = owner[p.digits]; return o == null || (o !== i && earlierThan(gs[o], g)); });
      var listed = [];
      g.fresh = [];
      (g.rounds || []).forEach(function (r) {
        r.words.forEach(function (w) {
          if (!w.kept) return;
          if (listed.some(function (x) { return Z.same(x, w.word); })) return;
          if (before.some(function (p) { return Z.same(p.word, w.word); })) return;
          listed.push(w.word);
          g.fresh.push({ word: w.word, name: w.name, place: r.zero });
        });
      });
    });
  }

  // Across all games under each first word (shown once a first word has two
  // games or more): the words that keep colliding, and the words only one
  // person ever played.
  var ENOUGH = 2;
  function across(tree, gs) {
    var by = {};
    gs.forEach(function (g) { if (g.first) (by[g.first] = by[g.first] || []).push(g); });
    return Object.keys(by).sort().filter(function (d) { return by[d].length >= ENOUGH; }).map(function (d) {
      var words = {};
      by[d].forEach(function (g, gi) {
        g.words.forEach(function (w) {
          var k = Z.fold(w.word);
          var x = words[k] || (words[k] = { word: w.word, plays: 0, collidedIn: {}, names: [] });
          x.plays++;
          if (w.collided) x.collidedIn[gi] = true;
          x.names.push(w.name);
        });
      });
      var list = Object.keys(words).map(function (k) { return words[k]; });
      return {
        digits: d, word: Z.readPlace(tree && tree[d]).word, games: by[d].length,
        collide: list.filter(function (x) { return Object.keys(x.collidedIn).length >= 2; }).map(function (x) { return x.word; }).sort(),
        alone: list.filter(function (x) { return x.plays === 1; }).map(function (x) { return { word: x.word, name: x.names[0] }; }).sort(function (a, b) { return a.word.toLowerCase() < b.word.toLowerCase() ? -1 : 1; })
      };
    });
  }

  // ---- the readings of spine:zero 4.91, worked out from a game ---------------------------
  // Words in a sentence, as they are counted and matched.
  function wordsIn(line) { return String(line || '').match(/[\p{L}\p{N}]+(?:['’-][\p{L}\p{N}]+)*/gu) || []; }
  function tokenSame(token, word) { return Z.same(String(token).replace(/['’]s$/i, ''), word); }
  function mean(list) { return list.length ? list.reduce(function (a, b) { return a + b; }, 0) / list.length : 0; }
  // [{name, n}] counted, most first, then in the order they sat.
  function tally(list, order) {
    var n = {};
    list.forEach(function (x) { n[x] = (n[x] || 0) + 1; });
    return Object.keys(n).sort(function (a, b) { return n[b] - n[a] || order.indexOf(a) - order.indexOf(b); }).map(function (k) { return { name: k, n: n[k] }; });
  }
  function readings(g) {
    var order = uniq(g.people.concat(g.votes.map(function (v) { return v.from; }), g.votes.map(function (v) { return v.to; })));
    var isComp = function (n) { return !!g.computer[n]; };
    var rounds = g.rounds || [];
    var voiced = rounds.filter(function (r) { return r.lines.some(function (l) { return l.won; }); });
    var out = {};

    // People or computer: votes between people and computer players
    var pc = { pp: 0, pc: 0, cp: 0, cc: 0 };
    g.votes.forEach(function (v) { pc[(isComp(v.from) ? 'c' : 'p') + (isComp(v.to) ? 'c' : 'p')]++; });
    pc.people = order.filter(function (n) { return !isComp(n); });
    pc.computers = order.filter(isComp).length;
    out.pc = pc;

    // Who listens to whom: pairs who voted for each other, and who received no votes
    var gave = {};
    g.votes.forEach(function (v) { gave[v.from + '→' + v.to] = true; });
    var pairs = [];
    order.forEach(function (a, i) {
      order.slice(i + 1).forEach(function (b) { if (gave[a + '→' + b] && gave[b + '→' + a]) pairs.push([a, b]); });
    });
    out.listens = { pairs: pairs, unvoted: order.filter(function (n) { return !g.votes.some(function (v) { return v.to === n; }); }) };

    // Bold or careful: each player's stakes beside how their sentences did
    out.bold = order.map(function (n) {
      var mine = [];
      voiced.forEach(function (r) { r.lines.forEach(function (l) { if (l.name === n) mine.push(l); }); });
      var stakes = mine.map(function (l) { return l.stake; });
      return { name: n, stakes: stakes, votes: mine.map(function (l) { return l.votes; }), won: mine.filter(function (l) { return l.won; }).length,
        known: stakes.every(function (s) { return typeof s === 'number'; }) };
    }).filter(function (b) { return b.stakes.length; });

    // The table's eye: whose words were marked as not belonging, and who marked most
    var eyeRounds = rounds.filter(function (r) { return r.eye; });
    var marked = [];
    eyeRounds.forEach(function (r, ri) {
      r.marks.forEach(function (m) {
        var x = marked.filter(function (y) { return y.ri === ri && y.name === m.to; })[0];
        if (!x) marked.push(x = { word: m.word, name: m.to, place: r.zero, n: 0, out: m.out, ri: ri });
        x.n++;
      });
    });
    // hand by hand, the most marked first
    marked.sort(function (a, b) { return a.ri - b.ri || b.n - a.n; });
    var markers = tally([].concat.apply([], eyeRounds.map(function (r) { return r.marks.map(function (m) { return m.from; }); })), order);
    out.eye = { hands: eyeRounds.length, of: rounds.length, marked: marked, markers: markers,
      most: markers.filter(function (m) { return m.n === (markers[0] || {}).n; }).map(function (m) { return m.name; }) };

    // Short wins: words in the winning sentences beside the rest, and each player's average
    var won = [], rest = [], by = {};
    voiced.forEach(function (r) {
      r.lines.forEach(function (l) {
        var n = wordsIn(l.line).length;
        (l.won ? won : rest).push(n);
        (by[l.name] = by[l.name] || []).push(n);
      });
    });
    out.short = { won: mean(won), rest: mean(rest), wonN: won.length, restN: rest.length,
      each: order.filter(function (n) { return by[n]; }).map(function (n) { return { name: n, avg: mean(by[n]), n: by[n].length }; })
        .sort(function (a, b) { return a.avg - b.avg || order.indexOf(a.name) - order.indexOf(b.name); }) };

    // Words that travelled: a word from one hand that came back in a later
    // hand, as a word or in a sentence. A word chosen as the next zero is the
    // trail, not travel, in the hand it is the centre of.
    var travelled = [];
    rounds.forEach(function (r, i) {
      r.words.forEach(function (w) {
        if (travelled.some(function (x) { return Z.same(x.word, w.word); })) return;
        var back = [];
        for (var j = i + 1; j < rounds.length; j++) {
          var q = rounds[j];
          if (Z.same(q.zero, w.word)) continue;
          var asWord = uniq(q.words.filter(function (x) { return Z.same(x.word, w.word); }).map(function (x) { return x.name; }));
          var inLines = q.lines.filter(function (l) { return wordsIn(l.line).some(function (tk) { return tokenSame(tk, w.word); }); }).map(function (l) { return l.name; });
          if (asWord.length || inLines.length) back.push({ place: q.zero, words: asWord, lines: inLines });
        }
        if (back.length) {
          travelled.push({ word: w.word, from: r.zero, names: uniq(r.words.filter(function (x) { return Z.same(x.word, w.word); }).map(function (x) { return x.name; })), back: back });
        }
      });
    });
    out.travelled = travelled;

    // Where the trail went: the centre words, each in its seat beneath the one before
    var trail = [];
    rounds.forEach(function (r) { if (!trail.length || trail[trail.length - 1].digits !== r.place) trail.push({ word: r.zero, digits: r.place, seat: Number(r.place.slice(-1)) }); });
    out.trail = trail;
    return out;
  }

  // ---- drawing ---------------------------------------------------------------------------------
  function el(tag, cls, text) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text != null) n.textContent = text;
    return n;
  }
  function cap(w) { w = String(w || ''); return w.charAt(0).toUpperCase() + w.slice(1); }
  function dateWords(stamp) {
    var d = new Date(stamp);
    return isNaN(d) ? '' : d.toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' });
  }
  function names(list) { return list.length <= 1 ? list.join('') : list.slice(0, -1).join(', ') + ' and ' + list[list.length - 1]; }

  // The tree: each played place a crimson point, its winning sentence on tap,
  // its nine seats as small dots, open seats hollow.
  function treeView(tree) {
    var box = el('div', 'look-tree');
    var tops = 0;
    for (var i = 1; i <= 9; i++) if (tree && tree[String(i)] != null) tops++;
    box.appendChild(el('p', 'look-note', tops + ' of nine first words'));
    var ul = el('ul', 'tree');
    for (var k = 1; k <= 9; k++) if (tree && tree[String(k)] != null) ul.appendChild(placeItem(tree[String(k)], String(k)));
    box.appendChild(ul);
    return box;
  }
  function placeItem(node, digits) {
    var p = Z.readPlace(node), played = Z.played(node);
    var li = el('li', played ? 'tp played' : 'tp');
    li.setAttribute('data-place', digits);
    var row = el('div', 'tp-row');
    var btn = el('button', 'tp-word');
    btn.type = 'button';
    btn.appendChild(el('span', played ? 'pt' : 'pt open', ''));
    btn.appendChild(el('span', 'w', p.word));
    btn.setAttribute('aria-expanded', 'false');
    row.appendChild(btn);
    var detail = el('div', 'tp-detail');
    detail.hidden = true;
    var showDetail = function (on) { detail.hidden = !on; btn.setAttribute('aria-expanded', String(on)); };
    if (played) {
      var dots = el('span', 'dots');
      dots.setAttribute('role', 'group');
      dots.setAttribute('aria-label', 'Seats beneath ' + p.word);
      for (var i = 1; i <= 9; i++) {
        var child = typeof node === 'object' ? node[String(i)] : null;
        var cp = child != null ? Z.readPlace(child) : null;
        var d = el('button', child != null ? 'dot' : 'dot open');
        d.type = 'button';
        d.setAttribute('aria-label', cp ? 'seat ' + i + ': ' + cp.word + (cp.by ? ', ' + cp.by : '') : 'seat ' + i + ': open');
        (function (cp, i) {
          d.addEventListener('click', function () {
            seatLine.textContent = cp ? cp.word + (cp.by ? ' · ' + cp.by : '') : 'seat ' + i + ' is open';
            showDetail(true);
          });
        })(cp, i);
        dots.appendChild(d);
      }
      row.appendChild(dots);
      voiceParts(p.voice).forEach(function (v) {
        var s = el('p', 'tp-sentence', v.line);
        detail.appendChild(s);
        if (v.by) detail.appendChild(el('p', 'tp-by', v.by + (v.votes ? ', ' + v.votes + (v.votes === 1 ? ' vote' : ' votes') : '')));
      });
    } else {
      detail.appendChild(el('p', 'tp-by', (p.by ? p.word + ' · ' + p.by + '. ' : '') + 'Nine seats open.'));
    }
    var seatLine = el('p', 'tp-seat', '');
    detail.appendChild(seatLine);
    btn.addEventListener('click', function () { seatLine.textContent = ''; showDetail(detail.hidden); });
    li.appendChild(row);
    li.appendChild(detail);
    if (played) {
      var sub = el('ul', 'tree');
      for (var k = 1; k <= 9; k++) {
        var c = node[String(k)];
        if (c != null && Z.played(c)) sub.appendChild(placeItem(c, digits + k));
      }
      if (sub.childNodes.length) li.appendChild(sub);
    }
    return li;
  }

  // Who voted for whom, drawn as a web (spine:zero 4.91): the full width of
  // the sheet and about as tall as it is wide, each name round a circle, an
  // arrow from each voter to the one they voted for, heavier for more votes.
  // Tapping a name lights only the arrows to and from that player, with a
  // line beneath saying whom they voted for and who voted for them.
  var NS = 'http://www.w3.org/2000/svg';
  var drawn = 0;     // every drawing's markers get their own ids
  function svg(tag, attrs) {
    var n = document.createElementNS(NS, tag);
    Object.keys(attrs || {}).forEach(function (k) { n.setAttribute(k, attrs[k]); });
    return n;
  }
  function either(list) { return list.length <= 1 ? list.join('') : list.slice(0, -1).join(', ') + ' or ' + list[list.length - 1]; }
  function times(x) { return x.name + (x.n > 1 ? ' (' + (x.n === 2 ? 'twice' : x.n + ' times') + ')' : ''); }
  function votedLine(p, votes, order) {
    var gave = tally(votes.filter(function (v) { return v.from === p; }).map(function (v) { return v.to; }), order);
    var got = tally(votes.filter(function (v) { return v.to === p; }).map(function (v) { return v.from; }), order);
    return (gave.length ? p + ' voted for ' + names(gave.map(times)) + '.' : p + ' gave no vote.') + ' ' +
      (got.length ? names(got.map(times)) + ' voted for ' + p + '.' : 'Nobody voted for ' + p + '.');
  }
  var HINT = 'Tap a name to light only their votes.';
  function web(g, line) {
    var people = uniq(g.people.concat(g.votes.map(function (v) { return v.from; }), g.votes.map(function (v) { return v.to; })));
    var n = people.length, W = 400, CX = 200, CY = 200, R = n > 1 ? 140 : 0;
    var pos = {}, received = {}, edges = {};
    people.forEach(function (p, i) {
      var a = -Math.PI / 2 + i * 2 * Math.PI / Math.max(n, 1);
      pos[p] = { x: CX + R * Math.cos(a), y: CY + R * Math.sin(a), a: a };
      received[p] = 0;
    });
    g.votes.forEach(function (v) { received[v.to]++; var k = v.from + '→' + v.to; edges[k] = (edges[k] || 0) + 1; });
    var top = Math.max.apply(null, people.map(function (p) { return received[p]; }).concat([0]));
    var s = svg('svg', { viewBox: '0 0 ' + W + ' 400', class: 'web', role: 'group', 'aria-label': 'Who voted for whom. Tap a name to light their votes.' });
    var id = 'web' + (++drawn);
    var defs = svg('defs');
    [['', 'rgba(42,36,32,0.55)'], ['-out', '#8e1b1b'], ['-in', '#2a2420']].forEach(function (m) {
      var mk = svg('marker', { id: id + m[0], viewBox: '0 0 10 10', refX: '9', refY: '5', markerWidth: '6', markerHeight: '6', orient: 'auto-start-reverse' });
      mk.appendChild(svg('path', { d: 'M0,1 L9,5 L0,9 z', fill: m[1] }));
      defs.appendChild(mk);
    });
    s.appendChild(defs);
    var arrows = [];
    Object.keys(edges).forEach(function (k) {
      var parts = k.split('→'), a = pos[parts[0]], b = pos[parts[1]];
      if (!a || !b) return;
      var dx = b.x - a.x, dy = b.y - a.y, len = Math.sqrt(dx * dx + dy * dy) || 1;
      var ux = dx / len, uy = dy / len;
      var x1 = a.x + ux * 10, y1 = a.y + uy * 10, x2 = b.x - ux * 12, y2 = b.y - uy * 12;
      // a slight bow, so a vote each way between two people draws two arcs
      var mx = (x1 + x2) / 2 - uy * 14, my = (y1 + y2) / 2 + ux * 14;
      var path = svg('path', { d: 'M' + x1.toFixed(1) + ',' + y1.toFixed(1) + ' Q' + mx.toFixed(1) + ',' + my.toFixed(1) + ' ' + x2.toFixed(1) + ',' + y2.toFixed(1),
        fill: 'none', stroke: 'rgba(42,36,32,0.42)', 'stroke-width': (1 + 1 * (edges[k] - 1)).toFixed(1), 'marker-end': 'url(#' + id + ')', class: 'edge' });
      path.setAttribute('data-from', parts[0]);
      path.setAttribute('data-to', parts[1]);
      s.appendChild(path);
      arrows.push(path);
    });
    var whos = [];
    people.forEach(function (p) {
      var o = pos[p], lead = top && received[p] === top;
      var grp = svg('g', { class: 'wn', tabindex: '0', role: 'button', 'aria-pressed': 'false' });
      grp.setAttribute('data-who', p);
      grp.setAttribute('aria-label', p + ': whom they voted for, and who voted for them');
      grp.appendChild(svg('circle', { cx: o.x.toFixed(1), cy: o.y.toFixed(1), r: 24, class: 'wn-hit' }));
      grp.appendChild(svg('circle', { cx: o.x.toFixed(1), cy: o.y.toFixed(1), r: lead ? 6 : 4.5, class: lead ? 'wn-pt lead' : 'wn-pt' }));
      // the name above its point (below it on the lower half), kept inside the sheet
      var half = p.length * 4.9 + 4;
      var lx = Math.min(W - half - 2, Math.max(half + 2, o.x)), ly = Math.sin(o.a) < 0.2 ? o.y - 13 : o.y + 28;
      var t = svg('text', { x: lx.toFixed(1), y: ly.toFixed(1), 'text-anchor': 'middle', class: lead ? 'lead' : '' });
      t.textContent = p;
      grp.appendChild(t);
      s.appendChild(grp);
      whos.push(grp);
    });
    var chosen = null;
    function light(p) {
      chosen = p === chosen ? null : p;
      s.classList.toggle('lit', !!chosen);
      arrows.forEach(function (e) {
        var how = !chosen ? '' : e.getAttribute('data-from') === chosen ? 'out' : e.getAttribute('data-to') === chosen ? 'in' : 'dim';
        e.setAttribute('class', how ? 'edge ' + how : 'edge');
        e.setAttribute('marker-end', 'url(#' + id + (how === 'out' ? '-out' : how === 'in' ? '-in' : '') + ')');
      });
      whos.forEach(function (w) {
        var me = w.getAttribute('data-who');
        var near = chosen && g.votes.some(function (v) { return (v.from === chosen && v.to === me) || (v.to === chosen && v.from === me); });
        w.setAttribute('class', 'wn' + (me === chosen ? ' on' : near ? ' near' : ''));
        w.setAttribute('aria-pressed', String(me === chosen));
      });
      if (line) line.textContent = chosen ? votedLine(chosen, g.votes, people) : HINT;
    }
    whos.forEach(function (w) {
      w.addEventListener('click', function (e) { e.stopPropagation(); light(w.getAttribute('data-who')); });
      w.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); light(w.getAttribute('data-who')); }
      });
    });
    // a tap anywhere else on the web puts every arrow back
    s.addEventListener('click', function () { if (chosen) light(chosen); });
    return { svg: s, received: received, people: people };
  }

  // Where the trail went (4.91): each centre word in the row of nine seats
  // beneath the one before, joined as a path down the sheet.
  function trailMap(trail) {
    var X = function (k) { return 40 + (k - 1) * 40; }, Y = function (i) { return 22 + i * 46; };
    var H = Y(trail.length - 1) + 22;
    var s = svg('svg', { viewBox: '0 0 400 ' + H, class: 'trail-map', role: 'img', 'aria-label': 'The trail: ' + trail.map(function (t) { return t.word; }).join(', ') });
    trail.forEach(function (t, i) {
      for (var k = 1; k <= 9; k++) s.appendChild(svg('circle', { cx: X(k), cy: Y(i), r: 1.8, class: 'tm-seat' }));
    });
    var d = 'M' + X(trail[0].seat) + ',' + Y(0);
    for (var i = 1; i < trail.length; i++) {
      d += ' C' + X(trail[i - 1].seat) + ',' + (Y(i - 1) + 23) + ' ' + X(trail[i].seat) + ',' + (Y(i) - 23) + ' ' + X(trail[i].seat) + ',' + Y(i);
    }
    if (trail.length > 1) s.appendChild(svg('path', { d: d, class: 'trail-path', fill: 'none' }));
    trail.forEach(function (t, i) {
      s.appendChild(svg('circle', { cx: X(t.seat), cy: Y(i), r: 4.5, class: i === trail.length - 1 ? 'tm-stop last' : 'tm-stop' }));
      var right = t.seat <= 5;
      var label = svg('text', { x: X(t.seat) + (right ? 11 : -11), y: Y(i) + 5, 'text-anchor': right ? 'start' : 'end' });
      label.textContent = t.word;
      s.appendChild(label);
    });
    return s;
  }

  // A game's poem card in Look around. Folded to its head (the date, the
  // trail, who sat) until tapped, so a long list of games stays short on a
  // phone; what is inside is drawn the first time it is opened.
  function card(g, open) {
    var box = el('details', 'poem look-card');
    box.setAttribute('data-game', g.code || g.hands[0]);
    var when = dateWords(g.stamp);
    var sum = el('summary', 'card-sum');
    sum.appendChild(el('span', 'top', 'a game of Zero' + (when ? ' · ' + when : '')));
    sum.appendChild(el('span', 'path', uniq(g.places.map(function (p) { return cap(p.word); })).join(' · ')));
    var top = g.book && g.book.length ? highest(g.book) : null;
    sum.appendChild(el('span', 'card-take', g.people.length + ' at the table' + (!top ? '' : top.names.length > 2 ? ' · ' + top.names.length + ' tied highest' : ' · ' + names(top.names) + ' finished highest')));
    box.appendChild(sum);
    var built = false;
    function build() {
      if (built) return;
      built = true;
      var body = el('div', 'card-body');
      g.places.forEach(function (p) {
        p.lines.forEach(function (l, i) {
          var d = el('div', 'place');
          if (!i) d.appendChild(el('div', 'pword', p.word));
          d.appendChild(el('div', 'pline', l.line));
          if (l.name) d.appendChild(el('div', 'pby', l.name));
          body.appendChild(d);
        });
      });
      body.appendChild(el('p', 'sat', g.people.join(' · ')));
      if (g.titles && g.titles.length) {
        var tl = el('ul', 'titles');
        g.titles.forEach(function (x) {
          var li = el('li');
          li.appendChild(el('span', 'tt', x.title));
          li.appendChild(el('span', 'tn', names(x.names)));
          li.appendChild(el('span', 'tw', x.note));
          tl.appendChild(li);
        });
        body.appendChild(tl);
      }
      showsPanel(body, g);
      box.appendChild(body);
    }
    // drawn on the tap itself, so the card opens with its poem already in it
    sum.addEventListener('click', build);
    box.addEventListener('toggle', function () { if (box.open) build(); });
    if (open) { box.open = true; build(); }
    return box;
  }
  // 'What this game shows', open by default and folded with a tap: the same
  // readings in Look around and at the end of a game (spine:zero 4.9, 4.62, 4.91).
  function showsPanel(box, g) {
    var toggle = el('button', 'quiet look-open', 'What this game shows');
    toggle.type = 'button';
    toggle.setAttribute('aria-expanded', 'true');
    var shows = el('div', 'look-shows');
    fillShows(shows, g);
    toggle.addEventListener('click', function () {
      shows.hidden = !shows.hidden;
      toggle.setAttribute('aria-expanded', String(!shows.hidden));
    });
    box.appendChild(toggle);
    box.appendChild(shows);
    return shows;
  }
  function say(box, text, cls) { box.appendChild(el('p', 'look-note' + (cls ? ' ' + cls : ''), text)); }
  function num1(x) { return String(Math.round(x * 10) / 10); }
  function votesWord(n) { return n + (n === 1 ? ' vote' : ' votes'); }
  function signed(n) { return n < 0 ? '−' + (-n) : String(n); }
  // Who finished with most chips: {names, total}.
  function highest(book) {
    var max = Math.max.apply(null, book.map(function (b) { return b.total; }));
    return { names: book.filter(function (b) { return b.total === max; }).map(function (b) { return b.name; }), total: max };
  }
  // The words a game put into the tree, each once (case and singular form ignored).
  function keptWords(g) {
    var kept = [];
    (g.rounds || []).forEach(function (r) { r.words.forEach(function (w) { if (w.kept && !kept.some(function (k) { return Z.same(k, w.word); })) kept.push(w.word); }); });
    return kept;
  }

  // ---- each reading in one plain line ----------------------------------------------------------
  // What a reading says while it is folded, and whether there is more to
  // open beneath it: {key: {text, more}}. Pure, so it is checked in Node.
  function takes(g, R) {
    R = R || readings(g);
    var order = uniq(g.people.concat(g.votes.map(function (v) { return v.from; }), g.votes.map(function (v) { return v.to; })));
    var K = {};
    // who voted for whom
    var got = tally(g.votes.map(function (v) { return v.to; }), order);
    if (!got.length) K.votes = { text: 'No votes are kept for this game.', more: false };
    else {
      var most = got.filter(function (x) { return x.n === got[0].n; }).map(function (x) { return x.name; });
      K.votes = { text: names(most) + ' drew the most votes: ' + got[0].n + (most.length > 1 ? ' each' : '') + ' of ' + g.votes.length + '.', more: true };
    }
    // where two players thought the same
    var alike = uniq(g.same.map(function (x) { return x.words[0]; }));
    K.same = alike.length ? { text: 'Thought alike on ' + names(alike) + '.', more: true } : { text: 'Nobody thought the same.', more: false };
    // words new to the tree
    var kept = keptWords(g);
    if (g.fresh === undefined) K.fresh = { text: 'Reading the tree…', more: false };
    else if (g.fresh === null) K.fresh = { text: 'The tree could not be read just now.', more: false };
    else if (!g.fresh.length) K.fresh = { text: kept.length ? 'Every word this game put into the tree had been played in it before.' : 'This game put no word into the tree.', more: false };
    else K.fresh = { text: (g.fresh.length === kept.length ? 'All ' + kept.length : g.fresh.length + ' of the ' + kept.length) + ' words this game put into the tree were new to it.', more: true };
    // sent out, and chips (a game played on the page keeps both)
    if (g.kind === 'table') {
      var outs = g.sentOut.map(function (x) { return x.word; });
      K.sent = outs.length ? { text: names(outs) + (outs.length === 1 ? ' was' : ' were') + ' sent out.', more: true } : { text: 'The table sent no word out.', more: false };
      var top = highest(g.book);
      K.chips = { text: names(top.names) + ' finished highest, with ' + top.total + (top.total === 1 ? ' chip' : ' chips') + (top.names.length > 1 ? ' each' : '') + '.', more: true };
    }
    // people or computer: the whole reading is its line
    var pc = R.pc, one = pc.people.length === 1 ? pc.people[0] : '';
    K.pc = { more: false, text: !g.votes.length ? 'No votes are kept for this game.'
      : !pc.computers ? 'Everyone at this table was a person: all ' + votesWord(pc.pp) + ' went from people to people.'
        : !pc.people.length ? 'Every seat was a computer player: all ' + votesWord(pc.cc) + ' went between them.'
          : one ? one + ', the only person at the table, gave ' + votesWord(pc.pc) + ' to computer players. Computer players gave ' + one + ' ' + pc.cp + ' of their ' + votesWord(pc.cp + pc.cc) + '.'
            : 'People gave ' + votesWord(pc.pp) + ' to people and ' + pc.pc + ' to computer players. Computer players gave ' + pc.cp + ' of their ' + votesWord(pc.cp + pc.cc) + ' to people.' };
    // who listens to whom
    var pairs = R.listens.pairs, none = R.listens.unvoted;
    var unvoted = none.length ? 'Nobody voted for ' + either(none) + '.' : 'Everyone received a vote.';
    K.listens = !g.votes.length ? { text: 'No votes are kept for this game.', more: false }
      : { more: pairs.length > 1, text: (!pairs.length ? 'No two players voted for each other.' : pairs.length === 1 ? pairs[0].join(' and ') + ' voted for each other.' : pairs.length + ' pairs voted for each other.') + ' ' + unvoted };
    // bold or careful
    var known = R.bold.filter(function (b) { return b.known; });
    if (!R.bold.length) K.bold = { text: 'No stakes are kept for this game.', more: false };
    else if (known.length < 2) K.bold = { text: 'Each player’s stakes, beside the votes their sentences drew.', more: true };
    else {
      var avg = function (b) { return mean(b.stakes); };
      var hi = Math.max.apply(null, known.map(avg)), lo = Math.min.apply(null, known.map(avg));
      var at = function (v) { return known.filter(function (b) { return avg(b) === v; }).map(function (b) { return b.name; }); };
      K.bold = { more: true, text: hi === lo ? 'Everyone staked about the same: ' + num1(hi) + ' a sentence.'
        : 'Boldest: ' + names(at(hi)) + ', staking ' + num1(hi) + ' a sentence. Most careful: ' + names(at(lo)) + ', ' + num1(lo) + '.' };
    }
    // the table's eye
    var eye = R.eye;
    if (!eye.hands) K.eye = { text: g.kind === 'table' ? 'This table played before the table’s eye.' : 'Played in chat, before the table’s eye.', more: false };
    else if (!eye.marked.length) K.eye = { text: 'Nobody marked a word as not belonging.', more: false };
    else {
      K.eye = { more: true, text: (eye.marked.length === 1 ? 'One word was' : eye.marked.length + ' words were') + ' marked as not belonging; ' + names(eye.most) + ' marked most.' };
    }
    // short wins
    var sh = R.short;
    K.short = !sh.wonN ? { text: 'No sentences are kept for this game.', more: false }
      : { more: true, text: sh.restN ? 'Winning sentences averaged ' + num1(sh.won) + ' words; the rest, ' + num1(sh.rest) + '.' : 'Every sentence won, with ' + num1(sh.won) + ' words on average.' };
    // words that travelled
    var tw = R.travelled.map(function (x) { return x.word; });
    K.travelled = !tw.length ? { text: 'No word from one hand came back in a later one.', more: false }
      : { more: true, text: tw.length > 3 ? tw.length + ' words came back in later hands, ' + names(tw.slice(0, 3)) + ' among them.'
        : names(tw) + (tw.length === 1 && R.travelled[0].back.length === 1 ? ' came back in a later hand.' : ' came back in later hands.') };
    // the thread: who picked up a word from the poem so far (a table that pays it)
    if (g.threaded) {
      var picked = [];
      (g.rounds || []).forEach(function (r) { r.lines.forEach(function (l) { if (l.thread) picked.push(l.name); }); });
      var keepers = tally(picked, order);
      K.thread = !picked.length ? { text: 'No sentence picked up a word from the poem before it.', more: false }
        : { more: true, text: (picked.length === 1 ? 'One sentence' : picked.length + ' sentences') + ' picked up a word from the poem before ' + (picked.length === 1 ? 'it' : 'them') + '; ' +
          names(keepers.filter(function (x) { return x.n === keepers[0].n; }).map(function (x) { return x.name; })) + ' most.' };
    }
    // where the trail went
    K.trail = R.trail.length > 1 ? { text: R.trail.map(function (x) { return cap(x.word); }).join(' › '), more: true }
      : { text: R.trail.length ? 'The game stayed at ' + R.trail[0].word + '.' : 'No trail is kept for this game.', more: false };
    return K;
  }

  // ---- the readings as rows that open -----------------------------------------------------------
  // One reading: its heading and its one plain line, always in view; where
  // there is more, the row opens on a tap to show it (an expansion box).
  function reading(key, title, take, fill, open) {
    var sec = el(take.more ? 'details' : 'div', take.more ? 'look-sec' : 'look-sec plain');
    sec.setAttribute('data-reading', key);
    var sum = el(take.more ? 'summary' : 'div', 'look-sum');
    sum.appendChild(el('span', 'look-h', title));
    sum.appendChild(el('span', 'look-take', take.text));
    sec.appendChild(sum);
    if (take.more) {
      var more = el('div', 'look-more');
      fill(more);
      sec.appendChild(more);
      if (open) sec.open = true;
    }
    return sec;
  }
  // [{word, name, place}] as one row a place: "under Gift: seed (Farmer) · Joy (Phenomemental)"
  function byPlaceList(list, cls) {
    var rows = [];
    list.forEach(function (x) {
      var row = rows.filter(function (r) { return r.place === x.place; })[0];
      if (!row) rows.push(row = { place: x.place, words: [] });
      row.words.push(x);
    });
    var ul = el('ul', 'look-list ' + cls);
    rows.forEach(function (r) {
      var li = el('li');
      li.appendChild(el('span', 'lp', 'under ' + r.place + ': '));
      r.words.forEach(function (x, i) {
        if (i) li.appendChild(document.createTextNode(' · '));
        li.appendChild(el(x.out ? 's' : 'span', 'lw', x.word));
        if (x.name) li.appendChild(document.createTextNode(' (' + x.name + (x.tail || '') + ')'));
      });
      ul.appendChild(li);
    });
    return ul;
  }
  // A small table: a name down the side, figures across, so nine players read at a glance.
  function table(cls, heads, rows) {
    var t = el('table', 'look-table ' + cls), head = el('tr');
    heads.forEach(function (h) { var th = el('th', null, h); th.setAttribute('scope', 'col'); head.appendChild(th); });
    var thead = el('thead'), body = el('tbody');
    thead.appendChild(head);
    rows.forEach(function (r) {
      var tr = el('tr');
      r.forEach(function (c, i) {
        var cell = el(i ? 'td' : 'th', null, c);
        if (!i) cell.setAttribute('scope', 'row');
        tr.appendChild(cell);
      });
      body.appendChild(tr);
    });
    t.appendChild(thead);
    t.appendChild(body);
    return t;
  }
  // Words new to the tree: drawn once the tree is read (Look around has it
  // already; the end of a game reads it, then calls setFresh).
  function freshRow(g, K) {
    return reading('fresh', 'Words new to the tree', (K || takes(g)).fresh, function (more) {
      say(more, 'Nobody had played these anywhere in the tree before this game:');
      more.appendChild(byPlaceList(g.fresh, 'fresh'));
    });
  }
  function setFresh(shows, g) {
    var old = shows && shows.querySelector('.look-sec[data-reading="fresh"]');
    if (!old) return;
    var fresh = freshRow(g);
    if (old.open && fresh.tagName === 'DETAILS') fresh.open = true;
    old.replaceWith(fresh);
  }
  function fillShows(box, g) {
    var R = readings(g), K = takes(g, R);
    // one tap opens every reading, or folds them all again
    var bar = el('p', 'shows-bar'), all = el('button', 'quiet shows-all', 'Open all');
    all.type = 'button';
    bar.appendChild(all);
    box.appendChild(bar);
    var boxes = function () { return Array.prototype.slice.call(box.querySelectorAll('details.look-sec')); };
    var anyShut = function () { return boxes().some(function (d) { return !d.open; }); };
    var sync = function () { all.textContent = anyShut() ? 'Open all' : 'Close all'; };
    all.addEventListener('click', function () {
      var to = anyShut();
      boxes().forEach(function (d) { d.open = to; });
      sync();
    });
    box.addEventListener('toggle', sync, true);
    var add = function (key, title, fill, open) { box.appendChild(reading(key, title, K[key], fill, open)); };

    // who voted for whom: the web, open from the start
    add('votes', 'Who voted for whom', function (more) {
      var line = el('p', 'look-note web-line', HINT);
      line.setAttribute('aria-live', 'polite');
      var w = web(g, line);
      more.appendChild(w.svg);
      more.appendChild(line);
      var ranked = w.people.filter(function (p) { return w.received[p]; }).sort(function (a, b) { return w.received[b] - w.received[a]; });
      say(more, 'Votes received: ' + ranked.map(function (p) { return p + ' ' + w.received[p]; }).join(' · ') +
        (w.people.length > ranked.length ? '. None for ' + names(w.people.filter(function (p) { return !w.received[p]; })) + '.' : '.'), 'votes-said');
    }, true);

    add('same', 'Where two players thought the same', function (more) {
      var ul = el('ul', 'look-list same');
      g.same.forEach(function (x) {
        var li = el('li');
        li.appendChild(el('span', 'lw', uniq(x.words).join(' / ')));
        li.appendChild(document.createTextNode(' · ' + names(x.names)));
        li.appendChild(el('span', 'lp', ' under ' + x.place));
        ul.appendChild(li);
      });
      more.appendChild(ul);
    });

    box.appendChild(freshRow(g, K));

    // words the table sent out, and each player's chips (a game played on the page keeps both)
    if (g.kind === 'table') {
      add('sent', 'Words the table sent out', function (more) {
        var so = el('ul', 'look-list sent');
        g.sentOut.forEach(function (x) {
          var li = el('li');
          li.appendChild(el('s', 'lw', x.word));
          li.appendChild(document.createTextNode(' (' + x.name + ')'));
          li.appendChild(el('span', 'lp', ' under ' + x.place));
          so.appendChild(li);
        });
        more.appendChild(so);
      });
      add('chips', 'Chips from words and from sentences', function (more) {
        more.appendChild(table('chips', ['', 'words', 'sentences', 'chips'], g.book.map(function (r) {
          return [r.name, String(r.words), signed(r.sentences), String(r.total)];
        })));
      });
    }

    add('pc', 'People or computer');

    add('listens', 'Who listens to whom', function (more) {
      var ul = el('ul', 'look-pills pairs');
      R.listens.pairs.forEach(function (p) { ul.appendChild(el('li', null, p.join(' and '))); });
      more.appendChild(ul);
    });

    add('bold', 'Bold or careful', function (more) {
      more.appendChild(table('bold', ['', 'staked', 'votes', 'won'], R.bold.map(function (b) {
        return [b.name, b.stakes.map(function (x) { return typeof x === 'number' ? x : '?'; }).join(', '), b.votes.join(', '),
          b.won ? b.won + ' of ' + b.stakes.length : 'none'];
      })));
      say(more, 'Hand by hand: what each player staked on a sentence, and the votes it drew.', 'small');
    });

    add('eye', 'The table’s eye', function (more) {
      var eye = R.eye;
      say(more, 'Words marked as not belonging, and by how many:');
      more.appendChild(byPlaceList(eye.marked.map(function (m) {
        return { word: m.word, name: m.name, place: m.place, out: m.out, tail: ', ' + m.n + (m.n === 1 ? ' mark' : ' marks') + (m.out ? ', sent out' : '') };
      }), 'marked'));
      var topN = eye.markers[0].n;
      say(more, (eye.most.length === 1 ? eye.most[0] + ' marked most: ' + topN + (topN === 1 ? ' word' : ' words') + ' in ' + eye.hands + (eye.hands === 1 ? ' hand.' : ' hands.')
        : names(eye.most) + ' marked most, ' + topN + (topN === 1 ? ' word each.' : ' words each.')));
    });

    // short wins: each player's average as a bar, shortest first
    add('short', 'Short wins', function (more) {
      say(more, 'Words in a sentence, each player’s average:');
      var longest = Math.max.apply(null, R.short.each.map(function (e) { return e.avg; }).concat([1]));
      var ul = el('ul', 'look-bars');
      R.short.each.forEach(function (e) {
        var li = el('li');
        li.appendChild(el('span', 'bn', e.name));
        var bar = el('span', 'bar'), fill = el('i');
        fill.style.width = Math.max(4, Math.round(100 * e.avg / longest)) + '%';
        bar.appendChild(fill);
        li.appendChild(bar);
        li.appendChild(el('span', 'bv', num1(e.avg)));
        ul.appendChild(li);
      });
      more.appendChild(ul);
    });

    add('travelled', 'Words that travelled', function (more) {
      var tl = el('ul', 'look-list travelled');
      R.travelled.forEach(function (x) {
        var li = el('li');
        li.appendChild(el('span', 'lw', x.word));
        li.appendChild(document.createTextNode(' (' + names(x.names) + ')'));
        li.appendChild(el('span', 'lp', ' from ' + x.from));
        li.appendChild(document.createTextNode(': back ' + x.back.map(function (b) {
          var how = [b.words.length ? 'as a word' : '', b.lines.length ? 'in ' + (b.lines.length === 1 ? 'a sentence' : b.lines.length + ' sentences') : ''].filter(Boolean).join(' and ');
          return 'at ' + b.place + ' ' + how;
        }).join('; ') + '.'));
        tl.appendChild(li);
      });
      more.appendChild(tl);
    });

    if (g.threaded) add('thread', 'The thread', function (more) {
      var list = [];
      g.rounds.forEach(function (r) { r.lines.forEach(function (l) { if (l.thread) list.push({ word: l.thread, name: l.name + (l.won ? ', and won' : ''), place: r.zero }); }); });
      say(more, 'A word picked up from the winning sentences before it: 1 chip from the bank each time.');
      more.appendChild(byPlaceList(list, 'threads'));
    });

    add('trail', 'Where the trail went', function (more) {
      more.appendChild(trailMap(R.trail));
      say(more, 'Each word was chosen from the nine seats beneath the one before (the small dots).');
    });
    sync();
  }
  function acrossView(rows) {
    var box = el('div', 'look-across');
    rows.forEach(function (r) {
      var d = el('div', 'across-row');
      d.appendChild(el('p', 'across-head', 'Under ' + cap(r.word) + ', across ' + r.games + ' games'));
      d.appendChild(el('p', 'look-note', r.collide.length ? 'The words that keep colliding: ' + r.collide.join(' · ') + '.' : 'No word has collided in more than one game yet.'));
      if (r.alone.length) {
        var shown = r.alone.slice(0, 30);
        d.appendChild(el('p', 'look-note', 'Only one person ever played: ' + shown.map(function (x) { return x.word; }).join(' · ') + (r.alone.length > shown.length ? ', and ' + (r.alone.length - shown.length) + ' more.' : '.')));
      }
      box.appendChild(d);
    });
    return box;
  }

  // Draw Look around into box: the tree, the games, and across all games.
  function render(box, tree, gs) {
    box.textContent = '';
    box.appendChild(el('h2', 'look-head', 'The tree'));
    box.appendChild(treeView(tree));
    box.appendChild(el('h2', 'look-head', 'Games played'));
    if (!gs.length) box.appendChild(el('p', 'look-note', 'No game has been played yet.'));
    // the newest game open, the rest folded to their heads
    gs.forEach(function (g, i) { box.appendChild(card(g, i === 0)); });
    var rows = across(tree, gs);
    if (rows.length) {
      box.appendChild(el('h2', 'look-head', 'Across all games'));
      box.appendChild(acrossView(rows));
    }
  }

  var api = { voiceParts: voiceParts, voicedPlaces: voicedPlaces, games: games, across: across, render: render, ENOUGH: ENOUGH,
    tableGame: tableGame, tables: tables, showsPanel: showsPanel, setFresh: setFresh, readings: readings, takes: takes, wordsIn: wordsIn };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.ZeroLook = api;
})(typeof self !== 'undefined' ? self : this);
