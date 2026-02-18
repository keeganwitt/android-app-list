package com.github.keeganwitt.applist.services

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi

interface PackageService {
    fun getInstalledApplications(flags: Long): List<ApplicationInfo>

    fun getLaunchIntentForPackage(packageName: String): Intent?

    fun getLaunchablePackages(): Set<String>

    fun loadLabel(applicationInfo: ApplicationInfo): String

    @Throws(PackageManager.NameNotFoundException::class)
    fun getPackageInfo(applicationInfo: ApplicationInfo): PackageInfo

    fun getInstallerPackageName(applicationInfo: ApplicationInfo): String?

    fun getApplicationIcon(packageName: String): Drawable?
}

class AndroidPackageService(
    context: Context,
) : PackageService {
    private val pm: PackageManager = context.packageManager

    override fun getInstalledApplications(flags: Long): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= 33) {
            Api33Impl.getInstalledApplications(pm, flags)
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(flags.toInt())
        }

    override fun getLaunchIntentForPackage(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)

    override fun getLaunchablePackages(): Set<String> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launcherApps =
            if (Build.VERSION.SDK_INT >= 33) {
                Api33Impl.queryIntentActivities(pm, launcherIntent, 0)
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }

        val infoIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_INFO)
        val infoApps =
            if (Build.VERSION.SDK_INT >= 33) {
                Api33Impl.queryIntentActivities(pm, infoIntent, 0)
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(infoIntent, 0)
            }

        return (launcherApps + infoApps).mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    override fun loadLabel(applicationInfo: ApplicationInfo): String = applicationInfo.loadLabel(pm).toString()

    override fun getPackageInfo(applicationInfo: ApplicationInfo): PackageInfo {
        var flags = PackageManager.GET_PERMISSIONS.toLong()

        if (Build.VERSION.SDK_INT >= 35) {
            flags = flags or PackageManager.MATCH_ARCHIVED_PACKAGES
        }
        // Also match disabled/uninstalled just in case
        flags = flags or (PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS).toLong()

        return if (Build.VERSION.SDK_INT >= 33) {
            Api33Impl.getPackageInfo(pm, applicationInfo.packageName, flags)
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
                Api35Impl.getApplicationIcon(pm, packageName)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

    @RequiresApi(33)
    private object Api33Impl {
        fun getInstalledApplications(
            pm: PackageManager,
            flags: Long,
        ): List<ApplicationInfo> = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags))

        fun getPackageInfo(
            pm: PackageManager,
            packageName: String,
            flags: Long,
        ): PackageInfo = pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))

        fun queryIntentActivities(
            pm: PackageManager,
            intent: Intent,
            flags: Long,
        ): List<android.content.pm.ResolveInfo> = pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags))
    }

    @RequiresApi(35)
    private object Api35Impl {
        fun getApplicationIcon(
            pm: PackageManager,
            packageName: String,
        ): Drawable? =
            try {
                val flags = PackageManager.MATCH_ARCHIVED_PACKAGES or PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()
                val ai = pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags))
                pm.getApplicationIcon(ai)
            } catch (_: Exception) {
                null
            }
    }
}
