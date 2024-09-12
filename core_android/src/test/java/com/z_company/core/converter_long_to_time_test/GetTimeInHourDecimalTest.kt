package com.z_company.core.converter_long_to_time_test

import com.z_company.core.util.ConverterLongToTime
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTimeInHourDecimalTest {
    @Test
    fun getTimeInHourDecimalNormalVer1(){
        val expected = "10,50"
        val testValue = 37_800_000L
        assertEquals(expected, ConverterLongToTime.getTimeInHourDecimal(testValue))
    }
    @Test
    fun getTimeInHourDecimalNormalVer2(){
        val expected = "10,10"
        val testValue = 36_360_000L
        assertEquals(expected, ConverterLongToTime.getTimeInHourDecimal(testValue))
    }
    @Test
    fun getTimeInHourDecimalNormalVer3(){
        val expected = "10,12"
        val testValue = 36_420_000L
        assertEquals(expected, ConverterLongToTime.getTimeInHourDecimal(testValue))
    }
    @Test
    fun getTimeInHourDecimalNullValue(){
        val expected = "0,00"
        val testValue: Long? = null
        assertEquals(expected, ConverterLongToTime.getTimeInHourDecimal(testValue))
    }
    @Test
    fun getTimeInHourDecimalZeroValue(){
        val expected = "0,00"
        val testValue = 0L
        assertEquals(expected, ConverterLongToTime.getTimeInHourDecimal(testValue))
    }
    @Test
    fun getTimeInHourDecimalMiniValue(){
        val expected = "0,00"
        val testValue = 3L
        assertEquals(expected, ConverterLongToTime.getTimeInHourDecimal(testValue))
    }
    @Test
    fun getTimeInHourDecimalOneMinuteValue(){
        val expected = "0,02"
        val testValue = 60_000L
        assertEquals(expected, ConverterLongToTime.getTimeInHourDecimal(testValue))
    }
}