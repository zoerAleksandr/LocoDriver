import SwiftUI
import ComposeApp

// Шторки экрана «Поезд»: «Плечи» и редактор плеча (§6.6), «Параметры поезда» (§6.2 п.7),
// «Вагонник» (§6.2 п.8), «История данных поезда» (§6.7).
// Макет: design/src/ios-add-train.jsx → ShouldersSheet, SettingsSheet.

// MARK: - Плечи (§6.6)

private struct PhaseEditTarget: Identifiable {
    let phase: TrainPhaseUi?
    var id: String { phase?.id ?? "new" }
}

struct TrainPhasesSheet: View {
    @ObservedObject var vm: TrainScreenViewModelWrapper
    let state0: TrainScreenState
    private var state: TrainScreenState { vm.state ?? state0 }

    @Environment(\.dismiss) private var dismiss
    @State private var selectedId: String? = nil
    @State private var loaded = false
    @State private var editor: PhaseEditTarget? = nil

    private var phases: [TrainPhaseUi] { (state.phases as? [TrainPhaseUi]) ?? [] }

    var body: some View {
        VStack(spacing: 0) {
            DSBottomSheet(header: .titleClose(title: "Плечи", subtitle: nil), onClose: { dismiss() }) {
                if phases.isEmpty {
                    Text("Список пуст").dsTextStyle(.labelMuted).frame(maxWidth: .infinity).padding(.vertical, 24)
                } else {
                    DSCard {
                        ForEach(Array(phases.enumerated()), id: \.element.id) { index, p in
                            row(p, last: index == phases.count - 1)
                        }
                    }
                }
            }
            // Кнопки закреплены внизу, прокручивается только список (§6.6).
            VStack(spacing: 10) {
                if let id = selectedId, let p = phases.first(where: { $0.id == id }) {
                    HStack(spacing: 10) {
                        DSCTAButton(title: "Выбрать") {
                            vm.selectPhase(id)
                            dismiss()
                        }
                        DSTonalButton(title: "Редактировать", tone: .neutral) { editor = PhaseEditTarget(phase: p) }
                    }
                }
                DSTonalButton(title: "Новое плечо", icon: .plus, tone: .subtle) { editor = PhaseEditTarget(phase: nil) }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.top, 8)
            .padding(.bottom, 20)
            .background(DSColor.bg)
        }
        .background(DSColor.bg.ignoresSafeArea())
        .onAppear {
            guard !loaded else { return }
            loaded = true
            selectedId = phases.first(where: { $0.isSelected })?.id
        }
        .sheet(item: $editor) { target in
            TrainPhaseEditor(vm: vm, state0: state, phase: target.phase) {
                if target.phase?.id == selectedId { selectedId = nil }
            }
        }
    }

    private func row(_ p: TrainPhaseUi, last: Bool) -> some View {
        let isSel = selectedId == p.id
        return VStack(spacing: 0) {
            Button {
                // Повторный тап по выделенной строке снимает выделение (решение 07.10.2026).
                selectedId = isSel ? nil : p.id
            } label: {
                HStack(spacing: 10) {
                    Text("\(p.from) — \(p.to)")
                        .font(DSFont.sans(16)).foregroundColor(DSColor.text).lineLimit(1)
                    Spacer(minLength: 8)
                    Text("\(p.distance) км").font(DSFont.mono(14, .medium)).foregroundColor(DSColor.textMuted)
                    // Слот под галочку зарезервирован у всех строк: километраж не прыгает.
                    DSIcon.check.image.font(.system(size: 14, weight: .bold)).foregroundColor(DSColor.accent)
                        .opacity(isSel ? 1 : 0).frame(width: 22)
                }
                .padding(.horizontal, DSMetrics.rowPadX)
                .padding(.vertical, 14)
                .background(isSel ? DSColor.accentSoft : Color.clear)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if !last { DSDivider() }
        }
    }
}

struct TrainPhaseEditor: View {
    @ObservedObject var vm: TrainScreenViewModelWrapper
    let state0: TrainScreenState
    private var state: TrainScreenState { vm.state ?? state0 }
    let phase: TrainPhaseUi?
    var onDeleted: () -> Void = {}

    @Environment(\.dismiss) private var dismiss
    @State private var loaded = false
    @State private var from = ""
    @State private var to = ""
    @State private var distance = ""
    @State private var rate = ""
    @State private var reverse = true
    @State private var skipSave = false
    @State private var deleteRequest: DSDeleteRequest? = nil

    private var distanceValue: Int { Int(distance) ?? 0 }
    private var rateValue: Double {
        let v = Double(rate.replacingOccurrences(of: ",", with: ".")) ?? 0
        return v < 0 ? 0 : v
    }
    /// Сохраняется, только если обе станции непусты (расстояние не обязательно, §6.6).
    private var isValid: Bool {
        !from.trimmingCharacters(in: .whitespaces).isEmpty && !to.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        DSBottomSheet(header: .titleClose(title: phase == nil ? "Новое плечо" : "Плечо", subtitle: nil), onClose: { dismiss() }) {
            VStack(alignment: .leading, spacing: 14) {
                TrainStationNameField(title: "От станции", text: $from, suggest: { vm.suggestStations($0) }, onRemove: { vm.removeStationSuggestion($0) })
                TrainStationNameField(title: "До станции", text: $to, suggest: { vm.suggestStations($0) }, onRemove: { vm.removeStationSuggestion($0) })
                HStack(spacing: 10) {
                    TrainLabeledField(title: "Расстояние, км", placeholder: "0", text: $distance, keyboard: .numberPad, mono: true)
                    TrainLabeledField(title: "Доплата за км", placeholder: "0", text: $rate, keyboard: .decimalPad, mono: true)
                }
                if phase == nil {
                    Toggle("Создать плечо в обратном направлении", isOn: $reverse)
                        .font(DSFont.sans(15))
                        .padding(.horizontal, DSMetrics.rowPadX).padding(.vertical, 10)
                        .background(DSColor.surface)
                        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
                }
                DSCTAButton(title: "Готово") { dismiss() }
                if let p = phase {
                    DSDangerButton(title: "Удалить плечо") {
                        deleteRequest = DSDeleteRequest(title: "Удалить плечо?") {
                            skipSave = true
                            vm.deletePhase(p.id)
                            onDeleted()
                            dismiss()
                        }
                    }
                }
            }
        }
        .onAppear {
            guard !loaded else { return }
            loaded = true
            if let p = phase {
                from = p.from; to = p.to
                distance = p.distance > 0 ? String(p.distance) : ""
                rate = p.rate > 0 ? String(p.rate) : ""
            }
        }
        // Автосохранение при закрытии любым способом (§6.6).
        .onDisappear {
            guard !skipSave, isValid else { return }
            vm.savePhase(id: phase?.id, from: from.trimmingCharacters(in: .whitespaces),
                         to: to.trimmingCharacters(in: .whitespaces),
                         distance: distanceValue, rate: rateValue, alsoReverse: phase == nil && reverse)
        }
        .dsDeleteAlert($deleteRequest)
    }
}

// MARK: - Параметры поезда (толкач / двойная тяга / сдвоенный)

struct TrainAssistsSheet: View {
    @ObservedObject var vm: TrainScreenViewModelWrapper
    let state0: TrainScreenState
    private var state: TrainScreenState { vm.state ?? state0 }
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Параметры поезда", subtitle: nil), onClose: { dismiss() }) {
            VStack(alignment: .leading, spacing: 10) {
                section(kind: 0, title: "Толкач", ui: state.pusher,
                        first: "Я толкаю", second: "Меня толкают", hint: nil)
                section(kind: 1, title: "Двойная тяга", ui: state.doubleTraction, first: nil, second: nil, hint: nil)
                section(kind: 2, title: "Сдвоенный поезд", ui: state.doubledTrain,
                        first: "Я первый", second: "Я второй", hint: "Данные второго / первого")
                DSCTAButton(title: "Готово") { dismiss() }.padding(.top, 8)
            }
        }
        // Закрытие любым способом сохраняет серии тяги в общий список серий.
        .onDisappear { vm.saveAssistSeries() }
    }

    @ViewBuilder
    private func section(kind: Int, title: String, ui: TrainAssistUi, first: String?, second: String?, hint: String?) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            TrainSectionToggleHeader(title: title.uppercased(), isPresent: ui.present,
                                     onAdd: { vm.addAssist(kind) }, onRemove: { vm.removeAssist(kind) })
            if ui.present {
                if let first = first, let second = second {
                    Picker("", selection: Binding(
                        get: { ui.isFirst?.boolValue },
                        set: { if let v = $0 { vm.setAssistDirection(kind, isFirst: v) } }
                    )) {
                        Text(first).tag(Optional(true))
                        Text(second).tag(Optional(false))
                    }
                    .pickerStyle(.segmented)
                    if kind == 2, let isFirst = ui.isFirst {
                        Text(isFirst.boolValue ? "Данные второго" : "Данные первого").dsTextStyle(.hint)
                    }
                }
                TrainStationNameField(title: "Серия", text: Binding(get: { ui.series }, set: { vm.setAssistField(kind, "series", $0) }),
                                      suggest: { vm.suggestSeries($0) }, onRemove: { vm.removeSeriesSuggestion($0) }, placeholder: "Серия")
                HStack(spacing: 10) {
                    TrainLabeledField(title: "Номер", placeholder: "—", text: Binding(get: { ui.number }, set: { vm.setAssistField(kind, "number", $0) }), prefix: "№ ", mono: true)
                    TrainLabeledField(title: "Машинист", placeholder: "—", text: Binding(get: { ui.driver }, set: { vm.setAssistField(kind, "driver", $0) }), prefix: "ТЧМ ")
                }
                TrainLabeledField(title: "Примечание", placeholder: "—", text: Binding(get: { ui.notes }, set: { vm.setAssistField(kind, "notes", $0) }))
            }
        }
        .padding(DSMetrics.rowPadX)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
    }
}

// MARK: - Вагонник

struct TrainInspectorSheet: View {
    @ObservedObject var vm: TrainScreenViewModelWrapper
    let state0: TrainScreenState
    private var state: TrainScreenState { vm.state ?? state0 }
    @Environment(\.dismiss) private var dismiss
    @State private var showTime = false

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Вагонник", subtitle: nil), onClose: { dismiss() }) {
            VStack(alignment: .leading, spacing: 10) {
                TrainSectionToggleHeader(title: "ВАГОННИК", isPresent: state.hasCarInspector,
                                         onAdd: { vm.addCarInspector() }, onRemove: { vm.removeCarInspector() })
                if state.hasCarInspector {
                    TrainLabeledField(title: "ФИО", placeholder: "—", text: Binding(get: { state.carInspectorFullName }, set: { vm.setCarInspectorFullName($0) }))
                    TrainLabeledField(title: "Табельный номер", placeholder: "—", text: Binding(get: { state.carInspectorTabNumber }, set: { vm.setCarInspectorTabNumber($0) }), keyboard: .numbersAndPunctuation)
                    Button { showTime = true } label: {
                        HStack {
                            Text("Время прицепки").font(DSFont.sans(15)).foregroundColor(DSColor.text)
                            Spacer()
                            if let ms = state.carInspectorCouplingTime?.int64Value {
                                Text("\(TrainTime.date(ms, offset: Int(state.displayOffsetSeconds))) \(TrainTime.hm(ms, offset: Int(state.displayOffsetSeconds)))")
                                    .font(DSFont.mono(15, .medium)).foregroundColor(DSColor.text)
                            }
                        }
                        .padding(.horizontal, 14).frame(height: 48)
                        .background(DSColor.surface)
                        .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous).stroke(DSColor.border, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
                DSCTAButton(title: "Готово") { dismiss() }.padding(.top, 8)
            }
        }
        .sheet(isPresented: $showTime) {
            DSDateTimeSheet(
                title: "Время прицепки",
                timeZone: TimeZone(secondsFromGMT: Int(state.displayOffsetSeconds)) ?? .current,
                initial: state.carInspectorCouplingTime.map { TimeFormatter.msToDate($0.int64Value) } ?? Date(),
                onDone: { ms in vm.setCouplingTime(ms); showTime = false },
                onCancel: { showTime = false }
            )
            .presentationDetents([.height(380)])
        }
    }
}

// MARK: - История данных (§6.7)

private struct TrainVersionCard: View {
    let version: TrainVersionUi
    let onSave: (String, String, String) -> Void
    @State private var weight: String
    @State private var axle: String
    @State private var length: String

    init(version: TrainVersionUi, onSave: @escaping (String, String, String) -> Void) {
        self.version = version
        self.onSave = onSave
        _weight = State(initialValue: version.weight)
        _axle = State(initialValue: version.axle)
        _length = State(initialValue: version.conditionalLength)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(version.title).font(DSFont.sans(15, .semibold)).foregroundColor(DSColor.text)
            HStack(spacing: 8) {
                TrainLabeledField(title: "Вес, т", placeholder: "—", text: $weight, keyboard: .numbersAndPunctuation, mono: true)
                TrainLabeledField(title: "Оси", placeholder: "—", text: $axle, keyboard: .numbersAndPunctuation, mono: true)
                TrainLabeledField(title: "У.Д.", placeholder: "—", text: $length, keyboard: .numbersAndPunctuation, mono: true)
            }
            DSTonalButton(title: "Сохранить", tone: .accent) { onSave(weight, axle, length) }
        }
        .padding(DSMetrics.rowPadX)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
    }
}

struct TrainHistorySheet: View {
    @ObservedObject var vm: TrainScreenViewModelWrapper
    let state0: TrainScreenState
    private var state: TrainScreenState { vm.state ?? state0 }
    @Environment(\.dismiss) private var dismiss

    private var versions: [TrainVersionUi] { (state.versions as? [TrainVersionUi]) ?? [] }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("История данных поезда").dsTextStyle(.sectionH2)
                Spacer()
                DSIconButton(icon: .close, variant: .subtle, size: 30, accessibilityLabel: "Закрыть") { dismiss() }
            }
            .padding(.horizontal, DSMetrics.rowPadX).padding(.top, 14).padding(.bottom, 8)
            // Удаление карточки — свайпом влево (постоянной иконки удаления нет).
            List {
                ForEach(versions, id: \.index) { v in
                    TrainVersionCard(version: v) { w, a, l in vm.updateDataVersion(Int(v.index), weight: w, axle: a, length: l) }
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) { vm.deleteDataVersion(Int(v.index)) } label: {
                                Label("Удалить", systemImage: "trash")
                            }
                        }
                }
            }
            .listStyle(.plain)
        }
        .background(DSColor.bg.ignoresSafeArea())
    }
}
