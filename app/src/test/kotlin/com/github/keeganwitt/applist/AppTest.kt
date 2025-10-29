package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTest {
    @Test
    fun `given App data class, when created with all fields, then all fields are set correctly`() {
        val storage =
            StorageUsage().apply {
                increaseAppBytes(1000L)
                increaseCacheBytes(500L)
            }

        val app =
            App(
                packageName = "com.test.app",
                name = "Test App",
                versionName = "1.0.0",
                archived = false,
                minSdk = 24,
                targetSdk = 33,
                firstInstalled = 1000000L,
                lastUpdated = 2000000L,
                lastUsed = 3000000L,
                sizes = storage,
                installerName = "Google Play",
                existsInStore = true,
                grantedPermissionsCount = 5,
                requestedPermissionsCount = 10,
                enabled = true,
            )

        assertEquals("com.test.app", app.packageName)
        assertEquals("Test App", app.name)
        assertEquals("1.0.0", app.versionName)
        assertFalse(app.archived!!)
        assertEquals(24, app.minSdk)
        assertEquals(33, app.targetSdk)
        assertEquals(1000000L, app.firstInstalled)
        assertEquals(2000000L, app.lastUpdated)
        assertEquals(3000000L, app.lastUsed)
        assertEquals(1500L, app.sizes.totalBytes)
        assertEquals("Google Play", app.installerName)
        assertTrue(app.existsInStore!!)
        assertEquals(5, app.grantedPermissionsCount)
        assertEquals(10, app.requestedPermissionsCount)
        assertTrue(app.enabled)
    }

    @Test
    fun `given App with null optional fields, when created, then nullable fields are null`() {
        val app =
            App(
                packageName = "com.test.app",
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

        assertEquals("com.test.app", app.packageName)
        assertEquals("Test App", app.name)
        assertEquals(null, app.versionName)
        assertEquals(null, app.archived)
        assertEquals(null, app.minSdk)
        assertEquals(null, app.targetSdk)
        assertEquals(null, app.firstInstalled)
        assertEquals(null, app.lastUpdated)
        assertEquals(null, app.lastUsed)
        assertNotNull(app.sizes)
        assertEquals(null, app.installerName)
        assertEquals(null, app.existsInStore)
        assertEquals(null, app.grantedPermissionsCount)
        assertEquals(null, app.requestedPermissionsCount)
        assertFalse(app.enabled)
    }

    @Test
    fun `given two App instances with same data, when compared, then they are equal`() {
        val storage1 = StorageUsage().apply { increaseAppBytes(1000L) }
        val storage2 = StorageUsage().apply { increaseAppBytes(1000L) }

        val app1 =
            App(
                packageName = "com.test.app",
                name = "Test App",
                versionName = "1.0.0",
                archived = false,
                minSdk = 24,
                targetSdk = 33,
                firstInstalled = 1000000L,
                lastUpdated = 2000000L,
                lastUsed = 3000000L,
                sizes = storage1,
                installerName = "Google Play",
                existsInStore = true,
                grantedPermissionsCount = 5,
                requestedPermissionsCount = 10,
                enabled = true,
            )

        val app2 = app1.copy()

        assertEquals(app1.packageName, app2.packageName)
        assertEquals(app1.name, app2.name)
        assertEquals(app1.versionName, app2.versionName)
    }

    @Test
    fun `given App, when copy with modified field, then only that field changes`() {
        val app =
            App(
                packageName = "com.test.app",
                name = "Test App",
                versionName = "1.0.0",
                archived = false,
                minSdk = 24,
                targetSdk = 33,
                firstInstalled = 1000000L,
                lastUpdated = 2000000L,
                lastUsed = 3000000L,
                sizes = StorageUsage(),
                installerName = "Google Play",
                existsInStore = true,
                grantedPermissionsCount = 5,
                requestedPermissionsCount = 10,
                enabled = true,
            )

        val modifiedApp = app.copy(versionName = "2.0.0")

        assertEquals("2.0.0", modifiedApp.versionName)
        assertEquals(app.packageName, modifiedApp.packageName)
        assertEquals(app.name, modifiedApp.name)
    }
}
