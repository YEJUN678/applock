package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppSchedule
import com.example.util.AppSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 잠금 제외 앱 관리 시트. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedAppsSheet(
    apps: List<AppItem>,
    excludedPackages: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sorted = remember(apps) { apps.sortedBy { it.name.lowercase(Locale.KOREA) } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberSurfaceDark
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("잠금 제외 앱", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "여기서 켠 앱은 잠금 화면이 뜨지 않습니다. 잠금 목록에 남아 있어도 완전히 제외됩니다.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            Text("${excludedPackages.size}개 앱 제외 중", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
            ) {
                items(sorted, key = { it.packageName }) { app ->
                    val excluded = excludedPackages.contains(app.packageName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberCardDark, RoundedCornerShape(12.dp))
                            .clickable { onToggle(app.packageName, !excluded) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp).background(Color(0x2200F0FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text(app.name.take(1), color = NeonCyan, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.name, color = TextPrimary, fontSize = 14.sp, maxLines = 1)
                            Text(app.packageName, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                        }
                        Switch(checked = excluded, onCheckedChange = { onToggle(app.packageName, it) })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) { Text("완료", fontWeight = FontWeight.Bold) }
        }
    }
}

/** 클립보드 자동 삭제 설정 카드. */
@Composable
fun ClipboardAutoClearCard(
    enabled: Boolean,
    seconds: Int,
    onEnabledChange: (Boolean) -> Unit,
    onSecondsChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberCardDark, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("클립보드 자동 삭제", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "비밀번호·주소 등을 복사하면 지정 시간 뒤 자동으로 지웁니다.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            if (enabled) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(30 to "30초", 60 to "1분", 180 to "3분", 300 to "5분").forEach { (value, label) ->
                        FilterChip(
                            selected = seconds == value,
                            onClick = { onSecondsChange(value) },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "안드로이드 10 이상은 백그라운드 클립보드 접근을 제한해 자동 삭제가 실패할 수 있습니다. 실패하면 알림으로 알려드립니다.",
                    color = NeonRed.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** 실패 횟수 제한(브루트포스 억제) 설정 카드. */
@Composable
fun FailedAttemptLimitCard(
    maxAttempts: Int,
    lockoutMinutes: Int,
    onMaxAttemptsChange: (Int) -> Unit,
    onLockoutMinutesChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberCardDark, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Text("실패 횟수 제한", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = "연속으로 실패하면 정해진 시간 동안 입력을 막아 누가 건드리는지 알 수 없게 만듭니다.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            Text("허용 실패 횟수", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(0 to "제한 없음", 3 to "3회", 5 to "5회", 10 to "10회").forEach { (value, label) ->
                    FilterChip(
                        selected = maxAttempts == value,
                        onClick = { onMaxAttemptsChange(value) },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
            if (maxAttempts > 0) {
                Spacer(Modifier.height(12.dp))
                Text("차단 시간", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(1 to "1분", 5 to "5분", 15 to "15분", 60 to "1시간").forEach { (value, label) ->
                        FilterChip(
                            selected = lockoutMinutes == value,
                            onClick = { onLockoutMinutesChange(value) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}

/** 앱별 잠금 일정 목록 + 편집 시트. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScheduleSheet(
    apps: List<AppItem>,
    schedules: Map<String, AppSchedule>,
    onSave: (String, AppSchedule?) -> Unit,
    onDismiss: () -> Unit
) {
    var editing by remember { mutableStateOf<AppItem?>(null) }
    val sorted = remember(apps) { apps.sortedBy { it.name.lowercase(Locale.KOREA) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberSurfaceDark
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("앱별 잠금 일정", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "지정한 요일과 시간 안에서만 그 앱을 잠급니다. 나머지 시간에는 저절로 열립니다.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
            ) {
                items(sorted, key = { it.packageName }) { app ->
                    val schedule = schedules[app.packageName]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberCardDark, RoundedCornerShape(12.dp))
                            .clickable { editing = app }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(app.name, color = TextPrimary, fontSize = 14.sp, maxLines = 1)
                            Text(
                                text = if (schedule == null) "일정 없음 (항상 잠금)"
                                else "${schedule.daysOfWeek.toSortedDays()} · ${formatMinute(schedule.startMinute)}~${formatMinute(schedule.endMinute)}",
                                color = if (schedule == null) TextSecondary else NeonGreen,
                                fontSize = 11.sp
                            )
                        }
                        Text("편집", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) { Text("완료", fontWeight = FontWeight.Bold) }
        }
    }

    editing?.let { target ->
        AppScheduleEditorDialog(
            app = target,
            current = schedules[target.packageName],
            onDismiss = { editing = null },
            onSave = { schedule ->
                onSave(target.packageName, schedule)
                editing = null
            }
        )
    }
}

private val DayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScheduleEditorDialog(
    app: AppItem,
    current: AppSchedule?,
    onDismiss: () -> Unit,
    onSave: (AppSchedule?) -> Unit
) {
    var days by remember { mutableStateOf(current?.daysOfWeek ?: setOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))) }
    var startMinute by remember { mutableStateOf(current?.startMinute ?: (9 * 60)) }
    var endMinute by remember { mutableStateOf(current?.endMinute ?: (18 * 60)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${app.name} 잠금 일정", color = TextPrimary) },
        text = {
            Column {
                Text("잠글 요일", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayLabels.forEachIndexed { index, label ->
                        val dayValue = index + 1
                        FilterChip(
                            selected = days.contains(dayValue),
                            onClick = {
                                days = if (days.contains(dayValue)) days - dayValue else days + dayValue
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("시작 · 종료 시간", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TimeFieldButton("시작", startMinute, onPick = { startMinute = it }, modifier = Modifier.weight(1f))
                    Text("~", color = TextSecondary)
                    TimeFieldButton("종료", endMinute, onPick = { endMinute = it }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (days.isEmpty()) "요일을 하나 이상 고르세요." else "이 시간에만 잠기고, 나머지 시간에는 자유롭게 열립니다.",
                    color = if (days.isEmpty()) NeonRed else TextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(if (days.isEmpty()) null else AppSchedule(days, startMinute, endMinute)) },
                enabled = days.isNotEmpty()
            ) { Text("저장", color = NeonCyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(null) }) { Text("일정 삭제", color = NeonRed, fontSize = 12.sp) }
                TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) }
            }
        },
        containerColor = CyberCardDark
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeFieldButton(label: String, minute: Int, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { showPicker = true },
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        modifier = modifier
    ) { Text("$label ${formatMinute(minute)}", fontSize = 13.sp) }

    if (showPicker) {
        val state = rememberTimePickerState(initialHour = minute / 60, initialMinute = minute % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("$label 시간", color = TextPrimary) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onPick(state.hour * 60 + state.minute)
                    showPicker = false
                }) { Text("확인", color = NeonCyan) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소", color = TextSecondary) } },
            containerColor = CyberCardDark
        )
    }
}

private fun formatMinute(minute: Int): String =
    "%02d:%02d".format(minute / 60, minute % 60)

private fun Set<Int>.toSortedDays(): String {
    if (isEmpty()) return "요일 없음"
    return sorted().joinToString(",") { DayLabels[(it - 1).coerceIn(0, 6)] }
}

/** 잠금 해제 실행 기록 타임라인. */
@Composable
fun SessionTimelineCard(
    sessions: List<AppSession>,
    onClear: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA) }
    val dayFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.KOREA) }
    val today = dayFormat.format(Date())
    val todayCount = remember(sessions) {
        sessions.count { dayFormat.format(Date(it.timestamp)) == today }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberCardDark, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("앱 실행 기록", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "오늘 잠금 해제 ${todayCount}회 · 최근 ${sessions.size}건 보관 (최대 100건)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (sessions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x22FF5252), RoundedCornerShape(10.dp))
                            .clickable(onClick = onClear)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("지우기", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (sessions.isEmpty()) {
                Text("아직 기록이 없습니다. 잠긴 앱을 인증해서 열면 여기에 남습니다.", color = TextSecondary, fontSize = 12.sp)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    sessions.take(50).forEachIndexed { index, session ->
                        if (index > 0) HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(NeonPurple, CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(session.appName.ifBlank { session.packageName }, color = TextPrimary, fontSize = 14.sp, maxLines = 1)
                                Text(session.method, color = TextSecondary, fontSize = 11.sp)
                            }
                            Text(formatter.format(Date(session.timestamp)), color = NeonCyan, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}