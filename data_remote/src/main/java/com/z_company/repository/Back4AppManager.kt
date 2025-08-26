package com.z_company.repository

import android.util.Log
import com.parse.FindCallback
import com.parse.ParseException
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar


const val TIMEOUT_LOADING = 360_000L

class Back4AppManager : KoinComponent {

    fun getAllRouteRemote() {
        val query = ParseQuery.getQuery<ParseObject>("Route")
        query.limit = 30_000

        try {
            Log.d("zzz", "start")
            // Synchronously find all Route objects
            val routes = query.find()
            Log.d("zzz", "routes size ${routes.size}")

            // Group routes by 'Data' field
            val groupedRoutes = routes.groupBy { it.getString("data") }

            // Process groups with more than one entry
            groupedRoutes.forEach { (_, routesWithSameData) ->
                if (routesWithSameData.size > 1) {
                    // Sort routes by creation time in descending order to keep the latest one
//                    Log.d("zzz", "size group ${routesWithSameData.size}")
                    val sortedRoutes = routesWithSameData.sortedByDescending { it.createdAt }

                    // Remove all but the most recent
//                    Log.d("zzz", "Останется ${sortedRoutes[0].createdAt} ${sortedRoutes[0].objectId} ${sortedRoutes[0].getString("data")}")
                    val routesToDelete = sortedRoutes.drop(1) // drop() the first (most recent) one

//                    Log.d("zzz", "size routesToDelete ${routesToDelete.size}")
//                    routesToDelete.forEachIndexed { index, route ->
//                        Log.d("zzz", "Удаление: ${route.createdAt} ${route.objectId}")
//                    }
                    // Delete the older routes
                    ParseObject.deleteAll(routesToDelete)

                    println(
                        "zzz Removed ${routesToDelete.size}"
                    )
                }
            }

        } catch (e: ParseException) {
            // Handle errors such as network issues
            println("Error fetching routes: ${e.message}")
        }
    }

    private val routeUseCase: RouteUseCase by inject()
    private val remoteRepository: RemoteRouteRepository by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    val pageSize = 100
    private var totalLoadOldRoutes = 0
    private val allNewRoutes: MutableList<Route> = mutableListOf<Route>()

    /* Удаляет данные одного Route из B4A Repository в таблицах BasicData Train Locomotive Passenger*/
    private fun removeRouteFromRemoteRepositoryOldMethod(route: Route): Flow<ResultState<Unit>> =
        channelFlow {
            withContext(Dispatchers.IO) {
                trySend(ResultState.Loading())
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
    private fun removeRouteFromRemoteRepositoryNewMethod(route: Route): Flow<ResultState<Unit>> =
        channelFlow {
            withContext(Dispatchers.IO) {
                trySend(ResultState.Loading())
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
    private fun removeRouteList(routeList: List<Route>): Flow<ResultState<Unit>> =
        channelFlow {
            withContext(Dispatchers.IO) {
                trySend(ResultState.Loading())
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
    private fun searchRemovedRoute(): Flow<ResultState<Unit>> {
        return channelFlow {
            withContext(Dispatchers.IO) {
                trySend(ResultState.Loading())
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

    fun synchronizedStorage(): Flow<ResultState<Int>> {
        return channelFlow {
            trySend(ResultState.Loading())
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
        totalLoadOldRoutes = 0
        allNewRoutes.clear()
        return channelFlow {
            trySend(ResultState.Loading())
            val flows = listOf(loadRouteListFromRemoteOldVersion(), loadRouteFromRemoteNewVersion())
            try {
                var totalCount = 0
                val result = withTimeout(TIMEOUT_LOADING) {
                    flows.map { flow ->
                        async {
                            flow.first {
                                it is ResultState.Success
                            }
                        }
                    }.awaitAll()
                }
                result.forEach {
                    if (it is ResultState.Success) {
                        totalCount += it.data
                    }
                }
                trySend(ResultState.Success(totalCount))
            } catch (e: TimeoutCancellationException) {
                trySend(ResultState.Error(ErrorEntity(message = "TimeOut: Время ожидания истекло")))
            } catch (e: Exception) {
                trySend(ResultState.Error(ErrorEntity(message = "Произошла ошибка: ${e.message}")))
            }
            awaitClose()
        }
    }

    /* Выгрузка на сервер несинхронизированных маршрутов
    * возвращает количество синхронизированных маршрутов*/
    private fun saveRouteToRemoteStorage(): Flow<ResultState<Int>> =
        channelFlow {
            trySend(ResultState.Loading())
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
        return channelFlow {
            trySend(ResultState.Loading())
            val flows = listOf(loadBathRouteNewVersionByUser(0), loadBathRouteNewVersionByEmail(0))
            try {
                withTimeout(TIMEOUT_LOADING) {
                    flows.map { flow ->
                        async {
                            flow.first {
                                it is ResultState.Success
                            }
                        }
                    }.awaitAll()
                    saveRouteListAfterLoading().collect { saveListResult ->
                        when (saveListResult) {
                            is ResultState.Success -> {
                                trySend(ResultState.Success(allNewRoutes.size))
                            }

                            is ResultState.Error -> {
                                trySend(ResultState.Error(ErrorEntity(saveListResult.entity.throwable)))
                                return@collect // Terminate the flow on error
                            }

                            is ResultState.Loading -> {
                                trySend(ResultState.Loading()) // Propagate the loading state
                            }
                        }
                    }
                }

            } catch (e: TimeoutCancellationException) {
                trySend(ResultState.Error(ErrorEntity(message = "TimeOut: Время ожидания истекло")))
            } catch (e: Exception) {
                trySend(ResultState.Error(ErrorEntity(message = "Произошла ошибка: ${e.message}")))
            }
            awaitClose()
        }
    }

    private fun saveRouteListAfterLoading(): Flow<ResultState<Unit>> = channelFlow {

        trySend(ResultState.Loading())
        for (route in allNewRoutes) {
            try {
                withTimeout(TIMEOUT_LOADING) {
                    withContext(Dispatchers.IO) {
                        routeUseCase.saveRouteAfterLoading(route).collect { result ->
                            when (result) {
                                is ResultState.Success -> {

                                }

                                is ResultState.Error -> {
                                    trySend(ResultState.Error(ErrorEntity(result.entity.throwable)))
                                    return@collect // Terminate the flow on error
                                }

                                is ResultState.Loading -> {
                                    trySend(ResultState.Loading()) // Propagate the loading state
                                }
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                trySend(ResultState.Error(ErrorEntity(message = "TimeOut: Время ожидания истекло")))
                return@channelFlow // Terminate the flow on timeout
            }

        }
        trySend(ResultState.Success(Unit))
    }

    private fun loadBathRouteNewVersionByUser(skip: Int): Flow<ResultState<Unit>> {
        return channelFlow {
            trySend(ResultState.Loading())
            val parseUserQuery: ParseQuery<ParseObject> =
                ParseQuery(RouteFieldName.ROUTE_CLASS_NAME_REMOTE)
            parseUserQuery.whereEqualTo(
                RouteFieldName.USER_FIELD_NAME,
                ParseUser.getCurrentUser()
            )

            parseUserQuery.setLimit(pageSize)
            parseUserQuery.setSkip(skip)

            parseUserQuery.findInBackground { parseObjects, parseException ->
                if (parseException == null) {
                    if (parseObjects.isEmpty()) {
                        trySend(ResultState.Success(Unit))
                    } else {
                        parseObjects.forEachIndexed { index, parseObject ->
                            parseObject.getString(RouteFieldName.DATA_FIELD_NAME)
                                ?.let { data ->
                                    var route = RouteJSONConverter.fromString(data)
                                    route = route.copy(
                                        basicData = route.basicData.copy(
                                            isSynchronizedRoute = true,
                                            remoteRouteId = parseObject.objectId
                                        )
                                    )
                                    allNewRoutes.add(route)
                                }
                        }
                        if (parseObjects.size == pageSize) {
                            CoroutineScope(Dispatchers.IO).launch {
                                loadBathRouteNewVersionByUser(skip + pageSize).collect {
                                    if (it is ResultState.Success) {
                                        trySend(ResultState.Success(Unit))
                                    }
                                }
                            }
                        } else {
                            trySend(ResultState.Success(Unit))
                        }
                    }
                } else {
                    trySend(ResultState.Error(ErrorEntity(throwable = parseException)))
                }
            }
            awaitClose()
        }
    }

    private fun loadBathRouteNewVersionByEmail(skip: Int): Flow<ResultState<Unit>> {
        return channelFlow {
            trySend(ResultState.Loading())
            val parseEmailQuery: ParseQuery<ParseObject> =
                ParseQuery(RouteFieldName.ROUTE_CLASS_NAME_REMOTE)
            parseEmailQuery.whereEqualTo(
                RouteFieldName.USER_EMAIL_FIELD_NAME,
                ParseUser.getCurrentUser().email
            )

            parseEmailQuery.setLimit(pageSize)
            parseEmailQuery.setSkip(skip)

            parseEmailQuery.findInBackground { parseObjects, parseException ->
                if (parseException == null) {
                    if (parseObjects.isEmpty()) {
                        trySend(ResultState.Success(Unit))
                    } else {
                        parseObjects.forEachIndexed { index, parseObject ->
                            parseObject.getString(RouteFieldName.DATA_FIELD_NAME)
                                ?.let { data ->
                                    var route = RouteJSONConverter.fromString(data)
                                    route = route.copy(
                                        basicData = route.basicData.copy(
                                            isSynchronizedRoute = true,
                                            remoteRouteId = parseObject.objectId
                                        )
                                    )
                                    allNewRoutes.add(route)
                                }
                        }

                        if (parseObjects.size == pageSize) {
                            CoroutineScope(Dispatchers.IO).launch {
                                loadBathRouteNewVersionByEmail(skip + pageSize).collect {
                                    if (it is ResultState.Success) {
                                        trySend(ResultState.Success(Unit))
                                    }
                                }
                            }
                        } else {
                            trySend(ResultState.Success(Unit))
                        }
                    }
                } else {
                    trySend(ResultState.Error(ErrorEntity(throwable = parseException)))
                }
            }
            awaitClose()
        }
    }

    private fun loadRouteListFromRemoteOldVersion(): Flow<ResultState<Int>> = flow {
        totalLoadOldRoutes = 0
        emit(ResultState.Loading())
        loadBathRouteOldVersion(0).collect {
            when (it) {
                is ResultState.Loading -> {
                    emit(ResultState.Loading())
                }

                is ResultState.Error -> {
                    emit(ResultState.Error(ErrorEntity(throwable = it.entity.throwable)))
                }

                is ResultState.Success -> {
                    emit(ResultState.Success(totalLoadOldRoutes))
                }
            }
        }
    }

    private fun loadBathRouteOldVersion(skip: Int): Flow<ResultState<Unit>> {
        val parseQuery: ParseQuery<ParseObject> =
            ParseQuery(BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE)
        parseQuery.whereEqualTo(BasicDataFieldName.USER_FIELD_NAME, ParseUser.getCurrentUser())
        parseQuery.orderByDescending(BasicDataFieldName.BASIC_DATA_UID_FIELD_NAME)
        return channelFlow {
            trySend(ResultState.Loading())
            parseQuery.findInBackground { parseObjects, parseException ->
                if (parseException == null) {
                    if (parseObjects.isEmpty()) {
                        trySend(ResultState.Success(Unit))
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
                                            if (loadRouteResult is ResultState.Error) {
                                                trySend(ResultState.Error(loadRouteResult.entity))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        totalLoadOldRoutes += parseObjects.size
                        if (parseObjects.size == pageSize) {
                            totalLoadOldRoutes += parseObjects.size
                            CoroutineScope(Dispatchers.IO).launch {
                                loadBathRouteOldVersion(skip + pageSize).collect {
                                    trySend(ResultState.Success(Unit))
                                }
                            }
                        } else {
                            totalLoadOldRoutes += parseObjects.size
                            trySend(ResultState.Success(Unit))
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
            trySend(ResultState.Loading())

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
