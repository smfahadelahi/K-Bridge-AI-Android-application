package com.kbridge.ai.core.domain.phrasebook

/**
 * Small, bundled phrase set for first-run access without a network connection.
 * These are practical starter translations, not certified medical, legal, or official translations.
 */
object OfflinePhrasebook {
    val entries: List<PhraseEntry> =
        listOf(
            phrase("daily-hello", "안녕하세요.", "হ্যালো।", PhraseScenario.EVERYDAY),
            phrase("daily-thanks", "감사합니다.", "ধন্যবাদ।", PhraseScenario.EVERYDAY),
            phrase("daily-sorry", "죄송합니다.", "দুঃখিত।", PhraseScenario.EVERYDAY),
            phrase(
                "daily-speak-slowly",
                "천천히 말씀해 주세요.",
                "দয়া করে ধীরে কথা বলুন।",
                PhraseScenario.EVERYDAY,
            ),
            phrase(
                "daily-repeat",
                "다시 말씀해 주세요.",
                "দয়া করে আবার বলুন।",
                PhraseScenario.EVERYDAY,
            ),
            phrase(
                "daily-no-korean",
                "한국어를 잘 못해요.",
                "আমি কোরিয়ান ভালো বলতে পারি না।",
                PhraseScenario.EVERYDAY,
            ),
            phrase(
                "daily-did-not-understand",
                "이해하지 못했어요.",
                "আমি বুঝতে পারিনি।",
                PhraseScenario.EVERYDAY,
            ),
            phrase(
                "daily-help",
                "도와주세요.",
                "দয়া করে আমাকে সাহায্য করুন।",
                PhraseScenario.EVERYDAY,
            ),
            phrase(
                "daily-from-bangladesh",
                "저는 방글라데시에서 왔어요.",
                "আমি বাংলাদেশ থেকে এসেছি।",
                PhraseScenario.EVERYDAY,
            ),
            phrase("shop-price", "이거 얼마예요?", "এটার দাম কত?", PhraseScenario.SHOPPING),
            phrase(
                "shop-this-one",
                "이걸 주세요.",
                "এটা দিন, দয়া করে।",
                PhraseScenario.SHOPPING,
            ),
            phrase(
                "shop-card",
                "카드로 결제할 수 있어요?",
                "কার্ডে পেমেন্ট করতে পারি?",
                PhraseScenario.SHOPPING,
            ),
            phrase(
                "shop-receipt",
                "영수증 주세요.",
                "দয়া করে রসিদ দিন।",
                PhraseScenario.SHOPPING,
            ),
            phrase(
                "transport-address",
                "이 주소로 가 주세요.",
                "দয়া করে এই ঠিকানায় নিয়ে যান।",
                PhraseScenario.TRANSPORT,
            ),
            phrase(
                "transport-stop",
                "여기서 내려 주세요.",
                "দয়া করে আমাকে এখানে নামিয়ে দিন।",
                PhraseScenario.TRANSPORT,
            ),
            phrase(
                "transport-bus",
                "이 버스는 서울역에 가요?",
                "এই বাসটি কি সিউল স্টেশনে যায়?",
                PhraseScenario.TRANSPORT,
            ),
            phrase(
                "health-hospital",
                "병원에 가고 싶어요.",
                "আমি হাসপাতালে যেতে চাই।",
                PhraseScenario.HEALTH,
            ),
            phrase(
                "health-stomach",
                "배가 아파요.",
                "আমার পেটে ব্যথা করছে।",
                PhraseScenario.HEALTH,
            ),
            phrase(
                "health-allergy",
                "알레르기가 있어요.",
                "আমার অ্যালার্জি আছে।",
                PhraseScenario.HEALTH,
            ),
            phrase(
                "health-medicine",
                "약을 먹고 있어요.",
                "আমি ওষুধ খাচ্ছি।",
                PhraseScenario.HEALTH,
            ),
            phrase(
                "work-late",
                "오늘은 조금 늦을 것 같습니다.",
                "মনে হচ্ছে আজ আমার একটু দেরি হবে।",
                PhraseScenario.WORKPLACE,
            ),
            phrase(
                "work-overtime",
                "오늘은 야근할 수 없습니다.",
                "আমি আজ অতিরিক্ত সময় কাজ করতে পারব না।",
                PhraseScenario.WORKPLACE,
            ),
            phrase(
                "work-explain-again",
                "다시 설명해 주시겠어요?",
                "আপনি কি আবার বুঝিয়ে বলবেন?",
                PhraseScenario.WORKPLACE,
            ),
            phrase(
                "work-deadline",
                "언제까지 하면 돼요?",
                "কখনের মধ্যে এটি করতে হবে?",
                PhraseScenario.WORKPLACE,
            ),
            phrase(
                "housing-rent",
                "월세가 얼마예요?",
                "মাসিক ভাড়া কত?",
                PhraseScenario.HOUSING,
            ),
            phrase(
                "housing-contract",
                "계약서를 보여 주세요.",
                "দয়া করে চুক্তিপত্রটি দেখান।",
                PhraseScenario.HOUSING,
            ),
            phrase(
                "housing-water",
                "물이 안 나와요.",
                "পানি আসছে না।",
                PhraseScenario.HOUSING,
            ),
        )

    private fun phrase(
        id: String,
        korean: String,
        bangla: String,
        scenario: PhraseScenario,
    ) = PhraseEntry(
            id = "bundled:$id",
            korean = korean,
            bangla = bangla,
            scenario = scenario,
            isBundled = true,
            isSaved = false,
            createdAtEpochMillis = 0L,
        )
}
