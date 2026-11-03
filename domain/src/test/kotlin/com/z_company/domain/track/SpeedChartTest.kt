package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeedChartTest {
    // 0.001° широты ≈ 111 м; точка раз в 10 с.
    private fun pts(n: Int, v: (Int) -> Double? = { 60.0 }) =
        (0 until n).map { TrackPoint(it * 10_000L, 55.0 + it * 0.001, 37.0, v(it), 5f) }

    @Test
    fun seriesAccumulatesDistance() {
        val s = SpeedChart.buildSeries(pts(11))
        assertEquals(0.0, s.first().distM)
        assertEquals(1112.0, s.last().distM, 15.0)
        assertTrue(s.zipWithNext().all { (a, b) -> b.distM >= a.distM })
        assertEquals(100_000L, SpeedChart.total(s, ChartAxis.TIME).toLong())
        assertEquals(10, s.last().index)
    }

    @Test
    fun gapDoesNotAddDistance() {
        val a = pts(3)
        val b = pts(3).map { it.copy(timeMs = it.timeMs + 3_600_000L, latitude = it.latitude + 1.0) }
        val s = SpeedChart.buildSeries(a + b)
        assertEquals(s[2].distM, s[3].distM, 1e-9)
        assertTrue(s[5].distM > s[3].distM)
    }

    @Test
    fun zoomHalvesSpanAroundAnchor() {
        val total = 10_000.0
        val w = SpeedChart.zoom(SpeedChart.fullWindow(total), total, 2.0, 2_500.0, 300.0)
        assertEquals(5_000.0, w.span, 1e-6)
        assertEquals(1_250.0, w.start, 1e-6) // якорь на 25% окна остаётся на 25%
    }

    @Test
    fun zoomAnchorStaysUnderFinger() {
        val total = 10_000.0
        val w1 = SpeedChart.zoom(ChartWindow(2_000.0, 8_000.0), total, 2.0, 5_000.0, 300.0)
        assertEquals(3_000.0, w1.span, 1e-6)
        assertEquals(3_500.0, w1.start, 1e-6)
    }

    @Test
    fun zoomLimits() {
        val total = 10_000.0
        val tiny = SpeedChart.zoom(SpeedChart.fullWindow(total), total, 1e9, 5_000.0, 300.0)
        assertEquals(300.0, tiny.span, 1e-6)
        val out = SpeedChart.zoom(ChartWindow(1_000.0, 2_000.0), total, 1e-9, 1_500.0, 300.0)
        assertEquals(ChartWindow(0.0, total), out)
    }

    @Test
    fun panClampsToSeries() {
        val total = 10_000.0
        val w = ChartWindow(2_000.0, 4_000.0)
        assertEquals(ChartWindow(3_000.0, 5_000.0), SpeedChart.pan(w, total, 1_000.0))
        assertEquals(ChartWindow(0.0, 2_000.0), SpeedChart.pan(w, total, -9_000.0))
        assertEquals(ChartWindow(8_000.0, 10_000.0), SpeedChart.pan(w, total, 99_000.0))
    }

    @Test
    fun visibleDecimatesButKeepsExtremes() {
        val s = SpeedChart.buildSeries(pts(2000) { if (it == 1000) 120.0 else if (it == 1500) 0.0 else 60.0 })
        val total = SpeedChart.total(s, ChartAxis.PATH)
        val out = SpeedChart.visible(s, ChartAxis.PATH, SpeedChart.fullWindow(total), 100)
        assertTrue(out.size <= 100 * 4 + 4)
        assertTrue(out.any { it.speedKmh == 120.0 })
        assertTrue(out.any { it.speedKmh == 0.0 })
        assertEquals(0, out.first().index)
        assertEquals(1999, out.last().index)
    }

    @Test
    fun visibleWindowIncludesNeighbours() {
        val s = SpeedChart.buildSeries(pts(100))
        val out = SpeedChart.visible(s, ChartAxis.PATH, ChartWindow(s[50].distM, s[60].distM), 1000)
        assertEquals(49, out.first().index)
        assertEquals(60, out.last().index)
    }

    @Test
    fun nearestAndIndexAtTime() {
        val s = SpeedChart.buildSeries(pts(11))
        assertEquals(3, SpeedChart.nearest(s, ChartAxis.TIME, 31_000.0))
        assertEquals(4, SpeedChart.nearest(s, ChartAxis.TIME, 36_000.0))
        assertEquals(0, SpeedChart.nearest(s, ChartAxis.PATH, -50.0))
        assertEquals(10, SpeedChart.nearest(s, ChartAxis.PATH, 1e9))
        assertEquals(2, SpeedChart.indexAtTime(s, 24_000L))
        assertEquals(-1, SpeedChart.nearest(emptyList(), ChartAxis.PATH, 0.0))
    }

    @Test
    fun stopsMapToAxes() {
        val s = SpeedChart.buildSeries(pts(11))
        val stops = listOf(
            TrainStopMark(55.0, 37.0, "Депо", 30_000L, 60_000L),
            TrainStopMark(55.0, 37.0, null, 9_999_999L, null), // вне записи
        )
        val onTime = SpeedChart.stopsOnAxis(s, stops, ChartAxis.TIME)
        assertEquals(1, onTime.size)
        assertEquals(30_000.0, onTime[0].x)
        assertEquals(60_000.0, onTime[0].xEnd)
        val onPath = SpeedChart.stopsOnAxis(s, stops, ChartAxis.PATH)
        assertEquals(s[3].distM, onPath[0].x)
        assertEquals(onPath[0].x, onPath[0].xEnd)
    }

    @Test
    fun ceilingAndTicks() {
        assertEquals(40.0, SpeedChart.speedCeiling(emptyList()))
        val s = SpeedChart.buildSeries(pts(5) { 83.0 })
        assertEquals(100.0, SpeedChart.speedCeiling(s))
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0), SpeedChart.niceTicks(0.0, 100.0, 6))
        assertTrue(SpeedChart.niceTicks(0.0, 0.0, 5).isEmpty())
    }

    @Test
    fun timeTicksAreRoundWallClock() {
        val h = 3_600_000L
        val t0 = 10 * h + 27 * 60_000L // 10:27 UTC, пояс +3 → 13:27
        val ticks = SpeedChart.timeTicks(t0, 0.0, 2.0 * h, 6, 3 * h)
        assertTrue(ticks.isNotEmpty())
        // каждая метка в местном времени кратна шагу (30 мин)
        ticks.forEach { assertEquals(0L, (t0 + it.toLong() + 3 * h) % (30 * 60_000L)) }
        assertTrue(ticks.all { it >= 0.0 && it <= 2.0 * h })
        assertEquals(30 * 60_000.0, ticks[1] - ticks[0])
        assertTrue(SpeedChart.timeTicks(t0, 5.0, 5.0, 6, 0).isEmpty())
    }

    @Test
    fun speedScaleFreightBands() {
        assertEquals(SpeedBand.GREEN, SpeedScale.band(0.0))
        assertEquals(SpeedBand.GREEN, SpeedScale.band(20.0))
        assertEquals(SpeedBand.YELLOW, SpeedScale.band(20.1))
        assertEquals(SpeedBand.YELLOW, SpeedScale.band(70.0))
        assertEquals(SpeedBand.ORANGE, SpeedScale.band(70.1))
        assertEquals(SpeedBand.ORANGE, SpeedScale.band(80.0))
        assertEquals(SpeedBand.RED, SpeedScale.band(80.1))
        assertEquals(SpeedBand.UNKNOWN, SpeedScale.band(null))
        assertEquals(SpeedBand.UNKNOWN, SpeedScale.band(-1.0))
        assertEquals(SpeedScale.UNKNOWN_RGB, SpeedScale.rgb(null))
    }

    @Test
    fun speedScalePassengerBands() {
        val p = SpeedProfile.PASSENGER
        assertEquals(SpeedBand.GREEN, SpeedScale.band(20.0, p))
        assertEquals(SpeedBand.YELLOW, SpeedScale.band(100.0, p))
        assertEquals(SpeedBand.YELLOW, SpeedScale.band(120.0, p))
        assertEquals(SpeedBand.ORANGE, SpeedScale.band(121.0, p))
        assertEquals(SpeedBand.ORANGE, SpeedScale.band(160.0, p))
        assertEquals(SpeedBand.RED, SpeedScale.band(160.1, p))
        assertEquals(SpeedBand.UNKNOWN, SpeedScale.band(null, p))
    }

    @Test
    fun speedScaleColorsDifferByBandAndTheme() {
        val bands = listOf(SpeedBand.GREEN, SpeedBand.YELLOW, SpeedBand.ORANGE, SpeedBand.RED)
        assertEquals(4, bands.map { SpeedScale.rgb(it) }.toSet().size)
        assertEquals(4, bands.map { SpeedScale.rgb(it, dark = true) }.toSet().size)
        assertTrue(bands.all { SpeedScale.rgb(it) != SpeedScale.rgb(it, dark = true) })
    }

    @Test
    fun speedScaleLegendLabels() {
        assertEquals(listOf("≤20", "20–70", "70–80", ">80"), SpeedScale.legend(SpeedProfile.FREIGHT).map { it.second })
        assertEquals(listOf("≤20", "20–120", "120–160", ">160"), SpeedScale.legend(SpeedProfile.PASSENGER).map { it.second })
    }

    @Test
    fun passengerCategoryDetection() {
        assertTrue(SpeedScale.isPassengerCategory("Скорые круглогодичные"))
        assertTrue(SpeedScale.isPassengerCategory("Пассажирские круглогодичные"))
        assertTrue(SpeedScale.isPassengerCategory("Пригородные, приграничные пригородные (региональные) поезда"))
        assertTrue(SpeedScale.isPassengerCategory("Скоростные пригородные поезда"))
        assertTrue(SpeedScale.isPassengerCategory("Высокоскоростные"))
        assertFalse(SpeedScale.isPassengerCategory("Тяжеловесные поезда"))
        assertFalse(SpeedScale.isPassengerCategory("Контейнерные поезда"))
        assertFalse(SpeedScale.isPassengerCategory("Грузопассажирские"))
        assertFalse(SpeedScale.isPassengerCategory("Состоящие из моторвагонного подвижного состава без пассажиров, в т.ч. от скоростных и высокоскоростных"))
        assertFalse(SpeedScale.isPassengerCategory("Толкачи-резервные локомотивы, следующие для подталкивания или после подталкивания пассажирских поездов"))
        assertFalse(SpeedScale.isPassengerCategory("Номер не найден"))
        assertFalse(SpeedScale.isPassengerCategory(null))
        assertEquals(SpeedProfile.PASSENGER, SpeedScale.profileForCategory("Скорые круглогодичные"))
        assertEquals(SpeedProfile.FREIGHT, SpeedScale.profileForCategory(null))
    }

    @Test
    fun passengerTrainByNumber() {
        assertTrue(SpeedScale.isPassengerTrain(com.z_company.domain.entities.route.Train(number = "2")))
        assertTrue(SpeedScale.isPassengerTrain(com.z_company.domain.entities.route.Train(number = "6001")))
        assertFalse(SpeedScale.isPassengerTrain(com.z_company.domain.entities.route.Train(number = "2201")))
        assertFalse(SpeedScale.isPassengerTrain(com.z_company.domain.entities.route.Train()))
        assertFalse(SpeedScale.isPassengerTrain(null))
    }

    // Движение, стоянка (одни координаты), движение: ось «Путь» на стоянке стоит, «Время» нет.
    private fun withStop(): List<SpeedSample> = SpeedChart.buildSeries(
        (0 until 30).map { i ->
            val steps = if (i < 10) i else if (i < 20) 10 else i - 10
            TrackPoint(i * 10_000L, 55.0 + steps * 0.001, 37.0, 50.0, 5f)
        }
    )

    private fun indexRange(s: List<SpeedSample>, axis: ChartAxis, w: ChartWindow): IntRange {
        val t0 = s.first().timeMs
        val ix = s.indices.filter { SpeedChart.xOf(s[it], axis, t0) >= w.start - 1e-6 && SpeedChart.xOf(s[it], axis, t0) <= w.end + 1e-6 }
        return ix.first()..ix.last()
    }

    @Test
    fun axisSwitchKeepsWindowPoints() {
        val s = withStop()
        // Окно на оси «Время»: точки 5..25.
        val timeW = ChartWindow(50_000.0, 250_000.0)
        val pathW = SpeedChart.convertWindow(s, ChartAxis.TIME, ChartAxis.PATH, timeW)
        // На «Пути» стоянка схлопнута: в окно попадают те же точки (первая-крайняя совпадают с 5 и 25).
        val r = indexRange(s, ChartAxis.PATH, pathW)
        assertEquals(5, r.first)
        assertEquals(25, r.last)
        // Обратно: левая/правая граница снова на точках 5 и 25.
        val back = SpeedChart.convertWindow(s, ChartAxis.PATH, ChartAxis.TIME, pathW)
        assertEquals(50_000.0, back.start, 10_000.0)
        assertEquals(250_000.0, back.end, 10_000.0)
    }

    @Test
    fun axisSwitchKeepsFullWindowFull() {
        val s = withStop()
        val w = SpeedChart.convertWindow(s, ChartAxis.PATH, ChartAxis.TIME, SpeedChart.fullWindow(SpeedChart.total(s, ChartAxis.PATH)))
        assertEquals(0.0, w.start)
        assertEquals(SpeedChart.total(s, ChartAxis.TIME), w.end)
    }

    @Test
    fun axisSwitchPathToTimeBoundariesAreSamples() {
        val s = SpeedChart.buildSeries(pts(21))
        // Равномерное движение: путь пропорционален времени, окно переносится линейно (с точностью до точки).
        val pathTotal = SpeedChart.total(s, ChartAxis.PATH)
        val w = SpeedChart.convertWindow(s, ChartAxis.PATH, ChartAxis.TIME, ChartWindow(pathTotal * 0.25, pathTotal * 0.75))
        assertEquals(50_000.0, w.start, 10_000.0)
        assertEquals(150_000.0, w.end, 10_000.0)
    }

    @Test
    fun edgeButtonsVisibility() {
        val total = 1000.0
        // Весь ряд виден - обеих кнопок нет.
        assertFalse(SpeedChart.canScrollToStart(ChartWindow(0.0, 1000.0), total))
        assertFalse(SpeedChart.canScrollToEnd(ChartWindow(0.0, 1000.0), total))
        // Прижато к началу - нет «В начало», есть «В конец».
        assertFalse(SpeedChart.canScrollToStart(ChartWindow(0.0, 400.0), total))
        assertTrue(SpeedChart.canScrollToEnd(ChartWindow(0.0, 400.0), total))
        // Прижато к концу - наоборот.
        assertTrue(SpeedChart.canScrollToStart(ChartWindow(600.0, 1000.0), total))
        assertFalse(SpeedChart.canScrollToEnd(ChartWindow(600.0, 1000.0), total))
        // Середина - обе.
        assertTrue(SpeedChart.canScrollToStart(ChartWindow(300.0, 600.0), total))
        assertTrue(SpeedChart.canScrollToEnd(ChartWindow(300.0, 600.0), total))
        // Пустой ряд - нет.
        assertFalse(SpeedChart.canScrollToStart(ChartWindow(0.0, 0.0), 0.0))
    }

    @Test
    fun edgeWindowKeepsSpan() {
        val w = ChartWindow(300.0, 600.0)
        assertEquals(ChartWindow(0.0, 300.0), SpeedChart.edgeWindow(w, 1000.0, toEnd = false))
        assertEquals(ChartWindow(700.0, 1000.0), SpeedChart.edgeWindow(w, 1000.0, toEnd = true))
    }
}
