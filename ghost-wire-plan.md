# Ghost Wire: the plan, redrafted for the beach

A one-touch precision game: guide a glowing point along a neon wire from start
to finish as fast as you can without touching the wire, racing a friend's
ghost. This redraft keeps the game as specified and changes where it lives
and how it keeps its data, so that it is built the way the other pages in
this repo are built, and keeps its record on the beach.

## 1. What changes, and why

| The original spec | This redraft | Why |
|---|---|---|
| Next.js + TypeScript | One static page, `ghost-wire.html`, and one script, `ghost-wire.js`, in plain JavaScript | This repo is static pages served from GitHub Pages (`phenomemental.github.io/happyseaurchin-beach/`) with no build step. Every page is HTML plus small IIFE scripts, cache-busted with `?v=`. Next.js would need a build and a different host. |
| "Insert your database here" | The beach, `beach.happyseaurchin.com/.well-known/pscale-beach`, through `beach.js` | `beach.js` already reads blocks, reads the beach's clock, and appends to pools from these pages. |
| Track paths stored | Not stored. Each day's wire is generated from that day's address on the beach's clock | The same address always gives the same wire, so nothing has to be kept. |
| Replays stored frame by frame | Inputs recorded (when you press and release); the frame-by-frame trace is re-run from them | This makes a replay about 100 characters long, and it lets the page check a claimed time by re-running it (§5). |
| Friend connections | None. A friend is whoever you send the link to | Visitors on these pages have no handles, and the link already carries everything a challenge needs. |
| `?track=[track_id]&ghost=[replay_id]` | `ghost-wire.html?track=2026413700&ghost=23` | `track` is the day's address; `ghost` is the slot the beach gave the run in the pool (§3). |

## 2. A decision to make first: the beach's bar for games

`spine:games` sets the bar for beach games: *the LLM brings the magic, humans
bring the juice*. It also says that *a game a first computer could have run is
a blank round*. As specified, Ghost Wire is that kind of game. Nothing in it
needs an LLM.

**My recommendation:** build v1 as one of your pages that keeps its record on
the beach. Don't propose it to the games catalogue yet. If you later want it
in the catalogue, it needs one part where an LLM counts. The obvious one: each
day's wire is drawn by the host's LLM from a hidden word ("a heron lifting
off"), and the word is shown at the finish. That would move the wire from
code into a block (`spine:ghost-wire`, one wire per day address). Steps 1–3
below don't depend on this choice.

## 3. Where it lives on the beach

The game needs two new blocks, following the `game:<name>` / `pool:<name>`
pattern the games already use (`game:count`, `pool:count`). Neither name is
taken yet.

**`game:ghost-wire`: the law.** It has the same shape as `game:count`:

- `_`: the briefing, one short line (see §6).
- `1` WIRE LAW: how a day's wire is drawn from its address (seed, generator,
  version).
- `2` RUN LAW: the moves (hold to go, let go to brake) and what a buzz does
  (the attempt restarts).
- `3` GHOST LAW: what a kept run carries (the replay format). Ghosts are
  re-run, never taken on trust.
- `4` SCORE LAW: time is counted in simulation steps; you're ranked among the
  day's runs that re-run to a finish.
- `9` PROVENANCE: who specified and built it, and when.

It is locked with your key. The page has these words written into it already,
so it makes sense before the beach answers, and it reads them live, the way
`night-watchman.js` reads its words.

**`pool:ghost-wire`: the room.** Each run someone chooses to keep becomes one
contribution, in the standard pool shape (block-conventions 4.22) plus one
pool-specific field:

```
{
  _: "23.41 s on the wire of Wednesday 21 October 2026",
  1: "Sam, a visitor",
  2: "2026413700",                      // the day's address: the run is located at its wire
  3: "2026-10-21T09:14:03.120Z",
  7: "gw1.2026413700.2809.AQIDBAUG..."  // the replay (§5)
}
```

- **Field 2** is the day's address, so "today's runs" are the entries
  located there. This is the pool-to-tree link the beach already uses.
- **Field 7** holds the replay. Block-conventions leaves fields 5–9 open for
  pool-specific metadata. Of those, 5 is written by `pscale_pool_engage`, 6 is
  used by liquid slots, and 9 holds a SAND rider, so 7 is free.
- **Signing.** Visitors sign as `<name>, a visitor`, exactly as
  `Beach.wireNoteForm` does, so nobody can sign as a beach handle.
- **The `ghost` id** is the slot number the beach assigns when the run is
  appended. It stays valid as the pool grows: when the pool supernests, the
  entry at `7` is read as `07` (block-conventions 4.29).

Nothing else new is needed: no per-day pools, no track blocks, no friends
block.

## 4. The day's wire

- **Today's address.** Every beach response carries the clock in its
  `X-Pscale-Now` header, and `Beach.clock()` already parses it:
  `2026-10-21T00:21:34Z | 2026413712 | Wednesday 21 October 2026, deep night (beat 2)`.
  The ten digits are year (4), season, month, week, day, gathering and beat.
  The day is the first eight digits. Written at full width it is
  `2026413700`.
- **Always full width.** A short address like `20264137` is read as
  `0020264137`, which is a different place.
- **When the wire changes.** Today's wire turns over when the beach's day
  does, the same for everyone.
- **Old wires.** Any past day's wire can be played forever, because
  `?track=` regenerates it. A challenge link never goes stale.
- **The generator.**
  1. Hash the address string to 32 bits (FNV-1a with `Math.imul`).
  2. Seed `mulberry32` with that hash.
  3. Lay out 9 control points in a winding path that fits a portrait
     screen.
  4. Draw a cubic B-spline over them. It passes near each point rather
     than through it, so turns become rounded bends instead of hairpins.
     The tightest bend has a radius of about 0.03 wire units.
  5. Sample the curve at equal arc-length steps into tables: `x`, `y`, the
     normal, and the curvature `κ` (from the turn between neighbouring
     segments).

## 5. Runs and ghosts: record inputs, not positions

- **Fixed-step simulation at 120 Hz.** The input is read once per step, and
  the screen interpolates between steps.
- **A replay** is the list of steps at which "hold" changed, plus the step the
  run ended on. It is stored as varint deltas, base64url, behind a version
  prefix: `gw1.<track>.<steps>.<data>`. A 30-second run is about 100
  characters.
- **The spec's per-frame record** (time, progress 0–1, speed, alive/buzzed)
  is rebuilt exactly by re-running the replay on its wire. The ghost is drawn
  from that rebuilt record, step for step alongside you.
- **Honesty comes from re-running.** A pool on an open beach takes anyone's
  lines, so the page never trusts a claimed time. It re-runs each ghost and
  counts only what reproduces. As `ways:games` 1.3 asks of a game's law, the
  replay format can be published in full without letting anyone fake a
  result.
- **Determinism rules** (so every browser re-runs a ghost identically):
  - Anything the simulation reads uses only `+ − × ÷` and `Math.sqrt`, over
    tables built at load.
  - Randomness for the wire comes from integer `mulberry32`.
  - Curves are polynomial (a uniform cubic B-spline).
  - No `Math.sin/cos/atan2/pow/exp` in the simulation, because engines may
    differ in the last bit.
  - Rendering can use anything.
- **Versions.** The version prefix lets the wire or the physics change later
  without breaking old ghosts. A new version gets a new prefix, and the old
  code path stays.

## 6. The physics, in one model

The spec says "higher speeds widen the tolerance margin around curves". This
plan reads it as: the faster you take a bend, the further the point swings
out from the centre of the wire, and past the wire's edge it buzzes.

State: `s` (distance along the wire), `v` (speed), `d` (sideways offset). Each
step:

```
v += (held ? ACCEL : -BRAKE) * dt        clamp 0 … VMAX
s += v * dt
d += (SWING * v*v * κ(s) - d) * SETTLE * dt
buzz   if |d| > HALF_WIDTH               → attempt restarts
finish if s >= LENGTH                    → time = steps / 120
```

- **Collision** is that one comparison. No polygon tests are needed.
- **The constants** live in one table. They are tuned by playing on phones
  (step 4), then frozen as wire law `gw1` before the first share link goes
  out.
- **The briefing on screen** tells the player three things only: where they
  start, their moves, and the objective (`ways:games` 1). The briefing is:
  *"Start at the open end of today's wire. Hold to go; let go to brake. Reach
  the far end as fast as you can without touching the wire."* How bends
  behave at speed is strategy, so the page never says it.

## 7. The page

**`ghost-wire.js`: the engine, with no DOM.** It contains the PRNG, the wire
(`wire(track)` → tables), the step function, and the replay encoder, decoder
and re-runner. It is exposed as `window.GhostWire`, and as `module.exports`
under Node so the determinism check can load it.

**`ghost-wire.html`: everything else.** Like the other pages, it has its style
and its page script inline:

- **Canvas.** Scaled for device pixel ratio. The whole wire fits the screen,
  so v1 needs no camera.
- **Input.** Pointer down/up on the canvas and the Space key. `touch-action:
  none` and prevented defaults stop scrolling and zooming.
- **Loop.** `requestAnimationFrame` drives a fixed-step accumulator.
- **Render.** The spec's palette: background `#0D0D12`, wire `#00F0FF`,
  player `#FFD700`, ghost `#A855F7`, with layered strokes for the glow.
- **Feel.** Sparks when braking or swinging wide. A buzz shakes the screen and
  calls `navigator.vibrate` where it exists (not iOS Safari).
- **Reduced motion.** For anyone whose device asks for less motion, there is
  no shake and fewer sparks, as with every page here.
- **End screen.** Final time, rank for the day, and a large "Challenge a
  friend" button.
- **A ghost before anyone else has played.** Your best run for each wire is
  kept on this device. Every storage call is wrapped in try/catch, like
  `Live.recall`.
- **Scripts.** It loads `beach.js` only. `beach.css` (the paper look) and
  `live.js` (140 KB) aren't needed here.

## 8. Talking to the beach (all through `beach.js`)

- **On load.** `Beach.get('game:ghost-wire')` gives the law's words and sets
  the clock. Today is `clock.address.slice(0, 8) + '00'`. The day's name for
  the end screen comes from `clock.voicing`.
- **Opening a link.** `?track=` picks the wire. `?ghost=` reads the one entry
  with `Beach.get('pool:ghost-wire', { spindle: ghost })`, decodes field 7,
  re-runs it, and races you against it.
- **Keeping a run.** A run is posted only when the player presses "Challenge
  a friend" (or "Put my time on the board"), not after every attempt. Playing
  is liquid; keeping is a commit.
- **Posting.**
  1. `Beach.note` gains an optional fifth argument for extra fields
     (`{ 7: replay }`).
  2. It resolves with the beach's acknowledgement, and the link takes the slot
     from that.
  3. If the acknowledgement turns out not to carry the slot over HTTP, the
     page re-reads the pool and finds the entry by its time and replay.
- **Rank.**
  1. Read `pool:ghost-wire` whole.
  2. Keep the entries whose field 2 is this wire's address and whose field 7
     re-runs to a finish.
  3. With 5 or more, show "Top 12% today!".
  4. With fewer, show "2nd of 4 today".
- **Sharing.** `navigator.share` where it exists, otherwise copy the link.
  The text is *"I ran today's Ghost Wire in 23.41 s. Race my ghost:"* with the
  link.
- **When the beach can't be reached.**
  - The wire comes from the link's `track`. With no link and no clock, it is
    a practice wire.
  - The end screen says plainly that the run can't be kept just now.
  - Nothing waits longer than `beach.js`'s six seconds.
- **Size.** A kept run is about 250 bytes, so a thousand runs is about 250 KB
  to read whole. That's fine for early days. If it outgrows that, David could
  set it up as its own world on the beach (`beach.happyseaurchin.com/w/ghost-wire`).
  That's an act only the beach's operator can do.
- **A risk to accept for now.** Visitor lines are unlocked, so anyone could
  overwrite one. Re-running limits the damage: a faked line still has to be a
  real run.

## 9. Setting it up on the beach

These are your acts, with your key. Nothing is written until you say so.

1. Write `game:ghost-wire` with the law in §3 and lock it.
2. Create `pool:ghost-wire` with `pscale_pool_engage` (`purpose` = its opening
   line, written to the person who arrives): *"Ghost Wire's room. Each line is
   a run of a day's wire, kept from the game page with its ghost; open its
   link to race it. https://phenomemental.github.io/happyseaurchin-beach/ghost-wire.html"*
3. Post one line in `pool:games`, the makers' room, saying what it is and
   linking the page.
4. Add a line to your map, `artifacts:Phenomemental` 8 ("My pages, off the
   beach"). That entry already holds nine pages, so this needs a nesting
   choice.
5. Only if you choose the LLM path in §2: propose it at `spine:games` 7 by
   writing your own mirror `games:Phenomemental` 7, and append a `scoreboard`
   line after the first day it is played.

## 10. Build order

This replaces "start by scaffolding the project structure…".

1. **Engine and loop, no beach.**
   - Build `ghost-wire.js` (PRNG, wire, step, recorder and codec).
   - Build `ghost-wire.html` (canvas, input, fixed-step loop, render, buzz
     and restart, finish, and your own best as the ghost).
   - Check it two ways: Node re-runs a recorded replay to the same final step;
     and Chromium (via Playwright, already in this environment) plays a
     scripted run on the page and the ghost matches the run. Nothing is added
     to the repo for these checks.
2. **Feel.** Sparks, shake, vibration, reduced motion, end screen.
3. **The beach.** Today's wire from the clock, the link's ghost, keeping a
   run, rank, sharing. This needs §9 steps 1–2. It also needs
   `beach.happyseaurchin.com` reachable from the test browser: this cloud
   environment's network policy currently blocks it.
4. **Tune and freeze.** Tune the constants on real phones, then freeze them as
   `gw1` before the first link goes out.
