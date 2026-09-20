package com.claudemani.domain.usecase

import com.claudemani.domain.csv.VocabularyCsv
import com.claudemani.domain.csv.VocabularyCsvRow
import com.claudemani.domain.parser.ParsedEntry
import com.claudemani.domain.repository.ConceptRepository
import com.claudemani.domain.repository.ContentRepository
import javax.inject.Inject

/**
 * Export: one CSV row per active Concept that has Content in both
 * languages of the current active [com.claudemani.domain.model.LanguagePair]
 * (README/tracker gap #5, Algorithms §9 — CSV only; XLSX/SQLite are
 * explicitly out of scope, see the tracker). A Concept missing either
 * side (shouldn't normally happen — CreateConceptUseCase always writes
 * both) is silently skipped rather than exported with a blank column.
 */
class ExportVocabularyCsvUseCase @Inject constructor(
    private val conceptRepository: ConceptRepository,
    private val contentRepository: ContentRepository,
    private val getActiveLanguagePair: GetActiveLanguagePairUseCase
) {
    suspend operator fun invoke(): String {
        val pair = getActiveLanguagePair()
        val rows = conceptRepository.getAllActive().mapNotNull { concept ->
            val contents = contentRepository.getAllByConceptId(concept.id)
            val source = contents.find { it.languageCode == pair.sourceLanguage } ?: return@mapNotNull null
            val target = contents.find { it.languageCode == pair.targetLanguage } ?: return@mapNotNull null
            VocabularyCsvRow(source.text, target.text, source.notes ?: target.notes)
        }
        return VocabularyCsv.encode(rows)
    }
}

data class VocabularyCsvImportSummary(
    val totalRows: Int,
    val created: Int,
    val merged: Int,
    val alreadyExists: Int,
    val conflicts: Int
)

/**
 * Import: reuses [ImportParsedEntryUseCase] — the exact same
 * Create-vs-Merge-vs-Conflict resolution the Paste Text flow (Phase 24)
 * already uses via [ResolveConceptForParsedEntryUseCase] — by wrapping
 * each CSV row as a [ParsedEntry]. No separate duplicate-detection logic
 * is written here.
 */
class ImportVocabularyCsvUseCase @Inject constructor(
    private val getActiveLanguagePair: GetActiveLanguagePairUseCase,
    private val importParsedEntry: ImportParsedEntryUseCase
) {
    suspend operator fun invoke(csvText: String): VocabularyCsvImportSummary {
        val pair = getActiveLanguagePair()
        val rows = VocabularyCsv.decode(csvText)

        var created = 0
        var merged = 0
        var alreadyExists = 0
        var conflicts = 0

        rows.forEach { row ->
            val entry = ParsedEntry(
                sourceText = row.sourceText,
                translationText = row.targetText,
                sourceLanguage = pair.sourceLanguage,
                targetLanguage = pair.targetLanguage,
                notes = row.notes,
                originalImportText = "${row.sourceText},${row.targetText}"
            )
            when (importParsedEntry(entry)) {
                is ImportEntryResult.Created -> created++
                is ImportEntryResult.Merged -> merged++
                ImportEntryResult.AlreadyExists -> alreadyExists++
                is ImportEntryResult.NeedsUserChoice -> conflicts++
            }
        }

        return VocabularyCsvImportSummary(
            totalRows = rows.size, created = created, merged = merged,
            alreadyExists = alreadyExists, conflicts = conflicts
        )
    }
}
