package com.claudemani.domain.model

import java.time.Instant

/**
 * A single key/value application setting, keyed by [key] (not database
 * ID) so Restore can merge by key without collision.
 *
 * Known keys used elsewhere in the app:
 * - "threshold_difficulty" (Int, default 3, no UI to change it in V1)
 * - "backup_encryption_enabled" (Boolean, default false, Future)
 */
data class AppSetting(
    val key: String,
    val value: String,
    val updatedAt: Instant
)

/** Canonical setting keys — kept centralized to avoid typo drift. */
object SettingKeys {
    const val THRESHOLD_DIFFICULTY = "threshold_difficulty"
    const val BACKUP_ENCRYPTION_ENABLED = "backup_encryption_enabled"
    const val DEFAULT_THRESHOLD_DIFFICULTY = 3

    /** Optional cap on cards per Review Session (Descriptions §6/Backlog F). Absent = no limit. */
    const val MAX_REVIEW_CARDS = "max_review_cards"

    /** Theme/Color Scheme (Descriptions §6/Backlog F, §12 Light+Dark support). */
    const val THEME = "theme"

    /** Quiz Difficulty (Algorithms O.3 Backlog #1, Phase 38) — independent from Vocabulary Difficulty. Absent = MEDIUM. */
    const val QUIZ_DIFFICULTY = "quiz_difficulty"
}
