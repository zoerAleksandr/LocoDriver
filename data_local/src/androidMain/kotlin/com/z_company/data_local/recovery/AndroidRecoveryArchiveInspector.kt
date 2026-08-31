package com.z_company.data_local.recovery

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/** Fully validates a published archive before it may be reused or uploaded. */
class AndroidRecoveryArchiveInspector {
    fun inspect(directory: File): RecoveryArchiveManifest {
        require(directory.isDirectory) { "Recovery archive directory does not exist" }
        val manifestFile = File(directory, MANIFEST_FILE)
        require(manifestFile.isFile) { "Recovery archive manifest does not exist" }
        require(manifestFile.length() <= RecoveryArchiveContract.MAX_MANIFEST_BYTES) {
            "Recovery archive manifest exceeds size limit"
        }
        val manifest = RecoveryArchiveJson.decodeManifest(manifestFile.readText())
        RecoveryArchiveValidator.validateManifest(manifest)
        require(manifest.sections.map { it.name }.toSet() == RecoveryArchiveContract.REQUIRED_SECTIONS) {
            "Recovery archive contains unsupported sections"
        }
        require(
            directory.listFiles()?.map { it.name }?.toSet() ==
                RecoveryArchiveContract.REQUIRED_SECTIONS + MANIFEST_FILE
        ) { "Recovery archive contains missing or unexpected files" }

        val actual = manifest.sections.associate { section ->
            val file = File(directory, section.name)
            require(file.isFile) { "Recovery archive section does not exist" }
            val itemCount = validateContent(section.name, file)
            section.name to RecoveryArchiveSectionDigest(itemCount, sha256(file))
        }
        RecoveryArchiveValidator.validatePayloadDigests(manifest, actual)
        return manifest
    }

    private fun validateContent(name: String, file: File): Long = when (name) {
        "routes.ndjson" -> validateRoutes(file)
        "settings.json" -> validateTable(file, RecoveryTableSectionKind.SETTINGS)
        "salary-settings.json" -> validateTable(file, RecoveryTableSectionKind.SALARY_SETTINGS)
        "norms.json" -> validateTable(file, RecoveryTableSectionKind.NORMS)
        "attachments-manifest.json" -> validateAttachments(file)
        else -> error("Unsupported recovery archive section")
    }

    private fun validateRoutes(file: File): Long {
        require(file.length() <= MAX_ROUTES_SECTION_BYTES) { "Recovery routes section too large" }
        var count = 0L
        val routeIds = mutableSetOf<String>()
        readBoundedLines(file) { line ->
            if (line.isBlank()) return@readBoundedLines
            val record = RecoveryRouteRecordValidator.decodeAndValidate(line)
            require(routeIds.add(record.routeId)) { "Duplicate recovery route ID" }
            count++
            require(count <= MAX_ROUTES) { "Too many recovery routes" }
        }
        return count
    }

    private fun validateTable(file: File, kind: RecoveryTableSectionKind): Long {
        require(file.length() <= RecoveryTableSectionJson.MAX_SECTION_BYTES) {
            "Recovery table section exceeds size limit"
        }
        return RecoveryTableSectionJson.decodeAndValidate(file.readText(), kind)
            .tables.values.sumOf { it.size.toLong() }
    }

    private fun validateAttachments(file: File): Long {
        require(file.length() <= RecoveryAttachmentsManifestJson.MAX_SECTION_BYTES) {
            "Recovery attachments section exceeds size limit"
        }
        return RecoveryAttachmentsManifestJson.decodeAndValidate(file.readText())
            .let { it.attachments.size.toLong() + it.embeddedAttachments.size.toLong() }
    }

    private fun readBoundedLines(file: File, consume: (String) -> Unit) {
        BufferedInputStream(FileInputStream(file)).use { input ->
            val line = ByteArrayOutputStream()
            while (true) {
                val byte = input.read()
                if (byte < 0) {
                    if (line.size() > 0) consume(line.toByteArray().decodeToString())
                    return
                }
                if (byte == '\n'.code) {
                    val bytes = line.toByteArray()
                    val length = if (bytes.lastOrNull() == '\r'.code.toByte()) bytes.size - 1 else bytes.size
                    consume(bytes.decodeToString(endIndex = length))
                    line.reset()
                } else {
                    require(line.size() < RecoveryRouteRecordJson.MAX_LINE_BYTES) {
                        "Recovery route line exceeds size limit"
                    }
                    line.write(byte)
                }
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    private companion object {
        const val MANIFEST_FILE = "manifest.json"
        const val MAX_ROUTES = 200_000L
        const val MAX_ROUTES_SECTION_BYTES = 512L * 1024L * 1024L
    }
}
