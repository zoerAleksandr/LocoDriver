package com.z_company.route.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinearPagerIndicator(
    modifier: Modifier = Modifier,
    state: PagerState
) {
    fun PagerState.offsetForPage(page: Int) = (currentPage - page) + state.currentPageOffsetFraction

    // NEW FUNCTION FOR INDICATORS
    fun PagerState.indicatorOffsetForPage(page: Int) =
        1f - offsetForPage(page).coerceIn(-1f, 1f).absoluteValue
    Row(
        modifier = modifier
            .padding(8.dp)
            .width(32.dp * state.pageCount),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        for (i in 0 until state.pageCount) {
            val offset = state.indicatorOffsetForPage(i)
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .weight(1f + (offset * 3f))
                    .height(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}