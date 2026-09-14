package com.z_company.route.viewmodel

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.UserSettings
import org.junit.Test
import kotlin.test.assertEquals

class SecondTurnaroundRestTest {
    private val settings = UserSettings(
        minTimeRestPointOfTurnover = 3_600_000L,
        minTimeRestPointOfTurnoverSecond = 14_400_000L,
    )

    private fun route(id: String, start: Long, restInPo: Boolean) = Route(
        basicData = BasicData(id = id, timeStartWork = start, restPointOfTurnover = restInPo)
    )

    @Test fun firstTurnaroundUsesOrdinaryMinimum() {
        val first = route("first", 1L, true)
        assertEquals(3_600_000L, effectiveTurnaroundMinimum(first, listOf(first), settings))
    }

    @Test fun secondConsecutiveTurnaroundUsesNewMinimum() {
        val first = route("first", 1L, true)
        val second = route("second", 2L, true)
        assertEquals(14_400_000L, effectiveTurnaroundMinimum(second, listOf(second, first), settings))
    }

    @Test fun homeRestBreaksTheChain() {
        val first = route("first", 1L, true)
        val home = route("home", 2L, false)
        val last = route("last", 3L, true)
        assertEquals(3_600_000L, effectiveTurnaroundMinimum(last, listOf(last, first, home), settings))
    }
}
