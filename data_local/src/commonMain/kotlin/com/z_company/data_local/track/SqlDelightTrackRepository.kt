package com.z_company.data_local.track

import com.z_company.data_local.track.db.TrackDatabase
import com.z_company.domain.track.TrackInfo
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.track.TrackRepository
import com.z_company.domain.track.TrackRetention

class SqlDelightTrackRepository(private val db: TrackDatabase) : TrackRepository {
    private val q get() = db.trackQueries

    override fun append(trackId: String, trainId: String, point: TrackPoint) {
        db.transaction {
            q.insertTrack(trackId, trainId, point.timeMs, point.timeMs)
            q.insertPoint(
                trackId, point.timeMs, point.latitude, point.longitude,
                point.speedKmh, point.accuracyM?.toDouble(), point.altitudeM, point.baroAltitudeM,
            )
            q.touchTrack(point.timeMs, trackId)
        }
    }

    override fun points(trackId: String): List<TrackPoint> =
        q.selectPoints(trackId).executeAsList().map {
            TrackPoint(it.timeMs, it.latitude, it.longitude, it.speedKmh, it.accuracyM?.toFloat(), it.altitudeM, it.baroAltitudeM)
        }

    override fun pointCount(trackId: String): Long = q.countPoints(trackId).executeAsOne()

    override fun tracks(): List<TrackInfo> = q.selectTracks().executeAsList().map {
        info(it.trackId, it.trainId, it.startedAt, it.endedAt, it.isFavorite, it.uploadState, it.uploadedPoints, it.favoriteDirty)
    }

    override fun track(trackId: String): TrackInfo? = q.selectTrack(trackId).executeAsOneOrNull()?.let { info(it.trackId, it.trainId, it.startedAt, it.endedAt, it.isFavorite, it.uploadState, it.uploadedPoints, it.favoriteDirty) }

    override fun tracksForTrain(trainId: String): List<TrackInfo> =
        q.selectTracksByTrain(trainId).executeAsList().map { info(it.trackId, it.trainId, it.startedAt, it.endedAt, it.isFavorite, it.uploadState, it.uploadedPoints, it.favoriteDirty) }

    override fun setFavorite(trackId: String, favorite: Boolean) {
        q.setFavorite(if (favorite) 1L else 0L, trackId)
    }

    override fun clearFavoriteDirty(trackId: String) {
        q.clearFavoriteDirty(trackId)
    }

    override fun setUploadState(trackId: String, state: Int, uploadedPoints: Long) {
        q.setUploadState(state.toLong(), uploadedPoints, trackId)
    }

    override fun deleteTrack(trackId: String, queueRemoteDelete: Boolean) {
        db.transaction {
            q.deletePointsOne(trackId)
            q.deleteTrackOne(trackId)
            if (queueRemoteDelete) q.insertPendingDelete(trackId)
        }
    }

    override fun pendingRemoteDeletes(): List<String> = q.selectPendingDeletes().executeAsList()

    override fun clearPendingRemoteDelete(trackId: String) {
        q.deletePendingDelete(trackId)
    }

    override fun resetUploadState() {
        db.transaction {
            q.resetAllUploadState()
            q.deleteAllPendingDeletes()
        }
    }

    override fun deleteByTrain(trainId: String) {
        db.transaction {
            q.selectTrackIdsByTrain(trainId).executeAsList().forEach { q.insertPendingDelete(it) }
            q.deletePointsByTrain(trainId)
            q.deleteTracksByTrain(trainId)
        }
    }

    override fun deleteAll() {
        db.transaction {
            // Отзыв согласия: сервер удаляет все треки одной операцией, очередь не нужна.
            q.deleteAllPendingDeletes()
            q.deleteAllPoints()
            q.deleteAllTracks()
        }
    }

    override fun purgeExpired(nowMs: Long): Int {
        val cutoff = nowMs - TrackRetention.MS
        var removed = 0
        db.transaction {
            removed = q.countExpiredTracks(cutoff).executeAsOne().toInt()
            q.deleteExpiredPoints(cutoff)
            q.deleteExpiredTracks(cutoff)
        }
        return removed
    }
}

private fun info(
    trackId: String, trainId: String, startedAt: Long, endedAt: Long,
    isFavorite: Long, uploadState: Long, uploadedPoints: Long, favoriteDirty: Long,
) = TrackInfo(
    trackId = trackId,
    trainId = trainId,
    startedAt = startedAt,
    endedAt = endedAt,
    isFavorite = isFavorite != 0L,
    uploadState = uploadState.toInt(),
    uploadedPoints = uploadedPoints,
    favoriteDirty = favoriteDirty != 0L,
)
