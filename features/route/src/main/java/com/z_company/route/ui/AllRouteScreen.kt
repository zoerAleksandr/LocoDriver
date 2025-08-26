package com.z_company.route.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.z_company.route.viewmodel.home_view_model.ItemState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.entities.route.Route
import com.z_company.route.component.ItemHomeScreen


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllRouteScreen(
    listRouteState: MutableList<ItemState>,
    makeCopyRoute: (String) -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onRouteClick: (String) -> Unit,
//    onNewRouteClick: () -> Unit,
    getTextWorkTime: (Route) -> String
) {

    var routeForPreview by remember {
        mutableStateOf<Route?>(null)
    }

    var showContextDialog by remember {
        mutableStateOf(false)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        topBar = {

        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Список"
                    )
                }
            }

            items(listRouteState, key = { item -> item.route.basicData.id }) { item ->
                var requiredSize by remember {
                    mutableStateOf(22.sp)
                }

                fun changingTextSize(value: TextUnit) {
                    if (requiredSize > value) {
                        requiredSize = value
                    }
                }

                var background = if (item.isFuture) {
                    MaterialTheme.colorScheme.surfaceBright
                } else if (item.isTransition) {
                    MaterialTheme.colorScheme.surfaceDim
                } else {
                    MaterialTheme.colorScheme.secondary
                }

                ItemHomeScreen(
                    modifier = Modifier.animateItemPlacement(),
                    route = item.route,
                    isExpand = true,
                    onDelete = onDeleteRoute,
                    requiredSizeText = requiredSize,
                    changingTextSize = ::changingTextSize,
                    onLongClick = {
                        showContextDialog = true
                        routeForPreview = item.route
                    },
                    containerColor = background,
                    onClick = { onRouteClick(item.route.basicData.id) },
                    getTextWorkTime = getTextWorkTime,
                    isHeavyTrains = listRouteState[0].isHeavyTrains,
                    isExtendedServicePhaseTrains = listRouteState[0].isExtendedServicePhaseTrains,
                    isHolidayTimeInRoute = listRouteState[0].isHoliday
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }
        }
    }
}