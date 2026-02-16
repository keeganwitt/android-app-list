package com.github.keeganwitt.applist

data class StorageUsage(
    val apkBytes: Long = 0,
    val appBytes: Long = 0,
    val cacheBytes: Long = 0,
    val dataBytes: Long = 0,
    val externalCacheBytes: Long = 0,
) {
    val totalBytes: Long
        get() = appBytes + cacheBytes + dataBytes + externalCacheBytes
}
