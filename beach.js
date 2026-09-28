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

  function entries(node, out) {
    out = out || [];
    if (!node || typeof node !== 'object') return out;
    if (node._ && typeof node._ === 'object') entries(node._, out);
    for (var i = 1; i <= 9; i++) {
      var e = node[String(i)];
      if (isEntry(e)) {
        if (e._.trim()) out.push({ who: e['1'] || '', when: e['3'], text: e._ });
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
      var div = document.createElement('div');
      div.className = 'comment';
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
      t.textContent = c.text;
      div.appendChild(t);
      el.appendChild(div);
    });
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

  // Wire a note form (fields: who, web [a trap for bots], note; a button;
  // a .form-status line) to leave notes in the given pool.
  function wireNoteForm(form, pool, onSent) {
    var status = form.querySelector('.form-status');
    var button = form.querySelector('button');
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

  window.Beach = {
    get: get,
    point: point,
    index: index,
    clock: function () { return clock; },
    text: text,
    renderThread: renderThread,
    wireNoteForm: wireNoteForm,
    live: live
  };
})();
