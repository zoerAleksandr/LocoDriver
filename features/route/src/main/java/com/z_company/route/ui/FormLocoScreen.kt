package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.GenericError
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.core.util.LocoTypeHelper.converterLocoTypeToString
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.str
import com.z_company.core.R as CoreR
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.DatePickerDialog
import com.z_company.route.component.DieselSectionItem
import com.z_company.route.component.TimePickerDialog
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.LocoFormUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.z_company.domain.util.*
import com.z_company.route.component.ElectricSectionItem
import com.z_company.route.component.rememberDatePickerStateInLocale
import com.z_company.route.viewmodel.DieselSectionFormState
import com.z_company.route.viewmodel.DieselSectionType
import com.z_company.route.viewmodel.ElectricSectionFormState
import com.z_company.route.viewmodel.ElectricSectionType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormLocoScreen(
    currentLoco: Locomotive?,
    currentSetting: UserSettings?,
    dieselSectionListState: SnapshotStateList<DieselSectionFormState>?,
    electricSectionListState: SnapshotStateList<ElectricSectionFormState>?,
    onBackPressed: () -> Unit,
    onSaveClick: () -> Unit,
    onLocoSaved: () -> Unit,
    onClearAllField: () -> Unit,
    formUiState: LocoFormUiState,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
    onSeriesChanged: (String) -> Unit,
    onChangedTypeLoco: (Int) -> Unit,
    onStartAcceptedTimeChanged: (Long?) -> Unit,
    onEndAcceptedTimeChanged: (Long?) -> Unit,
    onStartDeliveryTimeChanged: (Long?) -> Unit,
    onEndDeliveryTimeChanged: (Long?) -> Unit,
    onFuelAcceptedChanged: (Int, String?) -> Unit,
    onFuelDeliveredChanged: (Int, String?) -> Unit,
    onDeleteSectionDiesel: (DieselSectionFormState) -> Unit,
    addingSectionDiesel: () -> Unit,
    focusChangedDieselSection: (Int, DieselSectionType) -> Unit,
    onEnergyAcceptedChanged: (Int, Int?) -> Unit,
    onEnergyDeliveryChanged: (Int, Int?) -> Unit,
    onRecoveryAcceptedChanged: (Int, Int?) -> Unit,
    onRecoveryDeliveryChanged: (Int, Int?) -> Unit,
    onDeleteSectionElectric: (ElectricSectionFormState) -> Unit,
    addingSectionElectric: () -> Unit,
    focusChangedElectricSection: (Int, ElectricSectionType) -> Unit,
    onExpandStateElectricSection: (Int, Boolean) -> Unit,
    showRefuelDialog: (Pair<Boolean, Int>) -> Unit,
    onRefuelValueChanged: (Int, String?) -> Unit,
    showCoefficientDialog: (Pair<Boolean, Int>) -> Unit,
    onCoefficientValueChanged: (Int, String?) -> Unit,
    isShowRefuelDialog: Pair<Boolean, Int>,
    isShowCoefficientDialog: Pair<Boolean, Int>,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxWidth(),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Локомотив",
                        style = AppTypography.getType().headlineSmall
                            .copy(color = MaterialTheme.colorScheme.primary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    ClickableText(
                        text = AnnotatedString(text = "Сохранить"),
                        style = AppTypography.getType().titleMedium,
                        onClick = { onSaveClick() }
                    )
                    var dropDownExpanded by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = {
                            dropDownExpanded = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню"
                        )
                        DropdownMenu(
                            expanded = dropDownExpanded,
                            onDismissRequest = { dropDownExpanded = false },
                            offset = DpOffset(x = 4.dp, y = 8.dp)
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = {
                                    onClearAllField()
                                    dropDownExpanded = false
                                },
                                text = {
                                    Text(
                                        text = "Очистить",
                                        style = AppTypography.getType().bodyLarge
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AsyncData(resultState = formUiState.locoDetailState) {
                currentLoco?.let { locomotive ->
                    AsyncData(resultState = formUiState.saveLocoState, errorContent = {
                        GenericError(
                            onDismissAction = resetSaveState
                        )
                    }) {
                        if (formUiState.saveLocoState is ResultState.Success) {
                            LaunchedEffect(formUiState.saveLocoState) {
                                onLocoSaved()
                            }
                        } else {
                            LocoFormScreenContent(
                                locomotive = locomotive,
                                setting = currentSetting,
                                dieselSectionListState = dieselSectionListState,
                                electricSectionListState = electricSectionListState,
                                onNumberChanged = onNumberChanged,
                                onSeriesChanged = onSeriesChanged,
                                onTypeLocoChanged = onChangedTypeLoco,
                                onStartAcceptedTimeChanged = onStartAcceptedTimeChanged,
                                onEndAcceptedTimeChanged = onEndAcceptedTimeChanged,
                                onStartDeliveryTimeChanged = onStartDeliveryTimeChanged,
                                onEndDeliveryTimeChanged = onEndDeliveryTimeChanged,
                                onFuelAcceptedChanged = onFuelAcceptedChanged,
                                onFuelDeliveredChanged = onFuelDeliveredChanged,
                                onDeleteSectionDiesel = onDeleteSectionDiesel,
                                addingSectionDiesel = addingSectionDiesel,
                                focusChangedDieselSection = focusChangedDieselSection,
                                onEnergyAcceptedChanged = onEnergyAcceptedChanged,
                                onEnergyDeliveryChanged = onEnergyDeliveryChanged,
                                onRecoveryAcceptedChanged = onRecoveryAcceptedChanged,
                                onRecoveryDeliveryChanged = onRecoveryDeliveryChanged,
                                onDeleteSectionElectric = onDeleteSectionElectric,
                                addingSectionElectric = addingSectionElectric,
                                focusChangedElectricSection = focusChangedElectricSection,
                                onExpandStateElectricSection = onExpandStateElectricSection,
                                isShowRefuelDialog = isShowRefuelDialog,
                                showRefuelDialog = showRefuelDialog,
                                onRefuelValueChanged = onRefuelValueChanged,
                                isShowCoefficientDialog = isShowCoefficientDialog,
                                showCoefficientDialog = showCoefficientDialog,
                                onCoefficientValueChanged = onCoefficientValueChanged
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocoFormScreenContent(
    locomotive: Locomotive,
    setting: UserSettings?,
    dieselSectionListState: SnapshotStateList<DieselSectionFormState>?,
    electricSectionListState: SnapshotStateList<ElectricSectionFormState>?,
    onNumberChanged: (String) -> Unit,
    onSeriesChanged: (String) -> Unit,
    onTypeLocoChanged: (Int) -> Unit,
    onStartAcceptedTimeChanged: (Long?) -> Unit,
    onEndAcceptedTimeChanged: (Long?) -> Unit,
    onStartDeliveryTimeChanged: (Long?) -> Unit,
    onEndDeliveryTimeChanged: (Long?) -> Unit,
    onFuelAcceptedChanged: (Int, String?) -> Unit,
    onFuelDeliveredChanged: (Int, String?) -> Unit,
    onDeleteSectionDiesel: (DieselSectionFormState) -> Unit,
    addingSectionDiesel: () -> Unit,
    focusChangedDieselSection: (Int, DieselSectionType) -> Unit,
    onEnergyAcceptedChanged: (Int, Int?) -> Unit,
    onEnergyDeliveryChanged: (Int, Int?) -> Unit,
    onRecoveryAcceptedChanged: (Int, Int?) -> Unit,
    onRecoveryDeliveryChanged: (Int, Int?) -> Unit,
    onDeleteSectionElectric: (ElectricSectionFormState) -> Unit,
    addingSectionElectric: () -> Unit,
    focusChangedElectricSection: (Int, ElectricSectionType) -> Unit,
    onExpandStateElectricSection: (Int, Boolean) -> Unit,
    isShowRefuelDialog: Pair<Boolean, Int>,
    showRefuelDialog: (Pair<Boolean, Int>) -> Unit,
    onRefuelValueChanged: (Int, String?) -> Unit,
    isShowCoefficientDialog: Pair<Boolean, Int>,
    showCoefficientDialog: (Pair<Boolean, Int>) -> Unit,
    onCoefficientValueChanged: (Int, String?) -> Unit
) {
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        modifier = Modifier
            .zIndex(1f),
        visible = !scrollState.isScrollInInitialState(),
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        BottomShadow()
    }
    LazyColumn(
        state = scrollState,
        horizontalAlignment = Alignment.End,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .weight(1f),
                    value = locomotive.series ?: "",
                    textStyle = AppTypography.getType().bodyLarge,
                    placeholder = {
                        Text(text = "Серия")
                    },
                    onValueChange = {
                        onSeriesChanged(it)
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Right)
                        }
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                    value = locomotive.number ?: "",
                    textStyle = AppTypography.getType().bodyLarge,
                    placeholder = {
                        Text(
                            text = "Номер"
                        )
                    },
                    onValueChange = { onNumberChanged(it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true
                )
            }
        }
        item {
            val types = LocoType.values().map {
                converterLocoTypeToString(it)
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = index == locomotive.type.ordinal,
                        onClick = { onTypeLocoChanged(index) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = types.size,
                            baseShape = Shapes.small
                        )
                    ) {
                        Text(text = type)
                    }
                }
            }
        }
        item {
            val startAcceptedTime = locomotive.timeStartOfAcceptance
            val endAcceptedTime = locomotive.timeEndOfAcceptance

            val startAcceptedCalendar by remember {
                mutableStateOf(Calendar.getInstance().also { calendar ->
                    startAcceptedTime?.let { millis ->
                        calendar.timeInMillis = millis
                    }
                })
            }

            val startAcceptedTimePickerState = rememberTimePickerState(
                initialHour = startAcceptedCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = startAcceptedCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )

            val startAcceptedDatePickerState =
                rememberDatePickerStateInLocale(
                    initialSelectedDateMillis = startAcceptedTime
                        ?: startAcceptedCalendar.timeInMillis
                )

            var showStartAcceptedTimePicker by remember {
                mutableStateOf(false)
            }

            var showStartAcceptedDatePicker by remember {
                mutableStateOf(false)
            }

            if (showStartAcceptedDatePicker) {
                DatePickerDialog(
                    datePickerState = startAcceptedDatePickerState,
                    onDismissRequest = {
                        showStartAcceptedDatePicker = false
                    },
                    onConfirmRequest = {
                        showStartAcceptedDatePicker = false
                        showStartAcceptedTimePicker = true
                        startAcceptedCalendar.timeInMillis =
                            startAcceptedDatePickerState.selectedDateMillis!!
                    },
                    onClearRequest = {
                        showStartAcceptedDatePicker = false
                        onStartAcceptedTimeChanged(null)
                    }
                )
            }

            if (showStartAcceptedTimePicker) {
                TimePickerDialog(
                    timePickerState = startAcceptedTimePickerState,
                    onDismissRequest = { showStartAcceptedTimePicker = false },
                    onConfirmRequest = {
                        showStartAcceptedTimePicker = false
                        startAcceptedCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            startAcceptedTimePickerState.hour
                        )
                        startAcceptedCalendar.set(
                            Calendar.MINUTE,
                            startAcceptedTimePickerState.minute
                        )
                        onStartAcceptedTimeChanged(startAcceptedCalendar.timeInMillis)
                    }
                )
            }

            val endAcceptedCalendar by remember {
                mutableStateOf(Calendar.getInstance().also { calendar ->
                    endAcceptedTime?.let { millis ->
                        calendar.timeInMillis = millis
                    }
                })
            }

            val endAcceptedTimePickerState = rememberTimePickerState(
                initialHour = endAcceptedCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = endAcceptedCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )

            val endAcceptedDatePickerState =
                rememberDatePickerStateInLocale(
                    initialSelectedDateMillis = endAcceptedTime ?: endAcceptedCalendar.timeInMillis
                )

            var showEndAcceptedTimePicker by remember {
                mutableStateOf(false)
            }

            var showEndAcceptedDatePicker by remember {
                mutableStateOf(false)
            }

            if (showEndAcceptedDatePicker) {
                DatePickerDialog(
                    datePickerState = endAcceptedDatePickerState,
                    onDismissRequest = {
                        showEndAcceptedDatePicker = false
                    },
                    onConfirmRequest = {
                        showEndAcceptedDatePicker = false
                        showEndAcceptedTimePicker = true
                        endAcceptedCalendar.timeInMillis =
                            endAcceptedDatePickerState.selectedDateMillis!!
                    },
                    onClearRequest = {
                        showEndAcceptedDatePicker = false
                        onEndAcceptedTimeChanged(null)
                    }
                )
            }

            if (showEndAcceptedTimePicker) {
                TimePickerDialog(
                    timePickerState = endAcceptedTimePickerState,
                    onDismissRequest = { showEndAcceptedTimePicker = false },
                    onConfirmRequest = {
                        showEndAcceptedTimePicker = false
                        endAcceptedCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            endAcceptedTimePickerState.hour
                        )
                        endAcceptedCalendar.set(
                            Calendar.MINUTE,
                            endAcceptedTimePickerState.minute
                        )
                        onEndAcceptedTimeChanged(endAcceptedCalendar.timeInMillis)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .border(
                        width = 1.dp,
                        shape = Shapes.small,
                        color = MaterialTheme.colorScheme.outline
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = "Приемка",
                        style = AppTypography.getType().bodyLarge
                    )

                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable {
                                    showStartAcceptedDatePicker = true
                                }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val timeStartText = startAcceptedTime?.let { millis ->
                                SimpleDateFormat(
                                    DateAndTimeFormat.TIME_FORMAT,
                                    Locale.getDefault()
                                ).format(
                                    millis
                                )
                            } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                            Text(
                                text = timeStartText,
                                style = AppTypography.getType().bodyLarge,
                            )
                        }
                        Text(" - ")
                        Box(
                            modifier = Modifier
                                .padding(18.dp)
                                .clickable {
                                    showEndAcceptedDatePicker = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val timeStartText = endAcceptedTime?.let { millis ->
                                SimpleDateFormat(
                                    DateAndTimeFormat.TIME_FORMAT,
                                    Locale.getDefault()
                                ).format(
                                    millis
                                )
                            } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                            Text(
                                text = timeStartText,
                                style = AppTypography.getType().bodyLarge,
                            )
                        }
                    }
                }
            }
        }
        item {
            val startDeliveryTime = locomotive.timeStartOfDelivery
            val endDeliveryTime = locomotive.timeEndOfDelivery
            val startDeliveryCalendar by remember {
                mutableStateOf(Calendar.getInstance().also { calendar ->
                    startDeliveryTime?.let { millis ->
                        calendar.timeInMillis = millis
                    }
                })
            }

            val startDeliveryTimePickerState = rememberTimePickerState(
                initialHour = startDeliveryCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = startDeliveryCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )

            val startDeliveryDatePickerState =
                rememberDatePickerStateInLocale(
                    initialSelectedDateMillis = startDeliveryTime
                        ?: startDeliveryCalendar.timeInMillis
                )

            var showStartDeliveryTimePicker by remember {
                mutableStateOf(false)
            }

            var showStartDeliveryDatePicker by remember {
                mutableStateOf(false)
            }

            if (showStartDeliveryDatePicker) {
                DatePickerDialog(
                    datePickerState = startDeliveryDatePickerState,
                    onDismissRequest = {
                        showStartDeliveryDatePicker = false
                    },
                    onConfirmRequest = {
                        showStartDeliveryDatePicker = false
                        showStartDeliveryTimePicker = true
                        startDeliveryCalendar.timeInMillis =
                            startDeliveryDatePickerState.selectedDateMillis!!
                    },
                    onClearRequest = {
                        showStartDeliveryDatePicker = false
                        onStartDeliveryTimeChanged(null)
                    }
                )
            }

            if (showStartDeliveryTimePicker) {
                TimePickerDialog(
                    timePickerState = startDeliveryTimePickerState,
                    onDismissRequest = { showStartDeliveryTimePicker = false },
                    onConfirmRequest = {
                        showStartDeliveryTimePicker = false
                        startDeliveryCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            startDeliveryTimePickerState.hour
                        )
                        startDeliveryCalendar.set(
                            Calendar.MINUTE,
                            startDeliveryTimePickerState.minute
                        )
                        onStartDeliveryTimeChanged(startDeliveryCalendar.timeInMillis)
                    }
                )
            }

            val endDeliveryCalendar by remember {
                mutableStateOf(Calendar.getInstance().also { calendar ->
                    endDeliveryTime?.let { millis ->
                        calendar.timeInMillis = millis
                    }
                })
            }

            val endDeliveryTimePickerState = rememberTimePickerState(
                initialHour = endDeliveryCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = endDeliveryCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )

            val endDeliveryDatePickerState =
                rememberDatePickerStateInLocale(
                    initialSelectedDateMillis = endDeliveryTime ?: endDeliveryCalendar.timeInMillis
                )

            var showEndDeliveryTimePicker by remember {
                mutableStateOf(false)
            }

            var showEndDeliveryDatePicker by remember {
                mutableStateOf(false)
            }

            if (showEndDeliveryDatePicker) {
                DatePickerDialog(
                    datePickerState = endDeliveryDatePickerState,
                    onDismissRequest = {
                        showEndDeliveryDatePicker = false
                    },
                    onConfirmRequest = {
                        showEndDeliveryDatePicker = false
                        showEndDeliveryTimePicker = true
                        endDeliveryCalendar.timeInMillis =
                            endDeliveryDatePickerState.selectedDateMillis!!
                    },
                    onClearRequest = {
                        showEndDeliveryDatePicker = false
                        onEndDeliveryTimeChanged(null)
                    }
                )
            }

            if (showEndDeliveryTimePicker) {
                TimePickerDialog(
                    timePickerState = endDeliveryTimePickerState,
                    onDismissRequest = { showEndDeliveryTimePicker = false },
                    onConfirmRequest = {
                        showEndDeliveryTimePicker = false
                        endDeliveryCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            endDeliveryTimePickerState.hour
                        )
                        endDeliveryCalendar.set(
                            Calendar.MINUTE,
                            endDeliveryTimePickerState.minute
                        )
                        onEndDeliveryTimeChanged(endDeliveryCalendar.timeInMillis)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .border(
                        width = 1.dp,
                        shape = Shapes.small,
                        color = MaterialTheme.colorScheme.outline
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = "Сдача",
                        style = AppTypography.getType().bodyLarge
                    )

                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable {
                                    showStartDeliveryDatePicker = true
                                }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val timeStartText = startDeliveryTime?.let { millis ->
                                SimpleDateFormat(
                                    DateAndTimeFormat.TIME_FORMAT,
                                    Locale.getDefault()
                                ).format(
                                    millis
                                )
                            } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                            Text(
                                text = timeStartText,
                                style = AppTypography.getType().bodyLarge,
                            )
                        }
                        Text(" - ")
                        Box(
                            modifier = Modifier
                                .padding(18.dp)
                                .clickable {
                                    showEndDeliveryDatePicker = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val timeEndText = endDeliveryTime?.let { millis ->
                                SimpleDateFormat(
                                    DateAndTimeFormat.TIME_FORMAT,
                                    Locale.getDefault()
                                ).format(
                                    millis
                                )
                            } ?: DateAndTimeFormat.DEFAULT_TIME_TEXT

                            Text(
                                text = timeEndText,
                                style = AppTypography.getType().bodyLarge,
                            )
                        }
                    }
                }
            }
        }
        when (locomotive.type.name) {
            LocoType.DIESEL.name -> {
                dieselSectionListState?.let {
                    itemsIndexed(
                        items = dieselSectionListState,
                        key = { _, item -> item.sectionId }
                    ) { index, item ->
                        if (index == 0) {
                            Spacer(modifier = Modifier.height(dimensionResource(id = CoreR.dimen.secondary_spacing)))
                        } else {
                            Spacer(modifier = Modifier.height(dimensionResource(id = CoreR.dimen.secondary_spacing) / 2))
                        }
                        DieselSectionItem(
                            item = item,
                            index = index,
                            isShowRefuelDialog = isShowRefuelDialog,
                            isShowCoefficientDialog = isShowCoefficientDialog,
                            onFuelAcceptedChanged = onFuelAcceptedChanged,
                            onFuelDeliveredChanged = onFuelDeliveredChanged,
                            onDeleteItem = onDeleteSectionDiesel,
                            focusChangedDieselSection = focusChangedDieselSection,
                            showRefuelDialog = showRefuelDialog,
                            onRefuelValueChanged = onRefuelValueChanged,
                            showCoefficientDialog = showCoefficientDialog,
                            onCoefficientValueChanged = onCoefficientValueChanged
                        )

                        if (index == dieselSectionListState.lastIndex && index > 0) {
                            var overResult: Double? = null
                            dieselSectionListState.forEach {
                                val accepted = it.accepted.data?.toDoubleOrNull()
                                val delivery = it.delivery.data?.toDoubleOrNull()
                                val refuel = it.refuel.data?.toDoubleOrNull()
                                val result = CalculationEnergy.getTotalFuelConsumption(
                                    accepted, delivery, refuel
                                )
                                overResult += result
                            }
                            overResult?.let {
                                Text(
                                    text = "Всего расход = ${maskInLiter(it.str())}",
                                    style = AppTypography.getType().bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            LocoType.ELECTRIC.name -> {
                electricSectionListState?.let {
                    itemsIndexed(
                        items = electricSectionListState,
                        key = { _, item -> item.sectionId }
                    ) { index, item ->
                        if (index == 0) {
                            Spacer(modifier = Modifier.height(dimensionResource(id = CoreR.dimen.secondary_spacing)))
                        } else {
                            Spacer(modifier = Modifier.height(dimensionResource(id = CoreR.dimen.secondary_spacing) / 2))
                        }
                        ElectricSectionItem(
                            index = index,
                            item = item,
                            onDeleteItem = onDeleteSectionElectric,
                            onEnergyAcceptedChanged = onEnergyAcceptedChanged,
                            onEnergyDeliveryChanged = onEnergyDeliveryChanged,
                            onRecoveryAcceptedChanged = onRecoveryAcceptedChanged,
                            onRecoveryDeliveryChanged = onRecoveryDeliveryChanged,
                            focusChangedElectricSection = focusChangedElectricSection,
                            onExpandStateChanged = onExpandStateElectricSection
                        )

                        if (index == electricSectionListState.lastIndex && index > 0) {
                            var overResult: Int? = null
                            var overRecovery: Int? = null

                            electricSectionListState.forEach {
                                val accepted = it.accepted.data
                                val delivery = it.delivery.data
                                val acceptedRecovery =
                                    it.recoveryAccepted.data
                                val deliveryRecovery =
                                    it.recoveryDelivery.data

                                val result = delivery - accepted
                                val resultRecovery = deliveryRecovery - acceptedRecovery
                                overResult += result
                                overRecovery += resultRecovery
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                overResult?.let {
                                    Text(
                                        text = "Всего расход = ${it.str()}",
                                        style = AppTypography.getType().bodyMedium,
                                    )
                                }
                                overRecovery?.let {
                                    Text(
                                        text = "Всего рекуперация = ${it.str()}",
                                        style = AppTypography.getType().bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            ClickableText(
                modifier = Modifier.padding(top = 24.dp),
                text = AnnotatedString("Добавить секцию"),
                style = AppTypography.getType().titleMedium
            ) {
                when (locomotive.type.name) {
                    LocoType.DIESEL.name -> addingSectionDiesel()
                    LocoType.ELECTRIC.name -> addingSectionElectric()
                }
                scope.launch {
                    val countItems = scrollState.layoutInfo.totalItemsCount
                    scrollState.animateScrollToItem(countItems)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}