import Foundation

struct TimeFormatter {
    static func formatDuration(ms: Int64) -> String {
        let totalMinutes = ms / 60_000
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        if hours > 0 { return "\(hours)ч \(minutes)м" }
        return "\(minutes)м"
    }

    static func formatDateTime(ms: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(ms) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM HH:mm"
        return formatter.string(from: date)
    }

    static func formatDate(ms: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(ms) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM.yyyy"
        return formatter.string(from: date)
    }

    static func msToDate(_ ms: Int64) -> Date {
        Date(timeIntervalSince1970: Double(ms) / 1000)
    }

    static func dateToMs(_ date: Date) -> Int64 {
        Int64(date.timeIntervalSince1970 * 1000)
    }
}
