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
import com.z_company.domain.entities.FilterNames
import com.z_company.domain.entities.FilterSearch
import com.z_company.domain.entities.SearchStateScreen
import com.z_company.domain.entities.TimePeriod
import com.z_company.domain.entities.route.SearchResponse
import com.z_company.domain.repositories.HistoryResponseRepository
import com.z_company.data_local.route.SearchRouteUseCase
import com.z_company.domain.util.safetySubList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val COUNT_HINTS = 5

class SearchViewModel : ViewModel(), KoinComponent {
    private val searchRouteUseCase: SearchRouteUseCase by inject()
    private val historyRepository: HistoryResponseRepository by inject()

    private val _uiState = MutableStateFlow(SearchUIState())
    val uiState = _uiState.asStateFlow()

    var query by mutableStateOf(TextFieldValue(""))
        private set

    private fun setPreliminarySearch(value: Boolean) {
        _uiState.update {
            it.copy(
                preliminarySearch = value
            )
        }
    }

    fun sendRequest(value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val correctValue = value.trim()
            if (correctValue.isNotEmpty()) {
                searchRouteUseCase.searchRoute(
                    correctValue,
                    uiState.value.searchFilter,
                    uiState.value.preliminarySearch
                ).collect { result ->
                    Log.d("ZZZ", "search result -> $result")
                    if (result is SearchStateScreen.Loading){
                        _uiState.update {
                            it.copy(
                                searchState = result,
                            )
                        }
                    }
//                    delay(500L)
                    if (result is SearchStateScreen.Input) {
                        val resultList: MutableList<String> = result.hints
                            .toMutableList()
                        resultList.removeAll { it.isBlank() }
                        val newList = resultList.safetySubList(0, COUNT_HINTS)
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    isVisibleHistory = true,
                                    isVisibleHints = true,
                                    searchState = result,
                                    hints = newList
                                )
                            }
                        }
                    }
                    if (result is SearchStateScreen.Success) {
                        withContext(Dispatchers.Main) {
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
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            searchState = SearchStateScreen.Success(null),
                            isVisibleHistory = true
                        )
                    }
                }
            }
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
        _uiState.update {
            it.copy(
                preliminarySearch = true
            )
        }
        query = newValue.copy(selection = TextRange(newValue.text.length))
        sendRequest(query.text)
    }

    init {
        viewModelScope.launch {
            historyRepository.getAllResponse().collect { result ->
                if (result is ResultState.Success) {
                    _uiState.update {
                        it.copy(
                            searchHistoryList = result.data.asReversed()
                        )
                    }
                }
            }
        }
    }
}