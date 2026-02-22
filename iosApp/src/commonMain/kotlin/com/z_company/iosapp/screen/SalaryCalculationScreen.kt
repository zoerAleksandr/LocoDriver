package com.z_company.iosapp.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.domain.navigation.Router

/**
 * Экран расчёта зарплаты — Шаг 16 заменит стаб реальным SalaryCalculationViewModel.
 *
 * TODO Step 16: подключить SalaryCalculationViewModel + SalaryCalculationUseCase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SalaryCalculationScreen(router: Router) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Расчёт зарплаты") },
                navigationIcon = {
                    IconButton(onClick = { router.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Расчёт зарплаты",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Экран в разработке.\nStep 16 подключит SalaryCalculationViewModel\nиз features/route.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { router.showSettingSalary() }) {
                Text("Настройка зарплаты")
            }
            Button(onClick = { router.back() }) {
                Text("Назад")
            }
        }
    }
}
