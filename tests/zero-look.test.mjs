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
