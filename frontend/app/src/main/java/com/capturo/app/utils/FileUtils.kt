package com.capturo.app.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {
    fun savePdfResponse(context: Context, body: ResponseBody, filename: String): Uri? {
        return try {
            val file = File(context.cacheDir, filename)
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
