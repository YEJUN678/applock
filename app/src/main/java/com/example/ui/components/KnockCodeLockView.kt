package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun KnockCodeLockView(
    targetKnockCode: List<Int> = listOf(1, 2, 3, 4),
    isError: Boolean = false,
    enabled: Boolean = true,
    isVibrationEnabled: Boolean = true,
    onKnockCompleted: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val tappedSequence = remember { mutableStateListOf<Int>() }

    LaunchedEffect(isError) {
        if (isError) {
            tappedSequence.clear()
        }
    }

    fun triggerHaptic() {
        if (!isVibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (_: Exception) {}
    }

    fun handleQuadrantTap(quadrant: Int) {
        if (!enabled) return
        triggerHaptic()
        tappedSequence.add(quadrant)

        // If sequence reaches target length, auto-validate
        if (tappedSequence.size == targetKnockCode.size) {
            onKnockCompleted(tappedSequence.toList())
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        // Tapped indicators count
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            repeat(targetKnockCode.size.coerceAtLeast(4)) { index ->
                val isFilled = index < tappedSequence.size
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isError) NeonRed else if (isFilled) NeonCyan else Color(0x3300F0FF))
                        .border(
                            1.dp,
                            if (isError) NeonRed else if (isFilled) NeonCyan else CyberBorder,
                            CircleShape
                        )
                )
            }
        }

        // 2x2 Knock Quad Grid
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0x1A00F0FF),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (isError) NeonRed else CyberBorder),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Row: Quadrant 1 (Top-Left), Quadrant 2 (Top-Right)
                Row(modifier = Modifier.weight(1f)) {
                    QuadrantCell(
                        label = "1",
                        enabled = enabled,
                        onClick = { handleQuadrantTap(1) },
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, CyberBorder.copy(alpha = 0.4f))
                    )
                    QuadrantCell(
                        label = "2",
                        enabled = enabled,
                        onClick = { handleQuadrantTap(2) },
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, CyberBorder.copy(alpha = 0.4f))
                    )
                }
                // Bottom Row: Quadrant 3 (Bottom-Left), Quadrant 4 (Bottom-Right)
                Row(modifier = Modifier.weight(1f)) {
                    QuadrantCell(
                        label = "3",
                        enabled = enabled,
                        onClick = { handleQuadrantTap(3) },
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, CyberBorder.copy(alpha = 0.4f))
                    )
                    QuadrantCell(
                        label = "4",
                        enabled = enabled,
                        onClick = { handleQuadrantTap(4) },
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, CyberBorder.copy(alpha = 0.4f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset and Submit Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { tappedSequence.clear() },
                enabled = enabled && tappedSequence.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("초기화", color = TextSecondary, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    if (tappedSequence.isNotEmpty()) {
                        onKnockCompleted(tappedSequence.toList())
                    }
                },
                enabled = enabled && tappedSequence.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                modifier = Modifier.weight(1.5f)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("노크 입력 확인", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun QuadrantCell(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = enabled, onClick = onClick)
            .background(CyberCardDark.copy(alpha = 0.6f))
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0x2200F0FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
