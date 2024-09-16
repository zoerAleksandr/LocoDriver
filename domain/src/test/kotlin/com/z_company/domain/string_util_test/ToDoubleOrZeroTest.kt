package com.z_company.domain.string_util_test

import com.z_company.domain.util.toDoubleOrZero
import org.junit.Test
import kotlin.test.assertEquals

class ToDoubleOrZeroTest {
    @Test
    fun zeroValue(){
        val expected = 0.0
        val value = "0"
        assertEquals(expected, value.toDoubleOrZero())
    }

    @Test
    fun decimalValue(){
        val expected = 279.52
        val value = "279.52"
        assertEquals(expected, value.toDoubleOrZero())
    }

    @Test
    fun integerValue(){
        val expected = 15.0
        val value = "15"
        assertEquals(expected, value.toDoubleOrZero())
    }
    @Test
    fun incorrectValue(){
        val expected = 0.0
        val value = "text"
        assertEquals(expected, value.toDoubleOrZero())
    }
}