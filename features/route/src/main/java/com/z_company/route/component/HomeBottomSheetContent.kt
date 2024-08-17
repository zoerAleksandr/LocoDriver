package com.z_company.route.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.GenericError
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.route.R
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeBottomSheetContent(
    routeListState: ResultState<List<Route>>,
    reloadRoute: () -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onRouteClick: (String) -> Unit,
    onRouteLongClick: (Route) -> Unit,
    isExpand: Boolean,
) {
    var requiredSize by remember {
        mutableStateOf(24.sp)
    }

    fun changingTextSize(value: TextUnit) {
        if (requiredSize > value) {
            requiredSize = value
        }
    }

    AsyncData(resultState = routeListState, errorContent = {
        GenericError(onDismissAction = reloadRoute)
    }) { routeList ->
        routeList?.let {
            if (routeList.isEmpty()) {
                EmptyList()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(routeList, key = { route -> route.basicData.id }) { route ->
                        var background = MaterialTheme.colorScheme.secondaryContainer

                        if (route.basicData.timeStartWork!! > Calendar.getInstance().timeInMillis) {
                            background = MaterialTheme.colorScheme.surfaceBright
                        } else {
                            if (route.isTransition()) {
                                background = MaterialTheme.colorScheme.surfaceDim
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ItemHomeScreen(
                            modifier = Modifier.animateItemPlacement(),
                            route = route,
                            isExpand = isExpand,
                            onDelete = onDeleteRoute,
                            requiredSizeText = requiredSize,
                            changingTextSize = ::changingTextSize,
                            onLongClick = { onRouteLongClick(route) },
                            containerColor = background,
                            onClick = { onRouteClick(route.basicData.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun EmptyList() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = stringResource(id = R.string.msg_empty_route_list),
            style = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
        )
    }
}
