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

  // The gray module loads after the page's own scripts; wait for it, but
  // not for ever: should it never load, a whisper comes back to its field.
  function gray() {
    if (root.Gray) return Promise.resolve(root.Gray);
    return new Promise(function (ok, no) {
      var late = setTimeout(function () { no(new Error('the seal did not load')); }, 20000);
      root.addEventListener('gray-ready', function () { clearTimeout(late); ok(root.Gray); }, { once: true });
    });
  }

  // Every sealed envelope in a block, wherever the supernesting has put it,
  // with its address (the digits down to it, '_' read as 0).
  function sealedAt(block) {
    var out = [];
    (function walk(node, at) {
      if (!node || typeof node !== 'object') return;
      if (node['9'] && typeof node['9'] === 'object' && node['9']._ === 'gray' && node['9']['1'] === 'self') { out.push({ node: node, at: at }); return; }
      Object.keys(node).forEach(function (k) { if (k !== '9') walk(node[k], at + (k === '_' ? '0' : k)); });
    })(block, '');
    return out;
  }
  function sealedIn(block) {
    return sealedAt(block).map(function (s) { return s.node; });
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
  // it. An open day book proves nothing, and no stash is made. A hand
  // whose name has an older word from before Sundial (6.8.3.3 (2)) gives
  // it once, as `older`, sent alongside to make the stash under their one
  // word, and kept nowhere. Resolves 'kept'; 'wrong' (the word doesn't open
  // the day book, so no stash is made); 'passport' (the stash can't be made
  // without the name's older word); 'stash' (a standing stash refuses the
  // word: it is kept under an older word, see bringUnder); or 'unreachable'.
  function keep(name, word, whisper, older) {
    var entry = { _: MARK + whisper.phrase + ' · ' + whisper.date, 1: whisper.said, 2: whisper.asked || '' };
    var why = 'stash';
    function failed(r) { return r.status === 403 ? why : 'unreachable'; }
    return Beach.look('stash:' + name).then(function (r) {
      if (!r.status) throw 'unreachable';
      if (r.status === 200) return null;
      why = 'wrong';
      return Beach.write('now:' + name, { new_lock: '' }).then(function (w) {
        if (w.status === 200) throw 'wrong';
        if (w.status !== 403) throw 'unreachable';
        return Beach.write('now:' + name, { secret: word, new_lock: word });
      }).then(function (w) {
        if (w.status !== 200) throw failed(w);
        why = 'passport';
        var make = { content: { _: stashLine(name) }, new_lock: word };
        if (older) make.secret = older;
        return Beach.write('stash:' + name, make);
      }).then(function (w) {
        if (w.status !== 200) throw failed(w);
        why = 'stash';
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

  // One name, one word (6.8.3.3 (1)): a stash kept under an older word is
  // brought under the person's one word, with the older word given once by
  // its holder and kept nowhere. The older word first proves itself (its
  // lock turned to itself changes nothing); then every whisper it opens is
  // sealed again with the one word, in place, its arrival stamp kept, and
  // read back; then the lock is turned to the one word. Each whisper is
  // sealed again while the older word still holds the lock, so a visit cut
  // short loses nothing: the older word still opens what remains. Envelopes
  // the older word doesn't open are left as they are. Resolves 'done',
  // 'wrong' (the older word doesn't open it) or 'unreachable'.
  function bringUnder(name, older, word) {
    var block = 'stash:' + name;
    return Promise.all([Beach.look(block), gray()]).then(function (both) {
      var r = both[0], Gray = both[1];
      if (r.status !== 200 || !r.json) throw 'unreachable';
      return Beach.write(block, { secret: older, new_lock: older }).then(function (w) {
        if (w.status === 403) throw 'wrong';
        if (w.status !== 200) throw 'unreachable';
        return sealedAt(r.json).reduce(function (p, s) {
          return p.then(function () {
            return Gray.open(s.node, older);
          }).then(function (text) {
            var entry = null;
            try { entry = text == null ? null : JSON.parse(text); } catch (x) { entry = null; }
            if (!entry) return null;
            return Gray.seal(entry, word).then(function (env) {
              if (s.node['3'] !== undefined) env['3'] = s.node['3'];
              return Beach.write(block, { spindle: s.at, content: env, secret: older }).then(function (w) {
                if (w.status !== 200) throw 'unreachable';
                return Beach.look(block, '&spindle=' + s.at);
              }).then(function (b) {
                if (b.status !== 200 || !b.json || b.json['1'] !== env['1']) throw 'unreachable';
              });
            });
          });
        }, Promise.resolve());
      }).then(function () {
        return Beach.write(block, { secret: older, new_lock: word });
      }).then(function (w) {
        if (w.status !== 200) throw w.status === 403 ? 'wrong' : 'unreachable';
        return 'done';
      });
    }).catch(function (e) {
      return typeof e === 'string' ? e : 'unreachable';
    });
  }

  // Begin making the key for a word, so it is ready when it is needed.
  function warm(word) {
    gray().then(function (Gray) { Gray.key(word, Gray.BEACH).catch(function () {}); });
  }

  var api = { MARK: MARK, stashLine: stashLine, sealedIn: sealedIn, sealedAt: sealedAt, count: count, read: read, keep: keep, bringUnder: bringUnder, warm: warm };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Whispers = api;
})(this);
