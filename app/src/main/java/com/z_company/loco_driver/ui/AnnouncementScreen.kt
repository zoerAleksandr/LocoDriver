package com.z_company.loco_driver.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.announcement.Announcement
import com.z_company.loco_driver.ui.theme.LocoDriverTheme

/**
 * Полноэкранное сообщение-«новость при запуске» (см. [Announcement]).
 * Показывается overlay'ем поверх приложения после сплэша, если у пользователя
 * есть непросмотренное сообщение. Закрывается кнопкой «Понятно» или крестиком
 * в углу ([onDismiss]).
 *
 * Свёрстан по референсу-варианту «Новость»: hero-иконка в тональном кружке,
 * заголовок, текст и pill-кнопка снизу. Без mono-оверлайна с датой и без
 * нижней карточки со временем — по требованию дизайна.
 */
@Composable
fun AnnouncementScreen(
    announcement: Announcement,
    onDismiss: () -> Unit,
) {
    LocoDriverTheme {
        val cs = MaterialTheme.colorScheme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = cs.background,
        ) {
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
    }
}
