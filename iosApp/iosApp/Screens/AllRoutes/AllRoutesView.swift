import SwiftUI
import ComposeApp

struct AllRoutesView: View {
    @StateObject private var vm = HomeViewModelWrapper()

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if vm.routes.isEmpty {
                Text("Нет маршрутов")
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(vm.routes, id: \.basicData.id) { route in
                    NavigationLink(destination: FormView(routeId: route.basicData.id)) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Маршрут \(route.basicData.number ?? "—")")
                                .font(.headline)
                            if let ms = route.basicData.timeStartWork as? Int64, ms > 0 {
                                Text(TimeFormatter.formatDateTime(ms: ms))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 2)
                    }
                    .swipeActions(edge: .trailing) {
                        Button(role: .destructive) {
                            // TODO: delete
                        } label: {
                            Label("Удалить", systemImage: "trash")
                        }
                    }
                }
            }
        }
        .navigationTitle("Все маршруты")
    }
}
