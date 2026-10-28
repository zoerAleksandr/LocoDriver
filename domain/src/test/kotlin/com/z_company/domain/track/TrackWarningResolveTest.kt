package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackWarningResolveTest {
    private fun section() = SectionProfile(
        id = "s", name = "Тест", fromStation = "A", toStation = "D", lengthKm = 12.0, axisHash = "h",
        axis = SectionAxis("s", "h", listOf(AxisPoint(0.0, 59.0, 30.0), AxisPoint(12.0, 59.1, 30.1))),
        elements = emptyList(),
        // Смена нумерации: ось 0..5 - км 10..15, с оси 5 нумерация снова с 12: км 13 встречается на оси 3 и на оси 6.
        kmMarks = listOf(KmMark(0.0, 10.0, 1), KmMark(5.0, 12.0, 1)),
        stations = listOf(SectionStation(0.0, "А"), SectionStation(3.1, "Б"), SectionStation(6.1, "В"), SectionStation(12.0, "Г")),
    )

    private fun warn(place: String, absKm: Double = 13.0, startRel: Double = 0.0, endRel: Double = 0.0) = TrackWarning(
        id = "w", sectionId = "s", startAbsKm = absKm, endAbsKm = absKm + 0.1, startRelKm = startRel, endRelKm = endRel,
        type = WarningType.SPEED, speedKmh = 40, note = "", place = place,
    )

    @Test fun repeatedKmChoosesOccurrenceByStation() {
        assertEquals(3.0, TrackWarnings.resolve(section(), warn("Б"))!!.fromKm, 1e-6)
        assertEquals(6.0, TrackWarnings.resolve(section(), warn("В"))!!.fromKm, 1e-6)
    }

    @Test fun repeatedKmChoosesOccurrenceByStretch() {
        assertEquals(6.0, TrackWarnings.resolve(section(), warn("В — Г"))!!.fromKm, 1e-6)
        assertEquals(3.0, TrackWarnings.resolve(section(), warn("А — Б"))!!.fromKm, 1e-6)
    }

    @Test fun kmOfNeighbourPlaceStillTakesNearest() {
        // Место «Б — В» (3,1..6,1): км относится к соседнему - оба вхождения рядом, берётся ближайшее к месту (внутри диапазона).
        assertEquals(3.0, TrackWarnings.resolve(section(), warn("А"))!!.fromKm, 1e-6)
    }

    @Test fun unknownPlaceKeepsFirstOccurrence() {
        assertEquals(3.0, TrackWarnings.resolve(section(), warn("Неизвестная"))!!.fromKm, 1e-6)
    }

    @Test fun savedPlaceBoundsWinOverName() {
        assertEquals(6.0, TrackWarnings.resolve(section(), warn("Б", startRel = 5.5, endRel = 6.5))!!.fromKm, 1e-6)
    }

    @Test fun endTakesOccurrenceNearestToStart() {
        val r = TrackWarnings.resolve(section(), warn("В"))!!
        assertEquals(6.0, r.fromKm, 1e-6)
        assertEquals(6.1, r.toKm, 1e-6)
    }

    @Test fun legStationCoordinatesWinOverMissingSectionStation() {
        // Станции «Д» нет среди станций участка, но она есть у плеча (координаты на оси около км 6,1): вхождение выбирается по ней.
        val sec = section()
        val onAxisKm6 = sec.axis.locator.let { 59.0 + (59.1 - 59.0) * 6.1 / 12.0 to 30.0 + (30.1 - 30.0) * 6.1 / 12.0 }
        val stops = listOf(com.z_company.domain.util.TrainMapStop("Д", onAxisKm6.first, onAxisKm6.second))
        assertEquals(6.0, TrackWarnings.resolve(sec, warn("Д"), stops)!!.fromKm, 1e-6)
        assertEquals(3.0, TrackWarnings.resolve(sec, warn("Д"))!!.fromKm, 1e-6) // без станций плеча - первое вхождение
    }

    @Test fun placementIssueRejectsOutsideAndGrayZone() {
        val sec = section().copy(elements = listOf(ProfileElement(8.0, 10.0, noData = true)))
        fun resolved(from: Double, to: Double) = ResolvedWarning(warn("Б"), from, to)
        assertEquals(null, TrackWarnings.placementIssue(sec, resolved(3.0, 3.1)))
        assertEquals(TrackWarnings.UNKNOWN_KM_MESSAGE, TrackWarnings.placementIssue(sec, resolved(-20.5, -20.4)))
        assertEquals(TrackWarnings.UNKNOWN_KM_MESSAGE, TrackWarnings.placementIssue(sec, resolved(12.1, 12.2)))
        assertEquals(TrackWarnings.UNKNOWN_KM_MESSAGE, TrackWarnings.placementIssue(sec, resolved(9.0, 9.1)))
        assertEquals(TrackWarnings.UNKNOWN_KM_MESSAGE, TrackWarnings.placementIssue(sec, resolved(7.9, 8.5)))
    }
}
