import SwiftUI
import ComposeApp

@MainActor
final class ProfileViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getProfileViewModel()

    @Published var isLoggedIn: Bool = false
    @Published var isLoading: Bool = false
    @Published var isSyncing: Bool = false
    @Published var userEmail: String? = nil
    @Published var errorMessage: String? = nil
    @Published var syncMessage: String? = nil
    /// Типизированная ошибка для алерта (sync с retry-кнопкой).
    /// login-ошибки публикуются сюда же, но ProfileView фильтрует
    /// alert по isLoggedIn — login показывает inline через errorMessage.
    @Published var error: AppError? = nil

    /// Login БЕЗ retry — auth-операция, повтор через ручной ввод пароля.
    private var lastAction: LastAction? = nil
    private enum LastAction {
        case sync
    }

    init() {
        viewModel.watchIsLoggedIn { [weak self] value in
            DispatchQueue.main.async { self?.isLoggedIn = value.boolValue }
        }
        viewModel.watchIsLoading { [weak self] value in
            DispatchQueue.main.async { self?.isLoading = value.boolValue }
        }
        viewModel.watchIsSyncing { [weak self] value in
            DispatchQueue.main.async { self?.isSyncing = value.boolValue }
        }
        viewModel.watchUserEmail { [weak self] value in
            DispatchQueue.main.async { self?.userEmail = value }
        }
        viewModel.watchErrorMessage { [weak self] value in
            DispatchQueue.main.async { self?.errorMessage = value }
        }
        viewModel.watchSyncMessage { [weak self] value in
            DispatchQueue.main.async { self?.syncMessage = value }
        }
        viewModel.watchError { [weak self] e in
            DispatchQueue.main.async { self?.error = e }
        }
    }

    func login(email: String, password: String) {
        // НЕ записываем lastAction — login retry бесполезен.
        viewModel.login(email: email, password: password)
    }

    func loginWithVK() {
        viewModel.loginWithVK()
    }

    func syncData() {
        lastAction = .sync
        viewModel.syncData()
    }

    func logout() {
        lastAction = nil
        viewModel.logout()
    }

    func clearError() {
        viewModel.clearError()
        lastAction = nil
    }

    func retry() {
        guard let a = lastAction else { return }
        switch a {
        case .sync: syncData()
        }
    }
}
