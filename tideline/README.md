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

Tideline's screen should now say it is listening, and that the seal works on
this phone, with how long its key took to make.

A newer build installs over the old one the same way, from the same link, and
keeps these settings.

## What this version does (stage 6.3, listening only)

It catches messages from WhatsApp, WhatsApp Business, Messenger, Instagram and
Signal (the rules at `spine:tideline` 3, copied into `Rules.kt` until stage
6.5 reads them from the block), and shows each one on its screen. It sends
nothing anywhere: this version has no internet permission at all, and what it
catches is kept in memory only, so it goes when the app closes.

To check it: have someone message you on two different apps, and both should
appear on Tideline's screen.

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
- **`app/`, the phone.** A notification listener that opens only the five
  apps' notifications and makes a catch of each message (`Catch.kt`), a
  list kept in memory (`Catches.kt`), and one screen (`MainActivity.kt`).
  The screen also checks the seal on the phone itself against values made by
  `keys.ts`, using a test word that opens nothing.

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

    ./gradlew :seal:test :app:testDebugUnitTest :app:assembleRelease

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
