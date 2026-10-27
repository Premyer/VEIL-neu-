# VEIL browser integration

The root [README](../README.md) documents features, build commands and current
verification limits. The former minimal app is retained under `prototype/`.

- `upstream.json` pins Mozilla's active Firefox repository, release revision and
  source archive checksum.
- `fetch_source.py` verifies and prepares that source outside this checkout.
- `veil.patch` changes application identity, defaults and existing resources.
- `overlay/` supplies the VEIL theme, logos and theme tests.
- `apply_overlay.py` applies these changes and refuses conflicting local edits.
- `build.sh` prepares native Gecko artifacts, builds Fenix and exports ARM64.
- `check.sh` runs build-helper tests, affected Android tests and Android lint.

The upstream MPL notices remain in the source and overlay. VEIL is an independent
application, not an official Mozilla product. External account and push services
require separate verification; a successful build does not establish availability.
