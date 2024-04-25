package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.coroutines.suspendFind
import com.z_company.core.ResultState
import com.z_company.domain.use_cases.PhotoUseCase
import com.z_company.domain.util.ifNotZero
import com.z_company.entity.Photo
import com.z_company.entity_converter.PhotoConverter
import com.z_company.work_manager.PhotoFieldName.BASE_64_FIELD_NAME
import com.z_company.work_manager.PhotoFieldName.DATE_OF_CREATE
import com.z_company.work_manager.PhotoFieldName.PHOTO_BASIC_ID_FIELD_NAME
import com.z_company.work_manager.PhotoFieldName.PHOTO_CLASS_NAME_REMOTE
import com.z_company.work_manager.PhotoFieldName.PHOTO_ID_FIELD_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val LOAD_PHOTO_WORKER_INPUT_KEY = "LOAD_PHOTO_WORKER_INPUT_KEY"
const val LOAD_PHOTO_WORKER_OUTPUT_KEY = "LOAD_PHOTO_WORKER_OUTPUT_KEY"

class LoadPhotoRemoteWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters), KoinComponent {
    private val photoUseCase: PhotoUseCase by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val basicId = inputData.getString(LOAD_PHOTO_WORKER_INPUT_KEY)
            val query = ParseQuery<ParseObject>(PHOTO_CLASS_NAME_REMOTE)
            query.whereEqualTo(PHOTO_BASIC_ID_FIELD_NAME, basicId)
            val photoRemoteList = query.suspendFind()
            val photoList: MutableList<Photo> = mutableListOf()
            photoRemoteList.forEach { parseObject ->
                var photo = Photo()
                parseObject.apply {
                    getString(PHOTO_ID_FIELD_NAME)?.let { id ->
                        photo = photo.copy(photoId = id)
                    }
                    getString(PHOTO_BASIC_ID_FIELD_NAME)?.let { basicId ->
                        photo = photo.copy(basicId = basicId)
                        photo = photo.copy(remoteObjectId = parseObject.objectId)
                    }
                    getString(BASE_64_FIELD_NAME)?.let { base64 ->
                        photo = photo.copy(base64 = base64)
                    }
                    getLong(DATE_OF_CREATE).ifNotZero()?.let { date ->
                        photo = photo.copy(dateOfCreate = date)
                    }
                }
                photoList.add(photo)
            }
            var job: Job? = null
            PhotoConverter.fromRemoteList(photoList).forEach { photo ->
                job?.cancel()
                job = CoroutineScope(Dispatchers.IO).launch {
                    photoUseCase.addingPhoto(photo).collect {
                        if (it is ResultState.Success) {
                            job?.cancel()
                        }
                    }
                }
            }
            job?.join()
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "ex load photo = ${e.message}")
            return@withContext Result.retry()
        }
    }
}