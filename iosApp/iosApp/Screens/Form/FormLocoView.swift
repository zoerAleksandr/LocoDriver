import SwiftUI
import ComposeApp

// Экран «Локомотив» — SCREEN_SPECS §1.
// Макет: design/src/locomotive-screens-v2.jsx → IOSScreenLocomotiveV2.
// Логика и расчёты — Kotlin `LocoScreenIosViewModel` (аналог Android LocoFormViewModel).
// По спеке (макет противоречит): «Готово» слева и ⚙ справа в топ-баре, автосейв без «Сохранить».

struct FormLocoView: View {
    let routeId: String
    let locoId: String?

    @StateObject private var vm = LocoScreenViewModelWrapper()
    @State private var started = false
    @State private var timeKind: TimeKind?
    @State private var coeff: CoeffTarget?
    @State private var applySupplyToSection = true
    @State private var pendingDeleteId: String?
    @State private var showSuggestions = false
    @FocusState private var seriesFocused: Bool
    @State private var seriesText = ""

    private enum TimeKind: String, Identifiable {
        case acceptance, delivery
        var id: String { rawValue }
    }

    private struct CoeffTarget: Identifiable {
        let index: Int
        let isSupply: Bool
        var id: String { "\(index)-\(isSupply)" }
    }

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
                vm.start(basicId: routeId, locoId: locoId)
            } else {
                vm.reloadSettings()
            }
        }
        .onChange(of: vm.state?.isLoading) { _ in
            if let s = vm.state, !s.isLoading { seriesText = s.series }
        }
        .sheet(item: $timeKind) { kind in timeSheet(kind) }
        .sheet(item: $coeff) { target in coeffSheet(target) }
        .sheet(isPresented: Binding(get: { pendingDeleteId != nil }, set: { if !$0 { pendingDeleteId = nil } })) {
            deleteSheet.presentationDetents([.height(220)])
        }
        .alert("Не удалось сохранить", isPresented: Binding(get: { vm.state?.saveError != nil }, set: { if !$0 { vm.clearSaveError() } })) {
            Button("OK", role: .cancel) { vm.clearSaveError() }
        } message: {
            Text(vm.state?.saveError ?? "")
        }
    }

    // MARK: Навбар

    private var navBar: some View {
        ZStack {
            Text("Локомотив").dsTextStyle(.navTitle).lineLimit(1)
            HStack {
                DSNavPillButton(title: "Готово") {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    AppRouter.shared.back()
                }
                Spacer()
                DSNavPillButton(icon: .gear, accessibilityLabel: "Настройки локомотива") {
                    let name = vm.state?.settingsSeriesName ?? ""
                    AppRouter.shared.showSettingsSection(.locomotive(series: name.isEmpty ? nil : name))
                }
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.vertical, 12)
        .background(DSColor.bg)
    }

    // MARK: Содержимое

    private func content(_ s: LocoScreenState) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                DSGroupHeader(title: "ОСНОВНЫЕ ДАННЫЕ", topSpacing: DSMetrics.groupHeadFirst) { typeToggle(s) }
                mainCard(s)

                DSGroupHeader("ВРЕМЯ")
                timeCard(s)

                DSGroupHeader(title: "СЕКЦИИ") { stepper(s) }
                sections(s)

                if s.showHeating || s.showAuxiliary {
                    DSGroupHeader("ДРУГОЕ")
                    LocoCountersCard(state: s, vm: vm)
                }

                DSGroupHeader("ИТОГО")
                if s.isElectric { LocoElectricTotals(state: s, vm: vm) } else { LocoDieselTotals(state: s, vm: vm) }
                Spacer(minLength: 40)
            }
            .padding(.horizontal, DSMetrics.screenPadX)
        }
        .scrollDismissesKeyboardIfAvailable()
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Готово") {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                }
            }
        }
    }

    private func typeToggle(_ s: LocoScreenState) -> some View {
        HStack(spacing: 0) {
            typeButton(icon: "bolt.fill", active: s.isElectric, label: "Электровоз") { vm.changeType(isElectric: true) }
            typeButton(icon: "drop.fill", active: !s.isElectric, label: "Тепловоз") { vm.changeType(isElectric: false) }
        }
        .padding(2)
        .background(DSColor.bgSubtle)
        .clipShape(Capsule())
    }

    private func typeButton(icon: String, active: Bool, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(active ? DSColor.accentInk : DSColor.textMuted)
                .frame(width: 38, height: 28)
                .background(active ? DSColor.accent : Color.clear)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(label))
    }

    // MARK: Основные данные

    private func mainCard(_ s: LocoScreenState) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            DSCard {
                HStack(spacing: 0) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("СЕРИЯ").dsTextStyle(.group)
                        TextField("Серия", text: $seriesText)
                            .focused($seriesFocused)
                            .font(DSFont.mono(17, .medium))
                            .foregroundColor(DSColor.text)
                            .submitLabel(.done)
                            .autocorrectionDisabled()
                            .onChange(of: seriesText) { value in
                                guard seriesFocused else { return }
                                vm.setSeries(value)
                                showSuggestions = true
                            }
                            .onChange(of: seriesFocused) { focused in
                                showSuggestions = focused
                                if !focused { seriesText = vm.state?.series ?? seriesText }
                            }
                            .onChange(of: s.series) { value in
                                // Серия, выбранная в шторке времени, подхватывается, не мешая набору (§2.12).
                                if !seriesFocused { seriesText = value }
                            }
                    }
                    .padding(.horizontal, DSMetrics.rowPadX)
                    .padding(.vertical, 12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Rectangle().fill(DSColor.border).frame(width: 1)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("НОМЕР").dsTextStyle(.group)
                        LocoNumberField(external: s.number) { vm.setNumber($0) }
                    }
                    .padding(.horizontal, DSMetrics.rowPadX)
                    .padding(.vertical, 12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            if showSuggestions && seriesFocused {
                suggestions
            }
        }
    }

    @ViewBuilder
    private var suggestions: some View {
        let items = vm.suggestSeries(seriesText)
        if !items.isEmpty {
            DSCard {
                ForEach(Array(items.enumerated()), id: \.element) { i, name in
                    HStack {
                        Button {
                            vm.selectSeries(name)
                            seriesText = name
                            seriesFocused = false
                        } label: {
                            Text(name).font(DSFont.mono(16)).foregroundColor(DSColor.text)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .buttonStyle(.plain)
                        Button { vm.removeSeriesSuggestion(name) } label: {
                            DSIcon.close.image.font(.system(size: 12, weight: .bold)).foregroundColor(DSColor.textFaint)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(Text("Убрать из подсказок"))
                    }
                    .padding(.horizontal, DSMetrics.rowPadX)
                    .padding(.vertical, 12)
                    if i < items.count - 1 { DSDivider() }
                }
            }
        }
    }

    // MARK: Время

    private func timeCard(_ s: LocoScreenState) -> some View {
        let fmt = RouteTimeFormat(offsetSeconds: Int(s.displayOffsetSeconds))
        func t(_ ms: KotlinLong?) -> String? { ms.ms.map { fmt.time($0) } }
        return DSCard {
            timeRow(
                title: "ПРИЁМКА", kind: .acceptance,
                cells: [("начало", t(s.timeStartOfAcceptance)), ("конец", t(s.timeEndOfAcceptance)), ("КП", t(s.timeBarrierOut))]
            )
            DSDivider()
            timeRow(
                title: "СДАЧА", kind: .delivery,
                cells: [("КП", t(s.timeBarrierIn)), ("начало", t(s.timeStartOfDelivery)), ("конец", t(s.timeEndOfDelivery))]
            )
        }
    }

    private func timeRow(title: String, kind: TimeKind, cells: [(String, String?)]) -> some View {
        Button { timeKind = kind } label: {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text(title).dsTextStyle(.group)
                    Spacer()
                    DSIcon.chevronRight.image.font(.system(size: 13, weight: .semibold)).foregroundColor(DSColor.textFaint)
                }
                HStack(spacing: 6) {
                    ForEach(Array(cells.enumerated()), id: \.offset) { i, cell in
                        if i > 0 { Text("→").font(DSFont.sans(14)).foregroundColor(DSColor.textFaint) }
                        VStack(spacing: 2) {
                            Text(cell.1 ?? "—:—")
                                .font(DSFont.mono(17, .medium))
                                .foregroundColor(cell.1 == nil ? DSColor.textFaint : DSColor.text)
                            Text(cell.0).dsTextStyle(.group)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, DSMetrics.rowPadY)
            .contentShape(Rectangle())
        }
        .buttonStyle(DSRowPressStyle())
    }

    // MARK: Секции

    private func stepper(_ s: LocoScreenState) -> some View {
        let n = Int(s.sectionCount)
        return HStack(spacing: 10) {
            Button {
                if n > 1 { requestDeleteLast(s) }
            } label: {
                Text("−").font(DSFont.sans(20, .semibold)).frame(width: 30, height: 28)
            }
            .disabled(n <= 1)
            .opacity(n <= 1 ? 0.35 : 1)
            Text("\(n)").font(DSFont.mono(15, .semibold)).foregroundColor(DSColor.text)
            Button { vm.addSection() } label: {
                Text("+").font(DSFont.sans(20, .semibold)).frame(width: 30, height: 28)
            }
        }
        .foregroundColor(DSColor.accent)
        .padding(.horizontal, 6)
        .background(DSColor.bgSubtle)
        .clipShape(Capsule())
    }

    private func requestDeleteLast(_ s: LocoScreenState) {
        let id: String?
        if s.isElectric {
            id = ((s.electricSections as? [LocoElectricSectionUi]) ?? []).last?.id
        } else {
            id = ((s.dieselSections as? [LocoDieselSectionUi]) ?? []).last?.id
        }
        if let id = id { requestDelete(id) }
    }

    private func requestDelete(_ id: String) {
        if !vm.requestDeleteSection(id) { pendingDeleteId = id }
    }

    @ViewBuilder
    private func sections(_ s: LocoScreenState) -> some View {
        VStack(spacing: DSMetrics.cardGap) {
            if s.isElectric {
                let list = (s.electricSections as? [LocoElectricSectionUi]) ?? []
                ForEach(Array(list.enumerated()), id: \.element.id) { i, section in
                    LocoElectricSectionCard(section: section, index: i, showOtherCurrent: s.showOtherCurrent, vm: vm) {
                        requestDelete(section.id)
                    }
                }
            } else {
                let list = (s.dieselSections as? [LocoDieselSectionUi]) ?? []
                ForEach(Array(list.enumerated()), id: \.element.id) { i, section in
                    LocoDieselSectionCard(
                        section: section, index: i, isKiloMode: s.isKiloMode, vm: vm,
                        onCoeff: { vm.openCoefficient(i); coeff = CoeffTarget(index: i, isSupply: false) },
                        onSupplyCoeff: { vm.openCoefficient(i); applySupplyToSection = true; coeff = CoeffTarget(index: i, isSupply: true) },
                        onDelete: { requestDelete(section.id) }
                    )
                }
            }
        }
    }

    private var deleteSheet: some View {
        VStack(spacing: 14) {
            Text("Удалить секцию?").dsTextStyle(.sectionH2)
            DSDangerButton(title: "Да, удалить") {
                if let id = pendingDeleteId { vm.deleteSection(id) }
                pendingDeleteId = nil
            }
            DSTextAction(title: "Отмена") { pendingDeleteId = nil }
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(DSColor.bg.ignoresSafeArea())
    }

    // MARK: Шторки

    @ViewBuilder
    private func coeffSheet(_ target: CoeffTarget) -> some View {
        if let s = vm.state, let list = s.dieselSections as? [LocoDieselSectionUi], target.index < list.count {
            let section = list[target.index]
            let value = target.isSupply ? section.refuelCoefficientText : section.coefficientText
            LocoCoeffSheet(
                title: target.isSupply ? "Коэффициент экипировки" : "Коэффициент секции",
                value: value,
                recent: vm.recentCoefficients(),
                applyLabel: target.isSupply ? "Применить ко всем значениям этой секции" : nil,
                applyToSection: $applySupplyToSection,
                step: { vm.stepCoefficient($0, $1) },
                onChange: { text in
                    if target.isSupply {
                        vm.setRefuelCoefficient(target.index, text, applyToSection: applySupplyToSection)
                    } else {
                        vm.setCoefficient(target.index, text)
                    }
                },
                onApplyToggled: { on in if on { vm.applyRefuelCoefficientToSection(target.index) } },
                onDone: {
                    if target.isSupply { vm.closeRefuelCoefficient(target.index) } else { vm.closeCoefficient(target.index) }
                    coeff = nil
                }
            )
            .presentationDetents([.medium])
            .interactiveDismissDisabled(false)
        }
    }

    @ViewBuilder
    private func timeSheet(_ kind: TimeKind) -> some View {
        if let s = vm.state {
            LocoTimeSheetView(
                kind: kind == .acceptance ? "acceptance" : "delivery",
                state: s, routeId: routeId, vm: vm
            )
            .presentationDetents([.large])
        }
    }
}

/// Поле «Номер» — свободный текст (§1.3 п.2).
private struct LocoNumberField: View {
    let external: String
    let onChange: (String) -> Void
    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        TextField("Номер", text: $text)
            .focused($focused)
            .font(DSFont.mono(17, .medium))
            .foregroundColor(DSColor.text)
            .submitLabel(.done)
            .onAppear { text = external }
            .onChange(of: text) { v in if focused { onChange(v) } }
            .onChange(of: external) { v in if !focused { text = v } }
    }
}

private extension View {
    @ViewBuilder
    func scrollDismissesKeyboardIfAvailable() -> some View {
        if #available(iOS 16.0, *) { self.scrollDismissesKeyboard(.interactively) } else { self }
    }
}
