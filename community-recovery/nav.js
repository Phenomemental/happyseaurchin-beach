/* nav.js — Community Recovery's one navigation: the header, the menu panel and the
   "Next step" card, all drawn from the one list below, so the order of the journey is
   changed here and nowhere else. Each page loads this straight after its
   <header class="masthead"> and the header is drawn at once; the Next step card is
   placed above the page's footer once the page has loaded. The look is in brand.css. */
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
  var MORE = [['made.html', 'How this was made']];
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
  MORE.forEach(function (m) {
    var a = link(m[0], 'cr-more-link', m[1]);
    if (m[0] === here) a.appendChild(el('span', 'cr-here', 'You are here'));
    more.appendChild(a);
  });
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
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', nextStep);
  else nextStep();

  window.CRNav = { journey: JOURNEY, more: MORE, open: open, close: close };
})();
