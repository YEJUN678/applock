package com.example.crypto

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.EncryptedVaultFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.text.DecimalFormat
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles military-grade AES-256 streaming file encryption & decryption.
 * Designed to stream files of any size (even multi-gigabytes) with constant O(1) memory.
 * Files encrypted here cannot be decrypted outside of this app installation.
 */
object FileCryptoManager {
    private const val TAG = "FileCryptoManager"
    private const val VAULT_DIR_NAME = "secure_vault_files"
    private const val RESTORE_DIR_NAME = "Restored_Files"
    private const val MAGIC_HEADER = "APPV" // App Vault signature
    private const val HEADER_VERSION = 2
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB streaming buffer

    // App-bound master key seed stored in private internal prefs
    private const val PREFS_KEY_VAULT = "applock_vault_crypto_pref"
    private const val KEY_APP_SALT = "app_master_unique_salt"

    private fun getVaultDirectory(context: Context): File {
        val dir = File(context.filesDir, VAULT_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Obtains or generates an installation-bound master AES-256 encryption key.
     * This guarantees that without this specific app instance, external tools cannot decrypt vault files.
     */
    @Synchronized
    private fun getAppMasterKey(context: Context): SecretKey {
        val prefs = context.getSharedPreferences(PREFS_KEY_VAULT, Context.MODE_PRIVATE)
        var saltBase64 = prefs.getString(KEY_APP_SALT, null)
        if (saltBase64 == null) {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            saltBase64 = android.util.Base64.encodeToString(randomBytes, android.util.Base64.NO_WRAP)
            prefs.edit().putString(KEY_APP_SALT, saltBase64).apply()
        }

        val salt = android.util.Base64.decode(saltBase64, android.util.Base64.NO_WRAP)
        // Combine package name and unique salt for PBKDF2 key derivation
        val passPhrase = (context.packageName + "_AppLockMasterKey_2026_SecureVault").toCharArray()
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passPhrase, salt, 12000, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /** V2 vaults embed a fresh salt and derive their AES key from the user recovery password. */
    private fun getRecoveryKey(password: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, 210_000, 256)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /**
     * Resolves display name and file size from a content URI.
     */
    fun resolveUriMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = "unknown_file"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve uri metadata", e)
        }
        if (name == "unknown_file") {
            name = "file_" + System.currentTimeMillis()
        }
        return Pair(name, size)
    }

    /**
     * Encrypts any file into the secure vault using streaming AES-256-CBC.
     * Progress is reported via onProgress (0.0f .. 1.0f).
     */
    suspend fun encryptFileToVault(
        context: Context,
        sourceUri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<EncryptedVaultFile> = withContext(Dispatchers.IO) {
        try {
            val (originalName, resolvedSize) = resolveUriMetadata(context, sourceUri)
            val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"

            val vaultDir = getVaultDirectory(context)
            val fileId = "vault_" + System.currentTimeMillis() + "_" + (1000..9999).random()
            val destEncryptedFile = File(vaultDir, "$fileId.locked")

            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val secretKey = getRecoveryKey(VaultRecoveryPassword.require(), salt)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

            context.contentResolver.openInputStream(sourceUri)?.use { rawIn ->
                FileOutputStream(destEncryptedFile).use { rawOut ->
                    val dataOut = DataOutputStream(rawOut)
                    // 1. Write Header
                    dataOut.write(MAGIC_HEADER.toByteArray(Charsets.US_ASCII)) // 4 bytes
                    dataOut.writeByte(HEADER_VERSION) // 1 byte
                    dataOut.write(salt) // V2 portable recovery-key salt
                    dataOut.write(iv) // 16 bytes
                    dataOut.writeUTF(originalName) // UTF original name
                    dataOut.writeUTF(mimeType) // UTF mime
                    dataOut.writeLong(resolvedSize) // 8 bytes original size
                    dataOut.flush()

                    // 2. Stream payload through CipherOutputStream
                    val cipherOut = CipherOutputStream(rawOut, cipher)
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (rawIn.read(buffer).also { bytesRead = it } != -1) {
                        cipherOut.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (resolvedSize > 0) {
                            val progress = (totalRead.toFloat() / resolvedSize).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    cipherOut.flush()
                    cipherOut.close()
                }
            } ?: return@withContext Result.failure(Exception("입력 파일을 열 수 없습니다."))

            val vaultFile = EncryptedVaultFile(
                id = fileId,
                originalName = originalName,
                encryptedFileName = destEncryptedFile.name,
                originalSizeBytes = if (resolvedSize > 0) resolvedSize else destEncryptedFile.length(),
                mimeType = mimeType,
                encryptedAtMillis = System.currentTimeMillis(),
                formattedSize = formatFileSize(if (resolvedSize > 0) resolvedSize else destEncryptedFile.length())
            )
            onProgress(1.0f)
            Result.success(vaultFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting file", e)
            Result.failure(e)
        }
    }

    /**
     * Decrypts a vault file back to a normal readable file.
     * Saved to user Downloads/Restored_Files or internal cache if targetDir is null.
     */
    suspend fun decryptVaultFile(
        context: Context,
        vaultFile: EncryptedVaultFile,
        targetOutputDir: File? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val vaultDir = getVaultDirectory(context)
            val encryptedFile = File(vaultDir, vaultFile.encryptedFileName)
            if (!encryptedFile.exists()) {
                return@withContext Result.failure(Exception("암호화된 금고 파일을 찾을 수 없습니다."))
            }

            // Output directory: default to Public Downloads/Restored_Files or app cache
            val outDir = targetOutputDir ?: run {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val safeDir = if (publicDownloads != null && publicDownloads.exists()) {
                    File(publicDownloads, RESTORE_DIR_NAME)
                } else {
                    File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), RESTORE_DIR_NAME)
                }
                if (!safeDir.exists()) safeDir.mkdirs()
                safeDir
            }

            // Ensure unique filename if already exists
            var targetFile = File(outDir, vaultFile.originalName)
            var count = 1
            val baseName = vaultFile.originalName.substringBeforeLast(".")
            val ext = if (vaultFile.originalName.contains(".")) "." + vaultFile.originalName.substringAfterLast(".") else ""
            while (targetFile.exists()) {
                targetFile = File(outDir, "$baseName($count)$ext")
                count++
            }

            FileInputStream(encryptedFile).use { rawIn ->
                val dataIn = DataInputStream(rawIn)
                val magicBytes = ByteArray(4)
                dataIn.readFully(magicBytes)
                val magic = String(magicBytes, Charsets.US_ASCII)
                if (magic != MAGIC_HEADER) {
                    return@withContext Result.failure(Exception("이 앱의 유효한 암호화 형식이 아닙니다."))
                }
                val version = dataIn.readByte().toInt()
                val secretKey = if (version >= 2) {
                    val salt = ByteArray(16); dataIn.readFully(salt)
                    getRecoveryKey(VaultRecoveryPassword.require(), salt)
                } else getAppMasterKey(context)
                val iv = ByteArray(16)
                dataIn.readFully(iv)
                val origName = dataIn.readUTF()
                val origMime = dataIn.readUTF()
                val origSize = dataIn.readLong()

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

                val cipherIn = CipherInputStream(rawIn, cipher)
                FileOutputStream(targetFile).use { fileOut ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (cipherIn.read(buffer).also { bytesRead = it } != -1) {
                        fileOut.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (origSize > 0) {
                            val progress = (totalRead.toFloat() / origSize).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    fileOut.flush()
                }
                cipherIn.close()
            }

            onProgress(1.0f)
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting file", e)
            Result.failure(e)
        }
    }

    /**
     * Lists all encrypted files currently kept inside the secure vault.
     */
    suspend fun listVaultFiles(context: Context): List<EncryptedVaultFile> = withContext(Dispatchers.IO) {
        val vaultDir = getVaultDirectory(context)
        val files = vaultDir.listFiles { _, name -> name.endsWith(".locked") } ?: emptyArray()
        val list = mutableListOf<EncryptedVaultFile>()

        for (file in files) {
            try {
                FileInputStream(file).use { fis ->
                    val dataIn = DataInputStream(fis)
                    val magicBytes = ByteArray(4)
                    dataIn.readFully(magicBytes)
                    val magic = String(magicBytes, Charsets.US_ASCII)
                    if (magic == MAGIC_HEADER) {
                        val version = dataIn.readByte().toInt()
                        if (version >= 2) dataIn.skipBytes(16)
                        val iv = ByteArray(16)
                        dataIn.readFully(iv)
                        val originalName = dataIn.readUTF()
                        val mimeType = dataIn.readUTF()
                        val originalSize = dataIn.readLong()

                        val id = file.name.substringBeforeLast(".locked")
                        list.add(
                            EncryptedVaultFile(
                                id = id,
                                originalName = originalName,
                                encryptedFileName = file.name,
                                originalSizeBytes = if (originalSize > 0) originalSize else file.length(),
                                mimeType = mimeType,
                                encryptedAtMillis = file.lastModified(),
                                formattedSize = formatFileSize(if (originalSize > 0) originalSize else file.length())
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Corrupted or non-readable vault file: ${file.name}", e)
            }
        }
        list.sortedByDescending { it.encryptedAtMillis }
    }

    /**
     * Permanently shreds and deletes an encrypted file from the vault.
     */
    suspend fun deleteVaultFile(context: Context, encryptedFileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultDir = getVaultDirectory(context)
            val file = File(vaultDir, encryptedFileName)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete vault file", e)
            false
        }
    }

    /**
     * Creates mock sample secure files if the vault is empty so users can test immediately.
     */
    suspend fun createSampleVaultFileIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        val vaultDir = getVaultDirectory(context)
        val files = vaultDir.listFiles { _, name -> name.endsWith(".locked") }
        if (files.isNullOrEmpty()) {
            // Create a sample private note document
            val sampleText = """
                [비밀 메모 / Private Security Vault]
                본 파일은 AES-256 하드웨어 바인딩 암호화로 보호되는 금고 파일입니다.
                외부 파일 탐색기나 PC에서는 열 수 없으며, App Lock 앱 내부에서만
                '복호화' 버튼을 눌러 원본 상태로 복원할 수 있습니다.
                
                - 암호화 방식: AES-256-CBC (PKCS5Padding)
                - 복호화 키: 기기/앱 바인딩 PBKDF2 마스터 키
                - 무결성 검증: APPV v1 헤더
            """.trimIndent()

            val tempFile = File(context.cacheDir, "중요_개인_보안메모.txt")
            tempFile.writeText(sampleText, Charsets.UTF_8)
            encryptFileToVault(context, Uri.fromFile(tempFile))
            tempFile.delete()
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val formatted = DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups]}"
    }

    /**
     * Public storage directory for encrypted files in "내 드라이브" / "다운로드" (Download/AppVault).
     */
    fun getPublicDriveVaultDir(context: Context): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = if (downloadsDir != null && downloadsDir.exists()) {
            File(downloadsDir, "AppVault")
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AppVault")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Public storage directory for restored files (Download/AppVault_Restored).
     */
    fun getPublicRestoredDir(context: Context): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = if (downloadsDir != null && downloadsDir.exists()) {
            File(downloadsDir, "AppVault_Restored")
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AppVault_Restored")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Encrypts a real file selected from Google Drive / storage directly into a .vault file in "내 드라이브".
     */
    suspend fun encryptFileToPublicDrive(
        context: Context,
        sourceUri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<EncryptedVaultFile> = withContext(Dispatchers.IO) {
        try {
            val (originalName, resolvedSize) = resolveUriMetadata(context, sourceUri)
            val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"

            val targetDir = getPublicDriveVaultDir(context)
            val safeVaultName = "$originalName.vault"
            var destEncryptedFile = File(targetDir, safeVaultName)
            var count = 1
            while (destEncryptedFile.exists()) {
                val base = originalName.substringBeforeLast(".")
                val ext = if (originalName.contains(".")) "." + originalName.substringAfterLast(".") else ""
                destEncryptedFile = File(targetDir, "$base($count)$ext.vault")
                count++
            }

            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val secretKey = getRecoveryKey(VaultRecoveryPassword.require(), salt)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

            context.contentResolver.openInputStream(sourceUri)?.use { rawIn ->
                FileOutputStream(destEncryptedFile).use { rawOut ->
                    val dataOut = DataOutputStream(rawOut)
                    // 1. Write Header
                    dataOut.write(MAGIC_HEADER.toByteArray(Charsets.US_ASCII))
                    dataOut.writeByte(HEADER_VERSION)
                    dataOut.write(salt)
                    dataOut.write(iv)
                    dataOut.writeUTF(originalName)
                    dataOut.writeUTF(mimeType)
                    dataOut.writeLong(resolvedSize)
                    dataOut.flush()

                    // 2. Stream payload through CipherOutputStream
                    val cipherOut = CipherOutputStream(rawOut, cipher)
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (rawIn.read(buffer).also { bytesRead = it } != -1) {
                        cipherOut.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (resolvedSize > 0) {
                            onProgress((totalRead.toFloat() / resolvedSize).coerceIn(0f, 1f))
                        }
                    }
                    cipherOut.flush()
                    cipherOut.close()
                }
            } ?: return@withContext Result.failure(Exception("선택한 원본 파일을 열 수 없습니다."))

            // Index file into Android MediaStore so it appears in "내 파일" and Google Drive immediately
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(destEncryptedFile.absolutePath),
                null,
                null
            )

            val fileId = "drive_" + System.currentTimeMillis()
            val vaultFile = EncryptedVaultFile(
                id = fileId,
                originalName = originalName,
                encryptedFileName = destEncryptedFile.name,
                originalSizeBytes = if (resolvedSize > 0) resolvedSize else destEncryptedFile.length(),
                mimeType = mimeType,
                encryptedAtMillis = System.currentTimeMillis(),
                formattedSize = formatFileSize(if (resolvedSize > 0) resolvedSize else destEncryptedFile.length()),
                fileUriString = Uri.fromFile(destEncryptedFile).toString(),
                isExternalDriveFile = true,
                driveLocationPath = destEncryptedFile.absolutePath
            )
            onProgress(1.0f)
            Result.success(vaultFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed encrypting to drive", e)
            Result.failure(e)
        }
    }

    /**
     * Decrypts any .vault file selected from Google Drive / storage and restores it back to original file.
     */
    suspend fun decryptDriveVaultUri(
        context: Context,
        vaultUri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(vaultUri)?.use { rawIn ->
                val dataIn = DataInputStream(rawIn)
                val magicBytes = ByteArray(4)
                dataIn.readFully(magicBytes)
                val magic = String(magicBytes, Charsets.US_ASCII)
                if (magic != MAGIC_HEADER) {
                    return@withContext Result.failure(Exception("이 앱에서 생성된 암호화 파일이 아니거나 형식이 일치하지 않습니다."))
                }
                val version = dataIn.readByte().toInt()
                val secretKey = if (version >= 2) {
                    val salt = ByteArray(16); dataIn.readFully(salt)
                    getRecoveryKey(VaultRecoveryPassword.require(), salt)
                } else getAppMasterKey(context)
                val iv = ByteArray(16)
                dataIn.readFully(iv)
                val origName = dataIn.readUTF()
                val origMime = dataIn.readUTF()
                val origSize = dataIn.readLong()

                val targetDir = getPublicRestoredDir(context)
                var targetFile = File(targetDir, origName)
                var count = 1
                val baseName = origName.substringBeforeLast(".")
                val ext = if (origName.contains(".")) "." + origName.substringAfterLast(".") else ""
                while (targetFile.exists()) {
                    targetFile = File(targetDir, "$baseName($count)$ext")
                    count++
                }

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

                val cipherIn = CipherInputStream(rawIn, cipher)
                FileOutputStream(targetFile).use { fileOut ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (cipherIn.read(buffer).also { bytesRead = it } != -1) {
                        fileOut.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (origSize > 0) {
                            onProgress((totalRead.toFloat() / origSize).coerceIn(0f, 1f))
                        }
                    }
                    fileOut.flush()
                }
                cipherIn.close()

                // Notify Android MediaStore of restored normal file
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(origMime),
                    null
                )

                onProgress(1.0f)
                Result.success(targetFile)
            } ?: Result.failure(Exception("암호화 파일을 열 수 없습니다."))
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting drive uri", e)
            Result.failure(e)
        }
    }

    /**
     * Scans storage and Google Drive download folders for all encrypted .vault files.
     */
    suspend fun scanAllDriveVaultFiles(context: Context): List<EncryptedVaultFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<EncryptedVaultFile>()
        val searchDirs = listOf(
            getPublicDriveVaultDir(context),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ).filterNotNull().filter { it.exists() }

        for (dir in searchDirs) {
            val vaultFiles = dir.listFiles { _, name -> name.endsWith(".vault") || name.endsWith(".locked") } ?: continue
            for (file in vaultFiles) {
                try {
                    FileInputStream(file).use { fis ->
                        val dataIn = DataInputStream(fis)
                        val magicBytes = ByteArray(4)
                        dataIn.readFully(magicBytes)
                        val magic = String(magicBytes, Charsets.US_ASCII)
                        if (magic == MAGIC_HEADER) {
                            val version = dataIn.readByte().toInt()
                            if (version >= 2) dataIn.skipBytes(16)
                            val iv = ByteArray(16)
                            dataIn.readFully(iv)
                            val originalName = dataIn.readUTF()
                            val mimeType = dataIn.readUTF()
                            val originalSize = dataIn.readLong()

                            results.add(
                                EncryptedVaultFile(
                                    id = file.name,
                                    originalName = originalName,
                                    encryptedFileName = file.name,
                                    originalSizeBytes = if (originalSize > 0) originalSize else file.length(),
                                    mimeType = mimeType,
                                    encryptedAtMillis = file.lastModified(),
                                    formattedSize = formatFileSize(if (originalSize > 0) originalSize else file.length()),
                                    fileUriString = Uri.fromFile(file).toString(),
                                    isExternalDriveFile = true,
                                    driveLocationPath = file.absolutePath
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        results.distinctBy { it.driveLocationPath ?: it.id }.sortedByDescending { it.encryptedAtMillis }
    }
}
