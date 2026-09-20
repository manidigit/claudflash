package com.claudemani.domain.repository

/**
 * Domain-level transaction boundary. UseCases depend on this interface,
 * never on Room directly — keeps domain independent of Android/Room
 * (Descriptions §3.3). Implemented in `data` (Phase 9) by wrapping
 * Room's `RoomDatabase.withTransaction`.
 *
 * CreateConcept and SubmitReviewAnswer MUST run their entire body inside
 * a single [withTransaction] call so that all dependent writes commit or
 * roll back together.
 */
interface ClaudemaniDatabase {
    suspend fun <T> withTransaction(block: suspend () -> T): T
}
