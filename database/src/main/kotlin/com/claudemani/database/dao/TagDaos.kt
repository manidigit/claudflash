package com.claudemani.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.claudemani.database.entity.ConceptTagEntity
import com.claudemani.database.entity.TagEntity
import java.util.UUID

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TagEntity)

    @Update
    suspend fun update(entity: TagEntity)

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: UUID): TagEntity?

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>
}

@Dao
interface ConceptTagDao {
    /** IGNORE makes re-inserting an existing (conceptId, tagId) pair a silent no-op — required Idempotent Insert (Appendix N). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ConceptTagEntity)

    @Delete
    suspend fun delete(entity: ConceptTagEntity)

    @Query("SELECT tagId FROM concept_tags WHERE conceptId = :conceptId")
    suspend fun getTagIdsForConcept(conceptId: UUID): List<UUID>

    @Query("SELECT conceptId FROM concept_tags WHERE tagId = :tagId")
    suspend fun getConceptIdsForTag(tagId: UUID): List<UUID>

    /** Every (conceptId, tagId) pair — used by CreateBackup (Phase 17). */
    @Query("SELECT * FROM concept_tags")
    suspend fun getAll(): List<ConceptTagEntity>
}
