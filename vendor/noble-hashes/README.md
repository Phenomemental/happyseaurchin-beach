Argon2id from noble-hashes 2.4.0 (MIT, Paul Miller), copied as published,
for gray.js (plan, spine:front-door 3 and 14). argon2.js and the five
files it imports; nothing else from the package is used.

One change: GitHub Pages leaves out files whose names begin with an
underscore, so _blake.js, _md.js and _u64.js are here as blake-shared.js,
md.js and u64.js, and the import lines in blake2.js and md.js name them so.
The code is otherwise as published.
