package com.z_company.data_local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase

/**
 * iOS-реализация: SQLDelight NativeSqliteDriver.
 * Использует встроенный SQLite iOS/macOS через Kotlin/Native C-interop.
 * Файлы БД хранятся в стандартном Documents-каталоге приложения.
 */
actual class DatabaseDriverFactory {

    actual fun createRouteDriver(): SqlDriver =
        NativeSqliteDriver(RouteDatabase.Schema, "Route.db")

    actual fun createSettingsDriver(): SqlDriver =
        NativeSqliteDriver(SettingsDatabase.Schema, "Settings.db")

    actual fun createSalarySettingDriver(): SqlDriver =
        NativeSqliteDriver(SalarySettingDatabase.Schema, "SalarySetting.db")

    actual fun createSearchResponseDriver(): SqlDriver =
        NativeSqliteDriver(SearchResponseDatabase.Schema, "SearchResponse.db")
}
