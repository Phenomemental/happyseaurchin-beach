// A beach on this computer, for testing zero.html without writing to the
// real one: node tests/zero-beach.mjs [port]. It serves the repo's files at
// http://localhost:<port>/ and a beach at /.well-known/pscale-beach, seeded
// with copies of zero, function:zero and pool:zero (tests/zero-fixtures.json).
//
// It keeps the beach's own rules as pscale-beach has them (ported in
// pocket-beach/bsp/.../Beach.kt): an append takes the next free zero-free
// slot and supernests when the root fills; the arrival stamp goes at 3 only
// when 3 is empty; a write at an address wraps a string it passes through as
// "_"; a string written onto a node sets its "_"; a whole-block write over a
// block that stands needs confirm; only "_" and 1-9 are keys. No locks.
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const TYPES = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css', '.json': 'application/json', '.png': 'image/png', '.jpg': 'image/jpeg' };

export function makeBeach(seed) {
  const blocks = JSON.parse(JSON.stringify(seed || {}));
  const log = [];
  const isObj = v => v && typeof v === 'object' && !Array.isArray(v);
  function floorOf(b) { let f = 0, n = b; while (isObj(n) && n._ !== undefined) { f++; n = n._; if (typeof n === 'string') return f; } return f; }
  function zeroFree(i, floor) {
    let s = '';
    for (let k = 0; k < floor; k++) { s = String((i % 9) + 1) + s; i = Math.floor(i / 9); }
    return s;
  }
  function rawWalk(b, p) { let n = b; for (const c of p) { if (!isObj(n) || n[c] === undefined) return undefined; n = n[c]; } return n; }
  function rawSet(b, p, v) {
    let n = b;
    for (let k = 0; k < p.length - 1; k++) {
      const c = p[k];
      if (typeof n[c] === 'string') n[c] = { _: n[c] };
      else if (!isObj(n[c])) n[c] = {};
      n = n[c];
    }
    n[p[p.length - 1]] = v;
  }
  function validate(c) {
    if (c == null) return null;
    if (typeof c === 'string') {
      const t = c.trim();
      if (t.length > 1 && (t[0] === '{' || t[0] === '[')) { try { JSON.parse(t); return 'JSON passed as a string'; } catch (e) {} }
      return null;
    }
    if (Array.isArray(c)) return 'arrays are not allowed';
    if (isObj(c)) { for (const k of Object.keys(c)) { if (!/^(_|[1-9])$/.test(k)) return 'bad key ' + k; const e = validate(c[k]); if (e) return e; } }
    return null;
  }
  const ISO = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,9})?)?(Z|[+-]\d{2}:?\d{2})?$/;
  function digitsOf(spindle, floor) {
    const s = String(spindle);
    let d = s.replace('.', '');
    if (!s.includes('.') && d.length < floor) d = '0'.repeat(floor - d.length) + d;
    if (s.includes('.')) { const left = s.split('.')[0]; if (left.length < floor) d = '0'.repeat(floor - left.length) + d; }
    return d;
  }
  function walkRead(b, spindle) {
    // As the beach answers it: an address of noughts alone reads the first rung down.
    if (/^0+$/.test(String(spindle))) return isObj(b._) ? b._ : b;
    const d = digitsOf(spindle, floorOf(b));
    let n = b;
    for (const c of d) {
      if (c === '0') { if (isObj(n) && isObj(n._)) n = n._; continue; }
      if (!isObj(n) || n[c] === undefined) return undefined;
      n = n[c];
    }
    return n;
  }
  function writeAt(b, spindle, value) {
    const d = digitsOf(spindle, floorOf(b));
    let n = b;
    for (let k = 0; k < d.length - 1; k++) {
      const key = d[k] === '0' ? '_' : d[k];
      if (typeof n[key] === 'string') n[key] = { _: n[key] };
      else if (!isObj(n[key])) n[key] = {};
      n = n[key];
    }
    const last = d[d.length - 1] === '0' ? '_' : d[d.length - 1];
    if (isObj(n[last]) && !isObj(value)) { n[last]._ = value; return; }
    n[last] = value;
  }
  function count(n) { let c = 0; if (isObj(n)) for (let i = 1; i <= 9; i++) if (n[i] !== undefined) c++; return c; }

  function get(name, q) {
    if (!name) return [200, { _: 'A test beach.', blocks: Object.keys(blocks) }];
    const b = blocks[name];
    if (b === undefined) return [404, { error: `block "${name}" not found`, code: 'not_found' }];
    const spindle = q.get('spindle'), ps = q.get('pscale');
    const F = floorOf(b);
    if (spindle == null && ps != null) {
      const depth = F - Number(ps);
      const entries = [];
      (function walk(n, pre) {
        if (pre.length === depth) { entries.push({ address: pre, content: typeof n === 'string' ? n : (isObj(n) && typeof n._ === 'string' ? n._ : null), beneath: count(n) }); return; }
        if (!isObj(n)) return;
        for (let i = 1; i <= 9; i++) if (n[i] !== undefined) walk(n[i], pre + i);
      })(b, '');
      return [200, { floor: F, shape: 'disc', pscale: Number(ps), target_depth: depth, entries }];
    }
    if (spindle != null) {
      const n = walkRead(b, spindle);
      if (n === undefined) return [404, { error: 'no node', code: 'not_found' }];
      return [200, n];
    }
    return [200, b];
  }
  const locked = new Set();     // blocks that refuse every write, as a latched block refuses one without its secret
  function post(name, body, now) {
    if (!name) return [400, { error: 'block name required', code: 'invalid_shape' }];
    if (locked.has(name)) return [403, { error: `"${name}" is locked`, code: 'lock_required' }];
    const { content, spindle, confirm, append } = body;
    const bad = validate(content);
    if (bad) return [400, { error: bad, code: 'invalid_shape' }];
    if (append === true) {
      let entry = content;
      if (isObj(content) && !ISO.test(content['3'] || '')) entry = { ...content, 3: now.toISOString() };
      let b = blocks[name];
      const born = b === undefined;
      if (born) b = { _: `${name} at localhost.` };
      let F = floorOf(b), slot = null, supernested = false;
      for (let i = 0; i < 9 ** F; i++) { const p = zeroFree(i, F); if (rawWalk(b, p) === undefined) { slot = p; break; } }
      if (!slot) { b = { _: b }; F++; supernested = true; slot = zeroFree(0, F); }
      rawSet(b, slot, entry);
      blocks[name] = b;
      log.push({ name, slot, entry });
      return [200, { ok: true, slot, supernested, floor: F, ...(born ? { born: true } : {}) }];
    }
    const existing = blocks[name];
    if (content !== undefined && !spindle && existing !== undefined && confirm !== true) return [400, { error: 'whole-block replace requires {confirm: true}', code: 'confirm_required' }];
    if (!spindle) { blocks[name] = content; log.push({ name, whole: true }); return [200, { ok: true }]; }
    const b = existing === undefined ? { _: `${name} at localhost.` } : existing;
    writeAt(b, spindle, content);
    blocks[name] = b;
    log.push({ name, spindle, content });
    return [200, existing === undefined ? { ok: true, born: true } : { ok: true }];
  }
  return { blocks, log, get, post, locked };
}

export function serve(port = 0, seed) {
  const beach = makeBeach(seed || JSON.parse(fs.readFileSync(path.join(ROOT, 'tests/zero-fixtures.json'), 'utf8')));
  const cors = { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, POST, OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type', 'Access-Control-Expose-Headers': 'X-Pscale-Now' };
  const server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost');
    if (url.pathname === '/.well-known/pscale-beach') {
      const now = new Date();
      const headers = { ...cors, 'Content-Type': 'application/json; charset=utf-8', 'X-Pscale-Now': `${now.toISOString().slice(0, 19)}Z | 2026422183 | a test beach` };
      if (req.method === 'OPTIONS') { res.writeHead(204, headers); return res.end(); }
      const name = url.searchParams.get('block') || '';
      if (req.method === 'GET') { const [s, j] = beach.get(name, url.searchParams); res.writeHead(s, headers); return res.end(JSON.stringify(j)); }
      let raw = '';
      req.on('data', c => { raw += c; });
      req.on('end', () => {
        let body;
        try { body = JSON.parse(raw || '{}'); } catch (e) { res.writeHead(400, headers); return res.end('{"error":"bad json"}'); }
        const [s, j] = beach.post(name || body.block, body, now);
        res.writeHead(s, headers);
        res.end(JSON.stringify(j));
      });
      return;
    }
    let file = path.join(ROOT, decodeURIComponent(url.pathname));
    if (!file.startsWith(ROOT)) { res.writeHead(403); return res.end(); }
    if (fs.existsSync(file) && fs.statSync(file).isDirectory()) file = path.join(file, 'index.html');
    if (!fs.existsSync(file)) { res.writeHead(404); return res.end('not found'); }
    res.writeHead(200, { 'Content-Type': TYPES[path.extname(file)] || 'application/octet-stream', 'Cache-Control': 'no-store' });
    fs.createReadStream(file).pipe(res);
  });
  return new Promise(resolve => server.listen(port, '127.0.0.1', () => resolve({ server, beach, port: server.address().port })));
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  const { port } = await serve(Number(process.argv[2]) || 8787);
  console.log(`zero test beach: http://localhost:${port}/zero.html?beach=http://localhost:${port}`);
}
