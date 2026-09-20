package com.claudemani.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.claudemani.database.entity.ConceptEntity
import java.time.Instant
import java.util.UUID

@Dao
interface ConceptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ConceptEntity)

    @Update
    suspend fun update(entity: ConceptEntity)

    @Query("SELECT * FROM concepts WHERE id = :id AND active = 1 LIMIT 1")
    suspend fun getById(id: UUID): ConceptEntity?

    /** Includes soft-deleted rows — required so Restore can find and update an inactive Concept instead of duplicating it. */
    @Query("SELECT * FROM concepts WHERE id = :id LIMIT 1")
    suspend fun getByIdAny(id: UUID): ConceptEntity?

    @Query("SELECT * FROM concepts WHERE active = 1")
    suspend fun getAllActive(): List<ConceptEntity>

    /** Every Concept regardless of active flag — used by CreateBackup (Phase 17) for full-fidelity export. */
    @Query("SELECT * FROM concepts")
    suspend fun getAll(): List<ConceptEntity>

    @Query("UPDATE concepts SET active = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: UUID, now: Instant)
}
