package com.github.keeganwitt.applist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
        viewModel = AppListViewModel(repository, dispatcherProvider, summaryCalculator)
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
            coVerify { repository.loadApps(AppInfoField.VERSION, false, false, false) }
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
            coVerify { repository.loadApps(AppInfoField.TARGET_SDK, false, false, false) }
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
            coVerify { repository.loadApps(AppInfoField.VERSION, false, true, false) }
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
            coVerify { repository.loadApps(AppInfoField.VERSION, true, false, true) }
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

            coVerify(exactly = 2) { repository.loadApps(AppInfoField.VERSION, false, false, any()) }
            coVerify { repository.loadApps(AppInfoField.VERSION, false, false, true) }
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
                listOf(createTestApp("com.test.app1", "Test App 1"))
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
        }

    @Test
    fun `given apps loaded, when successful, then summary is calculated`() =
        runTest {
            val mockApps = listOf(createTestApp("com.test.app1", "Test App 1"))
            val mockSummary = SummaryItem(AppInfoField.ENABLED, mapOf("Enabled" to 1))
            coEvery { repository.loadApps(any(), any(), any(), any()) } returns flowOf(mockApps)
            coEvery { summaryCalculator.calculate(any(), any()) } returns mockSummary

            viewModel.init(AppInfoField.ENABLED)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(mockSummary, state.summary)
            coVerify { summaryCalculator.calculate(mockApps, AppInfoField.ENABLED) }
        }

    private fun createTestApp(
        packageName: String,
        name: String,
        versionName: String = "1.0.0",
        enabled: Boolean = true,
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
        )
}
