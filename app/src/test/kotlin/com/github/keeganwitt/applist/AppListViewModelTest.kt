package com.github.keeganwitt.applist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.text.DateFormat
import java.util.Date

@ExperimentalCoroutinesApi
class AppListViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AppRepository
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var summaryCalculator: SummaryCalculator
    private lateinit var viewModel: AppListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        summaryCalculator = mockk(relaxed = true)
        dispatcherProvider =
            object : DispatcherProvider {
                override val io = testDispatcher
                override val main = testDispatcher
                override val default = testDispatcher
            }
        viewModel = AppListViewModel(repository, dispatcherProvider, summaryCalculator, { it.toString() }, "Unknown", "⚠ Failed to load")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given initial state, when init called, then loading state is true and apps are loaded`() =
        runTest {
            val mockApps =
                listOf(
                    createTestApp("com.test.app1", "Test App 1"),
                    createTestApp("com.test.app2", "Test App 2"),
                )
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AppInfoField.VERSION, state.selectedField)
            assertEquals(2, state.items.size)
            assertFalse(state.isLoading)
            coVerify {
                repository.loadApps(
                    AppInfoField.VERSION,
                    showSystemApps = false,
                    descending = false,
                    reload = false,
                )
            }
        }

    @Test
    fun `given apps loaded, when updateSelectedField called, then apps are reloaded with new field`() =
        runTest {
            val mockApps = listOf(createTestApp("com.test.app1", "Test App 1"))
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            viewModel.updateSelectedField(AppInfoField.TARGET_SDK)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AppInfoField.TARGET_SDK, state.selectedField)
            coVerify {
                repository.loadApps(
                    AppInfoField.TARGET_SDK,
                    showSystemApps = false,
                    descending = false,
                    reload = false,
                )
            }
        }

    @Test
    fun `given apps loaded, when toggleDescending called, then descending state is toggled`() =
        runTest {
            val mockApps = listOf(createTestApp("com.test.app1", "Test App 1"))
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.descending)

            viewModel.toggleDescending()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.descending)
            coVerify {
                repository.loadApps(
                    AppInfoField.VERSION,
                    showSystemApps = false,
                    descending = true,
                    reload = false,
                )
            }
        }

    @Test
    fun `given apps loaded, when setShowSystem called with true, then system apps are shown`() =
        runTest {
            val mockApps =
                listOf(
                    createTestApp("com.test.app1", "Test App 1"),
                    createTestApp("com.android.system", "System App"),
                )
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            viewModel.setShowSystem(true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.showSystem)
            coVerify {
                repository.loadApps(
                    AppInfoField.VERSION,
                    showSystemApps = true,
                    descending = false,
                    reload = true,
                )
            }
        }

    @Test
    fun `given apps loaded, when setQuery called with search text, then items are filtered`() =
        runTest {
            val mockApps =
                listOf(
                    createTestApp("com.myapp.one", "Test App 1"),
                    createTestApp("com.other.two", "Another App"),
                    createTestApp("com.example.three", "Example Test"),
                )
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.items.size)

            viewModel.setQuery("test")

            val state = viewModel.uiState.value
            assertEquals("test", state.query)
            assertEquals(2, state.items.size)
            assertTrue(state.items.any { it.appName == "Test App 1" })
            assertTrue(state.items.any { it.appName == "Example Test" })
        }

    @Test
    fun `given apps loaded, when setQuery called with empty string, then all items are shown`() =
        runTest {
            val mockApps =
                listOf(
                    createTestApp("com.myapp.one", "Test App 1"),
                    createTestApp("com.other.two", "Another App"),
                )
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.items.size)

            viewModel.setQuery("test")
            assertEquals(1, viewModel.uiState.value.items.size)

            viewModel.setQuery("")

            assertEquals(2, viewModel.uiState.value.items.size)
        }

    @Test
    fun `given apps loaded, when refresh called, then apps are reloaded with reload flag true`() =
        runTest {
            val mockApps = listOf(createTestApp("com.test.app1", "Test App 1"))
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            coVerify(exactly = 2) {
                repository.loadApps(
                    AppInfoField.VERSION,
                    showSystemApps = false,
                    descending = false,
                    reload = any(),
                )
            }
            coVerify {
                repository.loadApps(
                    AppInfoField.VERSION,
                    showSystemApps = false,
                    descending = false,
                    reload = true,
                )
            }
        }

    @Test
    fun `given no apps, when init called, then empty list is emitted`() =
        runTest {
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(emptyList())

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.items.isEmpty())
            assertFalse(state.isLoading)
        }

    @Test
    fun `given apps with different fields, when mapToItem called, then correct info text is generated`() =
        runTest {
            val app = createTestApp("com.test.app", "Test App", versionName = "1.2.3")
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(listOf(app))

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("1.2.3", state.items[0].infoText)
        }

    @Test
    fun `given size field, when mapToItem called, then sizeFormatter is used`() =
        runTest {
            val app = createTestApp("com.test.app", "Test App").copy(sizes = StorageUsage(apkBytes = 1024))
            val mockSizeFormatter: (Long) -> String = { "formatted $it" }
            val viewModelWithSizeFormatter =
                AppListViewModel(repository, dispatcherProvider, summaryCalculator, mockSizeFormatter, "Unknown", "⚠ Failed to load")
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(listOf(app))

            viewModelWithSizeFormatter.init(AppInfoField.APK_SIZE)
            advanceUntilIdle()

            val state = viewModelWithSizeFormatter.uiState.value
            assertEquals("formatted 1024", state.items[0].infoText)
        }

    @Test
    fun `given apps loaded, when query matches package name, then app is included in filtered results`() =
        runTest {
            val mockApps =
                listOf(
                    createTestApp("com.test.myapp", "My Application"),
                    createTestApp("com.example.other", "Other App"),
                )
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            viewModel.setQuery("myapp")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.items.size)
            assertEquals("com.test.myapp", state.items[0].packageName)
        }

    @Test
    fun `given loading apps, when successful, then isLoading updates to false`() =
        runTest {
            val mockApps =
                listOf(createTestApp("com.test.app1", "Test App 1", isDetailed = true))
            // Use MutableSharedFlow to control emission timing
            val flow = kotlinx.coroutines.flow.MutableSharedFlow<List<App>>()
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flow

            viewModel.init(AppInfoField.VERSION)
            runCurrent()

            // Should be loading initially
            assertTrue(viewModel.uiState.value.isLoading)

            // Emit apps
            flow.emit(mockApps)
            advanceUntilIdle()

            // Should be done loading
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1, state.items.size)
            assertFalse(state.items[0].isLoading)
        }

    @Test
    fun `given basic apps emitted, then items show loading state`() =
        runTest {
            val basicApps = listOf(createTestApp("com.test.app", "Test App", isDetailed = false))
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(basicApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.items.size)
            assertTrue(state.items[0].isLoading)
        }

    @Test
    fun `given detailed apps emitted, then items do not show loading state`() =
        runTest {
            val detailedApps = listOf(createTestApp("com.test.app", "Test App", isDetailed = true))
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(detailedApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.items.size)
            assertFalse(state.items[0].isLoading)
        }

    @Test
    fun `given detailed apps loaded, when successful, then summary is calculated`() =
        runTest {
            val mockApps = listOf(createTestApp("com.test.app1", "Test App 1", isDetailed = true))
            val mockSummary = SummaryItem(AppInfoField.ENABLED, mapOf("Enabled" to 1))
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)
            coEvery { summaryCalculator.calculate(any(), any()) } returns mockSummary

            viewModel.init(AppInfoField.ENABLED)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isFullyLoaded)
            assertEquals(mockSummary, state.summary)
            coVerify { summaryCalculator.calculate(mockApps, AppInfoField.ENABLED) }
        }

    @Test
    fun `given basic apps loaded, when successful, then summary is not calculated`() =
        runTest {
            val mockApps = listOf(createTestApp("com.test.app1", "Test App 1", isDetailed = false))
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.ENABLED)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isFullyLoaded)
            assertEquals(null, state.summary)
            coVerify(exactly = 0) { summaryCalculator.calculate(any(), any()) }
        }

    @Test
    fun `given apps loaded, when query changes, then summary is recalculated based on filtered apps`() =
        runTest {
            val app1 = createTestApp("com.test.app1", "Test App 1")
            val app2 = createTestApp("com.other.app2", "Other App 2")
            val mockApps = listOf(app1, app2)
            val mockSummaryFull = SummaryItem(AppInfoField.ENABLED, mapOf("Enabled" to 2))
            val mockSummaryFiltered = SummaryItem(AppInfoField.ENABLED, mapOf("Enabled" to 1))

            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)
            // Initial calculation
            coEvery { summaryCalculator.calculate(mockApps, any()) } returns mockSummaryFull
            // Filtered calculation
            coEvery { summaryCalculator.calculate(listOf(app1), any()) } returns mockSummaryFiltered

            viewModel.init(AppInfoField.ENABLED)
            advanceUntilIdle()

            viewModel.setQuery("Test App 1")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(mockSummaryFiltered, state.summary)
            coVerify { summaryCalculator.calculate(listOf(app1), AppInfoField.ENABLED) }
        }

    @Test
    fun `given apps loaded, when query matches info text, then app is included in filtered results`() =
        runTest {
            val mockApps =
                listOf(
                    createTestApp("com.test.app1", "App One", versionName = "1.2.3"),
                    createTestApp("com.test.app2", "App Two", versionName = "2.0.0"),
                )
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            viewModel.setQuery("1.2.3")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.items.size)
            assertEquals("com.test.app1", state.items[0].packageName)
            assertEquals("1.2.3", state.items[0].infoText)
        }

    @Test
    fun `given apps loaded, when query matches, then filteredApps is populated`() =
        runTest {
            val app1 = createTestApp("com.test.app1", "App One")
            val app2 = createTestApp("com.test.app2", "App Two")
            val mockApps = listOf(app1, app2)
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            viewModel.setQuery("One")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.filteredApps.size)
            assertEquals("com.test.app1", state.filteredApps[0].packageName)
        }

    @Test
    fun `given apps with all fields, when mapToItem called, then correct info text is generated for each field`() =
        runTest {
            val app =
                App(
                    packageName = "com.test",
                    name = "Test App",
                    versionName = "1.2.3",
                    archived = true,
                    minSdk = 21,
                    targetSdk = 33,
                    firstInstalled = 1000000L,
                    lastUpdated = 2000000L,
                    lastUsed = 3000000L,
                    sizes =
                        StorageUsage(
                            apkBytes = 100L,
                            appBytes = 200L,
                            cacheBytes = 300L,
                            dataBytes = 400L,
                            externalCacheBytes = 500L,
                        ),
                    installerName = "Test Installer",
                    existsInStore = true,
                    grantedPermissionsCount = 5,
                    requestedPermissionsCount = 10,
                    enabled = true,
                )

            val expectedMap =
                mapOf(
                    AppInfoField.APK_SIZE to "100",
                    AppInfoField.APP_SIZE to "200",
                    AppInfoField.CACHE_SIZE to "300",
                    AppInfoField.DATA_SIZE to "400",
                    AppInfoField.ENABLED to "true",
                    AppInfoField.ARCHIVED to "true",
                    AppInfoField.EXISTS_IN_APP_STORE to "true",
                    AppInfoField.EXTERNAL_CACHE_SIZE to "500",
                    AppInfoField.FIRST_INSTALLED to DateFormat.getDateTimeInstance().format(Date(1000000L)),
                    AppInfoField.LAST_UPDATED to DateFormat.getDateTimeInstance().format(Date(2000000L)),
                    AppInfoField.LAST_USED to DateFormat.getDateTimeInstance().format(Date(3000000L)),
                    AppInfoField.MIN_SDK to "21",
                    AppInfoField.PACKAGE_MANAGER to "Test Installer",
                    AppInfoField.GRANTED_PERMISSIONS to "5",
                    AppInfoField.REQUESTED_PERMISSIONS to "10",
                    AppInfoField.TARGET_SDK to "33",
                    AppInfoField.TOTAL_SIZE to (200L + 300L + 400L + 500L).toString(),
                    AppInfoField.VERSION to "1.2.3",
                )

            for ((field, expectedInfo) in expectedMap) {
                coEvery { repository.loadApps(field, any(), any(), any()) } returns flowOf(listOf(app))
                viewModel.init(field)
                advanceUntilIdle()
                assertEquals(
                    "Failed for field $field",
                    expectedInfo,
                    viewModel.uiState.value.items[0]
                        .infoText,
                )
            }
        }

    @Test
    fun `given apps with null fields, when mapToItem called, then correct default info text is generated`() =
        runTest {
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

            val expectedMap =
                mapOf(
                    AppInfoField.APK_SIZE to "Unknown",
                    AppInfoField.APP_SIZE to "Unknown",
                    AppInfoField.CACHE_SIZE to "Unknown",
                    AppInfoField.DATA_SIZE to "Unknown",
                    AppInfoField.ENABLED to "false",
                    AppInfoField.ARCHIVED to "Unknown",
                    AppInfoField.EXISTS_IN_APP_STORE to "Unknown",
                    AppInfoField.EXTERNAL_CACHE_SIZE to "Unknown",
                    AppInfoField.FIRST_INSTALLED to "Unknown",
                    AppInfoField.LAST_UPDATED to "Unknown",
                    AppInfoField.LAST_USED to "Unknown",
                    AppInfoField.MIN_SDK to "Unknown",
                    AppInfoField.PACKAGE_MANAGER to "Unknown",
                    AppInfoField.GRANTED_PERMISSIONS to "Unknown",
                    AppInfoField.REQUESTED_PERMISSIONS to "Unknown",
                    AppInfoField.TARGET_SDK to "Unknown",
                    AppInfoField.TOTAL_SIZE to "Unknown",
                    AppInfoField.VERSION to "Unknown",
                )

            for ((field, expectedInfo) in expectedMap) {
                coEvery { repository.loadApps(field, any(), any(), any()) } returns flowOf(listOf(app))
                viewModel.init(field)
                advanceUntilIdle()
                assertEquals(
                    "Failed for field $field",
                    expectedInfo,
                    viewModel.uiState.value.items[0]
                        .infoText,
                )
            }
        }

    private fun createTestApp(
        packageName: String,
        name: String,
        versionName: String = "1.0.0",
        enabled: Boolean = true,
        isDetailed: Boolean = true,
    ): App =
        App(
            packageName = packageName,
            name = name,
            versionName = versionName,
            archived = false,
            minSdk = 24,
            targetSdk = 33,
            firstInstalled = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis(),
            lastUsed = System.currentTimeMillis(),
            sizes = StorageUsage(),
            installerName = "Google Play",
            existsInStore = true,
            grantedPermissionsCount = 5,
            requestedPermissionsCount = 10,
            enabled = enabled,
            isDetailed = isDetailed,
        )

    @Test
    fun `given apps loaded, when field changes and search query set, then cache is rebuilt for new field`() =
        runTest {
            val app = createTestApp("com.test", "App One", versionName = "1.2.3")
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(listOf(app))

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            // Change field without query
            viewModel.updateSelectedField(AppInfoField.ENABLED)
            advanceUntilIdle()

            // Set query - should trigger cache rebuild because cachedMappedItemsField != state.selectedField
            viewModel.setQuery("test")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.items.size)
            assertEquals("true", state.items[0].infoText)
        }

    @Test
    fun `given apps not fully loaded, when applyFilterAndEmit called, then summary is null`() =
        runTest {
            val app = createTestApp("com.test", "App", isDetailed = false)
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(listOf(app))

            viewModel.init(AppInfoField.VERSION)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isFullyLoaded)
            assertNull(viewModel.uiState.value.summary)

            viewModel.setQuery("test")
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.summary)
        }

    @Test
    fun `given size field with zero value, when mapToItem called, then field getFormattedValue is used`() =
        runTest {
            val app = createTestApp("com.test", "App").copy(sizes = StorageUsage(apkBytes = 0))
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(listOf(app))

            viewModel.init(AppInfoField.APK_SIZE)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // If rawValue is 0L, it falls to else: field.getFormattedValue(app, unknownValue)
            // Long.getFormattedValue for size field returns "Unknown" if it's 0 or null (usually)
            assertEquals("Unknown", state.items[0].infoText)
        }
}
