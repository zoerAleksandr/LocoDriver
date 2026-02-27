package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.z_company.core.ui.component.DateTimePickerBottomSheet
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.route.viewmodel.StationFormState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val hintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val dataTextStyle = MaterialTheme.typography.bodyLarge
    val hintStyle = MaterialTheme.typography.bodyMedium
    val fieldColor = MaterialTheme.colorScheme.surface
    val fieldShape = RoundedCornerShape(14.dp)

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
            // ── Заголовок ──
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ── Название станции ──
            Text(
                text = "Название",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 5.dp)
            )

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
                            color = hintColor
                        )
                    },
                    textStyle = dataTextStyle.copy(fontWeight = FontWeight.Medium),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            isDropdownExpanded = false
                        }
                    ),
                    singleLine = true,
                    colorBackgroundEmptyField = MaterialTheme.colorScheme.surface,
                    colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surface
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

            Spacer(modifier = Modifier.height(36.dp))

            // ── Прибытие ──
            TimeBlock(
                label = "Прибытие",
                timeMillis = localArrival,
                dateAndTimeConverter = dateAndTimeConverter,
                onFieldClick = { showArrivalPicker = true },
                onClear = { localArrival = null },
                onAdjust = { delta ->
                    val base = localArrival ?: nowTruncatedToMinutes()
                    localArrival = base + delta * 60_000L
                },
                onNow = { localArrival = nowTruncatedToMinutes() },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Бейдж стоянки (между прибытием и отправлением) ──
            val stopMinutes = if (localArrival != null && localDeparture != null) {
                val arr = localArrival!! - localArrival!! % 60_000L
                val dep = localDeparture!! - localDeparture!! % 60_000L
                val diff = dep - arr
                if (diff > 0) (diff / 60_000).toInt() else null
            } else null

            if (stopMinutes != null && stopMinutes > 0) {
                StopDurationDivider(stopMinutes = stopMinutes)
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Отправление ──
            TimeBlock(
                label = "Отправление",
                timeMillis = localDeparture,
                dateAndTimeConverter = dateAndTimeConverter,
                onFieldClick = { showDeparturePicker = true },
                onClear = { localDeparture = null },
                onAdjust = { delta ->
                    val base = localDeparture ?: nowTruncatedToMinutes()
                    localDeparture = base + delta * 60_000L
                },
                onNow = { localDeparture = nowTruncatedToMinutes() },
            )

            // ── Разделитель ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
                    .height(1.dp)
                    .background(primaryColor.copy(alpha = 0.1f))
            )

            // ── Кнопка «Сохранить» ──
            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                onClick = {
                    val name = localName.text.ifBlank { null }
                    onSave(name, localArrival, localDeparture)
                }
            ) {
                Text(
                    text = "Сохранить",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // ── Кнопка «Удалить станцию» ──
            if (onDelete != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    onClick = onDelete
                ) {
                    Text(
                        text = "Удалить станцию",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 2.dp)
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

// ─── Блок времени (лейбл + крупное время + кнопки ±) ─────────────────────────

@Composable
private fun TimeBlock(
    label: String,
    timeMillis: Long?,
    dateAndTimeConverter: DateAndTimeConverter?,
    onFieldClick: () -> Unit,
    onClear: () -> Unit,
    onAdjust: (Int) -> Unit,
    onNow: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val fieldColor = MaterialTheme.colorScheme.surface
    val fieldShape = RoundedCornerShape(14.dp)
    val clearColor = Color(0xFFC4392D)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Лейбл + дата
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor.copy(alpha = 0.5f)
            )
            if (timeMillis != null) {
                val dateText = formatDateShort(timeMillis)

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryColor.copy(alpha = 0.4f)
                )
            }
        }

        // Поле с крупным временем
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = fieldColor, shape = fieldShape)
                .clickable { onFieldClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,

        ) {
            // Крупное время по центру
            val timeText = if (timeMillis != null) {
                dateAndTimeConverter?.getTimeFromDateLong(timeMillis)
                    ?: DateAndTimeFormat.DEFAULT_TIME_TEXT
            } else {
                DateAndTimeFormat.DEFAULT_TIME_TEXT
            }

            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
                color = if (timeMillis != null) primaryColor else primaryColor.copy(alpha = 0.25f),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            // Кнопка очистки (Box всегда 32dp для стабильного layout)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .then(
                        if (timeMillis != null) Modifier
                            .border(
                                width = 1.dp,
                                color = clearColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onClear() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (timeMillis != null) {
                    Icon(
                        painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                        contentDescription = "Очистить",
                        tint = clearColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 5 кнопок: -5, -1, Сейчас, +1, +5
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // -5
            StepButton(
                label = "-5",
                modifier = Modifier.weight(1f),
                onClick = { onAdjust(-5) }
            )
            // -1
            StepButton(
                label = "-1",
                modifier = Modifier.weight(1f),
                onClick = { onAdjust(-1) }
            )
            // Сейчас
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .background(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onNow() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Сейчас",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32)
                )
            }
            // +1
            StepButton(
                label = "+1",
                modifier = Modifier.weight(1f),
                onClick = { onAdjust(1) }
            )
            // +5
            StepButton(
                label = "+5",
                modifier = Modifier.weight(1f),
                onClick = { onAdjust(5) }
            )
        }
    }
}

// ─── Кнопка ±N ───────────────────────────────────────────────────────────────

@Composable
private fun StepButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fieldColor = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .background(
                color = fieldColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
    }
}

// ─── Бейдж стоянки между прибытием и отправлением ────────────────────────────

@Composable
private fun StopDurationDivider(stopMinutes: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(primaryColor.copy(alpha = 0.1f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .background(
                    color = Color(0xFFFFC107),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Стоянка $stopMinutes мин",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E342E)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(primaryColor.copy(alpha = 0.1f))
        )
    }
}

// ─── Утилиты ────────────────────────────────────────────────────────────────

private fun formatDateShort(millis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun nowTruncatedToMinutes(): Long {
    val now = System.currentTimeMillis()
    return now - (now % 60_000L)
}
