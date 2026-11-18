package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AetherDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = AzureAccent,
    onSecondary = OnElectricCyan,
    secondaryContainer = AzureContainer,
    onSecondaryContainer = CrispWhite,
    tertiary = EmeraldStatus,
    onTertiary = OnElectricCyan,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = CrispWhite,
    background = ObsidianBackground,
    onBackground = CrispWhite,
    surface = ObsidianSurface,
    onSurface = CrispWhite,
    surfaceVariant = TitaniumSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = TitaniumSurfaceVariant,
    surfaceContainerHigh = TitaniumSurfaceHigh,
    outline = TitaniumBorder,
    outlineVariant = SlateDim,
    error = CoralDanger,
    onError = OnElectricCyan,
    errorContainer = CoralContainer,
    onErrorContainer = CrispWhite
)

private val AetherLightColorScheme = lightColorScheme(
    primary = DeepCyanLight,
    onPrimary = LightTitaniumSurface,
    primaryContainer = DeepCyanContainerLight,
    onPrimaryContainer = SlateDarkText,
    secondary = AzureAccent,
    onSecondary = SlateDarkText,
    tertiary = EmeraldStatus,
    background = LightTitaniumBg,
    onBackground = SlateDarkText,
    surface = LightTitaniumSurface,
    onSurface = SlateDarkText,
    surfaceVariant = LightTitaniumVariant,
    onSurfaceVariant = SlateMediumText,
    error = CoralDanger
)

val AetherShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AetherBudsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AetherDarkColorScheme else AetherLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AetherShapes,
        content = content
    )
}
