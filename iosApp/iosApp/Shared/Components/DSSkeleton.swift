import SwiftUI

// Макет: design/src/route-quick-view.jsx → QVGhostList (строка маршрута:
//        дата+время mono 16/600, часы mono 17/700 справа, маршрут 15, низ 14 pt).
// Спека: SCREEN_SPECS §4.1 — `SkeletonHomeScreen` / `SkeletonItemHomeScreen`
// (состояние первой загрузки Главной). Блоки — `bgSubtle`, мягкая пульсация.

/// Блок-заглушка скелетона.
struct DSSkeletonBlock: View {
    var width: CGFloat? = nil
    var height: CGFloat = 14
    var cornerRadius: CGFloat = DSRadius.xs

    @State private var isDimmed = false

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(DSColor.bgSubtle)
            .frame(width: width, height: height)
            .frame(maxWidth: width == nil ? .infinity : nil, alignment: .leading)
            .opacity(isDimmed ? 0.45 : 1)
            .onAppear {
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                    isDimmed = true
                }
            }
            .accessibilityHidden(true)
    }
}

/// Скелетон строки маршрута (геометрия `TripRow`: padding 14/20, gap 6).
struct DSSkeletonRouteRow: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                DSSkeletonBlock(width: 170, height: 16)
                Spacer()
                DSSkeletonBlock(width: 48, height: 17)
            }
            DSSkeletonBlock(width: 210, height: 15)
            DSSkeletonBlock(width: 36, height: 12)
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, DSMetrics.rowPadY)
    }
}

/// Скелетон списка маршрутов в карточке.
struct DSSkeletonList: View {
    var rows: Int = 3

    var body: some View {
        DSCard {
            ForEach(0..<rows, id: \.self) { index in
                DSSkeletonRouteRow()
                if index < rows - 1 { DSDivider() }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("Загрузка"))
    }
}

#Preview("Скелетон — light") {
    DSSkeletonPreview().preferredColorScheme(.light)
}

#Preview("Скелетон — dark") {
    DSSkeletonPreview().preferredColorScheme(.dark)
}

private struct DSSkeletonPreview: View {
    var body: some View {
        DSPreviewCanvas {
            DSSkeletonBlock(width: 120, height: 48, cornerRadius: DSRadius.md)
            DSSkeletonList(rows: 3)
        }
    }
}
