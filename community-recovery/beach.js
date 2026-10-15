/* Shared helpers for reading from and writing to David's beach
   (beach.happyseaurchin.com) from these static pages. */
(function () {
  var BEACH = 'https://beach.happyseaurchin.com/.well-known/pscale-beach';
  var REFRESH_MS = 30000;
  var clock = null;

  // Read a block, or only part of it: opts.spindle is an address such as
  // "19" (position 1.9), which returns that node and what's beneath it;
  // add opts.pscale to read a single node's own text (see point below).
  function get(block, opts) {
    var q = '?block=' + encodeURIComponent(block);
    if (opts && opts.spindle != null) q += '&spindle=' + encodeURIComponent(opts.spindle);
    if (opts && opts.pscale != null) q += '&pscale=' + encodeURIComponent(opts.pscale);
    return fetch(BEACH + q, { cache: 'no-store' })
      .then(function (r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        readClock(r.headers.get('X-Pscale-Now'));
        return r.json();
      });
  }

  // One node's own text, without anything beneath it. The pscale counts
  // down from the block's floor: in a floor-1 block, 0 is a first-level
  // position ("1"), -1 a second-level one ("1.8"), and so on.
  function point(block, spindle, pscale) {
    return get(block, { spindle: spindle, pscale: pscale }).then(function (r) {
      return r && typeof r.content === 'string' ? r.content : '';
    });
  }

  // The beach's front door: every block's name ("blocks"), and when each
  // last changed ("touched").
  function index() {
    return fetch(BEACH, { cache: 'no-store' }).then(function (r) {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    });
  }

  // Every beach response carries the shared clock as
  // "ISO | ten-digit sundial address | voicing", e.g.
  // "2026-09-27T10:04:23Z | 2026334647 | Sunday 27 September 2026, morning (beat 7)".
  function readClock(header) {
    var parts = (header || '').split('|').map(function (x) { return x.trim(); });
    if (parts.length === 3 && /^\d{10}$/.test(parts[1])) {
      clock = { iso: parts[0], address: parts[1], voicing: parts[2] };
    }
  }

  // A position's own text: a string leaf, or the "_" of a node.
  function text(node) {
    if (typeof node === 'string') return node;
    if (node && typeof node._ === 'string') return node._;
    return '';
  }

  // A pool entry is {_: text, 1: who, 2: address, 3: time}. As a pool
  // grows the beach nests older entries under "_" and newer ones one level
  // deeper, so walk "_" first, then 1-9, to read oldest to newest.
  function isEntry(n) {
    return n && typeof n === 'object' && typeof n._ === 'string' && typeof n['3'] === 'string';
  }

  // Position 1 of an entry is who left it. A few older entries keep words
  // of their own there instead, so only a name-length string counts.
  function name(v) {
    return typeof v === 'string' && v.length <= 60 ? v : '';
  }

  // A reply sits beneath the entry it answers, at the entry's next free
  // position: {_: text, 1: who, 3: time}, alongside the entry's own
  // who/where/when strings.
  function replies(e) {
    var out = [];
    for (var i = 1; i <= 9; i++) {
      var r = e[String(i)];
      if (r && typeof r === 'object' && typeof r._ === 'string' && r._.trim()) {
        out.push({
          who: name(r['1']),
          when: typeof r['3'] === 'string' ? r['3'] : '',
          text: r._
        });
      }
    }
    return out;
  }

  function entries(node, out) {
    out = out || [];
    if (!node || typeof node !== 'object') return out;
    if (node._ && typeof node._ === 'object') entries(node._, out);
    for (var i = 1; i <= 9; i++) {
      var e = node[String(i)];
      if (isEntry(e)) {
        if (e._.trim()) out.push({ who: name(e['1']), where: typeof e['2'] === 'string' ? e['2'] : '', when: e['3'], text: e._, replies: replies(e) });
      } else if (typeof e === 'string') {
        if (e.trim()) out.push({ who: '', when: '', text: e });
      } else {
        entries(e, out);
      }
    }
    return out;
  }

  function when(iso) {
    var d = new Date(iso);
    if (isNaN(d)) return '';
    return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  function renderThread(el, pool) {
    var list = entries(pool);
    el.textContent = '';
    if (!list.length) {
      var empty = document.createElement('p');
      empty.className = 'thread-empty';
      empty.textContent = 'No notes yet.';
      el.appendChild(empty);
      return;
    }
    list.forEach(function (c) {
      var div = comment(c, 'comment');
      if (c.replies && c.replies.length) {
        var rs = document.createElement('div');
        rs.className = 'replies';
        c.replies.forEach(function (r) { rs.appendChild(comment(r, 'comment reply')); });
        div.appendChild(rs);
      }
      el.appendChild(div);
    });
  }

  // One note: who and when, then its words. A very long note shows its
  // opening, with the rest a tap away.
  var LONG = 700;
  function comment(c, cls) {
    var div = document.createElement('div');
    div.className = cls;
    var meta = document.createElement('p');
    meta.className = 'comment-meta';
    if (c.who) {
      var b = document.createElement('strong');
      b.textContent = c.who;
      meta.appendChild(b);
    }
    var w = when(c.when);
    if (w) meta.appendChild(document.createTextNode((c.who ? ' · ' : '') + w));
    if (meta.childNodes.length) div.appendChild(meta);
    var t = document.createElement('p');
    t.className = 'comment-text';
    var full = c.text.trim();
    if (full.length > LONG) {
      var cut = full.lastIndexOf(' ', 500);
      t.textContent = full.slice(0, cut > 300 ? cut : 500) + '\u2026';
      var more = document.createElement('button');
      more.type = 'button';
      more.className = 'comment-more';
      more.textContent = 'Read all';
      more.addEventListener('click', function () {
        t.textContent = full;
        more.remove();
      });
      div.appendChild(t);
      div.appendChild(more);
    } else {
      t.textContent = full;
      div.appendChild(t);
    }
    return div;
  }

  // Append a note to a pool, in the same shape an assistant would write it.
  function postNote(pool, who, note) {
    return fetch(BEACH + '?block=' + encodeURIComponent(pool), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        append: true,
        content: { _: note, 1: who, 2: '', 3: new Date().toISOString() }
      })
    }).then(function (r) {
      return r.json().then(function (j) {
        if (!r.ok || !j.ok) throw new Error(j.error || ('HTTP ' + r.status));
      });
    });
  }

  // A microphone beside a text box, so people can speak their words in.
  // It uses the browser's own speech recognition (Chrome, Edge, Safari);
  // where there is none, no button appears. The words land in the box to
  // be read and changed before anything is sent; it never sends.
  //
  // How it avoids the echo on phones (ways:dictation 2): a phone's engine
  // keeps revising the phrase it hears and hands back each revision, often
  // marked final, repeating the words before it. So on every result the
  // words are worked out again from everything the engine holds, from
  // result 0: the finals in order, revisions collapsed, and only the last
  // unfinished entry as the words still forming. The box is set to that,
  // in place of what the last result wrote, never added to.
  var SVGNS = 'http://www.w3.org/2000/svg';
  function micIcon() {
    var svg = document.createElementNS(SVGNS, 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('width', '16');
    svg.setAttribute('height', '16');
    svg.setAttribute('aria-hidden', 'true');
    [['rect', { x: 9, y: 3, width: 6, height: 11, rx: 3 }],
     ['path', { d: 'M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21', 'stroke-linecap': 'round' }]].forEach(function (part) {
      var n = document.createElementNS(SVGNS, part[0]);
      Object.keys(part[1]).forEach(function (k) { n.setAttribute(k, part[1][k]); });
      n.setAttribute('fill', 'none');
      n.setAttribute('stroke', 'currentColor');
      n.setAttribute('stroke-width', '1.8');
      svg.appendChild(n);
    });
    return svg;
  }

  // A phrase's words alone, for comparing revisions: lower case, letters
  // and digits, punctuation stripped. (Built at run time, so a browser
  // without Unicode classes falls back rather than failing to load.)
  var NOT_WORD;
  try { NOT_WORD = new RegExp('[^\\p{L}\\p{N}\\s]', 'gu'); } catch (e) { NOT_WORD = /[^a-z0-9\u00c0-\u024f\s]/g; }
  function words(s) {
    return String(s).toLowerCase().replace(NOT_WORD, ' ').split(/\s+/).filter(Boolean);
  }
  // True when b says what a said, or begins with it: b is a revision of a.
  function revises(a, b) {
    var x = words(a), y = words(b);
    if (x.length > y.length) return false;
    for (var i = 0; i < x.length; i++) if (x[i] !== y[i]) return false;
    return true;
  }
  // Going through the phrases in order, one that repeats the phrase before
  // it, or begins with it, takes its place instead of being added.
  function collapse(list) {
    var out = [];
    list.forEach(function (p) {
      p = String(p).trim();
      if (!words(p).length) return;
      if (out.length && revises(out[out.length - 1], p)) out[out.length - 1] = p;
      else out.push(p);
    });
    return out;
  }
  // Bare words become sentences (ways:dictation 3): an unpunctuated phrase
  // gets a capital where a sentence starts and a full stop where it ended
  // at a pause, and in English a lone 'i' stands up. A phrase the engine
  // punctuated stays as it came.
  var ENDS = /[.!?…]["”’)]*\s*$/;
  function sentence(p, starts, ends, english) {
    if (/[.,!?;:…]/.test(p)) return p;
    if (english) p = p.replace(/(^|\s)i(?=\s|'|’|$)/g, '$1I');
    if (starts) p = p.charAt(0).toUpperCase() + p.slice(1);
    return ends ? p + '.' : p;
  }

  var listening = null;     // the one microphone listening now, as its stop
  function addMic(box) {
    var Rec = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Rec || !box || box.dataset.mic) return;
    box.dataset.mic = '1';
    var row = document.createElement('div');
    row.className = 'mic-row';
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'mic';
    btn.setAttribute('aria-pressed', 'false');
    btn.appendChild(micIcon());
    var word = document.createElement('span');
    btn.appendChild(word);
    var note = document.createElement('p');
    note.className = 'mic-note';
    note.setAttribute('role', 'status');
    row.appendChild(btn);
    row.appendChild(note);
    box.parentNode.insertBefore(row, box.nextSibling);

    // Where the voice goes, said before anyone presses (ways:dictation 4).
    var WHERE = 'Your browser turns your voice into text; Chrome may send it to Google to do that.';
    var lang = document.documentElement.lang === 'en' ? 'en-GB' : (document.documentElement.lang || 'en-GB');
    var english = /^en\b/i.test(lang);
    var rec = null;           // the engine's session now running
    var wanted = false;       // the person still wants to speak
    var frozen = false;       // stopped by typing or sending: write nothing more
    var base = '';            // the box's words before this session began
    var phrases = [], live = '';
    var began = 0, heard = false, spoke = false, quiet = 0;

    function label(on) {
      word.textContent = on ? 'Stop' : 'Speak';
      btn.setAttribute('aria-pressed', on ? 'true' : 'false');
      btn.setAttribute('aria-label', on ? 'Stop listening' : 'Speak your words into the box');
      btn.classList.toggle('listening', on);
    }
    // The box's words before dictation, then the spoken phrases as
    // sentences, then the words still forming, marked with an ellipsis.
    function compose(forming) {
      var t = base, starts = !t.trim() || ENDS.test(t);
      function put(s) { t += (t && !/\s$/.test(t) ? ' ' : '') + s; }
      phrases.forEach(function (p) {
        var s = sentence(p, starts, true, english);
        put(s);
        starts = ENDS.test(s);
      });
      if (live) put(sentence(live, starts, false, english) + (forming ? '…' : ''));
      return t;
    }
    function show(t) {
      var max = box.maxLength > 0 ? box.maxLength : Infinity;
      box.value = t.slice(0, max);
      box.dispatchEvent(new Event('input', { bubbles: true }));
    }
    // Stop listening. Stopping lets the last words land; halting (on typing
    // or sending) keeps what is in the box and writes nothing more.
    function stop() {
      wanted = false;
      if (rec) try { rec.stop(); } catch (e) {}
    }
    function halt() {
      wanted = false;
      frozen = true;
      if (rec) try { rec.abort(); } catch (e) {}
    }
    function done(msg) {
      rec = null;
      if (listening === stop) listening = null;
      label(false);
      if (!frozen) show(compose(false).replace(/\s+$/, ''));
      note.textContent = msg || (spoke ? 'Read it through and change anything before you send it.' : WHERE);
    }

    function session() {
      var r = new Rec();
      rec = r;
      r.lang = lang;
      r.continuous = true;
      r.interimResults = true;
      began = Date.now();
      heard = false;
      phrases = [];
      live = '';
      var fault = '';
      r.onresult = function (ev) {
        if (frozen || r !== rec) return;
        heard = spoke = true;
        quiet = 0;
        var finals = [], forming = '';
        for (var i = 0; i < ev.results.length; i++) {
          var res = ev.results[i];
          if (res.isFinal) finals.push(res[0].transcript);
          else forming = res[0].transcript;
        }
        phrases = collapse(finals);
        live = forming.trim();
        // words still forming that repeat the last finished phrase are its revision
        if (live && phrases.length && revises(phrases[phrases.length - 1], live)) phrases.pop();
        show(compose(true));
      };
      r.onerror = function (ev) {
        var e = ev.error;
        if (e === 'no-speech' || e === 'aborted') return;
        wanted = false;
        fault = e === 'not-allowed' || e === 'service-not-allowed'
          ? 'The microphone wasn’t allowed. You can let this page use it in your browser’s settings, or type instead.'
          : e === 'audio-capture'
            ? 'No microphone could be found. You can type instead.'
            : 'Speaking didn’t work just now. You can type instead.';
      };
      r.onend = function () {
        if (r !== rec) return;
        // Silence ends a session, sooner on a phone. While the person still
        // wants to speak, start again, carrying what was said over as the new
        // starting text; a session that dies at once is a fault, not a pause.
        if (wanted && !frozen) {
          if (Date.now() - began < 1000) return done('Speaking didn’t work just now. You can type instead.');
          if (!heard && ++quiet >= 2) return done('I stopped listening after a long silence. Press Speak to go on.');
          base = compose(false);
          show(base);
          try { session(); return; } catch (e) { return done('Speaking didn’t work just now. You can type instead.'); }
        }
        done(fault);
      };
      r.start();
    }

    label(false);
    note.textContent = WHERE;
    btn.addEventListener('click', function () {
      if (rec) return stop();
      if (listening) listening();      // one microphone at a time
      listening = stop;
      wanted = true;
      frozen = false;
      spoke = false;
      quiet = 0;
      base = box.value;
      try {
        session();
        label(true);
        note.textContent = 'Listening… press Stop when you’re done. Nothing is sent until you send it.';
      } catch (e) {
        rec = null;
        listening = null;
        note.textContent = 'Speaking didn’t work just now. You can type instead.';
      }
    });
    // Typing takes over: the words heard so far stay, without the mark of
    // words still forming, and listening stops.
    box.addEventListener('beforeinput', function (ev) {
      if (!rec || !ev.isTrusted) return;
      box.value = compose(false);
      halt();
    });
    box.addEventListener('blur', function (ev) {
      if (rec && ev.relatedTarget !== btn) stop();
    });
    // Sending stops the microphone first, so what is sent has no ellipsis
    // and nothing is written into the emptied box afterwards.
    if (box.form) box.form.addEventListener('submit', function () {
      if (!rec) return;
      show(compose(false).replace(/\s+$/, ''));
      halt();
    }, true);
  }

  // Wire a note form (fields: who, web [a trap for bots], note; a button;
  // a .form-status line) to leave notes in the given pool.
  function wireNoteForm(form, pool, onSent) {
    addMic(form.note);
    var status = form.querySelector('.form-status');
    var button = form.querySelector('button[type=submit]') || form.querySelector('button');
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var note = form.note.value.trim();
      if (form.web.value || !note) return;
      // A typed name is shown as a visitor's, never as a beach handle, so
      // nobody can sign as someone else.
      var name = form.who.value.replace(/\s+/g, ' ').trim().slice(0, 40);
      var who = name ? name + ', a visitor' : 'a visitor';
      button.disabled = true;
      status.textContent = 'Sending…';
      postNote(pool, who, note).then(function () {
        form.note.value = '';
        status.textContent = 'Thank you. Your note is on the beach.';
        if (onSent) return onSent();
      }).catch(function () {
        status.textContent = 'That didn’t reach the beach. Please try again in a moment.';
      }).then(function () {
        button.disabled = false;
      });
    });
  }

  // Run refresh() now, every 30 seconds while the page is visible, and when
  // it comes back into view. refresh() returns a promise; its outcome is
  // shown in noteEl, and onFirstSuccess runs once the beach has answered.
  function live(refresh, noteEl, onFirstSuccess) {
    var lastSeen = null;
    function run() {
      return refresh().then(function () {
        if (!lastSeen && onFirstSuccess) onFirstSuccess();
        lastSeen = new Date();
        noteEl.textContent = 'Live from the beach · checked ' +
          lastSeen.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
      }, function () {
        if (!lastSeen) {
          noteEl.textContent = 'The beach is out of reach just now, so this is the last saved version.';
        }
      });
    }
    run();
    setInterval(function () {
      if (!document.hidden) run();
    }, REFRESH_MS);
    document.addEventListener('visibilitychange', function () {
      if (!document.hidden) run();
    });
    return run;
  }

  // The live dot in every page's one footer (brand 7.7), with one wording
  // and one set of colours on every page: grey 'Opening the beach…' as the
  // page opens (written in the page itself), green once it is reading the
  // beach now, amber if the read failed.
  function dot(ok) {
    var d = document.getElementById('live-dot'), w = document.getElementById('live-word');
    if (!d || !w) return;
    d.classList.toggle('on', !!ok);
    d.classList.toggle('off', !ok);
    w.textContent = ok ? 'Reading the beach now' : 'The beach can’t be reached just now';
  }

  // ---- Community Recovery's own place --------------------------------------
  // A second reader, for the place that carries the organisation's name
  // (beach.happyseaurchin.com/w/community-recovery). Its blocks read
  // openly, no key needed.
  var PLACE = 'https://beach.happyseaurchin.com/w/community-recovery/.well-known/pscale-beach';

  function placeGet(block) {
    return fetch(PLACE + '?block=' + encodeURIComponent(block), { cache: 'no-store' })
      .then(function (r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
      });
  }

  // The place's front door: its blocks, and when each last changed.
  function placeIndex() {
    return fetch(PLACE, { cache: 'no-store' }).then(function (r) {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    });
  }

  // Write into the place in the beach's own write shape: a POST to the
  // place's endpoint naming the block, with body {spindle, content} and,
  // for a locked position, its secret. A string at a spindle lands at
  // that position (whetstone 2.2, 6.2). The secret goes in this one
  // request and is kept nowhere. A refusal is thrown with .refused set.
  // newLock latches the block (or, with the same words as the secret,
  // proves them and changes nothing), as the standards page's kit does;
  // leave spindle or content undefined to send neither.
  function placeWrite(block, spindle, content, secret, newLock) {
    var body = { block: block };
    if (spindle != null) body.spindle = spindle;
    if (content !== undefined) body.content = content;
    if (secret) body.secret = secret;
    if (newLock) body.new_lock = newLock;
    return fetch(PLACE + '?block=' + encodeURIComponent(block), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      cache: 'no-store',
      credentials: 'omit',
      referrerPolicy: 'no-referrer'
    }).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (j) {
        if (r.ok && j && j.ok) return j;
        var e = new Error((j && j.error) || ('HTTP ' + r.status));
        e.refused = r.status >= 400 && r.status < 500;
        throw e;
      });
    });
  }

  // A note in one of the place's rooms, in postNote's shape, located at
  // an address (position 2), so a room can gather notes by where they
  // were left.
  function placeNote(pool, who, note, where) {
    return fetch(PLACE + '?block=' + encodeURIComponent(pool), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        append: true,
        content: { _: note, 1: who, 2: where || '', 3: new Date().toISOString() }
      })
    }).then(function (r) {
      return r.json().then(function (j) {
        if (!r.ok || !j.ok) throw new Error(j.error || ('HTTP ' + r.status));
      });
    });
  }

  // ---- The earth beach --------------------------------------------------------
  // A third reader, for the earth (earth.beach.happyseaurchin.com), where
  // places sit at their spatial addresses in spatial:earth. Reads openly.
  var EARTH = 'https://earth.beach.happyseaurchin.com/.well-known/pscale-beach';

  function earthGet(block) {
    return fetch(EARTH + '?block=' + encodeURIComponent(block), { cache: 'no-store' })
      .then(function (r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
      });
  }

  // The node at a position written key by key: "1.2.2" is key "1", then
  // "2", then "2" through the returned JSON.
  function at(node, position) {
    return String(position).split('.').reduce(function (n, k) {
      return n && typeof n === 'object' ? n[k] : undefined;
    }, node);
  }

  // ---- Folds ---------------------------------------------------------------
  // The folding pattern from the pscale-commons Community Recovery pages: a
  // section.part (or .sect, a fold inside a fold) holding a button.fold and
  // a div.kids. Opening sets "open" on the section, aria-expanded on the
  // button, and shows the kids; closing reverses it.
  function setOpen(sec, open) {
    var btn = sec.querySelector(':scope > button.fold');
    var kids = sec.querySelector(':scope > .kids');
    if (!btn || !kids) return;
    sec.classList.toggle('open', open);
    btn.setAttribute('aria-expanded', open ? 'true' : 'false');
    kids.hidden = !open;
  }

  // Wire every button.fold on the page not yet wired. Pages that build
  // folds as they load call this again afterwards. A button answers tap,
  // Enter and Space by itself.
  function folds(root) {
    Array.prototype.forEach.call((root || document).querySelectorAll('button.fold'), function (btn) {
      if (btn.dataset.fold) return;
      btn.dataset.fold = '1';
      var sec = btn.parentNode;
      setOpen(sec, sec.classList.contains('open'));
      btn.addEventListener('click', function () {
        setOpen(sec, !sec.classList.contains('open'));
      });
    });
  }

  // Open every fold that holds the element with this id (and the element
  // itself, if it is a fold), then scroll to it. False if it isn't there.
  function openTo(id) {
    var target = id && document.getElementById(id);
    if (!target) return false;
    for (var n = target; n && n !== document.body; n = n.parentNode) {
      if (n.classList && (n.classList.contains('part') || n.classList.contains('sect'))) setOpen(n, true);
    }
    target.scrollIntoView();
    return true;
  }

  // Links with #something land, opening their folds: on arrival, when the
  // address changes, and when an in-page link is tapped again.
  function landOnHash() {
    openTo(decodeURIComponent(location.hash.slice(1)));
  }
  window.addEventListener('hashchange', landOnHash);
  document.addEventListener('click', function (ev) {
    var a = ev.target.closest && ev.target.closest('a[href^="#"]');
    if (a && a.getAttribute('href') === location.hash) openTo(location.hash.slice(1));
  });

  // The one line under a fold's title, taken from its own words: its
  // first sentence (a short lead line is one), cut at a word boundary to
  // about 120 characters with an ellipsis.
  function gloss(s) {
    s = String(s || '').replace(/\s+/g, ' ').trim();
    var m = s.match(/^.+?[.!?](?:["”’])?(?=\s|$)/);
    if (m) s = m[0];
    if (s.length <= 120) return s;
    var cut = s.lastIndexOf(' ', 119);
    return s.slice(0, cut > 60 ? cut : 119).replace(/[\s,;:—-]+$/, '') + '…';
  }

  // A fold's gloss from what it holds: its opening paragraph (its short
  // lead line, when it opens with one), else its first list item.
  function glossOf(kids) {
    var first = kids.querySelector('p:not(.live-note)') || kids.querySelector('li');
    return gloss(first ? first.textContent : kids.textContent);
  }

  window.Beach = {
    folds: folds,
    openTo: openTo,
    landOnHash: landOnHash,
    gloss: gloss,
    glossOf: glossOf,
    place: { get: placeGet, at: at, index: placeIndex, write: placeWrite, note: placeNote },
    earth: { get: earthGet },
    get: get,
    point: point,
    index: index,
    clock: function () { return clock; },
    text: text,
    notes: function (pool) { return entries(pool); },
    renderThread: renderThread,
    wireNoteForm: wireNoteForm,
    addMic: addMic,
    live: live,
    dot: dot
  };
})();
