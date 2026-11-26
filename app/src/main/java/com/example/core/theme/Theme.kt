package com.example.core.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val MyCareShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String): ThemeMode = entries.find { it.key == key } ?: SYSTEM
    }
}

enum class AccentTheme(
    val key: String,
    val previewBg: Color,
    val previewSurface: Color,
    val previewNavBar: Color,
    val previewAccent: Color
) {
    SOFT_ASH(
        key = "soft_ash",
        previewBg = SoftAshBackgroundLight,
        previewSurface = SoftAshSurfaceLight,
        previewNavBar = SoftAshContainerLight,
        previewAccent = SoftAshPrimaryLight
    ),
    SOFT_PINK(
        key = "soft_pink",
        previewBg = SoftPinkBackgroundLight,
        previewSurface = SoftPinkSurfaceLight,
        previewNavBar = SoftPinkContainerLight,
        previewAccent = SoftPinkPrimaryLight
    ),
    SOFT_BLUE(
        key = "soft_blue",
        previewBg = SoftBlueBackgroundLight,
        previewSurface = SoftBlueSurfaceLight,
        previewNavBar = SoftBlueContainerLight,
        previewAccent = SoftBluePrimaryLight
    ),
    SOFT_WHITE_GREEN(
        key = "soft_white_green",
        previewBg = SoftGreenBackgroundLight,
        previewSurface = SoftGreenSurfaceLight,
        previewNavBar = SoftGreenContainerLight,
        previewAccent = SoftGreenPrimaryLight
    );

    companion object {
        fun fromKey(key: String): AccentTheme = when (key) {
            "soft_ash" -> SOFT_ASH
            "soft_pink" -> SOFT_PINK
            "soft_blue", "ocean" -> SOFT_BLUE
            "soft_white_green", "teal", "emerald", "sage" -> SOFT_WHITE_GREEN
            else -> entries.find { it.key == key } ?: SOFT_WHITE_GREEN
        }
    }
}

private fun buildLightColorScheme(accent: AccentTheme): ColorScheme = when (accent) {
    AccentTheme.SOFT_ASH -> lightColorScheme(
        primary = SoftAshPrimaryLight,
        onPrimary = SoftAshOnPrimaryLight,
        primaryContainer = SoftAshPrimaryContainerLight,
        onPrimaryContainer = SoftAshOnPrimaryContainerLight,
        secondary = SoftAshSecondaryLight,
        onSecondary = SoftAshOnPrimaryLight,
        secondaryContainer = SoftAshSecondaryContainerLight,
        onSecondaryContainer = SoftAshOnSecondaryContainerLight,
        tertiary = SoftAshTertiaryLight,
        onTertiary = SoftAshOnPrimaryLight,
        tertiaryContainer = SoftAshTertiaryContainerLight,
        onTertiaryContainer = SoftAshOnPrimaryContainerLight,
        background = SoftAshBackgroundLight,
        onBackground = SoftAshOnSurfaceLight,
        surface = SoftAshSurfaceLight,
        onSurface = SoftAshOnSurfaceLight,
        surfaceVariant = SoftAshSurfaceVariantLight,
        onSurfaceVariant = SoftAshOnSurfaceVariantLight,
        surfaceTint = SoftAshPrimaryLight,
        surfaceBright = SoftAshSurfaceBrightLight,
        surfaceDim = SoftAshSurfaceDimLight,
        surfaceContainerLowest = SoftAshContainerLowestLight,
        surfaceContainerLow = SoftAshContainerLowLight,
        surfaceContainer = SoftAshContainerLight,
        surfaceContainerHigh = SoftAshContainerHighLight,
        surfaceContainerHighest = SoftAshContainerHighestLight,
        outline = SoftAshOutlineLight,
        outlineVariant = SoftAshOutlineVariantLight,
        error = ErrorRed,
        errorContainer = ErrorContainerLight
    )
    AccentTheme.SOFT_PINK -> lightColorScheme(
        primary = SoftPinkPrimaryLight,
        onPrimary = SoftPinkOnPrimaryLight,
        primaryContainer = SoftPinkPrimaryContainerLight,
        onPrimaryContainer = SoftPinkOnPrimaryContainerLight,
        secondary = SoftPinkSecondaryLight,
        onSecondary = SoftPinkOnPrimaryLight,
        secondaryContainer = SoftPinkSecondaryContainerLight,
        onSecondaryContainer = SoftPinkOnSecondaryContainerLight,
        tertiary = SoftPinkTertiaryLight,
        onTertiary = SoftPinkOnPrimaryLight,
        tertiaryContainer = SoftPinkTertiaryContainerLight,
        onTertiaryContainer = SoftPinkOnPrimaryContainerLight,
        background = SoftPinkBackgroundLight,
        onBackground = SoftPinkOnSurfaceLight,
        surface = SoftPinkSurfaceLight,
        onSurface = SoftPinkOnSurfaceLight,
        surfaceVariant = SoftPinkSurfaceVariantLight,
        onSurfaceVariant = SoftPinkOnSurfaceVariantLight,
        surfaceTint = SoftPinkPrimaryLight,
        surfaceBright = SoftPinkSurfaceBrightLight,
        surfaceDim = SoftPinkSurfaceDimLight,
        surfaceContainerLowest = SoftPinkContainerLowestLight,
        surfaceContainerLow = SoftPinkContainerLowLight,
        surfaceContainer = SoftPinkContainerLight,
        surfaceContainerHigh = SoftPinkContainerHighLight,
        surfaceContainerHighest = SoftPinkContainerHighestLight,
        outline = SoftPinkOutlineLight,
        outlineVariant = SoftPinkOutlineVariantLight,
        error = ErrorRed,
        errorContainer = ErrorContainerLight
    )
    AccentTheme.SOFT_BLUE -> lightColorScheme(
        primary = SoftBluePrimaryLight,
        onPrimary = SoftBlueOnPrimaryLight,
        primaryContainer = SoftBluePrimaryContainerLight,
        onPrimaryContainer = SoftBlueOnPrimaryContainerLight,
        secondary = SoftBlueSecondaryLight,
        onSecondary = SoftBlueOnPrimaryLight,
        secondaryContainer = SoftBlueSecondaryContainerLight,
        onSecondaryContainer = SoftBlueOnSecondaryContainerLight,
        tertiary = SoftBlueTertiaryLight,
        onTertiary = SoftBlueOnPrimaryLight,
        tertiaryContainer = SoftBlueTertiaryContainerLight,
        onTertiaryContainer = SoftBlueOnPrimaryContainerLight,
        background = SoftBlueBackgroundLight,
        onBackground = SoftBlueOnSurfaceLight,
        surface = SoftBlueSurfaceLight,
        onSurface = SoftBlueOnSurfaceLight,
        surfaceVariant = SoftBlueSurfaceVariantLight,
        onSurfaceVariant = SoftBlueOnSurfaceVariantLight,
        surfaceTint = SoftBluePrimaryLight,
        surfaceBright = SoftBlueSurfaceBrightLight,
        surfaceDim = SoftBlueSurfaceDimLight,
        surfaceContainerLowest = SoftBlueContainerLowestLight,
        surfaceContainerLow = SoftBlueContainerLowLight,
        surfaceContainer = SoftBlueContainerLight,
        surfaceContainerHigh = SoftBlueContainerHighLight,
        surfaceContainerHighest = SoftBlueContainerHighestLight,
        outline = SoftBlueOutlineLight,
        outlineVariant = SoftBlueOutlineVariantLight,
        error = ErrorRed,
        errorContainer = ErrorContainerLight
    )
    AccentTheme.SOFT_WHITE_GREEN -> lightColorScheme(
        primary = SoftGreenPrimaryLight,
        onPrimary = SoftGreenOnPrimaryLight,
        primaryContainer = SoftGreenPrimaryContainerLight,
        onPrimaryContainer = SoftGreenOnPrimaryContainerLight,
        secondary = SoftGreenSecondaryLight,
        onSecondary = SoftGreenOnPrimaryLight,
        secondaryContainer = SoftGreenSecondaryContainerLight,
        onSecondaryContainer = SoftGreenOnSecondaryContainerLight,
        tertiary = SoftGreenTertiaryLight,
        onTertiary = SoftGreenOnPrimaryLight,
        tertiaryContainer = SoftGreenTertiaryContainerLight,
        onTertiaryContainer = SoftGreenOnPrimaryContainerLight,
        background = SoftGreenBackgroundLight,
        onBackground = SoftGreenOnSurfaceLight,
        surface = SoftGreenSurfaceLight,
        onSurface = SoftGreenOnSurfaceLight,
        surfaceVariant = SoftGreenSurfaceVariantLight,
        onSurfaceVariant = SoftGreenOnSurfaceVariantLight,
        surfaceTint = SoftGreenPrimaryLight,
        surfaceBright = SoftGreenSurfaceBrightLight,
        surfaceDim = SoftGreenSurfaceDimLight,
        surfaceContainerLowest = SoftGreenContainerLowestLight,
        surfaceContainerLow = SoftGreenContainerLowLight,
        surfaceContainer = SoftGreenContainerLight,
        surfaceContainerHigh = SoftGreenContainerHighLight,
        surfaceContainerHighest = SoftGreenContainerHighestLight,
        outline = SoftGreenOutlineLight,
        outlineVariant = SoftGreenOutlineVariantLight,
        error = ErrorRed,
        errorContainer = ErrorContainerLight
    )
}

private fun buildDarkColorScheme(accent: AccentTheme): ColorScheme = when (accent) {
    AccentTheme.SOFT_ASH -> darkColorScheme(
        primary = SoftAshPrimaryDark,
        onPrimary = SoftAshOnPrimaryDark,
        primaryContainer = SoftAshPrimaryContainerDark,
        onPrimaryContainer = SoftAshOnPrimaryContainerDark,
        secondary = SoftAshSecondaryDark,
        onSecondary = SoftAshOnPrimaryDark,
        secondaryContainer = SoftAshSecondaryContainerDark,
        onSecondaryContainer = SoftAshOnPrimaryContainerDark,
        tertiary = SoftAshPrimaryDark,
        onTertiary = SoftAshOnPrimaryDark,
        tertiaryContainer = SoftAshPrimaryContainerDark,
        onTertiaryContainer = SoftAshOnPrimaryContainerDark,
        background = SoftAshBackgroundDark,
        onBackground = DarkTextPrimary,
        surface = SoftAshSurfaceDark,
        onSurface = DarkTextPrimary,
        surfaceVariant = SoftAshSurfaceVariantDark,
        onSurfaceVariant = DarkTextSecondary,
        surfaceTint = SoftAshPrimaryDark,
        surfaceBright = SoftAshSurfaceBrightDark,
        surfaceDim = SoftAshSurfaceDimDark,
        surfaceContainerLowest = SoftAshContainerLowestDark,
        surfaceContainerLow = SoftAshContainerLowDark,
        surfaceContainer = SoftAshContainerDark,
        surfaceContainerHigh = SoftAshContainerHighDark,
        surfaceContainerHighest = SoftAshContainerHighestDark,
        outline = SoftAshOutlineDark,
        outlineVariant = SoftAshOutlineVariantDark,
        error = ErrorRed,
        errorContainer = ErrorContainerDark
    )
    AccentTheme.SOFT_PINK -> darkColorScheme(
        primary = SoftPinkPrimaryDark,
        onPrimary = SoftPinkOnPrimaryDark,
        primaryContainer = SoftPinkPrimaryContainerDark,
        onPrimaryContainer = SoftPinkOnPrimaryContainerDark,
        secondary = SoftPinkSecondaryDark,
        onSecondary = SoftPinkOnPrimaryDark,
        secondaryContainer = SoftPinkSecondaryContainerDark,
        onSecondaryContainer = SoftPinkOnPrimaryContainerDark,
        tertiary = SoftPinkPrimaryDark,
        onTertiary = SoftPinkOnPrimaryDark,
        tertiaryContainer = SoftPinkPrimaryContainerDark,
        onTertiaryContainer = SoftPinkOnPrimaryContainerDark,
        background = SoftPinkBackgroundDark,
        onBackground = DarkTextPrimary,
        surface = SoftPinkSurfaceDark,
        onSurface = DarkTextPrimary,
        surfaceVariant = SoftPinkSurfaceVariantDark,
        onSurfaceVariant = DarkTextSecondary,
        surfaceTint = SoftPinkPrimaryDark,
        surfaceBright = SoftPinkSurfaceBrightDark,
        surfaceDim = SoftPinkSurfaceDimDark,
        surfaceContainerLowest = SoftPinkContainerLowestDark,
        surfaceContainerLow = SoftPinkContainerLowDark,
        surfaceContainer = SoftPinkContainerDark,
        surfaceContainerHigh = SoftPinkContainerHighDark,
        surfaceContainerHighest = SoftPinkContainerHighestDark,
        outline = SoftPinkOutlineDark,
        outlineVariant = SoftPinkOutlineVariantDark,
        error = ErrorRed,
        errorContainer = ErrorContainerDark
    )
    AccentTheme.SOFT_BLUE -> darkColorScheme(
        primary = SoftBluePrimaryDark,
        onPrimary = SoftBlueOnPrimaryDark,
        primaryContainer = SoftBluePrimaryContainerDark,
        onPrimaryContainer = SoftBlueOnPrimaryContainerDark,
        secondary = SoftBlueSecondaryDark,
        onSecondary = SoftBlueOnPrimaryDark,
        secondaryContainer = SoftBlueSecondaryContainerDark,
        onSecondaryContainer = SoftBlueOnPrimaryContainerDark,
        tertiary = SoftBluePrimaryDark,
        onTertiary = SoftBlueOnPrimaryDark,
        tertiaryContainer = SoftBluePrimaryContainerDark,
        onTertiaryContainer = SoftBlueOnPrimaryContainerDark,
        background = SoftBlueBackgroundDark,
        onBackground = DarkTextPrimary,
        surface = SoftBlueSurfaceDark,
        onSurface = DarkTextPrimary,
        surfaceVariant = SoftBlueSurfaceVariantDark,
        onSurfaceVariant = DarkTextSecondary,
        surfaceTint = SoftBluePrimaryDark,
        surfaceBright = SoftBlueSurfaceBrightDark,
        surfaceDim = SoftBlueSurfaceDimDark,
        surfaceContainerLowest = SoftBlueContainerLowestDark,
        surfaceContainerLow = SoftBlueContainerLowDark,
        surfaceContainer = SoftBlueContainerDark,
        surfaceContainerHigh = SoftBlueContainerHighDark,
        surfaceContainerHighest = SoftBlueContainerHighestDark,
        outline = SoftBlueOutlineDark,
        outlineVariant = SoftBlueOutlineVariantDark,
        error = ErrorRed,
        errorContainer = ErrorContainerDark
    )
    AccentTheme.SOFT_WHITE_GREEN -> darkColorScheme(
        primary = SoftGreenPrimaryDark,
        onPrimary = SoftGreenOnPrimaryDark,
        primaryContainer = SoftGreenPrimaryContainerDark,
        onPrimaryContainer = SoftGreenOnPrimaryContainerDark,
        secondary = SoftGreenSecondaryDark,
        onSecondary = SoftGreenOnPrimaryDark,
        secondaryContainer = SoftGreenSecondaryContainerDark,
        onSecondaryContainer = SoftGreenOnPrimaryContainerDark,
        tertiary = SoftGreenPrimaryDark,
        onTertiary = SoftGreenOnPrimaryDark,
        tertiaryContainer = SoftGreenPrimaryContainerDark,
        onTertiaryContainer = SoftGreenOnPrimaryContainerDark,
        background = SoftGreenBackgroundDark,
        onBackground = DarkTextPrimary,
        surface = SoftGreenSurfaceDark,
        onSurface = DarkTextPrimary,
        surfaceVariant = SoftGreenSurfaceVariantDark,
        onSurfaceVariant = DarkTextSecondary,
        surfaceTint = SoftGreenPrimaryDark,
        surfaceBright = SoftGreenSurfaceBrightDark,
        surfaceDim = SoftGreenSurfaceDimDark,
        surfaceContainerLowest = SoftGreenContainerLowestDark,
        surfaceContainerLow = SoftGreenContainerLowDark,
        surfaceContainer = SoftGreenContainerDark,
        surfaceContainerHigh = SoftGreenContainerHighDark,
        surfaceContainerHighest = SoftGreenContainerHighestDark,
        outline = SoftGreenOutlineDark,
        outlineVariant = SoftGreenOutlineVariantDark,
        error = ErrorRed,
        errorContainer = ErrorContainerDark
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun MyCareTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accentTheme: AccentTheme = AccentTheme.SOFT_WHITE_GREEN,
    fontScale: Float = 1.0f,
    isBangla: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (isDark) buildDarkColorScheme(accentTheme) else buildLightColorScheme(accentTheme)
    val typography = buildMyCareTypography(isBangla = isBangla, scale = fontScale)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = MyCareShapes,
        content = content
    )
}
