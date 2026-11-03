package com.z_company.route.autorecord

import android.content.SharedPreferences
import com.z_company.domain.gnss.GnssContribSession
import com.z_company.domain.gnss.GnssContribStore
import com.z_company.domain.gnss.GnssContribText
import com.z_company.domain.gnss.GnssTagStorage
import com.z_company.domain.gnss.GnssTileCache
import com.z_company.domain.gnss.GnssTileText
import com.z_company.repository.remote_rest.tracks.GnssTileStore
import java.io.File

/**
 * Временные файлы вклада «места без GPS» (согласие №2 с GPS). Не Track.db и не зависит от согласия №1:
 * файл живёт только до успешной отправки (или до отзыва согласия / 6 суток). Ячейки не логируются.
 */
class FileGnssContribStore(private val dir: File) : GnssContribStore {
    private fun file(id: String) = File(dir, "$id.cells")

    override fun save(session: GnssContribSession) {
        if (session.cells.isEmpty() || !isSafeId(session.id)) return
        dir.mkdirs()
        val tmp = File(dir, "${session.id}.tmp")
        tmp.writeText(GnssContribText.encode(session))
        if (!tmp.renameTo(file(session.id))) tmp.delete()
    }

    override fun pendingIds(): List<String> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".cells") }?.map { it.name.removeSuffix(".cells") }.orEmpty()

    override fun load(id: String): GnssContribSession? {
        if (!isSafeId(id)) return null
        val f = file(id)
        if (!f.isFile) return null
        return runCatching { GnssContribText.decode(f.readText()) }.getOrNull()
    }

    override fun delete(id: String) {
        if (isSafeId(id)) file(id).delete()
    }

    override fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }
}

/** Кэш серверных ячеек по плиткам 0,5° в cacheDir: система может его очистить, ничего важного там нет. */
class FileGnssTileStore(private val dir: File) : GnssTileStore {
    override fun load(key: String): GnssTileCache? {
        if (!isSafeId(key)) return null
        val f = File(dir, "$key.tile")
        if (!f.isFile) return null
        return runCatching { GnssTileText.decode(f.readText()) }.getOrNull()
    }

    override fun save(key: String, tile: GnssTileCache) {
        if (!isSafeId(key)) return
        dir.mkdirs()
        val tmp = File(dir, "$key.tmp")
        tmp.writeText(GnssTileText.encode(tile))
        if (!tmp.renameTo(File(dir, "$key.tile"))) tmp.delete()
    }

    override fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }
}

/** Случайный tag вклада в SharedPreferences (отдельный файл, не связан с аккаунтом). */
class PrefsGnssTagStorage(private val prefs: SharedPreferences) : GnssTagStorage {
    override fun tag(): String? = prefs.getString(KEY_TAG, null)
    override fun createdAtMs(): Long = prefs.getLong(KEY_AT, 0L)
    override fun save(tag: String, createdAtMs: Long) {
        prefs.edit().putString(KEY_TAG, tag).putLong(KEY_AT, createdAtMs).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_TAG).remove(KEY_AT).apply()
    }

    companion object {
        const val PREFS_NAME = "gnss_contrib_tag"
        private const val KEY_TAG = "tag"
        private const val KEY_AT = "created_at_ms"
    }
}

/** Допустимые имена файлов: буквы, цифры, `-` и `_` (без путей). */
private fun isSafeId(id: String) = id.isNotEmpty() && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }
