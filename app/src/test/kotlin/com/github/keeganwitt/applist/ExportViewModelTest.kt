package com.github.keeganwitt.applist

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.CompletableDeferred
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
    fun `initial load shows and selects non-archived user apps in name order`() =
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
            assertEquals(ExportAppTypeFilter.USER, state.appTypeFilter)
            assertEquals(listOf("Alpha", "Hidden", "Zulu"), state.visibleApps.map { it.appName })
            assertEquals(listOf("Alpha", "Hidden", "Zulu"), state.selectedApps.map { it.appName })
            assertEquals(ExportFormat.XML, state.format)
            assertFalse(state.showArchived)
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
    fun `changing type filter preserves selection and includes non-launchable packages`() =
        runTest(dispatcher) {
            repository.apps =
                listOf(
                    app("user.visible", "Alpha"),
                    app("user.hidden", "Beta", hasLaunchIntent = false),
                    app("system.visible", "Gamma", isUserInstalled = false),
                    app("system.hidden", "Delta", isUserInstalled = false, hasLaunchIntent = false),
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.selectedCount)
            viewModel.setAppTypeFilter(ExportAppTypeFilter.SYSTEM)

            assertEquals(2, viewModel.uiState.value.selectedCount)
            assertEquals(
                listOf("system.hidden", "system.visible"),
                viewModel.uiState.value.visibleApps
                    .map { it.packageName },
            )

            viewModel.selectAllVisible()
            assertEquals(
                listOf("system.hidden", "system.visible", "user.hidden", "user.visible"),
                viewModel.appsForExport().map { it.packageName }.sorted(),
            )

            viewModel.setAppTypeFilter(ExportAppTypeFilter.ALL)
            assertEquals(4, viewModel.uiState.value.selectedCount)
            assertEquals(4, viewModel.uiState.value.visibleApps.size)
        }

    @Test
    fun `changing type filter retains query and selection`() =
        runTest(dispatcher) {
            repository.apps =
                listOf(
                    app("user.app", "User"),
                    app("system.app", "System", isUserInstalled = false),
                )
            viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.setQuery("system")

            viewModel.setAppTypeFilter(ExportAppTypeFilter.SYSTEM)

            assertEquals("system", viewModel.uiState.value.query)
            assertEquals(
                listOf("system.app"),
                viewModel.uiState.value.visibleApps
                    .map { it.packageName },
            )
            assertEquals(listOf("user.app"), viewModel.appsForExport().map { it.packageName })
        }

    @Test
    fun `search hides selections without clearing them and select all adds visible results`() =
        runTest(dispatcher) {
            repository.apps = listOf(app("user.alpha", "Alpha"), app("user.beta", "Beta"))
            viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.clearSelection()
            viewModel.setQuery("Alpha")
            viewModel.selectAllVisible()

            viewModel.setQuery("Beta")

            assertEquals(listOf("user.alpha"), viewModel.appsForExport().map { it.packageName })
            assertEquals(1, viewModel.uiState.value.hiddenSelectedCount)
            viewModel.selectAllVisible()
            assertEquals(listOf("user.alpha", "user.beta"), viewModel.appsForExport().map { it.packageName })
        }

    @Test
    fun `archived filter exposes without selecting and hiding preserves archived selections`() =
        runTest(dispatcher) {
            repository.apps =
                listOf(
                    app("user.active", "Active"),
                    app("user.archived", "Archived", archived = true, hasLaunchIntent = false),
                )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setShowArchived(true)

            assertEquals(2, viewModel.uiState.value.visibleApps.size)
            assertEquals(listOf("user.active"), viewModel.appsForExport().map { it.packageName })
            viewModel.selectAllVisible()
            assertEquals(2, viewModel.uiState.value.selectedCount)

            viewModel.setShowArchived(false)

            assertEquals(listOf("user.active", "user.archived"), viewModel.appsForExport().map { it.packageName })
            assertEquals(
                listOf("user.active"),
                viewModel.uiState.value.visibleApps
                    .map { it.packageName },
            )
            assertEquals(1, viewModel.uiState.value.hiddenSelectedCount)
        }

    @Test
    fun `review shows every selection and returning restores browse filters`() =
        runTest(dispatcher) {
            repository.apps =
                listOf(
                    app("user.alpha", "Alpha"),
                    app("user.archived", "Archived", archived = true),
                    app("system.beta", "Beta", isUserInstalled = false),
                )
            viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.setShowArchived(true)
            viewModel.selectAllVisible()
            viewModel.setAppTypeFilter(ExportAppTypeFilter.SYSTEM)
            viewModel.selectAllVisible()
            viewModel.setQuery("no match")

            viewModel.showSelectionReview()

            assertTrue(viewModel.uiState.value.isReviewingSelection)
            assertEquals(
                listOf("user.alpha", "user.archived", "system.beta"),
                viewModel.uiState.value.visibleApps
                    .map { it.packageName },
            )
            assertEquals(0, viewModel.uiState.value.hiddenSelectedCount)

            viewModel.toggleSelection("user.archived")
            assertEquals(listOf("user.alpha", "system.beta"), viewModel.appsForExport().map { it.packageName })

            viewModel.showBrowse()

            assertFalse(viewModel.uiState.value.isReviewingSelection)
            assertEquals(ExportAppTypeFilter.SYSTEM, viewModel.uiState.value.appTypeFilter)
            assertEquals("no match", viewModel.uiState.value.query)
            assertTrue(
                viewModel.uiState.value.visibleApps
                    .isEmpty(),
            )
        }

    @Test
    fun `clear selection empties review`() =
        runTest(dispatcher) {
            repository.apps = listOf(app("user.app", "User"))
            viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.showSelectionReview()

            viewModel.clearSelection()

            assertTrue(viewModel.uiState.value.isReviewingSelection)
            assertTrue(
                viewModel.uiState.value.visibleApps
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.canExport)
        }

    @Test
    fun `row toggles and clear selection update export availability`() =
        runTest(dispatcher) {
            repository.apps = listOf(app("user.app", "User"))
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.toggleSelection("user.app")
            assertFalse(viewModel.uiState.value.canExport)
            viewModel.toggleSelection("user.app")
            viewModel.toggleSelection("missing")
            assertTrue(viewModel.uiState.value.canExport)
            viewModel.clearSelection()
            assertFalse(viewModel.uiState.value.canExport)
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
    fun `export request captures format and order while controls remain frozen`() =
        runTest(dispatcher) {
            repository.apps = listOf(app("zulu", "Zulu"), app("alpha", "Alpha"))
            viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.setFormat(ExportFormat.CSV)
            val beforeExport = viewModel.uiState.value

            val request = viewModel.beginExport()
            viewModel.setAppTypeFilter(ExportAppTypeFilter.SYSTEM)
            viewModel.setFormat(ExportFormat.HTML)
            viewModel.setShowArchived(true)
            viewModel.setQuery("ignored")
            viewModel.selectAllVisible()
            viewModel.clearSelection()
            viewModel.toggleSelection("alpha")

            assertEquals(ExportFormat.CSV, request?.format)
            assertEquals(listOf("alpha", "zulu"), request?.apps?.map { it.packageName })
            assertEquals(request, viewModel.pendingExportRequest())
            assertTrue(viewModel.uiState.value.isExporting)
            assertFalse(viewModel.uiState.value.canExport)

            viewModel.cancelPendingExport()
            assertEquals(null, viewModel.pendingExportRequest())
            assertEquals(beforeExport, viewModel.uiState.value)
        }

    @Test
    fun `retained export write completes once across owner recreation`() =
        runTest(dispatcher) {
            repository.apps = listOf(app("user.app", "User"))
            val writer = ControllableExportFileWriter()
            viewModel = createViewModel(writer)
            advanceUntilIdle()
            viewModel.beginExport()
            val destination = io.mockk.mockk<Uri>()

            viewModel.writePendingExport(destination)
            testScheduler.runCurrent()
            viewModel.writePendingExport(destination)
            assertEquals(1, writer.writeCount)
            assertTrue(viewModel.uiState.value.isExporting)

            writer.result.complete(ExportCompletion(ExportOutcome.SUCCESS))
            advanceUntilIdle()

            assertEquals(1, writer.writeCount)
            assertFalse(viewModel.uiState.value.isExporting)
            val completion = viewModel.exportCompletion.value!!
            assertTrue(viewModel.consumeExportCompletion(completion))
            assertFalse(viewModel.consumeExportCompletion(completion))
        }

    @Test
    fun `failed retained write unfreezes choices and exposes one failure`() =
        runTest(dispatcher) {
            repository.apps = listOf(app("user.app", "User"))
            val writer = ControllableExportFileWriter()
            viewModel = createViewModel(writer)
            advanceUntilIdle()
            val beforeExport = viewModel.uiState.value
            viewModel.beginExport()
            viewModel.writePendingExport(io.mockk.mockk())
            testScheduler.runCurrent()

            writer.result.complete(ExportCompletion(ExportOutcome.FAILURE, "Disk full"))
            advanceUntilIdle()

            assertEquals(beforeExport, viewModel.uiState.value)
            assertEquals(
                ExportCompletion(ExportOutcome.FAILURE, "Disk full"),
                viewModel.exportCompletion.value,
            )
        }

    @Test
    fun `picker launch failure unfreezes choices and exposes one failure`() =
        runTest(dispatcher) {
            repository.apps = listOf(app("user.app", "User"))
            viewModel = createViewModel()
            advanceUntilIdle()
            val beforeExport = viewModel.uiState.value
            viewModel.beginExport()

            viewModel.failPendingExport("Picker unavailable")

            assertEquals(beforeExport, viewModel.uiState.value)
            assertEquals(null, viewModel.pendingExportRequest())
            assertEquals(
                ExportCompletion(ExportOutcome.FAILURE, "Picker unavailable"),
                viewModel.exportCompletion.value,
            )
        }

    @Test
    fun `destination callbacks without an available request are ignored`() =
        runTest(dispatcher) {
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.writePendingExport(io.mockk.mockk())
            viewModel.cancelPendingExport()
            viewModel.failPendingExport("Ignored")

            assertEquals(null, viewModel.pendingExportRequest())
            assertEquals(null, viewModel.exportCompletion.value)
            assertFalse(viewModel.uiState.value.isExporting)
        }

    @Test
    fun `canExport rejects every unavailable state`() {
        val selected = listOf(ExportAppItemUiModel("app", "App", true, false, true))

        assertFalse(ExportUiState(selectedApps = selected).canExport)
        assertFalse(ExportUiState(selectedApps = selected, isLoading = false, loadFailed = true).canExport)
        assertFalse(ExportUiState(selectedApps = selected, isLoading = false, isExporting = true).canExport)
        assertTrue(ExportUiState(selectedApps = selected, isLoading = false).canExport)
    }

    private fun createViewModel(
        exportFileWriter: ExportFileWriter = ExportFileWriter { _, _ -> ExportCompletion(ExportOutcome.SUCCESS) },
    ): ExportViewModel =
        ExportViewModel(
            repository = repository,
            dispatchers =
                object : DispatcherProvider {
                    override val io = dispatcher
                    override val main = dispatcher
                    override val default = dispatcher
                },
            appComparator = compareBy<App> { it.name }.thenBy { it.packageName },
            exportFileWriter = exportFileWriter,
        )

    private class ControllableExportFileWriter : ExportFileWriter {
        val result = CompletableDeferred<ExportCompletion>()
        var writeCount = 0

        override suspend fun write(
            uri: Uri,
            request: ExportRequest,
        ): ExportCompletion {
            writeCount++
            return result.await()
        }
    }

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
