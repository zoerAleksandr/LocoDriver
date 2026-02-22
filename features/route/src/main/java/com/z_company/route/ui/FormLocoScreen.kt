package com.z_company.route.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.z_company.core.ResultState
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.util.CalculationEnergy
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.DieselSectionItem
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.DateTimePickerBottomSheet
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.LocoFormUiState
import java.util.Calendar
import com.z_company.domain.util.*
import com.z_company.domain.util.CalculationEnergy.rounding
import com.z_company.route.R
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.ElectricSectionItem
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.SwitchApp
import com.z_company.route.viewmodel.DieselSectionFormState
import com.z_company.route.viewmodel.DieselSectionType
import com.z_company.route.viewmodel.ElectricSectionFormState
import com.z_company.route.viewmodel.ElectricSectionType
import com.z_company.route.viewmodel.LocoFormViewModel
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun FormLocoScreen(
    viewModel: LocoFormViewModel,
    currentLoco: Locomotive?,
    dieselSectionListState: SnapshotStateList<DieselSectionFormState>?,
    electricSectionListState: SnapshotStateList<ElectricSectionFormState>?,
    onLocoSaved: () -> Unit,
    formUiState: LocoFormUiState,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
    onSeriesChanged: (String) -> Unit,
    onChangedTypeLoco: (LocoType) -> Unit,
    onStartAcceptedTimeChanged: (Long?) -> Unit,
    onEndAcceptedTimeChanged: (Long?) -> Unit,
    onStartDeliveryTimeChanged: (Long?) -> Unit,
    onEndDeliveryTimeChanged: (Long?) -> Unit,
    onFuelAcceptedChanged: (Int, String?) -> Unit,
    onFuelDeliveredChanged: (Int, String?) -> Unit,
    onDeleteSectionDiesel: (DieselSectionFormState) -> Unit,
    addingSectionDiesel: () -> Unit,
    focusChangedDieselSection: (Int, DieselSectionType) -> Unit,
    onDeleteSectionElectric: (ElectricSectionFormState) -> Unit,
    addingSectionElectric: () -> Unit,
    focusChangedElectricSection: (Int, ElectricSectionType) -> Unit,
    onExpandStateElectricSection: (Boolean) -> Unit,
    onRefuelValueChanged: (Int, String?) -> Unit,
    onRefuelInKiloValueChanged: (Int, String?) -> Unit,
    onRefuelCoefficientValueChanged: (Int, String?) -> Unit,
    onCoefficientValueChanged: (Int, String?) -> Unit,
    exitScreen: () -> Unit,
    dropDownSeriesMenuList: List<String>,
    isExpandedMenu: Boolean,
    onExpandedMenuChange: (Boolean) -> Unit,
    onChangedContentMenu: (String) -> Unit,
    onDeleteSeries: (String) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingBottomSheet by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var resultVisible by remember { mutableStateOf(false) }

    val noValueColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    Scaffold(
        modifier = Modifier
            .fillMaxWidth(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    TextButton(
                        onClick = viewModel::saveLoco,
                        enabled = formUiState.errorMessage == null,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = Color.Transparent
                        )
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
                    ) {
                        IconButton(
                            onClick = { viewModel.showHeatingCounter(!formUiState.isShowHeatingCounter) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.nest_farsight_heat_24px),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                        IconButton(
                            onClick = { resultVisible = !resultVisible }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.finance_24px),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                        IconButton(
                            onClick = { showTime = !showTime }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.nest_clock_farsight_analog_24px),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                        IconButton(
                            onClick = { showSettingBottomSheet = !showSettingBottomSheet }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.settings_24px),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        }
    ) { paddingValues ->
        if (formUiState.saveLocoState is ResultState.Success) {
            LaunchedEffect(formUiState.saveLocoState) {
                onLocoSaved()
            }
        }
        if (formUiState.saveLocoState is ResultState.Error) {
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка: ${formUiState.saveLocoState.entity.message}")
                }
                resetSaveState()
            }
        }
        if (formUiState.exitFromScreen) {
            LaunchedEffect(Unit) {
                exitScreen()
            }
        }

        Box(modifier = Modifier.padding(paddingValues)) {
            currentLoco?.let { locomotive ->
                val sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true
                )

                val scrollState = rememberLazyListState()
                val focusManager = LocalFocusManager.current
                val scope = rememberCoroutineScope()
                val dataTextStyle = MaterialTheme.typography.bodyLarge

                val subTitleTextStyle = MaterialTheme.typography.bodyMedium

                AnimatedVisibility(
                    modifier = Modifier
                        .zIndex(1f),
                    visible = !scrollState.isScrollInInitialState(),
                    enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    BottomShadow()
                }

                var showBottomSheetRemoveTimeStartAccepted by remember {
                    mutableStateOf(false)
                }

                if (showBottomSheetRemoveTimeStartAccepted) {
                    AppBottomSheet(
                        onDismissRequest = { showBottomSheetRemoveTimeStartAccepted = false },
                        sheetState = sheetState,
                        headerContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Начало приемки",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = listOf(
                            BottomSheetAction(text = "Удалить значение") {
                                onStartAcceptedTimeChanged(null)
                            }
                        )
                    )
                }

                var showBottomSheetRemoveTimeStartDelivery by remember { mutableStateOf(false) }

                if (showBottomSheetRemoveTimeStartDelivery) {
                    AppBottomSheet(
                        onDismissRequest = { showBottomSheetRemoveTimeStartDelivery = false },
                        sheetState = sheetState,
                        headerContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Начало сдачи",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = listOf(
                            BottomSheetAction(text = "Удалить значение") {
                                onStartDeliveryTimeChanged(null)
                            }
                        )
                    )
                }

                var showBottomSheetRemoveTimeEndAccepted by remember {
                    mutableStateOf(false)
                }

                if (showBottomSheetRemoveTimeEndAccepted) {
                    AppBottomSheet(
                        onDismissRequest = { showBottomSheetRemoveTimeEndAccepted = false },
                        sheetState = sheetState,
                        headerContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Окончание приемки",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = listOf(
                            BottomSheetAction(text = "Удалить значение") {
                                onEndAcceptedTimeChanged(null)
                            }
                        )
                    )
                }

                var showBottomSheetRemoveTimeEndDelivery by remember {
                    mutableStateOf(false)
                }

                if (showBottomSheetRemoveTimeEndDelivery) {
                    AppBottomSheet(
                        onDismissRequest = { showBottomSheetRemoveTimeEndDelivery = false },
                        sheetState = sheetState,
                        headerContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Окончание сдачи",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = listOf(
                            BottomSheetAction(text = "Удалить значение") {
                                onEndDeliveryTimeChanged(null)
                            }
                        )
                    )
                }

                if (showSettingBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showSettingBottomSheet = false },
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
                        val switchColors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                            checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                            item {
                                Text(
                                    text = "Настройки",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            item { Spacer(modifier = Modifier.height(20.dp)) }
                            item {
                                Text(
                                    text = "Норма на поездку",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (locomotive.type == LocoType.ELECTRIC) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        var norma1 =
                                            locomotive.normaElectricCurrent1?.takeIf { it != 0 }
                                                ?.toString() ?: ""
                                        val norma2 =
                                            locomotive.normaElectricCurrent2?.takeIf { it != 0 }
                                                ?.toString() ?: ""

                                        OutlinedTextFieldApp(
                                            modifier = Modifier.weight(1f),
                                            value = norma1,
                                            placeholder = {
                                                Text(
                                                    text = "Ток 1",
                                                    style = LocalTextStyle.current.copy(
                                                        fontWeight = FontWeight.Light
                                                    ),
                                                    color = noValueColor
                                                )
                                            },
                                            textStyle = dataTextStyle,
                                            onValueChange = { viewModel.setNormaElectricCurrent1(it) },
                                            borderColor = MaterialTheme.colorScheme.primary,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                                imeAction = ImeAction.Done
                                            ),
                                            singleLine = true
                                        )

                                        Row(modifier = Modifier.weight(1f)) {
                                            AnimatedVisibility(
                                                modifier = Modifier.fillMaxWidth(),
                                                visible = formUiState.isShowOtherCurrent
                                            ) {
                                                OutlinedTextFieldApp(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    value = norma2,
                                                    placeholder = {
                                                        Text(
                                                            text = "Ток 2",
                                                            style = LocalTextStyle.current.copy(
                                                                fontWeight = FontWeight.Light
                                                            ),
                                                            color = noValueColor
                                                        )
                                                    },
                                                    textStyle = dataTextStyle,
                                                    onValueChange = {
                                                        viewModel.setNormaElectricCurrent2(
                                                            it
                                                        )
                                                    },
                                                    borderColor = MaterialTheme.colorScheme.primary,
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Decimal,
                                                        imeAction = ImeAction.Done
                                                    ),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = "Смена рода тока",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            overflow = TextOverflow.Visible,
                                            maxLines = 2
                                        )
                                        Switch(
                                            checked = formUiState.isShowOtherCurrent,
                                            onCheckedChange = { viewModel.showOtherCurrent(it) },
                                            colors = switchColors
                                        )
                                    }
                                }
                            } else {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        var norma = locomotive.normaDiesel ?: ""
                                        OutlinedTextFieldApp(
                                            modifier = Modifier.fillMaxWidth(),
                                            value = norma,
                                            textStyle = dataTextStyle,
                                            suffix = {
                                                Text(
                                                    text = "Кг.",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onValueChange = { viewModel.setNormaDiesel(it) },
                                            borderColor = MaterialTheme.colorScheme.primary,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                                imeAction = ImeAction.Done
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(20.dp)) }
                            item {
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = Shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    elevation = ButtonDefaults.elevatedButtonElevation(
                                        defaultElevation = 3.dp,
                                        pressedElevation = 0.dp
                                    ),
                                    onClick = { scope.launch { showSettingBottomSheet = false } }
                                ) {
                                    Text(
                                        text = "Готово",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            item { Spacer(modifier = Modifier.height(20.dp)) }
                        }
                    }
                }

                LazyColumn(
                    state = scrollState,
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        val currentType = locomotive.type
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedContent(targetState = currentType, label = "") {
                                val text = if (it == LocoType.ELECTRIC) LocoType.ELECTRIC.text
                                else LocoType.DIESEL.text

                                Text(
                                    text = text,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            SwitchApp(
                                modifier = Modifier.wrapContentWidth(),
                                checked = currentType == LocoType.ELECTRIC,
                                onCheckedChange = {
                                    onChangedTypeLoco(
                                        if (currentType == LocoType.ELECTRIC) {
                                            LocoType.DIESEL
                                        } else {
                                            LocoType.ELECTRIC
                                        }
                                    )
                                },
                                positiveContent = {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.electric_bolt_24px),

                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            )
                                        )
                                    }
                                },
                                negativeContent = {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.opacity_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                modifier = Modifier
                                    .weight(1f),
                                expanded = isExpandedMenu,
                                onExpandedChange = onExpandedMenuChange
                            ) {
                                var seriesName by remember {
                                    mutableStateOf(
                                        TextFieldValue(
                                            text = locomotive.series ?: "",
                                            selection = TextRange(locomotive.series?.length ?: 0)
                                        )
                                    )
                                }

                                OutlinedTextFieldApp(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    value = seriesName,
                                    onValueChange = {
                                        seriesName = it
                                        onSeriesChanged(it.text)
                                        onChangedContentMenu(it.text)
                                    },
                                    placeholder = {
                                        Text(
                                            text = "Серия",
                                            style = LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            ),
                                            color = noValueColor
                                        )
                                    },
                                    textStyle = dataTextStyle,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    singleLine = true
                                )

                                if (dropDownSeriesMenuList.isNotEmpty()) {
                                    DropdownMenu(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = Shapes.medium
                                            )
                                            .exposedDropdownSize(true),
                                        expanded = isExpandedMenu,
                                        properties = PopupProperties(focusable = false),
                                        onDismissRequest = { onExpandedMenuChange(false) }
                                    ) {
                                        dropDownSeriesMenuList.forEach { selectionSeries ->
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
                                                            text = selectionSeries,
                                                            style = dataTextStyle,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Icon(
                                                            modifier = Modifier.clickable {
                                                                onDeleteSeries(selectionSeries)
                                                            },
                                                            painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    onSeriesChanged(selectionSeries)
                                                    onExpandedMenuChange(false)
                                                    seriesName = seriesName.copy(
                                                        text = selectionSeries,
                                                        selection = TextRange(selectionSeries.length)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .weight(1f),
                                value = locomotive.number ?: "",
                                textStyle = dataTextStyle,
                                placeholder = {
                                    Text(
                                        text = "Номер",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor
                                    )
                                },
                                onValueChange = { onNumberChanged(it) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                    }
                                ),
                                singleLine = true,
                            )
                        }
                    }

                    // время
                    item {
                        AnimatedVisibility(showTime) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CustomDivider(orientation = Orientation.Horizontal)
                                var showStartAcceptedDatePicker by remember {
                                    mutableStateOf(false)
                                }

                                if (showStartAcceptedDatePicker) {
                                    DateTimePickerBottomSheet(
                                        title = "Начало приемки",
                                        onDateTimeSelected = { timestamp ->
                                            onStartAcceptedTimeChanged(timestamp)
                                        },
                                        onDismiss = { showStartAcceptedDatePicker = false },
                                        startDateTime = locomotive.timeStartOfAcceptance
                                            ?: Calendar.getInstance().timeInMillis
                                    )
                                }

                                var showEndAcceptedDatePicker by remember {
                                    mutableStateOf(false)
                                }

                                if (showEndAcceptedDatePicker) {
                                    DateTimePickerBottomSheet(
                                        title = "Окончание приемки",
                                        onDateTimeSelected = { timestamp ->
                                            onEndAcceptedTimeChanged(timestamp)
                                        },
                                        onDismiss = { showEndAcceptedDatePicker = false },
                                        startDateTime = locomotive.timeEndOfAcceptance
                                            ?: Calendar.getInstance().timeInMillis
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Время приемки",
                                        style = subTitleTextStyle
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val animatedBackgroundColorsStartAcceptance by animateColorAsState(
                                            targetValue = if (locomotive.timeStartOfAcceptance == null) MaterialTheme.colorScheme.surface
                                            else MaterialTheme.colorScheme.secondary,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        val animatedBackgroundColorsEndAcceptance by animateColorAsState(
                                            targetValue = if (locomotive.timeEndOfAcceptance == null) MaterialTheme.colorScheme.surface
                                            else MaterialTheme.colorScheme.secondary,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = FastOutSlowInEasing
                                            )
                                        )

                                        Box(
                                            modifier = Modifier
                                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                                .background(
                                                    color = animatedBackgroundColorsStartAcceptance,
                                                    shape = Shapes.medium
                                                )
                                                .weight(1f)
                                                .combinedClickable(
                                                    onClick = {
                                                        showStartAcceptedDatePicker = true
                                                    },
                                                    onLongClick = {
                                                        locomotive.timeStartOfAcceptance?.let {
                                                            showBottomSheetRemoveTimeStartAccepted =
                                                                true
                                                        }
                                                    }
                                                )
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                        ) {
                                            val dateStartText =
                                                locomotive.timeStartOfAcceptance?.let {
                                                    dateAndTimeConverter?.getDateMiniAndTime(it)
                                                } ?: "Начало"

                                            val color = locomotive.timeStartOfAcceptance?.let {
                                                MaterialTheme.colorScheme.primary
                                            } ?: noValueColor

                                            val style = locomotive.timeStartOfAcceptance?.let {
                                                dataTextStyle
                                            } ?: LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            )

                                            Text(
                                                text = dateStartText,
                                                style = style,
                                                color = color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                                .background(
                                                    color = animatedBackgroundColorsEndAcceptance,
                                                    shape = Shapes.medium
                                                )
                                                .weight(1f)
                                                .combinedClickable(
                                                    onClick = {
                                                        showEndAcceptedDatePicker = true
                                                    },
                                                    onLongClick = {
                                                        locomotive.timeEndOfAcceptance?.let {
                                                            showBottomSheetRemoveTimeEndAccepted =
                                                                true
                                                        }
                                                    }
                                                )
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                        ) {
                                            val dateEndText = locomotive.timeEndOfAcceptance?.let {
                                                dateAndTimeConverter?.getDateMiniAndTime(it)
                                            } ?: "Окончание"

                                            val color = locomotive.timeStartOfAcceptance?.let {
                                                MaterialTheme.colorScheme.primary
                                            } ?: noValueColor

                                            val style = locomotive.timeStartOfAcceptance?.let {
                                                dataTextStyle
                                            } ?: LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            )

                                            Text(
                                                text = dateEndText,
                                                style = style,
                                                color = color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                var showStartDeliveryDatePicker by remember {
                                    mutableStateOf(false)
                                }

                                if (showStartDeliveryDatePicker) {
                                    DateTimePickerBottomSheet(
                                        title = "Начало сдачи",
                                        onDateTimeSelected = { timestamp ->
                                            onStartDeliveryTimeChanged(timestamp)
                                        },
                                        onDismiss = { showStartDeliveryDatePicker = false },
                                        startDateTime = locomotive.timeStartOfDelivery
                                            ?: Calendar.getInstance().timeInMillis
                                    )
                                }

                                var showEndDeliveryDatePicker by remember {
                                    mutableStateOf(false)
                                }

                                if (showEndDeliveryDatePicker) {
                                    DateTimePickerBottomSheet(
                                        title = "Окончание сдачи",
                                        onDateTimeSelected = { timestamp ->
                                            onEndDeliveryTimeChanged(timestamp)
                                        },
                                        onDismiss = { showEndDeliveryDatePicker = false },
                                        startDateTime = locomotive.timeEndOfDelivery
                                            ?: Calendar.getInstance().timeInMillis
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Время сдачи",
                                        style = subTitleTextStyle
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val animatedBackgroundColorsStartDelivery by animateColorAsState(
                                            targetValue = if (locomotive.timeStartOfDelivery == null) MaterialTheme.colorScheme.surface
                                            else MaterialTheme.colorScheme.secondary,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        val animatedBackgroundColorsEndDelivery by animateColorAsState(
                                            targetValue = if (locomotive.timeEndOfDelivery == null) MaterialTheme.colorScheme.surface
                                            else MaterialTheme.colorScheme.secondary,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                                .background(
                                                    color = animatedBackgroundColorsStartDelivery,
                                                    shape = Shapes.medium
                                                )
                                                .weight(1f)
                                                .combinedClickable(
                                                    onClick = {
                                                        showStartDeliveryDatePicker = true
                                                    },
                                                    onLongClick = {
                                                        locomotive.timeStartOfDelivery?.let {
                                                            showBottomSheetRemoveTimeStartDelivery =
                                                                true
                                                        }
                                                    }
                                                )
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            val dateStartText =
                                                locomotive.timeStartOfDelivery?.let {
                                                    dateAndTimeConverter?.getDateMiniAndTime(it)
                                                } ?: "Начало"

                                            val color = locomotive.timeStartOfDelivery?.let {
                                                MaterialTheme.colorScheme.primary
                                            } ?: noValueColor

                                            val style = locomotive.timeStartOfDelivery?.let {
                                                dataTextStyle
                                            } ?: LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            )

                                            Text(
                                                text = dateStartText,
                                                style = style,
                                                color = color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .shadow(elevation = 2.dp, shape = Shapes.medium)
                                                .background(
                                                    color = animatedBackgroundColorsEndDelivery,
                                                    shape = Shapes.medium
                                                )
                                                .weight(1f)
                                                .combinedClickable(
                                                    onClick = {
                                                        showEndDeliveryDatePicker = true
                                                    },
                                                    onLongClick = {
                                                        locomotive.timeEndOfDelivery?.let {
                                                            showBottomSheetRemoveTimeEndDelivery =
                                                                true
                                                        }
                                                    }
                                                )
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                        ) {
                                            val dateEndText = locomotive.timeEndOfDelivery?.let {
                                                dateAndTimeConverter?.getDateMiniAndTime(it)
                                            } ?: "Окончание"

                                            val color = locomotive.timeEndOfDelivery?.let {
                                                MaterialTheme.colorScheme.primary
                                            } ?: noValueColor

                                            val style = locomotive.timeEndOfDelivery?.let {
                                                dataTextStyle
                                            } ?: LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            )

                                            Text(
                                                text = dateEndText,
                                                style = style,
                                                color = color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // отопление
                    item {
                        val accepted =
                            locomotive.heatingCounterAccepted?.takeIf { it != 0.0 }
                                ?.toLong()?.toString() ?: ""
                        val delivered =
                            locomotive.heatingCounterDelivery?.takeIf { it != 0.0 }
                                ?.toLong()?.toString() ?: ""
                        val heatingResult = delivered.toIntOrNull() - accepted.toIntOrNull()
                        AnimatedVisibility(formUiState.isShowHeatingCounter) {
                            Column(
                                modifier = Modifier.padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CustomDivider(orientation = Orientation.Horizontal)
                                Text(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp),
                                    text = "Отопление",
                                    style = subTitleTextStyle
                                )
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextFieldApp(
                                        modifier = Modifier.weight(1f),
                                        value = accepted,
                                        textStyle = dataTextStyle,
                                        placeholder = {
                                            Text(
                                                text = "Принял",
                                                style = LocalTextStyle.current.copy(
                                                    fontWeight = FontWeight.Light
                                                ),
                                                color = noValueColor
                                            )
                                        },
                                        onValueChange = {
                                            viewModel.setHeatingCounterAccepted(it)
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            imeAction = ImeAction.Next,
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = {
                                                focusManager.moveFocus(FocusDirection.Right)
                                            }
                                        ),
                                        singleLine = true,
                                        shape = Shapes.medium,
                                    )

                                    OutlinedTextFieldApp(
                                        modifier = Modifier.weight(1f),
                                        value = delivered,
                                        textStyle = dataTextStyle,
                                        placeholder = {
                                            Text(
                                                text = "Сдал",
                                                style = LocalTextStyle.current.copy(
                                                    fontWeight = FontWeight.Light
                                                ),
                                                color = noValueColor
                                            )
                                        },
                                        onValueChange = {
                                            viewModel.setHeatingCounterDelivery(it)
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            imeAction = ImeAction.Done,
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                focusManager.clearFocus()
                                            }
                                        ),
                                        singleLine = true,
                                        shape = Shapes.medium,
                                    )
                                }
                                AnimatedVisibility(heatingResult != null) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(end = 16.dp),
                                            text = heatingResult.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Итоги
                    item {
                        AnimatedVisibility(
                            modifier = Modifier
                                .fillMaxWidth(),
                            visible = resultVisible && currentLoco.type == LocoType.ELECTRIC
                        ) {
                            var overResult: Double? = null
                            var overRecovery: Double? = null

                            var overResult2: Double? = null
                            var overRecovery2: Double? = null

                            electricSectionListState?.forEach {
                                val accepted =
                                    it.accepted.data?.toDoubleOrNull()
                                val delivery =
                                    it.delivery.data?.toDoubleOrNull()
                                val acceptedRecovery =
                                    it.recoveryAccepted.data?.toDoubleOrNull()
                                val deliveryRecovery =
                                    it.recoveryDelivery.data?.toDoubleOrNull()
                                val accepted2 =
                                    it.accepted2.data?.toDoubleOrNull()
                                val delivery2 =
                                    it.delivery2.data?.toDoubleOrNull()
                                val acceptedRecovery2 =
                                    it.recoveryAccepted2.data?.toDoubleOrNull()
                                val deliveryRecovery2 =
                                    it.recoveryDelivery2.data?.toDoubleOrNull()

                                val result = delivery - accepted
                                val resultRecovery =
                                    deliveryRecovery - acceptedRecovery

                                val result2 = delivery2 - accepted2
                                val resultRecovery2 = deliveryRecovery2 - acceptedRecovery2

                                overResult += result
                                overRecovery += resultRecovery

                                overResult2 += result2
                                overRecovery2 += resultRecovery2
                            }
                            Column(
                                modifier = Modifier.padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CustomDivider(orientation = Orientation.Horizontal)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    item {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .shadow(
                                                        elevation = 2.dp,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceDim,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .size(40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.electric_bolt_24px),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.8f
                                                    ),
                                                )
                                            }
                                            Text(
                                                text = "Расход",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = overResult?.toLong()?.toString()
                                                    ?: "0",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    item {
                                        Column(
                                            modifier = Modifier.noRippleEffect(
                                                onClick = { showSettingBottomSheet = true }
                                            ),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .shadow(
                                                        elevation = 2.dp,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .background(
                                                        color = Color(0xFFE29960),
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .size(40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.electric_bolt_24px),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.8f
                                                    ),
                                                )
                                            }
                                            Text(
                                                text = "Норма",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = locomotive.normaElectricCurrent1?.toString()
                                                    ?: "0",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    item {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val result =
                                                (locomotive.normaElectricCurrent1 ?: 0) -
                                                    (overResult?.toLong()?.toInt() ?: 0)

                                            val resultText = if (result > 0) {
                                                "+${result}"
                                            } else {
                                                result.toString()
                                            }

                                            val backgroundColor = if (result < 0) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .shadow(
                                                        elevation = 2.dp,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .background(
                                                        color = backgroundColor,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .size(40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.electric_bolt_24px),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                )
                                            }

                                            Text(
                                                text = "Итог",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = resultText,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    item {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .shadow(
                                                        elevation = 2.dp,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceDim,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        shape = RoundedCornerShape(60.dp)
                                                    )
                                                    .size(40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.cycle_24px),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.8f
                                                    ),
                                                )
                                            }
                                            Text(
                                                text = "Рекуп",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = overRecovery?.toLong()?.toString() ?: "0",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    if (formUiState.isShowOtherCurrent) {
                                        item {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .shadow(
                                                            elevation = 2.dp,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .background(
                                                            color = MaterialTheme.colorScheme.surfaceDim,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = MaterialTheme.colorScheme.tertiary,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .size(40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.electric_bolt_24px),
                                                        contentDescription = null
                                                    )
                                                }
                                                Text(
                                                    text = "Расход2",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = overResult2?.toLong()?.toString()
                                                        ?: "0",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }

                                        item {
                                            Column(
                                                modifier = Modifier.noRippleEffect(
                                                    onClick = { showSettingBottomSheet = true }
                                                ),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .shadow(
                                                            elevation = 2.dp,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .background(
                                                            color = Color(0xFFE29960),
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = MaterialTheme.colorScheme.tertiary,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .size(40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.electric_bolt_24px),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.8f
                                                        ),
                                                    )
                                                }
                                                Text(
                                                    text = "Норма2",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = locomotive.normaElectricCurrent2?.toString()
                                                        ?: "0",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }

                                        item {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {

                                                val result2 =
                                                    (locomotive.normaElectricCurrent2 ?: 0) -
                                                        (overResult2?.toLong()?.toInt() ?: 0)

                                                val resultText2 = if (result2 > 0) {
                                                    "+${result2}"
                                                } else {
                                                    result2.toString()
                                                }

                                                val backgroundColor = if (result2 < 0) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerLow
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .shadow(
                                                            elevation = 2.dp,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .background(
                                                            color = backgroundColor,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = MaterialTheme.colorScheme.tertiary,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .size(40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.electric_bolt_24px),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                    )
                                                }
                                                Text(
                                                    text = "Итог2",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = resultText2,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }

                                        item {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier

                                                        .shadow(
                                                            elevation = 2.dp,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .background(
                                                            color = MaterialTheme.colorScheme.surfaceDim,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = MaterialTheme.colorScheme.tertiary,
                                                            shape = RoundedCornerShape(60.dp)
                                                        )
                                                        .size(40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.cycle_24px),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.8f
                                                        ),
                                                    )
                                                }
                                                Text(
                                                    text = "Рекуп2",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = overRecovery2?.toString() ?: "0",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            modifier = Modifier
                                .fillMaxWidth(),
                            visible = resultVisible && currentLoco.type == LocoType.DIESEL
                        ) {
                            var overResultDieselInLiter: Double? = null
                            var overResultDieselInKilo: Double? = null

                            dieselSectionListState?.forEach {
                                val accepted = it.accepted.data?.toDoubleOrNull()
                                val delivery = it.delivery.data?.toDoubleOrNull()
                                val refuel = it.refuel.data?.toDoubleOrNull()
                                val result = CalculationEnergy.getTotalFuelConsumption(
                                    accepted, delivery, refuel
                                )
                                val resultInKilo =
                                    result.times(it.coefficient.data?.toDoubleOrZero())

                                overResultDieselInLiter += result
                                overResultDieselInKilo += resultInKilo
                            }

                            Column(
                                modifier = Modifier.padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CustomDivider(orientation = Orientation.Horizontal)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    item {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .shadow(
                                                        elevation = 2.dp,
                                                        shape = CircleShape
                                                    )
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceDim,
                                                        shape = CircleShape
                                                    )
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Л",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            Text(
                                                text = "Расход",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = rounding(
                                                    overResultDieselInLiter,
                                                    2
                                                )?.toString() ?: "0",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    item {
                                        Column(
                                            modifier = Modifier.noRippleEffect(
                                                onClick = { showSettingBottomSheet = true }
                                            ),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .shadow(
                                                        elevation = 2.dp,
                                                        shape = CircleShape
                                                    )
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceDim,
                                                        shape = CircleShape
                                                    )
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Кг",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            Text(
                                                text = "Расход",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = rounding(overResultDieselInKilo, 2)?.str()
                                                    ?: "0",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    item {
                                        Column(
                                            modifier = Modifier.noRippleEffect(
                                                onClick = { showSettingBottomSheet = true }
                                            ),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .shadow(
                                                        elevation = 2.dp,
                                                        shape = CircleShape
                                                    )
                                                    .background(
                                                        color = Color(0xFFE29960),
                                                        shape = CircleShape
                                                    )
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Кг",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            Text(
                                                text = "Норма",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = locomotive.normaDiesel?.toString() ?: "0",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    item {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (!locomotive.normaDiesel.isNullOrBlank()) {
                                                val result = (locomotive.normaDiesel?.toDoubleOrZero() - overResultDieselInKilo) ?: 0.0

                                                val resultText = if (result > 0) {
                                                    "+${rounding(result, 2).str()}"
                                                } else {
                                                    rounding(result, 2).str2decimalSign()
                                                }
                                                val backgroundColor = if (result < 0) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerLow
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .shadow(
                                                            elevation = 2.dp,
                                                            shape = CircleShape
                                                        )
                                                        .background(
                                                            color = backgroundColor,
                                                            shape = CircleShape
                                                        )
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = MaterialTheme.colorScheme.tertiary,
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.opacity_24px),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }

                                                Text(
                                                    text = "Итог",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = resultText,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
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
                                    Column(horizontalAlignment = Alignment.End) {
                                        CustomDivider(orientation = Orientation.Horizontal)

                                        DieselSectionItem(
                                            item = item,
                                            index = index,
                                            onFuelAcceptedChanged = onFuelAcceptedChanged,
                                            onFuelDeliveredChanged = onFuelDeliveredChanged,
                                            onDeleteItem = onDeleteSectionDiesel,
                                            focusChangedDieselSection = focusChangedDieselSection,
                                            onRefuelValueChanged = onRefuelValueChanged,
                                            onRefuelInKiloValueChanged = onRefuelInKiloValueChanged,
                                            onRefuelCoefficientValueChanged = onRefuelCoefficientValueChanged,
                                            onCoefficientValueChanged = onCoefficientValueChanged,
                                            sheetState = sheetState,
                                            isKiloMode = formUiState.isKiloMode,
                                            changeIsKiloMode = viewModel::toggleIsKiloMode
                                        )
                                        if (index == dieselSectionListState.lastIndex) {
                                            CustomDivider(orientation = Orientation.Horizontal)
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
                                    Column(horizontalAlignment = Alignment.End) {
                                        CustomDivider(orientation = Orientation.Horizontal)

                                        ElectricSectionItem(
                                            index = index,
                                            item = item,
                                            onDeleteItem = onDeleteSectionElectric,
                                            onEnergyAcceptedChanged = viewModel::setEnergyAccepted,
                                            onEnergyDeliveryChanged = viewModel::setEnergyDelivery,
                                            onRecoveryAcceptedChanged = viewModel::setRecoveryAccepted,
                                            onRecoveryDeliveryChanged = viewModel::setRecoveryDelivery,
                                            onEnergyAcceptedChanged2 = viewModel::setEnergyAccepted2,
                                            onEnergyDeliveryChanged2 = viewModel::setEnergyDelivery2,
                                            onRecoveryAcceptedChanged2 = viewModel::setRecoveryAccepted2,
                                            onRecoveryDeliveryChanged2 = viewModel::setRecoveryDelivery2,
                                            focusChangedElectricSection = focusChangedElectricSection,
                                            onExpandStateChanged = onExpandStateElectricSection,
                                            showOtherCurrent = formUiState.isShowOtherCurrent
                                        )

                                        if (index == electricSectionListState.lastIndex) {
                                            CustomDivider(orientation = Orientation.Horizontal)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .padding(top = 24.dp),
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
                                when (locomotive.type.name) {
                                    LocoType.DIESEL.name -> addingSectionDiesel()
                                    LocoType.ELECTRIC.name -> addingSectionElectric()
                                }
                                scope.launch {
                                    val countItems = scrollState.layoutInfo.totalItemsCount
                                    scrollState.animateScrollToItem(countItems)
                                }
                            }
                        ) {
                            Text(
                                text = "Добавить секцию",
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