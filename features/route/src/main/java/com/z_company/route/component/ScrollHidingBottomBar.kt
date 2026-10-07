package com.z_company.route.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset

/** Видимость нижней панели действий: уезжает вниз при прокрутке списка вверх, возвращается при прокрутке назад. */
@Stable
class BottomBarScrollState {
    var visible by mutableStateOf(true)
    val nestedScroll = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < -3f) visible = false
            else if (available.y > 3f) visible = true
            return Offset.Zero
        }
    }
}

@Composable
fun rememberBottomBarScrollState() = remember { BottomBarScrollState() }

/**
 * Общая анимация нижней панели действий (форма «Поезд», список плеч): сдвиг вниз на высоту панели
 * за 600 мс, FastOutSlowIn. Панель выравнивается по нижнему краю родителя-Box вызывающим кодом
 * через [modifier] (`Modifier.align(Alignment.BottomCenter)`).
 */
@Composable
fun ScrollHidingBottomBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var barHeightPx by remember { mutableIntStateOf(0) }
    val offsetY by animateIntAsState(
        targetValue = if (visible) 0 else barHeightPx,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "bottomBarOffset",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { barHeightPx = it.height }
            .offset { IntOffset(0, offsetY) },
    ) { content() }
}
