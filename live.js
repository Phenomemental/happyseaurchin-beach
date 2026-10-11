/* What lets a reader see, and touch, that a page is live: a count that
   pulses when it rises, lines that draw themselves as they are reached,
   a line of time to tap along, a sky with a flock of what has just moved
   on the beach; and the pieces a long page is folded into: chapters,
   rows, tiles, a dial, one-of-several, a bar at the foot of the screen,
   and web addresses shown short. Shared by the pages that read from the beach; every word stays
   the beach's, or the page's own short labels. Needs live.css. Nothing
   here is needed for the words to show, and all of it holds still for
   anyone who asks their device for less motion. */
(function () {
  var MOTION = window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : null;
  var DARK = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  // True when the reader has asked for less motion.
  function still() {
    return !!(MOTION && MOTION.matches);
  }

  // Run fn whenever the reader's motion or colour settings change.
  function onSettings(fn) {
    [MOTION, DARK].forEach(function (q) {
      if (!q) return;
      if (q.addEventListener) q.addEventListener('change', fn);
      else if (q.addListener) q.addListener(fn);
    });
  }

  function clamp(v, lo, hi) {
    return Math.min(Math.max(v, lo), hi);
  }

  // This device's memory. Storage can be missing, full or refused, so
  // every read and write may quietly come to nothing.
  function recall(key) {
    try {
      return window.localStorage.getItem(key);
    } catch (e) {
      return null;
    }
  }

  function remember(key, value) {
    try {
      window.localStorage.setItem(key, value);
      return true;
    } catch (e) {
      return false;
    }
  }

  // How long ago, in words: "5 minutes ago", "1 hour ago", "3 days ago".
  function plural(n, one) {
    return n + ' ' + one + (n === 1 ? '' : 's') + ' ago';
  }

  function ago(ms) {
    var m = Math.floor(ms / 60000);
    if (m < 60) return plural(Math.max(m, 0), 'minute');
    var h = Math.floor(m / 60);
    if (h < 24) return plural(h, 'hour');
    return plural(Math.floor(h / 24), 'day');
  }

  // What moved, in plain words, from a block's name; present holds every
  // name on the beach (name -> true), for the readings that depend on
  // what else is there. A name with no plain reading is given as it is.
  //   pool:X      a note in X’s room
  //   now:X       X said their day
  //   spine:X     a post in X’s diary, when X has a passport; otherwise
  //               the X family changed
  //   passport:X  X’s card changed
  //   X:Y         Y answered X, when there is a spine:X
  function moved(name, present) {
    var m = /^([^:]+):(.+)$/.exec(name);
    if (!m) return name;
    var kind = m[1];
    var x = m[2];
    present = present || {};
    if (kind === 'pool') return 'a note in ' + x + '’s room';
    if (kind === 'now') return x + ' said their day';
    if (kind === 'spine') {
      return present['passport:' + x] ? 'a post in ' + x + '’s diary' : 'the ' + x + ' family changed';
    }
    if (kind === 'passport') return x + '’s card changed';
    if (present['spine:' + kind]) return x + ' answered ' + kind;
    return name;
  }

  // Words put into el with a place to break after every colon and hyphen,
  // so a long name wraps there and not in the middle of a word.
  function breakable(el, s) {
    s.split(/([:-])/).forEach(function (part, i) {
      if (!part) return;
      el.appendChild(document.createTextNode(part));
      if (i % 2) el.appendChild(document.createElement('wbr'));
    });
    return el;
  }

  // One pulse on an element; none for anyone who asks for less motion,
  // nor for one not on the page just now (in a closed chapter), which
  // would only pulse later, out of time.
  function pulse(el) {
    if (still() || !el.getClientRects().length) return;
    el.classList.remove('live-pulse');
    void el.offsetWidth;
    el.classList.add('live-pulse');
  }

  // ---- A count ------------------------------------------------------------
  // A link that says how many there are, kept up to date. make(cls) gives
  // one more copy of it, for a page that shows it in more than one place;
  // set(n) changes the number in every copy, and each pulses once when it
  // rises. words(n) gives the label, with n null when the count couldn't
  // be read.
  function count(href, words) {
    var n = null;
    var all = [];
    function paint(a) {
      a.textContent = words(n);
    }
    return {
      make: function (cls) {
        var a = document.createElement('a');
        a.href = href;
        a.className = 'live-count' + (cls ? ' ' + cls : '');
        a.addEventListener('animationend', function (e) {
          if (e.animationName === 'live-ring') a.classList.remove('live-pulse');
        });
        paint(a);
        all.push(a);
        return a;
      },
      set: function (v) {
        if (v === n) return;
        var rose = n != null && v != null && v > n;
        n = v;
        all.forEach(function (a) {
          paint(a);
          if (rose) pulse(a);
        });
      },
      value: function () {
        return n;
      }
    };
  }

  // ---- Lines that draw themselves ---------------------------------------
  // Each element is given --live-draw, from 0 to 1, for the page's CSS to
  // scale the element's own line by. A pen moves down through them at a
  // steady pace towards the reading line, a little above the foot of the
  // window, and never goes back up. Once it has passed them all the
  // property is taken away, so the lines stand exactly as they were
  // drawn before. done() runs then. Nothing is drawn, and every line
  // stands whole, for anyone who asks for less motion. Returns a stop().
  var PEN_SPEED = 520; // pixels a second

  function drawLine(items, done) {
    items = Array.prototype.slice.call(items || []);
    if (!items.length || still()) {
      if (done) done();
      return function () {};
    }
    var pen = null;
    var target = -Infinity;
    var raf = 0;
    var last = 0;
    items.forEach(function (e) { e.style.setProperty('--live-draw', '0'); });

    function stop() {
      if (raf) cancelAnimationFrame(raf);
      raf = 0;
      window.removeEventListener('scroll', nudge);
      window.removeEventListener('resize', nudge);
    }
    function finish() {
      stop();
      items.forEach(function (e) { e.style.removeProperty('--live-draw'); });
      if (done) done();
    }
    function nudge() {
      if (!raf) raf = requestAnimationFrame(tick);
    }
    function tick(ts) {
      raf = 0;
      if (!document.documentElement.contains(items[0])) return stop();
      if (still()) return finish();
      var y = window.pageYOffset;
      var boxes = items.map(function (e) {
        var r = e.getBoundingClientRect();
        return { top: r.top + y, h: r.height };
      });
      // Not on the page yet: wait for the next scroll.
      if (!boxes[0].h) {
        last = 0;
        return;
      }
      if (pen == null) pen = boxes[0].top;
      var reach = y + window.innerHeight * 0.8;
      // At the foot of the page the reading line can go no lower, so
      // whatever is left is drawn.
      if (y + window.innerHeight >= document.documentElement.scrollHeight - 2) reach = Infinity;
      target = Math.max(target, reach);
      var dt = last ? Math.min((ts - last) / 1000, 0.1) : 1 / 60;
      if (pen < target) pen = Math.min(target, pen + PEN_SPEED * dt);
      boxes.forEach(function (b, i) {
        var p = b.h ? clamp((pen - b.top) / b.h, 0, 1) : 1;
        items[i].style.setProperty('--live-draw', p.toFixed(3));
      });
      var end = boxes[boxes.length - 1];
      if (pen >= end.top + end.h) return finish();
      if (pen < target) {
        last = ts;
        raf = requestAnimationFrame(tick);
      } else {
        last = 0;
      }
    }
    window.addEventListener('scroll', nudge, { passive: true });
    window.addEventListener('resize', nudge);
    nudge();
    return stop;
  }

  // ---- A line of time -----------------------------------------------------
  var SVG = 'http://www.w3.org/2000/svg';
  function svg(tag, cls) {
    var e = document.createElementNS(SVG, tag);
    if (cls) e.setAttribute('class', cls);
    return e;
  }

  // Centres xs, in order, moved apart so that no two are closer than gap,
  // each moving as little as it can, all within lo to hi. (Runs that
  // crowd together are spread about their middle.)
  function spread(xs, gap, lo, hi) {
    var groups = [];
    xs.forEach(function (x) {
      groups.push({ n: 1, sum: x, at: clamp(x, lo, hi) });
      while (groups.length > 1) {
        var b = groups[groups.length - 1];
        var a = groups[groups.length - 2];
        if (a.at + a.n * gap <= b.at + 0.001) break;
        a.sum += b.sum - b.n * a.n * gap;
        a.n += b.n;
        a.at = clamp(a.sum / a.n, lo, hi - (a.n - 1) * gap);
        groups.pop();
      }
    });
    var out = [];
    groups.forEach(function (g) {
      for (var j = 0; j < g.n; j++) out.push(g.at + j * gap);
    });
    return out;
  }

  // A line from the first moment to the last, with one dot for each moment
  // placed by its time. moments is [{ label, at, target, note }]: label is
  // the time as written, at is minutes, target is the element the moment
  // is in the page (its list item), and note, if given, describes it.
  //
  // Beside each dot hangs its time, a button at least 44px square; where
  // moments crowd, their times spread apart, each on a fine lead back to
  // its dot. When they cannot all sit in one row under the line they
  // alternate above and below it, and when two rows can't hold them the
  // line grows wider than the page and scrolls sideways on its own.
  //
  // Tapping or focusing a time, or tapping near a dot, marks that moment
  // as the current one, here and in the page (class live-current), and
  // brings it into view; opts.onPick(i) hears which (i is its place in
  // moments). select(i) marks one from outside, quietly, the ring gliding
  // along the line to it. Arrow keys move between times; Tab comes in at
  // the current one. Returns { el, select, destroy }.
  var TARGET = 44; // the least a thing to tap can be, in pixels
  var LEAD = 16;

  function timeline(moments, opts) {
    opts = opts || {};
    var box = document.createElement('div');
    box.className = 'live-time';
    var inner = document.createElement('div');
    inner.className = 'live-time-inner';
    inner.setAttribute('role', 'toolbar');
    if (opts.name) inner.setAttribute('aria-label', opts.name);
    box.appendChild(inner);
    var art = svg('svg', 'live-time-art');
    art.setAttribute('aria-hidden', 'true');
    art.setAttribute('focusable', 'false');
    inner.appendChild(art);
    var axis = svg('line', 'live-time-axis');
    art.appendChild(axis);

    var marks = moments.map(function (m, i) {
      return { label: m.label, at: m.at, target: m.target, note: m.note, i: m.i != null ? m.i : i };
    }).sort(function (a, b) {
      return a.at - b.at || a.i - b.i;
    });
    marks.forEach(function (k, j) {
      k.lead = svg('line', 'live-time-lead');
      art.appendChild(k.lead);
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'live-time-mark';
      b.textContent = k.label;
      b.tabIndex = j === 0 ? 0 : -1;
      if (k.target && k.target.id) b.setAttribute('aria-controls', k.target.id);
      if (k.note && k.note.id) b.setAttribute('aria-describedby', k.note.id);
      b.addEventListener('focus', function () { pick(j); });
      b.addEventListener('click', function () { pick(j); });
      k.btn = b;
      inner.appendChild(b);
    });
    marks.forEach(function (k) {
      k.dot = svg('circle', 'live-time-dot');
      k.dot.setAttribute('r', '4.5');
      art.appendChild(k.dot);
    });
    var ring = svg('circle', 'live-time-ring');
    ring.setAttribute('r', '8.5');
    ring.style.display = 'none';
    art.appendChild(ring);

    var cur = -1;
    var shown = { j: -1, t: 0 };
    var axisY = 0;

    function current(j) {
      if (cur === j) return;
      if (cur >= 0) mark(marks[cur], false);
      cur = j;
      mark(marks[j], true);
      marks.forEach(function (m, q) { m.btn.tabIndex = q === j ? 0 : -1; });
      placeRing();
    }

    function pick(j) {
      var k = marks[j];
      if (!k) return;
      var was = cur;
      current(j);
      if (was !== j && opts.onPick) opts.onPick(k.i);
      // A tap both focuses and clicks; bring the moment into view once.
      var now = Date.now();
      if (shown.j === j && now - shown.t < 400) return;
      shown = { j: j, t: now };
      if (k.target && k.target.scrollIntoView) {
        k.target.scrollIntoView({ block: 'nearest', behavior: still() ? 'auto' : 'smooth' });
      }
      sideways(k);
    }

    // On a line wider than the page, keep the current time in sight.
    function sideways(k) {
      if (box.scrollWidth <= box.clientWidth) return;
      var x = parseFloat(k.btn.style.left) || 0;
      if (x - TARGET < box.scrollLeft || x + TARGET > box.scrollLeft + box.clientWidth) {
        box.scrollLeft = x - box.clientWidth / 2;
      }
    }

    // An i that isn't on the line leaves no moment current.
    function select(i) {
      for (var j = 0; j < marks.length; j++) {
        if (marks[j].i === i) {
          current(j);
          sideways(marks[j]);
          return;
        }
      }
      if (cur >= 0) mark(marks[cur], false);
      cur = -1;
      ring.style.display = 'none';
      marks.forEach(function (m, q) { m.btn.tabIndex = q === 0 ? 0 : -1; });
    }

    function mark(k, on) {
      k.btn.classList.toggle('is-current', on);
      if (on) k.btn.setAttribute('aria-current', 'true');
      else k.btn.removeAttribute('aria-current');
      k.lead.classList.toggle('is-current', on);
      k.dot.setAttribute('r', on ? '6' : '4.5');
      if (k.target) {
        k.target.classList.toggle('live-current', on);
        if (on) k.target.setAttribute('aria-current', 'true');
        else k.target.removeAttribute('aria-current');
      }
    }

    // The ring is moved, not redrawn, so it glides from dot to dot.
    function placeRing() {
      var k = marks[cur];
      if (!k || k.x == null) return;
      ring.style.transform = 'translate(' + k.x + 'px,' + axisY + 'px)';
      ring.style.display = '';
    }

    inner.addEventListener('keydown', function (e) {
      var j = cur >= 0 ? cur : 0;
      var n = marks.length;
      if (e.key === 'ArrowRight' || e.key === 'ArrowDown') j = Math.min(j + 1, n - 1);
      else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') j = Math.max(j - 1, 0);
      else if (e.key === 'Home') j = 0;
      else if (e.key === 'End') j = n - 1;
      else return;
      e.preventDefault();
      marks[j].btn.focus({ preventScroll: true });
      pick(j);
    });

    // A tap on the line itself goes to the nearest dot.
    art.addEventListener('click', function (e) {
      var r = art.getBoundingClientRect();
      var x = e.clientX - r.left;
      var best = -1;
      var bd = TARGET;
      marks.forEach(function (k, j) {
        var d = Math.abs(k.x - x);
        if (d < bd) {
          bd = d;
          best = j;
        }
      });
      if (best >= 0) {
        marks[best].btn.focus({ preventScroll: true });
        pick(best);
      }
    });

    var lastW = 0;
    function layout(force) {
      var W = box.clientWidth;
      if (!W || (W === lastW && !force)) return;
      lastW = W;
      var n = marks.length;
      var widest = 0;
      marks.forEach(function (k) { widest = Math.max(widest, k.btn.offsetWidth); });
      var gap = Math.max(TARGET, widest + 4);
      var t0 = marks[0].at;
      var span = marks[n - 1].at - t0;
      function trueX(width) {
        var lo = gap / 2;
        var hi = width - gap / 2;
        return marks.map(function (k) {
          return span ? lo + (k.at - t0) / span * (hi - lo) : width / 2;
        });
      }
      var width = W;
      var xs = trueX(W);
      var at = null;
      var rows = 2;
      if (n * gap <= W) {
        at = spread(xs, gap, gap / 2, W - gap / 2);
        var shift = 0;
        at.forEach(function (x, j) { shift = Math.max(shift, Math.abs(x - xs[j])); });
        if (shift <= gap) rows = 1;
      }
      if (rows === 2) {
        width = Math.max(W, Math.ceil(n / 2) * gap);
        xs = trueX(width);
        at = new Array(n);
        [0, 1].forEach(function (r) {
          var idx = [];
          for (var j = r; j < n; j += 2) idx.push(j);
          var s = spread(idx.map(function (j) { return xs[j]; }), gap, gap / 2, width - gap / 2);
          idx.forEach(function (j, q) { at[j] = s[q]; });
        });
      }
      // The line, with a row of times above it when there are two rows,
      // and a row below it always.
      axisY = rows === 2 ? TARGET + LEAD + 2 : 7;
      var below = axisY + LEAD + 2;
      var height = below + TARGET;
      marks.forEach(function (k, j) {
        var under = rows === 1 || j % 2 === 1;
        var top = under ? below : 0;
        k.x = xs[j];
        k.btn.style.left = at[j] + 'px';
        k.btn.style.top = top + 'px';
        k.dot.setAttribute('cx', xs[j]);
        k.dot.setAttribute('cy', axisY);
        k.lead.setAttribute('x1', xs[j]);
        k.lead.setAttribute('y1', axisY + (under ? 7 : -7));
        k.lead.setAttribute('x2', at[j]);
        k.lead.setAttribute('y2', under ? top + 12 : top + TARGET - 12);
      });
      axis.setAttribute('x1', xs[0]);
      axis.setAttribute('x2', xs[n - 1]);
      axis.setAttribute('y1', axisY);
      axis.setAttribute('y2', axisY);
      axis.style.display = n > 1 ? '' : 'none';
      inner.style.width = width + 'px';
      inner.style.height = height + 'px';
      art.setAttribute('width', width);
      art.setAttribute('height', height);
      art.setAttribute('viewBox', '0 0 ' + width + ' ' + height);
      box.classList.toggle('live-time-wide', width > W);
      placeRing();
    }

    var ro = null;
    function relayout() { layout(false); }
    if (window.ResizeObserver) {
      ro = new ResizeObserver(relayout);
      ro.observe(box);
    } else {
      window.addEventListener('resize', relayout);
    }
    // The times are measured in the page's own type, once it has come.
    if (document.fonts && document.fonts.ready) {
      document.fonts.ready.then(function () { layout(true); });
    }

    return {
      el: box,
      select: select,
      layout: function () { layout(true); },
      destroy: function () {
        if (ro) ro.disconnect();
        else window.removeEventListener('resize', relayout);
      }
    };
  }

  // ---- Web addresses in beach text ----------------------------------------------
  // Text from the beach put into el, with every http or https address in it
  // shown as a link whose words are the host and the last piece of the
  // path, never the whole address. The link can break only after a slash,
  // never inside a piece. Everything is set with textContent and href; no
  // markup from the text is ever read. With plain, the short words are
  // shown without a link (for inside a button, where a link can't go).
  var ADDRESS = /\bhttps?:\/\/[^\s<>"'“”‘’]+/gi;

  function address(raw) {
    try {
      var u = new URL(raw);
      return u.protocol === 'http:' || u.protocol === 'https:' ? u : null;
    } catch (e) {
      return null;
    }
  }

  // Trailing punctuation belongs to the sentence, not the address; a
  // closing bracket stays if the address opened one.
  function trimAddress(raw) {
    return raw.replace(/[.,;:!?'"’”\]]+$/, '').replace(/\)+$/, function (tail) {
      return raw.indexOf('(') >= 0 ? tail : '';
    }).replace(/[.,;:!?'"’”\]]+$/, '');
  }

  function shortAddress(el, u) {
    var path = u.pathname.split('/').filter(Boolean);
    var pieces = [u.host];
    if (path.length > 1) pieces.push('…');
    if (path.length) {
      var last = path[path.length - 1];
      try {
        last = decodeURIComponent(last);
      } catch (e) {}
      pieces.push(last);
    }
    pieces.forEach(function (p, i) {
      var s = document.createElement('span');
      s.className = 'live-link-piece';
      s.textContent = p + (i < pieces.length - 1 ? '/' : '');
      el.appendChild(s);
      if (i < pieces.length - 1) el.appendChild(document.createElement('wbr'));
    });
    return el;
  }

  function linkify(el, text, plain) {
    text = text == null ? '' : String(text);
    var at = 0;
    var m;
    ADDRESS.lastIndex = 0;
    while ((m = ADDRESS.exec(text))) {
      var raw = trimAddress(m[0]);
      var u = address(raw);
      if (!u) continue;
      if (m.index > at) el.appendChild(document.createTextNode(text.slice(at, m.index)));
      var a = document.createElement(plain ? 'span' : 'a');
      a.className = 'live-link';
      if (!plain) a.href = u.href;
      el.appendChild(shortAddress(a, u));
      at = m.index + raw.length;
      ADDRESS.lastIndex = at;
    }
    if (at < text.length) el.appendChild(document.createTextNode(text.slice(at)));
    return el;
  }

  // ---- Folds ----------------------------------------------------------------
  // A button that opens and shuts a panel, the panel's height easing open
  // and shut (at once, for anyone who asks for less motion). The button
  // carries aria-expanded and aria-controls; the panel is hidden while
  // shut, so nothing in it can be reached, and is always open in print.
  // opts.onChange(open) hears each change. Returns
  // { open(quick), close(quick), isOpen() }.
  var made = 0;
  function uid(prefix) {
    made += 1;
    return prefix + '-' + made;
  }

  function fold(button, panel, opts) {
    opts = opts || {};
    if (!panel.id) panel.id = uid('live-fold');
    panel.classList.add('live-fold');
    button.setAttribute('aria-controls', panel.id);
    var open = !panel.hidden;
    var anim = null;
    button.setAttribute('aria-expanded', open ? 'true' : 'false');

    function set(on, quick) {
      if (on === open) return;
      open = on;
      button.setAttribute('aria-expanded', on ? 'true' : 'false');
      if (anim) {
        anim.cancel();
        anim = null;
      }
      if (quick || still() || !panel.animate) {
        panel.hidden = !on;
      } else if (on) {
        panel.hidden = false;
        anim = panel.animate([
          { height: '0px', overflow: 'hidden' },
          { height: panel.scrollHeight + 'px', overflow: 'hidden' }
        ], { duration: 320, easing: 'cubic-bezier(0.2, 0.7, 0.2, 1)' });
        anim.onfinish = function () { anim = null; };
      } else {
        anim = panel.animate([
          { height: panel.offsetHeight + 'px', overflow: 'hidden' },
          { height: '0px', overflow: 'hidden' }
        ], { duration: 220, easing: 'ease-in' });
        anim.onfinish = function () {
          anim = null;
          panel.hidden = true;
        };
      }
      if (opts.onChange) opts.onChange(on);
    }

    button.addEventListener('click', function () { set(!open); });
    return {
      open: function (quick) { set(true, quick); },
      close: function (quick) { set(false, quick); },
      isOpen: function () { return open; }
    };
  }

  // A chevron that turns when its fold opens.
  function mark(cls) {
    var s = document.createElement('span');
    s.className = 'live-mark' + (cls ? ' ' + cls : '');
    s.setAttribute('aria-hidden', 'true');
    return s;
  }

  // ---- Chapters -------------------------------------------------------------
  // A page in chapters that start closed: each a full-width button with
  // the chapter's label, its opening line in italic and a mark at the
  // right that turns when open; with a small drawing at the left, when
  // given (c.mini). Closed, the opening line shows at most two lines.
  // Opening one closes the others, keeping the chosen one where it was on
  // the screen. A link to a chapter's address (#id) opens it and brings
  // it into view; follow() does the same for the address the page was
  // opened at, once it is showing.
  //   list: [{ id, label, line, mini, body: element, onOpen, onClose }]
  // Returns { open(id, bring, quick), opened() -> id or null, follow(),
  // destroy(), list: [{ id, label, el }] }.
  function chapters(box, list) {
    var items = list.map(function (c) {
      var sec = document.createElement('section');
      sec.className = 'live-chapter';
      sec.id = c.id;
      var head = document.createElement('h2');
      head.className = 'live-chapter-head';
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'live-chapter-button';
      var words = document.createElement('span');
      words.className = 'live-chapter-words';
      var label = document.createElement('span');
      label.className = 'live-chapter-label';
      linkify(label, c.label, true);
      words.appendChild(label);
      if (c.line) {
        var line = document.createElement('span');
        line.className = 'live-chapter-line';
        linkify(line, c.line, true);
        words.appendChild(line);
      }
      if (c.mini) {
        c.mini.classList.add('live-chapter-mini');
        b.appendChild(c.mini);
      }
      b.appendChild(words);
      b.appendChild(mark('live-chapter-mark'));
      head.appendChild(b);
      var panel = document.createElement('div');
      panel.className = 'live-chapter-body';
      panel.hidden = true;
      panel.appendChild(c.body);
      sec.appendChild(head);
      sec.appendChild(panel);
      box.appendChild(sec);
      var it = { id: c.id, label: c.label, el: sec, button: b, c: c };
      it.fold = fold(b, panel, {
        onChange: function (on) {
          if (on) {
            others(it);
            if (c.onOpen) c.onOpen();
          } else if (c.onClose) {
            c.onClose();
          }
        }
      });
      return it;
    });

    function find(id) {
      for (var i = 0; i < items.length; i++) if (items[i].id === id) return items[i];
      return null;
    }

    // Close the rest at once, and keep the chosen chapter's button where
    // it was on the screen (the browser may already have done so).
    function others(it) {
      var before = it.button.getBoundingClientRect().top;
      items.forEach(function (o) {
        if (o !== it && o.fold.isOpen()) o.fold.close(true);
      });
      var shift = it.button.getBoundingClientRect().top - before;
      if (Math.abs(shift) > 1) window.scrollBy(0, shift);
    }

    function open(id, bring, quick) {
      var it = find(id);
      if (!it) return false;
      it.fold.open(quick);
      if (bring) it.el.scrollIntoView({ block: 'start', behavior: still() || quick ? 'auto' : 'smooth' });
      return true;
    }

    function fromHash(bring, quick) {
      var id = decodeURIComponent((location.hash || '').slice(1));
      return id && find(id) ? open(id, bring, quick) : false;
    }

    // Links to a chapter, wherever they are: open it, and say so in the
    // address without the browser's own jump.
    function onClick(e) {
      var a = e.target.closest && e.target.closest('a[href^="#"]');
      if (!a || e.defaultPrevented || e.button > 0 || e.metaKey || e.ctrlKey || e.shiftKey) return;
      var id = decodeURIComponent(a.getAttribute('href').slice(1));
      if (!find(id)) return;
      e.preventDefault();
      if (history.replaceState) history.replaceState(null, '', '#' + id);
      open(id, true);
    }
    function onHash() { fromHash(true); }
    document.addEventListener('click', onClick);
    window.addEventListener('hashchange', onHash);

    return {
      list: items,
      open: open,
      follow: function () { return fromHash(true, true); },
      opened: function () {
        for (var i = 0; i < items.length; i++) if (items[i].fold.isOpen()) return items[i].id;
        return null;
      },
      destroy: function () {
        document.removeEventListener('click', onClick);
        window.removeEventListener('hashchange', onHash);
      }
    };
  }

  // ---- One of several ---------------------------------------------------------
  // Buttons of which one is chosen at a time, each showing its own words
  // in one panel (the tabs pattern): the chosen one is aria-selected and
  // alone in the tab order; arrow keys, Home and End move between them.
  // Choosing calls opts.onSelect(i), which fills the panel. The buttons'
  // parent is the tablist. Returns { select(i), current() }.
  function tabs(buttons, panel, opts) {
    opts = opts || {};
    var cur = -1;
    if (!panel.id) panel.id = uid('live-panel');
    panel.setAttribute('role', 'tabpanel');
    if (buttons[0] && buttons[0].parentNode) buttons[0].parentNode.setAttribute('role', 'tablist');
    buttons.forEach(function (b, i) {
      if (!b.id) b.id = uid('live-tab');
      b.setAttribute('role', 'tab');
      b.setAttribute('aria-controls', panel.id);
      b.setAttribute('aria-selected', 'false');
      b.tabIndex = i === 0 ? 0 : -1;
      b.addEventListener('click', function () { select(i, true); });
      b.addEventListener('keydown', function (e) {
        var n = buttons.length;
        var j = i;
        if (e.key === 'ArrowRight' || e.key === 'ArrowDown') j = (i + 1) % n;
        else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') j = (i - 1 + n) % n;
        else if (e.key === 'Home') j = 0;
        else if (e.key === 'End') j = n - 1;
        else return;
        e.preventDefault();
        buttons[j].focus();
        select(j, true);
      });
    });

    function select(i, byUser) {
      if (!buttons[i]) return;
      cur = i;
      buttons.forEach(function (b, j) {
        b.setAttribute('aria-selected', j === i ? 'true' : 'false');
        b.tabIndex = j === i ? 0 : -1;
        b.classList.toggle('is-current', j === i);
      });
      panel.setAttribute('aria-labelledby', buttons[i].id);
      if (opts.onSelect) opts.onSelect(i, !!byUser);
    }

    return { select: select, current: function () { return cur; } };
  }

  // ---- A dial -----------------------------------------------------------------
  // An hour drawn as a dial: n stations evenly round the rim, numbered
  // clockwise from the top, an arc filling from the first round to the
  // chosen one and a hand pointing at it. Each station is a button at
  // least 44px across, one of several (tabs); previous and next buttons
  // sit beneath. opts.onSelect(i) fills the panel, opts.name names the
  // dial and opts.label(i) a station, for anyone who can't see it.
  // Returns { el, select(i) }.
  var RIM = 80;

  function dial(n, panel, opts) {
    opts = opts || {};
    var box = document.createElement('div');
    box.className = 'live-dial';
    var face = document.createElement('div');
    face.className = 'live-dial-face';
    var art = svg('svg', 'live-dial-art');
    art.setAttribute('viewBox', '0 0 200 200');
    art.setAttribute('aria-hidden', 'true');
    art.setAttribute('focusable', 'false');
    var rim = svg('circle', 'live-dial-rim');
    rim.setAttribute('cx', '100');
    rim.setAttribute('cy', '100');
    rim.setAttribute('r', String(RIM));
    art.appendChild(rim);
    // The arc: the whole rim from the top, clockwise, shown as far as the
    // chosen station by its dash.
    var arc = svg('path', 'live-dial-arc');
    arc.setAttribute('d', 'M100 ' + (100 - RIM) + ' A' + RIM + ' ' + RIM + ' 0 1 1 99.99 ' + (100 - RIM));
    arc.setAttribute('pathLength', '1');
    art.appendChild(arc);
    var hand = svg('line', 'live-dial-hand');
    hand.setAttribute('x1', '100');
    hand.setAttribute('y1', '100');
    hand.setAttribute('x2', '100');
    hand.setAttribute('y2', String(100 - RIM + 26));
    art.appendChild(hand);
    var hub = svg('circle', 'live-dial-hub');
    hub.setAttribute('cx', '100');
    hub.setAttribute('cy', '100');
    hub.setAttribute('r', '4');
    art.appendChild(hub);
    face.appendChild(art);

    var ring = document.createElement('div');
    ring.className = 'live-dial-stations';
    if (opts.name) ring.setAttribute('aria-label', opts.name);
    var stations = [];
    for (var i = 0; i < n; i++) {
      var a = (-90 + i * 360 / n) * Math.PI / 180;
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'live-dial-station';
      b.style.left = (100 + RIM * Math.cos(a)) / 2 + '%';
      b.style.top = (100 + RIM * Math.sin(a)) / 2 + '%';
      var dot = document.createElement('span');
      dot.className = 'live-dial-dot';
      dot.textContent = String(i + 1);
      b.appendChild(dot);
      if (opts.label) b.setAttribute('aria-label', opts.label(i));
      ring.appendChild(b);
      stations.push(b);
    }
    face.appendChild(ring);
    box.appendChild(face);

    var steps = document.createElement('div');
    steps.className = 'live-dial-steps';
    var prev = document.createElement('button');
    prev.type = 'button';
    prev.className = 'live-step-button';
    prev.textContent = opts.prev || 'Previous';
    var next = document.createElement('button');
    next.type = 'button';
    next.className = 'live-step-button';
    next.textContent = opts.next || 'Next';
    steps.appendChild(prev);
    steps.appendChild(next);
    box.appendChild(steps);

    var t = tabs(stations, panel, {
      onSelect: function (i, byUser) {
        arc.style.strokeDashoffset = String(1 - i / n);
        hand.style.transform = 'rotate(' + i * 360 / n + 'deg)';
        stations.forEach(function (s, j) { s.classList.toggle('is-past', j < i); });
        prev.disabled = i === 0;
        next.disabled = i === n - 1;
        if (opts.onSelect) opts.onSelect(i, byUser);
      }
    });
    prev.addEventListener('click', function () { t.select(Math.max(0, t.current() - 1), true); });
    next.addEventListener('click', function () { t.select(Math.min(n - 1, t.current() + 1), true); });
    return { el: box, select: t.select };
  }

  // ---- Rows and tiles ---------------------------------------------------------
  // Each item shows only its label; tapping opens its words beneath it
  // (in place, for a tile). Several may be open at once. An item with no
  // words beyond its label is shown as the label alone.
  //   items: [{ label, body: element or null }]
  function folds(items, cls) {
    var list = document.createElement('div');
    list.className = 'live-' + cls + 's';
    items.forEach(function (it) {
      var row = document.createElement('div');
      row.className = 'live-' + cls;
      if (!it.body) {
        var only = document.createElement('p');
        only.className = 'live-' + cls + '-button';
        linkify(only, it.label);
        row.appendChild(only);
        list.appendChild(row);
        return;
      }
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'live-' + cls + '-button';
      var label = document.createElement('span');
      label.className = 'live-' + cls + '-label';
      linkify(label, it.label, true);
      b.appendChild(label);
      b.appendChild(mark());
      var panel = document.createElement('div');
      panel.className = 'live-' + cls + '-body';
      panel.hidden = true;
      panel.appendChild(it.body);
      row.appendChild(b);
      row.appendChild(panel);
      list.appendChild(row);
      fold(b, panel);
    });
    return list;
  }

  function rows(items) { return folds(items, 'row'); }
  function tiles(items) { return folds(items, 'tile'); }

  // ---- A swipe ----------------------------------------------------------------
  // A sideways swipe across el calls onLeft or onRight; an up-and-down one
  // is left to scroll the page.
  function swipe(el, onLeft, onRight) {
    var s = null;
    el.addEventListener('pointerdown', function (e) {
      if (e.pointerType === 'mouse') return;
      s = { x: e.clientX, y: e.clientY, t: Date.now(), id: e.pointerId };
    });
    function end(e) {
      if (!s || e.pointerId !== s.id) return;
      var dx = e.clientX - s.x;
      var dy = e.clientY - s.y;
      var quick = Date.now() - s.t < 900;
      s = null;
      if (e.type !== 'pointerup' || !quick || Math.abs(dx) < 40 || Math.abs(dx) < Math.abs(dy) * 1.5) return;
      if (dx < 0) onLeft();
      else onRight();
    }
    el.addEventListener('pointerup', end);
    el.addEventListener('pointercancel', end);
  }

  // ---- Counting up --------------------------------------------------------------
  // A number counted up from 0 into el over ms (about a second); simply
  // shown, for anyone who asks for less motion.
  function countUp(el, to, ms) {
    if (el.liveCount) cancelAnimationFrame(el.liveCount);
    el.liveCount = 0;
    if (still() || !(to > 0)) {
      el.textContent = String(to);
      return;
    }
    ms = ms || 1000;
    var start = null;
    function tick(ts) {
      if (start == null) start = ts;
      var k = Math.min(1, (ts - start) / ms);
      el.textContent = String(Math.round(to * (1 - Math.pow(1 - k, 3))));
      el.liveCount = k < 1 ? requestAnimationFrame(tick) : 0;
    }
    el.textContent = '0';
    el.liveCount = requestAnimationFrame(tick);
  }

  // ---- The bar ------------------------------------------------------------------
  // A slim bar fixed to the foot of the screen once opts.after has
  // scrolled out of view: a dot for each chapter (the one in view filled;
  // a tap goes there) and one action. It hides while any of opts.hideWhile
  // is in view, and while a field in opts.form has the keyboard. The page
  // gains bottom padding to match, so the bar never covers its last line.
  //   items: [{ label, el, go() }]   action: { label, href }
  // Returns { items(list) }.
  function bar(opts) {
    var el = document.createElement('nav');
    el.className = 'live-bar';
    if (opts.name) el.setAttribute('aria-label', opts.name);
    var inner = document.createElement('div');
    inner.className = 'live-bar-inner';
    var dots = document.createElement('div');
    dots.className = 'live-bar-dots';
    inner.appendChild(dots);
    if (opts.action) {
      var act = document.createElement('a');
      act.className = 'live-bar-action';
      act.href = opts.action.href;
      act.textContent = opts.action.label;
      inner.appendChild(act);
    }
    el.appendChild(inner);
    document.body.appendChild(el);
    document.body.classList.add('has-live-bar');

    var list = [];
    var buttons = [];
    var past = false;
    var blocked = {};
    var typing = false;
    var raf = 0;

    function sync() {
      var shown = past && !typing && list.length > 0 && !Object.keys(blocked).some(function (k) { return blocked[k]; });
      el.classList.toggle('is-shown', shown);
      if (shown) el.removeAttribute('inert');
      else el.setAttribute('inert', '');
    }

    // The chapter in view: the one across a line 40% down the screen.
    function where() {
      raf = 0;
      var line = window.innerHeight * 0.4;
      var cur = -1;
      list.forEach(function (it, i) {
        var r = it.el.getBoundingClientRect();
        if (r.top <= line && r.bottom > line) cur = i;
      });
      buttons.forEach(function (b, i) {
        b.classList.toggle('is-current', i === cur);
        if (i === cur) b.setAttribute('aria-current', 'true');
        else b.removeAttribute('aria-current');
      });
    }
    function soon() {
      if (!raf) raf = requestAnimationFrame(where);
    }
    window.addEventListener('scroll', soon, { passive: true });
    window.addEventListener('resize', soon);

    if (window.IntersectionObserver) {
      new IntersectionObserver(function (es) {
        var e = es[es.length - 1];
        past = !e.isIntersecting && e.boundingClientRect.bottom <= 0;
        sync();
      }).observe(opts.after);
      (opts.hideWhile || []).forEach(function (w, i) {
        new IntersectionObserver(function (es) {
          blocked[i] = es[es.length - 1].isIntersecting;
          sync();
        }).observe(w);
      });
    }
    if (opts.form) {
      opts.form.addEventListener('focusin', function (e) {
        if (/^(INPUT|TEXTAREA|SELECT)$/.test(e.target.tagName)) {
          typing = true;
          sync();
        }
      });
      opts.form.addEventListener('focusout', function () {
        typing = false;
        sync();
      });
    }

    return {
      el: el,
      items: function (next) {
        list = next || [];
        dots.textContent = '';
        buttons = list.map(function (it) {
          var b = document.createElement('button');
          b.type = 'button';
          b.className = 'live-bar-dot';
          b.setAttribute('aria-label', it.label);
          b.appendChild(document.createElement('span'));
          b.addEventListener('click', function () { it.go(); });
          dots.appendChild(b);
          return b;
        });
        sync();
        soon();
      }
    };
  }

  // ---- Small drawings -----------------------------------------------------------
  // A miniature of what is inside a chapter, in the page's colours, for
  // the eye only: 'steps' (seven dots in a row), 'dial', 'birds', 'orbit'
  // (a ringed centre with points round it) or 'bar' (two of seven).
  function mini(name) {
    var s = svg('svg', 'live-mini');
    s.setAttribute('viewBox', '0 0 40 40');
    s.setAttribute('aria-hidden', 'true');
    s.setAttribute('focusable', 'false');
    function add(tag, cls, at) {
      var e = svg(tag, cls);
      Object.keys(at).forEach(function (k) { e.setAttribute(k, at[k]); });
      s.appendChild(e);
      return e;
    }
    var i;
    if (name === 'steps') {
      add('line', 'live-mini-line', { x1: 4, y1: 20, x2: 36, y2: 20 });
      for (i = 0; i < 7; i++) add('circle', i === 0 ? 'live-mini-hot' : i === 1 ? 'live-mini-ring' : 'live-mini-dim', { cx: 4 + i * 5.33, cy: 20, r: i < 2 ? 2.4 : 2 });
    } else if (name === 'dial') {
      add('circle', 'live-mini-line', { cx: 20, cy: 20, r: 13, fill: 'none' });
      for (i = 0; i < 6; i++) {
        var a = (-90 + i * 60) * Math.PI / 180;
        add('circle', i < 3 ? 'live-mini-hot' : 'live-mini-dim', { cx: 20 + 13 * Math.cos(a), cy: 20 + 13 * Math.sin(a), r: 2.4 });
      }
      add('line', 'live-mini-hand', { x1: 20, y1: 20, x2: 27, y2: 16 });
    } else if (name === 'birds') {
      [[9, 15, 'live-mini-ink'], [21, 24, 'live-mini-hand'], [30, 13, 'live-mini-ink']].forEach(function (b) {
        add('path', b[2], { d: 'M' + (b[0] - 5) + ' ' + b[1] + ' q2.5 -3 5 0.8 q2.5 -3.8 5 -0.8', fill: 'none' });
      });
    } else if (name === 'orbit') {
      add('circle', 'live-mini-line', { cx: 20, cy: 20, r: 13, fill: 'none' });
      add('circle', 'live-mini-hot', { cx: 20, cy: 20, r: 4 });
      for (i = 0; i < 6; i++) {
        var b2 = (-90 + i * 60) * Math.PI / 180;
        add('circle', i % 3 === 1 ? 'live-mini-hot' : 'live-mini-ink-dot', { cx: 20 + 13 * Math.cos(b2), cy: 20 + 13 * Math.sin(b2), r: i % 3 === 1 ? 2.6 : 1.9 });
      }
    } else if (name === 'bar') {
      for (i = 0; i < 7; i++) add('rect', i < 2 ? 'live-mini-hot' : 'live-mini-dim', { x: 3 + i * 5, y: 17, width: 4, height: 6, rx: 0.8 });
    }
    return s;
  }

  // ---- A film -------------------------------------------------------------------
  // How a thing was made, played as a film across a small universe. The
  // model is read from a node of a spine, in the page's way of reading:
  //   its own line             "Title. Opening"
  //   at 1, the trail          moments, "HH:MM. words", each with, at 1, the
  //                            label of the body that gave, and at 2 the
  //                            label (or labels, comma-separated, in order)
  //                            of where it went
  //   at 2, who                the bodies, "Label. Body", each with its kind
  //                            ("Person", "Device", "AI" or "Place") at 4
  //   at 3, the conclusion
  // Bodies are matched by label exactly. A moment without passes, or naming
  // a label no body has, keeps its words and draws no arc.
  function nodeWords(n) {
    while (n && typeof n === 'object' && n._ && typeof n._ === 'object') n = n._;
    if (typeof n === 'string') return n.trim();
    return n && typeof n._ === 'string' ? n._.trim() : '';
  }

  function labelled(s) {
    var i = s.indexOf('. ');
    if (i < 0) return { label: s.replace(/\.$/, ''), body: '' };
    return { label: s.slice(0, i), body: s.slice(i + 2).trim() };
  }

  function items(n) {
    var out = [];
    if (!n || typeof n !== 'object') return out;
    for (var i = 1; i <= 9; i++) {
      var t = nodeWords(n[String(i)]);
      if (t) out.push({ node: n[String(i)], text: t });
    }
    return out;
  }

  // An accumulating node, oldest first ("_" first when older entries have
  // been nested there).
  function accumulated(n, out) {
    out = out || [];
    if (!n || typeof n !== 'object') return out;
    if (n._ && typeof n._ === 'object') accumulated(n._, out);
    return out.concat(items(n));
  }

  function filmModel(node) {
    var open = labelled(nodeWords(node));
    var bodies = items(node && node['2']).map(function (c) {
      var p = labelled(c.text);
      var kind = c.node && typeof c.node === 'object' ? nodeWords(c.node['4']) : '';
      return { label: p.label, body: p.body, kind: kind };
    });
    var at = Object.create(null);
    bodies.forEach(function (b, i) {
      if (!(b.label in at)) at[b.label] = i;
    });
    var day = 0;
    var last = null;
    var moments = accumulated(node && node['1']).map(function (c, order) {
      var p = labelled(c.text);
      var pass = c.node && typeof c.node === 'object' ? c.node : {};
      var from = nodeWords(pass['1']);
      var to = nodeWords(pass['2']).split(',').map(function (x) { return x.trim(); }).filter(Boolean);
      var chain = from && to.length ? [from].concat(to) : [];
      var known = chain.every(function (l) { return l in at; });
      var hops = [];
      if (known) for (var h = 0; h + 1 < chain.length; h++) hops.push([at[chain[h]], at[chain[h + 1]]]);
      // A time earlier than the one before by more than half a day has
      // passed midnight; a moment without a time keeps its place.
      var t = /^(\d{1,2}):(\d{2})$/.exec(p.label);
      var mins = null;
      if (t) {
        mins = Number(t[1]) * 60 + Number(t[2]) + day;
        if (last != null && mins < last - 720) {
          day += 1440;
          mins += 1440;
        }
        last = mins;
      }
      return { time: p.label, words: p.body || (t ? '' : p.label), at: mins, order: order, hops: hops };
    });
    var sortAt = null;
    moments.forEach(function (m) {
      if (m.at != null) sortAt = m.at;
      m.sort = m.at != null ? m.at : sortAt == null ? -1 : sortAt;
    });
    moments.sort(function (a, b) { return a.sort - b.sort || a.order - b.order; });
    return {
      title: open.label,
      opening: open.body || open.label,
      trail: nodeWords(node && node['1']),
      who: nodeWords(node && node['2']),
      bodies: bodies,
      moments: moments,
      close: nodeWords(node && node['3'])
    };
  }

  // Live.film(root, opts) draws the model in root: a square stage in the
  // dusk hand (plum sky, a faint ember glow low down, a few still stars,
  // cream ink, the page's pink for people), the body of kind "Place" at
  // the centre and the rest on a slowly drifting ring round it; under it
  // the caption, a panel for a tapped body, the controls and the line of
  // time. The scenes: s1 the bodies arrive; one scene for each moment, in
  // time order, a spark carrying it from giver to receiver hop by hop
  // (arcs travelled stay faintly lit); the last, the whole constellation
  // glowing, with a small named star for each who has responded and
  // something to do.
  //   opts.model      from Live.filmModel
  //   opts.respond    { label, href } for the close
  //   opts.star(name) what a responder's star does (else opts.starHref)
  //   opts.onScene(n) hears each change of scene
  //   opts.labels     { back, next, play, stop, present, leave, line }
  // Returns { go(n), next(), back(), play(), stop(), responders(names),
  // count, current(), destroy() }.
  var FILM = { mid: 500, cy: 450, ring: 236, hop: 0.9, scene: 3000, drift: 0.8 };
  var KINDS = {
    Person: { r: 26, cls: 'person' },
    AI: { r: 17, cls: 'ai' },
    Device: { r: 10, cls: 'device' },
    Place: { r: 34, cls: 'place' }
  };

  function film(root, opts) {
    opts = opts || {};
    var model = opts.model;
    var L = opts.labels || {};
    var G = window.gsap || null;
    var id = uid('live-film');
    var scenes = [{ kind: 'open' }].concat(model.moments.map(function (m, k) {
      return { kind: 'moment', m: m, k: k };
    }), [{ kind: 'close' }]);
    var total = scenes.length;
    var cur = 0;
    var tl = null;
    var ring = 0;
    var active = false;
    var onScreen = !window.IntersectionObserver;
    var picked = -1;
    var timer = 0;
    var playing = false;

    function moving() {
      return !!G && !still();
    }
    function el(tag, cls, text) {
      var e = document.createElement(tag);
      if (cls) e.className = cls;
      if (text != null) e.textContent = text;
      return e;
    }
    function node(tag, cls, at, parent) {
      var e = svg(tag, cls);
      Object.keys(at || {}).forEach(function (k) { e.setAttribute(k, at[k]); });
      if (parent) parent.appendChild(e);
      return e;
    }

    root.classList.add('live-film');
    root.textContent = '';
    var stage = el('div', 'live-film-stage live-noprint');
    var art = node('svg', 'live-film-art', { viewBox: '0 0 1000 1000', role: 'group', 'aria-label': model.title || '' });
    stage.appendChild(art);
    var defs = node('defs', null, {}, art);
    defs.innerHTML = '' +
      '<linearGradient id="' + id + '-sky" x1="0" y1="0" x2="0" y2="1">' +
        '<stop offset="0" stop-color="#170b1d"/><stop offset="0.55" stop-color="#2a1131"/><stop offset="1" stop-color="#3c1437"/></linearGradient>' +
      '<radialGradient id="' + id + '-ember" cx="0.5" cy="1" r="0.62">' +
        '<stop offset="0" stop-color="#ff9a6b" stop-opacity="0.34"/><stop offset="0.45" stop-color="#e0566f" stop-opacity="0.12"/><stop offset="1" stop-color="#e0566f" stop-opacity="0"/></radialGradient>' +
      '<radialGradient id="' + id + '-soft">' +
        '<stop offset="0" stop-color="#ffe9d6" stop-opacity="0.55"/><stop offset="0.35" stop-color="#f2b58f" stop-opacity="0.22"/><stop offset="1" stop-color="#f2b58f" stop-opacity="0"/></radialGradient>' +
      '<radialGradient id="' + id + '-pink">' +
        '<stop offset="0" stop-color="#ff8fb0" stop-opacity="0.6"/><stop offset="1" stop-color="#ff8fb0" stop-opacity="0"/></radialGradient>' +
      '<radialGradient id="' + id + '-spark">' +
        '<stop offset="0" stop-color="#fffaf0" stop-opacity="1"/><stop offset="0.3" stop-color="#ffe2c4" stop-opacity="0.7"/><stop offset="1" stop-color="#ffe2c4" stop-opacity="0"/></radialGradient>';
    node('rect', null, { x: 0, y: 0, width: 1000, height: 1000, fill: 'url(#' + id + '-sky)' }, art);
    node('rect', null, { x: 0, y: 0, width: 1000, height: 1000, fill: 'url(#' + id + '-ember)' }, art);
    // A few still stars, the same each time.
    var starRand = chance(hash(model.title || 'film'));
    var still0 = node('g', 'live-film-sky', {}, art);
    for (var s = 0; s < 34; s++) {
      node('circle', null, { cx: (starRand() * 980 + 10).toFixed(1), cy: (starRand() * 640 + 10).toFixed(1), r: (0.8 + starRand() * 1.7).toFixed(2), opacity: (0.25 + starRand() * 0.5).toFixed(2) }, still0);
    }
    var glow = node('circle', null, { cx: FILM.mid, cy: FILM.cy, r: 430, fill: 'url(#' + id + '-soft)', opacity: 0 }, art);
    var arcsG = node('g', 'live-film-arcs', {}, art);
    var sparkG = node('g', 'live-film-spark', { opacity: 0 }, art);
    node('circle', null, { r: 30, fill: 'url(#' + id + '-spark)' }, sparkG);
    node('circle', null, { r: 7, fill: '#fffaf0' }, sparkG);
    var bodiesG = node('g', 'live-film-bodies', {}, art);
    var starsG = node('g', 'live-film-stars', {}, art);
    var clock = el('div', 'live-film-clock');
    clock.setAttribute('aria-hidden', 'true');
    var counter = el('div', 'live-film-count');
    counter.setAttribute('aria-hidden', 'true');
    stage.appendChild(clock);
    stage.appendChild(counter);
    root.appendChild(stage);

    var caption = el('div', 'live-film-caption live-noprint');
    caption.setAttribute('aria-live', 'polite');
    root.appendChild(caption);
    var panel = el('div', 'live-film-panel live-noprint');
    panel.hidden = true;
    root.appendChild(panel);
    var controls = el('div', 'live-film-controls live-noprint');
    function button(text) {
      var b = el('button', 'live-step-button', text);
      b.type = 'button';
      controls.appendChild(b);
      return b;
    }
    var backB = button(L.back || 'Back');
    var nextB = button(L.next || 'Next');
    var playB = button(L.play || 'Play');
    playB.setAttribute('aria-pressed', 'false');
    var showB = button(L.present || 'Present');
    showB.setAttribute('aria-pressed', 'false');
    root.appendChild(controls);

    // The bodies: the first of kind "Place" at the centre, the rest round
    // the ring in block order.
    var centre = -1;
    model.bodies.forEach(function (b, i) {
      if (centre < 0 && b.kind === 'Place') centre = i;
    });
    var onRing = model.bodies.map(function (b, i) { return i; }).filter(function (i) { return i !== centre; });
    var bodies = model.bodies.map(function (b, i) {
      var k = KINDS[b.kind] || { r: 13, cls: 'neutral' };
      var g = node('g', 'live-film-body is-' + k.cls, { tabindex: '0', role: 'button', 'aria-label': b.label + (b.kind ? ', ' + b.kind : '') }, bodiesG);
      // Something to tap, never under 44px on a phone's stage; the dot
      // swells as the spark reaches it; the label stays as it is.
      node('circle', 'live-film-hit', { r: Math.max(72, k.r + 30) }, g);
      var dot = node('g', 'live-film-dot', {}, g);
      if (k.cls === 'place') node('circle', 'live-film-halo', { r: 150, fill: 'url(#' + id + '-soft)' }, dot);
      if (k.cls === 'person') node('circle', 'live-film-halo', { r: 58, fill: 'url(#' + id + '-pink)' }, dot);
      node('circle', 'live-film-core', { r: k.r }, dot);
      var t = node('text', 'live-film-label', {}, g);
      t.textContent = b.label;
      var at = onRing.indexOf(i);
      return { i: i, g: g, dot: dot, text: t, r: k.r, angle: at < 0 ? null : -90 + at * 360 / Math.max(1, onRing.length), appear: 0, flash: 0, x: FILM.mid, y: FILM.cy };
    });

    // The arcs: one for each hop of each moment, bending more for a pair
    // already joined the same way, so a return or a repeat stays apart.
    var bends = {};
    var arcs = [];
    model.moments.forEach(function (m, k) {
      m.hops.forEach(function (hop, h) {
        if (hop[0] === hop[1]) return;
        var key = hop[0] + '>' + hop[1];
        bends[key] = (bends[key] || 0) + 1;
        var g = node('g', 'live-film-arc', {}, arcsG);
        var hit = node('path', 'live-film-arc-hit', {}, g);
        var line = node('path', 'live-film-arc-line', { pathLength: '1' }, g);
        arcs.push({ k: k, h: h, a: hop[0], b: hop[1], bend: 0.22 + 0.12 * (bends[key] - 1), g: g, hit: hit, line: line, draw: 0, op: 0 });
        g.addEventListener('click', function (e) {
          if (dragged) return;
          stop();
          go(nearestArc(e, k) + 2);
        });
      });
    });
    // Where tap areas overlap, the tap goes to the arc whose drawn line
    // is nearest the finger, not merely the one drawn last.
    function nearestArc(e, k) {
      if (!document.elementsFromPoint || !e.clientX && !e.clientY) return k;
      var best = k;
      var near = Infinity;
      document.elementsFromPoint(e.clientX, e.clientY).forEach(function (n) {
        arcs.forEach(function (arc) {
          if (arc.hit !== n) return;
          var len = n.getTotalLength();
          var m = n.getScreenCTM();
          if (!len || !m) return;
          for (var j = 0; j <= 40; j++) {
            var pt = n.getPointAtLength(len * j / 40);
            var dx = pt.x * m.a + pt.y * m.c + m.e - e.clientX;
            var dy = pt.x * m.b + pt.y * m.d + m.f - e.clientY;
            var d = dx * dx + dy * dy;
            if (d < near) {
              near = d;
              best = arc.k;
            }
          }
        });
      });
      return best;
    }
    var spark = { on: 0, p: 0, k: -1 };
    var lit = { v: 0 };

    // Responders: small named stars in an arc below the ring.
    var stars = [];
    function layoutStars() {
      var n = stars.length;
      stars.forEach(function (st, j) {
        var f = n === 1 ? 0.5 : j / (n - 1);
        var spread = Math.min(820, 260 * (n - 1));
        st.x = FILM.mid - spread / 2 + f * spread;
        st.y = 900 - Math.sin(f * Math.PI) * 30;
        st.g.setAttribute('transform', 'translate(' + st.x.toFixed(1) + ',' + st.y.toFixed(1) + ')');
        st.text.style.display = n > 8 ? 'none' : '';
      });
    }
    function responders(names) {
      var seen = {};
      (names || []).forEach(function (name) {
        if (!name || seen[name]) return;
        seen[name] = true;
        for (var j = 0; j < stars.length; j++) if (stars[j].name === name) return;
        var link = !opts.star && opts.starHref;
        var g = node(link ? 'a' : 'g', 'live-film-star', link ? { href: opts.starHref } : { tabindex: '0', role: 'button' }, starsG);
        g.setAttribute('aria-label', name);
        node('circle', 'live-film-hit', { r: 60 }, g);
        node('circle', 'live-film-star-glow', { r: 16, fill: 'url(#' + id + '-soft)' }, g);
        node('path', 'live-film-star-core', { d: 'M0 -9 L2.2 -2.2 L9 0 L2.2 2.2 L0 9 L-2.2 2.2 L-9 0 L-2.2 -2.2 Z' }, g);
        var t = node('text', 'live-film-star-label', { y: 40 }, g);
        // "name, a visitor" is shown on two lines.
        var parts = name.split(/,\s*/);
        t.textContent = '';
        parts.slice(0, 2).forEach(function (p, i) {
          var span = node('tspan', i ? 'live-film-star-also' : null, { x: 0, dy: i ? 30 : 0 }, t);
          span.textContent = i ? parts.slice(1).join(', ') : p;
        });
        var st = { name: name, g: g, text: t, appear: 0, x: 0, y: 0 };
        if (!link) {
          var act = function () { if (opts.star) opts.star(name); };
          g.addEventListener('click', function () { if (!dragged) act(); });
          g.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              act();
            }
          });
        }
        stars.push(st);
        // A new responder fades in while the close is showing.
        if (scenes[cur] && scenes[cur].kind === 'close') {
          if (moving()) G.to(st, { appear: 1, duration: 1.2, ease: 'power2.out', onUpdate: render });
          else st.appear = 1;
        }
      });
      layoutStars();
      render();
    }

    // Where everything is, and how it looks, each frame.
    function place(b) {
      if (b.angle == null) {
        b.x = FILM.mid;
        b.y = FILM.cy;
        return;
      }
      var a = (b.angle + ring) * Math.PI / 180;
      b.x = FILM.mid + FILM.ring * Math.cos(a);
      b.y = FILM.cy + FILM.ring * Math.sin(a);
    }
    function curve(arc) {
      var p = bodies[arc.a];
      var q = bodies[arc.b];
      var dx = q.x - p.x;
      var dy = q.y - p.y;
      var len = Math.sqrt(dx * dx + dy * dy) || 1;
      return { x0: p.x, y0: p.y, cx: (p.x + q.x) / 2 - dy / len * len * arc.bend, cy: (p.y + q.y) / 2 + dx / len * len * arc.bend, x1: q.x, y1: q.y };
    }
    function render() {
      bodies.forEach(function (b) {
        place(b);
        b.g.setAttribute('transform', 'translate(' + b.x.toFixed(1) + ',' + b.y.toFixed(1) + ') scale(' + b.appear.toFixed(3) + ')');
        b.dot.setAttribute('transform', 'scale(' + (1 + b.flash * 0.4).toFixed(3) + ')');
        b.g.style.opacity = String(Math.min(1, b.appear));
        // Labels outside the ring, away from the centre; the centre's below.
        var t = b.text;
        var ox = 0;
        var oy = b.r + 34;
        var anchor = 'middle';
        if (b.angle != null) {
          var a = (b.angle + ring) * Math.PI / 180;
          var c = Math.cos(a);
          var s2 = Math.sin(a);
          ox = c * (b.r + 16);
          oy = s2 * (b.r + 16) + (Math.abs(c) < 0.35 ? (s2 < 0 ? -14 : 30) : 12);
          anchor = c > 0.35 ? 'start' : c < -0.35 ? 'end' : 'middle';
        }
        t.setAttribute('x', ox.toFixed(1));
        t.setAttribute('y', oy.toFixed(1));
        t.setAttribute('text-anchor', anchor);
        b.g.classList.toggle('is-picked', picked === b.i);
      });
      var sc0 = scenes[cur];
      arcs.forEach(function (arc) {
        var c = curve(arc);
        var d = 'M' + c.x0.toFixed(1) + ' ' + c.y0.toFixed(1) + ' Q' + c.cx.toFixed(1) + ' ' + c.cy.toFixed(1) + ' ' + c.x1.toFixed(1) + ' ' + c.y1.toFixed(1);
        arc.line.setAttribute('d', d);
        arc.hit.setAttribute('d', d);
        var draw = arc.draw;
        if (spark.k === arc.k && sc0 && sc0.kind === 'moment' && sc0.k === arc.k) draw = Math.max(0, Math.min(1, spark.p - arc.h));
        arc.line.style.strokeDashoffset = String(1 - draw);
        var near = picked >= 0 && (arc.a === picked || arc.b === picked);
        var op = arc.op * (near && arc.op > 0 ? 1.8 : 1);
        arc.g.style.opacity = String(Math.min(1, op));
        arc.g.classList.toggle('is-near', near);
        // The scene showing needs no jump, so its arcs let a tap through
        // to the travelled ones beneath them.
        var showing = sc0 && sc0.kind === 'moment' && sc0.k === arc.k;
        var tappable = arc.op > 0.05 && draw > 0.5 && !showing;
        arc.g.style.pointerEvents = tappable ? '' : 'none';
        arc.hit.style.pointerEvents = tappable ? '' : 'none';
      });
      var sk = scenes[cur];
      if (spark.on > 0 && sk && sk.kind === 'moment') {
        var hops = arcs.filter(function (x) { return x.k === sk.k; });
        var h = Math.min(hops.length - 1, Math.floor(spark.p));
        var arc = hops[h];
        if (arc) {
          var c2 = curve(arc);
          var t2 = Math.max(0, Math.min(1, spark.p - h));
          var u = 1 - t2;
          var x = u * u * c2.x0 + 2 * u * t2 * c2.cx + t2 * t2 * c2.x1;
          var y = u * u * c2.y0 + 2 * u * t2 * c2.cy + t2 * t2 * c2.y1;
          sparkG.setAttribute('transform', 'translate(' + x.toFixed(1) + ',' + y.toFixed(1) + ')');
        }
      }
      sparkG.style.opacity = String(spark.on);
      glow.style.opacity = String(lit.v);
      stars.forEach(function (st) {
        st.g.style.opacity = String(st.appear);
        st.g.style.pointerEvents = st.appear > 0.5 ? '' : 'none';
      });
    }

    // The ring drifts very slowly while the film is on screen.
    function tick(time, delta) {
      if (!still() && !dragging) ring += (delta / 1000) * FILM.drift;
      render();
    }
    function setActive(on) {
      if (on === active) return;
      active = on;
      if (G) {
        if (on) G.ticker.add(tick);
        else G.ticker.remove(tick);
      }
      if (tl) {
        if (on) tl.resume();
        else tl.pause();
      }
      if (playing) {
        if (on) schedule();
        else clearTimeout(timer);
      }
    }
    function wake() {
      setActive(onScreen && !document.hidden && root.getClientRects().length > 0);
    }

    function words(text, strongLabel) {
      var p = el('p', 'live-film-words');
      if (strongLabel) {
        var lw = labelled(text);
        var b = el('strong', null, lw.label + (lw.body ? '.' : ''));
        p.appendChild(b);
        if (lw.body) linkify(p, ' ' + lw.body);
      } else {
        linkify(p, text);
      }
      return p;
    }

    function go(n, how) {
      n = Math.max(1, Math.min(total, n | 0 || 1));
      var from = cur;
      cur = n - 1;
      if (tl) tl.kill();
      tl = null;
      var sc = scenes[cur];
      var anim = moving() && how !== 'quiet';
      // Caption, clock, count.
      caption.textContent = '';
      if (sc.kind === 'open') caption.appendChild(words(model.opening));
      else if (sc.kind === 'moment') caption.appendChild(words(sc.m.words || sc.m.time));
      else {
        caption.appendChild(words(model.close, true));
        if (opts.respond) {
          var a = el('a', 'live-film-respond', opts.respond.label);
          a.href = opts.respond.href;
          caption.appendChild(a);
        }
      }
      clock.textContent = sc.kind === 'moment' ? sc.m.time : '';
      counter.textContent = n + ' / ' + total;
      backB.disabled = n === 1;
      nextB.disabled = n === total;
      if (line) line.select(sc.kind === 'moment' ? sc.k : -1);
      root.setAttribute('data-scene', String(n));

      // Bodies: in the opening they arrive one by one.
      if (anim) {
        tl = G.timeline({ onUpdate: render });
        if (sc.kind === 'open') {
          bodies.forEach(function (b) { b.appear = 0; });
          tl.to(bodies, { appear: 1, duration: 0.7, ease: 'back.out(1.6)', stagger: 0.32 }, 0.15);
        } else {
          tl.to(bodies, { appear: 1, duration: 0.35 }, 0);
        }
      } else {
        bodies.forEach(function (b) { b.appear = 1; });
      }
      // Arcs: travelled ones faintly lit; this moment's carried by the
      // spark, hop by hop; all of them glowing at the close.
      arcs.forEach(function (arc) {
        var op;
        var draw;
        if (sc.kind === 'close') {
          op = 0.6;
          draw = 1;
        } else if (sc.kind === 'moment' && arc.k < sc.k) {
          op = 0.26;
          draw = 1;
        } else if (sc.kind === 'moment' && arc.k === sc.k) {
          op = 0.95;
          draw = anim ? 0 : 1;
        } else {
          op = 0;
          draw = 0;
        }
        arc.draw = draw;
        if (anim) tl.to(arc, { op: op, duration: 0.4 }, 0);
        else arc.op = op;
      });
      spark.k = -1;
      spark.on = 0;
      if (sc.kind === 'moment' && anim) {
        var hops = arcs.filter(function (x) { return x.k === sc.k; });
        if (hops.length) {
          var per = Math.min(FILM.hop, 2.4 / hops.length);
          spark.k = sc.k;
          spark.p = 0;
          tl.to(spark, { on: 1, duration: 0.2 }, 0.25);
          hops.forEach(function (arc, h) {
            tl.to(spark, { p: h + 1, duration: per, ease: 'power1.inOut' }, 0.3 + h * per);
            tl.fromTo(bodies[arc.b], { flash: 1 }, { flash: 0, duration: 0.7, ease: 'power2.out' }, 0.3 + (h + 1) * per - 0.05);
          });
          tl.to(spark, { on: 0, duration: 0.35 }, 0.3 + hops.length * per);
          tl.add(function () { hops.forEach(function (arc) { arc.draw = 1; }); spark.k = -1; });
        }
      }
      // The close: the constellation glows; responders' stars come out.
      var closing = sc.kind === 'close';
      if (anim) {
        tl.to(lit, { v: closing ? 1 : 0, duration: closing ? 1.4 : 0.4 }, 0);
        tl.to(stars, { appear: closing ? 1 : 0, duration: closing ? 0.9 : 0.3, stagger: closing ? 0.25 : 0 }, closing ? 0.6 : 0);
      } else {
        lit.v = closing ? 1 : 0;
        stars.forEach(function (st) { st.appear = closing ? 1 : 0; });
      }
      render();
      if (tl && !active) tl.pause();
      if (opts.onScene && from !== cur) opts.onScene(n);
    }

    // Playing: about three seconds a scene; any tap or key stops it.
    function schedule() {
      clearTimeout(timer);
      timer = setTimeout(function () {
        if (!playing) return;
        if (cur >= total - 1) return stop();
        go(cur + 2);
        schedule();
      }, FILM.scene);
    }
    function play() {
      if (playing) return;
      playing = true;
      playB.textContent = L.stop || 'Stop';
      playB.setAttribute('aria-pressed', 'true');
      if (cur >= total - 1) go(1);
      if (active) schedule();
    }
    function stop() {
      if (!playing) return false;
      playing = false;
      clearTimeout(timer);
      playB.textContent = L.play || 'Play';
      playB.setAttribute('aria-pressed', 'false');
      return true;
    }
    function halt(e) {
      if (!playing) return;
      if (e.target === playB || (e.target.closest && e.target.closest('button') === playB)) return;
      stop();
    }
    document.addEventListener('pointerdown', halt, true);
    document.addEventListener('keydown', halt, true);

    backB.addEventListener('click', function () { go(cur); });
    nextB.addEventListener('click', function () { go(cur + 2); });
    playB.addEventListener('click', function () {
      if (playing) stop();
      else play();
    });

    // Present: the film alone, filling the screen.
    function presenting() {
      return document.fullscreenElement === root || document.webkitFullscreenElement === root || root.classList.contains('is-presenting');
    }
    function syncPresent() {
      var on = presenting();
      showB.textContent = on ? (L.leave || 'Leave') : (L.present || 'Present');
      showB.setAttribute('aria-pressed', on ? 'true' : 'false');
      root.classList.toggle('is-full', on);
    }
    showB.addEventListener('click', function () {
      if (presenting()) {
        if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen();
        else if (document.webkitFullscreenElement && document.webkitExitFullscreen) document.webkitExitFullscreen();
        root.classList.remove('is-presenting');
        syncPresent();
        return;
      }
      var req = root.requestFullscreen || root.webkitRequestFullscreen;
      var p = null;
      try {
        p = req ? req.call(root) : null;
      } catch (err) {
        p = null;
      }
      if (!req) {
        root.classList.add('is-presenting');
        syncPresent();
      } else if (p && p.catch) {
        p.catch(function () {
          root.classList.add('is-presenting');
          syncPresent();
        });
      }
    });
    document.addEventListener('fullscreenchange', syncPresent);
    document.addEventListener('webkitfullscreenchange', syncPresent);

    // Arrow keys step the scenes (the line of time keeps its own); Escape
    // leaves presenting.
    function keys(e) {
      if (e.defaultPrevented) return;
      var inLine = e.target.closest && e.target.closest('[role="toolbar"]');
      if (!inLine && e.key === 'ArrowRight') {
        e.preventDefault();
        go(cur + 2);
      } else if (!inLine && e.key === 'ArrowLeft') {
        e.preventDefault();
        go(cur);
      } else if (e.key === 'Escape' && root.classList.contains('is-presenting')) {
        root.classList.remove('is-presenting');
        syncPresent();
      }
    }
    root.addEventListener('keydown', keys);
    function keysAnywhere(e) {
      if (presenting() && !root.contains(e.target)) keys(e);
    }
    document.addEventListener('keydown', keysAnywhere);
    swipe(caption, function () { go(cur + 2); }, function () { go(cur); });

    // A tap on a body opens its panel and brightens its arcs; a sideways
    // drag on the sky turns the ring (up and down is left to scroll).
    var dragging = false;
    var dragged = false;
    var grip = null;
    art.addEventListener('pointerdown', function (e) {
      grip = { x: e.clientX, y: e.clientY, id: e.pointerId, turned: false };
      dragged = false;
    });
    art.addEventListener('pointermove', function (e) {
      if (!grip || e.pointerId !== grip.id) return;
      var dx = e.clientX - grip.x;
      var dy = e.clientY - grip.y;
      if (!grip.turned && Math.abs(dx) > 8 && Math.abs(dx) > Math.abs(dy)) grip.turned = true;
      if (grip.turned) {
        dragging = dragged = true;
        var w = art.getBoundingClientRect().width || 1;
        ring += dx / w * 180;
        grip.x = e.clientX;
        grip.y = e.clientY;
        render();
      }
    });
    function letGo() {
      grip = null;
      dragging = false;
    }
    art.addEventListener('pointerup', letGo);
    art.addEventListener('pointercancel', letGo);

    function pick(i) {
      picked = picked === i ? -1 : i;
      panel.textContent = '';
      panel.hidden = picked < 0;
      if (picked >= 0) {
        var b = model.bodies[picked];
        panel.appendChild(el('h3', 'live-film-panel-title', b.label));
        if (b.kind) panel.appendChild(el('span', 'tag live-film-kind', b.kind));
        if (b.body) panel.appendChild(linkify(el('p', 'live-film-panel-body'), b.body));
      }
      render();
    }
    bodies.forEach(function (b) {
      b.g.addEventListener('click', function (e) {
        e.stopPropagation();
        if (!dragged) pick(b.i);
      });
      b.g.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          pick(b.i);
        }
      });
    });
    art.addEventListener('click', function (e) {
      if (dragged || e.target.closest('.live-film-body, .live-film-arc, .live-film-star')) return;
      if (picked >= 0) pick(picked);
    });

    // The line of time, in step with the scenes.
    var marks = [];
    model.moments.forEach(function (m, k) {
      if (m.at != null) marks.push({ label: m.time, at: m.at, i: k });
    });
    var line = marks.length ? timeline(marks, {
      name: L.line || model.trail || '',
      onPick: function (k) {
        stop();
        go(k + 2);
      }
    }) : null;
    if (line) {
      line.el.classList.add('live-film-line', 'live-noprint');
      root.appendChild(line.el);
    }

    var io = null;
    if (window.IntersectionObserver) {
      io = new IntersectionObserver(function (es) {
        onScreen = es[es.length - 1].isIntersecting;
        wake();
      });
      io.observe(stage);
    }
    document.addEventListener('visibilitychange', wake);
    var ro = window.ResizeObserver ? new ResizeObserver(wake) : null;
    if (ro) ro.observe(root);

    go(1, 'quiet');
    wake();

    return {
      count: total,
      go: function (n) {
        stop();
        go(n);
      },
      next: function () { go(cur + 2); },
      back: function () { go(cur); },
      play: play,
      stop: stop,
      current: function () { return cur + 1; },
      responders: responders,
      destroy: function () {
        stop();
        setActive(false);
        if (tl) tl.kill();
        if (io) io.disconnect();
        if (ro) ro.disconnect();
        if (line) line.destroy();
        document.removeEventListener('pointerdown', halt, true);
        document.removeEventListener('keydown', halt, true);
        document.removeEventListener('keydown', keysAnywhere);
        document.removeEventListener('visibilitychange', wake);
        document.removeEventListener('fullscreenchange', syncPresent);
        document.removeEventListener('webkitfullscreenchange', syncPresent);
      }
    };
  }

  // ---- A scene ------------------------------------------------------------
  // A sky at dusk over water, with a flock in it: a cream bird for each
  // thing that has just moved, and a pink one for the reader, which flies
  // in from the edge as the page opens. The sky is the element's own
  // background (live.css), so it is there before any of this runs; the
  // canvas laid over it carries the birds and the water's slow shimmer.
  //
  // Live.scene(box, opts):
  //   opts.caption   an element under the sky, for the words of a bird
  //   opts.say(b)    [what it is, how long ago, a title for the first],
  //                  for a bird of the flock ({ id, when })
  //   opts.me        the words for the reader's own bird
  //   opts.label(n)  the sky's name, for anyone who can't see it
  //   opts.horizon   where the sky meets the water, from the top (0.78)
  //
  // set(list) gives the flock, newest first, as [{ id, when, fresh }]. A
  // bird new to the sky flies in from the side; one marked fresh has just
  // moved, and the caption says the newest of those. Birds no longer in
  // the list fly off. ring(on) puts a small ring on the reader's bird.
  //
  // Pressing or dragging in the sky draws the birds within 140px towards
  // the finger; let go and they drift back into a loose flock. A mostly
  // vertical drag is left to scroll the page. Tapping a bird, or picking
  // one with the arrow keys, puts its words in the caption and rings it.
  // For anyone who asks for less motion the birds sit still and the water
  // lies flat, and taps still work. Nothing moves while the sky is off the
  // screen or the tab is hidden.
  var scenes = [];
  var CREAM = '#f2e7d4';
  var DUSK = '#1a0d20';
  var REACH = 140; // how far a pressed finger draws the birds, in pixels
  var TAP = 24; // how near a tap must come to a bird: a 48px target
  var GLOW = 2.5; // seconds a bird that has just moved shows it

  function hash(s) {
    var h = 2166136261;
    for (var i = 0; i < s.length; i++) {
      h ^= s.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    return h >>> 0;
  }

  // A small steady source of chance for each bird, from its name.
  function chance(seed) {
    return function () {
      seed = (seed + 0x6d2b79f5) | 0;
      var t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function colour(name, fallback) {
    var v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    return v || fallback;
  }

  // A hex colour taken part of the way towards another: the page's pink
  // lightened for a dark sky, as the page itself lightens it at night.
  function towards(hex, to, k) {
    var a = /^#([0-9a-f]{6})$/i.exec(hex);
    var b = /^#([0-9a-f]{6})$/i.exec(to);
    if (!a || !b) return hex;
    var out = '#';
    for (var i = 0; i < 6; i += 2) {
      var x = parseInt(a[1].slice(i, i + 2), 16);
      var y = parseInt(b[1].slice(i, i + 2), 16);
      out += ('0' + Math.round(x + (y - x) * k).toString(16)).slice(-2);
    }
    return out;
  }

  function scene(box, opts) {
    opts = opts || {};
    var horizon = opts.horizon || 0.78;
    var say = opts.caption || null;
    var sky = document.createElement('canvas');
    sky.className = 'live-scene-sky';
    sky.tabIndex = 0;
    sky.setAttribute('role', 'img');
    box.insertBefore(sky, box.firstChild);
    var ctx = sky.getContext ? sky.getContext('2d') : null;
    if (!ctx) return { set: function () {}, ring: function () {} };

    var me = hatch({ id: '' }, true);
    me.me = true;
    me.enter = -1;
    me.delay = 0;
    var birds = [me];
    var order = [];
    var picked = null; // the bird last tapped, or picked by key
    var shown = null; // the bird the caption speaks of
    var ringed = false;
    var press = null; // where a finger or pointer is held in the sky
    var gesture = null;
    var W = 0;
    var H = 0;
    var dpr = 1;
    var time = 0;
    var running = false;
    var last = 0;
    var onScreen = !window.IntersectionObserver;
    var accent = '#c21d4e';
    var bright = '#ef5d85';

    function find(id) {
      for (var i = 0; i < birds.length; i++) if (!birds[i].me && birds[i].id === id) return birds[i];
      return null;
    }

    function hatch(item, flyIn) {
      var r = chance(hash(item.id || 'you'));
      return {
        id: item.id, when: item.when, r: r, a: r(), d: r(),
        phase: r() * 6.283, wander: r() * 6.283, pace: 16 + r() * 14,
        enter: flyIn ? (r() < 0.5 ? -1 : 1) : 0, delay: r(),
        x: null, y: null, vx: 0, vy: 0, glow: 0
      };
    }

    // The band of sky the birds keep to, above the water.
    function top() { return 16; }
    function waterline() { return H * horizon; }
    function bottom() { return waterline() - 18; }

    // Where a bird sits in a loose cloud: its place in a still sky, and
    // where a bird that doesn't fly in first appears.
    function home(b) {
      var a = b.a * Math.PI * 2;
      var d = Math.sqrt(b.d);
      var mid = (top() + bottom()) / 2 + (bottom() - top()) * 0.08;
      b.x = W / 2 + Math.cos(a) * d * W * 0.36;
      b.y = mid + Math.sin(a) * d * (bottom() - top()) * 0.34;
      if (!b.vx && !b.vy) {
        b.vx = 24;
        b.vy = (b.a - 0.5) * 8;
      }
    }

    // A bird's first place: beyond the edge, a little way out so the
    // flock comes in over a few seconds, if it is to fly in.
    function place(b) {
      if (b.enter && !still()) {
        b.x = b.enter < 0 ? -12 - b.delay * 140 : W + 12 + b.delay * 140;
        b.y = 30 + b.d * Math.max(0, bottom() - 50);
        b.vx = b.enter < 0 ? 42 : -42;
        b.vy = (b.a - 0.5) * 8;
      } else {
        home(b);
      }
      b.enter = 0;
    }

    function words() {
      if (!say) return;
      say.textContent = '';
      if (!shown) return;
      if (shown.me) {
        say.textContent = opts.me || '';
        return;
      }
      var w = opts.say ? opts.say(shown) : [shown.id];
      var name = document.createElement('strong');
      name.className = 'live-scene-name';
      breakable(name, w[0]);
      if (w[2]) name.title = w[2];
      say.appendChild(name);
      if (w[1]) {
        say.appendChild(document.createTextNode(' '));
        var when = document.createElement('span');
        when.className = 'live-scene-ago';
        when.textContent = w[1];
        say.appendChild(when);
      }
    }

    function pick(b) {
      picked = shown = b;
      words();
      draw();
    }

    function set(list) {
      list = list || [];
      var seen = {};
      var fresh = null;
      list.forEach(function (item) {
        seen[item.id] = true;
        var b = find(item.id);
        if (b) {
          b.when = item.when;
          b.leaving = false;
        } else {
          b = hatch(item, !still());
          birds.push(b);
        }
        if (item.fresh) {
          b.glow = GLOW;
          if (!fresh) fresh = b;
        }
      });
      birds = birds.filter(function (b) {
        if (b.me || seen[b.id]) return true;
        if (still() || !W || b.x == null) return false;
        b.leaving = true;
        return true;
      });
      order = list.map(function (item) { return item.id; });
      if (shown && !shown.me && !seen[shown.id]) shown = null;
      if (picked && !picked.me && !seen[picked.id]) picked = null;
      if (fresh) {
        shown = fresh;
        picked = null;
      }
      words();
      if (opts.label) sky.setAttribute('aria-label', opts.label(list.length));
      measure();
      wake();
      draw();
    }

    function measure() {
      var w = sky.clientWidth;
      var h = sky.clientHeight;
      if (!w || !h) return;
      var d = Math.min(window.devicePixelRatio || 1, 2);
      if (w === W && h === H && d === dpr) return;
      var kx = W ? w / W : 1;
      var ky = H ? h / H : 1;
      birds.forEach(function (b) {
        if (b.x == null) return;
        b.x *= kx;
        b.y *= ky;
      });
      W = w;
      H = h;
      dpr = d;
      sky.width = Math.round(W * dpr);
      sky.height = Math.round(H * dpr);
      draw();
    }

    function paints() {
      var pink = colour('--crimson', '#c21d4e');
      accent = pink;
      bright = towards(pink, '#ffe0e8', 0.3);
    }

    // The flock's rules, gently: keep a little apart, fly roughly the way
    // your neighbours fly, stay near them, each with a slow wandering of
    // its own; the whole drifts about the sky, never so far that it
    // presses on the edge, and every bird turns back before it. A finger
    // held in the sky draws the birds near it.
    function step(dt) {
      time += dt;
      var t = top();
      var btm = bottom();
      var flying = 0;
      birds.forEach(function (b) { if (!b.leaving) flying++; });
      var spread = 12 + 7 * Math.sqrt(flying);
      var gx = W / 2 + Math.max(0, W / 2 - 22 - spread) * Math.sin(time * 0.09);
      var gy = (t + btm) / 2 + Math.max(0, (btm - t) / 2 - spread * 0.8) * Math.sin(time * 0.13 + 1.3);
      birds.forEach(function (b) {
        if (b.x == null) place(b);
        if (b.glow) b.glow = Math.max(0, b.glow - dt);
        b.drawn = false;
        var entering = !b.leaving && (b.x < 0 || b.x > W);
        var fx = 0;
        var fy = 0;
        if (b.leaving) {
          fx = b.x < W / 2 ? -70 : 70;
          fy = -12;
        } else if (entering) {
          // Still beyond the edge: straight on in.
          b.vx = b.x < 0 ? 42 : -42;
        } else {
          var n = 0, cx = 0, cy = 0, ax = 0, ay = 0, sx = 0, sy = 0;
          birds.forEach(function (o) {
            if (o === b || o.leaving || o.x == null) return;
            var dx = o.x - b.x;
            var dy = o.y - b.y;
            var d2 = dx * dx + dy * dy;
            if (d2 > 3600) return;
            n++;
            cx += o.x;
            cy += o.y;
            ax += o.vx;
            ay += o.vy;
            if (d2 < 400 && d2 > 0.01) {
              var d = Math.sqrt(d2);
              sx -= dx / d * (20 - d) / 20;
              sy -= dy / d * (20 - d) / 20;
            }
          });
          if (n) {
            fx += (cx / n - b.x) * 0.2 + (ax / n - b.vx) * 0.3;
            fy += (cy / n - b.y) * 0.2 + (ay / n - b.vy) * 0.3;
          }
          b.wander += (b.r() - 0.5) * dt * 4;
          fx += sx * 36 + (gx - b.x) * 0.05 + Math.cos(b.wander) * 12;
          fy += sy * 36 + (gy - b.y) * 0.05 + Math.sin(b.wander) * 12;
          if (press) {
            var px = press.x - b.x;
            var py = press.y - b.y;
            if (px * px + py * py < REACH * REACH) {
              fx += px * 2.4;
              fy += py * 2.4;
              b.drawn = true;
            }
          }
          var m = 20;
          if (b.x < m) fx += (m - b.x) * 6;
          if (b.x > W - m) fx -= (b.x - W + m) * 6;
          if (b.y < t) fy += (t - b.y) * 6;
          if (b.y > btm) fy -= (b.y - btm) * 6;
        }
        b.vx += fx * dt;
        b.vy += fy * dt;
        var s = Math.sqrt(b.vx * b.vx + b.vy * b.vy);
        // Each bird keeps near a pace of its own, unless drawn or passing.
        if (!b.leaving && !b.drawn && !entering && s > 0) {
          var k = 1 + (b.pace / s - 1) * Math.min(1, dt * 0.8);
          b.vx *= k;
          b.vy *= k;
          s *= k;
        }
        var max = b.leaving ? 70 : b.drawn ? 95 : entering ? 42 : 34;
        if (s > max) {
          b.vx *= max / s;
          b.vy *= max / s;
        } else if (s < 10 && s > 0) {
          b.vx *= 10 / s;
          b.vy *= 10 / s;
        }
        b.phase += dt * (7 + s * 0.12);
      });
      birds.forEach(function (b) {
        b.x += b.vx * dt;
        b.y += b.vy * dt;
      });
      birds = birds.filter(function (b) {
        return !b.leaving || (b.x > -20 && b.x < W + 20 && b.y > -20 && b.y < H + 20);
      });
    }

    function stroke(x1, y, x2) {
      ctx.beginPath();
      ctx.moveTo(x1, y);
      ctx.lineTo(x2, y);
      ctx.stroke();
    }

    // Below the horizon, faint lines on the water: the glow's reflection
    // down the middle, cream glints either side, all slowly shimmering.
    function water() {
      var y0 = waterline();
      var n = 9;
      var t = still() ? 0 : time;
      ctx.lineWidth = 1;
      for (var k = 1; k <= n; k++) {
        var f = k / (n + 1);
        var y = Math.round(y0 + (H - y0) * Math.pow(f, 1.4)) + 0.5;
        var a = 0.16 + 0.1 * Math.sin(t * 0.8 + k * 1.7);
        var half = W * (0.06 + 0.2 * f);
        var drift = Math.sin(t * 0.25 + k * 2.3) * 8;
        ctx.strokeStyle = accent;
        ctx.globalAlpha = a + 0.12 * (1 - f);
        stroke(W / 2 - half + drift, y, W / 2 + half + drift);
        ctx.strokeStyle = CREAM;
        ctx.globalAlpha = a * 0.55;
        stroke(W / 2 - half * 2.1 - drift * 0.5, y, W / 2 - half * 1.35 - drift * 0.5);
        stroke(W / 2 + half * 1.4 + drift * 0.7, y, W / 2 + half * 2 + drift * 0.7);
      }
      ctx.globalAlpha = 1;
    }

    // A bird far off, as a "v" with its wings beating; still birds hold
    // their wings half raised. The reader's is pink, edged in dusk so it
    // shows against the glow.
    function glyph(b) {
      var flap = still() ? 0.3 : Math.sin(b.phase);
      var w = b.me ? 6 : 5.2;
      var tip = 0.7 - flap * 2.6;
      var bank = clamp(Math.atan2(b.vy, Math.abs(b.vx) + 0.001) * 0.5, -0.45, 0.45);
      ctx.save();
      ctx.translate(b.x, b.y);
      ctx.rotate(b.vx < 0 ? -bank : bank);
      ctx.beginPath();
      ctx.moveTo(-w, tip);
      ctx.quadraticCurveTo(-w * 0.45, -1.7 + flap * 0.4, 0, 1);
      ctx.quadraticCurveTo(w * 0.45, -1.7 + flap * 0.4, w, tip);
      if (b.me) {
        ctx.strokeStyle = DUSK;
        ctx.globalAlpha = 0.55;
        ctx.lineWidth = 4;
        ctx.stroke();
        ctx.strokeStyle = bright;
        ctx.globalAlpha = 1;
        ctx.lineWidth = 2.1;
      } else {
        ctx.strokeStyle = CREAM;
        ctx.globalAlpha = 0.88;
        ctx.lineWidth = 1.4;
      }
      ctx.stroke();
      ctx.restore();
    }

    function ring(b, r, colourOf, alpha, width) {
      ctx.globalAlpha = alpha;
      ctx.strokeStyle = colourOf;
      ctx.lineWidth = width;
      ctx.beginPath();
      ctx.arc(b.x, b.y, r, 0, Math.PI * 2);
      ctx.stroke();
    }

    function draw() {
      if (!W) return;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.clearRect(0, 0, W, H);
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      water();
      birds.forEach(function (b) {
        if (still()) home(b);
        else if (b.x == null) place(b);
        glyph(b);
        if (b.glow && !still()) ring(b, 6 + (1 - b.glow / GLOW) * 16, CREAM, b.glow / GLOW * 0.7, 1);
      });
      if (ringed) {
        ring(me, 10, DUSK, 0.5, 3.4);
        ring(me, 10, bright, 1, 1.5);
      }
      if (shown && !shown.leaving) ring(shown, 14, CREAM, 0.85, 1);
      ctx.globalAlpha = 1;
    }

    function wake() {
      if (running) return;
      if (still() || !onScreen || document.hidden || !W) {
        draw();
        return;
      }
      running = true;
      last = 0;
      requestAnimationFrame(frame);
    }

    function frame(ts) {
      if (still() || !onScreen || document.hidden || !W) {
        running = false;
        draw();
        return;
      }
      var dt = last ? Math.min((ts - last) / 1000, 0.05) : 1 / 60;
      last = ts;
      step(dt);
      draw();
      requestAnimationFrame(frame);
    }

    function at(e) {
      var r = sky.getBoundingClientRect();
      return { x: e.clientX - r.left, y: e.clientY - r.top };
    }

    // Hold the birds' attention at p, within the band of sky.
    function hold(p) {
      press = still() ? null : { x: clamp(p.x, 0, W), y: clamp(p.y, top(), bottom()) };
      wake();
    }

    function tapAt(p) {
      var best = null;
      var bd = TAP * TAP;
      birds.forEach(function (b) {
        if (b.leaving || b.x == null) return;
        var d = (b.x - p.x) * (b.x - p.x) + (b.y - p.y) * (b.y - p.y);
        if (d <= bd) {
          bd = d;
          best = b;
        }
      });
      pick(best);
    }

    // A press draws the birds at once. Once the finger has moved, a
    // mostly vertical drag is the page being scrolled (touch-action lets
    // the browser take it), and the birds are let go. A press that barely
    // moves and is soon lifted is a tap.
    sky.addEventListener('pointerdown', function (e) {
      if (e.button > 0) return;
      var p = at(e);
      gesture = { id: e.pointerId, x: p.x, y: p.y, t: Date.now(), moved: false, decided: false, scroll: false };
      try {
        sky.setPointerCapture(e.pointerId);
      } catch (err) {}
      hold(p);
    });
    sky.addEventListener('pointermove', function (e) {
      if (!gesture || e.pointerId !== gesture.id) return;
      var p = at(e);
      var dx = Math.abs(p.x - gesture.x);
      var dy = Math.abs(p.y - gesture.y);
      if (dx > 8 || dy > 8) gesture.moved = true;
      if (!gesture.decided && (dx > 10 || dy > 10)) {
        gesture.decided = true;
        gesture.scroll = dy > dx;
      }
      if (gesture.scroll) press = null;
      else hold(p);
    });
    function release(e) {
      if (!gesture || e.pointerId !== gesture.id) return;
      if (e.type === 'pointerup' && !gesture.moved && Date.now() - gesture.t < 600) tapAt(at(e));
      gesture = null;
      press = null;
    }
    sky.addEventListener('pointerup', release);
    sky.addEventListener('pointercancel', release);

    sky.addEventListener('keydown', function (e) {
      var all = [me].concat(order.map(find).filter(function (b) { return b && !b.leaving; }));
      var i = picked ? all.indexOf(picked) : -1;
      if (e.key === 'ArrowRight' || e.key === 'ArrowDown') i = (i + 1) % all.length;
      else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') i = i <= 0 ? all.length - 1 : i - 1;
      else if (e.key === 'Home') i = 0;
      else if (e.key === 'End') i = all.length - 1;
      else if (e.key === 'Escape') i = -1;
      else return;
      e.preventDefault();
      pick(i >= 0 ? all[i] : null);
    });

    if (window.IntersectionObserver) {
      new IntersectionObserver(function (es) {
        onScreen = es[es.length - 1].isIntersecting;
        wake();
      }).observe(sky);
    }
    if (window.ResizeObserver) {
      new ResizeObserver(function () {
        measure();
        wake();
      }).observe(sky);
    } else {
      window.addEventListener('resize', function () {
        measure();
        wake();
      });
    }
    paints();
    measure();
    wake();
    var self = {
      set: set,
      ring: function (on) {
        ringed = !!on;
        draw();
      },
      refresh: function () {
        paints();
        words();
        wake();
        draw();
      }
    };
    scenes.push(self);
    return self;
  }

  document.addEventListener('visibilitychange', function () {
    scenes.forEach(function (s) { s.refresh(); });
  });
  onSettings(function () {
    scenes.forEach(function (s) { s.refresh(); });
  });

  window.Live = {
    still: still,
    recall: recall,
    remember: remember,
    ago: ago,
    moved: moved,
    breakable: breakable,
    pulse: pulse,
    count: count,
    drawLine: drawLine,
    timeline: timeline,
    linkify: linkify,
    fold: fold,
    chapters: chapters,
    tabs: tabs,
    dial: dial,
    rows: rows,
    tiles: tiles,
    swipe: swipe,
    countUp: countUp,
    bar: bar,
    mini: mini,
    filmModel: filmModel,
    film: film,
    scene: scene
  };
})();
