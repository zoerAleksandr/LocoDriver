package com.z_company.route.viewmodel.all_route_view_model

import androidx.lifecycle.ViewModel
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
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
import com.z_company.route.viewmodel.home_view_model.InitialData
import com.z_company.route.viewmodel.home_view_model.ItemState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import java.util.TimeZone
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class AllRouteViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()

    private val _uiState = MutableStateFlow(AllRouteUiState())
    val uiState = _uiState.asStateFlow()

    // keep latest raw data for local filtering
    private val latestRawRoutes = MutableStateFlow<List<Route>>(emptyList())

    // combine repository flow and selected filter changes
    private var monthRequested: MonthOfYear? = null
    private var offsetRequested: Long = 0L
    private var dateAndTimeConverter: DateAndTimeConverter? = null
}