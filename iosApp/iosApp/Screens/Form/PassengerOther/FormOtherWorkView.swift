import SwiftUI
import ComposeApp

// Экран «Прочая работа» — SCREEN_SPECS §8. Макета нет: собран из компонентов кита в стиле «Пассажиром».
// Логика — Kotlin `OtherWorkScreenIosViewModel`. Раздел информационный: в расчёт зарплаты не входит.

struct FormOtherWorkView: View {
    let routeId: String
    let otherWorkId: String?

    @StateObject private var vm = OtherWorkScreenViewModelWrapper()
    @State private var started = false
    @State private var picker: PassTimeTarget?
    @State private var clear: PassTimeTarget?
    @State private var sheet: OwSheet?
    @State private var deleteRequest: DSDeleteRequest?
    @Environment(\.scenePhase) private var scenePhase

    private enum OwSheet: String, Identifiable {
        case type, newType
        var id: String { rawValue }
    }

    private var offsetSeconds: Int { Int(vm.state?.displayOffsetSeconds ?? 10800) }
    private var format: RouteTimeFormat { RouteTimeFormat(offsetSeconds: offsetSeconds) }

    var body: some View {
        VStack(spacing: 0) {
            PassNavBar(title: "Прочая работа") { AppRouter.shared.back() }
            if let s = vm.state, !s.isLoading {
                content(s)
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
                vm.start(basicId: routeId, otherWorkId: otherWorkId)
            }
        }
        .onChange(of: scenePhase) { phase in
            if phase != .active { vm.flush() }
        }
        .modifier(PassPickerHost(
            picker: $picker,
            clear: $clear,
            offsetSeconds: offsetSeconds,
            title: { $0 == .first ? "Начало" : "Окончание" },
            deleteTitle: { $0 == .first ? "Время начала" : "Время окончания" },
            current: { $0 == .first ? vm.state?.timeStart.ms : vm.state?.timeEnd.ms },
            onPick: { target, ms in
                if target == .first { vm.setTimeStart(ms) } else { vm.setTimeEnd(ms) }
            },
            onClear: { target in
                if target == .first { vm.setTimeStart(nil) } else { vm.setTimeEnd(nil) }
            }
        ))
        .sheet(item: $sheet) { which in
            switch which {
            case .type:
                typeSheet
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.hidden)
            case .newType:
                NewTypeSheet(
                    onAdd: { name in
                        vm.addCustomType(name)
                        sheet = nil
                    },
                    onClose: { sheet = nil }
                )
                .presentationDetents([.height(300)])
                .presentationDragIndicator(.hidden)
            }
        }
        .sheet(isPresented: Binding(
            get: { vm.state?.locoPicker != nil },
            set: { if !$0 { vm.dismissLocoPicker() } }
        )) { locoSheet.presentationDetents([.medium, .large]).presentationDragIndicator(.hidden) }
    }

    // MARK: Содержимое

    private func content(_ s: OtherWorkScreenState) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if let error = s.errorMessage {
                    DSWarningBlock(title: error).padding(.top, 8)
                }

                DSGroupHeader("ТИП РАБОТЫ", topSpacing: error(s) ? DSMetrics.groupHeadMt : DSMetrics.groupHeadFirst)
                DSCard {
                    Button { hideKeyboard(); sheet = .type } label: {
                        HStack(spacing: 12) {
                            DSIconAvatar(icon: .wrench, size: .md, tone: .accent, shape: .circle)
                            Text(s.workType.isEmpty ? "Выберите тип" : s.workType)
                                .font(DSFont.sans(17, .medium))
                                .foregroundColor(s.workType.isEmpty ? DSColor.textFaint : DSColor.text)
                                .lineLimit(1)
                            Spacer(minLength: 8)
                            DSIcon.chevronDown.image.font(.system(size: 13, weight: .semibold)).foregroundColor(DSColor.textFaint)
                        }
                        .padding(.horizontal, DSMetrics.rowPadX)
                        .padding(.vertical, 12)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }

                DSGroupHeader("СТАНЦИЯ")
                DSCard {
                    PassStationField(
                        placeholder: "Станция",
                        external: s.station,
                        suggestions: { vm.stationSuggestions($0) },
                        onChange: { vm.setStation($0) },
                        onRemoveSuggestion: { vm.removeStationName($0) }
                    )
                }

                DSGroupHeader("ВРЕМЯ")
                DSCard {
                    PassDateTimeRow(
                        label: "Начало",
                        valueText: s.timeStart.ms.map { format.dateTime($0) },
                        showsDivider: true,
                        onTap: { hideKeyboard(); picker = .first },
                        onLongPress: { clear = .first }
                    )
                    PassDateTimeRow(
                        label: "Окончание",
                        valueText: s.timeEnd.ms.map { format.dateTime($0) },
                        onTap: { hideKeyboard(); picker = .second },
                        onLongPress: { clear = .second }
                    )
                }
                VStack(spacing: 10) {
                    DSTonalButton(title: "От явки до окончания работы", icon: .clock, tone: .subtle) { vm.applyTimeFromWork() }
                    DSTonalButton(title: "От приёмки до сдачи локомотива", icon: .locomotive, tone: .subtle) { vm.applyTimeFromLoco() }
                }
                .padding(.top, 12)
                if let result = s.resultTime {
                    PassDurationBadge(text: "Длительность · \(passDurationText(result.int64Value))")
                        .padding(.top, 12)
                }

                DSGroupHeader("ПРИМЕЧАНИЯ")
                DSCard {
                    PassNotesField(placeholder: "Например: фамилия составителя", external: s.notes) { vm.setNotes($0) }
                }
                Spacer(minLength: 40)
            }
            .padding(.horizontal, DSMetrics.screenPadX)
        }
        .passScrollDismissesKeyboard()
        .dsDeleteAlert($deleteRequest)
    }

    private func error(_ s: OtherWorkScreenState) -> Bool { s.errorMessage != nil }

    // MARK: Шторка «ТИП РАБОТЫ» (§8.3 п.3)

    private var typeSheet: some View {
        let s = vm.state
        let types = (s?.typeList as? [String]) ?? []
        let custom = Set((s?.customTypes as? [String]) ?? [])
        let selected = s?.workType ?? ""
        return DSBottomSheet(header: .titleClose(title: "ТИП РАБОТЫ", subtitle: nil), onClose: { sheet = nil }) {
            DSCard {
                ForEach(Array(types.enumerated()), id: \.element) { _, name in
                    HStack(spacing: 12) {
                        Button {
                            vm.setWorkType(name)
                            sheet = nil
                        } label: {
                            HStack {
                                Text(name).font(DSFont.sans(16, .medium)).foregroundColor(DSColor.text)
                                Spacer()
                                if name == selected {
                                    DSIcon.check.image.font(.system(size: 14, weight: .bold)).foregroundColor(DSColor.accent)
                                }
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        if custom.contains(name) {
                            Button {
                                deleteRequest = DSDeleteRequest(
                                    title: "Удалить тип «\(name)»?",
                                    message: "Тип работы будет удалён из списка. Уже сохранённые записи не изменятся.",
                                    confirmTitle: "Удалить"
                                ) { vm.deleteCustomType(name) }
                            } label: {
                                DSIcon.trash.image.font(.system(size: 15)).foregroundColor(DSColor.textFaint).padding(6)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(Text("Удалить тип"))
                        }
                    }
                    .padding(.horizontal, DSMetrics.rowPadX)
                    .padding(.vertical, 14)
                    .background(name == selected ? DSColor.accentSoft : Color.clear)
                    DSDivider()
                }
                Button {
                    // Сначала закрыть шторку типов, затем показать ввод нового типа.
                    sheet = nil
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { sheet = .newType }
                } label: {
                    HStack(spacing: 10) {
                        DSIcon.plus.image.font(.system(size: 14, weight: .semibold))
                        Text("Добавить свой тип").font(DSFont.sans(16, .semibold))
                        Spacer()
                    }
                    .foregroundColor(DSColor.accent)
                    .padding(.horizontal, DSMetrics.rowPadX)
                    .padding(.vertical, 14)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .dsDeleteAlert($deleteRequest)
    }

    // MARK: Шторка выбора локомотива (§8.3)

    private var locoSheet: some View {
        let items = (vm.state?.locoPicker as? [OtherWorkLocoItem]) ?? []
        return DSBottomSheet(
            header: .titleClose(title: "ЛОКОМОТИВ · \(items.count)", subtitle: nil),
            onClose: { vm.dismissLocoPicker() }
        ) {
            DSCard {
                ForEach(Array(items.enumerated()), id: \.element.id) { i, item in
                    Button { vm.selectLoco(item.id) } label: {
                        HStack {
                            Text(item.label).font(DSFont.mono(16, .medium)).foregroundColor(DSColor.text)
                            Spacer()
                        }
                        .padding(.horizontal, DSMetrics.rowPadX)
                        .padding(.vertical, 14)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    if i < items.count - 1 { DSDivider() }
                }
            }
        }
    }
}

/// «Новый тип работы» (`AppInputBottomSheet`, §8.3): подсказка, поле «Название», «Добавить».
private struct NewTypeSheet: View {
    let onAdd: (String) -> Void
    let onClose: () -> Void

    @State private var name = ""
    @FocusState private var focused: Bool

    var body: some View {
        DSBottomSheet(
            header: .titleClose(title: "Новый тип работы", subtitle: "Как называется этот вид работы?"),
            onClose: onClose
        ) {
            TextField("Название", text: $name)
                .focused($focused)
                .font(DSFont.sans(17, .medium))
                .foregroundColor(DSColor.text)
                .submitLabel(.done)
                .padding(.horizontal, DSMetrics.inputPadX)
                .frame(minHeight: DSMetrics.inputMinH)
                .background(DSColor.bgSubtle)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                .padding(.bottom, 16)
            DSCTAButton(title: "Добавить", isEnabled: !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) {
                onAdd(name.trimmingCharacters(in: .whitespacesAndNewlines))
            }
        }
        .onAppear { focused = true }
    }
}
