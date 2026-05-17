@file:Suppress("MagicNumber")

package com.example.brand.zeroui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zero.zero_tools.zeroui.skin.ZeroDensity
import com.zero.zero_tools.zeroui.skin.ZeroPalette
import com.zero.zero_tools.zeroui.skin.ZeroShapes
import com.zero.zero_tools.zeroui.skin.ZeroSkin
import com.zero.zero_tools.zeroui.skin.ZeroTypography

/**
 * Copy this file into a brand/theme integration package and replace the color values.
 *
 * ZeroSkinProvider accepts the resolved ZeroSkin object:
 *
 * ZeroSkinProvider(skin = rememberBrandZeroSkin(darkTheme)) {
 *     ZeroUiHost(startPage = "home")
 * }
 */
@Composable
public fun rememberBrandZeroSkin(darkTheme: Boolean): ZeroSkin {
    val materialTypography = MaterialTheme.typography

    return remember(darkTheme, materialTypography) {
        val palette = if (darkTheme) brandDarkPalette() else brandLightPalette()

        ZeroSkin(
            palette = palette,
            typography = ZeroTypography(
                title = materialTypography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                ),
                sectionTitle = materialTypography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                ),
                body = materialTypography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp
                ),
                label = materialTypography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                ),
                support = materialTypography.bodySmall.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp
                )
            ),
            shapes = ZeroShapes(
                cardCornerRadius = 8.dp
            ),
            density = ZeroDensity.Comfortable
        )
    }
}

private fun brandLightPalette(): ZeroPalette {
    return ZeroPalette(
        content = Color(0xFF10141F),
        mutedContent = Color(0xFF596273),
        primaryContent = Color(0xFF255CDE),
        successContent = Color(0xFF1C8F67),
        errorContent = Color(0xFFC84444),
        warningContent = Color(0xFF9A6A00),
        container = Color(0xFFF7F9FC),
        mutedContainer = Color(0xFFE6EBF2),
        primaryContainer = Color(0xFFDDE7FF),
        successContainer = Color(0xFFDDF5EC),
        errorContainer = Color(0xFFFFE2E0),
        warningContainer = Color(0xFFFFF1C7),
        outline = Color(0xFFC8D0DC),
        mutedOutline = Color(0xFFDCE2EA),
        focusedOutline = Color(0xFF255CDE),
        errorOutline = Color(0xFFC84444),
        unknownContainer = Color(0x1A000000),
        inverseContent = Color(0xFFF7F9FC),
        inverseContainer = Color(0xFF10141F)
    )
}

private fun brandDarkPalette(): ZeroPalette {
    return ZeroPalette(
        content = Color(0xFFEAF0F8),
        mutedContent = Color(0xFFAAB4C4),
        primaryContent = Color(0xFF8FB0FF),
        successContent = Color(0xFF75DDBA),
        errorContent = Color(0xFFFF9999),
        warningContent = Color(0xFFFFCF6B),
        container = Color(0xFF1B2230),
        mutedContainer = Color(0xFF121824),
        primaryContainer = Color(0xFF18335F),
        successContainer = Color(0xFF14392E),
        errorContainer = Color(0xFF5B1F24),
        warningContainer = Color(0xFF4A3714),
        outline = Color(0xFF3E4A5F),
        mutedOutline = Color(0xFF2A3445),
        focusedOutline = Color(0xFF8FB0FF),
        errorOutline = Color(0xFFFF9999),
        unknownContainer = Color(0xFF3B3020),
        inverseContent = Color(0xFF10141F),
        inverseContainer = Color(0xFFEAF0F8)
    )
}
