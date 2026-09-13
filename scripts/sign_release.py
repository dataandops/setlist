#!/usr/bin/env python3
"""Sign a built release APK using a persistent, private local signing key."""
import hashlib
import os
import pathlib
import re
import secrets
import shutil
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[1]
SIGNING = ROOT / ".tools/signing"
KEY = SIGNING / "setlist-release.p12"
PASSWORD = SIGNING / "password"
SOURCE = ROOT / "app/build/outputs/apk/release/app-release-unsigned.apk"
OUTPUT = ROOT / "dist/Setlist-0.0.1.apk"
JAVA = pathlib.Path(os.environ["JAVA_HOME"])
SDK = pathlib.Path(os.environ.get("ANDROID_HOME", ROOT / ".tools/android-sdk"))
APKSIGNER = SDK / "build-tools/34.0.0/apksigner"
CERTIFICATE = ROOT / "docs/signing-certificate.sha256"

if not SOURCE.is_file():
    raise SystemExit("Build the release first: ./gradlew assembleRelease")
SIGNING.mkdir(parents=True, exist_ok=True, mode=0o700)
SIGNING.chmod(0o700)
if KEY.exists() != PASSWORD.exists():
    raise SystemExit("Signing key/password pair is incomplete. Restore the original pair; do not replace the signing identity.")
if not KEY.exists():
    if CERTIFICATE.exists():
        raise SystemExit("Restore the original private signing key and password from backup before making an update.")
    with PASSWORD.open("x") as output:
        output.write(secrets.token_urlsafe(36))
    PASSWORD.chmod(0o600)
    environment = os.environ.copy()
    environment["SETLIST_SIGNING_PASSWORD"] = PASSWORD.read_text()
    subprocess.run([
        str(JAVA / "bin/keytool"), "-genkeypair", "-keystore", str(KEY),
        "-storetype", "PKCS12", "-alias", "setlist", "-keyalg", "RSA",
        "-keysize", "3072", "-validity", "10000",
        "-dname", "CN=Setlist, OU=Android, O=Setlist",
        "-storepass:env", "SETLIST_SIGNING_PASSWORD",
    ], env=environment, check=True)
    KEY.chmod(0o600)
OUTPUT.parent.mkdir(exist_ok=True)
subprocess.run([
    str(APKSIGNER), "sign", "--ks", str(KEY), "--ks-key-alias", "setlist",
    "--ks-pass", "file:" + str(PASSWORD), "--out", str(OUTPUT), str(SOURCE),
], check=True)
verification = subprocess.check_output([str(APKSIGNER), "verify", "--verbose", "--print-certs", str(OUTPUT)], text=True)
fingerprint = re.search(r"Signer #1 certificate SHA-256 digest: ([a-f0-9]+)", verification)
if not fingerprint or (CERTIFICATE.exists() and fingerprint[1] != CERTIFICATE.read_text().strip()):
    raise SystemExit("Unexpected signing identity. Do not distribute this APK.")
print(verification)
digest = hashlib.sha256(OUTPUT.read_bytes()).hexdigest()
(OUTPUT.parent / "SHA256SUMS.txt").write_text(f"{digest}  {OUTPUT.name}\n")
shutil.copyfile(ROOT / "docs/INSTALL.md", OUTPUT.parent / "INSTALL.md")
print(f"Signed APK: {OUTPUT}")
print(f"SHA-256: {digest}")
