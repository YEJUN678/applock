package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinModal(
    currentPin: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Enter new PIN, 2: Confirm new PIN
    var firstEnteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("새 4자리 숫자 비밀번호를 입력하세요") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberSurfaceDark
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (step == 1) "새 PIN 비밀번호 등록" else "PIN 비밀번호 재확인",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = if (isError) NeonRed else if (step == 2 && firstEnteredPin.isNotEmpty()) NeonGreen else TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            PinKeypadView(
                targetLength = 4,
                isError = isError,
                enabled = !isError,
                onPinCompleted = { entered ->
                    if (step == 1) {
                        firstEnteredPin = entered
                        step = 2
                        message = "확인을 위해 비밀번호를 한 번 더 입력하세요"
                    } else {
                        if (entered == firstEnteredPin) {
                            onSave(entered)
                        } else {
                            isError = true
                            message = "비밀번호가 일치하지 않습니다. 처음부터 다시 입력하세요."
                            // Reset back to step 1 after brief pause
                            step = 1
                            firstEnteredPin = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("취소", color = TextSecondary)
                }

                if (step == 2) {
                    Button(
                        onClick = {
                            step = 1
                            firstEnteredPin = ""
                            message = "새 4자리 숫자 비밀번호를 입력하세요"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = NeonCyan
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("다시 설정", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
