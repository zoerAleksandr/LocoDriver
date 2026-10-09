package com.z_company.route.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.route.autorecord.TrackConsentStore
import com.z_company.route.autorecord.TrackConsentTexts
import com.z_company.route.component.AppAlertDialog
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * «Данные и конфиденциальность» (SCREEN_SPECS §17.7): согласие №1 «Сохранять мои треки и историю
 * поездок». Выключено по умолчанию; отзыв — с подтверждением, удаляет локальные треки.
 * Переключатель №2 (обезличенные высоты) пока не показывается.
 */
@Composable
fun SettingsPrivacyContent() {
    val consent: TrackConsentStore = koinInject()
    val saveTracks by consent.saveTracks.collectAsState()
    var confirmRevoke by remember { mutableStateOf(false) }

    if (confirmRevoke) {
        AppAlertDialog(
            onDismissRequest = { confirmRevoke = false },
            title = TrackConsentTexts.REVOKE_TITLE,
            text = TrackConsentTexts.REVOKE_TEXT,
            confirmText = TrackConsentTexts.REVOKE_CONFIRM,
            isDestructive = true,
            onConfirm = {
                confirmRevoke = false
                consent.setConsent(false)
            },
            dismissText = TrackConsentTexts.REVOKE_CANCEL,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 28.dp),
    ) {
        SettingsGroupHeader("ТРЕКИ И ИСТОРИЯ ПОЕЗДОК", top = 8.dp, startPad = 4.dp)
        SettingsCard {
            SettingsSwitchRow(
                label = TrackConsentTexts.SWITCH_LABEL,
                checked = saveTracks,
                onCheckedChange = { on ->
                    if (on) consent.setConsent(true) else confirmRevoke = true
                },
            )
        }
        if (saveTracks) {
            SettingsSectionNote(
                "Согласие дано " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(consent.consentAtMs()))
            )
        }
        SettingsSectionNote(TrackConsentTexts.BODY)
        TrackConsentTexts.POINTS.forEach { SettingsSectionNote("• $it") }
        SettingsSectionNote(TrackConsentTexts.NO_CONSENT_NOTE)
    }
}
