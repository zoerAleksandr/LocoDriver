package com.z_company.route.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncDataValue
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.entities.SurchargeExtendedServicePhase
import com.z_company.domain.util.str
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingSalaryScreen(
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
    isEnableSaveButton: Boolean,
    saveSettingState: ResultState<Unit>?,
    resetSaveState: () -> Unit,
    tariffRateValueState: ResultState<String>,
    setTariffRate: (String) -> Unit,
    isErrorInputTariffRate: Boolean,
    zonalSurchargeValueState: ResultState<String>,
    setZonalSurcharge: (String) -> Unit,
    isErrorInputZonalSurcharge: Boolean,
    surchargeQualificationClassValueState: ResultState<String>,
    setSurchargeQualificationClass: (String) -> Unit,
    isErrorInputSurchargeQualificationClass: Boolean,
    surchargeExtendedServicePhaseValueState: SnapshotStateList<SurchargeExtendedServicePhase>,
    addServicePhase: () -> Unit,
    setSurchargeExtendedServicePhaseDistance: (Int, String) -> Unit,
    setSurchargeExtendedServicePhasePercent: (Int, String) -> Unit,
    surchargeHeavyLongDistanceTrainsValueState: ResultState<String>,
    setSurchargeHeavyLongDistanceTrains: (String) -> Unit,
    isErrorInputSurchargeHeavyLongDistanceTrains: Boolean,
    ndflValueState: ResultState<String>,
    setNDFL: (String) -> Unit,
    isErrorInputNdfl: Boolean,
    unionistsRetentionState: ResultState<String>,
    setUnionistsRetention: (String) -> Unit,
    isErrorInputUnionistsRetention: Boolean,
    otherRetentionValueState: ResultState<String>,
    setOtherRetention: (String) -> Unit,
    isErrorInputOtherRetention: Boolean,
    onServicePhaseDismissed: (Int) -> Unit
) {
    val styleDataLight = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val titleStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Medium)
    val styleDataMedium = AppTypography.getType().titleMedium.copy(fontWeight = FontWeight.Normal)
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val paddingLarge = 12.dp
    val paddingSmall = 6.dp

    if (saveSettingState is ResultState.Success) {
        LaunchedEffect(Unit) {
            onBack()
        }
    }
    if (saveSettingState is ResultState.Error) {
        LaunchedEffect(Unit) {
            scope.launch {
                snackbarHostState.showSnackbar("Ошибка: ${saveSettingState.entity.message}")
            }
            resetSaveState()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Тарифная ставка и коэффициенты",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = titleStyle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    AsyncDataValue(resultState = saveSettingState) {
                        TextButton(
                            modifier = Modifier
                                .padding(end = 16.dp),
                            enabled = isEnableSaveButton,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.tertiary
                            ),
                            onClick = { onSaveClick() }
                        ) {
                            Text(text = "Сохранить", style = hintStyle)
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        }
    ) { paddingValue ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(top = paddingLarge),
                    text = "Начисления",
                    overflow = TextOverflow.Visible,
                    style = styleDataLight,
                    textAlign = TextAlign.End
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    Text(
                        "Тарифная ставка, руб.",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = tariffRateValueState) { tariffRateValue ->
                        tariffRateValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = tariffRateValue,
                                onValueChange = { value ->
                                    setTariffRate(value)
                                },
                                isError = isErrorInputTariffRate,
                                supportingText = {
                                    if (isErrorInputTariffRate) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    Text(
                        "Зональная надбавка, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = zonalSurchargeValueState) { zonalSurchargeValue ->
                        zonalSurchargeValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = zonalSurchargeValue,
                                onValueChange = { value ->
                                    setZonalSurcharge(value)
                                },
                                isError = isErrorInputZonalSurcharge,
                                supportingText = {
                                    if (isErrorInputZonalSurcharge) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                shape = Shapes.medium,
                            )
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    Text(
                        "Доплаты за класс и права, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = surchargeQualificationClassValueState) { surchargeQualificationClassValue ->
                        surchargeQualificationClassValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = surchargeQualificationClassValue,
                                onValueChange = { value ->
                                    setSurchargeQualificationClass(value)
                                },
                                isError = isErrorInputSurchargeQualificationClass,
                                supportingText = {
                                    if (isErrorInputSurchargeQualificationClass) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    Text(
                        "Доплата за тяжелые и длинные поезда, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = surchargeHeavyLongDistanceTrainsValueState) { surchargeHeavyLongDistanceTrainsValue ->
                        surchargeHeavyLongDistanceTrainsValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = surchargeHeavyLongDistanceTrainsValue,
                                onValueChange = { value ->
                                    setSurchargeHeavyLongDistanceTrains(value)
                                },
                                isError = isErrorInputSurchargeHeavyLongDistanceTrains,
                                supportingText = {
                                    if (isErrorInputSurchargeHeavyLongDistanceTrains) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Доплата за удлиненное плечо",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    TextButton(
                        onClick = addServicePhase) {
                        Text(
                            text = "Добавить",
                            style = styleDataMedium.copy(color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }

            itemsIndexed(
                items = surchargeExtendedServicePhaseValueState,
                key = { _, item -> item.id }
            ) { index, item ->
                val dismissState = rememberSwipeToDismissBoxState()

                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    onServicePhaseDismissed(index)
                }

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val color by animateColorAsState(
                            when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                else -> Color.Transparent
                            }, label = ""
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(top = 6.dp)
                                .background(color = color, shape = Shapes.medium),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                modifier = Modifier.padding(end = 16.dp),
                                imageVector = Icons.Outlined.Delete,
                                tint = MaterialTheme.colorScheme.background,
                                contentDescription = null
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = Shapes.medium
                            )
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = item.distance,
                            onValueChange = { value ->
                                setSurchargeExtendedServicePhaseDistance(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "км",
                                    style = styleDataMedium
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = Shapes.medium,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = item.percentSurcharge,
                            onValueChange = { value ->
                                setSurchargeExtendedServicePhasePercent(index, value)
                            },
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = "%",
                                    style = styleDataMedium
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = Shapes.medium,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    "Удержания",
                    overflow = TextOverflow.Visible,
                    style = styleDataLight,
                    modifier = Modifier.fillMaxWidth().padding(top = paddingLarge),
                    textAlign = TextAlign.End
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    Text(
                        "Подоходный налог, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = ndflValueState) { ndflValue ->
                        ndflValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = ndflValue,
                                onValueChange = { value ->
                                    setNDFL(value)
                                },
                                isError = isErrorInputNdfl,
                                supportingText = {
                                    if (isErrorInputNdfl) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    Text(
                        "Профсоюз, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = unionistsRetentionState) { unionistsRetention ->
                        unionistsRetention?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = unionistsRetention,
                                onValueChange = { value ->
                                    setUnionistsRetention(value)
                                },
                                isError = isErrorInputUnionistsRetention,
                                supportingText = {
                                    if (isErrorInputUnionistsRetention) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingSmall)
                ) {
                    Text(
                        "Прочие удержания, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = otherRetentionValueState) { otherRetentionValue ->
                        otherRetentionValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = otherRetentionValue,
                                onValueChange = { value ->
                                    setOtherRetention(value)
                                },
                                isError = isErrorInputOtherRetention,
                                supportingText = {
                                    if (isErrorInputOtherRetention) {
                                        Text(text = "Некорректные данные")
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = Shapes.medium,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}