/* Sundial's roster (the plan at spine:front-door 3, 4.3 and 6.6): one
   Claude API key, sealed in the browser with a roster passphrase and kept
   on the beach at roster:front-door, latched with the steward's own word.
   A person given the passphrase types it once; the page reads the block,
   opens the key here and keeps it in this browser. Nothing in this file
   holds a key, a word or a passphrase; each is passed in for one call.

   The plaintext sealed is { "_": "api", "1": "<key>" }, with seal.js's
   scheme salted with the block's name. Passphrases and the steward's word
   are trimmed, since a phone's keyboard may add a space; the page is the
   only writer, so sealing and opening agree.

   What the beach accepts (checked on the trial name, 22 October 2026): a
   lock "rotated" to the same word with that word answers 200 and leaves
   the latch as it was; with any other word, 403. So the steward's word is
   proved without changing anything. Uses beach.js and seal.js. */
(function (root) {
  var BLOCK = 'roster:front-door';

  function settle(s) { return String(s || '').trim(); }

  // The roster as it stands: 'sealed' (an envelope to open), 'open' (no
  // block yet, or a block with no envelope, so the steward's first seal
  // makes it), or 'unreachable'.
  function read() {
    return Beach.look(BLOCK).then(function (r) {
      if (!r.status) return { state: 'unreachable' };
      if (r.status === 200 && Seal.isSealed(r.json)) return { state: 'sealed', envelope: r.json, stands: true };
      return { state: 'open', stands: r.status === 200 };
    });
  }

  // The key a passphrase opens, or '' (a wrong passphrase, no roster, or
  // the beach out of reach: all the same to the person). Resolves
  // {key, roster} so the page can go on to ask whether it's the steward's.
  function open(passphrase) {
    var p = settle(passphrase);
    return read().then(function (roster) {
      if (!p || roster.state !== 'sealed') return { key: '', roster: roster };
      return Seal.key(BLOCK, p).then(function (k) { return Seal.unseal(roster.envelope, k); }).then(function (plain) {
        var key = plain && plain._ === 'api' && typeof plain['1'] === 'string' ? plain['1'] : '';
        return { key: key, roster: roster };
      }, function () {
        return { key: '', roster: roster };
      });
    });
  }

  // Is this the steward's word? On a sealed roster, only the word that
  // latches it (proved by the same-word rotation). Before the first seal
  // there is no latch to prove, so the page says whether this browser is
  // the steward's (firstSeal) and the first seal latches the block.
  function steward(word, roster, firstSeal) {
    var w = settle(word);
    if (!w || !roster || roster.state === 'unreachable') return Promise.resolve(false);
    if (roster.state === 'open') return Promise.resolve(!!firstSeal);
    return Beach.write(BLOCK, { secret: w, new_lock: w }).then(function (r) { return r.status === 200; });
  }

  // The steward seals a key with a passphrase to give out: the envelope is
  // written whole (latched with the word on the first seal, opened with it
  // after), read back and opened again before it counts. Resolves 'sealed',
  // 'wrong' (the word no longer opens the latch) or 'unreachable'.
  function seal(word, apiKey, passphrase) {
    var w = settle(word), p = settle(passphrase), key = settle(apiKey), made;
    return read().then(function (roster) {
      if (roster.state === 'unreachable') throw 'unreachable';
      return Seal.key(BLOCK, p).then(function (k) {
        made = k;
        return Seal.seal({ _: 'api', 1: key }, k);
      }).then(function (envelope) {
        var body = roster.state === 'sealed'
          ? { content: envelope, secret: w, confirm: true }
          : { content: envelope, new_lock: w, confirm: !!roster.stands };
        return Beach.write(BLOCK, body);
      });
    }).then(function (r) {
      if (r.status !== 200) throw r.status === 403 ? 'wrong' : 'unreachable';
      return Beach.look(BLOCK);
    }).then(function (r) {
      if (r.status !== 200) throw 'unreachable';
      return Seal.unseal(r.json, made);
    }).then(function (plain) {
      return plain && plain['1'] === key ? 'sealed' : 'unreachable';
    }).catch(function (e) {
      return typeof e === 'string' ? e : 'unreachable';
    });
  }

  var api = { BLOCK: BLOCK, read: read, open: open, steward: steward, seal: seal };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Roster = api;
})(this);
