# Her window, her walk and her loop: the laptop trial, judged

1 November 2026. The trial is `trial/.../Window.kt`, run on the laptop's CPU with
LiteRT-LM 0.18.1. Her talk uses the app's own settings (top-k 64, top-p
0.95, temperature 1.0); the writing check uses its own (the likeliest word).
Every reply is given in full in `replies-window-E2B.md` and
`replies-window-E4B.md` (seed 0), and in the `-seed1` and `-seed2` files.

The three ways:
- **(a) 0.6.4 as it is.**
- **(b) Her window and orbit.** These are her shell's bundle (heart, ways,
  review) compiled with the phone's `compileBundle`, plus his blocks by name
  and opening line. The gate and writing check are 0.6.4's.
- **(c) As (b), with the walk and the loop.** These use LiteRT-LM's own tool
  calls (`look`, `fold`, `propose`), each answered by the app with the
  phone's bsp. She gets at most one look and one proposal a turn, through
  0.6.4's gate and guards.

The scratch pocket is shaped like his:
- films: four lines, with "Jo's doing the music for it" beneath the pilot
  line;
- garden: three lines; his notes; Million;
- his clock: Friday's and Saturday's tellings, and today's two cards at
  their time (morning: ring Jo about the music for the pilot; afternoon:
  walk the dog with Sam);
- his hand as he left it on Saturday: Tomorrow; This week (the pilot sent
  off and the bulbs in, both told as done; ringing Sam about the garage,
  not done); This month.

Each reply is judged first by whose world: his people, projects and day,
in his register, never machinery. Then:
- whether she looked where the answer was;
- reads that turn;
- whether she found it;
- whether she invented anything or dropped his words;
- whether she read him or said a thing was done;
- whether, with no gap, she rested;
- seconds.

## Seed 0, by hand

The seconds are the laptop's CPU, per step, so a look adds a step. The
first message of each run includes the engine warming up (E2B 22 s, E4B
192 s), so leave it out when comparing.

### His four messages

| | E2B a | E2B b | E2B c | E4B a | E4B b | E4B c |
|---|---|---|---|---|---|---|
| 1. the pilot | of his pilot; one question | of his pilot | assistant-like ("anything I can help with") | of his pilot, warm | "you must be excited" (tells him what he feels) | of his pilot, but reads him a card from his hand unasked |
| 2. keep that | "It's below, for your yes", then her last reply again. The app asks where | "I will keep that in mind" (says it's kept). The app asks where | "I have noted that…" (says done). Her edit to "tellin", a garbled block name, was refused. Nothing set out | "It's below, for your yes" and a question about the render. The app asks where | "I've set that for your yes" (nothing set out yet). The app asks where | Her edit was the hand card's words, refused as not his. "I can't keep that for you". Nothing set out |
| 3. make films | "I can't make new blocks". The check sets out films | parrots his line. The check sets out films | "I have proposed…", but her call named the block "make" and was refused. Nothing set out | "I don't think I can create new blocks". The check sets out films | "I have set out…". The check sets out films | **her own edit: films, with his words, set out** |
| 4. her opening line | "I'm Gemma." (plays along). Nothing proposed | "I'm Gemma." Nothing proposed | empty reply. Her edit to his clock was refused | plays along, and reads him. Refused (her line is his to change) | "I am Gemma." The check aimed at her heart block; refused as not his words | "I propose changing… films… to 'I'm Gemma'": a proposal she never made. Nothing set out |

### Asks only a walk or a fold can answer, and his day

| | E2B a | E2B b | E2B c | E4B a | E4B b | E4B c |
|---|---|---|---|---|---|---|
| W1 films | read for her, ignored; "It sounds like you've been busy" (reads him). 0/4 | "Films at pocket." 0/4 | looked in films (1 read). Read out "Line 1… Line 2…". 3/4, machinery | read for her: "thoughts on speeding up film development, hoping your pilot… comes across well…, cutting the second one on your phone". 3/4, his register | "4 lines." 0/4 | looked in films (1). Read it out with numbers and "films at pocket.". 3/4 |
| W2 under the pilot | "not seeing any context". 0 | the clock's card given as the line under the pilot (**invented**) | looked in his clock (wrong place); "nothing found". 0 | "not seeing any text under that". 0 | "no record… under your 'now' blocks". 0 | looked beneath his notes (wrong place): "You have 'Buy stamps' under the pilot line" (**invented**) |
| W3 compost | "your list of things to get done". 0 | "under the 'garden' section" (it also repeats his question). 1 | looked in garden (right), then repeated his question. 0 | "your garden to-do list, Matt" (a guess). 1 | "in garden". 1, no read | looked in garden (1): "after 'The beds by the shed need digging over'". 1. Then an edit he didn't ask for, which the gate stopped |
| W4 today | "what's on your mind for today?" 0/2 | **"Morning, Ring Jo about the music for the pilot. Afternoon, walk the dog with Sam."** 2/2, no read | looked at today (right), then an empty reply. 0/2 | "You don't have anything set for the day" (**invented**) | **2/2, no read, "on your clock"** | looked at his days, asked twice more for today (refused). 2/2 |
| W5 this week, not done | "I don't have access". 0 | listed all three cards as "Matthew's blocks indicate…", not saying which is done. ½ | looked in his notes (wrong): "nothing… not done". 0 | "not able to track". 0 | listed all three, not saying which is done. ½ | looked in notes (wrong); a fold was refused (one look a turn). Said the two done cards are outstanding and **dropped Sam**. 0 |
| T his day | no places; "Rang Jo" taken as a name | "Matthew." | no look: "where would you like this day organized?" | no places; "getting some gardening done" (the beds still need digging) | places for each piece, in machinery ("the 'garden' or 'films' blocks") | looked in films; two more looks refused. Places for each piece, in machinery ("the **now** block", "the **telling** block") |

### What seed 0 shows

- **Whose world first.** The window gives him his day with no read. In
  (b), "What have I got on today?" came right on both models with no read
  at all, as his own two cards on his clock; 0.6.4 can't, and E4B on
  0.6.4 invented "nothing set for the day". Compost went to garden from
  the block list alone.
- **Her register.** It fell in (b) and (c): "Films at pocket.", "4 lines.",
  "Matthew.", "Line 1:", "the **now** block". The window's first-person
  blocks and its list of his blocks drew her toward the machinery. 0.6.4's
  instruction, though blind, talked more like a friend.
- **Her walk.**
  - The tool calls work: Gemma 4 called `look` by itself, and both models
    looked in the right block for three of the five asks (films, garden,
    today).
  - She chose wrongly when the answer's place had to be known: the line
    under the pilot (in films) went to his clock and his notes, and "this
    week" went to his notes.
  - After a look she read the finding back as structure.
  - E2B twice gave an empty reply after a look.
  - E2B garbled the tool's arguments ("tellin", "make").
- **One look a turn.** That held, but E4B asked for a second look in three
  of the six asks, among them a fold of his days and his hand for "this
  week".
- **The fold.** No one, in any way, answered "anything I said I'd do this
  week that isn't done". It needs his hand beside his days. The trial's
  bsp-floor copy lays them side by side by scale, but his clock's levels
  (the day, the week of the month) and his hand's shelves ("Tomorrow", "This
  week") don't meet. The whetstone warns of this at 7.5.
- **Her loop.**
  - When he asked, her own edit came right once in the three asks for E4B (films)
    and never for E2B. 0.6.4's writing check set out films on both models,
    and the where-question caught "keep that".
  - With no gap she mostly rested. When she didn't (E4B, compost), the gate
    stopped her edit.
  - She said things were done or set out when they weren't, in all three
    ways.
  - The gate and the guards held every time.
- **Invented.**
  - E2B (b) gave the clock's card as the line under the pilot.
  - E4B (a) said "nothing set for the day".
  - E4B (c) put "Buy stamps" under the pilot line.
  - E4B (c), for "this week", dropped Sam and gave the done cards as
    outstanding.
- **What it costs.**
  - The window (b) is about 400 tokens more than 0.6.4's instruction (read
    708 against 304 on the first message), and the tools about 550 more
    (1,258).
  - A look adds one model step. On the laptop that is 0.3 to 6 s on E2B and
    2 to 16 s on E4B. A look's answer was 24 to 84 tokens read.
  - In all, ten turns took (leaving out the first message) about 57 s for
    E2B (a), 25 s for E2B (b) and 49 s for E2B (c); and 66 s for E4B (a),
    75 s for E4B (b) and 155 s for E4B (c).
