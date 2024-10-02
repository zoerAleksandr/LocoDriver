package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.coroutines.suspendFind
import com.z_company.entity.Station
import com.z_company.entity.Train
import com.z_company.type_converter.StationJSONConverter
import com.z_company.type_converter.TrainJSONConverter
import com.z_company.work_manager.TrainFieldName.AXLE_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.LENGTH_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.STATIONS_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.TRAIN_BASIC_ID_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.TRAIN_CLASS_NAME_REMOTE
import com.z_company.work_manager.TrainFieldName.TRAIN_DISTANCE_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.TRAIN_ID_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.TRAIN_NUMBER_FIELD_NAME
import com.z_company.work_manager.TrainFieldName.WEIGHT_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


const val LOAD_TRAIN_WORKER_INPUT_KEY = "LOAD_TRAIN_WORKER_INPUT_KEY"
const val LOAD_TRAIN_WORKER_OUTPUT_KEY = "LOAD_TRAIN_WORKER_OUTPUT_KEY"

class LoadTrainFromRemoteWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val basicId = inputData.getString(LOAD_TRAIN_WORKER_INPUT_KEY)
            val query = ParseQuery<ParseObject>(TRAIN_CLASS_NAME_REMOTE)
            query.whereEqualTo(TRAIN_BASIC_ID_FIELD_NAME, basicId)
            val trainRemoteList = query.suspendFind()
            val trainList: MutableList<Train> = mutableListOf()
            trainRemoteList.forEach { parseObject ->
                var train = Train()
                parseObject.apply {
                    getString(TRAIN_ID_FIELD_NAME)?.let { id ->
                        train = train.copy(trainId = id)
                    }
                    getString(TRAIN_BASIC_ID_FIELD_NAME)?.let { basicId ->
                        train = train.copy(basicId = basicId)
                    }
                    train = train.copy(remoteObjectId = parseObject.objectId)
                    getString(TRAIN_NUMBER_FIELD_NAME)?.let { number ->
                        train = train.copy(number = number)
                    }
                    getString(TRAIN_DISTANCE_FIELD_NAME)?.let { distance ->
                        train = train.copy(distance = distance)
                    }
                    getString(WEIGHT_FIELD_NAME)?.let { weight ->
                        train = train.copy(weight = weight)
                    }
                    getString(AXLE_FIELD_NAME)?.let { axle ->
                        train = train.copy(axle = axle)
                    }
                    getString(LENGTH_FIELD_NAME)?.let { length ->
                        train = train.copy(conditionalLength = length)
                    }
                    getJSONArray(STATIONS_FIELD_NAME)?.let { stationsJSONArray ->
                        val stationsList: MutableList<Station> = mutableListOf()
                        val size = stationsJSONArray.length()
                        if (size > 0) {
                            for (i in 0 until size) {
                                val stationJSON = stationsJSONArray[i]
                                stationsList.add(StationJSONConverter.fromString(stationJSON.toString()))
                            }
                        }
                        train = train.copy(stations = stationsList)
                    }
                    trainList.add(train)
                }
            }
            val jsonList: Array<String> = trainList.map {
                TrainJSONConverter.toString(it)
            }.toTypedArray()

            val data = Data.Builder().putStringArray(LOAD_TRAIN_WORKER_OUTPUT_KEY, jsonList)
            return@withContext Result.success(data.build())
        } catch (e: Exception) {
            Log.d("ZZZ", "ex load train = ${e.message}")
            return@withContext Result.failure()
        }
    }
}