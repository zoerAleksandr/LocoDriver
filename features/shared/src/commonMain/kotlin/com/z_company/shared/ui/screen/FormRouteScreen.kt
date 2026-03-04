package com.z_company.shared.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.shared.ui.component.picker.DateTimePickerBottomSheet
import com.z_company.shared.util.ConverterLongToTime
import com.z_company.shared.viewmodel.FormRouteSharedViewModel
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Route create/edit screen — full-featured, mirrors Android FormScreen.
 *
 * [routeId] == null → new route; non-null → editing existing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormRouteScreen(
    routeId: String?,
    onBackClick: () -> Unit,
    onLocoClick: (locoId: String, basicId: String) -> Unit,
    onNewLocoClick: (basicId: String) -> Unit,
    onTrainClick: (trainId: String, basicId: String) -> Unit,
    onNewTrainClick: (basicId: String) -> Unit,
    onPassengerClick: (passengerId: String, basicId: String) -> Unit,
    onNewPassengerClick: (basicId: String) -> Unit,
) {
    val viewModel: FormRouteSharedViewModel = koinInject()
    val route by viewModel.route.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val dateAndTimeConverter by viewModel.dateAndTimeConverter.collectAsState()
    val nightTimeMs by viewModel.nightTimeMs.collectAsState()
    val holidayTimeMs by viewModel.holidayTimeMs.collectAsState()
    val passengerTimeMs by viewModel.passengerTimeMs.collectAsState()
    val workTimeMs by viewModel.workTimeMs.collectAsState()
    val homeRestDuration by viewModel.homeRestDuration.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    LaunchedEffect(routeId) {
        viewModel.loadRoute(routeId)
    }
    LaunchedEffect(isSaved) {
        if (isSaved) onBackClick()
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    TextButton(
                        onClick = { viewModel.saveRoute() },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = Color.Transparent,
                        ),
                    ) {
                        Text(
                            text = "Сохранить",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Favorite toggle
                        val isFavorite = route?.basicData?.isFavorite == true
                        IconButton(onClick = {
                            viewModel.toggleFavorite(!isFavorite)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isFavorite) "Убрали из избранного" else "Маршрут добавлен в избранное"
                                )
                            }
                        }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Избранное",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // One person operation toggle
                        val isOnePerson = route?.basicData?.isOnePersonOperation == true
                        IconButton(onClick = {
                            viewModel.setOnePersonOperation(!isOnePerson)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isOnePerson) "Работа в два лица" else "Работа в одно лицо"
                                )
                            }
                        }) {
                            Icon(
                                imageVector = if (isOnePerson) Icons.Filled.Person else Icons.Filled.PersonOutline,
                                contentDescription = "Режим работы",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        // Rest point toggle
                        val isRestAtTurnover = route?.basicData?.restPointOfTurnover == true
                        IconButton(onClick = {
                            viewModel.setRestPointOfTurnover(!isRestAtTurnover)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isRestAtTurnover) "Домашний отдых" else "Отдых в ПО"
                                )
                            }
                        }) {
                            Icon(
                                imageVector = if (isRestAtTurnover) Icons.Filled.Hotel else Icons.Filled.Home,
                                contentDescription = "Тип отдыха",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val currentRoute = route ?: return@Scaffold
        val basicData = currentRoute.basicData
        val converter = dateAndTimeConverter

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            state = scrollState,
        ) {
            // --- Header: work time + calculated stats ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Error message
                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    // Work time display
                    val startMs = basicData.timeStartWork
                    val endMs = basicData.timeEndWork
                    val breakDuration = currentRoute.getBreakDuration()
                    val workDurationMs = if (startMs != null && endMs != null && endMs > startMs) {
                        (endMs - startMs) - breakDuration
                    } else null

                    if (workDurationMs != null && workDurationMs > 0) {
                        Text(
                            text = viewModel.convertTimeToStringFormat(workDurationMs),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        // Stats row: night, passenger, holiday
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            nightTimeMs?.let { nt ->
                                if (nt > 0L) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            modifier = Modifier.size(24.dp).padding(end = 4.dp),
                                            imageVector = Icons.Filled.DarkMode,
                                            contentDescription = "Ночные",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = viewModel.convertTimeToStringFormat(nt),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            passengerTimeMs?.let { pt ->
                                if (pt > 0L) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            modifier = Modifier.size(24.dp).padding(end = 4.dp),
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = "Пассажиром",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = viewModel.convertTimeToStringFormat(pt),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            holidayTimeMs?.let { ht ->
                                if (ht > 0L) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "🎉",
                                            modifier = Modifier.padding(end = 4.dp),
                                        )
                                        Text(
                                            text = viewModel.convertTimeToStringFormat(ht),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Rest calculation expandable
                    var isVisibleDetailRest by remember { mutableStateOf(false) }
                    if (workDurationMs != null && workDurationMs > 0) {
                        TextButton(
                            onClick = { isVisibleDetailRest = !isVisibleDetailRest },
                        ) {
                            Text(
                                text = "Рассчитать отдых",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }

                        AnimatedVisibility(
                            visible = isVisibleDetailRest,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.medium,
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = MaterialTheme.shapes.medium,
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (basicData.restPointOfTurnover) {
                                    // Rest at point of turnover — simplified
                                    Text(
                                        text = "Отдых в пункте оборота",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    homeRestDuration?.let { restMs ->
                                        Text(
                                            text = "Продлится ${ConverterLongToTime.formatDurationFromMillis(restMs)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        val endWork = basicData.timeEndWork
                                        if (endWork != null) {
                                            val endRestTime = endWork + restMs
                                            val endRestText = converter?.getDateAndTime(endRestTime) ?: ""
                                            if (endRestText.isNotBlank()) {
                                                Text(
                                                    text = "До $endRestText",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    } ?: Text(
                                        text = "Невозможно рассчитать время отдыха.\nПроверьте начало и окончание работы.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    // Home rest
                                    Text(
                                        text = "Домашний отдых",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    homeRestDuration?.let { restMs ->
                                        Text(
                                            text = "Продлится ${ConverterLongToTime.formatDurationFromMillis(restMs)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        val endWork = basicData.timeEndWork
                                        if (endWork != null) {
                                            val endRestTime = endWork + restMs
                                            val endRestText = converter?.getDateAndTime(endRestTime) ?: ""
                                            if (endRestText.isNotBlank()) {
                                                Text(
                                                    text = "До $endRestText",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                        Text(
                                            text = "\nформула расчета\n(время рабочее * 2,6) - время отдыха в ПО",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontStyle = FontStyle.Italic,
                                                fontWeight = FontWeight.Light,
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } ?: Text(
                                        text = "Невозможно рассчитать время отдыха.\nПроверьте начало и окончание работы во всей цепочке маршрутов.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Route number ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = basicData.number ?: "",
                        onValueChange = { viewModel.updateNumber(it) },
                        label = { Text("№ маршрута") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // --- Work Times with DateTimePicker ---
                    var showStartWorkPicker by remember { mutableStateOf(false) }
                    var showEndWorkPicker by remember { mutableStateOf(false) }

                    FormTimeRow(
                        label = "Явка",
                        value = basicData.timeStartWork,
                        converter = converter,
                        isFilled = basicData.timeStartWork != null,
                        onClick = { showStartWorkPicker = true },
                    )
                    FormTimeRow(
                        label = "Сдача",
                        value = basicData.timeEndWork,
                        converter = converter,
                        isFilled = basicData.timeEndWork != null,
                        onClick = { showEndWorkPicker = true },
                    )

                    if (showStartWorkPicker) {
                        DateTimePickerBottomSheet(
                            title = "Явка",
                            onDateTimeSelected = { viewModel.setTimeStartWork(it) },
                            onDismiss = { showStartWorkPicker = false },
                            startDateTime = basicData.timeStartWork
                                ?: Clock.System.now().toEpochMilliseconds(),
                        )
                    }
                    if (showEndWorkPicker) {
                        DateTimePickerBottomSheet(
                            title = "Сдача",
                            onDateTimeSelected = { viewModel.setTimeEndWork(it) },
                            onDismiss = { showEndWorkPicker = false },
                            startDateTime = basicData.timeEndWork
                                ?: basicData.timeStartWork
                                ?: Clock.System.now().toEpochMilliseconds(),
                        )
                    }

                    // --- Break ---
                    val hasBreak = basicData.timeStartBreak != null || basicData.timeEndBreak != null
                    var isBreakVisible by remember { mutableStateOf(hasBreak) }
                    var showStartBreakPicker by remember { mutableStateOf(false) }
                    var showEndBreakPicker by remember { mutableStateOf(false) }

                    TextButton(
                        onClick = { isBreakVisible = !isBreakVisible },
                    ) {
                        Text(
                            text = "Перерыв",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }

                    AnimatedVisibility(visible = isBreakVisible) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FormTimeRow(
                                label = "Начало",
                                value = basicData.timeStartBreak,
                                converter = converter,
                                isFilled = basicData.timeStartBreak != null,
                                onClick = { showStartBreakPicker = true },
                            )
                            FormTimeRow(
                                label = "Окончание",
                                value = basicData.timeEndBreak,
                                converter = converter,
                                isFilled = basicData.timeEndBreak != null,
                                onClick = { showEndBreakPicker = true },
                            )
                        }
                    }

                    if (showStartBreakPicker) {
                        DateTimePickerBottomSheet(
                            title = "Начало перерыва",
                            onDateTimeSelected = { viewModel.setTimeStartBreak(it) },
                            onDismiss = { showStartBreakPicker = false },
                            startDateTime = basicData.timeStartBreak
                                ?: basicData.timeStartWork
                                ?: Clock.System.now().toEpochMilliseconds(),
                        )
                    }
                    if (showEndBreakPicker) {
                        DateTimePickerBottomSheet(
                            title = "Окончание перерыва",
                            onDateTimeSelected = { viewModel.setTimeEndBreak(it) },
                            onDismiss = { showEndBreakPicker = false },
                            startDateTime = basicData.timeEndBreak
                                ?: basicData.timeStartBreak
                                ?: basicData.timeStartWork
                                ?: Clock.System.now().toEpochMilliseconds(),
                        )
                    }
                }
            }

            // --- Locomotives ---
            item {
                ChildEntitySection(
                    title = "Локомотив",
                    items = currentRoute.locomotives,
                    onAddClick = { viewModel.preSaveRoute { basicId -> onNewLocoClick(basicId) } },
                ) { index, loco ->
                    ChildEntityRow(
                        onClick = {
                            viewModel.preSaveRoute { basicId ->
                                onLocoClick(loco.locoId, basicId)
                            }
                        },
                        onDelete = { viewModel.deleteLoco(loco) },
                    ) {
                        LocomotiveSubItem(loco, index)
                    }
                }
            }

            // --- Trains ---
            item {
                ChildEntitySection(
                    title = "Поезд",
                    items = currentRoute.trains,
                    onAddClick = { viewModel.preSaveRoute { basicId -> onNewTrainClick(basicId) } },
                ) { index, train ->
                    ChildEntityRow(
                        onClick = {
                            viewModel.preSaveRoute { basicId ->
                                onTrainClick(train.trainId, basicId)
                            }
                        },
                        onDelete = { viewModel.deleteTrain(train) },
                    ) {
                        TrainSubItem(index, train)
                    }
                }
            }

            // --- Passengers ---
            item {
                ChildEntitySection(
                    title = "Пассажиром",
                    items = currentRoute.passengers,
                    onAddClick = { viewModel.preSaveRoute { basicId -> onNewPassengerClick(basicId) } },
                ) { index, passenger ->
                    ChildEntityRow(
                        onClick = {
                            viewModel.preSaveRoute { basicId ->
                                onPassengerClick(passenger.passengerId, basicId)
                            }
                        },
                        onDelete = { viewModel.deletePassenger(passenger) },
                    ) {
                        PassengerSubItem(index, passenger)
                    }
                }
            }

            // --- Notes ---
            item {
                OutlinedTextField(
                    value = basicData.notes ?: "",
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Примечания") },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 105.dp),
                    maxLines = 4,
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- Reusable composables ---

@Composable
private fun FormTimeRow(
    label: String,
    value: Long?,
    converter: com.z_company.shared.util.DateAndTimeConverter?,
    isFilled: Boolean,
    onClick: () -> Unit = {},
) {
    val bgColor = if (isFilled) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = bgColor,
                shape = MaterialTheme.shapes.medium,
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
        )
        Text(
            text = value?.let { converter?.getDateAndTime(it) } ?: "",
            color = if (isFilled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun <T> ChildEntitySection(
    title: String,
    items: List<T>,
    onAddClick: () -> Unit,
    content: @Composable (index: Int, item: T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onAddClick) {
                Text(
                    text = "Добавить",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        if (items.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEachIndexed { index, item ->
                    content(index, item)
                }
            }
        }
    }
}

@Composable
private fun ChildEntityRow(
    onClick: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        ) {
            content()
        }
        Icon(
            modifier = Modifier
                .size(18.dp)
                .clickable { onDelete() },
            imageVector = Icons.Default.Close,
            contentDescription = "Удалить",
        )
    }
}

@Composable
private fun LocomotiveSubItem(locomotive: Locomotive, index: Int) {
    val series = locomotive.series ?: locomotive.type.text
    val number = locomotive.number
    val numberText = if (number != null) "№$number" else ""
    val type = locomotive.type.text

    if (locomotive.series.isNullOrBlank() && locomotive.number.isNullOrBlank()) {
        Text(
            text = "$type № ${index + 1}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        Text(
            text = "$series $numberText",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TrainSubItem(index: Int, train: Train) {
    if (train.number.isNullOrBlank()) {
        Text(
            text = "Поезд № ${index + 1}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        val stationStart = if (train.stations.isNotEmpty()) {
            train.stations.first().stationName ?: ""
        } else ""

        val stationEnd = if (train.stations.isNotEmpty() && train.stations.size > 1) {
            " - ${train.stations.last().stationName ?: ""}"
        } else ""

        Text(
            text = "№ ${train.number} $stationStart$stationEnd",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PassengerSubItem(index: Int, passenger: Passenger) {
    if (passenger.trainNumber.isNullOrBlank() &&
        passenger.stationDeparture.isNullOrBlank() &&
        passenger.stationArrival.isNullOrBlank()
    ) {
        Text(
            text = "Пассажиром № ${index + 1}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        val textNumber = passenger.trainNumber?.let { "№ $it" } ?: ""
        val textStationDeparture = passenger.stationDeparture ?: ""
        val textStationArrival = passenger.stationArrival?.let { " - $it" } ?: ""
        Text(
            text = "$textNumber $textStationDeparture$textStationArrival",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
