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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackgroundTheme
import com.example.model.LockType
import com.example.model.LockAccent
import com.example.model.LockClockPosition
import com.example.model.LockChargingStyle
import com.example.model.LockClockStyle
import com.example.model.LockFontStyle
import com.example.model.LockIconShape
import com.example.model.LockPreset
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
import com.example.util.BehavioralInputMetrics
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
    targetDecoyPin: String = "",
    targetPassword: String = "admin1234",
    targetCalculatorCode: String = "1234",
    targetKnockCode: List<Int> = listOf(1, 2, 3, 4),
    backgroundTheme: BackgroundTheme,
    customBackgroundUri: String? = null,
    emergencyContact: String = "",
    lockMessage: String = "",
    lockIconScale: Float = 1f,
    lockBackgroundDim: Float = 0.82f,
    lockClockStyle: LockClockStyle = LockClockStyle.LARGE,
    lockAccent: LockAccent = LockAccent.CYAN,
    lockPreset: LockPreset = LockPreset.ONE_UI,
    lockBackgroundBlur: Float = 0.55f,
    lockPanelAlpha: Float = 0.16f,
    lockCornerRadius: Float = 28f,
    lockClockPosition: LockClockPosition = LockClockPosition.TOP_CENTER,
    lockIconShape: LockIconShape = LockIconShape.CIRCLE,
    lockFontStyle: LockFontStyle = LockFontStyle.SANS,
    lockChargingStyle: LockChargingStyle = LockChargingStyle.RING,
    isLockQuickActionsEnabled: Boolean = true,
    biometricEnabled: Boolean,
    isStealthPattern: Boolean = false,
    isFakeCrashEnabled: Boolean = false,
    fakeScreenKind: FakeScreenKind = FakeScreenKind.CRASH,
    isVibrationEnabled: Boolean = true,
    isRandomPinKeypad: Boolean = false,
    isIntruderSirenEnabled: Boolean = false,
    deviceTiltDegrees: Float = 0f,
    initialLockoutRemainingMs: Long = 0L,
    onRequestRecovery: (() -> Unit)? = null,
    onRequestBiometric: (() -> Unit)? = null,
    onUnlockSuccess: () -> Unit,
    onCredentialVerified: ((BehavioralInputMetrics) -> Unit)? = null,
    onDuressUnlock: (() -> Unit)? = null,
    onDecoyUnlock: (() -> Unit)? = null,
    onFailedAttempt: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(lockAccent.hex)
    // 프리셋에서 넘어온 세부 옵션을 실제 위젯 스타일로 옮긴다.
    val lockFontFamily = when (lockFontStyle) {
        LockFontStyle.SANS -> FontFamily.Default
        LockFontStyle.SERIF -> FontFamily.Serif
        LockFontStyle.MONO -> FontFamily.Monospace
    }
    val iconShape = when (lockIconShape) {
        LockIconShape.CIRCLE -> CircleShape
        LockIconShape.ROUNDED -> RoundedCornerShape((18 * lockIconScale).dp)
        LockIconShape.SQUARE -> RoundedCornerShape(6.dp)
    }
    val clockAlignment = when (lockClockPosition) {
        LockClockPosition.TOP_CENTER -> Alignment.CenterHorizontally
        LockClockPosition.TOP_LEFT -> Alignment.Start
        LockClockPosition.TOP_RIGHT -> Alignment.End
    }
    val clockBoxAlignment = when (lockClockPosition) {
        LockClockPosition.TOP_CENTER -> Alignment.TopCenter
        LockClockPosition.TOP_LEFT -> Alignment.TopStart
        LockClockPosition.TOP_RIGHT -> Alignment.TopEnd
    }
    var isError by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(0) }
    var messageText by remember {
        mutableStateOf(
            when (lockType) {
                LockType.PIN -> "숫자 PIN 비밀번호를 입력하세요"
                LockType.PATTERN -> "보안 패턴을 입력하세요"
                LockType.PASSWORD -> "보안 비밀번호를 입력하세요"
                LockType.CALCULATOR -> "암호를 입력한 뒤 '=' 버튼을 길게 누르세요"
                LockType.KNOCK_CODE -> "4분면 노크 코드를 터치하세요"
            }
        )
    }
    var showBiometricModal by remember { mutableStateOf(false) }
    var showFakeCrash by remember { mutableStateOf(isFakeCrashEnabled) }
    // 실패 횟수 한도 초과로 잠긴 상태. 남은 시간이 0 이 되면 다시 입력할 수 있다.
    var lockoutRemainingMs by remember { mutableStateOf(initialLockoutRemainingMs) }
    LaunchedEffect(lockoutRemainingMs) {
        while (lockoutRemainingMs > 0) {
            delay(1000)
            lockoutRemainingMs = (lockoutRemainingMs - 1000).coerceAtLeast(0L)
        }
    }
    val lockoutSeconds = (lockoutRemainingMs / 1000).toInt()
    val lockoutActive = lockoutSeconds > 0
    val inputEnabled = !isError && !lockoutActive

    // This must be returned before the lock layout is composed.  The previous dialog
    // lived at the bottom of a Column, so it could be measured below the visible area.
    if (showFakeCrash) {
        when (fakeScreenKind) {
            FakeScreenKind.EMPTY_ALBUM -> EmptyAlbumDisguise(
                appName = appName,
                onRevealLock = { showFakeCrash = false }
            )
            FakeScreenKind.CALCULATOR -> CalculatorDisguiseLockView(
                onCodeSubmitted = { showFakeCrash = false },
                modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)
            )
            FakeScreenKind.MAINTENANCE -> MaintenanceDisguise(appName = appName, onRevealLock = { showFakeCrash = false })
            FakeScreenKind.CRASH -> FakeCrashDisguise(
                appName = appName,
                onDismiss = onDismiss,
                onRevealLock = { showFakeCrash = false }
            )
        }
        return
    }

    // Clear error automatically after short delay
    LaunchedEffect(isError) {
        if (isError) {
            delay(1200)
            isError = false
            messageText = when (lockType) {
                LockType.PIN -> "숫자 PIN 비밀번호를 다시 입력하세요"
                LockType.PATTERN -> "보안 패턴을 다시 입력하세요 (${gridSize}x${gridSize} 그리드)"
                LockType.PASSWORD -> "비밀번호를 다시 입력하세요"
                LockType.CALCULATOR -> "암호를 입력한 뒤 '=' 버튼을 길게 누르세요"
                LockType.KNOCK_CODE -> "노크 코드를 다시 순서대로 터치하세요"
            }
        }
    }


    AppLockBackground(theme = backgroundTheme, customImageUri = customBackgroundUri, dimAmount = lockBackgroundDim, blurAmount = lockBackgroundBlur, modifier = modifier) {
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
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "보호 모드 활성화",
                            color = accentColor,
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

            if (lockClockStyle != LockClockStyle.HIDDEN) {
                Box(Modifier.fillMaxWidth(), contentAlignment = clockBoxAlignment) {
                    Column(horizontalAlignment = clockAlignment) {
                        Text(
                            text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.KOREA).format(java.util.Date()),
                            color = Color.White,
                            fontSize = when (lockClockStyle) {
                                LockClockStyle.LARGE -> if (lockClockPosition == LockClockPosition.TOP_CENTER) 46.sp else 30.sp
                                else -> 22.sp
                            },
                            fontWeight = FontWeight.Light,
                            fontFamily = lockFontFamily
                        )
                        Text(
                            text = java.text.SimpleDateFormat("M월 d일 EEEE", java.util.Locale.KOREA).format(java.util.Date()),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = lockFontFamily
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 아이콘 · 앱 이름 · 안내를 반투명 패널로 묶는다 (프리셋 모서리/투명도 반영)
            Surface(
                shape = RoundedCornerShape(lockCornerRadius.dp),
                color = accentColor.copy(alpha = lockPanelAlpha.coerceIn(0f, 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth()
            ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
            ) {

            // App Icon and Title
            Surface(
                shape = iconShape,
                color = Color(0x4400F0FF),
                modifier = Modifier.size((72 * lockIconScale).dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, accentColor)
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
                            modifier = Modifier.size((46 * lockIconScale).dp)
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
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = lockFontFamily,
                textAlign = TextAlign.Center
            )
            if (lockMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(lockMessage, color = accentColor, fontSize = 13.sp, textAlign = TextAlign.Center, fontFamily = lockFontFamily)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = messageText,
                color = if (isError) NeonRed else TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontFamily = lockFontFamily
            )
            if (emergencyContact.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("비상 연락: $emergencyContact", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            }

            if (attemptCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "인증 실패 횟수: $attemptCount 회",
                    color = NeonRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            } // 패널 안 Column 끝
            } // 반투명 패널(Surface) 끝

            Spacer(modifier = Modifier.weight(1f))

            // 실패 횟수 한도 초과: 입력을 막고 남은 시간을 표시한다.
            if (lockoutActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33FF5252), RoundedCornerShape(16.dp))
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "잠금이 걸려 있습니다",
                            color = NeonRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "%02d:%02d 후 다시 시도할 수 있습니다".format(lockoutSeconds / 60, lockoutSeconds % 60),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = lockFontFamily
                        )
                    }
                }
            }

            // The Authentication Input: PIN, Pattern, Password, Calculator, or Knock Code
            when (lockType) {
                LockType.PIN -> {
                    PinKeypadView(
                        targetLength = targetPin.length.coerceIn(4, 8),
                        isError = isError,
                        enabled = inputEnabled,
                        isScrambleKeypad = isRandomPinKeypad,
                        isVibrationEnabled = isVibrationEnabled,
                        onPinCompleted = { enteredPin, metrics ->
                            if (targetDuressPin.isNotBlank() && enteredPin == targetDuressPin) {
                                onDuressUnlock?.invoke()
                            } else if (targetDecoyPin.isNotBlank() && enteredPin == targetDecoyPin) {
                                onDecoyUnlock?.invoke()
                            } else if (enteredPin == targetPin) {
                                onCredentialVerified?.invoke(metrics.copy(deviceTiltDegrees = deviceTiltDegrees)) ?: onUnlockSuccess()
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
                        enabled = inputEnabled,
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
                        enabled = inputEnabled,
                        isVibrationEnabled = isVibrationEnabled,
                        onPasswordSubmitted = { enteredPassword, metrics ->
                            if (enteredPassword == targetPassword) {
                                onCredentialVerified?.invoke(metrics.copy(deviceTiltDegrees = deviceTiltDegrees)) ?: onUnlockSuccess()
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
                        isError = isError,
                        enabled = inputEnabled,
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
                        enabled = inputEnabled,
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

            // 충전 중일 때만 나타나는 애니메이션 (설정에서 스타일을 고를 수 있다)
            if (lockChargingStyle != LockChargingStyle.NONE) {
                val chargingState = com.example.ui.components.rememberChargingState().value
                if (chargingState.isCharging) {
                    com.example.ui.components.ChargingIndicator(
                        style = lockChargingStyle,
                        state = chargingState,
                        modifier = Modifier.fillMaxWidth(),
                        accent = accentColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (isLockQuickActionsEnabled) Text("◉                                      🔦", color = accentColor.copy(alpha = 0.8f), fontSize = 17.sp, modifier = Modifier.fillMaxWidth())

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

            // 비밀번호를 잊었을 때: 설정해 둔 개인 확인 질문으로만 복구할 수 있다.
            if (onRequestRecovery != null) {
                androidx.compose.material3.TextButton(
                    onClick = onRequestRecovery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "비밀번호를 잊으셨나요?",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
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

        // Fake Crash Disguise Dialog (시스템 오류창 위장)
        if (showFakeCrash) {
            SystemCrashDisguiseDialog(
                appName = appName,
                onCloseApp = onDismiss,
                onRevealLock = { showFakeCrash = false }
            )
        }
    }
}

/** 안드로이드 시스템 오류창처럼 보이는 가짜 오류창. 닫기 버튼을 길게 눌러야 잠금 화면이 열린다. */
@Composable
private fun SystemCrashDisguiseDialog(
    appName: String,
    onCloseApp: () -> Unit,
    onRevealLock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000).copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF2B2B2F),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp)
                .pointerInput(Unit) {
                    // 대화창 여백을 길게 눌러도 잠금 화면으로 진입한다.
                    detectTapGestures(onLongPress = { onRevealLock() })
                }
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, top = 26.dp, end = 24.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF3A3A40),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE8EAED),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "불행하게도, $appName 앱이 중단되었습니다",
                    color = Color(0xFFE8EAED),
                    fontSize = 19.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "앱이 비정상적으로 종료되었습니다. 계속되면 다른 앱에도 문제가 생길 수 있습니다.",
                    color = Color(0xFFB7BAC1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onCloseApp,
                        shape = RoundedCornerShape(22.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6F7379)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB7BAC1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("앱 정보", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8AB4F8),
                            contentColor = Color(0xFF0B1220)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .pointerInput(Unit) {
                                // 한 번 누르면 앱 종료(위장), 길게 눌러야 잠금 화면이 열린다.
                                detectTapGestures(
                                    onTap = { onCloseApp() },
                                    onLongPress = { onRevealLock() }
                                )
                            }
                    ) {
                        Text("닫기", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/** The screen selected for a locked app while the disguise option is enabled. */
enum class FakeScreenKind { CRASH, EMPTY_ALBUM, CALCULATOR, MAINTENANCE }

@Composable
private fun FakeCrashDisguise(appName: String, onDismiss: () -> Unit, onRevealLock: () -> Unit) {
    SystemCrashDisguiseDialog(
        appName = appName,
        onCloseApp = onDismiss,
        onRevealLock = onRevealLock
    )
}

@Composable
private fun EmptyAlbumDisguise(appName: String, onRevealLock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF101114)).pointerInput(Unit) {
            detectTapGestures(onLongPress = { onRevealLock() })
        }.padding(20.dp)
    ) {
        Text(appName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        Text("앨범", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▧", color = Color(0xFF8E939B), fontSize = 48.sp)
                Spacer(Modifier.height(10.dp))
                Text("사진이나 동영상이 없습니다", color = Color(0xFFB8BBC1), fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun MaintenanceDisguise(appName: String, onRevealLock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).pointerInput(Unit) {
            detectTapGestures(onLongPress = { onRevealLock() })
        }.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(18.dp))
        Text("서비스 점검 중", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 21.sp)
        Spacer(Modifier.height(8.dp))
        Text("$appName 서비스를 잠시 점검하고 있습니다.\n잠시 후 다시 이용해 주세요.", color = Color(0xFF64748B), textAlign = TextAlign.Center, fontSize = 14.sp)
    }
}
