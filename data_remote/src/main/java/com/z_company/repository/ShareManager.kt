package com.z_company.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.z_company.core.util.ConverterLongToTime
import com.z_company.domain.entities.route.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Enum для кастомных MIME-типов
enum class CustomMimeType(val type: String) {
    JSON("application/json"),
    CUSTOM_DATA("application/route")
}

class ShareManager(val context: Context) {
    inline fun <reified T> pojoToFile(
        pojo: T,
        fileName: String? = null,
        mimeType: CustomMimeType = CustomMimeType.CUSTOM_DATA
    ): File {
        // Генерация уникального имени файла, если не передано
        val finalFileName =
            fileName ?: generateUniqueFileName(pojo!!::class.java.simpleName, mimeType)

        // Создание файла
        val file = File(context.getExternalFilesDir(null), finalFileName)

        // Конвертация в зависимости от MIME-типа
        when (mimeType) {
            CustomMimeType.JSON -> saveAsJson(pojo, file)
            CustomMimeType.CUSTOM_DATA -> saveAsCustomFormat(pojo, file)
        }

        return file
    }

    fun generateUniqueFileName(
        prefix: String,
        mimeType: CustomMimeType
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val extension = when (mimeType) {
            CustomMimeType.JSON -> ".json"
            CustomMimeType.CUSTOM_DATA -> ".route"
        }
        return "${prefix}_$timestamp$extension"
    }

    // Кастомный формат сериализации
    fun <T> saveAsCustomFormat(pojo: T, file: File) {
        FileWriter(file).use { writer ->
            // Пример кастомной сериализации
            writer.append("CUSTOM_DATA_HEADER\n")

            val fields = pojo!!::class.java.declaredFields
            fields.forEach { field ->
                field.isAccessible = true
                writer.append("${field.name}:${field.get(pojo)}\n")
            }
        }
    }

    // Сохранение как JSON
    fun <T> saveAsJson(pojo: T, file: File) {
        val gson = Gson()
        FileWriter(file).use { writer ->
            gson.toJson(pojo, writer)
        }
    }

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    inline fun <reified T> createShareIntent(
        pojo: T,
        mimeType: CustomMimeType = CustomMimeType.CUSTOM_DATA
    ): Intent {
        val file = pojoToFile(pojo = pojo)
        val uri = getUriForFile(file)
        Log.d("ZZZ", "File created: ${file.absolutePath}")
        Log.d("ZZZ", "URI: $uri")
        Log.d("ZZZ", "File exists: ${file.exists()}")

        return Intent().apply {
            action = Intent.ACTION_SEND
            type = mimeType.type
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    inline fun <reified T> fileToPojo(
        file: File,
        mimeType: CustomMimeType = CustomMimeType.CUSTOM_DATA
    ): T? {
        return when (mimeType) {
            CustomMimeType.JSON -> deserializeJson<T>(file)
            CustomMimeType.CUSTOM_DATA -> deserializeCustomFormat<T>(file)
        }
    }

    inline fun <reified T> deserializeJson(file: File): T? {
        return try {
            val gson = Gson()
            file.reader().use {
                gson.fromJson(it, T::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    inline fun <reified T> deserializeCustomFormat(file: File): T? {
        return try {
            // Логика десериализации кастомного формата
            val lines = file.readLines().drop(1) // Пропускаем заголовок
            val obj = T::class.java.getDeclaredConstructor().newInstance()

            lines.forEach { line ->
                val (fieldName, value) = line.split(":")
                val field = T::class.java.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(obj, value)
            }

            obj
        } catch (e: Exception) {
            null
        }
    }

    // Метод для множественной передачи файлов
    fun createMultiShareIntent(files: List<File>, mimeType: String): Intent {
        val uris = files.map { getUriForFile(it) }

        return Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

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