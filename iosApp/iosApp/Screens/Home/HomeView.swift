import SwiftUI
import ComposeApp

// MARK: - HomeView

struct HomeView: View {
    @StateObject private var vm = HomeViewModelWrapper()
    @State private var statsPage = 0
    @State private var showDeleteConfirm = false
    @State private var routeToDelete: String? = nil

    private let monthNames = [
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    ]
    private let pageCount = 3

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                monthNavigationHeader
                    .padding(.horizontal)
                    .padding(.top, 8)
                    .padding(.bottom, 4)

                // Свайпаемые блоки статистики
                statsCardPager
                    .padding(.horizontal)
                    .padding(.top, 4)

                // Точки-индикатор СНАРУЖИ карточки
                pageIndicator
                    .padding(.top, 8)
                    .padding(.bottom, 12)

                routesSection

                toolsSection
                    .padding(.bottom, 16)
            }
        }
        .navigationTitle(currentMonthTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Image(systemName: "magnifyingglass").foregroundColor(.accentColor)
            }
        }
        .alert("Удалить маршрут?", isPresented: $showDeleteConfirm) {
            Button("Удалить", role: .destructive) {
                if let id = routeToDelete { vm.deleteRoute(routeId: id); routeToDelete = nil }
            }
            Button("Отмена", role: .cancel) { routeToDelete = nil }
        } message: { Text("Это действие нельзя отменить.") }
    }

    // MARK: - Month

    private var currentMonthTitle: String {
        let idx = vm.currentMonth
        return "\((idx >= 0 && idx < monthNames.count) ? monthNames[idx] : "?") \(vm.currentYear)"
    }

    private var monthNavigationHeader: some View {
        HStack {
            Button { prevMonth() } label: {
                Image(systemName: "chevron.left").font(.title3).foregroundColor(.accentColor)
            }.buttonStyle(.plain)
            Spacer()
            Text(currentMonthTitle).font(.headline)
            Spacer()
            Button { nextMonth() } label: {
                Image(systemName: "chevron.right").font(.title3).foregroundColor(.accentColor)
            }.buttonStyle(.plain)
        }.padding(.vertical, 4)
    }

    private func prevMonth() {
        var m = vm.currentMonth, y = vm.currentYear
        if m == 0 { m = 11; y -= 1 } else { m -= 1 }
        vm.setCurrentMonth(month: m, year: y)
    }
    private func nextMonth() {
        var m = vm.currentMonth, y = vm.currentYear
        if m == 11 { m = 0; y += 1 } else { m += 1 }
        vm.setCurrentMonth(month: m, year: y)
    }

    // MARK: - Stats pager (карточки без встроенного индикатора)

    private var statsCardPager: some View {
        TabView(selection: $statsPage) {
            pageMainInfo.tag(0)
            pageDetailWorkTime.tag(1)
            pageDetailTrain.tag(2)
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .frame(height: statsCardHeight)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(12)
    }

    /// Высота карточки зависит от количества строк на самой высокой странице.
    private var statsCardHeight: CGFloat {
        // Страница 3 самая высокая: заголовок + пробел + 4 прогресс-ряда
        let rowH: CGFloat = 38   // label + bar + gap
        let headerH: CGFloat = 42
        let paddingV: CGFloat = 32
        let page3Rows: CGFloat = 4
        return headerH + 12 + page3Rows * rowH + paddingV
    }

    // Индикатор страниц ВНЕ карточки
    private var pageIndicator: some View {
        HStack(spacing: 8) {
            ForEach(0..<pageCount, id: \.self) { i in
                Circle()
                    .fill(i == statsPage ? Color.primary : Color.secondary.opacity(0.35))
                    .frame(width: 7, height: 7)
                    .animation(.easeInOut(duration: 0.2), value: statsPage)
            }
        }
    }

    // MARK: - Calculations

    private var isDecimal: Bool       { vm.settings?.isDecimalTime ?? false }
    private var isConsiderFuture: Bool { vm.settings?.isConsiderFutureRoute ?? true }

    /// Суммарное рабочее время за месяц (мс).
    private var totalWorkMs: Int64 {
        vm.routes.reduce(Int64(0)) { acc, r in
            routeWorkMs(r).map { acc + $0 } ?? acc
        }
    }

    /// Рабочее время сегодня.
    private var todayWorkMs: Int64 {
        let cal = Calendar.current
        let s0 = Int64(cal.startOfDay(for: Date()).timeIntervalSince1970 * 1000)
        let e0 = s0 + 86_400_000
        return vm.routes.reduce(Int64(0)) { acc, r in
            let s = r.basicData.timeStartWork?.int64Value ?? 0
            guard s >= s0 && s < e0 else { return acc }
            return routeWorkMs(r).map { acc + $0 } ?? acc
        }
    }

    /// Ночное время за месяц.
    private var nightWorkMs: Int64 {
        guard let st = vm.settings else { return 0 }
        let nh = Int(st.nightTime.startNightHour), nm = Int(st.nightTime.startNightMinute)
        let eh = Int(st.nightTime.endNightHour),   em = Int(st.nightTime.endNightMinute)
        return vm.routes.reduce(Int64(0)) { acc, r in
            let s = r.basicData.timeStartWork?.int64Value ?? 0
            let e = r.basicData.timeEndWork?.int64Value ?? 0
            guard e > s else { return acc }
            return acc + calcNightMs(s, e, nh, nm, eh, em)
        }
    }

    /// Время пассажиром (сумма интервалов из списка Passenger каждого маршрута).
    private var passengerWorkMs: Int64 {
        vm.routes.reduce(Int64(0)) { acc, r in
            let pMs = (r.passengers as! [DomainPassenger]).reduce(Int64(0)) { pAcc, p in
                let dep = p.timeDeparture?.int64Value ?? 0
                let arr = p.timeArrival?.int64Value ?? 0
                return arr > dep ? pAcc + (arr - dep) : pAcc
            }
            return acc + pMs
        }
    }

    /// Время резервом (маршруты без поездов).
    private var reserveWorkMs: Int64 {
        vm.routes.reduce(Int64(0)) { acc, r in
            guard (r.trains as! [DomainTrain]).isEmpty else { return acc }
            return routeWorkMs(r).map { acc + $0 } ?? acc
        }
    }

    /// Удлинённые плечи обслуживания (маршруты с заданной сервисной фазой поезда).
    private var extServicePhaseMs: Int64 {
        vm.routes.reduce(Int64(0)) { acc, r in
            guard (r.trains as! [DomainTrain]).contains(where: { $0.servicePhase != nil }) else { return acc }
            return routeWorkMs(r).map { acc + $0 } ?? acc
        }
    }

    /// Длинносоставные (isHeavyLongDistance на поезде).
    private var longCompositionMs: Int64 {
        vm.routes.reduce(Int64(0)) { acc, r in
            guard (r.trains as! [DomainTrain]).contains(where: { $0.isHeavyLongDistance }) else { return acc }
            return routeWorkMs(r).map { acc + $0 } ?? acc
        }
    }

    /// Тяжёлые (поезда с указанной массой).
    private var heavyTrainMs: Int64 {
        vm.routes.reduce(Int64(0)) { acc, r in
            guard (r.trains as! [DomainTrain]).contains(where: {
                let w = $0.weight; return w != nil && !(w!.isEmpty)
            }) else { return acc }
            return routeWorkMs(r).map { acc + $0 } ?? acc
        }
    }

    /// Одно лицо (isOnePersonOperation).
    private var onePersonMs: Int64 {
        vm.routes.reduce(Int64(0)) { acc, r in
            guard r.basicData.isOnePersonOperation else { return acc }
            return routeWorkMs(r).map { acc + $0 } ?? acc
        }
    }

    private func routeWorkMs(_ r: DomainRoute) -> Int64? {
        let s = r.basicData.timeStartWork?.int64Value ?? 0
        let e = r.basicData.timeEndWork?.int64Value ?? 0
        guard e > s else { return nil }
        let bs = r.basicData.timeStartBreak?.int64Value ?? 0
        let be = r.basicData.timeEndBreak?.int64Value ?? 0
        return e - s - (be > bs ? be - bs : 0)
    }

    // Норма
    private var normaHoursMonth: Int {
        guard let moy = vm.settings?.selectMonthOfYear else { return 165 }
        let t = (moy.days as! [DomainDay]).filter { !$0.isReleaseDay }.reduce(0) { acc, d in
            acc + (d.tag == DomainTagForDay.workingDay ? 8 : d.tag == DomainTagForDay.shortenedDay ? 7 : 0)
        }
        return t > 0 ? t : 165
    }

    private var normaHoursToday: Int {
        guard let moy = vm.settings?.selectMonthOfYear else { return 0 }
        let cal = Calendar.current
        let td = cal.component(.day, from: Date())
        let tm = cal.component(.month, from: Date()) - 1
        guard tm == Int(moy.month) else { return 0 }
        return (moy.days as! [DomainDay]).filter { !$0.isReleaseDay && Int($0.dayOfMonth) <= td }.reduce(0) { acc, d in
            acc + (d.tag == DomainTagForDay.workingDay ? 8 : d.tag == DomainTagForDay.shortenedDay ? 7 : 0)
        }
    }

    private func pct(_ ms: Int64, _ total: Int64) -> Double {
        guard total > 0 else { return 0 }
        return min(Double(ms) / Double(total), 1.0)
    }
    private func pctH(_ ms: Int64, _ hours: Int) -> Double {
        pct(ms, Int64(hours) * 3_600_000)
    }
    private func todayStr() -> String {
        let f = DateFormatter(); f.dateFormat = "dd.MM"; return f.string(from: Date())
    }

    // MARK: - Page 1: MainInfo (как в Android)

    private var pageMainInfo: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(TimeFormatter.formatWorkDuration(ms: totalWorkMs, isDecimal: isDecimal))
                .font(.title2).fontWeight(.semibold)
                .foregroundColor(.secondary)

            Spacer().frame(height: 18)

            statRow("Норма на месяц",         "\(normaHoursMonth) ч.",  pctH(totalWorkMs, normaHoursMonth))
            Spacer().frame(height: 7)
            statRow("Норма на \(todayStr())",  "\(normaHoursToday) ч.", pctH(totalWorkMs, normaHoursToday))

            if isConsiderFuture {
                Spacer().frame(height: 7)
                statRow("Отработано на \(todayStr())",
                         TimeFormatter.formatWorkDuration(ms: todayWorkMs, isDecimal: isDecimal),
                         pctH(todayWorkMs, normaHoursToday))
            }
            Spacer()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }

    // MARK: - Page 2: DetailWorkTimeCard (как в Android)

    private var pageDetailWorkTime: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(TimeFormatter.formatWorkDuration(ms: totalWorkMs, isDecimal: isDecimal))
                .font(.title2).fontWeight(.semibold)
                .foregroundColor(.secondary)

            Spacer().frame(height: 18)

            // Ночные
            if nightWorkMs > 0 {
                statRow("Ночные",
                         TimeFormatter.formatWorkDuration(ms: nightWorkMs, isDecimal: isDecimal),
                         pct(nightWorkMs, max(totalWorkMs, 1)))
                Spacer().frame(height: 7)
            }
            // Пассажиром
            if passengerWorkMs > 0 {
                statRow("Пассажиром",
                         TimeFormatter.formatWorkDuration(ms: passengerWorkMs, isDecimal: isDecimal),
                         pct(passengerWorkMs, max(totalWorkMs, 1)))
                Spacer().frame(height: 7)
            }
            // Резервом
            if reserveWorkMs > 0 {
                statRow("Резервом",
                         TimeFormatter.formatWorkDuration(ms: reserveWorkMs, isDecimal: isDecimal),
                         pct(reserveWorkMs, max(totalWorkMs, 1)))
            }
            Spacer()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }

    // MARK: - Page 3: DetailTrainCard (как в Android)

    private var pageDetailTrain: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(TimeFormatter.formatWorkDuration(ms: totalWorkMs, isDecimal: isDecimal))
                .font(.title2).fontWeight(.semibold)
                .foregroundColor(.secondary)

            Spacer().frame(height: 18)

            // Удл. плечи обслуживания
            if extServicePhaseMs > 0 {
                statRow("Удл. плечи обслуживания",
                         TimeFormatter.formatWorkDuration(ms: extServicePhaseMs, isDecimal: isDecimal),
                         pct(extServicePhaseMs, max(totalWorkMs, 1)))
                Spacer().frame(height: 7)
            }
            // Длинносоставные
            if longCompositionMs > 0 {
                statRow("Длинносоставные",
                         TimeFormatter.formatWorkDuration(ms: longCompositionMs, isDecimal: isDecimal),
                         pct(longCompositionMs, max(totalWorkMs, 1)))
                Spacer().frame(height: 7)
            }
            // Тяжёлые
            if heavyTrainMs > 0 {
                statRow("Тяжелые",
                         TimeFormatter.formatWorkDuration(ms: heavyTrainMs, isDecimal: isDecimal),
                         pct(heavyTrainMs, max(totalWorkMs, 1)))
                Spacer().frame(height: 7)
            }
            // Одно лицо
            if onePersonMs > 0 {
                statRow("Одно лицо",
                         TimeFormatter.formatWorkDuration(ms: onePersonMs, isDecimal: isDecimal),
                         pct(onePersonMs, max(totalWorkMs, 1)))
            }
            Spacer()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }

    // Строка метка + значение + прогресс-бар (аналог Android LinearProgressIndicator)
    @ViewBuilder
    private func statRow(_ label: String, _ value: String, _ progress: Double) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                Spacer()
                Text(value)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            ProgressView(value: progress)
                .progressViewStyle(.linear)
                .tint(Color.secondary)
        }
    }

    // MARK: - Routes section

    private var routesSection: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Маршруты").font(.headline).padding(.leading)
                Spacer()
                NavigationLink(destination: AllRoutesView(vm: vm)) {
                    HStack(spacing: 4) {
                        Text("Все (\(vm.routes.count))").font(.subheadline)
                        Image(systemName: "chevron.right").font(.caption)
                    }.foregroundColor(.accentColor)
                }.padding(.trailing)
            }
            .padding(.vertical, 10)

            if vm.routes.isEmpty {
                Text("Нет маршрутов за этот месяц")
                    .font(.subheadline).foregroundColor(.secondary)
                    .frame(maxWidth: .infinity).padding(.vertical, 20)
            } else {
                let last2 = Array(vm.routes.prefix(2))
                ForEach(Array(last2.enumerated()), id: \.element.basicData.id) { idx, route in
                    if idx > 0 { Divider().padding(.leading) }
                    NavigationLink(destination: FormView(routeId: route.basicData.id)) {
                        RouteItemView(route: route)
                    }
                    .buttonStyle(.plain)
                    .contextMenu {
                        Button { vm.copyRoute(routeId: route.basicData.id) } label: {
                            Label("Скопировать", systemImage: "doc.on.doc")
                        }
                        Button(role: .destructive) {
                            routeToDelete = route.basicData.id; showDeleteConfirm = true
                        } label: { Label("Удалить", systemImage: "trash") }
                    }
                }
            }
        }
        .padding(.bottom, 8)
    }

    // MARK: - Tools section

    private var toolsSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            Divider().padding(.horizontal)
            Text("Инструменты")
                .font(.headline)
                .padding(.horizontal)
                .padding(.top, 16)
                .padding(.bottom, 10)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    toolCard("График",     "calendar",       AnyView(WorkScheduleView()))
                    toolCard("Отвлечения", "moon.zzz.fill",  AnyView(stubScreen("Отвлечения")))
                    toolCard("Поиск",      "magnifyingglass", AnyView(SearchView()))
                }
                .padding(.horizontal)
            }
        }
    }

    @ViewBuilder
    private func toolCard(_ title: String, _ icon: String, _ dest: AnyView) -> some View {
        NavigationLink(destination: dest) {
            VStack(spacing: 10) {
                Image(systemName: icon).font(.system(size: 26)).foregroundColor(.primary)
                Text(title).font(.caption).foregroundColor(.primary)
            }
            .frame(width: 88, height: 88)
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(14)
        }.buttonStyle(.plain)
    }

    private func stubScreen(_ title: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "moon.zzz.fill").font(.system(size: 48)).foregroundColor(.secondary)
            Text(title).font(.title2).fontWeight(.semibold)
            Text("Скоро").foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationTitle(title)
    }

    // MARK: - Night helpers

    private func calcNightMs(_ ws: Int64, _ we: Int64,
                              _ nsh: Int, _ nsm: Int,
                              _ neh: Int, _ nem: Int) -> Int64 {
        let day: Int64 = 86_400_000
        let nsOff = Int64(nsh * 3_600_000 + nsm * 60_000)
        let neOff = Int64(neh * 3_600_000 + nem * 60_000)
        var total: Int64 = 0, base = dayStart(ws)
        while base < we {
            let a = base + nsOff
            let b = nsOff < neOff ? base + neOff : base + day + neOff
            let os = max(ws, a); let oe = min(we, b)
            if oe > os { total += oe - os }
            base += day
        }
        return total
    }

    private func dayStart(_ ms: Int64) -> Int64 {
        let day: Int64 = 86_400_000
        let tz = Int64(TimeZone.current.secondsFromGMT()) * 1_000
        return ((ms + tz) / day) * day - tz
    }
}

// MARK: - RouteItemView

struct RouteItemView: View {
    let route: DomainRoute
    private var startMs: Int64 { route.basicData.timeStartWork?.int64Value ?? 0 }
    private var endMs:   Int64 { route.basicData.timeEndWork?.int64Value ?? 0 }
    private var durationMs: Int64 {
        guard endMs > startMs else { return 0 }
        let bs = route.basicData.timeStartBreak?.int64Value ?? 0
        let be = route.basicData.timeEndBreak?.int64Value ?? 0
        return endMs - startMs - (be > bs ? be - bs : 0)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(route.basicData.number.map { "Маршрут \($0)" } ?? "Маршрут")
                    .font(.headline)
                    .foregroundColor(route.basicData.number != nil ? .primary : .secondary)
                Spacer()
                if (route.trains as! [DomainTrain]).contains(where: { $0.isHeavyLongDistance }) {
                    Image(systemName: "arrow.left.and.right").font(.caption).foregroundColor(.orange)
                }
            }
            HStack(spacing: 16) {
                if startMs > 0 {
                    Label(TimeFormatter.formatDateTime(ms: startMs), systemImage: "clock")
                        .font(.caption).foregroundColor(.secondary)
                }
                if durationMs > 0 {
                    Label(TimeFormatter.formatDuration(ms: durationMs), systemImage: "timer")
                        .font(.caption).foregroundColor(.secondary)
                }
            }
            if let loco = (route.locomotives as! [DomainLocomotive]).first {
                let n = [loco.series, loco.number].compactMap { $0 }.joined(separator: " ")
                if !n.isEmpty {
                    Label(n, systemImage: "tram.fill").font(.caption).foregroundColor(.secondary)
                }
            }
            if let train = (route.trains as! [DomainTrain]).first, let num = train.number {
                Label("Поезд \(num)", systemImage: "car.2.fill")
                    .font(.caption).foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(UIColor.systemBackground))
    }
}

#Preview { NavigationStack { HomeView() } }
