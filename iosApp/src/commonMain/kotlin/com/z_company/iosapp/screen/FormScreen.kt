package com.z_company.iosapp.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.util.TimeFormatter
import com.z_company.iosapp.viewmodel.FormIosViewModel
import org.koin.compose.koinInject

/**
 * Route create/edit screen.
 *
 * [routeId] == null -> new route; non-null -> editing existing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormScreen(
    router: Router,
    routeId: String? = null,
) {
    val viewModel: FormIosViewModel = koinInject()
    val route by viewModel.route.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(routeId) {
        viewModel.loadRoute(routeId)
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            router.back()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (routeId == null) "Новый маршрут" else "Маршрут")
                },
                navigationIcon = {
                    IconButton(onClick = { router.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    val isFavorite = route?.basicData?.isFavorite ?: false
                    IconToggleButton(
                        checked = isFavorite,
                        onCheckedChange = { viewModel.toggleFavorite(it) },
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Избранное",
                            tint = if (isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val currentRoute = route
        val basicData = currentRoute?.basicData

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Section: Route Number ---
            SectionCard(title = "Основные данные") {
                OutlinedTextField(
                    value = basicData?.number ?: "",
                    onValueChange = { viewModel.updateNumber(it) },
                    label = { Text("Номер маршрута") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Section: Work Times ---
            SectionCard(title = "Время работы") {
                FormReadOnlyRow(
                    label = "Начало работы",
                    value = basicData?.timeStartWork?.let { TimeFormatter.formatDateTime(it) },
                )

                FormReadOnlyRow(
                    label = "Окончание работы",
                    value = basicData?.timeEndWork?.let { TimeFormatter.formatDateTime(it) },
                )

                // Work duration
                val workDurationMs = computeWorkDuration(
                    startMs = basicData?.timeStartWork,
                    endMs = basicData?.timeEndWork,
                    breakStartMs = basicData?.timeStartBreak,
                    breakEndMs = basicData?.timeEndBreak,
                )
                if (workDurationMs != null && workDurationMs > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    FormReadOnlyRow(
                        label = "Продолжительность",
                        value = TimeFormatter.formatDuration(workDurationMs),
                    )
                }
            }

            // --- Section: Break Times (only if present) ---
            val hasBreak = basicData?.timeStartBreak != null || basicData?.timeEndBreak != null
            if (hasBreak) {
                SectionCard(title = "Перерыв") {
                    FormReadOnlyRow(
                        label = "Начало перерыва",
                        value = basicData?.timeStartBreak?.let { TimeFormatter.formatDateTime(it) },
                    )
                    FormReadOnlyRow(
                        label = "Окончание перерыва",
                        value = basicData?.timeEndBreak?.let { TimeFormatter.formatDateTime(it) },
                    )

                    val breakStart = basicData?.timeStartBreak
                    val breakEnd = basicData?.timeEndBreak
                    if (breakStart != null && breakEnd != null && breakEnd > breakStart) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        FormReadOnlyRow(
                            label = "Длительность перерыва",
                            value = TimeFormatter.formatDuration(breakEnd - breakStart),
                        )
                    }
                }
            }

            // --- Section: Counts ---
            currentRoute?.let { r ->
                val locoCount = r.locomotives.size
                val trainCount = r.trains.size
                val passengerCount = r.passengers.size

                if (locoCount > 0 || trainCount > 0 || passengerCount > 0) {
                    SectionCard(title = "Состав") {
                        if (locoCount > 0) {
                            FormReadOnlyRow(
                                label = "Локомотивов",
                                value = locoCount.toString(),
                            )
                        }
                        if (trainCount > 0) {
                            FormReadOnlyRow(
                                label = "Поездов",
                                value = trainCount.toString(),
                            )
                        }
                        if (passengerCount > 0) {
                            FormReadOnlyRow(
                                label = "Пассажирских",
                                value = passengerCount.toString(),
                            )
                        }
                    }
                }
            }

            // --- Section: Notes ---
            SectionCard(title = "Заметки") {
                OutlinedTextField(
                    value = basicData?.notes ?: "",
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Заметки") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // --- Section: Favorite toggle ---
            SectionCard(title = "Дополнительно") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Избранное",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = basicData?.isFavorite ?: false,
                        onCheckedChange = { viewModel.toggleFavorite(it) },
                    )
                }
            }

            // --- Save Button ---
            Button(
                onClick = { viewModel.saveRoute() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * A Card-based section with a title header.
 */
@Composable
private fun SectionCard(
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
 * Read-only label-value row for displaying route data.
 */
@Composable
private fun FormReadOnlyRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: "\u2014",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Computes work duration taking break into account.
 */
private fun computeWorkDuration(
    startMs: Long?,
    endMs: Long?,
    breakStartMs: Long?,
    breakEndMs: Long?,
): Long? {
    if (startMs == null || endMs == null || endMs <= startMs) return null
    val raw = endMs - startMs
    val breakDuration = if (breakStartMs != null && breakEndMs != null && breakEndMs > breakStartMs) {
        breakEndMs - breakStartMs
    } else {
        0L
    }
    return raw - breakDuration
}
