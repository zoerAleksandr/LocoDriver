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
        // Проверяем ВСЕ новые столбцы из всех миграций (1.sqm … 12.sqm).
        // Это покрывает Room→SQLDelight и SQLDelight→SQLDelight upgrade-пути,
        // где какие-либо миграции могли быть пропущены.
        // ВАЖНО: subscriptionPeriod и isDecimalTime (миграция 5) тоже должны быть
        // здесь — иначе fixVersionIfColumnsExist выставит version=5, SQLDelight
        // пропустит 5.sqm, и столбцы никогда не добавятся → SQLiteException.
        ensureSettingsTablesV6("Settings.db")
        ensureSettingsTablesV12("Settings.db")
        fixVersionIfColumnsExist(
            "Settings.db",
            SettingsDatabase.Schema.version.toInt(),
            "UserSettings" to "isShowBreak",
            "UserSettings" to "isShowOnePersonSwitch",
            "UserSettings" to "isShowLocoHeating",
            "UserSettings" to "isShowLocoAuxiliary",
            "UserSettings" to "isShowLocoStatistics",
            "UserSettings" to "isShowLocoNorma",
            "UserSettings" to "isShowOtherCurrent",
            "UserSettings" to "subscriptionPeriod",
            "UserSettings" to "isDecimalTime",
            "UserSettings" to "country",
            "UserSettings" to "crossMonthTimezone",
            "UserSettings" to "standardTimesStartWork",
            "UserSettings" to "useStandardTimePicker",
            "UserSettings" to "locomotiveSeriesList",
            "UserSettings" to "servicePhases",
            "UserSettings" to "region",
            "MonthOfYear" to "tariffRate",
            "MonthOfYear" to "dateSetTariffRate",
            primaryTable = "UserSettings")
        return createDriver(SettingsDatabase.Schema, "Settings.db")
    }

    /**
     * Создаёт таблицы ReleaseDay и ProductionCalendarDay если они не существуют.
     * Необходимо для пользователей, обновившихся с версии до 6.sqm — fixVersionIfColumnsExist
     * выставляет версию сразу в targetVersion, обходя SQLDelight-миграции.
     */
    private fun ensureSettingsTablesV6(dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS ReleaseDay (
                    id TEXT NOT NULL PRIMARY KEY,
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    dayOfMonth INTEGER NOT NULL,
                    releaseType TEXT NOT NULL
                )
            """.trimIndent())
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_release_day_year_month ON ReleaseDay(year, month)"
            )
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS ProductionCalendarDay (
                    country TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    dayOfMonth INTEGER NOT NULL,
                    tag TEXT NOT NULL,
                    PRIMARY KEY (country, year, month, dayOfMonth)
                )
            """.trimIndent())
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_prod_cal_country_year ON ProductionCalendarDay(country, year)"
            )
        } finally {
            db.close()
        }
    }

    /**
     * Создаёт таблицы LocomotiveSeries и StationNorm если они не существуют.
     * Необходимо для пользователей, обновившихся через fixVersionIfColumnsExist —
     * он выставляет версию сразу в targetVersion, обходя SQLDelight-миграцию 12.sqm.
     */
    private fun ensureSettingsTablesV12(dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS LocomotiveSeries (
                    seriesId TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    acceptanceDurationMin INTEGER,
                    deliveryDurationMin INTEGER,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS StationNorm (
                    stationId TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    appearanceToStartMin INTEGER,
                    endToBarrierMin INTEGER,
                    barrierToStartMin INTEGER,
                    endToWorkEndMin INTEGER,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
        } finally {
            db.close()
        }
    }

    actual fun createSalarySettingDriver(): SqlDriver {
        fixVersionIfColumnsExist(
            "SalarySetting.db",
            SalarySettingDatabase.Schema.version.toInt(),
            "SalarySetting" to "nightTimePercent",
            "SalarySetting" to "surchargeLongTrainsList",
            primaryTable = "SalarySetting")
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
     *
     * @param primaryTable Если указана — версия выставляется только когда эта таблица существует.
     *                     Если таблица отсутствует — позволяем SQLDelight выполнить миграции.
     *                     Пустая строка = всегда выставлять версию (прежнее поведение).
     */
    private fun fixVersionIfColumnsExist(
        dbName: String,
        targetVersion: Int,
        vararg checks: Pair<String, String>, // tableName to columnName
        primaryTable: String = ""
    ) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return

        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            // Добавляем недостающие столбцы (безопасно — если уже есть, пропускаем).
            // Если таблица ещё не существует — пропускаем ALTER: SQLDelight создаст
            // её заново со всеми столбцами через Schema.create().
            for ((table, column) in checks) {
                if (!hasTable(db, table)) continue
                if (!hasColumn(db, table, column)) {
                    val spec = COLUMN_SPECS["$table.$column"]
                        ?: ColumnSpec("INTEGER", true, "NULL")
                    val notNull = if (spec.nullable) "" else " NOT NULL"
                    db.execSQL(
                        "ALTER TABLE $table ADD COLUMN $column ${spec.type}$notNull DEFAULT ${spec.defaultValue}"
                    )
                }
            }
            // Исправляем NULL-значения в NOT NULL столбцах:
            // Предыдущие версии могли добавить столбцы с DEFAULT NULL (до того как
            // они появились в COLUMN_SPECS). SQLDelight читает NOT NULL Int — NPE.
            for ((table, column) in checks) {
                val spec = COLUMN_SPECS["$table.$column"] ?: continue
                if (!spec.nullable) {
                    if (!hasTable(db, table)) continue
                    db.execSQL(
                        "UPDATE $table SET $column = ${spec.defaultValue} WHERE $column IS NULL"
                    )
                }
            }
            // Выставляем целевую версию, чтобы SQLDelight-миграции не падали.
            // Если задана primaryTable — версию выставляем только когда эта таблица существует.
            // Если таблица отсутствует — пусть SQLDelight прогоняет миграции самостоятельно,
            // а первая миграция (1.sqm) создаст таблицу через CREATE TABLE IF NOT EXISTS.
            val canBumpVersion = primaryTable.isEmpty() || hasTable(db, primaryTable)
            if (canBumpVersion && db.version != targetVersion) {
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
            // Settings — все новые столбцы (миграции 1.sqm … 10.sqm)
            "UserSettings.isShowBreak" to ColumnSpec("INTEGER", false, "1"),
            "UserSettings.isShowOnePersonSwitch" to ColumnSpec("INTEGER", false, "1"),
            "UserSettings.isShowLocoHeating" to ColumnSpec("INTEGER", false, "1"),
            "UserSettings.isShowLocoAuxiliary" to ColumnSpec("INTEGER", false, "1"),
            "UserSettings.isShowLocoStatistics" to ColumnSpec("INTEGER", false, "1"),
            "UserSettings.isShowLocoNorma" to ColumnSpec("INTEGER", false, "1"),
            "UserSettings.isShowOtherCurrent" to ColumnSpec("INTEGER", false, "0"),
            "UserSettings.subscriptionPeriod" to ColumnSpec("INTEGER", false, "0"),
            "UserSettings.isDecimalTime" to ColumnSpec("INTEGER", false, "0"),
            "UserSettings.country" to ColumnSpec("TEXT", false, "'RU'"),
            "UserSettings.crossMonthTimezone" to ColumnSpec("TEXT", false, "'LOCAL'"),
            "UserSettings.useStandardTimePicker" to ColumnSpec("INTEGER", false, "0"),
            "UserSettings.standardTimesStartWork" to ColumnSpec("TEXT", false, "'[28800000, 72000000]'"),
            "UserSettings.locomotiveSeriesList" to ColumnSpec("TEXT", false, "'[]'"),
            "UserSettings.servicePhases" to ColumnSpec("TEXT", false, "'[]'"),
            "UserSettings.region" to ColumnSpec("TEXT", true, "NULL"),
            // Settings — MonthOfYear
            "MonthOfYear.tariffRate" to ColumnSpec("REAL", false, "0.0"),
            "MonthOfYear.dateSetTariffRate" to ColumnSpec("TEXT", true, "NULL"),
            // Route — BasicData
            "BasicData.timeStartBreak" to ColumnSpec("INTEGER", true, "NULL"),
            "BasicData.timeStartWorkBeforeArrival" to ColumnSpec("INTEGER", true, "NULL"),
            "BasicData.timeEndBreak" to ColumnSpec("INTEGER", true, "NULL"),
            // Route — Locomotive
            "Locomotive.auxiliaryCounterAccepted" to ColumnSpec("TEXT", true, "NULL"),
            "Locomotive.auxiliaryCounterDelivery" to ColumnSpec("TEXT", true, "NULL"),
            "Locomotive.timeBarrierOut" to ColumnSpec("INTEGER", true, "NULL"),
            "Locomotive.timeBarrierIn" to ColumnSpec("INTEGER", true, "NULL"),
            "Locomotive.acceptanceStationId" to ColumnSpec("TEXT", true, "NULL"),
            "Locomotive.deliveryStationId" to ColumnSpec("TEXT", true, "NULL"),
            // Route — Train
            "Train.additionalNumbers" to ColumnSpec("TEXT", true, "NULL"),
            "Train.pusher" to ColumnSpec("TEXT", true, "NULL"),
            "Train.doubleTraction" to ColumnSpec("TEXT", true, "NULL"),
            "Train.doubledTrain" to ColumnSpec("TEXT", true, "NULL"),
            // Route — Passenger (миграция 9)
            "Passenger.isWorkStartByArrival" to ColumnSpec("INTEGER", false, "0"),
            // SalarySetting
            "SalarySetting.nightTimePercent" to ColumnSpec("REAL", false, "40.0"),
            "SalarySetting.surchargeLongTrainsList" to ColumnSpec("TEXT", false, "'[]'")
        )
    }

    private fun hasTable(db: SQLiteDatabase, table: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table)
        )
        return cursor.use { it.count > 0 }
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
                        timeBarrierOut INTEGER DEFAULT NULL,
                        timeBarrierIn INTEGER DEFAULT NULL,
                        acceptanceStationId TEXT DEFAULT NULL,
                        deliveryStationId TEXT DEFAULT NULL,
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

            // Создаём BasicData, если она ещё не существует
            // (критическая таблица; без неё любой SELECT упадёт с "no such table/column")
            if (!hasTable(db, "BasicData")) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS BasicData (
                        id TEXT NOT NULL PRIMARY KEY,
                        remoteRouteId TEXT DEFAULT NULL,
                        isOnePersonOperation INTEGER NOT NULL DEFAULT 0,
                        isSynchronized INTEGER NOT NULL DEFAULT 0,
                        remoteObjectId TEXT,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        updatedAt TEXT NOT NULL DEFAULT '',
                        number TEXT,
                        timeStartWork INTEGER,
                        timeEndWork INTEGER,
                        restPointOfTurnover INTEGER NOT NULL DEFAULT 0,
                        notes TEXT,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        timeStartBreak INTEGER,
                        timeEndBreak INTEGER
                    )
                """.trimIndent())
            }

            // Создаём Locomotive, если она ещё не существует
            // (случай: Room→SQLDelight, версия выставлена без прохождения миграций 1-3)
            if (!hasTable(db, "Locomotive")) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS Locomotive (
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
                        normaElectricCurrent1 REAL DEFAULT NULL,
                        normaElectricCurrent2 REAL DEFAULT NULL,
                        normaDiesel TEXT DEFAULT NULL,
                        heatingCounterAccepted TEXT DEFAULT NULL,
                        heatingCounterDelivery TEXT DEFAULT NULL,
                        auxiliaryCounterAccepted TEXT DEFAULT NULL,
                        auxiliaryCounterDelivery TEXT DEFAULT NULL,
                        timeBarrierOut INTEGER DEFAULT NULL,
                        timeBarrierIn INTEGER DEFAULT NULL,
                        acceptanceStationId TEXT DEFAULT NULL,
                        deliveryStationId TEXT DEFAULT NULL,
                        FOREIGN KEY (basicId) REFERENCES BasicData(id) ON DELETE CASCADE ON UPDATE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Locomotive_basicId ON Locomotive(basicId)")
            }

            // Добавляем недостающие столбцы (для случаев когда таблицы не пересоздавались).
            // hasTable-проверка: если таблица отсутствует — пропускаем, не кидаем исключение.
            val routeChecks = arrayOf(
                "BasicData" to "timeStartBreak",
                "BasicData" to "timeEndBreak",
                "BasicData" to "timeStartWorkBeforeArrival",
                "Locomotive" to "auxiliaryCounterAccepted",
                "Locomotive" to "auxiliaryCounterDelivery",
                "Locomotive" to "timeBarrierOut",
                "Locomotive" to "timeBarrierIn",
                "Locomotive" to "acceptanceStationId",
                "Locomotive" to "deliveryStationId",
                "Train" to "additionalNumbers",
                "Train" to "pusher",
                "Train" to "doubleTraction",
                "Train" to "doubledTrain",
                "Passenger" to "isWorkStartByArrival"
            )
            for ((table, column) in routeChecks) {
                if (!hasTable(db, table)) continue
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
