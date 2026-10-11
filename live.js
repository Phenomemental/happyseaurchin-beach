/* What lets a reader see, and touch, that a page is live: a count that
   pulses when it rises, lines that draw themselves as they are reached,
   a line of time to tap along, and a sky with a flock of what has just
   moved on the beach. Shared by the pages that read from the beach; every word stays
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

  // One pulse on an element; none for anyone who asks for less motion.
  function pulse(el) {
    if (still()) return;
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
  // brings it into view. Arrow keys move between times; Tab comes in at
  // the current one. Returns { el, destroy }.
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
      return { label: m.label, at: m.at, target: m.target, note: m.note, i: i };
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

    function pick(j) {
      var k = marks[j];
      if (!k) return;
      if (cur !== j) {
        if (cur >= 0) mark(marks[cur], false);
        cur = j;
        mark(k, true);
        marks.forEach(function (m, q) { m.btn.tabIndex = q === j ? 0 : -1; });
        placeRing();
      }
      // A tap both focuses and clicks; bring the moment into view once.
      var now = Date.now();
      if (shown.j === j && now - shown.t < 400) return;
      shown = { j: j, t: now };
      if (k.target && k.target.scrollIntoView) {
        k.target.scrollIntoView({ block: 'nearest', behavior: still() ? 'auto' : 'smooth' });
      }
      if (box.scrollWidth > box.clientWidth) {
        var x = parseFloat(k.btn.style.left) || 0;
        if (x - TARGET < box.scrollLeft || x + TARGET > box.scrollLeft + box.clientWidth) {
          box.scrollLeft = x - box.clientWidth / 2;
        }
      }
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

    function placeRing() {
      var k = marks[cur];
      if (!k || k.x == null) return;
      ring.setAttribute('cx', k.x);
      ring.setAttribute('cy', axisY);
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
      layout: function () { layout(true); },
      destroy: function () {
        if (ro) ro.disconnect();
        else window.removeEventListener('resize', relayout);
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
    scene: scene
  };
})();
