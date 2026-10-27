# Pocket beach — Phase 0 findings

Read on 27 October 2026 for Matthew Bushell. Nothing has been built. No code
from the repos below was run: everything here comes from reading the code and
from bsp reads.

**What was read (pinned, so later pieces test against the same thing):**

| Source | Commit | Date |
|---|---|---|
| `pscale-commons/bsp-mcp-server` | `27908e1` | 26 Oct 2026 |
| `pscale-commons/pscale-beach` (the beach handler; not named in the plan, see §3) | `4770a49` | 27 Oct 2026 |
| `google-ai-edge/gallery` | `8f5f2a0` | 13 Oct 2026 |
| whetstone (whole disc, branches 2 and 4 in full), block-conventions 3.4, 3.5, 4.22, sunstone 1.6 | read through `bsp(agent_id="pscale", …)` | 27 Oct 2026 |

All three repos are public. bsp-mcp-server and pscale-beach are MIT-licensed,
Gallery is Apache 2.0.

---

## The short version

1. **bsp-mcp-server is only half of bsp.** It is the walker plus a router. It
   hands every write and every append to a second program, the beach handler
   (`pscale-beach`). That program has its own copy of the parser and walker,
   and it does the next free slot, supernesting, arrival stamps, seeding a new
   block's opening line, the shape gate and the block listing. To behave "exactly
   as bsp-mcp-server does", the phone has to reproduce both halves.
2. **Decision: (a), a Kotlin port.** It is proven by golden tests from both real
   programs running together in Node. The beach repo already ships an offline,
   folder-backed way to run its real handler, built for this kind of use.
3. **Gemma 4 E2B** is one public file of 2.59 GB, under Apache 2.0, from
   Hugging Face. It needs no sign-in and no licence click, and our app can fetch
   it on first use with a plain download that can resume. Gallery runs it with
   LiteRT-LM 0.18.0 on the GPU. LiteRT-LM can force JSON-shaped answers, but
   Gallery doesn't use that, so it is untested with this model.
4. **Concerns that change the plan** (details in §6):
   - On the beach today, only a block's root wraps past nine. A position inside
     a block holds nine and refuses the tenth. "Fill a position past nine" can't
     happen as written.
   - `agent_id: "pocket"` means something different in bsp-mcp-server.
   - Android's own backup sends the blocks to Google.
   - I can't create the signing key or set repository secrets from here.
   - The 72-test acceptance battery's fixture files aren't public.

---

## 1. bsp-mcp-server: what is there

### The walker and shape derivation

- **`src/bsp.ts`** holds the address parser and walk primitives. It is a
  TypeScript port of `bsp2-star.py`. The canonical Python isn't in any public
  repo; CLAUDE.md places it on David's machine.
  - `floorDepth` counts the root's `_` chain down to a string.
  - `parseSpindle` handles the address rules:
    - at most one decimal point, digits only;
    - the left side pads with `0` (meaning `_`) up to the floor's width;
    - trailing zeros are stripped;
    - a left side wider than the floor is refused.
  - `formatAddress` writes the short form. `readAt` reads; `writeAt` writes,
    with subnest-on-growth: a string in the way moves down into the new node's
    `_`.
- **`src/bsp-fn.ts`** holds `bspRead`, `bspWrite` and the formatters.
  - Six shapes: block, path-walk, disc, point, path-walk+descent and star.
    Pscale = floor − depth; the root has no pscale.
  - Writes infer their shape from the content:
    - a string is a point;
    - an object replaces the subtree at the spindle;
    - an array or sparse object is a disc;
    - a trailing `*` writes inside the hidden directory.
  - Reads carry extras that the returned text shows:
    - `beneath`: the ring under the terminus, as full-width addresses;
    - `head`: the newest entry of an accumulator;
    - a padding note when a short address walked the `_` chain;
    - `stamp`: an ISO time at field 3.
  - Writes report `landed` and a `landing_note`.
  - Address labels are **full width**, e.g. `20` rather than `2` at floor 2.
- **`src/tools/bsp.ts`** is the tool handler, and it does more than the walker:
  - It turns JSON-as-a-string content back into objects.
  - It refuses writes to `pscale`.
  - A whole-block read longer than 6,000 characters returns the disc at pscale
    0 instead, with a note. Spindle `0` with no attention means "really the
    whole block".
  - Any read longer than 30,000 characters is refused with a smaller read named.
  - Writes **load the whole block, run `bspWrite` on it locally, then send only
    the value at the spindle to the beach**. A read-back walk of what landed is
    added to the reply.
  - Appends: plain-string entries get an arrival stamp, and the reply reports
    owed summaries.
- The clock line (`now · ISO · address · voicing`) on every reply is added in
  `src/server.ts`, outside the handler. Floor-10 blocks also get "N days
  ago / AHEAD" tags beside addresses (`src/temporal.ts`, 838 lines).

### The storage adapter interface

The whetstone (4) describes an adapter interface: `load_block`, `save_block`,
`delete_block`, `list_blocks`, lock state and allocation hooks. It says
filesystem adapters "implement the same surface". **The code has no such
interface.** `src/db.ts` hard-codes two substrates and no others:

- the bundled sentinels, read-only and held in memory;
- federated beaches over HTTP.

No filesystem or local adapter exists. So under option (b) there is nothing for
a "Kotlin storage adapter" to plug into. What sits beneath bsp-mcp-server is a
whole beach.

### The sentinel-bundled teaching blocks

- `src/sentinels.ts` is the one registry. It has 27 entries: 26 blocks plus
  `protocol-paywall`, an alias of `payway`. About 696 KB of JSON in
  `src/*.json`, all read-only. `bsp(agent_id="pscale")` with no block lists the
  names.
- The live tool description lists only nine of them. The registry is the source
  of truth.
- MIT licence, so the app must carry the notice.

### The smoke tests

- **`smoke:unit`** (`scripts/smoke-bsp.ts`) has 23 assertions. It runs
  `bspRead` and `bspWrite` against sunstone and whetstone:
  - a whole-block read;
  - a path-walk at `1.1`;
  - a descent at `1`, pscale −1;
  - the disc at 0;
  - the star `7*`;
  - one point write and two subtree writes.

  It tests the walker only, not the handler or the beach. To run it against
  Kotlin, its assertions need porting (they're short).
- Other tests worth carrying over:
  - bsp-mcp-server: `smoke:parser` (446 lines, the parser's ground truth),
    `smoke:hollow-ack`, `smoke:numeric`, `smoke:shapes`.
  - pscale-beach: `smoke:append`, `smoke:append-spindle`, `smoke:floor`,
    `smoke:voicing`, `smoke:born`, `smoke:index-sizes`.
- **The `bsp-test` sentinel** is a 72-case acceptance battery: 8 batteries of 9.
  Expected answers are written as prose, with marker substrings in batteries
  5–8. Its fixture files are **not in any repo**: `test-spatial-floor3.json`,
  `absorb-floor1/2/3.json`, and the `subnest-` and `super-` before/after pairs.
  Nor is its runner, `run-bsp-tests.py`. The battery says they live in
  `~/Downloads/bsp-test-materials/`. Only the sunstone batteries can run from
  public material. `scripts/bsp-cli.ts` exists as the TypeScript shim for the
  runner.

## 2. The whetstone, read through bsp

`bsp(agent_id="pscale", block="whetstone")` gives the disc at pscale 0, because
the block is 40,810 characters. Branches 2 and 4 came through bsp as
path-walk+descent, which cuts lines at 150 characters, so the full text was read
from `src/whetstone.json`. That is the file the sentinel serves.

- **Branch 2 (shape derivation).**
  - P_end = floor − number of walked digits.
  - No spindle and no P gives the block. A spindle alone gives a path-walk. P
    alone gives the disc at depth floor − P.
  - A spindle with P ≥ P_end gives a point, or a null with a note when P is off
    the spindle. A spindle with P < P_end gives a path-walk plus a
    breadth-first descent over digits 1–9 only.
  - A trailing `*` enters `terminus._` and applies the rest inside.
  - Disc rule: emit a position when its last step is a digit 1–9, or when the
    whole walk is on the root's `_` chain and lands on the floor string.
  - 2.7: addresses keep their meaning when the floor grows, because the short
    left side pads with zeros.
  - 2.8: the read shape is a choice, and the whole block is almost never the
    right one.
- **Branch 4 (storage adapter).** The interface above; atomic at the single
  block; no multi-block transactions; plus the procedure for swapping adapters.
- **What the whetstone does not cover:** the next free slot on append,
  supernesting at nine, arrival stamps, and listing blocks. These are written in:
  - block-conventions 3.2, 3.4, 3.5, 4.22 and 9;
  - sunstone 1.5–1.6;
  - shell-genome 1;
  - the bsp tool's own parameter text.

  The exact behaviour is fixed in one place only: the beach code.

## 3. Duties done at the beach, not in bsp-mcp-server

These are the things the phone's storage layer must do itself. Prose means
where the rule is written; Code means where it is actually decided. The code
paths are in `pscale-beach` unless marked.

1. **Next free slot on a root append.**
   - The first free zero-free digit path whose length equals the floor, in
     lexical order: 1–9, then 11–99, then 111–999.
   - A number with a zero in it is never an entry.
   - Prose: block-conventions 3.4; sunstone 1.64; shell-genome 1.
   - Code: `api/floor.js` `nextZeroFreeSlot`, `zeroFreePath`, `rawSet`.
2. **Supernest when the root is full.**
   - Wrap `{_: old}`, raise the floor by one, and land at `11…1`.
   - The wraps come at the 10th entry, the 91st, then the 820th: 9 slots, then
     81 more (11–99), then 729 more (111–999).
   - Prose: sunstone 1.63; block-conventions 3.2.
   - Code: `api/floor.js` `appendWithSupernest`.
3. **Append beneath a node (append with a spindle).**
   - The node must exist and be an object. Otherwise the reply is 404, or 409
     `append_at_leaf`.
   - The entry takes the next free digit 1–9 under it.
   - **A full node refuses** with 409 `node_full`, naming the nine slots and the
     oldest by stamp. Nodes have not wrapped since 2026-08-08.
   - Prose: `proposals/2026-08-08-nine-slots-and-the-pointer.md`; `ways:grain`
     5, a beach block that isn't bundled.
   - Code: `api/floor.js` `appendAtNode`; `api/pscale-beach.js`
     `handleAppendAtSpindle`.
   - ⚠ The bsp tool's own `append` description still says the node
     "supernests when its 1-9 fill", and so do two comments in the handler. They
     are out of date.
4. **Arrival stamps, split across the two programs.**
   - bsp-mcp-server wraps a plain-string append as `{_: text, 3: ISO}`, to the
     second (`src/tools/bsp.ts` `stampPlainAppend`).
   - The beach adds `3: ISO` with milliseconds to an object entry that has no
     valid ISO there (`stampAppendTimestamp`).
   - Gray entries aren't stamped.
   - On read, the leading ISO at field 3 shows as the entry's stamp, and field 3
     is left out of "beneath".
   - Prose: block-conventions 4.22 and 9.
5. **Owed summaries.**
   - After a root append, the beach lists the zero-slot summaries now due and
     unpaid (`api/floor.js` `owedSummaries`).
   - bsp-mcp-server prints "ⓘ summary owed …" with the opening lines of the
     span.
   - Prose: block-conventions 3.5.
6. **Seeding a new block's opening line.**
   - A block born by a write below the root, or by a root append, gets
     `_ = "<name> at <origin>."`. Marks, presence and liquid get special lines.
   - A whole-block write with no `_` chain reaching a string is refused
     (`no_floor`).
   - Prose: sunstone 1.51; `proposals/2026-06-03-block-floor-invariant-and-malformed-repair.md`.
   - Code: `api/floor.js` `defaultIdentity`, `hasFloor`; `handleStandardWrite`.
7. **Where a write lands at the terminus.**
   - A string written where an object stands sets that object's `_` and keeps
     its children. An object replaces.
   - Subnest-on-growth applies on the way down.
   - Prose: block-conventions 3.5; whetstone 5.71.
   - Code: the beach's `writeAt`, which mirrors bsp-fn's point path.
8. **Shape gate.**
   - Only `_` and the single digits 1–9 are allowed as keys.
   - Objects or arrays sent as JSON strings are refused (400 `invalid_shape`).
   - Prose: well-formed 1.
   - Code: `validateShape`. bsp-mcp-server first turns JSON strings back into
     objects.
9. **Listing blocks (block omitted).**
   - The reply is `{_, origin, blocks, bytes, touched, born, now}`:
     - `blocks`: the names, sorted;
     - `bytes`: each block's stored size;
     - `touched`: its last write;
     - `born`: its first write.
   - bsp-mcp-server renders it as `[beach index @ …]` with sizes.
   - Prose: block-conventions 4; `docs/protocol-pscale-beach-v2.md`.
   - Code: the GET branch of `handler`, `listBlockNames`, `blockBytes`;
     bsp-mcp-server `formatBeachIndex`.
10. **Touched and born bookkeeping.**
    - Every save stamps `touched`. A first write stamps `born`, and the reply
      carries `born: true`.
    - bsp-mcp-server then prints "ⓘ new: this write created …".
    - Code: the beach's `saveBlock`. The phone needs a small record beside the
      block files to hold these.
11. **One append at a time per block.**
    - The beach uses a per-block mutex (`withAppendLock`).
    - On the phone: one in-process lock per block, plus write-temp-then-rename.
12. **Present on the beach but out of scope here**, listed so nothing drops
    silently:
    - locks and their hash salts; sed: and grain:;
    - caps and the kept last copy;
    - the tables listing; window resolution;
    - a sweep that runs only on a block literally named `presence`. A pocket
      block with that name would behave differently from the beach.

**The beach has its own second walker** (`bspCanonical`). bsp-mcp-server uses it
for point, disc and descent reads on URL beaches. Its answer differs from
bsp-mcp-server's own walker in three ways:

- it labels addresses in short form (bsp-mcp-server re-pads them);
- it has no `head`;
- it has no padding note.

So one call on one block can render slightly differently depending on the route.
The phone has no route to choose between. **The reference should be
bsp-mcp-server's own local path** (`bspRead` + `formatRead`), the one it uses for
`agent_id="pscale"`.

## 4. How the local bsp is made: (a) port to Kotlin

**Why (a):**

- **What has to be reproduced is two programs joined by HTTP.** One is
  bsp-mcp-server's handler and walker; the other is the beach handler that
  finishes every write. Option (b) would embed both in a JavaScript engine,
  joined in-process by a fake `fetch` and a fake Redis. It would also need
  stand-ins for `node:crypto`, `@upstash/redis`, `process.env`, and `hash-wasm`
  (WebAssembly, which QuickJS doesn't run).
- **That glue is new code that no upstream test covers.** Most of both programs
  would come along unused or stubbed: locks, gray, groups, federation, the
  reflection, orientation, bundles and the clock. Once stubbed, it is no longer
  "the original".
- **The part the phone needs is small and pure.** Rough line counts of logic to
  port:

  | Part | Lines |
  |---|---|
  | `bsp.ts` parser and walk | ~200 |
  | `bsp-fn.ts` shapes and formatters | ~600 |
  | `floor.js` append, supernest, owed summaries | ~150 |
  | the beach's `writeAt`, shape gate, seeding | ~100 |
  | a trimmed handler flow | ~300 |

  That is roughly 1,200–1,600 lines of Kotlin.
- **Fidelity is proven, not assumed.**
  - CI runs the two real programs together in Node: bsp-mcp-server's
    `handleBsp`, pointed at the real pscale-beach handler on its own offline file
    rig (`scripts/local-beach.mjs` + `file-redis.mjs`).
  - The real programs produce the expected outputs. Kotlin runs the same calls
    on the JVM, and any mismatch fails the build.
  - Beyond the 60+ hand-chosen calls, CI can push thousands of random blocks
    and calls through both. That covers paths a hand-made corpus misses, which
    is the main risk of a port.
- **Each call is compared twice:**
  - the stored block JSON after the call, byte for byte, in JavaScript's key
    order (digit keys ascending, then `_`);
  - the returned text, with the clock frozen.
- **What would change my mind:** if the port keeps drifting as upstream changes.
  Upstream changes most weeks, and if keeping up became a burden, embedding
  would win on upkeep. Either way, upstream is pinned to the commits above, and
  goldens are regenerated only when Matthew chooses to move.

**Traps to test for on purpose:**

- JavaScript's key order in JSON output.
- Timestamp formats (with and without milliseconds).
- `trim()` and regular expressions on non-ASCII text.
- Numbers and booleans as leaves (rendered via `JSON.stringify`).
- Sorting with `localeCompare`.
- Cutting text at 150 characters (JS counts UTF-16 units, as Kotlin does).
- `toFixed(10)` on numeric spindles. Not reachable from the tool, which takes
  spindles as strings, but it is in the parser.

## 5. Gemma 4 E2B with LiteRT-LM (AI Edge Gallery)

From Gallery's code, with the Hugging Face facts checked on 27 Oct 2026.
Gallery's root `model_allowlist.json` is out of date (it only lists Gemma 3n). The
list the app actually loads is `model_allowlists/1_0_20.json`, which it fetches
from GitHub at runtime.

**Where the model file comes from**

- Hugging Face repo `litert-community/gemma-4-E2B-it-litert-lm`, file
  `gemma-4-E2B-it.litertlm` (LiteRT-LM format, not a `.task` file).
- Gallery pins it to commit `6e5c4f1e395deb959c494953478fa5cec4b8008f`
  (`model_allowlists/1_0_20.json:4-47`) and builds the URL as
  `https://huggingface.co/<repo>/resolve/<commit>/<file>?download=true`
  (`Android/.../data/ModelAllowlist.kt:97`):
  `https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/gemma-4-E2B-it.litertlm?download=true`
- The same repo holds other builds: `-gpu`, `-web`, an uncompiled NPU build,
  Tensor G5/G6 NPU builds for Pixel 10 and 11, Qualcomm and Intel builds.
  - Gallery uses none of them. It uses the one file above on every phone.
  - There is no Pixel 9 (Tensor G4) build.
  - The `-gpu` file isn't explained anywhere, so we shouldn't guess. Use
    Gallery's file.

**Does the download need a sign-in or licence acceptance?** No.

- The Hugging Face API reports `"gated": false`, `"private": false`, licence
  `apache-2.0`.
- The model card's licence link is plain Apache 2.0 for Gemma 4. This differs
  from Gemma 3n, which was gated under Google's Gemma terms.
- Gallery has a Hugging Face sign-in flow (OAuth) and a terms dialog, but only
  for gated models. Neither applies to this one.
- Its OAuth client ID is a placeholder in the open-source code, so it couldn't
  be reused anyway. We don't need it.

**Size**

- 2,588,147,712 bytes, about 2.6 GB.
- SHA-256 `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c`.
- Gallery asks for at least 8 GB of phone memory. The Pixel 9 Pro has more than
  that.
- The model card's speed and memory figures are for other phones (Galaxy S26),
  not the Pixel 9. Speed on Matthew's phone is unmeasured.

**Can our app fetch it on first use?** Yes.

- An anonymous request redirects to Hugging Face's CDN and answers range
  requests. So a plain HTTPS GET with no token works, and an interrupted download
  can resume.
- Gallery's way (`DownloadRepository.kt`, `worker/DownloadWorker.kt`):
  - a WorkManager job running as a foreground service with a progress
    notification;
  - `HttpURLConnection`, resuming from a `.gallerytmp` file with a `Range`
    header, renamed when complete.
- For us: add a Wi-Fi-only constraint (Gallery has none), and check the SHA-256
  before use (Gallery doesn't).

**How Gallery runs it**

- The library is `com.google.ai.edge.litertlm:litertlm-android:0.18.0`. Gallery's
  minSdk is 31, the same as ours.
- `LlmChatModelHelper.kt`:
  - `Engine(EngineConfig(modelPath, backend = Backend.GPU(), maxNumTokens = …,
    cacheDir = …)).initialize()`;
  - then `engine.createConversation(ConversationConfig(samplerConfig,
    systemInstruction, tools, …))`;
  - then `conversation.sendMessageAsync(...)`.
- The manifest declares `libOpenCL.so` and `libvndksupport.so` as optional
  native libraries, which LiteRT-LM's GPU path requires.
- Gallery's defaults for this model: context 32,000 tokens, max output 4,000,
  "thinking" available. We'd keep thinking off for speed.
- **Structured output:** LiteRT-LM 0.18 can constrain an answer to a JSON schema
  (`ConversationConfig(enableResponseFormat = true)`, then
  `ResponseFormat.json(schema)` on `sendMessage`). It also has function calling
  (`@Tool` tools with constrained decoding), which Gallery uses for its agent
  skills.
  - Gallery never uses the JSON-schema path, so it is unverified with Gemma 4
    E2B on the GPU.
  - Piece 3 should try JSON-schema first and fall back to function calling.

**Licences**

- Gallery and LiteRT-LM are Apache 2.0, so the loading and download code can be
  copied if we:
  - keep the licence and copyright lines;
  - mark the files we change;
  - use no Google or "Edge Gallery" branding.
- The model is Apache 2.0 too.
- Gallery's Firebase and analytics parts are optional and are left out, per
  principle 5.

## 6. What in the plan is wrong, risky or unclear

In order of weight.

1. **"Past nine" works only at a block's root.**
   - The plan expects positions to wrap:
     - Piece 1: append "with next-free-slot and supernesting".
     - Piece 2: "Shows when a position is full at nine and has wrapped".
     - Matthew tries: "fill a position past nine".
   - On the beach since 8 August, only the root of a block wraps. A position
     inside a block holds nine and refuses the tenth (§3.3). Ordinary writes
     stop at nine too, because keys are digits 1–9.
   - **Change:**
     - Positions show "full at nine". Only a block's root shows that it has
       wrapped.
     - Matthew's test becomes "add ten lines to a block's root and see it wrap".
     - The pocket matches the beach's code, not the stale tool text.
     - Separately, David may want to know the live `append` description is out
       of date.
2. **`agent_id: "pocket"` is a new meaning, and other names need a rule.**
   - In bsp-mcp-server, `agent_id` is required. Anything that is neither a URL
     nor `"pscale"` is a bare handle on the default beach. So `agent_id
     "pocket"` with block `"garden"` means the block `garden:pocket` there, and
     with block omitted it reads a block named `:pocket`.
   - The plan's reading treats the pocket like a beach URL: names as typed, and
     an omitted block lists. That is the sensible reading, but it is a
     departure.
   - **Change:**
     - State the departure once in STATUS.
     - In the golden harness, map `"pocket"` to the local beach's URL.
     - Refuse bare names (`"weft"`), `sed:` and `grain:` with the same "can't
       reach the beach" message, since in bsp-mcp-server those all reach the
       beach. The plan only mentions URLs.
     - `face` and `tier` are only advisory in bsp-mcp-server. Accept and ignore
       them there too.
3. **The reference for the golden tests is two programs, not one.**
   - "Run through the original bsp-mcp-server in Node" can't store anything on
     its own. It needs pscale-beach beside it, both cloned at pinned commits in
     CI.
   - Some of what bsp-mcp-server returns depends on the clock or on other
     sessions:
     - the clock line on every reply;
     - floor-10 time tags;
     - the reflection and orientation lines;
     - bundle windows and clock frames.
   - **I'd carry:**
     - the read-back after a write;
     - the "new block" note;
     - owed summaries;
     - the disc-instead-of-whole probe;
     - the size refusal.
   - **I'd leave out**, and say so in STATUS: the clock line, floor-10 time tags
     (which would bring in 838 lines of `temporal.ts`; a pocket block reaches
     floor 10 only by deliberate authoring), reflection, orientation and bundles.
   - Matthew to confirm.
4. **The 72-test battery can't fully run.** Its fixture files and runner are on
   David's machine only (§1). Use the sunstone batteries plus our own corpus. If
   David will share `bsp-test-materials/`, conformance gets stronger for free.
5. **Android's own backup sends the blocks off the phone.**
   - It copies them into Matthew's Google account. That runs against principle
     5 ("nothing leaves the phone") and "private by place", at least in letter.
   - Google describes these backups as encrypted with the phone's screen lock.
     I haven't checked that for this case.
   - I'm also not sure a restore happens for an app installed from a downloaded
     APK rather than the Play Store. It needs testing before anyone relies on it.
   - **Change:** Matthew chooses this knowingly. "Save a copy" stays the
     dependable path.
6. **The signing key.**
   - This session can't set repository secrets: there's no `gh`, and no GitHub
     tool for secrets.
   - Making the key here would pass it through this session's record. That goes
     against the beach's own rule that a key is a pointer, never a value.
   - **Change:**
     - Matthew makes the key on his own computer, with Android Studio's "Generate
       Signed App Bundle or APK" or `keytool`.
     - He pastes it into the four secrets on GitHub's web page. I'll give exact
       steps when Piece 1 gets there.
   - **Risk:** if that key is ever lost, no update will install over the old app,
     and uninstalling erases the blocks. Keep the key in a password manager, and
     make "Save a copy" before any reinstall.
7. **Upstream moves fast.** bsp-mcp-server changed many times this month alone.
   "Same geometry as the beach" has to mean "as of commit X". Matthew decides
   when the pocket moves to a newer one.
8. **Piece 3: one call or several.**
   - "Make a block called garden and put 'plant the bulbs' under autumn" is at
     least two writes, three if "autumn" doesn't exist yet. The model must also
     find which position says "autumn".
   - The plan says "a bsp call".
   - **Suggestion:**
     - Allow a short list of calls, each shown in plain words.
     - One Yes runs them in order and stops at the first failure.
9. **Speech must stay on the phone.** Use Android's on-device recogniser
   explicitly (`SpeechRecognizer.createOnDeviceSpeechRecognizer`, Android 12+).
   The default recogniser may send audio to Google, against principle 5.
10. **The emulator check in CI.** An emulator on GitHub's runners needs
    hardware acceleration. I believe their standard Linux runners have it now,
    but it's unverified. If not, CI builds and runs the JVM tests, and the
    install check happens another way.
11. **The teaching shelf describes things the pocket refuses** (locks, grain,
    sed:, play, federation). That's fine for a teaching shelf. The About page
    might say so in a line.
12. **The model is big and unmeasured on this phone.**
    - The download is 2.6 GB.
    - The model card's figures are for a Galaxy S26, and nobody has timed this
      model on a Pixel 9 Pro's GPU. Each answer could take seconds.
    - The plan already shows how long each answer took, which will tell us.
    - The model card itself calls Android's AICore "the recommended path for
      production". I'd stay with LiteRT-LM as planned: it's open, pinned, and
      under our control.
13. **Version drift on the model side too.** Gallery fetches its model list from
    GitHub at runtime, and the Hugging Face repo keeps adding builds. Pin the
    exact file by commit and SHA-256, as Gallery pins the commit.
