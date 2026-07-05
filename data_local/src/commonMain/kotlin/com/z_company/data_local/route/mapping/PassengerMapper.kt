package com.z_company.data_local.route.mapping

import com.zcompany.datalocal.route.db.Passenger as PassengerRow
import com.z_company.domain.entities.route.Passenger

internal object PassengerMapper {
    fun toData(row: PassengerRow): Passenger = Passenger(
        passengerId = row.passengerId,
        basicId = row.basicId,
        remoteObjectId = row.remoteObjectId,
        trainNumber = row.trainNumber,
        stationDeparture = row.stationDeparture,
        stationArrival = row.stationArrival,
        timeArrival = row.timeArrival,
        timeDeparture = row.timeDeparture,
        notes = row.notes,
        isWorkStartByArrival = row.isWorkStartByArrival != 0L
    )
}
