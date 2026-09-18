package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.Achievement
import com.flashlearn.domain.model.AchievementType
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.repository.AchievementRepository
import com.flashlearn.domain.repository.ConceptRepository
import com.flashlearn.domain.repository.DifficultyStateRepository
import com.flashlearn.domain.repository.LearningStateRepository
import com.flashlearn.domain.repository.ReviewHistoryRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * CheckAndUnlockAchievements (Algorithms v4.20 §11.4). Meant to run after
 * every accepted review answer, or whenever the Statistics screen opens.
 * Returns only the Achievements unlocked *by this specific invocation* —
 * the UI must consume only [invoke]'s return value for
 * animation/notification and must never recompute achievements itself or
 * re-announce an already-unlocked Achievement.
 *
 * Composes [GetBasicStatisticsUseCase] and [CalculateStreakUseCase] for
 * the metrics they already compute (practicedWords, learnedWords,
 * totalActiveWords, streak), and reads Difficulty/ReviewHistory directly
 * for the two Achievements that need data neither of those exposes
 * (HARD_MODE_MASTER, LONG_TERM_MEMORY).
 */
class CheckAndUnlockAchievementsUseCase @Inject constructor(
    private val getBasicStatistics: GetBasicStatisticsUseCase,
    private val calculateStreak: CalculateStreakUseCase,
    private val conceptRepository: ConceptRepository,
    private val learningStateRepository: LearningStateRepository,
    private val difficultyStateRepository: DifficultyStateRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val achievementRepository: AchievementRepository
) {
    suspend operator fun invoke(now: Instant, zoneId: ZoneId = ZoneId.systemDefault()): List<Achievement> {
        val stats = getBasicStatistics()
        val streak = calculateStreak(now, zoneId)
        val activeIds = conceptRepository.getAllActive().map { it.id }.toSet()

        // HARD_MODE_MASTER: active + LEARNED + DifficultyState.hasReachedVeryHard == true.
        val hardModeMasterCount = learningStateRepository.getAll()
            .asSequence()
            .filter { it.stage == Stage.LEARNED && it.conceptId in activeIds }
            .count { difficultyStateRepository.get(it.conceptId)?.hasReachedVeryHard == true }

        // LONG_TERM_MEMORY: distinct active Concepts with >= 1 correct MONTHLY review.
        val longTermMemoryCount = reviewHistoryRepository.getAll()
            .asSequence()
            .filter { it.reviewType == ReviewType.MONTHLY && it.isCorrect && it.conceptId in activeIds }
            .map { it.conceptId }
            .distinct()
            .count()

        val conditionByType = linkedMapOf(
            AchievementType.FIRST_TEN_WORDS to (stats.practicedWords >= 10),
            AchievementType.SEVEN_DAY_STREAK to (streak >= 7),
            AchievementType.THIRTY_DAY_STREAK to (streak >= 30),
            AchievementType.MEMORY_BUILDER to (stats.learnedWords >= 100),
            AchievementType.VOCABULARY_BUILDER to (stats.totalActiveWords >= 500),
            AchievementType.HARD_MODE_MASTER to (hardModeMasterCount >= 25),
            AchievementType.LONG_TERM_MEMORY to (longTermMemoryCount >= 50)
        )

        val newlyUnlocked = mutableListOf<Achievement>()
        for ((type, conditionMet) in conditionByType) {
            val alreadyUnlocked = achievementRepository.findByType(type)?.isUnlocked ?: false
            if (alreadyUnlocked) continue
            if (conditionMet) {
                val achievement = Achievement(type = type, isUnlocked = true, unlockedAt = now)
                achievementRepository.upsert(achievement)
                newlyUnlocked.add(achievement)
            }
        }
        return newlyUnlocked
    }
}

/**
 * Read-only listing of every [AchievementType] with its current persisted
 * unlock state (locked types simply have no row yet in [AchievementRepository]).
 * Kept separate from [CheckAndUnlockAchievementsUseCase] — that UseCase's
 * return value is only the ones unlocked *by this call* (for animation/
 * notification, Algorithms v4.20 §11.4); a Progress screen listing every
 * Achievement's overall state is a different read and must not reuse or
 * be confused with that transient result.
 */
class GetAllAchievementsUseCase @Inject constructor(
    private val achievementRepository: AchievementRepository
) {
    suspend operator fun invoke(): List<Achievement> {
        val unlockedByType = achievementRepository.getAll().associateBy { it.type }
        return AchievementType.entries.map { type ->
            unlockedByType[type] ?: Achievement(type = type, isUnlocked = false, unlockedAt = null)
        }
    }
}
