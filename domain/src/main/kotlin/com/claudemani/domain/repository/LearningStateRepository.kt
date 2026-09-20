package com.claudemani.domain.repository

import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.Stage
import java.time.Instant
import java.util.UUID

interface LearningStateRepository {
    /** Exactly one LearningState per Concept (UNIQUE conceptId). */
    suspend fun get(conceptId: UUID): LearningState?

    suspend fun upsert(state: LearningState)

    suspend fun getAllByStage(stage: Stage): List<LearningState>

    /** stage == given stage AND nextReviewAt IS NOT NULL AND nextReviewAt <= now, ordered nextReviewAt ASC, conceptId ASC. */
    suspend fun getDueByStage(stage: Stage, now: Instant): List<LearningState>

    /** stage IN (DAILY, WEEKLY, MONTHLY) AND due — the base candidate set for Random Review (Appendix O.1). */
    suspend fun getAllDueNonLearned(now: Instant): List<LearningState>

    /** Full scan across every Stage — used by Statistics/Progress/Achievement aggregations (Phase 15). */
    suspend fun getAll(): List<LearningState>
}
