package com.z_company.shared.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.shared.viewmodel.SettingSalarySharedViewModel
import org.koin.compose.koinInject

/**
 * Shared KMP screen for salary settings — edits all numeric fields of [SalarySetting].
 *
 * Fields are grouped by cards: Main, Surcharges, Deductions.
 * Save button (checkmark in TopAppBar) saves and navigates back.
 *
 * Used by both Android and iOS.
 */
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

    // Navigate back after successful save
    LaunchedEffect(isSaved) {
        if (isSaved) {
            viewModel.onSaveAcknowledged()
            onBackClick()
        }
    }

    // Show error in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройка зарплаты") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveSalarySetting() },
                        enabled = !isLoading && setting != null,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Section: Main ---
            SalarySectionCard(title = "Основные") {
                SalaryDoubleField(
                    label = "Средняя оплата за час",
                    value = s.averagePaymentHour,
                    onValueChange = { viewModel.updateAveragePaymentHour(it) },
                )
                SalaryDoubleField(
                    label = "% ночных",
                    value = s.nightTimePercent,
                    onValueChange = { viewModel.updateNightTimePercent(it) },
                )
            }

            // --- Section: Surcharges ---
            SalarySectionCard(title = "Надбавки") {
                SalaryDoubleField(
                    label = "Зональная надбавка %",
                    value = s.zonalSurcharge,
                    onValueChange = { viewModel.updateZonalSurcharge(it) },
                )
                SalaryDoubleField(
                    label = "Квалификационный класс %",
                    value = s.surchargeQualificationClass,
                    onValueChange = { viewModel.updateSurchargeQualificationClass(it) },
                )
                SalaryDoubleField(
                    label = "Вредность %",
                    value = s.harmfulnessPercent,
                    onValueChange = { viewModel.updateHarmfulnessPercent(it) },
                )
                SalaryDoubleField(
                    label = "Районный коэффициент",
                    value = s.districtCoefficient,
                    onValueChange = { viewModel.updateDistrictCoefficient(it) },
                )
                SalaryDoubleField(
                    label = "Северная надбавка %",
                    value = s.nordicPercent,
                    onValueChange = { viewModel.updateNordicPercent(it) },
                )
                SalaryDoubleField(
                    label = "Одно лицо (грузовой) %",
                    value = s.onePersonOperationPercent,
                    onValueChange = { viewModel.updateOnePersonOperationPercent(it) },
                )
                SalaryDoubleField(
                    label = "Одно лицо (пассаж.) %",
                    value = s.onePersonOperationPassengerTrainPercent,
                    onValueChange = { viewModel.updateOnePersonOperationPassengerTrainPercent(it) },
                )
                SalaryDoubleField(
                    label = "Тяжеловесный поезд %",
                    value = s.percentLongDistanceTrain,
                    onValueChange = { viewModel.updatePercentLongDistanceTrain(it) },
                )
                SalaryDoubleField(
                    label = "Прочие надбавки",
                    value = s.otherSurcharge,
                    onValueChange = { viewModel.updateOtherSurcharge(it) },
                )
            }

            // --- Section: Deductions ---
            SalarySectionCard(title = "Удержания") {
                SalaryDoubleField(
                    label = "НДФЛ %",
                    value = s.ndfl,
                    onValueChange = { viewModel.updateNdfl(it) },
                )
                SalaryDoubleField(
                    label = "Профсоюзные %",
                    value = s.unionistsRetention,
                    onValueChange = { viewModel.updateUnionistsRetention(it) },
                )
                SalaryDoubleField(
                    label = "Прочие удержания",
                    value = s.otherRetention,
                    onValueChange = { viewModel.updateOtherRetention(it) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Private composables
// ---------------------------------------------------------------------------

@Composable
private fun SalarySectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

/**
 * OutlinedTextField for a Double salary field.
 *
 * Uses a local text state so the user can freely edit the raw string;
 * [onValueChange] is called whenever the text parses successfully as Double.
 */
@Composable
private fun SalaryDoubleField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
) {
    var text by rememberSaveable(value) { mutableStateOf(formatDouble(value)) }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            raw.replace(",", ".").toDoubleOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

/** Formats Double to a clean string, removing unnecessary trailing zeros. */
private fun formatDouble(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}
