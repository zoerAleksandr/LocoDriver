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
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.entity.BasicData
import com.z_company.entity.Locomotive
import com.z_company.entity_converter.BasicDataConverter
import com.z_company.entity_converter.LocomotiveConverter
import com.z_company.entity_converter.TrainConverter
import com.z_company.entity_converter.TrainJSONConverter
import com.z_company.type_converter.LocomotiveJSONConverter
import com.z_company.work_manager.BASIC_DATA_INPUT_KEY
import com.z_company.work_manager.GET_BASIC_DATA_WORKER_OUTPUT_KEY
import com.z_company.work_manager.SaveBasicDataWorker
import com.z_company.work_manager.GetBasicDataListWorker
import com.z_company.work_manager.LOCOMOTIVE_INPUT_KEY
import com.z_company.work_manager.REMOVE_BASIC_DATA_OBJECT_ID_KEY
import com.z_company.work_manager.REMOVE_LOCOMOTIVE_OBJECT_ID_KEY
import com.z_company.work_manager.RemoveBasicDataWorker
import com.z_company.work_manager.RemoveLocomotiveWorker
import com.z_company.work_manager.SaveLocomotiveListWorker
import com.z_company.work_manager.SaveTrainListWorker
import com.z_company.work_manager.SynchronizedWorker
import com.z_company.work_manager.TRAINS_INPUT_KEY
import com.z_company.work_manager.WorkManagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

private const val SAVE_ROUTE_WORKER_TAG = "SAVE_ROUTE_WORKER_TAG"
private const val SAVE_LOCO_WORKER_TAG = "SAVE_LOCO_WORKER_TAG"
private const val SAVE_TRAIN_WORKER_TAG = "SAVE_TRAIN_WORKER_TAG"

private const val GET_ALL_DATA_WORKER_TAG = "SYNC_DATA_WORKER_TAG"
private const val SYNC_DATA_WORKER_TAG = "SYNC_DATA_WORKER_TAG"
private const val REMOVE_BASIC_DATA_WORKER_TAG = "REMOVE_BASIC_DATA_WORKER_TAG"
private const val REMOVE_LOCOMOTIVE_WORKER_TAG = "REMOVE_LOCOMOTIVE_WORKER_TAG"

class B4ARouteRepository(private val context: Context) : RemoteRouteRepository, KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    override suspend fun saveRoute(route: Route): Flow<ResultState<Data>> {
        val basicDataJSON = BasicDataJSONConverter.toString(
            BasicDataConverter.fromData(route.basicData)
        )

        val locomotiveJSONList: Array<String> = Array(route.locomotives.size) {""}
        route.locomotives.forEachIndexed { index, loco ->
            locomotiveJSONList[index] = LocomotiveJSONConverter.toString(
                LocomotiveConverter.fromData(loco)
            )
        }

        val trainJSONList: Array<String> = Array(route.trains.size) {""}
        route.trains.forEachIndexed { index, train ->
            trainJSONList[index] = TrainJSONConverter.toString(
                TrainConverter.toRemote(train)
            )
        }

        val basicDataInput = Data.Builder()
            .putString(BASIC_DATA_INPUT_KEY, basicDataJSON)
            .build()

        val locomotiveInput = Data.Builder()
            .putStringArray(LOCOMOTIVE_INPUT_KEY, locomotiveJSONList)
            .build()

        val trainInput = Data.Builder()
            .putStringArray(TRAINS_INPUT_KEY, trainJSONList)
            .build()

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val basicDataWorker = OneTimeWorkRequestBuilder<SaveBasicDataWorker>()
            .setInputData(basicDataInput)
            .addTag(SAVE_ROUTE_WORKER_TAG)
            .setConstraints(constraints)
            .build()

        val locoWorker = OneTimeWorkRequestBuilder<SaveLocomotiveListWorker>()
            .setInputData(locomotiveInput)
            .addTag(SAVE_LOCO_WORKER_TAG)
            .setConstraints(constraints)
            .build()

        val trainWorker = OneTimeWorkRequestBuilder<SaveTrainListWorker>()
            .setInputData(trainInput)
            .addTag(SAVE_TRAIN_WORKER_TAG)
            .setConstraints(constraints)
            .build()

        val workChain = WorkManager.getInstance(context)
            .beginWith(basicDataWorker)
            .then(listOf(locoWorker, trainWorker))

        workChain.enqueue()

        val worksList = listOf(basicDataWorker.id, locoWorker.id, trainWorker.id)

        CoroutineScope(Dispatchers.IO).launch {
            WorkManagerState.listState(context, worksList, basicDataWorker.id).collect { result ->
                if (result is ResultState.Success) {
                    routeUseCase.isSynchronizedBasicData(result.data)
                        .launchIn(this)
                }
            }
        }

        return WorkManagerState.state(context, basicDataWorker.id)
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
            .putString(REMOVE_BASIC_DATA_OBJECT_ID_KEY, remoteObjectId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val worker = OneTimeWorkRequestBuilder<RemoveBasicDataWorker>()
            .setInputData(inputData)
            .addTag(REMOVE_BASIC_DATA_WORKER_TAG)
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

        val w = OneTimeWorkRequestBuilder<SynchronizedWorker>()
            .setConstraints(constraints)
            .addTag(SYNC_DATA_WORKER_TAG)
            .build()
        WorkManager.getInstance(context).enqueue(w)

//        val worker = PeriodicWorkRequestBuilder<SynchronizedWorker>(
//            15,
//            TimeUnit.MINUTES,
//            )
//            .setInitialDelay(15, TimeUnit.MINUTES)
//            .setConstraints(constraints)
//            .addTag(SYNC_DATA_WORKER_TAG)
//            .build()

//        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//            "periodicSynchronized",
//            ExistingPeriodicWorkPolicy.KEEP,
//            worker
//        )

    }

    override suspend fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Data>> {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workerSaveLoco = OneTimeWorkRequestBuilder<SaveLocomotiveListWorker>()
            .setConstraints(constraints)
            .addTag(SAVE_LOCO_WORKER_TAG)
            .build()
        WorkManager.getInstance(context).enqueue(workerSaveLoco)
        return WorkManagerState.state(context, workerSaveLoco.id)
    }

    override suspend fun removeLocomotive(remoteObjectId: String): Flow<ResultState<Data>> {
        val inputData = Data.Builder()
            .putString(REMOVE_LOCOMOTIVE_OBJECT_ID_KEY, remoteObjectId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val worker = OneTimeWorkRequestBuilder<RemoveLocomotiveWorker>()
            .setInputData(inputData)
            .addTag(REMOVE_LOCOMOTIVE_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return WorkManagerState.state(context, worker.id)
    }

    override suspend fun remoteTrain(remoteObjectId: String): Flow<ResultState<Data>> {
         return
    }
}