package com.github.keeganwitt.applist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity

internal enum class ExportOutcome {
    SUCCESS,
    FAILURE,
    CANCELED,
}

internal class AppExporter(
    private val activity: AppCompatActivity,
    private val crashReporter: CrashReporter? = null,
    registry: ActivityResultRegistry = activity.activityResultRegistry,
    private val onResult: (Uri?) -> Unit,
    private val onLaunchFailure: (String?) -> Unit,
) {
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
            onResult(uri)
        }

    fun export(format: ExportFormat) {
        try {
            exportLauncher.launch(format)
        } catch (e: Exception) {
            crashReporter?.recordException(e, "Error opening export picker")
            onLaunchFailure(e.message)
        }
    }
}
