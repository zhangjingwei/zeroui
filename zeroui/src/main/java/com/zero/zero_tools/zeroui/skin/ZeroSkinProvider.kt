package com.zero.zero_tools.zeroui.skin

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

val LocalZeroSkin = compositionLocalOf<ZeroSkin> {
    error("LocalZeroSkin is not provided")
}

val LocalZeroStyleResolver = compositionLocalOf<ZeroStyleResolver> {
    error("LocalZeroStyleResolver is not provided")
}

@Composable
fun ZeroSkinProvider(
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
fun rememberZeroSkinFromMaterialTheme(): ZeroSkin {
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
                unknownContainer = Color(0x1A000000)
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
