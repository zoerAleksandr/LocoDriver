package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.OtherWork
import com.z_company.domain.repositories.RouteRepository
import kotlinx.coroutines.flow.Flow

class OtherWorkUseCase(
    private val repository: RouteRepository
) {
    fun saveOtherWork(otherWork: OtherWork): Flow<ResultState<Unit>> {
        return repository.saveOtherWork(otherWork)
    }

    fun getOtherWorkById(otherWorkId: String): Flow<ResultState<OtherWork?>> {
        return repository.loadOtherWork(otherWorkId)
    }

    fun getOtherWorkListByBasicId(basicId: String): List<OtherWork> {
        return repository.loadOtherWorkListByBasicId(basicId)
    }

    fun removeOtherWork(otherWork: OtherWork): Flow<ResultState<Unit>> {
        return repository.removeOtherWork(otherWork)
    }
}
