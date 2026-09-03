package com.github.keeganwitt.applist

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestAppListApplication::class)
class ExportLifecycleTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var writer: ControllableWriter

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        writer = ControllableWriter()
        RetainedExportActivity.viewModelFactory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    check(modelClass == ExportViewModel::class.java)
                    @Suppress("UNCHECKED_CAST")
                    return ExportViewModel(
                        repository = OneAppRepository,
                        dispatchers = TestDispatchers(dispatcher),
                        appComparator = compareBy { it.name },
                        exportFileWriter = writer,
                    ) as T
                }
            }
    }

    @After
    fun tearDown() {
        RetainedExportActivity.viewModelFactory = null
        Dispatchers.resetMain()
    }

    @Test
    fun `recreated activity observes one retained write and finishes on success`() =
        runTest(dispatcher) {
            val controller = Robolectric.buildActivity(RetainedExportActivity::class.java).setup()
            val firstActivity = controller.get()
            advanceUntilIdle()
            firstActivity.viewModel.beginExport()
            firstActivity.viewModel.writePendingExport(mockk<Uri>())
            testScheduler.runCurrent()
            assertEquals(1, writer.writeCount)

            controller.recreate()
            val recreatedActivity = controller.get()
            assertSame(firstActivity.viewModel, recreatedActivity.viewModel)
            assertFalse(recreatedActivity.isFinishing)

            writer.result.complete(ExportCompletion(ExportOutcome.SUCCESS))
            advanceUntilIdle()
            Shadows.shadowOf(recreatedActivity.mainLooper).idle()

            assertEquals(1, writer.writeCount)
            assertTrue(recreatedActivity.isFinishing)
        }

    internal class RetainedExportActivity : AppCompatActivity() {
        internal lateinit var viewModel: ExportViewModel

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val factory = checkNotNull(viewModelFactory)
            viewModel = ViewModelProvider(this, factory)[ExportViewModel::class.java]
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.exportCompletion.filterNotNull().collect { completion ->
                        if (viewModel.consumeExportCompletion(completion) && completion.outcome == ExportOutcome.SUCCESS) {
                            finish()
                        }
                    }
                }
            }
        }

        companion object {
            var viewModelFactory: ViewModelProvider.Factory? = null
        }
    }

    private class ControllableWriter : ExportFileWriter {
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

    private class TestDispatchers(
        override val io: kotlinx.coroutines.CoroutineDispatcher,
    ) : DispatcherProvider {
        override val main = io
        override val default = io
    }

    private object OneAppRepository : AppRepository {
        private val app =
            App(
                packageName = "user.app",
                name = "User",
                versionName = "1.0",
                archived = false,
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
                isDetailed = true,
            )

        override fun loadApps(
            field: AppInfoField,
            systemAppsOnly: Boolean,
            showArchivedApps: Boolean,
            descending: Boolean,
            reload: Boolean,
        ): Flow<List<App>> = flowOf(listOf(app))

        override fun getSyncState(): Flow<SyncState> = flowOf(SyncState.Idle)

        override fun observeCachedApps(): Flow<List<App>> = flowOf(listOf(app))

        override suspend fun refreshCache(force: Boolean) = Unit

        override suspend fun getCachedApps(): List<App> = listOf(app)
    }
}
