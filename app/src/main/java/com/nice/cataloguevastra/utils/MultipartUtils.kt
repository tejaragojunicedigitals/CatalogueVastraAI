package com.nice.cataloguevastra.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.MessageDigest
import java.util.Locale

data class TempMultipartPart(
    val part: MultipartBody.Part,
    private val tempFile: File,
    val fileName: String,
    val byteSize: Long,
    val sha256: String
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
    val sourceFileName = queryDisplayName(uri)?.sanitizeFileName()
        ?: "${partName}_${System.currentTimeMillis()}.jpg"
    val extension = sourceFileName.substringAfterLast('.', "")
        .takeIf { it.isNotBlank() }
        ?: defaultImageExtension(contentResolver.getType(uri))
    val fileName = "${partName}_${System.currentTimeMillis()}.$extension"
    val mimeType = contentResolver.getType(uri) ?: "image/*"
    val targetFile = File.createTempFile(
        partName.take(24).padEnd(3, '_'),
        ".$extension",
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
        tempFile = targetFile,
        fileName = fileName,
        byteSize = targetFile.length(),
        sha256 = targetFile.sha256()
    )
}

fun Context.createCompressedImageMultipartPart(
    partName: String,
    uri: Uri,
    maxSidePx: Int = 1280,
    quality: Int = 85
): TempMultipartPart? {
    val bitmap = decodeScaledBitmap(uri, maxSidePx) ?: return createMultipartPart(partName, uri)
    val fileName = "${partName}_${System.currentTimeMillis()}.jpg"
    val targetFile = File.createTempFile(
        partName.take(24).padEnd(3, '_'),
        ".jpg",
        cacheDir
    )

    targetFile.outputStream().use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    }
    bitmap.recycle()

    val requestBody = targetFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
    return TempMultipartPart(
        part = MultipartBody.Part.createFormData(partName, fileName, requestBody),
        tempFile = targetFile,
        fileName = fileName,
        byteSize = targetFile.length(),
        sha256 = targetFile.sha256()
    )
}

fun Context.queryDisplayName(uri: Uri): String? {
    return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
}

private fun Context.decodeScaledBitmap(uri: Uri, maxSidePx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, bounds)
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val largestSide = maxOf(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (largestSide / sampleSize > maxSidePx) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, decodeOptions)
    }
}

private fun String.sanitizeFileName(): String {
    return replace(Regex("[^a-zA-Z0-9._-]"), "_")
}

private fun defaultImageExtension(mimeType: String?): String {
    return when (mimeType?.lowercase(Locale.ENGLISH)) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "jpg"
    }
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
