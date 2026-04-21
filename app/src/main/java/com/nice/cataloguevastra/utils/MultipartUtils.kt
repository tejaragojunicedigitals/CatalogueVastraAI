package com.nice.cataloguevastra.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class TempMultipartPart(
    val part: MultipartBody.Part,
    private val tempFile: File
) {
    fun deleteTempFile() {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}

fun String.toPlainTextRequestBody(): RequestBody {
    return toRequestBody("text/plain".toMediaTypeOrNull())
}

fun Context.createMultipartPart(
    partName: String,
    uri: Uri
): TempMultipartPart? {
    val fileName = queryDisplayName(uri)?.sanitizeFileName()
        ?: "${partName}_${System.currentTimeMillis()}.jpg"
    val mimeType = contentResolver.getType(uri) ?: "image/*"
    val extension = fileName.substringAfterLast('.', "")
    val tempFilePrefix = fileName
        .substringBeforeLast('.', fileName)
        .take(24)
        .padEnd(3, '_')
    val targetFile = File.createTempFile(
        tempFilePrefix,
        if (extension.isNotBlank()) ".$extension" else "",
        cacheDir
    )

    contentResolver.openInputStream(uri)?.use { inputStream ->
        targetFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    } ?: run {
        targetFile.delete()
        return null
    }

    val requestBody = targetFile.asRequestBody(mimeType.toMediaTypeOrNull())
    return TempMultipartPart(
        part = MultipartBody.Part.createFormData(partName, fileName, requestBody),
        tempFile = targetFile
    )
}

fun Context.queryDisplayName(uri: Uri): String? {
    return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
}

private fun String.sanitizeFileName(): String {
    return replace(Regex("[^a-zA-Z0-9._-]"), "_")
}
