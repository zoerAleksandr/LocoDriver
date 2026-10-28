package com.z_company.domain.track

import kotlin.math.abs

/** Фикс для журнала: координаты нужны только чтобы найти место на оси участка, в журнал не попадают и не логируются. */
data class RestrictionSample(
    val timeMs: Long,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double?,
    val accuracyM: Float?,
)

/** Предыдущее положение головы на оси: для интерполяции момента пересечения границы между двумя фиксами. */
data class HeadSample(val km: Double, val timeMs: Long, val speedKmh: Double?)

/**
 * Чистая математика проследования одной записи: заход головой, выход хвостом, максимальная скорость. Без состояния
 * (состояние - сама запись): после перезапуска процесса работа продолжается с сохранённой записи.
 */
object RestrictionPassMath {
    private enum class Side { BEFORE, INSIDE, AFTER }

    /** Зона на оси участка: `[from; to]`, from <= to. Через км-отметки, если есть, иначе по сохранённым км оси. */
    fun zone(section: SectionProfile, e: RestrictionPass): Pair<Double, Double>? {
        val a: Double
        val b: Double
        if (e.startAbsKm != null && e.endAbsKm != null && section.kmMarks.isNotEmpty()) {
            a = KmMarks.toRel(section.kmMarks, e.startAbsKm) ?: return null
            b = KmMarks.toRel(section.kmMarks, e.endAbsKm) ?: return null
        } else {
            a = e.startRelKm
            b = e.endRelKm
        }
        return minOf(a, b) to maxOf(a, b)
    }

    private fun side(from: Double, to: Double, p: Double, dir: Int): Side =
        if (dir >= 0) when { p < from -> Side.BEFORE; p > to -> Side.AFTER; else -> Side.INSIDE }
        else when { p > to -> Side.BEFORE; p < from -> Side.AFTER; else -> Side.INSIDE }

    /** Линейная интерполяция времени и скорости в точке [atKm] между [prev] и текущим положением. */
    fun interpolate(prev: HeadSample?, km: Double, timeMs: Long, speedKmh: Double?, atKm: Double): Pair<Long, Double?> {
        if (prev == null || abs(km - prev.km) < 1e-9) return timeMs to speedKmh
        val f = ((atKm - prev.km) / (km - prev.km)).coerceIn(0.0, 1.0)
        val t = prev.timeMs + ((timeMs - prev.timeMs) * f).toLong()
        val v = if (prev.speedKmh != null && speedKmh != null) prev.speedKmh + (speedKmh - prev.speedKmh) * f else speedKmh ?: prev.speedKmh
        return t to v
    }

    /**
     * Один фикс. [seenBefore] - голова уже была до зоны в этом сеансе (тогда заход точный, иначе - по первому фиксу внутри);
     * [lengthKm] - длина поезда в км (null - выход по голове). Возвращает новую запись или null, если ничего не изменилось.
     */
    fun step(
        e: RestrictionPass,
        zone: Pair<Double, Double>,
        headKm: Double,
        dir: Int,
        prev: HeadSample?,
        cur: RestrictionSample,
        lengthKm: Double?,
        seenBefore: Boolean,
        trainLengthM: Int?,
    ): RestrictionPass? {
        val (from, to) = zone
        val speed = cur.speedKmh
        if (e.enterAt == null) {
            if (dir == 0 && headKm !in from..to) return null
            val d = if (dir == 0) 1 else dir
            val sd = side(from, to, headKm, d)
            if (sd == Side.BEFORE) return null
            if (sd == Side.AFTER && !seenBefore) return null // зону уже проехали, момента входа не знаем
            val boundary = if (d > 0) from else to
            val (t, v) = if (seenBefore) interpolate(prev, headKm, cur.timeMs, speed, boundary) else cur.timeMs to speed
            var n = e.copy(
                enterAt = t, enterSpeedKmh = v, enterApprox = !seenBefore, dir = dir, trainLengthM = trainLengthM,
                maxSpeedKmh = v, updatedAt = cur.timeMs,
            )
            if (e.kind == RestrictionKind.SPEED) n = n.copy(exceeded = RestrictionPass.exceeded(n.maxSpeedKmh, e.limitKmh))
            if (e.kind == RestrictionKind.SPEED && sd == Side.AFTER) return finishExit(n, zone, headKm, d, prev, cur, lengthKm, trainLengthM) ?: n
            return n
        }
        if (e.kind != RestrictionKind.SPEED || e.exitAt != null) return null
        val d = if (e.dir != 0) e.dir else if (dir != 0) dir else 1
        val withMax = bumpMax(e, speed, cur.timeMs)
        val exited = finishExit(withMax ?: e, zone, headKm, d, prev, cur, lengthKm, trainLengthM)
        return exited ?: withMax
    }

    private fun bumpMax(e: RestrictionPass, speed: Double?, nowMs: Long): RestrictionPass? {
        if (speed == null || (e.maxSpeedKmh != null && speed <= e.maxSpeedKmh)) return null
        return e.copy(
            maxSpeedKmh = speed,
            exceeded = RestrictionPass.exceeded(speed, e.limitKmh) || e.exceeded,
            updatedAt = nowMs,
        )
    }

    private fun finishExit(
        e: RestrictionPass, zone: Pair<Double, Double>, headKm: Double, d: Int, prev: HeadSample?,
        cur: RestrictionSample, lengthKm: Double?, trainLengthM: Int?,
    ): RestrictionPass? {
        val (from, to) = zone
        val len = lengthKm ?: 0.0
        // Голова на расстоянии длины поезда за дальним краем зоны = хвост вышел.
        val boundary = if (d > 0) to + len else from - len
        val passed = if (d > 0) headKm >= boundary else headKm <= boundary
        if (!passed) return null
        val (t, v) = interpolate(prev, headKm, cur.timeMs, cur.speedKmh, boundary)
        val maxV = listOfNotNull(e.maxSpeedKmh, cur.speedKmh, v).maxOrNull()
        return e.copy(
            exitAt = t, exitSpeedKmh = v, exitByHead = lengthKm == null, trainLengthM = trainLengthM ?: e.trainLengthM,
            maxSpeedKmh = maxV, exceeded = RestrictionPass.exceeded(maxV, e.limitKmh) || e.exceeded, updatedAt = cur.timeMs,
        )
    }
}

/**
 * Фоновая запись журнала: на каждый фикс «АВТО» находит положение головы на оси участка (в памяти, без записи координат)
 * и двигает записи журнала поезда. Состояние лежит в записях ([RestrictionJournalStore]), поэтому переживает перезапуск
 * процесса; в памяти только то, что восстанавливается за пару фиксов (направление, предыдущая точка).
 * Вызывать последовательно (из одной корутины).
 */
class RestrictionJournalRecorder(
    private val store: RestrictionJournalStore,
    /** Участок по id (локальный кэш, без сети); null - нет данных. */
    private val sections: suspend (String) -> SectionProfile?,
    /** Поправка оси, км (ручная привязка профиля); 0 - нет. */
    private val calibrationKm: (SectionProfile) -> Double = { 0.0 },
    private val nowMs: () -> Long,
) {
    private class SectionState(val live: LiveProfile) {
        var prev: HeadSample? = null
        var dir = 0
        var refKm: Double? = null
    }

    private val states = HashMap<String, SectionState>()
    private val missingUntil = HashMap<String, Long>()
    private val seenBefore = HashSet<String>()
    private val lastWrite = HashMap<String, Long>()
    /** Последний фикс и поезд, которому он был отдан: переживают [reset] (выключили «АВТО» на одном поезде, включили на другом). */
    private var lastTrain: String? = null
    private var lastSample: RestrictionSample? = null
    private var cacheTrain: String? = null
    private var cacheAt = 0L
    private var cache: List<RestrictionPass> = emptyList()

    /** Записи изменились снаружи (привязали/сняли предупреждение): следующий фикс перечитает их из хранилища. */
    fun invalidate() { cacheAt = 0L }

    fun reset() {
        states.clear(); seenBefore.clear(); lastWrite.clear(); cache = emptyList(); cacheTrain = null; cacheAt = 0L
    }

    /**
     * [lengthM] - длина поезда сейчас в метрах (null - не указана); вызывается лениво, только когда у поезда есть
     * непроследованные записи (на каждый фикс «АВТО» без них работа сводится к одному обращению к кэшу).
     */
    suspend fun onPoint(trainId: String, p: RestrictionSample, lengthM: () -> Int?) {
        val now = nowMs()
        if (cacheTrain != trainId) closeForeignZones(trainId)
        lastTrain = trainId
        lastSample = p
        if (cacheTrain != trainId || now - cacheAt > CACHE_MS) {
            cache = store.openForTrain(trainId)
            cacheTrain = trainId
            cacheAt = now
        }
        if (cache.isEmpty()) return
        val trainLengthM = lengthM()
        val lengthKm = trainLengthM?.takeIf { it > 0 }?.let { it / 1000.0 }
        val bySection = cache.groupBy { it.sectionId }
        for ((sectionId, entries) in bySection) {
            val st = stateFor(sectionId, now) ?: continue
            val pos = st.live.onFix(p.timeMs, p.latitude, p.longitude, p.speedKmh, p.accuracyM)
            if (pos == null || pos.offRoute || !pos.locked) continue
            val km = pos.kmAxis
            val ref = st.refKm
            if (ref == null) st.refKm = km
            else if (abs(km - ref) >= DIR_STEP_KM) { st.dir = if (km > ref) 1 else -1; st.refKm = km }
            for (e in entries) {
                val zone = RestrictionPassMath.zone(st.live.section, e) ?: continue
                val key = e.trainId + ":" + e.warningId
                val d = if (e.enterAt != null && e.dir != 0) e.dir else st.dir
                val before = d != 0 && (if (d >= 0) km < zone.first else km > zone.second)
                if (before) seenBefore.add(key)
                val n = RestrictionPassMath.step(e, zone, km, st.dir, st.prev, p, lengthKm, key in seenBefore, trainLengthM) ?: continue
                val important = n.enterAt != e.enterAt || n.exitAt != e.exitAt
                val last = lastWrite[key] ?: 0L
                if (important || now - last >= MAX_WRITE_MS) {
                    store.upsert(n)
                    lastWrite[key] = now
                    cacheAt = 0L // перечитать: запись могла стать завершённой
                } else {
                    // Максимум растёт между записями: держим в кэше, на диск - по таймеру или при событии.
                    cache = cache.map { if (it.warningId == n.warningId) n else it }
                }
            }
            st.prev = HeadSample(km, p.timeMs, p.speedKmh)
        }
    }

    /**
     * Цель «АВТО» сменилась (или первый фикс после запуска): ограничения скорости других поездов, в зону которых голова зашла, но выход
     * не записан, закрываются - иначе они висели бы «Состав в зоне» вечно и блокировали отчёт. Время выхода - последний фикс прежнего
     * поезда (в памяти) либо метка последнего обновления записи; скорость выхода - со того фикса, выход помечается «по голове».
     * Записи без захода («ожидает проследования») не трогаются: они просто ждут, когда «АВТО» снова включат на их поезде.
     */
    private fun closeForeignZones(current: String) {
        val now = nowMs()
        for (e in store.inZoneExcept(current)) {
            val last = lastSample?.takeIf { lastTrain == e.trainId }
            val at = maxOf(last?.timeMs ?: e.updatedAt, e.enterAt ?: 0L)
            store.upsert(
                e.copy(
                    exitAt = at, exitSpeedKmh = last?.speedKmh, exitByHead = true,
                    updatedAt = maxOf(now, e.updatedAt + 1),
                ),
            )
        }
    }

    private suspend fun stateFor(sectionId: String, now: Long): SectionState? {
        states[sectionId]?.let { return it }
        if ((missingUntil[sectionId] ?: 0L) > now) return null
        val section = sections(sectionId)
        if (section == null) { missingUntil[sectionId] = now + MISSING_RETRY_MS; return null }
        val live = LiveProfile(section, reversed = false).also { it.applyCalibration(calibrationKm(section)) }
        return SectionState(live).also { states[sectionId] = it }
    }

    private companion object {
        const val CACHE_MS = 3_000L
        const val MAX_WRITE_MS = 20_000L
        const val DIR_STEP_KM = 0.03
        const val MISSING_RETRY_MS = 60_000L
    }
}
