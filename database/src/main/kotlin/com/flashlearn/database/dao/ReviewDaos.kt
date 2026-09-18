package com.flashlearn.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flashlearn.database.entity.ReviewHistoryEntity
import com.flashlearn.database.entity.ReviewSessionEntity
import java.util.UUID

@Dao
interface ReviewSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ReviewSessionEntity)

    @Update
    suspend fun update(entity: ReviewSessionEntity)

    @Query("SELECT * FROM review_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: UUID): ReviewSessionEntity?

    /** Every ReviewSession — used by CreateBackup (Phase 17). */
    @Query("SELECT * FROM review_sessions")
    suspend fun getAll(): List<ReviewSessionEntity>
}

/** Append-only — intentionally no @Update/@Delete method exists here. */
@Dao
interface ReviewHistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ReviewHistoryEntity)

    @Query("SELECT * FROM review_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: UUID): ReviewHistoryEntity?

    @Query("SELECT * FROM review_history WHERE conceptId = :conceptId")
    suspend fun getByConceptId(conceptId: UUID): List<ReviewHistoryEntity>

    @Query("SELECT * FROM review_history WHERE sessionId = :sessionId")
    suspend fun getBySessionId(sessionId: UUID): List<ReviewHistoryEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM review_history WHERE sessionId = :sessionId AND reviewAttemptId = :attemptId)")
    suspend fun existsByAttemptId(sessionId: UUID, attemptId: UUID): Boolean

    @Query("SELECT DISTINCT conceptId FROM review_history")
    suspend fun getDistinctConceptIds(): List<UUID>

    @Query("SELECT * FROM review_history")
    suspend fun getAll(): List<ReviewHistoryEntity>
}
