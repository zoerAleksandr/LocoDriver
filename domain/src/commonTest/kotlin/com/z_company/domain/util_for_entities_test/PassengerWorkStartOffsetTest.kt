package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.route.Passenger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PassengerWorkStartOffsetTest {
    private val arrival = 1_700_000_000_000L

    @Test
    fun workStartTime_addsOffsetMinutes() {
        val p = Passenger(timeArrival = arrival, workStartOffsetMinutes = 5)
        assertEquals(arrival + 5 * 60_000L, p.workStartTime)
    }

    @Test
    fun workStartTime_zeroOffsetEqualsArrival() {
        assertEquals(arrival, Passenger(timeArrival = arrival).workStartTime)
    }

    @Test
    fun workStartTime_nullWithoutArrival() {
        assertNull(Passenger(workStartOffsetMinutes = 3).workStartTime)
    }
}
