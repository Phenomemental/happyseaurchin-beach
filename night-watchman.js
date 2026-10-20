/* What the Night Watchman's three pages share. Their words are Dorylus's,
   kept on the beach at beings:night-watchman 8.8363 (the front door at 1,
   Begin at 2, the Beings at 3) and read live; each page is written with
   those words already in place, so it makes sense before the beach answers
   or if it never does, and a part whose words can't be read keeps them. */
(function () {
  var BLOCK = 'beings:night-watchman';
  var PATH = ['8', '8', '3', '6', '3'];

  // A node's own words: a string, or its "_", followed down when the beach
  // has nested older words there. A dated note at the end saying how the
  // words were changed ("(Changed 19 October 2026, evening: …)") is for
  // their keepers, never the page.
  var CHANGED = /\s*\([^()]*\bchanged\b[^()]*\b\d{4}\b[^()]*\)\s*$/i;
  function own(node) {
    while (node && typeof node === 'object' && node._ && typeof node._ === 'object') node = node._;
    return (Beach.text(node) || '').trim().replace(CHANGED, '');
  }
  function at(node, path) {
    for (var i = 0; i < path.length && node; i++) node = node && typeof node === 'object' ? node[path[i]] : null;
    return node || null;
  }

  // "Label: value. Label: value." Each label is looked for after the one
  // before it, at the start of a sentence; a label written twice is given
  // twice ("Button", "Button#2"). Only the labels found are returned.
  function fields(s, labels) {
    var found = [], from = 0;
    labels.forEach(function (key) {
      var name = key.replace(/#\d+$/, '') + ': ';
      var i = s.indexOf(name, from);
      while (i > 0 && !/(^|[.:]\s)$/.test(s.slice(Math.max(0, i - 2), i))) i = s.indexOf(name, i + 1);
      if (i < 0) return;
      found.push({ key: key, start: i, from: i + name.length });
      from = i + name.length;
    });
    var out = {};
    found.forEach(function (f, j) {
      out[f.key] = s.slice(f.from, j + 1 < found.length ? found[j + 1].start : s.length).trim();
    });
    return out;
  }
  // "i, Title: body. ii, Title: body." as items; a list with no titles
  // ("i, In Claude, open Settings…") gives the whole item as its body.
  function numbered(s, titled) {
    var re = /(?:^|[\s.:])(iv|vi|v|iii|ii|i), /g, marks = [], m;
    while ((m = re.exec(s))) marks.push({ n: m[1], start: m.index + m[0].length - m[1].length - 2, from: m.index + m[0].length });
    return marks.map(function (k, j) {
      var t = s.slice(k.from, j + 1 < marks.length ? marks[j + 1].start : s.length).trim();
      if (!titled) return { n: k.n, title: '', body: t };
      var c = t.indexOf(': ');
      return c < 0 ? { n: k.n, title: t, body: '' } : { n: k.n, title: t.slice(0, c), body: t.slice(c + 2) };
    });
  }
  // A part that opens in capitals ("WHAT YOU NEED: …"): its head in
  // sentence case, and the rest.
  function head(s) {
    var m = /^([A-Z][A-Z ,'’-]*[A-Z])[.:]\s*([\s\S]*)$/.exec(s);
    if (!m) return { head: '', rest: s };
    var h = m[1].toLowerCase();
    return { head: h.charAt(0).toUpperCase() + h.slice(1), rest: m[2] };
  }
  // A button's words without the sentence's full stop.
  function bare(s) {
    return (s || '').replace(/\.\s*$/, '');
  }
  function sentences(s) {
    return (s.match(/[^.!?]+[.!?]+["”’)]*(\s+|$)|[^.!?]+$/g) || []).map(function (x) { return x.trim(); }).filter(Boolean);
  }
  function set(el, s) {
    if (el && s) el.textContent = s;
  }

  // A line of care: the number to call stands out and can be called from a
  // phone, and the helpline finder opens.
  function care(el, s) {
    if (!el || !s) return;
    el.textContent = '';
    var re = /(Samaritans, 116 123|116 123|findahelpline\.com)/g, last = 0, m;
    while ((m = re.exec(s))) {
      if (m.index > last) el.appendChild(document.createTextNode(s.slice(last, m.index)));
      if (m[1] === 'findahelpline.com') {
        var a = document.createElement('a');
        a.href = 'https://findahelpline.com';
        a.textContent = m[1];
        el.appendChild(a);
      } else {
        var tel = document.createElement('a');
        tel.className = 'tel';
        tel.href = 'tel:116123';
        var b = document.createElement('strong');
        b.textContent = m[1];
        tel.appendChild(b);
        el.appendChild(tel);
      }
      last = re.lastIndex;
    }
    if (last < s.length) el.appendChild(document.createTextNode(s.slice(last)));
  }

  // What all three pages carry (8.83634): the signature each ends with,
  // and the care line the Beings page sets under its opening.
  function forAll(words) {
    var t = own(words && words['4']);
    var sig = /\bsignature ([A-Z][^,.]*?)[,.]/.exec(t);
    var care = /\bunder its opening: ([\s\S]+)$/.exec(t);
    return { signature: sig ? sig[1].trim() : '', care: care ? care[1].trim() : '' };
  }

  // Read the words, now and whenever the page comes back into view, and
  // hand them to render(words, block) only when they, or the Beings and
  // their portals (1, 2), how to read through one (3), the two extremes
  // (4), that they are for reading (5) or play (9), have changed. The
  // beach's own status line is kept off the page.
  function follow(render) {
    var shown = '';
    Beach.live(function () {
      return Beach.get(BLOCK).then(function (block) {
        var words = at(block, PATH);
        if (!words) throw new Error('no words');
        var sig = JSON.stringify(words) + JSON.stringify(['1', '2', '3', '4', '5', '9'].map(function (k) { return block[k]; }));
        if (sig === shown) return;
        shown = sig;
        render(words, block);
      });
    }, document.createElement('p'), null, '');
  }

  window.NW = {
    own: own,
    at: at,
    fields: fields,
    numbered: numbered,
    head: head,
    bare: bare,
    sentences: sentences,
    set: set,
    care: care,
    forAll: forAll,
    follow: follow
  };
})();
