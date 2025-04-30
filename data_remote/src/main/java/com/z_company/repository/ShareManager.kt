package com.z_company.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.route.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream

class ShareManager(private val context: Context) {
    fun getUri(file: Route): Uri {
        val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
        val savedFile = saveFile(
            name = "Маршрут от ${ConverterLongToTime.getDateAndTimeStringFormat(file.basicData.timeStartWork)}",
            data = gson.toJson(file).toByteArray()
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            savedFile
        )
    }

    suspend fun shareFile(file: Route): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
                val savedFile = saveFile(
                    name = file.basicData.number.toString(),
                    gson.toJson(file).toByteArray()
                )
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    savedFile
                )
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        flags += Intent.FLAG_ACTIVITY_NEW_TASK
                        flags += Intent.FLAG_GRANT_READ_URI_PERMISSION
                        type = ("text/*")
                    }
                    val chooser = Intent.createChooser(intent, "Отправить маршрут через")
                    context.startActivity(chooser)
                }
            }
        }
    }

    fun saveFile(name: String, data: ByteArray): File {
        val cache = context.cacheDir
        val savedFile = File(cache, name)
        savedFile.writeBytes(data)
        return savedFile
    }

    fun shareSerializableObject(
        serializableObject: java.io.Serializable,
    ): Uri {
        try {
            val cacheDir = File(context.cacheDir, "shared_objects")
            cacheDir.mkdirs()
            val file = File(cacheDir, "Маршрут")
            FileOutputStream(file).use { fileOutputStream ->
                ObjectOutputStream(fileOutputStream).use { objectOutputStream ->
                    objectOutputStream.writeObject(serializableObject)
                }
            }
            return FileProvider.getUriForFile(
                context.applicationContext,
                "com.z_company.loco_driver.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("ShareObject", "Error sharing object", e)
            throw e
        }
    }
}