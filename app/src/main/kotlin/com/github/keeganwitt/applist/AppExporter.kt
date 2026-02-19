package com.github.keeganwitt.applist

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppExporter(
    private val activity: AppCompatActivity,
    private val itemsProvider: () -> List<AppItemUiModel>,
    private val formatter: ExportFormatter,
    private val crashReporter: CrashReporter? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    private val registry: ActivityResultRegistry = activity.activityResultRegistry,
) {
    internal var selectedAppInfoField: AppInfoField? = null
    private var currentExportType: ExportFormat? = null

    private val createFileLauncher: ActivityResultLauncher<Intent>

    init {
        // Register the result launcher directly with the ActivityResultRegistry so we can
        // safely register at any point in the Activity lifecycle (including after RESUMED),
        // which is especially useful for unit tests that use a fully-started Activity.
        createFileLauncher =
            registry.register(
                "app_exporter_${System.identityHashCode(this)}",
                StartActivityForResult(),
            ) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                    val uri = result.data?.data ?: return@register
                    when (currentExportType) {
                        ExportFormat.XML -> {
                            writeXmlToFile(uri)
                        }

                        ExportFormat.HTML -> {
                            writeHtmlToFile(uri)
                        }

                        ExportFormat.CSV -> {
                            writeCsvToFile(uri)
                        }

                        ExportFormat.TSV -> {
                            writeTsvToFile(uri)
                        }

                        null -> { /* Should not happen */ }
                    }
                }
            }
    }

    fun export(selectedAppInfoField: AppInfoField) {
        this.selectedAppInfoField = selectedAppInfoField
        showExportDialog()
    }

    private fun showExportDialog() {
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_export_type, null)
        builder.setView(dialogView)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.export_radio_group)

        builder.setPositiveButton(
            R.string.export,
        ) { dialog: DialogInterface?, which: Int ->
            val selectedId = radioGroup.checkedRadioButtonId
            when (selectedId) {
                R.id.radio_xml -> {
                    initiateExport(ExportFormat.XML)
                }

                R.id.radio_html -> {
                    initiateExport(ExportFormat.HTML)
                }

                R.id.radio_csv -> {
                    initiateExport(ExportFormat.CSV)
                }

                R.id.radio_tsv -> {
                    initiateExport(ExportFormat.TSV)
                }
            }
        }
        builder.setNegativeButton(
            android.R.string.cancel,
        ) { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }

        val dialog = builder.create()
        dialog.show()
    }

    internal fun initiateExport(type: ExportFormat) {
        val items = itemsProvider()
        if (items.isEmpty()) {
            Toast
                .makeText(
                    activity,
                    activity.getString(R.string.export_no_apps),
                    Toast.LENGTH_SHORT,
                ).show()
            return
        }

        currentExportType = type
        val timestamp =
            SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                .format(Date())
        val appInfoType = selectedAppInfoField!!.name.lowercase(Locale.getDefault())
        val fileName = "apps_" + appInfoType + "_" + timestamp + "." + type.extension

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType(type.mimeType)
        intent.putExtra(Intent.EXTRA_TITLE, fileName)

        createFileLauncher.launch(intent)
    }

    internal fun writeXmlToFile(uri: Uri) {
        val items = itemsProvider()
        val field = selectedAppInfoField!!
        activity.lifecycleScope.launch(dispatchers.io) {
            exportToFile(uri, ExportFormat.XML, items) { writer, itemsToExport ->
                formatter.writeXml(writer, itemsToExport, field)
            }
        }
    }

    internal fun writeCsvToFile(uri: Uri) {
        val items = itemsProvider()
        val field = selectedAppInfoField!!
        activity.lifecycleScope.launch(dispatchers.io) {
            exportToFile(uri, ExportFormat.CSV, items) { writer, itemsToExport ->
                formatter.writeCsv(writer, itemsToExport, field)
            }
        }
    }

    internal fun writeTsvToFile(uri: Uri) {
        val items = itemsProvider()
        val field = selectedAppInfoField!!
        activity.lifecycleScope.launch(dispatchers.io) {
            exportToFile(uri, ExportFormat.TSV, items) { writer, itemsToExport ->
                formatter.writeTsv(writer, itemsToExport, field)
            }
        }
    }

    internal fun writeHtmlToFile(uri: Uri) {
        val items = itemsProvider()
        activity.lifecycleScope.launch(dispatchers.io) {
            exportToFile(uri, ExportFormat.HTML, items) { writer, itemsToExport ->
                formatter.writeHtml(writer, itemsToExport)
            }
        }
    }

    private suspend fun exportToFile(
        uri: Uri,
        format: ExportFormat,
        items: List<AppItemUiModel>,
        contentWriter: (java.io.Writer, List<AppItemUiModel>) -> Unit,
    ) {
        try {
            val outputStream =
                activity.contentResolver.openOutputStream(uri)
                    ?: throw IOException("Failed to open output stream")
            outputStream.use { stream ->
                val writer = OutputStreamWriter(stream, StandardCharsets.UTF_8)
                contentWriter(writer, items)
                writer.flush()
            }
            withContext(dispatchers.main) {
                Toast
                    .makeText(
                        activity,
                        activity.getString(R.string.export_successful),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        } catch (e: IOException) {
            handleExportError(e, format)
        } catch (e: SecurityException) {
            handleExportError(e, format)
        }
    }

    private suspend fun handleExportError(
        e: Exception,
        format: ExportFormat,
    ) {
        val message = "Error exporting ${format.displayName}"
        Log.e(TAG, message, e)
        crashReporter?.recordException(e, message)
        withContext(dispatchers.main) {
            Toast
                .makeText(
                    activity,
                    activity.getString(R.string.export_failed, e.message),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    companion object {
        private val TAG = AppExporter::class.java.simpleName
    }

    internal enum class ExportFormat(
        val extension: String,
        val mimeType: String,
        val displayName: String,
    ) {
        XML("xml", "text/xml", "XML"),
        HTML("html", "text/html", "HTML"),
        CSV("csv", "text/csv", "CSV"),
        TSV("tsv", "text/tab-separated-values", "TSV"),
    }
}
