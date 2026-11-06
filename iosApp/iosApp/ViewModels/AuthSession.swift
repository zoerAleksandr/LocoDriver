import SwiftUI
import ComposeApp

/// Единый источник истины авторизации на iOS (IOS_PLAN С4).
///
/// Обёртка Kotlin `AuthSessionHolder`: `isAuthorized` — непустой bearer-токен
/// в SecureTokenStorage. Гейт покупок и другие экраны берут признак отсюда,
/// а не передают «из рук в руки» от вызывающего экрана.
final class AuthSession: ObservableObject {
    static let shared = AuthSession()

    private let holder = IosViewModelHelper.shared.getAuthSessionHolder()

    /// Для подписок UI (@Published приходит асинхронно).
    @Published private(set) var isAuthorized: Bool = false

    /// Для гейтов (§3.2): живое значение — кэш @Published опаздывает
    /// (watchIsAuthorized доставляет обновления через main.async).
    var isAuthorizedNow: Bool { holder.isAuthorizedNow() }

    private init() {
        // Начальное значение — синхронно, чтобы гейт сразу видел актуальный статус.
        isAuthorized = holder.isAuthorizedNow()
        holder.watchIsAuthorized { [weak self] value in
            DispatchQueue.main.async { self?.isAuthorized = value.boolValue }
        }
    }
}
