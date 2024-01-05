package com.example.route.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.core.ResultState
import com.example.core.ui.component.AsyncData
import com.example.core.ui.component.GenericError
import com.example.domain.entities.route.BasicData
import com.example.domain.entities.route.Route
import com.example.route.R
import com.example.route.ui.changeAlphaWithOffset
import com.example.route.ui.changeDpWithScroll

@Composable
fun HomeBottomSheetContent(
    routeListState: ResultState<List<Route>>,
    reloadRoute: () -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onRouteClick: (BasicData) -> Unit,
    offset: Float,
    isExpand: Boolean
) {
    AsyncData(resultState = routeListState, errorContent = {
        GenericError(onDismissAction = reloadRoute)
    }) { routeList ->
        routeList?.let {
            if (routeList.isEmpty()) {
                EmptyList()
            } else {
                val paddingTop = changeDpWithScroll(offset, 48, 0).dp
                LazyColumn(
                    modifier = Modifier
                        .padding(top = paddingTop, start = 12.dp, end = 12.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(routeList) { route ->
                        ItemHomeScreen(
                            route = route,
                            isExpand = isExpand,
                            onDelete = onDeleteRoute,
                            alpha = changeAlphaWithOffset(offset)
                        ) {
                            onRouteClick(route.basicData)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun EmptyList() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material.Text(text = stringResource(id = R.string.msg_empty_route_list))
    }
}
