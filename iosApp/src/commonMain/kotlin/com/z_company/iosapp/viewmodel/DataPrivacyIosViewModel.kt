package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.iosapp.repository.IosTrackConsentStore
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RemoteRestClient
import com.z_company.repository.remote_rest.tracks.TrackConsentRequest
import com.z_company.repository.remote_rest.tracks.TrackUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ═════════════════════════════════════════════════════════════════════════════
// «Данные и конфиденциальность» — SCREEN_SPECS §17.7 (согласия №1 и №2).
// ═════════════════════════════════════════════════════════════════════════════

data class DataPrivacyState(
    val saveTracks: Boolean = false,
    /** Время согласия №1, мс (0 — не давалось). */
    val saveTracksAtMs: Long = 0L,
    val altitudeContrib: Boolean = false,
    /** Время согласия №2, мс (0 — не давалось). */
    val altitudeAtMs: Long = 0L,
    /** Согласие №2 дано на прежней редакции (только высоты): показать «Подтвердить новые условия». */
    val needsGnssConfirm: Boolean = false,
)

/**
 * Логика экрана «Данные и конфиденциальность» (§17.7). Включение согласий — сразу,
 * выключение — после подтверждения в UI (диалог вызывается SwiftUI, сюда приходит
 * уже подтверждённый отзыв). Само согласие без времени дублируется на сервер
 * `PUT /v1/tracks/consent` — best-effort, при ошибке сеть не мешает (локально
 * состояние уже записано).
 */
class DataPrivacyIosViewModel(
    private val consentStore: IosTrackConsentStore,
    private val secureTokenStorage: SecureTokenStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(buildState())

    fun watchState(callback: (DataPrivacyState) -> Unit) {
        viewModelScope.launch { _state.collect { callback(it) } }
    }

    /** Согласие №1 «Сохранять треки» (вызов после подтверждения отзыва в диалоге). */
    fun setSaveTracks(enabled: Boolean) {
        consentStore.setConsent(enabled)
        publishServerConsent(enabled)
        _state.value = buildState()
    }

    /** Согласие №2 «Передавать обезличенные данные для карт» (вызов после подтверждения отзыва). */
    fun setAltitudeContrib(enabled: Boolean) {
        consentStore.setAltitudeContrib(enabled)
        _state.value = buildState()
    }

    /** Подтвердить новую редакцию текста согласия №2 (высоты и места потери GPS). */
    fun confirmGnssText() {
        consentStore.confirmGnssText()
        _state.value = buildState()
    }

    fun dispose() {
        // Коллекции watchState живут в viewModelScope и закрываются с ним.
    }

    private fun buildState() = DataPrivacyState(
        saveTracks = consentStore.saveTracks.value,
        saveTracksAtMs = consentStore.consentAtMs(),
        altitudeContrib = consentStore.altitudeContrib.value,
        altitudeAtMs = consentStore.altitudeConsentAtMs(),
        needsGnssConfirm = consentStore.needsGnssTextConfirm(),
    )

    /** `PUT /tracks/consent {storageGranted}` — best-effort, без ожидания сети (§17.7). */
    private fun publishServerConsent(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (!token.isNullOrBlank()) {
                    RemoteRestClient.tracksApi.putConsent(
                        "Bearer $token",
                        TrackConsentRequest(enabled, TrackUploader.POLICY_VERSION),
                    )
                }
            }
        }
    }
}
