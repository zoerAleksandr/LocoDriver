import SwiftUI

// Единый диалог подтверждения удаления для ВСЕХ экранов iOS (SCREEN_SPECS §24.3).
// Образец — форма маршрута («Удалить локомотив?»): системный alert, заголовок-вопрос,
// пояснение «… Это действие нельзя отменить.», кнопки «Да, удалить» (destructive) и «Отмена».
// Новые экраны используют его вместо собственных шторок/диалогов подтверждения.

/// Запрос подтверждения удаления.
struct DSDeleteRequest: Identifiable {
    let id = UUID()
    let title: String
    var message: String = "Это действие нельзя отменить."
    var confirmTitle: String = "Да, удалить"
    let action: () -> Void
}

extension View {
    /// Показывает alert подтверждения удаления, пока `request` не `nil`.
    /// Alert вешается на отдельный фон: два `.alert` на одном View не работают
    /// (показывается только последний), поэтому экран может безопасно иметь и другие alert-ы.
    func dsDeleteAlert(_ request: Binding<DSDeleteRequest?>) -> some View {
        background(
            Color.clear.alert(
                request.wrappedValue?.title ?? "",
                isPresented: Binding(
                    get: { request.wrappedValue != nil },
                    set: { if !$0 { request.wrappedValue = nil } }
                ),
                presenting: request.wrappedValue
            ) { item in
                Button(item.confirmTitle, role: .destructive) {
                    item.action()
                    request.wrappedValue = nil
                }
                Button("Отмена", role: .cancel) { request.wrappedValue = nil }
            } message: { item in
                Text(item.message)
            }
        )
    }
}
