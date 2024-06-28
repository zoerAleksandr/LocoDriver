package com.z_company.route.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.str
import com.z_company.route.R
import com.z_company.route.viewmodel.ElectricSectionFormState
import com.z_company.route.viewmodel.ElectricSectionType
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe
import de.charlex.compose.RevealValue
import de.charlex.compose.rememberRevealState
import kotlinx.coroutines.launch
import com.z_company.domain.util.minus
import java.math.BigInteger

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ElectricSectionItem(
    index: Int,
    item: ElectricSectionFormState,
    onDeleteItem: (ElectricSectionFormState) -> Unit,
    onEnergyAcceptedChanged: (Int, Int?) -> Unit,
    onEnergyDeliveryChanged: (Int, Int?) -> Unit,
    onRecoveryAcceptedChanged: (Int, Int?) -> Unit,
    onRecoveryDeliveryChanged: (Int, Int?) -> Unit,
    focusChangedElectricSection: (Int, ElectricSectionType) -> Unit,
    onExpandStateChanged: (Int, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val revealState = rememberRevealState()

    val acceptedText = item.accepted.data.str()
    val deliveryText = item.delivery.data.str()
    val recoveryAcceptedText = item.recoveryAccepted.data.str()
    val recoveryDeliveryText = item.recoveryDelivery.data.str()
    val result = item.delivery.data - item.accepted.data
    val resultRecovery = item.recoveryDelivery.data - item.recoveryAccepted.data

    RevealSwipe(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
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
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = Shapes.extraSmall
                ),
            shape = Shapes.extraSmall
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 16.dp),
                    text = "${index + 1} секция",
                    style = AppTypography.getType().bodyLarge
                )

                IconButton(
                    modifier = Modifier,
                    onClick = {
                        onExpandStateChanged(index, !item.expandItemState)
                    }
                ) {
                    AnimatedContent(targetState = item.expandItemState, label = "") {
                        Icon(
                            painter = if (it) {
                                painterResource(R.drawable.close_fullscreen_24px)
                            } else {
                                painterResource(R.drawable.open_in_full_24px)
                            },
                            contentDescription = null
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
                    value = acceptedText,
                    onValueChange = {
                        onEnergyAcceptedChanged(index, it.take(10).toIntOrNull())
                        focusChangedElectricSection(index, ElectricSectionType.ACCEPTED)
                    },
                    textStyle = AppTypography.getType().bodyLarge,
                    placeholder = {
                        Text(text = "Принято", color = MaterialTheme.colorScheme.secondary)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        scope.launch {
                            focusManager.moveFocus(FocusDirection.Right)
                        }
                    })
                )

                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    value = deliveryText,
                    textStyle = AppTypography.getType().bodyLarge,
                    onValueChange = {
                        onEnergyDeliveryChanged(index, it.take(10).toIntOrNull())
                        focusChangedElectricSection(index, ElectricSectionType.DELIVERY)
                    },
                    placeholder = {
                        Text(text = "Сдано", color = MaterialTheme.colorScheme.secondary)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        scope.launch {
                            focusManager.clearFocus()
                        }
                    })
                )
            }

            AnimatedContent(
                targetState = item.expandItemState, label = ""
            ) { targetState ->
                if (targetState) {
                    Row(
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .weight(0.5f),
                            value = recoveryAcceptedText,
                            onValueChange = {
                                onRecoveryAcceptedChanged(index, it.take(10).toIntOrNull())
                                focusChangedElectricSection(
                                    index,
                                    ElectricSectionType.RECOVERY_ACCEPTED
                                )
                            },
                            textStyle = AppTypography.getType().bodyLarge,
                            placeholder = {
                                Text(
                                    text = "Принято",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ), keyboardActions = KeyboardActions(onNext = {
                                scope.launch {
                                    focusManager.moveFocus(FocusDirection.Right)
                                }
                            })
                        )

                        OutlinedTextField(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .weight(0.5f),
                            value = recoveryDeliveryText,
                            onValueChange = {
                                onRecoveryDeliveryChanged(index, it.take(10).toIntOrNull())
                                focusChangedElectricSection(
                                    index,
                                    ElectricSectionType.RECOVERY_DELIVERY
                                )
                            },
                            textStyle = AppTypography.getType().bodyLarge,
                            placeholder = {
                                Text(
                                    text = "Сдано",
                                    color = MaterialTheme.colorScheme.secondary
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
                            })
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 16.dp),
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
                        Text(text = it.str())
                    }
                    resultRecovery?.let {
                        Text(text = " / ${it.str()}")
                    }

                }
            }
        }
    }
}