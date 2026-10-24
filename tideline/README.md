# Tideline

Matthew's messages from other apps, caught on his phone and carried to the
beach sealed. The plan, the stages and what each one found are on the beach at
`spine:tideline` (https://beach.happyseaurchin.com). That block is the plan;
this folder is only the code.

## What is here so far

**The seal** (stage 2, `seal/`). The phone seals each message with the catch's
reading key before it leaves, exactly as the Pscale tool does, so the tool
opens what the phone seals and the phone opens what the tool seals. It is
bsp-mcp's own gray seal (`src/keys.ts` in pscale-commons/bsp-mcp-server),
ported to Kotlin and changed in nothing:

- the key: Argon2id over the reading key (64 MB, 3 passes), salted with the
  beach's address, `https://beach.happyseaurchin.com`, exactly as written;
- the seal: NaCl secretbox (XSalsa20-Poly1305) with a fresh nonce each time;
- the envelope: `{ "_": "Encrypted (gray); readable only with the author
  secret.", "1": ciphertext, "2": nonce, "9": { "_": "gray", "1": "self" } }`.

It is plain Kotlin with no Android in it, so the app can carry it and any
computer can check it. The cryptography comes from Bouncy Castle, which
never touches the network.

## Checking the seal

In this folder, with Java 17 or later:

    ./gradlew :seal:test

The tests compare the Kotlin code with values made by bsp-mcp's own
`keys.ts`, byte for byte. To prove it against the live beach, use a trial
block, never the real catch:

    echo 'a test line' | TIDELINE_READING_KEY=<a trial word> ./gradlew -q :seal:sealCheck --args=seal
    TIDELINE_READING_KEY=<a trial word> ./gradlew -q :seal:sealCheck --args="open <field 1> <field 2>"

The first prints an envelope to write to the trial block. Read it back
through the Pscale tool with the same word as `enc_secret` and the beach's
address as `agent_id`. The second opens an envelope the tool sealed. The word
is read from the environment, so it never sits on a command line.

## Installing on the phone

The steps come with stage 3, when there is an app to install.
