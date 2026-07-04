package com.z_company.route.component

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.util.toIntOrZero
import kotlinx.coroutines.launch

/**
 * Вторая шторка (поверх «Плечи») — создание и редактирование плеча.
 * Бизнес-логика (сохранение в UserSettings) — снаружи через колбэки, по образцу
 * SettingsShouldersContent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoulderEditBottomSheet(
    phase: ServicePhase?,
    stationList: List<String>,
    onSave: (phase: ServicePhase, createReverse: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDeleteStationName: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val isNew = phase == null
    val primaryColor = MaterialTheme.colorScheme.primary
    val hintColor = primaryColor.copy(alpha = 0.5f)
    val dataStyle = MaterialTheme.typography.bodyLarge
    val fieldColor = MaterialTheme.colorScheme.surfaceBright
    val fieldShape = RoundedCornerShape(14.dp)

    var departure by remember {
        mutableStateOf(
            TextFieldValue(
                phase?.departureStation ?: "",
                TextRange(phase?.departureStation?.length ?: 0)
            )
        )
    }
    var arrival by remember {
        mutableStateOf(
            TextFieldValue(
                phase?.arrivalStation ?: "",
                TextRange(phase?.arrivalStation?.length ?: 0)
            )
        )
    }
    var distance by remember { mutableStateOf(phase?.distance?.takeIf { it > 0 }?.toString() ?: "") }
    var createReverse by remember { mutableStateOf(false) }

    var depFiltered by remember { mutableStateOf(stationList) }
    var arrFiltered by remember { mutableStateOf(stationList) }
    var depExpanded by remember { mutableStateOf(false) }
    var arrExpanded by remember { mutableStateOf(false) }

    val isValid = departure.text.isNotBlank() && arrival.text.isNotBlank() && distance.toIntOrZero() > 0

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
            // ── Заголовок + закрытие ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNew) "Новое плечо" else "Редактировать плечо",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .clickable { onDismiss() },
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

            // ── От станции ──
            ExposedDropdownMenuBox(
                expanded = depExpanded,
                onExpandedChange = { depExpanded = it }
            ) {
                OutlinedTextFieldApp(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    value = departure,
                    onValueChange = { v ->
                        departure = v
                        depFiltered = stationList.filter { it.contains(v.text, ignoreCase = true) }
                        depExpanded = v.text.isNotEmpty()
                    },
                    placeholder = { Text("От станции", style = dataStyle, color = hintColor) },
                    textStyle = dataStyle,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text, imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        depExpanded = false
                        scope.launch { focusManager.moveFocus(FocusDirection.Down) }
                    }),
                    singleLine = true,
                    shape = fieldShape,
                    fieldElevation = 0.dp,
                    colorBackgroundEmptyField = fieldColor,
                    colorBackgroundNotEmptyField = fieldColor
                )
                StationDropdownMenu(
                    expanded = depExpanded,
                    stations = depFiltered,
                    onSelect = {
                        departure = TextFieldValue(it, TextRange(it.length))
                        depExpanded = false
                    },
                    onDelete = onDeleteStationName,
                    onDismiss = { depExpanded = false },
                    textStyle = dataStyle
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── До станции ──
            ExposedDropdownMenuBox(
                expanded = arrExpanded,
                onExpandedChange = { arrExpanded = it }
            ) {
                OutlinedTextFieldApp(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    value = arrival,
                    onValueChange = { v ->
                        arrival = v
                        arrFiltered = stationList.filter { it.contains(v.text, ignoreCase = true) }
                        arrExpanded = v.text.isNotEmpty()
                    },
                    placeholder = { Text("До станции", style = dataStyle, color = hintColor) },
                    textStyle = dataStyle,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text, imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        arrExpanded = false
                        scope.launch { focusManager.moveFocus(FocusDirection.Down) }
                    }),
                    singleLine = true,
                    shape = fieldShape,
                    fieldElevation = 0.dp,
                    colorBackgroundEmptyField = fieldColor,
                    colorBackgroundNotEmptyField = fieldColor
                )
                StationDropdownMenu(
                    expanded = arrExpanded,
                    stations = arrFiltered,
                    onSelect = {
                        arrival = TextFieldValue(it, TextRange(it.length))
                        arrExpanded = false
                    },
                    onDelete = onDeleteStationName,
                    onDismiss = { arrExpanded = false },
                    textStyle = dataStyle
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Расстояние ──
            OutlinedTextFieldApp(
                modifier = Modifier.fillMaxWidth(),
                value = distance,
                onValueChange = { distance = it },
                placeholder = { Text("Расстояние", style = dataStyle, color = hintColor) },
                suffix = {
                    Text(
                        "км",
                        style = MaterialTheme.typography.bodyMedium,
                        color = hintColor
                    )
                },
                textStyle = dataStyle.copy(fontFamily = com.z_company.core.ui.theme.MonoFont),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    scope.launch { focusManager.clearFocus() }
                }),
                singleLine = true,
                shape = fieldShape,
                fieldElevation = 0.dp,
                colorBackgroundEmptyField = fieldColor,
                colorBackgroundNotEmptyField = fieldColor
            )

            // ── Обратное направление (только для нового) ──
            if (isNew) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Создать плечо в обратном направлении",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Switch(
                        checked = createReverse,
                        onCheckedChange = { createReverse = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            }

            // ── Разделитель ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
                    .height(1.dp)
                    .background(primaryColor.copy(alpha = 0.1f))
            )

            // ── Сохранить / Добавить ──
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = Shapes.medium,
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
                    disabledContentColor = hintColor
                ),
                onClick = {
                    val dist = distance.toIntOrZero()
                    if (dist > 0 && departure.text.isNotBlank() && arrival.text.isNotBlank()) {
                        val dep = departure.text.trim()
                        val arr = arrival.text.trim()
                        // Для редактирования сохраняем id через copy, для нового — новый id по умолчанию.
                        val result = phase?.copy(
                            departureStation = dep,
                            arrivalStation = arr,
                            distance = dist
                        ) ?: ServicePhase(
                            departureStation = dep,
                            arrivalStation = arr,
                            distance = dist
                        )
                        onSave(result, createReverse && isNew)
                        onDismiss()
                    }
                }
            ) {
                Text(
                    text = if (isNew) "Добавить" else "Сохранить",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Удалить плечо (только редактирование) ──
            if (onDelete != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = Shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    onClick = {
                        onDelete()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Удалить плечо",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
