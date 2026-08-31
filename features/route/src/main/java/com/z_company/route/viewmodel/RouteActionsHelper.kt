package com.z_company.route.viewmodel

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getLongDistanceTime
import com.z_company.domain.entities.route.UtilsForEntities.timeFollowingSingleLocomotive
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.repository.SecureTokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.z_company.route.viewmodel.all_route_view_model.RouteFilter
import com.z_company.route.viewmodel.home_view_model.ItemState
import java.util.Calendar
import com.z_company.domain.entities.route.UtilsForEntities.isTimeWorkValid
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

data class HomeRestCalculation(
    val duration: Long,
    val endTime: Long,
    val minEndTime: Long,
)

class RouteActionsHelper() : KoinComponent {

    companion object {
        /** Лимит бесплатных маршрутов без подписки. Используется и в проверке
         * при создании маршрута, и в индикаторе на главном экране. */
        const val FREE_ROUTES_LIMIT = 20
    }

    // injected dependencies (same as used inside ViewModels)
    private val routeUseCase: RouteUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()

    // Result of newRouteClick decision — ViewModel will react accordingly
    sealed class NewRouteResult {
        object NeedSubscribeDialog : NewRouteResult()          // Show "need subscribe" dialog
        /** Лимит ещё не исчерпан: [freeRoutesLeft] маршрутов доступно бесплатно. */
        data class AlertSubscribeDialog(val freeRoutesLeft: Int) : NewRouteResult()
        data class ShowNewRouteScreen(val basicId: String?, val isMakeCopy: Boolean) :
            NewRouteResult()

        data class Error(val throwable: Throwable?) : NewRouteResult()
    }

    /**
     * Решение о том, что делать при попытке создания нового маршрута.
     *
     * Повторяет логику из HomeViewModel.newRouteClick:
     * - проверяет дату окончания подписки (+ грейс-период)
     * - считает количество локальных маршрутов
     * - возвращает одно из NewRouteResult
     *
     * Этот метод не меняет uiState — ViewModel делает это сама по результату.
     */
    suspend fun newRouteClick(
        basicId: String? = null,
        isMakeCopy: Boolean = false
    ): NewRouteResult {
        return try {
            val countFreeRoutes = FREE_ROUTES_LIMIT
            val currentTime = Calendar.getInstance().timeInMillis
            val gracePeriod = 24 * 3_600_000 // 1 day in ms
            val setting = settingsUseCase.getUserSettingFlow().first()
            val time = setting.subscriptionPeriod

            val endTimeSubscription = time + gracePeriod

            // Подписка активна: оформлена (time != 0) и не истекла (с учётом грейса).
            // Активная подписка снимает лимит бесплатных маршрутов полностью.
            val subscriptionActive = time != 0L && endTimeSubscription >= currentTime

            // get routes size on IO
            val routesSize = withContext(Dispatchers.IO) {
                routeUseCase.listRouteWithDeleting().size
            }

            return when {
                subscriptionActive -> {
                    NewRouteResult.ShowNewRouteScreen(basicId = basicId, isMakeCopy = isMakeCopy)
                }

                // Без активной подписки (не куплена ИЛИ истекла) лимит один и тот же:
                // 20 бесплатных маршрутов. 20-й уже создан → 21-й недоступен.
                routesSize >= countFreeRoutes -> {
                    NewRouteResult.NeedSubscribeDialog
                }

                else -> {
                    NewRouteResult.AlertSubscribeDialog(
                        freeRoutesLeft = (countFreeRoutes - routesSize).coerceAtLeast(0)
                    )
                }
            }
        } catch (t: Throwable) {
            NewRouteResult.Error(t)
        }
    }

    /**
     * Активна ли подписка на текущий момент.
     *
     * Единый критерий для гейта синхронизации (та же логика, что в
     * [SyncWorker] и в [ProfileViewModel.hasSubscription]): синхронизация —
     * платная функция, поэтому любой ручной upload в облако (полный или
     * по одному маршруту) должен быть заблокирован, если подписка не
     * оформлена (`subscriptionPeriod == 0`) или истекла (`< now`).
     *
     * Без грейс-периода — в отличие от [newRouteClick], где грейс нужен,
     * чтобы дать домотать локальную работу: здесь речь о записи на сервер.
     */
    suspend fun hasActiveSubscription(): Boolean {
        val setting = settingsUseCase.getUserSettingFlow().first()
        return setting.subscriptionPeriod > Calendar.getInstance().timeInMillis
    }

    /**
     * Результат проверки перед созданием **пачки** маршрутов (Календарь,
     * мастер «Заполнить месяц»).
     *
     * [newRouteClick] отвечает на вопрос «можно ли создать ещё один маршрут» и
     * для пачки не годится: с нулём маршрутов он разрешает создание, а дальше
     * цикл сохраняет хоть 30 штук и бесплатный лимит обходится целиком.
     */
    sealed class BatchRoutesResult {
        /**
         * Пачку можно создавать. [freeRoutesLeftAfter] — сколько бесплатных
         * маршрутов останется после создания; `null`, если подписка активна
         * (лимита нет).
         */
        data class Allowed(val freeRoutesLeftAfter: Int?) : BatchRoutesResult()

        /** Бесплатного лимита не хватает: запрошено [requested], свободно [remaining]. */
        data class LimitExceeded(val requested: Int, val remaining: Int) : BatchRoutesResult()

        data class Error(val throwable: Throwable?) : BatchRoutesResult()
    }

    /**
     * Сколько маршрутов ещё можно создать бесплатно.
     * `null` — подписка активна, лимита нет.
     *
     * Критерий подписки тот же, что в [newRouteClick] (с грейс-периодом), чтобы
     * пачка и ручное создание вели себя одинаково.
     */
    suspend fun freeRoutesLeft(): Int? {
        val setting = settingsUseCase.getUserSettingFlow().first()
        val time = setting.subscriptionPeriod
        val gracePeriod = 24 * 3_600_000 // 1 day in ms
        val subscriptionActive =
            time != 0L && time + gracePeriod >= Calendar.getInstance().timeInMillis
        if (subscriptionActive) return null
        val routesSize = freeRoutesUsedCount()
        return (FREE_ROUTES_LIMIT - routesSize).coerceAtLeast(0)
    }

    /**
     * Можно ли создать [count] маршрутов разом. Без подписки пачка создаётся
     * только целиком и только если помещается в остаток бесплатного лимита —
     * частичное создание дало бы пользователю неполный график без объяснений.
     */
    suspend fun canCreateRoutes(count: Int): BatchRoutesResult {
        return try {
            val left = freeRoutesLeft()
                ?: return BatchRoutesResult.Allowed(freeRoutesLeftAfter = null)
            if (count <= left) {
                BatchRoutesResult.Allowed(freeRoutesLeftAfter = left - count)
            } else {
                BatchRoutesResult.LimitExceeded(requested = count, remaining = left)
            }
        } catch (t: Throwable) {
            BatchRoutesResult.Error(t)
        }
    }

    /**
     * Есть ли действующая авторизация — сохранён непустой bearer-токен.
     *
     * Нужна перед переходом на экран покупок: подписка живёт на сервере и
     * привязывается к аккаунту. Без входа оплата пройдёт, но новый
     * `subscriptionPeriod` некому синхронизировать в приложение — срок
     * не обновится. Поэтому неавторизованного пользователя ведём в Профиль
     * (см. `rememberShowPurchasesScreen`).
     */
    suspend fun isAuthorized(): Boolean {
        val token = secureTokenStorage.getAuthBearerTokenFlow().first()
        return !token.isNullOrBlank()
    }

    /**
     * Сколько маршрутов уже «потрачено» из бесплатного лимита [FREE_ROUTES_LIMIT].
     * Считает так же, как [newRouteClick] — с учётом удалённых (см. комментарий
     * там про "20-й уже создан → 21-й недоступен"), чтобы индикатор на главном
     * экране совпадал с реальным поведением при попытке создать маршрут.
     */
    suspend fun freeRoutesUsedCount(): Int = withContext(Dispatchers.IO) {
        routeUseCase.listRouteWithDeleting().size
    }

    /**
     * Делает/снимает favorite у маршрута.
     * Возвращает Flow<ResultState<Boolean>> как делал routeUseCase.
     */
    fun setFavoriteRoute(route: Route): Flow<ResultState<Boolean>> {
        val id = route.basicData.id
        val newState = !route.basicData.isFavorite
        return routeUseCase.setFavoriteRoute(id, newState)
    }

    /**
     * Общая фильтрация маршрутов — можно использовать в AllRouteViewModel (и где угодно).
     * Конкретная логика скопирована из AllRouteViewModel.applyFilters.
     */
    fun applyFilters(
        routesState: List<ItemState>,
        filters: Set<RouteFilter>,
        salarySetting: SalarySetting
    ): List<ItemState> {
        if (filters.contains(RouteFilter.ALL)) return routesState

        val over12hMillis = 43_200_000L

        return routesState.filter { routeState ->

            var ok = true

            if (filters.contains(RouteFilter.FAVORITES)) {
                ok = ok && (routeState.route.basicData?.isFavorite == true)
            }
            if (filters.contains(RouteFilter.HEAVY)) {
                ok = ok && runCatching {
                    com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains(
                        salarySetting,
                        routeState.route
                    )
                }.getOrDefault(false)
            }
            if (filters.contains(RouteFilter.EXTENDED_SERVICE)) {
                ok = ok && runCatching {
                    com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains(
                        salarySetting,
                        routeState.route
                    )
                }.getOrDefault(false)
            }
            if (filters.contains(RouteFilter.LONG_TRAINS)) {
                ok =
                    ok && runCatching {
                        routeState.route.getLongDistanceTime(/* lengthIsLongDistance: Int */0) > 0L
                    }.getOrDefault(
                        false
                    )
            }
            if (filters.contains(RouteFilter.FOLLOWING_RESERVE)) {
                val has = routeState.route.trains.any { train ->
                    runCatching {
                        train.timeFollowingSingleLocomotive(
                            routeState.route.basicData?.timeStartWork,
                            routeState.route.basicData?.timeEndWork
                        )
                    }.getOrDefault(0L) > 0L
                }
                ok = ok && has
            }
            if (filters.contains(RouteFilter.ONE_PERSON)) {
                ok = ok && (routeState.route.basicData?.isOnePersonOperation == true)
            }
            if (filters.contains(RouteFilter.OVER_12_HOURS)) {
                val start = routeState.route.basicData?.timeStartWork ?: 0L
                val end = routeState.route.basicData?.timeEndWork ?: 0L
                ok = ok && (end > start && (end - start) > over12hMillis)
            }
            ok
        }
    }

    /**
     * Возвращает Flow<ResultState<Pair<Long, Long>?>>, где first - продолжительность отдыха, а second - время окончания отдыха
     *
     * Usage:
     * viewModelScope.launch {
     *   routeHelper.calculationHomeRest(myRoute).collect { result ->
     *     when (result) {
     *       is ResultState.Success -> { /* result.data is Pair<Long, Long>? */ }
     *       is ResultState.Error -> { /* handle error */ }
     *       else -> {}
     *     }
     *   }
     * }
     */
    fun calculationHomeRest(route: Route?): Flow<ResultState<HomeRestCalculation?>> = flow {
        emit(ResultState.Loading())
        try {
            if (route == null) {
                emit(ResultState.Success(null))
                return@flow
            }
            if (route.basicData.timeStartWork == null || route.basicData.timeEndWork == null) {
                emit(ResultState.Success(null))
                return@flow
            }

            val userSettings = settingsUseCase.getUserSettingFlow().first()

            val currentMonthOfYear = userSettings.selectMonthOfYear
            val minTimeHomeRest = userSettings.minTimeHomeRest
            val tz = userSettings.timeZone

            val previousMonth = if (currentMonthOfYear.month > 0) {
                currentMonthOfYear.copy(month = currentMonthOfYear.month - 1)
            } else {
                currentMonthOfYear.copy(year = currentMonthOfYear.year - 1, month = 11)
            }

            val currentResult: ResultState<List<Route>>
            val prevResult: ResultState<List<Route>>

            coroutineScope {
                val deferredCurrent = async(Dispatchers.IO) {
                    routeUseCase.listRoutesByMonth(currentMonthOfYear, tz)
                        .first { it is ResultState.Success || it is ResultState.Error }
                }
                val deferredPrev = async(Dispatchers.IO) {
                    routeUseCase.listRoutesByMonth(previousMonth, tz)
                        .first { it is ResultState.Success || it is ResultState.Error }
                }

                currentResult = deferredCurrent.await()
                prevResult = deferredPrev.await()
            }

            if (currentResult is ResultState.Error) {
                emit(currentResult)
                return@flow
            }
            if (prevResult is ResultState.Error) {
                emit(prevResult)
                return@flow
            }

            val combinedRoutes =
                (currentResult as ResultState.Success).data + (prevResult as ResultState.Success).data

            val sorted = combinedRoutes.sortedByDescending { it.basicData.timeStartWork ?: 0L }
                .toMutableList()

            val inputId = route.basicData.id
            val existingIndex = sorted.indexOfFirst { it.basicData.id == inputId }

            if (existingIndex != -1) {
                sorted[existingIndex] = route
            } else {
                val insertIndex = sorted.indexOfFirst {
                    (it.basicData.timeStartWork ?: 0L) < (route.basicData.timeStartWork ?: 0L)
                }
                if (insertIndex == -1) {
                    sorted.add(route)
                } else {
                    sorted.add(insertIndex, route)
                }
            }


            val index = sorted.indexOfFirst { it.basicData.id == inputId }
            if (index == -1) {
                emit(ResultState.Error(ErrorEntity(Exception("Route not found after insertion"))))
                return@flow
            }

            val chain = mutableListOf<Route>(sorted[index])

            var nextIdx = index + 1
            while (nextIdx < sorted.size) {
                if (sorted[nextIdx].basicData.restPointOfTurnover == true) {
                    chain.add(sorted[nextIdx])
                    nextIdx++
                } else {
                    break
                }
            }

            if (chain.any { it.basicData.timeStartWork == null || it.basicData.timeEndWork == null }) {
                emit(ResultState.Error(ErrorEntity(message = "В цепочке маршрутов не указано начало или окончание работы. Невозможно рассчитать отдых.")))
                return@flow
            }

            val sumWork = chain.sumOf { it.basicData.timeEndWork!! - it.basicData.timeStartWork!! }

            var sumRest = 0L
            for (i in 1 until chain.size) {
                sumRest += chain[i - 1].basicData.timeStartWork!! - chain[i].basicData.timeEndWork!!
            }

            val rawDuration = (sumWork.toDouble() * 2.6).toLong()
            val duration = maxOf(rawDuration - sumRest, minTimeHomeRest)

            val minEndTime = route.basicData.timeEndWork!! + minTimeHomeRest
            val endTime = route.basicData.timeEndWork!! + duration

            emit(ResultState.Success(HomeRestCalculation(duration, endTime, minEndTime)))
        } catch (c: kotlin.coroutines.cancellation.CancellationException) {
            // Отмена коллектора (collectLatest / .first()) бросает CancellationException
            // (в т.ч. AbortFlowException) через emit — её нельзя глотать и переэмитить,
            // иначе «Flow exception transparency is violated». Пробрасываем дальше.
            throw c
        } catch (t: Throwable) {
            emit(ResultState.Error(ErrorEntity(t)))
        }
    }

    /**
     * Фактический отдых по расписанию: время до **следующей явки** после сдачи
     * текущего маршрута. Возвращает `Pair(first = продолжительность, second =
     * момент следующей явки)`, либо `null`, если следующего маршрута нет.
     * Следующий маршрут ищется среди текущего и следующего месяца (граница месяца).
     */
    fun calculationActualRest(route: Route?): Flow<ResultState<Pair<Long, Long>?>> = flow {
        emit(ResultState.Loading())
        try {
            val endWork = route?.basicData?.timeEndWork
            val startWork = route?.basicData?.timeStartWork
            if (route == null || endWork == null || startWork == null) {
                emit(ResultState.Success(null))
                return@flow
            }

            val userSettings = settingsUseCase.getUserSettingFlow().first()
            val currentMonthOfYear = userSettings.selectMonthOfYear
            val tz = userSettings.timeZone
            val nextMonth = if (currentMonthOfYear.month < 11) {
                currentMonthOfYear.copy(month = currentMonthOfYear.month + 1)
            } else {
                currentMonthOfYear.copy(year = currentMonthOfYear.year + 1, month = 0)
            }

            val currentResult: ResultState<List<Route>>
            val nextResult: ResultState<List<Route>>
            coroutineScope {
                val deferredCurrent = async(Dispatchers.IO) {
                    routeUseCase.listRoutesByMonth(currentMonthOfYear, tz)
                        .first { it is ResultState.Success || it is ResultState.Error }
                }
                val deferredNext = async(Dispatchers.IO) {
                    routeUseCase.listRoutesByMonth(nextMonth, tz)
                        .first { it is ResultState.Success || it is ResultState.Error }
                }
                currentResult = deferredCurrent.await()
                nextResult = deferredNext.await()
            }

            val currentList = (currentResult as? ResultState.Success)?.data ?: emptyList()
            val nextList = (nextResult as? ResultState.Success)?.data ?: emptyList()
            val combined = currentList + nextList

            // Следующая явка — минимальный timeStartWork строго позже начала текущего
            // маршрута (сам маршрут исключаем по id).
            val nextStartWork = combined
                .asSequence()
                .filter { it.basicData.id != route.basicData.id }
                .mapNotNull { it.basicData.timeStartWork }
                .filter { it > startWork }
                .minOrNull()

            if (nextStartWork != null && nextStartWork > endWork) {
                emit(ResultState.Success(Pair(nextStartWork - endWork, nextStartWork)))
            } else {
                emit(ResultState.Success(null))
            }
        } catch (c: kotlin.coroutines.cancellation.CancellationException) {
            throw c
        } catch (t: Throwable) {
            emit(ResultState.Error(ErrorEntity(t)))
        }
    }

    /** Возвращает Flow<Pair<Long, Long>?>, где first - продолжительность отдыха, а second - время окончания отдыха
     */
    fun calculateShortRest(route: Route?): Flow<Pair<Long, Long>?> = flow {
        if (route == null) {
            emit(null)
            return@flow
        }

        val startTime = route.basicData.timeStartWork
        val endTime = route.basicData.timeEndWork

        if (startTime == null || endTime == null) {
            emit(null)
            return@flow
        }

        if (!route.isTimeWorkValid()) {
            emit(null)
            return@flow
        }

        val userSettings = settingsUseCase.getUserSettingFlow().first()
        val minTimeRestPointOfTurnover = userSettings.minTimeRestPointOfTurnover

        // Отдых считаем от полного отработанного времени (с учётом перерыва
        // и проезда пассажиром до явки), а не от «сдача − явка».
        val timeResult = route.getWorkTime() ?: (endTime - startTime)
        var halfRest = timeResult / 2

        if (halfRest % 60_000L != 0L) {
            halfRest += 60_000L
        }

        val effectiveRest =
            if (halfRest > minTimeRestPointOfTurnover) halfRest else minTimeRestPointOfTurnover
        val endRestTime = endTime + effectiveRest

        emit(Pair(effectiveRest, endRestTime))
    }


    /** Возвращает Flow<Pair<Long, Long>?>, где first - продолжительность отдыха, а second - время окончания отдыха
     */
    fun calculateFullRest(route: Route?): Flow<Pair<Long, Long>?> = flow {
        if (route == null) {
            emit(null)
            return@flow
        }

        val startTime = route.basicData.timeStartWork
        val endTime = route.basicData.timeEndWork

        if (startTime == null || endTime == null) {
            emit(null)
            return@flow
        }

        if (!route.isTimeWorkValid()) {
            emit(null)
            return@flow
        }

        val userSettings = settingsUseCase.getUserSettingFlow().first()
        val minTimeRestPointOfTurnover = userSettings.minTimeRestPointOfTurnover

        // Полный отдых в ПО равен всему отработанному времени (с учётом перерыва
        // и проезда пассажиром до явки), но не меньше минимального отдыха.
        val timeResult = route.getWorkTime() ?: (endTime - startTime)
        val effectiveRest =
            if (timeResult > minTimeRestPointOfTurnover) timeResult else minTimeRestPointOfTurnover
        val endRestTime = endTime + effectiveRest

        emit(Pair(effectiveRest, endRestTime))
    }
}
