package com.flashlearn.app.presentation.addword

import com.flashlearn.domain.parser.DuplicateGroup
import com.flashlearn.domain.parser.DuplicateType
import com.flashlearn.domain.parser.ParseResult
import com.flashlearn.domain.parser.ParsedEntry
import com.flashlearn.domain.usecase.ImportEntryResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class AddWordViewModelMappingTest {

    private fun entry(source: String = "hola", translation: String = "سلام") = ParsedEntry(
        sourceText = source, translationText = translation, sourceLanguage = "es", targetLanguage = "fa",
        originalImportText = "$source\n$translation"
    )

    @Test
    fun `applyParseResult maps every count field and clears any previous import summary`() {
        val previous = AddWordUiState(importSummary = ImportSummary(created = 1))
        val entries = listOf(entry(), entry("adiós", "خداحافظ"))
        val result = ParseResult(
            entries = entries,
            comments = listOf("یک توضیح"),
            orphanLines = listOf("خط بی‌ترجمه"),
            duplicates = listOf(DuplicateGroup("hola", DuplicateType.EXACT_DUPLICATE, entries))
        )

        val newState = applyParseResult(previous, result)

        assertEquals(entries, newState.parsedEntries)
        assertEquals(1, newState.commentCount)
        assertEquals(1, newState.orphanCount)
        assertEquals(1, newState.duplicateGroupCount)
        assertNull(newState.importSummary)
    }

    @Test
    fun `applyParseResult with no findings clears the previous preview`() {
        val previous = AddWordUiState(parsedEntries = listOf(entry()), commentCount = 3)
        val newState = applyParseResult(previous, ParseResult(emptyList(), emptyList(), emptyList(), emptyList()))

        assertEquals(0, newState.parsedEntries.size)
        assertEquals(0, newState.commentCount)
    }

    @Test
    fun `summarizeImportResults tallies each outcome type independently`() {
        val idA = UUID.randomUUID()
        val idB = UUID.randomUUID()
        val results = listOf(
            ImportEntryResult.Created(idA),
            ImportEntryResult.Created(idB),
            ImportEntryResult.Merged(idA),
            ImportEntryResult.AlreadyExists,
            ImportEntryResult.NeedsUserChoice(setOf(idA, idB))
        )

        val summary = summarizeImportResults(results)

        assertEquals(2, summary.created)
        assertEquals(1, summary.merged)
        assertEquals(1, summary.alreadyExists)
        assertEquals(1, summary.conflicts)
    }

    @Test
    fun `summarizeImportResults with no results is all zero`() {
        val summary = summarizeImportResults(emptyList())
        assertEquals(ImportSummary(), summary)
    }
}
