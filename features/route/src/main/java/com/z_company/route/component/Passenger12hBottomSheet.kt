package com.z_company.route.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.z_company.core.ui.component.DateTimePickerBottomSheet
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Passenger12hBottomSheet(
    prefilledTimeDeparture: Long,
    prefilledTimeArrival: Long,
    prefilledStationDeparture: String?,
    workTimeStart: Long,
    workTimeEnd: Long,
    stationList: List<String>,
    dateAndTimeConverter: DateAndTimeConverter?,
    onSave: (stationDep: String?, stationArr: String?, timeDep: Long, timeArr: Long) -> Unit,
    onDismissNo: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val hintColor = primaryColor.copy(alpha = 0.5f)
    val dataTextStyle = MaterialTheme.typography.bodyLarge
    val hintStyle = MaterialTheme.typography.bodyMedium
    val fieldColor = MaterialTheme.colorScheme.surface
    val fieldShape = RoundedCornerShape(14.dp)

    var showForm by remember { mutableStateOf(false) }

    var timeDeparture by remember { mutableStateOf(prefilledTimeDeparture) }
    var timeArrival by remember { mutableStateOf(prefilledTimeArrival) }
    var stationDeparture by remember {
        mutableStateOf(
            TextFieldValue(
                text = prefilledStationDeparture ?: "",
                selection = TextRange(prefilledStationDeparture?.length ?: 0)
            )
        )
    }
    var stationArrival by remember {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange(0)))
    }

    var showDeparturePicker by remember { mutableStateOf(false) }
    var showArrivalPicker by remember { mutableStateOf(false) }
    var isDropdownDepartureExpanded by remember { mutableStateOf(false) }
    var isDropdownArrivalExpanded by remember { mutableStateOf(false) }

    val timeError = if (timeDeparture < workTimeStart || timeArrival > workTimeEnd || timeDeparture >= timeArrival) {
        "Время следования должно быть в пределах рабочего времени"
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = Shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = if (showForm) "Пассажиром" else "Время работы превышает 12 часов",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = !showForm) {
                Column {
                    Text(
                        text = "Записать время свыше 12 часов как следование пассажиром?",
                        style = hintStyle,
                        color = primaryColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Изменить в настройках",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.clickable {
                            onDismiss()
                            onNavigateToSettings()
                        }
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onDismissNo() },
                            modifier = Modifier.weight(1f),
                            shape = Shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = "Нет",
                                style = MaterialTheme.typography.bodySmall,
                                color = primaryColor
                            )
                        }

                        Button(
                            onClick = { showForm = true },
                            modifier = Modifier.weight(1f),
                            shape = Shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            elevation = ButtonDefaults.elevatedButtonElevation(
                                defaultElevation = 3.dp,
                                pressedElevation = 0.dp
                            )
                        ) {
                            Text(
                                text = "Да",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showForm,
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // -- Станция отправления --
                    Text(
                        text = "Станция отправления",
                        style = hintStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = hintColor,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = isDropdownDepartureExpanded,
                        onExpandedChange = { isDropdownDepartureExpanded = it }
                    ) {
                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                            value = stationDeparture,
                            onValueChange = { value ->
                                stationDeparture = value
                                isDropdownDepartureExpanded = value.text.isNotEmpty()
                            },
                            placeholder = {
                                Text(
                                    text = "Станция",
                                    style = hintStyle,
                                    color = hintColor
                                )
                            },
                            textStyle = dataTextStyle.copy(fontWeight = FontWeight.Medium),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = {
                                focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                                isDropdownDepartureExpanded = false
                            }),
                            colorBackgroundEmptyField = fieldColor,
                            colorBackgroundNotEmptyField = fieldColor
                        )
                        val filtered = stationList.filter {
                            it.contains(stationDeparture.text, ignoreCase = true)
                        }.take(5)
                        if (filtered.isNotEmpty() && isDropdownDepartureExpanded) {
                            DropdownMenu(
                                modifier = Modifier
                                    .background(
                                        color = fieldColor,
                                        shape = Shapes.medium
                                    )
                                    .exposedDropdownSize(true),
                                expanded = true,
                                onDismissRequest = { isDropdownDepartureExpanded = false },
                                properties = PopupProperties(focusable = false)
                            ) {
                                filtered.forEach { name ->
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = fieldColor,
                                                shape = Shapes.medium
                                            ),
                                        text = {
                                            Text(
                                                text = name,
                                                style = dataTextStyle,
                                                color = primaryColor
                                            )
                                        },
                                        onClick = {
                                            stationDeparture = TextFieldValue(
                                                text = name,
                                                selection = TextRange(name.length)
                                            )
                                            isDropdownDepartureExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // -- Станция прибытия --
                    Text(
                        text = "Станция прибытия",
                        style = hintStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = hintColor,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = isDropdownArrivalExpanded,
                        onExpandedChange = { isDropdownArrivalExpanded = it }
                    ) {
                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                            value = stationArrival,
                            onValueChange = { value ->
                                stationArrival = value
                                isDropdownArrivalExpanded = value.text.isNotEmpty()
                            },
                            placeholder = {
                                Text(
                                    text = "Станция",
                                    style = hintStyle,
                                    color = hintColor
                                )
                            },
                            textStyle = dataTextStyle.copy(fontWeight = FontWeight.Medium),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                isDropdownArrivalExpanded = false
                            }),
                            colorBackgroundEmptyField = fieldColor,
                            colorBackgroundNotEmptyField = fieldColor
                        )
                        val filtered = stationList.filter {
                            it.contains(stationArrival.text, ignoreCase = true)
                        }.take(5)
                        if (filtered.isNotEmpty() && isDropdownArrivalExpanded) {
                            DropdownMenu(
                                modifier = Modifier
                                    .background(
                                        color = fieldColor,
                                        shape = Shapes.medium
                                    )
                                    .exposedDropdownSize(true),
                                expanded = true,
                                onDismissRequest = { isDropdownArrivalExpanded = false },
                                properties = PopupProperties(focusable = false)
                            ) {
                                filtered.forEach { name ->
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = fieldColor,
                                                shape = Shapes.medium
                                            ),
                                        text = {
                                            Text(
                                                text = name,
                                                style = dataTextStyle,
                                                color = primaryColor
                                            )
                                        },
                                        onClick = {
                                            stationArrival = TextFieldValue(
                                                text = name,
                                                selection = TextRange(name.length)
                                            )
                                            isDropdownArrivalExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // -- Время отправления --
                    Text(
                        text = "Время отправления",
                        style = hintStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = hintColor,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = fieldColor,
                                shape = fieldShape
                            )
                            .clickable { showDeparturePicker = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = dateAndTimeConverter?.getDateAndTime(timeDeparture) ?: timeDeparture.toString(),
                            style = dataTextStyle.copy(fontWeight = FontWeight.Medium),
                            color = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // -- Время прибытия --
                    Text(
                        text = "Время прибытия",
                        style = hintStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = hintColor,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = fieldColor,
                                shape = fieldShape
                            )
                            .clickable { showArrivalPicker = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = dateAndTimeConverter?.getDateAndTime(timeArrival) ?: timeArrival.toString(),
                            style = dataTextStyle.copy(fontWeight = FontWeight.Medium),
                            color = primaryColor
                        )
                    }

                    // -- Ошибка валидации --
                    timeError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // -- Кнопка сохранить --
                    Button(
                        onClick = {
                            onSave(
                                stationDeparture.text.ifBlank { null },
                                stationArrival.text.ifBlank { null },
                                timeDeparture,
                                timeArrival
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = timeError == null,
                        shape = Shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "Сохранить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }

    // -- Пикеры времени --
    if (showDeparturePicker) {
        DateTimePickerBottomSheet(
            title = "Отправление пассажиром",
            onDateTimeSelected = { timestamp ->
                timeDeparture = timestamp - timestamp % 60_000L
            },
            onDismiss = { showDeparturePicker = false },
            startDateTime = timeDeparture
        )
    }

    if (showArrivalPicker) {
        DateTimePickerBottomSheet(
            title = "Прибытие пассажиром",
            onDateTimeSelected = { timestamp ->
                timeArrival = timestamp - timestamp % 60_000L
            },
            onDismiss = { showArrivalPicker = false },
            startDateTime = timeArrival
        )
    }
}
