package com.z_company.domain.track

/**
 * Одноразовая «склейка» уже записанных треков: смежные треки одного поезда с разрывом не больше [gapMs]
 * (запись раньше открывала новый трек при каждом перезапуске сервиса/процесса) сливаются в один.
 * Каждое слияние - атомарная операция репозитория; повторный запуск ничего не меняет.
 */
object TrackMerger {
    /**
     * Слить смежные треки; возвращает, сколько треков поглощено. Треки с `endedAt >= liveFromMs` (пишутся прямо сейчас,
     * писатель держит их id в памяти) не трогаются.
     */
    fun mergeAdjacent(
        repository: TrackRepository,
        gapMs: Long = TrackWriter.RESUME_GAP_MS,
        liveFromMs: Long = Long.MAX_VALUE,
    ): Int {
        var absorbed = 0
        val byTrain = repository.tracks().filter { it.endedAt < liveFromMs }.groupBy { it.trainId }
        for (list in byTrain.values) {
            val sorted = list.sortedBy { it.startedAt }
            var keep = sorted.firstOrNull() ?: continue
            var keepEnd = keep.endedAt
            for (next in sorted.drop(1)) {
                if (next.startedAt - keepEnd <= gapMs) {
                    repository.mergeTracks(keep.trackId, next.trackId)
                    keepEnd = maxOf(keepEnd, next.endedAt)
                    absorbed++
                } else {
                    keep = next
                    keepEnd = next.endedAt
                }
            }
        }
        return absorbed
    }
}
