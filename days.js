/* Sundial's days on the beach (the plan at spine:front-door 4.1 to 4.6):
   a name claimed by passport:<name>, and each day one plain line in the
   person's own now, now:<name>, at the day's address on the shared clock,
   the same mirror a person keeps through Claude. Anyone can read the days;
   only the word, the edit-latch on both blocks, can write them. The word
   is passed in for one write and kept nowhere here.

   What the beach accepts (M0 checks, findings 7.1952 to 7.1956): a block
   born whole with new_lock is latched at its root; a whole write to a
   block that stands needs confirm:true; new_lock "" with the word lets the
   latch go; a now mirror stands on the clock only when the page lays its
   floor itself, ten nested "_" with the mirror's line innermost. Uses
   beach.js (Beach.look, Beach.write, Beach.clock). */
(function (root) {
  var FORGOTTEN = "Forgotten at the holder's request.";

  function passportLine(name) { return name + ' keeps a day book through the front door.'; }
  function mirrorLine(name) { return name + "'s own now on the shared clock, kept through the front door."; }

  // The mirror's floor: ten nested "_", the line innermost.
  function floorChain(line) {
    var n = line;
    for (var i = 0; i < 10; i++) n = { _: n };
    return n;
  }
  // A node's own words, followed down its "_" (a block's root line).
  function own(node) {
    while (node && typeof node === 'object') node = node._;
    return typeof node === 'string' ? node : '';
  }
  function floorOf(node) {
    var f = 0;
    while (node && typeof node === 'object' && node._ !== undefined) { f++; node = node._; }
    return f;
  }
  function forgotten(node) { return !node || own(node) === FORGOTTEN; }

  // A name as typed, made a handle: lower case, letters, digits and hyphens.
  function handle(typed) {
    return String(typed || '').trim().toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '')
      .replace(/-+/g, '-').replace(/^-|-$/g, '').slice(0, 40);
  }

  // Today's address at the day rung: the shared clock's first eight digits,
  // then two noughts (the clock every beach answer carries).
  function today() {
    var c = Beach.clock();
    return c && /^\d{10}$/.test(c.address) ? c.address.slice(0, 8) + '00' : '';
  }

  // Every day in a mirror, oldest first, as {address, text}: a line at the
  // day rung (depth 8 from the root), reached by digits only. The year's
  // digits may be nought ("_"); the season, month, band and day never are,
  // which keeps the floor's own chain of "_" out.
  function daysIn(mirror) {
    var out = [];
    (function walk(node, digits) {
      if (!node || typeof node !== 'object') return;
      Object.keys(node).forEach(function (k) {
        var d = k === '_' ? '0' : k;
        var ok = digits.length === 0 ? /^[1-9]$/.test(d) : digits.length < 4 ? /^[0-9]$/.test(d) : /^[1-9]$/.test(d);
        if (!ok) return;
        var next = node[k];
        if (digits.length === 7) {
          var text = typeof next === 'string' ? next : own(next);
          if (text) out.push({ address: digits + d + '00', text: text });
        } else {
          walk(next, digits + d);
        }
      });
    })(mirror, '');
    return out.sort(function (a, b) { return a.address < b.address ? -1 : 1; });
  }

  // Who a name is on the beach. The handle keeps the spelling of a block
  // that already stands under it in any case, so a person who keeps their
  // now through Claude reaches the same mirror here. Resolves
  // {name, state, passport, mirror, days}; state is 'free' (nothing stands,
  // or only forgotten lines), 'kept' (a mirror on the clock: the word
  // decides), or 'taken' (the name is someone's in a shape this page
  // doesn't write to). Rejects when the beach can't be reached.
  function resolve(typed) {
    var h = handle(typed);
    if (!h) return Promise.resolve(null);
    return Beach.look('').then(function (index) {
      if (!index.status) throw new Error('unreachable');
      var spelled = h;
      ((index.json && index.json.blocks) || []).some(function (b) {
        var m = /^(passport|now):(.+)$/.exec(b);
        if (m && m[2].toLowerCase() === h) { spelled = m[2]; return true; }
        return false;
      });
      return Promise.all([Beach.look('passport:' + spelled), Beach.look('now:' + spelled)]).then(function (both) {
        var p = both[0], n = both[1];
        if (!p.status || !n.status) throw new Error('unreachable');
        var passport = p.status === 200 ? p.json : null, mirror = n.status === 200 ? n.json : null;
        var state = forgotten(passport) && forgotten(mirror) ? 'free'
          : mirror && !forgotten(mirror) && floorOf(mirror) === 10 ? 'kept' : 'taken';
        return { name: spelled, state: state, passport: passport, mirror: mirror,
          days: state === 'kept' ? daysIn(mirror) : [] };
      });
    });
  }

  // A name's days, read with no word. Resolves [] when nothing stands;
  // rejects when the beach can't be reached.
  function read(name) {
    return Beach.look('now:' + name).then(function (r) {
      if (!r.status) throw new Error('unreachable');
      return r.status === 200 && floorOf(r.json) === 10 ? daysIn(r.json) : [];
    });
  }

  // Keep a day: on a free name, claim it (the passport, then the mirror on
  // the clock, both latched with the word), then write the day's text at
  // today's address and read it back. Resolves 'kept', 'wrong' (the word
  // doesn't open it), 'taken', or 'unreachable'.
  function keep(who, word, text) {
    var name = who.name;
    function failed(r) { return r.status === 403 ? 'wrong' : r.status ? 'taken' : 'unreachable'; }
    var claim = Promise.resolve(null);
    if (who.state === 'taken') return Promise.resolve('taken');
    if (who.state === 'free') {
      claim = Beach.write('passport:' + name, { content: { _: passportLine(name) }, new_lock: word, confirm: !!who.passport })
        .then(function (r) {
          if (r.status !== 200) throw failed(r);
          return Beach.write('now:' + name, { content: floorChain(mirrorLine(name)), new_lock: word, confirm: !!who.mirror });
        }).then(function (r) {
          if (r.status !== 200) throw failed(r);
        });
    }
    var at;
    return claim.then(function () {
      at = today();
      if (!at) return Beach.look('now:' + name, '&pscale=0').then(function () { at = today(); });
    }).then(function () {
      if (!at) throw 'unreachable';
      return Beach.write('now:' + name, { spindle: at, content: text, secret: word });
    }).then(function (r) {
      if (r.status !== 200) throw failed(r);
      return Beach.look('now:' + name, '&spindle=' + at);
    }).then(function (r) {
      return r.status === 200 && r.json === text ? 'kept' : 'unreachable';
    }).catch(function (e) {
      return typeof e === 'string' ? e : 'unreachable';
    });
  }

  // Forget me (plan 4.5): each block this page made for the name (its
  // passport line, its mirror line) is written whole as the one forgotten
  // line with the word, then its latch let go, so the name is free again.
  // A block the page didn't make, such as a mirror kept through Claude long
  // before, is left as it stands. Resolves 'forgotten', 'wrong' or
  // 'unreachable'.
  function forget(who, word) {
    var mine = [];
    if (who.passport && own(who.passport) === passportLine(who.name)) mine.push('passport:' + who.name);
    if (who.mirror && own(who.mirror) === mirrorLine(who.name)) mine.push('now:' + who.name);
    return mine.reduce(function (p, block) {
      return p.then(function () {
        return Beach.write(block, { content: { _: FORGOTTEN }, secret: word, confirm: true });
      }).then(function (r) {
        if (r.status !== 200) throw r.status === 403 ? 'wrong' : 'unreachable';
        return Beach.write(block, { new_lock: '', secret: word });
      }).then(function (r) {
        if (r.status !== 200) throw 'unreachable';
      });
    }, Promise.resolve()).then(function () { return 'forgotten'; }, function (e) {
      return typeof e === 'string' ? e : 'unreachable';
    });
  }

  var api = {
    FORGOTTEN: FORGOTTEN,
    handle: handle,
    today: today,
    daysIn: daysIn,
    resolve: resolve,
    read: read,
    keep: keep,
    forget: forget,
    passportLine: passportLine,
    mirrorLine: mirrorLine
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Days = api;
})(this);
