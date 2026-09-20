package com.claudemani.domain.usecase

import com.claudemani.domain.model.LanguagePair
import com.claudemani.domain.repository.FakeConceptRepository
import com.claudemani.domain.repository.FakeConceptTagRepository
import com.claudemani.domain.repository.FakeContentRepository
import com.claudemani.domain.repository.FakeDifficultyStateRepository
import com.claudemani.domain.repository.FakeClaudemaniDatabase
import com.claudemani.domain.repository.FakeLanguagePairRepository
import com.claudemani.domain.repository.FakeLearningStateRepository
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VocabularyCsvUseCasesTest {

    private class Fixture {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val learningStates = FakeLearningStateRepository()
        val difficultyStates = FakeDifficultyStateRepository()
        val conceptTags = FakeConceptTagRepository()
        val languagePairs = FakeLanguagePairRepository()
        val database = FakeClaudemaniDatabase()

        val createConcept = CreateConceptUseCase(concepts, contents, learningStates, difficultyStates, conceptTags, database)
        val resolve = ResolveConceptForParsedEntryUseCase(contents)
        val importParsedEntry = ImportParsedEntryUseCase(resolve, createConcept, contents)
        val getActivePair = GetActiveLanguagePairUseCase(languagePairs)

        val importCsv = ImportVocabularyCsvUseCase(getActivePair, importParsedEntry)
        val exportCsv = ExportVocabularyCsvUseCase(concepts, contents, getActivePair)

        init {
            kotlinx.coroutines.runBlocking {
                languagePairs.insert(LanguagePair(UUID.randomUUID(), "es", "fa", isActive = true))
            }
        }
    }

    @Test
    fun `import creates one concept per new row`() = runTest {
        val fx = Fixture()

        val summary = fx.importCsv("source,target,notes\nhola,سلام,\nadiós,خداحافظ,used often")

        assertEquals(2, summary.totalRows)
        assertEquals(2, summary.created)
        assertEquals(0, summary.merged)
        assertEquals(2, fx.concepts.getAll().size)
    }

    @Test
    fun `importing the same csv twice reports AlreadyExists the second time`() = runTest {
        val fx = Fixture()
        val csv = "source,target\nhola,سلام"

        fx.importCsv(csv)
        val second = fx.importCsv(csv)

        assertEquals(0, second.created)
        assertEquals(1, second.alreadyExists)
        assertEquals(1, fx.concepts.getAll().size)
    }

    @Test
    fun `export produces a row for every concept with both languages present`() = runTest {
        val fx = Fixture()
        fx.importCsv("source,target,notes\nhola,سلام,\nadiós,خداحافظ,")

        val csv = fx.exportCsv()
        val rows = com.claudemani.domain.csv.VocabularyCsv.decode(csv)

        assertEquals(2, rows.size)
        assertTrue(rows.any { it.sourceText == "hola" && it.targetText == "سلام" })
        assertTrue(rows.any { it.sourceText == "adiós" && it.targetText == "خداحافظ" })
    }

    @Test
    fun `export then import round-trips through a fresh fixture`() = runTest {
        val source = Fixture()
        source.importCsv("source,target,notes\nhola,سلام,greeting\nadiós,خداحافظ,")
        val csv = source.exportCsv()

        val target = Fixture()
        val summary = target.importCsv(csv)

        assertEquals(2, summary.created)
        assertEquals(2, target.concepts.getAll().size)
    }
}
