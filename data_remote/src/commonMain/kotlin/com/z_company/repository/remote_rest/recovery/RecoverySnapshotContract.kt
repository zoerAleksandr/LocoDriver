package com.z_company.repository.remote_rest.recovery

import kotlinx.serialization.Serializable

const val RECOVERY_API_BASE_URL = "https://api.locodriver.ru/v1/"

@Serializable
data class RecoverySnapshotCreateRequest(
    val idempotencyKey: String,
    val archiveFormatVersion: Int,
    val sourceDbVersion: Int,
    val appBuild: Int,
    val installationId: String,
    val sizeBytes: Long,
    val sha256: String,
)

@Serializable
data class RecoverySnapshotResponse(
    val snapshotId: String,
    val status: String,
    val archiveFormatVersion: Int,
    val sourceDbVersion: Int,
    val appBuild: Int,
    val sizeBytes: Long,
    val sha256: String,
    val createdAt: Long,
    val completedAt: Long? = null,
)

object RecoverySnapshotContractValidator {
    const val MAX_ARCHIVE_BYTES: Long = 256L * 1024L * 1024L
    private val sha256 = Regex("[a-fA-F0-9]{64}")
    private val id = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")

    fun validate(request: RecoverySnapshotCreateRequest) {
        require(request.idempotencyKey.length in 1..128)
        require(request.archiveFormatVersion in 1..16)
        require(request.sourceDbVersion in 1..10_000)
        require(request.appBuild in 1..10_000_000)
        require(id.matches(request.installationId))
        require(request.sizeBytes in 1..MAX_ARCHIVE_BYTES)
        require(sha256.matches(request.sha256))
    }

    fun validate(response: RecoverySnapshotResponse) {
        require(response.snapshotId.length in 1..128)
        require(response.status in setOf("UPLOADING", "READY", "FAILED"))
        require(response.archiveFormatVersion in 1..16)
        require(response.sourceDbVersion in 1..10_000)
        require(response.appBuild in 1..10_000_000)
        require(response.sizeBytes in 1..MAX_ARCHIVE_BYTES)
        require(sha256.matches(response.sha256))
        require(response.createdAt > 0L)
    }
}
