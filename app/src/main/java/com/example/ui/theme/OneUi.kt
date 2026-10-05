package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * One UI 8.5 계열 설정 화면용 디자인 토큰.
 *
 * 규칙: 카드 반경 24 · 카드 간격 12 · 행 최소 높이 64 · 여백 20.
 * 이 값들을 어긋나게 쓰지 않아야 화면 전체가 한 톤으로 보입니다.
 */
object OneUi {

    // 간격
    val ScreenPadding = 20.dp
    val CardSpacing = 12.dp
    val CardPadding = 20.dp
    val RowPadding = 16.dp
    val TightSpacing = 6.dp

    // 모양
    val CardRadius = 24.dp
    val RowRadius = 18.dp
    val SheetRadius = 28.dp
    val ChipRadius = 14.dp

    // 크기
    val RowMinHeight = 64.dp
    val IconTile = 42.dp
    val IconGlyph = 22.dp

    // 색 (기존 다크 시안 테마 위에 One UI 여백/라운드 구조를 얹는다)
    val Card = Color(0xFF162033)
    val CardSurface = CyberCardDark
    val RowSurface = Color(0xFF222D45)
    val Divider = Color(0xFF2E3D5B)
    val Pressed = Color(0xFF2A3550)

    // 상태색 (배지/아이콘 타일 배경에 사용)
    val OkTint = NeonGreen
    val WarnTint = NeonAmber
    val DangerTint = NeonRed
    val AccentTint = NeonCyan
    val InfoTint = NeonPurple

    fun tintAlpha(tint: Color, alpha: Float = 0.16f): Color = tint.copy(alpha = alpha)
}