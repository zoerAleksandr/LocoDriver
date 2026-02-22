package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.repositories.RouteRepository
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
    fun getLocomotiveList(basicId: String): List<Locomotive> {
        return repository.loadLocoListByBasicId(basicId)
    }

    fun isValidAcceptedTime(locomotive: Locomotive): Boolean {
        return true
    }

    fun isValidDeliveryTime(locomotive: Locomotive): Boolean {
        return true
    }
    fun removeLoco(locomotive: Locomotive): Flow<ResultState<Unit>>{
        return repository.removeLoco(locomotive)
    }
}