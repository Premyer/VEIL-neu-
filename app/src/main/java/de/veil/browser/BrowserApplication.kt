package de.veil.browser

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime

class BrowserApplication : Application() {
    val runtime: GeckoRuntime by lazy { GeckoRuntime.create(this) }
}
