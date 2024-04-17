package com.z_company.work_manager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseRelation
import com.parse.ParseUser
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val BASIC_DATA_INPUT_KEY = "BASIC_DATA_INPUT_KEY"
const val OBJECT_ID_INPUT_KEY = "OBJECT_ID_INPUT_KEY"

class SaveBasicDataWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val value = inputData.getString(BASIC_DATA_INPUT_KEY)
        val objectId = inputData.getString(OBJECT_ID_INPUT_KEY) ?: ""
        val basicData = BasicDataJSONConverter.fromString(value!!)

        val basicDataObject = ParseObject(BASIC_DATA_CLASS_NAME_REMOTE)
        if (objectId.isNotEmpty()) {
            basicDataObject.objectId = objectId
        }
        try {
            val userId = ParseUser.getCurrentUser()

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
            relation.add(userId)

            basicDataObject.saveInBackground {
                if (it == null) {
                    routeUseCase.isSynchronizedRoute(basicData.id, basicDataObject.objectId)
                        .launchIn(CoroutineScope(Dispatchers.IO))
                }
            }
            return@withContext Result.success()
        } catch (e: Exception) {
            return@withContext Result.retry()
        }
    }
}