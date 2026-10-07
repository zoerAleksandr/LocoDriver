package com.z_company.route.autorecord

import android.content.SharedPreferences
import com.z_company.domain.track.TrackRepository
import com.z_company.domain.track.TrackSyncSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Согласие №1: «Сохранять мои треки и историю поездок» (SCREEN_SPECS §6.9, §17.7).
 * По умолчанию ВЫКЛ; состояние и время согласия — только на устройстве (SharedPreferences).
 * Отзыв удаляет все локальные треки.
 */
class TrackConsentStore(
    private val prefs: SharedPreferences,
    private val trackRepository: TrackRepository?,
    private val scope: CoroutineScope,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    /** Согласие изменилось: запланировать отправку/отзыв на сервере (WorkManager в app). */
    private val onConsentChanged: () -> Unit = {},
) : TrackSyncSettings {
    private val _saveTracks = MutableStateFlow(prefs.getBoolean(KEY_SAVE_TRACKS, false))

    /** Дано ли согласие на сохранение треков. */
    val saveTracks: StateFlow<Boolean> = _saveTracks.asStateFlow()

    /** Время последнего согласия, мс (0 — не давалось). */
    fun consentAtMs(): Long = prefs.getLong(KEY_CONSENT_AT, 0L)

    /** Решение уже принято (экран согласия показан и подтверждён). */
    fun hasDecided(): Boolean = prefs.getBoolean(KEY_DECIDED, false)

    /** Включить/отозвать согласие. Отзыв удаляет локальные треки. */
    fun setConsent(enabled: Boolean) {
        val editor = prefs.edit().putBoolean(KEY_DECIDED, true).putBoolean(KEY_SAVE_TRACKS, enabled)
        if (enabled) editor.putLong(KEY_CONSENT_AT, nowMs()) else editor.remove(KEY_CONSENT_AT)
        editor.apply()
        _saveTracks.value = enabled
        if (!enabled) {
            scope.launch(Dispatchers.Default) { runCatching { trackRepository?.deleteAll() } }
        }
        // Серверу нужно сообщить об изменении (при отзыве он удалит все серверные треки).
        runCatching { onConsentChanged() }
    }

    override fun localConsent(): Boolean = _saveTracks.value

    /** Подтверждённое сервером состояние согласия (§17.7): null — ещё не сверялись. */
    override fun serverConsent(): Boolean? =
        if (prefs.contains(KEY_SERVER_CONSENT)) prefs.getBoolean(KEY_SERVER_CONSENT, false) else null

    override fun setServerConsent(value: Boolean?) {
        val editor = prefs.edit()
        if (value == null) editor.remove(KEY_SERVER_CONSENT) else editor.putBoolean(KEY_SERVER_CONSENT, value)
        editor.apply()
    }

    /** Экран согласия показан и закрыт (с любым выбором). */
    fun markDecided() {
        prefs.edit().putBoolean(KEY_DECIDED, true).apply()
    }

    /**
     * Однократное предупреждение «Трек не будет сохранён»: true один раз, пока согласия нет.
     * После получения согласия флаг сбрасывается — при новом отзыве предупредим снова.
     */
    fun takeNoConsentWarning(): Boolean {
        if (_saveTracks.value) {
            if (prefs.getBoolean(KEY_WARNED, false)) prefs.edit().putBoolean(KEY_WARNED, false).apply()
            return false
        }
        if (prefs.getBoolean(KEY_WARNED, false)) return false
        prefs.edit().putBoolean(KEY_WARNED, true).apply()
        return true
    }

    companion object {
        const val PREFS_NAME = "track_privacy"
        const val NO_CONSENT_WARNING = "Трек не будет сохранён: нет вашего согласия"
        private const val KEY_SAVE_TRACKS = "save_tracks"
        private const val KEY_CONSENT_AT = "consent_at_ms"
        private const val KEY_DECIDED = "decided"
        private const val KEY_WARNED = "no_consent_warned"
        private const val KEY_SERVER_CONSENT = "server_consent"
    }
}
