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
    isDownloading: Boolean,
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
        icon = {
            Icon(
                imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isReady) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = when {
                    isReady -> "업데이트 준비 완료"
                    isDownloading -> "업데이트 받는 중"
                    else -> "새 버전 ${currentUpdate.versionName}"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    isDownloading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("  안전하게 업데이트 파일을 다운로드하고 있어요.")
                        }
                        Text("완료되면 여기서 바로 설치를 시작할 수 있습니다.", style = MaterialTheme.typography.bodySmall)
                    }
                    isReady -> {
                        Text("파일을 받았습니다. 아래 버튼을 누르면 Android의 공식 설치 확인 화면이 열립니다.")
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
                isReady -> Button(onClick = { onInstall(downloadedApkUri!!) }) { Text("Android 설치 화면 열기") }
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
private fun SafetyNote() {
    Text(
        text = "설치 전에 백업을 한 번 만들어 두면 언제든 복원할 수 있어요.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
}
