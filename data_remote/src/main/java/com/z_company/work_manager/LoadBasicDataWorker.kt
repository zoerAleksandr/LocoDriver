package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseQuery
import com.z_company.domain.util.ifNotZero
import com.z_company.entity.BasicData
import com.z_company.type_converter.BasicDataJSONConverter
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_UID_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val LOAD_BASIC_DATA_ID_INPUT_KEY = "LOAD_BASIC_DATA_ID_INPUT_KEY"
const val LOAD_BASIC_DATA_ID_OUTPUT_KEY = "LOAD_BASIC_DATA_ID_OUTPUT_KEY"

class LoadBasicDataWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val basicId = inputData.getString(LOAD_BASIC_DATA_ID_INPUT_KEY)
            val parseQuery: ParseQuery<ParseObject> =
                ParseQuery(BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE)
            parseQuery.whereEqualTo(BASIC_DATA_UID_FIELD_NAME, basicId)
            val parseObject = parseQuery.find().first()

            if (parseObject != null) {
                var basicData = BasicData()
                parseObject.apply {
                    getString(BASIC_DATA_UID_FIELD_NAME)?.let { id ->
                        basicData = basicData.copy(id = id, isSynchronized = true)
                    }
                    getString(BasicDataFieldName.NUMBER_FIELD_NAME)?.let { number ->
                        basicData = basicData.copy(number = number)
                    }
                    basicData = basicData.copy(updatedAt = updatedAt)
                    basicData = basicData.copy(remoteObjectId = objectId)
                    getLong(BasicDataFieldName.TIME_START_WORK_FIELD_NAME).ifNotZero()
                        .let { timeStart ->
                            basicData = basicData.copy(timeStartWork = timeStart)
                        }
                    getLong(BasicDataFieldName.TIME_END_WORK_FIELD_NAME).ifNotZero()
                        .let { timeStart ->
                            basicData = basicData.copy(timeEndWork = timeStart)
                        }
                    getBoolean(BasicDataFieldName.REST_FIELD_NAME).let { rest ->
                        basicData = basicData.copy(restPointOfTurnover = rest)
                    }
                    getString(BasicDataFieldName.NOTES_FIELD_NAME)?.let { notes ->
                        basicData = basicData.copy(notes = notes)
                    }

                }
                val basicDataAsString = BasicDataJSONConverter.toString(basicData)
                val data =
                    Data.Builder().putString(LOAD_BASIC_DATA_ID_OUTPUT_KEY, basicDataAsString)
                return@withContext Result.success(data.build())
            } else {
                return@withContext Result.success()
            }
        } catch (e: Exception) {
            Log.d("ZZZ", "LoadBasicDataWorker ex = ${e.message}")
            return@withContext Result.failure()
        }
    }
}