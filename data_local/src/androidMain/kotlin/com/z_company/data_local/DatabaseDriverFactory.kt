package com.z_company.data_local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createRouteDriver(): SqlDriver =
        createDriver(RouteDatabase.Schema, "Route.db")

    actual fun createSettingsDriver(): SqlDriver =
        createDriver(SettingsDatabase.Schema, "Settings.db")

    actual fun createSalarySettingDriver(): SqlDriver =
        createDriver(SalarySettingDatabase.Schema, "SalarySetting.db")

    actual fun createSearchResponseDriver(): SqlDriver =
        createDriver(SearchResponseDatabase.Schema, "SearchResponse.db")

    /**
     * Создаёт драйвер с поддержкой downgrade из старой Room БД.
     * Room использовал version 14+, SQLDelight начинает с version 1.
     * При downgrade просто сбрасываем version — таблицы совместимы.
     */
    private fun createDriver(
        schema: SqlSchema<QueryResult.Value<Unit>>,
        name: String
    ): SqlDriver = AndroidSqliteDriver(
        schema = schema,
        context = context,
        name = name,
        callback = object : AndroidSqliteDriver.Callback(schema) {
            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // Room version > SQLDelight version — допускаем downgrade без потери данных.
                // Таблицы структурно совместимы после миграции Step 9.
            }
        }
    )
}
