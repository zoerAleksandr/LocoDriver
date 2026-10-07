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
    fun `departure after 30 s above move speed, time is moment of starting`() {
        val d = StopDetector()
        d.feed(0, 70, 0.0)
        assertNull(d.feed(100, 129, 8.0))
        val ev = d.onSample(sample(130, 8.0))
        assertNotNull(ev)
        assertEquals(StopEventType.DEPARTURE, ev.type)
        assertEquals(t0 + 100_000L, ev.timeMs)
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
}
