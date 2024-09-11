package com.z_company.domain.util_for_entities_test

import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.trainCategory
import org.junit.Test
import org.mockito.Mock
import kotlin.test.assertEquals

class TrainCategoryTest {

    @Mock
    val trainNormal = Train(number = "2")
    @Test
    fun testNormal() {
        val expected = "Скорые круглогодичные"
        assertEquals(expected, trainNormal.trainCategory())
    }

    @Mock
    val trainEmpty = Train()
    @Test
    fun testEmpty() {
        val expected = "Номер не найден"
        assertEquals(expected, trainEmpty.trainCategory())
    }

    @Mock
    val trainNullNumber = Train(number = null)
    @Test
    fun testNullNumber() {
        val expected = "Номер не найден"
        assertEquals(expected, trainNullNumber.trainCategory())
    }
}