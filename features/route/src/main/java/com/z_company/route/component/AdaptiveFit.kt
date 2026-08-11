package com.z_company.route.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer

/**
 * Помещаются ли переданные строки в сумме в [availableWidthPx] по ширине — измеряется
 * по факту (с учётом текущего системного масштаба шрифта), а не по фиксированному
 * порогу fontScale. Так перекладка «ряд → столбец» срабатывает только когда текст
 * реально не влезает: на широких экранах ряд сохраняется, на узких/крупном шрифте —
 * столбец.
 *
 * [extraPx] — суммарная ширина неизмеряемых элементов на той же строке (отступы,
 * иконки, стрелка), которую нужно вычесть из доступной ширины.
 *
 * Каждая часть меряется одной строкой без переноса (естественная ширина).
 * Измерение текста дёшево; вызывать можно прямо в композиции (для статичных экранов
 * и коротких списков влияние на производительность незаметно).
 */
@Composable
fun textsFitInWidth(
    availableWidthPx: Int,
    extraPx: Int = 0,
    vararg parts: Pair<String, TextStyle>,
): Boolean {
    if (availableWidthPx <= 0) return true
    val measurer = rememberTextMeasurer()
    var total = extraPx
    for ((text, style) in parts) {
        total += measurer.measure(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
        ).size.width
        if (total > availableWidthPx) return false
    }
    return total <= availableWidthPx
}
