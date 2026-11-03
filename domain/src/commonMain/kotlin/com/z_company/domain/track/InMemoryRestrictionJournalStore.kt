package com.z_company.domain.track

/** Журнал в памяти: тесты и запасной вариант без БД. Потокобезопасность - блокировкой на себя. */
class InMemoryRestrictionJournalStore : RestrictionJournalStore {
    private val rows = LinkedHashMap<Pair<String, String>, RestrictionPass>()
    private val dirty = HashSet<Pair<String, String>>()

    private fun key(p: RestrictionPass) = p.trainId to p.warningId

    override fun forTrain(trainId: String) = rows.values.filter { it.trainId == trainId && !it.isDeleted }

    override fun inZoneExcept(trainId: String) = rows.values.filter {
        !it.isDeleted && it.trainId != trainId && it.kind == RestrictionKind.SPEED && it.enterAt != null && it.exitAt == null
    }

    override fun openForTrain(trainId: String) = forTrain(trainId).filter { it.status != RestrictionStatus.DONE }

    override fun get(trainId: String, warningId: String) = rows[trainId to warningId]?.takeIf { !it.isDeleted }

    override fun peek(trainId: String, warningId: String) = rows[trainId to warningId]

    override fun upsert(pass: RestrictionPass) {
        rows[key(pass)] = pass
        dirty += key(pass)
    }

    override fun remove(trainId: String, warningId: String, nowMs: Long) {
        val cur = rows[trainId to warningId] ?: return
        rows[trainId to warningId] = cur.copy(isDeleted = true, updatedAt = maxOf(nowMs, cur.updatedAt + 1))
        dirty += trainId to warningId
    }

    override fun deleteByTrain(trainId: String) {
        // Надгробия, как в Track.db: об удалении должны узнать сервер и другие устройства.
        rows.entries.filter { it.key.first == trainId && !it.value.isDeleted }.forEach {
            rows[it.key] = it.value.copy(isDeleted = true, updatedAt = it.value.updatedAt + 1)
            dirty += it.key
        }
    }

    override fun pendingUpload(limit: Int) = dirty.mapNotNull { rows[it] }.take(limit)

    override fun markUploaded(sent: List<RestrictionPass>) {
        sent.forEach { s -> if (rows[key(s)]?.updatedAt == s.updatedAt) dirty -= key(s) }
    }

    override fun mergeRemote(remote: List<RestrictionPass>) {
        remote.forEach { r ->
            val cur = rows[key(r)]
            if (cur == null || r.updatedAt > cur.updatedAt) {
                rows[key(r)] = r
                dirty -= key(r)
            }
        }
    }

    override fun deleteAll() {
        rows.clear(); dirty.clear()
    }
}
