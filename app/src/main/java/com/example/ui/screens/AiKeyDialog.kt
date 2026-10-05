package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AiSettings

/** API 키 입력 화면. 키는 Keystore 로 암호화되어 저장된다. */
@Composable
fun AiKeyDialog(
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    var value by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurfaceDark,
        title = { Text("AI 키 설정", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "현재 키: ${AiSettings.maskedKey(context)}\n모델: ${AiSettings.model(context)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it; message = "" },
                    singleLine = true,
                    label = { Text("새 API 키", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "키는 기기 안의 Keystore로 암호화해서만 저장됩니다. 서버로 전송하지 않습니다.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(message, color = NeonRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (value.isBlank()) {
                    message = "키를 입력해 주세요."
                } else if (AiSettings.setApiKey(context, value)) {
                    onSaved(value)
                    onDismiss()
                } else {
                    message = "키를 저장하지 못했습니다. 저장 공간을 확인해 주세요."
                }
            }) { Text("저장", color = NeonCyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    AiSettings.clearApiKey(context)
                    onSaved("")
                    onDismiss()
                }) { Text("삭제", color = NeonRed, fontSize = 12.sp) }
                TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) }
            }
        }
    )
}