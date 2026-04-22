import SwiftUI
import ComposeApp

struct WorkScheduleView: View {
    @StateObject private var vm = HomeViewModelWrapper()

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
    private let weekdays = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]

    private var currentYear: Int { Calendar.current.component(.year, from: Date()) }
    private var currentMonth: Int { Calendar.current.component(.month, from: Date()) }

    private var daysInMonth: Int {
        let components = DateComponents(year: currentYear, month: currentMonth)
        let date = Calendar.current.date(from: components)!
        return Calendar.current.range(of: .day, in: .month, for: date)!.count
    }

    // Смещение первого дня (0=Пн, 6=Вс)
    private var firstWeekday: Int {
        let components = DateComponents(year: currentYear, month: currentMonth, day: 1)
        let date = Calendar.current.date(from: components)!
        let weekday = Calendar.current.component(.weekday, from: date)
        return (weekday + 5) % 7 // конвертируем: 1=Вс -> 6, 2=Пн -> 0
    }

    // Дни когда есть маршруты (набор чисел)
    private var routeDays: Set<Int> {
        var days = Set<Int>()
        for route in vm.routes {
            if let ms = route.basicData.timeStartWork as? Int64, ms > 0 {
                let date = TimeFormatter.msToDate(ms)
                let month = Calendar.current.component(.month, from: date)
                let year = Calendar.current.component(.year, from: date)
                if month == currentMonth && year == currentYear {
                    days.insert(Calendar.current.component(.day, from: date))
                }
            }
        }
        return days
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                // Заголовки дней недели
                LazyVGrid(columns: columns, spacing: 4) {
                    ForEach(weekdays, id: \.self) { day in
                        Text(day)
                            .font(.caption2)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity)
                    }
                }
                .padding(.horizontal)

                // Сетка дней
                LazyVGrid(columns: columns, spacing: 4) {
                    // Пустые ячейки до первого дня
                    ForEach(0..<firstWeekday, id: \.self) { _ in
                        Color.clear.frame(minHeight: 36)
                    }
                    // Дни месяца
                    ForEach(1...daysInMonth, id: \.self) { day in
                        let hasRoute = routeDays.contains(day)
                        let isToday = day == Calendar.current.component(.day, from: Date())
                        ZStack {
                            RoundedRectangle(cornerRadius: 6)
                                .fill(cellColor(day: day, hasRoute: hasRoute, isToday: isToday))
                            VStack(spacing: 2) {
                                Text("\(day)")
                                    .font(.caption)
                                    .fontWeight(isToday ? .bold : .regular)
                                if hasRoute {
                                    Circle()
                                        .fill(Color.white)
                                        .frame(width: 4, height: 4)
                                }
                            }
                        }
                        .frame(minHeight: 36)
                    }
                }
                .padding(.horizontal)
            }
            .padding(.vertical)
        }
        .navigationTitle("График работы")
    }

    private func cellColor(day: Int, hasRoute: Bool, isToday: Bool) -> Color {
        if isToday { return Color.appAccent }
        if hasRoute { return Color.appAccent.opacity(0.3) }
        let weekOffset = (firstWeekday + day - 1) % 7
        if weekOffset >= 5 { return Color.appSuccess.opacity(0.2) } // Сб/Вс
        return Color.appElevated
    }
}
