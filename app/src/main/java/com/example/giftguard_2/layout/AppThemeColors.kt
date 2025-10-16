// File: layout/AppThemeColors.kt

package com.example.giftguard.layout

import androidx.compose.ui.graphics.Color

/**
 * 앱 전용 커스텀 색상을 정의합니다.
 * MaterialTheme.colorScheme에 포함되지 않은 특정 디자인 색상을 여기에 명시합니다.
 */
object AppThemeColors {
    // 주요 색상 계열 (Primary)
    val PrimaryDark = Color(0xFF004D40) // 진한 녹색 (Primary Dark)
    val PrimaryLight = Color(0xFFB2DFDB) // 연한 녹색 (Primary Light - TopBar 배경 등으로 사용)
    val PrimaryDefault = Color(0xFF00796B) // 기본 녹색

    // 보조/강조 색상 (Accent/Secondary)
    val Accent = Color(0xFFFF9800) // 오렌지색 (강조색)

    // 텍스트/배경 색상 계열 (Neutral/Gray)
    val Gray900 = Color(0xFF212121) // 가장 진한 회색 (거의 검은색)
    val Gray600 = Color(0xFF757575) // 중간 회색
    val Gray300 = Color(0xFFE0E0E0) // 연한 회색 (경계선, 배경 등)
    val White = Color(0xFFFFFFFF)

    // 상태 색상
    val ErrorRed = Color(0xFFD32F2F)
    val SuccessGreen = Color(0xFF388E3C)
}