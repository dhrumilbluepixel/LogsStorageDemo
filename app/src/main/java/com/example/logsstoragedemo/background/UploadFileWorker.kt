package com.example.logsstoragedemo.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.logsstoragedemo.utils.FileLogger

class UploadFileWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {

        // upload logs files
        FileLogger.getAllFilesFromLocal(ctx)

        return Result.success()
    }

}