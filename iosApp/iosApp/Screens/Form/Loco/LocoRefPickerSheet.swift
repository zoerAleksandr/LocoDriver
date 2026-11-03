import SwiftUI
import ComposeApp

/// Пикер серии/станции внутри шторки времени (SCREEN_SPECS §2.10): поиск, группы,
/// выбор / снятие выбора, редактирование и создание прямо в этой же шторке.
struct LocoRefPickerSheet: View {
    let request: LocoTimeSheetView.RefPickerRequest
    @ObservedObject var sheet: LocoTimeSheetViewModelWrapper
    let onClose: () -> Void

    private enum Mode: Equatable {
        case list
        case edit(id: String?)
    }

    @State private var mode: Mode = .list
    @State private var query = ""
    @State private var didOpenInitialEditor = false

    private var isSeries: Bool { request.kind == .series }

    var body: some View {
        VStack(spacing: 0) {
            switch mode {
            case .list: listView
            case let .edit(id):
                if isSeries {
                    LocoSeriesEditor(sheet: sheet, id: id) { mode = .list }
                } else {
                    LocoStationEditor(sheet: sheet, id: id) { mode = .list }
                }
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .onAppear {
            // «Настроить серию/станцию» открывает пикер сразу в редакторе (§2.10).
            if !didOpenInitialEditor, let id = request.editId {
                didOpenInitialEditor = true
                mode = .edit(id: id)
            }
        }
    }

    private var listView: some View {
        VStack(spacing: 0) {
            ZStack {
                Text(isSeries ? "Серия" : "Станция").dsTextStyle(.navTitle)
                HStack {
                    Spacer()
                    Button("Готово", action: onClose)
                        .font(DSFont.sans(17, .semibold))
                        .foregroundColor(DSColor.accent)
                }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.top, 18)
            .padding(.bottom, 10)

            TextField(isSeries ? "Поиск серии..." : "Поиск станции...", text: $query)
                .padding(.horizontal, 14).frame(minHeight: DSMetrics.inputMinH)
                .background(DSColor.bgSubtle)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                .padding(.horizontal, DSMetrics.screenPadX)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HStack {
                        DSTextAction(title: isSeries ? "+ Добавить серию" : "+ Добавить станцию") { mode = .edit(id: nil) }
                        Spacer()
                    }
                    .padding(.horizontal, 4).padding(.top, 14)
                    if isSeries { seriesGroups } else { stationGroups }
                }
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.bottom, 28)
            }
        }
        .id(sheet.state.catalogVersion)
    }

    // MARK: Серии

    @ViewBuilder
    private var seriesGroups: some View {
        let rows = sheet.seriesRows(query)
        if rows.isEmpty && query.isEmpty {
            Text("Нет серий. Добавьте в Настройках → Серии.").font(DSFont.sans(14)).foregroundColor(DSColor.textMuted).padding(.top, 16)
        } else {
            group("ЭЛЕКТРОВОЗЫ", rows.filter { $0.isElectric })
            group("ТЕПЛОВОЗЫ", rows.filter { !$0.isElectric })
        }
    }

    @ViewBuilder
    private func group(_ title: String, _ rows: [SeriesRowUi]) -> some View {
        if !rows.isEmpty {
            DSGroupHeader(title)
            DSCard {
                ForEach(Array(rows.enumerated()), id: \.element.id) { i, row in
                    refRow(name: row.name, mono: true, sub: row.normsText, selected: row.isSelected,
                           onTap: { sheet.selectSeries(row.id); onClose() }, onEdit: { mode = .edit(id: row.id) })
                    if i < rows.count - 1 { DSDivider() }
                }
            }
        }
    }

    // MARK: Станции

    @ViewBuilder
    private var stationGroups: some View {
        let rows = sheet.stationRows(query)
        if rows.isEmpty && query.isEmpty {
            Text("Нет станций. Добавьте в Настройках → Станции.").font(DSFont.sans(14)).foregroundColor(DSColor.textMuted).padding(.top, 16)
        } else {
            stationGroup("С НОРМАМИ", rows.filter { $0.hasNorms })
            stationGroup("БЕЗ НОРМ", rows.filter { !$0.hasNorms })
        }
    }

    @ViewBuilder
    private func stationGroup(_ title: String, _ rows: [StationRowUi]) -> some View {
        if !rows.isEmpty {
            DSGroupHeader(title)
            DSCard {
                ForEach(Array(rows.enumerated()), id: \.element.id) { i, row in
                    refRow(name: row.name, mono: false, sub: row.normsText, selected: row.isSelected,
                           onTap: { sheet.selectStation(row.id); onClose() }, onEdit: { mode = .edit(id: row.id) })
                    if i < rows.count - 1 { DSDivider() }
                }
            }
        }
    }

    private func refRow(name: String, mono: Bool, sub: String, selected: Bool,
                        onTap: @escaping () -> Void, onEdit: @escaping () -> Void) -> some View {
        HStack(spacing: 10) {
            Button(action: onTap) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(name).font(mono ? DSFont.mono(16, .medium) : DSFont.sans(16, .medium)).foregroundColor(DSColor.text)
                    Text(sub).dsTextStyle(.captionMuted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            Button(action: onEdit) {
                Image(systemName: "pencil").font(.system(size: 15)).foregroundColor(DSColor.textMuted).frame(width: 36, height: 36)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Редактировать"))
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, 10)
        .background(selected ? DSColor.accentSoft : Color.clear)
    }
}

// MARK: Редакторы

private struct EditorHeader: View {
    let title: String
    let onDone: () -> Void
    var body: some View {
        ZStack {
            Text(title).dsTextStyle(.navTitle)
            HStack {
                Spacer()
                Button("Готово", action: onDone).font(DSFont.sans(17, .semibold)).foregroundColor(DSColor.accent)
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.top, 18).padding(.bottom, 10)
    }
}

private struct EditorField: View {
    let label: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).dsTextStyle(.group)
            TextField("", text: $text)
                .keyboardType(keyboard)
                .padding(.horizontal, 14).frame(minHeight: DSMetrics.inputMinH)
                .background(DSColor.bgSubtle)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
        }
    }
}

struct LocoSeriesEditor: View {
    @ObservedObject var sheet: LocoTimeSheetViewModelWrapper
    @State var id: String?
    let onDone: () -> Void

    @State private var name = ""
    @State private var isElectric = true
    @State private var acc = ""
    @State private var del = ""
    @State private var accH = ""
    @State private var delH = ""
    @State private var letters = false
    @State private var loaded = false
    @State private var saveTask: Task<Void, Never>?

    init(sheet: LocoTimeSheetViewModelWrapper, id: String?, onDone: @escaping () -> Void) {
        self.sheet = sheet
        self._id = State(initialValue: id)
        self.onDone = onDone
    }

    var body: some View {
        VStack(spacing: 0) {
            EditorHeader(title: id == nil ? "Новая серия" : "Серия") { commit(); onDone() }
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    EditorField(label: "НАЗВАНИЕ", text: $name)
                    Picker("", selection: $isElectric) {
                        Text("Электровоз").tag(true)
                        Text("Тепловоз").tag(false)
                    }.pickerStyle(.segmented)
                    Text("ПОСЛЕ ОТСТОЯ, МИН").dsTextStyle(.group)
                    HStack(spacing: 10) {
                        EditorField(label: "ПРИЁМКА", text: $acc, keyboard: .numberPad)
                        EditorField(label: "СДАЧА", text: $del, keyboard: .numberPad)
                    }
                    Text("ИЗ РУК В РУКИ, МИН").dsTextStyle(.group)
                    HStack(spacing: 10) {
                        EditorField(label: "ПРИЁМКА", text: $accH, keyboard: .numberPad)
                        EditorField(label: "СДАЧА", text: $delH, keyboard: .numberPad)
                    }
                    Toggle("Секции буквами (А, Б, В…)", isOn: $letters)
                }
                .padding(DSMetrics.screenPadX)
            }
        }
        .onAppear {
            guard !loaded else { return }
            loaded = true
            let e = sheet.seriesEditor(id)
            name = e.name; isElectric = e.isElectric
            acc = e.acceptanceDuration; del = e.deliveryDuration
            accH = e.acceptanceHandToHand; delH = e.deliveryHandToHand
            letters = e.numberingLetters
        }
        .onChange(of: name) { _ in schedule() }
        .onChange(of: isElectric) { _ in schedule() }
        .onChange(of: acc) { _ in schedule() }
        .onChange(of: del) { _ in schedule() }
        .onChange(of: accH) { _ in schedule() }
        .onChange(of: delH) { _ in schedule() }
        .onChange(of: letters) { _ in schedule() }
    }

    /// Автосохранение через 500 мс после изменения (§2.10).
    private func schedule() {
        guard loaded else { return }
        saveTask?.cancel()
        saveTask = Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            if !Task.isCancelled { commit() }
        }
    }

    private func commit() {
        saveTask?.cancel()
        let newId = sheet.saveSeries(id: id, name: name, isElectric: isElectric, acc: acc, del: del, accH: accH, delH: delH, letters: letters)
        if !newId.isEmpty { id = newId }
    }
}

struct LocoStationEditor: View {
    @ObservedObject var sheet: LocoTimeSheetViewModelWrapper
    @State var id: String?
    let onDone: () -> Void

    @State private var name = ""
    @State private var a = ""
    @State private var b = ""
    @State private var c = ""
    @State private var d = ""
    @State private var loaded = false
    @State private var saveTask: Task<Void, Never>?

    init(sheet: LocoTimeSheetViewModelWrapper, id: String?, onDone: @escaping () -> Void) {
        self.sheet = sheet
        self._id = State(initialValue: id)
        self.onDone = onDone
    }

    var body: some View {
        VStack(spacing: 0) {
            EditorHeader(title: id == nil ? "Новая станция" : "Станция") { commit(); onDone() }
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    EditorField(label: "НАЗВАНИЕ", text: $name)
                    Text("ПРИЁМКА, МИН").dsTextStyle(.group)
                    HStack(spacing: 10) {
                        EditorField(label: "ЯВКА → НАЧАЛО", text: $a, keyboard: .numberPad)
                        EditorField(label: "КОНЕЦ → КП", text: $b, keyboard: .numberPad)
                    }
                    Text("СДАЧА, МИН").dsTextStyle(.group)
                    HStack(spacing: 10) {
                        EditorField(label: "КП → НАЧАЛО", text: $c, keyboard: .numberPad)
                        EditorField(label: "КОНЕЦ → РАБОТА", text: $d, keyboard: .numberPad)
                    }
                }
                .padding(DSMetrics.screenPadX)
            }
        }
        .onAppear {
            guard !loaded else { return }
            loaded = true
            let e = sheet.stationEditor(id)
            name = e.name; a = e.appearanceToStart; b = e.endToBarrier; c = e.barrierToStart; d = e.endToWorkEnd
        }
        .onChange(of: name) { _ in schedule() }
        .onChange(of: a) { _ in schedule() }
        .onChange(of: b) { _ in schedule() }
        .onChange(of: c) { _ in schedule() }
        .onChange(of: d) { _ in schedule() }
    }

    private func schedule() {
        guard loaded else { return }
        saveTask?.cancel()
        saveTask = Task {
            try? await Task.sleep(nanoseconds: 500_000_000)
            if !Task.isCancelled { commit() }
        }
    }

    private func commit() {
        saveTask?.cancel()
        let newId = sheet.saveStation(id: id, name: name, a: a, b: b, c: c, d: d)
        if !newId.isEmpty { id = newId }
    }
}
