package top.pingwcy.wifianalyzer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    inversePrimary = TealInversePrimary,
    secondary = TealSecondary,
    onSecondary = TealOnSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,
    tertiary = TealTertiary,
    onTertiary = TealOnTertiary,
    tertiaryContainer = TealTertiaryContainer,
    onTertiaryContainer = TealOnTertiaryContainer,
    background = TealBackground,
    onBackground = TealOnBackground,
    surface = TealSurface,
    onSurface = TealOnSurface,
    surfaceVariant = TealSurfaceVariant,
    onSurfaceVariant = TealOnSurfaceVariant,
    surfaceTint = TealPrimary,
    inverseSurface = TealInverseSurface,
    inverseOnSurface = TealInverseOnSurface,
    outline = TealOutline,
    outlineVariant = TealOutlineVariant,
    scrim = TealScrim,
    surfaceBright = TealSurfaceBright,
    surfaceDim = TealSurfaceDim,
    surfaceContainer = TealSurfaceContainer,
    surfaceContainerHigh = TealSurfaceContainerHigh,
    surfaceContainerHighest = TealSurfaceContainerHighest,
    surfaceContainerLow = TealSurfaceContainerLow,
    surfaceContainerLowest = TealSurfaceContainerLowest,
)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealOnPrimaryContainerDark,
    inversePrimary = TealInversePrimaryDark,
    secondary = TealSecondaryDark,
    onSecondary = TealOnSecondaryDark,
    secondaryContainer = TealSecondaryContainerDark,
    onSecondaryContainer = TealOnSecondaryContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = TealOnTertiaryDark,
    tertiaryContainer = TealTertiaryContainerDark,
    onTertiaryContainer = TealOnTertiaryContainerDark,
    background = TealBackgroundDark,
    onBackground = TealOnBackgroundDark,
    surface = TealSurfaceDark,
    onSurface = TealOnSurfaceDark,
    surfaceVariant = TealSurfaceVariantDark,
    onSurfaceVariant = TealOnSurfaceVariantDark,
    surfaceTint = TealPrimaryDark,
    inverseSurface = TealInverseSurfaceDark,
    inverseOnSurface = TealInverseOnSurfaceDark,
    outline = TealOutlineDark,
    outlineVariant = TealOutlineVariantDark,
    scrim = TealScrimDark,
    surfaceBright = TealSurfaceBrightDark,
    surfaceDim = TealSurfaceDimDark,
    surfaceContainer = TealSurfaceContainerDark,
    surfaceContainerHigh = TealSurfaceContainerHighDark,
    surfaceContainerHighest = TealSurfaceContainerHighestDark,
    surfaceContainerLow = TealSurfaceContainerLowDark,
    surfaceContainerLowest = TealSurfaceContainerLowestDark,
)

/**
 * WiFi 分析器主题：固定「科技青绿」品牌配色，不随系统壁纸动态变色，
 * 保证 App 在不同设备上拥有稳定的视觉身份。
 */
@Composable
fun WiFiAnalyzerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
