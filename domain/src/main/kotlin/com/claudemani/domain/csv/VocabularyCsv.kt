package com.claudemani.domain.csv

/**
 * Plain CSV format for vocabulary Import/Export (Algorithms v4.20 §9 —
 * one of the four listed formats; XLSX/SQLite are explicitly out of
 * scope, see the tracker for why). Deliberately a pure, dependency-free
 * string transform (no repository, no UseCase) — same split as
 * [com.claudemani.domain.parser.VocabularyParser] itself, so the format
 * rules are unit-testable without any Fake repository at all.
 *
 * Columns: `source,target,notes` (notes optional/nullable). This is a
 * flat, single-language-pair format matching V1's own one-active-pair
 * scope (Descriptions §1.2) — it does not attempt to represent
 * Breakdown, GrammarNote, Category, or multiple translations per
 * Concept; that richer data only round-trips through the FULL JSON
 * Backup (Phase 36), not this simpler CSV.
 */
data class VocabularyCsvRow(val sourceText: String, val targetText: String, val notes: String? = null)

object VocabularyCsv {
    private const val HEADER = "source,target,notes"

    fun encode(rows: List<VocabularyCsvRow>): String = buildString {
        append(HEADER)
        append('\n')
        rows.forEach { row ->
            append(listOf(row.sourceText, row.targetText, row.notes.orEmpty()).joinToString(",") { escape(it) })
            append('\n')
        }
    }

    /**
     * Malformed rows (fewer than 2 columns, or an empty source/target)
     * are silently skipped, not thrown — Parser P0's own §83 principle
     * ("Parser نباید ... Duplicate را بدون Policy حذف کند") is about
     * duplicates, not malformed input; a genuinely empty/broken row here
     * has nothing meaningful to preserve or report back through this
     * pure function's return type. The caller (an ImportVocabularyCsvUseCase)
     * that has row numbers/line context can still choose to report on
     * the row-count mismatch — this function only returns what parsed
     * successfully.
     */
    fun decode(text: String): List<VocabularyCsvRow> {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val dataLines = if (isHeaderLine(lines.first())) lines.drop(1) else lines

        return dataLines.mapNotNull { line ->
            val columns = parseLine(line)
            if (columns.size < 2) return@mapNotNull null
            val source = columns[0].trim()
            val target = columns[1].trim()
            if (source.isEmpty() || target.isEmpty()) return@mapNotNull null
            VocabularyCsvRow(source, target, columns.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() })
        }
    }

    /**
     * A header row is `source,target` or `source,target,notes` (any case, optional
     * spaces). Both forms must be recognised: a hand-written file will often omit the
     * optional `notes` column name, and treating that header as data would silently
     * import a bogus "source → target" word.
     */
    private fun isHeaderLine(line: String): Boolean {
        val columns = parseLine(line).map { it.trim().lowercase() }
        return columns == listOf("source", "target") || columns == listOf("source", "target", "notes")
    }

    private fun escape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    /** RFC-4180-style: quoted fields may contain commas/newlines; `""` inside a quoted field is a literal quote. */
    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
