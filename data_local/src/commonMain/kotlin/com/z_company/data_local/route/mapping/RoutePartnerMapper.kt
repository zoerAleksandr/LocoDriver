package com.z_company.data_local.route.mapping

import com.zcompany.datalocal.route.db.RoutePartner as RoutePartnerRow
import com.z_company.domain.entities.route.RoutePartner

internal object RoutePartnerMapper {
    fun toData(row: RoutePartnerRow): RoutePartner = RoutePartner(
        routePartnerId = row.routePartnerId,
        basicId = row.basicId,
        remoteObjectId = row.remoteObjectId,
        sourcePartnerId = row.sourcePartnerId,
        fullName = row.fullName,
        tabNumber = row.tabNumber,
        notes = row.notes
    )
}
