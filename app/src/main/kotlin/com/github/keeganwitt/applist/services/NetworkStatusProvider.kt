package com.github.keeganwitt.applist.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun interface NetworkStatusProvider {
    fun hasValidatedInternet(): Boolean
}

class AndroidNetworkStatusProvider(
    context: Context,
) : NetworkStatusProvider {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    override fun hasValidatedInternet(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
