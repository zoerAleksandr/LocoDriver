package com.z_company.domain.use_cases

import com.z_company.domain.entities.announcement.Announcement
import com.z_company.domain.repositories.AnnouncementRepository
import com.z_company.domain.repositories.DiagnosticRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Логика показа сообщений при запуске — простая и предсказуемая.
 *
 * Новости ([Announcement.TYPE_NEWS]) и обновления ([Announcement.TYPE_UPDATE])
 * живут **независимо**: для каждого типа сервер отдаёт своё актуальное сообщение
 * (`GET /v1/announcements/latest?type=…`), а на устройстве хранится свой
 * `lastSeenNumber`. Поэтому «обновление» с бо́льшим номером не затирает
 * «новость» и наоборот — при запуске могут показаться оба, по очереди:
 * сначала обновление, потом новость.
 *
 * Режим показа приходит с сервера в [Announcement.displayMode]:
 * - [Announcement.DISPLAY_MODE_ONCE] («один раз») — показать активное сообщение,
 *   только если его `number` больше сохранённого «просмотренного» номера этого
 *   типа (по умолчанию `-1`). После закрытия экрана номер сохраняется
 *   ([markSeen]), поэтому повторно то же сообщение не показывается.
 * - [Announcement.DISPLAY_MODE_ALWAYS] («каждый запуск») — показывать при каждом
 *   старте, пока сообщение активно на сервере; `lastSeenNumber` игнорируется.
 *   Показ прекращается, когда владелец выключает сообщение или меняет режим в кабинете.
 *
 * Управление с сервера — через `number` (для режима «один раз»):
 * - чтобы разослать новое сообщение всем, задайте `number` **больше** предыдущего
 *   сообщения того же типа;
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
    private val diagnostics: DiagnosticRepository,
) {
    /**
     * Сообщения, которые нужно показать сейчас, в порядке показа
     * (сначала обновление, затем новость); пустой список — показывать нечего.
     *
     * @param isFreshInstall true — приложение установлено «с нуля» и ни разу не
     *   обновлялось; для таких пользователей сообщения типа «Обновление» не
     *   показываем.
     */
    suspend fun getAnnouncementsToShow(
        platform: String,
        build: Long,
        isFreshInstall: Boolean = false,
    ): List<Announcement> = coroutineScope {
        val update = async { repository.getLatest(platform, build, Announcement.TYPE_UPDATE) }
        val news = async { repository.getLatest(platform, build, Announcement.TYPE_NEWS) }
        listOfNotNull(update.await(), news.await())
            // Старый сервер игнорирует `type` и отдаёт одно и то же сообщение на оба
            // запроса — не показываем его дважды.
            .distinctBy { it.number }
            .filter { shouldShow(it, isFreshInstall) }
    }

    /** Показывать ли сообщение: правила по типу, режиму показа и счётчику «видел». */
    private fun shouldShow(announcement: Announcement, isFreshInstall: Boolean): Boolean {
        // «Обновление» не показываем свежим установкам (им нечего обновлять).
        if (announcement.type == Announcement.TYPE_UPDATE && isFreshInstall) {
            return false
        }
        // «Каждый запуск»: показываем всегда, пока сообщение активно (его вернул сервер).
        if (announcement.displayMode == Announcement.DISPLAY_MODE_ALWAYS) {
            return true
        }
        // «Один раз»: показываем, только если сообщение новее уже виденного этого типа.
        val lastSeen = sharedPreferences.getLastSeenAnnouncementNumber(announcement.type)
        return announcement.number > lastSeen
    }

    /** Пометить сообщение показанным (после закрытия полноэкранного экрана). */
    fun markSeen(announcement: Announcement) {
        sharedPreferences.setLastSeenAnnouncementNumber(announcement.type, announcement.number)
    }

    /**
     * Сообщить серверу о просмотре — для счётчика «сколько установок на какой
     * платформе увидели» в кабинете. Вызывать после [markSeen]; сетевые ошибки
     * репозиторий глотает.
     */
    suspend fun reportSeen(announcement: Announcement, platform: String) {
        repository.reportSeen(announcement.number, platform, diagnostics.getInstallationId())
    }
}
