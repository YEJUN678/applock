package com.example.model

/**
 * "누구에게 숨길 것인가" 에 대한 답.
 * 사용자가 스위치 20개를 고르는 대신 세 가지 질문에 답하게 한다.
 */
enum class GuardProfile(val title: String, val description: String) {
    FAMILY("가족", "가까운 사람이 보는 상황"),
    COWORKER("동료", "직장·학교에서 옆에 있는 상황"),
    OTHER("그 외", "지인이나 외부인이 보는 상황");

    companion object {
        fun fromName(value: String?): GuardProfile =
            entries.firstOrNull { it.name == value } ?: FAMILY
    }
}

/** "얼마나 강하게 숨길 것인가" 에 대한 답. */
enum class GuardStrength(val title: String, val description: String) {
    SIGNAL("신호", "잠긴 게 있다는 사실만 알립니다"),
    NORMAL("표준", "평범한 잠금 화면으로 막습니다"),
    STEALTH("신호 안 보임", "잠긴 흔적조차 지웁니다");

    companion object {
        fun fromName(value: String?): GuardStrength =
            entries.firstOrNull { it.name == value } ?: NORMAL
    }
}

/** "가장 먼저 지켜야 하는 것" 에 대한 답. */
enum class GuardPriority(val title: String, val description: String) {
    PHOTOS("사진", "사진첩과 갤러리부터 지킵니다"),
    MESSENGER("메신저", "카톡·디스코드 같은 대화부터 지킵니다"),
    MONEY("돈", "은행·결제 앱을 먼저 지킵니다");

    companion object {
        fun fromName(value: String?): GuardPriority =
            entries.firstOrNull { it.name == value } ?: MESSENGER
    }
}