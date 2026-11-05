import SwiftUI
import ComposeApp

// Быстрый просмотр маршрута — SCREEN_SPECS §9.1.
// Макет: design/src/route-quick-view.jsx → IOSRouteQuickView.
// Данные — Kotlin `RouteQuickViewIosViewModel` (только чтение).
// Действия нижней панели выполняет вызывающий экран (замыкания `RouteQuickViewActions`).

/// Действия нижней панели быстрого просмотра (§9.1).
struct RouteQuickViewActions {
    var onOpen: () -> Void
    var onToggleFavorite: () -> Void
    var onShare: () -> Void
    var onCopy: () -> Void
    var onSync: () -> Void
    var onDelete: () -> Void
}

struct RouteQuickViewSheet: View {
    let basicId: String
    let isFavorite: Bool
    let actions: RouteQuickViewActions

    @StateObject private var vm = RouteQuickViewViewModelWrapper()
    /// Локальный оптимистичный флаг избранного (§9.1).
    @State private var favorite: Bool

    init(basicId: String, isFavorite: Bool, actions: RouteQuickViewActions) {
        self.basicId = basicId
        self.isFavorite = isFavorite
        self.actions = actions
        _favorite = State(initialValue: isFavorite)
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if let ui = vm.ui, ui.isReady {
                        body(ui)
                    } else {
                        ProgressView()
                            .frame(maxWidth: .infinity, minHeight: 160)
                    }
                }
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.bottom, 24)
            }
            bottomBar
        }
        .background(DSColor.bg.ignoresSafeArea())
        .onAppear { vm.show(routeId: basicId) }
    }

    // ── Шапка (§9.1) ───────────────────────────────────────────────────
    private var header: some View {
        VStack(alignment: .leading, spacing: DSSpacing.sm) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text("Маршрут")
                    .font(DSFont.sans(16, .medium))
                    .foregroundColor(DSColor.text)
                Text(vm.ui?.routeNumberText ?? "№б/н")
                    .font(DSFont.mono(18, .semibold))
                    .foregroundColor(DSColor.text)
                Spacer(minLength: 0)
            }
            if let dir = vm.ui?.directionText, !dir.isEmpty {
                Text(dir)
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.textMuted)
                    .lineLimit(1)
            }
            HStack(spacing: DSSpacing.sm) {
                headerPill("Время работы", vm.ui?.workTimeText ?? "")
                headerPill("Заработано", vm.ui?.paymentText ?? "—")
            }
            DSPill(
                text: vm.ui?.isSynchronized == true ? "Синхронизирован" : "Не синхронизирован",
                tone: vm.ui?.isSynchronized == true ? .success : .muted,
                isMono: false
            )
        }
        .padding(DSMetrics.screenPadX)
        .background(DSColor.accentSoft)
    }

    private func headerPill(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(DSFont.sans(11))
                .foregroundColor(DSColor.textMuted)
            Text(value)
                .font(DSFont.mono(15, .semibold))
                .foregroundColor(DSColor.text)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, DSSpacing.md)
        .padding(.vertical, DSSpacing.sm)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
    }

    // ── Контент ────────────────────────────────────────────────────────
    @ViewBuilder
    private func body(_ ui: RouteQuickViewUi) -> some View {
        // Время работы
        if !ui.workRows.isEmpty {
            sectionCard("Время работы") {
                labelValueRows(ui.workRows)
            }
        }
        // Отдыхи
        ForEach(Array(ui.restSections.enumerated()), id: \.offset) { _, rest in
            sectionCard(rest.title) {
                labelValueRows(rest.rows)
            }
        }
        // Локомотивы
        ForEach(Array(ui.locomotives.enumerated()), id: \.offset) { _, loco in
            sectionCard("Локомотивы · \(ui.locomotives.count)") {
                locoBody(loco)
            }
        }
        // Поезда
        ForEach(Array(ui.trains.enumerated()), id: \.offset) { _, train in
            sectionCard(train.title) {
                trainBody(train)
            }
        }
        // Пассажиром
        if !ui.passengers.isEmpty {
            sectionCard("Следование пассажиром") {
                ForEach(Array(ui.passengers.enumerated()), id: \.offset) { _, p in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(p.title).font(DSFont.sans(14, .semibold)).foregroundColor(DSColor.text)
                        Text(p.routeText).font(DSFont.sans(13)).foregroundColor(DSColor.textMuted)
                        if !p.timeText.isEmpty {
                            Text(p.timeText).font(DSFont.mono(12)).foregroundColor(DSColor.textMuted)
                        }
                        if !p.notesText.isEmpty {
                            Text(p.notesText).font(DSFont.sans(12)).foregroundColor(DSColor.textFaint)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        // Доплаты
        if !ui.surcharges.isEmpty {
            sectionCard("Доплаты") {
                ForEach(Array(ui.surcharges.enumerated()), id: \.offset) { _, s in
                    Text(s).font(DSFont.sans(13)).foregroundColor(DSColor.text)
                        .padding(.vertical, 2)
                }
            }
        }
        // Заметки
        if !ui.notesText.isEmpty {
            sectionCard("Заметки") {
                Text(ui.notesText)
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.text)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private func sectionCard<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        let inner = content()
        return VStack(alignment: .leading, spacing: DSSpacing.sm) {
            Text(title)
                .font(DSFont.mono(11))
                .tracking(1.2)
                .textCase(.uppercase)
                .foregroundColor(DSColor.textMuted)
            DSCard {
                VStack(alignment: .leading, spacing: 6) {
                    inner
                }
                .padding(DSMetrics.rowPadX)
            }
        }
        .padding(.top, DSSpacing.lg)
    }

    private func labelValueRows(_ rows: [QuickLabelValueUi]) -> some View {
        ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
            HStack {
                Text(row.label)
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.textMuted)
                Spacer(minLength: DSSpacing.md)
                Text(row.value)
                    .font(DSFont.mono(14, .medium))
                    .foregroundColor(DSColor.text)
            }
            .padding(.vertical, 3)
        }
    }

    private func locoBody(_ loco: QuickLocoUi) -> some View {
        VStack(alignment: .leading, spacing: DSSpacing.sm) {
            Text(loco.title)
                .font(DSFont.mono(15, .medium))
                .foregroundColor(DSColor.text)
            if let acc = loco.acceptStops {
                stopsBlock(acc)
            }
            if let del = loco.deliveryStops {
                stopsBlock(del)
            }
            ForEach(Array(loco.sections.enumerated()), id: \.offset) { _, sec in
                if !sec.title.isEmpty {
                    Text(sec.title)
                        .font(DSFont.sans(13, .semibold))
                        .foregroundColor(DSColor.text)
                }
                ForEach(Array(sec.consumptions.enumerated()), id: \.offset) { _, cons in
                    VStack(alignment: .leading, spacing: 2) {
                        HStack {
                            Text(cons.title).font(DSFont.sans(12)).foregroundColor(DSColor.textMuted)
                            Spacer()
                            Text(cons.totalText)
                                .font(DSFont.mono(13, .semibold))
                                .foregroundColor(cons.isRecovery ? DSColor.success : DSColor.text)
                        }
                        if !cons.acceptedText.isEmpty || !cons.deliveryText.isEmpty {
                            HStack {
                                Text(cons.acceptedText).font(DSFont.mono(11)).foregroundColor(DSColor.textMuted)
                                Spacer()
                                Text(cons.deliveryText).font(DSFont.mono(11)).foregroundColor(DSColor.textMuted)
                            }
                        }
                    }
                    .padding(.vertical, 2)
                }
                if !sec.supplyText.isEmpty {
                    Text(sec.supplyText)
                        .font(DSFont.sans(12))
                        .foregroundColor(DSColor.textMuted)
                }
            }
        }
    }

    private func stopsBlock(_ stops: QuickTimeStopsUi) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(stops.title)
                .font(DSFont.sans(12, .semibold))
                .foregroundColor(DSColor.textMuted)
            ForEach(Array(stops.stops.enumerated()), id: \.offset) { _, s in
                HStack {
                    Text(s.label).font(DSFont.sans(12)).foregroundColor(DSColor.textMuted)
                    Spacer()
                    Text(s.value).font(DSFont.mono(12)).foregroundColor(DSColor.text)
                }
            }
        }
    }

    private func trainBody(_ train: QuickTrainUi) -> some View {
        VStack(alignment: .leading, spacing: DSSpacing.sm) {
            if !train.characteristicsText.isEmpty {
                Text(train.characteristicsText)
                    .font(DSFont.mono(12))
                    .foregroundColor(DSColor.textMuted)
            }
            if !train.carInspectorText.isEmpty {
                Text(train.carInspectorText)
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
            }
            // Вертикальный график станций
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(train.stations.enumerated()), id: \.offset) { idx, st in
                    if !st.segmentLine.isEmpty {
                        Text(st.segmentLine)
                            .font(DSFont.sans(11))
                            .foregroundColor(DSColor.textFaint)
                            .padding(.leading, 14)
                            .padding(.vertical, 2)
                    }
                    stationRow(st)
                    if idx < train.stations.count - 1 {
                        Rectangle()
                            .fill(DSColor.border)
                            .frame(width: 2, height: 12)
                            .padding(.leading, 5)
                    }
                }
            }
            ForEach(Array(train.assistLines.enumerated()), id: \.offset) { _, line in
                Text(line)
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
            }
        }
    }

    private func stationRow(_ st: QuickStationUi) -> some View {
        HStack(alignment: .top, spacing: DSSpacing.sm) {
            Circle()
                .fill(badgeColor(st.badge))
                .frame(width: 10, height: 10)
                .padding(.top, 4)
            VStack(alignment: .leading, spacing: 1) {
                Text(st.nameText)
                    .font(DSFont.sans(14, .medium))
                    .foregroundColor(st.isPassing ? DSColor.textMuted : DSColor.text)
                Text(st.timeText)
                    .font(DSFont.mono(12))
                    .foregroundColor(DSColor.accent)
            }
            Spacer(minLength: 0)
        }
    }

    private func badgeColor(_ badge: String) -> Color {
        switch badge {
        case "danger": return DSColor.danger
        case "warning": return DSColor.warning
        default: return DSColor.borderStrong
        }
    }

    // ── Нижняя панель действий (§9.1) ──────────────────────────────────
    private var bottomBar: some View {
        HStack(spacing: DSSpacing.md) {
            actionButton("Открыть", icon: .document) { actions.onOpen() }
            actionButton(favorite ? "В избранном" : "В избранное", icon: .heart, tint: favorite ? DSColor.danger : nil) {
                favorite.toggle()
                actions.onToggleFavorite()
            }
            actionButton("Поделиться", icon: .share) { actions.onShare() }
            actionButton("Копировать", icon: .copy) { actions.onCopy() }
            if vm.ui?.isSynchronized != true {
                actionButton("Синхронизировать", icon: .sync) { actions.onSync() }
            }
            Button {
                actions.onDelete()
            } label: {
                VStack(spacing: 3) {
                    DSIcon.trash.image.font(.system(size: 18, weight: .medium))
                    Text("Удалить").font(DSFont.sans(10))
                }
                .foregroundColor(DSColor.danger)
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, DSSpacing.sm)
        .padding(.vertical, DSSpacing.md)
        .background(DSColor.surface)
        .overlay(alignment: .top) { DSDivider(inset: 0) }
    }

    private func actionButton(_ title: String, icon: DSIcon, tint: Color? = nil, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 3) {
                icon.image.font(.system(size: 18, weight: .medium))
                Text(title)
                    .font(DSFont.sans(10))
                    .lineLimit(1)
            }
            .foregroundColor(tint ?? DSColor.text)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}
