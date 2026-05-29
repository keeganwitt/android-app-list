package com.github.keeganwitt.applist.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.keeganwitt.applist.AppInfoField
import com.github.keeganwitt.applist.StorageUsage
import com.github.keeganwitt.applist.services.AppStoreService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: AppDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.appDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetAll() =
        runTest {
            val entity =
                AppCacheEntity(
                    packageName = "com.test.app",
                    name = "Test App",
                    versionName = "1.0",
                    archived = false,
                    minSdk = 24,
                    targetSdk = 33,
                    firstInstalled = 1000L,
                    lastUpdated = 2000L,
                    lastUsed = 3000L,
                    sizes = StorageUsage(100, 200, 300, 400, 500),
                    installerName = AppStoreService.GOOGLE_PLAY,
                    existsInStore = true,
                    grantedPermissionsCount = 5,
                    requestedPermissionsCount = 10,
                    enabled = true,
                    isUserInstalled = true,
                    hasLaunchIntent = true,
                    isDetailed = true,
                    failedFields = setOf(AppInfoField.APP_SIZE),
                    lastCachedAt = System.currentTimeMillis(),
                )

            dao.insertApps(listOf(entity))
            val allApps = dao.getAllApps()
            assertEquals(1, allApps.size)
            assertEquals(entity, allApps[0])

            val flowApps = dao.getAllAppsFlow().first()
            assertEquals(1, flowApps.size)
            assertEquals(entity, flowApps[0])
        }

    @Test
    fun deleteByPackageNames() =
        runTest {
            val entity1 = createAppEntity("com.test.app1")
            val entity2 = createAppEntity("com.test.app2")
            dao.insertApps(listOf(entity1, entity2))

            dao.deleteApps(listOf("com.test.app1"))
            val allApps = dao.getAllApps()
            assertEquals(1, allApps.size)
            assertEquals("com.test.app2", allApps[0].packageName)
        }

    @Test
    fun clearAll() =
        runTest {
            dao.insertApps(listOf(createAppEntity("com.test.app1")))
            dao.clearAll()
            assertEquals(0, dao.getCount())
        }

    @Test
    fun convertersTest() {
        val converters = Converters()
        val fields = setOf(AppInfoField.APP_NAME, AppInfoField.VERSION)
        val string = converters.fromAppInfoFieldSet(fields)
        val back = converters.toAppInfoFieldSet(string)
        assertEquals(fields, back)

        assertEquals(emptySet<AppInfoField>(), converters.toAppInfoFieldSet(""))
        assertEquals(emptySet<AppInfoField>(), converters.toAppInfoFieldSet("INVALID_FIELD"))
    }

    @Test
    fun mappingTest() {
        val entity = createAppEntity("com.test.app")
        val domain = entity.toDomainModel()
        assertEquals(entity.packageName, domain.packageName)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.sizes, domain.sizes)
        assertEquals(entity.storeUrl, domain.storeUrl)

        val entityBack = domain.toCacheEntity(entity.lastCachedAt)
        assertEquals(entity, entityBack)
    }

    private fun createAppEntity(packageName: String): AppCacheEntity =
        AppCacheEntity(
            packageName = packageName,
            name = "Name",
            versionName = "1.0",
            archived = false,
            minSdk = 24,
            targetSdk = 33,
            firstInstalled = 1000L,
            lastUpdated = 2000L,
            lastUsed = 3000L,
            sizes = StorageUsage(100, 200, 300, 400, 500),
            installerName = AppStoreService.GOOGLE_PLAY,
            existsInStore = true,
            grantedPermissionsCount = 5,
            requestedPermissionsCount = 10,
            enabled = true,
            isUserInstalled = true,
            hasLaunchIntent = true,
            isDetailed = true,
            failedFields = emptySet(),
            lastCachedAt = 123456789L,
            storeUrl = "https://test.url",
        )
}
