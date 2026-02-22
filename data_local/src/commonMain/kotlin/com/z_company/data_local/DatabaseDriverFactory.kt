package com.z_company.data_local

import app.cash.sqldelight.db.SqlDriver

/**
 * Шаг 12 KMP-миграции: expect/actual для создания SQLDelight-драйверов.
 *
 * Android: AndroidSqliteDriver (Room-compatible SQLite через JNI)
 * iOS:     NativeSqliteDriver  (Kotlin/Native, встроенный SQLite)
 *
 * Конструктор не объявлен в expect — Android требует Context, iOS — нет.
 */
expect class DatabaseDriverFactory {
    fun createRouteDriver(): SqlDriver
    fun createSettingsDriver(): SqlDriver
    fun createSalarySettingDriver(): SqlDriver
    fun createSearchResponseDriver(): SqlDriver
}
