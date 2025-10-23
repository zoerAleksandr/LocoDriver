package com.z_company.route.component

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes

/**
 * A composable function that creates a switch with text labels for both states.
 *
 * @param checked The current checked state of the switch.
 * @param onCheckedChange Callback invoked when the switch state changes.
 * @param modifier The modifier to be applied to the switch.
 * @param enabled Whether the switch is enabled and can be interacted with.
 * @param shape The shape of the switch. Default is a rounded corner shape with a radius of 10.dp.
 * @param color The color of the switch's active background. Default is the primary container color from the current MaterialTheme.
 * @param borderColor The color of the switch's border. Default is the primary container color from the current MaterialTheme.
 * @param textColor The color of the text. Default is the onPrimaryContainer color from the current MaterialTheme color scheme.
 * @param disabledColor The color of the active background when the switch is disabled.
 * @param disabledBorderColor The color of the border when the switch is disabled.
 * @param disabledTextColor The color of the text when the switch is disabled.
 * @param positiveText The text to display on the 'on' side of the switch. Default is "Yes".
 * @param negativeText The text to display on the 'off' side of the switch. Default is "No".
 * @param interactionSource The MutableInteractionSource representing the stream of interactions with this switch.
 */
@Composable
fun SwitchApp(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    positiveColor: Color = MaterialTheme.colorScheme.secondary,
    negativeColor: Color = MaterialTheme.colorScheme.secondary,
    disabledPositiveColor: Color = positiveColor,
    disabledNegativeColor: Color = negativeColor,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    positiveContent: @Composable BoxScope.() -> Unit,
    negativeContent: @Composable BoxScope.() -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    var width by remember { mutableStateOf(0.dp) }
    var height by remember { mutableStateOf(ButtonDefaults.MinHeight) }

    val thumbOffset by remember(checked, width) {
        derivedStateOf {
            if (checked) width - (width / 2) else 0.dp
        }
    }

    val animatedThumbOffset by animateDpAsState(
        targetValue = thumbOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumb_offset"
    )

    val localDensity = LocalDensity.current
    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = ButtonDefaults.MinHeight,
                minHeight = ButtonDefaults.MinHeight
            )
            .onGloballyPositioned { coordinates ->
                width = with(localDensity) {
                    coordinates.size.width.toDp()
                }
                height = with(localDensity) {
                    coordinates.size.height.toDp()
                }
            }
            .shadow(elevation = 3.dp, shape = shape)
            .height(height)
            .clip(shape = shape)
            .border(
                width = 0.5.dp,
                color = borderColor,
                shape = shape
            )
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        interactionSource = interactionSource,
                        indication = null,
                        onValueChange = onCheckedChange
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(animatedThumbOffset)
            )
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .height(height)
                    .width(width / 2)
                    .clip(shape = shape.copy(CornerSize(10.dp)))
                    .shadow(elevation = 3.dp, shape = shape.copy(CornerSize(10.dp)))
                    .background(MaterialTheme.colorScheme.secondary)
                    .border(
                        shape = shape.copy(CornerSize(10.dp)),
                        color = borderColor,
                        width = 0.5.dp
                    )
//                    .alpha(animatedAlpha)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                negativeContent()
            }
            Box(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                positiveContent()
            }
        }
    }
}