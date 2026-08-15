package com.z_company.route.viewmodel

import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Route

data class RouteFormUiState(
    val routeDetailState: ResultState<Route?> = ResultState.Loading(),
    val saveRouteState: ResultState<Unit>? = null,
    var exitFromScreen: Boolean = false,
    val changesHaveState: Boolean = false,
    var confirmExitDialogShow: Boolean = false,
    val errorMessage: String? = null,
    val nightTime: Long? = null,
    val passengerTime: Long? = null,
    val isCopy: Boolean = false,
)

data class SalaryForRouteState(
    val isCalculated: Boolean = false,
    val isSetTariffRate: Boolean = false,
    val totalPayment: Double? = null,
    val paymentAtTariffRate: Double? = null,
    val zonalSurchargeMoney: Double? = null,
    val paymentAtNightTime: Double? = null,
    val paymentAtPassengerTime: Double? = null,
    // Оплата проезда пассажиром до явки (по тарифу, отдельно от работы)
    val paymentAtPassengerOutsideTime: Double? = null,
    val paymentHolidayMoney: Double? = null,
    val linearMileageDistance: Double? = null,
    val linearMileageMoney: Double? = null,
    val linearMileageAccruals: List<LinearMileageAccrual> = emptyList(),
    val surchargesAtTrain: Double? = null,
    val paymentAtOnePerson: Double? = null,
    val otherSurcharge: Double? = null,
    val overRestMoney: Double? = null,
    // Оплата за маршрут командировки — только по среднему часу (без надбавок).
    // Когда > 0, остальные строки расчёта равны 0 (маршрут исключён из обычного расчёта).
    val businessTripMoney: Double? = null,
    // true — маршрут попадает в период командировки (оплата только по среднему часу).
    // Может быть true при businessTripMoney == 0, если средний час не задан.
    val isBusinessTrip: Boolean = false,
    // Исходники для пояснений расчёта в шторке
    val tariffRate: Double? = null,
    val workTimeForPay: Long? = null,
    val zonalPercent: Double? = null,
    val zonalTime: Long? = null,
    val nightPercent: Double? = null,
    val onePersonPercent: Double? = null,
    val onePersonTime: Long? = null,
    val overRestTime: Long? = null,
    // Виды применённых доплат за поезд (тяжеловесный, длинносоставный и т.д.)
    val trainSurchargeTypes: List<String> = emptyList(),
)

/**
 * Данные предупреждения «вторая ночь подряд». Причина:
 *  - два ночных маршрута друг за другом, либо
 *  - один маршрут захватывает два ночных окна.
 */
data class NightWarnState(
    val rows: List<NightWarnRow>,
)

data class NightWarnRow(
    val label: String,
    val start: Long,
    val end: Long,
    val nightMillis: Long,
    // Интервалы ночных окон внутри маршрута (абсолютные ms) — для графической линии
    val nightIntervals: List<Pair<Long, Long>> = emptyList(),
)
