package com.example.logsstoragedemo.utils

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.logsstoragedemo.background.AlarmReceiver
import com.example.logsstoragedemo.model.FileReq
import com.example.logsstoragedemo.network.ApiClient
import com.example.logsstoragedemo.network.UploadRequestBody
import com.example.logsstoragedemo.utils.Constants.APP_NAME
import com.example.logsstoragedemo.views.FilePreviewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object FileLogger {
    private val TAG: String? = javaClass.simpleName
    private var controlViewAttached = false

    fun init(activity: Activity) {
        val view = activity.window.decorView.rootView
        val filePreviewer = FilePreviewer(activity)
        if (!controlViewAttached) {
            controlViewAttached = true
            filePreviewer.initFilePreviewer(view)
        } else {
            controlViewAttached = false
        }
    }

    // get all logs files from local storage
    suspend fun getAllFilesFromLocal(ctx: Context) {

        val files = ctx.filesDir?.listFiles()?.filter { logFile -> logFile.name.contains(APP_NAME) && !logFile.name.startsWith(".") }
        files?.forEach { logFile ->
            withContext(Dispatchers.IO) {
                Log.e(TAG, "Log File Name: " + logFile.name)
                val uploadFileReq = FileReq()
                val body = UploadRequestBody(
                    logFile,
                    "text",
                    object : UploadRequestBody.UploadCallback {
                        override fun onProgressUpdate(percentage: Int) {
                            Log.e(TAG, "onProgressUpdate: percentage-> $percentage ")
                        }

                    })
                val fileRequest: MultipartBody.Part =
                    MultipartBody.Part.createFormData("image", logFile.name, body)
                uploadFileReq.file = fileRequest
                uploadFile(logFile, uploadFileReq, ctx)
            }
        }

        // schedule alarm at next 24 hours
        val time = System.currentTimeMillis()
        scheduleAlarm(ctx, time + 1000 * 60 * 60 * 24)
    }

    // upload log files to server
    private suspend fun uploadFile(file: File, uploadFileReq: FileReq, ctx: Context) {
        try {
            val apiService = ApiClient.getApiService()
            val response = apiService.uploadFile(uploadFileReq.file)
            Log.e(TAG, "Upload File Response: $response")

            val responseTime = response.raw().receivedResponseAtMillis
            val requestTime = response.raw().sentRequestAtMillis
            val apiTime: Long = responseTime - requestTime
            Log.e(TAG, "Upload File Api Time: ${((apiTime / 1000.0))} Seconds")

            if (response.body() != null) {
                if (response.body()!!.status == "true") {
                    Log.e(TAG, "Upload File Successfully")

                    // delete logs files in local storage
                    if (file.exists()) {
                        file.delete()
                        Log.e(TAG, "Delete File from Local Storage")
                    }
                } else {
                    Log.e(TAG, "Upload File Error: ${response.body()!!.message}")
                    // append api error messages in log file
                    file.appendText(saveLog().toString())
                    // clear logs from logcat
                    clearLogs()
                }
            }

        } catch (e: Exception) {
            e.message?.let {
                Log.e(TAG, "Upload File Error: ${e.message}")
            }
        }

    }

    // get logs from logcat
    fun saveLog(): StringBuilder {
        val stringBuilderLog = StringBuilder()
        //logcat -d -v threadtime *:*
        val command = "logcat -d com.example.logsstoragedemo:I"
        val process = Runtime.getRuntime().exec(command)
        val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            stringBuilderLog.append(line).append("\n")
        }
        return stringBuilderLog
    }

    // clear logs from logcat
    fun clearLogs() {
        ProcessBuilder()
            .command("logcat", "-c")
            .redirectErrorStream(true)
            .start()
    }

    // trigger alarm at specific time
    private fun scheduleAlarm(context: Context?, scheduledTime: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE)
        val alarmManager =
            context?.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            scheduledTime,
            pendingIntent
        )
    }

}