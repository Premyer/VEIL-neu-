package de.veil.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class AddressResolverTest {
    @Test fun resolvesAddresses() {
        mapOf(
            "" to "about:blank",
            "  " to "about:blank",
            "about:blank" to "about:blank",
            " example.org/path?q=one#two " to "https://example.org/path?q=one#two",
            "example.org:8443/path" to "https://example.org:8443/path",
            "localhost:8443" to "https://localhost:8443",
            "127.0.0.1:8443" to "https://127.0.0.1:8443",
            "[::1]:8443/path" to "https://[::1]:8443/path",
            "bücher.de" to "https://bücher.de",
            "HTTPS://example.org" to "HTTPS://example.org",
            "http://example.org" to "http://example.org"
        ).forEach { (input, expected) -> assertEquals(input, expected, AddressResolver.resolve(input)) }
    }

    @Test fun encodesSearchesAndDoesNotExecuteOtherSchemes() {
        mapOf(
            "two words" to "two+words",
            "two\twords.example" to "two%09words.example",
            "example.org:invalid" to "example.org%3Ainvalid",
            "javascript:alert(1)" to "javascript%3Aalert%281%29",
            "file:///etc/hosts" to "file%3A%2F%2F%2Fetc%2Fhosts",
            "a & b" to "a+%26+b"
        ).forEach { (input, encoded) ->
            assertEquals(input, "https://duckduckgo.com/?q=$encoded", AddressResolver.resolve(input))
        }
    }
}
