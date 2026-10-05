package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * Gemini generateContent 호출 래퍼.
 *
 * 실패 원인을 그대로 사용자에게 보여 주기 위해 예외를 새로 정의하지 않고
 * 사람이 읽을 메시지를 담은 결과를 돌려준다.
 */
object GeminiClient {

    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"

    sealed interface Result {
        data class Success(val text: String) : Result
        data class Failure(val reason: String) : Result
    }

    /**
     * 텍스트 프롬프트 + 선택적 이미지 바이트를 보내고 텍스트 답을 받는다.
     * 이미지는 base64 인라인으로 첨부한다.
     */
    fun ask(
        context: Context,
        prompt: String,
        images: List<ByteArray> = emptyList(),
        model: String = AiSettings.model(context)
    ): Result {
        val key = AiSettings.apiKey(context)
            ?: return Result.Failure("API 키가 설정되지 않았습니다. 설정 → AI 키에서 입력해 주세요.")
        if (prompt.isBlank()) return Result.Failure("질문 내용이 비어 있습니다.")

        val parts = JSONArray()
        parts.put(JSONObject().put("text", prompt))
        images.filter { it.isNotEmpty() }.take(3).forEach { bytes ->
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject().put("mime_type", "image/jpeg").put("data", Base64.getEncoder().encodeToString(bytes))
                )
            )
        }

        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            put("generationConfig", JSONObject().put("maxOutputTokens", 1500))
        }.toString()

        val url = URL("$ENDPOINT/$model:generateContent?key=$key")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            if (status !in 200..299) {
                Result.Failure(describeHttpError(status, text))
            } else {
                val json = runCatching { JSONObject(text) }.getOrNull()
                val answer = json?.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                when {
                    answer.isNullOrBlank() -> {
                        val blockReason = json?.optJSONArray("promptFeedback")?.optJSONObject(0)?.optString("blockReason")
                        Result.Failure(
                            if (!blockReason.isNullOrBlank()) "요청이 차단되었습니다 ($blockReason)."
                            else "답변을 받지 못했습니다."
                        )
                    }
                    else -> Result.Success(answer)
                }
            }
        } catch (error: Exception) {
            Result.Failure("요청 실패: ${error.message ?: "알 수 없는 오류"}")
        } finally {
            connection.disconnect()
        }
    }

    /** HTTP 오류를 그대로 읽히게 번역한다(무엇을 고쳐야 하는지 알 수 있게). */
    private fun describeHttpError(status: Int, body: String): String {
        val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
            ?.takeIf { it.isNotBlank() }
        return when (status) {
            400, 404 -> message ?: "요청이 거부되었습니다 (HTTP $status). 모델 이름을 확인해 주세요."
            401, 403 -> message ?: "API 키가 인증되지 않았습니다 (HTTP $status)."
            429 -> message ?: "요청 한도를 초과했습니다 (HTTP 429). 잠시 뒤 다시 시도해 주세요."
            in 500..599 -> "Gemini 서버 오류 (HTTP $status). 잠시 뒤 다시 시도해 주세요."
            else -> message ?: "요청 실패 (HTTP $status)."
        }
    }
}