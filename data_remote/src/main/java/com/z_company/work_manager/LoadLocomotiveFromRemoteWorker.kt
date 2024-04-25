package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.coroutines.suspendFind
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.util.ifNotZero
import com.z_company.entity.Locomotive
import com.z_company.entity.SectionDiesel
import com.z_company.entity.SectionElectric
import com.z_company.type_converter.LocomotiveJSONConverter
import com.z_company.type_converter.SectionDieselJSONConverter
import com.z_company.type_converter.SectionElectricJSONConverter
import com.z_company.work_manager.LocomotiveFieldName.DIESEL_SECTIONS_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.ELECTRIC_SECTIONS_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.LOCOMOTIVE_BASIC_ID_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.LOCOMOTIVE_CLASS_NAME_REMOTE
import com.z_company.work_manager.LocomotiveFieldName.LOCOMOTIVE_NUMBER_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.LOCOMOTIVE_UID_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.SERIES_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.TIME_END_ACCEPTED_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.TIME_END_DELIVERY_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.TIME_START_ACCEPTED_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.TIME_START_DELIVERY_FIELD_NAME
import com.z_company.work_manager.LocomotiveFieldName.TYPE_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val LOAD_LOCOMOTIVE_WORKER_INPUT_KEY = "LOAD_LOCOMOTIVE_WORKER_INPUT_KEY"
const val LOAD_LOCOMOTIVE_WORKER_OUTPUT_KEY = "LOAD_LOCOMOTIVE_WORKER_OUTPUT_KEY"

class LoadLocomotiveFromRemoteWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val basicId = inputData.getString(LOAD_LOCOMOTIVE_WORKER_INPUT_KEY)
            val query = ParseQuery<ParseObject>(LOCOMOTIVE_CLASS_NAME_REMOTE)
            query.whereEqualTo(LOCOMOTIVE_BASIC_ID_FIELD_NAME, basicId)
            val locomotiveRemoteList = query.suspendFind()
            val locomotiveList: MutableList<Locomotive> = mutableListOf()
            locomotiveRemoteList.forEach { parseObject ->
                var locomotive = Locomotive()
                parseObject.apply {
                    getString(LOCOMOTIVE_UID_FIELD_NAME)?.let { id ->
                        locomotive = locomotive.copy(locoId = id)
                    }
                    getString(LOCOMOTIVE_BASIC_ID_FIELD_NAME)?.let { basicId ->
                        locomotive = locomotive.copy(basicId = basicId)
                    }
                    locomotive = locomotive.copy(removeObjectId = objectId)
                    getString(SERIES_FIELD_NAME)?.let { series ->
                        locomotive = locomotive.copy(series = series)
                    }
                    getString(LOCOMOTIVE_NUMBER_FIELD_NAME)?.let { number ->
                        locomotive = locomotive.copy(series = number)
                    }
                    getString(TYPE_FIELD_NAME)?.let { typeString ->
                        val type: LocoType = when (typeString) {
                            LocoType.ELECTRIC.name -> LocoType.ELECTRIC
                            LocoType.DIESEL.name -> LocoType.DIESEL
                            else -> LocoType.ELECTRIC
                        }
                        locomotive = locomotive.copy(type = type)
                    }
                    getJSONArray(ELECTRIC_SECTIONS_FIELD_NAME)?.let { electricSectionsJSONArray ->
                        val electricSectionList: MutableList<SectionElectric> = mutableListOf()
                        val size = electricSectionsJSONArray.length()
                        if (size > 0) {
                            for (i in 0 until size) {
                                val electricSectionsJSON =
                                    electricSectionsJSONArray[i]

                                electricSectionList.add(
                                    SectionElectricJSONConverter.fromString(
                                        electricSectionsJSON.toString()
                                    )
                                )
                            }
                        }
                        locomotive = locomotive.copy(electricSectionList = electricSectionList)
                    }

                    getJSONArray(DIESEL_SECTIONS_FIELD_NAME)?.let { dieselSectionsJSONArray ->
                        val dieselSectionList: MutableList<SectionDiesel> = mutableListOf()
                        val size = dieselSectionsJSONArray.length()
                        if (size > 0) {
                            for (i in 0 until size) {
                                val dieselSectionsJSON: String =
                                    dieselSectionsJSONArray[i] as String
                                dieselSectionList.add(
                                    SectionDieselJSONConverter.fromString(
                                        dieselSectionsJSON
                                    )
                                )
                            }
                        }
                        locomotive = locomotive.copy(dieselSectionList = dieselSectionList)
                    }
                    getLong(TIME_START_ACCEPTED_FIELD_NAME).ifNotZero().let { time ->
                        locomotive = locomotive.copy(timeStartOfAcceptance = time)
                    }
                    getLong(TIME_END_ACCEPTED_FIELD_NAME).ifNotZero().let { time ->
                        locomotive = locomotive.copy(timeEndOfAcceptance = time)
                    }
                    getLong(TIME_START_DELIVERY_FIELD_NAME).ifNotZero().let { time ->
                        locomotive = locomotive.copy(timeStartOfDelivery = time)
                    }
                    getLong(TIME_END_DELIVERY_FIELD_NAME).ifNotZero().let { time ->
                        locomotive = locomotive.copy(timeEndOfDelivery = time)
                    }
                    locomotiveList.add(locomotive)
                }
            }
            val jsonList: Array<String> = locomotiveList.map {
                LocomotiveJSONConverter.toString(it)
            }.toTypedArray()
            val data = Data.Builder().putStringArray(LOAD_LOCOMOTIVE_WORKER_OUTPUT_KEY, jsonList)
            return@withContext Result.success(data.build())
        } catch (e: Exception) {
            Log.d("ZZZ", "ex load loco = ${e.message}")
            return@withContext Result.retry()
        }
    }
}