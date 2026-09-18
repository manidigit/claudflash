package com.flashlearn.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.usecase.CalculateProgressPercentageUseCase
import com.flashlearn.domain.usecase.CalculateStreakUseCase
import com.flashlearn.domain.usecase.GetBasicStatisticsUseCase
import com.flashlearn.domain.usecase.SelectReviewQueueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns Home screen state only. All business logic runs through UseCases
 * (Descriptions §3.4) — this class never touches a Repository directly.
 *
 * [load] is re-invoked on `ON_RESUME` by the Composable (see HomeScreen),
 * not just once on creation — the due counts and streak can change after
 * a Review session that this ViewModel instance survived (NavHost keeps
 * Home's back-stack entry, and its LaunchedEffect(Unit) would not fire
 * again on its own).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBasicStatistics: GetBasicStatisticsUseCase,
    private val calculateStreak: CalculateStreakUseCase,
    private val calculateProgressPercentage: CalculateProgressPercentageUseCase,
    private val selectReviewQueue: SelectReviewQueueUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val now = Instant.now()

            val stats = getBasicStatistics()
            val streak = calculateStreak(now)
            val progress = calculateProgressPercentage()
            val dueDaily = selectReviewQueue(ReviewType.DAILY, now = now).size
            val dueWeekly = selectReviewQueue(ReviewType.WEEKLY, now = now).size
            val dueMonthly = selectReviewQueue(ReviewType.MONTHLY, now = now).size
            val dueRandom = selectReviewQueue(ReviewType.RANDOM, now = now).size
            val learnedAvailable = selectReviewQueue(ReviewType.LEARNED, now = now).size

            _uiState.value = buildHomeUiState(
                totalActiveWords = stats.totalActiveWords,
                learnedWords = stats.learnedWords,
                streak = streak,
                progressPercentage = progress,
                dueDaily = dueDaily,
                dueWeekly = dueWeekly,
                dueMonthly = dueMonthly,
                dueRandom = dueRandom,
                learnedAvailable = learnedAvailable
            )
        }
    }
}

/**
 * Pure state-shaping step, split out of [HomeViewModel.load] so it is
 * unit-testable without coroutines or fake repositories. Rounding the
 * percentage to an Int happens here deliberately — Algorithms §11.2 says
 * "نحوه گرد کردن فقط در لایه نمایش تعیین می‌شود" (rounding is a display-
 * layer concern), so [CalculateProgressPercentageUseCase] stays a raw
 * Double and this presentation-layer function does the rounding.
 */
internal fun buildHomeUiState(
    totalActiveWords: Int,
    learnedWords: Int,
    streak: Int,
    progressPercentage: Double,
    dueDaily: Int,
    dueWeekly: Int,
    dueMonthly: Int,
    dueRandom: Int,
    learnedAvailable: Int
): HomeUiState = HomeUiState(
    isLoading = false,
    streak = streak,
    progressPercentage = progressPercentage.roundToInt(),
    totalActiveWords = totalActiveWords,
    learnedWords = learnedWords,
    dueDaily = dueDaily,
    dueWeekly = dueWeekly,
    dueMonthly = dueMonthly,
    dueRandom = dueRandom,
    learnedAvailable = learnedAvailable
)
