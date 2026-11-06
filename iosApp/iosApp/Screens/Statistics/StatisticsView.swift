import SwiftUI
import ComposeApp

// Экран «Статистика» (SCREEN_SPECS §13).
// Макет: design/src/stats-screens.jsx → IOSStatsB (сетка плашек метрик, первая
//        крупная на всю ширину — closest к Android StatisticsScreen), IOSStatsYear,
//        IOSStatsEmpty, IOSStatsDetail, IOSStatsHistory.
// Логика (метрики, сравнение, детализация, история) — в Kotlin
// `StatisticsIosViewModel`; SwiftUI только рендерит `StatisticsIosUiState`.
// PDF (§13.5/§21, волна 4) — не реализован: кнопка скрыта.

struct StatisticsView: View {
    @StateObject private var vm = StatisticsViewModelWrapper()

    // Локальные шторки (как в AppRoute: экран показывает их через .dsSheet,
    // кейсы AppSheet остаются реестром).
    private enum StatSheet: Identifiable {
        case compare
        case monthPick
        case yearPick
        case freightInfo

        var id: String {
            switch self {
            case .compare: return "compare"
            case .monthPick: return "monthPick"
            case .yearPick: return "yearPick"
            case .freightInfo: return "freightInfo"
            }
        }
    }

    @State private var sheet: StatSheet?

    var body: some View {
        content
            .navigationTitle("Статистика")
            .navigationBarTitleDisplayMode(.inline)
            // Кнопка PDF (§13.5) — волна 4 (§21): скрыта, не заглушка в UI.
            .dsSheet(item: $sheet) { item in
                sheetView(item)
            }
    }

    @ViewBuilder
    private func sheetView(_ item: StatSheet) -> some View {
        switch item {
        case .compare:
            StatCompareSheet(
                isYear: vm.state?.tab == "year",
                options: vm.state?.compareOptions ?? [],
                selectedId: vm.state?.compareSelectedId ?? "prev",
                onSelect: { id in
                    sheet = nil
                    vm.setCompare(id: id)
                },
                onPick: {
                    sheet = vm.state?.tab == "year" ? .yearPick : .monthPick
                },
                onClose: { sheet = nil }
            )
        case .monthPick:
            StatMonthGridSheet(
                years: (vm.state?.pickYears ?? []).map { Int($0.int32Value) },
                availableYm: Set((vm.state?.availableMonthsYm ?? []).map { Int($0.int32Value) }),
                currentYm: Int(vm.state?.pickCurYm ?? 0),
                onPick: { ym in
                    sheet = nil
                    vm.setCompareCustomMonth(year: ym / 12, month0: ym % 12)
                },
                onBack: { sheet = .compare },
                onClose: { sheet = nil }
            )
        case .yearPick:
            StatYearListSheet(
                years: (vm.state?.pickYearOptions ?? []).map { Int($0.int32Value) },
                onPick: { year in
                    sheet = nil
                    vm.setCompareCustomYear(year: year)
                },
                onBack: { sheet = .compare },
                onClose: { sheet = nil }
            )
        case .freightInfo:
            StatFreightInfoSheet(onClose: { sheet = nil })
        }
    }

    // MARK: - Контент

    @ViewBuilder
    private var content: some View {
        if let state = vm.state, state.loading {
            loadingView
        } else if let state = vm.state {
            VStack(spacing: 0) {
                if state.tab != "history" {
                    StatPeriodHeader(state: state, vm: vm)
                } else {
                    tabSegment(state: state)
                        .padding(.horizontal, DSMetrics.screenPadX)
                        .padding(.top, DSSpacing.sm)
                }
                if state.empty {
                    emptyState(state: state)
                } else {
                    scrollBody(state: state)
                }
            }
            .background(DSColor.bg.ignoresSafeArea())
            .overlay {
                if state.detail != nil {
                    StatDetailView(state: state, vm: vm)
                }
            }
        } else {
            loadingView
        }
    }

    private var loadingView: some View {
        VStack(spacing: DSSpacing.lg) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Считаем статистику…")
                .font(DSFont.sans(15))
                .foregroundColor(DSColor.textMuted)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DSColor.bg.ignoresSafeArea())
    }

    @ViewBuilder
    private func emptyState(state: StatisticsIosUiState) -> some View {
        let isMonth = state.tab != "year"
        DSEmptyState(
            icon: .document,
            title: isMonth ? "За месяц данных нет" : "За год данных нет",
            message: "В этом периоде ещё не закрыто ни одного маршрута."
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func tabSegment(state: StatisticsIosUiState) -> some View {
        Picker("", selection: Binding(
            get: { state.tab },
            set: { vm.setTab($0) }
        )) {
            Text("Месяц").tag("month")
            Text("Год").tag("year")
            Text("История").tag("history")
        }
        .pickerStyle(.segmented)
    }

    @ViewBuilder
    private func scrollBody(state: StatisticsIosUiState) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if state.tab == "history" {
                    StatHistoryBody(state: state, vm: vm)
                } else {
                    StatCompareSelector(state: state) { sheet = .compare }
                    StatMetricsGrid(state: state, vm: vm, onFreightInfo: { sheet = .freightInfo })
                    if state.tab == "month" {
                        StatTopDirectionsSection(directions: state.topDirections)
                    } else {
                        StatYearHero(state: state)
                        StatYearBarsChart(state: state)
                        StatTopDirectionsSection(directions: state.topDirections)
                    }
                }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.bottom, 40)
        }
        .scrollDismissesKeyboard(.immediately)
        .background(DSColor.bg.ignoresSafeArea())
    }
}

// MARK: - Шапка: сегмент вкладок + навигация по периодам

private struct StatPeriodHeader: View {
    let state: StatisticsIosUiState
    @ObservedObject var vm: StatisticsViewModelWrapper

    var body: some View {
        VStack(spacing: DSSpacing.md) {
            Picker("", selection: Binding(
                get: { state.tab },
                set: { vm.setTab($0) }
            )) {
                Text("Месяц").tag("month")
                Text("Год").tag("year")
                Text("История").tag("history")
            }
            .pickerStyle(.segmented)

            HStack(spacing: DSSpacing.sm) {
                periodArrow(.chevronLeft, enabled: state.canGoPrev, label: "Предыдущий период") {
                    vm.prevPeriod()
                }
                VStack(spacing: 2) {
                    Text(state.periodLabel)
                        .font(DSFont.sans(17, .bold))
                        .foregroundColor(DSColor.text)
                    Text(state.periodSub)
                        .font(DSFont.mono(10, .semibold))
                        .tracking(1.2)
                        .foregroundColor(DSColor.textMuted)
                }
                .frame(maxWidth: .infinity)
                periodArrow(.chevronRight, enabled: state.canGoNext, label: "Следующий период") {
                    vm.nextPeriod()
                }
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.top, DSSpacing.sm)
        .padding(.bottom, DSSpacing.md)
    }

    private func periodArrow(
        _ icon: DSIcon,
        enabled: Bool,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            icon.image
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(enabled ? DSColor.accent : DSColor.textFaint)
                .frame(width: 44, height: 44)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(Text(label))
    }
}

// MARK: - Селектор сравнения (§13.3)

private struct StatCompareSelector: View {
    let state: StatisticsIosUiState
    let onOpen: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Button(action: onOpen) {
                HStack(spacing: 6) {
                    Text(state.compareEnabled ? "в сравнении с" : "режим")
                        .font(DSFont.sans(12.5))
                        .foregroundColor(DSColor.textMuted)
                    Text(state.compareEnabled ? state.compareTitle : "Без сравнения")
                        .font(DSFont.sans(12.5, .semibold))
                        .foregroundColor(DSColor.text)
                    DSIcon.chevronDown.image
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(DSColor.textMuted)
                }
                .padding(.vertical, 6)
                .padding(.horizontal, 10)
                .background(DSColor.chipBg)
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)

            if state.compareNoData {
                Text("В периоде «\(state.compareTitle)» нет маршрутов — сравнивать не с чем")
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
                    .padding(.horizontal, 4)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, DSSpacing.md)
    }
}

// MARK: - Сетка метрик (§13.2)

private struct StatMetricsGrid: View {
    let state: StatisticsIosUiState
    @ObservedObject var vm: StatisticsViewModelWrapper
    let onFreightInfo: () -> Void

    private let columns = [
        GridItem(.flexible(), spacing: DSSpacing.md),
        GridItem(.flexible(), spacing: DSSpacing.md),
    ]

    var body: some View {
        // При системном шрифте > 115% плашки идут по одной в ряд — широкая
        // первая плашка переносится на всю ширину, остальные — в 2 колонки.
        LazyVGrid(columns: columns, spacing: DSSpacing.md) {
            ForEach(Array(state.metrics.enumerated()), id: \.element.key) { index, metric in
                StatMetricCard(
                    metric: metric,
                    wide: state.wideFirst && index == 0,
                    showInfo: metric.key == "tkm",
                    compare: state.compareEnabled,
                    onInfo: onFreightInfo
                )
                .gridCellColumns(state.wideFirst && index == 0 ? 2 : 1)
                .onTapGesture {
                    vm.openDetail(key: metric.key)
                }
            }
        }
    }
}

private struct StatMetricCard: View {
    let metric: StatMetric
    let wide: Bool
    let showInfo: Bool
    let compare: Bool
    let onInfo: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 6) {
                Text(metric.label)
                    .font(DSFont.sans(12.5, .semibold))
                    .foregroundColor(DSColor.textMuted)
                    .lineLimit(2)
                Spacer(minLength: 0)
                if showInfo {
                    Button(action: onInfo) {
                        DSIcon.info.image
                            .font(.system(size: 13, weight: .regular))
                            .foregroundColor(DSColor.textFaint)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text("Что такое грузооборот"))
                }
            }
            .padding(.bottom, wide ? 10 : 12)

            HStack(alignment: .bottom, spacing: 8) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(metric.value)
                        .font(DSFont.mono(wide ? 34 : 24, .heavy))
                        .foregroundColor(DSColor.text)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    if !metric.unit.isEmpty {
                        Text(metric.unit)
                            .font(DSFont.mono(11))
                            .foregroundColor(DSColor.textMuted)
                    }
                }
                Spacer(minLength: 0)
                if metric.hasPrev && compare {
                    MiniBarPair(
                        prev: abs(metric.prevBar),
                        cur: abs(metric.curBar),
                        height: wide ? 40 : 30
                    )
                }
            }

            if compare {
                HStack(spacing: 6) {
                    StatDeltaTag(delta: metric.delta)
                    if metric.hasPrev {
                        Text("было \(metric.prevValue)")
                            .font(DSFont.sans(11))
                            .foregroundColor(DSColor.textFaint)
                            .lineLimit(1)
                    }
                }
                .padding(.top, 10)
            }
        }
        .padding(DSMetrics.cardPadX)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
    }
}

// MARK: - Дельта (§13.3): монохром, стрелка направления + процент

struct StatDeltaTag: View {
    let delta: StatDelta?

    var body: some View {
        if let delta = delta, let pct = delta.pct {
            HStack(spacing: 3) {
                arrow(dir: delta.dir)
                Text(pct)
                    .font(DSFont.mono(11, .semibold))
            }
            .foregroundColor(DSColor.textMuted)
        }
    }

    private func arrow(dir: String?) -> some View {
        Group {
            switch dir {
            case "up": Image(systemName: "arrow.up")
            case "down": Image(systemName: "arrow.down")
            default: Image(systemName: "minus")
            }
        }
        .font(.system(size: 9, weight: .bold))
    }
}

// MARK: - Мини-пара столбцов было/стало

private struct MiniBarPair: View {
    let prev: Float
    let cur: Float
    let height: CGFloat

    var body: some View {
        let maxValue = max(prev, cur, 0.0001)
        HStack(alignment: .bottom, spacing: 4) {
            Capsule()
                .fill(DSColor.borderStrong)
                .frame(width: 6, height: height * CGFloat(prev / maxValue))
            Capsule()
                .fill(DSColor.accent)
                .frame(width: 6, height: height * CGFloat(cur / maxValue))
        }
        .frame(height: height, alignment: .bottom)
    }
}

// MARK: - Топ направлений (§13.4)

private struct StatTopDirectionsSection: View {
    let directions: [StatTopDirection]

    var body: some View {
        if !directions.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                Text("Топ направлений")
                    .font(DSFont.sans(19, .bold))
                    .tracking(-0.3)
                    .foregroundColor(DSColor.text)
                    .padding(.top, DSSpacing.xl)
                    .padding(.bottom, DSSpacing.md)

                DSCard {
                    VStack(spacing: 0) {
                        StatDonutView(
                            segments: directions.map {
                                StatDonutSeg(label: "\($0.from) → \($0.to)", value: $0.tripsAsFloat, sub: "\($0.trips) поездок · \($0.hours) ч")
                            },
                            centerMain: "\(directions.reduce(0) { $0 + Int($1.trips) })",
                            centerSub: "поездок"
                        )
                        .padding(.vertical, DSSpacing.lg)

                        ForEach(Array(directions.enumerated()), id: \.offset) { index, dir in
                            if index > 0 { DSDivider(inset: 0) }
                            HStack(alignment: .top, spacing: DSSpacing.sm) {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text("\(dir.from) → \(dir.to)")
                                        .font(DSFont.sans(14, .medium))
                                        .foregroundColor(DSColor.text)
                                    Text(dir.hours)
                                        .font(DSFont.mono(11))
                                        .foregroundColor(DSColor.textMuted)
                                }
                                Spacer(minLength: 0)
                                Text("\(Int(dir.trips)) раз")
                                    .font(DSFont.sans(13, .semibold))
                                    .foregroundColor(DSColor.text)
                            }
                            .padding(.vertical, 10)
                        }
                    }
                    .padding(.horizontal, DSSpacing.md)
                }
            }
        }
    }
}

private extension StatTopDirection {
    var tripsAsFloat: Float { Float(trips) }
}

// MARK: - Год: hero + помесячные столбцы (§13.4)

private struct StatYearHero: View {
    let state: StatisticsIosUiState

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("ОТРАБОТАНО ЗА ГОД · Ч")
                .font(DSFont.mono(11, .semibold))
                .tracking(1.4)
                .foregroundColor(DSColor.textMuted)
                .padding(.bottom, 8)

            HStack(alignment: .top, spacing: DSSpacing.md) {
                Text(state.yearHeroValue)
                    .font(DSFont.mono(46, .heavy))
                    .tracking(-1.5)
                    .foregroundColor(DSColor.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Spacer(minLength: 0)
                if state.compareEnabled {
                    StatDeltaTag(delta: state.yearHeroDelta)
                        .padding(.top, 10)
                }
            }
        }
        .padding(DSMetrics.cardPadX)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        .padding(.bottom, DSSpacing.md)
    }
}

/// Помесячные столбцы: текущий год — широкий (14pt), база — уже (10pt).
/// Тап по столбцу показывает/скрывает число часов над ним (локальное состояние,
/// без обращения к ViewModel — §13).
private struct StatYearBarsChart: View {
    let state: StatisticsIosUiState
    @State private var selectedIndex: Int? = nil

    private var maxValue: Float {
        max(state.yearBars.map { max($0.cur, $0.prev) }.max() ?? 0, 0.0001)
    }

    var body: some View {
        DSCard {
            VStack(alignment: .leading, spacing: DSSpacing.md) {
                if state.compareEnabled && state.yearBars.contains(where: { $0.prev > 0 }) {
                    HStack(spacing: DSSpacing.lg) {
                        legendDot(color: DSColor.accent, label: "\(state.periodLabel)")
                        legendDot(color: DSColor.borderStrong, label: "предыдущий")
                    }
                }

                HStack(alignment: .bottom, spacing: 6) {
                    ForEach(Array(state.yearBars.enumerated()), id: \.offset) { index, bar in
                        VStack(spacing: 6) {
                            if selectedIndex == index {
                                Text(hoursLabel(bar.cur))
                                    .font(DSFont.mono(10, .semibold))
                                    .foregroundColor(DSColor.text)
                                    .transition(.opacity)
                            }
                            HStack(alignment: .bottom, spacing: 2) {
                                if bar.prev > 0 {
                                    Capsule()
                                        .fill(DSColor.borderStrong)
                                        .frame(width: 10, height: barHeight(bar.prev))
                                }
                                Capsule()
                                    .fill(DSColor.accent)
                                    .frame(width: 14, height: barHeight(bar.cur))
                            }
                            Text(bar.label)
                                .font(DSFont.sans(9))
                                .foregroundColor(DSColor.textMuted)
                        }
                        .frame(maxWidth: .infinity)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            withAnimation(.easeInOut(duration: 0.15)) {
                                selectedIndex = selectedIndex == index ? nil : index
                            }
                        }
                    }
                }
                .frame(height: 150, alignment: .bottom)
            }
            .padding(DSMetrics.cardPadX)
        }
    }

    private func barHeight(_ value: Float) -> CGFloat {
        CGFloat(value / maxValue) * 110
    }

    private func hoursLabel(_ value: Float) -> String {
        "\(Int(value.rounded()))"
    }

    private func legendDot(color: Color, label: String) -> some View {
        HStack(spacing: 5) {
            Capsule().fill(color).frame(width: 10, height: 10)
            Text(label)
                .font(DSFont.sans(11))
                .foregroundColor(DSColor.textMuted)
        }
    }
}

// MARK: - История (§13.4)

private struct StatHistoryBody: View {
    let state: StatisticsIosUiState
    @ObservedObject var vm: StatisticsViewModelWrapper

    private let columns = [
        GridItem(.flexible(), spacing: DSSpacing.md),
        GridItem(.flexible(), spacing: DSSpacing.md),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Всё время")
                .font(DSFont.sans(19, .bold))
                .tracking(-0.3)
                .foregroundColor(DSColor.text)
                .padding(.top, DSSpacing.sm)
                .padding(.bottom, DSSpacing.md)

            LazyVGrid(columns: columns, spacing: DSSpacing.md) {
                ForEach(state.historyMetrics, id: \.key) { metric in
                    Button {
                        vm.selectHistoryMetric(key: metric.key)
                    } label: {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(metric.label)
                                .font(DSFont.sans(12.5, .semibold))
                                .foregroundColor(DSColor.textMuted)
                            HStack(alignment: .firstTextBaseline, spacing: 5) {
                                Text(metric.total)
                                    .font(DSFont.mono(22, .heavy))
                                    .foregroundColor(
                                        state.historySelected == metric.key
                                            ? DSColor.text : DSColor.text
                                    )
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.6)
                                if !metric.unit.isEmpty {
                                    Text(metric.unit)
                                        .font(DSFont.mono(11))
                                        .foregroundColor(DSColor.textMuted)
                                }
                            }
                        }
                        .padding(DSMetrics.cardPadX)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(DSColor.surface)
                        .overlay(
                            RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous)
                                .stroke(
                                    state.historySelected == metric.key
                                        ? DSColor.accent : Color.clear,
                                    lineWidth: 2
                                )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }

            // Итог выбранной метрики
            VStack(alignment: .leading, spacing: 8) {
                Text(state.historyTotalCaption)
                    .font(DSFont.mono(11, .semibold))
                    .tracking(1.2)
                    .foregroundColor(DSColor.textMuted)
                Text(state.historyTotal)
                    .font(DSFont.mono(38, .heavy))
                    .tracking(-1)
                    .foregroundColor(DSColor.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Text("За \(state.historyYearsCount) \(StatFormat.shared.yearsWord(n: Int32(state.historyYearsCount))) · \(state.historyRoutesTotal) смен")
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.textMuted)
            }
            .padding(.top, DSSpacing.xl)

            Text("Год за годом")
                .font(DSFont.sans(19, .bold))
                .tracking(-0.3)
                .foregroundColor(DSColor.text)
                .padding(.top, DSSpacing.xl)
                .padding(.bottom, DSSpacing.md)

            DSCard {
                VStack(spacing: 0) {
                    ForEach(Array(state.historyRows.enumerated()), id: \.offset) { index, row in
                        if index > 0 { DSDivider(inset: 0) }
                        VStack(alignment: .leading, spacing: 6) {
                            HStack(alignment: .firstTextBaseline, spacing: 6) {
                                Text("\(Int(row.year))")
                                    .font(DSFont.sans(14, .semibold))
                                    .foregroundColor(DSColor.text)
                                if let note = row.note {
                                    Text(note)
                                        .font(DSFont.sans(11))
                                        .foregroundColor(DSColor.textMuted)
                                }
                                Spacer(minLength: 0)
                                Text("\(row.value) \(row.unit)")
                                    .font(DSFont.mono(13, .semibold))
                                    .foregroundColor(DSColor.text)
                            }
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule()
                                        .fill(DSColor.bgSubtle)
                                        .frame(height: 6)
                                    Capsule()
                                        .fill(DSColor.accent)
                                        .frame(width: geo.size.width * CGFloat(row.fraction), height: 6)
                                }
                            }
                            .frame(height: 6)
                        }
                        .padding(.vertical, 12)
                    }
                }
                .padding(.horizontal, DSSpacing.md)
            }
        }
    }
}

// MARK: - Donut (топ направлений / разбивка детализации)

struct StatDonutView: View {
    let segments: [StatDonutSeg]
    let centerMain: String
    let centerSub: String

    private let palette: [Color] = [
        DSColor.accent,
        DSColor.success,
        DSColor.warning,
        DSColor.rail,
        DSColor.textFaint,
    ]

    private var total: Float {
        max(segments.reduce(0) { $0 + $1.value }, 0.0001)
    }

    var body: some View {
        HStack(alignment: .center, spacing: DSSpacing.xl) {
            ZStack {
                Circle()
                    .stroke(DSColor.bgSubtle, lineWidth: 16)
                ForEach(Array(segments.enumerated()), id: \.offset) { index, seg in
                    Circle()
                        .trim(from: startFraction(index), to: endFraction(index))
                        .stroke(
                            palette[index % palette.count],
                            style: StrokeStyle(lineWidth: 16, lineCap: .butt)
                        )
                        .rotationEffect(.degrees(-90))
                }
                VStack(spacing: 2) {
                    Text(centerMain)
                        .font(DSFont.mono(18, .heavy))
                        .foregroundColor(DSColor.text)
                    Text(centerSub)
                        .font(DSFont.sans(10))
                        .foregroundColor(DSColor.textMuted)
                }
            }
            .frame(width: 116, height: 116)

            if segments.count > 1 {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(Array(segments.prefix(4).enumerated()), id: \.offset) { index, seg in
                        HStack(spacing: 8) {
                            Circle()
                                .fill(palette[index % palette.count])
                                .frame(width: 8, height: 8)
                            Text(seg.label)
                                .font(DSFont.sans(11))
                                .foregroundColor(DSColor.textMuted)
                                .lineLimit(1)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private func startFraction(_ index: Int) -> CGFloat {
        CGFloat(segments.prefix(index).reduce(0) { $0 + $1.value } / total)
    }

    private func endFraction(_ index: Int) -> CGFloat {
        CGFloat(segments.prefix(index + 1).reduce(0) { $0 + $1.value } / total)
    }
}

#Preview("Статистика — light") {
    NavigationStack {
        StatisticsView()
    }
    .preferredColorScheme(.light)
}
