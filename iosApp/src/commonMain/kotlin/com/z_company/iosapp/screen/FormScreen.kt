package com.z_company.iosapp.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.viewmodel.FormIosViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

/**
 * Экран создания / редактирования маршрута.
 *
 * [routeId] == null → новый маршрут; не null → редактирование существующего.
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

    // Загружаем маршрут при входе на экран
    LaunchedEffect(routeId) {
        viewModel.loadRoute(routeId)
    }

    // Возвращаемся назад после успешного сохранения
    LaunchedEffect(isSaved) {
        if (isSaved) {
            router.back()
        }
    }

    // Показываем ошибку
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

        val basicData = route?.basicData

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Номер маршрута
            OutlinedTextField(
                value = basicData?.number ?: "",
                onValueChange = { viewModel.updateNumber(it) },
                label = { Text("Номер маршрута") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
            )

            // Время начала работы (read-only, отображаем если установлено)
            basicData?.timeStartWork?.let { ms ->
                Text(
                    text = "Начало: ${formatEpochMs(ms)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // Время окончания работы
            basicData?.timeEndWork?.let { ms ->
                Text(
                    text = "Окончание: ${formatEpochMs(ms)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // Количество локомотивов / поездов / пассажирских
            route?.let { r ->
                Text(
                    text = "Локомотивов: ${r.locomotives.size}  " +
                            "Поездов: ${r.trains.size}  " +
                            "Пассажирских: ${r.passengers.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Заметки
            OutlinedTextField(
                value = basicData?.notes ?: "",
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Заметки") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Сохранить
            Button(
                onClick = { viewModel.saveRoute() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }
        }
    }
}

private fun formatEpochMs(ms: Long): String {
    val instant = Instant.fromEpochMilliseconds(ms)
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val d = ldt.dayOfMonth.toString().padStart(2, '0')
    val mo = ldt.monthNumber.toString().padStart(2, '0')
    val h = ldt.hour.toString().padStart(2, '0')
    val min = ldt.minute.toString().padStart(2, '0')
    return "$d.$mo.${ldt.year} $h:$min"
}
