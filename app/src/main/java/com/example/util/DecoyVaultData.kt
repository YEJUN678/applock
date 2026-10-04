package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** 미끼 금고에 보이는 항목. 실제 파일은 없고 화면에만 표시된다. */
data class DecoyItem(
    val id: String,
    val title: String,
    val type: String, // 사진 / 문서 / 메모 / 일정
    val sizeLabel: String,
    val createdAt: Long,
    val note: String? = null
)

/**
 * 미끼(디코이) 금고용 더미 데이터.
 * 미끼 PIN으로 들어왔을 때 "평범하게 채워진 보관함"이 보여야 설득력이 생기므로,
 * 실제 파일 없이 목록만 만들어 화면에 제시한다.
 */
object DecoyVaultData {

    private const val PREFS = "decoy_vault"
    private const val KEY_ITEMS = "items"

    private val photoTitles = listOf(
        "바다_view", "가족 모임", "꽃밭", "제주 날씨", "축구 경기", "학교 앞", "고양이", "노을",
        "달빛 등산", "겨울 눈", "커피 한 잔", "비 오는 날"
    )
    private val docTitles = listOf(
        "주민등록등본", "영수증 모음", "약국 처방전", "임대차 계약서", "학교 생활기록부",
        "보험 약관", "기차표 예약", "답례장 목록", "분리수거 안내", "출입 기록"
    )
    private val memoTitles = listOf(
        "여행 준비 목록", "장보기 메모", "전화할 사람", "비밀번호 힌트(옛날)", "노트북 충전기",
        "택배 도착 예정", "요가 시간", "동네 정원사", "설날 계획", "반려동물 병원"
    )
    private val eventTitles = listOf(
        "치과 예약", "보호자 회", "앞치마 결례", "친구 주말", "세금 신고", "이사 업체 견적"
    )

    private fun typeSizeLabel(type: String, random: Random): String = when (type) {
        "사진" -> "${random.nextInt(180, 4200)}KB"
        "문서" -> "${random.nextInt(80, 2400)}KB"
        "메모" -> "메모"
        else -> "일정"
    }

    /** 최초 1회만 생성한다. 이후에는 사용자가 넣은 메모가 누적된다. */
    fun ensureSeeded(context: Context) {
        val prefs = prefs(context)
        if (prefs.getString(KEY_ITEMS, null) != null) return
        val random = Random(System.currentTimeMillis())
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000
        val items = mutableListOf<DecoyItem>()

        photoTitles.forEach { title ->
            items.add(
                DecoyItem(
                    id = "photo_${items.size}",
                    title = title,
                    type = "사진",
                    sizeLabel = typeSizeLabel("사진", random),
                    createdAt = now - random.nextLong(1, 300) * dayMillis
                )
            )
        }
        docTitles.forEach { title ->
            items.add(
                DecoyItem(
                    id = "doc_${items.size}",
                    title = title,
                    type = "문서",
                    sizeLabel = typeSizeLabel("문서", random),
                    createdAt = now - random.nextLong(1, 300) * dayMillis
                )
            )
        }
        memoTitles.forEach { title ->
            items.add(
                DecoyItem(
                    id = "memo_${items.size}",
                    title = title,
                    type = "메모",
                    sizeLabel = "메모",
                    createdAt = now - random.nextLong(1, 300) * dayMillis,
                    note = "마트 우유, 세제, 계란"
                )
            )
        }
        eventTitles.forEach { title ->
            items.add(
                DecoyItem(
                    id = "event_${items.size}",
                    title = title,
                    type = "일정",
                    sizeLabel = "일정",
                    createdAt = now - random.nextLong(1, 60) * dayMillis
                )
            )
        }
        write(context, items)
    }

    fun getItems(context: Context): List<DecoyItem> {
        ensureSeeded(context)
        val raw = prefs(context).getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val list = mutableListOf<DecoyItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DecoyItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        type = obj.optString("type", "메모"),
                        sizeLabel = obj.optString("size"),
                        createdAt = obj.optLong("at"),
                        note = if (obj.isNull("note")) null else obj.optString("note")
                    )
                )
            }
            // 최근에 추가된 항목이 위로
            list.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    fun addMemo(context: Context, title: String) {
        if (title.isBlank()) return
        val items = getItems(context).toMutableList()
        items.add(
            0,
            DecoyItem(
                id = "memo_user_${System.currentTimeMillis()}",
                title = title.trim(),
                type = "메모",
                sizeLabel = "메모",
                createdAt = System.currentTimeMillis()
            )
        )
        write(context, items)
    }

    fun delete(context: Context, id: String) {
        write(context, getItems(context).filterNot { it.id == id })
    }

    private fun write(context: Context, items: List<DecoyItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("type", item.type)
                put("size", item.sizeLabel)
                put("at", item.createdAt)
                if (item.note != null) put("note", item.note)
            })
        }
        prefs(context).edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}