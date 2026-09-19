package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AppItemCard(
    app: AppItem,
    onToggleLock: (Boolean) -> Unit,
    onTestLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onToggleLock(!app.isLocked) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CyberCardDark
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (app.isLocked) NeonCyan.copy(alpha = 0.4f) else CyberBorder
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
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

            // Test launch button if locked
            if (app.isLocked) {
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

            // Lock Switch
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
