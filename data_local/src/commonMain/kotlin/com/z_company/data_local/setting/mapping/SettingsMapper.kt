package com.z_company.data_local.setting.mapping

import com.zcompany.datalocal.setting.db.UserSettings as UserSettingsRow
import com.zcompany.datalocal.setting.db.MonthOfYear as MonthOfYearRow
import com.z_company.domain.entities.DateSetTariffRate
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.CrossMonthTimezone
import com.z_company.domain.entities.setting.NightTime
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.route.LocoType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val settingsJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

internal object SettingsMapper {

    fun encodeNightTime(nt: NightTime): String = settingsJson.encodeToString(nt)
    fun encodeLocoType(lt: LocoType): String = lt.name
    fun encodeMonthOfYear(m: MonthOfYear): String = settingsJson.encodeToString(m)
    fun encodeStringList(list: List<String>): String = settingsJson.encodeToString(list)
    fun encodeLongList(list: List<Long>): String = settingsJson.encodeToString(list)
    fun encodeServicePhaseList(list: List<ServicePhase>): String = settingsJson.encodeToString(list)

    private fun decodeNightTime(v: String): NightTime =
        runCatching { settingsJson.decodeFromString<NightTime>(v) }.getOrElse { NightTime() }

    private fun decodeLocoType(v: String): LocoType =
        runCatching { LocoType.valueOf(v) }.getOrElse { LocoType.ELECTRIC }

    private fun decodeMonthOfYear(v: String): MonthOfYear =
        runCatching { settingsJson.decodeFromString<MonthOfYear>(v) }.getOrElse { MonthOfYear() }

    private fun decodeStringList(v: String): List<String> =
        runCatching { settingsJson.decodeFromString<List<String>>(v) }.getOrElse { emptyList() }

    private fun decodeLongList(v: String): List<Long> =
        runCatching {
            settingsJson.decodeFromString<List<Long>>(v.replace("_", ""))
        }.getOrElse { listOf(8 * 3_600_000L, 20 * 3_600_000L) }

    private fun decodeServicePhaseList(v: String): List<ServicePhase> =
        runCatching { settingsJson.decodeFromString<List<ServicePhase>>(v) }.getOrElse { emptyList() }

    fun toData(row: UserSettingsRow): UserSettings = UserSettings(
        key = row.settingsKey,
        minTimeRestPointOfTurnover = row.minTimeRest,
        minTimeHomeRest = row.minTimeHomeRest,
        lastEnteredDieselCoefficient = row.lastEnteredDieselCoefficient,
        nightTime = decodeNightTime(row.nightTime),
        defaultLocoType = decodeLocoType(row.defaultLocoType),
        defaultWorkTime = row.defaultWorkTime,
        usingDefaultWorkTime = row.usingDefaultWorkTime != 0L,
        isConsiderFutureRoute = row.isConsiderFutureRoute != 0L,
        updateAt = row.updateAt,
        selectMonthOfYear = decodeMonthOfYear(row.monthOfYear),
        stationList = decodeStringList(row.stationList),
        timeZone = row.timeZone,
        locomotiveSeriesList = runCatching { decodeStringList(row.locomotiveSeriesList) }.getOrElse { emptyList() },
        servicePhases = runCatching { decodeServicePhaseList(row.servicePhases) }.getOrElse { emptyList() },
        standardTimesStartWork = runCatching { decodeLongList(row.standardTimesStartWork) }.getOrElse { listOf(8 * 3_600_000L, 20 * 3_600_000L) },
        subscriptionPeriod = row.subscriptionPeriod,
        isDecimalTime = row.isDecimalTime != 0L,
        isShowBreak = row.isShowBreak != 0L,
        isShowOnePersonSwitch = row.isShowOnePersonSwitch != 0L,
        isShowLocoHeating = row.isShowLocoHeating != 0L,
        isShowLocoAuxiliary = row.isShowLocoAuxiliary != 0L,
        isShowLocoStatistics = row.isShowLocoStatistics != 0L,
        isShowLocoNorma = row.isShowLocoNorma != 0L,
        isShowOtherCurrent = row.isShowOtherCurrent != 0L,
        country = row.country,
        crossMonthTimezone = runCatching {
            CrossMonthTimezone.valueOf(row.crossMonthTimezone)
        }.getOrElse { CrossMonthTimezone.LOCAL },
        useStandardTimePicker = row.useStandardTimePicker != 0L,
        region = row.region,
    )
}

internal object MonthOfYearMapper {

    fun encodeDays(days: List<Day>): String = settingsJson.encodeToString(days)
    fun encodeDateSetTariffRate(d: DateSetTariffRate?): String? = d?.let { settingsJson.encodeToString(it) }

    private fun decodeDays(v: String): List<Day> =
        runCatching { settingsJson.decodeFromString<List<Day>>(v) }.getOrElse { emptyList() }

    private fun decodeDateSetTariffRate(v: String?): DateSetTariffRate? =
        v?.let { runCatching { settingsJson.decodeFromString<DateSetTariffRate>(it) }.getOrNull() }

    fun toData(row: MonthOfYearRow): MonthOfYear = MonthOfYear(
        id = row.id,
        year = row.year.toInt(),
        month = row.month.toInt(),
        days = decodeDays(row.days),
        tariffRate = row.tariffRate,
        dateSetTariffRate = decodeDateSetTariffRate(row.dateSetTariffRate)
    )
}
