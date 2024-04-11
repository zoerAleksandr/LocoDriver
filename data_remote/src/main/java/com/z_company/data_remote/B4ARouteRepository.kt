package com.z_company.data_remote

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.z_company.converter.BasicDataConverter
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.repositories.RemoteRouteRepository
import com.z_company.work_manager.SaveBasicDataWorker
import com.z_company.work_manager.SyncBasicDataWorker

private const val SAVE_ROUTE_WORKER_TAG = "SAVE_ROUTE_WORKER_TAG"
private const val SYNC_DATA_WORKER_TAG = "SYNC_DATA_WORKER_TAG"

const val BASIC_DATA_INPUT = "BASIC_DATA_INPUT"

/**
 * проверять время обновления последнего файла в remote и сравнивать со временем обновления
 * последнего файла local если время меньше записывать его в remote
 **/
class B4ARouteRepository(private val context: Context) : RemoteRouteRepository {
    override fun saveBasicData(basicData: BasicData) {
        val basicDataJSON = BasicDataConverter.toString(basicData)

        val inputData = Data.Builder()
            .putString(BASIC_DATA_INPUT, basicDataJSON)
            .build()

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val worker = OneTimeWorkRequestBuilder<SaveBasicDataWorker>()
            .setInputData(inputData)
            .addTag(SAVE_ROUTE_WORKER_TAG)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(worker)
    }

    override fun getAllBasicData() {
        val parseQuery: ParseQuery<ParseObject> = ParseQuery("BasicData")
        parseQuery.whereEqualTo("user", ParseUser.getCurrentUser())
        parseQuery.find().forEach {
            Log.d("ZZZ", it.objectId)
        }
    }

    override fun syncData() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val worker = OneTimeWorkRequestBuilder<SyncBasicDataWorker>()
            .addTag(SYNC_DATA_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
    }
}