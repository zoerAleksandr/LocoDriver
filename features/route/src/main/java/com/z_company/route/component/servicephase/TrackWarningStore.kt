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
 * Предупреждения по участкам: хранятся на устройстве (SharedPreferences, JSON по id участка) и на сервер не уходят. Список
 * сохранённых живёт между поездками - при повторном проследовании участка их предлагается применить снова.
 */
internal class TrackWarningStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("track_warnings", Context.MODE_PRIVATE)

    fun load(sectionId: String): List<TrackWarning> = runCatching {
        val raw = prefs.getString(sectionId, null) ?: return emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).map { fromJson(sectionId, arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun save(sectionId: String, list: List<TrackWarning>) {
        runCatching {
            if (list.isEmpty()) prefs.edit().remove(sectionId).apply()
            else prefs.edit().putString(sectionId, JSONArray(list.map { toJson(it) }).toString()).apply()
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

    fun speak(text: String) {
        if (!ready || !settings.enabled) return
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "warning") }
    }

    fun shutdown() {
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
        ready = false
    }
}
