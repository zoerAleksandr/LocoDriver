package com.z_company.route.component

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * Список внутри шторки: жест по списку — только прокрутка, шторку он не тянет никогда.
 *
 * Стандартно остаток жеста (и инерции), который список не смог потратить на прокрутку (список на
 * самом верху, оверскролл), уходит шторке — и она закрывается прямо при прокрутке. Здесь этот
 * остаток всегда съедается. Закрыть шторку свайпом вниз можно только за «ручку»/шапку — те части,
 * где нет прокручиваемого содержимого.
 *
 * [state] оставлен для читаемости вызова (какой список отделяется от шторки).
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.separateSheetFromScroll(state: ScrollableState? = null): Modifier =
    this.nestedScroll(SheetNeverDraggedByListConnection)

private object SheetNeverDraggedByListConnection : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        Offset(0f, available.y)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        Velocity(0f, available.y)
}
