package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.route.Locomotive
import com.example.domain.repositories.PreSaveRepository
import com.example.domain.repositories.RouteRepository
import kotlinx.coroutines.flow.Flow

class LocomotiveUseCase(
    private val repository: RouteRepository,
    private val preSaveRepository: PreSaveRepository
) {
    fun getAllLocomotiveFromPreSave(basicId: String): Flow<ResultState<List<Locomotive>>> {
        return preSaveRepository.loadAllLoco(basicId)
    }
    fun saveLocomotive(locomotive: Locomotive): Flow<ResultState<Unit>> {
        return repository.saveLocomotive(locomotive)
    }
}