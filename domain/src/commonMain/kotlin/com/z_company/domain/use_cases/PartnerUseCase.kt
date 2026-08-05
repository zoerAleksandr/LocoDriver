package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.repositories.PartnerRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase справочника напарников (Настройки → Справочники → Напарники).
 * По образцу использования [StationNormRepository]/[LocomotiveSeriesRepository].
 */
class PartnerUseCase(
    private val repository: PartnerRepository
) {
    fun getAllFlow(): Flow<List<Partner>> = repository.getAllFlow()

    fun getAll(): List<Partner> = repository.getAll()

    fun getById(partnerId: String): Partner? = repository.getById(partnerId)

    fun upsert(partner: Partner): Flow<ResultState<Unit>> = repository.upsert(partner)

    fun delete(partnerId: String): Flow<ResultState<Unit>> = repository.delete(partnerId)

    fun replaceAll(partners: List<Partner>): Flow<ResultState<Unit>> = repository.replaceAll(partners)
}
