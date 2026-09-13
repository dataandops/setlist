#!/usr/bin/env bash
set -euo pipefail
SETLIST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SETLIST_ROOT"

# Prefer a full JDK; some Macs report a JRE from java_home by default.
if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_HOME/bin/javac" ]]; then
  for candidate in /Library/Java/JavaVirtualMachines/*/Contents/Home; do
    if [[ -x "$candidate/bin/javac" ]] && "$candidate/bin/javac" -version 2>&1 | grep -q '^javac 17'; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi
export ANDROID_HOME="${ANDROID_HOME:-$SETLIST_ROOT/.tools/android-sdk}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$SETLIST_ROOT/.tools/android-user}"
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$ANDROID_USER_HOME/avd}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$SETLIST_ROOT/.tools/gradle-home}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

case "${1:-help}" in
  build) ./gradlew assembleDebug lintDebug ;;
  release) ./gradlew assembleRelease lintRelease; python3 scripts/sign_release.py ;;
  test) ./gradlew connectedDebugAndroidTest ;;
  emulator) exec emulator -avd "${2:-Setlist_Tablet}" -no-snapshot -gpu swiftshader_indirect ;;
  install)
    ./gradlew assembleDebug
    device_args=()
    if [[ -n "${2:-}" ]]; then device_args=(-s "$2"); fi
    adb "${device_args[@]}" install -r app/build/outputs/apk/debug/app-debug.apk
    adb "${device_args[@]}" shell am start -n com.dataandops.setlist.debug/com.setlist.MainActivity
    ;;
  *)
    cat <<'HELP'
Usage: scripts/android.sh build | release | test | emulator [AVD name] | install [device serial]

Local AVDs: Setlist_Tablet, Setlist_Phone. Requires the Android SDK and JDK 17.
Use `adb devices` to find emulator serials when both devices are running.
See docs/EMULATOR.md for first-time SDK setup and adding your own PDFs.
HELP
    ;;
esac
