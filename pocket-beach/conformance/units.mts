/**
 * units.mts — the bundle, clock and seat functions called directly.
 *
 * The golden calls reach a bundle only through bsp(); these call the original's
 * own functions in src/bundle.ts, src/temporal.ts and src/orientation.ts with
 * fixed inputs — a fixture loader, a fixed now and since, a fake rider io — so
 * every branch is checked whether or not a call can reach it. Inputs come from
 * a fixed seed, so every run makes the same ones. The Kotlin test
 * (BundleUnitsTest) runs the same inputs through its port and must give the
 * same outputs.
 */
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';

function rng(seed: number) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/**
 * The original tags every sundial address it prints on a floor-10 block with
 * its relation to now — "[2026000000] (now — this year)", "(AHEAD — tomorrow)",
 * "(3 days behind)" — inside formatRead itself. Those are the "days ago /
 * AHEAD" tags the pocket leaves out of every reply (decision 5), so they are
 * taken out of the reference text here, and nothing else is.
 */
export function stripRelations(text: string): string {
  return text.replace(/(\[\d{10}(?:\.\d{1,3})?\]) \((?:now — |AHEAD — )?[^()\n]*\)/g, '$1');
}

const threw = (f: () => any) => {
  try { return f(); } catch (e: any) { return `[threw] ${e?.message ?? String(e)}`; }
};

export async function unitGoldens(routerDir: string, RealDate: DateConstructor) {
  const imp = (p: string) => import(pathToFileURL(join(routerDir, p)).href);
  const B = await imp('src/bundle.ts');
  const T = await imp('src/temporal.ts');
  const O = await imp('src/orientation.ts');
  const { readAt } = await imp('src/bsp.ts');
  const { bspRead } = await imp('src/bsp-fn.ts');
  const r = rng(20261028);
  const pick = <X>(xs: X[]): X => xs[Math.floor(r() * xs.length)];
  const D = (ms: number) => new RealDate(ms);

  // ── the clock ──
  const instants: number[] = [
    RealDate.UTC(1000, 0, 1), RealDate.UTC(1000, 0, 1) - 1, RealDate.UTC(9999, 11, 31, 23, 59, 59, 999), RealDate.UTC(10000, 0, 1),
    RealDate.UTC(2026, 9, 28, 8, 0, 0), RealDate.UTC(2026, 9, 28, 7, 59, 59, 999), RealDate.UTC(2026, 9, 28, 16, 0, 0, 1),
    RealDate.UTC(2024, 1, 29, 12), RealDate.UTC(2026, 0, 31, 23, 59, 59, 999), RealDate.UTC(1969, 11, 31, 23, 59, 59, 999), 0,
  ];
  for (let i = 0; i < 400; i++) instants.push(Math.floor(RealDate.UTC(1900, 0, 1) + r() * (RealDate.UTC(2200, 0, 1) - RealDate.UTC(1900, 0, 1))));
  // Near beat edges: a part is 9,600 s, a beat a ninth of that, and the fine rungs ninths again.
  for (let i = 0; i < 120; i++) {
    const day = RealDate.UTC(2026, Math.floor(r() * 12), 1 + Math.floor(r() * 28));
    const edge = Math.round(Math.floor(r() * 729) * (86_400_000 / 729));
    instants.push(day + edge + Math.floor(r() * 5) - 2);
  }
  const moments = instants.map((ms) => [ms, ...[0, 1, 2, 3, 4].map((f) => threw(() => T.momentToAddress(D(ms), f)))]);

  const addresses = new Set<string>();
  for (const [, , , , ten] of moments) {
    if (typeof ten !== 'string' || ten.startsWith('[')) continue;
    const [beat, fine] = ten.split('.');
    for (let d = 1; d <= 10; d++) addresses.add(beat.slice(0, d).padEnd(10, '0'));
    for (let f = 1; f <= fine.length; f++) addresses.add(`${beat}.${fine.slice(0, f)}`);
  }
  const odd = ['0000000000', '2026000000', '2026410103', '2026990000', '2026419900', '2026451900', '2026000001', '2026410000.1', '2026413179.0',
    '2026413179.1234', '202641317', '2026413179.', 'abc', '', '9999999999', '1000000000', '2026413179.999', '2026413179.919', '0990000000', '0050000000'];
  const addrs = [...[...addresses].filter(() => r() < 0.15), ...odd];
  const voices = addrs.map((a) => [a, threw(() => T.voiceAddress(a))]);
  const spans = addrs.map((a) => [a, threw(() => { const s = T.addressToSpan(a); return [s.start.getTime(), s.end.getTime(), s.pscale]; })]);

  const stampsOf = moments.map((m) => m[4]).filter((s) => typeof s === 'string' && !s.startsWith('[')) as string[];
  const divergence: any[] = [];
  for (let i = 0; i < 300; i++) {
    const a = pick(stampsOf);
    let b = pick(stampsOf);
    if (r() < 0.6) {
      const keep = Math.floor(r() * 14);
      b = a.slice(0, keep) + b.slice(keep);
    }
    const form = (s: string) => pick([s, s.slice(0, 10), s.slice(0, 7), ` ${s} `, s.replace('.', ''), `${s}0`, `x${s}`]);
    const since = form(a), now = form(b);
    divergence.push([since, now, B.divergencePscale(since, now)]);
  }
  divergence.push(['2026413179.123', '2026413179.123', B.divergencePscale('2026413179.123', '2026413179.123')]);
  divergence.push(['2026413179', '2026413179.111', B.divergencePscale('2026413179', '2026413179.111')]);
  const clockStamps = ['', '2026', '2026413179', '2026413179.1', '2026413179.120', '2026413179.123456', '2026-04-13 1.7', '..', '12345678901234', '2026413179.000', ' 2026413179.12 ']
    .concat(divergence.slice(0, 60).map((d) => d[0])).map((s) => [s, B.clockStamp(s)]);
  const rungs = [];
  for (let p = -5; p <= 12; p++) rungs.push([p, B.rungName(p)]);

  // ── references and forms ──
  const segs = ['garden', 'Garden', 'notes', 'n0-te_s', 'shell', 'mira', '9lives', '', 'now', 'NOW', 'Head', 'head', '1', '0', '1.2', '12.34', '1.2.3', '-1', '-0', '7', 'x', '05', '1.', '.5', 'é', '99999999999'];
  const leaves = new Set<string>(['', ' ', 'garden', ' garden ', 'garden notes', 'garden\tnotes', 'a'.repeat(200), 'a'.repeat(201), 'garden:', ':garden', 'garden::1',
    'now', 'head', 'garden:now', 'garden:now:-2', 'garden:now:-2:3', 'garden:head:x', 'shell:mira:now', 'shell:9:now', 'garden:1:-1', 'garden:0', 'garden:0:0',
    'garden:1.2.3', 'garden:1:2:3', 'the third is prose', 'garden notes', 'garden ', 'notes:head:x', 'x:now:now', 'a:b:c:d:e:1:2']);
  for (let i = 0; i < 400; i++) {
    const n = 1 + Math.floor(r() * 4);
    leaves.add(Array.from({ length: n }, () => pick(segs)).join(':'));
  }
  const refs = [...leaves].map((l) => [l, B.parseBundleRef(l)]);
  const names = ['shell:weft', 'reflexive:egg-one', 'sed:circle', 'grain:pair', 'archive:shell:weft', 'garden', 'a:b:c', 'shell:', 'shell:9x', 'shell:x y', 'history:mira', ':mira', 'pool:Phenomemental', 'SED:x']
    .map((n) => [n, B.handleOfBlock(n)]);

  const garden = { _: 'Garden.', 1: { _: 'Spring', 1: 'peas', 2: { _: 'tulips', 1: 'red' } }, 2: 'Summer', 9: 'Winter' };
  const stamp = (t: string, d: number) => ({ _: t, 1: 'mira', 3: `2026-10-${String(d).padStart(2, '0')}T10:00:00Z` });
  const folded: any = { _: 'Folded.', 1: { _: 'span one' }, 2: { _: 'span two' } };
  for (let i = 1; i <= 9; i++) folded[1][i] = stamp(`one ${i}`, i);
  for (let i = 1; i <= 2; i++) folded[2][i] = stamp(`two ${i}`, 10 + i);
  const deepFold: any = { _: { _: 'Floor two, folded.' }, 1: { 1: { _: 'span 11' }, 2: { _: 'span 12' } } };
  for (let i = 1; i <= 3; i++) deepFold[1][2][i] = stamp(`twelve ${i}`, i);
  const nodes: any[] = [
    null, 'garden', ['garden', 'notes'], {}, { _: 'only a voicing' }, { 1: 'garden' }, { 1: 'garden', 2: 'a line' }, { 1: 'garden', 2: 'notes', 3: 'a line' },
    { 1: 'garden', 2: 'notes', 3: '2026-10-01T10:00:00Z' }, { 1: 'garden', 2: 'notes', 3: '2026-10-01 10:00' }, { 1: { _: 'garden' }, 2: { _: 'a line' }, 3: { _: 'notes' } },
    { 1: '', 2: '  ', 3: 'garden' }, { 1: 'a b', 2: 'c d', 3: 'garden', 4: 'notes' }, { _: 'x', 1: 'garden', 2: 5, 3: true }, { 1: { _: { _: 'garden' } } },
    garden, folded, { 3: ['2026-10-01T10:00:00Z'] },
  ];
  const looks = nodes.map((n) => [n, B.looksLikeBundle(n), B.isEntryNode(n), B.bundleSlots(n).map((s: any) => [s.slot, s.leaf, s.ref ? s.ref.ref : null])]);
  const heads = [garden, folded, deepFold, { _: 'empty' }, { _: 'one', 1: stamp('only', 1) }, { _: 'strings', 1: 'a', 2: 'b' }, { _: { _: 'f2' }, 3: 'x' }]
    .map((b) => [b, B.headAddress(b)]);

  // ── the clock range, over a block with lines at random moments in two days ──
  const t0 = RealDate.UTC(2026, 9, 27, 0, 0, 0);
  let chain: any = 'A clock for the units';
  for (let i = 1; i < 10; i++) chain = { _: chain };
  const clock: any = { _: chain };
  const put = (digits: string[], value: any) => {
    let node = clock;
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
  const walkOf = (ms: number, fine: number) => T.momentToAddress(D(ms), fine).replace('.', '').split('');
  const said: number[] = [];
  for (let i = 0; i < 70; i++) said.push(t0 + Math.floor(r() * 2 * 86_400_000));
  said.sort((a, b) => a - b);
  for (const ms of said) {
    const kind = r();
    if (kind < 0.15) put(walkOf(ms, 0).slice(0, 4 + Math.floor(r() * 6)), `voicing set near ${new RealDate(ms).toISOString()}`);
    else if (kind < 0.3) put(walkOf(ms, 3), { _: `an entry at ${new RealDate(ms).toISOString()}`, 1: 'mira', 2: 'a field', 3: new RealDate(ms).toISOString(), 4: { _: 'beneath the entry' } });
    else if (kind < 0.35) put(walkOf(ms, 3), '   ');
    else put(walkOf(ms, Math.floor(r() * 4)), `said at ${new RealDate(ms).toISOString()}`);
  }
  // The same line twice: a Set of strings voices it once.
  put(walkOf(said[10], 3), 'a line said twice');
  put(walkOf(said[11], 3), 'a line said twice');
  const ranges: any[] = [];
  for (let i = 0; i < 80; i++) {
    const a = t0 - 3_600_000 + Math.floor(r() * 2.2 * 86_400_000);
    const gap = pick([0, 1000, 60_000, 900_000, 3_600_000, 10_000_000, 86_400_000, -60_000]);
    const since = T.momentToAddress(D(a), pick([0, 1, 2, 3]));
    const now = T.momentToAddress(D(a + gap), 3);
    ranges.push([since, now, B.clockRange(clock, since, now)]);
  }
  // [the range rendered, the cap, the text]
  const full = ranges.map((x, i) => [i, x[2].length]).filter(([, n]) => n > 0).map(([i]) => i);
  const clockLines = full.slice(0, 6).map((i) => [i, 3, B.renderClockLines(ranges[i][2], 3)]);
  clockLines.push([full[0], 40, B.renderClockLines(ranges[full[0]][2])]);
  const beat = T.momentToAddress(D(said[20]), 0);
  const walk = bspRead(clock, beat, null).entries ?? [];
  if (!walk.length) throw new Error('units: the clock walk is empty — cutClockWalk would check nothing');
  const cuts = [[beat, beat, false], [beat, beat, true], [`${beat.slice(0, 8)}11`, beat, false], [`${beat.slice(0, 5)}11111`, beat, false], [beat, `${beat}.111`, true]]
    .map(([since, now, touched]) => { const c = B.cutClockWalk(walk, since, now, touched); return [since, now, touched, c.entries.map((e: any) => e.address), c.from, c.same, c.within]; });

  // ── whole bundles over a fixture surface ──
  const big: any = { _: 'A big block.' };
  for (let i = 1; i <= 9; i++) big[i] = `line ${i}: ${'long words of the big block. '.repeat(30)}`;
  const surface: Record<string, any> = {
    garden, 'notes:mira': { _: "Mira's notes", 1: 'seeds' }, notes: { _: 'Shared notes', 1: 'a shared line' }, folded, clock, big,
    diary: { _: 'Floor one.', 1: 'a day' }, empty: { _: 'nothing yet' }, 'shell:mira': { _: 'shell', 3: { 1: 'notes' } },
    broken: { _: 'reads that fail', 1: 'x' },
  };
  const bundleNodes: any[] = [
    { _: 'all kinds', 1: 'notes', 2: 'garden:1.2', 3: 'garden:1:-1', 4: 'folded:head', 5: 'clock:now', 6: 'diary:now', 7: 'empty:head', 8: 'nosuch', 9: 'a line of prose' },
    { 1: 'big', 2: 'big:3', 3: 'garden:0:0', 4: 'garden:9.5.1', 5: 'garden:1.2.3', 6: 'clock:now:-2', 7: 'folded:head:-1', 8: 'notes:mira:head', 9: 'broken' },
    { 1: 'nosuch', 2: 'nowhere:1' },
    { 1: 'garden', 2: 'a b', 3: 'c d' },
    { 1: { _: 'garden', 1: 'beneath' }, 2: { _: 'notes:1' }, 3: 'prose here', 4: '' },
    { 1: 'clock:now', 2: 'clock:now:5', 3: 'clock:NOW:-3' },
  ];
  const loads: string[] = [];
  const load = async (name: string) => { loads.push(name); if (name === 'broken') throw new Error('the loader refused'); return surface[name] ?? null; };
  // The members of one bundle load in parallel upstream, so the order of the loads is not theirs to keep: only which.
  const renderMembers = (ms: any[] | null) => ms && ms.map((m) => [m.slot, m.name, m.address, m.absent, m.note, stripRelations(B.renderMemberBody(m))]);
  const bundles: any[] = [];
  const nowA = RealDate.UTC(2026, 9, 27, 13, 0, 0) + 3_333;
  for (const node of bundleNodes) {
    for (const handle of [null, 'mira']) {
      for (const now of [nowA, said[31] + 400]) {
        for (const since of [null, T.momentToAddress(D(now - 30 * 60_000), 3), T.momentToAddress(D(now), 3), T.momentToAddress(D(now - 2_000), 0)]) {
          loads.length = 0;
          const members = await B.compileBundle(node, { load, handle, now: D(now), since });
          const opts: any = { label: '"pocket/desk" 2', now: D(now), since };
          bundles.push({
            node, handle, now, since, loads: [...loads].sort(),
            members: renderMembers(members),
            window: members ? stripRelations(B.renderBundle(members, opts)) : null,
            // A small cap counts characters, and the tags the pocket leaves out are characters the original
            // counts: so the cap is checked on bundles with no clock member, whose text has no tags.
            small: members && !since && !JSON.stringify(node).toLowerCase().includes(':now') ? stripRelations(B.renderBundle(members, { ...opts, cap: 1500 })) : null,
            member: members ? stripRelations(B.renderMember(members[0], '    ')) : null,
          });
        }
      }
    }
  }

  // ── the seat: declared, ridden, told, re-dialed, with a fake io ──
  const seats: any[] = [];
  const scenarios = [
    { handle: 'mira', block: 'shell:mira', spindle: '3', shell: { _: 'shell', 3: { 1: 'notes', 2: 'garden:1', 3: 'clock:now', 4: 'nosuch', 5: 'a line', 6: 'diary:now' } } },
    { handle: 'kit', block: 'shell:kit', spindle: '9', shell: { _: 'shell', 9: { 1: 'clock:now:-1', 2: 'folded:head' } } },
    { handle: '', block: 'shell:x', spindle: '3', shell: { _: 'shell', 3: { 1: 'garden' } } },
  ];
  for (const sc of scenarios) {
    const blocks: Record<string, any> = JSON.parse(JSON.stringify({ ...surface, [sc.block]: sc.shell }));
    const touched: Record<string, string> = {};
    const io = {
      load: () => async (name: string) => blocks[name] ?? null,
      touched: async () => ({ ...touched }),
    };
    const steps: any[] = [];
    const session = `units-${sc.handle || 'none'}`;
    O.resetSeats();
    let at = said[25] - 400_000;
    const ride = async () => { steps.push({ op: 'ride', at, text: stripRelations(await O.orientationRider(session, at, io)) }); };
    O.declareSeat(session, 'pocket', sc.handle, sc.block, sc.spindle, readAt(blocks[sc.block], sc.spindle), at);
    steps.push({ op: 'declare', at });
    const set = (name: string, content: any) => { blocks[name] = content; touched[name] = new RealDate(at).toISOString(); steps.push({ op: 'set', at, block: name, content }); };
    at += 1000; await ride();
    at += 2000; set('notes', { _: 'Shared notes', 1: 'a shared line', 2: 'a second shared line' });
    at += 1000; await ride();
    at += 5000; await ride();
    at += 6000; await ride();
    at += 400_000; await ride();
    at += 1000; set('garden', { ...garden, 1: { ...garden[1], 3: 'compost turned' } });
    at += 6000; await ride();
    at += 1000; O.seatTold(session, at); steps.push({ op: 'told', at });
    at += 6000; set('notes', { _: 'Shared notes', 1: 'rewritten' });
    at += 1000; await ride();
    at += 7000; set(sc.block, { ...sc.shell, [sc.spindle]: { 1: 'garden:2', 2: 'clock:now' } });
    at += 7000; await ride();
    at += 3_600_000; await ride();
    at += 86_400_000; await ride();
    seats.push({ ...sc, blocks: JSON.parse(JSON.stringify({ ...surface, [sc.block]: sc.shell })), steps });
  }
  O.resetSeats();

  return { moments, voices, spans, divergence, clockStamps, rungs, refs, names, looks, heads, clock, ranges, clockLines, beat, cuts, surface, bundles, seats };
}
