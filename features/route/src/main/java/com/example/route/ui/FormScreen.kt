package com.example.route.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.core.ResultState
import com.example.core.ui.component.AsyncData
import com.example.core.ui.component.GenericError
import com.example.domain.entities.route.Route
import com.example.route.viewmodel.RouteFormUiState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    formUiState: RouteFormUiState,
    currentRoute: Route?,
    onBackPressed: () -> Unit,
    onRouteSaved: () -> Unit,
    onSaveClick: () -> Unit,
    onClearAllField: () -> Unit,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit
) {
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Маршрут",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    ClickableText(
                        text = AnnotatedString(text = "Сохранить"),
                        onClick = { onSaveClick.invoke() }

                    )
                    var dropDownExpanded by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = {
                            dropDownExpanded = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню"
                        )
                        DropdownMenu(
                            expanded = dropDownExpanded,
                            onDismissRequest = { dropDownExpanded = false },
                            offset = DpOffset(x = 4.dp, y = 8.dp)
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = {
                                    onClearAllField.invoke()
                                    dropDownExpanded = false
                                },
                                text = {
                                    Text(
                                        text = "Очистить",
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
    ) {
        Box(Modifier.padding(it)) {
            AsyncData(resultState = formUiState.routeDetailState) {
                currentRoute?.let { route ->
                    AsyncData(
                        resultState = formUiState.saveRouteState,
                        errorContent = {
                            GenericError(
                                onDismissAction = resetSaveState
                            )
                        }
                    ) {
                        if (formUiState.saveRouteState is ResultState.Success) {
                            LaunchedEffect(formUiState.saveRouteState) {
                                onRouteSaved()
                            }
                        } else {
                            RouteFormScreenContent(
                                route = route,
                                onNumberChanged = onNumberChanged
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RouteFormScreenContent(
    route: Route?,
    onNumberChanged: (String) -> Unit
) {
    Column {
        val keyboardController = LocalSoftwareKeyboardController.current
        OutlinedTextField(
            value = route?.basicData?.number ?: "",
            onValueChange = onNumberChanged,
            singleLine = true,
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words),
        )
        var check by remember {
            mutableStateOf(false)
        }
        Switch(checked = check, onCheckedChange = {
            check = it
            onNumberChanged("300")
        })
    }
}