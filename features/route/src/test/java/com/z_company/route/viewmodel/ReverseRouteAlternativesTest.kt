package com.z_company.route.viewmodel

import com.z_company.repository.remote_rest.response.RouteLengthResponse
import com.z_company.repository.remote_rest.response.RouteVariantDto
import com.z_company.repository.remote_rest.response.RouteViaStationDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ReverseRouteAlternativesTest {
    @Test
    fun `uses richer reverse result and restores requested direction`() {
        val forward = RouteLengthResponse(lengthKm = 10.0, kmAt = listOf(0.0, 10.0))
        val reverse = RouteLengthResponse(
            lengthKm = 10.0,
            kmAt = listOf(0.0, 10.0),
            geometry = listOf(listOf(2.0, 2.0), listOf(1.0, 1.0)),
            routes = listOf(
                RouteVariantDto(
                    id = "r1",
                    lengthKm = 10.0,
                    geometry = listOf(listOf(2.0, 2.0), listOf(1.0, 1.0)),
                    viaStations = listOf(RouteViaStationDto("Б", 8.0), RouteViaStationDto("А", 3.0)),
                ),
                RouteVariantDto(id = "r2", lengthKm = 12.0),
            ),
        )

        val result = preferReverseAlternatives(forward, reverse)

        assertEquals(listOf(listOf(1.0, 1.0), listOf(2.0, 2.0)), result.geometry)
        assertEquals(listOf("А", "Б"), result.routes.first().viaStations.map { it.name })
        assertEquals(listOf(7.0, 2.0), result.routes.first().viaStations.map { it.km })
        assertEquals(listOf(0.0, 10.0), result.kmAt)
    }

    @Test
    fun `keeps forward result when reverse is not richer`() {
        val forward = RouteLengthResponse(
            lengthKm = 10.0,
            routes = listOf(RouteVariantDto(id = "forward", lengthKm = 10.0)),
        )
        val reverse = RouteLengthResponse(
            lengthKm = 10.0,
            routes = listOf(RouteVariantDto(id = "reverse", lengthKm = 10.0)),
        )

        assertEquals(forward, preferReverseAlternatives(forward, reverse))
    }
}
