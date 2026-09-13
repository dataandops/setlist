# Produce a downloadable APK

The public download is a signed, non-debuggable release APK. It requires no
Play Store account or subscription and works on Android phones and tablets.

```sh
scripts/android.sh release
```

This builds/lints the release variant, then signs and verifies it with Android's
`apksigner`. Outputs are in the ignored `dist/` directory: the APK, SHA-256 checksum,
and forwardable install guide. The signing helper currently names version 0.1.0;
update it alongside `versionCode`/`versionName` for the next release.

The signing identity is generated once in `.tools/signing/` and reused. That
directory and all keystores are ignored by Git and restricted to the local user.
Privately back up the key and password together for future releases: Android
requires the same signing identity to update an installed app without uninstalling.
Never upload these files as release assets or commit them to the repository.

Release checklist: build/lint, instrumentation suite on phone/tablet, signature
verification, release-APK emulator smoke test, review screenshots, then publish
the APK and checksum to the GitHub release matching the source commit. Confirm
the public download bytes match the local checksum. A source tag can point to a
reviewed feature-branch commit while its PR remains open; it does not merge the PR.
