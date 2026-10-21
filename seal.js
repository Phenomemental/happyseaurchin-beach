/* Sundial's seal: a day is sealed in the browser with its holder's word
   before it leaves the phone, and only that word opens it again (the plan
   on the beach at spine:front-door 7.13). The word is never stored, sent
   or shown here; it makes a key, and the key lives only as long as the
   page holds it.

   The scheme, aes-gcm-pbkdf2-v1, written out so any other reader can match
   it: the key is PBKDF2-SHA256 over the word's UTF-8, salted with the
   name's UTF-8, 310,000 rounds, 256 bits, for AES-GCM. Each seal draws a
   fresh 12-byte IV and encrypts the day's JSON as UTF-8, with no extra
   authenticated data; the ciphertext carries GCM's 16-byte tag at its end,
   as WebCrypto gives it. Both are standard base64. The envelope mirrors the
   beach's own gray shape, so other readers see it is sealed:

     { "_": "Sealed; readable only with the holder's word.",
       "1": "<base64 ciphertext>", "2": "<base64 iv>",
       "9": { "_": "gray", "1": "page", "2": "aes-gcm-pbkdf2-v1" } }

   The name and word are used exactly as given; the page settles them (the
   name lowercased) before it calls here, and uses the same word as the
   beach's edit-latch. WebCrypto and base64 are built in, so this runs in
   any current browser, and in Node for checking. */
(function (root) {
  var SCHEME = 'aes-gcm-pbkdf2-v1';
  var ROUNDS = 310000;
  var SAID = "Sealed; readable only with the holder's word.";
  var webcrypto = typeof crypto !== 'undefined' ? crypto : null;

  function utf8(s) {
    return new TextEncoder().encode(String(s));
  }
  function toBase64(bytes) {
    var s = '';
    for (var i = 0; i < bytes.length; i += 0x8000) {
      s += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
    }
    return btoa(s);
  }
  function fromBase64(b64) {
    var s = atob(b64), out = new Uint8Array(s.length);
    for (var i = 0; i < s.length; i++) out[i] = s.charCodeAt(i);
    return out;
  }

  // The key a name and word make. Slow on purpose (a third of a second or
  // so on a phone), so a page makes it once a visit and keeps the key, not
  // the word. The key cannot be read back out.
  function key(name, word) {
    // WebCrypto's subtle half exists only on a secure page (https, or the
    // machine's own localhost).
    if (!webcrypto || !webcrypto.subtle) return Promise.reject(new Error('sealing needs an https page'));
    var subtle = webcrypto.subtle;
    return subtle.importKey('raw', utf8(word), 'PBKDF2', false, ['deriveKey']).then(function (base) {
      return subtle.deriveKey(
        { name: 'PBKDF2', hash: 'SHA-256', salt: utf8(name), iterations: ROUNDS },
        base,
        { name: 'AES-GCM', length: 256 },
        false,
        ['encrypt', 'decrypt']
      );
    });
  }

  // True when a node is one of these envelopes (not bsp-mcp's own gray,
  // whose position 9 says "self", or anything else).
  function isSealed(node) {
    var mark = node && typeof node === 'object' ? node['9'] : null;
    return !!(mark && typeof mark === 'object' && mark._ === 'gray' && mark['1'] === 'page' &&
      mark['2'] === SCHEME && typeof node['1'] === 'string' && typeof node['2'] === 'string');
  }

  // Seal anything JSON can carry (a day, the roster's key) into an envelope.
  function seal(value, k) {
    var iv = webcrypto.getRandomValues(new Uint8Array(12));
    return webcrypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, k, utf8(JSON.stringify(value))).then(function (ct) {
      return {
        _: SAID,
        1: toBase64(new Uint8Array(ct)),
        2: toBase64(iv),
        9: { _: 'gray', 1: 'page', 2: SCHEME }
      };
    });
  }

  // Open an envelope with a key. A wrong word (or a different name) fails
  // GCM's check and rejects; nothing half-opened is ever returned.
  function unseal(envelope, k) {
    if (!isSealed(envelope)) return Promise.reject(new Error('not a sealed day'));
    var iv, ct;
    try {
      iv = fromBase64(envelope['2']);
      ct = fromBase64(envelope['1']);
    } catch (e) {
      return Promise.reject(new Error('not a sealed day'));
    }
    return webcrypto.subtle.decrypt({ name: 'AES-GCM', iv: iv }, k, ct).then(function (plain) {
      return JSON.parse(new TextDecoder().decode(plain));
    });
  }

  // ---- The console check ------------------------------------------------------
  // Seal.test() in a browser console: seals a day, opens it again with the
  // same word, and makes sure a wrong word, the right word under another
  // name, and a tampered envelope all fail. The words are made at random
  // for the run and never written anywhere. Resolves true when all pass.
  var DAY = {
    _: 'WEDNESDAY 21 OCTOBER · 9:40 · ARONA — Lizards and the first quiet',
    1: 'A slow morning by the pool. The kids in fits over lizards on the wall, your youngest laughing. The first morning without email. Monday is there, not dreaded, just known.',
    2: 'Tomorrow: walk up to the old town before it gets hot.',
    3: '1',
    4: '2026-10-21|Arona'
  };
  function randomWord() {
    var b = webcrypto.getRandomValues(new Uint8Array(9));
    return Array.prototype.map.call(b, function (x) { return ('0' + x.toString(16)).slice(-2); }).join('');
  }
  function canon(v) {
    if (!v || typeof v !== 'object') return JSON.stringify(v);
    return '{' + Object.keys(v).sort().map(function (k) { return JSON.stringify(k) + ':' + canon(v[k]); }).join(',') + '}';
  }
  function test(log) {
    log = log || function (s) { console.log(s); };
    var name = 'fd-console-check', word = randomWord(), wrong = randomWord();
    var results = [];
    function check(label, ok) {
      results.push(ok);
      log((ok ? 'pass  ' : 'FAIL  ') + label);
    }
    function fails(p) {
      return p.then(function () { return false; }, function () { return true; });
    }
    var started = Date.now(), k, env;
    return key(name, word).then(function (made) {
      k = made;
      check('the key is made (' + (Date.now() - started) + ' ms for ' + ROUNDS + ' rounds)', true);
      return seal(DAY, k);
    }).then(function (e) {
      env = e;
      var flat = JSON.stringify(env);
      check('the envelope has the beach’s gray shape', isSealed(env) && env._ === SAID &&
        Object.keys(env).sort().join() === '1,2,9,_');
      check('the IV is 12 bytes', fromBase64(env['2']).length === 12);
      check('none of the day’s words show in the envelope', flat.indexOf('Lizards') < 0 && flat.indexOf('Arona') < 0);
      return seal(DAY, k);
    }).then(function (again) {
      check('sealing the same day twice gives two different envelopes', again['1'] !== env['1'] && again['2'] !== env['2']);
      return key(name, word).then(function (k2) { return unseal(env, k2); });
    }).then(function (back) {
      check('the same name and word open it, word for word', canon(back) === canon(DAY));
      return key(name, wrong).then(function (k3) { return fails(unseal(env, k3)); });
    }).then(function (ok) {
      check('a wrong word does not open it', ok);
      return key(name + '-other', word).then(function (k4) { return fails(unseal(env, k4)); });
    }).then(function (ok) {
      check('the right word under another name does not open it', ok);
      var bytes = fromBase64(env['1']);
      bytes[0] ^= 1;
      var tampered = { _: env._, 1: toBase64(bytes), 2: env['2'], 9: env['9'] };
      return fails(unseal(tampered, k));
    }).then(function (ok) {
      check('a changed envelope does not open', ok);
      return fails(unseal({ _: 'plain words' }, k));
    }).then(function (ok) {
      check('something not sealed is refused, not misread', ok);
      var all = results.every(Boolean);
      log(all ? 'Seal: all ' + results.length + ' checks pass.' : 'Seal: a check failed.');
      return all;
    }, function (e) {
      log('FAIL  the check stopped: ' + (e && e.message));
      return false;
    });
  }

  var api = {
    SCHEME: SCHEME,
    ROUNDS: ROUNDS,
    key: key,
    seal: seal,
    unseal: unseal,
    isSealed: isSealed,
    test: test
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Seal = api;
})(this);
