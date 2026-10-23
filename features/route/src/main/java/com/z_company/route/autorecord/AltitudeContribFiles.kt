package com.z_company.route.autorecord

import com.z_company.domain.track.AltitudeContribStore
import com.z_company.domain.track.AltitudeContribText
import com.z_company.domain.track.AxisCache
import com.z_company.domain.track.PendingContribSession
import com.z_company.domain.track.SectionAxis
import java.io.File

/**
 * Временные файлы точек вклада высот (согласие №2). Не Track.db и не зависит от согласия №1: файл живёт только до
 * успешной отправки (или до отзыва согласия / недели). Координаты не логируются.
 */
class FileAltitudeContribStore(private val dir: File) : AltitudeContribStore {
    private fun file(id: String) = File(dir, "$id.pts")

    override fun save(session: PendingContribSession) {
        if (session.samples.isEmpty() || !isSafeId(session.id)) return
        dir.mkdirs()
        val tmp = File(dir, "${session.id}.tmp")
        tmp.writeText(AltitudeContribText.encodeSession(session))
        if (!tmp.renameTo(file(session.id))) tmp.delete()
    }

    override fun pendingIds(): List<String> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".pts") }?.map { it.name.removeSuffix(".pts") }.orEmpty()

    override fun load(id: String): PendingContribSession? {
        if (!isSafeId(id)) return null
        val f = file(id)
        if (!f.isFile) return null
        return runCatching { AltitudeContribText.decodeSession(f.readText()) }.getOrNull()
    }

    override fun delete(id: String) {
        if (isSafeId(id)) file(id).delete()
    }

    override fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun isSafeId(id: String) = id.isNotEmpty() && id.all { it.isLetterOrDigit() || it == '-' }
}

/** Кэш осей участков советника на диске: файл на участок, актуален по `axisHash`. */
class FileAxisCache(private val dir: File) : AxisCache {
    private fun file(sectionId: String) = File(dir, "$sectionId.axis")

    private fun isSafeId(id: String) = id.isNotEmpty() && id.all { it.isLetterOrDigit() || it == '-' }

    override fun load(sectionId: String): SectionAxis? {
        if (!isSafeId(sectionId)) return null
        val f = file(sectionId)
        if (!f.isFile) return null
        return runCatching { AltitudeContribText.decodeAxis(f.readText()) }.getOrNull()
    }

    override fun save(axis: SectionAxis) {
        if (!isSafeId(axis.sectionId)) return
        dir.mkdirs()
        val tmp = File(dir, "${axis.sectionId}.tmp")
        tmp.writeText(AltitudeContribText.encodeAxis(axis))
        if (!tmp.renameTo(file(axis.sectionId))) tmp.delete()
    }

    override fun retain(keepIds: Set<String>) {
        dir.listFiles { f -> f.name.endsWith(".axis") }?.forEach {
            if (it.name.removeSuffix(".axis") !in keepIds) it.delete()
        }
    }
}

/** Кэш профилей участков на диске: файл `<id>.json` на участок и `index.json` со списком. Пишется через временный файл. */
class FileSectionProfileCache(private val dir: File) : com.z_company.repository.remote_rest.tracks.SectionProfileCache {
    private fun isSafeId(id: String) = id.isNotEmpty() && id.all { it.isLetterOrDigit() || it == '-' }

    private fun write(target: File, text: String) {
        dir.mkdirs()
        val tmp = File(dir, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) tmp.delete()
    }

    override fun loadIndex(): String? = File(dir, "index.json").takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }

    override fun saveIndex(text: String) = write(File(dir, "index.json"), text)

    override fun load(sectionId: String): String? {
        if (!isSafeId(sectionId)) return null
        return File(dir, "$sectionId.json").takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }
    }

    override fun save(sectionId: String, text: String) {
        if (isSafeId(sectionId)) write(File(dir, "$sectionId.json"), text)
    }

    override fun retain(keepIds: Set<String>) {
        dir.listFiles { f -> f.name.endsWith(".json") && f.name != "index.json" }?.forEach {
            if (it.name.removeSuffix(".json") !in keepIds) it.delete()
        }
    }
}
