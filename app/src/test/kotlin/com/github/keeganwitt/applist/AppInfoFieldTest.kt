package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

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
}
