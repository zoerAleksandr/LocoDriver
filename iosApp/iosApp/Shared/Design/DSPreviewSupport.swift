import SwiftUI

/// Холст для `#Preview` компонентов кита: фон `DSColor.bg`, отступы экрана,
/// вертикальный стек. Используется только в превью.
struct DSPreviewCanvas<Content: View>: View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DSSpacing.lg) {
                content()
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.vertical, DSSpacing.xxl)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(DSColor.bg.ignoresSafeArea())
    }
}
