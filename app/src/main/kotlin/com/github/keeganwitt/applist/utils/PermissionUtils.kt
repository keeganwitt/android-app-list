package com.github.keeganwitt.applist.utils

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Context.APP_OPS_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast

object PermissionUtils {
    private const val TAG = "PermissionUtils"

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode =
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission(activity: Activity) {
        val intent =
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }

        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        } else {
            val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            if (fallbackIntent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(fallbackIntent)
            } else {
                Log.w(TAG, "No Activity found to handle USAGE_ACCESS_SETTINGS intent.")
                Toast
                    .makeText(
                        activity,
                        "Please enable Usage Access permission manually in Settings",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }
}
