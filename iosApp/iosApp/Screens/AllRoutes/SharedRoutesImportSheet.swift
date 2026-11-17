import SwiftUI
import ComposeApp

struct SharedRoutesImportSheet: View {
    @ObservedObject var viewModel: SharedRoutesImportViewModelWrapper
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(
            header: .titleClose(
                title: viewModel.routes.count == 1 ? "Получен маршрут" : "Получены маршруты",
                subtitle: viewModel.isLoading ? "Загрузка…" : "К импорту: \(viewModel.routes.count)"
            ),
            onClose: {
                if !viewModel.isSaving { onClose() }
            }
        ) {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 36)
            }

            if let message = viewModel.errorMessage {
                Text(message)
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.textMuted)
                    .padding(.horizontal, 4)
                    .padding(.bottom, 10)
            }

            ForEach(viewModel.routes, id: \.basicData.id) { route in
                AllRouteCard(route: route)
                .padding(.bottom, 8)
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            HStack(spacing: DSSpacing.md) {
                DSTonalButton(
                    title: "Отмена",
                    isEnabled: !viewModel.isSaving,
                    action: {
                        if !viewModel.isSaving { onClose() }
                    }
                )
                DSCTAButton(
                    title: "Сохранить",
                    isLoading: viewModel.isSaving,
                    isEnabled: !viewModel.isLoading && !viewModel.routes.isEmpty,
                    action: viewModel.save
                )
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.top, 10)
            .padding(.bottom, 12)
            .background(DSColor.bg)
        }
        .interactiveDismissDisabled(viewModel.isSaving)
    }
}
