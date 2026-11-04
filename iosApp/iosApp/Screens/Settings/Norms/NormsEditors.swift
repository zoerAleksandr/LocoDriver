import SwiftUI
import ComposeApp

// Редакторы серии и станции (SCREEN_SPECS §17.4): одни и те же вью в Настройках и в пикере шторки
// времени локомотива (§2.10). Макеты: design/src/norms-screens.jsx → IOSSeriesEditor / IOSStationEditor.
// Автосохранение: числа/переключатели — сразу, название — через 500 мс; «Готово»/уход с экрана —
// финальный commit (регистрация имени в справочнике автодополнения).

struct SeriesEditorContent: View {
    @ObservedObject var vm: SeriesEditorViewModelWrapper
    /// Вызывается, когда серия удалена (экран/шторка закрываются).
    let onDeleted: () -> Void

    @State private var texts: [NormsField: String] = [:]
    @State private var saveTask: Task<Void, Never>?
    @State private var deleteRequest: DSDeleteRequest?
    @State private var didLoad = false
    @FocusState private var focus: NormsField?
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                NormsHint(text: "Длительности применяются между «Начало приёмки» → «Окончание приёмки» (и так же для сдачи) в шторке локомотива.")

                DSGroupHeader("ОСНОВНОЕ")
                DSCard {
                    NormsInputRow(label: "Серия", placeholder: "Название", text: bind(.name), mono: true)
                        .focused($focus, equals: .name)
                    HStack {
                        Text("Тип").font(DSFont.sans(14)).foregroundColor(DSColor.textMuted)
                        Spacer()
                        Picker("", selection: Binding(get: { vm.state?.isElectric ?? true }, set: { vm.setElectric($0) })) {
                            Text("Электровоз").tag(true)
                            Text("Тепловоз").tag(false)
                        }
                        .pickerStyle(.segmented).frame(maxWidth: 240)
                    }
                    .padding(.horizontal, DSMetrics.rowPadX).padding(.vertical, 10)
                }

                DSGroupHeader("НОМЕР СЕКЦИИ")
                DSCard {
                    HStack {
                        Picker("", selection: Binding(get: { vm.state?.numberingLetters ?? false }, set: { vm.setNumberingLetters($0) })) {
                            Text("1 / 2 / 3").tag(false)
                            Text("А / Б / В").tag(true)
                        }
                        .pickerStyle(.segmented)
                    }
                    .padding(.horizontal, DSMetrics.rowPadX).padding(.vertical, 10)
                }

                DSGroupHeader("ПОСЛЕ ОТСТОЯ")
                DSCard {
                    stepper(.n1, icon: .calendar, label: "Приёмка", field: 0)
                    stepper(.n2, icon: .clock, label: "Сдача", field: 1, last: true)
                }
                DSGroupHeader("ИЗ РУК В РУКИ")
                DSCard {
                    stepper(.n3, icon: .calendar, label: "Приёмка", field: 2)
                    stepper(.n4, icon: .clock, label: "Сдача", field: 3, last: true)
                }

                DSGroupHeader("ДАННЫЕ ЛОКОМОТИВА")
                DSCard {
                    passport(.sections, "Количество секций", 0, .numberPad, nil)
                    passport(.weight, "Вес", 1, .decimalPad, "т")
                    passport(.axles, "Число осей", 2, .numberPad, nil)
                    passport(.axleLoad, "Нажатие на ось", 3, .decimalPad, "тс")
                    passport(.volume, "Объём ГР", 4, .numberPad, "л", last: true)
                }
                Text("Подставляются в форму локомотива (секции) и в справку ВУ-45. Пусто — не задано.")
                    .dsTextStyle(.captionMuted).padding(.top, 8).padding(.horizontal, 4)

                if vm.state?.isNewRecord == false {
                    DSDangerButton(title: "Удалить серию") {
                        deleteRequest = DSDeleteRequest(title: "Удалить серию?") { vm.delete() }
                    }
                    .padding(.top, 28)
                }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.top, 12).padding(.bottom, 32)
        }
        .scrollDismissesKeyboard(.interactively)
        .normsKeyboardDone($focus)
        .dsDeleteAlert($deleteRequest)
        .onReceive(vm.$state) { st in
            guard let st = st else { return }
            if st.deleted { onDeleted(); return }
            if st.loaded { sync(st) }
        }
        .onChange(of: focus) { _ in if let st = vm.state { sync(st) } }
        .onDisappear { saveTask?.cancel(); vm.commit() }
        .onChange(of: scenePhase) { if $0 != .active { vm.commit() } }
    }

    private func bind(_ f: NormsField) -> Binding<String> {
        Binding(
            get: { texts[f] ?? "" },
            set: { v in
                texts[f] = v
                switch f {
                case .name:
                    vm.setName(v)
                    saveTask?.cancel()
                    saveTask = Task {
                        try? await Task.sleep(nanoseconds: 500_000_000)
                        if !Task.isCancelled { vm.save() }
                    }
                case .n1: vm.setNorm(0, v)
                case .n2: vm.setNorm(1, v)
                case .n3: vm.setNorm(2, v)
                case .n4: vm.setNorm(3, v)
                case .sections: vm.setPassport(0, v)
                case .weight: vm.setPassport(1, v)
                case .axles: vm.setPassport(2, v)
                case .axleLoad: vm.setPassport(3, v)
                case .volume: vm.setPassport(4, v)
                default: break
                }
            }
        )
    }

    private func stepper(_ f: NormsField, icon: DSIcon, label: String, field: Int, last: Bool = false) -> some View {
        NormsStepperRow(icon: icon, label: label, text: bind(f), showsDivider: !last)
            .focused($focus, equals: f)
    }

    private func passport(_ f: NormsField, _ label: String, _ field: Int, _ kb: UIKeyboardType, _ suffix: String?, last: Bool = false) -> some View {
        NormsInputRow(label: label, placeholder: "—", text: bind(f), keyboard: kb, mono: true, suffix: suffix, showsDivider: !last)
            .focused($focus, equals: f)
    }

    /// Подтягивает значения из Kotlin в поля, не занятые вводом.
    private func sync(_ st: SeriesEditorStateUi) {
        let values: [NormsField: String] = [
            .name: st.name, .n1: st.acceptanceParking, .n2: st.deliveryParking,
            .n3: st.acceptanceHand, .n4: st.deliveryHand,
            .sections: st.sectionCount, .weight: st.weight, .axles: st.axleCount,
            .axleLoad: st.axleLoad, .volume: st.brakeVolume,
        ]
        for (f, v) in values where focus != f && texts[f] != v {
            texts[f] = v
        }
    }
}

struct StationEditorContent: View {
    @ObservedObject var vm: StationEditorViewModelWrapper
    let onDeleted: () -> Void

    @State private var texts: [NormsField: String] = [:]
    @State private var saveTask: Task<Void, Never>?
    @State private var deleteRequest: DSDeleteRequest?
    @FocusState private var focus: NormsField?
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                NormsHint(text: "4 интервала, привязанных к этой станции. Используются автоматически при заполнении времени приёмки и сдачи локомотива.")

                DSGroupHeader("ОСНОВНОЕ")
                DSCard {
                    NormsInputRow(label: "Название", placeholder: "Станция", text: bind(.name), alignRight: false, showsDivider: false)
                        .focused($focus, equals: .name)
                }

                DSGroupHeader("КООРДИНАТЫ")
                DSCard { coordinatesRow }

                DSGroupHeader("КИЛОМЕТРАЖ")
                DSCard {
                    NormsInputRow(label: "Километр", placeholder: "—", text: bind(.km), keyboard: .numberPad, mono: true, suffix: "км")
                        .focused($focus, equals: .km)
                    NormsInputRow(label: "Пикет", placeholder: "—", text: bind(.picket), keyboard: .numberPad, mono: true, suffix: "пк",
                                  showsDivider: false, isEnabled: !(texts[.km] ?? "").isEmpty)
                        .focused($focus, equals: .picket)
                        .opacity((texts[.km] ?? "").isEmpty ? 0.4 : 1)
                }
                Text("Ось станции по ж/д километражу.").dsTextStyle(.captionMuted).padding(.top, 8).padding(.horizontal, 4)

                DSGroupHeader("ПРИЁМКА")
                DSCard {
                    stepper(.n1, .calendar, "Явка → Начало", 0)
                    stepper(.n2, .mapPin, "Конец → КП", 1, last: true)
                }
                DSGroupHeader("СДАЧА")
                DSCard {
                    stepper(.n3, .mapPin, "КП → Начало", 2)
                    stepper(.n4, .clock, "Конец → Окончание работы", 3, last: true)
                }
                Text("Нормы выше используются автоматически при заполнении времени приёмки и сдачи локомотива на этой станции.")
                    .dsTextStyle(.captionMuted).padding(.top, 8).padding(.horizontal, 4)

                if vm.state?.loaded == true, vm.state?.deleted == false, !(texts[.name] ?? "").isEmpty {
                    DSDangerButton(title: "Удалить станцию") {
                        deleteRequest = DSDeleteRequest(title: "Удалить станцию?") { vm.delete() }
                    }
                    .padding(.top, 28)
                }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.top, 12).padding(.bottom, 32)
        }
        .scrollDismissesKeyboard(.interactively)
        .normsKeyboardDone($focus)
        .dsDeleteAlert($deleteRequest)
        .onReceive(vm.$state) { st in
            guard let st = st else { return }
            if st.deleted { onDeleted(); return }
            if st.loaded { sync(st) }
        }
        .onChange(of: focus) { _ in if let st = vm.state { sync(st) } }
        .onDisappear { saveTask?.cancel(); vm.commit() }
        .onChange(of: scenePhase) { if $0 != .active { vm.commit() } }
    }

    private var coordinatesRow: some View {
        let st = vm.state
        let has = st?.hasCoordinates == true
        return HStack(alignment: .top, spacing: 12) {
            Circle().fill(has ? DSColor.success : DSColor.warning).frame(width: 8, height: 8).padding(.top, 6)
            VStack(alignment: .leading, spacing: 3) {
                Text(has ? "Координаты есть" : "Нет координат").font(DSFont.sans(15, .medium)).foregroundColor(DSColor.text)
                if has, let t = st?.coordinatesText {
                    Text(t).font(DSFont.mono(12)).foregroundColor(DSColor.textMuted)
                } else {
                    Text("Без них станция не определится по GPS и не попадёт на карту плеча. Указать координаты можно в Android-версии или на сайте.")
                        .dsTextStyle(.captionMuted)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, DSMetrics.rowPadX).padding(.vertical, 14)
    }

    private func bind(_ f: NormsField) -> Binding<String> {
        Binding(
            get: { texts[f] ?? "" },
            set: { v in
                texts[f] = v
                switch f {
                case .name:
                    vm.setName(v)
                    saveTask?.cancel()
                    saveTask = Task {
                        try? await Task.sleep(nanoseconds: 500_000_000)
                        if !Task.isCancelled { vm.save() }
                    }
                case .n1: vm.setNorm(0, v)
                case .n2: vm.setNorm(1, v)
                case .n3: vm.setNorm(2, v)
                case .n4: vm.setNorm(3, v)
                case .km: vm.setAxisKm(v)
                case .picket: vm.setAxisPicket(v)
                default: break
                }
            }
        )
    }

    private func stepper(_ f: NormsField, _ icon: DSIcon, _ label: String, _ field: Int, last: Bool = false) -> some View {
        NormsStepperRow(icon: icon, label: label, text: bind(f), maxValue: 120, showsDivider: !last)
            .focused($focus, equals: f)
    }

    private func sync(_ st: StationEditorStateUi) {
        let values: [NormsField: String] = [
            .name: st.name, .n1: st.appearanceToStart, .n2: st.endToBarrier,
            .n3: st.barrierToStart, .n4: st.endToWorkEnd, .km: st.axisKm, .picket: st.axisPicket,
        ]
        for (f, v) in values where focus != f && texts[f] != v {
            texts[f] = v
        }
    }
}
