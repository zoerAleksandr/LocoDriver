package com.z_company.route.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncDataValue
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.DateRangePickerBottomSheet
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import androidx.compose.ui.text.font.FontWeight
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.SurchargeExtendedServicePhase
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import androidx.compose.material3.rememberModalBottomSheetState
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.SwipeToRevealDelete
import com.z_company.route.viewmodel.SettingSalaryUIState
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingSalaryScreen(
    onBack: () -> Unit,
    onTariffDateClick: () -> Unit,
    isEnableSaveButton: Boolean,
    uiState: SettingSalaryUIState,
    saveSettingState: ResultState<Unit>?,
    resetSaveState: () -> Unit,
    tariffRateValueState: ResultState<String>,
    setTariffRate: (String) -> Unit,
    oldTariffRateValueState: ResultState<String>,
    setOldTariffRate: (String) -> Unit,
    isErrorInputTariffRate: Boolean,
    setAveragePaymentHour: (String) -> Unit,
    setNordicCoefficient: (String) -> Unit,
    setDistrictCoefficient: (String) -> Unit,
    zonalSurchargeValueState: ResultState<String>,
    setZonalSurcharge: (String) -> Unit,
    isErrorInputZonalSurcharge: Boolean,
    surchargeQualificationClassValueState: ResultState<String>,
    setSurchargeQualificationClass: (String) -> Unit,
    isErrorInputSurchargeQualificationClass: Boolean,
    surchargeExtendedServicePhaseValueState: SnapshotStateList<SurchargeExtendedServicePhase>,
    addServicePhase: () -> Unit,
    setSurchargeExtendedServicePhaseDistance: (Int, String) -> Unit,
    setSurchargeExtendedServicePhasePercent: (Int, String) -> Unit,
    onePersonOperationPercent: ResultState<String>,
    setOnePersonOperationPercent: (String) -> Unit,
    isErrorInputOnePersonOperation: Boolean,
    onePersonOperationPassengerTrainPercent: ResultState<String>,
    setOnePersonOperationPassengerTrainPercent: (String) -> Unit,
    isErrorInputOnePersonOperationPassengerTrain: Boolean,
    harmfulnessPercentState: ResultState<String>,
    setHarmfulnessPercent: (String) -> Unit,
    isErrorInputHarmfulness: Boolean,
    surchargeHeavyLongDistanceTrainsState: ResultState<String>,
    setSurchargeHeavyLongDistanceTrains: (String) -> Unit,
    isErrorInputSurchargeHeavyLongDistanceTrains: Boolean,
    surchargeHeavyTrainsState: SnapshotStateList<SurchargeHeavyTrains>,
    addSurchargeHeavyTran: () -> Unit,
    setSurchargeHeavyTrainPercent: (Int, String) -> Unit,
    setSurchargeHeavyTrainWeight: (Int, String) -> Unit,
    onSurchargeHeavyTrainDismissed: (Int) -> Unit,
    surchargeLongTrainsState: SnapshotStateList<SurchargeLongTrains>,
    addSurchargeLongTrain: () -> Unit,
    setSurchargeLongTrainPercent: (Int, String) -> Unit,
    setSurchargeLongTrainLength: (Int, String) -> Unit,
    onSurchargeLongTrainDismissed: (Int) -> Unit,
    ndflValueState: ResultState<String>,
    setNDFL: (String) -> Unit,
    isErrorInputNdfl: Boolean,
    unionistsRetentionState: ResultState<String>,
    setUnionistsRetention: (String) -> Unit,
    isErrorInputUnionistsRetention: Boolean,
    otherRetentionValueState: ResultState<String>,
    setOtherRetention: (String) -> Unit,
    isErrorInputOtherRetention: Boolean,
    welfarePercentState: ResultState<String>,
    setWelfarePercent: (String) -> Unit,
    isErrorInputWelfarePercent: Boolean,
    alimonyPercentState: ResultState<String>,
    setAlimonyPercent: (String) -> Unit,
    isErrorInputAlimonyPercent: Boolean,
    showUnderworkPayments: Boolean,
    setShowUnderworkPayments: (Boolean) -> Unit,
    onServicePhaseDismissed: (Int) -> Unit,
    isShowDialogChangeTariffRate: Boolean,
    onHideDialogChangeTariffRate: () -> Unit,
    saveOnlyMonthTariffRate: () -> Unit,
    saveTariffRateCurrentAndNextMonth: () -> Unit,
    setOtherSurcharge: (String) -> Unit,
    currentMonthOfYear: MonthOfYear?,
    setDateNewTariffRate: (Calendar) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val hintStyle = MaterialTheme.typography.bodyMedium
    // Цифры в полях ввода — моноширинные (значения/деньги), 17sp SemiBold как в дизайне.
    val payFieldStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = MonoFont,
        fontWeight = FontWeight.SemiBold,
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val paddingLarge = 6.dp
    val paddingSmall = 6.dp

    if (saveSettingState is ResultState.Error) {
        LaunchedEffect(Unit) {
            scope.launch {
                snackbarHostState.showSnackbar("Ошибка: ${saveSettingState.entity.message}")
            }
            resetSaveState()
        }
    }

    var isShowSetDateTariffRateDialog by remember { mutableStateOf(false) }
    // Подтверждение удаления строки доплаты (свайп раскрывает «Удалить», потом спрашиваем).
    var pendingTierDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    var tierCloseSignal by remember { mutableStateOf(0) }
    var thresholdHelp by remember { mutableStateOf<ThresholdHelp?>(null) }

    pendingTierDelete?.let { action ->
        val tierDeleteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        AppBottomSheet(
            onDismissRequest = {
                pendingTierDelete = null
                tierCloseSignal++
            },
            sheetState = tierDeleteSheetState,
            title = "Удалить доплату?",
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    action()
                    pendingTierDelete = null
                }
            )
        )
    }

    thresholdHelp?.let { help ->
        val helpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        AppBottomSheet(
            onDismissRequest = { thresholdHelp = null },
            sheetState = helpSheetState,
            title = help.title,
            cancelText = "Понятно",
            onCancel = { thresholdHelp = null },
            contentAfterHeader = {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    text = help.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }

    AnimationDialog(
        showDialog = isShowSetDateTariffRateDialog,
        onDismissRequest = { isShowSetDateTariffRateDialog = false }
    ) {

        val currentCalendar = Calendar.getInstance()
        currentMonthOfYear?.let {
            currentCalendar.apply {
                set(Calendar.YEAR, it.year)
                set(Calendar.MONTH, it.month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            it.dateSetTariffRate?.let { date ->
                currentCalendar.set(Calendar.DAY_OF_MONTH, date.dateNewRate)
            }
        }

        // изменить !!!!!
        DateRangePickerBottomSheet(
            onDateRangeSelected = { list ->
                if (list.isNotEmpty()) {
                    setDateNewTariffRate(list.first())
                }
                isShowSetDateTariffRateDialog = false
            },
            onDismiss = {
                isShowSetDateTariffRateDialog = false
            },
            title = "Дата начала действия нового тарифа",
        )
    }

    AnimationDialog(
        showDialog = isShowDialogChangeTariffRate,
        onDismissRequest = onHideDialogChangeTariffRate
    ) {
        val currentDateSetTariffRate = currentMonthOfYear?.dateSetTariffRate?.dateNewRate ?: 1

        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        )
        {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Изменилась тарифная ставка",
                    overflow = TextOverflow.Visible,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Для какого месяца сохранить тариф?",
                    overflow = TextOverflow.Visible,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Text(
                    modifier = Modifier.noRippleEffect {
                        isShowSetDateTariffRateDialog = true
                    },
                    text = "Новый тариф начнет действовать с $currentDateSetTariffRate ${
                        getMonthFullText(
                            currentMonthOfYear?.month
                        )
                    } ${currentMonthOfYear?.year.toString()}",
                    overflow = TextOverflow.Visible,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 1.dp),
                        onClick = saveOnlyMonthTariffRate
                    ) {
                        Text(
                            text = "Только для этого",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 1.dp),
                        onClick = saveTariffRateCurrentAndNextMonth
                    ) {
                        Text(
                            text = "Для этого и следующих",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Visible,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        shape = Shapes.medium,
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 1.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error
                        ),
                        onClick = onHideDialogChangeTariffRate,
                    ) {
                        Text(
                            style = MaterialTheme.typography.bodySmall,
                            text = "Отмена",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Зарплата",
                        style = MaterialTheme.typography.titleMedium,
                        color = primaryColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(com.z_company.core.R.drawable.ic_arrow_back),
                            contentDescription = "Назад",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        }
    ) { paddingValue ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    text = "Начисления",
                    overflow = TextOverflow.Visible,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            item {
                var dateSetTariffRate = 1
                currentMonthOfYear?.dateSetTariffRate?.let {
                    dateSetTariffRate = it.dateNewRate
                }
                PayCard {
                    PayFieldSlot(
                        label = "Тарифная ставка",
                        action = {
                            Row(
                                modifier = Modifier.clickable(onClick = onTariffDateClick),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("на $dateSetTariffRate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                Text(getMonthFullText(currentMonthOfYear?.month), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                Text(currentMonthOfYear?.year.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    ) {
                        AsyncDataValue(resultState = tariffRateValueState) { v ->
                            v?.let { PayInput(it, setTariffRate, "₽", isErrorInputTariffRate) }
                        }
                    }
                    if (currentMonthOfYear?.dateSetTariffRate != null) {
                        PaySep()
                        PayFieldSlot(
                            label = "Тарифная ставка",
                            action = {
                                Row(
                                    modifier = Modifier.clickable(onClick = onTariffDateClick),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("до $dateSetTariffRate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                    Text(getMonthFullText(currentMonthOfYear.month), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                    Text(currentMonthOfYear.year.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        ) {
                            AsyncDataValue(resultState = oldTariffRateValueState) { v ->
                                v?.let { PayInput(it, setOldTariffRate, "₽", isErrorInputTariffRate) }
                            }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Средний час", description = "Для командировок и других выплат по среднему") {
                        AsyncDataValue(resultState = uiState.averagePaymentHour) { v ->
                            v?.let { PayInput(it, setAveragePaymentHour, "₽", uiState.isErrorInputAveragePayment) }
                        }
                    }
                }
            }

            item {
                PayCard {
                    PayFieldSlot("Зональная надбавка", description = "От тарифа рабочих часов и следования пассажиром") {
                        AsyncDataValue(resultState = zonalSurchargeValueState) { v ->
                            v?.let { PayInput(it, setZonalSurcharge, "%", isErrorInputZonalSurcharge) }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Доплаты за класс и права", description = "От тарифа обычной работы, без следования пассажиром") {
                        AsyncDataValue(resultState = surchargeQualificationClassValueState) { v ->
                            v?.let { PayInput(it, setSurchargeQualificationClass, "%", isErrorInputSurchargeQualificationClass) }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Работа в одно лицо (грузовой)", description = "Только фактические сегменты грузовой работы в одно лицо") {
                        AsyncDataValue(resultState = onePersonOperationPercent) { v ->
                            v?.let { PayInput(it, setOnePersonOperationPercent, "%", isErrorInputOnePersonOperation) }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Работа в одно лицо (пассажирский)", description = "Только фактические сегменты пассажирской работы в одно лицо") {
                        AsyncDataValue(resultState = onePersonOperationPassengerTrainPercent) { v ->
                            v?.let { PayInput(it, setOnePersonOperationPassengerTrainPercent, "%", isErrorInputOnePersonOperationPassengerTrain) }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Доплата за вредность", description = "От тарифа рабочих часов и следования пассажиром") {
                        AsyncDataValue(resultState = harmfulnessPercentState) { v ->
                            v?.let { PayInput(it, setHarmfulnessPercent, "%", isErrorInputHarmfulness) }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Северная надбавка", description = "От подтверждённой базы начислений, без повторного среднего") {
                        AsyncDataValue(resultState = uiState.nordicCoefficient) { v ->
                            v?.let { PayInput(it, setNordicCoefficient, "%", uiState.isErrorInputNordicCoefficient) }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Районный коэффициент", description = "От подтверждённой базы начислений, без повторного среднего") {
                        AsyncDataValue(resultState = uiState.districtCoefficient) { v ->
                            v?.let { PayInput(it, setDistrictCoefficient, "%", uiState.isErrorInputDistrictCoefficient) }
                        }
                    }
                }
            }

            item {
                PayCard {
                    PayFieldSlot("Доплата за ПДМ (>6000 т. и >350 осей)", description = "От тарифа фактического интервала подходящего поезда") {
                        AsyncDataValue(resultState = surchargeHeavyLongDistanceTrainsState) { v ->
                            v?.let {
                                PayInput(
                                    it,
                                    setSurchargeHeavyLongDistanceTrains,
                                    "%",
                                    isErrorInputSurchargeHeavyLongDistanceTrains
                                )
                            }
                        }
                    }
                }
            }

            item {
                PayTierSection(
                    label = "Доплата за тяж. поезда",
                    description = "Порог массы → процент от тарифа интервала поезда",
                    onAdd = addSurchargeHeavyTran,
                ) {
                    surchargeHeavyTrainsState.forEachIndexed { index, item ->
                        key(item.id) {
                            PayTierRow(
                                onDelete = { pendingTierDelete = { onSurchargeHeavyTrainDismissed(index) } },
                                closeSignal = tierCloseSignal,
                                value1 = item.weight,
                                onValue1 = { setSurchargeHeavyTrainWeight(index, it) },
                                unit1 = "т.",
                                value2 = item.percentSurcharge,
                                onValue2 = { setSurchargeHeavyTrainPercent(index, it) },
                                unit2 = "%",
                            )
                        }
                    }
                }
            }

            item {
                PayTierSection(
                    label = "Доплата за длинносост. поезда",
                    description = "Свыше указанной условной длины → процент от тарифа интервала поезда",
                    onAdd = addSurchargeLongTrain,
                    onHelp = {
                        thresholdHelp = ThresholdHelp(
                            title = "Граница длинносоставного поезда",
                            text = "Доплата начинается только когда условная длина поезда больше указанного значения. При равенстве порогу доплата ещё не начисляется. Например: для порога 80 доплата действует с 81 условного вагона.",
                        )
                    },
                ) {
                    surchargeLongTrainsState.forEachIndexed { index, item ->
                        key(item.id) {
                            PayTierRow(
                                onDelete = { pendingTierDelete = { onSurchargeLongTrainDismissed(index) } },
                                closeSignal = tierCloseSignal,
                                value1 = item.conditionalLength,
                                onValue1 = { setSurchargeLongTrainLength(index, it) },
                                unit1 = "ваг.",
                                value2 = item.percentSurcharge,
                                onValue2 = { setSurchargeLongTrainPercent(index, it) },
                                unit2 = "%",
                            )
                        }
                    }
                }
            }

            item {
                PayTierSection(
                    label = "Доплата за удлиненное плечо",
                    description = "Свыше указанного пробега → процент от тарифа обычной работы",
                    onAdd = addServicePhase,
                    onHelp = {
                        thresholdHelp = ThresholdHelp(
                            title = "Граница удлинённого плеча",
                            text = "Доплата начинается только когда пробег больше указанного значения. При равенстве порогу доплата ещё не начисляется. Например: для порога 250 км доплата действует при пробеге свыше 250 км.",
                        )
                    },
                ) {
                    surchargeExtendedServicePhaseValueState.forEachIndexed { index, item ->
                        key(item.id) {
                            PayTierRow(
                                onDelete = { pendingTierDelete = { onServicePhaseDismissed(index) } },
                                closeSignal = tierCloseSignal,
                                value1 = item.distance,
                                onValue1 = { setSurchargeExtendedServicePhaseDistance(index, it) },
                                unit1 = "км",
                                value2 = item.percentSurcharge,
                                onValue2 = { setSurchargeExtendedServicePhasePercent(index, it) },
                                unit2 = "%",
                            )
                        }
                    }
                }
            }

            item {
                PayCard {
                    PayFieldSlot("Другие надбавки", description = "Постоянный процент тарифа обычной работы, не фиксированная премия") {
                        AsyncDataValue(resultState = uiState.otherSurchargeState) { v ->
                            v?.let { PayInput(it, setOtherSurcharge, "%", uiState.isErrorInputOtherSurcharge) }
                        }
                    }
                }
            }

            item {
                PayCard {
                    PayToggleSlot(
                        label = "Показывать оплату недоработки",
                        checked = showUnderworkPayments,
                        onCheckedChange = setShowUnderworkPayments,
                    )
                }
            }

            item {
                Text(
                    "Удержания",
                    overflow = TextOverflow.Visible,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge + 12.dp),
                )
            }

            item {
                PayCard {
                    PayFieldSlot("Подоходный налог") {
                        AsyncDataValue(resultState = ndflValueState) { v ->
                            v?.let { PayInput(it, setNDFL, "%", isErrorInputNdfl) }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Профсоюз") {
                        AsyncDataValue(resultState = unionistsRetentionState) { v ->
                            v?.let { PayInput(it, setUnionistsRetention, "%", isErrorInputUnionistsRetention) }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Прочие удержания") {
                        AsyncDataValue(resultState = otherRetentionValueState) { v ->
                            v?.let { PayInput(it, setOtherRetention, "%", isErrorInputOtherRetention) }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Благосостояние") {
                        AsyncDataValue(resultState = welfarePercentState) { v ->
                            v?.let { PayInput(it, setWelfarePercent, "%", isErrorInputWelfarePercent) }
                        }
                    }
                    PaySep()
                    PayFieldSlot("Алименты") {
                        AsyncDataValue(resultState = alimonyPercentState) { v ->
                            v?.let { PayInput(it, setAlimonyPercent, "%", isErrorInputAlimonyPercent) }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ─── Общие примитивы формы ЗП (по референсу design/src/salary-settings.jsx) ───
// Карточка-группа: белый surface, мягкая тень, 16r. Поля внутри — filled bgSubtle.
@Composable
private fun PayCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .shadow(1.dp, Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium),
        content = content,
    )
}

// Разделитель строк внутри карточки (full-width, как Sep inset=0 в дизайне).
@Composable
private fun PaySep() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// Слот поля: лейбл (+ опц. действие справа) сверху, поле снизу.
@Composable
private fun PayFieldSlot(
    label: String,
    description: String? = null,
    action: (@Composable () -> Unit)? = null,
    input: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            action?.invoke()
        }
        description?.let {
            Spacer(Modifier.height(2.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        input()
    }
}

// Слот-переключатель: лейбл слева, Switch справа, вся строка кликабельна (как
// тумблеры видимости в форме маршрута — см. FormScreen).
@Composable
private fun PayToggleSlot(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

// Filled-инпут: mono-значение + единица справа (suffix). Серый bgSubtle, 12r, без тени.
@Composable
private fun PayInput(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val style = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = MonoFont,
        fontWeight = FontWeight.SemiBold,
    )
    OutlinedTextFieldApp(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        isError = isError,
        supportingText = if (isError) {
            { Text(text = "Некорректные данные") }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        fieldElevation = 0.dp,
        colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
        colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
        suffix = unit?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

// Карточка пороговой доплаты: заголовок + «Добавить» сверху, затем строки [порог·ед][%].
@Composable
private fun PayTierSection(
    label: String,
    description: String? = null,
    onAdd: () -> Unit,
    onHelp: (() -> Unit)? = null,
    rows: @Composable ColumnScope.() -> Unit,
) {
    PayCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                onHelp?.let {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = it,
                    ) {
                        Text(
                            text = "?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
            Text(
                modifier = Modifier.noRippleEffect { onAdd() },
                text = "Добавить",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        description?.let {
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        rows()
    }
}

private data class ThresholdHelp(
    val title: String,
    val text: String,
)

// Строка порога: два filled-поля [порог·ед][%] + свайп-раскрытие «Удалить» (как на Главной).
@Composable
private fun PayTierRow(
    onDelete: () -> Unit,
    closeSignal: Int,
    value1: String,
    onValue1: (String) -> Unit,
    unit1: String,
    value2: String,
    onValue2: (String) -> Unit,
    unit2: String,
) {
    Column {
        SwipeToRevealDelete(
            onDeleteClick = onDelete,
            closeSignal = closeSignal,
            compact = true,
            backgroundVerticalPadding = 0.dp,
        ) { _ ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PayInput(value1, onValue1, unit1, modifier = Modifier.weight(1f))
                PayInput(value2, onValue2, unit2, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
