package com.capturo.app.ui.gallery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DownloadAllWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val bookingId = inputData.getString("booking_id") ?: return Result.failure()
        val downloadUrl = inputData.getString("download_url") ?: return Result.failure()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "gallery_downloads"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gallery Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Downloading Delivered Gallery")
            .setContentText("Preparing archive...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, 0, true)

        val notificationId = bookingId.hashCode()
        notificationManager.notify(notificationId, notificationBuilder.build())

        return try {
            val client = OkHttpClient()
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                notificationBuilder.setContentText("Download failed")
                    .setOngoing(false)
                    .setProgress(0, 0, false)
                notificationManager.notify(notificationId, notificationBuilder.build())
                return Result.failure()
            }

            val body = response.body
            if (body == null) {
                return Result.failure()
            }

            val file = File(
                applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "delivery_${bookingId}.zip"
            )

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            val totalBytes = body.contentLength()
            var bytesDownloaded: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead
                if (totalBytes > 0) {
                    val progress = ((bytesDownloaded * 100) / totalBytes).toInt()
                    setProgress(workDataOf("progress" to progress))
                    
                    notificationBuilder.setContentText("Downloading... $progress%")
                        .setProgress(100, progress, false)
                    notificationManager.notify(notificationId, notificationBuilder.build())
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            notificationBuilder.setContentText("Download complete")
                .setOngoing(false)
                .setProgress(0, 0, false)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
            notificationManager.notify(notificationId, notificationBuilder.build())

            Result.success(workDataOf("file_path" to file.absolutePath))
        } catch (e: Exception) {
            notificationBuilder.setContentText("Download failed: ${e.localizedMessage}")
                .setOngoing(false)
                .setProgress(0, 0, false)
            notificationManager.notify(notificationId, notificationBuilder.build())
            Result.failure()
        }
    }
}
