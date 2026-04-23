package com.z_company.domain.helpers

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.isTimeWorkValid
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * KMP-вариант методов расчёта отдыха из Android-класса RouteActionsHelper.
 *
 * Зависимости передаются через конструктор (вместо Koin), чтобы класс жил в
 * commonMain. Используется Form/Home ViewModel-ами на iOS для отображения
 * типа и длительности отдыха.
 */
class RouteRestCalculator(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
) {
    /**
     * Детали расчёта домашнего отдыха.
     *
     * @property duration        продолжительность домашнего отдыха
     * @property endTime         время окончания отдыха
     * @property chainWorkTotal  суммарная продолжительность работы цепочки маршрутов
     * @property chainPORestTotal суммарный отдых в ПО между маршрутами цепочки
     *                             (0, если в цепочке всего один маршрут)
     * @property chainSize       количество маршрутов в цепочке (>= 1)
     */
    data class HomeRestDetails(
        val duration: Long,
        val endTime: Long,
        val chainWorkTotal: Long,
        val chainPORestTotal: Long,
        val chainSize: Int,
    )

    /**
     * Возвращает Flow<ResultState<HomeRestDetails?>>.
     */
    fun calculationHomeRest(route: Route?): Flow<ResultState<HomeRestDetails?>> = flow {
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
                val deferredCurrent = async {
                    routeUseCase.listRoutesByMonth(currentMonthOfYear, tz)
                        .first { it is ResultState.Success || it is ResultState.Error }
                }
                val deferredPrev = async {
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

            val endTime = route.basicData.timeEndWork!! + duration

            emit(
                ResultState.Success(
                    HomeRestDetails(
                        duration = duration,
                        endTime = endTime,
                        chainWorkTotal = sumWork,
                        chainPORestTotal = sumRest,
                        chainSize = chain.size,
                    )
                )
            )
        } catch (ce: CancellationException) {
            // AbortFlowException (внутренний механизм .first{}) и обычная отмена
            // корутины — не наша ошибка, просто пробрасываем дальше.
            throw ce
        } catch (t: Throwable) {
            emit(ResultState.Error(ErrorEntity(t)))
        }
    }

    /** Короткий отдых в оборотном пункте. */
    fun calculateShortRest(route: Route?): Flow<Pair<Long, Long>?> = flow {
        if (route == null) {
            emit(null); return@flow
        }
        val startTime = route.basicData.timeStartWork
        val endTime = route.basicData.timeEndWork
        if (startTime == null || endTime == null) {
            emit(null); return@flow
        }
        if (!route.isTimeWorkValid()) {
            emit(null); return@flow
        }

        val userSettings = settingsUseCase.getUserSettingFlow().first()
        val minTimeRestPointOfTurnover = userSettings.minTimeRestPointOfTurnover

        val timeResult = endTime - startTime
        var halfRest = timeResult / 2
        if (halfRest % 60_000L != 0L) halfRest += 60_000L

        val effectiveRest =
            if (halfRest > minTimeRestPointOfTurnover) halfRest else minTimeRestPointOfTurnover
        val endRestTime = endTime + effectiveRest

        emit(Pair(effectiveRest, endRestTime))
    }

    /** Полный отдых в оборотном пункте. */
    fun calculateFullRest(route: Route?): Flow<Pair<Long, Long>?> = flow {
        if (route == null) {
            emit(null); return@flow
        }
        val startTime = route.basicData.timeStartWork
        val endTime = route.basicData.timeEndWork
        if (startTime == null || endTime == null) {
            emit(null); return@flow
        }
        if (!route.isTimeWorkValid()) {
            emit(null); return@flow
        }

        val userSettings = settingsUseCase.getUserSettingFlow().first()
        val minTimeRestPointOfTurnover = userSettings.minTimeRestPointOfTurnover

        val timeResult = endTime - startTime
        val effectiveRest =
            if (timeResult > minTimeRestPointOfTurnover) timeResult else minTimeRestPointOfTurnover
        val endRestTime = endTime + effectiveRest

        emit(Pair(effectiveRest, endRestTime))
    }
}
