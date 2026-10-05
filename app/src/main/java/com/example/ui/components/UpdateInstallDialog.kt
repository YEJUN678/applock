package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppUpdate

/**
 * 업데이트 안내 창 (One UI 계열).
 * 상태: 확인 → 다운로드 중 → 설치 준비 완료 를 하나의 창에서 단계별로 보여 준다.
 */
@Composable
fun UpdateInstallDialog(
    update: AppUpdate?,
    installedVersion: String,
    isDownloading: Boolean,
    downloadProgress: Int?,
    downloadedApkUri: android.net.Uri?,
    onDismiss: () -> Unit,
    onDownload: (AppUpdate) -> Unit,
    onBackUp: () -> Unit,
    onInstall: (android.net.Uri) -> Unit
) {
    val currentUpdate = update ?: return
    val isReady = downloadedApkUri != null
    val progress by animateFloatAsState(
        targetValue = (downloadProgress ?: 0) / 100f,
        animationSpec = tween(400),
        label = "download"
    )

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        containerColor = CyberSurfaceDark,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isReady) NeonGreen.copy(alpha = 0.25f) else NeonCyan.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isReady -> Icons.Default.CheckCircle
                            isDownloading -> Icons.Default.Download
                            else -> Icons.Default.NewReleases
                        },
                        contentDescription = null,
                        tint = if (isReady) NeonGreen else NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = when {
                            isReady -> "설치 준비가 끝났어요"
                            isDownloading -> "안전하게 다운로드 중"
                            else -> "새 업데이트가 있어요"
                        },
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "App Lock & Vault",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 버전 비교 배너
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x1A00E5FF))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    VersionChip("현재", installedVersion, TextSecondary, Modifier.weight(1f))
                    Icon(Icons.Default.NewReleases, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    VersionChip("새 버전", currentUpdate.versionName, NeonCyan, Modifier.weight(1f))
                }

                // 진행 단계
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CyberCardDark)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StepRow("01", "업데이트 정보 확인", done = true)
                    StepRow("02", "파일 다운로드${if (isDownloading && downloadProgress != null) "  $downloadProgress%" else ""}", done = isReady || isDownloading, active = isDownloading)
                    StepRow("03", "Android 설치 확인", done = isReady, active = isReady)
                    if (isDownloading) {
                        Spacer(Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            color = NeonCyan,
                            trackColor = Color(0x3300E5FF),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 릴리즈 노트
                if (!isDownloading && !isReady && currentUpdate.notes.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CyberCardDark)
                            .padding(14.dp)
                    ) {
                        Text("이번 릴리스", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = currentUpdate.notes,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // 안전 안내
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NeonAmber.copy(alpha = 0.10f))
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            isReady -> "설치하면 Android 의 공식 설치 화면이 열립니다. 같은 서명이면 데이터와 설정이 유지됩니다."
                            isDownloading -> "앱을 닫어도 Android 다운로드 알림에서 진행 상황을 확인할 수 있어요."
                            else -> "설치 전에 백업을 한 번 만들어 두면 언제든 되돌릴 수 있어요."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        isReady -> onInstall(downloadedApkUri!!)
                        isDownloading -> Unit
                        else -> onDownload(currentUpdate)
                    }
                },
                enabled = !isDownloading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isReady) NeonGreen else NeonCyan,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = when {
                        isReady -> "설치하러 가기"
                        isDownloading -> "다운로드 중…"
                        else -> "업데이트 받기"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onBackUp, enabled = !isDownloading) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("백업", fontSize = 13.sp)
                }
                TextButton(onClick = onDismiss, enabled = !isDownloading) {
                    Text("나중에", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    )
}

@Composable
private fun VersionChip(label: String, version: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = "v$version",
            color = tint,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StepRow(number: String, label: String, done: Boolean, active: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (done) NeonGreen else if (active) NeonCyan else Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (done) "✓" else number,
                color = if (done) Color.Black else if (active) Color.Black else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = if (done || active) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}