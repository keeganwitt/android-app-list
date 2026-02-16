package com.github.keeganwitt.applist

class StorageUsage {
    var apkBytes: Long = 0
    var appBytes: Long = 0
    var cacheBytes: Long = 0
    var dataBytes: Long = 0
    var externalCacheBytes: Long = 0

    val totalBytes: Long
        get() = appBytes + cacheBytes + dataBytes + externalCacheBytes

    fun increaseAppBytes(value: Long) {
        appBytes += value
    }

    fun increaseCacheBytes(value: Long) {
        cacheBytes += value
    }

    fun increaseDataBytes(value: Long) {
        dataBytes += value
    }

    fun increaseExternalCacheBytes(value: Long) {
        externalCacheBytes += value
    }
}
