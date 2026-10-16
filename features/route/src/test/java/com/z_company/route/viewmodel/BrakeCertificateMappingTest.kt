package com.z_company.route.viewmodel

import com.z_company.domain.brake.CompositeShare
import com.z_company.domain.brake.DensityLocoGroup
import com.z_company.domain.brake.SlopeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrakeCertificateMappingTest {
    @Test
    fun uiState_toData_toUiState_roundTrip() {
        val src = BrakeCertificateUiState(
            categoryId = "FREIGHT_LOADED", slope = SlopeStep.S012, autoblock3 = false,
            weightText = "3040,5", totalAxlesText = "128", avgPressureText = "8,25", rowsMode = true,
            rows = listOf(BrakeRowUi(0, "8,25", "100"), BrakeRowUi(1, "3,5", "28")), nextRowId = 2,
            compositeShare = CompositeShare.P75, flags = setOf("ЭПТ", "П"), dpvText = "4,8",
            chargePressureText = "5,4", densityGroup = DensityLocoGroup.G4, locoCountText = "2",
            timerII = DensityTimerState(recordedSec = 73.2, grStartText = "8,5", grEndText = "8,0"),
        )
        val ui = src.toData(updatedAt = 5).toUiState(BrakeCertificateUiState())
        assertEquals("3040,5", ui.weightText)
        assertEquals("8,25", ui.avgPressureText)
        assertEquals(SlopeStep.S012, ui.slope)
        assertEquals(2, ui.rows.size)
        assertEquals("3,5", ui.rows[1].pressureText)
        assertEquals(setOf("ЭПТ", "П"), ui.flags)
        assertEquals(CompositeShare.P75, ui.compositeShare)
        assertEquals(DensityLocoGroup.G4, ui.densityGroup)
        assertEquals(73.2, ui.timerII.recordedSec!!, 1e-9)
        assertEquals("8,5", ui.timerII.grStartText)
        assertTrue(!ui.timerII.running)
        assertEquals("2", ui.locoCountText)
    }

    @Test
    fun defaults_compositeIsK100_autoblockOff() {
        assertEquals(com.z_company.domain.brake.CompositeShare.P100, BrakeCertificateViewModel.DEFAULT_COMPOSITE)
        assertEquals(com.z_company.domain.brake.CompositeShare.P100, BrakeCertificateUiState().compositeShare)
        assertEquals(false, BrakeCertificateViewModel.DEFAULT_AUTOBLOCK)
    }

    private fun st(rows: List<Pair<String, String>>, mode: Boolean = true, avg: String = "") = BrakeCertificateUiState(
        rowsMode = mode, avgPressureText = avg,
        rows = rows.mapIndexed { i, (p, a) -> BrakeRowUi(id = i, pressureText = p, axlesText = a) }, nextRowId = rows.size,
    )

    @Test
    fun pressureField_singleRow_oneValue() {
        val s = st(listOf("8,5" to "128"))
        assertEquals(PressureField.Single("8,5"), s.pressureField())
    }

    @Test
    fun pressureField_severalSameRows_single() {
        val s = st(listOf("7" to "100", "7,0" to "28"))
        assertTrue(s.pressureField() is PressureField.Single)
    }

    @Test
    fun pressureField_differentValues_multiple() {
        assertEquals(PressureField.Multiple, st(listOf("7" to "100", "3,5" to "28")).pressureField())
    }

    @Test
    fun pressureField_emptyTable_plainField() {
        assertEquals(PressureField.Empty, st(emptyList()).pressureField())
        assertEquals(PressureField.Empty, st(listOf("" to "")).pressureField())
        assertEquals(PressureField.Empty, st(listOf("8" to ""), mode = false, avg = "8").pressureField())
        assertEquals(PressureField.Empty, st(listOf("8" to ""), mode = true).pressureField()) // нет полностью заполненной строки
    }
}
