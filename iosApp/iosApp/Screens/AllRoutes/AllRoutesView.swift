import SwiftUI
import ComposeApp

struct AllRoutesView: View {
    @ObservedObject var vm: HomeViewModelWrapper

    @State private var showDeleteConfirm = false
    @State private var routeToDelete: String? = nil
    @State private var isExpanded = false
    @State private var isReversed = false
    /// ID маршрута, открываемого тапом по строке / preview (через UIKit bridge).
    @State private var openRouteId: String? = nil

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if vm.routes.isEmpty {
                Text("Нет маршрутов")
                    .foregroundColor(Color.appSecondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                routeList
            }
        }
        .background(Color.appBg)
        .navigationTitle("Все маршруты")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                HStack(spacing: 16) {
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            isReversed.toggle()
                        }
                    } label: {
                        Image(systemName: isReversed ? "arrow.up" : "arrow.down")
                            .font(.system(size: 14))
                            .foregroundColor(Color.appSecondary)
                    }
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            isExpanded.toggle()
                        }
                    } label: {
                        Image(systemName: isExpanded ? "list.bullet" : "list.bullet.below.rectangle")
                            .font(.system(size: 14))
                            .foregroundColor(Color.appSecondary)
                    }
                }
            }
        }
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
        ScrollView {
            VStack(spacing: 0) {
                let displayRoutes = isReversed ? vm.routes.reversed() : vm.routes
                let total = vm.routes.count
                ForEach(Array(displayRoutes.enumerated()), id: \.element.basicData.id) { idx, route in
                    let number = isReversed ? idx + 1 : total - idx
                    RouteContextMenuRow(
                        content: RouteItemView(
                            route: route,
                            number: number,
                            isExpanded: isExpanded,
                            settings: vm.settings
                        ),
                        preview: {
                            RoutePreviewCard(
                                route: route,
                                allRoutes: vm.routes,
                                settings: vm.settings
                            )
                        },
                        actions: {
                            routeMenuActions(for: route)
                        },
                        onCommit: {
                            openRouteId = route.basicData.id
                        }
                    )
                    if idx < displayRoutes.count - 1 {
                        Divider().padding(.leading, 16)
                    }
                }
            }
            .background(Color.appCard)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .navigationDestination(
            isPresented: Binding(
                get: { openRouteId != nil },
                set: { if !$0 { openRouteId = nil } }
            )
        ) {
            if let id = openRouteId {
                FormView(routeId: id)
            }
        }
    }

    private func routeMenuActions(for route: DomainRoute) -> [ContextMenuAction] {
        [
            ContextMenuAction(title: "Просмотр", systemImage: "eye") {
                openRouteId = route.basicData.id
            },
            ContextMenuAction(title: "Сохранить в облаке", systemImage: "icloud.and.arrow.up") {
                vm.syncRoute(routeId: route.basicData.id)
            },
            ContextMenuAction(
                title: route.basicData.isFavorite ? "Убрать из избранного" : "В избранное",
                systemImage: route.basicData.isFavorite ? "heart.slash" : "heart"
            ) {
                vm.toggleFavorite(routeId: route.basicData.id)
            },
            ContextMenuAction(title: "Дублировать", systemImage: "doc.on.doc") {
                vm.copyRoute(routeId: route.basicData.id)
            },
            ContextMenuAction(title: "Поделиться", systemImage: "square.and.arrow.up") {
                vm.shareRoute(routeId: route.basicData.id)
            },
            ContextMenuAction(title: "Удалить", systemImage: "trash", isDestructive: true) {
                routeToDelete = route.basicData.id
                showDeleteConfirm = true
            }
        ]
    }
}
