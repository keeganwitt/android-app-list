package com.github.keeganwitt.applist

import android.content.Context
import com.github.keeganwitt.applist.db.AppDatabase
import com.github.keeganwitt.applist.services.AndroidPackageService
import com.github.keeganwitt.applist.services.AndroidStorageService
import com.github.keeganwitt.applist.services.AndroidUsageStatsService
import com.github.keeganwitt.applist.services.AppStoreService

internal object AppRepositoryFactory {
    fun create(
        context: Context,
        appStoreService: AppStoreService,
        crashReporter: CrashReporter? = null,
    ): AppRepository =
        AndroidAppRepository(
            AndroidPackageService(context),
            AndroidUsageStatsService(context, crashReporter = crashReporter),
            AndroidStorageService(context, crashReporter = crashReporter),
            appStoreService,
            AppDatabase.getDatabase(context).appDao(),
            crashReporter,
        )
}
