package com.z_company.domain.entities_test

import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.TrainDataVersion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrainDataVersionSerializationTest {

    @Test
    fun historyIsIncludedInServerJson() {
        val train = Train(
            trainId = "train-1",
            weight = "5200",
            axle = "240",
            conditionalLength = "68",
            dataVersions = listOf(
                TrainDataVersion(stationName = "Тверь", weight = "5100")
            )
        )

        val encoded = Json.encodeToString(train)
        val decoded = Json.decodeFromString<Train>(encoded)

        assertTrue(encoded.contains("dataVersions"))
        assertEquals(train.dataVersions, decoded.dataVersions)
        assertEquals("5200", decoded.weight)
    }
}
