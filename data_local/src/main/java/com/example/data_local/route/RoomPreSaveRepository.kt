package com.example.data_local.route

import com.example.core.ResultState
import com.example.core.ResultState.Companion.flowMap
import com.example.core.ResultState.Companion.flowRequest
import com.example.data_local.route.dao.PreSaveDao
import com.example.data_local.route.entity_converters.PreLocomotiveConverter
import com.example.domain.entities.route.Locomotive
import com.example.domain.entities.route.pre_save.PreLocomotive
import com.example.domain.repositories.PreSaveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class RoomPreSaveRepository : PreSaveRepository, KoinComponent {
    private val dao: PreSaveDao by inject()
    override fun loadPreLoco(locoId: String): Flow<ResultState<PreLocomotive?>> {
        return flowMap {
            dao.getPreLocomotive(locoId).map { loco ->
                ResultState.Success(
                    loco?.let { PreLocomotiveConverter.toData(loco) }
                )
            }
        }
    }

    override fun loadAllLoco(basicId: String): Flow<ResultState<List<Locomotive>>> {
        return flowMap {
            dao.getAllLocomotives().map { list ->
                ResultState.Success(
                    list.map { preLocomotive ->
                        PreLocomotiveConverter.fromPreSave(
                            PreLocomotiveConverter.toData(preLocomotive), basicId
                        )
                    }
                )
            }
        }
    }

    override fun removeLoco(preLocomotive: PreLocomotive): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.deleteLocomotives(PreLocomotiveConverter.fromData(preLocomotive))
        }
    }

    override fun clearRepository(): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.clearRepository()
        }
    }

    override fun saveLocomotive(preLocomotive: PreLocomotive): Flow<ResultState<Unit>> {
        return flowRequest {
            if (preLocomotive.locoId.isBlank()) {
                preLocomotive.locoId = UUID.randomUUID().toString()
            }
            dao.saveLocomotive(PreLocomotiveConverter.fromData(preLocomotive))
        }
    }
}