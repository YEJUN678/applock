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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OneUi
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.LockReasonStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 잠근 사유 모아 보기.
 *
 * 앱 카드에서 한 줄씩 적었던 메모를 한곳에서 보고 고칠 수 있게 한다.
 * 여기서 모은 이유는 통계 화면과 잠금 해제 리포트(AI 요약 포함)의 근거로 쓰인다.
 */
@Composable
fun LockReasonScreen(
    onBack: () -> Unit,
    onUpdate: (String, String) -> Unit
) {
    val context = LocalContext.current
    val refreshKey = remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val rows = remember(refreshKey.value) {
        LockReasonStore.reasonsWithApps(context).map { (pkg, reason) ->
            Triple(pkg, appLabelOf(context, pkg), reason)
        }.sortedBy { it.second }
    }
    val staleCount = remember(refreshKey.value) { LockReasonStore.staleReasons(context).size }
    var filterQuery by remember { mutableStateOf("") }

    val visible = if (filterQuery.isBlank()) rows
    else rows.filter { it.second.contains(filterQuery, true) || it.third.contains(filterQuery, true) }

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
                Text("잠근 사유", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (rows.isEmpty()) "아직 사유를 남긴 앱이 없습니다" else "${rows.size}개 앱",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (rows.isEmpty()) {
            EmptyReasons()
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(OneUi.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "여기 남긴 이유는 통계 화면과 잠금 해제 리포트의 근거로 쓰입니다. 잠금을 해제하면 사유도 사라집니다.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            // 잠금을 풀고 남은 사유가 있으면 알려 준다(기록의 의미를 잃은 것).
            if (staleCount > 0) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(OneUi.tintAlpha(NeonAmber, 0.14f))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.StickyNote2, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "잠금을 해제해 사유만 남은 앱이 ${staleCount}개 있습니다. 목록에 안 보입니다.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                androidx.compose.material3.OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    singleLine = true,
                    placeholder = { Text("사유 검색", color = TextSecondary, fontSize = 13.sp) },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OneUi.AccentTint,
                        unfocusedBorderColor = OneUi.Divider,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = OneUi.AccentTint
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (visible.isEmpty()) {
                item { Text("일치하는 사유가 없습니다.", color = TextSecondary, fontSize = 13.sp) }
            }

            items(visible.size) { index ->
                val row = visible[index]
                ReasonRow(
                    appName = row.second,
                    reason = row.third,
                    onClick = { editing = row }
                )
            }
        }
    }

    // 편집 창
    editing?.let { target ->
        var draft by remember { mutableStateOf(target.third) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = CyberSurfaceDark,
            title = { Text("${target.second} 을 잠근 이유", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("예: 개인 사진이 있어서", color = TextSecondary, fontSize = 13.sp) },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = OneUi.Divider,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onUpdate(target.first, draft.trim())
                    editing = null
                    refreshKey.value++
                }) { Text("저장", color = NeonCyan, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Row {
                    androidx.compose.material3.TextButton(onClick = {
                        onUpdate(target.first, "")
                        editing = null
                        refreshKey.value++
                    }) { Text("삭제", color = OneUi.DangerTint, fontSize = 12.sp) }
                    androidx.compose.material3.TextButton(onClick = { editing = null }) {
                        Text("취소", color = TextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun ReasonRow(appName: String, reason: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .clickable(onClick = onClick)
            .padding(OneUi.CardPadding)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NeonAmber)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(appName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Text(reason, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.Edit, contentDescription = "수정", tint = TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun EmptyReasons() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.StickyNote2, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(12.dp))
        Text("아직 사유를 남긴 앱이 없습니다", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "잠긴 앱 카드에 있는 '잠근 이유' 칩을 눌러\n왜 잠갔는지 한 줄을 남겨 보세요.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

private fun appLabelOf(context: android.content.Context, packageName: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
}.getOrDefault(packageName)