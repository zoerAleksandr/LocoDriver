import SwiftUI
import ComposeApp

// Экраны справочников норм (SCREEN_SPECS §17.4): списки «Серии» / «Станции» и экраны-редакторы.
// Макеты: design/src/norms-screens.jsx → IOSSeriesList / IOSStationsList / IOSSeriesEditor / IOSStationEditor.
// Навигация — AppRoute.settingsSection(.seriesList/.stationList/.seriesEditor/.seriesNew/.stationEditor).

struct SeriesListView: View {
    @StateObject private var vm = SeriesListViewModelWrapper()
    @State private var query = ""
    @State private var deleteRequest: DSDeleteRequest?

    var body: some View {
        VStack(spacing: 0) {
            DSNavBar(title: "Серии", style: .text(backLabel: "Настройки"), onBack: { AppRouter.shared.back() })
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    NormsHint(text: "Для каждой серии задаются нормы длительности приёмки и сдачи. Нормы применяются автоматически при заполнении времени в шторке локомотива.")
                    NormsSearchField(placeholder: "Поиск серии...", text: $query).padding(.top, 12)
                    if let rows = vm.rows {
                        let filtered = rows.filter { query.isEmpty || $0.name.localizedCaseInsensitiveContains(query) }
                        if rows.isEmpty {
                            Text("Нет серий. Добавьте первую.").font(DSFont.sans(14)).foregroundColor(DSColor.textMuted).padding(.top, 20)
                        }
                        group("ЭЛЕКТРОВОЗЫ", filtered.filter { $0.isElectric })
                        group("ТЕПЛОВОЗЫ", filtered.filter { !$0.isElectric })
                    }
                    DSTonalButton(title: "Добавить серию", icon: .plus, tone: .subtle) {
                        AppRouter.shared.showSettingsSection(.seriesNew(name: ""))
                    }
                    .padding(.top, 16)
                }
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.top, 12).padding(.bottom, 32)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .dsDeleteAlert($deleteRequest)
    }

    @ViewBuilder
    private func group(_ title: String, _ rows: [NormsSeriesRowUi]) -> some View {
        if !rows.isEmpty {
            DSGroupHeader("\(title) · \(rows.count)")
            DSCard {
                ForEach(Array(rows.enumerated()), id: \.element.id) { i, row in
                    Button { AppRouter.shared.showSettingsSection(.seriesEditor(id: row.id)) } label: {
                        HStack(spacing: 14) {
                            Text(row.name)
                                .font(DSFont.mono(13, .bold)).foregroundColor(DSColor.accent)
                                .padding(.horizontal, 8).padding(.vertical, 4)
                                .frame(minWidth: 56)
                                .background(DSColor.accentSoft)
                                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                            Text(row.normsText)
                                .font(DSFont.mono(13))
                                .foregroundColor(row.hasNorms ? DSColor.text : DSColor.warning)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            DSIcon.chevronRight.image.font(.system(size: 13, weight: .semibold)).foregroundColor(DSColor.textFaint)
                        }
                        .padding(.horizontal, 18).padding(.vertical, 14)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(DSRowPressStyle())
                    .contextMenu {
                        Button(role: .destructive) {
                            deleteRequest = DSDeleteRequest(title: "Удалить серию?") { vm.delete(row.id) }
                        } label: { Label("Удалить", systemImage: "trash") }
                    }
                    if i < rows.count - 1 { DSDivider() }
                }
            }
        }
    }
}

struct StationListView: View {
    @StateObject private var vm = StationListViewModelWrapper()
    @State private var query = ""
    @State private var deleteRequest: DSDeleteRequest?

    var body: some View {
        VStack(spacing: 0) {
            DSNavBar(title: "Станции", style: .text(backLabel: "Настройки"), onBack: { AppRouter.shared.back() })
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    NormsHint(text: "Для каждой станции задаются 4 интервала: от явки до начала приёмки, от окончания приёмки до выхода на КП, от захода на КП до начала сдачи, от окончания сдачи до окончания работы.")
                    NormsSearchField(placeholder: "Поиск станции...", text: $query).padding(.top, 12)
                    if let rows = vm.rows {
                        let filtered = rows.filter { query.isEmpty || $0.name.localizedCaseInsensitiveContains(query) }
                        if rows.isEmpty {
                            Text("Нет станций. Добавьте первую.").font(DSFont.sans(14)).foregroundColor(DSColor.textMuted).padding(.top, 20)
                        }
                        group("С НОРМАМИ", filtered.filter { $0.hasNorms })
                        group("БЕЗ НОРМ", filtered.filter { !$0.hasNorms })
                    }
                    DSTonalButton(title: "Добавить станцию", icon: .plus, tone: .subtle) {
                        AppRouter.shared.showSettingsSection(.stationEditor(id: ""))
                    }
                    .padding(.top, 16)
                }
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.top, 12).padding(.bottom, 32)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .dsDeleteAlert($deleteRequest)
    }

    @ViewBuilder
    private func group(_ title: String, _ rows: [NormsStationRowUi]) -> some View {
        if !rows.isEmpty {
            DSGroupHeader("\(title) · \(rows.count)")
            DSCard {
                ForEach(Array(rows.enumerated()), id: \.element.id) { i, row in
                    Button { AppRouter.shared.showSettingsSection(.stationEditor(id: row.id)) } label: {
                        HStack(spacing: 14) {
                            DSIcon.mapPin.image.font(.system(size: 16))
                                .foregroundColor(row.hasCoordinates ? DSColor.accent : DSColor.textFaint)
                                .frame(width: 36, height: 36)
                                .background(DSColor.bgSubtle)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            VStack(alignment: .leading, spacing: 3) {
                                Text(row.name).font(DSFont.sans(14, .medium)).foregroundColor(DSColor.text)
                                Text(row.normsText).font(DSFont.mono(12))
                                    .foregroundColor(row.normsText.hasPrefix("Норма") ? DSColor.warning : DSColor.textMuted)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            DSIcon.chevronRight.image.font(.system(size: 13, weight: .semibold)).foregroundColor(DSColor.textFaint)
                        }
                        .padding(.horizontal, 18).padding(.vertical, 12)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(DSRowPressStyle())
                    .contextMenu {
                        Button(role: .destructive) {
                            deleteRequest = DSDeleteRequest(title: "Удалить станцию?") { vm.delete(row.id) }
                        } label: { Label("Удалить", systemImage: "trash") }
                    }
                    if i < rows.count - 1 { DSDivider() }
                }
            }
        }
    }
}

/// Экран редактора серии в стеке Настроек. `id == nil` — новая серия; `name` — «старая» серия по имени.
struct SeriesEditorScreen: View {
    @StateObject private var vm: SeriesEditorViewModelWrapper

    init(id: String?, name: String?) {
        _vm = StateObject(wrappedValue: SeriesEditorViewModelWrapper(id: id, initialName: name))
    }

    var body: some View {
        VStack(spacing: 0) {
            DSNavBar(title: title, style: .text(backLabel: "Серии"), onBack: { vm.commit(); AppRouter.shared.back() }) {
                Button("Готово") { vm.commit(); AppRouter.shared.back() }
                    .font(DSFont.sans(16, .semibold)).foregroundColor(DSColor.accent)
            }
            SeriesEditorContent(vm: vm) { AppRouter.shared.back() }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
    }

    private var title: String {
        let n = vm.state?.name ?? ""
        return n.isEmpty ? "Новая серия" : n
    }
}

struct StationEditorScreen: View {
    @StateObject private var vm: StationEditorViewModelWrapper

    init(id: String?) {
        _vm = StateObject(wrappedValue: StationEditorViewModelWrapper(id: id, initialName: nil))
    }

    var body: some View {
        VStack(spacing: 0) {
            DSNavBar(title: title, style: .text(backLabel: "Станции"), onBack: { vm.commit(); AppRouter.shared.back() }) {
                Button("Готово") { vm.commit(); AppRouter.shared.back() }
                    .font(DSFont.sans(16, .semibold)).foregroundColor(DSColor.accent)
            }
            StationEditorContent(vm: vm) { AppRouter.shared.back() }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
    }

    private var title: String {
        let n = vm.state?.name ?? ""
        return n.isEmpty ? "Новая станция" : n
    }
}

struct NormsSearchField: View {
    let placeholder: String
    @Binding var text: String
    var body: some View {
        TextField(placeholder, text: $text)
            .padding(.horizontal, 14).frame(minHeight: DSMetrics.inputMinH)
            .background(DSColor.bgSubtle)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
    }
}
