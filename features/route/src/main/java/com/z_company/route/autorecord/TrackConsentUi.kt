package com.z_company.route.autorecord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Тексты согласия №1 (SCREEN_SPECS §6.9, §17.7 — дословно). */
object TrackConsentTexts {
    const val TITLE = "Сохранять мои треки и историю поездок"
    /** Подпись самого переключателя (заголовок экрана/блока - [TITLE]). */
    const val SWITCH_LABEL = "Сохранять треки"
    const val BODY = "Когда включен режим «АВТО», приложение записывает ваш путь: время, положение и скорость " +
        "в точках следования. Это нужно, чтобы вы могли открыть пройденный путь на карте, посмотреть скорость " +
        "и историю поездок."

    /** Пункты под основным текстом (показываются отдельными абзацами с маркером). */
    val POINTS = listOf(
        "Треки хранятся на вашем устройстве и в зашифрованном виде на сервере LocoDriver. Доступ к ним только у вас, " +
            "администраторы приложения их не видят.",
        "Срок хранения — 180 дней. Избранные треки, отмеченные звездой в разделе «Мои треки», не удаляются " +
            "автоматически.",
    )
    const val NO_CONSENT_NOTE = "Без согласия режим «АВТО» продолжит записывать стоянки и отправления поезда, " +
        "но путь не сохранится."
    const val CONTINUE = "Продолжить"
    const val REVOKE_TITLE = "Отозвать согласие?"
    const val REVOKE_TEXT = "Новые треки сохраняться не будут. Уже записанные треки останутся в разделе «Мои треки» — " +
        "удалить их можно там."
    const val REVOKE_CONFIRM = "Отозвать"
    const val REVOKE_CANCEL = "Отмена"

    // Согласие №2 (SCREEN_SPECS §6.9, §17.7): без слов «трек» и «координаты».
    const val ALT_SWITCH_LABEL = "Передавать обезличенные высоты для режимной карты"
    const val ALT_NOTE = "Передаются только километр пути и высота — без времени, без вашего имени и без координат. " +
        "Помогает строить профиль пути для режимных карт."
    const val ALT_REVOKE_TITLE = "Остановить передачу высот?"
    const val ALT_REVOKE_TEXT = "Новые высоты передаваться не будут. Уже отправленное вернуть нельзя: " +
        "оно обезличено и не связано с вашим аккаунтом."
    const val ALT_REVOKE_CONFIRM = "Остановить"
}

/**
 * Экран согласия №1 при первом включении «АВТО»: переключатель по умолчанию ВЫКЛ (без предустановленной
 * галочки), «Продолжить» включает «АВТО» с выбранным значением, «Назад» отменяет включение.
 */
@Composable
fun TrackConsentDialog(
    initialChecked: Boolean,
    onContinue: (saveTracks: Boolean, shareAltitudes: Boolean) -> Unit,
    onDismiss: () -> Unit,
    initialAltitudeChecked: Boolean = false,
) {
    var checked by remember { mutableStateOf(initialChecked) }
    var altChecked by remember { mutableStateOf(initialAltitudeChecked) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize().testTag("track_consent_screen"), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        TrackConsentTexts.TITLE,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            TrackConsentTexts.SWITCH_LABEL,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = checked,
                            onCheckedChange = { checked = it },
                            colors = com.z_company.route.ui.settings.settingsSwitchColors(),
                            modifier = Modifier.testTag("track_consent_switch"),
                        )
                    }
                    Text(
                        TrackConsentTexts.BODY,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TrackConsentTexts.POINTS.forEach { point ->
                        Text(
                            "• $point",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        TrackConsentTexts.NO_CONSENT_NOTE,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider()
                    // Согласие №2: независимо от первого, по умолчанию выключено.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            TrackConsentTexts.ALT_SWITCH_LABEL,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = altChecked,
                            onCheckedChange = { altChecked = it },
                            colors = com.z_company.route.ui.settings.settingsSwitchColors(),
                            modifier = Modifier.testTag("track_consent_altitude_switch"),
                        )
                    }
                    Text(
                        TrackConsentTexts.ALT_NOTE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onContinue(checked, altChecked) },
                    modifier = Modifier.fillMaxWidth().testTag("track_consent_continue"),
                ) { Text(TrackConsentTexts.CONTINUE) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
            }
        }
    }
}
