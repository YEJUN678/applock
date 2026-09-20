package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.DecimalFormat

@Composable
fun CalculatorDisguiseLockView(
    targetCode: String = "1234",
    isError: Boolean = false,
    enabled: Boolean = true,
    isVibrationEnabled: Boolean = true,
    onCodeSubmitted: (String) -> Unit,
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

    var displayValue by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var rawInputBuffer by remember { mutableStateOf("") }

    fun triggerHaptic() {
        if (!isVibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        } catch (_: Exception) {}
    }

    fun handleClear() {
        triggerHaptic()
        displayValue = "0"
        expression = ""
        rawInputBuffer = ""
    }

    fun handleDigit(d: String) {
        triggerHaptic()
        rawInputBuffer += d
        if (displayValue == "0") {
            displayValue = d
        } else {
            displayValue += d
        }
    }

    fun handleOperator(op: String) {
        triggerHaptic()
        rawInputBuffer += op
        expression = "$displayValue $op"
        displayValue = "0"
    }

    fun handleEquals() {
        triggerHaptic()
        // 1. Check if raw input buffer or currently displayed number matches targetCode
        val cleanCurrent = displayValue.trim()
        val cleanBuffer = rawInputBuffer.trim()
        if (cleanCurrent == targetCode || cleanBuffer == targetCode || cleanBuffer.endsWith(targetCode)) {
            onCodeSubmitted(targetCode)
            return
        }

        // 2. Perform real calculation disguise
        try {
            if (expression.isNotBlank()) {
                val parts = expression.trim().split(" ")
                if (parts.size >= 2) {
                    val num1 = parts[0].toDoubleOrNull() ?: 0.0
                    val op = parts[1]
                    val num2 = displayValue.toDoubleOrNull() ?: 0.0
                    val result = when (op) {
                        "+" -> num1 + num2
                        "-" -> num1 - num2
                        "×" -> num1 * num2
                        "÷" -> if (num2 != 0.0) num1 / num2 else Double.NaN
                        else -> num2
                    }
                    val df = DecimalFormat("#.######")
                    displayValue = if (result.isNaN()) "Error" else df.format(result)
                    expression = ""
                }
            } else {
                // If just digits were entered and '=' pressed, check code submission
                onCodeSubmitted(cleanCurrent)
            }
        } catch (_: Exception) {
            displayValue = "Error"
        }
        rawInputBuffer = ""
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        // Calculator display screen
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isError) NeonRed else CyberBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expression.ifBlank { "" },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = displayValue,
                    color = if (isError) NeonRed else TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // Calculator Buttons Grid
        val buttonRows = listOf(
            listOf("C" to Color(0xFFEF4444), "÷" to NeonCyan, "×" to NeonCyan, "⌫" to Color(0xFF94A3B8)),
            listOf("7" to TextPrimary, "8" to TextPrimary, "9" to TextPrimary, "-" to NeonCyan),
            listOf("4" to TextPrimary, "5" to TextPrimary, "6" to TextPrimary, "+" to NeonCyan),
            listOf("1" to TextPrimary, "2" to TextPrimary, "3" to TextPrimary, "%" to NeonCyan),
            listOf("0" to TextPrimary, "00" to TextPrimary, "." to TextPrimary, "=" to NeonPurple)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in buttonRows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for ((label, textColor) in row) {
                        val isOp = label in listOf("+", "-", "×", "÷", "=", "C")
                        val isEquals = label == "="
                        val bgColor = when {
                            isEquals -> NeonCyan
                            label == "C" -> Color(0x33EF4444)
                            isOp -> Color(0x2200F0FF)
                            else -> CyberCardDark
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(
                                    1.dp,
                                    if (isEquals) NeonCyan else CyberBorder.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = enabled) {
                                    when (label) {
                                        "C" -> handleClear()
                                        "⌫" -> {
                                            triggerHaptic()
                                            if (displayValue.length > 1) {
                                                displayValue = displayValue.dropLast(1)
                                            } else {
                                                displayValue = "0"
                                            }
                                            if (rawInputBuffer.isNotEmpty()) rawInputBuffer = rawInputBuffer.dropLast(1)
                                        }
                                        "=" -> handleEquals()
                                        "+", "-", "×", "÷" -> handleOperator(label)
                                        else -> handleDigit(label)
                                    }
                                }
                        ) {
                            Text(
                                text = label,
                                color = if (isEquals) Color.Black else textColor,
                                fontSize = if (label.length > 1) 16.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
