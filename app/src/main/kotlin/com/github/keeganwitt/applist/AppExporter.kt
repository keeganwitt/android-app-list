package com.github.keeganwitt.applist

import android.app.usage.UsageStatsManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.Html
import android.util.Base64
import android.util.Log
import android.util.Xml
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppExporter(
    private val activity: AppCompatActivity,
    private val appInfoAdapter: AppInfoAdapter?,
    private val packageManager: PackageManager,
    private val usageStatsManager: UsageStatsManager
) {
    private var selectedAppInfoField: AppInfoField? = null
    private var currentExportType: String? = null

    private val createFileLauncher: ActivityResultLauncher<Intent>

    init {
        this.createFileLauncher = activity.registerForActivityResult(
            StartActivityForResult()
        ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                if (uri != null) {
                    if ("xml" == currentExportType) {
                        writeXmlToFile(uri)
                    } else if ("html" == currentExportType) {
                        writeHtmlToFile(uri)
                    }
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
            R.string.export
        ) { dialog: DialogInterface?, which: Int ->
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId == R.id.radio_xml) {
                initiateExport("xml")
            } else if (selectedId == R.id.radio_html) {
                initiateExport("html")
            }
        }
        builder.setNegativeButton(
            android.R.string.cancel
        ) { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }

        val dialog = builder.create()
        dialog.show()
    }

    private fun initiateExport(type: String) {
        if (appInfoAdapter == null || appInfoAdapter.itemCount == 0) {
            Toast.makeText(
                activity,
                activity.getString(R.string.export_no_apps),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        currentExportType = type
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            .format(Date())
        val appInfoType = selectedAppInfoField!!.name.lowercase(Locale.getDefault())
        val fileName = "apps_" + appInfoType + "_" + timestamp + "." + type

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType(if (type == "xml") "text/xml" else "text/html")
        intent.putExtra(Intent.EXTRA_TITLE, fileName)

        createFileLauncher.launch(intent)
    }

    private fun writeXmlToFile(uri: Uri) {
        try {
            activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val serializer = Xml.newSerializer()
                serializer.setOutput(outputStream, "UTF-8")
                serializer.startDocument("UTF-8", true)
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

                serializer.startTag("", "apps")

                for (i in 0..<appInfoAdapter!!.itemCount) {
                    val app = appInfoAdapter.currentList[i]
                    val appName = app.applicationInfo.loadLabel(packageManager).toString()
                    val packageName = app.applicationInfo.packageName
                    val infoType = selectedAppInfoField!!.name
                    val infoValue = try {
                        app.getTextValue(activity, packageManager, usageStatsManager)?.toString() ?: ""
                    } catch (e: PackageManager.NameNotFoundException) {
                        ""
                    }

                    serializer.startTag("", "app")
                    serializer.startTag("", "appName")
                    serializer.text(appName)
                    serializer.endTag("", "appName")
                    serializer.startTag("", "appPackage")
                    serializer.text(packageName ?: "")
                    serializer.endTag("", "appPackage")
                    serializer.startTag("", "appInfoType")
                    serializer.text(infoType)
                    serializer.endTag("", "appInfoType")
                    serializer.startTag("", "appInfoValue")
                    serializer.text(infoValue)
                    serializer.endTag("", "appInfoValue")
                    serializer.endTag("", "app")
                }

                serializer.endTag("", "apps")
                serializer.endDocument()
            }
            Toast.makeText(
                activity,
                activity.getString(R.string.export_successful),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            val message = "Error exporting XML"
            Log.e(TAG, message, e)
            FirebaseCrashlytics.getInstance().log(message)
            FirebaseCrashlytics.getInstance().recordException(e)
            Toast.makeText(
                activity,
                activity.getString(R.string.export_failed) + " " + e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun writeHtmlToFile(uri: Uri) {
        try {
            activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
                val htmlBuilder = StringBuilder()
                htmlBuilder.append("<!DOCTYPE html>\n")
                htmlBuilder.append("<html>\n")
                htmlBuilder.append("<head>\n")
                htmlBuilder.append("<meta charset=\"UTF-8\">\n")
                htmlBuilder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                htmlBuilder.append("<title>App List</title>\n")
                htmlBuilder.append("<style>\n")
                htmlBuilder.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n")
                htmlBuilder.append("h1 { text-align: center; color: #333; }\n")
                htmlBuilder.append(".app-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; padding: 10px; }\n")
                htmlBuilder.append(".app-item { background: white; border-radius: 8px; padding: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n")
                htmlBuilder.append(".app-icon { width: 50px; height: 50px; margin: 0 auto; }\n")
                htmlBuilder.append(".app-name { font-weight: bold; margin-top: 8px; word-wrap: break-word; }\n")
                htmlBuilder.append(".package-name { font-style: italic; font-size: 0.9em; color: #666; margin-top: 4px; word-wrap: break-word; }\n")
                htmlBuilder.append(".app-info { margin-top: 4px; color: #333; word-wrap: break-word; }\n")
                htmlBuilder.append("@media (min-width: 600px) { .app-grid { grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); } }\n")
                htmlBuilder.append("</style>\n")
                htmlBuilder.append("</head>\n")
                htmlBuilder.append("<body>\n")
                htmlBuilder.append("<h1>App List</h1>\n")
                htmlBuilder.append("<div class=\"app-grid\">\n")

                for (i in 0..<appInfoAdapter!!.itemCount) {
                    val app = appInfoAdapter.currentList[i]
                    val appName = Html.escapeHtml(
                        app.applicationInfo.loadLabel(packageManager).toString()
                    )
                    val packageName = Html.escapeHtml(app.applicationInfo.packageName)

                    val infoValue = try {
                        val spanned = app.getTextValue(
                            activity,
                            packageManager,
                            usageStatsManager
                        )
                        if (spanned != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Html.toHtml(spanned, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                            } else {
                                @Suppress("DEPRECATION")
                                Html.toHtml(spanned)
                            }
                        } else {
                            ""
                        }
                    } catch (_: PackageManager.NameNotFoundException) {
                        ""
                    }

                    val iconBase64 =
                        drawableToBase64(app.applicationInfo.loadIcon(packageManager))

                    htmlBuilder.append("<div class=\"app-item\">\n")
                    htmlBuilder.append("<img class=\"app-icon\" src=\"data:image/png;base64,")
                        .append(iconBase64).append("\" alt=\"App Icon\">\n")
                    htmlBuilder.append("<div class=\"app-name\">").append(appName)
                        .append("</div>\n")
                    htmlBuilder.append("<div class=\"package-name\">").append(packageName)
                        .append("</div>\n")
                    htmlBuilder.append("<div class=\"app-info\">").append(infoValue)
                        .append("</div>\n")
                    htmlBuilder.append("</div>\n")
                }

                htmlBuilder.append("</div>\n")
                htmlBuilder.append("</body>\n")
                htmlBuilder.append("</html>\n")

                writer.write(htmlBuilder.toString())
                writer.flush()
            }
            Toast.makeText(
                activity,
                activity.getString(R.string.export_successful),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            val message = "Error exporting HTML"
            Log.e(TAG, message, e)
            FirebaseCrashlytics.getInstance().log(message)
            FirebaseCrashlytics.getInstance().recordException(e)
            Toast.makeText(
                activity,
                activity.getString(R.string.export_failed) + " " + e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun drawableToBase64(drawable: Drawable?): String {
        if (drawable == null) {
            return ""
        }
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    companion object {
        private val TAG = AppExporter::class.java.simpleName
    }
}
