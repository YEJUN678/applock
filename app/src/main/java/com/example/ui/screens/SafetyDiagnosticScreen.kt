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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.OneUi
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SecurityAudit

/**
 * 안전 진단 화면.
 *
 * 권한 스위치가 켜져 있어도 실제로 통하지 않는 경우가 있다.
 * (알림 접근은 시스템에서 꺼지는데 앱은 모른다)
 * 그래서 "켜짐 표시"가 아니라 "실제로 동작하는가"를 확인한다.
 */
@Composable
fun SafetyDiagnosticScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var checks by remember { mutableStateOf<List<SecurityAudit.Check>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // 다시 확인할 때 권한 상태를 새로 읽는다.
    fun refresh() {
        checks = SecurityAudit.run(context)
        loading = false
    }
    LaunchedEffect(Unit) { refresh() }

    val score = SecurityAudit.scoreOf(checks)
    val failed = checks.count { !it.ok }

    Column(Modifier.fillMaxSize().background(CyberBgDark)) {
        // 고정 헤더
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(OneUi.HeaderSurface)
                .padding(horizontal = OneUi.ScreenPadding, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OneUi.RowSurface)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = TextPrimary, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("안전 진단", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (loading) "확인 중..." else if (failed == 0) "모든 항목이 정상입니다" else "확인이 필요한 항목 ${failed}개",
                    color = if (failed == 0) OneUi.OkTint else TextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "다시 확인", tint = OneUi.AccentTint)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(OneUi.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(OneUi.CardSpacing)
        ) {
            item { ScoreCard(score = score, failed = failed) }

            item {
                Text(
                    "아래는 지금 이 기기에서 실제로 동작하는지 확인한 결과입니다. 권한 화면을 켠 뒤 돌아오면 새로 확인합니다.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            items(checks.size) { index ->
                val check = checks[index]
                AuditRow(
                    title = check.title,
                    ok = check.ok,
                    detail = check.detail,
                    onFix = {
                        SecurityAudit.fixIntentFor(context, check.title)?.let { intent ->
                            runCatching { context.startActivity(intent) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ScoreCard(score: Int, failed: Int) {
    val tint = when {
        score >= 90 -> OneUi.OkTint
        score >= 60 -> OneUi.WarnTint
        else -> OneUi.DangerTint
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .padding(OneUi.CardPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("보안 점수", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        score >= 90 -> "잘 보호되고 있습니다"
                        score >= 60 -> "몇 가지를 더 확인해 주세요"
                        else -> "보호가 충분하지 않습니다"
                    },
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("$score", color = tint, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text("점", color = TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(OneUi.Divider)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(tint)
            )
        }
    }
}

@Composable
private fun AuditRow(title: String, ok: Boolean, detail: String, onFix: () -> Unit) {
    val tint = if (ok) OneUi.OkTint else OneUi.DangerTint
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .padding(OneUi.CardPadding)
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(OneUi.tintAlpha(tint, 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(detail, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (!ok) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OneUi.tintAlpha(OneUi.AccentTint, 0.18f))
                    .clickable(onClick = onFix)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("고치기", color = OneUi.AccentTint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}