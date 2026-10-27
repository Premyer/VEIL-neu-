package de.veil.browser

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.mozilla.geckoview.*

class MainActivity : Activity() {
    private data class Tab(
        val session: GeckoSession,
        var url: String,
        var back: Boolean = false,
        var forward: Boolean = false
    )
    private var backCallback: OnBackInvokedCallback? = null
    private val tabs = mutableListOf<Tab>()
    private var selected = -1
    private lateinit var browser: GeckoView
    private lateinit var address: EditText
    private lateinit var tabsButton: Button
    private val active: Tab? get() = tabs.getOrNull(selected)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val padding = insets.getInsets(WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            insets
        }
        val toolbar = LinearLayout(this)
        address = EditText(this).apply {
            setSingleLine(true)
            setHint(R.string.address_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO) {
                    active?.session?.loadUri(AddressResolver.resolve(text.toString()))
                    clearFocus()
                    val keyboard = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    keyboard.hideSoftInputFromWindow(windowToken, 0)
                    true
                } else false
            }
        }
        toolbar.addView(address, LinearLayout.LayoutParams(0, -2, 1f))
        tabsButton = Button(this).apply { setOnClickListener { showTabs() } }
        toolbar.addView(tabsButton)
        toolbar.addView(Button(this).apply {
            setText(R.string.menu)
            setOnClickListener { showMenu(it) }
        })
        root.addView(toolbar)
        browser = GeckoView(this)
        root.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()
        val restored = state?.getStringArrayList("urls")
        if (restored.isNullOrEmpty()) newTab("https://www.mozilla.org")
        else {
            restored.forEach { newTab(it) }
            selectTab(state.getInt("selected", 0).coerceIn(tabs.indices))
        }
    }

    private fun newTab(url: String = "https://www.mozilla.org") {
        val settings = GeckoSessionSettings.Builder()
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE).build()
        val session = GeckoSession(settings)
        val tab = Tab(session, url)
        session.contentDelegate = object : GeckoSession.ContentDelegate {}
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?, perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) {
                tab.url = url ?: "about:blank"
                if (active === tab) address.setText(tab.url)
            }
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                tab.back = canGoBack
                if (active === tab) updateBackCallback()
            }
            override fun onLoadError(session: GeckoSession, uri: String?, error: WebRequestError): GeckoResult<String> {
                val html = getString(R.string.load_error_html)
                return GeckoResult.fromValue("data:text/html;charset=utf-8," + android.net.Uri.encode(html))
            }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) { tab.forward = canGoForward }
        }
        session.open((application as BrowserApplication).runtime)
        tabs.add(tab)
        selectTab(tabs.lastIndex)
        session.loadUri(url)
    }

    private fun selectTab(index: Int) {
        if (index !in tabs.indices || index == selected) return
        active?.session?.setActive(false)
        browser.releaseSession()
        selected = index
        browser.setSession(tabs[index].session)
        tabs[index].session.setActive(true)
        address.setText(tabs[index].url)
        tabsButton.text = getString(R.string.tab_count, tabs.size)
        updateBackCallback()
    }

    private fun closeCurrentTab() {
        if (selected !in tabs.indices) return
        val index = selected
        browser.releaseSession()
        tabs.removeAt(index).session.close()
        selected = -1
        if (tabs.isEmpty()) newTab() else selectTab(index.coerceAtMost(tabs.lastIndex))
    }

    private fun showTabs() {
        AlertDialog.Builder(this).setTitle(R.string.open_tabs)
            .setItems(tabs.mapIndexed { i, tab -> "${i + 1}: ${tab.url}" }.toTypedArray()) { _, index -> selectTab(index) }
            .setPositiveButton(R.string.new_tab) { _, _ -> newTab() }
            .setNegativeButton(R.string.dismiss, null).show()
    }

    private fun showMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.back).isEnabled = active?.back == true
            menu.add(0, 2, 1, R.string.forward).isEnabled = active?.forward == true
            menu.add(0, 3, 2, R.string.reload)
            menu.add(0, 4, 3, R.string.new_tab)
            menu.add(0, 5, 4, R.string.close_tab)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> active?.session?.goBack()
                    2 -> active?.session?.goForward()
                    3 -> active?.session?.reload()
                    4 -> newTab()
                    5 -> closeCurrentTab()
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            show()
        }
    }

    private fun updateBackCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        backCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
        backCallback = if (active?.back == true) {
            OnBackInvokedCallback { active?.session?.goBack() }.also {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, it)
            }
        } else null
    }

    @Suppress("DEPRECATION") // Required fallback on Android 8–12.
    @Deprecated("Legacy back navigation")
    override fun onBackPressed() {
        if (active?.back == true) active?.session?.goBack() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("urls", ArrayList(tabs.map { it.url }))
        outState.putInt("selected", selected)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
        }
        browser.releaseSession()
        tabs.forEach { it.session.close() }
        tabs.clear()
        super.onDestroy()
    }
}
