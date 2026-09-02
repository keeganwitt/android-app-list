package com.github.keeganwitt.applist

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException

internal data class ExportCompletion(
    val outcome: ExportOutcome,
    val errorMessage: String? = null,
)

internal fun interface ExportFileWriter {
    suspend fun write(
        uri: Uri,
        request: ExportRequest,
    ): ExportCompletion
}

internal class DefaultExportFileWriter(
    private val contentResolver: ContentResolver,
    private val formatter: ExportFormatter,
    private val appSettings: AppSettings,
    private val loadingFailedValue: String,
    private val crashReporter: CrashReporter? = null,
) : ExportFileWriter {
    override suspend fun write(
        uri: Uri,
        request: ExportRequest,
    ): ExportCompletion =
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    formatter.write(
                        request.format,
                        writer,
                        request.apps,
                        appSettings.isIncludeUsageStatsInExportEnabled(),
                        loadingFailedValue,
                    )
                }
            } ?: throw IOException("Failed to open output stream")
            ExportCompletion(ExportOutcome.SUCCESS)
        } catch (exception: Exception) {
            crashReporter?.recordException(exception, "Error exporting ${request.format.name}")
            ExportCompletion(ExportOutcome.FAILURE, exception.message)
        }
}
