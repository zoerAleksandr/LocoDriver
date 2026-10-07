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
                point.speedKmh, point.accuracyM?.toDouble(), point.altitudeM,
            )
            q.touchTrack(point.timeMs, trackId)
        }
    }

    override fun points(trackId: String): List<TrackPoint> =
        q.selectPoints(trackId).executeAsList().map {
            TrackPoint(it.timeMs, it.latitude, it.longitude, it.speedKmh, it.accuracyM?.toFloat(), it.altitudeM)
        }

    override fun pointCount(trackId: String): Long = q.countPoints(trackId).executeAsOne()

    override fun tracks(): List<TrackInfo> = q.selectTracks().executeAsList().map {
        TrackInfo(it.trackId, it.trainId, it.startedAt, it.endedAt, it.isFavorite != 0L)
    }

    override fun tracksForTrain(trainId: String): List<TrackInfo> =
        q.selectTracksByTrain(trainId).executeAsList().map {
            TrackInfo(it.trackId, it.trainId, it.startedAt, it.endedAt, it.isFavorite != 0L)
        }

    override fun setFavorite(trackId: String, favorite: Boolean) {
        q.setFavorite(if (favorite) 1L else 0L, trackId)
    }

    override fun deleteByTrain(trainId: String) {
        db.transaction {
            q.deletePointsByTrain(trainId)
            q.deleteTracksByTrain(trainId)
        }
    }

    override fun deleteAll() {
        db.transaction {
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
