package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LockAccent
import com.example.model.LockClockPosition
import com.example.model.LockClockStyle
import com.example.model.LockConfig
import com.example.model.LockFontStyle
import com.example.model.LockIconShape
import com.example.model.LockPreset
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreenEditor(
    config: LockConfig,
    onDismiss: () -> Unit,
    onSave: (LockConfig) -> Unit
) {
    // 프리셋을 먼저 고르고, 그 위에서 세부 값을 조정한다.
    var preset by remember { mutableStateOf(config.lockPreset) }
    var iconScale by remember { mutableFloatStateOf(config.lockIconScale) }
    var dim by remember { mutableFloatStateOf(config.lockBackgroundDim) }
    var blur by remember { mutableFloatStateOf(config.lockBackgroundBlur) }
    var panelAlpha by remember { mutableFloatStateOf(config.lockPanelAlpha) }
    var corner by remember { mutableFloatStateOf(config.lockCornerRadius) }
    var clockStyle by remember { mutableStateOf(config.lockClockStyle) }
    var clockPosition by remember { mutableStateOf(config.lockClockPosition) }
    var iconShape by remember { mutableStateOf(config.lockIconShape) }
    var fontStyle by remember { mutableStateOf(config.lockFontStyle) }
    var accent by remember { mutableStateOf(config.lockAccent) }
    var quickActions by remember { mutableStateOf(config.isLockQuickActionsEnabled) }

    fun applyPreset(next: LockPreset) {
        preset = next
        blur = next.blur
        panelAlpha = next.panelAlpha
        corner = next.corner
        clockPosition = next.clockPosition
        iconShape = next.iconShape
        fontStyle = next.fontStyle
    }

    val accentColor = Color(accent.hex)
    val previewFont = when (fontStyle) {
        LockFontStyle.SANS -> FontFamily.Default
        LockFontStyle.SERIF -> FontFamily.Serif
        LockFontStyle.MONO -> FontFamily.Monospace
    }
    val previewIconShape = when (iconShape) {
        LockIconShape.CIRCLE -> CircleShape
        LockIconShape.ROUNDED -> RoundedCornerShape((18 * iconScale).dp)
        LockIconShape.SQUARE -> RoundedCornerShape(6.dp)
    }
    val previewClockAlign = when (clockPosition) {
        LockClockPosition.TOP_CENTER -> Alignment.CenterHorizontally
        LockClockPosition.TOP_LEFT -> Alignment.Start
        LockClockPosition.TOP_RIGHT -> Alignment.End
    }
    val previewClockBoxAlign = when (clockPosition) {
        LockClockPosition.TOP_CENTER -> Alignment.TopCenter
        LockClockPosition.TOP_LEFT -> Alignment.TopStart
        LockClockPosition.TOP_RIGHT -> Alignment.TopEnd
    }

    // The sheet body scrolls so short screens never push the save row off-screen.
    val bodyMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.58f).dp

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = CyberSurfaceDark) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = bodyMaxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("잠금 화면 꾸미기", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("One UI 8.5 스타일 프리셋을 고른 뒤 세부를 조정하세요.", color = TextSecondary, fontSize = 13.sp)

                // 프리셋
                Text("스타일 프리셋", color = TextPrimary, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    LockPreset.entries.forEach { item ->
                        FilterChip(
                            selected = preset == item,
                            onClick = { applyPreset(item) },
                            label = { Text(item.title, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                Text(preset.description, color = TextSecondary, fontSize = 12.sp)

                // 미리보기
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(corner.dp))
                        .background(Color(0xFF101827))
                        .padding(18.dp)
                ) {
                    if (clockStyle != LockClockStyle.HIDDEN) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = previewClockBoxAlign) {
                            Column(horizontalAlignment = previewClockAlign) {
                                Text(
                                    SimpleDateFormat("HH:mm", Locale.KOREA).format(Date()),
                                    color = Color.White,
                                    fontSize = when (clockStyle) {
                                        LockClockStyle.LARGE -> if (clockPosition == LockClockPosition.TOP_CENTER) 44.sp else 28.sp
                                        else -> 20.sp
                                    },
                                    fontWeight = FontWeight.Light,
                                    fontFamily = previewFont
                                )
                                Text("10월 2일 목요일", color = Color.LightGray, fontSize = 11.sp, fontFamily = previewFont)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(corner.dp),
                        color = accentColor.copy(alpha = panelAlpha.coerceIn(0f, 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp)
                        ) {
                            Surface(shape = previewIconShape, color = Color(0x3300F0FF), border = androidx.compose.foundation.BorderStroke(2.dp, accentColor), modifier = Modifier.size((56 * iconScale).dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("●", color = Color.White, fontSize = (18 * iconScale).sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("앱 이름 잠금", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = previewFont)
                            Text("비밀번호를 입력하세요", color = Color.LightGray, fontSize = 11.sp, fontFamily = previewFont)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (quickActions) {
                        Text("◉     🔦", color = Color.LightGray, fontSize = 15.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    }
                }

                // 세부 옵션
                Text("시계 위치", color = TextPrimary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    LockClockPosition.entries.forEach { item ->
                        FilterChip(
                            selected = clockPosition == item,
                            onClick = { clockPosition = item },
                            label = { Text(item.title, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.Black)
                        )
                    }
                }

                Text("아이콘 모양", color = TextPrimary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    LockIconShape.entries.forEach { item ->
                        FilterChip(
                            selected = iconShape == item,
                            onClick = { iconShape = item },
                            label = { Text(item.title, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.Black)
                        )
                    }
                }

                Text("글꼴", color = TextPrimary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    LockFontStyle.entries.forEach { item ->
                        FilterChip(
                            selected = fontStyle == item,
                            onClick = { fontStyle = item },
                            label = { Text(item.title, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.Black)
                        )
                    }
                }

                Text("강조색", color = TextPrimary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    LockAccent.entries.forEach { item ->
                        FilterChip(
                            selected = accent == item,
                            onClick = { accent = item },
                            label = { Text(item.title, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(item.hex), selectedLabelColor = Color.Black)
                        )
                    }
                }

                Text("시계", color = TextPrimary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    LockClockStyle.entries.forEach { style ->
                        FilterChip(
                            selected = clockStyle == style,
                            onClick = { clockStyle = style },
                            label = { Text(style.title) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.Black)
                        )
                    }
                }

                Text("배경 흐림  ${"%.0f".format(blur * 100)}%", color = TextSecondary, fontSize = 12.sp)
                Slider(value = blur, onValueChange = { blur = it }, valueRange = 0f..1f)

                Text("패널 투명도  ${"%.0f".format(panelAlpha * 100)}%", color = TextSecondary, fontSize = 12.sp)
                Slider(value = panelAlpha, onValueChange = { panelAlpha = it }, valueRange = 0f..0.5f)

                Text("모서리 둥글기  ${corner.toInt()}dp", color = TextSecondary, fontSize = 12.sp)
                Slider(value = corner, onValueChange = { corner = it }, valueRange = 8f..40f)

                Text("아이콘 크기  ${"%.0f".format(iconScale * 100)}%", color = TextSecondary, fontSize = 12.sp)
                Slider(value = iconScale, onValueChange = { iconScale = it }, valueRange = .75f..1.35f)

                Text("배경 어두움  ${"%.0f".format(dim * 100)}%", color = TextSecondary, fontSize = 12.sp)
                Slider(value = dim, onValueChange = { dim = it }, valueRange = .45f.. .95f)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("하단 빠른 동작 표시", color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(checked = quickActions, onCheckedChange = { quickActions = it })
                }
            }

            Spacer(Modifier.height(16.dp))
            // Pinned below the scrolling body so the save action is always tappable.
            Button(
                onClick = {
                    onSave(
                        config.copy(
                            lockPreset = preset,
                            lockIconScale = iconScale,
                            lockBackgroundDim = dim,
                            lockBackgroundBlur = blur,
                            lockPanelAlpha = panelAlpha,
                            lockCornerRadius = corner,
                            lockClockStyle = clockStyle,
                            lockClockPosition = clockPosition,
                            lockIconShape = iconShape,
                            lockFontStyle = fontStyle,
                            lockAccent = accent,
                            isLockQuickActionsEnabled = quickActions
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) { Text("이 스타일 저장", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
        }
    }
}