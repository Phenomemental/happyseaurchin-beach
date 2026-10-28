// export-shelf.mjs — copy bsp-mcp-server's bundled sentinel blocks into
// pocket-beach/shelf/, byte for byte, with index.json naming each block's file.
// Read off src/sentinels.ts, the one registry upstream keeps.
//
//   node export-shelf.mjs <upstream-dir> <shelf-dir>        write the shelf
//   node export-shelf.mjs <upstream-dir> <shelf-dir> --check  fail if it differs
import { readFileSync, writeFileSync, mkdirSync, readdirSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const [, , upstreamDir, shelfDir, flag] = process.argv;
const src = join(upstreamDir, 'bsp-mcp-server', 'src');
const registry = readFileSync(join(src, 'sentinels.ts'), 'utf8');

const files = {};
for (const m of registry.matchAll(/^import (\w+) from '\.\/([\w-]+\.json)'/gm)) files[m[1]] = m[2];
const entries = [];
for (const m of registry.matchAll(/name: '([\w-]+)',\s*\n\s*json: (\w+) as unknown as Block/g)) {
  const file = files[m[2]];
  if (!file) throw new Error(`no import for ${m[2]}`);
  entries.push({ name: m[1], file });
}
if (entries.length < 20) throw new Error(`only ${entries.length} sentinels found — the registry shape changed`);

const index = JSON.stringify(entries, null, 1) + '\n';
const wanted = new Set([...entries.map((e) => e.file), 'index.json', 'LICENSE']);
const expect = (name) => (name === 'index.json' ? index : readFileSync(name === 'LICENSE' ? join(upstreamDir, 'bsp-mcp-server', 'LICENSE') : join(src, name), 'utf8'));

if (flag === '--check') {
  const bad = [];
  for (const name of wanted) {
    const p = join(shelfDir, name);
    if (!existsSync(p) || readFileSync(p, 'utf8') !== expect(name)) bad.push(name);
  }
  for (const name of readdirSync(shelfDir)) if (!wanted.has(name)) bad.push(`${name} (not upstream)`);
  if (bad.length) {
    console.error(`shelf differs from bsp-mcp-server at the pinned commit: ${bad.join(', ')}`);
    process.exit(1);
  }
  console.log(`shelf matches upstream: ${entries.length} names, ${wanted.size - 2} files`);
} else {
  mkdirSync(shelfDir, { recursive: true });
  for (const name of wanted) writeFileSync(join(shelfDir, name), expect(name));
  console.log(`shelf written: ${entries.length} names, ${wanted.size - 2} files`);
}
