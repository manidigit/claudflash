package com.flashlearn.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.flashlearn.database.entity.DifficultyStateEntity
import java.util.UUID

@Dao
interface DifficultyStateDao {
    @Query("SELECT * FROM difficulty_states WHERE conceptId = :conceptId LIMIT 1")
    suspend fun getByConceptId(conceptId: UUID): DifficultyStateEntity?

    @Upsert
    suspend fun upsert(entity: DifficultyStateEntity)

    @Query("DELETE FROM difficulty_states WHERE conceptId = :conceptId")
    suspend fun delete(conceptId: UUID)

    /** Every DifficultyState — used by CreateBackup (Phase 17). */
    @Query("SELECT * FROM difficulty_states")
    suspend fun getAll(): List<DifficultyStateEntity>
}
