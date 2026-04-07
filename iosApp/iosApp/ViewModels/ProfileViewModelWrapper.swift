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
    }

    func login(email: String, password: String) {
        viewModel.login(email: email, password: password)
    }

    func loginWithVK() {
        viewModel.loginWithVK()
    }

    func syncData() {
        viewModel.syncData()
    }

    func logout() {
        viewModel.logout()
    }

    func clearError() {
        viewModel.clearError()
    }
}
