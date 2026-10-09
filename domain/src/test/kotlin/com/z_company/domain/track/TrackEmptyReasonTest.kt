package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackEmptyReasonTest {
    private fun st(
        consent: Boolean = true,
        auto: Boolean = true,
        target: String? = "t1",
        thisId: String? = "t1",
    ) = TrackRecordingState(consent, auto, target, thisId)

    private fun reason(s: TrackRecordingState?, n: Int = 0, withSpeed: Int = 0) =
        TrackEmptyReasons.determine(s, n, withSpeed)

    @Test
    fun consentOffWins() {
        assertEquals(TrackEmptyReason.CONSENT_OFF, reason(st(consent = false, auto = false)))
    }

    @Test
    fun autoOff() {
        assertEquals(TrackEmptyReason.AUTO_OFF, reason(st(auto = false)))
    }

    @Test
    fun noTarget() {
        assertEquals(TrackEmptyReason.NO_TARGET, reason(st(target = null)))
    }

    @Test
    fun otherTrain() {
        assertEquals(TrackEmptyReason.OTHER_TRAIN, reason(st(target = "t2")))
    }

    @Test
    fun unknownThisTrainIsNotOther() {
        assertEquals(TrackEmptyReason.NO_POINTS_YET, reason(st(target = "t2", thisId = null)))
    }

    @Test
    fun sessionJustStarted() {
        assertEquals(TrackEmptyReason.NO_POINTS_YET, reason(st(), n = 1, withSpeed = 1))
    }

    @Test
    fun pointsWithoutSpeed() {
        assertEquals(TrackEmptyReason.NO_SPEED, reason(st(consent = false), n = 5, withSpeed = 0))
        assertEquals(TrackEmptyReason.NO_SPEED, reason(null, n = 2, withSpeed = 0))
    }

    @Test
    fun unknownState() {
        assertEquals(TrackEmptyReason.UNKNOWN, reason(null))
    }

    @Test
    fun debugLineHasNoCoordinatesAndShowsFlags() {
        assertEquals(
            "согласие: вкл · АВТО: выкл · цель: другой · точек: 3",
            TrackEmptyReasons.debugLine(st(auto = false, target = "t2"), 3),
        )
        assertEquals(
            "согласие: выкл · АВТО: вкл · цель: нет · точек: 0",
            TrackEmptyReasons.debugLine(st(consent = false, target = null), 0),
        )
        assertEquals(
            "согласие: вкл · АВТО: вкл · цель: этот · точек: 7",
            TrackEmptyReasons.debugLine(st(), 7),
        )
    }
}
