package com.zero.zero_tools.zeroui.skin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ZeroSkin(
    val palette: ZeroPalette,
    val typography: ZeroTypography,
    val shapes: ZeroShapes = ZeroShapes(),
    val spacing: ZeroSpacing = ZeroSpacing(),
    val density: ZeroDensity = ZeroDensity.Comfortable,
    val components: ZeroComponentTokens = ZeroComponentTokens.fromPalette(palette, density)
)

@Immutable
data class ZeroPalette(
    val content: Color,
    val mutedContent: Color,
    val primaryContent: Color,
    val successContent: Color,
    val errorContent: Color,
    val warningContent: Color,
    val container: Color,
    val mutedContainer: Color,
    val primaryContainer: Color,
    val successContainer: Color,
    val errorContainer: Color,
    val warningContainer: Color,
    val outline: Color,
    val mutedOutline: Color,
    val focusedOutline: Color,
    val errorOutline: Color,
    val unknownContainer: Color
)

typealias ZeroColors = ZeroPalette

@Immutable
data class ZeroTypography(
    val title: TextStyle,
    val sectionTitle: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val support: TextStyle
)

@Immutable
data class ZeroShapes(
    val cardCornerRadius: Dp = 12.dp
)

@Immutable
data class ZeroSpacing(
    val unknownNodePadding: Dp = 12.dp,
    val unknownNodeSpacing: Dp = 4.dp
)

enum class ZeroDensity {
    Comfortable,
    Compact
}

@Immutable
data class DensityValue<T>(
    val comfortable: T,
    val compact: T
) {
    fun resolve(density: ZeroDensity): T = when (density) {
        ZeroDensity.Comfortable -> comfortable
        ZeroDensity.Compact -> compact
    }
}

@Immutable
data class ZeroComponentTokens(
    val button: ZeroButtonTokens
) {
    companion object {
        fun fromPalette(
            palette: ZeroPalette,
            density: ZeroDensity
        ): ZeroComponentTokens {
            return ZeroComponentTokens(
                button = ZeroButtonTokens.fromPalette(palette, density)
            )
        }
    }
}

@Immutable
data class ZeroButtonTokens(
    val primary: ZeroButtonVariantTokens,
    val secondary: ZeroButtonVariantTokens
) {
    companion object {
        fun fromPalette(
            palette: ZeroPalette,
            density: ZeroDensity
        ): ZeroButtonTokens {
            val cornerRadius = DensityValue(
                comfortable = 8.dp,
                compact = 6.dp
            )
            val minHeight = DensityValue(
                comfortable = 40.dp,
                compact = 32.dp
            )
            val horizontalPadding = DensityValue(
                comfortable = 18.dp,
                compact = 14.dp
            )

            return ZeroButtonTokens(
                primary = ZeroButtonVariantTokens(
                    colors = ButtonStateColors(
                        container = palette.primaryContent,
                        content = palette.container,
                        disabledContainer = palette.primaryContent.copy(alpha = 0.12f),
                        disabledContent = palette.content.copy(alpha = 0.38f)
                    ),
                    cornerRadius = cornerRadius.resolve(density),
                    minHeight = minHeight.resolve(density),
                    horizontalPadding = horizontalPadding.resolve(density)
                ),
                secondary = ZeroButtonVariantTokens(
                    colors = ButtonStateColors(
                        container = Color.Transparent,
                        content = palette.primaryContent,
                        disabledContainer = Color.Transparent,
                        disabledContent = palette.content.copy(alpha = 0.38f)
                    ),
                    cornerRadius = cornerRadius.resolve(density),
                    minHeight = minHeight.resolve(density),
                    horizontalPadding = horizontalPadding.resolve(density)
                )
            )
        }
    }
}

@Immutable
data class ZeroButtonVariantTokens(
    val colors: ButtonStateColors,
    val cornerRadius: Dp,
    val minHeight: Dp,
    val horizontalPadding: Dp
)

@Immutable
data class ButtonStateColors(
    val container: Color,
    val content: Color,
    val disabledContainer: Color,
    val disabledContent: Color
)
