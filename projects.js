/* Projects by their proper names, and the pages Matthew has made for his
   (as Matthew named them, 25 October 2026). walk.html reads these for its
   title. Sundial's YOUR WORK takes only titled(), for each family's name;
   since the lists were nested (25 October 2026) each family there goes to
   its walk on David's site. place() gives a project's own page or
   walk.html?p=<project>. For anyone but Matthew, a project is named from
   its block name. */
(function (root) {
  var MATTHEW = 'phenomemental';
  var NAMES = {
    'lero-nnh-recovery-capital': 'Recovery capital',
    'wholeless-nights': 'Wholeless Nights',
    'community-recovery-design': 'Community Recovery',
    'golden-flow': 'Golden Flow',
    'night-watchman': 'The Night Watchman',
    'front-door': 'Sundial',
    'tideline': 'Tideline'
  };
  // Each made page, and one short line saying what it is.
  var PAGES = {
    'wholeless-nights': { href: 'wholeless-nights.html', what: 'Bipolar and the included middle, a book in progress' },
    'golden-flow': { href: 'golden-flow.html', what: 'what is asked and answered, from where you stand' },
    'night-watchman': { href: 'night-watchman.html', what: 'a quiet place to keep a watch of your own' },
    'front-door': { href: 'sundial.html', what: 'your day back, in your own words' },
    'community-recovery-design': { href: 'community-recovery/', what: 'recovery, grown in the open' }
  };
  var WALK = 'a walk through it';

  // A block name as a title: "golden-flow" → "Golden Flow",
  // "leros-on-the-commons" → "Leros on the Commons".
  var SMALL = { a: 1, an: 1, and: 1, as: 1, at: 1, by: 1, 'for': 1, 'in': 1, of: 1, on: 1, or: 1, the: 1, to: 1, with: 1 };
  function titled(slug) {
    return String(slug).split(/[-_\s]+/).filter(Boolean).map(function (w, i) {
      return i && SMALL[w.toLowerCase()] ? w.toLowerCase() : w.charAt(0).toUpperCase() + w.slice(1);
    }).join(' ');
  }
  function walkOf(slug) { return 'walk.html?p=' + encodeURIComponent(slug); }
  function isMatthew(who) { return String(who || '').toLowerCase() === MATTHEW; }

  // A project's name, for whoever it belongs to (anyone, when not given).
  function name(slug, who) {
    return (who == null || isMatthew(who)) && NAMES[slug] ? NAMES[slug] : titled(slug);
  }
  // Where one of a person's projects goes, as {name, href, what, walk}.
  function place(slug, who) {
    var page = isMatthew(who) ? PAGES[slug] : null;
    return page
      ? { name: name(slug, who), href: page.href, what: page.what, walk: false }
      : { name: name(slug, who), href: walkOf(slug), what: WALK, walk: true };
  }

  var api = { NAMES: NAMES, PAGES: PAGES, titled: titled, walkOf: walkOf, name: name, place: place };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Projects = api;
})(this);
