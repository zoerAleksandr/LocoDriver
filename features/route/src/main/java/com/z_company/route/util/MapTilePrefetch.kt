package com.z_company.route.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.z_company.domain.track.AxisPoint
import com.z_company.route.component.servicephase.STYLE_URL
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.offline.OfflineGeometryRegionDefinition
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.tan

/**
 * Тайлы подложки вдоль оси скачанного участка (п. 4 плана: подложка работает без интернета).
 * Регион - полоса по линии оси ([OfflineGeometryRegionDefinition], зумы [MIN_ZOOM]..[MAX_ZOOM]),
 * один пак на участок (повторное скачивание заменяет прежний). Размер для диалога считается заранее
 * по числу тайлов в полосе ([TILE_AVG_KB] на тайл). По Wi-Fi качается молча, по мобильной сети -
 * после предупреждения о размере - скачать можно и по мобильной сети (жёсткого запрета нет).
 */
object MapTilePrefetch {
    const val MIN_ZOOM = 7.0
    const val MAX_ZOOM = 12.0
    const val TILE_AVG_KB = 20.0
    private const val DOWNLOAD_TIMEOUT_MS = 15 * 60 * 1000L

    /** Оценка числа тайлов в полосе вдоль линии (зумы MIN..MAX) по точкам оси. */
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

    /** Оценка размера пака, МБ (по среднему размеру тайла). */
    fun estimateMb(tileCount: Int): Double = tileCount * TILE_AVG_KB / 1024.0

    /** Безлимитная сеть (Wi-Fi/ethernet): тайлы качаются без вопроса. */
    fun isUnmetered(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Скачать тайлы вдоль оси [points] участка [sectionId]; прежний пак участка заменяется.
     * true - пак создан и загрузка завершилась (или нечего качать); false - ошибка/таймаут.
     */
    suspend fun download(context: Context, sectionId: String, axisHash: String, points: List<AxisPoint>): Boolean {
        if (points.size < 2) return true
        val app = context.applicationContext
        val definition = OfflineGeometryRegionDefinition(
            STYLE_URL,
            LineString.fromLngLats(points.map { Point.fromLngLat(it.lon, it.lat) }),
            MIN_ZOOM, MAX_ZOOM, 1f,
        )
        val metadata = """{"sectionId":"$sectionId","axisHash":"$axisHash"}""".toByteArray()
        return withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine<Boolean> { cont ->
                deletePacks(app, sectionId) {
                    OfflineManager.getInstance(app).createOfflineRegion(definition, metadata,
                        object : OfflineManager.CreateOfflineRegionCallback {
                            override fun onCreate(region: OfflineRegion) {
                                if (!cont.isActive) return
                                region.setDeliverInactiveMessages(true)
                                region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                                    override fun onStatusChanged(status: OfflineRegionStatus) {
                                        if (status.requiredResourceCount > 0 &&
                                            status.completedResourceCount >= status.requiredResourceCount
                                        ) {
                                            region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                            if (cont.isActive) cont.resume(true)
                                        }
                                    }

                                    override fun onError(error: OfflineRegionError) {
                                        if (cont.isActive) cont.resume(false)
                                    }

                                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                                        if (cont.isActive) cont.resume(false)
                                    }
                                })
                                region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                            }

                            override fun onError(error: String) {
                                if (cont.isActive) cont.resume(false)
                            }
                        })
                }
            }
        } ?: false
    }

    /** Удалить тайлы участка (при «Удалить» на телефоне). */
    suspend fun delete(context: Context, sectionId: String): Unit = suspendCancellableCoroutine { cont ->
        deletePacks(context.applicationContext, sectionId) { if (cont.isActive) cont.resume(Unit) }
    }

    private fun deletePacks(app: Context, sectionId: String, done: () -> Unit) {
        OfflineManager.getInstance(app).listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val targets = offlineRegions.orEmpty().filter {
                    runCatching { String(it.metadata).contains("\"sectionId\":\"$sectionId\"") }.getOrDefault(false)
                }
                if (targets.isEmpty()) {
                    done()
                    return
                }
                var left = targets.size
                targets.forEach { region ->
                    region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                        override fun onDelete() {
                            left--
                            if (left == 0) done()
                        }

                        override fun onError(error: String) {
                            left--
                            if (left == 0) done()
                        }
                    })
                }
            }

            override fun onError(error: String) {
                done()
            }
        })
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
