// Zero played end to end in a headless browser at phone size, against the
// beach on this computer (tests/zero-beach.mjs) with Claude stood in, so
// nothing reaches the real beach or spends a key (spine:zero 4.7).
//   node tests/zero-play.mjs            every table
//   node tests/zero-play.mjs A..G       one or more: A host + 8 computer seats,
//                                       B host + 2 phones by the link + 6 computer,
//                                       C nine people by the link, D someone leaves and
//                                       comes back by the link, E the room is locked,
//                                       F Claude refuses the host's key, G three people, no key
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
const which = (process.argv[2] || 'ABCDEFG').toUpperCase();

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
const calls = { word: 0, eye: 0, line: 0, vote: 0, pick: 0, headersOk: true, models: new Set(), badWords: [] };
const BANNED = /\b(ring|holds|lines?)\b/i;          // never on screen, nor in the computer players' instructions (spine:zero 4.23)
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
  if (BANNED.test(sys + ' ' + user)) calls.badWords.push((sys + ' ' + user).match(BANNED)[0]);
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
  } else if (/may send out a word/.test(user)) {
    // honest: only a word that plainly does not belong, never its own
    calls.eye++;
    const me = Number((/seat (\d): [^\n]* \(your own\)/.exec(user) || [])[1]);
    const odd = [...user.matchAll(/^seat (\d): (\S+)/gm)].find(m => /^spreadsheet$/i.test(m[2]) && Number(m[1]) !== me);
    out = { seat: odd ? Number(odd[1]) : 0, why: odd ? 'that is not a light' : 'they all belong' };
  } else if (/Answer as \{"sentence"/.test(user)) {
    calls.line++;
    const left = /still in after the flop are: ([^.]+)\./.exec(user)[1].split(', ');
    const chips = Number(/You have (\d+) chips/.exec(user)[1]);
    out = { sentence: `${left.slice(0, 3).join(' and ')}, as a ${persona.toLowerCase()} sees it.`, stake: Math.min(chips, (pi % 4) + 1) };
  } else if (/Vote for the sentence that brings the words together best/.test(user)) {
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
async function phone(browser, base, clipboard) {
  const ctx = await browser.newContext({ ...PHONE, ...(clipboard ? { permissions: ['clipboard-read', 'clipboard-write'] } : {}) });
  await ctx.route('https://api.anthropic.com/**', claudeRoute);
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  page.on('console', m => { if (m.type() === 'error' && !/Failed to load resource/.test(m.text())) errors.push(m.text()); });
  await page.goto(base);
  return { ctx, page, errors };
}
// Through the door (door.js), as on Matthew's other pages: "I'm new", a name,
// the three words it offers, the card. With `asked`, the sheet has already
// opened by itself (a seat taken before a name).
async function signUp(page, name, asked) {
  if (!asked) await page.locator('#who-line button', { hasText: 'I’m new' }).click();
  await page.fill('#door-name', name);
  if (name === 'Matthew') await shot(page, 'door-new');
  const words = await page.locator('.door-words span').allTextContents();
  await page.check('#door-adult');
  await page.getByRole('button', { name: 'Carry on' }).click();
  await page.getByRole('button', { name: 'I’ve kept them' }).waitFor();
  await page.waitForFunction(() => { const i = document.querySelector('.door-card'); return !i || (i.src && i.src.startsWith('data:image/png')); });
  const card = await page.locator('.door-card').getAttribute('alt');
  if (name === 'Matthew') await shot(page, 'door-card');
  await page.getByRole('button', { name: 'I’ve kept them' }).click();
  return { words, card };
}
async function shot(page, name) {
  if (!SHOTS) return;
  fs.mkdirSync(SHOTS, { recursive: true });
  await page.screenshot({ path: path.join(SHOTS, name + '.png'), fullPage: true });
}
// Every screen a person sees, read for the words that must never appear.
const seenBanned = [];
async function watchWords(p) {
  const hit = await p.page.evaluate((src) => {
    const re = new RegExp(src, 'i');
    let txt = '';
    ['lobby', 'invited', 'waiting', 'table', 'end', 'trailview'].forEach(id => { const n = document.getElementById(id); if (n && !n.hidden) txt += ' ' + n.innerText; });
    const m = txt.match(re);
    return m ? m[0] + ' in: …' + txt.slice(Math.max(0, m.index - 60), m.index + 40).replace(/\s+/g, ' ') + '…' : '';
  }, BANNED.source).catch(() => '');
  if (hit) seenBanned.push(hit);
}
const visible = async (loc) => (await loc.count()) > 0 && await loc.first().isVisible();

// Play whatever the panel asks, until the poem. words(hand) gives this person's word.
async function play(p, words, opts = {}) {
  const { page } = p;
  const seen = new Set();
  const deadline = Date.now() + (opts.timeout || 240000);
  let hand = 0;
  p.saw = p.saw || {};
  while (Date.now() < deadline) {
    await watchWords(p);
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
      } else if (await visible(page.locator('.picks.eye button'))) {
        p.saw.eye = p.saw.eye || /Does any word not belong under .+\? Tap one, or tap Skip\./.test(await page.textContent('#t-moves'));
        if (opts.shots && !seen.has('eye' + hand)) { seen.add('eye' + hand); await shot(page, opts.shots + '-hand-' + hand + '-the-eye'); }
        const target = opts.eye ? opts.eye(hand) : null;
        const tile = target && page.locator('.picks.eye button:not([disabled])', { hasText: new RegExp('^' + target + '$') });
        if (tile && await visible(tile)) {
          await tile.click();
          await page.locator('#t-moves .picks:not(.eye) button', { hasText: 'Seal it' }).click();
        } else {
          await page.locator('#t-moves button', { hasText: 'Skip' }).click();
        }
      } else if (await visible(page.locator('#m-line'))) {
        p.saw.turn = p.saw.turn || /Write a short sentence that brings all these words together\./.test(await page.textContent('#t-moves'));
        await page.fill('#m-line', opts.line ? opts.line(hand) : 'Everything here keeps someone company.');
        await page.locator('#m-stake').evaluate((el, v) => { el.value = String(Math.min(Number(el.max), v)); el.dispatchEvent(new Event('input')); }, opts.stake || 2);
        await page.locator('#t-moves form button[type=submit]').click();
      } else if (await visible(page.locator('.slip:not([disabled])'))) {
        p.saw.vote = p.saw.vote || /Vote for the sentence that brings the words together best\. Not your own\./.test(await page.textContent('#t-moves'));
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
  await watchWords({ page });
  check(label + ': no screen said ring, holds or line', !seenBanned.length, seenBanned.slice(0, 3).join(' / '));
  check(label + ': four hands played to the end', t.over && t.hands.length === 4, `hands ${t.hands.length}, phase ${t.phase}`);
  const chips = Object.values(t.chips).reduce((a, b) => a + b, 0), seats = Object.keys(t.roster).length;
  check(label + ': chips are kept (' + 10 * seats + ' in all)', chips === 10 * seats, 'total ' + chips);
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
// The host opens a table: through the door, a place, Open the table, the waiting room.
async function hostOpens(p, name, place, first) {
  const { page } = p;
  await page.click('#door-host');
  await signUp(page, name);
  await page.waitForSelector('#places button');
  if (place !== 'A new first word' && !(await visible(page.locator('#places button', { hasText: place }))) && await page.locator('#places-more').isVisible()) await page.click('#places-more');
  await page.locator('#places button', { hasText: place }).click();
  if (first) await page.fill('#first-word', first);
  await page.click('#open-go');
  await page.waitForSelector('#waiting:not([hidden])');
  return { code: await page.textContent('#w-code'), link: await page.inputValue('#w-link') };
}
async function addKey(page, key) {
  await page.fill('#w-key', key);
  await page.click('#w-key-go');
}
// Someone opens the invite link on a phone with no name yet: the door opens by
// itself, and once they are named they are seated, with no code typed.
async function arrives(browser, link, name) {
  const p = await phone(browser, link);
  await signUp(p.page, name, true);
  await p.page.waitForSelector('#waiting:not([hidden]), #table:not([hidden])');
  return p;
}
async function atTable(page) {
  return (await page.locator('#w-roster li').allTextContents()).map(s => s.replace(/\s+/g, ' ').trim());
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

async function tableA(browser, base, beach) {
  console.log('A. one host with eight computer seats');
  const host = await phone(browser, base, true);
  const { page } = host;
  await shot(page, 'a-lobby');
  check('A: the lobby has no computer-player count now (4.25)', (await page.locator('#cp-count').count()) === 0);
  check('A: the lobby says how it plays', /Write a word nobody else will think of/.test(await page.textContent('#lobby')));
  await page.click('#door-host');
  check('A: before a name, the lobby offers the door: I’m new · I’ve been here', /I’m new · I’ve been here/.test(await page.textContent('#who-line')));
  const door = await signUp(page, 'Matthew');
  check('A: the door offers three words, and a card showing the name and the words', door.words.length === 3 && door.card.includes('Matthew') && door.words.every(w => door.card.includes(w)), JSON.stringify(door));
  check('A: the name is a real name on the beach (passport:Matthew)', !!beach.blocks['passport:Matthew']);
  await page.waitForSelector('#places button');
  const rows = await page.locator('#places button').allTextContents();
  check('A: the lobby offers a new first word and the open words', rows[0] === 'A new first word' && rows.some(r => r.includes('beneath torch')) && !rows.some(r => /candle|vigil|Light/.test(r)), rows.join(' | '));
  await shot(page, 'a-lobby-host');
  await page.locator('#places button', { hasText: 'beneath torch' }).click();
  await page.click('#open-go');
  await page.waitForSelector('#waiting:not([hidden])');
  const code = await page.textContent('#w-code'), link = await page.inputValue('#w-link');
  check('A: the waiting room shows the code large, a word and a number', /^[A-Z]+ [1-9]$/.test(code), code);
  check('A: the invite link is zero.html?table=<code>', new URL(link).pathname.endsWith('/zero.html') && new URL(link).searchParams.get('table') === code.replace(' ', ''), link);
  check('A: the page\'s own address is the invite link, so a reload comes back', page.url() === link, page.url());
  await page.click('#w-copy');
  const copied = await page.evaluate(() => navigator.clipboard.readText()).catch(() => '');
  check('A: Copy the link copies it', copied === link, copied);
  check('A: the share button shows only where the phone can share', (await page.locator('#w-share').isHidden()) === !(await page.evaluate(() => !!navigator.share)));
  check('A: At the table lists the host, marked', (await atTable(page)).join('|') === 'Matthewhost', (await atTable(page)).join('|'));
  check('A: alone and without a key, Deal waits (three people, or a key)', await page.locator('#deal-go').isDisabled() && /three people or more/.test(await page.textContent('#w-say')));
  await shot(page, 'a-waiting-nokey');
  await addKey(page, 'sk-ant-test');
  check('A: with a key, the empty seats say the computer fills them', /^8 seats empty · computer fills them$/.test(await page.textContent('#w-empty')), await page.textContent('#w-empty'));
  check('A: and the host is told to keep the page open', /Keep this page open while you play. The computer players live on your phone./.test(await page.textContent('#w-keep')));
  check('A: the key stays in this browser and never reaches the beach', !JSON.stringify(beach.blocks).includes('sk-ant-test'));
  await shot(page, 'a-waiting');
  await page.click('#deal-go');
  await page.waitForSelector('#table:not([hidden])');
  await play(host, h => ['flicker', 'spreadsheet', 'match', 'wick'][h - 1], { shots: SHOTS ? 'a' : '', voteIndex: 1 });
  await shot(page, 'a-end');
  const t = await endChecks('A', beach, page, ['Matthew']);
  check('A: the turn says "Write a short sentence that brings all these words together."', host.saw.turn);
  check('A: the vote says "Vote for the sentence that brings the words together best. Not your own."', host.saw.vote);
  check('A: the table\'s eye asks "Does any word not belong under …? Tap one, or tap Skip."', host.saw.eye);
  const h2 = t.hands[1];
  check('A: the computer players sent out the word that did not belong (spreadsheet), by more than half the seats', h2.eyeOut[1] === true && h2.marked[1] > 4.5, JSON.stringify({ words: h2.words, marked: h2.marked }));
  check('A: and marked honestly otherwise: no other word was marked in any hand', t.hands.every(h => Object.keys(h.marked).every(k => h.words[k] === 'spreadsheet')));
  check('A: the host, sent out, wrote no sentence but still voted', !h2.lines[1] && h2.votes[1] != null);
  check('A: the seat sent out stays empty in the tree', Z.nodeAt(beach.blocks.zero, h2.place + '1') == null);
  check('A: the computer players\' instructions never say ring, holds or line', !calls.badWords.length && calls.eye > 0, calls.badWords.slice(0, 3).join(','));
  check('A: computer players took all eight empty seats at the deal', Object.values(t.roster).filter(r => r.kind === 'computer').length === 8);
  const owed = t.hands.reduce((a, h) => a + h.active.filter(s => Z.isComputer(t, s)).length, 0);
  check('A: the computer seats moved through Claude with the browser headers, on claude-haiku-5-5', calls.word >= owed && calls.headersOk && calls.models.has('claude-haiku-5-5') && calls.models.size === 1, JSON.stringify({ ...calls, models: [...calls.models] }));
  check('A: the computers sealed before the host could write, every step', await computersFirst(beach, code));
  check('A: the key is still nowhere on the beach', !JSON.stringify(beach.blocks).includes('sk-ant-test'));
  await keepAndNote('A', page, beach);
  check('A: no page errors', !host.errors.length, host.errors.join(' / '));
  await host.ctx.close();
}

async function tableB(browser, base, beach) {
  console.log('B. the host and two phones by the invite link; computer players fill the rest');
  const host = await phone(browser, base);
  const { code, link } = await hostOpens(host, 'Mira', 'A new first word', 'Harbour');
  await addKey(host.page, 'sk-ant-test');
  // Ann opens the link with no name: the door opens by itself, then she is seated
  const ann = await arrives(browser, link, 'Ann');
  check('B: the link takes a newcomer through the door and straight to a seat, no code typed', await ann.page.locator('#waiting').isVisible() && (await ann.page.inputValue('#w-link')) === link);
  check('B: a seated person sees the waiting line', /You’re seated. The game starts when the host deals./.test(await ann.page.textContent('#w-say')));
  // Ben already has a name on this phone: the link seats him at once
  const ben = await phone(browser, base);
  await ben.page.click('#door-join');
  await signUp(ben.page, 'Ben');
  await ben.page.goto(link);
  await ben.page.waitForSelector('#waiting:not([hidden])');
  check('B: a phone that already has a name goes straight to the seat', true);
  await shot(ann.page, 'b-seated');
  await host.page.waitForFunction(() => document.querySelectorAll('#w-roster li').length === 3, null, { timeout: 15000 });
  check('B: At the table lists who has arrived, the host marked', (await atTable(host.page)).join('|') === 'Mirahost|Ann|Ben', (await atTable(host.page)).join('|'));
  check('B: one invited… two invited plus the host: six seats empty, computer fills them', /^6 seats empty · computer fills them$/.test(await host.page.textContent('#w-empty')), await host.page.textContent('#w-empty'));
  check('B: the others are told the table pauses if the host leaves', /pauses until they come back/.test(await ann.page.textContent('#w-keep')));
  await shot(host.page, 'b-waiting-host');
  await host.page.click('#deal-go');
  await host.page.waitForSelector('#table:not([hidden])');
  check('B: the new first word is written at a free top seat', Object.keys(beach.blocks.zero).some(k => /^\d$/.test(k) && /^Harbour · Mira/.test(Z.textOf(beach.blocks.zero[k]))));
  // someone arriving after the deal is turned away
  const late = await phone(browser, link);
  await signUp(late.page, 'Cat', true);
  await late.page.waitForFunction(() => document.querySelector('#i-error').textContent.length > 0, null, { timeout: 15000 });
  check('B: a person arriving by the link after the deal is told the table has started', /already started/.test(await late.page.textContent('#i-error')), await late.page.textContent('#i-error'));
  await late.ctx.close();
  await Promise.all([
    play(host, h => ['mooring', 'gull', 'keel', 'lantern'][h - 1], { voteIndex: 0 }),
    play(ann, h => ['jetty', 'bollard', 'net', 'buoy'][h - 1], { voteIndex: 1, shots: SHOTS ? 'b-ann' : '' }),
    play(ben, h => ['Jetties', 'crane', 'anchor', 'chart'][h - 1], { voteIndex: 2 })
  ]);
  const t = await endChecks('B', beach, host.page, ['Mira', 'Ann', 'Ben']);
  check('B: computer players took the six empty seats at the deal', Object.values(t.roster).filter(r => r.kind === 'computer').length === 6);
  check('B: jetty and Jetties match at the flop and knock each other out', t.hands[0].out[2] && t.hands[0].out[3], JSON.stringify(t.hands[0].words));
  check('B: knocked-out seats still voted', t.hands[0].votes[2] != null && t.hands[0].votes[3] != null);
  for (const p of [host, ann, ben]) check('B: no page errors', !p.errors.length, p.errors.join(' / '));
  await Promise.all([host, ann, ben].map(p => p.ctx.close()));
}

async function tableC(browser, base, beach) {
  console.log('C. a full table of nine people by the link, no key');
  const names = ['Hugo', 'Iris', 'Jack', 'Kate', 'Liam', 'Mona', 'Nell', 'Owen', 'Pia'];
  const host = await phone(browser, base);
  const { code, link } = await hostOpens(host, 'Hugo', 'beneath lighthouse');
  const ps = [host];
  for (let i = 1; i < 9; i++) ps.push(await arrives(browser, link, names[i]));
  await host.page.waitForFunction(() => document.querySelectorAll('#w-roster li').length === 9, null, { timeout: 20000 });
  check('C: every seat is taken', /Every seat is taken/.test(await host.page.textContent('#w-empty')));
  await host.page.click('#deal-go');
  const pool = ['beam', 'keeper', 'rocks', 'storm', 'gull', 'lens', 'spiral', 'foghorn', 'island', 'stairs', 'wick', 'mist'];
  await Promise.all(ps.map((p, i) => play(p, h => (h === 1 && (i === 4 || i === 5)) ? 'Lamp' : pool[(i + h * 3) % pool.length] + (h > 1 ? 'x'.repeat(i) : ''), { voteIndex: i % 3, timeout: 300000, line: h => `${names[i]} keeps the light for hand ${h}.`, stake: 1 + (i % 3), eye: h => h === 1 ? 'mist' : null })));
  const t = await endChecks('C', beach, host.page, names);
  check('C: nine people, no computer seat', Object.keys(t.roster).length === 9 && Object.values(t.roster).every(r => r.kind === 'person'));
  check('C: Liam and Mona matched on Lamp and were knocked out', t.hands[0].out[5] && t.hands[0].out[6], JSON.stringify(t.hands[0].words));
  check('C: eight people marked mist and the table sent it out; Pia could not mark her own', t.hands[0].eyeOut[9] === true && t.hands[0].marked[9] === 8 && t.hands[0].marks[9] == null, JSON.stringify(t.hands[0].marked));
  for (const p of ps) check('C: no page errors', !p.errors.length, p.errors.join(' / '));
  await Promise.all(ps.map(p => p.ctx.close()));
}

async function tableG(browser, base, beach) {
  console.log('G. three people and no key: the table plays with who is there');
  const host = await phone(browser, base);
  const { code, link } = await hostOpens(host, 'Quinn', 'beneath photon');
  const rae = await arrives(browser, link, 'Rae');
  await host.page.waitForFunction(() => document.querySelectorAll('#w-roster li').length === 2, null, { timeout: 15000 });
  check('G: with two and no key, Deal waits for a third', await host.page.locator('#deal-go').isDisabled());
  const sol = await arrives(browser, link, 'Sol');
  await host.page.waitForFunction(() => !document.querySelector('#deal-go').disabled, null, { timeout: 15000 });
  check('G: with three, Deal is ready, and says the table plays with the three', /plays with the 3 of you/.test(await host.page.textContent('#w-say')), await host.page.textContent('#w-say'));
  await host.page.click('#deal-go');
  await Promise.all([
    play(host, h => ['proton', 'quark', 'lepton', 'boson'][h - 1], { voteIndex: 0, eye: h => h === 1 ? 'sun' : null }),
    play(rae, h => ['ray', 'glow', 'flash', 'gleam'][h - 1], { voteIndex: 0, eye: h => h === 1 ? 'sun' : null }),
    play(sol, h => ['sun', 'heat', 'spark', 'blaze'][h - 1], { voteIndex: 0 })
  ]);
  const t = await endChecks('G', beach, host.page, ['Quinn', 'Rae', 'Sol']);
  check('G: two of three is more than half: the table sent out sun', t.hands[0].eyeOut[3] === true && t.hands[0].marked[3] === 2);
  check('G: three seats only, no computer, empty seats left empty', Object.keys(t.roster).join(',') === '1,2,3' && Object.values(t.roster).every(r => r.kind === 'person'));
  for (const p of [host, rae, sol]) check('G: no page errors', !p.errors.length, p.errors.join(' / '));
  await Promise.all([host, rae, sol].map(p => p.ctx.close()));
}

async function tableD(browser, base, beach) {
  console.log('D. someone leaves mid-game and comes back by the same link');
  const host = await phone(browser, base);
  const { code, link } = await hostOpens(host, 'Dora', 'A new first word', 'Orchard');
  await addKey(host.page, 'sk-ant-test');
  const abe = await phone(browser, link);
  const abeDoor = await signUp(abe.page, 'Abe', true);
  await abe.page.waitForSelector('#waiting:not([hidden])');
  await host.page.waitForFunction(() => document.querySelectorAll('#w-roster li').length === 2, null, { timeout: 15000 });
  await host.page.click('#deal-go');
  await abe.ctx.close();                       // Abe goes, before writing a word
  let wentOn = 0, back = null;
  const deadline = Date.now() + 300000;
  const loop = (async () => {
    while (Date.now() < deadline && !(await host.page.locator('#end').isVisible())) {
      const b = host.page.locator('#m-goon');
      if (await visible(b)) { await b.click().catch(() => {}); wentOn++; }
      // in hand two Abe opens the same link on a fresh phone and says who he is
      if (!back && /Hand two/.test((await host.page.textContent('#t-phase').catch(() => '')) || '')) {
        back = await phone(browser, link);
        await back.page.getByRole('button', { name: 'I’ve been here before' }).click();
        await back.page.fill('#door-name2', 'Abe');
        await back.page.fill('#door-key', abeDoor.words.join('-'));
        await back.page.getByRole('button', { name: 'Come in' }).click();
        await back.page.waitForSelector('#table:not([hidden])', { timeout: 20000 });
        back.playing = play(back, h => ['pip', 'core', 'stalk', 'seed'][h - 1], { voteIndex: 0 });
      }
      await host.page.waitForTimeout(400);
    }
  })();
  await Promise.all([loop, play(host, h => ['blossom', 'ladder', 'cider', 'wasp'][h - 1], { voteIndex: 0 })]);
  if (back) await back.playing;
  check('D: the host was offered "Go on without Abe", and the table went on', wentOn >= 1, 'went on ' + wentOn + ' times');
  const t = await tableIn(beach, code);
  check('D: the game reached its end', t.over && t.hands.length === 4, 'hands ' + t.hands.length);
  check('D: Abe wrote no word in hand one and sat it out, but kept his seat', t.hands[0].out[2] === true && t.roster[2].name === 'Abe');
  check('D: Abe came back by the same link with his name and words, to his own seat', !!back && JSON.parse(await back.page.evaluate(() => localStorage.getItem('zero.table'))).seat === 2);
  check('D: and played on from there', t.hands.slice(1).some(h => ['word', 'line', 'vote'].some(st => h.steps[st] && h.steps[st].seals[2])));
  check('D: no page errors', !host.errors.length && (!back || !back.errors.length), host.errors.concat(back ? back.errors : []).join(' / '));
  await host.ctx.close();
  if (back) await back.ctx.close();
}

async function tableE(browser, base, beach) {
  console.log('E. the room is locked: the page says so');
  beach.locked.add('pool:zero');
  const host = await phone(browser, base);
  await host.page.click('#door-host');
  await signUp(host.page, 'Edith');
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
  await hostOpens({ page }, 'Fred', 'A new first word', 'Meadow');
  await addKey(page, 'sk-ant-wrong');
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
  if (which.includes('G')) await tableG(browser, base, beach);
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
