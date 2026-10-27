# VEIL

Standalone Android browser prototype using GeckoView 159.0.20261022150525.

Features: URL entry and DuckDuckGo search, mobile user agent, multiple tabs, tab switching and closing, back/forward/reload menu. Tabs share one runtime and browser profile. Tabs are NOT private or isolated. Activity recreation restores URLs, not full navigation history or forms. Background tabs are marked inactive; this is not a guarantee that all background execution stops.

This is NOT Firefox/Fenix and does not provide Firefox feature parity. Downloads, file upload, website permission dialogs, password manager, bookmarks, extensions, Sync and private browsing are not implemented. No certificate-error bypass is implemented. HTTP cleartext is disabled. No AI SDK or custom analytics is included; GeckoView defaults require a separate privacy review before production.

Build: JDK 17, Android SDK 36, Gradle 8.11.1; run `gradle :app:assembleDebug`. No Gradle wrapper is included. GitHub Actions installs Gradle and uploads the debug APK under Actions > workflow run > Artifacts. The ZIP artifact contains the APK. Builds must be checked for success; the initial source has not yet been compiled or device-tested.

The debug APK is for testing, not a production-signed release. CI debug signing keys may change between runners, requiring uninstall before updating (which removes app data). Never use this prototype as a security-reviewed browser for sensitive accounts.
