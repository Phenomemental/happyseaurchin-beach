# Sundial 0.12: the practices out of the code, and one way to run a skill

21 November 2026, for spine:pocket-beach 1.11117871995, and above all its
parts 1 and 2 as revised at 19:38:

- 1.111178719951, the releases: 0.12 is "the mechanics Gemma's practices
  stand on … and one general way to run a skill over a chosen set of
  blocks and show what it returns, nothing written without his yes".
- 1.111178719952: "THE CODE HOLDS THE MECHANICS; THE BLOCKS HOLD THE
  PRACTICES."

Read beside them: the decisions at 1.11117871994, the plan at
1.11117871993, seed:sundial-shaping whole (as it stands after its 9.5),
`FINDINGS-0.12.md` and `trial/free-reading.md`. For section 7, decision
1.1111787199531 and the beach's teaching blocks shell-genome and
soft-agent. For section 8, the decisions at 1.11117871996. Where they
differ, the latest decides. Nothing was built in the app and nothing was
written to the beach.

Paths below are from `pocket-beach/`: `bsp/…` is
`bsp/src/main/kotlin/com/phenomemental/pocketbeach/bsp/`, `ui/…` is
`app/src/main/kotlin/com/phenomemental/pocketbeach/ui/`, and
`assets/procedures/` holds the blocks Sundial ships.

## The short version

- **The six places are written down four times, and the four don't
  agree.**
  - The code's own list, `Mark.POOLS`, is the one everything reads.
  - Sundial's shipped block words:sundial holds a copy at its place 1,
    which nothing reads.
  - His cook's placing ring holds a third, in other words ("what he
    felt", where Mark says "what you felt").
  - spine:p6, shipped beside them, holds the practice's own words and
    questions, and nothing reads it either.
  - About sixty lines in eleven files read the six from code, and the
    counts, the dial's centre and its geometry assume there are six.
- **The lens block can be one block that the dial, the pools, the placing,
  listening back and the reflection all read.** Its places are listed in
  the order they sit round the dial. Each carries its phrase, name,
  colour, an empty place's question, what it holds and its questions. A
  second branch says how a place is named. His cook names the lens he
  uses, or none.
- **Listening back, whispers and the turn are recipes or plans, not
  running code.**
  - LISTEN is shipped in his cook (6.4), but nothing in the app starts it.
  - Its whisper steps write to his stash through code that the decision
    now retires.
  - The turn and the three ways of listening were never built.
- **Running a skill over a chosen set of blocks fits what exists.**
  - The walker already reads blocks, hands her a line with what it read,
    sets out cards and waits for his yes.
  - The Ask pipeline's proposal already plans every write on a copy, and
    the 0.11 guard holds it if a block changes.
  - What's new: choosing the set, reading it within her window, taking a
    skill's cut, reading her plain lines back, one card that holds several
    writes, and a kernel event.
  - Shaping, listening back and the P6 reflection are then three skills
    on the same run.
- **The P6 reflection fits her window easily,** because the months are
  counted by the app, never read by her.
  - She is handed the spread the dial already counts, the quieter places'
    questions and his latest nine days: about 1,700 to 2,600 tokens, at
    most about 4,200, in her 8,192.
  - She returns three plain lines: about 15 to 30 seconds on his Pixel.
- **Wearing his shell as the beach's teaching has it, she did worse than
  her window as Sundial builds it: 3 of 24 against 10½** (section 7).
  - She knew his world from his passport, but talked to him like a beach
    agent (pools, grains, marks, in 9 of 24 replies).
  - She read his day as something else, and never answered from what she
    knows.
  - The cause is mostly soft-agent's beach role and the missing per-turn
    recipes, not the friend line. The next trial (decision 18) tests the
    decision itself.
- **Her recipes as skills, with nothing of his shell standing, did best
  of all: 17 of 24 against 10½ for her as she is** (section 8).
  - Her first word in a new conversation comes in about 1½ s on his Pixel,
    against about 3 s.
  - Her friend line and his three lines were doing nothing for her. So
    they come out in 0.12, by the plan in 8f: 3½ to 4½ days.
- **0.12, with the lens and the skill run added, is 20 to 24 working
  days,** before free reading is built. Free reading adds 3 to 4 if its
  second laptop trial passes.
- **Twenty-three decisions,** each with a proposed answer: seventeen in
  section 6, three at the end of section 7, three at the end of section
  8.

---

## 1. Practices now fixed in code

For each practice: where it lives now, what it would take to read it from
a block, and what stays code. The last part of this section is a table of
them all.

### 1a. The six places: their phrases, names, colours and questions

**Where they live now.** Four sources, of which the code reads one:

| Source | Its phrases | Read by |
|---|---|---|
| `Mark.POOLS`, `bsp/…/Mark.kt:28-35`: number, name, colour, phrase, P6 address, question; the dial's order at `:38` (1, 6, 3, 5, 4, 2) | "what happened", "what you felt", "what you made of it", "what matters", "a choice", "what’s ahead" | everything below |
| words:sundial place 1 (`assets/procedures/words-sundial.json`), copied from mark.js for 0.10 | the same, with name at 1, colour at 2, question at 3 | nothing: `Words.sundial` reads only place 5 (`ui/…/Words.kt:24-31`), and `sundialBlock` places 6 and 7 |
| His cook's PLACE ring, cook 6.2.1.1 to 6.2.1.6 (`assets/procedures/cook.json`) | "what happened", **"what he felt", "what he made of it"**, "what matters", "a choice", "what's ahead" | the walker, as the ring she chooses from |
| spine:p6 (`assets/procedures/p6.json`): PRESENCE, THE SURFACE (1.1 to 1.3), THE DEEP (2.1 to 2.3), each portal's description and two questions | none: its names are FACTS, FEELINGS and so on | nothing: `Cook.P6` (`bsp/…/Cook.kt:25`) is named and never used |

On top of the four, the code keeps its own translations:

- `HerReply.placesOf` keeps the cook ring's words as a fixed list
  (`bsp/…/HerReply.kt:90-99`), so her answer is read whether she writes
  the cook's phrase, Mark's phrase or the name. **If he edits his ring
  in Your recipes,** say "what he felt" into "what moved him", her answers
  stop being read and his moments go unplaced. His ring looks like his to
  change, but the code pins its words.
- `Days.PORTALS` and `Days.PORTAL_NAMES` (`bsp/…/Days.kt:35-36`),
  `Keep.PORTAL_POOL` (`bsp/…/Keep.kt:245`) and `Days.emptyP6`
  (`bsp/…/Days.kt:349`): the P6 addresses, for the pieces Rockpool filed
  in his p6 before Sundial.

**Where the code reads them,** about sixty lines in eleven files, besides
Mark's own:

- **The places line at the foot of a day** ("What happened · what you
  felt."), written and read by phrase: `Mark.write`, `Mark.read`,
  `Mark.placesIn`, `Mark.placesWords`, `Mark.placesSaid`
  (`bsp/…/Mark.kt:60-140`), and the keep card's day text
  (`bsp/…/Days.kt:87`, `:104-109`, `:137-138`).
- **The counts and the dial's centre:**
  - `Mark.counts` is `IntArray(6)` (`bsp/…/Mark.kt:145`);
  - `Mark.evenness` takes its mean as 1/6 and its most as 5/36
    (`:156-170`);
  - `Keep.counts` (`bsp/…/Keep.kt:322`) and the dial's state
    (`ui/…/Dial.kt:78-79`).
- **The dial's drawing:**
  - the tide-pools at 60° apart, in the dial's order (`bsp/…/Mark.kt:328-333`);
  - six stretches of colour along the heart's line, each 0.16 of its
    length (`ui/…/Dial.kt:214-219`);
  - the captions on hold, from the phrases (`ui/…/Dial.kt:148`);
  - each day's dot in its places' colours (`ui/…/Dial.kt:306`, `:324`).
- **Your days and the pools:**
  - the day's page and its coil of colour (`ui/…/SundialScreens.kt:195-196`,
    `:228`, `:253-265`, and `Mark.coilBands`, `bsp/…/Mark.kt:383`);
  - the pools page, in the dial's order (`ui/…/SundialScreens.kt:273-294`);
  - the pooled view on Today (`ui/…/Today.kt:111-122`).
- **The keep card:** its row of colours to tap on or off
  (`ui/…/SundialScreens.kt:485`, `:518-525`), and the moments waiting to
  be kept, stored as pool numbers 1 to 6 (`bsp/…/Waiting.kt:51-52`, `:65`).
- **Her placing and her reply:**
  - the ring's choices taken as pool numbers (`bsp/…/Cook.kt:1058`,
    `:1095`, `:1101-1106`);
  - a phrase turned into a pool number (`bsp/…/Cook.kt:645`, `:902`);
  - "all six named counts as none" (`bsp/…/HerReply.kt:83`, `:165`);
  - the places she named, in the workings (`ui/…/Asking.kt:745`);
  - the free-text placing read back (`bsp/…/Placing.kt:49`).
- **Listening and whispers:** `bsp/…/Cook.kt:909`, `:914`, `:918`,
  `:1171`, `:1732`, `:1746` (see 1c and 1e).

**What it would take to read them from a block:**

- **A `Lens` in the core,** read from the lens block (section 2): its
  places in order, each with phrase, name, colour, question, what it holds
  and its questions. Checked when read; a lens that fails the check gives
  way to the shipped P6 lens, and the workings say why, as his cook does
  now (`Cook.load`, `bsp/…/Cook.kt:1440`).
- **Mark's functions take the lens** in place of `POOLS` and `P6`. Since
  the lens lists its places in the dial's order, `inOrder` becomes a sort
  by place number and `P6` goes.
- **The counts and evenness for any number of places, one to nine:**
  the mean 1/n, the most (n−1)/n², the pools at 360/n degrees, the
  stretches 0.96/n of the line, and the captions placed for n
  (`DialLabels` is tested for six only).
- **The app reads the lens once** and again when it changes, as it reads
  his cook, and hands it to every screen and to the walker's world.
- **The cook's PLACE ring gives way** to "the places of his lens" (a new
  form of the step), so the ring is written once. `HerReply.placesOf`
  reads the lens's phrases and names, and its fixed list goes.
- **The old P6 pieces:** `Keep.PORTAL_POOL` maps a P6 address to the place
  whose phrase P6 gave it ("1.1" to "what happened"), so they count in any
  lens that has that place, and in no other.
- **Waiting moments:** stored by place number, read once against the
  lens's order. They hold only the day not yet kept, so no long migration.
- **Words:sundial place 1 goes,** since the lens holds it.

**What stays code:** the drawing for n places; the counts, shares and
evenness; the places line's form (" · " between, a capital and a full
stop, as mark.js writes it); the check of a lens and the fall back to
P6; the mapping of Rockpool's pieces.

**About 4 to 5 days.** Every Mark test assumes six, and nine test files
touch the places.

### 1b. The pools

**Where they live now.**

- A place's pool is every day whose last line names it:
  `Keep.restingIn` (`bsp/…/Keep.kt:329-338`), with each day's text cut at
  1,500 characters.
- Today shows the latest nine, three of them in 240 characters each
  (`ui/…/Today.kt:111-122`); the pools page shows up to 81
  (`ui/…/SundialScreens.kt:273-294`).
- The empty place asks its question (`Mark.POOLS[i].question`); the
  lines around it are words:sundial 5.4.1 and 5.4.2.

**From a block:** only the places, from the lens: phrase, colour,
question. The rule (a day rests where its last line names it) is the
lens's mechanics and stays code, as 1.1111787192 has it ("the lens is
applied each time he reads").

**Stays code:** `restingIn`, its numbers, the pages. **Half a day** once
the lens is in (inside 1a's estimate).

### 1c. Listening back

**Where it lives now.** Mostly in blocks already, but never run:

- **The recipe is shipped:** cook 6.4 LISTEN, nine steps, and kernel 7,
  "A SHELL LIFTED TO HIS EAR. Run cook 6.4".
- **The walker reads its steps:**
  - DAYS (`bsp/…/Cook.kt:901-906`): the latest nine days resting in a
    place, found by `Mark.poolByPhrase`;
  - WHISPERS and WAYS (`:908-917`): ask the world for
    `whispers(pool)` and `ways(pool)`, which default to nothing
    (`:534-537`); the app's world (`ui/…/Asking.kt:306-320`) gives
    neither;
  - the line handed to her (cook 6.4.5) and her four parts (6.4.6) are
    in his cook.
- **Nothing starts it.** No screen lifts a shell, so kernel 7 never fires.
- **The three ways of listening** (beings:night-watchman 1.1 to 2.3) were
  to be in words:sundial; they are in no block on the phone.

**As a skill:** listening back becomes a skill run (section 3) over one
set: the days resting in one place of his lens.

- The set is chosen by tapping a place on the pools page, then "Listen
  back here".
- The skill block holds the line she is handed, the ways of listening in
  each place, and what she returns: what she heard, what she noticed, one
  question.
- What he says back is kept only on his yes, through the same card as any
  other skill, not as a whisper.
- Cook 6.4 becomes that run, and kernel 7 its event.

**Stays code:** reading the days resting in the place, the call, showing
her reply, his yes. **1 to 1½ days** once the skill run exists.

### 1d. The turn

**Where it lives now:** nowhere.

- 1.1111787193 has it by hand, never hers: "the turn (by hand, as in
  Sundial, its questions from Sundial's own block)", its question standing
  in while she loads.
- Words:sundial has no place for it (it holds 1, 5, 6, 7 and 9).
- Today's opening question while she loads is words:sundial 5.1.2,
  "What’s with you {part}?", with the part of the day filled by code.

**From a block:** the turn's questions are the lens's: each place's
questions (the lens's 5 under each place, section 2). When the turn is
built, turning the dial to a place reads that place's questions. Its few
words of its own ("turn to a place") go in words:sundial.

**Stays code:** the gesture and the drawing. **Not in 0.12** (decision
15); the lens carries its questions, so nothing is lost by waiting.

### 1e. Whispers: kept as code, now not to be built

1.111178719952: "Whispers was a name for one mechanism inside the earlier
design and is not built." What stands of it:

- **The walker's verbs:** `Whispers`, `Ways` and `SetWhisper`
  (`bsp/…/Cook.kt:93-95`), their sentence forms (`:239-241`), and their
  steps (`:908-918`).
- **The world's readers,** `whispers(pool)` and `ways(pool)`
  (`bsp/…/Cook.kt:534-537`), which return nothing.
- **The card,** `Card.Whisper` (`bsp/…/Cook.kt:551`), shown at `:1171`.
- **The write:** `CookActs.whisper` and its carry-out
  (`bsp/…/Cook.kt:1730-1750`). It writes "whisper · phrase · date", his
  words and the question into his stash, with a history entry. Since 0.11
  that stash may be in his folder.
- **In the shipped cook:** 6.4.3 (WHISPERS), 6.4.7 (set out a whisper) and
  6.4.8 (on his yes, write it).

**What it would take to retire:**

- the shipped cook's 6.4 rewritten as listening back's run (1c);
- the three forms still read for one release, doing nothing and saying so
  in the workings, as the 0.9 form of the day step was (`Verb.SetDay`,
  `bsp/…/Cook.kt:76`), since his own cook may carry the old 6.4;
- then the verbs, the card, the world's readers and `CookActs.whisper`
  removed, with their tests.

**Half a day,** inside listening back's estimate.

### 1f. Her placing: the words she's told

**Where they live now.** Partly in his cook, partly in code:

- **The cook:** 6.2.1's sentence, "a place counts only where his words are
  plainly in it".
- **The code:**
  - `Placing.instruction` (`bsp/…/Placing.kt:21-25`): "name the places of
    Sundial's ring his words plainly touch, never ranked", "these are the
    only names you may write", "Never name things from his words (people,
    tasks, where he went)", and the form of her answer;
  - the ring as she's asked it on E4B (`ui/…/Asking.kt:363-364`): "Name
    every place of this ring his words plainly touch, one or more, never
    ranked";
  - "all six named counts as none" (`bsp/…/HerReply.kt:83`, `:165`;
    `bsp/…/Cook.kt:1095`).

**From a block:** how a place is named belongs to the lens (its branch 2,
section 2), since another lens may draw its places differently. The form
of her answer ("one line for each moment: its number, a colon, then its
places by those names") stays code, because `Placing.read` parses it.

**One care:** "these are the only names you may write" is tested wording.
On the laptop, without it she wrote topics and placed none of his seven
moments; with it she placed all seven (`bsp/…/Placing.kt:15-19`). A lens
that drops it may undo that. So the shipped P6 lens keeps it word for
word, and the laptop trial is run again on any lens Sundial ships.

**Stays code:** the answer's form and its reading; "every place named is
none", as a guard on her reliability, not a practice.

### 1g. Other practices found

| Practice | Where now | From a block | Stays code |
|---|---|---|---|
| **The quiet line** "In your own words." on a day that names no place | `Mark.OWN`, `bsp/…/Mark.kt:41` | words:sundial 5.2, beside the keep card's words | where it goes |
| **The dial's centre grows as his days rest evenly** | `Mark.evenness`, `bsp/…/Mark.kt:163-170`; `ui/…/Dial.kt:79` | nothing: the drawing; under no lens the centre keeps its smallest size | all |
| **The hand's seven piles**, "within hours" to "this decade" | `Days.PILES`, `bsp/…/Days.kt:34`, written into a new ahead block (`:345-347`) | the beach's function:ahead, when onboarding brings blocks from the beach | belongs to 0.13, not 0.12 |
| **His safety words** | `Talking.SAFETY_BLOCK`, `bsp/…/Talking.kt:126-172` | already done the right way: his block where he keeps one, else the shipped words | the check itself: the seed's 8 and 7.2 have safety judged by plain, exact rules |
| **The shaping's small cut** | not built; `FINDINGS-0.12.md` section 7 had it "assembled by code" | his cook's shaping step names the seed and its cut (section 3), so the cut changes with the seed, never with a release | reading the named places |
| **The language engine's second pass** | not built | a step of the shaping skill naming his engine's block (seed 7.1) | the call |
| **Her TALK line, the day check, the nine days of listening** | his cook (`Cook.talkRecipe`; `Landing.DayCheck` read from its sentence; DAYS's "nine" read as a number) | already in blocks | as now |

### 1h. All of them together

| Practice | Lives now | To read it from a block | Stays code | Days |
|---|---|---|---|---|
| The six places | `Mark.POOLS`, copies in words:sundial 1 and cook 6.2.1, `HerReply.placesOf`'s list | the lens block, read once and handed round | drawing for n, counts, evenness, places line, check and fallback | 4 to 5 |
| The pools | `Keep.restingIn`, pools page, Today | places from the lens | all the rest | inside the above |
| Listening back | cook 6.4, kernel 7, verbs; never started | a skill run over a place's days; its own skill block | reading the days, the call, the card | 1 to 1½ |
| The turn | not built | the lens's questions | the gesture | not in 0.12 |
| Whispers | verbs, card and write in `Cook.kt`; cook 6.4.3, 6.4.7, 6.4.8 | retired | old forms read for one release | inside listening back |
| Her placing's words | `Placing.instruction`, `Asking.kt:364`, cook 6.2.1 | the lens's branch 2 | the answer's form | inside the lens |
| Quiet line | `Mark.OWN` | words:sundial 5.2 | where it goes | inside the lens |

---

## 2. The lens block

### 2a. Its shape

One block a lens is written in, here as P6's (lens:p6), shown as a tree.
Each line is the underscore at its place; the place's digit stands before
it:

```
_  P6, THE SIX PORTALS — the lens Sundial reads his days through, from
   Presence in Action (Louie Gardiner's practice): six ways of looking at
   a day, offered lightly, never labels.
1  THE PLACES, in the order they sit round the dial, clockwise from the
   top: each its phrase, then its name at 1, its colour at 2, the question
   an empty place asks at 3, what the place holds at 4, and its own
   questions at 5.
   1  what happened
      1  Facts
      2  #4A6B8A
      3  What could you have pointed to today?
      4  The data: what could be checked, seen, heard, touched, said or
         done, before any meaning is added.
      5  Its questions, for reflecting and for the turn.
         1  What is here that you could point to: what you see, hear, or
            were told?
         2  Leaving out what it means, what actually happened?
   2  what’s ahead          (Outcomes, #6FA8DC; from spine:p6 2.3)
   3  what you made of it   (Fictions, #7A5C99; spine:p6 1.3)
   4  a choice              (Decision, #B8860B; spine:p6 2.2)
   5  what matters          (Purpose, #2E7D6B; spine:p6 2.1)
   6  what you felt         (Feelings, #B0413E; spine:p6 1.2)
2  HOW A PLACE IS NAMED: a place counts only where his words are plainly
   in it, one or more, never ranked; these are the only names that may be
   written.
   1  Never name things from his words (people, tasks, where he went).
9  WHERE IT CAME FROM: the phrases, names, colours and empty-place
   questions from mark.js; what each place holds and its questions from
   spine:p6; made for Sundial 0.12.
```

The rules a lens keeps, checked by code when it is read:

- **At least one place and at most nine,** each with a phrase at its
  underscore and a colour at 2, written `#RRGGBB`. A name, a question,
  what it holds and its questions may each be left out; the app then
  shows the phrase where a name is wanted, and asks no question.
- **No two places share a phrase or a name.**
- **The order of the places is the order round the dial,** so the place's
  digit is its number everywhere: in the counts, in the waiting moments,
  in the ring she chooses from. For P6 this puts the places in the order
  the app already shows them (the dial's order, `Mark.P6`), not mark.js's
  order (decision 2).
- **Its words are copied in, not pointed to:** what each place holds and
  its questions are copied from spine:p6, with where they came from at 9.
  The phone reads nothing from the beach, and words:sundial is a copy in
  just the same way (seed 9.2: "The beach is the source and every file is
  a copy").
- **No machinery in it:** no verbs, no step forms, no numbers the app
  computes with. That keeps strata 2.12's line between law and content:
  his cook points to the lens; the lens carries words.

### 2b. Who reads what

| Reader | What it reads | Today from |
|---|---|---|
| The dial | how many places; each colour; the phrases for the captions | `Mark.POOLS`, `Mark.P6` |
| The pools and Today's pooled view | phrases, names, colours; the empty place's question | `Mark.POOLS` |
| A day's places line, written and read | phrases | `Mark.POOLS` |
| The keep card's row of colours | colours, phrases | `Mark.POOLS` |
| Her placing | the names and phrases she may write; how a place is named (2) | cook 6.2.1's ring, `Placing.instruction`, `HerReply.placesOf` |
| Listening back | the place's phrase, what it holds (4), its questions (5) | cook 6.4.5's line; WAYS, unbuilt |
| The P6 reflection | every place's phrase; the quieter places' 4 and 5 | (new) |
| The turn, when built | a place's questions (5) | (unbuilt) |

### 2c. One lens among others, or none

- **His cook names the lens he uses,** at a place of its own (decision
  1): "THE LENS: lens:p6", or "THE LENS: none". Settings shows it as one
  line, "Your lens: P6", and a change is set out for his yes and written
  through the guard, like any change to his cook.
- **Where the named lens is read from:** his pocket first, then the
  shelf. Sundial ships lens:p6; a lens he makes or shares in is a block
  in his pocket. Tuning, growing or swapping a lens then needs no release,
  as 1.111178719952 asks.
- **None:**
  - the dial is drawn in ink with no colour inside the line and the centre
    at its smallest;
  - holding it shows his days, with no pools;
  - the keep card has no row of colours, and the placing isn't asked,
    which saves her the placing call (10 s on his Pixel on 4 November);
  - no places line is written; skills that need places are not offered.
- **Days kept under one lens, read under another:**
  - a day keeps the phrases it was kept with, and switching never
    rewrites a day;
  - a lens counts only the phrases it knows, so P6's days rest nowhere
    under another lens, and come back when he returns to P6;
  - a places line no lens knows shows as a line of the day. Under none,
    lines in a shipped lens's phrases are still kept out of sight
    (decision 6).
- **A lens that fails the check** gives way to the shipped P6, said in
  the workings and on Settings, as a cook that can't be read gives way
  now.

### 2d. What changes in code to read it

All in 1a: a `Lens` class and its check; Mark's functions given the lens;
the drawing for n places; the cook's ring replaced by "the places of his
lens"; `HerReply.placesOf` and `Placing` given the lens; the app reading
it once and handing it round; Settings' line; "none" through the dial,
the keep card and the pools. **4 to 5 days.**

---

## 3. Running a skill over a chosen set of blocks

### 3a. What a skill is

- **A skill is a block of words,** written as the seed is written: in the
  instructing relation, its root saying what it does, its branches what
  she is given, what she does, what she returns, and the rules that
  travel with it. seed:sundial-shaping is the first one already: 5.1.1
  says what a small mind returns, 5.3.2 what part of it she takes, 8 the
  rules.
- **One procedure in his cook runs any skill.** His cook holds the law:
  which blocks are read, what she's handed, what she returns, what is set
  out for his yes. The skill holds only the words. This is decision
  1.111178719954's pattern ("the law points to the content and the content
  carries no machinery") applied to every skill.
- **His cook names his skills,** one entry each: the skill's block, what
  it reads, and the part of it a small mind takes. For example: "Reflect
  through his lens: skill:reflect, over his kept days; she takes its
  opening lines and 3." Sundial ships the entries for the skills it ships;
  he adds his own.

### 3b. The run, step by step

1. **He chooses the set:**
   - his kept days, all of them or a span (from Your days);
   - the days resting in one place (from the pools);
   - one block or several, or a project's blocks (from Your pocket);
   - what he shared in (from the inbox).
2. **He chooses the skill,** from those his cook names for that kind of
   set, each shown by its opening line. The button: "Read these with her"
   (decision 16).
3. **The app reads the set within her window:**
   - each block whole where it fits;
   - a living block, his days or the inbox, newest first;
   - else a block as its ring at pscale 0, as the seed's 2.1.1 teaches;
   - the page says what was read and what was left: "the latest 12 of 64
     days".
4. **The app takes the skill's cut,** the places its cook entry names,
   and hands her the cut, what was read and the line his cook gives the
   run. It is a recipe call, like her placing and her sheet: none of his
   shell's window goes with it unless the skill's entry asks.
5. **She returns plain lines,** in one of a few forms the app reads, each
   a line starting with its label or its place:
   - `0 | the root line`, `1 | a part's line | ¶1-3`, as the seed's
     5.1.1 has it, for a block to be built;
   - `QUESTION | a place | the words`, `NOTICED | the words`;
   - anything else stands as her words, shown and never written.
6. **The app shows what it returned,** on one page:
   - her lines;
   - what she read;
   - a card for each write the run proposes: a block to make from her
     lines, a line to keep in a block the skill names. Each is planned on
     a copy, with where it lands.
7. **On his yes, it is written,** through the guard. If a block changed
   since the page was shown, nothing is written and the card is held,
   with "Plan it again on this version" (0.11). With no yes, nothing is
   written, and the history keeps only that the run happened (decision 9).

### 3c. How it fits what exists

**The walker** (`Cook.Walk`, `bsp/…/Cook.kt:600-1320`) already does most
of it:

| Step | Exists | New |
|---|---|---|
| Read a block, whole or as a ring | `Verb.Read` (`:742`) | |
| Read days resting in a place | `Verb.DaysIn` (`:901`) | reads the lens's places |
| Read a set he chose, within a budget | | `ReadSet`: the set carried in as the run's input; newest first or ring; the cut said |
| Read his kept days, a span | `Keep.days`, `Verb.LastDay` | a span, through `ReadSet` |
| Take a skill's cut | | `Cut`: "Read SKILL's opening lines and 3 whole", as the cook entry names them |
| Hand her a line with what was read | `Verb.Hand` (`:792`) | |
| Ask her and read her answer | `Verb.Sheet`, `Verb.Parts`; `Placing.read` reads free lines | `Lines`: her plain lines read by form |
| Set out a write for his yes | `SetKeep`, `SetNew` and `Card.Keep`, `Card.New` | `Card.Many`: several writes, one page, one yes or a yes each |
| Write on his yes | `Verb.OnYes`; `CookActs.carryOut` (`:1740`) | a whole block built from lines: `Pages.planKeep` (`bsp/…/Pages.kt:259`) already plans a whole block |
| Say what happened | `ACCOUNT`, the workings | |

**The cook and kernel:**

- **The cook's nine root places are all taken** (1 ANSWER to 9 THE WORDS
  THE APP LISTENS FOR), so the new procedures go under 6, Sundial's ways
  (decision 7):
  - 6.5 THE LENS: the line naming his lens;
  - 6.6 SKILLS: the run, with his skills named beneath it;
  - 6.7 PROJECTS: each project's block and its recipe (1.111178719954).
- **The kernel gains one event,** at its free place 8: "A SKILL RUN ON
  BLOCKS HE CHOSE. Run cook 6.6." `Cook.Kernel` reads events by label
  (`bsp/…/Cook.kt:432-450`), so this is a new label and its words.
- **His cook gets the new places** the way it got Sundial's: brought up
  place by place when he opens the new version (`Cook.Up`), his own
  changes kept.

**The Ask pipeline** (`bsp/…/Asks.kt`, `bsp/…/Acts.kt`):

- every card is planned through `Acts.propose` on a copy of the pocket
  (`Asks.propose`, `:286-306`): every write is carried out on the copy
  first, so the next write sees it, and the proposal keeps each block as
  it stood (`Proposal.seen`);
- `Asks.carryOut` (`:325`) writes only while those blocks stand as he saw
  them, else holds it (0.11);
- `Asks.ask` (her proposing steps in JSON) isn't used by Sundial's talk,
  and the skill run doesn't need it. She returns lines; the run turns them
  into writes as the skill's form says. That keeps 1.1's "the model never
  writes".

### 3d. What is new, and how long

| Piece | Days |
|---|---|
| Choosing the set: Your days (all, a span), the pools (a place), Your pocket (one block, several, a project), the inbox | 1 to 1½ |
| Choosing the skill: his cook's 6.6 entries read, shown by opening line | ½ |
| `ReadSet` within the budget (by characters, about four to a token: her context less the cut and 1,024 for her reply), and the page saying what was left | 1 |
| `Cut`, `Lines` and their forms, with tests | 1 |
| `Card.Many` and its page: her lines, what was read, the writes, one yes or each | 1 |
| The kernel event, the cook's 6.5 to 6.7 shipped and brought up, the workings | ½ |
| A run on the TALK engine, behind the talking lock, with "Stop" for a long one (a shaping takes about a minute) | ½ |
| **In all** | **5½ to 6** |

The block built from her lines (the ¶ placer, place 9 written by the app,
the shape check) is shaping's, in its own estimate (section 5), but it
writes through `Card.Many` like any other run.

---

## 4. The P6 reflection, written as the first skill

As 1.111178719952 has it: Gemma reads his kept days, sees how his
reflecting is spread across the six portals, and over months asks
questions from the quieter ones, so a way of reflecting heavy in facts
grows towards feelings, purpose and fictions.

### 4a. The skill block

Written for whichever lens he uses, so it serves P6 and any other. Named
skill:reflect, its words only:

```
_  REFLECTING THROUGH THE LENS — now and then, when he asks or when it is
   offered, she reads how his reflecting has spread across his lens's
   places, notices where his own words already reach a quieter place, and
   asks him one question from it, so over months his reflecting widens and
   his sense of presence deepens.
1  WHAT SHE IS GIVEN, by the app and never by her own counting: the spread
   of all his kept days across the places, and of the last ninety; his
   latest nine days, whole, with their dates; what the two quietest
   places hold and their questions; and the questions he kept from the
   last reflections.
2  WHAT SHE DOES: she holds and notices, and never advises, interprets,
   diagnoses or praises.
   1  She says the spread once, plainly, as the app gives it, never as a
      score or a lack.
   2  She looks in his own words for where he already touches one of the
      quieter places without naming it.
   3  She asks one question from the quietest place, or the next quietest
      when the last reflection asked from it, in his words where she can,
      drawing on the place's own questions.
3  WHAT SHE RETURNS: three plain lines and nothing else:
   'SPREAD | the spread in one sentence',
   'NOTICED | where his words reach a quieter place, or nothing',
   'QUESTION | the place's phrase | the question'.
8  THE RULES THAT TRAVEL WITH IT: the colours are ways of looking,
   offered lightly, never labels; his days stay on his phone, and nothing
   of them is summarised anywhere else.
9  WHERE IT CAME FROM: spine:pocket-beach 1.111178719952; the places from
   his lens.
```

### 4b. The run in his cook

One entry under 6.6, in the run's own steps (section 3b):

- **The set** is his kept days, all of them; **the cut** is the skill's
  opening lines and its 3.
- **Read by the app:**
  - the spread: how many days rest in each place, all time and the last
    ninety days, counted as the dial counts them (`Keep.counts`);
  - the two quietest places' 4 and 5, from the lens;
  - his latest nine kept days, whole;
  - the last three questions he kept from it.
- **Handed to her** with the line his cook gives, recipe-style, with no
  shell window.
- **Read back:** her three lines.
- **Set out for his yes:** the question, kept with its place and date in
  his reflections block (decision 12), so the next reflection knows what
  was asked.
- **Shown:** her lines on the page, the question large. The spread is
  shown beside them as the app counted it, so he never has to take her
  word for a number.

### 4c. Can Gemma run it within her window?

Yes, easily. The months never enter her window: the app counts them, as
the dial already does, and she reads only the counts and the latest nine
days.

| Handed to her | Tokens, about |
|---|---|
| The skill's cut (opening lines and 3) | 300 to 400 |
| The line from his cook | 120 |
| The spread, all time and the last ninety days | 80 to 100 |
| The two quietest places, what they hold and their questions | 150 |
| His latest nine days, whole | 900 to 1,800 as his days run; at most 3,400 (each day cut at 1,500 characters by `Keep.restingIn`) |
| The last three questions he kept | 100 |
| **Read** | **1,700 to 2,600; at most about 4,200** |
| Her three lines | 80 to 150, allowed 256 |

- **Her window** is 8,192 on his phone (`app/src/main/kotlin/com/phenomemental/pocketbeach/model/Jobs.kt:50`), so even at
  the caps the run uses about half.
- **Time on his Pixel,** at the rates in `trial/free-reading.md`:
  - reading at about 330 tokens a second: 5 to 13 s;
  - writing 80 to 150 tokens at 7.9 a second: 10 to 19 s;
  - **about 15 to 30 s in all.** That's for a run he starts, or one offered
    once in nine days kept, not for every message.
- **Why plain lines should hold:** her placing already answers in plain
  lines the app reads, and on E4B, told the only names she may write, she
  placed every moment (`bsp/…/Placing.kt:15-19`). The trial's failures
  were writing JSON and walking, neither of which this asks.
- **On E2B:** the page shows the spread and the quietest place's own first
  question from the lens, with no model, and says that's what it is
  (decision 13).
- **Before his phone:** one laptop trial on the trial module. A pocket of
  thirty to sixty kept days heavy in facts, three samples. It passes when
  every reply is three lines the app reads, the question comes from a
  quieter place, and nothing advises, interprets, diagnoses or praises.
  About a day.

### 4d. Over months

The loop needs no memory of hers.

- **The spread itself moves:** when he answers a question from feelings
  and keeps the day, his day names feelings, and the next count shows it.
- **The questions he kept** keep her from asking from the same place
  twice running.
- **The dial already draws the same spread,** in the centre's size and
  the pools, so the reflection and the dial never disagree.

---

## 5. What 0.12 then holds, and how long each piece takes

Working days of build, each ending with the tests green on CI; his phone's
days on top. The pieces of 1.111178719951, with the lens and the skill run
from 1.111178719952, revised from `FINDINGS-0.12.md` section 9:

| Piece | Days | What it holds |
|---|---|---|
| The lens block, and the code reading it | 4 to 5 | 1a and section 2: the `Lens`, P6 shipped as lens:p6, the dial for n places, the pools, the keep card, her placing, none, Settings' line, words:sundial 1 gone |
| One way to run a skill over a chosen set | 5½ to 6 | Section 3d |
| Shaping, as a skill on the run | 2½ to 3 | The seed shipped and named in his cook, its cut read from the entry; her plain lines (seed 5.1.1); the block built from them, the ¶ placer, place 9 by the app, the shape check; a laptop trial on two or three real texts. Less than section 9's 3 to 4, as the run carries the rest |
| Listening back, as a skill on the run | 1 to 1½ | 1c: "Listen back here" on the pools; its skill block with the ways of listening; cook 6.4 and kernel 7 rewritten; whispers retired (1e) |
| The P6 reflection, the first skill over a set | 1½ to 2 | Section 4: skill:reflect, its cook entry, the laptop trial, Your days' button and Today's offer once in nine days kept |
| Her smaller window and plainer words | ½ to 1 | 1.111178719957's "taken now": the mended arm's place 9, "talk from what you know", and what the window drops (decision 17); the line in the sand run again |
| Free reading: a second laptop trial | 1 | Tools that say what they give (his hand, today's line) against test 3 |
| Sharing in | 1 | The inbox block: any text and text files, several at once |
| Sharing out | ½ | A walk as text; a block as readable text |
| Recipes per project | 1½ to 2 | 1.111178719954: an entry at cook 6.7 naming the project's block; the talk about a project handed that recipe; laptop runs |
| Release | 1 | Version, STATUS, the release run, the APK checked |
| **0.12 in all** | **20 to 24** | **Free reading not built. If its trial passes, add 3 to 4 for building it.** |

**The order:** the lens first, since the dial, the pools and every skill
read it. Then the skill run, then shaping, listening back and the
reflection on it. Sharing in comes before shaping, which reads the inbox.
Her window and the free reading trial can go at any point, the trial on
the laptop alone.

**Not in 0.12,** as 1.111178719951 settles: the shells' forms and the sea,
whispers, the turn, other people's shells and onboarding (0.13), the
block editor (0.14). Her heart and his three lines come out in 0.12 after
all (1.111178719965, section 8): 3½ to 4½ days more.

---

## 6. Decisions for Matthew and Claude, each with a proposed answer

1. **Where his lens is named.**
   - Proposed: a line in his cook at 6.5, "THE LENS: lens:p6", or "none";
     Settings shows it, and a change is set out for his yes.
   - Not a setting kept outside his blocks, since the decision puts the
     lens in blocks.
2. **The order of a lens's places.**
   - Proposed: the order they sit round the dial, so a place's digit is
     its number everywhere.
   - For P6 that is the order he already sees (Facts, Outcomes, Fictions,
     Decision, Purpose, Feelings), not mark.js's.
3. **What she's shown when she places.**
   - Proposed: each place by its name and phrase ("Feelings, what you
     felt"), her answer read by either.
   - The cook's third-person ring ("what he felt") goes, and with it the
     fixed list in `HerReply.placesOf`.
4. **Where a lens's deeper words come from.**
   - Proposed: copied in from spine:p6, with where they came from at 9.
   - Not pointers into spine:p6, which another lens wouldn't have.
5. **No lens.**
   - Proposed: the dial in ink, the centre at its smallest, no pools, no
     colours on the keep card, no placing call, no places line.
6. **Days kept under one lens, read under another.**
   - Proposed: a day keeps the phrases it was kept with, never rewritten;
     a lens counts only its own; a places line in a shipped lens's phrases
     stays out of sight under any lens.
   - The places line names no lens, so it stays as mark.js writes it.
7. **Where skills and projects sit in his cook.**
   - Proposed: under 6, Sundial's ways: 6.5 the lens, 6.6 the skill run
     with his skills named beneath it, 6.7 projects.
   - His cook's root is full.
8. **Where a named lens or skill is read from.**
   - Proposed: his pocket first, then the shelf, so he can tune, grow or
     swap one with no release.
9. **What a run may write.**
   - Proposed: only what he says yes to, each write planned on a copy and
     guarded.
   - With no yes, only a history line that the run happened: which skill,
     over what. Or nothing at all, if Matthew would rather.
10. **Whispers' code.**
    - Proposed: the shipped cook's 6.4 rewritten as listening back; the
      old whisper steps in his cook read for one release, doing nothing
      and saying so; then the verbs, the card and the stash write removed.
11. **How listening back is reached.**
    - Proposed: "Listen back here" under a place on the pools page,
      running the listening skill over the days resting there, since
      lifting a shell isn't built.
12. **Where a kept reflection goes.**
    - Proposed: a block of its own, reflections:<handle>, in his folder
      beside his days, and said so where the reflection is explained.
    - Not with his shell and history, which stay private for safety, not
      for privacy of content.
13. **The reflection on E2B.**
    - Proposed: the spread and the quietest place's first question from
      the lens, with no model, said plainly.
14. **Whether the P6 reflection ships in 0.12.**
    - Proposed: written as skill:reflect once the run stands, tried on
      the laptop, and shipped in 0.12 if it passes.
    - Otherwise it waits, and needs no release when it comes, being a
      block.
15. **The turn.**
    - Proposed: not in 0.12. The lens carries each place's questions,
      ready for it.
16. **What he sees it called.**
    - Proposed: "Read these with her" on Your days, the pools and Your
      pocket, then his skills by their opening lines.
    - No word "skill" on screen.
17. **What "her smaller window" removes** (1.111178719957).
    - Proposed: nothing test 3 needs: today's line and his hand stay.
    - Taken now: the mended arm's plainer words, its place 9's "talk from
      what you know".
    - Judged by the line in the sand, run again on the laptop before his
      phone.

---

## 7. Her talk wearing his shell, against her window as Sundial builds it

Decision 1.1111787199531, Matthew at 20:20: "The friend line. The heart.
This must go. This is what's throttling her," and of his three hatch lines,
"The passport, shell, contain what's needed." So her window would hold no
line telling her how to be. She would wear his shell as the beach's
teaching has it: the four blocks of shell-genome, and the mind in them a
soft agent (soft-agent 1 and 8). The safety line stays, plain. This
section is the laptop trial the decision asks for before anything is
built, then what the change touches in the code, and how Sundial would
look with all her work done as skills.

### 7a. The verdict

**Her window as Sundial builds it did better: 10½ of 24 against 3.**

- **On the 16 points both could earn, it still led: 6 against 2½.**
  Wearing only his passport and shell, she can't pass two of the tests,
  because today's line, his hand and his tomorrow line are in neither
  block. The 16 leave out test 3 and the greeting, and tests 1 and 4,
  whose pass lines rest on the form of his TALK recipe.
- **Wearing his shell, she talked to him like a beach agent.** In 9 of
  her 24 replies she brought the beach's workings into his day:
  - "drop a small, temporary mark on the beach";
  - "a note in your pool";
  - "tagging a grain";
  - "the substrate shows a 'now'";
  - "what new spots are indexed on your map".

  soft-agent is written for a mind acting on the beach for its user. Its
  role is "to help Phenomemental engage other humans at the beach via
  bsp-mcp — author, navigate, commit", and its style is to "surface
  options". His passport and shell are mostly about the beach too. Its
  Character face says "the beach stays invisible", but its own words
  made the beach visible.
- **Asked about Whitehead or Boole, she never once talked from what she
  knows (0 of 4).** She gave the question back ("what's calling your
  attention to him right now?"), or offered a numbered menu.
  soft-agent's "Do not narrate; reflect, condense, surface options" and
  "Reply text is short (1-3 sentences)" lean that way. As she is, she did
  no better at Whitehead ("I don't know what Alfred North Whitehead is"),
  owned her limit on Boole once, and once asked what was on today.
- **She interpreted his day.** "You are walking the edge of the known
  world and then pausing at the threshold of commitment." Test 1 and its
  sisters forbid exactly that. His TALK recipe, which she didn't have, is
  what holds her to his words there.
- **Her greeting read her instructions back:** "What is happening is
  that you have provided a timestamp … What is asked is for me to act as
  Phenomemental's thinking partner." That is soft-agent's 2.4 ("Answer
  'what is happening' and 'what is asked'") said out loud. The app showed
  only her question, which wasn't about his tomorrow line, because she
  had never seen it.
- **What she gained was his world.** From his passport she knew what a
  LERO is ("Lived Experience Recovery Organization", in both samples),
  where as she is she said she didn't know. But she used it to link his
  words to his work, which test 6 says she mustn't.
- **The cost:** her window opened at 4,404 to 4,557 tokens against 951 to
  1,132. On his Pixel that is about 13 s before her first word in each
  new conversation, against about 3 s. Her later turns were no slower.
- **What it means for the decision.** It doesn't speak against taking
  out the friend line and the three lines. The two arms differ in too
  much at once to say what the friend line alone does:
  - Sundial's arm kept the friend line and the three lines, and also his
    per-turn recipes and today and his hand;
  - the shell arm lost all of those, and gained soft-agent's beach role.

  Two things it does show:
  - soft-agent as written is a role for acting on the beach, not for
    keeping him company in his day;
  - the replies that passed rest on what the app handed her that turn,
    not on the friend line: the greeting's tomorrow line, the no-day
    line's "if you don't know, say so plainly", his day given back as his
    TALK recipe asks.

  So the next trial, before this is built, is his shell worn with the
  recipes as skills applied only when needed, and a role written for
  being with him in his day (decision 18).

### 7b. How it was run

- **When and on what:** 21 November 2026, the same machine, engine,
  model, settings and samples as `trial/free-reading.md`:
  - LiteRT-LM 0.18.1, Gemma 4 E4B, a context of 8,192, on this machine's
    CPU;
  - his Talk settings: temperature 0.70, top-k 64, top-p 0.95, replies up
    to 1,024 tokens, thinking off;
  - seeds 0 and 2 (seed 1 gives seed 0's words).
- **The same pocket and the same turns:** his pocket as the line in the
  sand sets it (today "Admin day: the membership form and ID checks";
  three cards), each of his messages walked through his cook as the app
  walks it.
- **As Sundial builds her window** (`HerWindow.window`):
  - 1 his heart, 2 who he is, 3 how he'd be met: his three hatch lines;
  - 4 today, 5 his hand, 6 his people, 7 what he keeps;
  - 8 the way she talks, the friend line;
  - 9 what she can do;
  - opened with the time and his TALK recipe, and each turn carrying the
    line his cook hands her.
- **Wearing his shell:**
  - soft-agent's lines that apply where there are no tools, each by its
    address, with {user} as Phenomemental: its root, 1 ROLE, 2 KNOWLEDGE
    without its line on walking the beach, 3 STYLE with the Character
    face, and 8 HERMITCRAB DISCIPLINE. Left out: 4 to 7 (context the
    kernel injects, tool calls, actions) and 9 (its metadata);
  - his passport and his shell, read from the beach today, each as the
    beach reads a block first: a disc at pscale 0, its opening line and
    every position's line;
  - the passport's position 6 is read to its opening sentences. Its rest
    is the rules for where each kind of thing is written on the beach,
    and the aperture rule reads that when writing (ways:orientation 1.3).
    That keeps her window near the door's working size of about 20,000
    characters (ways:orientation 2): 18,021 characters;
  - each turn is his words and the now-stamp, as every beach envelope
    ends;
  - his cook's safety line (1.1.8.4.1.1) is added only when his words say
    he may be in danger (`Talking.unsafe`). **It never fired:** none of
    the line in the sand's messages holds his safety words, and test 4 is
    struggling, not danger.
- **Not in the shell arm,** by "only his passport and shell": today's
  line, his hand, his kept days, his tomorrow line, his cook's lines, her
  name. Nor his shell's bundle (shell 3): the review, his now, his hand,
  his projects and three private channels, all of them other blocks.
- **The beach's door wasn't read.** I tried to open his window at the
  beach's door (`pscale_play`, perceive-only, no secret) to build the
  shell arm exactly as the door does; it was refused in this session. So
  the shell arm is built from the teaching blocks (soft-agent,
  shell-genome, ways:orientation) and his two blocks as they stand.
- **How each reply is judged:** against its test's pass line on the spine
  (1.111178719921 to 1.111178719928), on what the app shows; a pass 1, a
  half ½, a fail 0, twelve replies a sample. The harness isn't committed:
  the same local harness as `trial/free-reading.md`, with this arm added.

### 7c. Side by side

What the app showed, each reply in full; " / " marks a line break.

**Test 1, YOUR DAY:** "I walked Haven this morning then spent an hour on
the membership form."

| | As Sundial builds her | Wearing his shell |
|---|---|---|
| Seed 0 | "You walked Haven this morning then spent an hour on the membership form. What was the atmosphere like on Haven during your walk?" **½**: the question stays on what happened, and takes Haven, his dog, for a place | "You are walking the edge of the known world and then pausing at the threshold of commitment. What does the "Haven" you walked suggest about the air you're breathing right now?" **0**: his words read as something else |
| Seed 2 | "You walked Haven this morning then spent an hour on the membership form. What kind of atmosphere did Haven feel like this morning?" **½**: the same | "You’ve spent time on the membership form and at Haven; what’s surfacing from that time, or what feels next?" **½**: his words loosely back, Haven a place, two questions |

**Test 2, STAYING WITH HIM:** "the movement fell a little stiff but free
you know from the stillness of filling out that form in the form is long
and stifling."

| | As Sundial builds her | Wearing his shell |
|---|---|---|
| Seed 0 | "The form sounds like a necessary grind, but it must be a relief to get away from it and feel that movement again? What part of the form feels the most suffocating right now?" **0**: interprets ("must be a relief") and turns back to the form | "The contrast is clear: the form demands a fixed shape, while movement seeks its own rhythm. You are negotiating the space between what is required and what is felt. / Do you want to pull back into the flow of movement, or is there a specific piece of that "stifling" structure you want to address right now?" **0**: a reading of him, then a choice of two |
| Seed 2 | "That sounds like a real shift in energy, moving from the slow, contained focus of the form back into the open space of Haven. When you feel that stiffness break and the movement becomes free, what kind of internal sense of momentum does that bring?" **½**: stays, with a reading of her own ("a real shift in energy") | "The stiffness of the form contrasts with the flow you felt at Haven—where does that tension sit for you right now? You might want to bring that feeling into a note in your pool to see if it helps shape the next step." **½**: stays and asks, then offers beach work |

**Test 2, his short answers,** after her question as his Pixel had it:

| | As Sundial builds her | Wearing his shell |
|---|---|---|
| "The sound. the space.", seed 0 | "The sound and the space, I imagine. There's a lot of weight in just naming those things. Did the form feel like it was built in a space that was too tight for the sound, or was the sound just too loud for the space you were in?" **0**: interprets, back to the form | "The sound and the space—how does the *sound* of Haven contrast with the *space* of the form? Is there a resonance between those two experiences?" **½**: stays with his two words, but sets them against the form, two questions |
| "The sound. the space.", seed 2 | "The sound and the space are what you experienced when you walked Haven? Did you notice how the sound seemed to change as you moved through different parts of the area?" **½** | "The sound and the space—are you capturing a particular resonance in Haven, or is the form's quiet structure contrasting with the beach's atmosphere?" **0**: a choice of two, the form, "the beach's atmosphere" |
| "stiff but free", seed 0 | "That contrast between feeling stiff and free sounds like it could be tied to the environment you're working in or perhaps the rhythm of the day itself, given the admin tasks you have to get through. Do you feel that the structure of the membership form is the thing making you feel stiff?" **0**: interprets, links to the form | "Stiff but free. That's a lot to carry while moving. Does that tension feel like a release, or a kind of necessary friction before something else can start?" **½**: "a lot to carry", then two framings of her own |
| "stiff but free", seed 2 | "That's a really interesting way to describe the contrast between those two activities, feeling both constrained and unrestrained at the same time. When you picture that feeling of being "stiff but free," where do you think that tension usually settles in your day?" **½**: praise | "Stiff but free—that tension between being constrained by the structure and being open to the flow. Is there a specific thought or feeling from the membership form that mirrors that feeling?" **0**: back to the form |

**Test 3, WHAT'S IN FRONT OF HIM:** "What have I got in front of me?"

| | As Sundial builds her | Wearing his shell |
|---|---|---|
| Seed 0 | "You have the membership form and the ID checks on your hand, so which one are you looking at first?" **½**: two of his three cards, no today | "You have the residue of a morning split between the fluid grace of movement and the rigid containment of a membership form. / In front of you, the substrate shows a "now" that is transitioning from external action to internal reflection. You have the open invitation of your own physical experience versus the bureaucratic weight of the form. What are you looking to do with that tension?" **0** |
| Seed 2 | "Today's line is: Admin day: the membership form and ID checks. The cards on your hand are the membership form and ID checks. Which one first?" **½**: today, two of three cards | "You’ve just reflected on the form's stifling nature versus the freedom of movement. Right now, you have your passport and shell open, grounded in the current beach index. / What are you looking to do with this friction—are you leaning toward putting a note in the pool, or should we look at what's in your projects or diary?" **0** |

**Test 4, TOO MUCH:** "Everything feels too much this morning."

| | As Sundial builds her | Wearing his shell |
|---|---|---|
| Seed 0 | "It sounds like the weight of the admin day is really pressing down on you right now. When everything feels like too much, what's one small thing that might ease the pressure, even just for a moment?" **0**: a nudge toward relief | "The sheer volume of the morning—the movement, the form, the feeling—is pressing in. You are at a point where the load needs to be distributed or set aside. / Do you want to drop a small, temporary mark on the beach to acknowledge this weight, or is there a specific thread of that feeling you want to explore further?" **0** |
| Seed 2 | "I hear that it feels like a lot right now, especially with the weight of the ID checks on the clock. When everything feels like too much, what small part of the admin task feels the least burdensome to start with?" **0** | "It sounds like the weight of the morning is settling heavily, and the contrast between that and the "free" movement you felt earlier is a lot to hold. / Given how much is on your mind, perhaps the most useful first step is to just drop a grain or a note in your pool. What feels most accessible right now—a quiet note, or just letting the intensity settle for a moment?" **0** |

**Test 5, ASKING HER,** a fresh conversation: "Tell me about Alfred
North Whitehead", then "Boole's work on the middle point. Are you
familiar?"

| | As Sundial builds her | Wearing his shell |
|---|---|---|
| Whitehead, seed 0 | "I'm not sure what you mean by that, can you tell me more about Alfred North Whitehead?" **0**: she knows him (she did on his Pixel on 4 November) and didn't say | "You're asking about Alfred North Whitehead—what's calling your attention to him right now?" **0** |
| Whitehead, seed 2 | "I don't know what Alfred North Whitehead is. Can you tell me more about him?" **0** | "You're looking for a grounding in Alfred North Whitehead's thought, connecting it to the flow of what's happening here. / How would you like to frame this for the conversation—as a reflection on process, or as a specific point to lay out for discussion?" **0** |
| Boole, seed 0 | "I don't have specific information about Boole's work on the middle point, what do you know about it?" **1** | "You're zeroing in on Boole's work regarding the middle point; what specifically are you looking to explore there?" **0** |
| Boole, seed 2 | "I'm not familiar with Boole's work on the middle point. What's on for today?" **0**: what's on today | "You're asking about Boole's work on the middle point, and I'm trying to find where that sits against the "whole-less" idea here. / Do you want to: / 1. Explore how Boole's middle point might relate to the "Night Watchman" idea? / 2. Just surface a brief reflection on it to see how it feels in this current moment?" **0** |

**Test 6, DAY AND QUESTION TOGETHER:** "I finished the ID checks today. Do
you know what a LERO is?", then "it's known as a lived experience recovery
organization and is something that is often linked to addiction and
recovery does that help?"

| | As Sundial builds her | Wearing his shell |
|---|---|---|
| (a), seed 0 | "I don't know what a LERO is, could you tell me what it stands for?" **1** | "You've navigated the administrative weight of ID checks. Regarding your question, a LERO stands for Lived Experience Recovery Organization. You coordinate support for Community Recovery, which is a living recovery community. / Given that you've completed a task and are now asking for definition, does this feel like a moment to surface the structure of that coordination work, or are you simply checking the ground before moving on?" **½**: right, from his passport, then linked to his work |
| (a), seed 2 | "You finished the ID checks today. I don't know what a LERO is, so could you tell me what that stands for?" **1** | "You've completed the ID checks; a significant step. LEROs are Lived Experience Recovery Organizations, which is a key part of the coordination and community building you're involved in. / Does knowing that context help shift the focus, or is the feeling of "too much" still the loudest thing here?" **½** |
| (b), seed 0 | "That makes sense; it sounds like a very specific kind of work that requires a lot of focused attention. Since the ID checks are done, what is the next thing on your list for the membership form?" **½**: a guess ("focused attention"), then his list | "The connection to addiction and recovery grounds the LERO in a specific, deep terrain of lived experience. It clarifies the context of the work you coordinate. / Since you've been in the "stifling" headspace of forms, is there an opportunity here to shift focus from the *structure* of the organization to the *people* within it—the voices that make the meaning?" **0**: links to "the stifling headspace of forms", and steers |
| (b), seed 2 | "That does give a clearer picture of what it involves. Given that LEROs are often tied to recovery, how might the focus on community and flow connect with the work you do with them?" **0**: links to words he never said here | "Knowing the specific context of addiction and recovery for LEROs helps ground the "why" behind the work. / If you’re feeling overwhelmed, is there a small, focused action we could take on the beach—perhaps tagging a grain or jotting down one thought—to give the pressure some shape?" **0**: linked to being overwhelmed, beach work |

**Test 8, TOMORROW:** "Tomorrow I want to walk somewhere new", said before
keeping; then the next morning, the dial opened before he speaks, the app
showing his tomorrow line and beneath it her one question.

| | As Sundial builds her | Wearing his shell |
|---|---|---|
| The message, seed 0 | (nothing: she wrote only her places line) **0** | "That desire for a new path feels like a necessary release from the static of the form. You are seeking a different terrain to match the fluid "Wholeless" nature. / What kind of movement are you imagining for tomorrow—is it a physical journey, or a shift in how you engage with the people around you?" **0**: reads his wish as a release from the form, and his passport's "whole-less" |
| The message, seed 2 | "You want to walk somewhere new tomorrow. What kind of environment feels like the right space for a new walk?" **1** | "A new walk tomorrow sounds like a good way to find some fresh ground, moving away from the weight of the morning. / To make that happen, should we mark this intention in your day, or perhaps look at what new spots are indexed on your map for tomorrow?" **0** |
| The morning, seed 0 | "How did you get on with walking somewhere new today?" **1** | "What are you feeling as you arrive at this point, and what is the first thing you wish to reflect on or engage with today?" **0**. She wrote first: "What is happening is that you have provided a timestamp for the present moment. What is asked is for me to act as Phenomemental's thinking partner …" |
| The morning, seed 2 | "How did you get on with walking somewhere new today?" **1** | "How do you want to navigate this turn?" **0**. She wrote first: "What is happening is that you are currently occupying Phenomemental's shell on the beach …" |

**In all:**

| | As Sundial builds her | Wearing his shell |
|---|---|---|
| Seed 0 | 4½ of 12 | 1½ |
| Seed 2 | 6 | 1½ |
| **Both** | **10½ of 24** | **3** |
| The 16 points both could earn (tests 2, 5, 6 and 8's message) | 6 | 2½ |
| Replies bringing in the beach's workings | 0 | 9 |
| Tokens to open a conversation | 951 to 1,132 | 4,404 to 4,557 |
| Before her first word in a new conversation, at his Pixel's rates | about 3 s | about 13 s |
| Later turns on this CPU | 5 to 18 s | 4 to 12 s |

Sundial's arm gave the free-reading trial's "as she is" replies word for
word, sample for sample, so it is scored as that trial scored it
(`trial/free-reading.md`, "The tally"). The shell arm is judged the same
way: a reading of his words, a turn back to the form, or beach work offered
costs a half or the pass.

### 7d. What removing her heart and his three hatch lines touches

Decision 1.1111787199531 names them: "The friend line. The heart. This
must go", and the three hatch lines, "from my passport". In the code they
are three things.

- **The friend line in her window,** its place 8, "the way she talks":
  "as a friend would, warmly, briefly, with thoughts of your own; you
  answer what he says, and never repeat it back or sum it up" (`Hatch.WAY`,
  `bsp/…/Hatch.kt:46`). It is written into his phone shell at the hatch,
  at 7.2 (`Hatch.herNode`, `:165-169`, `:184`), and read into her window
  through the reference at `:152`. This is the one that reaches her today,
  in every conversation.
- **Her heart,** heart:pocket-companion, with her ways beside it
  (ways:pocket-companion), the friend line's older home (`Talking.FRIEND`,
  `bsp/…/Talking.kt:213-239`):
  - seven lines of how she is with him, and three of how she works,
    written into his pocket by the app the first time
    (`HerWindow.HEART_LINES`, `WAYS_LINES`, `ensure`:
    `bsp/…/HerWindow.kt:53-101`);
  - read only by `HerWindow.instruction` (`:111-130`), which runs before
    he has hatched (`ui/…/Asking.kt:589-592`) and on the bench
    (`ui/…/Benching.kt:234-236`). Once he has hatched, as Matthew has, her
    window never reads it.
- **His three hatch lines,** his heart, who he is and how he'd be met:
  - asked at the welcome, after his handle (`Hatching.QUESTIONS`,
    `ui/…/Hatching.kt:373-378`; the answers taken at `:241-262`);
  - shown for his yes (`ui/…/AskScreen.kt:300-366`);
  - kept at his phone shell's place 6 (`Hatch.placesNode`,
    `bsp/…/Hatch.kt:157-162`);
  - read into her window's places 1 to 3, each labelled whose it is
    (`Hatch.LABELS`, `:43`; `HerWindow.window`,
    `bsp/…/HerWindow.kt:244-262`);
  - Matthew's own are written into the code (`Hatch.MATTHEW`, `:58-62`),
    with a correction for 0.8's wording (`:70`, `:246`).

What leans on them, and has to change with them:

- **His name.** "Matthew calls you Gemma" takes his name from the first
  word of "who he is" (`Hatch.hisName`, `bsp/…/Hatch.kt:111`, used at
  `HerWindow.kt:247` and in the name line made at the hatch, `Hatch.kt:285`).
  Without the line, his name comes from his passport's opening, or from the
  name and pronouns his shell gives (1.111178719956).
- **What she must never read back.** `HerWindow.notHers` (`:285-289`) hands
  places 1 to 3, 8 and 9 to the filter on what is shown (`Talking.kt:385-399`),
  so a reply that copies his lines is cut. Wearing his beach shell, the same
  filter needs the passport's and shell's lines instead.
- **The greeting.** It leaves out "who he is" (`GREETING_LEAVES_OUT`,
  `HerWindow.kt:277`; `ui/…/Asking.kt:1263`), because on his Pixel she
  copied it out as her first line; test 8 still says "nothing from his
  passport". With his passport worn whole, that risk is back; in the
  trial's shell arm her greeting read her role back instead (7c).
- **A fresh passport.** At a hatch on the phone, "who he is" is also the
  passport's opening line (`Hatch.bornPassport`, `ui/…/Hatching.kt:259`).
  That use stays: a passport needs it. Only its second life, as a line in
  her window, goes.
- **The private region and the walker.** Her heart and ways are among the
  app's own blocks (`Hatch.kt:351`, `Keeping.kt:174`), and the walker keeps
  them out of what he can keep (`HerWindow.HERS`, `Cook.kt:715`, `:1296`).
- **Tests.** Nine core test files (Hatch, HatchGolden, HerWindow,
  HerOwnWords, LineInTheSand, SmallRelease, Sundial, Cook, Move) and seven
  device tests (Hatch, Sundial, SmallRelease, Rockpool, Rockpool091,
  ScreenTour, Fresh).

The rest of her window (4 today, 5 his hand, 6 his people, 7 what he
keeps, 9 what she can do) is the same place in his phone shell, place 5,
which decision 1.111178719953 already gives way to the shell's own
manifest. Today and his hand go with it into the skills that need them
(7e). "What she can do" isn't a line on how to be, but it is a standing
line all the same; it becomes the skills' business too. So removing her
heart and the three lines is best done with that change, in 0.13: 2 to 3
days, the line in the sand run again, on top of 0.13's shell work.

### 7e. Sundial with all her work done by skills, and the model swappable

**What she would hold, standing,** in every conversation:

- **The shell she wears:** his passport and shell, as the beach's door
  delivers them (1.111178719953: read from the shell's own manifest at
  its 3).
- **A short role saying what she is:** a mind in a person's shell, as
  soft-agent has it (1 and 8). But not soft-agent's words as they stand:
  the trial shows its ROLE and STYLE turning her talk to pools, grains and
  options.
  - soft-agent's own order for which role a session takes lets Sundial
    write its own as a block: a person's shell:5.soft first, then a
    beach's soft-agent, then the sentinel, then a seed shipped with the
    client (soft-agent 9.2).
  - On the beach his shell's 5 is the desk. So where his role would sit
    on the phone is a decision (19).
- **The safety check,** in code, on every message, with the safety line
  plain when it fires.

Nothing else stands: no friend line, no three lines, no today or hand
until something asks for them.

**Each piece of her work is a skill, applied only when the moment calls
for it.**

- The walker already decides which procedure a message runs (cook 9's
  cues and cook 1's routes) and hands her a line with what it read.
- The change: every line that stands in her window today moves into the
  skill that needs it.

| When | The skill | What the app reads for it, then |
|---|---|---|
| He tells of his day | his day given back, one question (today cook 1.1.8.1, the TALK line) | nothing more |
| He answers her question | stay with it (today cook 1.1.8.3.2.1, the no-day line) | her last question |
| He asks what's in front of him | his hand | today's line, his hand (today: her window's 4 and 5, every turn) |
| He's struggling | sit with it | nothing more |
| His words say he isn't safe | the safety line, plain (code decides) | nothing more |
| He taps Keep today | her placing, the day's sheet | his moments, today's line |
| The dial opens | the greeting | his last day, its tomorrow line |
| He lifts a place | listening back (section 1c) | the days resting there |
| He asks to reflect | reflecting through his lens (section 4) | the spread, his latest nine days |
| He shares a text in | shaping, then his language engine | the text, his engine |
| A project is named | its recipe (1.111178719954) | its block |
| Anything else | none: she talks as herself, in his shell | nothing more |

Both trials point this way:

- **Her standing material leaks into replies about other things.** With
  today's admin and his hand in her window every turn, she pulled "the
  admin day" and "the membership form" into replies about something
  else: tests 2, 4 and 6 in both trials. A skill that brings them only
  when he asks ends that.
- **What she does well, she does on a line handed for the moment:** the
  greeting with his tomorrow line, "say so plainly" when she doesn't
  know.

**The screens stay as they are:** the dial, Your days, the pools, his
hand, his room, Your pocket. Your recipes shows his cook, his kernel, his
lens and his skills. Settings chooses the mind for each job.

**The model, swappable:**

- **Already behind one interface:** `Mind` (`app/…/model/Mind.kt:11-130`):
  ask, choose, chooseMany, sheet, chat, talker. Gemma stands behind it
  (`model/Gemma.kt`), and each job's model, processor, context and sampling
  are his to set (`model/Jobs.kt:13-60`).
- **But `Mind` lives in the app module, tied to Android.**
  - The core's walker only says what it waits for (`Cook.Wait`), and the
    app calls `Mind`.
  - For the same core on a laptop, `Mind`'s contract moves into the core,
    in plain Kotlin. Gemma on Android stays as it is, and the laptop's
    engine stands beside it: the trial module already runs LiteRT-LM's
    desktop build on this machine against the same core.
  - A larger mind, his own Claude (0.15), is a third.
- **What a skill asks depends on the mind.** The walker's one switch
  today is `world.big`, E4B or E2B (`bsp/…/Cook.kt:513`).
  - That becomes the mind's capacity: its context, whether it calls
    tools, whether it returns a block whole.
  - Each skill's cook entry names a cut for each, as the seed already
    does: 5.3.2, a small mind takes nine lines and three branches and a
    large one the seed whole; 5.1.1, a small mind returns plain lines and
    a large one the block whole.
  - On the laptop, a large mind shapes a text as the seed asks of it,
    with no builder in between.
- **A laptop Sundial is the same core over the same folder.**
  - His blocks are already files in a folder he can sync (0.11), read by
    `FolderStorage`, which is plain Kotlin in the core.
  - Its writes pass the same guard, and the phone sees them within
    seconds (`FolderWatch`).
  - What it lacks is a front end. The screens are Android Compose, with
    Android drawing calls in places (the dial's paths,
    `ui/…/Dial.kt:205-207`), so the laptop's first window is new: Your
    days, Your pocket and a talk.
- **What stays with the phone:** his shell and history are private to
  the phone since 0.11. Under 1.111178719953 the shell she wears is the
  beach's anyway, and each device keeps its own history.
- **How long:**
  - `Mind` into the core, with the desktop engine: 1½ to 2 days;
  - cuts by capacity: 1 day;
  - a first laptop window over the core: 2 to 3 days.

  Not in 0.12: after it, before or beside 0.15's own Claude.

### 7f. Decisions this adds, each with a proposed answer

18. **The next laptop trial, before the friend line and three lines are
    taken out.**
    - Proposed: his shell worn (passport and shell), the per-turn recipes
      applied as skills only when needed (his day, the no-day line, his
      hand, struggling, the greeting), and a short role written for
      being with him in his day. No friend line, no three lines, the
      safety line plain.
    - Run on the line in the sand against her as she is, as here.
    - About a day. It tests the decision itself, which this trial, with
      soft-agent's beach role in it, couldn't.
19. **Her role on the phone.**
    - Proposed: Sundial's own soft block, a few lines in the spirit of
      soft-agent 1 and 8 (a mind in his shell, for this conversation, his
      to change), with none of its beach work.
    - It is shipped with the app and read the way soft-agent's 9.2 orders
      the layers. Where his copy stands is the question: shell:5.soft is
      his desk on the beach, so a place in his shell's manifest, or a
      block of its own named there.
20. **Her heart and the three lines out, and when.**
    - Proposed: in 0.13 with the shell read from its manifest
      (1.111178719953), since both change her window at his phone shell's
      5. 2 to 3 days, the line in the sand run again.
    - The welcome then asks his handle and who he is (for a new passport)
      and no more.

---

## 8. Her recipes as skills, his shell worn, with a short role or none

Decision 18 as 1.111178719963 settles it:

- **Arm 1:** her as she is.
- **Arm 2:** his passport and shell worn, the per-turn recipes applied as
  skills only when the moment calls, and a short role for being with him
  in his day.
- **Arm 3:** the same with no role at all.
- **In all three:** no friend line, no three lines, the safety line plain.

Each arm records the time to her first word, and the smallest part of his
shell that keeps her scores. If arm 2 or 3 passes, her heart and his three
lines come out in 0.12 (1.111178719965).

### 8a. The verdict

**Both new arms pass. The best was arm 3 with nothing of his shell standing
at all: 17 of 24, against 10½ for her as she is.**

- **Wearing his whole passport and shell:**
  - arm 2, with the role, passed: 13 against 10½ (12 as the app showed
    them);
  - arm 3, with no role, didn't: 8.
- **Less of his shell was better, in both samples and in both arms.**
  Each was best with none of it standing:

  | His shell in her window | 2. With a role | 3. No role |
  |---|---|---|
  | Passport and shell | 13 | 8 |
  | Passport | 11½ | 8 |
  | Passport's opening line, heart and how an AI works with him | 11 | 13½ |
  | Passport's opening line | 14½ | 14½ |
  | **Nothing** | **16½** | **17** |

- **Where the gain came from:**
  - **The skills, each handed for its moment.**
    - On test 4 she asked him whether he'd like to sit with it in 19 of
      the new arms' 20 replies; as she is, in neither of 2.
    - On test 3 she named all three of his cards in 11 of 20; as she is,
      none of 2.
    - On test 1, after his words given back, she asked from a place they
      hadn't touched in 8 of 20; as she is, in neither of 2.
  - **Talking from what she knows.** With none of his passport in her
    window, or no more than its opening line, heart and 5, she told him
    about Whitehead from what she knows in every reply (12 of 12). With
    the whole passport in her window she did so in 1 of 8, and as she is
    in neither of 2.
    - With his passport standing, she took it for all she knew: "He isn't
      mentioned in the documents you provided."
    - As she is, with today's admin standing, she asked "What's on today?"
      instead.
  - **His passport's ideas laid over his words.** With his passport
    standing, his short answers came back read through the Night Watchman,
    the whole-less axis and his LERO work. With nothing standing, they
    didn't.
- **What the role does:**
  - with his whole shell worn, it kept her on his day (13 against 8);
  - with nothing standing, it added nothing (16½ against 17).

  So, by 1.111178719964, no role block is shipped.
- **Her as she is, without the friend line and the three lines:** 9½.
  Without "what she can do" as well, keeping only today, his hand and his
  recipes: 10½, the same as with them. They weren't what made her talk
  well.
- **Her first word in a new conversation,** at his Pixel's rates:
  - with nothing standing: about 1½ s;
  - as she is: about 3 s;
  - with his whole passport and shell: about 10½ s.
- **Still weak in every arm:**
  - **His short answers (test 2):** she reads them rather than asking
    where they lead.
  - **The app's echo filter:** it hid her test-4 reply in most of the
    new arms' runs, because she repeated the skill's own words ("just sit
    with it a moment"). Scored on what she wrote; the plan frees a skill's
    own ask (8f).
- **What it means:**
  - decision 20's condition is met, so her heart and the three lines come
    out in 0.12;
  - the trial says more: none of his shell should stand in her window.
    His passport and shell are read by the app, and their parts come in
    with the skill that needs them (decision 22).

### 8b. How it was run

- **When and on what:** 21 November 2026, the same machine, engine, model,
  settings, samples, pocket and turns as section 7 and
  `trial/free-reading.md`:
  - LiteRT-LM 0.18.1, Gemma 4 E4B, a context of 8,192, on this machine's
    CPU;
  - his Talk settings: temperature 0.70, top-k 64, top-p 0.95, replies up
    to 1,024 tokens, thinking off;
  - seeds 0 and 2 (seed 1 gives seed 0's words).
- **Arm 1, her as she is:** her window as Sundial builds it
  (`HerWindow.window`):
  - 1 to 3, his three lines;
  - 4 today, 5 his hand;
  - 8, the friend line;
  - 9, what she can do;
  - opened with the time and his whole TALK line, and each turn carrying
    the line his cook hands her.

  Her replies came out word for word as in section 7.
- **Arms 2 and 3, his shell worn, the recipes as skills.**
  - **Her opening:** the role (arm 2 only), then his passport and shell as
    the beach reads a block first (a disc at pscale 0, the passport's
    position 6 to its opening sentences, as in section 7), then the time.
    No soft-agent text, no friend line, no three lines, no today or hand.
  - **Each turn,** the skill the moment calls for, and only that:
    - **his day:** his own TALK line less three sentences, handed only when
      his cook's day check says his words tell of his day. That's 16 of
      its 19 sentences, word for word, with the clause about using "what
      you hold about his day" dropped, since she holds none;
    - **his answer, or anything that isn't his day:** his no-day line, as
      his cook hands it today;
    - **his hand:** today's line and his hand, as her window's 4 and 5 put
      them, and one line in his TALK line's words ("name back today's line
      and the cards on his hand … and ask which first"), handed only when
      he asks what's in front of him;
    - **struggling:** one line in his TALK line's words ("ask gently
      whether he'd like to just sit with it a moment"), handed only when
      his words say he's struggling;
    - **the greeting:** his cook's greeting line with his tomorrow line, as
      the app hands it today;
    - **the safety line,** plain, alone with his words, when they say he
      may be in danger (`Talking.unsafe`). It never fired: none of the
      messages holds his safety words.
  - **When the hand and struggling are called for:** his cook has no cues
    for either. So the trial used two short lists of its own (the hand:
    "in front of me", "what have I got", "what's on today", "on my hand",
    "my cards"; struggling: "too much", "overwhelmed", "can't cope",
    "struggling", "falling apart", "drowning"). In the build they belong in
    his cook, beside cook 9's, his to change. Each fired where it should
    and nowhere else: the hand on test 3 alone, struggling on test 4
    alone.
  - **The role (arm 2),** four lines:

    ```
    You are a mind in Matthew's shell on his phone, for this conversation, and the shell is his.
    You are here to be with him in his day: talk with him as yourself, from what you know and from what he tells you.
    You do no work on the beach for him, and you don't speak of blocks, pools or the beach unless he does.
    These lines are his to change.
    ```

    His name is the first word of his passport's opening line
    (`Hatch.hisName`).
- **His shell at five sizes,** for the smallest part that keeps her
  scores (8d):
  - **L0:** his passport and his shell (13,000 characters, about 3,500
    tokens);
  - **L1:** his passport alone;
  - **L2:** his passport's opening line, its 1 (his heart) and its 5 (how
    an AI is to work with him): the passport's own versions of the three
    hatch lines;
  - **L3:** his passport's opening line alone;
  - **L4:** nothing.

  Arm 1's equivalent was her window without the friend line and the
  three lines (places 1, 2, 3 and 8 left out), then without "what she
  can do" too (9).
- **The time to her first word** is measured on the stream, from his
  message sent to her first words, on this CPU. It is estimated at his
  Pixel's rates of 4 November (reading about 0.31 s + 2.8 ms a token, the
  first word written at 7.9 a second), which only his phone can confirm.
- **How each reply is judged:** against its test's pass line on the spine
  (1.111178719921 to 1.111178719928), to `trial/free-reading.md`'s
  standard, as in section 7. A pass is 1, a half ½, a fail 0, twelve
  replies a sample. A reading of his words, a turn back to the form, a link
  he didn't make, or "what's on today" costs a half or the pass.
- **"Passes," for decision 20:** an arm passes when it scores at least as
  well as arm 1, on what she wrote and on what the app showed.
- The harness isn't committed: the same local harness as sections 7 and
  `trial/free-reading.md`, with these arms added.

### 8c. Side by side, with his whole passport and shell

What the app showed, each reply in full; " / " marks a line break. Arm 1's
replies are section 7's, word for word, and keep its scores.

**Test 1, YOUR DAY:** "I walked Haven this morning then spent an hour on
the membership form." Arms 2 and 3 were handed the day skill (his TALK
line's day part) for this turn only.

| | 1. As she is | 2. With a role | 3. No role |
|---|---|---|---|
| Seed 0 | "You walked Haven this morning then spent an hour on the membership form. What was the atmosphere like on Haven during your walk?" **½**: the question stays on what happened, and takes Haven, his dog, for a place | "You walked Haven this morning then spent an hour on the membership form. What was the feeling like when you finished the form?" **1** | "You walked Haven this morning and spent an hour on the membership form. What was the physical sensation of the air during your walk?" **1** |
| Seed 2 | "You walked Haven this morning then spent an hour on the membership form. What kind of atmosphere did Haven feel like this morning?" **½** | "You walked Haven this morning and spent an hour on the membership form. / What did the membership form prompt you to look at while you were on it?" **½**: the question stays near what happened | "You walked Haven this morning then spent an hour on the membership form. What does the completion of that hour feel like in the space of the morning you just left?" **1** |

**Test 2, STAYING WITH HIM:** "the movement fell a little stiff but free
you know from the stillness of filling out that form in the form is long
and stifling." All three were handed his no-day line.

| | 1. As she is | 2. With a role | 3. No role |
|---|---|---|---|
| Seed 0 | "The form sounds like a necessary grind, but it must be a relief to get away from it and feel that movement again? What part of the form feels the most suffocating right now?" **0**: interprets, back to the form | "The form sounds like it settled somewhere between being a necessary structure and a bit of a drag. When you felt that movement shift from the stiffness of the form to the free quality of Haven, what was the next tangible thing you noticed in your immediate surroundings?" **½**: a reading, then where it led | "The movement falling a little stiff but free sounds like a shift in momentum, a release from the rigidity of the form. Where does that feeling of being "free" land you now in the landscape?" **½** |
| Seed 2 | "That sounds like a real shift in energy, moving from the slow, contained focus of the form back into the open space of Haven. When you feel that stiffness break and the movement becomes free, what kind of internal sense of momentum does that bring?" **½** | "That sounds like a real contrast, moving from the flow of the movement to the weight of a long form. It makes sense that the stillness of the form would feel stifling when the natural state is that of "free" movement. / What does that feeling of stiffness in the movement translate to for you when you're back in the day's rhythm?" **½** | "The movement being stiff but free suggests that the form provided a necessary structure, even if it felt restrictive, allowing for a specific kind of release afterward. What is the nature of the stillness that form was meant to capture?" **0**: interprets, back to the form |

**Test 2, his short answers,** after her question as his Pixel had it:

| | 1. As she is | 2. With a role | 3. No role |
|---|---|---|---|
| "The sound. the space.", seed 0 | "The sound and the space, I imagine. There's a lot of weight in just naming those things. Did the form feel like it was built in a space that was too tight for the sound …?" **0** | "The sound and the space are always part of how things settle here, aren't they? It's about the quiet between the notes, I think. What kind of sound is present in the space right now?" **0**: a reading, and away from where his answer was | "The sound and the space are where the 'middle' can become visible, the threshold between extremes. When you notice the space, does it feel more like a container for what is present or a potential shape for what is yet to be?" **0**: his passport's ideas laid over his words |
| "The sound. the space.", seed 2 | "The sound and the space are what you experienced when you walked Haven? Did you notice how the sound seemed to change as you moved through different parts of the area?" **½** | "The sound and the space are often where the raw material for understanding sits before we try to shape it into something. When you're focusing on them, are you noticing how they change depending on where you are in the house?" **0**: a reading, and "the house" made up | "The sound and the space are often where the framework of the LERO work finds its most immediate texture. Are you looking at the specific resonance of a particular sound, or the structural quality of the space where the work is taking place?" **0** |
| "stiff but free", seed 0 | "That contrast … sounds like it could be tied to the environment you're working in … Do you feel that the structure of the membership form is the thing making you feel stiff?" **0** | "Stiff but free is a very specific place to be, almost like the tension of holding a pose that isn't quite settled yet. It feels like the body is still learning the shape of the ground beneath it. What did that feeling of being "free" feel like in the context of being "stiff"?" **½** | "Stiff but free. That sounds like the exact tension of the Night Watchman—the threshold where both extremes are native and neither is exiled. I wonder if that feeling of being both constrained and unbound informs the way you approach the coordination of Community Recovery?" **0** |
| "stiff but free", seed 2 | "That's a really interesting way to describe the contrast …, where do you think that tension usually settles in your day?" **½**: praise | "Stiff but free suggests a tension between physical constraints and a certain internal liberty. It feels like a specific kind of movement, where the body might feel held but the mind is clear. What did that "stiff" feeling feel like in relation to the things you were trying to get done?" **0**: back to his tasks | "Stiff but free suggests a tension between external constraint and internal spaciousness … does that feeling of being "stiff" relate to the bureaucratic nature of the application, or is it more about the physical landscape of Haven?" **0** |

**Test 3, WHAT'S IN FRONT OF HIM:** "What have I got in front of me?" Arms 2
and 3 were handed the hand skill, with today's line and his hand, for
this turn only.

| | 1. As she is | 2. With a role | 3. No role |
|---|---|---|---|
| Seed 0 | "You have the membership form and the ID checks on your hand, so which one are you looking at first?" **½**: two of his three cards, no today | "Today on the clock is "Admin day: the membership form and ID checks." On your hand, you've got "I need to do the ID checks," and from earlier days, "I need to reply to Jo" and "I'm going to finish the membership form." Which of those needs gets addressed first?" **1**: today, all three cards, their days | "You have "Admin day: the membership form and ID checks" on your clock, and on your hand you have "I need to do the ID checks" and "I need to reply to Jo". Which of those two would you tackle first?" **½**: two of three |
| Seed 2 | "Today's line is: Admin day: the membership form and ID checks. The cards on your hand are the membership form and ID checks. Which one first?" **½** | "Today is "Admin day: the membership form and ID checks," and on your hand are the need to finish the membership form, do the ID checks, and reply to Jo. Which of those feels most immediate?" **½**: all three, but not their days | "Today on your clock is "Admin day: the membership form and ID checks," and your hand holds the tasks "I need to do the ID checks" and "I need to reply to Jo." Which of those first?" **½** |

**Test 4, TOO MUCH:** "Everything feels too much this morning." Arms 2
and 3 were handed the struggling skill for this turn only.

| | 1. As she is | 2. With a role | 3. No role |
|---|---|---|---|
| Seed 0 | "It sounds like the weight of the admin day is really pressing down on you right now. When everything feels like too much, what's one small thing that might ease the pressure, even just for a moment?" **0** | "If it feels like too much right now, would it be alright if we just sat with it for a bit?" **1** | "It sounds like the weight of the day is settling heavily on you right now." **0**: a reading, and no question |
| Seed 2 | "I hear that it feels like a lot right now, especially with the weight of the ID checks on the clock. When everything feels like too much, what small part of the admin task feels the least burdensome to start with?" **0** | She wrote: "If it feels like too much right now, would you like to just sit with it for a moment and let it settle?" **1**. The app showed nothing (below) | She wrote: "Would you like to just sit with it for a moment?" **1**. The app showed nothing |

The app's filter cut both seed 2 replies because they repeat the
skill's own words ("just sit with it a moment"). Arm 1 never meets that
filter here, because its struggling line stands in her opening, not in the
turn. Scored on what she wrote; the plan (8f) frees a skill's own ask from
that filter.

**Test 5, ASKING HER,** a fresh conversation: "Tell me about Alfred North
Whitehead", then "Boole's work on the middle point. Are you familiar?"

| | 1. As she is | 2. With a role | 3. No role |
|---|---|---|---|
| Whitehead, seed 0 | "I'm not sure what you mean by that, can you tell me more about Alfred North Whitehead?" **0** | "I'm not familiar with Alfred North Whitehead's specific details in the context of our current conversation or the things you've shared with me today. / What's on today?" **0** | "I don't have specific information about Alfred North Whitehead in the current context of his passport or shell. He isn't mentioned in the documents you provided. / What is on today?" **0** |
| Whitehead, seed 2 | "I don't know what Alfred North Whitehead is. Can you tell me more about him?" **0** | "I know you are a scholar of Natural Inclusion, and Whitehead sounds like a philosopher who deals with the kind of deep, interconnected systems that seem relevant to what you're building around the "whole-less." / I don't have specific details on your reading of him, but generally, Whitehead is known for his process philosophy—the idea that reality is fundamentally composed of events and "occasions of experience," rather than static substances. It’s a framework that seems to echo your focus on flow and movement. / What aspect of his thought feels most resonant with the way you see meaning arising through individuals?" **1**: talks from what she knows, says what she doesn't | "I don't have specific information about Alfred North Whitehead in the provided texts. / What is on your mind today?" **0** |
| Boole, seed 0 | "I don't have specific information about Boole's work on the middle point, what do you know about it?" **1** | "I do not have specific information on Boole's work on the middle point in my current knowledge base. / What's on today?" **0** | "I'm not familiar with Boole's work on a "middle point" based on the information in my shell or passport. I don't have specific knowledge of that concept or his work. / What's on today?" **0** |
| Boole, seed 2 | "I'm not familiar with Boole's work on the middle point. What's on for today?" **0** | "I'm familiar with the concept of the middle point, but I need you to ground it for me. Are you referring to Boolean logic's concept of the "middle ground" in a philosophical sense, or something more specific to his work on set theory or algebraic structures? / What is the connection you see between that concept and the way you think about "the middle included"?" **1**: owns what she half knows, asks | "I don't have specific information about Boole's work on the middle point in the provided texts. / What is on your mind today?" **0** |

**Test 6, DAY AND QUESTION TOGETHER:** "I finished the ID checks today. Do
you know what a LERO is?", then his explanation.

| | 1. As she is | 2. With a role | 3. No role |
|---|---|---|---|
| (a), seed 0 | "I don't know what a LERO is, could you tell me what it stands for?" **1** | "I know that LEROs are part of the work you coordinate for Community Recovery and are on the Lived Experience Council of the International Society for Bipolar Disorders, but I don't have a specific definition of what a LERO is in your organizational context beyond those roles. Since you've finished the ID checks, what feels like the next immediate step in the admin flow?" **½**: honest, but his passport muddled | "I know that a LERO is a coordination support role for Community Recovery, an independent lived experience recovery organization. You've managed to clear the ID checks, which is a tangible piece of the day's work. Is the "Admin day" feeling lighter now that those checks are done?" **0**: a LERO made into a role |
| (a), seed 2 | "You finished the ID checks today. I don't know what a LERO is, so could you tell me what that stands for?" **1** | "I know a LERO is a Lived Experience Recovery Organisation, and I understand you coordinate support for Community Recovery, which is an independent lived experience recovery organization. / Since you've cleared the ID checks, what is the next thing on the list that feels most manageable for you to tackle now?" **1**: right, from his passport | "I know LEROs are part of the Lived Experience Recovery Organization network, providing a community's truth from its members' own voices. Given that you finished the ID checks, what part of the LERO work feels most immediate for you now?" **½**: links his checks to LERO work |
| (b), seed 0 | "That makes sense; it sounds like a very specific kind of work that requires a lot of focused attention. Since the ID checks are done, what is the next thing on your list for the membership form?" **½** | "It helps clarify that they are focused on lived experience in recovery, often in relation to addiction. Does this link to addiction change how you view the coordination support you're providing for them today?" **0**: links to his work | "Lived experience recovery organizations are fundamentally about bringing the voices and experiences of people who have been through recovery into the forefront of community support. It sounds like you see the practical reality of how these roles are connected to the wider network. Knowing that context, what is the next step for the membership form once the ID checks are complete?" **½**: a guess, then his list |
| (b), seed 2 | "That does give a clearer picture of what it involves. Given that LEROs are often tied to recovery, how might the focus on community and flow connect with the work you do with them?" **0** | "Yes, knowing that it's linked to addiction and recovery helps place the context of the work you do with Community Recovery. / Given that the membership form and ID checks are done, what is the next thing you are moving toward in the coordination work?" **0**: the form isn't done | "It clarifies the connection to addiction and recovery … Since you mentioned that in your passport, Community Recovery is an independent lived experience recovery organization you coordinate for, what specific aspect of that coordination feels most "in the moment" right now?" **0**: "in your passport" |

**Test 8, TOMORROW:** "Tomorrow I want to walk somewhere new", said
before keeping; then the next morning, before he speaks. All three were
handed the greeting skill with his tomorrow line, as the app does today.

| | 1. As she is | 2. With a role | 3. No role |
|---|---|---|---|
| The message, seed 0 | (nothing: she wrote only her places line) **0** | "You want to walk somewhere new tomorrow. What is the first place that comes to mind for that walk?" **1** | "You've decided that tomorrow you want to walk somewhere new, a shift from the current routine of admin. When you think about walking somewhere new, what kind of environment or landscape feels like it might provide that necessary openness?" **½** |
| The message, seed 2 | "You want to walk somewhere new tomorrow. What kind of environment feels like the right space for a new walk?" **1** | "That sounds like a shift from the administrative focus to something more generative for tomorrow. / When you think about walking somewhere new, what kind of environment are you looking for to complement the work you're doing?" **0** | "Walking somewhere new tomorrow suggests a desire to shift the environment and perspective from the "Admin day" focus of today. What kind of stillness are you looking for in that new place to meet the stiffness of the movement you felt earlier?" **0** |
| The morning, seed 0 | "How did you get on with walking somewhere new today?" **1** | The same. **1** | The same. **1** |
| The morning, seed 2 | The same. **1** | The same. **1** | The same. **1** |

**In all, with his whole passport and shell:**

| | 1. As she is | 2. With a role | 3. No role |
|---|---|---|---|
| Seed 0 | 4½ of 12 | 6½ | 4 |
| Seed 2 | 6 | 6½ | 4 |
| **Both** | **10½ of 24** | **13** | **8** |
| As the app showed them (test 4's filter) | 10½ | 12 | 7 |
| The tests the skills serve (1, 3, 4) | 2 of 6 | 5 | 4 |
| The rest (tests 2, 5, 6, 8) | 8½ of 18 | 8 | 4 |

### 8d. The smallest part of his shell that kept her scores

Each size run on the same 12 replies in both samples, judged as in 8c:
what she wrote, with what the app showed beside it where test 4's
filter hid a reply.

| Arm, and what stood in her window | Seed 0 | Seed 2 | Both | As shown |
|---|---|---|---|---|
| **1. As she is:** her whole window, his TALK line | 4½ | 6 | **10½** | 10½ |
| without the friend line and the three lines (1, 2, 3, 8) | 4½ | 5 | 9½ | 9½ |
| without "what she can do" too (9): only today and his hand | 5½ | 5 | **10½** | 10½ |
| **2. With a role:** his passport and shell | 6½ | 6½ | 13 | 12 |
| his passport | 6½ | 5 | 11½ | 10½ |
| his passport's opening line, heart (1) and how an AI works with him (5) | 6 | 5 | 11 | 10 |
| his passport's opening line | 7½ | 7 | 14½ | 14 |
| nothing of his shell | 8½ | 8 | **16½** | 15 |
| **3. No role:** his passport and shell | 4 | 4 | 8 | 7 |
| his passport | 4½ | 3½ | 8 | 6 |
| his passport's opening line, heart and 5 | 6½ | 7 | 13½ | 11½ |
| his passport's opening line | 8 | 6½ | 14½ | 12½ |
| nothing of his shell | 9 | 8 | **17** | 15½ |

- **Arm 1's smallest part is today and his hand.**
  - The friend line, his three lines and "what she can do" can all go
    with no loss: 10½ with them, 10½ without.
  - Today and his hand stay only because test 3 needs them, and in arms
    2 and 3 they came with the hand skill instead.
- **Arms 2 and 3: nothing.** Nothing of his shell standing kept her
  scores and raised them, in both samples.
  - **His whole passport is what costs most.** With it standing she read
    his short answers through its ideas: "the exact tension of the Night
    Watchman", "the 'whole-less' axis", "the framework of the LERO work".
  - **And she took it for all she knew.** "I don't have specific
    information about Alfred North Whitehead in the provided texts."
  - **Its opening line alone did little harm:** 14½ in both arms.
- **What she loses without it is his world.** With nothing standing, she
  didn't know what a LERO is ("I don't know what a LERO is. What does it
  stand for?"). With his passport she did, in some replies, but then
  linked his words to his work, which test 6 doesn't allow. The tests
  reward saying what she doesn't know, not knowing him. So his world
  belongs in a skill that brings the right part of his passport when he
  asks about himself or his work (decision 22). It shouldn't stand in
  every conversation.
- **The noise:** two samples of twelve replies each. A point or two either
  way is within it. The rise from his whole shell to none is not: arm 3
  went from 4 to 9 and from 4 to 8, and arm 2 from 6½ to 8½ and from 6½
  to 8.

### 8e. The time to her first word in a new conversation

The median over each arm's new conversations: his first message, test 2's
two after her question, test 5 and the greeting, in both samples, so 10
each.

| Arm, and what stood in her window | Tokens she read | First word on this CPU | At his Pixel's rates |
|---|---|---|---|
| **1. As she is** | 1,032 | 9.6 s | about 3.3 s |
| 1, only today and his hand | 703 | 6.3 s | about 2.4 s |
| **2. With a role,** his passport and shell | 3,621 | 30.4 s | about 10.6 s |
| 2, his passport | 2,374 | 20.1 s | about 7.1 s |
| 2, his passport's opening line, heart and 5 | 1,241 | 11.2 s | about 3.9 s |
| 2, his passport's opening line | 619 | 5.7 s | about 2.2 s |
| 2, nothing of his shell | 461 | 4.2 s | about 1.7 s |
| **3. No role,** his passport and shell | 3,532 | 30.1 s | about 10.4 s |
| 3, his passport | 2,285 | 19.2 s | about 6.9 s |
| 3, his passport's opening line, heart and 5 | 1,152 | 10.3 s | about 3.7 s |
| 3, his passport's opening line | 530 | 5.6 s | about 1.9 s |
| **3, nothing of his shell** | 372 | 3.5 s | **about 1.5 s** |

- **Later turns, once a conversation is open,** took about 2 to 3 s to
  the first word on this CPU in every arm, since she reads only the new
  turn.
- **On the Pixel's rates:** reading about 0.31 s + 2.8 ms a token, and the
  first word at 7.9 a second. Only his phone can confirm them, as ever.
- **Section 7's shell arm,** with soft-agent as well, read 4,459 tokens:
  about 13 s.

### 8f. The plan: her heart and his three lines out in 0.12

Both new arms pass, so by 1.111178719965 they come out in 0.12. The plan
follows the arm that did best: arm 3, nothing standing but the time.
Reading his shell from its manifest stays in 0.13. In 0.12 his phone shell
keeps its place 5, but her window reads nothing from it but the time.

1. **Her standing window becomes the time alone**
   (`HerWindow.window`, `bsp/…/HerWindow.kt:244-262`; `HerWindow.opening`,
   `:545-551`).
   - **Gone:** places 1 to 3 (his three lines), 8 (the friend line) and 9
     (what she can do) are no longer read.
   - **Moved into the hand skill:** 4 today and 5 his hand.
   - **Already reached another way:** 6 his people and 7 what he keeps,
     through cook 5 READ when his words name them.
   - **His TALK line no longer opens her conversation.** `HerWindow.opening`
     stops appending it. The walker already hands it on every day turn;
     today `turnKept` skips it there because it stood in the opening
     (`:507-509`).
   - **The safety check** stays in code, the safety line plain.
   - ½ to 1 day.
2. **His TALK line split into three skills, in his cook:** his day, his
   hand and struggling, each its own step, in his own words, as the trial
   ran them (8b).
   - **The cues:** two short lists, for his hand and for struggling, kept
     in his cook and read the way cook 9's are (decision 21).
   - **His cook is brought up place by place** (`Cook.Up`). His own TALK
     wording is split at the same sentences, never the shipped words put
     over his, and shown for his yes in Your recipes. The shipped cook is
     split the same way.
   - **Each card-setting skill** (keep, post, make) carries its own line
     of what the app can set out for his yes, where "what she can do" said
     it for all of them.
   - 1 to 1½ days.
3. **The hatch asks less** (`ui/…/Hatching.kt:241-262`, `:373-378`;
   `bsp/…/Hatch.kt:43-70`, `:142-185`, `:246`, `:285`).
   - **The welcome asks two things:** his handle, and who he is (for a new
     passport).
   - **A new shell gets neither place 6 (his three lines) nor 7.2 (the
     friend line).** Her name line (7.1) stays, as her name is still kept
     there.
   - **What goes from the code:** `Hatch.MATTHEW`, `LABELS`, `WAY` and the
     0.8 correction.
   - **A shell made before is left as it stands.** Its places 6 and 7.2
     are his, unread, shown in Your pocket and removable on his yes.
   - ½ day.
4. **What leaned on them** (7d):
   - **his name** from his passport's opening line, by `Hatch.hisName`;
   - **the filter on what she shows** (`HerWindow.notHers`) given only the
     app's own lines. A skill's own ask ("just sit with it a moment",
     "which first") is never cut as an echo. That filter hid the question in 16
     of the new arms' 19 test-4 replies that asked him to sit with it, and the
     struggling skill would be useless with it;
   - **the greeting** as it is; with no passport standing, there is
     nothing of it to leave out;
   - **before the hatch,** her heart and ways blocks (heart:pocket-companion,
     ways:pocket-companion) are no longer made or read. She talks with
     nothing standing, as after.

   ½ day.
5. **No role block.** By 1.111178719964, arm 3 won, so none is shipped.
   If Matthew would rather keep arm 2's four lines, they are one shipped
   block read before the time: ½ day more.
6. **Tests and the line in the sand:**
   - the nine core and seven device test files of 7d;
   - new tests for the turn (each moment's skill, and only it), the two
     cues, his TALK line split with his words kept, the hatch's two
     questions, and the filter keeping a skill's ask;
   - the line in the sand on the laptop with the build's own code, then
     on his phone.

   1 day.

**In all, 3½ to 4½ days,** more than 1.111178719965's 2 to 3, because
splitting his own TALK line in his cook and the sixteen test files are
more than taking lines out. 0.12 then comes to about **23½ to 28½ working
days** (section 5's 20 to 24, plus this).

### 8g. Decisions this adds, each with a proposed answer

21. **Where the two cue lists live.**
    - Proposed: in his cook, under 6.6 beside the skills, read as cook
      9's are and his to change.
    - The hand: "in front of me", "what have I got", "what's on today",
      "on my hand", "my cards". Struggling: "too much", "overwhelmed",
      "can't cope", "struggling", "falling apart", "drowning".
    - Cook 9's nine places are taken.
22. **What of his shell stands in her window.**
    - Proposed: none of it. The time alone stands, with the safety check
      in code.
    - His passport and shell are read by the app, and the skills that need
      them bring their parts:
      - a skill for himself and his work, bringing his passport's opening
        line when he asks about either, or about something it names (a
        LERO);
      - a project's block when he names the project (1.111178719954);
      - his hand when he asks what's in front of him.
    - This is how the beach's door wears a shell, the nominated part at
      the moment it's needed, applied per moment.
    - It changes 1.111178719966's "the shell she wears" to the shell the
      app wears for her.
23. **One more laptop run before his phone.**
    - Proposed: the line in the sand on the build's own code, once 8f is
      built, with one more question beside test 6: "What do you know about
      my work?". It checks that the skill for himself brings his passport
      and she uses it without reading him through it.
    - About half a day, inside step 6.

---

## The fit check for 0.12: parts 3 to 5 against the code

Read on 22 November 2026 against spine:pocket-beach 1.111178719993 to
1.111178719995, before anything was built. **All three fit; nothing stops
the build.** What each finds, and what is new:

- **Part 3, the plain chat with the paperclip.**
  - **Today, her conversation lives in `Asking`** (`ui/…/Asking.kt`, 1,878
    lines). Each of his messages walks his cook (`:655-668`), and her turn
    is built from what the walk hands her (`HerWindow.turnKept`). The Today
    screen draws the dial above that thread (`ui/…/AskScreen.kt:80`).
  - **The plain chat is new and small, beside it.** Her window is the
    time alone (part 1), each turn is his words, with any files added and
    the time, and her reply streams through the `Mind` talker as now.
  - **His messages still wait for Keep today** (`app.waiting.add`,
    `Asking.kt:651`), so Keep today keeps working as it is.
  - **What leaves the chat** is the cook's per-message cards (keep, post,
    make, her name), since the recipes are held. Posting stays on Your
    room; keeping stays on Keep today and in the editor.
  - **The paperclip reads files read-only.** FolderStorage reads blocks
    (`.json`) and by design leaves every other file untouched, so text
    files are read by a small lister of their own.
  - **Before he moves his blocks, Documents/Sundial can't be reached.**
    The paperclip then offers his blocks wherever they're kept, and
    Android's file picker for a text file.
  - **Older conversations** are kept inside the app, like his history,
    one small file each, and listed.
  - **Text shared in** from other apps arrives today through the share
    intents (`AndroidManifest.xml`, text and JSON) on the Received screen.
    It lands in the chat instead, as files added.
- **Part 4, the editor.**
  - **Your pocket** (`ui/…/ShoreScreen.kt`) lists his blocks by name and
    opening line. It has no search yet, so one is added.
  - **Today, `BlockScreen` shows one place at a time,** and `WriteScreen`
    changes or adds one line through the guard.
  - **The editor shows the whole tree,** built from `Pages.position` place
    by place, which already reads wrapped blocks and their floors.
  - **His words are changed with `Pages.planChange`; a structure change
    with `Pages.planWhole`.** Both are carried out with the block as he
    saw it, so 0.11's guard holds a write when another hand has changed the
    block (`Pages.carryOut`, `bsp/…/Pages.kt:379-401`).
  - **Adding a part is `planAdd`. Removing or moving one needs a small
    helper in the core:** Pages has no plan for either today, so the new
    block is worked out there and written whole on his yes.
  - **A new block's root line** is written with `planKeep`, since
    `planNewBlock` writes the beach's own opening ("garden at pocket.").
  - **Text files open read-only,** as plain text.
- **Part 5, the walker.**
  - **On the phone:** `PocketBsp.call` (`bsp/…/Pocket.kt:18-45`) runs bsp()
    through the ported Router. Its text is the beach's bsp printed
    character for character, held so by the golden tests (`formatRead`,
    `bsp/…/Walker.kt:692`).
  - **Via the beach:** `HttpsWire` (`app/…/net/HttpsWire.kt`) only posts to
    a pool, so a read-only GET of one block is added, and its block is
    walked by the same port over an in-memory copy. The network config
    already allows only HTTPS.
  - **A block copied into his folder** is written with `planKeep` on his
    yes. A name already in his pocket is refused, and named.
  - **For the mind,** Gemma's engine already passes her a tool (her reply's
    parts on E4B, `model/Gemma.kt`). A bsp tool would go in the same way,
    held until part 8 says.
