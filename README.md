# VEIL

Standalone Android browser prototype using GeckoView 159.0.20261022150525.

Features: URL entry and DuckDuckGo search, mobile user agent, multiple tabs, tab switching and closing, back/forward/reload menu. Tabs share one runtime and browser profile. Tabs are NOT private or isolated. Activity recreation restores URLs, not full navigation history or forms. Background tabs are marked inactive; this is not a guarantee that all background execution stops.

This is NOT Firefox/Fenix and does not provide Firefox feature parity. Downloads, file upload, website permission dialogs, password manager, bookmarks, extensions, Sync and private browsing are not implemented. No certificate-error bypass is implemented. GeckoView runs in HTTPS-only mode; HTTP addresses are upgraded to HTTPS and insecure fallback is not offered. Load failures show an error page without a certificate-error bypass. No AI SDK or custom analytics is included; GeckoView defaults require a separate privacy review before production.

Build: JDK 17, Android SDK 37 (SDK package `platforms;android-37.0`), Build Tools 36.0.0, Gradle 9.3.1 (Android Gradle plugin 9.1.1 with built-in Kotlin support and Kotlin 2.4.20); run `gradle :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`. No Gradle wrapper is included. GitHub Actions installs Gradle and uploads the debug APK under Actions > workflow run > Artifacts. The ZIP artifact contains the APK. The debug APK build, Android lint and URL/search unit tests have been verified locally. Lint retains the older-target-API warning because targetSdk remains 35; increasing it requires device compatibility testing. Device testing remains outstanding: the sandbox has no KVM acceleration and a bounded software-emulator attempt did not reach readiness.

The debug APK is for testing, not a production-signed release. CI debug signing keys may change between runners, requiring uninstall before updating (which removes app data). Never use this prototype as a security-reviewed browser for sensitive accounts.
