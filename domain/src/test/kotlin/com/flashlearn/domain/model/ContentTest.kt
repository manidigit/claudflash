package com.flashlearn.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ContentTest {

    @Test
    fun `trims, lowercases and collapses whitespace`() {
        assertEquals("el científico", computeCanonicalKey("  El   Científico  "))
    }

    @Test
    fun `accents are preserved and change meaning`() {
        // si (if) vs sí (yes) must never collapse to the same key
        assertNotEquals(computeCanonicalKey("si"), computeCanonicalKey("sí"))
        assertNotEquals(computeCanonicalKey("el"), computeCanonicalKey("él"))
    }

    @Test
    fun `punctuation is preserved`() {
        assertEquals("¿qué quieres?", computeCanonicalKey("¿Qué quieres?"))
    }

    @Test
    fun `tabs and multiple spaces collapse to single space`() {
        assertEquals("tener miedo de", computeCanonicalKey("tener\tmiedo    de"))
    }
}
