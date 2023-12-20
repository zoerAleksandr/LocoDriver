package com.example.route.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.core.ResultState
import com.example.core.ui.component.AsyncData
import com.example.core.ui.component.GenericError
import com.example.domain.entities.Route
import com.example.route.R
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun DetailsScreen(
    routeDetailState: ResultState<Route?>,
    minTimeRest: Long,
    onEditClick: (Route) -> Unit,
    onBackPressed: () -> Unit,
) {
    AsyncData(routeDetailState) { route ->
        if (route == null) {
            GenericError(onDismissAction = onBackPressed)
        } else {
            val pagerState = rememberPagerState(
                pageCount = 5,
                initialPage = 0
            )
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(text = route.number ?: "")
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackPressed) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Назад"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { onEditClick(route) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Изменить"
                                )
                            }
                        }
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Tabs(pagerState = pagerState)
                    TabContent(
                        pagerState = pagerState,
                        route = route,
                        minTimeRest = minTimeRest
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
private fun Tabs(pagerState: PagerState) {
    val tabsLabel = listOf(
        stringResource(id = R.string.work_time),
        stringResource(id = R.string.locomotive),
        stringResource(id = R.string.train),
        stringResource(id = R.string.passenger),
        stringResource(id = R.string.notes)
    )
    CustomScrollableTabRow(
        tabs = tabsLabel,
        selectedTabIndex = pagerState.currentPage,
        pagerState = pagerState
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun TabContent(
    pagerState: PagerState,
    route: Route,
    minTimeRest: Long
) {
    HorizontalPager(state = pagerState) { page ->
        when (page) {
            0 -> {
                Box(Modifier.fillMaxSize()) {
                    Text(text = route.id)
                }
            }
//                WorkTimeScreen(navController, state, minTimeRest)
            1 -> {}
//                LocoScreen(state)
            2 -> {}
//                TrainScreen(state)
            3 -> {}
//                PassengerScreen(state)
            4 -> {}
//                NotesScreen(state, navController)
        }
    }
}