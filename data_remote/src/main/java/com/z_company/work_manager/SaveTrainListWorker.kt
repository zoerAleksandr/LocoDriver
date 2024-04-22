package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseRelation
import com.parse.ParseUser
import com.parse.coroutines.suspendSave
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.entity_converter.TrainJSONConverter
import com.z_company.type_converter.StationJSONConverter
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE
import com.z_company.work_manager.TrainFieldName.AXLE_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.BASIC_DATA_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.LENGTH_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.STATIONS_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.TRAIN_BASIC_ID_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.TRAIN_CLASS_NAME_REMOTE
import com.z_company.work_manager.TrainFieldName.TRAIN_ID_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.TRAIN_NUMBER_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.WEIGHT_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val TRAINS_INPUT_KEY = "TRAINS_INPUT_KEY"

class SaveTrainListWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val basicDataObjectId = inputData.getString(BASIC_DATA_OBJECT_ID_KEY)
            val basicDataObject = ParseObject(BASIC_DATA_CLASS_NAME_REMOTE)
            basicDataObject.objectId = basicDataObjectId
            val value = inputData.getStringArray(TRAINS_INPUT_KEY)
            value?.forEach { trainJSON ->
                val trainObject = ParseObject(TRAIN_CLASS_NAME_REMOTE)
                val train = TrainJSONConverter.fromString(trainJSON)

                if (train.remoteObjectId.isNotEmpty()) {
                    trainObject.objectId = train.remoteObjectId
                }
                trainObject.put(TRAIN_ID_FIELD_NAME, train.trainId)
                trainObject.put(TRAIN_BASIC_ID_FIELD_NAME, train.basicId)
                train.number?.let { number ->
                    trainObject.put(TRAIN_NUMBER_FIELD_NAME, number)
                }
                train.weight?.let { weight ->
                    trainObject.put(WEIGHT_FIELD_NAME, weight)
                }
                train.axle?.let { axle ->
                    trainObject.put(AXLE_FIELD_NAME, axle)
                }
                train.conditionalLength?.let { length ->
                    trainObject.put(LENGTH_FIELD_NAME, length)
                }
                val stationsArray = JSONArray()
                train.stations.forEach { station ->
                    val jsonObject = JSONObject(StationJSONConverter.toString(station))
                    stationsArray.put(jsonObject)
                }
                trainObject.put(STATIONS_FIELD_NAME, stationsArray)

                val basicDataRelation: ParseRelation<ParseObject> =
                    trainObject.getRelation(BASIC_DATA_FIELD_NAME)
                basicDataRelation.add(basicDataObject)
                this.launch {
                    trainObject.suspendSave()
                }.join()
                routeUseCase.setRemoteObjectIdTrain(train.trainId, trainObject.objectId)
                    .launchIn(this)
            }
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "ex train save = $e")
            return@withContext Result.retry()
        }
    }
}