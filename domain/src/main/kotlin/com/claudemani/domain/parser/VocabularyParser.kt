package com.claudemani.domain.parser

import com.claudemani.domain.model.computeCanonicalKey
import java.text.Normalizer

/** Algorithms v4.20 §5 — one of four detectable states for a single line. */
enum class DetectedLanguage { SPANISH, PERSIAN, MIXED, UNKNOWN }

/** A single "Spanish: Persian" sub-line attached to a Main Entry (§10). Never promoted to its own Entry. */
data class ParsedBreakdown(val sourcePart: String, val translationPart: String)

/**
 * One parsed vocabulary entry, ready to become a Concept + two Contents.
 * [originalImportText] preserves the untouched raw lines this entry was
 * built from (§70 — Preserve Original); nothing here is ever
 * spelling-corrected (§51/§83).
 */
data class ParsedEntry(
    val sourceText: String,
    val translationText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val notes: String? = null,
    val grammarNotes: String? = null,
    val breakdowns: List<ParsedBreakdown> = emptyList(),
    val originalImportText: String
)

/** §43/44/45 — only the two crisply-defined duplicate types are detected in P0; see the class doc. */
enum class DuplicateType { EXACT_DUPLICATE, SAME_SOURCE_DIFFERENT_TRANSLATION }

data class DuplicateGroup(val canonicalKey: String, val type: DuplicateType, val entries: List<ParsedEntry>)

data class ParseResult(
    val entries: List<ParsedEntry>,
    val comments: List<String>,
    val orphanLines: List<String>,
    val duplicates: List<DuplicateGroup>
)

/**
 * Vocabulary Import & Parsing Algorithm v1.0 — **P0 scope only**
 * (Algorithms v4.20 §93): Normalization, Language Detection, Line
 * Classification, Entry Boundary Detection, Translation Detection,
 * Breakdown Detection, Note Detection, Duplicate Detection. Entirely
 * offline/deterministic (§1, §95) — no AI, no network.
 *
 * Deliberately deferred to P1 (per §93's own P1/P2 split — NOT
 * implemented here): Confidence/Evidence scoring, Gender Variants
 * (`el científico; la científica` stays one literal [ParsedEntry.sourceText]
 * rather than being split into variants), Entry Type detection (every
 * entry is left for the caller to default, e.g. to `EntryType.WORD`),
 * the Relationship System (DERIVED_FROM/…), Possible Correction, and the
 * structured Import Report/Manual Review pipeline. This means the
 * DERIVATIVE and RELATION [DetectedLanguage]-adjacent line types from the
 * doc's own Line Classification enum are not actively detected — a line
 * like "از ir" simply falls through to the generic note/orphan handling
 * below rather than building a relationship graph.
 *
 * Language/line-classification heuristics below (which characters count
 * as a "Spanish signal", the word-count threshold for [isLikelyComment])
 * are this implementation's own reasonable interpretation where the
 * Algorithm gives signals/examples rather than an exact formula — see
 * the Phase 18 decision log in PROGRESS_TRACKER.md.
 */
object VocabularyParser {

    private val PERSIAN_CHARS = "ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی".toSet()
    private val SPANISH_ACCENT_CHARS = "áéíóúüñ¿¡".toSet()
    private val SPANISH_FUNCTION_WORDS = setOf(
        "el", "la", "los", "las", "un", "una", "de", "del", "que", "para", "con", "por", "en", "a", "y", "o", "pero", "si", "como"
    )

    /** §11/§12 — only the markers with a crisp, literal form given in the spec. */
    private val NOTE_MARKERS = listOf("نکته گرامری", "نکته", "توضیحات", "توجه", "احتمال اشتباه")
    private const val GRAMMAR_NOTE_MARKER = "نکته گرامری"

    private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    private val NUMBERING_PREFIX_REGEX = Regex("""^\s*\d+\s*[.):\-]\s*""")
    private val LEADING_BULLET_REGEX = Regex("""^[\s•*\-—_→»«➜#]+""")

    /** §3/§4 — Unicode NFC, line-ending/whitespace/zero-width cleanup. Original text is preserved by the caller. */
    fun normalizeRaw(raw: String): String {
        var t = Normalizer.normalize(raw, Normalizer.Form.NFC)
        t = t.replace("\r\n", "\n").replace('\r', '\n')
        t = t.replace("\u200B", "")
        t = t.replace('\t', ' ')
        return t.lines().joinToString("\n") { it.replace(Regex(" +"), " ") }
    }

    private fun toStandardDigits(s: String): String =
        s.map { c -> val i = PERSIAN_DIGITS.indexOf(c); if (i >= 0) ('0' + i) else c }.joinToString("")

    /**
     * §15–17 — strips a leading numbering marker (a strong entry-boundary
     * signal) or a leading bullet/decoration character. Returns the
     * cleaned line and whether numbering was found; a line that is pure
     * numbering/bullet noise strips down to blank and the caller skips it.
     */
    internal fun stripLeadingMarkers(line: String): Pair<String, Boolean> {
        val digitLine = toStandardDigits(line)
        val numberMatch = NUMBERING_PREFIX_REGEX.find(digitLine)
        if (numberMatch != null) {
            return line.substring(numberMatch.value.length).trim() to true
        }
        return line.replace(LEADING_BULLET_REGEX, "").trim() to false
    }

    /** §5.1/§5.2 — Persian-character presence, Spanish accents/¿¡, and common Spanish function words. */
    internal fun detectLanguage(line: String): DetectedLanguage {
        val hasPersian = line.any { it in PERSIAN_CHARS }
        val hasSpanishAccent = line.any { it in SPANISH_ACCENT_CHARS }
        val words = line.lowercase().split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }
        val hasSpanishFunctionWord = words.any { it in SPANISH_FUNCTION_WORDS }
        val hasLatinLetter = line.any { it.isLetter() && it.code in 0x0041..0x024F }
        val spanishSignal = hasSpanishAccent || hasSpanishFunctionWord || hasLatinLetter
        return when {
            hasPersian && spanishSignal -> DetectedLanguage.MIXED
            hasPersian -> DetectedLanguage.PERSIAN
            spanishSignal -> DetectedLanguage.SPANISH
            else -> DetectedLanguage.UNKNOWN
        }
    }

    private fun matchedNoteMarker(line: String): String? =
        NOTE_MARKERS.firstOrNull { marker -> line.startsWith(marker) && (line.length == marker.length || line[marker.length] == ':') }

    private fun isWholeLineParenthetical(line: String): Boolean =
        line.startsWith("(") && line.endsWith(")") && line.count { it == '(' } == line.count { it == ')' }

    /**
     * Informed heuristic (not literally specified — see class doc): a
     * Persian-only line of several words, seen with no entry currently
     * open, reads as free commentary (§14 Test Case 8) rather than a
     * short vocabulary translation.
     */
    private fun isLikelyComment(line: String): Boolean =
        detectLanguage(line) == DetectedLanguage.PERSIAN &&
            line.split(Regex("\\s+")).count { it.isNotBlank() } >= 6

    private class MutableEntry(var sourceText: String, val originalLines: MutableList<String>) {
        var translationText: String? = null
        val notes: MutableList<String> = mutableListOf()
        val grammarNotes: MutableList<String> = mutableListOf()
        val breakdowns: MutableList<ParsedBreakdown> = mutableListOf()
    }

    /**
     * Parses a raw pasted block into entries. Never throws on malformed
     * input — anything that doesn't confidently fit is preserved in
     * [ParseResult.orphanLines] or [ParseResult.comments] rather than
     * silently dropped (§92: "Detect, Separate, Classify, Relate,
     * Preserve" — never invent, never discard).
     */
    fun parse(rawText: String, sourceLanguage: String = "es", targetLanguage: String = "fa"): ParseResult {
        val lines = normalizeRaw(rawText).lines()

        val entries = mutableListOf<ParsedEntry>()
        val comments = mutableListOf<String>()
        val orphanLines = mutableListOf<String>()

        var current: MutableEntry? = null
        var pendingPersian: String? = null // a Persian line seen before its Spanish pair (§23)
        var awaitingGrammarNoteText = false
        var awaitingNoteText = false

        fun finalizeCurrent() {
            val entry = current ?: return
            val translation = entry.translationText
            if (translation != null) {
                entries.add(
                    ParsedEntry(
                        sourceText = entry.sourceText,
                        translationText = translation,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        notes = entry.notes.takeIf { it.isNotEmpty() }?.joinToString(" "),
                        grammarNotes = entry.grammarNotes.takeIf { it.isNotEmpty() }?.joinToString(" "),
                        breakdowns = entry.breakdowns.toList(),
                        originalImportText = entry.originalLines.joinToString("\n")
                    )
                )
            } else {
                // §56 ORPHAN_SOURCE — a source line was seen but no translation ever followed;
                // preserved for manual review rather than discarded.
                //
                // Uses entry.sourceText (already number-stripped, same as a
                // successful entry's own sourceText field just above) — not
                // entry.originalLines, which still carries raw numbering
                // (e.g. "24. palabra"). originalImportText (line 180) is the
                // one field meant to keep that raw text (§70 Preserve
                // Original); orphanLines is not that field. Bug found via a
                // real CI test failure (VocabularyParserTest, numbering-gaps
                // case) — boundary detection itself was already correct
                // (all three of 24/29/47 were found); only the stored text
                // for the orphan was wrong.
                orphanLines.add(entry.sourceText)
            }
            current = null
        }

        for (rawLine in lines) {
            if (rawLine.isBlank()) continue
            val (stripped, hadNumbering) = stripLeadingMarkers(rawLine)
            if (stripped.isBlank()) continue // pure numbering/bullet noise, e.g. a lone "1."

            if (awaitingGrammarNoteText) {
                current?.grammarNotes?.add(stripped)
                current?.originalLines?.add(rawLine)
                awaitingGrammarNoteText = false
                continue
            }
            if (awaitingNoteText) {
                current?.notes?.add(stripped)
                current?.originalLines?.add(rawLine)
                awaitingNoteText = false
                continue
            }

            val noteMarker = matchedNoteMarker(stripped)
            if (noteMarker != null && current != null) {
                val rest = stripped.substringAfter(noteMarker).trimStart(':', ' ')
                val isGrammar = noteMarker == GRAMMAR_NOTE_MARKER
                current!!.originalLines.add(rawLine)
                if (rest.isNotBlank()) {
                    if (isGrammar) current!!.grammarNotes.add(rest) else current!!.notes.add(rest)
                } else {
                    if (isGrammar) awaitingGrammarNoteText = true else awaitingNoteText = true
                }
                continue
            }

            // §29 — a standalone parenthetical line is a Note on the entry it follows.
            if (isWholeLineParenthetical(stripped) && current != null && current!!.translationText != null) {
                current!!.notes.add(stripped.removePrefix("(").removeSuffix(")").trim())
                current!!.originalLines.add(rawLine)
                continue
            }

            // §22/§26/§27 — a colon splits Source:Translation. Which role it plays depends on
            // context (§26 "Colon فقط Signal است، نه تصمیم نهایی"): if an entry with its
            // translation already captured is open, this is one of ITS Breakdown lines;
            // otherwise it is a complete new Main Entry given in one line (Hâlat B, §22).
            if (stripped.contains(":") && noteMarker == null) {
                val idx = stripped.indexOf(':')
                val left = stripped.substring(0, idx).trim()
                val right = stripped.substring(idx + 1).trim()
                if (left.isNotBlank() && right.isNotBlank()) {
                    if (current != null && current!!.translationText != null && !hadNumbering) {
                        current!!.breakdowns.add(ParsedBreakdown(left, right))
                        current!!.originalLines.add(rawLine)
                    } else {
                        finalizeCurrent()
                        current = MutableEntry(left, mutableListOf(rawLine)).apply { translationText = right }
                    }
                    continue
                }
            }

            val lang = detectLanguage(stripped)

            // §23 — Persian appearing before its Spanish pair: hold it rather than treat it as
            // a Comment/Note immediately.
            if (lang == DetectedLanguage.PERSIAN && current == null && pendingPersian == null && !isLikelyComment(stripped)) {
                pendingPersian = stripped
                continue
            }

            if (pendingPersian != null) {
                if (lang == DetectedLanguage.SPANISH || lang == DetectedLanguage.UNKNOWN) {
                    finalizeCurrent()
                    current = MutableEntry(stripped, mutableListOf(rawLine)).apply { translationText = pendingPersian }
                    pendingPersian = null
                    continue
                }
                // Didn't pan out as a Candidate Pair — release it rather than lose it (§92).
                comments.add(pendingPersian!!)
                pendingPersian = null
            }

            if (lang == DetectedLanguage.PERSIAN && current != null && current!!.translationText == null) {
                current!!.translationText = stripped
                current!!.originalLines.add(rawLine)
                continue
            }

            if (lang == DetectedLanguage.SPANISH || lang == DetectedLanguage.UNKNOWN) {
                // §21 — still awaiting a translation and no numbering seen: this is a
                // continuation of the same (multi-line) Spanish entry, not a new one.
                if (current != null && current!!.translationText == null && !hadNumbering) {
                    current!!.sourceText = "${current!!.sourceText} $stripped"
                    current!!.originalLines.add(rawLine)
                    continue
                }
                finalizeCurrent()
                current = MutableEntry(stripped, mutableListOf(rawLine))
                continue
            }

            if (isLikelyComment(stripped) && current == null) {
                comments.add(stripped)
                continue
            }

            // Nothing matched confidently (e.g. a MIXED-language aside like Test Case 9's
            // "tortilla قبلاً در شماره ۱۸ ترجمه شد."): attach to the open entry as a Note rather
            // than silently drop it; with no open entry, preserve it as an orphan line.
            if (current != null) {
                current!!.notes.add(stripped)
                current!!.originalLines.add(rawLine)
            } else {
                orphanLines.add(stripped)
            }
        }

        finalizeCurrent()
        pendingPersian?.let { orphanLines.add(it) }

        return ParseResult(entries, comments, orphanLines, detectDuplicates(entries))
    }

    /**
     * §40/§41/§43–47 — must run after parsing, keyed by [computeCanonicalKey]
     * of [ParsedEntry.sourceText] (same canonicalization rule used
     * everywhere else in the app — accents/punctuation preserved, §42).
     * Only the two duplicate types with a crisp, unambiguous definition
     * are detected here; POSSIBLE_DUPLICATE and RELATED_FORM need fuzzy
     * matching and are deferred to P1 along with the rest of §93's P1 list.
     */
    fun detectDuplicates(entries: List<ParsedEntry>): List<DuplicateGroup> =
        entries.groupBy { computeCanonicalKey(it.sourceText) }
            .filterValues { it.size > 1 }
            .map { (key, group) ->
                val distinctTranslations = group.map { computeCanonicalKey(it.translationText) }.distinct()
                val type = if (distinctTranslations.size == 1) {
                    DuplicateType.EXACT_DUPLICATE
                } else {
                    DuplicateType.SAME_SOURCE_DIFFERENT_TRANSLATION
                }
                DuplicateGroup(key, type, group)
            }
}
