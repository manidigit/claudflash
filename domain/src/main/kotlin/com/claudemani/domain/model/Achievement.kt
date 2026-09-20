package com.claudemani.domain.model

import java.time.Instant

/**
 * Persisted unlock state for one [AchievementType]. UI only consumes the
 * `newlyUnlocked` list returned by CheckAndUnlockAchievements (Phase 15) —
 * it must never compute achievements itself (Algorithms v4.20 §11.4).
 */
data class Achievement(
    val type: AchievementType,
    val isUnlocked: Boolean,
    val unlockedAt: Instant?
)
