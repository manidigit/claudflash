package com.claudemani.domain.usecase

import com.claudemani.domain.parser.ParsedEntry
import com.claudemani.domain.repository.FakeConceptRepository
import com.claudemani.domain.repository.FakeConceptTagRepository
import com.claudemani.domain.repository.FakeContentRepository
import com.claudemani.domain.repository.FakeDifficultyStateRepository
import com.claudemani.domain.repository.FakeClaudemaniDatabase
import com.claudemani.domain.repository.FakeLearningStateRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportParsedEntryUseCaseTest {

    private class Fixture {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val learningStates = FakeLearningStateRepository()
        val difficultyStates = FakeDifficultyStateRepository()
        val conceptTags = FakeConceptTagRepository()
        val database = FakeClaudemaniDatabase()

        val createConcept = CreateConceptUseCase(concepts, contents, learningStates, difficultyStates, conceptTags, database)
        val resolve = ResolveConceptForParsedEntryUseCase(contents)
        val useCase = ImportParsedEntryUseCase(resolve, createConcept, contents)
    }

    private fun entry(source: String, translation: String, notes: String? = null) = ParsedEntry(
        sourceText = source, translationText = translation, sourceLanguage = "es", targetLanguage = "fa",
        notes = notes, originalImportText = "$source\n$translation"
    )

    @Test
    fun `brand new entry creates a concept`() = runTest {
        val fx = Fixture()
        val result = fx.useCase(entry("el gato", "گربه"))

        assertTrue(result is ImportEntryResult.Created)
        assertEquals(1, fx.concepts.getAll().size)
        assertEquals(2, fx.contents.getAll().size)
    }

    @Test
    fun `importing the exact same entry twice reports AlreadyExists the second time`() = runTest {
        val fx = Fixture()
        fx.useCase(entry("el gato", "گربه"))
        val second = fx.useCase(entry("el gato", "گربه"))

        assertEquals(ImportEntryResult.AlreadyExists, second)
        assertEquals(1, fx.concepts.getAll().size) // no duplicate concept created
    }

    @Test
    fun `a new translation for an existing source merges into the same concept`() = runTest {
        val fx = Fixture()
        val first = fx.useCase(entry("cura", "کشیش")) as ImportEntryResult.Created

        val second = fx.useCase(entry("cura", "درمان"))

        assertTrue(second is ImportEntryResult.Merged)
        assertEquals(first.conceptId, (second as ImportEntryResult.Merged).conceptId)
        assertEquals(1, fx.concepts.getAll().size)
        assertEquals(3, fx.contents.getAllByConceptId(first.conceptId).size) // cura, کشیش, درمان
    }

    @Test
    fun `a genuine conflict is surfaced, not silently resolved`() = runTest {
        val fx = Fixture()
        fx.useCase(entry("hola", "معنی قدیمی"))
        fx.useCase(entry("چیز دیگر", "معنی جدید"))
        // Now import an entry whose two pieces each match a DIFFERENT existing concept.
        val result = fx.useCase(entry("hola", "معنی جدید"))

        assertTrue(result is ImportEntryResult.NeedsUserChoice)
        assertEquals(2, (result as ImportEntryResult.NeedsUserChoice).matchedConceptIds.size)
    }

    @Test
    fun `notes are carried over when creating a new concept`() = runTest {
        val fx = Fixture()
        fx.useCase(entry("la esperanza", "امید", notes = "در شماره ۹ استفاده شد"))

        val content = fx.contents.getAll().first { it.languageCode == "es" }
        assertEquals("در شماره ۹ استفاده شد", content.notes)
    }
}
