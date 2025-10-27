package com.github.keeganwitt.applist.services

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

interface PackageService {
    fun getInstalledApplications(flags: Int): List<ApplicationInfo>
    fun getLaunchIntentForPackage(packageName: String): android.content.Intent?
    fun loadLabel(applicationInfo: ApplicationInfo): String
    fun loadIcon(applicationInfo: ApplicationInfo): Drawable
    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(applicationInfo: ApplicationInfo): PackageInfo
    fun getInstallerPackageName(applicationInfo: ApplicationInfo): String?
    fun getApplicationIcon(packageName: String): Drawable?
}

class AndroidPackageService(private val context: Context) : PackageService {
    private val pm: PackageManager = context.packageManager

    override fun getInstalledApplications(flags: Int): List<ApplicationInfo> =
        pm.getInstalledApplications(flags)

    override fun getLaunchIntentForPackage(packageName: String): android.content.Intent? =
        pm.getLaunchIntentForPackage(packageName)

    override fun loadLabel(applicationInfo: ApplicationInfo): String =
        applicationInfo.loadLabel(pm).toString()

    override fun loadIcon(applicationInfo: ApplicationInfo): Drawable =
        applicationInfo.loadIcon(pm)

    override fun getPackageInfo(applicationInfo: ApplicationInfo): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES
        }
        return pm.getPackageInfo(applicationInfo.packageName, flags)
    }

    override fun getInstallerPackageName(applicationInfo: ApplicationInfo): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                pm.getInstallSourceInfo(applicationInfo.packageName).installingPackageName
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(applicationInfo.packageName)
        }
    }

    override fun getApplicationIcon(packageName: String): Drawable? = try {
        pm.getApplicationIcon(packageName)
    } catch (_: Exception) { null }
}
