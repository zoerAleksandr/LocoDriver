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

    if (showRefuel) {
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
                            defaultElevation = 2.dp,
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
                            defaultElevation = 2.dp,
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

    val dataTextStyle = MaterialTheme.typography.bodyLarge
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
            .padding(6.dp),
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
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${index + 1} секция",
                    style = hintStyle,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.noRippleEffect {
                        showRefuel = true
                    },
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.refuelInKilo.data?.toDoubleOrNull()?.let {
                        Text(
                            modifier = Modifier.padding(end = 8.dp),
                            text = maskInKilo(it.str()) ?: "",
                            style = dataTextStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(
                        modifier = Modifier
                            .size(24.dp),
                        painter = painterResource(id = R.drawable.fuel_pump),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        contentDescription = null,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    textStyle = dataTextStyle,
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
                    textStyle = dataTextStyle,
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
                val animatedRotation by animateFloatAsState(
                    targetValue = if (isKiloMode) 180f else 0f,
                    animationSpec =
                        tween(
                            durationMillis = 300,  // Длительность анимации 300ms
                            easing = FastOutSlowInEasing  // Мягкий старт и финиш
                        ),
//                                + spring(  // Добавляем spring для отскока в конце (мягкий bounce)
//                        dampingRatio = Spring.DampingRatioLowBouncy,  // Низкий damping для заметного отскока
//                        stiffness = Spring.StiffnessMedium  // Средняя жёсткость для контроля
//                    ),
                    label = "rotation_animation"
                )

                Icon(
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            rotationX = animatedRotation
                        }  // Добавлено: анимация переворота на 180° по Y
                        .noRippleEffect { changeIsKiloMode() },
                    painter = painterResource(R.drawable.sync_24px),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
            }

            AnimatedVisibility(acceptedInKilo != null || deliveryInKilo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f),
                        contentAlignment = Alignment.TopStart
                    ) {
                        val acceptedText = if (isKiloMode) rounding(
                            item.accepted.data?.toDoubleOrZero(),
                            2
                        ).str() else rounding(acceptedInKilo, 2)?.str()
                        Text(
                            text = if (isKiloMode) maskInLiter(acceptedText) ?: "" else maskInKilo(
                                acceptedText
                            ) ?: "",
                            style = hintStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
//                            .padding(start = 12.dp)
                            .weight(1f),
                        contentAlignment = Alignment.TopStart
                    ) {
                        val deliveryText = if (isKiloMode)
                            rounding(item.delivery.data?.toDoubleOrZero(), 2).str()
                        else rounding(deliveryInKilo, 2)?.str()
                        Text(
                            text = if (isKiloMode) maskInLiter(deliveryText) ?: "" else maskInKilo(
                                deliveryText
                            ) ?: "",
                            style = hintStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ClickableText(
            text = AnnotatedString(
                text = "k = ${item.coefficient.data ?: 0.0}",
                spanStyle = SpanStyle(
                    color = MaterialTheme.colorScheme.tertiary,
                    fontFamily = hintStyle.fontFamily,
                    fontSize = hintStyle.fontSize,
                    fontWeight = hintStyle.fontWeight
                )
            ),
            style = hintStyle,
            onClick = {
                showCoefficient = true
            })

        result?.let {
            val resultInLiterText = maskInLiter(rounding(it, 2).str())
            val resultInKiloText = maskInKilo(rounding(resultInKilo, 2)?.str())
            Text(
                text = "${resultInLiterText ?: ""} / ${resultInKiloText ?: ""}",
                style = hintStyle,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}