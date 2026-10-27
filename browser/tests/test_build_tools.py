import contextlib
import hashlib
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import apply_overlay
import export_apk
import fetch_source


class BuildToolsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)

    def git(self, directory, *args):
        return subprocess.check_output(["git", *args], cwd=directory, text=True, stderr=subprocess.DEVNULL).strip()

    def repository(self):
        source = self.root / "upstream"
        source.mkdir()
        self.git(source, "init", "-q")
        self.git(source, "config", "commit.gpgsign", "false")
        self.git(source, "config", "tag.gpgsign", "false")
        self.git(source, "config", "user.name", "Premyer")
        self.git(source, "config", "user.email", "126298965+Premyer@users.noreply.github.com")
        (source / "README").write_text("upstream\n")
        (source / "default-theme").write_text("upstream theme")
        archival = source / "testing/web-platform/tests/tools/third_party/attrs/.git_archival.txt"
        archival.parent.mkdir(parents=True)
        archival.write_text("revision metadata\n")
        (source / "link").symlink_to("README")
        self.git(source, "add", ".")
        self.git(source, "commit", "-qm", "Synthetic build fixture\n\nCo-authored-by: coderabbitai[bot] <136622811+coderabbitai[bot]@users.noreply.github.com>")
        revision = self.git(source, "rev-parse", "HEAD")
        self.git(source, "tag", "fixture")
        return source, revision

    def test_fetch_checks_archive_and_reuses_pinned_checkout(self):
        source, revision = self.repository()
        archive = self.root / "source.tar.gz"
        self.git(source, "archive", "--format=tar.gz", f"--prefix=firefox-{revision}/", f"--output={archive}", "HEAD")
        pin = {"repository": source.as_uri(), "tag": "fixture", "revision": revision,
               "archive": archive.as_uri(), "sha256": fetch_source.digest(archive)}
        destination = self.root / "checkout"
        with contextlib.redirect_stdout(io.StringIO()):
            fetch_source.fetch(destination, pin)
            fetch_source.fetch(destination, pin)
        self.assertEqual("upstream\n", (destination / "README").read_text())
        self.assertTrue((destination / "link").is_symlink())
        self.assertEqual("", self.git(destination, "status", "--porcelain"))

    def test_fetch_rejects_invalid_checksum_and_preserves_existing_directory(self):
        archive = self.root / "invalid.tar.gz"
        archive.write_bytes(b"invalid archive")
        pin = {"archive": archive.as_uri(), "sha256": "0" * 64, "revision": "0" * 40}
        destination = self.root / "checkout"
        with self.assertRaisesRegex(SystemExit, "checksum mismatch"):
            fetch_source.fetch(destination, pin)
        self.assertFalse(destination.exists())
        destination.mkdir()
        (destination / "user-file").write_text("keep")
        with self.assertRaisesRegex(SystemExit, "unrecognized directory"):
            fetch_source.fetch(destination, pin)
        self.assertEqual("keep", (destination / "user-file").read_text())

    def test_overlay_is_repeatable_and_preserves_user_edits(self):
        source, revision = self.repository()
        overlay = self.root / "branding"
        (overlay / "overlay").mkdir(parents=True)
        (overlay / "upstream.json").write_text(json.dumps({"revision": revision}))
        (overlay / "veil.patch").write_text("--- a/README\n+++ b/README\n@@ -1 +1 @@\n-upstream\n+VEIL\n")
        (overlay / "overlay/theme").write_text("teal")
        (overlay / "overlay/default-theme").write_text("VEIL theme")
        with patch.object(apply_overlay, "ROOT", overlay), contextlib.redirect_stdout(io.StringIO()):
            apply_overlay.apply(source)
            alias = self.root / "source-link"
            alias.symlink_to(source, target_is_directory=True)
            apply_overlay.apply(alias)
            (source / "theme").write_text("user's theme")
            with self.assertRaisesRegex(SystemExit, "local edits"):
                apply_overlay.apply(source)
        self.assertEqual("user's theme", (source / "theme").read_text())
        self.assertEqual("VEIL\n", (source / "README").read_text())
        self.assertEqual("VEIL theme", (source / "default-theme").read_text())

    def apk_fixture(self, native=True):
        directory = self.root / "objdir-frontend/gradle/build/mobile/android/fenix/app/outputs/apk/debug"
        directory.mkdir(parents=True)
        apk = directory / "arm64.apk"
        with zipfile.ZipFile(apk, "w") as package:
            if native:
                package.writestr("lib/arm64-v8a/libxul.so", b"synthetic test fixture")
                package.writestr("lib/arm64-v8a/libmozglue.so", b"synthetic test fixture")
        metadata = {"applicationId": "de.veil.browser", "elements": [
            {"filters": [{"filterType": "ABI", "value": "x86_64"}], "outputFile": "do-not-export.apk"},
            {"filters": [{"filterType": "ABI", "value": "arm64-v8a"}], "outputFile": apk.name},
        ]}
        (directory / "output-metadata.json").write_text(json.dumps(metadata))
        return apk

    def test_export_selects_arm64_and_checksums_exact_bytes(self):
        apk = self.apk_fixture()
        output = self.root / "output"
        with contextlib.redirect_stdout(io.StringIO()):
            export_apk.export(self.root, output)
        result = output / "VEIL-0.2.0-arm64-debug.apk"
        self.assertEqual(apk.read_bytes(), result.read_bytes())
        self.assertIn(hashlib.sha256(apk.read_bytes()).hexdigest(), result.with_suffix(".apk.sha256").read_text())

    def test_export_rejects_apk_without_native_engine(self):
        self.apk_fixture(native=False)
        with self.assertRaisesRegex(SystemExit, "Missing native browser library"):
            export_apk.export(self.root, self.root / "output")
        self.assertFalse((self.root / "output").exists())


if __name__ == "__main__":
    unittest.main()
