package com.flashlearn.data.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.flashlearn.data.repository.AchievementRepositoryImpl
import com.flashlearn.data.repository.CategoryRepositoryImpl
import com.flashlearn.data.repository.ConceptRepositoryImpl
import com.flashlearn.data.repository.ConceptTagRepositoryImpl
import com.flashlearn.data.repository.ContentRepositoryImpl
import com.flashlearn.data.repository.DifficultyStateRepositoryImpl
import com.flashlearn.data.repository.FlashLearnDatabaseImpl
import com.flashlearn.data.repository.LanguagePairRepositoryImpl
import com.flashlearn.data.repository.LanguageRepositoryImpl
import com.flashlearn.data.repository.LearningStateRepositoryImpl
import com.flashlearn.data.repository.ReviewHistoryRepositoryImpl
import com.flashlearn.data.repository.ReviewSessionRepositoryImpl
import com.flashlearn.data.repository.SettingsRepositoryImpl
import com.flashlearn.data.repository.TagRepositoryImpl
import com.flashlearn.database.FlashLearnDatabaseCallback
import com.flashlearn.database.FlashLearnRoomDatabase
import com.flashlearn.domain.repository.FlashLearnDatabase

/**
 * Shared harness for Phase 29 real-Room integration tests. Every
 * Repository here is the ACTUAL `data`-module implementation wired to a
 * real (in-memory) Room database — not [com.flashlearn.domain.repository.FakeConceptRepository]
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
    val roomDb: FlashLearnRoomDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        FlashLearnRoomDatabase::class.java
    )
        .addCallback(FlashLearnDatabaseCallback)
        .allowMainThreadQueries()
        .build()

    val database: FlashLearnDatabase = FlashLearnDatabaseImpl(roomDb)

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
