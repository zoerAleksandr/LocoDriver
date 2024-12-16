package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Train
import com.z_company.domain.repositories.RouteRepository
import kotlinx.coroutines.flow.Flow

class TrainUseCase(
    private val repository: RouteRepository,
) {
    fun saveTrain(train: Train): Flow<ResultState<Unit>> {
        return repository.saveTrain(train)
    }

    fun getTrainById(trainId: String): Flow<ResultState<Train?>> {
        return repository.loadTrain(trainId)
    }

    fun getTrainListByBasicId(basicId: String): List<Train> {
        return repository.loadTrainListByBasicId(basicId)
    }
    fun removeTrain(train: Train): Flow<ResultState<Unit>> {
        return repository.removeTrain(train)
    }
}