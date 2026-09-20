package com.claudemani.domain.exception

/**
 * Thrown when an active Concept is missing its required LearningState or
 * DifficultyState. Per Descriptions §4 (Integrity Rule): this must never
 * be silently repaired or guessed — Statistics/Review/Queue code must let
 * this propagate.
 */
class DataIntegrityException(message: String) : IllegalStateException(message)

/**
 * Thrown when a (sessionId, reviewAttemptId) pair has already been
 * recorded in ReviewHistory. Duplicate-attempt detection runs before Due
 * validation (v4.10 Duplicate-attempt ordering correction).
 */
class DuplicateReviewAttemptException(message: String) : IllegalStateException(message)

/**
 * Thrown when a review answer is submitted for a Concept whose
 * nextReviewAt is still in the future. Does not apply to
 * ReviewType.LEARNED, which is always allowed.
 */
class ReviewNotDueException(message: String) : IllegalStateException(message)

/**
 * Thrown by CreateConceptUseCase when required text is missing (Edge
 * Case §8: the user must enter at least one translation, or the app
 * shows an error rather than silently creating an incomplete Concept).
 */
class InvalidConceptInputException(message: String) : IllegalArgumentException(message)

/** Thrown by Category UseCases when a required field (currently: name) is blank. */
class InvalidCategoryInputException(message: String) : IllegalArgumentException(message)
