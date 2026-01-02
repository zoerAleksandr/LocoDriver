package com.z_company.route.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.z_company.route.component.OutlinedTextFieldApp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.util.CalculationEnergy.getTotalEnergyConsumption
import com.z_company.route.R
import com.z_company.route.viewmodel.ElectricSectionFormState
import com.z_company.route.viewmodel.ElectricSectionType
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe
import de.charlex.compose.RevealValue
import de.charlex.compose.rememberRevealState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
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
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val revealState = rememberRevealState()

    val acceptedText = item.accepted.data ?: ""
    val deliveryText = item.delivery.data ?: ""
    val recoveryAcceptedText = item.recoveryAccepted.data ?: ""
    val recoveryDeliveryText = item.recoveryDelivery.data ?: ""
    val result = getTotalEnergyConsumption(
        item.accepted.data?.toBigDecimalOrNull(),
        item.delivery.data?.toBigDecimalOrNull()
    )
    val resultRecovery = getTotalEnergyConsumption(
        item.recoveryAccepted.data?.toBigDecimalOrNull(),
        item.recoveryDelivery.data?.toBigDecimalOrNull()
    )

    val acceptedText2 = item.accepted2.data ?: ""
    val deliveryText2 = item.delivery2.data ?: ""
    val recoveryAcceptedText2 = item.recoveryAccepted2.data ?: ""
    val recoveryDeliveryText2 = item.recoveryDelivery2.data ?: ""
    val result2 = getTotalEnergyConsumption(
        item.accepted2.data?.toBigDecimalOrNull(),
        item.delivery2.data?.toBigDecimalOrNull()
    )
    val resultRecovery2 = getTotalEnergyConsumption(
        item.recoveryAccepted2.data?.toBigDecimalOrNull(),
        item.recoveryDelivery2.data?.toBigDecimalOrNull()
    )

    val dataTextStyle = MaterialTheme.typography.bodyLarge
    val hintStyle = MaterialTheme.typography.bodyMedium

    val noValueColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    RevealSwipe(
        modifier = Modifier
            .fillMaxWidth(),
        state = revealState,
        directions = setOf(
            RevealDirection.EndToStart
        ),
        hiddenContentEnd = {
            IconButton(onClick = {
                onDeleteItem(item)
                scope.launch {
                    revealState.animateTo(RevealValue.Default)
                }
            }) {
                Icon(
                    modifier = Modifier.padding(end = 15.dp),
                    imageVector = Icons.Outlined.Delete,
                    tint = Color.White,
                    contentDescription = null
                )
            }
        },
        backgroundCardEndColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
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
            // счетчики расхода ток 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painter = painterResource(R.drawable.electric_bolt_24px), contentDescription = null)

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
                        Text(text = "Принял", style = LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Light
                        ), color = noValueColor)
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
                        Text(text = "Сдал", style = LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Light
                        ), color = noValueColor)
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
                        Icon(painter = painterResource(R.drawable.electric_bolt_24px), contentDescription = null)
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
                                Text(text = "Принял", style = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight.Light
                                ), color = noValueColor)
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
                                Text(text = "Сдал", style = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight.Light
                                ), color = noValueColor)
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
                        Row(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painter = painterResource(R.drawable.cycle_24px), contentDescription = null)
                            OutlinedTextFieldApp(
                                modifier = Modifier
                                    .weight(0.5f),
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
                                        color = noValueColor
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
                                Icon(painter = painterResource(R.drawable.cycle_24px), contentDescription = null)
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
                                            color = noValueColor
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
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        result?.let {
                            Text(text = it.toPlainString(), style = hintStyle)
                        }
                        resultRecovery?.let {
                            Text(text = " / ${it.toPlainString()}", style = hintStyle)
                        }

                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        result2?.let {
                            Text(text = it.toPlainString(), style = hintStyle)
                        }
                        resultRecovery2?.let {
                            Text(text = " / ${it.toPlainString()}", style = hintStyle)
                        }

                    }
                }
            }
        }
    }
}