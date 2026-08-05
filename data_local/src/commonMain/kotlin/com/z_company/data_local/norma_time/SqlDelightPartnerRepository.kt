@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.data_local.norma_time

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.repositories.PartnerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Справочник напарников в локальной БД (таблица Partner в SettingsDatabase).
 * По образцу [SqlDelightStationNormRepository].
 */
class SqlDelightPartnerRepository : PartnerRepository, KoinComponent {
    private val db: SettingsDatabase by inject()

    override fun getAllFlow(): Flow<List<Partner>> =
        db.partnerQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getAll(): List<Partner> =
        db.partnerQueries.getAll().executeAsList().map { it.toDomain() }

    override fun getById(partnerId: String): Partner? =
        db.partnerQueries.getById(partnerId).executeAsOneOrNull()?.toDomain()

    override fun replaceAll(partners: List<Partner>): Flow<ResultState<Unit>> = flowRequest {
        db.partnerQueries.deleteAll()
        partners.forEach { p ->
            db.partnerQueries.insertOrReplace(
                partnerId = p.partnerId,
                fullName = p.fullName,
                tabNumber = p.tabNumber,
                notes = p.notes,
                updatedAt = p.updatedAt
            )
        }
    }

    override fun upsert(partner: Partner): Flow<ResultState<Unit>> = flowRequest {
        db.partnerQueries.insertOrReplace(
            partnerId = partner.partnerId,
            fullName = partner.fullName,
            tabNumber = partner.tabNumber,
            notes = partner.notes,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    override fun delete(partnerId: String): Flow<ResultState<Unit>> = flowRequest {
        db.partnerQueries.delete(partnerId)
    }

    private fun com.zcompany.datalocal.setting.db.Partner.toDomain(): Partner =
        Partner(
            partnerId = partnerId,
            fullName = fullName,
            tabNumber = tabNumber,
            notes = notes,
            updatedAt = updatedAt
        )
}
