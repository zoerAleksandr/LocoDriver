package com.z_company.data_local.track

import com.zcompany.datalocal.track.db.RestrictionPass as Row
import com.z_company.data_local.track.db.TrackDatabase
import com.z_company.domain.track.RestrictionJournalStore
import com.z_company.domain.track.RestrictionPass

/**
 * Журнал проследования ограничений в Track.db (локальная БД, Route.db не затронута). Записи без координат.
 * Снятие с поезда и удаление поезда - надгробия (`isDeleted`), чтобы сервер и другие устройства узнали об удалении;
 * доставленные надгробия чистятся при следующей отметке отправки.
 */
class SqlDelightRestrictionJournalStore(
    private val db: TrackDatabase,
    private val nowMs: () -> Long,
) : RestrictionJournalStore {
    private val q get() = db.trackQueries

    override fun forTrain(trainId: String): List<RestrictionPass> = q.selectPassesByTrain(trainId).executeAsList().map(::domain)

    override fun openForTrain(trainId: String): List<RestrictionPass> = q.selectOpenPassesByTrain(trainId).executeAsList().map(::domain)

    override fun get(trainId: String, warningId: String): RestrictionPass? =
        q.selectPass(trainId, warningId).executeAsOneOrNull()?.takeIf { it.isDeleted == 0L }?.let(::domain)

    override fun peek(trainId: String, warningId: String): RestrictionPass? =
        q.selectPass(trainId, warningId).executeAsOneOrNull()?.let(::domain)

    override fun upsert(pass: RestrictionPass) {
        write(pass, dirty = true)
    }

    override fun remove(trainId: String, warningId: String, nowMs: Long) {
        val cur = q.selectPass(trainId, warningId).executeAsOneOrNull() ?: return
        q.tombstonePass(maxOf(nowMs, cur.updatedAt + 1), trainId, warningId)
    }

    override fun deleteByTrain(trainId: String) {
        q.tombstonePassesByTrain(nowMs(), trainId)
    }

    override fun pendingUpload(limit: Int): List<RestrictionPass> = q.selectPendingPasses(limit.toLong()).executeAsList().map(::domain)

    override fun markUploaded(sent: List<RestrictionPass>) {
        db.transaction {
            sent.forEach { q.markPassUploaded(it.trainId, it.warningId, it.updatedAt) }
            q.purgeUploadedTombstones()
        }
    }

    override fun mergeRemote(remote: List<RestrictionPass>) {
        db.transaction {
            remote.forEach { r ->
                val cur = q.selectPass(r.trainId, r.warningId).executeAsOneOrNull()
                if (cur == null || r.updatedAt > cur.updatedAt) {
                    // Серверное надгробие без локальной записи хранить не нужно.
                    if (r.isDeleted && cur == null) return@forEach
                    write(r, dirty = false)
                }
            }
            q.purgeUploadedTombstones()
        }
    }

    override fun deleteAll() {
        q.deleteAllPasses()
    }

    private fun write(p: RestrictionPass, dirty: Boolean) {
        q.upsertPass(
            p.trainId, p.warningId, p.routeId, p.sectionId, p.kind, p.title, p.place, p.note,
            p.limitKmh?.toLong(), p.startAbsKm, p.endAbsKm, p.startRelKm, p.endRelKm,
            p.enterAt, p.enterSpeedKmh, flag(p.enterApprox), p.exitAt, p.exitSpeedKmh, flag(p.exitByHead),
            p.trainLengthM?.toLong(), p.dir.toLong(), p.maxSpeedKmh, flag(p.exceeded),
            p.updatedAt, flag(p.isDeleted), flag(dirty),
        )
    }

    private fun flag(b: Boolean) = if (b) 1L else 0L

    private fun domain(r: Row) = RestrictionPass(
        trainId = r.trainId, warningId = r.warningId, routeId = r.routeId, sectionId = r.sectionId, kind = r.kind,
        title = r.title, place = r.place, note = r.note, limitKmh = r.limitKmh?.toInt(),
        startAbsKm = r.startAbsKm, endAbsKm = r.endAbsKm, startRelKm = r.startRelKm, endRelKm = r.endRelKm,
        enterAt = r.enterAt, enterSpeedKmh = r.enterSpeedKmh, enterApprox = r.enterApprox != 0L,
        exitAt = r.exitAt, exitSpeedKmh = r.exitSpeedKmh, exitByHead = r.exitByHead != 0L,
        trainLengthM = r.trainLengthM?.toInt(), dir = r.dir.toInt(), maxSpeedKmh = r.maxSpeedKmh,
        exceeded = r.exceeded != 0L, updatedAt = r.updatedAt, isDeleted = r.isDeleted != 0L,
    )
}
