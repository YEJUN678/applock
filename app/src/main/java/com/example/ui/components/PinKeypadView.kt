package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
fun PinKeypadView(
    targetLength: Int = 4,
    isError: Boolean = false,
    enabled: Boolean = true,
    isScrambleKeypad: Boolean = false,
    isVibrationEnabled: Boolean = true,
    onPinCompleted: (String) -> Unit,
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

    var currentPin by remember { mutableStateOf("") }

    // Clear entered digits when error triggered
    androidx.compose.runtime.LaunchedEffect(isError) {
        if (isError) {
            currentPin = ""
        }
    }

    fun triggerHaptic() {
        if (!isVibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    fun handleDigitPress(digit: String) {
        if (!enabled || currentPin.length >= targetLength) return
        triggerHaptic()
        val newPin = currentPin + digit
        currentPin = newPin
        if (newPin.length == targetLength) {
            onPinCompleted(newPin)
        }
    }

    fun handleBackspace() {
        if (!enabled || currentPin.isEmpty()) return
        triggerHaptic()
        currentPin = currentPin.dropLast(1)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        // PIN Dots indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            for (i in 0 until targetLength) {
                val isFilled = i < currentPin.length
                val dotColor by animateColorAsState(
                    targetValue = if (isError) NeonRed
                    else if (isFilled) NeonCyan
                    else Color(0xFF2E3D5B),
                    label = "pinDot"
                )

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(
                            width = 1.5.dp,
                            color = if (isError) NeonRed else if (isFilled) NeonCyan else CyberBorder,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keypad grid (3 x 4)
        val digitList = remember(isScrambleKeypad) {
            if (isScrambleKeypad) {
                listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9").shuffled()
            } else {
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            }
        }
        val digits = listOf(
            listOf(digitList[0], digitList[1], digitList[2]),
            listOf(digitList[3], digitList[4], digitList[5]),
            listOf(digitList[6], digitList[7], digitList[8]),
            listOf("", digitList[9], "DEL")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            for (row in digits) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (item in row) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (item) {
                                "" -> {
                                    // Empty placeholder
                                }
                                "DEL" -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = CyberCardDark,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clickable(enabled = enabled && currentPin.isNotEmpty()) {
                                                handleBackspace()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "삭제",
                                                tint = if (currentPin.isNotEmpty()) NeonCyan else TextSecondary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = CyberCardDark,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clickable(enabled = enabled) {
                                                handleDigitPress(item)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = item,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
