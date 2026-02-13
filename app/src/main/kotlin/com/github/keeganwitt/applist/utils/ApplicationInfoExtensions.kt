package com.github.keeganwitt.applist.utils

import android.content.pm.ApplicationInfo
import android.os.Build

/**
 * Extension properties for [ApplicationInfo].
 */

/**
 * Checks if the application is archived.
 * On API 35+, it uses the system [ApplicationInfo.isArchived] property.
 * On older versions, it checks for the "com.android.vending.archive" metadata key.
 */
val ApplicationInfo.isArchivedApp: Boolean
    get() = if (Build.VERSION.SDK_INT >= 35 && isArchived) {
        true
    } else {
        metaData?.containsKey("com.android.vending.archive") == true
    }

/**
 * Checks if the application is user-installed (not a system app).
 */
val ApplicationInfo.isUserInstalled: Boolean
    get() = (flags and ApplicationInfo.FLAG_SYSTEM) == 0
