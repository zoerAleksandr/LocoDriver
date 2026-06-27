package com.z_company.route.ui.stub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Всё", "Поездки", "Отвлечения")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                // Hero: МАЙ 2026 + отработано
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "МАЙ 2026",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "129:55",
                            style = MaterialTheme.typography.displayMedium,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = {}) {
                            Text("‹", style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(onClick = {}) {
                            Text("›", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Дни недели
            val days = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
            Row(modifier = Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (day == "СБ" || day == "ВС")
                            MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Сетка дней (заглушка — 5 недель)
            for (week in 0..4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (dayOfWeek in 0..6) {
                        val dayNum = week * 7 + dayOfWeek - 2 // offset for May
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                                .then(
                                    if (dayNum == 15) Modifier
                                        .border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                                .background(
                                    when {
                                        dayNum in 25..28 -> MaterialTheme.colorScheme.surfaceDim
                                        dayNum == 4 || dayNum == 5 -> MaterialTheme.colorScheme.surfaceDim
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (dayNum in 1..31) {
                                Text(
                                    text = "$dayNum",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = {},
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    "⌃ Свернуть в неделю",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Кнопка добавления
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
                    text = "+ Добавить событие на 15 мая",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = "📅 Заполнить месяц по графику",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Детали выбранного дня
            Text(
                "15 мая  Пт",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            "15.05  07:45",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Поездка №115",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "04:35",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
