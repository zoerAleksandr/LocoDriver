import SwiftUI
import ComposeApp

/// Блок «ОТРАБОТАНО» и карусель метрик (§4.4 п.3–4). Макет: `HeroCard`, `HeroStatRow`.
struct HomeHeroSection: View {
    let ui: HomeIosScreenUi
    @Binding var page: Int
    let onOpenStatistics: () -> Void

    @State private var tip: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            carousel
                .padding(.top, DSSpacing.lg)
            pageIndicator
                .padding(.top, DSSpacing.md)
        }
        .padding(.top, DSSpacing.lg)
    }

    // MARK: - «ОТРАБОТАНО»

    private var header: some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack(alignment: .firstTextBaseline) {
                Text("Отработано").dsTextStyle(.group)
                Spacer(minLength: DSSpacing.sm)
                Text(ui.moneyText)
                    .font(DSFont.mono(17, .semibold))
                    .foregroundColor(ui.moneyState == 1 ? DSColor.text : DSColor.textMuted)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
            // Адаптивная раскладка: если не помещается — чип строкой выше, разбивка под числом.
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .lastTextBaseline, spacing: DSSpacing.sm) {
                    totalText
                    breakdown
                    Spacer(minLength: DSSpacing.sm)
                    chip
                }
                VStack(alignment: .leading, spacing: 6) {
                    HStack { Spacer(minLength: 0); chip }
                    totalText
                    breakdown
                }
            }
            if let tip = tip {
                Text(tip)
                    .dsTextStyle(.captionMuted)
                    .transition(.opacity)
            }
        }
        .padding(.horizontal, 6)
    }

    @ViewBuilder
    private var totalText: some View {
        if let total = ui.totalTimeText {
            Text(total)
                .font(DSFont.mono(46, .heavy))
                .tracking(-1.5)
                .foregroundColor(DSColor.text)
                .lineLimit(1)
                .fixedSize()
                .onTapGesture { showTip("Общее отработанное время") }
        } else if ui.isTotalTimeError {
            Text("Ошибка").font(DSFont.sans(17, .semibold)).foregroundColor(DSColor.danger)
        } else {
            ProgressView().tint(DSColor.textMuted).frame(height: 46)
        }
    }

    @ViewBuilder
    private var breakdown: some View {
        if let text = ui.breakdownText {
            Text(text)
                .font(DSFont.mono(14, .medium))
                .foregroundColor(DSColor.textMuted)
                .lineLimit(1)
                .fixedSize()
                .onTapGesture { showTip("Рабочие + праздничные часы") }
        }
    }

    @ViewBuilder
    private var chip: some View {
        if let text = ui.normaChipText {
            DSPill(text: text, tone: text.hasPrefix("сверх") ? .accent : .muted)
                .fixedSize()
        }
    }

    private func showTip(_ text: String) {
        withAnimation { tip = text }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
            withAnimation { if tip == text { tip = nil } }
        }
    }

    // MARK: - Карусель

    private var carousel: some View {
        TabView(selection: $page) {
            ForEach(Array(ui.metricPages.enumerated()), id: \.offset) { index, rows in
                VStack(alignment: .leading, spacing: 14) {
                    ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                        HomeMetricRowView(row: row)
                    }
                    Spacer(minLength: 0)
                }
                .padding(22)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .background(DSColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.xl, style: .continuous))
                .dsShadow(.md)
                .padding(.horizontal, 2)
                .padding(.bottom, 10)
                .tag(index)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        // Все страницы по три строки — высота карусели постоянна.
        .frame(height: 178)
        .contentShape(Rectangle())
        .onTapGesture { onOpenStatistics() }
    }

    private var pageIndicator: some View {
        HStack(spacing: 6) {
            ForEach(0..<max(ui.metricPages.count, 1), id: \.self) { index in
                Capsule()
                    .fill(index == page ? DSColor.textMuted : DSColor.borderStrong)
                    .frame(width: index == page ? 18 : 6, height: 6)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: page)
        .frame(maxWidth: .infinity)
    }
}

/// Строка метрики: подпись … mono-значение + тонкая полоса (макет `HeroStatRow`).
struct HomeMetricRowView: View {
    let row: HomeIosMetricRow

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack(alignment: .firstTextBaseline) {
                Text(row.label)
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.textMuted)
                    .lineLimit(1)
                Spacer(minLength: DSSpacing.sm)
                value
            }
            HomeProgressBar(progress: Double(row.progress), height: 4, fill: DSColor.accent, track: DSColor.bgSubtle)
        }
    }

    @ViewBuilder
    private var value: some View {
        if row.isError {
            Text("Ошибка").font(DSFont.sans(14, .medium)).foregroundColor(DSColor.danger)
        } else if let value = row.value {
            Text(value)
                .font(DSFont.mono(14, .semibold))
                .foregroundColor(row.progress > 0 ? DSColor.text : DSColor.textFaint)
        } else {
            ProgressView().scaleEffect(0.6).frame(width: 16, height: 16)
        }
    }
}

/// Тонкая полоса прогресса с дорожкой.
struct HomeProgressBar: View {
    let progress: Double
    var height: CGFloat = 3
    var fill: Color = DSColor.accent
    var track: Color = DSColor.accentSoft

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(track)
                Capsule()
                    .fill(fill)
                    .frame(width: geo.size.width * CGFloat(min(max(progress, 0), 1)))
            }
        }
        .frame(height: height)
    }
}
