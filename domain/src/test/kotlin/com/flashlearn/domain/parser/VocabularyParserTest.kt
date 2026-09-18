package com.flashlearn.domain.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VocabularyParserTest {

    // --- Test Case 1 — numbered entry, semicolon variants kept literal (Gender Variants are P1) ---
    @Test
    fun `Test Case 1 - numbered entry with translation`() {
        val input = "1. el científico; la científica\nدانشمند (مذکر)؛ دانشمند (مؤنث)"
        val result = VocabularyParser.parse(input)

        assertEquals(1, result.entries.size)
        val entry = result.entries.single()
        assertEquals("el científico; la científica", entry.sourceText)
        assertEquals("دانشمند (مذکر)؛ دانشمند (مؤنث)", entry.translationText)
    }

    // --- Test Case 3 — Main Entry + translation + three Breakdown lines ---
    @Test
    fun `Test Case 3 - main entry with breakdown lines`() {
        val input = """
            estoy seguro de que todo irá bien
            مطمئنم که همه‌چیز خوب پیش می‌رود
            estoy seguro de que: مطمئنم که
            todo: همه‌چیز
            irá bien: خوب پیش خواهد رفت
        """.trimIndent()

        val result = VocabularyParser.parse(input)

        assertEquals(1, result.entries.size)
        val entry = result.entries.single()
        assertEquals("estoy seguro de que todo irá bien", entry.sourceText)
        assertEquals("مطمئنم که همه‌چیز خوب پیش می‌رود", entry.translationText)
        assertEquals(3, entry.breakdowns.size)
        assertEquals(ParsedBreakdown("estoy seguro de que", "مطمئنم که"), entry.breakdowns[0])
        assertEquals(ParsedBreakdown("todo", "همه‌چیز"), entry.breakdowns[1])
        assertEquals(ParsedBreakdown("irá bien", "خوب پیش خواهد رفت"), entry.breakdowns[2])
    }

    // --- Test Case 4 — parenthetical Note on its own line ---
    @Test
    fun `Test Case 4 - standalone parenthetical becomes a note`() {
        val input = "la esperanza\nامید\n(در شماره ۹ استفاده شد)"
        val result = VocabularyParser.parse(input)

        val entry = result.entries.single()
        assertEquals("la esperanza", entry.sourceText)
        assertEquals("امید", entry.translationText)
        assertEquals("در شماره ۹ استفاده شد", entry.notes)
    }

    // --- Test Case 5 — grammar note marker with content on the following line ---
    @Test
    fun `Test Case 5 - grammar note marker captures the next line`() {
        val input = "el análisis\nتحلیل\nنکته گرامری:\nاین واژه در این ساختار استفاده شده است."
        val result = VocabularyParser.parse(input)

        val entry = result.entries.single()
        assertEquals("el análisis", entry.sourceText)
        assertEquals("تحلیل", entry.translationText)
        assertEquals("این واژه در این ساختار استفاده شده است.", entry.grammarNotes)
        assertNull(entry.notes)
    }

    // --- Test Case 6 — "Spanish: Persian" after a complete entry is a Breakdown, not a new Entry ---
    @Test
    fun `Test Case 6 - colon line after a complete entry is a breakdown`() {
        val input = "contar con\nروی کسی حساب کردن\nquiero: می‌خواهم"
        val result = VocabularyParser.parse(input)

        assertEquals(1, result.entries.size)
        val entry = result.entries.single()
        assertEquals("contar con", entry.sourceText)
        assertEquals(1, entry.breakdowns.size)
        assertEquals(ParsedBreakdown("quiero", "می‌خواهم"), entry.breakdowns.single())
    }

    // --- Test Case 7 — numbering gaps never fabricate missing entries; boundaries stay distinct ---
    @Test
    fun `Test Case 7 - numbering gaps produce exactly the entries present, never phantom ones`() {
        val input = "24. palabra\n29. otra palabra\n47. tercera palabra"
        val result = VocabularyParser.parse(input)

        // None of these have a translation line, so each is a distinct ORPHAN_SOURCE (§56) —
        // the point of this test is that exactly 3 boundaries were found (not 1 merged, not 24).
        assertTrue(result.entries.isEmpty())
        assertEquals(3, result.orphanLines.size)
        assertEquals(listOf("palabra", "otra palabra", "tercera palabra"), result.orphanLines)
    }

    // --- Test Case 8 — free-standing Persian prose with no entry context is a Comment ---
    @Test
    fun `Test Case 8 - persian prose with no entry context is a comment`() {
        val input = "کلمات این صفحه تنوع بالایی داشتند و جالب بودند"
        val result = VocabularyParser.parse(input)

        assertTrue(result.entries.isEmpty())
        assertEquals(1, result.comments.size)
    }

    // --- Test Case 9 — a mixed-language aside referencing another entry becomes a Note ---
    @Test
    fun `Test Case 9 - mixed language aside becomes a note, not a new entry`() {
        val input = "no puedes hacer una tortilla sin romper huevos\n" +
            "نمی‌توانی برای درست کردن املت تخم‌مرغ‌ها را نشکنی\n" +
            "tortilla قبلاً در شماره ۱۸ ترجمه شد."
        val result = VocabularyParser.parse(input)

        assertEquals(1, result.entries.size)
        val entry = result.entries.single()
        assertEquals("tortilla قبلاً در شماره ۱۸ ترجمه شد.", entry.notes)
    }

    // --- Test Case 10 — typos are never auto-corrected ---
    @Test
    fun `Test Case 10 - source text is never spelling-corrected`() {
        val input = "lo melhor\nترجمه بد"
        val result = VocabularyParser.parse(input)

        assertEquals("lo melhor", result.entries.single().sourceText)
    }

    // --- §21 — a Spanish entry split across two lines is merged, not treated as two entries ---
    @Test
    fun `section 21 - multi-line spanish entry is merged before its translation arrives`() {
        val input = "no puedes hacer\nuna tortilla sin romper huevos\nنمی‌توانی"
        val result = VocabularyParser.parse(input)

        assertEquals(1, result.entries.size)
        assertEquals("no puedes hacer una tortilla sin romper huevos", result.entries.single().sourceText)
    }

    // --- §22 Hâlat B — "Spanish: Persian" on one line, with no prior open entry, is a complete entry ---
    @Test
    fun `section 22 - a bare colon line with no open entry is a complete entry on its own`() {
        val input = "hola: سلام"
        val result = VocabularyParser.parse(input)

        assertEquals(1, result.entries.size)
        val entry = result.entries.single()
        assertEquals("hola", entry.sourceText)
        assertEquals("سلام", entry.translationText)
    }

    // --- §23 — Persian appearing before its Spanish pair is still recognized as a translation pair ---
    @Test
    fun `section 23 - persian before its spanish pair is still paired correctly`() {
        val input = "دانشمند\nel científico"
        val result = VocabularyParser.parse(input)

        assertEquals(1, result.entries.size)
        val entry = result.entries.single()
        assertEquals("el científico", entry.sourceText)
        assertEquals("دانشمند", entry.translationText)
    }

    // --- Duplicate Detection (§44) — identical source + identical translation ---
    @Test
    fun `exact duplicate is detected when source and translation both match`() {
        val input = "el científico\nدانشمند\n\nel científico\nدانشمند"
        val result = VocabularyParser.parse(input)

        assertEquals(2, result.entries.size)
        assertEquals(1, result.duplicates.size)
        assertEquals(DuplicateType.EXACT_DUPLICATE, result.duplicates.single().type)
    }

    // --- Duplicate Detection (§45) — same source, different translation: both kept, flagged ---
    @Test
    fun `same source with different translations is flagged but both entries are kept`() {
        val input = "cura\nکشیش\n\ncura\nدرمان"
        val result = VocabularyParser.parse(input)

        assertEquals(2, result.entries.size)
        val group = result.duplicates.single()
        assertEquals(DuplicateType.SAME_SOURCE_DIFFERENT_TRANSLATION, group.type)
        assertEquals(setOf("کشیش", "درمان"), group.entries.map { it.translationText }.toSet())
    }

    @Test
    fun `no duplicates reported when every source is unique`() {
        val input = "hola\nسلام\n\nadiós\nخداحافظ"
        val result = VocabularyParser.parse(input)

        assertEquals(2, result.entries.size)
        assertTrue(result.duplicates.isEmpty())
    }

    @Test
    fun `blank lines and pure numbering noise are skipped without creating entries`() {
        val input = "\n\n1.\ngato\nگربه\n\n\n"
        val result = VocabularyParser.parse(input)

        assertEquals(1, result.entries.size)
        assertEquals("gato", result.entries.single().sourceText)
    }
}
