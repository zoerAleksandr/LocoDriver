package com.z_company.domain.autorecord

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StopDetectorTest {

    private val t0 = 1_700_000_000_000L - (1_700_000_000_000L % 60_000L) // кратно минуте

    private fun sample(sec: Int, kmh: Double?, acc: Float? = 5f) =
        MotionSample(t0 + sec * 1000L, 59.0, 30.0, kmh, acc)

    /** Подаёт отсчёты раз в секунду на [from]..[to] со скоростью [kmh]; возвращает первое событие. */
    private fun StopDetector.feed(from: Int, to: Int, kmh: Double?): StopEvent? {
        var ev: StopEvent? = null
        for (s in from..to) onSample(sample(s, kmh))?.let { if (ev == null) ev = it }
        return ev
    }

    @Test
    fun `arrival after a minute below stop speed, time is the start of the window`() {
        val d = StopDetector()
        assertNull(d.feed(0, 59, 1.0))
        val ev = d.onSample(sample(60, 1.0))
        assertNotNull(ev)
        assertEquals(StopEventType.ARRIVAL, ev.type)
        assertEquals(t0, ev.timeMs)
    }

    @Test
    fun `minute time is truncated to the minute`() {
        val d = StopDetector()
        d.onSample(sample(25, 1.0))
        val ev = d.feed(26, 90, 1.0)
        assertNotNull(ev)
        assertEquals(t0 + 25_000L, ev.timeMs)
        assertEquals(t0, ev.minuteTimeMs)
    }

    @Test
    fun `short stop is not an arrival`() {
        val d = StopDetector()
        assertNull(d.feed(0, 40, 0.5))
        assertNull(d.feed(41, 100, 20.0))
    }

    @Test
    fun `a speed blip above stop threshold resets the window`() {
        val d = StopDetector()
        d.feed(0, 50, 1.0)
        d.onSample(sample(51, 3.0))
        assertNull(d.feed(52, 100, 1.0)) // окно началось заново с 52-й секунды
        assertNotNull(d.onSample(sample(112, 1.0)))
    }

    @Test
    fun `departure after 30 s above move speed, abrupt start keeps last rest sample`() {
        val d = StopDetector()
        d.feed(0, 70, 0.0)
        d.feed(71, 99, 0.0)
        assertNull(d.feed(100, 129, 8.0))
        val ev = d.onSample(sample(130, 8.0))
        assertNotNull(ev)
        assertEquals(StopEventType.DEPARTURE, ev.type)
        assertEquals(t0 + 99_000L, ev.timeMs) // последний отсчёт «≈0» перед ростом
    }

    @Test
    fun `smooth acceleration 0 to 30 kmh in 2 min - departure at last rest sample`() {
        val d = StopDetector()
        assertNotNull(d.feed(0, 90, 0.0)) // arrival at 0
        var ev: StopEvent? = null
        // стоим до 120-й, дальше линейный разгон: 0.25 км/ч за секунду (30 км/ч за 120 с)
        for (s in 91..300) {
            val v = if (s <= 120) 0.0 else (s - 120) * 0.25
            d.onSample(sample(s, v))?.let { if (ev == null) ev = it }
        }
        val e = assertNotNull(ev)
        assertEquals(StopEventType.DEPARTURE, e.type)
        // скорость ≤ 1 км/ч до 124-й секунды включительно (0.25*4=1.0)
        assertEquals(t0 + 124_000L, e.timeMs)
        assertEquals(t0 + 120_000L, e.minuteTimeMs)
    }

    @Test
    fun `braking to a stop - arrival is first sample below threshold, not later`() {
        val d = StopDetector()
        d.feed(0, 10, 30.0)
        var ev: StopEvent? = null
        // торможение 30 → 0 за 60 с (0.5 км/ч/с), потом стоим
        for (s in 11..300) {
            val v = (30.0 - (s - 10) * 0.5).coerceAtLeast(0.0)
            d.onSample(sample(s, v))?.let { if (ev == null) ev = it }
        }
        val e = assertNotNull(ev)
        assertEquals(StopEventType.ARRIVAL, e.type)
        // v<2 впервые при s-10 > 56, т.е. s=67
        assertEquals(t0 + 67_000L, e.timeMs)
        assertEquals(t0 + 60_000L, e.minuteTimeMs)
    }

    @Test
    fun `gps speed jitter while standing gives one arrival and no departure`() {
        val d = StopDetector()
        val events = mutableListOf<StopEvent>()
        // дрожание 0..1.9 км/ч и редкие всплески 3 км/ч на 1-2 с
        for (s in 0..600) {
            val v = when {
                s % 97 == 0 && s > 100 -> 3.0
                else -> (s * 7 % 20) / 10.0 // 0.0..1.9
            }
            d.onSample(sample(s, v))?.let { events += it }
        }
        assertEquals(1, events.count { it.type == StopEventType.ARRIVAL })
        assertEquals(0, events.count { it.type == StopEventType.DEPARTURE })
    }

    @Test
    fun `spikes above move speed shorter than 30 s do not depart`() {
        val d = StopDetector()
        d.feed(0, 70, 0.0)
        assertNull(d.feed(71, 90, 8.0)) // 20 с
        assertNull(d.feed(91, 150, 0.0))
    }

    @Test
    fun `rollback does not go before the arrival of the current stop`() {
        val d = StopDetector()
        d.feed(0, 10, 20.0)
        val arr = d.feed(11, 80, 0.0)
        assertNotNull(arr)
        val ev = d.feed(81, 130, 12.0)
        assertNotNull(ev)
        assertEquals(t0 + 80_000L, ev.timeMs)
        assert(ev.timeMs >= arr.timeMs)
    }

    @Test
    fun `speed between thresholds neither departs nor resumes stopped state`() {
        val d = StopDetector()
        d.feed(0, 70, 0.0)
        assertNull(d.feed(100, 200, 4.0))
        assertNull(d.feed(201, 230, 5.0)) // ровно порог — не «выше»
    }

    @Test
    fun `slow crawling does not arrive again and next stop gives a new arrival`() {
        val d = StopDetector()
        d.feed(0, 70, 0.0)
        assertNotNull(d.feed(100, 140, 30.0)) // departure
        assertNull(d.feed(141, 150, 60.0))
        assertNotNull(d.feed(151, 300, 0.0)) // arrival
    }

    @Test
    fun `no repeated arrival while staying stopped`() {
        val d = StopDetector()
        assertNotNull(d.feed(0, 70, 0.0))
        assertNull(d.feed(71, 500, 0.0))
    }

    @Test
    fun `gps gap longer than limit resets the window`() {
        val d = StopDetector()
        d.feed(0, 40, 0.0)
        assertNull(d.onSample(sample(100, 0.0))) // дыра 60 с — окно с 100-й
        assertNotNull(d.feed(101, 160, 0.0))
    }

    @Test
    fun `samples without speed or with poor accuracy are ignored`() {
        val d = StopDetector()
        d.feed(0, 40, 0.0)
        assertNull(d.onSample(sample(45, null)))
        assertNull(d.onSample(sample(50, 30.0, acc = 200f))) // плохой фикс не сбрасывает окно
        assertNotNull(d.onSample(sample(61, 0.0)))
    }

    @Test
    fun `seeded as stopped does not repeat arrival and still gives departure`() {
        val d = StopDetector()
        d.seed(stopped = true, sinceMs = t0 - 600_000L)
        assertNull(d.feed(0, 120, 0.5)) // стоим дальше — повторного прибытия нет
        val ev = d.feed(130, 170, 20.0)
        assertNotNull(ev)
        assertEquals(StopEventType.DEPARTURE, ev.type)
    }

    @Test
    fun `state accessors follow events`() {
        val d = StopDetector()
        assertEquals(false, d.isStopped)
        d.feed(0, 70, 0.0)
        assertEquals(true, d.isStopped)
        assertEquals(t0, d.stoppedSince)
    }
}
