#!/usr/bin/env python3
"""Export only the ARM64 APK corresponding to the configured native target."""

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile


def export(source, destination):
    directory = source / "objdir-frontend/gradle/build/mobile/android/fenix/app/outputs/apk/debug"
    metadata = json.loads((directory / "output-metadata.json").read_text())
    if metadata["applicationId"] != "de.veil.browser":
        raise SystemExit("Refusing to export an APK with an unexpected application id.")
    candidates = [
        item for item in metadata["elements"]
        if any(f.get("filterType") == "ABI" and f.get("value") == "arm64-v8a" for f in item["filters"])
    ]
    if len(candidates) != 1:
        raise SystemExit("Expected exactly one ARM64 APK.")
    apk = (directory / candidates[0]["outputFile"]).resolve()
    if not apk.is_relative_to(directory.resolve()):
        raise SystemExit("APK metadata points outside its output directory.")
    with zipfile.ZipFile(apk) as package:
        for required in ("lib/arm64-v8a/libxul.so", "lib/arm64-v8a/libmozglue.so"):
            if required not in package.namelist():
                raise SystemExit(f"Missing native browser library: {required}")
    destination.mkdir(parents=True, exist_ok=True)
    output = destination / "VEIL-0.2.0-arm64-debug.apk"
    shutil.copy2(apk, output)
    with output.open("rb") as stream:
        checksum = hashlib.file_digest(stream, "sha256").hexdigest()
    output.with_suffix(".apk.sha256").write_text(f"{checksum}  {output.name}\n")
    print(output)
    print(f"SHA256: {checksum}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    export(args.source.resolve(), args.destination.resolve())
