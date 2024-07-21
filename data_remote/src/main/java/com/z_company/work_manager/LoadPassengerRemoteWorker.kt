package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.coroutines.suspendFind
import com.z_company.domain.util.ifNotZero
import com.z_company.entity.Passenger
import com.z_company.type_converter.PassengerJSONConverter
import com.z_company.work_manager.PassengerFieldName.NOTES_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.PASSENGER_BASIC_ID_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.PASSENGER_CLASS_NAME_REMOTE
import com.z_company.work_manager.PassengerFieldName.PASSENGER_ID_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.PASSENGER_TRAIN_NUMBER_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.STATION_ARRIVAL_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.STATION_DEPARTURE_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.TIME_ARRIVAL_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.TIME_DEPARTURE_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


const val LOAD_PASSENGER_WORKER_INPUT_KEY = "LOAD_PASSENGER_WORKER_INPUT_KEY"
const val LOAD_PASSENGER_WORKER_OUTPUT_KEY = "LOAD_PASSENGER_WORKER_OUTPUT_KEY"

class LoadPassengerRemoteWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val basicId = inputData.getString(LOAD_PASSENGER_WORKER_INPUT_KEY)
            val query = ParseQuery<ParseObject>(PASSENGER_CLASS_NAME_REMOTE)
            query.whereEqualTo(PASSENGER_BASIC_ID_FIELD_NAME, basicId)
            val passengerRemoteList = query.suspendFind()
            val passengerList: MutableList<Passenger> = mutableListOf()
            passengerRemoteList.forEach { parseObject ->
                var passenger = Passenger()
                parseObject.apply {
                    getString(PASSENGER_ID_FIELD_NAME)?.let { id ->
                        passenger = passenger.copy(passengerId = id)
                    }
                    getString(PASSENGER_BASIC_ID_FIELD_NAME)?.let { basicId ->
                        passenger = passenger.copy(basicId = basicId)
                    }
                    passenger = passenger.copy(remoteObjectId = parseObject.objectId)
                    getString(PASSENGER_TRAIN_NUMBER_FIELD_NAME)?.let { number ->
                        passenger = passenger.copy(trainNumber = number)
                    }
                    getString(STATION_DEPARTURE_FIELD_NAME)?.let { station ->
                        passenger = passenger.copy(stationDeparture = station)
                    }
                    getString(STATION_ARRIVAL_FIELD_NAME)?.let { station ->
                        passenger = passenger.copy(stationArrival = station)
                    }
                    getLong(TIME_ARRIVAL_FIELD_NAME).ifNotZero()?.let { time ->
                        passenger = passenger.copy(timeArrival = time)
                    }
                    getLong(TIME_DEPARTURE_FIELD_NAME).ifNotZero()?.let { time ->
                        passenger = passenger.copy(timeDeparture = time)
                    }
                    getString(NOTES_FIELD_NAME)?.let { notes ->
                        passenger = passenger.copy(notes = notes)
                    }
                }
                passengerList.add(passenger)
            }
            val jsonList: Array<String> = passengerList.map {
                PassengerJSONConverter.toString(it)
            }.toTypedArray()
            val data = Data.Builder().putStringArray(LOAD_PASSENGER_WORKER_OUTPUT_KEY, jsonList)
            return@withContext Result.success(data.build())
        } catch (e: Exception) {
            Log.d("ZZZ", "ex load passenger = ${e.message}")
            return@withContext Result.retry()
        }
    }

}