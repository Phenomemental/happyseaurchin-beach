// Zero played end to end in a headless browser at phone size, against the
// beach on this computer (tests/zero-beach.mjs) with Claude stood in, so
// nothing reaches the real beach or spends a key (spine:zero 4.7).
//   node tests/zero-play.mjs            every table
//   node tests/zero-play.mjs A..G       one or more: A host + 8 computer seats,
//                                       B host + 2 phones by the link + 6 computer,
//                                       C nine people by the link, D someone leaves and
//                                       comes back by the link, E the room is locked,
//                                       F Claude refuses the host's key, G three people, no key,
//                                       H Look around, I closing the game
// Screenshots, when asked for with SHOTS=<dir>, land there.
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import { serve } from './zero-beach.mjs';
const require = createRequire(import.meta.url);
const { chromium, devices } = require('playwright');   // the machine's own Playwright
const Z = require('../zero-law.js');
const L = require('../zero-look.js');

const PHONE = devices['iPhone 13'];
const SHOTS = process.env.SHOTS || '';
const which = (process.argv[2] || 'ABCDEFGIJH').toUpperCase();

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
  if (!asked) await page.locator('#f-who button', { hasText: 'I’m new' }).click();
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
// Word chips on the cards (spine:zero 4.32): read at the showdown, when the
// bank has paid, and again when a word is chosen.
async function earnWhere(p, hand) {
  const r = await p.page.evaluate(() => {
    const t = document.getElementById('table');
    if (!t || t.hidden) return null;
    const ph = document.getElementById('t-phase').textContent, moves = document.getElementById('t-moves').textContent;
    const labels = {};
    document.querySelectorAll('#t-ring .seat').forEach(seat => {
      const who = seat.querySelector('.who').textContent.replace(/ · left$/, '');
      const e = seat.querySelector('.earn');
      labels[who] = e ? e.textContent : '';
    });
    // every word and name on one line, inside its card (a name may reach a little past it)
    const fits = Array.from(document.querySelectorAll('#t-ring .seat')).every(seat => {
      const c = seat.querySelector('.card'), w = seat.querySelector('.card .w'), who = seat.querySelector('.who');
      return (!w || (w.offsetWidth <= c.clientWidth && w.getClientRects().length === 1)) && who.scrollWidth <= who.clientWidth + 1 && who.getBoundingClientRect().height < 24;
    });
    const slip = document.querySelector('#t-moves .slip:disabled');
    return { showdown: /the showdown/.test(ph), picked: / picked /.test(moves), labels, fits, shown: !!document.querySelector('#t-ring .card .w'),
      slip: slip ? Number(getComputedStyle(slip).opacity) : null, chipsNote: /Word chips: \+1 for a word still standing, \+2 if it is new to the tree\./.test(moves) };
  }).catch(() => null);
  if (!r) return;
  p.saw = p.saw || {};
  if (r.shown) p.saw.fits = p.saw.fits !== false && r.fits;
  if (r.slip != null) p.saw.slip = Math.min(p.saw.slip == null ? 1 : p.saw.slip, r.slip);
  if (r.chipsNote) p.saw.chipsNote = true;
  if (!hand) return;
  p.earn = p.earn || {}; p.chosen = p.chosen || {};
  if (r.showdown && !p.earn[hand]) p.earn[hand] = r.labels;
  if (r.picked && !p.chosen[hand]) p.chosen[hand] = r.labels;
}
// The foot of every screen (spine:zero 4.24), read from whatever screen is showing: who is at
// this phone with "not you?", Back to the lobby, Close the game on game screens, then the credits,
// in that order, beneath the screen, and the credits over no control.
async function footAt(page) {
  return page.evaluate(() => {
    const on = ['lobby', 'invited', 'waiting', 'table', 'end', 'trailview', 'look'].find(id => !document.getElementById(id).hidden);
    if (!on) return null;
    let screen = on;
    const over = on === 'table' && Array.from(document.querySelectorAll('#t-moves button')).some(b => b.textContent === 'The poem');
    if (on === 'table') {
      const moves = document.getElementById('t-moves');
      const ph = (document.getElementById('t-phase').textContent.split('·')[1] || '').trim();
      screen = over ? 'the game over' : moves.querySelector('.picks.eye') || /Does any word not belong/.test(moves.textContent) ? 'the eye' : ph || 'finding the table';
    }
    const form = document.getElementById('lobby-form');
    if (on === 'lobby' && !form.hidden) screen = !document.getElementById('host-part').hidden ? 'lobby, hosting' : 'lobby, joining';
    const vis = n => !n.hidden && n.offsetParent !== null && n.getBoundingClientRect().height > 0;
    const r = n => n.getBoundingClientRect();
    const who = document.getElementById('f-who'), back = document.getElementById('f-back-part'), close = document.getElementById('t-close'), credits = document.querySelector('footer.foot');
    const shown = [who, back, close, credits].filter(vis);
    const inOrder = shown.every((x, k) => !k || r(x).top >= r(shown[k - 1]).bottom - 1);
    const text = who.textContent;
    const signed = /^You are .+ on this phone · not you\?$/.test(text), door = text === 'I’m new · I’ve been here';
    const wantBack = !(on === 'lobby' && form.hidden), wantClose = on === 'table' && !over;
    const below = r(who).top >= r(document.getElementById(on)).bottom - 1;
    const cr = r(credits);
    const controls = Array.from(document.querySelectorAll('button, input, textarea, a[href], select')).filter(c => !credits.contains(c) && vis(c));
    const clear = controls.every(c => { const x = r(c); return x.bottom <= cr.top || x.top >= cr.bottom || x.right <= cr.left || x.left >= cr.right; });
    const last = credits.nextElementSibling === null && credits.lastElementChild.textContent === 'made by Matthew Bushell' &&
      /With thanks to David Pinto, who built the ground these pages stand on/.test(credits.textContent) && credits.querySelector('a').href === 'https://beach.happyseaurchin.com/';
    const ok = vis(who) && (signed || door) && vis(back) === wantBack && vis(close) === wantClose && inOrder && below && clear && last;
    return { screen, ok, text, why: JSON.stringify({ text, back: vis(back), wantBack, close: vis(close), wantClose, inOrder, below, clear, last }) };
  }).catch(() => null);
}
// Recorded on every screen a phone passes through: the first time, and any time it is wrong.
async function recordFoot(p) {
  const f = await footAt(p.page);
  if (!f) return f;
  p.footAt = p.footAt || {};
  if (!(f.screen in p.footAt) || !f.ok) p.footAt[f.screen] = f.ok ? true : f.why;
  p.typeAt = p.typeAt || {};
  if (!(f.screen in p.typeAt)) p.typeAt[f.screen] = await typeAt(p.page);
  return f;
}
// Every screen a person sees, read for the words that must never appear.
const seenBanned = [];
async function watchWords(p) {
  const hit = await p.page.evaluate((src) => {
    const re = new RegExp(src, 'i');
    let txt = '';
    ['lobby', 'invited', 'waiting', 'table', 'end', 'trailview', 'look'].forEach(id => { const n = document.getElementById(id); if (n && !n.hidden) txt += ' ' + n.innerText; });
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
  p.footAt = p.footAt || {};
  while (Date.now() < deadline) {
    await watchWords(p);
    if (opts.until && await opts.until(hand)) return true;
    if (await page.locator('#end').isVisible()) return true;
    const phase = (await page.locator('#t-phase').textContent().catch(() => '')) || '';
    const m = /Hand (one|two|three|four)/.exec(phase);
    if (m) hand = ['one', 'two', 'three', 'four'].indexOf(m[1]) + 1;
    await recordFoot(p);
    await earnWhere(p, hand);
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
        p.stakeMax = p.stakeMax || {};
        if (!(hand in p.stakeMax)) p.stakeMax[hand] = Number(await page.getAttribute('#m-stake', 'max'));
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

// The table now playing under this code: its entries from its own open on (a code may come round again).
async function tableIn(beach, code) {
  const all = Z.entriesIn(beach.blocks['pool:zero'], '', floorOf(beach.blocks['pool:zero'])).sort(Z.byAddr).filter(e => Z.codeKey(e.code) === Z.codeKey(code));
  const from = all.map(e => e.kind).lastIndexOf('open');
  return Z.derive(from > 0 ? all.slice(from) : all, code);
}
function floorOf(b) { let f = 0, n = b; while (n && typeof n === 'object' && n._ !== undefined) { f++; n = n._; } return f; }

// What this game shows, second version (spine:zero 4.91), checked wherever it is drawn.
const HINT = 'Tap a name to light only their votes.';
async function webChecks(label, page, where, g) {
  const size = await page.evaluate(w => {
    const s = document.querySelector(w + ' svg.web').getBoundingClientRect(), box = document.querySelector(w + ' .look-shows').getBoundingClientRect();
    const name = document.querySelector(w + ' svg.web .wn text').getBoundingClientRect();
    return { w: Math.round(s.width), h: Math.round(s.height), sheet: Math.round(box.width), name: Math.round(name.height) };
  }, where);
  check(label + ': the web is the full width of the sheet and about as tall as it is wide, its names larger', Math.abs(size.w - size.sheet) <= 2 && size.h / size.w > 0.9 && size.h / size.w < 1.1 && size.name >= 12, JSON.stringify(size));
  const pairs = new Set(g.votes.map(v => v.from + '>' + v.to)).size;
  // the player with the most arrows, so the lighting is seen to choose
  const who = g.people.slice().sort((a, b) => g.votes.filter(v => v.from === b || v.to === b).length - g.votes.filter(v => v.from === a || v.to === a).length)[0];
  const gave = new Set(g.votes.filter(v => v.from === who).map(v => v.to)), got = new Set(g.votes.filter(v => v.to === who).map(v => v.from));
  const node = page.locator(where + ` svg.web .wn[data-who="${who}"]`);
  await node.click();
  const lit = await page.evaluate(w => {
    const s = document.querySelector(w + ' svg.web'), n = c => s.querySelectorAll('path.edge.' + c).length;
    return { lit: s.classList.contains('lit'), out: n('out'), in: n('in'), dim: n('dim'), line: document.querySelector(w + ' .web-line').textContent, on: (s.querySelector('.wn.on') || {}).textContent };
  }, where);
  check(label + ': tapping a name (' + who + ') lights only the arrows to and from that player', lit.lit && lit.on === who && lit.out === gave.size && lit.in === got.size && lit.dim === pairs - gave.size - got.size, JSON.stringify(lit).slice(0, 160));
  check(label + ': with a line saying whom they voted for and who voted for them', (gave.size ? lit.line.startsWith(who + ' voted for ') : lit.line.startsWith(who + ' gave no vote.')) &&
    [...gave].every(n => lit.line.split('. ')[0].includes(n)) && (got.size ? [...got].every(n => lit.line.split('. ').slice(1).join('. ').includes(n)) && lit.line.endsWith(' voted for ' + who + '.') : lit.line.endsWith('Nobody voted for ' + who + '.')), lit.line);
  await node.click();
  check(label + ': and tapping it again puts every arrow back', !(await page.locator(where + ' svg.web.lit').count()) && (await page.textContent(where + ' .web-line')) === HINT);
}
// The words a game put into the tree, each once (case and singular form ignored, as the law has it).
function keptWords(g) {
  const kept = [];
  g.rounds.forEach(r => r.words.forEach(w => { if (w.kept && !kept.some(k => Z.same(k, w.word))) kept.push(w.word); }));
  return kept;
}
// What this game shows as rows that open (the expansion boxes), checked wherever it is drawn:
// every reading keeps its heading and one plain line in view, and the rows with more beneath open on a tap.
async function readingChecks(label, page, where, g) {
  const R = L.readings(g), K = L.takes(g, R);
  const n1 = x => String(Math.round(x * 10) / 10);
  const state = () => page.evaluate(w => Array.from(document.querySelectorAll(w + ' .look-shows > .look-sec')).map(s => ({
    key: s.dataset.reading, box: s.tagName === 'DETAILS', open: !!s.open, take: s.querySelector('.look-take').textContent,
    seen: s.querySelector('.look-take').checkVisibility(), more: s.querySelector('.look-more') && s.querySelector('.look-more').checkVisibility() ? s.querySelector('.look-more').getBoundingClientRect().height : 0 })), where);
  let rows = await state();
  check(label + ': every reading shows its heading and one plain line, folded or not', JSON.stringify(rows.map(r => [r.key, r.take])) === JSON.stringify(Object.keys(K).map(k => [k, K[k].text])) && rows.every(r => r.seen),
    rows.filter(r => !K[r.key] || r.take !== K[r.key].text).map(r => r.key + ': ' + r.take).join(' / ').slice(0, 200));
  check(label + ': the readings with more beneath are boxes that open; the web stands open, the rest folded', rows.every(r => K[r.key] && r.box === K[r.key].more && r.open === (r.box && r.key === 'votes') && (r.open ? r.more > 0 : r.more === 0)),
    rows.map(r => r.key + (r.box ? (r.open ? ':open' : ':shut') : ':plain')).join(' '));
  const shut = rows.find(r => r.box && r.key !== 'votes');
  if (shut) {
    const sum = page.locator(`${where} .look-sec[data-reading="${shut.key}"] > summary`), more = page.locator(`${where} .look-sec[data-reading="${shut.key}"] .look-more`);
    await sum.click();
    const opened = await more.isVisible();
    await sum.click();
    check(label + ': tapping a folded row (' + shut.key + ') opens it, and tapping again folds it', opened && !(await more.isVisible()));
  }
  const all = page.locator(where + ' .shows-all');
  const before = await all.textContent();
  await all.click();
  rows = await state();
  check(label + ': Open all opens every box at once, and then offers Close all', before === 'Open all' && rows.filter(r => r.box).every(r => r.open && r.more > 0) && (await all.textContent()) === 'Close all');
  await watchWords({ page });       // everything open: read for the words that must never appear
  const type = await typeAt(page);
  check(label + ': with everything open the words are large and dark enough to read (none under 12px; quiet text at 4.5 to 1 or better)', typeOk(type), typeSay(type));
  const sec = async k => (await page.textContent(where + ` .look-sec[data-reading="${k}"]`)) || '';
  const items = k => page.locator(where + ` .look-sec[data-reading="${k}"] .look-more li`).allTextContents();
  const cells = k => page.locator(where + ` .look-sec[data-reading="${k}"] tbody tr`).evaluateAll(trs => trs.map(r => Array.from(r.children).map(c => c.textContent)));
  // words new to the tree
  const fresh = await page.locator(where + ' .look-list.fresh .lw').allTextContents();
  check(label + ': words new to the tree: the words nobody had played anywhere in it before the game', g.fresh.length ? JSON.stringify(fresh) === JSON.stringify(g.fresh.map(x => x.word)) && K.fresh.text.startsWith(g.fresh.length === keptWords(g).length ? 'All ' + g.fresh.length + ' words' : g.fresh.length + ' of the ' + keptWords(g).length + ' words')
    : /had been played in it before|put no word/.test(await sec('fresh')), fresh.length + ' new: ' + fresh.slice(0, 6).join(', '));
  // who listens to whom
  const ls = await sec('listens');
  check(label + ': who listens to whom: the pairs who voted for each other, and anyone with no votes', R.listens.pairs.every(p => ls.includes(p.join(' and '))) &&
    (R.listens.pairs.length < 2 || JSON.stringify(await items('listens')) === JSON.stringify(R.listens.pairs.map(p => p.join(' and ')))) &&
    (R.listens.pairs.length || /No two players voted for each other/.test(ls)) && (R.listens.unvoted.length ? R.listens.unvoted.every(n => ls.includes(n)) && /Nobody voted for/.test(ls) : /Everyone received a vote/.test(ls)), ls.slice(19, 160));
  // bold or careful, as a small table
  const bold = await cells('bold');
  check(label + ': bold or careful: each player\'s stakes beside the votes their sentences drew, in a small table', JSON.stringify(bold) === JSON.stringify(R.bold.map(b => [b.name, b.stakes.join(', '), b.votes.join(', '), b.won ? b.won + ' of ' + b.stakes.length : 'none'])), JSON.stringify(bold.slice(0, 2)));
  // the table's eye
  const eye = await sec('eye');
  const places = new Set(R.eye.marked.map(m => m.place)).size;
  check(label + ': the table\'s eye: whose words were marked, and who marked most', !R.eye.hands ? /before the table’s eye/.test(eye)
    : !R.eye.marked.length ? /Nobody marked a word/.test(eye)
      : (await items('eye')).length === places && R.eye.most.every(n => eye.includes(n)) && /marked most/.test(eye) && R.eye.marked.every(m => eye.includes(m.word + ' (' + m.name + ', ' + m.n + (m.n === 1 ? ' mark' : ' marks'))), eye.slice(16, 200));
  // short wins, each player's average as a bar
  const bars = await page.locator(where + ' .look-bars li').evaluateAll(lis => lis.map(li => [li.querySelector('.bn').textContent, li.querySelector('.bv').textContent, li.querySelector('.bar i').getBoundingClientRect().width]));
  const widest = Math.max(...bars.map(b => b[2]), 0);
  check(label + ': short wins: the winning sentences\' length beside the rest, and each player\'s average as a bar, shortest first', K.short.text.includes(n1(R.short.won)) && (!R.short.restN || K.short.text.includes('the rest, ' + n1(R.short.rest) + '.')) &&
    JSON.stringify(bars.map(b => b.slice(0, 2))) === JSON.stringify(R.short.each.map(e => [e.name, n1(e.avg)])) && bars.every((b, k) => !k || b[2] >= bars[k - 1][2] - 1) && (!bars.length || widest > 40), K.short.text);
  // words that travelled
  const tr = await items('travelled');
  check(label + ': words that travelled: from one hand back in a later one', R.travelled.length ? tr.length === R.travelled.length && R.travelled.every((x, k) => tr[k].startsWith(x.word + ' (')) : /No word from one hand came back/.test(await sec('travelled')), tr.slice(0, 2).join(' / ') || 'none');
  // where the trail went
  const stops = await page.locator(where + ' .look-sec[data-reading="trail"] svg.trail-map circle.tm-stop').count();
  const words = await page.locator(where + ' .look-sec[data-reading="trail"] svg.trail-map text').allTextContents();
  check(label + ': where the trail went: the centre words drawn as a path', R.trail.length < 2 ? /The game stayed at/.test(await sec('trail'))
    : stops === R.trail.length && JSON.stringify(words) === JSON.stringify(R.trail.map(x => x.word)) && (await page.locator(where + ' .trail-path').count()) === 1, words.join(' → '));
  await all.click();
  rows = await state();
  check(label + ': Close all folds them again', rows.every(r => !r.open) && (await all.textContent()) === 'Open all');
  if (rows.some(r => r.key === 'votes' && r.box)) await page.locator(where + ' .look-sec[data-reading="votes"] > summary').click();   // as it was found
}

// How the words on the screen now showing can be read (asked for by Matthew, 10 November 2026:
// darker and larger on a phone): the smallest text, and the faintest against the paper. Text in a
// disabled button, or on a seat out of the hand, is dimmed on purpose and counted apart.
async function typeAt(page) {
  return page.evaluate(() => {
    const on = ['lobby', 'invited', 'waiting', 'table', 'end', 'trailview', 'look'].find(id => !document.getElementById(id).hidden);
    if (!on) return null;
    const lin = c => { c /= 255; return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4); };
    const lum = c => 0.2126 * lin(c[0]) + 0.7152 * lin(c[1]) + 0.0722 * lin(c[2]);
    const PAPER = [250, 245, 234];
    let small = { px: 99, text: '' }, faint = { ratio: 99, text: '' }, dim = { ratio: 99, text: '' };
    [document.getElementById(on), document.getElementById('foot'), document.querySelector('footer.foot')].forEach(root => {
      const walk = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
      for (let n = walk.nextNode(); n; n = walk.nextNode()) {
        const text = n.nodeValue.trim(), el = n.parentElement;
        if (!text || !el.getClientRects().length || !el.checkVisibility() || el.closest('.visually-hidden, .inked')) continue;   // folded or hidden: not on the screen
        const cs = getComputedStyle(el), svg = el.closest('svg');
        if (cs.visibility === 'hidden') continue;
        let px = parseFloat(cs.fontSize);
        if (svg && svg.viewBox.baseVal.width) px *= svg.getBoundingClientRect().width / svg.viewBox.baseVal.width;
        if (px < small.px) small = { px: Math.round(px * 10) / 10, text: text.slice(0, 40) };
        const m = ((svg ? cs.fill : cs.color).match(/[\d.]+/g) || []).map(Number);
        let a = m.length > 3 ? m[3] : 1;
        for (let e = el; e && e !== document.body; e = e.parentElement) a *= parseFloat(getComputedStyle(e).opacity);
        const mix = m.slice(0, 3).map((c, k) => c * a + PAPER[k] * (1 - a));
        const l1 = lum(PAPER), l2 = lum(mix), ratio = Math.round(100 * (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05)) / 100;
        if (el.closest('button:disabled, .seat.out')) { if (ratio < dim.ratio) dim = { ratio, text: text.slice(0, 40) }; }
        else if (ratio < faint.ratio) faint = { ratio, text: text.slice(0, 40) };
      }
    });
    const body = getComputedStyle(document.body);
    return { screen: on, body: parseFloat(body.fontSize), weight: body.fontWeight, small, faint, dim };
  }).catch(() => null);
}
const typeOk = t => !!t && t.small.px >= 12 && t.faint.ratio >= 4.5 && t.dim.ratio >= 2.5;
const typeSay = t => t ? `smallest ${t.small.px}px "${t.small.text}", faintest ${t.faint.ratio} "${t.faint.text}", dimmed ${t.dim.ratio} "${t.dim.text}"` : 'no screen';

async function endChecks(label, beach, page, people) {
  const code = (await page.evaluate(() => JSON.parse(localStorage.getItem('zero.table') || '{}').code));
  const t = await tableIn(beach, code);
  await watchWords({ page });
  check(label + ': no screen said ring, holds or line', !seenBanned.length, seenBanned.slice(0, 3).join(' / '));
  check(label + ': four hands played to the end', t.over && t.hands.length === 4, `hands ${t.hands.length}, phase ${t.phase}`);
  const chips = Object.values(t.chips).reduce((a, b) => a + b, 0), seats = Object.keys(t.roster).length;
  const book = Z.chipsBook(t), paid = Object.values(book).reduce((a, b) => a + b.words, 0);
  check(label + ': chips add up: 10 a seat, and the word chips the bank paid (' + (10 * seats + paid) + ')', chips === 10 * seats + paid && paid > 0 &&
    Object.values(book).every(b => b.total === 10 + b.words + b.sentences), 'total ' + chips + ', words ' + paid);
  check(label + ': only words still standing after the flop and the eye were paid, each 1 or 2, and the chosen word 2 more', t.hands.every(h =>
    Object.keys(h.wordChips).every(s => h.ring.includes(Number(s)) && [1, 2].includes(h.wordChips[s])) && (!h.ring.length || h.noRing || h.banked) &&
    (h.pick ? h.chosenChips === 2 : h.chosenChips === 0)));
  const rows = await page.locator('#e-poem .book tbody tr').evaluateAll(trs => trs.map(r => Array.from(r.children).map(c => c.textContent)));
  const want = Object.keys(t.roster).map(Number).sort((a, b) => a - b).map(s => [Z.nameOf(t, s), String(book[s].words), book[s].sentences < 0 ? '−' + (-book[s].sentences) : String(book[s].sentences), String(book[s].total)]);
  check(label + ': the end card lists each player\'s chips from words beside their chips from sentences, a line each, under words, sentences and chips', JSON.stringify(rows) === JSON.stringify(want) &&
    JSON.stringify(await page.locator('#e-poem .book thead th').allTextContents()) === JSON.stringify(['', 'words', 'sentences', 'chips']), JSON.stringify(rows.slice(0, 2)));
  await page.waitForSelector('#e-shows .look-shows');
  const place = await page.evaluate(() => {
    const poem = document.getElementById('e-poem').getBoundingClientRect(), sh = document.getElementById('e-shows').getBoundingClientRect(), btn = document.querySelector('#end .three').getBoundingClientRect();
    const box = document.querySelector('#e-shows .look-shows');
    return { below: sh.top >= poem.bottom - 1, above: sh.bottom <= btn.top + 1, open: !box.hidden && document.querySelector('#e-shows .look-open').getAttribute('aria-expanded') === 'true',
      heads: Array.from(document.querySelectorAll('#e-shows .look-h')).map(h => h.textContent) };
  });
  check(label + ': at the end, What this game shows sits beneath the poem card and above its buttons, open by default', place.below && place.above && place.open, JSON.stringify(place));
  const HEADS = ['Who voted for whom', 'Where two players thought the same', 'Words new to the tree', 'Words the table sent out', 'Chips from words and from sentences',
    'People or computer', 'Who listens to whom', 'Bold or careful', 'The table’s eye', 'Short wins', 'Words that travelled', 'Where the trail went'];
  check(label + ': with every reading of 4.91 in order: the web, the words alike, words new to the tree, sent out, chips, and the seven new ones', JSON.stringify(place.heads) === JSON.stringify(HEADS), place.heads.join(' | '));
  const g = L.tableGame(t);
  const pairs = new Set(g.votes.map(v => v.from + '>' + v.to)).size;
  check(label + ': the web draws every voter\'s choice, and the words alike and sent out match the record', (await page.locator('#e-shows svg.web path.edge').count()) === pairs &&
    (g.same.length ? (await page.locator('#e-shows .look-list.same li').count()) === g.same.length : /Nobody thought the same/.test(await page.textContent('#e-shows'))) &&
    (g.sentOut.length ? (await page.locator('#e-shows .look-list.sent li').count()) === g.sentOut.length : /The table sent no word out/.test(await page.textContent('#e-shows'))), pairs + ' pairs, ' + g.same.length + ' alike, ' + g.sentOut.length + ' sent out');
  const chipRows = await page.locator('#e-shows .look-table.chips tbody tr').evaluateAll(trs => trs.map(r => Array.from(r.children).map(c => c.textContent)));
  check(label + ': and each player\'s chips there, in a small table, match the book', JSON.stringify(chipRows) === JSON.stringify(g.book.map(b => [b.name, String(b.words), b.sentences < 0 ? '−' + (-b.sentences) : String(b.sentences), String(b.total)])), JSON.stringify(chipRows.slice(0, 2)));
  await webChecks(label, page, '#e-shows', g);
  // words new to the tree: the page reads the tree and the room once, then fills it in
  await page.waitForFunction(() => { const s = document.querySelector('#e-shows .look-sec[data-reading="fresh"]'); return s && !/Reading the tree/.test(s.textContent); }, null, { timeout: 30000 });
  const everything = Z.entriesIn(beach.blocks['pool:zero'], '', floorOf(beach.blocks['pool:zero'])).sort(Z.byAddr);
  const full = (await L.games(beach.blocks.zero, everything)).find(x => x.kind === 'table' && Z.codeKey(x.code) === Z.codeKey(t.code));
  await readingChecks(label, page, '#e-shows', full);
  const foot = await footAt(page);
  check(label + ': the end card has the foot: who is at this phone, Back to the lobby, the credits (no Close the game, the game is over)', foot && foot.screen === 'end' && foot.ok, foot && foot.why);
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
  // everyone who sat: the chips list at the foot (a table with word chips), else the line of names
  const sat = (await page.locator('#e-poem .book').count()) ? (await page.locator('#e-poem .book .bn').allTextContents()).join(' · ') : await page.locator('#e-poem .sat').textContent();
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
  p.door = await signUp(p.page, name, true);
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
  check('A: before a name, the lobby offers the door: I’m new · I’ve been here', /I’m new · I’ve been here/.test(await page.textContent('#f-who')));
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
  const label = (h, s) => h.wordChips[s] === 2 ? '+2 · new' : h.wordChips[s] === 1 ? '+1' : '';
  const seen = Object.keys(host.earn || {}).map(Number);
  check('A: at the showdown every card shows its word chips: +1, or +2 · new, and nothing for a word out', seen.length >= 3 &&
    seen.every(n => { const h = t.hands[n - 1]; return Object.keys(t.roster).every(s => host.earn[n][Z.nameOf(t, Number(s))] === label(h, Number(s))); }), JSON.stringify(host.earn && host.earn[seen[0]]));
  const chosenSeen = Object.keys(host.chosen || {}).map(Number);
  check('A: the card of the word chosen as the next zero shows +2 · chosen', chosenSeen.length >= 1 &&
    chosenSeen.every(n => { const h = t.hands[n - 1]; return host.chosen[n][Z.nameOf(t, h.pick)] === '+2 · chosen'; }), JSON.stringify(host.chosen));
  const h1 = t.hands[0], first = JSON.parse(fs.readFileSync(new URL('./zero-fixtures.json', import.meta.url), 'utf8')).zero;
  check('A: hand one paid 2 exactly to the words never played anywhere in the tree as it stood (case and singular ignored)', h1.ring.every(s => h1.wordChips[s] === (Z.playedBefore(first, h1.words[s]) ? 1 : 2)) && h1.ring.some(s => h1.wordChips[s] === 1) && h1.ring.some(s => h1.wordChips[s] === 2), JSON.stringify(h1.ring.map(s => h1.words[s] + ':' + h1.wordChips[s])));
  const stakes = Object.keys(host.stakeMax || {}).map(Number);
  check('A: the turn lets a seat stake its word chips too', stakes.length >= 1 && stakes.every(n => host.stakeMax[n] === Z.stakeable(t.hands[n - 1], 1)), JSON.stringify(host.stakeMax));
  const screens = ['seats', 'the flop', 'the eye', 'the turn', 'the showdown', 'the voice'];
  check('A: every game screen has the foot of 4.24: who is at this phone, Back to the lobby, Close the game, the credits', screens.every(x => host.footAt[x] === true), JSON.stringify(host.footAt));
  check('A: every game screen can be read on a phone: no text under 12px, and quiet text at 4.5 to 1 or better', screens.every(x => typeOk(host.typeAt[x])), screens.filter(x => !typeOk(host.typeAt[x])).map(x => x + ': ' + typeSay(host.typeAt[x])).join(' | '));
  check('A: round the table every word and name sits on one line, fitted to its card', host.saw.fits === true);
  check('A: sentences that cannot be chosen are never faded: they are still there to be read', host.saw.slip === 1, String(host.saw.slip));
  check('A: the turn says once what the labels on the cards mean: +1 for a word still standing, +2 if it is new to the tree', host.saw.chipsNote === true);
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
  await page.click('#f-back');
  check('A: from the end card, Back to the lobby goes straight there: the game is over, so nothing is asked', await page.locator('#lobby').isVisible() && await page.locator('#f-ask').isHidden() &&
    (await page.evaluate(() => localStorage.getItem('zero.table'))) === null);
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

async function tableH(browser, base, beach) {
  console.log('H. Look around: the tree, every game, what each game shows');
  const p = await phone(browser, base);
  const { page } = p;
  const writesBefore = beach.log.length;
  check('H: the lobby has a third door, Look around', await page.locator('#door-look').isVisible() && (await page.textContent('#door-look')) === 'Look around');
  await page.click('#door-look');
  await page.waitForSelector('#look li.tp', { timeout: 20000 });
  await page.waitForSelector('#look .look-card', { timeout: 20000 });
  await shot(page, 'h-look');
  const tree = beach.blocks.zero;
  const voiced = L.voicedPlaces(tree);
  const points = await page.locator('#look li.tp.played').count();
  check('H: every played place in the tree is a crimson point', points === voiced.length, points + ' of ' + voiced.length);
  await page.locator('#look li[data-place="2"] > .tp-row .tp-word').click();
  const said = await page.locator('#look li[data-place="2"] > .tp-detail .tp-sentence').first().textContent();
  check('H: tapping a place shows its winning sentence', said === L.voiceParts(Z.readPlace(tree['2']).voice)[0].line, said);
  const seats = [1, 2, 3, 4, 5, 6, 7, 8, 9].filter(i => tree['2'][i] != null).length;
  const filled = await page.locator('#look li[data-place="2"] > .tp-row .dot:not(.open)').count();
  const hollow = await page.locator('#look li[data-place="2"] > .tp-row .dot.open').count();
  check('H: its seats are small dots, open seats hollow', filled === seats && hollow === 9 - seats, filled + ' filled, ' + hollow + ' hollow');
  await page.locator('#look li[data-place="2"] > .tp-row .dot').nth(1).click();
  check('H: tapping a seat shows its word and who wrote it', (await page.locator('#look li[data-place="2"] > .tp-detail .tp-seat').textContent()) === Z.textOf(tree['2']['2']));
  const all = Z.entriesIn(beach.blocks['pool:zero'], '', floorOf(beach.blocks['pool:zero'])).sort(Z.byAddr);
  const gs = await L.games(tree, all);
  const cards = await page.locator('#look .look-card').count();
  check('H: every game played has its poem card', cards === gs.length && gs.length >= 2, cards + ' cards, ' + gs.length + ' games');
  check('H: newest first', (await page.locator('#look .look-card').first().getAttribute('data-game')) === (gs[0].code || gs[0].hands[0]));
  const folded = await page.evaluate(() => Array.from(document.querySelectorAll('#look .look-card')).map(c => ({ open: c.open, head: c.querySelector('summary').innerText.replace(/\s+/g, ' ').trim(), body: !!c.querySelector('.card-body') })));
  check('H: the newest game stands open; the others are folded to their heads (date, trail, who sat), drawn only when opened', folded[0].open && folded[0].body && folded.slice(1).every(c => !c.open && !c.body) &&
    folded.every((c, k) => c.head.startsWith('a game of Zero') && c.head.includes(gs[k].places.map(x => x.word.charAt(0).toUpperCase() + x.word.slice(1)).filter((w, n, a) => a.indexOf(w) === n).join(' · ')) && c.head.includes(gs[k].people.length + ' at the table')), JSON.stringify(folded).slice(0, 300));
  const tall = await page.evaluate(() => ({ screens: document.documentElement.scrollHeight / window.innerHeight,
    heads: Array.from(document.querySelectorAll('#look .look-card:not([open])')).map(c => c.getBoundingClientRect().height / window.innerHeight) }));
  check('H: folded, each older game takes under a quarter of a screen, so a long list of games is a short scroll', tall.heads.length === cards - 1 && tall.heads.every(h => h > 0 && h < 0.25),
    cards + ' games in ' + tall.screens.toFixed(1) + ' screens; a folded game ' + Math.max(...tall.heads).toFixed(2) + ' of a screen at most');
  const type = await typeAt(page);
  check('H: Look around can be read on a phone: no text under 12px, quiet text at 4.5 to 1 or better', typeOk(type), typeSay(type));
  while (await page.locator('#look .look-card:not([open])').count()) await page.locator('#look .look-card:not([open]) > summary').first().click();
  check('H: a tap on a folded game opens it', (await page.locator('#look .look-card[open] .card-body').count()) === cards);
  const kite = page.locator('#look .look-card[data-game="KITE 1"]');
  check('H: KITE 1 shows its date and who sat down', /a game of Zero · 8 November 2026/.test(await kite.locator('.top').textContent()) && /Phenomemental · Engineer/.test(await kite.locator('.sat').textContent()));
  check('H: What this game shows is open by default on every card, once the card is open', (await page.locator('#look .look-card .look-shows:not([hidden])').count()) === cards &&
    (await page.locator('#look .look-card .look-open[aria-expanded="true"]').count()) === cards);
  const kg = gs.find(g => g.code === 'KITE 1');
  const pairs = new Set(kg.votes.map(v => v.from + '>' + v.to)).size;
  check('H: who voted for whom is drawn as a web, an arrow for each voter and choice', (await kite.locator('svg.web path.edge').count()) === pairs, pairs + ' pairs');
  const sum = t => (/Votes received: ([^.]*)/.exec(t)[1].match(/\d+/g) || []).reduce((a, b) => a + Number(b), 0);
  check('H: and every vote is counted (36 in KITE 1)', sum(await kite.locator('.votes-said').textContent()) === 36);
  const same = await kite.locator('.look-list.same').textContent();
  check('H: where two players thought the same, with both names', /Team · Phenomemental and Child/.test(same) && /moon/.test(same), same.slice(0, 120));
  check('H: "Words only one person saw" is gone, and "Words new to the tree" stands in its place', !(await page.locator('#look .look-list.alone').count()) &&
    !/Words only one person saw/.test(await page.textContent('#look')) && (await page.locator('#look .look-sec[data-reading="fresh"]').count()) === cards);
  // the second version, on every real game (spine:zero 4.91)
  for (const g of gs) {
    const key = g.code || g.hands[0];
    const where = `#look .look-card[data-game="${key}"]`;
    const heads = await page.locator(where + ' .look-h').allTextContents();
    const want = ['Who voted for whom', 'Where two players thought the same', 'Words new to the tree'].concat(g.kind === 'table' ? ['Words the table sent out', 'Chips from words and from sentences'] : [],
      ['People or computer', 'Who listens to whom', 'Bold or careful', 'The table’s eye', 'Short wins', 'Words that travelled', 'Where the trail went']);
    check('H: ' + key + ' shows every reading, in order', JSON.stringify(heads) === JSON.stringify(want), heads.join(' | '));
    if (g.votes.length) await webChecks('H: ' + key, page, where, g);
    await readingChecks('H: ' + key, page, where, g);
  }
  // and what the real games say, read by hand from their records
  const reads = async (key, k) => page.textContent(`#look .look-card[data-game="${key}"] .look-sec[data-reading="${k}"]`);
  check('H: Organisation: Phenomemental, the only person, gave 4 votes to computer players and got none of their 32 back',
    /Phenomemental, the only person at the table, gave 4 votes to computer players\. Computer players gave Phenomemental 0 of their 32 votes\./.test(await reads('KITE 1', 'pc')));
  check('H: Organisation: kitchen travelled, from Organisation back at compost and at worms', /kitchen \(Grandmother and Chef\) from Organisation: back at compost in 5 sentences; at worms as a word and in 5 sentences\./.test(await reads('KITE 1', 'travelled')));
  check('H: Organisation played before the eye; the first game, in chat', /This table played before the table’s eye\./.test(await reads('KITE 1', 'eye')) && /Played in chat, before the table’s eye\./.test(await reads('1', 'eye')));
  check('H: Gift: Torque and infirmary marked out by six seats each; four marked most', /Torque \(Engineer, 6 marks, sent out\)/.test(await reads('RAIN 6', 'eye')) && /infirmary \(Night nurse, 6 marks, sent out\)/.test(await reads('RAIN 6', 'eye')) &&
    /Phenomemental, Sailor, Chef and Poet marked most, 3 words each\./.test(await reads('RAIN 6', 'eye')));
  check('H: Happening: Davido marked most, once a hand; Phenomemental and Davido voted for each other', /Davido marked most: 4 words in 4 hands\./.test(await reads('TIDE 3', 'eye')) && /Phenomemental and Davido/.test(await reads('TIDE 3', 'listens')));
  check('H: Happening: ward, Vigil and Monitor are not new to the tree (the first game had them); Kettle and Oscilloscope are', !(await page.locator('#look .look-card[data-game="TIDE 3"] .look-list.fresh .lw').allTextContents()).some(w => /^(ward|Vigil|Monitor)$/.test(w)) &&
    /Kettle/.test(await reads('TIDE 3', 'fresh')) && /Oscilloscope/.test(await reads('TIDE 3', 'fresh')));
  check('H: the first game\'s stakes, read from its prose: Phenomemental staked 5, 5 and 13, and won 1 of 3', (await page.locator('#look .look-card[data-game="1"] .look-table.bold tbody tr').evaluateAll(trs => trs.map(r => Array.from(r.children).map(c => c.textContent).join(' | ')))).includes('Phenomemental | 5, 5, 13 | 1, 2, 1 | 1 of 3'));
  check('H: the first game\'s trail: Light, candle, vigil, silence', JSON.stringify(await page.locator('#look .look-card[data-game="1"] .trail-map text').allTextContents()) === JSON.stringify(['Light', 'candle', 'vigil', 'silence']));
  await shot(page, 'h-kite');
  const first = page.locator('#look .look-card[data-game="1"]');
  for (const [code, out] of [['RAIN 6', ['Torque', 'infirmary']], ['TIDE 3', ['stubble']]]) {
    const c = page.locator(`#look .look-card[data-game="${code}"]`);
    check('H: ' + code + ' (played on the page tonight) shows its web, its words alike, and the words the table sent out', (await c.locator('svg.web path.edge').count()) > 0 &&
      JSON.stringify(await c.locator('.look-list.sent .lw').allTextContents()) === JSON.stringify(out) && (await c.locator('.look-table.chips tbody tr').count()) === 9, code);
  }
  check('H: the first game, played in chat, keeps no chips or marks, so shows neither', (await first.locator('.look-table.chips').count()) === 0);
  check('H: the first game, played in chat, has its votes read from its record (27)', sum(await first.locator('.votes-said').textContent()) === 27);
  check('H: and its matches: birthday, watch, snow', /birthday · Child and Phenomemental/.test(await first.locator('.look-list.same').textContent()) && /snow · Grandmother and Farmer/.test(await first.locator('.look-list.same').textContent()));
  const rows = L.across(tree, gs);
  check('H: Across all games shows once a first word has two games, and not before', (await page.locator('#look .look-across').count() > 0) === (rows.length > 0), rows.length + ' rows');
  await watchWords(p);
  check('H: no screen said ring, holds or line', !seenBanned.length, seenBanned.slice(0, 2).join(' / '));
  check('H: Look around wrote nothing to the beach', beach.log.length === writesBefore);
  check('H: the look has its own address (?look=1), and Back returns to the lobby', /[?&]look=1/.test(page.url()));
  await page.click('#f-back');
  check('H: …back to the lobby', await page.locator('#lobby').isVisible() && !/look=1/.test(page.url()));
  check('H: no page errors', !p.errors.length, p.errors.join(' / '));
  await p.ctx.close();
}

async function tableJ(browser, base, beach) {
  console.log('J. The foot of every screen: who is at this phone, Back to the lobby, Close the game, the credits');
  const foot = async (p, label, screen, extra) => {
    const f = await recordFoot(p);
    check('J: ' + label, f && f.screen === screen && f.ok && (!extra || extra(f)), f ? f.screen + ' ' + f.why : 'no screen');
    return f;
  };
  const p = await phone(browser, base);
  const { page } = p;
  await foot(p, 'the lobby, before a name: the door (I’m new · I’ve been here), no way back from where you are, the credits', 'lobby', f => f.text === 'I’m new · I’ve been here');
  await page.click('#door-host');
  await foot(p, 'hosting: Back to the lobby appears', 'lobby, hosting');
  await page.click('#f-back');
  check('J: …and takes you back to the three doors', await page.locator('#lobby-form').isHidden() && await page.locator('#f-back-part').isHidden() && await page.locator('#door-host').isVisible());
  await page.click('#door-join');
  await foot(p, 'joining: the same foot', 'lobby, joining');
  await page.click('#f-back');
  // a name, a table, the waiting room
  const { code, link } = await hostOpens(p, 'Jude', 'A new first word', 'Harbourside');
  await foot(p, 'the waiting room: You are Jude on this phone · not you?, Back to the lobby, the credits', 'waiting', f => f.text === 'You are Jude on this phone · not you?');
  check('J: before anyone sits down, one plain line: Everything you write here is public and kept.', /Everything you write here is public and kept\./.test(await page.textContent('#waiting')));
  // the invite link, before a name
  const v = await phone(browser, link);
  await v.page.locator('.door-sheet').waitFor();
  await v.page.click('.door-shut');
  await v.page.waitForSelector('#invited:not([hidden])');
  await foot(v, 'arriving by the invite link: the door in the foot, Back to the lobby, the credits', 'invited', f => f.text === 'I’m new · I’ve been here');
  check('J: and the invited screen says too: Everything you write here is public and kept.', /Everything you write here is public and kept\./.test(await v.page.textContent('#invited')));
  await v.ctx.close();
  // a guest in the waiting room: asked first, Stay keeps the seat, Leave frees it
  const kai = await arrives(browser, link, 'Kai');
  await kai.page.waitForSelector('#waiting:not([hidden])');
  await kai.page.click('#f-back');
  check('J: in the waiting room, Back to the lobby asks first: Leave this game? Your seat will be freed.', await kai.page.locator('#f-ask').isVisible() && (await kai.page.textContent('#f-ask-say')) === 'Leave this game? Your seat will be freed.' && await kai.page.locator('#f-back').isHidden());
  await kai.page.click('#f-ask-no');
  check('J: Stay keeps the seat', await kai.page.locator('#waiting').isVisible() && await kai.page.locator('#f-ask').isHidden() && await kai.page.locator('#f-back').isVisible());
  await page.waitForFunction(() => document.querySelectorAll('#w-roster li').length === 2, null, { timeout: 15000 });
  await kai.page.click('#f-back');
  await kai.page.click('#f-ask-yes');
  await kai.page.waitForSelector('#lobby:not([hidden])');
  check('J: Leave the game frees the seat and says so', /You left the table. Your seat is free./.test(await kai.page.textContent('#lobby-note')));
  await page.waitForFunction(() => document.querySelectorAll('#w-roster li').length === 1, null, { timeout: 15000 });
  check('J: and the host sees the seat free again', (await atTable(page)).length === 1);
  await foot(kai, 'back in the lobby, still named: You are Kai on this phone · not you?', 'lobby', f => f.text === 'You are Kai on this phone · not you?');
  await kai.ctx.close();
  // the game: the foot on every screen it passes, then asked before leaving
  await addKey(page, 'sk-ant-test');
  await page.click('#deal-go');
  await page.waitForSelector('#table:not([hidden])');
  await play(p, h => ['quay', 'bollard', 'gull', 'rope'][h - 1], { until: async () => visible(page.locator('#m-line')) });
  const seen = Object.keys(p.footAt).filter(k => ['seats', 'the flop', 'the eye', 'the turn'].includes(k));
  check('J: every game screen on the way (seats, the flop, the eye, the turn) has the foot, Close the game beneath Back to the lobby', seen.length >= 3 && seen.every(k => p.footAt[k] === true), JSON.stringify(p.footAt));
  await page.click('#f-back');
  check('J: in a game, Back to the lobby asks first; the host is told the game ends for everyone', await page.locator('#f-ask').isVisible() && (await page.textContent('#f-ask-say')) === 'Leave this game? You host it, so it ends for everyone at the table.');
  await page.click('#t-close-go');
  check('J: Close the game and the question never stand open together', await page.locator('#t-close-ask').isVisible() && await page.locator('#f-ask').isHidden() && await page.locator('#f-back').isVisible());
  await page.click('#t-close-no');
  await page.locator('#f-notyou').click();
  check('J: not you?, in a game, asks the same first', await page.locator('#f-ask').isVisible() && await page.locator('#table').isVisible());
  await page.click('#f-ask-no');
  check('J: Stay: still at the table, still Jude', await page.locator('#table').isVisible() && /You are Jude on this phone/.test(await page.textContent('#f-who')));
  await page.locator('#f-notyou').click();
  await page.click('#f-ask-yes');
  await page.locator('.door-sheet').waitFor({ timeout: 15000 });
  const all = Z.entriesIn(beach.blocks['pool:zero'], '', floorOf(beach.blocks['pool:zero'])).filter(e => Z.codeKey(e.code) === Z.codeKey(code));
  check('J: not you? then Leave the game: the table is closed (the host was leaving), Jude is signed out, and the door opens', all.some(e => e.kind === 'close') &&
    await page.locator('#lobby').isVisible() && /You closed the game/.test(await page.textContent('#lobby-note')) && (await page.evaluate(() => localStorage.getItem('zero.table'))) === null);
  await page.click('.door-shut');
  await page.locator('.door-sheet').waitFor({ state: 'detached' });
  await foot(p, 'signed out, the foot offers the door again', 'lobby', f => f.text === 'I’m new · I’ve been here');
  // Look around and a trail read on its own
  await signUp(page, 'Juno');
  await page.click('#door-look');
  await page.waitForSelector('#look .look-card', { timeout: 30000 });
  await foot(p, 'Look around: You are Juno on this phone · not you?, Back to the lobby, the credits', 'look', f => f.text === 'You are Juno on this phone · not you?');
  await page.locator('#f-notyou').click();
  await page.locator('.door-sheet').waitFor();
  check('J: not you? outside a game asks nothing: it signs out, and the door opens over Look around', await page.locator('#look').isVisible() && await page.locator('#f-ask').isHidden());
  await page.click('.door-shut');
  await page.locator('.door-sheet').waitFor({ state: 'detached' });
  check('J: …and the foot offers the door', (await page.textContent('#f-who')) === 'I’m new · I’ve been here');
  await page.click('#f-back');
  check('J: Back to the lobby from Look around', await page.locator('#lobby').isVisible() && await page.locator('#f-ask').isHidden());
  await page.goto(base + '&trail=1583');
  await page.waitForSelector('#trailview:not([hidden]) .poem .place');
  await foot(p, 'a trail read on its own: the same foot', 'trailview');
  await page.click('#f-back');
  check('J: Back to the lobby from a trail', await page.locator('#lobby').isVisible() && !/trail=/.test(page.url()));
  const typed = Object.keys(p.typeAt || {});
  check('J: every screen walked can be read on a phone: no text under 12px, quiet text at 4.5 to 1 or better', typed.length >= 8 && typed.every(k => typeOk(p.typeAt[k])), typed.filter(k => !typeOk(p.typeAt[k])).map(k => k + ': ' + typeSay(p.typeAt[k])).join(' | ') || typed.join(', '));
  const body = p.typeAt[typed[0]];
  check('J: on a phone the words are set larger and a weight heavier than before (20px or more, medium; they were 18.4px, regular)', body && body.body >= 20 && body.weight === '500', body && body.body + 'px, ' + body.weight);
  check('J: no page errors', !p.errors.length, p.errors.join(' / '));
  await p.ctx.close();
}

async function tableI(browser, base, beach) {
  console.log('I. Close the game: a guest frees their seat; the host ends the table for everyone');
  const host = await phone(browser, base);
  const { code, link } = await hostOpens(host, 'Ines', 'A new first word', 'Lantern');
  await addKey(host.page, 'sk-ant-test');
  // before the deal: Joe arrives and leaves, and his seat is free for the next
  const joe = await arrives(browser, link, 'Joe');
  await joe.page.click('#w-leave');
  await joe.page.waitForSelector('#lobby:not([hidden])');
  check('I: a guest leaving the waiting room is told plainly, where it can be seen', await joe.page.locator('#lobby-note').isVisible() && /You left the table. Your seat is free./.test(await joe.page.textContent('#lobby-note')));
  await joe.ctx.close();
  const kim = await arrives(browser, link, 'Kim');
  const lou = await arrives(browser, link, 'Lou');
  await host.page.waitForFunction(() => document.querySelectorAll('#w-roster li').length === 3, null, { timeout: 15000 });
  check('I: Joe\'s seat was freed: Kim took seat two', (await atTable(host.page)).join('|') === 'Ineshost|Kim|Lou', (await atTable(host.page)).join('|'));
  await host.page.click('#deal-go');
  await kim.page.waitForSelector('#table:not([hidden])');
  // Kim closes the game at the first move, before writing her word
  await kim.page.waitForSelector('#t-close-go');
  check('I: "Close the game" can be found at once', await kim.page.locator('#t-close-go').isVisible());
  await kim.page.click('#t-close-go');
  check('I: it asks first, in plain words, for a guest', /This frees your seat. The table plays on without you./.test(await kim.page.textContent('#t-close-say')) && (await kim.page.textContent('#t-close-yes')) === 'Leave the table');
  await kim.page.click('#t-close-yes');
  await kim.page.waitForSelector('#lobby:not([hidden])');
  check('I: Kim is back in the lobby, told her seat is free', /You left the table. Your seat is free./.test(await kim.page.textContent('#lobby-note')));
  // the table goes on without her: the host and Lou play on, nobody waits on Kim
  const louDone = play(lou, h => ['wick', 'oil', 'glass', 'hook'][h - 1], { voteIndex: 0, until: async () => lou.page.locator('#lobby').isVisible() });
  await play(host, h => ['beacon', 'shade', 'flame', 'bulb'][h - 1], { voteIndex: 0, until: async (hand) => hand >= 2 });
  let t = await tableIn(beach, code);
  check('I: the table plays on without Kim, never waiting on her', t.gone[2] === true && t.hands[0].steps.word.skipSeal[2] === true && t.hands.length >= 2 &&
    !Z.entriesIn(beach.blocks['pool:zero'], '', floorOf(beach.blocks['pool:zero'])).some(e => Z.codeKey(e.code) === Z.codeKey(code) && e.kind === 'go'), JSON.stringify(t.gone));
  check('I: her seat shows she has left', /Kim · left/.test(await host.page.textContent('#t-ring')));
  // now the host closes the game for everyone
  await host.page.click('#t-close-go');
  check('I: it asks the host first, in plain words', /This ends the game for everyone at the table./.test(await host.page.textContent('#t-close-say')));
  await host.page.click('#t-close-yes');
  await host.page.waitForSelector('#lobby:not([hidden])');
  check('I: the host is back in the lobby: "You closed the game."', /You closed the game. Everyone at the table has been told./.test(await host.page.textContent('#lobby-note')));
  await louDone;
  check('I: Lou is told plainly: "The host closed the game."', await lou.page.locator('#lobby').isVisible() && /The host closed the game./.test(await lou.page.textContent('#lobby-note')));
  t = await tableIn(beach, code);
  check('I: the table is closed, before its end', t.closed && !t.over);
  // Kim opens the link again, as herself: her seat went when she left
  const back = await phone(browser, link);
  await back.page.getByRole('button', { name: 'I’ve been here before' }).click();
  await back.page.fill('#door-name2', 'Kim');
  await back.page.fill('#door-key', kim.door.words.join('-'));
  await back.page.getByRole('button', { name: 'Come in' }).click();
  await back.page.waitForFunction(() => document.querySelector('#i-error').textContent.length > 0, null, { timeout: 15000 });
  check('I: coming back by the link after leaving, Kim is told her seat has gone', /You left this game, so your seat has gone./.test(await back.page.textContent('#i-error')), await back.page.textContent('#i-error'));
  check('I: no page errors', ![host, kim, lou].some(p => p.errors.length), [host, kim, lou].map(p => p.errors.join(' / ')).join(' | '));
  await Promise.all([host, kim, lou, back].map(p => p.ctx.close()));
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
  if (which.includes('I')) await tableI(browser, base, beach);
  if (which.includes('J')) await tableJ(browser, base, beach);
  if (which.includes('H')) await tableH(browser, base, beach);
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
