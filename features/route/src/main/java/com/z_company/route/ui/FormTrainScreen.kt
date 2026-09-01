package com.z_company.route.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import androidx.compose.ui.unit.sp
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
import com.z_company.domain.entities.route.TrainDataVersion
import com.z_company.domain.entities.route.UtilsForEntities.trainCategory
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.ShoulderEditBottomSheet
import com.z_company.route.component.StationEditBottomSheet
import com.z_company.route.component.SegmentEditBottomSheet
import com.z_company.route.component.TrainStationTimeline
import com.z_company.route.component.SwipeToRevealDelete
import com.z_company.route.component.computeRouteSummary
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
    onAddServicePhase: (ServicePhase) -> Unit = {},
    onUpdateServicePhase: (ServicePhase) -> Unit = {},
    onDeleteServicePhase: (ServicePhase) -> Unit = {},
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hintStyle = MaterialTheme.typography.bodyMedium
    val dataTextStyle = MaterialTheme.typography.bodyLarge
    // Моноширинный вариант для числовых значений карточек — как в референсе.
    val monoDataTextStyle = dataTextStyle.copy(
        fontFamily = com.z_company.core.ui.theme.MonoFont,
        fontWeight = FontWeight.SemiBold
    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val noValueColor = primaryColor.copy(alpha = 0.5f)
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showTrainDataHistory by remember { mutableStateOf(false) }
    // Сигнал закрытия свайп-раскрытой строки станции (при отмене подтверждения)
    var closeSwipeSignal by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier
            .fillMaxWidth(),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
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
                    TextButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onTrainSaved()
                        },
                    ) {
                        Text(
                            text = "Готово",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
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
        // Плечо, выделенное в шторке «Плечи» (ещё не применённое). Сбрасывается на текущее
        // выбранное при каждом открытии шторки.
        var selectedInSheet by remember(showSelectServicePhase) {
            mutableStateOf(selectedServicePhase)
        }
        // Вторая шторка — создание/редактирование плеча.
        var showEditPhaseSheet by remember { mutableStateOf(false) }
        var editingPhase by remember { mutableStateOf<ServicePhase?>(null) }

        // BottomSheet для редактирования/добавления станции
        val editingIndex = formUiState.editingStationIndex
        if (editingIndex != null) {
            val editingStation = if (editingIndex >= 0 && stationListState != null
                && editingIndex in stationListState.indices
            ) {
                stationListState[editingIndex]
            } else null
            val editingStationId = editingStation?.id ?: remember(editingIndex) { com.z_company.domain.util.generateId() }

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
                trainWeight = currentTrain?.dataVersions
                    ?.firstOrNull { it.stationId == editingStation?.id }?.weight
                    ?: currentTrain?.weight,
                trainAxle = currentTrain?.dataVersions
                    ?.firstOrNull { it.stationId == editingStation?.id }?.axle
                    ?: currentTrain?.axle,
                trainConditionalLength = currentTrain?.dataVersions
                    ?.firstOrNull { it.stationId == editingStation?.id }?.conditionalLength
                    ?: currentTrain?.conditionalLength,
                onTrainDataChange = if (
                    editingIndex > 0 || (editingIndex == -1 && !stationListState.isNullOrEmpty())
                ) {
                    { stationName, weight, axle, conditionalLength ->
                        viewModel.addTrainDataVersion(
                            stationId = editingStationId,
                            stationName = stationName,
                            weight = weight,
                            axle = axle,
                            conditionalLength = conditionalLength
                        )
                    }
                } else null,
                onSave = { name, arrival, departure, trackNumber, isFinalStation, isPassingStation ->
                    viewModel.saveStationFromSheet(
                        editingIndex, name, arrival, departure, trackNumber,
                        isFinalStation, isPassingStation, editingStationId
                    )
                },
                onDelete = if (editingIndex >= 0) {
                    { viewModel.requestDeleteStation(editingIndex) }
                } else null,
                onDismiss = { viewModel.stopEditingStation() },
                dateAndTimeConverter = dateAndTimeConverter
            )
        }

        // BottomSheet для редактирования перегона (клик по «времени в пути» между станциями)
        val editingSegmentAfterIndex = formUiState.editingSegmentAfterIndex
        if (editingSegmentAfterIndex != null && stationListState != null) {
            val fromState = stationListState.getOrNull(editingSegmentAfterIndex)
            val toState = stationListState.getOrNull(editingSegmentAfterIndex + 1)
            SegmentEditBottomSheet(
                fromStationName = fromState?.station?.data,
                toStationName = toState?.station?.data,
                trackNumber = toState?.segmentTrackNumber,
                notes = toState?.segmentNotes,
                onSave = { fromName, toName, trackNumber, notes ->
                    viewModel.saveSegmentFromSheet(
                        editingSegmentAfterIndex, fromName, toName, trackNumber, notes
                    )
                },
                onDismiss = { viewModel.stopEditingSegment() },
            )
        }

        if (showTrainDataHistory) {
            ModalBottomSheet(
                onDismissRequest = { showTrainDataHistory = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "История данных поезда",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    currentTrain?.dataVersions.orEmpty().forEachIndexed { index, version ->
                        TrainDataVersionEditor(
                            index = index,
                            version = version,
                            onSave = { weight, axle, conditionalLength ->
                                viewModel.updateTrainDataVersion(
                                    index = index,
                                    weight = weight,
                                    axle = axle,
                                    conditionalLength = conditionalLength
                                )
                            },
                            onDelete = { viewModel.deleteTrainDataVersion(index) }
                        )
                    }
                }
            }
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
                onDismissRequest = {
                    viewModel.cancelDeleteStation()
                    closeSwipeSignal++
                },
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
                containerColor = MaterialTheme.colorScheme.background,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Настройки поезда",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        TextButton(
                            onClick = {
                                showSettingsSheet = false
                                viewModel.saveAssistSeries()
                            }
                        ) {
                            Text(
                                text = "Готово",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                        onDeleteSeries = viewModel::removeSeries,
                        showPositionToggle = true,
                        firstLabel = "Я толкаю",
                        secondLabel = "Меня толкают",
                        onIsFirstChange = viewModel::setPusherIsFirst
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

                    CarInspectorSection(
                        carInspector = currentTrain?.carInspector,
                        onAdd = viewModel::addCarInspector,
                        onRemove = viewModel::removeCarInspector,
                        onFullNameChange = viewModel::setCarInspectorFullName,
                        onTabNumberChange = viewModel::setCarInspectorTabNumber,
                        onCouplingTimeChange = viewModel::setCarInspectorCouplingTime,
                        hintStyle = hintStyle,
                        dataTextStyle = dataTextStyle,
                        primaryColor = primaryColor,
                        noValueColor = noValueColor,
                        dateAndTimeConverter = dateAndTimeConverter,
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                ) {
                    // ── Заголовок + карандаш (при выделении) + закрытие ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Плечи",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceBright)
                                .clickable { showSelectServicePhase = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // ── Список плеч ──
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = servicePhaseList,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            val isSelected = selectedInSheet?.id == item.id
                            val rowBg by animateColorAsState(
                                targetValue = if (isSelected)
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                else Color.Transparent,
                                label = "phaseRowBg"
                            )
                            Column {
                                if (index != 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .height(0.5.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .clickable {
                                            // Первый тап — выделить; повторный по выделенному — применить.
                                            if (selectedInSheet?.id == item.id) {
                                                onSelectServicePhase(item)
                                                showSelectServicePhase = false
                                            } else {
                                                selectedInSheet = item
                                            }
                                        }
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = "${item.departureStation} — ${item.arrivalStation}",
                                        style = dataTextStyle,
                                        color = primaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected) {
                                        Icon(
                                            modifier = Modifier.size(18.dp),
                                            painter = painterResource(com.z_company.route.R.drawable.check_circle_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Text(
                                        text = "${item.distance} км",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = com.z_company.core.ui.theme.MonoFont
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        item {
                            if (servicePhaseList.isEmpty()) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                    text = "Список пуст",
                                    style = dataTextStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ── Нижние кнопки: «Выбрать» (при выделении) + «Новое плечо» ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedVisibility(
                            visible = selectedInSheet != null,
                            enter = fadeIn(animationSpec = tween(140)) +
                                    expandVertically(animationSpec = tween(140)),
                            exit = fadeOut(animationSpec = tween(110)) +
                                    shrinkVertically(animationSpec = tween(110))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = Shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    onClick = {
                                        selectedInSheet?.let { onSelectServicePhase(it) }
                                        showSelectServicePhase = false
                                    }
                                ) {
                                    Text(
                                        text = "Выбрать",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = Shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                                        contentColor = MaterialTheme.colorScheme.tertiary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 0.dp
                                    ),
                                    onClick = {
                                        editingPhase = selectedInSheet
                                        showEditPhaseSheet = true
                                    }
                                ) {
                                    Icon(
                                        modifier = Modifier.size(18.dp),
                                        painter = painterResource(R.drawable.ic_edit),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Редактировать",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = Shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.tertiary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            onClick = {
                                editingPhase = null
                                showEditPhaseSheet = true
                            }
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(com.z_company.route.R.drawable.add_circle_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Новое плечо",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ── Вторая шторка: создание/редактирование плеча (поверх «Плечи») ──
        if (showEditPhaseSheet) {
            ShoulderEditBottomSheet(
                phase = editingPhase,
                stationList = menuList,
                onSave = { phase, reverse ->
                    if (editingPhase == null) {
                        onAddServicePhase(phase)
                        if (reverse) {
                            onAddServicePhase(
                                ServicePhase(
                                    departureStation = phase.arrivalStation,
                                    arrivalStation = phase.departureStation,
                                    distance = phase.distance,
                                    linearMileageRate = phase.linearMileageRate,
                                )
                            )
                        }
                    } else {
                        onUpdateServicePhase(phase)
                        if (selectedInSheet?.id == phase.id) selectedInSheet = phase
                    }
                },
                onDelete = if (editingPhase != null) {
                    {
                        editingPhase?.let { p ->
                            onDeleteServicePhase(p)
                            if (selectedInSheet?.id == p.id) selectedInSheet = null
                        }
                    }
                } else null,
                onDeleteStationName = { viewModel.removeStationName(it) },
                onDismiss = { showEditPhaseSheet = false }
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

        Box(modifier = Modifier.padding(paddingValues)) {
            currentTrain?.let { train ->
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.testTag("form_train_lazy_column"),
                    horizontalAlignment = Alignment.End,
                    contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp)
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
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = com.z_company.core.ui.theme.MonoFont
                                                ),
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

                            val cellLabelStyle =
                                MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                            val cardShape = Shapes.medium
                            val cardColor = MaterialTheme.colorScheme.secondary
                            val dividerColor = MaterialTheme.colorScheme.outlineVariant

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

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Карточка: Плечо | Номер
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .shadow(elevation = 2.dp, shape = cardShape)
                                        .clip(cardShape)
                                        .background(color = cardColor, shape = cardShape),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ячейка «Плечо»
                                    Column(
                                        modifier = Modifier
                                            .weight(0.85f)
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Расстояние",
                                            style = cellLabelStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                        OutlinedTextFieldApp(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = train.distance?.takeIf { it != "0" } ?: "",
                                            onValueChange = { onDistanceChange(it) },
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
                                            textStyle = monoDataTextStyle,
                                            singleLine = true,
                                            fieldElevation = 0.dp,
                                            colorBackgroundEmptyField = Color.Transparent,
                                            colorBackgroundNotEmptyField = Color.Transparent,
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(40.dp)
                                            .background(dividerColor)
                                    )

                                    // Ячейка «Номер»
                                    Column(
                                        modifier = Modifier
                                            .weight(1.15f)
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Номер",
                                            style = cellLabelStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                        OutlinedTextFieldApp(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { focusState ->
                                                    numberFieldValue = if (focusState.isFocused) {
                                                        numberFieldValue.copy(
                                                            selection = TextRange(numberFieldValue.text.length)
                                                        )
                                                    } else {
                                                        numberFieldValue.copy(
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
                                            textStyle = monoDataTextStyle,
                                            singleLine = true,
                                            fieldElevation = 0.dp,
                                            colorBackgroundEmptyField = Color.Transparent,
                                            colorBackgroundNotEmptyField = Color.Transparent,
                                        )
                                    }
                                }

                                // Кнопка «+» — добавить дополнительный номер поезда.
                                // Фон непрозрачный (compositeOver), иначе полупрозрачная
                                // заливка поверх тени даёт «двойную рамку».
                                val addEnabled = !train.number.isNullOrBlank()
                                val addButtonColor = if (addEnabled)
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                        .compositeOver(cardColor)
                                else cardColor
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(56.dp)
                                        .shadow(elevation = 2.dp, shape = cardShape)
                                        .clip(cardShape)
                                        .background(color = addButtonColor, shape = cardShape)
                                        .clickable(enabled = addEnabled) {
                                            val num = train.number?.trim()
                                            if (!num.isNullOrBlank()) {
                                                viewModel.addAdditionalNumber(num)
                                                onNumberChanged("")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(com.z_company.route.R.drawable.add_circle_24px),
                                        contentDescription = "Добавить номер",
                                        tint = if (addEnabled)
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
                                        .clip(Shapes.medium)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceDim,
                                            shape = Shapes.medium
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
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
                        val cellLabelStyle =
                            MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        val cardShape = Shapes.medium
                        val dividerColor = MaterialTheme.colorScheme.outlineVariant

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .shadow(elevation = 2.dp, shape = cardShape)
                                .clip(cardShape)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = cardShape
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Ячейка «Вес»
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Вес, т",
                                    style = cellLabelStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                                OutlinedTextFieldApp(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = train.weight?.takeIf { it != "0" } ?: "",
                                    onValueChange = { onWeightChanged(it) },
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
                                    textStyle = monoDataTextStyle,
                                    singleLine = true,
                                    fieldElevation = 0.dp,
                                    colorBackgroundEmptyField = Color.Transparent,
                                    colorBackgroundNotEmptyField = Color.Transparent,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(dividerColor)
                            )

                            // Ячейка «Оси»
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Оси",
                                    style = cellLabelStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                                OutlinedTextFieldApp(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = train.axle?.takeIf { it != "0" } ?: "",
                                    onValueChange = { onAxleChanged(it) },
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
                                    textStyle = monoDataTextStyle,
                                    singleLine = true,
                                    fieldElevation = 0.dp,
                                    colorBackgroundEmptyField = Color.Transparent,
                                    colorBackgroundNotEmptyField = Color.Transparent,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(dividerColor)
                            )

                            // Ячейка «Удельное»
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                            ) {
                                // Длина в метрах = условная длина × 14 (подсказка рядом с «У.Д.»).
                                val lengthMeters = train.conditionalLength
                                    ?.takeIf { it != "0" }
                                    ?.toDoubleOrNull()
                                    ?.let { (it * 14).toInt() }
                                // При крупном шрифте полная подсказка «· 868 м»
                                // не помещается — тогда убираем точку-разделитель и
                                // единицу «м», оставляя только число.
                                var meterHintCompact by remember(lengthMeters) {
                                    mutableStateOf(false)
                                }
                                Row(
                                    modifier = Modifier.padding(start = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "У.Д.",
                                        style = cellLabelStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (lengthMeters != null) {
                                        Text(
                                            modifier = Modifier.weight(1f, fill = false),
                                            text = if (meterHintCompact) {
                                                "$lengthMeters"
                                            } else {
                                                "· $lengthMeters м"
                                            },
                                            style = cellLabelStyle.copy(
                                                fontFamily = com.z_company.core.ui.theme.MonoFont
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            softWrap = false,
                                            maxLines = 1,
                                            overflow = TextOverflow.Clip,
                                            onTextLayout = { result ->
                                                if (!meterHintCompact && result.hasVisualOverflow) {
                                                    meterHintCompact = true
                                                }
                                            }
                                        )
                                    }
                                }
                                OutlinedTextFieldApp(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = train.conditionalLength?.takeIf { it != "0" }
                                        ?.let { s ->
                                            val d = s.toDoubleOrNull()
                                            if (d != null && d == kotlin.math.floor(d)) d.toLong().toString() else s
                                        } ?: "",
                                    onValueChange = { onLengthChanged(it) },
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
                                    textStyle = monoDataTextStyle,
                                    singleLine = true,
                                    fieldElevation = 0.dp,
                                    colorBackgroundEmptyField = Color.Transparent,
                                    colorBackgroundNotEmptyField = Color.Transparent,
                                )
                            }
                        }
                        if (train.dataVersions.size > 1) {
                            TextButton(
                                modifier = Modifier.padding(top = 4.dp),
                                onClick = { showTrainDataHistory = true }
                            ) {
                                Text(
                                    "Данные менялись · ${train.dataVersions.size} ${versionWord(train.dataVersions.size)}",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    item {
                        val phase = selectedServicePhase
                        val cellLabelStyle =
                            MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        val animatedBackgroundColors by animateColorAsState(
                            targetValue = if (phase == null) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.secondary,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )
                        val accentSoft = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                            .compositeOver(MaterialTheme.colorScheme.secondary)

                        Row(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .shadow(elevation = 1.dp, shape = Shapes.medium)
                                .fillMaxWidth()
                                .clip(Shapes.medium)
                                .clickable {
                                    showSelectServicePhase = true
                                }
                                .background(
                                    color = animatedBackgroundColors,
                                    shape = Shapes.medium
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Иконка-чип
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(accentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.size(18.dp),
                                    painter = painterResource(com.z_company.route.R.drawable.ic_card_train_ref),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            // «Плечо» + «отправление — прибытие», либо плейсхолдер
                            Column(modifier = Modifier.weight(1f)) {
                                if (phase != null) {
                                    Text(
                                        text = "Плечо",
                                        style = cellLabelStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${phase.departureStation} — ${phase.arrivalStation}",
                                        style = dataTextStyle,
                                        color = primaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "Выбрать плечо",
                                        style = dataTextStyle,
                                        color = noValueColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (phase != null) {
                                Text(
                                    text = "${phase.distance} км",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = com.z_company.core.ui.theme.MonoFont
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceDim)
                                        .clickable { onSelectServicePhase(null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        modifier = Modifier.size(14.dp),
                                        painter = painterResource(R.drawable.ic_clear),
                                        contentDescription = "Очистить",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    item {
                        // Каждый заполненный раздел настроек поезда + направление из переключателя.
                        val assists = buildList {
                            currentTrain?.pusher?.let {
                                add(
                                    Triple(
                                        "Толкач", it,
                                        it.isFirst?.let { f -> if (f) "Я толкаю" else "Меня толкают" })
                                )
                            }
                            currentTrain?.doubleTraction?.let {
                                add(Triple("Двойная тяга", it, null))
                            }
                            currentTrain?.doubledTrain?.let {
                                add(
                                    Triple(
                                        "Сдвоенный поезд", it,
                                        it.isFirst?.let { f -> if (f) "Я первый" else "Я второй" })
                                )
                            }
                        }
                        if (assists.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(Shapes.medium)
                                    .clickable { showSettingsSheet = true }
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceDim,
                                        shape = Shapes.medium
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                assists.forEach { (label, assist, direction) ->
                                    AssistInfoRow(
                                        label = label,
                                        assist = assist,
                                        direction = direction
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.secondary_spacing))) }
                    item {
                        val stopwatch = rememberStopwatchState(
                            stations = stationListState?.map {
                                Triple(it.station.data, it.arrival.data, it.departure.data)
                            } ?: emptyList(),
                            legArrivalStation = selectedServicePhase?.arrivalStation,
                        )
                        val nextIsDeparture = viewModel.isNextDeparture()
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Слева: сортировка
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val isReversed = formUiState.isStationsReversed
                                    val rotation by animateFloatAsState(
                                        targetValue = if (isReversed) 180f else 0f,
                                        animationSpec = tween(durationMillis = 300)
                                    )
                                    Icon(
                                        modifier = Modifier
                                            .graphicsLayer { rotationZ = rotation }
                                            .noRippleEffect(
                                                onClick = { viewModel.toggleStationsSortOrder() }
                                            ),
                                        painter = painterResource(com.z_company.route.R.drawable.sort_24px),
                                        contentDescription = null
                                    )
                                }

                                // Центр: чип секундомера — по вертикали в одну линию с кнопками
                                if (stopwatch != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(stopwatch.bg)
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stopwatch.timeText,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = com.z_company.core.ui.theme.MonoFont,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = stopwatch.fg
                                        )
                                    }
                                }

                                // Справа: play/pause
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
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

                            // Подпись под чипом (что показывает секундомер)
                            if (stopwatch != null) {
                                Text(
                                    text = stopwatch.label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 3.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    stationListState?.let { stationList ->
                        item {
                            Text(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                                text = "МАРШРУТ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    letterSpacing = androidx.compose.ui.unit.TextUnit(
                                        1.4f,
                                        androidx.compose.ui.unit.TextUnitType.Sp
                                    )
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        item {
                            val displayList =
                                if (formUiState.isStationsReversed) stationList.reversed() else stationList
                            val timelineItems = displayList.toTimelineItems()

                          Column {
                            TrainStationTimeline(
                                stations = timelineItems,
                                showSummary = false,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth()
                                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                                    .clip(Shapes.medium)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    )
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                rowBackgroundColor = MaterialTheme.colorScheme.secondary,
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
                                onSegmentClick = { displayIndex ->
                                    // index здесь — станция ПЕРЕД перегоном в displayList;
                                    // при развороте списка меняем местами «раньше»/«позже».
                                    val originalAfterIndex =
                                        if (formUiState.isStationsReversed) {
                                            stationList.size - 2 - displayIndex
                                        } else {
                                            displayIndex
                                        }
                                    viewModel.startEditingSegment(originalAfterIndex)
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
                                closeSwipeSignal = closeSwipeSignal,
                            )

                            // Сводка маршрута — отдельными пилюлями под карточкой (как в референсе).
                            // Считаем всегда по исходному (не перевёрнутому) порядку станций,
                            // иначе при развороте списка время в пути отрицательное и сводка пропадает.
                            val summary = computeRouteSummary(
                                stationList.toTimelineItems(),
                                train.distance?.toDoubleOrNull()
                            )
                            if (summary != null) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetaPill(label = "Время в пути:", value = summary.totalTimeText)
                                    summary.sectionSpeed?.let {
                                        MetaPill(
                                            label = "Уч",
                                            value = "%.1f км/ч".format(it),
                                            mono = true
                                        )
                                    }
                                    summary.technicalSpeed?.let {
                                        MetaPill(
                                            label = "Техн",
                                            value = "%.1f км/ч".format(it),
                                            mono = true
                                        )
                                    }
                                }
                            }
                          }
                        }
                    }
                    item {
                        Button(
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = Shapes.medium,
                                    clip = false
                                ),
                            shape = Shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            onClick = {
                                viewModel.startAddingNewStation()
                            }
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(com.z_company.route.R.drawable.add_circle_24px),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Добавить станцию",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                ),
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TrainDataVersionEditor(
    index: Int,
    version: TrainDataVersion,
    onSave: (weight: String, axle: String, conditionalLength: String) -> Unit,
    onDelete: () -> Unit
) {
    var weight by remember(version.weight) { mutableStateOf(version.weight ?: "") }
    var axle by remember(version.axle) { mutableStateOf(version.axle ?: "") }
    var conditionalLength by remember(version.conditionalLength) {
        mutableStateOf(version.conditionalLength.orEmpty().withoutZeroFraction())
    }

    SwipeToRevealDelete(
        modifier = Modifier.fillMaxWidth(),
        onDeleteClick = onDelete,
        compact = true,
        backgroundVerticalPadding = 0.dp,
        itemKey = "${version.stationId}-${version.changedAt}-$index"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (index == 0 && version.stationId == null) "Исходные данные"
                else version.stationName?.let { "Станция $it" } ?: "Станция не указана",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VersionDataField("Вес, т", weight, Modifier.weight(1f)) { weight = it }
            VersionDataField("Оси", axle, Modifier.weight(1f)) { axle = it }
            VersionDataField("У.Д.", conditionalLength, Modifier.weight(1f)) {
                conditionalLength = it
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = Shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                contentColor = MaterialTheme.colorScheme.tertiary
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
            onClick = { onSave(weight, axle, conditionalLength) }
        ) {
            Text("Сохранить", fontWeight = FontWeight.SemiBold)
        }
        }
    }
}

private fun String.withoutZeroFraction(): String {
    val separatorIndex = indexOfLast { it == '.' || it == ',' }
    if (separatorIndex < 0) return this
    return if (substring(separatorIndex + 1).isNotEmpty() &&
        substring(separatorIndex + 1).all { it == '0' }
    ) substring(0, separatorIndex) else this
}

@Composable
private fun VersionDataField(
    label: String,
    value: String,
    modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        OutlinedTextFieldApp(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() || it == '.' || it == ',' }) onValueChange(newValue)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont,
                fontWeight = FontWeight.SemiBold
            ),
            singleLine = true,
            fieldElevation = 0.dp,
            colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
            colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
        )
    }
}

private fun versionWord(count: Int): String = when {
    count % 100 in 11..14 -> "версий"
    count % 10 == 1 -> "версия"
    count % 10 in 2..4 -> "версии"
    else -> "версий"
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
    onDeleteSeries: (String) -> Unit = {},
    // Опциональный сегментированный переключатель направления (для «Толкач»).
    showPositionToggle: Boolean = false,
    firstLabel: String = "",
    secondLabel: String = "",
    onIsFirstChange: (Boolean) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .clip(Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    if (showPositionToggle) {
                        SegmentedToggle(
                            options = listOf(firstLabel, secondLabel),
                            activeIndex = when (assist.isFirst) {
                                true -> 0
                                false -> 1
                                else -> -1
                            },
                            onSelect = { onIsFirstChange(it == 0) }
                        )
                    }
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
                                textStyle = dataTextStyle.copy(fontFamily = com.z_company.core.ui.theme.MonoFont),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                ),
                        fieldElevation = 0.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        colorBackgroundEmptyField = MaterialTheme.colorScheme.secondary,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.secondary
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
                            textStyle = dataTextStyle.copy(fontFamily = com.z_company.core.ui.theme.MonoFont),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                        fieldElevation = 0.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        colorBackgroundEmptyField = MaterialTheme.colorScheme.secondary,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.secondary
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
                        fieldElevation = 0.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        colorBackgroundEmptyField = MaterialTheme.colorScheme.secondary,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.secondary
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
                        fieldElevation = 0.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        colorBackgroundEmptyField = MaterialTheme.colorScheme.secondary,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

    }
}

// ─── Вагонник (осмотр/закрепление состава перед прицепкой) ───────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarInspectorSection(
    carInspector: com.z_company.domain.entities.route.CarInspector?,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onTabNumberChange: (String) -> Unit,
    onCouplingTimeChange: (Long?) -> Unit,
    hintStyle: androidx.compose.ui.text.TextStyle,
    dataTextStyle: androidx.compose.ui.text.TextStyle,
    primaryColor: Color,
    noValueColor: Color,
    dateAndTimeConverter: com.z_company.core.util.DateAndTimeConverter?,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .clip(Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Вагонник",
                style = hintStyle,
                color = primaryColor
            )
            if (carInspector == null) {
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
                        contentDescription = "Удалить вагонника",
                        tint = noValueColor
                    )
                }
            }
        }

        AnimatedVisibility(visible = carInspector != null) {
            if (carInspector != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextFieldApp(
                        modifier = Modifier.fillMaxWidth(),
                        value = carInspector.fullName ?: "",
                        onValueChange = onFullNameChange,
                        placeholder = {
                            Text(
                                text = "ФИО",
                                style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                color = noValueColor
                            )
                        },
                        textStyle = dataTextStyle,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        fieldElevation = 0.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        colorBackgroundEmptyField = MaterialTheme.colorScheme.secondary,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedTextFieldApp(
                        modifier = Modifier.fillMaxWidth(),
                        value = carInspector.tabNumber ?: "",
                        onValueChange = onTabNumberChange,
                        placeholder = {
                            Text(
                                text = "Табельный номер",
                                style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                color = noValueColor
                            )
                        },
                        textStyle = dataTextStyle.copy(fontFamily = com.z_company.core.ui.theme.MonoFont),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        fieldElevation = 0.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        colorBackgroundEmptyField = MaterialTheme.colorScheme.secondary,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.secondary
                    )

                    // ── Время прицепки к составу ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Время прицепки",
                            style = dataTextStyle,
                            color = if (carInspector.couplingTime != null) primaryColor else noValueColor
                        )
                        val timeText = carInspector.couplingTime?.let {
                            dateAndTimeConverter?.getTimeFromDateLong(it)
                        } ?: "—"
                        Text(
                            text = timeText,
                            style = dataTextStyle.copy(fontFamily = com.z_company.core.ui.theme.MonoFont),
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        com.z_company.route.component.AppDateTimePicker(
            title = "Время прицепки",
            onDateTimeSelected = { time ->
                onCouplingTimeChange(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
            startDateTime = carInspector?.couplingTime ?: com.z_company.core.util.TimeManager().now(),
            timeZoneStr = dateAndTimeConverter?.timeZoneText ?: "GMT+3"
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
    val title = "Сдвоенный поезд"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .clip(Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    SegmentedToggle(
                        options = listOf("Я первый", "Я второй"),
                        activeIndex = when (assist.isFirst) {
                            true -> 0
                            false -> 1
                            else -> -1
                        },
                        onSelect = { onIsFirstChange(it == 0) }
                    )

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
                                textStyle = dataTextStyle.copy(fontFamily = com.z_company.core.ui.theme.MonoFont),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                ),
                        fieldElevation = 0.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        colorBackgroundEmptyField = MaterialTheme.colorScheme.secondary,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.secondary
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
                            textStyle = dataTextStyle.copy(fontFamily = com.z_company.core.ui.theme.MonoFont),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                        fieldElevation = 0.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        colorBackgroundEmptyField = MaterialTheme.colorScheme.secondary,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.secondary
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
                        fieldElevation = 0.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        colorBackgroundEmptyField = MaterialTheme.colorScheme.secondary,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.secondary
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
                        fieldElevation = 0.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        colorBackgroundEmptyField = MaterialTheme.colorScheme.secondary,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

    }
}

/**
 * Секундомер маршрута: показывает время с последнего события ≤ текущего момента.
 * Если последнее событие — прибытие (поезд стоит) → «стоянка», если отправление → «в пути».
 * Формат: до 5 мин — секунды (M:SS), до часа — «Nмин», есть часы — «Yч Zмин», есть дни —
 * «Xд Yч Zмин»; фон жёлтый после 5 мин, красный после 30 (как чипы стоянки). Под чипом —
 * подпись, что именно показано.
 * Прибыв на конечную станцию выбранного плеча, стоянку не показываем (возвращаем null).
 */
private data class StopwatchData(
    val timeText: String,
    val bg: Color,
    val fg: Color,
    val label: String,
)

@Composable
private fun rememberStopwatchState(
    // (имя станции, прибытие, отправление)
    stations: List<Triple<String?, Long?, Long?>>,
    // Конечная станция выбранного плеча — прибыв на неё, время стоянки не показываем.
    legArrivalStation: String? = null,
): StopwatchData? {
    // Тикаем раз в секунду; берём АБСОЛЮТНОЕ системное время (не накапливаем),
    // поэтому после сна/дозы первое же обновление показывает корректное время.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    // Дополнительно мгновенно обновляем время при возврате на экран / разблокировке.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                now = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // События: (время, isDeparture, имя станции). При равном времени приоритет у отправления
    // (compareBy: false < true) → берём его, поэтому это «в пути», а не «стоянка».
    val events = buildList {
        stations.forEach { (name, arrival, departure) ->
            arrival?.let { add(Triple(it, false, name)) }
            departure?.let { add(Triple(it, true, name)) }
        }
    }
    val last = events
        .filter { it.first <= now }
        .maxWithOrNull(compareBy({ it.first }, { it.second })) ?: return null

    val isMoving = last.second

    // Прибыли на конечную станцию плеча (последнее событие — прибытие на станцию,
    // совпадающую с конечной станцией выбранного плеча) → стоянку не показываем.
    if (!isMoving && !legArrivalStation.isNullOrBlank() &&
        last.third?.trim().equals(legArrivalStation.trim(), ignoreCase = true)
    ) {
        return null
    }

    val totalSec = (now - last.first).coerceAtLeast(0L) / 1000
    val totalMin = totalSec / 60
    val totalHours = totalMin / 60
    val days = totalHours / 24

    // Цветом подсвечиваем только стоянку (как чипы стоянки). В пути — всегда нейтральный фон.
    val neutral = MaterialTheme.colorScheme.surfaceDim to MaterialTheme.colorScheme.primary
    val (bg, fg) = when {
        isMoving -> neutral
        totalMin >= 30 -> Color(0xFFEF5350) to Color.White
        totalMin >= 5 -> Color(0xFFFFC107) to Color(0xFF3E2723)
        else -> neutral
    }
    // Формат: есть дни → «Xд Yч Zмин», есть часы → «Yч Zмин»,
    // до 5 минут → секунды «M:SS», иначе — «Nмин».
    val timeText = when {
        days > 0 -> "%dд %dч %dмин".format(days, totalHours % 24, totalMin % 60)
        totalHours > 0 -> "%dч %dмин".format(totalHours, totalMin % 60)
        totalMin >= 5 -> "${totalMin}мин"
        else -> "%d:%02d".format(totalSec / 60, totalSec % 60)
    }
    return StopwatchData(
        timeText = timeText,
        bg = bg,
        fg = fg,
        label = if (isMoving) "в пути" else "стоянка"
    )
}

/** Строка инфо-блока для одного раздела настроек поезда: тип + направление (из
 *  переключателя) сверху, детали (серия/номер/примечание) снизу — без жирного. */
@Composable
private fun AssistInfoRow(label: String, assist: TrainAssist, direction: String?) {
    val details = buildString {
        assist.locomotiveSeries?.let { append(it) }
        assist.locomotiveNumber?.let {
            if (isNotEmpty()) append(" ")
            append("№$it")
        }
        assist.driverName?.let {
            if (isNotEmpty()) append(" · ")
            append("ТЧМ $it")
        }
        assist.notes?.let {
            if (isNotEmpty()) append(" · ")
            append(it)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (direction != null) {
                Text(
                    text = "· $direction",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        if (details.isNotBlank()) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Пилюля сводки маршрута: «Метка значение» (значение моноширинно при mono=true). */
@Composable
private fun MetaPill(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontFamily = if (mono) com.z_company.core.ui.theme.MonoFont else null
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Сегментированный переключатель (как SegmentedToggle в референсе): дорожка
 *  [surfaceBright] + белый «ползунок» на активном сегменте. activeIndex = -1 — ничего не выбрано. */
@Composable
private fun SegmentedToggle(
    options: List<String>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val active = i == activeIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (active) Modifier.shadow(1.dp, RoundedCornerShape(8.dp)) else Modifier
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.secondary else Color.Transparent
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
