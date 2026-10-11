package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.gnss.GnssCell
import com.z_company.domain.gnss.GnssServerCell
import com.z_company.domain.gnss.GnssTileCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.floor
import kotlin.time.Clock

/** Хранилище кэша плиток на устройстве (файлы в cacheDir на Android). */
interface GnssTileStore {
    fun load(key: String): GnssTileCache?
    fun save(key: String, tile: GnssTileCache)
    fun clear()
}

/**
 * Серверные ячейки «зон без GPS» для видимой области карты (SCREEN_SPECS §6.9, «Зоны без GPS»).
 *
 * Область режется на плитки 0,5° (сервер: область не больше 1°); на плитку - кэш с ETag, не чаще раза в [REFRESH_MS]
 * (5 мин) на плитку, независимо от исхода. Только при входе (нет токена - пусто, кэш стирается). Любая ошибка (404 до
 * развёртывания сервера, 429, 503, сеть) молча даёт то, что есть в кэше. 429 - пауза [RATE_LIMIT_PAUSE_MS] на все запросы.
 * 422 `too_many_cells` - плитка делится на четыре. Координаты не логируются.
 */
class GnssZonesRepository(
    private val api: GnssZonesApi,
    private val store: GnssTileStore,
    /** Полный заголовок `Bearer ...` или null, если пользователь не вошёл. */
    private val token: suspend () -> String?,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()
    private val lastAttempt = HashMap<String, Long>()
    private var pausedUntil = 0L
    private var clearedWhenSignedOut = false

    /** Ячейки плиток, перекрывающих [bbox] (не более [MAX_TILES], ближайшие к центру). Не бросает исключений. */
    suspend fun load(bbox: GnssBbox): List<GnssServerCell> = mutex.withLock {
        try {
            val bearer = token()
            if (bearer.isNullOrBlank()) {
                if (!clearedWhenSignedOut) {
                    runCatching { store.clear() }
                    lastAttempt.clear()
                    pausedUntil = 0L
                    clearedWhenSignedOut = true
                }
                return@withLock emptyList()
            }
            clearedWhenSignedOut = false
            val out = ArrayList<GnssServerCell>()
            for (t in tilesOf(bbox)) out += tileCells(bearer, t)
            out
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun tileCells(bearer: String, t: Tile): List<GnssServerCell> {
        val key = t.key
        val cached = runCatching { store.load(key) }.getOrNull()
        val now = nowMs()
        val last = lastAttempt[key] ?: 0L
        val fresh = (cached != null && now - cached.fetchedAtMs in 0 until REFRESH_MS) || now - last in 0 until REFRESH_MS
        if (fresh || now < pausedUntil) return cached?.cells.orEmpty()
        lastAttempt[key] = now
        return try {
            when (val r = api.zones(bearer, t.bbox, cached?.etag)) {
                GnssZonesFetch.NotModified -> {
                    val keep = cached ?: return emptyList()
                    keep.copy(fetchedAtMs = now).also { runCatching { store.save(key, it) } }.cells
                }
                is GnssZonesFetch.Data -> {
                    val cells = r.body.cells.mapNotNull(::toCell)
                    runCatching { store.save(key, GnssTileCache(r.etag, now, cells)) }
                    cells
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GnssZonesApiException) {
            when {
                e.status == 429 -> pausedUntil = now + RATE_LIMIT_PAUSE_MS
                e.status == 422 && e.code == "too_many_cells" -> return splitTile(bearer, t, now, key)
            }
            cached?.cells.orEmpty()
        } catch (e: Exception) {
            cached?.cells.orEmpty()
        }
    }

    /** Плитка дала слишком много ячеек: четыре четверти без ETag; результат кэшируется под ключом плитки. */
    private suspend fun splitTile(bearer: String, t: Tile, now: Long, key: String): List<GnssServerCell> {
        val b = t.bbox
        val midLat = (b.minLat + b.maxLat) / 2
        val midLon = (b.minLon + b.maxLon) / 2
        val parts = listOf(
            GnssBbox(b.minLat, b.minLon, midLat, midLon), GnssBbox(b.minLat, midLon, midLat, b.maxLon),
            GnssBbox(midLat, b.minLon, b.maxLat, midLon), GnssBbox(midLat, midLon, b.maxLat, b.maxLon),
        )
        val all = ArrayList<GnssServerCell>()
        for (p in parts) {
            val r = try {
                api.zones(bearer, p, null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return runCatching { store.load(key) }.getOrNull()?.cells.orEmpty()
            }
            if (r is GnssZonesFetch.Data) all += r.body.cells.mapNotNull(::toCell)
        }
        runCatching { store.save(key, GnssTileCache(null, now, all)) }
        return all
    }

    private fun toCell(d: GnssZoneCellDto): GnssServerCell? =
        if (d.level in 1..3 && d.iy in 0 until MAX_ROWS && d.ix >= 0) GnssServerCell(GnssCell(d.iy, d.ix), d.level, d.kinds) else null

    internal class Tile(val ty: Int, val tx: Int) {
        val key: String get() = "${ty}_$tx"
        val bbox: GnssBbox
            get() = GnssBbox(
                ty * TILE_DEG - 90.0, tx * TILE_DEG - 180.0,
                (ty + 1) * TILE_DEG - 90.0, (tx + 1) * TILE_DEG - 180.0,
            )
    }

    companion object {
        const val TILE_DEG = 0.5
        const val MAX_TILES = 4
        const val REFRESH_MS = 5L * 60_000
        const val RATE_LIMIT_PAUSE_MS = 60L * 60_000
        private const val MAX_ROWS = 80_000

        internal fun tilesOf(bbox: GnssBbox): List<Tile> {
            val lat0 = bbox.minLat.coerceIn(-90.0, 89.999)
            val lat1 = bbox.maxLat.coerceIn(-90.0, 89.999)
            val lon0 = bbox.minLon.coerceIn(-180.0, 179.999)
            val lon1 = bbox.maxLon.coerceIn(-180.0, 179.999)
            if (lat1 < lat0 || lon1 < lon0) return emptyList()
            val ty0 = floor((lat0 + 90.0) / TILE_DEG).toInt()
            val ty1 = floor((lat1 + 90.0) / TILE_DEG).toInt()
            val tx0 = floor((lon0 + 180.0) / TILE_DEG).toInt()
            val tx1 = floor((lon1 + 180.0) / TILE_DEG).toInt()
            val cy = (ty0 + ty1) / 2.0
            val cx = (tx0 + tx1) / 2.0
            val all = ArrayList<Tile>()
            for (y in ty0..ty1) for (x in tx0..tx1) all += Tile(y, x)
            return all.sortedBy { (it.ty - cy) * (it.ty - cy) + (it.tx - cx) * (it.tx - cx) }.take(MAX_TILES)
        }
    }
}
