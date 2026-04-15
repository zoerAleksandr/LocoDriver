package com.z_company.route.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Shimmer-кисть — анимированный градиент для эффекта пульсации скелетона.
// ---------------------------------------------------------------------------
@Composable
private fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation, y = translateAnimation)
    )
}

// ---------------------------------------------------------------------------
// Скелетон одной карточки маршрута (повторяет форму ItemHomeScreen).
// ---------------------------------------------------------------------------
@Composable
fun SkeletonItemHomeScreen(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 65.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            // Главная строка: время работы (80%) + продолжительность (20%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(0.2f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
            // Вторая строка-подсказка (маршрут / номер поезда)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Полноэкранный скелетон HomeScreen: заголовок-пейджер + 4 карточки маршрутов.
// ---------------------------------------------------------------------------
@Composable
fun HomeScreenSkeleton(contentPadding: PaddingValues = PaddingValues()) {
    val brush = shimmerBrush()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Заглушка для HorizontalPager (MainInfo + детальные карточки)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(brush)
                )
                // Заглушка для LinearPagerIndicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .width(if (index == 0) 16.dp else 8.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (index == 0)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }
        }

        // 4 заглушки карточек маршрутов
        items(4) {
            SkeletonItemHomeScreen(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
