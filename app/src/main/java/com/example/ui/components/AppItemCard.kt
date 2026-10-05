package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppLockPreferences
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonRed
import com.example.ui.theme.CyberSurfaceDark
import com.example.util.LockReasonStore
import com.example.util.ScreenBlockStore

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItemCard(
    app: AppItem,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onToggleLock: (Boolean) -> Unit,
    onTestLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var autoPrivacy by remember(app.packageName) { mutableStateOf(AppLockPreferences.isPrivacyShadeAutoEnabled(context, app.packageName)) }
    // 잠근 이유를 한 줄 메모로 남겨 둔다. 나중에 통계를 문장으로 말할 수 있게 된다.
    var reason by remember(app.packageName) { mutableStateOf(LockReasonStore.reasonOf(context, app.packageName)) }
    var showReasonEditor by remember(app.packageName) { mutableStateOf(false) }
    val blockedScreenCount = remember(app.packageName) { ScreenBlockStore.blockedScreens(context).count { it.packageName == app.packageName } }
    val borderColor = when {
        isSelected -> NeonCyan
        app.isLocked -> NeonCyan.copy(alpha = 0.4f)
        else -> CyberBorder
    }
    val cardBackground by animateColorAsState(
        targetValue = if (isSelected) Color(0x2200F0FF) else CyberCardDark,
        animationSpec = tween(220),
        label = "cardBg"
    )
    // 잠기면 살짝 커졌다 돌아온다. 상태 변화가 눈에 보여야 안 놓친다.
    val lockScale by animateFloatAsState(
        targetValue = if (app.isLocked) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "cardLock"
    )
    // 선택 모드에서는 모든 카드가 살짝 작아져 "지금 여러 개 고르는 중" 이라는 신호가 된다.
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelectionMode && !isSelected) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "cardSelect"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = lockScale * selectionScale
                scaleY = lockScale * selectionScale
            }
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectToggle()
                    } else {
                        onToggleLock(!app.isLocked)
                    }
                },
                onLongClick = {
                    onSelectToggle()
                }
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Checkbox when in selection mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NeonCyan,
                        checkmarkColor = Color.Black,
                        uncheckedColor = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // App Icon Graphic - renders real app icon drawable if available
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (app.isLocked) NeonCyan.copy(alpha = 0.15f) else Color(0xFF222F4C),
                modifier = Modifier.size(48.dp)
            ) {
                val appBitmap = remember(app.iconDrawable) {
                    app.iconDrawable?.let { drawableToBitmap(it) }
                }

                if (appBitmap != null) {
                    Image(
                        bitmap = appBitmap.asImageBitmap(),
                        contentDescription = app.name,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(36.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = app.name,
                        tint = if (app.isLocked) NeonCyan else TextSecondary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // App details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    if (app.isLocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "잠김",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${app.category} • ${app.packageName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            // 잠근 이유 메모 + 특정 화면 차단 수
                if (app.isLocked) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (blockedScreenCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "화면 ${blockedScreenCount}개",
                                    color = NeonRed,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonAmber.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { showReasonEditor = true }
                        ) {
                            Text(
                                text = if (reason.isBlank()) "잠근 이유" else reason,
                                color = if (reason.isBlank()) TextSecondary else NeonAmber,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 잠근 이유 편집 시트
                if (showReasonEditor) {
                    var draft by remember { mutableStateOf(reason) }
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showReasonEditor = false },
                        containerColor = CyberSurfaceDark,
                        title = { Text("${app.name} 을 잠근 이유", color = TextPrimary, fontSize = 16.sp) },
                        text = {
                            androidx.compose.material3.OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                placeholder = { Text("예: 개인 사진이 있어서", color = TextSecondary, fontSize = 13.sp) },
                                singleLine = true,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CyberBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = NeonCyan
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                reason = draft.trim()
                                LockReasonStore.setReason(context, app.packageName, draft.trim())
                                showReasonEditor = false
                            }) {
                                Text("저장", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            Row {
                                androidx.compose.material3.TextButton(onClick = {
                                    reason = ""
                                    LockReasonStore.setReason(context, app.packageName, "")
                                    showReasonEditor = false
                                }) { Text("삭제", color = NeonRed, fontSize = 12.sp) }
                                androidx.compose.material3.TextButton(onClick = { showReasonEditor = false }) {
                                    Text("취소", color = TextSecondary)
                                }
                            }
                        }
                    )
                }

                // In normal mode: Test launch button + Lock switch
            if (!isSelectionMode) {
                if (app.isLocked) {
                    IconButton(
                        onClick = { autoPrivacy = !autoPrivacy; AppLockPreferences.setPrivacyShadeAutoEnabled(context, app.packageName, autoPrivacy) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.VisibilityOff, "잠금 해제 후 사생활 필름 자동 실행", tint = if (autoPrivacy) NeonCyan else TextSecondary)
                    }
                    IconButton(
                        onClick = onTestLaunch,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "잠금 화면 테스트",
                            tint = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Switch(
                    checked = app.isLocked,
                    onCheckedChange = onToggleLock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0xFF0F172A)
                    )
                )
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (_: Throwable) {
        null
    }
}
