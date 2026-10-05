package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.OneUi
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.NotificationInbox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 잠긴 앱 알림 모아 보기.
 *
 * 알림 하이라이트가 "다른 사람에게 안 보이게" 하는 기능이라면,
 * 여기는 소유자가 나중에 내용을 확인할 수 있는 자리다.
 * 알림을 dismiss 하면 사라지므로 따로 모아 두지 않으면 결국 못 봅니다.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NotificationInboxScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("M/d HH:mm", Locale.KOREA) }
    // 탭 전환(0 = 전체, 이후 앱별) 상태
    var selectedTab by remember { mutableIntStateOf(0) }

    val refreshKey = remember { mutableIntStateOf(0) }
    // (패키지명, 앱이름) 을 묶어서 돌린다. 길게 눌러 앱 단위로 지울 때 패키지명이 필요하다.
    val groups = remember(refreshKey.value) {
        NotificationInbox.all(context)
            .groupBy { it.packageName to it.appName }
            .toList()
            .sortedByDescending { pair -> pair.second.maxOfOrNull { it.receivedAt } ?: 0L }
            .map { (key, values) -> Triple(key.first, key.second, values.sortedByDescending { it.receivedAt }) }
    }
    val unread = remember(refreshKey.value) { NotificationInbox.unreadCount(context) }

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
                Text("알림 모아 보기", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (unread > 0) "안 읽은 알림 ${unread}건" else "모두 읽었습니다",
                    color = if (unread > 0) NeonCyan else TextSecondary,
                    fontSize = 12.sp
                )
            }
            if (groups.isNotEmpty()) {
                TextButton(onClick = {
                    NotificationInbox.markAllRead(context)
                    refreshKey.value++
                }) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = OneUi.OkTint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("모두 읽음", color = OneUi.OkTint, fontSize = 12.sp)
                }
            }
        }

        if (groups.isEmpty()) {
            EmptyInbox()
            return
        }

        // 앱별 탭: "카톡 12" 형태
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OneUi.HeaderSurface)
                .padding(horizontal = OneUi.ScreenPadding, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChipTab(
                label = "전체 ${groups.sumOf { it.third.size }}",
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )
            groups.forEachIndexed { index, group ->
                val appName = group.second
                val items = group.third
                ChipTab(
                    label = "$appName ${items.size}",
                    selected = selectedTab == index + 1,
                    onClick = { selectedTab = index + 1 },
                    onLongClick = {
                        // 길게 누르면 그 앱 알림을 한 번에 지운다.
                        val removed = NotificationInbox.clearApp(context, groups[index].first)
                        refreshKey.value++
                        selectedTab = 0
                        android.widget.Toast.makeText(context, "${appName} 알림 ${removed}건을 지웠습니다.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        val visible = if (selectedTab == 0) groups else listOf(groups[selectedTab - 1])

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(OneUi.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(OneUi.CardSpacing)
        ) {
        visible.forEach { group ->
            val appName = group.second
            val items = group.third
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Box(Modifier.size(6.dp).background(NeonCyan, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(appName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text("${items.size}건", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                items(items.size) { index ->
                    val item = items[index]
                    InboxRow(
                        title = item.title.ifBlank { "새 알림" },
                        text = item.text,
                        summary = item.summary,
                        time = timeFormat.format(Date(item.receivedAt)),
                        read = item.read,
                        onClick = {
                            NotificationInbox.remove(context, item.id)
                            refreshKey.value++
                        }
                    )
                }
            }
            item {
                Text(
                    "누르면 목록에서 지워집니다. 기기 안에만 저장되며 외부로 보내지 않습니다.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ChipTab(label: String, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) NeonCyan else OneUi.CardSurface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) androidx.compose.ui.graphics.Color.Black else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun InboxRow(
    title: String,
    text: String,
    summary: String?,
    time: String,
    read: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(if (read) OneUi.CardSurface else OneUi.tintAlpha(NeonCyan, 0.10f))
            .clickable(onClick = onClick)
            .padding(OneUi.CardPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (!read) {
                Box(Modifier.size(7.dp).background(NeonCyan, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            Text(time, color = TextSecondary, fontSize = 11.sp)
        }
        if (text.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(text, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 3)
        }
        // AI 요약이 붙었으면 원문 아래에 따로 보여 준다.
        if (!summary.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OneUi.tintAlpha(NeonGreen, 0.14f))
                    .padding(10.dp)
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(summary, color = TextPrimary, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun EmptyInbox() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(12.dp))
        Text("모인 알림이 없습니다", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "잠긴 앱에서 알림이 오면 하이라이트 카드로 뜨고,\n여기에 함께 모아 둡니다.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}