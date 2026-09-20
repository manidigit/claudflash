package com.claudemani.app.presentation

import com.claudemani.app.navigation.Routes
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppViewModelTest {

    @Test
    fun `default route is Home`() = runTest {
        val viewModel = AppViewModel()
        assertEquals(Routes.HOME, viewModel.uiState.value.currentRoute)
    }

    @Test
    fun `onRouteChanged updates currentRoute`() = runTest {
        val viewModel = AppViewModel()
        viewModel.onRouteChanged(Routes.REVIEW)
        assertEquals(Routes.REVIEW, viewModel.uiState.value.currentRoute)
    }

    @Test
    fun `changing route again fully replaces the previous state`() = runTest {
        val viewModel = AppViewModel()
        viewModel.onRouteChanged(Routes.SETTINGS)
        viewModel.onRouteChanged(Routes.PROGRESS)
        assertEquals(Routes.PROGRESS, viewModel.uiState.value.currentRoute)
    }
}
