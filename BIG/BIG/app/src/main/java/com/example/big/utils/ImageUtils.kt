// ImageUtils.kt
package com.example.big.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// ImageUtils.kt
object ImageUtils {
    private const val TAG = "ImageUtils"

    fun uriToMultipart(context: Context, uri: Uri, paramName: String): MultipartBody.Part? {
        try {
            Log.d(TAG, "开始处理图片 URI: $uri, 参数名: $paramName")

            // 创建临时文件
            val file = createTempFileFromUri(context, uri)
            if (file == null) {
                Log.e(TAG, "无法从URI创建临时文件")
                return null
            }

            Log.d(TAG, "成功创建临时文件: ${file.absolutePath}, 大小: ${file.length()} bytes")

            // 验证文件是否存在且可读
            if (!file.exists() || !file.canRead()) {
                Log.e(TAG, "临时文件不存在或不可读: exists=${file.exists()}, canRead=${file.canRead()}")
                return null
            }

            // 创建 MultipartBody.Part
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            Log.d(TAG, "成功创建RequestBody")

            val part = MultipartBody.Part.createFormData(paramName, file.name, requestBody)
            Log.d(TAG, "成功创建MultipartBody.Part")

            return part
        } catch (e: Exception) {
            Log.e(TAG, "创建MultipartBody.Part过程中出现异常: ${e.message}", e)
            e.printStackTrace()
            return null
        }
    }

    private fun createTempFileFromUri(context: Context, uri: Uri): File? {
        try {
            Log.d(TAG, "开始从URI创建临时文件: $uri")

            // 获取输入流
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "无法从URI获取输入流")
                return null
            }

            Log.d(TAG, "成功获取输入流")

            // 读取bitmap
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Log.e(TAG, "无法解码图片流为Bitmap")
                return null
            }

            Log.d(TAG, "成功解码Bitmap，尺寸: ${bitmap.width}x${bitmap.height}")

            // 压缩图片
            val compressedBitmap = compressBitmap(bitmap)
            Log.d(TAG, "成功压缩Bitmap，新尺寸: ${compressedBitmap.width}x${compressedBitmap.height}")

            // 创建临时文件
            val file = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
            Log.d(TAG, "准备写入临时文件: ${file.absolutePath}")

            val outputStream = FileOutputStream(file)
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            Log.d(TAG, "成功写入临时文件，大小: ${file.length()} bytes")

            return file
        } catch (e: IOException) {
            Log.e(TAG, "创建临时文件过程中出现异常: ${e.message}", e)
            e.printStackTrace()
            return null
        }
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        // 调整图片大小，避免过大
        val maxSize = 1024
        val width = bitmap.width
        val height = bitmap.height

        Log.d(TAG, "原始Bitmap尺寸: ${width}x${height}")

        if (width <= maxSize && height <= maxSize) {
            Log.d(TAG, "Bitmap尺寸已符合要求，无需压缩")
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        Log.d(TAG, "Bitmap将被压缩至: ${newWidth}x${newHeight}")

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}