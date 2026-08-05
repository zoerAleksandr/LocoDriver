package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.RoutePartner
import com.z_company.domain.repositories.RouteRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase для напарников внутри маршрута (вложенный подраздел [RoutePartner]).
 * По образцу [OtherWorkUseCase].
 */
class RoutePartnerUseCase(
    private val repository: RouteRepository
) {
    fun savePartner(partner: RoutePartner): Flow<ResultState<Unit>> {
        return repository.savePartner(partner)
    }

    fun getPartnerById(routePartnerId: String): Flow<ResultState<RoutePartner?>> {
        return repository.loadPartner(routePartnerId)
    }

    fun getPartnerListByBasicId(basicId: String): List<RoutePartner> {
        return repository.loadPartnerListByBasicId(basicId)
    }

    fun removePartner(partner: RoutePartner): Flow<ResultState<Unit>> {
        return repository.removePartner(partner)
    }
}
