package com.z_company.route.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.ClickableText
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
        ModalBottomSheet(
            onDismissRequest = { showCoefficient = false },
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
                        text = "Коэффициент секция ${index + 1}",
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
                    OutlinedTextFieldApp(
                        modifier = Modifier
                            .padding(end = 4.dp, top = 24.dp),
                        value = item.coefficient.data ?: "",
                        onValueChange = {
                            onCoefficientValueChanged(index, it.take(6))
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
                            scope.launch {
                                focusChangedDieselSection(index, DieselSectionType.COEFFICIENT)
                                showCoefficient = false
                            }
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Пилюля коэффициента секции
                    Row(
                        modifier = Modifier
                            .noRippleEffect { showCoefficient = true }
                            .background(
                                MaterialTheme.colorScheme.surfaceBright,
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "k",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.coefficient.data ?: "1.0",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = com.z_company.core.ui.theme.MonoFont
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                val calculatedAccepted = if (isKiloMode) {
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
                                if (coeff != 0.0) rounding(input / coeff, 2)?.str() ?: "" else ""
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
                            text = "Принял",
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
                    supportingText = if (acceptedInKilo != null) {
                        {
                            val accConv = if (isKiloMode)
                                rounding(item.accepted.data?.toDoubleOrZero(), 2).str()
                            else rounding(acceptedInKilo, 2)?.str()
                            Text(
                                text = if (isKiloMode) maskInLiter(accConv) ?: "" else maskInKilo(accConv) ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else null,
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

                val calculatedDelivery = if (isKiloMode) {
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

                Text(
                    text = "→",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )

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
                                if (coeff != 0.0) rounding(input / coeff, 2)?.str() ?: "" else ""
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
                            text = "Сдал",
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
                    supportingText = if (deliveryInKilo != null) {
                        {
                            val delConv = if (isKiloMode)
                                rounding(item.delivery.data?.toDoubleOrZero(), 2).str()
                            else rounding(deliveryInKilo, 2)?.str()
                            Text(
                                text = if (isKiloMode) maskInLiter(delConv) ?: "" else maskInKilo(delConv) ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else null,
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

            // Экипировка — инлайн (вместо шторки)
            if (expandSupply) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ЭКИПИРОВКА",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // k экипировки — компактное поле
                        OutlinedTextFieldApp(
                            modifier = Modifier.width(90.dp),
                            value = item.refuelCoefficient.data ?: "",
                            onValueChange = { onRefuelCoefficientValueChanged(index, it.take(6)) },
                            textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    colorBackgroundEmptyField = Color.Transparent,
                    colorBackgroundNotEmptyField = Color.Transparent,
                            placeholder = {
                                Text(
                                    text = "k экип.",
                                    style = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                    color = noValueColor
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = Shapes.medium,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
            } else {
                Text(
                    text = "+ Экипировка",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .noRippleEffect { expandSupply = true }
                        .padding(start = 16.dp, top = 14.dp)
                )
            }

        }
    }

    result?.let {
        val resultInLiterText = maskInLiter(rounding(it, 2).str())
        val resultInKiloText = maskInKilo(rounding(resultInKilo, 2)?.str())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Расход",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${resultInLiterText ?: ""} л / ${resultInKiloText ?: ""} кг",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = com.z_company.core.ui.theme.MonoFont
                ),
                color = MaterialTheme.colorScheme.primary
            )
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
