// random.mjs — generated cases, from a fixed seed so every run makes the same
// ones. They reach the paths a hand-made corpus misses: random shapes at
// floors 1-3, hidden directories, numeric leaves, and random calls of every
// kind against them, valid and not. Every generated line holds a space, so no
// node can read as a bundle of references (bundle windows are not carried).

function rng(seed) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const WORDS = ['tide', 'shell', 'stone', 'sand', 'wave', 'kelp', 'gull', 'crab', 'salt', 'drift',
  'mère', 'café', 'ünder', '海', '砂', '🌊', '🦔', 'ﬁne', 'ß', 'a b', 'tab\there', 'line\nbreak', 'quote"d', 'back\\slash'];

export function randomCases(count, seed = 20261028) {
  const r = rng(seed);
  const pick = (xs) => xs[Math.floor(r() * xs.length)];
  const int = (lo, hi) => lo + Math.floor(r() * (hi - lo + 1));
  const chance = (p) => r() < p;

  const line = () => {
    const n = int(2, chance(0.1) ? 40 : 6);
    return Array.from({ length: n }, () => pick(WORDS)).join(' ');
  };
  const leaf = () => {
    if (chance(0.05)) return pick([0, 7, -3.5, 1e-7, 2.5e21, true, false, null]);
    return line();
  };
  const node = (depth) => {
    if (depth <= 0 || chance(0.55)) return leaf();
    const o = {};
    if (chance(0.85)) o._ = line();
    if (chance(0.06)) o._ = { _: line(), 1: line(), 2: line() };
    const kids = int(0, 5);
    for (let i = 0; i < kids; i++) o[String(int(1, 9))] = node(depth - 1);
    return o;
  };
  const block = () => {
    const floor = pick([1, 1, 1, 2, 3]);
    let root = line();
    for (let f = 1; f < floor; f++) {
      const era = { _: root };
      if (chance(0.5)) era[String(int(1, 9))] = node(1);
      root = era;
    }
    const b = { _: root };
    const kids = int(1, 6);
    for (let i = 0; i < kids; i++) b[String(int(1, 9))] = node(3);
    return b;
  };
  const spindle = () => {
    if (chance(0.12)) return null;
    const len = int(1, 5);
    let s = '';
    for (let i = 0; i < len; i++) s += String(chance(0.12) ? 0 : int(1, 9));
    if (chance(0.35)) {
      const at = int(0, s.length);
      s = s.slice(0, at) + '.' + s.slice(at);
    }
    if (chance(0.04)) s += '.' + int(1, 9);
    if (chance(0.08)) s += '*' + (chance(0.5) ? String(int(1, 9)) : '');
    return s;
  };
  const pscale = () => (chance(0.45) ? null : int(-4, 3));
  const content = () => {
    const k = r();
    if (k < 0.55) return line();
    if (k < 0.85) return node(2);
    if (k < 0.92) return JSON.stringify(node(2));
    return pick([42, null, true, '', ['a list', 'of lines'], { _: line(), x: line() }]);
  };

  const cases = [];
  for (let c = 0; c < count; c++) {
    const name = pick(['tide', 'rock pool', 'shell:matthew', 'dune', 'log']);
    const seed = chance(0.9) ? [{ block: name, content: block() }] : [];
    const calls = [];
    const n = int(1, 4);
    for (let i = 0; i < n; i++) {
      const k = r();
      const call = { agent_id: 'pocket', block: chance(0.93) ? name : pick(['other', 'tide']) };
      if (k < 0.5) {
        const s = spindle();
        const p = pscale();
        if (s !== null) call.spindle = s;
        if (p !== null) call.pscale_attention = p;
      } else if (k < 0.8) {
        const s = spindle();
        if (s !== null) call.spindle = s;
        if (chance(0.2)) call.pscale_attention = int(-3, 1);
        if (s === null && chance(0.5)) {
          call.pscale_attention = 0;
          call.content = Array.from({ length: int(1, 3) }, () => ({ address: pick(['1', '2.3', '45', '0', '9.9', '1.2.3']), content: line() }));
        } else {
          call.content = content();
        }
      } else {
        call.append = true;
        call.content = content();
        if (chance(0.3)) call.spindle = pick(['1', '2', '3', '12', '0', '9']);
      }
      calls.push(call);
    }
    cases.push({ name: `random ${c + 1}`, seed, calls });
  }
  return cases;
}
