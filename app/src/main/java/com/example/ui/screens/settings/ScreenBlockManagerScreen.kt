package com.example.ui.screens.settings

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.OneUi
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ScreenBlockStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "자세히 막기" 관리 화면.
 *
 * 앱 전체를 잠그는 대신, 앱 안의 특정 화면만 골라서 잠근다.
 * 목록에는 사용자가 실제로 본 화면만 담긴다(직접 관찰해서 모은 것이므로 없는 항목을 지어내지 않는다).
 */
@Composable
fun ScreenBlockManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // 다른 앱을 쓰다가 돌아올 때 목록이 갱신돼야 한다.
    val refreshKey = remember { mutableStateOf(0) }
    // 앱을 누르면 그 앱의 화면 목록으로 내려간다.
    var selectedApp by remember { mutableStateOf<String?>(null) }

    val apps = remember(refreshKey.value) { ScreenBlockStore.appsWithScreens(context) }
    val totalBlocked = remember(refreshKey.value) { ScreenBlockStore.blockedCount(context) }
    val accessibilityOn = remember(refreshKey.value) {
        AppLockPermissionHelperBridge.hasAccessibilityPermission(context)
    }

    Column(Modifier.fillMaxSize().background(com.example.ui.theme.CyberBgDark)) {
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
            ) { Text("‹", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("자세히 막기", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (totalBlocked > 0) "앱 ${apps.size}개 · 화면 ${totalBlocked}개 잠금" else "아직 막은 화면이 없습니다",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(OneUi.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(OneUi.CardSpacing)
        ) {
            item { HowItWorksCard() }

            if (!accessibilityOn) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(OneUi.CardRadius))
                            .background(OneUi.tintAlpha(OneUi.DangerTint, 0.16f))
                            .padding(OneUi.CardPadding)
                    ) {
                        Row {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = OneUi.DangerTint, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("접근성 권한이 필요합니다", color = OneUi.DangerTint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "앱이 어떤 화면을 보여 주는지 알아야 특정 화면만 막을 수 있습니다. 권한 없이는 목록이 비어 있습니다.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            if (apps.isEmpty()) {
                item { EmptyScreenCard() }
            } else {
                item { SectionLabelText("막을 수 있는 앱") }
                items(apps) { (packageName, appName) ->
                    val blockedInApp = remember(refreshKey.value, packageName) {
                        ScreenBlockStore.screensOf(context, packageName).count { it.blocked }
                    }
                    AppRow(
                        appName = appName.ifBlank { packageName },
                        screenCount = ScreenBlockStore.screensOf(context, packageName).size,
                        blockedCount = blockedInApp,
                        onClick = { selectedApp = packageName }
                    )
                }
                item {
                    Text(
                        "각 앱을 눌러 내부 화면을 고릅니다.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    // 앱을 고르면 그 앱의 화면 목록으로 들어간다.
    selectedApp?.let { pkg ->
        ScreenBlockSheet(
            packageName = pkg,
            onBack = { selectedApp = null; refreshKey.value++ },
            onToggle = { className, title, blocked ->
                ScreenBlockStore.setBlocked(context, pkg, className, title, blocked)
            },
            onToggleAll = { blocked ->
                ScreenBlockStore.setAllBlocked(context, pkg, blocked)
            },
            onRemove = { className, title ->
                ScreenBlockStore.remove(context, pkg, className, title)
            },
            onForgetApp = {
                ScreenBlockStore.forgetApp(context, pkg)
                selectedApp = null
                refreshKey.value++
            }
        )
    }
}

/** 한 앱 안의 화면 목록. */
@Composable
fun ScreenBlockSheet(
    packageName: String,
    onBack: () -> Unit,
    onToggle: (String, String, Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onRemove: (String, String) -> Unit,
    onForgetApp: () -> Unit
) {
    val context = LocalContext.current
    val refreshKey = remember { mutableStateOf(0) }
    val screens = remember(refreshKey.value, packageName) { ScreenBlockStore.screensOf(context, packageName) }
    val blockedCount = screens.count { it.blocked }
    val dateFormat = remember { SimpleDateFormat("M/d HH:mm", Locale.KOREA) }

    Column(Modifier.fillMaxSize().background(com.example.ui.theme.CyberBgDark)) {
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
                Text("화면 선택", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "화면 ${screens.size}개 · 잠금 ${blockedCount}개",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = { refreshKey.value++; onToggleAll(blockedCount != screens.size) }) {
                Text(if (blockedCount == screens.size) "모두 해제" else "모두 잠금", color = OneUi.AccentTint, fontSize = 12.sp)
            }
        }

        if (screens.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(12.dp))
                Text("아직 기록된 화면이 없습니다", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "이 앱을 잠금 해제하고 몇 개 화면을 둘러보세요. 본 화면이 목록에 쌓입니다.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(OneUi.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(OneUi.CardSpacing)
            ) {
                item { TitleWarningCard() }
                items(screens) { entry ->
                    ScreenRow(
                        name = entry.displayName,
                        className = entry.className,
                        title = entry.title,
                        seenAt = dateFormat.format(Date(entry.lastSeenAt)),
                        needsTitleMatch = entry.needsTitleMatch,
                        blocked = entry.blocked,
                        onToggle = {
                            onToggle(entry.className, entry.title, !entry.blocked)
                            refreshKey.value++
                        },
                        onRemove = {
                            onRemove(entry.className, entry.title)
                            refreshKey.value++
                        }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = onForgetApp,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("이 앱 기록 지우기", color = OneUi.DangerTint, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .padding(OneUi.CardPadding)
    ) {
        Text("이렇게 쓰입니다", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        listOf(
            "1. 아래 앱 중 하나를 누릅니다",
            "2. 그 앱을 잠금 없이 열어본 뒤 돌아오면 본 화면이 목록에 쌓입니다",
            "3. 막고 싶은 화면만 켭니다",
            "4. 그 화면으로 들어가면 잠금 화면이 뜹니다"
        ).forEach { line ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(OneUi.AccentTint).padding(top = 6.dp))
                Spacer(Modifier.width(10.dp))
                Text(line, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "앱 전체를 잠그지 않으므로 알림과 나머지 화면은 그대로 쓸 수 있습니다.",
            color = OneUi.InfoTint,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptyScreenCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .padding(OneUi.CardPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AccountTree, contentDescription = null, tint = OneUi.AccentTint, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(12.dp))
        Text("아직 기록된 화면이 없습니다", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "잠그지 않은 앱(카톡, 은행 등)을 평소대로 사용해 보세요.\n사용자가 직접 본 화면만 목록에 올라옵니다.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun TitleWarningCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OneUi.tintAlpha(OneUi.WarnTint, 0.13f))
            .padding(12.dp)
    ) {
        Text(
            "제목이 비어 있는 화면은 구분자가 없습니다. 이 경우 앱의 화면 구조가 바뀌면 막히지 않을 수 있습니다.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun AppRow(appName: String, screenCount: Int, blockedCount: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .clickable(onClick = onClick)
            .padding(OneUi.CardPadding)
    ) {
        Column(Modifier.weight(1f)) {
            Text(appName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    blockedCount > 0 -> "화면 ${screenCount}개 중 ${blockedCount}개 잠금"
                    screenCount > 0 -> "화면 ${screenCount}개 · 아직 선택 안 됨"
                    else -> "기록 없음"
                },
                color = if (blockedCount > 0) OneUi.DangerTint else TextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ScreenRow(
    name: String,
    className: String,
    title: String,
    seenAt: String,
    needsTitleMatch: Boolean,
    blocked: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(if (blocked) OneUi.tintAlpha(OneUi.DangerTint, 0.10f) else OneUi.CardSurface)
            .clickable(onClick = onToggle)
            .padding(OneUi.CardPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (needsTitleMatch) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Warning, contentDescription = "구분자 없음", tint = OneUi.WarnTint, modifier = Modifier.size(13.dp))
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    className.substringAfterLast('.'),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                if (title.isNotBlank()) {
                    Text("제목: $title", color = OneUi.InfoTint, fontSize = 11.sp, maxLines = 1)
                }
                Text("최근 본 시각 $seenAt", color = TextSecondary, fontSize = 10.sp)
            }
            Spacer(Modifier.width(10.dp))
            Switch(checked = blocked, onCheckedChange = { onToggle() })
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onRemove) {
            Text("기록에서 빼기", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SectionLabelText(text: String) {
    Text(text, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
}

/** 접근성 권한 확인(설정 모듈에서 직접 참조하지 않도록 가볍게 감싼다). */
private object AppLockPermissionHelperBridge {
    fun hasAccessibilityPermission(context: android.content.Context): Boolean =
        com.example.util.AppLockPermissionHelper.hasAccessibilityPermission(context)
}