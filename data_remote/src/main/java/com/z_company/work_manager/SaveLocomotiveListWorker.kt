package com.z_company.work_manager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SaveLocomotiveListWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }
}