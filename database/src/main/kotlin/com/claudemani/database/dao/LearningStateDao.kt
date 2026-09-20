package com.claudemani.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.claudemani.database.entity.LearningStateEntity
import java.time.Instant
import java.util.UUID

@Dao
interface LearningStateDao {
    @Query("SELECT * FROM learning_states WHERE conceptId = :conceptId LIMIT 1")
    suspend fun getByConceptId(conceptId: UUID): LearningStateEntity?

    @Upsert
    suspend fun upsert(entity: LearningStateEntity)

    @Query("SELECT * FROM learning_states WHERE stage = :stage")
    suspend fun getAllByStage(stage: String): List<LearningStateEntity>

    @Query(
        """SELECT * FROM learning_states
           WHERE stage = :stage AND nextReviewAt IS NOT NULL AND nextReviewAt <= :now
           ORDER BY nextReviewAt ASC, conceptId ASC"""
    )
    suspend fun getDueByStage(stage: String, now: Instant): List<LearningStateEntity>

    /** Base candidate set for Random Review (Appendix O.1) — DAILY/WEEKLY/MONTHLY only, LEARNED excluded. */
    @Query(
        """SELECT * FROM learning_states
           WHERE stage IN ('DAILY', 'WEEKLY', 'MONTHLY')
           AND nextReviewAt IS NOT NULL AND nextReviewAt <= :now"""
    )
    suspend fun getAllDueNonLearned(now: Instant): List<LearningStateEntity>

    @Query("SELECT * FROM learning_states")
    suspend fun getAll(): List<LearningStateEntity>
}
