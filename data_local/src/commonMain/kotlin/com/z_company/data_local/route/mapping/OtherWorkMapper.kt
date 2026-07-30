package com.z_company.data_local.route.mapping

import com.zcompany.datalocal.route.db.OtherWork as OtherWorkRow
import com.z_company.domain.entities.route.OtherWork

internal object OtherWorkMapper {
    fun toData(row: OtherWorkRow): OtherWork = OtherWork(
        otherWorkId = row.otherWorkId,
        basicId = row.basicId,
        remoteObjectId = row.remoteObjectId,
        workType = row.workType,
        timeStart = row.timeStart,
        timeEnd = row.timeEnd,
        station = row.station,
        notes = row.notes
    )
}
