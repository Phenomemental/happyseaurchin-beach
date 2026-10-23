/* Sundial's whispers (the plan at spine:front-door 6.8.3): what a person
   whispers back into a shell, sealed with their word in the beach's own
   gray (gray.js) and appended to their own stash, stash:<name> — the
   deliberate sibling of history in every hand's standard shell
   (block-conventions 3.7). Inside the seal an entry is
   {_: "whisper · <pool's phrase> · <date>", 1: <their words>, 2: <the
   question it answers>}; outside, only the seal shows, and the beach's
   arrival stamp. The connector opens them in Claude with the same word.

   A stash that doesn't stand yet is born as the standard shell's genome
   gives it (pscale shell-genome 5 and 1), locked with the word. Entries
   land by append, never at a computed slot. The word is passed in for one
   call and kept nowhere here. Uses beach.js and gray.js. */
(function (root) {
  var MARK = 'whisper · ';
  var LAW = 'THE ACCUMULATION LAW — how history and stash grow, and the one law they share. Entries land by APPEND — ' +
    "bsp(agent_id='<handle>', block='<name>', append=true, content=<entry>) — never at a computed slot: the substrate " +
    'allocates the next zero-free number, supernests at each all-nines boundary, and clears nothing, ever. Zero-carrying ' +
    'numbers are summary slots, never entries — the voicing law at 3. To deepen an existing entry, write beneath its ' +
    "digit. The rungs below carry the law's whys: the slip arithmetic invites (1), what the wrap preserves (2), what the " +
    'summary is (3).';

  function stashLine(name) {
    return name + "'s stash — deliberate notes kept by choice: ideas, principles, and runbooks worth refinding. " +
      'The intentional sibling of history:' + name + ', which logs automatically. ' + LAW;
  }

  // The gray module loads after the page's own scripts; wait for it.
  function gray() {
    if (root.Gray) return Promise.resolve(root.Gray);
    return new Promise(function (ok) { root.addEventListener('gray-ready', function () { ok(root.Gray); }, { once: true }); });
  }

  // Every sealed envelope in a block, wherever the supernesting has put it.
  function sealedIn(block) {
    var out = [];
    (function walk(node) {
      if (!node || typeof node !== 'object') return;
      if (node['9'] && typeof node['9'] === 'object' && node['9']._ === 'gray' && node['9']['1'] === 'self') { out.push(node); return; }
      Object.keys(node).forEach(function (k) { if (k !== '9') walk(node[k]); });
    })(block);
    return out;
  }

  // How many sealed entries a person's stash holds, with no word: 0 when
  // there is no stash; -1 when the beach can't be reached.
  function count(name) {
    return Beach.look('stash:' + name).then(function (r) {
      if (!r.status) return -1;
      return r.status === 200 ? sealedIn(r.json).length : 0;
    });
  }

  // A person's whispers, opened with their word, oldest first, as
  // {phrase, date, said, asked, at}. Entries that aren't whispers, or that
  // the word doesn't open, are passed over. Rejects when the beach can't
  // be reached.
  function read(name, word) {
    return Promise.all([Beach.look('stash:' + name), gray()]).then(function (both) {
      var r = both[0], Gray = both[1];
      if (!r.status) throw new Error('unreachable');
      if (r.status !== 200) return [];
      return Promise.all(sealedIn(r.json).map(function (env) {
        return Gray.open(env, word).then(function (text) {
          var e;
          try { e = JSON.parse(text); } catch (x) { return null; }
          if (!e || typeof e._ !== 'string' || e._.indexOf(MARK) !== 0 || typeof e['1'] !== 'string') return null;
          var parts = e._.slice(MARK.length).split(' · ');
          return { phrase: parts[0] || '', date: parts[1] || '', said: e['1'], asked: typeof e['2'] === 'string' ? e['2'] : '',
            at: typeof env['3'] === 'string' ? env['3'] : '' };
        });
      })).then(function (all) {
        return all.filter(Boolean).sort(function (a, b) { return a.at < b.at ? -1 : a.at > b.at ? 1 : 0; });
      });
    });
  }

  // Seal a whisper and append it to the stash, making the stash first if
  // it doesn't stand. Anyone can look at a name's days, so a stash is only
  // made once the word is proved on the person's own day book, now:<name>,
  // in two steps that change nothing (checked on the trial name, 23
  // October 2026): letting go of its lock with no word is refused when it
  // is locked (403) and does nothing when it is open (200); then the lock
  // "rotated" to the same word answers 200 only for the word that holds
  // it. An open day book proves nothing, and no stash is made. Resolves
  // 'kept', 'wrong' (the word doesn't open the lock) or 'unreachable'.
  function keep(name, word, whisper) {
    var entry = { _: MARK + whisper.phrase + ' · ' + whisper.date, 1: whisper.said, 2: whisper.asked || '' };
    function failed(r) { return r.status === 403 ? 'wrong' : 'unreachable'; }
    return Beach.look('stash:' + name).then(function (r) {
      if (!r.status) throw 'unreachable';
      if (r.status === 200) return null;
      return Beach.write('now:' + name, { new_lock: '' }).then(function (w) {
        if (w.status === 200) throw 'wrong';
        if (w.status !== 403) throw 'unreachable';
        return Beach.write('now:' + name, { secret: word, new_lock: word });
      }).then(function (w) {
        if (w.status !== 200) throw failed(w);
        return Beach.write('stash:' + name, { content: { _: stashLine(name) }, new_lock: word });
      }).then(function (w) {
        if (w.status !== 200) throw failed(w);
      });
    }).then(function () {
      return gray();
    }).then(function (Gray) {
      return Gray.seal(entry, word);
    }).then(function (envelope) {
      return Beach.write('stash:' + name, { append: true, content: envelope, secret: word });
    }).then(function (w) {
      if (w.status !== 200) throw failed(w);
      return 'kept';
    }).catch(function (e) {
      return typeof e === 'string' ? e : 'unreachable';
    });
  }

  // Begin making the key for a word, so it is ready when it is needed.
  function warm(word) {
    gray().then(function (Gray) { Gray.key(word, Gray.BEACH).catch(function () {}); });
  }

  var api = { MARK: MARK, stashLine: stashLine, sealedIn: sealedIn, count: count, read: read, keep: keep, warm: warm };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Whispers = api;
})(this);
