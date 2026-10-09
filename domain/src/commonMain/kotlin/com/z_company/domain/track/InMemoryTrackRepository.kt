package com.z_company.domain.track

/** Хранилище треков в памяти: для тестов и предпросмотра (в приложении — SQLDelight, `Track.db`). */
class InMemoryTrackRepository : TrackRepository {
    private val infos = LinkedHashMap<String, TrackInfo>()
    private val pts = HashMap<String, MutableList<TrackPoint>>()
    private val pending = LinkedHashSet<String>()
    private val removedTrains = HashSet<String>()

    override fun append(trackId: String, trainId: String, point: TrackPoint) {
        if (trainId in removedTrains) return
        val list = pts.getOrPut(trackId) { mutableListOf() }
        if (list.none { it.timeMs == point.timeMs }) list += point
        val old = infos[trackId]
        infos[trackId] = old?.copy(endedAt = maxOf(old.endedAt, point.timeMs))
            ?: TrackInfo(trackId, trainId, point.timeMs, point.timeMs, false)
    }

    override fun points(trackId: String): List<TrackPoint> = pts[trackId].orEmpty().sortedBy { it.timeMs }
    override fun pointCount(trackId: String): Long = pts[trackId]?.size?.toLong() ?: 0L
    override fun tracks(): List<TrackInfo> = infos.values.sortedByDescending { it.startedAt }
    override fun track(trackId: String): TrackInfo? = infos[trackId]
    override fun tracksForTrain(trainId: String): List<TrackInfo> = tracks().filter { it.trainId == trainId }

    override fun setFavorite(trackId: String, favorite: Boolean) {
        infos[trackId]?.let { infos[trackId] = it.copy(isFavorite = favorite, favoriteDirty = true) }
    }

    override fun clearFavoriteDirty(trackId: String) {
        infos[trackId]?.let { infos[trackId] = it.copy(favoriteDirty = false) }
    }

    override fun setUploadState(trackId: String, state: Int, uploadedPoints: Long) {
        infos[trackId]?.let { infos[trackId] = it.copy(uploadState = state, uploadedPoints = uploadedPoints) }
    }

    override fun deleteTrack(trackId: String, queueRemoteDelete: Boolean) {
        infos.remove(trackId)
        pts.remove(trackId)
        if (queueRemoteDelete) pending += trackId
    }

    override fun pendingRemoteDeletes(): List<String> = pending.toList()
    override fun clearPendingRemoteDelete(trackId: String) {
        pending -= trackId
    }

    override fun resetUploadState() {
        infos.keys.toList().forEach { id ->
            infos[id] = infos.getValue(id).copy(uploadState = TrackUploadState.PENDING, uploadedPoints = 0, favoriteDirty = false)
        }
        pending.clear()
    }

    override fun allowTrain(trainId: String) {
        removedTrains -= trainId
    }

    override fun deleteByTrain(trainId: String) {
        removedTrains += trainId
        tracksForTrain(trainId).forEach { deleteTrack(it.trackId, queueRemoteDelete = true) }
    }

    override fun deleteAll() {
        infos.clear()
        pts.clear()
        pending.clear()
    }

    override fun purgeExpired(nowMs: Long): Int {
        val expired = infos.values.filter { TrackRetention.isExpired(it.endedAt, it.isFavorite, nowMs) }
        expired.forEach { deleteTrack(it.trackId, queueRemoteDelete = false) }
        return expired.size
    }
}
