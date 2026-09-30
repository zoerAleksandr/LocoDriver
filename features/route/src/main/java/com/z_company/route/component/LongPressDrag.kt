package com.z_company.route.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange

/**
 * Перетаскивание элемента списка: долгое нажатие (или, если [immediate] → true, сразу после
 * порога сдвига) → [onDrag] со смещением пальца → [onEnd] при отпускании/отмене.
 *
 * После старта события забираются на проходе **Initial**: дочерние детекторы (свайп «Удалить»,
 * тапы, поля ввода) и родительская прокрутка их уже не видят. Иначе они «съедают» первое
 * движение и жест обрывается — так ломалась библиотека reorderable в списке станций поезда.
 *
 * Ключ `pointerInput` у вызывающего должен быть стабильным (id элемента): смена ключа
 * посреди жеста перезапускает детектор и рвёт перетаскивание.
 */
internal suspend fun PointerInputScope.detectReorderDrag(
    immediate: () -> Boolean = { false },
    onStart: () -> Unit,
    onDrag: (change: PointerInputChange, delta: Offset) -> Unit,
    onEnd: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val start = if (immediate()) {
            awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
        } else {
            awaitLongPressOrCancellation(down.id)
        } ?: return@awaitEachGesture
        onStart()
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == start.id } ?: break
                if (!change.pressed) {
                    change.consume()
                    break
                }
                val delta = change.positionChange()
                change.consume()
                if (delta != Offset.Zero) onDrag(change, delta)
            }
        } finally {
            onEnd()
        }
    }
}

/**
 * Шаг перестановки для списка переменной высоты/ширины: пока смещение перетаскиваемого
 * элемента заходит за половину соседа — меняем их местами и компенсируем смещение на размер
 * соседа (без этого элемент «прыгает» на слот). Возвращает новое смещение.
 *
 * @param order текущий порядок id (меняется на месте)
 * @param sizes id → размер по оси перетаскивания, px
 * @param gap промежуток между элементами, px
 * @param onMove вызывается на каждый обмен (from, to) — для обновления данных
 */
internal fun <K> reorderStep(
    order: MutableList<K>,
    id: K,
    offset: Float,
    sizes: Map<K, Int>,
    gap: Float = 0f,
    onMove: (from: Int, to: Int) -> Unit = { _, _ -> },
): Float {
    var off = offset
    var i = order.indexOf(id)
    if (i < 0) return off
    while (i < order.lastIndex) {
        val next = (sizes[order[i + 1]] ?: break) + gap
        if (off <= next / 2f) break
        off -= next
        order.add(i + 1, order.removeAt(i)); onMove(i, i + 1); i++
    }
    while (i > 0) {
        val prev = (sizes[order[i - 1]] ?: break) + gap
        if (off >= -prev / 2f) break
        off += prev
        order.add(i - 1, order.removeAt(i)); onMove(i, i - 1); i--
    }
    return off
}
