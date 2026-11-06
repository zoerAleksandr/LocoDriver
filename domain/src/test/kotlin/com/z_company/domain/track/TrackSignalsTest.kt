package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackSignalsTest {
    // Чётное направление - к росту км оси (как на Лужская — Мга): Ч виден лицом по оси (+1), Н - навстречу (−1).
    private fun even(km: Double, label: String, kind: SignalKind = SignalKind.PASS) = TrackSignal(km, label, SignalTrack.EVEN, kind, +1)
    private fun odd(km: Double, label: String, kind: SignalKind = SignalKind.PASS) = TrackSignal(km, label, SignalTrack.ODD, kind, -1)

    private val signals = listOf(
        even(7.0, "6"), odd(7.0, "1"),
        even(9.0, "4"), odd(9.0, "3"),
        even(11.0, "2"), odd(11.0, "5"),
        odd(5.6, "Н", SignalKind.STATION), even(12.5, "Ч2", SignalKind.STATION),
    )

    @Test
    fun even_direction_follows_even_signals() {
        assertEquals(+1, SignalRules.evenAxisDir(signals, 8.0))
        assertEquals(TravelParity.EVEN, SignalRules.parity(signals, 8.0, travelAxisDir = +1))
        assertEquals(TravelParity.ODD, SignalRules.parity(signals, 8.0, travelAxisDir = -1))
        assertEquals(+1, SignalRules.axisDir(signals, 8.0, TravelParity.EVEN))
        assertEquals(-1, SignalRules.axisDir(signals, 8.0, TravelParity.ODD))
    }

    @Test
    fun no_signals_means_unknown_parity() {
        assertEquals(0, SignalRules.evenAxisDir(emptyList(), 3.0))
        assertNull(SignalRules.parity(emptyList(), 3.0, +1))
        assertNull(SignalRules.axisDir(emptyList(), 3.0, TravelParity.EVEN))
    }

    @Test
    fun nearest_signal_decides_when_axis_turns_against_numbering() {
        // дальше по оси нумерация идёт в другую сторону: чётное направление там - к убыли км
        val turned = signals + listOf(
            TrackSignal(40.0, "4", SignalTrack.EVEN, SignalKind.PASS, -1),
            TrackSignal(40.0, "3", SignalTrack.ODD, SignalKind.PASS, +1),
        )
        assertEquals(+1, SignalRules.evenAxisDir(turned, 9.0))
        assertEquals(-1, SignalRules.evenAxisDir(turned, 39.0))
        assertEquals(TravelParity.EVEN, SignalRules.parity(turned, 39.0, travelAxisDir = -1))
    }

    @Test
    fun own_track_shows_the_letter_of_travel() {
        assertEquals(SignalTrack.EVEN, SignalRules.shownTrack(TravelParity.EVEN, wrongTrack = false))
        assertEquals(SignalTrack.ODD, SignalRules.shownTrack(TravelParity.ODD, wrongTrack = false))
        val shown = SignalRules.visible(signals, TravelParity.EVEN, wrongTrack = false)
        assertTrue(shown.all { it.track == SignalTrack.EVEN })
        assertEquals(4, shown.size)
    }

    @Test
    fun wrong_track_shows_opposite_letter_with_back_to_train() {
        // чётное направление (к росту км) по нечётному пути: ориентир - нечётные светофоры, они смотрят навстречу
        val ahead = SignalRules.ahead(signals, kmAxis = 6.0, travelAxisDir = +1, parity = TravelParity.EVEN, wrongTrack = true, limit = 5)
        assertEquals(listOf("1", "3", "5"), ahead.filter { it.signal.kind == SignalKind.PASS }.map { it.signal.title })
        assertTrue(ahead.all { it.backToTrain })
    }

    @Test
    fun ahead_counts_only_signals_in_travel_direction_sorted_by_distance() {
        val fwd = SignalRules.ahead(signals, kmAxis = 8.0, travelAxisDir = +1, parity = TravelParity.EVEN, wrongTrack = false, limit = 3)
        assertEquals(listOf("4", "2", "Ч2"), fwd.map { it.signal.title })
        assertEquals(1.0, fwd.first().distanceKm, 1e-9)
        assertFalse(fwd.any { it.backToTrain })
        val back = SignalRules.ahead(signals, kmAxis = 8.0, travelAxisDir = -1, parity = TravelParity.ODD, wrongTrack = false, limit = 3)
        assertEquals(listOf("1", "Н"), back.map { it.signal.title })
        assertEquals(1.0, back.first().distanceKm, 1e-9)
        assertFalse(back.any { it.backToTrain })
    }

    @Test
    fun passing_signal_is_labelled_by_number_only() {
        assertEquals("6", even(7.0, "6").title)
        assertEquals("5", odd(11.0, "5").title)
        assertEquals("ЧМ2А", even(1.0, "ЧМ2А", SignalKind.SHUNT).title)
    }

    @Test
    fun train_number_parity() {
        assertEquals(TravelParity.EVEN, SignalRules.parityOfTrainNumber("2704"))
        assertEquals(TravelParity.ODD, SignalRules.parityOfTrainNumber("2701"))
        assertEquals(TravelParity.EVEN, SignalRules.parityOfTrainNumber("2704Ч"))
        assertEquals(TravelParity.ODD, SignalRules.parityOfTrainNumber(" 31-й"))
        assertNull(SignalRules.parityOfTrainNumber(null))
        assertNull(SignalRules.parityOfTrainNumber("без номера"))
    }

    @Test
    fun ahead_of_track_uses_chosen_track_and_marks_back_to_train() {
        val ahead = SignalRules.aheadOfTrack(signals, 8.0, travelAxisDir = +1, track = SignalTrack.ODD, limit = 2)
        assertEquals(listOf("3", "5"), ahead.map { it.signal.label })
        assertTrue(ahead.all { it.backToTrain })
    }
}
