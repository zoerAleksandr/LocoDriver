package com.z_company.core.ui.component.customDatePicker


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.component.AutoSizeText
import kotlin.math.absoluteValue

@Composable
fun MyWheelTextPicker(
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    height: Dp = 128.dp,
    texts: List<String>,
    rowCount: Int,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    color: Color = LocalContentColor.current,
    contentAlignment: Alignment = Alignment.Center,
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
) {
    MyWheelPicker(
        modifier = modifier,
        startIndex = startIndex,
        count = texts.size,
        rowCount = rowCount,
        height = height,
        onScrollFinished = onScrollFinished,
        texts = texts,
        style = style,
        color = color,
        contentAlignment = contentAlignment
    )
}

@OptIn(ExperimentalMySnapperApi::class)
@Composable
fun MyWheelPicker(
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    count: Int,
    rowCount: Int,
    height: Dp = 128.dp,
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
    texts: List<String>,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    color: Color = LocalContentColor.current,
    contentAlignment: Alignment = Alignment.Center,
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState(startIndex)
    val snapperLayoutInfo = rememberLazyListSnapperLayoutInfo(lazyListState = lazyListState)
    val isScrollInProgress = lazyListState.isScrollInProgress

    LaunchedEffect(isScrollInProgress, count) {
        if (!isScrollInProgress) {
            onScrollFinished(calculateSnappedItemIndex(snapperLayoutInfo) ?: startIndex)?.let {
                lazyListState.scrollToItem(it)
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
                .wrapContentWidth()
                .fadingEdge(topBottomFade),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = height / rowCount * ((rowCount - 1) / 2)),
            flingBehavior = rememberSnapperFlingBehavior(
                lazyListState = lazyListState
            )
        ) {
            items(count) { index ->
                Box(
                    modifier = Modifier
                        .height(height / rowCount)
                        .fillMaxWidth(),
                    contentAlignment = contentAlignment
                ) {
                    AutoSizeText(
                        text = texts[index],
                        minTextSize = 10.sp,
                        maxTextSize = 18.sp,
                        color = if (calculateSnappedItemIndex(snapperLayoutInfo) == index) color else MaterialTheme.colorScheme.onSurface,
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