package com.example.route.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.core.ui.theme.LocoAppTheme
import com.example.core.ui.theme.custom.AppTypography


@Composable
fun CircularIndicator(
    canvasSize: Dp = 300.dp,
    valueHour: Int = 0,
    valueMinute: Int = 0,
    maxIndicatorValue: Int = 180,
    backgroundIndicatorColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    backgroundIndicatorStrokeWidth: Float = 50f,
    foregroundIndicatorColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    foregroundIndicatorStrokeWidth: Float = 70f,
) {
    var allowedIndicatorValue by remember {
        mutableIntStateOf(maxIndicatorValue)
    }

    allowedIndicatorValue = if (valueHour <= maxIndicatorValue) {
        valueHour
    } else {
        maxIndicatorValue
    }

    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }

    val animatedHour by animateIntAsState(
        targetValue = hours,
        animationSpec = tween(1000),
        label = ""
    )

    val animatedMinute by animateIntAsState(
        targetValue = valueMinute,
        animationSpec = tween(1000),
        label = ""
    )

    val animatorIndicatorValue = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(key1 = allowedIndicatorValue) {
        hours = valueHour
        minutes = valueMinute
        animatorIndicatorValue.animateTo(allowedIndicatorValue.toFloat())
    }

    val percent = (animatorIndicatorValue.value / maxIndicatorValue) * 100

    val sweepAngle by animateFloatAsState(
        targetValue = (2.4 * percent).toFloat(),
        animationSpec = tween(1000), label = ""
    )

    ConstraintLayout(
        modifier = Modifier
            .size(canvasSize)
            .drawBehind {
                val componentSize = size / 1.4f
                backgroundIndicator(
                    componentSize = componentSize,
                    indicatorColor = backgroundIndicatorColor,
                    indicatorStrokeWidth = backgroundIndicatorStrokeWidth
                )
                foregroundIndicator(
                    sweepAngle = sweepAngle,
                    componentSize = componentSize,
                    indicatorColor = foregroundIndicatorColor,
                    indicatorStrokeWidth = foregroundIndicatorStrokeWidth
                )
            },
    ) {
        val (embeddedText, externalText) = createRefs()

        EmbeddedText(
            modifier = Modifier
                .constrainAs(embeddedText) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            valueHour = animatedHour,
            valueMinute = animatedMinute
        )

        Text(
            modifier = Modifier
                .constrainAs(externalText) {
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
                .padding(bottom = canvasSize / 5),
            text = "$maxIndicatorValue",
            style = MaterialTheme.typography.bodyLarge
        )
    }

}

fun DrawScope.backgroundIndicator(
    componentSize: Size,
    indicatorColor: Color,
    indicatorStrokeWidth: Float
) {
    drawArc(
        size = componentSize,
        color = indicatorColor,
        startAngle = 150f,
        sweepAngle = 240f,
        useCenter = false,
        style = Stroke(
            width = indicatorStrokeWidth,
            cap = StrokeCap.Round
        ),
        topLeft = Offset(
            x = (size.width - componentSize.width) / 2f,
            y = (size.height - componentSize.height) / 2f
        )
    )
}

fun DrawScope.foregroundIndicator(
    sweepAngle: Float,
    componentSize: Size,
    indicatorColor: Color,
    indicatorStrokeWidth: Float
) {
    drawArc(
        size = componentSize,
        color = indicatorColor,
        startAngle = 150f,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(
            width = indicatorStrokeWidth,
            cap = StrokeCap.Round
        ),
        topLeft = Offset(
            x = (size.width - componentSize.width) / 2f,
            y = (size.height - componentSize.height) / 2f
        )
    )
}

@Composable
fun EmbeddedText(
    modifier: Modifier,
    valueHour: Int,
    valueMinute: Int
) {
    val hour: String = if (valueHour.toString().length == 1) {
        "0$valueHour"
    } else {
        valueHour.toString()
    }

    val minute: String = if (valueMinute.toString().length == 1) {
        "0$valueMinute"
    } else {
        valueMinute.toString()
    }

    Text(
        modifier = modifier,
        text = "$hour : $minute",
        style = AppTypography.getType().headlineLarge
    )
}

@Composable
@Preview
private fun PrevCircularIndicator() {
    LocoAppTheme {
        CircularIndicator(valueHour = 1)
    }
}