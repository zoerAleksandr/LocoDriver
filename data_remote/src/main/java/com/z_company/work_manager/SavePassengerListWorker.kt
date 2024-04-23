package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseRelation
import com.parse.coroutines.suspendSave
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.type_converter.PassengerJSONConverter
import com.z_company.work_manager.PassengerFieldName.BASIC_DATA_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.NOTES_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.PASSENGER_CLASS_NAME_REMOTE
import com.z_company.work_manager.PassengerFieldName.PASSENGER_ID_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.PASSENGER_BASIC_ID_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.PASSENGER_TRAIN_NUMBER_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.STATION_ARRIVAL_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.STATION_DEPARTURE_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.TIME_ARRIVAL_FIELD_NAME
import com.z_company.work_manager.PassengerFieldName.TIME_DEPARTURE_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val PASSENGERS_INPUT_KEY = "PASSENGERS_INPUT_KEY"

class SavePassengerListWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val basicDataObjectId = inputData.getString(BASIC_DATA_OBJECT_ID_KEY)
            val basicDataObject = ParseObject(BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE)
            basicDataObject.objectId = basicDataObjectId
            val value = inputData.getStringArray(PASSENGERS_INPUT_KEY)
            value?.forEach { passengerJSON ->
                val passengerObject = ParseObject(PASSENGER_CLASS_NAME_REMOTE)
                val passenger = PassengerJSONConverter.fromString(passengerJSON)
                if (!passenger.remoteObjectId.isNullOrEmpty()){
                    passengerObject.objectId = passenger.remoteObjectId
                }

                passengerObject.put(PASSENGER_ID_FIELD_NAME, passenger.passengerId)
                passengerObject.put(PASSENGER_BASIC_ID_FIELD_NAME, passenger.basicId)
                passenger.trainNumber?.let { number ->
                    passengerObject.put(PASSENGER_TRAIN_NUMBER_FIELD_NAME, number)
                }
                passenger.stationDeparture?.let { station ->
                    passengerObject.put(STATION_DEPARTURE_FIELD_NAME, station)
                }
                passenger.stationArrival?.let { station ->
                    passengerObject.put(STATION_ARRIVAL_FIELD_NAME, station)
                }
                passenger.timeDeparture?.let { time ->
                    passengerObject.put(TIME_DEPARTURE_FIELD_NAME, time)
                }
                passenger.timeArrival?.let { time ->
                    passengerObject.put(TIME_ARRIVAL_FIELD_NAME, time)
                }
                passenger.notes?.let { notes ->
                    passengerObject.put(NOTES_FIELD_NAME, notes)
                }
                val basicDataRelation: ParseRelation<ParseObject> =
                    passengerObject.getRelation(BASIC_DATA_FIELD_NAME)
                basicDataRelation.add(basicDataObject)
                this.launch {
                    passengerObject.suspendSave()
                }.join()
                routeUseCase.setRemoteObjectIdPassenger(passenger.passengerId, passengerObject.objectId)
                    .launchIn(this)
            }
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "ex passenger save = $e")
            return@withContext Result.retry()
        }
    }

}