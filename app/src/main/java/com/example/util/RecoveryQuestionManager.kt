package com.example.util

import android.content.Context
import android.util.Base64
import com.example.model.RecoveryQuestion
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 잠금 비밀번호를 잊었을 때 사용하는 개인 확인 질문 복구.
 *
 * - 답변은 평문으로 저장하지 않는다. 질문별 salt + SHA-256 해시만 보관한다.
 * - 오답이 반복되면 복구 시도 자체를 시간 동안 막는다(무한 추측 방지).
 */
object RecoveryQuestionManager {

    /** (id, 질문 문구, 힌트) */
    val PRESET_QUESTIONS: List<Triple<String, String, String>> = listOf(
        Triple("teacher", "초등학교 때 가장 싫었던 선생님 성함은?", "같은 학교를 다닌 사람만 알 수 있는 이름"),
        Triple("pet", "처음 키운 반려동물의 이름은?", "이모티콘이나 실제 이름"),
        Triple("classroom", "가장 기억에 남는 초등학교 교실 번호는?", "몇 학년 몇 반이었는지"),
        Triple("lunch", "초등학교 급식에서 가장 좋아했던 메뉴는?", "간식 이름으로도 괜찮습니다"),
        Triple("store", "어릴 때 가장 자주 가던 동네 가게 이름은?", "문구나 상호"),
        Triple("school", "초등학교 때 처음 입학한 학교 이름은?", "지금도 같은 학교라면 학교 이름"),
        Triple("embarrass", "초등학교 때 가장 부끄러웠던 순간은?", "한 문장으로 적어도 됩니다"),
        Triple("nickname", "어린 시절 친구에게 불리던 별명은?", "본명이 아니라 호칭"),
        Triple("dream", "초등학교 때 가장 되고 싶었던 직업은?", "선생님·의사·운동선수 등"),
        Triple("alarm", "어릴 때 매일 아침에 들었던 말버릇은?", "아침마다 하던 말이나 행동"),
        Triple("seat", "초등학교 때 자신의 자리 자리는?", "몇 행 몇 열이었는지"),
        Triple("rain", "가장 기억에 남는 날씨가 있었던 순간은?", "장마·폭염 등")
    ).map { (id, prompt, hint) ->
        Triple(id, prompt, hint)
    }

    private const val PREFS = "recovery_questions"
    private const val KEY_QUESTIONS = "questions"
    private const val KEY_FAILED_AT = "failed_at"
    private const val KEY_FAILED_COUNT = "failed_count"
    private const val REQUIRED_COUNT = 3

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isConfigured(context: Context): Boolean = getQuestions(context).size >= REQUIRED_COUNT

    fun getQuestions(context: Context): List<RecoveryQuestion> {
        val raw = prefs(context).getString(KEY_QUESTIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val list = mutableListOf<RecoveryQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    RecoveryQuestion(
                        id = obj.getString("id"),
                        prompt = obj.getString("prompt"),
                        answerHash = obj.getString("hash"),
                        salt = obj.getString("salt"),
                        hint = if (obj.isNull("hint")) null else obj.optString("hint")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    /** 답은 이 앱이 정규화해 해시한다. 사용자에게는 공백을 무시해도 된다고 안내한다. */
    fun setAnswers(context: Context, answers: Map<String, String>) {
        val usable = answers.filterValues { it.isNotBlank() }
        if (usable.size < REQUIRED_COUNT) return
        val array = JSONArray()
        usable.forEach { (id, answer) ->
            val prompt = PRESET_QUESTIONS.firstOrNull { it.first == id }?.second ?: id
            val hint = PRESET_QUESTIONS.firstOrNull { it.first == id }?.third
            val salt = randomSalt()
            array.put(JSONObject().apply {
                put("id", id)
                put("prompt", prompt)
                put("salt", salt)
                put("hash", hashOf(normalize(answer), salt))
                if (hint != null) put("hint", hint)
            })
        }
        prefs(context).edit()
            .putString(KEY_QUESTIONS, array.toString())
            .remove(KEY_FAILED_AT)
            .remove(KEY_FAILED_COUNT)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_QUESTIONS).remove(KEY_FAILED_AT).remove(KEY_FAILED_COUNT).apply()
    }

    /** 오답 연속 기록을 남기고, 잠겼으면 true 를 돌려준다. */
    fun registerFailure(context: Context): Boolean {
        val p = prefs(context)
        val count = p.getInt(KEY_FAILED_COUNT, 0) + 1
        p.edit()
            .putInt(KEY_FAILED_COUNT, count)
            .putLong(KEY_FAILED_AT, System.currentTimeMillis())
            .apply()
        if (count >= 3) {
            // 3회 이상 틀리면 1시간 동안 복구 시도를 막는다.
            p.edit().putLong(KEY_FAILED_AT, System.currentTimeMillis() + 60 * 60 * 1000L).apply()
            p.edit().putInt(KEY_FAILED_COUNT, 0).apply()
            return true
        }
        return false
    }

    /** 남은 복구 차단 시간. 0 이면 지금 시도할 수 있다. */
    fun blockedRemainingMs(context: Context): Long {
        val until = prefs(context).getLong(KEY_FAILED_AT, 0L)
        if (until <= 0L) return 0L
        val remaining = until - System.currentTimeMillis()
        return if (remaining > 0L) remaining else 0L
    }

    fun registerSuccess(context: Context) {
        prefs(context).edit().remove(KEY_FAILED_AT).remove(KEY_FAILED_COUNT).apply()
    }

    /**
     * 모든 답이 맞으면 true. 오답 수는 호출부가 registerFailure() 로 관리한다.
     */
    fun verify(context: Context, answers: Map<String, String>): Boolean {
        val questions = getQuestions(context)
        if (questions.isEmpty()) return false
        return questions.all { question ->
            val given = answers[question.id] ?: return@all false
            val computed = hashOf(normalize(given), question.salt)
            // 길이 차이로도 시간 정보가 새지 않도록 비교한다.
            constantTimeEquals(computed, question.answerHash)
        }
    }

    private fun normalize(answer: String): String =
        answer.trim().lowercase().replace(Regex("\\s+"), "")

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hashOf(normalized: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("$salt::$normalized".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (index in a.indices) diff = diff or (a[index].code xor b[index].code)
        return diff == 0
    }
}