package com.z_company.repository.remote_rest

/** Решение LWW для одиночных записей настроек (UserSettings, SalarySetting). */
enum class SettingsSyncAction { PUSH, PULL, NOOP }

/**
 * Чистое сравнение меток для [SyncManager.syncBidirectional], часть 1.
 * Только даты: локальная новее → PUSH, серверная новее → PULL, равны → NOOP.
 * Флаг settingsPending на решение не влияет.
 */
object SettingsLww {
    fun decide(localUpdatedAt: Long, remoteUpdatedAt: Long): SettingsSyncAction = when {
        localUpdatedAt > remoteUpdatedAt -> SettingsSyncAction.PUSH
        remoteUpdatedAt > localUpdatedAt -> SettingsSyncAction.PULL
        else -> SettingsSyncAction.NOOP
    }
}
