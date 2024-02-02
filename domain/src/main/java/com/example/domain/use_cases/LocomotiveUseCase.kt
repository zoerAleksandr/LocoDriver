package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.route.Locomotive
import com.example.domain.repositories.RouteRepository
import kotlinx.coroutines.flow.Flow

class LocomotiveUseCase(
    private val repository: RouteRepository
) {
    fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Unit>> {
        return repository.saveLocomotive(locomotive)
    }
    fun getLocoById(locoId: String) : Flow<ResultState<Locomotive?>> {
        return repository.loadLoco(locoId)
    }
    fun isValidAcceptedTime(locomotive: Locomotive): Boolean {
        return true
    }

    fun isValidDeliveryTime(locomotive: Locomotive): Boolean {
        return true
    }
}