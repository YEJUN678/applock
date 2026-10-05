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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.OneUi
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DataStorageManager

/**
 * 저장 공간 관리 화면.
 *
 * 침입 증거는 계속 쌓이는데 얼마나 쓰는지 알 방법이 없었다.
 * 여기서 용량을 보고 정리한다.
 */
@Composable
fun StorageManagerScreen(
    onBack: () -> Unit,
    onPrune: (Int) -> Unit,
    onDeleteAll: () -> Unit
) {
    val context = LocalContext.current
    var usage by remember { mutableStateOf(DataStorageManager.usage(context)) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pruneSheet by remember { mutableStateOf(false) }

    fun reload() {
        usage = DataStorageManager.usage(context)
    }
    LaunchedEffect(Unit) { reload() }

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
                Text("저장 공간", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("전체 ${DataStorageManager.formatSize(usage.total)}", color = TextSecondary, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(OneUi.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(OneUi.CardSpacing)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(OneUi.CardRadius))
                        .background(OneUi.CardSurface)
                        .padding(OneUi.CardPadding)
                ) {
                    Text("전체 사용량", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        DataStorageManager.formatSize(usage.total),
                        color = TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(16.dp))
                    UsageBar(usage)
                }
            }

            item { Text("항목별", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp)) }

            item {
                UsageRow("침입 사진", usage.intruderPhotos, Icons.Default.PhotoCamera, OneUi.DangerTint)
            }
            item {
                UsageRow("침입 영상", usage.intruderVideos, Icons.Default.Videocam, OneUi.WarnTint)
            }
            item {
                UsageRow("AI 가드 음성", usage.aiGuardAudio, Icons.Default.Mic, OneUi.InfoTint)
            }
            item {
                UsageRow("파일 금고", usage.vault, Icons.Default.Folder, OneUi.OkTint)
            }
            if (usage.other > 0) {
                item {
                    UsageRow("기타", usage.other, Icons.Default.Folder, TextSecondary)
                }
            }

            item {
                Text(
                    "정리하면 시도 횟수와 시각 기록은 남고 파일만 사라집니다.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(OneUi.CardRadius))
                        .background(OneUi.CardSurface)
                        .clickable { pruneSheet = true }
                        .padding(OneUi.CardPadding)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = OneUi.WarnTint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("오래된 증거 지우기", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("7일 / 30일 / 90일 지난 것만 지웁니다", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(OneUi.CardRadius))
                        .background(OneUi.tintAlpha(OneUi.DangerTint, 0.10f))
                        .clickable { confirmDelete = true }
                        .padding(OneUi.CardPadding)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = OneUi.DangerTint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("증거 파일 모두 지우기", color = OneUi.DangerTint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("기록은 남고 파일만 지워집니다", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (pruneSheet) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pruneSheet = false },
            containerColor = com.example.ui.theme.CyberSurfaceDark,
            title = { Text("얼마 지난 증거를 지울까요?", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(7 to "7일 지난 것", 30 to "30일 지난 것", 90 to "90일 지난 것").forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onPrune(days)
                                    pruneSheet = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Text(label, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { pruneSheet = false }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }

    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = com.example.ui.theme.CyberSurfaceDark,
            title = { Text("증거 파일을 모두 지울까요?", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "사진 · 영상 · 음성 파일이 모두 삭제됩니다.\n시도 횟수와 시각 기록은 남습니다.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onDeleteAll()
                    confirmDelete = false
                }) { Text("삭제", color = OneUi.DangerTint, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }

    // 정리 후 화면에 반영한다.
    LaunchedEffect(usage.total) { reload() }
}

/** 항목별 비중을 막대로 보여 준다. */
@Composable
private fun UsageBar(usage: DataStorageManager.Usage) {
    val total = usage.total.coerceAtLeast(1L)
    val segments = listOf(
        Triple(usage.intruderPhotos, OneUi.DangerTint, "사진"),
        Triple(usage.intruderVideos, OneUi.WarnTint, "영상"),
        Triple(usage.aiGuardAudio, OneUi.InfoTint, "음성"),
        Triple(usage.vault, OneUi.OkTint, "금고"),
        Triple(usage.other, OneUi.Divider, "기타")
    ).filter { it.first > 0 }

    if (segments.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(OneUi.Divider)
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        segments.forEach { (bytes, tint, _) ->
            Box(
                modifier = Modifier
                    .weight(bytes.toFloat() / total)
                    .height(10.dp)
                    .background(tint)
            )
        }
    }
}

@Composable
private fun UsageRow(label: String, bytes: Long, icon: ImageVector, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .padding(OneUi.CardPadding)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(OneUi.tintAlpha(tint, 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(DataStorageManager.formatSize(bytes), color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}