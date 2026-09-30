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
        if (e._.trim()) out.push({ who: name(e['1']), when: e['3'], text: e._, replies: replies(e) });
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
  // be read and changed before anything is posted.
  var MIC_ICON = '<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><rect x="9" y="3" width="6" height="11" rx="3" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>';
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
    var note = document.createElement('p');
    note.className = 'mic-note';
    note.setAttribute('role', 'status');
    row.appendChild(btn);
    row.appendChild(note);
    box.parentNode.insertBefore(row, box.nextSibling);

    var rec = null, base = '', said = '';
    function label(on) {
      btn.innerHTML = MIC_ICON + '<span>' + (on ? 'Stop' : 'Speak') + '</span>';
      btn.setAttribute('aria-pressed', on ? 'true' : 'false');
      btn.setAttribute('aria-label', on ? 'Stop listening' : 'Speak your words into the box');
      btn.classList.toggle('listening', on);
    }
    function fill(extra) {
      var t = base + said + extra;
      var max = box.maxLength > 0 ? box.maxLength : Infinity;
      box.value = t.slice(0, max);
      box.dispatchEvent(new Event('input', { bubbles: true }));
    }
    function stop() { if (rec) rec.stop(); }
    label(false);

    btn.addEventListener('click', function () {
      if (rec) return stop();
      rec = new Rec();
      rec.lang = document.documentElement.lang === 'en' ? 'en-GB' : (document.documentElement.lang || 'en-GB');
      rec.continuous = true;
      rec.interimResults = true;
      base = box.value && !/\s$/.test(box.value) ? box.value + ' ' : box.value;
      said = '';
      rec.onresult = function (ev) {
        var interim = '';
        for (var i = ev.resultIndex; i < ev.results.length; i++) {
          var t = ev.results[i][0].transcript;
          if (ev.results[i].isFinal) said += t.trim() + ' ';
          else interim += t;
        }
        fill(interim);
      };
      rec.onerror = function (ev) {
        note.textContent = ev.error === 'not-allowed' || ev.error === 'service-not-allowed'
          ? 'The microphone wasn\u2019t allowed. You can let this page use it in your browser\u2019s settings, or type instead.'
          : ev.error === 'no-speech'
            ? 'I didn\u2019t hear anything. Press Speak to try again.'
            : 'Speaking didn\u2019t work just now. You can type instead.';
      };
      rec.onend = function () {
        rec = null;
        fill('');
        box.value = box.value.replace(/\s+$/, '');
        label(false);
        if (/^Listening/.test(note.textContent)) note.textContent = 'Read it through and change anything before you post.';
      };
      try {
        rec.start();
        label(true);
        note.textContent = 'Listening\u2026 press Stop when you\u2019re done. Your browser may send your voice to its maker (Google or Apple) to turn it into text; nothing is posted until you press Post.';
      } catch (e) {
        rec = null;
        note.textContent = 'Speaking didn\u2019t work just now. You can type instead.';
      }
    });
    // Posting stops the microphone.
    if (box.form) box.form.addEventListener('submit', stop);
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

  // The node at a position written key by key: "1.2.2" is key "1", then
  // "2", then "2" through the returned JSON.
  function at(node, position) {
    return String(position).split('.').reduce(function (n, k) {
      return n && typeof n === 'object' ? n[k] : undefined;
    }, node);
  }

  window.Beach = {
    place: { get: placeGet, at: at },
    get: get,
    point: point,
    index: index,
    clock: function () { return clock; },
    text: text,
    renderThread: renderThread,
    wireNoteForm: wireNoteForm,
    addMic: addMic,
    live: live
  };
})();
