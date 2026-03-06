package com.z_company.shared.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.shadow
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
import com.z_company.shared.ui.component.AppBottomSheet
import com.z_company.shared.ui.component.BottomSheetAction
import com.z_company.shared.ui.component.picker.DateTimePickerBottomSheet
import com.z_company.shared.util.ConverterLongToTime
import com.z_company.shared.viewmodel.FormRouteSharedViewModel
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
    val homeRestDuration by viewModel.homeRestDuration.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    LaunchedEffect(routeId) { viewModel.loadRoute(routeId) }
    LaunchedEffect(isSaved) { if (isSaved) onBackClick() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
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
                        Text(text = "Сохранить", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val isFavorite = route?.basicData?.isFavorite == true
                        AnimatedContent(targetState = isFavorite, label = "") { fav ->
                            IconButton(onClick = {
                                viewModel.toggleFavorite(!fav)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (fav) "Убрали из избранного" else "Маршрут добавлен в избранное"
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (fav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        val isOnePerson = route?.basicData?.isOnePersonOperation == true
                        AnimatedContent(targetState = isOnePerson, label = "") { op ->
                            IconButton(onClick = {
                                viewModel.setOnePersonOperation(!op)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (op) "Работа в два лица" else "Работа в одно лицо"
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = if (op) Icons.Filled.Person else Icons.Filled.PersonOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        val isRestAtTurnover = route?.basicData?.restPointOfTurnover == true
                        AnimatedContent(targetState = isRestAtTurnover, label = "") { rest ->
                            IconButton(onClick = {
                                viewModel.setRestPointOfTurnover(!rest)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (rest) "Домашний отдых" else "Отдых в ПО"
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = if (rest) Icons.Filled.Hotel else Icons.Filled.Home,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val currentRoute = route ?: return@Scaffold
        val basicData = currentRoute.basicData
        val converter = dateAndTimeConverter

        // Bottom sheets for deleting time values
        var showDeleteStartWork by remember { mutableStateOf(false) }
        var showDeleteEndWork by remember { mutableStateOf(false) }
        var showDeleteStartBreak by remember { mutableStateOf(false) }
        var showDeleteEndBreak by remember { mutableStateOf(false) }

        if (showDeleteStartWork) {
            AppBottomSheet(
                onDismissRequest = { showDeleteStartWork = false },
                title = "Время явки",
                actions = listOf(BottomSheetAction("Удалить значение") { viewModel.setTimeStartWork(null) }),
            )
        }
        if (showDeleteEndWork) {
            AppBottomSheet(
                onDismissRequest = { showDeleteEndWork = false },
                title = "Время сдачи",
                actions = listOf(BottomSheetAction("Удалить значение") { viewModel.setTimeEndWork(null) }),
            )
        }
        if (showDeleteStartBreak) {
            AppBottomSheet(
                onDismissRequest = { showDeleteStartBreak = false },
                title = "Начало перерыва",
                actions = listOf(BottomSheetAction("Удалить значение") { viewModel.setTimeStartBreak(null) }),
            )
        }
        if (showDeleteEndBreak) {
            AppBottomSheet(
                onDismissRequest = { showDeleteEndBreak = false },
                title = "Окончание перерыва",
                actions = listOf(BottomSheetAction("Удалить значение") { viewModel.setTimeEndBreak(null) }),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            state = scrollState,
        ) {
            // --- Header: work time + stats ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Error card with gradient
                    errorMessage?.let { msg ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            ),
                        ) {
                            Text(
                                text = msg,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }

                    val startMs = basicData.timeStartWork
                    val endMs = basicData.timeEndWork
                    val breakDuration = currentRoute.getBreakDuration()
                    val workDurationMs = if (startMs != null && endMs != null && endMs > startMs) {
                        (endMs - startMs) - breakDuration
                    } else null

                    if (workDurationMs != null && workDurationMs > 0 && errorMessage == null) {
                        Text(
                            text = viewModel.convertTimeToStringFormat(workDurationMs),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            nightTimeMs?.let { nt ->
                                if (nt > 0L) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            modifier = Modifier.size(32.dp).padding(end = 4.dp),
                                            imageVector = Icons.Filled.DarkMode,
                                            contentDescription = null,
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
                                            modifier = Modifier.size(32.dp).padding(end = 4.dp),
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
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
                                        Text(text = "\uD83C\uDF89", modifier = Modifier.padding(end = 4.dp))
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

                    // Rest calculation
                    var isVisibleDetailRest by remember { mutableStateOf(false) }
                    if (workDurationMs != null && workDurationMs > 0) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(onClick = { isVisibleDetailRest = !isVisibleDetailRest }) {
                                Text(
                                    text = "Рассчитать отдых",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isVisibleDetailRest,
                            enter = slideInVertically(tween(150)) + fadeIn(tween(100)),
                            exit = fadeOut(tween(100)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceDim, MaterialTheme.shapes.medium)
                                    .border(0.5.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.medium)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (basicData.restPointOfTurnover) {
                                    Text("Отдых в пункте оборота", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    homeRestDuration?.let { restMs ->
                                        Text("Продлится ${ConverterLongToTime.formatDurationFromMillis(restMs)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        basicData.timeEndWork?.let { endWork ->
                                            converter?.getDateAndTime(endWork + restMs)?.takeIf { it.isNotBlank() }?.let {
                                                Text("До $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    } ?: Text("Невозможно рассчитать время отдыха.\nПроверьте начало и окончание работы.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Text("Домашний отдых", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    homeRestDuration?.let { restMs ->
                                        Text("Продлится ${ConverterLongToTime.formatDurationFromMillis(restMs)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        basicData.timeEndWork?.let { endWork ->
                                            converter?.getDateAndTime(endWork + restMs)?.takeIf { it.isNotBlank() }?.let {
                                                Text("До $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Text(
                                            "\nформула расчета\n(время рабочее * 2,6) - время отдыха в ПО",
                                            style = MaterialTheme.typography.labelLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } ?: Text("Невозможно рассчитать время отдыха.\nПроверьте начало и окончание работы во всей цепочке маршрутов.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // --- Route number + work times ---
            item {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = basicData.number ?: "",
                        onValueChange = { viewModel.updateNumber(it) },
                        label = { Text("№ маршрута") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    var showStartWorkPicker by remember { mutableStateOf(false) }
                    var showEndWorkPicker by remember { mutableStateOf(false) }

                    FormTimeRowAnimated(
                        label = "Явка", value = basicData.timeStartWork, converter = converter,
                        isFilled = basicData.timeStartWork != null,
                        onClick = { showStartWorkPicker = true },
                        onLongClick = { showDeleteStartWork = true },
                    )
                    FormTimeRowAnimated(
                        label = "Сдача", value = basicData.timeEndWork, converter = converter,
                        isFilled = basicData.timeEndWork != null,
                        onClick = { showEndWorkPicker = true },
                        onLongClick = { showDeleteEndWork = true },
                    )

                    if (showStartWorkPicker) {
                        DateTimePickerBottomSheet(
                            title = "Явка",
                            onDateTimeSelected = { viewModel.setTimeStartWork(it) },
                            onDismiss = { showStartWorkPicker = false },
                            startDateTime = basicData.timeStartWork ?: Clock.System.now().toEpochMilliseconds(),
                        )
                    }
                    if (showEndWorkPicker) {
                        DateTimePickerBottomSheet(
                            title = "Сдача",
                            onDateTimeSelected = { viewModel.setTimeEndWork(it) },
                            onDismiss = { showEndWorkPicker = false },
                            startDateTime = basicData.timeEndWork ?: basicData.timeStartWork ?: Clock.System.now().toEpochMilliseconds(),
                        )
                    }

                    // Break
                    val hasBreak = basicData.timeStartBreak != null || basicData.timeEndBreak != null
                    var isBreakVisible by remember { mutableStateOf(hasBreak) }
                    var showStartBreakPicker by remember { mutableStateOf(false) }
                    var showEndBreakPicker by remember { mutableStateOf(false) }

                    TextButton(onClick = { isBreakVisible = !isBreakVisible }) {
                        Text("Перерыв", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }

                    AnimatedVisibility(visible = isBreakVisible) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FormTimeRowAnimated(
                                label = "Начало", value = basicData.timeStartBreak, converter = converter,
                                isFilled = basicData.timeStartBreak != null,
                                onClick = { showStartBreakPicker = true },
                                onLongClick = { showDeleteStartBreak = true },
                            )
                            FormTimeRowAnimated(
                                label = "Окончание", value = basicData.timeEndBreak, converter = converter,
                                isFilled = basicData.timeEndBreak != null,
                                onClick = { showEndBreakPicker = true },
                                onLongClick = { showDeleteEndBreak = true },
                            )
                        }
                    }

                    if (showStartBreakPicker) {
                        DateTimePickerBottomSheet(
                            title = "Начало перерыва",
                            onDateTimeSelected = { viewModel.setTimeStartBreak(it) },
                            onDismiss = { showStartBreakPicker = false },
                            startDateTime = basicData.timeStartBreak ?: basicData.timeStartWork ?: Clock.System.now().toEpochMilliseconds(),
                        )
                    }
                    if (showEndBreakPicker) {
                        DateTimePickerBottomSheet(
                            title = "Окончание перерыва",
                            onDateTimeSelected = { viewModel.setTimeEndBreak(it) },
                            onDismiss = { showEndBreakPicker = false },
                            startDateTime = basicData.timeEndBreak ?: basicData.timeStartBreak ?: basicData.timeStartWork ?: Clock.System.now().toEpochMilliseconds(),
                        )
                    }
                }
            }

            // --- Locomotives ---
            item {
                ChildEntitySection(title = "Локомотив", items = currentRoute.locomotives,
                    onAddClick = { viewModel.preSaveRoute { basicId -> onNewLocoClick(basicId) } },
                ) { index, loco ->
                    ChildEntityRow(
                        onClick = { viewModel.preSaveRoute { basicId -> onLocoClick(loco.locoId, basicId) } },
                        onDelete = { viewModel.deleteLoco(loco) },
                    ) { LocomotiveSubItem(loco, index) }
                }
            }

            // --- Trains ---
            item {
                ChildEntitySection(title = "Поезд", items = currentRoute.trains,
                    onAddClick = { viewModel.preSaveRoute { basicId -> onNewTrainClick(basicId) } },
                ) { index, train ->
                    ChildEntityRow(
                        onClick = { viewModel.preSaveRoute { basicId -> onTrainClick(train.trainId, basicId) } },
                        onDelete = { viewModel.deleteTrain(train) },
                    ) { TrainSubItem(index, train) }
                }
            }

            // --- Passengers ---
            item {
                ChildEntitySection(title = "Пассажиром", items = currentRoute.passengers,
                    onAddClick = { viewModel.preSaveRoute { basicId -> onNewPassengerClick(basicId) } },
                ) { index, passenger ->
                    ChildEntityRow(
                        onClick = { viewModel.preSaveRoute { basicId -> onPassengerClick(passenger.passengerId, basicId) } },
                        onDelete = { viewModel.deletePassenger(passenger) },
                    ) { PassengerSubItem(index, passenger) }
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

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// --- Reusable composables ---

@Composable
private fun FormTimeRowAnimated(
    label: String,
    value: Long?,
    converter: com.z_company.shared.util.DateAndTimeConverter?,
    isFilled: Boolean,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isFilled) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium)
            .background(color = animatedBgColor, shape = MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
        )
        Text(
            text = value?.let { converter?.getDateAndTime(it) } ?: "",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun <T> ChildEntitySection(
    title: String, items: List<T>, onAddClick: () -> Unit,
    content: @Composable (index: Int, item: T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onAddClick) {
                Text("Добавить", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        if (items.isNotEmpty()) {
            Column(Modifier.padding(top = 8.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEachIndexed { index, item -> content(index, item) }
            }
        }
    }
}

@Composable
private fun ChildEntityRow(onClick: () -> Unit, onDelete: () -> Unit, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .background(color = MaterialTheme.colorScheme.secondary, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.weight(1f).padding(end = 8.dp)) { content() }
        Icon(
            modifier = Modifier.size(18.dp).clickable { onDelete() },
            imageVector = Icons.Default.Close, contentDescription = null,
        )
    }
}

@Composable
private fun LocomotiveSubItem(locomotive: Locomotive, index: Int) {
    val series = locomotive.series ?: locomotive.type.text
    val number = locomotive.number
    val numberText = if (number != null) "\u2116$number" else ""
    if (locomotive.series.isNullOrBlank() && locomotive.number.isNullOrBlank()) {
        Text("${locomotive.type.text} \u2116 ${index + 1}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    } else {
        Text("$series $numberText", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TrainSubItem(index: Int, train: Train) {
    if (train.number.isNullOrBlank()) {
        Text("Поезд \u2116 ${index + 1}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    } else {
        val stationStart = train.stations.firstOrNull()?.stationName ?: ""
        val stationEnd = if (train.stations.size > 1) " - ${train.stations.last().stationName ?: ""}" else ""
        Text("\u2116 ${train.number} $stationStart$stationEnd", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PassengerSubItem(index: Int, passenger: Passenger) {
    if (passenger.trainNumber.isNullOrBlank() && passenger.stationDeparture.isNullOrBlank() && passenger.stationArrival.isNullOrBlank()) {
        Text("Пассажиром \u2116 ${index + 1}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    } else {
        val textNumber = passenger.trainNumber?.let { "\u2116 $it" } ?: ""
        val textStationDeparture = passenger.stationDeparture ?: ""
        val textStationArrival = passenger.stationArrival?.let { " - $it" } ?: ""
        Text("$textNumber $textStationDeparture$textStationArrival", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
