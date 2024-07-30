package com.z_company.work_manager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.z_company.work_manager.PhotoFieldName.PHOTO_CLASS_NAME_REMOTE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val REMOVE_PHOTO_OBJECT_ID_KEY = "REMOVE_PHOTO_OBJECT_ID_KEY"

class RemovePhotoWorker(context: Context, parameters: WorkerParameters):
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val parseObject = ParseObject(PHOTO_CLASS_NAME_REMOTE)
            val data = inputData.getString(REMOVE_PHOTO_OBJECT_ID_KEY)
            parseObject.objectId = data
            parseObject.deleteInBackground()
            return@withContext Result.success()
        } catch (e: Exception) {
            return@withContext Result.retry()
        }
    }
}