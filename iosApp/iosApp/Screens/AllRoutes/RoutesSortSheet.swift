import SwiftUI

// Шторка «Сортировка» — SCREEN_SPECS §9.0.1. Радиокнопки, выбор закрывает шторку.
// Макет: design/src/all-routes.jsx → IOSSortSheet / ArSortBody.

struct RoutesSortSheet: View {
    let current: String
    let onSelect: (String) -> Void
    let onClose: () -> Void

    private struct Option: Identifiable {
        let id = UUID()
        let name: String
        let label: String
    }

    private let options: [Option] = [
        Option(name: "DATE_DESC", label: "Новые"),
        Option(name: "DATE_ASC", label: "Старые"),
        Option(name: "WORKTIME_DESC", label: "Много часов на работе"),
        Option(name: "WORKTIME_ASC", label: "Мало часов на работе"),
    ]

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Сортировка", subtitle: nil), onClose: onClose) {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(options.enumerated()), id: \.element.id) { idx, option in
                    Button {
                        onSelect(option.name)
                    } label: {
                        HStack(spacing: DSSpacing.md) {
                            ZStack {
                                Circle()
                                    .stroke(current == option.name ? DSColor.accent : DSColor.borderStrong, lineWidth: 2)
                                    .frame(width: 20, height: 20)
                                if current == option.name {
                                    Circle()
                                        .fill(DSColor.accent)
                                        .frame(width: 10, height: 10)
                                }
                            }
                            Text(option.label)
                                .font(DSFont.sans(15))
                                .foregroundColor(DSColor.text)
                            Spacer(minLength: 0)
                        }
                        .padding(.vertical, 14)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    if idx < options.count - 1 {
                        DSDivider(inset: 0)
                    }
                }
            }
        }
    }
}
