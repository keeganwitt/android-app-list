package com.github.keeganwitt.applist

data class StorageUsage(
    val apkBytes: Long? = null,
    val appBytes: Long? = null,
    val cacheBytes: Long? = null,
    val dataBytes: Long? = null,
    val externalCacheBytes: Long? = null,
) {
    val totalBytes: Long?
        get() =
            if (appBytes == null && cacheBytes == null && dataBytes == null && externalCacheBytes == null) {
                null
            } else {
                (appBytes ?: 0L) + (cacheBytes ?: 0L) + (dataBytes ?: 0L) + (externalCacheBytes ?: 0L)
            }
}
