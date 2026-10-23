/* Sundial's words left on the About sheet (the plan at spine:front-door
   6.1.3; the family at spine:front-door 9.1 and function:front-door 9.1).
   A person's words on a part of Sundial live in their own mirror,
   front-door:<name>, at the spine's own address for that part: 2.6 for
   Sundial as a whole, 3.61 to 3.67 for the stops on the road. Each is a
   plain line, public and under their name; nobody writes in anyone else's
   mirror. Writing at the same place again replaces the line.

   Each word keeps its own date (6.1.3.1): it is written at its place as
   {_: <their words>, 1: <the ISO time it was left>}, and leaving again
   replaces both. An older plain line still reads, dated from when the
   beach last saw the mirror change (the index's touched).

   Reading lists the beach's blocks, takes every front-door:<name> and
   reads each mirror once; a mirror that can't be read is passed over, and
   so is every trial name (one beginning 'trial-'). Uses beach.js; the word
   is passed in for one call and kept nowhere here. */
(function (root) {
  var PREFIX = 'front-door:';
  var PLACES = ['2.6', '3.61', '3.62', '3.63', '3.64', '3.65', '3.66', '3.67'];
  // Test words are left only on trial names, and never shown (6.1.3.1).
  function trial(name) { return /^trial-/i.test(String(name)); }

  function mirrorLine(name) {
    return name + "'s words on Sundial, at the spine's own addresses (spine:front-door 9.1).";
  }

  // The line at an address in a mirror: a plain string, or the address's
  // own line when someone has since written beneath it. A sealed (gray)
  // entry, an empty line or anything else counts for nothing; the
  // mirror's own underscore is never an address.
  function nodeAt(mirror, address) {
    var node = mirror;
    String(address).replace(/\./g, '').split('').forEach(function (k) {
      node = node && typeof node === 'object' ? node[k] : null;
    });
    return node;
  }
  function lineAt(mirror, address) {
    var node = nodeAt(mirror, address);
    if (node && typeof node === 'object') {
      if (node['9'] && typeof node['9'] === 'object' && node['9']._ === 'gray') return '';
      node = node._;
    }
    return typeof node === 'string' ? node.trim() : '';
  }
  // The time a word was left, from its own 1 (6.1.3.1); '' for an older
  // plain line, or where 1 holds anything but a time.
  function dateAt(mirror, address) {
    var node = nodeAt(mirror, address), t = node && typeof node === 'object' ? node['1'] : null;
    return typeof t === 'string' && /^\d{4}-\d\d-\d\dT\d\d:\d\d/.test(t) && !isNaN(Date.parse(t)) ? t : '';
  }

  // Every mirror's words at the places, as [{name, when, at: {address:
  // line}, dates: {address: time}}]: `when` is the mirror's last change,
  // for lines with no date of their own. Rejects when the beach's index
  // can't be read.
  function read() {
    return Beach.look('').then(function (r) {
      if (r.status !== 200 || !r.json || !Array.isArray(r.json.blocks)) throw new Error('unreachable');
      var touched = r.json.touched || {}, born = r.json.born || {};
      var names = r.json.blocks.map(function (b) { return typeof b === 'string' ? b : b && b.name; })
        .filter(function (b) { return typeof b === 'string' && b.indexOf(PREFIX) === 0 && b.length > PREFIX.length && !trial(b.slice(PREFIX.length)); });
      return Promise.all(names.map(function (block) {
        return Beach.look(block).then(function (m) {
          if (m.status !== 200 || !m.json || typeof m.json !== 'object') return null;
          var at = {}, dates = {}, any = false;
          PLACES.forEach(function (a) {
            var line = lineAt(m.json, a);
            if (line) {
              at[a] = line;
              dates[a] = dateAt(m.json, a);
              any = true;
            }
          });
          return any ? { name: block.slice(PREFIX.length), when: touched[block] || born[block] || '', at: at, dates: dates } : null;
        });
      })).then(function (all) { return all.filter(Boolean); });
    });
  }

  // Leave a person's words at a place in their mirror. Anyone can look at
  // a name's days, so a mirror is only made once the word is proved on the
  // person's own day book, now:<name>, exactly as whispers.js proves it:
  // letting go of its lock with no word must be refused (403); then the
  // lock "rotated" to the same word must answer 200. The mirror is then
  // made whole, latched with the word, and the words written as a point at
  // the place, with the time it was left (6.1.3.1). The beach lets a block
  // named for a hand be latched only with that hand's passport key: for a
  // Sundial keeper that is the day-book word, so nothing changes; a hand
  // whose passport has another key is refused, and the page asks once for
  // it (6.1.3.2.1), sent here as the secret with new_lock their day-book
  // word, and kept nowhere. Resolves 'left'; 'wrong' when the word doesn't
  // open the day book (or the day book proves nothing); 'passport' when the
  // mirror can't be made without the passport's key; 'refused' when the
  // mirror stands under another word; or 'unreachable'. `when` is the ISO
  // time to write beside the words; `passportKey` is '' unless asked. With
  // `prove`, for a name given in this browser (6.1.3.2), the word is proved
  // on the day book even when the mirror already stands.
  function leave(name, word, address, text, when, passportKey, prove) {
    var block = PREFIX + name;
    function failed(w, why) { return w.status === 403 ? why : 'unreachable'; }
    var stands = false;
    return Beach.look(block).then(function (r) {
      if (r.status !== 200 && r.status !== 404) throw 'unreachable';
      stands = r.status === 200;
      if (stands && !prove) return null;
      return Beach.write('now:' + name, { new_lock: '' }).then(function (w) {
        if (w.status === 200 || w.status === 404) throw 'wrong';
        if (w.status !== 403) throw 'unreachable';
        return Beach.write('now:' + name, { secret: word, new_lock: word });
      }).then(function (w) {
        if (w.status !== 200) throw failed(w, 'wrong');
        if (stands) return { status: 200 };
        var make = { content: { _: mirrorLine(name) }, new_lock: word };
        if (passportKey) make.secret = passportKey;
        return Beach.write(block, make);
      }).then(function (w) {
        if (w.status !== 200) throw failed(w, 'passport');
      });
    }).then(function () {
      return Beach.write(block, { spindle: address, content: { _: text, 1: when || new Date().toISOString() }, secret: word });
    }).then(function (w) {
      if (w.status !== 200) throw failed(w, 'refused');
      return 'left';
    }).catch(function (e) {
      return typeof e === 'string' ? e : 'unreachable';
    });
  }

  var api = { PREFIX: PREFIX, PLACES: PLACES, trial: trial, mirrorLine: mirrorLine, lineAt: lineAt, dateAt: dateAt, read: read, leave: leave };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Words = api;
})(this);
