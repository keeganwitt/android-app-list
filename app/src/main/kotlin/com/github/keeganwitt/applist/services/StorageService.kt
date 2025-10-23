package com.github.keeganwitt.applist.services

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.storage.StorageManager
import com.github.keeganwitt.applist.StorageUsage
import java.util.UUID

interface StorageService {
    fun getStorageUsage(applicationInfo: ApplicationInfo): StorageUsage
}

class AndroidStorageService(private val context: Context) : StorageService {
    override fun getStorageUsage(applicationInfo: ApplicationInfo): StorageUsage {
        val storageUsage = StorageUsage()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return storageUsage
        }
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        storageManager.storageVolumes.forEach { storageVolume ->
            if (Environment.MEDIA_MOUNTED == storageVolume.state) {
                val uuidStr = storageVolume.uuid
                val uuid: UUID = try {
                    if (uuidStr == null) StorageManager.UUID_DEFAULT else UUID.fromString(uuidStr)
                } catch (_: IllegalArgumentException) {
                    return@forEach
                }
                try {
                    val storageStats = storageStatsManager.queryStatsForPackage(uuid, applicationInfo.packageName, Process.myUserHandle())
                    storageUsage.increaseAppBytes(storageStats.appBytes)
                    storageUsage.increaseCacheBytes(storageStats.cacheBytes)
                    storageUsage.increaseDataBytes(storageStats.dataBytes)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        storageUsage.increaseExternalCacheBytes(storageStats.externalCacheBytes)
                    }
                } catch (_: Exception) {
                    // ignore; return whatever we have
                }
            }
        }
        return storageUsage
    }
}
