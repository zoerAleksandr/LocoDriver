package com.z_company.domain.entities.setting

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserSettingsSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun passengerWagonLengthIsIncludedInJsonAndRestored() {
        val encoded = json.encodeToString(
            UserSettings.serializer(),
            UserSettings(passengerWagonLengthMeters = 25.7),
        )

        assertTrue(encoded.contains("\"passengerWagonLengthMeters\":25.7"))
        assertEquals(
            25.7,
            json.decodeFromString(UserSettings.serializer(), encoded).passengerWagonLengthMeters,
        )
    }

    @Test
    fun missingPassengerWagonLengthUsesBackwardCompatibleDefault() {
        val decoded = json.decodeFromString(UserSettings.serializer(), "{}")

        assertEquals(24.5, decoded.passengerWagonLengthMeters)
    }
}
