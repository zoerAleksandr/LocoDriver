import SwiftUI
import ComposeApp

// «Данные и конфиденциальность» — SCREEN_SPECS §17.7 (согласия №1 и №2).
// Логика — Kotlin `DataPrivacyIosViewModel` (обёртка `DataPrivacyViewModelWrapper`).
// Top bar как у «Основные» (§17.1): заголовок по центру, слева синее «Готово».
// Выключение обоих согласий — только через подтверждение; включение — сразу.

struct DataPrivacyView: View {
    @StateObject private var vm = DataPrivacyViewModelWrapper()

    @State private var revokeTracksRequest: DSDeleteRequest?
    @State private var revokeAltitudeRequest: DSDeleteRequest?

    var body: some View {
        SettingsScaffold(title: "Данные и конфиденциальность") {
            tracksGroup
            mapsGroup
        }
        // Каждый диалог — на своём фоне: два `.alert` на одном View не работают.
        .dsDeleteAlert($revokeTracksRequest)
        .dsDeleteAlert($revokeAltitudeRequest)
    }

    // ── Треки и история поездок (согласие №1) ────────────────────────────

    private var tracksGroup: some View {
        VStack(alignment: .leading, spacing: 0) {
            DSGroupHeader("ТРЕКИ И ИСТОРИЯ ПОЕЗДОК", topSpacing: DSMetrics.groupHeadFirst)
            DSCard {
                SettingsToggleRow(
                    title: "Сохранять треки",
                    isOn: vm.state?.saveTracks ?? false,
                    showsDivider: false
                ) { on in
                    if on {
                        vm.setSaveTracks(true)
                    } else {
                        revokeTracksRequest = DSDeleteRequest(
                            title: "Отозвать согласие?",
                            message: "Новые треки сохраняться не будут. Уже записанные треки останутся в разделе «Мои треки» — удалить их можно там.",
                            confirmTitle: "Отозвать"
                        ) { vm.setSaveTracks(false) }
                    }
                }
            }
            if let state = vm.state, state.saveTracks, state.saveTracksAtMs > 0 {
                SettingsHint("Согласие дано \(Self.dateLabel(state.saveTracksAtMs))")
            }
            SettingsHint(TrackPrivacyTexts.body)
            ForEach(TrackPrivacyTexts.points, id: \.self) { point in
                SettingsHint("• \(point)")
            }
            SettingsHint(TrackPrivacyTexts.noConsentNote)
        }
    }

    // ── Данные для карт (согласие №2) ────────────────────────────────────

    private var mapsGroup: some View {
        VStack(alignment: .leading, spacing: 0) {
            DSGroupHeader("ДАННЫЕ ДЛЯ КАРТ", topSpacing: 24)
            DSCard {
                SettingsToggleRow(
                    title: "Передавать обезличенные данные для карт",
                    isOn: vm.state?.altitudeContrib ?? false,
                    showsDivider: false
                ) { on in
                    if on {
                        vm.setAltitudeContrib(true)
                    } else {
                        revokeAltitudeRequest = DSDeleteRequest(
                            title: "Остановить передачу данных для карт?",
                            message: "Новые высоты и места потери GPS передаваться не будут. Уже отправленное вернуть нельзя: оно обезличено и не связано с вашим аккаунтом.",
                            confirmTitle: "Остановить"
                        ) { vm.setAltitudeContrib(false) }
                    }
                }
            }
            if let state = vm.state, state.altitudeContrib, state.altitudeAtMs > 0 {
                SettingsHint("Согласие дано \(Self.dateLabel(state.altitudeAtMs))")
            }
            SettingsHint(TrackPrivacyTexts.altNote)
            // Согласие дано на прежних условиях (только высоты): места потери GPS
            // не передаются, пока пользователь не подтвердит новую редакцию текста.
            if vm.state?.needsGnssConfirm == true {
                SettingsHint(TrackPrivacyTexts.altReconfirmNote)
                Button(TrackPrivacyTexts.altReconfirmButton) {
                    vm.confirmGnssText()
                }
                .font(DSFont.sans(15, .semibold))
                .foregroundColor(DSColor.accent)
                .padding(.leading, DSMetrics.groupHeadPadX)
                .padding(.top, 2)
            }
        }
    }

    /// «дд.мм.гггг» — локальная дата согласия (§17.7).
    private static func dateLabel(_ ms: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(ms) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM.yyyy"
        formatter.locale = Locale(identifier: "ru_RU")
        return formatter.string(from: date)
    }
}

/// Тексты согласий (§6.9, дословно; совпадают с Android `TrackConsentTexts`).
private enum TrackPrivacyTexts {
    static let body = "Когда включен режим «Автозапись», приложение записывает ваш путь: время, положение и скорость в точках следования. Это нужно, чтобы вы могли открыть пройденный путь на карте, посмотреть скорость и историю поездок."
    static let points = [
        "Треки хранятся на вашем устройстве и в зашифрованном виде на сервере LocoDriver. Доступ к ним только у вас, администраторы приложения их не видят.",
        "Срок хранения — 180 дней. Избранные треки, отмеченные звездой в разделе «Мои треки», не удаляются автоматически.",
    ]
    static let noConsentNote = "Без согласия режим «Автозапись» продолжит записывать стоянки и отправления поезда, но путь не сохранится."
    static let altNote = "Передаются километр пути и высота, а также места, где пропадает GPS (участки карты около 250 м) — без вашего имени и без точного времени. Помогает строить профиль пути для режимных карт и общую карту мест без GPS."
    static let altReconfirmNote = "Вы согласились на передачу только высот. Места потери GPS не передаются, пока вы не подтвердите новые условия."
    static let altReconfirmButton = "Подтвердить новые условия"
}
