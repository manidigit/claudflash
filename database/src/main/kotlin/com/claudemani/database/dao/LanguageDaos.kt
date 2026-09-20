package com.claudemani.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.claudemani.database.entity.LanguageEntity
import com.claudemani.database.entity.LanguagePairEntity
import java.util.UUID

@Dao
interface LanguageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LanguageEntity)

    @Update
    suspend fun update(entity: LanguageEntity)

    @Query("SELECT * FROM languages WHERE id = :id LIMIT 1")
    suspend fun getById(id: UUID): LanguageEntity?

    @Query("SELECT * FROM languages WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): LanguageEntity?

    @Query("SELECT * FROM languages")
    suspend fun getAll(): List<LanguageEntity>
}

@Dao
interface LanguagePairDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LanguagePairEntity)

    @Update
    suspend fun update(entity: LanguagePairEntity)

    @Query("SELECT * FROM language_pairs WHERE id = :id LIMIT 1")
    suspend fun getById(id: UUID): LanguagePairEntity?

    /**
     * V1 has exactly one active pair. There is currently no
     * `SetActiveLanguagePairUseCase` in the domain layer to enforce this —
     * the *only* real enforcement is [com.claudemani.database.ClaudemaniDatabaseCallback]'s
     * raw partial `UNIQUE INDEX ... WHERE isActive = 1` (see its KDoc and
     * `LanguagePairActiveIndexTest`, Phase 29). This query itself does not
     * enforce anything; it only reads.
     */
    @Query("SELECT * FROM language_pairs WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): LanguagePairEntity?

    @Query("SELECT * FROM language_pairs")
    suspend fun getAll(): List<LanguagePairEntity>
}
