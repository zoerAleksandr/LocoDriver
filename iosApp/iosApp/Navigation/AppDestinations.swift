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
            AllRoutesView()
        case .search:
            SearchView()
        case .trash:
            TrashView()
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
        case .payrollCodeSearch:
            PayrollCodeSearchView()
        case .settingSalary:
            SettingSalaryView()
        case .settingsSection(let section):
            SettingsSectionDestination(section: section)
        case let .otherWorkForm(basicId, otherWorkId):
            FormOtherWorkView(routeId: basicId, otherWorkId: otherWorkId)
        case .partnersManage:
            PartnersListView(basicId: nil)
        case .partnerPicker(let basicId):
            PartnersListView(basicId: basicId)
        case .partnerEditor(let partnerId):
            PartnerEditorView(partnerId: partnerId)
        case .statistics:
            StatisticsView()
        case .referral:
            ReferralView()
        case .dataPrivacy:
            DataPrivacyView()
        case .trackHistory:
            TrackHistoryView()
        }
    }
}

/// Содержимое шторки/оверлея из реестра `AppSheet`.
/// ОБЩАЯ ЗОНА: волна заменяет ТОЛЬКО ветку своей шторки.
struct AppSheetView: View {
    let sheet: AppSheet
    let onClose: () -> Void

    var body: some View {
        switch sheet {
        case .announcement:
            // §23 Новость при запуске: очередь ведёт LaunchOverlays (closeCurrent
            // помечает «видел» и показывает следующее сообщение/«Что нового»).
            AnnouncementView(
                store: LaunchOverlays.shared.announcement,
                onClose: { LaunchOverlays.shared.closeCurrent() }
            )
        case .whatsNew:
            // §22 «Что нового»: «Пропустить»/«Начать» → закрыть.
            WhatsNewView(onClose: { LaunchOverlays.shared.closeCurrent() })
        case let .routeQuickView(basicId):
            RouteQuickViewStandaloneView(basicId: basicId, onClose: onClose)
        case .routeLegend:
            RouteLegendSheet(onClose: onClose)
        case .routesFilter:
            StandaloneFilterSheet(onClose: onClose)
        case .routesSort:
            StandaloneSortSheet(onClose: onClose)
        case .searchSettings:
            StandaloneSearchSettingsSheet(onClose: onClose)
        case let .calcSheet(basicId):
            CalcBottomSheet(basicId: basicId, onClose: onClose)
        case .tariffChanged:
            TariffChangedSheet(onClose: onClose)
        case .addEvent(let dayMillis):
            AddEventSheetHost(dayMillis: dayMillis, onClose: onClose)
        // §21 PDF: шторки «Содержание PDF» и «PDF сформирован».
        case .pdfContent:
            PdfContentSheetView(onClose: onClose)
        case .pdfActions:
            PdfActionSheetView(onClose: onClose)
        default:
            StubSheetView(title: sheet.title, specSection: sheet.specSection, onClose: onClose)
        }
    }
}

// MARK: - Самодостаточные шторки для глобального реестра `AppSheet`
//
// Экраны «Все маршруты» и «Поиск» показывают эти шторки ЛОКАЛЬНО (с доступом
// к своему ViewModel). Глобальный реестр — для переходов из других экранов
// (например, быстрый просмотр с Главной), поэтому здесь обёртки создают свой
// ViewModel для действий.

/// Быстрый просмотр из глобального реестра (действия — через свой ViewModel).
private struct RouteQuickViewStandaloneView: View {
    let basicId: String
    let onClose: () -> Void

    @StateObject private var actionsVM = AllRoutesViewModelWrapper()
    @State private var deleteRequest: DSDeleteRequest?
    @State private var shareItem: SharePayload?
    @State private var favorite = false

    var body: some View {
        RouteQuickViewSheet(
            basicId: basicId,
            isFavorite: favorite,
            actions: RouteQuickViewActions(
                onOpen: {
                    onClose()
                    AppRouter.shared.showRouteForm(basicId: basicId)
                },
                onToggleFavorite: { actionsVM.setFavoriteRoute(routeId: basicId) },
                onShare: {
                    onClose()
                    actionsVM.shareRoute(routeId: basicId)
                },
                onCopy: {
                    onClose()
                    actionsVM.onCopyRoute(routeId: basicId) { decision in
                        if decision.type == "open" {
                            AppRouter.shared.showRouteForm(basicId: basicId, isMakeCopy: true)
                        }
                    }
                },
                onSync: {
                    onClose()
                    actionsVM.syncRoute(routeId: basicId)
                },
                onDelete: {
                    onClose()
                    deleteRequest = DSDeleteRequest(
                        title: "Удалить маршрут?",
                        message: "Это действие нельзя отменить.",
                        confirmTitle: "Да, удалить"
                    ) {
                        actionsVM.deleteRoute(routeId: basicId)
                    }
                }
            )
        )
        .dsDeleteAlert($deleteRequest)
        .sheet(item: $shareItem) { RouteShareSheet(text: $0.text, subject: $0.subject) }
        .onReceive(actionsVM.$share) { payload in
            if let payload = payload {
                shareItem = SharePayload(text: payload.text, subject: payload.subject)
                actionsVM.share = nil
            }
        }
    }
}

/// Глобальная шторка «Фильтры» (состояние — в SharedPreferences через ViewModel).
private struct StandaloneFilterSheet: View {
    let onClose: () -> Void
    @StateObject private var vm = AllRoutesViewModelWrapper()

    var body: some View {
        RoutesFilterSheet(
            selectedFilters: vm.ui?.selectedFilters ?? ["ALL"],
            onToggle: { vm.toggleFilter(name: $0) },
            onClose: onClose
        )
    }
}

/// Глобальная шторка «Сортировка».
private struct StandaloneSortSheet: View {
    let onClose: () -> Void
    @StateObject private var vm = AllRoutesViewModelWrapper()

    var body: some View {
        RoutesSortSheet(
            current: vm.sortName,
            onSelect: { name in
                vm.setSort(name: name)
                onClose()
            },
            onClose: onClose
        )
    }
}

/// Глобальная шторка «Параметры поиска» (§10.3).
private struct StandaloneSearchSettingsSheet: View {
    let onClose: () -> Void
    @StateObject private var vm = SearchViewModelWrapper()

    var body: some View {
        SearchSettingsSheet(vm: vm, onClose: onClose)
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
