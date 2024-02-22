package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.route.Train
import com.example.domain.repositories.RouteRepository
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
}