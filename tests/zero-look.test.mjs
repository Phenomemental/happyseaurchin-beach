// Look around (spine:zero 4.9) and the end of a game (4.62), checked against
// the real tree and room as they stood on the night of 8 November 2026
// (tests/zero-fixtures.json): the first game, played in chat, and three
// played on the page, KITE 1 (Organisation), RAIN 6 (Gift) and TIDE 3
// (Happening). node --test tests/zero-look.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const Z = require('../zero-law.js');
const L = require('../zero-look.js');
const fx = JSON.parse(fs.readFileSync(new URL('./zero-fixtures.json', import.meta.url), 'utf8'));
const tree = fx.zero;
function floorOf(b) { let f = 0, n = b; while (n && typeof n === 'object' && n._ !== undefined) { f++; n = n._; } return f; }
const entries = Z.entriesIn(fx['pool:zero'], '', floorOf(fx['pool:zero'])).sort(Z.byAddr);

test('KITE 1, played before the eye, replays to its end and rebuilds the tree word for word', async () => {
  const t = await Z.derive(entries, 'KITE 1');
  assert.equal(t.over, true);
  assert.equal(t.hands.length, 4);
  t.hands.forEach(h => {
    assert.equal(h.noEye, true);
    const placed = Z.placeAfter(h, s => Z.nameOf(t, s));
    assert.equal(placed._, Z.textOf(Z.nodeAt(tree, h.place)));
  });
});

test('the games, newest first: the page tables from their records, the first game from the tree and its prose', async () => {
  const gs = await L.games(tree, entries);
  assert.deepEqual(gs.map(g => g.code || g.hands[0]), ['TIDE 3', 'RAIN 6', 'KITE 1', '1']);
  assert.deepEqual(gs.map(g => g.places[0].word), ['Happening', 'Gift', 'Organisation', 'Light']);
  const [, , kite, first] = gs;
  assert.equal(kite.kind, 'table');
  assert.equal(kite.code, 'KITE 1');
  assert.deepEqual(kite.places.map(p => p.word), ['Organisation', 'compost', 'worms', 'tide']);
  assert.deepEqual(kite.places.map(p => p.lines[0].name), ['Farmer', 'Poet', 'Sailor', 'Chef']);
  assert.equal(kite.people[0], 'Phenomemental');
  assert.equal(kite.votes.length, 36);
  assert.ok(kite.same.some(x => x.word === 'Team' && x.names.join() === 'Phenomemental,Child'));
  assert.ok(kite.same.some(x => x.word === 'moon' && x.names.length === 5 && x.place === 'tide'));

  assert.equal(first.kind, 'earlier');
  assert.equal(first.stamp, '2026-11-08T18:23:34.880Z');
  assert.deepEqual(first.places.map(p => p.word), ['Light', 'candle', 'vigil', 'silence']);
  assert.deepEqual(first.places[2].lines.map(l => l.name), ['Child', 'Phenomemental']);   // the tie stands together
  assert.deepEqual(first.people, ['Child', 'Sailor', 'Poet', 'Engineer', 'Grandmother', 'Chef', 'Farmer', 'Night nurse', 'Phenomemental']);
  const per = {};
  first.votes.forEach(v => { per[v.place] = (per[v.place] || 0) + 1; });
  assert.deepEqual(per, { Light: 7, candle: 7, vigil: 7, silence: 6 });   // as the tree says: of 7, 7, 7, 6
  assert.ok(first.votes.some(v => v.from === 'Child' && v.to === 'Phenomemental' && v.place === 'Light'));
  assert.ok(first.votes.some(v => v.from === 'Phenomemental' && v.to === 'Sailor' && v.place === 'silence'));
  assert.deepEqual(first.same.map(x => x.word + ':' + x.names.join('&')), ['birthday:Child&Phenomemental', 'watch:Sailor&Night nurse', 'snow:Grandmother&Farmer']);
  assert.ok(first.alone.some(x => x.word === 'torch' && x.name === 'Child'));
});

test('every game played on the page reads true against the tree: voices word for word, votes as counted, chips kept', async () => {
  for (const code of ['KITE 1', 'RAIN 6', 'TIDE 3']) {
    const t = await Z.derive(entries, code);
    assert.equal(t.over, true, code);
    t.hands.forEach(h => {
      const voice = Z.textOf(Z.nodeAt(tree, h.place));
      assert.equal(Z.placeAfter(h, s => Z.nameOf(t, s))._, voice, code + ' ' + h.zero);
      assert.equal(h.votesCast, Number(/of (\d+)/.exec(voice)[1]), code + ' votes at ' + h.zero);
    });
    const g = L.tableGame(t);
    assert.equal(g.votes.length, 36, code);
    assert.equal(g.book.reduce((a, b) => a + b.total, 0), 90, code);
    g.book.forEach(b => assert.equal(b.total, 10 + b.words + b.sentences, code + ' ' + b.name));
  }
});

test('what Gift and Happening show: the words alike, the words the table sent out', async () => {
  const gift = L.tableGame(await Z.derive(entries, 'RAIN 6'));
  assert.deepEqual(gift.same.map(x => x.word + ':' + x.names.join('&')), ['compost:Farmer&Grandmother']);
  assert.deepEqual(gift.sentOut.map(x => x.word + ':' + x.name + ':' + x.place), ['Torque:Engineer:Gift', 'infirmary:Night nurse:garden']);
  const happening = L.tableGame(await Z.derive(entries, 'TIDE 3'));
  assert.deepEqual(happening.same.map(x => x.word + ':' + x.names.join('&')), ['Candle:Poet&Child']);
  assert.deepEqual(happening.sentOut.map(x => x.word + ':' + x.name), ['stubble:Farmer']);
  assert.equal(happening.people[1], 'Davido');
});

test('across all games: hidden until a first word has two games, then what keeps colliding and what was played once', async () => {
  const gs = await L.games(tree, entries);
  assert.deepEqual(L.across(tree, gs), []);
  const a = { first: '1', words: [{ word: 'snow', name: 'A', collided: true }, { word: 'snow', name: 'B', collided: true }, { word: 'torch', name: 'C', collided: false }] };
  const b = { first: '1', words: [{ word: 'Snow', name: 'D', collided: true }, { word: 'snow', name: 'E', collided: true }, { word: 'lamp', name: 'F', collided: false }] };
  const rows = L.across(tree, [a, b]);
  assert.equal(rows.length, 1);
  assert.equal(rows[0].word, 'Light');
  assert.deepEqual(rows[0].collide, ['snow']);
  assert.deepEqual(rows[0].alone.map(x => x.word), ['lamp', 'torch']);
});

test('the voices read from the tree, single and tied', () => {
  assert.deepEqual(L.voiceParts('What you light when you\'re waiting for someone. (Grandmother, 4 votes of 7)'), [{ line: 'What you light when you\'re waiting for someone.', by: 'Grandmother', votes: 4 }]);
  const tie = L.voiceParts(Z.readPlace(Z.nodeAt(tree, '158')).voice);
  assert.equal(tie.length, 2);
  assert.equal(tie[1].by, 'Phenomemental');
  assert.equal(tie[1].votes, 2);
});

// ---- the second version of What this game shows (spine:zero 4.91) ------------------------------
test('4.91, Organisation (KITE 1): people or computer, who listens, short wins, a word that travelled, the trail', async () => {
  const gs = await L.games(tree, entries);
  const kite = gs.find(g => g.code === 'KITE 1');
  const r = L.readings(kite);
  // one person at the table: Phenomemental gave all four votes to computer players and got none back
  assert.deepEqual(r.pc.people, ['Phenomemental']);
  assert.deepEqual([r.pc.pp, r.pc.pc, r.pc.cp, r.pc.cc], [0, 4, 0, 32]);
  assert.deepEqual(r.listens.unvoted, ['Phenomemental', 'Child']);
  assert.ok(r.listens.pairs.some(p => p.join() === 'Chef,Poet'));
  // the four winning sentences, counted by hand: 16, 12, 10 and 8 words
  assert.deepEqual(kite.rounds.map(x => L.wordsIn(x.lines.find(l => l.won).line).length), [16, 12, 10, 8]);
  assert.equal(r.short.won, 11.5);
  assert.equal(r.short.wonN + r.short.restN, 22);
  // kitchen, knocked out under Organisation, came back in five sentences at compost and as Chef's word at worms
  assert.equal(r.travelled.length, 1);
  assert.equal(r.travelled[0].word, 'kitchen');
  assert.deepEqual(r.travelled[0].names, ['Grandmother', 'Chef']);
  assert.deepEqual(r.travelled[0].back.map(b => b.place + ':' + b.words.join('+') + ':' + b.lines.length), ['compost::5', 'worms:Chef:5']);
  // the trail: Organisation at the second first word, then the seats chosen beneath: 9, 3, 4
  assert.deepEqual(r.trail.map(x => x.word + '@' + x.seat), ['Organisation@2', 'compost@9', 'worms@3', 'tide@4']);
  // played before the eye
  assert.equal(r.eye.hands, 0);
  const pheno = r.bold.find(b => b.name === 'Phenomemental');
  assert.deepEqual([pheno.stakes, pheno.votes, pheno.won], [[10], [0], 0]);
});

test('4.91, Gift (RAIN 6) and Happening (TIDE 3): the table\'s eye, bold or careful, people or computer', async () => {
  const gs = await L.games(tree, entries);
  const gift = L.readings(gs.find(g => g.code === 'RAIN 6'));
  assert.equal(gift.eye.hands, 4);
  assert.deepEqual(gift.eye.marked.filter(m => m.place === 'Gift').map(m => m.word + ':' + m.n + ':' + m.out), ['Torque:6:true', 'Ganache:1:false']);
  assert.deepEqual(gift.eye.marked.filter(m => m.out).map(m => m.word), ['Torque', 'infirmary']);
  assert.deepEqual(gift.eye.most, ['Phenomemental', 'Sailor', 'Chef', 'Poet']);
  assert.equal(gift.eye.markers[0].n, 3);
  const happening = L.readings(gs.find(g => g.code === 'TIDE 3'));
  assert.deepEqual(happening.pc.people, ['Phenomemental', 'Davido']);
  assert.equal(happening.pc.pp + happening.pc.pc + happening.pc.cp + happening.pc.cc, 36);
  assert.ok(happening.listens.pairs.some(p => p.join() === 'Phenomemental,Davido'));
  assert.deepEqual(happening.eye.most, ['Davido']);
  assert.equal(happening.eye.markers[0].n, 4);
  const nurse = happening.bold.find(b => b.name === 'Night nurse');
  assert.deepEqual([nurse.stakes, nurse.votes, nurse.won], [[3, 3, 8, 12], [1, 3, 7, 3], 3]);
  assert.deepEqual(happening.trail.map(x => x.word), ['Happening', 'Ember', 'Vigil', 'Monitor']);
  assert.ok(happening.travelled.some(x => x.word === 'Kettle' && x.back[0].place === 'Vigil' && x.back[0].words.join() === 'Grandmother'));
});

test('4.91, the first game, played in chat: its sentences and stakes read from its prose', async () => {
  const gs = await L.games(tree, entries);
  const first = gs.find(g => g.kind === 'earlier');
  assert.deepEqual(first.rounds.map(x => x.lines.length), [7, 7, 7, 6]);
  assert.deepEqual(first.rounds[0].lines.map(l => l.name + ':' + l.stake), ['Child:3', 'Sailor:5', 'Engineer:4', 'Grandmother:6', 'Chef:3', 'Night nurse:5', 'Phenomemental:5']);
  const r = L.readings(first);
  const me = r.bold.find(b => b.name === 'Phenomemental');
  assert.deepEqual(me.stakes, [5, 5, 13]);           // the stake at Vigil named afterwards, all in at Silence
  assert.deepEqual(r.bold.find(b => b.name === 'Night nurse').won, 2);
  assert.ok(first.rounds.every(x => x.lines.reduce((a, l) => a + l.votes, 0) === first.votes.filter(v => v.place === x.zero).length));
  assert.deepEqual(r.pc.people, ['Phenomemental']);
  assert.equal(r.eye.hands, 0);
  assert.deepEqual(r.trail.map(x => x.word + '@' + x.seat), ['Light@1', 'candle@5', 'vigil@8', 'silence@3']);
});

test('4.91, words new to the tree: nobody had played them anywhere in it before the game began', async () => {
  const gs = await L.games(tree, entries);
  const by = c => gs.find(g => (g.code || g.hands[0]) === c);
  // the first game came first: all 27 words it put in the tree were new
  assert.equal(by('1').fresh.length, 27);
  // Happening: ward, Vigil and Monitor were the first game's, Furrow was Gift's; Kettle is listed once
  const h = by('TIDE 3').fresh.map(x => x.word);
  assert.equal(h.length, 27);
  ['ward', 'Vigil', 'Monitor', 'Furrow', 'Candle', 'stubble'].forEach(w => assert.ok(!h.includes(w), w));
  ['Kettle', 'Ember', 'Oscilloscope'].forEach(w => assert.ok(h.includes(w), w));
  assert.equal(h.filter(w => w === 'Kettle').length, 1);
  // Gift: garden and compost were Organisation's, lantern the first game's
  const gift = by('RAIN 6').fresh.map(x => x.word.toLowerCase());
  ['garden', 'compost', 'worm', 'lantern'].forEach(w => assert.ok(!gift.includes(w), w));
  // and nothing knocked out counts: words never written into the tree are not new to it
  assert.ok(!by('KITE 1').fresh.some(x => x.word === 'Team' || x.word === 'moon'));
  // no word is new in a game if an earlier game had it, case and plural ignored
  gs.forEach(g => g.fresh.forEach(x => gs.filter(o => o.stamp < g.stamp).forEach(o =>
    assert.ok(!o.rounds.some(rr => rr.words.some(w => w.kept && Z.same(w.word, x.word))), x.word + ' in ' + (g.code || g.hands[0])))));
});

test('a code that comes round again starts a table of its own: Happening, played again under KITE 1, stands beside Organisation', async () => {
  // Happening's record, relabelled KITE 1 and sealed again under that code with the law's own seal
  const shows = {};
  entries.filter(e => e.code === 'TIDE 3' && e.kind === 'show').forEach(e => {
    Object.keys(e.data).filter(k => /^[1-9]$/.test(k)).forEach(k => { shows[e.hand + '|' + e.data._ + '|' + k] = e.data[k]; });
  });
  const relabelled = await Promise.all(entries.map(async e => {
    if (e.code !== 'TIDE 3') return e;
    const c = { ...e, code: 'KITE 1', data: { ...e.data } };
    if (e.kind === 'seal') {
      for (const k of Object.keys(e.data).filter(k => /^[1-9]$/.test(k))) {
        const m = shows[e.hand + '|' + e.data._ + '|' + k];
        if (m) c.data[k] = await Z.seal('KITE 1', e.hand, e.data._, k, Z.textOf(m), m['2'], m['1']);
      }
    }
    return c;
  }));
  const gs = await L.games(tree, relabelled);
  const kites = gs.filter(g => g.code === 'KITE 1');
  assert.deepEqual(kites.map(g => g.places[0].word), ['Happening', 'Organisation']);
  assert.deepEqual(kites.map(g => g.votes.length), [36, 36]);
  assert.deepEqual(kites[0].rounds.map(r => r.zero), ['Happening', 'Ember', 'Vigil', 'Monitor']);
  assert.ok(!gs.some(g => g.kind === 'earlier' && g.first === '4'));   // its places are not taken for a game played in chat
});
