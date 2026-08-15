package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInHeavyLongDistanceTrain
import kotlin.test.Test
import kotlin.test.assertEquals

class HeavyLongDistanceTrainSurchargeTest {
    private val hour = 3_600_000L

    private fun route(weight: String?, axle: String?): Route = Route(
        basicData = BasicData(timeStartWork = 0L, timeEndWork = 10 * hour),
        trains = mutableListOf(Train(weight = weight, axle = axle))
    )

    @Test
    fun `accrues when weight is over 6000 and axles are at least 350`() {
        assertEquals(10 * hour, route("6001", "350").getTimeInHeavyLongDistanceTrain())
        assertEquals(10 * hour, route("6000.5", "350").getTimeInHeavyLongDistanceTrain())
    }

    @Test
    fun `does not accrue at exactly 6000 tonnes`() {
        assertEquals(0L, route("6000", "350").getTimeInHeavyLongDistanceTrain())
    }

    @Test
    fun `does not accrue below 350 axles`() {
        assertEquals(0L, route("7000", "349").getTimeInHeavyLongDistanceTrain())
    }

    @Test
    fun `does not accrue for missing or invalid train data`() {
        assertEquals(0L, route(null, "350").getTimeInHeavyLongDistanceTrain())
        assertEquals(0L, route("7000", "invalid").getTimeInHeavyLongDistanceTrain())
    }
}
