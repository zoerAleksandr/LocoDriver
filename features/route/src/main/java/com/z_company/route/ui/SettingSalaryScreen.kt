package com.z_company.route.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.AsyncDataValue
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.entities.SalarySetting
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
    surchargeExtendedServicePhaseValueState: ResultState<String>,
    setSurchargeExtendedServicePhase: (String) -> Unit,
    isErrorInputSurchargeExtendedServicePhase: Boolean,
    surchargeHeavyLongDistanceTrainsValueState: ResultState<String>,
    setSurchargeHeavyLongDistanceTrains: (String) -> Unit,
    isErrorInputSurchargeHeavyLongDistanceTrains: Boolean,
    otherSurchargeState: ResultState<String>,
    setOtherSurcharge: (String) -> Unit,
    isErrorInputOtherSurcharge: Boolean,
    ndflValueState: ResultState<String>,
    setNDFL: (String) -> Unit,
    isErrorInputNdfl: Boolean,
    unionistsRetentionState: ResultState<String>,
    setUnionistsRetention: (String) -> Unit,
    isErrorInputUnionistsRetention: Boolean,
    otherRetentionValueState: ResultState<String>,
    setOtherRetention: (String) -> Unit,
    isErrorInputOtherRetention: Boolean
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Начисления",
                    overflow = TextOverflow.Visible,
                    style = styleDataLight,
                    textAlign = TextAlign.End
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Доплата за удлиненное плечо, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = surchargeExtendedServicePhaseValueState) { surchargeExtendedServicePhaseValue ->
                        surchargeExtendedServicePhaseValue?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = surchargeExtendedServicePhaseValue,
                                onValueChange = { value ->
                                    setSurchargeExtendedServicePhase(value)
                                },
                                isError = isErrorInputSurchargeExtendedServicePhase,
                                supportingText = {
                                    if (isErrorInputSurchargeExtendedServicePhase) {
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Прочие доплаты, %",
                        overflow = TextOverflow.Visible,
                        style = styleDataMedium
                    )
                    AsyncDataValue(resultState = otherSurchargeState) { otherSurcharge ->
                        otherSurcharge?.let {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                value = otherSurcharge,
                                onValueChange = { value ->
                                    setOtherSurcharge(value)
                                },
                                isError = isErrorInputOtherSurcharge,
                                supportingText = {
                                    if (isErrorInputOtherSurcharge) {
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
                Text(
                    "Удержания",
                    overflow = TextOverflow.Visible,
                    style = styleDataLight,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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