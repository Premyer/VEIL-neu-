/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.compose.base.theme.Theme
import mozilla.components.compose.base.theme.acornDarkColorScheme
import mozilla.components.compose.base.theme.acornLightColorScheme
import mozilla.components.compose.base.theme.acornOledColorScheme
import mozilla.components.compose.base.theme.acornPrivateColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class VeilThemeTest {
    @Test
    @Config(qualifiers = "notnight")
    fun `light Android views match the light Compose theme`() {
        assertViewPalette(Theme.Light)
    }

    @Test
    @Config(qualifiers = "night")
    fun `dark Android views match the dark Compose theme`() {
        assertViewPalette(Theme.Dark)
    }

    private fun assertViewPalette(theme: Theme) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colors = veilColorScheme(theme)
        mapOf(
            R.color.fx_mobile_primary to colors.primary,
            R.color.fx_mobile_on_primary to colors.onPrimary,
            R.color.fx_mobile_surface to colors.surface,
            R.color.fx_mobile_on_surface to colors.onSurface,
        ).forEach { (resource, color) ->
            assertEquals(color.toArgb(), ContextCompat.getColor(context, resource))
        }
    }

    @Test
    fun `text and action colors retain readable contrast in every theme`() {
        Theme.entries.forEach { theme ->
            val colors = veilColorScheme(theme)
            listOf(
                colors.onPrimary to colors.primary,
                colors.onPrimaryContainer to colors.primaryContainer,
                colors.onSecondaryContainer to colors.secondaryContainer,
                colors.onSurface to colors.surface,
                colors.onSurfaceVariant to colors.surfaceVariant,
                colors.onBackground to colors.background,
            ).forEach { (foreground, background) ->
                assertTrue("Insufficient text contrast in $theme", contrast(foreground, background) >= 4.5f)
            }
        }
    }

    @Test
    fun `private browsing remains visually distinct and OLED keeps a black surface`() {
        assertNotEquals(veilColorScheme(Theme.Dark).primary, veilColorScheme(Theme.Private).primary)
        assertNotEquals(veilColorScheme(Theme.Dark).surface, veilColorScheme(Theme.Private).surface)
        assertEquals(Color.Black, veilColorScheme(Theme.Oled).surface)
        assertEquals(Color.Black, veilColorScheme(Theme.Oled).background)
    }

    @Test
    fun `rebranding preserves upstream error semantics`() {
        mapOf(
            Theme.Light to acornLightColorScheme(),
            Theme.Dark to acornDarkColorScheme(),
            Theme.Oled to acornOledColorScheme(),
            Theme.Private to acornPrivateColorScheme(),
        ).forEach { (theme, upstream) ->
            val colors = veilColorScheme(theme)
            assertEquals(upstream.error, colors.error)
            assertEquals(upstream.onError, colors.onError)
            assertEquals(upstream.errorContainer, colors.errorContainer)
            assertEquals(upstream.onErrorContainer, colors.onErrorContainer)
        }
    }

    private fun contrast(foreground: Color, background: Color): Float {
        val first = foreground.luminance()
        val second = background.luminance()
        return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
    }
}
