package com.example

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.crypto.SecureNotesManager
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppLockPreferences
import com.example.util.BiometricHelper
import com.example.util.BiometricStatus

class SecureNotesActivity : FragmentActivity() {
    companion object { private const val AUTO_LOCK_MS = 60_000L }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (BiometricHelper.checkBiometricStatus(this) == BiometricStatus.AVAILABLE) {
            BiometricHelper.authenticate(
                activity = this,
                title = "보안 메모 열기",
                subtitle = "생체 인증 또는 현재 PIN으로 열 수 있습니다.",
                onSuccess = ::showNotes,
                onError = { _, _ -> showPinPrompt() }
            )
        } else showPinPrompt()
    }

    private fun showPinPrompt() {
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD; hint = "현재 PIN" }
        AlertDialog.Builder(this).setTitle("보안 메모 인증").setMessage("듀레스 PIN은 사용할 수 없습니다.").setView(input)
            .setPositiveButton("열기") { _, _ ->
                if (input.text.toString() == AppLockPreferences.getLockConfig(this).savedPin) showNotes() else { finish() }
            }.setNegativeButton("취소") { _, _ -> finish() }.show()
    }

    private fun showNotes() = setContent {
        MyApplicationTheme {
            // remember 가 없으면 recomposition 마다 load() 가 다시 돌면서
            // 입력 중인 글자가 매 키 입력마다 저장된 값으로 덮어써진다.
            val activity = this@SecureNotesActivity
            var note by remember { mutableStateOf(SecureNotesManager.load(activity)) }
            var lastSaved by remember { mutableStateOf(0L) }

            // 1분 뒤 자동 잠금
            LaunchedEffect(Unit) {
                delay(AUTO_LOCK_MS)
                finish()
            }
            // 입력 후 잠시 멈추면 암호화해서 임시 저장한다 (자동 잠금으로 글자를 잃지 않게).
            LaunchedEffect(note) {
                delay(700)
                if (note.isNotEmpty() && SecureNotesManager.save(activity, note)) {
                    lastSaved = System.currentTimeMillis()
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("보안 메모", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = when {
                        lastSaved == 0L -> "AES‑GCM 암호화 저장 · 1분 후 자동 잠금"
                        System.currentTimeMillis() - lastSaved < 5_000 -> "암호화 임시 저장됨"
                        else -> "임시 저장 ${(System.currentTimeMillis() - lastSaved) / 1000}초 전"
                    } + " · ${note.length}자",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    placeholder = { Text("비밀 메모를 입력하세요") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    ),
                    textStyle = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
                )
                Button(
                    onClick = {
                        val saved = SecureNotesManager.save(activity, note)
                        Toast.makeText(
                            activity,
                            if (saved) "보안 메모를 암호화해 저장했습니다." else "암호화 저장에 실패했습니다. 잠금을 해제하지 않으면 지울 수 있습니다.",
                            Toast.LENGTH_LONG
                        ).show()
                        if (saved) finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("암호화해 저장", fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) { Text("취소") }
            }
        }
    }
}
