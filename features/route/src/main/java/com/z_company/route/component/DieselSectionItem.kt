package com.z_company.route.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DieselSectionItem(
    index: Int,
    item: DieselSectionFormState,
    isShowRefuelDialog: Pair<Boolean, Int>,
    isShowCoefficientDialog: Pair<Boolean, Int>,
    onFuelAcceptedChanged: (Int, String?) -> Unit,
    onFuelDeliveredChanged: (Int, String?) -> Unit,
    onDeleteItem: (DieselSectionFormState) -> Unit,
    focusChangedDieselSection: (Int, DieselSectionType) -> Unit,
    showRefuelDialog: (Pair<Boolean, Int>) -> Unit,
    onRefuelValueChanged: (Int, String?) -> Unit,
    showCoefficientDialog: (Pair<Boolean, Int>) -> Unit,
    onCoefficientValueChanged: (Int, String?) -> Unit
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
    val resultInKilo = CalculationEnergy.getTotalFuelInKiloConsumption(
        result,
        item.coefficient.data?.toDoubleOrNull()
    )

    if (isShowRefuelDialog.first) {
        EnteredRefuelDialog(
            index = isShowRefuelDialog.second,
            refuelValue = item.refuel.data,
            showDialog = showRefuelDialog,
            onSaveClick = onRefuelValueChanged,
            focusChangedDieselSection = focusChangedDieselSection
        )
    }

    if (isShowCoefficientDialog.first) {
        EnteredCoefficientDialog(
            index = isShowCoefficientDialog.second,
            coefficientValue = item.coefficient.data,
            showDialog = showCoefficientDialog,
            onSaveClick = onCoefficientValueChanged,
            focusChangedDieselSection = focusChangedDieselSection
        )
    }
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
        shape = Shapes.medium
    ) {
        Card(
            border = BorderStroke(width = 0.5.dp, color = MaterialTheme.colorScheme.outline),
            shape = Shapes.medium,
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
                    style = subTitleTextStyle
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.refuel.data?.toDoubleOrNull()?.let {
                        Text(
                            modifier = Modifier.padding(end = 8.dp),
                            text = maskInLiter(it.str()) ?: "",
                            style = dataTextStyle,
                        )
                    }

                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                showRefuelDialog(Pair(true, index))
                            },
                        painter = painterResource(id = R.drawable.fuel_pump),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                    )


                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f),
                    value = acceptedText,
                    onValueChange = {
                        onFuelAcceptedChanged(index, it.take(5))
                        focusChangedDieselSection(index, DieselSectionType.ACCEPTED)
                    },
                    placeholder = {
                        Text(text = "Принято", style = dataTextStyle)
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
                    onValueChange = {
                        onFuelDeliveredChanged(index, it.take(5))
                        focusChangedDieselSection(index, DieselSectionType.DELIVERY)
                    },
                    placeholder = {
                        Text(text = "Сдано", style = dataTextStyle)
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
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = Shapes.medium,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.TopStart
                ) {
                    val acceptedInKiloText = rounding(acceptedInKilo, 2)?.str()
                    Text(
                        text = maskInKilo(acceptedInKiloText) ?: "",
                        style = hintStyle
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.TopStart
                ) {
                    val deliveryInKiloText = rounding(deliveryInKilo, 2)?.str()
                    Text(
                        text = maskInKilo(deliveryInKiloText) ?: "",
                        style = hintStyle
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ClickableText(
                    text = AnnotatedString(
                        text = "k = ${item.coefficient.data ?: 0.0}",
                        spanStyle = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = hintStyle.fontFamily,
                            fontSize = hintStyle.fontSize,
                            fontWeight = hintStyle.fontWeight
                        )
                    ),
                    style = AppTypography.getType().bodyLarge,
                    onClick = {
                        showCoefficientDialog(Pair(true, index))
                    })
                result?.let {
                    val resultInLiterText = maskInLiter(it.str())
                    val resultInKiloText = maskInKilo(rounding(resultInKilo, 2)?.str())
                    Text(
                        text = "${resultInLiterText ?: ""} / ${resultInKiloText ?: ""}",
                        style = hintStyle
                    )
                }
            }
        }
    }
}