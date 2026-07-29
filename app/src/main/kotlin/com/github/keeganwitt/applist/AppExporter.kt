package com.github.keeganwitt.applist

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.ActivityResultRegistry
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class AppExporter(
    private val activity: AppCompatActivity,
    private val repository: AppRepository,
    private val formatter: ExportFormatter,
    private val appSettings: AppSettings,
    private val crashReporter: CrashReporter? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    registry: ActivityResultRegistry = activity.activityResultRegistry,
) {
    private var pendingExportFormat: ExportFormat? = null
    private var pendingPackageNames: List<String>? = null

    private val exportLauncher =
        registry.register(
            "app_exporter_${System.identityHashCode(this)}",
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
            uri?.let {
                val format = pendingExportFormat ?: ExportFormat.XML
                pendingExportFormat = null
                val packageNames = pendingPackageNames
                pendingPackageNames = null
                writeToFile(it, format, packageNames)
            }
        }

    fun export(packageNames: List<String>? = null) {
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
                pendingPackageNames = packageNames
                exportLauncher.launch(format)
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    internal fun writeToFile(
        uri: Uri,
        format: ExportFormat,
        packageNames: List<String>? = null,
    ) {
        val includeUsageStats = shouldIncludeUsageStats()
        val loadingFailedValue = activity.getString(R.string.export_loading_failed)
        activity.lifecycleScope.launch(dispatchers.io) {
            try {
                // Force refresh cache to ensure export has fresh data
                repository.refreshCache(force = true)
                val cachedApps = repository.getCachedApps()
                val apps =
                    packageNames?.let { names ->
                        val appsByPackageName = cachedApps.associateBy { it.packageName }
                        names.mapNotNull(appsByPackageName::get)
                    } ?: cachedApps
                exportToFile(uri, format) {
                    formatter.write(format, it, apps, includeUsageStats, loadingFailedValue)
                }
            } catch (e: Exception) {
                withContext(dispatchers.main) {
                    Toast.makeText(activity, R.string.export_failed, Toast.LENGTH_SHORT).show()
                }
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
            crashReporter?.recordException(e, "Error exporting ${format.name}")
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
