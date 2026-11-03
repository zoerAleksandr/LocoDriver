package com.z_company.domain.autorecord

import com.z_company.domain.entities.route.Train
import com.z_company.domain.track.NO_TRAIN_ID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Определение (и при необходимости создание) поезда-цели записи «АВТО». Все вызовы идут под одним мьютексом:
 * маршрут перечитывается ПОСЛЕ получения блокировки, а поезд записывается ([createTrain] возвращается только
 * после записи в БД). Поэтому параллельные запросы (включение АВТО, первое событие, кнопка записи на карте)
 * создают в маршруте ровно один поезд: второй видит пустой созданный первым ([TrainOrdering.pickAutoTarget]).
 */
class RecordTargetResolver(
    /** Id текущего маршрута и его поезда; null - текущего маршрута нет. */
    private val currentRoute: suspend () -> Pair<String, List<Train>>?,
    /** Записать новый поезд и дождаться завершения записи. */
    private val createTrain: suspend (Train) -> Unit,
) {
    private val mutex = Mutex()

    /** id поезда-цели; [NO_TRAIN_ID] - текущего маршрута нет (поезд не создаётся). */
    suspend fun resolve(openedTrainId: String?): String = mutex.withLock {
        val route = currentRoute()
        when (val t = TrainOrdering.pickRecordTarget(openedTrainId, route?.first, route?.second)) {
            is TrainOrdering.RecordTarget.Existing -> t.trainId
            is TrainOrdering.RecordTarget.NewTrainInRoute -> {
                val train = Train(basicId = t.routeId)
                createTrain(train)
                train.trainId
            }
            TrainOrdering.RecordTarget.NoTrain -> NO_TRAIN_ID
        }
    }
}
