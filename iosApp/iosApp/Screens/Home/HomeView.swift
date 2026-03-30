import SwiftUI
import ComposeApp

// MARK: - HomeView

/// Главный экран — список маршрутов текущего месяца со статистикой.
struct HomeView: View {
    @StateObject private var vm = HomeViewModelWrapper()
    @State private var showDeleteConfirm = false
    @State private var routeToDelete: String? = nil

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView("Загрузка...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if vm.routes.isEmpty {
                emptyStateView
            } else {
                contentList
            }
        }
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Image(systemName: "magnifyingglass")
            }
        }
        .alert("Удалить маршрут?", isPresented: $showDeleteConfirm) {
            Button("Удалить", role: .destructive) {
                if let id = routeToDelete {
                    // удаление через ViewModel
                }
            }
            Button("Отмена", role: .cancel) {}
        }
    }

    // MARK: - Navigation title (month/year from settings)
    private var navigationTitle: String {
        guard let s = vm.settings else { return "Поездки" }
        let monthNames = ["Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                          "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"]
        let moy = s.selectMonthOfYear
        let idx = Int(moy.month)
        let name = idx >= 0 && idx < monthNames.count ? monthNames[idx] : "?"
        return "\(name) \(moy.year)"
    }

    // MARK: - Empty state
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: "list.bullet.clipboard")
                .font(.system(size: 64))
                .foregroundColor(.secondary)
            Text("Нет маршрутов за этот месяц")
                .font(.headline)
                .foregroundColor(.secondary)
            Text("Нажмите «+» для добавления")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Main content
    private var contentList: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                mainInfoSection
                    .padding(.horizontal)
                    .padding(.vertical, 8)

                Divider()

                ForEach(vm.routes, id: \.basicData.id) { route in
                    NavigationLink(destination: FormView(routeId: route.basicData.id)) {
                        RouteItemView(route: route)
                    }
                    .buttonStyle(.plain)
                    .contextMenu {
                        Button {
                            // copyRoute
                        } label: {
                            Label("Скопировать", systemImage: "doc.on.doc")
                        }
                        Button(role: .destructive) {
                            routeToDelete = route.basicData.id
                            showDeleteConfirm = true
                        } label: {
                            Label("Удалить", systemImage: "trash")
                        }
                    }

                    Divider()
                        .padding(.leading)
                }
            }
        }
        .refreshable {
            // pull-to-refresh — reload from vm
        }
    }

    // MARK: - MainInfoSection
    private var mainInfoSection: some View {
        let totalWorkMs = vm.routes.reduce(Int64(0)) { acc, route in
            let start = route.basicData.timeStartWork?.int64Value ?? 0
            let end = route.basicData.timeEndWork?.int64Value ?? 0
            return acc + (end > start ? end - start : 0)
        }
        // Норма часов вычисляется из MonthOfYear — используем типичное значение 165 ч
        let normaMs = Int64(165 * 3600 * 1000)
        let progress = normaMs > 0 ? min(Double(totalWorkMs) / Double(normaMs), 1.0) : 0.0

        return VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("Всего: \(TimeFormatter.formatDuration(ms: totalWorkMs))")
                    .font(.subheadline)
                    .fontWeight(.medium)
                Spacer()
                Text("из \(TimeFormatter.formatDuration(ms: normaMs))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            ProgressView(value: progress)
                .progressViewStyle(.linear)
                .tint(.accentColor)

            Text("\(vm.routes.count) маршрутов")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - RouteItemView

/// Карточка маршрута в списке на главном экране.
struct RouteItemView: View {
    let route: DomainRoute

    private var startMs: Int64 { route.basicData.timeStartWork?.int64Value ?? 0 }
    private var endMs: Int64 { route.basicData.timeEndWork?.int64Value ?? 0 }
    private var durationMs: Int64 { endMs > startMs ? endMs - startMs : 0 }

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    if let number = route.basicData.number {
                        Text("Маршрут \(number)")
                            .font(.headline)
                    } else {
                        Text("Маршрут")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                    // Значок длинного/тяжёлого поезда
                    if route.trains.contains(where: { $0.isHeavyLongDistance }) {
                        Image(systemName: "arrow.left.and.right")
                            .font(.caption)
                            .foregroundColor(.orange)
                    }
                }

                HStack(spacing: 16) {
                    Label(TimeFormatter.formatDateTime(ms: startMs), systemImage: "clock")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    if durationMs > 0 {
                        Label(TimeFormatter.formatDuration(ms: durationMs), systemImage: "timer")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                if !route.locomotives.isEmpty {
                    let loco = route.locomotives[0]
                    HStack(spacing: 4) {
                        Image(systemName: "tram.fill")
                            .font(.caption2)
                            .foregroundColor(.accentColor)
                        Text([loco.series, loco.number].compactMap { $0 }.joined(separator: " "))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                if !route.trains.isEmpty {
                    let train = route.trains[0]
                    if let num = train.number {
                        HStack(spacing: 4) {
                            Image(systemName: "car.2.fill")
                                .font(.caption2)
                                .foregroundColor(.accentColor)
                            Text("Поезд \(num)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal)
        .background(Color(UIColor.systemBackground))
    }
}

#Preview {
    NavigationStack {
        HomeView()
    }
}
