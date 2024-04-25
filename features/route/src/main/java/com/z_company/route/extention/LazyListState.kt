package com.z_company.route.extention

import androidx.compose.foundation.lazy.LazyListState

fun LazyListState.isScrollInInitialState(): Boolean {
    return firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
}