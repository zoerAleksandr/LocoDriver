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

    private fun createDriver(
        schema: SqlSchema<QueryResult.Value<Unit>>,
        name: String
    ): SqlDriver = AndroidSqliteDriver(
        schema = schema,
        context = context,
        name = name,
        callback = object : AndroidSqliteDriver.Callback(schema) {
            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
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
