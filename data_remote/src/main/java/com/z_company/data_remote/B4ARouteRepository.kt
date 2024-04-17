package com.z_company.data_remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.z_company.type_converter.BasicDataJSONConverter
import com.z_company.core.ResultState
import com.z_company.entity.BasicData
import com.z_company.work_manager.BASIC_DATA_INPUT_KEY
import com.z_company.work_manager.GET_BASIC_DATA_WORKER_OUTPUT_KEY
import com.z_company.work_manager.SaveBasicDataWorker
import com.z_company.work_manager.GetBasicDataListWorker
import com.z_company.work_manager.OBJECT_ID_INPUT_KEY
import com.z_company.work_manager.REMOVE_OBJECT_ID_KEY
import com.z_company.work_manager.RemoveBasicDataWorker
import com.z_company.work_manager.SynchronizedWorker
import com.z_company.work_manager.WorkManagerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit

private const val SAVE_ROUTE_WORKER_TAG = "SAVE_ROUTE_WORKER_TAG"
private const val GET_ALL_DATA_WORKER_TAG = "SYNC_DATA_WORKER_TAG"
private const val SYNC_DATA_WORKER_TAG = "SYNC_DATA_WORKER_TAG"
private const val REMOVE_ROUTE_WORKER_TAG = "REMOVE_ROUTE_WORKER_TAG"

/**
 * проверять время обновления последнего файла в remote и сравнивать со временем обновления
 * последнего файла local если время меньше записывать его в remote
 **/
class B4ARouteRepository(private val context: Context) : RemoteRouteRepository {
    override suspend fun saveBasicData(basicData: BasicData): Flow<ResultState<Data>> {
        val basicDataJSON = BasicDataJSONConverter.toString(basicData)

        val inputData = Data.Builder()
            .putString(OBJECT_ID_INPUT_KEY, basicData.remoteObjectId)
            .putString(BASIC_DATA_INPUT_KEY, basicDataJSON)
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
        return WorkManagerState.state(context, worker.id)
    }

    override suspend fun getAllBasicData(): Flow<ResultState<List<BasicData>?>> {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val worker = OneTimeWorkRequestBuilder<GetBasicDataListWorker>()
            .addTag(GET_ALL_DATA_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return flow {
            WorkManagerState.state(context, worker.id)
                .collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val stringList = result.data
                                .getStringArray(GET_BASIC_DATA_WORKER_OUTPUT_KEY)
                            val basicDataList = stringList?.map {
                                BasicDataJSONConverter.fromString(it)
                            }
                            emit(ResultState.Success(basicDataList))
                        }

                        is ResultState.Loading -> {
                            emit(result)
                        }

                        is ResultState.Error -> {
                            emit(result)
                        }
                    }
                }
        }
    }

    override suspend fun removeBasicData(remoteObjectId: String): Flow<ResultState<Data>> {
        val inputData = Data.Builder()
            .putString(REMOVE_OBJECT_ID_KEY, remoteObjectId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val worker = OneTimeWorkRequestBuilder<RemoveBasicDataWorker>()
            .setInputData(inputData)
            .addTag(REMOVE_ROUTE_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return WorkManagerState.state(context, worker.id)
    }

    override fun synchronizedRoute() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val worker = PeriodicWorkRequestBuilder<SynchronizedWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(SYNC_DATA_WORKER_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "periodicSynchronized",
            ExistingPeriodicWorkPolicy.KEEP,
            worker
        )
    }
}