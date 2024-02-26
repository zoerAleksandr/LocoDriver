package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.route.Passenger
import com.example.domain.repositories.RouteRepository
import kotlinx.coroutines.flow.Flow

class PassengerUseCase(
    private val repository: RouteRepository
){
    fun savePassenger(passenger: Passenger): Flow<ResultState<Unit>> {
        return repository.savePassenger(passenger)
    }
    fun getPassengerById(passengerId: String): Flow<ResultState<Passenger?>> {
        return repository.loadPassenger(passengerId)
    }
}
