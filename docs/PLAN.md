# Implementation stages

1. PR: offline foundation — native Android project, local song/setlist data,
   PDF import, list editing, drag order, sample scores, persistence tests.
2. PR (stacked on foundation): concert reader — background PDF rendering,
   bounded page cache and lookahead, song transitions, return/jump navigation,
   audio playback, emulator workflow and performance measurements.

Confirmed: entirely offline; select multiple PDFs and derive song titles from
filenames; optional audio plays in the app. Each entry has its own ID and points
to a song with a stable ID, title, persisted PDF URI and optional audio URI.

Confirmed: open existing device files in place through persisted Android document
permissions; no permanent copies. Moved or deleted files can be relinked. Assumptions: one PDF per song; repeat songs allowed in a set; page
navigation supports buttons, left/right PDF swipes, and keyboard/pedal arrow events.

Performance target: page change within 1–2 seconds. Measure cache hits and cold
renders in the emulator, disclose that complex PDFs and real devices need their
own validation. Prepare adjacent pages and the next song's first page.

PR #1 (foundation) and PR #2 (concert reader) are merged. The next PR contains the
Material 3 dark-stage system, custom adaptive icon, empty states, responsive reader
controls, editable artist/key/BPM/notes, saved-song search and setlist filtering,
and bidirectional PDF swipe navigation. No PRs are automatically merged.
