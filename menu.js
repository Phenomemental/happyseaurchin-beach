// The site's one menu, and the small mark the products carry.
//   <script src="menu.js"></script>              the menu, where the script stands
//   <script src="menu.js" data-mark></script>    "made by Matthew Bushell", leading home
// The menu is Matthew · Try · Read · My map; Try and Read open their lists
// on the home page. The page you are on (or the kind it belongs to) is marked.
(function () {
  var s = document.currentScript;
  if (!s) return;
  var here = (location.pathname.split('/').pop() || 'index.html').toLowerCase();

  if (s.hasAttribute('data-mark')) {
    var p = document.createElement('p');
    p.className = 'made-by';
    p.style.cssText = 'margin:1.6rem auto 1rem;padding:0 16px;text-align:center;font:italic 0.9rem/1.4 "Cormorant Garamond",Georgia,serif;opacity:0.65;position:relative;z-index:1';
    var a = document.createElement('a');
    a.href = 'index.html';
    a.textContent = 'made by Matthew Bushell';
    a.style.cssText = 'color:inherit;text-decoration:none;border-bottom:1px solid currentColor';
    p.appendChild(a);
    var main = document.querySelector('main');
    if (main && !main.contains(s)) main.appendChild(p);
    else s.parentNode.insertBefore(p, s);
    return;
  }

  // Pages that read like writing sit under Read; the rest of the menu is its own.
  var READ = ['wholeless-nights.html', 'capability-gap.html', 'flavour-spark.html', 'flavour-spark-film.html'];
  var items = [
    ['Matthew', 'index.html', here === '' || here === 'index.html'],
    ['Try', 'index.html#try', false],
    ['Read', 'index.html#read', READ.indexOf(here) >= 0],
    ['My map', 'map.html', here === 'map.html']
  ];
  var nav = document.createElement('nav');
  nav.className = 'site-nav';
  nav.setAttribute('aria-label', 'Site');
  items.forEach(function (it) {
    var a = document.createElement('a');
    a.textContent = it[0];
    a.href = it[1];
    if (it[2]) a.setAttribute('aria-current', 'page');
    nav.appendChild(a);
  });
  s.parentNode.insertBefore(nav, s);
})();
