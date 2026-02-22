import SwiftUI
import ComposeApp

/// Мост SwiftUI → Compose Multiplatform.
///
/// UIViewControllerRepresentable оборачивает UIViewController,
/// который создаётся Kotlin-функцией MainViewController() из iosMain.
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose сам управляет клавиатурой
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Вызов Kotlin-функции из ComposeApp.xcframework
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
