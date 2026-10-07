@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.use_cases

import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.util.StationDirectory
import com.z_company.domain.util.StationDirectory.withDirectory
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Справочник станций как единый источник координат (SCREEN_SPECS §6.6):
 * плечо читается с координатами из справочника, сохранённое плечо дописывает
 * в справочник свои координаты.
 */
class StationDirectoryUseCase(private val repository: StationNormRepository) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun stations(): List<StationNorm> = repository.getAll()

    fun resolve(phase: ServicePhase): ServicePhase = phase.withDirectory(repository.getAll())

    fun resolve(phases: List<ServicePhase>): List<ServicePhase> {
        val stations = repository.getAll()
        return phases.map { it.withDirectory(stations) }
    }

    /**
     * Сохранение плеча из редактора: его координаты сразу пишутся в справочник (в фоне —
     * редактор закрывается), а станциям без координат подставляются известные справочнику.
     */
    fun prepareSave(phase: ServicePhase): ServicePhase {
        val merged = StationDirectory.mergePhase(repository.getAll(), phase, now())
        merged?.let { scope.launch { mutex.withLock { repository.replaceAll(it).collect {} } } }
        return phase.withDirectory(merged ?: repository.getAll())
    }

    /** Координаты станции [name] из GPS — в справочник, только если у станции их нет (§6.9). */
    suspend fun fillCoordinates(name: String, latitude: Double, longitude: Double) =
        update { StationDirectory.fillCoordinates(it, name, latitude, longitude, now()) }

    private var backfilled = false

    /** [backfill] один раз за запуск приложения. */
    fun backfillOnce(phases: List<ServicePhase>) {
        if (backfilled || phases.isEmpty()) return
        backfilled = true
        scope.launch { backfill(phases) }
    }

    /** Координаты сохранённого плеча — в справочник (новые станции создаются). */
    suspend fun rememberPhase(phase: ServicePhase) = update { StationDirectory.mergePhase(it, phase, now()) }

    /** Перенос координат из плеч, сохранённых до справочника (только туда, где их нет). */
    suspend fun backfill(phases: List<ServicePhase>) = update { StationDirectory.backfill(it, phases, now()) }

    private suspend fun update(change: (List<StationNorm>) -> List<StationNorm>?) {
        mutex.withLock {
            val updated = change(repository.getAll()) ?: return
            repository.replaceAll(updated).collect {}
        }
    }

    private fun now() = Clock.System.now().toEpochMilliseconds()
}
