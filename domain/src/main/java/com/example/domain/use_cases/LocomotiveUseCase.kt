package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.route.Locomotive
import com.example.domain.repositories.RouteRepositories
import kotlinx.coroutines.flow.Flow

class LocomotiveUseCase(private val repository: RouteRepositories) {
    fun getLocoById(locoId: String): Flow<ResultState<Locomotive?>> {
        return repository.loadLoco(locoId)
    }
    fun isValidAcceptedTime(locomotive: Locomotive): Boolean {
        return true
    }
    fun isValidDeliveryTime(locomotive: Locomotive): Boolean {
        return true
    }
    fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Unit>> {
        return repository.saveLocomotive(locomotive)
    }
}