package com.z_company.route.component.servicephase

import kotlin.test.Test
import kotlin.test.assertEquals

class StationNotFoundMessageTest {
    @Test
    fun messageNamesStationAndPointsToSupport() {
        assertEquals(
            "Станция «Подгон» не найдена на карте. Напишите в техническую поддержку — мы добавим её.",
            stationNotFoundMessage("Подгон"),
        )
    }

    @Test
    fun supportButtonAndAddressAreStable() {
        assertEquals("Написать в поддержку", WRITE_TO_SUPPORT)
        assertEquals("locodriver.app@yandex.ru", SUPPORT_EMAIL)
    }
}
