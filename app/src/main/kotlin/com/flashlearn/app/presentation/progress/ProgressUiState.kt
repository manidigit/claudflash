package com.flashlearn.app.presentation.progress

import com.flashlearn.domain.model.AchievementType
import java.time.Instant

/** One row for the Achievements list; see [achievementDescription] for the label source. */
data class AchievementUiItem(
    val type: AchievementType,
    val description: String,
    val isUnlocked: Boolean,
    val unlockedAt: Instant?
)

/**
 * Progress screen state. Every number shown comes from here (same rule
 * as [com.flashlearn.app.presentation.home.HomeUiState] — Descriptions
 * §12.3 UI rule #3).
 */
data class ProgressUiState(
    val isLoading: Boolean = true,
    val totalActiveWords: Int = 0,
    val practicedWords: Int = 0,
    val unpracticedWords: Int = 0,
    val learnedWords: Int = 0,
    val dueCount: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val accuracyPercentage: Int = 0,
    val progressPercentage: Int = 0,
    val streak: Int = 0,
    val achievements: List<AchievementUiItem> = emptyList(),
    /** Types unlocked by *this* screen load — shown once, then dismissed. Not a persisted list. */
    val newlyUnlockedTypes: List<AchievementType> = emptyList()
)

/**
 * Persian labels translated from the Algorithms v4.20 §11.4 Achievement
 * table (the source table mixes a couple of English words like "Concept"/
 * "Learned" into otherwise-Persian rows — fully translated here for the UI,
 * thresholds unchanged from the table).
 */
internal fun achievementDescription(type: AchievementType): String = when (type) {
    AchievementType.FIRST_TEN_WORDS -> "یادگیری ۱۰ کلمه اول"
    AchievementType.SEVEN_DAY_STREAK -> "۷ روز پیوسته تمرین"
    AchievementType.THIRTY_DAY_STREAK -> "۳۰ روز پیوسته تمرین"
    AchievementType.MEMORY_BUILDER -> "۱۰۰ کلمه یادگرفته‌شده"
    AchievementType.VOCABULARY_BUILDER -> "۵۰۰ کلمه در مجموعه"
    AchievementType.HARD_MODE_MASTER -> "تسلط بر ۲۵ کلمه خیلی‌سخت"
    AchievementType.LONG_TERM_MEMORY -> "۵۰ پاسخ صحیح در مرور ماهانه"
}
