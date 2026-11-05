import SwiftUI
import ComposeApp

/// Единый источник истины авторизации на iOS (IOS_PLAN С4).
///
/// Обёртка Kotlin `AuthSessionHolder`: `isAuthorized` — непустой bearer-токен
/// в SecureTokenStorage. Гейт покупок и другие экраны берут признак отсюда,
/// а не передают «из рук в руки» от вызывающего экрана.
@MainActor
final class AuthSession: ObservableObject {
    static let shared = AuthSession()

    private let holder = IosViewModelHelper.shared.getAuthSessionHolder()

    @Published private(set) var isAuthorized: Bool = false

    private init() {
        holder.watchIsAuthorized { [weak self] value in
            DispatchQueue.main.async { self?.isAuthorized = value.boolValue }
        }
    }
}
