package com.github.keeganwitt.applist.services

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

interface PackageService {
    fun getInstalledApplications(flags: Int): List<ApplicationInfo>

    fun getInstalledApplications(flags: Long): List<ApplicationInfo>

    fun getLaunchIntentForPackage(packageName: String): android.content.Intent?

    fun loadLabel(applicationInfo: ApplicationInfo): String

    fun loadIcon(applicationInfo: ApplicationInfo): Drawable

    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(applicationInfo: ApplicationInfo): PackageInfo

    fun getInstallerPackageName(applicationInfo: ApplicationInfo): String?

    fun getApplicationIcon(packageName: String): Drawable?
}

class AndroidPackageService(
    context: Context,
) : PackageService {
    private val pm: PackageManager = context.packageManager

    override fun getInstalledApplications(flags: Int): List<ApplicationInfo> = pm.getInstalledPackages(flags).map { it.applicationInfo!! }

    override fun getInstalledApplications(flags: Long): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags)).map { it.applicationInfo!! }
        } else {
            pm.getInstalledPackages(flags.toInt()).map { it.applicationInfo!! }
        }

    override fun getLaunchIntentForPackage(packageName: String): android.content.Intent? = pm.getLaunchIntentForPackage(packageName)

    override fun loadLabel(applicationInfo: ApplicationInfo): String = applicationInfo.loadLabel(pm).toString()

    override fun loadIcon(applicationInfo: ApplicationInfo): Drawable = pm.getApplicationIcon(applicationInfo)

    override fun getPackageInfo(applicationInfo: ApplicationInfo): PackageInfo {
        var flags = PackageManager.GET_PERMISSIONS.toLong()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            flags = flags or PackageManager.GET_SIGNING_CERTIFICATES.toLong()
        } else {
            @Suppress("DEPRECATION")
            flags = flags or PackageManager.GET_SIGNATURES.toLong()
        }

        if (Build.VERSION.SDK_INT >= 35) {
            flags = flags or PackageManager.MATCH_ARCHIVED_PACKAGES.toLong()
        }
        // Also match disabled/uninstalled just in case
        flags = flags or (PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS).toLong()

        return if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(applicationInfo.packageName, PackageManager.PackageInfoFlags.of(flags))
        } else {
            pm.getPackageInfo(applicationInfo.packageName, flags.toInt())
        }
    }

    override fun getInstallerPackageName(applicationInfo: ApplicationInfo): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                pm.getInstallSourceInfo(applicationInfo.packageName).installingPackageName
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(applicationInfo.packageName)
        }

    override fun getApplicationIcon(packageName: String): Drawable? =
        try {
            pm.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            if (Build.VERSION.SDK_INT >= 35) {
                try {
                    val flags = PackageManager.MATCH_ARCHIVED_PACKAGES.toLong() or PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()
                    val ai = pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags))
                    pm.getApplicationIcon(ai)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
}
