package com.z_company.core.ui.component.customDatePicker

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kotlin.math.absoluteValue

@Composable
fun MyWheelTextPicker(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    startIndex: Int = 0,
    height: Dp = 128.dp,
    texts: List<String>,
    rowCount: Int,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    color: Color = LocalContentColor.current,
    contentAlignment: Alignment = Alignment.Center,
    contentArrangement: Arrangement.Horizontal = Arrangement.Center,
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
) {
    MyWheelPicker(
        modifier = modifier,
        lazyListState = lazyListState,
        startIndex = startIndex,
        count = texts.size,
        rowCount = rowCount,
        height = height,
        onScrollFinished = onScrollFinished,
        texts = texts,
        style = style,
        color = color,
        contentArrangement = contentArrangement
    )
}

@OptIn(ExperimentalMySnapperApi::class)
@Composable
fun MyWheelPicker(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    startIndex: Int = 0,
    count: Int,
    rowCount: Int,
    height: Dp = 128.dp,
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
    texts: List<String>,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    color: Color = LocalContentColor.current,
    contentArrangement: Arrangement.Horizontal = Arrangement.Center,
) {
    val snapperLayoutInfo = rememberLazyListSnapperLayoutInfo(lazyListState = lazyListState)
    val isScrollInProgress = lazyListState.isScrollInProgress

    LaunchedEffect(isScrollInProgress, count) {
        if (!isScrollInProgress) {
            onScrollFinished(calculateSnappedItemIndex(snapperLayoutInfo) ?: startIndex)?.let {
                lazyListState.animateScrollToItem(it)
            }
        }
    }

    val topBottomFade = Brush.verticalGradient(
        0f to Color.Transparent,
        0.4f to Color.Black,
        0.6f to Color.Black,
        1f to Color.Transparent
    )

    Box(
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .height(height)
                .fillMaxWidth()
                .fadingEdge(topBottomFade),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = height / rowCount * ((rowCount - 1) / 2)),
            flingBehavior = rememberSnapperFlingBehavior(
                lazyListState = lazyListState,
                // Настройка анимации для более плавной инерции
                springAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy, // Низкое сопротивление для более длительного затухания
                    stiffness = Spring.StiffnessLow // Меньшая жесткость для плавного замедления
                ),
//                maximumFlingDistance = { layoutInfo ->
//                    // Увеличьте максимальное расстояние прокрутки для более длинной инерции
//                    layoutInfo.totalItemsCount * 2f
//                }
            )
        ) {
            items(count) { index ->
                val isCentered = index == snapperLayoutInfo.currentItem?.index
                val scale = if (isCentered) 1f else 0.8f
                val alpha = if (isCentered) 1f else 0.6f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height / rowCount),
                    horizontalArrangement = contentArrangement,
                    verticalAlignment = Alignment.CenterVertically
                ) {
//                    Box(
//                        modifier = Modifier.padding(horizontal = 4.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = texts[index],
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
//                }
            }
        }
    }
}


@OptIn(ExperimentalMySnapperApi::class)
private fun calculateSnappedItemIndex(snapperLayoutInfo: SnapperLayoutInfo): Int? {
    var currentItemIndex = snapperLayoutInfo.currentItem?.index

    if (snapperLayoutInfo.currentItem?.offset != 0) {
        if (currentItemIndex != null) {
            currentItemIndex++
        }
    }
    return currentItemIndex
}


private fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }


@OptIn(ExperimentalMySnapperApi::class)
@Composable
fun calculateAnimatedAlpha(
    lazyListState: LazyListState,
    snapperLayoutInfo: SnapperLayoutInfo,
    index: Int,
    rowCount: Int
): Float {

    val distanceToIndexSnap = snapperLayoutInfo.distanceToIndexSnap(index).absoluteValue
    val layoutInfo = remember { derivedStateOf { lazyListState.layoutInfo } }.value
    val viewPortHeight = layoutInfo.viewportSize.height.toFloat()
    val singleViewPortHeight = viewPortHeight / rowCount

    return if (distanceToIndexSnap in 0..singleViewPortHeight.toInt()) {
        1.2f - (distanceToIndexSnap / singleViewPortHeight)
    } else {
        0.2f
    }
}


fun customFlingBehavior(): FlingBehavior {
    val decay = exponentialDecay<Float>(
        frictionMultiplier = 0.5f // Уменьшаем трение для более длинной инерции
    )
    return object : FlingBehavior {
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            val animationSpec = decay
            val animation = FloatExponentialDecaySpec(
                frictionMultiplier = 0.5f,
//                absVelocityThreshold =
            )

            val result = with(animation) {
                // Выполняем анимацию затухания
                val target = initialVelocity * 0.5f // Уменьшаем скорость для плавности
                scrollBy(target)
                initialVelocity
            }
            return result
        }
    }
}