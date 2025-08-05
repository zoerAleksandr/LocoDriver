package com.z_company.route.component

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography

@Composable
fun PieChart(
    data: Map<String, Long>,
    radiusOuter: Dp,
    chartBarWidth: Dp = radiusOuter * 0.3f,
    animDuration: Int = 1000,
    centerText: String = "",
    nightTime: Long
) {

    val totalSum = data.values.sum()
    val floatValue = mutableListOf<Float>()
    val floatValueDayNight = mutableListOf<Float>()

    // To set the value of each Arc according to
    // the value given in the data, we have used a simple formula.
    // For a detailed explanation check out the Medium Article.
    // The link is in the about section and readme file of this GitHub Repository
    data.values.forEachIndexed { index, values ->
        floatValue.add(index, 360 * values.toFloat() / totalSum.toFloat())
    }
    val nightFloat = 360 * nightTime.toFloat() / totalSum.toFloat()
    val dayFloat = 360 - nightFloat

    floatValueDayNight.add(0, nightFloat)
    floatValueDayNight.add(1, dayFloat)

    // add the colors as per the number of data(no. of pie chart entries)
    // so that each data will get a color
    val redOrange = Color(0xFFf1642e)
    val purple = Color(0xFF504e76)
    val green = Color(0xFFa3b565)


    val colors = listOf(
        redOrange,
        purple,
        green
    )

    val colorsDayNight = listOf(
        Color.Gray,
        Color.Yellow
    )



    var animationPlayed by remember { mutableStateOf(false) }


    // it is the diameter value of the Pie
    val animateSize by animateFloatAsState(
        targetValue = if (animationPlayed) ((radiusOuter.value + (chartBarWidth.value / 2)) * 2f) else 0f,
        animationSpec = tween(
            durationMillis = animDuration,
            delayMillis = 0,
            easing = LinearOutSlowInEasing
        )
    )

    // if you want to stabilize the Pie Chart you can use value -90f
    // 90f is used to complete 1/4 of the rotation
    val animateRotation by animateFloatAsState(
        targetValue = if (animationPlayed) 90f * 11f else 0f,
        animationSpec = tween(
            durationMillis = animDuration,
            delayMillis = 0,
            easing = LinearOutSlowInEasing
        )
    )

    // to play the animation only once when the function is Created or Recomposed
    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(animateSize.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .offset { IntOffset.Zero }
                        .size(radiusOuter * 2f)
                        .rotate(animateRotation)
                ) {
                    var lastValue = 0f

                    // draw each Arc for each data entry in Pie Chart
                    floatValue.forEachIndexed { index, value ->
                        drawArc(
                            color = colors[index],
                            lastValue,
                            value,
                            useCenter = false,
                            style = Stroke(chartBarWidth.toPx(), cap = StrokeCap.Butt)
                        )
                        lastValue += value
                    }
                }
                Text(
                    text = centerText,
                    modifier = Modifier
                        .width(Dp(radiusOuter.value * 2 - chartBarWidth.value)),
                    textAlign = TextAlign.Center,
                    style = AppTypography.getType().bodyLarge,
                )
            }
            Box(
                modifier = Modifier
                    .size((animateSize * 0.7).dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .offset { IntOffset.Zero }
                        .size((radiusOuter * 0.7f) * 2f)
                        .rotate(animateRotation)
                ) {
                    var lastValue = 0f
                    // draw each Arc for each data entry in Pie Chart
                    floatValueDayNight.forEachIndexed { index, value ->
                        drawArc(
                            color = colorsDayNight[index],
                            lastValue,
                            value,
                            useCenter = false,
                            style = Stroke(chartBarWidth.toPx(), cap = StrokeCap.Butt)
                        )
                        lastValue += value
                    }
                }
            }
        }

        // To see the data in more structured way
        // Compose Function in which Items are showing data
        DetailsPieChart(
            data = data,
            colors = colors
        )
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsPieChart(
    data: Map<String, Long>,
    colors: List<Color>
) {
    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        data.values.forEachIndexed { index, value ->
            DetailsPieChartItem(
                data = Pair(data.keys.elementAt(index), value),
                color = colors[index]
            )
            if (data.size > 1 && index < data.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DetailsPieChartItem(
    data: Pair<String, Long>,
    color: Color
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = color,
                        shape = Shapes.medium
                    )
                    .size(12.dp)
            )
            Text(
                text = data.first,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.getType().bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Text(
            text = "${data.second}:00",
            maxLines = 1,
            style = AppTypography.getType().bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

//    Surface(
//        modifier = Modifier
//            .padding(vertical = 10.dp, horizontal = 40.dp),
//        color = Color.Transparent
//    ) {
//
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//
//            Box(
//                modifier = Modifier
//                    .background(
//                        color = color,
//                        shape = CircleShape
//                    )
//                    .size(height)
//            )
//
//            Column(modifier = Modifier.fillMaxWidth()) {
//                Text(
//                    modifier = Modifier.padding(start = 15.dp),
//                    text = data.first,
//                    color = MaterialTheme.colorScheme.onBackground
//                )
//                Text(
//                    modifier = Modifier.padding(start = 15.dp),
//                    text = "${data.second} часов",
//                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
//                )
//            }
//
//        }
//
//    }
}


//@OptIn(ExperimentalLayoutApi::class)
//@Composable
//fun PieChart(
//    modifier: Modifier = Modifier,
//    radius: Float,
//    innerRadius: Float = radius / 2f,
//    transparentWidth: Float = 70f,
//    input: List<PieChartInput>,
//    centerText: String = ""
//) {
//    var circleCenter by remember {
//        mutableStateOf(Offset.Zero)
//    }
//
//    var inputList by remember {
//        mutableStateOf(input)
//    }
//    var isCenterTapped by remember {
//        mutableStateOf(false)
//    }
//
////    Column(
////        modifier = modifier
////    ) {
//    Box(
//        modifier = modifier,
//        contentAlignment = Alignment.Center
//    ) {
//        val backgroundColor = MaterialTheme.colorScheme.background
//        Canvas(
//            modifier = Modifier
//                .fillMaxSize()
//                .pointerInput(true) {
//                    detectTapGestures(
//                        onTap = { offset ->
//                            val tapAngleInDegrees = (-atan2(
//                                x = circleCenter.y - offset.y,
//                                y = circleCenter.x - offset.x
//                            ) * (180f / PI).toFloat() - 90f).mod(360f)
//                            val centerClicked = if (tapAngleInDegrees < 90) {
//                                offset.x < circleCenter.x + innerRadius && offset.y < circleCenter.y + innerRadius
//                            } else if (tapAngleInDegrees < 180) {
//                                offset.x > circleCenter.x - innerRadius && offset.y < circleCenter.y + innerRadius
//                            } else if (tapAngleInDegrees < 270) {
//                                offset.x > circleCenter.x - innerRadius && offset.y > circleCenter.y - innerRadius
//                            } else {
//                                offset.x < circleCenter.x + innerRadius && offset.y > circleCenter.y - innerRadius
//                            }
//
//                            if (centerClicked) {
//                                inputList = inputList.map {
//                                    it.copy(isTapped = !isCenterTapped)
//                                }
//                                isCenterTapped = !isCenterTapped
//                            } else {
//                                val anglePerValue = 360f / input.sumOf {
//                                    it.value
//                                }
//                                var currAngle = 0f
//                                inputList.forEach { pieChartInput ->
//
//                                    currAngle += pieChartInput.value * anglePerValue
//                                    if (tapAngleInDegrees < currAngle) {
//                                        val description = pieChartInput.description
//                                        inputList = inputList.map {
//                                            if (description == it.description) {
//                                                it.copy(isTapped = !it.isTapped)
//                                            } else {
//                                                it.copy(isTapped = false)
//                                            }
//                                        }
//                                        return@detectTapGestures
//                                    }
//                                }
//                            }
//                        }
//                    )
//                }
//        ) {
//            val width = size.width
//            val height = size.height
//            circleCenter = Offset(x = width / 2f, y = height / 2f)
//
//            val totalValue = input.sumOf {
//                it.value
//            }
//            val anglePerValue = 360f / totalValue
//            var currentStartAngle = 0f
//
//            inputList.forEach { pieChartInput ->
//                val scale = if (pieChartInput.isTapped) 1.1f else 1.0f
//                val angleToDraw = pieChartInput.value * anglePerValue
//                scale(scale) {
//                    drawArc(
//                        color = pieChartInput.color,
//                        startAngle = currentStartAngle,
//                        sweepAngle = angleToDraw,
//                        useCenter = true,
//                        size = Size(
//                            width = radius * 2f,
//                            height = radius * 2f
//                        ),
//                        topLeft = Offset(
//                            (width - radius * 2f) / 2f,
//                            (height - radius * 2f) / 2f
//                        )
//                    )
//                    currentStartAngle += angleToDraw
//                }
//                var rotateAngle = currentStartAngle - angleToDraw / 2f - 90f
//                var factor = 1f
//                if (rotateAngle > 90f) {
//                    rotateAngle = (rotateAngle + 180).mod(360f)
//                    factor = -0.92f
//                }
//
//                val percentage = (pieChartInput.value / totalValue.toFloat() * 100).toInt()
//
//                drawContext.canvas.nativeCanvas.apply {
//                    if (percentage > 3) {
//                        rotate(rotateAngle) {
//                            drawText(
//                                "$percentage %",
//                                circleCenter.x,
//                                circleCenter.y + (radius - (radius - innerRadius) / 2f) * factor,
//                                Paint().apply {
//                                    textSize = 13.sp.toPx()
//                                    textAlign = Paint.Align.CENTER
//                                    color = white.toArgb()
//                                }
//                            )
//                        }
//                    }
//                }
//                if (pieChartInput.isTapped) {
//                    val tabRotation = currentStartAngle - angleToDraw - 90f
//                    rotate(tabRotation) {
//                        drawRoundRect(
//                            topLeft = circleCenter,
//                            size = Size(12f, radius * 1.2f),
//                            color = backgroundColor,
//                            cornerRadius = CornerRadius(15f, 15f)
//                        )
//                    }
//                    rotate(tabRotation + angleToDraw) {
//                        drawRoundRect(
//                            topLeft = circleCenter,
//                            size = Size(12f, radius * 1.2f),
//                            color = backgroundColor,
//                            cornerRadius = CornerRadius(15f, 15f)
//                        )
//                    }
////                    rotate(rotateAngle){
////                        drawContext.canvas.nativeCanvas.apply {
////                            drawText(
////                                "${pieChartInput.description}: ${pieChartInput.value}",
////                                circleCenter.x,
////                                circleCenter.y + radius*1.3f*factor,
////                                Paint().apply {
////                                    textSize = 22.sp.toPx()
////                                    textAlign = Paint.Align.CENTER
////                                    color = white.toArgb()
////                                    isFakeBoldText = true
////                                }
////                            )
////                        }
////                    }
//                }
//            }
//
//            if (inputList.first().isTapped) {
//                rotate(-90f) {
//                    drawRoundRect(
//                        topLeft = circleCenter,
//                        size = Size(12f, radius * 1.2f),
//                        color = backgroundColor,
//                        cornerRadius = CornerRadius(15f, 15f)
//                    )
//                }
//            }
//            drawContext.canvas.nativeCanvas.apply {
//                drawCircle(
//                    circleCenter.x,
//                    circleCenter.y,
//                    innerRadius,
//                    Paint().apply {
//                        color = white.copy(alpha = 0.6f).toArgb()
//                        setShadowLayer(10f, 0f, 0f, gray.toArgb())
//                    }
//                )
//            }
//
//            drawCircle(
//                color = backgroundColor,
//                radius = innerRadius + transparentWidth / 2f
//            )
//
//        }
//        Text(
//            text = centerText,
//            modifier = Modifier
//                .width(Dp(innerRadius / 1.5f))
//                .padding(25.dp),
//            textAlign = TextAlign.Center
//        )
//    }
////        FlowRow(
////            modifier = Modifier
////                .padding(top = 40.dp)
////                .fillMaxWidth(),
////            horizontalArrangement = Arrangement.spacedBy(8.dp)
////        ) {
////            input.forEachIndexed { index, value ->
////                DetailsPieChartItem(
////                    data = Pair(value.description, value.value),
////                    color = value.color
////                )
////            }
////        }
////    }
//}

data class PieChartInput(
    val color: Color,
    val value: Int,
    val description: String,
    val isTapped: Boolean = false
)

val white = Color(0xFFF3F3F3)
val gray = Color(0xFF3F3F3F)