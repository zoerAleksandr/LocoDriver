package com.z_company.repository.remote_rest.recovery

import com.z_company.repository.remote_rest.RemoteRestClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

interface RecoveryCloudClient {
    fun latest(token: String): RecoverySnapshotResponse
    fun download(token: String, snapshot: RecoverySnapshotResponse, destination: File): File
}

class AndroidRecoveryCloudClient(
    private val baseUrl: String = RECOVERY_API_BASE_URL,
) : RecoveryCloudClient {
    init {
        require(baseUrl.startsWith("https://")) { "Recovery API requires HTTPS" }
    }

    fun create(token: String, request: RecoverySnapshotCreateRequest): RecoverySnapshotResponse {
        RecoverySnapshotContractValidator.validate(request)
        val body = RemoteRestClient.appJson.encodeToString(request).encodeToByteArray()
        return jsonRequest("POST", "recovery/snapshots", token, body)
    }

    fun upload(
        token: String,
        snapshot: RecoverySnapshotResponse,
        bundle: File,
    ): RecoverySnapshotResponse {
        RecoverySnapshotContractValidator.validate(snapshot)
        require(bundle.isFile && bundle.length() == snapshot.sizeBytes)
        require(sha256(bundle).equals(snapshot.sha256, ignoreCase = true))
        val connection = connection("recovery/snapshots/${snapshot.snapshotId}/content", token)
        try {
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setFixedLengthStreamingMode(bundle.length())
            FileInputStream(bundle).use { input ->
                connection.outputStream.use { output -> input.copyTo(output, NETWORK_BUFFER_BYTES) }
            }
            return parseJsonResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    override fun latest(token: String): RecoverySnapshotResponse =
        jsonRequest("GET", "recovery/snapshots/latest", token, null)

    override fun download(
        token: String,
        snapshot: RecoverySnapshotResponse,
        destination: File,
    ): File {
        RecoverySnapshotContractValidator.validate(snapshot)
        require(snapshot.status == "READY")
        require(!destination.exists())
        val parent = requireNotNull(destination.parentFile)
        require(parent.isDirectory || parent.mkdirs())
        val temporary = File(parent, destination.name + ".tmp")
        temporary.delete()
        val connection = connection("recovery/snapshots/${snapshot.snapshotId}/download", token)
        try {
            connection.requestMethod = "GET"
            requireSuccess(connection)
            val declared = connection.contentLengthLong
            require(declared == -1L || declared == snapshot.sizeBytes)
            var received = 0L
            val digest = MessageDigest.getInstance("SHA-256")
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(NETWORK_BUFFER_BYTES)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        require(received <= snapshot.sizeBytes)
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            require(received == snapshot.sizeBytes)
            require(digest.digest().toHex().equals(snapshot.sha256, ignoreCase = true))
            require(temporary.renameTo(destination))
            return destination
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private inline fun <reified T> jsonRequest(
        method: String,
        path: String,
        token: String,
        body: ByteArray?,
    ): T {
        val connection = connection(path, token)
        try {
            connection.requestMethod = method
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            return parseJsonResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private inline fun <reified T> parseJsonResponse(connection: HttpURLConnection): T {
        requireSuccess(connection)
        val text = connection.inputStream.bufferedReader().use { reader ->
            val value = reader.readText()
            require(value.encodeToByteArray().size <= MAX_JSON_RESPONSE_BYTES)
            value
        }
        return RemoteRestClient.appJson.decodeFromString(text)
    }

    private fun requireSuccess(connection: HttpURLConnection) {
        val status = connection.responseCode
        if (status !in 200..299) {
            connection.errorStream?.use { input ->
                val buffer = ByteArray(MAX_ERROR_RESPONSE_BYTES)
                input.read(buffer)
            }
            throw RecoveryCloudHttpException(status)
        }
    }

    private fun connection(path: String, token: String): HttpsURLConnection {
        require(token.isNotBlank() && token.length <= MAX_TOKEN_LENGTH)
        val url = URL(baseUrl.trimEnd('/') + "/" + path.trimStart('/'))
        return (url.openConnection() as HttpsURLConnection).apply {
            connectTimeout = 25_000
            readTimeout = 120_000
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("Authorization", token)
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(NETWORK_BUFFER_BYTES)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }

    private companion object {
        const val NETWORK_BUFFER_BYTES = 64 * 1024
        const val MAX_JSON_RESPONSE_BYTES = 64 * 1024
        const val MAX_ERROR_RESPONSE_BYTES = 8 * 1024
        const val MAX_TOKEN_LENGTH = 8 * 1024
    }
}

class RecoveryCloudHttpException(val statusCode: Int) : IllegalStateException("Recovery HTTP $statusCode")
