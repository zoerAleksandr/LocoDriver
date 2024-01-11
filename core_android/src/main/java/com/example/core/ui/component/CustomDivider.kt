package com.example.core.ui.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun CustomDivider(
    modifier: Modifier = Modifier,
    color: Color = DividerDefaults.color,
    thickness: Dp = DividerDefaults.Thickness,
    orientation: Orientation
){
    when(orientation){
        Orientation.Vertical -> {
            Divider(modifier = modifier.fillMaxHeight().width(thickness), color = color)
        }
        Orientation.Horizontal -> {
            Divider(modifier = modifier.fillMaxWidth().height(thickness), color = color)
        }
    }
}