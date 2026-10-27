package de.veil.browser

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class BrowserApplication : Application() {
    val runtime: GeckoRuntime by lazy {
        val settings = GeckoRuntimeSettings.Builder()
            .allowInsecureConnections(GeckoRuntimeSettings.HTTPS_ONLY)
            .build()
        GeckoRuntime.create(this, settings)
    }
}
