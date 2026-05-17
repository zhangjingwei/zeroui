package com.zero.zero_tools.zeroui.skin

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

public val LocalZeroSkin: androidx.compose.runtime.ProvidableCompositionLocal<ZeroSkin> = compositionLocalOf {
    error("LocalZeroSkin is not provided")
}

public val LocalZeroStyleResolver: androidx.compose.runtime.ProvidableCompositionLocal<ZeroStyleResolver> = compositionLocalOf {
    error("LocalZeroStyleResolver is not provided")
}

@Composable
public fun ZeroSkinProvider(
    skin: ZeroSkin = rememberZeroSkinFromMaterialTheme(),
    content: @Composable () -> Unit
) {
    val resolver = remember(skin) {
        ZeroStyleResolver(skin)
    }

    CompositionLocalProvider(
        LocalZeroSkin provides skin,
        LocalZeroStyleResolver provides resolver,
        content = content
    )
}

@Composable
public fun rememberZeroSkinFromMaterialTheme(): ZeroSkin {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    return remember(colorScheme, typography) {
        ZeroSkin(
            palette = ZeroPalette(
                content = colorScheme.onSurface,
                mutedContent = colorScheme.onSurfaceVariant,
                primaryContent = colorScheme.primary,
                successContent = colorScheme.tertiary,
                errorContent = colorScheme.error,
                warningContent = colorScheme.secondary,
                container = colorScheme.surfaceContainer,
                mutedContainer = colorScheme.surfaceContainerLow,
                primaryContainer = colorScheme.primaryContainer,
                successContainer = colorScheme.tertiaryContainer,
                errorContainer = colorScheme.errorContainer,
                warningContainer = colorScheme.secondaryContainer,
                outline = colorScheme.outline,
                mutedOutline = colorScheme.outlineVariant,
                focusedOutline = colorScheme.primary,
                errorOutline = colorScheme.error,
                unknownContainer = Color(0x1A000000),
                inverseContent = colorScheme.inverseOnSurface,
                inverseContainer = colorScheme.inverseSurface
            ),
            typography = ZeroTypography(
                title = typography.headlineMedium,
                sectionTitle = typography.titleMedium,
                body = typography.bodyMedium,
                label = typography.labelLarge,
                support = typography.bodySmall
            )
        )
    }
}
