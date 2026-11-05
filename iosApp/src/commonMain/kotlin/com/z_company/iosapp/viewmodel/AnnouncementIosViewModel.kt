package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.announcement.Announcement
import com.z_company.domain.use_cases.AnnouncementUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

/**
 * Модель фичи карусели «Обновление» (§23.1) для SwiftUI.
 * Имена полей экспортируются в ObjC — без `new`/`init`/`copy`/`description`.
 */
data class AnnouncementFeatureUi(
    val position: Int,
    val title: String,
    val summary: String,
    val imageUrl: String?,
)

/**
 * Модель сообщения «Новость при запуске» (§23) для SwiftUI.
 */
data class AnnouncementUi(
    val number: Int,
    val title: String,
    val bodyText: String,
    val isUpdateType: Boolean,
    val imageUrl: String?,
    val features: List<AnnouncementFeatureUi>,
)

/**
 * KMP ViewModel «Новость при запуске» (SCREEN_SPECS §23).
 *
 * Зеркало `MainViewModel` Android: очередь сообщений (сначала «Обновление»,
 * затем «Новость» — порядок отдаёт `AnnouncementUseCase`), показ первого
 * элемента, `dismissAnnouncement()` помечает сообщение просмотренным
 * (хранение — на устройстве, §23.3) и открывает следующее.
 *
 * Загрузка запускается после появления корня приложения (`LaunchOverlays`),
 * платформа — `"ios"`, свежая установка определяется по `ld.firstSeenBuild`
 * (анлог Android `firstInstallTime == lastUpdateTime`): тип «Обновление»
 * свежим установкам не показываем.
 */
class AnnouncementIosViewModel(
    private val announcementUseCase: AnnouncementUseCase,
) : ViewModel() {

    private val _queue = MutableStateFlow<List<Announcement>>(emptyList())

    /** Текущее показываемое сообщение (первое в очереди), либо null. */
    val announcement: StateFlow<AnnouncementUi?> = _queue
        .map { it.firstOrNull()?.toUi() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Загрузить непросмотренные сообщения (офлайн/204 — пустая очередь). */
    fun loadAnnouncement() {
        viewModelScope.launch {
            try {
                _queue.value = announcementUseCase.getAnnouncementsToShow(
                    platform = "ios",
                    build = iosBuildNumber(),
                    isFreshInstall = isFreshInstall(),
                )
            } catch (_: Exception) {
                // Показ сообщения не критичен: офлайн/ошибка — просто не показываем.
            }
        }
    }

    /** Закрыть текущее сообщение: пометить виденным и перейти к следующему в очереди. */
    fun dismissAnnouncement() {
        val current = _queue.value.firstOrNull() ?: return
        announcementUseCase.markSeen(current)
        _queue.value = _queue.value.drop(1)
        viewModelScope.launch {
            try {
                announcementUseCase.reportSeen(current, platform = "ios")
            } catch (_: Exception) {
                // Счётчик просмотров — не критично.
            }
        }
    }

    fun watchAnnouncement(callback: (AnnouncementUi?) -> Unit) {
        viewModelScope.launch { announcement.collect { callback(it) } }
    }

    private fun Announcement.toUi(): AnnouncementUi = AnnouncementUi(
        number = number,
        title = title,
        bodyText = body,
        isUpdateType = type == Announcement.TYPE_UPDATE && features.isNotEmpty(),
        imageUrl = imageUrl,
        features = features.map {
            AnnouncementFeatureUi(
                position = it.position,
                title = it.title,
                summary = it.description,
                imageUrl = it.imageUrl,
            )
        },
    )
}

/** Номер сборки клиента (CFBundleVersion) — для фильтра min/maxBuild на сервере. */
internal fun iosBuildNumber(): Long {
    val build = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String
    return build?.toLongOrNull() ?: 0L
}

/**
 * Свежая установка: приложение установлено «с нуля» и ни разу не обновлялось.
 * В NSUserDefaults один раз пишется номер сборки первого запуска
 * (`ld.firstSeenBuild`); пока текущая сборка совпадает с ним — установка свежая.
 * При удалении и повторной установке ключ исчезает — снова свежая (как на Android
 * после переустановки того же APK). При ошибке — НЕ свежая (не прячем сообщение).
 */
internal fun isFreshInstall(): Boolean {
    return try {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = "ld.firstSeenBuild"
        val current = iosBuildNumber().toString()
        val stored = defaults.stringForKey(key)
        if (stored == null) {
            defaults.setObject(current, forKey = key)
            true
        } else {
            stored == current
        }
    } catch (_: Exception) {
        false
    }
}
