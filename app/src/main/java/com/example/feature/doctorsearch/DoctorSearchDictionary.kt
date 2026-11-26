package com.example.feature.doctorsearch

import kotlin.math.min

data class SpecialtySuggestion(
    val specialtyEnglish: String,
    val specialtyBangla: String,
    val matchedCondition: String,
    val isGeneralFallback: Boolean = false
)

private data class RuleEntry(
    val specialtyEn: String,
    val specialtyBn: String,
    val displayCondition: String,
    val keywords: List<String>
)

/**
 * Transparent, rule-based local keyword dictionary supporting English, Bangla, and Banglish
 * with normalization, synonyms, spelling variations, and bounded Levenshtein fuzzy matching.
 * This is strictly a deterministic lookup table, NOT AI diagnosis.
 */
object DoctorSearchDictionary {

    private val rules = listOf(
        RuleEntry(
            specialtyEn = "Ophthalmologist",
            specialtyBn = "চক্ষু বিশেষজ্ঞ (Ophthalmologist)",
            displayCondition = "Eye Problem / চোখের সমস্যা",
            keywords = listOf("eye", "vision", "blur", "chokh", "chokher", "chokher somossa", "chokh betha", "চোখ", "চোখের", "চোখের সমস্যা", "ঝাপসা")
        ),
        RuleEntry(
            specialtyEn = "Dermatologist",
            specialtyBn = "চর্ম ও যৌন রোগ বিশেষজ্ঞ (Dermatologist)",
            displayCondition = "Skin Problem / চর্মরোগ",
            keywords = listOf("skin", "rash", "itch", "acne", "allergy", "eczema", "chulkani", "chormo", "tvok", "ত্বক", "চর্ম", "চুলকানি", "এলার্জি", "ব্রণ")
        ),
        RuleEntry(
            specialtyEn = "Cardiologist",
            specialtyBn = "হৃদরোগ বিশেষজ্ঞ (Cardiologist)",
            displayCondition = "Heart / Chest Pain / হৃদরোগ",
            keywords = listOf("heart", "chest pain", "palpitation", "bp", "hypertension", "buke betha", "buk betha", "hridrog", "pressure", "হৃদরোগ", "বুকে ব্যথা", "হার্ট", "প্রেশার")
        ),
        RuleEntry(
            specialtyEn = "Dentist",
            specialtyBn = "দন্ত চিকিৎসক (Dentist)",
            displayCondition = "Dental Problem / দাঁতের সমস্যা",
            keywords = listOf("tooth", "teeth", "dental", "gum", "toothache", "dat", "dater", "dat betha", "dater betha", "দাঁত", "দাঁতের", "দাঁত ব্যথা", "মাড়ি")
        ),
        RuleEntry(
            specialtyEn = "Gastroenterologist",
            specialtyBn = "পরিপাকতন্ত্র ও লিভার বিশেষজ্ঞ (Gastroenterologist)",
            displayCondition = "Gastric / Stomach / গ্যাস্ট্রিক",
            keywords = listOf("gastric", "acidity", "stomach", "ulcer", "indigestion", "pet", "pet betha", "gas", "gastrik", "গ্যাস্ট্রিক", "পেট", "পেট ব্যথা", "অম্বল", "বদহজম")
        ),
        RuleEntry(
            specialtyEn = "Endocrinologist",
            specialtyBn = "ডায়াবেটিস ও হরমোন বিশেষজ্ঞ (Endocrinologist)",
            displayCondition = "Diabetes / Hormone / ডায়াবেটিস",
            keywords = listOf("diabetes", "sugar", "thyroid", "hormone", "insulin", "diabetis", "ডায়াবেটিস", "ডায়াবেটিস", "সুগার", "থাইরয়েড", "হরমোন")
        ),
        RuleEntry(
            specialtyEn = "Neurologist",
            specialtyBn = "স্নায়ুরোগ বিশেষজ্ঞ (Neurologist)",
            displayCondition = "Headache / Migraine / মাথাব্যথা",
            keywords = listOf("headache", "migraine", "nerve", "dizziness", "matha", "matha betha", "matha ghora", "মাথা", "মাথাব্যথা", "মাইগ্রেন", "মাথা ঘোরা")
        ),
        RuleEntry(
            specialtyEn = "ENT Specialist",
            specialtyBn = "নাক, কান ও গলা বিশেষজ্ঞ (ENT)",
            displayCondition = "Ear, Nose & Throat / নাক-কান-গলা",
            keywords = listOf("ear", "nose", "throat", "tonsil", "sinus", "kan", "gola", "gola betha", "nak", "কান", "নাক", "গলা", "গলা ব্যথা", "টনসিল")
        ),
        RuleEntry(
            specialtyEn = "Orthopedic Specialist",
            specialtyBn = "হাড় ও অর্থোপেডিক বিশেষজ্ঞ (Orthopedics)",
            displayCondition = "Bone / Joint Pain / হাড় ও জয়েন্ট",
            keywords = listOf("bone", "joint", "knee", "back pain", "fracture", "har", "komor betha", "hatu betha", "হাড়", "কোমর ব্যথা", "হাঁটু ব্যথা", "জয়েন্ট")
        ),
        RuleEntry(
            specialtyEn = "Pulmonologist",
            specialtyBn = "বক্ষব্যাধি বিশেষজ্ঞ (Pulmonologist)",
            displayCondition = "Cough / Asthma / শ্বাসকষ্ট ও কাশি",
            keywords = listOf("asthma", "cough", "breathing", "lung", "kashi", "shashkoshto", "hapani", "কাশি", "শ্বাসকষ্ট", "হাঁপানি", "ফুসফুস")
        ),
        RuleEntry(
            specialtyEn = "General Physician (Internal Medicine)",
            specialtyBn = "মেডিসিন বিশেষজ্ঞ (General Physician)",
            displayCondition = "Fever / Dengue / General Illness / জ্বর ও ডেঙ্গু",
            keywords = listOf("fever", "dengue", "cold", "flu", "weakness", "viral", "jor", "jwar", "dengu", "thanda", "জ্বর", "ডেঙ্গু", "ঠান্ডা", "সর্দি", "দুর্বলতা")
        ),
        RuleEntry(
            specialtyEn = "Pediatrician",
            specialtyBn = "শিশু বিশেষজ্ঞ (Pediatrician)",
            displayCondition = "Child Health / শিশু স্বাস্থ্য",
            keywords = listOf("child", "baby", "infant", "kid", "baccha", "shishu", "শিশু", "বাচ্চা")
        ),
        RuleEntry(
            specialtyEn = "Gynecologist & Obstetrician",
            specialtyBn = "স্ত্রী ও প্রসূতি রোগ বিশেষজ্ঞ (Gynecologist)",
            displayCondition = "Women's Health / স্ত্রী রোগ",
            keywords = listOf("pregnancy", "period", "gyne", "women", "garbhoboti", "masik", "গর্ভবতী", "মাসিক", "স্ত্রী রোগ")
        )
    )

    fun normalize(input: String): String {
        return input
            .trim()
            .lowercase()
            .replace("য়", "য়")
            .replace("ড়", "ড়")
            .replace("ঢ়", "ঢ়")
            .replace(Regex("[\\p{Punct}]+"), " ")
            .replace(Regex("\\s+"), " ")
    }

    fun searchSpecialties(rawQuery: String): List<SpecialtySuggestion> {
        val q = normalize(rawQuery)
        if (q.isBlank()) return emptyList()
        val tokens = q.split(" ").filter { it.length >= 2 }

        val matches = rules.mapNotNull { rule ->
            var bestScore = 0
            for (kw in rule.keywords) {
                val nKw = normalize(kw)
                if (q == nKw) {
                    bestScore = maxOf(bestScore, 100)
                } else if (q.contains(nKw) || nKw.contains(q)) {
                    bestScore = maxOf(bestScore, 80)
                } else {
                    for (t in tokens) {
                        if (t == nKw) {
                            bestScore = maxOf(bestScore, 85)
                        } else if (t.length >= 3 && nKw.length >= 3 && levenshtein(t, nKw) <= 1) {
                            bestScore = maxOf(bestScore, 60)
                        }
                    }
                }
            }
            if (bestScore > 0) bestScore to rule else null
        }.sortedByDescending { it.first }
            .map { (_, r) ->
                SpecialtySuggestion(
                    specialtyEnglish = r.specialtyEn,
                    specialtyBangla = r.specialtyBn,
                    matchedCondition = r.displayCondition
                )
            }

        return if (matches.isNotEmpty()) {
            matches.distinctBy { it.specialtyEnglish }
        } else {
            listOf(
                SpecialtySuggestion(
                    specialtyEnglish = "General Physician (Internal Medicine)",
                    specialtyBangla = "মেডিসিন বিশেষজ্ঞ (General Physician)",
                    matchedCondition = rawQuery.trim(),
                    isGeneralFallback = true
                )
            )
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + cost)
                prev = temp
            }
        }
        return dp[b.length]
    }
}
