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
            .padding(.top, 30)
            .padding(.bottom, 10)

            TextField(isSeries ? "Поиск серии..." : "Поиск станции...", text: $query)
                .padding(.horizontal, 14).frame(minHeight: DSMetrics.inputMinH)
                .background(DSColor.bgSubtle)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                .padding(.horizontal, DSMetrics.screenPadX)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    DSTonalButton(title: isSeries ? "Добавить серию" : "Добавить станцию", icon: .plus, tone: .subtle) {
                        mode = .edit(id: nil)
                    }
                    .padding(.top, 14)
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

// MARK: Редакторы (общие с Настройками, §17.4)

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
        .padding(.top, 30).padding(.bottom, 10)
    }
}

/// Редактор серии в пикере: `SeriesEditorContent` из Настроек; «Готово» — commit и возврат к списку.
struct LocoSeriesEditor: View {
    @ObservedObject var sheet: LocoTimeSheetViewModelWrapper
    @StateObject private var vm: SeriesEditorViewModelWrapper
    let onDone: () -> Void

    init(sheet: LocoTimeSheetViewModelWrapper, id: String?, onDone: @escaping () -> Void) {
        self.sheet = sheet
        self.onDone = onDone
        // Переименование выбранной серии и её удаление — обратно в форму локомотива (§2.12).
        _vm = StateObject(wrappedValue: SeriesEditorViewModelWrapper(
            id: id, initialName: nil,
            onRenamed: { old, new in sheet.seriesRenamed(old: old, new: new) },
            onDeleted: { name in sheet.seriesRemoved(name) }
        ))
    }

    var body: some View {
        VStack(spacing: 0) {
            EditorHeader(title: (vm.state?.isNewRecord ?? true) && (vm.state?.name ?? "").isEmpty ? "Новая серия" : "Серия") {
                vm.commit(); onDone()
            }
            SeriesEditorContent(vm: vm, onDeleted: onDone, showsHint: false)
        }
    }
}

struct LocoStationEditor: View {
    @StateObject private var vm: StationEditorViewModelWrapper
    let onDone: () -> Void

    init(sheet: LocoTimeSheetViewModelWrapper, id: String?, onDone: @escaping () -> Void) {
        self.onDone = onDone
        _vm = StateObject(wrappedValue: StationEditorViewModelWrapper(id: id, initialName: nil))
    }

    var body: some View {
        VStack(spacing: 0) {
            EditorHeader(title: (vm.state?.name ?? "").isEmpty ? "Новая станция" : "Станция") {
                vm.commit(); onDone()
            }
            StationEditorContent(vm: vm, onDeleted: onDone, showsHint: false)
        }
    }
}
