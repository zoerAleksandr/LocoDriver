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
import androidx.compose.runtime.mutableIntStateOf
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
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.trainCategory
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.StationFormState
import com.z_company.route.viewmodel.TrainFormUiState
import com.z_company.route.viewmodel.TrainFormViewModel
import com.z_company.route.component.TrainRouteSection
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
    onAddingStation: () -> Unit,
    onDeleteStation: (StationFormState) -> Unit,
    onStationNameChanged: (index: Int, s: String) -> Unit,
    onDepartureTimeChanged: (index: Int, time: Long?) -> Unit,
    onArrivalTimeChanged: (index: Int, time: Long?) -> Unit,
    stationListState: SnapshotStateList<StationFormState>?,
    menuList: List<String>,
    isExpandedMenu: Pair<Int, Boolean>?,
    onExpandedMenuChange: (Int, Boolean) -> Unit,
    onChangedContentMenu: (Int, String) -> Unit,
    onDeleteStationName: (String) -> Unit,
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
                actions = {},
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

        var selectSectionIndexState = remember { mutableIntStateOf(0) }

        var isTrainInfoVisible by remember {
            mutableStateOf(false)
        }

        var showSelectServicePhase by remember { mutableStateOf(false) }

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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        )
                        {
                            // Левая часть: sort + swap
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
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    modifier = Modifier
                                        .noRippleEffect(
                                            onClick = {
                                                viewModel.toggleReorderMode()
                                            }
                                        ),
                                    painter = painterResource(com.z_company.route.R.drawable.swap_vert_24px),
                                    contentDescription = null,
                                    tint = if (formUiState.isReorderMode) MaterialTheme.colorScheme.tertiary else primaryColor
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    modifier = Modifier
                                        .noRippleEffect(
                                            onClick = {
                                                viewModel.toggleTravelTimeMode()
                                            }
                                        ),
                                    painter = painterResource(com.z_company.route.R.drawable.schedule_24px),
                                    contentDescription = null,
                                    tint = if (formUiState.isShowTravelTime) MaterialTheme.colorScheme.tertiary else primaryColor
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
                    item {
                        TrainRouteSection(train = train)
                    }

//
//                    stationListState?.let { stationList ->
//                        val displayList =
//                            if (formUiState.isStationsReversed) stationList.reversed() else stationList
//                        itemsIndexed(
//                            items = displayList,
//                            key = { _, item -> item.id }
//                        ) { index, item ->
//                            val originalIndex =
//                                if (formUiState.isStationsReversed) stationList.size - 1 - index else index
//
//                            Spacer(
//                                modifier = Modifier
//                                    .height(8.dp)
//                                    .animateItem()
//                            )
//
//                            // Время перегона между станциями
//                            if (index > 0 && formUiState.isShowTravelTime) {
//                                val (fromIdx, toIdx) = if (formUiState.isStationsReversed) {
//                                    Pair(originalIndex, originalIndex + 1)
//                                } else {
//                                    Pair(originalIndex - 1, originalIndex)
//                                }
//                                if (fromIdx in stationList.indices && toIdx in stationList.indices) {
//                                    val depTime = stationList[fromIdx].departure.data
//                                    val arrTime = stationList[toIdx].arrival.data
//                                    if (depTime != null && arrTime != null && arrTime > depTime) {
//                                        val travelMinutesInLong = (arrTime - depTime)
//                                        val travelMinutes =
//                                            ConverterLongToTime.getTimeInStringFormat(
//                                                travelMinutesInLong
//                                            )
//                                        val travelTooltipState =
//                                            rememberBasicTooltipState(isPersistent = false)
//                                        Row(
//                                            modifier = Modifier
//                                                .animateItem()
//                                                .fillMaxWidth()
//                                                .padding(bottom = 4.dp)
//                                        ) {
//                                            BasicTooltipBox(
//                                                modifier = Modifier.weight(1f),
//                                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
//                                                tooltip = {
//                                                    Box(
//                                                        modifier = Modifier
//                                                            .background(
//                                                                shape = Shapes.medium,
//                                                                color = MaterialTheme.colorScheme.surface
//                                                            )
//                                                            .padding(12.dp)
//                                                    ) {
//                                                        Text(
//                                                            text = "Перегонное время",
//                                                            style = MaterialTheme.typography.bodyMedium,
//                                                            color = MaterialTheme.colorScheme.primary
//                                                        )
//                                                    }
//                                                },
//                                                state = travelTooltipState
//                                            ) {
//                                                Row(
//                                                    modifier = Modifier
//                                                        .fillMaxWidth()
//                                                ) {
//                                                    Text(
//                                                        modifier = Modifier
//                                                            .weight(1f)
//                                                            .noRippleEffect {
//                                                                scope.launch {
//                                                                    travelTooltipState.show(
//                                                                        MutatePriority.Default
//                                                                    )
//                                                                }
//                                                            },
//                                                        text = travelMinutes,
//                                                        textAlign = TextAlign.Center,
//                                                        style = MaterialTheme.typography.labelSmall,
//                                                        color = primaryColor.copy(alpha = 0.7f)
//                                                    )
//                                                }
//                                                Box(
//                                                    modifier = Modifier.weight(1f)
//                                                )
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//
//                            StationItem(
//                                modifier = Modifier.animateItem(),
//                                index = originalIndex,
//                                stationFormState = item,
//                                onDelete = onDeleteStation,
//                                menuList = menuList,
//                                isExpandedMenu = if (isExpandedMenu?.first == originalIndex) {
//                                    isExpandedMenu.second
//                                } else false,
//                                onExpandedMenuChange = onExpandedMenuChange,
//                                onChangedContentMenu = onChangedContentMenu,
//                                onStationNameChanged = onStationNameChanged,
//                                onArrivalTimeChanged = onArrivalTimeChanged,
//                                onDepartureTimeChanged = onDepartureTimeChanged,
//                                onDeleteStationName = onDeleteStationName,
//                                selectIndexState = selectSectionIndexState,
//                                dateAndTimeConverter = dateAndTimeConverter,
//                                trainNumber = train.number,
//                                isReorderMode = formUiState.isReorderMode,
//                                onMoveUp = if (originalIndex > 0) {
//                                    { viewModel.moveStation(originalIndex, originalIndex - 1) }
//                                } else null,
//                                onMoveDown = if (originalIndex < stationList.lastIndex) {
//                                    { viewModel.moveStation(originalIndex, originalIndex + 1) }
//                                } else null
//                            )
//                        }
//                    }
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
                                onAddingStation()
                                scope.launch {
                                    val countItems = scrollState.layoutInfo.totalItemsCount
                                    scrollState.animateScrollToItem(countItems)
                                }
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
