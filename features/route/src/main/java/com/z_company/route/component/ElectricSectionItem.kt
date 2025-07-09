package com.z_company.route.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
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
    focusChangedElectricSection: (Int, ElectricSectionType) -> Unit,
    onExpandStateChanged: (Boolean) -> Unit
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

    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

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
        shape = Shapes.medium
    ) {
        Card(
            border = BorderStroke(width = 0.5.dp, color = MaterialTheme.colorScheme.outline),
            shape = Shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${index + 1} секция",
                    style = subTitleTextStyle
                )

                AnimatedContent(targetState = item.expandItemState, label = "") {
                    Icon(
                        modifier = Modifier.clickable {
                            onExpandStateChanged(
                                !item.expandItemState
                            )
                        },
                        painter = if (it) {
                            painterResource(R.drawable.close_fullscreen_24px)
                        } else {
                            painterResource(R.drawable.open_in_full_24px)
                        },
                        contentDescription = null
                    )
                }

            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f),
                    value = acceptedText,
                    onValueChange = {
                        onEnergyAcceptedChanged(index, it.take(10))
                        focusChangedElectricSection(index, ElectricSectionType.ACCEPTED)
                    },
                    textStyle = dataTextStyle,
                    placeholder = {
                        Text(text = "Принято", style = dataTextStyle)
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
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = Shapes.medium,
                )

                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f),
                    value = deliveryText,
                    textStyle = dataTextStyle,
                    onValueChange = {
                        onEnergyDeliveryChanged(index, it.take(10))
                        focusChangedElectricSection(index, ElectricSectionType.DELIVERY)
                    },
                    placeholder = {
                        Text(text = "Сдано", style = dataTextStyle)
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
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = Shapes.medium,
                )
            }

            AnimatedContent(
                targetState = item.expandItemState, label = ""
            ) { targetState ->
                if (targetState) {
                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
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
                                    text = "Принято",
                                    style = dataTextStyle
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ), keyboardActions = KeyboardActions(onNext = {
                                scope.launch {
                                    focusManager.moveFocus(FocusDirection.Right)
                                }
                            }),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = Shapes.medium,
                        )

                        OutlinedTextField(
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
                                    text = "Сдано",
                                    style = dataTextStyle
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
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = Shapes.medium,
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                visible = item.resultVisibility,
                enter = slideInHorizontally(animationSpec = tween(durationMillis = 300))
                        + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = slideOutHorizontally(animationSpec = tween(durationMillis = 300))
                        + fadeOut(animationSpec = tween(durationMillis = 150)),
                label = ""
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
            }
        }
    }
}