import SwiftUI
import UIKit
import ComposeApp

// Вспомогательные типы формы маршрута (SCREEN_SPECS §5): поля времени, форматирование в поясе
// отображения, шторка выбора даты/времени, share-sheet.

/// Четыре поля времени формы (§5.3 п.8).
enum RouteTimeField: String, Identifiable {
    case start, end, breakStart, breakEnd

    var id: String { rawValue }

    var rowTitle: String {
        switch self {
        case .start: return "Явка"
        case .end: return "Сдача"
        case .breakStart: return "Начало перерыва"
        case .breakEnd: return "Конец перерыва"
        }
    }

    /// Ключ recent-times (§5.3 п.8).
    var recentKey: String {
        switch self {
        case .start: return "time_start_work"
        case .end: return "time_end_work"
        case .breakStart: return "time_start_break"
        case .breakEnd: return "time_end_break"
        }
    }

    /// Заголовок пикера (`AppDateTimePicker`).
    var pickerTitle: String {
        switch self {
        case .start: return "Явка"
        case .end: return "Сдача"
        case .breakStart: return "Начало перерыва"
        case .breakEnd: return "Окончание перерыва"
        }
    }

    /// Заголовок шторки «Удалить значение».
    var deleteTitle: String {
        switch self {
        case .start: return "Время явки"
        case .end: return "Время сдачи"
        case .breakStart: return "Начало перерыва"
        case .breakEnd: return "Окончание перерыва"
        }
    }
}

/// Форматирование времени в поясе отображения пользователя (`displayTimeZone`, §0.1).
struct RouteTimeFormat {
    let offsetSeconds: Int

    var timeZone: TimeZone { TimeZone(secondsFromGMT: offsetSeconds) ?? TimeZone(secondsFromGMT: 3 * 3600)! }

    private func make(_ format: String) -> DateFormatter {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = timeZone
        f.dateFormat = format
        return f
    }

    /// `dd.MM.yy` — дата в плашке строки времени.
    func date(_ ms: Int64) -> String { make("dd.MM.yy").string(from: TimeFormatter.msToDate(ms)) }
    /// `HH:mm`.
    func time(_ ms: Int64) -> String { make("HH:mm").string(from: TimeFormatter.msToDate(ms)) }
    /// `dd.MM.yy HH:mm` (`getDateAndTime`).
    func dateTime(_ ms: Int64) -> String { make("dd.MM.yy HH:mm").string(from: TimeFormatter.msToDate(ms)) }
}

extension Optional where Wrapped == KotlinLong {
    var ms: Int64? { self?.int64Value }
}

/// Системный share-sheet с темой письма (`ShareLinkData.subject`, §5.6).
struct RouteShareSheet: UIViewControllerRepresentable {
    let text: String
    let subject: String

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [ShareItem(text: text, subject: subject)], applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}

    private final class ShareItem: NSObject, UIActivityItemSource {
        let text: String
        let subject: String
        init(text: String, subject: String) { self.text = text; self.subject = subject }
        func activityViewControllerPlaceholderItem(_ activityViewController: UIActivityViewController) -> Any { text }
        func activityViewController(_ activityViewController: UIActivityViewController, itemForActivityType activityType: UIActivity.ActivityType?) -> Any? { text }
        func activityViewController(_ activityViewController: UIActivityViewController, subjectForActivityType activityType: UIActivity.ActivityType?) -> String { subject }
    }
}

struct SharePayload: Identifiable {
    let id = UUID()
    let text: String
    let subject: String
}

/// Подтверждение-шторка формы (`AppBottomSheet`, §24.1): заголовок, описание, действия, «Отмена».
/// Тап по действию сначала закрывает шторку, затем выполняет действие.
struct RouteConfirmModel: Identifiable {
    struct Action {
        enum Role { case normal, destructive }
        let title: String
        let role: Role
        let perform: () -> Void
    }

    let id = UUID()
    let title: String
    let message: String?
    var isDuplicate: Bool = false
    let actions: [Action]
}

/// Центральный диалог подтверждения (вид как у системного, но шире): заголовок, описание,
/// действия и «Отмена». Тап по затемнению = отмена.
struct RouteConfirmDialog: View {
    let model: RouteConfirmModel
    let onCancel: () -> Void
    let onAction: (RouteConfirmModel.Action) -> Void

    var body: some View {
        ZStack {
            DSColor.scrim
                .ignoresSafeArea()
                .onTapGesture(perform: onCancel)
            VStack(spacing: 0) {
                VStack(spacing: 8) {
                    Text(model.title)
                        .font(DSFont.sans(20, .bold))
                        .foregroundColor(DSColor.text)
                        .multilineTextAlignment(.center)
                    if let message = model.message, !message.isEmpty {
                        Text(message)
                            .font(DSFont.sans(15))
                            .foregroundColor(DSColor.textMuted)
                            .multilineTextAlignment(.center)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 26)
                .padding(.bottom, 20)

                VStack(spacing: 10) {
                    ForEach(Array(model.actions.enumerated()), id: \.offset) { _, action in
                        button(action.title, color: action.role == .destructive ? DSColor.danger : DSColor.text, bold: true) {
                            onAction(action)
                        }
                    }
                    button("Отмена", color: DSColor.accent, bold: false, action: onCancel)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
            .frame(maxWidth: 340)
            .background(DSColor.bgElevated)
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
            .dsShadow(.floating)
            .padding(.horizontal, 24)
        }
        .transition(.opacity)
    }

    private func button(_ title: String, color: Color, bold: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(DSFont.sans(17, bold ? .semibold : .regular))
                .foregroundColor(color)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(DSColor.bgSubtle)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
