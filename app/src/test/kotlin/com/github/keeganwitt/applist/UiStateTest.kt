package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateTest {
    @Test
    fun `given default UiState, when created, then has default values`() {
        val state = UiState()

        assertEquals(AppInfoField.VERSION, state.selectedField)
        assertFalse(state.showSystem)
        assertFalse(state.descending)
        assertEquals("", state.query)
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `given UiState, when copy with selectedField, then selectedField is updated`() {
        val state = UiState()

        val newState = state.copy(selectedField = AppInfoField.TARGET_SDK)

        assertEquals(AppInfoField.TARGET_SDK, newState.selectedField)
    }

    @Test
    fun `given UiState, when copy with showSystem, then showSystem is updated`() {
        val state = UiState()

        val newState = state.copy(showSystem = true)

        assertTrue(newState.showSystem)
    }

    @Test
    fun `given UiState, when copy with descending, then descending is updated`() {
        val state = UiState()

        val newState = state.copy(descending = true)

        assertTrue(newState.descending)
    }

    @Test
    fun `given UiState, when copy with query, then query is updated`() {
        val state = UiState()

        val newState = state.copy(query = "test")

        assertEquals("test", newState.query)
    }

    @Test
    fun `given UiState, when copy with isLoading, then isLoading is updated`() {
        val state = UiState()

        val newState = state.copy(isLoading = true)

        assertTrue(newState.isLoading)
    }

    @Test
    fun `given UiState, when copy with items, then items are updated`() {
        val state = UiState()
        val items =
            listOf(
                AppItemUiModel("com.test.app", "Test App", "1.0.0"),
            )

        val newState = state.copy(items = items)

        assertEquals(1, newState.items.size)
        assertEquals("com.test.app", newState.items[0].packageName)
    }

    @Test
    fun `given UiState, when copy with multiple properties, then all are updated`() {
        val state = UiState()
        val items =
            listOf(
                AppItemUiModel("com.test.app", "Test App", "1.0.0"),
            )

        val newState =
            state.copy(
                selectedField = AppInfoField.MIN_SDK,
                showSystem = true,
                descending = true,
                query = "search",
                isLoading = true,
                items = items,
            )

        assertEquals(AppInfoField.MIN_SDK, newState.selectedField)
        assertTrue(newState.showSystem)
        assertTrue(newState.descending)
        assertEquals("search", newState.query)
        assertTrue(newState.isLoading)
        assertEquals(1, newState.items.size)
    }
}
