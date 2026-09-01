package com.github.keeganwitt.applist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.IOException

internal enum class ExportOutcome {
    SUCCESS,
    FAILURE,
    CANCELED,
}

internal class AppExporter(
    private val activity: AppCompatActivity,
    private val formatter: ExportFormatter,
    private val appSettings: AppSettings,
    private val crashReporter: CrashReporter? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    registry: ActivityResultRegistry = activity.activityResultRegistry,
    private val onOutcome: (ExportOutcome) -> Unit = {},
) {
    private var pendingExportFormat: ExportFormat? = null
    private var pendingApps: List<App>? = null

    private val exportLauncher =
        registry.register(
            "app_exporter",
            activity,
            object : ActivityResultContract<ExportFormat, Uri?>() {
                override fun createIntent(
                    context: Context,
                    input: ExportFormat,
                ): Intent =
                    Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(input.mimeType)
                        .putExtra(Intent.EXTRA_TITLE, "app-list.${input.extension}")

                override fun parseResult(
                    resultCode: Int,
                    intent: Intent?,
                ): Uri? = if (resultCode == Activity.RESULT_OK) intent?.data else null
            },
        ) { uri ->
            val format = pendingExportFormat
            val apps = pendingApps
            pendingExportFormat = null
            pendingApps = null
            if (uri == null) {
                if (format != null && apps != null) onOutcome(ExportOutcome.CANCELED)
            } else if (format != null && apps != null) {
                writeToFile(uri, format, apps)
            } else {
                onOutcome(ExportOutcome.FAILURE)
            }
        }

    fun export(
        format: ExportFormat,
        apps: List<App>,
    ) {
        if (apps.isEmpty()) {
            onOutcome(ExportOutcome.FAILURE)
            return
        }
        pendingExportFormat = format
        pendingApps = apps.toList()
        try {
            exportLauncher.launch(format)
        } catch (e: Exception) {
            pendingExportFormat = null
            pendingApps = null
            crashReporter?.recordException(e, "Error opening export picker")
            Toast
                .makeText(
                    activity,
                    activity.getString(R.string.export_failed, e.message),
                    Toast.LENGTH_SHORT,
                ).show()
            onOutcome(ExportOutcome.FAILURE)
        }
    }

    internal fun restorePendingRequest(request: ExportRequest) {
        pendingExportFormat = request.format
        pendingApps = request.apps.toList()
    }

    internal fun writeToFile(
        uri: Uri,
        format: ExportFormat,
        apps: List<App>,
    ) {
        if (apps.isEmpty()) {
            onOutcome(ExportOutcome.FAILURE)
            return
        }
        val includeUsageStats = shouldIncludeUsageStats()
        val loadingFailedValue = activity.getString(R.string.export_loading_failed)
        activity.lifecycleScope.launch(dispatchers.io) {
            exportToFile(uri, format) {
                formatter.write(format, it, apps, includeUsageStats, loadingFailedValue)
            }
        }
    }

    private fun shouldIncludeUsageStats(): Boolean = appSettings.isIncludeUsageStatsInExportEnabled()

    private fun exportToFile(
        uri: Uri,
        format: ExportFormat,
        writeBlock: (java.io.Writer) -> Unit,
    ) {
        try {
            activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    writeBlock(writer)
                }
            } ?: throw IOException("Failed to open output stream")
            activity.runOnUiThread {
                Toast
                    .makeText(
                        activity,
                        activity.getString(R.string.export_successful),
                        Toast.LENGTH_SHORT,
                    ).show()
                onOutcome(ExportOutcome.SUCCESS)
            }
        } catch (e: Exception) {
            crashReporter?.recordException(e, "Error exporting ${format.name}")
            activity.runOnUiThread {
                Toast
                    .makeText(
                        activity,
                        activity.getString(R.string.export_failed, e.message),
                        Toast.LENGTH_SHORT,
                    ).show()
                onOutcome(ExportOutcome.FAILURE)
            }
        }
    }
}
