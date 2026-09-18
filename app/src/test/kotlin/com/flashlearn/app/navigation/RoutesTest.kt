package com.flashlearn.app.navigation

import com.flashlearn.domain.model.ReviewType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-level contract test for the primary/secondary destinations
 * (Descriptions §3.4/Phase 8: "A source-level contract test verifies
 * all primary destinations and their stable routes").
 */
class RoutesTest {

    @Test
    fun `all seven destinations are present`() {
        assertEquals(7, Routes.ALL.size)
        assertTrue(Routes.ALL.contains(Routes.HOME))
        assertTrue(Routes.ALL.contains(Routes.REVIEW))
        assertTrue(Routes.ALL.contains(Routes.ADD_WORD))
        assertTrue(Routes.ALL.contains(Routes.PROGRESS))
        assertTrue(Routes.ALL.contains(Routes.SETTINGS))
        assertTrue(Routes.ALL.contains(Routes.CATEGORIES))
        assertTrue(Routes.ALL.contains(Routes.ABOUT))
    }

    @Test
    fun `every route is unique`() {
        assertEquals(Routes.ALL.size, Routes.ALL.distinct().size)
    }

    @Test
    fun `no route is blank`() {
        assertTrue(Routes.ALL.all { it.isNotBlank() })
    }

    @Test
    fun `reviewRoute without a type returns the plain base route`() {
        assertEquals(Routes.REVIEW, Routes.reviewRoute())
    }

    @Test
    fun `reviewRoute with a type embeds it as a query argument`() {
        assertEquals("review?reviewType=DAILY", Routes.reviewRoute(ReviewType.DAILY))
        assertEquals("review?reviewType=RANDOM", Routes.reviewRoute(ReviewType.RANDOM))
    }

    @Test
    fun `route pattern still starts with the base review route`() {
        assertTrue(Routes.REVIEW_ROUTE_PATTERN.startsWith(Routes.REVIEW))
    }
}
