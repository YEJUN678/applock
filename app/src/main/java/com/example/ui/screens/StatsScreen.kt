package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LockConfig
import com.example.model.IntruderLog
import com.example.ui.theme.OneUi
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 7일 통계 화면.
 * 잠금 해제 추이 · 침입 시도 · 보호 기능 상태를 한 화면에서 보여 준다.
 */
@Composable
fun StatsScreen(
    lockConfig: LockConfig,
    sessionLog: List<AppSession>,
    intruderLogs: List<IntruderLog>,
    lockedAppCount: Int,
    totalAppCount: Int,
    onBack: () -> Unit,
    lockReasons: List<Pair<String, String>> = emptyList(),
    onExportReport: (String?, Boolean) -> Unit = { _, _ -> }
) {
    val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
    val labelFormat = SimpleDateFormat("EEEEE", Locale.KOREA)
    val today = Calendar.getInstance()
    val days = (6 downTo 0).map { back ->
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -back)
        }
        val key = dayFormat.format(cal.time)
        Triple(
            labelFormat.format(cal.time),
            key,
            sessionLog.count { dayFormat.format(Date(it.timestamp)) == key }
        )
    }
    val peak = (days.maxOfOrNull { it.third } ?: 0).coerceAtLeast(1)
    val weekTotal = days.sumOf { it.third }
    val intruderToday = intruderLogs.count { dayFormat.format(Date(it.timestamp)) == dayFormat.format(Date()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = OneUi.ScreenPadding, end = OneUi.ScreenPadding, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(OneUi.CardSpacing)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OneUi.RowSurface)
                        .clickable2 { onBack() },
                    contentAlignment = Alignment.Center
                ) { Text("‹", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("통계", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("최근 7일 기준", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // 7일 잠금 해제 추이
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OneUi.CardRadius))
                    .background(OneUi.CardSurface)
                    .padding(OneUi.CardPadding)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("잠금 해제", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$weekTotal 회", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("하루 평균 ${weekTotal / 7}회", color = TextSecondary, fontSize = 11.sp)
                    }
                    Text("피크 ${peak}회", color = OneUi.AccentTint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                ) {
                    days.forEach { (label, _, count) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$count", color = TextSecondary, fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            val fraction = count.toFloat() / peak
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((96f * fraction).dp.coerceAtLeast(4.dp))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        if (count == peak && count > 0) OneUi.AccentTint
                                        else OneUi.tintAlpha(OneUi.AccentTint, 0.35f)
                                    )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(label, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 숫자 요약
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(OneUi.CardSpacing), modifier = Modifier.fillMaxWidth()) {
                StatTile("오늘 침입 시도", "$intruderToday", Icons.Default.Psychology, OneUi.DangerTint, Modifier.weight(1f))
                StatTile("잠긴 앱", "$lockedAppCount", null, OneUi.AccentTint, Modifier.weight(1f))
                StatTile("누적 기록", "${sessionLog.size}", null, OneUi.InfoTint, Modifier.weight(1f))
            }
        }

        // 자주 해제하는 앱
        val topApps = sessionLog.groupingBy { it.appName.ifBlank { it.packageName } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
        if (topApps.isNotEmpty()) {
            item { Text("가장 많이 연 앱", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp)) }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(OneUi.CardRadius))
                        .background(OneUi.CardSurface)
                        .padding(vertical = 8.dp)
                ) {
                    topApps.forEach { (name, count) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(Modifier.size(8.dp).background(OneUi.AccentTint, CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Text(name, color = TextPrimary, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Text("${count}회", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 잠근 이유 통계: 숫자 나열이 아니라 문장으로 말한다.
        if (lockReasons.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(OneUi.CardRadius))
                        .background(OneUi.CardSurface)
                        .padding(OneUi.CardPadding)
                ) {
                    Text("잠근 이유", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    lockReasons.forEach { (appName, reason) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Box(Modifier.size(6.dp).background(OneUi.AccentTint, CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Text(appName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Spacer(Modifier.width(8.dp))
                            Text(reason, color = TextSecondary, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 리포트 내보내기
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OneUi.CardRadius))
                    .background(OneUi.CardSurface)
                    .padding(OneUi.CardPadding)
            ) {
                Text("리포트 내보내기", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "최근 7일 잠금 해제 기록을 이미지로 만듭니다. 기기 안의 기록만 쓰므로 API 비용이 없습니다.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
                androidx.compose.material3.Button(
                    onClick = { onExportReport(null, false) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = OneUi.AccentTint,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("이미지 리포트 만들기", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { onExportReport(null, true) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp), tint = OneUi.InfoTint)
                        Spacer(Modifier.width(6.dp))
                        Text("AI 요약 포함", fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }

        // 보호 기능 요약
        item { Text("보호 기능", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp)) }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OneUi.CardRadius))
                    .background(OneUi.CardSurface)
                    .padding(vertical = 6.dp)
            ) {
                StatRow("생체 인증", lockConfig.biometricEnabled)
                StatRow("AI 가드", lockConfig.isAiGuardEnabled)
                StatRow("잠긴 앱 알림 숨김", lockConfig.isNotificationPrivacyEnabled)
                StatRow("화면 꺼짐 잠금", lockConfig.isScreenOffLockEnabled)
                StatRow("뒤집기 보호", false)
                StatRow("가짜 오류 화면", lockConfig.isFakeCrashEnabled)
                StatRow("긴급 흔들기", lockConfig.isPanicShakeEnabled)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .padding(14.dp)
    ) {
        if (icon != null) {
            Box(Modifier.size(28.dp).background(OneUi.tintAlpha(tint), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(value, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun StatRow(label: String, enabled: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Box(
            Modifier.size(24.dp).background(OneUi.tintAlpha(if (enabled) OneUi.OkTint else OneUi.Divider), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) { Text(if (enabled) "✓" else "–", color = if (enabled) OneUi.OkTint else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 클릭 가능한 Box 를 위한 작은 헬퍼(BackHandler 없이 뒤로가기 제공). */
private fun Modifier.clickable2(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)