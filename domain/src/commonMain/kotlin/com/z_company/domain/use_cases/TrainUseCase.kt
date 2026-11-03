package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Train
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.track.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class TrainUseCase(
    private val repository: RouteRepository,
    /** Локальные треки «АВТО»: удаляются вместе с поездом (SCREEN_SPECS §6.9). */
    private val trackRepository: TrackRepository? = null,
    /** Справка ВУ-45 поезда: помечается удалённой вместе с поездом (надгробие уходит на сервер full-replace-ом). */
    private val brakeCertificates: com.z_company.domain.brake.storage.BrakeCertificateUseCase? = null,
    /** Журнал проследования ограничений поезда: снимается вместе с поездом (надгробия уходят на сервер). */
    private val restrictionJournal: com.z_company.domain.track.RestrictionJournalStore? = null,
) {
    fun saveTrain(train: Train): Flow<ResultState<Unit>> {
        return repository.saveTrain(train)
    }

    fun updateTrain(train: Train): Flow<ResultState<Unit>> {
        return repository.updateTrain(train)
    }

    fun getTrainById(trainId: String): Flow<ResultState<Train?>> {
        return repository.loadTrain(trainId)
    }

    fun getTrainListByBasicId(basicId: String): List<Train> {
        return repository.loadTrainListByBasicId(basicId)
    }
    fun removeTrain(train: Train): Flow<ResultState<Unit>> {
        return flow {
            // Не должно ронять удаление поезда: треки — вторичные данные.
            runCatching { trackRepository?.deleteByTrain(train.trainId) }
            runCatching { brakeCertificates?.deleteForTrain(train.trainId) }
            runCatching { restrictionJournal?.deleteByTrain(train.trainId) }
            emitAll(repository.removeTrain(train))
        }
    }
}