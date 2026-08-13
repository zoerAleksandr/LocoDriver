package com.z_company.loco_driver.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.z_company.domain.entities.announcement.Announcement
import com.z_company.loco_driver.ui.theme.LocoDriverTheme

/**
 * Полноэкранное сообщение при запуске (см. [Announcement]). Показывается
 * overlay'ем поверх приложения после сплэша, если есть непросмотренное
 * сообщение. Закрывается кнопкой или крестиком ([onDismiss]).
 *
 * Два типа экрана (диспетчеризуются по [Announcement.type]):
 * - [Announcement.TYPE_NEWS] — новость: hero-иконка, заголовок, текст;
 * - [Announcement.TYPE_UPDATE] — обновление: карусель фич
 *   (картинка → название → описание, «Далее»/«Понятно»), строго на одном
 *   экране без скролла.
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

/**
 * Вариант «Новость»: hero-иконка в тональном скруглённом квадрате, заголовок,
 * текст и pill-кнопка снизу.
 */
@Composable
private fun AnnouncementNewsContent(
    announcement: Announcement,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
    ) {
        // Крестик закрытия — верхний правый угол.
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                contentDescription = "Закрыть",
                tint = cs.onSurfaceVariant,
            )
        }

        // Контент — по центру, с прокруткой на случай длинного текста.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 96.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            // Hero-иконка в тональном скруглённом квадрате.
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
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = announcement.body,
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        // Кнопка закрытия — прижата к низу, pill-формы.
        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .padding(bottom = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Text(
                text = "Понятно",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Вариант «Обновление»: карусель фич. Одна фича на экране —
 * картинка сверху → название → описание, снизу pill-кнопка «Далее»
 * (если есть ещё фичи) или «Понятно» (на последней). Строго на одном экране,
 * без скролла: описание при нехватке места обрезается, кнопка видна всегда.
 */
@Composable
private fun AnnouncementUpdateContent(
    announcement: Announcement,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val features = announcement.features
    var index by remember { mutableIntStateOf(0) }
    val current = features[index.coerceIn(0, features.lastIndex)]
    val isLast = index >= features.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(48.dp)) // место под крестик

            // Картинка фичи — гибкая область сверху.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.25f)
                    .heightIn(min = 120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cs.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                if (current.imageUrl != null) {
                    AsyncImage(
                        model = current.imageUrl,
                        contentDescription = current.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Text(
                text = current.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground,
                modifier = Modifier.padding(top = 20.dp),
            )

            // Описание — гибкая область; при нехватке места обрезается (ellipsis).
            Text(
                text = current.description,
                style = MaterialTheme.typography.bodyLarge,
                color = cs.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .padding(top = 12.dp),
            )

            // Индикатор-точки (если фич больше одной).
            if (features.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    features.indices.forEach { i ->
                        val active = i == index
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

            Button(
                onClick = { if (isLast) onDismiss() else index++ },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .padding(top = 16.dp, bottom = 16.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(
                    text = if (isLast) "Понятно" else "Далее",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Крестик закрытия — верхний правый угол, поверх контента.
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                contentDescription = "Закрыть",
                tint = cs.onSurfaceVariant,
            )
        }
    }
}
