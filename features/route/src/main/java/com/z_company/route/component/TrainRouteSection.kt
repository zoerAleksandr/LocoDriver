package com.z_company.route.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.Train

/**
 * Пример секции маршрута внутри TrainScreen.
 *
 * Замените содержимое вашего текущего списка станций
 * на вызов TrainStationTimeline.
 */
@Composable
fun TrainRouteSection(
    train: Train,
    modifier: Modifier = Modifier,
) {
    val timelineItems = train.stations.toStationTimelineItems()

    Column(modifier = modifier) {
        TrainStationTimeline(
            stations = timelineItems,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}