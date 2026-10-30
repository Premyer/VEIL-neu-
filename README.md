# VEIL for Android

VEIL uses the Firefox for Android (Fenix) application,
with a separate identity and a teal/charcoal interface. The source is pinned to
Firefox **159.0**, revision `9a2dc53c369c7ae5172018b35988e1d2ffeff402`.
The former minimal GeckoView application is retained under `prototype/`.

## Browser and appearance

Fenix supplies tabs, private browsing, history, bookmarks, downloads, password
management, website permissions, extensions, reader mode, account/Sync interfaces,
search settings and the other Android browser screens. This is the Android
application, not the desktop Firefox interface or desktop extension ecosystem.

VEIL adds its own name, logo, wordmark, light/dark/OLED palettes and an amber
private-browsing palette. Dark appearance and the bottom address bar are the
initial choices; users can change them. HTTPS-only starts enabled in all tabs,
with Fenix's normal settings and exception handling. Android backup remains
disabled, including device-protected storage; telemetry/crash-reporting build
flags are disabled.

The full upstream source includes optional features and libraries absent from
the old prototype, including upstream AI-related components. The prototype's
old “no AI SDK” description therefore does not apply to this application.

## Build

Requirements: Python **3.12+**, Git, JDK **17**, Android SDK **37.2**, Build Tools
**37.0.0**, and enough disk/memory for the Firefox source and Android Components.
Gradle **9.8.1** is provided by the pinned upstream wrapper. Set `JAVA_HOME` and
`ANDROID_HOME` before building.

```sh
# First preparation; Mozilla's bootstrap installs its required tools.
bash browser/build.sh --setup

# Subsequent builds reuse the prepared toolchain and source cache.
bash browser/build.sh

# Build-tool tests, affected Android unit tests and Android lint.
bash browser/check.sh
```

`VEIL_FIREFOX_DIR` selects the source/build directory. Its default is outside the
checkout: `$RUNTIME_ROOT/repos/firefox`, or `$HOME/.cache/veil/repos/firefox`.
`VEIL_OUTPUT_DIR` selects the APK destination; the default is `build/outputs/apk`.
Logs are written to the source cache's `artifacts/` directory. If `veil.patch`
changes, preserve local edits and use a fresh `VEIL_FIREFOX_DIR` when the old
patch conflicts; the helper deliberately refuses to overwrite that state.

The build currently targets **ARM64 devices, Android 8.0 or newer**, and exports
`VEIL-0.2.0-arm64-debug.apk` with a SHA-256 checksum. Debug signing is for testing;
it is not a production release-signing setup. The APK exporter checks the package
identity, selected ABI and presence of the native browser libraries.

Keep the same signing key for updates. A fresh CI runner generates a different
debug key; its APK will not update a locally signed installation. The delivered
0.2.0 APK uses the existing prototype debug key. Keys are not
committed to this repository.

## Release signing

`VEIL_VARIANT=release bash browser/build.sh` builds an **unsigned** release APK
(`VEIL-0.2.0-arm64-release-unsigned.apk`). `browser/sign_apk.py` then runs
`zipalign` (16 KB alignment) and `apksigner`, verifies the result, refuses debug
certificates and writes `.sha256` and `.cert-sha256` files. Keystore and
passwords come only from environment variables, never from arguments:

```sh
keytool -genkeypair -v -keystore veil-release.jks -alias veil \
  -keyalg RSA -keysize 4096 -validity 10000
export VEIL_KEYSTORE=$PWD/veil-release.jks VEIL_KEYSTORE_PASSWORD=... VEIL_KEY_ALIAS=veil
python3 browser/sign_apk.py build/outputs/apk/VEIL-0.2.0-arm64-release-unsigned.apk build/outputs/apk/signed
```

`.github/workflows/release.yml` does this for tags `v0.2.0` (the tag must match the
app version) and manual runs. Set the repository secrets `VEIL_KEYSTORE_BASE64`
(`base64 -w0 veil-release.jks`), `VEIL_KEYSTORE_PASSWORD`, `VEIL_KEY_ALIAS` and,
if different, `VEIL_KEY_PASSWORD`. A tag creates a **draft** GitHub release for
manual review. Back up the keystore: losing it makes updates impossible, and the
release key differs from the debug key, so debug installations must be uninstalled
first (app data is removed).

## External services and migration limits

- WebPush and push-triggered “Send tab” require VEIL's own Firebase configuration
  and compatible Mozilla AutoPush service setup. No Mozilla Firebase credentials
  are included. Without that configuration, upstream disables the push service.
- Account/Sync code is retained, but an authenticated account flow has not been
  verified on a device. Inheriting source is not proof of service availability.
- The prototype's URL-only tab preferences are not imported as Fenix sessions.
  Migration of existing sessions has not been implemented or verified.
- Feature availability also depends on upstream defaults, provider support and
  Android permissions. No claim of verified parity across every feature is made.

## Validation status

The native Gecko and ARM64 APK builds, build-tool tests, 133 Android
theme/settings tests and APK signature/package checks passed locally. Android
lint reports no new issues; four existing warnings are filtered by the unchanged
upstream baseline. Beta/release manifest identities were validated. The release build path
(`assembleRelease`, R8, signing, `release.yml`) has only been covered by the
build-tool unit tests (eight in total); it has not been run end to end, and neither
has remote CI.
Device execution remains unverified: this sandbox has no KVM acceleration, and
the earlier software-emulator attempt did not become ready.

## Source and notices

`browser/upstream.json` pins the source archive and checksum. `browser/veil.patch`
and `browser/overlay/` contain the changes applied to it. Upstream MPL notices
are preserved; full third-party notices are in the pinned source tree and its
`toolkit/content/license.html`. VEIL is not an official Mozilla application.

Upstream: https://github.com/mozilla-firefox/firefox/tree/9a2dc53c369c7ae5172018b35988e1d2ffeff402/mobile/android/fenix
