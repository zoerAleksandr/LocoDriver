package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.route.pre_save.PreLocomotive
import com.example.domain.repositories.PreSaveRepository
import com.example.domain.repositories.RouteRepository
import kotlinx.coroutines.flow.Flow

class PreSaveLocomotiveUseCase(
    private val preSaveRepository: PreSaveRepository,
    private val repository: RouteRepository
) {
    fun isValidAcceptedTime(preLocomotive: PreLocomotive): Boolean {
        return true
    }

    fun isValidDeliveryTime(preLocomotive: PreLocomotive): Boolean {
        return true
    }

    fun saveLocomotive(preLocomotive: PreLocomotive): Flow<ResultState<Unit>> {
        return preSaveRepository.saveLocomotive(preLocomotive)
    }

    fun getPreLocoById(locoId: String): Flow<ResultState<PreLocomotive?>> {
        return preSaveRepository.loadPreLoco(locoId)
    }

    fun getLocoById(locoId: String) : Flow<ResultState<PreLocomotive?>> {
        return repository.loadLoco(locoId)
    }

    fun clearRepository(): Flow<ResultState<Unit>> {
        return preSaveRepository.clearRepository()
    }
}