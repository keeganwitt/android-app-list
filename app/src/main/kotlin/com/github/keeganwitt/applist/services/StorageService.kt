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
    private val crashReporter: com.github.keeganwitt.applist.CrashReporter? = null,
) : StorageService {
    override fun getStorageUsage(applicationInfo: ApplicationInfo): StorageUsage {
        var apkBytes: Long? = null
        try {
            val file = File(applicationInfo.publicSourceDir)
            if (file.exists()) {
                apkBytes = file.length()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to get APK size for ${applicationInfo.packageName}", e)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return StorageUsage(apkBytes = apkBytes)
        }

        var appBytes: Long? = null
        var cacheBytes: Long? = null
        var dataBytes: Long? = null
        var externalCacheBytes: Long? = null

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
                    appBytes = (appBytes ?: 0L) + storageStats.appBytes
                    cacheBytes = (cacheBytes ?: 0L) + storageStats.cacheBytes
                    dataBytes = (dataBytes ?: 0L) + storageStats.dataBytes
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        externalCacheBytes = (externalCacheBytes ?: 0L) + storageStats.externalCacheBytes
                    }
                } catch (e: SecurityException) {
                    val message = "Missing storage permission for ${applicationInfo.packageName}"
                    Log.w(TAG, message, e)
                } catch (e: Exception) {
                    val message = "Unable to process storage usage for ${applicationInfo.packageName} on $uuid"
                    Log.w(TAG, message, e)
                    crashReporter?.recordException(e, message)
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
