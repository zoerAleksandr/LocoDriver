package com.z_company.shared.ui.screen

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.shared.viewmodel.SettingSalarySharedViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingSalaryScreen(
    onBackClick: () -> Unit,
) {
    val viewModel: SettingSalarySharedViewModel = koinInject()
    val setting by viewModel.setting.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val hintStyle = MaterialTheme.typography.bodyMedium
    val paddingLarge = 6.dp

    LaunchedEffect(isSaved) {
        if (isSaved) {
            viewModel.onSaveAcknowledged()
            onBackClick()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    TextButton(
                        modifier = Modifier.padding(end = 16.dp),
                        enabled = !isLoading && setting != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.tertiary,
                        ),
                        onClick = { viewModel.saveSalarySetting() },
                    ) {
                        Text(text = "Сохранить", style = MaterialTheme.typography.bodySmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (isLoading || setting == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val s = setting!!

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Section: Начисления
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    text = "Начисления",
                    overflow = TextOverflow.Visible,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.End,
                )
            }

            // Средний час
            item {
                SalaryField(
                    label = "Средний час, руб.",
                    value = s.averagePaymentHour,
                    onValueChange = { viewModel.updateAveragePaymentHour(it) },
                    paddingTop = paddingLarge,
                )
            }

            // Зональная надбавка
            item {
                SalaryField(
                    label = "Зональная надбавка, %",
                    value = s.zonalSurcharge,
                    onValueChange = { viewModel.updateZonalSurcharge(it) },
                    paddingTop = paddingLarge,
                )
            }

            // Доплаты за класс
            item {
                SalaryField(
                    label = "Доплаты за класс и права, %",
                    value = s.surchargeQualificationClass,
                    onValueChange = { viewModel.updateSurchargeQualificationClass(it) },
                    paddingTop = paddingLarge,
                )
            }

            // Одно лицо (грузовой)
            item {
                SalaryField(
                    label = "Работа в одно лицо (грузовой), %",
                    value = s.onePersonOperationPercent,
                    onValueChange = { viewModel.updateOnePersonOperationPercent(it) },
                    paddingTop = paddingLarge,
                )
            }

            // Одно лицо (пассажирский)
            item {
                SalaryField(
                    label = "Работа в одно лицо (пассажирский), %",
                    value = s.onePersonOperationPassengerTrainPercent,
                    onValueChange = { viewModel.updateOnePersonOperationPassengerTrainPercent(it) },
                    paddingTop = paddingLarge,
                )
            }

            // Вредность
            item {
                SalaryField(
                    label = "Доплата за вредность, %",
                    value = s.harmfulnessPercent,
                    onValueChange = { viewModel.updateHarmfulnessPercent(it) },
                    paddingTop = paddingLarge,
                )
            }

            // Северная надбавка
            item {
                SalaryField(
                    label = "Северная надбавка, %",
                    value = s.nordicPercent,
                    onValueChange = { viewModel.updateNordicPercent(it) },
                    paddingTop = paddingLarge,
                )
            }

            // Районный коэффициент
            item {
                SalaryField(
                    label = "Районный коэффициент, %",
                    value = s.districtCoefficient,
                    onValueChange = { viewModel.updateDistrictCoefficient(it) },
                    paddingTop = paddingLarge,
                )
            }

            // Длинносоставные поезда
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(paddingLarge),
                ) {
                    Text(
                        text = "Доплата за длинносоставные поезда",
                        overflow = TextOverflow.Visible,
                        maxLines = 2,
                        style = hintStyle,
                        color = primaryColor,
                    )
                    Row(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = MaterialTheme.shapes.medium,
                            )
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SalaryInlineField(
                            modifier = Modifier.weight(1f),
                            value = s.lengthLongDistanceTrain.toDouble(),
                            onValueChange = { viewModel.updateLengthLongDistanceTrain(it.toInt()) },
                            suffix = "у.д.",
                        )
                        SalaryInlineField(
                            modifier = Modifier.weight(1f),
                            value = s.percentLongDistanceTrain,
                            onValueChange = { viewModel.updatePercentLongDistanceTrain(it) },
                            suffix = "%",
                        )
                    }
                }
            }

            // Тяжелые поезда header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Доплата за тяж. поезда",
                        overflow = TextOverflow.Visible,
                        style = hintStyle,
                        color = primaryColor,
                        maxLines = 2,
                    )
                    TextButton(onClick = { viewModel.addSurchargeHeavyTrain() }) {
                        Text(
                            text = "Добавить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            // Тяжелые поезда list with swipe-to-dismiss
            itemsIndexed(
                items = s.surchargeHeavyTrainsList,
                key = { _, item -> item.id },
            ) { index, item ->
                SwipeToDismissItem(
                    onDismissed = { viewModel.removeSurchargeHeavyTrain(index) },
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = MaterialTheme.shapes.medium,
                            )
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SalaryStringField(
                            modifier = Modifier.weight(1f),
                            value = item.weight,
                            onValueChange = { viewModel.updateSurchargeHeavyTrainWeight(index, it) },
                            suffix = "т.",
                        )
                        SalaryStringField(
                            modifier = Modifier.weight(1f),
                            value = item.percentSurcharge,
                            onValueChange = { viewModel.updateSurchargeHeavyTrainPercent(index, it) },
                            suffix = "%",
                        )
                    }
                }
            }

            // Удлиненное плечо header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Доплата за удлиненное плечо",
                        overflow = TextOverflow.Ellipsis,
                        style = hintStyle,
                        maxLines = 2,
                        color = primaryColor,
                    )
                    TextButton(onClick = { viewModel.addServicePhase() }) {
                        Text(
                            text = "Добавить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            // Удлиненное плечо list with swipe-to-dismiss
            itemsIndexed(
                items = s.surchargeExtendedServicePhaseList,
                key = { _, item -> item.id },
            ) { index, item ->
                SwipeToDismissItem(
                    onDismissed = { viewModel.removeServicePhase(index) },
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = MaterialTheme.shapes.medium,
                            )
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SalaryStringField(
                            modifier = Modifier.weight(1f),
                            value = item.distance,
                            onValueChange = { viewModel.updateServicePhaseDistance(index, it) },
                            suffix = "км",
                        )
                        SalaryStringField(
                            modifier = Modifier.weight(1f),
                            value = item.percentSurcharge,
                            onValueChange = { viewModel.updateServicePhasePercent(index, it) },
                            suffix = "%",
                        )
                    }
                }
            }

            // Другие надбавки
            item {
                SalaryField(
                    label = "Другие надбавки, %",
                    value = s.otherSurcharge,
                    onValueChange = { viewModel.updateOtherSurcharge(it) },
                    paddingTop = 18.dp,
                )
            }

            // Section: Удержания
            item {
                Text(
                    "Удержания",
                    overflow = TextOverflow.Visible,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingLarge),
                    textAlign = TextAlign.End,
                )
            }

            // НДФЛ
            item {
                SalaryField(
                    label = "Подоходный налог, %",
                    value = s.ndfl,
                    onValueChange = { viewModel.updateNdfl(it) },
                    paddingTop = paddingLarge,
                )
            }

            // Профсоюз
            item {
                SalaryField(
                    label = "Профсоюз, %",
                    value = s.unionistsRetention,
                    onValueChange = { viewModel.updateUnionistsRetention(it) },
                    paddingTop = paddingLarge,
                )
            }

            // Прочие удержания
            item {
                SalaryField(
                    label = "Прочие удержания, %",
                    value = s.otherRetention,
                    onValueChange = { viewModel.updateOtherRetention(it) },
                    paddingTop = paddingLarge,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Private composables
// ---------------------------------------------------------------------------

@Composable
private fun SalaryField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    paddingTop: androidx.compose.ui.unit.Dp,
) {
    var text by rememberSaveable(value) { mutableStateOf(formatDouble(value)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = paddingTop),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            overflow = TextOverflow.Visible,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                text = raw
                raw.replace(",", ".").toDoubleOrNull()?.let { onValueChange(it) }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

@Composable
private fun SalaryInlineField(
    modifier: Modifier = Modifier,
    value: Double,
    onValueChange: (Double) -> Unit,
    suffix: String,
) {
    var text by rememberSaveable(value) { mutableStateOf(formatDouble(value)) }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            raw.replace(",", ".").toDoubleOrNull()?.let { onValueChange(it) }
        },
        modifier = modifier,
        singleLine = true,
        suffix = { Text(text = suffix, style = MaterialTheme.typography.bodyMedium) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun SalaryStringField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        suffix = { Text(text = suffix, style = MaterialTheme.typography.bodyMedium) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissItem(
    onDismissed: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismissed()
                false
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "",
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxSize()
                    .background(color = color, shape = MaterialTheme.shapes.medium),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    modifier = Modifier.padding(end = 16.dp),
                    imageVector = Icons.Default.Delete,
                    tint = MaterialTheme.colorScheme.background,
                    contentDescription = null,
                )
            }
        },
    ) {
        content()
    }
}

private fun formatDouble(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}
