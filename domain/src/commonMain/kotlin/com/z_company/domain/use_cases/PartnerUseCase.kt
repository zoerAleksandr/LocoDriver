package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.RoutePartner
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

    fun getPermanent(): Partner? = repository.getPermanent()

    /** Сделать напарника постоянным (null — снять отметку). */
    fun setPermanent(partnerId: String?): Flow<ResultState<Unit>> = repository.setPermanent(partnerId)

    /**
     * Новый маршрут (форма, календарь, «Заполнить месяц», копия): добавляет
     * копию постоянного напарника, если он задан и ещё не в маршруте.
     */
    fun withPermanentPartner(route: Route): Route =
        withPermanentPartner(route, repository.getPermanent())

    companion object {
        fun withPermanentPartner(route: Route, permanent: Partner?): Route {
            if (permanent == null) return route
            if (route.partners.any { it.sourcePartnerId == permanent.partnerId }) return route
            val copy = RoutePartner(
                basicId = route.basicData.id,
                sourcePartnerId = permanent.partnerId,
                fullName = permanent.fullName,
                tabNumber = permanent.tabNumber,
                notes = permanent.notes,
            )
            return route.copy(partners = (route.partners + copy).toMutableList())
        }
    }
}
