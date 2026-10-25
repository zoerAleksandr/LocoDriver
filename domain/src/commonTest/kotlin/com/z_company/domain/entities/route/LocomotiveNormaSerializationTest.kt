package com.z_company.domain.entities.route

import com.z_company.domain.entities.serializers.DoubleAsStringSerializer
import com.z_company.domain.util.sanitizeNumericInput
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Норма тепловоза/электровоза может быть дробной: проверяем, что она уходит
 * на сервер с точкой и без потерь читается обратно (сервер отдаёт число:
 * 12.5 для дробной, 12 для целой).
 */
class LocomotiveNormaSerializationTest {
    // Та же конфигурация, что RemoteRestClient.appJson.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
        serializersModule = SerializersModule { contextual(DoubleAsStringSerializer) }
    }

    @Test
    fun fractionalNormaIsEncodedForServer() {
        val loco = Locomotive(
            basicId = "route",
            normaElectricCurrent1 = 2710.5,
            normaElectricCurrent2 = 300.0,
            normaDiesel = "12.5",
        )
        val encoded = json.encodeToString(loco)
        assertTrue(encoded.contains("\"normaElectricCurrent1\":2710.5"), encoded)
        assertTrue(encoded.contains("\"normaElectricCurrent2\":300"), encoded)
        assertTrue(encoded.contains("\"normaDiesel\":\"12.5\""), encoded)
    }

    @Test
    fun serverNumbersAreDecodedWithoutLoss() {
        val decoded = json.decodeFromString<Locomotive>(
            """{"locoId":"l","basicId":"r","normaElectricCurrent1":2710.5,"normaElectricCurrent2":300,"normaDiesel":12.5}"""
        )
        assertEquals(2710.5, decoded.normaElectricCurrent1)
        assertEquals(300.0, decoded.normaElectricCurrent2)
        assertEquals("12.5", decoded.normaDiesel)

        val integral = json.decodeFromString<Locomotive>(
            """{"locoId":"l","basicId":"r","normaDiesel":12}"""
        )
        assertEquals("12", integral.normaDiesel)
    }

    @Test
    fun normaInputIsNormalizedToDot() {
        assertEquals("12.5", "12,5".sanitizeNumericInput(allowDecimal = true))
        assertEquals("12.", "12.".sanitizeNumericInput(allowDecimal = true))
        assertEquals("12.5", "12.5.3".sanitizeNumericInput(allowDecimal = true))
        assertEquals("", "abc".sanitizeNumericInput(allowDecimal = true))
    }

    @Test
    fun fractionalSectionCountersSurviveServerRoundTrip() {
        // Сервер отдаёт показания числом (3337.77 дробное, 12 целое); на сервер клиент шлёт строкой.
        val decoded = json.decodeFromString<SectionElectric>(
            """{"sectionId":"s","acceptedEnergy":3337.77,"deliveryEnergy":3400,"acceptedRecovery":"12.5"}"""
        )
        assertEquals(3337.77, decoded.acceptedEnergy)
        assertEquals(3400.0, decoded.deliveryEnergy)
        assertEquals(12.5, decoded.acceptedRecovery)
        val encoded = json.encodeToString(decoded)
        assertTrue(encoded.contains("\"acceptedEnergy\":\"3337.77\""), encoded)
        assertTrue(encoded.contains("\"deliveryEnergy\":\"3400\""), encoded)
    }
}
