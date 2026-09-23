package com.example

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.crypto.SecureNotesManager
import com.example.ui.theme.MyApplicationTheme
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
            var note by mutableStateOf(SecureNotesManager.load(this))
            LaunchedEffect(Unit) {
                delay(AUTO_LOCK_MS)
                finish()
            }
            Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("보안 메모")
                Text("AES‑GCM 암호화 저장 · 1분 후 자동 잠금")
                TextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth().weight(1f), placeholder = { Text("비밀 메모를 입력하세요") })
                Button(onClick = { SecureNotesManager.save(this@SecureNotesActivity, note); finish() }, modifier = Modifier.fillMaxWidth()) { Text("암호화해 저장") }
                OutlinedButton(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) { Text("취소") }
            }
        }
    }
}
