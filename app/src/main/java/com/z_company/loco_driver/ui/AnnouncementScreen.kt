package com.z_company.loco_driver.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.z_company.domain.entities.announcement.Announcement
import com.z_company.loco_driver.ui.theme.LocoDriverTheme
import kotlinx.coroutines.launch

/**
 * Полноэкранное сообщение при запуске (см. [Announcement]). Показывается
 * overlay'ем поверх приложения после сплэша, если есть непросмотренное
 * сообщение. Закрывается кнопкой или крестиком ([onDismiss]).
 *
 * Два типа экрана (диспетчеризуются по [Announcement.type]):
 * - [Announcement.TYPE_NEWS] — новость: заголовок и текст, при наличии
 *   [Announcement.imageUrl] — картинка сверху, иначе текст центрируется;
 * - [Announcement.TYPE_UPDATE] — обновление: карусель фич
 *   (заголовок обновления → картинка → название → описание,
 *   «Далее»/«Понятно»), строго на одном экране без скролла.
 *
 * Общее правило: если у экрана/фичи нет картинки — текст центрируется.
 */
@Composable
fun AnnouncementScreen(
    announcement: Announcement,
    onDismiss: () -> Unit,
) {
    LocoDriverTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (announcement.type == Announcement.TYPE_UPDATE &&
                announcement.features.isNotEmpty()
            ) {
                AnnouncementUpdateContent(announcement, onDismiss)
            } else {
                AnnouncementNewsContent(announcement, onDismiss)
            }
        }
    }
}

/** Крестик закрытия — верхний правый угол (для использования внутри Box). */
@Composable
private fun BoxCloseButton(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onDismiss, modifier = modifier) {
        Icon(
            painter = painterResource(com.z_company.core.R.drawable.ic_clear),
            contentDescription = "Закрыть",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Pill-кнопка во всю ширину, прижатая к низу (жёсткий текст). */
@Composable
private fun BottomPillButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Блок «картинка сверху → заголовок → текст» (когда картинка есть). Занимает
 * гибкую высоту: картинка и текст делят место через weight (картинка —
 * бо́льшая доля, текст уходит ниже и обрезается при нехватке места).
 */
@Composable
private fun ColumnScope.ImageTitleTextBlock(
    imageUrl: String?,
    title: String,
    text: String,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1.9f)
            .heightIn(min = 160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = cs.onBackground,
        modifier = Modifier.padding(top = 28.dp),
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = cs.onSurfaceVariant,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .weight(0.55f)
            .padding(top = 12.dp),
    )
}

/**
 * Центрированный блок «заголовок + текст» (когда картинки нет). Текст выровнен
 * по центру. Занимает гибкую высоту (weight), при переполнении обрезается.
 */
@Composable
private fun ColumnScope.CenteredTitleTextBlock(
    title: String,
    text: String,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

/**
 * Вариант «Новость»: заголовок + текст. С картинкой — картинка сверху
 * (без скролла), без картинки — hero-иконка и центрированный текст (со скроллом
 * на случай длинного сообщения).
 */
@Composable
private fun AnnouncementNewsContent(
    announcement: Announcement,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val hasImage = announcement.imageUrl != null
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
    ) {
        if (hasImage) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(48.dp)) // место под крестик
                ImageTitleTextBlock(
                    imageUrl = announcement.imageUrl,
                    title = announcement.title,
                    text = announcement.body,
                )
                BottomPillButton(
                    label = "Понятно",
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                )
            }
        } else {
            // Контент по центру, текст центрирован, со скроллом для длинного текста.
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(cs.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(com.z_company.route.R.drawable.info_24px),
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    text = announcement.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            BottomPillButton(
                label = "Понятно",
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
        }

        BoxCloseButton(onDismiss, Modifier.align(Alignment.TopEnd))
    }
}

/**
 * Вариант «Обновление»: карусель фич, листается свайпом в обе стороны
 * ([HorizontalPager]) и кнопкой «Далее». Сверху на каждой странице — общий
 * заголовок обновления ([Announcement.title], например «Обновление 3.0.0»),
 * ниже — картинка (если есть; иначе текст центрируется) → название → описание
 * текущей фичи, снизу индикатор-точки и pill-кнопка «Далее» (если есть ещё
 * фичи) или «Понятно» (на последней). Строго на одном экране без скролла:
 * описание при нехватке места обрезается, кнопка видна всегда.
 *
 * Картинки всех фич ставятся в очередь на загрузку в Coil сразу при первом
 * показе карусели ([LaunchedEffect]), а не по одной при перелистывании — так
 * при свайпе между уже подгруженными фичами не видно задержки на загрузку.
 */
@Composable
private fun AnnouncementUpdateContent(
    announcement: Announcement,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val features = announcement.features
    val pagerState = rememberPagerState(pageCount = { features.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage >= features.lastIndex

    // Предзагрузка картинок всех фич в кэш Coil — чтобы при свайпе между
    // страницами карусели не было заметной задержки на загрузку.
    LaunchedEffect(features) {
        features.forEach { feature ->
            feature.imageUrl?.let { url ->
                context.imageLoader.enqueue(
                    ImageRequest.Builder(context).data(url).build()
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(48.dp)) // место под крестик

            if (announcement.title.isNotBlank()) {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                val feature = features[page]
                Column(modifier = Modifier.fillMaxSize()) {
                    if (feature.imageUrl != null) {
                        ImageTitleTextBlock(
                            imageUrl = feature.imageUrl,
                            title = feature.title,
                            text = feature.description,
                        )
                    } else {
                        CenteredTitleTextBlock(
                            title = feature.title,
                            text = feature.description,
                        )
                    }
                }
            }

            // Индикатор-точки (если фич больше одной).
            if (features.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    features.indices.forEach { i ->
                        val active = i == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (active) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) cs.primary
                                    else cs.onSurfaceVariant.copy(alpha = 0.3f)
                                ),
                        )
                    }
                }
            }

            BottomPillButton(
                label = if (isLast) "Понятно" else "Далее",
                onClick = {
                    if (isLast) {
                        onDismiss()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
            )
        }

        BoxCloseButton(onDismiss, Modifier.align(Alignment.TopEnd))
    }
}
