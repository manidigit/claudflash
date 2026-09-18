package com.flashlearn.app.presentation.home

/**
 * Home screen state. Every number the screen shows comes from here —
 * never hardcoded in the Composable (Descriptions §12.3 UI rule #3).
 *
 * [dueDaily]/[dueWeekly]/[dueMonthly]/[dueRandom]/[learnedAvailable] are
 * exactly the sizes of what [com.flashlearn.domain.usecase.SelectReviewQueueUseCase]
 * would return for each [com.flashlearn.domain.model.ReviewType] right
 * now — there is deliberately no second, independent counting path, so
 * the number shown here can never drift from what Review actually loads
 * (the class of bug UI rules #1 and #5 warn about: a duplicate/stale
 * "words waiting" count shown somewhere else on the page).
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val streak: Int = 0,
    val progressPercentage: Int = 0,
    val totalActiveWords: Int = 0,
    val learnedWords: Int = 0,
    val dueDaily: Int = 0,
    val dueWeekly: Int = 0,
    val dueMonthly: Int = 0,
    val dueRandom: Int = 0,
    val learnedAvailable: Int = 0
)
