// ImageUtils.kt
package com.example.big.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    fun uriToMultipart(context: Context, uri: Uri, paramName: String): MultipartBody.Part? {
        try {
            // 创建临时文件
            val file = createTempFileFromUri(context, uri) ?: return null

            // 创建 MultipartBody.Part
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            return MultipartBody.Part.createFormData(paramName, file.name, requestFile)

        } catch (e: Exception) {
            Log.e("ImageUtils", "Error converting Uri to MultipartBody.Part: ${e.message}")
            return null
        }
    }

    private fun createTempFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                // 创建临时文件
                val file = File.createTempFile("upload_", ".jpg", context.cacheDir)
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(4 * 1024) // 4k buffer
                    var read: Int
                    while (stream.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
                // 压缩图片
                compressImage(file.path, file.path, 80)
                file
            }
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error creating temp file: ${e.message}")
            null
        }
    }

    private fun compressImage(inputPath: String, outputPath: String, quality: Int): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(inputPath)
            val out = FileOutputStream(outputPath)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.close()
            bitmap.recycle()
            true
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error compressing image: ${e.message}")
            false
        }
    }
}