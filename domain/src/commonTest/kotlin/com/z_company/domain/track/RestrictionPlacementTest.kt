package com.z_company.domain.track

import com.z_company.domain.util.TrainMapStop
import kotlin.test.Test
import kotlin.test.assertEquals

class RestrictionPlacementTest {
    private val stops = listOf(TrainMapStop("Антропшино", 59.5, 30.0), TrainMapStop("Кобралово", 59.6, 30.1))

    private fun pass(sectionId: String = RestrictionPass.NO_SECTION, abs: Double? = 40.4, place: String = "Антропшино") = RestrictionPass(
        trainId = "t", warningId = "w", routeId = "r", sectionId = sectionId, kind = RestrictionKind.SPEED, title = "ограничение 60 км/ч",
        place = place, limitKmh = 60, startAbsKm = abs, endAbsKm = abs?.plus(0.1), updatedAt = 1L,
    )

    private fun summary(id: String, box: SectionBox?, hasProfile: Boolean = true) = SectionSummary(id, id, "А", "Б", 10.0, hasProfile, 1L, box)

    private val covering = SectionBox(59.4, 29.9, 59.7, 30.2)
    private val elsewhere = SectionBox(55.0, 37.0, 55.1, 37.1)

    @Test fun trackedIsOnMap() = assertEquals(Placement.ON_MAP, RestrictionPlacement.classify(pass("s1"), stops, emptyList(), emptyMap()))

    @Test fun withoutKmCannotBePlaced() = assertEquals(Placement.NO_KM, RestrictionPlacement.classify(pass(abs = null), stops, listOf(summary("s1", covering)), emptyMap()))

    @Test fun noSectionAroundMeansNoProfile() =
        assertEquals(Placement.NO_PROFILE, RestrictionPlacement.classify(pass(), stops, listOf(summary("s1", elsewhere)), emptyMap()))

    @Test fun sectionWithoutProfileIsIgnored() =
        assertEquals(Placement.NO_PROFILE, RestrictionPlacement.classify(pass(), stops, listOf(summary("s1", covering, hasProfile = false)), emptyMap()))

    @Test fun coveringSectionNotLoadedIsPending() =
        assertEquals(Placement.PENDING, RestrictionPlacement.classify(pass(), stops, listOf(summary("s1", covering)), emptyMap()))

    @Test fun unknownPlaceOrEmptyIndexIsUnknown() {
        assertEquals(Placement.UNKNOWN, RestrictionPlacement.classify(pass(place = "Где-то"), stops, listOf(summary("s1", covering)), emptyMap()))
        assertEquals(Placement.UNKNOWN, RestrictionPlacement.classify(pass(), stops, emptyList(), emptyMap()))
    }

    @Test fun sectionsWithoutBoxCannotBeExcluded() =
        assertEquals(Placement.UNKNOWN, RestrictionPlacement.classify(pass(), stops, listOf(summary("s1", elsewhere), summary("s2", null)), emptyMap()))
}
