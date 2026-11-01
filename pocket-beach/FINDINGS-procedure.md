# Rockpool — findings before pscale as procedure

1 November 2026, for spine:pocket-beach 1.111178714 (read from 1.11117871
down; the spine was not written). Point 7 asks for findings first, written
to the repository, and a hold if any finding changes the design. These are
they. **No finding changes the design, so it is built.** A few places needed
a reading, and each is said below with the reading taken.

**What was read:**
- the plan, 1.111178714 and its nine points, with 1.1111787136 to
  1.11117871365 (the small release's items, his days, what Gemma brings);
- the beach's convention:
  - spine:code-as-blocks, chiefly 1.3 (blocks as actuators, three runs) and
    1.4 (the path, the rule at 1.34, procedures at 1.43, authority at 1.44);
  - cook:keel 1 (CONVENE) and cook:weft 7.5 (ADDRESSED EMISSION);
  - **function:cook does not stand on the beach** ("not found"), so the
    rule is taken from code-as-blocks 1.34 and 1.43 and from the two
    exemplars;
- the blocks his days touch: spine:p6, spine:now, his now:Phenomemental and
  ahead:Phenomemental as the beach holds them, and **function:ahead** (the
  hand's law, since spine:ahead was retired on 26 August);
- the app on main after the small release (2ad3446, 0.8.1);
- FINDINGS-hatch piece 3 (the walker) and piece 6 (what she hands back).

## The short version

- **The app's behaviour becomes three blocks in his pocket:**
  - `cook:<handle>`, his cook: the procedures, one a place;
  - `kernel:<handle>`: the app's events, each naming the procedure it runs;
  - her window, which already is one (his shell's place 5).

  The shipped copies sit on the teaching shelf, read only. They are copied
  into his pocket the first time the app opens after the update, listed in
  his shell's manifest, and walked exactly as they stand.
- **The walker is the only new code:** `Cook.kt` in the core, with a fixed
  set of verbs, each a sentence form it parses. A step it can't parse
  refuses the whole block, and the shipped copy runs instead, with one
  plain line saying so beneath her reply.
- **The app's lines to her move into the blocks,** word for word, as the
  steps that hand them to her. So the lines he reads in his cook block are
  the ones she is given, and changing one changes her next turn.
- **The words the app listens for become place 9 of his cook,** one line
  for each answer. Adding "send … to my room" to the post line is an edit,
  not a release.
- **What stays code** is authority at the actuator:
  - the guards, and a whole copy before any change to his day;
  - the hatch;
  - the writing check, for a change to one of his lines only;
  - her talk itself.
- **His days are carried the beach's way, as function:ahead now has it.**
  That is not the five shelves the phone has used. A dated intent is a
  booking at its day in his now; other intents go to his hand, ahead, in
  piles by grain. Below, under "His days".
- **Her ring choices go through the engine's own tool call.** On the laptop
  it was right far more often than a bare digit on the same prompt (piece
  9 below). Her digit is still all that is taken.

## 1. The convention, and how the phone carries it

The beach's rule, kept exactly:
- **What a procedure is.** A cook block's place is one procedure. Its
  opening line says what it does, its inputs and its order. Its children are
  the steps, each a sentence.
- **Who performs a step.**
  - A step a machine performs carries its formula in the sentence.
  - A step that **names "she"** is hers: a ring choice of at most nine, her
    digit taken and her words never shown.
  - A step that **names "his yes" or "his tap"** waits for him.
  - The walker performs every other step.
- **Questions:** a step that asks a question has its answers beneath it,
  as cook:keel 1.2 has 1.2.1 and 1.2.2. Yes is 1, no is 2, and ANSWER has
  a ring of answers taken in order.
- **Carrying:** a step names what it carries forward in capitals (THE
  WORDS, THE PLACE), and later steps use it.
- **Every procedure ends with ACCOUNT.**

**Readings taken:**
- **Where ACCOUNT lands.** On the beach it is one history line per run. On
  the phone, a conversation is one history entry (0.8's decision 4). So
  each ACCOUNT is one line of what was done, and the close writes them all
  beneath its entry at 2. Nothing else writes to his history.
- **"A step rewritten is a second version beside the first, never an
  overwrite" (code-as-blocks 1.43).** The Write screen changes a line in
  place, so on the phone the earlier versions stand elsewhere:
  - the shipped copy stays on the teaching shelf;
  - the app's copies keep each earlier version of his cook as it stood
    (`Copies.kt`, as for every block).

  Nothing is lost, and the bench can lay the two side by side.
- **Which reader takes a step is set by the block's own words,** as the
  plan says, not by its geometry alone. A ring beneath a "she" step is the
  choice offered her. A ring beneath a question is its answers.

## 2. Where the blocks live

- **The shipped copies** are `cook.json` and `kernel.json` on the teaching
  shelf (`pocket-beach/shelf/`), packed into the app as the other shelf
  blocks are. The trial reads the same files, so **the bench runs the blocks
  the phone runs, never copies.**
- **At first run after the update** the app copies them into his pocket as
  `cook:<handle>` and `kernel:<handle>`, and adds both to his shell's
  manifest. A whole copy of the pocket is saved first, as before any
  change.
- **A later release with better procedures** brings his cook up to date
  only if he hasn't changed it: the app compares it with the copy it
  shipped last. A cook he has changed is his, and stays.
- **Before the hatch** there is no handle and no pocket of his to keep a
  cook in, so the app talks as 0.8.1 does until he hatches.

## 3. The walker's verbs

A fixed set. Each is a sentence form the walker parses; anything else is
refused.

| Verb | The sentence | What the walker does | Replaces |
|---|---|---|---|
| **Ask** (a question with answers beneath) | "Which does THE MESSAGE ask for, by the words at cook 9?"; "Are there words of his in THE WORDS?"; "Is there THE PLACE?" | Takes the answer: the first ring answer whose words at cook 9 match, else the last; or 1 when the named value holds something, else 2 | `Keeping.way`, `Keeping.asks` |
| **Lift** | "Lift his words from THE MESSAGE: after the colon; else in quotes; else his sentences with the asking at cook 9.8 taken off; else what he said before. Carry them as THE WORDS." (the sources, in the order the sentence gives them) | Lifts exactly as named, in that order | `Keeping.keptWords`, `postWords`, `made`, `name` |
| **Find** a block | "Find the block THE MESSAGE names …. Carry it as THE PLACE." | "My passport", "my stash", "my shell", "my history" are his own; else one of his blocks by its name; never the app's own | `Keeping.named`, `HerWindow.hisFour` |
| **Read** | "Read THE BLOCK at its aperture: whole when it is 1,500 characters or fewer, else as a ring of its places by their opening lines. Carry it as THE READING." | Reads through the phone's bsp | `HerWindow.readFor` |
| **She chooses** | "She chooses … from the ring beneath; on E2B, or when her answer isn't whole, the ring goes to him. Carry the choice as THE PLACE." | A ring of at most nine, through the engine's tool call; her digit only | the Write job's placement (`Talks.ask`) |
| **Hand her** | "Hand her this line: “…”", or "Hand her THE READING with this line: “…”" | The app's line in her next turn, his words as data beneath | `HerWindow.appLine8` and its lines |
| **Set out** | "Set out the keeping of THE WORDS in THE PLACE beneath her reply, for his yes." Also a post, a new block, her name, his day, or the card "Sort as my day". | A card beneath her reply; the walker waits for him | `Asking.byApp`, `postOut` |
| **On his yes / tap** | "On his yes, write it."; "On his tap, send it." | Carries the card out through the guards; sends his post | `Asks.carryOut`, `Asking.post` |
| **Cut** | "Cut THE TELLING into its sentences. Carry them as THE PIECES." | His sentences, as `Telling.sentences` cuts them | `Telling.cut` (30 fragments) |
| **Run** | "Run cook 2 KEEP." | Walks that procedure, then comes back | the `when (way)` in `Asking.ask` |
| **Account** | "ACCOUNT. Account what was kept, and where." | One line of what was done, for the close | — |
| **Stop** | "Stop." | Ends the walk | — |

**The trace.** The walker records each step it takes, as the film strip:
- the address;
- who took it (the walker, she, he);
- what it carried.

With Details on, the trace shows beneath her reply. It also goes in the
saved conversation, and is what the proofs compare.

## 4. Which Kotlin each procedure replaces

| Procedure | Its steps, in short | The Kotlin it replaces |
|---|---|---|
| **1 ANSWER** | Which does his message ask for, by the words at cook 9: HER NAME, POST, MAKE, TELL MY DAY, KEEP, READ, CHANGE, else TALK | `Keeping.way` and the `when (way)` of `Asking.ask` |
| **2 KEEP** | Lift his words; none, ask him; hand her the write line; a block named, else she chooses from the ring (his stash first, then his other blocks); set out the keeping for his yes; write; account | `Keeping.Way.Where`, the stash default, `byApp`, `WRITE_LINE` |
| **3 POST** | Lift his words (after the colon, else in quotes); none, she asks what he'd like to say; hand her the post line; set out the post for his tap; send; account | `Keeping.postWords`, `postOut`, `POST_LINE`, `postLine` |
| **4 MAKE** | Lift the block's name and his words; no name, ask; hand her the write line; set out the new block for his yes; write; account | the Write job's `new …` proposal |
| **5 READ** | Find the block (his four by "my"); read it whole, else as a ring of its places; the place his words point to read whole, else she chooses one; hand her what was read; account | `HerWindow.turn`'s reading of every block his words name, `readFor` |
| **6 TELL MY DAY** | Lift the telling; cut it into sentences; she chooses each sentence's portal from spine:p6's ring of six; intents found by the words at cook 9.9 go to his hand at their pile, or to his now at their day when dated; rungs named go to their level; set out the whole for his yes; write it, a whole copy saved first; account | `Telling.sorted`, `Landing.withDay`, `Telling.land`, the six flat places |
| **7 CLOSE** | Append one entry to his history: the concern line, what was read at 1 and what was done at 2 | `Close.plan` (its concern line stays the formula) |
| **8 HER NAME** | Lift the name; if his shell already gives it, hand her the name-she-has line; else hand her the name line, set out her name in his shell for his yes, write, account | `Keeping.Way.Name`, `Hatch.nameAction` |
| **9 THE WORDS THE APP LISTENS FOR** | One line for each answer, and two more: the asking taken off his words, and what he means to do | the regexes at the top of `Keeping.kt` |

**TALK and CHANGE are answers of ANSWER, not procedures of their own:**
- **TALK** hands her the talk line.
- **CHANGE** hands her the line that her own lines are his to change, when
  they are what he asks to change; otherwise it hands the message to the
  writing check.

## 5. What stays code

- **The guards** in `Acts.propose` and `Asks.carryOut`:
  - the model never writes;
  - nothing is written without his yes;
  - nothing leaves the phone without his tap, and only his own words;
  - her shell's and the app's protected blocks stay protected;
  - the teaching shelf stays read only.

  An edited block can't switch them off, since they hold whatever the
  block says.
- **A whole copy before his day changes,** and before his cook and kernel
  are first copied in.
- **The hatch, the corrections and the move out of `telling`:** each runs
  once, at opening (the kernel's place 4 says so).
- **The safety line** beneath her reply when his words say he isn't safe.
- **The writing check,** only for a change to one of his lines. It is the
  one ask left to it; keep, post and make no longer touch it.
- **Her talk:** the conversation, her window read once, the turn.

## 6. Steps the convention can't carry, or carries only with a reading

- **The words his message is matched against.** The gate's regexes had
  look-aheads ("write … down", "put … in") and a rule that a question asks
  only when it says so plainly. The cue lines carry these as phrases where
  "…" stands for up to five of his words. A clause asks when it begins
  with one of the words, once the asking ("could you", "please") is taken
  off. The 0.8.1 cases all still hold (see the core tests).
- **"Her choice."** A ring given to her is a separate short call through
  the engine's tool call. Her talk is never the place her choice is read
  from.
- **When she talks.** Her reply is given when the walker first waits for
  him, or ends, with every line handed to her so far. A step that is hers
  is served at once. So for KEEP on E4B her ring choice comes before her
  reply: one short call. On E2B the ring goes to him as the "Where shall
  this go?" card.
- **The concern line and the day's arithmetic** (sundial addresses, the
  pile a time word sizes) are formulas the walker computes. The sentence
  names them; it can't spell out a calendar. The formula is the sentence's
  name for code that is tested against the beach's own: `Temporal.kt` and
  `Whens.kt`.
- **The writing check** for a change to his lines stays a model call. It
  needs to find a line by meaning, and the ring for that (a block's lines,
  at most nine) is the next procedure to write.

## 7. His days, the beach's way

**What the beach holds:**
- **now:Phenomemental** is one clock. Each day's line sits at its date
  (eight digits), and the rungs above sit at their coarser levels: the
  week at seven digits, the month at six, the season at five, the year at
  four. Nothing else is in it.
- **function:ahead** (rewritten 26 August; spine:ahead was retired then)
  says his hand is `ahead:<handle>`:
  - **piles by rung:** 1 within hours, 2 today, 3 this week, 4 this month,
    5 this season, 6 this year, 7 this decade;
  - **a card:** his sentence at pscale 0, its conditions beneath it at −1,
    stamped and never typed;
  - **a dated intent** "is not a card at all: it is a booking, voiced in the
    mirror at that day".
- **spine:p6** is Presence with two rings: the surface at 1 (facts 1.1,
  feelings 1.2, fictions 1.3) and the deep at 2 (purpose 2.1, decision 2.2,
  outcomes 2.3).

**What the phone has:**
- a `now` block whose day holds the telling and, in a hidden directory, the
  six;
- a `telling` block holding the six names, a hand of five shelves
  (tomorrow, week, month, season, year) and four rungs.

**Read with function:ahead.** The plan says his hand becomes ahead, piles
by grain, sitting at the clock's levels. function:ahead's piles are the
clock's levels, so:
- an intent sized for this week goes to pile 3, this month to 4, and so on;
- "tomorrow" or "on Wednesday" names a date, so it is a booking at that day
  in his now, not a card.

The phone's shelf "tomorrow" has no pile of its own on the beach. This
follows the beach's law; it doesn't change the design.

**What TELL MY DAY writes, for his yes:**
- **in his now** (it keeps the name `now` in the pocket, since his window
  and manifest name it): the day's line at today's date (his telling, his
  words); a booking at its day; a rung's line at its level;
- **in `ahead:<handle>`:** each intent at its pile, its stamp beneath at 3;
- **in `p6:<handle>`, his mirror of the P6 family:** each sentence appended
  beneath the portal she chose. It carries its day's address at 2 and its
  arrival stamp at 3, so the walker can lay spine:p6 beside it and offer
  the portal's own questions.

**Nine at most.** A pile and a portal each hold nine, as every place does
(function:ahead: "a rung holds nine"). Past nine, the app says so beneath
the card and leaves the piece out. Folding a full place is the summary
job's (1.1111787134, step two), not this release's.

**The move out of `telling`** runs once, at opening, after a whole copy is
saved:
- the six from each day's hidden directory go to the p6 mirror, mapped to
  spine:p6's addresses:
  - "what happened" to facts, "what I felt" to feelings, "what I made of
    it" to fictions;
  - "what matters to me" to purpose, "what I chose" to decision, "what's
    ahead" to outcomes;
- each day's line stays at its date;
- the hand's cards go to their piles (a tomorrow card to pile 2, today, as
  the nearest pile);
- the rungs go to their levels in now;
- `telling` is retired: renamed `retired-telling`, which the hidden region
  keeps, never deleted;
- her window's place 5 is pointed at his ahead instead of the old hand.

**The cost:**
- On his phone at 20:16 the old sort cut one telling into 30 fragments,
  read 7,283 tokens and took 136 seconds.
- Cut by sentences, his telling of that length is about eight pieces. Each
  ring of six is one short call, about 170 tokens with no reply but a call.

## 8. His first evening on 0.8, and what answers each

The ask log (ask-2026-11-01-2016.txt) was read by Claude, not by Claude
Code. Its findings are taken from the spine at 1.1111787149.

1. **Who he is didn't reach her.** "Who am I, Gemma" got "You're the one
   I'm talking to right now", with his lines in her window. Her window now
   says whose each place is. Asked "what do you know about my film making",
   READ is what answers: "what do you know about my" is a READ cue.
2. **His lines became hers** ("If I've been tasked with managing the
   environment…"). Each of his places is labelled as his, in his words, not
   hers: "Who he is (his words about himself, not yours)".
3. **KEEP lifted the wrong words.**
   - "I meant Keep that for me please" lifted "I meant". "I meant" and "I
     mean" are in the asking at cook 9.8, so they are taken off, and with
     nothing left his words before are kept.
   - "Can you add in a little from my day today, my now?" is TELL MY DAY,
     since "my now" and "from my day" are its words. His telling is what
     he said before.
   - "Hold that for me" is heard: "hold that" is a KEEP word.
4. **A block named in passing derailed her.** The app no longer reads a
   block because a word in his message matches its name. Only READ reads,
   when his words ask.
5. **The day sort cut 30 fragments.** TELL MY DAY cuts by sentences, and
   each takes one ring of six.
6. **Posts** were the small release's, and stay as it has them (POST).
7. **She invents where she doesn't know** ("MCP" as "Mycobacterial Cell
   Protein…"). What she can do now says: "You can't look anything up: when
   he names a thing you don't know, say so and ask him; never guess what it
   stands for."
8. **Talk ran with a context of 4,096 tokens.** It is raised to 8,192 by
   default, his to change in Settings (up to 32,768). See piece 9 for what
   the laptop measured; the Pixel's own number is owed.
9. **Nothing she "held" was kept.** She can't keep anything herself. What
   she can do says so ("only what the app sets out beneath your reply and
   he says yes to is kept"), and "hold that" is now heard as keeping.

## 9. What Gemma brings that the app doesn't use (1.11117871365), measured on the laptop

These were measured on the laptop's CPU with LiteRT-LM 0.18.1, seed 0. The
replies are kept whole in `trial/gifts-<model>-seed0.md`.

| What | E4B | E2B |
|---|---|---|
| **A ring of six, her digit free** (eight of piece 6's ten words, the same prompt for both ways) | whole 6 of 8, in a right place 2 | whole 5 of 8, right 3 |
| **The same ring through the engine's tool call** (`choose`, place 1 to 6) | **whole 7 of 8, right 6** | whole 3 of 8, right 2 |
| **How much she holds:** an engine of 32,768 tokens | ready in 3.6 s | ready in 1.0 s |
| …given his whole passport (24,888 characters) and one ask | 169 s, a peak of 8.0 GB; she found place 6, "WHERE THINGS GO" | 133 s, 6.2 GB; she found place 6 |
| **A picture** (a screen of the chat, 1080 by 2400) | the engine with vision ready in 9.8 s; read in 11.5 s, 4.5 GB; it said what the screen showed | ready in 9.6 s; read in 9.4 s, 2.6 GB; right |

**What this decides for this release:**
- **Her ring choice goes through the tool call on E4B.** On the laptop it was
  right 6 of 8 against the free digit's 2 on the same prompt. Piece 6's
  free digit did better with a richer prompt, 8 of 10, but the tool call
  needs no parse at all.
- **On E2B the tool call was worse than the free digit,** so the ring goes
  to him, as decision 6 already has it.
- **Her whole passport fits** in a context of 32,768 on the laptop, but it
  costs minutes on a CPU and gigabytes. A ring of its places' opening lines
  costs about 190 tokens (0.8.1's judge: "Read my passport" read 753 tokens
  against 565 for a turn with no reading), and a place read whole about 940
  more. So the ring and the pointed place stay the way, and her talk
  context goes from 4,096 to 8,192 by default, which holds the window, a
  ring and a place with room for six exchanges.
- **She can see a picture,** on the CPU, in about ten seconds on the laptop.
  The Pixel's own seconds and memory are owed.
- **Her own ears weren't measured.** There is no speech on the laptop to
  give her: no recording of his voice, and nothing here speaks. It needs
  his voice and his Pixel, beside Android's recogniser.

**Owed on his Pixel, which Claude Code can't reach:**
- her own ears beside Android's recogniser, on his voice;
- a picture's seconds and memory on the Pixel's CPU;
- the talk context the Pixel holds with E4B on the GPU.

None of these changes this release; each waits for files and pictures.

## 10. The proofs (point 6), and how they are run

Each proof runs in the core's tests and on the bench, against the shipped
blocks from the teaching shelf, with one edit made to a copy:
- **(a) A cue line edited.** With "send … to my room" taken out of the post
  line, "Send a message to my room: back on Tuesday" is talk. Put back, it
  sets out a post of "back on Tuesday".
- **(b) The KEEP ring reordered.** With his stash first, "Keep that for me
  please" offers his stash first. Moved to third, after two of his blocks,
  his first block is offered first.
- **(c) A step rewritten.** With one step of POST rewritten (the line she
  is handed), the trace shows the new line, and her turn carries it.
- **The walker and a model read the same block.** A fresh Claude subagent,
  the kernel of code-as-blocks 1.3, is given each procedure and the same
  inputs. It walks the block and reports its trace in the walker's shape.
  The two traces are compared step by step. Where a step is hers or his,
  the same choice and the same tap are given to both, as keel and weft
  shared the window.

## 11. Rockpool, the name and the mark (1.1111787148)

- **The name:** the launcher label, the app's own title, About, and the
  release and its APK (`rockpool-v0.9.apk`). The tag is `rockpool-v0.9`.
- **What doesn't change:** the package id and the signing key, so it
  installs over 0.8.1 and keeps his blocks.
- **The mark:** Nine stones, as drawn at 20:07. It is an Android adaptive
  icon: the ink as the background, the pool, stones and point as the
  foreground, and a monochrome layer of the stones and point for themed
  icons. The ring of radius 29 with the largest stone half-width of 2.5
  stays within the 33 of the adaptive icon's safe circle. About is headed
  by the same mark.

## 12. What could go wrong

- **An edited block stops the app.** It is parsed when read; one that
  doesn't parse is refused, the shipped copy runs, and one plain line
  says so.
- **A cue line too wide hears talk as an ask.** The cards still wait for
  his yes or tap, so a wrong ask costs a tap, never a write.
- **Her ring choice on the phone is slower than measured.** It is one call
  with no reply but the tool call; the trace shows its seconds.
- **The move out of `telling` misplaces something.** A whole copy is saved
  first, and `telling` is kept in the hidden region, not deleted.
