package com.example.ui.screens

import android.app.Activity
import android.os.Build
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackgroundTheme
import com.example.model.LockType
import com.example.ui.components.AppLockBackground
import com.example.ui.components.CalculatorDisguiseLockView
import com.example.ui.components.KnockCodeLockView
import com.example.ui.components.NxNPatternLockView
import com.example.ui.components.PasswordLockView
import com.example.ui.components.PinKeypadView
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun LockOverlayScreen(
    appName: String,
    appIcon: Drawable? = null,
    lockType: LockType = LockType.PATTERN,
    gridSize: Int,
    targetPattern: List<Int>,
    targetPin: String = "1234",
    targetDuressPin: String = "",
    targetPassword: String = "admin1234",
    targetCalculatorCode: String = "1234",
    targetKnockCode: List<Int> = listOf(1, 2, 3, 4),
    backgroundTheme: BackgroundTheme,
    biometricEnabled: Boolean,
    isStealthPattern: Boolean = false,
    isFakeCrashEnabled: Boolean = false,
    isVibrationEnabled: Boolean = true,
    isRandomPinKeypad: Boolean = false,
    isIntruderSirenEnabled: Boolean = false,
    onRequestBiometric: (() -> Unit)? = null,
    onUnlockSuccess: () -> Unit,
    onDuressUnlock: (() -> Unit)? = null,
    onFailedAttempt: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isError by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(0) }
    var messageText by remember {
        mutableStateOf(
            when (lockType) {
                LockType.PIN -> "숫자 PIN 비밀번호를 입력하세요"
                LockType.PATTERN -> "보안 패턴을 입력하세요"
                LockType.PASSWORD -> "보안 비밀번호를 입력하세요"
                LockType.CALCULATOR -> "암호 입력 후 '=' 버튼을 터치하세요"
                LockType.KNOCK_CODE -> "4분면 노크 코드를 터치하세요"
            }
        )
    }
    var showBiometricModal by remember { mutableStateOf(false) }
    var showFakeCrash by remember { mutableStateOf(isFakeCrashEnabled) }

    // Clear error automatically after short delay
    LaunchedEffect(isError) {
        if (isError) {
            delay(1200)
            isError = false
            messageText = when (lockType) {
                LockType.PIN -> "숫자 PIN 비밀번호를 다시 입력하세요"
                LockType.PATTERN -> "보안 패턴을 다시 입력하세요 (${gridSize}x${gridSize} 그리드)"
                LockType.PASSWORD -> "비밀번호를 다시 입력하세요"
                LockType.CALCULATOR -> "올바른 암호 입력 후 '=' 버튼을 누르세요"
                LockType.KNOCK_CODE -> "노크 코드를 다시 순서대로 터치하세요"
            }
        }
    }


    AppLockBackground(theme = backgroundTheme, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Top close / back button for simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x3300F0FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "보호 모드 활성화",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Icon and Title
            Surface(
                shape = CircleShape,
                color = Color(0x4400F0FF),
                modifier = Modifier.size(72.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan)
            ) {
                val iconBitmap = remember(appIcon) {
                    appIcon?.let { icon ->
                        if (icon is BitmapDrawable && icon.bitmap != null) {
                            icon.bitmap
                        } else {
                            val w = if (icon.intrinsicWidth > 0) icon.intrinsicWidth else 96
                            val h = if (icon.intrinsicHeight > 0) icon.intrinsicHeight else 96
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val cv = Canvas(bmp)
                            icon.setBounds(0, 0, cv.width, cv.height)
                            icon.draw(cv)
                            bmp
                        }
                    }
                }

                Box(contentAlignment = Alignment.Center) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap.asImageBitmap(),
                            contentDescription = appName,
                            modifier = Modifier.size(46.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "$appName 잠금",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = messageText,
                color = if (isError) NeonRed else TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            if (attemptCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "인증 실패 횟수: $attemptCount 회",
                    color = NeonRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // The Authentication Input: PIN, Pattern, Password, Calculator, or Knock Code
            when (lockType) {
                LockType.PIN -> {
                    PinKeypadView(
                        targetLength = targetPin.length.coerceIn(4, 8),
                        isError = isError,
                        enabled = !isError,
                        isScrambleKeypad = isRandomPinKeypad,
                        isVibrationEnabled = isVibrationEnabled,
                        onPinCompleted = { enteredPin ->
                            if (targetDuressPin.isNotBlank() && enteredPin == targetDuressPin) {
                                onDuressUnlock?.invoke()
                            } else if (enteredPin == targetPin) {
                                onUnlockSuccess()
                            } else {
                                isError = true
                                attemptCount++
                                if (isIntruderSirenEnabled && attemptCount >= 2) {
                                    com.example.util.IntruderAlertSound.playAlertSiren()
                                }
                                onFailedAttempt?.invoke(attemptCount)
                                messageText = if (attemptCount >= 3) {
                                    "⚠️ 3회 실패! 침입 시도가 기록되었습니다."
                                } else {
                                    "PIN 번호가 일치하지 않습니다! (${attemptCount}회 실패)"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LockType.PATTERN -> {
                    NxNPatternLockView(
                        gridSize = gridSize,
                        isError = isError,
                        enabled = !isError,
                        isStealthMode = isStealthPattern,
                        isVibrationEnabled = isVibrationEnabled,
                        onPatternCompleted = { drawnPattern ->
                            if (drawnPattern == targetPattern) {
                                onUnlockSuccess()
                            } else {
                                isError = true
                                attemptCount++
                                onFailedAttempt?.invoke(attemptCount)
                                messageText = if (attemptCount >= 3) {
                                    "⚠️ 3회 실패! 침입 시도가 기록되었습니다."
                                } else {
                                    "패턴이 일치하지 않습니다! (${attemptCount}회 실패)"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = (if (gridSize > 6) 0.dp else 16.dp))
                    )
                }
                LockType.PASSWORD -> {
                    PasswordLockView(
                        isError = isError,
                        enabled = !isError,
                        isVibrationEnabled = isVibrationEnabled,
                        onPasswordSubmitted = { enteredPassword ->
                            if (enteredPassword == targetPassword) {
                                onUnlockSuccess()
                            } else {
                                isError = true
                                attemptCount++
                                onFailedAttempt?.invoke(attemptCount)
                                messageText = if (attemptCount >= 3) {
                                    "⚠️ 3회 실패! 침입 시도가 기록되었습니다."
                                } else {
                                    "비밀번호가 일치하지 않습니다! (${attemptCount}회 실패)"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LockType.CALCULATOR -> {
                    CalculatorDisguiseLockView(
                        targetCode = targetCalculatorCode,
                        isError = isError,
                        enabled = !isError,
                        isVibrationEnabled = isVibrationEnabled,
                        onCodeSubmitted = { enteredCode ->
                            if (enteredCode == targetCalculatorCode || enteredCode == targetPin) {
                                onUnlockSuccess()
                            } else {
                                isError = true
                                attemptCount++
                                onFailedAttempt?.invoke(attemptCount)
                                messageText = if (attemptCount >= 3) {
                                    "⚠️ 3회 실패! 침입 시도가 기록되었습니다."
                                } else {
                                    "암호가 일치하지 않습니다! (${attemptCount}회 실패)"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LockType.KNOCK_CODE -> {
                    KnockCodeLockView(
                        targetKnockCode = targetKnockCode,
                        isError = isError,
                        enabled = !isError,
                        isVibrationEnabled = isVibrationEnabled,
                        onKnockCompleted = { enteredKnock ->
                            if (enteredKnock == targetKnockCode) {
                                onUnlockSuccess()
                            } else {
                                isError = true
                                attemptCount++
                                onFailedAttempt?.invoke(attemptCount)
                                messageText = if (attemptCount >= 3) {
                                    "⚠️ 3회 실패! 침입 시도가 기록되었습니다."
                                } else {
                                    "노크 코드가 일치하지 않습니다! (${attemptCount}회 실패)"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))


            // Biometric Option (Fingerprint / Face Recognition)
            if (biometricEnabled) {
                Button(
                    onClick = {
                        if (onRequestBiometric != null) {
                            onRequestBiometric()
                        } else {
                            showBiometricModal = true
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = NeonCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "생체 인식",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "생체 인식(지문/얼굴)으로 즉시 해제",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.material3.TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "◀ 홈 화면으로 나가기 (앱 실행 차단)",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Biometric dialog / simulation dialog
        if (showBiometricModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF131B2E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "생체 인식 인증 (지문 & 얼굴)",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "하드웨어 지문 센서를 터치하거나 전면 카메라로 얼굴을 스캔하세요.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showBiometricModal = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("닫기", color = TextSecondary)
                            }

                            Button(
                                onClick = {
                                    showBiometricModal = false
                                    onUnlockSuccess()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("테스트 해제", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Fake Crash Disguise Dialog
        if (showFakeCrash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFA0B0F19)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B),
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonRed.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(56.dp)
                                .clickable {
                                    // Hidden unlock: tapping icon reveals pattern
                                    showFakeCrash = false
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "경고",
                                    tint = NeonRed,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "애플리케이션 오류",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "'$appName' 앱의 작동이 중지되었습니다.\n시스템 안정성을 위해 강제 종료합니다.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("보고서 전송", color = TextSecondary, fontSize = 13.sp)
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3B82F6),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { onDismiss() },
                                            onLongPress = {
                                                // Secret trigger: long press on OK unlocks fake crash!
                                                showFakeCrash = false
                                            }
                                        )
                                    }
                            ) {
                                Text("확인", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
