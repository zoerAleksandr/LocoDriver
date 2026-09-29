import SwiftUI

/// Экран для маршрута стека. Реализованные экраны — существующие View,
/// остальные — `StubView` с разделом спеки.
/// ОБЩАЯ ЗОНА: волна заменяет ТОЛЬКО ветку своего экрана (StubView → экран).
struct AppDestinationView: View {
    let route: AppRoute

    var body: some View {
        switch route {
        case .routeForm(let basicId):
            FormView(routeId: basicId)
        case let .locoForm(basicId, locoId):
            FormLocoView(routeId: basicId, locoId: locoId)
        case let .trainForm(basicId, trainId):
            FormTrainView(routeId: basicId, trainId: trainId)
        case let .passengerForm(basicId, passengerId):
            FormPassengerView(routeId: basicId, passengerId: passengerId)
        case .allRoutes:
            AllRoutesDestination()
        case .search:
            SearchView()
        case .calendar:
            // Временная частичная реализация (сетка месяца). Волна «Календарь» (§14)
            // заменяет на полноценный CalendarView.
            WorkScheduleView()
        case .purchases:
            PurchasesView()
        case .routeFormCopy, .sharedRoutePreview, .otherWorkForm,
             .partnersManage, .partnerPicker, .partnerEditor,
             .trash, .payrollCodeSearch, .settingSalary, .statistics,
             .scheduleWizard, .absence, .settingsSection, .referral:
            StubView(title: route.title, specSection: route.specSection)
        }
    }
}

/// «Все маршруты» использует ViewModel Главной (как и в Android — месяц общий).
private struct AllRoutesDestination: View {
    @StateObject private var vm = HomeViewModelWrapper()

    var body: some View {
        AllRoutesView(vm: vm)
    }
}

/// Содержимое шторки/оверлея из реестра `AppSheet`. Пока все — заглушки.
/// ОБЩАЯ ЗОНА: волна заменяет ТОЛЬКО ветку своей шторки.
struct AppSheetView: View {
    let sheet: AppSheet
    let onClose: () -> Void

    var body: some View {
        switch sheet {
        case .announcement:
            NavigationStack {
                StubView(title: sheet.title, specSection: sheet.specSection)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Закрыть", action: onClose)
                        }
                    }
            }
        default:
            StubSheetView(title: sheet.title, specSection: sheet.specSection, onClose: onClose)
        }
    }
}
