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
                    .foregroundColor(DSColor.textMuted)
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
                    AllRouteCard(route: route)
                }
                .listRowInsets(EdgeInsets(top: 4, leading: 8, bottom: 4, trailing: 8))
                .listRowBackground(DSColor.bg)
                .listRowSeparator(.hidden)
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
        .scrollContentBackground(.hidden)
        .background(DSColor.bg)
    }
}
