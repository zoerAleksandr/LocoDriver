package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegWarningsTest {
    private val leg = listOf("Лужская", "Толмачёво", "Луга")

    @Test fun stationOnLeg() = assertTrue(LegWarnings.matches("Толмачёво", leg))
    @Test fun stationOffLeg() = assertFalse(LegWarnings.matches("Будогощь", leg))
    @Test fun spanSameDirection() = assertTrue(LegWarnings.matches("Лужская — Толмачёво", leg))
    @Test fun spanFurtherOnLeg() = assertTrue(LegWarnings.matches("Толмачёво — Луга", leg))
    @Test fun spanOppositeDirection() = assertFalse(LegWarnings.matches("Луга — Толмачёво", leg))
    @Test fun spanLeavingLeg() = assertFalse(LegWarnings.matches("Лужская — Будогощь", leg))
    @Test fun blankPlace() = assertFalse(LegWarnings.matches("", leg))
}
