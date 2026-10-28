/**
 * harness.mts — the golden reference for the pocket's local bsp.
 *
 * Runs the REAL bsp-mcp-server handler (handleBsp) against the REAL
 * pscale-beach handler on pscale-beach's own folder-backed store, both at the
 * commits pinned in upstream.json, with the clock frozen per call. Every call
 * in the corpus is recorded with the reply text and the stored state after it;
 * the Kotlin conformance test replays the same calls and must match exactly.
 *
 * The pocket region stands where the reference has a beach URL: agent_id
 * "pocket" here is sent as POCKET_URL, and POCKET_URL in a reply reads back as
 * "pocket". Reads take bsp-mcp-server's own walker (the path it uses for
 * agent_id="pscale"): the beach is asked for whole blocks only, so the router
 * walks them itself, exactly as it does for a beach without ?pscale= reads.
 *
 *   tsx harness.mts <upstream-dir> <out.json> [random-count] [random-seed] [with-hand|random-only]
 */
import { mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';
import { cases as handCases } from './corpus.mjs';
import { randomCases } from './random.mjs';

const [, , upstreamDir, outFile, randomCount = '240', randomSeed = '20261028', handFlag = 'with-hand'] = process.argv;
if (!upstreamDir || !outFile) {
  console.error('usage: tsx harness.mts <upstream-dir> <out.json> [random-count] [random-seed] [with-hand|random-only]');
  process.exit(2);
}

// ── the frozen clock ──
const RealDate = Date;
let nowMs = RealDate.parse('2026-10-28T09:00:00.000Z');
class FrozenDate extends RealDate {
  constructor(...args: any[]) {
    if (args.length === 0) super(nowMs);
    else super(...(args as [any]));
  }
  static now() { return nowMs; }
}
(globalThis as any).Date = FrozenDate;

// ── the beach, as its own offline rig runs it ──
const ORIGIN = 'pocket';
const POCKET_URL = 'http://pocket.invalid';
process.env.KV_REST_API_URL ||= 'https://local.invalid';
process.env.KV_REST_API_TOKEN ||= 'local';
process.env.BEACH_ORIGIN = ORIGIN;
delete process.env.BEACH_CAPS;

const beachDir = join(upstreamDir, 'pscale-beach');
const routerDir = join(upstreamDir, 'bsp-mcp-server');
const { FileRedis } = await import(pathToFileURL(join(beachDir, 'scripts/file-redis.mjs')).href);
const beach = await import(pathToFileURL(join(beachDir, 'api/pscale-beach.js')).href);
const handler = beach.default;

// The beach logs a repaired floor to stderr; keep the run's output clean.
console.error = () => {};

let storeDir = '';
function freshStore() {
  if (storeDir) rmSync(storeDir, { recursive: true, force: true });
  storeDir = mkdtempSync(join(tmpdir(), 'pocket-golden-'));
  beach.__setRedis(new FileRedis(storeDir));
}

async function callBeach(method: string, url: URL, body: any) {
  const query: Record<string, string> = Object.fromEntries(url.searchParams);
  if (method === 'GET' && query.block) {
    delete query.spindle;
    delete query.pscale;
  }
  let status = 200;
  let text = '';
  const res = {
    setHeader() {},
    status(code: number) { status = code; return this; },
    json(obj: any) { text = JSON.stringify(obj); },
    end() {},
  };
  await handler({ method, query, body, headers: { host: 'pocket.invalid' }, url: url.pathname + url.search }, res);
  return { status, text };
}

const realFetch = globalThis.fetch;
(globalThis as any).fetch = async (input: any, init: any = {}) => {
  const url = new URL(String(input));
  if (url.origin !== POCKET_URL) return realFetch(input, init);
  const method = (init.method ?? 'GET').toUpperCase();
  const body = init.body ? JSON.parse(String(init.body)) : {};
  const { status, text } = await callBeach(method, url, body);
  return new Response(status === 204 ? null : text, { status, headers: { 'content-type': 'application/json' } });
};

// ── the router ──
const { handleBsp } = await import(pathToFileURL(join(routerDir, 'src/tools/bsp.ts')).href);

function stateOf() {
  const prefix = `pscale-beach-v2:${ORIGIN}:block:`;
  const blocks: Record<string, string> = {};
  for (const f of readdirSync(storeDir).sort()) {
    const key = decodeURIComponent(f.slice(0, -'.json'.length));
    if (key.startsWith(prefix)) blocks[key.slice(prefix.length)] = readFileSync(join(storeDir, f), 'utf8');
  }
  const hash = (k: string) => {
    try { return JSON.parse(readFileSync(join(storeDir, encodeURIComponent(k) + '.json'), 'utf8')); } catch { return {}; }
  };
  return { blocks, touched: hash(`pscale-beach-v2:${ORIGIN}:touched`), born: hash(`pscale-beach-v2:${ORIGIN}:born`) };
}

function toWire(params: Record<string, any>) {
  const p = { ...params };
  if (p.agent_id === 'pocket') p.agent_id = POCKET_URL;
  return p;
}

async function run(caseDef: any, caseIndex: number) {
  freshStore();
  const base = RealDate.parse('2026-10-28T09:00:00.000Z') + caseIndex * 3_600_000;
  let tick = 0;
  const seeded: any[] = [];
  for (const s of caseDef.seed ?? []) {
    nowMs = base + (tick++) * 1000;
    if (s.raw) {
      writeFileSync(join(storeDir, encodeURIComponent(`pscale-beach-v2:${ORIGIN}:block:${s.block}`) + '.json'), JSON.stringify(s.content));
    } else {
      const r = await callBeach('POST', new URL(`${POCKET_URL}/.well-known/pscale-beach?block=${encodeURIComponent(s.block)}`), { content: s.content, confirm: true });
      if (r.status !== 200) throw new Error(`seed ${s.block} refused: ${r.text}`);
    }
    seeded.push({ block: s.block, content: s.content, raw: !!s.raw, now: new RealDate(nowMs).toISOString() });
  }
  const seedState = stateOf();
  let before = seedState.blocks;
  const calls: any[] = [];
  for (const params of caseDef.calls) {
    nowMs = base + (tick++) * 1000;
    let text: string;
    try {
      const res = await handleBsp(toWire(params));
      text = res.content.map((c: any) => c.text).join('\n');
    } catch (e: any) {
      text = `[threw] ${e?.message ?? String(e)}`;
    }
    text = text.split(POCKET_URL).join(ORIGIN);
    // Only the blocks this call changed ride with it; the replay carries the rest.
    const after = stateOf();
    const changed: Record<string, string> = {};
    for (const [name, json] of Object.entries(after.blocks)) if (before[name] !== json) changed[name] = json;
    for (const name of Object.keys(before)) if (!(name in after.blocks)) throw new Error(`block ${name} vanished`);
    before = after.blocks;
    calls.push({ now: new RealDate(nowMs).toISOString(), params, text, changed, touched: after.touched, born: after.born });
  }
  return { name: caseDef.name, seed: seeded, seedState, calls };
}

const all = [...(handFlag === 'random-only' ? [] : handCases), ...randomCases(Number(randomCount), Number(randomSeed))];
const out: any[] = [];
for (let i = 0; i < all.length; i++) out.push(await run(all[i], i));
if (storeDir) rmSync(storeDir, { recursive: true, force: true });

const upstream = JSON.parse(readFileSync(new URL('./upstream.json', import.meta.url), 'utf8'));
writeFileSync(outFile, JSON.stringify({ upstream, origin: ORIGIN, cases: out }, null, 1) + '\n');
const callCount = out.reduce((n, c) => n + c.calls.length, 0);
process.stdout.write(`golden: ${out.length} cases, ${callCount} calls → ${outFile}\n`);
