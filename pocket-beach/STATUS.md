# Pocket beach — status

Current state only. Newest first.

## Now

Piece 3, the companion, is built, and with it today's finish line, the hello
world. Release 0.3 is next (see "Next"). Before it, wearing a shell offline was
built and matched against the original (decisions 8–12).

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
  - With the memory and 3.1 GB free, Ask offers "Get the model, 2.6 GB over
    Wi-Fi". Android's download manager fetches one pinned file over Wi-Fi
    only: `litert-community/gemma-4-E2B-it-litert-lm`, revision `6e5c4f1e`.
    The app checks it byte for byte against its SHA-256
    (`18193810…7139a63c`) before it is ever loaded.
  - It loads when Matthew asks, on the GPU, or on the CPU if the GPU can't
    take it. It is let go after three idle minutes, or as soon as Android
    asks for memory back.
- **The one way out** (decision 18).
  - `net/HttpsWire.kt` is the only code in the app that opens a connection.
    It makes one POST of one message to a pool on the beach, only after
    Matthew taps to confirm. It takes no address but the beach's pool
    append, follows no redirect, and keeps no cookie or cache.
  - The posts go only to `pool:Phenomemental`, under the name Phenomemental.
    No key is needed (see "Today's finish line"). If the beach ever asks for
    one, nothing is sent and it says so.
  - The network security config trusts no certificate for any host but
    `beach.happyseaurchin.com`, and refuses plain HTTP everywhere.
  - The model's download goes through Android's download manager, outside
    the app's process, so the config names only the beach.
  - A test fails the build if any other file names a network API, or hands
    the download manager anything but the one pinned file.
- **The hello world by hand**, on the test screen ("Try a call by hand"
  under About), so the chain can be shown without the companion:
  - "1. Make hello-world": the shell's opening line, the secret, a seal the
    app makes and the fingerprint, each a guarded write;
  - "2. Post it to pool:Phenomemental…": shows the whole message, then posts
    it only after its own "Post it".
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

**Piece 2, unchanged:** the Shore, a block, Write, Save a copy, Restore a copy,
and About, in Matthew's book look, with `Pages` and `Copies` beneath them.
**Piece 1, unchanged:** the local bsp, the teaching shelf, storage.

**The app itself:** version 0.3 (`versionCode` 3), Android 12 or later.

- Permissions: the internet, for the one confirmed post and the model's one
  download; the microphone, for speaking.
- Android's backup is off.
- The release is built for arm64 phones only: 49 MB, most of it the engine.

### What was checked

- **The core's tests:** 68, all passing, on the JVM and again on Windows with
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
  - It asked in eight different ways (`gradlew :trial:run -Ptrial`).
  - **With the instruction as built, 7 of the 8 passed.** In each,
    `hello-world` held the shell's opening line, the secret word for word,
    the app's seal and a matching fingerprint. The post carried all four
    and the block's address.
  - **The miss:** "hello world my secret is bananas are berries" wasn't taken
    for the hello world. It proposed a line for a block that doesn't exist,
    so the app refused it, and nothing was written.
  - Earlier forms of the instruction scored 0 to 3 of 4; see "Piece 3: how
    the companion is asked".
- **On a Pixel emulator (Android 16)**, the on-device tests, and the screen
  tour, light and dark: DEVICE_RESULT

### Not yet checked

- The companion on Matthew's phone: whether its GPU takes the model, and how
  quickly it answers.
- The model's download over real Wi-Fi.
- A post to the live beach. No test posts there: the tests post over a wire
  that goes nowhere.
- Sharing with real apps, and speaking on a phone that recognises speech on
  the device.

### Next

1. Matthew installs `pocket-beach-v0.3.apk` over 0.2. Then:
   - **Ask** (on the Shore): make the companion's shell, then get the model
     on Wi-Fi.
   - **The hello world with the companion:** ask "Do the hello world. My
     secret is …". Read what it proposes, then Yes. Read the post, then
     Post it.
   - **The hello world by hand** (About, Try a call by hand), if the
     companion isn't reliable on the phone.
   - Look at `ask-log`, and at the post on the beach.
2. After that: the "Later" list, and what the Ask log shows.

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
- **Leniency stops where meaning starts.** The app takes the JSON from
  around any words the model adds. It takes `pocket/name` as `name`, and an
  `at` that names the block itself as the opening. An answer that can't be
  read is sent back once, with why. Anything else is shown, or refused,
  as it is.

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

   "Hello world from the pocket. Secret: …. Seal: …. Fingerprint: …. Block:
   pocket/hello-world."

   It carries the secret, the seal, the fingerprint and the block's pocket
   address.

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
    host and allow no unencrypted traffic. The model's one download is made
    by Android's download manager, outside the app's process, so the beach is
    the only name they carry (principle 5).
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
