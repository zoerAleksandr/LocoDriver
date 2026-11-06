import Foundation
import SwiftUI
import ComposeApp

/// Обёртка Kotlin `DataPrivacyIosViewModel` (SCREEN_SPECS §17.7) — по экземпляру
/// на открытый экран. Вся логика согласий — в Kotlin; здесь только публикация
/// состояния и действия (выключение — после подтверждения диалогом в SwiftUI).
@MainActor
final class DataPrivacyViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createDataPrivacyViewModel()

    /// nil — Kotlin ещё не прислал первое состояние.
    @Published var state: DataPrivacyState? = nil

    init() {
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    deinit { viewModel.dispose() }

    /// Согласие №1 «Сохранять треки». Вызов только после подтверждения отзыва.
    func setSaveTracks(_ enabled: Bool) { viewModel.setSaveTracks(enabled: enabled) }

    /// Согласие №2 «Передавать обезличенные данные для карт». Вызов только после подтверждения отзыва.
    func setAltitudeContrib(_ enabled: Bool) { viewModel.setAltitudeContrib(enabled: enabled) }

    /// «Подтвердить новые условия» (редакция текста согласия №2).
    func confirmGnssText() { viewModel.confirmGnssText() }
}
