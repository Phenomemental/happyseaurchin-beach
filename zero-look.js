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
    var g = {
      kind: 'table', code: t.code, stamp: t.open.stamp || '', people: Object.keys(t.roster).map(Number).sort(function (a, b) { return a - b; }).map(nameOf),
      places: Z.poem(t), votes: [], same: [], alone: [], sentOut: [], words: [], hands: []
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
    });
    g.first = (g.hands[0] || '')[0] || '';
    return g;
  }
  function tableGames(entries) {
    var byCode = {};
    entries.forEach(function (e) {
      var k = Z.codeKey(e.code);
      if (!k || !e.kind) return;
      (byCode[k] = byCode[k] || []).push(e);
    });
    return Promise.all(Object.keys(byCode).map(function (k) {
      var list = byCode[k];
      if (!list.some(function (e) { return e.kind === 'open'; }) || !list.some(function (e) { return e.kind === 'deal'; })) return null;
      return Z.derive(list, list[0].code).then(function (t) {
        if (!t.roster || !t.hands.some(function (h) { return h.winners.length; })) return null;
        return tableGame(t);
      });
    })).then(function (gs) { return gs.filter(Boolean); });
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
      var g = { kind: 'earlier', code: '', stamp: '', people: [], places: [], votes: [], same: [], alone: [], sentOut: [], words: [], hands: chain.slice(), first: chain[0][0] };
      var people = [], wordOf = {}, seatOf = {};
      chain.forEach(function (d) {
        var node = Z.nodeAt(tree, d), rp = Z.readPlace(node);
        wordOf[d] = rp.word;
        var lines = voiceParts(rp.voice).map(function (v) { return { line: v.line, name: v.by, votes: v.votes }; });
        g.places.push({ word: rp.word, digits: d, lines: lines });
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
        var fre = /Flop: (\S+) from both ([^,.;]+?) and ([^,.;]+?)(?=[,.;])/g, f;
        while ((f = fre.exec(e.text))) {
          g.same.push({ word: f[1], words: [f[1], f[1]], names: [f[2].trim(), f[3].trim()], place: place });
          g.words.push({ word: f[1], name: f[2].trim(), place: place, collided: true }, { word: f[1], name: f[3].trim(), place: place, collided: true });
        }
      });
      return g;
    });
  }

  // Every game, newest first.
  function games(tree, entries) {
    return tableGames(entries).then(function (tg) {
      var covered = {};
      tg.forEach(function (g) { g.hands.forEach(function (d) { covered[d] = true; }); });
      var all = tg.concat(earlierGames(tree, entries, covered));
      return all.sort(function (a, b) {
        if (!a.stamp !== !b.stamp) return a.stamp ? -1 : 1;
        return a.stamp < b.stamp ? 1 : a.stamp > b.stamp ? -1 : 0;
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

  // Who voted for whom, drawn as a web: each name round a circle, an arrow
  // from each voter to the one they voted for, heavier for more votes.
  var NS = 'http://www.w3.org/2000/svg';
  function svg(tag, attrs) {
    var n = document.createElementNS(NS, tag);
    Object.keys(attrs || {}).forEach(function (k) { n.setAttribute(k, attrs[k]); });
    return n;
  }
  function web(g) {
    var people = uniq(g.people.concat(g.votes.map(function (v) { return v.from; }), g.votes.map(function (v) { return v.to; })));
    // wider than tall, so the longest name fits on either side of a phone
    var n = people.length, CX = 220, C = 170, R = 110;
    var pos = {}, received = {}, edges = {};
    people.forEach(function (p, i) {
      var a = -Math.PI / 2 + i * 2 * Math.PI / Math.max(n, 1);
      pos[p] = { x: CX + R * Math.cos(a), y: C + R * Math.sin(a), a: a };
      received[p] = 0;
    });
    g.votes.forEach(function (v) { received[v.to]++; var k = v.from + '→' + v.to; edges[k] = (edges[k] || 0) + 1; });
    var top = Math.max.apply(null, people.map(function (p) { return received[p]; }).concat([0]));
    var s = svg('svg', { viewBox: '0 0 440 340', class: 'web', role: 'img', 'aria-label': 'Who voted for whom' });
    var arrow = 'arrow-' + String(g.code || g.hands[0]).replace(/[^A-Za-z0-9]/g, '');
    var defs = svg('defs');
    var mk = svg('marker', { id: arrow, viewBox: '0 0 10 10', refX: '9', refY: '5', markerWidth: '6', markerHeight: '6', orient: 'auto-start-reverse' });
    mk.appendChild(svg('path', { d: 'M0,1 L9,5 L0,9 z', fill: 'rgba(42,36,32,0.55)' }));
    defs.appendChild(mk);
    s.appendChild(defs);
    Object.keys(edges).forEach(function (k) {
      var parts = k.split('→'), a = pos[parts[0]], b = pos[parts[1]];
      if (!a || !b) return;
      var dx = b.x - a.x, dy = b.y - a.y, len = Math.sqrt(dx * dx + dy * dy) || 1;
      var ux = dx / len, uy = dy / len;
      var x1 = a.x + ux * 9, y1 = a.y + uy * 9, x2 = b.x - ux * 11, y2 = b.y - uy * 11;
      // a slight bow, so a vote each way between two people draws two arcs
      var mx = (x1 + x2) / 2 - uy * 12, my = (y1 + y2) / 2 + ux * 12;
      s.appendChild(svg('path', { d: 'M' + x1.toFixed(1) + ',' + y1.toFixed(1) + ' Q' + mx.toFixed(1) + ',' + my.toFixed(1) + ' ' + x2.toFixed(1) + ',' + y2.toFixed(1),
        fill: 'none', stroke: 'rgba(42,36,32,0.42)', 'stroke-width': (0.9 + 0.9 * (edges[k] - 1)).toFixed(1), 'marker-end': 'url(#' + arrow + ')', class: 'edge' }));
    });
    people.forEach(function (p) {
      var o = pos[p], lead = top && received[p] === top;
      s.appendChild(svg('circle', { cx: o.x.toFixed(1), cy: o.y.toFixed(1), r: lead ? 5 : 3.5, fill: lead ? '#8e1b1b' : '#2a2420' }));
      var lx = CX + (R + 14) * Math.cos(o.a), ly = C + (R + 14) * Math.sin(o.a);
      var anchor = Math.abs(Math.cos(o.a)) < 0.3 ? 'middle' : Math.cos(o.a) > 0 ? 'start' : 'end';
      var t = svg('text', { x: lx.toFixed(1), y: (ly + 4).toFixed(1), 'text-anchor': anchor, class: lead ? 'lead' : '' });
      t.textContent = p;
      s.appendChild(t);
    });
    return { svg: s, received: received, people: people };
  }

  function card(g) {
    var box = el('div', 'poem look-card');
    box.setAttribute('data-game', g.code || g.hands[0]);
    var when = dateWords(g.stamp);
    box.appendChild(el('p', 'top', 'a game of Zero' + (when ? ' · ' + when : '')));
    box.appendChild(el('p', 'path', uniq(g.places.map(function (p) { return cap(p.word); })).join(' · ')));
    g.places.forEach(function (p) {
      p.lines.forEach(function (l, i) {
        var d = el('div', 'place');
        if (!i) d.appendChild(el('div', 'pword', p.word));
        d.appendChild(el('div', 'pline', l.line));
        if (l.name) d.appendChild(el('div', 'pby', l.name));
        box.appendChild(d);
      });
    });
    box.appendChild(el('p', 'sat', g.people.join(' · ')));
    var toggle = el('button', 'quiet look-open', 'What this game shows');
    toggle.type = 'button';
    toggle.setAttribute('aria-expanded', 'false');
    var shows = el('div', 'look-shows');
    shows.hidden = true;
    toggle.addEventListener('click', function () {
      if (!shows.childNodes.length) fillShows(shows, g);
      shows.hidden = !shows.hidden;
      toggle.setAttribute('aria-expanded', String(!shows.hidden));
    });
    box.appendChild(toggle);
    box.appendChild(shows);
    return box;
  }
  function fillShows(box, g) {
    box.appendChild(el('h3', 'look-h', 'Who voted for whom'));
    if (g.votes.length) {
      var w = web(g);
      box.appendChild(w.svg);
      var ranked = w.people.filter(function (p) { return w.received[p]; }).sort(function (a, b) { return w.received[b] - w.received[a]; });
      box.appendChild(el('p', 'look-note votes-said', 'Votes received: ' + ranked.map(function (p) { return p + ' ' + w.received[p]; }).join(' · ') +
        (w.people.length > ranked.length ? '. None for ' + names(w.people.filter(function (p) { return !w.received[p]; })) + '.' : '.')));
    } else {
      box.appendChild(el('p', 'look-note', 'No votes are kept for this game.'));
    }
    box.appendChild(el('h3', 'look-h', 'Where two players thought the same'));
    if (g.same.length) {
      var ul = el('ul', 'look-list same');
      g.same.forEach(function (x) {
        var li = el('li');
        li.appendChild(el('span', 'lw', uniq(x.words).join(' / ')));
        li.appendChild(document.createTextNode(' · ' + names(x.names)));
        li.appendChild(el('span', 'lp', ' under ' + x.place));
        ul.appendChild(li);
      });
      box.appendChild(ul);
    } else {
      box.appendChild(el('p', 'look-note', 'Nobody thought the same.'));
    }
    box.appendChild(el('h3', 'look-h', 'Words only one person saw'));
    var byPlace = [];
    g.alone.forEach(function (x) {
      var row = byPlace.filter(function (r) { return r.place === x.place; })[0];
      if (!row) byPlace.push(row = { place: x.place, words: [] });
      row.words.push(x);
    });
    var al = el('ul', 'look-list alone');
    byPlace.forEach(function (r) {
      var li = el('li');
      li.appendChild(el('span', 'lp', 'under ' + r.place + ': '));
      r.words.forEach(function (x, i) {
        if (i) li.appendChild(document.createTextNode(' · '));
        li.appendChild(el('span', 'lw', x.word));
        if (x.name) li.appendChild(document.createTextNode(' (' + x.name + ')'));
      });
      al.appendChild(li);
    });
    box.appendChild(al);
    if (g.sentOut.length) {
      box.appendChild(el('p', 'look-note', 'The table sent out: ' + g.sentOut.map(function (x) { return x.word + ' (' + x.name + ', under ' + x.place + ')'; }).join(' · ') + '.'));
    }
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
    gs.forEach(function (g) { box.appendChild(card(g)); });
    var rows = across(tree, gs);
    if (rows.length) {
      box.appendChild(el('h2', 'look-head', 'Across all games'));
      box.appendChild(acrossView(rows));
    }
  }

  var api = { voiceParts: voiceParts, voicedPlaces: voicedPlaces, games: games, across: across, render: render, ENOUGH: ENOUGH };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.ZeroLook = api;
})(typeof self !== 'undefined' ? self : this);
