package com.z_company.data_local.recovery

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RecoveryRawSnapshotManifestV1(
    val formatVersion: Int,
    val files: List<RecoveryRawSnapshotFileV1>,
)

@Serializable
data class RecoveryRawSnapshotFileV1(
    val name: String,
    val sizeBytes: Long,
    val sha256: String,
    val databaseVersion: Int,
)

object RecoveryRawSnapshotManifestJson {
    const val FORMAT_VERSION: Int = 1
    const val MAX_BYTES: Int = 16 * 1024

    private val allowedNames = setOf(
        "Route.snapshot.db",
        "Settings.snapshot.db",
        "Salary.snapshot.db",
    )
    private val sha256 = Regex("[a-fA-F0-9]{64}")
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(manifest: RecoveryRawSnapshotManifestV1): String =
        json.encodeToString(RecoveryRawSnapshotManifestV1.serializer(), manifest)

    fun decodeAndValidate(content: String): RecoveryRawSnapshotManifestV1 {
        require(content.encodeToByteArray().size <= MAX_BYTES) { "Raw snapshot manifest too large" }
        val manifest = json.decodeFromString(RecoveryRawSnapshotManifestV1.serializer(), content)
        require(manifest.formatVersion == FORMAT_VERSION) { "Unsupported raw snapshot format" }
        require(manifest.files.size in 1..allowedNames.size) { "Invalid raw snapshot file count" }
        val names = mutableSetOf<String>()
        manifest.files.forEach { file ->
            require(file.name in allowedNames && names.add(file.name)) {
                "Invalid raw snapshot file name"
            }
            require(file.sizeBytes > 0L && file.databaseVersion > 0 && sha256.matches(file.sha256)) {
                "Invalid raw snapshot metadata"
            }
        }
        return manifest
    }
}
