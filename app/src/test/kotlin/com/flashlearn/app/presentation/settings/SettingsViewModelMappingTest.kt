package com.flashlearn.app.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelMappingTest {

    @Test
    fun `empty text means no limit`() {
        val result = parseMaxReviewCards("")
        assertEquals(MaxReviewCardsParseResult.Valid(null), result)
    }

    @Test
    fun `blank text (whitespace only) also means no limit`() {
        val result = parseMaxReviewCards("   ")
        assertEquals(MaxReviewCardsParseResult.Valid(null), result)
    }

    @Test
    fun `positive integer text is valid`() {
        val result = parseMaxReviewCards("20")
        assertEquals(MaxReviewCardsParseResult.Valid(20), result)
    }

    @Test
    fun `surrounding whitespace is trimmed before parsing`() {
        val result = parseMaxReviewCards("  15  ")
        assertEquals(MaxReviewCardsParseResult.Valid(15), result)
    }

    @Test
    fun `zero is rejected`() {
        val result = parseMaxReviewCards("0")
        assertTrue(result is MaxReviewCardsParseResult.Invalid)
    }

    @Test
    fun `negative number is rejected`() {
        val result = parseMaxReviewCards("-5")
        assertTrue(result is MaxReviewCardsParseResult.Invalid)
    }

    @Test
    fun `non-numeric text is rejected`() {
        val result = parseMaxReviewCards("abc")
        assertTrue(result is MaxReviewCardsParseResult.Invalid)
    }
}
