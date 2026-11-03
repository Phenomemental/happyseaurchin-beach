# 0.10: Sundial, Today and keeping — judged

3 November 2026, for spine:pocket-beach 1.111178719 and the decisions at
1.11117871991 and beneath (911 to 919), with 1.1111787197 as redrafted at
07:47 and 1.111178719915 as it stood after 09:41. Where the decisions differ
from the plan's nine places or from FINDINGS-sundial.md, the decisions were
built. The build is section 9 of the findings, 0.10's part, as 911 sets it.

**Same package and key:** `com.phenomemental.pocketbeach`, signed in CI from
the repository's secrets. `versionCode` 23, `versionName` 0.10. The release
job now makes `sundial-v0.10`, and still accepts `rockpool-v` and
`pocket-beach-v` tags pushed by hand. **The release is held:** the job has
not been run.

## The verdict

**Every test named in the findings passes: the core's, the app's, and on
the emulator.** CI run 217, at `f85265d`, is green on all of it:
- the core on Linux and on Windows;
- the golden calls, regenerated and compared;
- the app's own tests;
- all 71 device tests on the emulator, and the screen tour after them.

Runs 215 and 216 failed first, on 7 and then 1 of the 71. What they caught
is under "What the emulator caught" below. The commit that adds this line
changes only this file.

What no test here can show: **her tool calls with a real model.** The new
calls (the places his words touch, the day's sheet, and her reply in two
parts on E4B) ran only against the stand-in mind. No laptop judge with
Gemma was run for 0.10. Each falls back to her plain words when the call
doesn't come whole, and the fallbacks are tested. But whether E4B on his
Pixel gives them whole is untried. The findings named this as 0.10's one
new risk (section 9); it is still the one to watch.

| The test, as the findings named it | Where | Result |
|---|---|---|
| `Sun.kt` against published times | core, `SunTest` (London midsummer and midwinter, Sydney, Tromsø's polar day and night; sunrise-sunset.org and NOAA's sheet) | passes |
| mark.js's sums against its own fixtures | core, `MarkTest` | passes |
| the grain function | core, `SundialTest` | passes |
| the minute, breath and second rule | core, `SundialTest` | passes |
| a 0.9.1 cook with his TALK line changed, brought up | core, `CookTest`, `HatchGoldenTest` | passes: his line kept, the shipped one offered beside it at 1.1.8.1 |
| the walker on every new form | core, `CookTest`, `SundialTest` | passes: KEEP TODAY, PLACE, GREET and LISTEN walked whole |
| Keep today end to end, on the stand-in mind | emulator, `SundialDeviceTest` | passes, run 217: the moment waits, the card sets it out, his yes keeps it in his now, no P6, the waiting room empty |
| the old P6 counted by address | emulator, `SundialDeviceTest` | passes, run 217: one day, counted by its address; what happened and what he felt, one day each; his P6 untouched |
| a second handle posting to its own room | emulator, `SundialDeviceTest` | passes, run 217: pool:ada-lovelace, under ada-lovelace, "Posted to your room, entry 7" |
| the golden calls, at their two pinned commits | CI `test` | passes, regenerated and compared |

**Beyond those:**
- **Core:** 264 tests, none skipped, on Linux and in CI on Windows.
- **App unit tests:** 14, one skipped as before (the real fetch).
- **On the emulator:** 71 device tests, Rockpool's among them. Six are new
  in `SundialDeviceTest`:
  - Keep today on the dial;
  - yesterday's words offered first;
  - a Rockpool day counted by its address;
  - a second handle posting to its own room;
  - the dial's one question after his tomorrow line;
  - a town from the list, and his waiting words under Show the workings.
- **The screen tour** photographs Sundial's screens, light and dark: Today,
  her reply, the keep card, Kept, Your places, Your days, the pools, Your
  room, Your hand, Settings and About. They are in CI's `screens` artifact.

**What the emulator caught** (runs 215 and 216; the six new tests passed
from the first):
- **A post kept for Post now was dropped on reloading, before a hatch.**
  It was re-read against his room, and before a hatch there is none. A
  real fault, now fixed: each kept post keeps its author (under Your room,
  below).
- **Five older tests waited for the companion's post to be offered**
  before a hatch. It now says it waits for his shell.
- **Two expected "Matthew calls me Gemma."** Her name line is now "He calls
  me Gemma." (918).
- **The screen tour tapped Try a call by hand** with the workings hidden.
  On About it now shows only under Show the workings.

**Changed device tests, and why:**
- **Rockpool's day card** (day-move, day-to, a P6 written) is now the keep
  card. `hisDayIsSetOutTheBeachsWay` is `hisDayIsKeptInSundialsForm`: his
  day in his now, no P6.
- **The name and the mark:** SUNDIAL, About's opening line, and the pocket's
  workings sentence under Show the workings.
- **The companion's hello world before a hatch** is written and checks out,
  and its post waits for his shell. That is the room rule (918). The tests
  of a post the beach couldn't receive, and of the hello world by hand, now
  hatch first and post to his room.
- **0.9.1's upgrade tests** offered his 5.6 against 0.9's cook. Over
  0.9.1, 5.6 hasn't changed, so nothing is offered for it. They now bring
  up a 0.9.1 pocket with his 5.6, his own TALK line and a line of his old
  cook 6 he rewrote:
  - cook 6 becomes Sundial's;
  - his old one is kept as `retired-cook-6`, the changed line named;
  - his TALK line is kept and the shipped one offered once;
  - Use the new one takes it, three deep.
- **The test pocket marks today as greeted** (`Fresh.clear`). The dial
  asks her its question once a day as it opens. Every test's first words
  are its own, and the greeting has its own test.

## How it was run

- **Core:** `./gradlew :bsp:test -PcoreOnly`, on this machine and in CI.
- **App:** `./gradlew :app:testDebugUnitTest`.
- **Emulator:** CI's job, a Pixel 6 profile at API 36, the app built from
  the branch, `connectedDebugAndroidTest`, then the screen tour on its own.
- **Goldens:** `conformance/generate.sh` against bsp-mcp-server and
  pscale-beach at the pinned commits. The hatch case (`HATCH_WRITE=1`) was
  regenerated for the new cook and kernel.

## Where the build departs from the plan, and why

### The name, the mark and the tokens (911, 913)

- **Built as decided.** The app's label is Sundial; Sundial's tokens are
  the theme, light and dark; the launcher's mark is the small heart and
  nine ink stones on Sundial's paper.
- **The mark on About is drawn in code:** the same small heart and nine
  ink stones on Sundial's paper. Rockpool's mark is gone from the app.

### The dial (911, 914)

- **Built as decided:** live on the real sun, ticking every 30 s, with
  s = sin(elevation) and the light, shadow and tints of mark.js.
- **Where he stands:**
  - asked once, on Today, as ACCESS_COARSE_LOCATION;
  - the phone's last known fix only, rounded to 0.1°. The app makes no
    location request of its own. With no fix, it says so and offers a town;
  - declined, or later: a town from the GeoNames list (towns of 15,000 or
    more, 1.1 MB, CC BY 4.0), searched in Settings.
- **Before he has said where he stands,** the dial glows as it does at
  night: no sun and no shadow. It never guesses, and never reads the
  passport.
- **The shadow's blur is Android's** (`BlurMaskFilter`), not the SVG
  filter's. It reads the same at the dial's size.
- **The summer note** ("Your day turns at 1 am in summer.") shows only
  between midnight and one, in summer, where summer time is an hour ahead
  of the beach's clock. Elsewhere the day turns at another hour, and the
  note would be wrong.

### Moments, the minute rung and the waiting room (915)

- **Built as decided,** with the four conditions of 09:41:
  - (a) Keep today writes and clears;
  - (b) a new day offers yesterday first, "You spoke yesterday and didn't
    keep it", kept at yesterday's address, before today's question; older
    words are let go;
  - (c) Settings, under Show the workings, shows "Today's words, not yet
    kept", and a whole copy carries them and puts them back;
  - (d) his words and their instants only.
- **The file is `moments.json`,** in the app's own files, never a block.
- **A moment is his whole message,** command words and all ("Put this in
  my now: …"). The decision says his words; it doesn't say which of them.
  The app cuts nothing from what he said.
- **Only once he has hatched.** Before a hatch there is no now to keep
  into, so nothing waits.

### Keep today and the keep card (912, 916)

- **Built as decided:**
  - the card is Sundial's form: the sheet, the places as dots to tap off
    or on, his moments, his hand;
  - his own line is never overwritten, and the card says so;
  - Sort as my day runs 6.1;
  - no P6 is written.
- **"Put this in my now" needed her a line.** The decisions route it to
  6.1. 0.9.1's TELL MY DAY at 1.1.4 handed her the line that says the day
  is below for his yes, "It's below, for your yes." Without it she was
  told nothing and could say his day was written, the fault 0.9.1's check
  (a) was made to catch. 1.1.4 now hands her that line, in Sundial's words
  ("on Sundial's keep card, for his yes"), then runs cook 6.1.
- **Bookings:**
  - a booking at the day being kept isn't written as a line of its own.
    The day's line is the sheet. A "later today" stays in his moment, word
    for word;
  - a booking at a later day is written there as Sundial's line for a day
    ahead, "… / What's ahead.";
  - a Rockpool booking already at a later day counts as his own line, so
    that day's line stays his.
- **Every place tapped off:** the places line says Sundial's own "In your
  own words."
- **His old cook's day forms** (0.9.1's "Set out his day … in his P6") are
  still read, so a cook he changed still walks, but they write no P6.

### Cook 6, the TALK line and the kernel (912)

- **Built as decided:**
  - cook 6 replaced whole, once; his old cook 6 kept whole as the block
    `retired-cook-6`, and the opening note names the lines of it he had
    changed;
  - the TALK line at 1.1.8.1, word for word, asserted equal to the plan's
    in the tests;
  - the danger line at 1.1.8.5;
  - his old 1.1.8.2 moved to 1.1.8.7 where he had changed it;
  - kernel 5, 6 and 7 by label;
  - a kernel naming none of them runs the shipped one, and says so.
- **A fix the TALK line needed.** "Use the new one" at a place three deep
  (1.1.8.1) failed with "There's no line at 1.1.8.1". The offer's address
  was used as written, not as a spindle. It is now read through
  `Cook.spindleOf`. This was latent since 0.9.1, whose offers were all one
  or two deep.

### TALK in two parts on E4B, PLACE and GREET (911, 916)

- **Built as decided:** one reply shape. On E4B, THE REFLECTION and THE
  QUESTION come through the tool call. On E2B, her last sentence ending in
  "?" is set in italic beneath the rest.
- **How the two parts are asked.** LiteRT-LM 0.18.1 fixes a conversation's
  tools when it is made. So her two-part reply is asked in a conversation
  of its own, seeded with her instruction and the conversation so far. Her
  kept conversation is then reopened with that reply in it, so her next
  turn follows on. Not yet tried on his Pixel.
- **The places, PLACE (6.2), on E4B** are named through a "places" tool,
  never ranked. On E2B, or when the call isn't whole, none are named, as
  6.2.1 says. He taps the dots.
- **The day's sheet** comes through a "sheet" tool on E4B. Otherwise it is
  written in plain lines and read as mark.js reads a day.
- **GREET (6.3)** asks her once a day, as the dial opens on a day he hasn't
  spoken, in her kept conversation. His tomorrow line from his last day
  kept stands just before her question.
  - Until she answers, or if she can't, Sundial's own question stands:
    "What's with you this morning?", by the part of the day. The turn of
    0.11 isn't here.
  - Asked once a day: the day, his line and her question are remembered in
    the app's settings, never a block.

### Your days and the six pools, read only (911, 915, 916)

- **Built as decided:**
  - the strandline newest first, each day's shell as its coil;
  - the six pools on hold (on the dial) and on Your days;
  - days counted up to today, Rockpool's days by their address in his P6;
  - a pre-release day takes the newest P6 stamp of its pieces.
- **The count** says "1 day" for one, not "1 days".

### LISTEN (911, 916)

- **Shipped, walkable, not yet heard:** cook 6.4, kernel 7, the whisper
  card and its write to his stash. Nothing in 0.10 lifts a shell, so
  nothing fires kernel 7 until 0.11. The ways in `words:sundial` are 0.11's,
  so THE WAYS is empty here.
- **Nine days, each to 1,500 characters with "…",** as 916 says. The "six
  if they don't fit" is left to 0.11, when she is first given them and the
  fit can be measured.

### words:sundial (917)

- **Named `words:sundial`,** beside procedures, read only, listed in the
  shelf's index. It holds:
  - place 1, the six places;
  - place 5, the words on screen;
  - place 6, About;
  - place 7, the safety line;
  - place 9, where each was copied from.
- **The rest are later releases'** and aren't in it yet: the ways, the
  turn, the films and the road.

### Your room (918)

- **Built as decided:**
  - a box and Post;
  - the author is the hatched handle;
  - the room is the first pool his passport's place 3 names, else his
    shell's manifest's, else pool:<handle>;
  - AUTHOR, POOLS and Keeping's "Matthew" are gone. Her name line is now
    "He calls me Gemma.";
  - "Posted to your room, entry N.";
  - the notes he sent are kept in the app's own file;
  - no dot lights;
  - the model note names E4B when it is running.
- **Before a hatch, nothing is posted.** With no handle there is no author
  and no room. So the companion's hello world before a hatch is written,
  and its post waits for his shell; the hello world by hand says the same.
  Rockpool posted these as "Phenomemental", the name 918 retires.
- **A post kept for later goes as it was made.** The room rule first
  re-read his room for every post kept for Post now. Before a hatch that
  dropped the post, and the emulator caught it (run 215). Now each kept post
  keeps its author. One kept by Rockpool, with no author kept, goes as
  Rockpool made it: Phenomemental, to pool:Phenomemental.
- **Scratch pockets carry a stand-in room.** A scratch pocket is the
  bench's, or a trial's; it has no hatch. Its stand-in room is Matthew's
  (pool:Phenomemental), so a post can be offered and judged there. Every
  post made there is marked scratch and never sent (see "After Claude's
  read", 3, below).

### Your places, the menu (911, 917)

- **Built as decided:** the three lines on every screen open Your places,
  in Sundial's form: YOURS (Today, Your days, Your room, Your hand, Your
  pocket, Your recipes), then Settings and About.
- **"The workings" at its foot:** Teaching blocks, Bench, Save this
  conversation, and Show the workings. Sundial's menu has no place for
  them. They are the app's own and are used, so they are kept, quietly,
  beneath. Show the workings is in Settings too.
- **Her name and "wearing your shell" stay,** small, above the input. Rockpool
  showed them in the top bar, which is now Sundial's wordmark and the date.

### Settings (914, 1.1111787197)

- **Built:**
  - where you are;
  - your name and your room;
  - save and restore (on Your pocket);
  - Show the workings;
  - the safety words and the ask log, as before.
- **"Her voice" is, for 0.10, the companion's model settings,** under
  that heading. Her other voices are 0.12's (919).

### About (1.1111787197)

- **Read from `words:sundial` place 6, as redrafted:**
  - the opening line;
  - the credits made true;
  - Why it exists;
  - Why a sundial;
  - How it holds you;
  - Where your words go, rewritten;
  - What it draws on, and If you want more;
  - What it is not, and If you need help now;
  - Sundial's links.
- **Rockpool's items at its foot:** the version; the font's licence; the
  teaching blocks' MIT licence; GeoNames, CC BY 4.0; "Made by Matthew
  Bushell"; and the thanks to David, linking to
  https://beach.happyseaurchin.com.
- **The sentence about pscale blocks, bsp and the cook** shows only under
  Show the workings, with "Try a call by hand". It now says "Sundial keeps
  pscale blocks…".

## After Claude's read (3 November)

Claude read this file whole and asked for two changes and one check
before the release. All three are done.

1. **Why it exists claims no daily limit.** "It is bounded: each day has a
   limit, and when it's reached, it rests until tomorrow." is gone from
   `words:sundial` 6.3. The app sets no daily limit, and none is planned.

2. **Where your words go names one thing that leaves.** It now says: "One
   thing can leave, and only when you choose: a note you post to your
   room, sent when you tap Post." The sentence about her own-Claude voice
   is held at `words:sundial` 6.6.9, for 0.12. About doesn't read 6.6.9:
   it shows each part's heading, its line 1, and a part at 2.

3. **No scratch pocket can send a post to the online beach.** It was so
   already, and now it is enforced.
   - **The one way out.** A post reaches the network only through
     `PoolPost.send`'s `wire.post` (`bsp/…/PoolPost.kt:109`). The app calls
     it only from `PocketApp.post` (`app/…/PocketApp.kt:119`), on the app's
     `wire` (`PocketApp.kt:84`, HttpsWire).
   - **Its four callers each build the post from the phone's own pocket**
     (`app.storage`), never from a scratch one:
     - Your room (`ui/SundialScreens.kt:275`);
     - the hello world by hand (`ui/CallsScreen.kt:305`);
     - Ask's Post, from `Asks.carryOut` on `app.storage` (`ui/Asking.kt:1213`,
       `1392`, sent at `1415`);
     - Post now, for a kept post (`ui/Waiting.kt:61`).
   - **The scratch pockets reach no wire:**
     - the bench's pocket (`bsp/…/Bench.kt:91`, run from `ui/Benching.kt:230`);
     - the trials' copies (`Asks.kt:277-280`, `Acts.kt:224-227`,
       `Pages.kt:380`);
     - the walker's `send`, which sends nothing (`Cook.kt:416`; Ask's,
       `ui/Asking.kt:275`, only reports what Post did);
     - the laptop trials, which use neither Wire nor PoolPost (`trial/src`).
   - **The block, so no later path can open one:**
     - **Every room read from a pocket held in memory is marked scratch.**
       Hatched or stood in, it carries the mark (`PoolPost.kt:77-78`,
       `89`), and so does the post made for it (`PoolPost.kt:102`).
       `PoolPost.send` refuses a scratch post before the wire is touched
       (`PoolPost.kt:107`).
     - **The app posts only from its own hatched pocket, under its own
       handle, to its own room.** Before a hatch it refuses; a post made
       as another handle, or for another room, it refuses; a scratch post
       it refuses (`PocketApp.kt:136-143`). Nothing refused touches the
       wire.
   - **Tested:**
     - core, `SundialTest.aScratchPocketsPostNeverTouchesTheWire`: the
       bench's, a hatched scratch pocket's and a trial copy's posts never
       touch the wire, and the phone's own goes;
     - on the emulator, `CompanionTest.aPostThatMayHaveLandedSaysSoAndCanBeDiscarded`:
       before a hatch, a bench post, and one made as another handle, are
       refused, the wire untouched, and his own goes.

**Tested and released.** CI run 220, at `3a3040a`, was green on all of it:
the core (264 tests) on Linux and Windows, the golden calls, the app's
tests, and all 71 device tests on the emulator. PR #236 merged it to main at
`c564ec8`. Release run 222 ran every test again there, then built and
published `sundial-v0.10`:
- the APK is signed with the same key (certificate SHA-256 `217887a6…`);
- `com.phenomemental.pocketbeach`, versionCode 23, versionName 0.10;
- APK SHA-256 `a1908891…`;
- its procedures and town list are byte for byte main's, About's two
  changes among them.

https://github.com/Phenomemental/happyseaurchin-beach/releases/tag/sundial-v0.10

## What to try on his Pixel

1. **Open it over Rockpool.** The opening note should say his cook was
   brought up, cook 6 is Sundial's ways, and his old cook 6 is kept as
   retired-cook-6. Your days should hold his Rockpool days, counted by
   their address.
2. **Say where you are,** or let the phone tell it. The dial's light should
   follow the sun through the day.
3. **Say something; then tap Keep today.** The card should set out a short
   sheet in her words, the places dotted, his moment, his hand. "Keep it"
   should say "Kept." and the day should be on Your days.
4. **On E4B, watch her reply.** It should come as a reflection, with the
   question in italic beneath. If it comes plain every time, the two-part
   call isn't arriving whole on the phone.
5. **Tomorrow, open the dial before saying anything.** His tomorrow line
   should stand above her one question.
6. **Post a note from Your room.** It should land in pool:Phenomemental
   under his name.
