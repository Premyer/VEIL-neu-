#!/usr/bin/env python3
"""Align and sign an exported VEIL release APK with a production key.

The keystore and passwords are read from the environment, never from arguments:
VEIL_KEYSTORE, VEIL_KEYSTORE_PASSWORD, VEIL_KEY_ALIAS, VEIL_KEY_PASSWORD
(the last defaults to the keystore password).
"""

import argparse
import hashlib
import os
from pathlib import Path
import re
import subprocess

REQUIRED = ("VEIL_KEYSTORE", "VEIL_KEYSTORE_PASSWORD", "VEIL_KEY_ALIAS")


def find_tool(name, environ):
    home = environ.get("ANDROID_HOME")
    if not home:
        raise SystemExit("Set ANDROID_HOME to your Android SDK.")
    tools = sorted((Path(home) / "build-tools").glob(f"*/{name}"), reverse=True)
    if not tools:
        raise SystemExit(f"{name} not found in {home}/build-tools.")
    return str(tools[0])


def sign(unsigned, destination, environ=None):
    environ = dict(os.environ if environ is None else environ)
    missing = [name for name in REQUIRED if not environ.get(name)]
    if missing:
        raise SystemExit("Missing signing configuration: " + ", ".join(missing))
    keystore = Path(environ["VEIL_KEYSTORE"])
    if not keystore.is_file():
        raise SystemExit(f"Keystore not found: {keystore}")
    if not unsigned.name.endswith("-release-unsigned.apk"):
        raise SystemExit("Expected an exported *-release-unsigned.apk.")
    environ.setdefault("VEIL_KEY_PASSWORD", environ["VEIL_KEYSTORE_PASSWORD"])
    zipalign = find_tool("zipalign", environ)
    apksigner = find_tool("apksigner", environ)

    destination.mkdir(parents=True, exist_ok=True)
    name = unsigned.name.replace("-release-unsigned.apk", "-release.apk")
    output = destination / name
    aligned = destination / (name + ".aligned")
    try:
        subprocess.run([zipalign, "-P", "16", "-f", "4", str(unsigned), str(aligned)], check=True)
        subprocess.run([zipalign, "-c", "-P", "16", "4", str(aligned)], check=True)
        subprocess.run(
            [apksigner, "sign", "--ks", str(keystore), "--ks-key-alias", environ["VEIL_KEY_ALIAS"],
             "--ks-pass", "env:VEIL_KEYSTORE_PASSWORD", "--key-pass", "env:VEIL_KEY_PASSWORD",
             "--out", str(output), str(aligned)],
            check=True, env=environ,
        )
    finally:
        aligned.unlink(missing_ok=True)
    verify = subprocess.run(
        [apksigner, "verify", "--verbose", "--print-certs", str(output)],
        check=True, capture_output=True, text=True,
    ).stdout
    if "Android Debug" in verify:
        output.unlink(missing_ok=True)
        raise SystemExit("Refusing to publish an APK signed with an Android debug certificate.")
    match = re.search(r"certificate SHA-256 digest: ([0-9a-f]{64})", verify)
    if not match:
        output.unlink(missing_ok=True)
        raise SystemExit("Could not read the signing certificate digest.")
    with output.open("rb") as stream:
        checksum = hashlib.file_digest(stream, "sha256").hexdigest()
    output.with_suffix(".apk.sha256").write_text(f"{checksum}  {output.name}\n")
    output.with_suffix(".apk.cert-sha256").write_text(match.group(1) + "\n")
    print(output)
    print(f"SHA256: {checksum}")
    print(f"Signing certificate SHA256: {match.group(1)}")
    return output


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("unsigned", type=Path, help="VEIL-*-release-unsigned.apk from export_apk.py")
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    sign(args.unsigned.resolve(), args.destination.resolve())
