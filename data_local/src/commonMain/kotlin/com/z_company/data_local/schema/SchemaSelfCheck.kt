package com.z_company.data_local.schema

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * Идемпотентная самопроверка схемы после открытия БД. Лечит БД, у которых `user_version`
 * уже поднят (в обход миграций — fixVersionIfColumnsExist, смена сборки/ветки), но таблиц
 * или столбцов нет («no such table / no such column»).
 *
 * 1. `Schema.create` — все CREATE TABLE/INDEX в .sq написаны с IF NOT EXISTS;
 * 2. недостающие столбцы добавляются через ALTER TABLE ADD COLUMN.
 *
 * Любая ошибка проглатывается (приложение не должно падать из-за самопроверки); в лог
 * пишется только тип ошибки, без данных.
 */
object SchemaSelfCheck {

    fun run(driver: SqlDriver, schema: SqlSchema<QueryResult.Value<Unit>>, specs: List<TableSpec>) {
        try {
            schema.create(driver)
        } catch (e: Throwable) {
            println("SchemaSelfCheck: create failed: ${e::class.simpleName}")
        }
        val existing = HashMap<String, Set<String>>()
        for (spec in specs) {
            try {
                existing[spec.name] = readColumns(driver, spec.name)
            } catch (e: Throwable) {
                println("SchemaSelfCheck: table_info failed: ${e::class.simpleName}")
            }
        }
        for (sql in SchemaRepairPlanner.missingColumnStatements(specs, existing)) {
            try {
                driver.execute(null, sql, 0)
                println("SchemaSelfCheck: applied $sql")
            } catch (e: Throwable) {
                println("SchemaSelfCheck: alter failed: ${e::class.simpleName}")
            }
        }
    }

    private fun readColumns(driver: SqlDriver, table: String): Set<String> =
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA table_info($table)",
            mapper = { cursor ->
                val names = mutableSetOf<String>()
                while (cursor.next().value) {
                    cursor.getString(1)?.let { names += it }
                }
                QueryResult.Value<Set<String>>(names)
            },
            parameters = 0,
        ).value
}
