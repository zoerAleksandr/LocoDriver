package com.z_company.data_local.schema

/** Таблица и её столбцы (имя to DDL-фрагмент после имени, например `INTEGER NOT NULL DEFAULT 0`). */
data class TableSpec(
    val name: String,
    val columns: List<Pair<String, String>>,
)

/**
 * Чистая логика самопроверки схемы: по описанию ожидаемых таблиц и фактическим столбцам
 * из `PRAGMA table_info` строит список недостающего. Не зависит от SQLite — тестируется на JVM.
 */
object SchemaRepairPlanner {

    /** Имена ожидаемых таблиц, которых нет в БД. */
    fun missingTables(specs: List<TableSpec>, existing: Map<String, Set<String>>): List<String> =
        specs.map { it.name }.filter { existing[it].isNullOrEmpty() }

    /**
     * `ALTER TABLE ... ADD COLUMN` для столбцов, которых нет в существующих таблицах.
     * Отсутствующие таблицы пропускаются — их создаёт `Schema.create` (все CREATE там идемпотентны).
     */
    fun missingColumnStatements(
        specs: List<TableSpec>,
        existing: Map<String, Set<String>>,
    ): List<String> {
        val result = mutableListOf<String>()
        for (spec in specs) {
            val have = existing[spec.name]?.takeIf { it.isNotEmpty() } ?: continue
            for ((column, ddl) in spec.columns) {
                if (column in have) continue
                result += "ALTER TABLE ${spec.name} ADD COLUMN $column ${withDefaultIfRequired(ddl)}"
            }
        }
        return result
    }

    /** SQLite запрещает ADD COLUMN ... NOT NULL без DEFAULT — подставляем нейтральное значение. */
    internal fun withDefaultIfRequired(ddl: String): String {
        val upper = ddl.uppercase()
        if (!upper.contains("NOT NULL") || upper.contains("DEFAULT")) return ddl
        val default = if (upper.trimStart().startsWith("TEXT")) "''" else "0"
        return "$ddl DEFAULT $default"
    }
}
