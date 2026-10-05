package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/** AI/리포트에서 쓰는 증거 묶음. */
data class EvidenceItem(
    val logId: String,
    val timestamp: Long,
    val appName: String,
    val attempts: Int,
    val usedLockType: String,
    val photoPath: String?,
    val videoPath: String?,
    val audioPath: String?,
    val locationText: String?,
    val placeName: String?
)

/** 리포트를 만들 때 쓰는 질문/지시. */
data class ReportRequest(
    val title: String,
    val prompt: String,
    val includeTimeline: Boolean = true,
    val includeThumbnails: Boolean = true,
    val includeMapLink: Boolean = true
)

/**
 * 침입 증거를 PNG 리포트로 만든다.
 * 완전 로컬 렌더링이라 API 비용이 없고, 생성된 이미지는 사진첩에 저장된다.
 */
object EvidenceReportRenderer {

    private const val WIDTH = 1080
    private const val ROW_HEIGHT = 168
    private const val HEADER_HEIGHT = 260

    // ?붾젅??(Int 由ы꽣??
    private const val C_BG = 0xFF0B1220.toInt()
    private const val C_ACCENT = 0xFF00E5FF.toInt()
    private const val C_CARD = 0xFF16203A.toInt()
    private const val C_TEXT = 0xFFF1F5F9.toInt()
    private const val C_SUB = 0xFF94A3B8.toInt()
    private const val C_TEXT2 = 0xFFCBD5E1.toInt()
    private const val C_EMPTY = 0xFF334155.toInt()
    private const val C_LINE = 0xFF1E293B.toInt()
    private const val C_MUTED = 0xFF64748B.toInt()

    private val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA)
    private val dayFormat = SimpleDateFormat("yyyy.MM.dd (EEE)", Locale.KOREA)

    /** 리포트 이미지를 만들어 Pictures/AppLockReports 아래에 저장하고 파일을 돌려준다. */
    fun render(
        context: Context,
        items: List<EvidenceItem>,
        request: ReportRequest
    ): File? = runCatching {
        val rowCount = items.size.coerceAtMost(12)
        val height = HEADER_HEIGHT + rowCount * ROW_HEIGHT + 160
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(C_BG)

        drawHeader(canvas, items, request)

        var y = HEADER_HEIGHT
        items.take(12).forEach { item ->
            drawRow(canvas, item, request, y)
            y += ROW_HEIGHT
        }

        drawFooter(canvas, items)

        val dir = File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
            "AppLockReports"
        )
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "침입리포트_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        bitmap.recycle()
        file
    }.getOrNull()

    private fun drawHeader(canvas: Canvas, items: List<EvidenceItem>, request: ReportRequest) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = C_ACCENT
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), 8f, paint)

        paint.color = C_TEXT
        paint.textSize = 52f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(request.title.take(28), 48f, 92f, paint)

        paint.color = C_SUB
        paint.textSize = 30f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("작성 ${dateFormat.format(Date())}", 48f, 140f, paint)

        // 요약 지표 3개
        val totalAttempts = items.sumOf { it.attempts }
        val withPhoto = items.count { it.photoPath != null }
        val withVideo = items.count { it.videoPath != null }
        val withLocation = items.count { !it.locationText.isNullOrBlank() }
        drawStat(canvas, "기록", "${items.size}건", 48, paint)
        drawStat(canvas, "실패 합계", "${totalAttempts}회", 312, paint)
        drawStat(canvas, "사진/영상", "${withPhoto}/${withVideo}", 576, paint)
        if (withLocation > 0) drawStat(canvas, "위치 기록", "${withLocation}건", 840, paint)

        // 지시문 요약
        paint.color = C_MUTED
        paint.textSize = 26f
        val note = request.prompt.trim().ifBlank { "침입 시도 기록 요약" }
        canvas.drawText("요청: ${note.take(40)}", 48f, 232f, paint)
    }

    private fun drawStat(canvas: Canvas, label: String, value: String, x: Int, basePaint: Paint) {
        val paint = Paint(basePaint)
        paint.color = C_CARD
        canvas.drawRoundRect(x.toFloat(), 158f, x + 240f, 226f, 18f, 18f, paint)
        paint.color = C_SUB
        paint.textSize = 24f
        canvas.drawText(label, x + 20f, 186f, paint)
        paint.color = C_TEXT
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 20f, 218f, paint)
        paint.typeface = Typeface.DEFAULT
    }

    private fun drawRow(canvas: Canvas, item: EvidenceItem, request: ReportRequest, y: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 썸네일 영역
        val thumbBox = Rect(48, y + 16, 168, y + 152)
        paint.color = C_CARD
        canvas.drawRoundRect(thumbBox.left.toFloat(), thumbBox.top.toFloat(), thumbBox.right.toFloat(), thumbBox.bottom.toFloat(), 16f, 16f, paint)
        if (request.includeThumbnails) {
            val thumbnail = item.photoPath?.let { decodeSampled(it, 220, 220) }
                ?: item.videoPath?.let { videoFrame(it) }
            if (thumbnail != null) {
                canvas.drawBitmap(thumbnail, null, thumbBox, paint)
                thumbnail.recycle()
            } else {
                paint.color = C_EMPTY
                paint.textSize = 30f
                canvas.drawText("증거 없음", thumbBox.left.toFloat() + 18f, (thumbBox.centerY() + 10f), paint)
            }
        }

        // 텍스트 정보
        paint.color = C_TEXT
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(item.appName.take(20), 200f, (y + 48f), paint)

        paint.typeface = Typeface.DEFAULT
        paint.color = C_TEXT2
        paint.textSize = 28f
        canvas.drawText("${dateFormat.format(Date(item.timestamp))} · ${item.attempts}회 실패 · ${item.usedLockType.take(14)}", 200f, (y + 90f), paint)

        paint.color = C_SUB
        paint.textSize = 26f
        val location = buildString {
            if (!item.placeName.isNullOrBlank()) append(item.placeName)
            if (!item.locationText.isNullOrBlank()) {
                if (isNotEmpty()) append(" · ")
                append(item.locationText)
            }
        }
        canvas.drawText(if (location.isBlank()) "위치 기록 없음 (시작 시점 이후 시도만 기록)" else location.take(46), 200f, (y + 128f), paint)

        // 구분선
        paint.color = C_LINE
        canvas.drawRect(48f, (y + ROW_HEIGHT - 1).toFloat(), (WIDTH - 48).toFloat(), (y + ROW_HEIGHT).toFloat(), paint)
    }

    private fun drawFooter(canvas: Canvas, items: List<EvidenceItem>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val y = HEADER_HEIGHT + items.size.coerceAtMost(12) * ROW_HEIGHT + 40
        paint.color = C_MUTED
        paint.textSize = 24f
        canvas.drawText("App Lock & Vault · 모든 기록은 기기 안에만 저장됩니다", 48f, y.toFloat(), paint)
        if (items.any { it.latitudePresent() }) {
            canvas.drawText("좌표는 로그인 기록 시점의 단말 GPS 값이며 정확도는 수십 미터입니다.", 48f, (y + 34f), paint)
        }
    }

    private fun EvidenceItem.latitudePresent(): Boolean = !locationText.isNullOrBlank()

    private fun decodeSampled(path: String, reqWidth: Int, reqHeight: Int): Bitmap? = runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        var sample = 1
        while (options.outWidth / sample > reqWidth * 2 || options.outHeight / sample > reqHeight * 2) sample *= 2
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(path, decodeOptions)
    }.getOrNull()

    /** 동영상의 첫 프레임을 썸네일로 뽑는다. */
    private fun videoFrame(path: String): Bitmap? = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(path)
            val frame = retriever.getFrameAtTime(0)
            if (frame != null && frame.width > 0) {
                val scaled = Bitmap.createScaledBitmap(frame, min(frame.width, 320), min(frame.height, 320), true)
                if (scaled !== frame) frame.recycle()
                scaled
            } else null
        }
    }.getOrNull()
}