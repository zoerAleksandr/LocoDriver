package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.route.component.ShoulderEditBottomSheet
import com.z_company.route.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsShouldersContent(
    settingsUiState: SettingsUiState,
    servicePhases: SnapshotStateList<ServicePhase>?,
    stationList: List<String>,
    showDialogAddServicePhase: (ServicePhase) -> Unit,
    hideDialogAddServicePhase: () -> Unit,
    addServicePhase: (ServicePhase, Int) -> Unit,
    deleteServicePhase: (ServicePhase) -> Unit,
    updateServicePhase: (ServicePhase, Int) -> Unit,
    onDeleteStationName: (String) -> Unit = {},
) {
    val phases = servicePhases?.toList() ?: emptyList()

    // Локальное состояние шторки добавления/редактирования — переиспользуем общий
    // ShoulderEditBottomSheet (тот же, что в FormTrainScreen), а не дублирующую шторку.
    var showEditSheet by remember { mutableStateOf(false) }
    var editingPhase by remember { mutableStateOf<ServicePhase?>(null) }

    if (showEditSheet) {
        ShoulderEditBottomSheet(
            phase = editingPhase,
            stationList = stationList,
            onSave = { phase, createReverse ->
                // Редактирование — тот же id → замена по индексу; новое — id новый,
                // indexOfFirst вернёт -1 → добавление в конец.
                val index = servicePhases?.indexOfFirst { it.id == phase.id } ?: -1
                addServicePhase(phase, index)
                if (createReverse) {
                    addServicePhase(
                        ServicePhase(
                            departureStation = phase.arrivalStation,
                            arrivalStation = phase.departureStation,
                            distance = phase.distance,
                            linearMileageRate = phase.linearMileageRate,
                        ),
                        -1
                    )
                }
            },
            onDelete = editingPhase?.let { p -> { deleteServicePhase(p) } },
            onDeleteStationName = onDeleteStationName,
            onDismiss = { showEditSheet = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Пояснение
        Text(
            text = "Расстояние участка обслуживания. Подставляется автоматически " +
                "при выборе плеча в маршруте.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
        )

        if (phases.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Нет сохранённых плеч",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Добавьте участки обслуживания с расстоянием",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "ПЛЕЧИ · ${phases.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                    .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
            ) {
                phases.forEachIndexed { idx, item ->
                    ShoulderRow(
                        item = item,
                        onClick = {
                            editingPhase = item
                            showEditSheet = true
                        }
                    )
                    if (idx < phases.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Добавить плечо — карточка-строка в стиле Серий/Станций
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = Shapes.medium)
                .background(color = MaterialTheme.colorScheme.tertiary, shape = Shapes.medium)
                .clickable {
                    editingPhase = null
                    showEditSheet = true
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.ic_add),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "  Добавить плечо",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// Строка плеча: «Отправление — Прибытие» (Inter, имена станций) + «N км» (Mono) + шеврон
@Composable
private fun ShoulderRow(
    item: ServicePhase,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "${item.departureStation} — ${item.arrivalStation}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${item.distance} км",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${item.linearMileageRate} ₽/км",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}
