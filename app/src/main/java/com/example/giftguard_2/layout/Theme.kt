package com.example.giftguard.layout

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light Theme Color Scheme (AppThemeColors 기반)
private val LightColorScheme = lightColorScheme(
    primary = AppThemeColors.PrimaryDefault,
    onPrimary = AppThemeColors.White,
    primaryContainer = AppThemeColors.PrimaryLight,
    onPrimaryContainer = AppThemeColors.PrimaryDark,
    secondary = AppThemeColors.Accent,
    onSecondary = AppThemeColors.White,
    tertiary = AppThemeColors.Gray600,
    background = AppThemeColors.White,
    surface = AppThemeColors.White,
    error = AppThemeColors.ErrorRed,
    onError = AppThemeColors.White
    /* Other default colors from lightColorScheme */
)

// Dark Theme Color Scheme (간소화)
private val DarkColorScheme = darkColorScheme(
    primary = AppThemeColors.PrimaryLight,
    onPrimary = AppThemeColors.PrimaryDark,
    secondary = AppThemeColors.Accent,
    tertiary = AppThemeColors.Gray300,
    background = AppThemeColors.Gray900,
    surface = AppThemeColors.Gray900,
    error = AppThemeColors.ErrorRed,
    onError = AppThemeColors.White
    /* Other default colors from darkColorScheme */
)

@Composable
fun GiftGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true, // 동적 색상 사용 여부
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

    // 상태 표시줄(Status Bar) 색상 및 아이콘 설정
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 상태 표시줄 색상을 PrimaryDefault로 설정
            window.statusBarColor = colorScheme.primary.toArgb()
            // 상태 표시줄 아이콘 색상을 설정합니다. (다크 모드일 때 라이트 아이콘)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // layout/Type.kt에서 정의
        content = content
    )
}
