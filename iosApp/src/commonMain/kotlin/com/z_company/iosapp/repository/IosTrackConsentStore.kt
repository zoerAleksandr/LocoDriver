@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.repository

import com.z_company.domain.track.AltitudeContribSettings
import com.z_company.domain.track.TrackSyncSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults
import kotlin.time.Clock

/**
 * Согласие №1 «Сохранять мои треки и историю поездок» и №2 «Передавать обезличенные данные
 * для карт» (SCREEN_SPECS §6.9, §17.7). Порт Android `TrackConsentStore`: состояние и время
 * согласия — только на устройстве (NSUserDefaults, префикс `ld.track_privacy.`).
 *
 * По умолчанию ВЫКЛ. Отзыв №1 треки не удаляет. Согласие №2 независимо от №1; редакция
 * текста (только высоты → высоты и места потери GPS) — [ALTITUDE_TEXT_VERSION].
 */
class IosTrackConsentStore(
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : TrackSyncSettings, AltitudeContribSettings {

    private fun ud(): NSUserDefaults = NSUserDefaults.standardUserDefaults
    private fun key(name: String) = "ld.track_privacy.$name"

    private fun bool(name: String, default: Boolean): Boolean =
        if (ud().objectForKey(key(name)) == null) default else ud().boolForKey(key(name))

    private fun long(name: String): Long = ud().integerForKey(key(name))

    private fun int(name: String, default: Int): Int =
        if (ud().objectForKey(key(name)) == null) default else ud().integerForKey(key(name)).toInt()

    private fun putBool(name: String, value: Boolean) = ud().setBool(value, forKey = key(name))

    private fun putLong(name: String, value: Long) = ud().setInteger(value, forKey = key(name))

    private fun remove(name: String) = ud().removeObjectForKey(key(name))

    private val _saveTracks = MutableStateFlow(bool(KEY_SAVE_TRACKS, false))

    /** Дано ли согласие на сохранение треков. */
    val saveTracks: StateFlow<Boolean> = _saveTracks.asStateFlow()

    private val _altitudeContrib = MutableStateFlow(bool(KEY_ALTITUDE_CONTRIB, false))

    /** Согласие №2: обезличенные данные для карт. Независимо от №1, по умолчанию ВЫКЛ. */
    val altitudeContrib: StateFlow<Boolean> = _altitudeContrib.asStateFlow()

    /** Время последнего согласия №1, мс (0 — не давалось). */
    fun consentAtMs(): Long = long(KEY_CONSENT_AT)

    /** Время согласия №2, мс (0 — не давалось). */
    fun altitudeConsentAtMs(): Long = long(KEY_ALTITUDE_CONTRIB_AT)

    /** Решение уже принято (экран согласия показан и подтверждён). */
    fun hasDecided(): Boolean = bool(KEY_DECIDED, false)

    /** Экран согласия показан и закрыт (с любым выбором). */
    fun markDecided() {
        putBool(KEY_DECIDED, true)
    }

    /** Включить/отозвать согласие №1. Отзыв треки не удаляет. */
    fun setConsent(enabled: Boolean) {
        putBool(KEY_DECIDED, true)
        putBool(KEY_SAVE_TRACKS, enabled)
        if (enabled) putLong(KEY_CONSENT_AT, nowMs()) else remove(KEY_CONSENT_AT)
        _saveTracks.value = enabled
    }

    /**
     * Включить/отозвать согласие №2. Отзыв только останавливает дальнейшую передачу:
     * уже отправленные обезличенные данные вернуть нельзя.
     */
    fun setAltitudeContrib(enabled: Boolean) {
        putBool(KEY_DECIDED, true)
        putBool(KEY_ALTITUDE_CONTRIB, enabled)
        // Включение — всегда по текущей редакции текста (с местами потери GPS).
        if (enabled) {
            putLong(KEY_ALTITUDE_CONTRIB_AT, nowMs())
            putInt(KEY_ALTITUDE_TEXT_VERSION, ALTITUDE_TEXT_VERSION)
        } else {
            remove(KEY_ALTITUDE_CONTRIB_AT)
            remove(KEY_ALTITUDE_TEXT_VERSION)
        }
        _altitudeContrib.value = enabled
    }

    private fun putInt(name: String, value: Int) = ud().setInteger(value.toLong(), forKey = key(name))

    /**
     * Вклад «места без GPS» разрешён: согласие №2 дано именно в редакции [ALTITUDE_TEXT_VERSION].
     * Согласившиеся на прежнюю редакцию (только высоты) подтверждают новую — [confirmGnssText].
     */
    fun gnssContribAllowed(): Boolean =
        _altitudeContrib.value && int(KEY_ALTITUDE_TEXT_VERSION, 1) >= ALTITUDE_TEXT_VERSION

    /** Согласие №2 дано, но на прежних условиях (только высоты): места без GPS не передаются. */
    fun needsGnssTextConfirm(): Boolean = _altitudeContrib.value && !gnssContribAllowed()

    /** Подтвердить новую редакцию согласия №2 (высоты и места потери GPS). */
    fun confirmGnssText() {
        if (!_altitudeContrib.value) return
        putInt(KEY_ALTITUDE_TEXT_VERSION, ALTITUDE_TEXT_VERSION)
    }

    /**
     * Однократное предупреждение «Трек не будет сохранён»: true один раз, пока согласия нет.
     * После согласия флаг сбрасывается — при новом отзыве предупредим снова.
     */
    fun takeNoConsentWarning(): Boolean {
        if (_saveTracks.value) {
            if (bool(KEY_WARNED, false)) putBool(KEY_WARNED, false)
            return false
        }
        if (bool(KEY_WARNED, false)) return false
        putBool(KEY_WARNED, true)
        return true
    }

    override fun enabled(): Boolean = _altitudeContrib.value

    override fun localConsent(): Boolean = _saveTracks.value

    /** Подтверждённое сервером состояние согласия (§17.7): null — ещё не сверялись. */
    override fun serverConsent(): Boolean? =
        if (ud().objectForKey(key(KEY_SERVER_CONSENT)) == null) null else bool(KEY_SERVER_CONSENT, false)

    override fun setServerConsent(value: Boolean?) {
        if (value == null) remove(KEY_SERVER_CONSENT) else putBool(KEY_SERVER_CONSENT, value)
    }

    companion object {
        const val NO_CONSENT_WARNING = "Трек не будет сохранён: нет вашего согласия"

        /** Редакция текста согласия №2: 1 — только высоты, 2 — высоты и места потери GPS. */
        const val ALTITUDE_TEXT_VERSION = 2

        private const val KEY_SAVE_TRACKS = "save_tracks"
        private const val KEY_CONSENT_AT = "consent_at_ms"
        private const val KEY_DECIDED = "decided"
        private const val KEY_WARNED = "no_consent_warned"
        private const val KEY_SERVER_CONSENT = "server_consent"
        private const val KEY_ALTITUDE_CONTRIB = "altitude_contrib"
        private const val KEY_ALTITUDE_CONTRIB_AT = "altitude_contrib_at_ms"
        private const val KEY_ALTITUDE_TEXT_VERSION = "altitude_contrib_text_v"
    }
}
