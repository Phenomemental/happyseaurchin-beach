# Tideline

Matthew's messages from other apps, caught on his phone and carried to the
beach sealed. The plan, the stages and what each one found are on the beach at
`spine:tideline` (https://beach.happyseaurchin.com). That block is the plan;
this folder is only the code.

## Installing on the Pixel

1. On the phone, open this link in Chrome. It downloads `tideline.apk`:
   https://github.com/Phenomemental/happyseaurchin-beach/releases/download/tideline-latest/tideline.apk
2. Open the download and tap **Install**. If Android asks first, let Chrome
   install apps (**Allow from this source**). If Play Protect says it doesn't
   know the app, choose to install it anyway: it was built from this folder
   by GitHub, not taken from the Play Store.
3. Allow restricted settings: **Settings → Apps → Tideline**, the **three dots**
   at the top right, **Allow restricted settings**, then confirm with your
   fingerprint or PIN. If the three dots don't offer it yet, do step 4 once
   first: Android shows the option only after it has refused once.
4. Switch on notification access: open Tideline and tap **Switch on
   notification access**, then switch Tideline on. (It is also under
   **Settings → Notifications → Device & app notifications**.)
5. Let it run in the background: **Settings → Apps → Tideline → Battery**
   (on some phones **App battery usage**), then **Unrestricted**.

6. Enter the catch's two keys: the screen asks for them once, the **edit
   latch** and the **reading key**. Get both from your own session (they are
   in your vault). They are kept on this phone only, in Android's encrypted
   storage.

Tideline's screen should now say it is listening, that it is ready to land in
your catch, and that the seal works on this phone, with how long its key took
to make.

A newer build installs over the old one the same way, from the same link, and
keeps these settings.

## What this version does (stage 6.4, landing)

It catches messages from WhatsApp, WhatsApp Business, Messenger, Instagram and
Signal (the rules at `spine:tideline` 3, copied into `Rules.kt` until stage
6.5 reads them from the block) and shows each one on its screen. It also:

- **seals** each message on the phone with the catch's reading key, the
  moment it is caught;
- **keeps** the sealed line in a queue on the phone until the beach takes
  it, so nothing is lost while you are offline and nothing goes twice;
- **lands** it at the root of your catch, `tideline:Phenomemental`, under the
  catch's edit latch: one stream that grows as it fills;
- **tells your room**, `pool:Phenomemental`, how many arrived from which app,
  at most once every ten minutes, with no names and no words.

Inside each seal is the message as caught, as JSON: `kind` ("message"),
`app`, `sender`, `conversation`, `text`, `time` (on the phone) and `key` (the
conversation key that will find the reply button in stage 6.6). Your session
opens a line with the catch's reading key as `enc_secret` and the beach
addressed as exactly `https://beach.happyseaurchin.com`.

Nothing of a message is written anywhere unsealed: not the queue, not the
room, not a log. The list on the screen lives in memory only.

To check it: when a real message arrives, the screen says it landed. Ask your
session to open the newest line in your catch, and it should read the message
back word for word.

## The parts

- **`seal/`, the seal.** The phone seals each message with the catch's
  reading key before it leaves, exactly as the Pscale tool does, so the tool
  opens what the phone seals and the phone opens what the tool seals. It is
  bsp-mcp's own gray seal (`src/keys.ts` in pscale-commons/bsp-mcp-server),
  ported to Kotlin and changed in nothing:
  - the key: Argon2id over the reading key (64 MB, 3 passes), salted with
    the beach's address, `https://beach.happyseaurchin.com`, exactly as
    written;
  - the seal: NaCl secretbox (XSalsa20-Poly1305) with a fresh nonce each
    time;
  - the envelope: `{ "_": "Encrypted (gray); readable only with the author
    secret.", "1": ciphertext, "2": nonce, "9": { "_": "gray", "1": "self" } }`.

  It is plain Kotlin with no Android in it, so the app carries it and any
  computer can check it. The cryptography comes from Bouncy Castle, which
  never touches the network.
- **`beach/`, the calls.** The phone's two calls to the beach, the same ones
  the Pscale tool makes: an append beneath a node (or at a block's root), and
  a read. Plain Kotlin on Java's own HTTP client.
- **`app/`, the phone.**
  - A notification listener that opens only the five apps' notifications and
    makes a catch of each message (`Catch.kt`, `Listener.kt`).
  - Hands that seal it (`Lines.kt`, `Hands.kt`) into the queue
    (`Queue.kt`), and a WorkManager job that lands it (`Landing.kt`,
    `LandWorker.kt`) and sends the count note (`RoomNote.kt`).
  - The two keys in Android's encrypted storage (`Keys.kt`).
  - One screen (`MainActivity.kt`). It also checks the seal on the phone
    itself against values made by `keys.ts`, using a test word that opens
    nothing.

## Building

GitHub builds it: every push that changes this folder runs
`.github/workflows/tideline.yml`, which checks the seal and the rules, builds
a signed APK and puts it on the release `tideline-latest`, replacing the one
before.

The signing key is kept in two repository secrets, set once under **Settings
→ Secrets and variables → Actions** on GitHub:

- `TIDELINE_SIGNING_KEY`: the key file, `tideline-signing.p12`, as base64;
- `TIDELINE_SIGNING_PASSWORD`: its password.

Keep the key file and its password somewhere safe as well. Every update must
be signed with this same key, or the phone refuses to install it over the old
one. Without the secrets, the workflow stops and builds nothing.

To build or check it on a computer, with Java 17 or later and the Android SDK:

    ./gradlew :seal:test :beach:test :app:testDebugUnitTest :app:assembleRelease

## Checking the seal against the beach

The tests compare the Kotlin seal with values made by bsp-mcp's own
`keys.ts`, byte for byte. To prove it against the live beach, use a trial
block, never the real catch:

    echo 'a test line' | TIDELINE_READING_KEY=<a trial word> ./gradlew -q :seal:sealCheck --args=seal
    TIDELINE_READING_KEY=<a trial word> ./gradlew -q :seal:sealCheck --args="open <field 1> <field 2>"

The first prints an envelope to write to the trial block. Read it back
through the Pscale tool with the same word as `enc_secret` and the beach's
address as `agent_id`. The second opens an envelope the tool sealed. The word
is read from the environment, so it never sits on a command line.

The landing can be tried the same way, on a trial block of its own
(`trial-tideline`) and never the catch:

    TIDELINE_READING_KEY=<trial word> TIDELINE_TRIAL_LATCH=<trial latch> \
      ./gradlew -q :beach:beachCheck --args="trial-tideline root 3"
    TIDELINE_READING_KEY=<trial word> TIDELINE_TRIAL_LATCH=<trial latch> TIDELINE_TRIAL_BLOCK=trial-tideline \
      ./gradlew :app:testDebugUnitTest --tests '*LiveLandingTest*'

The first lands numbered sealed lines at the block's root with the app's own
call. The second runs the phone's whole path for one message: catch, sealed
line, queue, landing.
