import SwiftUI
import ComposeApp
import QuickLook

// Шторка «Содержание PDF» (SCREEN_SPECS §21 сценарий п.1): три чекбокса, все
// по умолчанию включены (значения переживают пересоздание шторки — singleton VM),
// действие «Сформировать». Кнопка доступна и при всех выключенных чекбоксах.

struct PdfContentSheetView: View {
    let onClose: () -> Void

    @ObservedObject private var pdf = PdfExportViewModelWrapper.shared

    @State private var includeRouteDetails = true
    @State private var includeSchedule = true
    @State private var includeSalary = true

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Содержание PDF", subtitle: nil), onClose: onClose) {
            VStack(alignment: .leading, spacing: 0) {
                checkboxRow(
                    label: "Поездки детально",
                    checked: $includeRouteDetails
                )
                checkboxRow(
                    label: "График",
                    checked: $includeSchedule
                )
                checkboxRow(
                    label: "Расчётный листок",
                    checked: $includeSalary
                )
            }
            .padding(.top, DSSpacing.xs)

            DSCTAButton(title: "Сформировать") {
                // §24.1: действие сначала закрывает шторку, затем выполняется.
                let sections = PdfSectionsModel(
                    includeRouteDetails: includeRouteDetails,
                    includeSchedule: includeSchedule,
                    includeSalary: includeSalary
                )
                pdf.sections = sections
                onClose()
                pdf.generate(
                    includeRouteDetails: includeRouteDetails,
                    includeSchedule: includeSchedule,
                    includeSalary: includeSalary
                )
            }
            .padding(.top, DSSpacing.lg)
        }
        .onAppear {
            // Значения переживают поворот/пересоздание шторки (§21 п.1).
            includeRouteDetails = pdf.sections.includeRouteDetails
            includeSchedule = pdf.sections.includeSchedule
            includeSalary = pdf.sections.includeSalary
        }
    }

    private func checkboxRow(label: String, checked: Binding<Bool>) -> some View {
        Button {
            checked.wrappedValue.toggle()
        } label: {
            HStack(spacing: DSSpacing.sm) {
                Image(systemName: checked.wrappedValue ? "checkmark.square.fill" : "square")
                    .font(.system(size: 22))
                    .foregroundColor(checked.wrappedValue ? DSColor.accent : DSColor.textMuted)
                Text(label)
                    .dsTextStyle(.body)
                Spacer(minLength: 0)
            }
            .padding(.vertical, 6)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// Шторка «PDF сформирован» (§21 сценарий п.4): «Открыть PDF» (QuickLook) и
// «Поделиться» (share sheet). Действие сначала закрывает шторку (§24.1);
// закрытие шторки файл не удаляет.

struct PdfActionSheetView: View {
    let onClose: () -> Void

    @ObservedObject private var pdf = PdfExportViewModelWrapper.shared

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "PDF сформирован", subtitle: nil), onClose: onClose) {
            VStack(alignment: .leading, spacing: 0) {
                actionRow(title: "Открыть PDF", icon: "eye") {
                    let url = pdf.generatedFileURL
                    onClose()
                    if let url {
                        AppRouter.shared.presentPdfPreview(url: url)
                    }
                }
                DSDivider(inset: 0)
                actionRow(title: "Поделиться", icon: "square.and.arrow.up") {
                    let url = pdf.generatedFileURL
                    onClose()
                    if let url {
                        AppRouter.shared.presentPdfShare(url: url)
                    }
                }
            }
            .padding(.top, DSSpacing.xs)
        }
    }

    private func actionRow(title: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: DSSpacing.sm) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(DSColor.accent)
                Text(title)
                    .dsTextStyle(.body)
                Spacer(minLength: 0)
            }
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Презентация PDF (QuickLook / share sheet)

/// Просмотр PDF («Открыть PDF») — QuickLook, платформенный аналог chooser «Открыть в…».
struct PdfPreviewView: View {
    let url: URL
    let onClose: () -> Void

    var body: some View {
        NavigationStack {
            PdfQuickLook(url: url)
                .navigationTitle(url.lastPathComponent)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Закрыть", action: onClose)
                    }
                }
        }
    }
}

private struct PdfQuickLook: UIViewControllerRepresentable {
    let url: URL

    final class Coordinator: NSObject, QLPreviewControllerDataSource {
        let url: URL
        init(url: URL) { self.url = url }
        func numberOfPreviewItems(in controller: QLPreviewController) -> Int { 1 }
        func previewController(_ controller: QLPreviewController, previewItemAt index: Int) -> QLPreviewItem {
            url as NSURL as QLPreviewItem
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(url: url) }

    func makeUIViewController(context: Context) -> QLPreviewController {
        let controller = QLPreviewController()
        controller.dataSource = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: QLPreviewController, context: Context) {}
}

/// Шаринг PDF («Поделиться») — платформенный аналог chooser «Поделиться PDF».
struct PdfShareView: View {
    let url: URL
    let onClose: () -> Void

    var body: some View {
        PdfShareSheet(items: [url])
            .onDisappear(perform: onClose)
    }
}

private struct PdfShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
