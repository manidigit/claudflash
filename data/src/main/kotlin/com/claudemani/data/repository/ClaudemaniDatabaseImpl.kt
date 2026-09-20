package com.claudemani.data.repository

import androidx.room.withTransaction
import com.claudemani.database.ClaudemaniRoomDatabase
import com.claudemani.domain.repository.ClaudemaniDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudemaniDatabaseImpl @Inject constructor(
    private val roomDb: ClaudemaniRoomDatabase
) : ClaudemaniDatabase {
    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        roomDb.withTransaction { block() }
}
