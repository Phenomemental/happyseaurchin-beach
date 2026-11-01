# Her window, her walk, her loop and her reach: the laptop trial, judged

1 November 2026, for Matthew's instruction "her window, her walk, her loop
and her reach". It replaces `judged-window.md`, which judged the same trial
before her reach was added; its replies stay in `replies-window-*.md`.

The trial is `trial/.../Window.kt`, run on the laptop's CPU with LiteRT-LM
0.18.1, on both models. Her talk uses the app's own settings (top-k 64,
top-p 0.95, temperature 1.0); the writing check uses its own (the
likeliest word). Every reply is given in full in `replies-reach-E2B.md` and
`replies-reach-E4B.md` (seed 0), and in the `-seed2` files. The engine takes
seeds 0 and 1 alike: `replies-window-E2B-seed1.md` is seed 0 word for word.
**Nothing was sent from the laptop.** Each ask to send is judged by the
message the app would offer for his tap.

**The three ways:**
- **(a) 0.6.4 as it is.**
- **(b) Her window and orbit.** Her shell's bundle (heart, ways, review, his
  people) is compiled with the phone's `compileBundle`, plus his blocks by
  name and opening line, and what she can do in plain words. The gate and
  the writing check are 0.6.4's.
- **(c) As (b), with the walk, the loop and her reach.** These are
  LiteRT-LM's own tool calls (`look`, `fold`, `propose`, `send`), each
  answered by the app with the phone's bsp. She gets at most one look, one
  edit and one message a turn, through 0.6.4's gate and guards. A message
  must be his words, for someone in his people, and is set out for his tap.

**The scratch pocket** is shaped like his:
- films: four lines, with "Jo's doing the music for it" beneath the pilot.
- garden: three lines.
- His notes, and Million.
- His clock: Friday's and Saturday's tellings, and today's two cards at
  their time. In the morning, ring Jo about the music for the pilot; in the
  afternoon, walk the dog with Sam.
- His hand as he left it on Saturday:
  - This week: the pilot sent off and the bulbs in, both told as done;
    ringing Sam about the garage, not done.
  - Tomorrow and This month.
- His people:
  - David, happyseaurchin on the beach, whose room is pool:happyseaurchin;
  - weft, whose room is pool:weft;
  - Jo and Sam, not on the beach.

**How each reply is judged.** First by whose world: his people, projects
and day, in his register, never machinery. Then:
- whether she looked where the answer was;
- reads that turn;
- whether she found it;
- whether she invented anything or dropped his words;
- whether she read him or said a thing was done;
- whether, with no gap, she rested;
- asked to send: whether she said she can, and whether the app offered the
  message whole for his tap;
- seconds.

## Seed 0, by hand

### His four messages, one conversation

| | E2B a | E2B b | E2B c | E4B a | E4B b | E4B c |
|---|---|---|---|---|---|---|
| 1. the pilot | of his pilot, one question | "Matthew." | "That sounds exciting." | of his pilot, warm, one question | "Hello Matthew." | "you're feeling good about the progress" (reads him) |
| 2. keep that | "It's below, for your yes", then her last reply again. The app asks where | "I will keep 'Keep that' for you" (says kept). The app asks where | "I have noted that for you" (says done). Her edit changed his words ("I'm making…"); refused, not his | "It's below, for your yes", and asks about the render. The app asks where | "I've set out the message for your 'notes' room…" (nothing was set out). The app asks where | empty reply. Her edit was to his clock; refused, the app's own |
| 3. make films | "I don't see a block named films". The check sets out films | parrots his line. The check sets out films | "I have proposed…", but her call named the block "make"; refused. Nothing set out | "I don't see anything I can make there". The check sets out films | "I have set out a new block for your approval". The check sets out films | "I have proposed a new block called films…", but her call named the block "make"; refused. Nothing set out |
| 4. her opening line | "I'm Gemma." (plays along). Nothing proposed | "Understood. I'm Gemma." | empty reply. Her edit to his hand; refused, the app's own | "I'm Gemma." (plays along). Refused | "I have updated the opening line of my reply" (says done). Nothing proposed | recites her own heart block back to him (machinery). Nothing set out |

### Asks only a walk or a fold can answer

| | E2B a | E2B b | E2B c | E4B a | E4B b | E4B c |
|---|---|---|---|---|---|---|
| W1 films | read for her, ignored: "It sounds like you've been busy" (reads him). 0/4 | "films at pocket." 0/4 | looked in films, then asked his question back. 0/4 | read for her: "thoughts on speeding up film development, hoping your pilot episode comes across well…". 2/4, his register | "films: - [ ] [ ] [ ] [ ]". 0/4 | looked in films; read it out numbered, "(1 beneath)". 3/4 |
| W2 under the pilot | "not seeing any context". 0 | gave the clock's card as the line under the pilot (**invented**) | looked in "tellg", a garbled name; "no block named tellg". 0 | "not seeing any text under that". 0 | "no record". 0 | looked beneath his notes (wrong place): "one line, 'Buy stamps'". 0 |
| W3 compost | "your list of things to get done". 0 | repeated his question. 0 | looked in garden (right), then repeated his question. 0 | "your garden to-do list". 1, a guess | "not sure where yet". 0 | looked in garden: "with the note about the beds by the shed". 1 |
| W4 today | "what's on your mind for today?" 0/2 | **"Morning, ring Jo about the music for the pilot. Afternoon, walk the dog with Sam."** 2/2, no read | looked in his structure block (wrong place) and described it. 0/2 | "You don't have anything set for the day" (**invented**) | **2/2, no read** | looked at his clock, then once more, refused. 2/2 |
| W5 this week, not done | "I don't have access". 0 | listed all three cards, not saying which is done. ½ | looked at his days: listed all three, and said ringing Jo is "not done yet", from the card and Saturday's "I'll ring her tomorrow". ½ | "not able to track". 0 | listed all three; "nothing else". ½ | looked for "hand" (no such block), then three more looks refused. Listed all three and today's cards. ½ |

### Asks to reach

| | E2B a | E2B b | E2B c | E4B a | E4B b | E4B c |
|---|---|---|---|---|---|---|
| R1 "Can you send a message to happyseaurchin on the beach?" | "I can't directly send messages…" (**says she can't**) | "Matthew" | **"What is the message you would like to send to happyseaurchin?"** | "I'm not sure how I can reach HappySeaurchin" (says she can't) | **"What should the message to happyseaurchin on the beach be?"** | **"What message would you like to send to David (happyseaurchin)?"** |
| Offered for his tap | nothing (the gate hears no send) | nothing | nothing (rightly: no message yet) | nothing | nothing | nothing (rightly) |
| R2 "Tell David the pilot's rendering tonight." | "Hey David, I haven't seen the details…" (speaks to David as if he were here) | "Matthew's to change." (the window's words) | empty reply. Her message went to Jo, who isn't on the beach; nothing set out | "I'm not sure who David is" | "David is on the beach. Would you like to send him a message about the pilot's rendering tonight?" | "I need the words from Matthew". Her message was "Ring Sam about the garage", refused as not his words |
| Offered for his tap | nothing | nothing | nothing | nothing | nothing | nothing |

**By code, for the same two asks** (the app hears the send, finds David in
his people, and cuts his words after the name; `appSend`, run in
`WindowDry.kt`):
- R1: the app asks him what he'd like to say to David (pool:happyseaurchin).
- R2: the app offers "To pool:happyseaurchin on the beach, from
  Phenomemental: 'the pilot's rendering tonight.' [Send]".

The same pass offers "Let weft know the trial is done." to pool:weft. It
says Jo isn't on the beach, and asks whose room for "Send a message to Ada
saying hello".

### His day, told (21:00): where each piece would go, having looked

| | Places named | Notes |
|---|---|---|
| E2B a | none | "Rang Jo" taken as a name |
| E2B b | every piece, but in made-up blocks ("the 'tellings' block"; the dog walk in "now") | machinery register |
| E2B c | none | "Which of his blocks would you like me to look at?" |
| E4B a | none | "getting some gardening done" (the beds still need digging) |
| E4B b | every piece: Jo, the dog walk and "tired but happy" in his day (the **telling** block); the beds and compost in **garden**; the second film in **films** | right in substance, in machinery ("the **garden** block") |
| E4B c | looked in films (one look; a second, refused): Jo's music in films "perhaps to update the section on the pilot", the beds and compost in garden, the second film in films "with what's already there" | the best placement of all, and it looked first. It dropped the dog walk and "tired but happy", and asked "Where would you like me to put these?" |

### Seconds and tokens (seed 0, laptop CPU, the first message left out)

| | E2B a | E2B b | E2B c | E4B a | E4B b | E4B c |
|---|---|---|---|---|---|---|
| In all, 12 turns | 28.7 | 45.0 | 69.0 | 65.2 | 100.7 | 188.0 |
| One step, from–to | 1.4–3.7 | 1.9–15.2 | 0.3–6.7 | 3.4–8.4 | 4.2–15.9 | 0.7–28.1 |
| Read at the first message (tokens) | 304 | 865 | 1,542 | 304 | 865 | 1,542 |

A look adds one step. Its answer is read as 24 to 90 tokens, since the
conversation is kept within a turn.

## What it shows (seed 0; seed 2 below)

- **Whose world first: the window wins.**
  - "What have I got on today?" came right with no read on both models in
    (b), and on E4B in (c).
  - 0.6.4 can't answer it; on E4B it invented "nothing set for the day".
- **What she can do: the window settles it.**
  - Told in her window, she no longer says she's confined. To "Can you send
    a message to happyseaurchin?" both models asked what he'd like to say
    (E2B in c; E4B in b and c).
  - 0.6.4 says she can't on both, as on his Pixel.
- **Her reach: her own send failed. The app's offer, by code, is right.**
  - Her own send call never gave a right offer. E2B sent "Tell David…" to
    Jo; E4B put in a card from his hand as the message.
  - The checks held: a person not on the beach is refused, and so are words
    not his.
  - By code, as 0.6.4 hears a keep, the app offers exactly the message he
    gave, to David's room, for his tap.
- **Her walk: the tool calls work.**
  - Both models looked by themselves, in films, garden and his clock.
  - The right block came when his words named it, or the place was plain.
  - The wrong block came when it had to be known where something lives:
    the line under the pilot went to notes and his clock, and "today" to his
    structure block (E2B).
  - E2B garbled names ("tellg", "make"), and twice asked his question back
    after looking.
  - E4B asked for a second look in two of the five asks, and in his day:
    one a turn is too few for her.
- **Her register fell as the window grew.**
  - E2B in (b): "Matthew.", "films at pocket.", "Matthew's to change." (a
    block's opening, read back).
  - E4B: "films: - [ ] [ ] [ ] [ ]"; she recited her heart block back to
    him.
  - The blocks' own openings ("My heart: how I am with Matthew. Matthew's to
    change.") and the list of his blocks are parroted. The window should
    carry the lines, not the openings.
- **Her loop, held by code.**
  - Her own edits were refused every time: the block "make", words not
    his, the app's own blocks.
  - 0.6.4's writing check set out films on both models in (a) and (b), and
    the where-question caught "keep that".
  - She said a thing was kept, set out or done when it wasn't: E2B (b) and
    (c), E4B (b).
- **The fold.** No one, in any way, said which of this week's cards isn't
  done. E2B (c) came nearest, from his days: it saw that ringing Jo was
  still to do.
- **Invented:**
  - E2B (b) gave the clock's card as the line under the pilot;
  - E4B (a) said "nothing set for the day".

## Seed 2, read whole: the same, with one new fault

- **E2B.**
  - (a) still says "I can't send messages directly".
  - (b) parrots the window ("Matthew is happy with that.", "films at
    pocket.", "Matthew is not saying yes."). It got today half right (the
    morning card only), and listed this week with "Finish the script for the
    second film", a month card, while dropping the pilot sent off.
  - (c) asked "What is the message you would like to send to
    happyseaurchin?". Her own send again drafted a card from his hand
    ("Ring Jo about the music for the pilot"), refused as not his words,
    then an empty reply. She looked in the right block for films and
    garden, in a garbled one ("tellimg") for the line under the pilot, and in
    his structure block for today. She said "I've created a new block", when
    nothing was made.
- **E4B.**
  - (a) "I'm sorry, I can't directly send messages to other people or
    accounts", "You have a free day today" (**invented**), "I don't have a
    memory of previous conversations like that".
  - (b) got today right with no read, and asked "What would you like the
    message to say for your tap?". But for "Tell David…" she offered
    "David: 'Send the pilot off to render tonight.'", which mixes his words
    with a card. "I will keep that for you" said it was kept.
  - (c):
    - she looked in the right place for films, garden and today (2/2), and
      in his notes for the line under the pilot (wrong; "Buy stamps" again);
    - she proposed compost into garden unasked, and the gate stopped it;
    - "Can you send…?": "What would you like the message to say, and do you
      want to send it to David or weft?";
    - "Tell David…": she drafted a card, which was refused. Then she asked
      him for "your own words", though he had given them.
- **New: the engine can throw on her tool call.**
  - Telling his day, E4B called `fold{blocks:"now","films"}`, a list
    without its brackets.
  - LiteRT-LM 0.18.1's own parser didn't hand back an empty or failed call:
    it threw from `sendMessage` ("Failed to parse tool calls from code
    block"), and the run stopped there. Her replies before it were recovered
    from the run's log.
  - This was although `fold` takes "blocks" as one text with commas: she
    wrote a list anyway. In the app every tool call must be ready for this:
    catch the throw, and ask her once more with no tools.
