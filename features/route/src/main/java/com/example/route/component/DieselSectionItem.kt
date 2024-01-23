package com.example.route.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import com.example.core.ui.theme.Shapes
import com.example.core.ui.theme.custom.AppTypography
import com.example.domain.entities.route.SectionDiesel
import com.example.domain.util.CalculationEnergy
import com.example.domain.util.CalculationEnergy.rounding
import com.example.domain.util.str
import kotlinx.coroutines.launch
import com.example.domain.util.times
import com.example.route.R
import com.example.route.ui.maskInKilo
import com.example.core.R as CoreR
import com.example.route.ui.maskInLiter
import de.charlex.compose.RevealDirection
import de.charlex.compose.RevealSwipe
import de.charlex.compose.RevealValue
import de.charlex.compose.rememberRevealState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DieselSectionItem(
    index: Int,
    item: SectionDiesel,
    showRefuelDialog: (Double?) -> Unit,
    showCoefficientDialog: (Double?) -> Unit,
    onFuelAcceptedChanged: (SectionDiesel, String) -> Unit,
    onFuelDeliveredChanged: (SectionDiesel, String) -> Unit,
    onDeleteItem: (SectionDiesel) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val revealState = rememberRevealState()

    val acceptedText = item.acceptedFuel?.str() ?: ""
    val deliveryText = item.deliveryFuel?.str() ?: ""
    val acceptedInKilo = item.acceptedFuel.times(item.coefficient)
    val deliveryInKilo = item.deliveryFuel.times(item.coefficient)
    val result = CalculationEnergy.getTotalFuelConsumption(
        item.acceptedFuel,
        item.deliveryFuel,
        item.fuelSupply
    )
    val resultInKilo = CalculationEnergy.getTotalFuelInKiloConsumption(result, item.coefficient)

    RevealSwipe(
        state = revealState,
        maxRevealDp = 75.dp,
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
                    modifier = Modifier.padding(horizontal = 25.dp),
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
            shape = Shapes.extraSmall
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = Shapes.extraSmall
                    )
            ) {
                val (sectionNum, refuelButton,
                    energyAccepted, energyDelivery,
                    inKiloBlock, infoBlock, errorMessage) = createRefs()
                Text(
                    modifier = Modifier
                        .constrainAs(sectionNum) {
                            top.linkTo(errorMessage.bottom)
                            start.linkTo(parent.start)
                        }
                        .padding(top = 16.dp, start = 16.dp),
                    text = "${index + 1} секция",
                    style = AppTypography.getType().bodyLarge
                )

                Row(modifier = Modifier
                    .constrainAs(refuelButton) {
                        top.linkTo(errorMessage.bottom)
                        end.linkTo(parent.end)
                    }
                    .clickable {
                        showRefuelDialog(item.fuelSupply)
                    }
                    .padding(top = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    item.fuelSupply?.let {
                        Text(
                            text = maskInLiter(it.str()) ?: "",
                            style = AppTypography.getType().bodyLarge,
                        )
                    }
                    Image(
                        modifier = Modifier
                            .size(dimensionResource(id = CoreR.dimen.min_size_view))
                            .padding(8.dp),
                        painter = painterResource(id = R.drawable.refuel_icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary)
                    )
                }

                OutlinedTextField(
                    modifier = Modifier
                        .constrainAs(energyAccepted) {
                            start.linkTo(parent.start, 16.dp)
                            end.linkTo(energyDelivery.start, 8.dp)
                            top.linkTo(refuelButton.bottom, 8.dp)
                            width = Dimension.fillToConstraints
                        },
                    value = acceptedText,
                    onValueChange = {
                        onFuelAcceptedChanged(item, it)
                    },
                    placeholder = {
                        Text(text = "Принято")
                    },
                    textStyle = AppTypography.getType().bodyLarge,
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
                        .constrainAs(energyDelivery) {
                            start.linkTo(energyAccepted.end, 8.dp)
                            end.linkTo(parent.end, 16.dp)
                            top.linkTo(refuelButton.bottom, 8.dp)
                            width = Dimension.fillToConstraints
                        },
                    value = deliveryText,
                    onValueChange = {
                        onFuelDeliveredChanged(item, it)
                    },
                    placeholder = {
                        Text(text = "Сдано", color = MaterialTheme.colorScheme.secondary)
                    },
                    textStyle = AppTypography.getType().bodyLarge,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        scope.launch {
                            focusManager.clearFocus()
                        }
                    })
                )

                val visibleInfoInKiloState =
                    (!item.acceptedFuel?.str().isNullOrBlank() || !item.deliveryFuel?.str()
                        .isNullOrBlank())

                Row(
                    modifier = Modifier
                        .constrainAs(inKiloBlock) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(energyAccepted.bottom)
                            visibility =
                                if (visibleInfoInKiloState) Visibility.Visible else Visibility.Gone
                        }
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        val acceptedInKiloText = rounding(acceptedInKilo, 2)?.str()
                        Text(
                            text = maskInKilo(acceptedInKiloText) ?: "",
                            style = AppTypography.getType().bodyMedium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        val deliveryInKiloText = rounding(deliveryInKilo, 2)?.str()
                        Text(
                            text = maskInKilo(deliveryInKiloText) ?: "",
                            style = AppTypography.getType().bodyMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .constrainAs(infoBlock) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(inKiloBlock.bottom)
                        }
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ClickableText(
                        text = AnnotatedString("k = ${item.coefficient ?: 0.0}"),
                        style = AppTypography.getType().bodyLarge,
                        onClick = {
                            showCoefficientDialog(item.coefficient)
                        })

                    result?.let {
                        val resultInLiterText = maskInLiter(it.str())
                        val resultInKiloText = maskInKilo(rounding(resultInKilo, 2)?.str())
                        Text(
                            text = "${resultInLiterText ?: ""} / ${resultInKiloText ?: ""}",
                            style = AppTypography.getType().bodyMedium
                        )
                    }
                }
            }
        }
    }
}