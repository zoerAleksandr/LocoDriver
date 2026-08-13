package com.z_company.domain.entities.announcement

/**
 * Широковещательное сообщение пользователям, показываемое полноэкранным
 * экраном при запуске приложения (не пуш).
 *
 * `number` — порядковый номер сообщения; по нему на устройстве хранится
 * отметка «видел/не видел» (см. [com.z_company.domain.use_cases.AnnouncementUseCase]).
 *
 * `displayMode` — режим показа:
 * - [DISPLAY_MODE_ONCE] — показать один раз (дедуп по `number`);
 * - [DISPLAY_MODE_ALWAYS] — показывать при каждом запуске, пока сообщение
 *   активно на сервере (`number` игнорируется).
 * Неизвестное значение трактуется как «один раз» (безопасный дефолт).
 *
 * `type` — тип экрана:
 * - [TYPE_NEWS] — новость: показываются [title] + [body] (по умолчанию);
 * - [TYPE_UPDATE] — обновление: карусель фич из [features] (картинка +
 *   название + описание), [title]/[body] не показываются.
 * Неизвестное значение трактуется как «новость» (безопасный дефолт).
 */
data class Announcement(
    val number: Int,
    val title: String,
    val body: String,
    val displayMode: String = DISPLAY_MODE_ONCE,
    val type: String = TYPE_NEWS,
    val features: List<AnnouncementFeature> = emptyList(),
) {
    companion object {
        const val DISPLAY_MODE_ONCE = "once"
        const val DISPLAY_MODE_ALWAYS = "always"

        const val TYPE_NEWS = "news"
        const val TYPE_UPDATE = "update"
    }
}

/**
 * Одна фича обновления (строка карусели для [Announcement] типа [Announcement.TYPE_UPDATE]).
 * На экране показывается: [imageUrl] сверху → [title] → [description].
 *
 * `imageUrl` — абсолютный URL картинки (склеен из базового API-URL и пути,
 * пришедшего с сервера), либо null, если картинки нет.
 */
data class AnnouncementFeature(
    val position: Int,
    val title: String,
    val description: String,
    val imageUrl: String?,
)
