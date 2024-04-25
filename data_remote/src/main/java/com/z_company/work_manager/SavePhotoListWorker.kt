package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseRelation
import com.parse.coroutines.suspendSave
import com.z_company.core.ResultState
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.type_converter.PhotoJSONConverter
import com.z_company.work_manager.PhotoFieldName.BASE_64_FIELD_NAME
import com.z_company.work_manager.PhotoFieldName.BASIC_DATA_FIELD_NAME
import com.z_company.work_manager.PhotoFieldName.DATE_OF_CREATE
import com.z_company.work_manager.PhotoFieldName.PHOTO_BASIC_ID_FIELD_NAME
import com.z_company.work_manager.PhotoFieldName.PHOTO_CLASS_NAME_REMOTE
import com.z_company.work_manager.PhotoFieldName.PHOTO_ID_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val PHOTOS_INPUT_KEY = "PHOTOS_INPUT_KEY"

class SavePhotoListWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val basicDataObjectId = inputData.getString(BASIC_DATA_OBJECT_ID_KEY)
            val basicDataObject = ParseObject(BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE)
            basicDataObject.objectId = basicDataObjectId
            val value = inputData.getStringArray(PHOTOS_INPUT_KEY)
            value?.forEach { photoId ->
                var job: Job? = null
                job = this.launch {
                    routeUseCase.getPhotoById(photoId).collect { result ->
                        if (result is ResultState.Success) {
                            result.data?.let { photo ->
                                val photoObject = ParseObject(PHOTO_CLASS_NAME_REMOTE)
                                if (!photo.remoteObjectId.isNullOrEmpty()) {
                                    photoObject.objectId = photo.remoteObjectId
                                }
                                photoObject.put(PHOTO_ID_FIELD_NAME, photo.photoId)
                                photoObject.put(PHOTO_BASIC_ID_FIELD_NAME, photo.basicId)
                                photoObject.put(BASE_64_FIELD_NAME, photo.base64)
                                photoObject.put(DATE_OF_CREATE, photo.dateOfCreate)
                                val basicDataRelation: ParseRelation<ParseObject> =
                                    photoObject.getRelation(BASIC_DATA_FIELD_NAME)
                                basicDataRelation.add(basicDataObject)

                                this.launch {
                                    photoObject.suspendSave()
                                }.join()

                                this.launch {
                                    routeUseCase.setRemoteObjectIdPhoto(
                                        photo.photoId,
                                        photoObject.objectId
                                    ).collect{ resultSetId ->
                                        if (resultSetId is ResultState.Success){
                                            job?.cancelChildren()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "ex photo save = $e")
            return@withContext Result.retry()
        }
    }
}