package com.ddc.bansoogi.collection.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import java.io.OutputStream

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String): Boolean {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Bansoogi")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        var outputStream: OutputStream? = null
        try {
            outputStream = resolver.openOutputStream(it)
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            outputStream?.close()
        }

        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
        return true
    } ?: return false
}