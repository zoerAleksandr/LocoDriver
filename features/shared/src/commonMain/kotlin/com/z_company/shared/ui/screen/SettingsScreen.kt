package com.z_company.shared.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.shared.ui.component.picker.TimePickerBottomSheet
import com.z_company.shared.util.ConverterLongToTime
import com.z_company.shared.util.TimeFormatter
import com.z_company.shared.viewmodel.SettingsSharedViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onShowSettingSalary: () -> Unit,
) {
    val viewModel: SettingsSharedViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        viewModel.saveEvent.collect { onBackClick() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(
                        onClick = viewModel::saveSettings,
                        colors = ButtonDefaults.buttonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = Color.Transparent,
                        ),
                    ) {
                        Text("Сохранить", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                },
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        when (val settingDetails = uiState.settingDetails) {
            is ResultState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ResultState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Ошибка загрузки настроек", style = styleData, color = MaterialTheme.colorScheme.error)
                }
            }

            is ResultState.Success -> {
                val setting = settingDetails.data ?: return@Scaffold

                // Dialog states
                var showNightTimeStartDialog by remember { mutableStateOf(false) }
                var showNightTimeEndDialog by remember { mutableStateOf(false) }
                var showRestDialog by remember { mutableStateOf(false) }
                var showHomeRestDialog by remember { mutableStateOf(false) }
                var showWorkTimeDialog by remember { mutableStateOf(false) }

                // Service phase dialog
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var departureStation by remember(uiState.showDialogAddServicePhase) {
                    mutableStateOf(uiState.selectedServicePhase?.first?.departureStation ?: "")
                }
                var arrivalStation by remember(uiState.showDialogAddServicePhase) {
                    mutableStateOf(uiState.selectedServicePhase?.first?.arrivalStation ?: "")
                }
                var distance by remember(uiState.showDialogAddServicePhase) {
                    mutableStateOf(uiState.selectedServicePhase?.first?.distance?.toString() ?: "")
                }

                // Service phase add/edit bottom sheet
                if (uiState.showDialogAddServicePhase) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.hideDialogAddServicePhase() },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 8.dp,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Новое плечо", modifier = Modifier.padding(bottom = 16.dp), style = styleTitle, color = primaryColor)
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = departureStation,
                                onValueChange = { departureStation = it },
                                placeholder = { Text("От станции", style = styleData, color = primaryColor.copy(alpha = 0.5f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = arrivalStation,
                                onValueChange = { arrivalStation = it },
                                placeholder = { Text("До станции", style = styleData, color = primaryColor.copy(alpha = 0.5f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = distance,
                                onValueChange = { distance = it },
                                placeholder = { Text("Расстояние", style = styleData, color = primaryColor.copy(alpha = 0.5f)) },
                                suffix = { Text("км", style = styleHint, color = primaryColor.copy(alpha = 0.5f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            )
                            Button(
                                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                enabled = departureStation.isNotBlank() && arrivalStation.isNotBlank() && (distance.toIntOrNull() ?: 0) > 0,
                                onClick = {
                                    val dist = distance.toIntOrNull() ?: 0
                                    if (dist > 0) {
                                        val phase = ServicePhase(departureStation = departureStation.trim(), arrivalStation = arrivalStation.trim(), distance = dist)
                                        val index = uiState.selectedServicePhase?.second ?: -1
                                        viewModel.addServicePhase(phase, index)
                                    }
                                },
                            ) {
                                Text("Добавить", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                // Time picker dialogs
                if (showNightTimeStartDialog) {
                    val initMillis = (setting.nightTime.startNightHour * 3_600_000L + setting.nightTime.startNightMinute * 60_000L)
                    TimePickerBottomSheet(
                        title = "Начало ночи",
                        initialTimeMillis = initMillis,
                        onTimeSelected = { millis ->
                            val h = ConverterLongToTime.getHour(millis)
                            val m = ConverterLongToTime.getRemainingMinuteFromHour(millis)
                            viewModel.changeStartNightTime(h, m)
                            showNightTimeStartDialog = false
                            showNightTimeEndDialog = true
                        },
                        onDismiss = { showNightTimeStartDialog = false },
                    )
                }
                if (showNightTimeEndDialog) {
                    val initMillis = (setting.nightTime.endNightHour * 3_600_000L + setting.nightTime.endNightMinute * 60_000L)
                    TimePickerBottomSheet(
                        title = "Окончание ночи",
                        initialTimeMillis = initMillis,
                        onTimeSelected = { millis ->
                            val h = ConverterLongToTime.getHour(millis)
                            val m = ConverterLongToTime.getRemainingMinuteFromHour(millis)
                            viewModel.changeEndNightTime(h, m)
                        },
                        onDismiss = { showNightTimeEndDialog = false },
                    )
                }
                if (showRestDialog) {
                    TimePickerBottomSheet(
                        title = "Минимальный отдых в ПО",
                        initialTimeMillis = setting.minTimeRestPointOfTurnover,
                        onTimeSelected = { viewModel.changeMinTimeRest(it) },
                        onDismiss = { showRestDialog = false },
                    )
                }
                if (showHomeRestDialog) {
                    TimePickerBottomSheet(
                        title = "Минимальный домашний отдых",
                        initialTimeMillis = setting.minTimeHomeRest,
                        onTimeSelected = { viewModel.changeMinTimeHomeRest(it) },
                        onDismiss = { showHomeRestDialog = false },
                    )
                }
                if (showWorkTimeDialog) {
                    TimePickerBottomSheet(
                        title = "Время работы по умолчанию",
                        initialTimeMillis = setting.defaultWorkTime,
                        onTimeSelected = { viewModel.changeDefaultWorkTime(it) },
                        onDismiss = { showWorkTimeDialog = false },
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(padding).padding(horizontal = 12.dp),
                ) {
                    // --- Timezone ---
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            Text(
                                "Домашний часовой пояс",
                                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                                style = styleTitle, color = primaryColor, overflow = TextOverflow.Ellipsis,
                            )
                            Box(
                                Modifier.background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium).fillMaxWidth(),
                            ) {
                                val currentTz = viewModel.timeZoneList.find { it.offsetOfMoscow == setting.timeZone }
                                    ?: viewModel.timeZoneList[1]
                                var selectedTz by remember { mutableStateOf(currentTz) }
                                var expanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                    OutlinedTextField(
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        value = selectedTz.description,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                        textStyle = styleData.copy(color = primaryColor),
                                    )
                                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        viewModel.timeZoneList.forEach { item ->
                                            DropdownMenuItem(
                                                text = { Text(item.description, color = primaryColor, style = styleHint) },
                                                onClick = {
                                                    selectedTz = item
                                                    viewModel.setTimeZone(item.offsetOfMoscow)
                                                    expanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                "Установите местный часовой пояс. Будет учитываться при расчете ночных, праздничных часов и переходных поездках.",
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                                style = styleHint, maxLines = Int.MAX_VALUE,
                            )
                        }
                    }

                    // --- Time settings ---
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            Column(
                                modifier = Modifier
                                    .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Night time
                                Row(
                                    Modifier.fillMaxWidth().clickable { showNightTimeStartDialog = true },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Ночь", style = styleData, color = primaryColor, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(setting.nightTime.toString(), style = styleData, color = primaryColor)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Rest at ПО
                                Row(
                                    Modifier.fillMaxWidth().clickable { showRestDialog = true },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Отдых в ПО", style = styleData, color = primaryColor, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(ConverterLongToTime.getTimeInStringFormat(setting.minTimeRestPointOfTurnover), style = styleData, color = primaryColor)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Home rest
                                Row(
                                    Modifier.fillMaxWidth().clickable { showHomeRestDialog = true },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Домашний отдых", style = styleData, color = primaryColor, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(ConverterLongToTime.getTimeInStringFormat(setting.minTimeHomeRest), style = styleData, color = primaryColor)
                                }
                            }
                            Text(
                                "Установите время минимального отдыха. Это значение будет использовано при расчете отдыха после поездки.",
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                                style = styleHint, color = primaryColor,
                            )
                        }
                    }

                    // --- Service phases ---
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Плечи", style = styleTitle, color = primaryColor, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = {
                                departureStation = ""
                                arrivalStation = ""
                                distance = ""
                                viewModel.showDialogAddServicePhase(ServicePhase(departureStation = "", arrivalStation = "", distance = 0))
                            }) {
                                Text("Добавить", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }

                    itemsIndexed(
                        items = uiState.servicePhases,
                        key = { index, item -> item.hashCode() + index },
                    ) { index, item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteServicePhase(index)
                                    false
                                } else false
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                        else -> Color.Transparent
                                    }, label = "",
                                )
                                Box(
                                    Modifier.fillMaxSize().background(color, MaterialTheme.shapes.medium),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.surface)
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
                                    .clickable { viewModel.selectToUpdateServicePhase(item, index) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("${item.departureStation} - ${item.arrivalStation}", modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 2, style = styleData)
                                Text("${item.distance} км", overflow = TextOverflow.Visible, style = styleData)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // --- Default data ---
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                            Text("Данные по умолчанию", modifier = Modifier.padding(start = 16.dp, bottom = 6.dp), style = styleTitle, color = primaryColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Column(
                                modifier = Modifier
                                    .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Loco type
                                Row(Modifier.fillMaxWidth().clickable { viewModel.changeDefaultLocoType() }, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Вид тяги", modifier = Modifier.weight(1f), style = styleData, color = primaryColor)
                                    Text(setting.defaultLocoType.text, style = styleData, color = primaryColor)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Time format
                                Row(Modifier.fillMaxWidth().clickable { viewModel.changeTimeFormat() }, Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Формат времени", modifier = Modifier.padding(end = 12.dp).weight(1f), style = styleData, color = primaryColor, overflow = TextOverflow.Ellipsis, maxLines = 2)
                                    Text(if (setting.isDecimalTime) "12,5" else "12:30", style = styleData, color = primaryColor)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Using default work time
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Использовать cтандартное время работы", modifier = Modifier.padding(end = 12.dp).weight(1f), style = styleData, color = primaryColor, overflow = TextOverflow.Ellipsis, maxLines = 2)
                                    Switch(checked = setting.usingDefaultWorkTime, onCheckedChange = { viewModel.changeUsingDefaultWorkTime(it) })
                                }

                                AnimatedVisibility(visible = setting.usingDefaultWorkTime) {
                                    Row(Modifier.fillMaxWidth().clickable { showWorkTimeDialog = true }, Arrangement.SpaceBetween) {
                                        Text("Время работы", modifier = Modifier.weight(1f), style = styleData, color = primaryColor)
                                        Text(ConverterLongToTime.getTimeInStringFormat(setting.defaultWorkTime), style = styleData, color = primaryColor)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Consider future routes
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Учитывать будущие маршруты", modifier = Modifier.padding(end = 12.dp).weight(1f), style = styleData, color = primaryColor, overflow = TextOverflow.Ellipsis, maxLines = 2)
                                    Switch(checked = setting.isConsiderFutureRoute, onCheckedChange = { viewModel.changeConsiderFutureRoute(it) })
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Show break
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Показывать перерыв", modifier = Modifier.padding(end = 12.dp).weight(1f), style = styleData, color = primaryColor, overflow = TextOverflow.Ellipsis, maxLines = 2)
                                    Switch(checked = setting.isShowBreak, onCheckedChange = { viewModel.changeShowBreak(it) })
                                }
                            }
                        }
                    }

                    // --- Salary settings button ---
                    item {
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onShowSettingSalary, modifier = Modifier.fillMaxWidth()) {
                            Text("Настройка зарплаты")
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
