import SwiftUI
import ComposeApp

// MARK: - HomeView

/// Главный экран — SCREEN_SPECS §4. Макет: design/src/ios-screens.jsx → IOSScreenTrips
/// (HeroCard, HeroStatRow, CurrentTile, UpcomingRoute, RestAtTurnaround, TripRow, ToolTile).
///
/// Данные и расчёты — `HomeScreenIosViewModel` (Kotlin); экран только раскладывает
/// готовые строки. Системный навбар скрыт: верхняя строка и строка месяца — часть
/// экрана (заголовок месяца показывается один раз).
struct HomeView: View {
    @StateObject private var vm = HomeScreenViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared
    @Environment(\.openURL) private var openURL

    @State private var metricsPage = 0
    @State private var isMonthSheetShown = false
    @State private var isSiteSheetShown = false
    @State private var unitsSheet: HomeUnitsSheetItem? = nil
    @State private var routeToDelete: HomeDeleteRequest? = nil
    @State private var isReorderMode = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                topBar
                if vm.ui?.isBackgroundSyncing == true {
                    ProgressView()
                        .progressViewStyle(.linear)
                        .tint(DSColor.accent)
                        .frame(maxWidth: .infinity)
                }
                content
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.bottom, 50)
            .background(
                // Касание вне секции «Текущий маршрут» завершает режим перестановки (§4.3).
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture { if isReorderMode { isReorderMode = false } }
            )
        }
        .refreshable { await vm.pullToSync() }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .dsSnackbarQueue(vm.messages) { vm.popMessage() }
        .onAppear { vm.onAppear() }
        .onDisappear {
            vm.onDisappear()
            isReorderMode = false
        }
        .dsSheet(isPresented: $isMonthSheetShown, detents: [.medium, .large]) {
            HomeMonthSheet(
                months: (vm.ui?.monthOptions ?? []).map { Int($0.int32Value) },
                years: (vm.ui?.yearOptions ?? []).map { Int($0.int32Value) },
                selectedMonth: Int(vm.ui?.selectedMonth ?? 0),
                selectedYear: Int(vm.ui?.selectedYear ?? 0),
                onApply: { year, month in
                    vm.setMonth(year: year, month: month)
                    isMonthSheetShown = false
                },
                onClose: { isMonthSheetShown = false }
            )
        }
        .dsSheet(isPresented: $isSiteSheetShown, detents: [.height(260)]) {
            HomeSiteSheet(
                onGo: {
                    isSiteSheetShown = false
                    if let url = URL(string: "https://locodriver.ru") { openURL(url) }
                },
                onClose: { isSiteSheetShown = false }
            )
        }
        .dsSheet(item: $unitsSheet) { item in
            HomeUnitsSheet(
                tile: item.tile,
                onSelect: { unitId in
                    unitsSheet = nil
                    // Выбор закрывает шторку, затем навигация (§4.3).
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        openUnit(type: item.tile.type, basicId: item.basicId, unitId: unitId)
                    }
                },
                onAdd: {
                    unitsSheet = nil
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        openUnit(type: item.tile.type, basicId: item.basicId, unitId: nil)
                    }
                },
                onClose: { unitsSheet = nil }
            )
        }
        .dsSheet(item: $routeToDelete, detents: [.height(250)]) { request in
            HomeDeleteRouteSheet(
                subtitle: request.subtitle,
                onConfirm: {
                    routeToDelete = nil
                    vm.removeRoute(basicId: request.basicId)
                },
                onClose: { routeToDelete = nil }
            )
        }
        .dsSheet(isPresented: syncProgressBinding, detents: [.medium]) {
            if let dialog = vm.syncDialog {
                HomeSyncProgressSheet(
                    dialog: dialog,
                    onSendReport: { sendReport(dialog.reportText) },
                    onClose: { vm.resetSyncState() }
                )
            }
        }
        .alert("Выгрузка завершена!", isPresented: syncSuccessBinding) {
            Button("Отлично!") { vm.resetSyncState() }
        } message: {
            Text("Данные успешно синхронизированы.")
        }
    }

    // MARK: - Состояния

    @ViewBuilder
    private var content: some View {
        if let ui = vm.ui, ui.isReady {
            if ui.isError {
                DSEmptyState(icon: .warning, title: "Что-то пошло не так…")
                    .padding(.top, 80)
            } else {
                loaded(ui)
            }
        } else {
            HomeSkeleton()
        }
    }

    @ViewBuilder
    private func loaded(_ ui: HomeIosScreenUi) -> some View {
        monthRow(ui)
            .allowsHitTesting(!isReorderMode)

        HomeHeroSection(ui: ui, page: $metricsPage) {
            router.showStatistics()
        }
        .allowsHitTesting(!isReorderMode)

        if !ui.notices.isEmpty {
            HomeNoticeCarousel(
                notices: ui.notices,
                onDismiss: { vm.dismissNotice($0) },
                onAction: { notice in
                    if notice.action == "sync" {
                        vm.manualSync()
                    } else {
                        router.showPurchases(isAuthorized: ui.isAuthorized)
                    }
                }
            )
            .padding(.top, DSSpacing.xxl)
            .allowsHitTesting(!isReorderMode)
        }

        if let live = vm.live {
            HomeLiveSection(
                live: live,
                isReorderMode: $isReorderMode,
                onOpenRoute: { router.showRouteForm(basicId: $0) },
                onTile: { tile in handleTileTap(tile, basicId: live.basicId) },
                onAddUnit: { type in openUnit(type: type, basicId: live.basicId, unitId: nil) }
            )
        }

        lastRoutesSection(ui)
            .allowsHitTesting(!isReorderMode)

        toolsSection
            .allowsHitTesting(!isReorderMode)
    }

    // MARK: - Верхняя строка (§4.4 п.1)

    private var topBar: some View {
        HStack(spacing: DSSpacing.sm) {
            Button { isSiteSheetShown = true } label: {
                HStack(spacing: DSSpacing.sm) {
                    Text("М")
                        .font(DSFont.sans(28, .heavy))
                        .foregroundColor(DSColor.accent)
                    Text("Машинист")
                        .font(DSFont.sans(15, .semibold))
                        .foregroundColor(DSColor.text)
                }
                .padding(.vertical, 4)
                .contentShape(Rectangle())
            }
            .buttonStyle(DSPressableStyle())
            Spacer()
            DSIconButton(icon: .search, variant: .ghost, accessibilityLabel: "Поиск") {
                router.showSearch()
            }
        }
        .padding(.top, DSSpacing.sm)
        .allowsHitTesting(!isReorderMode)
    }

    // MARK: - Строка месяца (§4.4 п.2)

    private func monthRow(_ ui: HomeIosScreenUi) -> some View {
        HStack(alignment: .center, spacing: DSSpacing.xs) {
            Button { isMonthSheetShown = true } label: {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(ui.monthTitle)
                        .font(DSFont.sans(28, .bold))
                        .tracking(-0.5)
                        .foregroundColor(DSColor.text)
                    Text(ui.yearTitle)
                        .font(DSFont.mono(22, .medium))
                        .foregroundColor(DSColor.textMuted)
                }
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .padding(.vertical, 6)
                .contentShape(Rectangle())
            }
            .buttonStyle(DSPressableStyle())
            .accessibilityHint(Text("Выбрать месяц"))
            Spacer(minLength: DSSpacing.sm)
            monthArrow(.chevronLeft, enabled: ui.hasPrevMonth, label: "Предыдущий месяц") { vm.previousMonth() }
            monthArrow(.chevronRight, enabled: ui.hasNextMonth, label: "Следующий месяц") { vm.nextMonth() }
        }
        .padding(.top, DSSpacing.md)
    }

    private func monthArrow(_ icon: DSIcon, enabled: Bool, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            icon.image
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(enabled ? DSColor.accent : DSColor.textFaint)
                .frame(width: 40, height: 40)
                .contentShape(Rectangle())
        }
        .buttonStyle(DSPressableStyle())
        .disabled(!enabled)
        .accessibilityLabel(Text(label))
    }

    // MARK: - «Последние маршруты» (§4.4 п.7)

    private func lastRoutesSection(_ ui: HomeIosScreenUi) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            DSSectionHeader(title: "Последние маршруты") {
                DSTextAction(title: "Все (\(ui.monthRoutesCount))", trailingIcon: .chevronRight) {
                    router.showAllRoute()
                }
            }
            .padding(.top, DSSpacing.sm)
            if ui.lastRoutes.isEmpty {
                VStack(spacing: 6) {
                    Text("Список пуст")
                        .font(DSFont.sans(17, .semibold))
                        .foregroundColor(DSColor.text)
                    Text("Нажмите  +  чтобы добавить маршрут")
                        .font(DSFont.sans(14))
                        .foregroundColor(DSColor.textMuted)
                    Text("или создайте график работы")
                        .font(DSFont.sans(14))
                        .foregroundColor(DSColor.textMuted)
                }
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.vertical, DSSpacing.xxl)
            } else {
                DSCard {
                    ForEach(Array(ui.lastRoutes.enumerated()), id: \.element.basicId) { index, card in
                        HomeRouteCardRow(
                            card: card,
                            onTap: { router.showRouteForm(basicId: card.basicId) },
                            onLongPress: { router.present(.routeQuickView(basicId: card.basicId)) },
                            onLegend: { router.present(.routeLegend) },
                            onDelete: {
                                routeToDelete = HomeDeleteRequest(
                                    basicId: card.basicId,
                                    subtitle: vm.removeRouteSubtitle(basicId: card.basicId)
                                )
                            }
                        )
                        if index < ui.lastRoutes.count - 1 { DSDivider() }
                    }
                }
            }
        }
    }

    // MARK: - «Инструменты» (§4.4 п.8)

    private var toolsSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            DSSectionHeader("Инструменты")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    HomeToolTile(systemImage: DSIcon.calendar.rawValue, title: "Календарь") { router.showCalendar() }
                    HomeToolTile(systemImage: "chart.bar", title: "Статистика") { router.showStatistics() }
                    // PDF (§21): формирование PDF на iOS ещё не реализовано — карточка всегда активна.
                    HomeToolTile(systemImage: DSIcon.pdf.rawValue, title: "PDF") { router.present(.pdfContent) }
                    HomeToolTile(systemImage: DSIcon.search.rawValue, title: "Поиск") { router.showSearch() }
                }
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.vertical, 4)
            }
            .padding(.horizontal, -DSMetrics.screenPadX)
        }
    }

    // MARK: - Действия

    private func handleTileTap(_ tile: HomeIosUnitTile, basicId: String) {
        let count = Int(tile.count)
        if count > 1 {
            unitsSheet = HomeUnitsSheetItem(tile: tile, basicId: basicId)
        } else if count == 1 {
            openUnit(type: tile.type, basicId: basicId, unitId: tile.items.first?.id)
        } else {
            openUnit(type: tile.type, basicId: basicId, unitId: nil)
        }
    }

    private func openUnit(type: String, basicId: String, unitId: String?) {
        switch type {
        case "loco": router.showLocoForm(basicId: basicId, locoId: unitId)
        case "train": router.showTrainForm(basicId: basicId, trainId: unitId)
        default: router.showPassengerForm(basicId: basicId, passengerId: unitId)
        }
    }

    private var syncProgressBinding: Binding<Bool> {
        Binding(
            get: {
                guard let dialog = vm.syncDialog else { return false }
                return dialog.isVisible && !dialog.isSuccess
            },
            set: { shown in
                // Закрытие шторки = resetSyncState (§18.5); сама операция продолжается.
                if !shown, vm.syncDialog?.isVisible == true, vm.syncDialog?.isSuccess == false {
                    vm.resetSyncState()
                }
            }
        )
    }

    private var syncSuccessBinding: Binding<Bool> {
        Binding(
            get: { vm.syncDialog?.isSuccess == true },
            set: { shown in if !shown, vm.syncDialog?.isSuccess == true { vm.resetSyncState() } }
        )
    }

    /// «Отправить отчет об ошибке»: письмо на locodriver.app@yandex.ru с текстом отчёта.
    private func sendReport(_ report: String) {
        var components = URLComponents()
        components.scheme = "mailto"
        components.path = "locodriver.app@yandex.ru"
        components.queryItems = [
            URLQueryItem(name: "subject", value: "Отчет об ошибках синхронизации"),
            URLQueryItem(name: "body", value: report),
        ]
        if let url = components.url { openURL(url) }
    }
}

// MARK: - Вспомогательные типы

struct HomeUnitsSheetItem: Identifiable {
    let tile: HomeIosUnitTile
    let basicId: String
    var id: String { "\(basicId)-\(tile.type)" }
}

struct HomeDeleteRequest: Identifiable {
    let basicId: String
    let subtitle: String
    var id: String { basicId }
}

/// Карточка инструмента — `ToolTile` макета.
struct HomeToolTile: View {
    let systemImage: String
    let title: String
    var isEnabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 0) {
                Image(systemName: systemImage)
                    .font(.system(size: 34, weight: .regular))
                    .foregroundColor(DSColor.text.opacity(0.85))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                Text(title)
                    .font(DSFont.sans(13, .medium))
                    .foregroundColor(DSColor.text)
                    .lineLimit(1)
                    .padding(.horizontal, DSSpacing.lg)
                    .padding(.bottom, 14)
            }
            .frame(width: 120, height: 120)
            .background(DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
            .dsShadow(.sm)
            .opacity(isEnabled ? 1 : 0.4)
        }
        .buttonStyle(DSPressableStyle())
        .disabled(!isEnabled)
    }
}

/// Скелетон первой загрузки (§4.1): плашка вместо карусели, 3 точки и 4 карточки.
struct HomeSkeleton: View {
    var body: some View {
        VStack(alignment: .leading, spacing: DSSpacing.lg) {
            DSSkeletonBlock(width: 180, height: 30, cornerRadius: DSRadius.sm)
                .padding(.top, DSSpacing.md)
            DSSkeletonBlock(height: 140, cornerRadius: DSRadius.xl)
            HStack(spacing: 6) {
                ForEach(0..<3, id: \.self) { _ in
                    DSSkeletonBlock(width: 6, height: 6, cornerRadius: 3)
                }
            }
            .frame(maxWidth: .infinity)
            DSSkeletonList(rows: 4)
        }
    }
}

#Preview("Главная — light") {
    NavigationStack { HomeSkeleton().padding().background(DSColor.bg) }
        .preferredColorScheme(.light)
}

#Preview("Главная — dark") {
    NavigationStack { HomeSkeleton().padding().background(DSColor.bg) }
        .preferredColorScheme(.dark)
}
