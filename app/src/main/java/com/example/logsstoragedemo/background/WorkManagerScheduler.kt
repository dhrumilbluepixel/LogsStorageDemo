package com.example.logsstoragedemo.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

object WorkManagerScheduler {

    fun triggerScheduledWork(context: Context) {

        val myConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()


        val onTimeWorkRequest = OneTimeWorkRequest.Builder(UploadFileWorker::class.java)
            .setConstraints(myConstraints)
            .addTag("myWorkManager")
            .build()


        WorkManager.getInstance(context).enqueueUniqueWork("myWorkManager", ExistingWorkPolicy.REPLACE, onTimeWorkRequest)

    }
}