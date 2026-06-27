package com.z_company.loco_driver.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.loco_driver.ui.theme.MashinistTheme

@Composable
fun StubScreen(
    title: String,
    description: String = "Этот раздел находится в разработке",
    onBack: (() -> Unit)? = null,
) {
    val colors = MashinistTheme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        if (onBack != null) {
            MashinistTopBar(title = title, onBack = onBack)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.text,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
            }
        }
    }
}
