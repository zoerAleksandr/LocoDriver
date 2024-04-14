package com.z_company.data_remote

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.z_company.converter.BasicDataConverter
import com.z_company.domain.entities.route.BasicData
import com.z_company.work_manager.GET_BASIC_DATA_WORKER_OUTPUT_KEY
import com.z_company.work_manager.SaveBasicDataWorker
import com.z_company.work_manager.GetBasicDataListWorker

private const val SAVE_ROUTE_WORKER_TAG = "SAVE_ROUTE_WORKER_TAG"
private const val SYNC_DATA_WORKER_TAG = "SYNC_DATA_WORKER_TAG"

const val BASIC_DATA_INPUT = "BASIC_DATA_INPUT"

/**
 * проверять время обновления последнего файла в remote и сравнивать со временем обновления
 * последнего файла local если время меньше записывать его в remote
 **/
class B4ARouteRepository(private val context: Context) : RemoteRouteRepository {
    override suspend fun saveBasicData(basicData: BasicData) {
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

        WorkManager.getInstance(context)
            .getWorkInfoByIdFlow(worker.id)
            .collect {
                when (it.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        it.outputData
                        Log.d("ZZZ", "WorkInfo.State.SUCCEEDED")
                    }

                    WorkInfo.State.RUNNING -> {
                        Log.d("ZZZ", "WorkInfo.State.RUNNING")
                    }

                    WorkInfo.State.ENQUEUED -> {
                        Log.d("ZZZ", "WorkInfo.State.ENQUEUED")
                    }

                    WorkInfo.State.BLOCKED -> {
                        Log.d("ZZZ", "WorkInfo.State.BLOCKED")
                    }

                    WorkInfo.State.FAILED -> {
                        Log.d("ZZZ", "WorkInfo.State.FAILED")
                    }

                    WorkInfo.State.CANCELLED -> {
                        Log.d("ZZZ", "WorkInfo.State.CANCELLED")
                    }
                }
            }
    }

    override suspend fun getAllBasicData() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val worker = OneTimeWorkRequestBuilder<GetBasicDataListWorker>()
            .addTag(SYNC_DATA_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)

        WorkManager.getInstance(context)
            .getWorkInfoByIdFlow(worker.id)
            .collect {
                when (it.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        it.outputData
                        Log.d("ZZZ", "WorkInfo.State.SUCCEEDED")
                        val basicDataJsonArray =
                            it.outputData.getStringArray(GET_BASIC_DATA_WORKER_OUTPUT_KEY)
                        basicDataJsonArray?.forEach { basicDataJson ->
                            BasicDataConverter.fromString(basicDataJson)
                        }
                    }

                    WorkInfo.State.RUNNING -> {
                        Log.d("ZZZ", "WorkInfo.State.RUNNING")
                    }

                    WorkInfo.State.ENQUEUED -> {
                        Log.d("ZZZ", "WorkInfo.State.ENQUEUED")
                    }

                    WorkInfo.State.BLOCKED -> {
                        Log.d("ZZZ", "WorkInfo.State.BLOCKED")
                    }

                    WorkInfo.State.FAILED -> {
                        Log.d("ZZZ", "WorkInfo.State.FAILED")
                    }

                    WorkInfo.State.CANCELLED -> {
                        Log.d("ZZZ", "WorkInfo.State.CANCELLED")
                    }
                }
            }
    }
}