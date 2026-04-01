import SwiftUI
import ComposeApp

struct AllRoutesView: View {
    @ObservedObject var vm: HomeViewModelWrapper

    @State private var showDeleteConfirm = false
    @State private var routeToDelete: String? = nil

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
                routeList
            }
        }
        .navigationTitle("Все маршруты")
        .alert("Удалить маршрут?", isPresented: $showDeleteConfirm) {
            Button("Удалить", role: .destructive) {
                if let id = routeToDelete {
                    vm.deleteRoute(routeId: id)
                    routeToDelete = nil
                }
            }
            Button("Отмена", role: .cancel) {
                routeToDelete = nil
            }
        } message: {
            Text("Это действие нельзя отменить.")
        }
    }

    private var routeList: some View {
        List {
            ForEach(vm.routes, id: \.basicData.id) { route in
                NavigationLink(destination: FormView(routeId: route.basicData.id)) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Маршрут \(route.basicData.number ?? "—")")
                            .font(.headline)
                        let ms = route.basicData.timeStartWork?.int64Value ?? 0
                        if ms > 0 {
                            Text(TimeFormatter.formatDateTime(ms: ms))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 2)
                }
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) {
                        routeToDelete = route.basicData.id
                        showDeleteConfirm = true
                    } label: {
                        Label("Удалить", systemImage: "trash")
                    }
                }
            }
        }
    }
}
