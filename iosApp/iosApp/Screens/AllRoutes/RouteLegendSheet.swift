import SwiftUI

// Легенда значков — SCREEN_SPECS §9.3. Read-only шторка «Обозначения».
// Макет: design/src/all-routes.jsx → IOSLegendSheet / ArLegendBody.

struct RouteLegendSheet: View {
    let onClose: () -> Void

    private struct Item: Identifiable {
        let id = UUID()
        let key: String
        let label: String
    }

    private let items: [Item] = [
        Item(key: "holiday", label: "Работа в праздничный день"),
        Item(key: "break", label: "Перерыв в работе"),
        Item(key: "length", label: "Поезда повышенной длины"),
        Item(key: "weight", label: "Поезда повышенной массы"),
        Item(key: "shoulder", label: "Удлинённое плечо обслуживания"),
        Item(key: "solo", label: "Работа в одно лицо"),
        Item(key: "passenger", label: "Следование пассажиром"),
        Item(key: "over12", label: "Работа свыше 12-ти часов"),
        Item(key: "pusher", label: "Толкач"),
        Item(key: "double", label: "Двойная тяга"),
        Item(key: "coupled", label: "Сдвоенный поезд"),
        Item(key: "sync", label: "Статус синхронизации маршрута"),
    ]

    var body: some View {
        DSBottomSheet(
            header: .titleClose(
                title: "Обозначения",
                subtitle: "Значки в строке маршрута показывают его особенности. У одного маршрута может быть несколько отметок."
            ),
            onClose: onClose
        ) {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(items) { item in
                    HStack(spacing: DSSpacing.md) {
                        Group {
                            if item.key == "sync" {
                                DSIcon.sync.image
                                    .font(.system(size: 17, weight: .medium))
                                    .foregroundColor(DSColor.text)
                            } else {
                                HomeRouteAttributeIcon(key: item.key)
                            }
                        }
                        .frame(width: 28)
                        Text(item.label)
                            .font(DSFont.sans(15))
                            .foregroundColor(DSColor.text)
                        Spacer(minLength: 0)
                    }
                    .padding(.vertical, 12)
                    if item.key != items.last?.key {
                        DSDivider(inset: 0)
                    }
                }
            }
        }
    }
}
