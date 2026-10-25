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

  // The day's nine parts, as the beach's clock names them (bsp-mcp's
  // src/temporal.ts): each 2h40m of the UTC day, each of nine beats.
  var PARTS = ['deep night', 'dawn', 'early morning', 'morning', 'midday',
    'afternoon', 'late afternoon', 'evening', 'night'];
  var PART_S = 86400 / 9, BEAT_S = PART_S / 9;

  // The ten-digit address of a moment, by the clock's own arithmetic, so the
  // page can name the part of the day before the beach has answered.
  function addressAt(when) {
    var y = when.getUTCFullYear(), m = when.getUTCMonth(), d = when.getUTCDate();
    var s = when.getUTCHours() * 3600 + when.getUTCMinutes() * 60 + when.getUTCSeconds();
    var part = Math.floor(s / PART_S), beat = Math.floor((s - part * PART_S) / BEAT_S);
    return String(y) + (Math.floor(m / 3) + 1) + (m % 3 + 1) + (Math.floor((d - 1) / 7) + 1) + ((d - 1) % 7 + 1) +
      (part + 1) + (beat + 1);
  }
  // This beat's address: the beach's clock when it has spoken, else the
  // phone's.
  function now() {
    var c = Beach.clock();
    return c && /^\d{10}$/.test(c.address) ? c.address : addressAt(new Date());
  }
  // The part of the day an address falls in ('' at the day rung or above).
  function partOf(address) {
    var g = Number(String(address).charAt(8));
    return g >= 1 && g <= 9 ? PARTS[g - 1] : '';
  }
  // The middle of the beat an address names, as a time.
  function timeOf(address) {
    var a = String(address), day = dateOf(a.slice(0, 8) + '00');
    var g = Number(a.charAt(8)) || 1, b = Number(a.charAt(9)) || 5;
    return new Date(Date.UTC(day.getFullYear(), day.getMonth(), day.getDate()) +
      ((g - 1) * PART_S + (b - 1) * BEAT_S + BEAT_S / 2) * 1000);
  }
  // The day after a day's address.
  function tomorrowOf(address) {
    var d = dateOf(address);
    d.setDate(d.getDate() + 1);
    return addressAt(new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate(), 12))).slice(0, 8) + '00';
  }

  // The calendar date of a day's address (pscale://sundial 3.1): the year's
  // four digits, then season 1-4 (three months each), month of the season
  // 1-3, seven-day band of the month 1-5, day of the band 1-7. Noon, so no
  // clock's offset moves it to another day.
  function dateOf(address) {
    var a = String(address);
    return new Date(Number(a.slice(0, 4)), (Number(a[4]) - 1) * 3 + Number(a[5]) - 1,
      (Number(a[6]) - 1) * 7 + Number(a[7]), 12);
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

  // Every moment in a mirror, oldest first, as {address, text}: a line at a
  // beat (depth 10) or a part of the day (depth 9) beneath a day.
  function momentsIn(mirror) {
    var out = [];
    (function walk(node, digits) {
      if (!node || typeof node !== 'object') return;
      Object.keys(node).forEach(function (k) {
        var d = k === '_' ? '0' : k;
        var ok = digits.length === 0 ? /^[1-9]$/.test(d) : digits.length < 4 ? /^[0-9]$/.test(d) : /^[1-9]$/.test(d);
        if (!ok) return;
        var next = node[k];
        if (digits.length < 8) return walk(next, digits + d);
        // digits.length is 8 (d is the part) or 9 (d is the beat).
        var text = typeof next === 'string' ? next : own(next);
        if (digits.length === 9) {
          if (text) out.push({ address: digits + d, text: text });
          return;
        }
        if (text) out.push({ address: digits + d + '0', text: text });
        if (next && typeof next === 'object') walk(next, digits + d);
      });
    })(mirror, '');
    return out.sort(function (a, b) { return a.address < b.address ? -1 : 1; });
  }

  // Any day is a day (plan 6.4.1.1 (4)): a date with moments but no day
  // line of its own, written by any tool, is still a day; its text is its
  // moments joined in time order, each without its places line, so it
  // names no places of its own until a Claude places it.
  function bare(text) {
    var rows = String(text).split('\n');
    if (rows.length > 1 && root.Mark && root.Mark.read(text).places.length) rows.pop();
    return rows.join('\n');
  }
  function withMomentDays(days, moments) {
    var have = {}, by = {}, order = [];
    days.forEach(function (d) { have[d.address] = 1; });
    moments.forEach(function (m) {
      var a = m.address.slice(0, 8) + '00';
      if (have[a]) return;
      if (!by[a]) { by[a] = []; order.push(a); }
      by[a].push(bare(m.text));
    });
    return days.concat(order.map(function (a) { return { address: a, text: by[a].join('\n'), bare: true }; }))
      .sort(function (a, b) { return a.address < b.address ? -1 : 1; });
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
        var moments = state === 'kept' ? momentsIn(mirror) : [];
        return { name: spelled, state: state, passport: passport, mirror: mirror,
          days: state === 'kept' ? withMomentDays(daysIn(mirror), moments) : [], moments: moments };
      });
    });
  }

  // A name's days, read with no word. Resolves [] when nothing stands;
  // rejects when the beach can't be reached.
  function read(name) {
    return readAll(name).then(function (all) { return all.days; });
  }
  // A name's days and moments together, as {days, moments, rungs}: rungs
  // are the words standing over today (rungsIn).
  function readAll(name) {
    return Beach.look('now:' + name).then(function (r) {
      if (!r.status) throw new Error('unreachable');
      var mirror = r.status === 200 && floorOf(r.json) === 10 ? r.json : null;
      var moments = mirror ? momentsIn(mirror) : [];
      return { days: mirror ? withMomentDays(daysIn(mirror), moments) : [], moments: moments,
        rungs: mirror ? rungsIn(mirror, today()) : {} };
    });
  }

  // The words standing at the coarser rungs over a day, read down its own
  // digits, as {4: year, 5: season, 6: month, 7: week}, '' where none.
  function rungsIn(mirror, address) {
    var out = {}, node = mirror, a = String(address || '');
    if (!/^\d{10}$/.test(a)) return out;
    for (var i = 0; i < 7; i++) {
      node = node && typeof node === 'object' ? node[a.charAt(i) === '0' ? '_' : a.charAt(i)] : null;
      if (i >= 3) out[i + 1] = own(node);
    }
    return out;
  }
  // A rung's address: the day's first digits, then noughts (the week of
  // 2026414300 is 2026414000).
  function rungOf(address, digits) {
    return String(address).slice(0, digits) + '0000000000'.slice(digits);
  }

  // A rung of their own now set in their own words (spine:front-door 3.82):
  // the year, season, month or week (4 to 7 of the day's digits) the day
  // falls in, written only where nothing stands, and read back before it
  // counts. Resolves 'kept'; 'stands' (words were already there, nothing
  // written); 'wrong' (the word doesn't open it); or 'unreachable'.
  function setRung(name, word, digits, address, text) {
    var at = rungOf(address, digits);
    return Beach.look('now:' + name, '&spindle=' + at).then(function (r) {
      if (!r.status || r.status >= 500) throw 'unreachable';
      if (r.status === 200 && own(r.json)) return 'stands';
      return Beach.write('now:' + name, { spindle: at, content: text, secret: word }).then(function (w) {
        if (w.status !== 200) throw w.status === 403 ? 'wrong' : 'unreachable';
        return Beach.look('now:' + name, '&spindle=' + at);
      }).then(function (back) {
        if (!(back.status === 200 && own(back.json) === text)) throw 'unreachable';
        return 'kept';
      });
    }).catch(function (e) {
      return typeof e === 'string' ? e : 'unreachable';
    });
  }

  // An intention sized for this week or this month (function:ahead): a card
  // in the person's own hand in their now, ahead:now:<name>, at a free slot
  // of that rung's pile (3 this week, 4 this month): their sentence, with
  // the stamp of the change beneath it, and nothing else (a card in the
  // holder's own words carries no dealer). A pile holds nine and is never
  // appended to: an empty slot first, then one whose card is done or
  // dropped. The hand is made whole, latched with the word, if it doesn't
  // stand yet. Read back before it counts. Resolves 'kept'; 'full' (nine
  // standing at that rung); 'wrong'; or 'unreachable'.
  var PILES = { week: '3', month: '4' };
  function handLine(name) {
    return 'AHEAD \u2014 ' + name + "'s hand in their own now: cards held toward doing, each at the rung it is sized for, nine to a rung, " +
      'unordered within it, never arriving unless pulled into doing and landed as record in now:' + name +
      '. Sovereign; a rung is appetite, not sequence (function:ahead).';
  }
  function card(name, word, rung, sentence) {
    var pile = PILES[rung], block = 'ahead:now:' + name, at = '';
    if (!pile || !sentence) return Promise.resolve('unreachable');
    function failed(w) { return w.status === 403 ? 'wrong' : 'unreachable'; }
    return Beach.look(block).then(function (r) {
      if (r.status === 404) {
        return Beach.write(block, { content: { _: handLine(name) }, new_lock: word }).then(function (w) {
          if (w.status !== 200) throw failed(w);
          return {};
        });
      }
      if (r.status !== 200 || !r.json || typeof r.json !== 'object') throw 'unreachable';
      return r.json;
    }).then(function (hand) {
      var row = hand[pile], i, c;
      if (row != null && typeof row !== 'object' && own(row)) throw 'unreachable';
      row = row && typeof row === 'object' ? row : {};
      for (i = 1; i <= 9 && !at; i++) if (!own(row[String(i)])) at = pile + '.' + i;
      for (i = 1; i <= 9 && !at; i++) {
        c = row[String(i)];
        if (c && typeof c === 'object' && (c['4'] === 'done' || c['4'] === 'drop')) at = pile + '.' + i;
      }
      if (!at) throw 'full';
      var clock = Beach.clock(), stamp = (clock && clock.iso ? clock.iso : new Date().toISOString()).slice(0, 16) + 'Z';
      return Beach.write(block, { spindle: at, content: { _: sentence, 3: stamp }, secret: word });
    }).then(function (w) {
      if (w.status !== 200) throw failed(w);
      return Beach.look(block, '&spindle=' + at);
    }).then(function (back) {
      if (!(back.status === 200 && own(back.json) === sentence)) throw 'unreachable';
      return 'kept';
    }).catch(function (e) {
      return typeof e === 'string' ? e : 'unreachable';
    });
  }

  // A line Sundial wrote (plan 6.4.1 (3); function:front-door 4.7): one
  // whose last line names its places, as Mark.write ends it, or 'In your
  // own words.' (6.8.2.1.1), or one this browser wrote in the person's own
  // words before that line began, still standing exactly as it was written
  // (kept.wrote(address, text), from the page). Any other line is theirs,
  // written in their own voice, and is never rewritten.
  function sundials(text, at, wrote) {
    var r = root.Mark ? root.Mark.read(text) : null;
    if (r && (r.places.length || r.own)) return true;
    return typeof wrote === 'function' && !!wrote(at, text);
  }

  // Keep what was said (plan 4.2): on a free name, claim it first (the
  // passport, then the mirror on the clock, both latched with the word);
  // a name whose now already stands on the clock is someone's own and
  // nothing is made for it. Then the moment at this beat; the day's own
  // line at today, and tomorrow's line when one was voiced, each only where
  // that place is empty or already holds a line Sundial wrote (above);
  // otherwise their line is left exactly as it stands and the moment kept
  // alone (6.4.1 (3)). Each write is read back before it counts. `kept` is
  // {moment, day, ahead, wrote}, the first three a text or '' (a plain
  // text, as before, is the day alone). Resolves 'kept', 'wrong' (the word
  // doesn't open it), 'taken', or 'unreachable'.
  function keep(who, word, kept) {
    var name = who.name;
    if (typeof kept === 'string') kept = { moment: '', day: kept, ahead: '' };
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
    // One write, read back: the text must stand as the place's own line.
    function put(at, text) {
      return Beach.write('now:' + name, { spindle: at, content: text, secret: word }).then(function (r) {
        if (r.status !== 200) throw failed(r);
        return Beach.look('now:' + name, '&spindle=' + at);
      }).then(function (r) {
        if (!(r.status === 200 && own(r.json) === text)) throw 'unreachable';
      });
    }
    // A day's or tomorrow's line, only where Sundial may write it.
    function fold(at, text) {
      return Beach.look('now:' + name, '&spindle=' + at).then(function (r) {
        if (!r.status || r.status >= 500) throw 'unreachable';
        var there = r.status === 200 ? own(r.json) : '';
        if (there && there !== text && !sundials(there, at, kept.wrote)) return null;
        return put(at, text);
      });
    }
    var day;
    return claim.then(function () {
      day = today();
      if (!day) return Beach.look('now:' + name, '&pscale=0').then(function () { day = today(); });
    }).then(function () {
      if (!day) throw 'unreachable';
      return kept.moment ? put(now(), kept.moment) : null;
    }).then(function () {
      return kept.day ? fold(day, kept.day) : null;
    }).then(function () {
      return kept.ahead ? fold(tomorrowOf(day), kept.ahead) : null;
    }).then(function () {
      return 'kept';
    }).catch(function (e) {
      return typeof e === 'string' ? e : 'unreachable';
    });
  }

  // A word proved on a name's own day book the moment it is given (plan
  // 6.8.3.5 (1)), in two steps that change nothing, as whispers.js proves
  // it: letting go of the lock with no word is refused when it is locked;
  // then the lock "rotated" to the same word answers 200 only for the word
  // that holds it. Resolves 'proved'; 'wrong' (it doesn't open the day
  // book, or the day book is open and proves nothing); or 'unreachable'.
  function prove(name, word) {
    return Beach.write('now:' + name, { new_lock: '' }).then(function (w) {
      if (w.status === 200 || w.status === 404) throw 'wrong';
      if (w.status !== 403) throw 'unreachable';
      return Beach.write('now:' + name, { secret: word, new_lock: word });
    }).then(function (w) {
      if (w.status === 200) return 'proved';
      throw w.status === 403 ? 'wrong' : 'unreachable';
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
    now: now,
    PARTS: PARTS,
    addressAt: addressAt,
    partOf: partOf,
    timeOf: timeOf,
    tomorrowOf: tomorrowOf,
    dateOf: dateOf,
    daysIn: daysIn,
    withMomentDays: withMomentDays,
    momentsIn: momentsIn,
    readAll: readAll,
    rungsIn: rungsIn,
    rungOf: rungOf,
    setRung: setRung,
    card: card,
    handLine: handLine,
    resolve: resolve,
    read: read,
    keep: keep,
    prove: prove,
    sundials: sundials,
    forget: forget,
    passportLine: passportLine,
    mirrorLine: mirrorLine
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Days = api;
})(this);
