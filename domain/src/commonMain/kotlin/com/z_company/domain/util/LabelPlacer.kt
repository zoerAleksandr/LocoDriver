package com.z_company.domain.util

/** Точка с подписью на карте в экранных координатах (px). Подпись — под точкой по центру. */
data class MapLabelItem(
    val x: Double,
    val y: Double,
    /** Ширина и высота текста подписи; 0 — подписи нет (точка всё равно занимает место). */
    val labelW: Double,
    val labelH: Double,
    val dotRadius: Double,
    /** Меньше — важнее (крайние и «впереди» — 0). */
    val priority: Int,
)

/**
 * Выбор подписей станций, которые помещаются (SCREEN_SPECS §6.9): подпись показывается, только если её рамка не
 * пересекает ни одну точку (любой станции) и ни одну уже выбранную подпись. Более важные выбираются первыми,
 * при равенстве — по порядку в списке. При приближении расстояния в px растут и подписи появляются.
 */
object LabelPlacer {
    private const val GAP = 2.0
    private const val DOT_TO_LABEL = 3.0

    fun visible(items: List<MapLabelItem>): Set<Int> {
        val chosen = mutableListOf<DoubleArray>()
        val result = mutableSetOf<Int>()
        val order = items.indices.sortedWith(compareBy({ items[it].priority }, { it }))
        for (i in order) {
            val it = items[i]
            if (it.labelW <= 0.0 || it.labelH <= 0.0) continue
            val left = it.x - it.labelW / 2 - GAP
            val right = it.x + it.labelW / 2 + GAP
            val top = it.y + it.dotRadius + DOT_TO_LABEL - GAP
            val bottom = it.y + it.dotRadius + DOT_TO_LABEL + it.labelH + GAP
            val hitsDot = items.indices.any { j ->
                val o = items[j]
                // Своя точка выше подписи и её не пересекает по построению.
                j != i && o.x + o.dotRadius > left && o.x - o.dotRadius < right &&
                    o.y + o.dotRadius > top && o.y - o.dotRadius < bottom
            }
            if (hitsDot) continue
            val hitsLabel = chosen.any { c -> c[0] < right && c[1] > left && c[2] < bottom && c[3] > top }
            if (hitsLabel) continue
            chosen += doubleArrayOf(left, right, top, bottom)
            result += i
        }
        return result
    }
}
