import SwiftUI

// Шторка «Фильтры» — SCREEN_SPECS §9.0.1. Множественный выбор, условия
// объединяются через И; «Все» (ALL) очищает остальные.
// Макет: design/src/all-routes.jsx → IOSFilterSheet / ArFilterBody.

struct RoutesFilterSheet: View {
    let selectedFilters: [String]
    let onToggle: (String) -> Void
    let onClose: () -> Void

    private struct FilterItem: Identifiable {
        let id = UUID()
        let name: String
        let label: String
        let systemIcon: String
    }

    private let allItem = FilterItem(name: "ALL", label: "Все", systemIcon: "checkmark.circle")

    private let items: [FilterItem] = [
        FilterItem(name: "FAVORITES", label: "Избранные", systemIcon: "heart"),
        FilterItem(name: "HEAVY", label: "Тяжелые", systemIcon: "scalemass"),
        FilterItem(name: "EXTENDED_SERVICE", label: "Удлинённые плечи", systemIcon: "arrow.left.and.right"),
        FilterItem(name: "FOLLOWING_RESERVE", label: "Резервом", systemIcon: "arrow.triangle.branch"),
        FilterItem(name: "ONE_PERSON", label: "Одно лицо", systemIcon: "person"),
        FilterItem(name: "OVER_12_HOURS", label: "Свыше 12ч", systemIcon: "clock.badge.exclamationmark"),
        FilterItem(name: "LONG_TRAINS", label: "Длинные поезда", systemIcon: "ruler"),
        FilterItem(name: "HAS_BREAK", label: "С перерывами", systemIcon: "pause.circle"),
        FilterItem(name: "PUSHER", label: "Толкач", systemIcon: "arrow.right.to.line"),
        FilterItem(name: "DOUBLE_TRACTION", label: "Двойная тяга", systemIcon: "square.on.square"),
        FilterItem(name: "DOUBLED_TRAIN", label: "Сдвоенный", systemIcon: "rectangle.on.rectangle"),
    ]

    private func isSelected(_ name: String) -> Bool {
        selectedFilters.contains(name)
    }

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Фильтры", subtitle: nil), onClose: onClose) {
            Text("Выбранные условия объединяются через «И».")
                .font(DSFont.sans(12))
                .foregroundColor(DSColor.textMuted)
                .padding(.bottom, DSSpacing.sm)
            chipRow(allItem)
            Divider().padding(.vertical, DSSpacing.sm)
            FlowLayout(spacing: DSSpacing.sm) {
                ForEach(items) { item in
                    chipRow(item)
                }
            }
        }
    }

    private func chipRow(_ item: FilterItem) -> some View {
        Button {
            onToggle(item.name)
        } label: {
            HStack(spacing: 6) {
                Image(systemName: item.systemIcon)
                    .font(.system(size: 13, weight: .semibold))
                Text(item.label)
                    .font(DSFont.sans(13, .semibold))
                    .lineLimit(1)
            }
            .foregroundColor(isSelected(item.name) ? DSColor.accent : DSColor.text)
            .padding(.horizontal, 12)
            .frame(height: 34)
            .background(isSelected(item.name) ? DSColor.chipBgActive : DSColor.chipBg)
            .clipShape(Capsule())
        }
        .buttonStyle(DSPressableStyle())
    }
}

// MARK: - Простой flow-раскладчик для чипов

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        return CGSize(width: maxWidth == .infinity ? x : maxWidth, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX, x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            sub.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
