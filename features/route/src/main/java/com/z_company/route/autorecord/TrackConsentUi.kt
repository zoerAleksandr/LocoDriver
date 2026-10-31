package com.z_company.route.autorecord

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.z_company.route.component.servicephase.PstIcon
import com.z_company.route.component.servicephase.PstIcons
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
    const val BODY = "Когда включен режим «Автозапись», приложение записывает ваш путь: время, положение и скорость " +
        "в точках следования. Это нужно, чтобы вы могли открыть пройденный путь на карте, посмотреть скорость " +
        "и историю поездок."

    /** Пункты под основным текстом (показываются отдельными абзацами с маркером). */
    val POINTS = listOf(
        "Треки хранятся на вашем устройстве и в зашифрованном виде на сервере LocoDriver. Доступ к ним только у вас, " +
            "администраторы приложения их не видят.",
        "Срок хранения — 180 дней. Избранные треки, отмеченные звездой в разделе «Мои треки», не удаляются " +
            "автоматически.",
    )
    const val NO_CONSENT_NOTE = "Без согласия режим «Автозапись» продолжит записывать стоянки и отправления поезда, " +
        "но путь не сохранится."
    const val CONTINUE = "Продолжить"

    // Экран первого включения «АВТО» (ONBOARDING_MATERIALS §3 - дословно): над переключателями согласий.
    const val INTRO_TITLE = "Автозапись"
    const val INTRO_TEXT = "Автозапись сама записывает стоянки и отправления вашего поезда по скорости и месту. " +
        "Время считается по тому моменту, когда поезд остановился или тронулся. Режим выключится сам через 12 часов."
    const val CANCEL = "Отмена"
    const val CONSENT_LEAD = "Если вы согласитесь:"
    val CONSENT_POINTS = listOf(
        "сохраним ваши поездки: путь на карте, скорость и историю. Они хранятся на устройстве и в зашифрованном " +
            "виде на сервере, администраторы их не видят. Срок хранения - 180 дней, избранные не удаляются;",
        "передадим обезличенные данные для карт: километр пути, высоту и места потери GPS - без вашего имени " +
            "и точного времени.",
    )
    const val AGREE = "Согласиться и включить"
    const val WITHOUT_SAVING = "Включить без сохранения"
    const val REVOKE_TITLE = "Отозвать согласие?"
    const val REVOKE_TEXT = "Новые треки сохраняться не будут. Уже записанные треки останутся в разделе «Мои треки» — " +
        "удалить их можно там."
    const val REVOKE_CONFIRM = "Отозвать"
    const val REVOKE_CANCEL = "Отмена"

    // Согласие №2 (SCREEN_SPECS §6.9, §17.7): без слов «трек» и «координаты»; с 2-й редакции - ещё и места потери GPS.
    const val ALT_SWITCH_LABEL = "Передавать обезличенные данные для карт: высоты и места потери GPS"
    const val ALT_NOTE = "Передаются километр пути и высота, а также места, где пропадает GPS (участки карты около 250 м) — " +
        "без вашего имени и без точного времени. Помогает строить профиль пути для режимных карт и общую карту мест без GPS."
    const val ALT_REVOKE_TITLE = "Остановить передачу данных для карт?"
    const val ALT_REVOKE_TEXT = "Новые высоты и места потери GPS передаваться не будут. Уже отправленное вернуть нельзя: " +
        "оно обезличено и не связано с вашим аккаунтом."
    /** Для согласившихся на прежнюю редакцию (только высоты): места потери GPS не передаются, пока не подтвердят. */
    const val ALT_RECONFIRM_NOTE = "Вы согласились на передачу только высот. Места потери GPS не передаются, пока вы не подтвердите новые условия."
    const val ALT_RECONFIRM_BUTTON = "Подтвердить новые условия"
    const val ALT_REVOKE_CONFIRM = "Остановить"
}

/**
 * Экран первого включения «Автозаписи»: одно согласие на оба пункта (сохранение треков и обезличенные
 * данные для карт). Текст и пункты - на белом фоне, две кнопки закреплены внизу: «Согласиться и включить»
 * выставляет оба согласия, «Включить без сохранения» включает режим без них (согласие добровольно).
 * Крестик вверху справа отменяет включение.
 */
@Composable
fun TrackConsentDialog(
    onContinue: (saveTracks: Boolean, shareAltitudes: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize().testTag("track_consent_screen"), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onDismiss)
                            .testTag("track_consent_cancel"),
                        contentAlignment = Alignment.Center,
                    ) { PstIcon(PstIcons.Close, 18.dp, MaterialTheme.colorScheme.onSurfaceVariant, description = TrackConsentTexts.CANCEL) }
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        TrackConsentTexts.INTRO_TITLE,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("track_consent_intro_title"),
                    )
                    Text(
                        TrackConsentTexts.INTRO_TEXT,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        TrackConsentTexts.CONSENT_LEAD,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("track_consent_short"),
                    )
                    TrackConsentTexts.CONSENT_POINTS.forEach { point ->
                        Text(
                            "• $point",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        TrackConsentTexts.NO_CONSENT_NOTE,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onContinue(true, true) },
                        modifier = Modifier.fillMaxWidth().testTag("track_consent_continue"),
                    ) { Text(TrackConsentTexts.AGREE) }
                    OutlinedButton(
                        onClick = { onContinue(false, false) },
                        modifier = Modifier.fillMaxWidth().testTag("track_consent_without"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    ) { Text(TrackConsentTexts.WITHOUT_SAVING) }
                }
            }
        }
    }
}
