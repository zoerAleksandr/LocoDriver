package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.z_company.core.R
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.entities.ServicePhase
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.trainCategory
import com.z_company.route.component.AnimationDialog
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
    resetSaveState: () -> Unit,
    resetErrorMessage: () -> Unit,
    onNumberChanged: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
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
    exitWithoutSave: () -> Unit,
    menuList: List<String>,
    isExpandedMenu: Pair<Int, Boolean>?,
    onExpandedMenuChange: (Int, Boolean) -> Unit,
    onChangedContentMenu: (Int, String) -> Unit,
    onDeleteStationName: (String) -> Unit,
    servicePhaseList: List<ServicePhase>,
    isShowDialogSelectServicePhase: Boolean,
    onShowDialogSelectServicePhase: () -> Unit,
    onHideDialogSelectServicePhase: () -> Unit,
    onSelectServicePhase: (ServicePhase?) -> Unit,
    selectedServicePhase: ServicePhase?,
    onSettingClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val titleStyle = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
//    if (formUiState.errorMessage != null) {
//        LaunchedEffect(Unit) {
//            scope.launch {
//                snackbarHostState.showSnackbar(
//                    message = formUiState.errorMessage
//                )
//                resetErrorMessage()
//            }
//        }
//    }

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
                        style = titleStyle
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
                        TextButton(
                            modifier = Modifier
                                .padding(end = 16.dp),
                            enabled = formUiState.changesHaveState,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.tertiary
                            ),
                            onClick = { onSaveClick() }
                        ) {
                            Text(text = "Сохранить", style = hintStyle)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        when(formUiState.saveTrainState){
            is ResultState.Success -> {
                LaunchedEffect(formUiState.saveTrainState) {
                    onTrainSaved()
                }
            }
            is ResultState.Error -> {
                LaunchedEffect(Unit) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Ошибка: ${formUiState.saveTrainState.entity.message}")
                    }
                    resetSaveState()
                }
            }
            else -> {}
        }

        if (formUiState.exitFromScreen) {
            LaunchedEffect(Unit) {
                exitScreen()
            }
        }
        Box(modifier = Modifier.padding(paddingValues)) {
            AsyncData(resultState = formUiState.trainDetailState) {
                currentTrain?.let { train ->
                    TrainFormScreenContent(
                        train = train,
                        onNumberChanged = onNumberChanged,
                        onDistanceChange = onDistanceChange,
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
                        showConfirmExitDialog = formUiState.confirmExitDialogShow,
                        menuList = menuList,
                        isExpandedMenu = isExpandedMenu,
                        onChangedContentMenu = onChangedContentMenu,
                        onExpandedMenuChange = onExpandedMenuChange,
                        onDeleteStationName = onDeleteStationName,
                        servicePhaseList = servicePhaseList,
                        isShowDialogSelectServicePhase = isShowDialogSelectServicePhase,
                        onShowDialogSelectServicePhase = onShowDialogSelectServicePhase,
                        onHideDialogSelectServicePhase = onHideDialogSelectServicePhase,
                        onSelectServicePhase = onSelectServicePhase,
                        selectedServicePhase = selectedServicePhase,
                        onSettingClick = onSettingClick,
                        errorMessage = formUiState.errorMessage
                    )
                }
            }
        }
    }
}

@Composable
fun TrainFormScreenContent(
    train: Train,
    onNumberChanged: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
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
    menuList: List<String>,
    isExpandedMenu: Pair<Int, Boolean>?,
    onExpandedMenuChange: (Int, Boolean) -> Unit,
    onChangedContentMenu: (Int, String) -> Unit,
    onDeleteStationName: (String) -> Unit,
    servicePhaseList: List<ServicePhase>,
    isShowDialogSelectServicePhase: Boolean,
    onShowDialogSelectServicePhase: () -> Unit,
    onHideDialogSelectServicePhase: () -> Unit,
    onSelectServicePhase: (ServicePhase?) -> Unit,
    selectedServicePhase: ServicePhase?,
    onSettingClick: () -> Unit,
    errorMessage: String?
) {
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var isTrainInfoVisible by remember {
        mutableStateOf(false)
    }

    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 17.sp,
            fontWeight = FontWeight.Light
        )

    AnimationDialog(
        showDialog = isShowDialogSelectServicePhase,
        onDismissRequest = onHideDialogSelectServicePhase
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onHideDialogSelectServicePhase()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )
                    .clickable { }
                    .padding(horizontal = 8.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                itemsIndexed(
                    items = servicePhaseList,
                    key = { _, item -> item.id }
                ) { index, item ->
                    if (index != 0) {
                        HorizontalDivider()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectServicePhase(item)
                                focusManager.clearFocus()
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {

                        Text(
                            text = "${item.departureStation} - ${item.arrivalStation}",
                            style = dataTextStyle,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${item.distance} км",
                            style = dataTextStyle,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                item {
                    if (servicePhaseList.isEmpty()) {
                        Text(
                            modifier = Modifier.padding(start = 16.dp),
                            text = "Список пуст",
                            style = dataTextStyle,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                item {
                    val link = buildAnnotatedString {
                        val text = "Редактировать список"

                        val endIndex = text.lastIndex
                        val startIndex = 0

                        append(text)

                        addStringAnnotation(
                            tag = LINK_TO_SETTING,
                            annotation = LINK_TO_SETTING,
                            start = startIndex,
                            end = endIndex
                        )
                    }
                    ClickableText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, end = 12.dp),
                        text = link,
                        style = hintStyle.copy(
                            color = MaterialTheme.colorScheme.tertiary,
                            textAlign = TextAlign.End
                        )
                    ) {
                        link.getStringAnnotations(LINK_TO_SETTING, it, it).firstOrNull()?.let {
                            onHideDialogSelectServicePhase()
                            onSettingClick()
                        }
                    }
                }
            }
        }
    }

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
        item {
            errorMessage?.let {
                val errorTextStyle = AppTypography.getType().titleMedium.copy(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onError)
                val widthScreen = LocalConfiguration.current.screenWidthDp.toFloat()
                val gradient = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    ),
                    center = Offset(Float.POSITIVE_INFINITY, 0f),
                    radius = widthScreen * 2
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = gradient,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            style = errorTextStyle
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f),
                        value = train.distance ?: "",
                        onValueChange = {
                            onDistanceChange(it)
                        },
                        placeholder = {
                            Text(text = "Плечо", style = dataTextStyle)
                        },
                        suffix = {
                            Text(text = "км", style = hintStyle)
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

                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f),
                        value = train.number ?: "",
                        onValueChange = {
                            onNumberChanged(it)
                            if (it.isEmpty()) {
                                isTrainInfoVisible = false
                            }
                        },
                        placeholder = {
                            Text(text = "Номер", style = dataTextStyle)
                        },
                        prefix = {
                            if (!train.number.isNullOrBlank()) {
                                Text(text = "№ ", style = hintStyle)
                            }
                        },
                        trailingIcon = {
                            if (!train.number.isNullOrBlank()) {
                                Icon(
                                    modifier = Modifier.clickable {
                                        focusManager.clearFocus()
                                        isTrainInfoVisible = !isTrainInfoVisible
                                    },
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
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
                AnimatedVisibility(visible = isTrainInfoVisible) {
                    Text(
                        modifier = Modifier.padding(vertical = 4.dp),
                        text = train.trainCategory(),
                        style = hintStyle
                    )
                }
            }
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
                            Text(text = "о.", style = hintStyle)
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
        item {
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clickable {
                        onShowDialogSelectServicePhase()
                    }
                    .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val text = if (selectedServicePhase == null) {
                    "Выбрать плечо"
                } else {
                    "${selectedServicePhase.departureStation} - ${selectedServicePhase.arrivalStation}"
                }
                Text(
                    text = AnnotatedString(text = text),
                    style = dataTextStyle,
                    overflow = TextOverflow.Ellipsis
                )
                if (selectedServicePhase != null) {
                    Icon(
                        modifier = Modifier.clickable {
                            onSelectServicePhase(null)
                        },
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = null
                    )
                }
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
                    menuList = menuList,
                    isExpandedMenu = if (isExpandedMenu?.first == index) {
                        isExpandedMenu.second
                    } else false,
                    onExpandedMenuChange = onExpandedMenuChange,
                    onChangedContentMenu = onChangedContentMenu,
                    onStationNameChanged = onStationNameChanged,
                    onArrivalTimeChanged = onArrivalTimeChanged,
                    onDepartureTimeChanged = onDepartureTimeChanged,
                    onDeleteStationName = onDeleteStationName,
                    onSettingClick = onSettingClick
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
