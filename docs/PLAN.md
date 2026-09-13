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
navigation is explicit buttons (keyboard/pedal arrow support can accompany it).

Performance target: page change within 1–2 seconds. Measure cache hits and cold
renders in the emulator, disclose that complex PDFs and real devices need their
own validation. Prepare adjacent pages and the next song's first page.

The foundation is PR #1. The second branch, `feat/concert-reader`, builds on it.
Review/merge the foundation first, then retarget the reader PR to main (and rebase
if the foundation was squash-merged). Neither PR is automatically merged.
