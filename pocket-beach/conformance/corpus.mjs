import { readFileSync } from 'node:fs';

// corpus.mjs — the hand-chosen calls. Each case starts from an empty pocket,
// seeds its blocks (through the beach, or raw where a block is malformed on
// purpose), then makes its calls in order. agent_id "pocket" is the phone
// region; "pscale" is the teaching shelf.

const P = 'pocket';
const S = 'pscale';

const garden = {
  _: 'Garden at the back of the house.',
  1: {
    _: 'Spring — the beds wake',
    1: 'sow peas by the fence',
    2: { _: 'tulips along the path', 1: 'red ones first', 2: 'yellow after' },
    3: 'turn the compost',
  },
  2: 'Summer — water at dusk',
  3: { _: { _: 'Autumn, the hidden voice', 1: 'hidden: the bulbs list', 2: 'hidden: seed packets' }, 1: 'plant the bulbs', 2: 'rake the leaves' },
  9: 'Winter — rest the soil',
};

// Floor 3: the root's underscore chain is three deep, with an era at each rung.
const deep = {
  _: { _: { _: 'Deep — a floor-three block', 1: 'era two, position one' }, 2: 'era one, position two' },
  1: { _: 'one hundred', 2: { _: 'one twenty', 3: { _: 'one twenty-three', 4: { _: 'one twenty-three point four, below the floor', 5: 'deeper still' } } } },
  3: 'three hundred',
};

const iso = (d) => `2026-10-${String(d).padStart(2, '0')}T10:00:00Z`;

const marks = {
  _: 'Marks left in the pocket.',
  1: { _: 'first mark', 1: 'matthew', 2: '', 3: '2026-10-01T10:00:00Z' },
  2: { _: 'second mark, decorated stamp', 1: 'matthew', 2: '1.2', 3: '2026-10-02T11:30:00.250Z (+26 — 26 days ago)' },
  3: 'a bare line',
  4: { _: 'a mark with a tag at three', 1: 'matthew', 3: 'tagged' },
};

const log9 = { _: 'Log of the week.' };
for (let i = 1; i <= 9; i++) log9[i] = { _: `entry ${i} of the week`, 3: iso(i) };

// Floor 2, every slot taken: the era 1-9 under the root, then 11-99.
const logFull = { _: { _: 'A long log.' } };
for (let i = 1; i <= 9; i++) logFull._[i] = { _: `era line ${i}`, 3: iso(i) };
for (let c = 1; c <= 9; c++) {
  logFull[c] = {};
  for (let i = 1; i <= 9; i++) logFull[c][i] = { _: `line ${c}${i}`, 3: iso(10 + c) };
}

// Floor 2 with two completed spans and one entry in the third: 10 and 20 owed.
const logOwed = { _: { _: 'A log with summaries owed.' } };
for (let i = 1; i <= 9; i++) logOwed._[i] = { _: `the ${i} of the first nine`, 3: iso(i) };
logOwed[1] = {};
for (let i = 1; i <= 9; i++) logOwed[1][i] = { _: `second span line ${i}`, 3: iso(10 + i) };
logOwed[2] = { 1: { _: 'third span opens', 3: iso(25) } };

const shelf = {
  _: 'Shelves in the pantry.',
  1: { _: 'top shelf, three jars', 1: 'jam jar', 2: 'honey pot', 3: 'pickled onions' },
  2: { _: 'middle shelf, full' },
  3: 'bottom shelf is bare prose',
  4: { _: 'spice rack, full and dated' },
};
for (let i = 1; i <= 9; i++) shelf[2][i] = `item ${i}`;
for (let i = 1; i <= 9; i++) shelf[4][i] = { _: `spice ${i}`, 3: `2026-0${(i % 9) + 1}-15T08:00:00Z` };

const scores = {
  _: 'Scores and switches.',
  1: 5,
  2: true,
  3: 1.5,
  4: { _: 'nested numbers', 1: 0, 2: -2.25e-7, 3: 1e21, 4: false, 5: null, 6: 123456789012, 7: 0.30000000000000004 },
};

const uni = {
  _: 'Ünïcødé — 日本語 and the sea 🌊🦔',
  1: 'x'.repeat(149) + '🌊 a wave straddles the cut',
  2: 'quotes " and \\ backslash, tab\t, newline\n, bell \u0007, nbsp , line separator   end',
  3: '  spaced at both ends  ',
  4: { _: 'café crème', 1: 'naïve façade', 2: '🦔🦔🦔' },
};

const long = (n, word) => Array.from({ length: n }, (_, i) => `${word} ${i}`).join(' ');
const big = { _: 'A block of long lines.' };
for (let i = 1; i <= 9; i++) big[i] = { _: long(110, `line${i}`), 1: `short child of ${i}` };

const wide = { _: 'A wide block.' };
for (let a = 1; a <= 9; a++) {
  wide[a] = { _: `branch ${a}` };
  for (let b = 1; b <= 9; b++) {
    wide[a][b] = { _: `twig ${a}${b}` };
    for (let c = 1; c <= 3; c++) wide[a][b][c] = `leaf ${a}${b}${c} ` + long(14, 'word');
  }
}

export const cases = [
  {
    name: 'reads on a floor-1 block',
    seed: [{ block: 'garden', content: garden }],
    calls: [
      { agent_id: P, block: 'garden' },
      { agent_id: P, block: 'garden', pscale_attention: 0 },
      { agent_id: P, block: 'garden', pscale_attention: -1 },
      { agent_id: P, block: 'garden', pscale_attention: -2 },
      { agent_id: P, block: 'garden', pscale_attention: 1 },
      { agent_id: P, block: 'garden', pscale_attention: -9 },
      { agent_id: P, block: 'garden', spindle: '1' },
      { agent_id: P, block: 'garden', spindle: '1.2' },
      { agent_id: P, block: 'garden', spindle: '1.21' },
      { agent_id: P, block: 'garden', spindle: '12' },
      { agent_id: P, block: 'garden', spindle: '1.2', pscale_attention: 0 },
      { agent_id: P, block: 'garden', spindle: '1.2', pscale_attention: -1 },
      { agent_id: P, block: 'garden', spindle: '1.2', pscale_attention: 1 },
      { agent_id: P, block: 'garden', spindle: '1', pscale_attention: 5 },
      { agent_id: P, block: 'garden', spindle: '1', pscale_attention: -1 },
      { agent_id: P, block: 'garden', spindle: '1', pscale_attention: -3 },
      { agent_id: P, block: 'garden', spindle: '3*' },
      { agent_id: P, block: 'garden', spindle: '3*1' },
      { agent_id: P, block: 'garden', spindle: '3*', pscale_attention: 0 },
      { agent_id: P, block: 'garden', spindle: '1*' },
      { agent_id: P, block: 'garden', spindle: '9*' },
      { agent_id: P, block: 'garden', spindle: '*' },
      { agent_id: P, block: 'garden', spindle: '0' },
      { agent_id: P, block: 'garden', spindle: '0', pscale_attention: 0 },
      { agent_id: P, block: 'garden', spindle: '01' },
      { agent_id: P, block: 'garden', spindle: '.5' },
      { agent_id: P, block: 'garden', spindle: '7' },
      { agent_id: P, block: 'garden', spindle: '1.9' },
      { agent_id: P, block: 'garden', spindle: '10' },
      { agent_id: P, block: 'garden', spindle: '100' },
      { agent_id: P, block: 'garden', spindle: '1.2.3' },
      { agent_id: P, block: 'garden', spindle: '1a' },
      { agent_id: P, block: 'garden', spindle: '12.3' },
      { agent_id: P, block: 'garden', spindle: '1**' },
      { agent_id: P, block: 'garden', spindle: '', pscale_attention: 0 },
      { agent_id: P, block: 'garden', spindle: null, pscale_attention: -1 },
      { agent_id: P, block: 'garden', face: 'character', tier: 'soft', spindle: '2' },
    ],
  },
  {
    name: 'reads and writes on a floor-3 block',
    seed: [{ block: 'deep', content: deep }],
    calls: [
      { agent_id: P, block: 'deep' },
      { agent_id: P, block: 'deep', pscale_attention: 0 },
      { agent_id: P, block: 'deep', pscale_attention: 2 },
      { agent_id: P, block: 'deep', pscale_attention: -1 },
      { agent_id: P, block: 'deep', spindle: '1' },
      { agent_id: P, block: 'deep', spindle: '100' },
      { agent_id: P, block: 'deep', spindle: '123' },
      { agent_id: P, block: 'deep', spindle: '123.4' },
      { agent_id: P, block: 'deep', spindle: '123.45', pscale_attention: 0 },
      { agent_id: P, block: 'deep', spindle: '123.45', pscale_attention: 2 },
      { agent_id: P, block: 'deep', spindle: '123.45', pscale_attention: -1 },
      { agent_id: P, block: 'deep', spindle: '120', pscale_attention: -2 },
      { agent_id: P, block: 'deep', spindle: '1.2' },
      { agent_id: P, block: 'deep', spindle: '020' },
      { agent_id: P, block: 'deep', spindle: '02' },
      { agent_id: P, block: 'deep', spindle: '3' },
      { agent_id: P, block: 'deep', spindle: '300' },
      { agent_id: P, block: 'deep', spindle: '1234.5' },
      { agent_id: P, block: 'deep', spindle: '0*' },
      { agent_id: P, block: 'deep', spindle: '100', pscale_attention: -1 },
      { agent_id: P, block: 'deep', spindle: '1', content: 'written through the padding' },
      { agent_id: P, block: 'deep', spindle: '300', content: 'three hundred, revised' },
      { agent_id: P, block: 'deep', spindle: '123.46', content: 'a new one beside the deeper' },
      { agent_id: P, block: 'deep', spindle: '1.2', content: 'grows beneath a string in the era' },
      { agent_id: P, block: 'deep', spindle: '0' },
    ],
  },
  {
    name: 'writes on a floor-1 block',
    seed: [{ block: 'garden', content: garden }],
    calls: [
      { agent_id: P, block: 'garden', spindle: '4', content: 'Late autumn — mulch' },
      { agent_id: P, block: 'garden', spindle: '2', content: 'Summer — water at dawn' },
      { agent_id: P, block: 'garden', spindle: '1', content: 'Spring, re-voiced' },
      { agent_id: P, block: 'garden', spindle: '2.1', content: 'shade the lettuce' },
      { agent_id: P, block: 'garden', spindle: '5', content: { _: 'The allotment', 1: 'beans', 2: { _: 'onions', 1: 'red' } } },
      { agent_id: P, block: 'garden', spindle: '1', content: { _: 'Spring replaced', 1: 'only this now' } },
      { agent_id: P, block: 'garden', spindle: '1.2', pscale_attention: 0, content: 'lands one rung up' },
      { agent_id: P, block: 'garden', spindle: '1', pscale_attention: 0, content: { _: 'an object at a point' } },
      { agent_id: P, block: 'garden', spindle: '3', pscale_attention: -1, content: 'a string at a subtree' },
      { agent_id: P, block: 'garden', pscale_attention: 0, content: [{ address: '6', content: 'six' }, { address: '7', content: 'seven' }, { address: '7.1', content: 'seven one' }] },
      { agent_id: P, block: 'garden', pscale_attention: 0, content: { 8: 'eight', '8.1': 'eight one' } },
      { agent_id: P, block: 'garden', pscale_attention: 0, content: 'not a disc' },
      { agent_id: P, block: 'garden', spindle: '3*', content: { _: 'a new hidden voice', 1: 'new hidden one' } },
      { agent_id: P, block: 'garden', spindle: '3*', content: 'a string into a star' },
      { agent_id: P, block: 'garden', spindle: '3*1', content: 'star inside a write' },
      { agent_id: P, block: 'garden', spindle: '9*', content: { _: 'into a string' } },
      { agent_id: P, block: 'garden', spindle: '4', content: { a: 'a key the spine refuses' } },
      { agent_id: P, block: 'garden', spindle: '4', content: '{"1":"from a string of JSON"}' },
      { agent_id: P, block: 'garden', spindle: '4', content: '{not quite json' },
      { agent_id: P, block: 'garden', spindle: '4', content: '[1,2]' },
      { agent_id: P, block: 'garden', spindle: '0', content: 'Garden, renamed' },
      { agent_id: P, block: 'garden', spindle: '6', content: 42 },
      { agent_id: P, block: 'garden', spindle: '6', content: null },
      { agent_id: P, block: 'garden', spindle: '6', content: true },
      { agent_id: P, block: 'garden', spindle: '6', content: '' },
      { agent_id: P, block: 'garden', spindle: '1.2.3', content: 'x' },
      { agent_id: P, block: 'garden', spindle: '12.3', content: 'x' },
      { agent_id: P, block: 'garden', spindle: '9.9.9', content: { _: 'x' } },
      { agent_id: P, block: 'garden' },
      { agent_id: P, block: 'garden', content: { _: 'Garden, a new start', 1: 'one line' } },
      { agent_id: P, block: 'garden', content: { 1: 'no floor' } },
      { agent_id: P, block: 'garden', content: 'just a string' },
      { agent_id: P, block: 'garden', content: { _: { _: 'grown a floor', 1: 'old one' }, 1: 'new one' } },
      { agent_id: P, block: 'garden', spindle: '1' },
    ],
  },
  {
    name: 'new blocks and the index',
    seed: [],
    calls: [
      { agent_id: P },
      { agent_id: P, block: 'notes', spindle: '1', content: 'first note' },
      { agent_id: P, block: 'notes2', spindle: '1.2', content: 'born two deep' },
      { agent_id: P, block: 'whole', content: { _: 'Whole and new', 1: 'a' } },
      { agent_id: P, block: 'bad', content: { 1: 'x' } },
      { agent_id: P, block: 'disc-born', pscale_attention: 0, content: [{ address: '1', content: 'a' }] },
      { agent_id: P, block: 'obj-born', spindle: '2', content: { _: 'an object makes a block', 1: 'b' } },
      { agent_id: P, block: 'shell:matthew', spindle: '1', content: 'a colon in the name' },
      { agent_id: P, block: 'a b', spindle: '1', content: 'a space in the name' },
      { agent_id: P, block: 'ü-name', spindle: '1', content: 'a letter beyond ascii' },
      { agent_id: P, block: '123', spindle: '1', content: 'digits for a name' },
      { agent_id: P, block: 'lighthouse', content: { _: 'The lighthouse.' } },
      { agent_id: P },
      { agent_id: P, block: 'notes' },
      { agent_id: P, block: 'notes', spindle: '2', content: 'second note' },
      { agent_id: P, block: '', spindle: '1', content: 'no name' },
      { agent_id: P, block: '', content: 'no name, appended', append: true },
      { agent_id: P, block: 'nothing' },
      { agent_id: P, block: 'nothing', spindle: '1' },
      { agent_id: P, block: 'nothing', pscale_attention: 0 },
      { agent_id: P },
    ],
  },
  {
    name: 'appends at the root',
    seed: [{ block: 'log', content: log9 }],
    calls: [
      { agent_id: P, block: 'log', content: 'the tenth entry wraps the root', append: true },
      { agent_id: P, block: 'log', content: 'the eleventh', append: true },
      { agent_id: P, block: 'log', pscale_attention: 0 },
      { agent_id: P, block: 'log', spindle: '10', content: 'the first nine: a week of entries' },
      { agent_id: P, block: 'log', content: 'the twelfth, nothing owed', append: true },
      { agent_id: P, block: 'log', content: { _: 'an object entry', 1: 'matthew' }, append: true },
      { agent_id: P, block: 'log', content: { _: 'already dated', 3: '2026-01-01T00:00:00Z' }, append: true },
      { agent_id: P, block: 'log', content: { _: 'a tag at three', 3: 'tag' }, append: true },
      { agent_id: P, block: 'log', content: ['an', 'array'], append: true },
      { agent_id: P, block: 'log', content: '', append: true },
      { agent_id: P, block: 'log', content: '   ', append: true },
      { agent_id: P, block: 'log', append: true },
      { agent_id: P, block: 'log', content: { _: 'bad', x: 1 }, append: true },
      { agent_id: P, block: 'log', content: '{"1":"json in a string"}', append: true },
      { agent_id: P, block: 'log', content: 'with an aperture', pscale_attention: 0, append: true },
      { agent_id: P, block: 'log', spindle: '10' },
      { agent_id: P, block: 'log', spindle: '1' },
      { agent_id: P, block: 'journal', content: 'a journal is born by its first line', append: true },
      { agent_id: P, block: 'journal', content: 'and grows', append: true },
      { agent_id: P, block: 'journal' },
    ],
  },
  {
    name: 'appends beneath a node',
    seed: [{ block: 'shelf', content: shelf }],
    calls: [
      { agent_id: P, block: 'shelf', spindle: '1', content: 'marmalade', append: true },
      { agent_id: P, block: 'shelf', spindle: '2', content: 'one too many', append: true },
      { agent_id: P, block: 'shelf', spindle: '4', content: 'one spice too many', append: true },
      { agent_id: P, block: 'shelf', spindle: '3', content: 'beneath prose', append: true },
      { agent_id: P, block: 'shelf', spindle: '7', content: 'no node here', append: true },
      { agent_id: P, block: 'shelf', spindle: '1.2', content: 'beneath a string', append: true },
      { agent_id: P, block: 'shelf', spindle: '1*', content: 'a star', append: true },
      { agent_id: P, block: 'shelf', spindle: '1.2.3', content: 'multi-dot', append: true },
      { agent_id: P, block: 'shelf', spindle: '0', content: 'the root underscore', append: true },
      { agent_id: P, block: 'shelf', spindle: '1', content: { _: 'with fields', 1: 'matthew' }, append: true },
      { agent_id: P, block: 'nosuch', spindle: '1', content: 'no block', append: true },
      { agent_id: P, block: 'shelf', spindle: '1' },
      { agent_id: P, block: 'shelf', spindle: '1', pscale_attention: -1 },
    ],
  },
  {
    name: 'the second wrap',
    seed: [{ block: 'longlog', content: logFull }],
    calls: [
      { agent_id: P, block: 'longlog', pscale_attention: 0 },
      { agent_id: P, block: 'longlog', content: 'the ninety-first entry', append: true },
      { agent_id: P, block: 'longlog', content: 'the ninety-second', append: true },
      { agent_id: P, block: 'longlog', spindle: '100' },
      { agent_id: P, block: 'longlog', spindle: '111' },
      { agent_id: P, block: 'longlog', spindle: '1', pscale_attention: 0 },
      { agent_id: P, block: 'longlog', pscale_attention: 1 },
    ],
  },
  {
    name: 'summaries owed',
    seed: [{ block: 'owed', content: logOwed }],
    calls: [
      { agent_id: P, block: 'owed', content: 'the next line', append: true },
      { agent_id: P, block: 'owed', spindle: '10', content: 'the first nine, summarised' },
      { agent_id: P, block: 'owed', content: 'and another', append: true },
      { agent_id: P, block: 'owed', spindle: '22' },
    ],
  },
  {
    name: 'stamps and marks',
    seed: [{ block: 'marks', content: marks }],
    calls: [
      { agent_id: P, block: 'marks' },
      { agent_id: P, block: 'marks', pscale_attention: 0 },
      { agent_id: P, block: 'marks', spindle: '1' },
      { agent_id: P, block: 'marks', spindle: '1', pscale_attention: -1 },
      { agent_id: P, block: 'marks', spindle: '2', pscale_attention: 0 },
      { agent_id: P, block: 'marks', spindle: '4' },
      { agent_id: P, block: 'marks', content: { _: 'a third mark', 1: 'matthew', 2: '' }, append: true },
    ],
  },
  {
    name: 'numbers and switches',
    seed: [{ block: 'scores', content: scores }],
    calls: [
      { agent_id: P, block: 'scores' },
      { agent_id: P, block: 'scores', pscale_attention: 0 },
      { agent_id: P, block: 'scores', pscale_attention: -1 },
      { agent_id: P, block: 'scores', spindle: '4.2' },
      { agent_id: P, block: 'scores', spindle: '1', pscale_attention: 0 },
      { agent_id: P, block: 'scores', spindle: '4', pscale_attention: -1 },
      { agent_id: P, block: 'scores', spindle: '1', content: 'five, in words' },
      { agent_id: P, block: 'scores', spindle: '1.1', content: 'beneath a number' },
    ],
  },
  {
    name: 'unicode',
    seed: [{ block: 'uni', content: uni }],
    calls: [
      { agent_id: P, block: 'uni' },
      { agent_id: P, block: 'uni', pscale_attention: 0 },
      { agent_id: P, block: 'uni', spindle: '2' },
      { agent_id: P, block: 'uni', spindle: '4', pscale_attention: -1 },
      { agent_id: P, block: 'uni', spindle: '5', content: 'tide 🌊 and hedgehog 🦔, ß and ﬁ' },
      { agent_id: P, block: 'uni', content: '  ✦  ', append: true },
      { agent_id: P },
    ],
  },
  {
    name: 'long blocks',
    seed: [{ block: 'big', content: big }, { block: 'wide', content: wide }],
    calls: [
      { agent_id: P, block: 'big' },
      { agent_id: P, block: 'big', spindle: '0' },
      { agent_id: P, block: 'big', spindle: '1.1' },
      { agent_id: P, block: 'big', pscale_attention: -1 },
      { agent_id: P, block: 'wide' },
      { agent_id: P, block: 'wide', spindle: '0' },
      { agent_id: P, block: 'wide', pscale_attention: -2 },
      { agent_id: P, block: 'wide', spindle: '5', pscale_attention: -2 },
      { agent_id: P, block: 'wide', spindle: '55' },
    ],
  },
  {
    name: 'a block with no floor',
    seed: [{ block: 'floorless', content: { 1: 'no floor here', 2: { 1: 'deeper' } }, raw: true }],
    calls: [
      { agent_id: P, block: 'floorless' },
      { agent_id: P, block: 'floorless', pscale_attention: 0 },
      { agent_id: P, block: 'floorless', pscale_attention: -1 },
      { agent_id: P, block: 'floorless', spindle: '2.1' },
      { agent_id: P, block: 'floorless', spindle: '3', content: 'a write heals the floor' },
      { agent_id: P, block: 'floorless' },
      { agent_id: P },
    ],
  },
  {
    name: 'a floorless accumulator',
    seed: [{ block: 'heap', content: { 1: 'one', 2: 'two' }, raw: true }],
    calls: [
      { agent_id: P, block: 'heap', content: 'appended to a floorless heap', append: true },
      { agent_id: P, block: 'heap' },
    ],
  },
  {
    name: 'presence',
    seed: [],
    calls: [
      { agent_id: P, block: 'presence', spindle: '1', content: { 1: 'matthew', 2: 'garden', 3: '2026-01-01T00:00:00Z' } },
      { agent_id: P, block: 'presence', spindle: '2', content: { 1: 'matthew', 2: 'kitchen', 3: '2027-01-01T00:00:00Z' } },
      { agent_id: P, block: 'presence' },
    ],
  },
  {
    name: 'the teaching shelf',
    seed: [],
    calls: [
      { agent_id: S },
      { agent_id: S, block: '' },
      { agent_id: S, block: 'whetstone' },
      { agent_id: S, block: 'whetstone', spindle: '0' },
      { agent_id: S, block: 'whetstone', spindle: '2' },
      { agent_id: S, block: 'whetstone', spindle: '2', pscale_attention: -1 },
      { agent_id: S, block: 'whetstone', spindle: '2.8', pscale_attention: -9 },
      { agent_id: S, block: 'whetstone', spindle: '1.3', pscale_attention: -1 },
      { agent_id: S, block: 'whetstone', spindle: '9.4', pscale_attention: 0 },
      { agent_id: S, block: 'whetstone', pscale_attention: -1 },
      { agent_id: S, block: 'whetstone', spindle: '2.81111111' },
      { agent_id: S, block: 'sunstone' },
      { agent_id: S, block: 'sunstone', spindle: '1.1' },
      { agent_id: S, block: 'sunstone', spindle: '1', pscale_attention: -1 },
      { agent_id: S, block: 'sunstone', pscale_attention: 0 },
      { agent_id: S, block: 'sunstone', spindle: '7*' },
      { agent_id: S, block: 'sunstone', spindle: '7*1' },
      { agent_id: S, block: 'sunstone', spindle: '1.6', pscale_attention: -2 },
      { agent_id: S, block: 'block-conventions', spindle: '3.5' },
      { agent_id: S, block: 'block-conventions', spindle: '4.22' },
      { agent_id: S, block: 'manifest', pscale_attention: 0 },
      { agent_id: S, block: 'protocol-paywall', pscale_attention: 0 },
      { agent_id: S, block: 'sundial' },
      { agent_id: S, block: 'bsp-test', spindle: '2.1' },
      { agent_id: S, block: 'nosuch' },
      { agent_id: S, block: 'whetstone', spindle: '1', content: 'a write to the shelf' },
      { agent_id: S, block: 'whetstone', content: 'an append to the shelf', append: true },
      { agent_id: S, block: 'whetstone', spindle: '1.2.3' },
    ],
  },
];

// 0.8, the hatch: the calls the phone's hatch makes, exactly as its Pages plan
// them, written by bsp's HatchGoldenTest (HATCH_WRITE=1) and held to it there.
cases.push(...JSON.parse(readFileSync(new URL('./hatch-case.json', import.meta.url), 'utf8')));
