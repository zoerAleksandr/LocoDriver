package com.z_company.repository

import android.util.Log
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.entity_converter.BasicDataConverter
import com.z_company.entity_converter.LocomotiveConverter
import com.z_company.entity_converter.PassengerConverter
import com.z_company.entity_converter.TrainConverter
import com.z_company.type_converter.RouteJSONConverter
import com.z_company.work_manager.BasicDataFieldName
import com.z_company.work_manager.ROUTE_DATA_OBJECT_ID_KEY
import com.z_company.work_manager.RouteFieldName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class Back4AppManager : KoinComponent {

    private val routeUseCase: RouteUseCase by inject()
    private val remoteRepository: RemoteRouteRepository by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    /* Удаляет данные одного Route из B4A Repository */
    private suspend fun removeRouteFromRemoteRepository(route: Route): Flow<ResultState<Unit>> =
        channelFlow {
            withContext(Dispatchers.IO) {
                trySend(ResultState.Loading)
                if (route.basicData.remoteObjectId == null) {
                    trySend(ResultState.Success(Unit))
                } else {
                    this.launch {
                        route.basicData.remoteObjectId?.let { remoteObjectId ->
                            remoteRepository.removeBasicData(remoteObjectId).collect { result ->
                                if (result is ResultState.Success) {
                                    this@launch.cancel()
                                }
                                if (result is ResultState.Error) {
                                    trySend(ResultState.Error(result.entity))
                                    this@withContext.cancel()
                                }
                            }
                        }
                    }.join()

                    this.launch {
                        route.locomotives.forEach { locomotive ->
                            locomotive.remoteObjectId?.let { remoteId ->
                                remoteRepository.removeLocomotive(remoteId).collect { result ->

                                    if (result is ResultState.Success) {
                                        this@launch.cancel()
                                    }
                                    if (result is ResultState.Error) {
                                        trySend(ResultState.Error(result.entity))
                                        this@withContext.cancel()
                                    }
                                }
                            }
                        }
                    }.join()

                    this.launch {
                        route.trains.forEach { train ->
                            train.remoteObjectId?.let { remoteId ->
                                remoteRepository.removeTrain(remoteId).collect { result ->
                                    if (result is ResultState.Success) {
                                        this@launch.cancel()
                                    }
                                    if (result is ResultState.Error) {
                                        trySend(ResultState.Error(result.entity))
                                        this@withContext.cancel()
                                    }
                                }
                            }
                        }
                    }.join()

                    this.launch {
                        route.passengers.forEach { passenger ->
                            passenger.remoteObjectId?.let { remoteId ->
                                remoteRepository.removePassenger(remoteId).collect { result ->
                                    if (result is ResultState.Success) {
                                        this@launch.cancel()
                                    }
                                    if (result is ResultState.Error) {
                                        trySend(ResultState.Error(result.entity))
                                        this@withContext.cancel()
                                    }
                                }
                            }
                        }
                    }.join()

                    this.launch {
                        route.photos.forEach { photo ->
                            photo.remoteObjectId?.let { remoteId ->
                                remoteRepository.removePhoto(remoteId).collect { result ->
                                    if (result is ResultState.Success) {
                                        this@launch.cancel()
                                    }
                                    if (result is ResultState.Error) {
                                        trySend(ResultState.Error(result.entity))
                                        this@withContext.cancel()
                                    }
                                }
                            }
                        }
                    }.join()

                    trySend(ResultState.Success(Unit))
                }
            }
            awaitClose()
        }

    /* Удаляет список Route из B4A Repository и Room repository */
    private suspend fun removeRouteList(routeList: List<Route>): Flow<ResultState<Unit>> =
        channelFlow {
            withContext(Dispatchers.IO) {
                trySend(ResultState.Loading)
                var deleteJob: Job? = null
                deleteJob?.cancel()
                deleteJob = this.launch {
                    routeList.forEach { route ->
                        this.launch {
                            removeRouteFromRemoteRepository(route).collect { result ->
                                if (result is ResultState.Success) {
                                    routeUseCase.removeRoute(route).collect {
                                        if (it is ResultState.Success) {
                                            this.cancel()
                                        }
                                    }
                                }

                                if (result is ResultState.Error) {
                                    trySend(ResultState.Error(result.entity))
                                    deleteJob?.cancel()
                                }
                            }
                        }.join()
                    }
                    trySend(ResultState.Success(Unit))
                }
                deleteJob.join()
            }
        }

    /* Поиск маршрутов помеченых для удаления */
    private suspend fun searchRemovedRoute(): Flow<ResultState<Unit>> {
        return channelFlow {
            withContext(Dispatchers.IO) {
                trySend(ResultState.Loading)
                val allRouteList = routeUseCase.listRouteWithDeleting()

                val listToDelete = allRouteList.filter { route ->
                    route.basicData.isDeleted
                }
                if (listToDelete.isNotEmpty()) {
                    removeRouteList(listToDelete).collect { result ->
                        if (result is ResultState.Success) {
                            trySend(ResultState.Success(Unit))
                        }
                        if (result is ResultState.Error) {
                            trySend(ResultState.Error(result.entity))
                        }
                    }
                } else {
                    trySend(ResultState.Success(Unit))
                }
            }
            awaitClose()
        }
    }

    /* Выгрузка на сервер несинхронизированных маршрутов */
    private suspend fun uploadingUnSynchronizedRoutesToServer(): Flow<ResultState<Unit>> =
        channelFlow {
            trySend(ResultState.Loading)
            withContext(Dispatchers.IO) {
                val list = routeUseCase.listRouteWithDeleting()
                val notSynchronizedList = list.filter { route ->
                    !route.basicData.isSynchronized
                }
                var timestamp: Long
                var syncRouteCount = 0

                if (notSynchronizedList.isEmpty()) {
                    timestamp = Calendar.getInstance().timeInMillis
                    CoroutineScope(Dispatchers.IO).launch {
                        settingsUseCase.setUpdateAt(timestamp).launchIn(this)
                    }
                    trySend(ResultState.Success(Unit))
                } else {
                    notSynchronizedList.forEach { route ->
                        CoroutineScope(Dispatchers.IO).launch {
                            remoteRepository.saveRoute(route).collect { result ->
                                Log.d("ZZZ", "result = $result")
                                if (result is ResultState.Success) {
                                    syncRouteCount += 1
                                    if (syncRouteCount == notSynchronizedList.size) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            timestamp = Calendar.getInstance().timeInMillis
                                            settingsUseCase.setUpdateAt(timestamp).launchIn(this)
                                        }
                                        trySend(ResultState.Success(Unit))
                                    }
                                    this.cancel()
                                }
                                if (result is ResultState.Error) {
                                    trySend(ResultState.Error(result.entity))
                                }
                            }
                        }
                    }
                }
            }
            awaitClose()
        }

    suspend fun synchronizedStorage(): Flow<ResultState<Unit>> {
        return channelFlow {
            trySend(ResultState.Loading)
            CoroutineScope(Dispatchers.IO).launch {
                searchRemovedRoute().collect { resultRemote ->
                    if (resultRemote is ResultState.Success) {
                        // ver2
                        migrateToSchemaVersion2().collect { resultSync ->
                            if (resultSync is ResultState.Success) {
                                loadRouteListFromRemoteSchema2().collect { resultLoading ->
                                    if (resultLoading is ResultState.Success) {
                                        trySend(ResultState.Success(Unit))
                                    }
                                }
                            }
                            if (resultSync is ResultState.Error) {
                                trySend(ResultState.Error(resultSync.entity))
                            }
                        }

                        // ver1
//                        uploadingUnSynchronizedRoutesToServer().collect { resultSync ->
//                            if (resultSync is ResultState.Success) {
//                                loadRouteListFromRemoteSchema1().collect { resultLoading ->
//                                    if (resultLoading is ResultState.Success) {
//                                        trySend(ResultState.Success(Unit))
//                                    }
//                                }
//                            }
//                            if (resultSync is ResultState.Error) {
//                                trySend(ResultState.Error(resultSync.entity))
//                            }
//                        }
                    }
                    if (resultRemote is ResultState.Error) {
                        trySend(ResultState.Error(resultRemote.entity))
                    }
                }
            }
            awaitClose()
        }
    }

    // загрузка из сервера
    fun loadRouteListFromRemoteSchema1(): Flow<ResultState<Unit>> {
        val parseQuery: ParseQuery<ParseObject> =
            ParseQuery(BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE)
        parseQuery.whereEqualTo(BasicDataFieldName.USER_FIELD_NAME, ParseUser.getCurrentUser())
        parseQuery.orderByDescending(BasicDataFieldName.BASIC_DATA_UID_FIELD_NAME)
        return channelFlow {
            trySend(ResultState.Loading)
            parseQuery.findInBackground { parseObjects, parseException ->
                if (parseException == null) {
                    trySend(ResultState.Success(Unit))
                    parseObjects.forEach { parseObject ->
                        // ver2
//                        parseObject.getString(RouteFieldName.DATA_FIELD_NAME)?.let { data ->
//                            val testRoute = RouteJSONConverter.fromString(data)
//                            Log.d("ZZZ", "route $testRoute")
//                        }

                        // ver1
//                        parseObject.apply {
//                            getString(BasicDataFieldName.BASIC_DATA_UID_FIELD_NAME)?.let { id ->
//                                loadRouteFromRemote(id)
//                            }
//                        }
                    }
                } else {
                    trySend(ResultState.Error(ErrorEntity(throwable = parseException)))
                }
            }
            awaitClose()
        }
    }

    private fun loadRouteListFromRemoteSchema2(): Flow<ResultState<Unit>> {
        val parseQuery: ParseQuery<ParseObject> =
            ParseQuery(RouteFieldName.ROUTE_CLASS_NAME_REMOTE)
        parseQuery.whereEqualTo(RouteFieldName.USER_FIELD_NAME, ParseUser.getCurrentUser())
        return channelFlow {
            trySend(ResultState.Loading)
            parseQuery.findInBackground { parseObjects, parseException ->
                if (parseException == null) {
                    if (parseObjects.isEmpty()) {
                        trySend(ResultState.Success(Unit))
                    } else {
                        parseObjects.forEachIndexed { index, parseObject ->
                            parseObject.getString(RouteFieldName.DATA_FIELD_NAME)?.let { data ->
                                this.launch(Dispatchers.IO) {
                                    var route = RouteJSONConverter.fromString(data)
                                    route = route.copy(
                                        basicData = route.basicData.copy(
                                            isSynchronized = true,
                                            remoteObjectId = parseObject.objectId
                                        )
                                    )

                                    routeUseCase.saveRoute(route)
                                        .collect {
                                            if (it is ResultState.Success) {
                                                this.cancel()
                                                if (parseObjects.size == index + 1) {
                                                    trySend(ResultState.Success(Unit))
                                                }
                                            }
                                        }
                                }
                            }
                        }
                    }
                } else {
                    trySend(ResultState.Error(ErrorEntity(throwable = parseException)))
                }
            }
            awaitClose()
        }
    }

    private fun loadRouteFromRemote(id: String) {
        var route = Route()
        CoroutineScope(Dispatchers.IO).launch {
            this.launch {
                remoteRepository.loadBasicDataFromRemote(id).collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let {
                            route =
                                route.copy(
                                    basicData = BasicDataConverter.toData(
                                        it.copy(
                                            isSynchronized = true
                                        )
                                    ),
                                )
                        }
                        this.cancel()
                    }
                }
            }.join()

            this.launch {
                remoteRepository.loadLocomotiveFromRemote(id).collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { locomotives ->
                            route = route.copy(
                                locomotives = LocomotiveConverter.toDataList(locomotives)
                            )
                        }
                        this.cancel()
                    }
                }
            }.join()

            this.launch {
                remoteRepository.loadTrainFromRemote(id).collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { trains ->
                            route = route.copy(
                                trains = TrainConverter.fromRemoteList(trains)
                            )
                        }
                        this.cancel()
                    }
                }
            }.join()

            this.launch {
                remoteRepository.loadPassengerFromRemote(id).collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { passengers ->
                            route = route.copy(
                                passengers = PassengerConverter.fromRemoteList(
                                    passengers
                                )
                            )
                        }
                        this.cancel()
                    }
                }
            }.join()

            this.launch {
                routeUseCase.saveRoute(route).collect {
                    if (it is ResultState.Success) {
                        this.cancel()
                    }
                }
            }.join()
        }
    }

    private fun migrateToSchemaVersion2(): Flow<ResultState<Unit>> =
        channelFlow {
            // если маршрут не синхронизирован то удаляем данные из таблиц BasicData и т.д.
            // далее присваиваем ему номер схемы 2 и отправляем на сервер как Route

            trySend(ResultState.Loading)
            withContext(Dispatchers.IO) {
                val list = routeUseCase.listRouteWithDeleting()
                val notSynchronizedList = list.filter { route ->
                    !route.basicData.isSynchronized
                }
                var timestamp: Long

                if (notSynchronizedList.isEmpty()) {
                    timestamp = Calendar.getInstance().timeInMillis
                    settingsUseCase.setUpdateAt(timestamp).launchIn(this)
                    trySend(ResultState.Success(Unit))
                } else {
                    val notSynchronizedListVersion2 =
                        notSynchronizedList.filter { it.basicData.schemaVersion == 2 }
                    val notSynchronizedListVersion1 =
                        notSynchronizedList.filter {
                            it.basicData.schemaVersion == 1
                        }

                    if (notSynchronizedListVersion2.isNotEmpty()) {
                        var syncRouteCount = 0
                        notSynchronizedListVersion2.forEach { route ->
                            Log.d("ZZZ", "2 sync $route")
                            this.launch {
                                remoteRepository.saveRouteVer2(route).collect { result ->
                                    if (result is ResultState.Success) {
                                        result.data.getString(ROUTE_DATA_OBJECT_ID_KEY)
                                            ?.let { remoteId ->
                                                routeUseCase.setSynchronizedBasicData(remoteId)
                                                    .launchIn(this)
                                                    .join()
                                            }

                                        syncRouteCount += 1
                                        if (syncRouteCount == notSynchronizedListVersion2.size) {
                                            timestamp = Calendar.getInstance().timeInMillis
                                            settingsUseCase.setUpdateAt(timestamp).launchIn(this)
                                        }
                                        this@launch.cancel()
                                    }
                                    if (result is ResultState.Error) {
                                        trySend(ResultState.Error(result.entity))
                                    }
                                }
                            }.join()
                        }
                    }
                    if (notSynchronizedListVersion1.isNotEmpty()) {
                        var syncRouteCount = 0
                        notSynchronizedListVersion1.forEach { route ->
                            Log.d("ZZZ", "1 sync $route")
                                this.launch {
                                    if (route.basicData.remoteObjectId != null) {
                                        removeRouteFromRemoteRepository(route).collect { resultRemove ->
                                            if (resultRemove is ResultState.Success) {
                                                routeUseCase.setSchemaVersion(
                                                    version = 2,
                                                    id = route.basicData.id
                                                ).launchIn(this).join()

                                                routeUseCase.setRemoteObjectIdBasicData(
                                                    route.basicData.id,
                                                    null
                                                ).launchIn(this).join()

                                                val routeWithoutRemoteId = route.copy(
                                                    basicData = route.basicData.copy(
                                                        remoteObjectId = null
                                                    )
                                                )
                                                remoteRepository.saveRoute(routeWithoutRemoteId)
                                                    .collect { resultSave ->
                                                        Log.d("ZZZ", "save route ver1 = $resultSave")
                                                        if (resultSave is ResultState.Success) {

                                                            routeUseCase.setSynchronizedBasicData(route.basicData.id)
                                                                .launchIn(this)

                                                            syncRouteCount += 1
                                                            if (syncRouteCount == notSynchronizedListVersion1.size) {
                                                                timestamp =
                                                                    Calendar.getInstance().timeInMillis
                                                                settingsUseCase.setUpdateAt(timestamp)
                                                                    .launchIn(this)
                                                            }

                                                            this@launch.cancel()
                                                        }
                                                        if (resultSave is ResultState.Error) {
                                                            trySend(ResultState.Error(resultSave.entity))
                                                        }
                                                    }
                                            }
                                            if (resultRemove is ResultState.Error) {
                                                trySend(ResultState.Error(resultRemove.entity))
                                            }
                                        }
                                    } else {
                                        remoteRepository.saveRouteVer2(route)
                                            .collect { resultSave ->
                                                delay(200L)
                                                Log.d("ZZZ", "save new route ver1 = $resultSave")
                                                if (resultSave is ResultState.Success) {
                                                    routeUseCase.setSynchronizedBasicData(route.basicData.id)
                                                        .launchIn(this)

                                                    syncRouteCount += 1
                                                    if (syncRouteCount == notSynchronizedListVersion1.size) {
                                                        timestamp =
                                                            Calendar.getInstance().timeInMillis
                                                        settingsUseCase.setUpdateAt(timestamp)
                                                            .launchIn(this)
                                                    }

                                                    this@launch.cancel()
                                                }
                                                if (resultSave is ResultState.Error) {
                                                    trySend(ResultState.Error(resultSave.entity))
                                                }
                                            }
                                    }
                                }.join()

                        }
                    }
                    trySend(ResultState.Success(Unit))
                }
            }
        }
}