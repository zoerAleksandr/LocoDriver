package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class AddAllOrSkipTest {
    @Test
    fun addsOnlyMissingKeepingOrderAndSkippingInnerDuplicates() {
        val target = mutableListOf("a", "b")
        target.addAllOrSkip(mutableListOf("b", "c", "c", "a", "d"))
        assertEquals(listOf("a", "b", "c", "d"), target)
    }

    @Test
    fun nullAndEmptyAreNoOps() {
        val target = mutableListOf("a")
        target.addAllOrSkip(null)
        target.addAllOrSkip(emptyList())
        assertEquals(listOf("a"), target)
    }

    @Test
    fun largeDictionaryIsLinear() {
        val target = MutableList(20_000) { "st$it" }
        val incoming = List(20_000) { "st${it + 10_000}" }
        target.addAllOrSkip(incoming)
        assertEquals(30_000, target.size)
        assertEquals("st29999", target.last())
    }
}
