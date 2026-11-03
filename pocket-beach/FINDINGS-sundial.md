# Sundial, the app (formerly Rockpool): findings before the build

3 November 2026, for spine:pocket-beach 1.111178719 and its nine places
(1.1111787191 to 1.1111787199). The plan was read whole and only read;
nothing was written to the spine. **Nothing is built yet.** This is the
read of the code against the plan that 1.1111787199 asks for first. The
build waits until Claude has read this whole and said go.

**What was read**

- The plan: 1.111178719 and its nine places, whole. Beside them, read
  only for what they point to: 1.1111787171 (who a post is from, and where
  it goes) and 1.1111787172 (the link after a post).
- pscale://sundial, whole: the beach's clock.
- The web source, as the source of the app's look, words and ways only:
  - sundial.html (6,916 lines; version "Sundial M4.18 · 25 October 2026");
  - mark.js, shell.js, days.js, whispers.js, films.js;
  - and, where they hold Sundial's tokens, night-watchman.css and beach.css.
- The app on main at `4d8f5a8` (Rockpool 0.9.1, released):
  - Cook.kt, Days.kt, Temporal.kt, Clock.kt, Keeping.kt, PoolPost.kt and
    HerWindow.kt;
  - AskScreen.kt, Asking.kt, ShoreScreen.kt, BlockScreen.kt, ChatFrame.kt
    (the menu), App.kt, AboutScreen.kt, SettingsScreen.kt and Theme.kt;
  - Gemma.kt, Mind.kt and Jobs.kt (her tool call);
  - Hatch.kt, Hatching.kt (the bring-up), Posts.kt and Talking.kt;
  - assets/procedures/cook.json and kernel.json;
  - the manifest, the network config, the launcher icon, the build file
    and the release workflow.
- Read-only looks at the beach:
  - `passport:Phenomemental`, for whether it carries a location (it
    doesn't);
  - `beings:night-watchman` 1, for the ways of listening Sundial reads
    from it.

References are `file:line`. Paths under the app are given from
`pocket-beach/`, for example `bsp/…/Cook.kt:150` for
`pocket-beach/bsp/src/main/kotlin/com/phenomemental/pocketbeach/bsp/Cook.kt`
and `ui/…/AskScreen.kt:375` for the same under `app/…/ui/`.

---

## The short version

1. **The plan fits the app's base well.** The guards, the walker, his
   cook's place-by-place upgrade, the hatch, his now on the clock, the one
   post and Gemma's tool call are all there to build on. **Five places don't
   fit as the plan is written**, and each needs a decision before the build:
   - **His cook has no free place for the laws.** All nine are taken
     (cook.json 1 to 9), and the walker runs a procedure only at a single
     digit (`Cook.kt:150`, `164`, `273`). The proposal is in 5 below: the
     walker learns two-digit procedures, and cook 6 becomes Sundial's ways,
     with KEEP TODAY at 6.1, PLACE at 6.2, GREET at 6.3 and LISTEN at 6.4.
   - **The colours in 1.1111787191 aren't Sundial's.** `#ede5d4`,
     `#1a1611`, `#2a231b` and `#c41c4e` appear nowhere in the repository.
     Sundial's own are paper `#eee7d5`, ink `#1d1a17` and crimson `#c21d4e`
     (`night-watchman.css:13-21`). The app's are different again
     (`Theme.kt:39-48`).
   - **His passport carries no location.** Place 3 of
     `passport:Phenomemental` is his "meet me in the pool" line. A
     spatial:earth address couldn't become a latitude offline anyway. So
     where he stands comes from a town he picks, or from the phone.
   - **Sundial's listening reads its three ways from the beach**
     (`beings:night-watchman`, `sundial.html:3614-3641`), and so does its
     road (`spine:front-door` 3.6). The app reads nothing from the web, so
     both must be copied into Sundial's own block. The plan's list for that
     block leaves them out.
   - **Moments aren't kept anywhere today.** His messages live only in
     memory while Ask is open (`Asking.kt:133`). The dial's moments ring
     and the minute rung need an app-own record of today's moments from the
     moment he speaks, and the record must not be a block.
2. **The clock is mostly there.** `momentToAddress(ms, fine)` already gives
   the minute, breath and second digits in UTC (`Temporal.kt:27-57`). What
   must be added:
   - a grain-as-a-word form;
   - the moment write: at its minute, else at its breath beneath, with
     its instant in a hidden directory;
   - the Block screen's fine rungs;
   - one rule for "today": the beach's UTC day everywhere a day is meant.
     Four places use the phone's local date now.
3. **Reflect and ask through the tool call** is a new kind of call: a tool
   inside her kept conversation, which no call has yet (`Gemma.kt:164`).
   On E4B her reply stops streaming. Sundial's pulse along the line covers
   the wait, as it does on the web, where Claude's JSON never streams.
4. **It is too much for one release.** Three are proposed in 9: Today and
   keeping first, the shells and listening second, the films and her other
   voices third. The data the first one writes (the day line, the moments
   at their minutes) is what the later two read, so it has to be right
   first.

---

## 1. Every place the plan does not fit the code as it is

### 1a. The name and the mark (1.1111787191)

- **"Rockpool" is still shown** in the following places:
  - the launcher label, `AndroidManifest.xml:29`;
  - the Shore's title, `ui/…/ShoreScreen.kt:85`;
  - About's label and opening, `ui/…/AboutScreen.kt:34` and `:39`;
  - the speech notes, `ui/…/Speech.kt:285`, `:329` and `:349`;
  - the bench header, `ui/…/Benching.kt:302`;
  - the saved conversation's first line, `ui/…/Asking.kt:1291`.

  The plan puts Sundial at the head of every screen. The Shore becomes
  "Your pocket".
- **The launcher mark is the Nine stones, on ink.**
  `res/drawable/ic_launcher_foreground.xml` draws it, and so does
  About's own `Mark()` (`ui/…/AboutScreen.kt:65-95`).

  The new mark is Sundial's heart on paper with nine stones round it. The
  heart's paths are exactly Sundial's small heart (`sundial.html:2321`):
  - the open ring `M 311.7,49.6 A 124,118 0 1 0 353.3,115.4`;
  - the long line with its loop `M 375,25 C 320,45 262,78 215,102 C 196,113
    186,138 192,156 C 199,172 232,180 262,174 C 300,166 304,122 284,106 C
    264,92 240,96 222,108 C 195,118 172,165 150,212 C 122,262 68,300
    14,324`;
  - the ellipse `cx 250 cy 138 rx 40 ry 24`, rotated -18.

  The stones are new: Sundial's dial has no stones (`sundial.html:2244-2253`
  is the heart alone; its nine to a turn are the spiral's dots).
- **The plan's colours aren't Sundial's.** 1.1111787191 gives paper
  `#ede5d4`, ink `#1a1611`, stones `#2a231b` and crimson `#c41c4e`. None of
  them appears anywhere in the repository, and Sundial has no "stones"
  token. Sundial's resolved tokens are below (`night-watchman.css:13-21`
  for light, `sundial.html:36-46` for dark):

  | token | light | dark |
  |---|---|---|
  | paper | `#eee7d5` | `#16130f` |
  | page | `#f6f1e6` | `#1d1914` |
  | ink | `#1d1a17` | `#ede4d3` |
  | muted | `#5a5147` | `#a79c8b` |
  | frame | `#b9ad9a` | `#4a4237` |
  | crimson | `#c21d4e` | `#ef5d85` |
  | wash | `#f5e95a` | `#b3a73c` |

  The app's are different again: paper `#F4EEE3`, ink `#1C1A17`, crimson
  `#9B1B30` (`ui/…/Theme.kt:39-48`).

  **Proposal:** the app takes Sundial's tokens, light and dark. Sundial
  keeps a night look "by Matthew's choice" (`sundial.html:33-34`), so the
  app's dark theme (`Theme.kt:95`) stays, in Sundial's dark tokens. "Go
  with paper" is read as the launcher mark's ground. Matthew to say which
  hexes the mark wears: the plan's, or Sundial's.
- **The release names Rockpool** in three places:
  - the tag, `rockpool-v$VERSION`, at `.github/workflows/pocket-beach.yml:192`;
  - the tag check at `:227`;
  - the release notes at `:240`.

  It needs `sundial-v`, with the old prefixes still accepted.

### 1b. His cook and kernel: the walker (1.1111787194, 1.1111787197)

- **No free place for the laws.** cook.json holds:
  1 ANSWER, 2 KEEP, 3 POST, 4 MAKE, 5 READ, 6 TELL MY DAY, 7 CLOSE,
  8 HER NAME, 9 THE WORDS.

  `Cook.parse` takes a procedure only at a top-level digit
  (`bsp/…/Cook.kt:220-230`). Every way of naming one reads a single digit:
  - `Run cook (\d)` (`:164`);
  - `On his tap, run cook (\d)` (`:150`);
  - the kernel's `Run cook (\d)` (`:273`).

  GREET, LISTEN and PLACE need places of their own, and KEEP TODAY takes
  TELL MY DAY's. See 5 for the proposal.
- **The kernel runs two events only.** `Cook.kernel` reads HIS MESSAGE and
  THE CLOSE OF A CONVERSATION (`Cook.kt:268-275`). Kernel 2 and 4 are prose
  the walker never reads (kernel.json lines 4 and 6), and `Cook.load` checks
  only those two (`Cook.kt:950`). "Keep today as a kernel event" needs the
  kernel to learn new labels, and so do the greeting and the shell (5).
- **The SetDay forms carry P6.** Both sentences end "and each of ($CAP)
  beneath ($CAP) in his P6" (`Cook.kt:141-146`), and the rest of the path
  follows them:
  - `Verb.SetDay` holds pieces and portals (`:74`);
  - `Days.landing` and `Days.blocks` write `p6:<handle>`
    (`bsp/…/Days.kt:82-89`, `227-242`);
  - cook 6.3 asks her for a portal per piece (cook.json 146-154);
  - 6.6 and 6.7 tell her and him about the P6 (cook.json 157-158);
  - the day card on screen has a "Your P6" group, and portals to move a
    line to (`ui/…/AskScreen.kt:639`, `669-672`).
- **Her choice is one digit.** `ChooseRing` and `ChooseEach` take one place
  from a ring (`Cook.kt:533-568`). The `choose` tool has one integer
  (`model/Gemma.kt:317`). Placing names several places at once, so it
  needs a tool that returns a list, and a form that carries one (5).
- **Her talk has no tool.** Her kept conversation is created without tools
  (`model/Gemma.kt:164`) and her reply streams (`ui/…/Asking.kt:554`). The
  only tool call is `choose`, in a conversation of its own with 60 output
  tokens (`Gemma.kt:311-334`). Reflect and ask as parts is new (6).
- **There's no grain word.** The day's line is written at eight digits
  (`Days.kt:197-209`). A booking is written at its day and a rung at its
  level. Nothing writes below the day.
- **The writes append; Sundial's don't.** `Days.voiced` appends a new line
  beneath one already there, with a blank line between
  (`Days.kt:289-295`). Sundial's rule is the other way round (`days.js:316-331`,
  `358-365`):
  - a day's line is written only where the place is empty, or holds a
    line Sundial wrote;
  - a line of his own is left exactly as it stands.
- **Hand material can't carry a list.** `Hand` material carries a
  Reading, a PlaceRead or a String (`Cook.kt:571-579`). A carried list
  (the places untouched, the days in a place) is dropped silently.

### 1c. The clock (1.1111787192, pscale://sundial)

- **Moments aren't kept.**
  - An `Exchange` holds `at` in memory only (`ui/…/Asking.kt:133`).
  - The close keeps a count and his first words, not each message and its
    time (`bsp/…/Hatch.kt:371-392`).
  - The plan's moments ring and "each moment he spoke that day at its
    minute" need a record of today's moments from the moment each is
    spoken. It must survive Ask closing and the app being killed. Being an
    app-own file, never a block, it keeps "nothing is kept without his
    yes": he says yes at Keep it.
- **The local date and the UTC day are mixed.**
  - The writes use the beach's UTC day (`Days.kt:192`, `:330`), as the
    plan wants.
  - These read the phone's local date:
    - the walker's `now` (`Asking.kt:263`), whose `toLocalDate()` sizes
      bookings (`Cook.kt:623`);
    - her conversation's day (`Asking.kt:454-455`, `535-537`);
    - the clock screen's "today" (`ui/…/BlockScreen.kt:240`).
  - In summer, between midnight and one in the morning, these name
    different days. The Block screen would open on a day with nothing
    written, and "tomorrow" would be a day out.
- **The clock screen stops at the day.** `Clock.level`, `dateOf` and
  `rungSaid` know the year down to the day (`bsp/…/Clock.kt:23-66`).
  `level` would count the `.` of a fine address. Moments at the minute
  can't be stood at, or said, until the gathering, beat, minute and breath
  are added.
- **Her window can't see moments.** `HerWindow.today` reads each child of
  today as a part of the day with its own line (`bsp/…/HerWindow.kt:160-176`).
  Moments at the minute sit three levels below a gathering that has no
  line of its own, so she'd never see today's kept moments.

### 1d. Keeping and the six places (1.1111787192, 1.1111787197)

- **His old days have no places line.** A Rockpool day line is his telling
  as he said it, with no places line. His P6 pieces each carry their day's
  address at 2 (`Days.kt:239`, and `:349` for the move out of telling), so
  the plan's count by address works as written.
- **Bookings share the day's line.** A booking is written at its day's
  address (`Days.kt:203`) into the same underscore as a told day
  (`Days.kt:207`). Sundial's own rule is "any day is a day"
  (`days.js:150-170`): any line at a date counts. So the spiral and the
  strandline must count days up to today only, or a booking for Friday
  shows as a day kept on Friday.
- **"Sort as my day" runs cook 6** (cook.json 35-36). It must run the
  keeping procedure instead, as the plan says: "a day is kept one way only".
- **The day card's words are stale.** "Yes writes these blocks" still names
  `telling` (`AskScreen.kt:355-364`).
- **The day's instant has nowhere to live.** The plan has each day's dot
  carry "the light of the hour it was kept", but a day's line holds no
  instant. If one is added at the day node (a hidden directory under `_`),
  `Days.toMove` (`Days.kt:306-310`) would take a `3` there for an old place
  and try to move the day out. So `toMove` must look only for objects at
  1 to 6.

### 1e. Your room (1.1111787171, 1.1111787172, 1.1111787197)

- **The author and the room are hard-coded.**
  - `PoolPost.AUTHOR = "Phenomemental"` and
    `POOLS = setOf("Phenomemental")` (`bsp/…/PoolPost.kt:51`, `:55`), and
    `prepare` refuses any other room (`:60`).
  - The name line is `"Matthew calls me $name."` (`bsp/…/Keeping.kt:29`).
  - Her window falls back to "Matthew" (`HerWindow.kt:221`) and reads
    `knows:Phenomemental` (`HerWindow.kt:39`).
- **The link after a post opens the tools door.** It opens `BEACH_URL`
  (`AskScreen.kt:503-510`; About at `AboutScreen.kt:24`, `56`), which is
  the beach's door for tools: Matthew saw a 404.
  - No page in this repository shows a room's entries. index.html is his
    own page with a note form and a live feed, with `ME = 'Phenomemental'`
    fixed (`index.html:505`).
  - So, as 1.1111787172 says, the link is left out until such a page
    stands.
- **Sent posts aren't kept as notes.** Posts.kt keeps a hash and the entry
  it landed at (`Posts.kt:12-24`), not the words or the time. The room's
  list of notes sent needs both.
- **There's no room screen** and no post box: posting today is only
  through her, or a card beneath her reply.
- **The post card overclaims once Claude can speak.** It says "It is the
  only thing the pocket sends" (`AskScreen.kt:817-818`). That stops being
  true once his own Claude can speak.

### 1f. The screens and the menu (1.1111787197)

- **The menu is Rockpool's.** It holds Your blocks, Teaching blocks,
  Bench, Save this conversation, Details, Settings and About
  (`ui/…/ChatFrame.kt:82-88`).

  The plan's menu, as Sundial's own drawPlaces, puts YOURS first: Today,
  Your days, Your room, Your hand, Your pocket and Your recipes. Then
  Films, About, Settings and Coming soon.

  `Screen` has none of those pages (`ui/…/App.kt:21-32`), and the
  navigation's saver knows only its nine (`App.kt:46-72`).
- **The app opens on the chat.** After the hatch it opens on Ask
  (`App.kt:104-107`). The plan opens on Today, the dial.
- **Her reply is one plain paragraph** (`AskScreen.kt:375`). Sundial shows
  the reflection upright, then the question in italic beneath
  (`sundial.html:2294-2301`, `.sd-reflect` and `.sd-ask`).
- **The model note names the wrong model.** It says "The companion is
  Gemma 4 E2B" (`AskScreen.kt:855`). His phone talks on E4B.
- **The workings sit in the menu.** "Details" is the plan's "Show the
  workings" (`ChatFrame.kt:86`), and the plan puts it in Settings.
- **Sundial's menu has two more sections.** YOUR WORK and YOUR PAGES read
  `lists:<name>` and `artifacts:<name>` from the beach
  (`sundial.html:6159-6250`). They need the beach, so they're left for
  Coming soon. Sundial's "Your page" link to happyseaurchin.com goes to
  Coming soon, as the plan says.

### 1g. Settings, her voice, where he stands (1.1111787192, 1.1111787197)

- **Nothing uses the keystore.** No code touches the Android keystore.
- **The network can't reach Anthropic.** Only the beach and Hugging Face
  are trusted (`res/xml/network_security_config.xml`), and the manifest's
  own note says INTERNET is for two things only (`AndroidManifest.xml:3-6`).

  His own Claude needs `api.anthropic.com` added there, and the note said
  again.
- **There's no location permission** (`AndroidManifest.xml:14-19`), and no
  location or sun code anywhere in the app.
- **His passport carries no location.** `passport:Phenomemental` place 3
  is "Meet me in the pool: … (pool:Phenomemental)". pscale://sundial 5.5
  says place 3 is the Location star-ref, so the two disagree, and his
  passport says "meet". So "from his passport's own location if it
  carries one" finds nothing for him.
- **Sundial's own block can't be called "sundial".** The teaching shelf
  already holds a block of that name, the clock
  (`shelf/sundial.json`). It needs another name, such as `words:sundial`.
  It also can't sit on the shelf itself, which is the upstream's byte for
  byte (`app/build.gradle.kts:45`). It goes beside the procedures
  (`assets/procedures/`, `PocketApp.kt:53`).

### 1h. Sound and films

- **Nothing makes a sound yet.** The app has no audio code (no AudioTrack
  or MediaPlayer). The sea is new.
- **There's no film code.** The films are new.

---

## 2. The dial, drawn natively from Sundial's own paths and colours, live

**Where it lives.**
- The arithmetic sits in the core, as plain Kotlin with tests, as Temporal.kt
  does:
  - `Sun.kt`: where the sun is;
  - `Dial.kt`: mark.js's sums;
  - `MarkText.kt`: mark.js's read and write of a day.
- The drawing is a Compose `Canvas` in the app.
- Nothing is taken from the web page at run time. Paths and numbers are
  copied into Kotlin, and mark.js's own fixtures (`fixtures/days-trial.json`
  and `days-mixed.json`, which `Mark.test` checks) become the core's tests.

**The drawing, layer by layer.** The coordinates are the heart's own: the
viewBox is `17 -75 430 430` (`sundial.html:2244`), and the centre is
`CX 232, CY 140` (`mark.js:242`).

1. **Paper.**
2. **The night glow** (between sunset and sunrise): a circle of r 120 at
   the centre, a radial gradient of wash from 0.3 to 0 (`mark.js:305-317`).
3. **The sunlight** (between sunrise and sunset): the wash ellipse,
   rx = ry = 124, filled with a radial light of r 118.
   - Its stops are 1, 0.7 and 0 at 0, 0.5 and 1 (`mark.js:293-297`).
   - Its brightest point moves towards the sun by `36(1 - s) + 10`.
   - Its colour mixes apricot `#F3CB8F` (before noon) or amber `#EDB98A`
     (after) towards pale sun `#F1E79B` by `s`.
   - Its strength is `(0.25 + 0.5s) × max(0.35, min(1, days / 27))`
     (`mark.js:198-219`).

   Compose's radial brush has no separate focal point, so the gradient's
   centre itself moves. That is the same look at this size.
4. **The shadow**: the heart's two ink paths, not the crimson, moved by
   `L = 14 + 70(1 - s)` along the shadow's direction.
   - It is blurred at 4 (`mark.js:191-197`, `327-334`): `RenderEffect` blur
     on its own layer, since minSdk is 31.
   - Its opacity is `0.16 × min(1, 4s)`.
5. **The heart**: the two filled ink paths of `sundial.html:2244-2253`,
   parsed from their path data (`PathParser`).
6. **The colour inside the ink**: six stretches along the flow line, in
   P6 order from Facts (`mark.js:345-352`).
   - Each stretch is 160 of the line's 1000, offset `-k × 160`, width 7,
     butt ends.
   - Each one's opacity is `min(1, 0.35 + 1.6 × share)`, and none shows
     where the share is 0 (`mark.js:280-282`).
   - On a keep, the colour sweeps in along the line: a dash drawn through
     a mask, in a layer with `DstIn`.
7. **The centre**: the crimson ellipse, scaled `0.55 + 0.45 × evenness`
   once there are days, and to 0.5 before any (`mark.js:224-226`).
   - A halo ellipse sits behind it, rx 64, ry 40, rotated -18, at
     `0.18 × (1 - evenness)` (`mark.js:336-340`).
   - Evenness is 1 less the shares' variance over 5/36 (`mark.js:157-170`).
8. **Today's moments**:
   - a ring of r 166;
   - each moment an ink dot of r 4.5 at 0.6, at `-90 + (hour - 12) × 15`
     degrees by the phone's own clock: noon up, midnight down;
   - a faint arc 8 degrees beyond the first and last (`mark.js:477-492`).

   They say when he spoke, never where the shadow stood.
9. **The spiral of days.**
   - Day n sits at `-90 + 40(n - 1)` degrees, r `182 + 0.3(n - 1)`
     (`mark.js:255-270`).
   - The line is drawn from day 1 to day n, at ink 0.15, width 1.5.
   - The dots follow `dotOf` (`mark.js:273-277`): every ninth an open
     circle of r 5; the latest nine full, r 6; earlier turns r 2 at 0.35.
   - On the ninth keep the ring lights all round and folds
     (`mark.js:533-549`).
   - Each day's mark blooms in its colours when it is kept
     (`mark.js:514-529`).
10. **The light in each day's dot**, the plan's addition. Sundial's own
    dot is ink only: "A dot says only that a day was kept"
    (`mark.js:400`). Here each full dot is drawn in the light of the hour
    it was kept, against that day's real sun where he stood: the night
    glow's wash before sunrise and after sunset, otherwise `lightOf`'s
    colour at that instant.

    The faint dots of earlier turns stay ink, or the spiral turns into
    noise at that size. A day with no instant known (most Rockpool days)
    is drawn in ink. A day before the release takes the newest stamp of
    its P6 pieces, where it has one (`Days.kt:239`, at 3).
11. **While she writes**, the flow line pulses: a 150/850 dash travelling
    the line every 3.2 s, its opacity between 0.25 and 0.8 every 1.6 s
    (`sundial.html:476-499`).
12. **The date at the top**: weekday, day and month in en-GB, small caps,
    muted (`sundial.html:2771-2776`, `.sd-when`).

**The real sun, computed on the phone** (`Sun.kt`, new). The standard
solar arithmetic: NOAA's, after Meeus, good to about a minute.
- From the Julian day it gives the declination and the equation of time.
- From the hour angle it gives:
  - elevation, `asin(sin φ sin δ + cos φ cos δ cos H)`;
  - azimuth, from north and clockwise;
  - sunrise and sunset at -0.833 degrees;
  - solar noon.
- Where the sun neither rises nor sets that day, the elevation alone says
  day or night.
- Tests check it against published times for three towns and four dates.

**From the real sun to Sundial's drawing.** mark.js reads everything from
a clock hour. Here it is read from the sun:

| mark.js (fixed day) | the app (the real sun) |
|---|---|
| shadow at `-90 + (h - 12) × 15` (`mark.js:178-182`) | shadow at `azimuth + 90` degrees on screen, north at the top: east is right, so the morning shadow falls left, as in mark.js |
| day from 06:00 to 19:30 | day while the elevation is above -0.833 degrees |
| `s = sin(π(h - 6) / 13.5)`, 1 at 12:45 (`mark.js:184-188`) | `s = max(0, sin(elevation))` (a decision; see below) |
| apricot before 12:45, amber after | apricot before solar noon, amber after |

- **The decision on `s`.** With `s = sin(elevation)`, a December noon
  in Northampton (about 15 degrees) casts a long, faint shadow in warm
  light, and a June noon (about 61 degrees) a short one in pale light.
  That is "reflective of the real sun". The other reading is mark.js's
  own: `s` normalised to each day's own noon, which gives every noon the
  same short shadow. **The proposal is the first.**
- **Where he stands** is set once, roughly, kept in the app's own
  settings, never in a block and never sent:
  1. his passport's location, if it ever carries one (his doesn't);
  2. the phone's approximate location, on his permission
     (`ACCESS_COARSE_LOCATION`, the platform `LocationManager`, its last
     known fix first, rounded to a tenth of a degree);
  3. a town chosen from a list shipped with the app. The proposal is
     GeoNames' towns of 15,000 people or more, CC BY 4.0, a few MB, credited
     on About.

  The phone's own location service may consult its maker's servers to find
  him. That is the phone, not the app, but Settings should say it, and say
  that the town list asks nothing.
- **The tick**: every 30 seconds, and on returning to the app, eased from
  where the shadow and light were (`sundial.html:2787-2800`).
- **A new day.** On the first open of a new day, the shadow swings over
  3.4 s from where it fell at his last words to where it falls now, and
  yesterday's mark blooms in its colours (`mark.js:823`, `comeBack`).
  "His last words" is the newest moment's instant.

**Tap and hold.**
- A tap opens the strandline, newest first: each day a coiled shell in its
  colours, with its date, title and moments.
  - The coil has a radius of 25, 2.5 turns and 300 steps
    (`mark.js:967-976`).
  - A Rockpool day has no title row, so its first sentence stands for one.
- A hold of 700 ms opens the six pools round the centre
  (`sundial.html:3340-3386`).
  - They sit on a ring of r 128, in P6 order from Facts at the top.
  - Each is sized by its share, from 16 to 50, and an empty pool is a ring
    of 14 (`mark.js:925-934`).

---

## 3. The clock

**What pscale://sundial asks, and what the code already does.**

| The law | The code |
|---|---|
| Ten rungs, millennium to beat, in UTC; the beat about 17.8 minutes, anchored at pscale 0 (sundial 2, 3) | `momentToAddress` (`Temporal.kt:27-45`), the same arithmetic as `days.js:59-66` |
| Finer after the decimal: the minute (-1), the breath (-2), the second (-3) (sundial 2) | `momentToAddress(ms, fine = 1..3)` gives them (`Temporal.kt:46-56`); `addressToSpan` reads them back (`:75-88`); `voiceAddress` says them (`:117-122`) |
| A day's address, full width (sundial 3.2) | `Days.dayAddress(date)` (`Days.kt:182`) and `Landing.dayAddress(ms)` |
| No zero at the analogue rungs (sundial 1.1) | true by construction: the parts, beats and fine digits run 1 to 9 |
| Time computed by a pure function, never authored (sundial `_`) | true; the plan keeps the grain's arithmetic in code |
| No timezone stored (sundial 8.3) | true; the phone's zone is read for the hour shown and the sun only |

**What must be added.**

1. **The grain as a word.** One function, `grainAddress(ms, grain)`, where
   the grain is one of six words (shown here for 07:31 UTC on 3 November
   2026):

   | grain | the address |
   |---|---|
   | today | the day, `2026421300` |
   | tomorrow | the next day |
   | this gathering | nine digits, `2026421330` |
   | this beat | ten digits, `2026421338` |
   | this minute | `2026421338.4` |
   | this breath | `2026421338.4x` |

   The cook names the grain in words, as 4 shows, and the code only
   computes it. "Its minute" in "each of THE MOMENTS at its minute" is the
   same function on each moment's own instant.
2. **A moment's place.**
   - A moment goes at its minute.
   - When that minute already holds a moment, it goes beneath it, at its
     breath. When that breath holds one too, at its second.
   - Two moments within one second join the line already there.
   - At a coarser grain, if he changes the word (`at its beat`), the same
     rule steps one rung finer.
3. **A moment's instant.**
   - Every digit beneath a minute is a breath, so the instant can't sit at
     a digit.
   - It goes in the underscore's hidden directory (sunstone 1.4):
     `{"_": {"_": "his words", "3": "2026-11-03T07:31:04.120Z"}}`. The 3
     mirrors field 3 of the mark shape.
   - `collectUnderscore` already reads through it to his words
     (`Walker.kt:24-30`), and the line walkers never step into it
     (`HerWindow.linesOf`, `HerWindow.kt:526-540`).
   - So "the instant kept and the address voicing it" holds, with no
     timezone stored.

   The day's own line can carry the instant it was kept the same way.
   That is the dot's light in 2, once `Days.toMove` is tightened (1d).
4. **Today's moments as they're spoken.** An app-own file, `moments.json`
   in the app's files, never a block. It holds each message he sends,
   with its instant, kept until it is written at Keep today or the day is
   long past.
   - The dial's moments ring reads it, together with the moments already
     kept in today's node of his now.
   - Keep today writes from it, and a moment already standing at its
     address is not written again.
5. **One "today".**
   - Every "today" that means the day being kept is the beach's UTC day,
     as `days.js` has it: the walker's world, her conversation's day, the
     clock screen, the dial's date and the moments ring's day. The places
     to change are in 1c.
   - The hour shown and the sun are the phone's.
   - So in summer, between midnight and one in the morning, the dial still
     shows yesterday's date and yesterday's moments. That is right by the
     plan, and the dial should say so gently ("Your day turns at 1 am in
     summer"), or it will look like a fault.
6. **The Block screen's fine rungs.**
   - `Clock.level`, `dateOf` and `rungSaid` learn the gathering, beat,
     minute and breath. For example "Tuesday 3 November, early morning,
     07:31" says a moment by the hour of its instant, in the phone's own
     zone.
   - `Clock.trail` goes down to them, so a moment can be stood at and read
     whole.

**Sundial on the web keeps moments at the beat.** A moment there is the
sheet for that telling, written at this beat's ten digits
(`days.js:372-374`), and two in one beat share a slot. The plan's minute
and breath are the app's own advance, written in the beach's native shape,
and need nothing from the web.

---

## 4. Keeping

**The new sentence form, taking the grain as a word.**
```
Set out his day on the keep card, for his yes: THE SHEET as the day's line
at today in his now, in Sundial's form, THE PLACES TOUCHED named last in
plain words; each of THE MOMENTS at its minute; THE CARDS on his hand (the
block ahead: and his handle), those booked at their day in his now; and
THE RUNGS at their levels of his now.
```
- The grain words read are `today`, `tomorrow`, `this gathering`,
  `this beat`, `this minute` and `this breath`, for one line. For each of a
  list they are `its day`, `its gathering`, `its beat`, `its minute` and
  `its breath`.
- A smaller form serves anything else he writes into a recipe: `Set out
  THE X at this beat in his now beneath her reply, for his yes.`
- **"In Sundial's form"** is `Mark.write`, ported (`mark.js:86-97`):
  1. the title;
  2. the lines, one a row;
  3. `Tomorrow: …` when he voiced one;
  4. last, the places line: the phrases in P6 order, joined by ` · `,
     capitalised, ending with a stop, for example `What happened · what’s
     ahead · what you felt.`

  With no places it ends `In your own words.` (`mark.js:76-80`). Sundial
  writes no `place — ` prefix (`sundial.html:2770`), and nor does this.
- **His own line stays.** Sundial's rule is kept: the day's line is
  written only where today is empty, or holds a line in Sundial's form. A
  line of his own (a Rockpool telling) is left exactly as it stands, and
  the card says so: "Today already holds your own words; Keep it adds
  your moments." Only today's moments are added in that case.
- **The dot's light.** The keep's instant is kept in the day line's hidden
  directory, at 3, for the dot's light (3, point 3).

**The SetDay forms losing their P6 part.**
- The two shipped sentences (`Cook.kt:141-146`) lose "and each of THE
  PIECES beneath THE PORTALS in his P6 (the block p6: and his handle)".
  `Verb.SetDay` keeps the telling, the cards and the rungs, and gains the
  places and the moments.
- The old sentences still read for this one release, so an old cook can't
  refuse. They write no P6, and the trace says so.
- Nothing else is changed in the path:
  - `Days.blocks` loses its P6 branch (`Days.kt:227-242`);
  - `Days.landing` loses the pieces and portals (`:82-89`);
  - the day card loses "Your P6" and its portal moves (`AskScreen.kt:639`,
    `669-672`);
  - "Sort as my day" runs the same procedure as Keep today.

**Keep today as a kernel event.** kernel.json gains three events, and
`Cook.kernel` reads them by their labels:
```json
"5": "THE DIAL OPENED, AND NOTHING SAID TODAY. Run cook 6.3.",
"6": "KEEP TODAY, HIS TAP. Run cook 6.1.",
"7": "A SHELL LIFTED TO HIS EAR. Run cook 6.4."
```
- `Cook.load` checks that each named procedure stands, as it does for 1
  and 3 (`Cook.kt:950`).
- A kernel that names none of them (his, if he has changed it) runs the
  shipped one for that event only, and says so.
- Places 2 and 4 stay prose for the app's own checks, as now.

**The six pools, read from each day's places line.** This is mark.js
ported: `read` (`mark.js:101-126`), `placesOf` (`:131-136`), `counts`
(`:141-147`), `shares` and `evenness`.
- A day rests in a place when its own last line names that place.
- **Days kept before this release**, with no places line, rest where his
  P6 holds a piece of them.
  - Each piece's 2 is its day's address: `2026420500`, ten digits
    (`Days.kt:239`, `:349`).
  - Portal 1.1 is pool 1 (what happened), 1.2 pool 2, 1.3 pool 3, 2.1
    pool 4, 2.2 pool 5 and 2.3 pool 6, as `Mark.POOLS[].at` maps them
    (`mark.js:11-24`).
  - A day counts once in each place a piece of it rests. A day whose only
    trace is a piece (he unticked its line) still counts.
  - Listening is handed those pieces with the day.
- No piece is cut from a day after this release, and his P6 stays exactly
  as it is.
- The spiral counts days with a line, a moment or a piece, up to today,
  each once (1d).

---

## 5. The four laws as cook recipes

**Where they go.** All nine of his cook's places are taken (1b). The
proposal:
- **cook 6 becomes Sundial's ways**, a place whose places are procedures:
  6.1 KEEP TODAY (TELL MY DAY's job, kept one way), 6.2 PLACE, 6.3 GREET
  and 6.4 LISTEN.
- **LAW is split in two, as the phone needs it.**
  - Its reflect and ask stay where his talk is, at the TALK answer, cook
    1.1.8.
  - Its sheet, day and ahead go to KEEP TODAY.
- **Kept where they are:** the TALK line at 1.1.8.1, so his own line is
  kept by the upgrade, and cook 9's words, so "sort my day" and "into my
  now" still ask for a day.

**The forms each needs, named.**

| | form | what it does |
|---|---|---|
| N1 | `Run cook 6.1.`, `On his tap, run cook 6.1.`, and the kernel's `Run cook 6.1.` | the run forms read one or two digits; a top-level place whose opening ends "each a procedure beneath" holds procedures |
| N2 | `Carry THE X as THE Y.` | names what a procedure reads |
| N3 | `Gather what he said today, each moment with its instant, from the first after midnight on the beach's clock. Carry them as THE X.` | today's moments, from the moments file (3.4) and his now |
| N4 | `She names every place THE X plainly touch(es), one or more, never ranked, from the ring beneath: …; on E2B, or when her answer isn't whole, nothing is named. Carry them as THE Y.` | her placing; the words after the colon are what she is told; a tool returning a list |
| N5 | `Find the places of that ring THE X leaves out. Carry them as THE Y.` | the places untouched, as one line |
| N6 | `She gives her reply as THE A, THE B and THE C through the tool call; on E2B, or when her answer isn't whole, her reply stands as she writes it.` | her reply in named parts (6) |
| N7 | `She draws the day's sheet from THE X and today's line in his now, with this line: “…” On E2B, or when her answer isn't whole, she writes it in plain lines, the title first and tomorrow last after “Tomorrow:”. Carry it as THE Y.` | the keep card's sheet |
| N8 | `Read his last day kept before today, whole, with its tomorrow line. Carry it as THE X.` | for the greeting |
| N9 | `Read the latest nine days resting in THE PLACE, each whole with its date: …. Carry them as THE X.` | for listening |
| N10 | `Read what he whispered at THE PLACE, each with its date and the question it answered. Carry them as THE X.` | for listening |
| N11 | `Read THE PLACE's three ways of listening from Sundial's block, and which was used there last, with the questions asked there before, each with its date. Carry them as THE X.` | for listening |
| N12 | the keep card's SetDay form, in 4 | the keep, with grain words |
| N13 | `Set out a whisper beneath her words, for his tap: his words, kept in his stash with THE PLACE's phrase, today's date and THE QUESTION.` | the whisper, on his tap |

Also, `Hand` carries a list as one line of its items (1b). What the
existing forms already do, they keep doing: Is there, Hand her … with this
line, Cut, Find among … what he means to do, On his yes write it, ACCOUNT
and Stop.

**The hosting rules** run through every line handed to her:
- he is the authority on his day;
- she holds, notices, follows and reflects, adding nothing and taking
  nothing away;
- she never advises, interprets, diagnoses, praises or cheers;
- her questions point at the day, never at him;
- she never uses the words block, pool, passport, handle, beach, pscale,
  Being, Facts, Feelings, Fictions, Purpose, Decision or Outcomes.

**The safety lines** are Sundial's own words: "his local emergency number;
Samaritans 116 123 in the UK and Ireland", as LAW and LISTEN_LAW have them
(`sundial.html:2637`, `2699`).
- GREET_LAW and PLACE_LAW have none on the web, and none is added.
- The app's own guard stays as code: his safety words, the safety line in
  his turn and the note beneath her reply (`Talking.kt:123-124`, `148`;
  `AskScreen.kt:393`).

**TALK, cook 1.1.8: LAW's reflect and ask.** The plan's line goes at
1.1.8.1 word for word, as 1.1111787195 has it. The rest follows from 1.1.8.2.
```json
"8": {
 "_": "TALK.",
 "1": "Hand her this line: “The app: nothing is being kept or set out from this. Your reply always has two parts, and always ends with a question mark. Part one: give back what he said in this message, in his own words, beginning with You, adding nothing. Part two: one question about that same thing, from a place his words haven't touched, so he sees the day slightly differently. The places are: what happened, what he felt, what he made of it, what matters, a choice, what's ahead. If his words are mostly what happened, ask from another place; if mostly what he made of it, ask about what happened; and so on. The question stays on his day, something he can answer from the day itself; use what you hold about his day to shape it, but never read it out to him. Never ask how he is, what he's like, or why. Never advise, interpret, praise, cheer or wish him well. For example, he says: Just back from the shop, kettle's on. You say: You're back from the shop and the kettle's on. Once the tea's made, what comes first? Or he says: Today's going to be a write-off. You say: You think today's going to be a write-off. What's actually on for today? When he asks you something, answer it in a sentence or two instead. Only when he asks you what he has in front of him, name back today's line and the cards on his hand and ask which first. If he seems to be struggling, ask gently whether he'd like to just sit with it a moment. Speak to him as you, never by his name.”",
 "2": "Carry THE MESSAGE as THE WORDS.",
 "3": "PLACES. Run cook 6.2.",
 "4": {
  "_": "Is there THE UNTOUCHED?",
  "1": { "_": "Yes:", "1": "Hand her THE UNTOUCHED with this line: “The app: the places above are the ones his words haven't touched; your question comes from one of them.”" },
  "2": "No: walk on."
 },
 "5": "Hand her this line: “The app: if he may be in danger, your question says plainly where help is (his local emergency number; Samaritans 116 123 in the UK and Ireland), and that you'll stay.”",
 "6": "She gives her reply as THE REFLECTION and THE QUESTION through the tool call; on E2B, or when her answer isn't whole, her reply stands as she writes it.",
 "7": {
  "_": "Does THE MESSAGE tell of his day: three sentences or more and forty words or more in all, or news of a card on his hand?",
  "1": { "_": "Yes:", "1": "Set out the card “Sort as my day” beneath her reply, for his tap.", "2": "On his tap, run cook 6.1." },
  "2": "No: walk on."
 }
}
```
- **The danger line is a step of its own (1.1.8.5),** so his TALK line
  stays his, word for word. His line has no danger line, and Sundial's LAW
  has one (see 10).
- **The upgrade keeps his line.**
  - If his 1.1.8.1 stands as the plan quotes it, it is the new shipped
    sentence and nothing is offered.
  - If he has changed it since, it is kept as his, with the new one
    offered beside it (`Cook.up`, `Cook.kt:1010-1055`).
  - His old 1.1.8.2, the day question, moves to 1.1.8.7, and the old
    place is taken by the new step. Where he had changed 1.1.8.2, the
    release keeps his sentence at 1.1.8.7 and tells him.

**cook 6: Sundial's ways.**
```json
"6": {
 "_": "YOUR DAY — Sundial's ways, each a procedure beneath, walked as the others are: 6.1 KEEP TODAY, 6.2 PLACE, 6.3 GREET, 6.4 LISTEN.",
 "1": {
  "_": "KEEP TODAY — his day kept in Sundial's form, the one way a day is kept: on his tap of Keep today, on Sort as my day beneath her reply, or when his words ask for it (cook 9.4). Inputs: what he said today. Walk 6.1.1 to 6.1.9 in order; branch at 6.1.2.",
  "1": "GATHER. Gather what he said today, each moment with its instant, from the first after midnight on the beach's clock. Carry them as THE MOMENTS.",
  "2": {
   "_": "Is there THE MOMENTS?",
   "1": "Yes: walk on.",
   "2": { "_": "No:", "1": "ACCOUNT. He has said nothing today, so nothing is set out.", "2": "Stop." }
  },
  "3": "Carry THE MOMENTS as THE WORDS.",
  "4": "PLACES. Run cook 6.2.",
  "5": "SHEET. She draws the day's sheet from THE WORDS and today's line in his now, with this line: “The app: he is keeping today. From his words today, given above with their times, and today's line if one stands, draw the day's sheet as Sundial's keep card has it: a title, short, from his own words; two to four lines, in the second person, drawing together what he said and nothing he did not say, keeping what today's line already holds; and tomorrow, his intention if he voiced one, else empty. Never advise, interpret, diagnose, praise or cheer. Never use the words block, pool, passport, handle, beach, pscale, Being, Facts, Feelings, Fictions, Purpose, Decision or Outcomes.” On E2B, or when her answer isn't whole, she writes it in plain lines, the title first and tomorrow last after “Tomorrow:”. Carry it as THE SHEET.",
  "6": {
   "_": "HIS HAND.",
   "1": "CUT. Cut THE WORDS into its sentences; where it has fewer full stops than one in thirty words, as a spoken telling has, cut it instead before each of the joins at cook 9.4.1, a join matched wherever its words begin, so “and I” cuts before “and I also” and “and I've” too, keeping every word his and in order. Carry them as THE PIECES.",
   "2": "HAND. Find among THE PIECES what he means to do, by the words at cook 9.9, when what follows them names something to do, more than “do”, “do it” or “do that”; a piece cut at a join, with no I of its own, carries on the piece before it and is what he means to do when that one holds the words at cook 9.9. Each goes to the pile of his ahead its time words size it for (1 within hours: soon, in an hour, right now, in a bit; 2 today: today, tonight, this morning, this afternoon, this evening; 3 this week; 4 this month; 5 this season; 6 this year; 7 this decade, one day, someday; and by the time words he keeps), with no time words 3 this week; one naming a date (tomorrow, a day of the week, a date of the month) is a booking at that day in his now; and every piece naming a meeting, a call or an appointment of his with its day (today, later today, tonight, tomorrow, a day of the week, a date of the month) is a booking at that day in his now. Carry them as THE CARDS.",
   "3": "RUNGS. Find among THE PIECES those that name this week, this month, this season or this year. Carry them as THE RUNGS, each at its level of his now."
  },
  "7": "Set out his day on the keep card, for his yes: THE SHEET as the day's line at today in his now, in Sundial's form, THE PLACES TOUCHED named last in plain words; each of THE MOMENTS at its minute; THE CARDS on his hand (the block ahead: and his handle), those booked at their day in his now; and THE RUNGS at their levels of his now.",
  "8": "On his yes, write it.",
  "9": "ACCOUNT. Account what landed, and where."
 },
 "2": {
  "_": "PLACE — the places his words touch, named by her, as Sundial's placing law has it. Inputs: THE WORDS. Walk 6.2.1 to 6.2.3 in order.",
  "1": {
   "_": "She names every place THE WORDS plainly touch, one or more, never ranked, from the ring beneath: a place counts only where his words are plainly in it; on E2B, or when her answer isn't whole, nothing is named. Carry them as THE PLACES TOUCHED.",
   "1": "what happened",
   "2": "what he felt",
   "3": "what he made of it",
   "4": "what matters",
   "5": "a choice",
   "6": "what's ahead"
  },
  "2": "Find the places of that ring THE PLACES TOUCHED leaves out. Carry them as THE UNTOUCHED.",
  "3": "ACCOUNT. Account the places named."
 },
 "3": {
  "_": "GREET — her one question about today as the dial opens, before he has said anything today, as Sundial's greeting law has it. Inputs: his last day kept, and what he has said today. Walk 6.3.1 to 6.3.5 in order.",
  "1": "GATHER. Gather what he said today, each moment with its instant, from the first after midnight on the beach's clock. Carry them as THE MOMENTS.",
  "2": "LAST DAY. Read his last day kept before today, whole, with its tomorrow line. Carry it as THE LAST DAY.",
  "3": "Hand her THE LAST DAY and THE MOMENTS with this line: “The app: he has opened his sundial and hasn't spoken to you yet today. Ask him one question about today, in plain words, as a host would ask it aloud: something he can answer from the day itself. If his last day kept has a tomorrow line, your question follows from it and asks it from the day (where it went, what came of it, what today held of it), so that his line and your question read as one thought; the app shows him his line just before your question, so never repeat it, paraphrase it or answer it, and never ask whether he did it. If today already holds moments, ask what has come since, in his own terms from those moments, never naming a title or a label. If no day is kept yet, ask where he is and what today has been like. Never ask how he is, what he's like, or why; never walk him inward; never ask two questions. Never advise, interpret, diagnose, praise or cheer, and never use the words block, pool, passport, handle, beach, pscale, Being, Facts, Feelings, Fictions, Purpose, Decision or Outcomes. Speak to him as you, never by his name.”",
  "4": "She gives her reply as THE QUESTION through the tool call; on E2B, or when her answer isn't whole, her reply stands as she writes it.",
  "5": "ACCOUNT. Account the question asked."
 },
 "4": {
  "_": "LISTEN — she listens back over the days resting in one place, and what he whispered there, as Sundial's listening law has it. Inputs: THE PLACE, the place whose shell he lifted. Walk 6.4.1 to 6.4.9 in order; branch at 6.4.2.",
  "1": "DAYS. Read the latest nine days resting in THE PLACE, each whole with its date: a day rests in a place when its last line names it, or, for a day kept before Sundial with no such line, when his P6 holds a piece of it beneath that place, those pieces read with it. Carry them as THE DAYS.",
  "2": {
   "_": "Is there THE DAYS?",
   "1": "Yes: walk on.",
   "2": { "_": "No:", "1": "ACCOUNT. Nothing has come to rest here yet; the place's own question stands.", "2": "Stop." }
  },
  "3": "WHISPERS. Read what he whispered at THE PLACE, each with its date and the question it answered. Carry them as THE WHISPERS.",
  "4": "WAYS. Read THE PLACE's three ways of listening from Sundial's block, and which was used there last, with the questions asked there before, each with its date. Carry them as THE WAYS.",
  "5": "Hand her THE DAYS and THE WHISPERS and THE WAYS with this line: “The app: he is listening back to the days that came to rest in one place, given above with their dates, and to what he whispered there before. He is the authority on his days; no one can make meaning on his behalf. You hold, notice, follow and reflect, with nothing added and nothing taken away, and you return him to now; you never interpret. The way: choose one of the three ways of listening above, not the one used last time, and of the others the one that finds most in his own words; it decides only which of his words you gather. The heard: one to four short passages copied exactly from his days or whispers, one to a line, where that listening is most alive. The noticing: one or two whole, plain sentences, as a host would say them aloud, never a string of quotations or notes: what is there in his own terms and what is absent from it (a place his words never enter; a thing he'd like to do and hasn't reported doing; a feeling never named beside what happened; a word that repeats while another never comes; what a whisper added). His days and their dates are on the screen above, so don't list them or spend words on dates. Keep to his words for what he said; name the absence plainly; add no word of meaning; never say why, what it means, or what it shows about him. The question: one open question that returns him to now, asked from the place the absence points to, a place other than this one that his words lean toward and haven't entered (a wish among choices leans ahead, and under it toward what matters; a story told about what happened leans away from what happened; a hope leans on what matters; told as stories, ask toward what happened; looking ahead, toward what matters). Follow the newest words, or the whisper; the question follows from the noticing, so the two read as one thought, worded from his own words so it could only be asked of him and so it is plain which place it asks from. It carries no expectation and no hidden should: any answer, or none, is welcome. Never ask a question asked here before, one he answered in a whisper, or one that could be asked of anyone. When there are whispers, begin the noticing from the newest; if the newest was made today, he is already here: leave the question empty. If little is heard, say so plainly and leave the heard empty. Never advise, diagnose, praise or cheer; never name a feeling or state he didn't name himself; never ask why; never turn him in on himself (how he thinks, what he's like, what kind of person he is). Never name or describe the way of listening, and never use the words block, pool, passport, handle, beach, pscale, Being, shell, Facts, Feelings, Fictions, Purpose, Decision or Outcomes, except where his own exact words hold them. If what he wrote or whispered suggests he may be in danger, always give a question, and make it say plainly where help is (his local emergency number; Samaritans 116 123 in the UK and Ireland). Speak to him as you, never by his name.”",
  "6": "She gives her reply as THE WAY, THE HEARD, THE NOTICING and THE QUESTION through the tool call; on E2B, or when her answer isn't whole, her reply stands as she writes it.",
  "7": "Set out a whisper beneath her words, for his tap: his words, kept in his stash with THE PLACE's phrase, today's date and THE QUESTION.",
  "8": "On his yes, write it.",
  "9": "ACCOUNT. Account what she heard there, and what he whispered."
 }
}
```
Notes on the drafts.

- **They are faithful to the four laws.** Where the laws say "they" the
  app says "he", as every line of his cook does. The JSON instructions
  ("You return ONLY JSON …") are dropped, since the parts come through the
  tool call (6).
- **Shortened for Gemma only where the shape forced it.**
  - The sheet says "two to four lines", as the plan's keep card does.
    LAW's own day takes up to five.
  - GREET loses "the date, the time and the part of the day". Every turn
    already carries "(It is …)" (`HerWindow.kt:432`).
  - LISTEN is handed nine days, as the plan has it. The web hands the
    newest twelve (`sundial.html:3719-3741`).
- **Two guards stay code, as on the web.**
  - The heard passages are each kept only if they are an exact substring
    of his days or whispers (`sundial.html:3660-3672`).
  - The tomorrow line shown before her greeting is his own words, quoted
    by the app ("You said: “Tomorrow, …”", `sundial.html:2962-2966`), never
    hers.
- **The command words stay.** Cook 1.1.4 still answers "TELL MY DAY" and
  becomes `Run cook 6.1.`, so "sort my day" and "into my now" still keep a
  day.
- **The upgrade replaces cook 6 whole, once.** Place by place it can't
  hold: old 6.1 to 6.9 are steps, and new 6.1 to 6.4 are procedures. His
  old cook 6 is kept whole beside it, as `retired-cook-6`, the way his old
  telling was retired (`Hatching.kt:133-140`). Any line he changed there
  is named in the note at opening.
- **past-cook.json is 0.9's copy.** It must become 0.9.1's cook.json, for
  a phone with no shipped copy of its own. His phone has one
  (`Hatching.kt:144`).

---

## 6. Reflect and ask through the tool call on E4B, and E2B

**On E4B** (`world.big`, `Asking.kt:264`):
- Her kept conversation is opened with one tool for that turn:
  `reply {reflection: string, question: string}` for TALK, and
  `reply {question}` for GREET. They are named from the walker's N6 step,
  so his words in the cook name the parts.
- Automatic tool calling stays off, as with `choose` (`Gemma.kt:324`).
- The app sets out the reflection upright, then the question beneath it
  in italic, as `.sd-reflect` and `.sd-ask` have it
  (`sundial.html:2294-2301`). Where her greeting follows his tomorrow
  line, his line is italic and her question upright (`sundial.html:568-583`).
- Placing runs first. PLACE is a `choose`-like call of its own, with a
  tool `places {places: [1-6]}`, before her reply (`serveHer`,
  `Asking.kt:282-300`). Its untouched places are handed to her turn.
- The sheet on Keep today is its own call:
  `sheet {title, lines[], tomorrow}`.
- LISTEN is a conversation of its own, with
  `listening {way, heard[], noticing, question}`.

**What it costs, and what can go wrong.**
- **It no longer streams.** A tool call comes whole. Her first words came
  after about 2 s, streaming (1.1111). Now the whole reply comes at once,
  after placing and her reply.

  Sundial's own replies don't stream either (structured JSON,
  `sundial.html:4500-4565`). The dial pulses along its line while she
  writes (`sundial.html:476-499`), and the app does the same.
- **A tool call in a kept conversation is untried.** With automatic
  calling off, her history holds a tool call that nothing answers.
  - Gemma 4 on LiteRT-LM 0.18.1 hasn't been seen to keep a conversation
    sane across that.
  - The app records her parts as her plain turn ("You're back from the
    shop… / Once the tea's made, what comes first?"), so the next turn
    sees ordinary talk. If the engine won't allow that, the conversation is
    opened afresh with the parts as her turn, as an echo already does
    (`Asking.kt:572`).
- **A malformed call has thrown before.** 0.6.4's trial saw one throw from
  `sendMessage` (`STATUS.md:2603-2606`). Every tool call catches that and
  asks once more with no tools: "her reply stands as she writes it".
- **Lengths change.** `maxOutputToken` goes from `choose`'s 60 to her
  reply's own (`Jobs.kt:49`, 1,024). The echo filter (`Talking.unechoed`)
  runs on each part.

**On E2B** (the default for Talk, `Jobs.kt:49`, and a choice in Settings):
- There's no tool call, as the 0.9 bench found it worse than a free digit
  there (`FINDINGS-procedure.md:326-332`).
- Her reply streams free, under the same TALK line, as now.
- PLACE names nothing ("nothing is named"), so no untouched places are
  handed. Her line still asks her to ask "from a place his words haven't
  touched", and she judges.
- **The proposal for the display:** on E2B the app still sets the last
  sentence ending in "?" in italic beneath the rest. That's a reading of
  her free reply, not a trust in her shape. Matthew to say.
- The sheet on Keep today is her free text in plain lines: the title,
  then the lines, then "Tomorrow: …". It is read as `Mark.read` reads a
  day (`mark.js:101-126`), and anything left over is a line.
- The places on the keep card are his to tap. They are open to him on
  E4B too: he can tap a coloured dot off or on before Keep it, as he
  unticks a line now.
- GREET and LISTEN reply free. LISTEN's free reply is shown whole beneath
  the shell, with no heard passages set apart.

**With his own Claude** (7, 8): the same recipes and the same steps.
- Every she-step is a tool call (`strict: true`).
- The model counts as "big".
- So his cook governs every voice the same.

---

## 7. The shells and their sea, made on the phone; the films, drawn natively

**The shells.**
- **The three forms** are shell.js's generators, ported as code, not
  their output (`shell.js:14-66`): the nautilus, the whelk and the moon
  snail.
- **Each pool keeps its form and hue** (`shell.js` FORMS, `mark.js:11-24`):

  | pool | form | hue |
  |---|---|---|
  | what happened | whelk | `#4A6B8A` |
  | what you felt | nautilus | `#B0413E` |
  | what you made of it | moon | `#7A5C99` |
  | what matters | nautilus | `#2E7D6B` |
  | a choice | whelk | `#B8860B` |
  | what's ahead | moon | `#6FA8DC` |

- **Drawn** filled with the hue at 0.38, the body stroked in ink at 1.6
  and the lines at 1.
- **It rises** from rocking (±4 degrees, 5 s) to the ear pose: rotate
  -34/-27, scale 1.22/1.28, over 2.6 s. Ripple rings show while it waits
  (`sundial.html:238-300`).
- **An empty pool** offers no shell. It says "Nothing has come to rest at
  <phrase>." and shows its question (`sundial.html:3453-3485`).

**The sea**, made by the phone itself.
- An `AudioTrack` in float PCM, written by a synthesiser in Kotlin. There
  are no sound files.
- **Each place's water is shell.js's table, word for word**
  (`shell.js:112-119`):
  - the bandpass's centre and Q;
  - the swell's period and depth, from three LFOs;
  - the drift;
  - wind gusts (a second filtered noise);
  - a wave that gathers and breaks every 9 to 15 s, for "a choice" only;
  - the far lowpass at 650 Hz, for "what's ahead" only;
  - the level.
- **The part of the day softens it** at dawn and at night
  (`shell.js:120`). By day a far seabird calls now and then.
- **It starts only on his tap** and hushes over 1.6 s when the shell is
  put back.
- **The filters follow Web Audio's own definitions** (the RBJ biquads;
  the lowpass's Q is in dB), so the water sounds as it does on the web.

Two quirks of the web to decide on:
1. In `shell.js`, "what you felt" has wind 0.05, and the test is `> 0.05`,
   so it has no wind at all. The proposal is to keep it: that's how it
   sounds.
2. The three noise sources share one buffer, and the random `loopStart`
   does nothing, since `loopEnd` stays 0. Each source gets its own noise
   here: that is what "never the same twice" meant.

**Listening and whispers on the phone.**
- **The sea sounds while she listens** (LISTEN, 5). Her heard passages
  rise from the shell in an arc, then her noticing and question
  (`sundial.html:3814-3880`).
- **A whisper** is kept in `stash:<handle>` on the phone by "On his yes,
  write it" as one entry: `{_: "whisper · <phrase> · 3 November 2026", 1:
  his words, 2: the question it answered}`. That is whispers.js's own
  plaintext record (`whispers.js:113`), never sealed.
- **When a place is listened to,** its last three whispers come back,
  captioned "you whispered · <date>" (`sundial.html:3830-3839`).
- **Not done on the phone.** The web seals each whisper with his word,
  Argon2id and secretbox, and sends it to the beach as his lock
  (`whispers.js`, `gray.js`). The app never asks for his word. The two
  can't read each other's whispers, which is as Matthew said: the phone's
  days are the phone's own.
- **The ways and the questions.** Which way was used last in each place,
  and the questions asked there, are kept in an app-own file. On the web
  that is localStorage `sundial.heard`, up to nine per place. Her
  questions aren't his words, so they never go in a block.

**The films**, drawn natively.

There are nine (`films.js:66`). Their real titles differ from the plan's
wording: the plan quotes their first caption lines (`films.js:59-64`).

| # | film | title shown | stop |
|---|---|---|---|
| 1 | what | What this is | – |
| 2 | listen | How it listens | – |
| 3 | day | Your day | 0 |
| 4 | sundial | Your sundial | 1 |
| 5 | chat | In a chat | 2 |
| 6 | bring | Bring someone in | 3 |
| 7 | bay | The bay | 4 |
| 8 | back | Look back | 5 |
| 9 | night | The night watch | 6 |

- **The engine is code:**
  - one Compose Canvas;
  - a state of about 40 values, set each frame (`films.js:242-246`);
  - the eleven gestures as Kotlin functions (`films.js:247-306`, `339-396`);
  - fifteen primitives: fade, eased transform, ink revealed by a mask,
    stroke draw-on, typewriter, width grow, the finger and its ripple, the
    sun, the spiral and its fold, the colour bands, night, staggered
    fades, words rising in an arc, the flock and the captions.
- **The timing is films.js's formula** (`films.js:72-82`): each beat lasts
  `max(1.6 + words / 3, span × dur × 1.4)` s.
- **The heart's ink** is the two filled outlines of 321 and 841 points in
  `films.js:14`, revealed by two thick dashed strokes through a mask.
- **The only randomness** is the bay's 72 birds (`films.js:210-212`),
  seeded here.

**The scenes in Sundial's own block** (`words:sundial` 3, below):
- each film's title, poster and stop;
- beneath each, its scenes as `"<kind> · <dur>"`;
- beneath each scene, its beats as `"<at> · <line>"`.

Where a film has more than nine beats ("What this is" has ten), they are
grouped beneath two places, as three-calm-days.html already does
(`three-calm-days.html:919-941`). Fractions like `1/6` are written as such
and read as fractions. The choreography stays code, as on the web.

**Decisions the films need.**
- **The words say Claude, and the open.** "What this is" begins "I’m
  Claude, an AI made by Anthropic" and says "Your days are yours, kept in
  the open, under your name" (`films.js:38`, `45`). "In a chat" says "The
  same days, kept in the same place" (`films.js:60`).

  In the app, by default, she is Gemma, the days stay on the phone, and
  the phone and the web can't read the same days. The plan changes About's
  words "only where they say where your words go and whose voice speaks".
  The same rule should hold for these three lines. **The proposal is to
  redraft them for Claude to read**, since the newcomer's first screen
  plays "What this is".
- **"What's ahead" has two hues.** It is `#7FB2E5` in films.js
  (`films.js:86`) and `#6FA8DC` everywhere else. The proposal is
  `#6FA8DC`.
- **The road has an eighth stop**, "For young people", with no film.

**Sundial's own block, `words:sundial`.** The proposed shape, shipped
beside the procedures and read only:

| place | what it holds |
|---|---|
| 1 | the six places, in pool order, each with its phrase, name, hue, empty-pool question, three ways of listening (A, B and C, taken from `beings:night-watchman` 1.1 to 2.3 and stripped as `wayOf` strips them, `sundial.html:3624-3628`) and the turn's questions (from the spine:p6 the app already ships, `assets/procedures/p6.json`) |
| 2 | the turn's own words |
| 3 | the films |
| 4 | the road, as `spine:front-door` 3.6 stands at the build: each stop's name, line, state and film |
| 5 | the words on screen (the invites, "That’s enough for today. Come back tomorrow.", the keep card's labels, the strandline's) |
| 6 | About, as the app says it |
| 7 | the safety line, `sundial.html:2488` |
| 9 | where each was copied from, and when |

The ways and the road are snapshots, read by Claude Code at the build:
the app reads nothing from the web. One reading of the plan to confirm:
"shipped in the pocket like the teaching blocks" is taken as read only.
The four laws in his cook are his to change; these words change by
release.

---

## 8. Your room, the handle and room, the link; her voice in Settings

**Your room.**
- **The handle** is the shell he hatched as (`Hatch.hatched`,
  `Hatch.kt:99`).
- **The room** is the one his own shell names:
  - the first `pool:<name>` in his passport's place 3 ("Meet me in the
    pool: … (pool:Phenomemental)");
  - else his shell's manifest;
  - else `pool:<handle>`.

  It is read when a post is set out and shown on the card: "to
  pool:Phenomemental, as Phenomemental".
- **The guard stays code**, as authority at the actuator:
  - `PoolPost.prepare` takes the author and the one room from the hatched
    shell, never from his words, and refuses before a hatch;
  - `AUTHOR`, `POOLS` and Keeping's "Matthew" go (1e).

  Tests hatch a second handle and show its post goes to its own room
  under its own name.
- **The box and Post.** What he writes goes when he taps Post, by code,
  through the same one way out (`PoolPost.send`). Beneath it is the plan's
  line: "Write a note and tap Post. Or tell her any time, 'post this to my
  room', and she'll set it ready for you to send."
- **The notes sent**, newest first, with their words, time and entry, are
  kept in an app-own file (1e). Unsent ones are kept as now (Unsent.kt).
- **The link is left out.** No page shows a room and its entries
  (1.1111787172 says to leave it out, rather than send him to an error). The
  card says "Posted to your room, entry 189", with no link.
- **Before the hatch**, the room shows its film and what reaching the
  beach will add, and no box.
- **About's link.** Sundial's About links to its own people's pages
  (`sundial.html:2526`). The proposal is that the thanks to David link
  nowhere until there is a beach front page for people. Matthew to name it.

**Her voice in Settings, one of three.**
1. **Gemma on this phone** is the default. It is free and nothing leaves.
   E2B or E4B stays the choice per job (`SettingsScreen.kt:229-263`).
2. **Your own Claude.**
   - **The key** is entered once and encrypted with an AES key held in
     the Android keystore. Only the ciphertext sits in the app's
     preferences, never in a block, a copy or a saved conversation. "Take
     it away" removes both.
   - **The call** is `POST https://api.anthropic.com/v1/messages`, with
     `x-api-key` and `anthropic-version`, from the app's one network
     module. `api.anthropic.com` is added to the network config (1g).
   - **The model** is Sundial's own: `claude-sonnet-5-5`
     (`sundial.html:2622`), with low effort and the server-side fallback,
     as Sundial asks (`sundial.html:4500-4565`).
   - **The same recipes and steps** run (6). Each she-step is a tool with
     `strict: true`, and her reply's parts come through a tool or
     structured output (`output_config.format`).
   - **Said plainly**, on the voice card and in About: "Your words go to
     Anthropic each time she speaks: what you say, and what she holds of
     your day to answer it." That is her window: his shell's places,
     today's line, his hand and his people. "Your key costs a little each
     time."
   - **The post card's** "the only thing the pocket sends" is said truly
     (1e).
   - **The 40 a day.** Sundial caps a day at 40 turns
     (`sundial.html:2624`). The proposal is the same cap, said, for his key.
3. **A friend's code** goes under Coming soon. Sundial's steward and code
   need the beach (`sundial.html:4190-4302`).

**Also in Settings:**
- Where you are (2).
- Your name and your room, as read.
- Save and restore, from the Shore (`ShoreScreen.kt:139-149`).
- Show the workings, from the menu's Details (`ChatFrame.kt:86`).

---

## 9. The release

**Same package and key, so it installs over Rockpool.**
- `applicationId` stays `com.phenomemental.pocketbeach`
  (`app/build.gradle.kts:15`).
- It is signed in CI from the same repository secrets as every release,
  certificate `217887a6…` (`pocket-beach.yml:173-220`).
- `versionCode` 22 goes to 23 (`app/build.gradle.kts:18`).
- `versionName` is proposed as **0.10**, tagged `sundial-v0.10` (1a).
  Matthew may prefer 1.0 for the new name.
- Android keeps the app's files over an update, so his pocket, his now,
  ahead, P6, stash, cook, kernel and shipped copies all stay where they are
  (`filesDir/regions/pocket`).

**At first opening, as every release since 0.8.1 has done.** A whole copy
of the pocket is saved first (`Hatching.kt:106`). Then, in turn:
1. his cook and kernel are brought up place by place, his changed lines
   kept, with the new ones offered beside them once (`Cook.bringUp`,
   `Hatching.kt:95`);
2. cook 6 is replaced whole and his old one retired (5);
3. the new kernel events are added;
4. `words:sundial` is listed;
5. nothing in his now, ahead or P6 is touched.

The moments file starts empty: the dial shows his kept days from his now,
and today's moments from his first words after the update.

**Tests.** Unit tests in the core cover:
- `Sun.kt` against published times;
- mark.js's sums against its own fixtures;
- the grain function;
- the minute, breath and second rule;
- the upgrade of a 0.9.1 cook with his TALK line changed, as on his phone;
- the walker on every new form.

Device tests cover:
- Keep today end to end on the stand-in mind;
- the old P6 counted by address;
- a second handle posting to its own room.

The golden calls stay at their two commits.

**It is too much for one release. The proposal is three, in this order.**

1. **0.10, Sundial: Today and keeping.**
   - the name and the mark, Sundial's tokens, and the menu and its screens;
   - the dial, live on the real sun, with where he stands;
   - the moments, the minute rung and the moments file;
   - Keep today and its card, the SetDay forms without P6, and the kernel
     events;
   - the walker's new forms, cook 6, and TALK in two parts on E4B with
     PLACE and GREET;
   - Your days (the strandline) and the six pools on hold, read only, with
     the old P6 counted;
   - Your room, with the box, the handle and room read, and no dead link;
   - Your hand and Your recipes;
   - the TALK line shipped;
   - About's words.
2. **0.11: the shells.**
   - a colour tapped and its shell rising;
   - the sea;
   - LISTEN and whispers into his stash;
   - the turn, by hand, with its questions;
   - the ways in `words:sundial`.
3. **0.12: the films and her voices.**
   - the film engine and the nine films, Films in the menu, and Coming
     soon with the road;
   - Settings: his own Claude (the keystore, the network opened to one more
     host, the voice card's words), and a friend's code under Coming soon.

**Why this order.**
- Each release is something he uses every day: first his day kept and
  seen, then listened to, then explained and voiced.
- What the first release writes is what the later ones read: the day's
  line in Sundial's form, the places line, the moments at their minutes
  with their instants. It has to be right, and lived with for a few days,
  before listening is built on it.
- Each carries one new risk:
  - the first, a tool call inside her kept conversation on his Pixel;
  - the second, a synthesiser playing in real time;
  - the third, the app's first way out beyond the one post. That one is a
    change to what "nothing leaves the phone" means, and it deserves a
    release, and a line in About, of its own.
- The films are the largest piece of drawing code. They need nothing the
  first two lack, and nothing waits on them except the newcomer's way in,
  which the plan already puts after this release.

---

## 10. What in the plan looks wrong, and why

1. **"Each becomes a procedure in his cook"**: his cook has no room
   (1b). Two-digit procedures under cook 6 is the proposal (5). The
   other way is a second procedures block. That would make "his cook"
   two blocks.
2. **The mark's colours** in 1.1111787191 are not Sundial's (1a). Using
   them would give the launcher a different paper and crimson from every
   screen.
3. **"From his passport's own location"** can't work for him. His
   passport has no location, and a spatial:earth address can't be read
   into latitude offline (1g). It's harmless as a first choice that finds
   nothing; the town list does the work.
4. **Sundial's block, as listed, leaves out two things listening and Coming
   soon need:** the three ways of listening, and the road. Both are read
   live from the beach on the web (7).
5. **The TALK line has no danger line.** Sundial's LAW has "if they may be
   in danger, make ask say plainly where help is … and that you'll stay"
   (`sundial.html:2637`). The app's safety words catch only the words in
   their list (`Talking.kt:135-140`). Shipping his line word for word, as
   the plan says, needs the danger line as its own step (1.1.8.5). Without
   it, a worrying message that misses the list is met only by "sit with it
   a moment".
6. **The film words claim Claude and the open web** (7). As drawn "from
   their scenes as films.js gives them", the newcomer's first film would
   say "I’m Claude" over a Gemma reply, and "kept in the open" over a
   pocket that is private.
7. **"The light of the hour it was kept"** needs the keep's instant kept
   somewhere. Most of his days so far have none. They'll be ink, and the
   spiral will light only from this release on (2 and 3).
8. **The day turns at 1 am in summer.** "Today" on the beach's UTC day is
   right by pscale://sundial. But the date at the top will read yesterday
   for that hour, and the plan should say the app tells him so (3).
9. **The sheet's lines are hers, in the second person,** not his words.
   The 0.6.4 guard "every line proposed is his words" (`Keeping.kt:192-197`)
   can't apply to them. What keeps him the authority is that he sees the
   card whole before Keep it, and that his own words are kept whole
   beside it as moments. Worth saying in the plan, since the guard is
   otherwise held everywhere.
10. **"With Claude the reply returns in Sundial's own shape (reflect, ask,
    sheet, day, ahead)"** sits against "the same recipes guide whichever
    voice speaks". Sundial's shape returns the sheet with every reply. The
    phone's recipes make it on Keep today. The proposal is one shape for
    every voice, the phone's (6), so his cook governs Claude as it
    governs Gemma.
11. **Nine days "whole" in 8,192 tokens is tight** for his spoken
    Rockpool tellings. Nine of 300 words each is about 3,600 tokens,
    before the law, the ways, the whispers and her reply. The proposal is
    to read whole up to a per-day cap (1,500 characters, the walker's own
    aperture) and say "…" past it, as READ does.
12. **"What it will do … the dot on the three lines when one is new"**
    belongs to Reach the beach. Until then the dot can light only for
    something on the phone, such as an unsent post, or not at all. The
    proposal is not at all, so it means one thing when it comes.

---

*Findings only. Nothing is built, and nothing was written to the beach. The
build waits for Claude to read this whole and say go.*
