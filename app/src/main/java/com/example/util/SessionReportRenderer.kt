package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 잠금 해제 기록을 PNG 리포트로 만든다.
 *
 * 침입 증거 리포트와 같은 방식이라 완전히 로컬에서 그려진다(API 비용 없음).
 * "이번 주 잠금을 몇 번 풀었나"를 물었을 때 답으로 보여줄 수 있는 이미지다.
 */
object SessionReportRenderer {

    private const val WIDTH = 1080
    private const val ROW_HEIGHT = 96
    private const val HEADER_HEIGHT = 560
    private const val CHART_HEIGHT = 300
    private const val REASON_ROW_HEIGHT = 62

    // 팔레트는 Int 상수로 둔다(Color(0x...)는 이 파일 빌드에서 해석이 불안정하다)
    private const val C_BG = 0xFF0B1220.toInt()
    private const val C_ACCENT = 0xFF00E5FF.toInt()
    private const val C_CARD = 0xFF16203A.toInt()
    private const val C_TEXT = 0xFFF1F5F9.toInt()
    private const val C_SUB = 0xFF94A3B8.toInt()
    private const val C_TEXT2 = 0xFFCBD5E1.toInt()
    private const val C_GREEN = 0xFF34D399.toInt()
    private const val C_AMBER = 0xFFFFB800.toInt()
    private const val C_RED = 0xFFFB7185.toInt()
    private const val C_LINE = 0xFF1E293B.toInt()

    private val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)

    /** 리포트를 만들어 사진첩에 저장하고 파일을 돌려준다. */
    fun render(
        context: Context,
        totalUnlocks: Int,
        lockedAppCount: Int,
        totalAppCount: Int,
        dayCounts: List<Pair<String, Int>>,
        topApps: List<Pair<String, Int>>,
        /** (앱 이름, 잠근 사유). 비어 있으면 이 구역 자체가 그려지지 않는다. */
        lockReasons: List<Pair<String, String>> = emptyList(),
        aiSummary: String? = null
    ): File? = runCatching {
        val rows = topApps.take(6)
        val reasonRows = lockReasons.take(8)
        val height = HEADER_HEIGHT + CHART_HEIGHT + 90 + rows.size * ROW_HEIGHT +
            (if (reasonRows.isEmpty()) 0 else 90 + reasonRows.size * REASON_ROW_HEIGHT) +
            (if (aiSummary.isNullOrBlank()) 0 else 200) + 140

        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF()

        paint.color = C_BG
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), height.toFloat(), paint)

        // --- 헤더 ---
        paint.color = C_ACCENT
        paint.textSize = 52f
        paint.isFakeBoldText = true
        canvas.drawText("잠금 해제 리포트", 60f, 120f, paint)

        paint.color = C_SUB
        paint.textSize = 30f
        paint.isFakeBoldText = false
        canvas.drawText("App Lock & Vault · ${dateFormat.format(Date())}", 60f, 176f, paint)

        // --- 핵심 지표 3개 ---
        val cardY = 220f
        val cardW = (WIDTH - 60f * 2 - 24f * 2) / 3f
        val stats = listOf(
            Triple("총 해제", totalUnlocks.toString(), C_ACCENT),
            Triple("잠긴 앱", "$lockedAppCount", C_GREEN),
            Triple("전체 앱", "$totalAppCount", C_TEXT2)
        )
        stats.forEachIndexed { index, (label, value, color) ->
            val left = 60f + index * (cardW + 24f)
            rect.set(left, cardY, left + cardW, cardY + 170f)
            paint.color = C_CARD
            canvas.drawRoundRect(rect, 28f, 28f, paint)

            paint.color = color
            paint.textSize = 52f
            paint.isFakeBoldText = true
            canvas.drawText(value, left + 32f, cardY + 96f, paint)

            paint.color = C_SUB
            paint.textSize = 26f
            paint.isFakeBoldText = false
            canvas.drawText(label, left + 32f, cardY + 138f, paint)
        }

        // --- 7일 차트 ---
        val chartTop = cardY + 210f
        paint.color = C_TEXT2
        paint.textSize = 30f
        paint.isFakeBoldText = true
        canvas.drawText("최근 7일 해제 횟수", 60f, chartTop + 30f, paint)

        val barAreaTop = chartTop + 60f
        val maxCount = dayCounts.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
        val slotW = (WIDTH - 60f * 2) / dayCounts.size.coerceAtLeast(1)
        dayCounts.forEachIndexed { index, (label, count) ->
            val centerX = 60f + slotW * index + slotW / 2f
            val barW = slotW * 0.5f
            val ratio = count.toFloat() / maxCount
            val barH = (CHART_HEIGHT - 70f) * ratio

            // 기준선
            paint.color = C_LINE
            canvas.drawRoundRect(
                RectF(centerX - barW / 2f, barAreaTop + (CHART_HEIGHT - 70f) - barH, centerX + barW / 2f, barAreaTop + (CHART_HEIGHT - 70f)),
                14f, 14f, paint
            )
            if (barH > 0f) {
                paint.color = if (count == 0) C_LINE else C_ACCENT
                canvas.drawRoundRect(
                    RectF(centerX - barW / 2f, barAreaTop + (CHART_HEIGHT - 70f) - barH, centerX + barW / 2f, barAreaTop + (CHART_HEIGHT - 70f)),
                    14f, 14f, paint
                )
                paint.color = C_TEXT
                paint.textSize = 24f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(count.toString(), centerX, barAreaTop + (CHART_HEIGHT - 70f) - barH - 12f, paint)
                paint.textAlign = Paint.Align.LEFT
            }
            paint.color = C_SUB
            paint.textSize = 22f
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(label, centerX, barAreaTop + (CHART_HEIGHT - 70f) + 34f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        // --- 자주 연 앱 ---
        var y = barAreaTop + CHART_HEIGHT + 40f
        paint.color = C_TEXT2
        paint.textSize = 30f
        paint.isFakeBoldText = true
        canvas.drawText("자주 연 앱", 60f, y, paint)
        y += 46f

        if (rows.isEmpty()) {
            paint.color = C_SUB
            paint.textSize = 26f
            paint.isFakeBoldText = false
            canvas.drawText("기록이 없습니다.", 60f, y + 30f, paint)
            y += ROW_HEIGHT
        } else {
            rows.forEachIndexed { index, (appName, count) ->
                rect.set(60f, y, (WIDTH - 60f).toFloat(), y + ROW_HEIGHT - 16f)
                paint.color = C_CARD
                canvas.drawRoundRect(rect, 22f, 22f, paint)

                paint.color = C_TEXT
                paint.textSize = 28f
                paint.isFakeBoldText = false
                val maxLen = 24
                val shown = if (appName.length > maxLen) appName.take(maxLen - 1) + "…" else appName
                canvas.drawText(shown, 92f, y + 54f, paint)

                paint.color = C_ACCENT
                paint.textSize = 26f
                paint.isFakeBoldText = true
                canvas.drawText("${count}회", (WIDTH - 100f), y + 54f, paint)

                y += ROW_HEIGHT
            }
        }

        // --- 잠근 사유 ---
        // "왜 잠갔는가" 를 남겨 두지 않으면 숫자만 보이기 때문에 리포트가два anonim하다.
        if (reasonRows.isNotEmpty()) {
            y += 24f
            paint.color = C_TEXT2
            paint.textSize = 30f
            paint.isFakeBoldText = true
            canvas.drawText("잠근 사유", 60f, y, paint)
            y += 34f

            reasonRows.forEach { (appName, reason) ->
                rect.set(60f, y, (WIDTH - 60f).toFloat(), y + REASON_ROW_HEIGHT - 12f)
                paint.color = C_CARD
                canvas.drawRoundRect(rect, 18f, 18f, paint)

                paint.color = C_TEXT
                paint.textSize = 25f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(ellipsize(appName, 12), 92f, y + 36f, paint)

                // 사유는 카드 안쪽에 작게 붙인다.
                paint.color = C_SUB
                paint.textSize = 23f
                paint.isFakeBoldText = false
                canvas.drawText(ellipsize(reason, 26), 300f, y + 36f, paint)

                y += REASON_ROW_HEIGHT
            }
            y += 10f
        }

        // --- AI 요약 ---
        if (!aiSummary.isNullOrBlank()) {
            y += 24f
            rect.set(60f, y, (WIDTH - 60f).toFloat(), y + 170f)
            paint.color = C_CARD
            canvas.drawRoundRect(rect, 26f, 26f, paint)

            paint.color = C_GREEN
            paint.textSize = 26f
            paint.isFakeBoldText = true
            canvas.drawText("AI 요약", 92f, y + 48f, paint)

            paint.color = C_TEXT2
            paint.textSize = 26f
            paint.isFakeBoldText = false
            var lineY = y + 92f
            aiSummary.lines().take(3).forEach { line ->
                val trimmed = if (line.length > 34) line.take(33) + "…" else line
                canvas.drawText(trimmed, 92f, lineY, paint)
                lineY += 34f
            }
        }

        // --- 바닥 ---
        paint.color = C_SUB
        paint.textSize = 22f
        canvas.drawText("이 리포트는 기기 안의 기록만으로 만들어졌습니다. 서버로 전송되지 않습니다.", 60f, height - 60f, paint)

        saveToGallery(context, bitmap)
    }.getOrNull()

    /** 카드 폭에 들어맞게 자른다. */
    private fun ellipsize(text: String, maxChars: Int): String =
        if (text.length <= maxChars) text else text.take(maxChars - 1) + "…"

    private fun saveToGallery(context: Context, bitmap: Bitmap): File? {
        val name = "lock_session_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AppLockReports")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(bitmap.compressToPngBytes())
            }
        } else {
            // 저장소 접근이 막힌 경우 앱 외부 디렉터리로 내린다.
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
            val file = File(dir, name)
            file.outputStream().use { out ->
                out.write(bitmap.compressToPngBytes())
            }
            return file
        }
        return File(context.cacheDir, name).also { it.writeBytes(ByteArray(0)) }
    }

    /** PNG 바이트 배열로 압축한다. */
    private fun Bitmap.compressToPngBytes(): ByteArray =
        java.io.ByteArrayOutputStream().use { out ->
            compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
}