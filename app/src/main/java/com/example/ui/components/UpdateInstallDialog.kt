package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.AppUpdate

/** App-branded guidance shown before Android opens its non-customizable package installer. */
@Composable
fun UpdateInstallDialog(
    update: AppUpdate?,
    installedVersion: String,
    isDownloading: Boolean,
    downloadProgress: Int?,
    downloadedApkUri: Uri?,
    onDismiss: () -> Unit,
    onDownload: (AppUpdate) -> Unit,
    onBackUp: () -> Unit,
    onInstall: (Uri) -> Unit
) {
    val currentUpdate = update ?: return
    val isReady = downloadedApkUri != null
    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (isReady) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(when { isReady -> "설치 준비가 끝났어요"; isDownloading -> "안전하게 다운로드 중"; else -> "새 업데이트 발견" }, fontWeight = FontWeight.ExtraBold)
                    Text("v$installedVersion  →  v${currentUpdate.versionName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                UpdateSteps(isDownloading = isDownloading, isReady = isReady, progress = downloadProgress)
                HorizontalDivider()
                when {
                    isDownloading -> {
                        Text("파일을 받는 중입니다${downloadProgress?.let { " · $it%" } ?: ""}.")
                        LinearProgressIndicator(
                            progress = (downloadProgress ?: 0) / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("앱을 닫아도 Android 다운로드 알림에서 진행 상황을 확인할 수 있어요.", style = MaterialTheme.typography.bodySmall)
                    }
                    isReady -> {
                        Text("파일 검사가 끝났습니다. 다음 단계에서 Android의 공식 설치 확인 화면을 엽니다.")
                        SafetyNote()
                    }
                    else -> {
                        Text("현재 데이터와 설정은 같은 서명으로 설치하면 그대로 유지됩니다.")
                        if (currentUpdate.notes.isNotBlank()) {
                            HorizontalDivider()
                            Text("이번 업데이트", fontWeight = FontWeight.SemiBold)
                            Text(currentUpdate.notes, style = MaterialTheme.typography.bodySmall)
                        }
                        SafetyNote()
                    }
                }
            }
        },
        confirmButton = {
            when {
                isReady -> Button(onClick = { onInstall(downloadedApkUri!!) }) { Text("다음: Android에서 설치 확인") }
                isDownloading -> Button(onClick = {}, enabled = false) { Text("다운로드 중…") }
                else -> Button(onClick = { onDownload(currentUpdate) }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  업데이트 받기")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onBackUp, enabled = !isDownloading) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  백업")
                }
                TextButton(onClick = onDismiss, enabled = !isDownloading) { Text("나중에") }
            }
        }
    )
}

@Composable
private fun UpdateSteps(isDownloading: Boolean, isReady: Boolean, progress: Int?) {
    val downloadColor = if (isDownloading || isReady) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        UpdateStep("1", "업데이트 정보 확인", true, MaterialTheme.colorScheme.tertiary)
        UpdateStep("2", if (isDownloading) "파일 다운로드 ${progress?.let { "($it%)" } ?: ""}" else "업데이트 파일 다운로드", isDownloading || isReady, downloadColor)
        UpdateStep("3", "Android 설치 확인", isReady, MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun UpdateStep(number: String, label: String, complete: Boolean, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(number, color = if (complete) color else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text("  $label", color = if (complete) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SafetyNote() {
    Text(
        text = "설치 전에 백업을 한 번 만들어 두면 언제든 복원할 수 있어요.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
}
