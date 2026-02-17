package com.github.keeganwitt.applist.services

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.storage.StorageManager
import android.util.Log
import com.github.keeganwitt.applist.StorageUsage
import java.io.File
import java.util.UUID

interface StorageService {
    fun getStorageUsage(applicationInfo: ApplicationInfo): StorageUsage
}

class AndroidStorageService(
    private val context: Context,
    private val storageManager: StorageManager? = null,
    private val storageStatsManager: StorageStatsManager? = null,
) : StorageService {
    override fun getStorageUsage(applicationInfo: ApplicationInfo): StorageUsage {
        var apkBytes: Long = 0
        try {
            apkBytes = File(applicationInfo.publicSourceDir).length()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to get APK size for ${applicationInfo.packageName}", e)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return StorageUsage(apkBytes = apkBytes)
        }

        var appBytes: Long = 0
        var cacheBytes: Long = 0
        var dataBytes: Long = 0
        var externalCacheBytes: Long = 0

        val actualStorageManager = storageManager ?: (context.getSystemService(Context.STORAGE_SERVICE) as StorageManager)
        val actualStorageStatsManager =
            storageStatsManager ?: (context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager)
        actualStorageManager.storageVolumes.forEach { storageVolume ->
            if (Environment.MEDIA_MOUNTED == storageVolume.state) {
                val uuidStr = storageVolume.uuid
                val uuid: UUID =
                    try {
                        if (uuidStr == null) StorageManager.UUID_DEFAULT else UUID.fromString(uuidStr)
                    } catch (_: IllegalArgumentException) {
                        return@forEach
                    }
                try {
                    val storageStats =
                        actualStorageStatsManager.queryStatsForPackage(
                            uuid,
                            applicationInfo.packageName,
                            Process.myUserHandle(),
                        )
                    appBytes += storageStats.appBytes
                    cacheBytes += storageStats.cacheBytes
                    dataBytes += storageStats.dataBytes
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        externalCacheBytes += storageStats.externalCacheBytes
                    }
                } catch (e: SecurityException) {
                    val message = "Missing storage permission"
                    Log.w(TAG, message, e)
                } catch (e: Exception) {
                    val message = "Unable to process storage usage"
                    Log.w(TAG, message + " for ${applicationInfo.packageName} on $uuid", e)
                }
            }
        }
        return StorageUsage(
            apkBytes = apkBytes,
            appBytes = appBytes,
            cacheBytes = cacheBytes,
            dataBytes = dataBytes,
            externalCacheBytes = externalCacheBytes,
        )
    }

    companion object {
        private val TAG = StorageService::class.java.simpleName
    }
}
