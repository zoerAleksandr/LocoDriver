package com.z_company.data_local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import com.z_company.data_local.schema.SchemaSelfCheck
import com.z_company.data_local.schema.SchemaSpecs
import com.z_company.data_local.schema.TableSpec
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase
import com.z_company.data_local.brake.db.BrakeCertificateDatabase
import com.z_company.data_local.track.db.TrackDatabase

/**
 * iOS-реализация: SQLDelight NativeSqliteDriver.
 * Использует встроенный SQLite iOS/macOS через Kotlin/Native C-interop.
 * Файлы БД хранятся в стандартном Documents-каталоге приложения.
 */
actual class DatabaseDriverFactory {

    actual fun createRouteDriver(): SqlDriver =
        createHealedDriver(RouteDatabase.Schema, "Route.db", SchemaSpecs.route)

    actual fun createSettingsDriver(): SqlDriver =
        createHealedDriver(SettingsDatabase.Schema, "Settings.db", SchemaSpecs.settings)

    actual fun createSalarySettingDriver(): SqlDriver =
        createHealedDriver(SalarySettingDatabase.Schema, "SalarySetting.db", SchemaSpecs.salarySetting)

    actual fun createSearchResponseDriver(): SqlDriver =
        createHealedDriver(SearchResponseDatabase.Schema, "SearchResponse.db", SchemaSpecs.searchResponse)

    actual fun createTrackDriver(): SqlDriver =
        createHealedDriver(TrackDatabase.Schema, "Track.db", SchemaSpecs.track)

    actual fun createBrakeCertificateDriver(): SqlDriver =
        createHealedDriver(BrakeCertificateDatabase.Schema, "BrakeCertificate.db", SchemaSpecs.brake)

    /**
     * Драйвер + самопроверка схемы: достраивает таблицы/столбцы, если user_version
     * поднят в обход миграций (как в Android createDriver).
     */
    private fun createHealedDriver(
        schema: SqlSchema<QueryResult.Value<Unit>>,
        name: String,
        specs: List<TableSpec>,
    ): SqlDriver = NativeSqliteDriver(schema, name).also { driver ->
        SchemaSelfCheck.run(driver, schema, specs)
    }
}
