package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.RecoveryQuestionManager

/**
 * 개인 확인 질문 등록.
 * 고른 질문에 답을 입력하면 해시로만 저장된다.
 */
@Composable
fun RecoverySetupDialog(
    alreadyConfigured: Boolean,
    onSave: (Map<String, String>) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val answers = remember { mutableStateMapOf<String, String>() }
    var selected by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("비밀번호를 잊었을 때 복구 질문", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "3개 이상 고르고 답을 직접 기억하세요. 답은 해시로만 저장되어 앱을 열어도 읽을 수 없습니다. 공백과 대소문자는 무시하고 비교합니다.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                RecoveryQuestionManager.PRESET_QUESTIONS.forEach { (id, prompt, hint) ->
                    val isOn = selected.contains(id)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isOn) NeonCyan.copy(alpha = 0.12f) else CyberCardDark,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = isOn,
                                    onClick = {
                                        selected = if (isOn) selected - id else selected + id
                                    },
                                    label = { Text(if (isOn) "선택됨" else "선택", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(prompt, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(hint, color = TextSecondary, fontSize = 10.sp)
                            if (isOn) {
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = answers[id] ?: "",
                                    onValueChange = { answers[id] = it },
                                    singleLine = true,
                                    label = { Text("내 답", fontSize = 11.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = CyberBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = NeonCyan
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(answers.filterKeys { selected.contains(it) })
                    onDismiss()
                },
                enabled = selected.size >= 3 && answers.count { selected.contains(it.key) && it.value.isNotBlank() } >= 3
            ) { Text("저장", color = NeonCyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (alreadyConfigured) {
                    TextButton(onClick = { onClear(); onDismiss() }) { Text("삭제", color = NeonRed, fontSize = 12.sp) }
                }
                TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) }
            }
        },
        containerColor = CyberCardDark
    )
}

/** 복구 수단 선택 (개인 질문 / 12자리 복구키). */
@Composable
fun RecoveryMethodChooserDialog(
    hasQuestions: Boolean,
    hasKey: Boolean,
    onUseQuestions: () -> Unit,
    onUseKey: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("잠금 복구", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "설정한 수단으로만 잠금을 해제할 수 있습니다.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                if (hasQuestions) {
                    Button(
                        onClick = onUseQuestions,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("개인 확인 질문으로 복구", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                }
                if (hasKey) {
                    Button(
                        onClick = onUseKey,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("12자리 복구키 입력", fontWeight = FontWeight.Bold) }
                }
                if (!hasQuestions && !hasKey) {
                    Text(
                        text = "아직 복구 수단이 없습니다. 설정에서 개인 확인 질문이나 복구키를 먼저 등록하세요.",
                        color = NeonRed,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) } },
        containerColor = CyberCardDark
    )
}

/** 12자리 복구키 등록. 두 번 입력해야 저장된다. */
@Composable
fun RecoveryKeySetupDialog(
    alreadyConfigured: Boolean,
    failedAttempts: Int,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("12자리 복구키", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "숫자 12자리를 직접 정하거나 아래 버튼으로 자동 생성할 수 있습니다. " +
                            "잊어버리면 잠금을 해제할 방법이 없어지므로 여러 곳에 보관하세요.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it.filter(Char::isDigit).take(12); error = "" },
                    singleLine = true,
                    label = { Text("복구키 (12자리)", fontSize = 11.sp) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
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
                OutlinedTextField(
                    value = second,
                    onValueChange = { second = it.filter(Char::isDigit).take(12); error = "" },
                    singleLine = true,
                    label = { Text("한 번 더 입력", fontSize = 11.sp) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberCardDark, RoundedCornerShape(10.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("자동 생성", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = NeonRed, fontSize = 12.sp)
                }
                if (alreadyConfigured && failedAttempts > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("이전 복구키 오입력 ${failedAttempts}회", color = TextSecondary, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        first.length != 12 -> error = "12자리 숫자를 입력하세요."
                        first != second -> error = "두 번 입력한 값이 다릅니다."
                        else -> { onSave(first); onDismiss() }
                    }
                }
            ) { Text("저장", color = NeonCyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (alreadyConfigured) {
                    TextButton(onClick = { onClear(); onDismiss() }) { Text("삭제", color = NeonRed, fontSize = 12.sp) }
                }
                TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) }
            }
        },
        containerColor = CyberCardDark
    )
}

/** 12자리 복구키 입력 (시도 횟수 제한 없음). */
@Composable
fun RecoveryKeyVerifyDialog(
    failedAttempts: Int,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("복구키 입력", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "12자리 복구키를 입력하세요. 추적이 어려운 숫자열이라 시도 횟수 제한이 없습니다.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter(Char::isDigit).take(12) },
                    singleLine = true,
                    label = { Text("복구키", fontSize = 11.sp) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (failedAttempts > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("지금까지 틀린 횟수: ${failedAttempts}회", color = TextSecondary, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(value) },
                enabled = value.length == 12
            ) { Text("확인", color = NeonGreen, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) } },
        containerColor = CyberCardDark
    )
}

/**
 * 잠금 화면에서 비밀번호를 잊었을 때 쓰는 복구 시도.
 * 모든 질문에 맞아야만 복구된다.
 */
@Composable
fun RecoveryVerifyDialog(
    questions: List<com.example.model.RecoveryQuestion>,
    onSubmit: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val answers = remember { mutableStateMapOf<String, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("복구 인증", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "잠금 비밀번호를 잊으셨나요? 설정해 둔 개인 질문에 모두 맞게 답하면 잠금을 해제할 수 있습니다.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                questions.forEach { question ->
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Text(question.prompt, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (!question.hint.isNullOrBlank()) {
                            Text("힌트: ${question.hint}", color = TextSecondary, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = answers[question.id] ?: "",
                            onValueChange = { answers[question.id] = it },
                            singleLine = true,
                            label = { Text("답", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(answers.toMap()); onDismiss() },
                enabled = questions.all { answers[it.id]?.isNotBlank() == true }
            ) { Text("인증하기", color = NeonGreen, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = TextSecondary) } },
        containerColor = CyberCardDark
    )
}