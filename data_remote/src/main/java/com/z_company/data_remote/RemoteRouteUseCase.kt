package com.z_company.data_remote

import android.util.Log
import com.z_company.type_converter.BasicDataJSONConverter
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.BasicData
import com.z_company.entity_converter.BasicDataConverter
import com.z_company.work_manager.GET_BASIC_DATA_WORKER_OUTPUT_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class RemoteRouteUseCase(val repository: RemoteRouteRepository) {
    suspend fun getAllBasicData() {
        repository.getAllBasicData()
    }

    suspend fun saveBasicData(basicData: BasicData) {
        repository.getAllBasicData().onEach { result ->
            if (result is ResultState.Success) {
                var objectId = ""
                result.data?.forEach { entity ->
                    if (entity.id == basicData.id) {
                        objectId = entity.objectId
                    }
                }
                repository.saveBasicData(BasicDataConverter.fromData(basicData), objectId)
            }
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }
}