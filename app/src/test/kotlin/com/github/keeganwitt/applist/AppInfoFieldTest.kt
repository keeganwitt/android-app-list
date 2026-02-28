package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.text.DateFormat
import java.util.Date

class AppInfoFieldTest {
    @Test
    fun `given AppInfoField enum, when entries accessed, then all fields are present`() {
        val entries = AppInfoField.entries

        assertEquals(18, entries.size)
    }

    @Test
    fun `given VERSION field, when accessed, then has correct resource id`() {
        val field = AppInfoField.VERSION

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_version, field.titleResId)
    }

    @Test
    fun `given TARGET_SDK field, when accessed, then has correct resource id`() {
        val field = AppInfoField.TARGET_SDK

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_targetSdk, field.titleResId)
    }

    @Test
    fun `given MIN_SDK field, when accessed, then has correct resource id`() {
        val field = AppInfoField.MIN_SDK

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_minSdk, field.titleResId)
    }

    @Test
    fun `given TOTAL_SIZE field, when accessed, then has correct resource id`() {
        val field = AppInfoField.TOTAL_SIZE

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_totalSize, field.titleResId)
    }

    @Test
    fun `given APP_SIZE field, when accessed, then has correct resource id`() {
        val field = AppInfoField.APP_SIZE

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_appSize, field.titleResId)
    }

    @Test
    fun `given CACHE_SIZE field, when accessed, then has correct resource id`() {
        val field = AppInfoField.CACHE_SIZE

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_cacheSize, field.titleResId)
    }

    @Test
    fun `given DATA_SIZE field, when accessed, then has correct resource id`() {
        val field = AppInfoField.DATA_SIZE

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_dataSize, field.titleResId)
    }

    @Test
    fun `given ENABLED field, when accessed, then has correct resource id`() {
        val field = AppInfoField.ENABLED

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_enabled, field.titleResId)
    }

    @Test
    fun `given ARCHIVED field, when accessed, then has correct resource id`() {
        val field = AppInfoField.ARCHIVED

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_archived, field.titleResId)
    }

    @Test
    fun `given FIRST_INSTALLED field, when accessed, then has correct resource id`() {
        val field = AppInfoField.FIRST_INSTALLED

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_firstInstalled, field.titleResId)
    }

    @Test
    fun `given LAST_UPDATED field, when accessed, then has correct resource id`() {
        val field = AppInfoField.LAST_UPDATED

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_lastUpdated, field.titleResId)
    }

    @Test
    fun `given LAST_USED field, when accessed, then has correct resource id`() {
        val field = AppInfoField.LAST_USED

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_lastUsed, field.titleResId)
    }

    @Test
    fun `given PACKAGE_MANAGER field, when accessed, then has correct resource id`() {
        val field = AppInfoField.PACKAGE_MANAGER

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_packageManager, field.titleResId)
    }

    @Test
    fun `given GRANTED_PERMISSIONS field, when accessed, then has correct resource id`() {
        val field = AppInfoField.GRANTED_PERMISSIONS

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_grantedPermissions, field.titleResId)
    }

    @Test
    fun `given REQUESTED_PERMISSIONS field, when accessed, then has correct resource id`() {
        val field = AppInfoField.REQUESTED_PERMISSIONS

        assertNotNull(field.titleResId)
        assertEquals(R.string.appInfoField_requestedPermissions, field.titleResId)
    }

    @Test
    fun `given all fields, when accessed, then each has unique resource id`() {
        val resourceIds = AppInfoField.entries.map { it.titleResId }.toSet()

        assertEquals(AppInfoField.entries.size, resourceIds.size)
    }

    @Test
    fun `given App with data, when getValue called, then correct value returned`() {
        val storage =
            StorageUsage(
                apkBytes = 100,
                appBytes = 200,
                cacheBytes = 300,
                dataBytes = 400,
                externalCacheBytes = 500,
            )
        val app =
            App(
                packageName = "com.test",
                name = "Test App",
                versionName = "1.0",
                archived = true,
                minSdk = 24,
                targetSdk = 33,
                firstInstalled = 1000L,
                lastUpdated = 2000L,
                lastUsed = 3000L,
                sizes = storage,
                installerName = "Installer",
                existsInStore = true,
                grantedPermissionsCount = 5,
                requestedPermissionsCount = 10,
                enabled = true,
            )

        assertEquals(100L, AppInfoField.APK_SIZE.getValue(app))
        assertEquals(200L, AppInfoField.APP_SIZE.getValue(app))
        assertEquals(300L, AppInfoField.CACHE_SIZE.getValue(app))
        assertEquals(400L, AppInfoField.DATA_SIZE.getValue(app))
        assertEquals(500L, AppInfoField.EXTERNAL_CACHE_SIZE.getValue(app))
        assertEquals(storage.totalBytes, AppInfoField.TOTAL_SIZE.getValue(app))

        assertEquals(true, AppInfoField.ARCHIVED.getValue(app))
        assertEquals(true, AppInfoField.ENABLED.getValue(app))
        assertEquals(true, AppInfoField.EXISTS_IN_APP_STORE.getValue(app))

        assertEquals(1000L, AppInfoField.FIRST_INSTALLED.getValue(app))
        assertEquals(2000L, AppInfoField.LAST_UPDATED.getValue(app))
        assertEquals(3000L, AppInfoField.LAST_USED.getValue(app))

        assertEquals(24, AppInfoField.MIN_SDK.getValue(app))
        assertEquals(33, AppInfoField.TARGET_SDK.getValue(app))

        assertEquals("Installer", AppInfoField.PACKAGE_MANAGER.getValue(app))
        assertEquals("1.0", AppInfoField.VERSION.getValue(app))

        assertEquals(5, AppInfoField.GRANTED_PERMISSIONS.getValue(app))
        assertEquals(10, AppInfoField.REQUESTED_PERMISSIONS.getValue(app))
    }

    @Test
    fun `given App with nulls, when getValue called, then defaults returned`() {
        val app =
            App(
                packageName = "com.test",
                name = "Test App",
                versionName = null,
                archived = null,
                minSdk = null,
                targetSdk = null,
                firstInstalled = null,
                lastUpdated = null,
                lastUsed = null,
                sizes = StorageUsage(),
                installerName = null,
                existsInStore = null,
                grantedPermissionsCount = null,
                requestedPermissionsCount = null,
                enabled = false,
            )

        assertEquals(false, AppInfoField.ARCHIVED.getValue(app))
        assertEquals(false, AppInfoField.EXISTS_IN_APP_STORE.getValue(app))

        assertEquals(0, AppInfoField.MIN_SDK.getValue(app))
        assertEquals(0, AppInfoField.TARGET_SDK.getValue(app))

        assertEquals("", AppInfoField.PACKAGE_MANAGER.getValue(app))
        assertEquals("", AppInfoField.VERSION.getValue(app))

        assertEquals(0, AppInfoField.GRANTED_PERMISSIONS.getValue(app))
        assertEquals(0, AppInfoField.REQUESTED_PERMISSIONS.getValue(app))
    }

    @Test
    fun `given App with data, when getFormattedValue called, then correct string returned`() {
        val storage =
            StorageUsage(
                apkBytes = 100,
                appBytes = 200,
                cacheBytes = 300,
                dataBytes = 400,
                externalCacheBytes = 500,
            )
        val app =
            App(
                packageName = "com.test",
                name = "Test App",
                versionName = "1.0",
                archived = true,
                minSdk = 24,
                targetSdk = 33,
                firstInstalled = 1000L,
                lastUpdated = 2000L,
                lastUsed = 3000L,
                sizes = storage,
                installerName = "Installer",
                existsInStore = true,
                grantedPermissionsCount = 5,
                requestedPermissionsCount = 10,
                enabled = true,
            )

        assertEquals("100", AppInfoField.APK_SIZE.getFormattedValue(app))
        assertEquals("200", AppInfoField.APP_SIZE.getFormattedValue(app))
        assertEquals("300", AppInfoField.CACHE_SIZE.getFormattedValue(app))
        assertEquals("400", AppInfoField.DATA_SIZE.getFormattedValue(app))
        assertEquals("500", AppInfoField.EXTERNAL_CACHE_SIZE.getFormattedValue(app))
        assertEquals(storage.totalBytes.toString(), AppInfoField.TOTAL_SIZE.getFormattedValue(app))

        assertEquals("true", AppInfoField.ARCHIVED.getFormattedValue(app))
        assertEquals("true", AppInfoField.ENABLED.getFormattedValue(app))
        assertEquals("true", AppInfoField.EXISTS_IN_APP_STORE.getFormattedValue(app))
        assertEquals("24", AppInfoField.MIN_SDK.getFormattedValue(app))
        assertEquals("33", AppInfoField.TARGET_SDK.getFormattedValue(app))
        assertEquals("Installer", AppInfoField.PACKAGE_MANAGER.getFormattedValue(app))
        assertEquals("1.0", AppInfoField.VERSION.getFormattedValue(app))
        assertEquals("5", AppInfoField.GRANTED_PERMISSIONS.getFormattedValue(app))
        assertEquals("10", AppInfoField.REQUESTED_PERMISSIONS.getFormattedValue(app))

        val dateFormat = DateFormat.getDateTimeInstance()
        assertEquals(dateFormat.format(Date(1000L)), AppInfoField.FIRST_INSTALLED.getFormattedValue(app))
        assertEquals(dateFormat.format(Date(2000L)), AppInfoField.LAST_UPDATED.getFormattedValue(app))
        assertEquals(dateFormat.format(Date(3000L)), AppInfoField.LAST_USED.getFormattedValue(app))
    }

    @Test
    fun `given App with nulls, when getFormattedValue called, then empty strings returned for optional fields`() {
        val app =
            App(
                packageName = "com.test",
                name = "Test App",
                versionName = null,
                archived = null,
                minSdk = null,
                targetSdk = null,
                firstInstalled = null,
                lastUpdated = null,
                lastUsed = null,
                sizes = StorageUsage(),
                installerName = null,
                existsInStore = null,
                grantedPermissionsCount = null,
                requestedPermissionsCount = null,
                enabled = false,
            )

        assertEquals("0", AppInfoField.APK_SIZE.getFormattedValue(app))
        assertEquals("0", AppInfoField.APP_SIZE.getFormattedValue(app))
        assertEquals("0", AppInfoField.CACHE_SIZE.getFormattedValue(app))
        assertEquals("0", AppInfoField.DATA_SIZE.getFormattedValue(app))
        assertEquals("0", AppInfoField.EXTERNAL_CACHE_SIZE.getFormattedValue(app))
        assertEquals("0", AppInfoField.TOTAL_SIZE.getFormattedValue(app))

        assertEquals("false", AppInfoField.ARCHIVED.getFormattedValue(app)) // false.toString()
        assertEquals("false", AppInfoField.EXISTS_IN_APP_STORE.getFormattedValue(app)) // false.toString()

        assertEquals("0", AppInfoField.MIN_SDK.getFormattedValue(app))
        assertEquals("0", AppInfoField.TARGET_SDK.getFormattedValue(app))
        assertEquals("", AppInfoField.FIRST_INSTALLED.getFormattedValue(app))
        assertEquals("", AppInfoField.LAST_UPDATED.getFormattedValue(app))
        assertEquals("", AppInfoField.LAST_USED.getFormattedValue(app))
        assertEquals("", AppInfoField.PACKAGE_MANAGER.getFormattedValue(app))
        assertEquals("", AppInfoField.VERSION.getFormattedValue(app))

        assertEquals("0", AppInfoField.GRANTED_PERMISSIONS.getFormattedValue(app))
        assertEquals("0", AppInfoField.REQUESTED_PERMISSIONS.getFormattedValue(app))
    }

    @Test
    fun `given field requiring usage stats, when accessed, then requiresUsageStats is true`() {
        assertEquals(true, AppInfoField.APP_SIZE.requiresUsageStats)
        assertEquals(true, AppInfoField.CACHE_SIZE.requiresUsageStats)
        assertEquals(true, AppInfoField.DATA_SIZE.requiresUsageStats)
        assertEquals(true, AppInfoField.EXTERNAL_CACHE_SIZE.requiresUsageStats)
        assertEquals(true, AppInfoField.TOTAL_SIZE.requiresUsageStats)
        assertEquals(true, AppInfoField.LAST_USED.requiresUsageStats)
    }

    @Test
    fun `given field not requiring usage stats, when accessed, then requiresUsageStats is false`() {
        assertEquals(false, AppInfoField.APK_SIZE.requiresUsageStats)
        assertEquals(false, AppInfoField.ARCHIVED.requiresUsageStats)
        assertEquals(false, AppInfoField.ENABLED.requiresUsageStats)
        assertEquals(false, AppInfoField.EXISTS_IN_APP_STORE.requiresUsageStats)
        assertEquals(false, AppInfoField.FIRST_INSTALLED.requiresUsageStats)
        assertEquals(false, AppInfoField.GRANTED_PERMISSIONS.requiresUsageStats)
        assertEquals(false, AppInfoField.LAST_UPDATED.requiresUsageStats)
        assertEquals(false, AppInfoField.MIN_SDK.requiresUsageStats)
        assertEquals(false, AppInfoField.PACKAGE_MANAGER.requiresUsageStats)
        assertEquals(false, AppInfoField.REQUESTED_PERMISSIONS.requiresUsageStats)
        assertEquals(false, AppInfoField.TARGET_SDK.requiresUsageStats)
        assertEquals(false, AppInfoField.VERSION.requiresUsageStats)
    }

    @Test
    fun `given size fields, when accessed, then isSize is true`() {
        assertEquals(true, AppInfoField.APK_SIZE.isSize)
        assertEquals(true, AppInfoField.APP_SIZE.isSize)
        assertEquals(true, AppInfoField.CACHE_SIZE.isSize)
        assertEquals(true, AppInfoField.DATA_SIZE.isSize)
        assertEquals(true, AppInfoField.EXTERNAL_CACHE_SIZE.isSize)
        assertEquals(true, AppInfoField.TOTAL_SIZE.isSize)
    }

    @Test
    fun `given non-size fields, when accessed, then isSize is false`() {
        assertEquals(false, AppInfoField.VERSION.isSize)
        assertEquals(false, AppInfoField.TARGET_SDK.isSize)
        assertEquals(false, AppInfoField.MIN_SDK.isSize)
        assertEquals(false, AppInfoField.ENABLED.isSize)
        assertEquals(false, AppInfoField.ARCHIVED.isSize)
        assertEquals(false, AppInfoField.FIRST_INSTALLED.isSize)
        assertEquals(false, AppInfoField.LAST_UPDATED.isSize)
        assertEquals(false, AppInfoField.LAST_USED.isSize)
        assertEquals(false, AppInfoField.PACKAGE_MANAGER.isSize)
        assertEquals(false, AppInfoField.GRANTED_PERMISSIONS.isSize)
        assertEquals(false, AppInfoField.REQUESTED_PERMISSIONS.isSize)
        assertEquals(false, AppInfoField.EXISTS_IN_APP_STORE.isSize)
    }
}
