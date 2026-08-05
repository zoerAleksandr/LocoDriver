package com.z_company.loco_driver.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.announcement.Announcement
import com.z_company.loco_driver.ui.theme.LocoDriverTheme

/**
 * Полноэкранное сообщение-«новость при запуске» (см. [Announcement]).
 * Показывается overlay'ем поверх приложения после сплэша, если у пользователя
 * есть непросмотренное сообщение. Закрывается кнопкой «Понятно» ([onDismiss]).
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp),
            ) {
                // Контент — по центру, с прокруткой на случай длинного текста.
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 96.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = announcement.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                // Кнопка закрытия — прижата к низу.
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
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
