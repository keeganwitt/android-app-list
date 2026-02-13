package com.github.keeganwitt.applist

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
) {
    internal var selectedAppInfoField: AppInfoField? = null
    private var currentExportType: String? = null

    private val createFileLauncher: ActivityResultLauncher<Intent>

    init {
        // Register the result launcher directly with the ActivityResultRegistry so we can
        // safely register at any point in the Activity lifecycle (including after RESUMED),
        // which is especially useful for unit tests that use a fully-started Activity.
        createFileLauncher =
            activity.activityResultRegistry.register(
                "app_exporter_${System.identityHashCode(this)}",
                StartActivityForResult(),
            ) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                    val uri = result.data?.data ?: return@register
                    when (currentExportType) {
                        "xml" -> writeXmlToFile(uri)
                        "html" -> writeHtmlToFile(uri)
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
            if (selectedId == R.id.radio_xml) {
                initiateExport("xml")
            } else if (selectedId == R.id.radio_html) {
                initiateExport("html")
            }
        }
        builder.setNegativeButton(
            android.R.string.cancel,
        ) { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }

        val dialog = builder.create()
        dialog.show()
    }

    internal fun initiateExport(type: String) {
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
        val fileName = "apps_" + appInfoType + "_" + timestamp + "." + type

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType(if (type == "xml") "text/xml" else "text/html")
        intent.putExtra(Intent.EXTRA_TITLE, fileName)

        createFileLauncher.launch(intent)
    }

    internal fun writeXmlToFile(uri: Uri) {
        exportToFile(uri, "XML") { items ->
            formatter.toXml(items, selectedAppInfoField!!)
        }
    }

    internal fun writeHtmlToFile(uri: Uri) {
        exportToFile(uri, "HTML") { items ->
            formatter.toHtml(items)
        }
    }

    private fun exportToFile(
        uri: Uri,
        formatName: String,
        contentGenerator: (List<AppItemUiModel>) -> String,
    ) {
        try {
            val outputStream =
                activity.contentResolver.openOutputStream(uri)
                    ?: throw java.io.IOException("Failed to open output stream")
            outputStream.use { stream ->
                val writer = OutputStreamWriter(stream, StandardCharsets.UTF_8)
                val items = itemsProvider()
                val content = contentGenerator(items)
                writer.write(content)
                writer.flush()
            }
            Toast
                .makeText(
                    activity,
                    activity.getString(R.string.export_successful),
                    Toast.LENGTH_SHORT,
                ).show()
        } catch (e: Exception) {
            val message = "Error exporting $formatName"
            Log.e(TAG, message, e)
            crashReporter?.recordException(e, message)
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
}
