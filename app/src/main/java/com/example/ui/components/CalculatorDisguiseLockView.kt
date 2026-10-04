package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import java.text.DecimalFormat

/**
 * 계산기로 위장한 잠금 화면.
 *
 * 짧게 누르면 정상적인 계산기로 동작하고, '=' 버튼을 **길게 누르면** 잠금 인증이 제출된다.
 * 한 번만 누르면 계산 결과만 표시되므로 위장 화면이 그대로 유지된다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorDisguiseLockView(
    isError: Boolean = false,
    enabled: Boolean = true,
    isVibrationEnabled: Boolean = true,
    onCodeSubmitted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }.getOrNull()
    }

    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var accumulator by remember { mutableStateOf<Double?>(null) }
    var pendingOperator by remember { mutableStateOf<String?>(null) }
    var typedDigits by remember { mutableStateOf("") }
    var justEvaluated by remember { mutableStateOf(false) }
    // 실제 계산기처럼 계산 기록이 남는다 (앱을 껐다 켜도 유지되어 위장에 유리하다).
    var history by remember {
        mutableStateOf(
            runCatching {
                context.getSharedPreferences("calculator_disguise", android.content.Context.MODE_PRIVATE)
                    .getString("history", "")
                    ?.lines()
                    ?.filter { it.isNotBlank() }
                    ?.takeLast(20)
                    ?: emptyList()
            }.getOrDefault(emptyList())
        )
    }

    fun pushHistory(line: String) {
        val updated = (history + line).takeLast(20)
        history = updated
        runCatching {
            context.getSharedPreferences("calculator_disguise", android.content.Context.MODE_PRIVATE)
                .edit().putString("history", updated.joinToString("\n")).apply()
        }
    }

    fun clearHistory() {
        history = emptyList()
        runCatching {
            context.getSharedPreferences("calculator_disguise", android.content.Context.MODE_PRIVATE)
                .edit().remove("history").apply()
        }
    }

    // 인증 실패 시 입력창을 비워 다시 입력할 수 있게 한다.
    LaunchedEffect(isError) {
        if (isError) {
            display = "0"
            expression = ""
            typedDigits = ""
            accumulator = null
            pendingOperator = null
            justEvaluated = false
        }
    }

    fun triggerHaptic(durationMs: Long = 18) {
        if (!isVibrationEnabled || vibrator == null) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "오류"
        return DecimalFormat("#.######").format(value)
    }

    fun calculate(left: Double, op: String, right: Double): Double = when (op) {
        "+" -> left + right
        "-" -> left - right
        "×" -> left * right
        "÷" -> if (right == 0.0) Double.NaN else left / right
        else -> right
    }

    fun resetAll() {
        display = "0"
        expression = ""
        accumulator = null
        pendingOperator = null
        typedDigits = ""
        justEvaluated = false
    }

    fun inputDigit(digit: String) {
        triggerHaptic()
        if (justEvaluated) resetAll()
        if (digit == ".") {
            if (display.contains(".")) return
            display = if (display == "0") "0." else display + "."
            return
        }
        if (display.replace(".", "").length >= 10) return
        display = if (display == "0") digit else display + digit
        if (typedDigits.length < 8) typedDigits += digit
    }

    fun inputOperator(op: String) {
        triggerHaptic()
        val current = display.toDoubleOrNull() ?: 0.0
        val pending = pendingOperator
        val left = accumulator
        if (left != null && pending != null) {
            val step = calculate(left, pending, current)
            accumulator = step
            display = format(step)
        } else {
            accumulator = current
        }
        pendingOperator = op
        expression = "${format(accumulator ?: current)} $op"
        display = "0"
        typedDigits = ""
        justEvaluated = false
    }

    fun inputPercent() {
        triggerHaptic()
        val current = display.toDoubleOrNull() ?: 0.0
        display = format(current / 100.0)
        typedDigits = ""
        justEvaluated = false
    }

    fun evaluate() {
        triggerHaptic()
        val pending = pendingOperator
        val left = accumulator
        if (left != null && pending != null) {
            val right = display.toDoubleOrNull() ?: 0.0
            val result = format(calculate(left, pending, right))
            pushHistory("${format(left)} $pending ${format(right)} = $result")
            display = result
            expression = "${format(left)} $pending ${format(right)} ="
        } else if (expression.isNotBlank() && expression.endsWith(pendingOperator.orEmpty()) && pendingOperator != null) {
            // 연산자만 누르고 = 를 누른 경우 (예: 5 + =)
            val result = format(calculate(left ?: 0.0, pendingOperator ?: "+", display.toDoubleOrNull() ?: 0.0))
            pushHistory("${format(left ?: 0.0)} $pendingOperator = $result")
            display = result
        }
        accumulator = null
        pendingOperator = null
        justEvaluated = true
    }

    fun backspace() {
        triggerHaptic()
        if (display.isNotEmpty() && display != "0") {
            display = display.dropLast(1).ifEmpty { "0" }
            if (typedDigits.isNotEmpty()) typedDigits = typedDigits.dropLast(1)
        }
    }

    /** '=' 길게 누르기: 잠금 인증 제출 */
    fun submitByLongPress() {
        triggerHaptic(durationMs = 45)
        val submitted = typedDigits.ifBlank { display.filter { it.isDigit() } }
        onCodeSubmitted(submitted)
    }

    val displayFontSize: TextUnit = when {
        display.length > 11 -> 26.sp
        display.length > 8 -> 32.sp
        else -> 40.sp
    }

    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
        // 계산 기록 (실제 계산기와 동일하게 '=' 을 눌렀을 때만 쌓인다)
        if (history.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                history.takeLast(3).forEach { line ->
                    Text(
                        text = line,
                        color = Color(0xFF7C8698),
                        fontSize = 12.sp,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF161920),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isError) NeonRed.copy(alpha = 0.8f) else Color(0xFF2A2E38)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (expression.isNotBlank()) {
                    Text(
                        text = expression,
                        color = Color(0xFF8A93A6),
                        fontSize = 13.sp,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = display,
                    color = if (isError) NeonRed else Color(0xFFF2F4F8),
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val digitBackground = Color(0xFF22252E)
        val operatorBackground = Color(0xFF2C3039)
        val rows = listOf(
            listOf("C" to KeyKind.CLEAR, "÷" to KeyKind.OPERATOR, "×" to KeyKind.OPERATOR, "⌫" to KeyKind.BACKSPACE),
            listOf("7" to KeyKind.DIGIT, "8" to KeyKind.DIGIT, "9" to KeyKind.DIGIT, "-" to KeyKind.OPERATOR),
            listOf("4" to KeyKind.DIGIT, "5" to KeyKind.DIGIT, "6" to KeyKind.DIGIT, "+" to KeyKind.OPERATOR),
            listOf("1" to KeyKind.DIGIT, "2" to KeyKind.DIGIT, "3" to KeyKind.DIGIT, "%" to KeyKind.PERCENT),
            listOf("0" to KeyKind.DIGIT, "." to KeyKind.DIGIT, "00" to KeyKind.DIGIT, "=" to KeyKind.EQUALS)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for ((label, kind) in row) {
                        when (kind) {
                            KeyKind.EQUALS -> {
                                val interactionSource = remember { MutableInteractionSource() }
                                val pressed by interactionSource.collectIsPressedAsState()
                                CalculatorKey(
                                    label = label,
                                    background = if (pressed) NeonCyan.copy(alpha = 0.72f) else NeonCyan,
                                    contentColor = Color(0xFF07131A),
                                    borderColor = null,
                                    fontSize = 22.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .combinedClickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            enabled = enabled,
                                            // 짧게 누르면 계산, 길게 누르면 잠금 인증 제출
                                            onClick = { evaluate() },
                                            onLongClick = { submitByLongPress() }
                                        )
                                )
                            }

                            KeyKind.CLEAR -> {
                                val interactionSource = remember { MutableInteractionSource() }
                                val pressed by interactionSource.collectIsPressedAsState()
                                CalculatorKey(
                                    label = label,
                                    background = if (pressed) NeonRed.copy(alpha = 0.28f) else Color(0xFF33222A),
                                    contentColor = NeonRed,
                                    borderColor = NeonRed.copy(alpha = 0.35f),
                                    fontSize = 19.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .combinedClickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            enabled = enabled,
                                            onClick = {
                                                triggerHaptic()
                                                resetAll()
                                            },
                                            // 길게 누르면 계산 기록까지 지운다 (은밀한 초기화)
                                            onLongClick = {
                                                triggerHaptic(durationMs = 45)
                                                clearHistory()
                                            }
                                        )
                                )
                            }

                            KeyKind.BACKSPACE -> CalculatorKey(
                                label = label,
                                background = operatorBackground,
                                contentColor = Color(0xFFE6E9F0),
                                borderColor = CyberBorder.copy(alpha = 0.35f),
                                fontSize = 19.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = enabled) { backspace() }
                            )

                            KeyKind.PERCENT -> CalculatorKey(
                                label = label,
                                background = operatorBackground,
                                contentColor = NeonCyan,
                                borderColor = CyberBorder.copy(alpha = 0.35f),
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = enabled) { inputPercent() }
                            )

                            KeyKind.OPERATOR -> CalculatorKey(
                                label = label,
                                background = operatorBackground,
                                contentColor = NeonCyan,
                                borderColor = CyberBorder.copy(alpha = 0.35f),
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = enabled) { inputOperator(label) }
                            )

                            KeyKind.DIGIT -> CalculatorKey(
                                label = label,
                                background = digitBackground,
                                contentColor = Color(0xFFE6E9F0),
                                borderColor = Color(0xFF2E323C),
                                fontSize = if (label.length > 1) 17.sp else 21.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = enabled) {
                                        if (label == "00") {
                                            inputDigit("0")
                                            inputDigit("0")
                                        } else {
                                            inputDigit(label)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class KeyKind { DIGIT, OPERATOR, PERCENT, CLEAR, BACKSPACE, EQUALS }

@Composable
private fun CalculatorKey(
    label: String,
    background: Color,
    contentColor: Color,
    borderColor: Color?,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .then(
                if (borderColor == null) Modifier
                else Modifier.border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            )
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}