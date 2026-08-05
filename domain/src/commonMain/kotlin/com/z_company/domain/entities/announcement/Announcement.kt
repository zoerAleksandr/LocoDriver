package com.z_company.domain.entities.announcement

/**
 * Широковещательное сообщение пользователям, показываемое полноэкранным
 * экраном при запуске приложения (не пуш).
 *
 * `number` — порядковый номер сообщения; по нему на устройстве хранится
 * отметка «видел/не видел» (см. [com.z_company.domain.use_cases.AnnouncementUseCase]).
 */
data class Announcement(
    val number: Int,
    val title: String,
    val body: String,
)
