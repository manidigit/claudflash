package com.flashlearn.app.navigation

import com.flashlearn.domain.model.ReviewType

/**
 * Stable route names (Descriptions §13/Phase 8: "Primary destinations
 * are declared with stable routes"). Home, Review, Progress and Settings
 * are the four primary destinations; AddWord is a secondary screen
 * reached from Home, per the UI/UX rules (§12.3).
 *
 * [REVIEW] carries one optional query argument, [REVIEW_TYPE_ARG]. The
 * Review Scheduling Algorithm's own input contract says `reviewType`
 * "از انتخاب کاربر در Home Screen دریافت می‌شود" (comes from the user's
 * choice on the Home Screen) — so Home (Phase 23) must be able to hand a
 * concrete [ReviewType] to Review when the user taps a specific due-count
 * card. This revises the Phase 22 note that Review needed no arguments:
 * the *base* route string `"review"` is unchanged (so Phase 22's route
 * contract test still holds), only an optional argument was added. The
 * Review screen (Phase 25/26) still owns its own in-screen type-selector
 * *state* for when the user changes their mind after arriving — this
 * argument only supplies the initial choice.
 */
object Routes {
    const val HOME = "home"
    const val REVIEW = "review"
    const val ADD_WORD = "add_word"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"
    const val ABOUT = "about"

    const val REVIEW_TYPE_ARG = "reviewType"

    /** Nav-graph route pattern for Review, with [REVIEW_TYPE_ARG] as an optional query param. */
    const val REVIEW_ROUTE_PATTERN = "$REVIEW?$REVIEW_TYPE_ARG={$REVIEW_TYPE_ARG}"

    /** Builds the concrete Review route Home navigates to. Null means "let Review pick its own default". */
    fun reviewRoute(reviewType: ReviewType? = null): String =
        if (reviewType == null) REVIEW else "$REVIEW?$REVIEW_TYPE_ARG=${reviewType.name}"

    /**
     * All primary + secondary destinations, for the Phase 8-style route
     * contract test. [CATEGORIES] and [ABOUT] (Phase 28) are secondary
     * destinations reached from Settings, same relationship [ADD_WORD]
     * has to Home.
     */
    val ALL = listOf(HOME, REVIEW, ADD_WORD, PROGRESS, SETTINGS, CATEGORIES, ABOUT)
}
