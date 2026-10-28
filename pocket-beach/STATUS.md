# Pocket beach — status

Current state only. Newest first.

## Now

Piece 1 (the local bsp, release v0.1) is built and its tests pass on this
machine. CI on GitHub is the next check. The release waits on the signing key,
which Matthew makes.

### What is built

- **The local bsp**, a Kotlin port of bsp-mcp-server and the beach duties
  (`bsp/src/main/kotlin/`).
  - It covers the full surface for ordinary blocks: point, path-walk,
    path-walk plus descent, disc, whole block, star (`*`), listing blocks
    (block left out), writes of every shape, and append.
  - Append takes the next free slot, and supernests when a block's root fills.
    A position inside a block holds nine and refuses a tenth.
- **The teaching shelf** (`shelf/`): the 27 blocks bsp-mcp-server bundles,
  under 28 names, byte for byte, with its MIT licence. It is the read-only
  `pscale` region.
- **Storage.** Blocks are stored as `filesDir/regions/pocket/<name>.json`. Each
  write goes to a temporary file and is renamed into place. When each block
  was touched and born is kept beside them, in `regions/pocket.meta.json`.
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
- **The app builds:** debug, release, and the on-device tests.

### Not yet checked

- CI on GitHub, including the emulator: install, open, the whetstone from the
  shelf, a write read back from its own file, the test screen, and the full
  golden replay on the device.
- The signed release, which needs the key.

### Next

1. CI green on GitHub.
2. Matthew makes the signing key and adds the four secrets (steps below).
3. Tag `pocket-beach-v0.1`; CI builds the signed APK and attaches it to the
   Release.
4. Matthew tries: install, open, read the whetstone, make a block, write a
   line, read it back, then again in aeroplane mode.

### Making the signing key (Matthew, once, on your own computer)

Every release must be signed with this one key, or Android will refuse to
install an update over the app. If the key is ever lost, the only way forward
is to uninstall, which erases the blocks. So keep two things safe for good:
the key file and its password. Put both in your password manager, and keep a
second copy of the file somewhere you trust. Never put either in the repo, a
block or a chat, including a chat with Claude.

1. **Open a terminal.** On a Mac, that's Terminal; on Windows, PowerShell. Go
   to a folder you'll keep, for example `cd ~/Documents`.
2. **Make the key.** Type the line below, and when asked, choose a long
   password and type it twice. `keytool` comes with Java; Android Studio carries
   its own copy.
   - Mac with Android Studio:
     `"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -genkeypair -v -keystore pocket-beach.jks -storetype PKCS12 -alias pocket-beach -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=Matthew Bushell"`
   - Windows with Android Studio:
     `& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore pocket-beach.jks -storetype PKCS12 -alias pocket-beach -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=Matthew Bushell"`
   - If neither works, install Java from adoptium.net (Temurin) and use
     `keytool` with the same words after it.
3. **Copy the key as text.** This puts it on the clipboard:
   - Mac: `base64 -i pocket-beach.jks | pbcopy`
   - Windows: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\pocket-beach.jks")) | Set-Clipboard`
4. **Add four secrets on GitHub.** Go to
   github.com/Phenomemental/happyseaurchin-beach, then Settings, then Secrets
   and variables, then Actions, and choose "New repository secret" once for
   each:
   - `POCKET_KEYSTORE_B64` — paste the clipboard (one long line of text).
   - `POCKET_KEYSTORE_PASSWORD` — the password you chose.
   - `POCKET_KEY_ALIAS` — `pocket-beach`
   - `POCKET_KEY_PASSWORD` — the same password again. This kind of key file
     uses one password for both.
5. **Say "the secrets are in".** Claude then tags `pocket-beach-v0.1`. CI
   builds the APK, signs it, checks the signature, and puts it on the Releases
   page.

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
4. **Signing key.** Matthew makes it on his own computer. Exact steps are given
   when the release is ready. It never passes through a Claude session.
5. **Lines left out of every reply:** the clock line (`now · …`), the
   "days ago / AHEAD" tags, and the who's-here lines (the reflection and
   orientation).
6. **The assistant (Piece 3) proposes a list of plain steps with one Yes.**
   Every step is validated before any step runs. If one fails, it stops and
   says exactly which steps ran.

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
- **Bundle windows are left out.** In bsp-mcp-server, reading a position whose
  lines are mostly references to other blocks that exist adds a window
  compiling those blocks. No teaching block triggers it; one of Matthew's blocks
  would have to be written that way.
- **Floor-10 blocks** (the clock's form) get no clock framing beneath a write,
  part of decision 5.
- **A block named `presence`** is swept of stale entries as the beach sweeps
  it, but only by ISO dates. The beach would also read some other date
  spellings.
- **Upstream is pinned:** bsp-mcp-server `27908e1`, pscale-beach `4770a49`.
  Matthew decides when to move to newer commits.
- **CI also runs when only its own workflow file changes,** so a change to the
  build is itself tested.

## Open questions

None.
