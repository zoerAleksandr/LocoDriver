import SwiftUI

/// Единая заглушка для ещё не реализованных экранов и шторок.
/// Показывает название и раздел SCREEN_SPECS.md, по которому экран будет сделан.
/// Волна, реализующая экран, заменяет ветку в `AppDestinationView`/`AppSheetView`
/// на настоящий экран — сам StubView не трогает.
struct StubView: View {
    let title: String
    let specSection: String

    var body: some View {
        VStack {
            Spacer()
            DSEmptyState(
                icon: .document,
                title: title,
                message: "Экран в разработке.\nПоведение — SCREEN_SPECS.md, \(specSection)."
            )
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DSColor.bg.ignoresSafeArea())
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Заглушка шторки: каркас DSBottomSheet + StubView-содержимое.
struct StubSheetView: View {
    let title: String
    let specSection: String
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(header: .titleClose(title: title, subtitle: nil), onClose: onClose) {
            Text("Шторка в разработке.\nПоведение — SCREEN_SPECS.md, \(specSection).")
                .dsTextStyle(.hint)
                .padding(.horizontal, 4)
                .padding(.top, DSSpacing.sm)
        }
    }
}

#Preview("StubView — light") {
    NavigationStack { StubView(title: "Корзина", specSection: "§9.4") }
        .preferredColorScheme(.light)
}

#Preview("StubView — dark") {
    NavigationStack { StubView(title: "Корзина", specSection: "§9.4") }
        .preferredColorScheme(.dark)
}

#Preview("StubSheetView — light") {
    StubSheetView(title: "Фильтры", specSection: "§9.0.1", onClose: {})
        .preferredColorScheme(.light)
}

#Preview("StubSheetView — dark") {
    StubSheetView(title: "Фильтры", specSection: "§9.0.1", onClose: {})
        .preferredColorScheme(.dark)
}
