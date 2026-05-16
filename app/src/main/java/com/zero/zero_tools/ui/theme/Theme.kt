package com.zero.zero_tools.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.zero.zero_tools.zeroui.skin.ZeroSkinProvider
import com.zero.zero_tools.zeroui.skin.rememberTechElegantZeroSkin

private val DarkColorScheme = darkColorScheme(
    primary = LumenCyan,
    onPrimary = TechInk,
    primaryContainer = Color(0xFF18335F),
    onPrimaryContainer = Color(0xFFEAF0F8),
    secondary = ChampagneGold,
    onSecondary = TechInk,
    secondaryContainer = Color(0xFF3B3020),
    onSecondaryContainer = Color(0xFFF4EAD4),
    tertiary = SignalGreen,
    onTertiary = TechInk,
    tertiaryContainer = Color(0xFF14392E),
    onTertiaryContainer = Color(0xFFDDF5EC),
    error = Color(0xFFFF9999),
    errorContainer = Color(0xFF5B1F24),
    background = Color(0xFF0D111A),
    onBackground = Color(0xFFEAF0F8),
    surface = Color(0xFF101621),
    onSurface = Color(0xFFEAF0F8),
    surfaceVariant = Color(0xFF1B2230),
    onSurfaceVariant = Color(0xFFAAB4C4),
    outline = Color(0xFF3E4A5F),
    surfaceContainer = Color(0xFF1B2230),
    surfaceContainerLow = Color(0xFF121824)
)

private val LightColorScheme = lightColorScheme(
    primary = CircuitBlue,
    onPrimary = Color.White,
    primaryContainer = CircuitBlueSoft,
    onPrimaryContainer = TechInk,
    secondary = ChampagneGold,
    onSecondary = TechInk,
    secondaryContainer = ChampagneGoldSoft,
    onSecondaryContainer = TechInk,
    tertiary = SignalGreen,
    onTertiary = Color.White,
    tertiaryContainer = SignalGreenSoft,
    onTertiaryContainer = TechInk,
    error = SignalRed,
    errorContainer = SignalRedSoft,
    background = TechPorcelain,
    onBackground = TechInk,
    surface = Color.White,
    onSurface = TechInk,
    surfaceVariant = TechMist,
    onSurfaceVariant = Color(0xFF596273),
    outline = Color(0xFFC8D0DC),
    surfaceContainer = TechPorcelain,
    surfaceContainerLow = Color.White
)

@Composable
fun ZerotoolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        ZeroSkinProvider(
            skin = rememberTechElegantZeroSkin(darkTheme),
            content = content
        )
    }
}
