import SwiftUI
import SafariServices

/// Страница оплаты Robokassa во встроенном браузере (SFSafariViewController,
/// iOS 16). Логин и пароли мерчанта в клиенте запрещены — только `payment_url`
/// серверного checkout (SCREEN_SPECS §19.3 «iOS»).
struct PurchasesSafariView: UIViewControllerRepresentable {
    let url: URL
    /// Пользователь закрыл страницу («Done») — запустить проверку оплаты (§19.4/§19.6).
    let onFinish: () -> Void

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: url)
        controller.delegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onFinish: onFinish) }

    final class Coordinator: NSObject, SFSafariViewControllerDelegate {
        let onFinish: () -> Void
        init(onFinish: @escaping () -> Void) { self.onFinish = onFinish }

        func safariViewControllerDidFinish(_ controller: SFSafariViewController) {
            onFinish()
        }
    }
}
