package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
fun ChangeKnockCodeModal(
    currentCode: List<Int>,
    onDismiss: () -> Unit,
    onSave: (List<Int>) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: First knock sequence, 2: Confirm
    var firstEntered = remember { mutableStateListOf<Int>() }
    var isError by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("4분면을 4~8번 순서대로 터치하여 새 노크 코드를 만드세요") }

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
                text = if (step == 1) "새 노크 코드 등록" else "노크 코드 재확인",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = if (isError) NeonRed else if (step == 2) NeonGreen else TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            KnockCodeLockView(
                targetKnockCode = if (step == 1) List(4) { 0 } else firstEntered.toList(),
                isError = isError,
                onKnockCompleted = { entered ->
                    if (step == 1) {
                        if (entered.size < 4) {
                            isError = true
                            message = "최소 4번 이상 터치해야 합니다."
                        } else {
                            firstEntered.clear()
                            firstEntered.addAll(entered)
                            step = 2
                            message = "확인을 위해 같은 순서로 한 번 더 터치하세요"
                        }
                    } else {
                        if (entered == firstEntered.toList()) {
                            onSave(entered)
                        } else {
                            isError = true
                            message = "노크 코드가 일치하지 않습니다. 처음부터 다시 시도하세요."
                            step = 1
                            firstEntered.clear()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("취소", color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
