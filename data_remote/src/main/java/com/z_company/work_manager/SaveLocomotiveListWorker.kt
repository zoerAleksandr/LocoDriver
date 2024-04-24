package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseRelation
import com.parse.coroutines.suspendSave
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.type_converter.LocomotiveJSONConverter
import com.z_company.type_converter.SectionDieselJSONConverter
import com.z_company.type_converter.SectionElectricJSONConverter
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE
import com.z_company.work_manager.LocomotiveFieldName.BASIC_DATA_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.DIESEL_SECTIONS_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.ELECTRIC_SECTIONS_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.LOCOMOTIVE_BASIC_ID_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.LOCOMOTIVE_CLASS_NAME_REMOTE
import com.z_company.work_manager.LocomotiveFieldName.LOCOMOTIVE_UID_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.LOCOMOTIVE_NUMBER_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.SERIES_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.TIME_END_ACCEPTED_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.TIME_END_DELIVERY_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.TIME_START_ACCEPTED_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.TIME_START_DELIVERY_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.TYPE_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val LOCOMOTIVE_INPUT_KEY = "LOCOMOTIVE_INPUT_DATA"

class SaveLocomotiveListWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val basicDataObjectId = inputData.getString(BASIC_DATA_OBJECT_ID_KEY)
            val basicDataObject = ParseObject(BASIC_DATA_CLASS_NAME_REMOTE)
            basicDataObject.objectId = basicDataObjectId

            val value = inputData.getStringArray(LOCOMOTIVE_INPUT_KEY)
            value?.forEach { locomotiveJSON ->
                val locomotiveObject = ParseObject(LOCOMOTIVE_CLASS_NAME_REMOTE)
                val locomotive = LocomotiveJSONConverter.fromString(locomotiveJSON)

                if (locomotive.removeObjectId.isNotEmpty()) {
                    locomotiveObject.objectId = locomotive.removeObjectId
                }

                locomotiveObject.put(LOCOMOTIVE_UID_FIELD_NAME, locomotive.locoId)

                locomotiveObject.put(LOCOMOTIVE_BASIC_ID_FIELD_NAME, locomotive.basicId)

                locomotive.series?.let { series ->
                    locomotiveObject.put(SERIES_FIELD_NAME, series)
                }

                locomotive.number?.let { number ->
                    locomotiveObject.put(LOCOMOTIVE_NUMBER_FIELD_NAME, number)
                }

                locomotiveObject.put(TYPE_FIELD_NAME, locomotive.type.name)

                val electricSectionArray = JSONArray()
                locomotive.electricSectionList.forEach { sectionElectric ->
                    val jsonObject = JSONObject(SectionElectricJSONConverter.toString(sectionElectric))
                    electricSectionArray.put(jsonObject)
                }
                locomotiveObject.put(ELECTRIC_SECTIONS_FIELD_NAME, electricSectionArray)

                val dieselSectionArray = JSONArray()
                locomotive.dieselSectionList.forEach { sectionDiesel ->
                    val jsonObject = JSONObject(SectionDieselJSONConverter.toString(sectionDiesel))
                    dieselSectionArray.put(jsonObject)
                }
                locomotiveObject.put(DIESEL_SECTIONS_FIELD_NAME, dieselSectionArray)

                locomotive.timeStartOfAcceptance?.let { time ->
                    locomotiveObject.put(TIME_START_ACCEPTED_FIELD_NAME, time)
                }
                locomotive.timeEndOfAcceptance?.let { time ->
                    locomotiveObject.put(TIME_END_ACCEPTED_FIELD_NAME, time)
                }
                locomotive.timeStartOfDelivery?.let { time ->
                    locomotiveObject.put(TIME_START_DELIVERY_FIELD_NAME, time)
                }
                locomotive.timeEndOfDelivery?.let { time ->
                    locomotiveObject.put(TIME_END_DELIVERY_FIELD_NAME, time)
                }
                val basicDataRelation: ParseRelation<ParseObject> =
                    locomotiveObject.getRelation(BASIC_DATA_FIELD_NAME)
                basicDataRelation.add(basicDataObject)

                this.launch {
                    locomotiveObject.suspendSave()
                }.join()
                routeUseCase.setRemoteObjectIdLocomotive(locomotive.locoId, locomotiveObject.objectId)
                    .launchIn(this)
            }
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "e = ${e}")
            return@withContext Result.retry()
        }
    }
}