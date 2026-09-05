package com.z_company.data_local.recovery

import android.system.Os
import android.system.OsConstants
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class AndroidRecoveryArchiveBundle {
    fun pack(archiveDirectory: File, destination: File): File {
        AndroidRecoveryArchiveInspector().inspect(archiveDirectory)
        require(!destination.exists()) { "Recovery bundle destination already exists" }
        val parent = requireNotNull(destination.parentFile)
        require(parent.isDirectory || parent.mkdirs()) { "Cannot create recovery bundle directory" }
        val temporary = File(parent, destination.name + ".tmp")
        temporary.delete()
        try {
            FileOutputStream(temporary).use { fileOutput ->
                val output = DataOutputStream(BufferedOutputStream(fileOutput))
                output.write(MAGIC)
                output.writeInt(FILE_NAMES.size)
                FILE_NAMES.forEach { name ->
                    val file = File(archiveDirectory, name)
                    val nameBytes = name.encodeToByteArray()
                    output.writeInt(nameBytes.size)
                    output.write(nameBytes)
                    output.writeLong(file.length())
                    FileInputStream(file).use { input -> input.copyTo(output) }
                }
                output.flush()
                fileOutput.fd.sync()
            }
            require(temporary.length() <= MAX_BUNDLE_BYTES) { "Recovery bundle exceeds size limit" }
            Os.rename(temporary.path, destination.path)
            syncDirectory(parent)
            return destination
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    fun unpack(bundle: File, destinationDirectory: File): File {
        require(bundle.isFile && bundle.length() in 1..MAX_BUNDLE_BYTES) {
            "Recovery bundle is missing or too large"
        }
        require(!destinationDirectory.exists()) { "Recovery archive destination already exists" }
        val parent = requireNotNull(destinationDirectory.parentFile)
        require(parent.isDirectory || parent.mkdirs()) { "Cannot create recovery archive parent" }
        val staging = File(parent, destinationDirectory.name + ".building")
        require(!staging.exists() && staging.mkdir()) { "Cannot create recovery archive staging" }
        try {
            DataInputStream(BufferedInputStream(FileInputStream(bundle))).use { input ->
                val magic = ByteArray(MAGIC.size).also(input::readFully)
                require(magic.contentEquals(MAGIC)) { "Unsupported recovery bundle format" }
                require(input.readInt() == FILE_NAMES.size) { "Invalid recovery bundle file count" }
                val seen = mutableSetOf<String>()
                repeat(FILE_NAMES.size) {
                    val nameLength = input.readInt()
                    require(nameLength in 1..64) { "Invalid recovery bundle file name" }
                    val name = ByteArray(nameLength).also(input::readFully).decodeToString()
                    require(name in FILE_NAMES && seen.add(name)) { "Invalid recovery bundle file" }
                    val size = input.readLong()
                    require(size in 0..MAX_ENTRY_BYTES) { "Recovery bundle entry too large" }
                    writeExactly(input, File(staging, name), size)
                }
                require(input.read() == -1) { "Recovery bundle has trailing content" }
                require(seen == FILE_NAMES) { "Recovery bundle is incomplete" }
            }
            AndroidRecoveryArchiveInspector().inspect(staging)
            syncDirectory(staging)
            Os.rename(staging.path, destinationDirectory.path)
            syncDirectory(parent)
            return destinationDirectory
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }

    fun sha256(file: File): String {
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

    private fun writeExactly(input: DataInputStream, destination: File, size: Long) {
        var remaining = size
        FileOutputStream(destination).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (remaining > 0) {
                val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                require(count > 0) { "Truncated recovery bundle" }
                output.write(buffer, 0, count)
                remaining -= count
            }
            output.fd.sync()
        }
    }

    private fun syncDirectory(directory: File) {
        val descriptor = Os.open(directory.path, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

    private companion object {
        val MAGIC = "LOCO-RECOVERY-1\n".encodeToByteArray()
        val FILE_NAMES = setOf(
            "manifest.json",
            "routes.ndjson",
            "settings.json",
            "salary-settings.json",
            "norms.json",
            "attachments-manifest.json",
        )
        const val MAX_ENTRY_BYTES = 256L * 1024L * 1024L
        const val MAX_BUNDLE_BYTES = 256L * 1024L * 1024L
    }
}
