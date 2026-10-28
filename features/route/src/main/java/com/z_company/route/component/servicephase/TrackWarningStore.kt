package com.z_company.route.component.servicephase

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.z_company.domain.track.TrackWarning
import com.z_company.domain.track.WarningType
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Предупреждения по участкам: хранятся на устройстве (SharedPreferences, JSON по ключу «участок + направление + плечо») и на сервер не уходят. Список
 * сохранённых живёт между поездками - при повторном проследовании участка их предлагается применить снова.
 */
internal class TrackWarningStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("track_warnings", Context.MODE_PRIVATE)

    /** [key] - ключ хранения ([ProfileModel.warningsKey]: участок + направление + плечо), [sectionId] - id участка в самих записях. */
    fun load(key: String, sectionId: String): List<TrackWarning> = runCatching {
        val raw = prefs.getString(key, null) ?: return emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).map { fromJson(sectionId, arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    /** История участка: предупреждения всех ключей (плечо/направление) участка без повторов. */
    fun loadAll(sectionId: String): List<TrackWarning> = runCatching {
        prefs.all.keys.filter { it == sectionId || it.startsWith("$sectionId|") }.flatMap { load(it, sectionId) }.distinctBy { it.id }
    }.getOrDefault(emptyList())

    fun save(key: String, list: List<TrackWarning>) {
        runCatching {
            if (list.isEmpty()) prefs.edit().remove(key).apply()
            else prefs.edit().putString(key, JSONArray(list.map { toJson(it) }).toString()).apply()
        }
    }

    /** Заменить предупреждение [w] во всех ключах его участка (плечо/направление) - правка из «Ограничений поезда». */
    fun updateEverywhere(w: TrackWarning) = mutateEverywhere(w.sectionId) { list -> list.map { if (it.id == w.id) w else it } }

    /** Убрать предупреждение [id] участка [sectionId] из всех ключей. */
    fun removeEverywhere(sectionId: String, id: String) = mutateEverywhere(sectionId) { list -> list.filter { it.id != id } }

    private fun mutateEverywhere(sectionId: String, change: (List<TrackWarning>) -> List<TrackWarning>) {
        runCatching {
            prefs.all.keys.filter { it == sectionId || it.startsWith("$sectionId|") }.forEach { key ->
                val before = load(key, sectionId)
                val after = change(before)
                if (after != before) save(key, after)
            }
        }
    }

    private fun toJson(w: TrackWarning) = JSONObject().apply {
        put("id", w.id)
        w.startAbsKm?.let { put("sa", it) }
        w.endAbsKm?.let { put("ea", it) }
        put("sr", w.startRelKm)
        put("er", w.endRelKm)
        put("t", w.type.name)
        w.speedKmh?.let { put("v", it) }
        put("n", w.note)
        put("p", w.place)
    }

    private fun fromJson(sectionId: String, o: JSONObject) = TrackWarning(
        id = o.getString("id"),
        sectionId = sectionId,
        startAbsKm = if (o.has("sa")) o.getDouble("sa") else null,
        endAbsKm = if (o.has("ea")) o.getDouble("ea") else null,
        startRelKm = o.getDouble("sr"),
        endRelKm = o.getDouble("er"),
        type = runCatching { WarningType.valueOf(o.getString("t")) }.getOrDefault(WarningType.OTHER),
        speedKmh = if (o.has("v")) o.getInt("v") else null,
        note = o.optString("n"),
        place = o.optString("p"),
    )
}

/** Настройки голосовых предупреждений: включено, за сколько метров, как часто повторять, голос и скорость речи. */
internal class WarningVoiceSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("warning_voice", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) { prefs.edit().putBoolean("enabled", v).apply() }

    /** За сколько метров до начала зоны начать озвучку. */
    var distanceM: Int
        get() = prefs.getInt("distance", 1500)
        set(v) { prefs.edit().putInt("distance", v).apply() }

    /** Через сколько секунд повторять; 0 - один раз. */
    var repeatSec: Int
        get() = prefs.getInt("repeat", 30)
        set(v) { prefs.edit().putInt("repeat", v).apply() }

    /** Имя голоса TTS; пусто - голос по умолчанию для русского. */
    var voiceName: String
        get() = prefs.getString("voice", "") ?: ""
        set(v) { prefs.edit().putString("voice", v).apply() }

    var rate: Float
        get() = prefs.getFloat("rate", 1.0f)
        set(v) { prefs.edit().putFloat("rate", v).apply() }
}

/** Озвучка через системный TextToSpeech (русский). Освобождается через [shutdown]. */
internal class WarningVoice(context: Context, private val settings: WarningVoiceSettings) {
    private var ready = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                apply()
            }
        }
    }

    /** Русские голоса устройства для выбора в настройках. */
    fun voices(): List<Voice> = runCatching {
        tts?.voices.orEmpty().filter { it.locale.language == "ru" && !it.isNetworkConnectionRequired }.sortedBy { it.name }
    }.getOrDefault(emptyList())

    /** Применить текущие настройки (язык, голос, скорость). */
    fun apply() {
        val t = tts ?: return
        if (!ready) return
        runCatching {
            t.language = Locale("ru", "RU")
            settings.voiceName.takeIf { it.isNotBlank() }?.let { name -> t.voices?.firstOrNull { it.name == name }?.let { t.voice = it } }
            t.setSpeechRate(settings.rate)
        }
    }

    /** [append] - не обрывать то, что говорится сейчас (заход/выход из зоны не должны перебивать друг друга). */
    fun speak(text: String, append: Boolean = false) {
        if (!ready || !settings.enabled) return
        val mode = if (append) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        runCatching { tts?.speak(text, mode, null, "warning") }
    }

    fun shutdown() {
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
        ready = false
    }
}
