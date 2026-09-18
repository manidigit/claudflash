package com.flashlearn.data.repository

import androidx.room.withTransaction
import com.flashlearn.database.FlashLearnRoomDatabase
import com.flashlearn.domain.repository.FlashLearnDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashLearnDatabaseImpl @Inject constructor(
    private val roomDb: FlashLearnRoomDatabase
) : FlashLearnDatabase {
    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        roomDb.withTransaction { block() }
}
