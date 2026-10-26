// Community Recovery's app on the home screen, adapted from the site's own home-screen helper: a small
// service worker that keeps the app's page, its scripts, styles, fonts and icons, so it opens
// offline too. It answers only for app.html (it is registered with that scope), and only GETs
// of this folder's own files and the fonts. The beach is never touched here: every read and
// every write goes to it live, and nothing is ever kept back to send later. Online, the page and
// its files always come fresh from the network (network first); the kept copy only answers when
// the network fails or is slow.
var KEPT = 'cr-app-files-1';
// Files the page loads itself (gray.js's own imports) and the app's own.
var ALWAYS = [
  'app.html',
  'app.webmanifest',
  'img/app-192.png',
  'img/app-512.png',
  'img/app-maskable-512.png',
  'img/logo-app.jpg',
  'vendor/noble-hashes/argon2.js',
  'vendor/noble-hashes/blake2.js',
  'vendor/noble-hashes/blake-shared.js',
  'vendor/noble-hashes/md.js',
  'vendor/noble-hashes/u64.js',
  'vendor/noble-hashes/utils.js'
];
// Online, the page and its own files come fresh; the kept copy answers when
// the network fails, or is slower than this.
var SLOW_MS = 3000;

// Every file app.html names (scripts and stylesheets on this site, the
// fonts' stylesheet), so a page kept here always has its files kept too.
function named(html, base) {
  var out = [];
  html.replace(/<(?:script|link)\b[^>]*?\s(?:src|href)="([^"#]+)"/g, function (all, ref) {
    var u = new URL(ref, base);
    if ((u.origin === self.location.origin && /\.(js|css|webmanifest|png|jpg)$/.test(u.pathname)) ||
      (u.hostname === 'fonts.googleapis.com' && /^\/css/.test(u.pathname))) out.push(u.href);
    return all;
  });
  return out;
}
// Keep a fresh copy of the page only once all its own files are kept, so an
// offline open never finds the page without them. The fonts are kept if
// they can be reached; without them the page still opens, in a fallback face.
function keepPage(cache, res) {
  return res.clone().text().then(function (html) {
    var all = named(html, res.url), own = all.filter(function (u) { return new URL(u).origin === self.location.origin; });
    all.filter(function (u) { return own.indexOf(u) < 0; }).forEach(function (u) { cache.add(u).catch(function () {}); });
    return cache.addAll(own.map(function (u) { return new Request(u, { cache: 'no-cache' }); }));
  }).then(function () {
    return cache.put(new URL('app.html', self.location.href).href, res);
  });
}

self.addEventListener('install', function (ev) {
  ev.waitUntil(caches.open(KEPT).then(function (cache) {
    return cache.addAll(ALWAYS.map(function (u) { return new Request(u, { cache: 'no-cache' }); })).then(function () {
      return fetch('app.html', { cache: 'no-cache' }).then(function (res) { return keepPage(cache, res); });
    });
  }).then(function () { return self.skipWaiting(); }));
});

self.addEventListener('activate', function (ev) {
  ev.waitUntil(caches.keys().then(function (keys) {
    return Promise.all(keys.filter(function (k) { return k.indexOf('cr-app-files-') === 0 && k !== KEPT; }).map(function (k) { return caches.delete(k); }));
  }).then(function () { return self.clients.claim(); }));
});

function ours(url) {
  return url.origin === self.location.origin || url.hostname === 'fonts.googleapis.com' || url.hostname === 'fonts.gstatic.com';
}

// The page and this site's own files: fresh from the network, asked of the
// server each time (so a change is seen on the first open after it) and
// kept for later; the kept copy only when the network fails or is slow, and
// so offline. The fonts, which never change: the kept copy at once, a fresh
// one fetched behind it. The page is kept whatever its address says after
// it (?from=, ?days=).
self.addEventListener('fetch', function (ev) {
  var req = ev.request, url = new URL(req.url);
  if (req.method !== 'GET' || !ours(url)) return;
  var own = url.origin === self.location.origin;
  var page = req.mode === 'navigate' || (own && /\/app\.html$/.test(url.pathname));
  ev.respondWith(caches.open(KEPT).then(function (cache) {
    var key = page ? new URL('app.html', self.location.href).href : req;
    // A navigation can't be refetched as itself with options, so by its address.
    var fresh = fetch(own ? new Request(req.url, { cache: 'no-cache', credentials: 'same-origin' }) : req).then(function (res) {
      if (page && res.ok) return keepPage(cache, res.clone()).then(function () { return res; }, function () { return res; });
      if (res.ok || res.type === 'opaque') cache.put(key, res.clone());
      return res;
    });
    ev.waitUntil(fresh.catch(function () {}));
    if (!own) {
      return cache.match(key).then(function (hit) { return hit || fresh; });
    }
    return new Promise(function (answer) {
      var done = false;
      function give(res) {
        if (done || !res) return;
        done = true;
        answer(res);
      }
      function kept(otherwise) {
        return cache.match(key).then(function (hit) { give(hit || otherwise); });
      }
      var slow = setTimeout(function () { cache.match(key).then(give); }, SLOW_MS);
      fresh.then(function (res) {
        clearTimeout(slow);
        if (res.ok) give(res);
        else kept(res);
      }, function () {
        clearTimeout(slow);
        kept(Response.error());
      });
    });
  }));
});
