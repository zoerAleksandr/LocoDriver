package com.z_company.route.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.CalculationEnergy.rounding
import com.z_company.domain.util.str
import kotlinx.coroutines.launch
import com.z_company.domain.util.times
import com.z_company.domain.util.toDoubleOrZero
import com.z_company.route.R
import com.z_company.route.ui.maskInKilo
import com.z_company.route.ui.maskInLiter
import com.z_company.route.viewmodel.DieselSectionFormState
import com.z_company.route.viewmodel.DieselSectionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DieselSectionItem(
    index: Int,
    item: DieselSectionFormState,
    onFuelAcceptedChanged: (Int, String?) -> Unit,
    onFuelDeliveredChanged: (Int, String?) -> Unit,
    onDeleteItem: (DieselSectionFormState) -> Unit,
    focusChangedDieselSection: (Int, DieselSectionType) -> Unit,
    onRefuelValueChanged: (Int, String?) -> Unit,
    onRefuelInKiloValueChanged: (Int, String?) -> Unit,
    onRefuelCoefficientValueChanged: (Int, String?) -> Unit,
    onCoefficientValueChanged: (Int, String?) -> Unit,
    sheetState: SheetState,
    isKiloMode: Boolean,
    changeIsKiloMode: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

//    var isKiloMode by remember { mutableStateOf(false) }
    val coeff = item.coefficient.data?.toDoubleOrNull() ?: 1.0

    val noValueColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    val acceptedInKilo =
        item.accepted.data?.toDoubleOrNull().times(item.coefficient.data?.toDoubleOrNull())
    val deliveryInKilo =
        item.delivery.data?.toDoubleOrNull().times(item.coefficient.data?.toDoubleOrNull())

    val result = CalculationEnergy.getTotalFuelConsumption(
        item.accepted.data?.toDoubleOrNull(),
        item.delivery.data?.toDoubleOrNull(),
        item.refuel.data?.toDoubleOrNull()
    )
    val refuelInKilo = item.refuelInKilo.data?.toDoubleOrNull()

    val resultInKilo = CalculationEnergy.getTotalFuelInKiloConsumption(
        acceptedInKilo, deliveryInKilo, refuelInKilo
    )

    var showCoefficient by remember {
        mutableStateOf(false)
    }

    // Снимок кг на момент открытия шторки коэффициента (для режима «кг»):
    // при смене коэффициента сохраняем кг и пересчитываем литры.
    var kgSnapshotAccepted by remember { mutableStateOf<Double?>(null) }
    var kgSnapshotDelivery by remember { mutableStateOf<Double?>(null) }

    var showSupplyCoefficient by remember {
        mutableStateOf(false)
    }

    var showRefuel by remember {
        mutableStateOf(false)
    }

    // Экипировка развёрнута, если есть данные
    var expandSupply by remember {
        mutableStateOf(
            !item.refuel.data.isNullOrBlank() || !item.refuelInKilo.data.isNullOrBlank()
        )
    }

    if (false && showRefuel) {
        ModalBottomSheet(
            onDismissRequest = {
                showRefuel = false
            },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text(
                        text = "Экипировка секция ${index + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Visible
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .weight(1f),
                                value = item.refuel.data ?: "",
                                onValueChange = {
                                    onRefuelValueChanged(index, it.take(7))
                                },
                                suffix = {
                                    Text(
                                        text = "л.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = noValueColor
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
                                ),
                                borderColor = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .weight(1f),
                                value = item.refuelInKilo.data ?: "",
                                onValueChange = {
                                    onRefuelInKiloValueChanged(index, it.take(7))
                                },
                                suffix = {
                                    Text(
                                        text = "кг.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = noValueColor
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
                                ),
                                borderColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .padding(end = 6.dp),
                            value = item.refuelCoefficient.data ?: "",
                            onValueChange = {
                                onRefuelCoefficientValueChanged(index, it.take(6))
                            },
                            suffix = {
                                Text(
                                    text = "k",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = noValueColor
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
                            ),
                            borderColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 1.dp,
                            pressedElevation = 0.dp
                        ),
                        onClick = {
                            showRefuel = false
                        }
                    ) {
                        Text(
                            text = "Готово",
                            style = MaterialTheme.typography.bodySmall,
                        )

                    }
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }


    if (showCoefficient) {
        CoeffSheet(
            title = "Коэффициент секции",
            hint = "Применяется ко всем расчётам секции",
            value = item.coefficient.data,
            sheetState = sheetState,
            onValueChange = { newVal ->
                onCoefficientValueChanged(index, newVal)
                // В режиме «кг» введённые килограммы остаются неизменными (поле не «прыгает»),
                // а литры пересчитываются вживую — меняется только вспомогательный текст.
                // В режиме «л» литры остаются как ввёл пользователь, кг пересчитываются сами.
                if (isKiloMode) {
                    val newCoeff = newVal?.toDoubleOrNull()
                    if (newCoeff != null && newCoeff != 0.0) {
                        kgSnapshotAccepted?.let {
                            onFuelAcceptedChanged(index, (it / newCoeff).str())
                        }
                        kgSnapshotDelivery?.let {
                            onFuelDeliveredChanged(index, (it / newCoeff).str())
                        }
                    }
                }
            },
            onDismiss = {
                focusChangedDieselSection(index, DieselSectionType.COEFFICIENT)
                showCoefficient = false
            }
        )
    }

    if (showSupplyCoefficient) {
        CoeffSheet(
            title = "Коэффициент экипировки",
            hint = "Может отличаться от k секции",
            value = item.refuelCoefficient.data,
            sheetState = sheetState,
            onValueChange = { onRefuelCoefficientValueChanged(index, it) },
            onDismiss = { showSupplyCoefficient = false }
        )
    }

    val dataTextStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = com.z_company.core.ui.theme.MonoFont)
    val hintStyle = MaterialTheme.typography.bodyMedium

    // Анти-паттерн confirmValueChange для side-effect → dismissState мог застрять.
    // Используем LaunchedEffect + явный snapTo (как в ElectricSectionItem/TrainStationTimeline).
    val dismissState = rememberSwipeToDismissBoxState()
    val currentItem = androidx.compose.runtime.rememberUpdatedState(item)
    val currentOnDelete = androidx.compose.runtime.rememberUpdatedState(onDeleteItem)
    androidx.compose.runtime.LaunchedEffect(dismissState) {
        androidx.compose.runtime.snapshotFlow { dismissState.currentValue }
            .collect { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    currentOnDelete.value(currentItem.value)
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }
    }
    SwipeToDismissBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
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
                    tint = MaterialTheme.colorScheme.surface,
                    contentDescription = null
                )
            }
        }
    ) {
        Card(
            shape = Shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Секция ${index + 1}",
                    style = hintStyle,
                    color = MaterialTheme.colorScheme.primary
                )

                // Пилюля коэффициента секции
                CoeffPill(
                    label = "k секции",
                    value = item.coefficient.data,
                    onClick = {
                        // Снимок введённых кг (до изменения коэффициента)
                        if (isKiloMode) {
                            kgSnapshotAccepted = item.accepted.data?.toDoubleOrNull()?.times(coeff)
                            kgSnapshotDelivery = item.delivery.data?.toDoubleOrNull()?.times(coeff)
                        } else {
                            kgSnapshotAccepted = null
                            kgSnapshotDelivery = null
                        }
                        showCoefficient = true
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ТОПЛИВО",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                UnitSegToggle(isKiloMode = isKiloMode, onToggle = changeIsKiloMode)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                val calculatedAccepted = if (item.accepted.data.isNullOrBlank()) {
                    ""
                } else if (isKiloMode) {
                    item.accepted.data?.toDoubleOrNull()?.let { liters ->
                        rounding(liters * coeff, 2)?.str() ?: ""
                    } ?: ""
                } else {
                    rounding(item.accepted.data?.toDoubleOrZero(), 2).str()
                }
                var localAccepted by remember { mutableStateOf(calculatedAccepted) }

                LaunchedEffect(
                    isKiloMode,
                    item.accepted.data,
                    coeff
                ) {  // Запускается при смене mode/данных/коэффициента, обновляет local для показа пересчитанного значения
                    localAccepted = calculatedAccepted
                }

                OutlinedTextFieldApp(
                    modifier = Modifier
                        .weight(1f),
                    value = localAccepted,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { it.isDigit() || it == '.' }.take(6)
                        if (filtered.isEmpty()) {
                            onFuelAcceptedChanged(index, "")
                            focusChangedDieselSection(index, DieselSectionType.ACCEPTED)
                        }
                        if (filtered.count { it == '.' } > 1) return@OutlinedTextFieldApp  // Запрещаем >1 точки
                        localAccepted = filtered  // Показываем filtered как есть (включая ".")
                        try {
                            val input = filtered.toDouble()  // Если валидно, парсим
                            val liters = if (isKiloMode) {
                                if (coeff != 0.0) (input / coeff).str() else ""
                            } else {
                                rounding(input, 2).str()
                            }
                            onFuelAcceptedChanged(index, liters)  // Сохраняем только валидное
                            focusChangedDieselSection(index, DieselSectionType.ACCEPTED)
                        } catch (e: NumberFormatException) {
                            // Игнорируем invalid (e.g., "."), не меняем VM, только local
                        }
                    },
                    placeholder = {
                        Text(
                            text = "0",
                            style = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Light
                            ),
                            color = noValueColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    suffix = {
                        Text(
                            text = if (isKiloMode) "кг." else "л.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = noValueColor
                        )
                    },
                    // Подсказка-конвертация всегда занимает место (поле не «прыгает»),
                    // но текст виден только когда есть введённое значение.
                    supportingText = {
                        val hasValue = !item.accepted.data.isNullOrBlank() && acceptedInKilo != null
                        val accConv = if (isKiloMode)
                            rounding(item.accepted.data?.toDoubleOrZero(), 2).str()
                        else rounding(acceptedInKilo, 2)?.str()
                        Text(
                            text = if (!hasValue) ""
                            else if (isKiloMode) maskInLiter(accConv) ?: "" else maskInKilo(accConv) ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    colorBackgroundEmptyField = Color.Transparent,
                    colorBackgroundNotEmptyField = Color.Transparent,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        scope.launch {
                            focusManager.moveFocus(FocusDirection.Right)
                        }
                    }),
                    singleLine = true,
                    shape = Shapes.medium,
                )

                val calculatedDelivery = if (item.delivery.data.isNullOrBlank()) {
                    ""
                } else if (isKiloMode) {
                    item.delivery.data?.toDoubleOrNull()?.let { liters ->
                        rounding(liters * coeff, 2)?.str() ?: ""
                    } ?: ""
                } else {
                    rounding(item.delivery.data?.toDoubleOrZero(), 2).str()
                }
                var localDelivery by remember { mutableStateOf(calculatedDelivery) }

                LaunchedEffect(
                    isKiloMode,
                    item.delivery.data,
                    coeff
                ) {  // Запускается при смене mode/данных/коэффициента, обновляет local для показа пересчитанного значения
                    localDelivery = calculatedDelivery
                }

                // Стрелка выровнена по центру поля ввода (без учёта подсказки снизу)
                Box(
                    modifier = Modifier.height(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                OutlinedTextFieldApp(
                    modifier = Modifier
                        .weight(1f),
                    value = localDelivery,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { it.isDigit() || it == '.' }.take(6)
                        if (filtered.isEmpty()) {
                            onFuelDeliveredChanged(index, "")
                            focusChangedDieselSection(index, DieselSectionType.DELIVERY)
                        }
                        if (filtered.count { it == '.' } > 1) return@OutlinedTextFieldApp
                        localDelivery = filtered
                        try {
                            val input = filtered.toDouble()
                            val liters = if (isKiloMode) {
                                if (coeff != 0.0) (input / coeff).str() else ""
                            } else {
                                rounding(input, 2).str()
                            }
                            onFuelDeliveredChanged(index, liters)
                            focusChangedDieselSection(index, DieselSectionType.DELIVERY)
                        } catch (e: NumberFormatException) {
                            // Игнорируем
                        }
                    },
                    placeholder = {
                        Text(
                            text = "0",
                            style = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Light
                            ),
                            color = noValueColor
                        )
                    },
                    suffix = {
                        Text(
                            text = if (isKiloMode) "кг." else "л.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = noValueColor
                        )
                    },
                    supportingText = {
                        val hasValue = !item.delivery.data.isNullOrBlank() && deliveryInKilo != null
                        val delConv = if (isKiloMode)
                            rounding(item.delivery.data?.toDoubleOrZero(), 2).str()
                        else rounding(deliveryInKilo, 2)?.str()
                        Text(
                            text = if (!hasValue) ""
                            else if (isKiloMode) maskInLiter(delConv) ?: "" else maskInKilo(delConv) ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    colorBackgroundEmptyField = Color.Transparent,
                    colorBackgroundNotEmptyField = Color.Transparent,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        scope.launch {
                            focusManager.clearFocus()
                        }
                    }),
                    singleLine = true,
                    shape = Shapes.medium,
                )
            }

            // Экипировка — раскрываемый блок с заголовком и шевроном
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 28.dp) // высота под пилюлю — заголовок не съезжает при разворачивании
                        .noRippleEffect { expandSupply = !expandSupply },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ЭКИПИРОВКА",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // k экипировки — пилюля, открывает шторку (только в развёрнутом виде)
                        if (expandSupply) {
                            CoeffPill(
                                label = "k экипировки",
                                value = item.refuelCoefficient.data,
                                onClick = { showSupplyCoefficient = true }
                            )
                        }
                        Icon(
                            painter = painterResource(
                                if (expandSupply) R.drawable.keyboard_arrow_up_24px
                                else R.drawable.keyboard_arrow_down_24px
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                AnimatedVisibility(expandSupply) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextFieldApp(
                            modifier = Modifier.weight(1f),
                            value = item.refuel.data ?: "",
                            onValueChange = { onRefuelValueChanged(index, it.take(7)) },
                            textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    colorBackgroundEmptyField = Color.Transparent,
                    colorBackgroundNotEmptyField = Color.Transparent,
                            placeholder = {
                                Text(
                                    text = "Объём",
                                    style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                    color = noValueColor
                                )
                            },
                            suffix = {
                                Text(
                                    text = "л.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = noValueColor
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = Shapes.medium,
                        )
                        OutlinedTextFieldApp(
                            modifier = Modifier.weight(1f),
                            value = item.refuelInKilo.data ?: "",
                            onValueChange = { onRefuelInKiloValueChanged(index, it.take(7)) },
                            textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    colorBackgroundEmptyField = Color.Transparent,
                    colorBackgroundNotEmptyField = Color.Transparent,
                            placeholder = {
                                Text(
                                    text = "Масса",
                                    style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                    color = noValueColor
                                )
                            },
                            suffix = {
                                Text(
                                    text = "кг.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = noValueColor
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = Shapes.medium,
                        )
                    }
                }
            }

            result?.let {
                val resultInLiterText = maskInLiter(rounding(it, 2).str())
                val resultInKiloText = maskInKilo(rounding(resultInKilo, 2)?.str())
                val dashColor = MaterialTheme.colorScheme.outlineVariant
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .height(1.dp)
                ) {
                    drawLine(
                        color = dashColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = size.height,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Расход",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${resultInLiterText ?: ""} / ${resultInKiloText ?: ""}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = com.z_company.core.ui.theme.MonoFont
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

        }
    }
}
/** Сегментированный тумблер единиц «л | кг» (как в референсе UnitSeg). */
@Composable
private fun UnitSegToggle(isKiloMode: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceBright,
                RoundedCornerShape(999.dp)
            )
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UnitSegItem(label = "л", active = !isKiloMode) { if (isKiloMode) onToggle() }
        UnitSegItem(label = "кг", active = isKiloMode) { if (!isKiloMode) onToggle() }
    }
}

@Composable
private fun UnitSegItem(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .noRippleEffect { onClick() }
            .background(
                if (active) MaterialTheme.colorScheme.secondary else Color.Transparent,
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont,
                fontWeight = FontWeight.W700
            ),
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Пилюля коэффициента: «k секции 0.83» — открывает шторку по клику. */
@Composable
private fun CoeffPill(label: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .noRippleEffect { onClick() }
            .background(MaterialTheme.colorScheme.surfaceBright, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value ?: "1.0",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Шторка выбора коэффициента: степпер [− value +] + пресеты «из истории». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoeffSheet(
    title: String,
    hint: String,
    value: String?,
    sheetState: SheetState,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val presets = listOf("0.79", "0.83", "0.87", "0.91", "0.95")
    val current = value?.toDoubleOrNull()
    // Шаг зависит от точности введённого значения: 3 знака → 0.001, иначе 0.01
    val decimals = (value ?: "").substringAfter('.', "").length
    val precision = if (decimals >= 3) 3 else 2
    val step = if (decimals >= 3) 0.001 else 0.01
    fun fmt(d: Double): String = rounding(d, precision)?.str() ?: ""

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.secondary,
        dragHandle = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            // Заголовок + Готово
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Готово",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W500),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.noRippleEffect { onDismiss() }
                )
            }
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Степпер
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoeffStepBtn(symbol = "−") {
                    val base = current ?: 1.0
                    onValueChange(fmt((base - step).coerceAtLeast(0.0)))
                }
                BasicTextField(
                    value = value ?: "",
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() || it == '.' }.take(5)
                        if (filtered.count { it == '.' } <= 1) onValueChange(filtered)
                    },
                    modifier = Modifier.width(140.dp).padding(horizontal = 12.dp),
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = com.z_company.core.ui.theme.MonoFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (value.isNullOrEmpty()) {
                                Text(
                                    text = "1.0",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = com.z_company.core.ui.theme.MonoFont,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            }
                            inner()
                        }
                    }
                )
                CoeffStepBtn(symbol = "+") {
                    val base = current ?: 1.0
                    onValueChange(fmt(base + step))
                }
            }

            // Пресеты
            Text(
                text = "ИЗ ИСТОРИИ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = com.z_company.core.ui.theme.MonoFont,
                    fontWeight = FontWeight.W600,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.4f, androidx.compose.ui.unit.TextUnitType.Sp)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { p ->
                    val active = p == value
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .border(
                                1.dp,
                                if (active) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(999.dp)
                            )
                            .background(
                                if (active) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .noRippleEffect { onValueChange(p) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = p,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = com.z_company.core.ui.theme.MonoFont,
                                fontWeight = FontWeight.W600
                            ),
                            color = if (active) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoeffStepBtn(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .noRippleEffect { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
