package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.IntruderLog
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.EvidenceItem
import com.example.util.EvidenceReportRenderer
import com.example.util.GeminiClient
import com.example.util.ReportRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ChatMessage(val fromUser: Boolean, val text: String)

/**
 * 침입 증거를 골라서
 * 1) AI에게 자연어로 질문하고
 * 2) 리포트 이미지(PNG)로 내려받는 화면.
 */
@Composable
fun EvidenceAiScreen(
    logs: List<IntruderLog>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA) }

    val selected = remember { mutableStateListOf<String>() }
    var reportTitle by remember { mutableStateOf("침입 시도 리포트") }
    var reportPrompt by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    val chat = remember { mutableStateListOf<ChatMessage>() }
    var isLoading by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var showKeyDialog by remember { mutableStateOf(false) }

    fun selectedItems(): List<EvidenceItem> = logs.filter { selected.contains(it.id) }.map { log ->
        EvidenceItem(
            logId = log.id,
            timestamp = log.timestamp,
            appName = log.appName,
            attempts = log.attemptCount,
            usedLockType = log.usedLockType,
            photoPath = log.photoPath,
            videoPath = log.videoPath,
            audioPath = log.audioPath,
            locationText = log.locationText,
            placeName = log.placeName
        )
    }

    // 질문용 프롬프트: 앱이 이미 갖고 있는 정확한 사실 + 질문
    fun buildPrompt(userQuestion: String, items: List<EvidenceItem>): String {
        val facts = buildString {
            appendLine("다음은 'App Lock & Vault' 앱이 기기 안에서 직접 기록한 인증 실패 기록이다. 사실값만 신뢰하고 추측은 '모름'이라고 답하라.")
            items.forEach { item ->
                appendLine("- ${dateFormat.format(Date(item.timestamp))} · 앱: ${item.appName} · ${item.attempts}회 실패 · 사용 인증: ${item.usedLockType}")
                appendLine("  위치: ${item.locationText ?: "기록 없음"} ${item.placeName?.let { "($it)" } ?: ""}")
                appendLine("  증거: 사진=${if (item.photoPath != null) "있음" else "없음"}, 영상=${if (item.videoPath != null) "있음" else "없음"}, 음성=${if (item.audioPath != null) "있음" else "없음"}")
            }
            if (items.none { it.locationText != null }) {
                appendLine("(위치 기록은 이 기능을 켠 이후 수집된 시도부터만 존재한다.)")
            }
        }
        return "$facts\n\n사용자 질문: $userQuestion"
    }

    Column(Modifier.fillMaxSize().background(CyberBgDark).padding(OneUiPad)) {
        // 헤더
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(CyberCardDark).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("‹", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("증거 AI 분석", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("기록 ${selected.size}건 선택됨", color = TextSecondary, fontSize = 12.sp)
            }
            Text(
                text = "AI 키",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x2200F0FF)).clickable { showKeyDialog = true }.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // 증거 선택
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(logs.size) { index ->
                val log = logs[index]
                val isSelected = selected.contains(log.id)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0x2200F0FF) else CyberCardDark)
                        .clickable {
                            if (isSelected) selected.remove(log.id) else selected.add(log.id)
                        }
                        .padding(10.dp)
                ) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF20293C)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            log.photoPath != null -> AsyncImage(
                                model = File(log.photoPath),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            log.videoPath != null -> Icon(Icons.Default.Videocam, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                            else -> Icon(Icons.Default.Image, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(log.appName.take(18), color = TextPrimary, fontSize = 14.sp, maxLines = 1)
                        Text(
                            "${dateFormat.format(Date(log.timestamp))} · ${log.attemptCount}회",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = log.locationText?.let { "📍 ${log.placeName ?: it}" } ?: "📍 위치 기록 없음",
                            color = if (log.locationText != null) NeonGreen else TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (log.photoPath != null) Icon(Icons.Default.PhotoCamera, "사진", tint = NeonCyan, modifier = Modifier.size(15.dp))
                        if (log.audioPath != null) Text("🎙", fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(20.dp).clip(CircleShape).background(if (isSelected) NeonCyan else Color(0x55000000)),
                        contentAlignment = Alignment.Center
                    ) { if (isSelected) Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // 리포트 생성
        Column {
            OutlinedTextField(
                value = reportPrompt,
                onValueChange = { reportPrompt = it },
                singleLine = true,
                placeholder = { Text("리포트 지시 (예: 간결하게, 위치 강조)", color = TextSecondary, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = CyberBorder,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = NeonCyan
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val items = selectedItems()
                    if (items.isEmpty()) {
                        toast = "증거를 먼저 선택해 주세요."
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        val file = withContext(Dispatchers.IO) {
                            EvidenceReportRenderer.render(context, items, ReportRequest(title = reportTitle, prompt = reportPrompt))
                        }
                        isLoading = false
                        toast = if (file != null) "리포트를 저장했습니다: Pictures/AppLockReports" else "리포트 생성에 실패했습니다."
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) { Text("리포트 이미지 만들기 (PNG)", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))

            // AI 질문
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text("예: 언제 어디서 시도했나요?", color = TextSecondary, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen, unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = NeonGreen
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val items = selectedItems()
                    val asked = question.trim()
                    if (items.isEmpty() || asked.isEmpty()) return@IconButton
                    chat.add(ChatMessage(true, asked))
                    question = ""
                    scope.launch {
                        isLoading = true
                        val images = items.mapNotNull { it.photoPath }.mapNotNull { path ->
                            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()?.let { bitmap ->
                                val bytes = java.io.ByteArrayOutputStream().use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out); out.toByteArray()
                                }
                                bitmap.recycle(); bytes
                            }
                        }
                        val result = withContext(Dispatchers.IO) { GeminiClient.ask(context, buildPrompt(asked, items), images) }
                        isLoading = false
                        when (result) {
                            is GeminiClient.Result.Success -> chat.add(ChatMessage(false, result.text))
                            is GeminiClient.Result.Failure -> chat.add(ChatMessage(false, "⚠ ${result.reason}"))
                        }
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "질문", tint = NeonGreen)
                }
            }

            if (isLoading) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("AI 가 증거를 확인하는 중...", color = TextSecondary, fontSize = 11.sp)
                }
            }

            // 대화 기록
            if (chat.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(chat.size) { index ->
                        val message = chat[index]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (message.fromUser) Color(0x2200F0FF) else CyberCardDark)
                                .padding(10.dp)
                        ) {
                            Text(
                                if (message.fromUser) "질문" else "AI",
                                color = if (message.fromUser) NeonCyan else NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(message.text, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    toast?.let {
        Text(it, color = NeonRed, fontSize = 11.sp, modifier = Modifier.padding(horizontal = OneUiPad, vertical = 6.dp))
    }

    if (showKeyDialog) {
        AiKeyDialog(
            onDismiss = { showKeyDialog = false },
            onSaved = { toast = "API 키를 암호화해 저장했습니다." }
        )
    }
}

private val OneUiPad = 16.dp