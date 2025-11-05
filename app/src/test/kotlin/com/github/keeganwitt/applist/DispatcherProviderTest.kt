package com.github.keeganwitt.applist

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DispatcherProviderTest {
    @Test
    fun `given DefaultDispatcherProvider, when accessing io dispatcher, then returns Dispatchers IO`() {
        val provider = DefaultDispatcherProvider()

        assertEquals(Dispatchers.IO, provider.io)
    }

    @Test
    fun `given DefaultDispatcherProvider, when accessing main dispatcher, then returns Dispatchers Main`() {
        val provider = DefaultDispatcherProvider()

        assertEquals(Dispatchers.Main, provider.main)
    }

    @Test
    fun `given DefaultDispatcherProvider, when accessing default dispatcher, then returns Dispatchers Default`() {
        val provider = DefaultDispatcherProvider()

        assertEquals(Dispatchers.Default, provider.default)
    }
}
