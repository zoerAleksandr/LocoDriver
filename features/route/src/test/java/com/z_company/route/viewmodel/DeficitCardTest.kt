package com.z_company.route.viewmodel

import com.z_company.domain.brake.BrakeCertificateCalculator
import com.z_company.domain.brake.BrakeCertificateInput
import com.z_company.domain.brake.AxleRow
import com.z_company.domain.brake.BrakeNorms
import com.z_company.route.ui.deficitFacts
import com.z_company.route.ui.deficitReferenceLines
import kotlin.test.Test
import kotlin.test.assertTrue

class DeficitCardTest {
    @Test fun referenceHasTwoStepsAndNormMinTableFromBrakeNorms() {
        val lines = deficitReferenceLines()
        assertTrue(lines.count { it.startsWith("#") } == 2) // жёлтая и красная; зелёной (норма) нет
        assertTrue(lines.none { it.startsWith("#G") })
        assertTrue(lines.any { it.contains("норма 33, минимум 28") }) // груженый: табл. 1/2 прил. 2
        assertTrue(lines.any { it.contains("норма 55, минимум 50") }) // порожний до 350 осей
        assertTrue(lines.none { it.contains("Что можно сделать") })
    }

    @Test fun factsHaveNumbersAndNoAdvice() {
        val cat = BrakeNorms.category("FREIGHT_LOADED")!!
        val r = BrakeCertificateCalculator.calculate(BrakeCertificateInput(cat, 3000.0, listOf(AxleRow(7.5, 128)))) // факт 960 из 990
        val f = deficitFacts(r)
        assertTrue(f[0].contains("Не хватает 30 тс") && f[0].contains("960") && f[0].contains("990"))
        assertTrue(f[1].startsWith("Скорость: 75 км/ч") || f[1].startsWith("Скорость"))
        assertTrue(f.none { it.contains("отцепить") || it.contains("диспетчер") })
    }
}
