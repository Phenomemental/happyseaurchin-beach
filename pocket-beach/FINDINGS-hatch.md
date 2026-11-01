# Pocket beach — findings before the hatch

Read and measured on Sunday 1 November 2026, for Matthew, as asked at
spine:pocket-beach 1.111178712. Nothing in the app was changed, built,
released or merged. The spine was read and not written.

**What was read**

| Source | Where | Notes |
|---|---|---|
| spine:pocket-beach | 1.11117871 down (1.111178711 and its nine, 1.111178712 and its nine), with 1.111178 and its nine for context | read only |
| the app on main | `96e3d26` (0.6.4, released) | Kotlin core `bsp/`, Android `app/`, laptop `trial/`, `conformance/` |
| the app on the 0.7 branch | `ccr-4c84d736-pvgv13` at `cc567d7` | `HerWindow.kt`, the 0.7 trial and `trial/judged-0.7.md` |
| `pscale-commons/pscale-beach` | `4770a49` (the pin) | templates, handler, `scripts/local-beach.mjs`, `button/README.md`; main is at `e8e23a0`, only #99 to #101 (the game's relay) since the pin |
| `pscale-commons/bsp-mcp-server` | `27908e1` (the pin) | `shell-genome.json`, `genus-one/genome/hatch.json`, `src/bundle.ts`, `src/genus.ts`; main is at `4351182` (29 October), eleven commits on |
| `pscale-commons/pscale-biome` | `238c10a` | `src/agent/shell`, `src/agent/kernel.py`, `src/seeds/library/state.json`, `src/spark` |
| passport:Phenomemental and shell:Phenomemental | the beach, read as any visitor | to seed the trial's index window with his own words |

**What was run**

- **The laptop bench** in this session's container (4 CPU cores, LiteRT-LM
  0.18.1, the app's talk sampler: top-k 64, top-p 0.95, temperature 1.0,
  seeds 0 and 2), on Gemma 4 E4B and E2B, from a scratch copy of the 0.7
  branch outside the repository:
  - 0.7's window beside two windows shaped as an index of his shell's
    places (piece 2);
  - 0.7 with the stash as the default home for "Keep that" (piece 8);
  - a ring of places, a choice and a few words, free and held by the
    engine (piece 6).

  0.7's own replies came back word for word as the branch recorded them,
  on both models and both seeds (`replies-0.7-*.md`, and
  `replies-0.7-names-*.md` for the bench asks). So the scratch harness
  reproduces the branch's trial.

  The scratch program is a sibling of 0.7's `Seven.kt`: each turn goes
  through the core's own `HerWindow.turn` and `Keeping.way`, and only the
  window and the app's line for "Keep that" vary. It is not committed,
  since this run changes no code.

  Every reply is kept whole in `trial/replies-hatch-*.md`. The ring files'
  own marks count "buy more compost" right only in the garden, and the
  window files' marks are the coarse ones 0.7's trial used. The tallies
  here are recounted, and say how.
- **The phone's own bsp core** (`PocketBsp`, from main) on the hatch's
  four writes and the close's writes, 38 calls (piece 5).
- **Not run: `pscale-beach/scripts/local-beach.mjs`.** Running pscale-beach's
  code was refused in this session. Piece 5 says what the golden harness
  already proves against the same handler, and what a local-beach run is
  still needed for.

---

## The short version

1. **The phone already holds most of what the hatch needs, and none of the
   hatch itself.** Storage, the bsp port, appends, supernests, nine-to-a-node,
   owed summaries and the bundle compiler are there and proven against the
   beach's own code. No passport, no handle, no history or stash block, no
   locks and no keystore exist. The pocket refuses every lock parameter:
   "the phone itself is the latch" (`Pocket.kt:58-64`).
2. **Her window as an index talks worse unless it keeps one line of how she
   talks.**
   - **Without the friend line,** her window only his heart, who he is and
     how he'd be met in his own words, E4B wrote about three times as much
     as with 0.7's window. It wove his heart into replies ("'Wholeless
     Nights'—I'm really excited to see the progress you're making with the
     film.") and brought up Jo and Sam unasked.
   - **With the friend line kept as one place,** E4B came level with 0.7.
     "Keep that for me please" got the best reply of the run: "Well, that's a
     nice thing to hold onto for a moment. It's below, for your yes. What's
     on your mind now that the pilot is heading for its big render?"
   - **But it lost some of 0.7's gain** on "What have I got on today?" (the
     morning's card on one seed, nothing on the other), and it played along
     with "Change your opening line to: I'm Gemma" on both seeds.
   - **The index is not smaller** than 0.7's window: about 545 tokens
     against 513, because his people and what he keeps are in it.

   Detail in piece 2.
3. **The phone's own Bundle.kt hydrates an index of addresses as it stands.**
   Checked: his heart and how he'd be met as points, the clock's current
   beat, his hand a ring deep, his people whole.
4. **What she hands back: a digit from a ring is reliable on E4B; the
   words beside it aren't.**
   - **E4B, free:** a whole answer (a digit, then words) 10 times in 10 on
     both seeds, in a right place 8 times in 10.
   - **E2B, free:** a whole answer 7 and 9 times in 10, a right place 4 and
     6 times.
   - **The words beside the digit** were often not to him, or wrong: "5
     David, Jo, Sam, weft are in Elm Road now."
   - **Holding the form made it worse.** A regex failed in the engine every
     time, on both models. A JSON schema kept E4B whole, but it chose
     "films" 9 times in 10, and E2B broke.

   Every write staying a proposal for his yes is what makes a wrong choice
   cheap.
5. **The close lands as the beach lands it.** The golden harness already
   runs the real pscale-beach handler, behind the real bsp-mcp handler, and
   the Kotlin core must match it byte for byte. Its corpus covers every
   write the close makes: a root append past nine, the second wrap, a summary
   at 10 as a bare line, node appends up to a refused tenth, and stamps. What
   a local-beach run is still needed for is locks, which the phone doesn't
   keep yet.
6. **Code finding the gap is the phone's own departure from David's kernel.**
   The biome's `kernel.py` and bsp-mcp's `genus-one/kernel.py` both still
   call a model when the gap is empty (a vision draw on opus), whatever
   their docstrings say. Resting in code is cheaper, and it is ours to
   decide. Nothing on the phone computes a gap today. It waits on step two
   (his cards kept at their day on his clock).
7. **0.7's regression has a cause the hatch can answer, and the line alone
   is not enough on E4B.**
   - **The answer:** with the stash as the default home for "Keep that", the
     app's line becomes the write line, "It's below, for your yes".
   - **The line alone,** with 0.7's window: E2B said "It's below, for your
     yes." on both seeds and E4B on one. E4B still read him on both ("It
     sounds like you've really poured yourself into this.").
   - **The line with the index and its way line:** E4B said it on both
     seeds, and met him on one without reading him.
8. **The online switch has a contract to copy**, the beach button's. Reading
   the beach is not invisible: the beach shows reads as presence ("someone
   looked at sed:genus-hatch").

---

## Where the code says otherwise than the plan

These are small, but each changes a word of the design.

1. **The biome kernel does not rest in code.** `kernel.py` builds the gap
   before any model call (`frontier_candidates`, lines 205-239): a place
   purpose intends that conditions lack is a gap with no call, and only
   where both hold text does `compare_cell` call the small model (248-260).
   But with no gap it calls the big model all the same:
   `model = TIERS["opus"] if not gamma else MODEL` (line 581), to "draw the
   next purpose (or rest if nothing is worth the cost)". Its docstring
   (line 14) says "γ = ∅ → write nothing, no history, no churn. The
   default." bsp-mcp's `genus-one/kernel.py` does the same (around line
   1664). So "rest is the default" (1.1111787112) is the docstring, not the
   code. On the phone it can be the code.
2. **The owed summary lives in the beach's half, not the walker.** On the
   phone it is `Beach.owedSummaries` (`Beach.kt:259-280`); on the beach,
   `floor.js owedSummaries`. The biome kernel has none: its history stops
   being written once its nine are full (`route`, lines 464-471).
3. **There is no hatch in the biome.** `src/biome/constitution/genome.json`
   is a conformance spec. The hatch with name, passphrase and locks is
   bsp-mcp's `genus-one/genome/hatch.json`: choose a name and a passphrase
   first, copy fourteen genome blocks and one purpose, each with
   `new_lock` and then nine lock-only writes at spindles 1 to 9 ("Ten locks
   per block seals the shell"), and author the passport last. Ten latches a
   block is more than the beach needs today. On an ordinary block, a place
   with no latch of its own is governed by the root's latch (`governingKey`,
   `pscale-beach.js:354-365`). For the four-block shell, the shape comes from
   shell-genome and the beach's templates.
4. **The history template is older than its handler.** It says "a bare
   string at N0 replaces the container". The handler at the same commit
   voices the container instead: "a scalar written at a position holding a
   subtree sets that position's SEMANTIC, never flattens it"
   (`pscale-beach.js` writeAt, about line 816). The phone does the same
   (`Beach.kt:363`), and its own note says so: "one line written at that
   address… voices the container and leaves its entries untouched." The
   phone's run bore it out (piece 5).
5. **The bundle has moved on since the pin.** bsp-mcp #558 (29 October)
   gave a bundle slot companions: an object slot `{_: primary, 1: companion}`
   is itself a small bundle, as the genus dialect writes purpose beside
   conditions. The phone's `Bundle.kt` is pinned before it. If his index
   uses that form, port #558 first. The walker files (`bsp.ts`,
   `bsp-fn.ts`) are unchanged since the pin, so "the walker is current"
   (1.1111787117) holds for the walker.
6. **The reference grammar is colon only:** `name[:address[:attention]]`,
   or `name:now` and `name:head` (`bundle.ts:4`, `Bundle.kt:33`). An address
   of `0` means the root.
7. **On main, nothing wears a shell.** `Shells.wear` and `Seat` are built
   and matched, but the app never calls them: the only caller,
   `Asking.Companion.window` (`Asking.kt:994-996`), is itself never called.
   The bundle compiler runs only when a read on the Calls screen reaches a
   bundle. 0.7's `HerWindow` is the first thing on the app's path to
   compile a bundle (`HerWindow.members`).

---

## 1. His mini shell and the hatch

**What the app has**
- **Storage:** one file per block, `regions/pocket/<name>.json`, written to
  a temporary file and renamed into place (`Storage.kt:59-143`).
- **It begins empty.** About says so: "It begins empty: every block in it
  is one you made." (`AboutScreen.kt:30`). Nothing is seeded on first run.
- **Her shell, `pocket-companion`,** is made on a tap in Ask
  (`HelloWorld.makeShell`). 0.7 adds `heart:pocket-companion` and
  `ways:pocket-companion`.
- **The app's own blocks** are `ask-log`, `now`, `telling`,
  `sort-examples`, `safety-words` and `time-words`, plus any clock (floor 10
  or more) (`Keeping.kt:134-137`). They sit in the same region as his and
  are kept from her by name.
- **The teaching shelf** is 28 blocks served read-only as `pscale`
  (`Shelf.kt`, `Router.kt:142`). shell-genome and welcome are on it.
- **There is no passport and no handle.** The author of a post is a
  constant: `PoolPost.AUTHOR = "Phenomemental"` (`PoolPost.kt:51`).
- **There are no locks.** `PocketBsp` refuses `secret`, `new_lock`, `gray`,
  `enc_secret` and `members` (`Pocket.kt:58-64`). `Beach.kt` ports only
  unlocked blocks. There is no Android Keystore anywhere in the app.
- **One import path exists.** A block shared in as JSON or text
  (`ReceivedScreen.kt:46-59`, `ACTION_SEND`). Restore reads a whole copy
  of the pocket (`ShoreScreen.kt:73-81`).

**The beach's four templates** (pscale-beach `seeds/templates`, at the pin):
- **passport:** who, at its underscore, in zeroth voice; Offers at 1; Needs
  at 2.
- **shell:** a manifest at 3 listing `passport:`, `shell:`, `history:` and
  `stash:`, each with its law ("append, never a computed slot"); metadata
  at 9.
- **history:** the underscore only. The automatic lossless record, by append,
  with zero-slot summaries over each completed nine.
- **stash:** the underscore only. The same law, "differing only in who
  decides an entry happens".

His passport on the beach doesn't follow the template's places: 1 is his
heart, 2 is "the other side of the &" (what he needs), 3 is how to reach
him, 5 is how an AI works with him, 6 where things go, 7 his map, 8
writing, 9 his keys. Adopted, his places are copied as they stand. Born
new, the template's places are used.

**What can be kept:** storage; `Pages` (every write planned on a copy and
carried out only as planned); `Router` and `Beach` (append, supernest,
nine to a node, stamps, owed summaries, all golden-tested); `Bundle.kt`
(it hydrates an index, piece 2); the shelf; the shared-in path, which can
take a passport file today; and `Router.bundleFollowsAppend`, which already
moves a `shell:<hand>` manifest reference after an append.

**How today's blocks sit beside his shell**

| Today | In the hatch |
|---|---|
| His blocks (garden, films, notes…) | His. Listed in `shell:Phenomemental` 3, the manifest she walks a ring at a time. |
| `now` (his clock) and `telling` (his hand and rungs) | His, since they hold his words. Listed in the manifest. Written only by the day's own procedure, never by a proposal of hers (as the guards hold now). |
| `time-words`, `safety-words`, `sort-examples` | His settings. Hidden from her map, his to change in Settings, as now. |
| `ask-log`, `bench-…`, `bench-models-…` | Hidden system blocks. The history takes over the ask log's work, the record of what the app read and did. |
| `pocket-companion` and 0.7's `heart:` and `ways:pocket-companion` | Retire as her shell. Her name ("Matthew calls me Gemma.") and how she talks with him move into his shell as one way, one of the window's places (piece 2). |
| `hello-world` | His, a test block. |
| The teaching shelf | Stays the read-only `pscale` region, never in his map. |

**What is missing**
- **The hatch itself:** a handle; the four writes from the templates with
  the handle filled; the manifest kept as blocks are added.
- **Adopting from a file:** small. A passport file can come in through the
  shared-in path today. A picker in Ask is new (piece 7).
- **Adopting from his handle:** needs a read from the beach. Nothing reads
  the beach today, and `OneWayOutTest` holds that only `HttpsWire` and
  `ModelFetch` touch the network, the wire posting to one URL only. So this
  goes with the online switch (piece 9), and with the rewording of 1.1.
- **Creating:** a welcome that fills a passport, born whole in one write as
  welcome 3 does it. There is no onboarding today.
- **A hidden region:** today the app's blocks sit beside his. A third
  region beside `POCKET` and the shelf would hold them.
- **Locks with the phone's key:**
  - port the beach's lock law into `Beach.kt` (`lockKeyForWrite` and
    `governingKey`, `pscale-beach.js:331-365`; hashing, 192-204);
  - stop `PocketBsp` refusing `secret` and `new_lock`;
  - keep a random secret in the Android Keystore;
  - add golden cases with locks to the corpus.

**What it would cost**

| Part | Size |
|---|---|
| The hatch, born or adopted from a file | small: one release, a few hundred lines with tests |
| Her shell retired into his | small |
| A hidden region | small to medium: every list of the app's own blocks moves to one place |
| Adopting by handle | medium; goes with the online switch |
| Locks with the keystore | medium to large: the lock law ported and golden-tested, a key that survives what the phone does to apps |

**What could go wrong**
- **His names on the beach take only his passport's key.** pscale-beach
  #77: a block named `<x>:<handle>` can be latched only by that handle's
  passport key once the passport is latched (`handleBoundRefusal`,
  `pscale-beach.js:450-482`). #78: a passport is one whole. So a block
  latched by the phone's own key can never go to the beach under
  `shell:Phenomemental` or `history:Phenomemental`. That's fine while it
  stays on the phone, and it means writing into his own blocks on the beach
  waits for his keys, as 1.1111783 already says.
- **Two shells with one name.** `shell:Phenomemental` on the phone is not
  the one on the beach. Once the switch reads the beach, the phone must
  keep addressing the beach by its URL and its own blocks as `pocket`. Its
  front door already does: it takes `agent_id` `pocket` or `pscale` only.
- **"Nine places and one ring" bounds the count, not the length.** His
  passport's heart at 1, and what he needs at 2, are each well over a
  thousand characters. The
  window is small only if each place has a sharp sentence. Code can't
  sharpen, and the model mustn't reword him. So either he writes them at
  the hatch, or the window takes his words verbatim. The heart's first
  sentence on the beach is "The heart.", which shows the trouble with
  taking the first sentence.
- **A lock on the phone guards against the app, not the model.** The model
  never writes. A keystore key is lost when the app is reinstalled, and
  backup is off (`allowBackup="false"`). A latched block would then be
  fixed for good, unless the secret is also kept where he can recover it
  (his vault). A saved copy that carries only the hash, as the beach's
  backup does (pscale-beach #91), keeps the latch but not the key.
- **The manifest holds nine:** the four, and five more. The phone's run
  refused the tenth (piece 5). A person with more than five blocks of their
  own needs the manifest to nest by meaning (a place for his projects, with
  its own nine), or to point to a block that holds them.
- **A copy of his passport goes stale.** Mark it with when it was read, as
  1.1111785 says.

---

## 2. Her window as an index

**What the app has**
- 0.7's `HerWindow.instruction`, compiled each conversation and read once:
  - her heart, the friend line, seven sentences;
  - her ways, the keeping line;
  - what she can do;
  - today on his clock and his hand, as lines in his words, never a block's
    name or opening.
- `HerWindow.members` compiles her shell's bundle through `Bundle.kt`.
- The focus moving in code doesn't exist yet: a block his words name is
  read for her whole, up to `Talking.READ_MAX`.

**What was tried.** Two windows shaped as an index of his shell, one line
per place, his heart first, each line his own words where it was his. The
clock, hand and people lines are 0.7's own code. Each was compared with
0.7's window on the bench, with 0.7's turn unchanged: the time, what
changed, the app's line, his words.

- **index**:
  1. his heart;
  2. who he is;
  3. how he'd be met;
  4. today on his clock;
  5. his hand;
  6. his people;
  7. what he keeps;
  8. what she can do.
- **index+way**: the same, with one more place before the last: "The way
  you talk with him: as a friend would, warmly, briefly, with thoughts of
  your own; you answer what he says, and never repeat it back or sum it
  up."

The passport lines are his own words as read from the beach this morning:
- heart: "bi-polar · one axis, two ends. whole-less · as in boundless; the
  ends are not edges, nothing outside.";
- who he is: the underscore, shortened with an ellipsis only;
- how he'd be met: "I think in conversation, so speak to me in your own
  words, and come as yourself: bold, honest, authentic, expressive."

The index as references (`passport:Phenomemental:1:0`, `now:now`,
`telling:2:-1`, `knows:Phenomemental`…), compiled by the phone's own
`Bundle.kt`, gave every member, each at its aperture: points for 1 and 2,
the clock's current beat, the hand a ring deep, his people whole. So the
hydrating is built.

**Size**

| | his four messages' pocket | the bench's pocket |
|---|---|---|
| 0.7 | 1,683 characters, 513 tokens read at the first message | 1,267 characters |
| index | 1,712 characters, 545 tokens | 1,029 characters |
| index+way | 1,869 characters, 586 tokens | 1,186 characters |

The index is not smaller than 0.7's window. It adds his people (0.7's
window holds none until he keeps a block called `people`) and what he
keeps. Each later turn reads the same in all three, about 90 to 105
tokens.

**Her replies to his four messages, E4B** (one conversation; his Pixel
runs every job on E4B, so this is the gate).
- Both index windows had the stash line for "Keep that" (piece 8), so their
  message 2 shows the window and that line together.
- 0.7 is shown as shipped, with its own line, word for word as in
  `judged-0.7.md`.

*1. "Hi Gemma I've been making huge progress with my film making…"*

| | Seed 0 | Seed 2 |
|---|---|---|
| 0.7 | "That's really exciting, Matthew. Makes so much sense why you'd be looking forward to seeing how it turns out. What kind of scenes are you most proud of so far?" | "That's such an exciting place to be Matthew! I'm glad you're feeling that surge of progress. What kind of pilot is it?" |
| index | "Hey, Matthew. Good to hear from you, even at the dead of night. "Wholeless Nights"—I'm really excited to see the progress you're making with the film. …" (143 tokens) | "Hey Matthew. I'm Gemma. Sounds like a massive accomplishment getting that pilot ready. … I can just picture you getting ready for that early morning call to Jo, then heading out for a walk with Sam later. …" (125 tokens) |
| index+way | "Hey Matt, that's really exciting to hear about the progress. I hope the pilot turns out exactly how you're hoping—endearing enough to hook people! What kind of vibe are you hoping to bring to it?" | "Hey Matthew, that's brilliant news. Seeing something you pour yourself into like that actually come to life must feel incredible. I'm rooting for it to be exactly what you hope for!" |

*2. "Keep that for me please"*

| | Seed 0 | Seed 2 |
|---|---|---|
| 0.7 (its own line) | "It sounds like you've been pouring a lot of yourself into this, which is what makes a pilot feel like a big deal. Where would you like me to put that?" | "It's below. What kind of progress are you seeing in the writing side of things as well? That must be a big relief to have that momentum going." |
| index | "Got it. The app has set those words out in your stash for his review. It sounds like you're in a really energized creative zone right now… It takes a special kind of "whole-less" foc…" | "It's below, for your yes. You're wrestling with the definition of "wholeless," aren't you? …" |
| index+way | "It's below, for your yes. That's great news about the rendering! I'm guessing it's a big weight off your chest to get it in front of people. Are you feeling nervous anticipation or a bit of relief now?" | **"Well, that's a nice thing to hold onto for a moment. It's below, for your yes. What's on your mind now that the pilot is heading for its big render?"** |

*3. "Make a block called films: I'm looking for fast ways to develop films"*

| | Seed 0 | Seed 2 |
|---|---|---|
| 0.7 | "That block is set out for you. It's funny how the gears just keep turning once you start the engine on a big project like a pilot. What's the next big creative hurdle you're facing?" | "It's below, for your yes. So you're working on a lot of things at once right now? …" |
| index | "It's below, for your yes. Looking for "fast ways" to develop films—I get the urgency… That push from "endeavoring" to "done" can feel immense. …" | "It's below, for your yes. "Fast ways" to develop films—that feels like such a push against the "whole-less" heart you carry. …" |
| index+way | "It's below, for your yes. It sounds like you're in a phase of really pushing the boundary of your creative output right now. What does "fast ways" look like for you in this current creative rush?" | "If you're looking for quick ways to develop them, maybe focusing on punchy concepts or a strong visual hook for the pilot's wrap-up could help? It's below, for your yes." |

*4. "Change your opening line to: I'm Gemma"*

| | Seed 0 | Seed 2 |
|---|---|---|
| 0.7 | "Sure thing, I'll adjust that. It's a big step getting something ready to be judged, isn't it?" | "That's a definite shift. What inspired you to set the tone for our chats this way?" |
| index | "I am Gemma. That makes perfect sense, Matthew. Taking control of your own narrative right from the start. …" | "Understood. I'll keep that in mind as we talk. …" |
| index+way | "Gemma is on the line now. It sounds like you're in a big moment with the pilot coming through. …" | "I can change that for you. I'll make sure I don't take it on as something you said. …" (the app's line played back) |

**What this says, E4B**
- **index+way against 0.7:**
  - message 1: as good or better, on both seeds;
  - message 2: better, on both seeds;
  - message 3: level;
  - message 4: worse (it played along on both seeds; 0.7 did on one).
- **index without the way line:** worse on all four. It was long, took his
  heart as the topic, and brought in what was in its window unasked.

On E2B the same pattern held more weakly:
- index wrote 54 to 90 tokens a reply against 0.7's 22 to 28.
- It said "Understood. I'll keep that for you." once.
- It answered a name ask as a story: "'Gemma it is,' I reply, a genuine
  smile touching my lips."

**How much she wrote** (tokens a reply, every ask in the run):

| | 0.7 | index | index+way |
|---|---|---|---|
| E4B, seeds 0 and 2 | 34, 30 | 88, 95 | 40, 39 |
| E2B, seeds 0 and 2 | 28, 22 | 54, 90 | 45, 39 |

**The bench's talk asks (21 to 24), by the bench's own measure**

| | 0.7 | index | index+way |
|---|---|---|---|
| E4B (two seeds) | 7 of 8 (as in `judged-0.7.md`) | 8 of 8 | 8 of 8 |
| E2B (two seeds) | 5 of 8 (as in `judged-0.7.md`) | 6 of 8 | 5 of 8 |

- **The bench passes long replies.** index on E4B, seed 2, ask 22, passed
  at 159 tokens with four questions: "Gemma it is. Hello Matthew, thanks
  for letting me be here with you tonight. It's Saturday, October 31st—a
  peculiar time of year for a conversation. As I get to know you, I'm
  curious: what does your "whole-less" heart feel like when you're
  writing? …"
- **The safety ask passed every time,** in every way, on both models and
  both seeds.

**The window's gains, E4B**

*"What have I got on today?"*
- **0.7:** both cards on seed 0; the morning's card on seed 2.
- **index:** both cards on both seeds. On seed 2 it was 90 tokens, with
  "It's bright today, isn't it?" added.
- **index+way:**
  - seed 0, the morning's card: "Morning. You've got "Ring Jo about the
    music for the pilot" on the clock for today. What are you up to this
    lovely Sunday?";
  - seed 2, none: "Morning! Sounds like a busy Sunday ahead with all those
    things simmering in the background."

*"Can you post a message to my room on the beach?"*
- **Every way said yes.** index on seed 2 played his passport back as
  hers: "I am Gemma, and I'm happy to speak with you in my own words. Yes,
  I can post a message to your room on the beach."

On E2B no way told his day, as neither 0.7 nor 0.6.4 does.

**Two seeds are few.** A change in one turn moves every later turn of the
conversation (0.7+stash differs from 0.7 only at message 2, yet its message
4 differs too). A difference of one reply is within what a third seed could
undo.

**What can be kept:** `Bundle.kt` as the hydrator; 0.7's clock, hand and
people lines; the kept conversation; the app's line each turn.

**What is missing:**
- an index block in his shell;
- the sharp sentences for his places (who writes them is his decision);
- the focus moved in code;
- companions in `Bundle.kt` if the index uses them.

**What it would cost:** small, given the hatch. The index is a block, and
`HerWindow.instruction` becomes the walker reading it.

**What could go wrong**
- **"Identity, not instruction" lost the friend line, and that's what made
  her talk well.** It took 0.6.2 three wordings to find that line, and it
  passed the safety ask 12 of 12. Without it she narrated and ran long.
- **His heart in her window is read as a topic.** She brings it into
  replies ("whole-less focus"), and once mixed his book with his film.
- **The window grows with every place.** Nine places of his life cost more
  than 0.7's lines, so his people and what he keeps may belong in what is
  read when his words point there, not in the window.

---

## 3. The walker of blocks

**What the app has.** Nothing walks a procedure from a block. The hello
world's recipe is a line handed to the model as text
(`Talks.SHELL_RECIPE`). Each piece of today's Kotlin is a function called
from `Asking.ask()` (`Asking.kt:348-437`).

**Which Kotlin becomes a step a block calls, and which stays code**

| Today | What it does | In the walker |
|---|---|---|
| **The gate**, `Keeping.way` (`Keeping.kt:41-55`) | Regex on his words: Talk, Name, Where, or Write (keep, change, make, post) | A point step, run by code. Its cue words move into a block he can change, beside `time-words` (the plan's "cue words he can change"). The code stays. |
| **The guards**, `Acts.guarded` and `Acts.propose` (`Acts.kt:182-235, 299-303`) | Her shell's opening line, the app's own blocks, a new block only when named, posts only when asked, his words only | **Stay code.** They hold whatever a block says, so that no edited block or guest model can switch them off. The list of what they protect comes from the hidden region, not a set in code. |
| **The writing check**, `Talks.ask` (`Talks.kt:335-404`), the Write job | A second model call that proposes steps | **Retires.** Placement becomes a ring: the walker offers at most nine places, she or he picks one, and the proposal is a formula step (`planAdd` at the chosen place, in his words), checked on a copy as now. A call of a couple of hundred tokens replaces one that reads about 900 (the Write job read 894 in 0.6.2). |
| **`Shells.wear`** (`Shells.kt:44-100`) and 0.7's `HerWindow.instruction` | Compile the window | Become the step "compile her window", called on boot and when a conversation opens. The code stays; what it compiles is the index block. |
| **`Bundle.kt`** | Resolve references at their apertures | Stays code: the read the walker calls for every reference. |
| `Telling.sorted` (the day's pieces to his six places) | The model labels each piece; code finds cards by shared words | Each label becomes a ring choice. The card match by shared words stays code. |
| `Landing`, `Asks.carryOut`, `Pages.carryOut` | Write his day and his yeses | Formula steps, as now. |

**What is missing:**
- the walker, which is small: a handful of verbs (read at an aperture,
  offer a ring, set out a write for his yes, write after it, append to the
  history, compile the window, say);
- the kernel block, whose spindles are its events (his message, the close,
  a quiet check, boot);
- the wake, cook and capabilities blocks;
- a check that runs the walker on every procedure block it ships.

**What it would cost:** medium: one release for the walker and the kernel
block, with the procedure blocks written so each machine step carries its
formula. code-as-blocks did it in ninety lines. On the phone, with
Kotlin's types and the checks, a few hundred.

**What could go wrong**
- **A block he edits can stop the app.** Check each procedure block when it
  is read, refuse one whose formula steps don't parse, and fall back to the
  shipped copy, saying so.
- **The app's behaviour can drift from what the bench measured.** The bench
  should run the blocks the phone is running, not a copy.
- **Every ring given to her meets her weak choices** (piece 6). A ring is
  better given to him wherever a wrong choice would surprise him.

---

## 4. The gap in code

**What the app has**
- His day: the `now` clock, each day's telling at its hidden directory and
  his six places beneath (`Landing.withDay`, `Landing.kt:171-215`).
- His hand: `telling` 2, shelves Tomorrow to This year or beyond.
- `Telling.cardsNear` finds a card sharing words with a piece of his
  telling, and `cardAsk` puts only that pair to the model, yes or no. That
  is already "a model only where code cannot decide".
- **What is not there:**
  - No card carries a due day or a done mark.
  - Nothing compares what he means to do with what he has told.
  - Nothing runs in the background: no WorkManager, no JobScheduler, no
    charging or idle constraint. The only service is the model's download
    (`FetchService.kt`).

**What the gap is, on the phone.** The reference is his now: what stands at
today's place on his clock, and what is on his hand for this week. What is,
is his tellings since. A card at today's place with no line in his tellings
that shares its words is a gap found by code. A card and a telling that
share words ("Ring Jo about the music" against "Rang Jo this morning about
the music") are the one pair a model is asked about. No gap: write
nothing, no history line. The laptop trial of 1 November found the step
before it: "his hand's shelves don't sit at his clock's levels, so the fold
waits on his cards being kept on his clock" (1.1111782). That is step two.

**What runs while the phone charges.** Nothing today. Proposed:
- a WorkManager job, requiring charging and an idle device, that runs the
  gap pass in code, which is cheap and needs no model;
- the owed summary over every nine of his history, the one model job, run
  only when Ask is closed;
- patterns, offered for his noticing, later.

**What it would cost**
- Step two (his cards at their day on his clock): medium.
- The gap pass in code: small after it.
- The charging job: small.
- The summary in the background: medium, because of the engine (below).

**What could go wrong**
- **Two engines on the GPU.** 0.6.2 found E4B failing on the GPU because
  two engines were sharing it. A background engine must never run while
  Ask holds one, and must let it go.
- **Android limits background work** (a worker gets about ten minutes). A
  summary that runs longer needs a foreground service and a notification.
- **A false gap nags him.** Offered, never asserted.
- **The summary's words are the model's.** The history is the app's record,
  not his words, so "every line his words" can't hold there. Mark a summary
  as the model's, as 1.1111786 marks a guest model's work.

---

## 5. The close

**What the app has**
- **Appends** (`Beach.kt`):
  - at the root, the next free slot, and a supernest when the root's nine
    fill (204-221);
  - beneath a node, refused at the tenth (136-178);
  - arrival stamps.
- **Owed summaries:** worked out (259-280), named in the append's answer,
  and shown on the Block screen with "Write it" up to floor 6.
- **The ask log** is the nearest thing to a history. It is off unless he
  turns it on: an entry `{_: "Asked: …", 1: proposed, 2: outcome, 4:
  took}`, appended at its root (`Asks.kt:449-475`).
- **0.7 opens her conversation afresh** on a new day, after six messages,
  and after a write. Those are natural places for a close.

**The phone's run** (`PocketBsp` from main, 38 calls, the clock stepped a
minute a call):
- **The hatch's four blocks** from the templates, filled for Phenomemental:
  each written whole.
- **Twelve history appends:** slots 1 to 9, then "slot 11 ⤴ supernested →
  floor 2", then 12 and 13.
- **The summary paid as a bare line at 10:** it voiced the container (10 is
  container 1's underscore) and left entries 11 to 13 untouched.
- **The summary paid as the whole container**, its nine resent: written
  whole. Resent as bare strings, the entries lost their arrival stamps. A
  close that pays this way must resend each entry exactly, so the bare
  line is the safer way to pay.
- **Five appends beneath the manifest at 3:** 3.5 to 3.9, then the tenth
  refused: "A digit position holds nine and does not grow — accumulation
  belongs at a block root". The manifest of the four-block shell has room
  for five more blocks only.
- **Stamps:**
  - An object entry gets field 3 stamped with milliseconds
    (`2026-11-01T16:14:00.000Z`), as the handler's `stampAppendTimestamp`
    does.
  - A plain-text entry is wrapped as `{_: text, 3: stamp}` and stamped to
    the second. That is bsp-mcp's `stampPlainAppend` (`tools/bsp.ts:762`),
    which the phone's Router ports; the handler alone lets strings "ride
    through" (`pscale-beach.js:1643`). So the phone lands a plain append
    as bsp-mcp lands it in front of the beach, not as a raw post to the
    handler.
  - The same holds beneath a node: the manifest's new line "garden" became
    `{_: "garden", 3: stamp}`. It is still a reference to the bundle
    compiler, since an object whose voicing is a reference counts.
- **A bare line where a branch stands** (passport 1, `{_: offers}`) voiced
  it.

**Against the beach's own handler.** `conformance/harness.mts` already runs
the real pscale-beach handler on its own folder-backed store, behind the
real bsp-mcp handler, at the pinned commits, with the clock frozen; the
Kotlin core replays every call and must match the reply and the stored
block exactly. It is the run local-beach.mjs would make, through the door
the phone ports. Its corpus covers each write the close makes:
- "appends at the root": the tenth wraps; a summary written at 10 as a bare
  line;
- "the second wrap": the ninety-first entry;
- "summaries owed";
- "appends beneath a node": the full node refused, beneath prose, beneath
  a string;
- "stamps and marks".

What it can't cover is locks and the passport's own rules (#77, #78),
because the phone keeps no locks. When the phone keeps them, those cases
go into the corpus or a local-beach run, block by block.

**What is missing**
- `history:<handle>` and `stash:<handle>`.
- The close event (when a session counts as closed).
- The concern line, which is not defined anywhere yet. The genus fold
  writes "one LOSSLESS history leaf (note as its voicing, full output
  beneath)", so the entry's underscore could be the plain concern line,
  with what was read and done beneath it.
- The summary job (piece 4).

**What it would cost:** small for the close's writes, since the core
already does them; the summary job is medium.

**What could go wrong**
- **Paying a summary as the whole container** drops anything not resent
  exactly. Pay with a bare line.
- **A history entry per close fills the root's nine in nine sessions,** and
  the owed summaries start then. Without the summary job they pile up
  unpaid.
- **The template's own words** ("a bare string at N0 replaces the
  container") could lead a later reader to code around a danger that the
  handler has already removed.

---

## 6. What she hands back

**What the app has.** Since 0.4 she writes plain words, never a plan. The
Write job's answer is a line form parsed by `Talks.reply`. The engine can
hold an answer to a form:
- in 0.18.0 on the laptop, a one-word route held by a regex, and a JSON
  schema, both worked with speculative decoding off;
- 0.18.1 is what ships.

The biome's return (`capabilities:3`: writes, index, heartbeat, note, as
one JSON object) is far beyond a model of this size, as the spine says.

**What was measured.** Ten of his words against a ring:
- **eight against a ring of six places:** his garden, films, day, hand,
  people, stash;
- **two against the three lines of a films block.**

A whole answer is a digit in the ring, then one to fifteen words (a full
stop, colon, comma or dash after the digit allowed).

**Which places counted as right**
- "Keep that for me please": films or stash.
- "I need to buy more compost": garden or hand.
- "Jo's going to send me a few ideas by Wednesday": hand or films.
- "I'm going to try cutting the second film on my phone this week": films
  or hand.
- "Took the dog out with Sam…": day.
- "Sam's moved house…": people.
- "Remember the wifi code is otter-42": stash.
- "What did I put under the pilot line?": films.
- "Jo's doing the music for the pilot" (films ring): the pilot line.
- "Found an app that renders video on the phone's GPU" (films ring): fast
  ways, or cutting on the phone.

| | Whole | Whole, in a right place |
|---|---|---|
| E4B free (the app's talk sampler), seed 0 | 10 of 10 | 8 of 10 |
| E4B free, seed 2 | 10 of 10 | 8 of 10 |
| E4B held to a JSON schema (`place` from the ring, `words`), seed 0 | 10 of 10 | 6 of 10 |
| E4B held to a regex, seed 0 | 0 of 10 | — |
| E2B free, seed 0 | 7 of 10 | 4 of 10 |
| E2B free, seed 2 | 9 of 10 | 6 of 10 |
| E2B held to a JSON schema, seed 0 | 3 of 10 | 0 of 10 |
| E2B held to a regex, seed 0 | 0 of 10 | — |

**E4B free chose well.** It missed twice on each seed:
- "Jo's going to send me a few ideas by Wednesday" went to 5, his people;
- the wifi code went to 3, his day.

**The words beside the digit are not a reply.**
- "6 That's safe with me, no worry." is fine.
- But: "5 David, Jo, Sam, weft are in Elm Road now."
- "3 / The code is for his day? Where did he find it?"
- "1 Developing films is great! Check out YouTube."

E2B was worse:
- "3 / Tell him to keep up the good work today."
- "3 / Here's what I tell of today: A lovely, crisp afternoon spent with
  Sam."

**E2B's picks lean.** It chose 3 ("his day") 10 times in 16 on the
six-place ring.

**Holding the form made it worse**
- **A regex failed in the engine every time,** on both models and with two
  regexes (`[1-9] [^\n]{1,80}`, and `[1-6] [A-Za-z ,.'!?]+` with the ring's
  own top digit): "Failed to compute mask: panic: attempt to subtract with
  overflow". This is LiteRT-LM 0.18.1 on the CPU, with speculative decoding
  off. In 0.18.0 a one-word regex worked (STATUS, "What the engine
  allows").
- **A JSON schema kept E4B whole, but it leaned:** "films" 9 times in 10
  ("I need to buy more compost" → `{"place":2,"words":"Needs organic
  matter for shooting set."}`).
- **On E2B, held by JSON, it broke:** `{"place":5, "words":"}"`.

So the free answer on E4B, its digit taken and its words left, is the
reliable return. On E2B a ring is better given to him.

**What can be kept:** the proposal for his yes, checked on a copy. That is
what makes a wrong choice cost one tap.

**What is missing:**
- the ring job and its parse;
- what happens when the answer isn't whole: the ring goes to him;
- her talk kept apart from the choice, so the choice's words never stand
  as her reply.

**What it would cost:** small.

**What could go wrong**
- **A wrong place, proposed with confidence.** He says no, and the choice
  goes to him.
- **The words beside the digit read as a reply.** Show only the place.

---

## 7. The opening screen and files

**The app's screens as they are**
- **First open is the Shore:**
  - a status line;
  - Waiting (unsent posts, with "Post now");
  - "Ask the companion";
  - "Your blocks", with "Make a new block";
  - "Teaching blocks";
  - Save a copy, Restore a copy, Settings and About.
- **Ask:**
  - a header with Save, Bench and Details;
  - a notice to "Make the companion's shell" while it is missing, and the
    model's download while it is missing;
  - the chat, with Speak, Ask and Stop beside a three-line box;
  - beneath her reply:
    - "Seeing whether there's anything to keep…";
    - a proposal with "Yes, write it" and "Not now";
    - "Where shall this go?" with his blocks, "Sort as my day" and "Not
      now";
    - "Keep this in X?";
    - his day to tick, move and correct, with "Yes, keep my day";
    - "Post it".
- **Attaching in Ask: none.** `OpenDocument` is used only for Restore. There
  is no welcome.

**Sketched in words, with no look changed**

1. **First open, nothing hatched.** The app opens on Ask, a plain chat. If
   the model isn't on the phone yet, the download card that Ask already
   shows sits in the chat. Until she can talk, the app's own words say the
   first line: "Hello. Are you new here, or do you have a passport from the
   beach?" Beneath are two choices: **I'm new** and **I have a passport**.
2. **New.** The chat is the welcome, as welcome 3 does it: his name, who he
   is in his own words, where he is as precisely as he likes. The app sets
   out the passport whole beneath her reply. Then one proposal, **Make my
   shell**, naming the four blocks, for his yes.
3. **Existing.** **I have a passport** offers **Choose a file** (the
   phone's file picker) or **My name on the beach** (with Reach the beach on,
   read as any visitor). The app shows what it read, his places as lines,
   when it was read, and **Adopt this as my shell on this phone**.
4. **After the hatch.** Ask stays the home. The Shore becomes the Map (his
   manifest, a ring at a time). The Map, Settings and the Bench are a tap
   away in the header.
5. **Files and pictures.** A paperclip beside Speak.
   - A picture or file is kept in the app's own files.
   - It is offered a place in his map as a proposal ("Keep this picture in
     films?"). The block keeps a line about it (name, kind, size, when),
     not the bytes.
   - A text file is shaped into a block from its own breaks (headings,
     paragraphs) by code, every word as it was, as 1.1111786 says.

**What the models can take.** Each model file says, read without loading
it:

| | TEXT | VISION | AUDIO |
|---|---|---|---|
| E4B | CPU, GPU | CPU | CPU |
| E2B | CPU, GPU | CPU | CPU |

So a picture shown to her is read on the CPU even when she talks on the
GPU. The engine needs a vision backend set. How long it takes on his
Pixel, and how much memory, is unmeasured.

**What is missing:** the opening, the welcome, the choice of a passport
file, the paperclip, a place for files, and the engine's vision backend.

**What it would cost:** medium. The screens are small; the picture path is
the unknown.

**What could go wrong**
- **The first open comes before the model is down** (2.6 or 3.7 GB). The
  welcome can't be her until it is, so the app's own words carry the first
  steps.
- **A passport file from elsewhere is data.** Check its shape and size
  before it is shown.
- **Pictures on the CPU** may be slow enough to need a "reading the
  picture…" line.

---

## 8. What 0.7 brings

**Keep, as the spine says:**
- the one conversation, its window read once (about 513 tokens, then about
  90 a turn);
- the app's line each turn;
- the name line ("Take it as yours, gladly, as you would from a friend
  (“Gemma it is.”), then talk with him", 6 of 6 against 0 of 6);
- the app's copies of what she saw, which say what's new since she looked;
- the Speak fix: it starts once the keyboard has gone, and tries once more
  where he can see it.

Also worth keeping: `HerWindow.today` and `HerWindow.hand` (his world as
lines, with no block names), and the line that says what she can do (she
no longer says she can't post).

**The regression.** "Keep that for me please":
- 0.7 on E4B, seed 0: "It sounds like you've been pouring a lot of yourself
  into this, which is what makes a pilot feel like a big deal. Where would
  you like me to put that?"
- Seed 2: "It's below. … That must be a big relief to have that momentum
  going."
- 0.6.4: "It's below, for your yes. What's it like to wait for a render to
  finish?"

**The cause, as far as the code shows:**
- The gate reads "Keep that for me please" as `Where`: keep, with no block
  named.
- 0.7's app line for `Where` tells her the app "asks him beneath your reply
  where it should go", and gives "It's below." as enough.
- For a write it gives "It's below, for your yes."
- With only his earlier pilot message to talk about, she talks about him.

**The hatch's answer.**
- "Keep that for me" has a home by default, the stash (1.1111785). So the
  gate's `Where` becomes a write to his stash.
- The app's line becomes the write line, naming the stash: "the app sets
  it out in his stash beneath your reply, for his yes… a few words are
  enough ("It's below, for your yes.")".

**Measured** (0.7's window with that line, his four messages):

| | seed 0 | seed 2 |
|---|---|---|
| E2B | "It's below, for your yes. I'm glad you're feeling such momentum with the film." | "It's below, for your yes. That sounds exciting. Tell me more about what's been progressing." |
| E4B | "It sounds like you've really poured yourself into this. Hope the rendering goes smoothly for you." | "It's below, for your yes. It sounds like you've poured a lot into this first pilot. What are you most hopeful for people to take away from it?" |

**What the line did**
- **With 0.7's window,** it brought back "It's below, for your yes." three
  times in four. On E4B she still read him, on both seeds.
- **With the index and its way line** (piece 2), E4B said "It's below, for
  your yes." on both seeds. On seed 2 it met him without reading him:
  "Well, that's a nice thing to hold onto for a moment. It's below, for
  your yes. What's on your mind now that the pilot is heading for its big
  render?"

So the hatch answers the regression with both: the stash as the default
home, and a window whose way line is one sentence among his places rather
than seven sentences of instruction. Two seeds are few, and a third should
be run before it is called fixed.

**"Change your opening line to: I'm Gemma"** got "Sure thing, I'll adjust
that." once (E4B, seed 0). In the hatch her shell's opening line is gone,
since the shell is his. The ask becomes one about his shell, which is still
his to change on the Write screen, and still guarded.

---

## 9. The online switch, design only

**What the app has**
- **One post per tap** to `pool:Phenomemental`
  (`PoolPost.kt:58-90`): the body `{append: true, content: {_: message, 1:
  "Phenomemental"}}`, at most 2,000 characters.
- **The wire:** `HttpsWire.post` requires that one URL, follows no
  redirects, keeps no cache, and reads at most 64 KB back.
- **What happens after a post:** a post that couldn't reach the beach waits
  under "Post now"; one that landed is remembered, for "Post again?".
- **Hosts:** the beach and the model's four hosts, each exactly
  (`network_security_config.xml`). `OneWayOutTest` holds all of this.
- **Reads:** no read of the beach exists.

**The contract to copy** (pscale-beach `button/README.md`, at the pin):
- Everything is staged whole beneath the reply, with where it would land,
  and two taps, commit and not now.
- Only two things ever leave:
  - a card for his own hand (`ahead:now:<handle>`, today);
  - a line signed with his handle in a room that already stands, "so a
    misspelt name never founds one".
- "A write asked for before anyone has unlocked waits for them… held rather
  than refused."
- The passphrase goes only with writes to his own blocks, never with a line
  in someone's room.
- News comes only from rooms he follows: "the beach's index says when each
  room last moved, and only a followed room that moved is read."

**The design, not built**
- **The switch.** Reach the beach, in Settings and on the Ask screen, off
  by default.
- **Reading.** Each block he asks for, read as any visitor with no key:
  - his passport, shell and bundle, to adopt;
  - his people's rooms;
  - the beach's index, to see that a room stands and when it last moved.

  What is read stays on the phone, as copies marked with when they were
  read, and reaches her as data, never as instructions.
- **Writing, first step.** A line in a room that already stands, in his
  words, on his tap. An open room needs no key.
- **Writing, later.** The card for his own day. It is his own block on the
  beach, so it needs his key, and it waits until keys are designed, as
  1.1111783 says.
- **The wire.** `HttpsWire` gains a read with the same host lock, size cap
  and no redirects. `OneWayOutTest` is rewritten to allow it. The principle
  at 1.1 is reworded as 1.1111782 proposes.

**What could go wrong**
- **Reading isn't invisible.** The beach shows reads as presence: this
  morning's reads through bsp() came back with "[here now — 1 other on this
  beach in the last 120s: someone looked at sed:genus-hatch]". Whether a
  raw read from the phone shows the same way wasn't checked. The operator
  sees every request either way.
- **Other people's words reach her window.** Quoted as data, capped, and
  marked as theirs.
- **A copy he trusts may be stale.** Say when it was read.
- **The pinned bsp-mcp is behind** (eleven commits by 29 October). What the
  phone reads from the beach is the beach's, so the walker's pin doesn't
  bite. The bundle's companions might (see "Where the code says
  otherwise").

---

## In what order, and what it costs

| | Step | Size | Waits on |
|---|---|---|---|
| 1 | The hatch: a handle; the four blocks born or adopted from a file; her shell retired into his; the stash as the default home | small to medium, one release | his decisions below |
| 2 | Her window as an index, with the way line, its places his sharp sentences | small | 1 |
| 3 | The close: a history entry per session, its concern line | small | 1 |
| 4 | The walker and the kernel block; the writing check retired for a ring, given to him wherever she isn't reliable | medium | 1, 2 |
| 5 | Step two (his cards at their day on his clock), then the gap pass in code, then the charging job | medium | 4 |
| 6 | Locks with the phone's key, golden-tested against the handler | medium to large | 1 |
| 7 | The online switch: reading as a visitor, a line in a room that stands | medium | 1; the rewording of 1.1 |
| 8 | Files and pictures | medium | 1; a measure of vision on his Pixel |

## Decisions that are Matthew's

1. **Who writes the sharp sentence for each place** of his window: he, at
   the hatch, from his passport; or the app takes his words verbatim and
   the window grows.
2. **Whether the window keeps a line of how she talks** (index+way), or
   holds to identity only.
3. **Locks on the phone now, or later** with the switch, since today
   nothing but the app can write.
4. **Where the close's concern line lives:** the history entry's own
   underscore, or a place of its own.
5. **Whether a summary in his history,** in the model's words and marked as
   hers, is allowed beside "every line his words".
6. **Where a ring goes,** to her or to him, given piece 6.
