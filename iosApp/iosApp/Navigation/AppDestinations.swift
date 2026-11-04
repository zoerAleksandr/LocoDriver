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
            CalendarView()
        case .scheduleWizard:
            ScheduleWizardView()
        case .absence:
            AbsenceView()
        case .purchases:
            PurchasesView()
        case .settingsSection(.seriesList):
            SeriesListView()
        case .settingsSection(.stationList):
            StationListView()
        case .settingsSection(.seriesEditor(let id)):
            SeriesEditorScreen(id: id, name: nil)
        case .settingsSection(.seriesNew(let name)):
            SeriesEditorScreen(id: nil, name: name.isEmpty ? nil : name)
        case .settingsSection(.stationEditor(let id)):
            StationEditorScreen(id: id.isEmpty ? nil : id)
        case .routeFormCopy(let basicId):
            FormView(routeId: basicId, isCopy: true)
        case .sharedRoutePreview(let routeId):
            FormView(routeId: routeId, isSharedPreview: true)
        case .settingsSection(let section):
            SettingsSectionDestination(section: section)
        case .otherWorkForm,
             .partnersManage, .partnerPicker, .partnerEditor,
             .trash, .payrollCodeSearch, .settingSalary, .statistics,
             .referral:
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
        case .addEvent(let dayMillis):
            AddEventSheetHost(dayMillis: dayMillis, onClose: onClose)
        default:
            StubSheetView(title: sheet.title, specSection: sheet.specSection, onClose: onClose)
        }
    }
}

/// Хост «Что добавить?» (§14.3) для `AppSheet.addEvent`: шторка действует на
/// общий CalendarViewModel (single), поэтому работает и из реестра шторок.
private struct AddEventSheetHost: View {
    let dayMillis: Int64
    let onClose: () -> Void
    @StateObject private var vm = CalendarViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared
    @State private var showTechStudy = false

    private var day: Int {
        let date = Date(timeIntervalSince1970: Double(dayMillis) / 1000)
        return Calendar.current.component(.day, from: date)
    }

    var body: some View {
        AddEventSheetView(
            day: day,
            monthLabel: vm.ui?.monthLabelNormal ?? "",
            onRoute: {
                onClose()
                vm.enterRoutePlan(initialDay: day)
            },
            onAbsence: {
                onClose()
                router.showAbsence()
            },
            onDayOff: {
                onClose()
                vm.addDayOff(day)
            },
            onTechStudy: {
                showTechStudy = true
            }
        )
        .dsSheet(isPresented: $showTechStudy, detents: [.height(340)]) {
            TechnicalStudyDialog(
                day: day,
                monthLabel: vm.ui?.monthLabelNormal ?? "",
                averagePayText: { hours in vm.techStudyPayText(hours: hours) },
                onSave: { hours in
                    onClose()
                    vm.addTechnicalStudy(day, hours: hours)
                }
            )
        }
    }
}
