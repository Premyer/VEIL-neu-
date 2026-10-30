#!/usr/bin/env python3
"""Apply VEIL changes to the pinned Firefox checkout without discarding edits."""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parent


def sha(data):
    return hashlib.sha256(data).hexdigest()


def apply(source):
    source = source.resolve()
    pin = json.loads((ROOT / "upstream.json").read_text())
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=source, text=True).strip()
    if revision != pin["revision"]:
        raise SystemExit("The checkout does not match the pinned Firefox revision.")
    patch = ROOT / "veil.patch"
    forward = subprocess.run(["git", "apply", "--check", str(patch)], cwd=source, capture_output=True)
    if forward.returncode:
        reverse = subprocess.run(["git", "apply", "--reverse", "--check", str(patch)], cwd=source, capture_output=True)
        if reverse.returncode:
            raise SystemExit("Patch conflicts with the checkout or a previously applied patch version. Preserve local edits and inspect browser/veil.patch before retrying.")

    marker = source / ".veil-overlay.json"
    previous = json.loads(marker.read_text()) if marker.exists() else {}
    files = {}
    for path in sorted((ROOT / "overlay").rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(ROOT / "overlay")
        target = source / relative
        if not target.resolve().is_relative_to(source):
            raise SystemExit(f"Overlay destination leaves the source directory: {relative}")
        content = path.read_bytes()
        if target.exists() and target.read_bytes() != content:
            baseline = subprocess.run(
                ["git", "show", f"HEAD:{relative.as_posix()}"], cwd=source, capture_output=True,
            )
            is_pristine = baseline.returncode == 0 and baseline.stdout == target.read_bytes()
            if not is_pristine and previous.get("files", {}).get(str(relative)) != sha(target.read_bytes()):
                raise SystemExit(f"Refusing to replace local edits: {relative}")
        files[str(relative)] = sha(content)

    if not forward.returncode:
        subprocess.run(["git", "apply", str(patch)], cwd=source, check=True)
    for relative in files:
        target = source / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes((ROOT / "overlay" / relative).read_bytes())
    marker.write_text(json.dumps({"revision": revision, "patch": sha(patch.read_bytes()), "files": files}, indent=2) + "\n")
    print(f"Applied VEIL overlay to {source}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    args = parser.parse_args()
    apply(args.source.resolve())
