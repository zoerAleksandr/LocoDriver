package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_UID_FIELD_NAME
import com.z_company.work_manager.BasicDataFieldName.USER_FIELD_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

const val GET_BASIC_DATA_WORKER_OUTPUT_KEY = "GET_BASIC_DATA_WORKER_OUTPUT_KEY"
class LoadBasicDataListWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val parseQuery: ParseQuery<ParseObject> = ParseQuery(BASIC_DATA_CLASS_NAME_REMOTE)
            parseQuery.whereEqualTo(USER_FIELD_NAME, ParseUser.getCurrentUser())
            parseQuery.orderByDescending(BASIC_DATA_UID_FIELD_NAME)
            val basicDataIdList: MutableList<String> = mutableListOf()
            val remoteList = parseQuery.findInBackground { parseObjects, parseException ->
                parseObjects.forEach { parseObject ->
                    parseObject.apply {
                        getString(BASIC_DATA_UID_FIELD_NAME)?.let { id ->
                            basicDataIdList.add(id)
                        }
                    }
                }
            }

            val data = Data.Builder().putStringArray(GET_BASIC_DATA_WORKER_OUTPUT_KEY, basicDataIdList.toTypedArray())
            return@withContext Result.success(data.build())
        } catch (e: Exception) {
            Log.d("ZZZ", "ex sync load basic data = ${e.message}")
            return@withContext Result.retry()
        }
    }
}