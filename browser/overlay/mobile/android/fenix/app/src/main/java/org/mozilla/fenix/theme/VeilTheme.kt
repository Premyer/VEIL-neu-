/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import mozilla.components.compose.base.theme.Theme
import mozilla.components.compose.base.theme.acornDarkColorScheme
import mozilla.components.compose.base.theme.acornLightColorScheme
import mozilla.components.compose.base.theme.acornOledColorScheme
import mozilla.components.compose.base.theme.acornPrivateColorScheme

internal fun veilColorScheme(theme: Theme): ColorScheme {
    val base = when (theme) {
        Theme.Light -> acornLightColorScheme()
        Theme.Dark -> acornDarkColorScheme()
        Theme.Oled -> acornOledColorScheme()
        Theme.Private -> acornPrivateColorScheme()
    }
    return when (theme) {
        Theme.Light -> base.copy(
            primary = Color(0xFF006B60),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFB9F4E7),
            onPrimaryContainer = Color(0xFF063B35),
            inversePrimary = Color(0xFF62DFC5),
            secondary = Color(0xFF46605A),
            secondaryContainer = Color(0xFFDCECE6),
            onSecondaryContainer = Color(0xFF18352E),
            tertiary = Color(0xFF285F86),
            tertiaryContainer = Color(0xFFD4E9FF),
            onTertiaryContainer = Color(0xFF12334D),
            background = Color(0xFFF3F8F6),
            onBackground = Color(0xFF142622),
            surface = Color(0xFFF3F8F6),
            onSurface = Color(0xFF142622),
            surfaceVariant = Color(0xFFE0EBE6),
            onSurfaceVariant = Color(0xFF405950),
            surfaceTint = Color(0xFF006B60),
            outline = Color(0xFF657F75),
            outlineVariant = Color(0xFFC1D4CC),
            surfaceBright = Color.White,
            surfaceDim = Color(0xFFDCE7E2),
            surfaceContainer = Color(0xFFEDF4F0),
            surfaceContainerHigh = Color(0xFFE5EFE9),
            surfaceContainerHighest = Color(0xFFDCE8E1),
            surfaceContainerLow = Color(0xFFF7FBF9),
            surfaceContainerLowest = Color.White,
        )
        Theme.Dark, Theme.Oled -> base.copy(
            primary = Color(0xFF62DFC5),
            onPrimary = Color(0xFF002F28),
            primaryContainer = Color(0xFF164C42),
            onPrimaryContainer = Color(0xFFC3FBEA),
            inversePrimary = Color(0xFF006B60),
            secondary = Color(0xFFB9CEC5),
            secondaryContainer = Color(0xFF294139),
            onSecondaryContainer = Color(0xFFDCEEE5),
            tertiary = Color(0xFF9CCBEB),
            tertiaryContainer = Color(0xFF1B4058),
            onTertiaryContainer = Color(0xFFD4E9FF),
            background = if (theme == Theme.Oled) Color.Black else Color(0xFF0B1514),
            onBackground = Color(0xFFE2F1EB),
            surface = if (theme == Theme.Oled) Color.Black else Color(0xFF0B1514),
            onSurface = Color(0xFFE2F1EB),
            surfaceVariant = Color(0xFF233731),
            onSurfaceVariant = Color(0xFFB7CEC4),
            surfaceTint = Color(0xFF62DFC5),
            outline = Color(0xFF7E9B8F),
            outlineVariant = Color(0xFF344E44),
            surfaceBright = Color(0xFF2A4038),
            surfaceDim = Color(0xFF07100E),
            surfaceContainer = Color(0xFF101F1B),
            surfaceContainerHigh = Color(0xFF192C25),
            surfaceContainerHighest = Color(0xFF23372F),
            surfaceContainerLow = Color(0xFF0E1B17),
            surfaceContainerLowest = if (theme == Theme.Oled) Color.Black else Color(0xFF060E0B),
        )
        Theme.Private -> base.copy(
            primary = Color(0xFFFFCC80),
            onPrimary = Color(0xFF402600),
            primaryContainer = Color(0xFF59421D),
            onPrimaryContainer = Color(0xFFFFE4B8),
            secondary = Color(0xFFD5C4A9),
            secondaryContainer = Color(0xFF443B2D),
            onSecondaryContainer = Color(0xFFF1E2CA),
            background = Color(0xFF17130E),
            onBackground = Color(0xFFF1E9DB),
            surface = Color(0xFF17130E),
            onSurface = Color(0xFFF1E9DB),
            surfaceVariant = Color(0xFF393126),
            onSurfaceVariant = Color(0xFFD4C5AD),
            surfaceTint = Color(0xFFFFCC80),
            outline = Color(0xFFAA9474),
            outlineVariant = Color(0xFF514531),
            surfaceBright = Color(0xFF40372B),
            surfaceDim = Color(0xFF100D08),
            surfaceContainer = Color(0xFF211B13),
            surfaceContainerHigh = Color(0xFF2C2419),
            surfaceContainerHighest = Color(0xFF393023),
            surfaceContainerLow = Color(0xFF1C160F),
            surfaceContainerLowest = Color(0xFF0E0B06),
        )
    }
}
