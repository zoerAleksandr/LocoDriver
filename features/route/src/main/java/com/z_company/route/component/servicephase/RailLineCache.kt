package com.z_company.route.component.servicephase

import android.content.Context
import com.z_company.domain.util.PhaseLineSource
import java.io.File

/**
 * Кэш рельсовых линий плеч на устройстве (файлы по набору точек станций): пишет ServicePhaseAutoFillViewModel при
 * получении линии с сервера, читает карта (без сети) и [PhaseLineSource] для выбора названия станции остановки.
 */
class RailLineCache(private val context: Context) : PhaseLineSource {

    fun key(points: List<Pair<Double, Double>>): String =
        points.joinToString(";") { (lat, lon) -> "%.5f,%.5f".format(java.util.Locale.US, lat, lon) }

    fun file(key: String): File = File(context.noBackupFilesDir, "rail_lines/${key.hashCode().toUInt().toString(16)}.txt")

    fun read(file: File, key: String): List<Pair<Double, Double>>? = runCatching {
        if (!file.exists()) return null
        val (head, body) = file.readText().split("\n", limit = 2).takeIf { it.size == 2 } ?: return null
        if (head != key) return null
        body.split(";").mapNotNull { p ->
            val (lat, lon) = p.split(",").takeIf { it.size == 2 } ?: return@mapNotNull null
            lat.toDoubleOrNull()?.let { a -> lon.toDoubleOrNull()?.let { a to it } }
        }.takeIf { it.size >= 2 }
    }.getOrNull()

    override suspend fun line(stationPoints: List<Pair<Double, Double>>): List<Pair<Double, Double>>? {
        if (stationPoints.size < 2) return null
        val k = key(stationPoints)
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { read(file(k), k) }
    }
}
