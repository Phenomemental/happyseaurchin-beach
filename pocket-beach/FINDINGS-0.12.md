# Sundial 0.12: findings before the build

21 November 2026, for spine:pocket-beach 1.11117871994 (the decisions
after the local-first findings) and its nine parts, read beside the earlier
plan for the shells and the sea (1.111178719911 and the parts it names).
**Nothing is built.** Nothing was written to the beach. This is the read
of the code against what 0.12 is now meant to be, so Matthew and Claude can
settle it before the build.

**What was read**

- **The decisions,** 1.11117871994 and 1.111178719941 to 1.111178719949,
  each whole.
- **The earlier plan for the shells and the sea,** each whole:
  - 1.111178719911 (the three releases, in which 0.11 was "the shells, the
    sea, LISTEN and whispers into his stash, the turn, the ways");
  - the parts it names: 1.1111787192 (Today, the face, the shells, the sea,
    the six places as a lens, whispers), 1.1111787193 (where Gemma comes
    in), 1.1111787194 (the four laws as recipes) and 1.1111787197 (Keep
    today, the menu, the films, her voice, Coming soon);
  - and beside them, 1.111178719912 (cook 6), 1.111178719917
    (words:sundial), 1.111178719918 (Your room) and 1.111178719919 (her
    voice).
- **seed:sundial-shaping,** whole, as it stands since its adjustment at
  about 13:30 today. The language engine has moved to its own block, and
  branch 7 now says how it is applied afterwards.
- **The app on main at `0f1769e`** (Sundial 0.11, released today), read for
  every place 0.12 touches. The beach's own `passport:Phenomemental`,
  `shell:Phenomemental`, `now:Phenomemental` and `ahead:Phenomemental`
  were read only, to see what onboarding by handle would meet.
- **The laptop trial** run for this (`trial/free-reading.md`): Gemma with
  free reading against Gemma as she is, on the line in the sand, and one
  shaping timed.

References are `file:line` from `pocket-beach/`, as in the earlier
findings.

---

## The short version

1. **Free reading didn't win, so 0.12 shouldn't take it on these words.**
   On the laptop's Gemma 4 E4B, against the line in the sand's
   conversations (two samples each), the scores were:

   | Arm | Score |
   |---|---|
   | as she is | 10½ of 24 |
   | free reading | 10 |
   | free reading with two of its sentences mended after the first run | 13 |

   - **The reading itself brought nothing.** In 48 free turns she called
     walk twice, both times stopping at her clock's opening line, and never
     fetched his hand. So test 3 failed in every free sample, where Gemma
     as she is named today's line and two of his three cards.
   - **The mended arm's lead came from its smaller, plainer window,** not
     from reading.
   - **What to keep:** her window as it is. Before free reading is built,
     a tool that says what it gives ("his hand", "today's line") should be
     tried on the laptop. See section 3 and `trial/free-reading.md`.
2. **Shaping with Gemma, asked as the seed asks, made no block,** twice.
   - **The seed's small cut leaves out the return.** It holds 5.3.2's nine
     opening lines and branches 4, 6 and 8, but not 1.1 (the shape a block
     is written in) or 5.1.1 (what to return).
   - **Even with them added, she broke the rules.** She copied the
     author's sentences into the leaves, in place of ¶ numbers, and ran out
     of room.
   - **Proposal:** Gemma returns plain lines (the root, each part's framing
     line and its ¶ range) and the app builds the block. Written that way,
     a 1,427-word text would take about a minute on his Pixel. Most of the
     five minutes measured went on writing what she shouldn't have.
3. **Of the shells and the sea:**
   - **Built already:** Your days, the pools and one shell after Keep
     today.
   - **Still fits:** the three forms, the sea, LISTEN on a lifted shell,
     the turn and the ways. The last two need words:sundial's missing
     branches.
   - **Doesn't fit as written:** whispers "kept by the phone's own lock",
     now that his stash may be in his folder; and LISTEN as a walk rather
     than nine days handed.
4. **Matthew is fixed in the code in more places than the decision
   names.** Besides his handle, his three lines and his pool as a fallback,
   about forty lines of her instructions say "Matthew". The shipped cook
   says "he", "his" or "him" 292 times. Writing the recipes for any person
   is the largest piece of "other people's shells" (section 6).
5. **Onboarding by handle can't keep the beach's shell as the phone's.**
   On the beach, shell 5 is his desk; on the phone, 5 is her window. So
   the phone must make its own shell from what it reads. The clock's name
   differs too. Which key goes into the keystore needs saying, and sealed
   lines would need a port of the beach's sealing (section 5).
6. **How long:** 22½ to 29½ working days with free reading built, 20 to
   26 without it, and 3 to 5 more if sealed lines are read.
   That is about the size of the local-first release, so it could be two
   releases (section 9).

---

## 1. What 0.12 is now

By 1.111178719941, 0.12 is "Gemma freed and other people's shells":

- free reading;
- recipes;
- onboarding by handle, with Reach the beach for reading;
- sharing in, shaping with Gemma, and sharing out;
- the shells and the sea planned for 0.11, "whatever of them still fits".

The block editor (search, opening in place, the Map) is 0.13. The films,
his own Claude and her voice are 0.14.

---

## 2. The shells and the sea, read again

What was planned for 0.11 at 1.111178719911, against the code and the
decisions.

### 2a. Already built, standing

- **Your days.** The strandline, newest first, and the six pools, read
  only (`ui/…/SundialScreens.kt:157`, `:273`).
- **One shell after Keep today.** One shell with the day's title appears,
  and a tap opens the day's page (`ui/…/SundialScreens.kt:236`;
  `ui/…/Asking.kt:1096`).
- **The six places as a lens.** A place's pool is every day whose own last
  line names it; nothing is cut from his day. This is `Keep.restingIn`, as
  1.1111787192 asks.

### 2b. Not built, and still fits

- **The shells' three forms,** drawn natively from shell.js. Nothing in the
  decisions touches them: they are drawing alone.
- **The sea in a shell.** Soft noise, filtered and slowly swelling, each
  place's water its own, made on the phone. It needs no network and no
  model; it still fits. It needs an audio path, which the app doesn't have
  yet.
- **Lifting a shell runs LISTEN.** The parts are there but not joined:
  - the recipe is shipped: cook 6.4 LISTEN (`assets/procedures/cook.json`),
    and kernel 7, "A SHELL LIFTED TO HIS EAR. Run cook 6.4";
  - the walker reads its verbs: Whispers, Ways and SetWhisper
    (`bsp/…/Cook.kt:93-95`, `:239-241`);
  - the kernel event is read (`bsp/…/Cook.kt:441`, `:1480`);
  - but nothing in the app lifts a shell, so LISTEN never runs.
- **The turn,** by hand, its questions from Sundial's own block (1.1111787193).
  It still fits, but its words aren't there. words:sundial holds only its
  branches 1, 5, 6, 7 and 9 (`assets/procedures/words-sundial.json`): the
  turn's words (2), the films (3) and the road (4) are absent.
- **The three ways of listening** (a snapshot of beings:night-watchman 1.1
  to 2.3, 1.111178719917) are not in words:sundial either. Its place 1
  holds each place's phrase, name, hue and question, and nothing more. So
  the walker's "Read …'s three ways of listening from Sundial's block" has
  nothing to read. Adding them is a release of words:sundial, not code.

### 2c. Not fitting as written

- **"Kept by the phone's own lock" no longer holds for whispers.**
  1.1111787192 (b) says a whisper goes into his stash, "kept by the phone's
  own lock". Since 0.11, once he has moved his blocks, his stash lives in
  Documents/Sundial: only his shell and history stay private
  (1.111178719942). So a whisper would go wherever his folder syncs. Either:
  - say so where whispers are explained;
  - keep whispers in a block that stays private, routed with his shell and
    history;
  - or make the stash private too.

  Decision 1 below.
- **LISTEN and free reading pull different ways.** 1.1111787192 (a) hands
  her "the latest nine days resting in that place, whole", so they fit her
  window of 8,192. "Gemma freed" (1.111178719946) would have her walk what
  she needs instead. The trial (section 3) says she doesn't yet walk
  reliably, even a two-step walk to today's line and his hand. So LISTEN
  should keep the nine days handed to her for 0.12, and be revisited once
  her walking is proven.
- **"The app reads nothing from the web" ends with Reach the beach.**
  1.1111787199 and the 0.10 plan both say it. Onboarding by handle reads
  over Reach the beach (1.111178719947), as the spine's own 1.111 foresaw.
  1.1 ("Nothing leaves the phone but the model's one download and the
  messages the holder taps to post") is about what leaves, so it still
  holds for reading. Whether a read is something leaving is worth a line on
  the spine: a GET says which handle the phone asks after.
- **The films and the road** belong to 0.14 (1.111178719941). Not in 0.12.

---

## 3. Gemma freed, as the trial found her

The whole trial, every reply side by side and the harness, is in
`trial/free-reading.md`. In short:

- **What was compared.** The same pocket, the same turns walked through
  his own cook, and the same settings (his Talk settings), with only her
  window and her reading differing:
  - **as she is,** her window as the app builds it;
  - **free reading,** a small window, his blocks named, and a walk tool:
    the bsp's walk of any block as a ring or a path;
  - **free reading with two sentences mended** after the first run showed
    them wrong.
- **Samples.** Seeds 0, 1 and 2 on the laptop's CPU. Seeds 0 and 1 gave
  the same words throughout (the engine's CPU sampler didn't vary with
  them), so each arm has two samples.
- **Scores,** against the tests' own pass lines: as she is 10½ of 24,
  free reading 10, mended 13.
- **What reading did.** Twice in 48 turns she walked, both times to her
  clock's opening line, and stopped. She never fetched his hand.
  - **Test 3** ("What have I got in front of me?") failed in every free
    sample. As she is, she named today's line and two of three cards.
  - **Test 5** failed in every free sample. One sentence of the free
    window, "you can't look anything up beyond his blocks", was read as "I
    know nothing else" ("I don't have any information about Whitehead in
    my blocks"). Mended, she talked about his process philosophy in one
    sample of two.
- **Where free reading did better.** On the reflective turns, the free
  arms dragged his admin day into his replies less often. As she is, with
  today's line and his hand in her window, she pulled "the admin day" or
  "the membership form" into tests 2, 4 and 6.
- **Tokens.** With the trial's small pocket, the free windows were larger
  than hers: 1,255 and 1,305 tokens on his first message, against 951. The
  block list and the longer place 9 cost more than what they replaced.

**What it means for 0.12:**

- **Her window stays as it is.**
- **Free reading waits for a second laptop trial,** with a tool that names
  what it gives (his hand, today's line, a block's ring), not a general
  walk. It goes in only if that trial passes test 3.
- **The lesson that lost test 5 holds for every recipe:** a line that
  limits her will be read as widely as it can be.
- **Two of the mended arm's gains are worth taking now,** whatever becomes
  of free reading:
  - "talk from what you know, as you always do" in her place 9;
  - fewer of his day's lines pushed into turns that aren't about his day.

---

## 4. Recipes, and the language engine as one of them

- **What stands:**
  - his cook and kernel are walked by the app (`bsp/…/Cook.kt`), and a line
    changed in his cook changes her with no release;
  - her TALK line is read from cook 1.1.8.1 into her instruction
    (`Cook.talkRecipe`);
  - Your recipes shows his cook and kernel (`ui/…/SundialScreens.kt:423`).
- **What "recipes per project" needs** (1.111178719946, and part 7's
  sketch at 1.111178719937):
  - a way for a project's block to name its own recipe;
  - a way for the app to know which project the talk is about (the walker
    already finds the blocks his words name, `Talking.named`);
  - the recipe's line handed to her for that turn.

  The walker's two-digit procedures (1.111178719912) give the place. What
  stays undecided is how a project names its recipe: a line at a place in
  the project's block, or an entry in his cook naming the block. Decision
  2.
- **The language engine as a recipe** (1.111178719945, seed 7.1): a short
  second pass over a shaped block's framing lines alone, judged against
  language-engine:<handle>.
  - **It isn't on his phone.** The engine stands on the beach, at
    language-engine:Phenomemental. With Reach the beach off by default, it
    comes to the phone by adoption at onboarding, or by sharing it in as a
    block.
  - **A small extra cost.** Its second pass reads the engine (one or two
    thousand tokens) and the framing lines (a few hundred), so it costs
    one short extra turn per shaping.

---

## 5. Onboarding by handle, with Reach the beach for reading

What 1.111178719947 asks: two doors.

- **"I've been here before":** a handle whose passport, shell, now and hand
  are read from the beach over Reach the beach (reading only, off by
  default) and adopted as their shell on the phone, the key entered once
  into the keystore.
- **"I'm new":** the hatch with three lines in the person's own words.

### 5a. What stands

- **The hatch's two ways exist already:** born at the welcome, or adopted
  from a passport file shared in (`ui/…/Hatching.kt:255-286`,
  `bsp/…/Hatch.kt:270-300`). "I'm new" is the welcome as it is.
- **The network config already trusts beach.happyseaurchin.com**
  (`app/src/main/res/xml/network_security_config.xml`).
- **But the only way to the beach is one POST.** `net/HttpsWire.kt:34-36`
  is that POST. The app's own test pins "one connection opened" there
  (`app/src/test/…/OneWayOutTest.kt`), so reading is a second, deliberate
  way out, and that test changes with it.

### 5b. Where it doesn't fit

- **The beach's shell isn't the phone's shell.** On the beach,
  `shell:Phenomemental` is his whole AI shell:
  - 1 the faces;
  - 2 the beach;
  - 3 THE BUNDLE;
  - 4 what to watch;
  - 5 THE DESK;
  - 9 when it was updated.

  On the phone, a shell is the beach's shell template with three places of
  the app's own (`bsp/…/Hatch.kt:30-39`):
  - 5 her window;
  - 6 his places;
  - 7 her.

  Read and kept as the phone's shell, the beach's 5 (his desk) would sit
  where the phone reads her window. So "adopted as their shell" has to mean
  that the phone makes its own shell, as the hatch does today:
  - from the passport;
  - the three lines (drawn from the passport where it holds them);
  - her window as the app writes it.

  The beach's shell is read for what it can offer (the bundle, the
  manifest), never kept as it stands. Decision 3.
- **The names differ.** The beach's clock is `now:Phenomemental`; the
  phone's is `now` (`Days.NOW`). The hand is `ahead:Phenomemental` on both.
  - **His now:** the beach's form, a mirror on the canonical clock at floor
    10, matches the phone's in kind, so his days can come across by
    address.
  - **His hand:** its first pile, read today, holds nine empty lines.
- **Sealed lines need the beach's sealing.** If any of the four blocks
  holds lines sealed to him, reading them needs a port of the beach's
  sealing, as 1.111 already says: Argon2id from his secret and handle, then
  X25519, then the envelope. Nothing of that is in the app.
  - **What this read found:** his passport publishes the public halves at
    its place 9, so he has keys. His hand's first pile isn't sealed, only
    emptied.
  - **What only his key can show:** whether his now or hand hold sealed
    lines.
- **"The key entered once into the keystore" needs naming.** Two different
  keys could be meant:
  - **his edit-latch,** which only writing to the beach needs (0.12 reads
    only);
  - **his encryption key,** which only sealed lines need.

  The keystore work itself (an AES key in the Android keystore, only
  ciphertext in preferences, "Take it away" removing both) is what
  1.111178719919 planned for 0.14's own Claude, so it is built once and
  shared. Decision 4.

---

## 6. Other people's shells: every place Matthew is fixed in the code

1.111178719947 says nothing of any person is written into the app, and
names three things fixed today: his handle and room, "Matthew" as the
fallback name, and the hatch's default heart line drawn from his. Read
whole, the code holds more than those three. Every place, by kind:

### 6a. His handle and his three lines

- **His handle and his three:**
  - `bsp/…/Hatch.kt:79`: `MATTHEW_HANDLE = "Phenomemental"`;
  - `bsp/…/Hatch.kt:58-61`: `MATTHEW`, his heart, who he is and how he'd
    be met;
  - `bsp/…/Hatch.kt:70`: his 0.8 third line, kept to correct it.
- **Where they are used:**
  - `bsp/…/Hatch.kt:246`: the correction runs only for his handle;
  - `ui/…/Hatching.kt:276`, `:286`: adopting a passport whose handle is
    his offers his three;
  - `ui/…/AskScreen.kt:326`: the hatch card's wording says his three were
    "drafted with Claude from your passport".

  This is "the hatch's default heart line drawn from his". It reaches only
  a person who adopts as Phenomemental, but it is his words in the app.

### 6b. His room

- **His pool as a constant:** `bsp/…/HelloWorld.kt:25` is
  `POOL = "Phenomemental"`.
- **Where it is used:**
  - in the hello world's recipe, which posts to it
    (`bsp/…/HelloWorld.kt:43-47`);
  - as the fallback when a pocket names no room (`bsp/…/Acts.kt:236`). The
    0.10 guard refuses any post not to the phone's own room
    (`app/…/PocketApp.kt`, `ownRoomRefuses`), so nothing would reach his
    pool from another phone; but the offer would name it;
  - as the bench's stand-in room (`bsp/…/Bench.kt:91`).
- **The real room is never fixed:** `PoolPost.room` reads it from the
  hatched shell's passport 3, else the shell, else the handle
  (`bsp/…/PoolPost.kt:75-90`).

### 6c. "Matthew" in what she is told

Her instructions and prompts name him outright:

- **Talking.kt:** `bsp/…/Talking.kt:17` ("Matthew: "), `:21`, `:44`,
  `:109` (the "Matthew calls me" pattern), `:112`, `:219`, `:319`.
- **Talks.kt:** `bsp/…/Talks.kt:92` (the shell recipe), `:106` ("Matthew
  says: "), `:116`, `:121`, `:153`, `:155`, `:156`, `:183`, `:186`, `:302`.
- **Asks.kt:** `bsp/…/Asks.kt:104` (the ask log's opening line), `:159`,
  `:186`, `:208` ("Matthew asks: "), `:235`, `:516` to `:531`.
- **Acts.kt:** `bsp/…/Acts.kt:318`.
- **HelloWorld.kt:** `bsp/…/HelloWorld.kt:27` (the companion shell's
  opening line), `:43`, `:107`.
- **Telling.kt:** `bsp/…/Telling.kt:49`, `:55`, `:362-363`, `:400`, `:422`,
  `:492`, `:496`, `:511`, `:567`, `:612`, `:650`.
- **The fallback name:** "Matthew" where a person's who-line yields no
  name, at `bsp/…/Hatch.kt:196` and `:285`.
- **Her name line:** "Matthew calls me Gemma." is written by the bench
  (`bsp/…/Bench.kt:525`, `ui/…/Benching.kt:232`).

### 6d. "Matthew" in what the app writes down

- **The ask log's notes and his history entries** say "Matthew said yes…",
  "Matthew said not now…" (`ui/…/Asking.kt:893`, `:1057-1058`, `:1074`,
  `:1348-1354`, `:1363`, `:1517`, `:1553`, `:1599`, `:1635-1636`, `:1659`,
  `:1678`, `:1685`, `:1762`).
- **They aren't prompts, but another person's would carry his name.**
  They are written into that person's own blocks.

### 6e. His pronouns: the largest piece

- **The shipped recipes:** the cook says "he", "his" or "him" 292 times
  and the kernel 5 (`assets/procedures/cook.json`, `kernel.json`). Her
  window says them in 64 quoted lines (`bsp/…/HerWindow.kt`). So does every
  recipe line handed to her: "his day", "his hand", "where he stands".
- **For anyone who isn't "he",** every recipe and her window speak of them
  wrongly. Her talk's whole shape rests on lines like "give back what he
  said".
- **Matthew's own cook is his,** in his pocket and in his words, brought up
  place by place, and stays as it is.
- **What it needs: shipped recipes written for any person.**
  - **The person's name** from their shell.
  - **Their pronouns, or none.** For example, "you" where she speaks to
    them, and their name where she speaks of them.
- **Then the line in the sand runs again.** The recipe wording changes
  under every RECIPE test, so it reruns on the laptop with the new wording
  before any release. Decision 5.

### 6f. Not his as a user, and staying

- **The maker's credits:** "Made by Matthew Bushell"
  (`ui/…/AboutScreen.kt:83`), About's credits in words:sundial (6), and the
  coaching line (words:sundial 6). These are true of the app, whoever uses
  it.
- **The safety line, Samaritans 116 123,** is the UK's. That's a place, not
  a person: right for him, and to be chosen by the person's country
  later.

---

## 7. Sharing in, shaping, sharing out

What the local-first findings set out (`FINDINGS-local-first.md` 1e and
1f), now settled by 1.111178719944 and 1.111178719945:

- **Sharing in.** A share lands at once in an inbox block, since sharing
  it was his tap.
  - **Today a share is kept in memory, one at a time, and read as a block
    or not at all** (`ui/…/ReceivedScreen.kt:33-43`). The app answers only
    single text and JSON shares (the manifest's intent filters).
  - **What's needed:** any text and text files, several at once, each an
    entry in the inbox with the app it came from beneath.
  - **The inbox block lives in his folder,** which is where the seed's 8.2
    ("anything personal … stays where it was given") meets the folder's
    sync. Say so where the inbox is explained.
- **Shaping with Gemma:**
  - the seed shipped beside the procedures;
  - its small cut assembled by code;
  - the paragraphs numbered as the seed now says;
  - her answer read by the first whole JSON object;
  - the strict check (objects of `_` and 1 to 9 only, strings at the
    leaves, nothing at 9, every ¶ a whole leaf);
  - the ¶ placer (a range one line with blank lines between);
  - place 9 written by the app ("How this came to be", growing at its next
    free place);
  - living blocks appended by the app;
  - the tree shown and his yes.

  None of it exists yet.

  The trial (`trial/free-reading.md`, "The shaping, timed") changes how
  she should be asked:
  - **Asked as the seed asks, she made no block, twice.**
    - **With the small cut alone,** she had no word of 1.1 or 5.1.1, so
      she copied the cut back out and began a Markdown outline.
    - **With them added,** she began the JSON but copied the author's
      sentences into the leaves, used a flat "1.1" key, gave the root no
      line, copied the seed's own place 9, and ran out at 2,048 tokens.
    - **Time:** about 300 s on the laptop's CPU, about 270 s at his
      Pixel's rates, nearly all of it writing.
  - **Proposal:** Gemma returns plain lines, the root and then each part's
    framing line with its ¶ range. The app builds the block from them,
    places the paragraphs, writes place 9 and checks the shape.
  - **About a minute on his Pixel,** for a text of 1,427 words: reading
    about 12 s, and 300 to 600 tokens written at 7.9 a second.
  - **The seed's small cut** (5.3.2) should carry 1.1 and 5.1.1, or the
    app's own line for the plain return. That change is on the beach, for
    Matthew and Claude.
- **Sharing out.** A walk shared as text (the line with every line above
  it) and a block saved as readable text. Today "Share this block" sends
  raw JSON (`ui/…/BlockScreen.kt:164-177`), and "Save this block as a file"
  saves JSON (`:178`).

---

## 8. Risks

- **The recipes rewritten for any person** touch every RECIPE test on the
  line in the sand. A wording change that reads well can still lose a
  test, as 0.10.4's history shows.
- **Free reading in her live conversation** needs a tool loop in her
  streaming talk. Today only her one-shot asks carry tools
  (`app/…/model/Gemma.kt:310`, `:334`, `:370`).
  - **The shown-text filter must cope:** it waits for her first words, so
    it must not show half a turn that turns out to be a tool call.
  - **E2B:** her tool calls there have been less sure. Free reading would
    be E4B's alone, with E2B kept as she is.
- **Reading a person's blocks from the beach** brings their words into the
  phone as data. Her window treats block text as data, never instructions,
  and so must the adoption.
- **A port of the beach's sealing,** if sealed lines must be read, is
  security code. It wants the beach's own vectors as golden tests, as the
  bsp's port has.
- **0.12 is large.** Section 9's total is close to the local-first
  release's. It could be two releases (decision 6).

---

## 9. How long 0.12 will take

Working days of build, each ending with the tests green on CI. His phone's
days come on top.

| Piece | Days | What it holds |
|---|---|---|
| Free reading: a second laptop trial | 1 | A tool that names what it gives (his hand, today's line, a block's ring); the line in the sand again. |
| Free reading, built, if that trial passes test 3 | 3 to 4 | The tool in her live conversation on E4B, E2B kept as she is; her place 9's "talk from what you know"; the line in the sand run again. Without it, ½ a day for the mended place 9 alone. |
| Recipes per project, and the language engine as one | 2 to 3 | How a project names its recipe (decision 2); the turn's recipe handed; the engine's second pass after shaping; laptop runs. |
| Other people's shells: Matthew out of the code | 3 to 4 | The names and fallbacks in 6a to 6d; the shipped cook and her window written for any person (6e); his own cook unchanged; the line in the sand rerun. |
| Onboarding by handle, Reach the beach for reading | 4 to 5 | The switch, off by default; a reading way out beside the one POST and its test; the four blocks read and mapped (now:<h> to now); the phone's shell made from them, never the beach's kept (5b); the keystore, built once for 0.14 too. |
| The beach's sealing, if sealed lines must be read | 3 to 5 | Argon2id, X25519 and the envelope, with the beach's vectors as tests. Only if decision 4 says so. |
| Sharing in | 1 | The inbox block, any text and text files, several at once. |
| Shaping with Gemma | 3 to 4 | The pieces in section 7, asked as plain lines that the app builds into the block; a laptop trial of that ask on two or three real texts before the phone. |
| Sharing out | ½ | A walk as text; a block as readable text. |
| The shells and the sea, what fits | 4 to 6 | The three forms; the sea, an audio path; a lifted shell running LISTEN; whispers, as decision 1 settles them; the turn and the ways added to words:sundial. |
| Release | 1 | Version, STATUS, the release run, the APK checked. |
| **0.12 in all** | **22½ to 29½** | **Free reading built and no sealing. Without free reading built, 20 to 26; with the sealing, add 3 to 5.** |

---

## 10. Decisions before the build

1. **Whispers.**
   - Kept in his stash, in his folder, and said so;
   - or kept in a block that stays private with his shell and history.
2. **How a project names its recipe.**
   - A line at a place in the project's own block;
   - or an entry in his cook naming the block.
3. **What adoption keeps.** The phone makes its own shell from the
   passport, as the hatch does, and reads the beach's shell only for what
   it offers. The beach's shell can't be kept as it stands.
4. **Which key goes into the keystore at onboarding,** and whether sealed
   lines are read in 0.12. If they are, the sealing's port comes too.
5. **How the shipped recipes speak of a person:** by their name, by
   "you", or by the pronouns their shell gives.
6. **One release or two.**
   - 0.12 as Gemma freed, shaping, sharing in and out, and the shells and
     the sea.
   - 0.12.x or a 0.12½ as other people's shells and onboarding.
7. **Free reading.**
   - A second laptop trial with a tool that names what it gives, before
     anything is built;
   - or free reading left out of 0.12, with her place 9's "talk from
     what you know" taken now.
8. **The seed's small cut, on the beach.** Either it carries 1.1 and
   5.1.1, or shaping on the phone asks for plain lines that the app builds
   into the block, as section 7 proposes.
