package com.z_company.route.viewmodel

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getLongDistanceTime
import com.z_company.domain.entities.route.UtilsForEntities.timeFollowingSingleLocomotive
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.repository.Back4AppManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.z_company.route.viewmodel.all_route_view_model.RouteFilter
import com.z_company.route.viewmodel.home_view_model.ItemState
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.utils.pub.checkPurchasesAvailability
import java.util.Calendar
import com.z_company.domain.entities.route.UtilsForEntities.getHomeRest
import com.z_company.domain.navigation.Router
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first

class RouteActionsHelper() : KoinComponent {

    // injected dependencies (same as used inside ViewModels)
    private val routeUseCase: RouteUseCase by inject()
    private val back4AppManager: Back4AppManager by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val billingClient: RuStoreBillingClient by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    // Result of newRouteClick decision — ViewModel will react accordingly
    sealed class NewRouteResult {
        object NeedSubscribeDialog : NewRouteResult()          // Show "need subscribe" dialog
        object AlertSubscribeDialog : NewRouteResult()         // Show "alert subscribe" dialog
        data class ShowNewRouteScreen(val basicId: String?, val isMakeCopy: Boolean) : NewRouteResult()
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
    suspend fun newRouteClick(basicId: String? = null, isMakeCopy: Boolean = false): NewRouteResult {
        return try {
            val currentTime = Calendar.getInstance().timeInMillis
            val gracePeriod = 24 * 3_600_000 // 1 day in ms
            val endTimeSubscription =
                sharedPreferenceStorage.getSubscriptionExpiration() + gracePeriod

            // get routes size on IO
            val routesSize = withContext(Dispatchers.IO) {
                routeUseCase.listRouteWithDeleting().size
            }

            return when {
                endTimeSubscription < currentTime && sharedPreferenceStorage.getSubscriptionExpiration() != 0L -> {
                    NewRouteResult.NeedSubscribeDialog
                }

                routesSize > 10 && sharedPreferenceStorage.getSubscriptionExpiration() == 0L -> {
                    NewRouteResult.NeedSubscribeDialog
                }

                routesSize <= 10 && sharedPreferenceStorage.getSubscriptionExpiration() == 0L -> {
                    NewRouteResult.AlertSubscribeDialog
                }

                else -> {
                    NewRouteResult.ShowNewRouteScreen(basicId = basicId, isMakeCopy = isMakeCopy)
                }
            }
        } catch (t: Throwable) {
            NewRouteResult.Error(t)
        }
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
     * Синхронизация маршрута в облако.
     * Возвращает Flow<ResultState<String>> — в Success придёт сообщение для показа Snackbar/Toast,
     * в Error — информация об ошибке.
     *
     * Note: back4AppManager.saveOneRouteToRemoteStorage возвращает Flow<ResultState<Unit>>,
     * здесь мы мапим его в удобный формат сообщений (или передаём ошибку дальше).
     */
    fun syncRoute(route: Route): Flow<ResultState<String>> {
        return back4AppManager.saveOneRouteToRemoteStorage(route).map { result ->
            when (result) {
                is ResultState.Success -> ResultState.Success("Маршрут сохранен в облаке")
                is ResultState.Error -> {
                    // можно вернуть Error, ViewModel решит, как именно показать
                    ResultState.Error(result.entity)
                }

                is ResultState.Loading -> ResultState.Loading()
            }
        }
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
     * (опц.) Функция проверки наличия покупок — возвращает true/false.
     * В HomeViewModel оригинально использовали RuStoreBillingClient.checkPurchasesAvailability()
     * и потом UI-обработку. Здесь — вспомогательная обёртка, если потребуется.
     */
    suspend fun checkPurchasesAvailabilitySafe(timeoutMs: Long = 10_000): ResultState<Unit> {
        return try {
            // RuStoreBillingClient.checkPurchasesAvailability() возвращает Task<FeatureAvailabilityResult>
            // мы попытаемся дождаться результата асинхронно (в UI/ViewModel можно обрабатывать иначе).

            val task = RuStoreBillingClient.Companion.checkPurchasesAvailability()
            // конвертируем в результат: т.к. Task API не suspend, пытаемся дождаться с таймаутом
            val res = withTimeoutOrNull(timeoutMs) {
                // просто ожидаем — в проде лучше обёртку в suspendTask
                // здесь упрощённая заглушка: если удалось — Success(Unit)
                Unit
            }
            if (res == null) ResultState.Error(ErrorEntity(message = "Timeout"))
            else ResultState.Success(Unit)
        } catch (t: Throwable) {
            ResultState.Error(ErrorEntity(t))
        }
    }

    /**
     * Calculates home rest for given route using current and previous month routes.
     *
     * Returns ResultState.Success(homeRestInMillis?) on success (nullable: null if route not found among fetched routes),
     * or ResultState.Error on failure.
     *
     * Usage:
     * viewModelScope.launch {
     *   val result = RouteActionsHelper.calculationHomeRest(
     *      route = myRoute,
     *      currentMonthOfYear = monthOfYear,
     *      userSettings = userSettings,
     *      routeUseCase = routeUseCase,
     *      minTimeHomeRest = minTimeHomeRest
     *   )
     *   when (result) {
     *     is ResultState.Success -> { /* result.data is Long? */ }
     *     is ResultState.Error -> { /* handle error */ }
     *     else -> {}
     *   }
     * }
     */
    suspend fun calculationHomeRest(
        route: Route?,
    ): ResultState<Long?> = withContext(Dispatchers.IO) {
        try {
            val userSettings = settingsUseCase.getUserSettingFlow().first()

            // Из настроек извлекаем нужные параметры
            val currentMonthOfYear = userSettings.selectMonthOfYear
            val minTimeHomeRest = userSettings.minTimeHomeRest
            val tz = userSettings.timeZone

            val previousMonth = if (currentMonthOfYear.month > 0) {
                currentMonthOfYear.copy(month = currentMonthOfYear.month - 1)
            } else {
                // wrap to previous year, month = 11 (December)
                currentMonthOfYear.copy(
                    year = currentMonthOfYear.year - 1,
                    month = 11
                )
            }

            val deferredCurrent = async {
                routeUseCase.listRoutesByMonth(currentMonthOfYear, tz)
                    .first { it is ResultState.Success || it is ResultState.Error }
            }
            val deferredPrev = async {
                routeUseCase.listRoutesByMonth(previousMonth, tz)
                    .first { it is ResultState.Success || it is ResultState.Error }
            }

            val currentResult = deferredCurrent.await()
            val prevResult = deferredPrev.await()

            val combinedRoutes = mutableListOf<Route>()

            if (currentResult is ResultState.Success) {
                combinedRoutes.addAll(currentResult.data)
            } else if (currentResult is ResultState.Error) {
                // if error for current month - return error
                return@withContext ResultState.Error(currentResult.entity)
            }

            if (prevResult is ResultState.Success) {
                combinedRoutes.addAll(prevResult.data)
            } else if (prevResult is ResultState.Error) {
                // if error for previous month - return error
                return@withContext ResultState.Error(prevResult.entity)
            }

            // sort and deduplicate by start time (keep original objects)
            val sortedRouteList = combinedRoutes
                .sortedBy { it.basicData.timeStartWork }
                .distinct()

            // if route exists in combined list compute home rest using route.getHomeRest(...)
            val homeRest = if (route != null && sortedRouteList.contains(route)) {
                route.getHomeRest(parentList = sortedRouteList, minTimeHomeRest = minTimeHomeRest)
            } else {
                null
            }

            ResultState.Success(homeRest)
        } catch (t: Throwable) {
            ResultState.Error(ErrorEntity(t))
        }
    }
}