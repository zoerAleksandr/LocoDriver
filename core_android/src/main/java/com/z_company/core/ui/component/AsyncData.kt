package com.z_company.core.ui.component

import androidx.compose.runtime.Composable
import com.z_company.core.ResultState

@Composable
fun <T> AsyncData(
    resultState: ResultState<T?>?,
    loadingContent: @Composable () -> Unit = { GenericLoading() },
    errorContent: @Composable () -> Unit = { GenericError() },
    content: @Composable (data: T?) -> Unit
) {
    resultState.let { state ->
        when (state) {
            is ResultState.Loading -> {
                loadingContent()
            }
            is ResultState.Error -> {
                errorContent()
            }
            null -> {
                content(null)
            }
            is ResultState.Success -> {
                content(state.data)
            }
        }
    }
}