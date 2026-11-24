package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhite,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    secondary = SkyBlueSecondary,
    onSecondary = PureWhite,
    secondaryContainer = SkyBlueContainer,
    onSecondaryContainer = OnSkyBlueContainer,
    tertiary = TealTertiary,
    onTertiary = PureWhite,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,
    background = MintSurface,
    onBackground = TextPrimary,
    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFEAF4EE),
    onSurfaceVariant = TextSecondary,
    outline = CardOutline,
    error = StatusCritical,
    onError = PureWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF003822),
    primaryContainer = EmeraldDark,
    onPrimaryContainer = MintContainer,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF003549),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = SkyBlueContainer,
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF003731),
    background = DarkBackground,
    onBackground = Color(0xFFE2F1EA),
    surface = DarkSurface,
    onSurface = Color(0xFFE2F1EA),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7C4B5),
    outline = Color(0xFF284D3C),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF690005)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartVillageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
