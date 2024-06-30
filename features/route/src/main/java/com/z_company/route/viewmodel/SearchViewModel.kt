package com.z_company.route.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.route.ui.FilterNames
import com.z_company.route.ui.FilterSearch
import com.z_company.route.ui.TimePeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(SearchUIState())
    val uiState = _uiState.asStateFlow()
    var query by mutableStateOf(TextFieldValue(""))
        private set

    fun sendRequest(value: String) {
        val correctValue = value.trim()
        if (correctValue.isNotEmpty()) {
            viewModelScope.launch {
//                searchRouteUseCase.execute(correctValue, searchFilter, preliminarySearch)
//                    .collect { result ->
//                        searchState.value = result
//                        if (result is SearchStateScreen.Input) {
//                            hintsList.clear()
//                            val resultList: MutableList<String> = result.hints
//                                .toMutableList()
//                            resultList.removeAll { it.isBlank() }
//                            val newList = resultList.safetySubList(0, COUNT_HINTS)
//
//                            hintsList.addAll(newList)
//                        }
//                        if (result is SearchStateScreen.Success) {
//                            resultList.clear()
//                            result.data?.let {
//                                resultList.addAll(it)
//                            }
//                        }
//                    }
            }
        } else {
//            searchState.value = SearchStateScreen.Success(null)
        }
    }

    fun clearFilter(){
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
    fun addResponse(response: String) {
        if (response.isNotEmpty()) {
            val correctResponse = response.trim()
            viewModelScope.launch {
//                historyRepository.addResponse(correctResponse).collect { result ->
//                    if (result == ResultState.Success(true)) {
//                        historyList.add(0, correctResponse)
//                    }
//                }
            }
        }
    }

    fun removeHistoryResponse(response: String) {
        viewModelScope.launch {
//            historyRepository.removeResponse(response).collect { result ->
//                if (result == ResultState.Success(true)) {
//                    historyList.remove(response)
//                }
//
//            }
        }
    }

    fun setQueryValue(newValue: TextFieldValue) {
//        preliminarySearch = true
        query = newValue
        sendRequest(query.text)
    }
}