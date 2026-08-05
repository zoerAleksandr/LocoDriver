package com.z_company.route.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.CalculationEnergy.getTotalEnergyConsumption
import com.z_company.domain.util.str
import com.z_company.route.R
import com.z_company.route.viewmodel.ElectricSectionFormState
import com.z_company.route.viewmodel.ElectricSectionType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectricSectionItem(
    index: Int,
    item: ElectricSectionFormState,
    onDeleteItem: (ElectricSectionFormState) -> Unit,
    onEnergyAcceptedChanged: (Int, String?) -> Unit,
    onEnergyDeliveryChanged: (Int, String?) -> Unit,
    onRecoveryAcceptedChanged: (Int, String?) -> Unit,
    onRecoveryDeliveryChanged: (Int, String?) -> Unit,
    onEnergyAcceptedChanged2: (Int, String?) -> Unit,
    onEnergyDeliveryChanged2: (Int, String?) -> Unit,
    onRecoveryAcceptedChanged2: (Int, String?) -> Unit,
    onRecoveryDeliveryChanged2: (Int, String?) -> Unit,
    focusChangedElectricSection: (Int, ElectricSectionType) -> Unit,
    onExpandStateChanged: (Boolean) -> Unit,
    showOtherCurrent: Boolean = false,
    closeSwipeSignal: Int = 0
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val acceptedText = item.accepted.data ?: ""
    val deliveryText = item.delivery.data ?: ""
    val recoveryAcceptedText = item.recoveryAccepted.data ?: ""
    val recoveryDeliveryText = item.recoveryDelivery.data ?: ""

    fun decimalPlaces(vararg texts: String): Int {
        return texts.maxOf { s ->
            val dot = s.indexOf('.')
            if (dot < 0) 0 else s.length - dot - 1
        }
    }

    val energyPrecision = decimalPlaces(acceptedText, deliveryText)
    val recoveryPrecision = decimalPlaces(recoveryAcceptedText, recoveryDeliveryText)

    val result = CalculationEnergy.rounding(
        getTotalEnergyConsumption(
            item.accepted.data?.toDoubleOrNull(),
            item.delivery.data?.toDoubleOrNull()
        ), energyPrecision
    )
    val resultRecovery = CalculationEnergy.rounding(
        getTotalEnergyConsumption(
            item.recoveryAccepted.data?.toDoubleOrNull(),
            item.recoveryDelivery.data?.toDoubleOrNull()
        ), recoveryPrecision
    )

    val acceptedText2 = item.accepted2.data ?: ""
    val deliveryText2 = item.delivery2.data ?: ""
    val recoveryAcceptedText2 = item.recoveryAccepted2.data ?: ""
    val recoveryDeliveryText2 = item.recoveryDelivery2.data ?: ""

    val energyPrecision2 = decimalPlaces(acceptedText2, deliveryText2)
    val recoveryPrecision2 = decimalPlaces(recoveryAcceptedText2, recoveryDeliveryText2)

    val result2 = CalculationEnergy.rounding(
        getTotalEnergyConsumption(
            item.accepted2.data?.toDoubleOrNull(),
            item.delivery2.data?.toDoubleOrNull()
        ), energyPrecision2
    )
    val resultRecovery2 = CalculationEnergy.rounding(
        getTotalEnergyConsumption(
            item.recoveryAccepted2.data?.toDoubleOrNull(),
            item.recoveryDelivery2.data?.toDoubleOrNull()
        ), recoveryPrecision2
    )

    val dataTextStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = com.z_company.core.ui.theme.MonoFont)
    val hintStyle = MaterialTheme.typography.bodyMedium

    val noValueColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    // Раскрытие рекуперации — локальное состояние (без обновления всего списка
    // через VM, иначе пересоздание всех секций вызывает моргание).
    var expandRecovery by androidx.compose.runtime.remember(item.sectionId) {
        androidx.compose.runtime.mutableStateOf(item.expandItemState)
    }

    SwipeToRevealDelete(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        onDeleteClick = { onDeleteItem(item) },
        closeSignal = closeSwipeSignal
    ) { _ ->
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Секция ${index + 1}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600)
                )
            }
            // РАСХОД — лейбл + Принял → Сдал
            Text(
                text = "РАСХОД",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextFieldApp(
                    modifier = Modifier
                        .weight(1f),
                    value = acceptedText,
                    onValueChange = {
                        onEnergyAcceptedChanged(index, it.take(10))
                        focusChangedElectricSection(index, ElectricSectionType.ACCEPTED)
                    },
                    textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = Color.Transparent,
                    colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    placeholder = {
                        Text(
                            text = if (showOtherCurrent) "Ток 1 принял" else "Принял",
                            style = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Light
                            ), color = noValueColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
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

                Text(
                    text = "→",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedTextFieldApp(
                    modifier = Modifier
                        .weight(1f),
                    value = deliveryText,
                    textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = Color.Transparent,
                    colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    onValueChange = {
                        onEnergyDeliveryChanged(index, it.take(10))
                        focusChangedElectricSection(index, ElectricSectionType.DELIVERY)
                    },
                    placeholder = {
                        Text(
                            text = if (showOtherCurrent) "Ток 1 сдал" else "Сдал",
                            style = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Light
                            ), color = noValueColor
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
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

            // счетчики расхода ток 2
            AnimatedContent(
                targetState = showOtherCurrent, label = ""
            ) { targetState ->
                if (targetState) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .weight(1f),
                            value = acceptedText2,
                            onValueChange = {
                                onEnergyAcceptedChanged2(index, it.take(10))
                                focusChangedElectricSection(index, ElectricSectionType.ACCEPTED2)
                            },
                            textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = Color.Transparent,
                    colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            placeholder = {
                                Text(
                                    text = "Ток 2 принял", style = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Light
                                    ), color = noValueColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
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

                        Text(
                            text = "→",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .weight(1f),
                            value = deliveryText2,
                            textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = Color.Transparent,
                    colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            onValueChange = {
                                onEnergyDeliveryChanged2(index, it.take(10))
                                focusChangedElectricSection(index, ElectricSectionType.DELIVERY2)
                            },
                            placeholder = {
                                Text(
                                    text = "Ток 2 сдал", style = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Light
                                    ), color = noValueColor
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
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
                }
            }

            // Рекуперация — раскрываемый блок с заголовком и шевроном
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 28.dp)
                        .padding(horizontal = 16.dp)
                        .noRippleEffect { expandRecovery = !expandRecovery },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "РЕКУПЕРАЦИЯ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        painter = painterResource(
                            if (expandRecovery) R.drawable.keyboard_arrow_up_24px
                            else R.drawable.keyboard_arrow_down_24px
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                AnimatedVisibility(expandRecovery) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .weight(1f),
                                value = recoveryAcceptedText,
                                onValueChange = {
                                    onRecoveryAcceptedChanged(index, it.take(10))
                                    focusChangedElectricSection(
                                        index,
                                        ElectricSectionType.RECOVERY_ACCEPTED
                                    )
                                },
                                textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = Color.Transparent,
                    colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                placeholder = {
                                    Text(
                                        text = if (showOtherCurrent) "Ток 1 принял" else "Принял",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = {
                                    scope.launch {
                                        focusManager.moveFocus(FocusDirection.Right)
                                    }
                                }),
                                singleLine = true,
                                shape = Shapes.medium,
                            )

                            Text(
                                text = "→",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .weight(1f),
                                value = recoveryDeliveryText,
                                onValueChange = {
                                    onRecoveryDeliveryChanged(index, it.take(10))
                                    focusChangedElectricSection(
                                        index,
                                        ElectricSectionType.RECOVERY_DELIVERY
                                    )
                                },
                                textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = Color.Transparent,
                    colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                placeholder = {
                                    Text(
                                        text = if (showOtherCurrent) "Ток 1 сдал" else "Сдал",
                                        style = LocalTextStyle.current.copy(
                                            fontWeight = FontWeight.Light
                                        ),
                                        color = noValueColor
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    scope.launch {
                                        focusManager.clearFocus()
                                    }
                                }),

                                singleLine = true,
                            )
                        }

                        // рекуперация 2
                        AnimatedVisibility(visible = showOtherCurrent) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextFieldApp(
                                    modifier = Modifier
                                        .weight(1f),
                                    value = recoveryAcceptedText2,
                                    onValueChange = {
                                        onRecoveryAcceptedChanged2(index, it.take(10))
                                        focusChangedElectricSection(
                                            index,
                                            ElectricSectionType.RECOVERY_ACCEPTED2
                                        )
                                    },
                                    textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = Color.Transparent,
                    colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                    placeholder = {
                                        Text(
                                            text = "Ток 2 принял",
                                            style = LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            ),
                                            color = noValueColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onNext = {
                                        scope.launch {
                                            focusManager.moveFocus(FocusDirection.Right)
                                        }
                                    }),
                                    singleLine = true,
                                    shape = Shapes.medium,
                                )

                                Text(
                                    text = "→",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                                OutlinedTextFieldApp(
                                    modifier = Modifier
                                        .weight(1f),
                                    value = recoveryDeliveryText2,
                                    onValueChange = {
                                        onRecoveryDeliveryChanged2(index, it.take(10))
                                        focusChangedElectricSection(
                                            index,
                                            ElectricSectionType.RECOVERY_DELIVERY2
                                        )
                                    },
                                    textStyle = dataTextStyle,
                    fieldElevation = 0.dp,
                    borderColor = Color.Transparent,
                    colorBackgroundEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    colorBackgroundNotEmptyField = MaterialTheme.colorScheme.surfaceBright,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                    placeholder = {
                                        Text(
                                            text = "Ток 2 сдал",
                                            style = LocalTextStyle.current.copy(
                                                fontWeight = FontWeight.Light
                                            ),
                                            color = noValueColor
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
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
                        }
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                visible = true,
                enter = slideInHorizontally(animationSpec = tween(durationMillis = 300))
                        + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = slideOutHorizontally(animationSpec = tween(durationMillis = 300))
                        + fadeOut(animationSpec = tween(durationMillis = 150)),
                label = ""
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                    SectionSummaryRow(label = "Расход", value = "${(result ?: 0.0).str()}")
                    resultRecovery?.let {
                        SectionSummaryRow(label = "Рекуперация", value = "${it.str()}")
                    }
                    result2?.let {
                        SectionSummaryRow(label = "Расход (ток 2)", value = "${it.str()}")
                    }
                    resultRecovery2?.let {
                        SectionSummaryRow(label = "Рекуперация (ток 2)", value = "${it.str()}")
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}