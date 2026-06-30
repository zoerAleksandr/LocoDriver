package com.z_company.route.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import kotlin.math.max

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
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean = true,
    shape: Shape = Shapes.medium,
    positiveColor: Color = MaterialTheme.colorScheme.secondary,
    negativeColor: Color = MaterialTheme.colorScheme.secondary,
    disabledPositiveColor: Color = positiveColor,
    disabledNegativeColor: Color = negativeColor,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    positiveContent: @Composable BoxScope.() -> Unit,
    negativeContent: @Composable BoxScope.() -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    var sideWidth by remember { mutableStateOf(0.dp) }
    val targetOffset = if (checked) sideWidth else 0.dp
    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumb_offset"
    )

    val density = LocalDensity.current

    SubcomposeLayout(modifier = modifier.defaultMinSize(minHeight = androidx.compose.material3.ButtonDefaults.MinHeight)) { constraints ->

        // Изменено: Добавлена проверка на фиксированную ширину (когда modifier.fillMaxWidth() применен, constraints имеют minWidth == maxWidth).
        // Для чего: Чтобы компонент мог растягиваться на всю доступную ширину (fillMaxWidth), разделяя её поровну между двумя сторонами свитча.
        // Если ширина не фиксирована (нет fillMaxWidth), то ширина рассчитывается на основе содержимого, как раньше.
        val hasFixedWidth = constraints.minWidth == constraints.maxWidth && constraints.hasBoundedWidth

        val looseConstraints = constraints.copy(
            minWidth = 0,
            minHeight = 0,
            maxWidth = Constraints.Infinity,
            maxHeight = Constraints.Infinity
        )

        val paddingPerSidePx = with(density) { 4.dp.toPx() }.toInt()
        val sidePaddingPx = paddingPerSidePx * 2

        var sideWidthPx: Int
        var maxContentWidthPx: Int
        var maxContentHeightPx: Int

        if (hasFixedWidth) {
            // Изменено: При фиксированной ширине (fillMaxWidth) рассчитываем sideWidthPx как половину доступной ширины.
            // Для чего: Чтобы свитч занимал всю ширину, а содержимое (positive/negative) могло адаптироваться (например, текст оборачиваться).
            sideWidthPx = constraints.maxWidth / 2
            val contentMaxWidthPx = sideWidthPx - sidePaddingPx
            val contentConstraints = looseConstraints.copy(maxWidth = contentMaxWidthPx)

            val negativePlaceable = subcompose("negative") {
                Box(contentAlignment = Alignment.Center) {
                    negativeContent()
                }
            }[0].measure(contentConstraints)

            val positivePlaceable = subcompose("positive") {
                Box(contentAlignment = Alignment.Center) {
                    positiveContent()
                }
            }[0].measure(contentConstraints)

            maxContentWidthPx = max(negativePlaceable.width, positivePlaceable.width)
            maxContentHeightPx = max(negativePlaceable.height, positivePlaceable.height)
        } else {
            // Оригинальный код для расчета на основе содержимого (без изменений, кроме переменных).
            val negativePlaceable = subcompose("negative") {
                Box(contentAlignment = Alignment.Center) {
                    negativeContent()
                }
            }[0].measure(looseConstraints)

            val positivePlaceable = subcompose("positive") {
                Box(contentAlignment = Alignment.Center) {
                    positiveContent()
                }
            }[0].measure(looseConstraints)

            maxContentWidthPx = max(negativePlaceable.width, positivePlaceable.width)
            maxContentHeightPx = max(negativePlaceable.height, positivePlaceable.height)

            sideWidthPx = maxContentWidthPx + sidePaddingPx
        }

        // Изменено: totalWidthPx теперь зависит от hasFixedWidth: если фиксирована, используем constraints.maxWidth; иначе - на основе sideWidthPx * 2.
        // Для чего: Чтобы при fillMaxWidth свитч занимал всю ширину, а без него - минимальную на основе содержимого.
        val totalWidthPx = if (hasFixedWidth) constraints.maxWidth else sideWidthPx * 2

        val minHeightPx = with(density) { androidx.compose.material3.ButtonDefaults.MinHeight.toPx() }.toInt()
        val totalHeightPx = max(maxContentHeightPx + sidePaddingPx, minHeightPx) // adding vertical padding if needed

        val newSideWidth = with(density) { sideWidthPx.toDp() }
        if (sideWidth != newSideWidth) {
            sideWidth = newSideWidth
        }

        val indicatorColor = if (enabled) {
            if (checked) positiveColor else negativeColor
        } else {
            if (checked) disabledPositiveColor else disabledNegativeColor
        }

        val mainConstraints = constraints.copy(
            minWidth = totalWidthPx,
            maxWidth = totalWidthPx,
            minHeight = totalHeightPx,
            maxHeight = totalHeightPx
        )

        val mainPlaceable = subcompose("main") {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .border(
                        width = 0.5.dp,
                        color = borderColor,
                        shape = shape
                    )
                    .background(MaterialTheme.colorScheme.background)
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
                // индикатор
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(animatedOffset)
                    )
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxHeight()
                            .width(with(density) { sideWidthPx.toDp() }),
                        elevation = CardDefaults.elevatedCardElevation(
                            defaultElevation = 3.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = indicatorColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {}
                }
                // контент
                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(with(density) { sideWidthPx.toDp() })
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        negativeContent()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(with(density) { sideWidthPx.toDp() })
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        positiveContent()
                    }
                }
            }
        }[0].measure(mainConstraints)

        layout(totalWidthPx, totalHeightPx) {
            mainPlaceable.placeRelative(0, 0)
        }
    }
}