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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.z_company.core.R
import com.z_company.core.ResultState
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.trainCategory
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.StationEditBottomSheet
import com.z_company.route.component.TrainStationTimeline
import com.z_company.route.component.toTimelineItems
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.StationFormState
import com.z_company.route.viewmodel.TrainFormUiState
import com.z_company.route.viewmodel.TrainFormViewModel
import kotlinx.coroutines.launch
import kotlin.text.isNullOrBlank

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
                title = {},
                navigationIcon = {
                    TextButton(
                        onClick = viewModel::saveTrain,
                        enabled = formUiState.errorMessage == null,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = "Готово",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            painter = painterResource(com.z_company.route.R.drawable.settings_24px),
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        when (formUiState.saveTrainState) {
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
                onFilterMenu = { viewModel.onChangedDropDownContent(editingIndex.coerceAtLeast(0), it) },
                onDeleteStationName = { viewModel.removeStationName(it) },
                onSave = { name, arrival, departure ->
                    viewModel.saveStationFromSheet(editingIndex, name, arrival, departure)
                },
                onDelete = if (editingIndex >= 0) {
                    { viewModel.requestDeleteStation(editingIndex) }
                } else null,
                onDismiss = { viewModel.stopEditingStation() },
                dateAndTimeConverter = dateAndTimeConverter
            )
        }

        // Подтверждение удаления станции
        if (formUiState.confirmDeleteStationIndex != null) {
            val deleteIndex = formUiState.confirmDeleteStationIndex!!
            val stationName = stationListState
                ?.getOrNull(deleteIndex)
                ?.station?.data

            val title = if (!stationName.isNullOrBlank())
                "Удалить станцию $stationName?"
            else
                "Удалить станцию?"

            AppBottomSheet(
                onDismissRequest = { viewModel.cancelDeleteStation() },
                sheetState = sheetState,
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
            AppBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = sheetState,
                title = "Настройки поезда",
                actions = emptyList()
            )
        }

        if (formUiState.showCreateServicePhaseSheet) {
            var newPhaseDistance by remember {
                mutableStateOf(currentTrain?.distance?.takeIf { it != "0" } ?: "")
            }
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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
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
                                style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
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
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        onClick = { viewModel.createServicePhaseAndSave(newPhaseDistance) }
                    ) {
                        Text(text = "Добавить", style = MaterialTheme.typography.bodySmall)
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
                    Spacer(modifier = Modifier.height(40.dp))
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
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
                                                    .clickable { viewModel.removeAdditionalNumber(index) },
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
                                        .weight(1f),
                                    value = train.distance?.takeIf { it != "0" } ?: "",
                                    onValueChange = {
                                        onDistanceChange(it)
                                    },
                                    placeholder = {
                                        Text(
                                            text = "Плечо",
                                            style = LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            ),
                                            color = noValueColor
                                        )
                                    },
                                    suffix = {
                                        Text(
                                            text = "км",
                                            style = hintStyle,
                                            color = noValueColor
                                        )
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

                                OutlinedTextFieldApp(
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
                                value = train.conditionalLength?.takeIf { it != "0" } ?: "",
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
