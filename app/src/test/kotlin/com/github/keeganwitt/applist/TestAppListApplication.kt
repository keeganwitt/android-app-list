package com.github.keeganwitt.applist

class TestAppListApplication : AppListApplication() {
    override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {}

    override fun deleteUnsentReports() {}
}
