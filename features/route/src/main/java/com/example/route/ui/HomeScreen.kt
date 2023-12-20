package com.example.route.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.domain.entities.Route

@Composable
fun HomeScreen(
    onRouteClick: (Route) -> Unit,
    addingRoute: () -> Unit
) {
    Scaffold { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            Column {
                Button(onClick = { onRouteClick(Route(id = "da3647eb-ef19-4f44-9539-ba3a28327bb6")) }) {
                    Text(text = "Click Me")
                }
                Button(onClick =  addingRoute ) {
                    Text(text = "New Route")
                }
            }
        }
    }
}