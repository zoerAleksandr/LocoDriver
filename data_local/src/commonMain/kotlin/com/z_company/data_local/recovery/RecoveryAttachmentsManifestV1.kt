package com.z_company.data_local.recovery

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RecoveryAttachmentsManifestV1(
    val sectionFormatVersion: Int,
    val attachments: List<RecoveryRemoteAttachmentV1>,
    val embeddedAttachments: List<RecoveryEmbeddedAttachmentV1> = emptyList(),
)

@Serializable
data class RecoveryRemoteAttachmentV1(
    val photoId: String,
    val routeId: String,
    val remoteObjectId: String? = null,
    val url: String,
    val createdAt: Long,
)

@Serializable
data class RecoveryEmbeddedAttachmentV1(
    val photoId: String,
    val routeId: String,
    val encoding: String,
    val sizeBytes: Long,
    val sha256: String,
    val createdAt: Long,
)

enum class RecoveryAttachmentsValidationCode {
    SECTION_TOO_LARGE,
    UNSUPPORTED_FORMAT,
    TOO_MANY_ATTACHMENTS,
    INVALID_ID,
    DUPLICATE_PHOTO_ID,
    UNSAFE_REMOTE_URL,
    INVALID_EMBEDDED_METADATA,
}

class RecoveryAttachmentsValidationException(
    val code: RecoveryAttachmentsValidationCode,
) : IllegalArgumentException(code.name)

object RecoveryAttachmentsManifestJson {
    const val CURRENT_FORMAT_VERSION: Int = 1
    const val MAX_SECTION_BYTES: Int = 4 * 1024 * 1024
    const val MAX_ATTACHMENTS: Int = 20_000

    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    fun encode(manifest: RecoveryAttachmentsManifestV1): String =
        json.encodeToString(RecoveryAttachmentsManifestV1.serializer(), manifest)

    fun decodeAndValidate(content: String): RecoveryAttachmentsManifestV1 {
        if (content.encodeToByteArray().size > MAX_SECTION_BYTES) fail(
            RecoveryAttachmentsValidationCode.SECTION_TOO_LARGE
        )
        val manifest = json.decodeFromString(
            RecoveryAttachmentsManifestV1.serializer(),
            content,
        )
        if (manifest.sectionFormatVersion != CURRENT_FORMAT_VERSION) fail(
            RecoveryAttachmentsValidationCode.UNSUPPORTED_FORMAT
        )
        if (manifest.attachments.size + manifest.embeddedAttachments.size > MAX_ATTACHMENTS) fail(
            RecoveryAttachmentsValidationCode.TOO_MANY_ATTACHMENTS
        )
        val ids = mutableSetOf<String>()
        manifest.attachments.forEach { attachment ->
            if (!ID.matches(attachment.photoId) || !ID.matches(attachment.routeId)) fail(
                RecoveryAttachmentsValidationCode.INVALID_ID
            )
            if (!ids.add(attachment.photoId)) fail(
                RecoveryAttachmentsValidationCode.DUPLICATE_PHOTO_ID
            )
            if (!isSafeRemoteUrl(attachment.url)) fail(
                RecoveryAttachmentsValidationCode.UNSAFE_REMOTE_URL
            )
        }
        manifest.embeddedAttachments.forEach { attachment ->
            if (!ID.matches(attachment.photoId) || !ID.matches(attachment.routeId)) fail(
                RecoveryAttachmentsValidationCode.INVALID_ID
            )
            if (!ids.add(attachment.photoId)) fail(
                RecoveryAttachmentsValidationCode.DUPLICATE_PHOTO_ID
            )
            if (
                attachment.encoding != "base64" ||
                attachment.sizeBytes !in 1..MAX_EMBEDDED_ATTACHMENT_BYTES ||
                !SHA256.matches(attachment.sha256)
            ) fail(RecoveryAttachmentsValidationCode.INVALID_EMBEDDED_METADATA)
        }
        return manifest
    }

    fun isSafeRemoteUrl(url: String): Boolean {
        if (url.length !in 1..2_048 || url.any { it.isWhitespace() || it.isISOControl() }) {
            return false
        }
        val separator = url.indexOf("://")
        if (separator <= 0) return false
        val scheme = url.substring(0, separator).lowercase()
        val remainder = url.substring(separator + 3)
        return scheme in setOf("http", "https") && remainder.isNotBlank() &&
            !remainder.substringBefore('/').contains('@')
    }

    private fun fail(code: RecoveryAttachmentsValidationCode): Nothing =
        throw RecoveryAttachmentsValidationException(code)

    private val ID = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
    private val SHA256 = Regex("[a-fA-F0-9]{64}")
    const val MAX_EMBEDDED_ATTACHMENT_BYTES: Long = 20L * 1024L * 1024L
}

class LocalRecoveryAttachmentRequiresContentException(val photoId: String) :
    IllegalStateException("Local recovery attachment bytes are not packaged")
