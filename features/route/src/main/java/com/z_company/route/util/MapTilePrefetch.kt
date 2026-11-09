package com.z_company.route.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.z_company.domain.track.AxisPoint
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

/** Полностью скачанные версии карты участка; во время атомарной замены старая и новая могут сосуществовать. */
data class OfflineTilePacks(val bytesByAxisHash: Map<String, Long>) {
    fun bytesFor(axisHash: String): Long? = bytesByAxisHash[axisHash]
    val totalBytes: Long get() = bytesByAxisHash.values.sum()
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

    internal data class PackMetadata(
        val sectionId: String,
        val axisHash: String,
        val state: String,
        val createdAt: Long,
    )

    private class Active {
        @Volatile var region: OfflineRegion? = null
        @Volatile var cont: CancellableContinuation<Boolean>? = null
        @Volatile var finishing: Boolean = false
        @Volatile var finished: Boolean = false
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

    fun estimateMb(tileCount: Int): Double = tileCount * TILE_AVG_KB / 1024.0

    /** Сеть считается бесплатной по системному признаку, а не просто по типу Wi-Fi. */
    fun isUnmetered(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /** Скачать новую версию карты, не трогая прежний ready-пак до полного успеха. */
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
        val definition = OfflineGeometryRegionDefinition(
            STYLE_URL,
            LineString.fromLngLats(points.map { Point.fromLngLat(it.lon, it.lat) }),
            MIN_ZOOM,
            MAX_ZOOM,
            1f,
        )
        return try {
            withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    slot.cont = cont
                    cont.invokeOnCancellation { discardStaging(slot) }
                    manager(app).createOfflineRegion(
                        definition,
                        encodeMetadata(PackMetadata(sectionId, axisHash, STATE_STAGING, createdAt)),
                        object : OfflineManager.CreateOfflineRegionCallback {
                            override fun onCreate(region: OfflineRegion) {
                                slot.region = region
                                if (!cont.isActive || slot.finished) {
                                    deleteRegion(region)
                                    return
                                }
                                region.setDeliverInactiveMessages(true)
                                region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                                    override fun onStatusChanged(status: OfflineRegionStatus) {
                                        if (slot.finished || slot.finishing) return
                                        onProgress(status.completedResourceCount, status.requiredResourceCount)
                                        if (isComplete(status)) {
                                            slot.finishing = true
                                            region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                            makeReady(app, region, sectionId, axisHash, createdAt, slot, cont)
                                        }
                                    }

                                    override fun onError(error: OfflineRegionError) = finishFailure(slot)

                                    override fun mapboxTileCountLimitExceeded(limit: Long) = finishFailure(slot)
                                })
                                region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                            }

                            override fun onError(error: String) = finishFailure(slot)
                        },
                    )
                }
            } ?: false
        } finally {
            active.remove(sectionId, slot)
            if (!slot.finished) discardStaging(slot)
        }
    }

    /** Отменить только staging-регион; прежний ready-пак остаётся на телефоне. */
    fun cancel(context: Context, sectionId: String) {
        val slot = active.remove(sectionId) ?: return
        if (slot.finished) return
        slot.finished = true
        discardStaging(slot)
        slot.cont?.let { if (it.isActive) it.resume(false) }
    }

    /** После фиксации профиля оставить только самый новый ready-пак его оси. */
    suspend fun commit(context: Context, sectionId: String, axisHash: String): Unit = suspendCancellableCoroutine { cont ->
        val app = context.applicationContext
        manager(app).listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val owned = offlineRegions.orEmpty().mapNotNull { region ->
                    parseMetadata(region.metadata)?.takeIf { it.sectionId == sectionId }?.let { region to it }
                }
                val keep = owned.filter { it.second.state == STATE_READY && it.second.axisHash == axisHash }
                    .maxByOrNull { it.second.createdAt }?.first?.id
                // Если новый ready-регион неожиданно исчез между download и commit,
                // старую рабочую карту не трогаем: профиль ещё можно откатить/обновить позднее.
                if (keep == null) {
                    if (cont.isActive) cont.resume(Unit)
                    return
                }
                deleteRegions(owned.map { it.first }.filter { it.id != keep }) {
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
                val result = ConcurrentHashMap<String, ConcurrentHashMap<String, Candidate>>()
                val left = AtomicInteger(candidates.size)
                fun completeOne() {
                    if (left.decrementAndGet() == 0 && cont.isActive) {
                        cont.resume(result.mapValues { entry ->
                            OfflineTilePacks(entry.value.mapValues { it.value.bytes })
                        })
                    }
                }
                candidates.forEach { (region, metadata) ->
                    region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                        override fun onStatus(status: OfflineRegionStatus?) {
                            if (status != null && isComplete(status)) {
                                val byAxis = result.computeIfAbsent(metadata.sectionId) { ConcurrentHashMap() }
                                byAxis.compute(metadata.axisHash) { _, old ->
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
        app: Context,
        region: OfflineRegion,
        sectionId: String,
        axisHash: String,
        createdAt: Long,
        slot: Active,
        cont: CancellableContinuation<Boolean>,
    ) {
        region.updateMetadata(
            encodeMetadata(PackMetadata(sectionId, axisHash, STATE_READY, createdAt)),
            object : OfflineRegion.OfflineRegionUpdateMetadataCallback {
                override fun onUpdate(metadata: ByteArray) {
                    if (!cont.isActive || slot.finished) {
                        deleteRegion(region)
                        return
                    }
                    slot.finished = true
                    cont.resume(true)
                }

                override fun onError(error: String) = finishFailure(slot)
            },
        )
    }

    private fun finishFailure(slot: Active) {
        if (slot.finished) return
        slot.finished = true
        discardStaging(slot)
        slot.cont?.let { if (it.isActive) it.resume(false) }
    }

    private fun discardStaging(slot: Active) {
        slot.region?.let { region ->
            runCatching { region.setDownloadState(OfflineRegion.STATE_INACTIVE) }
            deleteRegion(region)
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
        """{"owner":"$OWNER","sectionId":"${m.sectionId}","axisHash":"${m.axisHash}","state":"${m.state}","createdAt":${m.createdAt}}""".toByteArray()

    /** Старые JSON-паки без owner/state считаются ready для бесшовного обновления. */
    internal fun parseMetadata(raw: ByteArray): PackMetadata? {
        val text = runCatching { raw.decodeToString() }.getOrNull() ?: return null
        val sectionId = Regex("\\\"sectionId\\\":\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1) ?: return null
        val axisHash = Regex("\\\"axisHash\\\":\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1) ?: return null
        val owner = Regex("\\\"owner\\\":\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1)
        if (owner != null && owner != OWNER) return null
        val state = Regex("\\\"state\\\":\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1) ?: STATE_READY
        val createdAt = Regex("\\\"createdAt\\\":([0-9]+)").find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        return PackMetadata(sectionId, axisHash, state, createdAt)
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
