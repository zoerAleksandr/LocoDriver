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
        fixVersionIfColumnsExist("Route.db", RouteDatabase.Schema.version.toInt(),
            "BasicData" to "timeStartBreak")
        return createDriver(RouteDatabase.Schema, "Route.db")
    }

    actual fun createSettingsDriver(): SqlDriver {
        fixVersionIfColumnsExist("Settings.db", SettingsDatabase.Schema.version.toInt(),
            "UserSettings" to "isShowBreak")
        return createDriver(SettingsDatabase.Schema, "Settings.db")
    }

    actual fun createSalarySettingDriver(): SqlDriver =
        createDriver(SalarySettingDatabase.Schema, "SalarySetting.db")

    actual fun createSearchResponseDriver(): SqlDriver =
        createDriver(SearchResponseDatabase.Schema, "SearchResponse.db")

    /**
     * После даунгрейда (v4→v3) onDowngrade не удаляет столбцы —
     * SQLite не поддерживает DROP COLUMN на старых API.
     * Версия БД понижается, но столбцы остаются.
     * При повторном апгрейде ALTER TABLE ADD COLUMN падает с "duplicate column".
     *
     * Фикс: перед созданием драйвера проверяем — если столбец уже существует,
     * а версия ниже целевой, выставляем целевую версию, чтобы миграция не запускалась.
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
            if (db.version in 1..<targetVersion) {
                val allExist = checks.all { (table, column) -> hasColumn(db, table, column) }
                if (allExist) {
                    db.version = targetVersion
                }
            }
        } finally {
            db.close()
        }
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

    private fun createDriver(
        schema: SqlSchema<QueryResult.Value<Unit>>,
        name: String
    ): SqlDriver = AndroidSqliteDriver(
        schema = schema,
        context = context,
        name = name,
        callback = object : AndroidSqliteDriver.Callback(schema) {
            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                if (oldVersion < 14) {
                    // SQLDelight → SQLDelight даунгрейд (например v4→v3).
                    // Лишние столбцы (timeStartBreak и т.д.) безвредны — SQLite их игнорирует.
                    return
                }
                // Room использовал version 14+, SQLDelight начинает с version 1-3.
                // Пересоздаём таблицы Train (без remoteObjectId) и Locomotive (nullable removeObjectId).
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

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS Locomotive_new (
                        locoId TEXT NOT NULL PRIMARY KEY,
                        basicId TEXT NOT NULL,
                        removeObjectId TEXT DEFAULT NULL,
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
                        FOREIGN KEY (basicId) REFERENCES BasicData(id) ON DELETE CASCADE ON UPDATE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO Locomotive_new (locoId, basicId, removeObjectId, series, number, type, electricSectionList, dieselSectionList, timeStartOfAcceptance, timeEndOfAcceptance, timeStartOfDelivery, timeEndOfDelivery, normaElectricCurrent1, normaElectricCurrent2, normaDiesel, heatingCounterAccepted, heatingCounterDelivery)
                    SELECT locoId, basicId, CASE WHEN removeObjectId = '' THEN NULL ELSE removeObjectId END, series, number, type, electricSectionList, dieselSectionList, timeStartOfAcceptance, timeEndOfAcceptance, timeStartOfDelivery, timeEndOfDelivery, normaElectricCurrent1, normaElectricCurrent2, normaDiesel, heatingCounterAccepted, heatingCounterDelivery
                    FROM Locomotive
                """.trimIndent())
                db.execSQL("DROP TABLE Locomotive")
                db.execSQL("ALTER TABLE Locomotive_new RENAME TO Locomotive")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Locomotive_basicId ON Locomotive(basicId)")
            }
        }
    )
}
