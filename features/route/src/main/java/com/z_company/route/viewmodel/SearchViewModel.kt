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
import com.z_company.domain.entities.RouteWithTag
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val COUNT_HINTS = 5
private const val SEARCH_DEBOUNCE_MS = 200L

class SearchViewModel : ViewModel(), KoinComponent {
    private val searchRouteUseCase: SearchRouteUseCase by inject()
    private val historyRepository: HistoryResponseRepository by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private var indexJob: Job? = null
    private var searchJob: Job? = null

    /**
     * Готовый индекс поиска. Пересобирается только при изменении списка
     * маршрутов или настроек — не на каждое нажатие клавиши. Сам поиск
     * ([runSearch]) работает по этому индексу без обращений к БД.
     */
    @Volatile
    private var searchIndex: List<SearchRouteUseCase.RouteSearchEntry> = emptyList()

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
        searchJob = viewModelScope.launch {
            runSearch(value)
        }
    }

    private suspend fun runSearch(value: String) {
        try {
            val correctValue = value.trim()
            if (correctValue.isEmpty()) {
                _uiState.update {
                    it.copy(
                        searchState = SearchStateScreen.Success(null),
                        isVisibleHistory = true
                    )
                }
                return
            }

            val state = uiState.value
            val result = withContext(Dispatchers.Default) {
                searchRouteUseCase.search(
                    searchIndex,
                    correctValue,
                    state.searchFilter,
                    state.preliminarySearch
                )
            }
            applySearchResult(result)
        } catch (e: CancellationException) {
            throw e // ожидаемая отмена при дебаунсе, не ошибка
        } catch (e: Exception) {
            e.sendToSentry("SearchViewModel", "runSearch")
        }
    }

    private fun applySearchResult(result: SearchStateScreen<List<RouteWithTag>>) {
        when (result) {
            is SearchStateScreen.Loading -> {
                _uiState.update { it.copy(searchState = result) }
            }

            is SearchStateScreen.Input -> {
                val newList = result.hints
                    .filter { it.isNotBlank() }
                    .safetySubList(0, COUNT_HINTS)
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
                _uiState.update { it.copy(searchState = result) }
            }
        }
    }

    fun clearFilter() {
        _uiState.update {
            it.copy(
                searchFilter = FilterSearch()
            )
        }
        rerunCurrentQuery()
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

        // Дебаунс 200мс: пока пользователь печатает — предыдущий поиск отменяется.
        // Поиск теперь идёт по готовому индексу в памяти, без обращения к БД.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(query.text)
        }
    }

    /** Повторить текущий запрос по последнему индексу (после его пересборки или смены фильтра). */
    private fun rerunCurrentQuery() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runSearch(query.text)
        }
    }

    /**
     * Единая подписка на настройки и список маршрутов. При любом изменении
     * перестраиваем индекс один раз и повторяем текущий запрос. Это убирает
     * пересборку всех маршрутов из БД (4×N SQL-запросов) на каждое нажатие.
     */
    private fun observeIndex() {
        indexJob?.cancel()
        indexJob = viewModelScope.launch {
            try {
                combine(
                    settingsUseCase.getFlowCurrentSettingsState(),
                    searchRouteUseCase.routesFlow()
                ) { settingState, routesState -> settingState to routesState }
                    .collect { (settingState, routesState) ->
                        if (settingState is ResultState.Success) {
                            val setting = settingState.data
                            entityString = EntityString(setting)
                            dateAndTimeConverter = DateAndTimeConverter(setting)
                        }

                        when (routesState) {
                            is ResultState.Loading -> {
                                if (query.text.isNotBlank()) {
                                    _uiState.update {
                                        it.copy(searchState = SearchStateScreen.Loading())
                                    }
                                }
                            }

                            is ResultState.Error -> {
                                _uiState.update {
                                    it.copy(searchState = SearchStateScreen.Failure(routesState.entity))
                                }
                            }

                            is ResultState.Success -> {
                                val converter = dateAndTimeConverter ?: return@collect
                                searchIndex = withContext(Dispatchers.Default) {
                                    searchRouteUseCase.buildIndex(routesState.data, converter)
                                }
                                // Обновить результаты, если запрос уже введён.
                                if (query.text.isNotBlank()) {
                                    rerunCurrentQuery()
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                e.sendToSentry("SearchViewModel", "observeIndex")
            }
        }
    }

    init {
        observeIndex()
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
