package com.z_company.route.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.z_company.route.component.StationDropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.z_company.core.R
import com.z_company.core.ResultState
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.TrainAssist
import com.z_company.domain.entities.route.UtilsForEntities.trainCategory
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.StationEditBottomSheet
import com.z_company.route.component.TrainStationTimeline
import com.z_company.route.component.toTimelineItems
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.StationFormState
import com.z_company.route.viewmodel.TrainFormUiState
import com.z_company.route.viewmodel.TrainFormViewModel
import kotlinx.coroutines.launch

@SuppressLint("SuspiciousIndentation")
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun FormTrainScreen(
    viewModel: TrainFormViewModel,
    formUiState: TrainFormUiState,
    currentTrain: Train?,
    onTrainSaved: () -> Unit,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onAxleChanged: (String) -> Unit,
    onLengthChanged: (String) -> Unit,
    stationListState: SnapshotStateList<StationFormState>?,
    menuList: List<String>,
    servicePhaseList: List<ServicePhase>,
    onSelectServicePhase: (ServicePhase?) -> Unit,
    selectedServicePhase: ServicePhase?,
    onSettingClick: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hintStyle = MaterialTheme.typography.bodyMedium
    val dataTextStyle = MaterialTheme.typography.bodyLarge

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val noValueColor = primaryColor.copy(alpha = 0.5f)
    var showSettingsSheet by remember { mutableStateOf(false) }

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = "Поезд",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onTrainSaved()
                        },
                    ) {
                        Text(
                            text = "‹",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    val hasAnyAssist = currentTrain?.let {
                        it.pusher != null || it.doubleTraction != null || it.doubledTrain != null
                    } ?: false

                    Box {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(
                                painter = painterResource(com.z_company.route.R.drawable.settings_24px),
                                contentDescription = "Настройки",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (hasAnyAssist) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp),
                                containerColor = Color(0xFFf1642e)
                            )
                        }
                    }
                },
            )
        }
    ) { paddingValues ->
        if (formUiState.saveTrainState is ResultState.Error) {
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка: ${(formUiState.saveTrainState as ResultState.Error).entity.message}")
                }
                resetSaveState()
            }
        }

        var isTrainInfoVisible by remember {
            mutableStateOf(false)
        }

        var showSelectServicePhase by remember { mutableStateOf(false) }

        // BottomSheet для редактирования/добавления станции
        val editingIndex = formUiState.editingStationIndex
        if (editingIndex != null) {
            val editingStation = if (editingIndex >= 0 && stationListState != null
                && editingIndex in stationListState.indices
            ) {
                stationListState[editingIndex]
            } else null

            StationEditBottomSheet(
                stationFormState = editingStation,
                menuList = menuList,
                onFilterMenu = {
                    viewModel.onChangedDropDownContent(
                        editingIndex.coerceAtLeast(0),
                        it
                    )
                },
                onDeleteStationName = { viewModel.removeStationName(it) },
                onSave = { name, arrival, departure, trackNumber ->
                    viewModel.saveStationFromSheet(editingIndex, name, arrival, departure, trackNumber)
                },
                onDelete = if (editingIndex >= 0) {
                    { viewModel.requestDeleteStation(editingIndex) }
                } else null,
                onDismiss = { viewModel.stopEditingStation() },
                dateAndTimeConverter = dateAndTimeConverter
            )
        }

        // Подтверждение удаления станции — ОТДЕЛЬНЫЙ sheetState, чтобы не конфликтовать
        // с общим sheetState (StationEditBottomSheet и др.). Из-за shared state ранее
        // bottom-sheet не появлялся — оставался "в hidden" с прошлого использования.
        if (formUiState.confirmDeleteStationIndex != null) {
            val deleteIndex = formUiState.confirmDeleteStationIndex!!
            val stationName = stationListState
                ?.getOrNull(deleteIndex)
                ?.station?.data

            val title = if (!stationName.isNullOrBlank())
                "Удалить станцию $stationName?"
            else
                "Удалить станцию?"

            val confirmDeleteSheetState = androidx.compose.material3.rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            )

            AppBottomSheet(
                onDismissRequest = { viewModel.cancelDeleteStation() },
                sheetState = confirmDeleteSheetState,
                title = title,
                actions = listOf(
                    BottomSheetAction(text = "Да, удалить") {
                        // deleteIndex захвачен до того как onDismissRequest обнулит confirmDeleteStationIndex
                        viewModel.deleteStationFromSheet(deleteIndex)
                    }
                )
            )
        }

        // Настройки поезда
        if (showSettingsSheet) {
            val settingsSheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            )
            ModalBottomSheet(
                onDismissRequest = {
                    showSettingsSheet = false
                    viewModel.saveAssistSeries()
                },
                sheetState = settingsSheetState,
                containerColor = MaterialTheme.colorScheme.secondary,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        )
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Настройки поезда",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TrainAssistSection(
                        title = "Толкач",
                        assist = currentTrain?.pusher,
                        onAdd = viewModel::addPusher,
                        onRemove = viewModel::removePusher,
                        onSeriesChange = viewModel::setPusherSeries,
                        onNumberChange = viewModel::setPusherNumber,
                        onDriverNameChange = viewModel::setPusherDriverName,
                        onNotesChange = viewModel::setPusherNotes,
                        hintStyle = hintStyle,
                        dataTextStyle = dataTextStyle,
                        primaryColor = primaryColor,
                        noValueColor = noValueColor,
                        seriesMenuList = viewModel.dropDownSeriesList,
                        isSeriesMenuExpanded = formUiState.expandedSeriesSectionId == "pusher",
                        onSeriesMenuExpandedChange = { expanded ->
                            viewModel.changeSeriesMenuExpanded(
                                "pusher",
                                expanded
                            )
                        },
                        onSeriesMenuContentChange = { content ->
                            viewModel.onChangedSeriesDropDown(
                                "pusher",
                                content
                            )
                        },
                        onDeleteSeries = viewModel::removeSeries
                    )

                    TrainAssistSection(
                        title = "Двойная тяга",
                        assist = currentTrain?.doubleTraction,
                        onAdd = viewModel::addDoubleTraction,
                        onRemove = viewModel::removeDoubleTraction,
                        onSeriesChange = viewModel::setDoubleTractionSeries,
                        onNumberChange = viewModel::setDoubleTractionNumber,
                        onDriverNameChange = viewModel::setDoubleTractionDriverName,
                        onNotesChange = viewModel::setDoubleTractionNotes,
                        hintStyle = hintStyle,
                        dataTextStyle = dataTextStyle,
                        primaryColor = primaryColor,
                        noValueColor = noValueColor,
                        seriesMenuList = viewModel.dropDownSeriesList,
                        isSeriesMenuExpanded = formUiState.expandedSeriesSectionId == "doubleTraction",
                        onSeriesMenuExpandedChange = { expanded ->
                            viewModel.changeSeriesMenuExpanded(
                                "doubleTraction",
                                expanded
                            )
                        },
                        onSeriesMenuContentChange = { content ->
                            viewModel.onChangedSeriesDropDown(
                                "doubleTraction",
                                content
                            )
                        },
                        onDeleteSeries = viewModel::removeSeries
                    )

                    DoubledTrainSection(
                        assist = currentTrain?.doubledTrain,
                        onAdd = viewModel::addDoubledTrain,
                        onRemove = viewModel::removeDoubledTrain,
                        onSeriesChange = viewModel::setDoubledTrainSeries,
                        onNumberChange = viewModel::setDoubledTrainNumber,
                        onDriverNameChange = viewModel::setDoubledTrainDriverName,
                        onNotesChange = viewModel::setDoubledTrainNotes,
                        onIsFirstChange = viewModel::setDoubledTrainIsFirst,
                        hintStyle = hintStyle,
                        dataTextStyle = dataTextStyle,
                        primaryColor = primaryColor,
                        noValueColor = noValueColor,
                        seriesMenuList = viewModel.dropDownSeriesList,
                        isSeriesMenuExpanded = formUiState.expandedSeriesSectionId == "doubledTrain",
                        onSeriesMenuExpandedChange = { expanded ->
                            viewModel.changeSeriesMenuExpanded(
                                "doubledTrain",
                                expanded
                            )
                        },
                        onSeriesMenuContentChange = { content ->
                            viewModel.onChangedSeriesDropDown(
                                "doubledTrain",
                                content
                            )
                        },
                        onDeleteSeries = viewModel::removeSeries
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        if (formUiState.showCreateServicePhaseSheet) {
            var newPhaseDistance by remember {
                mutableStateOf(currentTrain?.distance?.takeIf { it != "0" } ?: "")
            }
            var createReverse by remember { mutableStateOf(false) }
            val createPhaseSheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            )
            ModalBottomSheet(
                onDismissRequest = { viewModel.cancelCreateServicePhaseSheet() },
                sheetState = createPhaseSheetState,
                containerColor = MaterialTheme.colorScheme.secondary,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        )
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = "Новое плечо",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryColor
                    )
                    Text(
                        text = "${formUiState.suggestedDepartureStation} — ${formUiState.suggestedArrivalStation}",
                        style = dataTextStyle,
                        color = primaryColor
                    )
                    OutlinedTextFieldApp(
                        modifier = Modifier.fillMaxWidth(),
                        value = newPhaseDistance,
                        onValueChange = { newPhaseDistance = it },
                        placeholder = {
                            Text(
                                text = "Расстояние",
                                style = dataTextStyle,
                                color = noValueColor
                            )
                        },
                        suffix = {
                            Text(text = "км", style = hintStyle, color = noValueColor)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        textStyle = dataTextStyle,
                        singleLine = true,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Создать плечо в обратном направлении",
                            style = hintStyle,
                            color = primaryColor,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )
                        Switch(
                            checked = createReverse,
                            onCheckedChange = { createReverse = it }
                        )
                    }

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        shape = Shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        enabled = newPhaseDistance.isNotBlank() && newPhaseDistance.toIntOrNull() != null && newPhaseDistance.toInt() > 0,
                        onClick = { viewModel.createServicePhaseAndSave(newPhaseDistance, createReverse) }
                    ) {
                        Text(
                            text = "Добавить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.skipServicePhaseAndSave() }
                    ) {
                        Text(
                            text = "Пропустить",
                            style = MaterialTheme.typography.bodySmall,
                            color = primaryColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (showSelectServicePhase) {
            ModalBottomSheet(
                onDismissRequest = { showSelectServicePhase = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.secondary,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        )
                    }
                }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Плечи",
                                style = MaterialTheme.typography.titleSmall,
                                color = primaryColor,
                                maxLines = 2,
                                overflow = TextOverflow.Visible
                            )
                            Icon(
                                modifier = Modifier.clickable(
                                    onClick = onSettingClick
                                ),
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = null,
                                tint = primaryColor
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    itemsIndexed(
                        items = servicePhaseList,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        if (index != 0) {
                            CustomDivider(orientation = Orientation.Horizontal)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectServicePhase(item)
                                    showSelectServicePhase = false
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "${item.departureStation} - ${item.arrivalStation}",
                                style = dataTextStyle,
                                color = primaryColor,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "${item.distance} км",
                                style = dataTextStyle,
                                color = primaryColor,
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
                                color = primaryColor,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
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

        Box(modifier = Modifier.padding(paddingValues)) {
            currentTrain?.let { train ->
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.testTag("form_train_lazy_column"),
                    horizontalAlignment = Alignment.End,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        formUiState.errorMessage?.let {
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
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = it,
                                        style = hintStyle,
                                        color = MaterialTheme.colorScheme.onError
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
                            // Дополнительные номера поезда
                            if (train.additionalNumbers.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        6.dp,
                                        Alignment.End
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    train.additionalNumbers.forEachIndexed { index, num ->
                                        Row(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceDim,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "№ $num",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = primaryColor
                                            )
                                            Icon(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        viewModel.removeAdditionalNumber(
                                                            index
                                                        )
                                                    },
                                                painter = painterResource(R.drawable.ic_clear),
                                                contentDescription = "Удалить",
                                                tint = noValueColor
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextFieldApp(
                                    modifier = Modifier
                                        .weight(0.7f),
                                    value = train.distance?.takeIf { it != "0" } ?: "",
                                    onValueChange = {
                                        onDistanceChange(it)
                                    },
                                    placeholder = {
                                        Text(
                                            text = "Плечо",
                                            maxLines = 1,
                                            style = LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            ),
                                            color = noValueColor
                                        )
                                    },
                                    suffix = {
                                        val distanceValue =
                                            train.distance?.takeIf { it != "0" } ?: ""
                                        if (distanceValue.isNotBlank()) {
                                            Text(
                                                text = "км",
                                                style = hintStyle,
                                                color = noValueColor
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
                                                focusManager.moveFocus(FocusDirection.Right)
                                            }
                                        }
                                    ),
                                    textStyle = dataTextStyle,
                                    singleLine = true,
                                )

                                var numberFieldValue by remember {
                                    mutableStateOf(
                                        TextFieldValue(
                                            text = train.number ?: "",
                                            selection = TextRange(train.number?.length ?: 0)
                                        )
                                    )
                                }

                                // Синхронизация при внешнем изменении (загрузка данных)
                                LaunchedEffect(train.number) {
                                    val current = numberFieldValue.text
                                    val external = train.number ?: ""
                                    if (current != external) {
                                        numberFieldValue = TextFieldValue(
                                            text = external,
                                            selection = TextRange(external.length)
                                        )
                                    }
                                }

                                OutlinedTextFieldApp(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                numberFieldValue = numberFieldValue.copy(
                                                    selection = TextRange(numberFieldValue.text.length)
                                                )
                                            } else {
                                                numberFieldValue = numberFieldValue.copy(
                                                    selection = TextRange(0)
                                                )
                                            }
                                        },
                                    value = numberFieldValue,
                                    onValueChange = {
                                        numberFieldValue = it
                                        onNumberChanged(it.text)
                                        if (it.text.isEmpty()) {
                                            isTrainInfoVisible = false
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            text = "Номер",
                                            style = LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            ),
                                            color = noValueColor
                                        )
                                    },
                                    prefix = {
                                        if (!train.number.isNullOrBlank()) {
                                            Text(
                                                text = "№ ",
                                                style = LocalTextStyle.current.copy(
                                                    fontWeight = FontWeight.Light
                                                ),
                                                color = noValueColor
                                            )
                                        }
                                    },
                                    suffix = {
                                        if (!train.number.isNullOrBlank()) {
                                            Icon(
                                                modifier = Modifier.clickable {
                                                    focusManager.clearFocus()
                                                    isTrainInfoVisible = !isTrainInfoVisible
                                                },
                                                painter = painterResource(com.z_company.route.R.drawable.info_24px),
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
                                                focusManager.moveFocus(FocusDirection.Right)
                                            }
                                        }
                                    ),
                                    textStyle = dataTextStyle,
                                    singleLine = true,
                                )

                                // Кнопка "+" для добавления дополнительного номера
                                IconButton(
                                    onClick = {
                                        val num = train.number?.trim()
                                        if (!num.isNullOrBlank()) {
                                            viewModel.addAdditionalNumber(num)
                                            onNumberChanged("")
                                        }
                                    },
                                    enabled = !train.number.isNullOrBlank()
                                ) {
                                    Icon(
                                        painter = painterResource(com.z_company.route.R.drawable.add_circle_24px),
                                        contentDescription = "Добавить номер",
                                        tint = if (!train.number.isNullOrBlank())
                                            MaterialTheme.colorScheme.tertiary
                                        else
                                            noValueColor
                                    )
                                }
                            }
                            AnimatedVisibility(visible = isTrainInfoVisible) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceDim,
                                            shape = Shapes.medium
                                        )
                                        .border(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            shape = Shapes.medium
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = train.trainCategory(),
                                        style = hintStyle,
                                        color = primaryColor
                                    )
                                }
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
                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .weight(1f),
                                value = train.weight?.takeIf { it != "0" } ?: "",
                                onValueChange = {
                                    onWeightChanged(it)
                                },
                                placeholder = {
                                    Text(
                                        text = "Вес",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor
                                    )
                                },
                                suffix = {
                                    if (!train.weight.isNullOrBlank() && train.weight != "0") {
                                        Text(
                                            text = "т.",
                                            style = hintStyle,
                                            color = noValueColor
                                        )
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
                            )

                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .weight(1f),
                                value = train.axle?.takeIf { it != "0" } ?: "",
                                onValueChange = {
                                    onAxleChanged(it)
                                },
                                placeholder = {
                                    Text(
                                        text = "Оси",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor
                                    )
                                },
                                suffix = {
                                    if (!train.axle.isNullOrBlank() && train.axle != "0") {
                                        Text(
                                            text = "о.",
                                            style = hintStyle,
                                            color = noValueColor
                                        )
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
                            )

                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .weight(1f),
                                value = train.conditionalLength?.takeIf { it != "0" }
                                    ?.let { s ->
                                        val d = s.toDoubleOrNull()
                                        if (d != null && d == kotlin.math.floor(d)) d.toLong().toString() else s
                                    } ?: "",
                                onValueChange = {
                                    onLengthChanged(it)
                                },
                                placeholder = {
                                    Text(
                                        text = "у.д.",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor
                                    )
                                },
                                suffix = {
                                    if (!train.conditionalLength.isNullOrBlank() && train.conditionalLength != "0") {
                                        Text(
                                            text = "у.д.",
                                            style = hintStyle,
                                            color = noValueColor
                                        )
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
                            )
                        }
                    }
                    item {
                        val animatedBackgroundColors by animateColorAsState(
                            targetValue = if (selectedServicePhase == null) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.secondary,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )

                        Row(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                .fillMaxWidth()
                                .clickable {
                                    showSelectServicePhase = true
                                }
                                .background(
                                    color = animatedBackgroundColors,
                                    shape = Shapes.medium
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val text = if (selectedServicePhase == null) {
                                "Выбрать плечо"
                            } else {
                                "${selectedServicePhase.departureStation} - ${selectedServicePhase.arrivalStation}"
                            }

                            val color = if (selectedServicePhase == null) {
                                noValueColor
                            } else {
                                primaryColor
                            }
                            Text(
                                text = AnnotatedString(text = text),
                                style = dataTextStyle,
                                overflow = TextOverflow.Ellipsis,
                                color = color
                            )
                            if (selectedServicePhase != null) {
                                Icon(
                                    modifier = Modifier.clickable {
                                        onSelectServicePhase(null)
                                    },
                                    painter = painterResource(R.drawable.ic_clear),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    item {
                        val assistParts = buildList {
                            currentTrain?.pusher?.let { add(formatAssistInfo("толкач", it)) }
                            currentTrain?.doubleTraction?.let {
                                add(
                                    formatAssistInfo(
                                        "двойная тяга",
                                        it
                                    )
                                )
                            }
                            currentTrain?.doubledTrain?.let {
                                add(
                                    formatAssistInfo(
                                        "сдвоенный",
                                        it
                                    )
                                )
                            }
                        }
                        if (assistParts.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clickable { showSettingsSheet = true }
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceDim,
                                        shape = Shapes.medium
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                assistParts.forEach { text ->
                                    Text(
                                        text = text,
                                        style = hintStyle,
                                        color = primaryColor.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.secondary_spacing))) }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isReversed = formUiState.isStationsReversed
                                val rotation by animateFloatAsState(
                                    targetValue = if (isReversed) 180f else 0f,
                                    animationSpec = tween(durationMillis = 300)
                                )

                                Icon(
                                    modifier = Modifier
                                        .graphicsLayer { rotationZ = rotation }
                                        .noRippleEffect(
                                            onClick = {
                                                viewModel.toggleStationsSortOrder()
                                            }
                                        ),
                                    painter = painterResource(com.z_company.route.R.drawable.sort_24px),
                                    contentDescription = null
                                )
                            }

                            // Правая часть: play/pause
                            val nextIsDeparture = viewModel.isNextDeparture()
                            OutlinedButton(
                                onClick = { viewModel.onGoClicked() },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (nextIsDeparture) MaterialTheme.colorScheme.surfaceContainerLow
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                AnimatedContent(targetState = nextIsDeparture) {
                                    val icon =
                                        if (it) com.z_company.route.R.drawable.play_arrow_24px
                                        else com.z_company.route.R.drawable.pause_24px
                                    Icon(
                                        painter = painterResource(icon),
                                        contentDescription = null,
                                        tint = if (it) MaterialTheme.colorScheme.surfaceContainerLow
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                }
                            }
                        }
                    }

                    stationListState?.let { stationList ->
                        item {
                            val displayList =
                                if (formUiState.isStationsReversed) stationList.reversed() else stationList
                            val timelineItems = displayList.toTimelineItems()

                            TrainStationTimeline(
                                stations = timelineItems,
                                modifier = Modifier.padding(top = 8.dp),
                                trainNumber = train.number,
                                reorderingStationId = formUiState.reorderingStationId,
                                onStationClick = { displayIndex ->
                                    if (formUiState.reorderingStationId != null) {
                                        viewModel.stopReorderStation()
                                    } else {
                                        val originalIndex =
                                            if (formUiState.isStationsReversed) stationList.size - 1 - displayIndex
                                            else displayIndex
                                        viewModel.startEditingStation(originalIndex)
                                    }
                                },
                                onStationLongPress = { displayIndex ->
                                    val item = displayList[displayIndex]
                                    if (formUiState.reorderingStationId == item.id) {
                                        viewModel.stopReorderStation()
                                    } else {
                                        viewModel.startReorderStation(item.id)
                                    }
                                },
                                onMoveUp = { displayIndex ->
                                    val originalIndex =
                                        if (formUiState.isStationsReversed) stationList.size - 1 - displayIndex
                                        else displayIndex
                                    viewModel.moveStation(originalIndex, originalIndex - 1)
                                },
                                onMoveDown = { displayIndex ->
                                    val originalIndex =
                                        if (formUiState.isStationsReversed) stationList.size - 1 - displayIndex
                                        else displayIndex
                                    viewModel.moveStation(originalIndex, originalIndex + 1)
                                },
                                onDismissReorder = { viewModel.stopReorderStation() },
                                onStationSwipeDelete = { displayIndex ->
                                    val originalIndex =
                                        if (formUiState.isStationsReversed) stationList.size - 1 - displayIndex
                                        else displayIndex
                                    viewModel.requestDeleteStation(originalIndex)
                                },
                                distance = train.distance?.toDoubleOrNull(),
                            )
                        }
                    }
                    item {
                        Button(
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .fillMaxWidth(),
                            shape = Shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            elevation = ButtonDefaults.elevatedButtonElevation(
                                defaultElevation = 3.dp,
                                pressedElevation = 0.dp
                            ),
                            onClick = {
                                viewModel.startAddingNewStation()
                            }
                        ) {
                            Text(
                                text = "Добавить станцию",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainAssistSection(
    title: String,
    assist: TrainAssist?,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onSeriesChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    onDriverNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    hintStyle: androidx.compose.ui.text.TextStyle,
    dataTextStyle: androidx.compose.ui.text.TextStyle,
    primaryColor: Color,
    noValueColor: Color,
    // Dropdown series
    seriesMenuList: List<String> = emptyList(),
    isSeriesMenuExpanded: Boolean = false,
    onSeriesMenuExpandedChange: (Boolean) -> Unit = {},
    onSeriesMenuContentChange: (String) -> Unit = {},
    onDeleteSeries: (String) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = hintStyle,
                color = primaryColor
            )
            if (assist == null) {
                TextButton(onClick = onAdd) {
                    Text(
                        text = "Добавить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = onRemove
                ) {
                    Icon(
                        painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                        contentDescription = "Удалить $title",
                        tint = noValueColor
                    )
                }
            }
        }

        AnimatedVisibility(visible = assist != null) {
            if (assist != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var seriesFieldValue by remember(assist.locomotiveSeries) {
                        mutableStateOf(
                            TextFieldValue(
                                text = assist.locomotiveSeries ?: "",
                                selection = TextRange(assist.locomotiveSeries?.length ?: 0)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            modifier = Modifier.weight(1f),
                            expanded = isSeriesMenuExpanded,
                            onExpandedChange = { onSeriesMenuExpandedChange(it) }
                        ) {
                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                                value = seriesFieldValue,
                                onValueChange = {
                                    seriesFieldValue = it
                                    onSeriesChange(it.text)
                                    onSeriesMenuContentChange(it.text)
                                },
                                placeholder = {
                                    Text(
                                        text = "Серия",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor
                                    )
                                },
                                textStyle = dataTextStyle,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                ),
                                colorBackgroundEmptyField = surfaceColor,
                                colorBackgroundNotEmptyField = surfaceColor
                            )

                            StationDropdownMenu(
                                expanded = isSeriesMenuExpanded,
                                stations = seriesMenuList,
                                onSelect = { selectionSeries ->
                                    onSeriesChange(selectionSeries)
                                    onSeriesMenuExpandedChange(false)
                                    seriesFieldValue = seriesFieldValue.copy(
                                        text = selectionSeries,
                                        selection = TextRange(selectionSeries.length)
                                    )
                                },
                                onDelete = onDeleteSeries,
                                onDismiss = { onSeriesMenuExpandedChange(false) },
                                textStyle = dataTextStyle
                            )
                        }

                        OutlinedTextFieldApp(
                            modifier = Modifier.weight(1f),
                            value = assist.locomotiveNumber ?: "",
                            onValueChange = onNumberChange,
                            placeholder = {
                                Text(
                                    text = "Номер",
                                    style = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = noValueColor
                                )
                            },
                            prefix = {
                                if (!assist.locomotiveNumber.isNullOrBlank()) {
                                    Text(
                                        text = "№ ",
                                        style = hintStyle,
                                        color = noValueColor
                                    )
                                }
                            },
                            textStyle = dataTextStyle,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            colorBackgroundEmptyField = surfaceColor,
                            colorBackgroundNotEmptyField = surfaceColor
                        )
                    }

                    OutlinedTextFieldApp(
                        modifier = Modifier.fillMaxWidth(),
                        value = assist.driverName ?: "",
                        onValueChange = onDriverNameChange,
                        placeholder = {
                            Text(
                                text = "Машинист",
                                style = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight.Light
                                ),
                                color = noValueColor
                            )
                        },
                        prefix = {
                            if (!assist.driverName.isNullOrBlank()) {
                                Text(
                                    text = "ТЧМ ",
                                    style = hintStyle,
                                    color = noValueColor
                                )
                            }
                        },
                        textStyle = dataTextStyle,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        colorBackgroundEmptyField = surfaceColor,
                        colorBackgroundNotEmptyField = surfaceColor
                    )
                    OutlinedTextFieldApp(
                        modifier = Modifier.fillMaxWidth(),
                        value = assist.notes ?: "",
                        onValueChange = onNotesChange,
                        placeholder = {
                            Text(
                                text = "Примечание",
                                style = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight.Light
                                ),
                                color = noValueColor
                            )
                        },
                        textStyle = dataTextStyle,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        colorBackgroundEmptyField = surfaceColor,
                        colorBackgroundNotEmptyField = surfaceColor
                    )
                }
            }
        }

        HorizontalDivider(
            color = primaryColor.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoubledTrainSection(
    assist: TrainAssist?,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onSeriesChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    onDriverNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onIsFirstChange: (Boolean) -> Unit,
    hintStyle: androidx.compose.ui.text.TextStyle,
    dataTextStyle: androidx.compose.ui.text.TextStyle,
    primaryColor: Color,
    noValueColor: Color,
    seriesMenuList: List<String> = emptyList(),
    isSeriesMenuExpanded: Boolean = false,
    onSeriesMenuExpandedChange: (Boolean) -> Unit = {},
    onSeriesMenuContentChange: (String) -> Unit = {},
    onDeleteSeries: (String) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val title = "Сдвоенный поезд"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = hintStyle,
                color = primaryColor
            )
            if (assist == null) {
                TextButton(onClick = onAdd) {
                    Text(
                        text = "Добавить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = onRemove
                ) {
                    Icon(
                        painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                        contentDescription = "Удалить $title",
                        tint = noValueColor
                    )
                }
            }
        }

        AnimatedVisibility(visible = assist != null) {
            if (assist != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Переключатель "Я первый" / "Я второй"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isFirst = assist.isFirst
                        if (isFirst == true) {
                            Button(
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Я первый")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onIsFirstChange(true) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Я первый")
                            }
                        }

                        if (isFirst == false) {
                            Button(
                                onClick = {},
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Я второй")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onIsFirstChange(false) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Я второй")
                            }
                        }
                    }

                    // Текст "Данные второго" / "Данные первого"
                    if (assist.isFirst != null) {
                        Text(
                            text = if (assist.isFirst == true) "Данные второго" else "Данные первого",
                            style = hintStyle,
                            color = noValueColor
                        )
                    }

                    // Поля ввода (серия + номер)
                    var seriesFieldValue2 by remember(assist.locomotiveSeries) {
                        mutableStateOf(
                            TextFieldValue(
                                text = assist.locomotiveSeries ?: "",
                                selection = TextRange(assist.locomotiveSeries?.length ?: 0)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            modifier = Modifier.weight(1f),
                            expanded = isSeriesMenuExpanded,
                            onExpandedChange = { onSeriesMenuExpandedChange(it) }
                        ) {
                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                                value = seriesFieldValue2,
                                onValueChange = {
                                    seriesFieldValue2 = it
                                    onSeriesChange(it.text)
                                    onSeriesMenuContentChange(it.text)
                                },
                                placeholder = {
                                    Text(
                                        text = "Серия",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor
                                    )
                                },
                                textStyle = dataTextStyle,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                ),
                                colorBackgroundEmptyField = surfaceColor,
                                colorBackgroundNotEmptyField = surfaceColor
                            )

                            StationDropdownMenu(
                                expanded = isSeriesMenuExpanded,
                                stations = seriesMenuList,
                                onSelect = { selectionSeries ->
                                    onSeriesChange(selectionSeries)
                                    onSeriesMenuExpandedChange(false)
                                    seriesFieldValue2 = seriesFieldValue2.copy(
                                        text = selectionSeries,
                                        selection = TextRange(selectionSeries.length)
                                    )
                                },
                                onDelete = onDeleteSeries,
                                onDismiss = { onSeriesMenuExpandedChange(false) },
                                textStyle = dataTextStyle
                            )
                        }

                        OutlinedTextFieldApp(
                            modifier = Modifier.weight(1f),
                            value = assist.locomotiveNumber ?: "",
                            onValueChange = onNumberChange,
                            placeholder = {
                                Text(
                                    text = "Номер",
                                    style = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = noValueColor
                                )
                            },
                            prefix = {
                                if (!assist.locomotiveNumber.isNullOrBlank()) {
                                    Text(
                                        text = "№ ",
                                        style = hintStyle,
                                        color = noValueColor
                                    )
                                }
                            },
                            textStyle = dataTextStyle,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            colorBackgroundEmptyField = surfaceColor,
                            colorBackgroundNotEmptyField = surfaceColor
                        )
                    }

                    OutlinedTextFieldApp(
                        modifier = Modifier.fillMaxWidth(),
                        value = assist.driverName ?: "",
                        onValueChange = onDriverNameChange,
                        placeholder = {
                            Text(
                                text = "Машинист",
                                style = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight.Light
                                ),
                                color = noValueColor
                            )
                        },
                        prefix = {
                            if (!assist.driverName.isNullOrBlank()) {
                                Text(
                                    text = "ТЧМ ",
                                    style = hintStyle,
                                    color = noValueColor
                                )
                            }
                        },
                        textStyle = dataTextStyle,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        colorBackgroundEmptyField = surfaceColor,
                        colorBackgroundNotEmptyField = surfaceColor
                    )
                    OutlinedTextFieldApp(
                        modifier = Modifier.fillMaxWidth(),
                        value = assist.notes ?: "",
                        onValueChange = onNotesChange,
                        placeholder = {
                            Text(
                                text = "Примечание",
                                style = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight.Light
                                ),
                                color = noValueColor
                            )
                        },
                        textStyle = dataTextStyle,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        colorBackgroundEmptyField = surfaceColor,
                        colorBackgroundNotEmptyField = surfaceColor
                    )
                }
            }
        }

        HorizontalDivider(
            color = primaryColor.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}

private fun formatAssistInfo(type: String, assist: TrainAssist): String {
    return buildString {
        append("$type: ")
        assist.locomotiveSeries?.let { append(it) }
        assist.locomotiveNumber?.let {
            if (isNotEmpty() && last() != ' ') append(" ")
            append("№$it")
        }
        assist.driverName?.let {
            if (isNotEmpty() && last() != ' ') append(" ")
            append("ТЧМ $it")
        }
        assist.notes?.let {
            if (isNotEmpty() && last() != ' ') append(" ")
            append("($it)")
        }
    }
}
