import SwiftUI
import ComposeApp

// Шторки и детализация экрана «Статистика» (SCREEN_SPECS §13.3–§13.4).
// Макет: design/src/stats-screens.jsx → IOSStatsCompare (ComparePicker),
//        IOSStatsDetail / IOSStatsDetailYear, IOSStatsLegend.
// Показываются локально через `.dsSheet` (см. примечание в AppRoute.swift:
// кейсы AppSheet остаются реестром/заглушкой).

// MARK: - Шторка «Сравнить месяц/год с» (§13.3)

struct StatCompareSheet: View {
    let isYear: Bool
    let options: [StatCompareOption]
    let selectedId: String
    let onSelect: (String) -> Void
    let onPick: () -> Void
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(
            header: .titleClose(
                title: isYear ? "Сравнить год с" : "Сравнить месяц с",
                subtitle: "С чем сравнивать выбранный период"
            ),
            onClose: onClose
        ) {
            VStack(spacing: 0) {
                ForEach(Array(options.enumerated()), id: \.offset) { _, option in
                    Button {
                        if option.action {
                            onPick()
                        } else {
                            onSelect(option.id)
                        }
                    } label: {
                        HStack(spacing: DSSpacing.md) {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(option.title)
                                    .font(DSFont.sans(15, .medium))
                                    .foregroundColor(option.off ? DSColor.textMuted : DSColor.text)
                                Text(option.note)
                                    .font(DSFont.sans(12))
                                    .foregroundColor(DSColor.textMuted)
                            }
                            Spacer(minLength: 0)
                            if option.action {
                                DSIcon.chevronRight.image
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(DSColor.textFaint)
                            } else if option.id == selectedId {
                                DSIcon.check.image
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(DSColor.accent)
                            }
                        }
                        .padding(.vertical, 14)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    DSDivider(inset: 0)
                }
            }
        }
    }
}

// MARK: - Пикер «Выбрать месяц…» (§13.3)

struct StatMonthGridSheet: View {
    let years: [Int]
    let availableYm: Set<Int>
    let currentYm: Int
    let onPick: (Int) -> Void
    let onBack: () -> Void
    let onClose: () -> Void

    private let shortNames = [
        "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
        "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек",
    ]

    private let columns = Array(
        repeating: GridItem(.flexible(), spacing: DSSpacing.sm),
        count: 4
    )

    var body: some View {
        DSBottomSheet(
            header: .titleClose(
                title: "Выбрать месяц",
                subtitle: "Сравнить можно с месяцем, где есть маршруты"
            ),
            onClose: onClose
        ) {
            HStack(spacing: 0) {
                Button(action: onBack) {
                    HStack(spacing: 2) {
                        DSIcon.chevronLeft.image
                            .font(.system(size: 13, weight: .semibold))
                        Text("Назад")
                            .font(DSFont.sans(15))
                    }
                    .foregroundColor(DSColor.accent)
                }
                .buttonStyle(.plain)
                Spacer()
            }
            .padding(.bottom, DSSpacing.md)

            ForEach(years, id: \.self) { year in
                Text(String(year))
                    .font(DSFont.sans(15, .bold))
                    .foregroundColor(DSColor.text)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, DSSpacing.md)
                    .padding(.bottom, DSSpacing.sm)

                LazyVGrid(columns: columns, spacing: DSSpacing.sm) {
                    ForEach(0..<12, id: \.self) { month in
                        StatMonthCell(
                            title: shortNames[month],
                            enabled: availableYm.contains(year * 12 + month)
                                && (year * 12 + month) != currentYm
                        )
                        .onTapGesture {
                            let ym = year * 12 + month
                            if availableYm.contains(ym) && ym != currentYm {
                                onPick(ym)
                            }
                        }
                    }
                }
            }
        }
    }
}

/// Ячейка месяца в сетке пикера: недоступные приглушены (30%).
private struct StatMonthCell: View {
    let title: String
    let enabled: Bool

    var body: some View {
        Text(title)
            .font(DSFont.sans(14, .medium))
            .foregroundColor(DSColor.text)
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .background(DSColor.chipBg)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.sm, style: .continuous))
            .opacity(enabled ? 1 : 0.3)
    }
}

// MARK: - Пикер «Выбрать год…» (§13.3)

struct StatYearListSheet: View {
    let years: [Int]
    let onPick: (Int) -> Void
    let onBack: () -> Void
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(
            header: .titleClose(
                title: "Выбрать год",
                subtitle: "Сравнить с любым годом из истории"
            ),
            onClose: onClose
        ) {
            HStack(spacing: 0) {
                Button(action: onBack) {
                    HStack(spacing: 2) {
                        DSIcon.chevronLeft.image
                            .font(.system(size: 13, weight: .semibold))
                        Text("Назад")
                            .font(DSFont.sans(15))
                    }
                    .foregroundColor(DSColor.accent)
                }
                .buttonStyle(.plain)
                Spacer()
            }
            .padding(.bottom, DSSpacing.sm)

            ForEach(years, id: \.self) { year in
                Button {
                    onPick(year)
                } label: {
                    HStack {
                        Text("\(year) год")
                            .font(DSFont.sans(15, .medium))
                            .foregroundColor(DSColor.text)
                        Spacer()
                    }
                    .padding(.vertical, 14)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                DSDivider(inset: 0)
            }
        }
    }
}

// MARK: - Шторка «Грузооборот» (кружок «?», §13.4)

struct StatFreightInfoSheet: View {
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(
            header: .titleClose(title: "Грузооборот", subtitle: nil),
            onClose: onClose
        ) {
            VStack(alignment: .leading, spacing: DSSpacing.md) {
                Text("Тонно-км брутто — объём выполненной перевозочной работы.")
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.text)
                Text("Считается как вес поезда брутто (в тоннах) × пройденное расстояние (в км), просуммированный по всем поездам за период.")
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.text)
                Text("Единица — млн ткм (миллионы тонно-километров).")
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.text)
            }
            .padding(.top, DSSpacing.sm)
        }
    }
}

// MARK: - Детализация метрики (§13.4) — overlay поверх вкладки

struct StatDetailView: View {
    let state: StatisticsIosUiState
    @ObservedObject var vm: StatisticsViewModelWrapper

    @State private var selectedBar: Int32 = 0

    var body: some View {
        if let detail = state.detail {
            VStack(spacing: 0) {
                topBar(detail: detail)
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        hero(detail: detail)
                        bars(detail: detail)
                        if !detail.breakdown.isEmpty {
                            breakdown(detail: detail)
                        }
                    }
                    .padding(.horizontal, DSMetrics.screenPadX)
                    .padding(.bottom, 40)
                }
            }
            .background(DSColor.bg.ignoresSafeArea())
            .onAppear {
                selectedBar = detail.selectedIndex
            }
            .onChange(of: detail.selectedIndex) { newValue in
                selectedBar = newValue
            }
        }
    }

    private func topBar(detail: StatDetailState) -> some View {
        ZStack {
            Text(detail.title)
                .font(DSFont.sans(17, .bold))
                .foregroundColor(DSColor.text)
            HStack {
                Button {
                    vm.closeDetail()
                } label: {
                    HStack(spacing: 2) {
                        DSIcon.chevronLeft.image
                            .font(.system(size: 15, weight: .semibold))
                        Text("Назад")
                            .font(DSFont.sans(15))
                    }
                    .foregroundColor(DSColor.accent)
                }
                .buttonStyle(.plain)
                Spacer()
            }
        }
        .padding(.horizontal, DSSpacing.md)
        .frame(height: 48)
    }

    private func hero(detail: StatDetailState) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(detail.caption)
                .font(DSFont.mono(11, .semibold))
                .tracking(1.2)
                .foregroundColor(DSColor.textMuted)
                .padding(.bottom, 10)

            HStack(alignment: .top, spacing: DSSpacing.md) {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(detail.heroValue)
                        .font(DSFont.mono(40, .heavy))
                        .tracking(-1)
                        .foregroundColor(DSColor.text)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    if !detail.heroUnit.isEmpty {
                        Text(detail.heroUnit)
                            .font(DSFont.mono(13))
                            .foregroundColor(DSColor.textMuted)
                    }
                }
                Spacer(minLength: 0)
                VStack(alignment: .trailing, spacing: 4) {
                    StatDeltaTag(delta: detail.heroDelta)
                    if !detail.heroPrevCaption.isEmpty {
                        Text(detail.heroPrevCaption)
                            .font(DSFont.sans(11))
                            .foregroundColor(DSColor.textFaint)
                    }
                }
                .padding(.top, 8)
            }
        }
        .padding(DSMetrics.cardPadX)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        .padding(.bottom, DSSpacing.md)
    }

    /// 12 столбцов фиксированной ширины (14pt) — геометрия не меняется при
    /// выборе месяца. Ripple при тапе не показывается (столбец — не кнопка).
    private func bars(detail: StatDetailState) -> some View {
        let maxValue = max(detail.bars.map { $0.value }.max() ?? 0, 0.0001)
        return DSCard {
            VStack(alignment: .leading, spacing: DSSpacing.md) {
                HStack(alignment: .bottom, spacing: 6) {
                    ForEach(Array(detail.bars.enumerated()), id: \.offset) { index, bar in
                        StatDetailBarColumn(
                            bar: bar,
                            selected: Int32(index) == selectedBar,
                            maxValue: maxValue
                        )
                        .onTapGesture {
                            selectedBar = Int32(index)
                            vm.selectDetailMonth(index: index)
                        }
                    }
                }
                .frame(height: 150, alignment: .bottom)
            }
            .padding(DSMetrics.cardPadX)
        }
        .padding(.bottom, DSSpacing.md)
    }

    /// Разбивка по направлениям («Детали · по направлениям») для метрик на
    /// основе поездов: Расстояние / Грузооборот / Время в пути.
    private func breakdown(detail: StatDetailState) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Детали · по направлениям")
                .font(DSFont.sans(19, .bold))
                .tracking(-0.3)
                .foregroundColor(DSColor.text)
                .padding(.top, DSSpacing.sm)
                .padding(.bottom, DSSpacing.md)

            DSCard {
                StatDonutView(
                    segments: detail.breakdown,
                    centerMain: detail.breakdownCenter,
                    centerSub: detail.breakdownCenterSub
                )
                .padding(DSMetrics.cardPadX)
            }
        }
    }
}

/// Один столбец графика детализации: фиксированная ширина 14pt.
private struct StatDetailBarColumn: View {
    let bar: StatDetailBar
    let selected: Bool
    let maxValue: Float

    var body: some View {
        VStack(spacing: 6) {
            if selected {
                Text(bar.shortLabel)
                    .font(DSFont.mono(10, .semibold))
                    .foregroundColor(DSColor.text)
            }
            Capsule()
                .fill(selected ? DSColor.accent : DSColor.accentSoft)
                .frame(width: 14, height: barHeight)
            Text(bar.shortLabel)
                .font(DSFont.sans(9))
                .foregroundColor(selected ? DSColor.text : DSColor.textMuted)
        }
        .frame(maxWidth: .infinity)
        .contentShape(Rectangle())
    }

    private var barHeight: CGFloat {
        CGFloat(bar.value / max(maxValue, 0.0001)) * 100
    }
}
