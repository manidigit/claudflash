package com.flashlearn.domain.csv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VocabularyCsvTest {

    @Test
    fun `encode then decode reproduces the same rows`() {
        val rows = listOf(
            VocabularyCsvRow("hola", "سلام", null),
            VocabularyCsvRow("adiós", "خداحافظ", "used often")
        )

        val decoded = VocabularyCsv.decode(VocabularyCsv.encode(rows))

        assertEquals(rows, decoded)
    }

    @Test
    fun `a value containing a comma is quoted and survives round-trip`() {
        val rows = listOf(VocabularyCsvRow("uno, dos", "یک، دو", null))

        val decoded = VocabularyCsv.decode(VocabularyCsv.encode(rows))

        assertEquals(rows, decoded)
    }

    @Test
    fun `a value containing a literal quote survives round-trip`() {
        val rows = listOf(VocabularyCsvRow("""he said "hola"""", "او گفت «سلام»", null))

        val decoded = VocabularyCsv.decode(VocabularyCsv.encode(rows))

        assertEquals(rows, decoded)
    }

    @Test
    fun `decode works without a header row too`() {
        val decoded = VocabularyCsv.decode("hola,سلام\nadiós,خداحافظ")

        assertEquals(2, decoded.size)
        assertEquals(VocabularyCsvRow("hola", "سلام", null), decoded[0])
    }

    @Test
    fun `a two-column header row is recognised and skipped, not imported as a word`() {
        val decoded = VocabularyCsv.decode("source,target\nhola,سلام")

        assertEquals(listOf(VocabularyCsvRow("hola", "سلام", null)), decoded)
    }

    @Test
    fun `header detection ignores case and surrounding spaces`() {
        val decoded = VocabularyCsv.decode(" Source , TARGET , Notes \nhola,سلام")

        assertEquals(1, decoded.size)
        assertEquals("hola", decoded[0].sourceText)
    }

    @Test
    fun `rows missing a target column are skipped, not thrown`() {
        val decoded = VocabularyCsv.decode("source,target,notes\nhola,سلام\nonlyonecolumn")

        assertEquals(1, decoded.size)
    }

    @Test
    fun `blank lines are ignored`() {
        val decoded = VocabularyCsv.decode("source,target,notes\nhola,سلام\n\n\nadiós,خداحافظ")

        assertEquals(2, decoded.size)
    }

    @Test
    fun `empty input decodes to an empty list`() {
        assertTrue(VocabularyCsv.decode("").isEmpty())
    }
}
