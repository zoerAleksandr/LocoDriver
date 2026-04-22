import SwiftUI
import ComposeApp

// MARK: - SalaryCalculationView

struct SalaryCalculationView: View {
    @StateObject private var vm = SalaryCalculationViewModelWrapper()

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView("Загрузка...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                mainContent
            }
        }
        .navigationTitle("Зарплата")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Main content

    private var mainContent: some View {
        List {
            // Month navigation header
            Section {
                monthNavigationHeader
            }

            // Summary card
            if let summary = vm.summary {
                Section(header: Text("Сводка")) {
                    summaryRows(summary: summary)
                }
            }

            // Routes list
            if vm.routes.isEmpty {
                Section(header: Text("Маршруты")) {
                    Text("Нет маршрутов за этот месяц")
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.vertical, 8)
                }
            } else {
                Section(header: Text("Маршруты (\(vm.routes.count))")) {
                    ForEach(vm.routes, id: \.routeId) { row in
                        routeRow(row: row)
                    }
                }
            }
        }
        .listStyle(InsetGroupedListStyle())
    }

    // MARK: - Month navigation header

    private var monthNavigationHeader: some View {
        HStack {
            Button(action: { vm.previousMonth() }) {
                Image(systemName: "chevron.left")
                    .font(.title3.weight(.semibold))
                    .foregroundColor(Color.appAccent)
                    .frame(width: 44, height: 44)
            }

            Spacer()

            VStack(spacing: 2) {
                if let summary = vm.summary {
                    Text(summary.month)
                        .font(.headline)
                        .fontWeight(.semibold)
                } else {
                    Text(formatMonthYear(month: vm.currentMonth, year: vm.currentYear))
                        .font(.headline)
                        .fontWeight(.semibold)
                }
            }

            Spacer()

            Button(action: { vm.nextMonth() }) {
                Image(systemName: "chevron.right")
                    .font(.title3.weight(.semibold))
                    .foregroundColor(Color.appAccent)
                    .frame(width: 44, height: 44)
            }
        }
        .padding(.vertical, 4)
    }

    // MARK: - Summary rows

    @ViewBuilder
    private func summaryRows(summary: SalaryCalculationIosViewModel.MonthlySummary) -> some View {
        // Total routes
        HStack {
            Label("Маршрутов", systemImage: "list.bullet.rectangle")
                .foregroundColor(.primary)
            Spacer()
            Text("\(summary.routeCount)")
                .fontWeight(.medium)
                .foregroundColor(.secondary)
        }

        // Total work time
        HStack {
            Label("Отработано", systemImage: "clock")
                .foregroundColor(.primary)
            Spacer()
            Text(formatDurationHM(hours: summary.totalWorkHours, minutes: summary.totalWorkMinutes))
                .fontWeight(.medium)
                .foregroundColor(.secondary)
        }

        // Night hours
        HStack {
            Label("Ночное время", systemImage: "moon.stars")
                .foregroundColor(.primary)
            Spacer()
            Text(formatDurationHM(hours: summary.nightTimeHours, minutes: summary.nightTimeMinutes))
                .fontWeight(.medium)
                .foregroundColor(summary.nightTimeMs > 0 ? Color.appAccent : .secondary)
        }

        // Norma
        HStack {
            Label("Норма часов", systemImage: "calendar.badge.clock")
                .foregroundColor(.primary)
            Spacer()
            Text("\(summary.normalNormaHours) ч")
                .fontWeight(.medium)
                .foregroundColor(.secondary)
        }

        // Overtime
        HStack {
            Label("Переработка", systemImage: "clock.badge.exclamationmark")
                .foregroundColor(.primary)
            Spacer()
            if summary.overtimeMs > 0 {
                Text(formatDurationHM(hours: summary.overtimeHours, minutes: summary.overtimeMinutes))
                    .fontWeight(.medium)
                    .foregroundColor(Color.appWarning)
            } else {
                Text("Нет")
                    .fontWeight(.medium)
                    .foregroundColor(.secondary)
            }
        }
    }

    // MARK: - Route row

    private func routeRow(row: SalaryCalculationIosViewModel.RouteRow) -> some View {
        HStack(alignment: .center, spacing: 12) {
            // Left: route number
            VStack(alignment: .leading, spacing: 2) {
                Text(row.number)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(.primary)
                    .lineLimit(1)
                if row.timeStartWorkMs > 0 {
                    Text(TimeFormatter.formatDateTime(ms: row.timeStartWorkMs))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Spacer()

            // Right: work duration
            if row.workDurationMs > 0 {
                Text(formatDurationHM(hours: row.workHours, minutes: row.workMinutes))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .monospacedDigit()
            }
        }
        .padding(.vertical, 2)
    }

    // MARK: - Helpers

    private func formatDurationHM(hours: Int64, minutes: Int64) -> String {
        if hours > 0 {
            return "\(hours)ч \(minutes)м"
        }
        return "\(minutes)м"
    }

    private func formatMonthYear(month: Int, year: Int) -> String {
        let monthNames = ["Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                          "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"]
        let name = (month >= 0 && month < monthNames.count) ? monthNames[month] : "?"
        return "\(name) \(year)"
    }
}
