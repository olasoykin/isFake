package com.isfake.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Teal50,
    onPrimary = White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = Teal60,
    onSecondary = White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal10,
    tertiary = Teal40,
    onTertiary = White,
    tertiaryContainer = Teal95,
    onTertiaryContainer = Teal10,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Teal99,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Teal70,
    onSecondary = Teal10,
    secondaryContainer = Teal20,
    onSecondaryContainer = Teal80,
    tertiary = Teal60,
    onTertiary = Teal10,
    tertiaryContainer = Teal20,
    onTertiaryContainer = Teal90,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Teal10,
    onBackground = Teal90,
    surface = Color(0xFF0E1517),
    onSurface = Teal90,
    surfaceVariant = Color(0xFF3F4849),
    onSurfaceVariant = Color(0xFFBEC8CA),
    outline = Color(0xFF899393),
    outlineVariant = Color(0xFF3F4849)
)

@Composable
fun IsFakeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

// Workaround for the Color import in the DarkColorScheme definition above
private fun Color(value: Long): androidx.compose.ui.graphics.Color =
    androidx.compose.ui.graphics.Color(value)
