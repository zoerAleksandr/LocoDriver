package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.repositories.SharedPreferencesRepositories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * KMP ViewModel «Что нового» после обновления (SCREEN_SPECS §22).
 *
 * Легаси-блок из 2 слайдов (`OnBoardingItems.getDataUpdatePresentation()` на
 * Android; контент зашит в SwiftUI — `WhatsNewView`), показывается overlay'ем
 * поверх приложения, если флаг `TOKEN_IS_SHOW_UPDATE_PRESENTATION_VER_1_2_16`
 * (по умолчанию `true`) ещё не сброшен и это не первый запуск.
 *
 * Название `enableShowingUpdatePresentation()` обманчиво: метод пишет `false`
 * (как и на Android в `SharedPreferenceStorage`) — флаг гасится после показа,
 * поэтому блок видят один раз.
 *
 * ⚠️ Отклонение от Android (BUG-08): Android-оверлей не исчезает в сессии
 * (управляется неизменяемым значением, прочитанным при старте). Здесь кнопки
 * «Пропустить»/«Начать» реально закрывают блок.
 */
class WhatsNewIosViewModel(
    private val sharedPrefs: SharedPreferencesRepositories,
) : ViewModel() {

    private val _shouldShow = MutableStateFlow(false)
    val shouldShow: StateFlow<Boolean> = _shouldShow.asStateFlow()

    init {
        // Флаг первого запуска читается один раз и сразу гасится
        // (зеркало `MainViewModel.init` на Android).
        val isFirstEntry = sharedPrefs.tokenIsFirstAppEntry()
        if (isFirstEntry) {
            sharedPrefs.setTokenIsFirstAppEntry(false)
        }
        _shouldShow.value = sharedPrefs.isShowUpdatePresentation() && !isFirstEntry
    }

    /** «Пропустить»/«Начать»: закрыть блок и сбросить флаг (показ — один раз). */
    fun dismissWhatsNew() {
        _shouldShow.value = false
        sharedPrefs.enableShowingUpdatePresentation()
    }

    fun watchShouldShow(callback: (Boolean) -> Unit) {
        viewModelScope.launch { shouldShow.collect { callback(it) } }
    }
}
