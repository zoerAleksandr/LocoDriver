package com.z_company.route.component.servicephase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Чип поезда маршрута на карте (SCREEN_SPECS §6.9, «Переключатель поездов»). */
internal data class MapTrainChip(val id: String, val label: String, val recording: Boolean)

/** Высота ряда чипов вместе с отступом под ним: на столько сдвигаются плашки под шапкой карты. */
internal val MapTrainChipsHeight = 40.dp

/** Ряд чипов поездов маршрута; выбран - [selectedId]; поезд с идущей записью помечен точкой. */
@Composable
internal fun MapTrainChips(
    chips: List<MapTrainChip>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            val selected = chip.id == selectedId
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .background(if (selected) Pst.accent else Pst.surface, RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { if (!selected) onSelect(chip.id) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (chip.recording) "● ${chip.label}" else chip.label,
                    style = Pst.sans.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = if (selected) Pst.onAccent else Pst.text,
                    maxLines = 1,
                )
            }
        }
    }
}
