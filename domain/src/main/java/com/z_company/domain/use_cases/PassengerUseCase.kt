package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.repositories.RouteRepository
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
    fun removePassenger(passenger: Passenger): Flow<ResultState<Unit>>{
        return repository.removePassenger(passenger)
    }
}
