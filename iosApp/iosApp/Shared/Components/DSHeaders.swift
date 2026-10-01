import SwiftUI

// Макет: design/src/shared-ui.jsx → GroupHead, SectionH2;
//        design/src/ios-screens.jsx → GroupLabel, SecH.

/// Заголовок группы карточек: mono 11, UPPERCASE, tracking 1.4, textMuted
/// («ОСНОВНЫЕ ДАННЫЕ», «ЛОКОМОТИВЫ»). Для экранов-форм.
/// `topSpacing`: 22 между группами (по умолчанию), 4 — для первого заголовка после навбара.
struct DSGroupHeader<Trailing: View>: View {
    let title: String
    var topSpacing: CGFloat = DSMetrics.groupHeadMt
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(alignment: .center) {
            Text(title).dsTextStyle(.group)
            Spacer(minLength: DSSpacing.sm)
            trailing()
        }
        .padding(.top, DSMetrics.groupHeadPadTop)
        .padding(.horizontal, DSMetrics.groupHeadPadX)
        .padding(.bottom, DSMetrics.groupHeadPadBottom)
        .padding(.top, topSpacing)
    }
}

extension DSGroupHeader where Trailing == EmptyView {
    init(_ title: String, topSpacing: CGFloat = DSMetrics.groupHeadMt) {
        self.title = title
        self.topSpacing = topSpacing
        self.trailing = { EmptyView() }
    }
}

/// Крупный заголовок секции: sans 20/700, tracking −0.3 («Текущий маршрут», «Маршруты»).
/// Для контентных экранов (Главная, дашборды). Padding `22 4 12`.
struct DSSectionHeader<Trailing: View>: View {
    let title: String
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(alignment: .center) {
            Text(title).dsTextStyle(.sectionH2)
            Spacer(minLength: DSSpacing.sm)
            trailing()
        }
        .padding(.top, 22)
        .padding(.horizontal, 4)
        .padding(.bottom, 12)
    }
}

extension DSSectionHeader where Trailing == EmptyView {
    init(_ title: String) {
        self.title = title
        self.trailing = { EmptyView() }
    }
}

#Preview("Заголовки — light") {
    DSHeadersPreview().preferredColorScheme(.light)
}

#Preview("Заголовки — dark") {
    DSHeadersPreview().preferredColorScheme(.dark)
}

private struct DSHeadersPreview: View {
    var body: some View {
        DSPreviewCanvas {
            VStack(alignment: .leading, spacing: 0) {
                DSGroupHeader("Основные данные", topSpacing: DSMetrics.groupHeadFirst)
                DSCard { Text("…").padding(DSMetrics.rowPadX) }
                DSGroupHeader(title: "Время работы") {
                    Text("08:00").dsTextStyle(.timeMono)
                }
                DSSectionHeader(title: "Маршруты") {
                    DSTextAction(title: "Все (23)", trailingIcon: .chevronRight) {}
                }
            }
        }
    }
}
