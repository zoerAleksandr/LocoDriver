import SwiftUI
import ComposeApp

// Экран «Все маршруты» — SCREEN_SPECS §9.
// Макет: design/src/all-routes.jsx → IOSScreenAllRoutes / IOSRouteCard.
// Логика — Kotlin `AllRoutesScreenIosViewModel` (обёртка `AllRoutesViewModelWrapper`).
// По спеке (макет противоречит): PDF-чипа нет (§21 волна 4); переключатели
// «отдых в ПО» и плотности — в строке фильтра/сортировки, не в навбаре.

struct AllRoutesView: View {
    @StateObject private var vm = AllRoutesViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared

    // Локальные шторки (§9.0.1 / §9.1 / §9.3).
    private enum ActiveSheet: Identifiable {
        case month, filter, sort, legend
        case quickView(basicId: String, isFavorite: Bool)
        var id: String {
            switch self {
            case .month: return "month"
            case .filter: return "filter"
            case .sort: return "sort"
            case .legend: return "legend"
            case let .quickView(basicId, _): return "quick-\(basicId)"
            }
        }
    }

    @State private var sheet: ActiveSheet?
    @State private var deleteRequest: DSDeleteRequest?
    @State private var shareItem: SharePayload?
    @State private var newRouteGate: NewRouteGateAlert?
    /// Локальное зеркало выбранных id (Kotlin-модель не отдаёт признак выбора по карточке).
    @State private var selectedIds: Set<String> = []

    var body: some View {
        VStack(spacing: 0) {
            navBar
            monthRow
            if !(vm.ui?.rows.isEmpty ?? true) || vm.ui?.isSelectionMode == true {
                filterSortRow
            }
            content
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .overlay(alignment: .bottom) { selectionBar }
        .overlay(alignment: .bottomTrailing) { fab }
        .dsSnackbarQueue(vm.messages) { vm.popMessage() }
        .onAppear {
            vm.syncOnScreenOpen()
        }
        .onDisappear {
            vm.stopScreenSync()
        }
        .onChange(of: vm.ui?.isSelectionMode) { sel in
            if sel != true { selectedIds.removeAll() }
        }
        .sheet(item: $shareItem) { RouteShareSheet(text: $0.text, subject: $0.subject) }
        .dsSheet(item: $sheet, detents: [.medium, .large]) { s in
            sheetView(s)
        }
        .dsDeleteAlert($deleteRequest)
        .background(newRouteGateHost)
    }

    // ── Топбар (§9.0.1) ──────────────────────────────────────────────────
    @ViewBuilder
    private var navBar: some View {
        if vm.ui?.isSelectionMode == true {
            DSNavBar(title: "Выбрано: \(Int(vm.ui?.selectedCount ?? 0))", onBack: nil) {
                HStack(spacing: DSSpacing.md) {
                    Button(vm.ui?.allVisibleSelected == true ? "Снять" : "Все") {
                        doToggleSelectAll()
                    }
                    .font(DSFont.sans(15, .medium))
                    .foregroundColor(DSColor.accent)
                    Spacer()
                    Button("Готово") { exitSelection() }
                        .font(DSFont.sans(15, .semibold))
                        .foregroundColor(DSColor.accent)
                }
            }
        } else {
            DSNavBar(title: "Маршруты", onBack: { router.back() }) {
                DSNavPillButton(title: "Выбрать", isAccent: false) {
                    selectedIds.removeAll()
                    vm.enterSelectionMode()
                }
            }
        }
    }

    // ── Строка месяца (§9.0.1) ───────────────────────────────────────────
    private var monthRow: some View {
        HStack(spacing: DSSpacing.sm) {
            monthArrow(.chevronLeft, enabled: vm.ui?.hasPrevMonth == true, label: "Предыдущий месяц") {
                vm.prevMonth()
            }
            Button {
                sheet = .month
            } label: {
                VStack(spacing: 1) {
                    Text(vm.ui?.monthTitle ?? "")
                        .font(DSFont.sans(19, .bold))
                        .tracking(-0.3)
                        .foregroundColor(DSColor.text)
                    Text(vm.ui?.yearTitle ?? "")
                        .font(DSFont.mono(11))
                        .tracking(1)
                        .foregroundColor(DSColor.textMuted)
                }
                .frame(minWidth: 150)
            }
            .buttonStyle(.plain)
            monthArrow(.chevronRight, enabled: vm.ui?.hasNextMonth == true, label: "Следующий месяц") {
                vm.nextMonth()
            }
            Spacer(minLength: DSSpacing.sm)
            summaryPills
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.vertical, 4)
    }

    private var summaryPills: some View {
        HStack(spacing: DSSpacing.sm) {
            let count = Int(vm.ui?.routesCount ?? 0)
            DSPill(text: "\(count) марш.", tone: .muted, isMono: true)
            if !(vm.ui?.monthWorkedTimeText.isEmpty ?? true) {
                HStack(spacing: 4) {
                    DSIcon.clock.image.font(.system(size: 11, weight: .semibold))
                    Text(vm.ui?.monthWorkedTimeText ?? "")
                        .font(DSFont.mono(12, .semibold))
                }
                .foregroundColor(DSColor.textMuted)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(DSColor.bgSubtle)
                .clipShape(Capsule())
            }
        }
    }

    private func monthArrow(_ icon: DSIcon, enabled: Bool, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            icon.image
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(enabled ? DSColor.accent : DSColor.textFaint)
                .frame(width: 36, height: 36)
                .background(DSColor.surface)
                .clipShape(Circle())
                .overlay(Circle().stroke(DSColor.border, lineWidth: 1))
        }
        .buttonStyle(DSPressableStyle())
        .disabled(!enabled)
        .accessibilityLabel(Text(label))
    }

    // ── Строка фильтра / сортировки (§9.0.1) ─────────────────────────────
    private var filterSortRow: some View {
        HStack(spacing: DSSpacing.sm) {
            DSChip(title: "Фильтр", icon: .filter) { sheet = .filter }
            DSChip(title: sortTitle, icon: .sort) { sheet = .sort }
            Spacer(minLength: 0)
            DSIconChip(icon: .bed, accessibilityLabel: "Отдых в ПО") {
                let shown = vm.toggleShowTurnaroundRest()
                ToastCenter.shared.show(shown ? "Отдых в ПО показан" : "Отдых в ПО скрыт")
            }
            DSIconChip(
                icon: vm.ui?.isExpandedView == true ? .filter : .sort,
                accessibilityLabel: "Плотность карточек"
            ) {
                withAnimation(.easeOut(duration: 0.22)) { vm.toggleExpandedView() }
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.bottom, 8)
    }

    private var sortTitle: String {
        switch vm.sortName {
        case "DATE_ASC": return "Дата ↑"
        case "DATE_DESC": return "Дата ↓"
        case "WORKTIME_ASC": return "Часы ↑"
        case "WORKTIME_DESC": return "Часы ↓"
        default: return "Дата ↓"
        }
    }

    // ── Контент (загрузка / ошибка / пусто / список) ─────────────────────
    @ViewBuilder
    private var content: some View {
        if let ui = vm.ui {
            if ui.isLoading && !ui.isReady {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let err = ui.errorMessage, ui.rows.isEmpty {
                VStack(spacing: DSSpacing.lg) {
                    Text("Ошибка: \(err)")
                        .font(DSFont.sans(15))
                        .foregroundColor(DSColor.text)
                        .multilineTextAlignment(.center)
                    DSCTAButton(title: "Повторить") { vm.reload() }
                        .frame(maxWidth: 220)
                }
                .padding(DSMetrics.screenPadX)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if ui.rows.isEmpty {
                DSEmptyState(
                    icon: .calendar,
                    title: "Список пуст",
                    message: "Маршруты появятся здесь, когда вы отметите смену."
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                routeList(ui)
            }
        } else {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private func routeList(_ ui: AllRoutesUi) -> some View {
        ScrollView {
            LazyVStack(spacing: DSSpacing.md) {
                ForEach(Array(chunked(ui.rows).enumerated()), id: \.offset) { _, chunk in
                    chunkView(chunk)
                }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.top, 2)
            .padding(.bottom, ui.isSelectionMode ? 110 : 96)
        }
        .refreshable { vm.pullToRefresh() }
    }

    // ── Чанки: одиночный маршрут или трей «поездка с отдыхом в ПО» ──────
    @ViewBuilder
    private func chunkView(_ chunk: RowChunk) -> some View {
        if chunk.isTrip {
            VStack(alignment: .leading, spacing: 0) {
                tripHeader(chunk.tripTitle)
                ForEach(Array(chunk.rows.enumerated()), id: \.offset) { idx, row in
                    tripRow(row, isLast: idx == chunk.rows.count - 1)
                }
            }
            .background(DSColor.surfaceAlt)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        } else if let row = chunk.rows.first, let card = row.card {
            cardRow(card, rowGroup: row.groupIndex)
        }
    }

    private func tripHeader(_ title: String) -> some View {
        HStack(spacing: DSSpacing.sm) {
            DSIcon.bed.image.font(.system(size: 14, weight: .semibold))
            Text(title)
                .font(DSFont.mono(12, .semibold))
                .tracking(0.6)
                .textCase(.uppercase)
                .foregroundColor(DSColor.text)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.top, DSSpacing.md)
        .padding(.bottom, DSSpacing.sm)
    }

    @ViewBuilder
    private func tripRow(_ row: AllRouteRowUi, isLast: Bool) -> some View {
        switch row.kind {
        case "tripConnector":
            connectorView(row)
        default:
            if let card = row.card {
                cardRow(card, rowGroup: row.groupIndex)
            }
        }
    }

    private func connectorView(_ row: AllRouteRowUi) -> some View {
        HStack(spacing: DSSpacing.sm) {
            Rectangle()
                .fill(DSColor.border)
                .frame(width: 2, height: 22)
                .padding(.leading, DSMetrics.rowPadX)
            HStack(spacing: 6) {
                DSIcon.bed.image.font(.system(size: 12, weight: .medium))
                Text(row.title)
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
                Spacer(minLength: 0)
                Text(row.connectorDurationText)
                    .font(DSFont.mono(13, .semibold))
                    .foregroundColor(DSColor.accent)
            }
            .padding(.vertical, 6)
        }
        .padding(.trailing, DSMetrics.rowPadX)
    }

    // ── Карточка маршрута ───────────────────────────────────────────────
    private func cardRow(_ card: AllRouteCardUi, rowGroup: Int32) -> some View {
        AllRoutesCardRow(
            card: card,
            isExpanded: vm.ui?.isExpandedView == true,
            isSelectionMode: vm.ui?.isSelectionMode == true,
            isSelected: selectedIds.contains(card.basicId),
            onTap: {
                if vm.ui?.isSelectionMode == true {
                    toggleSelection(routeId: card.basicId)
                } else {
                    router.showRouteForm(basicId: card.basicId)
                }
            },
            onLongPress: {
                if vm.ui?.isSelectionMode != true {
                    sheet = .quickView(basicId: card.basicId, isFavorite: card.isFavorite)
                }
            },
            onLegend: { sheet = .legend },
            onDelete: { requestDelete(basicId: card.basicId, dateText: card.startText) }
        )
    }

    // ── Нижняя панель массовых действий (§9.2) ──────────────────────────
    @ViewBuilder
    private var selectionBar: some View {
        if vm.ui?.isSelectionMode == true {
            HStack {
                DSIconButton(
                    icon: vm.ui?.selectedAllFavorite == true ? .heart : .heart,
                    variant: .surface
                ) {
                    vm.toggleFavoriteSelectedRoutes()
                }
                .disabled((vm.ui?.selectedCount ?? 0) == 0)
                Spacer()
                DSIconButton(icon: .share, variant: .surface) { vm.shareSelectedRoutes() }
                    .disabled((vm.ui?.selectedCount ?? 0) == 0)
                Spacer()
                DSIconButton(icon: .trash, variant: .danger) {
                    requestDeleteSelected()
                }
                .disabled((vm.ui?.selectedCount ?? 0) == 0)
            }
            .padding(.horizontal, 40)
            .padding(.vertical, 12)
            .background(
                Capsule()
                    .fill(DSColor.surface)
                    .dsShadow(.floating)
            )
            .padding(.horizontal, 40)
            .padding(.bottom, 12)
        }
    }

    // ── FAB «Добавить маршрут» (§9.0.1) ─────────────────────────────────
    @ViewBuilder
    private var fab: some View {
        if vm.ui?.isSelectionMode != true {
            DSIconButton(icon: .plus, variant: .accent, size: 52) {
                vm.onNewRouteClick { decision in
                    handleFormDecision(decision)
                }
            }
            .padding(.trailing, 20)
            .padding(.bottom, 28)
        }
    }

    // ── Шторки ──────────────────────────────────────────────────────────
    @ViewBuilder
    private func sheetView(_ s: ActiveSheet) -> some View {
        switch s {
        case .month:
            HomeMonthSheet(
                months: vm.months,
                years: vm.years,
                selectedMonth: vm.selectedMonth,
                selectedYear: vm.selectedYear,
                onApply: { year, month in
                    vm.setCurrentMonth(year: year, month: month)
                    sheet = nil
                },
                onClose: { sheet = nil }
            )
        case .filter:
            RoutesFilterSheet(
                selectedFilters: vm.ui?.selectedFilters ?? ["ALL"],
                onToggle: { vm.toggleFilter(name: $0) },
                onClose: { sheet = nil }
            )
        case .sort:
            RoutesSortSheet(
                current: vm.sortName,
                onSelect: { name in
                    vm.setSort(name: name)
                    sheet = nil
                },
                onClose: { sheet = nil }
            )
        case .legend:
            RouteLegendSheet(onClose: { sheet = nil })
        case let .quickView(basicId, isFavorite):
            RouteQuickViewSheet(
                basicId: basicId,
                isFavorite: isFavorite,
                actions: RouteQuickViewActions(
                    onOpen: {
                        sheet = nil
                        router.showRouteForm(basicId: basicId)
                    },
                    onToggleFavorite: {
                        vm.setFavoriteRoute(routeId: basicId)
                    },
                    onShare: {
                        sheet = nil
                        vm.shareRoute(routeId: basicId)
                    },
                    onCopy: {
                        sheet = nil
                        vm.onCopyRoute(routeId: basicId) { decision in
                            handleFormDecision(decision)
                        }
                    },
                    onSync: {
                        sheet = nil
                        vm.syncRoute(routeId: basicId)
                    },
                    onDelete: {
                        sheet = nil
                        let dateText = cardDate(basicId: basicId)
                        requestDelete(basicId: basicId, dateText: dateText)
                    }
                )
            )
        }
    }

    // ── Подтверждения удаления ──────────────────────────────────────────
    private func requestDelete(basicId: String, dateText: String) {
        var message = dateText.isEmpty ? "" : "от \(dateText). "
        if vm.hasRecordedTracks(routeId: basicId) {
            message = "Будут удалены и записанные треки. " + message
        }
        message += "Это действие нельзя отменить."
        deleteRequest = DSDeleteRequest(
            title: "Удалить маршрут?",
            message: message,
            confirmTitle: "Да, удалить"
        ) {
            vm.deleteRoute(routeId: basicId)
        }
    }

    private func requestDeleteSelected() {
        let count = Int(vm.ui?.selectedCount ?? 0)
        deleteRequest = DSDeleteRequest(
            title: count == 1 ? "Удалить маршрут?" : "Удалить маршруты?",
            message: "Выбрано: \(count). Это действие нельзя отменить.",
            confirmTitle: "Да, удалить"
        ) {
            vm.deleteSelectedRoutes()
        }
    }

    private func cardDate(basicId: String) -> String {
        for row in vm.ui?.rows ?? [] {
            if let card = row.card, card.basicId == basicId {
                return card.startText
            }
        }
        return ""
    }

    // ── Выделение (локальное зеркало) ──────────────────────────────────
    private func toggleSelection(routeId: String) {
        vm.toggleRouteSelection(routeId: routeId)
        if selectedIds.contains(routeId) {
            selectedIds.remove(routeId)
        } else {
            selectedIds.insert(routeId)
        }
    }

    private func doToggleSelectAll() {
        let wasAllSelected = vm.ui?.allVisibleSelected == true
        vm.toggleSelectAll()
        if wasAllSelected {
            selectedIds.removeAll()
        } else {
            selectedIds = visibleCardIds()
        }
    }

    private func exitSelection() {
        vm.exitSelectionMode()
        selectedIds.removeAll()
    }

    private func visibleCardIds() -> Set<String> {
        var ids: Set<String> = []
        for row in vm.ui?.rows ?? [] {
            if let card = row.card { ids.insert(card.basicId) }
        }
        return ids
    }

    // ── Решение по открытию формы (§9.0.1 FAB / копия) ─────────────────
    private func handleFormDecision(_ decision: AllRoutesFormDecision) {
        switch decision.type {
        case "open":
            if let id = decision.basicId {
                router.showRouteForm(basicId: decision.isMakeCopy ? nil : id, isMakeCopy: decision.isMakeCopy)
            } else {
                router.openNewRouteForm()
            }
        case "limit":
            newRouteGate = NewRouteGateAlert(
                title: "Нужна подписка",
                message: "Бесплатных маршрутов больше нет.",
                isAuthorized: decision.isAuthorized
            )
        case "trial":
            newRouteGate = NewRouteGateAlert(
                title: "Пробный период",
                message: "Оформите подписку, чтобы продолжить без ограничений.",
                isAuthorized: decision.isAuthorized,
                onContinue: { router.openNewRouteForm() }
            )
        default:
            break
        }
    }

    private var newRouteGateHost: some View {
        Color.clear
            .alert(
                newRouteGate?.title ?? "",
                isPresented: Binding(
                    get: { newRouteGate != nil },
                    set: { if !$0 { newRouteGate = nil } }
                ),
                presenting: newRouteGate
            ) { gate in
                if let onContinue = gate.onContinue {
                    Button("Продолжить бесплатно") {
                        newRouteGate = nil
                        onContinue()
                    }
                }
                Button("Оформить подписку за 69 руб/мес") {
                    let auth = gate.isAuthorized
                    newRouteGate = nil
                    router.showPurchasesFromNewRouteGate(isAuthorized: auth)
                }
                Button("Отмена", role: .cancel) { newRouteGate = nil }
            } message: { gate in
                Text(gate.message)
            }
    }
}

// MARK: - Модель чанка списка

private struct RowChunk: Identifiable {
    let id = UUID()
    let isTrip: Bool
    let tripTitle: String
    let rows: [AllRouteRowUi]
}

/// Группирует строки: одиночные маршруты и треи (группы с общим `groupIndex` ≥ 0).
private func chunked(_ rows: [AllRouteRowUi]) -> [RowChunk] {
    var result: [RowChunk] = []
    var idx = 0
    while idx < rows.count {
        let row = rows[idx]
        if row.kind == "tripHeader" {
            let group = row.groupIndex
            var items: [AllRouteRowUi] = []
            var j = idx + 1
            while j < rows.count, rows[j].groupIndex == group, rows[j].kind != "tripHeader" {
                items.append(rows[j])
                j += 1
            }
            result.append(RowChunk(isTrip: true, tripTitle: row.title, rows: items))
            idx = j
        } else {
            result.append(RowChunk(isTrip: false, tripTitle: "", rows: [row]))
            idx += 1
        }
    }
    return result
}

// MARK: - Карточка маршрута (§9.0.1 / §4.5)

/// Карточка маршрута списка «Все маршруты». Макет — как `HomeRouteCardRow`,
/// плюс развёрнутое тело и режим выбора. Жесты: тап → форма/выбор;
/// долгое нажатие → быстрый просмотр; тап по значкам → легенда; свайп → удаление.
struct AllRoutesCardRow: View {
    let card: AllRouteCardUi
    let isExpanded: Bool
    let isSelectionMode: Bool
    let isSelected: Bool
    let onTap: () -> Void
    let onLongPress: () -> Void
    let onLegend: () -> Void
    let onDelete: () -> Void

    @State private var revealed = false
    @GestureState private var dragX: CGFloat = 0

    private let deleteWidth: CGFloat = 88

    private var rowOffset: CGFloat {
        let base: CGFloat = revealed ? -deleteWidth : 0
        return min(0, max(-deleteWidth * 1.3, base + dragX))
    }

    var body: some View {
        ZStack(alignment: .trailing) {
            if !isSelectionMode {
                Button {
                    withAnimation(.easeOut(duration: 0.2)) { revealed = false }
                    onDelete()
                } label: {
                    DSIcon.trash.image
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(DSColor.accentInk)
                        .frame(width: deleteWidth)
                        .frame(maxHeight: .infinity)
                        .background(DSColor.danger)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("Удалить маршрут"))
                .opacity(rowOffset < 0 ? 1 : 0)
            }

            content
                .background(background)
                .overlay(selectionOverlay)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
                .offset(x: rowOffset)
                .simultaneousGesture(isSelectionMode ? nil : swipe)
        }
        .clipped()
    }

    private var selectionOverlay: some View {
        RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous)
            .stroke(isSelected ? DSColor.accent : Color.clear, lineWidth: 2)
    }

    private var background: Color {
        if card.isFuture { return DSColor.surfaceAlt }
        if card.isTransition { return DSColor.bgSubtle }
        return DSColor.surface
    }

    private var content: some View {
        HStack(alignment: .top, spacing: DSSpacing.md) {
            if isSelectionMode {
                selectionMarker
                    .padding(.top, 2)
            }
            VStack(alignment: .leading, spacing: 6) {
                header
                if !card.summary.isEmpty {
                    Text(card.summary)
                        .font(DSFont.sans(15))
                        .foregroundColor(DSColor.text)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
                if isExpanded {
                    expandedBody
                }
                HStack(alignment: .center, spacing: DSSpacing.md) {
                    Text(card.numberText)
                        .font(DSFont.mono(13))
                        .foregroundColor(DSColor.textMuted)
                    Spacer(minLength: DSSpacing.sm)
                    icons
                }
            }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, DSMetrics.rowPadY)
        .frame(maxWidth: .infinity, minHeight: 65, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture {
            if revealed {
                withAnimation(.easeOut(duration: 0.2)) { revealed = false }
            } else {
                onTap()
            }
        }
        .onLongPressGesture(minimumDuration: 0.5) {
            if !revealed { onLongPress() }
        }
    }

    private var selectionMarker: some View {
        ZStack {
            Circle()
                .stroke(isSelected ? DSColor.accent : DSColor.borderStrong, lineWidth: 2)
                .frame(width: 22, height: 22)
            if isSelected {
                Circle()
                    .fill(DSColor.accent)
                    .frame(width: 22, height: 22)
                DSIcon.check.image
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(DSColor.accentInk)
            }
        }
    }

    private var header: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: DSSpacing.md) {
                Text(card.endText.isEmpty ? card.startText : "\(card.startText) — \(card.endText)")
                    .lineLimit(1)
                    .fixedSize()
                Spacer(minLength: DSSpacing.sm)
                Text(card.durationText).fixedSize()
            }
            HStack(alignment: .center, spacing: DSSpacing.md) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(card.startText)
                    if !card.endTextWithDate.isEmpty { Text(card.endTextWithDate) }
                }
                .lineLimit(1)
                .minimumScaleFactor(10.0 / 17.0)
                Spacer(minLength: DSSpacing.sm)
                Text(card.durationText).lineLimit(1).fixedSize()
            }
        }
        .font(DSFont.mono(17, .semibold))
        .foregroundColor(DSColor.text)
    }

    private var expandedBody: some View {
        VStack(alignment: .leading, spacing: 8) {
            DSDivider(inset: 0)
            if !card.locoLines.isEmpty {
                groupBlock("Локомотивы", icon: .locomotive, lines: card.locoLines)
            }
            if !card.trainLines.isEmpty {
                groupBlock("Поезда", icon: .train, lines: card.trainLines)
            }
            if !card.passengerLines.isEmpty {
                groupBlock("Пассажиром", icon: .passenger, lines: card.passengerLines)
            }
            if !card.otherWorkLines.isEmpty {
                groupBlock("Прочая работа", icon: .wrench, lines: card.otherWorkLines)
            }
            if !card.notesText.isEmpty {
                Text(card.notesText)
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.textMuted)
                    .lineLimit(2)
            }
            if !card.paymentText.isEmpty {
                HStack {
                    DSIcon.ruble.image.font(.system(size: 13, weight: .medium))
                    Text("Расчёт за смену")
                        .font(DSFont.sans(12))
                    Spacer()
                    Text(card.paymentText)
                        .font(DSFont.mono(14, .semibold))
                        .foregroundColor(DSColor.text)
                }
                .foregroundColor(DSColor.textMuted)
                .padding(10)
                .background(DSColor.bgSubtle)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
            }
        }
    }

    private func groupBlock(_ label: String, icon: DSIcon, lines: [String]) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(DSFont.mono(10))
                .tracking(1.2)
                .textCase(.uppercase)
                .foregroundColor(DSColor.textFaint)
            ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                HStack(spacing: DSSpacing.sm) {
                    DSIconAvatar(icon: icon, size: .sm, tone: .accent, shape: .square)
                    Text(line)
                        .font(DSFont.mono(13))
                        .foregroundColor(DSColor.text)
                        .lineLimit(1)
                    Spacer(minLength: 0)
                }
            }
        }
    }

    private var icons: some View {
        HStack(spacing: 10) {
            ForEach(card.icons, id: \.self) { key in
                HomeRouteAttributeIcon(key: key)
            }
            if card.isFavorite {
                Image(systemName: "heart.fill")
                    .font(.system(size: 15))
                    .foregroundColor(DSColor.danger)
            }
            DSIcon.sync.image
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(card.isSynchronized ? DSColor.success : DSColor.textFaint)
                .accessibilityLabel(Text(card.isSynchronized ? "Синхронизирован" : "Не синхронизирован"))
        }
        .contentShape(Rectangle())
        .onTapGesture { onLegend() }
    }

    private var swipe: some Gesture {
        DragGesture(minimumDistance: 20)
            .updating($dragX) { value, state, _ in
                if abs(value.translation.width) > abs(value.translation.height) * 1.5 {
                    state = value.translation.width
                }
            }
            .onEnded { value in
                guard abs(value.translation.width) > abs(value.translation.height) * 1.5 else { return }
                withAnimation(.easeOut(duration: 0.2)) {
                    let final = (revealed ? -deleteWidth : 0) + value.translation.width
                    revealed = final < -deleteWidth / 2
                }
            }
    }
}

// MARK: - Алерт «Нужна подписка / Пробный период»

private struct NewRouteGateAlert: Identifiable {
    let id = UUID()
    let title: String
    let message: String
    let isAuthorized: Bool
    var onContinue: (() -> Void)? = nil
}
