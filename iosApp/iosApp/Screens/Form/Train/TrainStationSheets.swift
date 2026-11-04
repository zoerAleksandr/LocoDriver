import SwiftUI
import ComposeApp

// Шторки станции (§6.4) и перегона (§6.5). Макет: design/src/station-edit-sheet.jsx.
// Закрытие шторки любым способом = сохранение (`saveAndDismiss`), кроме пустой новой станции.

/// Блок времени шторки станции: подпись + дата, крупное время, «✕», ряд -5/-1/Сейчас/+1/+5.
private struct TrainTimeBlock: View {
    let title: String
    let value: Int64?
    let offset: Int
    let onPick: () -> Void
    let onChange: (Int64?) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(title).dsTextStyle(.group)
                Spacer()
                if let v = value {
                    Text(TrainTime.date(v, offset: offset)).font(DSFont.mono(13)).foregroundColor(DSColor.textMuted)
                }
            }
            HStack {
                Button(action: onPick) {
                    Text(value.map { TrainTime.hm($0, offset: offset) } ?? "00:00")
                        .font(DSFont.mono(34, .bold))
                        .foregroundColor(value == nil ? DSColor.textFaint : DSColor.text)
                }
                .buttonStyle(.plain)
                Spacer()
                if value != nil {
                    DSIconButton(icon: .close, variant: .subtle, size: 30, accessibilityLabel: "Очистить время") { onChange(nil) }
                }
            }
            HStack(spacing: 8) {
                quick("-5") { shift(-5) }
                quick("-1") { shift(-1) }
                quick("Сейчас") { onChange(TrainTime.nowMinute()) }
                quick("+1") { shift(1) }
                quick("+5") { shift(5) }
            }
        }
        .padding(DSMetrics.rowPadX)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
    }

    private func shift(_ minutes: Int64) {
        let base = value.map { $0 - $0 % 60_000 } ?? TrainTime.nowMinute()
        onChange(base + minutes * 60_000)
    }

    private func quick(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(DSFont.sans(14, .semibold))
                .foregroundColor(DSColor.accent)
                .frame(maxWidth: .infinity, minHeight: 36)
                .background(DSColor.bgSubtle)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.sm, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct TrainTimePick: Identifiable {
    let isArrival: Bool
    var id: Bool { isArrival }
}

struct TrainStationSheet: View {
    @ObservedObject var vm: TrainScreenViewModelWrapper
    let state0: TrainScreenState
    private var state: TrainScreenState { vm.state ?? state0 }
    /// Индекс станции в исходном порядке; -1 — новая.
    let index: Int
    let newStationId: String?

    @Environment(\.dismiss) private var dismiss
    @State private var loaded = false
    @State private var name = ""
    @State private var track = ""
    @State private var arrival: Int64? = nil
    @State private var departure: Int64? = nil
    @State private var isFinal = false
    @State private var isPassing = false
    @State private var skipSave = false
    @State private var pick: TrainTimePick? = nil
    @State private var showData = false
    @State private var dWeight = ""
    @State private var dAxle = ""
    @State private var dLength = ""
    @State private var deleteRequest: DSDeleteRequest? = nil

    private var stations: [TrainStationUi] { (state.stations as? [TrainStationUi]) ?? [] }
    private var isNew: Bool { index == -1 }
    private var isFirst: Bool { index == 0 || (isNew && stations.isEmpty) }
    private var stationId: String { isNew ? (newStationId ?? "") : (stations.indices.contains(index) ? stations[index].id : "") }
    private var offset: Int { Int(state.displayOffsetSeconds) }

    var body: some View {
        DSBottomSheet(header: .titleClose(title: isNew ? "Новая станция" : "Редактировать станцию", subtitle: nil), onClose: { dismiss() }) {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .top, spacing: 10) {
                    TrainStationNameField(title: "Название", text: $name,
                                          suggest: { vm.suggestStations($0) }, onRemove: { vm.removeStationSuggestion($0) })
                    TrainLabeledField(title: "Путь", placeholder: "№", text: $track)
                        .frame(width: 110)
                        .onChange(of: track) { if $0.count > 6 { track = String($0.prefix(6)) } }
                }
                if !isFirst {
                    flags
                }
                if isPassing {
                    TrainTimeBlock(title: "ПРОСЛЕДОВАНИЕ", value: arrival, offset: offset,
                                   onPick: { pick = TrainTimePick(isArrival: true) }, onChange: { arrival = $0 })
                } else {
                    TrainTimeBlock(title: "ПРИБЫТИЕ", value: arrival, offset: offset,
                                   onPick: { pick = TrainTimePick(isArrival: true) }, onChange: { arrival = $0 })
                    if let a = arrival, let d = departure, d > a, !isFinal {
                        Text("Стоянка \((d - d % 60_000 - (a - a % 60_000)) / 60_000) мин")
                            .font(DSFont.mono(13, .semibold)).foregroundColor(DSColor.textMuted)
                            .frame(maxWidth: .infinity)
                    }
                    TrainTimeBlock(title: "ОТПРАВЛЕНИЕ", value: departure, offset: offset,
                                   onPick: { pick = TrainTimePick(isArrival: false) }, onChange: { departure = $0 })
                }
                if !isFirst { dataBlock }
                DSCTAButton(title: "Готово") { dismiss() }
                if !isNew {
                    DSDangerButton(title: "Удалить станцию") {
                        deleteRequest = DSDeleteRequest(
                            title: name.isEmpty ? "Удалить станцию?" : "Удалить станцию \(name)?"
                        ) {
                            skipSave = true
                            vm.deleteStation(index)
                            dismiss()
                        }
                    }
                }
            }
        }
        .onAppear { load() }
        .onDisappear { if !skipSave { commit() } }
        .dsDeleteAlert($deleteRequest)
        .sheet(item: $pick) { p in
            let current = p.isArrival ? arrival : departure
            DSDateTimeSheet(
                title: p.isArrival ? (isPassing ? "Проследование" : "Прибытие") : "Отправление",
                timeZone: TimeZone(secondsFromGMT: offset) ?? .current,
                initial: current.map { TimeFormatter.msToDate($0) } ?? Date(),
                onDone: { ms in
                    if p.isArrival { arrival = ms } else { departure = ms }
                    pick = nil
                },
                onCancel: { pick = nil }
            )
            .presentationDetents([.height(380)])
        }
    }

    private var flags: some View {
        VStack(spacing: 0) {
            Toggle("Конечная", isOn: $isFinal)
                .padding(.horizontal, DSMetrics.rowPadX).padding(.vertical, 10)
            DSDivider()
            Toggle("Проходная", isOn: $isPassing)
                .padding(.horizontal, DSMetrics.rowPadX).padding(.vertical, 10)
        }
        .font(DSFont.sans(16))
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
    }

    /// «Изменить данные поезда» (кроме первой станции) → история версий (§6.7).
    private var dataBlock: some View {
        VStack(alignment: .leading, spacing: 10) {
            Button { showData.toggle() } label: {
                HStack {
                    Text("Изменить данные поезда").font(DSFont.sans(15, .semibold)).foregroundColor(DSColor.accent)
                    Spacer()
                    (showData ? DSIcon.chevronDown : DSIcon.chevronRight).image
                        .font(.system(size: 13, weight: .semibold)).foregroundColor(DSColor.textFaint)
                }
            }
            .buttonStyle(.plain)
            if showData {
                HStack(spacing: 8) {
                    TrainLabeledField(title: "Вес, т", placeholder: "—", text: $dWeight, keyboard: .numbersAndPunctuation, mono: true)
                    TrainLabeledField(title: "Оси", placeholder: "—", text: $dAxle, keyboard: .numbersAndPunctuation, mono: true)
                    TrainLabeledField(title: "У.Д.", placeholder: "—", text: $dLength, keyboard: .numbersAndPunctuation, mono: true)
                }
                DSTonalButton(title: "Сохранить новые данные", tone: .accent) {
                    vm.addDataVersion(stationId: stationId, stationName: name.isEmpty ? nil : name,
                                      weight: dWeight, axle: dAxle, length: dLength)
                    showData = false
                }
            }
        }
        .padding(DSMetrics.rowPadX)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
    }

    private func load() {
        guard !loaded else { return }
        loaded = true
        if stations.indices.contains(index) {
            let s = stations[index]
            name = s.name
            track = s.trackNumber
            arrival = s.arrival?.int64Value
            departure = s.departure?.int64Value
            isFinal = s.isFinal
            isPassing = s.isPassing
        }
        // BUG-26 (паритет с Android): для новой станции версия ищется по stationId == nil.
        let prefill = vm.dataPrefill(stationId: isNew ? nil : stationId)
        dWeight = prefill.weight
        dAxle = prefill.axle
        dLength = prefill.conditionalLength
    }

    private func commit() {
        vm.saveStation(
            index: index, newStationId: newStationId, name: name.trimmingCharacters(in: .whitespaces), track: track,
            arrival: arrival, departure: departure, isFinal: isFinal, isPassing: isPassing
        )
    }
}

// MARK: - Перегон

struct TrainSegmentSheet: View {
    @ObservedObject var vm: TrainScreenViewModelWrapper
    let state0: TrainScreenState
    private var state: TrainScreenState { vm.state ?? state0 }
    /// Перегон между станциями `afterIndex` и `afterIndex + 1` (исходный порядок).
    let afterIndex: Int

    @Environment(\.dismiss) private var dismiss
    @State private var loaded = false
    @State private var from = ""
    @State private var to = ""
    @State private var track = ""
    @State private var notes = ""

    private var stations: [TrainStationUi] { (state.stations as? [TrainStationUi]) ?? [] }

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Перегон", subtitle: nil), onClose: { dismiss() }) {
            VStack(alignment: .leading, spacing: 14) {
                TrainStationNameField(title: "От", text: $from, suggest: { vm.suggestStations($0) }, onRemove: { vm.removeStationSuggestion($0) })
                TrainStationNameField(title: "До", text: $to, suggest: { vm.suggestStations($0) }, onRemove: { vm.removeStationSuggestion($0) })
                VStack(alignment: .leading, spacing: 6) {
                    Text("Путь").dsTextStyle(.captionMuted)
                    HStack(spacing: 8) {
                        TextField("№", text: $track)
                            .font(DSFont.sans(16))
                            .padding(.horizontal, 14)
                            .frame(maxWidth: .infinity, minHeight: 48)
                            .background(DSColor.surface)
                            .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous).stroke(DSColor.border, lineWidth: 1))
                            .onChange(of: track) { if $0.count > 6 { track = String($0.prefix(6)) } }
                        ForEach(["I", "II", "III"], id: \.self) { numeral in
                            Button { track = numeral } label: {
                                Text(numeral)
                                    .font(DSFont.sans(15, .semibold))
                                    .foregroundColor(track == numeral ? DSColor.accentInk : DSColor.text)
                                    .frame(maxWidth: .infinity, minHeight: 48)
                                    .background(track == numeral ? DSColor.accent : DSColor.surface)
                                    .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                                    .overlay(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous).stroke(DSColor.border, lineWidth: 1))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text("Примечание").dsTextStyle(.captionMuted)
                    ZStack(alignment: .topLeading) {
                        if notes.isEmpty {
                            Text("Например: по неправильному").font(DSFont.sans(16)).foregroundColor(DSColor.textFaint)
                                .padding(.horizontal, 18).padding(.vertical, 14)
                        }
                        TextEditor(text: $notes)
                            .font(DSFont.sans(16))
                            .scrollContentBackground(.hidden)
                            .padding(.horizontal, 10).padding(.vertical, 6)
                            .frame(minHeight: 96)
                    }
                    .background(DSColor.surface)
                    .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous).stroke(DSColor.border, lineWidth: 1))
                }
                DSCTAButton(title: "Готово") { dismiss() }
            }
        }
        .onAppear {
            guard !loaded else { return }
            loaded = true
            if stations.indices.contains(afterIndex) { from = stations[afterIndex].name }
            if stations.indices.contains(afterIndex + 1) {
                let next = stations[afterIndex + 1]
                to = next.name
                track = next.segmentTrack
                notes = next.segmentNotes
            }
        }
        // Закрытие любым способом = сохранение (§6.5).
        .onDisappear {
            vm.saveSegment(afterIndex: afterIndex, from: from.trimmingCharacters(in: .whitespaces),
                           to: to.trimmingCharacters(in: .whitespaces), track: track, notes: notes)
        }
    }
}
