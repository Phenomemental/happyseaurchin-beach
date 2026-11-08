// Zero played end to end in a headless browser at phone size, against the
// beach on this computer (tests/zero-beach.mjs) with Claude stood in, so
// nothing reaches the real beach or spends a key (spine:zero 4.7).
//   node tests/zero-play.mjs            every table
//   node tests/zero-play.mjs A..F       one: A host + 8 computer seats,
//                                       B host + 2 phones + 6 computer, C nine people,
//                                       D someone leaves mid-game, E the room is locked,
//                                       F Claude refuses the host's key
// Screenshots, when asked for with SHOTS=<dir>, land there.
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import { serve } from './zero-beach.mjs';
const require = createRequire(import.meta.url);
const { chromium, devices } = require('playwright');   // the machine's own Playwright
const Z = require('../zero-law.js');

const PHONE = devices['iPhone 13'];
const SHOTS = process.env.SHOTS || '';
const which = (process.argv[2] || 'ABCDEF').toUpperCase();

// ---- Claude, stood in -------------------------------------------------------------------
const WORDS = {
  'Child': ['puddle', 'kite', 'crayon', 'blanket', 'tadpole', 'conker'],
  'Sailor': ['fog', 'harbour', 'gale', 'rope', 'compass', 'tide'],
  'Poet': ['hush', 'ember', 'threshold', 'breath', 'salt', 'echo'],
  'Engineer': ['signal', 'circuit', 'lever', 'voltage', 'gear', 'sensor'],
  'Grandmother': ['snow', 'kettle', 'quilt', 'letter', 'jam', 'shawl'],
  'Chef': ['simmer', 'knife', 'bread', 'stock', 'pan', 'apron'],
  'Farmer': ['snow', 'lambing', 'hedge', 'mud', 'barn', 'gate'],
  'Night nurse': ['ward', 'chart', 'monitor', 'hush', 'lamp', 'pulse']
};
const PERSONAS = Object.keys(WORDS);
const calls = { word: 0, line: 0, vote: 0, pick: 0, headersOk: true, models: new Set() };
const turns = {};
async function claudeRoute(route) {
  const req = route.request();
  const cors = { 'access-control-allow-origin': '*', 'access-control-allow-headers': '*', 'access-control-allow-methods': 'POST, OPTIONS' };
  if (req.method() === 'OPTIONS') return route.fulfill({ status: 204, headers: cors });
  const h = req.headers();
  if (!h['x-api-key'] || h['anthropic-dangerous-direct-browser-access'] !== 'true' || !h['anthropic-version']) calls.headersOk = false;
  const body = JSON.parse(req.postData());
  calls.models.add(body.model);
  const sys = body.system, user = body.messages[0].content;
  const persona = (/playing as the ([A-Za-z ]+)\./.exec(sys) || [])[1] || 'Child';
  const pi = PERSONAS.indexOf(persona);
  let out;
  if (/Answer as \{"word"/.test(user)) {
    calls.word++;
    const zero = /The zero word is “([^”]+)”/.exec(user)[1];
    const k = persona + zero;
    turns[k] = (turns[k] || 0) + 1;
    const list = WORDS[persona];
    out = { word: list[(zero.length + turns[k] - 1) % list.length] };
  } else if (/Answer as \{"line"/.test(user)) {
    calls.line++;
    const ring = /surviving ring is: ([^.]+)\./.exec(user)[1].split(', ');
    const chips = Number(/You have (\d+) chips/.exec(user)[1]);
    out = { line: `${ring.slice(0, 3).join(' and ')}, as a ${persona.toLowerCase()} sees it.`, stake: Math.min(chips, (pi % 4) + 1) };
  } else if (/Vote for the one line/.test(user)) {
    calls.vote++;
    const me = Number(/You are seat (\d)/.exec(user)[1]);
    const seats = [...user.matchAll(/^seat (\d) \(/gm)].map(m => Number(m[1])).filter(s => s !== me);
    out = { seat: seats[pi % seats.length], why: 'it holds the ring' };
  } else {
    calls.pick++;
    const seats = [...user.matchAll(/^seat (\d):/gm)].map(m => Number(m[1]));
    out = { seat: seats[0], why: 'deeper' };
  }
  await route.fulfill({
    status: 200, headers: cors, contentType: 'application/json',
    body: JSON.stringify({ id: 'msg_test', type: 'message', role: 'assistant', model: body.model, content: [{ type: 'thinking', thinking: '', signature: 'x' }, { type: 'text', text: JSON.stringify(out) }], stop_reason: 'end_turn', usage: { input_tokens: 10, output_tokens: 10 } })
  });
}

// ---- a person at a phone -----------------------------------------------------------------
async function phone(browser, base) {
  const ctx = await browser.newContext({ ...PHONE });
  await ctx.route('https://api.anthropic.com/**', claudeRoute);
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  page.on('console', m => { if (m.type() === 'error' && !/Failed to load resource/.test(m.text())) errors.push(m.text()); });
  await page.goto(base);
  return { ctx, page, errors };
}
async function fillMe(page, name, three) {
  await page.fill('#my-name', name);
  await page.fill('#my-three', three);
}
async function shot(page, name) {
  if (!SHOTS) return;
  fs.mkdirSync(SHOTS, { recursive: true });
  await page.screenshot({ path: path.join(SHOTS, name + '.png'), fullPage: true });
}
const visible = async (loc) => (await loc.count()) > 0 && await loc.first().isVisible();

// Play whatever the panel asks, until the poem. words(hand) gives this person's word.
async function play(p, words, opts = {}) {
  const { page } = p;
  const seen = new Set();
  const deadline = Date.now() + (opts.timeout || 240000);
  let hand = 0;
  while (Date.now() < deadline) {
    if (await page.locator('#end').isVisible()) return true;
    const phase = (await page.locator('#t-phase').textContent().catch(() => '')) || '';
    const m = /Hand (one|two|three|four)/.exec(phase);
    if (m) hand = ['one', 'two', 'three', 'four'].indexOf(m[1]) + 1;
    if (opts.shots && !seen.has(phase)) { seen.add(phase); await shot(page, opts.shots + '-' + phase.replace(/[^a-z]+/gi, '-').toLowerCase()); }
    try {
      if (await visible(page.locator('#m-word'))) {
        await page.fill('#m-word', words(hand));
        await page.click('#t-moves button[type=submit]');
      } else if (await visible(page.getByRole('button', { name: 'Turn the cards' }))) {
        await page.getByRole('button', { name: 'Turn the cards' }).click();
      } else if (await visible(page.locator('#m-line'))) {
        await page.fill('#m-line', opts.line ? opts.line(hand) : 'Everything here keeps someone company.');
        await page.locator('#m-stake').evaluate((el, v) => { el.value = String(Math.min(Number(el.max), v)); el.dispatchEvent(new Event('input')); }, opts.stake || 2);
        await page.getByRole('button', { name: 'Seal line and stake' }).click();
      } else if (await visible(page.locator('.slip:not([disabled])'))) {
        await page.locator('.slip:not([disabled])').nth(opts.voteIndex || 0).click().catch(() => page.locator('.slip:not([disabled])').first().click());
        await page.getByRole('button', { name: /^Seal vote for/ }).click();
      } else if (await visible(page.locator('.picks button:not(.quiet)'))) {
        await page.locator('.picks button:not(.quiet)').first().click();
      } else if (await visible(page.getByRole('button', { name: 'The poem' }))) {
        if (opts.shots) await shot(page, opts.shots + '-last-voice');
        await page.getByRole('button', { name: 'The poem' }).click();
      }
    } catch (e) { /* the panel moved under us: look again */ }
    await page.waitForTimeout(250);
  }
  throw new Error('timed out at: ' + (await page.locator('#t-phase').textContent().catch(() => '?')) + ' / ' + (await page.locator('#t-moves').textContent().catch(() => '?')) + ' / ' + (await page.locator('#t-error').textContent().catch(() => '')));
}

// ---- checks -----------------------------------------------------------------------------
const results = [];
function check(name, ok, detail) { results.push({ name, ok: !!ok, detail }); console.log((ok ? '  ok   ' : '  FAIL ') + name + (detail && !ok ? ' — ' + detail : '')); }

async function tableIn(beach, code) {
  const all = Z.entriesIn(beach.blocks['pool:zero'], '', floorOf(beach.blocks['pool:zero']));
  return Z.derive(all, code);
}
function floorOf(b) { let f = 0, n = b; while (n && typeof n === 'object' && n._ !== undefined) { f++; n = n._; } return f; }

async function endChecks(label, beach, page, people) {
  const code = (await page.evaluate(() => JSON.parse(localStorage.getItem('zero.table') || '{}').code));
  const t = await tableIn(beach, code);
  check(label + ': four hands played to the end', t.over && t.hands.length === 4, `hands ${t.hands.length}, phase ${t.phase}`);
  const chips = Object.values(t.chips).reduce((a, b) => a + b, 0);
  check(label + ': chips are kept (90 in all)', chips === 90, 'total ' + chips);
  const tree = beach.blocks.zero;
  const voiced = t.hands.filter(h => h.winners.length);
  const allVoiced = voiced.every(h => { const n = Z.nodeAt(tree, h.place); return n && Z.readPlace(n).voice && Z.textOf(n) === Z.placeAfter(h, s => Z.nameOf(t, s))._; });
  check(label + ': every played place carries its voice in the tree', voiced.length && allVoiced, voiced.map(h => h.place).join(','));
  // A picked seat is the next hand's place, so it carries that place's voice now.
  const seatsOk = voiced.every(h => h.ring.every(s => s === h.pick ? Z.readPlace(Z.nodeAt(tree, h.place + s)).word === h.words[s] : Z.textOf(Z.nodeAt(tree, h.place + s)) === h.words[s] + ' · ' + Z.nameOf(t, s)) &&
    Object.keys(h.out).every(s => Z.nodeAt(tree, h.place + s) == null));
  check(label + ': surviving words sit in their seats, knocked-out seats stay empty', seatsOk);
  check(label + ': the trail goes one level deeper each hand', t.hands.every((h, i) => !i || h.place.length === t.hands[i - 1].place.length + (t.hands[i - 1].noRing ? 0 : 1)));
  const poemLines = await page.locator('#e-poem .pline').count();
  check(label + ': the poem card shows the winning lines', poemLines === voiced.reduce((a, h) => a + h.winners.length, 0), poemLines + ' lines');
  const sat = await page.locator('#e-poem .sat').textContent();
  check(label + ': everyone who sat is at the foot of the card', people.every(n => sat.includes(n)), sat);
  check(label + ': the table closed after the game', t.closed);
  const mine = Z.entriesIn(beach.blocks['pool:zero'], '', floorOf(beach.blocks['pool:zero'])).filter(e => Z.codeKey(e.code) === Z.codeKey(code));
  const twice = {};
  mine.forEach(e => {
    if (e.kind === 'deal' || e.kind === 'pick' || e.kind === 'close') twice[e.kind + e.hand] = (twice[e.kind + e.hand] || 0) + 1;
    if (e.kind === 'seal' || e.kind === 'show') Object.keys(e.data).filter(k => /\d/.test(k)).forEach(k => { const x = e.kind + e.hand + e.data._ + k; twice[x] = (twice[x] || 0) + 1; });
  });
  const dup = Object.keys(twice).filter(k => twice[k] > 1);
  check(label + ': no move, deal or pick was written twice', !dup.length, dup.join(','));
  check(label + ': every entry carries the beach\'s own arrival stamp', mine.every(e => /^\d{4}-\d\d-\d\dT/.test(e.stamp)));
  // no entry carries a time of the page's own at 3 that differs from the beach's: the beach stamped them
  return t;
}

async function keepAndNote(label, page, beach) {
  const dl = page.waitForEvent('download', { timeout: 15000 });
  await page.click('#e-keep');
  const d = await dl;
  check(label + ': Keep the card downloads a picture', /^zero-.*\.png$/.test(d.suggestedFilename()), d.suggestedFilename());
  await page.fill('#note-text', 'A good table.');
  await page.click('#note-go');
  await page.waitForFunction(() => document.querySelector('#e-notes').textContent.includes('A good table.'), null, { timeout: 15000 });
  check(label + ': a word about the game is left and shown', true);
  const href = await page.getAttribute('#e-trail', 'href');
  const v = await page.context().newPage();
  await v.goto(href);
  await v.waitForSelector('#v-poem .pline', { timeout: 15000 });
  check(label + ': the trail link reads the trail from the tree', (await v.locator('#v-poem .pline').count()) >= 1, href);
  await v.close();
}

// ---- the tables --------------------------------------------------------------------------
async function tableA(browser, base, beach) {
  console.log('A. one host with eight computer seats');
  const host = await phone(browser, base);
  const { page } = host;
  await shot(page, 'a-lobby');
  await page.click('#door-host');
  await fillMe(page, 'Matthew', 'curious, kind, restless');
  await page.fill('#my-key', 'sk-ant-test');
  await page.waitForSelector('#places button');
  // the first game's tree: torch, lighthouse, photon … are open; Light, candle, vigil, silence are played
  const rows = await page.locator('#places button').allTextContents();
  check('A: the lobby offers a new first word and the open words', rows[0] === 'A new first word' && rows.some(r => r.includes('beneath torch')) && !rows.some(r => /candle|vigil|Light/.test(r)), rows.join(' | '));
  await shot(page, 'a-lobby-host');
  await page.locator('#places button', { hasText: 'beneath torch' }).click();
  await page.click('#open-go');
  await page.waitForSelector('#waiting:not([hidden])');
  const code = await page.textContent('#w-code');
  check('A: the table code shows large, a word and a number', /^[A-Z]+ [1-9]$/.test(code), code);
  check('A: the key stays in this browser and never reaches the beach', !JSON.stringify(beach.blocks).includes('sk-ant-test'));
  await shot(page, 'a-waiting');
  await page.click('#deal-go');
  await page.waitForSelector('#table:not([hidden])');
  await play(host, h => ['flicker', 'beacon', 'match', 'wick'][h - 1], { shots: SHOTS ? 'a' : '', voteIndex: 1 });
  await shot(page, 'a-end');
  const t = await endChecks('A', beach, page, ['Matthew', ...t0names(beach, code)]);
  const owed = t.hands.reduce((a, h) => a + h.active.filter(s => Z.isComputer(t, s)).length, 0);
  check('A: the computer seats moved through Claude with the browser headers, on claude-haiku-5-5', calls.word >= owed && calls.headersOk && calls.models.has('claude-haiku-5-5') && calls.models.size === 1, JSON.stringify({ ...calls, models: [...calls.models] }));
  check('A: the computers sealed before the host could write (one seal entry before the host\'s, every step)', await computersFirst(beach, code));
  check('A: the key is still nowhere on the beach', !JSON.stringify(beach.blocks).includes('sk-ant-test'));
  await keepAndNote('A', page, beach);
  check('A: no page errors', !host.errors.length, host.errors.join(' / '));
  await host.ctx.close();
  return t;
}
function t0names(beach, code) {
  const all = Z.entriesIn(beach.blocks['pool:zero'], '', floorOf(beach.blocks['pool:zero']));
  const deal = all.find(e => e.kind === 'deal' && Z.codeKey(e.code) === Z.codeKey(code));
  return Object.keys(deal.data).filter(k => /\d/.test(k)).map(k => deal.data[k]._).filter(n => n !== 'Matthew');
}
async function computersFirst(beach, code) {
  const all = Z.entriesIn(beach.blocks['pool:zero'], '', floorOf(beach.blocks['pool:zero'])).sort(Z.byAddr).filter(e => Z.codeKey(e.code) === Z.codeKey(code));
  const t = await Z.derive(all, code);
  const host = t.open.host;
  let ok = true;
  t.hands.forEach(h => ['word', 'line', 'vote'].forEach(step => {
    const st = h.steps[step];
    if (!st) return;
    const comp = Object.keys(st.seals).filter(s => Z.isComputer(t, Number(s))).map(s => st.seals[s].addr);
    const people = Object.keys(st.seals).filter(s => !Z.isComputer(t, Number(s))).map(s => st.seals[s].addr);
    if (comp.length && people.length && Z.byAddr({ addr: comp.sort()[comp.length - 1] }, { addr: people.sort()[0] }) > 0) ok = false;
  }));
  return ok;
}

async function tableB(browser, base, beach) {
  console.log('B. the host and two phones, six computer seats');
  const host = await phone(browser, base), ann = await phone(browser, base), ben = await phone(browser, base);
  await host.page.click('#door-host');
  await fillMe(host.page, 'Matthew', 'curious, kind, restless');
  await host.page.fill('#my-key', 'sk-ant-test');
  await host.page.click('#cp-less'); await host.page.click('#cp-less');
  check('B: two fewer computer players leaves six', (await host.page.textContent('#cp-count')) === '6');
  await host.page.waitForSelector('#places button');
  await host.page.locator('#places button', { hasText: 'A new first word' }).click();
  await host.page.fill('#first-word', 'Harbour');
  await host.page.click('#open-go');
  await host.page.waitForSelector('#waiting:not([hidden])');
  const code = await host.page.textContent('#w-code');
  check('B: the host waits for two more people', /Waiting for 2 more people/.test(await host.page.textContent('#w-say')));
  for (const [p, name] of [[ann, 'Ann'], [ben, 'Ben']]) {
    await p.page.click('#door-join');
    await fillMe(p.page, name, 'tea, rain, maps');
    await p.page.fill('#join-code', code.toLowerCase().replace(' ', ''));
    await p.page.click('#join-go');
    await p.page.waitForSelector('#waiting:not([hidden])');
  }
  check('B: a joiner sees the seated line', /You’re seated. The game starts when the host deals./.test(await ann.page.textContent('#w-say')));
  await shot(ann.page, 'b-seated');
  await host.page.waitForFunction(() => /Everyone is here/.test(document.querySelector('#w-say').textContent), null, { timeout: 15000 });
  // a fourth person is turned away: the table is full
  const extra = await phone(browser, base);
  await extra.page.click('#door-join');
  await fillMe(extra.page, 'Cat', 'late, sorry, hello');
  await extra.page.fill('#join-code', code);
  await extra.page.click('#join-go');
  await extra.page.waitForFunction(() => document.querySelector('#lobby-error').textContent.length > 0, null, { timeout: 15000 });
  check('B: a fourth person finds the table full', /full/.test(await extra.page.textContent('#lobby-error')), await extra.page.textContent('#lobby-error'));
  await extra.ctx.close();
  await host.page.click('#deal-go');
  await host.page.waitForSelector('#table:not([hidden])');
  check('B: the new first word is written at a free top seat', Object.keys(beach.blocks.zero).some(k => /^\d$/.test(k) && /^Harbour · Matthew/.test(Z.textOf(beach.blocks.zero[k]))));
  const runs = [
    play(host, h => ['mooring', 'gull', 'keel', 'lantern'][h - 1], { voteIndex: 0 }),
    // Ann and Ben write the same word in hand one: both are knocked out
    play(ann, h => ['jetty', 'bollard', 'net', 'buoy'][h - 1], { voteIndex: 1, shots: SHOTS ? 'b-ann' : '' }),
    play(ben, h => ['Jetties', 'crane', 'anchor', 'chart'][h - 1], { voteIndex: 2 })
  ];
  await Promise.all(runs);
  const t = await endChecks('B', beach, host.page, ['Matthew', 'Ann', 'Ben']);
  const h1 = t.hands[0];
  check('B: jetty and Jetties match at the flop and knock each other out', h1.out[2] && h1.out[3], JSON.stringify(h1.words));
  check('B: knocked-out seats still voted', h1.votes[2] != null && h1.votes[3] != null);
  await shot(ann.page, 'b-ann-end');
  for (const p of [host, ann, ben]) check('B: no page errors', !p.errors.length, p.errors.join(' / '));
  await Promise.all([host, ann, ben].map(p => p.ctx.close()));
}

async function tableC(browser, base, beach) {
  console.log('C. a full table of nine people, no key');
  const names = ['Matthew', 'Ann', 'Ben', 'Cat', 'Dev', 'Eve', 'Fin', 'Gus', 'Hal'];
  const ps = [];
  for (let i = 0; i < 9; i++) ps.push(await phone(browser, base));
  const host = ps[0];
  await host.page.click('#door-host');
  await fillMe(host.page, 'Matthew', 'curious, kind, restless');
  while ((await host.page.textContent('#cp-count')) !== '0') await host.page.click('#cp-less');
  check('C: at 0 computer players the lobby says no key is needed', (await host.page.textContent('#cp-line')) === 'Only people. No key needed.' && await host.page.locator('#key-part').isHidden());
  await host.page.evaluate(() => localStorage.removeItem('zero.key'));
  await host.page.waitForSelector('#places button');
  if (await host.page.locator('#places-more').isVisible()) await host.page.click('#places-more');
  await host.page.locator('#places button', { hasText: 'beneath lighthouse' }).click();
  await host.page.click('#open-go');
  await host.page.waitForSelector('#waiting:not([hidden])');
  const code = await host.page.textContent('#w-code');
  check('C: without a key the deal waits for all nine', await host.page.locator('#deal-go').isDisabled());
  for (let i = 1; i < 9; i++) {
    const p = ps[i];
    await p.page.click('#door-join');
    await fillMe(p.page, names[i], 'one, two, three');
    await p.page.fill('#join-code', code);
    await p.page.click('#join-go');
    await p.page.waitForSelector('#waiting:not([hidden])');
  }
  await host.page.waitForFunction(() => !document.querySelector('#deal-go').disabled, null, { timeout: 20000 });
  await host.page.click('#deal-go');
  const pool = ['beam', 'keeper', 'rocks', 'storm', 'gull', 'lens', 'spiral', 'foghorn', 'island', 'stairs', 'wick', 'mist'];
  await Promise.all(ps.map((p, i) => play(p, h => (h === 1 && (i === 4 || i === 5)) ? 'Lamp' : pool[(i + h * 3) % pool.length] + (h > 1 ? 'x'.repeat(i) : ''), { voteIndex: i % 3, timeout: 300000, line: h => `${names[i]} keeps the light for hand ${h}.`, stake: 1 + (i % 3) })));
  const t = await endChecks('C', beach, host.page, names);
  check('C: no computer seat at the table', Object.values(t.roster).every(r => r.kind === 'person'));
  check('C: Dev and Eve matched on Lamp and were knocked out', t.hands[0].out[5] && t.hands[0].out[6], JSON.stringify(t.hands[0].words));
  for (const p of ps) check('C: no page errors', !p.errors.length, p.errors.join(' / '));
  await Promise.all(ps.map(p => p.ctx.close()));
}

async function tableD(browser, base, beach) {
  console.log('D. someone leaves mid-game: the host goes on without them');
  const host = await phone(browser, base), ann = await phone(browser, base);
  await host.page.click('#door-host');
  await fillMe(host.page, 'Matthew', 'curious, kind, restless');
  await host.page.fill('#my-key', 'sk-ant-test');
  await host.page.click('#cp-less');
  await host.page.waitForSelector('#places button');
  await host.page.locator('#places button', { hasText: 'A new first word' }).click();
  await host.page.fill('#first-word', 'Orchard');
  await host.page.click('#open-go');
  await host.page.waitForSelector('#waiting:not([hidden])');
  const code = await host.page.textContent('#w-code');
  await ann.page.click('#door-join');
  await fillMe(ann.page, 'Ann', 'tea, rain, maps');
  await ann.page.fill('#join-code', code);
  await ann.page.click('#join-go');
  await ann.page.waitForSelector('#waiting:not([hidden])');
  await host.page.waitForFunction(() => /Everyone is here/.test(document.querySelector('#w-say').textContent), null, { timeout: 15000 });
  await host.page.click('#deal-go');
  await ann.ctx.close();                       // Ann goes, before writing a word
  // the host plays, and goes on without Ann whenever the table waits on her
  const deadline = Date.now() + 240000;
  let wentOn = 0;
  const loop = (async () => {
    while (Date.now() < deadline && !(await host.page.locator('#end').isVisible())) {
      const b = host.page.locator('#m-goon');
      if (await visible(b)) { await b.click().catch(() => {}); wentOn++; }
      await host.page.waitForTimeout(400);
    }
  })();
  await Promise.all([loop, play(host, h => ['blossom', 'ladder', 'cider', 'wasp'][h - 1], { voteIndex: 0 })]);
  check('D: the host was offered "Go on without Ann", and the table went on', wentOn >= 1, 'went on ' + wentOn + ' times');
  const t = await tableIn(beach, code);
  check('D: the game reached its end without her', t.over && t.hands.length === 4, 'hands ' + t.hands.length);
  check('D: Ann wrote no word and sat out, but the table kept her seat', t.hands[0].out[2] === true && t.roster[2].name === 'Ann');
  check('D: no page errors', !host.errors.length, host.errors.join(' / '));
  await host.ctx.close();
}

async function tableE(browser, base, beach) {
  console.log('E. the room is locked: the page says so');
  beach.locked.add('pool:zero');
  const host = await phone(browser, base);
  await host.page.click('#door-host');
  await fillMe(host.page, 'Matthew', 'curious, kind, restless');
  while ((await host.page.textContent('#cp-count')) !== '0') await host.page.click('#cp-less');
  await host.page.waitForSelector('#places button');
  await host.page.locator('#places button', { hasText: 'A new first word' }).click();
  await host.page.fill('#first-word', 'Quarry');
  await host.page.click('#open-go');
  await host.page.waitForFunction(() => document.querySelector('#lobby-error').textContent.length > 0, null, { timeout: 15000 });
  const said = await host.page.textContent('#lobby-error');
  check('E: a refused write is said plainly, and the page stays in the lobby', /refused that move: the room is locked/.test(said) && await host.page.locator('#lobby').isVisible(), said);
  beach.locked.delete('pool:zero');
  await host.ctx.close();
}

async function tableF(browser, base, beach) {
  console.log('F. Claude refuses the key: the host is asked for another');
  const ctx = await browser.newContext({ ...PHONE });
  let refuse = true;
  await ctx.route('https://api.anthropic.com/**', route => {
    if (refuse && route.request().method() === 'POST') return route.fulfill({ status: 401, headers: { 'access-control-allow-origin': '*' }, contentType: 'application/json', body: JSON.stringify({ type: 'error', error: { type: 'authentication_error', message: 'invalid x-api-key' } }) });
    return claudeRoute(route);
  });
  const page = await ctx.newPage();
  await page.goto(base);
  await page.click('#door-host');
  await fillMe(page, 'Matthew', 'curious, kind, restless');
  await page.fill('#my-key', 'sk-ant-wrong');
  await page.waitForSelector('#places button');
  await page.locator('#places button', { hasText: 'A new first word' }).click();
  await page.fill('#first-word', 'Meadow');
  await page.click('#open-go');
  await page.waitForSelector('#waiting:not([hidden])');
  await page.click('#deal-go');
  await page.waitForSelector('#m-key', { timeout: 20000 });
  const said = await page.textContent('#t-error');
  check('F: a refused key is said plainly, with a box for another', /didn’t accept this key/.test(said), said);
  refuse = false;
  await page.fill('#m-key', 'sk-ant-right');
  await page.click('#t-error button[type=submit]');
  await page.waitForSelector('#m-word', { timeout: 20000 });
  check('F: with a good key the computer players write, and the host\'s word opens', true);
  check('F: neither key reached the beach', !/sk-ant-(wrong|right)/.test(JSON.stringify(beach.blocks)));
  await ctx.close();
}

// ---- run ----------------------------------------------------------------------------------
const { server, beach, port } = await serve(0);
const base = `http://localhost:${port}/zero.html?beach=http://localhost:${port}&fast=1`;
const browser = await chromium.launch();
try {
  if (which.includes('A')) await tableA(browser, base, beach);
  if (which.includes('B')) await tableB(browser, base, beach);
  if (which.includes('C')) await tableC(browser, base, beach);
  if (which.includes('D')) await tableD(browser, base, beach);
  if (which.includes('E')) await tableE(browser, base, beach);
  if (which.includes('F')) await tableF(browser, base, beach);
} catch (e) {
  check('the run finished', false, e.stack || String(e));
  // what the room held when it stopped
  const all = Z.entriesIn(beach.blocks['pool:zero'], '', floorOf(beach.blocks['pool:zero'])).sort(Z.byAddr);
  all.slice(-25).forEach(x => console.log('   ', x.addr, x.kind, x.hand, x.who, JSON.stringify(x.data).slice(0, 160)));
} finally {
  await browser.close();
  server.close();
}
const failed = results.filter(r => !r.ok);
console.log(`\n${results.length - failed.length} of ${results.length} checks pass.`);
process.exit(failed.length ? 1 : 0);
