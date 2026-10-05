package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LockConfig
import com.example.ui.theme.OneUi
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 홈 대시보드.
 * "지금 안전 한가?" 를 한 화면에서 보여 주는 것이 목적이다.
 */
@Composable
fun HomeDashboardScreen(
    lockConfig: LockConfig,
    lockedAppCount: Int,
    totalAppCount: Int,
    hasAccessibilityPermission: Boolean,
    hasOverlayPermission: Boolean,
    hasUsageStatsPermission: Boolean,
    sessionLog: List<AppSession>,
    isLostModeActive: Boolean,
    onOpenPermissionWizard: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val requiredReady = hasAccessibilityPermission && hasOverlayPermission
    val optionalCount = listOf(hasUsageStatsPermission, lockConfig.isNotificationPrivacyEnabled, lockConfig.isScreenOffLockEnabled)
        .count { it }
    // 보안 점수: 필수 권한 2개 + 선택 보호 3개 + 잠금 앱 수 가중치
    val score = remember(requiredReady, optionalCount, lockedAppCount, totalAppCount) {
        computeSecurityScore(requiredReady, optionalCount, lockedAppCount, totalAppCount)
    }

    val todayCount = remember(sessionLog) { countToday(sessionLog) }
    val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
    val recent = sessionLog.take(5)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = OneUi.ScreenPadding, end = OneUi.ScreenPadding, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(OneUi.CardSpacing)
    ) {
        item {
            Text("홈", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = timeFormat.format(Date()),
                color = OneUi.AccentTint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 보안 점수 카드
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OneUi.CardRadius))
                    .background(OneUi.CardSurface)
                    .padding(OneUi.CardPadding)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("보안 상태", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when {
                                !requiredReady -> "권한을 마무리해 주세요"
                                score >= 85 -> "잘 보호되고 있습니다"
                                score >= 60 -> "조금 더 설정하면 좋습니다"
                                else -> "보호 수준이 낮습니다"
                            },
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    ScoreRing(score = score, ready = requiredReady)
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { score / 100f },
                    color = if (requiredReady) OneUi.OkTint else OneUi.DangerTint,
                    trackColor = OneUi.Divider,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!requiredReady) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(OneUi.tintAlpha(OneUi.DangerTint, 0.18f))
                            .clickable(onClick = onOpenPermissionWizard)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = OneUi.DangerTint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("권한 설정 도우미 열기", color = OneUi.DangerTint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 숫자 요약
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(OneUi.CardSpacing), modifier = Modifier.fillMaxWidth()) {
                MetricTile("잠긴 앱", "$lockedAppCount", "전체 ${totalAppCount}개", Icons.Default.Lock, OneUi.AccentTint, Modifier.weight(1f))
                MetricTile("오늘 해제", "$todayCount", "회", Icons.Default.CheckCircle, OneUi.OkTint, Modifier.weight(1f))
                MetricTile("추가 보호", "$optionalCount", "/ 3", Icons.Default.Security, OneUi.WarnTint, Modifier.weight(1f))
            }
        }

        // 빠른 실행
        item {
            Text("빠른 실행", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(OneUi.CardSpacing), modifier = Modifier.fillMaxWidth()) {
                QuickAction("금고 열기", Icons.Default.Lock, OneUi.OkTint, Modifier.weight(1f), onOpenVault)
                QuickAction("침입 기록", Icons.Default.Psychology, OneUi.DangerTint, Modifier.weight(1f), onOpenLogs)
                QuickAction("설정", Icons.Default.Shield, OneUi.AccentTint, Modifier.weight(1f), onOpenSettings)
            }
        }

        // 활성 보호 상태
        item { Text("보호 기능", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp)) }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OneUi.CardRadius))
                    .background(OneUi.CardSurface)
                    .padding(vertical = 6.dp)
            ) {
                ProtectionRow("접근성 서비스", hasAccessibilityPermission, "앱 실행 즉시 잠금")
                ProtectionRow("다른 앱 위에 표시", hasOverlayPermission, "최후 방어선")
                ProtectionRow("사용 정보 접근", hasUsageStatsPermission, "앱 실행 감지 보조")
                ProtectionRow("잠긴 앱 알림 숨김", lockConfig.isNotificationPrivacyEnabled, "알림 내용 보호")
                ProtectionRow("화면 꺼짐 잠금", lockConfig.isScreenOffLockEnabled, "화면 끄면 재잠금")
                ProtectionRow("Lost Mode", isLostModeActive, "분실 위치 기록")
                ProtectionRow("AI 가드", lockConfig.isAiGuardEnabled, "이상 입력 패턴 감지")
            }
        }

        // 최근 활동
        if (recent.isNotEmpty()) {
            item { Text("최근 잠금 해제", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp)) }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(OneUi.CardRadius))
                        .background(OneUi.CardSurface)
                        .padding(vertical = 6.dp)
                ) {
                    recent.forEachIndexed { index, session ->
                        if (index > 0) {
                            androidx.compose.material3.HorizontalDivider(color = OneUi.Divider.copy(alpha = 0.4f))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Box(Modifier.size(8.dp).background(OneUi.InfoTint, CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    session.appName.ifBlank { session.packageName },
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Text(session.method, color = TextSecondary, fontSize = 11.sp)
                            }
                            Text(dayFormat.format(Date(session.timestamp)), color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int, ready: Boolean) {
    val animated by animateFloatAsState(targetValue = score / 100f, label = "score")
    androidx.compose.foundation.Canvas(modifier = Modifier.size(72.dp)) {
        val stroke = 7.dp.toPx()
        val inset = stroke / 2
        drawArc(
            color = OneUi.Divider,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        drawArc(
            color = if (ready) OneUi.OkTint else OneUi.DangerTint,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Text("$score", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    suffix: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .padding(14.dp)
    ) {
        Box(Modifier.size(30.dp).background(OneUi.tintAlpha(tint), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(3.dp))
            Text(suffix, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
        Text(label, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(38.dp).background(OneUi.tintAlpha(tint), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun ProtectionRow(label: String, enabled: Boolean, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            Modifier.size(26.dp).background(OneUi.tintAlpha(if (enabled) OneUi.OkTint else OneUi.Divider), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (enabled) "✓" else "–", color = if (enabled) OneUi.OkTint else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

/** 보안 점수: 필수 권한 55 + 선택 보호 최대 30 + 잠금 비율 최대 15. */
private fun computeSecurityScore(requiredReady: Boolean, optional: Int, locked: Int, total: Int): Int {
    var base = 0
    if (requiredReady) base += 55
    base += optional * 10
    if (total > 0) base += ((locked.toFloat() / total) * 15).toInt().coerceAtMost(15)
    return base.coerceIn(0, 100)
}

private fun countToday(sessionLog: List<AppSession>): Int {
    val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
    val today = dayFormat.format(Date())
    return sessionLog.count { dayFormat.format(Date(it.timestamp)) == today }
}