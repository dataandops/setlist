#!/usr/bin/env python3
"""Capture the app's sample flow on a running emulator (no third-party packages)."""
import argparse
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("serial")
parser.add_argument("prefix", choices=["tablet", "phone"])
parser.add_argument("--package", default="com.setlist")
args = parser.parse_args()
ADB = [str(ROOT / ".tools/android-sdk/platform-tools/adb"), "-s", args.serial]


def adb(*command):
    return subprocess.check_output(ADB + list(command))


def hierarchy():
    adb("shell", "uiautomator", "dump", "/sdcard/setlist-review.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/setlist-review.xml"))


def find(predicate, timeout=12):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        for node in hierarchy().iter("node"):
            if predicate(node.attrib):
                return node.attrib
        time.sleep(0.2)
    raise RuntimeError("Expected app control not found")


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node["bounds"]))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def text(value):
    return find(lambda n: n.get("text", "").casefold() == value.casefold())


def capture(name):
    path = ROOT / "docs/screenshots" / f"{args.prefix}-{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(adb("exec-out", "screencap", "-p"))
    print(path.relative_to(ROOT), flush=True)


adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
adb("shell", "settings", "put", "system", "user_rotation", "0")
adb("shell", "am", "force-stop", args.package)
adb("shell", "am", "start", "-n", args.package + "/com.setlist.MainActivity")
text("Setlist")
cards = [n.attrib for n in hierarchy().iter("node") if "sample set" in n.get("content-desc", "")]
if not cards:
    tap(text("Try a sample set"))
    find(lambda n: "Sample set ready" in n.get("text", ""))
    tap(text("OK"))
capture("home")
tap(find(lambda n: n.get("content-desc", "").startswith("Open setlist Friday night")))
text("Start setlist")
capture("editor")
tap(text("Start setlist"))
find(lambda n: n.get("content-desc", "").endswith(", page 1 of 3"))
capture("reader")
if args.prefix == "tablet":
    adb("shell", "settings", "put", "system", "user_rotation", "1")
    deadline = time.monotonic() + 12
    while time.monotonic() < deadline:
        if hierarchy().get("rotation") == "1":
            break
        time.sleep(0.2)
    find(lambda n: n.get("content-desc", "").endswith(", page 1 of 3"))
    capture("reader-portrait")
