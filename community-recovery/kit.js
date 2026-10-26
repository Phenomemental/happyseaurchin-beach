/* kit.js — everything the pages share.
 *
 * Copied with config.js, words.js, kit.css and the pages that use them (constitution, standards,
 * agreement, join, made) from the pscale-commons Community Recovery pages, under the MIT licence
 * in kit-LICENSE.txt, when David handed them to Matthew (15 October 2026). They read and write the
 * same blocks as before; the look is Community Recovery's own (brand.css).
 *
 * The pages read and write one place on the beach (config.js). A person is a name and three words:
 * the name is a handle, the words are its key, and the first thing made is their card,
 * passport:<name>, latched with the words. The beach then lets only those words latch any block
 * named for that person, so one name and one set of words hold everywhere.
 *
 * Nothing here calls any service but the beach. What a phone remembers is kept in its own storage,
 * under this place's address, and is never sent anywhere else.
 */
(function () {
  'use strict';
  var CFG = window.CR || {};
  var LOCAL = /^(localhost|127\.0\.0\.1|\[::1\])$/.test(location.hostname);

  function sget(store, k) { try { return store.getItem(k); } catch (e) { return null; } }
  function sset(store, k, v) { try { if (v == null) store.removeItem(k); else store.setItem(k, v); } catch (e) {} }

  // The beach. Only a page served from this computer itself may be pointed at another one
  // (a builder's test); a published page reads config.js and nothing else, so no link can
  // send a person's words anywhere but here.
  var BEACH = CFG.beach;
  if (LOCAL) {
    var asked = new URLSearchParams(location.search).get('beach') || sget(localStorage, 'cr:test-beach');
    if (asked) { BEACH = asked; sset(localStorage, 'cr:test-beach', asked); }
  }
  BEACH = String(BEACH || '').replace(/\/+$/, '');
  function wireOf(beach) { return String(beach).replace(/\/+$/, '') + '/.well-known/pscale-beach'; }
  var WIRE = wireOf(BEACH);

  // ---- reading and writing the beach -------------------------------------------------------
  function getJSON(url) {
    return fetch(url, { cache: 'no-store', headers: { Accept: 'application/json' } }).then(function (r) {
      if (r.status === 404) return null;
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    });
  }
  function read(block, beach) { return getJSON((beach ? wireOf(beach) : WIRE) + '?block=' + encodeURIComponent(block)); }
  function index(beach) { return getJSON(beach ? wireOf(beach) : WIRE); }
  function names(idx) {
    var b = idx && idx.blocks;
    if (Array.isArray(b)) return b.map(function (n) { return typeof n === 'string' ? n : (n && n.name) || ''; });
    if (b && typeof b === 'object') return Object.keys(b);
    return [];
  }
  function send(method, body) {
    return fetch(WIRE + '?block=' + encodeURIComponent(body.block), {
      method: method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    }).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (j) {
        return { ok: r.ok && j.ok !== false && !j.error, status: r.status, data: j || {} };
      });
    }, function () { return { ok: false, status: 0, data: {} }; });
  }
  // The beach reads a block, changes it and writes it back, so two writes to one block sent at
  // once can lose one of them. Writes to the same block therefore go one after another.
  var queue = {};
  function post(body) {
    var k = body.block, run = function () { return send('POST', body); };
    var p = (queue[k] || Promise.resolve()).then(run, run);
    queue[k] = p;
    return p;
  }
  function wipe(block, secret) { return send('DELETE', { block: block, confirm: true, secret: secret }); }

  // ---- blocks ---------------------------------------------------------------------------------
  function text(n) { while (n && typeof n === 'object') n = n._; return typeof n === 'string' ? n : ''; }
  function digits(n) {
    if (!n || typeof n !== 'object') return [];
    return Object.keys(n).filter(function (k) { return /^[1-9]$/.test(k); }).sort();
  }
  function at(block, addr) {
    var n = block;
    for (var i = 0; i < addr.length; i++) { if (!n || typeof n !== 'object') return null; n = n[addr.charAt(i)]; }
    return n == null ? null : n;
  }
  // every frame and notebook here is born with one line at its root, so an address has one digit
  // before its point: part 1, section 3, theme 2, clause 1 is 1.321
  function dotted(addr) { return addr.length <= 1 ? addr : addr.charAt(0) + '.' + addr.slice(1); }
  function split(line) {           // "Title — what it holds" → the two halves
    var s = String(line || ''), i = s.indexOf(' — ');
    return i < 0 ? { title: s.trim(), gloss: '' } : { title: s.slice(0, i).trim(), gloss: s.slice(i + 3).trim() };
  }
  function cap(s) { return s ? s.charAt(0).toUpperCase() + s.slice(1) : s; }

  // ---- small things for building a page -----------------------------------------------------------
  function el(tag, cls, t) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (t != null) e.textContent = t;
    return e;
  }
  function when(iso) {
    var d = new Date(iso);
    if (isNaN(d)) return '';
    return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' });
  }
  function today() { return new Date().toISOString().slice(0, 10); }

  // ---- a name and three words -------------------------------------------------------------------
  var ME_KEY = 'cr:me:' + BEACH;
  function me() {
    var raw = sget(localStorage, ME_KEY) || sget(sessionStorage, ME_KEY);
    try { var m = raw && JSON.parse(raw); return m && m.name && m.words ? m : null; } catch (e) { return null; }
  }
  function keepMe(m, remember) {
    sset(localStorage, ME_KEY, remember ? JSON.stringify(m) : null);
    sset(sessionStorage, ME_KEY, remember ? null : JSON.stringify(m));
  }
  function forgetMe() { sset(localStorage, ME_KEY, null); sset(sessionStorage, ME_KEY, null); }

  var WORDS = window.CR_WORDS || [];
  // "phone" on a phone, "computer" anywhere else: the words a person reads should be true
  var DEVICE = /Mobi|Android|iPhone|iPad|iPod/.test(navigator.userAgent) ? 'phone' : 'computer';
  function threeWords() {
    if (WORDS.length < 500) throw new Error('the word list did not load');
    var out = [], buf = new Uint32Array(1), n = WORDS.length, limit = Math.floor(4294967296 / n) * n;
    while (out.length < 3) {
      crypto.getRandomValues(buf);
      if (buf[0] >= limit) continue;                 // no leaning toward any word
      var w = WORDS[buf[0] % n];
      if (out.indexOf(w) < 0) out.push(w);
    }
    return out;
  }
  // how far apart two words are, counting a slip of one letter as one step
  function apart(a, b) {
    var m = a.length, n = b.length, d = [], i, j;
    if (Math.abs(m - n) > 2) return 3;
    for (i = 0; i <= m; i++) { d[i] = [i]; }
    for (j = 1; j <= n; j++) d[0][j] = j;
    for (i = 1; i <= m; i++) for (j = 1; j <= n; j++) {
      var c = a.charAt(i - 1) === b.charAt(j - 1) ? 0 : 1;
      d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + c);
      if (i > 1 && j > 1 && a.charAt(i - 1) === b.charAt(j - 2) && a.charAt(i - 2) === b.charAt(j - 1)) d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
    }
    return d[m][n];
  }
  // a typed word, put right if it is one slip away from exactly one word of the list
  function nearest(w) {
    if (WORDS.indexOf(w) >= 0) return w;
    var near = WORDS.filter(function (x) { return apart(w, x) <= (w.length > 4 ? 2 : 1); });
    return near.length === 1 ? near[0] : null;
  }
  // whatever a person types, a phone's capital letter or a comma included, becomes the same three words
  function tidyWords(typed) {
    return String(typed || '').toLowerCase().split(/[^a-z]+/).filter(Boolean);
  }
  function tidyName(typed) {
    var s = String(typed || '');
    try { s = s.normalize('NFC'); } catch (e) {}
    s = s.replace(/\s+/g, ' ').trim().replace(/ /g, '-');
    // letters of any alphabet where the browser knows them; built at run time so an older browser still loads the page
    try { s = s.replace(new RegExp("[^\\p{L}\\p{N}'\\-]", 'gu'), ''); } catch (e) { s = s.replace(/[^A-Za-z0-9'\-]/g, ''); }
    return s.replace(/^[-']+|[-']+$/g, '').slice(0, 24);
  }
  function people(idx) {
    return names(idx).filter(function (n) { return n.indexOf('passport:') === 0 && n.length > 9; }).map(function (n) { return n.slice(9); });
  }
  function sameName(list, name) {
    var low = name.toLowerCase();
    for (var i = 0; i < list.length; i++) if (list[i].toLowerCase() === low) return list[i];
    return null;
  }
  function birth(family, name) {
    return { _: 'MIRROR — ' + name + '’s own notebook for ' + family + ', at the frame’s own addresses. Only ' + name + ' writes here.' };
  }
  // A person's notebooks are made, latched, the moment they are named, so nobody else can ever
  // write under their name. A name made a moment ago has written nothing: whatever already stands
  // under it is not theirs and is replaced. A returning name's notebooks are only latched again.
  function ensureNotebooks(name, words, fresh) {
    return index().then(function (idx) {
      var have = names(idx);
      return Promise.all((CFG.families || []).map(function (f) {
        var block = f + ':' + name;
        if (have.indexOf(block) < 0) return post({ block: block, content: birth(f, name), new_lock: words });
        return fresh ? post({ block: block, content: birth(f, name), new_lock: words, confirm: true })
                     : post({ block: block, secret: words, new_lock: words });
      }));
    });
  }
  // Before a phone's first write on a page, make sure this person's notebooks stand and are
  // latched: a write to a notebook that is missing would otherwise bring it back unlatched.
  var readyFor = {};
  function ready(m) { return readyFor[m.name] || (readyFor[m.name] = ensureNotebooks(m.name, m.words, false)); }
  function foundName(name, words, line) {
    return index().then(function (idx) {
      if (sameName(people(idx), name)) return { ok: false, why: 'taken' };
      return post({ block: 'passport:' + name, content: { _: line || name }, new_lock: words }).then(function (r) {
        if (!r.ok) return { ok: false, why: r.data.code === 'confirm_required' ? 'taken' : 'beach' };
        return ensureNotebooks(name, words, true).then(function () { return { ok: true, name: name, words: words }; });
      });
    }, function () { return { ok: false, why: 'beach' }; });
  }
  function comeBack(typedName, typedWords) {
    var toks = tidyWords(typedWords), fixed = toks.map(nearest);
    if (toks.length !== 3) return Promise.resolve({ ok: false, why: 'three' });
    if (fixed.indexOf(null) >= 0) return Promise.resolve({ ok: false, why: 'word', which: toks[fixed.indexOf(null)] });
    var words = fixed.join('-');
    return index().then(function (idx) {
      var name = sameName(people(idx), tidyName(typedName));
      if (!name) return { ok: false, why: 'noname' };
      // proves the words and changes nothing
      return post({ block: 'passport:' + name, secret: words, new_lock: words }).then(function (r) {
        if (!r.ok) return { ok: false, why: r.status === 403 ? 'words' : 'beach' };
        return ensureNotebooks(name, words, false).then(function () { return { ok: true, name: name, words: words }; });
      });
    }, function () { return { ok: false, why: 'beach' }; });
  }
  // after someone who keeps the store has cleared a person's old words, with the person there
  function newWords(typedName, words) {
    return index().then(function (idx) {
      var name = sameName(people(idx), tidyName(typedName));
      if (!name) return { ok: false, why: 'noname' };
      return post({ block: 'passport:' + name, new_lock: words }).then(function (r) {
        if (!r.ok) return { ok: false, why: r.status === 403 ? 'stand' : 'beach' };
        return ensureNotebooks(name, words, false).then(function () { return { ok: true, name: name, words: words }; });
      });
    }, function () { return { ok: false, why: 'beach' }; });
  }

  // Leaving: a person removes their own notebooks and their card. Only their words can do it.
  function leave() {
    var m = me();
    if (!m) return Promise.resolve(false);
    return index().then(function (idx) {
      var mine = names(idx).filter(function (n) { return n !== 'passport:' + m.name && n.slice(n.lastIndexOf(':') + 1) === m.name && !/^(sed|grain|pool):/.test(n); });
      return mine.reduce(function (p, b) { return p.then(function () { return wipe(b, m.words); }); }, Promise.resolve())
        .then(function () { return wipe('passport:' + m.name, m.words); })
        .then(function (r) { if (r.ok) forgetMe(); return r.ok; });
    }, function () { return false; });
  }

  // ---- the card: a picture of the name and its words, to keep -----------------------------------
  function cardPicture(name, words) {
    var c = document.createElement('canvas'); c.width = 900; c.height = 560;
    // Community Recovery's own colours and face, from the block 'brand' (as brand.css)
    var g = c.getContext('2d'), font = '"Nunito Sans", system-ui, -apple-system, "Segoe UI", sans-serif';
    function fit(t, weight, px) { g.font = weight + px + 'px ' + font; while (px > 12 && g.measureText(t).width > 760) { px -= 2; g.font = weight + px + 'px ' + font; } }
    g.fillStyle = '#F6F2FA'; g.fillRect(0, 0, 900, 560);
    g.fillStyle = '#FDFBFF'; g.fillRect(30, 30, 840, 500);
    g.fillStyle = '#7B3FBF'; g.fillRect(30, 30, 840, 8); g.beginPath(); g.arc(98, 104, 26, 0, Math.PI * 2); g.fill();
    g.fillStyle = '#1E1828'; g.font = '700 40px ' + font; g.fillText(CFG.place || '', 140, 118);
    g.font = '700 92px ' + font; g.fillText(name, 70, 262);
    g.fillStyle = '#7B3FBF';
    var line = words.split('-').join('  ');
    fit(line, '700 ', 62);
    g.fillText(line, 70, 372);
    g.fillStyle = '#746882'; g.font = '34px ' + font;
    g.fillText('The only key to this name. Keep it safe.', 70, 448);
    fit(CFG.home || '', '', 30); g.fillText(CFG.home || '', 70, 494);
    return c.toDataURL('image/png');
  }

  // ---- the sheet that asks, once -----------------------------------------------------------------
  var sheetOpen = null, onNamed = [];
  function nameSheet() {
    if (sheetOpen) return sheetOpen;
    sheetOpen = new Promise(function (resolve, reject) {
      var veil = el('div', 'veil'), sheet = el('div', 'sheet');
      sheet.setAttribute('role', 'dialog'); sheet.setAttribute('aria-modal', 'true'); sheet.setAttribute('aria-label', 'Your name');
      var body = el('div');
      var shut = el('button', 'sheet-shut', '×'); shut.type = 'button'; shut.setAttribute('aria-label', 'Close');
      sheet.appendChild(shut); sheet.appendChild(body);
      document.body.appendChild(veil); document.body.appendChild(sheet);
      document.body.classList.add('sheet-up');
      var before = document.activeElement;
      function done(m) {
        veil.remove(); sheet.remove(); document.body.classList.remove('sheet-up'); sheetOpen = null;
        if (before && before.focus && document.contains(before)) try { before.focus({ preventScroll: true }); } catch (e) {}
        if (m) { readyFor[m.name] = Promise.resolve(); onNamed.forEach(function (fn) { try { fn(m); } catch (e) {} }); resolve(m); } else reject(new Error('closed'));
      }
      shut.addEventListener('click', function () { done(null); });
      veil.addEventListener('click', function () { done(null); });
      sheet.addEventListener('keydown', function (ev) { if (ev.key === 'Escape') done(null); });

      function field(labelText, id, placeholder) {
        var l = el('label', null, labelText); l.htmlFor = id;
        var i = el('input'); i.id = id; i.type = 'text'; i.autocomplete = 'off'; i.spellcheck = false;
        i.setAttribute('autocapitalize', 'none'); i.setAttribute('autocorrect', 'off');
        if (placeholder) i.placeholder = placeholder;
        return [l, i];
      }
      function tick(labelText, id, on) {
        var row = el('div', 'tick'), i = el('input'); i.type = 'checkbox'; i.id = id; i.checked = !!on;
        var l = el('label', null, labelText); l.htmlFor = id;
        row.appendChild(i); row.appendChild(l);
        return [row, i];
      }
      function go(labelText) { var b = el('button', 'go', labelText); b.type = 'submit'; return b; }
      function link(labelText, fn) {
        var b = el('button', 'plain', labelText); b.type = 'button'; b.addEventListener('click', fn); return b;
      }
      var SAY = {
        taken: 'Someone here already has that name. Pick another, or add a letter.',
        beach: 'That didn’t reach the beach. Please try again in a moment.',
        noname: 'There’s nobody here by that name yet. Check the spelling, or choose “I’m new here”.',
        words: 'Those aren’t the words for that name. Check each one and try again.',
        three: 'Your words are three words. Please type all three.',
        stand: 'Your old words still stand. Ask someone at a session to clear them first.'
      };

      function chooseView() {
        body.textContent = '';
        var words = threeWords();
        var form = el('form'); form.noValidate = true;
        var f = field('What shall we call you?', 'cr-name'); f[1].setAttribute('autocapitalize', 'words'); f[1].maxLength = 24;
        var small1 = el('p', 'small', 'A first name or a nickname is fine. It shows beside what you write, and everyone can read it.');
        var lab = el('p', 'label', 'Your three words');
        var chips = el('div', 'words');
        function paint() { chips.textContent = ''; words.forEach(function (w) { chips.appendChild(el('span', null, w)); }); chips.appendChild(other); }
        var other = link('↻ other words', function () { words = threeWords(); paint(); });
        paint();
        var small2 = el('p', 'small', 'These are the only key to your name. Nobody can look them up for you, so keep them somewhere safe.');
        var adult = tick('I am 18 or over', 'cr-adult', false);
        var rem = tick('Remember me on this ' + DEVICE, 'cr-rem', true);
        var status = el('p', 'form-status'); status.setAttribute('role', 'status');
        var b = go('Carry on');
        [f[0], f[1], small1, lab, chips, small2, adult[0], rem[0], b, status].forEach(function (x) { form.appendChild(x); });
        var alt = el('p', 'alt'); alt.appendChild(link('I already have a name', comeBackView));
        body.appendChild(form); body.appendChild(alt);
        form.addEventListener('submit', function (ev) {
          ev.preventDefault();
          var name = tidyName(f[1].value);
          if (name.length < 2) { status.textContent = 'Please choose a name of two letters or more.'; f[1].focus(); return; }
          if (!adult[1].checked) { status.textContent = 'This place is for people aged 18 or over. Please tick the box if that’s you.'; return; }
          b.disabled = true; status.textContent = 'One moment…';
          foundName(name, words.join('-')).then(function (r) {
            b.disabled = false;
            if (!r.ok) { status.textContent = SAY[r.why] || SAY.beach; return; }
            var m = { name: r.name, words: r.words };
            keepMe(m, rem[1].checked);
            cardView(m, 'You’re ' + m.name + '.');
          });
        });
        setTimeout(function () { f[1].focus(); }, 30);
      }

      function comeBackView() {
        body.textContent = '';
        var form = el('form'); form.noValidate = true;
        var f = field('Your name', 'cr-name2'); f[1].setAttribute('autocapitalize', 'words');
        var w = field('Your three words', 'cr-words', 'three words');
        var rem = tick('Remember me on this ' + DEVICE, 'cr-rem2', true);
        var status = el('p', 'form-status'); status.setAttribute('role', 'status');
        var b = go('Carry on');
        [f[0], f[1], w[0], w[1], rem[0], b, status].forEach(function (x) { form.appendChild(x); });
        var alt = el('p', 'alt');
        alt.appendChild(link('I’m new here', chooseView));
        alt.appendChild(document.createTextNode(' · '));
        alt.appendChild(link('I’ve lost my words', lostView));
        body.appendChild(form); body.appendChild(alt);
        form.addEventListener('submit', function (ev) {
          ev.preventDefault();
          b.disabled = true; status.textContent = 'One moment…';
          comeBack(f[1].value, w[1].value).then(function (r) {
            b.disabled = false;
            if (!r.ok) { status.textContent = r.why === 'word' ? 'I don’t know the word “' + r.which + '”. Check its spelling.' : (SAY[r.why] || SAY.beach); return; }
            var m = { name: r.name, words: r.words };
            keepMe(m, rem[1].checked);
            done(m);
          });
        });
        setTimeout(function () { f[1].focus(); }, 30);
      }

      function lostView() {
        body.textContent = '';
        body.appendChild(el('p', 'label', 'Lost your words?'));
        body.appendChild(el('p', 'small', 'Tell someone at a session. They can clear your old words while you are there. Then come back here and take new ones. Everything you wrote stays.'));
        var words = threeWords();
        var form = el('form'); form.noValidate = true;
        var f = field('Your name', 'cr-name3'); f[1].setAttribute('autocapitalize', 'words');
        var status = el('p', 'form-status'); status.setAttribute('role', 'status');
        var b = go('My old words were cleared. Give me new ones');
        [f[0], f[1], b, status].forEach(function (x) { form.appendChild(x); });
        var alt = el('p', 'alt'); alt.appendChild(link('Back', comeBackView));
        body.appendChild(form); body.appendChild(alt);
        form.addEventListener('submit', function (ev) {
          ev.preventDefault();
          b.disabled = true; status.textContent = 'One moment…';
          newWords(f[1].value, words.join('-')).then(function (r) {
            b.disabled = false;
            if (!r.ok) { status.textContent = SAY[r.why] || SAY.beach; return; }
            var m = { name: r.name, words: r.words };
            keepMe(m, true);
            cardView(m, 'New words for ' + m.name + '.');
          });
        });
      }

      function cardView(m, heading) {
        body.textContent = '';
        body.appendChild(el('p', 'label', heading));
        var img = el('img', 'card'); img.alt = m.name + ': ' + m.words.split('-').join(', ');
        var src = cardPicture(m.name, m.words); img.src = src;
        body.appendChild(img);
        body.appendChild(el('p', 'small', 'This is your card. Keep it: save the picture, or write the three words down.'));
        var a = el('a', 'go', 'Save the picture'); a.href = src; a.download = 'my-card-' + m.name + '.png';
        var on = el('button', 'go quiet', 'Carry on'); on.type = 'button';
        on.addEventListener('click', function () { done(m); });
        body.appendChild(a); body.appendChild(on);
        shut.onclick = function () { done(m); }; veil.onclick = function () { done(m); };
      }
      chooseView();
    });
    return sheetOpen;
  }
  // the person at this phone: known already, or asked now
  function name() { var m = me(); return m ? Promise.resolve(m) : nameSheet(); }

  // the line at the foot of a page that says who this phone thinks you are
  function whoLine(mount) {
    function draw() {
      mount.textContent = '';
      var m = me();
      if (m) {
        mount.appendChild(document.createTextNode('You are '));
        mount.appendChild(el('strong', null, m.name));
        mount.appendChild(document.createTextNode(' on this ' + DEVICE + ' · '));
        var b = el('button', 'plain', 'not you?'); b.type = 'button';
        b.addEventListener('click', function () { forgetMe(); location.reload(); });
        mount.appendChild(b);
      } else {
        var c = el('button', 'plain', 'I already have a name'); c.type = 'button';
        c.addEventListener('click', function () { nameSheet().then(function () { location.reload(); }, function () {}); });
        mount.appendChild(c);
      }
    }
    draw();
    onNamed.push(draw);
    return draw;
  }

  // ---- listening and speaking ----------------------------------------------------------------------
  function listenButton(getText) {
    if (!('speechSynthesis' in window)) return null;
    var b = el('button', 'quiet', '🔊 Listen'); b.type = 'button';
    b.addEventListener('click', function () {
      if (speechSynthesis.speaking) { speechSynthesis.cancel(); return; }
      var u = new SpeechSynthesisUtterance(getText()); u.lang = 'en-GB'; u.rate = 0.95;
      speechSynthesis.speak(u);
    });
    return b;
  }
  // A microphone beside a box, as on Matthew's pages: the words land in the box to be read and
  // changed before anything is saved. Where a browser has none, no button appears.
  // How it avoids the echo on phones (ways:dictation 2, the same fix as beach.js addMic): a phone's
  // engine keeps revising the phrase it hears and hands back each revision, often marked final,
  // repeating the words before it. So on every result the words are worked out again from all of
  // the results, from 0: the finals in order, revisions collapsed, and only the last unfinished
  // entry as the words still forming. The box is set to that, never added to.
  var NOT_WORD;
  try { NOT_WORD = new RegExp('[^\\p{L}\\p{N}\\s]', 'gu'); } catch (e) { NOT_WORD = /[^a-z0-9À-ɏ\s]/g; }
  function spoken(s) { return String(s).toLowerCase().replace(NOT_WORD, ' ').split(/\s+/).filter(Boolean); }
  // true when b says what a said, or begins with it: b is a revision of a
  function revises(a, b) {
    var x = spoken(a), y = spoken(b);
    if (x.length > y.length) return false;
    for (var i = 0; i < x.length; i++) if (x[i] !== y[i]) return false;
    return true;
  }
  function collapse(list) {
    var out = [];
    list.forEach(function (p) {
      p = String(p).trim();
      if (!spoken(p).length) return;
      if (out.length && revises(out[out.length - 1], p)) out[out.length - 1] = p; else out.push(p);
    });
    return out;
  }
  // bare words become sentences (ways:dictation 3); a phrase the engine punctuated stays as it came
  var ENDS = /[.!?…]["”’)]*\s*$/;
  function sentence(p, starts, ends) {
    if (/[.,!?;:…]/.test(p)) return p;
    p = p.replace(/(^|\s)i(?=\s|'|’|$)/g, '$1I');
    if (starts) p = p.charAt(0).toUpperCase() + p.slice(1);
    return ends ? p + '.' : p;
  }
  var listening = null;     // the one microphone listening now, as its stop
  function speakButton(box, note) {
    var Rec = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Rec) return null;
    var b = el('button', 'quiet', '🎙 Speak'); b.type = 'button';
    var rec = null, wanted = false, frozen = false, hooked = false;
    var base = '', phrases = [], live = '', began = 0, heard = false, quiet = 0;
    // the box's words before speaking, then the spoken phrases as sentences, then the words still forming
    function compose(forming) {
      var t = base, starts = !t.trim() || ENDS.test(t);
      function put(w) { t += (t && !/\s$/.test(t) ? ' ' : '') + w; }
      phrases.forEach(function (p) { var w = sentence(p, starts, true); put(w); starts = ENDS.test(w); });
      if (live) put(sentence(live, starts, false) + (forming ? '…' : ''));
      return t;
    }
    function show(t) { box.value = t.slice(0, box.maxLength > 0 ? box.maxLength : 100000); }
    function stop() { wanted = false; if (rec) try { rec.stop(); } catch (e) {} }
    function halt() { wanted = false; frozen = true; if (rec) try { rec.abort(); } catch (e) {} }
    function done(msg) {
      rec = null; if (listening === stop) listening = null;
      b.textContent = '🎙 Speak';
      if (!frozen) show(compose(false).replace(/\s+$/, ''));
      if (msg) note.textContent = msg;
      else if (/^Listening/.test(note.textContent)) note.textContent = 'Read it through and change anything before you save.';
    }
    function session() {
      var r = new Rec(); rec = r;
      r.lang = 'en-GB'; r.continuous = true; r.interimResults = true;
      began = Date.now(); heard = false; phrases = []; live = '';
      var fault = '';
      r.onresult = function (ev) {
        if (frozen || r !== rec) return;
        heard = true; quiet = 0;
        var finals = [], forming = '';
        for (var i = 0; i < ev.results.length; i++) {
          if (ev.results[i].isFinal) finals.push(ev.results[i][0].transcript); else forming = ev.results[i][0].transcript;
        }
        phrases = collapse(finals); live = forming.trim();
        // words still forming that repeat the last finished phrase are its revision
        if (live && phrases.length && revises(phrases[phrases.length - 1], live)) phrases.pop();
        show(compose(true));
      };
      r.onerror = function (ev) {
        if (ev.error === 'no-speech' || ev.error === 'aborted') return;
        wanted = false;
        fault = ev.error === 'not-allowed' || ev.error === 'service-not-allowed'
          ? 'The microphone wasn’t allowed. You can type instead.' : 'Speaking didn’t work just now. You can type instead.';
      };
      r.onend = function () {
        if (r !== rec) return;
        // silence ends a session, sooner on a phone: while the person still wants to speak, start
        // again with what was said as the new starting words; a session that dies at once is a fault
        if (wanted && !frozen) {
          if (Date.now() - began < 1000) return done('Speaking didn’t work just now. You can type instead.');
          if (!heard && ++quiet >= 2) return done('I stopped listening after a long silence. Press Speak to go on.');
          base = compose(false); show(base);
          try { session(); return; } catch (e) { return done('Speaking didn’t work just now. You can type instead.'); }
        }
        done(fault);
      };
      r.start();
    }
    b.addEventListener('click', function () {
      if (rec) return stop();
      if (listening) listening();      // one microphone at a time
      // saving stops the microphone first, so what is saved has no ellipsis
      if (!hooked && box.form) {
        hooked = true;
        box.form.addEventListener('submit', function () { if (rec) { show(compose(false).replace(/\s+$/, '')); halt(); } }, true);
      }
      listening = stop; wanted = true; frozen = false; quiet = 0;
      base = box.value && !/\s$/.test(box.value) ? box.value + ' ' : box.value;
      try {
        session(); b.textContent = '■ Stop';
        note.textContent = 'Listening… press Stop when you’re done. Your ' + DEVICE + ' may send your voice to its maker to turn it into text. Nothing is saved until you press Save.';
      } catch (e) { rec = null; listening = null; note.textContent = 'Speaking didn’t work just now. You can type instead.'; }
    });
    // typing takes over: the words heard so far stay, and listening stops
    box.addEventListener('beforeinput', function (ev) { if (rec && ev.isTrusted) { box.value = compose(false); halt(); } });
    box.addEventListener('blur', function (ev) { if (rec && ev.relatedTarget !== b) stop(); });
    return b;
  }

  // ---- the circle beside a clause: what the answers say together ------------------------------------
  var SVG = 'http://www.w3.org/2000/svg';
  function svg(tag, attrs) { var e = document.createElementNS(SVG, tag); Object.keys(attrs).forEach(function (k) { e.setAttribute(k, attrs[k]); }); return e; }
  var COLOR = ['var(--first)', 'var(--second)', 'var(--third)'];
  // seen: which of the three words stand among the answers, e.g. [0, 1]
  function circle(seen, size) {
    size = size || 26;
    var s = svg('svg', { viewBox: '0 0 24 24', width: size, height: size, 'class': 'dot', 'aria-hidden': 'true' });
    var left = 'M12 2 A10 10 0 0 0 12 22 Z', right = 'M12 2 A10 10 0 0 1 12 22 Z';
    function ring(c, dash) { var a = { cx: 12, cy: 12, r: 10, fill: 'none', stroke: c, 'stroke-width': 2 }; if (dash) a['stroke-dasharray'] = '3.2 2.6'; return svg('circle', a); }
    if (!seen.length) s.appendChild(ring('var(--third)', true));
    else if (seen.length > 1) {
      s.appendChild(svg('path', { d: left, fill: COLOR[seen[0]] }));
      s.appendChild(svg('path', { d: right, fill: COLOR[seen[seen.length - 1]], 'fill-opacity': seen[seen.length - 1] === 2 ? 0.5 : 1 }));
      s.appendChild(svg('path', { d: 'M12 1.5 V22.5', stroke: 'var(--card)', 'stroke-width': 1.6 }));
    } else if (seen[0] === 0) s.appendChild(svg('circle', { cx: 12, cy: 12, r: 11, fill: COLOR[0] }));
    else if (seen[0] === 1) { s.appendChild(svg('path', { d: left, fill: COLOR[1] })); s.appendChild(ring(COLOR[1])); }
    else s.appendChild(ring(COLOR[2]));
    return s;
  }

  // ---- a family drawn as a page: the frame, everyone's answers, the kept minutes ------------------
  function family(opt) {
    var fam = opt.name, mount = opt.mount, status = opt.status;
    var also = (CFG.also || {})[fam];
    var state = { spine: null, law: null, fold: null, words: [], voices: {}, open: {} };
    var keeper = sget(localStorage, 'cr:keeper:' + BEACH);

    function parseWords(law) {
      var n = law && law['2'];
      // the three words are the lines beneath the law's 2, each "Word — what it means"
      return digits(n).map(function (k) { var sp = split(text(n[k])); return { word: sp.title, means: sp.gloss }; })
        .filter(function (w) { return w.means && w.word.length <= 16; }).slice(0, 3);
    }
    // an answer begins with one of the three words; the rest is the person's own
    function parse(s) {
      var t = String(s || '').trim(), low = t.toLowerCase();
      for (var i = 0; i < state.words.length; i++) {
        var w = state.words[i].word.toLowerCase();
        if (low.indexOf(w) === 0 && !/[a-z]/.test(low.charAt(w.length) || '')) {
          return { token: i, rest: t.slice(w.length).replace(/^[\s.:,;—-]+/, '').trim() };
        }
      }
      return { token: -1, rest: t };
    }
    function gather(mirrors, from) {
      mirrors.forEach(function (m) {
        (function walk(n, addr) {
          digits(n).forEach(function (k) {
            var c = n[k], a = addr + k, t = typeof c === 'string' ? c : (c && typeof c._ === 'string' ? c._ : '');
            if (t && t.trim() && at(state.spine, a) != null && typeof at(state.spine, a) === 'string') {
              var p = parse(t);
              (state.voices[a] = state.voices[a] || []).push({ who: m.who, token: p.token, rest: p.rest, from: from });
            }
            if (c && typeof c === 'object') walk(c, a);
          });
        })(m.block, '');
      });
    }
    function loadMirrors(beach, familyName, from, have) {
      return (have ? Promise.resolve(have) : index(beach)).then(function (idx) {
        var pre = familyName + ':';
        var list = names(idx).filter(function (n) { return n.indexOf(pre) === 0 && n.length > pre.length; }).sort();
        return Promise.all(list.map(function (n) {
          return read(n, beach).then(function (b) { return { who: n.slice(pre.length), block: b }; }, function () { return null; });
        }));
      }).then(function (ms) { gather(ms.filter(function (m) { return m && m.block && typeof m.block === 'object'; }), from); }, function () {});
    }

    function load() {
      return Promise.all([read('spine:' + fam), read('function:' + fam), index()]).then(function (r) {
        state.spine = r[0]; state.law = r[1]; state.words = parseWords(r[1]); state.voices = {};
        if (!state.spine) throw new Error('no frame');
        var jobs = [loadMirrors(null, fam, null, r[2])];
        if (also) jobs.push(loadMirrors(also.beach, also.family, 'wider'));
        // the kept minutes are born of use: read them only once they stand
        jobs.push(names(r[2]).indexOf(fam) >= 0 ? read(fam).then(function (b) { state.fold = b; }, function () {}) : Promise.resolve());
        return Promise.all(jobs);
      });
    }

    function seenAt(addr) {
      var seen = {};
      (state.voices[addr] || []).forEach(function (v) { if (v.token >= 0) seen[v.token] = true; });
      return Object.keys(seen).map(Number).sort();
    }
    function leavesUnder(n, addr, out) {
      digits(n).forEach(function (k) { var c = n[k]; if (typeof c === 'string') out.push(addr + k); else leavesUnder(c, addr + k, out); });
      return out;
    }
    function deepest(n, d) { var m = d; digits(n).forEach(function (k) { var c = n[k]; m = Math.max(m, typeof c === 'string' ? d + 1 : deepest(c, d + 1)); }); return m; }

    // one clause, or one standard: its own words, three answers, who said what, the kept minute
    function leaf(addr) {
      var full = String(at(state.spine, addr));
      var nl = full.indexOf('\n'), head = nl < 0 ? '' : full.slice(0, nl), bodyText = nl < 0 ? full : full.slice(nl + 1);
      var box = el('article', 'clause'); box.id = 'a' + addr;
      function draw(openVoices, openBox) {
        box.textContent = '';
        var voices = state.voices[addr] || [], m = me();
        var mine = m ? voices.filter(function (v) { return !v.from && v.who === m.name; })[0] : null;
        var top = el('div', 'clause-head');
        top.appendChild(circle(seenAt(addr)));
        var words = el('div');
        if (head) words.appendChild(el('strong', 'clause-title', head));
        bodyText.split('\n').forEach(function (p) { if (p.trim()) words.appendChild(el('p', null, p)); });
        top.appendChild(words); box.appendChild(top);

        var row = el('div', 'btns');
        state.words.forEach(function (w, i) {
          var b = el('button', 'ans' + (mine && mine.token === i ? ' on' : ''), w.word); b.type = 'button';
          b.title = w.means; b.setAttribute('aria-pressed', mine && mine.token === i ? 'true' : 'false');
          b.addEventListener('click', function () { answer(i, mine && mine.token === i ? mine.rest : (mine ? mine.rest : '')); });
          row.appendChild(b);
        });
        var hear = listenButton(function () { return (head ? head + ' ' : '') + bodyText; });
        if (hear) row.appendChild(hear);
        box.appendChild(row);

        if (mine && openBox) {
          var form = el('form', 'saymore'); form.noValidate = true;
          var ta = el('textarea'); ta.maxLength = 1500; ta.value = mine.rest; ta.setAttribute('aria-label', 'Say more, if you like');
          ta.placeholder = opt.prompt || 'Say more, if you like';
          var note = el('p', 'mic-note'); note.setAttribute('role', 'status');
          var row2 = el('div', 'btns');
          var mic = speakButton(ta, note); if (mic) row2.appendChild(mic);
          var save = el('button', 'ans on', 'Save'); save.type = 'submit'; row2.appendChild(save);
          var gone = el('button', 'plain', 'Remove my answer'); gone.type = 'button'; row2.appendChild(gone);
          form.appendChild(ta); form.appendChild(row2); form.appendChild(note);
          form.addEventListener('submit', function (ev) { ev.preventDefault(); save.disabled = true; write(state.words[mine.token].word + '.' + (ta.value.trim() ? ' ' + ta.value.trim() : ''), note); });
          gone.addEventListener('click', function () { write('', note); });
          box.appendChild(form);
        }

        if (voices.length) {
          var d = el('details', 'voices'); if (openVoices) d.open = true;
          d.appendChild(el('summary', null, voices.length + (voices.length === 1 ? ' person' : ' people')));
          voices.forEach(function (v) {
            var line = el('div', 'voice');
            line.appendChild(el('span', 'who', v.who));
            if (v.token >= 0) line.appendChild(el('span', 'tag t' + v.token, state.words[v.token].word));
            if (v.rest) line.appendChild(said(v.rest));
            d.appendChild(line);
          });
          box.appendChild(d);
        }
        var minute = state.fold ? at(state.fold, addr) : null;
        if (minute != null && text(minute).trim()) box.appendChild(kept(minute));
        if (keeper) box.appendChild(keepBox());
      }
      // a long answer opens on its first part; a reading of the standards shows its three headings
      function said(t) {
        var span = el('span', 'said');
        function put(s) {
          span.textContent = '';
          s.split(/\s+(?=(?:In place:|Not there yet:|Would show it:))/).forEach(function (bit, i) {
            var m = bit.match(/^(In place:|Not there yet:|Would show it:)/);
            var p = el('span', i ? 'bit' : null);
            if (m) { p.appendChild(el('b', null, m[1])); p.appendChild(document.createTextNode(bit.slice(m[1].length))); } else p.textContent = bit;
            span.appendChild(p);
          });
        }
        if (t.length > 320) {
          put(t.slice(0, t.lastIndexOf(' ', 260)) + '…');
          var more = el('button', 'plain', 'Read all'); more.type = 'button';
          more.addEventListener('click', function () { put(t); });
          span.appendChild(document.createTextNode(' ')); span.appendChild(more);
        } else put(t);
        return span;
      }
      function kept(node) {
        var k = el('div', 'kept');
        function one(s) {
          var m = String(s).match(/^(\d{4}-\d{2}-\d{2})\s*[·—-]\s*([\s\S]*)$/), p = el('p');
          if (m) { p.appendChild(el('span', 'kept-when', 'Kept on ' + when(m[1]) + ' · ')); p.appendChild(document.createTextNode(m[2])); } else p.textContent = s;
          return p;
        }
        k.appendChild(one(text(node)));
        var older = digits(node).map(function (d) { return text(node[d]); }).filter(Boolean);
        if (older.length) {
          var d = el('details'); d.appendChild(el('summary', null, 'Earlier'));
          older.forEach(function (s) { d.appendChild(one(s)); });
          k.appendChild(d);
        }
        return k;
      }
      function keepBox() {
        var d = el('details', 'keepbox'); d.appendChild(el('summary', null, 'Keep a minute'));
        var form = el('form'); form.noValidate = true;
        var ta = el('textarea'); ta.maxLength = 1500; ta.setAttribute('aria-label', 'The minute');
        ta.placeholder = 'What the answers say together';
        var b = el('button', 'ans on', 'Keep'); b.type = 'submit';
        var note = el('p', 'form-status'); note.setAttribute('role', 'status');
        form.appendChild(ta); form.appendChild(b); form.appendChild(note); d.appendChild(form);
        form.addEventListener('submit', function (ev) {
          ev.preventDefault();
          var t = ta.value.trim(); if (!t) return;
          b.disabled = true; note.textContent = 'Keeping…';
          keepMinute(addr, t).then(function (ok) {
            b.disabled = false;
            if (!ok) { note.textContent = 'That wasn’t kept. Check the keepers’ words on the “how this was made” page.'; return; }
            draw(false, false);
          });
        });
        return d;
      }
      function answer(i, rest) {
        name().then(function (m) {
          var t = state.words[i].word + '.' + (rest ? ' ' + rest : '');
          setMine(m, i, rest); draw(true, true);
          var r = box.getBoundingClientRect();
          if (r.top < 0 || r.top > innerHeight - 120) box.scrollIntoView({ block: 'start' });
          save(m, t).then(function (ok) { if (!ok) { failed(); } });
        }, function () {});
      }
      function write(t, note) {
        var m = me(); if (!m) return;
        if (t) { var p = parse(t); setMine(m, p.token, p.rest); } else clearMine(m);
        save(m, t).then(function (ok) {
          if (!ok) { note.textContent = 'That didn’t reach the beach. Please try again in a moment.'; var b = box.querySelector('button[type=submit]'); if (b) b.disabled = false; return; }
          draw(true, false);
          refreshUp();
        });
      }
      function failed() {
        var p = el('p', 'form-status', 'That didn’t reach the beach. Please try again in a moment.');
        box.appendChild(p);
      }
      function setMine(m, token, rest) {
        clearMine(m);
        (state.voices[addr] = state.voices[addr] || []).push({ who: m.name, token: token, rest: rest || '', from: null });
      }
      function clearMine(m) {
        state.voices[addr] = (state.voices[addr] || []).filter(function (v) { return v.from || v.who !== m.name; });
      }
      function save(m, t) {
        return ready(m).then(function () {
          return post({ block: fam + ':' + m.name, spindle: dotted(addr), content: t, secret: m.words });
        }).then(function (r) { refreshUp(); return r.ok; });
      }
      function refreshUp() { if (opt.onChange) opt.onChange(); var c = box.querySelector('.dot'); if (c) c.replaceWith(circle(seenAt(addr))); }
      draw(false, false);
      return box;
    }

    // the kept minute: the newest first, the older beneath it
    function keepMinute(addr, t) {
      var prev = state.fold ? at(state.fold, addr) : null;
      var node = { _: today() + ' · ' + t }, older = [];
      if (prev != null && text(prev).trim()) { older.push(text(prev)); digits(prev).forEach(function (d) { older.push(text(prev[d])); }); }
      older.slice(0, 8).forEach(function (s, i) { node[String(i + 1)] = s; });
      var founded = state.fold ? Promise.resolve({ ok: true }) : post({
        block: fam, new_lock: keeper,
        content: { _: 'WHAT PEOPLE ARE SAYING — the shared minutes of ' + fam + ', kept at the frame’s own addresses by its keepers: the newest minute first at each address, the older beneath it.' }
      });
      return founded.then(function (f) {
        if (!f.ok && f.data.code !== 'confirm_required') return false;
        return post({ block: fam, spindle: dotted(addr), content: node, secret: keeper }).then(function (r) {
          if (!r.ok) return false;
          return read(fam).then(function (b) { state.fold = b; return true; }, function () { return true; });
        });
      });
    }

    function rung(n, addr, depth, foldDepth) {
      var sp = split(text(n));
      var kids = el('div', 'kids');
      function fill() {
        if (kids.firstChild) return;
        digits(n).forEach(function (k) {
          var c = n[k];
          kids.appendChild(typeof c === 'string' ? leaf(addr + k) : rung(c, addr + k, depth + 1, foldDepth));
        });
      }
      if (depth > foldDepth) {                       // a theme: a quiet heading over its clauses
        var t = el('div', 'theme');
        t.appendChild(el('p', 'theme-title', sp.title));
        fill(); t.appendChild(kids);
        return t;
      }
      var sec = el('section', depth === 1 ? 'part' : 'sect'); sec.id = 'a' + addr;
      var b = el('button', 'fold'); b.type = 'button'; b.setAttribute('aria-expanded', 'false');
      var label = el('span', 'fold-words');
      label.appendChild(el('span', 'fold-title', sp.title));
      if (sp.gloss) label.appendChild(el('span', 'fold-gloss', cap(sp.gloss)));
      b.appendChild(label); b.appendChild(el('span', 'chev', '⌄'));
      function set(open) {
        if (open) fill();
        sec.classList.toggle('open', open); b.setAttribute('aria-expanded', open ? 'true' : 'false'); kids.hidden = !open;
        state.open[addr] = open;
      }
      b.addEventListener('click', function () { set(!sec.classList.contains('open')); });
      sec.appendChild(b); sec.appendChild(kids); kids.hidden = true;
      sec._set = set;
      return sec;
    }

    function render() {
      mount.textContent = '';
      // two folding levels where the frame is deep (parts, then sections), one where it is shallow
      var foldDepth = deepest(state.spine, 0) >= 4 ? 2 : 1;
      digits(state.spine).forEach(function (k) {
        var c = state.spine[k];
        mount.appendChild(typeof c === 'string' ? leaf(k) : rung(c, k, 1, foldDepth));
      });
      // arriving on a link to one clause, open the way to it
      var want = (location.hash || '').replace(/^#a/, '');
      if (/^[1-9]+$/.test(want)) {
        for (var i = 1; i <= want.length; i++) { var s = document.getElementById('a' + want.slice(0, i)); if (s && s._set) s._set(true); }
        var target = document.getElementById('a' + want); if (target) target.scrollIntoView();
      }
    }

    return load().then(function () {
      if (status) status.textContent = '';
      render();
      return state;
    }, function () {
      if (status) status.textContent = 'This can’t be reached just now. Please try again in a little while.';
      throw new Error('unreachable');
    });
  }

  // ---- the keepers' words, kept on a keeper's own phone ------------------------------------------
  function keeperWords() { return sget(localStorage, 'cr:keeper:' + BEACH); }
  function setKeeper(words) {
    if (!words) { sset(localStorage, 'cr:keeper:' + BEACH, null); return Promise.resolve(true); }
    // proves the words against the frame and changes nothing
    return post({ block: 'spine:constitution', secret: words, new_lock: words }).then(function (r) {
      if (r.ok) sset(localStorage, 'cr:keeper:' + BEACH, words);
      return r.ok;
    });
  }

  // ---- the live dot in the one footer (brand 7.7) ----------------------------------------------
  // The same classes and words as Beach.dot in beach.js, so the dot reads alike on every page.
  function dot(ok) {
    var d = document.getElementById('live-dot'), w = document.getElementById('live-word');
    if (!d || !w) return;
    d.classList.toggle('on', !!ok);
    d.classList.toggle('off', !ok);
    w.textContent = ok ? 'Reading the beach now' : 'The beach can\u2019t be reached just now';
  }

  window.Kit = {
    beach: BEACH, wire: WIRE, config: CFG,
    read: read, index: index, names: names, post: post, wipe: wipe,
    text: text, digits: digits, at: at, dotted: dotted, split: split, cap: cap, el: el, when: when, today: today,
    me: me, name: name, nameSheet: nameSheet, forgetMe: forgetMe, whoLine: whoLine, people: people, ready: ready, leave: leave,
    listenButton: listenButton, speakButton: speakButton, circle: circle, family: family,
    keeperWords: keeperWords, setKeeper: setKeeper, dot: dot,
    // for the checks only
    _t: { tidyWords: tidyWords, tidyName: tidyName, nearest: nearest, threeWords: threeWords, apart: apart },
    // for Community Recovery's app (app.html), which draws its own screens round the same steps
    // the name sheet takes; added beside the rest, nothing above changes
    app: { foundName: foundName, comeBack: comeBack, newWords: newWords, keepMe: keepMe, cardPicture: cardPicture,
           threeWords: threeWords, tidyName: tidyName, device: DEVICE }
  };
})();
