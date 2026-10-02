package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LockAccent
import com.example.model.LockClockStyle
import com.example.model.LockConfig
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockScreenEditor(
    config: LockConfig,
    onDismiss: () -> Unit,
    onSave: (LockConfig) -> Unit
) {
    var iconScale by remember { mutableFloatStateOf(config.lockIconScale) }
    var dim by remember { mutableFloatStateOf(config.lockBackgroundDim) }
    var clockStyle by remember { mutableStateOf(config.lockClockStyle) }
    var accent by remember { mutableStateOf(config.lockAccent) }
    var quickActions by remember { mutableStateOf(config.isLockQuickActionsEnabled) }
    val accentColor = Color(accent.hex)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = CyberSurfaceDark) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("잠금 화면 편집기", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text("미리보기에서 스타일을 조합하고 저장하세요.", color = TextSecondary, fontSize = 13.sp)
            Column(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF101827)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("보호 모드", color = accentColor, fontSize = 12.sp)
                Spacer(Modifier.height(28.dp))
                if (clockStyle != LockClockStyle.HIDDEN) Text(SimpleDateFormat("HH:mm", Locale.KOREA).format(Date()), color = Color.White, fontSize = if (clockStyle == LockClockStyle.LARGE) 54.sp else 30.sp, fontWeight = FontWeight.Light)
                Text("10월 2일 목요일", color = Color.LightGray, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("●", color = accentColor, fontSize = (34 * iconScale).sp)
                if (quickActions) Text("◉     🔦", color = Color.LightGray, fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            }
            Text("시계", color = TextPrimary, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { LockClockStyle.entries.forEach { style -> FilterChip(selected = clockStyle == style, onClick = { clockStyle = style }, label = { Text(style.title) }, modifier = Modifier.weight(1f), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.Black)) } }
            Text("강조색", color = TextPrimary, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { LockAccent.entries.forEach { item -> FilterChip(selected = accent == item, onClick = { accent = item }, label = { Text(item.title, fontSize = 11.sp) }, modifier = Modifier.weight(1f), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(item.hex), selectedLabelColor = Color.Black)) } }
            Text("아이콘 크기  ${"%.0f".format(iconScale * 100)}%", color = TextSecondary, fontSize = 12.sp)
            Slider(value = iconScale, onValueChange = { iconScale = it }, valueRange = .75f..1.35f)
            Text("배경 어두움  ${"%.0f".format(dim * 100)}%", color = TextSecondary, fontSize = 12.sp)
            Slider(value = dim, onValueChange = { dim = it }, valueRange = .45f.. .95f)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text("하단 빠른 동작 표시", color = TextPrimary, modifier = Modifier.weight(1f)); Switch(checked = quickActions, onCheckedChange = { quickActions = it }) }
            Button(onClick = { onSave(config.copy(lockIconScale = iconScale, lockBackgroundDim = dim, lockClockStyle = clockStyle, lockAccent = accent, isLockQuickActionsEnabled = quickActions)) }, colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black), modifier = Modifier.fillMaxWidth()) { Text("이 스타일 저장", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}
