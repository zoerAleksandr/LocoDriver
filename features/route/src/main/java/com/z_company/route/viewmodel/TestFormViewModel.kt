package com.z_company.route.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.route.Const.NULLABLE_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TestFormViewModel(
    private val routeId: String?,
    private val isCopy: Boolean = false,
    application: Application
) : ViewModel(), KoinComponent {
    val routeUseCase: RouteUseCase by inject()


     var currentRoute: Route
         get() {
             return uiState.value.route
         }
        private set(value) {
            _uiState.update {
                it.copy(route = value)
            }
        }

    private val _uiState = MutableStateFlow(TestFormScreenUIState(route = currentRoute))
    val uiState = _uiState.asStateFlow()

    init {
        if (routeId == NULLABLE_ID) {
            createNewRoute()

        } else {
            loadRoute(routeId!!, isCopy)
        }
//        viewModelScope.launch(Dispatchers.IO) {
//            prepareReviewDialog()
//        }
    }

    fun createNewRoute(){
        viewModelScope.launch {
            currentRoute = Route()
            routeUseCase.saveRoute(currentRoute).first {
                it is ResultState.Success
            }
            _uiState.update {
                it.copy(isLoading = false)
            }
        }
    }


    private fun loadRoute(id: String, isCopy: Boolean) {
        viewModelScope.launch {
            routeUseCase.routeDetails(id).collect { routeState ->
                if (routeState is ResultState.Success) {
                    routeState.data?.let { route ->
                        currentRoute = route
                        _uiState.update {
                            it.copy(
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun saveRoute(){
        viewModelScope.launch {
            routeUseCase.saveRoute(currentRoute)
        }
    }

    fun setFavoriteState(){

    }

    fun setNumber(number: String){

    }
}