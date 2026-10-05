package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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

/**
 * 충전 상태.
 *
 * [justStarted] 는 충전기�� 꽂은 그 순간에만 true 다.
 * 사용자는 "충전 시작"을 한 번만 보고 싶어 한다. 이후로는 정지된 표시만 보면 된다.
 */
data class ChargingState(
    val isCharging: Boolean,
    val levelPercent: Int = 0,
    val isFull: Boolean = false,
    val justStarted: Boolean = false
)

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

        // 충전이 시작됐는지 따로 기억한다. 시작 순간에만 true 를 내보내야 한다.
        var wasCharging = false

        fun readFrom(intent: Intent?, plugEvent: Boolean): ChargingState {
            val batteryIntent = intent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: return ChargingState(false)
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
            val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val full = status == BatteryManager.BATTERY_STATUS_FULL
            val charging = plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING

            // 충전기 연결/해제 방송일 때만 새로 시작한 것으로 본다.
            val started = charging && !wasCharging && plugEvent
            wasCharging = charging

            return ChargingState(
                isCharging = charging,
                levelPercent = percent,
                isFull = full,
                justStarted = started
            )
        }

        state = readFrom(null, plugEvent = false)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val plugEvent = intent?.action == Intent.ACTION_POWER_CONNECTED
                state = readFrom(intent, plugEvent)
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
    // 충전 시작 순간에만 아주 짧게 움직이고, 그 뒤로는 완전히 정지한다.
    val transient = rememberInfiniteTransition(label = "charging")
    val transientPulse by transient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "transientPulse"
    )
    val transientSpin by transient.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1400)),
        label = "transientSpin"
    )
    // 한 번 지나간 뒤에는 0 / 0 에서 멈춘 값으로 고정된다.
    var shown by remember { mutableFloatStateOf(0.5f) }
    var shownSpin by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.justStarted) {
        if (!state.justStarted) return@LaunchedEffect
        // 충전 시작 시 0.8초 동안만 움직이고, 그 뒤 완전히 멈춘다.
        shown = 0f
        shownSpin = 0f
        val startedAt = System.currentTimeMillis()
        while (System.currentTimeMillis() - startedAt < 800) {
            shown = transientPulse
            shownSpin = transientSpin
            delay(60)
        }
        // 정지 상태는 아래 그려지는 정적 표현(맥동 0.5, 회전 0)을 유지한다.
        shown = 0.5f
        shownSpin = 0f
    }
    // 충전 시작 순간에만 움직이고, 이후에는 고정된 값으로 멈춰 있는다.
    val pulse = shown
    val spin = shownSpin

    // 충전기 꽂은 순간에만 가운데에서 한 번 나타나고, 그 뒤로는 멈춰 있는다.
    // 계속 움직이면 잠금 화면이 산뜻거려서 불편하다.
    var revealed by remember(style) { mutableStateOf(!state.justStarted) }
    LaunchedEffect(style, state.justStarted) {
        if (state.justStarted) {
            revealed = false
            delay(420)
            revealed = true
        } else {
            revealed = true
        }
    }
    val enter by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "chargingEnter"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            // 처음엔 아주 작게였다가 커지며 나타난다(가운데에서 부풀어 나오는 느낌).
            .graphicsLayer {
                scaleX = 0.4f + 0.6f * enter
                scaleY = 0.4f + 0.6f * enter
                alpha = enter
            },
        contentAlignment = Alignment.Center
    ) {
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
                // 남은 충전량을 링으로 보여준다.
                // 크기를 고정하지 않고 주어진 칸에 맞춰 그려야 찌부려지지 않는다.
                val fraction = (state.levelPercent / 100f).coerceIn(0.02f, 1f)
                Canvas(Modifier.fillMaxSize()) {
                    // 테두리 두께도 칸 크기에 비례하게 정한다.
                    val diameter = size.minDimension
                    val stroke = (diameter * 0.068f).coerceIn(4.dp.toPx(), 10.dp.toPx())
                    val inset = stroke / 2f
                    val arcSize = Size(diameter - stroke, diameter - stroke)
                    val topLeft = Offset((size.width - diameter) / 2f + inset, (size.height - diameter) / 2f + inset)

                    // 배경 트랙
                    drawArc(
                        color = accent.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    // 충전량
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(accent.copy(alpha = 0.5f), accent, accent.copy(alpha = 0.5f), accent),
                            center = center
                        ),
                        startAngle = -90f + (spin * 0.15f),
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    // 완충이면 링 바깥에 은은한 빛을 한 겹 더한다.
                    if (state.isFull) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Transparent, accent.copy(alpha = 0.14f)),
                                center = center,
                                radius = diameter / 2f
                            ),
                            radius = diameter / 2f,
                            center = center
                        )
                    }
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
        // 높이를 넉넉히 준다. 좁으면 링이 찌부려진다.
        ChargingAnimation(style = style, state = state, modifier = Modifier.height(108.dp), accent = accent)
        Spacer(Modifier.height(8.dp))
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