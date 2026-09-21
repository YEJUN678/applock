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
    val borderColor = when {
        isSelected -> NeonCyan
        app.isLocked -> NeonCyan.copy(alpha = 0.4f)
        else -> CyberBorder
    }
    val cardBackground = when {
        isSelected -> Color(0x2200F0FF)
        else -> CyberCardDark
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        ),
        modifier = modifier
            .fillMaxWidth()
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
