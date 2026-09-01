package com.github.keeganwitt.applist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAppRepository
    private lateinit var viewModel: ExportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAppRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load refreshes and selects visible user apps in name order`() =
        runTest(dispatcher) {
            repository.apps =
                listOf(
                    app("com.example.zulu", "Zulu"),
                    app("com.android.system", "System", isUserInstalled = false),
                    app("com.example.hidden", "Hidden", hasLaunchIntent = false),
                    app("com.example.archived", "Archived", archived = true, hasLaunchIntent = false),
                    app("com.example.alpha", "Alpha"),
                )

            viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf("Alpha", "Zulu"), state.selectedApps.map { it.appName })
            assertEquals(ExportScope.USER_APPS, state.scope)
            assertEquals(ExportFormat.XML, state.format)
            assertFalse(state.includeArchived)
            assertFalse(state.isLoading)
            assertEquals(listOf(true), repository.refreshForces)
        }

    @Test
    fun `empty cache produces a loaded empty state with export disabled`() =
        runTest(dispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.loadFailed)
            assertEquals(0, viewModel.uiState.value.selectedCount)
            assertFalse(viewModel.uiState.value.canExport)
        }

    @Test
    fun `system and all scopes apply their documented visibility rules`() =
        runTest(dispatcher) {
            repository.apps =
                listOf(
                    app("user.visible", "User Visible"),
                    app("user.hidden", "User Hidden", hasLaunchIntent = false),
                    app("system.visible", "System Visible", isUserInstalled = false),
                    app("system.hidden", "System Hidden", isUserInstalled = false, hasLaunchIntent = false),
                    app("system.archived", "System Archived", isUserInstalled = false, archived = true, hasLaunchIntent = false),
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setScope(ExportScope.SYSTEM_APPS)
            assertEquals(listOf("system.visible"), viewModel.appsForExport().map { it.packageName })

            viewModel.setScope(ExportScope.ALL_APPS)
            assertEquals(
                listOf("system.hidden", "system.visible", "user.hidden", "user.visible"),
                viewModel.appsForExport().map { it.packageName }.sorted(),
            )

            viewModel.setIncludeArchived(true)
            assertEquals(5, viewModel.appsForExport().size)
        }

    @Test
    fun `custom scope keeps preset selection while search only filters visible choices`() =
        runTest(dispatcher) {
            repository.apps =
                listOf(
                    app("user.alpha", "Alpha"),
                    app("user.beta", "Beta"),
                    app("system.gamma", "Gamma System", isUserInstalled = false),
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setScope(ExportScope.CUSTOM)
            viewModel.setQuery("system.gamma")

            assertEquals(
                listOf("system.gamma"),
                viewModel.uiState.value.visibleApps
                    .map { it.packageName },
            )
            assertEquals(2, viewModel.uiState.value.selectedCount)

            viewModel.selectAllVisible()
            assertEquals(3, viewModel.uiState.value.selectedCount)

            viewModel.clearSelection()
            assertFalse(viewModel.uiState.value.canExport)

            viewModel.toggleSelection("user.alpha")
            assertEquals(listOf("user.alpha"), viewModel.appsForExport().map { it.packageName })
        }

    @Test
    fun `leaving custom scope clears search and replaces custom selection`() =
        runTest(dispatcher) {
            repository.apps =
                listOf(
                    app("user.app", "User"),
                    app("system.app", "System", isUserInstalled = false),
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setScope(ExportScope.CUSTOM)
            viewModel.clearSelection()
            viewModel.toggleSelection("system.app")
            viewModel.setQuery("system")
            viewModel.setScope(ExportScope.USER_APPS)

            assertEquals("", viewModel.uiState.value.query)
            assertEquals(listOf("user.app"), viewModel.appsForExport().map { it.packageName })
        }

    @Test
    fun `archived toggle adds preset apps but never implicitly adds custom apps`() =
        runTest(dispatcher) {
            repository.apps =
                listOf(
                    app("user.active", "Active"),
                    app("user.archived", "Archived", archived = true, hasLaunchIntent = false),
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setIncludeArchived(true)
            assertEquals(2, viewModel.uiState.value.selectedCount)

            viewModel.setScope(ExportScope.CUSTOM)
            viewModel.clearSelection()
            viewModel.setIncludeArchived(false)
            viewModel.setIncludeArchived(true)

            assertTrue(
                viewModel.uiState.value.visibleApps
                    .any { it.packageName == "user.archived" },
            )
            assertEquals(0, viewModel.uiState.value.selectedCount)
        }

    @Test
    fun `disabling archived apps removes archived custom selections`() =
        runTest(dispatcher) {
            repository.apps =
                listOf(
                    app("user.active", "Active"),
                    app("user.archived", "Archived", archived = true, hasLaunchIntent = false),
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setIncludeArchived(true)
            viewModel.setScope(ExportScope.CUSTOM)
            viewModel.setIncludeArchived(false)

            assertEquals(listOf("user.active"), viewModel.appsForExport().map { it.packageName })
            assertTrue(
                viewModel.uiState.value.visibleApps
                    .none { it.packageName == "user.archived" },
            )
        }

    @Test
    fun `failed refresh shows retryable error state`() =
        runTest(dispatcher) {
            repository.refreshError = IllegalStateException("load failed")
            viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.loadFailed)
            assertFalse(viewModel.uiState.value.isLoading)

            repository.refreshError = null
            repository.apps = listOf(app("user.app", "User"))
            viewModel.refresh()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.loadFailed)
            assertEquals(1, viewModel.uiState.value.selectedCount)
        }

    @Test
    fun `export request captures format and order while outcomes re-enable export`() =
        runTest(dispatcher) {
            repository.apps = listOf(app("zulu", "Zulu"), app("alpha", "Alpha"))
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setFormat(ExportFormat.CSV)
            viewModel.setIncludeArchived(false)
            val request = viewModel.beginExport()

            assertEquals(ExportFormat.CSV, viewModel.uiState.value.format)
            assertEquals(ExportFormat.CSV, request?.format)
            assertEquals(listOf("alpha", "zulu"), request?.apps?.map { it.packageName })
            assertEquals(request, viewModel.pendingExportRequest())
            assertTrue(viewModel.uiState.value.isExporting)
            assertFalse(viewModel.uiState.value.canExport)

            viewModel.setScope(ExportScope.ALL_APPS)
            viewModel.setFormat(ExportFormat.HTML)
            viewModel.setIncludeArchived(true)
            viewModel.setQuery("ignored")
            viewModel.selectAllVisible()
            viewModel.clearSelection()
            viewModel.toggleSelection("alpha")
            assertEquals(null, viewModel.beginExport())
            assertEquals(ExportScope.USER_APPS, viewModel.uiState.value.scope)
            assertEquals(ExportFormat.CSV, viewModel.uiState.value.format)
            assertEquals(2, viewModel.uiState.value.selectedCount)

            viewModel.handleExportOutcome(ExportOutcome.CANCELED)
            assertEquals(null, viewModel.pendingExportRequest())
            viewModel.toggleSelection("alpha")
            viewModel.toggleSelection("missing")
            assertTrue(viewModel.uiState.value.canExport)
        }

    @Test
    fun `canExport rejects every unavailable state`() {
        val selected =
            listOf(
                ExportAppItemUiModel("app", "App", true, false, true),
            )

        assertFalse(ExportUiState(selectedApps = selected).canExport)
        assertFalse(
            ExportUiState(
                selectedApps = selected,
                isLoading = false,
                loadFailed = true,
            ).canExport,
        )
        assertFalse(
            ExportUiState(
                selectedApps = selected,
                isLoading = false,
                isExporting = true,
            ).canExport,
        )
        assertTrue(
            ExportUiState(
                selectedApps = selected,
                isLoading = false,
            ).canExport,
        )
    }

    private fun createViewModel(): ExportViewModel =
        ExportViewModel(
            repository = repository,
            dispatchers =
                object : DispatcherProvider {
                    override val io = dispatcher
                    override val main = dispatcher
                    override val default = dispatcher
                },
            appComparator = compareBy<App> { it.name }.thenBy { it.packageName },
        )

    private fun app(
        packageName: String,
        name: String,
        isUserInstalled: Boolean = true,
        hasLaunchIntent: Boolean = true,
        archived: Boolean = false,
    ): App =
        App(
            packageName = packageName,
            name = name,
            versionName = "1.0",
            archived = archived,
            minSdk = 24,
            targetSdk = 37,
            firstInstalled = 1L,
            lastUpdated = 1L,
            lastUsed = 1L,
            sizes = StorageUsage(),
            installerName = null,
            existsInStore = null,
            grantedPermissionsCount = 0,
            requestedPermissionsCount = 0,
            enabled = true,
            isUserInstalled = isUserInstalled,
            hasLaunchIntent = hasLaunchIntent,
            isDetailed = true,
        )

    private class FakeAppRepository : AppRepository {
        var apps: List<App> = emptyList()
        var refreshError: Exception? = null
        val refreshForces = mutableListOf<Boolean>()

        override fun loadApps(
            field: AppInfoField,
            systemAppsOnly: Boolean,
            showArchivedApps: Boolean,
            descending: Boolean,
            reload: Boolean,
        ): Flow<List<App>> = flowOf(apps)

        override fun getSyncState(): Flow<SyncState> = flowOf(SyncState.Idle)

        override suspend fun refreshCache(force: Boolean) {
            refreshForces += force
            refreshError?.let { throw it }
        }

        override suspend fun getCachedApps(): List<App> = apps
    }
}
