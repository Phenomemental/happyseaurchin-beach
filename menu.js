// The site's one menu, and the lines at the foot of every page.
//   <script src="menu.js"></script>              the menu, where the script stands
//   <script src="menu.js" data-mark></script>    "made by Matthew Bushell", leading home
// The menu is Matthew · Try · Read · My map; Try and Read open their lists
// on the home page. The page you are on (or the kind it belongs to) is marked.
// Every page that loads this also carries the thanks to David Pinto at its
// foot, beneath the mark where both are. A game's own screen never scrolls,
// so there the foot is the end of its end-of-run panel; elsewhere it is the
// end of the page's <main>, the last thing a reader reaches.
(function () {
  var s = document.currentScript;
  if (!s) return;
  var here = (location.pathname.split('/').pop() || 'index.html').toLowerCase();
  var mark = s.hasAttribute('data-mark');

  // Small italic Cormorant Garamond, in the colour of the page it sits on.
  // Pages that don't load the italic face get it here.
  if (!document.querySelector('link[href*="Cormorant+Garamond:ital"]')) {
    var font = document.createElement('link');
    font.rel = 'stylesheet';
    font.href = 'https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@1,400&display=swap';
    document.head.appendChild(font);
  }
  function link(href, text) {
    var a = document.createElement('a');
    a.href = href;
    a.textContent = text;
    a.style.cssText = 'color:inherit;border:0;text-decoration:underline;text-decoration-thickness:1px;text-underline-offset:0.18em';
    return a;
  }
  var foot = document.createElement('footer');
  foot.className = 'site-foot';
  foot.style.cssText = 'margin:1.6rem auto 1rem;padding:0 16px;max-width:30rem;text-align:center;font:italic 400 0.9rem/1.45 "Cormorant Garamond",Georgia,serif;opacity:0.65;text-wrap:balance;position:relative;z-index:1';
  if (mark) {
    var made = document.createElement('p');
    made.className = 'made-by';
    made.style.margin = '0';
    made.appendChild(link('index.html', 'made by Matthew Bushell'));
    foot.appendChild(made);
  }
  var thanks = document.createElement('p');
  thanks.className = 'thanks';
  thanks.style.margin = mark ? '0.15rem 0 0' : '0';
  thanks.appendChild(document.createTextNode('With thanks to David Pinto, who built '));
  thanks.appendChild(link('https://happyseaurchin.com', 'the ground these pages stand on'));
  thanks.appendChild(document.createTextNode('.'));
  foot.appendChild(thanks);

  function place() {
    var panel = document.querySelector('.end .make-line');
    if (panel) {
      foot.style.margin = '1.2rem auto 0';
      foot.style.padding = '0';
      panel.parentNode.appendChild(foot);
      return;
    }
    (document.querySelector('main') || document.body).appendChild(foot);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', place);
  else place();

  if (mark) return;

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
