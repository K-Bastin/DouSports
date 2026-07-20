package com.dousports.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OrangeEnergy,
    onPrimary = Color.White,
    primaryContainer = OrangeDark,
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFE7BDAD),
    onSecondary = Color(0xFF442B20),
    secondaryContainer = Color(0xFF5D4035),
    onSecondaryContainer = Color(0xFFFFDBD0),
    background = NavyDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF21262D),
    error = RedError,
    onError = Color.White,
    tertiary = GreenSuccess,
    onTertiary = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = OrangeEnergy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3A0900),
    secondary = Color(0xFF77574B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD0),
    onSecondaryContainer = Color(0xFF2C160C),
    background = Color(0xFFEEF1F5),
    onBackground = Color(0xFF1C2128),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C2128),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF444C56),
    outline = Color(0xFFA8B2BE),
    outlineVariant = Color(0xFFCDD5DF),
    error = Color(0xFFCF222E),
    onError = Color.White,
    tertiary = Color(0xFF1A7F37),
    onTertiary = Color.White
)

@Composable
fun DouSportsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
