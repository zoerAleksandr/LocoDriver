package com.z_company.route.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
    showOtherCurrent: Boolean = false
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

    // Анти-паттерн confirmValueChange для side-effect → dismissState мог застрять
    // (красная полоса оставалась видимой). Используем LaunchedEffect + явный snapTo.
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
        modifier = Modifier.fillMaxWidth(),
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1} секция",
                    style = MaterialTheme.typography.bodyMedium
                )

                AnimatedContent(targetState = item.expandItemState, label = "") {
                    Icon(
                        modifier = Modifier.clickable {
                            onExpandStateChanged(
                                !item.expandItemState
                            )
                        },
                        painter = if (it) {
                            painterResource(R.drawable.zoom_in_map_24px)
                        } else {
                            painterResource(R.drawable.zoom_out_map_24px)
                        },
                        contentDescription = null
                    )
                }

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
                    placeholder = {
                        Text(
                            text = "Принял", style = LocalTextStyle.current.copy(
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
                    onValueChange = {
                        onEnergyDeliveryChanged(index, it.take(10))
                        focusChangedElectricSection(index, ElectricSectionType.DELIVERY)
                    },
                    placeholder = {
                        Text(
                            text = "Сдал", style = LocalTextStyle.current.copy(
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.electric_bolt_24px),
                            contentDescription = null
                        )
                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .weight(1f),
                            value = acceptedText2,
                            onValueChange = {
                                onEnergyAcceptedChanged2(index, it.take(10))
                                focusChangedElectricSection(index, ElectricSectionType.ACCEPTED2)
                            },
                            textStyle = dataTextStyle,
                            placeholder = {
                                Text(
                                    text = "Принял", style = LocalTextStyle.current.copy(
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
                        )

                        OutlinedTextFieldApp(
                            modifier = Modifier
                                .weight(1f),
                            value = deliveryText2,
                            textStyle = dataTextStyle,
                            onValueChange = {
                                onEnergyDeliveryChanged2(index, it.take(10))
                                focusChangedElectricSection(index, ElectricSectionType.DELIVERY2)
                            },
                            placeholder = {
                                Text(
                                    text = "Сдал", style = LocalTextStyle.current.copy(
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
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = item.expandItemState, label = ""
            ) { targetState ->
                if (targetState) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "РЕКУПЕРАЦИЯ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                        )
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
                                placeholder = {
                                    Text(
                                        text = "Сдал",
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
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.cycle_24px),
                                    contentDescription = null
                                )
                                OutlinedTextFieldApp(
                                    modifier = Modifier
                                        .weight(0.5f),
                                    value = recoveryAcceptedText2,
                                    onValueChange = {
                                        onRecoveryAcceptedChanged2(index, it.take(10))
                                        focusChangedElectricSection(
                                            index,
                                            ElectricSectionType.RECOVERY_ACCEPTED2
                                        )
                                    },
                                    textStyle = dataTextStyle,
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
                                )

                                OutlinedTextFieldApp(
                                    modifier = Modifier
                                        .weight(0.5f),
                                    value = recoveryDeliveryText2,
                                    onValueChange = {
                                        onRecoveryDeliveryChanged2(index, it.take(10))
                                        focusChangedElectricSection(
                                            index,
                                            ElectricSectionType.RECOVERY_DELIVERY2
                                        )
                                    },
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
                        }
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                visible = item.resultVisibility,
                enter = slideInHorizontally(animationSpec = tween(durationMillis = 300))
                        + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = slideOutHorizontally(animationSpec = tween(durationMillis = 300))
                        + fadeOut(animationSpec = tween(durationMillis = 150)),
                label = ""
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    result?.let {
                        SectionSummaryRow(label = "Расход", value = "${it.str()} кВт·ч")
                    }
                    resultRecovery?.let {
                        SectionSummaryRow(label = "Рекуперация", value = "${it.str()} кВт·ч")
                    }
                    result2?.let {
                        SectionSummaryRow(label = "Расход (ток 2)", value = "${it.str()} кВт·ч")
                    }
                    resultRecovery2?.let {
                        SectionSummaryRow(label = "Рекуперация (ток 2)", value = "${it.str()} кВт·ч")
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