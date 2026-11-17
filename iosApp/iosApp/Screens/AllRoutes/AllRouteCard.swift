import SwiftUI
import ComposeApp

/// Общая карточка маршрута для «Все маршруты» и импорта общей ссылки.
struct AllRouteCard: View {
    let route: DomainRoute

    var body: some View {
        DSCard {
            VStack(alignment: .leading, spacing: 4) {
                Text("Маршрут \(route.basicData.number ?? "—")")
                    .font(.headline)
                    .foregroundColor(DSColor.text)
                let start = route.basicData.timeStartWork?.int64Value ?? 0
                if start > 0 {
                    Text(TimeFormatter.formatDateTime(ms: start))
                        .font(DSFont.mono(12))
                        .foregroundColor(DSColor.textMuted)
                }
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 12)
        }
    }
}
