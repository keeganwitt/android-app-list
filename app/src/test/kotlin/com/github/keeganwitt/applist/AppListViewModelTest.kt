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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
        viewModel = AppListViewModel(repository, dispatcherProvider, summaryCalculator) { it.toString() }
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
            val viewModelWithSizeFormatter = AppListViewModel(repository, dispatcherProvider, summaryCalculator, mockSizeFormatter)
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
}
