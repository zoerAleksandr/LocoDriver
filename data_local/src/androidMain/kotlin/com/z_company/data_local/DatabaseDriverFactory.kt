package com.z_company.data_local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase

class DatabaseDriverFactory(private val context: Context) {
    fun createRouteDriver(): SqlDriver =
        AndroidSqliteDriver(RouteDatabase.Schema, context, "Route.db")

    fun createSettingsDriver(): SqlDriver =
        AndroidSqliteDriver(SettingsDatabase.Schema, context, "Settings.db")

    fun createSalarySettingDriver(): SqlDriver =
        AndroidSqliteDriver(SalarySettingDatabase.Schema, context, "SalarySetting.db")

    fun createSearchResponseDriver(): SqlDriver =
        AndroidSqliteDriver(SearchResponseDatabase.Schema, context, "SearchResponse.db")
}
