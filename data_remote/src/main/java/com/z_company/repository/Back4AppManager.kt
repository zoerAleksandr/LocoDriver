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
import com.z_company.work_manager.RouteFieldName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class Back4AppManager : KoinComponent {

    private val routeUseCase: RouteUseCase by inject()
    private val remoteRepository: RemoteRouteRepository by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    /* Удаляет данные одного Route из B4A Repository в таблицах BasicData Train Locomotive Passenger*/
    private suspend fun removeRouteFromRemoteRepositoryOldMethod(route: Route): Flow<ResultState<Unit>> =
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

    /* Удаляет данные одного Route из B4A Repository в таблице Route*/
    private suspend fun removeRouteFromRemoteRepositoryNewMethod(route: Route): Flow<ResultState<Unit>> =
        channelFlow {
            withContext(Dispatchers.IO) {
                trySend(ResultState.Loading)
                val routeId = route.basicData.remoteRouteId
                if (routeId == null) {
                    trySend(ResultState.Success(Unit))
                } else {
                    remoteRepository.removeRoute(remoteRouteId = routeId).collect { result ->
                        if (result is ResultState.Success) {
                            trySend(ResultState.Success(Unit))
                            this@withContext.cancel()
                        }
                        if (result is ResultState.Error) {
                            trySend(ResultState.Error(result.entity))
                            this@withContext.cancel()
                        }
                    }
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
                deleteJob = this.launch {
                    routeList.forEach { route ->
                        if (route.basicData.remoteObjectId != null) {
                            this.launch {
                                removeRouteFromRemoteRepositoryOldMethod(route).collect { result ->
                                    if (result is ResultState.Success) {
                                        // после успешного удаления на облаке удалить из Room
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
                        if (route.basicData.remoteRouteId != null) {
                            this.launch {
                                removeRouteFromRemoteRepositoryNewMethod(route).collect { result ->
                                    if (result is ResultState.Success) {
                                        // после успешного удаления на облаке удалить из Room
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
                        if (route.basicData.remoteRouteId == null && route.basicData.remoteObjectId == null) {
                            this.launch {
                                routeUseCase.removeRoute(route).collect {
                                    if (it is ResultState.Success) {
                                        this.cancel()
                                    }
                                }
                            }
                        }
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

    suspend fun synchronizedStorage(): Flow<ResultState<Int>> {
        return channelFlow {
            trySend(ResultState.Loading)
            CoroutineScope(Dispatchers.IO).launch {
                searchRemovedRoute().collect { resultRemote ->
                    if (resultRemote is ResultState.Success) {
                        saveRouteToRemoteStorage().collect { resultSync ->
                            if (resultSync is ResultState.Success) {
                                trySend(ResultState.Success(resultSync.data))
                            }
                            if (resultSync is ResultState.Error) {
                                trySend(ResultState.Error(resultSync.entity))
                            }
                        }
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
    fun loadRouteListFromRemote(): Flow<ResultState<Int>> {
        return channelFlow {
            trySend(ResultState.Loading)
            loadRouteListFromRemoteOldVersion().collect { loadOldVersionResult ->
                if (loadOldVersionResult is ResultState.Success) {
                    val countOldRoutes = loadOldVersionResult.data
                    loadRouteFromRemoteNewVersion().collect { loadNewVersionResult ->
                        if (loadNewVersionResult is ResultState.Success) {
                            val allRoutes = countOldRoutes + loadNewVersionResult.data
                            trySend(ResultState.Success(allRoutes))
                        }
                        if (loadNewVersionResult is ResultState.Error) {
                            trySend(ResultState.Error(loadNewVersionResult.entity))
                        }
                    }
                }
                if (loadOldVersionResult is ResultState.Error) {
                    trySend(ResultState.Error(loadOldVersionResult.entity))
                }
            }
            awaitClose()
        }
    }

    /* Выгрузка на сервер несинхронизированных маршрутов
    * возвращает количество синхронизированных маршрутов*/
    private fun saveRouteToRemoteStorage(): Flow<ResultState<Int>> =
        channelFlow {
            trySend(ResultState.Loading)
            withContext(Dispatchers.IO) {
                val notSynchronizedList = routeUseCase.getListRoutes().filter {
                    !it.basicData.isSynchronizedRoute
                }
                var timestamp: Long
                if (notSynchronizedList.isEmpty()) {
                    timestamp = Calendar.getInstance().timeInMillis
                    settingsUseCase.setUpdateAt(timestamp).collect()
                    trySend(ResultState.Success(0))
                } else {
                    var syncRouteCount = 0
                    notSynchronizedList.forEach { route ->
                        this.launch {
                            Log.d("ZZZ", "route for save $route")
                            remoteRepository.saveRouteVer2(route).collect { result ->
                                if (result is ResultState.Success) {
                                    result.data.let { remoteId ->
                                        this.launch {
                                            routeUseCase.setRemoteRouteIdRoute(
                                                basicId = route.basicData.id,
                                                remoteRouteId = remoteId
                                            ).collect {
                                                if (it is ResultState.Success) {
                                                    this.cancel()
                                                }
                                            }
                                        }.join()

                                        this.launch {
                                            routeUseCase.setSynchronizedRoute(route.basicData.id)
                                                .collect {
                                                    if (it is ResultState.Success) {
                                                        this.cancel()
                                                    }
                                                }
                                        }.join()
                                    }

                                    syncRouteCount += 1
                                    if (syncRouteCount == notSynchronizedList.size) {
                                        timestamp = Calendar.getInstance().timeInMillis
                                        settingsUseCase.setUpdateAt(timestamp).collect()
                                        trySend(ResultState.Success(syncRouteCount))
                                        this@withContext.cancel()
                                    }
                                    this@launch.cancel()
                                }
                                if (result is ResultState.Error) {
                                    trySend(ResultState.Error(result.entity))
                                    this@withContext.cancel()
                                }
                            }
                        }.join()
                    }
                }
            }
            awaitClose()
        }

    fun saveOneRouteToRemoteStorage(route: Route): Flow<ResultState<Unit>> =
        channelFlow {
            remoteRepository.saveRouteVer2(route).collect { result ->
                if (result is ResultState.Success) {
                    result.data.let { remoteId ->
                        this.launch {
                            routeUseCase.setRemoteRouteIdRoute(
                                route.basicData.id, remoteId
                            ).collect {
                                if (it is ResultState.Success) {
                                    this.cancel()
                                }
                            }
                        }.join()

                        this.launch {
                            routeUseCase.setSynchronizedRoute(route.basicData.id)
                                .collect {
                                    if (it is ResultState.Success) {
                                        this.cancel()
                                    }
                                }
                        }.join()

                        trySend(ResultState.Success(Unit))
                    }
                }
                if (result is ResultState.Error) {
                    trySend(ResultState.Error(result.entity))
                }
            }
            awaitClose()
        }

    private fun loadRouteFromRemoteNewVersion(): Flow<ResultState<Int>> {
        val parseQuery: ParseQuery<ParseObject> =
            ParseQuery(RouteFieldName.ROUTE_CLASS_NAME_REMOTE)
        parseQuery.whereEqualTo(RouteFieldName.USER_FIELD_NAME, ParseUser.getCurrentUser())
        return channelFlow {
            trySend(ResultState.Loading)
            parseQuery.findInBackground { parseObjects, parseException ->
                if (parseException == null) {
                    if (parseObjects.isEmpty()) {
                        trySend(ResultState.Success(0))
                    } else {
                        parseObjects.sortBy {
                            it.updatedAt
                        }
                        parseObjects.forEachIndexed { index, parseObject ->
                            CoroutineScope(Dispatchers.IO).launch {
                                val saveRouteJob = this.launch {
                                    parseObject.getString(RouteFieldName.DATA_FIELD_NAME)
                                        ?.let { data ->
                                            var route = RouteJSONConverter.fromString(data)
                                            route = route.copy(
                                                basicData = route.basicData.copy(
                                                    isSynchronizedRoute = true,
                                                    remoteRouteId = parseObject.objectId
                                                )
                                            )

                                            routeUseCase.saveRouteAfterLoading(route)
                                                .collect {
                                                    if (it is ResultState.Success) {
                                                        if (parseObjects.size == index + 1) {
                                                            trySend(ResultState.Success(parseObjects.size))
                                                            val timeInMillis =
                                                                Calendar.getInstance().timeInMillis
                                                            settingsUseCase.setUpdateAt(timeInMillis)
                                                                .collect()
                                                        }
                                                        this.cancel()
                                                    }
                                                    if (it is ResultState.Error) {
                                                        trySend(ResultState.Error(it.entity))
                                                    }
                                                }
                                        }
                                }
                                saveRouteJob.join()
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

    private fun loadRouteListFromRemoteOldVersion(): Flow<ResultState<Int>> {
        val parseQuery: ParseQuery<ParseObject> =
            ParseQuery(BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE)
        parseQuery.whereEqualTo(BasicDataFieldName.USER_FIELD_NAME, ParseUser.getCurrentUser())
        parseQuery.orderByDescending(BasicDataFieldName.BASIC_DATA_UID_FIELD_NAME)
        return channelFlow {
            trySend(ResultState.Loading)
            parseQuery.findInBackground { parseObjects, parseException ->
                if (parseException == null) {
                    if (parseObjects.isEmpty()) {
                        trySend(ResultState.Success(0))
                    } else {
                        parseObjects.forEachIndexed { index, parseObject ->
                            parseObject.apply {
                                getString(BasicDataFieldName.BASIC_DATA_UID_FIELD_NAME)?.let { id ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        loadRouteFromRemote(id).collect { loadRouteResult ->
                                            if (loadRouteResult is ResultState.Success) {
                                                saveOneRouteToRemoteStorage(loadRouteResult.data).collect { saveResult ->
                                                    if (saveResult is ResultState.Success) {
                                                        removeRouteFromRemoteRepositoryOldMethod(
                                                            loadRouteResult.data
                                                        ).collect {}
                                                    }
                                                }
                                            }
                                            if (loadRouteResult is ResultState.Success && parseObjects.size == index + 1) {
                                                val timeInMillis =
                                                    Calendar.getInstance().timeInMillis
                                                settingsUseCase.setUpdateAt(timeInMillis)
                                                    .collect()
                                                trySend(ResultState.Success(parseObjects.size))
                                            }
                                            if (loadRouteResult is ResultState.Error) {
                                                trySend(ResultState.Error(loadRouteResult.entity))
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

    private fun loadRouteFromRemote(id: String): Flow<ResultState<Route>> {
        return channelFlow {
            trySend(ResultState.Loading)

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
                    routeUseCase.saveRouteAfterLoading(route).collect {
                        if (it is ResultState.Success) {
                            trySend(ResultState.Success(route))
                            this.cancel()
                        }
                    }
                }.join()
            }
            awaitClose()
        }
    }
}
