# Validation — 2026-09-13

`assembleDebug`, `lintDebug`, and `connectedDebugAndroidTest` pass. The complete
23-test suite passed on both Android 15 ARM64 emulators: Pixel Tablet and Pixel 7,
for 46 successful checks, run on `main` after the compact phone layout, phone-landscape
fixes and `com.dataandops.setlist` application ID. The earlier reader release also passed release build/lint and signed installation; this update has been verified as a debug preview.

## Automated coverage

- Horizontal swipes advance/reverse pages across song boundaries in both orientations; taps, short movements and vertical drags do not turn pages.
- Metadata search combines fields and source-setlist filters; empty search recovery works.
- Schema version 1 upgrades preserve song IDs, file links and ordering.

- SQLite references survive database reopening; deleting a set preserves source files.
- Reorder/removal and duplicate song entries keep stable identities and ordering.
- Invalid reorder requests roll back; invalid PDFs leave no broken entries.
- Relinking a PDF preserves the song and entry IDs.
- Real Android document-picker multi-selection imports two original PDFs and
  retains their persisted read permissions.
- Dragging the actual editor changes order and survives activity recreation.
- Page boundaries advance to the next song; Previous returns to the prior song;
  finishing/restarting a set works.
- Setlist jumping and activity recreation preserve the selected score/page.
- Actual portrait/landscape rotation preserves the page, supports continued page
  turns, and correctly advances to the next song on both emulator profiles.
- An actual MP3 fixture plays, pauses, and stops on song change.
- Missing PDFs display an error and allow skipping to a working score.
- Visible PDF requests supersede stale requests; lookahead is served from cache;
  the bitmap cache remains within its memory budget.

## Page performance

Measurements below are from the earlier reader baseline, with both emulators running.
Draw timing is from a visible page request to the ImageView's next `onDraw`, not
physical display latency. Generated vector-score fixtures contain repeated music
marks; no personal scores or scanned PDFs were supplied.

| Measurement | Tablet | Phone |
| --- | ---: | ---: |
| Cached page request → draw | 6–181 ms | 17–96 ms |
| Uncached page request → draw | 69–817 ms | 38–512 ms |
| Worst cold render in 12-page engine test | 18 ms | 43 ms |
| Repeated cached engine request | <1 ms | <1 ms |

All observed page draws were within the requested 1–2 second target. These results
do not guarantee the same latency for unusually complex/scanned PDFs, slow storage
providers or physical devices. The engine test requests a 1600 × 2000 viewport;
bitmap resolution is capped to reserve room for five cached pages. Engine timings
exclude view layout/drawing; the first two rows include drawing.

Raw extracted timings and test totals are in [validation-results.json](validation-results.json).
Full local JUnit reports and per-test logcat files are under
`app/build/outputs/androidTest-results/connected/debug/` (ignored by Git).

The renderer draws pages with PDFium, including annotations (see the 0.0.3 section
below). It uses one worker with foreground-request priority, at most two open
PDF descriptors, and an LRU bitmap budget of the smaller of 48 MiB or one-sixth of
the app heap. It prepares upcoming pages, the previous page and the next song's
first page without writing permanent media copies.

## Visual review and remaining acceptance work

The signed release was installed on both emulator profiles. Tablet portrait and
landscape reader screenshots, plus phone editor/reader screenshots, were captured
and visually inspected. The full PDF fits without cropping; page controls and
setlist navigation remain visible. See [the design system](DESIGN.md).

Release signing verifies with APK Signature Schemes v2 and v3. The public certificate
fingerprint is recorded in `signing-certificate.sha256`; private signing material
is excluded from Git. Install guide: [INSTALL.md](INSTALL.md).

Before relying on the app on stage, exercise your own PDFs on the intended tablet,
including large scans, offline/removable-storage availability, font scaling,
any physical pedal, and unusually large system text settings. Android 8–14 support is declared
and lint-checked but has not been exercised on those system images. Audio focus,
headphone removal and background-stop logic are implemented; their full real-device
acceptance checks remain. MP3 tests verify player state, not acoustic output.

The 8-second test MP3 is an original low-volume 440 Hz tone generated with FFmpeg:
`ffmpeg -f lavfi -i sine=frequency=440:duration=8 -af volume=0.05 -codec:a libmp3lame -b:a 64k test-tone.mp3`.
It is packaged only in the test APK.

GitHub CI builds the APK, compiles instrumentation tests and runs lint. Emulator
instrumentation was executed locally; it is not yet part of the hosted CI job.

Follow-up validation: the direct score-menu Home route passed on both emulators. Pagination and combined search/filter recovery tests also passed on both (six focused checks across the two runs), including 31-song page boundaries, page restoration after recreation and reset after query/filter changes. Debug build and lint pass.

## 0.0.3 — PDF markups (2026-11-22)

A user reported that markups made in an annotation app (typed chord names and
boxes around chords) were missing in 0.0.2. Cause: Android's
`android.graphics.pdf.PdfRenderer` draws page content only and never draws
annotations. `PdfEngine` now renders through PDFium (`io.legere:pdfiumandroid`
1.0.35) with annotation rendering on, for fitted pages and zoom detail
([#12](https://github.com/dataandops/setlist/pull/12)).

- New `PdfEngineTest.markupAnnotationsAreRendered` builds a PDF with a red `/Square`
  annotation and checks its pixels in the fitted render and a 2× zoom detail render.
  It fails with the old renderer.
- All `PdfEngineTest` and `LibraryTest` cases pass on the Pixel 7 (Android 15) emulator.
  Worst cold render in the 12-page engine test: 13–28 ms (budget 2 s).
- Debug and release build and lint pass. Release lint prints warnings about the
  library's newer Kotlin metadata but reports no issues.
- The signed release APK installed over 0.0.2 on the phone emulator kept the existing
  setlist. PDFium's native libraries loaded, and opening a song and turning pages worked.
- The public v0.0.3 download matches the local SHA-256, and the signing certificate
  is unchanged.
- Release APK size: 5.7 MB → 12.9 MB. Native libraries ship for `arm64-v8a`,
  `armeabi-v7a` and `x86_64`, compressed in the APK.

Not yet verified: the reporting user's own PDF in 0.0.3, and the tablet emulator.
On the headless phone emulator, `ConcertFlowTest`'s two rotation tests fail the
same way on unchanged `main` (the display doesn't rotate without a window).
`tapTogglesFullScreenAndKeepsPage` failed once in a full run, then passed twice
on its own.
