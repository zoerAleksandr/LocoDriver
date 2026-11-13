package com.z_company.route.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.z_company.domain.track.AxisPoint
import com.z_company.route.component.servicephase.DARK_STYLE_URL
import com.z_company.route.component.servicephase.STYLE_URL
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.MapLibre
import org.maplibre.android.offline.OfflineGeometryRegionDefinition
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.tan

/** Пакеты по осям и стилям; участок полностью готов только при наличии обеих подложек. */
data class OfflineTilePacks(val bytesByAxisHashAndStyle: Map<String, Map<String, Long>>) {
    fun bytesFor(axisHash: String): Long? {
        val styles = bytesByAxisHashAndStyle[axisHash] ?: return null
        if (!styles.keys.containsAll(setOf(MapTilePrefetch.STYLE_LIGHT, MapTilePrefetch.STYLE_DARK))) return null
        return styles.values.sum()
    }

    val totalBytes: Long get() = bytesByAxisHashAndStyle.values.sumOf { it.values.sum() }
}

/**
 * Тайлы подложки вдоль оси скачанного участка. Новая карта создаётся отдельным staging-регионом;
 * прежний рабочий пакет удаляется только после полной загрузки и перевода staging в ready.
 */
object MapTilePrefetch {
    const val MIN_ZOOM = 7.0
    const val MAX_ZOOM = 12.0
    const val TILE_AVG_KB = 20.0
    private const val DOWNLOAD_TIMEOUT_MS = 15 * 60 * 1000L
    private const val OWNER = "locodriver-offline"
    private const val STATE_STAGING = "staging"
    private const val STATE_READY = "ready"
    internal const val STYLE_LIGHT = "light"
    internal const val STYLE_DARK = "dark"
    private val REQUIRED_STYLE_IDS = setOf(STYLE_LIGHT, STYLE_DARK)
    private val styleUrls = listOf(STYLE_LIGHT to STYLE_URL, STYLE_DARK to DARK_STYLE_URL)

    internal data class PackMetadata(
        val sectionId: String,
        val axisHash: String,
        val state: String,
        val createdAt: Long,
        val styleId: String = STYLE_LIGHT,
    )

    private class Attempt {
        @Volatile var region: OfflineRegion? = null
        @Volatile var cont: CancellableContinuation<Boolean>? = null
        @Volatile var finishing: Boolean = false
        @Volatile var finished: Boolean = false
    }

    private class Active {
        @Volatile var current: Attempt? = null
        val createdRegions = java.util.Collections.synchronizedList(mutableListOf<OfflineRegion>())
        @Volatile var cancelled = false
        @Volatile var complete = false
    }

    private val active = ConcurrentHashMap<String, Active>()

    fun estimateTileCount(points: List<AxisPoint>): Int {
        if (points.size < 2) return 0
        val tiles = HashSet<Long>()
        fun addTile(z: Int, lat: Double, lon: Double) {
            val n = 1 shl z
            val x = ((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
            val la = lat.coerceIn(-85.0, 85.0)
            val rad = Math.toRadians(la)
            val y = ((1.0 - ln(tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
            tiles.add((z.toLong() shl 56) or (x.toLong() shl 28) or y.toLong())
        }
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val steps = max(1, (distM(a.lat, a.lon, b.lat, b.lon) / 250.0).toInt())
            for (k in 0..steps) {
                val t = k.toDouble() / steps
                val lat = a.lat + (b.lat - a.lat) * t
                val lon = a.lon + (b.lon - a.lon) * t
                for (z in MIN_ZOOM.toInt()..MAX_ZOOM.toInt()) addTile(z, lat, lon)
            }
        }
        return tiles.size
    }

    /** Оценка включает два независимых стилевых пака; общие ресурсы MapLibre может дедуплицировать. */
    fun estimateMb(tileCount: Int): Double = tileCount * TILE_AVG_KB * REQUIRED_STYLE_IDS.size / 1024.0

    /** Сеть считается бесплатной по системному признаку, а не просто по типу Wi-Fi. */
    fun isUnmetered(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /** Скачать обе темы карты, не трогая прежнюю пару паков до полного успеха. */
    suspend fun download(
        context: Context,
        sectionId: String,
        axisHash: String,
        points: List<AxisPoint>,
        onProgress: (done: Long, total: Long) -> Unit = { _, _ -> },
    ): Boolean {
        if (points.size < 2) return false
        val slot = Active()
        if (active.putIfAbsent(sectionId, slot) != null) return false
        val app = context.applicationContext
        val createdAt = System.currentTimeMillis()
        return try {
            withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
                val line = LineString.fromLngLats(points.map { Point.fromLngLat(it.lon, it.lat) })
                styleUrls.forEachIndexed { index, (styleId, styleUrl) ->
                    if (slot.cancelled) return@withTimeoutOrNull false
                    val ok = downloadStyle(
                        app, sectionId, axisHash, createdAt, styleId, styleUrl, line, slot,
                    ) { done, total ->
                        val perStyle = 1_000L
                        val withinStyle = if (total > 0) (done.coerceAtMost(total) * perStyle / total) else 0L
                        onProgress(index * perStyle + withinStyle, styleUrls.size * perStyle)
                    }
                    if (!ok) return@withTimeoutOrNull false
                }
                slot.complete = true
                true
            } ?: false
        } finally {
            active.remove(sectionId, slot)
            if (!slot.complete) discardBatch(slot)
        }
    }

    private suspend fun downloadStyle(
        app: Context,
        sectionId: String,
        axisHash: String,
        createdAt: Long,
        styleId: String,
        styleUrl: String,
        line: LineString,
        batch: Active,
        onProgress: (Long, Long) -> Unit,
    ): Boolean {
        val attempt = Attempt()
        batch.current = attempt
        val definition = OfflineGeometryRegionDefinition(styleUrl, line, MIN_ZOOM, MAX_ZOOM, 1f)
        return suspendCancellableCoroutine { cont ->
            attempt.cont = cont
            cont.invokeOnCancellation { discardStaging(attempt) }
            manager(app).createOfflineRegion(
                definition,
                encodeMetadata(PackMetadata(sectionId, axisHash, STATE_STAGING, createdAt, styleId)),
                object : OfflineManager.CreateOfflineRegionCallback {
                    override fun onCreate(offlineRegion: OfflineRegion) {
                        attempt.region = offlineRegion
                        if (!cont.isActive || attempt.finished || batch.cancelled) {
                            deleteRegion(offlineRegion)
                            return
                        }
                        offlineRegion.setDeliverInactiveMessages(true)
                        offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                            override fun onStatusChanged(status: OfflineRegionStatus) {
                                if (attempt.finished || attempt.finishing || batch.cancelled) return
                                onProgress(status.completedResourceCount.toLong(), status.requiredResourceCount.toLong())
                                if (isComplete(status)) {
                                    attempt.finishing = true
                                    offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                    makeReady(offlineRegion, sectionId, axisHash, createdAt, styleId, batch, attempt, cont)
                                }
                            }

                            override fun onError(error: OfflineRegionError) = finishFailure(batch, attempt)
                            override fun mapboxTileCountLimitExceeded(limit: Long) = finishFailure(batch, attempt)
                        })
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                    }

                    override fun onError(error: String) = finishFailure(batch, attempt)
                },
            )
        }.also { batch.current = null }
    }

    /** Отменить текущий staging-регион и новые паки; прежняя пара ready-паков остаётся на телефоне. */
    fun cancel(context: Context, sectionId: String) {
        val slot = active.remove(sectionId) ?: return
        if (slot.complete) return
        slot.cancelled = true
        slot.current?.let { attempt ->
            attempt.finished = true
            discardStaging(attempt)
            attempt.cont?.let { if (it.isActive) it.resume(false) }
        }
        discardBatch(slot)
    }

    /** После фиксации профиля оставить самый новый ready-пак каждой темы его оси. */
    suspend fun commit(context: Context, sectionId: String, axisHash: String): Unit = suspendCancellableCoroutine { cont ->
        val app = context.applicationContext
        manager(app).listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val owned = offlineRegions.orEmpty().mapNotNull { region ->
                    parseMetadata(region.metadata)?.takeIf { it.sectionId == sectionId }?.let { region to it }
                }
                val current = owned.filter { it.second.state == STATE_READY && it.second.axisHash == axisHash }
                val keep = REQUIRED_STYLE_IDS.mapNotNull { styleId ->
                    current.filter { it.second.styleId == styleId }.maxByOrNull { it.second.createdAt }?.first
                }
                // Если не хватает одной темы, старую рабочую пару не трогаем.
                if (keep.size != REQUIRED_STYLE_IDS.size) {
                    if (cont.isActive) cont.resume(Unit)
                    return
                }
                val keepIds = keep.map { it.id }.toSet()
                deleteRegions(owned.map { it.first }.filter { it.id !in keepIds }) {
                    if (cont.isActive) cont.resume(Unit)
                }
            }

            override fun onError(error: String) {
                if (cont.isActive) cont.resume(Unit)
            }
        })
    }

    /** Удалить все принадлежащие приложению пакеты участка. */
    suspend fun delete(context: Context, sectionId: String): Unit = suspendCancellableCoroutine { cont ->
        deletePacks(context.applicationContext, sectionId, keepRegionId = null) {
            if (cont.isActive) cont.resume(Unit)
        }
    }

    /** Только полностью завершённые ready-паки; staging и нулевые/частичные регионы игнорируются. */
    suspend fun downloadedPacks(context: Context): Map<String, OfflineTilePacks> = suspendCancellableCoroutine { cont ->
        manager(context.applicationContext).listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val now = System.currentTimeMillis()
                val candidates = offlineRegions.orEmpty().mapNotNull { region ->
                    val metadata = parseMetadata(region.metadata) ?: return@mapNotNull null
                    if (metadata.state == STATE_STAGING) {
                        // После падения процесса observer уже не вернётся. Старый staging
                        // не считается скачанной картой и удаляется как мусор.
                        if (metadata.createdAt > 0 && now - metadata.createdAt > DOWNLOAD_TIMEOUT_MS) {
                            deleteRegion(region)
                        }
                        return@mapNotNull null
                    }
                    metadata.takeIf { it.state == STATE_READY }?.let { region to it }
                }
                if (candidates.isEmpty()) {
                    if (cont.isActive) cont.resume(emptyMap())
                    return
                }
                data class Candidate(val metadata: PackMetadata, val bytes: Long)
                val result = ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, Candidate>>>()
                val left = AtomicInteger(candidates.size)
                fun completeOne() {
                    if (left.decrementAndGet() == 0 && cont.isActive) {
                        cont.resume(result.mapValues { entry ->
                            OfflineTilePacks(entry.value.mapValues { axis ->
                                axis.value.mapValues { it.value.bytes }
                            })
                        })
                    }
                }
                candidates.forEach { (region, metadata) ->
                    region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                        override fun onStatus(status: OfflineRegionStatus?) {
                            if (status != null && isComplete(status)) {
                                val byAxis = result.computeIfAbsent(metadata.sectionId) { ConcurrentHashMap() }
                                    .computeIfAbsent(metadata.axisHash) { ConcurrentHashMap() }
                                byAxis.compute(metadata.styleId) { _, old ->
                                    val fresh = Candidate(metadata, status.completedResourceSize)
                                    if (old == null || fresh.metadata.createdAt >= old.metadata.createdAt) fresh else old
                                }
                            }
                            completeOne()
                        }

                        override fun onError(error: String?) = completeOne()
                    })
                }
            }

            override fun onError(error: String) {
                if (cont.isActive) cont.resume(emptyMap())
            }
        })
    }

    private fun makeReady(
        region: OfflineRegion,
        sectionId: String,
        axisHash: String,
        createdAt: Long,
        styleId: String,
        batch: Active,
        attempt: Attempt,
        cont: CancellableContinuation<Boolean>,
    ) {
        region.updateMetadata(
            encodeMetadata(PackMetadata(sectionId, axisHash, STATE_READY, createdAt, styleId)),
            object : OfflineRegion.OfflineRegionUpdateMetadataCallback {
                override fun onUpdate(metadata: ByteArray) {
                    if (!cont.isActive || attempt.finished || batch.cancelled) {
                        deleteRegion(region)
                        return
                    }
                    batch.createdRegions.add(region)
                    attempt.finished = true
                    cont.resume(true)
                }

                override fun onError(error: String) = finishFailure(batch, attempt)
            },
        )
    }

    private fun finishFailure(batch: Active, attempt: Attempt) {
        if (attempt.finished) return
        attempt.finished = true
        discardStaging(attempt)
        attempt.cont?.let { if (it.isActive) it.resume(false) }
    }

    private fun discardStaging(attempt: Attempt) {
        attempt.region?.let { region ->
            runCatching { region.setDownloadState(OfflineRegion.STATE_INACTIVE) }
            deleteRegion(region)
        }
    }

    private fun discardBatch(batch: Active) {
        batch.current?.let(::discardStaging)
        synchronized(batch.createdRegions) {
            batch.createdRegions.toList().forEach(::deleteRegion)
            batch.createdRegions.clear()
        }
    }

    private fun deleteRegion(region: OfflineRegion) {
        runCatching {
            region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() = Unit
                override fun onError(error: String) = Unit
            })
        }
    }

    private fun deletePacks(app: Context, sectionId: String, keepRegionId: Long?, done: () -> Unit) {
        manager(app).listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val targets = offlineRegions.orEmpty().filter { region ->
                    region.id != keepRegionId && parseMetadata(region.metadata)?.sectionId == sectionId
                }
                deleteRegions(targets, done)
            }

            override fun onError(error: String) = done()
        })
    }

    private fun deleteRegions(regions: List<OfflineRegion>, done: () -> Unit) {
        if (regions.isEmpty()) {
            done()
            return
        }
        val left = AtomicInteger(regions.size)
        regions.forEach { region ->
            region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() { if (left.decrementAndGet() == 0) done() }
                override fun onError(error: String) { if (left.decrementAndGet() == 0) done() }
            })
        }
    }

    internal fun encodeMetadata(m: PackMetadata): ByteArray =
        """{"owner":"$OWNER","sectionId":"${m.sectionId}","axisHash":"${m.axisHash}","state":"${m.state}","createdAt":${m.createdAt},"styleId":"${m.styleId}"}""".toByteArray()

    /** Старые JSON-паки без owner/state считаются ready для бесшовного обновления. */
    internal fun parseMetadata(raw: ByteArray): PackMetadata? {
        val text = runCatching { raw.decodeToString() }.getOrNull() ?: return null
        val sectionId = Regex("\\\"sectionId\\\":\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1) ?: return null
        val axisHash = Regex("\\\"axisHash\\\":\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1) ?: return null
        val owner = Regex("\\\"owner\\\":\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1)
        if (owner != null && owner != OWNER) return null
        val state = Regex("\\\"state\\\":\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1) ?: STATE_READY
        val createdAt = Regex("\\\"createdAt\\\":([0-9]+)").find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val styleId = Regex("\\\"styleId\\\":\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1) ?: STYLE_LIGHT
        return PackMetadata(sectionId, axisHash, state, createdAt, styleId)
    }

    private fun isComplete(status: OfflineRegionStatus): Boolean =
        status.requiredResourceCount > 0 &&
            status.completedResourceCount >= status.requiredResourceCount &&
            status.completedResourceSize > 0

    private fun manager(app: Context): OfflineManager {
        runCatching { MapLibre.getInstance(app) }
        return OfflineManager.getInstance(app)
    }

    private fun distM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = p2 - p1
        val dl = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dp / 2) * kotlin.math.sin(dp / 2) +
            kotlin.math.cos(p1) * kotlin.math.cos(p2) * kotlin.math.sin(dl / 2) * kotlin.math.sin(dl / 2)
        return 2 * 6_371_000.0 * kotlin.math.asin(kotlin.math.sqrt(a))
    }
}
