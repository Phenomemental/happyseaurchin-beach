/* nav.js — Community Recovery's one navigation: the header, the menu panel, the
   "Next step" card and the footer's legal words, all drawn from here, so the order of
   the journey is changed here and nowhere else. Each page loads this straight after its
   <header class="masthead"> and the header is drawn at once; the Next step card is
   placed above the page's footer, and the legal words at the top of it, once the page has
   loaded. The look is in brand.css. */
(function () {
  'use strict';

  // THE JOURNEY, in order: page, label, one line.
  var JOURNEY = [
    ['presentation.html', 'The presentation', 'Start here: who we are, in 16 slides'],
    ['meet.html', 'Meet the LERO', 'The community, in its own words'],
    ['where-we-are-now.html', 'Where we are now', 'Our journey so far, and what’s next'],
    ['standards.html', 'The standards', 'How we’re growing towards the 33 national LERO standards'],
    ['constitution.html', 'The constitution', 'How we’re run, and who decides'],
    ['proposal.html', 'The proposal', 'What we offer other lived experience communities'],
    ['join.html', 'Join us', 'Come along, or get involved']
  ];
  // MORE: small, at the foot of the menu, not numbered.
  var MORE = [['made.html', 'How this was made'], ['policies.html', 'Policies']];
  var JOIN = 'join.html';

  var here = (location.pathname.split('/').pop() || 'index.html').toLowerCase();
  var step = -1;
  JOURNEY.forEach(function (s, i) { if (s[0] === here) step = i; });

  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }
  function link(href, cls, text) {
    var a = el('a', cls, text);
    a.href = href;
    if (href === here) a.setAttribute('aria-current', 'page');
    return a;
  }

  // ---- the header: a slim bar, the logo and the name on the left, Join us and Menu on the right
  var header = document.querySelector('header.masthead');
  if (!header) return;
  header.id = header.id || 'top';
  header.textContent = '';
  var bar = el('div', 'wrap cr-bar');
  var home = link('./', 'cr-home');
  var logo = el('span', 'cr-logo logo-on-black');
  var img = el('img');
  img.src = 'img/logo-small.jpg';
  img.alt = '';
  img.width = 40;
  img.height = 40;
  logo.appendChild(img);
  home.appendChild(logo);
  home.appendChild(el('span', 'cr-name', 'Community Recovery'));
  var actions = el('div', 'cr-actions');
  var join = link(JOIN, 'cr-join', 'Join us');
  var menuBtn = el('button', 'cr-menu-btn');
  menuBtn.type = 'button';
  menuBtn.setAttribute('aria-expanded', 'false');
  menuBtn.setAttribute('aria-controls', 'cr-menu');
  menuBtn.appendChild(el('span', 'cr-burger', '☰'));
  menuBtn.lastChild.setAttribute('aria-hidden', 'true');
  menuBtn.appendChild(document.createTextNode(' Menu'));
  actions.appendChild(join);
  actions.appendChild(menuBtn);
  bar.appendChild(home);
  bar.appendChild(actions);
  header.appendChild(bar);

  // ---- the menu panel: every step with its number, label and one line; More beneath
  var menu = el('div', 'cr-menu');
  menu.id = 'cr-menu';
  menu.hidden = true;
  var backdrop = el('div', 'cr-menu-backdrop');
  var sheet = el('div', 'cr-menu-sheet');
  sheet.setAttribute('role', 'dialog');
  sheet.setAttribute('aria-modal', 'true');
  sheet.setAttribute('aria-label', 'Menu');
  var head = el('div', 'cr-menu-head');
  head.appendChild(el('p', 'cr-menu-title', 'Community Recovery, step by step'));
  var shut = el('button', 'cr-menu-close', '×');
  shut.type = 'button';
  shut.setAttribute('aria-label', 'Close the menu');
  head.appendChild(shut);
  sheet.appendChild(head);
  var ol = el('ol', 'cr-steps');
  JOURNEY.forEach(function (s, i) {
    var li = el('li');
    var a = link(s[0], 'cr-step');
    a.appendChild(el('span', 'cr-num', String(i + 1)));
    var words = el('span', 'cr-step-words');
    words.appendChild(el('span', 'cr-step-label', s[1]));
    words.appendChild(el('span', 'cr-step-line', s[2]));
    a.appendChild(words);
    if (s[0] === here) a.appendChild(el('span', 'cr-here', 'You are here'));
    li.appendChild(a);
    ol.appendChild(li);
  });
  sheet.appendChild(ol);
  var more = el('div', 'cr-more');
  more.appendChild(el('p', 'cr-more-title', 'More'));
  var moreLinks = el('div', 'cr-more-links');
  MORE.forEach(function (m) {
    var a = link(m[0], 'cr-more-link', m[1]);
    if (m[0] === here) a.appendChild(el('span', 'cr-here', 'You are here'));
    moreLinks.appendChild(a);
  });
  more.appendChild(moreLinks);
  sheet.appendChild(more);
  menu.appendChild(backdrop);
  menu.appendChild(sheet);
  header.appendChild(menu);

  var lastFocus = null;
  function focusables() { return Array.prototype.slice.call(sheet.querySelectorAll('a[href], button')); }
  function open() {
    lastFocus = document.activeElement;
    menu.hidden = false;
    menuBtn.setAttribute('aria-expanded', 'true');
    document.documentElement.classList.add('cr-menu-open');
    shut.focus();
  }
  function close(back) {
    if (menu.hidden) return;
    menu.hidden = true;
    menuBtn.setAttribute('aria-expanded', 'false');
    document.documentElement.classList.remove('cr-menu-open');
    if (back !== false) (lastFocus && lastFocus.focus ? lastFocus : menuBtn).focus();
  }
  menuBtn.addEventListener('click', function () { if (menu.hidden) open(); else close(); });
  shut.addEventListener('click', function () { close(); });
  backdrop.addEventListener('click', function () { close(); });
  // a link to the page already open only closes the menu (a hash still lands)
  sheet.addEventListener('click', function (ev) {
    var a = ev.target.closest && ev.target.closest('a');
    if (a && a.getAttribute('href') === here) { ev.preventDefault(); close(); }
  });
  document.addEventListener('keydown', function (ev) {
    if (menu.hidden) return;
    if (ev.key === 'Escape') { ev.preventDefault(); close(); return; }
    if (ev.key !== 'Tab') return;
    // keep the keyboard inside the open menu
    var f = focusables();
    if (!f.length) return;
    var first = f[0], last = f[f.length - 1];
    if (ev.shiftKey && document.activeElement === first) { ev.preventDefault(); last.focus(); }
    else if (!ev.shiftKey && document.activeElement === last) { ev.preventDefault(); first.focus(); }
    else if (!sheet.contains(document.activeElement)) { ev.preventDefault(); first.focus(); }
  });

  // ---- the Next step card, at the foot of every page in the journey
  function nextStep() {
    if (step < 0 || document.querySelector('.cr-next')) return;
    var last = step === JOURNEY.length - 1;
    var to = JOURNEY[last ? 0 : step + 1];
    var box = el('nav', 'wrap cr-next');
    box.setAttribute('aria-label', 'Next step');
    var card = el('a', 'cr-next-card');
    card.href = to[0];
    card.appendChild(el('span', 'cr-next-k', last ? 'Back to the start:' : 'Next:'));
    card.appendChild(document.createTextNode(' '));
    card.appendChild(el('span', 'cr-next-label', to[1]));
    card.appendChild(el('span', 'cr-next-sep', ', '));
    card.appendChild(el('span', 'cr-next-line', to[2]));
    var arrow = el('span', 'cr-next-arrow', ' →');
    arrow.setAttribute('aria-hidden', 'true');
    card.appendChild(arrow);
    box.appendChild(card);
    if (step > 0) {
      var prev = JOURNEY[step - 1];
      var p = el('p', 'cr-prev');
      var back = el('a', null, '← ' + prev[1]);
      back.href = prev[0];
      p.appendChild(back);
      box.appendChild(p);
    }
    var foot = document.querySelector('body > footer') || document.querySelector('footer');
    if (foot) foot.parentNode.insertBefore(box, foot);
    else document.body.appendChild(box);
  }

  // ---- the footer's legal words, at the top of every page's footer
  // Every word is read live from the block 'policies' in Community Recovery's own place,
  // the way beach.js reads the place: 1 the footer's lines, 2 to 7 the six policies (shown
  // on policies.html), 9.1 whether they are still a draft. Only two things are written here:
  // the short names of the six policy links, and the help line for when the beach can't be
  // reached (1.5's crisis numbers), so the help line never disappears. The page's own beach
  // line and live dot stay beneath, smaller.
  var PLACE = 'https://beach.happyseaurchin.com/w/community-recovery/.well-known/pscale-beach';
  // the six policies: the block's position, the anchor on policies.html, the link's name
  var POLICIES = [
    ['2', 'privacy', 'Privacy'],
    ['3', 'terms', 'Terms'],
    ['4', 'ai', 'AI policy'],
    ['5', 'safeguarding', 'Safeguarding'],
    ['6', 'accessibility', 'Accessibility'],
    ['7', 'contact', 'Contact']
  ];
  var HELP = 'Need help now? In an emergency call 999. For urgent mental health help call ' +
    'NHS 111 and choose option 2. Samaritans, any time: 116 123. Text SHOUT to 85258.';

  // One read of the block, shared by the footer and the policies page; fresh asks again.
  var reading = null;
  function policies(fresh) {
    if (fresh || !reading) {
      reading = fetch(PLACE + '?block=policies', { cache: 'no-store' }).then(function (r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
      });
    }
    return reading;
  }
  policies().catch(function () {});

  // A position's own words: a string, or the "_" of a node.
  function words(n) {
    while (n && typeof n === 'object') n = n._;
    return typeof n === 'string' ? n.trim() : '';
  }
  // The words at a dotted address, such as "1.5".
  function line(block, addr) {
    var n = block;
    String(addr).split('.').forEach(function (k) { n = n && typeof n === 'object' ? n[k] : undefined; });
    return words(n);
  }
  function isDraft(block) { return /^\s*DRAFT\b/.test(line(block, '9.1')); }

  // Words with their links: an email address becomes a mailto: link, "Facebook: name" the
  // page on Facebook, a web address its site, "Text WORD to 85258" an sms: link, and a phone
  // number (999, 111, 116 123, or a number beginning 0) a tel: link.
  var LINKS = new RegExp([
    '([\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+)',
    '(Facebook:\\s*(\\w+(?:\\.\\w+)*))',
    '\\b((?:[a-z0-9-]+\\.)+(?:org\\.uk|co\\.uk|gov\\.uk|nhs\\.uk|org|com|uk)\\b(?:\\/[\\w\\/-]*)?)',
    '\\b(text\\s+\\w+\\s+to\\s+(\\d{5}))\\b',
    '\\b(999|111|116 ?\\d{3}|0\\d{3} ?\\d{3} ?\\d{4}|0\\d{4} ?\\d{6}|0\\d{2} ?\\d{4} ?\\d{4})\\b'
  ].join('|'), 'gi');
  // `before` is the line's lead-in, already shown: it is read for context (so a lead-in
  // ending "Facebook:" still links the name after it) but not shown again.
  function linked(s, before) {
    var frag = document.createDocumentFragment(), skip = before ? before.length : 0, last = skip, m;
    s = (before || '') + s;
    LINKS.lastIndex = 0;
    while ((m = LINKS.exec(s))) {
      var from = Math.max(m.index, skip), end = m.index + m[0].length;
      if (end <= skip) continue;
      if (from > last) frag.appendChild(document.createTextNode(s.slice(last, from)));
      var a = el('a', null, s.slice(from, end));
      if (m[1]) a.href = 'mailto:' + m[1];
      else if (m[2]) {
        a.href = 'https://www.facebook.com/' + m[3];
        if (!/facebook/i.test(a.textContent)) a.setAttribute('aria-label', a.textContent + ' on Facebook');
      }
      else if (m[4]) a.href = 'https://' + m[4];
      else if (m[5]) a.href = 'sms:' + m[6];
      else {
        a.href = 'tel:' + m[7].replace(/\D/g, '');
        a.setAttribute('aria-label', 'Call ' + m[7]);
      }
      frag.appendChild(a);
      last = end;
    }
    if (last < s.length) frag.appendChild(document.createTextNode(s.slice(last)));
    return frag;
  }

  // A line into a paragraph: the words before its first colon (or its opening question),
  // when they are short enough to be a lead-in, in bold; the rest with its links.
  function lead(p, s) {
    var m = /^([^:?]{1,80}[:?])\s+/.exec(s);
    if (m && !/[.!]\s/.test(m[1])) {
      p.appendChild(el('strong', null, m[1]));
      p.appendChild(document.createTextNode(' '));
      p.appendChild(linked(s.slice(m[0].length), m[0]));
    } else {
      p.appendChild(linked(s));
    }
    return p;
  }
  function para(s, cls) { return lead(el('p', cls), s); }

  // The footer's legal part, made once: the page's own footer lines (its beach line and
  // live dot) move beneath it, and say that the words above them are read from policies.
  function legal() {
    var foot = document.querySelector('body > footer') || document.querySelector('footer');
    if (!foot) {
      foot = el('footer');
      foot.appendChild(el('div', 'wrap'));
      document.body.appendChild(foot);
    }
    var box = foot.querySelector('.cr-legal');
    if (box) return box;
    var wrap = foot.querySelector('.wrap') || foot;
    var beach = el('div', 'cr-beach');
    while (wrap.firstChild) beach.appendChild(wrap.firstChild);
    // the quiet way home is a dot on the screen; a screen reader hears where it goes
    var home = beach.querySelector('a.home');
    if (home && !home.hasAttribute('aria-label')) home.setAttribute('aria-label', 'Phenomemental’s home page');
    if (here !== 'policies.html') {
      var where = beach.querySelector('.where');
      beach.insertBefore(el('p', 'where', 'The words above are read live from policies, in the same place.'),
        where ? where.nextSibling : beach.firstChild);
    }
    box = el('div', 'cr-legal');
    wrap.appendChild(box);
    wrap.appendChild(beach);
    return box;
  }

  // Draw the legal part from the block; with no block, only the help line (from HELP above)
  // and the links to the policies.
  function drawFooter(block) {
    var box = legal();
    if (block && typeof block !== 'object') block = null;
    box.textContent = '';
    if (block) {
      var who = el('p');
      ['1.1', '1.2'].forEach(function (a) {
        var s = line(block, a);
        if (!s) return;
        if (who.firstChild) who.appendChild(document.createTextNode(' '));
        lead(who, s);
      });
      if (who.firstChild) box.appendChild(who);
      if (line(block, '1.3')) box.appendChild(para(line(block, '1.3')));
      if (line(block, '1.4')) box.appendChild(para(line(block, '1.4')));
    }
    box.appendChild(para((block && line(block, '1.5')) || HELP, 'cr-help'));
    if (block && line(block, '1.6')) box.appendChild(para(line(block, '1.6'), 'cr-small'));
    var nav = el('nav', 'cr-policy-links');
    nav.setAttribute('aria-label', 'Policies');
    var ul = el('ul');
    POLICIES.forEach(function (p) {
      var li = el('li');
      var dot = el('span', 'cr-sep', '·');
      dot.setAttribute('aria-hidden', 'true');
      li.appendChild(dot);
      li.appendChild(link('policies.html#' + p[1], null, p[2]));
      ul.appendChild(li);
    });
    nav.appendChild(ul);
    box.appendChild(nav);
    if (block && isDraft(block)) box.appendChild(el('p', 'cr-draft', 'Draft policies'));
  }

  function atFoot() {
    drawFooter(null);
    policies().then(drawFooter, function () {});
    nextStep();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', atFoot);
  else atFoot();

  window.CRNav = {
    journey: JOURNEY, more: MORE, open: open, close: close,
    policyList: POLICIES, policies: policies, words: words, line: line, isDraft: isDraft,
    linked: linked, para: para, drawFooter: drawFooter
  };
})();
