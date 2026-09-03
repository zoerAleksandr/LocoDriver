package com.z_company.route.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class SalaryPartialStateMergeTest {
    @Test
    fun `empty calculation part cannot erase month header`() {
        val month = mergeNonEmptyText("Январь", "")

        assertEquals("Январь", month)
    }

    @Test
    fun `non-empty calculation part replaces previous header`() {
        val month = mergeNonEmptyText("Январь", "Февраль")

        assertEquals("Февраль", month)
    }
}
