// The law of Zero, checked without a page: node --test tests/zero-law.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const Z = require('../zero-law.js');

test('one word, no spaces', () => {
  assert.equal(Z.oneWord('torch').ok, true);
  assert.equal(Z.oneWord(' Candle. ').word, 'Candle');
  assert.equal(Z.oneWord('two words').why, 'One word, no spaces');
  assert.equal(Z.oneWord('').ok, false);
  assert.equal(Z.oneWord('...').ok, false);
  assert.equal(Z.oneWord("night-light").word, 'night-light');
});

test('the flop matches after lower case and a simple singular (function:zero 2)', () => {
  assert.equal(Z.same('house', 'Houses'), true);
  assert.equal(Z.same('box', 'boxes'), true);
  assert.equal(Z.same('berry', 'berries'), true);
  assert.equal(Z.same('watch', 'watching'), false);
  assert.equal(Z.same('Snow', 'snow'), true);
  const f = Z.flop({ 1: 'snow', 2: 'fog', 3: 'Snow', 4: 'library', 5: 'houses', 6: 'house', 7: 'watching', 8: 'watch' });
  assert.deepEqual(Object.keys(f.out).map(Number).sort(), [1, 3, 5, 6]);
  assert.deepEqual(f.ring, [2, 4, 7, 8]);
});

test('the tree: open places, free tops, voices (the first game\'s own tree)', () => {
  const tree = {
    _: 'The tree of Zero',
    1: {
      _: 'Light — Small lights kept on so someone isn\'t alone. (Night nurse, 3 votes of 7)',
      1: 'torch · Child', 2: 'lighthouse · Sailor',
      5: { _: 'candle — What you light when you\'re waiting for someone. (Grandmother, 4 votes of 7)', 2: 'lantern · Sailor',
        8: { _: 'vigil — tied …', 3: { _: 'silence — The hush. (Night nurse, 2 votes of 6)', 1: 'library · Child' } } }
    }
  };
  const open = Z.openPlaces(tree).map(p => p.digits);
  assert.deepEqual(open, ['11', '12', '152', '15831']);
  assert.equal(Z.openPlaces(tree)[0].parent, 'Light');
  assert.deepEqual(Z.freeTops(tree), ['2', '3', '4', '5', '6', '7', '8', '9']);
  assert.equal(Z.at('1583'), '1.583');
  assert.equal(Z.at('2'), '2');
  assert.deepEqual(Z.trail(tree, '1583').map(x => x.word), ['Light', 'candle', 'vigil', 'silence']);
  assert.equal(Z.readPlace('torch · Child').by, 'Child');
});

test('entries are read from every rung of a supernested room, oldest first', () => {
  const room = {
    _: { _: 'The room', 1: { _: 'a', 1: 'x', 3: 't' }, 2: { _: 'b', 1: 'x', 3: 't' } },
    1: { 1: { _: 'c', 1: 'x', 3: 't' }, 2: { _: 'd', 1: 'x', 3: 't', 9: { _: 'word', 1: 'f'.repeat(64) } } }
  };
  const list = Z.entriesIn(room, '', 2).sort(Z.byAddr);
  assert.deepEqual(list.map(e => e.text), ['a', 'b', 'c', 'd']);
  assert.deepEqual(list.map(e => e.addr), ['01', '02', '11', '12']);
  assert.equal(list[3].data._, 'word');
  const node = Z.entriesIn(room['1'], '1', 2);
  assert.deepEqual(node.map(e => e.addr), ['11', '12']);
});

// ---- a whole table, written as the page writes it ----------------------------------
const CODE = 'LAMP 4';
function room() {
  let n = 0;
  const list = [];
  const add = (who, kind, hand, data, at = '11') => {
    n++;
    list.push({ addr: String(n).padStart(3, '0'), who, at: Z.at(at), stamp: '', code: CODE, kind, hand, data, text: kind });
  };
  return { list, add };
}
async function play(r, hand, step, moves, writerOf) {
  // moves: {seat: move} or {seat: [line, stake]}
  const secrets = {};
  const bySeal = {};
  for (const [seat, mv] of Object.entries(moves)) {
    const move = Array.isArray(mv) ? mv[0] : mv, stake = Array.isArray(mv) ? mv[1] : undefined;
    const s = Z.salt();
    secrets[seat] = { move, stake, s };
    const w = writerOf(Number(seat));
    (bySeal[w] = bySeal[w] || { _: step })[seat] = await Z.seal(CODE, hand, step, seat, move, stake, s);
  }
  for (const [w, d] of Object.entries(bySeal)) r.add(w, 'seal', hand, d);
  const byShow = {};
  for (const [seat, x] of Object.entries(secrets)) {
    const w = writerOf(Number(seat));
    const m = { _: String(x.move), 1: x.s };
    if (x.stake != null) m['2'] = String(x.stake);
    (byShow[w] = byShow[w] || { _: step })[seat] = m;
  }
  for (const [w, d] of Object.entries(byShow)) r.add(w, 'show', hand, d);
}

test('a hand: seats, flop, turn, showdown, a tie split, the pick and the next deal', async () => {
  const r = room();
  const names = ['Matthew', 'Child', 'Sailor', 'Poet', 'Engineer', 'Grandmother', 'Chef', 'Farmer', 'Night nurse'];
  r.add('Matthew', 'open', 0, { _: 'torch', 1: 'Matthew', 2: 'curious kind quiet', 3: '8', 4: names.slice(1).join(', ') });
  const roster = { _: 'torch' };
  names.forEach((nm, i) => { roster[String(i + 1)] = { _: nm, 1: i ? 'computer' : 'person' }; });
  r.add('Matthew', 'deal', 1, roster);
  const writer = s => 'Matthew';
  let t = await Z.derive(r.list, CODE);
  assert.equal(t.phase, 'word');
  assert.equal(t.current.active.length, 9);

  await play(r, 1, 'word', { 1: 'flame', 2: 'dark', 3: 'beam', 4: 'wick', 5: 'Flames', 6: 'cave', 7: 'oil', 8: 'bedside', 9: 'shadow' }, writer);
  t = await Z.derive(r.list, CODE);
  assert.equal(t.phase, 'eye');
  assert.deepEqual(Object.keys(t.current.out).map(Number).sort(), [1, 5]);
  assert.deepEqual(t.current.survivors, [2, 3, 4, 6, 7, 8, 9]);
  assert.equal(t.current.steps.eye.need.length, 9);          // every seat may mark, the knocked-out too
  await play(r, 1, 'eye', { 1: '0', 2: '0', 3: '0', 4: '0', 5: '0', 6: '0', 7: '0', 8: '0', 9: '0' }, writer);
  t = await Z.derive(r.list, CODE);
  assert.equal(t.phase, 'line');
  assert.deepEqual(t.current.ring, [2, 3, 4, 6, 7, 8, 9]);

  await play(r, 1, 'line', { 2: ['A', 2], 3: ['B', 3], 4: ['C', 1], 6: ['D', 3], 7: ['E', 0], 8: ['F', 1], 9: ['G', 5] }, writer);
  t = await Z.derive(r.list, CODE);
  assert.equal(t.phase, 'vote');
  assert.equal(t.current.pot, 15);
  assert.equal(t.current.steps.vote.need.length, 9);

  // 3 and 6 tie on two votes each, at equal stakes: the earlier seal takes the odd chip
  await play(r, 1, 'vote', { 1: 3, 2: 3, 3: 6, 4: 6, 5: 9, 6: 2, 7: 4, 8: 2, 9: 8 }, writer);
  t = await Z.derive(r.list, CODE);
  const h = t.current;
  assert.deepEqual(h.winners.sort(), [2, 3, 6]);
  assert.equal(h.tally[3], 2);
  // stakes: 3 → 3, 6 → 3, 2 → 2; pot 15 split three ways, 5 each
  assert.equal(h.shares[3] + h.shares[6] + h.shares[2], 15);
  assert.equal(h.picker, 3);           // higher stake, then the earlier seal (all in one seal: lower seat)
  assert.equal(t.chips[9], 5);
  assert.equal(t.chips[3], 10 - 3 + 5);
  assert.equal(t.phase, 'voice');
  assert.match(Z.voiceText('torch', h, s => names[s - 1]), /^torch — tied, 2 votes each of 9: /);

  r.add('Matthew', 'pick', 1, { _: '4' });
  t = await Z.derive(r.list, CODE);
  assert.equal(t.current.pick, 4);
  assert.equal(Z.nextPlace(t.current), '114');
  const after = Z.placeAfter(t.current, s => names[s - 1]);
  assert.equal(after['4'], 'wick · Poet');
  assert.equal(after['1'], undefined);
  r.add('Matthew', 'deal', 2, { _: 'wick' }, '114');
  t = await Z.derive(r.list, CODE);
  assert.equal(t.phase, 'word');
  assert.equal(t.current.n, 2);
  assert.equal(t.current.active.length, 9);
});

test('a forged reveal is void; a late seat is gone on without; no ring deals again at the same zero', async () => {
  const r = room();
  r.add('Ann', 'open', 0, { _: 'stone', 1: 'Ann', 2: '', 3: '0', 4: '' }, '2');
  r.add('Ben', 'sit', 0, { _: 'a b c' });
  r.add('Ann', 'deal', 1, { _: 'stone', 1: { _: 'Ann', 1: 'person' }, 2: { _: 'Ben', 1: 'person' }, 3: { _: 'Cat', 1: 'person' } }, '2');
  const writer = s => ['Ann', 'Ben', 'Cat'][s - 1];
  // Cat never seals: the host goes on without her
  const sA = Z.salt(), sB = Z.salt();
  r.add('Ann', 'seal', 1, { _: 'word', 1: await Z.seal(CODE, 1, 'word', 1, 'rock', undefined, sA) }, '2');
  r.add('Ben', 'seal', 1, { _: 'word', 2: await Z.seal(CODE, 1, 'word', 2, 'Rocks', undefined, sB) }, '2');
  let t = await Z.derive(r.list, CODE);
  assert.deepEqual(Z.waitingSeal(t.current.steps.word), [3]);
  r.add('Ann', 'go', 1, { _: 'word', 1: 'seal', 2: '3' }, '2');
  // Ben shows a different word than he sealed: void
  r.add('Ann', 'show', 1, { _: 'word', 1: { _: 'rock', 1: sA } }, '2');
  r.add('Ben', 'show', 1, { _: 'word', 2: { _: 'pebble', 1: sB } }, '2');
  t = await Z.derive(r.list, CODE);
  assert.equal(t.current.steps.word.shows[2].ok, false);
  assert.deepEqual(t.current.survivors, [1]);
  // the table's eye: Ann and Ben skip; Cat has gone, and the host goes on without her
  await play(r, 1, 'eye', { 1: '0', 2: '0' }, writer);
  r.add('Ann', 'go', 1, { _: 'eye', 1: 'seal', 2: '3' }, '2');
  t = await Z.derive(r.list, CODE);
  assert.deepEqual(t.current.ring, [1]);
  // the lone sentence: Ann can vote for nobody else, so only Ben and Cat vote
  await play(r, 1, 'line', { 1: ['A stone remembers.', 4] }, writer);
  t = await Z.derive(r.list, CODE);
  assert.deepEqual(t.current.steps.vote.need, [2, 3]);
});

test('the table\'s eye: a word marked by more than half the seats goes out; never one\'s own', async () => {
  const r = room();
  const names = ['Ann', 'Ben', 'Cat', 'Dev', 'Eve', 'Fin', 'Gus', 'Hal', 'Ivy'];
  r.add('Ann', 'open', 0, { _: 'sea', 1: 'Ann' }, '3');
  const roster = { _: 'sea' };
  names.forEach((nm, i) => { roster[String(i + 1)] = { _: nm, 1: 'person' }; });
  r.add('Ann', 'deal', 1, roster, '3');
  const writer = s => names[s - 1];
  await play(r, 1, 'word', { 1: 'wave', 2: 'shell', 3: 'gull', 4: 'tide', 5: 'salt', 6: 'Waves', 7: 'spreadsheet', 8: 'invoice', 9: 'foam' }, writer);
  let t = await Z.derive(r.list, CODE);
  assert.equal(t.phase, 'eye');
  assert.deepEqual(t.current.survivors, [2, 3, 4, 5, 7, 8, 9]);
  // five of nine mark spreadsheet (more than half: out); two mark invoice (not more than half: stays);
  // spreadsheet's own seat marks itself (ignored); one marks wave, already matched out (ignored)
  await play(r, 1, 'eye', { 1: '7', 2: '7', 3: '7', 4: '7', 5: '7', 6: '8', 7: '7', 8: '1', 9: '8' }, writer);
  t = await Z.derive(r.list, CODE);
  const h = t.current;
  assert.equal(h.marked[7], 5);
  assert.equal(h.marked[8], 2);
  assert.equal(h.marks[7], undefined);
  assert.equal(h.marks[8], undefined);
  assert.equal(h.eyeOut[7], true);
  assert.equal(h.eyeOut[8], undefined);
  assert.deepEqual(h.ring, [2, 3, 4, 5, 8, 9]);
  assert.equal(t.phase, 'line');
  assert.deepEqual(h.steps.line.need, [2, 3, 4, 5, 8, 9]);
  // the seat sent out writes no sentence, but still votes, and stays empty in the tree
  await play(r, 1, 'line', { 2: ['a', 1], 3: ['b', 1], 4: ['c', 1], 5: ['d', 1], 8: ['e', 1], 9: ['f', 1] }, writer);
  t = await Z.derive(r.list, CODE);
  assert.ok(t.current.steps.vote.need.includes(7));
  await play(r, 1, 'vote', { 1: 2, 2: 3, 3: 2, 4: 2, 5: 2, 6: 2, 7: 2, 8: 2, 9: 2 }, writer);
  t = await Z.derive(r.list, CODE);
  const place = Z.placeAfter(t.current, s => names[s - 1]);
  assert.equal(place['7'], undefined);
  assert.equal(place['8'], 'invoice · Hal');
});

test('the eye can send out every word left: the hand has no sentences and is dealt again', async () => {
  const r = room();
  r.add('Ann', 'open', 0, { _: 'sea', 1: 'Ann' }, '3');
  r.add('Ann', 'deal', 1, { _: 'sea', 1: { _: 'Ann', 1: 'person' }, 2: { _: 'Ben', 1: 'person' }, 3: { _: 'Cat', 1: 'person' } }, '3');
  const writer = s => ['Ann', 'Ben', 'Cat'][s - 1];
  await play(r, 1, 'word', { 1: 'wave', 2: 'waves', 3: 'tax' }, writer);
  await play(r, 1, 'eye', { 1: '3', 2: '3', 3: '0' }, writer);
  const t = await Z.derive(r.list, CODE);
  assert.equal(t.current.eyeOut[3], true);
  assert.equal(t.current.noRing, true);
  assert.equal(Z.nextPlace(t.current), '3');
});

test('every word knocked out: the hand counts and the next is dealt at the same zero', async () => {
  const r = room();
  r.add('Ann', 'open', 0, { _: 'sea', 3: '0', 1: 'Ann' }, '3');
  r.add('Ann', 'deal', 1, { _: 'sea', 1: { _: 'Ann', 1: 'person' }, 2: { _: 'Ben', 1: 'person' } }, '3');
  await play(r, 1, 'word', { 1: 'wave', 2: 'waves' }, s => ['Ann', 'Ben'][s - 1]);
  const t = await Z.derive(r.list, CODE);
  assert.equal(t.current.noRing, true);
  assert.equal(t.phase, 'done');
  assert.equal(Z.nextPlace(t.current), '3');
});

test('seals only count from the seat\'s own writer', async () => {
  const r = room();
  r.add('Ann', 'open', 0, { _: 'sea', 3: '0', 1: 'Ann' }, '3');
  r.add('Ann', 'deal', 1, { _: 'sea', 1: { _: 'Ann', 1: 'person' }, 2: { _: 'Ben', 1: 'person' } }, '3');
  r.add('Mallory', 'seal', 1, { _: 'word', 2: 'a'.repeat(64) }, '3');
  const t = await Z.derive(r.list, CODE);
  assert.deepEqual(Z.waitingSeal(t.current.steps.word), [1, 2]);
});

test('leaving before the deal frees the seat for the next to arrive', async () => {
  const r = room();
  r.add('Ann', 'open', 0, { _: 'sea', 1: 'Ann', 6: 'eye' }, '3');
  r.add('Ben', 'sit', 0, {});
  r.add('Ben', 'leave', 0, {});
  r.add('Cat', 'sit', 0, {});
  const t = await Z.derive(r.list, CODE);
  assert.deepEqual(t.sits.map(x => x.name), ['Cat']);
  assert.equal(t.seats[2].name, 'Cat');
});

test('leaving during play: the seat sits out every move left, and the table does not wait', async () => {
  const r = room();
  const names = ['Ann', 'Ben', 'Cat', 'Dev'];
  r.add('Ann', 'open', 0, { _: 'sea', 1: 'Ann', 6: 'eye' }, '3');
  r.add('Ann', 'deal', 1, { _: 'sea', 1: { _: 'Ann', 1: 'person' }, 2: { _: 'Ben', 1: 'person' }, 3: { _: 'Cat', 1: 'person' }, 4: { _: 'Dev', 1: 'person' } }, '3');
  const writer = s => names[s - 1];
  // Ben leaves before writing his word; the others' words complete the step without him
  r.add('Ben', 'leave', 0, {});
  r.add('Mallory', 'leave', 0, {});          // nobody of that name: nothing happens
  r.add('Ann', 'leave', 0, {});              // the host closes instead: a leave from the host does nothing
  await play(r, 1, 'word', { 1: 'wave', 3: 'gull', 4: 'tide' }, writer);
  let t = await Z.derive(r.list, CODE);
  assert.equal(t.gone[2], true);
  assert.equal(t.gone[1], undefined);
  assert.equal(t.phase, 'eye');
  assert.deepEqual(t.current.steps.eye.need, [1, 3, 4]);
  await play(r, 1, 'eye', { 1: '0', 3: '0', 4: '0' }, writer);
  await play(r, 1, 'line', { 1: ['a', 1], 3: ['b', 1], 4: ['c', 1] }, writer);
  t = await Z.derive(r.list, CODE);
  assert.deepEqual(t.current.steps.vote.need, [1, 3, 4]);
  await play(r, 1, 'vote', { 1: 3, 3: 1, 4: 1 }, writer);
  r.add('Ann', 'pick', 1, { _: '1' }, '3');
  r.add('Ann', 'deal', 2, { _: 'wave' }, '31');
  t = await Z.derive(r.list, CODE);
  assert.deepEqual(t.current.active, [1, 3, 4]);
  // Dev leaves after sealing his word but before it is shown: the step goes on without it
  const s1 = Z.salt(), s3 = Z.salt(), s4 = Z.salt();
  r.add('Ann', 'seal', 2, { _: 'word', 1: await Z.seal(CODE, 2, 'word', 1, 'foam', undefined, s1) }, '31');
  r.add('Cat', 'seal', 2, { _: 'word', 3: await Z.seal(CODE, 2, 'word', 3, 'spray', undefined, s3) }, '31');
  r.add('Dev', 'seal', 2, { _: 'word', 4: await Z.seal(CODE, 2, 'word', 4, 'crest', undefined, s4) }, '31');
  r.add('Ann', 'show', 2, { _: 'word', 1: { _: 'foam', 1: s1 } }, '31');
  r.add('Cat', 'show', 2, { _: 'word', 3: { _: 'spray', 1: s3 } }, '31');
  r.add('Dev', 'leave', 0, {});
  t = await Z.derive(r.list, CODE);
  assert.equal(t.phase, 'eye');
  assert.deepEqual(t.current.survivors, [1, 3]);
  assert.deepEqual(t.current.steps.eye.need, [1, 3]);
});

test('word chips (function:zero 2.1): from the bank, 1 for each word left after the eye, 2 if never played, 2 more when chosen', async () => {
  const r = room();
  const names = ['Ann', 'Ben', 'Cat', 'Dev'];
  r.add('Ann', 'open', 0, { _: 'sea', 1: 'Ann', 6: 'eye', 7: 'chips' }, '3');
  r.add('Ann', 'deal', 1, { _: 'sea', 1: { _: 'Ann', 1: 'person' }, 2: { _: 'Ben', 1: 'person' }, 3: { _: 'Cat', 1: 'person' }, 4: { _: 'Dev', 1: 'person' } }, '3');
  const writer = s => names[s - 1];
  await play(r, 1, 'word', { 1: 'wave', 2: 'Waves', 3: 'gull', 4: 'spreadsheet' }, writer);
  await play(r, 1, 'eye', { 1: '4', 2: '4', 3: '4', 4: '0' }, writer);
  let t = await Z.derive(r.list, CODE);
  assert.deepEqual(t.current.ring, [3]);
  assert.equal(t.current.banked, false);
  r.add('Ben', 'bank', 1, { _: 'words', 3: '2' }, '3');              // not the host: ignored
  r.add('Ann', 'bank', 1, { _: 'words', 1: '2', 3: '2', 4: '1' }, '3'); // only the word left is paid
  r.add('Ann', 'bank', 1, { _: 'words', 3: '2' }, '3');              // once a hand
  t = await Z.derive(r.list, CODE);
  const h = t.current;
  assert.deepEqual(h.wordChips, { 3: 2 });
  assert.equal(t.chips[3], 12);
  assert.equal(t.chips[1], 10);
  assert.equal(Z.stakeable(h, 3), 12);
  // the word chips can be staked in the same turn
  await play(r, 1, 'line', { 3: ['Gulls over the sea.', 12] }, writer);
  await play(r, 1, 'vote', { 1: 3, 2: 3, 4: 3 }, writer);
  t = await Z.derive(r.list, CODE);
  assert.equal(t.current.lines[3].stake, 12);
  assert.equal(t.chips[3], 12);                       // staked 12, won the pot of 12
  r.add('Ann', 'pick', 1, { _: '3' }, '3');
  t = await Z.derive(r.list, CODE);
  assert.equal(t.current.chosenChips, 2);
  assert.equal(t.chips[3], 14);
  const book = Z.chipsBook(t);
  assert.deepEqual(book[3], { words: 4, sentences: 0, total: 14 });
  assert.deepEqual(book[1], { words: 0, sentences: 0, total: 10 });
  Object.keys(book).forEach(s => assert.equal(book[s].total, 10 + book[s].words + book[s].sentences));
});

test('a table from before word chips pays none, whatever its record says', async () => {
  const r = room();
  r.add('Ann', 'open', 0, { _: 'sea', 1: 'Ann', 6: 'eye' }, '3');
  r.add('Ann', 'deal', 1, { _: 'sea', 1: { _: 'Ann', 1: 'person' }, 2: { _: 'Ben', 1: 'person' } }, '3');
  await play(r, 1, 'word', { 1: 'wave', 2: 'gull' }, s => ['Ann', 'Ben'][s - 1]);
  await play(r, 1, 'eye', { 1: '0', 2: '0' }, s => ['Ann', 'Ben'][s - 1]);
  r.add('Ann', 'bank', 1, { _: 'words', 1: '1', 2: '2' }, '3');
  const t = await Z.derive(r.list, CODE);
  assert.deepEqual(t.chips, { 1: 10, 2: 10 });
});

test('played before: anywhere in the tree, case and singular form ignored', () => {
  const tree = { 1: { _: 'Light — Small lights. (Night nurse, 3 votes of 7)', 1: 'torch · Child', 5: { _: 'candle — x (Grandmother, 4 votes of 7)', 2: 'lantern · Sailor' } } };
  assert.equal(Z.playedBefore(tree, 'Candles'), true);
  assert.equal(Z.playedBefore(tree, 'TORCHES'), true);
  assert.equal(Z.playedBefore(tree, 'light'), true);
  assert.equal(Z.playedBefore(tree, 'spreadsheet'), false);
  assert.equal(Z.playedBefore(tree, 'lanterns'), true);
});
