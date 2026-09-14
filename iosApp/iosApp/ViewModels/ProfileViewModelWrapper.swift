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
    @Published var referralCode: String? = nil
    @Published var referralCount: Int = 0
    @Published var referralRewardedCount: Int = 0
    @Published var referralError: String? = nil
    @Published var canApplyReferralCode: Bool = false
    @Published var appliedReferralCode: String? = nil

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
        viewModel.watchReferralCode { [weak self] value in
            DispatchQueue.main.async { self?.referralCode = value }
        }
        viewModel.watchReferralCount { [weak self] value in
            DispatchQueue.main.async { self?.referralCount = value.intValue }
        }
        viewModel.watchReferralRewardedCount { [weak self] value in
            DispatchQueue.main.async { self?.referralRewardedCount = value.intValue }
        }
        viewModel.watchReferralError { [weak self] value in
            DispatchQueue.main.async { self?.referralError = value }
        }
        viewModel.watchCanApplyReferralCode { [weak self] value in
            DispatchQueue.main.async { self?.canApplyReferralCode = value.boolValue }
        }
        viewModel.watchAppliedReferralCode { [weak self] value in
            DispatchQueue.main.async { self?.appliedReferralCode = value }
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

    func loadReferrals() {
        viewModel.loadReferrals()
    }

    func applyReferralCode(_ code: String) {
        viewModel.applyReferralCode(code: code)
    }

    func logout() {
        viewModel.logout()
    }

    func clearError() {
        viewModel.clearError()
    }
}
