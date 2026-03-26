package com.z_company.core.ui.component.customDatePicker

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.component.AutoSizeText

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
    dampingRatio: Float = Spring.DampingRatioLowBouncy,
    stiffness: Float = Spring.StiffnessLow,
    frictionMultiplier: Float = 0.5f,
    changeWidth: (Dp) -> Unit = {},
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
        contentArrangement = contentArrangement,
        dampingRatio = dampingRatio,
        stiffness = stiffness,
//        changeWidth = changeWidth,
        frictionMultiplier = frictionMultiplier
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
    texts: List<String>,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    color: Color = LocalContentColor.current,
    contentArrangement: Arrangement.Horizontal = Arrangement.Center,
    dampingRatio: Float,
    stiffness: Float,
    frictionMultiplier: Float,
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
) {
    val snapperLayoutInfo = rememberLazyListSnapperLayoutInfo(lazyListState = lazyListState)
    val isScrollInProgress = lazyListState.isScrollInProgress
    val haptic = LocalHapticFeedback.current

    // Haptic feedback на каждом пройденном значении при скролле
    LaunchedEffect(Unit) {
        var lastIndex = startIndex
        snapshotFlow { snapperLayoutInfo.currentItem?.index }
            .collect { currentIndex ->
                if (currentIndex != null && currentIndex != lastIndex) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastIndex = currentIndex
                }
            }
    }

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
                decayAnimationSpec = exponentialDecay<Float>(frictionMultiplier = frictionMultiplier),
                springAnimationSpec = spring<Float>(
                    dampingRatio = dampingRatio,
                    stiffness = stiffness
                )
            )
        ) {
            items(count) { index ->
                val isCentered = index == snapperLayoutInfo.currentItem?.index
                val alpha = if (isCentered) 1f else 0.6f

                val textAlignment = when (contentArrangement) {
                    Arrangement.End -> Alignment.CenterEnd
                    Arrangement.Start -> Alignment.CenterStart
                    else -> Alignment.Center
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height / rowCount),
                    horizontalArrangement = contentArrangement,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AutoSizeText(
                        maxTextSize = 26.sp,
                        minTextSize = 18.sp,
                        modifier = Modifier.fillMaxWidth(),
                        text = texts[index],
                        alignment = textAlignment,
                        color = color.copy(alpha = alpha),
                        style = style
                    )
                }
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