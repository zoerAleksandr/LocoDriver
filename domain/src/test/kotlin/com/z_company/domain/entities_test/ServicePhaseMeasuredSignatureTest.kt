package com.z_company.domain.entities_test

import com.z_company.domain.entities.setting.ServicePhase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServicePhaseMeasuredSignatureTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun oldJsonWithoutFieldDecodesToNull() {
        val old = """{"id":"p1","departureStation":"Гатчина","arrivalStation":"Луга","distance":96}"""
        assertNull(json.decodeFromString<ServicePhase>(old).measuredSignature)
    }

    @Test
    fun signatureSurvivesRoundTrip() {
        val phase = ServicePhase(
            id = "p1", departureStation = "Гатчина", arrivalStation = "Луга", distance = 96,
            measuredSignature = "96|59.57000,30.12000;58.73000,29.85000",
        )
        assertEquals(phase, json.decodeFromString<ServicePhase>(json.encodeToString(phase)))
    }

    @Test
    fun tripCopyAndReversedDoNotCarrySignature() {
        val phase = ServicePhase(
            id = "p1", departureStation = "Гатчина", arrivalStation = "Луга", distance = 96,
            measuredSignature = "96|a;b",
        )
        assertNull(phase.forTrain().measuredSignature)
        assertNull(phase.reversed().measuredSignature)
    }
}
