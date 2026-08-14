package com.z_company.domain.use_cases

import com.z_company.domain.entities.announcement.Announcement
import com.z_company.domain.repositories.AnnouncementRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories

/**
 * Логика показа сообщений при запуске — простая и предсказуемая.
 *
 * Режим показа приходит с сервера в [Announcement.displayMode]:
 * - [Announcement.DISPLAY_MODE_ONCE] («один раз») — показать активное сообщение,
 *   только если его `number` больше локально сохранённого «просмотренного» номера
 *   (`lastSeenAnnouncementNumber`, по умолчанию `-1`). После закрытия экрана номер
 *   сохраняется ([markSeen]), поэтому повторно то же сообщение не показывается.
 * - [Announcement.DISPLAY_MODE_ALWAYS] («каждый запуск») — показывать при каждом
 *   старте, пока сообщение активно на сервере; `lastSeenNumber` игнорируется.
 *   Показ прекращается, когда владелец выключает сообщение или меняет режим в кабинете.
 *
 * Управление с сервера — через `number` (для режима «один раз»):
 * - чтобы разослать новое сообщение всем, задайте `number` **больше** предыдущего;
 * - новые установки (lastSeen = -1) увидят текущее активное сообщение один раз;
 * - повторный POST с тем же/меньшим `number` никому не покажется (уже видели).
 *
 * Тип [Announcement.TYPE_UPDATE] («Обновление» — карусель фич) показываем **только
 * тем, кто обновил приложение**, а не свежим установкам: у новой установки нечего
 * «обновлять». Признак свежей установки передаёт платформа ([isFreshInstall]) —
 * на Android это `PackageInfo.firstInstallTime == lastUpdateTime`. Тип
 * [Announcement.TYPE_NEWS] (акции, новости) показываем всем, независимо от этого.
 */
class AnnouncementUseCase(
    private val repository: AnnouncementRepository,
    private val sharedPreferences: SharedPreferencesRepositories,
) {
    /**
     * Сообщение, которое нужно показать сейчас, либо null.
     *
     * @param isFreshInstall true — приложение установлено «с нуля» и ни разу не
     *   обновлялось; для таких пользователей сообщения типа «Обновление» не
     *   показываем.
     */
    suspend fun getAnnouncementToShow(
        platform: String,
        build: Long,
        isFreshInstall: Boolean = false,
    ): Announcement? {
        val announcement = repository.getLatest(platform, build) ?: return null
        // «Обновление» не показываем свежим установкам (им нечего обновлять).
        if (announcement.type == Announcement.TYPE_UPDATE && isFreshInstall) {
            return null
        }
        // «Каждый запуск»: показываем всегда, пока сообщение активно (его вернул сервер).
        if (announcement.displayMode == Announcement.DISPLAY_MODE_ALWAYS) {
            return announcement
        }
        // «Один раз»: показываем, только если сообщение новее уже виденного.
        val lastSeen = sharedPreferences.getLastSeenAnnouncementNumber()
        return if (announcement.number > lastSeen) announcement else null
    }

    /** Пометить сообщение показанным (после закрытия полноэкранного экрана). */
    fun markSeen(number: Int) {
        sharedPreferences.setLastSeenAnnouncementNumber(number)
    }
}
