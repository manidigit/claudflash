package com.claudemani.domain.usecase

import com.claudemani.domain.model.Content
import com.claudemani.domain.model.computeCanonicalKey
import com.claudemani.domain.parser.ParsedEntry
import com.claudemani.domain.repository.ContentRepository
import java.util.UUID
import javax.inject.Inject

/** Outcome of importing a single [ParsedEntry], per §7 "اجرای نتیجه توسط لایه بالاتر". */
sealed interface ImportEntryResult {
    data class Created(val conceptId: UUID) : ImportEntryResult
    data class Merged(val conceptId: UUID) : ImportEntryResult

    /** ReuseConcept resolved with nothing new to insert — the whole entry already existed verbatim. */
    data object AlreadyExists : ImportEntryResult

    /** §7 Conflict — never resolved automatically; the caller must ask the user or skip. */
    data class NeedsUserChoice(val matchedConceptIds: Set<UUID>) : ImportEntryResult
}

/**
 * Bridges [com.claudemani.domain.parser.VocabularyParser]'s output to
 * persistence for the Paste Text import flow (Phase 24), using
 * [ResolveConceptForParsedEntryUseCase] to decide Create vs. Reuse vs.
 * Conflict and [CreateConceptUseCase] / [ContentRepository] to actually
 * write.
 *
 * [ParsedEntry.breakdowns] and [ParsedEntry.grammarNotes] are not
 * persisted here — the domain model has no storage for Breakdown or a
 * separate grammar-notes field yet (P0 Parser scope, Phase 18 decision
 * log); only [ParsedEntry.notes] maps onto Content's existing `notes`
 * column. Adding real Breakdown storage is P1 Parser work, not this
 * phase's job.
 */
class ImportParsedEntryUseCase @Inject constructor(
    private val resolveConceptForParsedEntry: ResolveConceptForParsedEntryUseCase,
    private val createConcept: CreateConceptUseCase,
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(entry: ParsedEntry): ImportEntryResult {
        val pieces = listOf(
            ResolvePiece(entry.sourceLanguage, entry.sourceText),
            ResolvePiece(entry.targetLanguage, entry.translationText)
        )

        return when (val resolution = resolveConceptForParsedEntry(pieces)) {
            is ResolveConceptResult.CreateNewConcept -> {
                val conceptId = createConcept(
                    CreateConceptCommand(
                        sourceText = entry.sourceText,
                        targetText = entry.translationText,
                        sourceLanguage = entry.sourceLanguage,
                        targetLanguage = entry.targetLanguage,
                        notes = entry.notes
                    )
                )
                ImportEntryResult.Created(conceptId)
            }

            is ResolveConceptResult.ReuseConcept -> {
                if (resolution.newContentsToInsert.isEmpty()) {
                    ImportEntryResult.AlreadyExists
                } else {
                    resolution.newContentsToInsert.forEach { piece ->
                        contentRepository.upsert(
                            Content(
                                id = UUID.randomUUID(),
                                conceptId = resolution.conceptId,
                                languageCode = piece.languageCode,
                                text = piece.text,
                                canonicalKey = computeCanonicalKey(piece.text)
                            )
                        )
                    }
                    ImportEntryResult.Merged(resolution.conceptId)
                }
            }

            is ResolveConceptResult.Conflict -> ImportEntryResult.NeedsUserChoice(resolution.matchedConceptIds)
        }
    }
}
