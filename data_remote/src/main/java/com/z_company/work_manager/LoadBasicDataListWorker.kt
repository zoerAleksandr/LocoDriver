package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.parse.coroutines.suspendFind
import com.z_company.domain.util.ifNotZero
import com.z_company.type_converter.BasicDataJSONConverter
import com.z_company.entity.BasicData
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE
import com.z_company.work_manager.BasicDataFieldName.NOTES_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.NUMBER_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.REST_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.TIME_END_WORK_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.TIME_START_WORK_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_UID_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.USER_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

const val GET_BASIC_DATA_WORKER_OUTPUT_KEY = "GET_BASIC_DATA_WORKER_OUTPUT_KEY"

class LoadBasicDataListWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val parseQuery: ParseQuery<ParseObject> = ParseQuery(BASIC_DATA_CLASS_NAME_REMOTE)
            parseQuery.whereEqualTo(USER_FIELD_NAME, ParseUser.getCurrentUser())
            parseQuery.orderByDescending(BASIC_DATA_UID_FIELD_NAME)
            val remoteList = parseQuery.suspendFind()
            val basicDataList: MutableList<BasicData> = mutableListOf()

            remoteList.forEach { parseObject ->
                var basicData = BasicData()
                parseObject.apply {
                    getString(BASIC_DATA_UID_FIELD_NAME)?.let { id ->
                        basicData = basicData.copy(id = id, isSynchronized = true)
                    }
                    getString(NUMBER_FIELD_NAME)?.let { number ->
                        basicData = basicData.copy(number = number)
                    }
                    basicData = basicData.copy(updatedAt = updatedAt)
                    basicData = basicData.copy(remoteObjectId = objectId)
                    getLong(TIME_START_WORK_FIELD_NAME).ifNotZero().let { timeStart ->
                        basicData = basicData.copy(timeStartWork = timeStart)
                    }
                    getLong(TIME_END_WORK_FIELD_NAME).ifNotZero().let { timeStart ->
                        basicData = basicData.copy(timeEndWork = timeStart)
                    }
                    getBoolean(REST_FIELD_NAME).let { rest ->
                        basicData = basicData.copy(restPointOfTurnover = rest)
                    }
                    getString(NOTES_FIELD_NAME)?.let { notes ->
                        basicData = basicData.copy(notes = notes)
                    }
                }
                basicDataList.add(basicData)
            }
            val stringList: Array<String> = basicDataList.map {
                BasicDataJSONConverter.toString(it)
            }.toTypedArray()

            val data = Data.Builder().putStringArray(GET_BASIC_DATA_WORKER_OUTPUT_KEY, stringList)

            return@withContext Result.success(data.build())
        } catch (e: Exception) {
            Log.d("ZZZ", "ex sync = ${e.message}")
            return@withContext Result.retry()
        }
    }
}