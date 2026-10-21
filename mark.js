/* Sundial's mark: the six places a day can stand in, and the sums the
   drawing is made from (the plan on the beach at spine:front-door, 7.16
   to 7.17). One table holds each place's hue, the quiet line beneath a
   sheet, the question an empty pool asks, and where its Beings stand in
   beings:night-watchman; the Beings themselves are always read from the
   beach, never written here. Nothing here touches a page. */
(function (root) {
  // In pool order, 1 to 6: the order of the colour along the heart's
  // stroke and of the tide-pools round the ring, Facts at the top.
  var POOLS = [
    { n: 1, name: 'Facts', hue: '#4A6B8A', phrase: 'what happened', at: '1.1',
      question: 'What could you have pointed to today?' },
    { n: 2, name: 'Feelings', hue: '#B0413E', phrase: 'what you felt', at: '1.2',
      question: 'What did your body know today?' },
    { n: 3, name: 'Fictions', hue: '#7A5C99', phrase: 'what you made of it', at: '1.3',
      question: 'What did you make of it, that might not be so?' },
    { n: 4, name: 'Purpose', hue: '#2E7D6B', phrase: 'what matters', at: '2.1',
      question: 'What mattered, underneath?' },
    { n: 5, name: 'Decision', hue: '#B8860B', phrase: 'a choice', at: '2.2',
      question: 'What did you choose, or let be chosen?' },
    { n: 6, name: 'Outcomes', hue: '#5B7F95', phrase: 'what’s ahead', at: '2.3',
      question: 'What are you expecting, or fearing?' }
  ];

  // The pool a day stood in (its position 3, "1" to "6"), or 0 when the
  // day carries none that can be read.
  function poolOf(day) {
    var n = Number(day && day['3']);
    return n >= 1 && n <= 6 && Math.floor(n) === n ? n : 0;
  }

  // How many days stood in each pool, in pool order.
  function counts(days) {
    var c = [0, 0, 0, 0, 0, 0];
    (days || []).forEach(function (d) {
      var n = poolOf(d);
      if (n) c[n - 1]++;
    });
    return c;
  }

  // Each pool's share of the days, summing to 1; all nought before any day.
  function shares(days) {
    var c = counts(days);
    var total = c.reduce(function (a, b) { return a + b; }, 0);
    return c.map(function (x) { return total ? x / total : 0; });
  }

  // How evenly the days spread across the six: 1 less the variance of the
  // shares over the most it can be (every day in one pool, 5/36), so 1 when
  // all six hold the same and 0 when one holds everything. Nought before
  // any day: the drawing gives the empty heart its own centre.
  var MOST = 5 / 36;
  function evenness(s) {
    var total = s.reduce(function (a, b) { return a + b; }, 0);
    if (!total) return 0;
    var mean = 1 / 6, v = 0;
    s.forEach(function (x) { v += (x - mean) * (x - mean); });
    v /= 6;
    return Math.min(1, Math.max(0, 1 - v / MOST));
  }

  // ---- The console check ------------------------------------------------------
  // Mark.test(days) with the trial fixture's nine days (fixtures/days-trial.json,
  // pools 1,1,3,2,1,3,5,6,1): the counts, shares and evenness after the
  // first 1, 3 and 9 days and at 27, against sums worked by hand.
  function test(days, log) {
    log = log || function (s) { console.log(s); };
    var results = [];
    function near(a, b) { return Math.abs(a - b) < 1e-12; }
    function all(a, b) { return a.length === b.length && a.every(function (x, i) { return near(x, b[i]); }); }
    function check(label, ok) {
      results.push(ok);
      log((ok ? 'pass  ' : 'FAIL  ') + label);
    }
    var thrice = days.concat(days, days);
    check('the fixture holds nine days, pools 1,1,3,2,1,3,5,6,1',
      days.length === 9 && days.map(poolOf).join() === '1,1,3,2,1,3,5,6,1');
    check('the table names six pools in order, with the plan’s hues',
      POOLS.map(function (p) { return p.n + p.hue; }).join() ===
        '1#4A6B8A,2#B0413E,3#7A5C99,4#2E7D6B,5#B8860B,6#5B7F95');
    check('no days: no shares, evenness 0', all(shares([]), [0, 0, 0, 0, 0, 0]) && evenness(shares([])) === 0);
    check('1 day: all in what happened, evenness 0',
      all(shares(days.slice(0, 1)), [1, 0, 0, 0, 0, 0]) && near(evenness(shares(days.slice(0, 1))), 0));
    check('3 days: shares 2/3 and 1/3, evenness 8/15',
      all(shares(days.slice(0, 3)), [2 / 3, 0, 1 / 3, 0, 0, 0]) && near(evenness(shares(days.slice(0, 3))), 8 / 15));
    check('9 days: counts 4,1,2,0,1,1', counts(days).join() === '4,1,2,0,1,1');
    check('9 days: shares 4/9, 1/9, 2/9, 0, 1/9, 1/9',
      all(shares(days), [4 / 9, 1 / 9, 2 / 9, 0, 1 / 9, 1 / 9]));
    check('9 days: evenness 116/135 (' + evenness(shares(days)).toFixed(4) + ')',
      near(evenness(shares(days)), 116 / 135));
    check('27 days: the same shares and evenness as nine',
      counts(thrice).join() === '12,3,6,0,3,3' && near(evenness(shares(thrice)), 116 / 135));
    check('one day in each pool: evenness 1',
      near(evenness(shares([1, 2, 3, 4, 5, 6].map(function (n) { return { 3: String(n) }; }))), 1));
    check('a day without a readable pool is not counted',
      counts([{ 3: '7' }, { 3: '' }, {}, { 3: '2' }]).join() === '0,1,0,0,0,0');
    var ok = results.every(Boolean);
    log(ok ? 'Mark: all ' + results.length + ' checks pass.' : 'Mark: a check failed.');
    return ok;
  }

  var api = {
    POOLS: POOLS,
    poolOf: poolOf,
    counts: counts,
    shares: shares,
    evenness: evenness,
    test: test
  };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.Mark = api;
})(this);
