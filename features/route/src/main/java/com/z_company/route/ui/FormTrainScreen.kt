package com.z_company.route.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.z_company.core.R
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.entities.route.Train
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.ConfirmExitDialog
import com.z_company.route.component.StationItem
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.StationFormState
import com.z_company.route.viewmodel.TrainFormUiState
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
    stationListState: SnapshotStateList<StationFormState>?,
    exitScreen: () -> Unit,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    exitWithoutSave: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    if (formUiState.errorMessage != null) {
        LaunchedEffect(Unit) {
            scope.launch {
                snackbarHostState.showSnackbar(
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
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        },
        topBar = {
            TopAppBar(
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
                    AsyncData(
                        resultState = formUiState.saveTrainState,
                        loadingContent = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        },
                        errorContent = {}
                    ) {
                        ClickableText(
                            modifier = Modifier.padding(end = 16.dp),
                            text = AnnotatedString(text = "Готово"),
                            style = AppTypography.getType().titleMedium.copy(color = MaterialTheme.colorScheme.tertiary),
                            onClick = { onSaveClick() }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        if (formUiState.saveTrainState is ResultState.Error) {
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка: ${formUiState.saveTrainState.entity.message}")
                }
                resetSaveState()
            }
        }
        if (formUiState.exitFromScreen) {
            LaunchedEffect(Unit) {
                exitScreen()
            }
        }
        Box(modifier = Modifier.padding(paddingValues)) {
            AsyncData(resultState = formUiState.trainDetailState) {
                currentTrain?.let { train ->
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
                            stationListState = stationListState,
                            changeShowConfirmExitDialog = changeShowConfirmExitDialog,
                            onSaveClick = onSaveClick,
                            exitWithoutSave = exitWithoutSave,
                            showConfirmExitDialog = formUiState.confirmExitDialogShow
                        )
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
    stationListState: SnapshotStateList<StationFormState>?,
    showConfirmExitDialog: Boolean,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    exitWithoutSave: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

    if (showConfirmExitDialog) {
        ConfirmExitDialog(
            showExitConfirmDialog = changeShowConfirmExitDialog,
            onSaveClick = onSaveClick,
            exitWithoutSave = exitWithoutSave
        )
    }

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
        horizontalAlignment = Alignment.End,
        contentPadding = PaddingValues(16.dp)
    ) {
//        item { Spacer(modifier = Modifier.height(70.dp)) }

        item {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(0.5f),
                value = train.number ?: "",
                onValueChange = {
                    onNumberChanged(it)
                },
                placeholder = {
                    Text(text = "Номер", style = dataTextStyle)
                },
                prefix = {
                    if (!train.number.isNullOrBlank()) {
                        Text(text = "№ ", style = hintStyle)
                    }
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
                textStyle = dataTextStyle,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = Shapes.medium,
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
                    placeholder = {
                        Text(text = "Вес", style = dataTextStyle)
                    },
                    suffix = {
                        if (!train.weight.isNullOrBlank()) {
                            Text(text = "т.", style = hintStyle)
                        }
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
                    textStyle = dataTextStyle,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = Shapes.medium,
                )

                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f),
                    value = train.axle ?: "",
                    onValueChange = {
                        onAxleChanged(it)
                    },
                    placeholder = {
                        Text(text = "Оси", style = dataTextStyle)
                    },
                    suffix = {
                        if (!train.axle.isNullOrBlank()) {
                            Text(text = "осей", style = hintStyle)
                        }
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
                    textStyle = dataTextStyle,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = Shapes.medium,
                )

                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f),
                    value = train.conditionalLength ?: "",
                    onValueChange = {
                        onLengthChanged(it)
                    },
                    placeholder = {
                        Text(text = "у.д.", style = dataTextStyle)
                    },
                    suffix = {
                        if (!train.conditionalLength.isNullOrBlank()) {
                            Text(text = "у.д.", style = hintStyle)
                        }
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
                    textStyle = dataTextStyle,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = Shapes.medium,
                )
            }
        }
        stationListState?.let { stationList ->
            itemsIndexed(
                items = stationList,
                key = { _, item -> item.id }
            ) { index, item ->
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.secondary_spacing)))

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
            Button(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                shape = Shapes.medium,
                onClick = {
                    onAddingStation()
                    scope.launch {
                        val countItems = scrollState.layoutInfo.totalItemsCount
                        scrollState.animateScrollToItem(countItems)
                    }
                }
            ) {
                Text(text = "Добавить станцию", style = subTitleTextStyle)
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
