package com.claudemani.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `UUID round-trips through String`() {
        val original = UUID.randomUUID()
        val stored = converters.fromUUID(original)
        val restored = converters.toUUID(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `null UUID round-trips to null`() {
        assertNull(converters.fromUUID(null))
        assertNull(converters.toUUID(null))
    }

    @Test
    fun `Instant round-trips through epoch millis`() {
        val original = Instant.parse("2026-09-13T08:30:00.123Z")
        val stored = converters.fromInstant(original)
        val restored = converters.toInstant(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `null Instant round-trips to null`() {
        assertNull(converters.fromInstant(null))
        assertNull(converters.toInstant(null))
    }
}
