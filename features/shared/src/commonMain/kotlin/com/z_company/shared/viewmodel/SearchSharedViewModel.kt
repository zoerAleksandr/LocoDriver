package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.FilterNames
import com.z_company.domain.entities.FilterSearch
import com.z_company.domain.entities.SearchStateScreen
import com.z_company.domain.entities.TimePeriod
import com.z_company.domain.entities.RouteWithTag
import com.z_company.domain.entities.route.SearchResponse
import com.z_company.domain.repositories.HistoryResponseRepository
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.safetySubList
import com.z_company.shared.search.SharedSearchRouteUseCase
import com.z_company.shared.util.DateAndTimeConverter
import com.z_company.shared.util.EntityString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val COUNT_HINTS = 5

data class SearchUIState(
    val searchState: SearchStateScreen<List<RouteWithTag>?> = SearchStateScreen.Success(null),
    val preliminarySearch: Boolean = false,
    val searchFilter: FilterSearch = FilterSearch(),
    val isVisibleHistory: Boolean = true,
    val isVisibleHints: Boolean = false,
    val isVisibleResult: Boolean = false,
    val hints: List<String> = mutableListOf(),
    val searchResultList: List<RouteWithTag> = mutableListOf(),
    val searchHistoryList: List<SearchResponse> = mutableListOf(),
)

/**
 * Shared ViewModel for the search screen.
 * Based on Android SearchViewModel with full FTS search, filters, history, and hints.
 */
class SearchSharedViewModel(
    private val searchRouteUseCase: SharedSearchRouteUseCase,
    private val historyRepository: HistoryResponseRepository,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    private var loadSettingJob: Job? = null

    private val _uiState = MutableStateFlow(SearchUIState())
    val uiState = _uiState.asStateFlow()

    private val _queryText = MutableStateFlow("")
    val queryText = _queryText.asStateFlow()

    var entityString: EntityString? = null
        private set
    var dateAndTimeConverter: DateAndTimeConverter? = null
        private set

    private fun setPreliminarySearch(value: Boolean) {
        _uiState.update {
            it.copy(preliminarySearch = value)
        }
    }

    fun sendRequest(value: String) {
        viewModelScope.launch(Dispatchers.Default) {
            if (entityString == null) {
                delay(100)
                if (entityString == null) {
                    return@launch
                }
            }

            val correctValue = value.trim()
            if (correctValue.isNotEmpty()) {
                searchRouteUseCase.searchRoute(
                    correctValue,
                    uiState.value.searchFilter,
                    uiState.value.preliminarySearch,
                    entityString!!
                ).collect { result ->
                    if (result is SearchStateScreen.Loading) {
                        _uiState.update {
                            it.copy(searchState = result)
                        }
                    }
                    if (result is SearchStateScreen.Input) {
                        val resultList: MutableList<String> = result.hints.toMutableList()
                        resultList.removeAll { it.isBlank() }
                        val newList = resultList.safetySubList(0, COUNT_HINTS)
                        _uiState.update {
                            it.copy(
                                isVisibleHistory = true,
                                isVisibleHints = true,
                                searchState = result,
                                hints = newList
                            )
                        }
                    }
                    if (result is SearchStateScreen.Success) {
                        _uiState.update {
                            it.copy(
                                isVisibleHistory = false,
                                isVisibleHints = false,
                                isVisibleResult = true,
                                searchState = result
                            )
                        }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        searchState = SearchStateScreen.Success(null),
                        isVisibleHistory = true
                    )
                }
            }
        }
    }

    fun clearFilter() {
        _uiState.update {
            it.copy(searchFilter = FilterSearch())
        }
    }

    fun setSearchFilter(pair: Pair<String, Boolean>) {
        when (pair.first) {
            FilterNames.GENERAL_DATA.value -> {
                _uiState.update {
                    it.copy(searchFilter = it.searchFilter.copy(generalData = pair))
                }
            }
            FilterNames.LOCO_DATA.value -> {
                _uiState.update {
                    it.copy(searchFilter = it.searchFilter.copy(locoData = pair))
                }
            }
            FilterNames.TRAIN_DATA.value -> {
                _uiState.update {
                    it.copy(searchFilter = it.searchFilter.copy(trainData = pair))
                }
            }
            FilterNames.PASSENGER_DATA.value -> {
                _uiState.update {
                    it.copy(searchFilter = it.searchFilter.copy(passengerData = pair))
                }
            }
            FilterNames.NOTES_DATA.value -> {
                _uiState.update {
                    it.copy(searchFilter = it.searchFilter.copy(notesData = pair))
                }
            }
        }
    }

    fun setPeriodFilter(timePeriod: TimePeriod) {
        _uiState.update {
            it.copy(searchFilter = it.searchFilter.copy(timePeriod = timePeriod))
        }
    }

    fun onSearch() {
        setPreliminarySearch(false)
        sendRequest(_queryText.value)
        addResponse(_queryText.value)
    }

    fun addResponse(response: String) {
        if (response.isNotEmpty()) {
            val correctResponse = response.trim()
            val searchResponse = SearchResponse(responseText = correctResponse)
            viewModelScope.launch {
                historyRepository.addResponse(searchResponse).collect { }
            }
        }
    }

    fun removeHistoryResponse(response: String) {
        viewModelScope.launch {
            historyRepository.removeResponse(SearchResponse(response)).collect { }
        }
    }

    fun setQueryValue(newValue: String) {
        _uiState.update {
            it.copy(preliminarySearch = true)
        }
        _queryText.value = newValue
        sendRequest(newValue)
    }

    fun loadSetting() {
        loadSettingJob?.cancel()
        loadSettingJob = viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collect { result ->
                if (result is ResultState.Success) {
                    result.data.let { setting ->
                        val timeZoneId = settingsUseCase.getTimeZone(setting.timeZone)
                        entityString = EntityString(setting, timeZoneId)
                        dateAndTimeConverter = DateAndTimeConverter(timeZoneId)
                    }
                    loadSettingJob?.cancel()
                }
            }
        }
    }

    init {
        loadSetting()
        viewModelScope.launch {
            historyRepository.getAllResponse().collect { result ->
                if (result is ResultState.Success) {
                    _uiState.update {
                        it.copy(searchHistoryList = result.data.asReversed())
                    }
                }
            }
        }
    }
}
