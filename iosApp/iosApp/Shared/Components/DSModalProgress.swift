import SwiftUI

// Контракт: SCREEN_SPECS §24.1 (`AnimationDialog`): контейнер модального
// прогресса/результата с анимацией появления. Сам по себе бизнес-логики не
// имеет; результат задаёт вызывающий экран (заголовок, текст, иконка, действие).
// Показывается поверх контента; взаимоисключаем с другими модальными
// состояниями (§24.1).

/// Режим отображения модального диалога.
enum DSModalProgressMode {
    /// Блокирующий прогресс (§24.4: только когда без результата нельзя продолжить).
    case progress
    /// Результат: успех/ошибка с необязательным действием.
    case result(success: Bool)
}

/// Модальный контейнер прогресса/результата (`AnimationDialog`, §24.1).
struct DSModalProgress: View {
    let mode: DSModalProgressMode
    let title: String
    var message: String? = nil
    var actionTitle: String? = nil
    var onAction: (() -> Void)? = nil

    var body: some View {
        ZStack {
            DSColor.scrim
                .ignoresSafeArea()
            VStack(spacing: 0) {
                switch mode {
                case .progress:
                    ProgressView()
                        .controlSize(.large)
                        .tint(DSColor.accent)
                        .padding(.bottom, 18)
                case .result(let success):
                    ZStack {
                        Circle()
                            .fill(success ? DSColor.success.opacity(0.15) : DSColor.danger.opacity(0.15))
                        (success ? DSIcon.check : DSIcon.warning).image
                            .font(.system(size: 26, weight: .semibold))
                            .foregroundColor(success ? DSColor.success : DSColor.danger)
                    }
                    .frame(width: 56, height: 56)
                    .padding(.bottom, 18)
                }

                Text(title)
                    .font(DSFont.sans(18, .bold))
                    .foregroundColor(DSColor.text)
                    .multilineTextAlignment(.center)
                if let message {
                    Text(message)
                        .font(DSFont.sans(14))
                        .foregroundColor(DSColor.textMuted)
                        .multilineTextAlignment(.center)
                        .padding(.top, 8)
                }
                if let actionTitle, let onAction {
                    DSCTAButton(title: actionTitle, action: onAction)
                        .padding(.top, 20)
                }
            }
            .padding(24)
            .frame(maxWidth: 320)
            .background(DSColor.bgElevated)
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
            .dsShadow(.floating)
        }
        .transition(.opacity.combined(with: .scale(scale: 0.96)))
    }
}

#Preview("Прогресс") {
    DSModalProgress(mode: .progress, title: "Выгрузка данных…", message: "Не закрывайте приложение")
}

#Preview("Результат") {
    DSModalProgress(
        mode: .result(success: true),
        title: "Синхронизация завершена",
        message: "Все маршруты выгружены",
        actionTitle: "Готово",
        onAction: {}
    )
}
