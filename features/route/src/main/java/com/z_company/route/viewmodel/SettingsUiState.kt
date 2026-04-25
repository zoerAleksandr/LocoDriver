package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.User
import com.z_company.domain.entities.setting.UserSettings

sealed class CountryLoadingState {
    data class Loading(val countryName: String) : CountryLoadingState()
    object Success : CountryLoadingState()
    object Error : CountryLoadingState()
    object NoInternet : CountryLoadingState()
}

/**
 * Состояние загрузки региональных праздников при смене региона.
 * Аналог [CountryLoadingState] — показываем пользователю что идёт загрузка
 * с сервера, чтобы он не подумал что приложение зависло.
 *
 * Loading.regionName — отображаемое название региона ("Республика Крым" и т.п.).
 * Success — праздники подтянуты, норма пересчитана.
 * Error — серверная ошибка (нерегиональная — сетевые ошибки идут в NoInternet).
 * NoInternet — нет сети; работаем в режиме fallback (хардкод).
 */
sealed class RegionLoadingState {
    data class Loading(val regionName: String) : RegionLoadingState()
    object Success : RegionLoadingState()
    object Error : RegionLoadingState()
    object NoInternet : RegionLoadingState()
}

data class SettingsUiState(
    val settingDetails: ResultState<UserSettings?> = ResultState.Loading(),
    val userDetailsState: ResultState<User?> = ResultState.Loading(),
    val calendarState: ResultState<MonthOfYear?> = ResultState.Loading(),
    val saveSettingsState: ResultState<Unit>? = null,
    val uploadState: ResultState<Int>? = null,
    val downloadState: ResultState<Int>? = null,
    val updateAt: Long? = null,
    val monthList: List<Int> = listOf(),
    val yearList: List<Int> = listOf(),
    val logOutState: ResultState<Unit>? = null,
    val resentVerificationEmailButton: Boolean = true,
    var purchasesEndTime: ResultState<String> = ResultState.Loading(),
    val isRefreshing: Boolean = false,
    val showDialogAddServicePhase: Boolean = false,
    val selectedServicePhase: Pair<ServicePhase, Int>? = null,
    val servicePhases: SnapshotStateList<ServicePhase>? = mutableStateListOf(),
    val dateAndTimeConverter: DateAndTimeConverter? = null,
    val countryLoadingState: CountryLoadingState? = null,
    val regionLoadingState: RegionLoadingState? = null,
    /**
     * Норма часов за текущий выбранный месяц — рассчитывается через NormaUseCase.
     * Учитывает региональный календарь + дни отвлечений + производственный календарь.
     * null = ещё не загружена.
     */
    val normaHours: Int? = null,
)