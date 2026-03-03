package com.z_company.iosapp.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.Route
import com.z_company.domain.navigation.Router
import com.z_company.iosapp.util.TimeFormatter
import com.z_company.iosapp.viewmodel.AllRouteIosViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AllRouteScreen(router: Router) {
    val viewModel: AllRouteIosViewModel = koinInject()
    val routes by viewModel.routes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Все маршруты") },
                navigationIcon = {
                    IconButton(onClick = { router.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            routes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            Icons.Default.DirectionsRailway,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "Нет маршрутов",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Text(
                        text = "Маршрутов: ${routes.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(routes, key = { it.basicData.id }) { route ->
                            AllRouteCard(
                                route = route,
                                onClick = { router.showRouteDetails(route.basicData) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllRouteCard(route: Route, onClick: () -> Unit) {
    val basicData = route.basicData
    val workDuration = run {
        val start = basicData.timeStartWork ?: return@run null
        val end = basicData.timeEndWork ?: return@run null
        if (end > start) end - start else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = basicData.number?.takeIf { it.isNotBlank() } ?: "Без номера",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (basicData.isFavorite) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Избранное",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                workDuration?.let {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = TimeFormatter.formatDuration(it),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            basicData.timeStartWork?.let { start ->
                Text(
                    text = TimeFormatter.formatDateTime(start),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
