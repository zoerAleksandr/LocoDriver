package com.z_company.route.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.key
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.route.component.StationDropdownMenu
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.util.toIntOrZero
import com.z_company.route.R
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.viewmodel.SettingsUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsShouldersContent(
    settingsUiState: SettingsUiState,
    servicePhases: SnapshotStateList<ServicePhase>?,
    stationList: List<String>,
    showDialogAddServicePhase: (ServicePhase) -> Unit,
    hideDialogAddServicePhase: () -> Unit,
    addServicePhase: (ServicePhase, Int) -> Unit,
    deleteServicePhase: (ServicePhase) -> Unit,
    updateServicePhase: (ServicePhase, Int) -> Unit,
    onDeleteStationName: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val styleData = MaterialTheme.typography.bodyLarge
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary
    val noValueColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var departureStation by remember(settingsUiState.showDialogAddServicePhase) {
        mutableStateOf(TextFieldValue(settingsUiState.selectedServicePhase?.first?.departureStation ?: ""))
    }
    var arrivalStation by remember(settingsUiState.showDialogAddServicePhase) {
        mutableStateOf(TextFieldValue(settingsUiState.selectedServicePhase?.first?.arrivalStation ?: ""))
    }
    var distance by remember(settingsUiState.showDialogAddServicePhase) {
        mutableStateOf(settingsUiState.selectedServicePhase?.first?.distance?.toString() ?: "")
    }
    var createReverse by remember(settingsUiState.showDialogAddServicePhase) {
        mutableStateOf(false)
    }

    if (settingsUiState.showDialogAddServicePhase) {
        ModalBottomSheet(
            onDismissRequest = { hideDialogAddServicePhase() },
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
            val focusManager = LocalFocusManager.current

            var filteredDepartureStations by remember { mutableStateOf(stationList) }
            var filteredArrivalStations by remember { mutableStateOf(stationList) }
            var isDepartureDropdownExpanded by remember { mutableStateOf(false) }
            var isArrivalDropdownExpanded by remember { mutableStateOf(false) }

            LaunchedEffect(settingsUiState.selectedServicePhase) {
                if (settingsUiState.selectedServicePhase != null) {
                    val phase = settingsUiState.selectedServicePhase.first
                    departureStation = TextFieldValue(phase.departureStation, TextRange(phase.departureStation.length))
                    arrivalStation = TextFieldValue(phase.arrivalStation, TextRange(phase.arrivalStation.length))
                    distance = phase.distance.toString()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = if (settingsUiState.selectedServicePhase != null) "Редактировать плечо" else "Новое плечо",
                    style = styleTitle,
                    color = primaryColor
                )

                // Departure station with dropdown
                ExposedDropdownMenuBox(
                    expanded = isDepartureDropdownExpanded,
                    onExpandedChange = { isDepartureDropdownExpanded = it }
                ) {
                    OutlinedTextFieldApp(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                        value = departureStation,
                        onValueChange = { newValue ->
                            departureStation = newValue
                            filteredDepartureStations = stationList.filter {
                                it.contains(newValue.text, ignoreCase = true)
                            }
                            isDepartureDropdownExpanded = newValue.text.isNotEmpty()
                        },
                        placeholder = {
                            Text(text = "От станции", style = styleData, color = noValueColor)
                        },
                        textStyle = styleData,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text, imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = {
                            isDepartureDropdownExpanded = false
                            scope.launch { focusManager.moveFocus(FocusDirection.Down) }
                        }),
                        singleLine = true,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surface
                    )

                    StationDropdownMenu(
                        expanded = isDepartureDropdownExpanded,
                        stations = filteredDepartureStations,
                        onSelect = { station ->
                            departureStation = TextFieldValue(
                                text = station,
                                selection = TextRange(station.length)
                            )
                            isDepartureDropdownExpanded = false
                        },
                        onDelete = onDeleteStationName,
                        onDismiss = { isDepartureDropdownExpanded = false },
                        textStyle = styleData
                    )
                }

                // Arrival station with dropdown
                ExposedDropdownMenuBox(
                    expanded = isArrivalDropdownExpanded,
                    onExpandedChange = { isArrivalDropdownExpanded = it }
                ) {
                    OutlinedTextFieldApp(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                        value = arrivalStation,
                        onValueChange = { newValue ->
                            arrivalStation = newValue
                            filteredArrivalStations = stationList.filter {
                                it.contains(newValue.text, ignoreCase = true)
                            }
                            isArrivalDropdownExpanded = newValue.text.isNotEmpty()
                        },
                        placeholder = {
                            Text(text = "До станции", style = styleData, color = noValueColor)
                        },
                        textStyle = styleData,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text, imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = {
                            isArrivalDropdownExpanded = false
                            scope.launch { focusManager.moveFocus(FocusDirection.Down) }
                        }),
                        singleLine = true,
                        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surface
                    )

                    StationDropdownMenu(
                        expanded = isArrivalDropdownExpanded,
                        stations = filteredArrivalStations,
                        onSelect = { station ->
                            arrivalStation = TextFieldValue(
                                text = station,
                                selection = TextRange(station.length)
                            )
                            isArrivalDropdownExpanded = false
                        },
                        onDelete = onDeleteStationName,
                        onDismiss = { isArrivalDropdownExpanded = false },
                        textStyle = styleData
                    )
                }

                OutlinedTextFieldApp(
                    modifier = Modifier.fillMaxWidth(),
                    value = distance,
                    onValueChange = { distance = it },
                    placeholder = {
                        Text(text = "Расстояние", style = styleData, color = noValueColor)
                    },
                    suffix = {
                        Text(text = "км", style = styleHint, color = noValueColor)
                    },
                    textStyle = styleData,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        scope.launch { focusManager.clearFocus() }
                    }),
                    singleLine = true,
                    colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surface
                )

                // Reverse direction switch
                if (settingsUiState.selectedServicePhase == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Создать плечо в обратном направлении",
                            style = styleHint,
                            color = primaryColor,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )
                        Switch(
                            checked = createReverse,
                            onCheckedChange = { createReverse = it }
                        )
                    }
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
                    enabled = departureStation.text.isNotBlank() && arrivalStation.text.isNotBlank() && distance.toIntOrZero() > 0,
                    onClick = {
                        val dist = distance.toIntOrZero()
                        if (dist > 0 && departureStation.text.isNotBlank() && arrivalStation.text.isNotBlank()) {
                            val dep = departureStation.text.trim()
                            val arr = arrivalStation.text.trim()
                            val newPhase = ServicePhase(
                                departureStation = dep,
                                arrivalStation = arr,
                                distance = dist
                            )
                            val index = settingsUiState.selectedServicePhase?.second ?: -1
                            addServicePhase(newPhase, index)

                            if (createReverse && settingsUiState.selectedServicePhase == null) {
                                val reversePhase = ServicePhase(
                                    departureStation = arr,
                                    arrivalStation = dep,
                                    distance = dist
                                )
                                addServicePhase(reversePhase, -1)
                            }
                        }
                    }
                ) {
                    Text(
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        text = if (settingsUiState.selectedServicePhase != null) "Сохранить" else "Добавить"
                    )
                }
            }
        }
    }

    var pendingDeleteIndex by remember { mutableStateOf(-1) }
    var pendingDeleteItem by remember { mutableStateOf<ServicePhase?>(null) }

    if (pendingDeleteItem != null) {
        val deleteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val capturedItem = pendingDeleteItem!!
        AppBottomSheet(
            onDismissRequest = {
                pendingDeleteIndex = -1
                pendingDeleteItem = null
            },
            sheetState = deleteSheetState,
            title = "Удалить плечо?\n${capturedItem.departureStation} — ${capturedItem.arrivalStation}",
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    deleteServicePhase(capturedItem)
                    pendingDeleteIndex = -1
                    pendingDeleteItem = null
                }
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .verticalScroll(state = rememberScrollState())
    ) {
        servicePhases?.toList()?.forEachIndexed { index, item ->
            key(item.id) {
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        pendingDeleteIndex = index
                        pendingDeleteItem = item
                        false
                    } else {
                        true
                    }
                }
            )
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    val color by animateColorAsState(
                        when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.Settled -> Color.Transparent
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                            else -> Color.Transparent
                        }, label = ""
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color = color, shape = Shapes.medium),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            modifier = Modifier.padding(end = 16.dp),
                            painter = painterResource(R.drawable.delete_24px),
                            tint = MaterialTheme.colorScheme.onError,
                            contentDescription = null
                        )
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .shadow(elevation = 1.dp, shape = Shapes.medium)
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = Shapes.medium
                        )
                        .noRippleEffect {
                            updateServicePhase(
                                ServicePhase(
                                    departureStation = item.departureStation,
                                    arrivalStation = item.arrivalStation,
                                    distance = item.distance
                                ),
                                index
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "${item.departureStation} - ${item.arrivalStation}",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        style = styleData
                    )
                    Text(
                        text = "${item.distance} км",
                        overflow = TextOverflow.Visible,
                        style = styleData
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            } // key
        }

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
                departureStation = TextFieldValue("")
                arrivalStation = TextFieldValue("")
                distance = ""
                createReverse = false
                showDialogAddServicePhase(
                    ServicePhase(
                        departureStation = "",
                        arrivalStation = "",
                        distance = 0
                    )
                )
            }
        ) {
            Text(
                text = "Добавить плечо",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
