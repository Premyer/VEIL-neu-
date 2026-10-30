#!/usr/bin/env python3
"""Fetch the exact Mozilla source used by VEIL (Python 3.12 or newer)."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import tempfile
import urllib.request


def run(*args, cwd):
    return subprocess.check_output(args, cwd=cwd, text=True).strip()


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def fetch(destination, pin=None):
    if pin is None:
        pin = json.loads(Path(__file__).with_name("upstream.json").read_text())
    marker = destination / ".veil-source-revision"
    if destination.exists():
        if not marker.is_file() or marker.read_text().strip() != pin["revision"]:
            raise SystemExit(f"Refusing to overwrite an unrecognized directory: {destination}")
        if run("git", "rev-parse", "HEAD", cwd=destination) != pin["revision"]:
            raise SystemExit("The source checkout revision has changed.")
        print(destination)
        return

    destination.parent.mkdir(parents=True, exist_ok=True)
    archive = destination.parent / f"firefox-{pin['revision']}.tar.gz"
    if not archive.exists() or digest(archive) != pin["sha256"]:
        with tempfile.NamedTemporaryFile(dir=destination.parent, delete=False) as output:
            download = Path(output.name)
            try:
                with urllib.request.urlopen(pin["archive"], timeout=120) as response:
                    shutil.copyfileobj(response, output, length=1024 * 1024)
                output.flush()
                if digest(download) != pin["sha256"]:
                    raise SystemExit("Firefox source archive checksum mismatch.")
                download.replace(archive)
            finally:
                download.unlink(missing_ok=True)

    with tempfile.TemporaryDirectory(prefix="veil-source-", dir=destination.parent) as temporary:
        staging = Path(temporary) / "firefox"
        run(
            "git", "clone", "--depth=1", "--filter=blob:none", "--no-checkout",
            "--branch", pin["tag"], pin["repository"], str(staging),
            cwd=destination.parent,
        )
        if run("git", "rev-parse", "HEAD", cwd=staging) != pin["revision"]:
            raise SystemExit("Firefox release tag no longer matches the pinned revision.")
        prefix = f"firefox-{pin['revision']}/"
        with tarfile.open(archive, "r:gz") as source:
            for member in source:
                if member.name == prefix.rstrip("/"):
                    continue
                if not member.name.startswith(prefix):
                    raise SystemExit("Unexpected source archive layout.")
                member.name = member.name[len(prefix):]
                source.extract(member, staging, filter="data")
        run("git", "reset", "--mixed", "HEAD", cwd=staging)
        paths = subprocess.check_output(["git", "ls-files", "-z"], cwd=staging).split(b"\0")
        regular = []
        for raw in paths:
            if not raw:
                continue
            path = staging / os.fsdecode(raw)
            if path.is_symlink():
                subprocess.run(
                    ["git", "hash-object", "-w", "--stdin"], cwd=staging,
                    input=os.fsencode(os.readlink(path)), stdout=subprocess.DEVNULL, check=True,
                )
            elif path.is_file():
                regular.append(json.dumps(os.fsdecode(raw), ensure_ascii=False))
        subprocess.run(
            ["git", "hash-object", "-w", "--no-filters", "--stdin-paths"], cwd=staging,
            input="\n".join(regular) + "\n", text=True,
            stdout=subprocess.DEVNULL, check=True,
        )
        # GitHub expands export-subst metadata in this vendored file.
        run("git", "restore", "--", "testing/web-platform/tests/tools/third_party/attrs/.git_archival.txt", cwd=staging)
        run("git", "update-index", "--refresh", cwd=staging)
        with (staging / ".git/info/exclude").open("a") as excludes:
            excludes.write("\n.veil-source-revision\nartifacts/\n")
        (staging / marker.name).write_text(pin["revision"] + "\n")
        staging.rename(destination)
    print(destination)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", type=Path, help="New build directory outside the VEIL checkout")
    args = parser.parse_args()
    fetch(args.destination.resolve())
