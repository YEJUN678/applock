package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.crypto.FileCryptoManager
import com.example.crypto.VaultRecoveryPassword
import com.example.model.EncryptedVaultFile
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileVaultScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Tab state: 0 = "내 드라이브 암호화 파일 (.vault)", 1 = "내부 보안 금고"
    var selectedTab by remember { mutableIntStateOf(0) }

    val driveVaultFiles = remember { mutableStateListOf<EncryptedVaultFile>() }
    val internalVaultFiles = remember { mutableStateListOf<EncryptedVaultFile>() }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("전체") }

    // Progress State for streaming encryption/decryption
    var isProcessing by remember { mutableStateOf(false) }
    var processingTitle by remember { mutableStateOf("") }
    var processingSubtext by remember { mutableStateOf("") }
    var processingProgress by remember { mutableFloatStateOf(0f) }

    // Dialogs
    var previewFile by remember { mutableStateOf<Pair<EncryptedVaultFile, String?>?>(null) }
    var fileToDelete by remember { mutableStateOf<EncryptedVaultFile?>(null) }
    var decryptedSuccessFile by remember { mutableStateOf<File?>(null) }
    var showRecoveryPasswordDialog by remember { mutableStateOf(!VaultRecoveryPassword.isSet()) }
    var recoveryPassword by remember { mutableStateOf("") }
    var recoveryPasswordConfirm by remember { mutableStateOf("") }

    // Reload files from both drive public storage and internal vault
    fun reloadFiles() {
        coroutineScope.launch {
            isLoading = true
            val driveFiles = FileCryptoManager.scanAllDriveVaultFiles(context)
            val internalFiles = FileCryptoManager.listVaultFiles(context)
            driveVaultFiles.clear()
            driveVaultFiles.addAll(driveFiles)
            internalVaultFiles.clear()
            internalVaultFiles.addAll(internalFiles)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (VaultRecoveryPassword.isSet()) FileCryptoManager.createSampleVaultFileIfEmpty(context)
        reloadFiles()
    }

    if (showRecoveryPasswordDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("금고 복구 비밀번호") },
            text = {
                Column {
                    Text("새 .vault 파일은 이 비밀번호로 암호화됩니다. 재설치·다른 기기에서도 같은 비밀번호로 복원할 수 있습니다. 비밀번호는 기기에 저장하지 않습니다.", color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = recoveryPassword, onValueChange = { recoveryPassword = it }, label = { Text("복구 비밀번호 (8자 이상)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    OutlinedTextField(value = recoveryPasswordConfirm, onValueChange = { recoveryPasswordConfirm = it }, label = { Text("비밀번호 확인") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (recoveryPassword.length >= 8 && recoveryPassword == recoveryPasswordConfirm) {
                        VaultRecoveryPassword.set(recoveryPassword.toCharArray())
                        recoveryPassword = ""; recoveryPasswordConfirm = ""; showRecoveryPasswordDialog = false
                        coroutineScope.launch { FileCryptoManager.createSampleVaultFileIfEmpty(context); reloadFiles() }
                    }
                }) { Text("금고 열기") }
            }
        )
    }

    // 1. Encrypt Real File: pick from Google Drive / storage -> creates .vault file in My Drive (Download/AppVault)
    val encryptRealFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isProcessing = true
                processingTitle = "내 드라이브 파일 암호화 (.vault 생성)"
                processingProgress = 0f

                var successCount = 0
                for ((index, uri) in uris.withIndex()) {
                    val (name, _) = FileCryptoManager.resolveUriMetadata(context, uri)
                    processingSubtext = "[$name] (${index + 1}/${uris.size}) 암호화 진행 중..."
                    val result = FileCryptoManager.encryptFileToPublicDrive(context, uri) { prog ->
                        processingProgress = (index + prog) / uris.size
                    }
                    if (result.isSuccess) {
                        successCount++
                    }
                }

                isProcessing = false
                reloadFiles()
                selectedTab = 0 // Switch to drive tab to see newly created .vault files
                Toast.makeText(
                    context,
                    "${successCount}개 파일이 '내 드라이브 / Download/AppVault'에 .vault 암호화 파일로 생성되었습니다.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // 2. Decrypt .vault File: pick any .vault file directly from Google Drive / storage -> restores original file
    val decryptFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isProcessing = true
                processingTitle = "내 드라이브 암호화 파일 복구 (.vault)"
                processingProgress = 0f

                var restoredLastFile: File? = null
                var successCount = 0

                for ((index, uri) in uris.withIndex()) {
                    val (name, _) = FileCryptoManager.resolveUriMetadata(context, uri)
                    processingSubtext = "[$name] (${index + 1}/${uris.size}) 복호화 및 복구 중..."
                    val result = FileCryptoManager.decryptDriveVaultUri(context, uri) { prog ->
                        processingProgress = (index + prog) / uris.size
                    }
                    if (result.isSuccess) {
                        restoredLastFile = result.getOrNull()
                        successCount++
                    } else {
                        Toast.makeText(context, "복호화 실패: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                isProcessing = false
                reloadFiles()
                if (restoredLastFile != null) {
                    decryptedSuccessFile = restoredLastFile
                    Toast.makeText(context, "${successCount}개 파일 복호화 성공! 정상 복구되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Single file decryption
    fun decryptSingleVaultFile(item: EncryptedVaultFile) {
        coroutineScope.launch {
            isProcessing = true
            processingTitle = "파일 복호화 진행 중..."
            processingSubtext = "[${item.originalName}] 복원 중..."
            processingProgress = 0f

            val result = if (item.isExternalDriveFile && item.driveLocationPath != null) {
                FileCryptoManager.decryptDriveVaultUri(context, Uri.fromFile(File(item.driveLocationPath))) { prog ->
                    processingProgress = prog
                }
            } else {
                FileCryptoManager.decryptVaultFile(context, item) { prog ->
                    processingProgress = prog
                }
            }

            isProcessing = false
            if (result.isSuccess) {
                decryptedSuccessFile = result.getOrNull()
                Toast.makeText(context, "복호화 성공! 원래 파일로 복구되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "복호화 실패: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Batch decrypt current tab files
    fun decryptAllCurrentTabFiles() {
        val currentList = if (selectedTab == 0) driveVaultFiles else internalVaultFiles
        if (currentList.isEmpty()) return

        coroutineScope.launch {
            isProcessing = true
            processingTitle = "전체 파일 일괄 복호화 중..."
            processingProgress = 0f

            var success = 0
            for ((idx, file) in currentList.withIndex()) {
                processingSubtext = "[${file.originalName}] (${idx + 1}/${currentList.size})"
                val res = if (file.isExternalDriveFile && file.driveLocationPath != null) {
                    FileCryptoManager.decryptDriveVaultUri(context, Uri.fromFile(File(file.driveLocationPath))) { prog ->
                        processingProgress = (idx + prog) / currentList.size
                    }
                } else {
                    FileCryptoManager.decryptVaultFile(context, file) { prog ->
                        processingProgress = (idx + prog) / currentList.size
                    }
                }
                if (res.isSuccess) success++
            }

            isProcessing = false
            Toast.makeText(
                context,
                "총 ${success}개 파일이 다운로드 폴더로 정상 복호화되었습니다.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Active list based on tab
    val activeFilesList = if (selectedTab == 0) driveVaultFiles else internalVaultFiles

    val filteredFiles = remember(activeFilesList.toList(), searchQuery, selectedCategory) {
        activeFilesList.filter { file ->
            val matchesQuery = searchQuery.isBlank() ||
                    file.originalName.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "사진" -> isImageFile(file.originalName, file.mimeType)
                "동영상" -> isVideoFile(file.originalName, file.mimeType)
                "문서" -> isDocumentFile(file.originalName, file.mimeType)
                "기타" -> !isImageFile(file.originalName, file.mimeType) &&
                        !isVideoFile(file.originalName, file.mimeType) &&
                        !isDocumentFile(file.originalName, file.mimeType)
                else -> true
            }
            matchesQuery && matchesCategory
        }
    }

    val totalBytes = remember(activeFilesList.toList()) {
        activeFilesList.sumOf { it.originalSizeBytes }
    }

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "보안 파일 금고 (Drive Vault)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "내 드라이브 암호화 • 복호화 전용 엔진",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { reloadFiles() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Primary Drive Encryption Action Buttons (Select Real File & Find Encrypted File)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        encryptRealFileLauncher.launch(arrayOf("*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("내 드라이브 파일 암호화", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        decryptFilePickerLauncher.launch(arrayOf("*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("암호화 파일 찾아 복구", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // 2. Info Status Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CyberCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (selectedTab == 0) "내 드라이브/저장소 .vault 보관함" else "내부 보안 금고",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${activeFilesList.size}개 파일 (${FileCryptoManager.formatFileSize(totalBytes)})",
                                    fontSize = 11.sp,
                                    color = NeonGreen
                                )
                            }
                        }

                        if (activeFilesList.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { decryptAllCurrentTabFiles() },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("일괄 복호화", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 암호화된 .vault 파일은 본 앱 없이는 절대 열 수 없습니다. 복호화 버튼을 누르면 원본 이름과 확장자로 복원되어 정상 사용이 가능합니다.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // 3. Tab Navigation (내 드라이브 탐색 vs 내부 금고)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "내 드라이브 (.vault) (${driveVaultFiles.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) NeonCyan else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "내부 금고 (${internalVaultFiles.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) NeonCyan else TextSecondary
                        )
                    }
                )
            }

            // 4. Search and Category Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("파일명 검색...", color = TextSecondary, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "검색어 지우기", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberCardBg,
                    unfocusedContainerColor = CyberCardBg,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("전체", "사진", "동영상", "문서", "기타").forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5. Files List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "검색 결과가 없습니다" else "암호화된 파일이 없습니다",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "상단의 '내 드라이브 파일 암호화' 버튼을 눌러 사진, 동영상, 문서를 안전하게 .vault로 암호화하세요.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFiles, key = { it.id }) { item ->
                        VaultFileCard(
                            file = item,
                            onDecrypt = { decryptSingleVaultFile(item) },
                            onShare = {
                                val fileToShare = if (item.driveLocationPath != null) File(item.driveLocationPath) else null
                                if (fileToShare != null && fileToShare.exists()) {
                                    try {
                                        val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fileToShare)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            type = "*/*"
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "암호화 파일 내보내기 / 공유"))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "공유 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = { fileToDelete = item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }

    // 6. Progress Modal Dialog for Streaming Encrypt / Decrypt
    if (isProcessing) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF131A2A),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { processingProgress },
                        color = NeonCyan,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = processingTitle,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = processingSubtext,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { processingProgress },
                        color = NeonCyan,
                        trackColor = CyberBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${(processingProgress * 100).toInt()}% 완료",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // 7. Decrypt Success Dialog with Immediate "Open" & "Share"
    decryptedSuccessFile?.let { restoredFile ->
        AlertDialog(
            onDismissRequest = { decryptedSuccessFile = null },
            icon = {
                Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(32.dp))
            },
            title = { Text("복호화 완료! 파일 복구됨", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("암호화가 성공적으로 해제되어 원본 파일로 복원되었습니다.", color = TextPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("파일명: ${restoredFile.name}", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("저장 위치: ${restoredFile.parent}", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("갤러리나 기본 뷰어 앱에서 바로 열거나 공유할 수 있습니다.", color = TextMuted, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        decryptedSuccessFile = null
                        openFileWithSystem(context, restoredFile)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) {
                    Text("지금 파일 열기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            try {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", restoredFile)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    type = context.contentResolver.getType(uri) ?: "*/*"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "복원 파일 공유"))
                            } catch (_: Exception) {}
                        }
                    ) {
                        Text("공유하기", color = NeonCyan)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(onClick = { decryptedSuccessFile = null }) {
                        Text("닫기", color = TextSecondary)
                    }
                }
            },
            containerColor = Color(0xFF131A2A)
        )
    }

    // 8. Delete Confirmation Dialog
    fileToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = NeonRed, modifier = Modifier.size(30.dp))
            },
            title = { Text("암호화 파일 삭제", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "'${target.originalName}' 암호화 파일을 영구히 삭제하시겠습니까? 삭제된 파일은 복구할 수 없습니다.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (target.isExternalDriveFile && target.driveLocationPath != null) {
                                try {
                                    File(target.driveLocationPath).delete()
                                } catch (_: Exception) {}
                            } else {
                                FileCryptoManager.deleteVaultFile(context, target.encryptedFileName)
                            }
                            reloadFiles()
                            fileToDelete = null
                            Toast.makeText(context, "파일이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White)
                ) {
                    Text("영구 삭제")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { fileToDelete = null }) {
                    Text("취소", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF131A2A)
        )
    }
}

@Composable
private fun VaultFileCard(
    file: EncryptedVaultFile,
    onDecrypt: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(file.encryptedAtMillis) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(file.encryptedAtMillis))
    }

    val (icon, iconTint) = remember(file.originalName, file.mimeType) {
        getFileIconAndTint(file.originalName, file.mimeType)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CyberCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.originalName,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.formattedSize,
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " • $dateStr",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (file.isExternalDriveFile) NeonCyan.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (file.isExternalDriveFile) "📁 내 드라이브 (.vault)" else "🔒 내부 금고",
                            color = if (file.isExternalDriveFile) NeonCyan else NeonGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Actions: Decrypt, Share, Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onDecrypt,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen.copy(alpha = 0.2f),
                        contentColor = NeonGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "복호화",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("복구", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (file.isExternalDriveFile) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "공유",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = NeonRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun getFileIconAndTint(name: String, mime: String): Pair<ImageVector, Color> {
    val lower = name.lowercase()
    return when {
        isImageFile(name, mime) -> Pair(Icons.Default.Image, NeonCyan)
        isVideoFile(name, mime) -> Pair(Icons.Default.VideoFile, NeonPurple)
        lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") || mime.startsWith("audio/") ->
            Pair(Icons.Default.AudioFile, NeonGreen)
        lower.endsWith(".pdf") -> Pair(Icons.Default.PictureAsPdf, NeonRed)
        lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z") || lower.endsWith(".tar") ->
            Pair(Icons.Default.FolderZip, NeonAmber)
        isDocumentFile(name, mime) -> Pair(Icons.Default.InsertDriveFile, Color(0xFF60A5FA))
        else -> Pair(Icons.Default.InsertDriveFile, TextSecondary)
    }
}

private fun isImageFile(name: String, mime: String): Boolean {
    val l = name.lowercase()
    return mime.startsWith("image/") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png") || l.endsWith(".webp") || l.endsWith(".gif")
}

private fun isVideoFile(name: String, mime: String): Boolean {
    val l = name.lowercase()
    return mime.startsWith("video/") || l.endsWith(".mp4") || l.endsWith(".mkv") || l.endsWith(".avi") || l.endsWith(".mov")
}

private fun isDocumentFile(name: String, mime: String): Boolean {
    val l = name.lowercase()
    return l.endsWith(".pdf") || l.endsWith(".txt") || l.endsWith(".doc") || l.endsWith(".docx") ||
            l.endsWith(".xls") || l.endsWith(".xlsx") || l.endsWith(".ppt") || l.endsWith(".pptx")
}

private fun openFileWithSystem(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "파일을 열 수 있는 전용 뷰어 앱이 없습니다.", Toast.LENGTH_SHORT).show()
    }
}
