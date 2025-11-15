package com.z_company.route.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
import com.z_company.route.R
import com.z_company.route.ui.maskInKilo
import com.z_company.route.ui.maskInLiter
import com.z_company.route.viewmodel.DieselSectionFormState
import com.z_company.route.viewmodel.DieselSectionType
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe
import de.charlex.compose.RevealValue
import de.charlex.compose.rememberRevealState

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
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
    sheetState: SheetState
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val revealState = rememberRevealState()

    val acceptedText = item.accepted.data ?: ""
    val deliveryText = item.delivery.data ?: ""
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
                                    onRefuelValueChanged(index, it.take(6))
                                },
                                suffix = {
                                    Text(text = "л.", style = MaterialTheme.typography.bodyMedium)
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
                                    onRefuelInKiloValueChanged(index, it.take(6))
                                },
                                suffix = {
                                    Text(text = "кг.", style = MaterialTheme.typography.bodyMedium)
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
                                Text(text = "k", style = MaterialTheme.typography.bodyMedium)
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
                            defaultElevation = 3.dp,
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
                            Text(text = "k", style = MaterialTheme.typography.bodyMedium)
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
                            defaultElevation = 3.dp,
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

    RevealSwipe(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
        state = revealState,
        directions = setOf(
            RevealDirection.EndToStart
        ),
        hiddenContentEnd = {
            IconButton(
                onClick = {
                    onDeleteItem(item)
                    scope.launch {
                        revealState.animateTo(RevealValue.Default)
                    }
                }
            ) {
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
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextFieldApp(
                    modifier = Modifier
                        .weight(1f),
                    value = acceptedText,
                    onValueChange = {
                        onFuelAcceptedChanged(index, it.take(5))
                        focusChangedDieselSection(index, DieselSectionType.ACCEPTED)
                    },
                    placeholder = {
                        Text(text = "Принято", style = hintStyle)
                    },
                    suffix = {
                        Text(text = "л.", style = MaterialTheme.typography.bodyMedium)
                    },
                    textStyle = dataTextStyle,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Next
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
                    onValueChange = {
                        onFuelDeliveredChanged(index, it.take(5))
                        focusChangedDieselSection(index, DieselSectionType.DELIVERY)
                    },
                    placeholder = {
                        Text(text = "Сдано", style = hintStyle)
                    },
                    suffix = {
                        Text(text = "л.", style = MaterialTheme.typography.bodyMedium)
                    },
                    textStyle = dataTextStyle,
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
                        val acceptedInKiloText = rounding(acceptedInKilo, 2)?.str()
                        Text(
                            text = maskInKilo(acceptedInKiloText) ?: "",
                            style = hintStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f),
                        contentAlignment = Alignment.TopStart
                    ) {
                        val deliveryInKiloText = rounding(deliveryInKilo, 2)?.str()
                        Text(
                            text = maskInKilo(deliveryInKiloText) ?: "",
                            style = hintStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                    val resultInLiterText = maskInLiter(it.str())
                    val resultInKiloText = maskInKilo(rounding(resultInKilo, 2)?.str())
                    Text(
                        text = "${resultInLiterText ?: ""} / ${resultInKiloText ?: ""}",
                        style = hintStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}