import SwiftUI
import ComposeApp

// Обёртки справочников норм (SCREEN_SPECS §17.4, §2.10) над Kotlin
// SeriesListIosViewModel / StationListIosViewModel / SeriesEditorIosViewModel / StationEditorIosViewModel.

@MainActor
final class SeriesListViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createSeriesListViewModel()
    /// nil — ещё не загружено.
    @Published var rows: [NormsSeriesRowUi]?

    init() {
        viewModel.watchRows { [weak self] list in
            DispatchQueue.main.async { self?.rows = list as? [NormsSeriesRowUi] }
        }
    }

    deinit { viewModel.dispose() }

    func delete(_ id: String) { viewModel.delete(id: id) }
}

@MainActor
final class StationListViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.createStationListViewModel()
    @Published var rows: [NormsStationRowUi]?

    init() {
        viewModel.watchRows { [weak self] list in
            DispatchQueue.main.async { self?.rows = list as? [NormsStationRowUi] }
        }
    }

    deinit { viewModel.dispose() }

    func delete(_ id: String) { viewModel.delete(id: id) }
}

@MainActor
final class SeriesEditorViewModelWrapper: ObservableObject {
    private let viewModel: SeriesEditorIosViewModel
    @Published var state: SeriesEditorStateUi?

    /// `onRenamed(старое, новое)`, `onDeleted(имя)` — для синхронизации с формой локомотива (§2.12).
    init(id: String?, initialName: String?, onRenamed: @escaping (String, String) -> Void = { _, _ in },
         onDeleted: @escaping (String) -> Void = { _ in }) {
        viewModel = IosViewModelHelper.shared.createSeriesEditorViewModel(id: id, initialName: initialName)
        viewModel.setCallbacks(
            onRenamed: { o, n in DispatchQueue.main.async { onRenamed(o, n) } },
            onDeleted: { n in DispatchQueue.main.async { onDeleted(n) } }
        )
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    deinit { viewModel.dispose() }

    func setName(_ v: String) { viewModel.setName(value: v) }
    func setElectric(_ v: Bool) { viewModel.setElectric(value: v) }
    func setNumberingLetters(_ v: Bool) { viewModel.setNumberingLetters(value: v) }
    func setNorm(_ field: Int, _ text: String) { viewModel.setNorm(field: Int32(field), text: text) }
    func setPassport(_ field: Int, _ text: String) { viewModel.setPassport(field: Int32(field), text: text) }
    func save() { viewModel.save() }
    func commit() { viewModel.commit() }
    func delete() { viewModel.delete() }
}

@MainActor
final class StationEditorViewModelWrapper: ObservableObject {
    private let viewModel: StationEditorIosViewModel
    @Published var state: StationEditorStateUi?

    init(id: String?, initialName: String?, onDeleted: @escaping (String) -> Void = { _ in }) {
        viewModel = IosViewModelHelper.shared.createStationEditorViewModel(id: id, initialName: initialName)
        viewModel.setCallbacks(onDeleted: { i in DispatchQueue.main.async { onDeleted(i) } })
        viewModel.watchState { [weak self] s in
            DispatchQueue.main.async { self?.state = s }
        }
    }

    deinit { viewModel.dispose() }

    func setName(_ v: String) { viewModel.setName(value: v) }
    func setNorm(_ field: Int, _ text: String) { viewModel.setNorm(field: Int32(field), text: text) }
    func setAxisKm(_ t: String) { viewModel.setAxisKm(text: t) }
    func setAxisPicket(_ t: String) { viewModel.setAxisPicket(text: t) }
    func save() { viewModel.save() }
    func commit() { viewModel.commit() }
    func delete() { viewModel.delete() }
}
