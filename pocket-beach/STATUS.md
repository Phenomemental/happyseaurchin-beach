# Pocket beach — status

Current state only. Newest first.

## Now

**pocket-beach-v0.4.2** is released, from the Pixel's bench of 0.4.1: her
first word free in Ask, opening Ask opens her conversation once, and a
block can be saved as a file, whole (see "What 0.4.2 changes"). Its tag is
at `e25d892` on main. Next is Matthew trying it (see "Next").

**pocket-beach-v0.4.1** is released: the companion keeps what Matthew
tells her that is worth keeping; block names match whatever their capitals;
the bench runs 0.4 on the CPU beside the GPU (see "What 0.4.1 changes").
Its tag is at `73ed17a` on main. Matthew ran its bench on his Pixel on 29
October.

**pocket-beach-v0.4** is released, finished against the Pixel's bench of 0.3
(see "What 0.4 changes"). Its tag is at `62511f7` on main.

**pocket-beach-v0.3.5** is released: speaking a line no longer holds the
phone's speech recogniser, which on Matthew's Pixel (0.3.2) failed with
"error 10" and stopped Gboard's voice typing while the app ran (see "What
0.3.5 changes"). Its tag is at `ad69a9c` on main. On his Pixel, in Ask
with the keyboard open, "Speak the line" still said speech was busy; 0.4
gives the error's number and hides the keyboard first.

**pocket-beach-v0.3.4** is released: the bench, the first step of 0.4 (see
"What 0.3.4 changes"). Its tag is at `f3bf644` on main.

**pocket-beach-v0.3.3** is released: a post the beach couldn't be reached
for is kept, with "Post now" (see "What 0.3.3 changes"). Its tag is at
`e27e439` on main.

**pocket-beach-v0.3.2** used the block's own seal and fingerprint in the
hello world's post, and showed where the companion ran (see "What 0.3.2
changes"). Its tag is at `6ed8f7f` on main.

**pocket-beach-v0.3.1** fixed the model's download and the hello world
posting twice (see "What 0.3.1 changes"). Its tag is at `c7a5155` on main.

Piece 3, the companion, was released in **pocket-beach-v0.3** on 28 October,
and with it today's finish line, the hello world. Its tag is at `ec9cea3` on
main, signed with the same key as 0.1 and 0.2, so it installs over them.
Before it, wearing a shell offline was built and matched against the original
(decisions 8–12).

### What 0.4.2 changes

From Matthew's bench of 0.4.1 on his Pixel 9 Pro XL (Tensor G4, Android 17),
29 October. Each of the 23 asks was put in a conversation of its own:

- 0.3, one JSON plan held to its form: 13 of 23 right, its answers after a
  median of 25.4 s. It read a block to answer "What's your name?" and
  "Good morning!".
- 0.4 on the GPU, her first word held: 22 of 23, something showed after a
  median of 4.7 s, answers done after 5.7 s.
- 0.4 on the GPU, her first word free: 22 of 23, something showed after
  1.8 s, answers done after 2.4 s.
- 0.4 on the CPU alone, her first word held: 23 of 23, something showed
  after 10.8 s, answers done after 11.2 s.

What it says:

- **Held and free gave the same answers**, word for word, on every ask.
  With each ask in a conversation of its own, holding cost about 2.8 s
  between the first token and her first words, on the GPU and the CPU
  alike. In the one conversation kept for three asks, held, it cost
  nothing that showed (below). Free is never slower and answered the
  same, so her first word is free in Ask now; the next bench times the
  kept conversation free, beside these held figures.
- **The GPU reads much faster; the CPU writes about as fast.** The first
  token came after 1.6 s on the GPU and 7.5 s on the CPU: that is the
  reading of her instruction, about 1,300 tokens. After it, her words came
  about as fast on either.
- **In one conversation kept for three asks** (held), her first words
  came after 1.9 s, then 0.5 s, then 0.6 s on the GPU, and after 8.5 s,
  then 1.0 s, then 1.0 s on the CPU. The CPU's slower reading falls on the
  first ask, as Matthew expected, and the asks after it are still about
  twice as quick on the GPU. Ask stays on the GPU.
- **The engine's probes:** speculative decoding off wrote 15.4 tokens a
  second, on 13.1, so it stays off. The model loaded in about 3 s on the
  GPU and on the CPU. No build of the model for the Tensor G4's NPU (only
  for the G5 and G6).
- **The share came through cut short.** Shared and copied from the menu,
  the block stopped at the same place twice, at about 19,000 characters,
  partway into "How it was run". A block is written with its opening
  last, so "How it was run", its last part, and the opening's summary of
  every run were what was lost. Matthew sent screenshots of the rest.
- **The one miss on the GPU was the judge's.** For "Is there anything
  about roses in my garden block?" she said "Yes, the garden block has a
  line about winter pruning roses." The judge wanted "prune"; "pruning"
  counts now.

What 0.4.2 changes:

- Her first word is free in Ask, and in the bench's conversation kept for
  three asks. The bench still asks with it held once, to compare.
- The bench's run on the CPU is free, as Ask is.
- The bench's block puts "How it was run" first, so a share cut short
  loses only the end of the last run.
- **Save this block as a file**, under "Share this block": the block
  whole, as a `.json` file where Matthew picks (Downloads, Drive), to keep
  or attach. Only the one block, unlike the Shore's copy of the whole
  pocket.
- Opening Ask opens her conversation once. Now and then it opened it and
  shut it again at once: the screen let its wait for the model go on
  another thread, in either order with the opening. The first ask then read
  her instruction a second time. Seen twice in CI; a device test repeats it.
- Version 0.4.2 (`versionCode` 11).

### What 0.4.1 changes

From Matthew on 0.4's Ask. He said "I'm going to make a million pound by
creating tech that changes the world… before the end of 2027", with an
empty block called Million in his pocket. She only chatted back, and only
the ask log got a line.

- **She keeps what's worth keeping.** Her instruction now says to act not
  only when he asks her to write, but also when he tells her something
  worth keeping: a goal, a plan, an idea, something about his life.
  - If a block fits it, by the block's name or its opening line, his words
    go there (`add <block>: …`).
  - If none fits, they go in a new block named for it in a word or two,
    with his words as its opening line (`new <name>: …`).
  - Greetings, thanks and questions stay talk. 0.4's "When unsure, talk"
    is gone, since it was what stopped her.
  - Two more example turns show both kinds, and a thanks shows talk.
  - Each of his messages reaches her as "Matthew says: …", not "Matthew
    asks: …", so something told isn't read as a question.
  - As ever, he sees the writes before Yes.
- **The app offers it when she doesn't.** When she only talks, and what he
  said names one of his blocks (not a question, an ask to write, or a
  greeting), the app shows "Keep this in Million?" under her reply. A tap
  plans his words, exactly as he said them, as a new line of that block,
  for Yes.
- **Block names match whatever their capitals.** Speech writes "million",
  and the block is Million.
  - An action that names million is for Million.
  - A new block whose name differs from one of his only in its capitals is
    refused ("There's already a block called Million.").
  - When she writes `new million: …` with Million there, which she did on
    the laptop, his words go in Million.
  - A rename may change a block's own capitals. On a disk that doesn't
    tell capitals apart, the block passes through a passing name, so
    nothing is lost.
- **The bench:**
  - It has 23 asks now:
    - the Million case: an empty Million block, that sentence, and the
      right answer is an add to Million;
    - a plan that fits no block ("My plan is to learn to sail next
      summer.", which should go in a new block, with his words as its
      opening);
    - one more chat ("Good night, talk tomorrow.").
  - Keep is judged as a kind of its own, beside talk, read and act.
  - Where she only talked, each ask's line says what the app would offer
    ("Keep this in Million?").
  - **0.4 on the CPU beside the GPU:** after 0.3's run and 0.4's two on
    the GPU, the same asks are put to 0.4 on the CPU alone, with her first
    word held. Then one conversation is kept for three asks on each, since
    the CPU's slower reading of the instruction should fall mostly on the
    first ask. Google's own LiteRT-LM table has the CPU nearly matching the
    GPU for E2B on new phones, and on Tensor the Mali GPU is the weak part.
  - It takes about half an hour now.
- Version 0.4.1 (`versionCode` 10).

**On the laptop's CPU** (her first word held): 21 of 23 with her new
instruction, then right on the Million case once `new million` with Million
there kept his words in it. A rerun of the keep and chat asks (3, 6, 13, 21,
22, 23) got 6 of 6. The plan that fits no block went in a new block,
`sailing`, though she wrote "I want to learn to sail next summer.", not his
words word for word. Her instruction is about 230 tokens longer (about
1,300), which the kept conversation reads once.

### What 0.4 changes

From Matthew's plan (see "0.4: as fast and dependable as the phone allows"),
finished against his Pixel's bench of 0.3 (28 October).

**His Pixel's bench of 0.3:**
- The phone: a Pixel 9 Pro XL, Tensor G4, 16 GB, Android 17. The model ran
  on its GPU.
- 0.3 got 17 of 20 right, with a median of 26 s an answer.
- The engine itself is quick: the first token came after 1.6 s every time,
  and it wrote about 14 tokens a second on the GPU.
- The wait was 0.3's way of asking:
  - a whole JSON plan held to its form, written at about 8 tokens a second
    against 14 free;
  - two rounds for talk and read;
  - nothing shown until the end.
- Its misses (11, 14, 15) are the kinds 0.4 is built for: naming a line by
  its words, adding beneath a line, and one line a step.
- Speculative decoding gained nothing there: 13.2 tokens a second on, 13.9
  off.
- There is no NPU build for the Tensor G4.

**What changes:**
- **Short actions** (`Acts`). An act is one action a line:
  - `new <block>`, or `new <block>: <its opening line>`;
  - `add <block>: <line>`;
  - `add <block> under "<the words of a line>": <line>`;
  - `change <block> "<the words of a line>": <the line rewritten>`, or
    `change <block>: <its new opening line>`;
  - `read <block>`;
  - `post: <message>`.
- **A line is named by quoting its words,** never its address. The app finds
  it, trying in turn:
  1. the exact words;
  2. the same words, ignoring case and a stop at the end;
  3. the one line that holds them.

  A quote that two lines match is put to Matthew ("Which one?") before
  anything is planned. A quote that no line matches is sent back to her,
  once.
- **The app does everything mechanical:**
  - it opens up a plain line to add beneath it;
  - it builds the bsp() calls on a copy and shows them before Yes, as 0.3
    did;
  - for the hello world, it writes the shell's line, the seal, the
    fingerprint and the message;
  - if she leaves the secret's slot and Matthew gave the secret in so many
    words ("my secret is …"), it writes it in. He sees it before Yes.
- **What she plainly meant, the app reads as meant:**
  - a change split over two lines;
  - a line restated just before the action that quotes it;
  - a quote of a block's opening, meaning the block itself;
  - the shell's opening written out where the shell's line goes.
- **Three ways of answering, by her first word:** talk, read or act.
  - **The first word is held by the engine:** each reply must begin with
    talk, read or act on a line of its own, and anything may follow
    (`Talks.HELD`). With speculative decoding off this works. Should it fail
    on a phone, she is asked again free, and stays free for the app's run.
  - Her reply is read whole. Any words she says before an act are what she
    says with it.
  - An act needs actions after it, and a read needs read lines (or the
    blocks she named). Anything else is talk: when unsure, talk.
  - A read of a block that isn't there is still answered: she is told so,
    and answers in plain words.
- **Speculative decoding is off.** It gained nothing on the Pixel, and it
  stops the engine holding a reply to a form. The bench still times it,
  on and off.
- **Talk streams as she writes,** and so does her answer after a read.
- **One conversation, kept while Ask is open.**
  - The model loads as Ask opens. Her instruction and the shell are read
    once; each later ask adds only its own words.
  - When writes land, it is opened afresh (her instruction shows the
    pocket), carrying the last six exchanges. It is also opened afresh
    after a reply is stopped part way, since the engine won't take another
    ask in that conversation.
  - She is told how her last act ended (yes, or not now).
  - The earlier exchanges show above the ask.
- **Her instruction** lists each block with its opening (named as its
  title, not a line), how many lines it has, and its lines by their words,
  with no addresses. Three example exchanges are given as the
  conversation's first turns, and a read is written into the instruction.
- **The shell's hello world line,** in actions: `act / new hello-world /
  add hello-world: Secret: HIS SECRET / post: Hello world`. Ask offers to
  update an older line.
- **Show timings** (Settings, new, from the Shore; on by default for now).
  - On, every answer (talk, read or act) shows how long it took, when her
    first words came, and whether the GPU or CPU ran her; the earlier
    exchanges do too.
  - Off, answers show nothing extra.
  - The ask log keeps the times either way.
  - A read-and-say answer now shows its time line like any other.
- **Speaking a line, beside the keyboard** (from Matthew's Pixel on 0.3.5;
  see "Next"):
  - the busy message now gives the phone's error by its number ("Speech is
    busy (the phone's error 10). Try again in a moment."), so his next test
    says which;
  - if the keyboard is up, it is hidden first, and listening starts a
    moment later.
- **The bench:**
  - it puts the twenty asks to 0.3's way (before), then to 0.4's twice:
    with her first word held, then free;
  - then it asks three questions of one kept conversation, then runs the
    engine's probes;
  - it is kept as one block;
  - **its judge is stricter on talk.** 0.3's "I don't see any block named
    'block'" (to "In one sentence, what is a block?") and "… named
    'hello-world'" (to "Thanks, that's all for now.") were marked right. Now
    each talk ask has words it must say, a talk or read answer that looks
    for a block nobody asked about is wrong, and a greeting, a thanks or
    "What's your name?" answered by reading a block is wrong.

  Judged this way, his Pixel's 0.3 run comes to 13 of 20, not 17: 1, 3, 5
  and 6 are now wrong.

**On the laptop's CPU** (Gemma 4 E2B, the same twenty asks, the stricter
judge, speculative decoding off). A laptop's CPU is far slower than a
Pixel's GPU, so only the comparison counts:

| | Right | First words, median | Whole answer, median |
|---|---|---|---|
| 0.3 (the first 13 asks, the old judge) | 12 of 13 | 35.2 s (shown only when whole) | 35.2 s |
| 0.4, her first word held | 20 of 20 | 8.9 s | 9.5 s |
| 0.4, her first word free | 20 of 20 | 5.4 s | 5.8 s |

- Each of the bench's asks is a fresh conversation. The held first word
  costs about 3.5 s once per conversation (setting up the form), and
  nothing after it.
- Ask keeps one conversation, where that cost falls once.
- In one kept conversation, every ask after the first had its first token
  in 0.6 to 0.8 s, held or free, and its whole answer in 1.0 to 2.5 s.
- Writing ran at about 11 tokens a second, held or free.

**Not built, and why:**
- **The NPU:** Gemma 4 E2B has NPU builds only for Tensor G5 and G6; his
  Pixel has a G4.
- **FunctionGemma:** Google's base model is gated behind a licence an account
  must accept, and comes as weights to fine-tune and convert. The
  ready-made builds are tuned for other tasks. 0.4's own acts were 8 of 8
  on the laptop, so there is little room for it to be both faster and more
  accurate.

### What 0.3.5 changes

**Speaking a line lets go of the phone's speech recogniser** as soon as it
is done with it. Matthew's Pixel, on 0.3.2: "Speak the line" failed with
"the recogniser's error 10" (too many requests), and while the app ran,
Gboard's voice typing stopped working too.

- **Why:**
  - each "Speak the line" made its own recogniser;
  - a tap to stop only asked it to stop listening, and it was let go only
    if the phone answered;
  - nothing let it go when the app went to the background.

  So a recogniser could stay bound to the phone's speech service, the one
  Gboard's voice typing uses.
- **Now** (`Speech`):
  - the app holds one recogniser at most, and starting one lets any other
    go first;
  - it is destroyed when listening ends, when Matthew taps to stop (what it
    had heard so far is the line), on any error, when the screen is left,
    and when the app goes to the background;
  - nothing is ever tried again by itself.
- **Error 10 (or the recogniser busy):** it says "Speech is busy. Try again
  in a moment." For five seconds a tap makes no recogniser, and says the
  same.
- **Still on-device only:** the recogniser is the phone's own on-device one,
  and there is no fallback to a server.
- **Device tests** with a stand-in recogniser (the emulator recognises no
  speech): one at a time; let go on a tap, at the end, on an error, on
  leaving the screen and in the background; busy said plainly, and nothing
  made until its time.
- **Not checkable here:** whether Gboard's voice typing works beside the
  app. No emulator has Gboard's on-device speech, so that is for Matthew's
  Pixel (see "Next").
- Version 0.3.5 (`versionCode` 8).

### What 0.3.4 changes

**The bench** ("Run the bench", on Ask), the first step of 0.4: every change
to the companion is judged by what it does on Matthew's Pixel.

- **Twenty fixed asks** (`Bench.ASKS`): six to talk ("What's your name?"),
  six to read ("What's in my garden block?") and eight to act. Among the acts
  are a line added beneath a plain line, two lines at once, a change, a new
  block and a fresh hello world.
- **Each on a scratch copy of a small pocket:** the companion's shell,
  `garden` (an opening line and two plain lines) and `notes` (one line). It
  is made fresh for every ask, so nothing of Matthew's is written, and
  nothing is posted.
- **Each is judged mechanically:**
  - talk and read must plan no write and offer no post, and say plain words
    (read: the words the block holds);
  - an act must plan exactly the writes it needs, landing where they should;
  - the hello world must check out once carried out on the scratch copy,
    with its post the block's own.
- **Each is timed:** to the engine's first token, until anything showed, and
  in all, with the exchanges and the tokens read and written. Today's
  companion shows nothing until its whole plan is written, so its "shown"
  is its total.
- **Then it tries the engine itself, on the phone:**
  - what the model file runs on, and the phone's chip;
  - a plain paragraph, streamed, with speculative decoding off and then on:
    the first word, the tokens written per second, the total;
  - whether a one-word choice held by the engine survives speculative
    decoding;
  - the NPU, where the file has a build for it.
- **The run is kept as one new block,** `bench-` and the date and time:
  - its opening line is the score and the medians;
  - then a line each for talk, read and act, with every ask beneath;
  - then how it was run.

  It is written by Matthew's tap on "Run the bench", and can be shared from
  its page.

### What 0.3.3 changes

Matthew's offline test of 0.3.2 passed: the writes landed, and the post
failed plainly and sent nothing. Now such a post is kept:

- **When the beach can't be reached,** the message he said yes to is kept on
  the phone, with when it failed and why. It shows under "Waiting to be
  posted" on the Shore, at the end of the Ask, and on the test screen.
- **"Post now"** can be tapped only while the phone has a connection that
  reaches the internet, as Android says. Offline it reads "Waiting for a
  connection". Coming back online sends nothing: a kept post goes only on
  his tap, and once it lands it is no longer kept, and it ends in "Posted to
  your pool at entry N".
- **"Discard"** drops a kept post after asking. The writes it came with stay.
- **If the message went but no answer came back,** it is kept too, saying it
  may be on the beach already, so he can look at the pool before posting it
  again.
- **A post the beach answered and refused** (a key needed, too many for the
  moment) is not kept, as before. If a kept post is refused on "Post now",
  it stays kept with the beach's answer.
- The Ask log says when a post was kept.
- The connection is read in `net/HttpsWire.kt`, beside the one post. It
  remains one of the only two files that name a network API.

### What 0.3.2 changes

From Matthew's first hello world with the companion, on 0.3.1:

- **The post kept an earlier run's seal and fingerprint.** With an old
  `hello-world` in the pocket, the companion proposed changing its secret
  line, and copied the old seal and fingerprint from the block's lines into
  the post. Now the hello world's seal, fingerprint and message are the
  app's alone:
  - an Ask that writes to `hello-world`, or offers its post, gets two writes
    of the app's own after the companion's, shown with them in crimson
    before Yes: the seal line made this Ask's new seal, then the last line a
    fingerprint of the block as it stands after every other write, computed
    as that line is written;
  - the post is made from the block once the writes have landed
    (`HelloWorld.messageFor`), whatever message the companion wrote. If the
    block's last line isn't a fingerprint that matches it, there is no post,
    and Ask says why;
  - the shell's hello world now offers only `"message": "Hello world"`, since
    the app writes the message. A shell made before offers to update that
    line;
  - with a `hello-world` already in the pocket, the companion is told so,
    and the one step that changes its secret line, at the line's address.
    It then answers in one exchange, not two.
- **The answer took 56.2 s.** It isn't known yet whether that was on the GPU
  or the CPU: 0.3.1 fell back to the CPU without saying why. Now:
  - beside the time, Ask says where it ran: "Took 12.3 s on the GPU, in 1
    exchange";
  - beneath it, from the engine's own count: loading the model, the tokens
    read and how long, the tokens written, how long and how fast;
  - if the GPU can't take the model, its own error is shown ("The GPU
    couldn't run it: …"), and kept in the Ask log with the rest;
  - what the companion reads is about 1,350 tokens (4,700 characters): the
    instructions and two examples, the shell twice (worn, then plainly), the
    pocket's blocks and the teaching blocks' names. The hello world's answer
    is now about 240 tokens, from 270, or about 95 when only the secret
    changes. See "Piece 3: how the companion is asked" for the laptop's
    figures.
- **A block can be renamed or removed from the Shore.** "Rename or remove a
  block", beneath your blocks, puts Rename and Remove on each row.
  - Rename keeps every line, the opening line too, and refuses a name that's
    taken or can't be a block's, as Make a new block does.
  - Remove asks first, and says that only a copy saved before can bring the
    block back.
  - For the companion's shell and the Ask log, both say what renaming or
    removing them does.

### What 0.3.1 changes

From Matthew's first runs on his Pixel:

- **The model's download failed.** Android's download manager stopped with
  error 1004, an HTTP data error, on Hugging Face's redirect to its content
  servers.
  - The app now fetches the model itself (`net/ModelFetch.kt`). It follows
    the redirect one hop at a time, and only to the hosts the download
    needs.
  - An interrupted download carries on from where it stopped, with a range
    request. A broken connection is tried again, up to six times, each wait
    longer.
  - Progress, speed, a pause and any failure's real reason show on screen
    (see "The model").
  - The byte-for-byte check against the SHA-256 is kept.
- **The hello world by hand posted twice** (entries 168 and 169 of his pool),
  because nothing visibly confirmed the first. Now:
  - each step ends in a confirmation in the page's own type: "hello-world
    made", then "Posted to your pool at entry N";
  - once a message is posted, its button reads "Posted at entry N" and won't
    send it again. "Post again?" asks once more, saying it would add a
    second, identical entry. The app keeps what it has posted, so this holds
    after leaving the screen, on Ask too;
  - more space between buttons, at the foot of every page and in every
    dialog.
- **The post carries the shell's opening line**, so it can be read on the
  beach: "Hello world from the pocket. Shell: “Pocket companion. …” Secret: ….
  Seal: …. Fingerprint: …. Block: pocket/hello-world."
  - A shell made in 0.3 has the older hello world line. Ask offers to update
    that one line.

Piece 2 (v0.2) is released, and Piece 1 (v0.1) works on Matthew's Pixel, in
aeroplane mode too.

### What is built

**Piece 3: the companion.** "Ask the companion" on the Shore opens Ask.

- **The companion** is Gemma 4 E2B, run on the phone by LiteRT-LM 0.18.0, the
  version Google's own gallery app pins. It wears the shell `pocket-companion`,
  put on as the play door puts it on, offline (see "Wearing a shell offline").
- **Its shell** is a pocket block made by one tap on Ask. Its opening line:
  "Pocket companion. I live on this phone, read freely, and write only when
  Matthew says yes." Its line 1 says how to do the hello world.
- **Asking:** type or speak the ask (decision 16), then Ask. The companion
  answers with what it says and the steps it proposes.
  - Reads run straight away; it is shown what they found and answers again.
  - Writes are planned in order on a copy of the pocket, each the same guarded
    write the Write screen makes, and shown with where each line lands and the
    exact bsp() call. Nothing is written until Yes (decision 6).
  - On Yes, each write is planned again on the pocket itself and must land
    where the copy showed, or nothing more is written, and it says which
    landed.
  - "Not now" asks why with one tap: wrong place, wrong block, misunderstood,
    or just not now (decision 15).
  - A post the companion offers comes after the writes, shown whole, on a card
    of its own: "Post it" or "Not now".
  - Under the answer: how long it took, on which processor (GPU or CPU), in
    how many exchanges; then loading, reading and writing as the engine
    counts them, and, on the CPU, why not the GPU.
  - A post is offered only when Matthew's own words speak of posting, a
    pool, the beach or the hello world. Any other the companion offers is
    left out, and the log says so.
- **The app's gaps.** Some things must be the app's, never the model's. The
  companion writes gaps and the app fills them as it writes:
  - `{{seal}}`: 128 random bits from the phone's secure random source, new
    for each Ask;
  - `{{fingerprint}}`: the first 16 hex digits of the SHA-256 of the block's
    stored text, as it stands just before that line;
  - `{{shell}}`: the shell's opening line;
  - `{{address}}`: the pocket address of the block the Ask made.

  A gap the app doesn't know is refused. A fingerprint can't be known before
  its line is written, since every line the beach appends carries its arrival
  stamp. So Ask shows it as "its fingerprint, made as it is written", and the
  post card shows the real one.
- **The hello world's seal, fingerprint and message are the app's.** After
  the companion's writes to `hello-world`, the app adds its own, shown with
  them: the seal line made this Ask's seal, then the last line a fingerprint
  of the block as it then stands. Its post is made from the block once
  written.
- **The Ask log** (decision 15) is the block `ask-log`. Every Ask leaves one
  entry:
  - the entry's line: what was asked;
  - field 1: the bsp() calls proposed, and any post offered;
  - field 2: how it ended (Yes and where the writes landed; Not now and why;
    the post's slot);
  - field 3: its arrival stamp, from the beach;
  - field 4: how long the companion took, on which processor, and in how many
    exchanges.
- **The model** (decision 13).
  - It is offered only on a phone that reports at least 7.0 GB of memory.
    Google's gallery app asks 8 GB for this model, and a phone sold with
    8 GB reports a little less.
  - A phone with less is told so plainly, and pointed to the hello world by
    hand.
  - With the memory and the room, Ask offers "Get the model, 2.6 GB over
    Wi-Fi". The app fetches one pinned file itself:
    `litert-community/gemma-4-E2B-it-litert-lm`, revision `6e5c4f1e`. It checks
    it byte for byte against its SHA-256 (`18193810…7139a63c`) before it is
    ever loaded.
  - **The way it comes:** Hugging Face answers the file's address with a
    redirect to its content servers. The app follows each hop itself, only
    over HTTPS and only to `huggingface.co`, `us.aws.cdn.hf.co`,
    `us.gcp.cdn.hf.co` or `cas-bridge.xethub.hf.co`.
    - On 28 October it went to `us.aws.cdn.hf.co`. The other two are Hugging
      Face's own hosts for files stored as this one is.
    - Sent anywhere else, it stops and names the host.
  - **Wi-Fi only:** before each pass, a metered connection such as mobile
    data stops it with a plain reason.
  - **It carries on while Matthew is away from the screen,** in a foreground
    service with a notification showing how far it has come. Android asks
    once whether the app may show notifications; the download runs either
    way. Android allows such a service six hours a day, and if it calls time
    the download stops, keeping what has come.
  - **On screen:**
    - while fetching: how far, as a figure, a bar and a speed, with any retry
      said in crimson;
    - "Paused at …", with "Carry on" and "Discard what has come";
    - a failure in its real words: the HTTP status and what the server said,
      the refused host, the broken connection, or the mismatched SHA-256.
      Anything kept is carried on from.
  - It loads when Matthew asks, on the GPU, or on the CPU if the GPU can't
    take it, keeping the GPU's error to show. It is let go after three idle
    minutes, or as soon as Android asks for memory back.
- **The one way out** (decision 18).
  - `net/HttpsWire.kt` opens a connection for one thing: one POST of one
    message to a pool on the beach, only after Matthew taps to confirm. It
    takes no address but the beach's pool append, follows no redirect, and
    keeps no cookie or cache. It also reads whether the phone is connected,
    only so a kept post's "Post now" waits for a connection.
  - `net/ModelFetch.kt` is the only other code that opens one, for the
    model's one download (see "The model").
  - The posts go only to `pool:Phenomemental`, under the name Phenomemental.
    No key is needed (see "Today's finish line"). If the beach ever asks for
    one, nothing is sent and it says so.
  - The network security config trusts no certificate for any host but
    `beach.happyseaurchin.com` and the download's four, each exactly, with
    no subdomains. It refuses plain HTTP everywhere.
  - A test fails the build if any other file names a network API, or if the
    hosts the config trusts and the hosts the download follows ever differ.
- **The hello world by hand**, on the test screen ("Try a call by hand"
  under About), so the chain can be shown without the companion:
  - Step 1, "Make hello-world": the shell's opening line, the secret, a seal
    the app makes and the fingerprint, each a guarded write. It ends in
    "hello-world made".
  - Step 2 shows the whole message. "Post it to pool:Phenomemental…" posts
    it only after its own "Post it", and ends in "Posted to your pool at
    entry N".
  - Then the button reads "Posted at entry N". The same message goes again
    only through "Post again?".
- **Share a block out, receive one in** (decision 19).
  - "Share this block", at a block's opening, sends its JSON through
    Android's share menu, to whichever app Matthew picks.
  - A block shared into the pocket opens on "Received" as data: its opening
    line and lines, nothing in it followed. "Keep it as a block" makes it a
    new block of his, exactly as it came. It joins no shell.

**Wearing a shell offline**, in the core (decisions 8–12):

- `Bundle.kt`: bsp-mcp-server's `bundle.ts`.
- `Temporal.kt`: the clock functions it needs.
- `Seat.kt`: the "what moved" rider.
- `Shells.kt`: the play door's own-context compile.
- The bundle window on ordinary reads.
- A shell's list following new entries without a secret.

See "Wearing a shell offline" for what is built and what is not.

**In the core for Piece 3** (pure Kotlin, so a laptop front end gets the same):

- `Asks`: the companion's answer read and checked, its writes planned and
  carried out as above, the log, and the companion's standing instruction.
- `PoolPost`: the one post, prepared and sent over a wire the front end
  hands it.
- `HelloWorld`: the shell, and the hello world made and checked.

**Piece 2:** the Shore, a block, Write, Save a copy, Restore a copy, and
About, in Matthew's book look, with `Pages` and `Copies` beneath them. Since
0.3.2, a block can be renamed or removed from the Shore (`Pages.rename`,
`Pages.remove`).
**Piece 1, unchanged:** the local bsp, the teaching shelf, storage.

**The app itself:** version 0.3.4 (`versionCode` 7), Android 12 or later.

- Permissions:
  - the internet, for the one confirmed post and the model's one download,
    with the network state, so that download keeps to Wi-Fi;
  - a foreground service and notifications, for that download while Matthew
    is away from the screen;
  - the microphone, for speaking.
- Android's backup is off.
- The release is built for arm64 phones only: 49 MB, most of it the engine.

### What was checked

- **The core's tests:** 70, all passing, on the JVM and again on Windows with
  no Android SDK.
  - Asks: answers read leniently and checked strictly; the hello world shown
    first, then written as shown; nothing written when the pocket moved since
    it was shown; a write that would land elsewhere stopping the rest; unknown
    gaps, missing blocks, teaching blocks and other pools refused.
  - The log wrapping at its tenth entry, like any block.
  - The post: exactly the request bsp-mcp-server's wire sends, never retried,
    each refusal said plainly.
  - The shell's own answer making a hello world that checks out.
  - A received block kept exactly as it came.
- **The app's own tests:** the one way out, as above.
- **Golden calls:** 261 cases and 865 calls match the original, in files and
  in memory, along with 5,497 checks against the original's own functions (see
  "Wearing a shell offline").
- **The companion against the real model, on a laptop.** `trial/` runs the
  hello world with the model file the phone downloads, the engine's desktop
  build of the same API, and the phone's own instruction and settings. It runs
  on the CPU, 20 to 60 s an answer; the phone's GPU should be much quicker.
  - **With 0.3.2's instruction** (the post the app's, the shell's post only
    "Hello world"), asked the same eight ways:
    - on an empty pocket, 7 of 8 passed, six in one exchange. Each answer
      was about 240 tokens, 25–40 s here;
    - with a `hello-world` made before, as on Matthew's phone (`existing`),
      7 of 8 passed, each in one exchange: the one change to its secret
      line, about 95 tokens and 15–20 s here. The app's seal and fingerprint
      followed, and every post was the block's own, with this Ask's seal;
    - both misses were "secret: 42 is not the answer", written as
      "Secret: 42", which shows before Yes. The block's seal and
      fingerprint still checked out.
  - It asked in eight different ways (`gradlew :trial:run -Ptrial`).
  - **With 0.3.1's instruction, 7 of the 8 passed.** In each,
    `hello-world` held the shell's opening line, the secret word for word,
    the app's seal and a matching fingerprint. The post carried all four
    and the block's address.
  - One of the seven, "hello world my secret is bananas are berries", needed
    one round of being told why: its first answer added to `hello-world`
    before making it.
  - **The miss:** "please do hello-world, secret: 42 is not the answer". It
    left `HIS SECRET` unfilled. Told why, it answered with a single change,
    as if the first proposal had been written. The app refused it, so
    nothing was written. Asked as "Do the hello world. My secret is: …", it
    passes.
  - Ordinary asks of a small pocket:
    - "Add a line to garden" landed where it should.
    - "What's in my garden block?" was read and answered.
    - "Under the autumn line…" found the right line, but repeated the line
      it was put beneath. That is shown before Yes, for "Not now: wrong
      place".
  - Earlier forms of the instruction scored 0 to 3 of 4; see "Piece 3: how
    the companion is asked".
- **On a Pixel emulator (Android 16):** all 17 on-device tests pass. The
  six new ones:
  - The hello world with a stand-in for the companion (no test can run the
    model). The proposal is shown and nothing is written before Yes. The
    block checks out, and nothing is sent before Post it. Then exactly one
    request goes, to a wire that goes nowhere, and the log keeps the slot.
  - "Not now" writes nothing and logs why.
  - The hello world by hand, on the test screen.
  - A block shared in, shown as data and kept only on a tap.
  - The model is never offered without the memory. The emulator has 2.6
    GB, and it was told so plainly.
  - Plain HTTP refused everywhere.
- **The screen tour** now photographs Piece 3 too (Ask at each stage, the
  hello world by hand, Received), light and dark. Reading them by eye led
  to three changes:
  - more room between the rows of a proposal;
  - after a post, "Posted" first;
  - the shell's notice reworded.

### The 0.4.2 release

- Its tag is at `e25d892` on main, published 29 October.
- Signed with Matthew's key: certificate `CN=Matthew Bushell`, SHA-256
  `217887a66749cf2666e6eab726a2e904ceeec76f85285ce5c716eb72ce156ad2`, as
  before. apksigner verified it in CI and again after download.
- Version 0.4.2 (`versionCode` 11), for arm64 phones, 50 MB. Its SHA-256 is
  `4cb0c7581b2424f57c369bdbb4f57c84a28f454ebc785596d79ee2059cd42af0`.
- Read back from the downloaded APK: the same five hosts, each without
  subdomains, and nothing else; plain HTTP refused everywhere; the same
  seven permissions (saving a file goes through the phone's own picker,
  and needs none).
- Built by the release run on main after every test passed again there.

### The 0.4.1 release

- Its tag is at `73ed17a` on main, published 28 October.
- Signed with Matthew's key: certificate `CN=Matthew Bushell`, SHA-256
  `217887a66749cf2666e6eab726a2e904ceeec76f85285ce5c716eb72ce156ad2`, as
  before. apksigner verified it in CI and again after download.
- Version 0.4.1 (`versionCode` 10), for arm64 phones, 50 MB. Its SHA-256 is
  `4eddf26bfc764f48af3d89d31a5fa85bd506d8eb9cf114bc26bc1903e94a57fc`.
- Read back from the downloaded APK: the same five hosts, each without
  subdomains, and nothing else; plain HTTP refused everywhere; the same
  seven permissions.
- Built by the release run on main after every test passed again there.

### The 0.4 release

- Its tag is at `62511f7` on main, published 28 October.
- Signed with Matthew's key: certificate `CN=Matthew Bushell`, SHA-256
  `217887a66749cf2666e6eab726a2e904ceeec76f85285ce5c716eb72ce156ad2`, as
  before. apksigner verified it in CI and again after download.
- Version 0.4 (`versionCode` 9), for arm64 phones, 50 MB. Its SHA-256 is
  `e4310992c5c4a65b6dbb2c638b38b130b9baf29ac76ee6231dea5225d3196516`.
- Read back from the downloaded APK: the same five hosts, each without
  subdomains, and nothing else; plain HTTP refused everywhere; the same
  seven permissions.
- Built by the release run on main after every test passed again there.
- The first release run failed twice, on a different tap each time: Ask
  just after typing, and "Post again?" just after a dialog closed. The
  page resizes as the keyboard comes or goes, so a tap aimed while it moved
  could miss. The device tests' taps now wait for what they tap to stop
  moving; nothing in the app changed. The second release run passed.

### The 0.3.5 release

- Its tag is at `ad69a9c` on main, published 28 October.
- Signed with Matthew's key: certificate `CN=Matthew Bushell`, SHA-256
  `217887a66749cf2666e6eab726a2e904ceeec76f85285ce5c716eb72ce156ad2`, as
  before. apksigner verified it in CI and again after download.
- Version 0.3.5 (`versionCode` 8), for arm64 phones, 50 MB. Its SHA-256 is
  `a84f7c8185c9d9895032aa2aecbf6e96347dab023a1fbc0f0f40672add133c2d`.
- Read back from the downloaded APK: the same five hosts, each without
  subdomains, and nothing else; plain HTTP refused everywhere; the same
  seven permissions as 0.3.4.
- Built by the release run on main after every test passed again there,
  the speech tests among them. The CI run before it lost its emulator to a
  failed download before any test ran; the one re-run passed.

### The 0.3.4 release

- Its tag is at `f3bf644` on main, published 28 October.
- Signed with Matthew's key: certificate `CN=Matthew Bushell`, SHA-256
  `217887a66749cf2666e6eab726a2e904ceeec76f85285ce5c716eb72ce156ad2`, as
  before. apksigner verified it in CI and again after download.
- Version 0.3.4 (`versionCode` 7), for arm64 phones, 50 MB. Its SHA-256 is
  `71e7fe736beb913aae14749ea3b9473ba153c877a755f7f697b8ead0cc21f416`.
- Read back from the downloaded APK:
  - its network security config trusts the same five hosts, each without
    subdomains, and nothing else; plain HTTP is refused everywhere;
  - its permissions are 0.3.3's.
- Built by the release run on main after every test passed again there,
  the bench's device test among them.

### The 0.3.3 release

- Signed with Matthew's key: certificate `CN=Matthew Bushell`, SHA-256
  `217887a66749cf2666e6eab726a2e904ceeec76f85285ce5c716eb72ce156ad2`, as
  before. apksigner verified it in CI and again after download.
- Version 0.3.3 (`versionCode` 6), for arm64 phones, 49 MB. Its SHA-256 is
  `3b896b5da7adf7024a8bd7f7545b0024d445decd22bd8e5de79c7c098425de66`.
- Read back from the downloaded APK: its network security config trusts
  the same five hosts as 0.3.2, each without subdomains, and nothing else.
- Built by the release run on main after every test passed again there.
- Checked on Matthew's Pixel before it: offline, the writes succeeded, the
  post failed plainly, and nothing was sent.

### The 0.3.2 release

- Signed with Matthew's key: certificate `CN=Matthew Bushell`, SHA-256
  `217887a66749cf2666e6eab726a2e904ceeec76f85285ce5c716eb72ce156ad2`, as
  before. apksigner verified it in CI and again after download.
- Version 0.3.2 (`versionCode` 5), for arm64 phones, 49 MB. Its SHA-256 is
  `23a6457fad9384baa3455c3c0b68270f1b63e1286de2a25a249e97f2d60849e1`.
- Read back from the downloaded APK: its network security config trusts
  `beach.happyseaurchin.com`, `huggingface.co`, `us.aws.cdn.hf.co`,
  `us.gcp.cdn.hf.co` and `cas-bridge.xethub.hf.co`, each without
  subdomains, and nothing else; plain HTTP is refused everywhere. Its
  permissions are those of 0.3.1.
- Built by the release run on main after every test passed again there: the
  core on Linux and Windows, the golden calls and 1,000 fresh random cases,
  the one-way-out tests, and every on-device test.

### The 0.3.1 release

- Signed with Matthew's key: certificate `CN=Matthew Bushell`, SHA-256
  `217887a66749cf2666e6eab726a2e904ceeec76f85285ce5c716eb72ce156ad2`, as
  before. apksigner verified it in CI and again after download.
- Version 0.3.1 (`versionCode` 4), for arm64 phones, 49 MB. Its SHA-256 is
  `070cb2c5e94ff3a57f3aee5ba23d33d6faaa8dc6cc0039d4a6d11b16cce776d1`.
- Read back from the downloaded APK:
  - its network security config trusts `beach.happyseaurchin.com`,
    `huggingface.co`, `us.aws.cdn.hf.co`, `us.gcp.cdn.hf.co` and
    `cas-bridge.xethub.hf.co`, and nothing else;
  - the download's service is declared as data sync.
- Checked before it:
  - the real download: across a stop at 300 MB and a resume, the whole file
    matched its SHA-256;
  - the laptop trial with the new post form: 7 of 8 hello worlds checked
    out. The miss was "secret: 42 is not the answer", refused, with nothing
    written;
  - all 19 on-device tests and the screen tour.

### The 0.3 release

- Signed with Matthew's key: certificate `CN=Matthew Bushell`, SHA-256
  `217887a66749cf2666e6eab726a2e904ceeec76f85285ce5c716eb72ce156ad2`, the same
  as 0.1 and 0.2. apksigner verified it in CI and again after download.
- Version 0.3 (`versionCode` 3), for arm64 phones, 49 MB. Its SHA-256 is
  `7db3bca9c3e37200fc6a07c5fdcff765c2c8bd93087ba47eb7d3090db58bb959`.
- Permissions: the internet, the microphone, and one AndroidX adds for the
  app's own receivers.
- Built by the release run on main, after all its tests passed again: the
  core on Linux and Windows, the golden calls regenerated from the original,
  1,000 fresh random cases, the one-way-out tests, and the 17 on-device
  tests.

### Not yet checked

- The bench on Matthew's Pixel: the companion's figures there, and what the
  engine allows on its GPU (and its NPU, if it has one).
- "Post now" on the phone, back online after a post that couldn't go.
- Speaking a line on the phone since 0.3.5, and Gboard's voice typing
  beside it. First result: busy with the keyboard open (see "Next", 2).
- Sharing with real apps, and speaking on a phone that recognises speech on
  the device.

Checked on Matthew's Pixel: the model's download over Wi-Fi; posts to the
live beach (the hello world by hand, entries 168 and 169 of his pool); a
post offline failing plainly, with nothing sent (0.3.2).

### Next

1. Matthew installs `pocket-beach-v0.4.2.apk` over 0.4.1. Then:
   - **Ask her things, one after another,** in one visit to Ask: a question,
     a read ("What's in my garden block?"), an act ("Under the autumn line
     in garden, note …"). Her words should begin to show within about 2 s
     and stream; an act shows its writes for Yes.
   - **Tell her something worth keeping:** "I'm going to make a million
     pound…" with Million there. She should offer an add to Million; if she
     only talks, the app offers "Keep this in Million?". And something
     that fits no block: she should offer a new one.
   - **Show timings** (Settings, from the Shore): under every answer, the
     time and GPU or CPU; off, nothing.
   - **Speaking a line with the keyboard up,** in Ask: the keyboard should
     go, and listening start. If it still says speech is busy, the message
     gives the phone's error number (10 or 8): send it back.
   - **The bench, if he likes** (about half an hour): its `bench-` block,
     with "Save this block as a file", and the file attached.
2. **0.5's item 7: what next, his to choose** (see "Item 7 on the
   laptop"). The pocket's copying holds; E2B's sorting does not. The ways
   on:
   - **The pocket reckons "when"** from the time words a piece carries
     (tomorrow, a weekday, a date, a season, "one day"), as plain code,
     and "what's ahead" follows from it. She judges only the day's other
     places, and whether a piece is something he'll do.
   - **A bigger Gemma:** E4B on the laptop first, then whether it runs on
     his Pixel, and how fast.
   - **His hand corrects it:** the landing shows as one proposal, any line
     untickable, with a way to move a line to another place.
   - The spoken telling, his `marathon` block, is still to come: it is
     needed for the runs either way.
3. **Later:** the speech service and Gboard, if hiding the keyboard isn't
   enough (no source documents the two competing; the error number will
   say which way it is turned down).

### Running the core on a laptop

The core builds and tests with a JDK alone, version 17 or later, on Windows,
Mac or Linux. From the `pocket-beach` folder:

- Windows: `gradlew.bat :bsp:test -PcoreOnly`
- Mac or Linux: `./gradlew :bsp:test -PcoreOnly`

`-PcoreOnly` leaves out the Android app, so no Android SDK is needed. A laptop
front end, still to be written, would call
`PocketBsp(FileStorage(folder), Shelf(DirShelfSource(shelfFolder)))`, exactly
as the phone calls it with its own folder and assets.

The companion's trial needs the model file, fetched once:

- `./gradlew :trial:run -Ptrial -PcoreOnly --args="<path to gemma-4-E2B-it.litertlm> 8"`
- It runs the hello world eight times, eight ways of asking, and prints each
  answer and whether it checks out.

### The signing key

Matthew made it on his own computer on 28 October: a PKCS12 (`.p12`) file made
with OpenSSL, with the same password for the store and the key. He put it in
as four repository secrets. It never passed through a Claude session.

Every release must be signed with this one key, or Android will refuse to
install an update over the app. If the key is ever lost, the only way forward
is to uninstall, which erases the blocks. So keep two things safe for good:
the key file and its password. Keep both in a password manager, and a second
copy of the file somewhere trusted. Never put either in the repo, a block or a
chat, including a chat with Claude.

The release reads four secrets (Settings, then Secrets and variables, then
Actions):

- `POCKET_KEYSTORE_B64` — the file as base64 text, on one line;
- `POCKET_KEYSTORE_PASSWORD` — its password;
- `POCKET_KEY_ALIAS` — the alias the file was made with;
- `POCKET_KEY_PASSWORD` — the same password again.

To put the file in as text again:

- Mac: `base64 -i <file>.p12 | pbcopy`
- Windows: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\<file>.p12")) | Set-Clipboard`

### Starting a release

1. **For any release after 0.1, raise the version first.** Set `versionName`
   (and `versionCode`, one higher) in `app/build.gradle.kts`, merged into main.
2. **On GitHub, go to Actions, then pocket-beach, then "Run workflow".** Choose
   the branch `main`, type the version (`0.1`), and run it. A Claude session can
   start it too.
3. **CI runs every check** (the golden calls, Windows, the emulator). The
   release job then makes the tag `pocket-beach-v<version>` at the commit it
   built, signs the APK, checks the signature, and publishes it on the Releases
   page.

The release job refuses, and says why, when:

- it is started anywhere but main;
- the version doesn't look like `0.1`, or already has its tag;
- the version isn't the app's `versionName`;
- a signing secret is missing.

Pushing a tag `pocket-beach-v<version>` by hand starts the same release. A
release run is never cancelled by a later push.

## Wearing a shell offline

The app wears pscale shells on the phone, offline, with its behaviour taken
from blocks. Everything a shell needs to be worn offline, including gathering
the blocks it points to, was built and matched against the original before
Piece 3 (decisions 8–12). This is what the original does, read at the pinned
bsp-mcp-server `27908e1`.

### What the original does

- **The left-out bundle window is the shell's gathering mechanism.** It is one
  set of functions, in `src/bundle.ts`. A plain bsp read of a bundle uses it,
  and so does the door that wears a shell (`pscale_play`) when it gathers the
  shell's manifest. Upstream states it outright: the door compiles the
  manifest "through the same functions a bare bsp() read of the node uses — so
  the door and the read cannot diverge."
- **What a bundle is.** A node whose digit children are mostly references:
  - `name`, `name:address`, or `name:address:attention`;
  - `name:head`, the newest entry of an accumulator;
  - `name:now`, the clock's current beat.

  It counts as a bundle only if at least one reference names a block that
  exists. An entry with an arrival stamp is never a bundle.
- **How it gathers.**
  - In a shell, a bare name tries `<name>:<handle>` first, then `<name>`.
    In `shell:matthew`, `history` means `history:matthew`.
  - The teaching shelf is looked in before the region.
  - A whole block over 6,000 characters arrives as its disc at pscale 0.
  - The window holds at most 30,000 characters. Members that don't fit are
    named, never dropped.
  - Prose rides through as written. A missing block is marked absent.
- **Where a shell keeps its bundle:** position 3 of `shell:<handle>` by
  convention, or else the first position that reads as a bundle.
- **The bundle follows the record.** When a handle appends to one of its own
  blocks that its manifest dials, the manifest's slot moves to the new entry.
  Upstream does this only when a secret is passed.
- **Wearing is more than the bundle.** Besides the manifest, the door:
  - carries the passport whole, or, for a shell with no manifest, whichever
    of six standard blocks exist;
  - engages the room pool: its operating directive and the live scene;
  - compiles `frame:<room>` and sweeps `order:<handle>`, through a second
    gatherer, `src/compile.ts`, built on the agent kernel's own gathering;
  - adds completions beside the window, where the bundle lacks one of four
    things (relation, identity, situation, stance);
  - lists who is here and the beach's lighthouse;
  - seats the session: from then on, every reply carries what moved, since it
    was last told, in the blocks the bundle dials (`src/orientation.ts`).
- **A third door, `pscale_genus`,** wears a genus-one agent's mind: the shell
  grown to eighteen blocks (shell-genome 6). It has its own reader,
  `src/genus.ts`, a port of the Python kernel held to byte parity with it.
- **The beach takes no part.** pscale-beach does no bundling; it is all in
  bsp-mcp-server.
- **The clock is inside it.** Every bundle window's first line carries the
  sundial stamp and its voicing, and a `now` reference walks the clock, so the
  port needs part of `temporal.ts`.

### What is built

1. **`bundle.ts` in full, in the core** (`Bundle.kt`, with `parseReference`
   from genus.ts):
   - the references, `head` and `now`;
   - the gathering, `<name>:<hand>` first, the shelf before the pocket;
   - the 6,000-character disc and the 30,000-character window;
   - the window on ordinary reads.

   The clock functions it needs are `Temporal.kt`.
2. **The shell door for the pocket** (`Shells.kt`, decision 9).
   `Shells(storage, shelf).wear(shell, now)` gives:
   - the shell's opening line;
   - one plain line each for the gate, genesis, the room's live scene and
     who is here, left out offline (decision 10);
   - then the shell's own context, laid out as the door lays it out:
     - **with a manifest,** each member it dials, and the wearer seated, so
       later replies carry what moved (`Seat.kt`, decision 11);
     - **with no manifest,** the shell whole, beside whichever passport,
       witnessed, knows, history and stash blocks stand for its hand.
3. **A shell's list following new entries without a secret** (decision 12).
4. **Golden calls for both,** generated by the original at the pinned commits:
   - **Five bundle cases (32 calls)** cover bare names, `head`, `now`, absent
     members, prose, the cut, the cap, and the list following an append.
     They run after the random cases, so the 256 earlier cases are
     unchanged.
   - **5,497 unit checks** call the original's own functions directly, at a
     fixed time (decision 11):
     - 4,482 on the clock;
     - 413 on references and forms;
     - 92 on the clock range;
     - 480 on bundles;
     - 30 on the seat.
   - **Secrets** (decision 12): the original is given a secret on the
     appends the list follows, and the pocket is not. They match.

`pocket-companion` has no manifest, so the companion wears it laid out whole.
Its instruction then gives the shell's lines once more, plainly, without the
escaping a block's JSON carries, which the small model reads more surely (see
"Piece 3: how the companion is asked").

### Left out, or not built

- **Not reachable offline** (decision 10): the gate, genesis, the room's live
  scene, who is here. Each is one plain line in the window. References to
  other beaches pass through as written.
- **Not needed by a pocket shell:**
  - `frame:<room>`, and the room's operating directive. Decision 10 keeps the
    directive, but the pocket has no rooms, so there is none to carry.
  - The lighthouse, and the lines that place a session on a beach.
  - The cast, and the situation from `now:` and `torus-mirror:`.
- **Not built yet, and not needed today:**
  - the completions, which run only over a manifest's members;
  - the `order:<hand>` sweep, which matters only once that block exists;
  - the folding of grown witnessed and history accounts, which lay out whole
    for now.
- **Quirks of the original, matched as they are:**
  - The manifest follows appends only for a shell named `shell:<hand>`, and
    only for references written `block:address:attention`.
  - A change stamped at the very millisecond the rider last looked is never
    reported.
  - The relation tags the original adds to floor-10 addresses are left out,
    as decision 5 leaves them out of every reply. So near the 30,000-character
    cap the pocket's window can hold a little more.

## Decisions (Matthew, 28 October 2026)

These answer the concerns in `FINDINGS.md` §6 and bind every piece.

1. **Only a block's root wraps past nine.** The pocket follows the beach's code.
   A position inside a block holds nine and refuses a tenth. Appending to a
   block's root supernests when it is full. Matthew's test in Piece 2 becomes
   "add ten lines to a block's root and see it wrap".
2. **`agent_id: "pocket"` is Matthew's phone region.** This is a deliberate
   difference from bsp-mcp-server, where "pocket" would be a handle on the
   beach. Block names stay as typed, and an omitted block lists the region.
   - URLs, other bare names, `sed:` and `grain:` are refused with "The pocket
     can't reach the beach in this version."
   - Block names starting `sed:` or `grain:` get the same refusal, because those
     substrates live only on the beach.
3. **No Android backup.** Android's cloud backup and device-to-device transfer
   are both off. "Save a copy" is the only copy; moving to a new phone means
   Save a copy, then Restore. In Piece 2 the Shore shows when Matthew last saved
   a copy, with a reminder once three days have passed.
4. **Signing key.** Matthew makes it on his own computer. It never passes
   through a Claude session. Made on 28 October: an OpenSSL `.p12`, one
   password for store and key.
5. **Lines left out of every reply:** the clock line (`now · …`), the
   "days ago / AHEAD" tags, and the who's-here lines (the reflection and
   orientation). Inside a worn shell the clock stays (decision 11).
6. **The assistant (Piece 3) proposes a list of plain steps with one Yes.**
   Every step is validated before any step runs. If one fails, it stops and
   says exactly which steps ran.
7. **The local bsp is its own pure Kotlin module.** It has no Android
   dependencies: no Android APIs, no Context. Storage goes through an
   interface, so the same tested core can later run on a Windows laptop. The
   Android app uses it through that interface.
   - Kept by a test that fails if any core class names an Android API.
   - Kept by a CI job that runs the core's tests on Windows.
8. **A shell can be worn offline before Piece 3.** The app will wear pscale
   shells locally, offline, with its behaviour taken from blocks. Everything a
   shell needs to be worn offline, including gathering the blocks it points
   to, is built and matched against the original before Piece 3. See "Wearing
   a shell offline".
9. **The shell door first.** The door that wears a shell (`pscale_play`) is
   built first. The agent door (`pscale_genus`) comes later, if at all.
10. **Offline, four parts of wearing are left out:** the room's live scene,
    who's here, the gate, and genesis. Each says so plainly where it would
    appear.
    - The room's operating directive is a block, so it stays.
    - References to other beaches pass through as written, as the original
      does when it can't reach a beach.
11. **The clock stays inside a worn shell,** as in the original:
    - the sundial stamp on a bundle window's first line;
    - `now` references;
    - the "what moved" line after a shell is worn.

    The golden tests fix the clock at a set time for each call, so results
    repeat.
12. **A shell's list follows new entries without a secret.** When a block the
    list points to gets a new entry, the list moves to it. On the phone, the
    phone is the latch.
13. **Piece 3: the model and memory.**
    - The model is loaded only when Ask is used, and released when idle.
    - Before the model download is offered, the app checks the phone has
      enough memory, and says plainly if it doesn't.
14. **Later: what other people send is data, never instructions.** Content that
    arrives from other people (blocks read from the online beach, relay
    requests) is untrusted. It can be read, but it never leads to a write
    without Matthew's confirmation. See "Later".
15. **Piece 3: every Ask is logged.** Each Ask leaves an entry in a local log
    block: what Matthew asked, the calls proposed, whether he confirmed, and the
    time taken.
    - "Not now" offers one tap for why: wrong place, wrong block, or
      misunderstood.
    - The log is an ordinary block, so it goes out with every saved copy, and
      the assistant's shell can be refined from real use.
16. **Speaking is built in.** A microphone on Ask (Piece 3) and on the Write
    screen, so lines can be spoken straight into a block.
    - Only Android's on-device speech recognition is used, never a server's.
    - If the phone can't recognise speech on the device, the app says so
      plainly and does not fall back to the cloud.
    - The Write screen's microphone comes in Piece 2: it was simple, one
      Android call that makes an on-device recogniser.
17. **Today's finish line is a hello-world test across every node,** built
    after Pieces 2 and 3. See "Today's finish line".
18. **One narrow way out, early.** The app may append one message to a pool on
    the online beach, only after Matthew taps to confirm, with nothing else
    able to reach the network. It is a deliberate early piece of "Reach the
    beach".
    - It widens principle 5 by this one confirmed message: until now nothing
      left the phone but the model download and links Matthew taps.
    - Decision 2 stands for bsp(): a URL `agent_id` is still refused. The
      message goes by its own action, never through bsp().
    - Appending to `pool:Phenomemental` needs no key (see "Today's finish
      line"). If a pool ever needs one, that key is kept in the Android
      Keystore, never Matthew's passport key.
19. **Piece 3: share a block out, receive a block in,** through Android's share
    menu, so a block (the Ask log, say, or a coaching block) can move between
    the pocket and other apps without the beach.
    - Sharing out is Matthew's act, like saving a copy: the block goes only
      where he sends it.
    - A block received arrives as data (decision 14). Keeping it in the pocket
      is a write he taps, and adopting it into a shell is a separate, explicit
      choice of his.

## 0.5

Matthew's plan for 0.5 is not all in STATUS yet: only its item 7, below
(29 October). Items 1 to 6 are to be added when he gives them.

### Item 7: one telling, three landings (a pocket test)

Build this test, then the shape that passes it.

**What it proves.** Someone tells their day once, in whatever order it
comes, and the pocket, offline, hands it back sorted and landed:
- the day six ways;
- the forward bits on the hand's shelves;
- the rungs above refreshed.

If Gemma does it from the block alone, the sophistication is in the
structure, not the model. The shapes are those of his now and his hand on
the beach, so it lands the same whichever door he comes through.

**The structure** is a block in the pocket, not her instruction. The six are
P6's points in plain words (Facts, Feelings, Fictions, Purpose, Decision,
Outcomes):

```
{"_": "One telling, three landings: my day, told once in any order, kept in my pocket. In the day and the hand every line is my own words, copied exactly; a piece may stand in more than one place; nothing is added.",
 "1": {"_": "THE DAY: what I said, sorted six ways",
  "1": "What happened: what could be seen or checked, before any meaning",
  "2": "What I felt: in my body and in me",
  "3": "What I made of it: my conclusions, judgements and the stories I tell",
  "4": "What matters to me: how I want to be",
  "5": "What I chose: the choice I've made",
  "6": "What's ahead: what's coming, planned or expected"},
 "2": {"_": "THE HAND: what I say I'll do, one card each, on the shelf for when; a card already there is updated, not doubled",
  "1": "Tomorrow", "2": "This week", "3": "This month", "4": "This season", "5": "This year or beyond"},
 "3": {"_": "THE RUNGS ABOVE: one line each in my own voice, as what I intend, drawn only from what I said",
  "1": "This week", "2": "This month", "3": "This season", "4": "This year"}}
```

She is given this block, today's date and what he said, nothing else. She
gives the block back filled: each piece of his words, copied, as a line
beneath every place it belongs.

**What the pocket checks** before anything lands:
- every line in the day and the hand must be found in what he said, or it
  is refused as invented;
- every sentence must land somewhere in the day, or it is listed as
  dropped.

**Where it lands,** for one Yes:
- The day goes at today's place on his clock: the telling whole as the
  day's line, with the six kept with it. The digits beneath a day on the
  clock are its day-parts, so block-conventions says where the six go.
- Cards go on the hand's shelves, and a card already there is updated, not
  doubled.
- The rung lines are refreshed in his voice.

In Ask, she replies to his day first, in her own words. The landings show
under her reply as one proposal, and any line can be unticked.

**The telling, written:** "Right, so the car wouldn't start again this
morning, which meant the bus, which meant I was twenty minutes late and
walked into that meeting with everyone already looking at me. I've been
putting that car off for weeks and I know it, same as the tax, same as the
garden. Anyway Jo's birthday is on the twelfth so I need to think about that
at some point. The meeting went on forever but Sam said something that
stuck with me, that I'm always the one apologising. Felt rotten about the
morning, that hot-faced thing. Walked home the long way by the water and
felt lighter by the end of it. Dentist on Thursday, I keep forgetting. I've
decided I'm ringing the garage tomorrow, not fiddling with it myself again.
Before the frost comes I want the garden shut down properly for once. The
thing I keep coming back to is the kids seeing me calm, not this. One day I
want a job where mornings aren't a fight."

And spoken, unpunctuated, as his phone heard it: his `marathon` block (not
yet received here). The bench fixes the date at Thursday 29 October 2026,
so the key holds.

**The key.** The day:
- What happened: car, bus, late, the meeting, Sam's line, the walk home.
- What I felt: rotten, hot-faced; lighter after the walk.
- What I made of it: putting things off (car, tax, garden); Sam's line
  about apologising.
- What I chose: ringing the garage, not fixing it myself.
- What matters to me: the kids seeing me calm.
- What's ahead: Jo's birthday, the dentist, the garage, the garden.

The hand:
- Tomorrow: the garage.
- This week: the dentist, Thursday.
- This month: Jo's birthday on the twelfth, two weeks off.
- This season: the garden before the frost.
- This year or beyond: a job where mornings aren't a fight.

The rungs are judged by Matthew, not matched.

**The marks:**
- Nothing invented.
- Nothing dropped.
- The three doubles handled:
  - the garage as both what he chose and what's ahead;
  - Sam's line as both what happened and what he made of it;
  - the twelfth on one shelf with the date's reason, or on two.
- Every line in the first person. The day and the hand are copies, so that
  comes free there; the rung lines are checked for "you".

**The second telling,** straight after: "Rang the garage first thing,
they're coming Monday." The garage card lands as done and Monday is kept;
there is no second garage card.

**The runs, in order:**
1. In the pocket's own core on the laptop: five runs of each telling.
   Beside them, the same Gemma with no pocket: his six names and shelves as
   plain text, then the paragraph. Both sets of marks are reported; the
   difference is what the structure adds.
2. On his Pixel's bench, timed.
3. Then he speaks it in Ask.

If the one-pass fill drops pieces or misses the doubles, the phone cuts the
telling into pieces, copying and never changing a word, and she labels each
piece with its places, one short choice at a time.

### Item 7 on the laptop (29 October)

Built in the pocket's core (`Telling`, with tests): the block, what she is
given, reading her answer, the pocket's check (invented, dropped), the
hand's cards updated not doubled, the key and the marks. The laptop trial
(`trial … telling`) runs each shape on the written telling, then the second
telling after it lands. Each shape is run once as the phone asks (the
likeliest word each time), then five times with Gemma's own sampling.
Gemma 4 E2B is run on the laptop's CPU, with the same model file as the
phone. The spoken telling (his `marathon` block) has not reached this
session yet.

There are 11 marks:
- nothing invented;
- nothing dropped;
- the day by the key (17 pieces);
- the hand by the key (5 cards);
- the three doubles;
- the first person;
- for the second telling: one garage card, done, Monday kept.

| Shape | Marks, six runs | What happened |
|---|---|---|
| One pass: the block, the date, what he said | 1 to 4 of 11 | Nothing invented, but barely sorted. Greedy, she numbered his sentences in the order he said them; sampled, she filled one line a place, as near-JSON. The day 0 to 5 of 17, the hand 0 to 3 of 5, 1 to 11 sentences dropped. |
| Plain Gemma: his names, then the paragraph | 0 to 4 of 11 | She sorts the meaning roughly right, but in her own words and as "you": 10 to 13 lines refused as not his, every run. |
| A piece at a time, in one conversation | 4 to 5 of 11 | Better in the day (10 to 11 of 17), but she drifts, repeating her earlier answers ("1.1, 1.3, 1.3, 1.3"). |
| Each piece alone, a conversation of its own | 3 to 5 of 11 | Nothing invented and nothing dropped in all six. The day 10 to 11 of 17 (one run 4); the hand 0 to 1 of 5; Sam's double in three runs; the garage and the twelfth never. About 30 s for 25 pieces on the laptop's CPU. |
| Others, one run each | 3 to 5 of 11 | Each piece answered in words (3), all the pieces numbered at once (4), and her plain answer anchored back to his words (3 to 5). |

What it says:
- **Copying holds.** When the phone cuts the telling into pieces, nothing
  is invented and nothing dropped, every run. Plain Gemma never copies.
- **The sorting doesn't, with Gemma 4 E2B.** Whatever the shape, she
  rarely picks what matters to him, what he chose or what's ahead. She
  almost never puts a forward piece on its right shelf:
  - "Dentist on Thursday" should go on this week;
  - "the twelfth" on this month;
  - "before the frost" on this season;
  - "one day" on this year or beyond.
- **The second telling** seldom lands on the garage card.
- **The claim isn't met.** "From the block alone" does not hold for E2B on
  this telling. The structure carries the copying, but not the judgement.

The choices this leaves are Matthew's (see "Next").

## 0.4: as fast and dependable as the phone allows

Matthew's plan (28 October). The principle: Gemma states intent in as few
tokens as possible; code does everything mechanical. Every change is judged
by the bench on his Pixel, before and after.

1. **The bench first** (0.3.4).
2. **A compact action language:** short actions (new block, change, add
   beneath, read, post) that name lines by quoting their text, never by
   address. The app resolves a quote to its line, opens up a plain line
   itself, and builds the bsp() calls, all shown before Yes. A quote that
   matches more than one line is put to Matthew.
3. **Routing:** a one-word first choice (talk, read, act) held by the engine;
   when unsure, talk.
4. **Talk and read stream** token by token, with speculative decoding on and
   nothing held.
5. **The model stays loaded while Ask is open,** the shell and instructions
   processed once, and the last six exchanges kept.
6. **The NPU against the GPU** on the bench; the faster is used, and shown.
7. **FunctionGemma for the act route,** adopted only if both faster and more
   accurate.

The shell's hello world line moves to the action language.

**What the engine allows** (LiteRT-LM 0.18.0 and 0.18.1, tried on the
laptop):

- **Holding an answer to a form, and speculative decoding, don't mix.**
  - Held to a JSON schema or a regex with speculative decoding on, the
    engine stops mid-answer ("compute_mask() called after stop").
  - Once any speculative session has run, even a held one-word choice fails
    that way. The conversation is then dead for every later turn.
  - The bench tries this on the phone's GPU too.
- **A held form stays held** for the whole conversation. A later turn asked
  without it is still held to it.
- **With speculative decoding off, holding works** (the laptop, 0.18.0).
  - It holds the first word to talk, read or act, and lets anything follow.
  - It costs about 2 to 3.5 s once per conversation, to set up the form,
    and nothing after: each later turn's first token came as fast as free,
    and writing ran as fast.
  - On the Pixel's GPU, 0.3.4's probe tried it with speculative decoding on
    only, and it failed. The 0.4 bench says how it does with it off.
- **A reply stopped part way ends its conversation:** the next ask in it is
  cancelled at once. Ask opens a new one after a stop.
- **One conversation kept open** processes its instruction once; each later
  turn adds only its own words. No release lets a processed instruction be
  copied for a fresh conversation.
- **The NPU:** builds of Gemma 4 E2B exist only for Google Tensor G5 and G6,
  as separate files of 3.1 and 3.3 GB. There is also a GPU-only build,
  2.0 GB against 2.6.
- **FunctionGemma:** Google's base model is gated on Hugging Face (its
  licence must be accepted by an account), and comes only as weights to
  convert. The ready-made `.litertlm` builds are fine-tuned for other tasks
  (phone actions, a garden game).

## Piece 3: how the companion is asked

Found by running the hello world against the real model on a laptop
(`trial/`), round by round, and kept:

- **One JSON object an answer, held to a schema** by the engine's constrained
  decoding:
  - `say`: what it says;
  - `steps`: each with `do` (read, make, add or change), `block`, `at` and
    `text`;
  - `post`: optional, with `pool` and `message`.

  All four step fields are required. When `text` was optional, the model put
  the line's words where the block's name goes.
- **Two short examples in the instruction,** and the rule "a step writes one
  line". Without them it folded several lines into one.
- **The hello world is in the shell as the answer itself,** to be copied with
  the secret written in. A description of the steps, plain or set out as
  lines beneath line 1, was followed less surely (2 of 4 runs passed, then
  0 of 2).
- **The block's address is written out** (`pocket/hello-world`), because the
  model filled the `{{address}}` gap itself, with the bare name.
- **The likeliest word each time** (top-k 1), a context of 4,096 tokens, and
  answers of at most 1,024. Thinking is off.
- **The pocket's blocks are shown with their lines,** each at pscale 0 with
  its address, and the teaching blocks are named. Shown only the opening
  lines, it placed "under the autumn line" by guess.
- **A post only when asked.** On ordinary asks the model offered a post with
  nearly every answer ("Done. Have a great day!"), and an unasked post kept a
  read from running. A post is now kept only when Matthew's words speak of
  posting, a pool, the beach or the hello world. Any other is left out and
  logged. An empty post, or one to no pool, is none.
- **The hello world's seal, fingerprint and message are the app's** (0.3.2).
  Shown an old `hello-world` with its lines, the model copied its seal and
  fingerprint into the post. So the app now writes the seal and the
  fingerprint itself after the companion's writes, and makes the post from
  the block once written. The shell's post is only `"Hello world"`, which
  also spares the model about 70 tokens of writing.
- **A hello world already there is said so,** with the one step that
  changes its secret at its line's address, only when there is one. Put in
  the shell as a second answer for that case, the model chose it on an
  empty pocket too and copied "the Secret line's address" as it stood.
- **What it reads and writes, and how fast** (the laptop's CPU, 4 cores,
  0.3.2's instruction):
  - it reads about 1,350 tokens (4,700 characters): the instructions and
    two examples, about 580; the shell twice, worn and plainly, about 620;
    the pocket's blocks and the teaching blocks' names, about 150;
  - the hello world's answer is about 240 tokens; with `hello-world` already
    there, about 95;
  - reading ran at 100–250 tokens a second and writing at 6–11, so writing
    is most of the time: 25–40 s an answer here;
  - the phone's GPU should read many times faster and write several times
    faster. Ask now shows the phone's own figures.
- **Speculative decoding, not used yet.** The model file supports it (its
  multi-token prediction). On the laptop it wrote 17.4 tokens a second
  against 10.5, but only with the answer's form not held: held to the
  schema, the engine stopped with an error mid-answer ("compute_mask()
  called after stop"). Unheld, the answer sometimes came wrapped in a code
  fence, and its length varied more (170 to 280 tokens, against about 240).
  The hello world passed as often (7 of 8) in about 60% of the time, but ordinary asks went worse: "buy
  more compost" was put under the autumn line rather than at the end, and
  "under the autumn line…" twice put two lines in one step, so nothing could
  be shown. Whether it is worth giving up the held form waits on the
  phone's own figures.
- **Reads end when they repeat.** Asked what a block holds, the model read it
  again each round. A round that asks only for reads it has been shown
  already is taken as its answer.
- **Leniency stops where meaning starts.**
  - The app takes the JSON from around any words the model adds. It takes
    `pocket/name` as `name`, and an `at` that names the block itself as the
    opening.
  - Once in an Ask, the companion is told why and answers again, and the
    answer to that is the one shown. This happens when an answer:
    - can't be read;
    - puts two lines in one step;
    - leaves `HIS SECRET` unfilled;
    - proposes something that can't go ahead (a line for a block it hasn't
      made, say).
  - Anything else is shown, or refused, as it is.

## Other differences from bsp-mcp-server

Chosen in Phase 0, or found while building Piece 1.

- **Reads use bsp-mcp-server's own walker.** That is the path it takes for
  `agent_id="pscale"`, never the beach's second walker. Address labels are
  always full width, and point reads name the newest entry ("head").
- **Locks and encryption are refused.** `secret`, `new_lock`, `gray`,
  `enc_secret` and `members` get a plain message. `face` and `tier` are
  accepted and ignored, as bsp-mcp-server ignores them.
- **`agent_id` may be left out;** it then means "pocket".
- **The beach's wording is kept.** Upstream text comes through word for word,
  with "pocket" where the reference names its beach URL. That includes
  upstream quirks:
  - A refusal that mentions a "block" carries a note about drifted keys,
    because "block" contains "lock".
  - A write that makes a new block reports "landed at" with a leading dot
    (`.1`).
- **Bundle windows are built,** as wearing a shell needs them (decision 8): a
  read of a position whose lines are mostly references to blocks that exist
  carries the window compiling them, matched against the original. No
  teaching block is shaped like one.
- **Floor-10 blocks** (the clock's form) get no clock framing beneath a write,
  part of decision 5.
- **A block named `presence`** is swept of stale entries as the beach sweeps
  it, but only by ISO dates. The beach would also read some other date
  spellings.
- **Upstream is pinned:** bsp-mcp-server `27908e1`, pscale-beach `4770a49`.
  Matthew decides when to move to newer commits.
- **CI also runs when only its own workflow file changes,** so a change to the
  build is itself tested.

## Today's finish line: hello world

A test across every node (decision 17), built in 0.3:

1. **On the phone,** the companion (Gemma wearing `pocket-companion`) makes a
   block `hello-world` in the pocket. Beneath its opening line it holds four
   lines:
   - the shell's opening line;
   - a fun secret Matthew gives it;
   - a seal the app makes at random, which neither Matthew nor the model
     chooses;
   - a short fingerprint of the whole block, computed by the app.

   The fingerprint is of the block as it stands before that last line, since
   a line can't carry a fingerprint of itself. `HelloWorld.verify` checks it.
2. **With Matthew's confirmation,** the app posts one message to
   `pool:Phenomemental` on https://beach.happyseaurchin.com, under the name
   Phenomemental:

   "Hello world from the pocket. Shell: “…” Secret: …. Seal: …. Fingerprint:
   …. Block: pocket/hello-world."

   It carries the shell's opening line, the secret, the seal, the fingerprint
   and the block's pocket address. The app makes it from the block as it
   stands once written, never from the companion's words.

Ask checks the result: whether `hello-world` holds the shell's opening line,
a secret, the seal the app made, and a fingerprint that matches. Both steps
can also be done by hand from the test screen, so the chain can be shown
without the companion.

### Does appending to the pool need a key? No.

- **The beach's code** (pscale-beach `4770a49`, `api/pscale-beach.js`): an
  append at a block's root needs a key only when the root (`_`) is locked.
  - The rule that ties a block to its handle's passport governs setting a
    lock, not appending.
  - With no lock, only the beach's caps apply (size, appends a minute), and
    they are off unless the operator turns them on.
- **The conventions** (block-conventions 4.9): `pool:<handle>` is the handle's
  own room, where visitors' words land as entries under their own names.
- **The pool itself**, read on 28 October, opens "open, no key needed to leave
  something here". It holds 164 entries, left by weft, David, Dwayne and
  Matthew's agents.
- So the app sends no key. If the beach ever answers that a key is required,
  the app says so plainly and stops.

### How the message goes

- **One HTTPS POST** to `https://beach.happyseaurchin.com/.well-known/pscale-beach`:
  `{block: "pool:Phenomemental", content: {_: <the message>, 1: <author>}, append: true}`.
  This is the shape bsp-mcp-server's pool tool sends, and the beach adds the
  arrival stamp.
- **Only after a tap,** on a screen that shows the whole message first.
- **Nothing else reaches the network:**
  - one class holds the only network code, with the beach's address fixed in
    it, and a test fails the build if any other class names a network API;
  - the app's network security settings trust no certificate for any other
    host but the four the model's one download needs, and allow no
    unencrypted traffic (principle 5).
- **The seal:** 128 random bits from the phone's secure random source.
- **The fingerprint:** the first 16 hex digits of the SHA-256 of the block's
  stored text.

## Later

Work after Piece 3. Nothing here is built yet, except the first piece of
"Reach the beach", which comes early for the finish line (decision 18).

- **What other people send is data, never instructions** (decision 14). This
  covers blocks read from the online beach and relay requests. Such content can
  be read, but it never leads to a write without Matthew's confirmation.
- **A "Reach the beach" switch, off by default.** When it is on, URL
  `agent_id`s go through to the online beach over HTTPS.
  - Sealed (gray) blocks are handled by porting the beach's client-side
    encryption.
  - The keys are kept in the Android Keystore, one per block, and never
    Matthew's passport key.
  - To settle when this starts:
    - the app then needs the internet permission, which it has so far gone
      without, so principles 1 and 5 change for that version;
    - a key made inside the Keystore can never leave the phone, so a block
      sealed with it opens only on this phone and is lost with it. The port
      decides whether the Keystore holds each block's key itself, or guards a
      key Matthew can also keep elsewhere.
- **A relay through a sealed room on the online beach.** Claude or Matthew's
  agents leave requests there.
  - The app checks the room every few seconds while it is open, and about every
    15 minutes in the background. Fifteen minutes is the shortest regular
    interval Android allows.
  - It answers through the local bsp, and clears each answer once it has been
    read.
  - Requests from others are reads only unless Matthew confirms (decision 14).
  - It needs "Reach the beach" on.
- **Gathering for Matthew's agents.** The app reads what each agent would read,
  notes what is new, and leaves each a short note, so their wakes cost fewer
  tokens.
  - To settle when this starts: where the notes live, and whether leaving one
    needs Matthew's tap each time (principle 6) or a standing permission for
    that one kind of write.

## Open questions

None waiting. Answered on 28 October:

- The pool entry carries the name `Phenomemental`.
- The companion wears `pocket-companion`.
