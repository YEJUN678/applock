package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LockChargingStyle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/** 충전 상태. */
data class ChargingState(val isCharging: Boolean, val levelPercent: Int = 0, val isFull: Boolean = false)

/**
 * 실제 충전 상태를 시스템에서 읽는다.
 * ACTION_POWER_CONNECTED / DISCONNECTED 방송과 배터리 수치를 함께 추적한다.
 */
@Composable
fun rememberChargingState(): State<ChargingState> {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    var state by remember { mutableStateOf(ChargingState(isCharging = false)) }

    DisposableEffect(preview) {
        if (preview) return@DisposableEffect onDispose { }

        fun readFrom(intent: Intent?): ChargingState {
            val batteryIntent = intent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: return ChargingState(false)
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
            val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val full = status == BatteryManager.BATTERY_STATUS_FULL
            return ChargingState(
                isCharging = plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING,
                levelPercent = percent,
                isFull = full
            )
        }

        state = readFrom(null)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                state = readFrom(intent)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        // API 33+ 는 플래그를 요구한다.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
    return remember { object : State<ChargingState> { override val value get() = state } }
}

/** 잠금 화면에 표시되는 충전 애니메이션. */
@Composable
fun ChargingAnimation(
    style: LockChargingStyle,
    state: ChargingState,
    modifier: Modifier = Modifier,
    accent: Color = NeonCyan
) {
    val transition = rememberInfiniteTransition(label = "charging")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state.isFull) 2400 else 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2600, easing = LinearEasing)),
        label = "spin"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (style) {
            LockChargingStyle.NONE -> Unit

            LockChargingStyle.PULSE -> {
                // 부드럽게 커졌다 줄어드는 에너지 블롭
                Canvas(Modifier.fillMaxSize()) {
                    val maxRadius = size.minDimension / 2f
                    listOf(0.35f, 0.6f, 0.85f).forEachIndexed { index, factor ->
                        val offset = ((pulse + index / 3f) % 1f)
                        val radius = maxRadius * factor * (0.82f + 0.18f * offset)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.30f * (1f - offset)), Color.Transparent),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )
                    }
                    // 위에서 아래로 지나가는 광선
                    val y = size.height * pulse
                    drawLine(
                        color = accent.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            LockChargingStyle.RING -> {
                // 남은 충전량을 링으로 보여준다
                val fraction = (state.levelPercent / 100f).coerceIn(0.02f, 1f)
                Canvas(Modifier.size(132.dp)) {
                    val stroke = 9.dp.toPx()
                    val inset = stroke / 2
                    drawArc(
                        color = accent.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.5f), accent, accent.copy(alpha = 0.5f), accent), center = center),
                        startAngle = -90f + (spin * 0.15f),
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }

            LockChargingStyle.WAVE -> {
                // 좌우로 흐르는 파형 (기기의 전류를 연상)
                Canvas(Modifier.fillMaxSize()) {
                    val bars = 5
                    val gap = size.width / (bars * 2f)
                    repeat(bars) { index ->
                        val phase = (pulse + index / bars.toFloat()) % 1f
                        val wave = if (state.isFull) 1f else phase
                        val barHeight = size.height * (0.16f + 0.62f * wave)
                        val x = gap + index * gap * 2f
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.15f))),
                            topLeft = Offset(x, center.y - barHeight / 2f),
                            size = Size(gap, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(gap / 2f)
                        )
                    }
                    // 주변을 도는 작은 점
                    val angle = Math.toRadians(spin.toDouble())
                    val orbit = size.minDimension * 0.46f
                    drawCircle(
                        color = accent.copy(alpha = 0.9f),
                        radius = 3.dp.toPx(),
                        center = Offset(
                            center.x + (cos(angle) * orbit).toFloat(),
                            center.y + (sin(angle) * orbit * 0.35f).toFloat()
                        )
                    )
                }
            }
        }
    }
}

/** 충전 상태 문구 + 애니메이션 묶음 (잠금 화면 하단용). */
@Composable
fun ChargingIndicator(
    style: LockChargingStyle,
    state: ChargingState,
    modifier: Modifier = Modifier,
    accent: Color = NeonCyan
) {
    if (style == LockChargingStyle.NONE || !state.isCharging) return
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ChargingAnimation(style = style, state = state, modifier = Modifier.height(72.dp), accent = accent)
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                state.isFull -> "충전 완료 · ${state.levelPercent}%"
                else -> "충전 중 · ${state.levelPercent}%"
            },
            color = if (state.isFull) NeonGreen else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}