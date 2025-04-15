package com.z_company.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.parse.ParseUser
import com.z_company.ParseHelper
import com.z_company.type_converter.BasicDataJSONConverter
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.entity.BasicData
import com.z_company.entity.Locomotive
import com.z_company.entity.Passenger
import com.z_company.entity.Train
import com.z_company.type_converter.TrainJSONConverter
import com.z_company.type_converter.LocomotiveJSONConverter
import com.z_company.type_converter.PassengerJSONConverter
import com.z_company.type_converter.RouteJSONConverter
import com.z_company.work_manager.GET_BASIC_DATA_WORKER_OUTPUT_KEY
import com.z_company.work_manager.LOAD_BASIC_DATA_ID_INPUT_KEY
import com.z_company.work_manager.LOAD_BASIC_DATA_ID_OUTPUT_KEY
import com.z_company.work_manager.LoadBasicDataListWorker
import com.z_company.work_manager.LOAD_LOCOMOTIVE_WORKER_INPUT_KEY
import com.z_company.work_manager.LOAD_LOCOMOTIVE_WORKER_OUTPUT_KEY
import com.z_company.work_manager.LOAD_PASSENGER_WORKER_INPUT_KEY
import com.z_company.work_manager.LOAD_PASSENGER_WORKER_OUTPUT_KEY
import com.z_company.work_manager.LOAD_TRAIN_WORKER_INPUT_KEY
import com.z_company.work_manager.LOAD_TRAIN_WORKER_OUTPUT_KEY
import com.z_company.work_manager.LoadBasicDataWorker
import com.z_company.work_manager.LoadLocomotiveFromRemoteWorker
import com.z_company.work_manager.LoadPassengerRemoteWorker
import com.z_company.work_manager.LoadTrainFromRemoteWorker
import com.z_company.work_manager.REMOVE_BASIC_DATA_OBJECT_ID_KEY
import com.z_company.work_manager.REMOVE_LOCOMOTIVE_OBJECT_ID_KEY
import com.z_company.work_manager.REMOVE_PASSENGER_OBJECT_ID_KEY
import com.z_company.work_manager.REMOVE_PHOTO_OBJECT_ID_KEY
import com.z_company.work_manager.REMOVE_ROUTE_OBJECT_ID_KEY
import com.z_company.work_manager.REMOVE_TRAIN_OBJECT_ID_KEY
import com.z_company.work_manager.RemoveBasicDataWorker
import com.z_company.work_manager.RemoveLocomotiveWorker
import com.z_company.work_manager.RemovePassengerWorker
import com.z_company.work_manager.RemovePhotoWorker
import com.z_company.work_manager.RemoveRouteWorker
import com.z_company.work_manager.RemoveTrainWorker
import com.z_company.work_manager.RouteFieldName
import com.z_company.work_manager.SaveLocomotiveListWorker
import com.z_company.work_manager.SynchronizedWorker
import com.z_company.work_manager.WorkManagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import ru.ok.tracer.crash.report.TracerCrashReport
import java.io.NotActiveException
import java.util.concurrent.TimeUnit

private const val SAVE_ROUTE_WORKER_TAG = "SAVE_ROUTE_WORKER_TAG"
private const val SAVE_LOCO_WORKER_TAG = "SAVE_LOCO_WORKER_TAG"

private const val UNIQUE_SYNC_WORK_NAME = "periodicSynchronized"
private const val GET_ALL_DATA_WORKER_TAG = "GET_ALL_DATA_WORKER_TAG"
private const val SYNC_DATA_PERIODIC_WORKER_TAG = "SYNC_DATA_PERIODIC_WORKER_TAG"
private const val REMOVE_ROUTE_WORKER_TAG = "REMOVE_ROUTE_WORKER_TAG"
private const val REMOVE_BASIC_DATA_WORKER_TAG = "REMOVE_BASIC_DATA_WORKER_TAG"
private const val REMOVE_LOCOMOTIVE_WORKER_TAG = "REMOVE_LOCOMOTIVE_WORKER_TAG"
private const val REMOVE_TRAIN_WORKER_TAG = "REMOVE_TRAIN_WORKER_TAG"
private const val REMOVE_PASSENGER_WORKER_TAG = "REMOVE_PASSENGER_WORKER_TAG"
private const val REMOVE_PHOTO_WORKER_TAG = "REMOVE_PHOTO_WORKER_TAG"

private const val LOAD_BASIC_DATA_FROM_REMOTE_WORKER_TAG = "LOAD_BASIC_DATA_FROM_REMOTE_WORKER_TAG"
private const val LOAD_LOCOMOTIVE_FROM_REMOTE_WORKER_TAG = "LOAD_DATA_FROM_REMOTE_WORKER_TAG"
private const val LOAD_TRAIN_FROM_REMOTE_WORKER_TAG = "LOAD_TRAIN_FROM_REMOTE_WORKER_TAG"
private const val LOAD_PASSENGER_FROM_REMOTE_WORKER_TAG = "LOAD_PASSENGER_FROM_REMOTE_WORKER_TAG"

class B4ARouteRepository(private val context: Context) : RemoteRouteRepository, KoinComponent {
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override suspend fun loadBasicDataFromRemote(id: String): Flow<ResultState<BasicData?>> {
        val inputData = Data.Builder()
            .putString(LOAD_BASIC_DATA_ID_INPUT_KEY, id)
            .build()

        val worker = OneTimeWorkRequestBuilder<LoadBasicDataWorker>()
            .addTag(LOAD_BASIC_DATA_FROM_REMOTE_WORKER_TAG)
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return flow {
            WorkManagerState.state(context, worker.id).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val data = result.data.getString(LOAD_BASIC_DATA_ID_OUTPUT_KEY)
                        data?.let {
                            val basicData = BasicDataJSONConverter.fromString(data)
                            emit(ResultState.Success(basicData))
                        }
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

    override suspend fun saveRouteVer2(route: Route): Flow<ResultState<String>> =
        channelFlow {
            trySend(ResultState.Loading)
            val currentUser = ParseUser.getCurrentUser()
            val routeJSON = RouteJSONConverter.toString(route)
            ParseHelper.saveOrUpdateObjectAsync(
                className = RouteFieldName.ROUTE_CLASS_NAME_REMOTE,
                uniqueKey = "objectId",
                uniqueValue = route.basicData.remoteRouteId,
                fieldsToUpdate = mapOf(
                    Pair(RouteFieldName.DATA_FIELD_NAME, routeJSON),
                    Pair(RouteFieldName.USER_EMAIL_FIELD_NAME, currentUser.email)
                )
            ).collect { saveResult ->
                if (saveResult is ResultState.Success) {
                    trySend(ResultState.Success(saveResult.data))
                    this.cancel()
                }
                if (saveResult is ResultState.Error) {
                    trySend(ResultState.Error(saveResult.entity))
                    TracerCrashReport.report(NotActiveException("${saveResult.entity.throwable} \n$route"))
                    this.cancel()
                }
            }
            awaitClose()
        }

    override suspend fun getAllBasicDataId(): Flow<ResultState<List<String>?>> {
        val worker = OneTimeWorkRequestBuilder<LoadBasicDataListWorker>()
            .addTag(GET_ALL_DATA_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return flow {
            WorkManagerState.state(context, worker.id)
                .collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val idList = result.data
                                .getStringArray(GET_BASIC_DATA_WORKER_OUTPUT_KEY)
                            emit(ResultState.Success(idList?.toList()))
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

    override suspend fun removeRoute(remoteRouteId: String): Flow<ResultState<Data>> {
        val inputData = Data.Builder()
            .putString(REMOVE_ROUTE_OBJECT_ID_KEY, remoteRouteId)
            .build()

        val worker = OneTimeWorkRequestBuilder<RemoveRouteWorker>()
            .setInputData(inputData)
            .addTag(REMOVE_ROUTE_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return WorkManagerState.state(context, worker.id)
    }

    override suspend fun removeBasicData(remoteObjectId: String): Flow<ResultState<Data>> {
        val inputData = Data.Builder()
            .putString(REMOVE_BASIC_DATA_OBJECT_ID_KEY, remoteObjectId)
            .build()

        val worker = OneTimeWorkRequestBuilder<RemoveBasicDataWorker>()
            .setInputData(inputData)
            .addTag(REMOVE_BASIC_DATA_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return WorkManagerState.state(context, worker.id)
    }

    override suspend fun synchronizedRoutePeriodic(): Flow<ResultState<Unit>> {
        val worker = PeriodicWorkRequestBuilder<SynchronizedWorker>(
            36,
            TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .addTag(SYNC_DATA_PERIODIC_WORKER_TAG)
            .build()

        withContext(Dispatchers.IO) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                worker
            )
        }

        return flow {
            WorkManagerState.state(context, worker.id).collect { result ->
                when (result) {
                    is ResultState.Loading -> {
                        emit(result)
                    }

                    is ResultState.Success -> {
                        emit(ResultState.Success(Unit))
                    }

                    is ResultState.Error -> {
                        emit(result)
                    }
                }
            }
        }
    }

    override suspend fun cancelingSync() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK_NAME)
    }

    override suspend fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Data>> {
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

        val worker = OneTimeWorkRequestBuilder<RemoveLocomotiveWorker>()
            .setInputData(inputData)
            .addTag(REMOVE_LOCOMOTIVE_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return WorkManagerState.state(context, worker.id)
    }

    override suspend fun removeTrain(remoteObjectId: String): Flow<ResultState<Data>> {
        val inputData = Data.Builder()
            .putString(REMOVE_TRAIN_OBJECT_ID_KEY, remoteObjectId)
            .build()

        val worker = OneTimeWorkRequestBuilder<RemoveTrainWorker>()
            .setInputData(inputData)
            .addTag(REMOVE_TRAIN_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return WorkManagerState.state(context, worker.id)
    }

    override suspend fun removePassenger(remoteId: String): Flow<ResultState<Data>> {
        val inputData = Data.Builder()
            .putString(REMOVE_PASSENGER_OBJECT_ID_KEY, remoteId)
            .build()

        val worker = OneTimeWorkRequestBuilder<RemovePassengerWorker>()
            .setInputData(inputData)
            .addTag(REMOVE_PASSENGER_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return WorkManagerState.state(context, worker.id)
    }

    override suspend fun removePhoto(remoteId: String): Flow<ResultState<Data>> {
        val inputData = Data.Builder()
            .putString(REMOVE_PHOTO_OBJECT_ID_KEY, remoteId)
            .build()
        val worker = OneTimeWorkRequestBuilder<RemovePhotoWorker>()
            .setInputData(inputData)
            .addTag(REMOVE_PHOTO_WORKER_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return WorkManagerState.state(context, worker.id)
    }

    override suspend fun loadLocomotiveFromRemote(basicId: String): Flow<ResultState<List<Locomotive>?>> {
        val inputData = Data.Builder()
            .putString(LOAD_LOCOMOTIVE_WORKER_INPUT_KEY, basicId)
            .build()

        val worker = OneTimeWorkRequestBuilder<LoadLocomotiveFromRemoteWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(LOAD_LOCOMOTIVE_FROM_REMOTE_WORKER_TAG)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return flow {
            WorkManagerState.state(context, worker.id)
                .collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val stringList = result.data
                                .getStringArray(LOAD_LOCOMOTIVE_WORKER_OUTPUT_KEY)
                            val locomotiveList = stringList?.map {
                                LocomotiveJSONConverter.fromString(it)
                            }
                            emit(ResultState.Success(locomotiveList))
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

    override suspend fun loadTrainFromRemote(basicId: String): Flow<ResultState<List<Train>?>> {
        val inputData = Data.Builder()
            .putString(LOAD_TRAIN_WORKER_INPUT_KEY, basicId)
            .build()

        val worker = OneTimeWorkRequestBuilder<LoadTrainFromRemoteWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(LOAD_TRAIN_FROM_REMOTE_WORKER_TAG)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return flow {
            WorkManagerState.state(context, worker.id)
                .collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val stringList = result.data
                                .getStringArray(LOAD_TRAIN_WORKER_OUTPUT_KEY)
                            val trainList = stringList?.map {
                                TrainJSONConverter.fromString(it)
                            }
                            emit(ResultState.Success(trainList))
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

    override suspend fun loadPassengerFromRemote(basicId: String): Flow<ResultState<List<Passenger>?>> {
        val inputData = Data.Builder()
            .putString(LOAD_PASSENGER_WORKER_INPUT_KEY, basicId)
            .build()

        val worker = OneTimeWorkRequestBuilder<LoadPassengerRemoteWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(LOAD_PASSENGER_FROM_REMOTE_WORKER_TAG)
            .build()
        WorkManager.getInstance(context).enqueue(worker)
        return flow {
            WorkManagerState.state(context, worker.id)
                .collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val stringList = result.data
                                .getStringArray(LOAD_PASSENGER_WORKER_OUTPUT_KEY)
                            val passengerList = stringList?.map {
                                PassengerJSONConverter.fromString(it)
                            }
                            emit(ResultState.Success(passengerList))
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
}