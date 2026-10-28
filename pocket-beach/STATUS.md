# Pocket beach — status

Current state only. Newest first.

## Now

Piece 1 is released: **pocket-beach-v0.1**, on the repo's Releases page since
28 October, signed with Matthew's key. The tag is at `9bd153b` on main. The
local bsp is its own pure Kotlin module (decision 7), and CI is green on
Linux, on Windows and on a Pixel emulator.

A release is started with "Run workflow" on main, and the release job makes
the tag itself (see "Starting a release"). A Claude session can't push a tag,
but it can start this, as it did for 0.1.

Before Piece 3, wearing a shell offline is built and matched against the
original (decisions 8–12), as set out under "Wearing a shell offline".

### What is built

- **The local bsp**, a Kotlin port of bsp-mcp-server and the beach duties, is
  its own module, `bsp/`.
  - It is plain Kotlin on the JVM: no Android APIs, no Context, no Android
    library. It needs only a JDK, so the same tested core can run on a Windows
    laptop.
  - A front end hands it two things through interfaces: a `BlockStorage` for
    the blocks and a `ShelfSource` for the teaching blocks. It gets text back
    from `PocketBsp.call`.
  - It covers the full surface for ordinary blocks: point, path-walk,
    path-walk plus descent, disc, whole block, star (`*`), listing blocks
    (block left out), writes of every shape, and append.
  - Append takes the next free slot, and supernests when a block's root fills.
    A position inside a block holds nine and refuses a tenth.
- **The Android app** (`app/`) uses the core only through those interfaces. It
  supplies a `FileStorage` on its private files and an `AssetShelfSource`
  reading the shelf from its assets.
- **The teaching shelf** (`shelf/`): the 27 blocks bsp-mcp-server bundles,
  under 28 names, byte for byte, with its MIT licence. It is the read-only
  `pscale` region.
- **Storage.** The core ships two storages.
  - `FileStorage` keeps blocks as files: `regions/pocket/<name>.json` under a
    root folder (on the phone, its private files). Each write goes to a
    temporary file and is renamed into place. When each block was touched and
    born is kept beside them, in `regions/pocket.meta.json`.
  - File names are safe on every system. "garden" is `garden.json`; capitals,
    spaces and other marks are written `%XX`, so "Garden" is `%47arden.json`
    and never shares a file with "garden" where case is ignored (Windows, a
    Mac); and Windows' reserved names (con, nul, com1…) are escaped.
  - `MemoryStorage` keeps blocks in memory, for tests and any front end that
    keeps nothing.
- **The test screen.** It has a field for each bsp parameter, a Run button, the
  call sent, the raw result, and how long it took. Two shortcut buttons fill in
  "The whetstone" and "My blocks".
- **The app itself.**
  - It has no internet permission: nothing can leave the phone.
  - Android's cloud backup and device-to-device transfer are both off.
  - App id `com.phenomemental.pocketbeach`. This can never change without
    losing updates.
  - Version 0.1, Android 12 or later, built against Android 17 (SDK 37).

### What was checked

- **Golden calls** (`conformance/`). 833 calls in 256 cases ran through the
  original bsp-mcp-server and pscale-beach at the pinned commits, with the clock
  frozen. The Kotlin gives the identical reply and identical stored blocks for
  every one.
  - The calls are 23 hand-made cases and 233 random ones.
  - Separately, 7,563 more random calls on three other seeds also matched.
  - The test was shown to fail when an expected reply or block is altered.
- **bsp-mcp-server's `smoke:unit`**, ported assertion for assertion, against
  sunstone and whetstone: passes.
- **The pocket's own rules** pass: the refusals, the files, the tenth line
  wrapping the root, and names with spaces, colons and accents.
- **JavaScript's rules** for numbers, escapes, key order, dates and whitespace
  pass, checked against Node 22.
- **The core is pure.** A test reads every compiled class of the core and finds
  no Android API named: 0 of 57 classes, where 7 of the app's 8 name one.
- **The golden replay passes on both storages,** in files and in memory: the
  core answers identically whatever stands beneath it.
- **The core builds and passes its tests with no Android SDK in reach**
  (`-PcoreOnly`).
- **The app builds** on the interfaces: debug, release, and the on-device tests.
- **CI on GitHub** is green, most recently on main at `9608a3c`, after the merge.
  - The golden calls regenerated from the original code match the committed
    ones, and the shelf matches upstream.
  - The Kotlin replays pass, for the corpus and for 1,000 fresh random cases.
  - On Windows, with no Android SDK, every core test passes, and the golden
    replay matches all 833 calls in files and again in memory.
  - On a Pixel emulator running Android 16, the app installed and opened, and
    all 5 on-device tests passed:
    - the whetstone from the shelf;
    - a line written and read back from its own file;
    - the beach refused;
    - the test screen: fields, Run, and the result;
    - the full 833-call golden replay on Android's own runtime.
- **The 0.1 release.**
  - The APK is signed with Matthew's key, certificate `CN=Matthew Bushell`,
    SHA-256 `217887a66749cf2666e6eab726a2e904ceeec76f85285ce5c716eb72ce156ad2`.
  - apksigner verified it in CI, and again after downloading it from the
    Release.
  - It is version 0.1 (`versionCode` 1).
  - It has no internet permission. Its only declared permission is one AndroidX
    adds for the app's own receivers.
- **Starting a release** passes actionlint. The step that picks the tag was run
  locally for five cases:
  - a start on main makes `pocket-beach-v0.1`;
  - a start elsewhere is refused;
  - a malformed version, including one carrying a shell command, is refused;
  - a version that already has its tag is refused;
  - a tag pushed by hand is taken as given.

### Not yet checked

- The app on Matthew's own phone.

### Next

1. Matthew installs `pocket-beach-v0.1.apk` from the Release and tries it:
   - open it and read the whetstone;
   - make a block, write a line, and read it back;
   - then all of it again in aeroplane mode.
2. Before Piece 3: wearing a shell offline is built and matched against the
   original (decisions 8–12).

### Running the core on a laptop

The core builds and tests with a JDK alone, version 17 or later, on Windows,
Mac or Linux. From the `pocket-beach` folder:

- Windows: `gradlew.bat :bsp:test -PcoreOnly`
- Mac or Linux: `./gradlew :bsp:test -PcoreOnly`

`-PcoreOnly` leaves out the Android app, so no Android SDK is needed. A laptop
front end, still to be written, would call
`PocketBsp(FileStorage(folder), Shelf(DirShelfSource(shelfFolder)))`, exactly
as the phone calls it with its own folder and assets.

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

## Wearing a shell offline (before Piece 3)

The app will wear pscale shells on the phone, offline, with its behaviour
taken from blocks. Everything a shell needs to be worn offline, including
gathering the blocks it points to, is built and matched against the original
before Piece 3 (decisions 8–12). This is what the original does, read at the
pinned bsp-mcp-server `27908e1`.

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

### What it means for v0.1

A read of a node shaped like a bundle comes back without its window, so it
differs from the original. No teaching block is shaped like one, and no golden
call covers it.

### What is built before Piece 3

1. **`bundle.ts` in full, in the core:** the references, the gathering, the
   window, `head` and `now`, and the window on ordinary reads.
2. **The shell door, `pscale_play`, for the pocket** (decision 9).
   - Everything the door does from blocks: the manifest gathered, the
     passport, the room's operating directive, the frame, the completions, the
     order sweep, and the lighthouse.
   - The seat: the "what moved" line on later replies.
   - The sundial clock wherever the original uses it (decision 11).
   - The list following new entries without a secret (decision 12).
   - Left out offline, and said plainly where each would appear (decision 10):
     the room's live scene, who's here, the gate, and genesis.
   - References to other beaches can't be reached offline. They pass through
     as written, which is what the original does when it can't reach a beach.
3. **Golden calls for both,** generated by the original code at the pinned
   commits as now. They cover:
   - shells with bare names, `head` and `now`;
   - absent members and prose;
   - the 6,000-character cut and the window cap;
   - the list following an append.

   Each call runs at a fixed time, so the results repeat (decision 11). For the
   list following an append, the original is given a secret and the pocket is
   not (decision 12); that this matches is confirmed when it is built. Where a
   part is left out (decision 10), the test compares everything else and checks
   the plain note in its place. Any other mismatch fails the build.

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
- **Bundle windows are left out of v0.1 only.** In bsp-mcp-server, reading a
  position whose lines are mostly references to other blocks that exist adds a
  window compiling those blocks. No teaching block triggers it. It is the
  shell's gathering mechanism, so it is built before Piece 3 (decision 8).
- **Floor-10 blocks** (the clock's form) get no clock framing beneath a write,
  part of decision 5.
- **A block named `presence`** is swept of stale entries as the beach sweeps
  it, but only by ISO dates. The beach would also read some other date
  spellings.
- **Upstream is pinned:** bsp-mcp-server `27908e1`, pscale-beach `4770a49`.
  Matthew decides when to move to newer commits.
- **CI also runs when only its own workflow file changes,** so a change to the
  build is itself tested.

## Today's finish line: hello world (after Pieces 2 and 3)

A test across every node (decision 17):

1. **On the phone,** the assistant (Gemma wearing a shell) makes a block
   `hello-world` in the pocket, holding:
   - the shell's opening line;
   - a fun secret Matthew gives it;
   - a seal the app makes at random, which neither Matthew nor the model
     chooses.

   The app then computes a short fingerprint of the whole block.
2. **With Matthew's confirmation,** the app posts one message to
   `pool:Phenomemental` on https://beach.happyseaurchin.com, carrying the
   secret, the seal, the fingerprint and the block's pocket address.

If Gemma can't do step 1 reliably by the end of today, both steps are made to
work by hand from the test screen, so the chain can still be shown.

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

### How the message goes (to be built)

- **One HTTPS POST** to `https://beach.happyseaurchin.com/.well-known/pscale-beach`:
  `{block: "pool:Phenomemental", content: {_: <the message>, 1: <author>}, append: true}`.
  This is the shape bsp-mcp-server's pool tool sends, and the beach adds the
  arrival stamp.
- **Only after a tap,** on a screen that shows the whole message first.
- **Nothing else reaches the network:**
  - one class holds the only network code, with the beach's address fixed in
    it, and a test fails the build if any other class names a network API;
  - the app's network security settings trust no certificate for any other
    host and allow no unencrypted traffic. The model's download host in Piece
    3 is the one other name they carry (principle 5).
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

1. **Whose name the pool entry carries** at its position 1. I'd suggest
   `Phenomemental`, since Matthew confirms each message, with the message
   saying it came from the pocket.
2. **Which shell the assistant wears** on the phone, and so whose opening line
   goes into `hello-world`.
