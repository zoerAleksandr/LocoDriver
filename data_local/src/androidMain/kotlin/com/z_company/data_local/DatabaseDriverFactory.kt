package com.z_company.data_local

import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.os.StatFs
import android.system.Os
import android.system.OsConstants
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException

class RouteMigrationLowStorageException(requiredBytes: Long, availableBytes: Long) :
    IllegalStateException("Route migration needs $requiredBytes bytes; $availableBytes available")

class RouteMigrationAlreadyRunningException :
    IllegalStateException("Another Route.db migration is already running")

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createRouteDriver(): SqlDriver = withRouteMigrationLock {
        createRouteDriverLocked()
    }

    private fun createRouteDriverLocked(): SqlDriver {
        var backup: RouteBackup? = null
        return try {
            cleanupDisposableMigrationFiles()
            val interruptedSourceVersion = recoverInterruptedMigrationAttempt()
            backup = prepareRouteBackupIfNeeded()
            val preparedBackup = backup
            if (preparedBackup != null) {
                recordMigrationStage("BACKUP_READY", preparedBackup.sourceVersion)
                migrateCandidateAndSwap(preparedBackup)
            }
            recordMigrationSuccess(preparedBackup)
            createDriver(RouteDatabase.Schema, "Route.db").also {
                val sourceVersion = preparedBackup?.sourceVersion ?: interruptedSourceVersion
                if (sourceVersion != null) recordMigrationStage("SUCCEEDED", sourceVersion)
            }
        } catch (error: Throwable) {
            backup?.let(::restoreRouteBackup)
            recordMigrationFailure(backup, error)
            throw error
        }
    }

    private fun <T> withRouteMigrationLock(block: () -> T): T {
        val lockDir = File(context.filesDir, "data_safety").apply { mkdirs() }
        check(lockDir.isDirectory) { "Cannot create Route.db migration lock directory" }
        val lockFile = File(lockDir, "Route.migration.lock")
        return RandomAccessFile(lockFile, "rw").channel.use { channel ->
            val lock = try {
                channel.tryLock()
            } catch (_: OverlappingFileLockException) {
                null
            } ?: throw RouteMigrationAlreadyRunningException()
            lock.use { block() }
        }
    }

    /** Migrates a disposable copy and replaces Route.db only after validation. */
    private fun migrateCandidateAndSwap(backup: RouteBackup) {
        val dbFile = context.getDatabasePath("Route.db")
        val candidate = File(dbFile.parentFile, "Route.candidate.db")
        File(candidate.path + "-wal").delete()
        File(candidate.path + "-shm").delete()
        recordMigrationStage("CANDIDATE_COPYING", backup.sourceVersion)
        backup.file.copyTo(candidate, overwrite = true)

        try {
            recordMigrationStage("CANDIDATE_MIGRATING", backup.sourceVersion)
            migrateRouteDbIfNeeded(candidate)
            validateMigratedRouteDb(backup, candidate)
            syncFile(candidate)
            recordMigrationStage("CANDIDATE_VALIDATED", backup.sourceVersion)

            // rename(2) replaces a file atomically on the same filesystem. The live
            // database is therefore always either the complete source or candidate.
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            recordMigrationStage("SWAPPING", backup.sourceVersion)
            Os.rename(candidate.path, dbFile.path)
            syncDirectory(dbFile.parentFile)
            recordMigrationStage("SWAPPED", backup.sourceVersion)
        } finally {
            candidate.delete()
            File(candidate.path + "-wal").delete()
            File(candidate.path + "-shm").delete()
        }
    }

    /**
     * A process can disappear between any two migration instructions. The live DB is
     * still authoritative because only a validated candidate is atomically renamed.
     * On the next start, discard only disposable candidate artifacts and retry from
     * the untouched live DB (or accept it if the rename had already completed).
     */
    private fun recoverInterruptedMigrationAttempt(): Int? {
        val preferences = migrationPreferences()
        val stage = preferences.getString(KEY_MIGRATION_STAGE, null) ?: return null
        if (stage in TERMINAL_MIGRATION_STAGES) return null
        val sourceVersion = preferences.getInt(KEY_MIGRATION_FROM, -1).takeIf { it >= 0 }

        preferences.edit()
            .putString(KEY_MIGRATION_STAGE, "INTERRUPTED")
            .putLong(KEY_MIGRATION_UPDATED_AT, System.currentTimeMillis())
            .commit()
        return sourceVersion
    }

    private fun cleanupDisposableMigrationFiles() {
        val dbFile = context.getDatabasePath("Route.db")
        listOf(
            File(dbFile.parentFile, "Route.candidate.db"),
            File(dbFile.parentFile, "Route.restore.tmp"),
            File(context.filesDir, "data_safety/Route.pre_migration.tmp"),
        ).forEach { file ->
            file.delete()
            File(file.path + "-wal").delete()
            File(file.path + "-shm").delete()
        }
    }

    private fun recordMigrationStage(stage: String, sourceVersion: Int) {
        check(
            migrationPreferences().edit()
                .putString(KEY_MIGRATION_STAGE, stage)
                .putInt(KEY_MIGRATION_FROM, sourceVersion)
                .putInt(KEY_MIGRATION_TO, RouteDatabase.Schema.version.toInt())
                .putLong(KEY_MIGRATION_UPDATED_AT, System.currentTimeMillis())
                .commit()
        ) { "Cannot persist Route.db migration stage" }
    }

    private fun recordMigrationFailure(backup: RouteBackup?, error: Throwable) {
        migrationPreferences().edit()
            .putString(KEY_MIGRATION_STAGE, "FAILED")
            .putInt(KEY_MIGRATION_FROM, backup?.sourceVersion ?: -1)
            .putInt(KEY_MIGRATION_TO, RouteDatabase.Schema.version.toInt())
            .putString(KEY_MIGRATION_ERROR, error::class.simpleName ?: "MigrationError")
            .putLong(KEY_MIGRATION_UPDATED_AT, System.currentTimeMillis())
            .commit()
    }

    private fun migrationPreferences() =
        context.getSharedPreferences("data_safety_status", Context.MODE_PRIVATE)

    private fun syncFile(file: File) {
        RandomAccessFile(file, "rw").use { randomAccessFile ->
            randomAccessFile.fd.sync()
        }
    }

    private fun syncDirectory(directory: File?) {
        requireNotNull(directory) { "Route.db directory is missing" }
        val descriptor = Os.open(
            directory.path,
            OsConstants.O_RDONLY,
            0,
        )
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
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
        ensureRegionalHolidayTable("Settings.db")
        ensurePartnerTable("Settings.db")
        fixVersionIfColumnsExist(
            "Settings.db",
            SettingsDatabase.Schema.version.toInt(),
            "UserSettings" to "isShowPartner",
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
            // Найдено через Sentry: "no such column: UserSettings.stationList"
            // (682 события у 9 пользователей). Столбец есть ещё в 1.sqm, но
            // на БД, унаследованных от Room (таблица уже существовала), он не
            // появлялся — тот же случай, что и
            // SalarySetting.onePersonOperationPassengerTrainPercent ниже.
            "UserSettings" to "stationList",
            "UserSettings" to "region",
            "UserSettings" to "isShowTrain",
            "UserSettings" to "isShowOtherWork",
            "UserSettings" to "otherWorkTypeList",
            "UserSettings" to "isShowLocomotive",
            "UserSettings" to "isShowPassenger",
            "MonthOfYear" to "tariffRate",
            "MonthOfYear" to "dateSetTariffRate",
            "ReleaseDay" to "hours",
            "LocomotiveSeries" to "acceptanceHandToHandMin",
            "LocomotiveSeries" to "deliveryHandToHandMin",
            "LocomotiveSeries" to "sectionNumberingType",
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
                    acceptanceHandToHandMin INTEGER,
                    deliveryHandToHandMin INTEGER,
                    sectionNumberingType TEXT NOT NULL DEFAULT 'NUMERIC',
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

    /**
     * Создаёт таблицу Partner (справочник напарников, миграция 16), если её нет.
     * Необходимо для пользователей, обновившихся через fixVersionIfColumnsExist —
     * он выставляет версию сразу в targetVersion, обходя SQLDelight-миграцию 16.sqm.
     */
    private fun ensurePartnerTable(dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS Partner (
                    partnerId TEXT NOT NULL PRIMARY KEY,
                    fullName TEXT NOT NULL,
                    tabNumber TEXT,
                    notes TEXT,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_Partner_fullName ON Partner(fullName)"
            )
        } finally {
            db.close()
        }
    }

    /**
     * Создаёт таблицу RegionalHoliday если её нет. Таблица добавлена в схему позже,
     * а fixVersionIfColumnsExist выставляет версию сразу в targetVersion, из-за чего
     * SQLDelight не создаёт её на уже существующих БД (была ошибка "no such table:
     * RegionalHoliday", и Календарь ходил за праздниками в сеть на каждую загрузку).
     */
    private fun ensureRegionalHolidayTable(dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS RegionalHoliday (
                    region TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    dayOfMonth INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    PRIMARY KEY (region, year, month, dayOfMonth)
                )
            """.trimIndent())
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_regional_holiday_region_year ON RegionalHoliday(region, year)"
            )
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
            "SalarySetting" to "surchargeHeavyLongDistanceTrains",
            // Найдено через Sentry: "no such column: SalarySetting.onePersonOperationPassengerTrainPercent"
            // (~595 случаев). Столбец создаётся только через "CREATE TABLE IF NOT EXISTS" в 1.sqm,
            // поэтому на БД, унаследованных от Room (таблица уже существовала), он не появлялся.
            "SalarySetting" to "onePersonOperationPassengerTrainPercent",
            // Миграция 4: Благосостояние / Алименты
            "SalarySetting" to "welfarePercent",
            "SalarySetting" to "alimonyPercent",
            // Миграция 5: тумблер «Показывать оплаты недоработки»
            "SalarySetting" to "showUnderworkPayments",
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
        private const val KEY_MIGRATION_STAGE = "migration_stage"
        private const val KEY_MIGRATION_FROM = "migration_from"
        private const val KEY_MIGRATION_TO = "migration_to"
        private const val KEY_MIGRATION_ERROR = "migration_error"
        private const val KEY_MIGRATION_UPDATED_AT = "migration_updated_at"
        private val TERMINAL_MIGRATION_STAGES = setOf("SUCCEEDED", "FAILED")

        private val ROUTE_CHILD_TABLES = listOf(
            "Locomotive",
            "Train",
            "Passenger",
            "OtherWork",
            "RoutePartner",
            "Photo",
        )
        private val CORE_ROUTE_TABLES = listOf(
            "BasicData",
            "Locomotive",
            "Train",
            "Passenger",
            "Photo",
        )
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
            "UserSettings.stationList" to ColumnSpec("TEXT", false, "'[]'"),
            "UserSettings.servicePhases" to ColumnSpec("TEXT", false, "'[]'"),
            "UserSettings.region" to ColumnSpec("TEXT", true, "NULL"),
            "UserSettings.isShowTrain" to ColumnSpec("INTEGER", false, "1"),
            "UserSettings.isShowOtherWork" to ColumnSpec("INTEGER", false, "1"),
            "UserSettings.otherWorkTypeList" to ColumnSpec("TEXT", false, "'[]'"),
            "UserSettings.isShowLocomotive" to ColumnSpec("INTEGER", false, "1"),
            "UserSettings.isShowPassenger" to ColumnSpec("INTEGER", false, "1"),
            "UserSettings.isShowPartner" to ColumnSpec("INTEGER", false, "1"),

            "LocomotiveSeries.acceptanceHandToHandMin" to ColumnSpec("INTEGER", true, "NULL"),
            "LocomotiveSeries.deliveryHandToHandMin" to ColumnSpec("INTEGER", true, "NULL"),
            "LocomotiveSeries.sectionNumberingType" to ColumnSpec(
                "TEXT", false, "'NUMERIC'"
            ),
            // Settings — MonthOfYear
            "MonthOfYear.tariffRate" to ColumnSpec("REAL", false, "0.0"),
            "MonthOfYear.dateSetTariffRate" to ColumnSpec("TEXT", true, "NULL"),
            // ReleaseDay — часы «Технических занятий» (миграция 18)
            "ReleaseDay.hours" to ColumnSpec("REAL", true, "NULL"),
            // Route — BasicData
            "BasicData.timeStartBreak" to ColumnSpec("INTEGER", true, "NULL"),
            "BasicData.timeStartWorkBeforeArrival" to ColumnSpec("INTEGER", true, "NULL"),
            "BasicData.timeEndBreak" to ColumnSpec("INTEGER", true, "NULL"),
            "BasicData.deletedAt" to ColumnSpec("INTEGER", true, "NULL"),
            "BasicData.deletionReason" to ColumnSpec("TEXT", true, "NULL"),
            "BasicData.remoteDeletionPending" to ColumnSpec("INTEGER", false, "0"),
            "BasicData.remoteDeletedAt" to ColumnSpec("INTEGER", true, "NULL"),
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
            "Train.carInspector" to ColumnSpec("TEXT", true, "NULL"),
            // Route — Passenger (миграция 9)
            "Passenger.isWorkStartByArrival" to ColumnSpec("INTEGER", false, "0"),
            // SalarySetting
            "SalarySetting.nightTimePercent" to ColumnSpec("REAL", false, "40.0"),
            "SalarySetting.surchargeLongTrainsList" to ColumnSpec("TEXT", false, "'[]'"),
            "SalarySetting.surchargeHeavyLongDistanceTrains" to ColumnSpec("REAL", false, "5.0"),
            "SalarySetting.onePersonOperationPassengerTrainPercent" to ColumnSpec("REAL", false, "50.0"),
            "SalarySetting.welfarePercent" to ColumnSpec("REAL", false, "0.0"),
            "SalarySetting.alimonyPercent" to ColumnSpec("REAL", false, "0.0"),
            "SalarySetting.showUnderworkPayments" to ColumnSpec("INTEGER", false, "1")
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

    /** Android's default corruption handler deletes the database; recovery must preserve it. */
    private fun openRouteDatabase(file: File, flags: Int): SQLiteDatabase =
        SQLiteDatabase.openDatabase(
            file.path,
            null,
            flags,
            DatabaseErrorHandler {
                // Deliberately do nothing. SQLite still reports the failure, while the
                // original bytes remain available for recovery or support export.
            },
        )

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

    private data class RouteBackup(
        val file: File,
        val snapshot: RouteDbSnapshot,
        val sourceVersion: Int,
    )

    private data class RouteDbSnapshot(
        val routeIds: Set<String>,
        val unsynchronizedCount: Long,
        val childCounts: Map<String, Long>,
        val orphanCount: Long,
    ) {
        val routeCount: Long get() = routeIds.size.toLong()
    }

    /** Создаёт и проверяет backup до любого изменения существующей Route.db. */
    private fun prepareRouteBackupIfNeeded(): RouteBackup? {
        val dbFile = context.getDatabasePath("Route.db")
        if (!dbFile.exists()) return null
        val targetVersion = RouteDatabase.Schema.version.toInt()
        var sourceVersion = 0
        var snapshot = RouteDbSnapshot(emptySet(), 0L, emptyMap(), 0L)
        val sourceDb = openRouteDatabase(dbFile, SQLiteDatabase.OPEN_READWRITE)
        try {
            sourceVersion = sourceDb.version
            require(CORE_ROUTE_TABLES.all { hasTable(sourceDb, it) }) {
                "Route.db is missing a required table"
            }
            val requiresStructuralRepair =
                !hasColumn(sourceDb, "BasicData", "remoteDeletionPending") ||
                    !hasTable(sourceDb, "RouteEvent") ||
                    !hasTable(sourceDb, "DiagnosticOutbox")
            if (sourceVersion >= targetVersion && !requiresStructuralRepair) return null
            sourceDb.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
            snapshot = routeSnapshot(sourceDb)
        } finally {
            sourceDb.close()
        }

        val availableBytes = StatFs(dbFile.parentFile?.path ?: context.filesDir.path).availableBytes
        val requiredBytes = RouteMigrationStoragePolicy.requiredAvailableBytes(dbFile.length())
        if (availableBytes < requiredBytes) {
            throw RouteMigrationLowStorageException(requiredBytes, availableBytes)
        }

        val backupDir = File(context.filesDir, "data_safety").apply { mkdirs() }
        require(backupDir.isDirectory) { "Cannot create Route.db backup directory" }
        val temp = File(backupDir, "Route.pre_migration.tmp")
        val backupFile = File(backupDir, "Route.pre_migration.db")
        dbFile.copyTo(temp, overwrite = true)
        validateBackupFile(temp, snapshot)
        syncFile(temp)
        // rename(2) replaces the previous backup atomically. A process death before
        // this instruction leaves the old backup intact; after it, the new one is complete.
        Os.rename(temp.path, backupFile.path)
        syncDirectory(backupFile.parentFile)
        return RouteBackup(backupFile, snapshot, sourceVersion)
    }

    private fun validateBackupFile(file: File, expectedSnapshot: RouteDbSnapshot) {
        val backupDb = openRouteDatabase(file, SQLiteDatabase.OPEN_READONLY)
        try {
            val integrity = backupDb.rawQuery("PRAGMA quick_check", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "missing_result"
            }
            require(integrity.equals("ok", ignoreCase = true)) { "Route.db backup integrity check failed" }
            require(routeSnapshot(backupDb) == expectedSnapshot) { "Route.db backup snapshot mismatch" }
        } finally {
            backupDb.close()
        }
    }

    private fun validateMigratedRouteDb(backup: RouteBackup, dbFile: File) {
        val migrated = openRouteDatabase(dbFile, SQLiteDatabase.OPEN_READONLY)
        try {
            require(migrated.version == RouteDatabase.Schema.version.toInt()) {
                "Route.db target version was not applied"
            }
            require(hasColumn(migrated, "BasicData", "remoteDeletionPending")) {
                "Route.db trash migration is incomplete"
            }
            require(hasTable(migrated, "RouteEvent") && hasTable(migrated, "DiagnosticOutbox")) {
                "Route.db diagnostic tables are missing"
            }
            val migratedSnapshot = routeSnapshot(migrated)
            require(migratedSnapshot.routeIds == backup.snapshot.routeIds) {
                "Route.db route identifiers changed during migration"
            }
            require(migratedSnapshot.unsynchronizedCount == backup.snapshot.unsynchronizedCount) {
                "Route.db unsynchronized route count changed during migration"
            }
            require(migratedSnapshot.childCounts == backup.snapshot.childCounts) {
                "Route.db child row counts changed during migration"
            }
            require(migratedSnapshot.orphanCount <= backup.snapshot.orphanCount) {
                "Route.db orphan child count increased during migration"
            }
        } finally {
            migrated.close()
        }
    }

    private fun restoreRouteBackup(backup: RouteBackup) {
        val dbFile = context.getDatabasePath("Route.db")
        val backupDir = backup.file.parentFile ?: context.filesDir
        if (dbFile.exists()) {
            dbFile.copyTo(
                File(backupDir, "Route.failed_migration_${System.currentTimeMillis()}.db"),
                overwrite = true,
            )
        }
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        val restoreTemp = File(dbFile.parentFile, "Route.restore.tmp")
        try {
            backup.file.copyTo(restoreTemp, overwrite = true)
            validateBackupFile(restoreTemp, backup.snapshot)
            syncFile(restoreTemp)
            // Never copy directly over Route.db: interruption during copy must leave
            // either the complete current DB or the complete validated backup.
            Os.rename(restoreTemp.path, dbFile.path)
            syncDirectory(dbFile.parentFile)
        } finally {
            restoreTemp.delete()
        }
        context.getSharedPreferences("data_safety_status", Context.MODE_PRIVATE)
            .edit()
            .putString("last_migration_status", "ROLLED_BACK")
            .putInt("last_migration_from", backup.sourceVersion)
            .putInt("last_migration_to", RouteDatabase.Schema.version.toInt())
            .apply()
    }

    private fun recordMigrationSuccess(backup: RouteBackup?) {
        if (backup == null) return
        val dbFile = context.getDatabasePath("Route.db")
        val db = openRouteDatabase(dbFile, SQLiteDatabase.OPEN_READWRITE)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS MigrationStatus (
                    singletonId INTEGER NOT NULL PRIMARY KEY CHECK (singletonId = 1),
                    status TEXT NOT NULL,
                    fromVersion INTEGER NOT NULL,
                    toVersion INTEGER NOT NULL,
                    routesBefore INTEGER NOT NULL,
                    routesAfter INTEGER NOT NULL,
                    backupCreated INTEGER NOT NULL,
                    errorCode TEXT,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL(
                """INSERT OR REPLACE INTO MigrationStatus(
                    singletonId, status, fromVersion, toVersion, routesBefore, routesAfter,
                    backupCreated, errorCode, updatedAt
                ) VALUES (1, ?, ?, ?, ?, ?, 1, NULL, ?)""".trimIndent(),
                arrayOf<Any>(
                    "SUCCEEDED",
                    backup.sourceVersion,
                    RouteDatabase.Schema.version.toInt(),
                    backup.snapshot.routeCount,
                    countRows(db, "BasicData"),
                    System.currentTimeMillis(),
                ),
            )
        } finally {
            db.close()
        }
        context.getSharedPreferences("data_safety_status", Context.MODE_PRIVATE)
            .edit()
            .putString("last_migration_status", "SUCCEEDED")
            .putInt("last_migration_from", backup.sourceVersion)
            .putInt("last_migration_to", RouteDatabase.Schema.version.toInt())
            .apply()
    }

    private fun countRows(db: SQLiteDatabase, table: String): Long {
        if (!hasTable(db, table)) return 0L
        return db.rawQuery("SELECT count(*) FROM $table", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

    private fun routeSnapshot(db: SQLiteDatabase): RouteDbSnapshot {
        val routeIds = if (hasTable(db, "BasicData")) {
            db.rawQuery("SELECT id FROM BasicData", null).use { cursor ->
                buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
        } else emptySet()
        val unsynchronizedCount =
            if (hasTable(db, "BasicData") && hasColumn(db, "BasicData", "isSynchronized")) {
                db.rawQuery(
                    "SELECT count(*) FROM BasicData WHERE isSynchronized = 0",
                    null,
                ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
            } else 0L
        return RouteDbSnapshot(
            routeIds = routeIds,
            unsynchronizedCount = unsynchronizedCount,
            childCounts = ROUTE_CHILD_TABLES.associateWith { countRows(db, it) },
            orphanCount = countOrphans(db),
        )
    }

    private fun countOrphans(db: SQLiteDatabase): Long = ROUTE_CHILD_TABLES.sumOf { table ->
        if (!hasTable(db, table) || !hasColumn(db, table, "basicId")) 0L
        else db.rawQuery(
            "SELECT count(*) FROM $table child LEFT JOIN BasicData parent " +
                "ON parent.id = child.basicId WHERE parent.id IS NULL",
            null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
    }

    /**
     * Route.db: пересоздаёт Train и Locomotive при миграции с Room (v14+),
     * добавляет недостающие столбцы, выставляет целевую версию.
     */
    private fun migrateRouteDbIfNeeded(dbFile: File) {
        val targetVersion = RouteDatabase.Schema.version.toInt()
        val db = openRouteDatabase(dbFile, SQLiteDatabase.OPEN_READWRITE)
        try {
            // Candidate must be self-contained before the atomic rename. In WAL mode
            // committed pages could otherwise remain in a sidecar file.
            db.rawQuery("PRAGMA journal_mode=DELETE", null).use { it.moveToFirst() }
            // Room → SQLDelight: пересоздаём таблицы с несовместимой схемой
            val needsTrainRecreate = hasColumn(db, "Train", "remoteObjectId")
            val needsLocoRecreate = hasColumn(db, "Locomotive", "removeObjectId")

            if (needsTrainRecreate) {
                // Добавляем недостающие колонки в старую таблицу ПЕРЕД копированием
                val trainNewColumns = arrayOf(
                    "additionalNumbers" to "TEXT DEFAULT NULL",
                    // Room v1 predates distance; an empty string is the current
                    // domain default and preserves the legacy string contract.
                    "distance" to "TEXT NOT NULL DEFAULT ''",
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
                        stations TEXT NOT NULL,
                        servicePhase TEXT DEFAULT NULL,
                        pusher TEXT DEFAULT NULL,
                        doubleTraction TEXT DEFAULT NULL,
                        doubledTrain TEXT DEFAULT NULL,
                        FOREIGN KEY (basicId) REFERENCES BasicData(id) ON DELETE CASCADE ON UPDATE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO Train_new (trainId, basicId, number, additionalNumbers, distance, weight, axle, conditionalLength, stations, servicePhase, pusher, doubleTraction, doubledTrain)
                    SELECT trainId, basicId, number, additionalNumbers, distance, weight, axle, conditionalLength, stations, servicePhase, pusher, doubleTraction, doubledTrain
                    FROM Train
                """.trimIndent())
                db.execSQL("DROP TABLE Train")
                db.execSQL("ALTER TABLE Train_new RENAME TO Train")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Train_basicId ON Train(basicId)")
            }

            if (needsLocoRecreate) {
                // Добавляем недостающие колонки в старую таблицу ПЕРЕД копированием.
                // На очень старых Room-схемах (найдено через Sentry: "no such column:
                // normaElectricCurrent1") часть этих столбцов ещё не существовала —
                // INSERT...SELECT ниже падал с SQLiteException, а RouteDatabase не
                // создавалась вовсе (InstanceCreationException, ~1000+ случаев).
                val locoNewColumns = arrayOf(
                    "auxiliaryCounterAccepted" to "TEXT DEFAULT NULL",
                    "auxiliaryCounterDelivery" to "TEXT DEFAULT NULL",
                    "normaElectricCurrent1" to "REAL DEFAULT NULL",
                    "normaElectricCurrent2" to "REAL DEFAULT NULL",
                    "normaDiesel" to "TEXT DEFAULT NULL",
                    "heatingCounterAccepted" to "TEXT DEFAULT NULL",
                    "heatingCounterDelivery" to "TEXT DEFAULT NULL",
                    "timeBarrierOut" to "INTEGER DEFAULT NULL",
                    "timeBarrierIn" to "INTEGER DEFAULT NULL",
                    "acceptanceStationId" to "TEXT DEFAULT NULL",
                    "deliveryStationId" to "TEXT DEFAULT NULL"
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

            // Создаём OtherWork («прочая работа», миграция 11), если её ещё нет —
            // на случай форс-бампа версии в обход .sqm.
            if (!hasTable(db, "OtherWork")) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS OtherWork (
                        otherWorkId TEXT NOT NULL PRIMARY KEY,
                        basicId TEXT NOT NULL,
                        remoteObjectId TEXT,
                        workType TEXT,
                        timeStart INTEGER,
                        timeEnd INTEGER,
                        station TEXT,
                        notes TEXT,
                        FOREIGN KEY (basicId) REFERENCES BasicData(id) ON DELETE CASCADE ON UPDATE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_OtherWork_basicId ON OtherWork(basicId)")
            }

            // Создаём RoutePartner («напарники» в маршруте, миграция 12), если её ещё нет —
            // на случай форс-бампа версии в обход .sqm.
            if (!hasTable(db, "RoutePartner")) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS RoutePartner (
                        routePartnerId TEXT NOT NULL PRIMARY KEY,
                        basicId TEXT NOT NULL,
                        remoteObjectId TEXT,
                        sourcePartnerId TEXT,
                        fullName TEXT,
                        tabNumber TEXT,
                        notes TEXT,
                        FOREIGN KEY (basicId) REFERENCES BasicData(id) ON DELETE CASCADE ON UPDATE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_RoutePartner_basicId ON RoutePartner(basicId)")
            }

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS DiagnosticInstallation (
                    singletonId INTEGER NOT NULL PRIMARY KEY CHECK (singletonId = 1),
                    installationId TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS RouteEvent (
                    eventId TEXT NOT NULL PRIMARY KEY,
                    installationId TEXT NOT NULL,
                    routeIdHash TEXT,
                    eventType TEXT NOT NULL,
                    reasonCode TEXT,
                    createdAt INTEGER NOT NULL,
                    appVersion TEXT,
                    appBuild INTEGER,
                    dbVersion INTEGER,
                    detailsJson TEXT,
                    uploadedAt INTEGER
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_RouteEvent_createdAt ON RouteEvent(createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_RouteEvent_uploadedAt ON RouteEvent(uploadedAt)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS DiagnosticOutbox (
                    eventId TEXT NOT NULL PRIMARY KEY,
                    createdAt INTEGER NOT NULL,
                    attemptCount INTEGER NOT NULL DEFAULT 0,
                    nextAttemptAt INTEGER NOT NULL,
                    lastErrorCode TEXT,
                    FOREIGN KEY (eventId) REFERENCES RouteEvent(eventId) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS MigrationStatus (
                    singletonId INTEGER NOT NULL PRIMARY KEY CHECK (singletonId = 1),
                    status TEXT NOT NULL,
                    fromVersion INTEGER NOT NULL,
                    toVersion INTEGER NOT NULL,
                    routesBefore INTEGER NOT NULL,
                    routesAfter INTEGER NOT NULL,
                    backupCreated INTEGER NOT NULL,
                    errorCode TEXT,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())

            // Добавляем недостающие столбцы (для случаев когда таблицы не пересоздавались).
            // hasTable-проверка: если таблица отсутствует — пропускаем, не кидаем исключение.
            val routeChecks = arrayOf(
                "BasicData" to "timeStartBreak",
                "BasicData" to "timeEndBreak",
                "BasicData" to "timeStartWorkBeforeArrival",
                "BasicData" to "deletedAt",
                "BasicData" to "deletionReason",
                "BasicData" to "remoteDeletionPending",
                "BasicData" to "remoteDeletedAt",
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
                "Train" to "carInspector",
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
