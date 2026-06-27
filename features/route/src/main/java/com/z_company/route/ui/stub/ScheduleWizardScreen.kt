package com.z_company.route.ui.stub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleWizardScreen(
    onBack: () -> Unit,
) {
    var selectedPattern by remember { mutableIntStateOf(0) }
    val patterns = listOf("2 / 2" to "2 работа, 2 отдых", "5 / 2" to "Пн–пт, выходные", "3 / 3" to "3 через 3", "Свой" to "Настроить вручную")

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Заполнить месяц", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("‹ Апрель", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Text(
                        "Далее →",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Stepper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("1", color = MaterialTheme.colorScheme.surface,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
                Text(
                    "  Паттерн",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("2", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "  Старт и предпросмотр",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))

            // ПАТТЕРН ГРАФИКА
            Text(
                "ПАТТЕРН ГРАФИКА",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Grid 2x2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                patterns.chunked(2).forEachIndexed { rowIdx, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEachIndexed { colIdx, (title, subtitle) ->
                            val idx = rowIdx * 2 + colIdx
                            val isSelected = selectedPattern == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .then(
                                        if (isSelected) Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.tertiary,
                                            RoundedCornerShape(16.dp)
                                        )
                                        else Modifier
                                    )
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                    .clickable { selectedPattern = idx }
                                    .padding(14.dp),
                            ) {
                                Column {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ВРЕМЯ СМЕНЫ
            Text(
                "ВРЕМЯ СМЕНЫ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Начало", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("08:00", style = MaterialTheme.typography.headlineLarge)
                }
                Column {
                    Text("Конец", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("20:00", style = MaterialTheme.typography.headlineLarge)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Смены продолжительностью 12 часов разложатся по всем рабочим дням паттерна.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
