package com.z_company.work_manager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseRelation
import com.parse.ParseUser
import com.parse.coroutines.suspendSave
import com.z_company.use_case.RemoteRouteUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.type_converter.BasicDataJSONConverter
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_UID_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.NUMBER_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.TIME_START_WORK_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.TIME_END_WORK_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.REST_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.NOTES_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.USER_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val BASIC_DATA_INPUT_KEY = "BASIC_DATA_INPUT_KEY"
const val BASIC_DATA_OBJECT_ID_KEY = "BASIC_DATA_OBJECT_ID_KEY"

class SaveBasicDataWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val remoteRouteUseCase: RemoteRouteUseCase by inject()
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val value = inputData.getString(BASIC_DATA_INPUT_KEY)
        val basicData = BasicDataJSONConverter.fromString(value!!)

        val basicDataObject = ParseObject(BASIC_DATA_CLASS_NAME_REMOTE)
        if (basicData.remoteObjectId.isNotEmpty()) {
            basicDataObject.objectId = basicData.remoteObjectId
        }
        try {
            val currentUser = ParseUser.getCurrentUser()
            basicData.remoteObjectId
            basicDataObject.put(BASIC_DATA_UID_FIELD_NAME, basicData.id)
            basicData.number?.let { number ->
                basicDataObject.put(NUMBER_FIELD_NAME, number)
            }
            basicData.timeStartWork?.let { time ->
                basicDataObject.put(TIME_START_WORK_FIELD_NAME, time)
            }
            basicData.timeEndWork?.let { time ->
                basicDataObject.put(TIME_END_WORK_FIELD_NAME, time)
            }
            basicDataObject.put(REST_FIELD_NAME, basicData.restPointOfTurnover)
            basicData.notes?.let { notes ->
                basicDataObject.put(NOTES_FIELD_NAME, notes)
            }
            val relation: ParseRelation<ParseUser> = basicDataObject.getRelation(USER_FIELD_NAME)
            relation.add(currentUser)

            this.launch {
                basicDataObject.suspendSave()
            }.join()

            routeUseCase.setRemoteObjectIdBasicData(basicData.id, basicDataObject.objectId)
                .launchIn(this)

            val data = Data.Builder().putString(BASIC_DATA_OBJECT_ID_KEY, basicDataObject.objectId)
            return@withContext Result.success(data.build())
        } catch (e: Exception) {
            return@withContext Result.retry()
        }
    }
}