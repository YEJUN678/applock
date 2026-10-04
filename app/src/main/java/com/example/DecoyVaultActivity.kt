package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DecoyItem
import com.example.util.DecoyVaultData
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 미끼(디코이) 금고. 미끼 PIN으로 열렸을 때 보여지는 화면입니다.
 * 실제 파일은 없고, 평범하게 채워진 목록만 보여 줍니다.
 */
class DecoyVaultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme { DecoyVaultScreen() }
        }
    }
}

@Composable
private fun DecoyVaultScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var items by remember { mutableStateOf(DecoyVaultData.getItems(context)) }
    var filter by remember { mutableStateOf("전체") }
    var detail by remember { mutableStateOf<DecoyItem?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.KOREA) }

    val types = remember(items) { listOf("전체") + items.map { it.type }.distinct() }
    val visible = remember(items, filter) { if (filter == "전체") items else items.filter { it.type == filter } }

    Column(Modifier.fillMaxSize().background(CyberBgDark).padding(horizontal = 18.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("내 보관함", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("항목 ${items.size}개 · 최근 사용 순", color = TextSecondary, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .background(Color(0x2200F0FF), RoundedCornerShape(12.dp))
                    .clickable { showAdd = true }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) { Text("+ 새 메모", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            types.forEach { type ->
                FilterChip(
                    selected = filter == type,
                    onClick = { filter = type },
                    label = { Text(type, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("이 분류에는 항목이 없습니다", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberCardDark, RoundedCornerShape(14.dp))
                            .clickable { detail = item }
                            .padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).background(Color(0x2200F0FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text(item.type.take(1), color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = TextPrimary, fontSize = 15.sp, maxLines = 1)
                            Text(
                                text = "${item.type} · ${dateFormat.format(Date(item.createdAt))}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(item.sizeLabel, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    detail?.let { selected ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(selected.title, color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("종류: ${selected.type}", color = TextSecondary, fontSize = 12.sp)
                    Text("크기: ${selected.sizeLabel}", color = TextSecondary, fontSize = 12.sp)
                    Text("만든 날짜: ${dateFormat.format(Date(selected.createdAt))}", color = TextSecondary, fontSize = 12.sp)
                    if (!selected.note.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(selected.note, color = TextPrimary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("이 항목은 기본 보기 전용입니다.", color = TextSecondary, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("닫기", color = NeonCyan) } },
            dismissButton = {
                TextButton(onClick = {
                    DecoyVaultData.delete(context, selected.id)
                    items = DecoyVaultData.getItems(context)
                    detail = null
                }) { Text("삭제", color = Color(0xFFFF5252), fontSize = 12.sp) }
            },
            containerColor = CyberCardDark
        )
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("새 메모", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("제목", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        DecoyVaultData.addMemo(context, title)
                        items = DecoyVaultData.getItems(context)
                        showAdd = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    enabled = title.isNotBlank()
                ) { Text("저장", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("취소", color = TextSecondary) } },
            containerColor = CyberCardDark
        )
    }
}