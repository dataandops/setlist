# Setlist

A native Android app for a keyboard player's concert setlists. Offline, with a
dark-stage design system: ink surfaces, warm amber controls, and readable scores.

## Current foundation

- Create, rename and delete setlists; add, remove and reuse songs.
- Select multiple PDFs from device storage. Filenames become editable song names.
- Open files **in place** using persistent Android document permissions. No
  permanent copies of imported PDFs or audio, no broad storage permissions.
- Drag songs to reorder, or use Move up / Move down in song options.
- Stable song and setlist-entry IDs, stored in SQLite.
- Attach audio, inspect file references, and relink moved PDFs.
- Generate an original sample set to explore the app without supplying files.

The next implementation PR adds the concert PDF reader and audio controls.

## Build

Requires JDK 17 and an Android SDK with platform 35. Open this directory in Android
Studio, or set `sdk.dir` in the ignored `local.properties` file, then run:

```sh
./gradlew assembleDebug lintDebug
./gradlew connectedDebugAndroidTest
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Android 8.0+ (API 26).
The test suite requires a running Android emulator. SDK/tool downloads are not
checked into Git. The Gradle wrapper and dependency versions are pinned.

## Design and implementation

- [Design system](docs/DESIGN.md)
- [Implementation stages](docs/PLAN.md)
- Shared palette: `app/src/main/res/values/colors.xml`
- Shared dimensions: `app/src/main/res/values/dimens.xml`
- Native components and type scale: `app/src/main/java/com/setlist/Ui.java`

The app has no Internet permission and no cloud backup. Original files must stay
available at their selected locations. Local document providers with seekable
file descriptors are supported; cloud-only/streaming documents should first be
saved to the device. Unprotected PDFs only. Removing a setlist keeps saved songs
available for reuse and never deletes source files.
