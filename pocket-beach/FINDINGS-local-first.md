# Sundial, local-first: findings before the build

21 November 2026, for spine:pocket-beach 1.11117871993 ("The super pocket
beach: local-first") and its nine parts (1.111178719931 to
1.111178719939), and the seed it shapes with, seed:sundial-shaping. This
is part 1, the read and the critique. **Nothing is built.** Nothing was
written to the beach. Matthew and Claude read this whole before parts 2 to
6 are built.

**What was read**

- The plan: 1.11117871993 and its nine parts, each whole. Its parent,
  1.1111787199, for how a Sundial build is done (a read of the code first,
  findings in the repo, then the build, the release and his phone). Beside
  them, for what they point to: 1.11117871991 and its decisions 1 and 9
  (the three releases; his own Claude in 0.12), and 1.11117871992 (the
  line in the sand).
- seed:sundial-shaping, whole (21,670 characters).
- The app on main at `4ab2acc` (Sundial 0.10.5, released 15 November):
  - the core: Storage.kt (BlockStorage, FileStorage, RoutedStorage,
    MemoryStorage, the file names), Shelf.kt, Asks.kt, Pocket.kt,
    Pages.kt, Beach.kt (loadBlock, the shape gate), Copies.kt, the
    parts of Hatch.kt, Acts.kt, Talks.kt and Walker.kt that the plan
    leans on;
  - the app: PocketApp.kt, MainActivity.kt, the manifest, the network
    config, App.kt, ShoreScreen.kt, BlockScreen.kt, WriteScreen.kt,
    ReceivedScreen.kt, Speech.kt, Jobs.kt;
  - the trial module (trial/build.gradle.kts, Trial.kt, Window.kt);
  - the device tests' Fresh.kt and the CI workflow's emulator.

References are `file:line` from `pocket-beach/`: `bsp/…/Storage.kt:273`
is `bsp/src/main/kotlin/com/phenomemental/pocketbeach/bsp/Storage.kt`,
`ui/…/ShoreScreen.kt:207` is the same under `app/…/ui/`, and
`app/…/PocketApp.kt:49` is under `app/src/main/kotlin/…/pocketbeach/`.

---

## The short version

1. **The base is good for this.** The core asks the phone for one thing,
   a BlockStorage. Every block is read afresh on every call
   (`bsp/…/Beach.kt:27`). Nearly every write the app makes goes through
   one guarded place, `Pages.carryOut` (`bsp/…/Pages.kt:329`); rename,
   remove, restore and the hatch's move are the exceptions (1c). The three
   editing acts the plan names are already there and guarded:
   - change a line, whose number never moves;
   - add a line beneath, where the bsp gives the number;
   - make a block.

   A block screen already opens on its first ring, goes deeper on a tap
   and shows the path above (`ui/…/BlockScreen.kt:47`, `:192`). The app's
   own blocks are already routed to a region of their own
   (`bsp/…/Storage.kt:153`).
2. **"FileStorage, unchanged, pointed at the folder" doesn't hold.**
   FileStorage was made for a folder nobody else touches. In a folder he
   can see and sync, four things in it go wrong:
   - **One oddly named file stops the app.** A file such as
     `50%off.json` makes the name decoder throw (`bsp/…/Storage.kt:273`).
     Every listing then fails, the hatch check at start among them
     (`app/…/PocketApp.kt:49`).
   - **One file that isn't readable JSON stops Your pocket and her
     turns.** `Pages.shore()` parses every block and catches nothing
     (`bsp/…/Pages.kt:87`), and it is called on every turn (Talks,
     Talking, Landing, HerWindow, Cook, Keeping, Asking). "Save a copy"
     fails whole the same way (`bsp/…/Copies.kt:27`).
   - **A restore renames the whole folder away and deletes it**
     (`bsp/…/Storage.kt:98-121`). That takes with it any file that isn't
     a block: notes, a sync tool's marker, files the plan says to leave
     as they are.
   - **Files he makes by hand vanish.** A file named on the laptop
     (`Notes.json`, `garden (1).json`) is listed, but the app looks for
     it under another file name (`%4Eotes.json`), finds nothing and
     drops it silently (`bsp/…/Storage.kt:254-276`).

   The proposal (2a below): leave FileStorage as it is for the app's
   private region and the tests. Give the visible folder a storage of its
   own, FolderStorage, which:
   - reads every file without stopping, and lists what it can't read;
   - restores file by file, and never moves the folder;
   - names files readably;
   - keeps its stamps and temporary files out of sight.
3. **"A write never lands over a newer change" is not true today.** A
   change to a line is planned again when he says yes, and that check
   only asks whether the line still lands at the same address
   (`bsp/…/Pages.kt:332`, `:339`; `bsp/…/Asks.kt:353`). If the laptop
   rewrote that same line in between, the phone overwrites it without a
   word. Each write needs to carry what the block held when he saw it
   (4c).
4. **His own Claude doesn't exist yet.** Part 6 names "Sundial's Claude
   option, the key kept encrypted in the keystore". The app has no Claude
   client, no keystore code, and api.anthropic.com is not in its network
   config. It was decided for 0.12 (1.111178719919) and never built. It
   is a piece of its own, two to three days (6c).
5. **The seed has three places that don't fit the app, and one that
   doesn't fit itself:**
   - **Sundial's checker is looser than the seed says.** The seed says a
     list "is refused by every beach and by Sundial's own checker" (seed
     1.1). The app's gate is the beach's, ported, and it lets lists,
     numbers and true/false through (`bsp/…/Beach.kt:412-415`). Shaping
     needs a stricter check of its own.
   - **A small mind is told to use branch 7 but isn't given it.** A
     small mind takes the nine opening lines and branches 4, 6 and 8
     (seed 5.3.2). Yet 6.2.1 tells it to judge every framing line
     against branch 7, and 5.1.1 asks it for flagged words (7.2.1).
   - **The place-9 and paragraph rules need settling before they become
     code:** 5.4.1 says "added beneath" without saying beneath what;
     "¶3-5" needs a rule for joining; 6.3 asks a small mind to write
     wrapped living blocks.
   - **The text to shape must fit her window.** "A small model can shape
     long texts" holds for what she writes back, not for what she reads.
     The whole text still has to fit her 8,192-token window beside the
     seed. About 2,500 words is the most Gemma can take on the phone.
6. **How long:** about 18 to 22 working days of build for parts 2 to 6,
   before his phone's days. Parts 2, 3 and 4 can't ship apart (the folder
   without the watching is the crash in point 2), so they make one
   release; 5 and 6 can each be a release after it. The table is in 4.

---

## 1. Every place the plan does not fit the code

### 1a. The visible folder (1.111178719932)

- **"FileStorage, unchanged, is pointed at it (regions/pocket and its
  stamps)."** Pointed at Documents/Sundial as it stands, FileStorage puts
  his blocks two folders down, in Documents/Sundial/regions/pocket/, and
  the stamps beside that folder, in regions/pocket.meta.json
  (`bsp/…/Storage.kt:59-65`). During a restore it also puts two more
  folders there for a while, pocket.incoming and pocket.outgoing. Opening
  Documents/Sundial, he would find one folder, regions, and his blocks
  two taps in. More important are the four failures in the short version.
  None of them can happen in the private folder, and all of them can
  happen in a visible one. So "unchanged" can't hold: either FileStorage
  changes, or the folder gets a class of its own. 2a proposes the second.
- **"Named as now."** Today a block's file name encodes every capital and
  every character outside a-z, 0-9 and -_.!~'() as %XX
  (`bsp/…/Storage.kt:254`). That was right for a folder only the app
  reads. In a folder he reads, his blocks are called
  `shell%3A%50henomemental.json`, `history%3A%50henomemental.json` and
  `%47arden.json`. A file he makes on the laptop in the natural way
  (`Garden.json`, `my notes.json`) doesn't come back to the same name, so
  the app can't read it (`:267`). The capitals were encoded so that
  "Garden" and "garden" could never share a file on a disk that ignores
  case. Making a new block already refuses a name that differs from
  another only in its capitals (`bsp/…/Pages.kt:316`), so the encoding
  isn't needed for that inside the app. Decision 2 in section 5.
- **"The app's own region stays private, only his blocks go into the
  folder."** This fits once he has hatched, and he has. Before a hatch,
  RoutedStorage is shut and every block, the app's own among them, lives
  in the pocket (`bsp/…/Storage.kt:153-160`). Offer the folder only once
  the phone is hatched.
- **Whether he is hatched would come to depend on the folder.**
  `Hatch.hatched` asks whether the four blocks of a shell stand in the
  pocket (`bsp/…/Hatch.kt:99`), and PocketApp asks it once, at start, to
  decide whether the private region is open (`app/…/PocketApp.kt:49`). If
  one of the four goes missing from the folder (removed on the laptop, or
  by a sync), the next start finds no hatch. The private region then
  stays shut, and her safety block, his heart and her ways, which live
  only there (`bsp/…/Hatch.kt:350-355`), are no longer read. Whether the
  private region is open must be decided from the private region itself,
  never from the folder.
- **Asked once, then what?** The plan says what happens when he says
  yes. It doesn't say what happens:
  - when he declines (proposal: his blocks stay where they are, and
    Settings offers the folder again);
  - when he later takes the permission back in Android's settings. This
    one matters: see 3a.
- **The whole app is made around one storage, made once.** `storage`,
  `pocket` and `pages` are each made the first time they are asked for,
  and never again (`app/…/PocketApp.kt:47-55`). Moving the root while the
  app is open needs one of two things:
  - a storage that hands every call to the one in use, and can be
    switched between calls (the bsp's lock is already where to do it);
  - or Sundial closing and opening again.

  The first is half a day and keeps her conversation open.
- **What else stays private.** These are kept beside the blocks in the
  app's own files, never in the folder, so his laptop won't see them:
  - today's moments not yet kept (`app/…/PocketApp.kt:93`);
  - the notes sent to his room (`:99`);
  - posts, and posts waiting to go (`:87-90`);
  - the copies saved before the hatch;
  - the last-saved time.

  That is right, but it should be said in Settings beside the folder's
  name.

### 1b. The move (1.111178719933)

- Copy, check, switch fits the code well:
  - Copies.make already writes a whole copy, and the hatch already keeps
    one in the app's files;
  - each copied file can be read back and compared byte for byte;
  - the switch can be the last step, one preference, so a move stopped
    part way leaves the private pocket in use and untouched.
- **What if the folder isn't empty?** The plan doesn't say. Documents/
  Sundial may already hold files: a second phone, a reinstall, a laptop
  that synced first, or a move tried before. Proposal: if any name in the
  folder is also a block of his, the move doesn't start, and the clashing
  names are listed (decision 3).
- **"The private copy stays until he removes it."** That needs an act to
  remove it, with its own warning ("Remove the copy kept inside the
  app"). It also needs a plain line saying which pocket is in use, so
  he never edits one believing it is the other. Nothing in the plan
  offers a way back to the private pocket. That is fine for this release,
  but should be decided rather than left out.
- **"Restoring a copy is tried once on the new folder before release."**
  Today's restore must not run on the folder at all (short version, point
  2). With FolderStorage it becomes file by file: write each block in the
  copy, remove the block files the copy doesn't hold, and leave every
  other file alone. A small journal in the hidden sidecar folder lets an
  interrupted restore finish at the next start. That gives up the
  whole-folder swap's all-or-nothing, so the journal is what keeps it
  safe.

### 1c. Watching the folder (1.111178719934)

- **"Noticed on return to the app and while it is open."** Nothing
  notices today. The screens refresh only when the app says a block moved
  (`ui/…/App.kt:104-112`, the version). The fit:
  - on return to the app, look at each file's size and time;
  - while the app is open, look again every few seconds;
  - when anything moved, refresh.

  Android's file watcher can add speed, but shouldn't be relied on: on
  shared storage it has been known to miss changes. Two screens hold what
  they read when they opened and must look again:
  - the Write screen reads its line once (`ui/…/WriteScreen.kt:34`);
  - her proposal cards hold a proposal planned earlier.
- **"A block is read fresh before any write."** Already true
  (`bsp/…/Beach.kt:27`, `bsp/…/Pages.kt:329-337`).
- **"A write never lands over a newer change from outside: it is held and
  shown to him."** Not true today, as in the short version, point 3:
  - **a changed line:** planned again, it still lands at the same
    address, so the check passes and the outside wording is overwritten;
  - **an added line:** caught only if the outside change took the slot
    it would have landed in;
  - **a whole-block write:** the one kind already guarded (it keeps
    `before`, `bsp/…/Pages.kt:335`);
  - **rename and remove:** they write to storage directly
    (`bsp/…/Pages.kt:272-302`), so remove deletes whatever stands, even a
    newer version from the laptop.

  Where a held write waits, and what he can do with it, isn't designed
  yet: see 4c.
- **"A file that is not a well-formed block is left as it is and listed
  as one the app can't read."** Not true today:
  - a file that isn't JSON stops the listings (short version, point 2);
  - a file that is JSON but not a block is listed and walked as if it
    were one;
  - a file whose name doesn't decode stops the app;
  - a file whose name doesn't come back to itself is dropped without a
    word;
  - a restore deletes all of these.

  The read gate it needs is cheap. But it must first be run over his own
  latest pocket copy: if any block of his fails it, the block would
  appear among the ones that can't be read. Blocks made before the gate,
  and healed ones, are the ones to look at.
- **Sync conflicts look like blocks.** Every sync tool, faced with the
  same block changed on both sides, keeps the second version under a new
  name that still ends in .json:
  - `garden.sync-conflict-20261121-120000-ABCDEFG.json`;
  - `garden (conflicted copy).json`;
  - `garden (1).json`.

  Today these vanish silently (point 2). The listing should name them for
  what they are: "a second version of garden, from your sync".
- **Stamps don't see outside changes.** "Touched" is written only by the
  app (`bsp/…/Beach.kt:36-38`). An edit on the laptop doesn't move a
  block up Your pocket, and the copy reminder doesn't count it
  (`bsp/…/Copies.kt:75-91`). Use the file's own time where it is newer.

### 1d. Your pocket, a block editor (1.111178719935)

- **Built from the screens as they stand: fits.** Every item the plan
  lists is on the Shore today (`ui/…/ShoreScreen.kt`):
  - the copy line, 88;
  - Ask the companion, 92;
  - Your blocks and Make a new block, 96-97;
  - Rename or remove a block, 113-117;
  - Teaching blocks, 119;
  - the app's own, 128-136.
- **"Each block opening in place to its first ring, a tap going deeper,
  the path shown above."** The block screen already does this. If "in
  place" means within Your pocket's list, opening under its row without
  going to another screen, that is new: about half a day. Decision 5.
- **Map: "joined where two share addresses."** Every block has a 1, a 2
  and a 3, so every block shares addresses with every other, and the
  joins would join everything. Joins "where one names another" work: a
  line that names another block, or points into it (`block:address`).
  Two narrower joins make sense too:
  - blocks that hold a line at the same full-width date (his clock and
    his days);
  - blocks shaped from the same original (their place 9.2).

  This needs deciding before it is drawn (decision 5).
- **The copy line's words stop being true.** It says "Your blocks are
  kept only on this phone" (`ui/…/ShoreScreen.kt:207`). That isn't so
  once the folder syncs, and the app can't know whether it does.
  - Proposal for the copy line: "Kept in Documents/Sundial on this
    phone."
  - The remove dialog says "It leaves this phone, every line of it"
    (`:294`). With a sync it leaves the laptop too.
- **"She may offer to read what lies beneath."** This is a new turn of
  hers on the one engine, with a line handed to her that is his to change
  (a RECIPE part, in the line in the sand's terms). It must not start
  while her conversation is mid-reply. Offered as a link under "Saved.",
  never run by itself.
- **Search.** Every line of every block of his, each with its address
  and the path above it. Parsing everything on each keystroke is too
  slow on shared storage. A small index, rebuilt only for files whose
  size or time moved, is the fit, and it serves the Map and the watching
  too.

### 1e. Sharing in, shaping, sharing out (1.111178719936)

- **"Anything shared into Sundial lands whole and unchanged as one line
  in an inbox block."** Today a share is held in memory, one at a time
  (`app/…/PocketApp.kt:81`). It is read only as a block. Anything else is
  shown as "can't be read as a pscale block" and let go
  (`ui/…/ReceivedScreen.kt:33-43`, `:127`). The app answers only text and
  JSON sent one at a time (manifest, lines 53-62). The fit:
  - widen it to any text, and to text files sent as files;
  - several items at once;
  - each item an entry in the inbox block, its words at the line and
    the app it came from beneath. The beach stamps an entry's arrival
    only when it is more than a line (`bsp/…/Beach.kt:328-335`), so give
    it a field.

  Two things to decide or check:
  - **When it lands.** Does a share land at once, or wait for a tap? His
    share is itself a tap, and a share is his act, not hers, so the
    proposal is that it lands at once and the screen says so (decision
    4).
  - **Pointers survive the inbox wrapping. Checked, fits.** The inbox
    wraps at nine, and an address written before a wrap still reaches
    the same entry after it, because shorter addresses are padded at the
    floor (`bsp/…/Walker.kt:84`). So 9.2's pointer to the original stays
    true.
- **Images, PDFs and documents.** They hold no text the app can read. The
  plan says "anything"; this release can take text and say plainly what
  it can't take.
- **"Gemma on the phone or Claude through his own key (Sundial's Claude
  option…)".** Gemma fits: one engine, a conversation of its own, and a
  job that follows her settings. Claude doesn't exist yet (short version,
  point 4). Building it also changes three promises that are written
  down:
  - the manifest's note that the internet is "for two things only"
    (manifest, line 3);
  - the network config's "No other host is trusted";
  - the spine's own 1.1: "Nothing leaves the phone but the model's one
    download and the messages the holder taps to post."

  1.1 is Matthew's and Claude's to change on the beach. Shared-out walks
  and files leave the phone by his tap too, through Android's share menu,
  as "Share this block" does already.
- **"The mind shapes and the app places."** This fits the app's way: code
  decides what code can. Four rules must be fixed before it is code. They
  are in 1f.
- **"Out: any line shared as a walk… any block exported as a file (its
  JSON, or readable text)."** What exists is:
  - "Share this block", which sends the raw JSON as text
    (`ui/…/BlockScreen.kt:164-177`);
  - "Save this block as a file", which saves the JSON (`:178`).

  A walk shared out, and a readable-text export, are new and small. The
  plan calls it "Gemini Notebook"; NotebookLM takes text and Markdown
  files, but may not take .json as a source. So the readable export
  should be .md or .txt. Check this on his phone.
- **"Sundial ships a copy of the seed beside its procedures."** This fits
  `assets/procedures/`, read only. The app reads nothing from the web
  (1.1111787199), so the copy changes only with a release.

### 1f. The seed against the app (seed:sundial-shaping)

- **"No lists… a node that carries any other key is refused by every
  beach and by Sundial's own checker" (seed 1.1).** Named keys are
  refused, but lists, numbers, true/false and null are let through
  (`bsp/…/Beach.kt:397-424`). That is the beach's gate, ported, so the
  phone agrees with the beach. A shaped block needs a stricter check:
  - every node an object, holding `_` and digits only;
  - every value a string or such an object;
  - a floor;
  - nothing at 9;
  - every leaf that is the author's either a ¶ reference or a framing
    line, never a mixture.
- **Small mind and branch 7.** 5.3.2 gives a small mind the nine opening
  lines and branches 4, 6 and 8. 6.2.1 tells it to "judge each one against
  the seed in branch 7", and 5.1.1 asks it for "any words flagged with
  their reasons (7.2.1)". Branch 7 is about 5,000 characters, roughly
  1,200 tokens, so it costs real room. Two ways:
  - give the small mind branch 7 too;
  - or change 6.2.1 and 5.1.1 so a small mind uses plain words and flags
    nothing, and the framing lines go through his language engine
    afterwards.

  The seed is the beach's; the change is Matthew's and Claude's
  (decision 6).
- **How much she can read.** Her window is 8,192 tokens by default
  (`app/…/model/Jobs.kt:49`). The small cut of the seed is about 7,600
  characters, some 1,900 tokens. With the instruction around it and room
  for her answer (a root, up to eight parts, the parts beneath them and
  the ¶ numbers: 700 to 1,000 tokens), about 4,500 tokens are left for
  the text: some 2,500 words to be safe, near 2,000 if branch 7 goes
  too. A longer text needs one of three things:
  - shaping in pieces, which costs the whole view the seed asks for;
  - Claude, which takes the seed whole;
  - a plain line saying it is too long for her.
- **¶ placement (6.2):**
  - **What a paragraph is:** split at blank lines, or, where the text has
    none (chats, poems, pasted pages), at line breaks.
  - **Only whole leaves:** a ¶ reference is a whole leaf, `¶3` or `¶3-5`,
    never inside a sentence; anything else is a framing line.
  - **A range:** `¶3-5` is one line holding the three paragraphs with a
    blank line between, so each paragraph still has one place.
  - **Order:** the text's own order, checked.
- **Place 9 (5.4.1):**
  - **9 needs its own opening line.** A node with children needs one, in
    plain words and with no beach terms (seed 8.3).
  - **Where a reshaping goes.** "If a block is shaped again, the new line
    is added beneath." Beneath 9.1 (as 9.1.1), or beside it as the next
    of 9? Proposal: the next free slot of 9, after 9.2, so 9 reads as a
    list in time.
  - **Which date names the seed.** "This seed by its name and the date on
    its own place 9." The seed's 9 holds two dates: made on 21 November,
    adjusted at 12:22 the same day (9.3). Proposal: the last adjusted
    date, as the seed's 9 states it.
- **Living blocks (6.3).** A journal of more than nine entries has to be
  wrapped, with summaries at the zero places. Writing that wrapping as
  JSON is the hardest thing in the seed for a small model, and it is
  exactly what the app's append already does. Proposal:
  - the mind returns the entries in order (¶ numbers with their dates)
    and the summaries it owes;
  - the app appends the entries one by one, the beach's own way;
  - the app places the summaries where its append says they are owed.
- **What she returns (5.1.1).** One JSON object, then plain sentences.
  The Ask reader takes the first `{` to the last `}`
  (`bsp/…/Asks.kt:127-133`), which would swallow a brace in the
  sentences after it. Shaping needs a reader that takes the first whole
  object.
- **Constrained output.** The Ask's answers have a JSON schema the engine
  can hold her to (`bsp/…/Asks.kt:99-106`). A pscale block is a tree
  that nests without end, and whether the engine holds a model to a
  schema that refers to itself is unchecked. Without that hold, she
  writes freely, the strict check reads it, and she is asked once more
  if it fails, as Ask does.

### 1g. Speech.kt and the trial module

- **Speech.kt: no misfit.** Nothing in parts 2 to 6 changes it.
  - The rules stand: one recogniser at most, let go on leaving or going
    to the background, and Gboard's hold on the phone's speech waited
    out (`ui/…/Speech.kt:86-114`).
  - Any new field (search, a held write's own words) can take Speak as
    the Write screen does.
  - Part 8's voice (later) has to live within the same one-recogniser
    rule.
- **The trial module.** It works on MemoryStorage only (Trial.kt:198,
  417, 454; Window.kt:200) and never touches FileStorage, so parts 2 to
  4 don't touch it.
  - **It is where shaping should be tried first.** Run E4B on the laptop
    over a handful of his real texts, the way the line in the sand's
    RECIPE parts were run (`trial/sand-0.10.3.md`), before any of part 6
    is built on the phone.
  - **Part 7 misnames it.** Part 7 (later) speaks of "the trial module's
    core (PocketBsp and FileStorage)". Those live in the core, the bsp
    module. The trial module brings LiteRT-LM's native engine with it
    (`trial/build.gradle.kts:25`), which a laptop MCP server doesn't
    need. So the laptop door wants a small module of its own on the core
    alone.
  - **The laptop door should use FolderStorage.** It should read the
    synced folder through the same FolderStorage, not FileStorage, which
    would look for regions/pocket inside it.

### 1h. Her window as the pocket grows (Asks.kt, Talks.kt)

- **The Write job is shown every block he has.** Its window lists each
  of his blocks with its opening line, with no limit (`bsp/…/Talks.kt:168-
  173`, used at `ui/…/Asking.kt:596`). The old Ask window does the same
  (`bsp/…/Asks.kt:531-549`). Her talk's window is nine places and
  doesn't grow (`bsp/…/Hatch.kt:143-154`).
- **Shaped blocks fill it quickly.** Their roots are full sentences by
  the seed's rule. Fifty of them would take 2,000 to 3,000 of the Write
  job's 8,192 tokens. Name the inbox and shaped blocks without their
  lines, or show the most recently touched and say how many more.
- **A proposal copies the whole pocket into memory.** Planning one copies
  every block (`bsp/…/Asks.kt:277-280`, `bsp/…/Acts.kt:224-227`). That
  is fine for tens of blocks and slow for hundreds. It doesn't parse
  them, so a file that can't be read does no harm there.

---

## 2. What fits, and the proposals in short

### 2a. FolderStorage, for the visible folder

A second BlockStorage in the core, beside FileStorage, so the private
region, the laptop trial and the 317 core tests keep the class they were
proven with:

- **Where things go.**
  - His blocks directly in Documents/Sundial, one file each, flat.
  - Its own things in one hidden folder, Documents/Sundial/.sundial/:
    the stamps, the temporary files writes go through (so a sync tool
    never sees a half-written block), and the restore journal.
- **Names readable** (decision 2). Only what the common file systems
  refuse is encoded: `/ \ : * ? " < > |`, control characters, a trailing
  dot or space, and the names Windows keeps for devices. So `garden`
  is garden.json, `Garden` is Garden.json, and `history:Phenomemental`
  is history%3APhenomemental.json. Any file name maps to the block of
  that name, and back.
- **Reads that never stop.**
  - Every `.json` file is listed.
  - A name that doesn't decode is taken as it stands.
  - A file that isn't a well-formed block is set aside with why. Your
    pocket lists these under "Files Sundial can't read", and a sync's
    second versions are named for what they are.
  - Files that aren't `.json` are never touched.
- **A restore file by file,** with the journal (1b).
- **A fingerprint for each file** (size, time and a hash), kept for what
  the app last read or wrote. This is the watching's and the guard's
  ground (4c).
- **A parse kept per fingerprint,** so the listings stop parsing every
  block several times a turn. On shared storage every read goes through
  Android's file layer and is slower than in the app's own files.

### 2b. A guard that knows what he saw

Every plan carries the stored text of each block it touches, as it stood
when planned. `Pages.carryOut` writes only while the block still stands
so. For a proposal of several steps, each block is compared once, before
its first step lands.

When the block has moved:

- nothing is written;
- the write is kept, privately, as held;
- a notice shows his line, the block as it now stands, and two acts:
  "Plan it again on this version" and "Let it go".

Rename and remove get the same check. A change the app makes itself
between the plan and his yes (his history at a close) also counts as
moved. That is rare, and safe.

### 2c. The permission, watched

At every start and every return, and only while his blocks are in the
folder, Sundial checks that it still has all-files access. Without it,
Sundial shows one plain page and writes nothing, and it never shows an
empty pocket (3a).

---

## 3. Risks

### 3a. Losing the permission looks like losing his blocks

- **The symptom.** If he takes all-files access back in Android's
  settings, the folder can't be listed. Today's code then sees no blocks
  at all: a folder it can't list reads as an empty one
  (`bsp/…/Storage.kt:82`).
- **It is likely worse than empty.** Without the permission, Android lets
  an app see only the files it made itself. So the blocks last written
  by a sync tool would vanish and the rest would stay: a pocket half
  there. This is likely, not checked; it needs trying on his phone.
- **The danger.** A pocket that looks empty, or half empty, invites
  "Make a new block", a hatch, or a restore over the top.
- **The guard** is 2c: checked at every start and return, it shows one
  plain page and writes nothing.

### 3b. Sync

- **No sync app yet.** The plan says "synced from the laptop", but
  Android's own Google Drive app doesn't keep a folder in step both ways.
  A sync app is needed, and the plan doesn't name one. The sync app
  decides what the conflict files are called and whether its temporary
  files show, so choose it before part 4's tests are written. Syncthing
  (its Android fork), FolderSync and Resilio are the usual choices; which
  are kept up in November 2026 is unchecked.
- **A restore's folder swap would stop a sync.** Syncthing, for one,
  stops syncing a folder when its marker file disappears, and today's
  restore deletes every file that isn't a block (FolderStorage removes
  this).
- **Both sides edited.** The same block edited on the phone and the
  laptop between syncs gives two files. Sundial can name the second;
  joining them is his. The two can't be merged line by line safely: a
  line's number is its meaning.

### 3c. Privacy, said plainly

- **Wider sight, both ways.** All-files access lets Sundial see every
  file in shared storage, not only Documents/Sundial. The app will only
  touch its folder, but the "plain line on why" should say what the
  permission reaches. And the folder can be seen in turn by:
  - him in Files;
  - any app he gives all-files access;
  - any app he picks the folder for (a sync app);
  - a computer over USB while the phone is unlocked.

  Today his blocks sit in the app's private files, where no other app
  can see them (`app/…/PocketApp.kt:35-38`). Part 9 says "his knowledge
  stays with him"; that stays true only as far as the sync he chooses.
- **Personal shares.** A share-in may be personal, and seed 8.2 says it
  "stays where it was given and goes nowhere else". In the folder, it
  goes wherever the folder syncs. Say so where the inbox is explained.
- **His history moves into the folder.** Today Matthew can't edit it,
  because no file of his is reachable. In the folder he can edit it on
  the laptop like any block.

### 3d. Tests on his phone

- **The device tests' reset deletes the blocks.** It deletes
  `filesDir/regions` (`Fresh.kt:38`, `clear()`). Taught the folder, a reset
  run on his phone would delete his blocks. The device tests need a
  folder of their own (Documents/Sundial-test, say). The reset should
  refuse any folder that isn't a test's.
- **The emulator needs the permission.** CI's emulator must be given
  all-files access before the tests run (one appops line in the
  workflow).
- **CI runs an older Android than the app targets.** The emulator is
  API 36 (`.github/workflows/pocket-beach.yml:139`) and the app targets
  37 (`app/build.gradle.kts:17`). How all-files access behaves on the
  newer Android is for his Pixel to show first.

### 3e. Speed and size

- **Shared storage is slower.** Every listing reads every block through
  Android's file layer, and her turns list the pocket several times
  (1h). The parse kept per fingerprint (2a) is what keeps this from
  showing.
- **A share can be large.** One share can be up to a megabyte
  (`ui/…/ReceivedScreen.kt:43`), and it lands as one line. A few large
  ones make the inbox the heaviest block, read on every turn. Show a
  line's first words in lists; read it whole only when it is opened.
- **Shaping on the phone takes time.** It means reading thousands of
  tokens and writing about a thousand. On the CPU she read 1,202 tokens
  in 31.3 s (STATUS, 15 November), so a long text there would take
  minutes. On the GPU it is unmeasured; it belongs to part 8's measure,
  but one timing should come from the laptop trial before part 6 is
  built.

### 3f. Release order

- **0.11 is taken.** In the earlier plan 0.11 is the shells, the sea,
  LISTEN and the turn, and 0.12 is the films and his own Claude
  (1.111178719911). This release needs a number, and the Claude half of
  part 6 takes a piece of 0.12 (decision 8).
- **2, 3 and 4 ship together.** Shipped alone, part 2 puts his blocks
  where the four failures of point 2 can happen, with nothing yet to
  catch them.

---

## 4. How long each part takes

Working days of build, each ending with the core and device tests green
on CI. His phone's days come on top.

| Part | Days | What it holds |
|---|---|---|
| 2. The visible folder | 2 | The permission and its plain line; the declined and taken-back cases (2c); FolderStorage (2a) with core tests for every file the folder may hold; the switchable storage; Settings naming the folder; CI's emulator given the permission; the device tests' own folder. |
| 3. The move | 1½ | The whole copy first; copy, read back and compare each block and the stamps; the switch last; a folder already holding files; "Remove the copy kept inside the app"; the file-by-file restore and its journal, tried on the folder. |
| 4. Watching | 3 | Refresh on return and every few seconds while open; the read gate, run first over his latest pocket copy; "Files Sundial can't read" and the sync's second versions; stamps from the file's time; the guard (2b) through every write, rename and remove; held writes, kept and shown; the Write screen and her cards looking again. |
| 5. The block editor | 5 to 6½ | The folder line and the new copy-line words, ½. Search with its index, 1. Opening in place in Your pocket, ½ (if wanted). The Map, 2 to 3, once its joins are decided. Her offer after a parent line changes, 1 to 1½, with its handed line. |
| 6. In, shaping, out | 6 to 8½ | In: the inbox block, any text and text files, several at once, ½ to 1. Out: a walk shared, a block as readable text, ½. Shaping with Gemma, 3 to 4: the seed shipped and cut by code, the paragraphs numbered, the strict check (1f), the ¶ placer, place 9, the tree shown, his yes, a laptop trial first on his texts. His own Claude, 2 to 3: the key in the keystore, the third way out added to the network config, the manifest's note, About and the voice card's words, the request with the seed whole, and a stand-in for the tests, as the post has, since no test may reach Anthropic. |
| **Parts 2 to 6** | **18 to 22** | Before releases and his phone's days. |

Of these, only his phone can confirm:

- the permission on his Android, and the folder seen in Files;
- the sync app's real conflict names;
- the move with his real pocket;
- the time a shaping takes on the GPU;
- how NotebookLM takes a shared walk or file.

---

## 5. Decisions before the build

1. **Where his blocks stand.**
   - **Proposed:** directly in Documents/Sundial, through FolderStorage
     (2a).
   - **The plan as written:** FileStorage unchanged, which gives
     Documents/Sundial/regions/pocket/ and the four failures in point 2.
2. **File names.**
   - **Proposed:** readable, with only what file systems refuse encoded
     (2a). Then a file he makes on the laptop is a block on the phone.
   - **"Named as now":** `%47arden.json`, and his own files not read.
3. **A folder already holding files at the move.**
   - **Proposed:** the move doesn't start while any name clashes, and
     the clashes are listed.
4. **A share-in lands at once, or waits for a tap.**
   - **Proposed:** at once; his share is his tap.
5. **The Map's joins, and "in place".**
   - **The joins:** what "where two share addresses" means. Proposed:
     links by name or by `block:address`, the same full-width date, and
     the same original.
   - **"In place":** whether a block opens within Your pocket's list, or
     on its own screen as now.
6. **The seed's small mind and branch 7.**
   - Give it branch 7, about 1,200 tokens more;
   - or change 6.2.1 and 5.1.1 on the beach so a small mind flags
     nothing.

   Settle also:
   - the ¶ rules, the joining of ranges, and paragraphs at line breaks
     (1f);
   - place 9's opening line, where a reshaping goes, and which date names
     the seed;
   - living blocks appended by the app.
7. **His own Claude in this release, or Gemma alone first.** Claude is
   2 to 3 days. It changes the spine's 1.1, the manifest's note and
   About's words, and it is the key 0.12's voice would share.
8. **The release's number and order.**
   - **Proposed:** 2, 3 and 4 as one release.
   - **Then:** 6 (in, out and Gemma) before 5's Map, if shaping is what
     he wants sooner.
   - **The number:** 0.11 as planned was the shells and the sea.

Once these are decided, part 2 is the first piece to build.
