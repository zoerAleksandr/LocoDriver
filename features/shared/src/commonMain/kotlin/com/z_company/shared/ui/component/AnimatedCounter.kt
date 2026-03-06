package com.z_company.shared.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedCounter(
    count: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    var oldCount by remember { mutableStateOf(count) }
    SideEffect { oldCount = count }

    Row(modifier = modifier) {
        for (i in count.indices) {
            val oldChar = oldCount.getOrNull(i)
            val newChar = count[i]
            val char = if (oldChar == newChar) oldCount[i] else count[i]
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    slideInVertically { -it } togetherWith slideOutVertically { it }
                },
            ) { c ->
                Text(
                    text = c.toString(),
                    style = style,
                    color = color,
                    softWrap = false,
                )
            }
        }
    }
}
