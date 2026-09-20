package com.claudemani.data.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.claudemani.data.repository.AchievementRepositoryImpl
import com.claudemani.data.repository.CategoryRepositoryImpl
import com.claudemani.data.repository.ConceptRepositoryImpl
import com.claudemani.data.repository.ConceptTagRepositoryImpl
import com.claudemani.data.repository.ContentRepositoryImpl
import com.claudemani.data.repository.DifficultyStateRepositoryImpl
import com.claudemani.data.repository.ClaudemaniDatabaseImpl
import com.claudemani.data.repository.LanguagePairRepositoryImpl
import com.claudemani.data.repository.LanguageRepositoryImpl
import com.claudemani.data.repository.LearningStateRepositoryImpl
import com.claudemani.data.repository.ReviewHistoryRepositoryImpl
import com.claudemani.data.repository.ReviewSessionRepositoryImpl
import com.claudemani.data.repository.SettingsRepositoryImpl
import com.claudemani.data.repository.TagRepositoryImpl
import com.claudemani.database.ClaudemaniDatabaseCallback
import com.claudemani.database.ClaudemaniRoomDatabase
import com.claudemani.domain.repository.ClaudemaniDatabase

/**
 * Shared harness for Phase 29 real-Room integration tests. Every
 * Repository here is the ACTUAL `data`-module implementation wired to a
 * real (in-memory) Room database — not [com.claudemani.domain.repository.FakeConceptRepository]
 * and friends, which every UseCase test up to this phase used instead.
 * Fakes are fast and fine for algorithm/orchestration-order tests, but
 * they cannot catch a real SQL/Room mistake (wrong column, missing
 * index, a transaction that doesn't actually roll back) — that gap is
 * exactly what Phase 29 exists to close.
 *
 * Built by hand (no Hilt test rule) since a handful of `@Inject`
 * constructors taking one DAO each are trivial to wire directly, and
 * doing so keeps these tests running as plain Robolectric JVM tests.
 */
class RealRoomTestHarness {
    val roomDb: ClaudemaniRoomDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        ClaudemaniRoomDatabase::class.java
    )
        .addCallback(ClaudemaniDatabaseCallback)
        .allowMainThreadQueries()
        .build()

    val database: ClaudemaniDatabase = ClaudemaniDatabaseImpl(roomDb)

    val conceptRepository = ConceptRepositoryImpl(roomDb.conceptDao())
    val contentRepository = ContentRepositoryImpl(roomDb.contentDao())
    val learningStateRepository = LearningStateRepositoryImpl(roomDb.learningStateDao())
    val difficultyStateRepository = DifficultyStateRepositoryImpl(roomDb.difficultyStateDao())
    val conceptTagRepository = ConceptTagRepositoryImpl(roomDb.conceptTagDao())
    val reviewHistoryRepository = ReviewHistoryRepositoryImpl(roomDb.reviewHistoryDao())
    val reviewSessionRepository = ReviewSessionRepositoryImpl(roomDb.reviewSessionDao())
    val settingsRepository = SettingsRepositoryImpl(roomDb.settingsDao())
    val achievementRepository = AchievementRepositoryImpl(roomDb.achievementDao())
    val categoryRepository = CategoryRepositoryImpl(roomDb.categoryDao())
    val tagRepository = TagRepositoryImpl(roomDb.tagDao())
    val languageRepository = LanguageRepositoryImpl(roomDb.languageDao())
    val languagePairRepository = LanguagePairRepositoryImpl(roomDb.languagePairDao())

    fun close() {
        roomDb.close()
    }
}
