package com.z_company.data_local.setting

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.SurchargeExtendedServicePhase
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.repositories.SalarySettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val salaryJson = Json { ignoreUnknownKeys = true; isLenient = true }

class SqlDelightSalarySettingRepository : SalarySettingRepository, KoinComponent {
    private val db: SalarySettingDatabase by inject()

    private fun encodeExtendedPhaseList(list: List<SurchargeExtendedServicePhase>): String =
        salaryJson.encodeToString(list)

    private fun encodeHeavyTrainsList(list: List<SurchargeHeavyTrains>): String =
        salaryJson.encodeToString(list)

    private fun decodeExtendedPhaseList(v: String): List<SurchargeExtendedServicePhase> =
        runCatching { salaryJson.decodeFromString<List<SurchargeExtendedServicePhase>>(v) }.getOrElse { emptyList() }

    private fun decodeHeavyTrainsList(v: String): List<SurchargeHeavyTrains> =
        runCatching { salaryJson.decodeFromString<List<SurchargeHeavyTrains>>(v) }.getOrElse { emptyList() }

    private fun rowToData(row: com.z_company.data_local.setting.salarydb.SalarySetting): SalarySetting =
        SalarySetting(
            key = row.salarySettingKey,
            nightTimePercent = row.nightTimePercent,
            averagePaymentHour = row.averagePaymentHour,
            districtCoefficient = row.districtCoefficient,
            nordicPercent = row.nordicCoefficient,
            zonalSurcharge = row.zonalSurcharge,
            surchargeQualificationClass = row.surchargeQualificationClass,
            harmfulnessPercent = row.harmfulnessPercent,
            onePersonOperationPercent = row.onePersonOperationPercent,
            onePersonOperationPassengerTrainPercent = row.onePersonOperationPassengerTrainPercent,
            surchargeExtendedServicePhaseList = decodeExtendedPhaseList(row.surchargeExtendedServicePhaseList),
            surchargeHeavyTrainsList = decodeHeavyTrainsList(row.surchargeHeavyTrainsList),
            percentLongDistanceTrain = row.surchargeLongTrain,
            lengthLongDistanceTrain = row.lengthLongDistanceTrain.toInt(),
            otherSurcharge = row.otherSurcharge,
            ndfl = row.ndfl,
            unionistsRetention = row.unionistsRetention,
            otherRetention = row.otherRetention
        )

    override fun getSalarySetting(): SalarySetting {
        val row = db.salarySettingQueries.getAll().executeAsOneOrNull()
        return row?.let { rowToData(it) } ?: SalarySetting()
    }

    override fun getSalarySettingState(): Flow<ResultState<SalarySetting>> {
        return ResultState.flowMap {
            db.salarySettingQueries.getAll()
                .asFlow()
                .mapToOneOrNull(Dispatchers.Default)
                .map { row ->
                    ResultState.Success(row?.let { rowToData(it) } ?: SalarySetting())
                }
        }
    }

    override fun saveSalarySetting(setting: SalarySetting): Flow<ResultState<Unit>> {
        return flowRequest {
            db.salarySettingQueries.insertOrReplace(
                salarySettingKey = setting.key,
                nightTimePercent = setting.nightTimePercent,
                zonalSurcharge = setting.zonalSurcharge,
                averagePaymentHour = setting.averagePaymentHour,
                districtCoefficient = setting.districtCoefficient,
                nordicCoefficient = setting.nordicPercent,
                onePersonOperationPercent = setting.onePersonOperationPercent,
                onePersonOperationPassengerTrainPercent = setting.onePersonOperationPassengerTrainPercent,
                harmfulnessPercent = setting.harmfulnessPercent,
                surchargeQualificationClass = setting.surchargeQualificationClass,
                surchargeExtendedServicePhaseList = encodeExtendedPhaseList(setting.surchargeExtendedServicePhaseList),
                surchargeHeavyTrainsList = encodeHeavyTrainsList(setting.surchargeHeavyTrainsList),
                surchargeLongTrain = setting.percentLongDistanceTrain,
                lengthLongDistanceTrain = setting.lengthLongDistanceTrain.toLong(),
                otherSurcharge = setting.otherSurcharge,
                ndfl = setting.ndfl,
                unionistsRetention = setting.unionistsRetention,
                otherRetention = setting.otherRetention
            )
        }
    }

    override fun getSalarySettingFlow(): Flow<SalarySetting> {
        return db.salarySettingQueries.getAll()
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { row -> row?.let { rowToData(it) } ?: SalarySetting() }
    }
}
