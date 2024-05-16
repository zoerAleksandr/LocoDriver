package com.z_company.route.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
fun TopSnackbar(snackBarData: SnackbarData) {
    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = {
            snackBarData.dismiss()
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Snackbar(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.TopCenter),
                snackbarData = snackBarData,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}