package com.claudemani.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claudemani.domain.model.Achievement
import com.claudemani.domain.usecase.CalculateProgressPercentageUseCase
import com.claudemani.domain.usecase.CalculateStreakUseCase
import com.claudemani.domain.usecase.CheckAndUnlockAchievementsUseCase
import com.claudemani.domain.usecase.GetAllAchievementsUseCase
import com.claudemani.domain.usecase.GetBasicStatisticsUseCase
import com.claudemani.domain.usecase.GetProgressSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns Progress screen state only — all business logic runs through
 * UseCases, same rule as [com.claudemani.app.presentation.home.HomeViewModel].
 *
 * [load] runs [CheckAndUnlockAchievementsUseCase] every time the screen
 * opens (Algorithms v4.20 §11.4 explicitly allows this trigger point, as
 * an alternative to "after every review answer" — no Achievement check is
 * wired into [com.claudemani.app.presentation.review.ReviewViewModel] yet,
 * so this is currently the only place Achievements get unlocked). The
 * result of that call is shown once via [ProgressUiState.newlyUnlockedTypes]
 * and cleared by [dismissNewlyUnlocked] — a second [load] (e.g. on-resume)
 * must not re-show Achievements that were already unlocked in a prior call.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val getProgressSummary: GetProgressSummaryUseCase,
    private val getBasicStatistics: GetBasicStatisticsUseCase,
    private val calculateStreak: CalculateStreakUseCase,
    private val calculateProgressPercentage: CalculateProgressPercentageUseCase,
    private val checkAndUnlockAchievements: CheckAndUnlockAchievementsUseCase,
    private val getAllAchievements: GetAllAchievementsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val now = Instant.now()

            val stats = getBasicStatistics()
            val summary = getProgressSummary(now)
            val streak = calculateStreak(now)
            val progress = calculateProgressPercentage()
            val newlyUnlocked = checkAndUnlockAchievements(now)
            val achievements = getAllAchievements()

            _uiState.value = buildProgressUiState(
                totalActiveWords = stats.totalActiveWords,
                practicedWords = stats.practicedWords,
                unpracticedWords = stats.unpracticedWords,
                learnedWords = stats.learnedWords,
                dueCount = summary.dueCount,
                totalCorrect = summary.totalCorrect,
                totalWrong = summary.totalWrong,
                accuracyPercentage = summary.accuracyPercentage,
                streak = streak,
                progressPercentage = progress,
                achievements = achievements,
                newlyUnlocked = newlyUnlocked
            )
        }
    }

    fun dismissNewlyUnlocked() {
        _uiState.value = _uiState.value.copy(newlyUnlockedTypes = emptyList())
    }
}

/**
 * Pure state-shaping step, unit-testable without coroutines/fakes — same
 * split as [com.claudemani.app.presentation.home.buildHomeUiState].
 * Rounding of percentages happens here (display-layer concern, per
 * Algorithms §11.2's own note that rounding is not the algorithm's job).
 */
internal fun buildProgressUiState(
    totalActiveWords: Int,
    practicedWords: Int,
    unpracticedWords: Int,
    learnedWords: Int,
    dueCount: Int,
    totalCorrect: Int,
    totalWrong: Int,
    accuracyPercentage: Double,
    streak: Int,
    progressPercentage: Double,
    achievements: List<Achievement>,
    newlyUnlocked: List<Achievement>
): ProgressUiState = ProgressUiState(
    isLoading = false,
    totalActiveWords = totalActiveWords,
    practicedWords = practicedWords,
    unpracticedWords = unpracticedWords,
    learnedWords = learnedWords,
    dueCount = dueCount,
    totalCorrect = totalCorrect,
    totalWrong = totalWrong,
    accuracyPercentage = accuracyPercentage.roundToInt(),
    progressPercentage = progressPercentage.roundToInt(),
    streak = streak,
    achievements = achievements.map {
        AchievementUiItem(
            type = it.type,
            description = achievementDescription(it.type),
            isUnlocked = it.isUnlocked,
            unlockedAt = it.unlockedAt
        )
    },
    newlyUnlockedTypes = newlyUnlocked.map { it.type }
)
