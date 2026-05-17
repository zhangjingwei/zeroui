package com.zero.zero_tools.zeroui.skin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class ZeroSkin(
    val palette: ZeroPalette,
    val typography: ZeroTypography,
    val shapes: ZeroShapes = ZeroShapes(),
    val spacing: ZeroSpacing = ZeroSpacing(),
    val density: ZeroDensity = ZeroDensity.Comfortable,
    val components: ZeroComponentTokens = ZeroComponentTokens.fromPalette(palette, density)
)

@Immutable
public data class ZeroPalette(
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
    val unknownContainer: Color,
    val inverseContent: Color = container,
    val inverseContainer: Color = content
)

public typealias ZeroColors = ZeroPalette

@Immutable
public data class ZeroTypography(
    val title: TextStyle,
    val sectionTitle: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val support: TextStyle
)

@Immutable
public data class ZeroShapes(
    val cardCornerRadius: Dp = 12.dp
)

@Immutable
public data class ZeroSpacing(
    val unknownNodePadding: Dp = 12.dp,
    val unknownNodeSpacing: Dp = 4.dp
)

public enum class ZeroDensity {
    Comfortable,
    Compact
}

@Immutable
public data class DensityValue<T>(
    val comfortable: T,
    val compact: T
) {
    public fun resolve(density: ZeroDensity): T = when (density) {
        ZeroDensity.Comfortable -> comfortable
        ZeroDensity.Compact -> compact
    }
}

@Immutable
public data class ZeroComponentTokens(
    val button: ZeroButtonTokens,
    val field: ZeroFieldTokens,
    val chip: ZeroChipTokens,
    val switch: ZeroSwitchTokens,
    val card: ZeroCardTokens
) {
    public companion object {
        public fun fromPalette(
            palette: ZeroPalette,
            density: ZeroDensity
        ): ZeroComponentTokens {
            return ZeroComponentTokens(
                button = ZeroButtonTokens.fromPalette(palette, density),
                field = ZeroFieldTokens.fromPalette(palette, density),
                chip = ZeroChipTokens.fromPalette(palette, density),
                switch = ZeroSwitchTokens.fromPalette(palette),
                card = ZeroCardTokens.fromPalette(palette)
            )
        }
    }
}

@Immutable
public data class ZeroButtonTokens(
    val primary: ZeroButtonVariantTokens,
    val secondary: ZeroButtonVariantTokens
) {
    public companion object {
        public fun fromPalette(
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
public data class ZeroButtonVariantTokens(
    val colors: ButtonStateColors,
    val cornerRadius: Dp,
    val minHeight: Dp,
    val horizontalPadding: Dp
)

@Immutable
public data class ButtonStateColors(
    val container: Color,
    val content: Color,
    val disabledContainer: Color,
    val disabledContent: Color
)

@Immutable
public data class ZeroFieldTokens(
    val colors: FieldStateColors,
    val cornerRadius: Dp,
    val minHeight: Dp
) {
    public companion object {
        public fun fromPalette(
            palette: ZeroPalette,
            density: ZeroDensity
        ): ZeroFieldTokens {
            val cornerRadius = DensityValue(
                comfortable = 8.dp,
                compact = 6.dp
            )
            val minHeight = DensityValue(
                comfortable = 56.dp,
                compact = 48.dp
            )

            return ZeroFieldTokens(
                colors = FieldStateColors(
                    content = palette.content,
                    label = palette.mutedContent,
                    container = Color.Transparent,
                    focusedOutline = palette.focusedOutline,
                    unfocusedOutline = palette.outline,
                    errorOutline = palette.errorOutline,
                    cursor = palette.primaryContent,
                    disabledContent = palette.content.copy(alpha = 0.38f),
                    disabledOutline = palette.mutedOutline.copy(alpha = 0.48f)
                ),
                cornerRadius = cornerRadius.resolve(density),
                minHeight = minHeight.resolve(density)
            )
        }
    }
}

@Immutable
public data class FieldStateColors(
    val content: Color,
    val label: Color,
    val container: Color,
    val focusedOutline: Color,
    val unfocusedOutline: Color,
    val errorOutline: Color,
    val cursor: Color,
    val disabledContent: Color,
    val disabledOutline: Color
)

@Immutable
public data class ZeroChipTokens(
    val selected: SelectableStateColors,
    val unselected: SelectableStateColors,
    val cornerRadius: Dp,
    val minHeight: Dp,
    val horizontalPadding: Dp
) {
    public companion object {
        public fun fromPalette(
            palette: ZeroPalette,
            density: ZeroDensity
        ): ZeroChipTokens {
            val cornerRadius = DensityValue(
                comfortable = 8.dp,
                compact = 6.dp
            )
            val minHeight = DensityValue(
                comfortable = 32.dp,
                compact = 28.dp
            )
            val horizontalPadding = DensityValue(
                comfortable = 12.dp,
                compact = 10.dp
            )

            return ZeroChipTokens(
                selected = SelectableStateColors(
                    container = palette.primaryContainer,
                    content = palette.primaryContent,
                    outline = palette.primaryContent,
                    disabledContainer = palette.mutedContainer.copy(alpha = 0.48f),
                    disabledContent = palette.content.copy(alpha = 0.38f),
                    disabledOutline = palette.mutedOutline.copy(alpha = 0.38f)
                ),
                unselected = SelectableStateColors(
                    container = Color.Transparent,
                    content = palette.content,
                    outline = palette.outline,
                    disabledContainer = Color.Transparent,
                    disabledContent = palette.content.copy(alpha = 0.38f),
                    disabledOutline = palette.mutedOutline.copy(alpha = 0.38f)
                ),
                cornerRadius = cornerRadius.resolve(density),
                minHeight = minHeight.resolve(density),
                horizontalPadding = horizontalPadding.resolve(density)
            )
        }
    }
}

@Immutable
public data class SelectableStateColors(
    val container: Color,
    val content: Color,
    val outline: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    val disabledOutline: Color
)

@Immutable
public data class ZeroSwitchTokens(
    val checked: SwitchStateColors,
    val unchecked: SwitchStateColors
) {
    public companion object {
        public fun fromPalette(palette: ZeroPalette): ZeroSwitchTokens {
            return ZeroSwitchTokens(
                checked = SwitchStateColors(
                    thumb = palette.container,
                    track = palette.primaryContent,
                    border = palette.primaryContent,
                    disabledThumb = palette.content.copy(alpha = 0.38f),
                    disabledTrack = palette.primaryContent.copy(alpha = 0.12f),
                    disabledBorder = palette.mutedOutline.copy(alpha = 0.38f)
                ),
                unchecked = SwitchStateColors(
                    thumb = palette.outline,
                    track = palette.mutedContainer,
                    border = palette.outline,
                    disabledThumb = palette.content.copy(alpha = 0.38f),
                    disabledTrack = palette.mutedContainer.copy(alpha = 0.48f),
                    disabledBorder = palette.mutedOutline.copy(alpha = 0.38f)
                )
            )
        }
    }
}

@Immutable
public data class SwitchStateColors(
    val thumb: Color,
    val track: Color,
    val border: Color,
    val disabledThumb: Color,
    val disabledTrack: Color,
    val disabledBorder: Color
)

@Immutable
public data class ZeroCardTokens(
    val default: CardToneTokens,
    val muted: CardToneTokens,
    val primary: CardToneTokens,
    val success: CardToneTokens,
    val error: CardToneTokens,
    val warning: CardToneTokens,
    val inverse: CardToneTokens = CardToneTokens(
        container = default.content,
        content = default.container
    ),
    val cornerRadius: Dp
) {
    public companion object {
        public fun fromPalette(palette: ZeroPalette): ZeroCardTokens {
            return ZeroCardTokens(
                default = CardToneTokens(container = palette.container, content = palette.content),
                muted = CardToneTokens(container = palette.mutedContainer, content = palette.content),
                primary = CardToneTokens(container = palette.primaryContainer, content = palette.content),
                success = CardToneTokens(container = palette.successContainer, content = palette.content),
                error = CardToneTokens(container = palette.errorContainer, content = palette.content),
                warning = CardToneTokens(container = palette.warningContainer, content = palette.content),
                inverse = CardToneTokens(container = palette.inverseContainer, content = palette.inverseContent),
                cornerRadius = 8.dp
            )
        }
    }
}

@Immutable
public data class CardToneTokens(
    val container: Color,
    val content: Color
)

@Immutable
public data class ZeroCardResolvedTokens(
    val colors: CardToneTokens,
    val cornerRadius: Dp
)
