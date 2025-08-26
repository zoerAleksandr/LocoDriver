package com.z_company.route.viewmodel.all_route_view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.isCurrentRoute
import com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHolidayTimeInRoute
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.lessThan
import com.z_company.domain.util.moreThan
import com.z_company.route.viewmodel.home_view_model.HomeUiState
import com.z_company.route.viewmodel.home_view_model.ItemState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import java.util.TimeZone

class AllRouteViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()

    private val _uiState = MutableStateFlow(AllRouteUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var dateAndTimeConverter: DateAndTimeConverter
    private lateinit var userSettings: UserSettings
    private lateinit var salarySetting: SalarySetting


    fun newRouteClick(){

    }

    fun getTextWorkTime(route: Route): String {
        val startWork =
            dateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeStartWork)

        val isDifference = dateAndTimeConverter.isDifferenceDate(
            first = route.basicData.timeStartWork,
            second = route.basicData.timeEndWork
        )

        val endWork = if (isDifference) {
            dateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeEndWork)
        } else {
            dateAndTimeConverter.getTime(route.basicData.timeEndWork)
        }
        return "$startWork - $endWork"
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            salarySetting = salarySettingUseCase.salarySettingFlow().first()
            settingsUseCase.getUserSettingFlow().collect { settings ->
                userSettings = settings
                dateAndTimeConverter = DateAndTimeConverter(settings)
                routeUseCase.listRoutesByMonth(settings.selectMonthOfYear, settings.timeZone)
                    .collect { result ->
                        if (result is ResultState.Success) {
                            val dateAndTimeConverter = DateAndTimeConverter(settings)
                            val timeZone = dateAndTimeConverter.timeZoneText
                            val currentTimeCalendar =
                                Calendar.getInstance(TimeZone.getTimeZone(timeZone))

                            val currentTimeInMillis = currentTimeCalendar.timeInMillis
                            val routeList = if (settings.isConsiderFutureRoute) {
                                result.data
                            } else {
                                result.data.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
                            }
                            val routeStateList = mutableListOf<ItemState>()
                            routeList.forEach { route ->
                                val routeState = ItemState(
                                    route = route,
                                    isHoliday = isHolidayTimeInRoute(
                                        monthOfYear = settings.selectMonthOfYear,
                                        userSetting = settings,
                                        route = route
                                    ),
                                    isHeavyTrains = isHeavyTrains(salarySetting, route),
                                    isExtendedServicePhaseTrains = isExtendedServicePhaseTrains(
                                        salarySetting,
                                        route
                                    ),
                                    isTransition = route.isTransition(settings.timeZone),
                                    isFuture = route.basicData.timeStartWork.moreThan(Calendar.getInstance().timeInMillis)
                                )
                                routeStateList.add(routeState)
                            }

                            withContext(Dispatchers.Main) {
                                _uiState.update {
                                    it.copy(
                                        uiState = ResultState.Success(Unit),
                                        listItemState = routeStateList
                                    )
                                }
                            }
                        }
                    }
            }
        }
    }
}