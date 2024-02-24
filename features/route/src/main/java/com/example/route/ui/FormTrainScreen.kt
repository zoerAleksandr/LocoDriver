package com.example.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.core.R
import com.example.core.ResultState
import com.example.core.ui.component.AsyncData
import com.example.core.ui.component.GenericError
import com.example.core.ui.theme.custom.AppTypography
import com.example.domain.entities.route.Train
import com.example.route.component.BottomShadow
import com.example.route.component.StationItem
import com.example.route.extention.isScrollInInitialState
import com.example.route.viewmodel.StationFormState
import com.example.route.viewmodel.TrainFormUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormTrainScreen(
    formUiState: TrainFormUiState,
    currentTrain: Train?,
    onBackPressed: () -> Unit,
    onSaveClick: () -> Unit,
    onTrainSaved: () -> Unit,
    onClearAllField: () -> Unit,
    resetSaveState: () -> Unit,
    resetErrorMessage: () -> Unit,
    onNumberChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onAxleChanged: (String) -> Unit,
    onLengthChanged: (String) -> Unit,
    onAddingStation: () -> Unit,
    onDeleteStation: (StationFormState) -> Unit,
    onStationNameChanged: (index: Int, s: String) -> Unit,
    onDepartureTimeChanged: (index: Int, time: Long?) -> Unit,
    onArrivalTimeChanged: (index: Int, time: Long?) -> Unit,
    stationListState: SnapshotStateList<StationFormState>?
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            confirmValueChange = {
                it != SheetValue.Hidden
            }
        )
    )

    if (formUiState.errorMessage != null) {
        LaunchedEffect(Unit) {
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = formUiState.errorMessage
                )
                resetErrorMessage()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxWidth(),
        snackbarHost = {
            SnackbarHost(hostState = scaffoldState.snackbarHostState) { snackBarData ->
                Snackbar(snackBarData)
            }
        },
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Поезд",
                        style = AppTypography.getType().headlineSmall
                            .copy(color = MaterialTheme.colorScheme.primary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    ClickableText(
                        text = AnnotatedString(text = "Сохранить"),
                        style = AppTypography.getType().titleMedium,
                        onClick = { onSaveClick() }
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
                                    onClearAllField()
                                    dropDownExpanded = false
                                },
                                text = {
                                    Text(
                                        text = "Очистить",
                                        style = AppTypography.getType().bodyLarge
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AsyncData(resultState = formUiState.trainDetailState) {
                currentTrain?.let { train ->
                    AsyncData(resultState = formUiState.saveTrainState, errorContent = {
                        GenericError(
                            onDismissAction = resetSaveState
                        )
                    }) {
                        if (formUiState.saveTrainState is ResultState.Success) {
                            LaunchedEffect(formUiState.saveTrainState) {
                                onTrainSaved()
                            }
                        } else {
                            TrainFormScreenContent(
                                train = train,
                                onNumberChanged = onNumberChanged,
                                onWeightChanged = onWeightChanged,
                                onAxleChanged = onAxleChanged,
                                onLengthChanged = onLengthChanged,
                                onAddingStation = onAddingStation,
                                onDeleteStation = onDeleteStation,
                                onStationNameChanged = onStationNameChanged,
                                onDepartureTimeChanged = onDepartureTimeChanged,
                                onArrivalTimeChanged = onArrivalTimeChanged,
                                stationListState = stationListState
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrainFormScreenContent(
    train: Train,
    onNumberChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onAxleChanged: (String) -> Unit,
    onLengthChanged: (String) -> Unit,
    onAddingStation: () -> Unit,
    onDeleteStation: (StationFormState) -> Unit,
    onStationNameChanged: (index: Int, s: String) -> Unit,
    onDepartureTimeChanged: (index: Int, time: Long?) -> Unit,
    onArrivalTimeChanged: (index: Int, time: Long?) -> Unit,
    stationListState: SnapshotStateList<StationFormState>?
) {
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()



    AnimatedVisibility(
        modifier = Modifier
            .zIndex(1f),
        visible = !scrollState.isScrollInInitialState(),
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        BottomShadow()
    }
    LazyColumn(
        state = scrollState,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        item {
            OutlinedTextField(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .fillMaxWidth(0.5f),
                value = train.number ?: "",
                onValueChange = {
                    onNumberChanged(it)
                },
                label = {
                    Text(text = "Номер", color = MaterialTheme.colorScheme.secondary)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        scope.launch {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    }
                ),
                singleLine = true
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f),
                    value = train.weight ?: "",
                    onValueChange = {
                        onWeightChanged(it)
                    },
                    label = {
                        Text(text = "Вес", color = MaterialTheme.colorScheme.secondary)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            scope.launch {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f),
                    value = train.axle ?: "",
                    onValueChange = {
                        onAxleChanged(it)
                    },
                    label = {
                        Text(text = "Оси", color = MaterialTheme.colorScheme.secondary)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            scope.launch {
                                focusManager.moveFocus(FocusDirection.Right)
                            }
                        }
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f),
                    value = train.conditionalLength ?: "",
                    onValueChange = {
                        onLengthChanged(it)
                    },
                    label = {
                        Text(text = "у.д.", color = MaterialTheme.colorScheme.secondary)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            scope.launch {
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    singleLine = true
                )
            }
        }
        stationListState?.let { stationList ->
            itemsIndexed(
                items = stationList,
                key = { _, item -> item.id }
            ) { index, item ->
                if (index == 0) {
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.secondary_spacing)))
                } else {
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.secondary_spacing) / 2))
                }
                StationItem(
                    index = index,
                    stationFormState = item,
                    onDelete = onDeleteStation,
                    onStationNameChanged = onStationNameChanged,
                    onArrivalTimeChanged = onArrivalTimeChanged,
                    onDepartureTimeChanged = onDepartureTimeChanged,
                )
            }
        }
        item {
            ClickableText(
                modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                text = AnnotatedString("Добавить станцию"),
                style = TextStyle(textAlign = TextAlign.End)
            ) {
                onAddingStation()
                scope.launch {
                    val countItems = scrollState.layoutInfo.totalItemsCount
                    scrollState.animateScrollToItem(countItems)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}
