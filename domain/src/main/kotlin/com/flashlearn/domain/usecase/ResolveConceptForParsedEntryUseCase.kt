package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.computeCanonicalKey
import com.flashlearn.domain.repository.ContentRepository
import java.util.UUID
import javax.inject.Inject

/** One languageCode+text pair from a parsed entry — matches the Algorithm's "Piece". */
data class ResolvePiece(val languageCode: String, val text: String)

/** Result of [ResolveConceptForParsedEntryUseCase] — what the caller should do next, never done automatically. */
sealed interface ResolveConceptResult {
    data class ReuseConcept(val conceptId: UUID, val newContentsToInsert: List<ResolvePiece>) : ResolveConceptResult
    data class CreateNewConcept(val allContentsToInsert: List<ResolvePiece>) : ResolveConceptResult
    data class Conflict(val matchedConceptIds: Set<UUID>, val pieces: List<ResolvePiece>) : ResolveConceptResult
}

/**
 * ResolveConceptForParsedEntry (Algorithms v4.20 §7). Read-only —
 * decides whether a parsed entry's pieces belong to a brand-new Concept,
 * an existing one (only the not-yet-present pieces need inserting), or
 * a genuine Conflict (pieces matched more than one existing Concept) —
 * it never creates, merges, or inserts anything itself; that is entirely
 * the caller's job ("مرحله ۳ — اجرای نتیجه توسط لایه بالاتر").
 *
 * Matching uses [computeCanonicalKey] + [ContentRepository.findByCanonicalKey]
 * (the `(languageCode, canonicalKey)` index), per the later, FROZEN
 * Appendix M.3 override — NOT the superseded §7 pseudocode's own
 * `normalize(text) = trim().lowercase()` (no whitespace-collapse) run as
 * a live comparison; Appendix M.3 explicitly replaces that pseudocode.
 */
class ResolveConceptForParsedEntryUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(pieces: List<ResolvePiece>): ResolveConceptResult {
        val matchedIdsByPiece: Map<ResolvePiece, Set<UUID>> = pieces.associateWith { piece ->
            contentRepository.findByCanonicalKey(piece.languageCode, computeCanonicalKey(piece.text))
                .map { it.conceptId }
                .toSet()
        }
        val matchedIds = matchedIdsByPiece.values.flatten().toSet()

        return when {
            matchedIds.size > 1 -> ResolveConceptResult.Conflict(matchedIds, pieces)
            matchedIds.isEmpty() -> ResolveConceptResult.CreateNewConcept(pieces)
            else -> {
                val conceptId = matchedIds.single()
                val newPieces = pieces.filter { piece -> conceptId !in matchedIdsByPiece.getValue(piece) }
                ResolveConceptResult.ReuseConcept(conceptId, newPieces)
            }
        }
    }
}
