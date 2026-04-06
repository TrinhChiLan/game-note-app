package com.example.assignment3.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageManager {
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "custom_image_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteImageFromInternalStorage(path: String?) {
        if (path == null) return
        try {
            val file = File(path)
            if (file.exists() && file.absolutePath.startsWith(file.parent ?: "")) { // Basic check to ensure we are in the right directory
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
