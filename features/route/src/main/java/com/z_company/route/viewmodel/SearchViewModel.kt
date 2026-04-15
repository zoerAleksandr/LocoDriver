package com.z_company.route.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.EntityString
import com.z_company.domain.entities.FilterNames
import com.z_company.domain.entities.FilterSearch
import com.z_company.domain.entities.SearchStateScreen
import com.z_company.domain.entities.TimePeriod
import com.z_company.domain.entities.route.SearchResponse
import com.z_company.domain.repositories.HistoryResponseRepository
import com.z_company.data_local.route.SearchRouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.safetySubList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val COUNT_HINTS = 5
private const val SEARCH_DEBOUNCE_MS = 200L

class SearchViewModel : ViewModel(), KoinComponent {
    private val searchRouteUseCase: SearchRouteUseCase by inject()
    private val historyRepository: HistoryResponseRepository by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private var loadSettingJob: Job? = null
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(SearchUIState())
    val uiState = _uiState.asStateFlow()

    var query by mutableStateOf(TextFieldValue(""))
        private set

    var entityString: EntityString? = null
    var dateAndTimeConverter: DateAndTimeConverter? = null

    private fun setPreliminarySearch(value: Boolean) {
        _uiState.update {
            it.copy(
                preliminarySearch = value
            )
        }
    }

    /**
     * Публичный API для немедленного поиска (без дебаунса).
     * Используется при явном нажатии «Поиск» и из внешних вызовов.
     */
    fun sendRequest(value: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            executeSearch(value)
        }
    }

    /**
     * Suspend-реализация поиска. Правильно обрабатывает CancellationException
     * чтобы отмена корутины (дебаунс) не попадала в Sentry.
     */
    private suspend fun executeSearch(value: String) {
        try {
            if (entityString == null) {
                delay(100)
                if (entityString == null) {
                    Log.e("SearchViewModel", "EntityString not initialized; aborting request")
                    return
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
                    when (result) {
                        is SearchStateScreen.Loading -> {
                            _uiState.update { it.copy(searchState = result) }
                        }
                        is SearchStateScreen.Input -> {
                            val resultList = result.hints.toMutableList()
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
                        is SearchStateScreen.Success -> {
                            Log.d("ZZZ", "$result")
                            _uiState.update {
                                it.copy(
                                    isVisibleHistory = false,
                                    isVisibleHints = false,
                                    isVisibleResult = true,
                                    searchState = result
                                )
                            }
                        }
                        is SearchStateScreen.Failure -> {
                            Log.e("SearchViewModel", "Search error: ${result.entity}")
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
        } catch (e: CancellationException) {
            throw e // Re-throw — ожидаемая отмена при дебаунсе, не ошибка
        } catch (e: Exception) {
            e.sendToSentry("SearchViewModel", "executeSearch")
        }
    }

    fun clearFilter() {
        _uiState.update {
            it.copy(
                searchFilter = FilterSearch()
            )
        }
    }

    fun setSearchFilter(pair: Pair<String, Boolean>) {
        when (pair.first) {
            FilterNames.GENERAL_DATA.value -> {
                _uiState.update {
                    it.copy(
                        searchFilter = it.searchFilter.copy(
                            generalData = pair
                        )
                    )
                }
            }

            FilterNames.LOCO_DATA.value -> {
                _uiState.update {
                    it.copy(
                        searchFilter = it.searchFilter.copy(
                            locoData = pair
                        )
                    )
                }
            }

            FilterNames.TRAIN_DATA.value -> {
                _uiState.update {
                    it.copy(
                        searchFilter = it.searchFilter.copy(
                            trainData = pair
                        )
                    )
                }
            }

            FilterNames.PASSENGER_DATA.value -> {
                _uiState.update {
                    it.copy(
                        searchFilter = it.searchFilter.copy(
                            passengerData = pair
                        )
                    )
                }
            }

            FilterNames.NOTES_DATA.value -> {
                _uiState.update {
                    it.copy(
                        searchFilter = it.searchFilter.copy(
                            notesData = pair
                        )
                    )
                }
            }
        }
    }

    fun setPeriodFilter(timePeriod: TimePeriod) {
        _uiState.update {
            it.copy(
                searchFilter = it.searchFilter.copy(
                    timePeriod = timePeriod
                )
            )
        }
    }

    fun onSearch() {
        setPreliminarySearch(false)
        sendRequest(query.text)
        addResponse(query.text)
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
            historyRepository.removeResponse(SearchResponse(response)).collect { result ->

            }
        }
    }

    fun setQueryValue(newValue: TextFieldValue) {
        _uiState.update { it.copy(preliminarySearch = true) }
        query = newValue.copy(selection = TextRange(newValue.text.length))

        // Отменяем предыдущий (ещё не завершённый) поиск и запускаем дебаунс 200мс.
        // Если пользователь продолжает печатать — предыдущий запрос к БД отменяется.
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(SEARCH_DEBOUNCE_MS)
            executeSearch(query.text)
        }
    }

    fun loadSetting() {
        loadSettingJob?.cancel()
        loadSettingJob = viewModelScope.launch {
            try {
                settingsUseCase.getFlowCurrentSettingsState().collect { result ->
                    if (result is ResultState.Success) {
                        result.data.let { setting ->
                            entityString = EntityString(setting)
                            dateAndTimeConverter = DateAndTimeConverter(setting)
                        }
                        loadSettingJob?.cancel()
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("SearchViewModel", "loadSetting")
            }
        }
    }

    init {
        loadSetting()
        viewModelScope.launch {
            try {
                historyRepository.getAllResponse().collect { result ->
                    if (result is ResultState.Success) {
                        _uiState.update {
                            it.copy(
                                searchHistoryList = result.data.asReversed()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("SearchViewModel", "init")
            }
        }
    }
}