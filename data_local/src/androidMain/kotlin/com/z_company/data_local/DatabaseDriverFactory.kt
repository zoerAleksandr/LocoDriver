package com.z_company.data_local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
    actual fun createRouteDriver(): SqlDriver {
        migrateRouteDbIfNeeded()
        return createDriver(RouteDatabase.Schema, "Route.db")
    }

    actual fun createSettingsDriver(): SqlDriver {
        fixVersionIfColumnsExist("Settings.db", SettingsDatabase.Schema.version.toInt(),
            "UserSettings" to "isShowBreak")
        return createDriver(SettingsDatabase.Schema, "Settings.db")
    }

    actual fun createSalarySettingDriver(): SqlDriver {
        fixVersionIfColumnsExist("SalarySetting.db", SalarySettingDatabase.Schema.version.toInt(),
            "SalarySetting" to "surchargeLongTrainsList")
        return createDriver(SalarySettingDatabase.Schema, "SalarySetting.db")
    }

    actual fun createSearchResponseDriver(): SqlDriver =
        createDriver(SearchResponseDatabase.Schema, "SearchResponse.db")

    /**
     * Перед созданием драйвера гарантируем, что все нужные столбцы существуют
     * и версия БД соответствует SQLDelight-схеме.
     *
     * Покрывает все сценарии:
     * - Room → SQLDelight (любая Room-версия): добавляет недостающие столбцы
     * - SQLDelight → SQLDelight (повторный апгрейд после даунгрейда): пропускает существующие
     * - Свежая установка (файла нет): ничего не делает
     */
    private fun fixVersionIfColumnsExist(
        dbName: String,
        targetVersion: Int,
        vararg checks: Pair<String, String> // tableName to columnName
    ) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return

        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            // Добавляем недостающие столбцы (безопасно — если уже есть, пропускаем)
            for ((table, column) in checks) {
                if (!hasColumn(db, table, column)) {
                    val spec = COLUMN_SPECS["$table.$column"]
                        ?: ColumnSpec("INTEGER", true, "NULL")
                    val notNull = if (spec.nullable) "" else " NOT NULL"
                    db.execSQL(
                        "ALTER TABLE $table ADD COLUMN $column ${spec.type}$notNull DEFAULT ${spec.defaultValue}"
                    )
                }
            }
            // Выставляем целевую версию, чтобы SQLDelight-миграции не падали
            if (db.version != targetVersion) {
                db.version = targetVersion
            }
        } finally {
            db.close()
        }
    }

    private data class ColumnSpec(
        val type: String,
        val nullable: Boolean,
        val defaultValue: String
    )

    companion object {
        private val COLUMN_SPECS = mapOf(
            // Settings
            "UserSettings.isShowBreak" to ColumnSpec("INTEGER", false, "1"),
            // Route — BasicData
            "BasicData.timeStartBreak" to ColumnSpec("INTEGER", true, "NULL"),
            "BasicData.timeEndBreak" to ColumnSpec("INTEGER", true, "NULL"),
            // Route — Locomotive
            "Locomotive.auxiliaryCounterAccepted" to ColumnSpec("TEXT", true, "NULL"),
            "Locomotive.auxiliaryCounterDelivery" to ColumnSpec("TEXT", true, "NULL"),
            // Route — Train
            "Train.additionalNumbers" to ColumnSpec("TEXT", true, "NULL"),
            "Train.pusher" to ColumnSpec("TEXT", true, "NULL"),
            "Train.doubleTraction" to ColumnSpec("TEXT", true, "NULL"),
            "Train.doubledTrain" to ColumnSpec("TEXT", true, "NULL"),
            // SalarySetting
            "SalarySetting.surchargeLongTrainsList" to ColumnSpec("TEXT", false, "'[]'")
        )
    }

    private fun hasColumn(db: SQLiteDatabase, table: String, column: String): Boolean {
        val cursor = db.rawQuery("PRAGMA table_info($table)", null)
        try {
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return true
            }
            return false
        } finally {
            cursor.close()
        }
    }

    /**
     * Общий драйвер — при даунгрейде просто пропускает (лишние столбцы безвредны).
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
                // Все миграции выполнены в fix*IfNeeded — просто пропускаем.
            }
        }
    )

    /**
     * Route.db: пересоздаёт Train и Locomotive при миграции с Room (v14+),
     * добавляет недостающие столбцы, выставляет целевую версию.
     */
    private fun migrateRouteDbIfNeeded() {
        val dbFile = context.getDatabasePath("Route.db")
        if (!dbFile.exists()) return

        val targetVersion = RouteDatabase.Schema.version.toInt()
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            // Room → SQLDelight: пересоздаём таблицы с несовместимой схемой
            val needsTrainRecreate = hasColumn(db, "Train", "remoteObjectId")
            val needsLocoRecreate = hasColumn(db, "Locomotive", "removeObjectId")

            if (needsTrainRecreate) {
                // Добавляем недостающие колонки в старую таблицу ПЕРЕД копированием
                val trainNewColumns = arrayOf(
                    "additionalNumbers" to "TEXT DEFAULT NULL",
                    "servicePhase" to "TEXT DEFAULT NULL",
                    "pusher" to "TEXT DEFAULT NULL",
                    "doubleTraction" to "TEXT DEFAULT NULL",
                    "doubledTrain" to "TEXT DEFAULT NULL"
                )
                for ((col, def) in trainNewColumns) {
                    if (!hasColumn(db, "Train", col)) {
                        db.execSQL("ALTER TABLE Train ADD COLUMN $col $def")
                    }
                }

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS Train_new (
                        trainId TEXT NOT NULL PRIMARY KEY,
                        basicId TEXT NOT NULL,
                        number TEXT,
                        additionalNumbers TEXT DEFAULT NULL,
                        distance TEXT DEFAULT '',
                        weight TEXT,
                        axle TEXT,
                        conditionalLength TEXT,
                        isHeavyLongDistance INTEGER NOT NULL DEFAULT 0,
                        stations TEXT NOT NULL,
                        servicePhase TEXT DEFAULT NULL,
                        pusher TEXT DEFAULT NULL,
                        doubleTraction TEXT DEFAULT NULL,
                        doubledTrain TEXT DEFAULT NULL,
                        FOREIGN KEY (basicId) REFERENCES BasicData(id) ON DELETE CASCADE ON UPDATE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO Train_new (trainId, basicId, number, additionalNumbers, distance, weight, axle, conditionalLength, isHeavyLongDistance, stations, servicePhase, pusher, doubleTraction, doubledTrain)
                    SELECT trainId, basicId, number, additionalNumbers, distance, weight, axle, conditionalLength, isHeavyLongDistance, stations, servicePhase, pusher, doubleTraction, doubledTrain
                    FROM Train
                """.trimIndent())
                db.execSQL("DROP TABLE Train")
                db.execSQL("ALTER TABLE Train_new RENAME TO Train")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Train_basicId ON Train(basicId)")
            }

            if (needsLocoRecreate) {
                // Добавляем недостающие колонки в старую таблицу ПЕРЕД копированием
                val locoNewColumns = arrayOf(
                    "auxiliaryCounterAccepted" to "TEXT DEFAULT NULL",
                    "auxiliaryCounterDelivery" to "TEXT DEFAULT NULL"
                )
                for ((col, def) in locoNewColumns) {
                    if (!hasColumn(db, "Locomotive", col)) {
                        db.execSQL("ALTER TABLE Locomotive ADD COLUMN $col $def")
                    }
                }

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS Locomotive_new (
                        locoId TEXT NOT NULL PRIMARY KEY,
                        basicId TEXT NOT NULL,
                        series TEXT,
                        number TEXT,
                        type INTEGER NOT NULL,
                        electricSectionList TEXT NOT NULL,
                        dieselSectionList TEXT NOT NULL,
                        timeStartOfAcceptance INTEGER,
                        timeEndOfAcceptance INTEGER,
                        timeStartOfDelivery INTEGER,
                        timeEndOfDelivery INTEGER,
                        normaElectricCurrent1 INTEGER DEFAULT NULL,
                        normaElectricCurrent2 INTEGER DEFAULT NULL,
                        normaDiesel TEXT DEFAULT NULL,
                        heatingCounterAccepted TEXT DEFAULT NULL,
                        heatingCounterDelivery TEXT DEFAULT NULL,
                        auxiliaryCounterAccepted TEXT DEFAULT NULL,
                        auxiliaryCounterDelivery TEXT DEFAULT NULL,
                        FOREIGN KEY (basicId) REFERENCES BasicData(id) ON DELETE CASCADE ON UPDATE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO Locomotive_new (locoId, basicId, series, number, type, electricSectionList, dieselSectionList, timeStartOfAcceptance, timeEndOfAcceptance, timeStartOfDelivery, timeEndOfDelivery, normaElectricCurrent1, normaElectricCurrent2, normaDiesel, heatingCounterAccepted, heatingCounterDelivery)
                    SELECT locoId, basicId, series, number, type, electricSectionList, dieselSectionList, timeStartOfAcceptance, timeEndOfAcceptance, timeStartOfDelivery, timeEndOfDelivery, normaElectricCurrent1, normaElectricCurrent2, normaDiesel, heatingCounterAccepted, heatingCounterDelivery
                    FROM Locomotive
                """.trimIndent())
                db.execSQL("DROP TABLE Locomotive")
                db.execSQL("ALTER TABLE Locomotive_new RENAME TO Locomotive")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Locomotive_basicId ON Locomotive(basicId)")
            }

            // Добавляем недостающие столбцы (для случаев когда таблицы не пересоздавались)
            val routeChecks = arrayOf(
                "BasicData" to "timeStartBreak",
                "BasicData" to "timeEndBreak",
                "Locomotive" to "auxiliaryCounterAccepted",
                "Locomotive" to "auxiliaryCounterDelivery",
                "Train" to "additionalNumbers",
                "Train" to "pusher",
                "Train" to "doubleTraction",
                "Train" to "doubledTrain"
            )
            for ((table, column) in routeChecks) {
                if (!hasColumn(db, table, column)) {
                    val spec = COLUMN_SPECS["$table.$column"]
                        ?: ColumnSpec("INTEGER", true, "NULL")
                    val notNull = if (spec.nullable) "" else " NOT NULL"
                    db.execSQL(
                        "ALTER TABLE $table ADD COLUMN $column ${spec.type}$notNull DEFAULT ${spec.defaultValue}"
                    )
                }
            }

            if (db.version != targetVersion) {
                db.version = targetVersion
            }
        } finally {
            db.close()
        }
    }
}
