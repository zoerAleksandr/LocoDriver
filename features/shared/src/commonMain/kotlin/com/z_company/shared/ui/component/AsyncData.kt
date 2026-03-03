package com.z_company.shared.ui.component

import androidx.compose.runtime.Composable
import com.z_company.core.ResultState

@Composable
fun <T> AsyncData(
    resultState: ResultState<T?>?,
    loadingContent: @Composable () -> Unit = { GenericLoading() },
    errorContent: @Composable (error: ResultState.Error) -> Unit = { GenericError() },
    content: @Composable (data: T?) -> Unit
) {
    when (resultState) {
        is ResultState.Loading -> loadingContent()
        is ResultState.Error -> errorContent(resultState)
        null -> content(null)
        is ResultState.Success -> content(resultState.data)
    }
}

@Composable
fun <T> AsyncDataValue(
    resultState: ResultState<T?>?,
    loadingContent: @Composable () -> Unit = { GenericLoadingValue() },
    errorContent: @Composable () -> Unit = { GenericErrorValue() },
    content: @Composable (data: T?) -> Unit
) {
    when (resultState) {
        is ResultState.Loading -> loadingContent()
        is ResultState.Error -> errorContent()
        null -> content(null)
        is ResultState.Success -> content(resultState.data)
    }
}
