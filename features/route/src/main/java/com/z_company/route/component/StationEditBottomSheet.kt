package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.z_company.core.ui.component.DateTimePickerBottomSheet
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.route.viewmodel.StationFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationEditBottomSheet(
    stationFormState: StationFormState?,
    menuList: List<String>,
    onFilterMenu: (String) -> Unit,
    onDeleteStationName: (String) -> Unit,
    onSave: (name: String?, arrival: Long?, departure: Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    val isNewStation = stationFormState == null
    val title = if (isNewStation) "Новая станция" else "Редактировать станцию"

    var localName by remember {
        mutableStateOf(
            TextFieldValue(
                text = stationFormState?.station?.data ?: "",
                selection = TextRange(stationFormState?.station?.data?.length ?: 0)
            )
        )
    }
    var localArrival by remember { mutableStateOf(stationFormState?.arrival?.data) }
    var localDeparture by remember { mutableStateOf(stationFormState?.departure?.data) }

    var showArrivalPicker by remember { mutableStateOf(false) }
    var showDeparturePicker by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val dataTextStyle = MaterialTheme.typography.bodyLarge
    val hintStyle = MaterialTheme.typography.bodyMedium

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = Shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Station name with autocomplete
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = it }
            ) {
                OutlinedTextFieldApp(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    value = localName,
                    onValueChange = { newValue ->
                        localName = newValue
                        onFilterMenu(newValue.text)
                        isDropdownExpanded = newValue.text.isNotEmpty()
                    },
                    placeholder = {
                        Text(
                            text = "Станция",
                            style = hintStyle,
                            color = primaryColor.copy(alpha = 0.5f)
                        )
                    },
                    textStyle = dataTextStyle,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            isDropdownExpanded = false
                        }
                    ),
                    singleLine = true
                )

                if (menuList.isNotEmpty() && isDropdownExpanded) {
                    DropdownMenu(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = Shapes.medium
                            )
                            .exposedDropdownSize(true),
                        expanded = isDropdownExpanded,
                        properties = PopupProperties(focusable = false),
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        menuList.forEach { stationName ->
                            DropdownMenuItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = Shapes.medium
                                    ),
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stationName,
                                            style = dataTextStyle,
                                            color = primaryColor
                                        )
                                        Icon(
                                            modifier = Modifier.clickable {
                                                onDeleteStationName(stationName)
                                            },
                                            painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                                            contentDescription = null,
                                            tint = primaryColor
                                        )
                                    }
                                },
                                onClick = {
                                    localName = localName.copy(
                                        text = stationName,
                                        selection = TextRange(stationName.length)
                                    )
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Arrival time row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )
                    .clickable { showArrivalPicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Прибытие",
                    style = hintStyle,
                    color = primaryColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val arrivalText = localArrival?.let {
                        dateAndTimeConverter?.getTimeFromDateLong(it)
                    } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT
                    Text(
                        text = arrivalText,
                        style = dataTextStyle,
                        color = if (localArrival != null) primaryColor else primaryColor.copy(alpha = 0.4f)
                    )
                    if (localArrival != null) {
                        IconButton(onClick = { localArrival = null }) {
                            Icon(
                                painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                                contentDescription = "Очистить",
                                tint = primaryColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Quick adjust buttons for arrival
            TimeQuickAdjustRow(
                currentTime = localArrival,
                onTimeChanged = { localArrival = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Departure time row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )
                    .clickable { showDeparturePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Отправление",
                    style = hintStyle,
                    color = primaryColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val departureText = localDeparture?.let {
                        dateAndTimeConverter?.getTimeFromDateLong(it)
                    } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT
                    Text(
                        text = departureText,
                        style = dataTextStyle,
                        color = if (localDeparture != null) primaryColor else primaryColor.copy(alpha = 0.4f)
                    )
                    if (localDeparture != null) {
                        IconButton(onClick = { localDeparture = null }) {
                            Icon(
                                painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                                contentDescription = "Очистить",
                                tint = primaryColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Quick adjust buttons for departure
            TimeQuickAdjustRow(
                currentTime = localDeparture,
                onTimeChanged = { localDeparture = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                onClick = {
                    val name = localName.text.ifBlank { null }
                    onSave(name, localArrival, localDeparture)
                }
            ) {
                Text(
                    text = "Сохранить",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }

            // Delete button (only for existing stations)
            if (onDelete != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDelete
                ) {
                    Text(
                        text = "Удалить станцию",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Arrival DateTimePicker
    if (showArrivalPicker) {
        DateTimePickerBottomSheet(
            title = "Прибытие",
            onDateTimeSelected = { time ->
                localArrival = time
                showArrivalPicker = false
            },
            onDismiss = { showArrivalPicker = false },
            startDateTime = localArrival
        )
    }

    // Departure DateTimePicker
    if (showDeparturePicker) {
        DateTimePickerBottomSheet(
            title = "Отправление",
            onDateTimeSelected = { time ->
                localDeparture = time
                showDeparturePicker = false
            },
            onDismiss = { showDeparturePicker = false },
            startDateTime = localDeparture
        )
    }
}

/**
 * Строка кнопок быстрой корректировки времени:
 * -10  -5  -1  [Сейчас]  +1  +5  +10
 */
@Composable
private fun TimeQuickAdjustRow(
    currentTime: Long?,
    onTimeChanged: (Long) -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val chipShape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Минусовые кнопки
        listOf(-10, -5, -1).forEach { minutes ->
            Box(
                modifier = Modifier
                    .border(
                        width = 0.5.dp,
                        color = primaryColor.copy(alpha = 0.3f),
                        shape = chipShape
                    )
                    .clickable {
                        val base = currentTime ?: System.currentTimeMillis()
                        onTimeChanged(base + minutes * 60_000L)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$minutes",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor
                )
            }
        }

        // Кнопка "Сейчас"
        Box(
            modifier = Modifier
                .border(
                    width = 0.5.dp,
                    color = primaryColor.copy(alpha = 0.5f),
                    shape = chipShape
                )
                .background(
                    color = primaryColor.copy(alpha = 0.08f),
                    shape = chipShape
                )
                .clickable { onTimeChanged(System.currentTimeMillis()) }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Сейчас",
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor
            )
        }

        // Плюсовые кнопки
        listOf(1, 5, 10).forEach { minutes ->
            Box(
                modifier = Modifier
                    .border(
                        width = 0.5.dp,
                        color = primaryColor.copy(alpha = 0.3f),
                        shape = chipShape
                    )
                    .clickable {
                        val base = currentTime ?: System.currentTimeMillis()
                        onTimeChanged(base + minutes * 60_000L)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$minutes",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor
                )
            }
        }
    }
}
