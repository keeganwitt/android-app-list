package com.github.keeganwitt.applist

import android.content.DialogInterface
import android.net.Uri
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.IOException

class AppExporter(
    private val activity: AppCompatActivity,
    private val appsProvider: () -> List<App>,
    private val formatter: ExportFormatter,
    private val appSettings: AppSettings,
    private val crashReporter: CrashReporter? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    private val registry: ActivityResultRegistry = activity.activityResultRegistry,
) {
    private var pendingExportFormat: ExportFormat? = null

    private val exportLauncher =
        registry.register(
            "app_exporter_${System.identityHashCode(this)}",
            activity,
            ActivityResultContracts.CreateDocument("text/plain"),
        ) { uri ->
            uri?.let {
                val format = pendingExportFormat ?: ExportFormat.XML
                pendingExportFormat = null
                writeToFile(it, format)
            }
        }

    fun export() {
        val view = activity.layoutInflater.inflate(R.layout.dialog_export_type, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.export_radio_group)

        AlertDialog
            .Builder(activity)
            .setTitle(R.string.export_as)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                val format =
                    when (radioGroup.checkedRadioButtonId) {
                        R.id.radio_xml -> ExportFormat.XML
                        R.id.radio_html -> ExportFormat.HTML
                        R.id.radio_csv -> ExportFormat.CSV
                        R.id.radio_tsv -> ExportFormat.TSV
                        else -> ExportFormat.XML
                    }
                pendingExportFormat = format
                exportLauncher.launch("app-list." + format.extension)
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    internal fun writeToFile(
        uri: Uri,
        format: ExportFormat,
    ) {
        val apps = appsProvider()
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
            }
        } catch (e: Exception) {
            if (e !is IOException && e !is SecurityException) {
                crashReporter?.recordException(e, "Error exporting ${format.name}")
            }
            activity.runOnUiThread {
                Toast
                    .makeText(
                        activity,
                        activity.getString(R.string.export_failed, e.message),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }
}
