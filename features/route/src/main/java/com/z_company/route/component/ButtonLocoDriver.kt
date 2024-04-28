package com.z_company.route.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.z_company.core.ui.theme.Shapes

@Composable
fun ButtonLocoDriver(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable (RowScope.() -> Unit)
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = Shapes.medium
    ) {
        content()
    }
}