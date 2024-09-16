package com.z_company.core.ui.component

import androidx.compose.runtime.Composable
import com.z_company.core.ResultState
import com.z_company.domain.entities.SearchStateScreen

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

@Composable
fun <T> SearchAsyncData(
    resultState: SearchStateScreen<T?>?,
    loadingContent: @Composable () -> Unit = { GenericLoading() },
    errorContent: @Composable () -> Unit = { GenericError() },
    inputContent: @Composable () -> Unit,
    content: @Composable (data: T?) -> Unit
) {
    resultState.let { state ->
        when (state) {
            is SearchStateScreen.Loading -> {
                loadingContent()
            }

            is SearchStateScreen.Failure -> {
                errorContent()
            }

            null -> {
                content(null)
            }

            is SearchStateScreen.Success -> {
                content(state.data)
            }
            is SearchStateScreen.Input -> {
                inputContent()
            }
        }
    }
}

@Composable
fun <T> AsyncDataValue(
    resultState: ResultState<T?>?,
    loadingContent: @Composable () -> Unit = { GenericLoadingValue() },
    errorContent: @Composable () -> Unit = { GenericErrorValue() },
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