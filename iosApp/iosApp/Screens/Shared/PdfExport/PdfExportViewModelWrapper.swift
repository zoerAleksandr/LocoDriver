import SwiftUI
import ComposeApp

/// Обёртка `PdfExportIosViewModel` (SCREEN_SPECS §21) для SwiftUI.
///
/// Пайплайн «Сформировать»: Kotlin собирает модель вёрстки (8 с на данные расчёта,
/// §21 п.3) → Swift рендерит PDF (UIGraphicsPDFRenderer) в кэш → шторка
/// «PDF сформирован» (`AppSheet.pdfActions`). Ошибка → snackbar на Главном.
@MainActor
final class PdfExportViewModelWrapper: ObservableObject {
    static let shared = PdfExportViewModelWrapper()

    private let viewModel = IosViewModelHelper.shared.getPdfExportViewModel()

    /// true во время генерации (Kotlin) и рендера файла — карточка «PDF» неактивна (§21).
    @Published var isGenerating = false
    /// Однократная ошибка для snackbar Главного («Ошибка формирования PDF: …», §21 п.5).
    @Published var errorMessage: String?
    /// Последний сформированный файл (кэш приложения, §21 «Файл»).
    @Published var generatedFileURL: URL?

    private var isRendering = false

    private init() {
        viewModel.watchIsGenerating { [weak self] generating in
            Task { @MainActor in
                guard let self else { return }
                self.isGenerating = self.isRendering || generating.boolValue
            }
        }
    }

    /// Выбор разделов шторки «Содержание PDF» (переживает пересоздание шторки — singleton VM).
    var sections: PdfSectionsModel {
        get { viewModel.sections }
        set { viewModel.sections = newValue }
    }

    func generate(
        includeRouteDetails: Bool,
        includeSchedule: Bool,
        includeSalary: Bool
    ) {
        guard !isGenerating else { return } // повторный вызов игнорируется (§21 п.2)
        isGenerating = true
        isRendering = true
        errorMessage = nil
        let sections = PdfSectionsModel(
            includeRouteDetails: includeRouteDetails,
            includeSchedule: includeSchedule,
            includeSalary: includeSalary
        )
        viewModel.generate(sections: sections) { [weak self] result in
            Task { @MainActor in
                guard let self else { return }
                if let document = result.document {
                    self.render(document: document)
                } else {
                    self.isRendering = false
                    self.isGenerating = false
                    self.errorMessage = result.errorMessage
                }
            }
        }
    }

    private func render(document: PdfLayoutDocument) {
        Task.detached(priority: .userInitiated) { [weak self] in
            let url = PdfRenderer.writeToFile(document: document)
            await MainActor.run {
                guard let self else { return }
                self.isRendering = false
                self.isGenerating = false
                if let url {
                    self.generatedFileURL = url
                    // Успех → шторка «PDF сформирован» (§21 п.4).
                    AppRouter.shared.present(.pdfActions)
                } else {
                    self.errorMessage = "Ошибка формирования PDF: не удалось записать файл"
                }
            }
        }
    }
}
