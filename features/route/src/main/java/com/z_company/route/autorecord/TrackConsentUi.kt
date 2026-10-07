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
    const val TITLE = "Сохранять треки поездок?"
    const val SWITCH_LABEL = "Сохранять мои треки и историю поездок"
    const val BODY = "Пока включена «АВТО», приложение может сохранять на этом устройстве GPS-трек вашего " +
        "движения (время, координаты, скорость). Треки хранятся 180 дней и затем удаляются автоматически; " +
        "трек, добавленный в избранное, не удаляется. Вы можете удалить любой трек сами. Согласие можно " +
        "отозвать в любой момент в Настройках → «Данные и конфиденциальность»: сохранённые на устройстве " +
        "треки при этом удаляются."
    const val NO_CONSENT_NOTE = "Без согласия «АВТО» работает как раньше: записывает стоянки и отправления " +
        "поезда, а трек не сохраняется."
    const val CONTINUE = "Продолжить"
    const val REVOKE_TITLE = "Отозвать согласие?"
    const val REVOKE_TEXT = "Все сохранённые на этом устройстве треки будут удалены. Это действие нельзя отменить."
    const val REVOKE_CONFIRM = "Отозвать"
    const val REVOKE_CANCEL = "Отмена"
}

/**
 * Экран согласия №1 при первом включении «АВТО»: переключатель по умолчанию ВЫКЛ (без предустановленной
 * галочки), «Продолжить» включает «АВТО» с выбранным значением, «Назад» отменяет включение.
 */
@Composable
fun TrackConsentDialog(
    initialChecked: Boolean,
    onContinue: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var checked by remember { mutableStateOf(initialChecked) }
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
                            modifier = Modifier.testTag("track_consent_switch"),
                        )
                    }
                    Text(
                        TrackConsentTexts.BODY,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        TrackConsentTexts.NO_CONSENT_NOTE,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onContinue(checked) },
                    modifier = Modifier.fillMaxWidth().testTag("track_consent_continue"),
                ) { Text(TrackConsentTexts.CONTINUE) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
            }
        }
    }
}
