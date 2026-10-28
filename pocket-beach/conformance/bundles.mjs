// bundles.mjs — the hand-chosen calls that meet bundles: nodes whose digit
// children are references, read through an ordinary path-walk, and the
// manifest that follows an append. They run after the random cases, so the
// cases before them keep their clocks.
//
// A seed's content may be a function of the moment the seed is made and the
// original's temporal module, so a clock block can hold lines at the beat its
// calls are made in.
//
// `secret` on a call is given to the original only, never recorded: upstream
// moves a manifest's reference after an append only under a secret, and the
// pocket, which keeps no secrets, always does (decision 12). The block it is
// given on is unlocked, and the beach accepts a secret there.

const P = 'pocket';
const SECRET = 'golden-only-key-20261028';

const garden = {
  _: 'Garden at the back of the house.',
  1: {
    _: 'Spring — the beds wake',
    1: 'sow peas by the fence',
    2: { _: 'tulips along the path', 1: 'red ones first', 2: 'yellow after' },
    3: 'turn the compost',
  },
  2: 'Summer — water at dusk',
  9: 'Winter — rest the soil',
};

const stamped = (text, day) => ({ _: text, 1: 'mira', 3: `2026-10-${String(day).padStart(2, '0')}T10:00:00Z` });

// Folded: two spans, the newest entry at 2.3.
const log = { _: 'Log of the garden.', 1: { _: 'the first nine, in short: sowing and watering' }, 2: { _: 'the second span, in short: the frost' } };
for (let i = 1; i <= 9; i++) log[1][i] = stamped(`line ${i} of the first span`, i);
for (let i = 1; i <= 3; i++) log[2][i] = stamped(`line ${i} of the second span`, 10 + i);

// Not yet folded: the head is the greatest digit.
const marks = { _: 'Marks left.', 1: stamped('first mark', 1), 2: stamped('second mark', 2), 3: stamped('third mark', 3) };

// Large enough that a bare reference takes its disc.
const almanac = { _: 'An almanac of the year, month by month, at length.' };
for (let i = 1; i <= 9; i++) {
  almanac[i] = { _: `Month ${i} of the almanac — ${'what the month asks of the garden, and what it gives back. '.repeat(6)}` };
  for (let j = 1; j <= 3; j++) almanac[i][j] = `week ${j} of month ${i}: ${'a long note on the work of the week. '.repeat(3)}`;
}

// Nine blocks a little under the whole-block threshold: their windows overflow.
const fillers = [];
for (let n = 1; n <= 9; n++) {
  const b = { _: `Filler block ${n}, read whole.` };
  for (let i = 1; i <= 9; i++) b[i] = `line ${i} of filler ${n}: ${'words that fill the window line by line. '.repeat(14)}`;
  fillers.push({ block: `filler${n}`, content: b });
}

// A floor-10 clock: the underscore chain ten deep, then lines put at the
// addresses of moments around the seed's own.
function clockAround(ms, t) {
  let chain = 'The clock of the beds — what was said, by when';
  for (let i = 1; i < 10; i++) chain = { _: chain };
  const block = { _: chain };
  // `digits` is the walk itself: no padding, no point (0 is the underscore step).
  const put = (digits, value) => {
    let node = block;
    for (let i = 0; i < digits.length - 1; i++) {
      const key = digits[i] === '0' ? '_' : digits[i];
      if (typeof node[key] === 'string') node[key] = { _: node[key] };
      else if (!node[key] || typeof node[key] !== 'object') node[key] = {};
      node = node[key];
    }
    const last = digits[digits.length - 1] === '0' ? '_' : digits[digits.length - 1];
    if (node[last] && typeof node[last] === 'object' && typeof value === 'string') node[last]._ = value;
    else node[last] = value;
  };
  const at = (offset, fine) => t.momentToAddress(new Date(ms + offset), fine).replace('.', '').split('');
  put(at(0, 0).slice(0, 4), 'the year of the new beds');
  put(at(0, 0).slice(0, 8), 'this day: the frost came and went');
  put(at(-3 * 3600_000, 0), 'a beat three hours before: the hose was mended');
  put(at(-3 * 3600_000, 3), 'said three hours before');
  put(at(0, 0), 'this beat: watering the beds');
  put(at(-90_000, 3), 'said a minute and a half before');
  put(at(500, 3), { _: 'an entry, stamped, half a second in', 1: 'mira', 3: new Date(ms + 500).toISOString() });
  put(at(2_500, 3), 'said two and a half seconds in');
  put(at(3_500, 3), 'said three and a half seconds in');
  put(at(600_000, 3), 'said ten minutes on, after the calls');
  return block;
}

export const cases = [
  {
    name: 'bundles: a shell manifest read through bsp',
    seed: [
      { block: 'garden', content: garden },
      { block: 'notes:mira', content: { _: "Mira's own notes", 1: 'the seeds came on Tuesday', 2: 'ask about the frost cloth' } },
      { block: 'notes', content: { _: 'Notes anyone keeps', 1: 'a shared line' } },
      { block: 'log:mira', content: log },
      { block: 'marks', content: marks },
      {
        block: 'shell:mira',
        content: {
          _: "Mira — a gardener's shell",
          1: 'Mira keeps a garden at the back of the house.',
          2: 'She writes short.',
          3: {
            _: 'what Mira holds, by address',
            1: 'notes',
            2: 'garden:1.2',
            3: 'garden:1:-1',
            4: 'whetstone:2:-1',
            5: 'log:head',
            6: 'marks:head:0',
            7: 'missing',
            8: 'and one line of prose',
            9: 'garden:0:0',
          },
        },
      },
    ],
    calls: [
      { agent_id: P, block: 'shell:mira', spindle: '3' },
      { agent_id: P, block: 'shell:mira', spindle: ' 3 ' },
      { agent_id: P, block: 'shell:mira', spindle: '3.1' },
      { agent_id: P, block: 'shell:mira', spindle: '3', pscale_attention: 0 },
      { agent_id: P, block: 'shell:mira', spindle: '3', pscale_attention: -1 },
      { agent_id: P, block: 'shell:mira' },
      { agent_id: P, block: 'shell:mira', spindle: '0' },
      { agent_id: P, block: 'shell:mira', spindle: '1' },
      { agent_id: P, block: 'garden', spindle: '1' },
    ],
  },
  {
    name: 'bundles: names, words and notes',
    seed: [
      { block: 'garden', content: garden },
      { block: 'notes', content: { _: 'Notes anyone keeps', 1: 'a shared line' } },
      { block: 'notes:mira', content: { _: "Mira's own notes", 1: 'the seeds came on Tuesday' } },
      { block: 'empty', content: { _: 'nothing written yet' } },
      { block: 'almanac', content: almanac },
      { block: 'sunstone', content: { _: 'a pocket block named as the shelf names one' } },
      {
        block: 'desk',
        content: {
          _: 'The desk.',
          1: 'a line about the desk',
          2: {
            _: 'what the desk holds',
            1: 'notes',
            2: 'notes:mira:1',
            3: 'garden:now',
            4: 'empty:head',
            5: 'garden:9.5',
            6: 'garden:1.2:-5',
            7: 'almanac',
            8: 'sunstone',
            9: 'GARDEN',
          },
          3: {
            _: 'the genus dialect: references voiced as objects',
            1: { _: 'garden', 1: 'a condition beneath' },
            2: { _: 'notes:1' },
            3: 'the third is prose',
          },
          4: { _: 'mostly prose', 1: 'garden', 2: 'a line', 3: 'another line', 4: 'notes' },
          5: { _: 'references to nothing that stands', 1: 'nosuch', 2: 'nosuch:1', 3: 'nowhere:head' },
          6: { _: 'an entry is never a bundle', 1: 'garden', 2: 'notes:1', 3: '2026-10-01T10:00:00Z' },
          7: { _: 'words with odd attention', 1: 'garden:NOW', 2: 'garden:Head:2', 3: 'notes:head:x', 4: 'garden:1:2:3', 5: 'notes:mira:head' },
          8: { _: 'a lone reference', 1: 'garden:9' },
        },
      },
    ],
    calls: [
      { agent_id: P, block: 'desk', spindle: '2' },
      { agent_id: P, block: 'desk', spindle: '3' },
      { agent_id: P, block: 'desk', spindle: '4' },
      { agent_id: P, block: 'desk', spindle: '5' },
      { agent_id: P, block: 'desk', spindle: '6' },
      { agent_id: P, block: 'desk', spindle: '7' },
      { agent_id: P, block: 'desk', spindle: '8' },
      { agent_id: P, block: 'desk', spindle: '1' },
    ],
  },
  {
    name: 'bundles: the window fills',
    seed: [
      ...fillers,
      {
        block: 'reading',
        content: { _: 'Everything at once.', 1: { _: 'all nine, whole', 1: 'filler1', 2: 'filler2', 3: 'filler3', 4: 'filler4', 5: 'filler5', 6: 'filler6', 7: 'filler7', 8: 'filler8', 9: 'filler9' } },
      },
    ],
    calls: [
      { agent_id: P, block: 'reading', spindle: '1' },
      { agent_id: P, block: 'reading', spindle: '1', pscale_attention: 0 },
    ],
  },
  {
    name: 'bundles: the clock now',
    seed: [
      { block: 'diary', content: { _: 'A diary on floor one.', 1: 'first day' } },
      { block: 'clock', content: clockAround },
      {
        block: 'shell:kit',
        content: {
          _: "Kit's shell",
          3: { _: 'what Kit holds', 1: 'clock:now', 2: 'clock:now:-1', 3: 'diary:now', 4: 'clock:now:3', 5: 'garden:now' },
        },
      },
    ],
    calls: [
      { agent_id: P, block: 'shell:kit', spindle: '3' },
      { agent_id: P, block: 'shell:kit', spindle: '3' },
      { agent_id: P, block: 'shell:kit', spindle: '3' },
      { agent_id: P, block: 'clock', pscale_attention: 0 },
    ],
  },
  {
    name: 'bundles: the manifest follows an append',
    seed: [
      { block: 'history:mira', content: { _: 'What Mira did.', 1: stamped('planted the bulbs', 1) } },
      { block: 'notes:mira', content: { _: "Mira's own notes", 1: 'a note' } },
      { block: 'history:nobody', content: { _: 'No shell dials this.' } },
      {
        block: 'shell:mira',
        content: {
          _: "Mira's shell",
          3: { _: 'what Mira holds', 1: 'history:mira:1:-1', 2: 'notes', 3: 'notes:mira:1:0', 4: 'history:mira:1:-1' },
        },
      },
    ],
    calls: [
      { agent_id: P, block: 'shell:mira', spindle: '3' },
      { agent_id: P, block: 'history:mira', content: 'watered the beds', append: true, secret: SECRET },
      { agent_id: P, block: 'shell:mira', spindle: '3' },
      { agent_id: P, block: 'history:mira', content: { _: 'mended the hose', 1: 'mira' }, append: true, secret: SECRET },
      { agent_id: P, block: 'notes:mira', content: 'another note', append: true, secret: SECRET },
      { agent_id: P, block: 'history:mira', spindle: '3', content: 'beneath the third', append: true, secret: SECRET },
      { agent_id: P, block: 'history:nobody', content: 'nothing follows', append: true, secret: SECRET },
      { agent_id: P, block: 'shell:mira', spindle: '3' },
      { agent_id: P, block: 'shell:mira' },
    ],
  },
];
