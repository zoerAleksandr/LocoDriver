import SwiftUI
import Combine
import ComposeApp

// MARK: - HomeView

struct HomeView: View {
    @StateObject private var vm = HomeViewModelWrapper()
    @State private var showDeleteConfirm = false
    @State private var routeToDelete: String? = nil
    @State private var showMonthPicker = false
    @State private var statsPage = 0
    // Измеренные высоты каждой страницы пейджера (страница → высота в px).
    @State private var pageHeights: [Int: CGFloat] = [:]

    private let statsPageCount = 3

    /// Текущая высота пейджера = высота выбранной страницы (с фоллбэком на макс.).
    private var statsPagerHeight: CGFloat {
        if let h = pageHeights[statsPage] { return h }
        return pageHeights.values.max() ?? 280
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                MonthCarousel(
                    selectedMonth: vm.currentMonth,
                    selectedYear: vm.currentYear,
                    onMonthChanged: { m, y in vm.setCurrentMonth(month: m, year: y) },
                    onCenterTap: { showMonthPicker = true }
                )
                .padding(.top, 8)
                .padding(.bottom, 4)

                // Пейджер статистики
                statsCardPager
                    .padding(.horizontal, 16)

                // Точки-индикатор — под блоком
                HStack(spacing: 6) {
                    ForEach(0..<statsPageCount, id: \.self) { i in
                        Circle()
                            .fill(i == statsPage ? Color.appAccent : Color.appTertiary)
                            .frame(width: 6, height: 6)
                            .animation(.easeInOut(duration: 0.25), value: statsPage)
                    }
                }
                .padding(.top, 10)
                .padding(.bottom, 16)

                // Текущий маршрут или Следующий маршрут
                if let current = currentRoute {
                    HStack {
                        Text("Текущий маршрут")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(Color.appPrimary)
                        Spacer()
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)

                    CurrentRouteCard(route: current)
                        .padding(.bottom, 28)
                } else if let next = nextFutureRoute {
                    HStack {
                        Text("Следующий маршрут")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(Color.appPrimary)
                        Spacer()
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)

                    NavigationLink(destination: FormView(routeId: next.basicData.id)) {
                        NextRouteCard(route: next)
                    }
                    .buttonStyle(.plain)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 28)
                }

                RoutesSection(
                    routes: Array(vm.routes.prefix(2)),
                    totalCount: vm.routes.count,
                    onRouteClick: { route in },
                    onCopyRoute: { vm.copyRoute(routeId: $0) },
                    onDeleteRoute: { routeToDelete = $0; showDeleteConfirm = true },
                    onToggleFavorite: { vm.toggleFavorite(routeId: $0) },
                    onShareRoute: { vm.shareRoute(routeId: $0) },
                    onSyncRoute: { vm.syncRoute(routeId: $0) },
                    allRoutesDestination: { AllRoutesView(vm: vm) },
                    allRoutes: vm.routes,
                    settings: vm.settings
                )
                .padding(.bottom, 28)

                ToolsSection()
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)
            }
            // Плавная перестройка layout всего столбца при смене высоты пейджера.
            .animation(.interpolatingSpring(stiffness: 180, damping: 22), value: statsPagerHeight)
        }
        .background(Color.appBg)
        .navigationBarHidden(true)
        // Pull-to-refresh: Variant 2 с polling vm.isRefreshing.
        // SwiftUI ждёт async-блок — поэтому polling до момента, когда
        // KMP-VM сбросит isRefreshing (или таймаут 30с — соответствует
        // HttpTimeout в RemoteRestClient).
        .refreshable {
            vm.refresh()
            let start = Date()
            while vm.isRefreshing && Date().timeIntervalSince(start) < 30 {
                try? await Task.sleep(nanoseconds: 100_000_000) // 100ms
            }
        }
        .sheet(isPresented: $showMonthPicker) {
            MonthPickerSheet(
                selectedMonth: vm.currentMonth,
                selectedYear: vm.currentYear
            ) { month, year in
                vm.setCurrentMonth(month: month, year: year)
            }
            .presentationDetents([.medium])
            .presentationDragIndicator(.visible)
        }
        .alert("Удалить маршрут?", isPresented: $showDeleteConfirm) {
            Button("Удалить", role: .destructive) {
                if let id = routeToDelete { vm.deleteRoute(routeId: id); routeToDelete = nil }
            }
            Button("Отмена", role: .cancel) { routeToDelete = nil }
        } message: { Text("Это действие нельзя отменить.") }
        // Алерт для типизированных ошибок (sync/share/refresh).
        // delete и copy НЕ публикуют _error — см. HomeViewModelWrapper.LastAction.
        .alert(
            vm.error?.userMessage ?? "Ошибка",
            isPresented: Binding(
                get: { vm.error != nil },
                set: { if !$0 { vm.clearError() } }
            )
        ) {
            if vm.error?.canRetry == true {
                Button("Повторить") { vm.retry() }
            }
            Button("OK", role: .cancel) {}
        }
    }

    // MARK: - Stats Pager

    private var statsCardPager: some View {
        TabView(selection: $statsPage) {
            StatsCard(
                totalWorkMs: vm.totalWorkMs,
                normaHoursMonth: vm.normaHoursMonth,
                normaHoursToday: vm.normaHoursToday,
                todayWorkMs: vm.todayWorkMs,
                isDecimal: vm.settings?.isDecimalTime ?? false,
                considerFutureRoutes: vm.settings?.isConsiderFutureRoute ?? true
            )
            .measurePageHeight(index: 0)
            .tag(0)

            DetailWorkTimeCard(
                totalWorkMs: vm.totalWorkMs,
                nightWorkMs: vm.nightWorkMs,
                passengerWorkMs: vm.passengerWorkMs,
                reserveWorkMs: vm.reserveWorkMs,
                isDecimal: vm.settings?.isDecimalTime ?? false
            )
            .measurePageHeight(index: 1)
            .tag(1)

            DetailTrainCard(
                totalWorkMs: vm.totalWorkMs,
                extServicePhaseMs: 0, // TODO: нужен SalaryCalculationHelper
                longCompositionMs: 0, // TODO: нужен SalaryCalculationHelper
                heavyTrainMs: 0,      // TODO: нужен SalaryCalculationHelper
                onePersonMs: vm.onePersonMs,
                isDecimal: vm.settings?.isDecimalTime ?? false
            )
            .measurePageHeight(index: 2)
            .tag(2)
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .frame(height: statsPagerHeight)
        .onPreferenceChange(StatsPageHeightPreference.self) { dict in
            // Мержим все измерения в state.
            for (k, v) in dict { pageHeights[k] = v }
        }
    }

    // MARK: - Route helpers

    // Текущий маршрут (повторяет логику findCurrentRoute из domain/UtilsForEntities.kt)
    private var currentRoute: DomainRoute? {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let all = vm.routes
        return all
            .filter { r in
                let s = r.basicData.timeStartWork?.int64Value ?? 0
                guard s > 0, now > s else { return false }
                if let e = r.basicData.timeEndWork?.int64Value {
                    return now < e
                }
                // Незавершённый маршрут: считается текущим, только если после него
                // не стартовал другой маршрут (otherStart > s && otherStart <= now)
                return !all.contains { other in
                    let os = other.basicData.timeStartWork?.int64Value ?? 0
                    return os > s && os <= now
                }
            }
            .max { ($0.basicData.timeStartWork?.int64Value ?? 0) < ($1.basicData.timeStartWork?.int64Value ?? 0) }
    }

    // Следующий будущий маршрут (startWork > now)
    private var nextFutureRoute: DomainRoute? {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        return vm.routes
            .filter { ($0.basicData.timeStartWork?.int64Value ?? 0) > now }
            .min { ($0.basicData.timeStartWork?.int64Value ?? Int64.max) < ($1.basicData.timeStartWork?.int64Value ?? Int64.max) }
    }

}

// MARK: - CurrentRouteCard

struct CurrentRouteCard: View {
    let route: DomainRoute

    @State private var timeText = ""

    private var startMs: Int64 { route.basicData.timeStartWork?.int64Value ?? 0 }
    private var breakDuration: Int64 {
        let bs = route.basicData.timeStartBreak?.int64Value ?? 0
        let be = route.basicData.timeEndBreak?.int64Value ?? 0
        return be > bs ? be - bs : 0
    }

    private var trains: [DomainTrain] { route.trains as! [DomainTrain] }
    private var locos: [DomainLocomotive] { route.locomotives as! [DomainLocomotive] }
    private var passengers: [DomainPassenger] { route.passengers as! [DomainPassenger] }

    private func calcTimeText() -> String {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        return TimeFormatter.formatDuration(ms: max(0, nowMs - startMs - breakDuration))
    }

    private let tileSize: CGFloat = 110

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // 1. На работе — таймер (клик → общая форма)
                NavigationLink(destination: FormView(routeId: route.basicData.id)) {
                    tile(title: "На работе") {
                        Text(timeText)
                            .font(.system(size: 20, weight: .semibold, design: .rounded))
                            .foregroundStyle(Color.appPrimary)
                            .contentTransition(.numericText())
                    }
                }
                .buttonStyle(.plain)

                // 2-4. Локомотив / Поезд / Пассажиром.
                // Порядок: сначала заполненные (в каноническом порядке
                // Локо → Поезд → Пассажиром), затем пустые в том же порядке.
                // Так слева всегда нет «дыр» из пунктирных плейсхолдеров:
                // как только юзер добавил, скажем, Поезд — он уезжает к
                // «На работе», а пустые Локо/Пассажиром остаются справа.
                ForEach(tileOrder, id: \.self) { kind in
                    tileView(for: kind)
                }
            }
            .padding(.horizontal, 16)
        }
        .onAppear { timeText = calcTimeText() }
        .onReceive(MinuteSyncTimer.shared.tick) { _ in
            let newText = calcTimeText()
            if newText != timeText {
                withAnimation(.easeInOut(duration: 0.4)) { timeText = newText }
            }
        }
    }

    // MARK: - Порядок тайлов Локо/Поезд/Пассажиром

    private enum TileKind: Hashable { case loco, train, passenger }

    private var locoFilled: Bool { locos.last != nil }

    private var trainFilled: Bool {
        guard let train = trains.last else { return false }
        let hasNumber = (train.number?.isEmpty == false)
        let hasStations = !(train.stations as! [DomainStation]).isEmpty
        return hasNumber || hasStations
    }

    private var passengerFilled: Bool { passengers.last != nil }

    private func isFilled(_ kind: TileKind) -> Bool {
        switch kind {
        case .loco: return locoFilled
        case .train: return trainFilled
        case .passenger: return passengerFilled
        }
    }

    /// Порядок тайлов: заполненные слева (каноническим порядком
    /// Локо → Поезд → Пассажиром), затем пустые в том же порядке.
    private var tileOrder: [TileKind] {
        let canonical: [TileKind] = [.loco, .train, .passenger]
        return canonical.filter { isFilled($0) } + canonical.filter { !isFilled($0) }
    }

    @ViewBuilder
    private func tileView(for kind: TileKind) -> some View {
        switch kind {
        case .loco:
            if let loco = locos.last {
                NavigationLink(destination: FormLocoView(
                    routeId: route.basicData.id, locoId: loco.locoId
                )) {
                    tile(title: "Локомотив", isEmpty: false) {
                        let series = loco.series ?? ""
                        let number = loco.number ?? ""
                        VStack(alignment: .leading, spacing: 2) {
                            if !series.isEmpty {
                                Text(series)
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundStyle(Color.appPrimary)
                                    .lineLimit(1)
                            }
                            if !number.isEmpty {
                                Text("№\(number)")
                                    .font(.system(size: 12))
                                    .foregroundStyle(Color.appSecondary)
                                    .lineLimit(1)
                            }
                        }
                    }
                }
                .buttonStyle(.plain)
            } else {
                NavigationLink(destination: FormLocoView(
                    routeId: route.basicData.id, locoId: nil
                )) {
                    emptyTile(title: "Локомотив")
                }
                .buttonStyle(.plain)
            }

        case .train:
            if trainFilled, let train = trains.last {
                NavigationLink(destination: FormTrainView(
                    routeId: route.basicData.id, trainId: train.trainId
                )) {
                    tile(title: "Поезд", isEmpty: false) {
                        VStack(alignment: .leading, spacing: 2) {
                            if let num = train.number, !num.isEmpty {
                                Text("№\(num)")
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundStyle(Color.appPrimary)
                                    .lineLimit(1)
                            }
                            let station = (train.stations as! [DomainStation]).first?.stationName ?? ""
                            if !station.isEmpty {
                                Text(station)
                                    .font(.system(size: 11))
                                    .foregroundStyle(Color.appSecondary)
                                    .lineLimit(1)
                            }
                        }
                    }
                }
                .buttonStyle(.plain)
            } else {
                NavigationLink(destination: FormTrainView(
                    routeId: route.basicData.id, trainId: nil
                )) {
                    emptyTile(title: "Поезд")
                }
                .buttonStyle(.plain)
            }

        case .passenger:
            if let p = passengers.last {
                NavigationLink(destination: FormPassengerView(
                    routeId: route.basicData.id, passengerId: p.passengerId
                )) {
                    tile(title: "Пассажиром", isEmpty: false) {
                        VStack(alignment: .leading, spacing: 2) {
                            if let n = p.trainNumber, !n.isEmpty {
                                Text("№\(n)")
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundStyle(Color.appPrimary)
                                    .lineLimit(1)
                            }
                            let dep = p.timeDeparture?.int64Value ?? 0
                            if dep > 0 {
                                Text(TimeFormatter.formatDateTime(ms: dep))
                                    .font(.system(size: 11))
                                    .foregroundStyle(Color.appSecondary)
                                    .lineLimit(1)
                            }
                        }
                    }
                }
                .buttonStyle(.plain)
            } else {
                NavigationLink(destination: FormPassengerView(
                    routeId: route.basicData.id, passengerId: nil
                )) {
                    emptyTile(title: "Пассажиром")
                }
                .buttonStyle(.plain)
            }
        }
    }

    /// Пустое состояние тайла: плюс по центру без внутренней плашки,
    /// вся карточка обведена пунктирной рамкой (iOS-паттерн).
    @ViewBuilder
    private func emptyTile(title: String) -> some View {
        VStack(spacing: 0) {
            Image(systemName: "plus")
                .font(.system(size: 24, weight: .regular))
                .foregroundStyle(Color.appAccent)
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            HStack {
                Text(title)
                    .font(.system(size: 11))
                    .foregroundStyle(Color.appSecondary)
                    .lineLimit(1)
                Spacer()
            }
        }
        .padding(10)
        .frame(width: tileSize, height: tileSize)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .strokeBorder(
                    Color.appSecondary.opacity(0.35),
                    style: StrokeStyle(lineWidth: 1, dash: [5, 4])
                )
        )
    }

    @ViewBuilder
    private func tile<Content: View>(
        title: String,
        isEmpty: Bool = false,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(spacing: 0) {
            // Контент центрирован в оставшемся месте (над подписью).
            content()
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)

            // Название — нижний левый угол.
            HStack {
                Text(title)
                    .font(.system(size: 11))
                    .foregroundStyle(Color.appSecondary)
                    .lineLimit(1)
                Spacer()
            }
        }
        .padding(10)
        .frame(width: tileSize, height: tileSize)
        .background(Color.appCard)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

// MARK: - NextRouteCard

struct NextRouteCard: View {
    let route: DomainRoute

    @State private var displayText = ""

    private var startMs: Int64 { route.basicData.timeStartWork?.int64Value ?? 0 }

    private func calcText() -> String {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let ms = max(0, startMs - nowMs)
        return TimeFormatter.formatDuration(ms: ms)
    }

    var body: some View {
        HStack(spacing: 0) {
            // Левая часть: До явки + таймер
            VStack(spacing: 4) {
                Text("До явки")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondary)
                Text(displayText)
                    .font(.system(size: 24, weight: .semibold, design: .rounded))
                    .foregroundStyle(Color.appPrimary)
                    .contentTransition(.numericText())
            }
            .frame(maxWidth: .infinity)

            // Вертикальный разделитель
            Rectangle()
                .fill(Color.appSeparator)
                .frame(width: 1, height: 40)

            // Правая часть: Явка + дата/время
            VStack(spacing: 4) {
                Text("Явка")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.appSecondary)
                if startMs > 0 {
                    Text(TimeFormatter.formatDateTime(ms: startMs))
                        .font(.system(size: 17, weight: .medium))
                        .foregroundStyle(Color.appPrimary)
                }
            }
            .frame(maxWidth: .infinity)
        }
        .padding(16)
        .background(Color.appCard)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .onAppear { displayText = calcText() }
        .onReceive(MinuteSyncTimer.shared.tick) { _ in
            let newText = calcText()
            if newText != displayText {
                withAnimation(.easeInOut(duration: 0.4)) { displayText = newText }
            }
        }
    }
}

// MARK: - DetailWorkTimeCard (страница 2 пейджера)

struct DetailWorkTimeCard: View {
    let totalWorkMs: Int64
    let nightWorkMs: Int64
    let passengerWorkMs: Int64
    let reserveWorkMs: Int64
    let isDecimal: Bool

    private var safeTotal: Int64 { max(totalWorkMs, 1) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(TimeFormatter.formatWorkDuration(ms: totalWorkMs, isDecimal: isDecimal))
                .font(.system(size: 30, weight: .semibold, design: .rounded))
                .foregroundStyle(Color.appPrimary)
                .padding(.bottom, 10)

            ProgressRowView(
                label: "Ночные",
                valueText: TimeFormatter.formatWorkDuration(ms: nightWorkMs, isDecimal: isDecimal),
                valueColor: Color.appSecondary,
                progress: Double(nightWorkMs) / Double(safeTotal),
                barColor: Color.appAccent
            )
            ProgressRowView(
                label: "Пассажиром",
                valueText: TimeFormatter.formatWorkDuration(ms: passengerWorkMs, isDecimal: isDecimal),
                valueColor: Color.appSecondary,
                progress: Double(passengerWorkMs) / Double(safeTotal),
                barColor: Color.appAccent
            )
            ProgressRowView(
                label: "Резервом",
                valueText: TimeFormatter.formatWorkDuration(ms: reserveWorkMs, isDecimal: isDecimal),
                valueColor: Color.appSecondary,
                progress: Double(reserveWorkMs) / Double(safeTotal),
                barColor: Color.appAccent
            )
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(Color.appCard)
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

// MARK: - DetailTrainCard (страница 3 пейджера)

struct DetailTrainCard: View {
    let totalWorkMs: Int64
    let extServicePhaseMs: Int64
    let longCompositionMs: Int64
    let heavyTrainMs: Int64
    let onePersonMs: Int64
    let isDecimal: Bool

    private var safeTotal: Int64 { max(totalWorkMs, 1) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(TimeFormatter.formatWorkDuration(ms: totalWorkMs, isDecimal: isDecimal))
                .font(.system(size: 30, weight: .semibold, design: .rounded))
                .foregroundStyle(Color.appPrimary)
                .padding(.bottom, 10)

            ProgressRowView(
                label: "Удл. плечи обслуживания",
                valueText: TimeFormatter.formatWorkDuration(ms: extServicePhaseMs, isDecimal: isDecimal),
                valueColor: Color.appSecondary,
                progress: Double(extServicePhaseMs) / Double(safeTotal),
                barColor: Color.appAccent
            )
            ProgressRowView(
                label: "Длинносоставные",
                valueText: TimeFormatter.formatWorkDuration(ms: longCompositionMs, isDecimal: isDecimal),
                valueColor: Color.appSecondary,
                progress: Double(longCompositionMs) / Double(safeTotal),
                barColor: Color.appAccent
            )
            ProgressRowView(
                label: "Тяжёлые",
                valueText: TimeFormatter.formatWorkDuration(ms: heavyTrainMs, isDecimal: isDecimal),
                valueColor: Color.appSecondary,
                progress: Double(heavyTrainMs) / Double(safeTotal),
                barColor: Color.appAccent
            )
            ProgressRowView(
                label: "Одно лицо",
                valueText: TimeFormatter.formatWorkDuration(ms: onePersonMs, isDecimal: isDecimal),
                valueColor: Color.appSecondary,
                progress: Double(onePersonMs) / Double(safeTotal),
                barColor: Color.appAccent
            )
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(Color.appCard)
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

// MARK: - StatsCard

struct StatsCard: View {
    let totalWorkMs: Int64
    let normaHoursMonth: Int
    let normaHoursToday: Int
    var todayWorkMs: Int64 = 0
    let isDecimal: Bool
    var considerFutureRoutes: Bool = true

    private var hoursWorked: String {
        TimeFormatter.formatWorkDuration(ms: totalWorkMs, isDecimal: isDecimal)
    }
    private var progressMonth: Double {
        guard normaHoursMonth > 0 else { return 0 }
        return min(Double(totalWorkMs) / Double(Int64(normaHoursMonth) * 3_600_000), 1.0)
    }
    private var progressToday: Double {
        guard normaHoursToday > 0 else { return 0 }
        return min(Double(totalWorkMs) / Double(Int64(normaHoursToday) * 3_600_000), 1.0)
    }
    private var todayDone: Bool {
        normaHoursToday > 0 && totalWorkMs >= Int64(normaHoursToday) * 3_600_000
    }

    // Чип: до нормы / сверх (как в Android widget)
    private var normaChip: (text: String, label: String, isOvertime: Bool)? {
        guard normaHoursMonth > 0 else { return nil }
        let normaMs = Int64(normaHoursMonth) * 3_600_000
        let diff = totalWorkMs - normaMs
        let isOvertime = diff >= 0
        let remaining = abs(diff)
        return (
            text: TimeFormatter.formatDuration(ms: remaining),
            label: isOvertime ? "сверх" : "до нормы",
            isOvertime: isOvertime
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Строка 1: часы слева, чип справа
            HStack(alignment: .top) {
                Text(hoursWorked)
                    .font(.system(size: 30, weight: .semibold, design: .rounded))
                    .foregroundStyle(Color.appPrimary)

                Spacer()

                if let chip = normaChip {
                    // Подпись сверху (secondary), значение снизу цветом
                    // состояния. Без пилюли-фона — два разных элемента
                    // не склеиваются в один, как было раньше.
                    VStack(alignment: .trailing, spacing: 0) {
                        Text(chip.label)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundStyle(Color.appSecondary)
                        Text(chip.text)
                            .font(.system(size: 17, weight: .semibold, design: .rounded))
                            .foregroundStyle(chip.isOvertime ? Color.appWarning : Color.appAccent)
                            .contentTransition(.numericText())
                    }
                    .padding(.top, 4)
                }
            }
            .padding(.bottom, 14)

            // Прогресс 1: норма на месяц
            ProgressRowView(
                label: "Норма на месяц",
                valueText: "\(normaHoursMonth) ч.",
                valueColor: Color.appSecondary,
                progress: progressMonth,
                barColor: Color.appAccent
            )

            // Прогресс 2: норма на сегодняшнее число
            let normaTodayProgress: Double = normaHoursToday > 0
                ? min(Double(totalWorkMs) / Double(Int64(normaHoursToday) * 3_600_000), 1.0) : 0
            ProgressRowView(
                label: "Норма на \(todayDateStr())",
                valueText: "\(normaHoursToday) ч.",
                valueColor: Color.appSecondary,
                progress: normaTodayProgress,
                barColor: Color.appAccent
            )

            // Прогресс 3: отработано на сегодня — показывается только если
            // в настройках включено «учитывать будущие маршруты»
            if considerFutureRoutes {
                let todayProgress: Double = normaHoursToday > 0
                    ? min(Double(todayWorkMs) / Double(Int64(normaHoursToday) * 3_600_000), 1.0) : 0
                ProgressRowView(
                    label: "Отработано на \(todayDateStr())",
                    valueText: TimeFormatter.formatWorkDuration(ms: todayWorkMs, isDecimal: isDecimal),
                    valueColor: Color.appSecondary,
                    progress: todayProgress,
                    barColor: Color.appAccent
                )
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(Color.appCard)
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func todayDateStr() -> String {
        let f = DateFormatter(); f.dateFormat = "dd.MM"; return f.string(from: Date())
    }
}

struct ProgressRowView: View {
    let label: String
    let valueText: String
    let valueColor: Color
    let progress: Double
    let barColor: Color

    var body: some View {
        VStack(spacing: 5) {
            HStack {
                Text(label)
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
                Spacer()
                Text(valueText)
                    .font(.system(size: 12, weight: valueColor == .appSuccess ? .medium : .regular))
                    .foregroundStyle(valueColor)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color.appElevated)
                        .frame(height: 5)
                    RoundedRectangle(cornerRadius: 3)
                        .fill(barColor)
                        .frame(width: geo.size.width * min(progress, 1.0), height: 5)
                        .animation(.easeInOut(duration: 0.4), value: progress)
                }
            }
            .frame(height: 5)
        }
        .padding(.bottom, 10)
    }
}

// MARK: - RoutesSection

struct RoutesSection: View {
    let routes: [DomainRoute]
    let totalCount: Int
    let onRouteClick: (DomainRoute) -> Void
    let onCopyRoute: (String) -> Void
    let onDeleteRoute: (String) -> Void
    let onToggleFavorite: (String) -> Void
    let onShareRoute: (String) -> Void
    let onSyncRoute: (String) -> Void
    let allRoutesDestination: () -> AllRoutesView
    var allRoutes: [DomainRoute] = []
    var settings: DomainUserSettings? = nil

    /// ID маршрута, открываемого тапом по строке / preview (через UIKit bridge).
    @State private var openRouteId: String? = nil

    var body: some View {
        VStack(spacing: 0) {
            // Заголовок
            HStack {
                Text("Маршруты")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.primary)
                Spacer()
                NavigationLink(destination: allRoutesDestination()) {
                    Text("Все (\(totalCount)) ›")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.appSecondary)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 8)

            // Список
            if routes.isEmpty {
                Text("Нет маршрутов за этот месяц")
                    .font(.subheadline).foregroundColor(.secondary)
                    .frame(maxWidth: .infinity).padding(.vertical, 20)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(routes.enumerated()), id: \.element.basicData.id) { idx, route in
                        RouteContextMenuRow(
                            content: RouteItemView(
                                route: route,
                                number: totalCount - idx,
                                isExpanded: false,
                                settings: settings
                            ),
                            preview: {
                                RoutePreviewCard(
                                    route: route,
                                    allRoutes: allRoutes,
                                    settings: settings
                                )
                            },
                            actions: {
                                routeMenuActions(for: route)
                            },
                            onCommit: {
                                openRouteId = route.basicData.id
                            }
                        )
                        if idx < routes.count - 1 {
                            Divider().padding(.leading, 16)
                        }
                    }
                }
                .background(Color.appCard)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .padding(.horizontal, 16)
            }
        }
        .navigationDestination(
            isPresented: Binding(
                get: { openRouteId != nil },
                set: { if !$0 { openRouteId = nil } }
            )
        ) {
            if let id = openRouteId {
                FormView(routeId: id)
            }
        }
    }

    private func routeMenuActions(for route: DomainRoute) -> [ContextMenuAction] {
        [
            ContextMenuAction(title: "Просмотр", systemImage: "eye") {
                openRouteId = route.basicData.id
            },
            ContextMenuAction(title: "Сохранить в облаке", systemImage: "icloud.and.arrow.up") {
                onSyncRoute(route.basicData.id)
            },
            ContextMenuAction(
                title: route.basicData.isFavorite ? "Убрать из избранного" : "В избранное",
                systemImage: route.basicData.isFavorite ? "heart.slash" : "heart"
            ) {
                onToggleFavorite(route.basicData.id)
            },
            ContextMenuAction(title: "Дублировать", systemImage: "doc.on.doc") {
                onCopyRoute(route.basicData.id)
            },
            ContextMenuAction(title: "Поделиться", systemImage: "square.and.arrow.up") {
                onShareRoute(route.basicData.id)
            },
            ContextMenuAction(title: "Удалить", systemImage: "trash", isDestructive: true) {
                onDeleteRoute(route.basicData.id)
            }
        ]
    }
}

// MARK: - RoutePreviewCard (Telegram-style long-press preview)

private struct PreviewContentHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}

struct RoutePreviewCard: View {
    let route: DomainRoute
    var allRoutes: [DomainRoute] = []
    var settings: DomainUserSettings? = nil

    private var startMs: Int64 { route.basicData.timeStartWork?.int64Value ?? 0 }
    private var endMs:   Int64 { route.basicData.timeEndWork?.int64Value ?? 0 }
    private var breakMs: Int64 {
        let bs = route.basicData.timeStartBreak?.int64Value ?? 0
        let be = route.basicData.timeEndBreak?.int64Value ?? 0
        return be > bs ? be - bs : 0
    }
    private var durationMs: Int64 {
        guard endMs > startMs else { return 0 }
        return endMs - startMs - breakMs
    }
    private var trains:    [DomainTrain]      { route.trains as! [DomainTrain] }
    private var locos:     [DomainLocomotive] { route.locomotives as! [DomainLocomotive] }
    private var passengers:[DomainPassenger]  { route.passengers as! [DomainPassenger] }

    private var passengerMs: Int64 {
        passengers.reduce(0) { acc, p in
            let d = p.timeDeparture?.int64Value ?? 0
            let a = p.timeArrival?.int64Value ?? 0
            return a > d ? acc + (a - d) : acc
        }
    }

    private func formatTimeOnly(ms: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(ms) / 1000)
        let f = DateFormatter(); f.dateFormat = "HH:mm"; return f.string(from: date)
    }

    var body: some View {
        // Ширина фиксированная (360pt). Высота = по контенту, но не более
        // maxPreviewHeight — чтобы iOS не зумил preview и контекстное меню
        // помещалось целиком. Если контент выше лимита — обрезаем по нижнему
        // краю с плавным fade-градиентом ("есть ещё, откройте полный экран").
        // ScrollView в preview не используем: UIKit context-menu перехватывает
        // жесты и скролл внутри не работает.
        previewContent
            .fixedSize(horizontal: false, vertical: true)
            .background(
                GeometryReader { g in
                    Color.clear.preference(
                        key: PreviewContentHeightKey.self,
                        value: g.size.height
                    )
                }
            )
            .onPreferenceChange(PreviewContentHeightKey.self) { h in
                contentHeight = h
            }
            .frame(width: 360, alignment: .top)
            .frame(maxHeight: maxPreviewHeight, alignment: .top)
            .clipped()
            .overlay(alignment: .bottom) {
                if contentHeight > maxPreviewHeight {
                    LinearGradient(
                        colors: [Color.appCard.opacity(0), Color.appCard],
                        startPoint: .top, endPoint: .bottom
                    )
                    .frame(height: 36)
                    .allowsHitTesting(false)
                }
            }
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color.appCard)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    @State private var contentHeight: CGFloat = 1
    private let maxPreviewHeight: CGFloat = 460

    @ViewBuilder
    private var previewContent: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Шапка: "МАРШРУТ №N" — только номер, без даты/времени.
            HStack(alignment: .firstTextBaseline) {
                Text("МАРШРУТ")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(Color.appSecondary)
                if let num = route.basicData.number, !num.isEmpty {
                    Text("№\(num)")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Color.appPrimary)
                }
                Spacer()
            }

            // Блок времени — всегда показываем (даже если нет окончания).
            timeBlock

            if !trains.isEmpty {
                previewSection(title: "Поезда") {
                    VStack(alignment: .leading, spacing: 8) {
                        let sorted = trains.sorted {
                            let t0 = (($0.stations as! [DomainStation]).first?.timeDeparture?.int64Value) ?? 0
                            let t1 = (($1.stations as! [DomainStation]).first?.timeDeparture?.int64Value) ?? 0
                            return t0 < t1
                        }
                        ForEach(sorted, id: \.trainId) { train in
                            trainDetail(train)
                        }
                    }
                }
            }

            if !locos.isEmpty {
                previewSection(title: "Локомотивы") {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(locos, id: \.locoId) { loco in
                            locoDetail(loco)
                        }
                    }
                }
            }

            if !passengers.isEmpty {
                previewSection(title: "Пассажиром") {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(passengers, id: \.passengerId) { p in
                            passengerDetail(p)
                        }
                    }
                }
            }

            if let notes = route.basicData.notes, !notes.isEmpty {
                previewSection(title: "Заметки") {
                    Text(notes)
                        .font(.system(size: 13))
                        .foregroundStyle(Color.appPrimary)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Time block

    @ViewBuilder
    private var timeBlock: some View {
        VStack(alignment: .leading, spacing: 6) {
            // Явка (начало) — всегда, если есть.
            if startMs > 0 {
                kvRow(icon: "calendar",
                      label: "Явка",
                      value: TimeFormatter.formatDateTime(ms: startMs))
            }
            // Окончание — если задано.
            if endMs > 0 {
                kvRow(icon: "flag",
                      label: "Окончание",
                      value: TimeFormatter.formatDateTime(ms: endMs))
            }
            // Время работы.
            if durationMs > 0 {
                kvRow(icon: "clock",
                      label: "Время работы",
                      value: TimeFormatter.formatDuration(ms: durationMs))
            }
            // Перерыв.
            if breakMs > 0,
               let bs = route.basicData.timeStartBreak?.int64Value,
               let be = route.basicData.timeEndBreak?.int64Value {
                kvRow(icon: "pause.circle",
                      label: "Перерыв",
                      value: "\(formatTimeOnly(ms: bs))–\(formatTimeOnly(ms: be)) · \(TimeFormatter.formatDuration(ms: breakMs))")
            }
            // Пассажиром.
            if passengerMs > 0 {
                kvRow(icon: "figure.walk",
                      label: "Пассажиром",
                      value: TimeFormatter.formatDuration(ms: passengerMs))
            }
            // Тип отдыха.
            restRows
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appElevated)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    @ViewBuilder
    private var restRows: some View {
        if route.basicData.restPointOfTurnover {
            // Отдых в ПО: заголовок + две под-строки (Короткий / Полный)
            // с правым выравниванием значения «Продолжительность · до ДАТА».
            let minPO = settings?.minTimeRestPointOfTurnover ?? 10_800_000
            let short = computeShortRest(minTime: minPO)
            let full  = computeFullRest(minTime: minPO)
            if short != nil || full != nil {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Image(systemName: "bed.double").font(.system(size: 11))
                            .foregroundStyle(Color.appSecondary)
                        Text("Отдых в ПО").font(.system(size: 12))
                            .foregroundStyle(Color.appSecondary)
                        Spacer(minLength: 0)
                    }
                    if let sr = short {
                        poRestRow(label: "Короткий:", duration: sr.duration, endAt: sr.endAt)
                    }
                    if let fr = full {
                        poRestRow(label: "Полный:", duration: fr.duration, endAt: fr.endAt)
                    }
                }
            }
        } else {
            // Домашний отдых: «Домашний отдых» слева, продолжительность справа;
            // под ней — дата и время окончания отдыха.
            let minHome = settings?.minTimeHomeRest ?? 57_600_000
            if let hr = computeHomeRest(minTime: minHome) {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Image(systemName: "house").font(.system(size: 11))
                            .foregroundStyle(Color.appSecondary)
                        Text("Домашний отдых").font(.system(size: 12))
                            .foregroundStyle(Color.appSecondary)
                        Spacer(minLength: 6)
                        Text(TimeFormatter.formatDuration(ms: hr.duration))
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(Color.appPrimary)
                    }
                    HStack {
                        Spacer(minLength: 0)
                        Text("до \(TimeFormatter.formatDateTime(ms: hr.endAt))")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(Color.appPrimary)
                    }
                }
            }
        }
    }

    /// Строка под-отдыха ПО: «Короткий:» / «Полный:» слева, справа —
    /// «ЧЧ:ММ · до ДАТА ВРЕМЯ».
    @ViewBuilder
    private func poRestRow(label: String, duration: Int64, endAt: Int64) -> some View {
        HStack(spacing: 6) {
            Text(label).font(.system(size: 12))
                .foregroundStyle(Color.appSecondary)
            Spacer(minLength: 6)
            Text("\(TimeFormatter.formatDuration(ms: duration)) · до \(TimeFormatter.formatDateTime(ms: endAt))")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(Color.appPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.85)
        }
        .padding(.leading, 17) // отступ под иконку заголовка
    }

    // MARK: - Rest math (mirrors domain/UtilsForEntities.kt)

    private func computeShortRest(minTime: Int64) -> (duration: Int64, endAt: Int64)? {
        guard startMs > 0, endMs > startMs else { return nil }
        let work = endMs - startMs - breakMs
        var half = work / 2
        if half % 60_000 != 0 { half += 60_000 }
        let dur = max(half, minTime)
        return (dur, endMs + dur)
    }

    private func computeFullRest(minTime: Int64) -> (duration: Int64, endAt: Int64)? {
        guard startMs > 0, endMs > startMs else { return nil }
        let work = endMs - startMs - breakMs
        let dur = max(work, minTime)
        return (dur, endMs + dur)
    }

    private func computeHomeRest(minTime: Int64) -> (duration: Int64, endAt: Int64)? {
        guard !allRoutes.isEmpty,
              let idx0 = allRoutes.firstIndex(where: { $0.basicData.id == route.basicData.id })
        else { return nil }
        // Строим цепочку назад через restPointOfTurnover-предшественников.
        var chain: [DomainRoute] = [allRoutes[idx0]]
        var i = idx0
        if i > 0 {
            i -= 1
            while allRoutes[i].basicData.restPointOfTurnover {
                chain.append(allRoutes[i])
                if i == 0 { break }
                i -= 1
            }
        }
        chain.sort {
            ($0.basicData.timeStartWork?.int64Value ?? 0) <
            ($1.basicData.timeStartWork?.int64Value ?? 0)
        }
        var totalWork: Int64 = 0
        var totalRest: Int64 = 0
        for (k, r) in chain.enumerated() {
            if let s = r.basicData.timeStartWork?.int64Value,
               let e = r.basicData.timeEndWork?.int64Value, e > s {
                let bs = r.basicData.timeStartBreak?.int64Value ?? 0
                let be = r.basicData.timeEndBreak?.int64Value ?? 0
                let brk: Int64 = (be > bs) ? (be - bs) : 0
                totalWork += (e - s) - brk
            }
            if k != chain.count - 1 {
                if let eNow = r.basicData.timeEndWork?.int64Value,
                   let sNext = chain[k + 1].basicData.timeStartWork?.int64Value {
                    totalRest += sNext - eNow
                }
            }
        }
        var home = Int64(Double(totalWork) * 2.6) - totalRest
        if home < minTime { home = minTime }
        guard let startLast = chain.last?.basicData.timeStartWork?.int64Value else { return nil }
        return (home, startLast + home)
    }

    // MARK: - Train detail

    @ViewBuilder
    private func trainDetail(_ train: DomainTrain) -> some View {
        let stations = train.stations as! [DomainStation]
        let hasNumber = (train.number?.isEmpty == false)
        let from = stations.first?.stationName ?? ""
        let to   = stations.count > 1 ? stations.last?.stationName ?? "" : ""
        let path: String = {
            if from.isEmpty && to.isEmpty { return "" }
            if to.isEmpty { return from }
            if from.isEmpty { return to }
            return "\(from) – \(to)"
        }()
        VStack(alignment: .leading, spacing: 6) {
            if hasNumber || !path.isEmpty {
                HStack(spacing: 6) {
                    if hasNumber {
                        Text("№\(train.number!)").font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color.appPrimary)
                    }
                    if !path.isEmpty {
                        Text(path).font(.system(size: 13))
                            .foregroundStyle(Color.appPrimary).lineLimit(1)
                    }
                }
            }
            // Характеристики — НАД станциями, с переносом по ширине.
            // Без "distance" (он вынесен под список станций рядом со скоростями).
            let ud: String? = train.conditionalLength.flatMap { s -> String? in
                let t = s.trimmingCharacters(in: .whitespaces)
                if t.isEmpty { return nil }
                if let d = Double(t.replacingOccurrences(of: ",", with: ".")) {
                    if d == d.rounded() { return "у.д. \(Int64(d))" }
                }
                return "у.д. \(t)"
            }
            let specs: [String] = [
                train.weight.flatMap { $0.isEmpty ? nil : "Вес: \($0)" },
                train.axle.flatMap { $0.isEmpty ? nil : "Оси: \($0)" },
                ud,
                train.isHeavyLongDistance ? "Повыш. длина/масса" : nil
            ].compactMap { $0 }
            if !specs.isEmpty {
                WrapChips(items: specs) { chip($0) }
            }
            // Станции с временами.
            ForEach(Array(stations.enumerated()), id: \.element.stationId) { _, st in
                let name = st.stationName ?? "—"
                let arr = st.timeArrival?.int64Value
                let dep = st.timeDeparture?.int64Value
                let timeStr: String = {
                    switch (arr, dep) {
                    case let (a?, d?): return "\(formatTimeOnly(ms: a)) → \(formatTimeOnly(ms: d))"
                    case let (a?, nil): return "приб. \(formatTimeOnly(ms: a))"
                    case let (nil, d?): return "отпр. \(formatTimeOnly(ms: d))"
                    default: return ""
                    }
                }()
                HStack(spacing: 6) {
                    Image(systemName: "circle.fill").font(.system(size: 4))
                        .foregroundStyle(Color.appSecondary)
                    Text(name).font(.system(size: 12)).foregroundStyle(Color.appPrimary)
                    Spacer(minLength: 6)
                    if !timeStr.isEmpty {
                        Text(timeStr).font(.system(size: 12, weight: .medium))
                            .foregroundStyle(Color.appPrimary)
                    }
                }
            }
            // Путь и скорости — под списком станций.
            trainStatsRow(train, stations: stations)
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appElevated)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    /// Путь, участковая и техническая скорости (порт Train.getTechnicalSpeed /
    /// getSectionSpeed / getTravelTime из domain/UtilsForEntities.kt).
    @ViewBuilder
    private func trainStatsRow(_ train: DomainTrain, stations: [DomainStation]) -> some View {
        let distKm = train.distance.flatMap { Double($0.replacingOccurrences(of: ",", with: ".")) }
        let travelMs: Int64? = {
            guard stations.count >= 2,
                  let dep = stations.first?.timeDeparture?.int64Value,
                  let arr = stations.last?.timeArrival?.int64Value,
                  arr > dep else { return nil }
            return arr - dep
        }()
        // Техническая: расстояние / (travel - суммарные стоянки на промежут. станциях).
        let techSpeed: Double? = {
            guard let d = distKm, let travel = travelMs else { return nil }
            var stopMs: Int64 = 0
            if stations.count > 2 {
                for i in 1..<(stations.count - 1) {
                    if let a = stations[i].timeArrival?.int64Value,
                       let dp = stations[i].timeDeparture?.int64Value, dp > a {
                        stopMs += dp - a
                    }
                }
            }
            let running = travel - stopMs
            guard running > 0 else { return nil }
            return d / (Double(running) / 3_600_000.0)
        }()
        // Участковая: расстояние / travel.
        let sectionSpeed: Double? = {
            guard let d = distKm, let travel = travelMs, travel > 0 else { return nil }
            return d / (Double(travel) / 3_600_000.0)
        }()
        if distKm != nil || sectionSpeed != nil || techSpeed != nil {
            VStack(alignment: .leading, spacing: 2) {
                if let d = distKm {
                    kvRow(icon: "road.lanes", label: "Расстояние", value: "\(fmtNum(d)) км")
                }
                if let v = sectionSpeed {
                    kvRow(icon: "speedometer", label: "Участковая", value: "\(fmtNum(v)) км/ч")
                }
                if let v = techSpeed {
                    kvRow(icon: "gauge.with.dots.needle.67percent", label: "Техническая", value: "\(fmtNum(v)) км/ч")
                }
            }
            .padding(.top, 4)
        }
    }

    // MARK: - Locomotive detail

    @ViewBuilder
    private func locoDetail(_ loco: DomainLocomotive) -> some View {
        let s = (loco.series ?? "").trimmingCharacters(in: .whitespaces)
        let n = (loco.number ?? "").trimmingCharacters(in: .whitespaces)
        let title: String = {
            if s.isEmpty && n.isEmpty { return "Локомотив" }
            if n.isEmpty { return s }
            if s.isEmpty { return "№\(n)" }
            return "\(s) №\(n)"
        }()
        let isDiesel = (loco.type.name == "DIESEL")
        let typeStr: String = isDiesel ? "Тепловоз" : "Электровоз"

        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Text(title).font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Color.appPrimary)
                Text("·").foregroundStyle(Color.appSecondary)
                Text(typeStr).font(.system(size: 12)).foregroundStyle(Color.appSecondary)
            }
            // Времена приёмки / сдачи.
            if let a1 = loco.timeStartOfAcceptance?.int64Value,
               let a2 = loco.timeEndOfAcceptance?.int64Value, a2 > a1 {
                kvRow(icon: "arrow.down.circle", label: "Приёмка",
                      value: "\(formatTimeOnly(ms: a1))–\(formatTimeOnly(ms: a2)) · \(TimeFormatter.formatDuration(ms: a2 - a1))")
            } else if let a1 = loco.timeStartOfAcceptance?.int64Value {
                kvRow(icon: "arrow.down.circle", label: "Приёмка", value: formatTimeOnly(ms: a1))
            }
            if let d1 = loco.timeStartOfDelivery?.int64Value,
               let d2 = loco.timeEndOfDelivery?.int64Value, d2 > d1 {
                kvRow(icon: "arrow.up.circle", label: "Сдача",
                      value: "\(formatTimeOnly(ms: d1))–\(formatTimeOnly(ms: d2)) · \(TimeFormatter.formatDuration(ms: d2 - d1))")
            } else if let d1 = loco.timeStartOfDelivery?.int64Value {
                kvRow(icon: "arrow.up.circle", label: "Сдача", value: formatTimeOnly(ms: d1))
            }
            // Счётчики отопления / собственных нужд.
            if let ha = loco.heatingCounterAccepted?.doubleValue,
               let hd = loco.heatingCounterDelivery?.doubleValue, hd >= ha {
                kvRow(icon: "flame", label: "Отопление",
                      value: "\(fmtNum(ha)) → \(fmtNum(hd)) · Δ \(fmtNum(hd - ha))")
            }
            if let aa = loco.auxiliaryCounterAccepted?.doubleValue,
               let ad = loco.auxiliaryCounterDelivery?.doubleValue, ad >= aa {
                kvRow(icon: "bolt.circle", label: "Собств. нужды",
                      value: "\(fmtNum(aa)) → \(fmtNum(ad)) · Δ \(fmtNum(ad - aa))")
            }

            if isDiesel {
                dieselBlock(loco)
            } else {
                electricBlock(loco)
            }
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appElevated)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    // MARK: - Electric sections

    @ViewBuilder
    private func electricBlock(_ loco: DomainLocomotive) -> some View {
        let sections = loco.electricSectionList as! [DomainSectionElectric]
        if !sections.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                ForEach(Array(sections.enumerated()), id: \.element.sectionId) { idx, sec in
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Секция \(idx + 1)")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(Color.appSecondary)
                        if let d = delta(sec.acceptedEnergy?.doubleValue, sec.deliveryEnergy?.doubleValue) {
                            kvRow(icon: "bolt", label: "Расход",
                                  value: "\(fmtNum(sec.acceptedEnergy?.doubleValue ?? 0)) → \(fmtNum(sec.deliveryEnergy?.doubleValue ?? 0)) · \(fmtNum(d))")
                        }
                        if let d = delta(sec.acceptedRecovery?.doubleValue, sec.deliveryRecovery?.doubleValue) {
                            kvRow(icon: "arrow.triangle.2.circlepath", label: "Рекуперация",
                                  value: "\(fmtNum(sec.acceptedRecovery?.doubleValue ?? 0)) → \(fmtNum(sec.deliveryRecovery?.doubleValue ?? 0)) · \(fmtNum(d))")
                        }
                        if let d = delta(sec.acceptedEnergyOtherCurrent?.doubleValue, sec.deliveryEnergyOtherCurrent?.doubleValue) {
                            kvRow(icon: "bolt.badge.a", label: "Расход (др.)",
                                  value: fmtNum(d))
                        }
                        if let d = delta(sec.acceptedRecoveryOtherCurrent?.doubleValue, sec.deliveryRecoveryOtherCurrent?.doubleValue) {
                            kvRow(icon: "arrow.triangle.2.circlepath.circle", label: "Рекуп. (др.)",
                                  value: fmtNum(d))
                        }
                    }
                }
            }
            // Итоги.
            let totalEnergy = sections.reduce(0.0) { $0 + (delta($1.acceptedEnergy?.doubleValue, $1.deliveryEnergy?.doubleValue) ?? 0) }
                + sections.reduce(0.0) { $0 + (delta($1.acceptedEnergyOtherCurrent?.doubleValue, $1.deliveryEnergyOtherCurrent?.doubleValue) ?? 0) }
            let totalRecov = sections.reduce(0.0) { $0 + (delta($1.acceptedRecovery?.doubleValue, $1.deliveryRecovery?.doubleValue) ?? 0) }
                + sections.reduce(0.0) { $0 + (delta($1.acceptedRecoveryOtherCurrent?.doubleValue, $1.deliveryRecoveryOtherCurrent?.doubleValue) ?? 0) }
            Divider().padding(.vertical, 2)
            if totalEnergy > 0 {
                kvRow(icon: "sum", label: "Итого расход", value: "\(fmtNum(totalEnergy)) кВт·ч")
            }
            if totalRecov > 0 {
                kvRow(icon: "sum", label: "Итого рекуперация", value: "\(fmtNum(totalRecov)) кВт·ч")
            }
            // Статистика vs норма.
            let norma = (loco.normaElectricCurrent1?.doubleValue ?? 0) + (loco.normaElectricCurrent2?.doubleValue ?? 0)
            if norma > 0 && totalEnergy > 0 {
                let diff = totalEnergy - norma
                let sign = diff >= 0 ? "+" : ""
                kvRow(icon: "chart.bar", label: "Норма / Δ",
                      value: "\(fmtNum(norma)) · \(sign)\(fmtNum(diff))")
            }
        }
    }

    // MARK: - Diesel sections

    @ViewBuilder
    private func dieselBlock(_ loco: DomainLocomotive) -> some View {
        let sections = loco.dieselSectionList as! [DomainSectionDiesel]
        if !sections.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                ForEach(Array(sections.enumerated()), id: \.element.sectionId) { idx, sec in
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Секция \(idx + 1)")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(Color.appSecondary)
                        if let d = delta(sec.acceptedFuel?.doubleValue, sec.deliveryFuel?.doubleValue) {
                            kvRow(icon: "fuelpump", label: "Топливо",
                                  value: "\(fmtNum(sec.acceptedFuel?.doubleValue ?? 0)) → \(fmtNum(sec.deliveryFuel?.doubleValue ?? 0)) · \(fmtNum(d)) л")
                        }
                        if let c = sec.coefficient?.doubleValue, c > 0 {
                            kvRow(icon: "scalemass", label: "Коэффициент", value: fmtNum(c))
                        }
                        if let fs = sec.fuelSupply?.doubleValue, fs > 0 {
                            kvRow(icon: "drop", label: "Экипировка", value: "\(fmtNum(fs)) л")
                        }
                        if let fsk = sec.fuelSupplyInKilo?.doubleValue, fsk > 0 {
                            kvRow(icon: "scalemass.fill", label: "Экипировка (кг)", value: "\(fmtNum(fsk)) кг")
                        }
                    }
                }
            }
            // Итоги.
            let totalLiters = sections.reduce(0.0) { $0 + (delta($1.acceptedFuel?.doubleValue, $1.deliveryFuel?.doubleValue) ?? 0) }
            // Средний коэффициент для перевода в кг.
            let coeffs = sections.compactMap { $0.coefficient?.doubleValue }.filter { $0 > 0 }
            let avgCoeff = coeffs.isEmpty ? 0 : coeffs.reduce(0, +) / Double(coeffs.count)
            let totalKg = avgCoeff > 0 ? totalLiters * avgCoeff : 0
            Divider().padding(.vertical, 2)
            if totalLiters > 0 {
                kvRow(icon: "sum", label: "Итого расход (л)", value: "\(fmtNum(totalLiters)) л")
            }
            if totalKg > 0 {
                kvRow(icon: "sum", label: "Итого расход (кг)", value: "\(fmtNum(totalKg)) кг")
            }
            // Статистика vs норма.
            if let normaStr = loco.normaDiesel,
               let norma = Double(normaStr.replacingOccurrences(of: ",", with: ".")),
               norma > 0 && totalLiters > 0 {
                let diff = totalLiters - norma
                let sign = diff >= 0 ? "+" : ""
                kvRow(icon: "chart.bar", label: "Норма / Δ",
                      value: "\(fmtNum(norma)) · \(sign)\(fmtNum(diff)) л")
            }
        }
    }

    // MARK: - Math helpers

    private func delta(_ a: Double?, _ b: Double?) -> Double? {
        guard let a = a, let b = b, b >= a, (b - a) > 0 else { return nil }
        return b - a
    }

    private func fmtNum(_ d: Double) -> String {
        if d == d.rounded() { return String(Int64(d)) }
        return String(format: "%.2f", d)
    }

    // MARK: - Passenger detail

    @ViewBuilder
    private func passengerDetail(_ p: DomainPassenger) -> some View {
        let hasNumber = (p.trainNumber?.isEmpty == false)
        let dep = p.timeDeparture?.int64Value
        let arr = p.timeArrival?.int64Value
        let depStation = (p.stationDeparture ?? "").trimmingCharacters(in: .whitespaces)
        let arrStation = (p.stationArrival   ?? "").trimmingCharacters(in: .whitespaces)
        VStack(alignment: .leading, spacing: 4) {
            // Заголовок: только номер поезда (если задан).
            if hasNumber {
                HStack(spacing: 6) {
                    Text("№\(p.trainNumber!)").font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color.appPrimary)
                    Spacer(minLength: 0)
                }
            }
            // В пути.
            if let d = dep, let a = arr, a > d {
                kvRow(icon: "clock", label: "В пути",
                      value: TimeFormatter.formatDuration(ms: a - d))
            }
            // Отправление: Станция · Дата Время.
            if !depStation.isEmpty || dep != nil {
                let value = [
                    depStation.isEmpty ? nil : depStation,
                    dep.map { TimeFormatter.formatDateTime(ms: $0) }
                ].compactMap { $0 }.joined(separator: " · ")
                kvRow(icon: "arrow.up.circle", label: "Отправление", value: value)
            }
            // Прибытие: Станция · Дата Время.
            if !arrStation.isEmpty || arr != nil {
                let value = [
                    arrStation.isEmpty ? nil : arrStation,
                    arr.map { TimeFormatter.formatDateTime(ms: $0) }
                ].compactMap { $0 }.joined(separator: " · ")
                kvRow(icon: "arrow.down.circle", label: "Прибытие", value: value)
            }
            // Заметки.
            if let notes = p.notes, !notes.isEmpty {
                kvRow(icon: "note.text", label: "Заметки", value: notes)
            }
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appElevated)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    // MARK: - Helpers

    @ViewBuilder
    private func kvRow(icon: String, label: String, value: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon).font(.system(size: 11))
                .foregroundStyle(Color.appSecondary)
            Text(label).font(.system(size: 12))
                .foregroundStyle(Color.appSecondary)
            Spacer(minLength: 6)
            Text(value).font(.system(size: 12, weight: .medium))
                .foregroundStyle(Color.appPrimary)
        }
    }

    @ViewBuilder
    private func chip(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .medium))
            .foregroundStyle(Color.appSecondary)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Color.appCard)
            .clipShape(Capsule())
    }

    @ViewBuilder
    private func previewSection<C: View>(title: String, @ViewBuilder content: () -> C) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(Color.appSecondary)
                .textCase(.uppercase)
            content()
        }
    }
}

// MARK: - WrapChips (обёртывающий ряд chip-элементов)

/// Простой wrap-layout через SwiftUI Layout API (iOS 16+): кладёт элементы
/// в строки с переносом, если не влезают по ширине контейнера.
struct WrapChips<Content: View>: View {
    let items: [String]
    let spacing: CGFloat = 6
    let lineSpacing: CGFloat = 6
    @ViewBuilder let content: (String) -> Content

    var body: some View {
        FlowLayout(spacing: spacing, lineSpacing: lineSpacing) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, s in
                content(s)
            }
        }
    }
}

struct FlowLayout: Layout {
    var spacing: CGFloat = 6
    var lineSpacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, lineH: CGFloat = 0, totalW: CGFloat = 0
        for sv in subviews {
            let size = sv.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 {
                y += lineH + lineSpacing
                totalW = max(totalW, x - spacing)
                x = 0; lineH = 0
            }
            x += size.width + spacing
            lineH = max(lineH, size.height)
        }
        totalW = max(totalW, x - spacing)
        return CGSize(width: max(0, totalW), height: y + lineH)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let maxWidth = bounds.width
        var x: CGFloat = 0, y: CGFloat = 0, lineH: CGFloat = 0
        for sv in subviews {
            let size = sv.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 {
                y += lineH + lineSpacing
                x = 0; lineH = 0
            }
            sv.place(at: CGPoint(x: bounds.minX + x, y: bounds.minY + y),
                     proposal: ProposedViewSize(size))
            x += size.width + spacing
            lineH = max(lineH, size.height)
        }
    }
}

// MARK: - Share sheet helper (UIActivityViewController)

enum ShareSheetPresenter {
    static func present(text: String) {
        guard !text.isEmpty,
              let scene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene }).first,
              let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController
        else { return }
        let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        // Для iPad — popover
        if let pop = av.popoverPresentationController {
            pop.sourceView = root.view
            pop.sourceRect = CGRect(x: root.view.bounds.midX,
                                    y: root.view.bounds.midY,
                                    width: 0, height: 0)
            pop.permittedArrowDirections = []
        }
        // Находим верхний presented, чтобы не конфликтовать с контекстным меню
        var top: UIViewController = root
        while let presented = top.presentedViewController { top = presented }
        top.present(av, animated: true)
    }
}

// MARK: - RoutePreviewSheet

struct RoutePreviewSheet: View {
    let route: DomainRoute
    let onCopy: () -> Void
    let onDelete: () -> Void

    private var startMs: Int64 { route.basicData.timeStartWork?.int64Value ?? 0 }
    private var endMs:   Int64 { route.basicData.timeEndWork?.int64Value ?? 0 }
    private var breakMs: Int64 {
        let bs = route.basicData.timeStartBreak?.int64Value ?? 0
        let be = route.basicData.timeEndBreak?.int64Value ?? 0
        return be > bs ? be - bs : 0
    }
    private var durationMs: Int64 {
        guard endMs > startMs else { return 0 }
        return endMs - startMs - breakMs
    }
    private var trains:    [DomainTrain]      { route.trains as! [DomainTrain] }
    private var locos:     [DomainLocomotive] { route.locomotives as! [DomainLocomotive] }
    private var passengers:[DomainPassenger]  { route.passengers as! [DomainPassenger] }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Период работы
                    infoBlock(title: "Время работы") {
                        if startMs > 0 {
                            row("Явка", TimeFormatter.formatDateTime(ms: startMs))
                        }
                        if endMs > startMs {
                            row("Окончание", TimeFormatter.formatDateTime(ms: endMs))
                        }
                        if breakMs > 0 {
                            row("Перерыв", TimeFormatter.formatDuration(ms: breakMs))
                        }
                        if durationMs > 0 {
                            row("Продолжительность", TimeFormatter.formatDuration(ms: durationMs))
                        }
                    }

                    // Локомотивы
                    if !locos.isEmpty {
                        infoBlock(title: "Локомотивы") {
                            ForEach(locos, id: \.locoId) { loco in
                                let text = [loco.series, loco.number]
                                    .compactMap { $0 }
                                    .filter { !$0.isEmpty }
                                    .joined(separator: " №")
                                if !text.isEmpty {
                                    row(nil, text)
                                }
                            }
                        }
                    }

                    // Поезда
                    if !trains.isEmpty {
                        infoBlock(title: "Поезда") {
                            ForEach(trains, id: \.trainId) { train in
                                let stations = train.stations as! [DomainStation]
                                let from = stations.first?.stationName ?? ""
                                let to = stations.count > 1 ? stations.last?.stationName ?? "" : ""
                                let head = (train.number?.isEmpty == false) ? "№\(train.number!)" : "—"
                                row(head, to.isEmpty ? from : "\(from) – \(to)")
                            }
                        }
                    }

                    // Пассажиром
                    if !passengers.isEmpty {
                        infoBlock(title: "Пассажиром") {
                            ForEach(passengers, id: \.passengerId) { p in
                                let head = (p.trainNumber?.isEmpty == false) ? "№\(p.trainNumber!)" : "—"
                                let route = [p.stationDeparture, p.stationArrival]
                                    .compactMap { $0 }.filter { !$0.isEmpty }
                                    .joined(separator: " – ")
                                row(head, route)
                            }
                        }
                    }

                    // Заметки
                    if let notes = route.basicData.notes, !notes.isEmpty {
                        infoBlock(title: "Заметки") {
                            Text(notes)
                                .font(.system(size: 14))
                                .foregroundStyle(Color.appPrimary)
                        }
                    }

                    // Действия
                    VStack(spacing: 8) {
                        NavigationLink(destination: FormView(routeId: route.basicData.id)) {
                            actionButton("Открыть маршрут", "arrow.up.right.square", Color.appAccent)
                        }
                        Button(action: onCopy) {
                            actionButton("Скопировать", "doc.on.doc", Color.appSecondary)
                        }
                        Button(role: .destructive, action: onDelete) {
                            actionButton("Удалить", "trash", Color.appDanger)
                        }
                    }
                    .padding(.top, 4)
                }
                .padding(16)
            }
            .background(Color.appBg)
            .navigationTitle("Просмотр маршрута")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    @ViewBuilder
    private func infoBlock<C: View>(title: String, @ViewBuilder content: () -> C) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color.appSecondary)
            VStack(alignment: .leading, spacing: 6) {
                content()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(Color.appCard)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    @ViewBuilder
    private func row(_ label: String?, _ value: String) -> some View {
        HStack {
            if let l = label {
                Text(l).font(.system(size: 13)).foregroundStyle(Color.appSecondary)
                Spacer()
            }
            Text(value)
                .font(.system(size: 14, weight: label == nil ? .regular : .medium))
                .foregroundStyle(Color.appPrimary)
                .lineLimit(2)
        }
    }

    @ViewBuilder
    private func actionButton(_ title: String, _ icon: String, _ color: Color) -> some View {
        HStack {
            Image(systemName: icon)
            Text(title).font(.system(size: 15, weight: .medium))
            Spacer()
        }
        .foregroundStyle(color)
        .padding(.vertical, 12)
        .padding(.horizontal, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appCard)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - ToolsSection

struct ToolsSection: View {
    @State private var showPdfNotice = false

    private let tileSize: CGFloat = 110

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("Инструменты")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.primary)
                Spacer()
            }
            .padding(.bottom, 12)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    // Иконки — те же monochrome ассеты, что на Android
                    // (features/route/src/main/res/drawable/ic_card_*).
                    // Рендерятся как template и тинтуются через foregroundStyle,
                    // повторяя поведение Android `Icon(tint = colorScheme.primary)`.

                    // Единый синий тинт для всех 4-х карточек — оранжевый
                    // у «Отвлечений» перекликался с оранжевым часов переработки
                    // в MainInfo и сбивал с толку.
                    let tint = Color.appAccent

                    // График
                    toolTile(title: "График",
                             asset: "ic_card_calendar",
                             color: tint,
                             action: nil)

                    // Отвлечения
                    toolTile(title: "Отвлечения",
                             asset: "ic_card_vacation",
                             color: tint,
                             action: nil)

                    // Поиск
                    NavigationLink(destination: SearchView()) {
                        toolTile(title: "Поиск",
                                 asset: "ic_card_search",
                                 color: tint,
                                 action: nil)
                    }
                    .buttonStyle(.plain)

                    // PDF
                    Button {
                        showPdfNotice = true
                    } label: {
                        toolTile(title: "PDF",
                                 asset: "ic_card_pdf",
                                 color: tint,
                                 action: nil)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .alert("PDF-отчёт", isPresented: $showPdfNotice) {
            Button("ОК", role: .cancel) {}
        } message: {
            Text("Формирование PDF будет доступно в следующем обновлении.")
        }
    }

    @ViewBuilder
    private func toolTile(
        title: String,
        asset: String,
        color: Color,
        action: (() -> Void)?
    ) -> some View {
        let content = VStack(spacing: 0) {
            Image(asset)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 52, height: 52)
                .foregroundStyle(color)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            HStack {
                Text(title)
                    .font(.system(size: 11))
                    .foregroundStyle(Color.appSecondary)
                    .lineLimit(1)
                Spacer()
            }
        }
        .padding(10)
        .frame(width: tileSize, height: tileSize)
        .background(Color.appCard)
        .clipShape(RoundedRectangle(cornerRadius: 14))

        if let action {
            Button(action: action) { content }
                .buttonStyle(.plain)
        } else {
            content
        }
    }
}

// MARK: - RouteItemView (унифицированный порт Android ItemHomeScreen).
// Используется на HomeScreen (isExpanded: false) и на AllRoutesView
// (isExpanded: false/true по переключателю).

struct RouteItemView: View {
    let route: DomainRoute
    var number: Int? = nil
    var isExpanded: Bool = false
    /// Нужно для определения попадания маршрута в праздничный день
    /// (settings.selectMonthOfYear.days с tag = HOLIDAY).
    var settings: DomainUserSettings? = nil

    /// Показ диалога с пояснением значений иконок (по тапу на ряд иконок).
    @State private var showIconsLegend: Bool = false

    // MARK: - Derived values

    private var startMs: Int64 { route.basicData.timeStartWork?.int64Value ?? 0 }
    private var endMs:   Int64 { route.basicData.timeEndWork?.int64Value ?? 0 }
    private var breakMs: Int64 {
        let bs = route.basicData.timeStartBreak?.int64Value ?? 0
        let be = route.basicData.timeEndBreak?.int64Value ?? 0
        return be > bs ? be - bs : 0
    }
    /// Рабочее время = end - start - перерыв (как в Android Route.getWorkTime()).
    private var workTimeMs: Int64 {
        guard endMs > startMs else { return 0 }
        return endMs - startMs - breakMs
    }
    private var trains:     [DomainTrain]      { route.trains as! [DomainTrain] }
    private var locos:      [DomainLocomotive] { route.locomotives as! [DomainLocomotive] }
    private var passengers: [DomainPassenger]  { route.passengers as! [DomainPassenger] }

    /// Суммарное время "следования пассажиром".
    private var passengerMs: Int64 {
        passengers.reduce(0) { acc, p in
            let d = p.timeDeparture?.int64Value ?? 0
            let a = p.timeArrival?.int64Value ?? 0
            return a > d ? acc + (a - d) : acc
        }
    }

    /// "dd.MM HH:mm - dd.MM HH:mm" или "dd.MM HH:mm - HH:mm" если один день.
    private var timeRangeText: String {
        guard startMs > 0 else { return "—" }
        let startStr = TimeFormatter.formatDateTime(ms: startMs)
        guard endMs > startMs else { return "\(startStr) —" }
        let sameDay = Calendar.current.isDate(
            TimeFormatter.msToDate(startMs),
            inSameDayAs: TimeFormatter.msToDate(endMs)
        )
        let endStr = sameDay ? formatTimeOnly(ms: endMs) : TimeFormatter.formatDateTime(ms: endMs)
        return "\(startStr) - \(endStr)"
    }

    private func formatTimeOnly(ms: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(ms) / 1000)
        let f = DateFormatter(); f.dateFormat = "HH:mm"; return f.string(from: date)
    }

    // MARK: - Body

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Ряд 1: период работы + общее рабочее время.
            HStack(alignment: .center) {
                Text(timeRangeText)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                    .layoutPriority(1)
                Spacer()
                if workTimeMs > 0 {
                    Text(TimeFormatter.formatDuration(ms: workTimeMs))
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(.primary)
                }
            }

            // Контент маршрута.
            if isExpanded {
                expandedContent
            } else {
                compactContent
            }

            // Ряд N: номер слева, статус-иконки справа (как было изначально).
            HStack {
                if let n = number {
                    Text("#\(n)")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.appSecondary)
                }
                Spacer()
                statusIcons
                    .contentShape(Rectangle())
                    .onTapGesture { showIconsLegend = true }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .sheet(isPresented: $showIconsLegend) {
            RouteIconsLegendSheet()
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        }
    }

    // MARK: - Content blocks

    @ViewBuilder
    private var compactContent: some View {
        if let train = trains.first {
            let text = trainDisplayText(train)
            if !text.isEmpty {
                Text(text)
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    @ViewBuilder
    private var expandedContent: some View {
        // Поезда, отсортированные по времени отправления первой станции.
        let sortedTrains = trains.sorted {
            let t0 = (($0.stations as! [DomainStation]).first?.timeDeparture?.int64Value) ?? 0
            let t1 = (($1.stations as! [DomainStation]).first?.timeDeparture?.int64Value) ?? 0
            return t0 < t1
        }
        if !sortedTrains.isEmpty {
            VStack(alignment: .leading, spacing: 2) {
                ForEach(sortedTrains, id: \.trainId) { train in
                    let t = trainDisplayText(train)
                    if !t.isEmpty {
                        Text(t)
                            .font(.system(size: 13))
                            .foregroundStyle(.primary)
                            .lineLimit(1)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }

        // Локомотивы: "серия №номер".
        if !locos.isEmpty {
            VStack(alignment: .leading, spacing: 2) {
                ForEach(locos, id: \.locoId) { loco in
                    let s = (loco.series ?? "").trimmingCharacters(in: .whitespaces)
                    let n = (loco.number ?? "").trimmingCharacters(in: .whitespaces)
                    let text: String = {
                        if s.isEmpty && n.isEmpty { return "" }
                        if n.isEmpty { return s }
                        if s.isEmpty { return "№\(n)" }
                        return "\(s) №\(n)"
                    }()
                    if !text.isEmpty {
                        Text(text)
                            .font(.system(size: 13))
                            .foregroundStyle(.primary)
                            .lineLimit(1)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }

        // Заметки.
        if let notes = route.basicData.notes, !notes.isEmpty {
            Text(notes)
                .font(.system(size: 13))
                .foregroundStyle(.primary)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 2)
        }
    }

    private func trainDisplayText(_ train: DomainTrain) -> String {
        let num = (train.number?.isEmpty == false) ? "№\(train.number!)" : ""
        let stations = train.stations as! [DomainStation]
        let firstName = (stations.first?.stationName ?? "").trimmingCharacters(in: .whitespaces)
        let lastName = stations.count > 1
            ? (stations.last?.stationName ?? "").trimmingCharacters(in: .whitespaces)
            : ""
        let route: String = {
            if firstName.isEmpty && lastName.isEmpty { return "" }
            if lastName.isEmpty { return firstName }
            if firstName.isEmpty { return lastName }
            return "\(firstName) - \(lastName)"
        }()
        return [num, route].filter { !$0.isEmpty }.joined(separator: " ")
    }

    // MARK: - Status icons (порядок как в Android ItemHomeScreen)

    private var statusIcons: some View {
        // Оранжевый как в Android 0xFFf1642e (избранное/>12h/праздник).
        let orange = Color(red: 0.945, green: 0.392, blue: 0.18)
        return HStack(spacing: 6) {
            if isHolidayTimeInRoute {
                Image(systemName: "sun.max.fill")
                    .font(.system(size: 12)).foregroundColor(orange)
            }
            if breakMs > 0 {
                Image(systemName: "pause.fill")
                    .font(.system(size: 12)).foregroundColor(Color.appSecondary)
            }
            if isLongCompositionTrain {
                // Повышенная длина (по осности: пасс. ≥80, груз. ≥350).
                Image(systemName: "ruler")
                    .font(.system(size: 12)).foregroundColor(Color.appSecondary)
            }
            if isHeavyTrain {
                // Повышенная масса — используем train.isHeavyLongDistance.
                Image(systemName: "scalemass")
                    .font(.system(size: 12)).foregroundColor(Color.appSecondary)
            }
            if route.basicData.isOnePersonOperation {
                Image(systemName: "person.fill")
                    .font(.system(size: 12)).foregroundColor(Color.appSecondary)
            }
            if passengerMs > 0 {
                Image(systemName: "figure.seated.side")
                    .font(.system(size: 12)).foregroundColor(Color.appSecondary)
            }
            // Работа свыше 12 часов — медаль (orden).
            if workTimeMs > 12 * 3_600_000 {
                Image(systemName: "medal.fill")
                    .font(.system(size: 12)).foregroundColor(orange)
            }
            if trains.contains(where: { $0.pusher != nil }) {
                Image(systemName: "arrow.right.to.line")
                    .font(.system(size: 12)).foregroundColor(Color.appSecondary)
            }
            if trains.contains(where: { $0.doubleTraction != nil }) {
                Image(systemName: "chevron.right.2")
                    .font(.system(size: 12)).foregroundColor(Color.appSecondary)
            }
            if trains.contains(where: { $0.doubledTrain != nil }) {
                Image(systemName: "rectangle.split.2x1")
                    .font(.system(size: 12)).foregroundColor(Color.appSecondary)
            }
            if route.basicData.isFavorite {
                Image(systemName: "heart.fill")
                    .font(.system(size: 12)).foregroundColor(orange)
            }
            if route.basicData.isSynchronized {
                Image(systemName: "checkmark.icloud.fill")
                    .font(.system(size: 12)).foregroundColor(.green)
            } else {
                Image(systemName: "xmark.icloud")
                    .font(.system(size: 12)).foregroundColor(Color.appSecondary)
            }
        }
    }

    // MARK: - Иконочная логика (порт domain/UtilsForEntities.kt)

    /// Длинносоставный поезд: пасс. (1..150, 151..298, 301..450, 451..598,
    /// 601..698, 701..750, 751..788, 801..898) — осей ≥ 80; груз. — ≥ 350.
    private var isLongCompositionTrain: Bool {
        let passengerRanges: [ClosedRange<Int>] = [
            1...150, 151...298, 301...450, 451...598,
            601...698, 701...750, 751...788, 801...898,
        ]
        return trains.contains { train in
            guard let axle = train.axle.flatMap(Int.init) else { return false }
            let num = train.number.flatMap(Int.init)
            let isPassenger = num.map { n in passengerRanges.contains { $0.contains(n) } } ?? false
            return isPassenger ? axle >= 80 : axle >= 350
        }
    }

    /// Повышенная масса — пользовательский флаг `Train.isHeavyLongDistance`
    /// (в Android эта логика берёт SalarySetting.surchargeHeavyTrainsList,
    /// но эти настройки ещё не выведены в iOS — используем явный флаг).
    private var isHeavyTrain: Bool {
        trains.contains { $0.isHeavyLongDistance }
    }

    /// Маршрут попал в праздничный день, если диапазон [timeStartWork;
    /// timeEndWork] пересекается с каким-либо днём `TagForDay.HOLIDAY` в
    /// `settings.selectMonthOfYear`.
    private var isHolidayTimeInRoute: Bool {
        guard let settings = settings else { return false }
        let moy = settings.selectMonthOfYear
        let days = moy.days
        guard !days.isEmpty, startMs > 0 else { return false }
        let e = endMs > startMs ? endMs : startMs
        // GMT+3 — как в Android (startOfDay в offsetInMoscow).
        let tz = TimeZone(identifier: "Europe/Moscow") ?? TimeZone(secondsFromGMT: 3 * 3600)!
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = tz
        for day in days {
            guard day.tag.name == "HOLIDAY" else { continue }
            let dayNum = Int(day.dayOfMonth)
            guard let dayStart = cal.date(from: DateComponents(
                year: Int(moy.year),
                month: Int(moy.month) + 1,
                day: dayNum
            )) else { continue }
            guard let dayEnd = cal.date(byAdding: .day, value: 1, to: dayStart) else { continue }
            let s = Int64(dayStart.timeIntervalSince1970 * 1000)
            let eDay = Int64(dayEnd.timeIntervalSince1970 * 1000)
            let overlap = min(e, eDay) - max(startMs, s)
            if overlap > 0 { return true }
        }
        return false
    }
}

// MARK: - RouteIconsLegendSheet

/// Диалог с пояснением значений статус-иконок маршрута (аналог Android-тултипа).
struct RouteIconsLegendSheet: View {
    @Environment(\.dismiss) private var dismiss

    private let orange = Color(red: 0.945, green: 0.392, blue: 0.18)

    var body: some View {
        NavigationStack {
            List {
                row("sun.max.fill",            orange,             "Работа в праздничный день")
                row("pause.fill",              Color.appSecondary, "Перерыв в работе")
                row("ruler",                   Color.appSecondary, "Поезд повышенной длины")
                row("scalemass",               Color.appSecondary, "Поезд повышенной массы")
                row("person.fill",             Color.appSecondary, "Работа в одно лицо")
                row("figure.seated.side",      Color.appSecondary, "Следование пассажиром")
                row("medal.fill",              orange,             "Работа свыше 12 часов")
                row("arrow.right.to.line",     Color.appSecondary, "Толкач (локомотив-помощник сзади поезда)")
                row("chevron.right.2",         Color.appSecondary, "Двойная тяга (два локомотива в голове)")
                row("rectangle.split.2x1",     Color.appSecondary, "Сдвоенный поезд")
                row("heart.fill",              orange,             "В избранном")
                row("checkmark.icloud.fill",   .green,             "Маршрут синхронизирован с облаком")
                row("xmark.icloud",            Color.appSecondary, "Маршрут не синхронизирован")
            }
            .listStyle(.plain)
            .navigationTitle("Значения иконок")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Понятно") { dismiss() }
                }
            }
        }
    }

    @ViewBuilder
    private func row(_ icon: String, _ color: Color, _ text: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(color)
                .frame(width: 24, alignment: .center)
            Text(text)
                .font(.system(size: 14))
                .foregroundColor(Color.appPrimary)
            Spacer()
        }
    }
}

// MARK: - MonthCarousel

struct MonthCarousel: View {
    let selectedMonth: Int
    let selectedYear: Int
    let onMonthChanged: (Int, Int) -> Void
    var onCenterTap: () -> Void = {}

    @State private var scrolledId: Int?

    private let months = [
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    ]

    private let yearsRange = 3
    private var totalItems: Int { (yearsRange * 2 + 1) * 12 }
    private var centerIndex: Int { yearsRange * 12 + selectedMonth }
    private func itemId(_ index: Int) -> Int { index }
    private func monthForIndex(_ index: Int) -> Int { ((index % 12) + 12) % 12 }
    private func yearForIndex(_ index: Int) -> Int { selectedYear - yearsRange + index / 12 }

    private let cellWidth: CGFloat = 120

    @ViewBuilder
    private func monthItem(index: Int) -> some View {
        let m = monthForIndex(index)
        let y = yearForIndex(index)
        let isCurrent = (scrolledId ?? centerIndex) == index

        VStack(spacing: 1) {
            Text(months[m])
                .font(.system(size: isCurrent ? 17 : 14, weight: isCurrent ? .bold : .medium))
                .foregroundStyle(isCurrent ? .primary : .secondary)
            if isCurrent {
                Text(String(y))
                    .font(.system(size: 11, weight: .regular))
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: cellWidth, height: 44)
        .id(itemId(index))
        .onTapGesture {
            if isCurrent { onCenterTap() }
            else {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                withAnimation(.spring(response: 0.3, dampingFraction: 0.85)) {
                    scrolledId = index
                }
                onMonthChanged(m, y)
            }
        }
    }

    private func advance(by delta: Int) {
        var newMonth = selectedMonth + delta
        var newYear = selectedYear
        while newMonth > 11 { newMonth -= 12; newYear += 1 }
        while newMonth < 0 { newMonth += 12; newYear -= 1 }
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        onMonthChanged(newMonth, newYear)
    }

    @ViewBuilder
    private var fallbackCarousel: some View {
        HStack(spacing: 0) {
            ForEach(-2...2, id: \.self) { delta in
                let (m, y) = (monthForIndex(centerIndex + delta), yearForIndex(centerIndex + delta))
                let isCurrent = delta == 0
                VStack(spacing: 1) {
                    Text(months[m])
                        .font(.system(size: isCurrent ? 17 : 14, weight: isCurrent ? .bold : .medium))
                        .foregroundStyle(isCurrent ? .primary : .secondary)
                    if isCurrent {
                        Text(String(y)).font(.system(size: 11)).foregroundStyle(.secondary)
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .onTapGesture {
                    if isCurrent { onCenterTap() }
                    else { advance(by: delta) }
                }
            }
        }
        .gesture(
            DragGesture(minimumDistance: 20)
                .onEnded { v in
                    if v.translation.width < -30 { advance(by: 1) }
                    else if v.translation.width > 30 { advance(by: -1) }
                }
        )
    }

    var body: some View {
        GeometryReader { geo in
            let sideInset = (geo.size.width - cellWidth) / 2
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.appElevated)
                    .frame(width: cellWidth + 20, height: 44)

                if #available(iOS 17.0, *) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 0) {
                            ForEach(0..<totalItems, id: \.self) { index in
                                monthItem(index: index)
                            }
                        }
                        .scrollTargetLayout()
                    }
                    .contentMargins(.horizontal, sideInset, for: .scrollContent)
                    .scrollTargetBehavior(.viewAligned)
                    .scrollPosition(id: $scrolledId, anchor: .center)
                    .onChange(of: scrolledId) { _, newId in
                        guard let id = newId else { return }
                        let m = monthForIndex(id)
                        let y = yearForIndex(id)
                        if m != selectedMonth || y != selectedYear {
                            UIImpactFeedbackGenerator(style: .light).impactOccurred()
                            onMonthChanged(m, y)
                        }
                    }
                    .onAppear { scrolledId = centerIndex }
                    .onChange(of: selectedMonth) { _, _ in
                        let target = yearsRange * 12 + selectedMonth
                        if scrolledId != target { scrolledId = target }
                    }
                } else {
                    fallbackCarousel
                }
            }
            .mask(
                HStack(spacing: 0) {
                    LinearGradient(colors: [.clear, .black], startPoint: .leading, endPoint: .trailing)
                        .frame(width: 40)
                    Color.black
                    LinearGradient(colors: [.black, .clear], startPoint: .leading, endPoint: .trailing)
                        .frame(width: 40)
                }
            )
        }
        .frame(height: 52)
    }
}

// MARK: - MonthPickerSheet

struct MonthPickerSheet: View {
    let selectedMonth: Int
    let selectedYear: Int
    let onApply: (Int, Int) -> Void

    @Environment(\.dismiss) var dismiss
    @State private var tempMonth: Int
    @State private var tempYear: Int

    private let shortMonths = [
        "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
        "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"
    ]
    private let nowMonth = Calendar.current.component(.month, from: Date()) - 1
    private let nowYear = Calendar.current.component(.year, from: Date())

    init(selectedMonth: Int, selectedYear: Int, onApply: @escaping (Int, Int) -> Void) {
        self.selectedMonth = selectedMonth
        self.selectedYear = selectedYear
        self.onApply = onApply
        _tempMonth = State(initialValue: selectedMonth)
        _tempYear = State(initialValue: selectedYear)
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button { tempYear -= 1 } label: {
                    Image(systemName: "chevron.left").font(.system(size: 16, weight: .semibold))
                }
                Spacer()
                Text(String(tempYear)).font(.system(size: 17, weight: .semibold))
                Spacer()
                Button { tempYear += 1 } label: {
                    Image(systemName: "chevron.right").font(.system(size: 16, weight: .semibold))
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
            .background(Color.appElevated)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .padding(.horizontal, 20)
            .padding(.top, 8)

            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 10) {
                ForEach(0..<12, id: \.self) { month in
                    let isSelected = month == tempMonth
                    let isCurrent = month == nowMonth && tempYear == nowYear && !isSelected

                    Button(shortMonths[month]) { tempMonth = month }
                    .font(.system(size: 14, weight: isSelected ? .semibold : .regular))
                    .foregroundStyle(isSelected ? Color.white : isCurrent ? Color.appAccent : Color.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 11)
                    .background(isSelected ? Color.appAccent : Color.appElevated)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(isCurrent ? Color.appAccent : Color.clear, lineWidth: 1.5)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 14)

            Spacer()

            Button {
                onApply(tempMonth, tempYear)
                dismiss()
            } label: {
                Text("Применить")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.appAccent)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
    }
}

// MARK: - MinuteSyncTimer

/// Таймер, синхронизированный с системными часами.
/// Первый тик — ровно на границе следующей минуты (секунды == 0),
/// далее каждые 60 секунд.
final class MinuteSyncTimer {
    static let shared = MinuteSyncTimer()

    let tick = PassthroughSubject<Date, Never>()

    private var timer: Timer?

    private init() {
        let seconds = Calendar.current.component(.second, from: Date())
        let delay = Double(60 - seconds)

        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            self?.tick.send(Date())
            self?.timer = Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { [weak self] _ in
                self?.tick.send(Date())
            }
        }
    }
}

// MARK: - Stats page height measurement

/// Агрегирует высоты страниц пейджера по индексу.
/// reduce объединяет словари со всех страниц, тогда `onPreferenceChange`
/// получает полный `[Int: CGFloat]` разом.
private struct StatsPageHeightPreference: PreferenceKey {
    static var defaultValue: [Int: CGFloat] = [:]
    static func reduce(value: inout [Int: CGFloat], nextValue: () -> [Int: CGFloat]) {
        value.merge(nextValue()) { _, new in new }
    }
}

private extension View {
    /// Измеряет высоту страницы пейджера и пишет её в preference по индексу.
    func measurePageHeight(index: Int) -> some View {
        background(
            GeometryReader { geo in
                Color.clear.preference(
                    key: StatsPageHeightPreference.self,
                    value: [index: geo.size.height]
                )
            }
        )
    }
}

#Preview { NavigationStack { HomeView() } }
