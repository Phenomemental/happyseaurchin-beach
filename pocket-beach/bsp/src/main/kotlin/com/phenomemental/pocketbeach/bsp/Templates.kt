package com.phenomemental.pocketbeach.bsp

/*
 * The beach's own four templates, as pscale-beach seeds/templates holds them at
 * the pinned commit (4770a49), byte for byte: the hatch fills {{HANDLE}},
 * {{BEACH_URL}} and {{TIMESTAMP}} as the beach's init/seed-beach.js does.
 * TemplatesTest holds them to the files whenever the pinned upstream is
 * fetched (conformance/fetch-upstream.sh).
 */
object Templates {
    const val PASSPORT = """{
  "_": "{{HANDLE}} — replace this underscore with a description in zeroth-voice (no I/you/it; the agent describes itself from inside). The underscore says who this agent is, what their work is, where they are situated. Look at sunstone:8 for voicing discipline.",
  "1": {
    "_": "Offers — what {{HANDLE}} provides at {{BEACH_URL}}. Replace with substantive sentences naming capabilities, knowledge, services, availability windows. Not labels."
  },
  "2": {
    "_": "Needs — what {{HANDLE}} is reaching for. Replace with what kinds of collaborators, information, or opportunities would land here as useful. Equal weight with offers."
  }
}
"""

    const val SHELL = """{
  "_": "{{HANDLE}}'s shell at {{BEACH_URL}}. Personal operational state — faces, watched beaches, manifest of named blocks, pool subscriptions. Locked with {{HANDLE}}'s passphrase.",
  "3": {
    "_": "Manifest of {{HANDLE}}'s named blocks at {{BEACH_URL}} — the address a stranger's LLM reads to find the rest of the shell without walking the whole beach index. The four below are the standard shell (pscale://shell-genome); add entries here as new blocks are authored, and keep it current, because a manifest that lags is worse than none — it is trusted.",
    "1": "passport:{{HANDLE}}",
    "2": "shell:{{HANDLE}}",
    "3": "history:{{HANDLE}} — automatic, one entry per session; append, never a computed slot",
    "4": "stash:{{HANDLE}} — deliberate, kept by choice; same law as history"
  },
  "9": {
    "_": "Shell metadata.",
    "1": "v1 — initialised at {{TIMESTAMP}}"
  }
}
"""

    const val HISTORY = """{
  "_": "{{HANDLE}}'s history at {{BEACH_URL}} — the automatic lossless record of what was done: one entry per session or engagement, written by the acting instance and never by hand. Initialised at {{TIMESTAMP}}. Deliberate notes go to stash:{{HANDLE}}. Entries land by APPEND — never compute a slot: the substrate allocates the next zero-free number (1..9, 11..19, …, 99, 111…) and supernests at each all-nines boundary, wrapping the past under this underscore where its addresses keep reading zero-padded, the count continuing and never restarting. Nothing is ever cleared. Zero-carrying numbers are summary slots, never entries: N0 voices container N with a +0 summary of the previous completed nine — 10 over 01-09, 20 over 11-19, 100 over 10-90 — falling due as the next span opens, and paid by writing N0 as the whole container, its digit children resent beside the new underscore (a bare string at N0 replaces the container). To deepen an existing entry, write beneath its digit."
}
"""

    const val STASH = """{
  "_": "{{HANDLE}}'s stash at {{BEACH_URL}} — deliberate notes kept by choice: ideas, principles, and runbooks worth refinding. Initialised at {{TIMESTAMP}}. The intentional sibling of history:{{HANDLE}}, which logs automatically; the two are one convention differing only in who decides an entry happens. Entries land by APPEND — never compute a slot: the substrate allocates the next zero-free number (1..9, 11..19, …, 99, 111…) and supernests at each all-nines boundary, wrapping the past under this underscore where its addresses keep reading zero-padded, the count continuing and never restarting. Nothing is ever cleared. Zero-carrying numbers are summary slots, never entries: N0 voices container N with a +0 summary of the previous completed nine — 10 over 01-09, 20 over 11-19, 100 over 10-90 — falling due as the next span opens, and paid by writing N0 as the whole container, its digit children resent beside the new underscore (a bare string at N0 replaces the container). To deepen an existing entry, write beneath its digit."
}
"""
}
