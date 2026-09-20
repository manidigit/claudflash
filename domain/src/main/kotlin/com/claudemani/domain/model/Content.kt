package com.claudemani.domain.model

import java.util.UUID

/**
 * A Concept's text in one language. Unique per (conceptId, languageCode)
 * — enforced at the Room layer (Phase 6). [canonicalKey] is a derived,
 * persisted, indexed field used only for Matching/Duplicate Detection; it
 * is NOT the same thing as the Parser's transient normalizedText
 * (Content Data Model Finalization, Appendix M — FROZEN).
 *
 * canonicalKey computation: trim + lowercase + collapse consecutive
 * whitespace. Accents and punctuation are preserved (Spanish accents are
 * meaning-bearing: si ≠ sí, el ≠ él).
 */
data class Content(
    val id: UUID,
    val conceptId: UUID,
    val languageCode: String,
    val text: String,
    val canonicalKey: String,
    val notes: String? = null,
    val pronunciation: String? = null,
    val example: String? = null,
    val dataVersion: Int = 0
)

/** trim + lowercase + collapse consecutive spaces. Accents/punctuation kept. */
fun computeCanonicalKey(text: String): String =
    text.trim().lowercase().replace(Regex("\\s+"), " ")
