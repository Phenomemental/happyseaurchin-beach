// Sundial on the home screen (spine:front-door 6.1.4): a small service worker
// that keeps the page, its scripts, styles, fonts and the heart, so the app
// opens at once, and opens offline too. It answers only for sundial.html (it
// is registered with that scope), and only GETs of this site's own files
// and the fonts. The beach and Claude are never touched here: every read
// and every write goes to them live, and nothing is ever kept back to send
// later.
var KEPT = 'sundial-files-1';
// Files the page loads itself (gray.js's own imports) and the app's own.
var ALWAYS = [
  'sundial.html',
  'manifest.webmanifest',
  'icons/sundial-192.png',
  'icons/sundial-512.png',
  'icons/sundial-maskable-512.png',
  'vendor/noble-hashes/argon2.js',
  'vendor/noble-hashes/blake2.js',
  'vendor/noble-hashes/blake-shared.js',
  'vendor/noble-hashes/md.js',
  'vendor/noble-hashes/u64.js',
  'vendor/noble-hashes/utils.js'
];

// Every file sundial.html names (scripts and stylesheets on this site, the
// fonts' stylesheet), so a page kept here always has its files kept too.
function named(html, base) {
  var out = [];
  html.replace(/<(?:script|link)\b[^>]*?\s(?:src|href)="([^"#]+)"/g, function (all, ref) {
    var u = new URL(ref, base);
    if ((u.origin === self.location.origin && /\.(js|css|webmanifest|png)$/.test(u.pathname)) ||
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
    return cache.addAll(own);
  }).then(function () {
    return cache.put(new URL('sundial.html', self.location.href).href, res);
  });
}

self.addEventListener('install', function (ev) {
  ev.waitUntil(caches.open(KEPT).then(function (cache) {
    return cache.addAll(ALWAYS).then(function () {
      return fetch('sundial.html', { cache: 'no-cache' }).then(function (res) { return keepPage(cache, res); });
    });
  }).then(function () { return self.skipWaiting(); }));
});

self.addEventListener('activate', function (ev) {
  ev.waitUntil(caches.keys().then(function (keys) {
    return Promise.all(keys.filter(function (k) { return k.indexOf('sundial-files-') === 0 && k !== KEPT; }).map(function (k) { return caches.delete(k); }));
  }).then(function () { return self.clients.claim(); }));
});

function ours(url) {
  return url.origin === self.location.origin || url.hostname === 'fonts.googleapis.com' || url.hostname === 'fonts.gstatic.com';
}

// The kept copy at once, and a fresh one fetched behind it for next time.
// The page itself is kept whatever its address says after it (?from=).
self.addEventListener('fetch', function (ev) {
  var req = ev.request, url = new URL(req.url);
  if (req.method !== 'GET' || !ours(url)) return;
  var page = req.mode === 'navigate' || (url.origin === self.location.origin && /\/sundial\.html$/.test(url.pathname));
  ev.respondWith(caches.open(KEPT).then(function (cache) {
    var key = page ? new URL('sundial.html', self.location.href).href : req;
    return cache.match(key).then(function (hit) {
      var fresh = fetch(req).then(function (res) {
        if (page && res.ok) return keepPage(cache, res.clone()).then(function () { return res; }, function () { return res; });
        if (res.ok || res.type === 'opaque') cache.put(req, res.clone());
        return res;
      });
      if (hit) {
        ev.waitUntil(fresh.catch(function () {}));
        return hit;
      }
      return fresh;
    });
  }));
});
