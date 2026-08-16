package com.z_company.route.component

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex

/** Классический pull-to-refresh, связанный с полной ручной синхронизацией. */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PullToSyncContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    message: String? = null,
    onMessageShown: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberPullRefreshState(isRefreshing, onRefresh)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Box(modifier = modifier.fillMaxSize().pullRefresh(state)) {
        Box(Modifier.fillMaxSize()) { content() }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.tertiary,
            scale = true,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(20f),
        )
    }
}
