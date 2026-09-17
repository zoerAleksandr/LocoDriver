package com.z_company.route.viewmodel

import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.OverRestRoutes.overRestPayment
import com.z_company.domain.entities.route.OverRestRoutes.previousRouteFor
import com.z_company.domain.entities.route.UtilsForEntities.getPureWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.passengerTrainNumberList
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.util.toIntOrZero
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Итог оплаты за одну смену (маршрут) — то же суммирование, что и в
 * [FormViewModel.calculateSalary] (переменная totalMoney). Месячные величины
 * (переработка, недоработка, средний час месяца) сюда НЕ входят: развёрнутая
 * карточка показывает начисление именно по этому плечу («Расчёт за смену»).
 *
 * Считается через [SalaryCalculationHelper] с `allRoutes = listOf(route)`, как
 * это делает форма при редактировании одного маршрута.
 *
 * @param candidateRoutes маршруты, среди которых ищется предыдущий по явке
 *   (для доплаты за переотдых): маршруты месяца плюс последний маршрут
 *   предыдущего месяца (`RouteUseCase.adjacentRoutesOfMonthFlow`), чтобы
 *   первый маршрут месяца видел отдых в ПО в конце прошлого — как FormViewModel.
 * @return сумма в рублях/валюте пользователя, либо null если по маршруту нельзя
 *   посчитать время работы (пустой маршрут).
 */
suspend fun computeRouteTotalPayment(
    route: Route,
    userSettings: UserSettings,
    salarySetting: SalarySetting,
    candidateRoutes: List<Route>,
): Double? {
    if (route.getPureWorkTime() == null) return null

    val helper = SalaryCalculationHelper(
        userSettings = userSettings,
        salarySetting = salarySetting,
        allRoutes = listOf(route),
    )

    return coroutineScope {
        val tariff = async {
            helper.getMoneyAtWorkTimeAtTariffSingleRoute().first().coerceAtLeast(0.0)
        }
        val night = async { helper.getMoneyAtNightTimeFlow().first() }
        val zonal = async { helper.getMoneyZonalSurchargeFlow().first() }
        val passenger = async { helper.getMoneyAtPassengerFlow().first() }
        val passengerOutside = async { helper.getMoneyAtPassengerOutsideWorkFlow().first() }
        // Ожидание следования пассажиром — по тарифу, отдельно от «работы по тарифу»
        // (та его больше не включает). Входит в каноническую формулу базы.
        val passengerWaiting = async { helper.getMoneyAtPassengerWaitingFlow().first() }
        // Следование резервом (одиночный локомотив) — как и «Пассажиром»,
        // отдельное начисление сверх тарифа. Входит в каноническую формулу
        // SalaryCalculationHelper.getBasicMoneyForOvertimeCalculation.
        val singleLocomotive = async { helper.getMoneyAtSingleLocomotiveFlow().first() }
        val holiday = async { helper.getMoneyAtHolidayFlow().first() }
        val servicePhase = async {
            helper.getMoneyListSurchargeExtendedServicePhaseFlow().first().sum()
        }
        val heavy = async { helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum() }
        val longTrain = async { helper.getMoneyListSurchargeLongTrainsFlow().first().sum() }
        val doubledFirst = async {
            helper.getMoneyDoubledTrainFirstSurchargeFlow(listOf(route)).first()
        }
        val doubledSecond = async {
            helper.getMoneyDoubledTrainSecondSurchargeFlow(listOf(route)).first()
        }
        val qualification = async { helper.getMoneyAtQualificationClassFlow().first() }
        val nordic = async { helper.getMoneyNordicSurcharge().first() }
        val district = async { helper.getMoneyDistrictSurcharge().first() }
        val harmfulness = async { helper.getMoneyHarmfulnessFlow().first() }
        val other = async { helper.getMoneyOtherSurchargeFlow().first() }
        val businessTrip = async { helper.getMoneyBusinessTripFlow().first() }

        // «В одно лицо» считается по разному тарифу для пассажирских поездов.
        val isPassengerTrain = route.trains.any { train ->
            passengerTrainNumberList.any { it.contains(train.number.toIntOrZero()) }
        }
        val onePerson = async {
            if (isPassengerTrain) {
                helper.getMoneyOnePersonOperationPassengerTrainFlow().first()
            } else {
                helper.getMoneyOnePersonOperationFlow().first()
            }
        }

        // Переотдых: доплата 2/3 тарифа за часы сверх нормы отдыха в пункте
        // оборота ближайшего по явке предыдущего маршрута (как в FormViewModel).
        val overRestMoney = route.overRestPayment(
            previous = candidateRoutes.previousRouteFor(route),
            minTimeRest = userSettings.minTimeRestPointOfTurnover,
            tariffRate = userSettings.selectMonthOfYear.tariffRate,
        ).money

        tariff.await() + night.await() + zonal.await() + passenger.await() +
            passengerWaiting.await() +
            passengerOutside.await() + singleLocomotive.await() + holiday.await() + servicePhase.await() +
            heavy.await() + longTrain.await() + doubledFirst.await() + doubledSecond.await() +
            onePerson.await() + qualification.await() + nordic.await() + district.await() +
            harmfulness.await() + other.await() + overRestMoney + businessTrip.await()
    }
}
