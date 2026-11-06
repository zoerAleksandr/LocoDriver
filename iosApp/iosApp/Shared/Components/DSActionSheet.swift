import SwiftUI

// Контракт: SCREEN_SPECS §24.1 (`AppBottomSheet` + `BottomSheetAction`):
// нижняя шторка действий — ручка, необязательный заголовок/описание, список
// действий; опасное действие выделяется цветом ошибки; тап по действию сначала
// закрывает шторку (`onDismissRequest`), затем выполняется действие.
// Показывается системным `.sheet` (см. `View.dsSheet`).

/// Одно действие шторки `DSActionSheet`.
struct DSActionSheetAction: Identifiable {
    enum Role {
        case normal
        /// Визуально выделяется цветом ошибки (§24.1).
        case destructive
    }

    let id = UUID()
    let title: String
    var role: Role = .normal
    let perform: () -> Void
}

/// Стандартная нижняя шторка действий (`AppBottomSheet`, §24.1).
/// Тап по действию сначала закрывает шторку, затем выполняет действие.
struct DSActionSheet: View {
    let title: String
    var message: String? = nil
    let actions: [DSActionSheetAction]
    /// Закрытие шторки (свайп/тап вне/крестик/после выбора действия).
    let onDismiss: () -> Void

    var body: some View {
        DSBottomSheet(
            header: .titleClose(title: title, subtitle: message),
            onClose: onDismiss
        ) {
            VStack(spacing: 0) {
                ForEach(actions) { action in
                    Button {
                        // §24.1: сначала закрыть шторку, затем действие.
                        onDismiss()
                        action.perform()
                    } label: {
                        Text(action.title)
                            .font(DSFont.sans(16, .medium))
                            .foregroundColor(action.role == .destructive ? DSColor.danger : DSColor.text)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.vertical, 14)
                    }
                    DSDivider(inset: 0)
                }
                Button("Отмена", action: onDismiss)
                    .font(DSFont.sans(16))
                    .foregroundColor(DSColor.accent)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, 14)
            }
        }
    }
}

#Preview("DSActionSheet — light") {
    Color.gray.sheet(isPresented: .constant(true)) {
        DSActionSheet(
            title: "Удалить значение",
            message: "08:15",
            actions: [
                DSActionSheetAction(title: "Удалить значение", role: .destructive) {},
            ],
            onDismiss: {}
        )
        .presentationDetents([.height(320)])
        .presentationDragIndicator(.hidden)
    }
    .preferredColorScheme(.light)
}
