import SwiftUI
import ComposeApp

// Экран «Поезд» — SCREEN_SPECS §6.
// Макет: design/src/ios-add-train.jsx → IOSScreenAddTrain; station-edit-sheet.jsx → StationEditSheetWrap(ios).
// Логика и расчёты — Kotlin `TrainScreenIosViewModel` (аналог Android TrainFormViewModel).
// По спеке (макет противоречит): «Готово» слева и ⚙ справа в топ-баре, автосейв без кнопки «Сохранить»,
// станции добавляются кнопкой «+» на нижней панели.

/// Поле с локальным текстом: внешнее значение подхватывается, только пока поле не в фокусе
/// (иначе устаревший снимок состояния затирал бы быстрый ввод).
private struct TrainInputField: View {
    let value: String
    let placeholder: String
    var keyboard: UIKeyboardType = .default
    var prefix: String? = nil
    var suffix: String? = nil
    let onSet: (String) -> Void

    @State private var local = ""
    @FocusState private var focused: Bool

    var body: some View {
        HStack(spacing: 4) {
            if let prefix = prefix, !local.isEmpty { Text(prefix).font(DSFont.mono(20, .semibold)).foregroundColor(DSColor.text) }
            TextField(placeholder, text: $local)
                .focused($focused)
                .keyboardType(keyboard)
                .font(DSFont.mono(20, .semibold))
                .foregroundColor(DSColor.text)
                .autocorrectionDisabled()
            if let suffix = suffix, !local.isEmpty { Text(suffix).font(DSFont.sans(13)).foregroundColor(DSColor.textMuted) }
        }
        .onAppear { local = value }
        .onChange(of: value) { v in if !focused && v != local { local = v } }
        .onChange(of: local) { v in if v != value { onSet(v) } }
        .onChange(of: focused) { f in if !f { local = value } }
    }
}

private enum TrainSheet: Identifiable {
    case station(index: Int, newId: String?)
    case segment(after: Int)
    case phases, assists, inspector, history

    var id: String {
        switch self {
        case let .station(index, newId): return "station-\(index)-\(newId ?? "")"
        case let .segment(after): return "segment-\(after)"
        case .phases: return "phases"
        case .assists: return "assists"
        case .inspector: return "inspector"
        case .history: return "history"
        }
    }
}

struct FormTrainView: View {
    let routeId: String
    let trainId: String?

    @StateObject private var vm = TrainScreenViewModelWrapper()
    @State private var started = false
    @State private var sheet: TrainSheet?
    @State private var deleteRequest: DSDeleteRequest?
    @State private var snackbars: [DSSnackbarItem] = []
    @State private var showInfo = false
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        VStack(spacing: 0) {
            navBar
            if let state = vm.state, !state.isLoading {
                content(state)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .onAppear {
            if !started {
                started = true
                vm.start(basicId: routeId, trainId: trainId)
            }
        }
        .onChange(of: scenePhase) { phase in if phase != .active { vm.flush() } }
        .dsDeleteAlert($deleteRequest)
        .dsSnackbarQueue(snackbars, bottomPadding: 110) { if !snackbars.isEmpty { snackbars.removeFirst() } }
        .alert("Не удалось сохранить", isPresented: Binding(get: { vm.state?.saveError != nil }, set: { if !$0 { vm.clearSaveError() } })) {
            Button("OK", role: .cancel) { vm.clearSaveError() }
        } message: {
            Text(vm.state?.saveError ?? "")
        }
        .sheet(item: $sheet) { item in sheetContent(item) }
    }

    // MARK: Навбар

    private var navBar: some View {
        ZStack {
            Text("Поезд").dsTextStyle(.navTitle).lineLimit(1)
            HStack {
                DSNavPillButton(title: "Готово") {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    vm.flush()
                    AppRouter.shared.back()
                }
                Spacer()
                DSNavPillButton(icon: .gear, accessibilityLabel: "Настройки поезда") {
                    AppRouter.shared.showSettingsSection(.train)
                }
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.vertical, 12)
        .background(DSColor.bg)
    }

    // MARK: Шторки

    @ViewBuilder
    private func sheetContent(_ item: TrainSheet) -> some View {
        if let s = vm.state {
            sheetBody(item, s)
        }
    }

    @ViewBuilder
    private func sheetBody(_ item: TrainSheet, _ s: TrainScreenState) -> some View {
        switch item {
        case let .station(index, newId):
            TrainStationSheet(vm: vm, state0: s, index: index, newStationId: newId)
                .presentationDetents([.large])
        case let .segment(after):
            TrainSegmentSheet(vm: vm, state0: s, afterIndex: after)
                .presentationDetents([.large])
        case .phases:
            TrainPhasesSheet(vm: vm, state0: s)
                .presentationDetents([.large])
        case .assists:
            TrainAssistsSheet(vm: vm, state0: s)
                .presentationDetents([.large])
        case .inspector:
            TrainInspectorSheet(vm: vm, state0: s)
                .presentationDetents([.medium, .large])
        case .history:
            TrainHistorySheet(vm: vm, state0: s)
                .presentationDetents([.large])
        }
    }

    // MARK: Содержимое

    private func content(_ s: TrainScreenState) -> some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if let error = s.errorMessage {
                        Text(error)
                            .font(DSFont.sans(14, .medium)).foregroundColor(DSColor.danger)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(DSColor.danger.opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
                            .padding(.bottom, 10)
                    }
                    additionalNumbers(s)
                    numberCard(s)
                    if showInfo, let category = s.categoryText, !s.number.isEmpty {
                        Text(category).font(DSFont.sans(14)).foregroundColor(DSColor.text)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 14).padding(.vertical, 12)
                            .background(DSColor.bgSubtle)
                            .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
                            .padding(.top, 8)
                    }
                    characteristicsCard(s)
                    if let versions = s.versionsText {
                        DSTextAction(title: versions) { sheet = .history }.padding(.top, 8).padding(.horizontal, 4)
                    }
                    phaseCard(s)
                    summaries(s)
                    controlsRow(s)

                    DSGroupHeader(title: "МАРШРУТ") { routeButtons(s) }
                    TrainStationListView(
                        state: s,
                        onStation: { sheet = .station(index: $0, newId: nil) },
                        onSegment: { sheet = .segment(after: $0) },
                        onMove: { vm.moveStation(from: $0, to: $1) },
                        onDelete: { index in
                            let stations = (s.stations as? [TrainStationUi]) ?? []
                            let name = stations.indices.contains(index) ? stations[index].name : ""
                            deleteRequest = DSDeleteRequest(title: name.isEmpty ? "Удалить станцию?" : "Удалить станцию \(name)?") {
                                vm.deleteStation(index)
                            }
                        }
                    )
                    TrainSummaryPills(state: s)
                    Spacer(minLength: 110)
                }
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.top, 4)
            }
            .trainScrollDismissesKeyboard()
            .toolbar {
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Готово") {
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    }
                }
            }
            bottomBar(s)
        }
    }

    // MARK: Номера

    @ViewBuilder
    private func additionalNumbers(_ s: TrainScreenState) -> some View {
        let numbers = (s.additionalNumbers as? [String]) ?? []
        if !numbers.isEmpty {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 96), spacing: 8, alignment: .trailing)], alignment: .trailing, spacing: 8) {
                ForEach(Array(numbers.enumerated()), id: \.offset) { index, number in
                    HStack(spacing: 6) {
                        Text("№ \(number)").font(DSFont.mono(14, .semibold)).foregroundColor(DSColor.accent)
                        Button { vm.removeAdditionalNumber(index) } label: {
                            DSIcon.close.image.font(.system(size: 10, weight: .bold)).foregroundColor(DSColor.accent)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(Text("Удалить номер \(number)"))
                    }
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(DSColor.accentSoft)
                    .clipShape(Capsule())
                }
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
            .padding(.bottom, 8)
        }
    }

    /// «Расстояние | Номер (i) | +» (§6.2 п.2).
    private func numberCard(_ s: TrainScreenState) -> some View {
        let canAdd = !s.number.trimmingCharacters(in: .whitespaces).isEmpty
        return HStack(spacing: 10) {
            DSCard {
                HStack(spacing: 0) {
                    cell(title: "Расстояние") {
                        TrainInputField(value: s.distance == "0" ? "" : s.distance, placeholder: "0", keyboard: .decimalPad, suffix: "км") { vm.setDistance($0) }
                    }
                    Rectangle().fill(DSColor.border).frame(width: 1)
                    cell(title: "Номер") {
                        HStack {
                            TrainInputField(value: s.number, placeholder: "№", keyboard: .decimalPad, prefix: "№ ") { vm.setNumber($0) }
                            if !s.number.isEmpty {
                                Button { showInfo.toggle() } label: {
                                    DSIcon.info.image.font(.system(size: 17)).foregroundColor(DSColor.accent)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel(Text("Категория поезда"))
                            }
                        }
                    }
                }
            }
            Button { vm.addCurrentNumberToAdditional() } label: {
                DSIcon.plus.image.font(.system(size: 20, weight: .semibold))
                    .foregroundColor(canAdd ? DSColor.accentInk : DSColor.textFaint)
                    .frame(width: 60, height: 60)
                    .background(canAdd ? DSColor.accent : DSColor.bgSubtle)
                    .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
            }
            .buttonStyle(DSPressableStyle())
            .disabled(!canAdd)
            .accessibilityLabel(Text("Добавить дополнительный номер"))
        }
    }

    private func cell<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            content()
            Text(title).dsTextStyle(.captionMuted)
        }
        .padding(.horizontal, 14).padding(.vertical, 10)
        .frame(maxWidth: .infinity, minHeight: 60, alignment: .leading)
    }

    /// «Вес | Оси | У.Д.» (§6.2 п.3).
    private func characteristicsCard(_ s: TrainScreenState) -> some View {
        DSCard {
            HStack(spacing: 0) {
                cell(title: "Вес, т") {
                    TrainInputField(value: s.weight, placeholder: "—", keyboard: .numberPad) { vm.setWeight($0) }
                }
                Rectangle().fill(DSColor.border).frame(width: 1)
                cell(title: "Оси") {
                    TrainInputField(value: s.axle, placeholder: "—", keyboard: .numberPad) { vm.setAxle($0) }
                }
                Rectangle().fill(DSColor.border).frame(width: 1)
                cell(title: s.lengthMetersText.map { "У.Д. \($0)" } ?? "У.Д.") {
                    TrainInputField(value: s.conditionalLength, placeholder: "—", keyboard: .decimalPad) { vm.setLength($0) }
                }
            }
        }
        .padding(.top, 10)
    }

    // MARK: Плечо

    private func phaseCard(_ s: TrainScreenState) -> some View {
        DSCard {
            HStack(spacing: 12) {
                Button { sheet = .phases } label: {
                    HStack(spacing: 12) {
                        DSIconAvatar(icon: .train, size: .lg, tone: .accent)
                        if s.hasPhase {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Плечо").dsTextStyle(.captionMuted)
                                Text(s.phaseTitle).font(DSFont.sans(16)).foregroundColor(DSColor.text).lineLimit(1)
                            }
                            Spacer(minLength: 6)
                            Text(s.phaseDistanceText).font(DSFont.mono(13, .medium)).foregroundColor(DSColor.textMuted)
                        } else {
                            Text("Выбрать плечо").font(DSFont.sans(16)).foregroundColor(DSColor.textMuted)
                            Spacer()
                        }
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                if s.hasPhase {
                    Button { vm.selectPhase(nil) } label: {
                        DSIcon.close.image.font(.system(size: 11, weight: .bold)).foregroundColor(DSColor.textMuted)
                            .frame(width: 24, height: 24).background(DSColor.bgSubtle).clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text("Снять плечо"))
                }
            }
            .padding(.horizontal, 14).padding(.vertical, 10)
            .frame(minHeight: 56)
        }
        .padding(.top, 10)
    }

    // MARK: Сводки тяги и вагонника

    @ViewBuilder
    private func summaries(_ s: TrainScreenState) -> some View {
        let assist = (s.assistSummary as? [TrainSummaryRowUi]) ?? []
        if !assist.isEmpty {
            summaryBlock(assist) { sheet = .assists }
        }
        if let inspector = s.carInspectorSummary {
            summaryBlock([inspector]) { sheet = .inspector }
        }
    }

    private func summaryBlock(_ rows: [TrainSummaryRowUi], action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(row.title).dsTextStyle(.captionMuted)
                        if !row.details.isEmpty {
                            Text(row.details).font(DSFont.sans(15)).foregroundColor(DSColor.text)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(DSColor.accentSoft)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        }
        .buttonStyle(.plain)
        .padding(.top, 10)
    }

    // MARK: Секундомер и GO

    private func controlsRow(_ s: TrainScreenState) -> some View {
        // Слот «Автозапись» слева — только Android (§6.2 п.6а); секундомер по центру, GO справа.
        ZStack {
            TrainStopwatchChip(vm: vm)
            HStack {
                Spacer()
                TrainGoButton(isDeparture: s.goIsDeparture) { vm.go() }
            }
        }
        .padding(.top, 14)
    }

    // MARK: Заголовок «МАРШРУТ»: сортировка и перегоны

    private func routeButtons(_ s: TrainScreenState) -> some View {
        HStack(spacing: 10) {
            circle(system: "arrow.up.arrow.down", label: "Порядок станций") { vm.toggleSortOrder() }
                .rotationEffect(.degrees(s.isReversed ? 180 : 0))
                .animation(.easeInOut(duration: 0.3), value: s.isReversed)
            circle(system: s.showSegments ? "rectangle.compress.vertical" : "rectangle.expand.vertical",
                   label: s.showSegments ? "Скрыть перегоны" : "Показать перегоны") {
                let shown = vm.toggleSegmentsVisibility()
                snackbars.append(DSSnackbarItem(text: shown ? "Перегоны показаны" : "Перегоны скрыты"))
            }
        }
    }

    private func circle(system: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: system).font(.system(size: 14, weight: .medium)).foregroundColor(DSColor.text)
                .frame(width: 36, height: 36)
                .background(DSColor.accent.opacity(0.14))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(label))
    }

    // MARK: Нижняя панель

    private func bottomBar(_ s: TrainScreenState) -> some View {
        HStack(spacing: 0) {
            barButton(system: "link", label: "Вагонник", dot: s.inspectorHasData) {
                vm.addCarInspector()
                sheet = .inspector
            }
            barButton(system: "slider.horizontal.3", label: "Параметры поезда", dot: s.assistHasData) { sheet = .assists }
            Button { sheet = .station(index: -1, newId: vm.makeStationId()) } label: {
                DSIcon.plus.image.font(.system(size: 24, weight: .semibold)).foregroundColor(DSColor.accentInk)
                    .frame(width: 56, height: 56).background(DSColor.accent).clipShape(Circle())
                    .dsShadow(.floating)
            }
            .buttonStyle(DSPressableStyle())
            .frame(maxWidth: .infinity)
            .accessibilityLabel(Text("Добавить станцию"))
        }
        .padding(.top, 8)
        .padding(.bottom, 12)
        .background(DSColor.surface.ignoresSafeArea(edges: .bottom))
        .overlay(alignment: .top) { DSDivider(inset: 0) }
    }

    private func barButton(system: String, label: String, dot: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: system).font(.system(size: 20)).foregroundColor(DSColor.text)
                .frame(width: 48, height: 48)
                .overlay(alignment: .topTrailing) {
                    if dot { Circle().fill(DSColor.warning).frame(width: 9, height: 9).offset(x: -4, y: 4) }
                }
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .accessibilityLabel(Text(label))
    }
}
