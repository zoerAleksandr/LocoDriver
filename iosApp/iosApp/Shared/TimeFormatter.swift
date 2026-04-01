import Foundation

struct TimeFormatter {

    /// Продолжительность работы: HH:MM или десятичный формат (12,50).
    static func formatWorkDuration(ms: Int64, isDecimal: Bool = false) -> String {
        guard ms > 0 else { return isDecimal ? "0,00" : "00:00" }
        let totalMinutes = ms / 60_000
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        if isDecimal {
            let fraction = Int((Double(minutes) / 60.0) * 100 + 0.5)
            return "\(hours),\(String(format: "%02d", fraction))"
        }
        return String(format: "%02d:%02d", hours, minutes)
    }

    /// Продолжительность — используется в RouteItemView (всегда HH:MM).
    static func formatDuration(ms: Int64) -> String {
        formatWorkDuration(ms: ms, isDecimal: false)
    }

    /// Дата и время: "dd.MM HH:mm"
    static func formatDateTime(ms: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(ms) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM HH:mm"
        return formatter.string(from: date)
    }

    /// Только дата: "dd.MM.yyyy"
    static func formatDate(ms: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(ms) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM.yyyy"
        return formatter.string(from: date)
    }

    /// Короткая дата: "dd.MM"
    static func formatShortDate(ms: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(ms) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM"
        return formatter.string(from: date)
    }

    static func msToDate(_ ms: Int64) -> Date {
        Date(timeIntervalSince1970: Double(ms) / 1000)
    }

    static func dateToMs(_ date: Date) -> Int64 {
        Int64(date.timeIntervalSince1970 * 1000)
    }
}
