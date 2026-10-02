package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BehavioralInputMetrics

@Composable
fun PasswordLockView(
    isError: Boolean = false,
    enabled: Boolean = true,
    isVibrationEnabled: Boolean = true,
    onPasswordSubmitted: (String, BehavioralInputMetrics) -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var firstInputAt by remember { mutableStateOf(0L) }
    var previousInputAt by remember { mutableStateOf(0L) }
    var intervalTotal by remember { mutableStateOf(0L) }
    var intervalCount by remember { mutableStateOf(0) }

    fun submit() {
        if (passwordText.isBlank()) return
        val now = android.os.SystemClock.elapsedRealtime()
        onPasswordSubmitted(passwordText, BehavioralInputMetrics(
            durationMs = (now - firstInputAt).coerceAtLeast(0),
            averageIntervalMs = if (intervalCount == 0) 0 else intervalTotal / intervalCount,
            averagePressure = 1f, // IME does not expose hardware touch pressure.
            deviceTiltDegrees = 0f
        ))
    }

    LaunchedEffect(isError) {
        if (isError) {
            passwordText = ""
            firstInputAt = 0L
            previousInputAt = 0L
            intervalTotal = 0L
            intervalCount = 0
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = passwordText,
            onValueChange = {
                if (enabled) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (it.length > passwordText.length) {
                        if (firstInputAt == 0L) firstInputAt = now
                        if (previousInputAt != 0L) { intervalTotal += now - previousInputAt; intervalCount++ }
                        previousInputAt = now
                    }
                    passwordText = it
                }
            },
            enabled = enabled,
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (passwordText.isNotBlank()) {
                        submit()
                    }
                }
            ),
            placeholder = {
                Text("비밀번호 입력", color = TextSecondary.copy(alpha = 0.6f))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isError) NeonRed else NeonCyan
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (passwordText.isNotEmpty()) {
                        IconButton(onClick = { passwordText = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "지우기",
                                tint = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "비밀번호 보기 토글",
                            tint = if (isPasswordVisible) NeonCyan else TextSecondary
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = if (isError) NeonRed else NeonCyan,
                unfocusedBorderColor = if (isError) NeonRed.copy(alpha = 0.6f) else CyberBorder,
                focusedContainerColor = CyberCardDark,
                unfocusedContainerColor = CyberCardDark
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (passwordText.isNotBlank()) {
                    submit()
                }
            },
            enabled = enabled && passwordText.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = Color.Black,
                disabledContainerColor = CyberCardDark,
                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "잠금 해제 확인",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
