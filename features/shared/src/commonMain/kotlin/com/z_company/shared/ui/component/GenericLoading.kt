package com.z_company.shared.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun GenericLoading(
    message: String? = null,
    onCloseClick: () -> Unit = {}
) {
    Column(
        Modifier
            .zIndex(1f)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                Modifier.size(48.dp)
            )
            IconButton(
                modifier = Modifier.zIndex(1f),
                onClick = onCloseClick
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = message ?: "Пожалуйста подождите…",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun GenericLoadingValue() {
    CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        strokeWidth = 2.dp
    )
}
