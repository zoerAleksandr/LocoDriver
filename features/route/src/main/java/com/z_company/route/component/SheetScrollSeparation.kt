package com.z_company.route.component

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity

/**
 * Список внутри шторки: прокрутка и закрытие шторки — два отдельных движения.
 *
 * Стандартно, когда список докручивается до верха, остаток того же жеста (и инерция) уходит
 * шторке — и она закрывается вместе с прокруткой. Здесь остаток съедается, если жест начался
 * не с самого верха списка или в нём уже была прокрутка. Потянуть шторку вниз можно новым
 * движением, начатым при списке наверху.
 */
fun Modifier.separateSheetFromScroll(state: ScrollableState): Modifier = composed {
    val gesture = remember { GestureState() }
    val connection = remember(state) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y != 0f) gesture.scrolled = true
                return if (gesture.blocksSheet) available else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (gesture.blocksSheet) available else Velocity.Zero
        }
    }
    this
        .pointerInput(state) {
            awaitEachGesture {
                // Initial: только запоминаем, ничего не потребляем — список и шторка работают как раньше.
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                gesture.startedAtTop = !state.canScrollBackward
                gesture.scrolled = false
            }
        }
        .nestedScroll(connection)
}

private class GestureState {
    var startedAtTop = true
    var scrolled = false
    val blocksSheet: Boolean get() = !startedAtTop || scrolled
}
