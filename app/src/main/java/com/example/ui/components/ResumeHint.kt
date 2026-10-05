package com.example.ui.components

/**
 * 잠금 화면에 보여 줄 복귀 지점.
 *
 * 잠그기 전에 보고 있던 화면(앱 안 Activity + 제목)을 기억해 두고,
 * 잠금 화면에서 "방금 하던 화면으로" 버튼으로 되돌아간다.
 */
data class ResumeHint(
    val displayName: String,
    val className: String,
    val title: String
)