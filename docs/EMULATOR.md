# Run without an Android device

This workspace has JDK 17, a project-local Android SDK, and two Android 15
Apple Silicon virtual devices: `Setlist_Tablet` and `Setlist_Phone`.
The SDK, downloaded system images, Gradle caches and device data are ignored by Git.

```sh
scripts/android.sh emulator Setlist_Tablet
# In another terminal:
scripts/android.sh install emulator-5554
```

The app opens in the emulator. Choose **Try a sample set**, open the new set,
then **Start setlist**. It contains three original three-page charts.

To use your own PDFs, drag files from Finder onto the emulator. Android places
them in Downloads. In the app, create a setlist, choose **Import PDFs**, navigate
to Downloads, long-press one file, select others, then choose **Open** or **Select**. This grants
read access to those original files; the app does not keep permanent copies.

Alternatively, use Android Debug Bridge:

```sh
.tools/android-sdk/platform-tools/adb -s emulator-5554 push /path/to/score.pdf /sdcard/Download/
```

Open a song's options to rename it, attach an MP3, move it with accessible controls,
or locate a moved PDF. In concert mode, use **Next page**, **Previous**, or **Setlist**
to jump to another song. At a song boundary, the primary button becomes **Next song**.
The lower control also allows skipping the rest of a song. Arrow keys, Page Up/Down,
and Space work for keyboard/pedal devices that send those key events.

Audio starts only when you press Play. Pause and seek are available. Changing
songs, backgrounding the app, unplugging headphones, or leaving the reader stops
playback. Rotation preserves the current score/page and stops audio.

## Another development machine

Install Android Studio with JDK 17 and the Android SDK. Use Device Manager to create
an API 35 tablet/phone emulator (ARM64 on Apple Silicon; x86_64 on Intel/Linux).
Set `sdk.dir` in `local.properties` or export `ANDROID_HOME`. Run:

```sh
./gradlew assembleDebug lintDebug
./gradlew connectedDebugAndroidTest
```

For command-line-only setup, obtain Android SDK Command-line Tools from Android
Developers, accept the SDK licenses, and use `sdkmanager` to install:

```text
platform-tools
platforms;android-35
build-tools;34.0.0
emulator
system-images;android-35;google_apis;arm64-v8a
```

Use `avdmanager create avd -n Setlist_Tablet -k 'system-images;android-35;google_apis;arm64-v8a' -d pixel_tablet`.
The helper script expects project-local tooling unless you override its environment
variables. Plain Gradle commands also work with a standard Android Studio setup.

Debug builds install as **Setlist Preview** (`com.setlist.debug`), alongside the signed release. Swipe left/right on the score to advance/go back. In landscape, page controls move beside the score. In Saved songs, search name or metadata and choose a source setlist. Use **Edit metadata** in a song’s options for artist, key, BPM and notes.

From a score, tap **Setlist → Home** to return to all setlists. **Edit setlist** returns to the current running order; **Back to score** resumes viewing. Leaving for Home stops audio.

Saved-song results show 25 songs per page, with Previous/Next controls, a result range and a page count. Search and setlist-filter changes return to page one; rotation preserves the selected page.
