package com.z_company.route.component.servicephase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Подпись значка станции для доступности: есть ли у станции координаты. */
internal fun stationGeoPinDescription(hasCoordinates: Boolean): String =
    if (hasCoordinates) "Координаты станции есть. Открыть карточку станции" else "Нет координат. Открыть карточку станции"

/**
 * Значок местоположения станции в правой части строки/поля (редактор плеча, SCREEN_SPECS §6.6): с координатами -
 * акцентный, без них - приглушённый с оранжевой точкой «!». Нажатие открывает карточку этой станции
 * (название, блок «Координаты»).
 */
@Composable
internal fun StationGeoPin(
    hasCoordinates: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val description = stationGeoPinDescription(hasCoordinates)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PstIcon(PstIcons.MapPin, 20.dp, if (hasCoordinates) Pst.accent else Pst.textFaint, description = description)
        if (!hasCoordinates) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = size * 0.15f, end = size * 0.12f)
                    .size(12.dp)
                    .background(Pst.warning, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "!", style = Pst.sans.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.White)
            }
        }
    }
}
