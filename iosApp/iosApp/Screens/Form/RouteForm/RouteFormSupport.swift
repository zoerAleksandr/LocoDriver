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
