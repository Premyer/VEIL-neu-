package de.veil.browser

import java.net.URLEncoder

internal object AddressResolver {
    private val authorityPattern = Regex("""(?:\[[0-9a-fA-F:.%]+\]|[^:/?#@\s]+)(?::[0-9]{1,5})?""")

    fun resolve(input: String): String {
        val text = input.trim()
        if (text.isEmpty() || text == "about:blank") return "about:blank"
        if (text.startsWith("https://", ignoreCase = true) || text.startsWith("http://", ignoreCase = true)) return text
        val authority = text.substringBefore('/').substringBefore('?').substringBefore('#')
        val host = authority.substringBefore(':')
        if (text.none { it.isWhitespace() } && authorityPattern.matches(authority) &&
            (host.contains('.') || host == "localhost" || authority.startsWith('['))) {
            return "https://$text"
        }
        return "https://duckduckgo.com/?q=" + URLEncoder.encode(text, "UTF-8")
    }
}
