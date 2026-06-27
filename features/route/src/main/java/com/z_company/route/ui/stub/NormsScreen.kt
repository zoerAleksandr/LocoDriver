package com.z_company.route.ui.stub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormsScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Серии локомотивов", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("‹", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.surface,
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Info box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Для каждой серии задаются нормы длительности приёмки и сдачи. Нормы применяются автоматически.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ЭЛЕКТРОВОЗЫ
            Text(
                "ЭЛЕКТРОВОЗЫ · 3",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            val electrics = listOf(
                Triple("ВЛ80с", "40 мин", "50 мин"),
                Triple("ЧС7", "35 мин", "45 мин"),
                Triple("ЭП1М", null, null),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            ) {
                electrics.forEachIndexed { index, (series, accept, delivery) ->
                    NormRow(series, accept, delivery)
                    if (index < electrics.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ТЕПЛОВОЗЫ
            Text(
                "ТЕПЛОВОЗЫ · 2",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            val diesels = listOf(
                Triple("2ТЭ10М", "50 мин", "60 мин"),
                Triple("2ТЭ116", "45 мин", "55 мин"),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            ) {
                diesels.forEachIndexed { index, (series, accept, delivery) ->
                    NormRow(series, accept, delivery)
                    if (index < diesels.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun NormRow(series: String, acceptTime: String?, deliveryTime: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceDim, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                series,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Spacer(Modifier.width(12.dp))
        if (acceptTime != null && deliveryTime != null) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Приёмка  $acceptTime", style = MaterialTheme.typography.bodySmall)
                Text("Сдача    $deliveryTime", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text(
                "Нормы не заданы",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        Text("›", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
