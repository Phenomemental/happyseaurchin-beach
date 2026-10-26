/* Community Recovery's app's copy of the beach's own gray seal (the site's gray.js, unchanged
   but for the address below), so the app stands alone in this folder. A person's days
   are kept in Community Recovery's place, and the beach's connector seals a line there
   salted with the place's own address, so this copy seals with that address and opens
   with it first, then with the handle: a day sealed here opens through the connector,
   and one sealed through the connector opens here.

   The beach's own gray seal, self mode, in the page (plan, section 3,
   agreed by Matthew 23 October 2026). Exactly as bsp-mcp's src/keys.ts,
   so a line sealed here opens through the connector, and one sealed
   through the connector opens here:

     key   Argon2id over the word (64 MB, 3 passes, parallelism 1, 64
           bytes), salted with the name the beach was addressed by
           (padded with noughts to 8 if shorter); its first 32 bytes.
     seal  NaCl secretbox (XSalsa20-Poly1305), a random 24-byte nonce;
           text as it is, anything else as JSON.
     shape { "_": "Encrypted (gray); readable only with the author
           secret.", "1": <base64 ciphertext>, "2": <base64 nonce>,
           "9": { "_": "gray", "1": "self" } }

   bsp-mcp salts with whichever name its caller used, the beach's address
   or the handle, so opening tries the address first, then the handle.
   Sealing always uses the address.

   The two algorithms are copied into vendor/ as plain files: TweetNaCl
   1.0.3 (public domain; secretbox only) and Argon2id from noble-hashes
   2.4.0 (MIT). Nothing else from them is used, and nothing is fetched
   from elsewhere. The word is passed in for one call and kept nowhere
   here; a key made from it lives in memory for the visit. */
import { argon2idAsync } from './vendor/noble-hashes/argon2.js';

var nacl = window.nacl;
var NOTE = 'Encrypted (gray); readable only with the author secret.';
var BEACH = 'https://beach.happyseaurchin.com/w/community-recovery';
var made = new Map();

function b64(bytes) {
  var s = '';
  for (var i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
  return btoa(s);
}
function unb64(text) {
  var s = atob(text), out = new Uint8Array(s.length);
  for (var i = 0; i < s.length; i++) out[i] = s.charCodeAt(i);
  return out;
}

// The key a word makes under one salt: slow on purpose (memory-hard), so
// it is made once a visit and kept, never the word.
function key(word, salt) {
  var s = String(salt || BEACH);
  if (s.length < 8) s = s + '\0'.repeat(8 - s.length);
  var id = s + '\n' + word;
  if (!made.has(id)) {
    made.set(id, argon2idAsync(String(word), s, { t: 3, m: 65536, p: 1, dkLen: 64 }).then(function (h) {
      return nacl.box.keyPair.fromSecretKey(h.slice(0, 32)).secretKey;
    }, function (e) {
      made.delete(id);
      throw e;
    }));
  }
  return made.get(id);
}

function isGray(node) {
  var mark = node && typeof node === 'object' ? node['9'] : null;
  return !!(mark && typeof mark === 'object' && mark._ === 'gray' && mark['1'] === 'self' &&
    typeof node['1'] === 'string' && typeof node['2'] === 'string');
}

// Seal text (or anything JSON can carry) with the word, as the beach does.
function seal(content, word) {
  return key(word, BEACH).then(function (k) {
    var text = typeof content === 'string' ? content : JSON.stringify(content);
    var nonce = nacl.randomBytes(nacl.secretbox.nonceLength);
    var box = nacl.secretbox(new TextEncoder().encode(text), nonce, k);
    return { _: NOTE, 1: b64(box), 2: b64(nonce), 9: { _: 'gray', 1: 'self' } };
  });
}

// Open an envelope with the word: under the beach's address, then under
// the handle. Resolves the text, or null when neither opens it.
function open(envelope, word, handle) {
  if (!isGray(envelope)) return Promise.resolve(null);
  var salts = [BEACH].concat(handle ? [handle] : []);
  return salts.reduce(function (p, salt) {
    return p.then(function (found) {
      if (found !== null) return found;
      return key(word, salt).then(function (k) {
        var plain;
        try { plain = nacl.secretbox.open(unb64(envelope['1']), unb64(envelope['2']), k); } catch (e) { plain = null; }
        return plain ? new TextDecoder().decode(plain) : null;
      });
    });
  }, Promise.resolve(null));
}

window.Gray = { BEACH: BEACH, NOTE: NOTE, key: key, isGray: isGray, seal: seal, open: open };
window.dispatchEvent(new Event('gray-ready'));
