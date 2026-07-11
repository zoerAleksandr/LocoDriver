package com.z_company.route.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.z_company.route.R

/**
 * Одна строка легенды: иконка + подпись.
 *
 * @param iconRes ресурс иконки.
 * @param label подпись обозначения.
 * @param isImage true — рисуем как [Image] (цветной ассет без тонировки),
 *                false — как [Icon] с тонировкой [tint].
 * @param tint цвет тонировки для [Icon].
 */
private data class LegendItem(
    val iconRes: Int,
    val label: String,
    val isImage: Boolean = false,
    val tint: Color? = null,
)

/**
 * Bottom-sheet «Обозначения» — расшифровка значков в строке маршрута.
 * Открывается с главного экрана (Поездки) по нажатию на значки маршрута.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteLegendSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
) {
    val cs = MaterialTheme.colorScheme
    val items = listOf(
        LegendItem(R.drawable.icon_holiday, "Работа в праздничный день", isImage = true),
        LegendItem(R.drawable.pause_24px, "Перерыв в работе"),
        LegendItem(R.drawable.straighten_24px, "Поезда повышенной длины"),
        LegendItem(R.drawable.weight_24px, "Поезда повышенной массы"),
        LegendItem(R.drawable.long_distance_24px, "Удлинённое плечо обслуживания"),
        LegendItem(R.drawable.person_24px, "Работа в одно лицо"),
        LegendItem(R.drawable.passenger_24px, "Следование пассажиром"),
        LegendItem(R.drawable.orden, "Работа свыше 12-ти часов", isImage = true),
        LegendItem(R.drawable.ic_pusher_24px, "Толкач"),
        LegendItem(R.drawable.ic_double_traction_24px, "Двойная тяга"),
        LegendItem(R.drawable.ic_doubled_train_24px, "Сдвоенный поезд"),
        LegendItem(R.drawable.sync_on_icon, "Статус синхронизации маршрута", isImage = true),
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = cs.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Обозначения",
                style = MaterialTheme.typography.titleLarge,
                color = cs.primary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 6.dp)
            )
            Text(
                text = "Значки в строке маршрута показывают его особенности. " +
                        "У одного маршрута может быть несколько отметок.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
            )
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = cs.surfaceBright,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isImage) {
                            Image(
                                modifier = Modifier.size(22.dp),
                                painter = painterResource(id = item.iconRes),
                                contentDescription = null
                            )
                        } else {
                            Icon(
                                modifier = Modifier.size(22.dp),
                                painter = painterResource(id = item.iconRes),
                                tint = item.tint ?: cs.primary,
                                contentDescription = null
                            )
                        }
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
