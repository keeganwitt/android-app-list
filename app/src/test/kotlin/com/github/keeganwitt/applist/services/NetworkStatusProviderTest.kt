package com.github.keeganwitt.applist.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkStatusProviderTest {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var provider: AndroidNetworkStatusProvider

    @Before
    fun setup() {
        val context = mockk<Context>()
        connectivityManager = mockk()
        every { context.getSystemService(ConnectivityManager::class.java) } returns connectivityManager
        provider = AndroidNetworkStatusProvider(context)
    }

    @Test
    fun `given no active network, when checked, then internet is unavailable`() {
        every { connectivityManager.activeNetwork } returns null

        assertFalse(provider.hasValidatedInternet())
    }

    @Test
    fun `given unvalidated internet network, when checked, then internet is unavailable`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

        assertFalse(provider.hasValidatedInternet())
    }

    @Test
    fun `given validated internet network, when checked, then internet is available`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        assertTrue(provider.hasValidatedInternet())
    }
}
